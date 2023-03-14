/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.build.update.util;

/**
 * Very basic time keeping logger.
 */
public class Logger {
    public Logger() {
        this(null, null);
    }

    public Logger(Logger parent) {
        this(parent, null);
    }

    public Logger(String prefix) {
        this(null, prefix);
    }

    public Logger(Logger parent, String prefix) {
        this.parent = parent;
        this.prefix = prefix;
        this.markMs = getTime();
    }

    //

    private final Logger parent;

    public Logger getParent() {
        return parent;
    }

    private final String prefix;

    public String getPrefix() {
        return prefix;
    }

    // Basic logging is always done in the parent.

    public void log(String m, String text) {
        if (prefix != null) {
            m = prefix + "." + m;
        }

        if (parent != null) {
            parent.log(m, text);
        } else {
            System.out.println(m + ": " + text);
        }
    }

    //

    private static long getTime() {
        return System.currentTimeMillis();
    }

    private static String formatTime(long timeMs) {
        return Long.toString(timeMs);
    }

    private long markMs;

    private long mark() {
        long oldMarkMs = markMs;
        markMs = getTime();
        return (markMs - oldMarkMs);
    }

    //

    // Time keeping is always done in the immediate logger.  That
    // enables a hierarchy of loggers, each with their own time
    // keeping, which share basic logging.

    public long logMark(String m, String text) {
        long diff = mark();
        log(m, text);
        return diff;
    }

    public long logTime(String m, String text) {
        long diff = mark();
        text += " [ " + formatTime(diff) + " (ms) ]";
        log(m, text);
        return diff;
    }
}
