/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.kernel.service.utils;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;

/**
 * A set of utilities for working with Files
 */
public class FileUtils {

    /**
     * Execute the {@link File#isFile()} from within a {@link PrivilegedAction}.
     *
     * @param f
     * @return
     */
    public static boolean fileIsFile(final File target) {
        Object token = ThreadIdentityManager.runAsServer();
        try {
            return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                @Override
                public Boolean run() {
                    return target.isFile();
                }

            });
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    /**
     * Execute the {@link File#isDirectory()} from within a {@link PrivilegedAction}.
     *
     * @param f
     * @return
     */
    public static boolean fileIsDirectory(final File target) {
        Object token = ThreadIdentityManager.runAsServer();
        try {
            return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                @Override
                public Boolean run() {
                    return target.isDirectory();
                }

            });
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    /**
     * Execute the {@link File#exists()} from within a {@link PrivilegedAction}.
     *
     * @param target
     * @return
     */
    public static boolean fileExists(final File target) {
        Object token = ThreadIdentityManager.runAsServer();
        try {
            return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                @Override
                public Boolean run() {
                    return target.exists();
                }

            });
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    /**
     * Execute the {@link File#length()} from within a {@link PrivilegedAction}.
     *
     * @param target
     * @return
     */
    public static long fileLength(final File target) {
        Object token = ThreadIdentityManager.runAsServer();
        try {
            return AccessController.doPrivileged(new PrivilegedAction<Long>() {
                @Override
                public Long run() {
                    return target.length();
                }

            });
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    /**
     * Execute the {@link File#listFiles()} from within a {@link PrivilegedAction}.
     *
     * @param f
     * @return
     */
    public static File[] listFiles(final File target) {
        Object token = ThreadIdentityManager.runAsServer();
        try {
            return AccessController.doPrivileged(new PrivilegedAction<File[]>() {
                @Override
                public File[] run() {
                    return target.listFiles();
                }

            });
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    /**
     * Execute the {@link File#list()} from within a {@link PrivilegedAction}.
     *
     * @param f
     * @return
     */
    public static String[] list(final File target) {
        Object token = ThreadIdentityManager.runAsServer();
        try {
            return AccessController.doPrivileged(new PrivilegedAction<String[]>() {
                @Override
                public String[] run() {
                    return target.list();
                }

            });
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    /**
     * Create a new {@link InputStream} for the file within a {@link PrivilegedAction}.
     */
    public static InputStream getInputStream(final File target) throws FileNotFoundException {
        Object token = ThreadIdentityManager.runAsServer();
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<InputStream>() {
                @Override
                public InputStream run() throws FileNotFoundException {
                    return new FileInputStream(target);
                }
            });
        } catch (PrivilegedActionException e) {
            Exception e2 = e.getException();
            if (e2 instanceof FileNotFoundException)
                throw (FileNotFoundException) e2;
            if (e2 instanceof RuntimeException)
                throw (RuntimeException) e2;
            throw new UndeclaredThrowableException(e);
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    /**
     * Create a new {@link FileOutputStream} for the file within a {@link PrivilegedAction}.
     */
    public static FileOutputStream getFileOutputStream(final File target) throws FileNotFoundException {
        Object token = ThreadIdentityManager.runAsServer();
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<FileOutputStream>() {
                @Override
                public FileOutputStream run() throws FileNotFoundException {
                    return new FileOutputStream(target);
                }
            });
        } catch (PrivilegedActionException e) {
            Exception e2 = e.getException();
            if (e2 instanceof FileNotFoundException)
                throw (FileNotFoundException) e2;
            if (e2 instanceof RuntimeException)
                throw (RuntimeException) e2;
            throw new UndeclaredThrowableException(e);
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    /**
     * Execute the {@link File#lastModified()} from within a {@link PrivilegedAction}.
     *
     * @param target The file to get the last modified for
     * @return The last modified for the file
     */
    public static long fileLastModified(final File target) {
        Object token = ThreadIdentityManager.runAsServer();
        try {
            return AccessController.doPrivileged(new PrivilegedAction<Long>() {
                @Override
                public Long run() {
                    return target.lastModified();
                }

            });
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    /**
     * Execute the {@link File#canRead()} from within a {@link PrivilegedAction}.
     *
     * @param target The file to test if it can be read
     * @return <code>true</code> if the file can be read
     */
    public static boolean fileCanRead(final File target) {
        Object token = ThreadIdentityManager.runAsServer();
        try {
            return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                @Override
                public Boolean run() {
                    return target.canRead();
                }

            });
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    /**
     * Execute the {@link File#canWrite()} from within a {@link PrivilegedAction}.
     *
     * @param target The file to test if it can be written
     * @return <code>true</code> if the file can be written
     */
    public static boolean fileCanWrite(final File target) {
        Object token = ThreadIdentityManager.runAsServer();
        try {
            return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                @Override
                public Boolean run() {
                    return target.canWrite();
                }

            });
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    /**
     * Calls {@link File#mkdirs()} on the specified <code>target</code> from
     * within a {@link PrivilegedAction}.
     *
     * @param target The tarket to make a directory for
     * @return <code>true</code> if this succeeded.
     */
    public static boolean fileMkDirs(final File target) {
        Object token = ThreadIdentityManager.runAsServer();
        try {
            return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                @Override
                public Boolean run() {
                    return target.mkdirs();
                }

            });
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    /**
     * Delete file
     *
     * @parm file or empty directory to delete
     * @return <code>true</code> if file was deleted
     */
    public static boolean fileDelete(final File file) {
        Object token = ThreadIdentityManager.runAsServer();
        try {
            return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                @Override
                public Boolean run() {
                    return file.delete();
                }

            });
        } finally {
            ThreadIdentityManager.reset(token);
        }
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
     * Close the closeable object
     *
     * @param closeable
     */
    public static boolean tryToClose(Closeable closeable) {
        Object token = ThreadIdentityManager.runAsServer();
        try {
            if (closeable != null) {
                try {
                    closeable.close();
                    return true;
                } catch (IOException e) {
                    // ignore
                }
            }
        } finally {
            ThreadIdentityManager.reset(token);
        }
        return false;
    }

    static private class SetFilePermsAction implements PrivilegedAction<Boolean> {
        private final File file;

        SetFilePermsAction(File file) {
            this.file = file;
        }

        @Override
        public Boolean run() {
            // Set the file as 000
            file.setReadable(false, false);
            file.setWritable(false, false);
            file.setExecutable(false, false);

            // Set the file as 600
            file.setReadable(true, true);
            file.setWritable(true, true);

            return true;
        }
    }

    /**
     * Set the file permissions of the file to be user rw only.
     * <p>
     * This is a best effort attempt as Windows does NOT play
     * nicely with file perms.
     *
     * @param an existing File
     */
    public static boolean setUserReadWriteOnly(final File file) {
        return AccessController.doPrivileged(new SetFilePermsAction(file));
    }

    /**
     * Calls {@link File#createNewFile()} on the specified <code>target</code>
     *
     * @param target The target to create if it doesn't exist
     * @return <code>true</code> if call succeeded.
     */
    public static Boolean fileCreate(final File target) throws IOException {
        Object token = ThreadIdentityManager.runAsServer();
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Boolean>() {
                @Override
                public Boolean run() throws IOException {
                    return target.createNewFile();
                }
            });
        } catch (PrivilegedActionException e) {
            Exception e2 = e.getException();
            if (e2 instanceof IOException)
                throw (IOException) e2;
            if (e2 instanceof RuntimeException)
                throw (RuntimeException) e2;
            throw new UndeclaredThrowableException(e);
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }
}
