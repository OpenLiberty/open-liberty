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
 * <p>A fixed well-known 16-byte IV is used for all operations. Using a fixed IV
 * is intentional: the LTPA key file is a configuration artifact (not user data),
 * and the key itself provides the necessary entropy. Hardware-backed keys
 * (e.g. ICSF/CKDS) return {@code null} from {@code getEncoded()}, so deriving
 * the IV from key bytes is not possible in the general case.
 *
 * <p>This class lives in {@code com.ibm.ws.crypto.ltpakeyutil} so that it can be
 * used from both {@code com.ibm.ws.security.token.ltpa} and
 * {@code com.ibm.ws.security.utility} without a cross-bundle dependency.
 */
public class AesLTPAKeyEncryptor implements LTPAKeyEncryptor {

    /** AES/CBC cipher — same value as {@code CryptoUtils.AES_CBC_CIPHER}. */
    private static final String AES_CIPHER = CryptoUtils.AES_CBC_CIPHER;

    /**
     * Fixed 16-byte IV shared by all instances. Hardware-backed AES keys cannot
     * expose their raw bytes to derive a key-dependent IV, so a well-known
     * constant is used instead.
     */
    private static final IvParameterSpec FIXED_IV = new IvParameterSpec(new byte[16]);

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
        Cipher cipher = Cipher.getInstance(AES_CIPHER);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, FIXED_IV);
        return cipher.doFinal(data);
    }

    /** {@inheritDoc} */
    @Override
    public byte[] decrypt(byte[] encryptedData) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_CIPHER);
        cipher.init(Cipher.DECRYPT_MODE, aesKey, FIXED_IV);
        return cipher.doFinal(encryptedData);
    }
}
