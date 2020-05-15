package net.inet_lab.life.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
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
        var img = new Image(getClass().getResourceAsStream("Icon.png"));
        primaryStage.getIcons().add(img);

        // Seems like the only "correct" way to set Dock Image is via packaging
        // (so not during debugging)
        // However, here is a hackish way to do it directly from Java code (via reflection)
        // Not using it here (among other problems, it'll inflate size of packaged app)
        // but keep info here just in case
        // Utils.setDockIconImage(img);


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
