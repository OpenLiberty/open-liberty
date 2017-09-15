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
import java.util.logging.Logger;

import javax.batch.runtime.BatchStatus;

import com.ibm.jbatch.container.navigator.ModelNavigator;
import com.ibm.jbatch.container.persistence.jpa.RemotableSplitFlowKey;
import com.ibm.jbatch.container.status.ExecutionStatus;
import com.ibm.jbatch.container.util.SplitFlowConfig;
import com.ibm.jbatch.container.ws.events.BatchEventsPublisher;
import com.ibm.jbatch.jsl.model.JSLJob;
import com.ibm.websphere.ras.annotation.Trivial;

public class RuntimeSplitFlowExecution extends RuntimeWorkUnitExecution {

    private final static String sourceClass = RuntimeSplitFlowExecution.class.getName();
    protected final static Logger logger = Logger.getLogger(sourceClass);

    private final String splitName;
    private final String flowName;
    private Date createTime = null;
    private Date startTime = null;
    private Date endTime = null;

    private ExecutionStatus flowStatus;

    /**
     * @param splitFlowConfig
     * @param navigator
     */
    public RuntimeSplitFlowExecution(SplitFlowConfig splitFlowConfig, ModelNavigator<JSLJob> navigator) {
        super(navigator, splitFlowConfig.getTopLevelNameInstanceExecutionInfo());
        this.splitName = splitFlowConfig.getSplitName();
        this.flowName = splitFlowConfig.getFlowName();
        this.correlationId = splitFlowConfig.getCorrelationId();
        this.type = WorkUnitType.SPLIT_FLOW;
        createTime = new Date();
    }

    public ExecutionStatus getFlowStatus() {
        return flowStatus;
    }

    public void setFlowStatus(ExecutionStatus flowStatus) {
        this.flowStatus = flowStatus;
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

        buf.append("invoking execution for a split-flow");
        buf.append("\n Split-Flow split name = ");
        buf.append(splitName);
        buf.append("\n Split-Flow flow name =");
        buf.append(flowName);
        buf.append("\n Associated Top-level JobInstance id= ");
        buf.append(getTopLevelInstanceId());
        buf.append("\n Associated Top-level JobExecution id = ");
        buf.append(getTopLevelExecutionId());
        buf.append("\n Associated Top-level Job Name = ");
        buf.append(getTopLevelJobName());;

        if (!msgType.equals(MessageType.STARTED)) {
            buf.append("\n Job Batch Status = " + getBatchStatus());
            buf.append(", Job Exit Status = " + getExitStatus());
        }
        buf.append("\n" + LogHelper.dashes);

        return buf.toString();
    }

    @Override
    public String getSplitName() {
        return splitName;
    }

    @Override
    public String getFlowName() {
        return flowName;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    @Override
    public void workStarted(Date date) {
        startTime = date;
        batchStatus = BatchStatus.STARTED;
        getPersistenceManagerService().updateSplitFlowExecution(this, batchStatus, date);
        publishStartedEvent();

    }

    @Override
    public void workStopping(Date date) {
        batchStatus = BatchStatus.STOPPING;
        getPersistenceManagerService().updateSplitFlowExecution(this, batchStatus, date);
    }

    @Override
    public void workEnded(Date date) {
        endTime = date;
        getPersistenceManagerService().updateSplitFlowExecution(this, batchStatus, date);
        publishEndedEvent();
    }

    @Override
    public void updateExecutionJobLogDir(String logDirPath) {
        RemotableSplitFlowKey key = new RemotableSplitFlowKey(getTopLevelExecutionId(), flowName);
        getPersistenceManagerService().updateSplitFlowExecutionLogDir(key, logDirPath);
    }

    /**
     * Publish started event
     */
    private void publishStartedEvent() {
        BatchEventsPublisher publisher = getBatchEventsPublisher();
        if (publisher != null) {

            publisher.publishSplitFlowEvent(getSplitName(),
                                            getFlowName(),
                                            getTopLevelInstanceId(),
                                            getTopLevelExecutionId(),
                                            BatchEventsPublisher.TOPIC_EXECUTION_SPLIT_FLOW_STARTED,
                                            correlationId);
        }
    }

    /**
     * Publish ended event
     */
    private void publishEndedEvent() {
        BatchEventsPublisher publisher = getBatchEventsPublisher();
        if (publisher != null) {

            publisher.publishSplitFlowEvent(getSplitName(),
                                            getFlowName(),
                                            getTopLevelInstanceId(),
                                            getTopLevelExecutionId(),
                                            BatchEventsPublisher.TOPIC_EXECUTION_SPLIT_FLOW_ENDED,
                                            correlationId);
        }
    }

    @Override
    public boolean isRemotePartitionDispatch() {
        // We don't have remote split-flows yet
        return false;
    }
}
