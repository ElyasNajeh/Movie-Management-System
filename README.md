# Movies Management System

A JavaFX desktop application for maintaining, searching, and reviewing an in-memory movie catalog with text-file import and export.

## Features

- Add, update, and remove movies with title, description, release year, and rating validation.
- Search for an exact movie title or all movies released in a given year.
- Filter the main table to the highest- or lowest-rated movies.
- Browse each hash-table bucket in ascending or descending title order.
- Load and save movie records using the application's four-line text format.
- Clear the current in-memory catalog.

## Technologies & Tools

- Java 25: the project's compilation target and required LTS JDK.
- JavaFX 25: desktop UI controls, tables, dialogs, layouts, and styling.
- Maven Wrapper: reproducible dependency management, builds, tests, and application launch.
- JUnit 6: regression tests for the existing data structures and file handling.

## Data Structures

- Hash table of AVL trees: indexes movies by title while keeping collisions balanced and title-sorted.
- Year AVL tree: indexes movies by release year and title for year-based searches.
- JavaFX `ObservableList`: keeps table contents synchronized with the active in-memory movie collection.

## Prerequisites

- JDK 25. `JAVA_HOME` is optional when `java` and `javac` are available on `PATH`.
- Internet access on the first build so the Maven Wrapper can download Maven and project dependencies.

## Getting Started

```bash
git clone https://github.com/ElyasNajeh/Movies-Management-System.git
cd Movies-Management-System
./mvnw clean
./mvnw javafx:run
```

On Windows PowerShell, use `./mvnw.cmd clean test` and `./mvnw.cmd javafx:run`.

## Project Structure

- `src/main/java/application/`: existing Java classes and package organization.
- `src/main/resources/application/`: stylesheet and bundled icons.regression tests.
- `pom.xml`: Java, JavaFX, testing, and run configuration.
- `.mvn/wrapper/`, `mvnw`, `mvnw.cmd`: Maven Wrapper files.
