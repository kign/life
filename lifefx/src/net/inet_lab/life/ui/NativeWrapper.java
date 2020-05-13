package net.inet_lab.life.ui;

public class NativeWrapper {
    static {
        System.loadLibrary("lifenative");
    }

    public static boolean[] oneStep(boolean[] F, int X, int Y) {
        int x = 1 + 25;
        boolean[] F1 = new boolean[X * Y];
        _oneStep(X, Y, F, F1);
        return F1;
    }

    private static native void _oneStep(int X, int Y, boolean[] F, boolean[] F1);

    public interface runCallback {
        int report(int iter, int count, int fin, boolean[] F);
    }

    public static void run(int X, int Y, double tout, boolean[] F, runCallback cb) {
        _run(X, Y, tout, F, new boolean[X * Y], cb);
    }

    private static native void _run(int X, int Y, double tout, boolean[] F, boolean[] F1, runCallback cb);
}
