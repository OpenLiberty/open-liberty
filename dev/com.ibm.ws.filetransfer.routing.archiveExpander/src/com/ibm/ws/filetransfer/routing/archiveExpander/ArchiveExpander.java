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
package com.ibm.ws.filetransfer.routing.archiveExpander;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

/**
 * This class provides simple archive expansion capability.
 */
public class ArchiveExpander {

    private static final UnixModeHelper helper;

    static {
        UnixModeHelper helper2 = null;
        try {
            Class.forName("java.nio.file.attribute.PosixFilePermission");
            helper2 = new Java7UnixModeHelper();
        } catch (ClassNotFoundException e) {
            // Expected on Java 6, in which case we don't use the helper and cope.
            helper2 = new ChmodUnixModeHelper();
        }
        helper = helper2;
    }

    /**
     * @param args the source and target locations, as absolute paths.
     */
    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            System.exit(-1);
        } else {
            if (!expandArchive(args[0], args[1])) {
                System.exit(-1);
            }
        }
    }

    /**
     * Expand the specified archive to the specified location
     * <p>
     * 
     * @param sourcePath path of the archive to be expanded.
     * @param targetPath location to where the archive is to be expanded.
     *            <p>
     * @returns true if the archive was successfully expanded, false otherwise.
     */
    public static boolean expandArchive(String sourcePath, String targetPath) {
        try {
            return coreExpandArchive(sourcePath, targetPath);
        } catch (IOException e) {
            e.printStackTrace(System.err);
            return false;
        }
    }

    /**
     * The core portion of the archive expander code was refactored so that in this
     * standalone class/jar we can output to System.err but internal Liberty callers
     * can call the proper FFDC routines.
     */
    public static boolean coreExpandArchive(String sourcePath, String targetPath) throws IOException {
        ZipArchiveInputStream in = null;
        OutputStream out = null;

        try {

            // make sure we're working with absolute canonical paths
            File source = new File(sourcePath).getCanonicalFile();
            File target = new File(targetPath).getCanonicalFile();

            // open the archive
            in = new ZipArchiveInputStream(getInputStream(source));

            // expand all entries of the archive
            for (ZipArchiveEntry entry = in.getNextZipEntry(); entry != null; entry = in.getNextZipEntry()) {

                // get the pathname of the entry (this will be a relative path)
                String outFilename = entry.getName();

                // construct the absolute path of where this entry will be expanded
                String targetPlusOutFile = target.getPath() + File.separator + outFilename;

                File targetFile = new File(targetPlusOutFile);
                char ending = outFilename.charAt(outFilename.length() - 1);

                if (ending == '/' || ending == '\\') {
                    fileMkDirs(targetFile);
                    if (helper != null) {
                        helper.setPermissions(targetFile, entry.getUnixMode());
                    }
                    continue;
                } else {
                    fileMkDirs(targetFile.getParentFile());
                }

                // expand the entry
                out = getOutputStream(targetFile);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }

                // Close the streams
                out.close();

                if (helper != null) {
                    helper.setPermissions(targetFile, entry.getUnixMode());
                }

                out = null;
            }
            in.close();
            in = null;

            return true;

        } catch (IOException e) {
            throw e;
        } finally {
            // try to close any open files
            tryToClose(out);
            tryToClose(in);
        }
    }

    public static boolean tryToClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
                return true;
            } catch (IOException e) {
                // ignore
            }
        }
        return false;
    }

    public static boolean fileMkDirs(final File target) {
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return target.mkdirs();
            }

        });
    }

    private static OutputStream getOutputStream(final File target) throws FileNotFoundException {

        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<OutputStream>() {
                @Override
                public OutputStream run() throws FileNotFoundException {
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
        }
    }

    public static InputStream getInputStream(final File target) throws FileNotFoundException {
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
        }
    }

    public static boolean fileIsDirectory(final File target) {
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return target.isDirectory();
            }

        });
    }

}
