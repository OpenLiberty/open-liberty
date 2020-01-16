/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.plugins;

import java.util.HashSet;
import java.util.Set;

import com.ibm.ws.security.oauth20.api.Constants;
import com.ibm.ws.security.oauth20.util.OidcOAuth20Util;

/**
 *
 */
public class OidcBaseClientScopeReducer {
    private Set<String> scopeSet = new HashSet<String>();
    private Set<String> preAuthorizedScopeSet = new HashSet<String>();
    private static final String SPACE = " ";
    private boolean bAllScopes = false;
    private boolean bNullEmptyScope = false;
    private boolean bNullEmptyPreAuthorized = false;

    public OidcBaseClientScopeReducer(OidcBaseClient client) {
        if (client == null) {
            throw new IllegalArgumentException("OidcBaseClientScopeReducer must be instantiated with a non-null client value.");
        }

        bNullEmptyScope = setScopeSet(client.getScope(), scopeSet);
        bNullEmptyPreAuthorized = setScopeSet(client.getPreAuthorizedScope(), preAuthorizedScopeSet);
    }

    private boolean setScopeSet(String scopes, Set<String> myScopeSet) {
        if (OidcOAuth20Util.isNullEmpty(scopes)) {
            return true;
        }
        String trimmedScopes = scopes.trim();

        String[] scopeArr = trimmedScopes.split(SPACE);
        for (String scope : scopeArr) {
            myScopeSet.add(scope);
            if (Constants.ALL_SCOPES.equals(scope)) {
                bAllScopes = true;
            }
        }
        return false;
    }

    public boolean hasClientScope(String requestScope) {
        if (OidcOAuth20Util.isNullEmpty(requestScope)) {
            return false;
        }
        if (bAllScopes)
            return true;
        return this.scopeSet.contains(requestScope.trim());
    }

    public boolean hasClientPreAuthorizedScope(String requestScope) {
        if (OidcOAuth20Util.isNullEmpty(requestScope)) {
            return false;
        }
        if (hasClientScope(requestScope)) {
            return this.preAuthorizedScopeSet.contains(requestScope.trim());
        } else {
            return false;
        }
    }

    public boolean isNullEmptyScope() {
        return bNullEmptyScope;
    }
}
