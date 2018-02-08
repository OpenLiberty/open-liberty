/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.utils;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import com.ibm.websphere.logging.WsLevel;
import com.ibm.ws.logging.internal.impl.LoggingConstants;

/**
 * Utility to class for all common formatting requirements on log data.
 * Required: For both message & trace log data.
 */
public class LogFormatUtils {

    static final String ENTRY = "Entry ";
    static final String EXIT = "Exit ";

    enum LevelFormat {
        FATAL(WsLevel.FATAL, "F"),
        ERROR(WsLevel.ERROR, "E"),
        WARNING(Level.WARNING, "W"),
        AUDIT(WsLevel.AUDIT, "A"),
        INFO(Level.INFO, "I"),
        CONFIG(Level.CONFIG, "C"),
        DETAIL(WsLevel.DETAIL, "D"),
        EVENT(WsLevel.FINE, "1"),
        FINE(Level.FINE, "1"),
        FINER(Level.FINER, "2"),
        FINEST(Level.FINEST, "3");

        final Level level;
        final int id;
        final String marker;

        LevelFormat(Level l, String s) {
            level = l;
            id = l.intValue();
            marker = s;
        }

        public static LevelFormat findFormat(Level level) {
            int levelId = level.intValue();
            for (LevelFormat f : LevelFormat.values()) {
                if (level == f.level)
                    return f;
                else if (levelId == f.id) {
                    return f;
                }
            }
            return null;
        }
    }

    public static String mapLevelToType(LogRecord logRecord) {
        Level level = logRecord.getLevel();
        if (level == null)
            return "Z";

        // err & out are in the mix
        if (level.getName() == LoggingConstants.SYSTEM_ERR)
            return "R";
        if (level.getName() == LoggingConstants.SYSTEM_OUT)
            return "O";

        LevelFormat f = LevelFormat.findFormat(level);
        if (f == null)
            return "Z";

        if (f.level == Level.FINER) {
            // Check for Tr.entry/.exit, which use the ENTRY/EXIT constants, or
            // Logger.entering/.exiting, which use "ENTRY", "ENTRY {0}",
            // "ENTRY {0} {1}", etc. and "RETURN" or "RETURN {0}".
            String message = logRecord.getMessage();
            if (message != null) {
                if (message.equals(ENTRY) || message.startsWith("ENTRY"))
                    return ">";
                if (message.equals(EXIT) || message.startsWith("RETURN"))
                    return "<";
            }
        }

        return f.marker;
    }

    public static String mapLevelToRawType(LogRecord logRecord) {

        Level level = logRecord.getLevel();
        if (level == null)
            return "***";

        LevelFormat f = LevelFormat.findFormat(level);
        if (f == null)
            return "***";

        if (f.level == Level.FINER) {
            // Check for Tr.entry/.exit, which use the ENTRY/EXIT constants, or
            // Logger.entering/.exiting, which use "ENTRY", "ENTRY {0}",
            // "ENTRY {0} {1}", etc. and "RETURN" or "RETURN {0}".
            String message = logRecord.getMessage();
            if (message != null) {
                if (message.equals(ENTRY) || message.startsWith("ENTRY"))
                    return "ENTRY";
                if (message.equals(EXIT) || message.startsWith("RETURN"))
                    return "EXIT";
            }
        }

        return level.getName();
    }

}
