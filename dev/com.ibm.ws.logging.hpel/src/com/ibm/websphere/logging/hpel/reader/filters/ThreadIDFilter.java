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
package com.ibm.websphere.logging.hpel.reader.filters;

import com.ibm.websphere.logging.hpel.reader.LogRecordHeaderFilter;
import com.ibm.websphere.logging.hpel.reader.RepositoryLogRecordHeader;

/**
 * Implementation of the {@link LogRecordHeaderFilter} interface for filtering out
 * records not written by a thread with a given thread ID.
 * 
 * @ibm-api
 */
public class ThreadIDFilter implements LogRecordHeaderFilter {
	private final int threadID;

	/**
	 * Creates a filter instance with a specified thread ID.
	 * 
	 * @param threadID ID that each record's thread ID will be compared to
	 */
	public ThreadIDFilter(int threadID) {
		this.threadID = threadID;
	}

	/* (non-Javadoc)
	 * @see com.ibm.websphere.logging.hpel.reader.LogRecordHeaderFilter#accept(com.ibm.websphere.logging.hpel.reader.RepositoryLogRecordHeader)
	 */
	public boolean accept(RepositoryLogRecordHeader record) {
		return threadID>=0 && record.getThreadID() == threadID;
	}

}
