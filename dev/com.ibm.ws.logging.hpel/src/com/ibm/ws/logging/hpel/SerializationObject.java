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

import java.util.Properties;
import java.util.logging.LogRecord;

/**
 * Converter of log records and header information into byte arrays.
 */
public interface SerializationObject {
	/**
	 * Converts <code>header</code> into byte array.
	 * 
	 * @param header header information as a set of properties.
	 * @return byte array representing header information in a format maintained
	 * 		by this implementation.
	 */
	byte[] serializeFileHeader(Properties header);
	
	/**
	 * Converts log record into byte array.
	 * 
	 * @param record {@link LogRecord} instance to convert into bytes.
	 * @return byte array representing log record information in a format maintained
	 * 		by this implemention.
	 */
	byte[] serialize(LogRecord record);
}
