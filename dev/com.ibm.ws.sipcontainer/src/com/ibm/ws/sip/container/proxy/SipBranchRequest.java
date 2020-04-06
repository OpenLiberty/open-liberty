/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.proxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.sip.Address;
import javax.servlet.sip.AuthInfo;
import javax.servlet.sip.B2buaHelper;
import javax.servlet.sip.Parameterable;
import javax.servlet.sip.Proxy;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.TooManyHopsException;
import javax.servlet.sip.URI;
import javax.servlet.sip.ar.SipApplicationRoutingDirective;
import javax.servlet.sip.ar.SipApplicationRoutingRegion;

import com.ibm.ws.sip.container.servlets.SipServletRequestImpl;

/**
 * The class represent a request associated with a proxy branch.
 * This is only a decorator for the request and is meant to keep JSR289 chapter 10.2.1
 * A request associated with a proxy branch should fail on performing the operations that were not 
 * Specifically allowed by the JSR.
 * For example send and createCancel should throw IlligelStateException and should not be performed.
 * The original request is used to enable the application to perform the operations that were defined on the request
 * that will be sent and not a clone.
 * @author SAGIA
 *
 */
public class SipBranchRequest implements SipServletRequest {
	
	/**
	 * The branch associated request that is being warped here.
	 */
	private SipServletRequestImpl _request;
	
	/**
	 * Receive the branch associated request to use.
	 * @param req
	 */
	public SipBranchRequest(SipServletRequestImpl req){
		_request = req;
	}

	/**
	 * 
	 * Not allowed on a branch associated request 
	 * Throws IlligalStateException
	 * @see javax.servlet.sip.SipServletRequest#addAuthHeader(javax.servlet.sip.SipServletResponse, javax.servlet.sip.AuthInfo)
	 */
	public void addAuthHeader(SipServletResponse challengeResponse,
			AuthInfo authInfo) {
		throw new IllegalStateException("Cannot add auth header on branch request");
	}

	/**
	 * Not allowed on a branch associated request 
	 * Throws IlligalStateException
	 * @see javax.servlet.sip.SipServletRequest#addAuthHeader(javax.servlet.sip.SipServletResponse, java.lang.String, java.lang.String)
	 */
	public void addAuthHeader(SipServletResponse challengeResponse,
			String username, String password) {
		throw new IllegalStateException("Cannot add auth header on branch  request");
	}

	/**
	 * Not allowed on a branch associated request 
	 * Throws IlligalStateException
	 * @see javax.servlet.sip.SipServletRequest#createCancel()
	 */
	public SipServletRequest createCancel() throws IllegalStateException {
		throw new IllegalStateException("Cannot create Cancel on branch  request");
	}

	/**
	 * Not allowed on a branch associated request 
	 * Throws IlligalStateException
	 * @see javax.servlet.sip.SipServletRequest#createResponse(int)
	 */
	public SipServletResponse createResponse(int statuscode)
			throws IllegalArgumentException, IllegalStateException {
		throw new IllegalStateException("Cannot create response for branch request");
	}

	/**
	 * @param locale
	 * @see javax.servlet.sip.SipServletMessage#addAcceptLanguage(java.util.Locale)
	 */
	public void addAcceptLanguage(Locale locale) {
		_request.addAcceptLanguage(locale);
	}

	/**
	 * Not allowed on a branch associated request 
	 * Throws IlligalStateException
	 * @see javax.servlet.sip.SipServletRequest#createResponse(int, java.lang.String)
	 */
	public SipServletResponse createResponse(int statusCode, String reasonPhrase)
			throws IllegalArgumentException, IllegalStateException {
		throw new IllegalStateException("Cannot create response on branch request");
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#addAddressHeader(java.lang.String, javax.servlet.sip.Address, boolean)
	 */
	public void addAddressHeader(String name, Address addr, boolean first)
			throws IllegalArgumentException {
		_request.addAddressHeader(name, addr, first);
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#addHeader(java.lang.String, java.lang.String)
	 */
	public void addHeader(String name, String value)
			throws IllegalArgumentException {
		_request.addHeader(name, value);
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#addParameterableHeader(java.lang.String, javax.servlet.sip.Parameterable, boolean)
	 */
	public void addParameterableHeader(String name, Parameterable param,
			boolean first) throws IllegalArgumentException {
		_request.addParameterableHeader(name, param, first);
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#getAcceptLanguage()
	 */
	public Locale getAcceptLanguage() {
		return _request.getAcceptLanguage();
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#getAcceptLanguages()
	 */
	public Iterator getAcceptLanguages() {
		return _request.getAcceptLanguages();
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#getAddressHeader(java.lang.String)
	 */
	public Address getAddressHeader(String name) throws ServletParseException {
		return _request.getAddressHeader(name);
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#getAddressHeaders(java.lang.String)
	 */
	public ListIterator getAddressHeaders(String name)
			throws ServletParseException {
		return _request.getAddressHeaders(name);
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#getApplicationSession()
	 */
	public SipApplicationSession getApplicationSession() {
		return _request.getApplicationSession();
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#getApplicationSession(boolean)
	 */
	public SipApplicationSession getApplicationSession(boolean create) {
		return _request.getApplicationSession(create);
	}

	/**
	 * @see javax.servlet.ServletRequest#getAttribute(java.lang.String)
	 */
	public Object getAttribute(String arg0) {
		return _request.getAttribute(arg0);
	}

	/**
	 * @see javax.servlet.ServletRequest#getAttributeNames()
	 */
	public Enumeration getAttributeNames() {
		return _request.getAttributeNames();
	}

	/**
	 * @see javax.servlet.sip.SipServletRequest#getB2buaHelper()
	 */
	public B2buaHelper getB2buaHelper() throws IllegalStateException {
		return _request.getB2buaHelper();
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#getCallId()
	 */
	public String getCallId() {
		return _request.getCallId();
	}

	/**
	 * @see javax.servlet.ServletRequest#getCharacterEncoding()
	 */
	public String getCharacterEncoding() {
		return _request.getCharacterEncoding();
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#getContent()
	 */
	public Object getContent() throws IOException, UnsupportedEncodingException {
		return _request.getContent();
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#getContentLanguage()
	 */
	public Locale getContentLanguage() {
		return _request.getContentLanguage();
	}

	/**
	 * @see javax.servlet.ServletRequest#getContentLength()
	 */
	public int getContentLength() {
		return _request.getContentLength();
	}

	/**
	 * @see javax.servlet.ServletRequest#getContentType()
	 */
	public String getContentType() {
		return _request.getContentType();
	}

	/**
	 * @see javax.servlet.sip.SipServletRequest#getInitialPoppedRoute()
	 */
	public Address getInitialPoppedRoute() {
		return _request.getInitialPoppedRoute();
	}

	/**
	 * @see javax.servlet.sip.SipServletRequest#getInputStream()
	 */
	public ServletInputStream getInputStream() throws IOException {
		return _request.getInputStream();
	}

	/**
	 * @see javax.servlet.sip.SipServletRequest#getPoppedRoute()
	 */
	public Address getPoppedRoute() {
		return _request.getPoppedRoute();
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#getExpires()
	 */
	public int getExpires() {
		return _request.getExpires();
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#getFrom()
	 */
	public Address getFrom() {
		return _request.getFrom();
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#getHeader(java.lang.String)
	 */
	public String getHeader(String name) throws NullPointerException {
		return _request.getHeader(name);
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#getHeaderForm()
	 */
	public HeaderForm getHeaderForm() {
		return _request.getHeaderForm();
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#getHeaderNames()
	 */
	public Iterator getHeaderNames() {
		return _request.getHeaderNames();
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#getHeaders(java.lang.String)
	 */
	public ListIterator getHeaders(String name) throws NullPointerException {
		return _request.getHeaders(name);
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#getInitialRemoteAddr()
	 */
	public String getInitialRemoteAddr() {
		return _request.getInitialRemoteAddr();
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#getInitialRemotePort()
	 */
	public int getInitialRemotePort() {
		return _request.getInitialRemotePort();
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#getInitialTransport()
	 */
	public String getInitialTransport() {
		return _request.getInitialTransport();
	}

	/**
	 * @see javax.servlet.ServletRequest#getLocalAddr()
	 */
	public String getLocalAddr() {
		return _request.getLocalAddr();
	}

	/**
	 * @see javax.servlet.ServletRequest#getLocalName()
	 */
	public String getLocalName() {
		return _request.getLocalName();
	}

	/**
	 * @see javax.servlet.ServletRequest#getLocalPort()
	 */
	public int getLocalPort() {
		return _request.getLocalPort();
	}

	/**
	 * @see javax.servlet.ServletRequest#getLocale()
	 */
	public Locale getLocale() {
		return _request.getLocale();
	}

	/**
	 * @see javax.servlet.ServletRequest#getLocales()
	 */
	public Enumeration getLocales() {
		return _request.getLocales();
	}

	/**
	 * @see javax.servlet.sip.SipServletRequest#getMaxForwards()
	 */
	public int getMaxForwards() {
		return _request.getMaxForwards();
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#getMethod()
	 */
	public String getMethod() {
		return _request.getMethod();
	}

	/**
	 * @see javax.servlet.ServletRequest#getParameter(java.lang.String)
	 */
	public String getParameter(String arg0) {
		return _request.getParameter(arg0);
	}

	/**
	 * @see javax.servlet.ServletRequest#getParameterMap()
	 */
	public Map getParameterMap() {
		return _request.getParameterMap();
	}

	/**
	 * @see javax.servlet.ServletRequest#getParameterNames()
	 */
	public Enumeration getParameterNames() {
		return _request.getParameterNames();
	}

	/**
	 * @see javax.servlet.ServletRequest#getParameterValues(java.lang.String)
	 */
	public String[] getParameterValues(String arg0) {
		return _request.getParameterValues(arg0);
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#getParameterableHeader(java.lang.String)
	 */
	public Parameterable getParameterableHeader(String name)
			throws ServletParseException {
		return _request.getParameterableHeader(name);
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#getParameterableHeaders(java.lang.String)
	 */
	public ListIterator<? extends Parameterable> getParameterableHeaders(
			String name) throws ServletParseException {
		return _request.getParameterableHeaders(name);
	}

	/**
	 * @see javax.servlet.ServletRequest#getProtocol()
	 */
	public String getProtocol() {
		return _request.getProtocol();
	}

	/**
	 * @see javax.servlet.sip.SipServletRequest#getProxy()
	 */
	public Proxy getProxy() throws TooManyHopsException {
		return _request.getProxy();
	}

	/**
	 * @see javax.servlet.sip.SipServletRequest#getProxy(boolean)
	 */
	public Proxy getProxy(boolean create) throws TooManyHopsException {
		return _request.getProxy(create);
	}

	/**
	 * @see javax.servlet.sip.SipServletRequest#getReader()
	 */
	public BufferedReader getReader() throws IOException {
		return _request.getReader();
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#getRawContent()
	 */
	public byte[] getRawContent() throws IOException {
		return _request.getRawContent();
	}

	/**
	 * @see javax.servlet.ServletRequest#getRealPath(java.lang.String)
	 */
	public String getRealPath(String arg0) {
		return _request.getRealPath(arg0);
	}

	/**
	 * @see javax.servlet.sip.SipServletRequest#getRegion()
	 */
	public SipApplicationRoutingRegion getRegion() throws IllegalStateException {
		return _request.getRegion();
	}

	/**
	 * @see javax.servlet.ServletRequest#getRemoteAddr()
	 */
	public String getRemoteAddr() {
		return _request.getRemoteAddr();
	}

	/**
	 * @see javax.servlet.ServletRequest#getRemoteHost()
	 */
	public String getRemoteHost() {
		return _request.getRemoteHost();
	}

	/**
	 * @see javax.servlet.ServletRequest#getRemotePort()
	 */
	public int getRemotePort() {
		return _request.getRemotePort();
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#getRemoteUser()
	 */
	public String getRemoteUser() {
		return _request.getRemoteUser();
	}

	/**
	 * @see javax.servlet.ServletRequest#getRequestDispatcher(java.lang.String)
	 */
	public RequestDispatcher getRequestDispatcher(String arg0) {
		return _request.getRequestDispatcher(arg0);
	}

	/**
	 * @see javax.servlet.sip.SipServletRequest#getRequestURI()
	 */
	public URI getRequestURI() {
		return _request.getRequestURI();
	}

	/**
	 * @see javax.servlet.sip.SipServletRequest#getRoutingDirective()
	 */
	public SipApplicationRoutingDirective getRoutingDirective()
			throws IllegalStateException {
		return _request.getRoutingDirective();
	}

	/**
	 * @see javax.servlet.ServletRequest#getScheme()
	 */
	public String getScheme() {
		return _request.getScheme();
	}

	/**
	 * @see javax.servlet.ServletRequest#getServerName()
	 */
	public String getServerName() {
		return _request.getServerName();
	}

	/**
	 * @see javax.servlet.ServletRequest#getServerPort()
	 */
	public int getServerPort() {
		return _request.getServerPort();
	}

	/**
	 * @see javax.servlet.sip.SipServletRequest#getSubscriberURI()
	 */
	public URI getSubscriberURI() throws IllegalStateException {
		return _request.getSubscriberURI();
	}

	/**
	 * @see javax.servlet.sip.SipServletRequest#isInitial()
	 */
	public boolean isInitial() {
		return _request.isInitial();
	}

	/**
	 * @see javax.servlet.sip.SipServletRequest#pushRoute(javax.servlet.sip.SipURI)
	 */
	public void pushRoute(SipURI uri) {
		_request.pushRoute(uri);
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#getSession()
	 */
	public SipSession getSession() {
		return _request.getSession();
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#getSession(boolean)
	 */
	public SipSession getSession(boolean create) {
		return _request.getSession(create);
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#getTo()
	 */
	public Address getTo() {
		return _request.getTo();
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#getTransport()
	 */
	public String getTransport() {
		return _request.getTransport();
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#getUserPrincipal()
	 */
	public Principal getUserPrincipal() {
		return _request.getUserPrincipal();
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#isCommitted()
	 */
	public boolean isCommitted() {
		return _request.isCommitted();
	}

	/**
	 * @see javax.servlet.ServletRequest#isSecure()
	 */
	public boolean isSecure() {
		return _request.isSecure();
	}

	/**
	 * @see javax.servlet.sip.SipServletRequest#pushPath(javax.servlet.sip.Address)
	 */
	public void pushPath(Address uri) throws IllegalStateException {
		_request.pushPath(uri);
	}

	/**
	 * @see javax.servlet.sip.SipServletRequest#pushRoute(javax.servlet.sip.Address)
	 */
	public void pushRoute(Address uri) {
		_request.pushRoute(uri);
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#isUserInRole(java.lang.String)
	 */
	public boolean isUserInRole(String role) {
		return _request.isUserInRole(role);
	}

	/**
	 * @see javax.servlet.ServletRequest#removeAttribute(java.lang.String)
	 */
	public void removeAttribute(String arg0) {
		 _request.removeAttribute(arg0);
	}

	/**
	 * Not allowed on a branch associated request 
	 * Throws IlligalStateException
	 * @see javax.servlet.sip.SipServletRequest#send()
	 */
	public void send() throws IOException {
		throw new IllegalStateException("Cannot send branch request");
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#removeHeader(java.lang.String)
	 */
	public void removeHeader(String name) throws IllegalArgumentException {
		_request.removeHeader(name);
	}

	/**
	 * Not allowed on a branch associated request 
	 * Throws IlligalStateException
	 * @see javax.servlet.sip.SipServletMessage#setAcceptLanguage(java.util.Locale)
	 */
	public void setAcceptLanguage(Locale locale) {
		throw new IllegalStateException("Cannot set request URI on branch request");
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#setAddressHeader(java.lang.String, javax.servlet.sip.Address)
	 */
	public void setAddressHeader(String name, Address addr)
			throws IllegalArgumentException {
		_request.setAddressHeader(name, addr);
	}

	/**
	 * @see javax.servlet.ServletRequest#setAttribute(java.lang.String, java.lang.Object)
	 */
	public void setAttribute(String arg0, Object arg1) {
		_request.setAttribute(arg0, arg1);
	}

	/**
	 * Not allowed on a branch associated request 
	 * Throws IlligalStateException
	 * @see javax.servlet.ServletRequest#setCharacterEncoding(java.lang.String)
	 */
	public void setCharacterEncoding(String arg0)
			throws UnsupportedEncodingException {
		throw new IllegalStateException("Cannot set request URI on branch request");
	}

	/**
	 * @see javax.servlet.sip.SipServletRequest#setMaxForwards(int)
	 */
	public void setMaxForwards(int n) throws IllegalArgumentException {
		throw new IllegalStateException("Cannot set max forwarders on branch request");
	}

	/**
	 * Not allowed on a branch associated request 
	 * Throws IlligalStateException
	 * @see javax.servlet.sip.SipServletRequest#setRequestURI(javax.servlet.sip.URI)
	 */
	public void setRequestURI(URI uri) throws NullPointerException {
		throw new IllegalStateException("Cannot set request URI on branch request");
	}

	/**
	 * Not allowed on a branch associated request 
	 * Throws IlligalStateException
	 * @see javax.servlet.sip.SipServletRequest#setRoutingDirective(javax.servlet.sip.ar.SipApplicationRoutingDirective, javax.servlet.sip.SipServletRequest)
	 */
	public void setRoutingDirective(SipApplicationRoutingDirective directive,
			SipServletRequest origRequest) throws IllegalStateException {
		throw new IllegalStateException("Cannot set routing directive on branch request");
	}

	/**
	 * Not allowed on a branch associated request 
	 * Throws IlligalStateException
	 * @see javax.servlet.sip.SipServletMessage#setContent(java.lang.Object, java.lang.String)
	 */
	public void setContent(Object content, String contentType)
			throws IllegalArgumentException, IllegalStateException,
			UnsupportedEncodingException {
		throw new IllegalStateException("Cannot set routing directive on branch request");
	}

	/**
	 * Not allowed on a branch associated request 
	 * Throws IlligalStateException
	 * @see javax.servlet.sip.SipServletMessage#setContentLanguage(java.util.Locale)
	 */
	public void setContentLanguage(Locale locale) throws IllegalStateException {
		throw new IllegalStateException("Cannot set routing directive on branch request");
	}

	/**
	 * Not allowed on a branch associated request 
	 * Throws IlligalStateException
	 * @see javax.servlet.sip.SipServletMessage#setContentLength(int)
	 */
	public void setContentLength(int len) {
		throw new IllegalStateException("Cannot set routing directive on branch request");
	}

	/**
	 * Not allowed on a branch associated request 
	 * Throws IlligalStateException
	 * @see javax.servlet.sip.SipServletMessage#setContentType(java.lang.String)
	 */
	public void setContentType(String type) {
		throw new IllegalStateException("Cannot set routing directive on branch request");
	}

	/**
	 * Not allowed on a branch associated request 
	 * Throws IlligalStateException
	 * @see javax.servlet.sip.SipServletMessage#setExpires(int)
	 */
	public void setExpires(int seconds) {
		throw new IllegalStateException("Cannot set routing directive on branch request");
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#setHeader(java.lang.String, java.lang.String)
	 */
	public void setHeader(String name, String value)
			throws IllegalArgumentException, NullPointerException {
		_request.setHeader(name, value);
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#setHeaderForm(javax.servlet.sip.SipServletMessage.HeaderForm)
	 */
	public void setHeaderForm(HeaderForm form) {
		_request.setHeaderForm(form);
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#setParameterableHeader(java.lang.String, javax.servlet.sip.Parameterable)
	 */
	public void setParameterableHeader(String name, Parameterable param)
			throws IllegalArgumentException {
		_request.setParameterableHeader(name, param);
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
		return _request.getServletContext();
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
	public AsyncContext startAsync() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * Returns the internal SipServletRequest for internal container use
	 * @return
	 */
	/*package-private*/ SipServletRequestImpl getInternalSipServletRequest(){
		return _request;
	}

}
