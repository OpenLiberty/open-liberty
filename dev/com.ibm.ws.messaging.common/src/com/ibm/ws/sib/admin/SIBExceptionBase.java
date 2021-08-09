
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
package com.ibm.ws.sib.admin;


public class SIBExceptionBase extends Exception {

	private static final long serialVersionUID = -7076891032889941247L;

	/**
	 * @see java.lang.Throwable#Throwable(String)
	 */
	public SIBExceptionBase(String msg) {
		super(msg);
	}

	/**
	 * @see java.lang.Throwable#Throwable(Throwable)
	 */
	public SIBExceptionBase(Throwable t) {
		super(t);
	}
}
