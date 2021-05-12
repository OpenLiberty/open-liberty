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
package com.ibm.wsspi.security.tai.extension;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import com.ibm.websphere.security.tai.extension.HTTPTrustAssociationInterceptor;
import com.ibm.websphere.security.tai.extension.SIPTrustAssociationInterceptor;
import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.ws.sip.container.was.ThreadLocalStorage;
import com.ibm.wsspi.security.tai.TAIResult;
import com.ibm.wsspi.security.tai.TrustAssociationInterceptor;

/**
 * This abstract <code>BaseTrustAssociationInterceptor</code> class provides a base implementation of the <code>TrustAssociationInterceptor</code> interface.
 */
public abstract class BaseTrustAssociationInterceptor implements TrustAssociationInterceptor {

	
	private boolean isHTTPTrustAssociationInterceptor = false;
	private boolean isSIPTrustAssociationInterceptor = false;
	
	/**
	 * Constructor for a new BaseTrustAssociationInterceptor
	 */
	public BaseTrustAssociationInterceptor() {
		super();
		if (this instanceof HTTPTrustAssociationInterceptor){
			isHTTPTrustAssociationInterceptor = true;
		}
		if (this instanceof SIPTrustAssociationInterceptor){
			isSIPTrustAssociationInterceptor = true;
		}
	}

	/**
	 * Determines the <code>HttpServletRequest</code> protocol if it's either HTTP or SIP and 
	 * invokes an appropriate <code>isTargetProtocolInterceptor</code> method for either <code>HTTPTrustAssociationInterceptor</code> 
	 * or <code>SIPTrustAssociationInterceptor</code> implementation.
	 * 
	 * @param req <code>HttpServletRequest</code> object
	 */
	public boolean isTargetInterceptor(HttpServletRequest req)
		throws WebTrustAssociationFailedException {

		boolean result = false;
		if ((isHTTPTrustAssociationInterceptor)&&(req.getProtocol().startsWith("HTTP"))) {
			HTTPTrustAssociationInterceptor inter = (HTTPTrustAssociationInterceptor)this;
			result = result || inter.isTargetProtocolInterceptor(req);
		}else if ((isSIPTrustAssociationInterceptor)&&(req.getProtocol().startsWith("SIP"))){
			SIPTrustAssociationInterceptor inter = (SIPTrustAssociationInterceptor)this;
			SipServletRequest sipReq = (SipServletRequest)ThreadLocalStorage.getSipServletRequest();
			if (sipReq != null) {
				result = result || inter.isTargetProtocolInterceptor(sipReq);
			}else{
				SipServletResponse sipResp = ThreadLocalStorage.getSipServletResponse();
				if (sipResp != null) {
					result = result || inter.isTargetProtocolInterceptor(sipResp);
				}
			}
		}

		return result;

	}

	/**
	 * Determines the <code>HttpServletRequest</code> and <code>HttpServletResponse</code> protocol if it's either HTTP or SIP and 
	 * invokes an appropriate <code>negotiateValidateandEstablishProtocolTrust</code> method for either <code>HTTPTrustAssociationInterceptor</code> 
	 * or <code>SIPTrustAssociationInterceptor</code> implementation.
	 * 
	 * @param req <code>HttpServletRequest</code> object 
	 * @param resp <code>HttpServletResponse</code> object
	 */
	public TAIResult negotiateValidateandEstablishTrust(HttpServletRequest req,
		HttpServletResponse resp)
		throws WebTrustAssociationFailedException {

		TAIResult result = null;

		
		if ((isSIPTrustAssociationInterceptor)&&(req.getProtocol().startsWith("SIP"))){
			SIPTrustAssociationInterceptor inter = (SIPTrustAssociationInterceptor)this;
			SipServletRequest sipReq = ThreadLocalStorage.getSipServletRequest();
			SipServletResponse sipResp = ThreadLocalStorage.getSipServletResponse();
//			SipAppDesc appdesc = ThreadLocalStorage.getSipAppDesc();
//			SIPSecurityThreadLocalStorage.setSipAppDesc(appdesc);
//			String servletName = ThreadLocalStorage.getSipServletName();
//			SIPSecurityThreadLocalStorage.setSipServletName(servletName);
			result = inter.negotiateValidateandEstablishProtocolTrust(sipReq, sipResp);			
		}
		else if ((isHTTPTrustAssociationInterceptor)&&(req.getProtocol().startsWith("HTTP"))) {
			HTTPTrustAssociationInterceptor inter = (HTTPTrustAssociationInterceptor)this;
			result = inter.negotiateValidateandEstablishProtocolTrust(req, resp);
		}

		return result;

	}

}
