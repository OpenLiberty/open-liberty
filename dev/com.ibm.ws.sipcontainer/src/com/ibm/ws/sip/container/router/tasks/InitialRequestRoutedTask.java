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

import com.ibm.ws.sip.container.parser.SipServletDesc;
import com.ibm.ws.sip.container.servlets.SipServletRequestImpl;
import com.ibm.ws.sip.container.transaction.ServerTransaction;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;

public class InitialRequestRoutedTask extends RequestRoutedTask {

    private SipServletDesc _sipDesc;
    
    /**
     * 
     */
    public static InitialRequestRoutedTask getInstance( TransactionUserWrapper transactionUser, 
			 ServerTransaction serverTransaction,
			 SipServletRequestImpl request,
			 SipServletDesc sipDesc) {
    	//TODO will be pooled
    	return new InitialRequestRoutedTask( transactionUser, 
				 							 serverTransaction,
				 							 request,
				 							 sipDesc);
    }
    
	/**
	 * Ctor
	 * @param transactionUser
	 */
	private InitialRequestRoutedTask( TransactionUserWrapper transactionUser, 
								 ServerTransaction serverTransaction,
								 SipServletRequestImpl request,
								 SipServletDesc sipDesc) {
		super(transactionUser, request, serverTransaction);
		_sipDesc = sipDesc;
	}
	
	protected void doTask(){
        // An initial request is one that is dispatched to applications
        //based on the containers configured rule set, as opposed to
        //subsequent requests which are routed based on the
        //<em>application path</em> established by a previous initial
        // request.
		_transactionUser.setInitialRequestProcessed();
		
        _transactionUser.setSipServletDesc(_sipDesc);
        _transactionUser.createSessionsWhenListenerExists();
        super.doTask();
	}

	@Override
	public String getAppName() {
		return _sipDesc.getSipApp().getApplicationName();
	}

	@Override
	public Integer getAppIndexForPMI() {
		return _sipDesc.getSipApp().getAppIndexForPmi();
	}
	
	
	
}
