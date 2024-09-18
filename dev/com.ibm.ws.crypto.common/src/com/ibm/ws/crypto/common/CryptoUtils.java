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

    public static final String SIGNATURE_ALGORITHM_SHA1WITHRSA = "SHA1withRSA";
    public static final String SIGNATURE_ALGORITHM_SHA256WITHRSA = "SHA256withRSA";
//    private static final String signatureAlgorithm = getSignatureAlgorithm();

    public static final String CRYPTO_ALGORITHM_RSA = "RSA";

    public static final String ENCRYPT_ALGORITHM_DESEDE = "DESede";
    public static final String ENCRYPT_ALGORITHM_RSA = "RSA";
//    private static final String encryptAlgorithm = getEncryptionAlgorithm();

    public static final String AES_GCM_CIPHER = "AES/GCM/NoPadding";
    public static final String DES_ECB_CIPHER = "DESede/ECB/PKCS5Padding";
    public static final String AES_CBC_CIPHER = "AES/CBC/PKCS5Padding";

    private static final boolean fipsEnabled = FipsUtils.isFIPSEnabled();

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
     * @param algorithm The algorithm to check.
     * @return True if the algorithm is considered insecure, false otherwise.
     */
    public static boolean isAlgorithmInsecure(String algorithm) {
        return secureAlternative.containsKey(algorithm);
    }

    /**
     * Returns a secure crypto algorithm to use in place of the given one.
     *
     * @param algorithm The insecure algorithm to be replaced.
     * @return A secure replacement algorithm. If there is none a null is returned.
     */
    public static String getSecureAlternative(String algorithm) {
        return secureAlternative.get(algorithm);
    }

    public static String getSignatureAlgorithm() {
        if (fipsEnabled && (CryptoProvider.isOpenJCEPlusFIPSAvailable() || CryptoProvider.isIBMJCEPlusFIPSAvailable()))
            return SIGNATURE_ALGORITHM_SHA256WITHRSA;
        else
            return SIGNATURE_ALGORITHM_SHA1WITHRSA;
    }

    public static String getEncryptionAlgorithm() {
        if (fipsEnabled && (CryptoProvider.isOpenJCEPlusFIPSAvailable() || CryptoProvider.isIBMJCEPlusFIPSAvailable()))
            return ENCRYPT_ALGORITHM_RSA;
        else
            return ENCRYPT_ALGORITHM_DESEDE;
    }

}
