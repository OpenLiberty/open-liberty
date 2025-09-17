/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.xml.security.encryption;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;

import org.apache.xml.security.utils.EncryptionConstants;

public final class XMLCipherUtil {

    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(XMLCipherUtil.class);

    private static final boolean gcmUseIvParameterSpec =
        AccessController.doPrivileged((PrivilegedAction<Boolean>)
            () -> Boolean.getBoolean("org.apache.xml.security.cipher.gcm.useIvParameterSpec"));

    /**
     * Build an <code>AlgorithmParameterSpec</code> instance used to initialize a <code>Cipher</code> instance
     * for block cipher encryption and decryption.
     *
     * @param algorithm the XML encryption algorithm URI
     * @param iv the initialization vector
     * @return the newly constructed AlgorithmParameterSpec instance, appropriate for the
     *         specified algorithm
     */
    public static AlgorithmParameterSpec constructBlockCipherParameters(String algorithm, byte[] iv) {
        if (EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES128_GCM.equals(algorithm)
                || EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES192_GCM.equals(algorithm)
                || EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES256_GCM.equals(algorithm)) {
            return constructBlockCipherParametersForGCMAlgorithm(algorithm, iv);
        } else {
            LOG.debug("Saw non-AES-GCM mode block cipher, returning IvParameterSpec: {}", algorithm);
            return new IvParameterSpec(iv);
        }
    }

    public static AlgorithmParameterSpec constructBlockCipherParameters(boolean gcmAlgorithm, byte[] iv) {
        if (gcmAlgorithm) {
            return constructBlockCipherParametersForGCMAlgorithm("AES/GCM/NoPadding", iv);
        } else {
            LOG.debug("Saw non-AES-GCM mode block cipher, returning IvParameterSpec");
            return new IvParameterSpec(iv);
        }
    }

    private static AlgorithmParameterSpec constructBlockCipherParametersForGCMAlgorithm(String algorithm, byte[] iv) {
        if (gcmUseIvParameterSpec) {
            // This override allows to support Java 1.7+ with (usually older versions of) third-party security
            // providers which support or even require GCM via IvParameterSpec rather than GCMParameterSpec,
            // e.g. BouncyCastle <= 1.49 (really <= 1.50 due to a semi-related bug).
            LOG.debug("Saw AES-GCM block cipher, using IvParameterSpec due to system property override: {}", algorithm);
            return new IvParameterSpec(iv);
        }

        LOG.debug("Saw AES-GCM block cipher, attempting to create GCMParameterSpec: {}", algorithm);

        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
        LOG.debug("Successfully created GCMParameterSpec");
        return gcmSpec;
    }
}
