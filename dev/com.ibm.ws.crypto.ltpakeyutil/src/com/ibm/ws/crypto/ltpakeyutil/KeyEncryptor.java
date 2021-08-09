/*******************************************************************************
 * Copyright (c) 1997, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.crypto.ltpakeyutil;

import java.security.MessageDigest;

/**
 * A package local class for performing encryption and decryption of keys
 * based on admin's password
 */
public class KeyEncryptor {

    private static final String MESSAGE_DIGEST_ALGORITHM = "SHA";
    private static final String DES_ECB_CIPHER = "DESede/ECB/PKCS5Padding";

    private final byte[] desKey;

    /**
     * A KeyEncryptor constructor.
     * 
     * @param password The key password
     */
    public KeyEncryptor(byte[] password) throws Exception {
        MessageDigest md = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM);
        byte[] digest = md.digest(password);
        desKey = new byte[24];
        System.arraycopy(digest, 0, desKey, 0, digest.length);
        desKey[20] = (byte) 0x00;
        desKey[21] = (byte) 0x00;
        desKey[22] = (byte) 0x00;
        desKey[23] = (byte) 0x00;
    }

    /**
     * Decrypt the key.
     * 
     * @param encryptedKey The encrypted key
     * @return The decrypted key
     */
    public byte[] decrypt(byte[] encryptedKey) throws Exception {
        return LTPACrypto.decrypt(encryptedKey, desKey, DES_ECB_CIPHER);
    }

    public byte[] encrypt(byte[] key) throws Exception {
        return LTPACrypto.encrypt(key, desKey, DES_ECB_CIPHER);
    }
}
