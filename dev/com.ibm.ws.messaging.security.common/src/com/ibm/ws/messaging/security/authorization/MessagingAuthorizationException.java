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

package com.ibm.ws.messaging.security.authorization;

import com.ibm.ws.messaging.security.MessagingSecurityException;

/**
 * Exception class for Messaging Authorization
 * @author Sharath Chandra B
 *
 */
public class MessagingAuthorizationException extends MessagingSecurityException {

	private static final long serialVersionUID = 1L;
	
	public MessagingAuthorizationException() {
		super();
	}
	
	public MessagingAuthorizationException(String message) {
		super(message);
	}
	
	public MessagingAuthorizationException(String message, Throwable throwable) {
		super(message, throwable);
	}
	
	public MessagingAuthorizationException(Throwable throwable) {
		super(throwable);
	}

}
