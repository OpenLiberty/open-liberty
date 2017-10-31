/*
* IBM Confidential
*
* OCO Source Materials
*
* WLP Copyright IBM Corp. 2017
*
* The source code for this program is not published or otherwise divested
* of its trade secrets, irrespective of what has been deposited with the
* U.S. Copyright Office.
*/
package com.ibm.wsspi.security.audit;

import java.security.Key;

/**
 *
 */
public interface AuditEncrypting {

    /**
     * <p>
     * The <code>encrypt</code> operation takes a UTF-8 encoded String in the form of a byte[].
     * The byte[] is generated from String.getBytes("UTF-8").
     * An encrypted byte[] is returned.
     * </p>
     * 
     * @param byte[] data to encrypt
     * @param java.security.Key shared key
     * @return byte[] of encrypted data
     * @throws com.ibm.wsspi.security.audit.AuditEncryptException
     **/
    public byte[] encrypt(byte[] data, Key key) throws AuditEncryptionException;

    /**
     * <p>
     * The <code>decrypt</code> operation takes a UTF-8 encoded String in the form of a byte[].
     * The byte[] is generated from String.getBytes("UTF-8").
     * A decrypted byte[] is returned.
     * </p>
     * 
     * @param byte[] data to decrypt
     * @return byte[]
     * @throws com.ibm.wsspi.security.audit.AuditDecryptException
     **/
    public byte[] decrypt(byte[] data, Key key) throws AuditDecryptionException;

    /**
     * <p>
     * The <code>initialize</code> method initializes the AuditEncryption implementation
     * </p>
     * 
     * @param String representing the non-fully qualified keystore name
     * @param String representing the path to the keystore
     * @param String representing the keystore type
     * @param String representing the keystore provider
     * @param String representing the password for the keystore
     * @param String representing the alias for the keystore entry
     **/
    public void initialize(String keyStoreName, String keyStorePath, String keyStoreType, String keyStoreProvider,
                           String keyStorePassword, String keyAlias) throws AuditEncryptionException;
}
