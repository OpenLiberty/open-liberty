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
package com.ibm.ws.sip.container.naptr;

import jain.protocol.ip.sip.address.SipURL;

import java.io.IOException;

import com.ibm.ws.sip.container.servlets.OutgoingSipServletRequest;
import com.ibm.ws.sip.container.servlets.SipServletResponseImpl;

/**
 * 
 * @author Anat Fradin, Dec 6, 2006
 * Interface that will define the basic usage of the request sender.
 */
public interface ISendingProcessor {


	/**
	 * Order the Sender to send the request.
	 * @param request
	 * @param client
	 */
	public void sendRequest(OutgoingSipServletRequest request, ISenderListener client)
		throws IOException;
	
	/**
	 * Update the Sender about received Response
	 * @param resp
	 * @param client
	 */
	public void responseReceived(SipServletResponseImpl resp, ISenderListener client);
	
	/**
	 * Update the Sender about timedOut() event on the previous sent response
	 * @param client
	 */
	 public void processTimeout(ISenderListener client); 
	 
	 /**
	  * Method used to get the destination which used for the last
	  * request. Needed when Requests (like CANCEL) should be sent 
	  * to the same IP like the initial INVITE.
	  */
	 public SipURL getLastUsedDestination( ISenderListener client);
}
