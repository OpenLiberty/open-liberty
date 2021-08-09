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
package com.ibm.ws.webcontainer.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;
import java.util.Vector;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.servlet.response.IResponse;
import com.ibm.wsspi.webcontainer.servlet.IExtendedResponse;
import com.ibm.wsspi.webcontainer.servlet.IOutputMethodListener;
import com.ibm.wsspi.webcontainer.util.IResponseOutput;

/**
 * Proxies function invocations to an underlying HttpServletResponse.
 */
public abstract class HttpServletResponseProxy implements HttpServletResponse, IResponseOutput, IExtendedResponse
{
    /**
     * Get the response that this object is supposed to proxy.
     */
    abstract public HttpServletResponse getProxiedHttpServletResponse();
    public HttpServletResponse getWASProxiedHttpServletResponse()
    {
        return  getProxiedHttpServletResponse();
    }

    public IResponse getIResponse(){
    	return ((IExtendedResponse)getWASProxiedHttpServletResponse()).getIResponse();
    }
    
    // --- IExtendedResponse interface --- //
    public Vector[] getHeaderTable()
    {
        return((IExtendedResponse)getWASProxiedHttpServletResponse()).getHeaderTable();
    }
    public void addSessionCookie(Cookie cookie)
    {
        ((IExtendedResponse)getWASProxiedHttpServletResponse()).addSessionCookie(cookie);
    }

    //------------- HttpServletResponse ------------------------//
    public void addCookie(Cookie cookie)
    {
        getProxiedHttpServletResponse().addCookie(cookie);
    }
    public boolean containsHeader(String name)
    {
        return getProxiedHttpServletResponse().containsHeader(name);
    }
    public String encodeRedirectUrl(String url)
    {
        return getProxiedHttpServletResponse().encodeRedirectUrl(url);
    }
    public String encodeURL(String url)
    {
        return getProxiedHttpServletResponse().encodeURL(url);
    }
    public String encodeRedirectURL(String url)
    {
        return getProxiedHttpServletResponse().encodeRedirectURL(url);
    }
    public String encodeUrl(String url)
    {
        return getProxiedHttpServletResponse().encodeUrl(url);
    }
    public void setDateHeader(String name, long date)
    {
        getProxiedHttpServletResponse().setDateHeader(name, date);
    }
    public void sendError(int sc) throws IOException{
        getProxiedHttpServletResponse().sendError(sc);
    }
    public void sendError(int sc, String msg) throws IOException{
        getProxiedHttpServletResponse().sendError(sc, msg);
    }
   // PQ97429
    public void sendRedirect303(String location) throws IOException{
	HttpServletResponse resp = getProxiedHttpServletResponse();
        if(resp instanceof IExtendedResponse) {
		((IExtendedResponse)resp).sendRedirect303(location);
	} else {
		sendRedirect(location);
	}
    }
    // PQ97429
    public void sendRedirect(String location) throws IOException{
        getProxiedHttpServletResponse().sendRedirect(location);
    }
    public void setStatus(int sc)
    {
        getProxiedHttpServletResponse().setStatus(sc);
    }
    public void setStatus(int sc, String sm)
    {
        getProxiedHttpServletResponse().setStatus(sc,sm);
    }
    public void setHeader(String name, String value)
    {
        getProxiedHttpServletResponse().setHeader(name, value);
    }
    public void setIntHeader(String name, int value)
    {
        getProxiedHttpServletResponse().setIntHeader(name, value);
    }
    public void addDateHeader(String name, long date)
    {
        getProxiedHttpServletResponse().addDateHeader(name,date);
    }
    public void addHeader(String name, String value)
    {
        getProxiedHttpServletResponse().addHeader(name,value);
    }
    public void addIntHeader(String name, int value)
    {
        getProxiedHttpServletResponse().addIntHeader(name,value);
    }

    //------------- ServletResponse ------------------------//
    public String getCharacterEncoding()
    {
        return getProxiedHttpServletResponse().getCharacterEncoding();
    }
    public ServletOutputStream getOutputStream() throws IOException{
        return getProxiedHttpServletResponse().getOutputStream();
    }
    public PrintWriter getWriter() throws IOException{
        return getProxiedHttpServletResponse().getWriter();
    }
    public void setContentLength(int len)
    {
        getProxiedHttpServletResponse().setContentLength(len);
    }
    public void setContentType(String type)
    {
        getProxiedHttpServletResponse().setContentType(type);
    }
    public void setBufferSize(int size)
    {
        getProxiedHttpServletResponse().setBufferSize(size);
    }
    public int getBufferSize()
    {
        return getProxiedHttpServletResponse().getBufferSize();
    }
    public void flushBuffer() throws IOException{
    	flushBuffer(true);
    }
    public void flushBuffer(boolean flushToWire) throws IOException{
        getProxiedHttpServletResponse().flushBuffer();
    }
    public boolean isCommitted()
    {
        return getProxiedHttpServletResponse().isCommitted();
    }
    public void reset()
    {
        if(getProxiedHttpServletResponse() != null)
        {
            getProxiedHttpServletResponse().reset();
        }
    }
    public void setLocale(Locale loc)
    {
        getProxiedHttpServletResponse().setLocale(loc);
    }
    public Locale getLocale()
    {
        return getProxiedHttpServletResponse().getLocale();
    }
    public boolean writerObtained()
    {
        return((IResponseOutput)getWASProxiedHttpServletResponse()).writerObtained();
    }
    public boolean outputStreamObtained()
    {
        return((IResponseOutput)getWASProxiedHttpServletResponse()).outputStreamObtained();
    }

    // LIDB1234.3 - added method below
    /**
     * Clears the content of the underlying buffer in the response without clearing
     * headers or status code.
     *
     * @throws IllegalStateException if the response has already been committed
     *
     * @since 2.3
     */
    public void resetBuffer()
    {
        getProxiedHttpServletResponse().resetBuffer();
    }
    
	public void fireOutputStreamRetrievedEvent(ServletOutputStream sos) {
		((IExtendedResponse)getWASProxiedHttpServletResponse()).fireOutputStreamRetrievedEvent(sos);
	}

	public void fireWriterRetrievedEvent(PrintWriter pw) {
		((IExtendedResponse)getWASProxiedHttpServletResponse()).fireWriterRetrievedEvent(pw);
	}

	public void registerOutputMethodListener(IOutputMethodListener listener) {
		((IExtendedResponse)getWASProxiedHttpServletResponse()).registerOutputMethodListener(listener);
	}
	
    @Override
    public String getHeader(String arg0) {
        return getProxiedHttpServletResponse().getHeader(arg0);
    }

    @Override
    public Collection<String> getHeaderNames() {
        return getProxiedHttpServletResponse().getHeaderNames();
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return getProxiedHttpServletResponse().getHeaders(name);
    }

    @Override
    public int getStatus() {
        return getProxiedHttpServletResponse().getStatus();
    }
    
	@Override
	public boolean isOutputWritten() {
		return ((IExtendedResponse)getProxiedHttpServletResponse()).isOutputWritten();
	}


}
