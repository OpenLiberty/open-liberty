/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.utility;

import java.io.File;
import java.io.PrintStream;

/**
 *
 */
public interface IFileUtility {

    /**
     * Returns the WLP servers directory for the utility.
     * Always returns with a trailing slash.
     *
     * @return The fully qualified path to the servers directory, ending in a trailing slash
     */
    public abstract String getServersDirectory();

    /**
     * Recursively creates the parent directory for the given File if they do
     * not exist.
     *
     * @param file
     * @return {@code true} if all parent directories exist or were created, {@code false} otherwise.
     */
    public abstract boolean createParentDirectory(PrintStream stdout, File file);

    /**
     * @see #resolvePath(File)
     */
    public abstract String resolvePath(String path);

    /**
     * Beautify the path, align the slashes, etc. This does this on
     * a best effort basis.
     *
     * @param f
     * @return The resolved path
     */
    public abstract String resolvePath(File f);

    /**
     * Answers if the path exists.
     *
     * @param path Path whose existence to check for
     * @return {@code true} if the path exists, {@code false} otherwise.
     */
    public abstract boolean exists(String path);

    /**
     * Answers if the File exists.
     *
     * @param file File whose existence to check for
     * @return {@code true} if the File exists, {@code false} otherwise.
     */
    boolean exists(File file);

    /**
     * Checks if the file is a directory, just like the invocation to {@link File#isDirectory()}.
     *
     * @param file The file to check.
     * @return {@code true} If the file is a directory.
     */
    boolean isDirectory(File file);

    /**
     * Store the String to the specified File.
     *
     * @param toWrite
     * @param outFile
     * @return
     */
    boolean writeToFile(PrintStream stderr, String toWrite, File outFile);

    /**
     * @return clientDirectory
     */
    String getClientsDirectory();

}