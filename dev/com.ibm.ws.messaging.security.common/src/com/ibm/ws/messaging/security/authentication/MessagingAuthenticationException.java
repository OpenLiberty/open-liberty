/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.messaging.security.authentication;

import com.ibm.ws.messaging.security.MessagingSecurityException;

/**
 * Exception class for Messaging Authentication
 * @author Sharath Chandra B
 *
 */
public class MessagingAuthenticationException extends
		MessagingSecurityException {

	private static final long serialVersionUID = 1L;
	
	public MessagingAuthenticationException() {
		super();
	}
	
	public MessagingAuthenticationException(String message) {
		super(message);
	}
	
	public MessagingAuthenticationException(String message, Throwable throwable) {
		super(message, throwable);
	}
	
	MessagingAuthenticationException(Throwable throwable) {
		super(throwable);
	}

}
