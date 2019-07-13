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

import com.ibm.ws.sip.container.transaction.SipTransaction;

public class TimeoutRoutedTask extends RoutedTask {
	private SipTransaction _transaction;
	public static TimeoutRoutedTask getInstance( SipTransaction transaction) {
   	//TODO will be pooled
		return new TimeoutRoutedTask( transaction);
   }
	
	/**
	 * Ctor
	 * @param transaction
	 */
	public TimeoutRoutedTask(SipTransaction transaction) {
		super(transaction.getOriginalRequest().getTransactionUser());
		_transaction = transaction;
		if( _transactionUser == null){
			//Todo need to see if this is at all possible, if not throw exception here
			setForDispatching(false);
		}
	}

	/**
	 * @see com.ibm.ws.sip.container.router.tasks.RoutedTask#doTask()
	 */
	protected void doTask() {
		_transaction.processTimeout();

	}
	
	/**
	 * @see com.ibm.ws.sip.container.router.tasks.RoutedTask#getMethod()
	 */
	public String getMethod() {
		return " Timeout: " + _transaction.getOriginalRequest().getMethod();
	}
	
	/**
	 * @see com.ibm.ws.sip.container.util.Queueable#priority()
	 */
	public int priority() {
		//This task is critical since we must remove the transaction from table 
		//after timeout to prevent memory leaks, even in case of overload
		return PRIORITY_CRITICAL;
	}
}
