/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.utils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * This should only be used by something that doesn't have access to normal logging.
 * By default, this will write to elog.txt in the JVM's current working directory.
 */
public class EmergencyLog {
    /**
     * Activate debug* methods if -Dcom.ibm.ws.logging.edebug=true is set on the JVM.
     */
    public static final boolean EDEBUG = Boolean.getBoolean("com.ibm.ws.logging.edebug");

    /**
     * Set this to blank to send to System.err instead.
     */
    public static final String EFILE = System.getProperty("com.ibm.ws.logging.efile", "elog.txt");

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private static PrintStream out;

    private static synchronized void ensureStream() {
        if (out == null) {
            if (EFILE != null && EFILE.length() > 0) {
                try {
                    out = new PrintStream(new FileOutputStream(EFILE, true), true);
                } catch (FileNotFoundException e) {
                    throw new Error(e);
                }
            } else {
                out = System.err;
            }
        }
    }

    /**
     * Print specified data to the emergency log.
     *
     * @param clazz The class source.
     * @param method The method source.
     * @param objects A list of objects to trace.
     */
    public static void print(final Class<?> clazz, final String method, final Object... objects) {
        final StringBuilder sb = new StringBuilder();
        sb.append("ELOG: [");
        sb.append(dateFormat.format(new Date()));
        sb.append("] ");
        sb.append(clazz.getSimpleName());
        sb.append('.');
        sb.append(method);
        if (objects != null && objects.length > 0) {
            sb.append(": ");
            for (int i = 0; i < objects.length; i++) {
                if (i > 0) {
                    sb.append("; ");
                }
                sb.append(objects[i]);
            }
        }

        ensureStream();
        out.println(sb);
        out.flush();
    }

    /**
     * Print specified data with an Entry prefix to the emergency log.
     *
     * @param clazz The class source.
     * @param method The method source.
     * @param objects A list of objects to trace.
     */
    public static void printEntry(final Class<?> clazz, final String method, final Object... objects) {
        printPrefixed(clazz, method, "> Entry", objects);
    }

    /**
     * Print specified data with an Exit prefix to the emergency log.
     *
     * @param clazz The class source.
     * @param method The method source.
     * @param objects A list of objects to trace.
     */
    public static void printExit(final Class<?> clazz, final String method, final Object... objects) {
        printPrefixed(clazz, method, "< Exit", objects);
    }

    private static void printPrefixed(final Class<?> clazz, final String method, final String prefix, final Object... objects) {
        if (objects != null && objects.length > 0) {
            final Object[] allObjects = new Object[objects.length + 1];
            allObjects[0] = prefix;
            for (int i = 0; i < objects.length; i++) {
                allObjects[i + 1] = objects[i];
            }
            print(clazz, method, allObjects);
        } else {
            print(clazz, method, prefix);
        }
    }

    /**
     * Print specified data to the emergency log if -Dcom.ibm.ws.logging.edebug=true.
     *
     * @param clazz The class source.
     * @param method The method source.
     * @param objects A list of objects to trace.
     */
    public static void debug(final Class<?> clazz, final String method, final Object... objects) {
        if (EDEBUG) {
            print(clazz, method, objects);
        }
    }

    /**
     * Print specified data with an Entry prefix to the emergency log if -Dcom.ibm.ws.logging.edebug=true.
     *
     * @param clazz The class source.
     * @param method The method source.
     * @param objects A list of objects to trace.
     */
    public static void debugEntry(final Class<?> clazz, final String method, final Object... objects) {
        if (EDEBUG) {
            printEntry(clazz, method, objects);
        }
    }

    /**
     * Print specified data with an Exit prefix to the emergency log if -Dcom.ibm.ws.logging.edebug=true.
     *
     * @param clazz The class source.
     * @param method The method source.
     * @param objects A list of objects to trace.
     */
    public static void debugExit(final Class<?> clazz, final String method, final Object... objects) {
        if (EDEBUG) {
            printExit(clazz, method, objects);
        }
    }

    /**
     * Get the current thread stack as a String.
     *
     * @return Current thread stack.
     */
    public static String getCurrentStack() {
        return Arrays.toString(Thread.currentThread().getStackTrace());
    }
}
