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

import java.util.Objects;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.webcontainer.security.CookieHelper;

public class OidcSessionInfo {

    private static TraceComponent tc = Tr.register(OidcSessionInfo.class);

    private static String DELIMITER = ":";

    private final String sessionId;
    private final String configId;
    private final String iss;
    private final String sub;
    private final String sid;
    private final String timestamp;

    private final static EncryptionUtils encryptionUtils = new EncryptionUtils();

    public OidcSessionInfo(String configId, String iss, String sub, String sid, String timestamp, @Sensitive String clientSecret) throws OidcSessionException {
        if (configId == null) configId = "";
        if (iss == null) iss = "";
        if (sub == null) sub = "";
        if (sid == null) sid = "";
        if (timestamp == null) timestamp = "";

        this.configId = configId;
        this.iss = iss;
        this.sub = sub;
        this.sid = sid;
        this.timestamp = timestamp;
        this.sessionId = createSessionId(clientSecret);
    }

    /**
     * Gets the session id in the format of
     * 'AES(Base64(configId):Base64(iss):Base64(sub):Base64(sid):Base64(timestamp), clientSecret)'
     * from the request cookies and returns an OidcSessionInfo object which contains
     * the config id, iss, sub, sid, and timestamp.
     *
     * @param request The http servlet request.
     * @return An OidcSessionInfo object containing info parsed from the session id.
     * @throws OidcSessionException
     */
    public static OidcSessionInfo getSessionInfo(HttpServletRequest request, @Sensitive String clientSecret) throws OidcSessionException {
        try {
            String encryptedSessionId = getSessionIdFromCookies(request.getCookies());
            if (encryptedSessionId == null || encryptedSessionId.isEmpty()) {
                return null;
            }

            String sessionId = encryptionUtils.aesDecrypt(clientSecret, encryptedSessionId);
            String[] parts = sessionId.split(DELIMITER, -1);
            if (parts.length != 5) {
                return null;
            }

            String configId = new String(Base64.decodeBase64(parts[0]));
            String iss = new String(Base64.decodeBase64(parts[1]));
            String sub = new String(Base64.decodeBase64(parts[2]));
            String sid = new String(Base64.decodeBase64(parts[3]));
            String timestamp = new String(Base64.decodeBase64(parts[4]));

            return new OidcSessionInfo(configId, iss, sub, sid, timestamp, clientSecret);
        } catch (Exception e) {
            String errorMsg = Tr.formatMessage(tc, "OIDC_SESSION_PARSING_FAILURE", new Object[] { e.getLocalizedMessage() });
            throw new OidcSessionException(errorMsg);
        }
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
     * format of 'AES(Base64(configId):Base64(iss):Base64(sub):Base64(sid):Base64(timestamp), clientSecret)'.
     * It is assumed that the inputs have been validated before creating the session id.
     *
     * @return A session id in the format 'AES(Base64(configId):Base64(iss):Base64(sub):Base64(sid):Base64(timestamp), clientSecret)'.
     * @throws OidcSessionException
     */
    private String createSessionId(@Sensitive String clientSecret) throws OidcSessionException {
        try {
            String encodedConfigId = new String(Base64.encodeBase64(configId.getBytes()));
            String encodedIss = new String(Base64.encodeBase64(iss.getBytes()));
            String encodedSub = new String(Base64.encodeBase64(sub.getBytes()));
            String encodedSid = new String(Base64.encodeBase64(sid.getBytes()));
            String encodedTimestamp = new String(Base64.encodeBase64(timestamp.getBytes()));

            String encodedSessionId = String.join(DELIMITER, encodedConfigId, encodedIss, encodedSub, encodedSid, encodedTimestamp);

            return encryptionUtils.aesEncrypt(clientSecret, encodedSessionId);
        } catch (Exception e) {
            String errorMsg = Tr.formatMessage(tc, "OIDC_SESSION_CREATION_FAILURE", new Object[] { e.getLocalizedMessage() });
            throw new OidcSessionException(errorMsg);
        }
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