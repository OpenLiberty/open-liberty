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
package com.ibm.ws.sip.container.was;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.wsspi.webcontainer.servlet.IServletRequestWrapper;
import com.ibm.wsspi.webcontainer.servlet.IServletResponseWrapper;

public class DummySipServletRequestResponse implements 
						ServletResponse, ServletRequest, 
						IServletRequestWrapper, IServletResponseWrapper{

	/**
	 * Don't change this constant, it's used for distinguishing this request
	 * from real requests on SipFilter
	 */
	private static final String DUMMY_SIP_PROTOCOL = "DUMMY_SIP";
	
	private static final Enumeration emptyEnumeration = Collections.enumeration(Collections.emptyList());
	
	
    /**
     * Http Servlet Request associated with this message. Used for accessing
     * WAS security/authentication API. 
     * Can be transient - nobody will use it after failover
     */
    private transient HttpServletRequest m_httpServletRequest = null;
    
    /**
     * Http Servlet Response associated with this message. Used for forwarding 
     * message between servlets.
     * Can be transient - nobody will use it after failover
     */
    private transient HttpServletResponse m_httpServletResponse = null;

	
	public DummySipServletRequestResponse(HttpServletRequest request, HttpServletResponse response) {
		m_httpServletRequest = request;
		m_httpServletResponse = response;
	}
	
	public String getProtocol() {
		return DUMMY_SIP_PROTOCOL;
	}
	

	public void flushBuffer() throws IOException {
	}

	public int getBufferSize() {
		return 0;
	}

	public String getCharacterEncoding() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getContentType() {
		return null;
	}

	public Locale getLocale() {
		return Locale.getDefault();
	}

	public ServletOutputStream getOutputStream() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public PrintWriter getWriter() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isCommitted() {
		return false;
	}

	public void reset() {
	}

	public void resetBuffer() {
	}

	public void setBufferSize(int i) {
	}

	public void setCharacterEncoding(String s) {
	}

	public void setContentLength(int i) {
	}

	public void setContentType(String s) {
	}

	public void setLocale(Locale locale) {
	}

	public Object getAttribute(String s) {
		return null;
	}

	public Enumeration getAttributeNames() {
		return emptyEnumeration;
	}

	public int getContentLength() {
		return 0;
	}

	public ServletInputStream getInputStream() throws IOException {
		return null;
	}

	public String getLocalAddr() {
		return null;
	}

	public String getLocalName() {
		return null;
	}

	public int getLocalPort() {
		return 0;
	}

	public Enumeration getLocales() {
		return emptyEnumeration;
	}

	public String getParameter(String s) {
		return null;
	}

	public Map getParameterMap() {
		return Collections.emptyMap();
	}

	public Enumeration getParameterNames() {
		return emptyEnumeration;
	}

	public String[] getParameterValues(String s) {
		return new String[] {};
	}


	public BufferedReader getReader() throws IOException {
		return null;
	}

	public String getRealPath(String s) {
		return null;
	}

	public String getRemoteAddr() {
		return null;
	}

	public String getRemoteHost() {
		return null;
	}

	public int getRemotePort() {
		return 0;
	}

	public RequestDispatcher getRequestDispatcher(String s) {
		return m_httpServletRequest.getRequestDispatcher(s);
	}

	public String getScheme() {
		return null;
	}

	public String getServerName() {
		return null;
	}

	public int getServerPort() {
		return 0;
	}

	public boolean isSecure() {
		return false;
	}

	public void removeAttribute(String s) {
	}

	public void setAttribute(String s, Object obj) {
	}

	public ServletRequest getWrappedRequest() {
		return m_httpServletRequest;
	}

	public ServletResponse getWrappedResponse() {
		return m_httpServletResponse;
	}
	
	public boolean isAsyncSupported() {
		return false;
	}

	@Override
	public AsyncContext getAsyncContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DispatcherType getDispatcherType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServletContext getServletContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isAsyncStarted() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public AsyncContext startAsync() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1) {
		// TODO Auto-generated method stub
		return null;
	}

}
