package org.web;

import java.util.logging.Level;
import java.util.logging.Logger;

// Uses the singleton structure
public class WebLogger {

    private static final Logger webLogger = Logger.getLogger(WebLogger.class.getName());

    public static void log(Level level, String info) {
        webLogger.log(level, info);
    }
}