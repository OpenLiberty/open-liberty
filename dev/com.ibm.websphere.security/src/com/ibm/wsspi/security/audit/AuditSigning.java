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
public interface AuditSigning {
    /**
     * <p>
     * The <code>initialize</code> method initializes the AuditSigning implementation
     * </p>
     *
     * @throws AuditSignException
     **/
    public void initialize(String keyStoreName, String keyStorePath, String keyStoreType, String keyStoreProvider,
                           String keyStorePassword, String keyAlias) throws AuditSigningException;

    /**
     * <p>
     * The <code>sign</code> method signs the data with a key
     * </p>
     *
     * @param a byte array of data to be signed
     * @param the key used to sign the byte array of data
     * @throws AuditSignException
     **/
    public byte[] sign(byte[] value, Key key) throws AuditSigningException;

    /**
     * <p>
     * The <code>verify</code> method verifies the data is signed with a key
     * </p>
     *
     * @param a signed byte array of data
     * @param the key used to sign the byte array of data
     * @returns a boolean value based the successful verification of the data
     * @throws AuditSignException
     **/
    public boolean verify(byte[] data, Key key) throws AuditSigningException;

}
