/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.internal.oauth20.token.impl;

import java.util.Map;

import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.OAuth20Util;

public class OAuth20AuthorizationGrantRefreshImpl extends
        OAuth20AuthorizationGrant {

    private static final String GRANT_TYPE = OAuth20Constants.SUBTYPE_REFRESH_TOKEN;

    private static final boolean IS_PERSISTENT = true;

    private String _clientId = null;

    private String _username = null;

    private String _redirectUri = null;

    private String[] _scope = null;

    public OAuth20AuthorizationGrantRefreshImpl(String id,
            String componentId,
            String clientId,
            String username,
            String redirectUri,
            String stateId,
            String[] scope,
            int lifetimeSeconds,
            Map<String, String[]> extensionProps)
    {
        super(id, componentId, GRANT_TYPE, stateId, lifetimeSeconds, IS_PERSISTENT,
                extensionProps);
        init(clientId, username, redirectUri, scope);
    }

    private void init(String clientId,
            String username,
            String redirectUri,
            String[] scope)
    {
        _clientId = clientId;
        _username = username;
        _redirectUri = redirectUri;
        _scope = scope;
    }

    public String getClientId()
    {
        return _clientId;
    }

    public String getTokenString()
    {
        return this.getId();
    }

    public String getUsername()
    {
        return _username;
    }

    public String getRedirectUri()
    {
        return _redirectUri;
    }

    public String[] getScope()
    {
        return _scope;
    }

    public String toString()
    {
        StringBuffer grant = new StringBuffer(super.toString());

        grant.append("&client_id=" + _clientId + "&");
        grant.append("username=" + _username + "&");
        grant.append("redirect_uri=" + _redirectUri + "&");
        grant.append("scope=" + OAuth20Util.arrayToSpaceString(_scope));

        return grant.toString();
    }
}
