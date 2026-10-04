package application;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.FileChooser;

public class MovieCatalog {
	private static final int INITIAL_CAPACITY = 17;
	private static final String TITLE_PREFIX = "Title:";
	private static final String DESCRIPTION_PREFIX = "Description:";
	private static final String YEAR_PREFIX = "Release Year:";
	private static final String RATING_PREFIX = "Rating:";

	int max_Size = INITIAL_CAPACITY; // Initial hash table size (must be a prime number)
	AVL[] arrayOfAVLTree; // Hash table: each index holds an AVL tree
	AVL yearTree; // AVL tree to support search by year
	Alerts alerts = new Alerts();

	// Constructor: initializes the hash table and year tree
	MovieCatalog() {
		arrayOfAVLTree = createTable(max_Size);
		yearTree = new AVL();
	}

	private AVL[] createTable(int size) {
		AVL[] table = new AVL[size];
		for (int i = 0; i < size; i++) {
			table[i] = new AVL();
		}
		return table;
	}

	// Converts a string into a number hash using base 32
	public int hashString(String key) {
		int hash = 0;
		for (int i = 0; i < key.length(); i++) {
			hash = hash * 32 + key.charAt(i);
		}
		return hash;
	}

	// Hash function that reduces a title to a valid array index
	public int hashFunction(String key) {
		return Math.floorMod(hashString(key), max_Size);
	}

	// Checks if a number is prime
	public boolean isPrime(int num) {
		if (num < 2) {
			return false;
		}
		for (int i = 2; i <= Math.sqrt(num); i++) {
			if (num % i == 0) {
				return false;
			}
		}
		return true;
	}

	// Finds the next prime number after twice the current table size
	public int LargestPrimetoRehash(int size) {
		int candidate = 2 * size + 1;
		while (!isPrime(candidate)) {
			candidate++;
		}
		return candidate;
	}

	// Calculates the average height of non-empty bucket trees
	private double AverageHeightofAllAVL() {
		int count = 0;
		int totalHeight = 0;
		for (AVL tree : arrayOfAVLTree) {
			if (tree.getRoot() != null) {
				totalHeight += tree.getRoot().getHeight();
				count++;
			}
		}
		return count == 0 ? 0 : (double) totalHeight / count;
	}

	// Rehashes the title index when its average bucket height becomes too large
	private void rehash() {
		int newSize = LargestPrimetoRehash(max_Size);
		AVL[] oldTable = arrayOfAVLTree;
		arrayOfAVLTree = createTable(newSize);
		max_Size = newSize;

		for (AVL oldTree : oldTable) {
			for (Movie movie : oldTree.inOrderMovies()) {
				arrayOfAVLTree[hashFunction(movie.getTitle())].insert(movie);
			}
		}
	}

	// Inserts a movie into the title hash table and year tree
	public boolean insert(Movie movie) {
		int index = hashFunction(movie.getTitle());
		if (arrayOfAVLTree[index].find(movie.getTitle()) != null) {
			return false;
		}

		arrayOfAVLTree[index].insert(movie);
		yearTree.insertByYear(movie);
		if (AverageHeightofAllAVL() > 3) {
			rehash();
		}
		return true;
	}

	// Moves a movie to the correct title bucket after a title change
	public boolean updateTitleTree(Movie movie, String newTitle) {
		if (movie.getTitle().equals(newTitle)) {
			return true;
		}

		int newIndex = hashFunction(newTitle);
		if (arrayOfAVLTree[newIndex].find(newTitle) != null) {
			return false;
		}

		int oldIndex = hashFunction(movie.getTitle());
		yearTree.deleteByYear(movie);
		arrayOfAVLTree[oldIndex].delete(movie);
		movie.setTitle(newTitle);
		arrayOfAVLTree[hashFunction(newTitle)].insert(movie);
		yearTree.insertByYear(movie);
		return true;
	}

	// Moves a movie to the correct position in the year tree after a year change
	public void updateYearTree(Movie movie, int newYear) {
		if (movie.getReleaseYear() == newYear) {
			return;
		}
		yearTree.deleteByYear(movie);
		movie.setReleaseYear(newYear);
		yearTree.insertByYear(movie);
	}

	// Searches for movies by exact title or release year
	public ObservableList<Movie> searchMovie(String search) {
		ObservableList<Movie> results = FXCollections.observableArrayList();
		if (search == null || search.trim().isEmpty()) {
			alerts.ErrorAlert("Error", "Please enter a movie title or release year to search.");
			return results;
		}

		String query = search.trim();
		if (query.chars().allMatch(Character::isDigit)) {
			try {
				results.addAll(yearTree.findYear(Integer.parseInt(query)));
			} catch (NumberFormatException ignored) {
				// An out-of-range number cannot match an int release year.
			}
		}

		AVLTNode titleMatch = arrayOfAVLTree[hashFunction(query)].find(query);
		if (titleMatch != null && !results.contains(titleMatch.getMovie())) {
			results.add(titleMatch.getMovie());
		}
		return results;
	}

	// Deletes a movie from both indexes
	public void delete(String title) {
		int index = hashFunction(title);
		AVLTNode existing = arrayOfAVLTree[index].find(title);
		if (existing == null) {
			alerts.ErrorAlert("Error", "This movie does not exist.");
			return;
		}
		arrayOfAVLTree[index].delete(existing.getMovie());
		yearTree.deleteByYear(existing.getMovie());
	}

	// Removes all movie data from memory while retaining the initial structure
	public void deallocate() {
		max_Size = INITIAL_CAPACITY;
		arrayOfAVLTree = createTable(max_Size);
		yearTree = new AVL();
	}

	// Lets the user select and load a movie text file
	public void loadFromFile() {
		FileChooser chooser = createTextFileChooser("Select Movie File");
		File file = chooser.showOpenDialog(null);
		if (file == null) {
			return;
		}

		try {
			LoadResult result = loadFromFile(file, Main.movieList);
			String message = result.loaded() + " movie(s) loaded.";
			if (result.skipped() > 0) {
				message += " " + result.skipped() + " invalid or duplicate record(s) skipped.";
			}
			alerts.InfoAlert("Load Complete", message);
		} catch (IOException e) {
			alerts.ErrorAlert("Error", "Could not read the selected file.");
		}
	}

	LoadResult loadFromFile(File file, ObservableList<Movie> destination) throws IOException {
		List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
		int loaded = 0;
		int skipped = 0;

		for (int index = 0; index < lines.size();) {
			if (lines.get(index).isBlank()) {
				index++;
				continue;
			}
			if (index + 3 >= lines.size()) {
				skipped++;
				break;
			}

			String titleLine = lines.get(index++).trim();
			String descriptionLine = lines.get(index++).trim();
			String yearLine = lines.get(index++).trim();
			String ratingLine = lines.get(index++).trim();

			try {
				if (!titleLine.startsWith(TITLE_PREFIX) || !descriptionLine.startsWith(DESCRIPTION_PREFIX)
						|| !yearLine.startsWith(YEAR_PREFIX) || !ratingLine.startsWith(RATING_PREFIX)) {
					throw new IllegalArgumentException("Unexpected record format");
				}
				String title = titleLine.substring(TITLE_PREFIX.length()).trim();
				String description = descriptionLine.substring(DESCRIPTION_PREFIX.length()).trim();
				int releaseYear = Integer.parseInt(yearLine.substring(YEAR_PREFIX.length()).trim());
				double rating = Double.parseDouble(ratingLine.substring(RATING_PREFIX.length()).trim());
				if (title.isEmpty() || description.isEmpty() || releaseYear < 1 || !Double.isFinite(rating)
						|| rating < 0 || rating > 10) {
					throw new IllegalArgumentException("Invalid movie values");
				}

				Movie movie = new Movie(title, description, releaseYear, rating);
				if (insert(movie)) {
					destination.add(movie);
					loaded++;
				} else {
					skipped++;
				}
			} catch (IllegalArgumentException e) {
				skipped++;
			}
		}
		return new LoadResult(loaded, skipped);
	}

	// Lets the user select a writable destination for all current movie data
	public void saveToFile() {
		FileChooser chooser = createTextFileChooser("Save Movies");
		chooser.setInitialFileName("movies.txt");
		File file = chooser.showSaveDialog(null);
		if (file == null) {
			return;
		}

		try {
			saveToFile(file);
			alerts.InfoAlert("Success", "Movie data saved successfully.");
		} catch (IOException e) {
			alerts.ErrorAlert("Error", "Could not save movie data to the selected file.");
		}
	}

	void saveToFile(File file) throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
			for (AVL tree : arrayOfAVLTree) {
				for (Movie movie : tree.inOrderMovies()) {
					writer.write(buildUserData(movie));
					writer.newLine();
					writer.newLine();
				}
			}
		}
	}

	private FileChooser createTextFileChooser(String title) {
		FileChooser chooser = new FileChooser();
		chooser.setTitle(title);
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text files", "*.txt"));
		File home = new File(System.getProperty("user.home", "."));
		if (home.isDirectory()) {
			chooser.setInitialDirectory(home);
		}
		return chooser;
	}

	// Formats movie data as text (used when saving to file)
	private String buildUserData(Movie movie) {
		return TITLE_PREFIX + " " + movie.getTitle() + System.lineSeparator()
				+ DESCRIPTION_PREFIX + " " + movie.getDescription() + System.lineSeparator()
				+ YEAR_PREFIX + " " + movie.getReleaseYear() + System.lineSeparator()
				+ RATING_PREFIX + " " + movie.getRating();
	}

	record LoadResult(int loaded, int skipped) {
	}
}
