/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.audit;

/**
 *
 */
public class AuditEncryptedInfo {
    private final byte[] bytes;
    private final String alias;

    /**
     * <p>
     * The <code>AuditEncryptedInfo</code> takes the encrypted bytes and a keyAlias as parameters.
     * This inferface is used for passing to/from the WebSphere Application Server runtime so
     * the runtime can associate the bytes with a specific key used to encrypt
     * the bytes.
     * </p>
     * 
     * @param an array of bytes representing the encrypted bytes of a key
     * @param a String representing the alias for this key
     */

    public AuditEncryptedInfo(byte[] encryptedBytes, String keyAlias) {
        bytes = encryptedBytes;
        alias = keyAlias;
    }

    /**
     * <p>
     * The <code>getEncryptedBytes</code> method returns the encrypted bytes of this structure.
     * </p>
     * 
     * @return byte[]
     */
    public byte[] getEncryptedBytes() {
        return bytes;
    }

    /**
     * <p>
     * The <code>getKeyAlias</code> method returns the key alias. This key alias is a logical string associated
     * with the encrypted password in the model. The format is {custom:keyAlias}encrypted_password. Typically
     * just the key alias is put here, but algorithm information could also be returned.
     * </p>
     * 
     * @return String
     */
    public String getKeyAlias() {
        return alias;
    }

}
