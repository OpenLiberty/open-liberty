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

import java.util.regex.Pattern;

import com.ibm.websphere.logging.hpel.reader.LogRecordFilter;
import com.ibm.websphere.logging.hpel.reader.RepositoryLogRecord;

/**
 * Implementation of the {@link LogRecordFilter} interface for filtering out
 * records not written by a logger with a matching name.
 * 
 * @ibm-api
 */
public class LoggerNameFilter implements LogRecordFilter {
	private final Pattern pattern;
	
	/**
	 * Creates a filter instance for matching logger names using a specified regular expression.
	 * 
	 * 
 	 * @param namePattern regular expression {@link Pattern} that each record's
 	 *                    logger name will be compared to
	 */
	public LoggerNameFilter(String namePattern) {
		this.pattern = Pattern.compile(namePattern==null ? ".*" : namePattern);
	}

	/* (non-Javadoc)
	 * @see com.ibm.websphere.logging.hpel.reader.LogRecordFilter#accept(com.ibm.websphere.logging.hpel.reader.RepositoryLogRecord)
	 */
	public boolean accept(RepositoryLogRecord record) {
		return pattern.matcher(record.getLoggerName()).find();
	}

}
