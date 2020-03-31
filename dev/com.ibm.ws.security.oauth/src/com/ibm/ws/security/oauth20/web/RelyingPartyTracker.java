/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.web;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.webcontainer.security.CookieHelper;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;

public class RelyingPartyTracker {

    public static final String TRACK_RELYING_PARTY_COOKIE_NAME = "WasOidcTrackRps";

    private final String clientIdDelimiter = ",";

    private final HttpServletRequest request;
    private final HttpServletResponse response;

    public RelyingPartyTracker(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    public void trackRelyingParty(String clientId) {
        ReferrerURLCookieHandler handler = getReferrerURLCookieHandler();
        Cookie trackingCookie = CookieHelper.getCookie(request.getCookies(), TRACK_RELYING_PARTY_COOKIE_NAME);
        if (trackingCookie == null) {
            trackingCookie = createNewRelyingPartyTrackingCookie(request, handler, clientId);
        } else {
            trackingCookie = updateExistingTrackingCookie(trackingCookie, clientId, handler);
        }
        setAdditionalCookieProperties(trackingCookie);
        response.addCookie(trackingCookie);
    }

    ReferrerURLCookieHandler getReferrerURLCookieHandler() {
        return WebAppSecurityCollaboratorImpl.getGlobalWebAppSecurityConfig().createReferrerURLCookieHandler();
    }

    Cookie createNewRelyingPartyTrackingCookie(HttpServletRequest request, ReferrerURLCookieHandler handler, String clientId) {
        // Each entry in the cookie value is encoded, then the whole cookie value is encoded - hence the double encoding here
        return handler.createCookie(TRACK_RELYING_PARTY_COOKIE_NAME, encodeValue(encodeValue(clientId)), request);
    }

    Cookie updateExistingTrackingCookie(Cookie trackingCookie, String clientId, ReferrerURLCookieHandler handler) {
        String existingCookieValue = trackingCookie.getValue();
        if (existingCookieValue == null || existingCookieValue.isEmpty()) {
            trackingCookie = createNewRelyingPartyTrackingCookie(request, handler, clientId);
        } else {
            trackingCookie = updateExistingCookieValue(existingCookieValue, clientId, handler);
        }
        return trackingCookie;
    }

    Cookie updateExistingCookieValue(String existingCookieValue, String clientId, ReferrerURLCookieHandler handler) {
        List<String> clientIdList = getExistingTrackedClientIds(existingCookieValue);
        if (!clientIdList.contains(clientId)) {
            clientIdList.add(clientId);
        }
        String updatedCookieValue = createCookieValue(clientIdList);
        return handler.createCookie(TRACK_RELYING_PARTY_COOKIE_NAME, updatedCookieValue, request);
    }

    List<String> getExistingTrackedClientIds(String rawExistingCookieValue) {
        List<String> clientIdList = new ArrayList<String>();
        String decodedCookieValue = decodeValue(rawExistingCookieValue);
        if (decodedCookieValue == null) {
            return clientIdList;
        }
        String[] cookieEntries = decodedCookieValue.split(clientIdDelimiter);
        for (String clientIdEntry : cookieEntries) {
            String decodedClientIdEntry = decodeValue(clientIdEntry);
            if (decodedClientIdEntry != null && !clientIdList.contains(decodedClientIdEntry)) {
                clientIdList.add(decodedClientIdEntry);
            }
        }
        return clientIdList;
    }

    String createCookieValue(List<String> clientIdSet) {
        String updatedCookieValue = "";
        for (String clientIdEntry : clientIdSet) {
            String encodedClientIdEntry = encodeValue(clientIdEntry);
            updatedCookieValue += encodedClientIdEntry + clientIdDelimiter;
        }
        if (updatedCookieValue.endsWith(clientIdDelimiter)) {
            // Remove trailing delimiter
            updatedCookieValue = updatedCookieValue.substring(0, updatedCookieValue.length() - clientIdDelimiter.length());
        }
        return encodeValue(updatedCookieValue);
    }

    void setAdditionalCookieProperties(Cookie trackingCookie) {
        String requestUri = request.getRequestURI();
        // /oidc/endpoint/OidcConfigSample/authorize
        System.out.println("getRequestURI: [" + requestUri + "]");
        // TODO Set cookie path
    }

    String encodeValue(String input) {
        return Base64Coder.base64Encode(input);
    }

    String decodeValue(String input) {
        return Base64Coder.base64Decode(input);
    }

}
