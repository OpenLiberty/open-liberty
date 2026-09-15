/*******************************************************************************
 * Copyright (c) 1997, 2024 IBM Corporation and others.
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

/**
 * Common interface for encrypting and decrypting LTPA key material.
 * Implementations include {@link KeyEncryptor} (password-derived cipher key)
 * and {@link AesLTPAKeyEncryptor} (raw AES key).
 */
public interface LTPAKeyEncryptor {

    /**
     * Encrypt the given data.
     *
     * @param data The plaintext data to encrypt
     * @return The encrypted data
     * @throws Exception if encryption fails
     */
    byte[] encrypt(byte[] data) throws Exception;

    /**
     * Decrypt the given encrypted data.
     *
     * @param encryptedData The encrypted data to decrypt
     * @return The decrypted data
     * @throws Exception if decryption fails
     */
    byte[] decrypt(byte[] encryptedData) throws Exception;
}
