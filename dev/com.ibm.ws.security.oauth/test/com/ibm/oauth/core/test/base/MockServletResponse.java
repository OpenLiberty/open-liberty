/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.test.base;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/*
 * Mock servlet response for use in OAuth20Component.processAuthorization()
 */
public class MockServletResponse implements HttpServletResponse {

    @Override
    public void addCookie(Cookie arg0) {}

    @Override
    public void addDateHeader(String arg0, long arg1) {}

    @Override
    public void addHeader(String arg0, String arg1) {}

    @Override
    public void addIntHeader(String arg0, int arg1) {}

    @Override
    public boolean containsHeader(String arg0) {
        return false;
    }

    @Override
    public String encodeRedirectURL(String arg0) {
        return null;
    }

    @Override
    public String encodeRedirectUrl(String arg0) {
        return null;
    }

    @Override
    public String encodeURL(String arg0) {
        return null;
    }

    @Override
    public String encodeUrl(String arg0) {
        return null;
    }

    @Override
    public void sendError(int arg0) throws IOException {}

    @Override
    public void sendError(int arg0, String arg1) throws IOException {}

    @Override
    public void sendRedirect(String arg0) throws IOException {}

    @Override
    public void setDateHeader(String arg0, long arg1) {}

    @Override
    public void setHeader(String arg0, String arg1) {}

    @Override
    public void setIntHeader(String arg0, int arg1) {}

    @Override
    public void setStatus(int arg0) {}

    @Override
    public void setStatus(int arg0, String arg1) {}

    @Override
    public void flushBuffer() throws IOException {}

    @Override
    public int getBufferSize() {
        return 0;
    }

    @Override
    public String getCharacterEncoding() {
        return null;
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public Locale getLocale() {
        return null;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return null;
    }

    PrintWriter writer = null;

    @Override
    public PrintWriter getWriter() throws IOException {
        return writer;
    }

    public void setWriter(Writer in) {
        writer = new PrintWriter(in);
    }

    @Override
    public boolean isCommitted() {
        return false;
    }

    @Override
    public void reset() {}

    @Override
    public void resetBuffer() {}

    @Override
    public void setBufferSize(int arg0) {}

    @Override
    public void setCharacterEncoding(String arg0) {}

    @Override
    public void setContentLength(int arg0) {}

    @Override
    public void setContentType(String arg0) {}

    @Override
    public void setLocale(Locale arg0) {}

    @Override
    public String getHeader(String arg0) {
        return null;
    }

    @Override
    public Collection<String> getHeaderNames() {
        return null;
    }

    @Override
    public Collection<String> getHeaders(String arg0) {
        return null;
    }

    @Override
    public int getStatus() {
        return 0;
    }

}
