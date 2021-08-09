/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.logging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This is for use when creating user-facing/user-readable text-based files.
 * <p>
 * On some platforms, text files need to be associated with a file encoding.
 * TextFileOutputStream allows for the use of platform-specific utilities to
 * associate the appropriate file encoding with newly created files.
 * <p>
 * By using the TextFileOutputStreamFactory to create text-based output streams,
 * you can ensure that your file will be readable by users on all platforms.
 */
public interface TextFileOutputStreamFactory {
    /**
     * Creates a file output stream to write to the file represented by the specified File object.
     * A new FileDescriptor object is created to represent this file connection.
     * First, if there is a security manager, its checkWrite method is called with the path
     * represented by the file argument as its argument.
     * <p>
     * If the file exists but is a directory rather than a regular file, does not exist but
     * cannot be created, or cannot be opened for any other reason then a FileNotFoundException
     * is thrown.
     * 
     * @param file the file to be opened for writing.
     * @throws IOException if the file exists but is a directory rather than a regular file,
     *             does not exist but cannot be created, or cannot be opened for any other
     *             reason
     * @throws SecurityException if a security manager exists and its checkWrite method denies write access to the file.
     * 
     * @see FileOutputStream#FileOutputStream(File)
     */
    FileOutputStream createOutputStream(File file) throws IOException;

    /**
     * Creates a file output stream to write to the file represented by the specified File object.
     * A new FileDescriptor object is created to represent this file connection.
     * First, if there is a security manager, its checkWrite method is called with the path
     * represented by the file argument as its argument.
     * <p>
     * If the file exists but is a directory rather than a regular file, does not exist but
     * cannot be created, or cannot be opened for any other reason then a FileNotFoundException
     * is thrown.
     * 
     * @param file the file to be opened for writing.
     * @param append if true, then bytes will be written to the end of the file rather than the beginning
     * @throws IOException if the file exists but is a directory rather than a regular file,
     *             does not exist but cannot be created, or cannot be opened for any other
     *             reason
     * @throws SecurityException if a security manager exists and its checkWrite method denies write access to the file.
     * 
     * @see FileOutputStream#FileOutputStream(File, boolean)
     */
    FileOutputStream createOutputStream(File file, boolean append) throws IOException;

    /**
     * Creates an output file stream to write to the file with the specified name.
     * A new FileDescriptor object is created to represent this file connection.
     * First, if there is a security manager, its checkWrite method is called with name as its argument.
     * <p>
     * If the file exists but is a directory rather than a regular file, does not exist but cannot be created, or cannot be opened for any other reason then a FileNotFoundException
     * is thrown.
     * 
     * @param name the system-dependent filename
     * @throws IOException if the file exists but is a directory rather than a regular file,
     *             does not exist but cannot be created, or cannot be opened for any other
     *             reason
     * @throws SecurityException if a security manager exists and its checkWrite method denies write access to the file.
     * 
     * @see FileOutputStream#FileOutputStream(String)
     */
    FileOutputStream createOutputStream(String name) throws IOException;

    /**
     * Creates an output file stream to write to the file with the specified name.
     * A new FileDescriptor object is created to represent this file connection.
     * First, if there is a security manager, its checkWrite method is called with name as its argument.
     * <p>
     * If the file exists but is a directory rather than a regular file, does not exist but cannot be created, or cannot be opened for any other reason then a FileNotFoundException
     * is thrown.
     * 
     * @param name the system-dependent filename
     * @param append if true, then bytes will be written to the end of the file rather than the beginning
     * @throws IOException if the file exists but is a directory rather than a regular file,
     *             does not exist but cannot be created, or cannot be opened for any other
     *             reason
     * @throws SecurityException if a security manager exists and its checkWrite method denies write access to the file.
     * 
     * @see FileOutputStream#FileOutputStream(String, boolean)
     */
    FileOutputStream createOutputStream(String name, boolean append) throws IOException;
}
