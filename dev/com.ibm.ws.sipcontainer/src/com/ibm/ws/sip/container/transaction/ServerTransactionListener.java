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

import javax.servlet.sip.*;

import com.ibm.ws.sip.container.servlets.SipServletRequestImpl;

/**
 * @author yaronr
 * Created on Jul 15, 2003
 * 
 * the server transaction listener
 */
public interface ServerTransactionListener extends TransactionListener
{
	/**
	 * A request arrived to this transaction, process it
	 * @param response
	 */
	public void processRequest(SipServletRequest request);
	
	/**
	 * The transaction is about to send a response
	 * @param response - the response to be sent
	 * @return - should we send the the request afterall
	 */
	public boolean onSendingResponse(SipServletResponse response);
	
	/**
	 * The transaction completed  - the final response was sent
	 *
	 */
	public void onTransactionCompleted();

	/**
	 * Notify listener about terminated transacation
	 * @param request 
	 *
	 * @param request - the request
	 */
	public void serverTransactionTerminated(SipServletRequestImpl request);
		
}
