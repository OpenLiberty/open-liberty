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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.operations.JobExecutionAlreadyCompleteException;
import javax.batch.operations.JobExecutionNotMostRecentException;
import javax.batch.operations.JobExecutionNotRunningException;
import javax.batch.operations.JobRestartException;
import javax.batch.operations.JobStartException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.runtime.BatchStatus;

import com.ibm.jbatch.container.IExecutionElementController;
import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;
import com.ibm.jbatch.container.execution.impl.RuntimeSplitFlowExecution;
import com.ibm.jbatch.container.execution.impl.RuntimeWorkUnitExecution;
import com.ibm.jbatch.container.execution.impl.RuntimeWorkUnitExecution.StopLock;
import com.ibm.jbatch.container.impl.ParallelStepBuilder;
import com.ibm.jbatch.container.services.IBatchKernelService;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.servicesmanager.ServicesManagerStaticAnchor;
import com.ibm.jbatch.container.status.ExecutionStatus;
import com.ibm.jbatch.container.status.ExtendedBatchStatus;
import com.ibm.jbatch.container.status.SplitExecutionStatus;
import com.ibm.jbatch.container.util.BatchSplitFlowWorkUnit;
import com.ibm.jbatch.container.util.SplitFlowConfig;
import com.ibm.jbatch.container.ws.JoblogUtil;
import com.ibm.jbatch.container.ws.WSJobExecution;
import com.ibm.jbatch.container.ws.smf.ZosJBatchSMFLogging;
import com.ibm.jbatch.jsl.model.Flow;
import com.ibm.jbatch.jsl.model.JSLJob;
import com.ibm.jbatch.jsl.model.Split;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

public class SplitControllerImpl implements IExecutionElementController {

    private final static String sourceClass = SplitControllerImpl.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);

    private final RuntimeWorkUnitExecution runtimeWorkUnitExecution;

    private volatile List<BatchSplitFlowWorkUnit> splitFlowWorkUnits;

    private final BlockingQueue<BatchSplitFlowWorkUnit> completedWorkQueue = new LinkedBlockingQueue<BatchSplitFlowWorkUnit>();

    final List<JSLJob> subJobs = new ArrayList<JSLJob>();

    protected Split split;

    public SplitControllerImpl(RuntimeWorkUnitExecution jobExecution, Split split, long rootJobExecutionId) {
        this.runtimeWorkUnitExecution = jobExecution;
        this.split = split;
    }

    // Moving to a field to hold state across flow statuses.
    private ExtendedBatchStatus aggregateStatus = null;

    /**
     * @return the batch kernel
     */
    protected IBatchKernelService getBatchKernelService() {
        return ServicesManagerStaticAnchor.getServicesManager().getBatchKernelService();
    }

    /**
     * @return the SMF service
     */
    protected ZosJBatchSMFLogging getJBatchSMFLoggingService() {
        return ServicesManagerStaticAnchor.getServicesManager().getJBatchSMFService();
    }

    /**
     * @return the persistence manager
     */
    protected IPersistenceManagerService getPersistenceManagerService() {
        return ServicesManagerStaticAnchor.getServicesManager().getPersistenceManagerService();
    }

    @Override
    @FFDCIgnore(JobExecutionNotRunningException.class)
    public void stop() {
        StopLock stopLock = getStopLock(); // Store in local variable to facilitate Ctrl+Shift+G search in Eclipse
        synchronized (stopLock) {
            if (splitFlowWorkUnits != null) {
                for (BatchSplitFlowWorkUnit splitFlow : splitFlowWorkUnits) {
                    long workUnitExecutionId = -1;
                    try {
                        getBatchKernelService().stopWorkUnit(splitFlow);
                    } catch (JobExecutionNotRunningException e) {
                        logger.fine("Caught exception trying to stop work unit: " + workUnitExecutionId + ", which was not running.");
                        // We want to stop all running sub steps.
                        // We do not want to throw an exception if a sub step has already been completed.
                    } catch (Exception e) {
                        // Blow up if it happens to force the issue.
                        throw new IllegalStateException(e);
                    }
                }
            }
        }
    }

    // Since it's in the same JVM as top-level job, it's been notified and we can check in-memory w/o
    // needing to check the DB.
    private boolean isJobStopping() {
        return runtimeWorkUnitExecution.getBatchStatus().equals(BatchStatus.STOPPING);
    }

    @Override
    public SplitExecutionStatus execute() throws JobRestartException, JobStartException, JobExecutionAlreadyCompleteException, JobExecutionNotMostRecentException, NoSuchJobExecutionException {

        // Locking here has the downside that we prevent a stop from taking effect this whole time.
        // It may be redundant or pointless to submit them, but as long as the thread pool continually
        // expands to accomodate new work, it shouldn't be a big deal (as long as we don't block putting work
        // onto the executor queue).
        StopLock stopLock = getStopLock(); // Store in local variable to facilitate Ctrl+Shift+G search in Eclipse
        synchronized (stopLock) {
            if (isJobStopping()) {
                SplitExecutionStatus retVal = new SplitExecutionStatus();
                retVal.setExtendedBatchStatus(ExtendedBatchStatus.JOB_OPERATOR_STOPPING);
                return retVal;
            } else {
                // Build all sub jobs from partitioned step
                buildSubJobBatchWorkUnits();
                // kick off the threads
                executeWorkUnits();
            }
        }

        // It shouldn't matter at the split level if the job has been stopped.  The steps have their
        // own statuses beneath us, and above us we know just fine that the job's been stopped.
        return waitForCompletionAndAggregateStatus();
    }

    // Must be called after obtaining the stop lock.  Since this is a private method we don't worry much
    // about reobtaining.
    private void buildSubJobBatchWorkUnits() {

        List<Flow> flows = (List<Flow>) this.split.getFlows();

        splitFlowWorkUnits = new ArrayList<BatchSplitFlowWorkUnit>();

        for (Flow flow : flows) {
            // 1. First, we build the subjob JSLJob model for flows in split
            JSLJob splitFlowJSLJob = ParallelStepBuilder.buildFlowInSplitSubJob(runtimeWorkUnitExecution.getTopLevelInstanceId(), runtimeWorkUnitExecution.getWorkUnitJobContext(),
                                                                                this.split, flow);
            subJobs.add(splitFlowJSLJob);

            // 2. Next, we build a (persisted) execution and a work unit (thread) around it.
            SplitFlowConfig splitFlowConfig = new SplitFlowConfig(runtimeWorkUnitExecution.getTopLevelNameInstanceExecutionInfo(), split.getId(), flow.getId(), runtimeWorkUnitExecution.getCorrelationId());
            BatchSplitFlowWorkUnit workUnit = getBatchKernelService().createSplitFlowWorkUnit(splitFlowConfig, splitFlowJSLJob, completedWorkQueue);
            splitFlowWorkUnits.add(workUnit);
        }
    }

    private void executeWorkUnits() {

        // Validate counts
        /*
         * for (BatchSplitFlowWorkUnit work : splitFlowWorkUnits) {
         * String internalJobName = work.getRuntimeWorkUnitExecution().getWorkUnitInternalJobName();
         * int count = getBatchKernelService().getJobInstanceCount(internalJobName);
         * if (count > 1) {
         * throw new IllegalStateException("There is an inconsistency somewhere in the internal subjob creation: count of internal job name: " + internalJobName + ", count = " +
         * count);
         * }
         * }
         */

        // Then start or restart all subjobs in parallel
        for (BatchSplitFlowWorkUnit work : splitFlowWorkUnits) {
            getBatchKernelService().runSplitFlow(work);

            JoblogUtil.logToJobLogAndTraceOnly(Level.FINE, "flow.started", new Object[] {
                                                                                          work.getRuntimeWorkUnitExecution().getFlowName(),
                                                                                          runtimeWorkUnitExecution.getTopLevelInstanceId(),
                                                                                          runtimeWorkUnitExecution.getTopLevelExecutionId() },
                                               logger);

        }
    }

    private SplitExecutionStatus waitForCompletionAndAggregateStatus() {

        SplitExecutionStatus splitStatus = new SplitExecutionStatus();

        for (int i = 0; i < subJobs.size(); i++) {
            BatchSplitFlowWorkUnit batchWork;
            try {
                batchWork = completedWorkQueue.take(); //wait for each thread to finish and then look at it's status
            } catch (InterruptedException e) {
                throw new BatchContainerRuntimeException(e);
            }

            RuntimeSplitFlowExecution flowExecution = batchWork.getRuntimeWorkUnitExecution();
            ExecutionStatus flowStatus = flowExecution.getFlowStatus();

            if (flowStatus.getExtendedBatchStatus().equals(ExtendedBatchStatus.NORMAL_COMPLETION)) {
                ZosJBatchSMFLogging smflogger = getJBatchSMFLoggingService();
                logger.fine("cutting an smf flow end record");
                if (smflogger != null) {
                    WSJobExecution jobExecution = getPersistenceManagerService().getJobExecution(flowExecution.getTopLevelExecutionId());
                    int rc = smflogger.buildAndWriteFlowEndRecord(flowExecution,
                                                                  jobExecution,
                                                                  getPersistenceManagerService().getPersistenceType(),
                                                                  getPersistenceManagerService().getDisplayId());
                    logger.fine("back from calling native smf, rc = " + rc);
                }
            }

            aggregateTerminatingStatusFromSingleFlow(flowStatus, splitStatus);

            if (flowStatus.equals(ExtendedBatchStatus.JSL_FAIL) || flowStatus.equals(ExtendedBatchStatus.EXCEPTION_THROWN)) {
                JoblogUtil.logToJobLogAndTraceOnly(Level.WARNING, "flow.failed", new Object[] {
                                                                                                batchWork.getRuntimeWorkUnitExecution().getFlowName(),
                                                                                                runtimeWorkUnitExecution.getTopLevelInstanceId(),
                                                                                                runtimeWorkUnitExecution.getTopLevelExecutionId() },
                                                   logger);
            }

            JoblogUtil.logToJobLogAndTraceOnly(Level.FINE, "flow.ended", new Object[] {
                                                                                        batchWork.getRuntimeWorkUnitExecution().getFlowName(),
                                                                                        runtimeWorkUnitExecution.getTopLevelInstanceId(),
                                                                                        runtimeWorkUnitExecution.getTopLevelExecutionId() },
                                               logger);

        }

        // If this is still set to 'null' that means all flows completed normally without terminating the job.
        if (aggregateStatus == null) {
            logger.fine("Setting normal split status as no contained flows ended the job.");
            aggregateStatus = ExtendedBatchStatus.NORMAL_COMPLETION;
        }

        splitStatus.setExtendedBatchStatus(aggregateStatus);
        logger.fine("Returning from waitForCompletionAndAggregateStatus with return value: " + splitStatus);
        return splitStatus;
    }

    //
    // A <fail> and an uncaught exception are peers.  They each take precedence over a <stop>, which take precedence over an <end>.
    // Among peers the last one seen gets to set the exit stauts.
    //
    private void aggregateTerminatingStatusFromSingleFlow(ExecutionStatus flowStatus, SplitExecutionStatus splitStatus) {

        String exitStatus = flowStatus.getExitStatus();
        String restartOn = flowStatus.getRestartOn();
        ExtendedBatchStatus flowBatchStatus = flowStatus.getExtendedBatchStatus();

        logger.fine("Aggregating possible terminating status for flow ending with status: " + flowStatus
                    + ", restartOn = " + restartOn);

        if (flowBatchStatus.equals(ExtendedBatchStatus.JSL_END) || flowBatchStatus.equals(ExtendedBatchStatus.JSL_STOP) ||
            flowBatchStatus.equals(ExtendedBatchStatus.JSL_FAIL) || flowBatchStatus.equals(ExtendedBatchStatus.EXCEPTION_THROWN)) {
            if (aggregateStatus == null) {
                logger.fine("A flow detected as ended because of a terminating condition: " + flowBatchStatus.name()
                            + ". First flow detected in terminating state.  Setting exitStatus if non-null.");
                setInJobContext(flowBatchStatus, exitStatus, restartOn);
                aggregateStatus = flowBatchStatus;
            } else {
                splitStatus.setCouldMoreThanOneFlowHaveTerminatedJob(true);
                if (aggregateStatus.equals(ExtendedBatchStatus.JSL_END)) {
                    logger.fine("Current flow's batch and exit status will take precedence over and override earlier one from <end> transition element. " +
                                "Overriding, setting exit status if non-null and preparing to end job.");
                    setInJobContext(flowBatchStatus, exitStatus, restartOn);
                    aggregateStatus = flowBatchStatus;
                } else if (aggregateStatus.equals(ExtendedBatchStatus.JSL_STOP)) {
                    // Everything but an <end> overrides a <stop>
                    if (!(flowBatchStatus.equals(ExtendedBatchStatus.JSL_END))) {
                        logger.fine("Current flow's batch and exit status will take precedence over and override earlier one from <stop> transition element. " +
                                    "Overriding, setting exit status if non-null and preparing to end job.");
                        setInJobContext(flowBatchStatus, exitStatus, restartOn);
                        aggregateStatus = flowBatchStatus;
                    } else {
                        logger.fine("End does not override stop.  The flow with <end> will effectively be ignored with respect to terminating the job.");
                    }
                } else if (aggregateStatus.equals(ExtendedBatchStatus.JSL_FAIL) || aggregateStatus.equals(ExtendedBatchStatus.EXCEPTION_THROWN)) {
                    if (flowBatchStatus.equals(ExtendedBatchStatus.JSL_FAIL) || flowBatchStatus.equals(ExtendedBatchStatus.EXCEPTION_THROWN)) {
                        logger.fine("Current flow's batch and exit status will take precedence over and override earlier one from <fail> transition element or exception thrown. " +
                                    "Overriding, setting exit status if non-null and preparing to end job.");
                        setInJobContext(flowBatchStatus, exitStatus, restartOn);
                        aggregateStatus = flowBatchStatus;
                    } else {
                        logger.fine("End and stop do not override exception thrown or <fail>.   The flow with <end> or <stop> will effectively be ignored with respect to terminating the job.");
                    }
                }
            }
        } else {
            logger.fine("Flow completing normally without any terminating transition or exception thrown.");
        }
    }

    private void setInJobContext(ExtendedBatchStatus flowBatchStatus, String exitStatus, String restartOn) {
        if (exitStatus != null) {
            runtimeWorkUnitExecution.setExitStatus(exitStatus);
        }
        if (ExtendedBatchStatus.JSL_STOP.equals(flowBatchStatus)) {
            if (restartOn != null) {
                runtimeWorkUnitExecution.setRestartOnForNextExecution(restartOn);
            }
        }
    }

    public List<BatchSplitFlowWorkUnit> getParallelJobExecs() {
        return splitFlowWorkUnits;
    }

    @Override
    public List<Long> getLastRunStepExecutions() {

        List<Long> stepExecIdList = new ArrayList<Long>();

        for (BatchSplitFlowWorkUnit workUnit : splitFlowWorkUnits) {

            List<Long> stepExecIds = workUnit.getController().getLastRunStepExecutions();

            // Though this would have been one way to have a failure in a constituent flow
            // "bubble up" to a higher-level failure, let's not use this as the mechanism, so
            // it's clearer how our transitioning logic functions.
            if (stepExecIds != null) {
                stepExecIdList.addAll(stepExecIds);
            }
        }

        return stepExecIdList;
    }

    protected StopLock getStopLock() {
        return runtimeWorkUnitExecution.getStopLock();
    }
}
