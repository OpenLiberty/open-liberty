/*******************************************************************************
 * Copyright (c) 2009, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.hpel.impl;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.logging.hpel.reader.RepositoryLogRecord;
import com.ibm.websphere.logging.hpel.reader.ServerInstanceLogRecordList;
import com.ibm.websphere.logging.hpel.writer.HPELRepositoryExporter;
import com.ibm.websphere.logging.hpel.writer.RepositoryExporter;
import com.ibm.ws.logging.hpel.LogRecordSerializer;
import com.ibm.ws.logging.hpel.LogRepositoryWriter;

/**
 * Abstract implementation of the {@link RepositoryExporter} interface providing functionality
 * common to all HPEL based exporters.
 */
public abstract class AbstractHPELRepositoryExporter implements
		RepositoryExporter {
	private final static String BUNDLE_NAME = "com.ibm.ws.logging.hpel.resources.HpelMessages";
	private final static String className = HPELRepositoryExporter.class.getName();
	private final static Logger logger = Logger.getLogger(className, BUNDLE_NAME);
	private final static String UNKNOWN_LABEL = "unknown";

	private LogRecordSerializer lrs = LogRepositoryManagerImpl.KNOWN_FORMATTERS[0];
	private LogRepositoryWriter logWriter = null;
	private ByteArrayOutputStream buffer = new ByteArrayOutputStream(1024);
	private boolean isClosed = false; // value "true" indicates that exporter was already closed
	private String parentId = null; // Process Id of the controller. Value "null" means that storeHeader wasn't issued yet.

	public void storeHeader(Properties header) {
		storeHeader(header, null);
	}
	
	public void storeHeader(Properties header, String subProcess) {
		if (header == null) {
			throw new IllegalArgumentException("Argument 'header' cannot be null.");
		}
		if (isClosed) {
			throw new IllegalStateException("This instance of the exporter is already closed");
		}
		if (subProcess != null && parentId == null) {
			throw new IllegalStateException("This instance of the exporter does not have parent header information yet");
		}
		if (logWriter != null) {
			logWriter.stop();
		}
		if (subProcess == null) {
			String pid = header.getProperty(ServerInstanceLogRecordList.HEADER_PROCESSID);
			 //On iSeries platforms, the pid will be a qualified combination of job number, user, and jobname with a
		     //qualifier of /.  The job number is a unique number assigned by the iSeries system that is always 6 digits.
	      	//Since job number is unique and numeric, we will strip off the rest and treat the job number as a pid value.
            int index = pid.indexOf("/");
			if(index > -1 ){
				pid = pid.substring(0, index);			
			}
			if (pid == null) {
				logger.logp(Level.SEVERE, className, "storeHeader", "HPEL_HeaderWithoutProcessId", ServerInstanceLogRecordList.HEADER_PROCESSID);
				logWriter = null;
				return;
			}

			String label;
			if ("Y".equalsIgnoreCase(header.getProperty(ServerInstanceLogRecordList.HEADER_ISZOS))) {
				label = header.getProperty(ServerInstanceLogRecordList.HEADER_JOBNAME, UNKNOWN_LABEL);
				String jobId = header.getProperty(ServerInstanceLogRecordList.HEADER_JOBID);
				if (jobId != null) {
					label += LogRepositoryBaseImpl.TIMESEPARATOR + jobId;
				}
			} else {
				//the header will contain the full server name that includes cell and node info
				String fullServerName = header.getProperty(ServerInstanceLogRecordList.HEADER_SERVER_NAME);
				//Parse the server name out of the full name to use as a label
				if(fullServerName != null){
					int begIndex = fullServerName.lastIndexOf("\\");

					if( begIndex > -1 && begIndex+1 < fullServerName.length()){
						label = fullServerName.substring(begIndex+1);
					} else {
						label = fullServerName;
					}
				} else {
					label = UNKNOWN_LABEL;
				}
			}

			logWriter = createWriter(pid, label);

			parentId = pid;
		} else {
			String pid = LogRepositoryBaseImpl.parseProcessID(subProcess);
			String label = LogRepositoryBaseImpl.parseLabel(subProcess);

			logWriter = createSubWriter(pid, label, parentId);
		}
		buffer.reset();
		try {
			DataOutputStream dataStream = new DataOutputStream(buffer);
			lrs.serializeFileHeader(header, dataStream);
			dataStream.flush();
		} catch (IOException e) {
			logger.logp(Level.SEVERE, className, "storeHeader", "HPEL_ErrorWhileSerializingHeader", e);
			// Most probably memory related issues. Use an empty header.
			buffer.reset();
		}
		logWriter.setHeader(buffer.toByteArray());
	}

	public void storeRecord(RepositoryLogRecord record) {
		if (isClosed) {
			throw new IllegalStateException("This instance of the exporter is already closed");
		}
		if (parentId == null) {
			throw new IllegalStateException("This instance of the exporter does not have header information yet");
		}
		if (record == null) {
			throw new IllegalArgumentException("Argument 'record' cannot be null.");
		}
		if (logWriter == null) {
			logger.logp(Level.SEVERE, className, "storeRecord", "HPEL_LogHeaderWasNotSet");
			return;
		}
		buffer.reset();
		try {
			DataOutputStream dataStream = new DataOutputStream(buffer);
			lrs.serialize(record, dataStream);
			dataStream.flush();
		} catch (IOException e) {
			logger.logp(Level.SEVERE, className, "storeRecord", "HPEL_ErrorWhileSerializingRecord", e);
			// Most probably memory related issues. Use an empty record.
			buffer.reset();
		}
		logWriter.logRecord(record.getMillis(), buffer.toByteArray());
	}

	public void close() {
		if (logWriter != null) {
			logWriter.stop();
			logWriter = null;
		}
		isClosed = true;
	}

	/**
	 * Creates and returns new repository writer.
	 *
	 * @param pid process Id of the original process writing log records.
	 * @param label label for writers that write to subdirectories.
	 *
	 * @return repository writer to write new set of log records.
	 */
	protected abstract LogRepositoryWriter createWriter(String pid, String label);
	
	/**
	 * Creates and returns new sub process repository writer.
	 *
	 * @param pid process Id of the original process writing log records.
	 * @param label label for writers that write to subdirectories.
	 * @param superPid process Id of the parent process
	 *
	 * @return repository writer to write new set of log records.
	 */
	protected abstract LogRepositoryWriter createSubWriter(String pid, String label, String superPid);

}
