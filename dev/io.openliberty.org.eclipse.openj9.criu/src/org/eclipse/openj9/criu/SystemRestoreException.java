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
 * A CRIU exception representing a failure after restore.
 */
public final class SystemRestoreException extends JVMCRIUException {
	private static final long serialVersionUID = 1539393473417716292L;

	/**
	 * Creates a RestoreException with the specified message and error code.
	 *
	 * @param message   the message
	 * @param errorCode the error code
	 */
	public SystemRestoreException(String message, int errorCode) {
		super(message, errorCode);
	}

	/**
	 * Creates a RestoreException with the specified message and error code.
	 *
	 * @param message   the message
	 * @param errorCode the error code
	 * @param causedBy  throwable that cuased the exception
	 */
	public SystemRestoreException(String message, int errorCode, Throwable causedBy) {
		super(message, errorCode, causedBy);
	}
}
