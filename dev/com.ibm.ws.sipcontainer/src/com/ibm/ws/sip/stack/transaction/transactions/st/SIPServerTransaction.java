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
package com.ibm.ws.sip.stack.transaction.transactions.st;

import jain.protocol.ip.sip.message.Request;
import jain.protocol.ip.sip.message.Response;

import com.ibm.ws.sip.stack.transaction.transactions.MergedRequestKey;
import com.ibm.ws.sip.stack.transaction.transactions.SIPTransaction;
import com.ibm.ws.sip.stack.transaction.transport.SIPTransportException;

public interface SIPServerTransaction
	extends SIPTransaction
{

	/**
	 * called when a request arrives from the network, to match a server
	 * transaction to this request per rfc 3261 section 17.2.3
	 * @param req the incoming request
	 * @return true if all request-matching rules pass, otherwise false
	 */
	public boolean isRequestPartOfTransaction(Request req);
	
	
	/**
	 *  send the response back to the transport layer
	 *  after prossesing it
	 */
	public void sendResponseToTransport( Response sipResponse )
			throws SIPTransportException;
	
	
	/**
	 *  send the request to the UA
	 *  after prossesing it
	 */
	public void sendRequestToUA( Request req );
	
	
	/**
	 * send responce back to the UA if you ecountered an error
	 * while sending the responce , like an IOException
	 */
	public void notifyRespnseErrorToUA( Response sipResponse );

	/**
	 * gets the identifier of the source request per rfc3261 8.2.2.2
	 * @return identifier of the source request,
	 *  or null if this feature is disabled in configuration
	 */
	public MergedRequestKey getMergedRequestKey();
	
	/**
	 * sets the identifier of the source request per rfc3261 8.2.2.2
	 * @param key identifier of the source request
	 */
	public void setMergedRequestKey(MergedRequestKey key);
}
