/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jbatch.container.ws;

import java.io.StringReader;
import java.text.MessageFormat;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.batch.operations.JobExecutionAlreadyCompleteException;
import javax.batch.operations.JobExecutionNotMostRecentException;
import javax.batch.operations.JobRestartException;
import javax.batch.runtime.BatchStatus;
import javax.xml.transform.stream.StreamSource;

import com.ibm.jbatch.container.jsl.ModelResolverFactory;
import com.ibm.jbatch.container.modelresolver.PropertyResolver;
import com.ibm.jbatch.container.modelresolver.PropertyResolverFactory;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.servicesmanager.ServicesManagerStaticAnchor;
import com.ibm.jbatch.container.ws.impl.StringUtils;
import com.ibm.jbatch.jsl.model.JSLJob;

public class BatchStatusValidator {

    private static final BatchStatus[] RESTARTABLE_EXECUTION_STATES = { BatchStatus.FAILED, BatchStatus.STOPPED, BatchStatus.STARTING };

    private static final InstanceState[] RESTARTABLE_INSTANCE_STATES = { InstanceState.FAILED, InstanceState.STOPPED };

    private static final BatchStatus[] RESTARTABLE_STEP_STATES = { BatchStatus.FAILED, BatchStatus.STOPPED, BatchStatus.COMPLETED };

    private final static Logger logger = Logger.getLogger(BatchStatusValidator.class.getName());

    /**
     * validates job is restart-able,
     * validates the jobInstance is in failed or stopped
     */
    public static void validateStatusAtInstanceRestart(long jobInstanceId, Properties restartJobParameters) throws JobRestartException, JobExecutionAlreadyCompleteException {

        IPersistenceManagerService iPers = ServicesManagerStaticAnchor.getServicesManager().getPersistenceManagerService();
        WSJobInstance jobInstance = iPers.getJobInstance(jobInstanceId);

        Helper helper = new Helper(jobInstance, restartJobParameters);
        if (!StringUtils.isEmpty(jobInstance.getJobXml())) {
            helper.validateRestartableFalseJobsDoNotRestart();
        }
        helper.validateJobInstanceFailedOrStopped();
    }

    /*
     * validates job is restart-able, jobExecution is most recent,
     * validates the jobExecutions and stepExecutions in non-final states
     */
    public static void validateStatusAtExecutionRestart(long previousExecutionId,
                                                        Properties restartJobParameters) throws JobRestartException, JobExecutionNotMostRecentException, JobExecutionAlreadyCompleteException {
        Helper helper = new Helper(previousExecutionId, restartJobParameters);
        helper.validateRestartableFalseJobsDoNotRestart();
        helper.validateJobExecutionIsMostRecent();
        helper.validateJobNotCompleteOrAbandonded();
        helper.validateJobInstanceFailedOrStopped();
        //Added starting since execution is now created on the Dispatcher
        helper.validateJobExecutionFailedOrStoppedOrStarting();
        helper.validateStepsAndPartitionsInNonFinalStates();
    }

    /**
     * Validates that the JobExecution is the latest and greatest
     *
     * @param previousExecutionId
     * @return
     */
    public static boolean isJobExecutionMostRecent(long previousExecutionId) {
        return isJobExecutionMostRecentImpl(previousExecutionId);
    }

    private static boolean isJobExecutionMostRecentImpl(long previousExecutionId) {
        boolean retMe = true;

        IPersistenceManagerService iPers = ServicesManagerStaticAnchor.getServicesManager().getPersistenceManagerService();
        WSJobInstance jobInstance = iPers.getJobInstanceFromExecutionId(previousExecutionId);
        long mostRecentExecutionId = iPers.getJobExecutionIdMostRecent(jobInstance.getInstanceId());

        if (mostRecentExecutionId != previousExecutionId) {
            retMe = false;
        }

        return retMe;
    }

    private static class Helper {

        private final WSJobInstance jobInstance;
        private long previousExecutionId;
        private final Properties restartJobParameters;

        private Helper(long previousExecutionId, Properties restartJobParameters) {
            this.jobInstance = getPersistenceManagerService().getJobInstanceFromExecutionId(previousExecutionId);
            this.previousExecutionId = previousExecutionId;
            this.restartJobParameters = restartJobParameters;
        }

        private Helper(WSJobInstance jobInstance, Properties restartJobParameters) {
            this.jobInstance = jobInstance;
            this.restartJobParameters = restartJobParameters;
        }

        /**
         * @return the persistence manager
         */
        private IPersistenceManagerService getPersistenceManagerService() {
            return ServicesManagerStaticAnchor.getServicesManager().getPersistenceManagerService();
        }

        /*
         * checks if Job restartable attribute is false
         */
        private void validateRestartableFalseJobsDoNotRestart() throws JobRestartException {

            if (!StringUtils.isEmpty(jobInstance.getJobXml())) {
                JSLJob jobModel = ModelResolverFactory.createJobResolver().resolveModel(new StreamSource(new StringReader(jobInstance.getJobXml())));

                PropertyResolver<JSLJob> propResolver = PropertyResolverFactory.createJSLJobPropertyResolver(false);
                propResolver.substituteProperties(jobModel, restartJobParameters);

                if (jobModel.getRestartable() != null && jobModel.getRestartable().equalsIgnoreCase("false")) {
                    throw new JobRestartException("Job Restartable attribute is false, Job cannot be restarted.");
                }
            }
        }

        /*
         * checks if JobExecution is most recent
         */
        private void validateJobExecutionIsMostRecent() throws JobExecutionNotMostRecentException {

            long mostRecentExecutionId = getPersistenceManagerService().getJobExecutionIdMostRecent(jobInstance.getInstanceId());

            if (mostRecentExecutionId != previousExecutionId) {
                String message = "ExecutionId: " + previousExecutionId + " is not the most recent execution.";
                throw new JobExecutionNotMostRecentException(message);
            }
        }

        /*
         * checks that each of JobInstance and previous JobExecution are in neither
         * Completed or Abandoned state
         */
        private void validateJobNotCompleteOrAbandonded() throws JobRestartException, JobExecutionAlreadyCompleteException {

            BatchStatus instanceBatchStatus = jobInstance.getBatchStatus();
            BatchStatus executionStatus = getPersistenceManagerService().getJobExecution(previousExecutionId).getBatchStatus();

            if (instanceBatchStatus == null) {
                String msg = "On restart, we didn't find an earlier instance batch status.";
                logger.fine(msg);
                throw new IllegalStateException(msg);
            }

            if (instanceBatchStatus.equals(BatchStatus.COMPLETED) || executionStatus.equals(BatchStatus.COMPLETED)) {
                String msg = "Job already completed.  (Instance, most recent execution) = (" + jobInstance.getInstanceId() + "," +
                             previousExecutionId + "), instanceStatus = " + instanceBatchStatus + ", executionStatus = " + executionStatus;
                logger.fine(msg);
                throw new JobExecutionAlreadyCompleteException(msg);
            } else if (instanceBatchStatus.equals(BatchStatus.ABANDONED) || executionStatus.equals(BatchStatus.ABANDONED)) {
                String msg = "Job previously abandoned.  (Instance, most recent execution) = (" + jobInstance.getInstanceId() + "," +
                             previousExecutionId + "), instanceStatus = " + instanceBatchStatus + ", executionStatus = " + executionStatus;
                logger.fine(msg);
                throw new JobRestartException(msg);
            }
        }

        /*
         * checks if jobExecution is either in STOPPED or FAILED state
         * TODO we might consider allowing restart on batchStatus STOPPING, but not for now
         */
        private void validateJobExecutionFailedOrStoppedOrStarting() throws JobRestartException {

            BatchStatus executionStatus = getPersistenceManagerService().getJobExecution(previousExecutionId).getBatchStatus();

            if (!checkIfStatusMatchesFromList(executionStatus, RESTARTABLE_EXECUTION_STATES)) {
//				String msg = getFormattedMessage("job.restart.denied", new Object[]{jobInstance.getInstanceId()}, "The job instance " + jobInstance.getInstanceId() + " cannot be restarted because the most recent execution is still in a non-final state.");
                String msg = "The job instance " + jobInstance.getInstanceId() + " cannot be restarted because the most recent execution is still in a non-final state.";
                logger.fine(msg);
                throw new JobRestartException(msg);
            }
        }

        private void validateJobInstanceFailedOrStopped() throws JobRestartException {

            InstanceState instanceState = jobInstance.getInstanceState();

            if (!checkIfStateMatchesFromList(instanceState, RESTARTABLE_INSTANCE_STATES)) {
                String msg = "The job instance " + jobInstance.getInstanceId() + " cannot be restarted because it is still in a non-final state.";
                logger.fine(msg);
                throw new JobRestartException(msg);
            }
        }

        /*
         * checks if all stepExecutions/partitions are in either STOPPED or COMPLETED or FAILED state
         * TODO we might consider allowing restart on batchStatus STOPPING, but not now
         */
        private void validateStepsAndPartitionsInNonFinalStates() throws JobRestartException {

            List<WSStepThreadExecutionAggregate> stepExecutionAggregates = getPersistenceManagerService().getStepExecutionAggregatesFromJobExecutionId(previousExecutionId);

            for (WSStepThreadExecutionAggregate stepExecutionAggregate : stepExecutionAggregates) {
                if (!checkIfStatusMatchesFromList(stepExecutionAggregate.getTopLevelStepExecution().getBatchStatus(), RESTARTABLE_STEP_STATES)) {
                    String msg = "The job instance " + jobInstance.getInstanceId() + " cannot be restarted because step " +
                                 stepExecutionAggregate.getTopLevelStepExecution().getStepName() + " is still in a non-final state.";
                    logger.fine(msg);
                    throw new JobRestartException(msg);
                } else {
                    /*
                     * 222050 - Backout 205106
                     * for(WSPartitionStepAggregate partitionStepAggregate : stepExecutionAggregate.getPartitionAggregate()){
                     * WSPartitionStepThreadExecution partitionExec = partitionStepAggregate.getPartitionStepThread();
                     */
                    for (WSPartitionStepThreadExecution partitionExec : stepExecutionAggregate.getPartitionLevelStepExecutions()) {
                        if (!checkIfStatusMatchesFromList(partitionExec.getBatchStatus(), RESTARTABLE_STEP_STATES)) {
                            String msg = "The job instance " + jobInstance.getInstanceId() + " cannot be restarted because step " +
                                         partitionExec.getStepName() + " partition " + partitionExec.getPartitionNumber() + " is still in a non-final state.";
                            logger.fine(msg);
                            throw new JobRestartException(msg);
                        }
                    }
                }
            }
        }

        /**
         * @return a formatted msg with the given key from the resource bundle.
         */
        private String getFormattedMessage(String msgKey, Object[] fillIns,
                                           String defaultMsg) {
            ResourceBundle resourceBundle = ResourceBundle.getBundle("com.ibm.jbatch.container.internal.resources.JBatchMessages");

            if (resourceBundle == null) {
                return defaultMsg;
            }

            String msg = resourceBundle.getString(msgKey);

            return (msg != null) ? MessageFormat.format(msg, fillIns) : defaultMsg;
        }

        /*
         * @param status The BatchStatus that needs to be checked
         *
         * @param listAcceptable The list of BatchStatus that should be acceptable
         * match
         *
         * @returns boolean true if status is one of the list
         */
        private boolean checkIfStatusMatchesFromList(BatchStatus status,
                                                     BatchStatus[] listAcceptable) {

            for (BatchStatus batchStatusFromList : listAcceptable) {
                if (status.equals(batchStatusFromList)) {
                    return true;
                }
            }
            return false;
        }

        /*
         * @param state The InstanceState that needs to be checked
         *
         * @param listAcceptable The list of InstanceStates that should be acceptable
         * match
         *
         * @returns boolean true if state is one of the list
         */
        private boolean checkIfStateMatchesFromList(InstanceState state,
                                                    InstanceState[] listAcceptable) {

            for (InstanceState batchStateFromList : listAcceptable) {
                if (state.equals(batchStateFromList)) {
                    return true;
                }
            }
            return false;
        }

    }
}
