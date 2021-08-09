/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.common.internal.encoder.Base64Coder;

/**
 * Utility class to generate hash code
 * TODO: REPLACE WITH CALLS TO THIS CLASS IN THE NEW BUNDLE
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
    @Trivial
    public static String digest(@Sensitive String input) {
        if (input == null || input.isEmpty())
            return null;
        return digest(input, DEFAULT_ALGORITHM, DEFAULT_CHARSET);
    }

    /**
     * generate hash code by using specified algorithm and character set.
     * If there is some error, log the error.
     */
    @Sensitive
    @Trivial
    protected static String digest(@Sensitive String input, @Sensitive String algorithm, String charset) {
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

    @Sensitive
    @Trivial
    public static String getStrHashCode(String input) {
        if (input != null && !input.isEmpty()) {
            int hashCode = input.hashCode();
            if (hashCode < 0) {
                hashCode = hashCode * -1;
                return "n" + hashCode;
            } else {
                return "p" + hashCode;
            }
        } else {
            // This should not happen
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "hash() gets a null or empty parameter");
            }
            return "";
        }

    }

    /**
     * @param clientConfig
     * @param state
     * @return
     */
    @Sensitive
    @Trivial
    public static String getCookieName(String prefix, ConvergedClientConfig clientConfig, String state) {
        String newValue = state + clientConfig.getId();
        String newName = getStrHashCode(newValue);
        return prefix + newName;
    }

    /**
     * @param clientConfig
     * @param state
     * @return
     */
    @Sensitive
    @Trivial
    public static String createStateCookieValue(ConvergedClientConfig clientConfig, String state) {
        String timestamp = state.substring(0, OidcUtil.TIMESTAMP_LENGTH);
        String newValue = state + clientConfig.getClientSecret(); // state already has a timestamp in it
        String value = digest(newValue);
        return timestamp + value;
    }

}
