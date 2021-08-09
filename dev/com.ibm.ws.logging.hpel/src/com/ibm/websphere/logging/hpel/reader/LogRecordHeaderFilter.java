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

/**
 * A filter to select log records based on fields available from the {@link RepositoryLogRecordHeader}.
 * 
 * @ibm-api
 */
public interface LogRecordHeaderFilter {
	/**
	 * Checks if record should be accepted into the list.
	 * 
	 * @param record log record header to check
	 * @return <code>true</code> if record should be included in the list;
	 * 			<code>false</code> otherwise.
	 */
	boolean accept(RepositoryLogRecordHeader record);
}
