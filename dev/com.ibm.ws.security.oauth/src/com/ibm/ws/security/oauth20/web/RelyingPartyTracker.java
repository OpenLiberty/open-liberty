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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.webcontainer.security.CookieHelper;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;

public class RelyingPartyTracker {

    public static final String TRACK_RELYING_PARTY_COOKIE_NAME = "WasOAuthTrackRps";
    public static final String POST_LOGOUT_QUERY_PARAMETER_NAME = "clients_interacted_with";

    private final String clientIdDelimiter = ",";

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final OAuth20Provider provider;

    public RelyingPartyTracker(HttpServletRequest request, HttpServletResponse response, OAuth20Provider provider) {
        this.request = request;
        this.response = response;
        this.provider = provider;
    }

    public Cookie trackRelyingParty(String clientId) {
        ReferrerURLCookieHandler handler = getReferrerURLCookieHandler();
        Cookie trackingCookie = CookieHelper.getCookie(request.getCookies(), getCookieName());
        if (trackingCookie == null) {
            trackingCookie = createNewRelyingPartyTrackingCookie(handler, clientId);
        } else {
            trackingCookie = updateExistingTrackingCookie(trackingCookie, clientId, handler);
        }
        response.addCookie(trackingCookie);
        return trackingCookie;
    }

    public String updateLogoutUrlAndDeleteCookie(String logoutUrl) {
        if (logoutUrl == null || logoutUrl.isEmpty()) {
            return logoutUrl;
        }
        Cookie trackingCookie = CookieHelper.getCookie(request.getCookies(), getCookieName());
        if (trackingCookie == null) {
            return logoutUrl;
        }
        String updatedUrl = getUpdatedLogoutUrl(logoutUrl, trackingCookie);
        invalidateCookie();
        return updatedUrl;
    }

    ReferrerURLCookieHandler getReferrerURLCookieHandler() {
        return WebAppSecurityCollaboratorImpl.getGlobalWebAppSecurityConfig().createReferrerURLCookieHandler();
    }

    Cookie createNewRelyingPartyTrackingCookie(ReferrerURLCookieHandler handler, String clientId) {
        // Each entry in the cookie value is encoded, then the whole cookie value is encoded - hence the double encoding here
        return createCookie(handler, encodeValue(encodeValue(clientId)));
    }

    Cookie createCookie(ReferrerURLCookieHandler handler, String cookieValue) {
        Cookie cookie = handler.createCookie(getCookieName(), cookieValue, request);
        setAdditionalCookieProperties(cookie);
        return cookie;
    }

    Cookie updateExistingTrackingCookie(Cookie trackingCookie, String clientId, ReferrerURLCookieHandler handler) {
        String existingCookieValue = trackingCookie.getValue();
        if (existingCookieValue == null || existingCookieValue.isEmpty()) {
            trackingCookie = createNewRelyingPartyTrackingCookie(handler, clientId);
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
        return createCookie(handler, updatedCookieValue);
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
        trackingCookie.setPath(getCookiePath());
        trackingCookie.setSecure(true);
    }

    String getCookiePath() {
        String requestUri = request.getRequestURI();
        int lastSlashIndex = requestUri.lastIndexOf("/");
        if (lastSlashIndex < 0) {
            return requestUri;
        }
        String path = requestUri.substring(0, lastSlashIndex);
        Pattern pathPattern = Pattern.compile("(/oidc/[^/]+/[^/]+)/authorize");
        Matcher matcher = pathPattern.matcher(requestUri);
        if (matcher.matches()) {
            path = matcher.group(1);
        }
        return path;
    }

    String getUpdatedLogoutUrl(String logoutUrl, Cookie trackingCookie) {
        String existingCookieValue = trackingCookie.getValue();
        if (existingCookieValue == null || existingCookieValue.isEmpty()) {
            return logoutUrl;
        }
        List<String> clientIdList = getExistingTrackedClientIds(existingCookieValue);
        return addTrackedClientIdsToUrl(logoutUrl, clientIdList);
    }

    String addTrackedClientIdsToUrl(String url, List<String> clientIdList) {
        if (clientIdList == null || clientIdList.isEmpty()) {
            return url;
        }
        String newUrl = url;
        if (url.contains("?")) {
            newUrl += "&";
        } else {
            newUrl += "?";
        }
        newUrl += POST_LOGOUT_QUERY_PARAMETER_NAME + "=";
        for (String clientId : clientIdList) {
            try {
                newUrl += URLEncoder.encode(encodeValue(clientId), "UTF-8") + ",";
            } catch (UnsupportedEncodingException e) {
                // Do nothing - UTF-8 encoding will be supported
            }
        }
        if (newUrl.endsWith(",")) {
            // Remove trailing comma
            newUrl = newUrl.substring(0, newUrl.length() - 1);
        }
        return newUrl;
    }

    void invalidateCookie() {
        ReferrerURLCookieHandler handler = getReferrerURLCookieHandler();
        handler.invalidateReferrerURLCookie(request, response, getCookieName());
    }

    String encodeValue(String input) {
        return Base64Coder.base64Encode(input);
    }

    String decodeValue(String input) {
        return Base64Coder.base64Decode(input);
    }

    private String getCookieName() {
        return TRACK_RELYING_PARTY_COOKIE_NAME + "_" + provider.getID().hashCode();
    }

}
