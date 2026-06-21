package org.mnm.gui;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

/**
 * Allocates a Windows console window at runtime via {@code AllocConsole()} from
 * {@code kernel32.dll}, redirecting {@code stdout} and {@code stderr} to it.
 *
 * <p>Required when the native executable is built with {@code /SUBSYSTEM:WINDOWS}.
 * Errors are written to {@code console-error.log} if console allocation fails.
 */
public class ConsoleAllocator {

    public static void allocConsole() {
        try {
            Linker linker = Linker.nativeLinker();
            SymbolLookup kernel32 = SymbolLookup.libraryLookup("kernel32.dll", Arena.global());

            MethodHandle allocConsole = linker.downcallHandle(
                kernel32.find("AllocConsole").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT)
            );

            allocConsole.invoke();

            PrintStream out = new PrintStream(new FileOutputStream("CONOUT$"), true);
            System.setOut(out);
            System.setErr(out);

        } catch (Throwable e) {
            try {
                PrintStream log = new PrintStream(new FileOutputStream("console-error.log", true));
                e.printStackTrace(log);
            } catch (Exception ignored) {
            }
        }
    }
}
