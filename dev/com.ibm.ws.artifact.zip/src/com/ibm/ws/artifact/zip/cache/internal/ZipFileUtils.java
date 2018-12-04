/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.zip.cache.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;

public class ZipFileUtils {
    private static final TraceComponent tc = Tr.register(ZipFileUtils.class);

    public static void diagnose(String title, File file, ZipFile zipFile) {
        String methodName = "diagnose";

        try {
            if ( !tc.isDebugEnabled() ) {
                return;
            }

            Tr.debug(tc, methodName + ": " + title);

            if ( file == null ) {
                Tr.debug(tc, methodName + ": Null file");
                return;
            }

            String filePrefix = methodName + ": File [ " + file + "]: ";
            Tr.debug(tc, filePrefix + "Name [ " + file.getName() + " ] path [ " + file.getPath() + " ]");
            Tr.debug(tc, filePrefix + "Absolute path [ " + file.getAbsolutePath() + " ]");

            boolean exists = file.exists();
            Tr.debug(tc,  filePrefix + "Exists [ " + exists + " ]");

            if ( !exists ) {
                return;
            }

            Tr.debug(tc, filePrefix + "Length [ " + file.length() + " ]");
            Tr.debug(tc, filePrefix + "stamp [ " + file.lastModified() + " ]");
            Tr.debug(tc, filePrefix + "isFile [ " + file.isFile() + " ]");
            Tr.debug(tc, filePrefix + "isDir [ " + file.isDirectory() + " ]");
            Tr.debug(tc, filePrefix + "read[ " + file.canRead() + " ] write [ " + file.canWrite() + " ] exec [ " + file.canExecute() + " ]");

            if ( zipFile == null ) {
                Tr.debug(tc, methodName + ": Null zip file");
                return;
            }

            String zipPrefix = methodName + ": Zip [ " + zipFile + "]: ";

            Tr.debug(tc, zipPrefix + "Name [ " + zipFile.getName() + " ]");

            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while ( entries.hasMoreElements() ) {
                ZipEntry entry = entries.nextElement();

                String entryPrefix = methodName + ": Entry [ " + zipFile + "] [ " + entry + " ]: ";

                Tr.debug(tc, entryPrefix + "Name [ " + entry.getName() + " ]");
                Tr.debug(tc, entryPrefix + "Size [ " + entry.getSize() + " ] compressed [ " + entry.getCompressedSize() + " ]");
                Tr.debug(tc, entryPrefix + "Modified [ " + entry.getTime() + " ]");

                ZipEntry altEntry = zipFile.getEntry( entry.getName() );
                Tr.debug(tc, entryPrefix + " getEntry [ " + altEntry + " ]");

                InputStream zipStream;
                try {
                    zipStream = zipFile.getInputStream(entry); // throws IOException
                    Tr.debug(tc,  entryPrefix + "Open [ Success ]");
                    try {
                        zipStream.close(); // throws IOException
                        Tr.debug(tc,  entryPrefix + "Close [ Success ]");
                    } catch ( IOException eClose ) {
                        Tr.debug(tc,  entryPrefix + "Close [ fail ] [ " + eClose.getMessage() + " ]");
                    }
                } catch ( IOException eOpen ) {
                    Tr.debug(tc,  entryPrefix + "Open [ fail ] [ " + eOpen.getMessage() + " ]");
                }
            }
        } catch ( Throwable th ) {
            // FFDC
        }
    }

    public static ZipFile openZipFile(final String path) throws ZipException, IOException {
        return openZipFile( new File(path) ); // throws IOException
    }

    @FFDCIgnore({PrivilegedActionException.class})
    public static ZipFile openZipFile(final File file) throws IOException {
        Object token = ThreadIdentityManager.runAsServer();

        ZipFile zipFile;
        try {
            zipFile = AccessController.doPrivileged(new PrivilegedExceptionAction<ZipFile>() {
                @Override
                public ZipFile run() throws IOException {
                    return new ZipFile(file);
                }
            });

        } catch (PrivilegedActionException e) {
            Exception e2 = e.getException();
            if (e2 instanceof IOException) {
                throw (IOException) e2;
            } else if (e2 instanceof RuntimeException) {
                throw (RuntimeException) e2;
            } else {
                throw new UndeclaredThrowableException(e);
            }

        } finally {
            ThreadIdentityManager.reset(token);
        }

        ZipFileUtils.diagnose("open", file, zipFile);

        return zipFile;
    }

    @FFDCIgnore({PrivilegedActionException.class})
    public static void closeZipFile(final String path, final ZipFile zipFile) throws IOException {
        Object token = ThreadIdentityManager.runAsServer();

        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                @Override
                public Void run() throws IOException {
                    zipFile.close(); // throws IOException
                    return null;
                }
            });
        } catch ( PrivilegedActionException e ) {
            Throwable innerException = e.getCause();
            if ( innerException instanceof ZipException ) {
                throw (ZipException) innerException;
            } else if ( innerException instanceof IOException ) {
                throw (IOException) innerException;
            } else if ( innerException instanceof RuntimeException ) {
                throw (RuntimeException) innerException;
            } else {
                throw new UndeclaredThrowableException(innerException);
            }

        } finally {
            ThreadIdentityManager.reset(token);
        }
    }
}
