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
 * Unchecked exception to wrap checked one thrown in methods without 'throws' clause.
 * 
 * @ibm-api
 */
public class LogRepositoryRuntimeException extends RuntimeException {
	private static final long serialVersionUID = 6213833785797600508L;

	/**
	 * constructs exception with the specified cause.
	 * 
	 * @param cause underlying LogRepositoryException
	 */
	public LogRepositoryRuntimeException(LogRepositoryException cause) {
		super(cause);
	}
}
