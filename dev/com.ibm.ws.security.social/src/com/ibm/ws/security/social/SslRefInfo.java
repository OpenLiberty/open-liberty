/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
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
