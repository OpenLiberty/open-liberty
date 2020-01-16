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

import java.io.IOException;

/**
 * exception thrown when trying to send out a large message over UDP.
 * 
 * rfc 3261-18.1.1
 * If a request is within 200 bytes of the path MTU, or if it is larger
 * than 1300 bytes and the path MTU is unknown, the request MUST be sent
 * using an RFC 2914 [43] congestion controlled transport protocol, such
 * as TCP
 * 
 * @author ran
 */
public class PathMtuExceeded extends IOException
{
	/**
	 * static instance. this instance is thrown when the thrower knows about
	 * the catcher, and the catcher does not need the stack trace or any other
	 * exception state, other than the fact that this exception was thrown.
	 */ 
	private static final PathMtuExceeded s_instance = new PathMtuExceeded();
	
	/**
	 * private constructor
	 */
	private PathMtuExceeded() {
		super();
	}
	
	/**
	 * throws the singleton instance
	 */
	public static void throwIt() throws PathMtuExceeded {
		throw s_instance;
	}
}
