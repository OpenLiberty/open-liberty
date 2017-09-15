/*
 * Copyright 2012 International Business Machines Corp.
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
package com.ibm.jbatch.container.controller.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.jbatch.container.IController;
import com.ibm.jbatch.container.artifact.proxy.InjectionReferences;
import com.ibm.jbatch.container.artifact.proxy.PartitionCollectorProxy;
import com.ibm.jbatch.container.artifact.proxy.ProxyFactory;
import com.ibm.jbatch.container.artifact.proxy.StepListenerProxy;
import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.container.execution.impl.RuntimeWorkUnitExecution;
import com.ibm.jbatch.container.validation.ArtifactValidationException;
import com.ibm.jbatch.container.ws.PartitionReplyMsg;
import com.ibm.jbatch.container.ws.PartitionReplyMsg.PartitionReplyMsgType;
import com.ibm.jbatch.jsl.model.Collector;
import com.ibm.jbatch.jsl.model.Property;
import com.ibm.jbatch.jsl.model.Step;

/**
 * 
 * When a partitioned step is run, this controller will only be used for the partition threads, 
 * NOT the top-level main thread that the step executes upon.
 * 
 * When a non-partitioned step is run this controller will be used as well (and there will be no
 * separate main thread with controller).
 *
 */
public abstract class SingleThreadedStepControllerImpl extends BaseStepControllerImpl implements IController {

	private final static Logger logger = Logger.getLogger(SingleThreadedStepControllerImpl.class.getName());

	/**
	 * User-supplied collector.
	 * Collector only used from partition threads, not top-level thread
	 */
	protected PartitionCollectorProxy collectorProxy = null;

	protected SingleThreadedStepControllerImpl(RuntimeWorkUnitExecution runtimeWorkUnitExecution, Step step) {
		super(runtimeWorkUnitExecution, step);
	}

	List<StepListenerProxy> stepListeners = null;
	
	
	/**
	 * @return true if this is a partitioned step
	 */
	protected boolean isPartitionedStep() {
	    return (getStep().getPartition() != null);
	}

	protected void setupStepArtifacts() {
		// set up listeners

		InjectionReferences injectionRef = new InjectionReferences(runtimeWorkUnitExecution.getWorkUnitJobContext(), runtimeStepExecution, null);
		this.stepListeners = runtimeWorkUnitExecution.getListenerFactory().getStepListeners(getStep(), injectionRef, runtimeStepExecution);

		// set up collectors if we are running a partitioned step
		if ( isPartitionedStep() ) {
			Collector collector = getStep().getPartition().getCollector();
			if (collector != null) {
				List<Property> propList = (collector.getProperties() == null) ? null : collector.getProperties().getPropertyList();
				/**
				 * Inject job flow, split, and step contexts into partition
				 * artifacts like collectors and listeners some of these
				 * contexts may be null
				 */
				injectionRef = new InjectionReferences(runtimeWorkUnitExecution.getWorkUnitJobContext(), runtimeStepExecution, propList);

				try {
					this.collectorProxy = ProxyFactory.createPartitionCollectorProxy(collector.getRef(), injectionRef, this.runtimeStepExecution);
				} catch (ArtifactValidationException e) {
					throw new BatchContainerServiceException("Cannot create the collector [" + collector.getRef() + "]", e);
				}
			}
		}
	}

	@Override
	protected void invokePreStepArtifacts() {
		// Don't call beforeStep() in the partitioned case, since we are now on a partition thread, and
		// have already called beforeStep() on the main thread as the spec says.
		if ((stepListeners != null) && !isSubJobPartitionThread()) {
			for (StepListenerProxy listenerProxy : stepListeners) {
				listenerProxy.beforeStep();
			}
		}
	}

	@Override
	protected void invokePostStepArtifacts() {
		// Don't call beforeStep() in the partitioned case, since we are now on a partition thread, and
		// have already called beforeStep() on the main thread as the spec says.
		if ((stepListeners != null) && !isSubJobPartitionThread()) {
			for (StepListenerProxy listenerProxy : stepListeners) {
				listenerProxy.afterStep();
			}
		}
	}

	/**
	 * Invoke the user-supplied PartitionCollectorProxy and send the data
	 * back to the top-level thread.
	 */
	protected void invokeCollectorIfPresent() {
		if (collectorProxy != null) {
			Serializable data = collectorProxy.collectPartitionData();
			logger.finer("Got partition data: " + data + ", from collector: " + collectorProxy);
			sendCollectorDataPartitionReplyMsg(data);
		} 
	}
	
	/**
	 * Send sub-job partition thread data back to top-level thread via analyzerStatusQueue.
	 */
	protected void sendCollectorDataPartitionReplyMsg(Serializable data) {

	    if (logger.isLoggable(Level.FINE)) {
	        logger.fine("Sending collector partition data: " + data + " to analyzer queue: " + getPartitionReplyQueue());
	    }
		
		PartitionReplyMsg msg = new PartitionReplyMsg( PartitionReplyMsgType.PARTITION_COLLECTOR_DATA )
			                                        .setCollectorData(serializeToByteArray(data));
		getPartitionReplyQueue().add(msg);
		    
		
	}
	
	private byte[] serializeToByteArray(Serializable data) {
	    byte[] retVal = null;
	    try {
	    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    	ObjectOutputStream oos = null;
	   		try {
	   			oos = new ObjectOutputStream(baos);
	   			oos.writeObject(data);
	   			retVal = baos.toByteArray();
	    	} catch (IOException e) {
	    		throw new IllegalStateException("Cannot serialize the message payload");
	   		}
	   		finally {
	   			oos.close();
			}
    	}catch (IOException e) {
    		throw new IllegalStateException("Cannot serialize the message payload");
			}
	    return retVal;
	}	
	
	
	

	/*
	 * This is now done in PartitionedThreadHelper.sendStatusToAnalyzer
	 * 
	@Override
	protected void sendStatusFromPartitionToAnalyzerIfPresent() {
	    // Useless to have collector without analyzer but let's check so we don't hang or blow up.
		if (analyzerStatusQueue != null) {
			logger.fine("Send status from partition for analyzeStatus with batchStatus = " + stepThreadInstance.getBatchStatus() + ", exitStatus = " + stepThreadInstance.getExitStatus());
			PartitionDataWrapper dataWrapper = new PartitionDataWrapper();
			dataWrapper.setBatchStatus(stepThreadInstance.getBatchStatus());
			dataWrapper.setExitStatus(stepThreadInstance.getExitStatus());
			dataWrapper.setEventType(PartitionEventType.ANALYZE_STATUS);
			analyzerStatusQueue.add(dataWrapper);
		} else {
			logger.fine("Analyzer not configured.");
		}
	}
	*/
}
