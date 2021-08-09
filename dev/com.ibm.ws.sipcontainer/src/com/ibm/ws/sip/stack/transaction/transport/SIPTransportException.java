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

import jain.protocol.ip.sip.SipException;

/**
 * @author Amirk
 *
 * on error in the transport error
 */
public class SIPTransportException extends SipException
{
   
	/**
	 * Constructs a new SipException
	 */
	public SIPTransportException() 
	{
		super();
	}
    
	/**
	 * Constructs a new SipException with the specified
	 * detail message.
	 * @param <var>msg</var> the message detail of this Exception.
	 */
	public SIPTransportException(String msg) 
	{
		super(msg);
	}	
}
