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

/**
 * The base class for backup senders in charge of sending
 * messages to backup destinations. 
 *  
 * @author nogat
 *
 */
public abstract class BackupMessageSenderBase implements IBackupMessageSender {

	/**
	 * The flag deciding if the sender is poolable or not 
	 */
	private boolean _isPoolable = false;

	/*
	 * (non-Javadoc)
	 * @see com.ibm.ws.sip.stack.transaction.transport.IMessageSender#setIsPoolable(boolean)
	 */
	public void setIsPoolable(boolean b) {
		_isPoolable = b;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.ws.sip.stack.transaction.transport.IMessageSender#isPoolable()
	 */
	public boolean isPoolable() {
		return _isPoolable;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.ws.sip.stack.transaction.transport.IMessageSender#cleanItself()
	 */
	public void cleanItself() {
		
	}

}
