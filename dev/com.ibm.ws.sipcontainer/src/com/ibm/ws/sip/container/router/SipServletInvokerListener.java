/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.router;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

/**
 * @author yaronr
 * 
 * Listener for the SIP servlet invoker, used by the invoker to 
 * 		send notification after the siplet was invoked 
 */
public interface SipServletInvokerListener
{
	/**
	 * A servlet has been invoked
	 * 
	 * @param response - the response object 
	 * 		(which ccould be modified by the application)
	 */
	public void servletInvoked(SipServletResponse response);
	
	/**
	 * A servlet has been invoked
	 * 
	 * @param request - the request object 
	 * 		(which ccould be modified by the application)
	 */
	public void servletInvoked(SipServletRequest request);		
}
