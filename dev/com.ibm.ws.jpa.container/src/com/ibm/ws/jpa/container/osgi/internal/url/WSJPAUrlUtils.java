/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.container.osgi.internal.url;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 *
 */
public class WSJPAUrlUtils {
    public final static String WSJPA_PROTOCOL_NAME = "wsjpa";

    /**
     * Encapsulates the specified URL within a wsjpa URL.
     * 
     * @param url - the URL to encapsulate
     * @return - a wsjpa URL encapsulating the argument URL
     * @throws MalformedURLException
     */
    @Trivial
    public static URL createWSJPAURL(URL url) throws MalformedURLException {
        if (url == null) {
            return null;
        }

        // Encode the URL to be embedded into the wsjpa URL's path
        final String encodedURLPathStr = encode(url.toExternalForm());

        URL returnURL;
        try {
            returnURL = AccessController.doPrivileged(new PrivilegedExceptionAction<URL>() {
                @Override
                @Trivial
                public URL run() throws MalformedURLException {
                    return new URL(WSJPA_PROTOCOL_NAME + ":" + encodedURLPathStr);
                }
            });
        } catch (PrivilegedActionException e) {
            throw (MalformedURLException) e.getException();
        }

        return returnURL;
    }

    /**
     * Extracts the embedded URL from the provided wsjpa protocoled URL.
     * 
     * @param url - the wsjpa URL from which to extract the embedded URL.
     * @return - the URL embedded in the wsjpa URL argument. Returns null if provided a null URL argument.
     * @throws MalformedURLException if the embedded URL is malformed, IllegalArgumentException
     *             if the provided url is not a wsjpa URL or if the wsjpa URL is missing an embedded URL.
     */
    @Trivial
    public static URL extractEmbeddedURL(URL url) throws MalformedURLException {
        if (url == null) {
            return null;
        }

        if (!url.getProtocol().equalsIgnoreCase(WSJPA_PROTOCOL_NAME)) {
            throw new IllegalArgumentException("The specified URL \"" + url +
                                               "\" does not use the \"" + WSJPA_PROTOCOL_NAME + "\" protocol.");
        }

        String encodedPath = url.getPath();
        if (encodedPath == null || encodedPath.trim().equals("")) {
            throw new IllegalArgumentException("The specified URL \"" + url + "\" is missing path information.");
        }

        final String decodedPath = decode(encodedPath);

        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<URL>() {
                @Override
                @Trivial
                public URL run() throws MalformedURLException {
                    return new URL(decodedPath);
                }
            });
        } catch (PrivilegedActionException e) {
            throw (MalformedURLException) e.getException();
        }
    }

    /**
     * Private method that substitutes "!" characters with its escaped code, "%21".
     * 
     * @param s
     * @return
     */
    @Trivial
    private static String encode(String s) {
        if (s == null) {
            return null;
        }

        // Throw an IllegalArgumentException if "%21" is already present in the String
        if (s.contains("%21")) {
            throw new IllegalArgumentException("WSJPAURLUtils.encode() cannot encode Strings containing \"%21\".");
        }
        return s.replace("!", "%21");
    }

    /**
     * Private method that substitutes the escape code "%21" with the "!" character.
     * 
     * @param s
     * @return
     */
    @Trivial
    private static String decode(String s) {
        if (s == null) {
            return null;
        }
        return s.replace("%21", "!");
    }
}
