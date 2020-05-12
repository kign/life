package net.inet_lab.life.ui;

public class NativeWrapper {
    static {
        System.loadLibrary("native");
    }

    public static boolean[] oneStep (boolean[] F, int X, int Y) {
        int x = 1 + 25;
        boolean[] F1 = new boolean[X*Y];
        _oneStep (X, Y, F, F1);
        return F1;
    }

    private static native void _oneStep (int X, int Y, boolean[] F, boolean[] F1);
}
