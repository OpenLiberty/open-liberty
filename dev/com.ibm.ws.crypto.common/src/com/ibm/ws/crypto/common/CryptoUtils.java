/*******************************************************************************
* Copyright (c) 2024 IBM Corporation and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License 2.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*******************************************************************************/
package com.ibm.ws.crypto.common;

import java.util.HashMap;
import java.util.Map;

public class CryptoUtils {
    
    private static Map<String, String> secureAlternative = new HashMap<>();
    static {
        secureAlternative.put("SHA", "SHA256");
        secureAlternative.put("SHA1", "SHA256");
        secureAlternative.put("SHA-1", "SHA256");
        secureAlternative.put("SHA128", "SHA256");
        secureAlternative.put("MD5", "SHA256");
    }

    /**
     * Answers whether a crypto algorithm is considered insecure.
     * 
     * @param algorithm   The algorithm to check.
     * @return            True if the algorithm is considered insecure, false otherwise.
     */
    public static boolean isAlgorithmInsecure(String algorithm) {
        return secureAlternative.containsKey(algorithm);
    }

    /**
     * Returns a secure crypto algorithm to use in place of the given one.
     * 
     * @param algorithm   The insecure algorithm to be replaced.
     * @return            A secure replacement algorithm.  If there is none a null is returned.
     */
    public static String getSecureAlternative(String algorithm) {
        return secureAlternative.get(algorithm);
    }
}
