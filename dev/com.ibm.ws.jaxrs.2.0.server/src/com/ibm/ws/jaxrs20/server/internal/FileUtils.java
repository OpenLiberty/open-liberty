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
package com.ibm.ws.jaxrs20.server.internal;

import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class FileUtils {

    public static boolean recursiveDelete(final File fileToRemove) {
        if (fileToRemove == null)
            return true;

        if (!fileToRemove.exists())
            return true;

        boolean success = true;

        if (fileToRemove.isDirectory()) {
            File[] files = fileToRemove.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    success |= recursiveDelete(file);
                } else {
                    success |= file.delete();
                }
            }
            files = fileToRemove.listFiles();
            if (files.length == 0)
                success |= fileToRemove.delete();
        } else {
            success |= fileToRemove.delete();
        }
        return success;
    }

    /**
     * Calls {@link File#mkdirs()} and {@link File#exists()} on the specified <code>target</code>
     * 
     * @param target The target to check for existence or to create if it doesn't exist
     * @return <code>true</code> if either call succeeded.
     */
    public static boolean ensureDirExists(File dir) {
        return (fileMkDirs(dir) || fileExists(dir));
    }

    /**
     * Execute the {@link File#exists()} from within a {@link PrivilegedAction}.
     * 
     * @param target
     * @return
     */
    public static boolean fileExists(final File target) {
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return target.exists();
            }

        });
    }

    /**
     * Calls {@link File#mkdirs()} on the specified <code>target</code> from
     * within a {@link PrivilegedAction}.
     * 
     * @param target The tarket to make a directory for
     * @return <code>true</code> if this succeeded.
     */
    public static boolean fileMkDirs(final File target) {
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return target.mkdirs();
            }

        });
    }
}
