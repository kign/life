package net.inet_lab.life.ui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
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
    @FXML private SplitMenuButton sbRun;
    @FXML private MenuItem miRun;
    @FXML private MenuItem miRunSlow;
    @FXML private MenuItem miRunFast;
    @FXML private Button bSave;
    @FXML private Button bRead;
    @FXML private Button bReset;
    @FXML private Button bRandomize;
    @FXML private TextField tFreq;
    @FXML private Button bPreferences;
    @FXML private Button bExit;

    @FXML private Label lStatus;

    private final Object lock = new Object();

    private static final String KEY_F = "f";
    private static final String KEY_DIR = "D";
    private static final String KEY_RM = "R";

    private final Preferences prefs = Preferences.userNodeForPackage(net.inet_lab.life.ui.LifeUIMain.class);

    private int csize;
    private Properties p;
    private boolean[] F;
    private Stage primaryStage;
    private FileChooser fileChooser;

    private int step_count;

    private MenuItem runMode;

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
        fileChooser.setInitialDirectory(
                new File(
                        new String(prefs.getByteArray(
                                KEY_DIR,
                                System.getProperty("user.home").getBytes()))));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Life Text", "*.life"),
                new FileChooser.ExtensionFilter("Life Binary", "*.blife"));

        p = new Properties(prefs);
        F = decodeF(prefs.getByteArray(KEY_F,
                encodeF(new boolean[p.nX * p.nY])), p.nX * p.nY);

        final int rm = prefs.getInt(KEY_RM, 0);
        Platform.runLater(() -> ((rm == -1)?miRunSlow:((rm == 1)?miRunFast:miRun)).fire ());

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
            userInitiatedChange();
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
                userInitiatedChange();
            });
        });

        bReset.setOnAction(event -> {
            F = new boolean[p.nX * p.nY];
            redraw ();
            userInitiatedChange();
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
            userInitiatedChange();
        });

        bStep.setOnAction(event -> {
            final boolean[] F1 = NativeWrapper.oneStep(F, p.nX, p.nY);
            //System.err.println("(Step) " + ", F1 = " + F1 + ", 63=" + F1[63] + ", 64=" + F1[64] + ", 66=" + F1[66] + ", 67=" + F1[67]);
            update(F1);
            step_count ++;
            lStatus.setText("Step " + step_count);
        });

        sbRun.setOnAction(event ->
                         new Thread(() ->
                         NativeWrapper.run(p.nX, p.nY, (runMode == miRunFast)?p.skip:(-1), F, (iter, count, fin, _F1) -> {
            //System.err.println("[" + iter + "] C:" + count + " F:" + fin);
            final boolean[] F1 = Arrays.copyOfRange(_F1, 0, _F1.length);

            Platform.runLater(() -> {
                lStatus.setText(String.format("Iteration %d density %.3f%s", iter, 1.0 * count / p.nX / p.nY, (fin==1)?"; stopped":""));
                update(F1);
                synchronized (lock) {
                    lock.notifyAll();
                }
            });

            try {
                synchronized (lock) {
                    if (runMode == miRunSlow)
                        Thread.sleep((int)(p.delay * 1000));
                    lock.wait(10000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return 0;
        })).start());

        final EventHandler<ActionEvent> runSelectorCb = event -> {
            var target = event.getTarget();
            runMode = (MenuItem) target;
            sbRun.setText(runMode.getText());
            prefs.putInt(KEY_RM, (runMode == miRunSlow)?-1:((runMode == miRunFast)?1:0));
        };

        miRun.setOnAction(runSelectorCb);
        miRunSlow.setOnAction(runSelectorCb);
        miRunFast.setOnAction(runSelectorCb);

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
                    prefs.putByteArray(KEY_DIR, dir.toString().getBytes());
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
                    else {
                        userInitiatedChange();
                        final File dir = file.getParentFile();//gets the selected directory
                        //update the file chooser directory to user selected, so the choice is "remembered"
                        fileChooser.setInitialDirectory(dir);
                        prefs.putByteArray(KEY_DIR, dir.toString().getBytes());
                    }
                }
            } catch (Exception err) {
                System.err.println("Could not read file " + err);
            }
        });

        drawGrid(cvs.getGraphicsContext2D());

        lStatus.setText("Click on board or use control buttons");
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
        //System.err.println("Updated " + cng + " cells");
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

    private void userInitiatedChange() {
        step_count = 0;
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
        //noinspection StatementWithEmptyBody
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
                        F1[y * X + x] = rows[y].charAt(x) != '.' && rows[y].charAt(x) != ' ';
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
