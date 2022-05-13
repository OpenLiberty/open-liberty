/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import org.apache.commons.codec.binary.Base64;

/**
 * Class used to help generate session id's which contain information
 * about the oidc config id, sub, sid, and timestamp.
 * The session id's are stored in the WASOidcSession cookie to keep track of
 * sessions in the RP.
 */
public class OidcSessionHelper {
    private static String DELIMITER = ",";

    /**
     * Generate a new session id using the config id, sub, sid, and timestamp by
     * concatenating them using a delimiter and then base64 encoding the result.
     * It is assumed that the inputs have been validated before creating the session id.
     * If a value does not exist (e.g., the sid claim), an empty string should be passed in.
     *
     * @param configId The oidc config id.
     * @param sub The sub claim.
     * @param sid The sid claim.
     * @param timestamp The current time.
     * @return A base64 encoded session id.
     */
    public static String createSessionId(String configId, String sub, String sid, String timestamp) {
        String sessionId = String.join(DELIMITER, configId, sub, sid, timestamp);
        return new String(Base64.encodeBase64(sessionId.getBytes()));
    }

    /**
     * Takes a base64 encoded session id and returns an OidcSessionInfo object
     * which contains the config id, sub, sid, and timestamp embedded in the session id.
     *
     * @param encodedSessionId The base64 encoded session id.
     * @return An OidcSessionInfo object containing info parsed from the session id.
     */
    public static OidcSessionInfo getSessionInfo(String encodedSessionId) {
        if (encodedSessionId == null || encodedSessionId.isEmpty()) {
            return null;
        }

        String sessionId = new String(Base64.decodeBase64(encodedSessionId));
        String[] parts = sessionId.split(DELIMITER);
        if (parts.length != 4) {
            return null;
        }

        return new OidcSessionInfo(parts[0], parts[1], parts[2], parts[3]);
    }
}
