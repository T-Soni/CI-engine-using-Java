package org.openjfx;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class FXMLController {

    @FXML
    private ComboBox<String> languageComboBox;
    @FXML
    private TextArea outputTextArea;
    @FXML
    private Label outputLabel;
    @FXML
    private CheckBox testCheckBox; // CheckBox to toggle test phase
    @FXML
    private TextField repoUrlField; // TextField for repository URL

    @FXML
    private Button back;

    private File selectedDirectory;

    String repoName = new String();
    
    
    public void initializeController(){
        repoName = Globals.selectedRepoName.trim();
    }

    public void initializeRepoTextField(){
        if(!Globals.username.isEmpty() && !repoName.isEmpty()){
            String url = "https://github.com/" + Globals.username.trim() + "/" + repoName;
            repoUrlField.appendText(url);
        }
        
    }

    @FXML
    void backAction(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("RepoScene.fxml"));
            Parent repoSceneRoot = loader.load();
            Stage mainWindow = (Stage)back.getScene().getWindow();
            RepoSceneController repoSceneController = loader.getController();
            repoSceneController.initialize();
            Scene repoScene = new Scene(repoSceneRoot);
            mainWindow.setScene(repoScene);
            mainWindow.setTitle("Repositories");
            mainWindow.show();

        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }

    public void initialize() {
        // Populate the ComboBox with programming languages
        languageComboBox.getItems().addAll(
            "Java (Maven)",
            "Java (Gradle)",
            "Python",
            "JavaScript",
            "Docker"
        );
        languageComboBox.setVisible(false); // Initially hidden, will show only if needed
    }

    // Handle project directory selection
    @FXML
    private void handleSelectProject() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Project Directory");
        File directory = directoryChooser.showDialog(outputTextArea.getScene().getWindow());

        if (directory != null) {
            selectedDirectory = directory;

            // Detect the programming language
            String detectedLanguage = detectLanguage(selectedDirectory);

            if (!detectedLanguage.equals("Unknown")) {
                // Display detected language and hide the dropdown
                outputLabel.setText("Detected Language: " + detectedLanguage);
                languageComboBox.setVisible(false);
            } else {
                // If language is unknown, prompt user to manually select
                outputLabel.setText("Detected Language: Unknown. Please select.");
                languageComboBox.setVisible(true);
            }
        } else {
            outputTextArea.appendText("No directory selected.\n");
        }
    }

    // Handle cloning of repository
    @FXML
    private void handleCloneRepository() {
        String repoUrl = repoUrlField.getText().trim();

        if (repoUrl.isEmpty()) {
            outputTextArea.appendText("Please enter a repository URL.\n");
            return;
        }

        String repoName = extractRepoNameFromUrl(repoUrl);
        File repoDirectory = new File(System.getProperty("user.dir") + File.separator + repoName);

        if (repoDirectory.exists()) {
            outputTextArea.appendText("Repository already exists, pulling latest changes...\n");
            pullRepository(repoDirectory);
        } else {
            outputTextArea.appendText("Cloning repository...\n");
            cloneRepository(repoUrl, repoDirectory);
        }
    }

    // Clone the repository
    private void cloneRepository(String repoUrl, File repoDirectory) {
        String command = "git clone " + repoUrl + " " + repoDirectory.getAbsolutePath();

        executeCommand(command, "Repository cloned successfully!", "Error cloning repository.");
    }

    // Pull the latest changes from the repository
    private void pullRepository(File repoDirectory) {
        String command = "git pull";

        executeCommand(command, "Repository updated with the latest changes!", "Error pulling repository.");
    }

    // Extract the repository name from the URL (assumes GitHub URL structure)
    private String extractRepoNameFromUrl(String repoUrl) {
        String[] parts = repoUrl.split("/");
        return parts[parts.length - 1].replace(".git", "");
    }

    // Detect the programming language from files in the project
    private String detectLanguage(File projectDir) {
        if (containsFile(projectDir, "pom.xml")) {
            return "Java (Maven)";
        } else if (containsFile(projectDir, "build.gradle")) {
            return "Java (Gradle)";
        } else if (containsFile(projectDir, "package.json")) {
            return "JavaScript (Node.js)";
        } else if (containsFile(projectDir, "requirements.txt") || containsFile(projectDir, "setup.py")) {
            return "Python";
        } else if (containsFile(projectDir, "Dockerfile")) {
            return "Docker";
        }
        return "Unknown";
    }

    // Check if a specific file exists in the project directory
    private boolean containsFile(File projectDir, String fileName) {
        File[] files = projectDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().equals(fileName)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Handle language selection if detected language is not found
    @FXML
    private void handleLanguageSelection() {
        String selectedLanguage = languageComboBox.getValue();
        if (selectedLanguage != null) {
            outputLabel.setText("Selected Language: " + selectedLanguage);
            runBuildTool(selectedLanguage);
            if (testCheckBox.isSelected()) {
                runTests(selectedLanguage);
            }
        } else {
            outputTextArea.appendText("Please select a language from the dropdown.\n");
        }
    }

    // Run the corresponding build tool based on selected programming language
    private void runBuildTool(String selectedLanguage) {
        String command = null;
        switch (selectedLanguage) {
            case "Java (Maven)":
                command = "mvn clean install"; // Maven command to build the project
                break;
            case "Java (Gradle)":
                command = "./gradlew build"; // Gradle command to build the project
                break;
            case "Python":
                command = "python3 setup.py install"; // Python setup script
                break;
            case "JavaScript (Node.js)":
                command = "npm install"; // Node.js package install
                break;
            case "Docker":
                command = "docker build -t myimage ."; // Docker build command
                break;
            default:
                outputTextArea.appendText("Unsupported build tool or language.\n");
                return;
        }

        executeCommand(command, "Build completed successfully!", "Error during build.");
    }

    // Run the test phase if applicable
    private void runTests(String selectedLanguage) {
        String testCommand = null;

        if (selectedLanguage.equals("Java (Maven)")) {
            testCommand = "mvn test"; // Maven command to run tests
        } else if (selectedLanguage.equals("Java (Gradle)")) {
            testCommand = "./gradlew test"; // Gradle command to run tests
        } else if (selectedLanguage.equals("Python")) {
            testCommand = "pytest"; // Python pytest command to run tests
        } else if (selectedLanguage.equals("JavaScript (Node.js)")) {
            testCommand = "npm test"; // Node.js test command
        } else {
            outputTextArea.appendText("No tests available for this language.\n");
            return;
        }

        // Detect if tests are present in the project directory
        if (!containsTestFiles(selectedDirectory)) {
            outputTextArea.appendText("No tests found. Please add tests to your project.\n");
            return;
        }

        executeCommand(testCommand, "Tests completed successfully!", "Error during tests.");
    }

    // Check if test files exist in the project directory
    private boolean containsTestFiles(File projectDir) {
        // Check common directories for tests
        File testDir = new File(projectDir, "src/test");
        return testDir.exists() && testDir.isDirectory();
    }

    // Execute the given command (clone/pull, build, or test) and capture output
    private void executeCommand(String command, String successMessage, String errorMessage) {
        try {
            Process process = Runtime.getRuntime().exec(command, null, selectedDirectory);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                outputTextArea.appendText(line + "\n");
            }

            outputTextArea.appendText(successMessage + "\n");
        } catch (IOException e) {
            outputTextArea.appendText(errorMessage + ": " + e.getMessage() + "\n");
        }
    }
}
