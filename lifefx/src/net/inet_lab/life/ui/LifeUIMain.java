package net.inet_lab.life.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class LifeUIMain extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        final FXMLLoader loader = new FXMLLoader();

        // Weird getResource() won't work, yields NULL controller
        final GridPane root = loader.load(getClass().getResourceAsStream("lifeui.fxml"));
        final LifeUIController controller = loader.getController();
        controller.setStage(primaryStage);

        primaryStage.setTitle("LIFE");
        final Scene scene = new Scene(root, root.getPrefWidth(), root.getPrefHeight());
        scene.widthProperty().addListener((__, oldWidth, newWidth) -> controller.resize());
        scene.heightProperty().addListener((__, oldHeight, newHeight) -> controller.resize());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
