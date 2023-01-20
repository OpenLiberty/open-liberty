/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.ras.instrument.internal.main;

/**
 * Processing exception.  Thrown to indicate that trace
 * injection of a class requires {@link #COMPUTE_FRAMES}.
 * 
 * This is a subtype of {@link RuntimeException} instead of
 * {@link Exception} because the exception must be thrown
 * through the ASM visitor API.
 */
public class ComputeRequiredException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public ComputeRequiredException(String msg, Throwable cause) {
		super(msg, cause);
	}	

	public ComputeRequiredException(String msg) {
		super(msg);
	}

	public ComputeRequiredException(Throwable cause) {
		super(cause);
	}	

	public ComputeRequiredException() {
		super();
	}		
}
