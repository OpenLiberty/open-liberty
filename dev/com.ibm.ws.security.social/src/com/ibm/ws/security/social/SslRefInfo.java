/*
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2016, 2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.security.social;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;

import javax.crypto.SecretKey;

import com.ibm.ws.security.social.error.SocialLoginException;

public interface SslRefInfo {

    String getTrustStoreName() throws SocialLoginException;

    String getKeyStoreName() throws SocialLoginException;

    /**
     * @return
     * @throws SocialLoginException
     */
    HashMap<String, PublicKey> getPublicKeys() throws SocialLoginException;

    /**
     * @return public key.
     * @throws SocialLoginException
     */
    PublicKey getPublicKey() throws SocialLoginException;

    /**
     * @return private key.
     * @throws SocialLoginException
     */
    PrivateKey getPrivateKey() throws SocialLoginException;

    /**
     * @return secret key.
     * @throws SocialLoginException
     */
    SecretKey getSecretKey() throws SocialLoginException;

}
