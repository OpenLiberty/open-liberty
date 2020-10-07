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
package com.ibm.ws.sip.stack.util;

import java.io.IOException;

/**
 * Exception thrown by the container when the application is trying to
 * send out a request to a SIPS URI with a transport parameter of UDP.
 * 
 * For example:
 * INVITE sips:bob@biloxi.com;transport=udp SIP/2.0
 * 
 * @author ran
 */
public class AmbiguousUriException extends IOException
{
	/**
	 * unique serialization identifier
	 */
	private static final long serialVersionUID = -3467670075152106963L;

	/**
	 * no-arg constructor
	 */
	public AmbiguousUriException() {
		super();
	}

	/**
	 * constructor
	 * @param message the detail message
	 */
	public AmbiguousUriException(String message) {
		super(message);
	}
}
