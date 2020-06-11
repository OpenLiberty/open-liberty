/*******************************************************************************
 * Copyright (c) 2004, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.internal.values;

import java.util.Iterator;
import java.util.List;

import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpResponseMessage;

public class AccessLogRequestCookie extends AccessLogData {

    public AccessLogRequestCookie() {
        super("%C");
        //%{CookieName}C
        // The contents of cookie CookieName in the request sent to the server.
    }

    @Override
    public Object init(String rawToken) {
        if (rawToken != null && rawToken.length() == 0) {
            return null;
        }

        return rawToken;
    }

    @Override
    public boolean set(StringBuilder accessLogEntry,
                       HttpResponseMessage response, HttpRequestMessage request, Object data) {

        String cookieName = (String) data;
        HttpCookie headerCookie = null;
        List<HttpCookie> cookieValues = null;

        if (cookieName != null) {
            headerCookie = request.getCookie(cookieName);
        } else {
            cookieValues = request.getAllCookies();
        }

        if (headerCookie != null) {
            accessLogEntry.append(headerCookie.getName());
            accessLogEntry.append(":");
            accessLogEntry.append(headerCookie.getValue());
        } else if (cookieValues != null && !cookieValues.isEmpty()) {
            Iterator<HttpCookie> iter = cookieValues.iterator();
            while (iter.hasNext()) {
                HttpCookie token = iter.next();
                accessLogEntry.append(token.getName());
                accessLogEntry.append(":");
                accessLogEntry.append(token.getValue());
                if (iter.hasNext()) {
                    accessLogEntry.append(" ");
                }
            }
        } else {
            accessLogEntry.append("-");
        }

        return true;
    }

    public static HttpCookie getCookie(HttpResponseMessage response, HttpRequestMessage request, Object data) {
        String cookieName = (String) data;
        HttpCookie headerCookie = null;

        if (cookieName != null) {
            headerCookie = request.getCookie(cookieName);
        }

        return headerCookie;
    }

    public static List<HttpCookie> getAllCookies(HttpResponseMessage response, HttpRequestMessage request, Object data) {
        List<HttpCookie> cookieValues = null;
        cookieValues = request.getAllCookies();
        return cookieValues;
    }

}
