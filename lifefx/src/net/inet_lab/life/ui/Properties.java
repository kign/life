package net.inet_lab.life.ui;

import java.util.prefs.Preferences;

public class Properties {
    static private final String KEY_NX = "nx";
    static private final String KEY_NY = "ny";

    public final int nX;
    public final int nY;
    public Properties (final int nX, final int nY) {
        this.nX = nX;
        this.nY = nY;
    }
    public Properties(Preferences pref) {
        nX = pref.getInt(KEY_NX, 25);
        nY = pref.getInt(KEY_NY, 25);
    }
    public void save (Preferences pref) {
        pref.putInt(KEY_NX, nX);
        pref.putInt(KEY_NY, nY);
    }
}
