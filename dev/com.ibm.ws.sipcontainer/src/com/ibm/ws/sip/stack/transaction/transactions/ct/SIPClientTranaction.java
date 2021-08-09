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
package com.ibm.ws.sip.stack.transaction.transactions.ct;

import jain.protocol.ip.sip.SipException;
import jain.protocol.ip.sip.message.Request;
import jain.protocol.ip.sip.message.Response;

import java.io.IOException;

import com.ibm.ws.sip.stack.transaction.transactions.SIPTransaction;
import com.ibm.ws.sip.stack.transaction.transport.Hop;

public interface SIPClientTranaction
	extends SIPTransaction
{
	// states that are common to invite and non-invite client transactions
	public static final int STATE_PROCEEDING = 1;
	public static final int STATE_COMPLETED  = 2;
	public static final int STATE_TERMINATED = 3;
	
	/**
	 *  send the response back to the UA layer
	 *  after prossesing it
	 */
	public void sendResponseToUA( Response sipResponse );
	
	
	/**
	 *  send the request to the Transport Layer
	 *  after prossesing it
	 */
	public void sendRequestToTransport( Request req )
		throws IOException,SipException;
		
		
	/**
	 * send request back to the UA if you ecountered an error
	 * while sending the request , such as an IOException
	 */
	public void notifyRequestErrorToUA( Request sipRequest );
	
	
	
	/**
	 *  notify the transaction to work with
	 */
	public void notifyTransactionTimeoutToUA();
	
	
	/**
	 * get the final response that ended this transaction
	 * @return - the final response
	 */
	public Response getFinalResponse();

	/** @return the hop for sending retransmissions */
	public Hop getHop();
	
	/** @param hop the hop for sending retransmissions */
	public void setHop(Hop hop);
}
