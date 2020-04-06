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
package com.ibm.ws.sip.security.auth;

import java.util.LinkedList;
import java.util.List;

import javax.servlet.sip.AuthInfo;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.servlets.SipServletRequestImpl;
import com.ibm.ws.sip.container.servlets.SipServletResponseImpl;

/**
 * Request Authentication Information (implementation). 
 * @author dedi
 */
public class AuthInfoImpl implements AuthInfo {

	/**
     * Our logger object.
     */
    private static final LogMgr c_logger = Log.get(AuthInfoImpl.class);

	/**
	 * The list of auth info headers (since each auth info object can have
	 * several headers.
	 */
	List<AuthHeader> _authHeaders = new LinkedList<AuthHeader>();
	
	/**
	 * Add an additional auth info header.
	 */
	public void addAuthInfo(int statusCode, String realm, String username,
			String password)
	{
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "<init>", new Object[] {statusCode, realm, 
					username, "*****"});
		}

		AuthHeader newHeader = 
			new AuthHeader(statusCode, realm, username, password);
		_authHeaders.add(newHeader);

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "<init>");
		}
	}
	
	/**
	 * Write this auth info (all headers) to a sip request.
	 * @param request The SIP Request to write to
	 * @param response The SIP response that contained the auth challange.
	 */
	public void writeToRequest(SipServletRequestImpl request,
			SipServletResponseImpl response)
	{
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "writeToRequest", 
					new Object[] {request, response});
		}

		for(AuthHeader header : _authHeaders)
		{
			header.writeToRequest(request, response);
		}
		
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "writeToRequest");
		}
	}
}
