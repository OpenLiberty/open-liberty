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
 * This is the Dummy Message Sender Implementation.
 * This sender will be selected in case the stack is not 
 * running within WAS.
 * 
 * Since it is selected when running outside WAS all
 * resolving services are not available, so this sender
 * cannot do a thing about handling failure to a primary detsination.
 *
 */
public class DefaultBackupMessageSender extends BackupMessageSenderBase {

	//this is the sender singleton instance
	private static IBackupMessageSender _instance;

	//this is the sender singleton constructor
	private DefaultBackupMessageSender(){
		
	}

	/**
	 * This method does nothing, cause it cannot handle cases of
	 * message failures to primary destination.
	 * 
	 * @param messageContext - the message context
	 */
	public void sendMessageToBackup(MessageContext messageContext) {
		MessageContext.doneWithContext(messageContext);
	}

	/**
	 * a method that returns the message sender singleton instance
	 * 
	 * @return the message sender singleton instance
	 */
	public static IBackupMessageSender getInstance() {
		if (_instance == null){
			synchronized(DefaultBackupMessageSender.class){
				if (_instance == null){
					_instance = new DefaultBackupMessageSender();
				}
			}
		}
		return _instance;
	}
}
