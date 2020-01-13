/*******************************************************************************
 * Copyright (c) 2011, 2019 IBM Corporation and others.
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
import com.ibm.wsspi.security.ltpa.Token;

/**
 * The implementation of this services creates a token specific to the implementation
 * and recreates a token from the token bytes.
 */
public interface TokenService {

    /**
     * Creates a Token object from the specified token data properties.
     *
     * @param tokenData
     * @return
     * @throws TokenCreationFailedException
     */
    public Token createToken(Map<String, Object> tokenData) throws TokenCreationFailedException;

    /**
     * Recreates a Token object based on previous token bytes.
     *
     * @param tokenBytes
     * @return
     */
    public Token recreateTokenFromBytes(byte[] tokenBytes) throws InvalidTokenException, TokenExpiredException;

    /**
     * Recreates a Token object based on previous token bytes without a list of attributes.
     *
     * @param tokenBytes
     * @param removeAttributes A list of attributes will be removed from the token
     * @return
     */
    public Token recreateTokenFromBytes(byte[] tokenBytes, String... removeAttributes) throws InvalidTokenException, TokenExpiredException;

}
