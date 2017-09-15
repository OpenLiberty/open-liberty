/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wsspi.security.crypto;

import java.util.Map;

/**
 * The interface for encrypting or decrypting the sensitive data.
 * @ibm-spi
 */
public interface CustomPasswordEncryption {

    /**
     * The encrypt operation takes a UTF-8 encoded String in the form of a byte[].
     * The byte[] is generated from String.getBytes("UTF-8"). An encrypted byte[]
     * is returned from the implementation in the EncryptedInfo object.
     * Additionally, a logically key alias is returned in EncryptedInfo so which
     * is passed back into the decrypt method to determine which key was used to
     * encrypt this password. The WebSphere Application Server runtime has no
     * knowledge of the algorithm or key used to encrypt the data.
     * 
     * @param decrypted_bytes
     * @return com.ibm.wsspi.security.crypto.EncryptedInfo
     * @throws com.ibm.wsspi.security.crypto.PasswordEncryptException
     **/
    EncryptedInfo encrypt(byte[] decrypted_bytes) throws PasswordEncryptException;

    /**
     * The decrypt operation takes the EncryptedInfo object containing a byte[]
     * and the logical key alias and converts it to the decrypted byte[]. The
     * WebSphere Application Server runtime will convert the byte[] to a String
     * using new String (byte[], "UTF-8");
     * 
     * @param info
     * @return byte[]
     * @throws com.ibm.wsspi.security.crypto.PasswordDecryptException
     **/
    byte[] decrypt(EncryptedInfo info) throws PasswordDecryptException;

    /**
     * This is reserved for future use and is currently not called by the
     * WebSphere Application Server runtime.
     * 
     * @param initialization_data
     **/
    void initialize(Map initialization_data);
}
