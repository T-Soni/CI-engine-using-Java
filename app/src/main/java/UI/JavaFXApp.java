package UI;

import java.io.File;
import java.io.IOException;
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
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class JavaFXApp extends Application {

    private Label statusLabel; // label for status feedback
    private ScheduledExecutorService executorService; // For periodic updates

    @Override
    public void start(Stage primaryStage) {
        TextField repoInputField = new TextField();
        repoInputField.setPromptText("Enter GitHub repository URL");

        Button fetchButton = new Button("Fetch Repo");

        statusLabel = new Label(); // Initializing the status label
        statusLabel.setStyle("-fx-text-fill: red;"); // Setting error color initially to red

        fetchButton.setOnAction(e -> {
            String repoUrl = repoInputField.getText();
            if (repoUrl.isEmpty()) {
                statusLabel.setText("Please enter a valid GitHub repository URL.");
            } else {
                fetchRepoFromGitHub(repoUrl); // Calling fetch repo
            }
        });

        VBox layout = new VBox(10);
        layout.getChildren().addAll(repoInputField, fetchButton, statusLabel);

        Scene scene = new Scene(layout, 400, 200);
        primaryStage.setTitle("GitHub Repo Fetcher");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void fetchRepoFromGitHub(String repoUrl) {
        // Converting GitHub URL to GitHub API format (GET /repos/:owner/:repo)
        String[] urlParts = repoUrl.split("/");
        if (urlParts.length < 2) {
            Platform.runLater(() -> statusLabel.setText("Invalid repository URL."));
            return;
        }

        String owner = urlParts[urlParts.length - 2];//last but one
        String repo = urlParts[urlParts.length - 1];//last paart
        String apiUrl = "https://api.github.com/repos/" + owner + "/" + repo;

        // asynchronous HTTP request to GitHub API
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))//get request
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            // If the repo is found, a success message with some repo details
                            String repoData = response.body(); // To get the repo details in JSON format
                            String repoName = extractRepoName(repoData); // Extracting repo name
                            statusLabel.setText("Repository Found: " + repoName);

                            // Clone or pull the repository
                            checkAndUpdateRepository(repoUrl, repoName);
                        } else {
                            // error message for unsuccessful requests
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
        String repoName = "Repo"; //A random Default value
        if (repoData.contains("\"name\":")) {
            int start = repoData.indexOf("\"name\":") + 8;//since size of "name:" is 8 and we dont need it
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
            // If the repository exists, pull updates
            schedulePeriodicPull(repoUrl, localRepoPath);
        } else {
            // Clone the repository
            cloneRepository(repoUrl, repoName);
        }
    }

    private void cloneRepository(String repoUrl, String repoName) {
        String cloneCommand = "git clone " + repoUrl + " cloned_repos/" + repoName;

        executeCommand(cloneCommand, "Repository cloned successfully!", "Failed to clone the repository.");
    }

    private void pullRepository(String localRepoPath) {
        String pullCommand = "git -C " + localRepoPath + " pull";

        executeCommand(pullCommand, "Repository updated successfully!", "Failed to update the repository.");
    }

    private void executeCommand(String command, String successMessage, String failureMessage) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("bash", "-c", command);

        try {
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            Platform.runLater(() -> {
                if (exitCode == 0) {
                    statusLabel.setText(successMessage);
                } else {
                    statusLabel.setText(failureMessage);
                }
            });
        } catch (IOException | InterruptedException e) {
            Platform.runLater(() -> statusLabel.setText("Error: " + e.getMessage()));
        }
    }

   private void schedulePeriodicPull(String repoUrl, String localRepoPath) {
    if (executorService == null || executorService.isShutdown()) {
        executorService = Executors.newSingleThreadScheduledExecutor();
    }

    executorService.scheduleAtFixedRate(() -> {
        pullRepository(localRepoPath);
    }, 0, 1, TimeUnit.MINUTES);

    Platform.runLater(() -> statusLabel.setText("Periodic updates scheduled for repository: " + repoUrl));
}

}
