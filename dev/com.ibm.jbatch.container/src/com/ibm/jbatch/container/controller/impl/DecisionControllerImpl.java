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

import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.batch.runtime.StepExecution;

import com.ibm.jbatch.container.IExecutionElementController;
import com.ibm.jbatch.container.artifact.proxy.DeciderProxy;
import com.ibm.jbatch.container.artifact.proxy.InjectionReferences;
import com.ibm.jbatch.container.artifact.proxy.ProxyFactory;
import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;
import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.container.execution.impl.RuntimeWorkUnitExecution;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.servicesmanager.ServicesManagerStaticAnchor;
import com.ibm.jbatch.container.status.ExecutionStatus;
import com.ibm.jbatch.container.status.ExtendedBatchStatus;
import com.ibm.jbatch.container.validation.ArtifactValidationException;
import com.ibm.jbatch.container.ws.WSJobExecution;
import com.ibm.jbatch.container.ws.smf.ZosJBatchSMFLogging;
import com.ibm.jbatch.jsl.model.Decision;
import com.ibm.jbatch.jsl.model.Property;
import com.ibm.jbatch.jsl.model.helper.ExecutionElement;

public class DecisionControllerImpl implements IExecutionElementController {

    private final static String sourceClass = DecisionControllerImpl.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);

    private final RuntimeWorkUnitExecution jobExecution;

    private final Decision decision;

    private StepExecution[] previousStepExecutions = null;

    public DecisionControllerImpl(RuntimeWorkUnitExecution jobExecution, Decision decision) {
        this.jobExecution = jobExecution;
        this.decision = decision;
    }

    /**
     * @return the batch persistence service
     */
    public IPersistenceManagerService getPersistenceManagerService() {
        return ServicesManagerStaticAnchor.getServicesManager().getPersistenceManagerService();
    }

    /**
     * @return the SMF service
     */
    protected ZosJBatchSMFLogging getJBatchSMFLoggingService() {
        return ServicesManagerStaticAnchor.getServicesManager().getJBatchSMFService();
    }

    @Override
    public ExecutionStatus execute() {

        String deciderId = decision.getRef();
        List<Property> propList = (decision.getProperties() == null) ? null : decision.getProperties().getPropertyList();

        DeciderProxy deciderProxy;

        //Create a decider proxy and inject the associated properties

        /* Set the contexts associated with this scope */
        //job context is always in scope
        //the parent controller will only pass one valid context to a decision controller
        //so two of these contexts will always be null
        InjectionReferences injectionRef = new InjectionReferences(jobExecution.getWorkUnitJobContext(), null, propList);

        try {
            deciderProxy = ProxyFactory.createDeciderProxy(deciderId, injectionRef);
        } catch (ArtifactValidationException e) {
            throw new BatchContainerServiceException("Cannot create the decider [" + deciderId + "]", e);
        }

        byte[] timeUsedBefore = null;

        ZosJBatchSMFLogging smflogger = getJBatchSMFLoggingService();
        if (smflogger != null) {
            timeUsedBefore = smflogger.getTimeUsedData();
        }
        Date startTime = new Date();
        String exitStatus = deciderProxy.decide(this.previousStepExecutions);
        Date endTime = new Date();

        byte[] timeUsedAfter = null;

        if (smflogger != null) {
            timeUsedAfter = smflogger.getTimeUsedData();
        }

        logger.fine("Decider exiting and setting job-level exit status to " + exitStatus);

        //Set the value returned from the decider as the job context exit status.
        this.jobExecution.getWorkUnitJobContext().setExitStatus(exitStatus);

        if (smflogger != null) {
            WSJobExecution execution = getPersistenceManagerService().getJobExecution(jobExecution.getTopLevelExecutionId());
            getJBatchSMFLoggingService().buildAndWriteDeciderEndRecord(exitStatus,
                                                                       this.jobExecution,
                                                                       execution,
                                                                       decision.getRef(),
                                                                       getPersistenceManagerService().getPersistenceType(),
                                                                       getPersistenceManagerService().getDisplayId(),
                                                                       startTime,
                                                                       endTime,
                                                                       timeUsedBefore,
                                                                       timeUsedAfter);
        }

        return new ExecutionStatus(ExtendedBatchStatus.NORMAL_COMPLETION, exitStatus);
    }

    protected void setPreviousStepExecutions(ExecutionElement previousExecutionElement, IExecutionElementController previousElementController) {
        if (previousExecutionElement == null) {
            // only job context is available to the decider
        } else if (previousExecutionElement instanceof Decision) {

            throw new BatchContainerRuntimeException("A decision cannot precede another decision.");

        }

        List<Long> previousStepExecsIds = previousElementController.getLastRunStepExecutions();

        StepExecution[] stepExecArray = new StepExecution[previousStepExecsIds.size()];

        for (int i = 0; i < stepExecArray.length; i++) {
            StepExecution stepExec = getPersistenceManagerService().getStepExecutionTopLevel(previousStepExecsIds.get(i));
            stepExecArray[i] = stepExec;
        }

        this.previousStepExecutions = stepExecArray;

    }

    @Override
    public void stop() {
        // no-op
    }

    @Override
    public List<Long> getLastRunStepExecutions() {
        // TODO Auto-generated method stub
        return null;
    }

}
