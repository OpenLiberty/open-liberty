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
import java.lang.reflect.UndeclaredThrowableException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;

public class ZipFileUtils {

    public static ZipFile openZipFile(final String path) throws ZipException, IOException {
        return openZipFile( new File(path) );
    }

    @FFDCIgnore({PrivilegedActionException.class})
    public static ZipFile openZipFile(final File target) throws IOException {
        Object token = ThreadIdentityManager.runAsServer();

        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<ZipFile>() {
                @Override
                public ZipFile run() throws IOException {
                    return new ZipFile(target);
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
