/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.util;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>
 * Helper class for tracking Simplicity and FAT trace.
 * </p>
 * <p>
 * By default, the <code>enable()</code> method enables two log files: one
 * that publishes all messages at the INFO level, and another that publishes all
 * messages at any level. The <code>createHandlers()</code> method can
 * be overridden by subclasses to redefine the specific FileHandler instances
 * constructed and cached by this instance.
 * </p>
 * <p>
 * Errors encountered while handling log files are considered warnings, and no
 * exceptions are thrown. Instead, messages are printed to
 * <code>System.out</code> to warn the user about unexpected log behavior.
 * (Failures of this nature are uncommon).
 * </p>
 * 
 * @author Tim Burns
 */
public class LogHandler {

    protected Set<FileHandler> handlers;

    /**
     * Creates a new set of log handlers.
     * 
     * @param directory
     *            the directory where you want to store log files.
     * @return a set of new Handler instances. never returns null.
     * @throws IOException
     *             if any handler can't be constructed
     */
    protected Set<FileHandler> createHandlers(File directory) throws IOException {
        HashSet<FileHandler> handlers = new HashSet<FileHandler>();
        Formatter formatter = this.createFormatter();

        FileHandler outputHandler = new FileHandler(new File(directory, "output.txt").getAbsolutePath());
        outputHandler.setFormatter(formatter);
        outputHandler.setLevel(Level.INFO);
        handlers.add(outputHandler);

        FileHandler traceHandler = new FileHandler(new File(directory, "trace.txt").getAbsolutePath());
        traceHandler.setFormatter(formatter);
        traceHandler.setLevel(Level.ALL);
        handlers.add(traceHandler);

        return handlers;
    }

    /**
     * Creates a new log formatter.
     * 
     * @return a new Formatter instance
     */
    protected Formatter createFormatter() {
        GenericFormatter formatter = new GenericFormatter();
        formatter.setLogClassName(true);
        formatter.setLogFullClass(false);
        formatter.setClassLength(30);
        formatter.setLogLevel(true);
        formatter.setLogMethodName(true);
        formatter.setMethodLength(30);
        formatter.setLogThreadId(true);
        formatter.setThreadIdLength(3);
        formatter.setLogTimeStamp(true);
        return formatter;
    }

    /**
     * Creates the local logging directory, creates logging handlers, and adds
     * all handlers to the parent logger. These handlers are cached until the
     * next time <code>disable()</code> is called. If <code>enable()</code> is
     * called again before <code>disable()</code>, then <code>disable()</code>
     * will automatically be called.
     * 
     * @param directory
     *            the directory where you want to store log files. If the
     *            directory does not currently exist, it will be created. null
     *            indicates that no log files should be created.
     */
    public void enable(File directory) {
        if (this.handlers != null) {
            this.disable();
        }
        if (directory == null) {
            return;
        }
        try {
            directory.mkdirs();
            Logger logger = Logger.getLogger(""); // get the parent logger
            Set<FileHandler> handlers = this.createHandlers(directory);
            for (Handler handler : handlers) {
                logger.addHandler(handler);
            }
            this.handlers = handlers;
        } catch (Throwable e) {
            System.out.println("Failed to enable log handlers!");
            e.printStackTrace(System.out);
        }
    }

    /**
     * Removes all cached handlers from the parent logger, and closes all cached
     * handlers.
     */
    public void disable() {
        if (this.handlers == null) {
            return; // nothing to do
        }
        try {
            Logger logger = Logger.getLogger(""); // get the parent logger
            for (FileHandler handler : this.handlers) {
                logger.removeHandler(handler);
                this.closeHandler(handler);
            }
        } catch (Throwable e) {
            System.out.println("Failed to disable log handlers!");
            e.printStackTrace(System.out);
        }
        this.handlers.clear();
        this.handlers = null;
    }

    /**
     * Flushes and closes a specific logging handler
     * 
     * @param handler
     *            the handler to disable
     * @throws NullPointerException
     *             if <code>handler</code> is null
     */
    protected void closeHandler(FileHandler handler) throws NullPointerException {
        try {
            handler.close(); // JDK bug ID 4775533: despite calling close(), a .lck file is left behind on Java 1.4.x
        } catch (Throwable e) {
            System.out.println("Failed to close a log handler!");
            e.printStackTrace(System.out);
        }
    }

}
