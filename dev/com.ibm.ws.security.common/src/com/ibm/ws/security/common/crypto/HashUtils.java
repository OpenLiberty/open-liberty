/*******************************************************************************
 * Copyright (c) 2016,2022 IBM Corporation and others.
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
package com.ibm.ws.security.common.crypto;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.common.TraceConstants;
import com.ibm.ws.common.crypto.CryptoUtils;

public class HashUtils {

    private static final TraceComponent tc = Tr.register(HashUtils.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private static String DEFAULT_ALGORITHM = CryptoUtils.MESSAGE_DIGEST_ALGORITHM_SHA_256;
    private static Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    /**
     * generate hash code by using SHA-256 If there is some error, log the
     * error.
     */
    @Sensitive
    public static String digest(@Sensitive String input) {
        return digest(input, DEFAULT_ALGORITHM);
    }

    /**
     * generate hash code by using specified algorithm If there is some error,
     * log the error.
     */
    @Sensitive
    protected static String digest(@Sensitive String input, String algorithm) {
        return digest(input, algorithm, DEFAULT_CHARSET);
    }

    @Sensitive
    protected static String digest(@Sensitive String input, String algorithm, String charsetName) {
        try {
            return digest(input, algorithm, Charset.forName(charsetName));
        } catch (UnsupportedCharsetException uce) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception converting String object : " + uce);
            }
            throw new RuntimeException("Exception converting String object : " + uce);
        }
    }

    /**
     * generate hash code by using specified algorithm and character set. If
     * there is some error, log the error.
     */
    @Sensitive
    private static String digest(@Sensitive String input, String algorithm, Charset charset) {
        MessageDigest md;
        String output = null;
        if (input != null && input.length() > 0) {
            try {
                md = MessageDigest.getInstance(algorithm);
                md.update(input.getBytes(charset));
                output = Base64.getEncoder().encodeToString(md.digest());
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
