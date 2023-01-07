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
 * A CRIU exception representing a failure in the JVM before checkpoint.
 */
public final class JVMCheckpointException extends JVMCRIUException {
	private static final long serialVersionUID = 4486137934620495516L;

	/**
	 * Creates a JVMCheckpointException with the specified message and error code.
	 *
	 * @param message   the message
	 * @param errorCode the error code
	 */
	public JVMCheckpointException(String message, int errorCode) {
		super(message, errorCode);
	}

	/**
	 * Creates a CheckpointException with the specified message and error code.
	 *
	 * @param message   the message
	 * @param errorCode the error code
	 * @param causedBy  throwable that cuased the exception
	 */
	public JVMCheckpointException(String message, int errorCode, Throwable causedBy) {
		super(message, errorCode, causedBy);
	}
}
