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
package com.ibm.ws.sip.stack.context;

import jain.protocol.ip.sip.SipParseException;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.stack.transaction.transactions.BranchMethodKey;
import com.ibm.ws.sip.stack.transaction.transactions.SIPTransactionsModel;

/**
 * the class holding the request message context.
 * It also has methods that determine the behavior in case of failures  
 * 
 * @author nogat
 */
public class RequestContext extends MessageContext {

	/** class logger */
	private static final LogMgr s_logger = Log.get(RequestContext.class);

	/*
	 * (non-Javadoc)
	 * @see com.ibm.ws.sip.stack.context.MessageContext#handleFailure()
	 */
	public void handleFailure() {
		//first we check if the transaction is present
		if (sipTransaction == null){

			//transaction is not present, so we'll try and look for it.
			SIPTransactionsModel sipTransactionsModel = SIPTransactionsModel.instance();
			try {
				BranchMethodKey key = sipTransactionsModel.createTransactionId(sipMessage);
				sipTransaction = sipTransactionsModel.getClientTransaction(key);
			} catch (SipParseException e) {
				//will be handled next because transaction will remain null
			}

			//if all failed, we report error
			if (sipTransaction == null){
				if (s_logger.isTraceDebugEnabled()) {
					s_logger.traceDebug(this,"handleFailure",
							"Sip Transaction not found when handling failure of request. " +
							"cannot continue. Sip Message: "+ sipMessage);
				}
				return;
			}
		}
		//report transaction about the transport error that occurred. 
		sipTransaction.prossesTransportError();
	}

	protected void doneWithContext() {
	}
}
