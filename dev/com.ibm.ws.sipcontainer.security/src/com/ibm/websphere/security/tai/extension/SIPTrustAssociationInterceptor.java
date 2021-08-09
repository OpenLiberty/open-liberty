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

import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.wsspi.security.tai.TAIResult;

/**
 * The <code>SIPTrustAssociationInterceptor</code> is an interface for developing a SIP custom trust association interceptor (TAI).
 * 
 * A custom TAI that implements this interface should also extend <code>com.ibm.wsspi.security.tai.extension.BaseTrustAssociationInterceptor</code> class.
 * 
 * <p>
 * The custom TAI must implement the following methods:
 * <p>
 * <code>initialize</code> to allocate any resources needed for the TAI
 * <p>
 * <code>negotiateValidateandEstablishProtocolTrust</code> that returns <code>TAIResult</code> to indicate the status of the message being processed
 * <p> 
 * <code>isTargetProtocolInterceptor</code> that returns false/true to indicate whether a SIP message will be handled by the TAI. 
 * <p>
 * 
 * The idea is during processing a SIP request, the Liberty server will pass the <code>
 * SipServletRequest</code> and <code>SipServletResponse</code> to the SIP trust association interceptor. 
 * The trust association interceptor can inspect the SIP message to see if it contains security
 * attributes (authentication or authorization attributes) from the third party security service.
 * </p>
 */
public interface SIPTrustAssociationInterceptor {

	/**
	 * The custom TAI should use this method to inspect if it can process the <code>SipServletMessage</code>.
	 * The implementation should return <code>true</code> if the TAI can handle the sipMsg, else <code>false</code> should be returned.
	 * @param sipMsg SipServletMessage to be handled by the TAI
	 * @return boolean <i>true</i> indicates that the  message will be handled by the TAI, otherwise <i>false</i>
	 * @throws WebTrustAssociationFailedException exception
	 */
	public boolean isTargetProtocolInterceptor(SipServletMessage sipMsg)
		throws WebTrustAssociationFailedException;

	/**
	 * This method is used to determine whether trust association can be
     * established between the Liberty server and the third party security service.
     * 
	 * This method returns <code>TAIResult</code> that indicates the status of the message being processed.
	 * <p>
	 * If authentication succeeds, the TAIResult should contain the status HttpServletResponse.SC_OK and a principal. 
	 * <p>	 
	 * If the interceptor finds that the request does not contains the expected authentication data, 
	 * it can write the challenge information in the SIP response and return <code>TAIResult</code> with status code 
	 * HttpServletResponse.SC_UNAUTHORIZED (401), or SC_FORBIDDEN (403), or SC_PROXY_AUTHENTICATION_REQUIRED (407). 
	 * 
	 * @param req incoming SipServletRequest to be handled by the TAI 
	 * @param resp incoming SipServletResponse to be handled by the TAI 
	 * @return TAIResult result of trust association interceptor negotiation
	 * @throws WebTrustAssociationFailedException exception
	 */
	public TAIResult negotiateValidateandEstablishProtocolTrust(
		SipServletRequest req, SipServletResponse resp)
		throws WebTrustAssociationFailedException;

	/**
	 * Initializes the SIP trust association interceptor.
	 * Invoked before the first message is processed so that the implementation can allocate any resources it needs. 
	 * For example, it could establish a connection to a database or LDAP. 
	 * 
	 * @param properties Properties defined in the TAI properties
	 * @return int - <i>0</i> indicates success and anything else a failure
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
