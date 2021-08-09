/*******************************************************************************
 * Copyright (c) 1997, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth20.api;

import java.util.Collection;

import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.api.oauth20.token.OAuth20TokenCache;

public interface OAuth20EnhancedTokenCache extends OAuth20TokenCache {

    public void initialize();

    public Collection<OAuth20Token> getAllUserTokens(String username);

    public Collection<OAuth20Token> getUserAndClientTokens(String username, String client);

    public Collection<OAuth20Token> getMatchingTokens(String username, String client, String tokenType);

    public Collection<OAuth20Token> getAll();

    // Lookup the number of tokens this user has for the given client
    public int getNumTokens(String username, String client);

    public OAuth20Token getByHash(String hash);

    public void removeByHash(String hash);

    public void addByHash(String hash, OAuth20Token entry, int lifetime);

    @Override
    public OAuth20Token get(String lookupKey);

    @Override
    public void remove(String lookupKey);

    public void stopCleanupThread();
}
