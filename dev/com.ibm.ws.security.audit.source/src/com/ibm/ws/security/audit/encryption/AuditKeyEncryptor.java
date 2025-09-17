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
    private final String algorithm = CryptoUtils.MESSAGE_DIGEST_ALGORITHM_SHA256;
    byte[] password;
    byte[] passwordDigestBytes;
    AuditCrypto des;

    public AuditKeyEncryptor(byte[] password) {
        this.password = password;
        java.security.MessageDigest md = null;
        try {
            md = java.security.MessageDigest.getInstance(algorithm);
            passwordDigestBytes = new byte[CryptoUtils.AES_256_KEY_LENGTH_BYTES];
            byte[] digest = md.digest(this.password);
            ByteArray.copy(digest, 0, digest.length, passwordDigestBytes, 0);

        } catch (java.security.NoSuchAlgorithmException e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.encryption.AuditKeyEncryptor", "21", this);
        }
        des = new AuditCrypto();

    }

    public byte[] decrypt(byte[] encrKey) {
        return des.decrypt(encrKey, passwordDigestBytes);
    }

    public byte[] encrypt(byte[] key) {
        return des.encrypt(key, passwordDigestBytes);
    }
}