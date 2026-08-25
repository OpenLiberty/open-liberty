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

import java.util.Properties;

import com.ibm.ws.common.crypto.CryptoUtils;

public interface LTPAKeyFileUtility {

	/**
	 * Properties used in the LTPA keys file.
	 */
	public static final String KEYIMPORT_SECRETKEY = CryptoUtils.isFips140_3Enabled() ? "com.ibm.websphere.ltpa.SharedKey" : "com.ibm.websphere.ltpa.3DESKey";
	public static final String KEYIMPORT_PRIVATEKEY = "com.ibm.websphere.ltpa.PrivateKey";
	public static final String KEYIMPORT_PUBLICKEY = "com.ibm.websphere.ltpa.PublicKey";
	public static final String KEYIMPORT_REALM = "com.ibm.websphere.ltpa.Realm";
	public static final String LTPA_VERSION_PROPERTY = "com.ibm.websphere.ltpa.version";
	public static final String CREATION_DATE_PROPERTY = "com.ibm.websphere.CreationDate";
	public static final String CREATION_HOST_PROPERTY = "com.ibm.websphere.CreationHost";

	public static final String VALIDATION_KEYS_PROPERTY = "com.ibm.websphere.ltpa.ltpa_validation_keys";

	/**
	 * Create the LTPA keys file at the specified location using the specified
	 * password bytes.
	 *
	 * @param keyFile
	 * @param keyPasswordBytes
	 * @return A Properties object containing the various attributes created for the
	 *         LTPA keys
	 * @throws Exception
	 */
	Properties createLTPAKeysFile(String keyFile, byte[] keyPasswordBytes) throws Exception;

	/**
	 * Create the LTPA keys file at the specified location, protecting the key
	 * material with the supplied {@link LTPAKeyEncryptor} (e.g. an
	 * {@link AesLTPAKeyEncryptor} backed by an ICSF/CKDS hardware key).
	 *
	 * @param keyFile   path where the LTPA keys file will be written
	 * @param encryptor encryptor used to protect the private and secret key bytes
	 * @return a Properties object containing the generated LTPA key attributes
	 * @throws Exception if key generation or file I/O fails
	 */
	Properties createLTPAKeysFile(String keyFile, LTPAKeyEncryptor encryptor) throws Exception;

	/**
	 * Read an existing LTPA keys file, decrypt its key material using
	 * {@code currentEncryptor}, re-encrypt that same key material using
	 * {@code newEncryptor}, and write the result to {@code newKeyFile}.
	 * The source and destination paths may be the same (in-place update).
	 * <p>
	 * Use {@link KeyEncryptor} to wrap a plaintext password, and
	 * {@link AesLTPAKeyEncryptor} to wrap a hardware AES/CKDS key.
	 *
	 * @param currentKeyFile   path to the existing LTPA keys file
	 * @param currentEncryptor encryptor used to decrypt the current file
	 * @param newKeyFile       path to write the re-encrypted LTPA keys file
	 * @param newEncryptor     encryptor used to protect the output key material
	 * @throws Exception if the file cannot be read, decryption fails, or the
	 *                   new file cannot be written
	 */
	Properties reEncryptLTPAKeysFile(String currentKeyFile, LTPAKeyEncryptor currentEncryptor,
	                                  String newKeyFile, LTPAKeyEncryptor newEncryptor) throws Exception;

}
