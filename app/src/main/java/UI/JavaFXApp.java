package UI;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class JavaFXApp extends Application {

    private Label statusLabel;  // label for status feedback

    @Override
    public void start(Stage primaryStage) {
       
        TextField repoInputField = new TextField();
        repoInputField.setPromptText("Enter GitHub repository URL");

        Button fetchButton = new Button("Fetch Repo");

        statusLabel = new Label();  // Initialize the status label
        statusLabel.setStyle("-fx-text-fill: red;"); // Setting error color initially to red

        fetchButton.setOnAction(e -> {
            String repoUrl = repoInputField.getText();
            if (repoUrl.isEmpty()) {
                statusLabel.setText("Please enter a valid GitHub repository URL.");
            } else {
                fetchRepoFromGitHub(repoUrl); // Call the method to fetch repo
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

    String owner = urlParts[urlParts.length - 2];
    String repo = urlParts[urlParts.length - 1];
    String apiUrl = "https://api.github.com/repos/" + owner + "/" + repo;

    // Performing asynchronous HTTP request to GitHub API
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(apiUrl))
            .build();

    client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        // If the repo is found, a success message with some repo details
                        String repoData = response.body(); // To get the repo details in JSON format
                        String repoName = extractRepoName(repoData);  // Extracting repo name
                        statusLabel.setText("Repository Found: " + repoName);

                        // Clone the repository if it exists,& pass repoName
                        cloneRepository(repoUrl, repoName);
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


    // extracting repo name from the JSON response (just to show a user-friendly prompt)
    private String extractRepoName(String repoData) {
        String repoName = "Repo"; // Default value
        if (repoData.contains("\"name\":")) {
            int start = repoData.indexOf("\"name\":") + 8; // 8 is the length of `"name":` we skip that length and take the next value which will be required
            int end = repoData.indexOf("\"", start);
            if (start > 0 && end > 0) {
                repoName = repoData.substring(start, end);
            }
        }
        return repoName;
    }

  // To clone the repository locally
private void cloneRepository(String repoUrl, String repoName) {
    
    String cloneCommand = "git clone " + repoUrl + " cloned_repos/" + repoName;

    // Creating a new ProcessBuilder to execute the git command
    ProcessBuilder processBuilder = new ProcessBuilder();
    processBuilder.command("bash", "-c", cloneCommand);  // Use bash to execute git command

    try {
        // Starting the process to clone the repository
        Process process = processBuilder.start();
        int exitCode = process.waitFor();

        // Checking if the cloning was successful
        if (exitCode == 0) {
            Platform.runLater(() -> statusLabel.setText("Repository cloned successfully!"));
        } else {
            Platform.runLater(() -> statusLabel.setText("Failed to clone the repository."));
        }
    } catch (IOException | InterruptedException e) {
        Platform.runLater(() -> statusLabel.setText("Error cloning repository: " + e.getMessage()));
    }
}

}
