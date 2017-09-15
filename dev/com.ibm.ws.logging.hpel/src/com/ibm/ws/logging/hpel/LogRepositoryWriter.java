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

/**
 * Log repository writer with assigned log record level.
 */
public interface LogRepositoryWriter {
	/**
	 * Sets header information for this writer.
	 * 
	 * @param headerBytes header information as a byte array.
	 */
	public void setHeader(byte[] headerBytes);
	
	/**
	 * Publishes log record with this writer.
	 * 
	 * @param timestamp the time of the log record.
	 * @param bytes record information as a byte array.
	 */
	public void logRecord(long timestamp, byte[] bytes);
	
	/**
	 * Returns manager used by this writer.
	 *
	 * @return manager configured during construction of this writer.
	 */
	public LogRepositoryManager getLogRepositoryManager();
	
	/**
	 * Stops this writer and releases resources held by it.
	 */
	public void stop();
}
