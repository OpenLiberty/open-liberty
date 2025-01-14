/*******************************************************************************
 * Copyright (c) 2018, 2024 IBM Corporation and others.
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
package com.ibm.ws.security.audit.encryption;

import com.ibm.ws.common.crypto.CryptoUtils;
import com.ibm.ws.security.audit.source.utils.ByteArray;

/**
 * A package local class for performing encryption and decryption of keys based on a key
 */
public class AuditKeyEncryptor {
    private String algorithm = CryptoUtils.MESSAGE_DIGEST_ALGORITHM_SHA;
    private int len = 24;
    byte[] password;
    byte[] desKey;
    AuditCrypto des;

    public AuditKeyEncryptor(byte[] password) {
        this.password = password;
        java.security.MessageDigest md = null;
        try {
            if (CryptoUtils.isFips140_3Enabled()) {
                algorithm = CryptoUtils.MESSAGE_DIGEST_ALGORITHM_SHA256;
                len = 32;
            }

            md = java.security.MessageDigest.getInstance(algorithm);
            desKey = new byte[len];
            byte[] digest = md.digest(this.password);
            ByteArray.copy(digest, 0, digest.length, desKey, 0);
            desKey[20] = (byte) 0x00;
            desKey[21] = (byte) 0x00;
            desKey[22] = (byte) 0x00;
            desKey[23] = (byte) 0x00;
        } catch (java.security.NoSuchAlgorithmException e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.ltpa.KeyEncryptor.KeyEncryptor", "21", this);
        }
        String provider = null;
        des = new AuditCrypto();

    }

    public byte[] decrypt(byte[] encrKey) {
        return des.decrypt(encrKey, desKey);
    }

    public byte[] encrypt(byte[] key) {
        return des.encrypt(key, desKey);
    }
}