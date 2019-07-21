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
package com.ibm.ws.sip.container.transaction;

/**
 * This interface informs the listener that it is not longer
 * responsible for this transaction.
 * For Example, when transaction became a part of the Derived TU
 * and original TU is no longer responsilbe for this one. 
 * @author anatf
 *
 */
public interface TransactionListener {
	
	/**
	 * Notification that transaction need to be removed from the listeners counter
	 * @param msg - can be null
	 */
	public void removeTransaction(String method);
	
		
	/**
	 * Notification that transaction need to be added to the the listeners counter
	 * @param msg - can be null
	 */
	public void addTransaction(String method);

}
