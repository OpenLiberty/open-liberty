/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.plugins;

import java.util.Map;

import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;

public class OAuth20BearerTokenImpl extends OAuth20TokenImpl {

    private static final long serialVersionUID = -2816928040364640013L;

    private static final String TOKEN_TYPE = OAuth20Constants.TOKENTYPE_ACCESS_TOKEN;
    private static final String TOKEN_SUBTYPE = OAuth20Constants.SUBTYPE_BEARER;

    public OAuth20BearerTokenImpl(String id, String token, String componentId,
            String clientId, String username, String redirectUri,
            String stateId, String[] scope, int lifetimeSeconds,
            Map<String, String[]> extensionProperties, String grantType) {

        super(id, componentId, TOKEN_TYPE, TOKEN_SUBTYPE, System
                .currentTimeMillis(), lifetimeSeconds, token, clientId, username,
                scope, redirectUri, stateId, extensionProperties, grantType);
    }

}
