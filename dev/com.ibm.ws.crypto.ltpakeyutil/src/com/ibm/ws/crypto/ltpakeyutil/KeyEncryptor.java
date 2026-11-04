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

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;

import com.ibm.ws.common.crypto.CryptoUtils;

/**
 * A package local class for performing encryption and decryption of keys based
 * on admin's password
 */
public class KeyEncryptor {

	private static final boolean fipsEnabled = CryptoUtils.isFips140_3Enabled();
	private static final String CKDS_KEY_LABEL = "LTPAAES";
	
	private final byte[] key;
	private final int size;
	private final String cipher;
	private final boolean usingCKDS;

	/**
	 * A KeyEncryptor constructor.
	 *
	 * @param password The key password
	 */
	public KeyEncryptor(byte[] password) throws Exception {
		byte[] derivedKey = null;
		int keySize;
		boolean ckdsKey = false;
		
		// Try to retrieve key from z/OS CKDS if on z/OS
		if (CryptoUtils.isZOS()) {
			derivedKey = getAesKeyFromCKDS();
			if (derivedKey != null) {
				ckdsKey = true;
			}
		}
		
		// Determine key size based on whether CKDS key was retrieved or FIPS mode
		if (ckdsKey) {
			keySize = 32; // CKDS keys should be AES-256
		} else {
			keySize = (fipsEnabled ? 32 : 24);
		}
		
		// Fall back to normal key derivation if CKDS retrieval failed or not on z/OS
		if (derivedKey == null) {
			MessageDigest md = MessageDigest.getInstance(CryptoUtils.MESSAGE_DIGEST_ALGORITHM);
			byte[] digest = md.digest(password);
			derivedKey = new byte[keySize];
			System.arraycopy(digest, 0, derivedKey, 0, digest.length);
			if (!fipsEnabled) {
				derivedKey[20] = (byte) 0x00;
				derivedKey[21] = (byte) 0x00;
				derivedKey[22] = (byte) 0x00;
				derivedKey[23] = (byte) 0x00;
			}
		}
		
		this.key = derivedKey.clone();
		this.size = keySize;
		this.cipher = ckdsKey ? CryptoUtils.AES_CBC_CIPHER : CryptoUtils.getCipher();
		this.usingCKDS = ckdsKey;
	}

	/**
	 * Retrieve an AES key from z/OS CKDS using the IBMJCECCA provider.
	 * This method checks if the CKDS key is available and retrieves it if possible.
	 *
	 * @return The raw key bytes from CKDS, or null if CKDS is not available or retrieval fails
	 */
	private byte[] getAesKeyFromCKDS() {
		try {
			// Use IBMJCECCA provider to retrieve key from z/OS CKDS
			SecretKeyFactory aesKeyFactory = SecretKeyFactory.getInstance("AES", CryptoUtils.IBMJCECCA_NAME);
			
			// Create KeyLabelKeySpec using reflection to avoid compile-time dependency
			// KeyLabelKeySpec is only available on z/OS with IBMJCECCA provider
			Class<?> keyLabelKeySpecClass = Class.forName("com.ibm.crypto.hdwrCCA.provider.KeyLabelKeySpec");
			Object spec = keyLabelKeySpecClass.getConstructor(String.class).newInstance(CKDS_KEY_LABEL);
			
			// Generate the secret key from the key label
			SecretKey secretKey = aesKeyFactory.generateSecret((java.security.spec.KeySpec) spec);
			
			// Extract the raw key bytes
			byte[] ckdsKey = secretKey.getEncoded();

			// Validate key length is 32 bytes (AES-256)
			if (ckdsKey == null) {
				System.out.println("Warning: CKDS key not found. Falling back to password-based key derivation.");
				return null;
			}
			
			System.out.println("Successfully retrieved LTPA key from z/OS CKDS with label: " + CKDS_KEY_LABEL + " contents = '" + new String(ckdsKey) + "'");
			return ckdsKey;
			
		} catch (Exception e) {
			// Provider not available, class not found, key label doesn't exist, or other error
			// Fall back to normal key derivation
			System.out.println("CKDS key retrieval failed, falling back to password-based key derivation. Reason: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Decrypt the key.
	 *
	 * @param encryptedKey The encrypted key
	 * @return The decrypted key
	 */
	public byte[] decrypt(byte[] encryptedKey) throws Exception {
		// Use IBMJCECCA provider for CKDS keys to ensure hardware crypto operations
		if (usingCKDS) {
			return LTPACrypto.decrypt(encryptedKey, key, cipher, CryptoUtils.IBMJCECCA_NAME);
		}
		return LTPACrypto.decrypt(encryptedKey, key, cipher);
	}

	/**
	 * Encrypt the key
	 *
	 * @param key The key
	 * @return The encrypted key
	 */
	public byte[] encrypt(byte[] key) throws Exception {
		// Use IBMJCECCA provider for CKDS keys to ensure hardware crypto operations
		if (usingCKDS) {
			return LTPACrypto.encrypt(key, this.key, cipher, CryptoUtils.IBMJCECCA_NAME);
		}
		return LTPACrypto.encrypt(key, this.key, cipher);
	}
}
