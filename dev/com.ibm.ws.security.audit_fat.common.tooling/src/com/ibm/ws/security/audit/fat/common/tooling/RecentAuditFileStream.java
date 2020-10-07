/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.audit.fat.common.tooling;

import java.io.File;
/**
 * Provides lines from a file based audit stream where an AuditFileStream is used to manage the file.
 */
public class RecentAuditFileStream implements IAuditStream {

	private Long offset;
	protected AuditFileStream stream;
	private boolean doneOnce;
	
	/**
	 * Given an audit file log name, create a RecentAuditFileStream starting from the current end of log.
	 * The AuditFileStream is used internally to manage the file.
	 * @param logName
	 */
	public RecentAuditFileStream(String logName) {

		File logFile = new File(logName);
		this.offset = logFile.exists() ? logFile.length() : 0L;
		this.stream = new AuditFileStream(logName);	
	}
	/**
	 * For an audit file, return the next line starting at the offset. The offset is
	 * managed internally. The first time the file is read, the offset is used to determine
	 * the starting point for the read.
	 * 
	 * @throws Exception
	 */
	@Override
	public String readNext() throws Exception {
		if (!doneOnce){
			doneOnce = true;
			stream.skipAhead(offset);
		}
		return stream.readNext();
	}

}
