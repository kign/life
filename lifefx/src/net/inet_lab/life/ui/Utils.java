package net.inet_lab.life.ui;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.image.BufferedImage;
import java.lang.reflect.Method;

public class Utils {
    public static void setDockIconImage(final Image img) {
        try {
            // https://gist.github.com/bchapuis/1562406
            Class eawtApp = Class.forName("com.apple.eawt.Application");
            Method getApplication = eawtApp.getMethod("getApplication");
            Object application = getApplication.invoke(eawtApp);
            BufferedImage awtImage = SwingFXUtils.fromFXImage(img, null);
            Method setDockIconImage = eawtApp.getMethod("setDockIconImage", java.awt.Image.class);
            setDockIconImage.invoke(application, awtImage);
        } catch (Exception err) {
            System.err.println("Can't invoke setDockIconImage: " + err);
        }
    }

}
