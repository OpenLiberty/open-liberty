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
package com.ibm.ws.security.token.ltpa.internal;

import java.util.Properties;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.crypto.ltpakeyutil.LTPAKeyEncryptor;
import com.ibm.ws.crypto.ltpakeyutil.LTPAKeyFileUtility;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

/**
 * Utility class to create the LTPA keys file.
 */
public interface LTPAKeyFileCreator extends LTPAKeyFileUtility {

    /**
     * Create the LTPA keys file at the specified location using
     * the specified password bytes.
     * <p>
     * Access the keyFile using the WsLocationAdmin
     *
     * @param locService
     * @param keyFile
     * @param keyPasswordBytes
     * @return A Properties object containing the various attributes created for the LTPA keys
     * @throws Exception
     */
    public Properties createLTPAKeysFile(WsLocationAdmin locService, String keyFile, @Sensitive byte[] keyPasswordBytes) throws Exception;

    /**
     * Create the LTPA keys file at the specified location using
     * the specified password bytes, shared key bytes, private key bytes, and public key bytes.
     * <p>
     * Access the keyFile using the WsLocationAdmin
     *
     * @param locService
     * @param keyFile
     * @param keyPasswordBytes
     * @param sharedKeyBytes
     * @param privateKeyBytes
     * @param publicKeyBytes
     * @return A Properties object containing the various attributes created for the LTPA keys
     * @throws Exception
     */
    public Properties createLTPAKeysFile(WsLocationAdmin locService, String keyFile, @Sensitive byte[] keyPasswordBytes,
                                         @Sensitive byte[] sharedKeyBytes, @Sensitive byte[] privateKeyBytes, @Sensitive byte[] publicKeyBytes) throws Exception;

    /**
     * Create the LTPA keys file at the specified location using the supplied
     * {@link LTPAKeyEncryptor} to protect the given key material.
     * <p>
     * Access the keyFile using the WsLocationAdmin.
     *
     * @param locService
     * @param keyFile
     * @param encryptor       the encryptor that will protect the key material
     * @param sharedKeyBytes  plaintext shared (3DES/AES) key bytes
     * @param privateKeyBytes plaintext RSA private key bytes
     * @param publicKeyBytes  RSA public key bytes (stored as-is)
     * @return A Properties object containing the re-encrypted LTPA key attributes
     * @throws Exception
     */
    public Properties createLTPAKeysFile(WsLocationAdmin locService, String keyFile, @Sensitive LTPAKeyEncryptor encryptor,
                                         @Sensitive byte[] sharedKeyBytes, @Sensitive byte[] privateKeyBytes, @Sensitive byte[] publicKeyBytes) throws Exception;

    /**
     * Create the LTPA keys file at the specified location using the supplied
     * {@link LTPAKeyEncryptor} (AES key path).
     * <p>
     * Access the keyFile using the WsLocationAdmin.
     *
     * @param locService
     * @param keyFile
     * @param encryptor  the encryptor that will protect the generated key material
     * @return A Properties object containing the various attributes created for the LTPA keys
     * @throws Exception
     */
    public Properties createLTPAKeysFile(WsLocationAdmin locService, String keyFile, @Sensitive LTPAKeyEncryptor encryptor) throws Exception;

}