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

import java.security.MessageDigest;

import com.ibm.ws.common.crypto.CryptoUtils;

/**
 * A package local class for performing encryption and decryption of keys based
 * on admin's password
 */
public class KeyEncryptor {

	private static final boolean fipsEnabled = CryptoUtils.isFips140_3Enabled();
	private static final int size = (fipsEnabled ? 32 : 24);
	private static final String CIPHER = CryptoUtils.getCipher();
	private final byte[] key;

	/**
	 * A KeyEncryptor constructor.
	 *
	 * @param password The key password
	 */
	public KeyEncryptor(byte[] password) throws Exception {
		MessageDigest md = MessageDigest.getInstance(CryptoUtils.MESSAGE_DIGEST_ALGORITHM);
		byte[] digest = md.digest(password);
		key = new byte[size];
		System.arraycopy(digest, 0, key, 0, digest.length);
		if (!fipsEnabled) {
			key[20] = (byte) 0x00;
			key[21] = (byte) 0x00;
			key[22] = (byte) 0x00;
			key[23] = (byte) 0x00;
		}
	}

	/**
	 * Decrypt the key.
	 *
	 * @param encryptedKey The encrypted key
	 * @return The decrypted key
	 */
	public byte[] decrypt(byte[] encryptedKey) throws Exception {
		return LTPACrypto.decrypt(encryptedKey, key, CIPHER);
	}

	/**
	 * Encrypt the key
	 *
	 * @param key The key
	 * @return The encrypted key
	 */
	public byte[] encrypt(byte[] key) throws Exception {
		return LTPACrypto.encrypt(key, this.key, CIPHER);
	}
}
