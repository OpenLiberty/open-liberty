/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.logging.hpel.reader;

/**
 * Checked exception thrown in API methods in case of an error.
 * 
 * @ibm-api
 */
public class LogRepositoryException extends Exception {
	private static final long serialVersionUID = -4997826722025129896L;

	/**
	 * constructs exception with the specified message.
	 * 
	 * @param message details of the problem.
	 */
	public LogRepositoryException(String message) {
		super(message);
	}

	/**
	 * constructs exception with the specified cause.
	 * 
	 * @param cause cause for throwing this exception.
	 */
	public LogRepositoryException(Throwable cause) {
		super(cause);
	}

	/**
	 * constructs exception with the specified message and cause.
	 * 
	 * @param message details of the problem.
	 * @param cause cause for throwing this exception.
	 */
	public LogRepositoryException(String message, Throwable cause) {
		super(message, cause);
	}

}
