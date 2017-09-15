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
package com.ibm.jbatch.container.execution.impl;

import java.util.Date;
import java.util.Properties;
import java.util.logging.Logger;

import javax.batch.runtime.BatchStatus;

import com.ibm.jbatch.container.navigator.ModelNavigator;
import com.ibm.jbatch.container.servicesmanager.ServicesManagerStaticAnchor;
import com.ibm.jbatch.container.ws.TopLevelNameInstanceExecutionInfo;
import com.ibm.jbatch.container.ws.WSJobExecution;
import com.ibm.jbatch.container.ws.WSJobInstance;
import com.ibm.jbatch.container.ws.events.BatchEventsPublisher;
import com.ibm.jbatch.container.ws.smf.ZosJBatchSMFLogging;
import com.ibm.jbatch.jsl.model.JSLJob;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * This class only serves to make the inheritance hierarchy more clear.
 */
public class RuntimeJobExecution extends RuntimeWorkUnitExecution {

    private final static String sourceClass = RuntimeJobExecution.class.getName();
    protected final static Logger logger = Logger.getLogger(sourceClass);

    protected String restartOnForThisExecution;

    private final Properties jobParameters;

    byte[] timeUsedBefore = null;

    public RuntimeJobExecution(TopLevelNameInstanceExecutionInfo topLevelInfo,
                               Properties jobParameters,
                               String restartOn,
                               ModelNavigator<JSLJob> navigator) {
        super(navigator, topLevelInfo);

        this.restartOnForThisExecution = restartOn;
        this.jobParameters = jobParameters;
        this.type = WorkUnitType.TOP_LEVEL_JOB;

        if (jobParameters != null)
            this.correlationId = jobParameters.getProperty("com_ibm_ws_batch_events_correlationId", null);

    }

    public RuntimeJobExecution(TopLevelNameInstanceExecutionInfo topLevelInfo,
                               Properties jobParameters,
                               ModelNavigator<JSLJob> navigator) {
        this(topLevelInfo, jobParameters, null, navigator);
    }

    /**
     * This is unique for top-level jobs, and will be extended with subclass
     * implementations to trace partitions and split-flows a bit differently.
     */
    @Override
    @Trivial
    protected String getExecutionLogMessage(MessageType msgType) {

        LogHelper lh = new LogHelper();
        StringBuilder buf = lh.getBeginningPart(msgType);

        buf.append("invoking execution for a job");
        buf.append("\n JobInstance id = ");
        buf.append(getTopLevelNameInstanceExecutionInfo().getInstanceId());
        buf.append("\n JobExecution id = ");
        buf.append(getTopLevelNameInstanceExecutionInfo().getExecutionId());
        buf.append("\n Job Name = ");
        buf.append(getTopLevelNameInstanceExecutionInfo().getJobName());

        if (jobParameters != null) {
            buf.append("\n Job Parameters = ");
            buf.append(jobParameters);
        }

        if (!msgType.equals(MessageType.STARTED)) {
            buf.append("\n Job Batch Status = " + getBatchStatus());
            buf.append(", Job Exit Status = " + getExitStatus());
        }
        buf.append("\n" + LogHelper.dashes);
        return buf.toString();
    }

    @Override
    protected Logger getClassNameLogger() {
        return logger;
    }

    @Override
    public String toString() {
        return "Superclass info: " + super.toString() + "; " + getTopLevelNameInstanceExecutionInfo();
    }

    @Override
    public String getRestartOnForThisExecution() {
        return restartOnForThisExecution;
    }

    public Properties getJobParameters() {
        return jobParameters;
    }

    /**
     * @return the SMF service
     */
    protected ZosJBatchSMFLogging getJBatchSMFLoggingService() {
        return ServicesManagerStaticAnchor.getServicesManager().getJBatchSMFService();
    }

    @Override
    public void workStarted(Date date) {
        if (batchStatus.equals(BatchStatus.STARTING)) {
            batchStatus = BatchStatus.STARTED;
            WSJobExecution execution = (WSJobExecution) getPersistenceManagerService().updateJobExecutionAndInstanceOnStarted(getTopLevelExecutionId(), new Date());
            publishEvent(execution, execution.getJobInstance(), batchStatus);
        } else {
            logger.fine("No-op on workStarted() since batch status is set to: " + batchStatus);
        }

        ZosJBatchSMFLogging smflogger = getJBatchSMFLoggingService();
        if (smflogger != null) {
            timeUsedBefore = getJBatchSMFLoggingService().getTimeUsedData();
        }
    }

    /**
     * Publish event for this job instance
     *
     * @param jobInstance
     * @param eventToBePublished
     */
    private void publishEvent(WSJobInstance objectToPublish, String eventToPublish) {

        if (getBatchEventsPublisher() != null) {
            getBatchEventsPublisher().publishJobInstanceEvent(objectToPublish, eventToPublish, correlationId);
        }
    }

    /**
     * Publish jms topic if batch jms event is available
     *
     * @param objectToPublish WSJobExecution
     * @param eventToPublish
     */
    private void publishEvent(WSJobExecution objectToPublish, String eventToPublishTo) {
        if (getBatchEventsPublisher() != null) {
            getBatchEventsPublisher().publishJobExecutionEvent(objectToPublish, eventToPublishTo, correlationId);
        }
    }

    @Override
    public void workStopping(Date date) {
        if (!batchStatus.equals(BatchStatus.COMPLETED) && !batchStatus.equals(BatchStatus.FAILED) && !batchStatus.equals(BatchStatus.ABANDONED)) {
            batchStatus = BatchStatus.STOPPING;
            WSJobExecution execution = (WSJobExecution) getPersistenceManagerService().updateJobExecutionAndInstanceOnStatusChange(getTopLevelExecutionId(), BatchStatus.STOPPING,
                                                                                                                                   new Date());
            WSJobInstance jobInstance = getPersistenceManagerService().getJobInstanceFromExecutionId(execution.getExecutionId());
            publishEvent(execution, jobInstance, batchStatus);
        } else {
            logger.fine("No-op on workStopping() since batch status is set to: " + batchStatus);
        }
    }

    @Override
    public void workEnded(Date date) {
        /////
        // Update atomically.  Mainly we're trying to avoid inconsistency such as the instance showing COMPLETED with the execution FAILED
        //
        // This has some tradeoff.  Maybe things have failed already.  We're now making it harder to update with the FAILED state since
        // the whole update has to succeed...otherwise we leave things in some failed-but-not-recognized-as-failure state.
        WSJobExecution execution = (WSJobExecution) getPersistenceManagerService().updateJobExecutionAndInstanceOnEnd(getTopLevelExecutionId(), batchStatus, exitStatus,
                                                                                                                      new Date());
        //WSJobInstance jobInstance = getPersistenceManagerService().getJobInstanceFromExecutionId(execution.getExecutionId());

        ZosJBatchSMFLogging smflogger = getJBatchSMFLoggingService();
        if (smflogger != null) {
            byte[] timeUsedAfter = getJBatchSMFLoggingService().getTimeUsedData();

            getJBatchSMFLoggingService().buildAndWriteJobEndRecord(execution,
                                                                   this,
                                                                   getPersistenceManagerService().getPersistenceType(),
                                                                   getPersistenceManagerService().getDisplayId(),
                                                                   timeUsedBefore,
                                                                   timeUsedAfter);
        }

        publishEvent(execution, execution.getJobInstance(), batchStatus);
    }

    /**
     * Helper method to publish execution data to appropriate topic per batchStatus
     *
     * @param execution
     * @param jobInstance
     * @param batchStatus
     */
    private void publishEvent(WSJobExecution execution, WSJobInstance jobInstance, BatchStatus batchStatus) {

        if (batchStatus == BatchStatus.FAILED) {
            publishEvent(execution, BatchEventsPublisher.TOPIC_EXECUTION_FAILED);
            publishEvent(jobInstance, BatchEventsPublisher.TOPIC_INSTANCE_FAILED);
        } else if (batchStatus == BatchStatus.COMPLETED) {
            publishEvent(execution, BatchEventsPublisher.TOPIC_EXECUTION_COMPLETED);
            publishEvent(jobInstance, BatchEventsPublisher.TOPIC_INSTANCE_COMPLETED);
        } else if (batchStatus == BatchStatus.STOPPED) {
            publishEvent(execution, BatchEventsPublisher.TOPIC_EXECUTION_STOPPED);
            publishEvent(jobInstance, BatchEventsPublisher.TOPIC_INSTANCE_STOPPED);
            //smf call to go here, eventually
        } else if (batchStatus == BatchStatus.STOPPING) {
            publishEvent(jobInstance, BatchEventsPublisher.TOPIC_INSTANCE_STOPPING);
            publishEvent(execution, BatchEventsPublisher.TOPIC_EXECUTION_STOPPING);
        } else if (batchStatus == BatchStatus.STARTED) {
            publishEvent(jobInstance, BatchEventsPublisher.TOPIC_INSTANCE_DISPATCHED);
            publishEvent(execution, BatchEventsPublisher.TOPIC_EXECUTION_STARTED);
        }

    }

    @Override
    public void updateExecutionJobLogDir(String logDirPath) {
        getPersistenceManagerService().updateJobExecutionLogDir(getTopLevelExecutionId(), logDirPath);
    }

    @Override
    public boolean isRemotePartitionDispatch() {
        //JobExecution cannot be remote
        return false;
    }
}
