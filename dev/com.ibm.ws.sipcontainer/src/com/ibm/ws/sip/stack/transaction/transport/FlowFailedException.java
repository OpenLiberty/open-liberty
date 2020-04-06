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
 * Exception thrown when the application attempts to send out a request over
 * an existing flow (RFC 5626) and there is no connection matching that flow.
 * The catcher of this exception delivers a 430 (Flow Failed) response back
 * to the application.
 * 
 * @author ran
 */
public class FlowFailedException extends SIPTransportException
{
	/** serialization version identifier */
	private static final long serialVersionUID = 1L;

	/** response code */
	public static final int FLOW_FAILED_STATUS_CODE = 430;

	/** reason phrase */
	public static final String FLOW_FAILED_REASON_PHRASE = "Flow Failed";

	/**
	 * static instance. this instance is thrown when the thrower knows about
	 * the catcher, and the catcher does not need the stack trace or any other
	 * exception state, other than the fact that this exception was thrown.
	 */ 
	private static final FlowFailedException s_instance = new FlowFailedException();

	/**
	 * private constructor
	 */
	private FlowFailedException() {
		super();
	}

	/**
	 * throws the singleton instance
	 */
	public static void throwIt() throws FlowFailedException {
		throw s_instance;
	}
}
