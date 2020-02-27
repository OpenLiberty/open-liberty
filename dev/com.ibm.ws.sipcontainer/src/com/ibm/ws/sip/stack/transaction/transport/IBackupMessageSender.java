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
package com.ibm.ws.sip.stack.transaction.transport;

import com.ibm.ws.sip.stack.context.MessageContext;

/**
 * @author nogat
 * 
 * This is the message sender interface.
 * 
 * All Message Senders must be able to send messages,
 * get/set a flag indicating they are poolable
 * and clean themselves (needed for poolable senders) 
 *
 */
public interface IBackupMessageSender {

	/**
	 * Order the Sender to send the request.
	 * 
	 * @param messageContext
	 */
	public void sendMessageToBackup(MessageContext messageContext);

	/**
	 * Set the poolable flag
	 * 
	 * @param isPoolable 
	 */
	public void setIsPoolable(boolean isPoolable);

	/**
	 * Clean all members.
	 * This method is called upon poolable objects 
	 * when they are returned to pool. 
	 *
	 */
	public void cleanItself();

	/**
	 * returns true if this sender should be kept in a pool 
	 * 
	 * @return true if poolable. otherwise, false
	 */
	public boolean isPoolable();

}
