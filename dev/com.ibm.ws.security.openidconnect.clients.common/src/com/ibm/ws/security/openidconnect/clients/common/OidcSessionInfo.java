/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import java.util.Objects;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer.security.CookieHelper;

public class OidcSessionInfo {

    private static final TraceComponent tc = Tr.register(OidcSessionInfo.class);

    private static String DELIMITER = ":";

    private final String sessionId;
    private final String configId;
    private final String iss;
    private final String sub;
    private final String sid;
    private final String timestamp;

    public OidcSessionInfo(String configId, String iss, String sub, String sid, String timestamp, ConvergedClientConfig clientConfig) {
        this.configId = configId != null ? configId : "";
        this.iss = iss != null ? iss : "";
        this.sub = sub != null ? sub : "";
        this.sid = sid != null ? sid : "";
        this.timestamp = timestamp != null ? timestamp : "";

        this.sessionId = createSessionId(clientConfig);
    }

    /**
     * Gets the base64 encoded session id from the request cookies
     * and returns an OidcSessionInfo object which contains
     * the config id, sub, sid, and timestamp embedded in the session id.
     *
     * @param request The http servlet request.
     * @return An OidcSessionInfo object containing info parsed from the session id.
     */
    public static OidcSessionInfo getSessionInfo(HttpServletRequest request, ConvergedClientConfig clientConfig) {
        String sessionId = getSessionIdFromCookies(request.getCookies());
        if (sessionId == null || sessionId.isEmpty()) {
            return null;
        }

        String sessionIdWithoutHash = sessionId.split("_")[0];
        String testSessionId = OidcClientUtil.addSignatureToStringValue(sessionIdWithoutHash, clientConfig);

        if (!testSessionId.equals(sessionId)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "The cookie may have been tampered with.");
            }
            return null;
        }

        String[] parts = sessionIdWithoutHash.split(DELIMITER);
        if (parts.length != 5) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "The cookie may have been tampered with.");
            }
            return null;
        }

        String configId = new String(Base64.decodeBase64(parts[0]));
        String iss = new String(Base64.decodeBase64(parts[1]));
        String sub = new String(Base64.decodeBase64(parts[2]));
        String sid = new String(Base64.decodeBase64(parts[3]));
        String timestamp = new String(Base64.decodeBase64(parts[4]));

        return new OidcSessionInfo(configId, iss, sub, sid, timestamp, clientConfig);
    }

    private static String getSessionIdFromCookies(Cookie[] cookies) {
        return CookieHelper.getCookieValue(cookies, ClientConstants.WAS_OIDC_SESSION);
    }

    public String getSessionId() {
        return this.sessionId;
    }

    public String getConfigId() {
        return this.configId;
    }

    public String getIss() {
        return this.iss;
    }

    public String getSub() {
        return this.sub;
    }

    public String getSid() {
        return this.sid;
    }

    public String getTimestamp() {
        return this.timestamp;
    }

    @Override
    public String toString() {
        return sessionId;
    }

    /**
     * Generate a new session id using the config id, sub, sid, and timestamp in the
     * format of 'Base64(configId):Base64(sub):Base64(sid):Base64(timestamp)'.
     * A signature is then appended using a hash of the session id and the client secret.
     * It is assumed that the inputs have been validated before creating the session id.
     *
     * @return A session id in the format 'Base64(configId):Base64(sub):Base64(sid):Base64(timestamp)_Signature(SessionId, ClientSecret)'.
     */
    private String createSessionId(ConvergedClientConfig clientConfig) {
        String encodedConfigId = new String(Base64.encodeBase64(configId.getBytes()));
        String encodedIss = new String(Base64.encodeBase64(iss.getBytes()));
        String encodedSub = new String(Base64.encodeBase64(sub.getBytes()));
        String encodedSid = new String(Base64.encodeBase64(sid.getBytes()));
        String encodedTimestamp = new String(Base64.encodeBase64(timestamp.getBytes()));

        String sessionId = String.join(DELIMITER, encodedConfigId, encodedIss, encodedSub, encodedSid, encodedTimestamp);
        return OidcClientUtil.addSignatureToStringValue(sessionId, clientConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(configId, iss, sid, sub, timestamp);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OidcSessionInfo other = (OidcSessionInfo) obj;
        return Objects.equals(configId, other.configId) && Objects.equals(iss, other.iss) && Objects.equals(sid, other.sid) && Objects.equals(sub, other.sub) && Objects.equals(timestamp, other.timestamp);
    }

}