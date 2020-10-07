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

/**
 * Represents an OAuth 2.0 access token
 */
public abstract class OAuth20AccessToken extends OAuth20TokenBase {

    private static final String TYPE = OAuth20Constants.TOKENTYPE_ACCESS_TOKEN;

    /**
     * The type of access token this object represents e.g. "BEARER", "MAC"
     */
    private String _tokenType = null;

    public OAuth20AccessToken(String id, String componentId, String tokenType,
            String stateId, int lifetimeSeconds, boolean isPersistent,
            Map<String, String[]> extensionProperties, String grantType) {
        super(id, componentId, TYPE, stateId, lifetimeSeconds, isPersistent,
                extensionProperties, grantType);

        _tokenType = tokenType;
    }

    /**
     * @return the type of access token this object represents e.g. "BEARER"
     */
    public String getSubType() {
        return _tokenType;
    }

    /**
     * @return a string representation of this token
     */
    public String toString() {
        StringBuffer token = new StringBuffer(super.toString());

        token.append("&token_type=" + _tokenType);

        return token.toString();
    }
}
