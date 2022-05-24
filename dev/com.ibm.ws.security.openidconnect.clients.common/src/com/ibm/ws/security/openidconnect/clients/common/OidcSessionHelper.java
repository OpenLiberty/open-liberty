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
    private static String DELIMITER = ":";

    /**
     * Generate a new session id using the config id, sub, sid, and timestamp in the
     * format of 'Base64(configId):Base64(sub):Base64(sid):Base64(timestamp)'.
     * It is assumed that the inputs have been validated before creating the session id.
     * If a value does not exist (e.g., the sid claim), an empty string should be passed in.
     *
     * @param configId The oidc config id.
     * @param sub The sub claim.
     * @param sid The sid claim.
     * @param timestamp The current time.
     * @return A session id in the format 'Base64(configId):Base64(sub):Base64(sid):Base64(timestamp)'.
     */
    public static String createSessionId(String configId, String sub, String sid, String timestamp) {
        String encodedConfigId = new String(Base64.encodeBase64(configId.getBytes()));
        String encodedSub = new String(Base64.encodeBase64(sub.getBytes()));
        String encodedSid = new String(Base64.encodeBase64(sid.getBytes()));
        String encodedTimestamp = new String(Base64.encodeBase64(timestamp.getBytes()));
        
        return String.join(DELIMITER, encodedConfigId, encodedSub, encodedSid, encodedTimestamp);
    }

    /**
     * Takes a session id and returns an OidcSessionInfo object which contains 
     * the config id, sub, sid, and timestamp embedded in the session id.
     *
     * @param sessionId The session id.
     * @return An OidcSessionInfo object containing info parsed from the session id.
     */
    public static OidcSessionInfo getSessionInfo(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return null;
        }

        String[] parts = sessionId.split(DELIMITER);
        if (parts.length != 4) {
            return null;
        }
        
        String configId = new String(Base64.decodeBase64(parts[0]));
        String sub = new String(Base64.decodeBase64(parts[1]));
        String sid = new String(Base64.decodeBase64(parts[2]));
        String timestamp = new String(Base64.decodeBase64(parts[3]));

        return new OidcSessionInfo(configId, sub, sid, timestamp);
    }
}
