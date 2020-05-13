package net.inet_lab.life.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.Pane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Random;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LifeUIController {
    @FXML private Canvas cvs;
    @FXML private Pane pane;
    @FXML private ColumnConstraints ccLeft;
    @FXML private RowConstraints ccRow;

    @FXML private Button bStep;
    @FXML private Button bRun;
    @FXML private CheckBox cbSkip;
    @FXML private Button bSave;
    @FXML private Button bRead;
    @FXML private Button bReset;
    @FXML private Button bRandomize;
    @FXML private TextField tFreq;
    @FXML private Button bPreferences;
    @FXML private Button bExit;

    @FXML private Label lStatus;

    private static final String KEY_F = "f";
    private final  Preferences prefs = Preferences.userNodeForPackage(net.inet_lab.life.ui.LifeUIMain.class);

    private int csize;
    private Properties p;
    private boolean[] F;
    private Stage primaryStage;
    private FileChooser fileChooser;

    public void setStage(final Stage stage) {
        primaryStage = stage;
    }

    public void resize() {
        cvs.setHeight(pane.getHeight());
        cvs.setWidth(pane.getWidth());

        setCSize();

        redraw ();
    }

    public void initialize () {
        lStatus.setText("Initializing");

        fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Life Text", "*.life"),
                new FileChooser.ExtensionFilter("Life Binary", "*.blife"));

        p = new Properties(prefs);
        F = decodeF(prefs.getByteArray(KEY_F,
                encodeF(new boolean[p.nX * p.nY])), p.nX * p.nY);

        cvs.setHeight(ccRow.getPrefHeight());
        cvs.setWidth(ccLeft.getPrefWidth());

        System.err.println("Initial canvas size: " + cvs.getWidth() + " x " + cvs.getHeight());

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

        bExit.setOnAction(event -> Platform.exit());

        bPreferences.setOnAction(event -> {
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
            update(F1);
        });

        bRun.setOnAction(event -> NativeWrapper.run(p.nX, p.nY, cbSkip.isSelected()?0.1:(-1), F, (iter, count, fin, F1) -> {
            System.err.println("[" + iter + "] C:" + count + " F:" + fin);
            lStatus.setText(String.format("Iteration %d density %.3f", iter, 1.0*count/p.nX/p.nY));
            update(F1);
            return 0;
        }));

        bSave.setOnAction(event -> {
            fileChooser.setTitle("Save current board");
            fileChooser.setInitialFileName("myboard");
            try {
                //Actually displays the save dialog - blocking
                final File file = fileChooser.showSaveDialog(primaryStage);
                if (file != null) {
                    final File dir = file.getParentFile();//gets the selected directory
                    //update the file chooser directory to user selected, so the choice is "remembered"
                    fileChooser.setInitialDirectory(dir);
                    System.err.println("Saving to " + file);
                    final boolean bin = file.getName().endsWith(".blife");
                    (new FileOutputStream(file)).write(bytes_save(bin));
                }
            } catch (Exception err) {
                System.err.println("Could not save file " + err);
            }
        });

        bRead.setOnAction(event -> {
            fileChooser.setTitle("Open board");
            try {
                final File file = fileChooser.showOpenDialog(primaryStage);
                if (file != null) {
                    if (!bytes_read(Files.readAllBytes(file.toPath()), (X, Y, F1) -> {
                        if (p.nX == X && p.nY == Y) {
                            GraphicsContext gc = cvs.getGraphicsContext2D();
                            for (int x = 0; x < X; x++)
                                for (int y = 0; y < Y; y++) {
                                    F[y * X + x] = F1[y * X + x];
                                    drawCell(gc, x, y);
                                }
                        }
                        else {
                            p = new Properties(X, Y);
                            F = F1;
                            setCSize();
                            p.save(prefs);
                            redraw ();
                        }
                    })) {
                        System.err.println("Improperly formatted file " + file);
                    }
                }
            } catch (Exception err) {
                System.err.println("Could not read file " + err);
            }
        });

        drawGrid(cvs.getGraphicsContext2D());
    }

    private void update (final boolean[] F1) {
        GraphicsContext gc = cvs.getGraphicsContext2D();
        int cng = 0;
        for (int x = 0; x < p.nX; x++)
            for (int y = 0; y < p.nY; y++)
                if (F[y * p.nX + x] != F1[y * p.nX + x]) {
                    F[y * p.nX + x] = F1[y * p.nX + x];
                    drawCell(gc, x, y);
                    cng ++;
                }
        System.err.println("Updated " + cng + "cells");
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

    static private final byte[] prefix = new byte[]{0x02, 0x57, (byte) 0xAB};
    private byte[] bytes_save (boolean bin) {
        if (bin) {
            final byte[] size = (p.nX + "." + p.nY + ".").getBytes();
            final byte[] cont = encodeF(F);

            final byte[] res = new byte[prefix.length + size.length + cont.length];
            System.arraycopy(prefix, 0, res, 0, prefix.length);
            System.arraycopy(size, 0, res, prefix.length, size.length);
            System.arraycopy(cont, 0, res, prefix.length + size.length, cont.length);

            return res;
        }
        else {
            StringBuilder res = new StringBuilder();

            for (int y = 0; y < p.nY; y ++) {
                for (int x = 0; x < p.nX; x ++)
                    res.append(F[y * p.nX + x]?'x':'.');
                res.append('\n');
            }

            return res.toString().getBytes();
        }
    }

    private interface _bytes_read {
        void read_successfully(int X, int Y, boolean[] F1);
    }

    private boolean bytes_read(final byte[] bytes, _bytes_read cb) {
        int X=0, Y=0;
        boolean[] F1=null;

        int i;
        for (i = 0; i < bytes.length && i < prefix.length && bytes[i] == prefix[i]; i ++);
        if (i == prefix.length) {
            final Pattern p = Pattern.compile("(\\d+)\\.(\\d+)\\.");

            boolean found = false;
            for (int j = i + 1; j < i + 100 && !found; j ++) {
                try {
                    final String xy = new String(Arrays.copyOfRange(bytes, i, j));
                    Matcher m = p.matcher(xy);
                    if (m.lookingAt()) {
                        X = Integer.parseInt(m.group(1));
                        Y = Integer.parseInt(m.group(2));
                        if (X < 2 || Y < 2 || X*Y > 1_000_000_000)
                            return false;
                        F1 = decodeF(Arrays.copyOfRange(bytes, j, bytes.length), X * Y);
                        found = true;
                    }
                } catch (Exception ignored) {}
            }
            if (!found)
                return false;
        }
        else {
            try {
                final String[] rows = (new String(bytes)).split("\n");
                X = rows[0].length();
                Y = rows.length;
                F1 = new boolean[X * Y];
                for (int y = 0; y < Y; y ++) {
                    if (rows[y].length() != X)
                        return false;
                    for (int x = 0; x < X; x ++)
                        F1[y * p.nX + x] = rows[y].charAt(x) != '.' && rows[y].charAt(x) != ' ';
                }
            } catch (Exception err) {
                return false;
            }
        }
        cb.read_successfully(X, Y, F1);
        return true;
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
