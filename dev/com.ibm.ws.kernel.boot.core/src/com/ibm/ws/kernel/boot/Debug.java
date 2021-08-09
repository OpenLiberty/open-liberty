/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import com.ibm.ws.kernel.boot.logging.TextFileOutputStreamFactory;

/**
 * Lightweight capture of critical diagnostic information for launcher commands.
 */
public class Debug {
    private static PrintStream out = System.err;
    private static File openedFile;

    /**
     * Create the debug log at the specified location if possible. If this call
     * fails, {@link #isOpen} returns false and diagnostic information will be
     * printed to System.err.
     */
    static void open(File dir, String fileName) {
        if (dir.mkdirs() || dir.isDirectory()) {
            File file = new File(dir, fileName);
            try {
                out = new PrintStream(TextFileOutputStreamFactory.createOutputStream(file), true, "UTF-8");
                openedFile = file;
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Returns true if {@link #open} has been called successfully.
     */
    static boolean isOpen() {
        return openedFile != null;
    }

    /**
     * Close the debug log opened by {@link #open} and optionally delete it.
     * This method has no effect if {@link #open} was not called successfully.
     * 
     * @param delete true if the debug log should be deleted after closing
     */
    static void close(boolean delete) {
        if (openedFile != null) {
            out.close();
            out = System.err;

            if (delete) {
                openedFile.delete();
            }
        }
    }

    public static void println() {
        out.println();
    }

    public static void println(Object o) {
        out.println(o);
    }

    public static void printStackTrace(Throwable t) {
        t.printStackTrace(out);
    }
}
