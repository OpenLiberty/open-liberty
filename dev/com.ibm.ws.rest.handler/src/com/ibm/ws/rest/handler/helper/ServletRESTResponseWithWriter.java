/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;

import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 *
 */
public class ServletRESTResponseWithWriter implements RESTResponse {

    private StringWriter stringWriter;
    private final RESTResponse response;

    /**
     * @param response The HttpServletResponseImpl to wrap.
     */
    public ServletRESTResponseWithWriter(RESTResponse response) {
        this.response = response;
        this.stringWriter = new StringWriter();
    }

    public Writer geStringtWriter() throws IOException {
        return stringWriter;
    }

    public void writeToWriter(String content) {
        stringWriter = stringWriter.append(content);
    }

    public String writerToString() {
        return stringWriter.toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.rest.handler.RESTResponse#getWriter()
     */
    @Override
    public Writer getWriter() throws IOException {
        return response.getWriter();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.rest.handler.RESTResponse#getOutputStream()
     */
    @Override
    public OutputStream getOutputStream() throws IOException {
        return response.getOutputStream();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.rest.handler.RESTResponse#setResponseHeader(java.lang.String, java.lang.String)
     */
    @Override
    public void setResponseHeader(String key, String value) {
        this.response.setResponseHeader(key, value);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.rest.handler.RESTResponse#addResponseHeader(java.lang.String, java.lang.String)
     */
    @Override
    public void addResponseHeader(String key, String value) {
        this.response.addResponseHeader(key, value);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.rest.handler.RESTResponse#setStatus(int)
     */
    @Override
    public void setStatus(int statusCode) {
        this.response.setStatus(statusCode);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.rest.handler.RESTResponse#sendError(int)
     */
    @Override
    public void sendError(int statusCode) throws IOException {
        this.response.sendError(statusCode);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.rest.handler.RESTResponse#sendError(int, java.lang.String)
     */
    @Override
    public void sendError(int statusCode, String msg) throws IOException {
        this.response.sendError(statusCode, msg);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.rest.handler.RESTResponse#setContentType(java.lang.String)
     */
    @Override
    public void setContentType(String contentType) {
        this.response.setContentType(contentType);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.rest.handler.RESTResponse#setContentLength(int)
     */
    @Override
    public void setContentLength(int len) {
        this.response.setContentLength(len);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.rest.handler.RESTResponse#setCharacterEncoding(java.lang.String)
     */
    @Override
    public void setCharacterEncoding(String charset) {
        this.response.setCharacterEncoding(charset);
    }

}
