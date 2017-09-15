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
package com.ibm.ws.jca.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.atomic.AtomicReference;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Some general utilities/common methods and such for the
 * JCA Utilities project.
 */
@Trivial
public class Utils {
    public enum ConstructType {
        AdminObject,
        ConnectionFactory,
        MessageListener,
        ResourceAdapter,
        Unknown
    }

    public static final String NEW_LINE = String.format("%n");

    public static String getSpaceBufferString(int bufferSpaces) {
        StringBuilder sb = new StringBuilder(bufferSpaces);

        while (bufferSpaces-- > 0)
            sb.append(' ');

        return sb.toString();
    }

    /**
     * Returns whether or not a file exists using AccessController.doPrivileged(...)
     * 
     * @param file the file to check
     * @return true if it exists, else false
     */
    public static boolean doesFileExistPrivileged(final File file) {
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return file.exists();
            }
        });
    }

    /**
     * Get a FileInputStream object using AccessController.doPrivileged(...)
     * 
     * @param file the file to get the input stream for
     * @return the file input stream
     * @throws FileNotFoundException
     */
    public static FileInputStream getFileInputStreamPrivileged(final File file) throws FileNotFoundException {
        final AtomicReference<FileNotFoundException> exceptionRef = new AtomicReference<FileNotFoundException>();
        FileInputStream fis = AccessController.doPrivileged(new PrivilegedAction<FileInputStream>() {
            @Override
            public FileInputStream run() {
                try {
                    return new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    exceptionRef.set(e);
                    return null;
                }
            }
        });

        if (exceptionRef.get() != null)
            throw exceptionRef.get();

        return fis;
    }
}
