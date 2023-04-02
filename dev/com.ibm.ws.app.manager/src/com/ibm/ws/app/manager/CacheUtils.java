/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.app.manager;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

public class CacheUtils {

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
        } catch (UnsupportedEncodingException e) { // Unexpected
            throw new IllegalArgumentException("Unable to use UTF-8 encoding [ " + UTF_8_ENCODING_NAME + " ]", e);
        }
    }

    /**
     * Generate a cache ID given the PID and ID property of an application.
     *
     * When the ID is null, answer the PID. Otherwise, answer the URL encoded
     * ID. (The ID is often null.)
     *
     * URL encoding the ID ensures that the generated value is safe for use as
     * a file name.
     *
     * Encoding is technically unnecessary on unix systems. However, embedding
     * special characters can cause problems for scripts. Also, encoding in
     * all cases ensures that unix and windows systems have the same cache
     * IDs.
     *
     * @param pid The PID of the application.
     * @param id  The ID property of the application.
     *
     * @return The encoded cache ID of the application.
     */
    public static String getCacheId(String pid, String id) {
        return urlEncode((id == null) ? pid : id);
    }
}
