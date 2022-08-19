/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.internal;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * Application configuration utilities.
 *
 * Element: application, ejbApplication, webApplication
 *
 * Relevant attributes:
 *
 * id string [not-required] [unique] [null]
 * "A unique configuration ID."
 *
 * location string [required] [not-unique]
 * "The location of the application. Specified as an absolute path or as a path relative to the server-level 'apps' directory."
 *
 * name string [not-required] [unique] [simple name of the application file]
 * "The name of the application."
 *
 * type string [not-required] [not-unique] [extension of the application file]
 * "The type of the application."
 */
public class ApplicationUtils {
    // Encode samples:

    // Value [ a ] [ a ]
    // Value [ a a ] [ a+a ]
    // Value [ a+b ] [ a%2Bb ]
    // Value [ a\b ] [ a%5Cb ]
    // Value [ a/b ] [ a%2Fb ]
    // Value [ a a/b b ] [ a+a%2Fb+b ]
    // Value [ a a\b b ] [ a+a%5Cb+b ]

    public static final String UTF_8_ENCODING_NAME = "UTF-8";

    /**
     * URL encode a value, per {@link URLEncoder#encode(String, String)}.
     *
     * This currently intended for generating safe file names for
     * applications, starting with the application ID or PID.
     *
     * @param value The value which is to be encoded.
     *
     * @return The encoded value.
     */
    @FFDCIgnore(UnsupportedEncodingException.class)
    public static final String urlEncode(String value) {
        if (value == null) {
            return null;
        }

        try {
            return URLEncoder.encode(value, UTF_8_ENCODING_NAME); // throws UnsupportedEncodingException
        } catch (UnsupportedEncodingException e) {
            // This should never occur.
            throw new IllegalArgumentException("Unable to use UTF-8 encoding [ " + UTF_8_ENCODING_NAME + " ]", e);
        }
    }

    /**
     * Decode a URL encoded value, per {@link URLEncoder#decode(String, String)}.
     *
     * This currently intended for recovering cache ID values from
     * file names.
     *
     * @param e_value The encoded value.
     *
     * @return The decoded value.
     */
    @FFDCIgnore(UnsupportedEncodingException.class)
    public static final String urlDecode(String e_value) {
        if (e_value == null) {
            return null;
        }

        try {
            return URLDecoder.decode(e_value, UTF_8_ENCODING_NAME); // throws UnsupportedEncodingException
        } catch (UnsupportedEncodingException e) {
            // This should never occur.
            throw new IllegalArgumentException("Unable to use UTF-8 encoding [ " + UTF_8_ENCODING_NAME + " ]", e);
        }
    }

    /**
     * Generate a cache ID given an application ID and and application PID.
     *
     * The ID may be null, in which case the PID is used.
     *
     * The resulting value is URL encoded. This ensures that the generated
     * value is safe for use as a file name.
     *
     * Encoding is technically unnecessary on unix systems. However, embedding
     * special characters can cause problems for scripts. Also, encoding in
     * all cases ensures that unix and windows systems have the same cache
     * IDs.
     *
     * @param appId  The ID of the application.
     * @param appPid The PID of the application.
     *
     * @return The encoded cache ID of the application.
     */
    public static String getCacheId(String appId, String appPid) {
        return urlEncode((appId == null) ? appPid : appId);
    }
}
