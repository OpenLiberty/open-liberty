/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package cdi.beans.v2.log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * General purpose logger.
 */
public class Log implements Serializable {
    //
    private static final long serialVersionUID = 1L;

    //
    public Log() {
        this.lines = new ArrayList<String>();
    }

    //
    private class LineGuard {
        // EMPTY
    }

    private final LineGuard lineGuard = new LineGuard();
    private List<String> lines;

    public static final boolean DO_CLEAR_LINES = true;
    public static final boolean DO_NOT_CLEAR_LINES = true;

    public List<String> getLines(boolean clearLines) {
        synchronized (lineGuard) {
            List<String> returnLines;

            if (clearLines) {
                returnLines = lines;
                lines = new ArrayList<String>();
            } else {
                returnLines = new ArrayList<String>(lines);
            }

            return returnLines;
        }
    }

    public void clear() {
        synchronized (lineGuard) {
            lines.clear();
        }
    }

    public void addLine(String line) {
        synchronized (lineGuard) {
            lines.add(line);
        }
    }

    public String getText() {
        synchronized (lineGuard) {
            int numLines = lines.size();

            int textLength = 0;
            for (int lineNo = 0; lineNo < numLines; lineNo++) {
                textLength += lines.get(lineNo).length() + 2;
            }

            StringBuilder textBuilder = new StringBuilder(textLength);

            for (int lineNo = 0; lineNo < numLines; lineNo++) {
                textBuilder.append(lines.get(lineNo));
                textBuilder.append("/r/n");
            }

            return textBuilder.toString();
        }
    }

    //

    public static long getSystemTime() {
        return System.currentTimeMillis();
    }

    public static long initialTime = getSystemTime();

    public static long getInitialTime() {
        return initialTime;
    }

    //

    public String log(String className, String methodName, String text) {
        long thisTime = getSystemTime();
        long thisTimeDelta = thisTime - getInitialTime();

        // @formatter:off
        String line =
            "[ " + thisTime + " ] [ " + thisTimeDelta + " ]" +
            " [ " + Thread.currentThread().getId() + " ]" +
            " [ " + className + ": " + methodName + ": " + text;
        // @formatter:on

        addLine(line);

        return line;
    }
}
