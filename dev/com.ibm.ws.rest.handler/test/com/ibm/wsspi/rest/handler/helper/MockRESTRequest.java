/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.rest.handler.helper;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import com.ibm.wsspi.rest.handler.RESTRequest;

/**
 *
 */
public class MockRESTRequest implements RESTRequest {
    private final String mockedMethod;
    private final String mockedUserInRole;

    public MockRESTRequest(String method, String userInRole) {
        this.mockedMethod = method;
        this.mockedUserInRole = userInRole;
    }

    @Override
    public Reader getInput() throws IOException {
        return null;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return null;
    }

    @Override
    public String getHeader(String key) {
        return null;
    }

    @Override
    public String getMethod() {
        return mockedMethod;
    }

    @Override
    public String getCompleteURL() {
        return null;
    }

    @Override
    public String getURL() {
        return null;
    }

    @Override
    public String getURI() {
        return null;
    }

    @Override
    public String getContextPath() {
        return null;
    }

    @Override
    public String getPath() {
        return null;
    }

    @Override
    public String getQueryString() {
        return null;
    }

    @Override
    public String getParameter(String name) {
        return null;
    }

    @Override
    public String[] getParameterValues(String name) {
        return null;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return null;
    }

    @Override
    public Principal getUserPrincipal() {
        return null;
    }

    @Override
    public boolean isUserInRole(String role) {
        if (mockedUserInRole.equals(role)) {
            return true;
        }
        return false;
    }

    @Override
    public String getPathVariable(String variable) {
        return null;
    }

    @Override
    public Locale getLocale() {
        return null;
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return null;
    }

    @Override
    public String getRemoteAddr() {
        return null;
    }

    @Override
    public String getRemoteHost() {
        return null;
    }

    @Override
    public int getRemotePort() {
        return 0;
    }

    @Override
    public InputStream getPart(String partName) throws IOException {
        return null;
    }

    @Override
    public boolean isMultiPartRequest() {
        return false;
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public String getSessionId() {
        return null;
    }

}
