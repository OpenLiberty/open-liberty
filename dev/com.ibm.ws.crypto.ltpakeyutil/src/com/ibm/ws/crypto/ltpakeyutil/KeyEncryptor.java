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
	private final SecretKey ckdsSecretKey; // Store the SecretKey object for ICSF labels
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
		SecretKey ckdsKey = null;
		int keySize;
		boolean useCKDS = false;
		
		// Try to retrieve key from z/OS CKDS if on z/OS
		if (CryptoUtils.isZOS()) {
			ckdsKey = getSecretKeyFromCKDS();
			if (ckdsKey != null) {
				useCKDS = true;
			}
		}
		
		// Determine key size based on whether CKDS key was retrieved or FIPS mode
		if (useCKDS) {
			keySize = 32; // CKDS keys should be AES-256
			// Use ICSF hardware to derive a 32-byte key from the password
			// This creates a deterministic key that can be used for encryption/decryption
			derivedKey = deriveKeyFromPassword(ckdsKey, password, keySize);
			this.key = derivedKey;
			this.ckdsSecretKey = null; // We don't need the SecretKey anymore
			this.usingCKDS = false; // Use normal byte array encryption
		} else {
			keySize = (fipsEnabled ? 32 : 24);
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
			this.key = derivedKey.clone();
			this.ckdsSecretKey = null;
			this.usingCKDS = false;
		}
		
		this.size = keySize;
		this.cipher = CryptoUtils.getCipher();
	}

	/**
	 * Retrieve an AES SecretKey from z/OS CKDS using the IBMJCECCA provider.
	 * This method checks if the CKDS key is available and retrieves it if possible.
	 *
	 * IMPORTANT: For ICSF labels, the SecretKey object must be used directly for crypto operations.
	 * Calling getEncoded() on an ICSF label returns the label string, not the actual key bytes.
	 *
	 * @return The SecretKey object from CKDS, or null if CKDS is not available or retrieval fails
	 */
	private SecretKey getSecretKeyFromCKDS() {
		try {
			// Use IBMJCECCA provider to retrieve key from z/OS CKDS
			SecretKeyFactory aesKeyFactory = SecretKeyFactory.getInstance("AES", CryptoUtils.IBMJCECCA_NAME);
			
			// Create KeyLabelKeySpec using reflection to avoid compile-time dependency
			// KeyLabelKeySpec is only available on z/OS with IBMJCECCA provider
			Class<?> keyLabelKeySpecClass = Class.forName("com.ibm.crypto.hdwrCCA.provider.KeyLabelKeySpec");
			Object spec = keyLabelKeySpecClass.getConstructor(String.class).newInstance(CKDS_KEY_LABEL);
			
			// Generate the secret key from the key label
			SecretKey secretKey = aesKeyFactory.generateSecret((java.security.spec.KeySpec) spec);
			
			if (secretKey == null) {
				System.out.println("Warning: CKDS key not found. Falling back to password-based key derivation.");
				return null;
			}
			
			System.out.println("Successfully retrieved LTPA key from z/OS CKDS with label: " + CKDS_KEY_LABEL);
			return secretKey;
			
		} catch (Exception e) {
			// Provider not available, class not found, key label doesn't exist, or other error
			// Fall back to normal key derivation
			System.out.println("CKDS key retrieval failed, falling back to password-based key derivation. Reason: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Derive a deterministic encryption key from a password using the ICSF hardware key.
	 * This uses the ICSF key to encrypt the password, creating a 32-byte derived key.
	 *
	 * @param ckdsKey The ICSF SecretKey
	 * @param password The password bytes
	 * @param keySize The desired key size (32 bytes for AES-256)
	 * @return A 32-byte derived key
	 */
	private byte[] deriveKeyFromPassword(SecretKey ckdsKey, byte[] password, int keySize) throws Exception {
		try {
			// Use the ICSF key to encrypt the password, creating a deterministic derived key
			javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(CryptoUtils.AES_CBC_CIPHER, CryptoUtils.IBMJCECCA_NAME);
			
			// Create a fixed IV from the password hash
			MessageDigest md = MessageDigest.getInstance(CryptoUtils.MESSAGE_DIGEST_ALGORITHM_SHA256);
			byte[] passwordHash = md.digest(password);
			byte[] iv = new byte[16];
			System.arraycopy(passwordHash, 0, iv, 0, 16);
			javax.crypto.spec.IvParameterSpec ivSpec = new javax.crypto.spec.IvParameterSpec(iv);
			
			// Initialize cipher with ICSF key
			cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, ckdsKey, ivSpec);
			
			// Encrypt the password hash to create derived key material
			// Pad to 32 bytes if needed
			byte[] input = new byte[32];
			System.arraycopy(passwordHash, 0, input, 0, Math.min(passwordHash.length, 32));
			
			byte[] encrypted = cipher.doFinal(input);
			
			// Use first 32 bytes as the derived key
			byte[] derivedKey = new byte[keySize];
			System.arraycopy(encrypted, 0, derivedKey, 0, keySize);
			
			System.out.println("Successfully derived encryption key using ICSF hardware");
			return derivedKey;
			
		} catch (Exception e) {
			System.out.println("Failed to derive key using ICSF, falling back to standard derivation: " + e.getMessage());
			// Fall back to standard password-based derivation
			MessageDigest md = MessageDigest.getInstance(CryptoUtils.MESSAGE_DIGEST_ALGORITHM);
			byte[] digest = md.digest(password);
			byte[] derivedKey = new byte[keySize];
			System.arraycopy(digest, 0, derivedKey, 0, Math.min(digest.length, keySize));
			return derivedKey;
		}
	}

	/**
	 * Decrypt the key.
	 *
	 * @param encryptedKey The encrypted key
	 * @return The decrypted key
	 */
	public byte[] decrypt(byte[] encryptedKey) throws Exception {
		byte[] decrypted = LTPACrypto.decrypt(encryptedKey, key, cipher);
		System.out.println("KeyEncryptor.decrypt: Decrypted key length = " + (decrypted != null ? decrypted.length : "null"));
		if (decrypted != null && decrypted.length < 32) {
			System.out.println("WARNING: Decrypted key is too short (" + decrypted.length + " bytes). Expected 32 bytes for AES-256.");
			System.out.println("This likely means the ltpa.keys file was created with an old password, not using ICSF.");
			System.out.println("Please regenerate the ltpa.keys file using: securityUtility createLTPAKeys --password=<any-password> --file=ltpa.keys");
		}
		return decrypted;
	}

	/**
	 * Encrypt the key
	 *
	 * @param key The key
	 * @return The encrypted key
	 */
	public byte[] encrypt(byte[] key) throws Exception {
		return LTPACrypto.encrypt(key, this.key, cipher);
	}
}