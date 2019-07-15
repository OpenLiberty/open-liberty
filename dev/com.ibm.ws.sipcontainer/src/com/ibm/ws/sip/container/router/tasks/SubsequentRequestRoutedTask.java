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

import com.ibm.ws.sip.container.servlets.SipServletRequestImpl;
import com.ibm.ws.sip.container.transaction.ServerTransaction;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;

public class SubsequentRequestRoutedTask extends RequestRoutedTask {

	public static SubsequentRequestRoutedTask getInstance( TransactionUserWrapper transactionUser, 
			 ServerTransaction serverTransaction,
			 SipServletRequestImpl request) {
   	//TODO will be pooled
	   	return new SubsequentRequestRoutedTask( transactionUser, 
	   											request, serverTransaction);
   }
	
	public SubsequentRequestRoutedTask( TransactionUserWrapper transactionUser, 
										SipServletRequestImpl request,
										ServerTransaction serverTransaction) {
		super(transactionUser, request, serverTransaction);
	}

	/**
	 * @see com.ibm.ws.sip.container.router.tasks.RoutedTask#doTask()
	 */
	protected void doTask() {
		super.doTask();

	}

	public int priority() {
		return PRIORITY_CRITICAL;
	}
}
