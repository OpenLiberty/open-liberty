/*******************************************************************************
 * Copyright (c) 2013, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rest.handler.helper;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.ibm.wsspi.rest.handler.RESTRequest;

/**
 * Implementation of RESTRequest that uses an HttpServletRequest object.
 */
public class ServletRESTRequestImpl implements RESTRequest {

    private final HttpServletRequest request;

    /**
     * Currently, RESTRequestImpl merely wraps a request.
     * That said, we do not want to expose this object directly as we may
     * replace the underlying mechanism some day to make the handler
     * lighter-weight.
     *
     * @param response The request to wrap.
     */
    public ServletRESTRequestImpl(HttpServletRequest request) {
        this.request = request;
        try {
            this.request.setCharacterEncoding("UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Ignore the exception, it will fall back to default encoding
        }
    }

    /** {@inheritDoc} */
    @Override
    public Reader getInput() throws IOException {
        return request.getReader();
    }

    /** {@inheritDoc} */
    @Override
    public String[] getParameterValues(String name) {
        return request.getParameterValues(name);
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
        final String url = request.getRequestURL().toString();
        final String queryString = request.getQueryString();

        if (queryString != null && queryString.length() > 0) {
            return url + "?" + queryString;
        } else {
            return url;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getURL() {
        return request.getRequestURL().toString();
    }

    /** {@inheritDoc} */
    @Override
    public String getURI() {
        return request.getRequestURI();
    }

    /** {@inheritDoc} */
    @Override
    public String getPath() {
        return request.getPathInfo();
    }

    /**
     * {@inheritDoc}
     *
     * @throws ServletException
     * @throws IOException
     */
    @Override
    public InputStream getPart(String partName) throws IOException {
        try {
            Part part = request.getPart(partName);
            return (part == null) ? null : part.getInputStream();
        } catch (ServletException e) {
            throw new IOException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMultiPartRequest() {
        boolean isMultiPart = ServletFileUpload.isMultipartContent(request);
        return isMultiPart;
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
        return null;
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
        return request.getSession().getId();
    }

}
