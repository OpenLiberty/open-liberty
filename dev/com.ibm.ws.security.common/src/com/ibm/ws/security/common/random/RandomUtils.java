/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.random;

import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.TraceConstants;

public class RandomUtils {

    private static final TraceComponent tc = Tr.register(RandomUtils.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    private static final char[] alphaNumChars = new char[] {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
            'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
            'U', 'V', 'W', 'X', 'Y', 'Z',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
            'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
            'u', 'v', 'w', 'x', 'y', 'z'
    };

    static final String JCEPROVIDER_IBM = "IBMJCE";
    static final String SECRANDOM_IBM = "IBMSecureRandom";
    static final String SECRANDOM_SHA1PRNG = "SHA1PRNG";

    /**
     * Generates a random alphanumeric string of length n.
     * 
     * @param length
     * @return
     */
    public static String getRandomAlphaNumeric(int length) {
        if (length <= 0) {
            return "";
        }

        Random r = getRandom();

        StringBuffer result = new StringBuffer(length);

        for (int i = 0; i < length; i++) {
            int n = r.nextInt(alphaNumChars.length);
            result.append(alphaNumChars[n]);
        }

        return result.toString();
    }

    public static Random getRandom() {
        Random result = null;
        try {
            if (Security.getProvider(JCEPROVIDER_IBM) != null) {
                result = SecureRandom.getInstance(SECRANDOM_IBM);
            } else {
                result = SecureRandom.getInstance(SECRANDOM_SHA1PRNG);
            }
        } catch (Exception e) {
            result = new Random();
        }
        return result;
    }

    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Randomly chooses and returns one of the provided options.
     * 
     * @param options
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T getRandomSelection(List<T> options) {
        return (T) getRandomSelection(options.toArray(new Object[options.size()]));
    }

    /**
     * Randomly chooses and returns one of the provided options.
     * 
     * @param options
     * @return
     */
    public static <T> T getRandomSelection(T... options) {
        if (options == null || options.length == 0) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "No options provided to random selection, returning null");
            }
            return null;
        }
        if (options.length == 1) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Only one option provided to random selection, returning it: [" + options[0] + "]");
            }
            return options[0];
        }

        Random rand = new Random();
        Integer index = rand.nextInt(options.length);

        T entry = options[index];
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Chose random selection: [" + ((entry == null) ? null : entry) + "]");
        }
        return entry;
    }

    /**
     * Randomly chooses and returns one of the provided array options.
     * 
     * @param options
     * @return
     */
    public static <T> T[] getRandomSelection(T[]... options) {
        if (options == null || options.length == 0) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "No options provided to random selection, returning null");
            }
            return null;
        }
        if (options.length == 1) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Only one option provided to random selection, returning it: " + Arrays.toString(options[0]));
            }
            return options[0];
        }

        Random rand = new Random();
        Integer index = rand.nextInt(options.length);

        T[] entry = options[index];
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Chose random selection: " + ((entry == null) ? null : Arrays.toString(entry)));
        }
        return entry;
    }
}
