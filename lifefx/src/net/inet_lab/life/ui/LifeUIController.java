package net.inet_lab.life.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.Pane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;

import java.util.Base64;
import java.util.Random;
import java.util.prefs.Preferences;

public class LifeUIController {
    @FXML private Canvas cvs;
    @FXML private Pane pane;
    @FXML private ColumnConstraints left;
    @FXML private RowConstraints row;

    @FXML private Button bExit;
    @FXML private Button bProperties;
    @FXML private Button bReset;
    @FXML private Button bRandomize;
    @FXML private Button bStep;
    @FXML private Button bRun;
    @FXML private TextField tFreq;

    private static final String KEY_F = "f";
    private final  Preferences prefs = Preferences.userNodeForPackage(net.inet_lab.life.ui.LifeUIMain.class);

    private int csize;
    private Properties p;
    private boolean[] F;

    public void resize() {
        cvs.setHeight(pane.getHeight());
        cvs.setWidth(pane.getWidth());

        setCSize();

        redraw ();
    }

    public void initialize () {
        p = new Properties(prefs);
        F = decodeF(prefs.getByteArray(KEY_F,
                encodeF(new boolean[p.nX * p.nY])), p.nX * p.nY);

        cvs.setHeight(row.getPrefHeight());
        cvs.setWidth(left.getPrefWidth());

        setCSize();

        tFreq.setText("0.3");

        tFreq.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                Float.parseFloat(newValue);
            }
            catch (Exception err) {
                Platform.runLater(() -> tFreq.setText(oldValue));
            }
        });

        cvs.setOnMouseClicked(event -> {
            final double ex = event.getX();
            final double ey = event.getY();
            final int x = (int) Math.floor(ex / (csize + 1));
            final int y = (int) Math.floor(ey / (csize + 1));
            final double rx = ex - x * (csize + 1);
            final double ry = ey - y * (csize + 1);
            if (0 <= x && x < p.nX && 0 <= y && y < p.nY && rx > 1 && ry > 1) {
                GraphicsContext gc = cvs.getGraphicsContext2D();
                final int idx = y * p.nX + x;
                F[idx] = !F[idx];
                drawCell(gc, x, y);
            }

            prefs.putByteArray(KEY_F, encodeF(F));
        });

        bExit.setOnAction(event -> {
            Platform.exit();
        });

        bProperties.setOnAction(event -> {
            PropertiesDlg dlg = new PropertiesDlg(p);
            dlg.showAndWait().ifPresent(p1 -> {
                final boolean[] F1 = new boolean[p1.nX * p1.nY];
                for (int x = 0; x < p.nX && x < p1.nX; x ++)
                    for (int y = 0; y < p.nY && y < p1.nY; y ++)
                        F1[y * p1.nX + x] = F[y * p.nX + x];
                F = F1;
                p = p1;
                setCSize();
                p.save(prefs);
                prefs.putByteArray(KEY_F, encodeF(F));
                redraw ();
            });
        });

        bReset.setOnAction(event -> {
            F = new boolean[p.nX * p.nY];
            redraw ();
        });

        bRandomize.setOnAction(event -> {
            Random rand = new Random();
            double f;
            try {
                f = Float.parseFloat(tFreq.getText());
            }
            catch (Exception err) {
                return;
            }

            for (int i = 0; i < p.nX * p.nY; i ++)
                F[i] = rand.nextFloat() < f;
            redraw ();
        });

        bStep.setOnAction(event -> {
            final boolean[] F1 = NativeWrapper.oneStep(F, p.nX, p.nY);
            GraphicsContext gc = cvs.getGraphicsContext2D();
            for (int x = 0; x < p.nX; x++)
                for (int y = 0; y < p.nY; y++)
                    if (F[y * p.nX + x] != F1[y * p.nX + x]) {
                        F[y * p.nX + x] = F1[y * p.nX + x];
                        drawCell(gc, x, y);
                    }
        });

        drawGrid(cvs.getGraphicsContext2D());
    }

    private void redraw ()  {
        final GraphicsContext gc = cvs.getGraphicsContext2D();

        final double W = cvs.getWidth();
        final double H = cvs.getHeight();

        gc.clearRect(0, 0, W, H);
        drawGrid(gc);
    }

    private void drawCell(final GraphicsContext gc, final int x, final int y) {
        final int idx = y * p.nX + x;
        final double r = (csize - 3)/ 2.0;
        if (F[idx]) {
            gc.setFill(Color.RED);
            gc.fillOval(
                    1 + x * (csize + 1) + 0.5 * csize - r,
                    1 + y * (csize + 1) + 0.5 * csize - r,
                    2 * r, 2 * r);
        }
        else
            gc.clearRect(2 + x * (csize + 1), 2 + y * (csize + 1), csize-1, csize-1);
    }

    private void setCSize() {
        final double W = cvs.getWidth();
        final double H = cvs.getHeight();

        csize = (int)Math.floor(Math.min(W/p.nX,H/p.nY))-1;
    }

    private void drawGrid(final GraphicsContext gc) {

        for ( int i = 0; i <= p.nY; i ++ )
            gc.strokeLine (
                    1, 1 + i * (csize + 1),
                    1 + p.nX * (csize + 1), 1 + i * (csize + 1) );
        for ( int i = 0; i <= p.nX; i ++ )
            gc.strokeLine (
		      1 + i * (csize + 1), 1,
		      1 + i * (csize + 1), 1 + p.nY * (csize + 1) );

        for (int x = 0; x < p.nX; x ++)
            for (int y = 0; y < p.nY; y ++)
                if (F[y * p.nX + x])
                    drawCell(gc, x, y);
    }

    static private byte[] encodeF(boolean[] d) {
        final byte[] bytes = new byte[(d.length - 1)/ 8 + 1];
        int n = 0;
        for (int i = 0; i < d.length; i += 8) {
            byte x = (byte) (
                            ((i + 7 < d.length && d[i + 7]) ? 1 : 0)<<7 |
                            ((i + 6 < d.length && d[i + 6]) ? 1 : 0)<<6 |
                            ((i + 5 < d.length && d[i + 5]) ? 1 : 0)<<5 |
                            ((i + 4 < d.length && d[i + 4]) ? 1 : 0)<<4 |
                            ((i + 3 < d.length && d[i + 3]) ? 1 : 0)<<3 |
                            ((i + 2 < d.length && d[i + 2]) ? 1 : 0)<<2 |
                            ((i + 1 < d.length && d[i + 1]) ? 1 : 0)<<1 |
                            (d[i] ? 1 : 0));
            bytes[n ++] = x;
        }
        return bytes;
    }

    static private boolean[] decodeF(final byte[] bytes, final int N) {
        final boolean[] d = new boolean[N];

        for (int n = 0; n < bytes.length; n ++) {
            int v = bytes[n] + 256;
            for (int i = 0; i < 8 && 8*n + i < N; i ++) {
                d[8*n + i] = v % 2 > 0;
                v = (v - v%2) / 2;
            }
        }
        return d;
    }
}
