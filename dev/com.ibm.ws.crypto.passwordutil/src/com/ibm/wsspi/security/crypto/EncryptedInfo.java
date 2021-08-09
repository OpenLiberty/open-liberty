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

/**
 * Return code information for password utilities, deciphering or enciphering.
 */
public class EncryptedInfo {
    private final byte[] bytes;
    private final String alias;

    /**
     * This constructor takes the encrypted bytes and a keyAlias as parameters.
     * This is for passing to/from the WebSphere Application Server runtime so the
     * runtime can associate the bytes with a specific key used to encrypt the
     * bytes.
     * 
     * @param encryptedBytes
     * @param keyAlias
     */

    public EncryptedInfo(byte[] encryptedBytes, String keyAlias) {
        this.bytes = encryptedBytes == null ? null : encryptedBytes.clone();
        this.alias = keyAlias;
    }

    /**
     * This returns the encrypted bytes.
     * 
     * @return byte[]
     */
    public byte[] getEncryptedBytes() {
        return bytes == null ? null : bytes.clone();
    }

    /**
     * This returns the key alias. This key alias is a logical string associated
     * with the encrypted password in the model. The format is
     * {custom:keyAlias}encrypted_password. Typically just the key alias is put
     * here, but algorithm information could also be returned.
     * 
     * @return String
     */
    public String getKeyAlias() {
        return this.alias;
    }

}
