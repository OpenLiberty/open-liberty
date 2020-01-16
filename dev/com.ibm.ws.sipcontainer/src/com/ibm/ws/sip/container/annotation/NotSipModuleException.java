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
package com.ibm.ws.sip.container.annotation;

@SuppressWarnings("serial")
public class NotSipModuleException extends Exception {

	public NotSipModuleException() {
		super();
	}

	public NotSipModuleException(String message, Throwable cause) {
		super(message, cause);
	}

	public NotSipModuleException(String message) {
		super(message);
	}

	public NotSipModuleException(Throwable cause) {
		super(cause);
	}
}
