/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.jwk;

import java.security.SecureRandom;
import java.security.Security;
import java.util.Random;

/**
 * TODO: reduce duplication with OauthUtil
 */
public class Util {

    static final String JCEPROVIDER_IBM = "IBMJCE";

    static final String SECRANDOM_IBM = "IBMSecureRandom";

    static final String SECRANDOM_SHA1PRNG = "SHA1PRNG";

    static final String UTF_ENCODING = "UTF-8";

    // tune up the performance of random
    public static String generateRandom() {
        return getRandom(20);
    }

    /**
     * Generates a random alphanumeric string of length n to be used for OAuth
     * 2.0 keys, tokens, secrets etc
     *
     * @param length
     * @return
     */
    public static String getRandom(int length) {
        StringBuffer result = new StringBuffer(length);
        final char[] chars = new char[] {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
                'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
                'U', 'V', 'W', 'X', 'Y', 'Z',
                'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
                'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
                'u', 'v', 'w', 'x', 'y', 'z'
        };
        Random r = getRandom();

        for (int i = 0; i < length; i++) {
            int n = r.nextInt(62);
            result.append(chars[n]);
        }

        return result.toString();
    }

    static Random getRandom() {
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

}
