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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;
import java.util.List;

public class MessageDigestUtils {

    // Message Digest algorithms
    public final static String MESSAGE_DIGEST_ALGORITHM_SHA256 = "SHA-256";
    public final static String MESSAGE_DIGEST_ALGORITHM_SHA384 = "SHA-384";
    public final static String MESSAGE_DIGEST_ALGORITHM_SHA512 = "SHA-512";
    public final static String MESSAGE_DIGEST_ALGORITHM_SHA = "SHA";

    public static boolean fipsEnabled = FipsUtils.isFIPSEnabled();
    public static final String MESSAGE_DIGEST_ALGORITHM = (fipsEnabled ? MessageDigestUtils.MESSAGE_DIGEST_ALGORITHM_SHA256 : MessageDigestUtils.MESSAGE_DIGEST_ALGORITHM_SHA);
    /**
     * List of supported Message Digest Algorithms.
     */
    private static final List<String> supportedMessageDigestAlgorithms = Arrays.asList(
                                                                                       MESSAGE_DIGEST_ALGORITHM_SHA256,
                                                                                       MESSAGE_DIGEST_ALGORITHM_SHA384,
                                                                                       MESSAGE_DIGEST_ALGORITHM_SHA512);

    public static String getMessageDigestAlgorithm() {
        return MESSAGE_DIGEST_ALGORITHM_SHA256;
    }

    public static MessageDigest getMessageDigest() throws NoSuchAlgorithmException {
        return getMessageDigest(getMessageDigestAlgorithm());
    }

    public static MessageDigest getMessageDigest(String algorithm) throws NoSuchAlgorithmException {
        if (!supportedMessageDigestAlgorithms.contains(algorithm))
            throw new NoSuchAlgorithmException(String.format("Algorithm %s is not supported", algorithm));
        return MessageDigest.getInstance(algorithm);
    }

    public static MessageDigest getMessageDigestForLTPA() {
        MessageDigest md1 = null;
        try {
            if (fipsEnabled && CryptoProvider.isOpenJCEPlusFIPSAvailable()) {
                md1 = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM_SHA256,
                                                CryptoProvider.OPENJCE_PLUS_FIPS_NAME);
            } else if (fipsEnabled && CryptoProvider.isIBMJCEPlusFIPSAvailable()) {
                md1 = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM_SHA256,
                                                CryptoProvider.IBMJCE_PLUS_FIPS_NAME);
            } else if (CryptoProvider.isOpenJCEPlusAvailable()) {
                md1 = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM_SHA,
                                                CryptoProvider.OPENJCE_PLUS_NAME);
            } else if (CryptoProvider.isIBMJCEAvailable()) {
                md1 = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM_SHA,
                                                CryptoProvider.IBMJCE_NAME);
            } else {
                md1 = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM_SHA);
            }

        } catch (NoSuchAlgorithmException e) {
            // instrumented ffdc
        } catch (NoSuchProviderException e) {
            // instrumented ffdc;
        }

        return md1;
    }
}
