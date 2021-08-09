/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.hpel;

import java.io.IOException;

/**
 * Exception thrown due to an inconsistency found during reading log files.
 */
public class DeserializerException extends IOException {
	private static final long serialVersionUID = -9017682593255710738L;

	/**
	 * Creates SerializerException instance
	 * 
	 * @param msg description of the mismatch location.
	 * @param expected the value expected during deserialization.
	 * @param actual the value read during deserialization.
	 */
	public DeserializerException(String msg, String expected, String actual) {
		super(msg + " expected: " + expected + " actual: " + actual);
	}
}
