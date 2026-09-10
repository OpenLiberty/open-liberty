/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.crypto.ltpakeyutil;

import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;

import com.ibm.ws.common.crypto.CryptoUtils;

/**
 * An {@link LTPAKeyEncryptor} that encrypts and decrypts LTPA key material
 * using a raw AES {@link Key}. The cipher is always {@code AES/CBC/PKCS5Padding}.
 *
 * <p>A fresh random 16-byte IV is generated on every {@link #encrypt} call and
 * prepended to the ciphertext. {@link #decrypt} reads the first 16 bytes as the
 * IV before decrypting the remainder. This avoids a fixed all-zero IV while
 * requiring no access to the key's raw bytes, which makes it compatible with
 * hardware-backed keys (e.g. ICSF/CKDS) that return {@code null} from
 * {@code getEncoded()}.
 *
 * <p>This class lives in {@code com.ibm.ws.crypto.ltpakeyutil} so that it can be
 * used from both {@code com.ibm.ws.security.token.ltpa} and
 * {@code com.ibm.ws.security.utility} without a cross-bundle dependency.
 */
public class AesLTPAKeyEncryptor implements LTPAKeyEncryptor {

    private static final String AES_CIPHER = CryptoUtils.AES_CBC_CIPHER;

    private static final int IV_LENGTH = CryptoUtils.AES_IV_LENGTH_BYTES;

    private final Key aesKey;

    /**
     * Constructs an {@code AesLTPAKeyEncryptor} backed by the given AES key.
     * The key is used as-is; its raw bytes are never accessed.
     *
     * @param aesKey the AES key to use for encryption and decryption
     */
    public AesLTPAKeyEncryptor(Key aesKey) {
        this.aesKey = aesKey;
    }

    /** {@inheritDoc} */
    @Override
    public byte[] encrypt(byte[] data) throws Exception {
        byte[] iv = CryptoUtils.generateRandomBytes(IV_LENGTH);
        Cipher cipher = Cipher.getInstance(AES_CIPHER);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, new IvParameterSpec(iv));
        byte[] encrypted = cipher.doFinal(data);
        byte[] result = new byte[IV_LENGTH + encrypted.length];
        System.arraycopy(iv, 0, result, 0, IV_LENGTH);
        System.arraycopy(encrypted, 0, result, IV_LENGTH, encrypted.length);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public byte[] decrypt(byte[] encryptedData) throws Exception {
        IvParameterSpec iv = new IvParameterSpec(encryptedData, 0, IV_LENGTH);
        Cipher cipher = Cipher.getInstance(AES_CIPHER);
        cipher.init(Cipher.DECRYPT_MODE, aesKey, iv);
        return cipher.doFinal(encryptedData, IV_LENGTH, encryptedData.length - IV_LENGTH);
    }
}
