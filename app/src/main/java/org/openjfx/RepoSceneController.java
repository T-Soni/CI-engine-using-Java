package org.openjfx;

import java.io.IOException;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.ComboBoxListCell;
import javafx.stage.Stage;


public class RepoSceneController {

    @FXML
    private Button backbutton;

    @FXML
    private Label heading;

    @FXML
    private Label WelcomeLabel;

    @FXML
    private Button nextbutton;

    @FXML
    private ListView<String> repoListView;

    public void initialize(){
        WelcomeLabel.setText("Welcome " + Globals.fullName + "!");
        populateRepoList(Globals.repoNames);
    }

    public void populateRepoList(ObservableList<String> repoNames){
        repoListView.setItems(repoNames);
        repoListView.setCellFactory(ComboBoxListCell.forListView(repoNames));
        repoListView.getSelectionModel().selectedItemProperty().addListener(
            new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> ov, String old_val, String new_val){
                                //System.out.println(new_val);
                                Globals.selectedRepoName = new_val;
                        }
            
            }
        );
        
    }

    @FXML
    void backAction(ActionEvent event) {
        //System.out.println("Back button clicked");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("AuthScene.fxml"));
            Parent nextSceneRoot = loader.load();
            Stage mainWindow = (Stage)heading.getScene().getWindow();
            AuthSceneController controller = loader.getController();
            Scene nextScene = new Scene(nextSceneRoot);
            mainWindow.setScene(nextScene);
            mainWindow.setTitle("");;
            mainWindow.show();

        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }

    @FXML
    void nextAction(ActionEvent event) throws IOException {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("scene.fxml"));
            Parent nextSceneRoot = loader.load();
            Stage mainWindow = (Stage)heading.getScene().getWindow();
            FXMLController controller = loader.getController();
            controller.initializeController();
            controller.initializeRepoTextField();
            Scene nextScene = new Scene(nextSceneRoot);
            mainWindow.setScene(nextScene);
            mainWindow.setTitle("Clone-Build");
            mainWindow.show();

        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
        
    }

}
