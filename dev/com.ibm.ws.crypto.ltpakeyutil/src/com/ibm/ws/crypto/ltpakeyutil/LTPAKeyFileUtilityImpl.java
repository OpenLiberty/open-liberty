/*******************************************************************************
 * Copyright (c) 2016, 2026 IBM Corporation and others.
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.KeyPair;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Properties;

import com.ibm.ws.common.crypto.CryptoUtils;
import com.ibm.ws.common.encoder.Base64Coder;

/**
 *
 */
public class LTPAKeyFileUtilityImpl implements LTPAKeyFileUtility {

	/** {@inheritDoc} */
	@Override
	public Properties createLTPAKeysFile(String keyFile, byte[] keyPasswordBytes) throws Exception {
		Properties ltpaProps = generateLTPAKeys(keyPasswordBytes, "defaultRealm");
		addLTPAKeysToFile(getOutputStream(keyFile), ltpaProps);
		return ltpaProps;
	}

	/**
	 * Generates the LTPA keys and stores them into a Properties object.
	 *
	 * @param keyPasswordBytes
	 * @param realm
	 * @return
	 * @throws Exception
	 */
	protected final Properties generateLTPAKeys(byte[] keyPasswordBytes, final String realm) throws Exception {
		return generateLTPAKeys(keyPasswordBytes, null, null, null, realm);
	}

	/**
	 * Generates the LTPA keys and stores them into a Properties object.
	 *
	 * In the case of generating new ltpa keys, pass null in sharedKeyBytes,
	 * privateKeyBytes, and publicKeyBytes to generate them.
	 *
	 * Otherwise, in the case of re-encrypting existing ltpa keys, pass the bytes in
	 * sharedKeyBytes, privateKeyBytes, and publicKeyBytes to reuse them.
	 *
	 * @param keyPasswordBytes
	 * @param sharedKeyBytes
	 * @param privateKeyBytes
	 * @param publicKeyBytes
	 * @param realm
	 * @return
	 * @throws Exception
	 */
	protected final Properties generateLTPAKeys(byte[] keyPasswordBytes, byte[] sharedKeyBytes, byte[] privateKeyBytes,
			byte[] publicKeyBytes, final String realm) throws Exception {
		Properties expProps = null;

		try {
			KeyEncryptor encryptor = new KeyEncryptor(keyPasswordBytes);

			if (publicKeyBytes == null && privateKeyBytes == null) {
				LTPAKeyPair pair = LTPADigSignature.generateLTPAKeyPair();
				publicKeyBytes = pair.getPublic().getEncoded();
				privateKeyBytes = pair.getPrivate().getEncoded();
			}
			byte[] encryptedPrivateKeyBytes = encryptor.encrypt(privateKeyBytes);

			if (sharedKeyBytes == null) {
				sharedKeyBytes = LTPACrypto.generateSharedKey(); // key length is 32 bytes (256 bits) for FIPS (AES), 24
																	// bytes (192 bits) for non-FIPS (3DES)
			}
			byte[] encryptedSharedKeyBytes = encryptor.encrypt(sharedKeyBytes);

			String tmpShared = Base64Coder.base64EncodeToString(encryptedSharedKeyBytes);
			String tmpPrivate = Base64Coder.base64EncodeToString(encryptedPrivateKeyBytes);
			String tmpPublic = Base64Coder.base64EncodeToString(publicKeyBytes);

			expProps = new Properties();

			expProps.put(KEYIMPORT_SECRETKEY, tmpShared);
			expProps.put(KEYIMPORT_PRIVATEKEY, tmpPrivate);
			expProps.put(KEYIMPORT_PUBLICKEY, tmpPublic);

			expProps.put(KEYIMPORT_REALM, realm);
			expProps.put(CREATION_HOST_PROPERTY, "localhost");
			expProps.put(LTPA_VERSION_PROPERTY, CryptoUtils.isFips140_3Enabled() ? "2.0" : "1.0");
			expProps.put(CREATION_DATE_PROPERTY, (new java.util.Date()).toString());

			// Generate PQC (ML-DSA) keys for signatures
			try {
				KeyPair mldsaKeyPair = generateMLDSAKeyPair();
				if (mldsaKeyPair != null) {
					byte[] mldsaPublicKeyBytes = mldsaKeyPair.getPublic().getEncoded();
					byte[] mldsaPrivateKeyBytes = mldsaKeyPair.getPrivate().getEncoded();
					byte[] encryptedMLDSAPrivateKeyBytes = encryptor.encrypt(mldsaPrivateKeyBytes);

					String tmpMLDSAPublic = Base64Coder.base64EncodeToString(mldsaPublicKeyBytes);
					String tmpMLDSAPrivate = Base64Coder.base64EncodeToString(encryptedMLDSAPrivateKeyBytes);

					expProps.put("com.ibm.websphere.ltpa.pqc.PublicKey", tmpMLDSAPublic);
					expProps.put("com.ibm.websphere.ltpa.pqc.PrivateKey", tmpMLDSAPrivate);
//					expProps.put("com.ibm.websphere.ltpa.pqc.Algorithm", "ML-DSA-65");
					expProps.put("com.ibm.websphere.ltpa.pqc.Algorithm", "ML-DSA-44");
				}
			} catch (Exception mldsaEx) {
				// ML-DSA key generation failed - log but continue with classical keys only
				System.err.println("Warning: ML-DSA key generation failed: " + mldsaEx.getMessage());
			}

			// Generate PQC (ML-KEM) keys for encryption (Phase 4)
			try {
				KeyPair mlkemKeyPair = generateMLKEMKeyPair();
				if (mlkemKeyPair != null) {
					byte[] mlkemPublicKeyBytes = mlkemKeyPair.getPublic().getEncoded();
					byte[] mlkemPrivateKeyBytes = mlkemKeyPair.getPrivate().getEncoded();
					byte[] encryptedMLKEMPrivateKeyBytes = encryptor.encrypt(mlkemPrivateKeyBytes);

					String tmpMLKEMPublic = Base64Coder.base64EncodeToString(mlkemPublicKeyBytes);
					String tmpMLKEMPrivate = Base64Coder.base64EncodeToString(encryptedMLKEMPrivateKeyBytes);

					expProps.put("com.ibm.websphere.ltpa.mlkem.PublicKey", tmpMLKEMPublic);
					expProps.put("com.ibm.websphere.ltpa.mlkem.PrivateKey", tmpMLKEMPrivate);
//                    expProps.put("com.ibm.websphere.ltpa.mlkem.Algorithm", "ML-KEM-768");
					expProps.put("com.ibm.websphere.ltpa.mlkem.Algorithm", "ML-KEM-512");
				}
			} catch (Exception mlkemEx) {
				// ML-KEM key generation failed - log but continue without encryption
				System.err.println("Warning: ML-KEM key generation failed: " + mlkemEx.getMessage());
			}
		} catch (Exception e) {
			throw e;
		}

		return expProps;
	}

	/**
	 * Obtain the OutputStream for the given file.
	 *
	 * @param keyFile
	 * @return
	 * @throws IOException
	 */
	private OutputStream getOutputStream(final String keyFile) throws IOException {
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<OutputStream>() {
				@Override
				public OutputStream run() throws IOException {
					return new FileOutputStream(new File(keyFile));
				}
			});
		} catch (PrivilegedActionException e) {
			// Wrap the wrapped IOException from doPriv in an IOException and re-throw
			throw new IOException(e.getCause());
		}
	}

	/**
	 * Write the LTPA key properties to the given OutputStream. This method will
	 * close the OutputStream.
	 *
	 * @param keyImportFile The import file to be created
	 * @param ltpaProps     The properties containing the LTPA keys
	 *
	 * @throws TokenException
	 * @throws IOException
	 */
	protected void addLTPAKeysToFile(OutputStream os, Properties ltpaProps) throws Exception {
		try {
			// Write the ltpa key propeperties to
			ltpaProps.store(os, null);
		} catch (IOException e) {
			throw e;
		} finally {
			if (os != null)
				try {
					os.close();
				} catch (IOException e) {
				}
		}

		return;
	}

	/**
	 * Generate ML-DSA (Dilithium) key pair for PQC support. Uses Java 25's built-in
	 * ML-DSA support without requiring MLDSAParameterSpec. The KeyPairGenerator
	 * uses default parameters (ML-DSA-65) when not explicitly initialized.
	 *
	 * @return KeyPair containing ML-DSA public and private keys, or null if
	 *         generation fails
	 */
	private KeyPair generateMLDSAKeyPair() {
		System.out.println("DEBUG: Starting ML-DSA key pair generation");
		System.out.println("DEBUG: Java version: " + System.getProperty("java.version"));
		System.out.println("DEBUG: Java vendor: " + System.getProperty("java.vendor"));

		try {
			// List all available security providers
			System.out.println("DEBUG: Available security providers:");
			java.security.Provider[] providers = java.security.Security.getProviders();
			for (java.security.Provider provider : providers) {
				System.out.println("DEBUG:   - " + provider.getName() + " (version " + provider.getVersion() + ")");
				// Check if provider supports ML-DSA
				if (provider.getService("KeyPairGenerator", "ML-DSA") != null) {
					System.out.println("DEBUG:     * Supports ML-DSA KeyPairGenerator");
				}
			}

			// Generate ML-DSA key pair using default SUN provider
			// Note: IBM Java 25 doesn't expose MLDSAParameterSpec class, but the
			// KeyPairGenerator
			// works with default parameters (ML-DSA-65) without explicit initialization
			System.out.println("DEBUG: Getting KeyPairGenerator instance for ML-DSA...");
//			java.security.KeyPairGenerator keyGen = java.security.KeyPairGenerator.getInstance("ML-DSA");
			java.security.KeyPairGenerator keyGen = java.security.KeyPairGenerator.getInstance("ML-DSA-44");
			System.out.println("DEBUG: KeyPairGenerator obtained, provider: " + keyGen.getProvider().getName());
//			System.out.println("DEBUG: Using default parameters (ML-DSA-65) - no explicit initialization needed");
			System.out.println("DEBUG: Using default parameters (ML-DSA-44) - no explicit initialization needed");

			System.out.println("DEBUG: Generating ML-DSA key pair...");
			KeyPair keyPair = keyGen.generateKeyPair();
			System.out.println("DEBUG: ML-DSA key pair generated successfully!");
			System.out.println("DEBUG: Public key algorithm: " + keyPair.getPublic().getAlgorithm());
			System.out.println("DEBUG: Public key format: " + keyPair.getPublic().getFormat());
			System.out.println("DEBUG: Public key size: " + keyPair.getPublic().getEncoded().length + " bytes");
			System.out.println("DEBUG: Private key algorithm: " + keyPair.getPrivate().getAlgorithm());
			System.out.println("DEBUG: Private key format: " + keyPair.getPrivate().getFormat());
			System.out.println("DEBUG: Private key size: " + keyPair.getPrivate().getEncoded().length + " bytes");

			return keyPair;
		} catch (java.security.NoSuchAlgorithmException e) {
			System.err.println("ERROR: ML-DSA algorithm not available in any security provider");
			System.err.println("ERROR: Exception: " + e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		} catch (Exception e) {
			System.err.println("ERROR: ML-DSA key generation failed");
			System.err.println("ERROR: Exception: " + e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
	}

	/**
	 * Generate ML-KEM (Kyber) key pair for PQC encryption support. Uses Java 26's
	 * built-in ML-KEM support (JEP 478). The KeyPairGenerator uses default
	 * parameters (ML-KEM-768) when not explicitly initialized.
	 *
	 * @return KeyPair containing ML-KEM public and private keys, or null if
	 *         generation fails
	 */
	private KeyPair generateMLKEMKeyPair() {
		System.out.println("DEBUG: Starting ML-KEM key pair generation");
		System.out.println("DEBUG: Java version: " + System.getProperty("java.version"));
		System.out.println("DEBUG: Java vendor: " + System.getProperty("java.vendor"));

		try {
			// List all available security providers
			System.out.println("DEBUG: Available security providers:");
			java.security.Provider[] providers = java.security.Security.getProviders();
			for (java.security.Provider provider : providers) {
				System.out.println("DEBUG:   - " + provider.getName() + " (version " + provider.getVersion() + ")");
				// Check if provider supports ML-KEM
				if (provider.getService("KeyPairGenerator", "ML-KEM") != null) {
					System.out.println("DEBUG:     * Supports ML-KEM KeyPairGenerator");
				}
			}

			// Generate ML-KEM key pair using default SUN provider
			// Note: Java 26 provides ML-KEM support via JEP 478
			// The KeyPairGenerator works with default parameters (ML-KEM-768) without
			// explicit initialization
			System.out.println("DEBUG: Getting KeyPairGenerator instance for ML-KEM...");
//			java.security.KeyPairGenerator keyGen = java.security.KeyPairGenerator.getInstance("ML-KEM");
			java.security.KeyPairGenerator keyGen = java.security.KeyPairGenerator.getInstance("ML-KEM-512");
			System.out.println("DEBUG: KeyPairGenerator obtained, provider: " + keyGen.getProvider().getName());
			// System.out.println("DEBUG: Using default parameters (ML-KEM-768) - no
			// explicit initialization needed");
			System.out.println("DEBUG: Using default parameters (ML-KEM-512) - no explicit initialization needed");

			System.out.println("DEBUG: Generating ML-KEM key pair...");
			KeyPair keyPair = keyGen.generateKeyPair();
			System.out.println("DEBUG: ML-KEM key pair generated successfully!");
			System.out.println("DEBUG: Public key algorithm: " + keyPair.getPublic().getAlgorithm());
			System.out.println("DEBUG: Public key format: " + keyPair.getPublic().getFormat());
			System.out.println("DEBUG: Public key size: " + keyPair.getPublic().getEncoded().length + " bytes");
			System.out.println("DEBUG: Private key algorithm: " + keyPair.getPrivate().getAlgorithm());
			System.out.println("DEBUG: Private key format: " + keyPair.getPrivate().getFormat());
			System.out.println("DEBUG: Private key size: " + keyPair.getPrivate().getEncoded().length + " bytes");

			// Validate key sizes (ML-KEM-768: public=1184 bytes, private=2400 bytes)
			int publicKeySize = keyPair.getPublic().getEncoded().length;
			int privateKeySize = keyPair.getPrivate().getEncoded().length;

			if (publicKeySize != 1184) {
				System.err
						.println("WARNING: ML-KEM public key size is " + publicKeySize + " bytes, expected 1184 bytes");
			}
			if (privateKeySize != 2400) {
				System.err.println(
						"WARNING: ML-KEM private key size is " + privateKeySize + " bytes, expected 2400 bytes");
			}

			return keyPair;
		} catch (java.security.NoSuchAlgorithmException e) {
			System.err.println("ERROR: ML-KEM algorithm not available in any security provider");
			System.err.println("ERROR: This requires Java 26+ with JEP 478 support");
			System.err.println("ERROR: Exception: " + e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		} catch (Exception e) {
			System.err.println("ERROR: ML-KEM key generation failed");
			System.err.println("ERROR: Exception: " + e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
	}
}
