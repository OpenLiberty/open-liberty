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
package com.ibm.ws.sip.container.transaction;

import jain.protocol.ip.sip.address.SipURL;

import com.ibm.ws.sip.container.servlets.*;

/**
 * @author yaronr
 * Created on Jul 15, 2003
 * 
 * The client transaction listener
 */
public interface ClientTransactionListener extends TransactionListener 
{
	/**
	 * A response to this client transaction arrived
	 * @param response - the response
	 */
	public void processResponse(SipServletResponseImpl response);
	
	/**
	 * A timeout occur while sending this request
	 *
	 * @param request - the request
	 */
	public void processTimeout(SipServletRequestImpl request);
	

	/**
	 * Process an error on application router exception.
	 * 
	 * @param request
	 */
	public void processCompositionError(SipServletRequestImpl request);
		

	/**
	 * Notify listener about terminated transacation
	 * @param request 
	 *
	 * @param request - the request
	 */
	public void clientTransactionTerminated(SipServletRequestImpl request);
	
	
	/**
	 * The transaction is about to send a request. The request can be an 
	 * intial request or a CANCEL or ACK message. 
	 * @param request - the request to be sent
	 * @return true if the sender should continue sending the request or false
	 * if the request should not be sent a this time. E.g. Cancel request when
	 *  a provisional response has not been received yet. 
	 */
	public boolean onSendingRequest(SipServletRequestImpl request);

	
	/**
	 * Method that saved where the request was actually sent.(The IP address).
	 * This can be used later when the next request should be sent to the same
	 * destnation (E.g.CANCEL should be sent to the same address where the
	 * INVITE was sent - no new NAPTR/SRV query should be done).
	 * @param lastUsedDestination
	 */
	public void setUsedDestination(SipURL lastUsedDestination);
	
	
	/**
	 * Retuns the saved latest destination if any srored at the object that implements
	 * this listener.
	 * @return
	 */
	public SipURL getUsedDestination();
	
}
