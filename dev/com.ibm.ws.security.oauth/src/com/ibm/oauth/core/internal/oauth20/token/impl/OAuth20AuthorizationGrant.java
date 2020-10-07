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
 * Represents an OAuth 2.0 authorization grant
 */
public abstract class OAuth20AuthorizationGrant extends OAuth20TokenBase {

    private static final String TYPE = OAuth20Constants.TOKENTYPE_AUTHORIZATION_GRANT;

    /**
     * The type of authorization grant this object represents e.g.
     * "AUTHORIZATION_CODE", "REFRESH_TOKEN"
     */
    private String _grantType = null;

    public OAuth20AuthorizationGrant(String id,
            String componentId,
            String grantType,
            String stateId,
            int lifetimeSeconds,
            boolean isPersistent,
            Map<String, String[]> extensionProps) {
        super(id, componentId, TYPE, stateId, lifetimeSeconds, isPersistent, extensionProps, grantType);
        _grantType = grantType;
    }

    // public OAuth20AuthorizationGrant(String id,
    // String componentId,
    // String grantType,
    // String stateId,
    // int lifetimeSeconds,
    // boolean isPersistent) {
    // super(id, componentId, TYPE, stateId, lifetimeSeconds, isPersistent, null, grantType);
    // _grantType = grantType;
    // }

    /**
     * @return the type of grant this object represents e.g. "REFRESH_TOKEN"
     */
    public String getSubType() {
        return _grantType;
    }

    /**
     * @return a string representation of this token
     */
    public String toString() {
        StringBuffer token = new StringBuffer(super.toString());

        token.append("&grant_type=" + _grantType);

        return token.toString();
    }
}
