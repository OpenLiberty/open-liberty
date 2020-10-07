/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jain.protocol.ip.sip.address;

import jain.protocol.ip.sip.SipException;
import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.address.SipURL;

import java.net.InetAddress;
import java.util.Iterator;

import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
 * This class comes to solve the problem of too many SIPURL
 * in the memory. Its name is not accurate . It should be CachableSipURLImpl
 * This class assumes the SipURL is immutable until the caller use one of
 * the setXXX() method. 
 * This is basically a wrapper class above SipURLImpl 
 * We have noticed that most SIPURL are not
 * changed by our container. This is good since it hints we can
 * cache them and eliminates duplicates.
 * However, the user may want to use the public API and set some
 * of the SIPURL data. In such case we no longer able to use the cached
 * data. Therefore, we shall clone the original and present a new SipURLImpl
 * in the m_url member. (and turn on the immutable flag)
 * @author Moti
 */
public class ImmutableSipURLImpl extends URIImpl implements SipURL
{
	/**
	 * This varible usually references some SIPURL
	 * 
	 */
	private SipURLImpl m_url = null;
	
	/**
	 * Is this SipURL has gone changes since its creation
	 * @param jainSipUrl
	 */
	private boolean m_isMutable = false;
	
	
	protected ImmutableSipURLImpl(SipURLImpl sipURL)
	{
		m_url = sipURL;
	}


	/**
	 * if the sip URL need to be immutable we must treat it differently.
	 *
	 */
	private synchronized void URLchanged()
	{
		if (m_isMutable == false)
		{
			m_isMutable = true;
			m_url = (SipURLImpl)m_url.clone();
			// TODO : here we have a chance to save the changed 
			// sip URL in the global sip url cache (maintained inside
			// AddressFactoryImpl. But I think it is not wise
			// since if the user changed it once it may changed it again
			// thus we shall have many cloned version in the cache.
		}
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#hasIsdnSubAddress()
	 */
	public boolean hasIsdnSubAddress() {
		return m_url.hasIsdnSubAddress();
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#removeIsdnSubAddress()
	 */
	public void removeIsdnSubAddress() {
		URLchanged();
		m_url.removeIsdnSubAddress();
		
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#getPostDial()
	 */
	public String getPostDial() {
		return m_url.getPostDial();
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#setMethod(java.lang.String)
	 */
	public void setMethod(String method) throws IllegalArgumentException, SipParseException {
		URLchanged();
		m_url.setMethod(method);
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#hasMAddr()
	 */
	public boolean hasMAddr() {
		return m_url.hasMAddr();
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#setHeader(java.lang.String, java.lang.String)
	 */
	public void setHeader(String name, String value) throws IllegalArgumentException, SipParseException {
		URLchanged();
		m_url.setHeader(name, value);
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#setTransport(java.lang.String)
	 */
	public void setTransport(String transport) throws IllegalArgumentException, SipParseException {
		URLchanged();
		m_url.setTransport(transport);
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#setUserType(java.lang.String)
	 */
	public void setUserType(String userType) throws IllegalArgumentException, SipParseException {
		URLchanged();
		m_url.setUserType(userType);		
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#hasPostDial()
	 */
	public boolean hasPostDial() {
		return m_url.hasPostDial();
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#isGlobal()
	 */
	public boolean isGlobal() throws SipException {
		return m_url.isGlobal();
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#removePostDial()
	 */
	public void removePostDial() {
		URLchanged();
		m_url.removePostDial();
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#removeHeaders()
	 */
	public void removeHeaders() {
		URLchanged();
		m_url.removeHeaders();
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#getUserName()
	 */
	public String getUserName() {
		return m_url.getUserName();
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#hasUserType()
	 */
	public boolean hasUserType() {
		return m_url.hasUserType();
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#hasUserName()
	 */
	public boolean hasUserName() {
		return m_url.hasUserName();
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#hasMethod()
	 */
	public boolean hasMethod() {
		return m_url.hasMethod();
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#removeUserName()
	 */
	public void removeUserName() {
		URLchanged();
		m_url.removeUserName();
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#setPostDial(java.lang.String)
	 */
	public void setPostDial(String postDial) throws IllegalArgumentException, SipException, SipParseException {
		URLchanged();
		m_url.setPostDial(postDial);
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#setUserName(java.lang.String)
	 */
	public void setUserName(String userName) throws IllegalArgumentException, SipParseException {
		URLchanged();
		m_url.setUserName(userName);
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#setMAddr(java.lang.String)
	 */
	public void setMAddr(String mAddr) throws IllegalArgumentException, SipParseException {
		URLchanged();
		m_url.setMAddr(mAddr);
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#getUserPassword()
	 */
	public String getUserPassword() {
		return m_url.getUserPassword();
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#getHeaders()
	 */
	public Iterator getHeaders() {
		return m_url.getHeaders();
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#hasUserPassword()
	 */
	public boolean hasUserPassword() {
		return m_url.hasUserPassword();
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#hasHeader(java.lang.String)
	 */
	public boolean hasHeader(String name) throws IllegalArgumentException {
		return m_url.hasHeader(name);
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#removeUserPassword()
	 */
	public void removeUserPassword() {
		URLchanged();
		m_url.removeUserPassword();
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#getIsdnSubAddress()
	 */
	public String getIsdnSubAddress() {
		return m_url.getIsdnSubAddress();
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#setUserPassword(java.lang.String)
	 */
	public void setUserPassword(String userPassword) throws IllegalArgumentException, SipException, SipParseException {
		URLchanged();
		m_url.setUserPassword(userPassword);
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#getUserType()
	 */
	public String getUserType() {
		return m_url.getUserType();
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#getHost()
	 */
	public String getHost() {
		return m_url.getHost();
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#removeUserType()
	 */
	public void removeUserType() {
		URLchanged();
		m_url.removeUserType();
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#setHost(java.lang.String)
	 */
	public void setHost(String host) throws IllegalArgumentException, SipParseException {
		URLchanged();
		m_url.setHost(host);
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#getMethod()
	 */
	public String getMethod() {
		return m_url.getMethod();
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#setHost(java.net.InetAddress)
	 */
	public void setHost(InetAddress host) throws IllegalArgumentException, SipParseException {
		URLchanged();
		m_url.setHost(host);
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#removeMethod()
	 */
	public void removeMethod() {
		URLchanged();
		m_url.removeMethod();
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#getPort()
	 */
	public int getPort() {
		return m_url.getPort();
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#setIsdnSubAddress(java.lang.String)
	 */
	public void setIsdnSubAddress(String isdnSubAddress) throws IllegalArgumentException, SipException, SipParseException {
		URLchanged();
		m_url.setIsdnSubAddress(isdnSubAddress);
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#hasPort()
	 */
	public boolean hasPort() {
		return m_url.hasPort();
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#getMAddr()
	 */
	public String getMAddr() {
		return m_url.getMAddr();
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#removePort()
	 */
	public void removePort() {
		URLchanged();
		m_url.removePort();
		
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#removeMAddr()
	 */
	public void removeMAddr() {
		URLchanged();
		m_url.removeMAddr();
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#setPort(int)
	 */
	public void setPort(int port) throws SipParseException {
		URLchanged();
		m_url.setPort(port);
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#setMAddr(java.net.InetAddress)
	 */
	public void setMAddr(InetAddress mAddr) throws IllegalArgumentException, SipParseException {
		URLchanged();
		m_url.setMAddr(mAddr);
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#getTTL()
	 */
	public int getTTL() {
		return m_url.getTTL();
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#setGlobal(boolean)
	 */
	public void setGlobal(boolean global) throws SipException, SipParseException {
		URLchanged();
		m_url.setGlobal(global);
		
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#hasTTL()
	 */
	public boolean hasTTL() {
		return m_url.hasTTL();
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#getHeader(java.lang.String)
	 */
	public String getHeader(String name) throws IllegalArgumentException {
		return m_url.getHeader(name);
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#removeTTL()
	 */
	public void removeTTL() {
		URLchanged();
		m_url.removeTTL();
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#hasHeaders()
	 */
	public boolean hasHeaders() {
		return m_url.hasHeaders();
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#setTTL(int)
	 */
	public void setTTL(int ttl) throws SipParseException {
		URLchanged();
		m_url.setTTL(ttl);
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#removeHeader(java.lang.String)
	 */
	public void removeHeader(String name) throws IllegalArgumentException {
		URLchanged();
		m_url.removeHeader(name) ;
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#getTransport()
	 */
	public String getTransport() {
		return m_url.getTransport();
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#hasTransport()
	 */
	public boolean hasTransport() {
		return m_url.hasTransport();
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.SipURL#removeTransport()
	 */
	public void removeTransport() {
		URLchanged();
		m_url.removeTransport() ;	
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.URI#getSchemeData()
	 */
	public String getSchemeData() {
		return m_url.getSchemeData();
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.URI#setScheme(java.lang.String)
	 */
	public void setScheme(String scheme) throws IllegalArgumentException, SipParseException {
		URLchanged();
		m_url.setScheme(scheme) ;
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.URI#getScheme()
	 */
	public String getScheme() {
		return m_url.getScheme();
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.URI#clone()
	 */
	public Object clone() {
		return new ImmutableSipURLImpl((SipURLImpl) m_url.clone());
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.address.URI#setSchemeData(java.lang.String)
	 */
	public void setSchemeData(String schemeData) throws IllegalArgumentException, SipParseException {
		URLchanged();
		m_url.setSchemeData(schemeData)  ;
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.Parameters#getParameter(java.lang.String)
	 */
	public String getParameter(String name) throws IllegalArgumentException {
		return m_url.getParameter(name);
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.Parameters#setParameter(java.lang.String, java.lang.String)
	 */
	public void setParameter(String name, String value) throws IllegalArgumentException, SipParseException {
		URLchanged();
		m_url.setParameter(name, value) ;
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.Parameters#hasParameters()
	 */
	public boolean hasParameters() {
		return m_url.hasParameters();
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.Parameters#hasParameter(java.lang.String)
	 */
	public boolean hasParameter(String name) throws IllegalArgumentException {
		return m_url.hasParameter(name);
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.Parameters#removeParameter(java.lang.String)
	 */
	public void removeParameter(String name) throws IllegalArgumentException {
		URLchanged();
		m_url.removeParameter(name) ;	
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.Parameters#removeParameters()
	 */
	public void removeParameters() {
		URLchanged();
		m_url.removeParameters() ;	
	}


	/* (non-Javadoc)
	 * @see jain.protocol.ip.sip.Parameters#getParameters()
	 */
	public Iterator getParameters() {
		return m_url.getParameters();
	}
	
    public String toString()
    {
    	return m_url.toString();
    }
    
    

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		System.err.println("ImmutableSipURLImpl:equal" + obj);
		if (obj == null)
			return false;
		if (obj instanceof ImmutableSipURLImpl)
		{
			ImmutableSipURLImpl other = (ImmutableSipURLImpl)obj;
			System.err.println("url1:" + m_url.toString());
			System.err.println("url2:" + other.m_url.toString());
			return other.m_url.equals(m_url);
		}
		return false;
	}
	
	
	/* (non-Javadoc)
	 * @see com.ibm.ws.jain.protocol.ip.sip.address.URIImpl#writeToCharBuffer(com.ibm.ws.sip.parser.util.CharsBuffer)
	 */
	public void writeToCharBuffer(CharsBuffer ret) {
		m_url.writeToCharBuffer(ret);
	}
	
	
}
