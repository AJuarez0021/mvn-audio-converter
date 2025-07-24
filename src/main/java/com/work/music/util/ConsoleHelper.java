package com.work.music.util;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 *
 * @author ajuar
 */
public final class ConsoleHelper {

    private static PrintStream utf8Out;

    static {
        try {
            utf8Out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        } catch (Exception e) {
            utf8Out = System.out;
        }
    }

    public static void print(String text) {
        utf8Out.print(text);
    }

    public static void println(String text) {
        utf8Out.println(text);
    }

    public static void println() {
        utf8Out.println();
    }

    public static void printf(String format, Object... args) {
        utf8Out.printf(format, args);
    }
}
