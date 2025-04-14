/*******************************************************************************
 * Copyright (c) 2014, 2025 IBM Corporation and others.
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
package com.ibm.ws.security.openidconnect.server.internal;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.common.encoder.Base64Coder;

/**
 * Utility class to generate hash code
 */
public class HashUtils {
    private static final TraceComponent tc = Tr.register(HashUtils.class);
    private static String DEFAULT_ALGORITHM = "SHA-256";
    private static Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    /**
     * generate hash code by using SHA-256
     * If there is some error, log the error.
     */
    public static String digest(String input) {
        return digest(input, DEFAULT_ALGORITHM);
    }

    /**
     * generate hash code by using specified algorithm
     * If there is some error, log the error.
     */
    protected static String digest(String input, String algorithm) {
        return digest(input, algorithm, DEFAULT_CHARSET);
    }

    /**
     * generate hash code by using specified algorithm and character set.
     * If there is some error, log the error.
     */
    protected static String digest(String input, String algorithm, Charset charset) {
        MessageDigest md;
        String output = null;
        if (input != null && input.length() > 0) {
            try {
                md = MessageDigest.getInstance(algorithm);
                md.update(input.getBytes(charset));
                output = Base64Coder.toString(Base64Coder.base64Encode(md.digest()));
            } catch (NoSuchAlgorithmException nsae) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exception instanciating MessageDigest. The algorithm is " + algorithm + nsae);
                }
                throw new RuntimeException("Exception instanciating MessageDigest : " + nsae);
            }
        }
        return output;
    }
}
