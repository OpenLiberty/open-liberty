/**
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
import com.ibm.jbatch.container.persistence.jpa.RemotablePartitionKey;
import com.ibm.jbatch.container.ws.PartitionPlanConfig;
import com.ibm.jbatch.container.ws.events.BatchEventsPublisher;
import com.ibm.jbatch.jsl.model.JSLJob;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * For sub-job partitions
 */
public class RuntimePartitionExecution extends RuntimeWorkUnitExecution {

    protected final static Logger logger = Logger.getLogger(RuntimePartitionExecution.class.getName());

    /**
     * The config related to this specific partition.
     */
    private final PartitionPlanConfig partitionPlanConfig;

    private Long partitionLevelStepExecutionId = null; // TODO: do something with this.

    private boolean isRemoteDispatch;

    private boolean finalStatusSent = false;

    /**
     * @param partitionConfig
     * @param navigator
     */
    public RuntimePartitionExecution(PartitionPlanConfig partitionPlanConfig, ModelNavigator<JSLJob> navigator, boolean isRemoteDispatch) {
        super(navigator, partitionPlanConfig.getTopLevelNameInstanceExecutionInfo());

        this.partitionPlanConfig = partitionPlanConfig;
        this.isRemoteDispatch = isRemoteDispatch;
        this.type = WorkUnitType.PARTITIONED_STEP;
        this.correlationId = partitionPlanConfig.getCorrelationId();
    }

    @Override
    protected Logger getClassNameLogger() {
        return logger;
    }

    @Override
    @Trivial
    protected String getExecutionLogMessage(MessageType msgType) {

        LogHelper lh = new LogHelper();
        StringBuilder buf = lh.getBeginningPart(msgType);

        buf.append("invoking execution for a partition");
        buf.append("\n Partition step name = ");
        buf.append(getStepName());
        buf.append("\n Partition number = ");
        buf.append(getPartitionNumber());
        if (partitionLevelStepExecutionId != null) {
            buf.append("\n Partition-level (internal) step execution id = ");
            buf.append(partitionLevelStepExecutionId);
        }
        buf.append("\n Associated Top-level StepExecution id = ");
        buf.append(getTopLevelStepExecutionId());
        buf.append("\n Associated Top-level JobInstance id = ");
        buf.append(getTopLevelInstanceId());
        buf.append("\n Associated Top-level JobExecution id = ");
        buf.append(getTopLevelExecutionId());
        buf.append("\n Associated Top-level Job Name = ");
        buf.append(getTopLevelJobName());

        if (!msgType.equals(MessageType.STARTED)) {
            buf.append("\n Partition Batch Status = " + getBatchStatus());
            buf.append(", Partition Exit Status = " + getExitStatus());
        }
        Properties partitionProperties = partitionPlanConfig.getPartitionPlanProperties();
        if (partitionProperties != null) {
            buf.append("\n Partition Properties = ");
            buf.append(partitionProperties);
        }
        buf.append("\n" + LogHelper.dashes);

        return buf.toString();
    }

    @Trivial
    @Override
    public String getStepExecutionCreatedMessage(RuntimeStepExecution runtimeStepExecution) {

        partitionLevelStepExecutionId = runtimeStepExecution.getInternalStepThreadExecutionId();

        StringBuilder buf = new StringBuilder("\n");
        buf.append(LogHelper.dashes);

        buf.append("Invoking step execution partition, created new step execution for a partition-level step");
        buf.append("\n Partition step name = ");
        buf.append(getStepName());
        buf.append("\n Partition number = ");
        buf.append(getPartitionNumber());
        buf.append("\n Partition-level (internal) step execution id = ");
        buf.append(partitionLevelStepExecutionId);
        buf.append("\n Associated Top-level StepExecution id = ");
        buf.append(runtimeStepExecution.getTopLevelStepExecutionId());

        return buf.toString();
    }

    private long getTopLevelStepExecutionId() {
        return partitionPlanConfig.getTopLevelStepExecutionId();
    }

    public String getStepName() {
        return partitionPlanConfig.getStepName();
    }

    @Override
    public void workStarted(Date date) {
        batchStatus = BatchStatus.STARTED;
        publishPartitionEvent();
    }

    @Override
    public void workStopping(Date date) {
        batchStatus = BatchStatus.STOPPING;
    }

    @Override
    public void workEnded(Date date) {
        publishPartitionEvent();
    }

    @Override
    public String getPartitionedStepName() {
        return getStepName();
    }

    @Override
    public Integer getPartitionNumber() {
        return partitionPlanConfig.getPartitionNumber();
    }

    @Override
    public String getCorrelationId() {
        return partitionPlanConfig.getCorrelationId();
    }

    @Override
    public void updateExecutionJobLogDir(String logDirPath) {
        RemotablePartitionKey key = new RemotablePartitionKey(getTopLevelExecutionId(), getStepName(), getPartitionNumber());
        getPersistenceManagerService().updatePartitionExecutionLogDir(key, logDirPath);
    }

    /**
     * @return the PartitionPlanConfig
     */
    public PartitionPlanConfig getPartitionPlanConfig() {
        return partitionPlanConfig;
    }

    protected void publishPartitionEvent() {
        BatchEventsPublisher publisher = getBatchEventsPublisher();

        if (publisher != null) {
            String correlationId = getCorrelationId();
            publisher.publishPartitionEvent(getPartitionNumber(),
                                            getBatchStatus(),
                                            getExitStatus(),
                                            getPartitionedStepName(),
                                            getTopLevelInstanceId(),
                                            getTopLevelExecutionId(),
                                            getTopLevelStepExecutionId(),
                                            getPartitionTopicName(getBatchStatus()),
                                            correlationId);
        }
    }

    /**
     * @return the topic name for the partition-ended event with the given batch
     *         status.
     */
    private String getPartitionTopicName(BatchStatus batchStatus) {
        switch (batchStatus) {
            case STARTED:
                return BatchEventsPublisher.TOPIC_EXECUTION_PARTITION_STARTED;
            case COMPLETED:
                return BatchEventsPublisher.TOPIC_EXECUTION_PARTITION_COMPLETED;
            case STOPPED:
                return BatchEventsPublisher.TOPIC_EXECUTION_PARTITION_STOPPED;
            case FAILED:
                return BatchEventsPublisher.TOPIC_EXECUTION_PARTITION_FAILED;
            default:
                throw new IllegalStateException("Invalid BatchStatus for partition ended job event: "
                                                + batchStatus);
        }
    }

/*
 * @returns isRemotePartitoin
 */
    @Override
    public boolean isRemotePartitionDispatch() {

        return isRemoteDispatch;
    }

    /*
     * @param isRemotePartition
     */
    public void setIsRemoteDispatch(boolean isRemote) {
        this.isRemoteDispatch = isRemote;
    }

    /**
     * @return the finalStatusSent
     */
    public boolean isFinalStatusSent() {
        return finalStatusSent;
    }

    /**
     * @param finalStatusSent the finalStatusSent to set
     */
    public void setFinalStatusSent(boolean finalStatusSent) {
        this.finalStatusSent = finalStatusSent;
    }

}
