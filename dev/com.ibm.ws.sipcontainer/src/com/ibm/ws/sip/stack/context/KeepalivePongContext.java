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
package com.ibm.ws.sip.stack.context;

/**
 * Wraps a KeepalivePong message through the entire process of sending
 * out the keepalive pong.
 * 
 * @author ran
 * @see com.ibm.ws.jain.protocol.ip.sip.message.KeepalivePong
 */
public class KeepalivePongContext extends MessageContext
{
	/**
	 * @see com.ibm.ws.sip.stack.context.MessageContext#doneWithContext()
	 */
	protected void doneWithContext() {
	}

	/**
	 * @see com.ibm.ws.sip.stack.context.MessageContext#handleFailure()
	 */
	public void handleFailure() {
	}
}
