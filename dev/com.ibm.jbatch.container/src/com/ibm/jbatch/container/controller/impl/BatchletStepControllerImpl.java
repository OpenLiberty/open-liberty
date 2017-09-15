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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.runtime.BatchStatus;

import com.ibm.jbatch.container.artifact.proxy.BatchletProxy;
import com.ibm.jbatch.container.artifact.proxy.InjectionReferences;
import com.ibm.jbatch.container.artifact.proxy.ProxyFactory;
import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.container.execution.impl.RuntimeWorkUnitExecution;
import com.ibm.jbatch.container.execution.impl.RuntimeWorkUnitExecution.StopLock;
import com.ibm.jbatch.container.validation.ArtifactValidationException;
import com.ibm.jbatch.jsl.model.Batchlet;
import com.ibm.jbatch.jsl.model.Property;
import com.ibm.jbatch.jsl.model.Step;

public class BatchletStepControllerImpl extends SingleThreadedStepControllerImpl {

	private final static String sourceClass = BatchletStepControllerImpl.class.getName();
	private final static Logger logger = Logger.getLogger(sourceClass);

	private BatchletProxy batchletProxy;

	public BatchletStepControllerImpl(RuntimeWorkUnitExecution runtimeWorkUnitExecution, Step step) {
		super(runtimeWorkUnitExecution, step);
	}

	private void invokeBatchlet(Batchlet batchlet) throws BatchContainerServiceException {

		String batchletId = batchlet.getRef();
		List<Property> propList = (batchlet.getProperties() == null) ? null : batchlet.getProperties().getPropertyList();

		String sourceMethod = "invokeBatchlet";
		if (logger.isLoggable(Level.FINER)) {
			logger.entering(sourceClass, sourceMethod, batchletId);
		}

		InjectionReferences injectionRef = new InjectionReferences(runtimeWorkUnitExecution.getWorkUnitJobContext(), runtimeStepExecution, 
				propList);

		try {
			batchletProxy = ProxyFactory.createBatchletProxy(batchletId, injectionRef, runtimeStepExecution);
		} catch (ArtifactValidationException e) {
			throw new BatchContainerServiceException("Cannot create the batchlet [" + batchletId + "]", e);
		}

		if (logger.isLoggable(Level.FINE))
			logger.fine("Batchlet is loaded and validated: " + batchletProxy);

		if (wasStopIssuedOnJob()) {
			logger.fine("Exit without executing batchlet since stop() request has been received.");
		} else {
			// The lack of synchronization here implies a very small window in which a stop() can get called before a process(),
			// and the application must deal with that, if it wants to fail cleanly.  Ending up in stopped vs.
			// failed might not be a huge difference typically if you just want to restart next, but one
			// could imagine a case where it DOES matter.  OTOH, we can't just hold a lock and prevent stop while
			// process() runs, obviously.
			//
			// Consider adding statement to the SPEC.
			logger.fine("Starting process() for the Batchlet Artifact");
			String processRetVal = batchletProxy.process();

			logger.fine("Set process() return value = " + processRetVal + " for possible use as exitStatus");
			runtimeStepExecution.setBatchletProcessRetVal(processRetVal);

			logger.exiting(sourceClass, sourceMethod, processRetVal==null ? "<null>" : processRetVal);
		}
	}

	@Override
	protected void invokeCoreStep() throws BatchContainerServiceException {
		try {
			invokeBatchlet(getStep().getBatchlet());
		} finally {
			invokeCollectorIfPresent();
		}
	}

	@Override
	public void stop() {
		StopLock stopLock = getStopLock(); // Store in local variable to facilitate Ctrl+Shift+G search in Eclipse 
		synchronized (stopLock) {
			if ( isStepStartingOrStarted() ) {
				markStepStopping();
				// Possible for stop() to come before process().  See comment above.
				if (batchletProxy != null) {
					batchletProxy.stop();	
				}
			} else {
				// Might not be set up yet to have a state.
				logger.fine("Ignoring stop, since step not in a state which has a valid status (might not be far enough along to have a state yet)");
			}
		}
	}


}
