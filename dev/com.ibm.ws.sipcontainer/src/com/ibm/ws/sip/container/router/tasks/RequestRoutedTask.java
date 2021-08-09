/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.router.tasks;

import jain.protocol.ip.sip.message.Request;

import javax.servlet.sip.SipServletResponse;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.router.SipRouter;
import com.ibm.ws.sip.container.servlets.SipServletRequestImpl;
import com.ibm.ws.sip.container.transaction.ServerTransaction;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;

public abstract class RequestRoutedTask extends RoutedTask {
	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger = Log.get(RequestRoutedTask.class);
	
	protected SipServletRequestImpl _request;
	protected ServerTransaction _serverTransaction;
	
	/**
	 * CTOR
	 * @param transactionUser
	 * @param request
	 * @param serverTransaction
	 */
	public RequestRoutedTask( TransactionUserWrapper transactionUser, 
							  SipServletRequestImpl request,
							  ServerTransaction serverTransaction) {
		super(transactionUser);
		_request = request;
		_serverTransaction = serverTransaction;
	}

	/**
	 * @see com.ibm.ws.sip.container.router.tasks.RoutedTask#getMethod()
	 */
	public String getMethod(){
		return _request.getMethod();
	}
	
	/**
	 * Check whether the request to process is ACK
	 * @return
	 */
	public boolean isAck(){
		return _request.getMethod().equals(Request.ACK);
	}
	
	/**
	 * Returns the request to process
	 * @return
	 */
	public SipServletRequestImpl getRequest(){
		return _request;
	}
	
	/**
	 * @see com.ibm.ws.sip.container.router.tasks.RoutedTask#doTask()
	 */
	protected void doTask(){
		//check whether the TU was invalidated during the time that the request was queued.
		if (_transactionUser.isTransactionUserInvalidated() || _transactionUser.isInvalidating()){
			if( isAck()){
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "doTask",
							"ACK request (callid="+ _request.getCallId() +")will not be processed since "
							+ _transactionUser + " was already invalidated");
				}
				return; //Cannot send response to an ACK< but the process should be aborted.
			}
			SipRouter.sendErrorResponse(_request, SipServletResponse.SC_CALL_LEG_DONE);
		}else{
			//Bind the session to the request
			_request.setTransactionUser(_transactionUser);

			//Connect the session to current active transaction
			_serverTransaction.setTransactionListener(_transactionUser);

			//Pass notification for the session to handle it.
			_serverTransaction.processRequest(_request);
		}
	}
}
