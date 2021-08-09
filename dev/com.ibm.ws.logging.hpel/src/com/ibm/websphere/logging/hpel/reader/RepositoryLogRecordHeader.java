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
package com.ibm.websphere.logging.hpel.reader;

import java.io.Serializable;
import java.util.logging.Level;

/**
 * The header of an individual record in the HPEL repository.
 * 
 * The RepositoryLogRecord is an extension of the RepositoryLogRecordHeader.
 * Some RepositoryReader implementations are able to load the 
 * RepositoryLogRecordHeader content without loading the rest of the
 * RepositoryLogRecord content.  This makes filtering based on 
 * RepositoryLogRecordHeader content more efficient than filtering that 
 * requires fields from other parts of the RepositoryLogRecord.
 * 
 * <p>
 * Many of the RepositoryLogRecordHeader's fields are modeled after identically
 * named fields in the java.util.logging.LogRecord class:
 * <ul>
 * <li><code>getLevel</code></li>
 * <li><code>getMillis</code></li>
 * <li><code>getThreadID</code></li>
 * </ul>
 * 
 * @ibm-api
 */
public interface RepositoryLogRecordHeader extends Serializable {
	/**
	 * Returns an identifier of the position of this record so that the record 
	 * can be relocated in subsequent queries.
	 * 
	 * @return a location in the repository
	 */
	public RepositoryPointer getRepositoryPointer();
	
	/**
	 * Returns the time at which this record was created expressed in milliseconds since 1970.
	 * 
	 * @return the time the record was created.
	 * @see java.util.logging.LogRecord#getMillis()
	 */
	public long getMillis(); 
	
	/**
	 * Returns the message level.
	 * <p>
	 * The level is an indication of the severity of the message.
	 * 
	 * @return the message level.
	 * @see java.util.logging.LogRecord#getLevel()
	 */
	public Level getLevel();
	
	/**
	 * Returns the ID of the thread on which this request was logged.
	 * <p>
	 * Note that this ID is based on the java.util.logging representation of
	 * the thread ID, and is not equivalent to the operating system 
	 * representation of the thread ID. 
	 * 
	 * @return the thread ID
	 * @see java.util.logging.LogRecord#getThreadID()
	 */
	public int getThreadID();
	
}
