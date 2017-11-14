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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.operations.JobExecutionAlreadyCompleteException;
import javax.batch.operations.JobExecutionNotMostRecentException;
import javax.batch.operations.JobRestartException;
import javax.batch.operations.JobStartException;
import javax.batch.runtime.BatchStatus;

import com.ibm.jbatch.container.IExecutionElementController;
import com.ibm.jbatch.container.context.impl.MetricImpl;
import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;
import com.ibm.jbatch.container.execution.impl.RuntimePartitionExecution;
import com.ibm.jbatch.container.execution.impl.RuntimeStepExecution;
import com.ibm.jbatch.container.execution.impl.RuntimeWorkUnitExecution;
import com.ibm.jbatch.container.execution.impl.RuntimeWorkUnitExecution.StopLock;
import com.ibm.jbatch.container.jsl.ModelSerializer;
import com.ibm.jbatch.container.jsl.ModelSerializerFactory;
import com.ibm.jbatch.container.persistence.jpa.StepThreadExecutionEntity;
import com.ibm.jbatch.container.persistence.jpa.StepThreadInstanceEntity;
import com.ibm.jbatch.container.persistence.jpa.StepThreadInstanceKey;
import com.ibm.jbatch.container.persistence.jpa.TopLevelStepInstanceEntity;
import com.ibm.jbatch.container.persistence.jpa.TopLevelStepInstanceKey;
import com.ibm.jbatch.container.services.IBatchKernelService;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.servicesmanager.ServicesManagerStaticAnchor;
import com.ibm.jbatch.container.status.ExecutionStatus;
import com.ibm.jbatch.container.status.ExtendedBatchStatus;
import com.ibm.jbatch.container.ws.JoblogUtil;
import com.ibm.jbatch.container.ws.PartitionReplyMsg;
import com.ibm.jbatch.container.ws.PartitionReplyMsg.PartitionReplyMsgType;
import com.ibm.jbatch.container.ws.PartitionReplyQueue;
import com.ibm.jbatch.container.ws.WSStepThreadExecutionAggregate;
import com.ibm.jbatch.container.ws.events.BatchEventsPublisher;
import com.ibm.jbatch.container.ws.smf.ZosJBatchSMFLogging;
import com.ibm.jbatch.jsl.model.JSLProperties;
import com.ibm.jbatch.jsl.model.Property;
import com.ibm.jbatch.jsl.model.Step;
import com.ibm.jbatch.spi.services.ITransactionManagementService;
import com.ibm.jbatch.spi.services.TransactionManagerAdapter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/** Change the name of this class to something else!! Or change BaseStepControllerImpl. */
public abstract class BaseStepControllerImpl implements IExecutionElementController {

    private final static Logger logger = Logger.getLogger(BaseStepControllerImpl.class.getName());

    byte[] timeUsedBefore = null;

    protected RuntimeWorkUnitExecution runtimeWorkUnitExecution;

    /**
     * Created under startStep().
     */
    protected RuntimeStepExecution runtimeStepExecution;

    /**
     * The JSL model for the step.
     */
    private final Step step;

    /**
     * Created first thing under execute()
     */
    private StepThreadInstanceEntity stepThreadInstance;
    private StepThreadExecutionEntity stepThreadExecution;

    /**
     * This is used by sub-job partition threads to send data back to the top-level thread.
     * The top-level thread waits on this queue in PartitionedStepControllerImpl.
     * PartitionedStepControllerImpl creates the queue, passes it to the sub-job partitions.
     */
    private PartitionReplyQueue partitionReplyQueue;

    /**
     * Created under startStep()
     */
    private TransactionManagerAdapter transactionManager;

    /**
     * Created in CTOR.
     */
    private final StepThreadHelper stepThreadHelper;

    /**
     * markStepFailed might get called more than once.
     */
    private boolean issuedFailureMessageToJobLog = false;

    /**
     * CTOR.
     */
    protected BaseStepControllerImpl(RuntimeWorkUnitExecution runtimeWorkUnitExecution, Step step) {
        this.runtimeWorkUnitExecution = runtimeWorkUnitExecution;

        if (step == null) {
            throw new IllegalArgumentException("Step parameter to ctor cannot be null. " + runtimeWorkUnitExecution);
        }

        this.step = step;
        this.stepThreadHelper = createStepHelper();
    }

    /**
     * Called by PartitionedStepControllerImpl.setupStepArtifacts
     *
     * @return this
     */
    protected BaseStepControllerImpl setPartitionReplyQueue(PartitionReplyQueue partitionReplyQueue) {
        this.partitionReplyQueue = partitionReplyQueue;
        return this;
    }

    /**
     * @return the partitionreplyqueue
     */
    protected PartitionReplyQueue getPartitionReplyQueue() {
        return partitionReplyQueue;
    }

    /**
     * @return the top-level job instance id.
     */
    protected long getJobInstanceId() {
        return runtimeWorkUnitExecution.getTopLevelInstanceId();
    }

    /**
     * @return the top-level job execution id.
     */
    protected long getJobExecutionId() {
        return runtimeWorkUnitExecution.getTopLevelExecutionId();
    }

    /**
     * @return the step name.
     */
    protected String getStepName() {
        return getStep().getId();
    }

    /**
     * @return the step
     */
    protected Step getStep() {
        return step;
    }

    /**
     * @return the stepThreadInstance
     */
    protected StepThreadInstanceEntity getStepThreadInstance() {
        return stepThreadInstance;
    }

    /**
     * @return the persistence manager
     */
    protected IPersistenceManagerService getPersistenceManagerService() {
        return ServicesManagerStaticAnchor.getServicesManager().getPersistenceManagerService();
    }

    /**
     * @return the SMF service
     */
    protected ZosJBatchSMFLogging getJBatchSMFLoggingService() {
        return ServicesManagerStaticAnchor.getServicesManager().getJBatchSMFService();
    }

    /**
     * @return the batch kernel
     */
    protected IBatchKernelService getBatchKernelService() {
        return ServicesManagerStaticAnchor.getServicesManager().getBatchKernelService();
    }

    /**
     * @return the tran service
     */
    protected ITransactionManagementService getTransactionManagementService() {
        return ServicesManagerStaticAnchor.getServicesManager().getTransactionManagementService();
    }

    /**
     * @return the TransactionManagerAdapter
     */
    protected TransactionManagerAdapter getTransactionManager() {
        return transactionManager;
    }

    /**
     * @return the batch event publisher
     */
    protected BatchEventsPublisher getBatchEventsPublisher() {
        return ServicesManagerStaticAnchor.getServicesManager().getBatchEventsPublisher();
    }

    ///////////////////////////
    // ABSTRACT METHODS ARE HERE
    ///////////////////////////
    protected abstract void invokeCoreStep() throws JobRestartException, JobStartException, JobExecutionAlreadyCompleteException, JobExecutionNotMostRecentException;

    protected abstract void setupStepArtifacts();

    protected abstract void invokePreStepArtifacts();

    protected abstract void invokePostStepArtifacts();

    @Override
    @FFDCIgnore(DoNotRestartStepThreadException.class)
    public ExecutionStatus execute() {

        // Here we're just setting up to decide if we're going to run the step or not (if it's already complete and
        // allow-start-if-complete=false.
        try {
            try {
                stepThreadExecution = createStepExecutionIfStepShouldBeExecuted();

                // Helper convenience object bridging application-facing StepContext and persistent store StepExecution
                runtimeStepExecution = new RuntimeStepExecution(stepThreadExecution);
            } catch (DoNotRestartStepThreadException e) {
                // Don't fail job in this case.  This is a "normal" path and the exception is just a convenient
                // flow of control tool.
                String prevExitStatus = stepThreadInstance.getLatestStepThreadExecution().getExitStatus();
                logger.fine("Not going to run this step.  Returning previous exit status of: " + prevExitStatus);
                return new ExecutionStatus(ExtendedBatchStatus.DO_NOT_RUN, prevExitStatus);
            }
        } catch (Throwable t) {
            // Treat an error at this point as unrecoverable, so fail job too.
            markJobOnlyForFailure();
            throw new BatchContainerRuntimeException("Caught throwable while determining if step should be executed.  Failing job.", t);
        }

        // At this point we have a StepExecution.  Setup so that we're ready to invoke artifacts.
        StopLock stopLock = getStopLock(); // Store in local variable to facilitate Ctrl+Shift+G search in Eclipse
        synchronized (stopLock) {
            if (wasStopIssuedOnJob()) {
                cleanupOnStepStoppedBeforeCoreExecution();
                return new ExecutionStatus(ExtendedBatchStatus.JOB_OPERATOR_STOPPING);
            }

            try {

                // Start CPU time here for single step (or top-level step) and partitions
                ZosJBatchSMFLogging smflogger = getJBatchSMFLoggingService();
                if (smflogger != null) {
                    timeUsedBefore = smflogger.getTimeUsedData();
                }
                startStep();
                stepThreadHelper.publishEvent(getPersistenceManagerService().getStepExecutionAggregate(runtimeStepExecution.getTopLevelStepExecutionId()),
                                              runtimeStepExecution.getBatchStatus());
            } catch (Throwable t) {
                // We haven't even run app code, so treat an error at this point as unrecoverable, so fail job too.
                markJobAndStepForFailure();
                throw new BatchContainerRuntimeException("Caught throwable while starting step. Failing job.", t);
            }
        }

        // Let go of the lock and re-obtain.
        // This block may be overkill
        synchronized (stopLock) {
            if (wasStopIssuedOnJob()) {
                cleanupOnStepStoppedBeforeCoreExecution();
                return new ExecutionStatus(ExtendedBatchStatus.JOB_OPERATOR_STOPPING);
            }
        }

        // At this point artifacts are in the picture so we want to try to invoke afterStep() on a failure.
        try {
            invokePreStepArtifacts(); //Call PartitionReducer and StepListener(s)
            invokeCoreStep();
        } catch (Throwable t) {
            JoblogUtil.logToJobLogAndTraceOnly(Level.SEVERE, "exception.executing.step", new Object[] { stepThreadExecution.getStepName(), getExceptionString(t) }, logger);
            markStepForFailure();
        }

        //
        // At this point we may have already failed or stopped the step, but we still try to
        // invoke the end of step artifacts.
        //
        try {
            //Call PartitionAnalyzer, PartitionReducer and StepListener(s)
            invokePostStepArtifacts();
        } catch (Throwable t) {
            JoblogUtil.logToJobLogAndTraceOnly(Level.SEVERE, "exception.after.step", new Object[] { stepThreadExecution.getStepName(), getExceptionString(t) }, logger);
            markStepForFailure();
        }

        //
        // No more application code is on the path from here on out (excluding the call to the PartitionAnalyzer
        // analyzeStatus().  If an exception bubbles up and leaves the statuses inconsistent or incorrect then so be it;
        // maybe there's a runtime bug that will need to be fixed.
        //
        synchronized (stopLock) {
            try {
                // Now that all step-level artifacts have had a chance to run,
                // we set the exit status to one of the defaults if it is still unset.

                // This is going to be the very last sequence of calls from the step running on the main thread,
                // since the call back to the partition analyzer only happens on the partition threads.
                // On the partition threads, then, we harden the status at the partition level before we
                // send it back to the main thread.
                transitionToFinalBatchStatus();
                defaultExitStatusIfNecessary();
                persistStepExecutionOnEnd();
            } catch (Throwable t) {

                //NOTE:  No need to worry about partition reply message, this is handled at the Thread.run() level.

                // If we've just caught a Throwable it was likely on persisting the StepExecution, and
                // we're likely to also fail trying a second time now.  Still, having a nicely marked
                // (with FAILED) status is worth it enough to try, so let's write some job log entries
                // and try to persist again.

                RuntimeException exceptionToLog = null;

                try {
                    exceptionToLog = new BatchContainerRuntimeException("Failure ending step execution", t);
                } catch (Throwable t1) {
                    // Ignore, if job logging fails at this point not a big deal in the scheme of things.
                }

                try {
                    markJobAndStepForFailure();
                } catch (Throwable t1) {
                    // Ignore, if job logging fails at this point not a big deal in the scheme of things.
                }

                try {
                    stepThreadHelper.publishEvent(getPersistenceManagerService().getStepExecutionAggregate(runtimeStepExecution.getTopLevelStepExecutionId()),
                                                  runtimeStepExecution.getBatchStatus());
                } catch (Throwable t1) {
                    // Ignore, not much we can do at this point.
                }

                try {
                    persistStepExecutionOnEnd();
                } catch (Throwable t1) {
                    // Ignore, not much we can do at this point.  A bit more comprehensible to throw back the exception we've written to the job log,
                    // though it shouldn't be too different than throwing 't1' here.  Limiting cascading exceptions.
                    JoblogUtil.logToJobLogAndTrace(Level.SEVERE, "error.persisting.stepExecution", new Object[] { getExceptionString(exceptionToLog) }, logger);
                    throw exceptionToLog;
                }

            }
        }

        // If this is a sub-job partition, then the final status msg is sent here.
        stepThreadHelper.sendFinalPartitionReplyMsg();

        try {
            stepThreadHelper.publishEvent(getPersistenceManagerService().getStepExecutionAggregate(runtimeStepExecution.getTopLevelStepExecutionId()),
                                          runtimeStepExecution.getBatchStatus());
        } catch (Throwable t) {
            // Might be nice to have an NLS message.  FFDC is perhaps enough.
            // Error handling is confusing enough without allowing a publish failure to change the flow of control perhaps.
        }

        if (runtimeStepExecution.getBatchStatus().equals(BatchStatus.FAILED)) {
            // We've already written the logFailedMessage()
            return new ExecutionStatus(ExtendedBatchStatus.EXCEPTION_THROWN, runtimeStepExecution.getExitStatus());
        } else {
            stepThreadHelper.logEndedMessage();
            return new ExecutionStatus(ExtendedBatchStatus.NORMAL_COMPLETION, runtimeStepExecution.getExitStatus());
        }
    }

    private void cleanupOnStepStoppedBeforeCoreExecution() {
        updateStepBatchStatus(BatchStatus.STOPPED);
        defaultExitStatusIfNecessary();
        persistStepExecutionOnEnd();
        stepThreadHelper.sendFinalPartitionReplyMsg();
    }

    protected void persistStepExecutionOnEnd() {
        Date endTime = new Date();
        runtimeStepExecution.setLastUpdatedTime(endTime);
        runtimeStepExecution.setEndTime(endTime);
        updateStepExecution();
    }

    private void defaultExitStatusIfNecessary() {
        String stepExitStatus = runtimeStepExecution.getExitStatus();
        String processRetVal = runtimeStepExecution.getBatchletProcessRetVal();
        if (stepExitStatus != null) {
            logger.fine("Returning with user-set exit status: " + stepExitStatus);
        } else if (processRetVal != null) {
            logger.fine("Returning with exit status from batchlet.process(): " + processRetVal);
            runtimeStepExecution.setExitStatus(processRetVal);
        } else {
            logger.fine("Returning with default exit status");
            runtimeStepExecution.setExitStatus(runtimeStepExecution.getBatchStatus().name());
        }
    }

    /**
     * Updates in-memory status and issues message to job log, but does not persist final status.
     *
     * Possible this routine gets called more than once, e.g. we might fail during normal step
     * execution, then also fail in afterStep().
     */
    protected void markStepForFailure() {
        StopLock stopLock = getStopLock(); // Store in local variable to facilitate Ctrl+Shift+G search in Eclipse
        synchronized (stopLock) {
            updateStepBatchStatus(BatchStatus.FAILED);
            if (!issuedFailureMessageToJobLog) {
                stepThreadHelper.logFailedMessage();
                issuedFailureMessageToJobLog = true;
            }
        }
    }

    // Will be overridden for partitioned steps by special method which
    // aggregates partition-level metrics
    protected void updateStepExecution() {
        getPersistenceManagerService().updateStepExecution(runtimeStepExecution);
    }

    protected void markJobAndStepForFailure() {
        StopLock stopLock = getStopLock(); // Store in local variable to facilitate Ctrl+Shift+G search in Eclipse
        synchronized (stopLock) {
            runtimeWorkUnitExecution.setBatchStatus(BatchStatus.FAILED);
            markStepForFailure();
        }
    }

    // Useful if there is no step execution yet
    protected void markJobOnlyForFailure() {
        StopLock stopLock = getStopLock(); // Store in local variable to facilitate Ctrl+Shift+G search in Eclipse
        synchronized (stopLock) {
            runtimeWorkUnitExecution.setBatchStatus(BatchStatus.FAILED);
        }
    }

    protected StopLock getStopLock() {
        return runtimeWorkUnitExecution.getStopLock();
    }

    private void validateStartLimitNotExceeded() {

        // The spec default is '0', which we get by initializing to '0' in the next line
        int startLimit = 0;
        String startLimitString = step.getStartLimit();
        if (startLimitString != null) {
            try {
                startLimit = Integer.parseInt(startLimitString);
            } catch (NumberFormatException e) {
                // We want to fail the job since the (substituted) JSL is invalid
                throw new IllegalArgumentException("Could not parse start limit value.  Received NumberFormatException for start-limit value:  " + startLimitString
                                                   + " for stepId: " + getStepName() + ", with start-limit=" + step.getStartLimit());
            }
        }

        if (startLimit < 0) {
            throw new IllegalArgumentException("Found negative start-limit of " + startLimit + "for stepId: " + getStepName());
        }

        if (startLimit > 0) {
            int newStepStartCount = ((TopLevelStepInstanceEntity) stepThreadInstance).getStartCount() + 1;
            if (newStepStartCount > startLimit) {
                // Per the spec, we want this to fail the job, so don't merely throw DoNotRestartStepThreadException
                throw new IllegalStateException("For stepId: " + getStepName() + ", tried to start step for the " + newStepStartCount
                                                + " time, but startLimit = " + startLimit);
            } else {
                logger.fine("Starting (possibly restarting) step: " + getStepName() + ", since newStepStartCount = " + newStepStartCount
                            + " and startLimit=" + startLimit);
            }
        }
    }

    private void startStep() {

        // log to job log
        runtimeWorkUnitExecution.logStepExecutionCreatedMessage(runtimeStepExecution);

        //Set Step context properties
        setContextProperties();

        //Set up step artifacts like step listeners, partition reducers
        setupStepArtifacts();

        // Move batch status to started.
        Date startTime = new Date();
        runtimeStepExecution.setLastUpdatedTime(startTime);
        runtimeStepExecution.setStartTime(startTime);
        updateStepBatchStatus(BatchStatus.STARTED);

        updateStepExecution();

        stepThreadHelper.logStartedMessage();
    }

    /**
     * We need this because the stop can come in at the job level, while the step-level constructs
     * are still getting set up.
     *
     * @return true if jobexecution is STOPPING.
     */
    protected boolean wasStopIssuedOnJob() {
        StopLock stopLock = getStopLock(); // Store in local variable to facilitate Ctrl+Shift+G search in Eclipse
        synchronized (stopLock) {
            if (runtimeWorkUnitExecution.getBatchStatus().equals(BatchStatus.STOPPING)) {
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * @return True if the current status is either STARTING or STARTED.
     *         False in any other case, including the case that we haven't yet made it to starting.
     */
    protected boolean isStepStartingOrStarted() {

        if (runtimeStepExecution == null) {
            return false;
        }

        BatchStatus currentStatus = runtimeStepExecution.getBatchStatus();

        if (BatchStatus.STARTED.equals(currentStatus) || BatchStatus.STARTING.equals(currentStatus))
            return true;
        else
            return false;
    }

    /**
     * The only valid states at this point are STARTED,STOPPING, or FAILED.
     * been able to get to STOPPED, or COMPLETED yet at this point in the code.
     *
     */
    private void transitionToFinalBatchStatus() {
        StopLock stopLock = getStopLock(); // Store in local variable to facilitate Ctrl+Shift+G search in Eclipse
        synchronized (stopLock) {
            BatchStatus currentBatchStatus = runtimeStepExecution.getBatchStatus();
            if (currentBatchStatus.equals(BatchStatus.STARTED)) {
                updateStepBatchStatus(BatchStatus.COMPLETED);
            } else if (currentBatchStatus.equals(BatchStatus.STOPPING)) {
                updateStepBatchStatus(BatchStatus.STOPPED);
            } else if (currentBatchStatus.equals(BatchStatus.FAILED)) {
                updateStepBatchStatus(BatchStatus.FAILED); // Should have already been done but maybe better for possible code refactoring to have it here.
            } else {
                throw new IllegalStateException("Step batch status should not be in a " + currentBatchStatus.name() + " state");
            }
        }
    }

    protected void updateStepBatchStatus(BatchStatus updatedBatchStatus) {
        if (runtimeStepExecution != null) {
            runtimeStepExecution.setBatchStatus(updatedBatchStatus);
        }
    }

    protected void markRestartAfterCompletion() {
        // No-op in base class
    }

    /**
     * stepThreadInstance and stepThreadExecution are either retrieved from the DB,
     * or if they don't exist yet, they're created.
     *
     * @return stepThreadExecution
     */
    protected StepThreadExecutionEntity createStepExecutionIfStepShouldBeExecuted() throws DoNotRestartStepThreadException {

        StepThreadExecutionEntity newStepExecution = null;

        StepThreadInstanceKey stepThreadInstanceKey = getStepThreadInstanceKey();
        stepThreadInstance = getPersistenceManagerService().getStepThreadInstance(stepThreadInstanceKey);

        // Note that for a partition thread, we're assuming we won't find a partition-level step thread instance
        // in the case that the top-level step is restarting after completing (i.e. in the allow-start-if-complete="true" case).
        // We assume that something at the top-level will remove these step thread instances so that we don't see them.

        if (stepThreadInstance == null) {
            logger.finer("No existing step instance found.  Create new step execution and proceed to execution.");
            newStepExecution = stepThreadHelper.createStepThreadInstanceAndFirstExecution(stepThreadInstanceKey, runtimeWorkUnitExecution.isRemotePartitionDispatch());
            stepThreadInstance = getPersistenceManagerService().getStepThreadInstance(stepThreadInstanceKey);
            if (stepThreadInstance == null) {
                throw new IllegalStateException("Should have just created step thread instance.");
            }
        } else {
            logger.finer("Existing step instance found.");
            newStepExecution = stepThreadHelper.setupStepThreadExecutionForRestartIfNecessary(runtimeWorkUnitExecution.isRemotePartitionDispatch());
        }

        return newStepExecution;
    }

    private void setContextProperties() {
        JSLProperties jslProps = step.getProperties();

        if (jslProps != null) {
            for (Property property : jslProps.getPropertyList()) {
                Properties contextProps = runtimeStepExecution.getJSLProperties();
                contextProps.setProperty(property.getName(), property.getValue());
            }
        }

        // set up metrics
        runtimeStepExecution.addMetric(MetricImpl.MetricType.READ_COUNT, 0);
        runtimeStepExecution.addMetric(MetricImpl.MetricType.WRITE_COUNT, 0);
        runtimeStepExecution.addMetric(MetricImpl.MetricType.READ_SKIP_COUNT, 0);
        runtimeStepExecution.addMetric(MetricImpl.MetricType.PROCESS_SKIP_COUNT, 0);
        runtimeStepExecution.addMetric(MetricImpl.MetricType.WRITE_SKIP_COUNT, 0);
        runtimeStepExecution.addMetric(MetricImpl.MetricType.FILTER_COUNT, 0);
        runtimeStepExecution.addMetric(MetricImpl.MetricType.COMMIT_COUNT, 0);
        runtimeStepExecution.addMetric(MetricImpl.MetricType.ROLLBACK_COUNT, 0);

        transactionManager = getTransactionManagementService().getTransactionManager(runtimeStepExecution);
    }

    @Override
    public List<Long> getLastRunStepExecutions() {
        // The signature calls for returning a list, since the call can be from a job-level <decision>
        // element which must structurally account for a transition coming from a split (with one
        // "last step" for each flow).

        // From this (step-level) perspective, we are only going to have one most-recent
        // StepExecution (for a top-level thread).
        List<Long> stepExecIdList = new ArrayList<Long>(1);
        Long lastStepExecId = stepThreadHelper.getLastRunStepExecutionId();
        if (lastStepExecId != null) {
            stepExecIdList.add(lastStepExecId);
        }
        return stepExecIdList;
    }

    protected void markStepStopping() {
        StopLock stopLock = getStopLock(); // Store in local variable to facilitate Ctrl+Shift+G search in Eclipse
        synchronized (stopLock) {
            updateStepBatchStatus(BatchStatus.STOPPING);
        }
    }

    @Override
    public String toString() {
        return "BaseStepControllerImpl for step = " + getStepName();
    }

    /**
     * @return true if the runtimeWorkUnitExecution is an instance of RuntimePartitionExecution,
     *         which indicates that this is a sub-job partition thread.
     */
    protected boolean isSubJobPartitionThread() {
        return (runtimeWorkUnitExecution instanceof RuntimePartitionExecution);
    }

    /**
     * @return the runtimeWorkUnitExecution casted as a RuntimePartitionExecution
     */
    private RuntimePartitionExecution getRuntimePartitionExecution() {
        return (RuntimePartitionExecution) runtimeWorkUnitExecution;
    }

    /**
     * @return Either a TopLevelStepInstanceKey or StepThreadInstanceKey (for sub-job partitions)
     */
    protected StepThreadInstanceKey getStepThreadInstanceKey() {
        return isSubJobPartitionThread() ? new StepThreadInstanceKey(getJobInstanceId(), getStepName(), getRuntimePartitionExecution().getPartitionNumber()) : new TopLevelStepInstanceKey(getJobInstanceId(), getStepName());
    }

    /**
     * Note that the "top-level" cases includes steps running in a split-flow, since from the step level,
     * a step running in a split-flow looks exactly the same as a step running outside of one... in both cases
     * the step itself runs on one thread (though in a split-flow that might be in a different
     * thread than other steps in the job).
     *
     * @return PartitionThreadHelper (for sub-job partitions) or TopLevelThreadHelper (everything else).
     *
     *         TODO: might need a new type here, to handle multiJVM case (e.g. sendStatusToAnalyzer)
     */
    private StepThreadHelper createStepHelper() {
        if (isSubJobPartitionThread()) {
            return new PartitionThreadHelper();
        } else {
            return new TopLevelThreadHelper();
        }
    }

    //
    // This answers the question:  "Is the top-level step a partitioned step", not
    // "is this a top-level thread or a partitioned thread of a partitioned step".
    //
    // The reason we rely on this method instead of the model is this question is
    // only significant for the top-level thread.   From the partition thread, the answer
    // is obvious.  But we don't want to have a dummy piece of data that's meaningless or at
    // face value incorrect on the partition thread OR we don't want to have to worry
    // about setting this flag correctly on the partition thread, even though it won't
    // be used.
    protected boolean isTopLevelStepThreadOfPartitionedStep() {
        return false;
    }

    // Use as a tool for flow of control.  Private to this class.
    private class DoNotRestartStepThreadException extends Exception {
        private static final long serialVersionUID = 1L;

        public DoNotRestartStepThreadException() {
            super();
        }
    }

    /**
     * Abstraction for differences of behavior between normal job threads ("top-level")
     * and sub-job partition threads.
     */
    private interface StepThreadHelper {

        /**
         *
         * @param instanceKey
         * @param isRemoteDispatch Only relevant for Partition threads. It is redundant in case of top-level thread
         * @return
         */
        public StepThreadExecutionEntity createStepThreadInstanceAndFirstExecution(StepThreadInstanceKey instanceKey, boolean isRemoteDispatch);

        public void logStartedMessage();

        public void logEndedMessage();

        public void logFailedMessage();

        /**
         * Sub-job partition threads send their batchStatus/exitStatus back to
         * the top-level thread. Top-level threads do nothing in this method.
         */
        public void sendFinalPartitionReplyMsg();

        public Long getLastRunStepExecutionId();

        /**
         *
         * @param isRemoteDispatch Only relevant for Partition threads. It is redundant in case of top-level thread
         * @return
         * @throws DoNotRestartStepThreadException
         */
        public StepThreadExecutionEntity setupStepThreadExecutionForRestartIfNecessary(boolean isRemoteDispatch) throws DoNotRestartStepThreadException;

        public void publishEvent(WSStepThreadExecutionAggregate objectToPublish, BatchStatus batchStatus);
    }

    /**
     * For sub-job partition threads.
     */
    private class PartitionThreadHelper implements StepThreadHelper {

        @Override
        public StepThreadExecutionEntity setupStepThreadExecutionForRestartIfNecessary(boolean isRemoteDispatch) throws DoNotRestartStepThreadException {

            // Check if it's been previously completed, and only restart if it's not been completed
            BatchStatus stepBatchStatus = stepThreadInstance.getLatestStepThreadExecution().getBatchStatus();
            if (stepBatchStatus.equals(BatchStatus.COMPLETED)) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Step: " + getStepName() + ", partition + " + getRuntimePartitionExecution().getPartitionNumber()
                                + " already has batch status of COMPLETED, so won't be run again.");
                }
                throw new DoNotRestartStepThreadException();
            } else {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Found previous batchStatus of " + stepBatchStatus + ", so re-execute step: " + getStepName() + ", partition + "
                                + getRuntimePartitionExecution().getPartitionNumber());
                }
                return getPersistenceManagerService().createPartitionStepExecutionOnRestartFromPreviousStepInstance(getJobExecutionId(), stepThreadInstance, isRemoteDispatch);
            }
        }

        @Override
        public Long getLastRunStepExecutionId() {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("This is a meaningless call for a partition thread, easier for our impl to just make it and ignore.");
            }
            return null;
        }

        @Override
        public void sendFinalPartitionReplyMsg() {

            logger.fine("Send status from partition for analyzeStatus with batchStatus = " + runtimeStepExecution.getBatchStatus() + ", exitStatus = "
                        + runtimeStepExecution.getExitStatus());

            // Final msg for partition being sent, cut smf record for partition ended
            if (runtimeStepExecution.getBatchStatus() == BatchStatus.COMPLETED ||
                runtimeStepExecution.getBatchStatus() == BatchStatus.FAILED ||
                runtimeStepExecution.getBatchStatus() == BatchStatus.STOPPED) {
                ZosJBatchSMFLogging smflogger = getJBatchSMFLoggingService();
                if (smflogger != null) {
                    if (timeUsedBefore != null) {
                        byte[] timeUsedAfter = smflogger.getTimeUsedData(); // end cpu time for partition
                        int rc = smflogger.buildAndWritePartitionEndRecord(getRuntimePartitionExecution(),
                                                                           stepThreadExecution.getTopLevelStepExecution().getJobExecution(),
                                                                           runtimeStepExecution,
                                                                           getPersistenceManagerService().getPersistenceType(),
                                                                           getPersistenceManagerService().getDisplayId(),
                                                                           timeUsedBefore,
                                                                           timeUsedAfter);
                    } else {
                        if (logger.isLoggable(Level.FINE)) {
                            logger.fine("Won't log SMF record since before time was never captured.  Probably step was stopped before execution.");
                        }
                    }
                }
            }

            PartitionReplyMsg msg = new PartitionReplyMsg(PartitionReplyMsgType.PARTITION_FINAL_STATUS).setBatchStatus(runtimeStepExecution.getBatchStatus()).setExitStatus(runtimeStepExecution.getExitStatus()).setPartitionPlanConfig(((RuntimePartitionExecution) runtimeWorkUnitExecution).getPartitionPlanConfig());
            getPartitionReplyQueue().add(msg);
            getPartitionReplyQueue().close();
            ((RuntimePartitionExecution) runtimeWorkUnitExecution).setFinalStatusSent(true);
        }

        @Override
        public StepThreadExecutionEntity createStepThreadInstanceAndFirstExecution(StepThreadInstanceKey instanceKey, boolean isRemoteDispatch) {
            return getPersistenceManagerService().createPartitionStepExecutionAndNewThreadInstance(getJobExecutionId(), instanceKey, isRemoteDispatch);
        }

        @Override
        public void logStartedMessage() {
            JoblogUtil.logToJobLogAndTraceOnly(Level.FINE, "partition.started", new Object[] {
                                                                                               getRuntimePartitionExecution().getPartitionNumber(),
                                                                                               getStepName(),
                                                                                               getJobInstanceId(),
                                                                                               getJobExecutionId() },
                                               logger);
            ModelSerializer<Step> ms = ModelSerializerFactory.createStepModelSerializer();
            String prettyXml = ms.prettySerializeModel(step);
            JoblogUtil.logToJobLogAndTraceOnly(Level.INFO, "display.resolved.jsl", new Object[] { "partition", prettyXml }, logger);

        }

        @Override
        public void logEndedMessage() {
            JoblogUtil.logToJobLogAndTraceOnly(Level.FINE, "partition.ended", new Object[] {
                                                                                             getRuntimePartitionExecution().getPartitionNumber(),
                                                                                             runtimeStepExecution.getBatchStatus(), // Log the step-level, not the "work unit" level
                                                                                             runtimeStepExecution.getExitStatus(),
                                                                                             getStepName(),
                                                                                             getJobInstanceId(),
                                                                                             getJobExecutionId() },
                                               logger);
        }

        @Override
        public void logFailedMessage() {
            //If this is a partitioned thread log some additional info
            JoblogUtil.logToJobLogAndTraceOnly(Level.WARNING, "partition.failed", new Object[] {
                                                                                                 getRuntimePartitionExecution().getPartitionNumber(),
                                                                                                 runtimeStepExecution.getBatchStatus(), // Log the step-level, not the "work unit" level
                                                                                                 runtimeStepExecution.getExitStatus(),
                                                                                                 getStepName(),
                                                                                                 getJobInstanceId(),
                                                                                                 getJobExecutionId() },
                                               logger);
        }

        @Override
        public void publishEvent(WSStepThreadExecutionAggregate objectToPublish, BatchStatus batchStatus) {}

    }

    /**
     * For top-level job threads (i.e. non-sub-job-partition threads).
     */
    private class TopLevelThreadHelper implements StepThreadHelper {

        @Override
        public StepThreadExecutionEntity setupStepThreadExecutionForRestartIfNecessary(boolean isRemoteDispatch) throws DoNotRestartStepThreadException {

            TopLevelStepInstanceEntity topLevelStepInstance = (TopLevelStepInstanceEntity) stepThreadInstance;

            boolean restartAfterCompletion = false;

            BatchStatus stepBatchStatus = topLevelStepInstance.getLatestStepThreadExecution().getBatchStatus();
            if (stepBatchStatus.equals(BatchStatus.COMPLETED)) {
                // A bit of parsing involved since the model gives us a String not a
                // boolean, but it should default to 'false', which is the spec'd default.
                if (!Boolean.parseBoolean(getStep().getAllowStartIfComplete())) {
                    logger.fine("Step: " + getStepName() + " already has batch status of COMPLETED, so won't be run again since it does not allow start if complete.");
                    throw new DoNotRestartStepThreadException();
                } else {
                    logger.fine("Step: " + getStepName() + " already has batch status of COMPLETED, and allow-start-if-complete is set to 'true'");
                    // Save the fact that the step has already completed, but we'll see if we've reached the start-limit before actually restarting.
                    restartAfterCompletion = true;
                }
            }

            validateStartLimitNotExceeded();

            if (restartAfterCompletion) {

                markRestartAfterCompletion();

                return getPersistenceManagerService().createTopLevelStepExecutionOnRestartAndCleanStepInstance(getJobExecutionId(), topLevelStepInstance);
            } else {
                return getPersistenceManagerService().createTopLevelStepExecutionOnRestartFromPreviousStepInstance(getJobExecutionId(), topLevelStepInstance);
            }
        }

        @Override
        public Long getLastRunStepExecutionId() {
            return stepThreadInstance.getLatestStepThreadExecution().getStepExecutionId();
        }

        @Override
        public void sendFinalPartitionReplyMsg() {
            // no-op
        }

        @Override
        public StepThreadExecutionEntity createStepThreadInstanceAndFirstExecution(StepThreadInstanceKey instanceKey, boolean isRemoteDispatch) {
            return getPersistenceManagerService().createTopLevelStepExecutionAndNewThreadInstance(getJobExecutionId(), instanceKey, isTopLevelStepThreadOfPartitionedStep());
        }

        @Override
        public void logEndedMessage() {
            JoblogUtil.logToJobLogAndTraceOnly(Level.FINE, "step.ended", new Object[] {
                                                                                        runtimeStepExecution.getStepName(),
                                                                                        runtimeStepExecution.getBatchStatus(),
                                                                                        runtimeStepExecution.getExitStatus(),
                                                                                        getJobInstanceId(),
                                                                                        getJobExecutionId() },
                                               logger);
        }

        @Override
        public void logFailedMessage() {
            JoblogUtil.logToJobLogAndTraceOnly(Level.WARNING, "step.failed", new Object[] {
                                                                                            runtimeStepExecution.getStepName(),
                                                                                            runtimeStepExecution.getBatchStatus(),
                                                                                            runtimeStepExecution.getExitStatus(),
                                                                                            getJobInstanceId(),
                                                                                            getJobExecutionId() },
                                               logger);
        }

        @Override
        public void logStartedMessage() {
            JoblogUtil.logToJobLogAndTraceOnly(Level.FINE, "step.started", new Object[] { runtimeStepExecution.getStepName(), getJobInstanceId(), getJobExecutionId() }, logger);
        }

        @Override
        public void publishEvent(WSStepThreadExecutionAggregate objectToPublish, BatchStatus batchStatus) {
            BatchEventsPublisher publisher = getBatchEventsPublisher();
            logger.fine("in step publish event - publisher = " + publisher + "will attempt to cut smf - batchStatus = " + batchStatus);

            //record some smf
            if (batchStatus == BatchStatus.COMPLETED || batchStatus.equals(BatchStatus.STOPPED) || batchStatus.equals(BatchStatus.FAILED)) {
                // smf call here
                ZosJBatchSMFLogging smflogger = getJBatchSMFLoggingService();
                logger.fine("cutting an smf step end record");
                if (smflogger != null) {

                    int partitionPlanCount = -1;
                    int partitionCount = -1;

                    if (isTopLevelStepThreadOfPartitionedStep()) {
                        // Partitions planned to execute
                        partitionPlanCount = ((TopLevelStepInstanceEntity) getPersistenceManagerService().getStepThreadInstance(getStepThreadInstanceKey())).getPartitionPlanSize();

                        // Count of partitions that did execute in a step
                        partitionCount = objectToPublish.getPartitionLevelStepExecutions().size();
                    }

                    byte[] timeUsedAfter = smflogger.getTimeUsedData(); // end CPU time for step
                    int rc = smflogger.buildAndWriteStepEndRecord(objectToPublish.getTopLevelStepExecution(),
                                                                  stepThreadExecution.getTopLevelStepExecution().getJobExecution(),
                                                                  runtimeWorkUnitExecution,
                                                                  partitionPlanCount,
                                                                  partitionCount,
                                                                  getPersistenceManagerService().getPersistenceType(),
                                                                  getPersistenceManagerService().getDisplayId(),
                                                                  getStep(),
                                                                  isTopLevelStepThreadOfPartitionedStep(),
                                                                  timeUsedBefore,
                                                                  timeUsedAfter);
                    logger.fine("back from calling native smf, rc = " + rc);
                }
            }

            if (publisher != null) {

                String correlationId = runtimeWorkUnitExecution.getCorrelationId();
                if (batchStatus == BatchStatus.STARTED) {
                    publisher.publishStepEvent(objectToPublish, BatchEventsPublisher.TOPIC_EXECUTION_STEP_STARTED, correlationId);
                } else if (batchStatus == BatchStatus.STOPPING) {
                    publisher.publishStepEvent(objectToPublish, BatchEventsPublisher.TOPIC_EXECUTION_STEP_STOPPING, correlationId);
                } else if (batchStatus == BatchStatus.COMPLETED) {
                    publisher.publishStepEvent(objectToPublish, BatchEventsPublisher.TOPIC_EXECUTION_STEP_COMPLETED, correlationId);
                } else if (batchStatus == BatchStatus.STOPPED) {
                    publisher.publishStepEvent(objectToPublish, BatchEventsPublisher.TOPIC_EXECUTION_STEP_STOPPED, correlationId);
                } else if (batchStatus == BatchStatus.FAILED) {
                    publisher.publishStepEvent(objectToPublish, BatchEventsPublisher.TOPIC_EXECUTION_STEP_FAILED, correlationId);
                }
            }
        }
    }

    private String getExceptionString(Throwable t) {

        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

}
