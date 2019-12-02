/*******************************************************************************
 * Copyright (c) 2012, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.token;

import java.util.Map;

import com.ibm.websphere.security.auth.InvalidTokenException;
import com.ibm.websphere.security.auth.TokenCreationFailedException;
import com.ibm.websphere.security.auth.TokenExpiredException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.security.ltpa.Token;
import com.ibm.wsspi.security.token.SingleSignonToken;

/**
 *
 */
public interface TokenManager {

    /**
     * Creates a token of the specified token type from the token data.
     *
     * @param tokenType The type of token to create.
     * @param tokenData The token data used to create the token.
     * @return the token of the specified type.
     * @throws TokenException
     * @throws TokenCreationFailedException
     */
    public abstract Token createToken(String tokenType, Map<String, Object> tokenData) throws TokenCreationFailedException;

    /**
     * Creates an SSO token from the token data.
     *
     * @param tokenData
     * @return
     * @throws TokenCreationFailedException
     */
    public abstract SingleSignonToken createSSOToken(Map<String, Object> tokenData) throws TokenCreationFailedException;

    /**
     * Creates an SSO token from the existing token.
     *
     * @param token
     * @return
     */
    public abstract SingleSignonToken createSSOToken(Token token) throws TokenCreationFailedException;

    /**
     * Recreates the token from the given token bytes without a list of attributes. Will try all known
     * TokenServices in an attempt to recreate the token.
     *
     * @param tokenBytes
     * @param removeAttributes A list of attributes will be removed from the token
     * @return A non-null Token
     * @throws InvalidTokenException
     */
    @FFDCIgnore(InvalidTokenException.class)
    public abstract Token recreateTokenFromBytes(byte[] tokenBytes, String... removeAttributes) throws InvalidTokenException, TokenExpiredException;

    /**
     * Recreates the token of the specified token type from the given token bytes.
     *
     * @param tokenType The type of token to recreate.
     * @param encryptedTokenBytes The encrypted token bytes.
     * @return The recreated token of the specified type.
     * @throws TokenExpiredException
     * @throws InvalidTokenException
     */
    public abstract Token recreateTokenFromBytes(String tokenType, byte[] tokenBytes) throws InvalidTokenException, TokenExpiredException;

}