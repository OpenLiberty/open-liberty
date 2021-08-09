/*******************************************************************************
 * Copyright (c) 1997, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openid20.consumer;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class MessageDigestUtil {

    private static final TraceComponent tc = Tr.register(MessageDigestUtil.class);

    private static Object locker = new Object(); // @GK1
    private static MessageDigest md = null;
    private static final char[] map = new char[] {
                                                   '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };
    private static final SecureRandom srandom = new SecureRandom();
    static {
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Internal error initializing message digest", e);
            }
        }
    }

    /**
     * Calculate the message digest of the given String and convert
     * it to a hexadecimal String
     *
     * @param value input String
     * @return message digest hexadecimal String
     */
    public static String getDigest() {
        String digest = null;
        try {
            byte[] digestBytes = null;
            synchronized (locker) {
                md.reset();
                byte[] seed = new byte[20];
                srandom.nextBytes(seed);
                md.update(seed);
                digestBytes = md.digest();

            }
            digest = toHex(digestBytes);
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Internal error calculating message digest of :", e);
            }
        }
        return digest;
    }

    /**
     * Convert a byte array to a String of hex characters
     *
     * @param bytes
     * @return Hexadecimal String
     */
    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(map[b >>> 4 & 0x0f]);
            sb.append(map[b & 0x0f]);
        }
        return sb.toString();
    }
}
