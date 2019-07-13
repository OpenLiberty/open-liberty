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

import jain.protocol.ip.sip.SipProvider;
import jain.protocol.ip.sip.message.Response;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.sessions.SipTransactionUserTable;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;

public class StrayResponseRoutedTask extends RoutedTask {

	/**
     * Class Logger.
     */
    private static final transient LogMgr c_logger = Log
            .get(StrayResponseRoutedTask.class);
    
	private SipProvider _provider;
	private Response _JAINresponse;
	private boolean _createDerived;
	public static StrayResponseRoutedTask getInstance( TransactionUserWrapper transactionUser, 
													   Response response, 
												   	   SipProvider provider,
												   	   boolean createDerived) {
		return new StrayResponseRoutedTask( transactionUser, response, provider,createDerived);
	}
	
	public StrayResponseRoutedTask( TransactionUserWrapper transactionUser, 
									Response response, 
									SipProvider provider,
									boolean createDerived) {
		super(transactionUser);
		_provider = provider;
		_JAINresponse = response;
		_createDerived = createDerived;
	}

	protected void doTask() {
		if(_transactionUser.isInvalidating() || _transactionUser.isTransactionUserInvalidated()){
			if (c_logger.isTraceDebugEnabled()) {
    			c_logger.traceDebug(this, "StrayResponseRoutedTask", 
    				"Transaction user got invalidated by the time this task was queued. Aborting task. TU ID = " + _transactionUser.getId());
    		}
			return;
		}
		TransactionUserWrapper tu = _transactionUser;
		if(_createDerived){
			TransactionUserWrapper transactionUser = 
				SipTransactionUserTable.getInstance().getTransactionUserInboundResponse(_JAINresponse);
			if(transactionUser != null){	
				if(transactionUser.isInvalidating()){		
					if (c_logger.isTraceDebugEnabled()) {
	                c_logger.traceDebug(
	                    this,
	                    "doTask",
	                    "return because got stray response on invalidating transaction: " + transactionUser.getId());
					}
					return;
				}
				//We should look again - maybe till now appropriate TU was already created.
				if (c_logger.isTraceDebugEnabled()) {
	    			c_logger.traceDebug(this, "StrayResponseRoutedTask", 
	    				" appropriate TU was already created ID = " + tu.getId());
	    		}
				tu = transactionUser;
			}
			else{
				if (c_logger.isTraceDebugEnabled()) {
	    			c_logger.traceDebug(this, "StrayResponseRoutedTask", 
	    				" Derived should be created for " + tu.getId());
	    		}
				if(!tu.isProxying()){
					//in case of proxying, we will create the derived TU later, when we will determine
					//which is the TU of that current branch
					tu = tu.createDerivedTU(_JAINresponse,"StrayResponseRoutedTask - create Derived as reason of stray response");
				}
			}
		}
		tu.processStrayResponse(_JAINresponse, _provider);
	}

	public String getMethod() {
		return "Stray Response";
	}
	
	public int priority() {
		return PRIORITY_CRITICAL;
	}
}
