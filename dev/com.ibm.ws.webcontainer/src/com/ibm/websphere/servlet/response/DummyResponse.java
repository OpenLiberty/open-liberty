/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.servlet.response;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.webcontainer.core.Response;

public class DummyResponse implements HttpServletResponse, Response
{

    /**
     * @see HttpServletResponse#addCookie(Cookie)
     */
    public void addCookie(Cookie arg0)
    {
    }

    /**
     * @see HttpServletResponse#containsHeader(String)
     */
    public boolean containsHeader(String arg0)
    {
        return false;
    }
    
    public String getContentType()
    {
    	return null;
    }

    /**
     * @see HttpServletResponse#encodeURL(String)
     */
    public String encodeURL(String arg0)
    {
        return null;
    }

    /**
     * @see HttpServletResponse#encodeRedirectURL(String)
     */
    public String encodeRedirectURL(String arg0)
    {
        return null;
    }

    /**
     * @see HttpServletResponse#encodeUrl(String)
     * @deprecated
     */
    public String encodeUrl(String arg0)
    {
        return null;
    }

    /**
     * @see HttpServletResponse#encodeRedirectUrl(String)
     * @deprecated
     */
    public String encodeRedirectUrl(String arg0)
    {
        return null;
    }

    /**
     * @see HttpServletResponse#sendError(int, String)
     */
    public void sendError(int arg0, String arg1) throws IOException {
    }

    /**
     * @see HttpServletResponse#sendError(int)
     */
    public void sendError(int arg0) throws IOException {
    }

    /**
     * @see HttpServletResponse#sendRedirect(String)
     */
    public void sendRedirect(String arg0) throws IOException {
    }

    /**
     * @see HttpServletResponse#setDateHeader(String, long)
     */
    public void setDateHeader(String arg0, long arg1)
    {
    }

    /**
     * @see HttpServletResponse#addDateHeader(String, long)
     */
    public void addDateHeader(String arg0, long arg1)
    {
    }

    /**
     * @see HttpServletResponse#setHeader(String, String)
     */
    public void setHeader(String arg0, String arg1)
    {
    }

    /**
     * @see HttpServletResponse#addHeader(String, String)
     */
    public void addHeader(String arg0, String arg1)
    {
    }

    /**
     * @see HttpServletResponse#setIntHeader(String, int)
     */
    public void setIntHeader(String arg0, int arg1)
    {
    }

    /**
     * @see HttpServletResponse#addIntHeader(String, int)
     */
    public void addIntHeader(String arg0, int arg1)
    {
    }

    /**
     * @see HttpServletResponse#setStatus(int)
     */
    public void setStatus(int arg0)
    {
    }

    /**
     * @see HttpServletResponse#setStatus(int, String)
     * @deprecated
     */
    public void setStatus(int arg0, String arg1)
    {
    }

    /**
     * @see ServletResponse#getCharacterEncoding()
     */
    public String getCharacterEncoding()
    {
        return null;
    }
    
    public void setCharacterEncoding(String encoding)
    {
    }
    
    /**
     * @see ServletResponse#getOutputStream()
     */
    public ServletOutputStream getOutputStream() throws IOException {
        return null;
    }

    /**
     * @see ServletResponse#getWriter()
     */
    public PrintWriter getWriter() throws IOException {
        return null;
    }

    /**
     * @see ServletResponse#setContentLength(int)
     */
    public void setContentLength(int arg0)
    {
    }

    /**
     * @see ServletResponse#setContentType(String)
     */
    public void setContentType(String arg0)
    {
    }

    /**
     * @see ServletResponse#setBufferSize(int)
     */
    public void setBufferSize(int arg0)
    {
    }

    /**
     * @see ServletResponse#getBufferSize()
     */
    public int getBufferSize()
    {
        return 0;
    }

    /**
     * @see ServletResponse#flushBuffer()
     */
    public void flushBuffer() throws IOException {
    }

    /**
     * @see ServletResponse#resetBuffer()
     */
    public void resetBuffer()
    {
    }

    /**
     * @see ServletResponse#isCommitted()
     */
    public boolean isCommitted()
    {
        return false;
    }

    /**
     * @see ServletResponse#reset()
     */
    public void reset()
    {
    }

    /**
     * @see ServletResponse#setLocale(Locale)
     */
    public void setLocale(Locale arg0)
    {
    }

    /**
     * @see ServletResponse#getLocale()
     */
    public Locale getLocale()
    {
        return null;
    }

    public void finish() throws ServletException {

    }
    /* PK22448
    public int getStatusCode() {
        return 0;
    }
    */
    

    
    public void start() {
    }
    
    public void destroy(){
    }

	public void initForNextResponse(com.ibm.websphere.servlet.response.IResponse res) {

	}

    @Override
    public String getHeader(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<String> getHeaderNames() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<String> getHeaders(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getStatus() {
        // TODO Auto-generated method stub
        return 0;
    }
}

