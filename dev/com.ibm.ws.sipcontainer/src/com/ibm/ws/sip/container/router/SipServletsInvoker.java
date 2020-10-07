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
package com.ibm.ws.sip.container.router;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import com.ibm.ws.sip.container.parser.SipServletDesc;

/**
 * @author Amir Perlman, Feb 16, 2003
 *
 * Passes calls to the acutal Servlet for execution. The purpose is to be able 
 * to tie container logic to different deployment models, e.g. Websphere Web
 * Container or a Stand alone test enviroment.  
 * 
 */
public interface  SipServletsInvoker
{
	/**
	 * Invoke the specified Sip Servlet. 
	 * @param request The Sip Servlet Request if available otherwise null. 
	 * @param response The Sip Servlet Response if available otherwise null. 
	 * @param sipServletDesc The siplet to be invoked.  
	 * @param listener Get notification after invoking the siplet
	 */
	public void invokeSipServlet(SipServletRequest request, 
							      SipServletResponse response, 
							      SipServletDesc sipServletDesc,
							      SipServletInvokerListener listener);
	/**
	 * Stops the invoker. Called by the container when it is no longer needed.  
	 */
	public void stop();
}