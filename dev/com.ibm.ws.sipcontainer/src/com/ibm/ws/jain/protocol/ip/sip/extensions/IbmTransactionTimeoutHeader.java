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
package com.ibm.ws.jain.protocol.ip.sip.extensions;

/**
 * The IBM-TransactionTimeout header.
 * This header is set by the application before sending the message out,
 * and then the stack reads its value to determine the timer value.
 * 
 * @author ran
 */
public class IbmTransactionTimeoutHeader extends IbmTimerHeader
{
	/** serialization version identifier */
	private static final long serialVersionUID = -4537589749531303826L;

	/** name of the IBM-TransactionTimeout header */
	public static final String name = "IBM-TransactionTimeout";

	/**
	 * constructor
	 * @param application true if created by the application, false if came through the network
	 */
	public IbmTransactionTimeoutHeader(boolean application) {
		super(application);
	}

	/**
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#getName()
	 */
	public String getName() {
		return name;
	}
}
