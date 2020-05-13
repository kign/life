package net.inet_lab.life.ui;

import java.util.prefs.Preferences;

public class Properties {
    static private final String KEY_NX = "nx";
    static private final String KEY_NY = "ny";

    public final int nX;
    public final int nY;
    public final double skip;
    public final double delay;

    public Properties (final int nX, final int nY) {
        this.nX = nX;
        this.nY = nY;
        this.skip = 0.1;
        this.delay = 0.1;
    }

    public Properties(Preferences pref) {
        nX = pref.getInt(KEY_NX, 25);
        nY = pref.getInt(KEY_NY, 25);
        this.skip = 0.1;
        this.delay = 0.1;
    }

    public void save (Preferences pref) {
        pref.putInt(KEY_NX, nX);
        pref.putInt(KEY_NY, nY);
    }
}
