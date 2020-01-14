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

import com.ibm.ws.sip.container.servlets.OutgoingSipServletRequest;
import com.ibm.ws.sip.container.servlets.SipServletResponseImpl;

/**
 * 
 * @author Anat Fradin, Dec 6, 2006
 * 
 * Interface that is implemented by side that will use Sender processor.
 * This interface will provide an access from the Sender (which can be a NAPTR 
 * or not NAPTR processor )to the client that implements this interface.
 *
 */
public interface ISenderListener {
		
	/**
	 *  Save the transaction ID in the client that sent the request.
	 * @param id
	 */
	public void saveTransactionId(long id);
	
	/** Notify a client about failed trial to send a request. */
	public void failedToSendRequest(boolean errorDuringSent);
	 
	/** Notify a client about final response received. */
	public void responseReceived(SipServletResponseImpl resp);
	
	/** Returns a reference to the request */
	public OutgoingSipServletRequest getOutgoingRequest();

	/** Returns a reference to the target of the request */
	public SipURL getTarget();
	
	/** Notify a client about timed out response. */
	public void timedOut();
	
}
