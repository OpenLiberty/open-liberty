/*******************************************************************************
 * Copyright (c) 2016, 2025 IBM Corporation and others.
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

}
