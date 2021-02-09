/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.tai.extension;

import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.wsspi.security.tai.TAIResult;

/**
 * The <code>HTTPTrustAssociationInterceptor</code> is an interface for developing a HTTP custom trust association interceptor (TAI).
 * A custom  TAI that implements this interface should also extend <code>com.ibm.wsspi.security.tai.extension.BaseTrustAssociationInterceptor</code> class.
 * <p> 
 * The custom TAI must implement the following methods:
 * <p>
 * <code>initialize</code> to allocate any resources needed for the TAI
 * <p>
 * <code>negotiateValidateandEstablishProtocolTrust</code> that returns <code>TAIResult</code> to indicate the status of the HTTP message being processed
 * <p> 
 * <code>isTargetProtocolInterceptor</code> that returns false/true to indicate whether a HTTP message will be handled by the TAI. 
 */

public interface HTTPTrustAssociationInterceptor {

	/**
	 * The custom TAI should use this method to handle the http request. 
	 * If the method returns false, a request will be ignored by the TAI.
	 * @param req HttpServletRequest to be handled by the TAI
	 * @return boolean <i>true</i> indicates that the  message will be handled by the TAI, otherwise <i>false</i>
	 * @throws WebTrustAssociationFailedException exception
	 */
	public boolean isTargetProtocolInterceptor(HttpServletRequest req)
		throws WebTrustAssociationFailedException;

	/**
	 * Returns <code>TAIResult</code> that indicates the status of the message being processed.
	 * <p>
	 * If authentication succeeds, the TAIResult should contain the status HttpServletResponse.SC_OK and a principal. 
	 * <p>
	 * If authentication fails, the TAIResult should contain a return code of HttpServletResponse.SC_UNAUTHORIZED (401), SC_FORBIDDEN (403), or SC_PROXY_AUTHENTICATION_REQUIRED (407). 
	 * 
	 * @param req incoming SipServletRequest to be handled by the TAI 
	 * @param resp incoming SipServletResponse to be handled by the TAI 
	 * @return TAIResult result of trust association interceptor negotiation
	 * @throws WebTrustAssociationFailedException exception
	 */
	public TAIResult negotiateValidateandEstablishProtocolTrust(
		HttpServletRequest req, HttpServletResponse resp)
		throws WebTrustAssociationFailedException;
	
	/**
	 * Initializes the trust association interceptor.
	 * 
	 * @param properties Properties defined in the TAI properties
	 * @return int <i>0</i> indicates success, any other code indicates a failure
	 * @throws WebTrustAssociationFailedException exception
	 */
	public int initialize(Properties properties)
		throws WebTrustAssociationFailedException;

	/**
	 * Returns the version number of the current TAI implementation.
	 * 
	 * @return String the version of the TAI
	 */
	public String getVersion();

	/**
	 * Returns a type value of the TAI.
	 * 
	 * @return String the type of the TAI
	 */
	public String getType();

	/**
	 * Invoked when the TAI should free any resources it holds. 
	 * For example, it could close a connection to a database.
	 */
	public void cleanup();

}
