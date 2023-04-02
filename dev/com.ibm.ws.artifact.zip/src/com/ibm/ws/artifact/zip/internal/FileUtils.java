/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
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
package com.ibm.ws.artifact.zip.internal;

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
import java.util.function.Function;

import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;

/**
 * File utilities.
 */
public class FileUtils {

    public static <R> R fileAction(final File target, Function<File,R> function) {
        Object token = ThreadIdentityManager.runAsServer();
        try {
            return AccessController.doPrivileged(new PrivilegedAction<R>() {
                @Override
                public R run() {
                    return function.apply(target);
                }
            });
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    private static final Function<File, Boolean> isFileAction = new Function<File, Boolean>() {
        @Override
        public Boolean apply(File target) {
            return target.isFile();
        }
    };

    public static boolean fileIsFile(final File target) {
        return fileAction(target, isFileAction);
    }

    private static final Function<File, Boolean> isDirectoryAction = new Function<File, Boolean>() {
        @Override
        public Boolean apply(File target) {
            return target.isDirectory();
        }
    };

    public static boolean fileIsDirectory(final File target) {
        return fileAction(target, isDirectoryAction);
    }

    private static final Function<File, Boolean> existsAction = new Function<File, Boolean>() {
        @Override
        public Boolean apply(File target) {
            return target.exists();
        }
    };

    public static boolean fileExists(final File target) {
        return fileAction(target, existsAction);
    }

    private static final Function<File, Long> lengthAction = new Function<File, Long>() {
        @Override
        public Long apply(File target) {
            return target.length();
        }
    };

    public static long fileLength(final File target) {
        return fileAction(target, lengthAction);
    }

    private static final Function<File, File[]> listFilesAction = new Function<File, File[]>() {
        @Override
        public File[] apply(File target) {
            return target.listFiles();
        }
    };

    public static File[] listFiles(final File target) {
        return fileAction(target, listFilesAction);
    }

    private static final Function<File, String[]> listAction = new Function<File, String[]>() {
        @Override
        public String[] apply(File target) {
            return target.list();
        }
    };

    public static String[] list(final File target) {
        return fileAction(target, listAction);
    }

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

    private static final Function<File, Long> lastModifiedAction = new Function<File, Long>() {
        @Override
        public Long apply(File target) {
            return target.lastModified();
        }
    };

    public static long fileLastModified(final File target) {
        return fileAction(target, lastModifiedAction);
    }

    public static boolean fileSetLastModified(final File target, final long lastModified) {
        Object token = ThreadIdentityManager.runAsServer();
        try {
            return AccessController.doPrivileged( new PrivilegedAction<Boolean>() {
                @Override
                public Boolean run() {
                    return Boolean.valueOf( target.setLastModified(lastModified) );
                }
            } ).booleanValue();
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    private static final Function<File, Boolean> canReadAction = new Function<File, Boolean>() {
        @Override
        public Boolean apply(File target) {
            return target.canRead();
        }
    };

    public static boolean fileCanRead(final File target) {
        return fileAction(target, canReadAction);
    }

    private static final Function<File, Boolean> canWriteAction = new Function<File, Boolean>() {
        @Override
        public Boolean apply(File target) {
            return target.canWrite();
        }
    };

    public static boolean fileCanWrite(final File target) {
        return fileAction(target, canWriteAction);
    }

    private static final Function<File, Boolean> deleteAction = new Function<File, Boolean>() {
        @Override
        public Boolean apply(File target) {
            return target.delete();
        }
    };

    public static boolean fileDelete(final File file) {
        return fileAction(file, deleteAction);
    }

    private static final Function<File, Boolean> ensureDirExistsAction = new Function<File, Boolean>() {
        @Override
        public Boolean apply(File target) {
            return target.mkdirs() || target.exists();
        }
    };

    public static boolean ensureDirExists(File dir) {
        return fileAction(dir, ensureDirExistsAction);
    }

    public static boolean tryToClose(Closeable closeable) {
        Object token = ThreadIdentityManager.runAsServer();
        try {
            if (closeable != null) {
                try {
                    closeable.close();
                    return true;
                } catch (IOException e) {
                    // FFDC
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

    public static boolean setUserReadWriteOnly(final File file) {
        return AccessController.doPrivileged(new SetFilePermsAction(file));
    }

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
