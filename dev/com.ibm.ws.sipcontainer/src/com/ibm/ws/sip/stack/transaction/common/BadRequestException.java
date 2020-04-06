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
package com.ibm.ws.sip.stack.transaction.common;

import jain.protocol.ip.sip.SipException;

/**
 * @author amirk
 */
public class BadRequestException extends SipException
{
	int m_statusCode;
	
	/**
	 * 
	 */
	public BadRequestException( int statusCode )
	{
		super();
		m_statusCode = statusCode;
	}

	/**
	 * @param message
	 */
	public BadRequestException( String message , int statusCode)
	{
		super(message);
		m_statusCode = statusCode;
	}
	
	public int getStatusCode()
	{
		return m_statusCode;
	}

}
