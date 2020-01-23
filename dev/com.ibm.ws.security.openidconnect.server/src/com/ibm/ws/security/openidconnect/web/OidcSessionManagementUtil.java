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
package com.ibm.ws.security.openidconnect.web;

import java.security.MessageDigest;

import com.ibm.oauth.core.internal.OAuthUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.common.internal.encoder.Base64Coder;

/**
 * Class used to facilitate compliance with the OpenID Connect Session
 * Management specification found at: http://openid.net/specs/openid-connect-session-1_0.html
 */
public class OidcSessionManagementUtil {
    private static final TraceComponent tc = Tr.register(OidcSessionManagementUtil.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    public final static String ENCODING = "UTF-8";
    public final static int RANDOM_STATE_LENGTH = 40;

    /**
     * Calculate a new session state given the provided parameters. According
     * to the OIDC Session Management spec, session state must be calculated
     * using the client ID, the source origin URL from the RP, and the current
     * OP browser state. Based on issues still under discussion, however, the
     * origin URL is not used in the calculation in this implementation.
     * Inclusion of a salt provides sufficient randomness to prevent
     * identification of an end-user across successive calls to the
     * authorization endpoint and is recommended by the spec.
     * 
     * @param clientId
     * @param state
     * @param salt
     * @return
     */
    public static String calculateSessionState(String clientId, String state, String salt) {
        // Session state = hash(client ID + current state + salt)
        StringBuilder stateBuilder = new StringBuilder();
        stateBuilder = stateBuilder.append(clientId).append(state);
        if (salt != null && !salt.isEmpty()) {
            stateBuilder.append(salt);
        }
        String stringToHash = stateBuilder.toString();

        String newState = null;
        try {
            String hashAlg = "SHA-256";
            MessageDigest messageDigest = MessageDigest.getInstance(hashAlg);
            byte[] digest = messageDigest.digest(stringToHash.getBytes(ENCODING));
            if (digest != null) {
                byte[] encodedDigest = Base64Coder.base64Encode(digest);
                newState = new String(encodedDigest, ENCODING);
            }
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception caught while attempting to create new browser state: " + e);
            }
        }
        if (newState == null) {
            newState = OAuthUtil.getRandom(RANDOM_STATE_LENGTH);
        }
        if (salt != null && !salt.isEmpty()) {
            newState = newState + "." + salt;
        }
        return newState;
    }

}
