package UI;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class JavaFXApp extends Application {

    private ComboBox<String> languageComboBox;
    private TextArea outputTextArea;
    private Label statusLabel;
    private TextField repoUrlTextField;
    private TextField customBuildCommandTextField;
    private Button fetchRepoButton;
    private Button selectProjectButton;
    private Button buildButton;
    private File selectedDirectory;
    private ScheduledExecutorService executorService;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Initialize UI Components
        languageComboBox = new ComboBox<>();
        outputTextArea = new TextArea();
        statusLabel = new Label("Welcome to GitHub Repo Builder");
        repoUrlTextField = new TextField();
        repoUrlTextField.setPromptText("Enter GitHub Repository URL");
        customBuildCommandTextField = new TextField();
        customBuildCommandTextField.setPromptText("Custom Build Command (Optional)");
        
        fetchRepoButton = new Button("Fetch Repository");
        selectProjectButton = new Button("Select Project Directory");
        buildButton = new Button("Build Project");

        // Set up UI Layout
        VBox layout = new VBox(10);
        layout.getChildren().addAll(
            statusLabel,
            repoUrlTextField,
            fetchRepoButton,
            selectProjectButton,
            languageComboBox,
            customBuildCommandTextField,
            buildButton,
            outputTextArea
        );

        // Set up event handlers
        fetchRepoButton.setOnAction(event -> handleFetchRepository());
        selectProjectButton.setOnAction(event -> handleProjectSelection());
        buildButton.setOnAction(event -> handleBuildProject());

        // Configure ComboBox
        languageComboBox.getItems().addAll(
            "Automatic Detection", 
            "Java (Maven)", 
            "Java (Gradle)", 
            "Python", 
            "JavaScript (Node.js)", 
            "Docker", 
            "Custom"
        );
        languageComboBox.setOnAction(event -> handleLanguageSelection());

        // Initial UI Configuration
        buildButton.setDisable(true);
        customBuildCommandTextField.setVisible(false);

        // Set up Scene and Stage
        Scene scene = new Scene(layout, 600, 500);
        primaryStage.setTitle("GitHub Repo Builder");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void handleFetchRepository() {
        String repoUrl = repoUrlTextField.getText().trim();
        if (repoUrl.isEmpty()) {
            statusLabel.setText("Please enter a valid GitHub repository URL.");
            return;
        }

        fetchRepoFromGitHub(repoUrl);
    }

    private void fetchRepoFromGitHub(String repoUrl) {
        // GitHub API request URL
        String[] urlParts = repoUrl.split("/");
        if (urlParts.length < 2) {
            statusLabel.setText("Invalid repository URL.");
            return;
        }

        String owner = urlParts[urlParts.length - 2];  // Last but one
        String repo = urlParts[urlParts.length - 1];   // Last part
        String apiUrl = "https://api.github.com/repos/" + owner + "/" + repo;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(apiUrl)).build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            String repoData = response.body();
                            String repoName = extractRepoName(repoData);
                            statusLabel.setText("Repository Found: " + repoName);
                            checkAndUpdateRepository(repoUrl, repoName);
                        } else {
                            statusLabel.setText("Error: " + response.statusCode() + " - " + response.body());
                        }
                    });
                    return response;
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> statusLabel.setText("An error occurred: " + e.getMessage()));
                    return null;
                });
    }

    private String extractRepoName(String repoData) {
        String repoName = "Repo";
        if (repoData.contains("\"name\":")) {
            int start = repoData.indexOf("\"name\":") + 8;
            int end = repoData.indexOf("\"", start);
            if (start > 0 && end > 0) {
                repoName = repoData.substring(start, end);
            }
        }
        return repoName;
    }

    private void checkAndUpdateRepository(String repoUrl, String repoName) {
        String localRepoPath = "cloned_repos/" + repoName;
        File repoDirectory = new File(localRepoPath);

        if (repoDirectory.exists()) {
            schedulePeriodicPull(repoUrl, localRepoPath);
            selectedDirectory = repoDirectory;
            statusLabel.setText("Repository already exists. Scheduling periodic updates.");
        } else {
            cloneRepository(repoUrl, repoName);
        }
    }

    private void cloneRepository(String repoUrl, String repoName) {
        String cloneCommand = "git clone " + repoUrl + " cloned_repos/" + repoName;
        executeCommand(cloneCommand, "Repository cloned successfully!", "Failed to clone the repository.");
        
        // Set selected directory after cloning
        selectedDirectory = new File("cloned_repos/" + repoName);
    }

    private void pullRepository(String localRepoPath) {
        String pullCommand = "git -C " + localRepoPath + " pull";
        executeCommand(pullCommand, "Repository updated successfully!", "Failed to update the repository.");
    }

    private void schedulePeriodicPull(String repoUrl, String localRepoPath) {
        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newSingleThreadScheduledExecutor();
        }

        executorService.scheduleAtFixedRate(() -> pullRepository(localRepoPath), 0, 1, TimeUnit.MINUTES);
        statusLabel.setText("Periodic updates scheduled for repository: " + repoUrl);
    }

    private void handleProjectSelection() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Project Directory");
        
        selectedDirectory = directoryChooser.showDialog(null);
        
        if (selectedDirectory != null) {
            String detectedLanguage = detectLanguage(selectedDirectory);
            statusLabel.setText("Detected Language: " + detectedLanguage);
            
            // Automatically select detected language if possible
            languageComboBox.setValue(detectedLanguage);
            
            buildButton.setDisable(false);
            customBuildCommandTextField.setVisible(false);
        }
    }

    private void handleLanguageSelection() {
        String selectedLanguage = languageComboBox.getValue();
        
        if ("Custom".equals(selectedLanguage)) {
            customBuildCommandTextField.setVisible(true);
            customBuildCommandTextField.setPromptText("Enter custom build command");
        } else {
            customBuildCommandTextField.setVisible(false);
        }
    }

    private void handleBuildProject() {
        String selectedLanguage = languageComboBox.getValue();
        String buildCommand = null;

        // Reset output area
        outputTextArea.clear();

        if ("Custom".equals(selectedLanguage)) {
            buildCommand = customBuildCommandTextField.getText().trim();
            if (buildCommand.isEmpty()) {
                statusLabel.setText("Error: Please enter a custom build command");
                return;
            }
        } else {
            buildCommand = getBuildCommandForLanguage(selectedLanguage);
        }

        if (buildCommand == null) {
            statusLabel.setText("Error: Invalid build configuration");
            return;
        }

        executeBuildCommand(buildCommand);
    }

    private String getBuildCommandForLanguage(String language) {
        if (selectedDirectory == null) {
            statusLabel.setText("Error: No project directory selected");
            return null;
        }

        switch (language) {
            case "Java (Maven)":
                return "mvn clean install -f " + selectedDirectory.getAbsolutePath() + "/pom.xml";
            case "Java (Gradle)":
                return "cd " + selectedDirectory.getAbsolutePath() + " && ./gradlew build";
            case "Python":
                return "cd " + selectedDirectory.getAbsolutePath() + " && python3 server.py ";
            case "JavaScript (Node.js)":
                return "cd " + selectedDirectory.getAbsolutePath() + " && npm install";
            case "Docker":
                return "cd " + selectedDirectory.getAbsolutePath() + " && docker build -t myproject .";
            case "Automatic Detection":
                return getBuildCommandForDetectedLanguage();
            default:
                statusLabel.setText("Unsupported build configuration");
                return null;
        }
    }

    private String getBuildCommandForDetectedLanguage() {
        String detectedLanguage = detectLanguage(selectedDirectory);
        return getBuildCommandForLanguage(detectedLanguage);
    }

    private void executeCommand(String command, String successMessage, String failureMessage) {
        outputTextArea.clear();
        statusLabel.setText("Executing command...");

        new Thread(() -> {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);
                processBuilder.redirectErrorStream(true);
                Process process = processBuilder.start();

                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
                );

                String line;
                while ((line = reader.readLine()) != null) {
                    final String finalLine = line;
                    Platform.runLater(() -> {
                        outputTextArea.appendText(finalLine + "\n");
                    });
                }

                int exitCode = process.waitFor();
                
                Platform.runLater(() -> {
                    if (exitCode == 0) {
                        statusLabel.setText(successMessage);
                        // Enable build button after successful clone/pull
                        buildButton.setDisable(false);
                    } else {
                        statusLabel.setText(failureMessage + " Exit Code: " + exitCode);
                    }
                });

            } catch (IOException | InterruptedException e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Error: " + e.getMessage());
                    outputTextArea.appendText("Error: " + e.getMessage());
                });
            }
        }).start();
    }

    private void executeBuildCommand(String command) {
        statusLabel.setText("Building project...");
        outputTextArea.clear();

        new Thread(() -> {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);
                processBuilder.redirectErrorStream(true);
                Process process = processBuilder.start();

                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
                );

                String line;
                StringBuilder output = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    final String finalLine = line;
                    Platform.runLater(() -> {
                        outputTextArea.appendText(finalLine + "\n");
                    });
                    output.append(line).append("\n");
                }

                int exitCode = process.waitFor();
                
                Platform.runLater(() -> {
                    if (exitCode == 0) {
                        statusLabel.setText("Build Successful!");
                    } else {
                        statusLabel.setText("Build Failed. Exit Code: " + exitCode);
                    }
                });

            } catch (IOException | InterruptedException e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Build Error: " + e.getMessage());
                    outputTextArea.appendText("Error: " + e.getMessage());
                });
            }
        }).start();
    }

    private String detectLanguage(File projectDir) {
        if (projectDir == null) return "Automatic Detection";

        if (new File(projectDir, "pom.xml").exists()) {
            return "Java (Maven)";
        } else if (new File(projectDir, "build.gradle").exists()) {
            return "Java (Gradle)";
        } else if (new File(projectDir, "package.json").exists()) {
            return "JavaScript (Node.js)";
        } else if (new File(projectDir, "requirements.txt").exists()) {
            return "Python";
        } else if (new File(projectDir, "Dockerfile").exists()) {
            return "Docker";
        }
        return "Custom";
    }

    @Override
    public void stop() {
        // Shutdown executor service if it's running
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}