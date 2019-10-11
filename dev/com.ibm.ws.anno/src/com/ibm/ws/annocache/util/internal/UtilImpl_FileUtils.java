/*******************************************************************************
 * Copyright (c) 2011, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.annocache.util.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.channels.Channels;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.annotation.Trivial;

// TODO: Are there global utilities to use instead of these?

public class UtilImpl_FileUtils {
    private static final String CLASS_NAME = "UtilImpl_FileUtils";

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger("com.ibm.ws.annocache.util");

    // Testing ...

    public static boolean isFile(final File target) {
        Boolean result = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return Boolean.valueOf(target.isFile());
            }
        });
        return result.booleanValue();
    }

    public static boolean isDirectory(final File target) {
        Boolean result = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return Boolean.valueOf(target.isDirectory());
            }
        });
        return result.booleanValue();
    }

    public static boolean exists(final File target) {
        Boolean exists = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return Boolean.valueOf(target.exists());
            }
        });
        return exists.booleanValue();
    }

    public static File validateDir(final File dir) {
        return AccessController.doPrivileged(new PrivilegedAction<File>() {
            @Override
            public File run() {
                if ( !dir.exists() ) {
                    throw new IllegalArgumentException("Target location [ " + dir.getPath() + " ] does not exist.");
                } else if ( !dir.isDirectory() ) {
                    throw new IllegalArgumentException("Target location [ " + dir.getPath() + " ] is not a directory.");
                } else {
                    return dir;
                }
            }
        });
    }

    public static String getAbsolutePath(final File target) {
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return target.getAbsolutePath();
            }
        });
    }

    public static String getCanonicalPath(final File target) {
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                try {
                    return target.getCanonicalPath(); // throws IOException
                } catch ( IOException e ) {
                    return target.getAbsolutePath();
                }
            }
        });
    }

    // Listing ...

    public static final File[] EMPTY_FILES = new File[] {};
    public static final String[] EMPTY_FILE_NAMES = new String[] {};

    public static File[] listFiles(final File target) {
        return AccessController.doPrivileged(new PrivilegedAction<File[]>() {
            @Override
            public File[] run() {
                File[] fileList = target.listFiles();
                return ( (fileList == null) ? EMPTY_FILES : fileList );
            }
        });
    }

    public static File[] listFiles(final File target, final FilenameFilter filter) {
        return AccessController.doPrivileged(new PrivilegedAction<File[]>() {
            @Override
            public File[] run() {
                File[] fileList = target.listFiles(filter);
                return ( (fileList == null) ? EMPTY_FILES : fileList );
            }
        });
    }

    public static String[] list(final File target) {
        return AccessController.doPrivileged(new PrivilegedAction<String[]>() {
            @Override
            public String[] run() {
                String[] fileNames = target.list();
                return ( (fileNames == null) ? EMPTY_FILE_NAMES : fileNames );
            }
        });
    }

    public static String[] list(final File target, final FilenameFilter filter) {
        return AccessController.doPrivileged(new PrivilegedAction<String[]>() {
            @Override
            public String[] run() {
                String[] fileNames = target.list(filter);
                return ( (fileNames == null) ? EMPTY_FILE_NAMES : fileNames );
            }
        });
    }

    // IO ...

    public static FileOutputStream createFileOutputStream(final File target) throws IOException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<FileOutputStream>() {
                @Override
                public FileOutputStream run() throws IOException {
                    return new FileOutputStream(target);
                }
            });
        } catch (PrivilegedActionException e) {
            Exception innerException = e.getException();
            if (innerException instanceof IOException) {
                throw (IOException) innerException;
            } else if (innerException instanceof RuntimeException) {
                throw (RuntimeException) innerException;
            } else {
                throw new UndeclaredThrowableException(e);
            }
        }
    }

    public static OutputStream createOverwriteOutputStream(final File target) throws IOException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<OutputStream>() {
                @Override
                public OutputStream run() throws IOException {
                    @SuppressWarnings("resource")
                    RandomAccessFile randomAccessFile = new RandomAccessFile(target, "rw");
                    return Channels.newOutputStream( randomAccessFile.getChannel() );
                }
            });
        } catch (PrivilegedActionException e) {
            Exception innerException = e.getException();
            if (innerException instanceof IOException) {
                throw (IOException) innerException;
            } else if (innerException instanceof RuntimeException) {
                throw (RuntimeException) innerException;
            } else {
                throw new UndeclaredThrowableException(e);
            }
        }
    }

    public static final boolean DO_APPEND = true;
    public static final boolean DO_NOT_APPEND = false;

    public static FileOutputStream createFileOutputStream(final File target, final boolean append) throws IOException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<FileOutputStream>() {
                @Override
                public FileOutputStream run() throws IOException {
                    return new FileOutputStream(target, append);
                }
            });
        } catch (PrivilegedActionException e) {
            Exception innerException = e.getException();
            if (innerException instanceof IOException) {
                throw (IOException) innerException;
            } else if (innerException instanceof RuntimeException) {
                throw (RuntimeException) innerException;
            } else {
                throw new UndeclaredThrowableException(e);
            }
        }
    }

    public static FileInputStream createFileInputStream(final File target) throws IOException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<FileInputStream>() {
                @Override
                public FileInputStream run() throws IOException {
                    return new FileInputStream(target);
                }
            });
        } catch (PrivilegedActionException e) {
            Exception innerException = e.getException();
            if (innerException instanceof IOException) {
                throw (IOException) innerException;
            } else if (innerException instanceof RuntimeException) {
                throw (RuntimeException) innerException;
            } else {
                throw new UndeclaredThrowableException(e);
            }
        }
    }

    public static RandomAccessFile createRandomInputFile(final File target) throws IOException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<RandomAccessFile>() {
                @Override
                public RandomAccessFile run() throws IOException {
                    return new RandomAccessFile(target, "r");
                }
            });
        } catch (PrivilegedActionException e) {
            Exception innerException = e.getException();
            if (innerException instanceof IOException) {
                throw (IOException) innerException;
            } else if (innerException instanceof RuntimeException) {
                throw (RuntimeException) innerException;
            } else {
                throw new UndeclaredThrowableException(e);
            }
        }
    }

    public static JarFile createJarFile(final String jarPath) throws IOException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<JarFile>() {
                @Override
                public JarFile run() throws IOException {
                    return new JarFile(jarPath);
                }
            });
        } catch ( PrivilegedActionException e ) {
            Exception innerException = e.getException();
            if ( innerException instanceof IOException ) {
                throw (IOException) innerException;
            } else if ( innerException instanceof RuntimeException ) {
                throw ( RuntimeException ) innerException;
            } else {
                throw new UndeclaredThrowableException(e);
            }
        }
    }

    // Dir ops ...

    public static boolean mkdirs(final File file) {
        Boolean result = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return Boolean.valueOf( file.mkdirs() );
            }
        });
        return result.booleanValue();
    }

    public static boolean ensureDir(final Logger useLogger, final File file) {
        Boolean existsAsDir = AccessController.doPrivileged( new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return Boolean.valueOf( unprotectedEnsureDir(useLogger, file) );
            }
        } );
        return existsAsDir.booleanValue();
    }

    public static boolean unprotectedEnsureDir(final Logger useLogger, final File file) {
        String methodName = "unprotectedEnsureDir";

        if ( file.exists() ) {
            if ( !file.isDirectory() ) {
                if ( useLogger != null ) {
                    useLogger.logp(Level.WARNING, CLASS_NAME, methodName,
                            "Target [ {0} ] already exists as a simple file",
                            file.getPath());
                } else {
                    System.out.println("Target [ " + file.getPath() + " ] already exists as a simple file");
                }
                return false;

            } else {
                if ( (useLogger != null) && useLogger.isLoggable(Level.FINER) ) {
                    useLogger.logp(Level.FINER, CLASS_NAME, methodName,
                            "Target [ {0} ] already exists as directory",
                            file.getPath());
                }
                return true;
            }

        } else {
            @SuppressWarnings("unused")
            boolean didMake = file.mkdirs();

            if ( !file.exists() ) {
                if ( useLogger != null ) {
                    useLogger.logp(Level.WARNING, CLASS_NAME, methodName,
                            "Target [ {0} ] could not be created",
                            file.getPath());
                } else {
                    System.out.println("Target [ " + file.getPath() + " ] could not be created");
                }
                return false;

            } else {
                if ( !file.isDirectory() ) {
                    if ( useLogger != null ) {
                        useLogger.logp(Level.WARNING, CLASS_NAME, methodName,
                                "Target [ {0} ] was created, but is not a directory",
                                file.getPath());
                    } else {
                        System.out.println("Target [ " + file.getPath() + " ] was created, but is not a directory");
                    }
                    return false;

                } else {
                    if ( (useLogger != null) && useLogger.isLoggable(Level.FINER) ) {
                        useLogger.logp(Level.FINER, CLASS_NAME, methodName,
                                "Target [ {0} ] was created as a directory",
                                file.getPath());
                    }
                    return true;
                }
            }
        }
    }

    public static boolean remove(final Logger useLogger, final File file) {
        final String methodName = "remove";
        final Logger innerLogger = useLogger;
        
        Boolean failedRemoval = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                final String innerMethodName = methodName + "." + "run";

                @SuppressWarnings("unused")
                boolean didDelete = file.delete();

                if ( file.exists() ) {
                    if ( useLogger != null ) {
                        innerLogger.logp(Level.WARNING, CLASS_NAME, innerMethodName, 
                                "Failed to delete [ {0} ]",
                                file.getPath());
                    } else {
                        System.out.println("Failed to delete [ " + file.getPath() + " ]");
                    }
                    return Boolean.TRUE;

                } else {
                    if ( (innerLogger != null) && innerLogger.isLoggable(Level.FINER) ) {
                        innerLogger.logp(Level.FINER, CLASS_NAME, innerMethodName, "Deleted [ {0} ]", file.getPath());
                    }
                    return Boolean.FALSE;
                }
            }
        });
        return failedRemoval.booleanValue();
    }

    public static int removeAll(final Logger useLogger, final File file) {
        Integer failedRemovals = AccessController.doPrivileged(new PrivilegedAction<Integer>() {
            @Override
            public Integer run() {
                return Integer.valueOf( unprotectedRemoveAll(useLogger, file) );
            }
        });

        return failedRemovals.intValue();
    }

    public static int unprotectedRemoveAll(Logger useLogger, File file) {
        String methodName = "unprotectedRemoveAll";

        int failedRemovals = 0;

        if ( file.isDirectory() ) {
            File[] childFiles = file.listFiles();
            if ( childFiles != null ) {
                for ( File childFile : childFiles) {
                    failedRemovals += unprotectedRemoveAll(useLogger, childFile);
                }
            }
        }

        file.delete();

        if ( file.exists() ) {
            failedRemovals++;

            if ( useLogger != null ) {
                useLogger.logp(Level.WARNING, CLASS_NAME, methodName, "Failed to delete [ {0} ]", file.getPath());
            } else {
                System.out.println("Failed to delete [ " + file.getPath() + " ]");
            }

        } else {
            if ( (useLogger != null) && useLogger.isLoggable(Level.FINER) ) {
                useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Deleted [ {0} ]", file.getPath());
            }
        }

        return failedRemovals;
    }

    //

    @Trivial
    public static byte[] readFully(File file) throws IOException {
        long rawFileLength = file.length();
        if ( rawFileLength > Integer.MAX_VALUE ) {
            throw new IOException(
                "File length [ " + rawFileLength + " ]" +
                " greater than [ " + Integer.MAX_VALUE + " ]" +
                " for [ " + file.getAbsolutePath() + " ]");
        }
        
        int bufferFill = (int) rawFileLength; 
        byte[] buffer = new byte[ bufferFill ];

        InputStream inputStream = new FileInputStream(file);
        try {
            int bytesRead = inputStream.read(buffer);
            if ( bytesRead != bufferFill ) {
                throw new IOException(
                    "Incomplete read [ " + file.getAbsolutePath() + " ]" +
                    " expected [ " + bufferFill + " ]" +
                    " but read [ " + bytesRead + " ]");
            }
        } finally {
            inputStream.close();
        }

        return buffer;
    }
}
