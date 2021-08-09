/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.server.plugins;

import java.util.Map;

import com.ibm.ws.security.oauth20.plugins.OAuth20TokenImpl;
import com.ibm.ws.security.oauth20.util.OIDCConstants;

public class IDTokenImpl extends OAuth20TokenImpl {

    private static final long serialVersionUID = -2816928040364640013L;

    private static final String TOKEN_TYPE = OIDCConstants.TOKENTYPE_ID_TOKEN;
    private static final String TOKEN_SUBTYPE = OIDCConstants.ID_TOKEN;

    public IDTokenImpl(String id, String tokenString, String componentId, String clientId, String username,
                       String redirectUri, String stateId, String[] scope, int lifetimeSeconds,
                       Map<String, String[]> extensionProperties, String grantType) {

        super(id, componentId, TOKEN_TYPE, TOKEN_SUBTYPE, System.currentTimeMillis(),
              lifetimeSeconds, tokenString, clientId, username, scope, redirectUri, stateId, extensionProperties, grantType);
    }

}
