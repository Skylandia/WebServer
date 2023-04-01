package org.web;

import java.util.Date;

// Uses the singleton structure
public class WebLogger {

    private static int bufferCount = 0;

    public static void log(String info) {
        doBuffer();
        System.out.println(new Date() + " | " + info);
    }
    public static void error(String info) {
        doBuffer();
        System.err.println(new Date() + " | " + info);
    }

    public static void buffer() {
        buffer(1);
    }
    public static void buffer(int count) {
        bufferCount += count;
    }

    private static void doBuffer() {
        for (int i = 0; i < bufferCount; i++) {
            System.out.println();
        }
        bufferCount = 0;
    }
}