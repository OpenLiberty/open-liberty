/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webcontainer.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import com.ibm.websphere.servlet.request.IRequest;
import com.ibm.wsspi.webcontainer.webapp.IWebAppDispatcherContext;

public class DummyRequest implements HttpServletRequest, IExtendedRequest {

    @Override
    public boolean authenticate(HttpServletResponse arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getAuthType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getContextPath() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Cookie[] getCookies() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getDateHeader(String arg0) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getHeader(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Enumeration<String> getHeaders(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getIntHeader(String arg0) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getMethod() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Part getPart(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<Part> getParts() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getPathInfo() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getPathTranslated() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getQueryString() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getRemoteUser() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getRequestURI() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public StringBuffer getRequestURL() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getRequestedSessionId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getServletPath() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpSession getSession() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpSession getSession(boolean arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Principal getUserPrincipal() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isUserInRole(String arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void login(String arg0, String arg1) throws ServletException {
        // TODO Auto-generated method stub

    }

    @Override
    public void logout() throws ServletException {
        // TODO Auto-generated method stub

    }


    @Override
    public AsyncContext getAsyncContext() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getAttribute(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getCharacterEncoding() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getContentLength() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getContentType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DispatcherType getDispatcherType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getLocalAddr() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getLocalName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getLocalPort() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Locale getLocale() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Enumeration<Locale> getLocales() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getParameter(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String[] getParameterValues(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getProtocol() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getRealPath(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getRemoteAddr() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getRemoteHost() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getRemotePort() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getScheme() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getServerName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getServerPort() {
        // TODO Auto-generated method stub
        return 0;
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
    public boolean isAsyncSupported() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isSecure() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void removeAttribute(String arg0) {
        // TODO Auto-generated method stub

    }
    @Override
    public void setAttribute(String arg0, Object arg1) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException {
        // TODO Auto-generated method stub

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

    @Override
    public void addParameter(String name, String[] values) {
        // TODO Auto-generated method stub

    }

    @Override
    public void aggregateQueryStringParams(String additionalQueryString, boolean setQS) {
        // TODO Auto-generated method stub

    }

    @Override
    public void attributeAdded(String key, Object newVal) {
        // TODO Auto-generated method stub

    }

    @Override
    public void attributeRemoved(String key, Object oldVal) {
        // TODO Auto-generated method stub

    }

    @Override
    public void attributeReplaced(String key, Object oldVal) {
        // TODO Auto-generated method stub

    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub

    }

    @Override
    public void finish() throws ServletException {
        // TODO Auto-generated method stub

    }

    @Override
    public List getAllCookieValues(String cookieName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public byte[] getCookieValueAsBytes(String cookieName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getEncodedRequestURI() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IRequest getIRequest() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getReaderEncoding() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IExtendedResponse getResponse() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean getRunningCollaborators() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public byte[] getSSLId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getSessionAffinityContext() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getUpdatedSessionId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IWebAppDispatcherContext getWebAppDispatcherContext() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void initForNextRequest(IRequest req) {
        // TODO Auto-generated method stub

    }

    @Override
    public void pushParameterStack() {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeHeader(String header) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeQSFromList() {
        // TODO Auto-generated method stub

    }

    @Override
    public void setMethod(String method) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setQueryString(String qs) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setResponse(IExtendedResponse extResp) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setRunningCollaborators(boolean runningCollaborators) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setSessionAffinityContext(Object sac) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setSessionId(String id) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setWebAppDispatcherContext(IWebAppDispatcherContext ctx) {
        // TODO Auto-generated method stub

    }

    @Override
    public void start() {
        // TODO Auto-generated method stub

    }

    @Override
    public void closeResponseOutput() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void finishAndDestroyConnectionContext() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setAsyncSupported(boolean asyncSupported) {
        // TODO Auto-generated method stub
        
    }

	@Override
	public void setDispatcherType(DispatcherType dispatcherType) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setAsyncStarted(boolean b) {
		// TODO Auto-generated method stub
		
	}

    /* (non-Javadoc)
     * @see com.ibm.wsspi.webcontainer.servlet.IExtendedRequest#getInputStreamData()
     */
    @Override
    public HashMap getInputStreamData() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.ibm.wsspi.webcontainer.servlet.IExtendedRequest#setInputStreamData(java.util.HashMap)
     */
    @Override
    public void setInputStreamData(HashMap inStreamInfo) throws IOException {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see com.ibm.wsspi.webcontainer.servlet.IExtendedRequest#serializeInputStreamData(java.util.Map)
     */
    @Override
    public byte[][] serializeInputStreamData(Map isd) throws IOException, UnsupportedEncodingException, IllegalStateException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.ibm.wsspi.webcontainer.servlet.IExtendedRequest#deserializeInputStreamData(byte[][])
     */
    @Override
    public HashMap deserializeInputStreamData(byte[][] input) throws UnsupportedEncodingException, IllegalStateException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.ibm.wsspi.webcontainer.servlet.IExtendedRequest#sizeInputStreamData(java.util.Map)
     */
    @Override
    public long sizeInputStreamData(Map isd) throws UnsupportedEncodingException, IllegalStateException {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.ibm.wsspi.webcontainer.servlet.IExtendedRequest#setValuesIfMultiReadofPostdataEnabled()
     */
    @Override
    public void setValuesIfMultiReadofPostdataEnabled() {
        // TODO Auto-generated method stub
        
    }


}
