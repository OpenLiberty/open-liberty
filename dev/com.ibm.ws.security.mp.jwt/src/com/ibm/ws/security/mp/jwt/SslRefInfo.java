/*******************************************************************************
 * Copyright (c) 2016, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;

import javax.crypto.SecretKey;

import com.ibm.ws.security.mp.jwt.error.MpJwtProcessingException;

public interface SslRefInfo {

    String getTrustStoreName() throws MpJwtProcessingException;

    String getKeyStoreName() throws MpJwtProcessingException;

    /**
     * @return
     * @throws MpJwtProcessingException
     */
    HashMap<String, PublicKey> getPublicKeys() throws MpJwtProcessingException;

    /**
     * @return public key.
     * @throws MpJwtProcessingException
     */
    PublicKey getPublicKey() throws MpJwtProcessingException;

    /**
     * @return private key.
     * @throws MpJwtProcessingException
     */
    PrivateKey getPrivateKey() throws MpJwtProcessingException;

    /**
     * @return secret key.
     * @throws MpJwtProcessingException
     */
    SecretKey getSecretKey() throws MpJwtProcessingException;

}
