/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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
package com.ibm.ws.sip.container.annotation;

@SuppressWarnings("serial")
public class StopDeploymentException extends Exception {

	public StopDeploymentException() {
		super();
	}

	public StopDeploymentException(String message, Throwable cause) {
		super(message, cause);
	}

	public StopDeploymentException(String message) {
		super(message);
	}

	public StopDeploymentException(Throwable cause) {
		super(cause);
	}
}

