package org.openjfx;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class AuthSceneController {

    @FXML
    private Label heading;

    @FXML
    private TextField usernameField;

    @FXML
    private TextField fullnameField;

    @FXML
    private TextField emailField;

    @FXML
    private AnchorPane rootPane;

    @FXML
    private Button saveButton;

    Alert a = new Alert(AlertType.NONE);

    String username = new String();
    String fullName = new String();
    //String email = new String();

    @FXML
    void saveButtonAction(ActionEvent event) {

        username = usernameField.getText().trim();
        Globals.username = username;
        fullName = fullnameField.getText();
        Globals.fullName = fullName;
        //email = emailField.getText().trim();

        //System.out.println(username);

        if(username.isEmpty()){

            a.setAlertType(AlertType.ERROR);
            a.setContentText("GitHub Username cannot be empty");
            a.show();
            return;
        }
        try{
            JsonArray repos = fetchRepos(username);
            if (repos.size() > 0)   {
                System.out.println(username);
                showRepos(repos);
            }
            else{
                a.setAlertType(AlertType.ERROR);
                a.setContentText("No repos available for the given GitHub username");
                a.show();
            }
        }catch(Exception e){
            e.printStackTrace();
            a.setAlertType(AlertType.ERROR);
            a.setContentText("Error fetching repositories");
            a.show();
        }
    }

   private JsonArray fetchRepos(String username) throws Exception{

            // Using Http Client to fetch the repositories
            String apiUrl = "https://api.github.com/users/" + username + "/repos";
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(apiUrl)).header("Accept", "application/vnd.github.v3+json").build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return JsonParser.parseString(response.body()).getAsJsonArray();
   }

   @SuppressWarnings("rawtypes")
private void showRepos(JsonArray repos){
        System.out.println("Repositories fetched");

        ObservableList names = FXCollections.observableArrayList();

        Stage mainWindow = (Stage) heading.getScene().getWindow();

        for (JsonElement repo : repos)  {
            JsonObject repoObject = repo.getAsJsonObject();
            String repoName = repoObject.get("name").getAsString();
            //System.out.println(repoName);
            names.add(repoName);
        }

        Globals.repoNames = names;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("RepoScene.fxml"));
            Parent repoSceneRoot = loader.load();

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
}
