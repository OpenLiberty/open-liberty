/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.url.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

/**
 *
 */
public class Utils {

    /**
     * @param f
     * @return
     */
    public static URL newURL(final String protocol, final String host, final int port, final String file) throws MalformedURLException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<URL>() {
                @Override
                public URL run() throws MalformedURLException {
                    return new URL(protocol, host, port, file);
                }

            });
        } catch (PrivilegedActionException e) {
            Exception e2 = e.getException();
            if (e2 instanceof MalformedURLException)
                throw (MalformedURLException) e2;
            if (e2 instanceof RuntimeException)
                throw (RuntimeException) e2;
            throw new UndeclaredThrowableException(e);
        }
    }

    /**
     * @param f
     * @return
     */
    public static URL newURL(final String path) throws MalformedURLException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<URL>() {
                @Override
                public URL run() throws MalformedURLException {
                    return new URL(path);
                }

            });
        } catch (PrivilegedActionException e) {
            Exception e2 = e.getException();
            if (e2 instanceof MalformedURLException)
                throw (MalformedURLException) e2;
            if (e2 instanceof RuntimeException)
                throw (RuntimeException) e2;
            throw new UndeclaredThrowableException(e);
        }
    }

    /**
     * @param f
     * @return
     */
    public static URL newURL(final URL base, final String spec) throws MalformedURLException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<URL>() {
                @Override
                public URL run() throws MalformedURLException {
                    return new URL(base, spec);
                }

            });
        } catch (PrivilegedActionException e) {
            Exception e2 = e.getException();
            if (e2 instanceof MalformedURLException)
                throw (MalformedURLException) e2;
            if (e2 instanceof RuntimeException)
                throw (RuntimeException) e2;
            throw new UndeclaredThrowableException(e);
        }
    }

    public static long getLastModified(final File target) {
        return AccessController.doPrivileged(new PrivilegedAction<Long>() {
            @Override
            public Long run() {
                return target.lastModified();
            }

        });
    }

    public static long getStreamLength(InputStream is) throws IOException {
        long len = 0;
        byte[] buf = new byte[256];
        int bytesRead = 0;
        while ((bytesRead = is.read(buf)) != -1) {
            len += bytesRead;
        }
        return len;
    }
}