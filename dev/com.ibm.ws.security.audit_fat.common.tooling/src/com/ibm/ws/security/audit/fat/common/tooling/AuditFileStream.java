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

import java.io.BufferedReader;
import java.io.FileReader;


/**
 * Provides a file based audit stream.
 */
public class AuditFileStream implements IAuditStream {

	private String logName ;
	protected BufferedReader buffer;

	/**
	 * Give an audit log file name, create AuditFileStream for reading from the log.
	 * @param logName - name of the audit log file.
	 */
	public AuditFileStream (String logName){
		this.logName = logName;
	}
	
	/**
	 * Check that the audit file is open and if not already open, then open it.
	 * Catch and rethrow any exception from opening the file.
	 * @throws Exception
	 */
	private void ensureOpen() throws Exception {
	 if (this.buffer == null)	{
		 this.buffer =  new BufferedReader(new FileReader(logName));
	 }
	}

	/**
	 * For an audit file, return the next line from the file.
	 * If the audit file is not open, then open the file for reading.
	 * If the file is empty, close the file.
	 * Catch and rethrow exceptions from opening/closing the file.
	 * 
	 * @throws Exception
	 */
	@Override
	public String readNext() throws Exception {
		ensureOpen();

		try {
			String line = this.buffer.readLine();
			if (line == null) 
				closeBuffer();
			return line;        


        } catch (Exception e) {

            closeBuffer();
            throw e;
        }
	}

	/**
	 * Close buffered reader for audit file.
	 */
	private void closeBuffer() {
		try {
		    this.buffer.close();
		    
		} catch (Exception e2) {
		    e2.printStackTrace();
		}
	}
	
	/**
	 * Given an offset, skip ahead to this offset in the AuditFileStream.
	 * @param offset - offset into audit file
	 * @throws Exception
	 */
	 public void skipAhead(long offset) throws Exception {
		ensureOpen();
		buffer.skip(offset);
	 }
}
