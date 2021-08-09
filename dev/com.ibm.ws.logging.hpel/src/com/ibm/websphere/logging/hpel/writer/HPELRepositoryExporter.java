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

import java.io.File;

import com.ibm.ws.logging.hpel.LogRepositorySubProcessCommunication;
import com.ibm.ws.logging.hpel.LogRepositoryWriter;
import com.ibm.ws.logging.hpel.impl.AbstractHPELRepositoryExporter;
import com.ibm.ws.logging.hpel.impl.LogRepositoryManagerImpl;
import com.ibm.ws.logging.hpel.impl.LogRepositorySubManagerImpl;
import com.ibm.ws.logging.hpel.impl.LogRepositoryWriterImpl;

/**
 * Implementation of the {@link RepositoryExporter} interface exporting log records in
 * a directory in HPEL formatted files.  The <code>storeHeader</code> method of the parent class must be called before
 * any records can be stored.  Each record is stored with the <code>storeRecord</code> function.  Failure to
 * follow the order will result in runtime exceptions.
 *
 * @ibm-api
 */
public class HPELRepositoryExporter extends AbstractHPELRepositoryExporter {
	private final File repositoryDir;

	/**
	 * Constructs an exporter which stores log records in HPEL format.
	 *
	 * @param repositoryDir export directory where repository log files will be created.
	 */
	public HPELRepositoryExporter(File repositoryDir) {
		this.repositoryDir = repositoryDir;
	}

	protected LogRepositoryWriter createWriter(String pid, String label) {
        //provide the repository directory, pid/labels to use as the subdirectory name, and true
        //to indicate that subdirectories should be created
		LogRepositoryManagerImpl logManager = new LogRepositoryManagerImpl(repositoryDir, pid, label, true);
		return new LogRepositoryWriterImpl(logManager);
	}
	
	private final static LogRepositorySubProcessCommunication DUMMY_COMMAGENT = new LogRepositorySubProcessCommunication() {
		@Override
		public boolean removeFiles(String destinationType) {
			return false;
		}
		@Override
		public String notifyOfFileCreation(String destinationType,
				long spTimeStamp, String spPid, String spLabel) {
			return null;
		}
		@Override
		public void inactivateSubProcess(String spPid) {
		}
	};

	@Override
	protected LogRepositoryWriter createSubWriter(String pid, String label,
			String superPid) {
		LogRepositorySubManagerImpl logManager = new LogRepositorySubManagerImpl(repositoryDir, pid, label, superPid);
		logManager.setSubProcessCommunicationAgent(DUMMY_COMMAGENT);
		return new LogRepositoryWriterImpl(logManager);
	}
}
