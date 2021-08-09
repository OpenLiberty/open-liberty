/*******************************************************************************
 * Copyright (c) 2014, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rest.handler.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletException;

import com.ibm.wsspi.rest.handler.RESTRequest;

/**
 * Implementation that extends another RESTRequest object and adds extended support, such as path variables.
 */
public class ExtendedRESTRequestImpl implements RESTRequest {

    private final RESTRequest request;
    private final Map<String, String> pathVariables;

    /**
     * Currently, RESTRequestImpl merely wraps a request.
     * That said, we do not want to expose this object directly as we may
     * replace the underlying mechanism some day to make the handler
     * lighter-weight.
     *
     * @param response The request to wrap.
     */
    public ExtendedRESTRequestImpl(RESTRequest request, Map<String, String> pathVariables) {
        this.request = request;
        this.pathVariables = pathVariables;
    }

    /** {@inheritDoc} */
    @Override
    public Reader getInput() throws IOException {
        return request.getInput();
    }

    /** {@inheritDoc} */
    @Override
    public InputStream getInputStream() throws IOException {
        return request.getInputStream();
    }

    /** {@inheritDoc} */
    @Override
    public String getHeader(String key) {
        return request.getHeader(key);
    }

    /** {@inheritDoc} */
    @Override
    public String getMethod() {
        return request.getMethod();
    }

    /** {@inheritDoc} */
    @Override
    public String getCompleteURL() {
        return request.getCompleteURL();
    }

    /** {@inheritDoc} */
    @Override
    public String getURL() {
        return request.getURL();
    }

    /** {@inheritDoc} */
    @Override
    public String getURI() {
        return request.getURI();
    }

    /** {@inheritDoc} */
    @Override
    public String getPath() {
        return request.getPath();
    }

    /** {@inheritDoc} */
    @Override
    public String getQueryString() {
        return request.getQueryString();
    }

    /** {@inheritDoc} */
    @Override
    public String getParameter(String name) {
        return request.getParameter(name);
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, String[]> getParameterMap() {
        return request.getParameterMap();
    }

    /** {@inheritDoc} */
    @Override
    public Principal getUserPrincipal() {
        return request.getUserPrincipal();
    }

    /** {@inheritDoc} */
    @Override
    public String getPathVariable(String variable) {
        return pathVariables.get(variable);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isUserInRole(String role) {
        return request.isUserInRole(role);
    }

    /** {@inheritDoc} */
    @Override
    public Locale getLocale() {
        return request.getLocale();
    }

    /** {@inheritDoc} */
    @Override
    public Enumeration<Locale> getLocales() {
        return request.getLocales();
    }

    /** {@inheritDoc} */
    @Override
    public String getRemoteAddr() {
        return request.getRemoteAddr();
    }

    /** {@inheritDoc} */
    @Override
    public String getRemoteHost() {
        return request.getRemoteHost();
    }

    /** {@inheritDoc} */
    @Override
    public int getRemotePort() {
        return request.getRemotePort();
    }

    /** {@inheritDoc} */
    @Override
    public String[] getParameterValues(String name) {
        return request.getParameterValues(name);
    }

    /**
     * {@inheritDoc}
     *
     * @throws ServletException
     * @throws IOException
     */
    @Override
    public InputStream getPart(String partName) throws IOException {
        return request.getPart(partName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMultiPartRequest() {
        return request.isMultiPartRequest();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getContextPath() {
        return request.getContextPath();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getContentType() {
        return request.getContentType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSessionId() {
        return request.getSessionId();
    }

}
