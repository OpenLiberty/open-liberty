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
package com.ibm.ws.sip.stack.transaction.common;

import jain.protocol.ip.sip.SipException;

/**
 * @author amirk
 */
public class BadResponseException extends SipException
{
	
	int m_statusCode;
	
	/**
	 * 
	 */
	public BadResponseException( int statusCode )
	{
		super();
		m_statusCode = statusCode;
	}

	/**
	 * @param message
	 */
	public BadResponseException( String message , int statusCode)
	{
		super(message);
		m_statusCode = statusCode;
	}	

}


