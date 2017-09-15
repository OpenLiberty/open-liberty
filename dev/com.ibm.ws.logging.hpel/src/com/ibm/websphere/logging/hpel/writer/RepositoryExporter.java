/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.logging.hpel.writer;

import java.util.Properties;

import com.ibm.websphere.logging.hpel.reader.RepositoryLogRecord;
import com.ibm.websphere.logging.hpel.reader.ServerInstanceLogRecordList;

/**
 * Interface for exporting read repository records into another repository.
 * 
 * @ibm-api
 */
public interface RepositoryExporter {
	/**
	 * Writes header information into exported repository. This call starts export of
	 * a new server instance.
	 * 
	 * @param header Header information related to all consequent log records.
	 */
	public void storeHeader(Properties header);
	
	/**
	 * Writes header information into exported sub process repository. This call starts export
	 * of a new sub process instance. It should be called after all records of the main process
	 * were exported with {@link #storeRecord(RepositoryLogRecord)}.
	 * 
	 * @param header Header information related to all consequent log records.
	 * @param subProcess String identifier of the sub process. Use the key corresponding to the
	 * sub process used in {@link ServerInstanceLogRecordList#getChildren()} map.
	 */
	public void storeHeader(Properties header, String subProcess);
	
	
	/**
	 * Writes log record into exported repository. Calling this method before {@link #storeHeader(Properties)}
	 * will result in {@link IllegalStateException} being thrown.
	 * 
	 * @param record log record to be exported.
	 */
	public void storeRecord(RepositoryLogRecord record);
	
	/**
	 * Finishes writing exported repository and closes all open resources.
	 * Calling either {@link #storeHeader(Properties)} or {@link #storeRecord(RepositoryLogRecord)}
	 * after calling {@link #close()} will result in {@link IllegalStateException} being thrown.
	 */
	public void close();
}
