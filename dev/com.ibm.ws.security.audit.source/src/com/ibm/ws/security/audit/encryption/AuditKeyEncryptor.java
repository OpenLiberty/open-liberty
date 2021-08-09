/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.audit.encryption;

/**
 *
 */
import com.ibm.ws.security.audit.utils.ByteArray;

/**
 * A package local class for performing encryption and decryption of keys based on a key
 */
public class AuditKeyEncryptor {
    byte[] password;
    byte[] desKey;
    AuditCrypto des;

    public AuditKeyEncryptor(byte[] password) {
        this.password = password;
        java.security.MessageDigest md = null;
        try {
            md = java.security.MessageDigest.getInstance("SHA");
            desKey = new byte[24]; // for 3DES
            byte[] digest = md.digest(this.password);
            ByteArray.copy(digest, 0, digest.length, desKey, 0);
            desKey[20] = (byte) 0x00;
            desKey[21] = (byte) 0x00;
            desKey[22] = (byte) 0x00;
            desKey[23] = (byte) 0x00;
        } catch (java.security.NoSuchAlgorithmException e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.ltpa.KeyEncryptor.KeyEncryptor", "21", this);
        }
        des = new AuditCrypto();

    }

    public byte[] decrypt(byte[] encrKey) {
        return des.decrypt(encrKey, desKey);
    }

    public byte[] encrypt(byte[] key) {
        return des.encrypt(key, desKey);
    }
}