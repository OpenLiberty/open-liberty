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

import java.io.IOException;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.jain.protocol.ip.sip.SipProviderImpl;
import com.ibm.ws.sip.container.servlets.OutgoingSipServletRequest;

/**
 * 
 * @author Anat Fradin, Dec 6, 2006
 * Abstract class that has a common information for future extended objects.
 */
public abstract class SendProcessor implements ISendingProcessor {
	
	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger = Log.get(SendProcessor.class);
	
	/**
	 * This flag will be true when instance of this object will be created
	 * from pool and will be able to be returned to pool.
	 */
	private boolean _isPoolable = false;
	
	/**
	 * Method that used to clean the SendProcessor from derived class object 
	 * before it inserted back to the pool
	 * 
	 */
	public  void cleanItself(){
		_isPoolable = false;
	}
	
	/**
	 * Helper method that allocates the transactionId from SipProvider
	 * and then actually sent the request.
	 * This method is used only after the destination was chose and set.
	 * @param request
	 * @throws IOException
	 */
	protected void send(OutgoingSipServletRequest request,
						ISenderListener client) throws IOException{
    	// call proprietary API from the stack to allocate a transaction ID before sending.
		// this ensures the transaction exists in the TransactionTable before receiving
		// the response on a different thread.
	    long transactionId = SipProviderImpl.allocateTransactionId();
        client.saveTransactionId(transactionId);
        
        if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "send", "Request = " + request);
		}
        
        request.sendImpl(transactionId);
	}

	/**
	 * Returns the value of the _isPoolable member
	 * @return
	 */
	public boolean isPoolable() {
		return _isPoolable;
	}

	/**
	 * Sets the value of the _isPoolable member
	 * @return
	 */
	public void setIsPoolable(boolean poolable) {
		_isPoolable = poolable;
	}

}
