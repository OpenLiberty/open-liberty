/*
 * Copyright 2012,2015 International Business Machines Corp.
 * 
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.ibm.jbatch.container.persistence;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.api.chunk.CheckpointAlgorithm;

import com.ibm.jbatch.container.artifact.proxy.ItemReaderProxy;
import com.ibm.jbatch.container.artifact.proxy.ItemWriterProxy;
import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;
import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.container.persistence.jpa.StepThreadInstanceEntity;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.ws.JoblogUtil;


public class CheckpointManager {
	private final static String sourceClass = CheckpointManager.class.getName();
	private final static Logger logger = Logger.getLogger(sourceClass);

	private ItemReaderProxy readerProxy = null;
	private ItemWriterProxy writerProxy = null;
	int commitInterval = 0;
	private CheckpointAlgorithm checkpointAlgorithm;
	private long executionId = 0;
	private StepThreadInstanceEntity stepThreadInstance = null;
	
    /**
     * CTOR, manually injected with IPersistenceManagerService.
     */
	public CheckpointManager(ItemReaderProxy reader, 
                             ItemWriterProxy writer,
                             CheckpointAlgorithm chkptAlg,
			                 StepThreadInstanceEntity stepThreadInstance, 
                             IPersistenceManagerService persistenceManagerService) {
		this.readerProxy = reader;
		this.writerProxy = writer;
		this.checkpointAlgorithm = chkptAlg;
		this.stepThreadInstance = stepThreadInstance; 
	}

	public void beginCheckpoint() {
		try {
			checkpointAlgorithm.beginCheckpoint();
		} catch (Exception e) {
			throw new BatchContainerRuntimeException("Checkpoint algorithm failed", e);
		}
	}
	
	public void endCheckpoint() {
		try {
			checkpointAlgorithm.endCheckpoint();
		} catch (Exception e) {
			throw new BatchContainerRuntimeException("Checkpoint algorithm failed", e);
		}
	}


	public boolean isReadyToCheckpoint() {
		String method = "isReadyToCheckpoint";
		if(logger.isLoggable(Level.FINER)) { logger.entering(sourceClass, method); }

		boolean checkpoint = false;
		
		try {
			checkpoint = checkpointAlgorithm.isReadyToCheckpoint();
		} catch (Exception e) {
		    throw new BatchContainerRuntimeException("Checkpoint algorithm failed", e);
		}

		if (logger.isLoggable(Level.FINE) && checkpoint)
			logger.fine("isReadyToCheckpoint - " + checkpoint);

		if(logger.isLoggable(Level.FINER)) { logger.exiting(sourceClass, method);}
		
		return checkpoint;
	}

	public void checkpoint() {
		String method = "checkpoint";
		if(logger.isLoggable(Level.FINER)) { logger.entering(sourceClass, method, " [executionId " + executionId + "] "); }

		CheckpointData data  = null;
		try{
			data  = new CheckpointData();
			data.setReaderCheckpoint(readerProxy.checkpointInfo());
			data.setWriterCheckpoint(writerProxy.checkpointInfo());
			stepThreadInstance.setCheckpointData(data);
		}
		catch (Exception ex){
			throw new BatchContainerServiceException("Cannot persist the checkpoint data for " + stepThreadInstance);
		}

		if(logger.isLoggable(Level.FINER)) { logger.exiting(sourceClass, method, " [executionId " + executionId + "] ");}

	}
	
	public int checkpointTimeout() {
		
		int returnTimeout = 0; 

        try {
            returnTimeout = this.checkpointAlgorithm.checkpointTimeout();
        } catch (Exception e) {
            throw new BatchContainerRuntimeException("Checkpoint algorithm checkpointTimeout() failed", e);
        }

        return returnTimeout;
	}

}
