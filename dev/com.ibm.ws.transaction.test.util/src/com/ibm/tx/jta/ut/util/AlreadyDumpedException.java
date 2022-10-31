/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.tx.jta.ut.util;

/*
 *  This is here so it can be an allowed exception in tests
 */
@SuppressWarnings("serial")
public class AlreadyDumpedException extends RuntimeException {

	public AlreadyDumpedException(String message) {
		super(message);
	}
}