package com.alexzhelyapov1;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ListView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class TabPaneController {
    @FXML private Button addFolderButton;
    @FXML private Button findDuplicatesButton;
    @FXML private ListView<Directory> selectedFoldersListView;
    private ObservableList<Directory> selectedFolders = FXCollections.observableArrayList();

    private ObservableList<String> duplicateFindMode = FXCollections.observableArrayList(
            DuplicateManager.FIND_BY_NAME,
            DuplicateManager.FIND_BY_HASH
    );
    @FXML private ChoiceBox<String> findModeChoiceBox;
    
    @FXML public void initialize() {
        selectedFoldersListView.setItems(selectedFolders);
        findModeChoiceBox.setItems(duplicateFindMode);
        findModeChoiceBox.setValue(DuplicateManager.FIND_BY_HASH);
    }

    @FXML private void addFolder(ActionEvent event) {
        System.out.println("Add button pressed.");
        try {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Выберите папку");
            Stage stage = (Stage) addFolderButton.getScene().getWindow();
            File selectedDirectory = directoryChooser.showDialog(stage);
            if (selectedDirectory != null) {
                Directory new_directory = new Directory(selectedDirectory.getAbsolutePath());
                new_directory.scan();
                System.out.println("Directory " + new_directory.getPath() + " scanned.");
                selectedFolders.add(new_directory);
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            System.out.println("Can't create dir in addFolder.");
        }
    }
    @FXML private void findDuplicates(ActionEvent event) {
        System.out.println("Find button pressed.");
        DuplicateManager manager = new DuplicateManager(selectedFolders);
        try {
            DuplicateManager.printDuplicatesHashMap(manager.duplicated(findModeChoiceBox.getValue()));
        } catch (Exception e) {
            System.out.println("ERROR in duplicate manager.");
        }
    }
}
