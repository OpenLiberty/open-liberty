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

import javax.servlet.sip.SipApplicationSession;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.servlets.SipServletResponseImpl;
import com.ibm.ws.sip.container.transaction.ClientTransaction;
import com.ibm.ws.sip.container.transaction.ServerTransaction;
import com.ibm.ws.sip.container.transaction.SipTransaction;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;

public class ResponseRoutedTask extends RoutedTask {
	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(ResponseRoutedTask.class);
    
	private SipTransaction _transaction;
	private SipServletResponseImpl _response;
	private boolean _isOnClientTransaction = false;
	
	public static ResponseRoutedTask getInstance(SipTransaction transaction,
			TransactionUserWrapper transactionUser,
			SipServletResponseImpl response) 
	{
		return new ResponseRoutedTask(transaction, transactionUser, response);
	}
	
	public static ResponseRoutedTask getInstance(SipTransaction transaction, String sessionId, SipServletResponseImpl response) 
	{
		return new ResponseRoutedTask(transaction, sessionId, response);
	}
	
	
	public ResponseRoutedTask(SipTransaction transaction,
							  TransactionUserWrapper transactionUser,
							  SipServletResponseImpl response) 
	{
		super(transactionUser);
		_response = response;
		_transaction = transaction;
		_isOnClientTransaction = (_transaction instanceof ClientTransaction);
		if(_transactionUser == null && _isOnClientTransaction){
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this,"ResponseRoutedTask", "Could not locate TU, executing task on current thread");
            }
			// TU object might not be available when request/responses are used
			// internally for proxying
			setForDispatching(false);// will be executed on container thread
		}
	}
	
	public ResponseRoutedTask(SipTransaction transaction,
			String sessionId,
			SipServletResponseImpl response) 
	{
		super(sessionId);
		_response = response;
		_transaction = transaction;
		_isOnClientTransaction = (_transaction instanceof ClientTransaction);
	}
	

	protected void doTask() {
		//@TODO why do we let the TU be null here (when called from the stack code on SipRouter)?
		boolean isTransInvalidating = false;		
		if(_transactionUser != null && _transactionUser.isInvalidating()){		
			if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug(
                this,
                "doTask",
                "got response on invalidating server transaction: " + _transaction);
			}
			isTransInvalidating = true;
		}
		if (_isOnClientTransaction) {
			// normal response
    	    ClientTransaction clientTransaction = (ClientTransaction)_transaction;
    	    _response.setTransaction(_transaction);
    	    if (isTransInvalidating) {
    	    	// need to remove the transaction from the transactions table
    	    	if (_response.getStatus() >= 200) {
    	    		clientTransaction.onFinalResponse(_response);
    	    	}
    	    }
    	    else {
    	    	clientTransaction.processResponse(_response);
    	    }
    	}
    	else /*on serverTransaction*/{
            // receiving a response on a server transaction is actually
            // an error response generated internally by the stack
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(
                    this,
                    "doTask",
                    "got response on server transaction: " + _transaction);
            }
            // handle as if it was a timeout, just to remove the transaction
            ServerTransaction serverTransaction = (ServerTransaction)_transaction;
            serverTransaction.processTimeout();
    	}
	}

	public String getMethod() {
		return _response.getMethod();
	}
	
	public int priority() {
		return PRIORITY_CRITICAL;
	}

	/**
	 * @see com.ibm.ws.sip.container.util.Queueable#getApplicationSession()
	 */
	@Override
	public SipApplicationSession getApplicationSession() {
		return _response.getApplicationSession(false);
	}
	
	/**
	 * @see com.ibm.ws.sip.container.util.Queueable#getTuWrapper()
	 */
	@Override
	public TransactionUserWrapper getTuWrapper() {
		if (_transactionUser == null) {
			//When the c'tor is called without TU, get the TU from the response
			return _response.getTransactionUser();
		}
		return _transactionUser;
	}
}
