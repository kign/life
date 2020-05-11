package net.inet_lab.life.ui;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

public class PropertiesDlg extends Dialog<Properties> {
    final TextField tfX;
    final TextField tfY;

    public PropertiesDlg(final Properties init) {
        setTitle("Properties");
        setHeaderText("You can configure properties");

        // ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Create the username and password labels and fields.
        final GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        tfX = new TextField();
        tfX.setPromptText("X dimension");
        tfX.setText(String.valueOf(init.nX));
        tfY = new TextField();
        tfY.setPromptText("Y dimension");
        tfY.setText(String.valueOf(init.nY));

        grid.add(new Label("X dim:"), 0, 0);
        grid.add(tfX, 1, 0);
        grid.add(new Label("Y dim:"), 0, 1);
        grid.add(tfY, 1, 1);

        final Node okButton = getDialogPane().lookupButton(ButtonType.OK);

        // no need to enable ok button initially, haven't changed anything yet
        okButton.setDisable(true);

        // Do some validation
        ChangeListener<String> verify = (observable, oldValue, newValue) -> {
                    try {
                        getProp();
                        okButton.setDisable(false);
                    }
                    catch (Exception e) {
                        okButton.setDisable(true);
                    }
                };
        tfX.textProperty().addListener(verify);
        tfY.textProperty().addListener(verify);

        getDialogPane().setContent(grid);

        // Request focus on the username field by default.
        Platform.runLater(tfX::requestFocus);

        // Convert the result to a username-password-pair when the login button is clicked.
        setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return getProp ();
            }
            return null;
        });
    }

    private Properties getProp () {
        return new Properties(Integer.parseInt(tfX.getText()), Integer.parseInt(tfY.getText()));
    }

}
