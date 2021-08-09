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
/**
 * @version 1.0.0
 */
package com.ibm.websphere.security.openidconnect;

import com.ibm.websphere.security.openidconnect.token.IdToken;
import com.ibm.ws.security.openidconnect.common.impl.PropagationHelperImpl;

public class PropagationHelper {
    /**
     * Get the type of access token which the runAsSubject authenticated
     *
     * @return the Type of Token, such as: Bearer. If failed, a null is returned
     */
    public static String getAccessTokenType() {
        return PropagationHelperImpl.getAccessTokenType();
    }

    /**
     * Get the approximate expiration time of the access_token
     * It adds the expires_in(seconds) to the time the process gets the access_token.
     * The access_token could be invalid or revoked by the OP.
     *
     * @return the expiration time of the access_token. If failed, 0 is returned
     */
    public static long getAccessTokenExpirationTime() {
        return PropagationHelperImpl.getAccessTokenExpirationTime();
    }

    /**
     *
     * @return the access token. If failed, a null is returned
     */
    public static String getAccessToken() {
        return PropagationHelperImpl.getAccessToken();
    }

    /**
     *
     * @return return all granted scopes separated by a space. If failed, a null is returned
     */
    public static String getScopes() {
        return PropagationHelperImpl.getScopes();
    }

    /**
     *
     * @return Id Token. If failed, a null is returned
     */
    public static IdToken getIdToken() {
        return PropagationHelperImpl.getIdToken();
    }

    /**
     * Retrieve userInfo information from an OpenIdConnect provider's userInfo
     * endpoint for an authenticated user.
     *
     * @return the userInfo as a String or null if the info is not available or invalid.
     */
    public static String getUserInfo() {
        return PropagationHelperImpl.getUserInfo();
    }
}
