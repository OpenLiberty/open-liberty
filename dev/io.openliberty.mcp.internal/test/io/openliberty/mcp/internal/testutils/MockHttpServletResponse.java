/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.testutils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Locale;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

/**
 *
 */
public class MockHttpServletResponse implements HttpServletResponse {

    private StringWriter writer = new StringWriter();

    @Override
    public PrintWriter getWriter() throws IOException {
        return new PrintWriter(writer);
    }

    @Override
    public void setCharacterEncoding(String arg0) {
        // Do nothing
    }

    @Override
    public void flushBuffer() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getBufferSize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCharacterEncoding() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getContentType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Locale getLocale() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCommitted() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void resetBuffer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBufferSize(int arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setContentLength(int arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setContentLengthLong(long arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setContentType(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLocale(Locale arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addCookie(Cookie arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addDateHeader(String arg0, long arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addHeader(String arg0, String arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addIntHeader(String arg0, int arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsHeader(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String encodeRedirectURL(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String encodeURL(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getHeader(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> getHeaderNames() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> getHeaders(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getStatus() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendError(int arg0, String arg1) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendError(int arg0) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendRedirect(String arg0) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDateHeader(String arg0, long arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setHeader(String arg0, String arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setIntHeader(String arg0, int arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setStatus(int arg0) {
        throw new UnsupportedOperationException();
    }

}
