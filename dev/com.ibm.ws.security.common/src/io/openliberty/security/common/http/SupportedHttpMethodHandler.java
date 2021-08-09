/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.common.http;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SupportedHttpMethodHandler {

    public static enum HttpMethod {
        GET, HEAD, POST, DELETE, PUT, TRACE, OPTIONS
    }

    protected HttpServletRequest request;
    protected HttpServletResponse response;

    public SupportedHttpMethodHandler(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    public boolean isValidHttpMethodForRequest(HttpMethod requestMethod) {
        return true;
    };

    public void sendHttpOptionsResponse() throws IOException {
        Set<HttpMethod> supportedMethods = getAllHttpMethods();
        setAllowHeaderAndSendResponse(supportedMethods);
    }

    public void sendUnsupportedMethodResponse() throws IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    protected void setAllowHeaderAndSendResponse(Set<HttpMethod> supportedMethods) {
        String allowHeaderValue = buildHeaderValue(supportedMethods);
        if (allowHeaderValue != null) {
            response.setHeader("Allow", allowHeaderValue);
        }
        response.setStatus(HttpServletResponse.SC_OK);
    }

    String buildHeaderValue(Set<HttpMethod> supportedMethods) {
        if (supportedMethods == null) {
            return null;
        }
        String allowHeaderValue = "";
        Iterator<HttpMethod> iter = supportedMethods.iterator();
        while (iter.hasNext()) {
            allowHeaderValue += iter.next();
            if (iter.hasNext()) {
                allowHeaderValue += ", ";
            }
        }
        return allowHeaderValue;
    }

    private Set<HttpMethod> getAllHttpMethods() {
        Set<HttpMethod> allMethods = new HashSet<HttpMethod>();
        HttpMethod[] allMethodsArray = HttpMethod.values();
        for (HttpMethod method : allMethodsArray) {
            allMethods.add(method);
        }
        return allMethods;
    }

}