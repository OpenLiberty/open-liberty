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
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.operations.JobExecutionNotRunningException;
import javax.batch.runtime.BatchStatus;

import com.ibm.jbatch.container.IController;
import com.ibm.jbatch.container.IThreadRootController;
import com.ibm.jbatch.container.artifact.proxy.InjectionReferences;
import com.ibm.jbatch.container.artifact.proxy.JobListenerProxy;
import com.ibm.jbatch.container.artifact.proxy.ListenerFactory;
import com.ibm.jbatch.container.execution.impl.RuntimeJobExecution;
import com.ibm.jbatch.container.execution.impl.RuntimePartitionExecution;
import com.ibm.jbatch.container.execution.impl.RuntimeSplitFlowExecution;
import com.ibm.jbatch.container.execution.impl.RuntimeWorkUnitExecution;
import com.ibm.jbatch.container.execution.impl.RuntimeWorkUnitExecution.StopLock;
import com.ibm.jbatch.container.jsl.ModelSerializer;
import com.ibm.jbatch.container.jsl.ModelSerializerFactory;
import com.ibm.jbatch.container.navigator.ModelNavigator;
import com.ibm.jbatch.container.persistence.jpa.JobInstanceEntity;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.servicesmanager.ServicesManagerStaticAnchor;
import com.ibm.jbatch.container.status.ExecutionStatus;
import com.ibm.jbatch.container.status.ExtendedBatchStatus;
import com.ibm.jbatch.container.ws.JoblogUtil;
import com.ibm.jbatch.container.ws.PartitionReplyQueue;
import com.ibm.jbatch.jsl.model.JSLJob;

public class WorkUnitThreadControllerImpl implements IThreadRootController {

    private final static String CLASSNAME = WorkUnitThreadControllerImpl.class.getName();
    private final static Logger logger = Logger.getLogger(CLASSNAME);

    private final RuntimeWorkUnitExecution runtimeWorkUnitExecution;

    private ListenerFactory listenerFactory = null;

    private ExecutionTransitioner transitioner;
    private final ModelNavigator<JSLJob> jobNavigator;
    private PartitionReplyQueue partitionReplyQueue;

    private ControllerHelper threadTypeBasedControllerHelper = null;

    boolean listenersCalled = false;

    public WorkUnitThreadControllerImpl(RuntimeWorkUnitExecution runtimeWorkUnitExecution) {
        this.runtimeWorkUnitExecution = runtimeWorkUnitExecution;
        this.threadTypeBasedControllerHelper = new ThreadTypeBasedHelperFactory().createControllerHelper();
        this.jobNavigator = runtimeWorkUnitExecution.getJobNavigator();
    }

    /*
     * By not passing the rootJobExecutionId, we are "orphaning" the subjob execution and making it not findable from the parent.
     * This is exactly what we want for getStepExecutions()... we don't want it to get extraneous entries for the partitions.
     */
    public WorkUnitThreadControllerImpl(RuntimeWorkUnitExecution runtimeWorkUnitExecution, PartitionReplyQueue partitionReplyQueue) {
        this(runtimeWorkUnitExecution);
        this.partitionReplyQueue = partitionReplyQueue;
    }

    /**
     * @return the batch persistence service
     */
    protected IPersistenceManagerService getPersistenceManagerService() {
        return ServicesManagerStaticAnchor.getServicesManager().getPersistenceManagerService();
    }

    @Override
    public ExecutionStatus runExecutionOnThread() {
        return threadTypeBasedControllerHelper.runExecutionOnThread();
    }

    private ExecutionStatus executeCoreTransitionLoop() {
        ExecutionStatus retVal = transitioner.doExecutionLoop();
        ExtendedBatchStatus extBatchStatus = retVal.getExtendedBatchStatus();
        switch (extBatchStatus) {
            case JSL_STOP:
                jslStop();
                break;
            case JSL_FAIL:
                updateJobBatchStatus(BatchStatus.FAILED);
                break;
            case EXCEPTION_THROWN:
                updateJobBatchStatus(BatchStatus.FAILED);
                break;
            default:
                break; //no-op
        }
        return retVal;
    }

    private boolean startWorkUnitIfNotStopping() {

        BatchStatus batchStatus = runtimeWorkUnitExecution.getBatchStatus();

        // Our check is more narrow than the method title;  we think it amounts to the same thing.
        if (batchStatus.equals(BatchStatus.STARTING)) {
            // Now that we're ready to start invoking artifacts, set the status to 'STARTED'
            markWorkStarted();
            if (!(runtimeWorkUnitExecution instanceof RuntimePartitionExecution)) {
                JoblogUtil.logToJobLogAndTraceOnly(Level.FINE, "job.started", new Object[] {
                                                                                             runtimeWorkUnitExecution.getTopLevelJobName(),
                                                                                             runtimeWorkUnitExecution.getTopLevelInstanceId(),
                                                                                             runtimeWorkUnitExecution.getTopLevelExecutionId() },
                                                   logger);

                // If we're not a split/flow we're a job, so log the original JSL
                if (!(runtimeWorkUnitExecution instanceof RuntimeSplitFlowExecution)) {

                    JobInstanceEntity instanceEntity = getPersistenceManagerService().getJobInstance(runtimeWorkUnitExecution.getTopLevelInstanceId());
                    String jsl = instanceEntity.getJobXml();
                    JoblogUtil.logToJobLogAndTraceOnly(Level.INFO, "display.unresolved.jsl", new Object[] { jsl }, logger);
                }

                // Print the resolved JSL for the job or flow
                JSLJob jslJob = jobNavigator.getRootModelElement();
                ModelSerializer<JSLJob> ms = ModelSerializerFactory.createJobModelSerializer();
                String prettyXml = ms.prettySerializeModel(jslJob);

                // Set the type.  Partitions are handled elsewhere, step doesn't go through here
                String type = "job";
                if (runtimeWorkUnitExecution instanceof RuntimeSplitFlowExecution) {
                    type = "flow";
                }
                JoblogUtil.logToJobLogAndTraceOnly(Level.INFO, "display.resolved.jsl", new Object[] { type, prettyXml }, logger);
            }

            listenersCalled = true;
            jobListenersBeforeJob();

            transitioner = new ExecutionTransitioner(runtimeWorkUnitExecution, jobNavigator, partitionReplyQueue);
            return true;
        } else {
            logger.fine("Won't start work unit because status is currently : " + batchStatus);
            return false;
        }
    }

    public ExecutionStatus executeWorkUnit() {

        ExecutionStatus retVal = null;
        boolean workStarted = false;
        setupListeners();

        try {
            StopLock stopLock = getStopLock(); // Store in local variable to facilitate Ctrl+Shift+G search in Eclipse
            synchronized (stopLock) {
                workStarted = startWorkUnitIfNotStopping();
            }

            // --------------------
            // Let go of the lock and begin the BIG loop
            // transitioning within the job !!!
            // --------------------
            if (workStarted) {
                retVal = executeCoreTransitionLoop();
            }

        } catch (Throwable t) {
            // We still want to try to call the afterJob() listener and persist the batch and exit
            // status for the failure in an orderly fashion.  So catch and continue.
            // FFDC
            threadTypeBasedControllerHelper.batchStatusFailedFromException();
            // Log job-level exception in the job log with message
            if (runtimeWorkUnitExecution instanceof RuntimeJobExecution) {
                JoblogUtil.logToJobLogAndTraceOnly(Level.SEVERE, "exception.executing.job", new Object[] { getExceptionString(t) }, logger);
                // Moved the following block of code from below where the check was
                // if (!(runtimeWorkUnitExecution instanceof RuntimePartitionExecution))
                // Moving it here prevents it being logged for a split-flow.
                JoblogUtil.logToJobLogAndTrace(Level.WARNING, "job.failed", new Object[] {
                                                                                           runtimeWorkUnitExecution.getTopLevelJobName(),
                                                                                           runtimeWorkUnitExecution.getBatchStatus(),
                                                                                           runtimeWorkUnitExecution.getExitStatus(),
                                                                                           runtimeWorkUnitExecution.getTopLevelInstanceId(),
                                                                                           runtimeWorkUnitExecution.getTopLevelExecutionId() },
                                               logger);
            } else {
                // Log exception stack trace without a message
                JoblogUtil.logRawMsgToJobLogAndTraceOnly(Level.SEVERE, getExceptionString(t), logger);
            }

        }

        StopLock stopLock = getStopLock(); // Store in local variable to facilitate Ctrl+Shift+G search in Eclipse
        synchronized (stopLock) {
            // This prevents a stop from taking effect in the middle of afterJob, but what
            // chance would we the container have to do anything anyway?
            endOfWorkUnit(listenersCalled);
            if (!(runtimeWorkUnitExecution instanceof RuntimePartitionExecution)) {

                Object[] params = new Object[] {
                                                 runtimeWorkUnitExecution.getTopLevelJobName(),
                                                 runtimeWorkUnitExecution.getBatchStatus(),
                                                 runtimeWorkUnitExecution.getExitStatus(),
                                                 runtimeWorkUnitExecution.getTopLevelInstanceId(),
                                                 runtimeWorkUnitExecution.getTopLevelExecutionId() };

                if (runtimeWorkUnitExecution.getBatchStatus() == BatchStatus.FAILED) {
                    JoblogUtil.logToJobLogAndTrace(Level.WARNING, "job.failed", params, logger);
                } else {
                    JoblogUtil.logToJobLogAndTraceOnly(Level.FINE, "job.ended", params, logger);
                }
            }
        }
        return retVal;
    }

    protected void jslStop() {
        threadTypeBasedControllerHelper.jslStop();
    }

    /**
     * Set batchStatus=STARTED, startTime, and lastUpdateTime, in the local
     * jobExecution object and in the DB. Set instance state to DISPATCHED.
     * Publish instance DISPATCHED event
     */
    protected void markWorkStarted() {
        runtimeWorkUnitExecution.workStarted(new Date());
    }

    /*
     * Follow similar pattern for end of step in BaseStepControllerImpl
     *
     * 1. Execute the very last artifacts (jobListener)
     * 2. transition to final batch status
     * 3. default ExitStatus if necessary
     * 4. persist statuses and end time data
     *
     * We don't want to give up on the orderly process of 2,3,4, if we blow up
     * in after job, so catch that and keep on going.
     */
    protected void endOfWorkUnit(boolean callAfterJobListeners) {

        // 1. Execute the very last artifacts (jobListener) if the before job listeners were called
        if (callAfterJobListeners) {
            try {
                jobListenersAfterJob();
            } catch (Throwable t) {
                // Log job-level exception in the job log with message
                if (runtimeWorkUnitExecution instanceof RuntimeJobExecution) {
                    JoblogUtil.logToJobLogAndTraceOnly(Level.SEVERE, "exception.after.job", new Object[] { getExceptionString(t) }, logger);
                } else {
                    // Log exception stack trace without a message
                    JoblogUtil.logRawMsgToJobLogAndTraceOnly(Level.SEVERE, getExceptionString(t), logger);
                }
                // FFDC
                threadTypeBasedControllerHelper.batchStatusFailedFromException();
            }
        }

        // 2. transition to final batch status
        transitionToFinalBatchStatus();

        // 3. default ExitStatus if necessary
        if (runtimeWorkUnitExecution.getExitStatus() == null) {
            logger.fine("No job-level exitStatus set, defaulting to job batch Status = " + runtimeWorkUnitExecution.getBatchStatus());
            runtimeWorkUnitExecution.setExitStatus(runtimeWorkUnitExecution.getBatchStatus().name());
        }

        // 4. persist statuses and end time data

        /* Maybe this should be part of RWUE toString() */

        /*
         * logger.fine("Job complete for job id=" + runtimeWorkUnitExecution.getWorkUnitInternalJobName()
         * + ", executionId=" + runtimeWorkUnitExecution.getWorkUnitInternalExecutionId()
         * + ", batchStatus=" + runtimeWorkUnitExecution.getBatchStatus()
         * + ", exitStatus=" + runtimeWorkUnitExecution.getExitStatus());
         */
        persistWorkUnitBatchAndExitStatus();

        // 5. purge checkpoint data iff batchStatus==COMPLETED. (TODO: ABANDONED too?)
//		if (runtimeWorkUnitExecution.getBatchStatus().equals( BatchStatus.COMPLETED ) ) {
//		    getPersistenceManagerService().deleteCheckpointData( runtimeWorkUnitExecution.getWorkUnitInternalInstanceId() );
//		}

    }

    private void persistWorkUnitBatchAndExitStatus() {
        StopLock stopLock = getStopLock(); // Store in local variable to facilitate Ctrl+Shift+G search in Eclipse
        synchronized (stopLock) {

            BatchStatus batchStatus = runtimeWorkUnitExecution.getBatchStatus();

            boolean reachedAnExpectedTerminatingStatus = false;
            String errorMsg = null;
            // Arguably we should do nothing at let the error bubble up if we complete in a non-final state somehow.
            // Let's err on the side of tidying up
            if (batchStatus.equals(BatchStatus.COMPLETED) || batchStatus.equals(BatchStatus.STOPPED) ||
                batchStatus.equals(BatchStatus.FAILED)) {
                reachedAnExpectedTerminatingStatus = true;
            } else {
                errorMsg = "Not expected to encounter batchStatus of " + batchStatus + " at this point.";
                runtimeWorkUnitExecution.setBatchStatus(BatchStatus.FAILED);
            }

            try {
                runtimeWorkUnitExecution.workEnded(new Date());
            } catch (Throwable t) {
                // If we've just caught a Throwable it was likely on persisting the JobExecution, and
                // we're likely to also fail trying a second time now.  Still, having a nicely marked
                // (with FAILED) status is worth it enough to try, I think.
                try {
                    runtimeWorkUnitExecution.setBatchStatus(BatchStatus.FAILED);
                    runtimeWorkUnitExecution.workEnded(new Date());
                } catch (Throwable t2) {
                    // Log job-level exception in the job log with message
                    if (runtimeWorkUnitExecution instanceof RuntimeJobExecution) {
                        JoblogUtil.logToJobLogAndTrace(Level.SEVERE, "error.persisting.jobExecution", new Object[] { getExceptionString(t) }, logger);
                    } else {
                        // Log exception stack trace without a message
                        JoblogUtil.logRawMsgToJobLogAndTraceOnly(Level.SEVERE, getExceptionString(t), logger);
                    }
                }
            }

            if (!reachedAnExpectedTerminatingStatus) {
                logger.fine(errorMsg);
                throw new IllegalStateException(errorMsg);
            }
        }
    }

    /**
     * The only valid states at this point are STARTED or STOPPING. Shouldn't have
     * been able to get to COMPLETED, STOPPED, or FAILED at this point in the code.
     */
    private void transitionToFinalBatchStatus() {
        StopLock stopLock = getStopLock(); // Store in local variable to facilitate Ctrl+Shift+G search in Eclipse
        synchronized (stopLock) {

            BatchStatus currentBatchStatus = runtimeWorkUnitExecution.getBatchStatus();
            if (currentBatchStatus.equals(BatchStatus.STARTED)) {
                updateJobBatchStatus(BatchStatus.COMPLETED);
            } else if (currentBatchStatus.equals(BatchStatus.STOPPING)) {
                updateJobBatchStatus(BatchStatus.STOPPED);
            } else if (currentBatchStatus.equals(BatchStatus.FAILED)) {
                updateJobBatchStatus(BatchStatus.FAILED); // Should have already been done but maybe better for possible code refactoring to have it here.
            } else {
                throw new IllegalStateException("Step batch status should not be in a " + currentBatchStatus.name() + " state");
            }
        }
    }

    protected void updateJobBatchStatus(BatchStatus batchStatus) {
        StopLock stopLock = getStopLock(); // Store in local variable to facilitate Ctrl+Shift+G search in Eclipse
        synchronized (stopLock) {

            runtimeWorkUnitExecution.setBatchStatus(batchStatus);
        }
    }

    /*
     * The thought here is that while we don't persist all the transitions in batch status (given
     * we plan to persist at the very end), we do persist STOPPING right away, since if we end up
     * "stuck in STOPPING" we at least will have a record in the database.
     */
    protected void batchStatusStopping() {
        runtimeWorkUnitExecution.workStopping(new Date());
    }

    private void setupListeners() {
        JSLJob jobModel = runtimeWorkUnitExecution.getJobNavigator().getRootModelElement();
        InjectionReferences injectionRef = new InjectionReferences(runtimeWorkUnitExecution.getWorkUnitJobContext(), null, null);
        listenerFactory = new ListenerFactory(jobModel, injectionRef);
        runtimeWorkUnitExecution.setListenerFactory(listenerFactory);
    }

    protected StopLock getStopLock() {
        return runtimeWorkUnitExecution.getStopLock();
    }

    @Override
    public void stop() {
        StopLock stopLock = getStopLock(); // Store in local variable to facilitate Ctrl+Shift+G search in Eclipse
        synchronized (stopLock) {

            BatchStatus batchStatus = runtimeWorkUnitExecution.getBatchStatus();

            // We don't want to overwrite a FAILED with a STOPPED
            switch (batchStatus) {
                case STARTING:
                case STARTED:
                    batchStatusStopping();

                    if (transitioner != null) {
                        IController stoppableElementController = transitioner.getCurrentStoppableElementController();
                        if (stoppableElementController != null) {
                            stoppableElementController.stop();
                        }
                    }

                    break;
                case ABANDONED:
                case COMPLETED:
                case FAILED:
                case STOPPED:
                    logger.fine("Stop unsuccessful since batch status for job is already set to: " + batchStatus);

                    throw new JobExecutionNotRunningException();

                case STOPPING: //Possibly still running.
                default:
                    logger.fine("Stop ignored since batch status for job is already set to: " + batchStatus);

            }
        }
    }

    // Call beforeJob() on all the job listeners
    protected void jobListenersBeforeJob() {
        List<JobListenerProxy> jobListeners = listenerFactory.getJobListeners();
        for (JobListenerProxy listenerProxy : jobListeners) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Invoking beforeJob() on jobListener: " + listenerProxy.getDelegate() + " of type: " + listenerProxy.getDelegate().getClass());
            }
            listenerProxy.beforeJob();
        }
    }

    // Call afterJob() on all the job listeners
    private void jobListenersAfterJob() {
        List<JobListenerProxy> jobListeners = listenerFactory.getJobListeners();
        for (JobListenerProxy listenerProxy : jobListeners) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(" Invoking afterJob() on jobListener: " + listenerProxy.getDelegate() + " of type: " + listenerProxy.getDelegate().getClass());
            }
            listenerProxy.afterJob();
        }
    }

    @Override
    public List<Long> getLastRunStepExecutions() {
        return this.transitioner.getStepExecIds();
    }

    private String getExceptionString(Throwable t) {

        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private class ThreadTypeBasedHelperFactory {
        private ControllerHelper createControllerHelper() {
            if (runtimeWorkUnitExecution instanceof RuntimeSplitFlowExecution) {
                return new SplitFlowControllerHelper();
            } else if (runtimeWorkUnitExecution instanceof RuntimePartitionExecution) {
                return new PartitionControllerHelper();
            } else {
                return new JobControllerHelper();
            }
        }
    }

    /**
     * Encapsulate some areas where we need to do slightly different things for a split-flow thread vs.
     * a top-level (job) thread or a partition thread.
     */
    private interface ControllerHelper {
        public void jslStop();

        public ExecutionStatus runExecutionOnThread();

        public void batchStatusFailedFromException();
    }

    /*
     * This use of this set of inner classes provides a bit more organization than single methods with instanceof checks scattered throughout.
     *
     * In some ways it's a little more to get your head around than separate, outer classes, for each of these, but that makes the overall type hierarchy
     * look a bit more complicated than it really is.. since this logic is contained to this one class.
     */
    private abstract class AbstractControllerHelper implements ControllerHelper {

        @Override
        abstract public void jslStop();

        @Override
        public ExecutionStatus runExecutionOnThread() {
            return executeWorkUnit();
        }

        @Override
        public void batchStatusFailedFromException() {
            updateJobBatchStatus(BatchStatus.FAILED);
        }
    }

    private class JobControllerHelper extends AbstractControllerHelper {
        @Override
        public void jslStop() {
            String restartOn = runtimeWorkUnitExecution.getRestartOnForNextExecution();
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Logging JSL stop(): exitStatus = " + runtimeWorkUnitExecution.getExitStatus() + ", restartOn = " + restartOn);
            }

            StopLock stopLock = getStopLock(); // Store in local variable to facilitate Ctrl+Shift+G search in Eclipse
            synchronized (stopLock) {

                BatchStatus batchStatus = runtimeWorkUnitExecution.getBatchStatus();

                // This might already be in STOPPING from another thread.  Performing this check prevents us from
                // confusingly publishing two sets of STOP events.
                //
                // Actually at this point I don't see how this could ever be anything but
                // STARTED or STOPPING, but maybe it's simpler to follow the pattern of checking for STARTING or STARTED
                // whenever possible.  In particular we never want to overwrite a FAILED with a STOPPED.
                //
                if (batchStatus.equals(BatchStatus.STARTING) || batchStatus.equals(BatchStatus.STARTED)) {
                    batchStatusStopping();
                }
            }

            // Seems worth doing this whether we call batchStatusStopping() on this thread or not (perhaps because a non-JSL stop() was issued on another thread).
            // This way we can resume on the JSL-intended execution element.
            // This is such a small timing window, it's hardly worth worrying about... but that seems like a sensible decision.
            getPersistenceManagerService().updateJobInstanceWithRestartOn(runtimeWorkUnitExecution.getTopLevelInstanceId(), restartOn);
            return;
        }
    }

    private class PartitionControllerHelper extends AbstractControllerHelper {
        @Override
        public void jslStop() {
            throw new IllegalStateException("Don't support stopping from within a partition \"subjob\".");
        }
    }

    private class SplitFlowControllerHelper extends AbstractControllerHelper {

        RuntimeSplitFlowExecution flowInSplitExecution = (RuntimeSplitFlowExecution) runtimeWorkUnitExecution;

        @Override
        public void jslStop() {
            throw new IllegalStateException("Don't support stopping within flow.");
        }

        @Override
        public ExecutionStatus runExecutionOnThread() {
            ExecutionStatus status = super.runExecutionOnThread();
            flowInSplitExecution.setFlowStatus(status);
            return status;
        }

        @Override
        public void batchStatusFailedFromException() {
            super.batchStatusFailedFromException();
            flowInSplitExecution.getFlowStatus().setExtendedBatchStatus(ExtendedBatchStatus.EXCEPTION_THROWN);
        }
    }
}
