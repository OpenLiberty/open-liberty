/*******************************************************************************
 * Copyright (c) 2004, 2011, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.ltpa;

import com.ibm.websphere.security.auth.InvalidTokenException;
import com.ibm.websphere.security.auth.TokenExpiredException;

/**
 * <p> This interface is implemented by a provider to create LTPA tokens. The
 * class is loaded via the security property "com.ibm.wsspi.security.ltpa.tokenFactory".
 * One can put multiple token factories in this property using the | delimiter.
 * The order determines which token will be used for a specific purpose, to be
 * defined. </p>
 *
 * @ibm-spi
 */
public interface TokenFactory {

    /**
     * Initializes the token factories with a Map of configuration info.
     * This method will be called any time the configuration data
     * changes including the encryption keys.
     *
     * @param java.util.Map tokenFactoryMap
     */
    public void initialize(java.util.Map tokenFactoryMap);

    /**
     * Returns a Token based on the type of configured token
     *
     * @param byte[] token data
     * @return com.ibm.wsspi.security.ltpa.Token
     * @throws com.ibm.websphere.security.auth.InvalidTokenException
     * @throws com.ibm.websphere.security.auth.TokenExpiredException
     */
    public Token validateTokenBytes(byte[] encryptedData) throws InvalidTokenException, TokenExpiredException;

    /**
     * Validate the token byte[] and returns a Token based on the type of configured token without the attributes specify in the removeAttributes parameter
     *
     * @param byte[] token data
     * @param attributes[] A list of attributes will be removed from the token.
     * @return com.ibm.wsspi.security.ltpa.Token
     * @throws com.ibm.websphere.security.auth.InvalidTokenException
     * @throws com.ibm.websphere.security.auth.TokenExpiredException
     */
    public Token validateTokenBytes(byte[] encryptedData, String... removeAttributes) throws InvalidTokenException, TokenExpiredException;

    /**
     * Returns a Token based on the type of configured token
     *
     * @param java.util.Map a hashmap containing configuration info for the Token impl.
     *            The tokenData Map should contain a userUniqueId: "com.ibm.wsspi.security.ltpa.userUniqueId"
     * @return com.ibm.wsspi.security.ltpa.Token
     * @throws com.ibm.websphere.security.auth.TokenCreationFailedException
     */
    public Token createToken(java.util.Map tokenData) throws com.ibm.websphere.security.auth.TokenCreationFailedException;

}
