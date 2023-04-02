/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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

package com.ibm.ws.messaging.security;

/**
 * Exception class for MessagingSecurity component
 * @author Sharath Chandra B
 *
 */
public class MessagingSecurityException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public MessagingSecurityException(String message) {
		super(message);
	}
	
	public MessagingSecurityException(String message, Throwable throwable) {
		super(message, throwable);
	}
	
	public MessagingSecurityException(Throwable throwable) {
		super(throwable);
	}
	
	public MessagingSecurityException() {
		super();
	}

}
