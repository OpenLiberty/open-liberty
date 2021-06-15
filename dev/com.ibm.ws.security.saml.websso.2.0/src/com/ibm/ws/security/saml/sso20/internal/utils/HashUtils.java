/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.internal.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.common.internal.encoder.Base64Coder;

/**
 * Utility class to generate hash code
 */
public class HashUtils {
    private static final TraceComponent tc = Tr.register(HashUtils.class);
    private static String DEFAULT_ALGORITHM = "SHA-256";
    private static String DEFAULT_CHARSET = "UTF-8";

    /**
     * generate hash code by using SHA-256
     * If there is some error, log the error.
     */
    @Sensitive
    public static String digest(String input) {
        return digest(input, DEFAULT_ALGORITHM);
    }

    /**
     * generate hash code by using specified algorithm
     * If there is some error, log the error.
     */
    @Sensitive
    protected static String digest(String input, String algorithm) {
        return digest(input, algorithm, DEFAULT_CHARSET);
    }

    /**
     * generate hash code by using specified algorithm and character set.
     * If there is some error, log the error.
     */
    @Sensitive
    protected static String digest(String input, String algorithm, String charset) {
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
            } catch (UnsupportedEncodingException uee) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exception converting String object : " + uee);
                }
                throw new RuntimeException("Exception converting String object : " + uee);
            }
        }
        return output;
    }
}
