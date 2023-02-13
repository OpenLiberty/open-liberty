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
package io.openliberty.security.jakartasec.fat.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gargoylesoftware.htmlunit.util.Cookie;
import com.gargoylesoftware.htmlunit.util.NameValuePair;

import componenttest.topology.impl.LibertyServer;

public class ResponseValues {

    static private final Class<?> thisClass = ResponseValues.class;

    LibertyServer rpServer = null;
    String subject = "testuser";
    String clientId = "client_1";
    String realm = "BasicRealm";
    String issuer = "https://localhost:8920/oidc/endpoint/OP1"; // users should always override this
    String tokenType = "Bearer";
    Map<String, String> headers = null;
    List<NameValuePair> parms = null;
    List<Cookie> cookies = null;
    String originalRequest = null;
    boolean useSession = true;
    boolean useAuthApp = false;
    String baseApp = ServletMessageConstants.BASE_SERVLET;

    public void setRPServer(LibertyServer inRPServer) {

        rpServer = inRPServer;
    }

    public LibertyServer getRPServer() {

        return rpServer;
    }

    public void setSubject(String inSubject) {

        subject = inSubject;
    }

    public String getSubject() {

        return subject;
    }

    public void setClientId(String inClientId) {

        clientId = inClientId;
    }

    public String getClientId() {

        return clientId;
    }

    public void setRealm(String inRealm) {

        realm = inRealm;
    }

    public String getRealm() {

        return realm;
    }

    public void setIssuer(String inIssuer) {

        issuer = inIssuer;
    }

    public String getIssuer() {

        return issuer;
    }

    public void setTokenType(String inTokenType) {

        tokenType = inTokenType;
    }

    public String getTokenType() {

        return tokenType;
    }

    public void setOriginalRequest(String inOriginalRequest) {

        originalRequest = inOriginalRequest;

    }

    public String getOriginalRequest() {

        return originalRequest;
    }

    public void setHeaders(Map<String, String> inHeaders) {

        if (inHeaders != null) {
            headers = new HashMap<String, String>();
            for (Map.Entry<String, String> header : inHeaders.entrySet()) {
                headers.put(header.getKey(), header.getValue());
            }
        }

    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setParms(List<NameValuePair> inParms) {

        if (inParms != null) {
            parms = new ArrayList<NameValuePair>();
            for (NameValuePair parm : inParms) {
                parms.add(new NameValuePair(parm.getName(), parm.getValue()));
            }
        }

    }

    public List<NameValuePair> getParms() {
        return parms;
    }

    public void setCookies(Cookie... inCookies) {

        if (inCookies != null && inCookies.length != 0) {
            for (Cookie c : inCookies) {
                if (cookies == null) {
                    new ArrayList<>(Arrays.asList(new Cookie("", c.getName(), c.getValue())));
                } else {
                    // we're only setting the name and value, and will only be checking those values
                    cookies.add(new Cookie("", c.getName(), c.getValue()));
                }
            }
        }
    }

    public List<Cookie> getCookies() {
        return cookies;
    }

    public void setUseSession(boolean inUseSession) {
        useSession = inUseSession;
    }

    public boolean getUseSession() {
        return useSession;
    }

    public void setBaseApp(String inBaseApp) {
        baseApp = inBaseApp;
    }

    public String getBaseApp() {
        return baseApp;
    }

    public void setUseAuthApp(boolean inUseAuthApp) {
        useAuthApp = inUseAuthApp;
    }

    public boolean getUseAuthApp() {
        return useAuthApp;
    }
}
