/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package org.eclipse.openj9.criu;

/**
 * Abstract CRIU exception superclass. Contains an error code returned from a
 * failed operation.
 */
public abstract class JVMCRIUException extends RuntimeException {
	private static final long serialVersionUID = 4486137934620495516L;
	protected int errorCode;

	protected JVMCRIUException(String message, int errorCode) {
		super(message);
		this.errorCode = errorCode;
	}

	protected JVMCRIUException(String message, int errorCode, Throwable causedBy) {
		super(message, causedBy);
		this.errorCode = errorCode;
	}

	/**
	 * Returns the error code.
	 *
	 * @return errorCode
	 */
	public int getErrorCode() {
		return errorCode;
	}

	/**
	 * Sets the error code.
	 *
	 * @param errorCode the value to set to
	 */
	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}
}
