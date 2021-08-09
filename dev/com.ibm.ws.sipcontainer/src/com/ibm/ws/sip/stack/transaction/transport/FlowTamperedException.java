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
 * an existing flow (RFC 5626) but the flow token was tampered with.
 * The catcher of this exception delivers a 403 (Forbidden) response back
 * to the application.
 * 
 * @author ran
 */
public class FlowTamperedException extends SIPTransportException
{
	/** serialization version identifier */
	private static final long serialVersionUID = 1L;

	/**
	 * static instance. this instance is thrown when the thrower knows about
	 * the catcher, and the catcher does not need the stack trace or any other
	 * exception state, other than the fact that this exception was thrown.
	 */ 
	private static final FlowTamperedException s_instance = new FlowTamperedException();

	/**
	 * private constructor
	 */
	private FlowTamperedException() {
		super();
	}

	/**
	 * throws the singleton instance
	 */
	public static void throwIt() throws FlowTamperedException {
		throw s_instance;
	}
}
