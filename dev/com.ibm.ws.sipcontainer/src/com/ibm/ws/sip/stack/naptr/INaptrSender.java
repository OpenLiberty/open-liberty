/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.stack.naptr;

import com.ibm.ws.sip.stack.context.MessageContext;

/**
 * This is an interface defining actions that Naptr Senders
 * should  have. 
 * 
 * @author nogat
 *
 */
public interface INaptrSender{

	/**
	 * The Naptr Sender can now send the message.
	 * This is called after Naptr Info has been set on message
	 * 
	 * @param messageContext - the context containing the message
	 * @param transport - the transport on which the message will be sent
	 */
	public void sendMessage(MessageContext messageContext,String transport);

	/**
	 * The Naptr Sender is notified that an error
	 * occurred when trying to get Naptr Info for message
	 *   
	 * @param messageContext - the context containing the message
	 */
	public void error(MessageContext messageContext);
	
}
