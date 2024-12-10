package org.openjfx;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Timer;
import java.util.TimerTask;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

public class FXMLController {

    @FXML
    private TextArea outputTextArea;

    @FXML
    private CheckBox testCheckBox; 
    @FXML
    private TextArea buildCommandField; 
    @FXML
    private TextArea testCommandField; 

    @FXML
    private TextFlow outputTextFlow;

    @FXML
    private Button back;

    Text text = new Text();

    private String repoName = Globals.selectedRepoName.trim();
    private String repoUrl = "https://github.com/" + Globals.username.trim() + "/" + repoName;
    private File clonedReposDir = new File(System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "CI_clonedrepos");

    private String lastCommitHash = ""; // for the last commit hash to detect changes

    public void initializeController() {
        repoName = Globals.selectedRepoName.trim();
    }

    public void initializeRepoTextField() {
        if (!Globals.username.isEmpty() && !repoName.isEmpty()) {
            repoUrl = "https://github.com/" + Globals.username.trim() + "/" + repoName;
        }
    }

    @FXML
    void backAction(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("RepoScene.fxml"));
            Parent repoSceneRoot = loader.load();
            Stage mainWindow = (Stage) back.getScene().getWindow();
            RepoSceneController repoSceneController = loader.getController();
            repoSceneController.initialize();
            Scene repoScene = new Scene(repoSceneRoot);
            mainWindow.setScene(repoScene);
            mainWindow.setTitle("Repositories");
            mainWindow.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBuildAction() {
        if (!clonedReposDir.exists()) {
            if (clonedReposDir.mkdirs()) {
                text.setText("Created directory: CI_clonedrepos\n");
                outputTextFlow.getChildren().add(text);
                // outputTextArea.appendText("Created directory: CI_clonedrepos\n");
            } else {
                text.setText("Failed to create directory: CI_clonedrepos\n");
                outputTextFlow.getChildren().add(text);
                // outputTextArea.appendText("Failed to create directory: CI_clonedrepos\n");
                return;
            }
        }

        File repoDirectory = new File(clonedReposDir, repoName);

        if (repoDirectory.exists()) {
            text.setText("Repository already exists, pulling latest changes...\n");
            outputTextFlow.getChildren().add(text);
            // outputTextArea.appendText("Repository already exists, pulling latest changes...\n");
            checkAndAddRemote(repoDirectory, repoUrl);
            pullRepository(repoDirectory, repoUrl);
        } else {
            text.setText("Cloning repository...\n");
            outputTextFlow.getChildren().add(text);
            cloneRepository(repoUrl, repoDirectory);
        }

        startCIPipeline(repoDirectory);
    }

    private void cloneRepository(String repoUrl, File repoDirectory) {
        executeCommand("git clone " + repoUrl + " " + repoDirectory.getAbsolutePath(), clonedReposDir, "Repository cloned successfully!", "Error cloning repository.");
    }

    private void pullRepository(File repoDirectory, String repoUrl) {
        executeCommand("git pull origin main", repoDirectory, "Repository updated with the latest changes!", "Error pulling repository.");
    }

    private void checkAndAddRemote(File repoDirectory, String repoUrl) {
        try {
            String command = "git remote -v";
            Process process = Runtime.getRuntime().exec(command, null, repoDirectory);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            boolean hasRemote = false;

            while ((line = reader.readLine()) != null) {
                if (line.contains("origin")) {
                    hasRemote = true;
                    break;
                }
            }

            if (!hasRemote) {
                executeCommand("git remote add origin " + repoUrl, repoDirectory, "Remote 'origin' added successfully.", "Error adding remote.");
            }
        } catch (IOException e) {
            text.setText("Error checking or adding remote: " + e.getMessage() + "\n");
            outputTextFlow.getChildren().add(text);
            // outputTextArea.appendText("Error checking or adding remote: " + e.getMessage() + "\n");
        }
    }

    private void startCIPipeline(File repoDirectory) {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkForChangesAndRunBuild(repoDirectory);
            }
        }, 0, 60000); //to repeat every minute
    }

    private String getLatestCommitHash(File repoDirectory) {
        return executeCommandAndGetOutput("git log -1 --format=%H", repoDirectory, "Error checking commit hash.");
    }

    private void checkForChangesAndRunBuild(File repoDirectory) {
        pullRepository(repoDirectory, repoUrl);
        String currentCommitHash = getLatestCommitHash(repoDirectory);
        if (!currentCommitHash.equals(lastCommitHash)) {
            lastCommitHash = currentCommitHash;
            text.setText("New commit detected. Running build...\n");
            outputTextFlow.getChildren().add(text);
            // outputTextArea.appendText("New commit detected. Running build...\n");
            runBuildCommand(repoDirectory);
            if (testCheckBox.isSelected()) {
                runTestCommand(repoDirectory);
            }
        }
    }

    private void runBuildCommand(File repoDirectory) {
        String buildCommand = buildCommandField.getText().trim();
        if (!buildCommand.isEmpty()) {
            executeMultipleCommands(buildCommand, repoDirectory, "Build completed successfully!", "Build failed.");
        } else {
            text.setText("Please enter a build command.\n");
            outputTextFlow.getChildren().add(text);
            // outputTextArea.appendText("Please enter a build command.\n");
        }
    }

    private void runTestCommand(File repoDirectory) {
        String testCommand = testCommandField.getText().trim();
        if (!testCommand.isEmpty()) {
            executeMultipleCommands(testCommand, repoDirectory, "Tests completed successfully!", "Tests failed.");
        } else {
            text.setText("Please enter a test command.\n");
            outputTextFlow.getChildren().add(text);
            // outputTextArea.appendText("Please enter a test command.\n");
        }
    }

    private void executeCommand(String command, File workingDirectory, String successMessage, String errorMessage) {
        try {
            Process process = Runtime.getRuntime().exec(command, null, workingDirectory);
            BufferedReader stdOutputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader stdErrorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();
            String line;

            while ((line = stdOutputReader.readLine()) != null) {
                output.append(line).append("\n");
            }
            while ((line = stdErrorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                //green
                //outputTextArea.appendText(successMessage + "\n");
                Text successText = new Text(successMessage + "\n");
                successText.setStyle("-fx-fill: green;");
                outputTextFlow.getChildren().add(successText);
            } else {
                //red
                //outputTextArea.appendText(errorMessage + "\n" + errorOutput.toString());
                Text errorText = new Text(successMessage + "\n");
                errorText.setStyle("-fx-fill: red;");
                outputTextFlow.getChildren().add(errorText);
            }
            if (output.length() > 0) {
                text.setText(output.toString());
                outputTextFlow.getChildren().add(text);
                // outputTextArea.appendText(output.toString());
            }
        } catch (IOException | InterruptedException e) {
            text.setText(errorMessage + ": " + e.getMessage() + "\n");
            outputTextFlow.getChildren().add(text);
            // outputTextArea.appendText(errorMessage + ": " + e.getMessage() + "\n");
        }
    }

    private String executeCommandAndGetOutput(String command, File workingDirectory, String errorMessage) {
        try {
            Process process = Runtime.getRuntime().exec(command, null, workingDirectory);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            return output.toString().trim();
        } catch (IOException e) {
            text.setText(errorMessage + ": " + e.getMessage() + "\n");
            outputTextFlow.getChildren().add(text);
            // outputTextArea.appendText(errorMessage + ": " + e.getMessage() + "\n");
        }
        return "";
    }

    private void executeMultipleCommands(String commandText, File workingDirectory, String successMessage, String errorMessage) {
        String[] commands = commandText.split("\n");
        for (String command : commands) {
            if (!command.trim().isEmpty()) {
                executeCommand(command.trim(), workingDirectory, successMessage, errorMessage);
            }
        }
    }
}
