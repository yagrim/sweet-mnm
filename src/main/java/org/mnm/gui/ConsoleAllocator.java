package org.mnm.gui;

class ConsoleAllocator {

    static {
        System.loadLibrary("kernel32");
    }

    private static native boolean AllocConsole();

    private static native boolean AttachConsole(int dwProcessId);

    static void attachConsole() {
        boolean attached = AttachConsole(-1); // attach to parent if exists
        if (!attached) {
            AllocConsole();
        }
        try {
            System.setOut(new java.io.PrintStream(
                new java.io.FileOutputStream("CONOUT$"), true));
            System.setErr(new java.io.PrintStream(
                new java.io.FileOutputStream("CONOUT$"), true));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
