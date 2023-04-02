/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.internal.oauth20;

import java.util.HashMap;
import java.util.Map;

import com.ibm.oauth.core.api.oauth20.client.OAuth20Client;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;

/**
 * This helper class provides some per-request caching of data retrieved from
 * external plugins for performance purposes.
 * 
 */
public class OAuth20RequestContext {
    Map<String, OAuth20Token> _tokenCache;
    Map<String, OAuth20Client> _clientCache;

    public OAuth20RequestContext() {
        _tokenCache = new HashMap<String, OAuth20Token>();
        _clientCache = new HashMap<String, OAuth20Client>();
    }

    Map<String, OAuth20Token> getRequestTokenCache() {
        return _tokenCache;
    }

    Map<String, OAuth20Client> getRequestClientCache() {
        return _clientCache;
    }
}
