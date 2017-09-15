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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.context.JobContext;

import com.ibm.jbatch.container.artifact.proxy.ListenerFactory;
import com.ibm.jbatch.container.context.impl.JobContextImpl;
import com.ibm.jbatch.container.instance.WorkUnitDescriptor;
import com.ibm.jbatch.container.navigator.ModelNavigator;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.servicesmanager.ServicesManagerStaticAnchor;
import com.ibm.jbatch.container.ws.JoblogUtil;
import com.ibm.jbatch.container.ws.TopLevelNameInstanceExecutionInfo;
import com.ibm.jbatch.container.ws.events.BatchEventsPublisher;
import com.ibm.jbatch.jsl.model.JSLJob;
import com.ibm.jbatch.jsl.model.JSLProperties;
import com.ibm.jbatch.jsl.model.Property;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Known subclasses:
 * RuntimeJobExecution
 * RuntimePartitionExecution
 * RuntimeSplitFlowExecution
 *
 * These objects are created by BatchKernelImpl/JobExecutionHelper when starting a job execution / sub partition.
 *
 * Publishes status changes and job events under workStarted, workStopping, workEnded methods
 */
public abstract class RuntimeWorkUnitExecution implements WorkUnitDescriptor {

    private final static String sourceClass = RuntimeWorkUnitExecution.class.getName();
    protected final static Logger logger = Logger.getLogger(sourceClass);

    protected ModelNavigator<JSLJob> jobNavigator = null;
    protected String restartOnForNextExecution;

    private final TopLevelNameInstanceExecutionInfo topLevelNameInstanceExecutionInfo;

    protected ListenerFactory listenerFactory;

    protected String exitStatus = null;
    protected BatchStatus batchStatus = null;

    protected JobContext jobContext = null;
    private final Properties jobProperties;

    protected WorkUnitType type;
    protected String correlationId = null;

    public class StopLock {}

    private final StopLock stopLock = new StopLock();

    public RuntimeWorkUnitExecution(ModelNavigator<JSLJob> jobNavigator,
                                    TopLevelNameInstanceExecutionInfo topLevelNameInstanceExecutionInfo) {

        this.jobNavigator = jobNavigator;
        this.batchStatus = BatchStatus.STARTING;
        this.jobProperties = initTopLevelJobProperties();
        this.jobContext = new JobContextImpl(this);

        this.topLevelNameInstanceExecutionInfo = topLevelNameInstanceExecutionInfo;
    }

    /**
     * @return the persistence manager
     */
    protected IPersistenceManagerService getPersistenceManagerService() {
        return ServicesManagerStaticAnchor.getServicesManager().getPersistenceManagerService();
    }

    @Trivial
    protected BatchEventsPublisher getBatchEventsPublisher() {
        return ServicesManagerStaticAnchor.getServicesManager().getBatchEventsPublisher();
    }

    private Properties initTopLevelJobProperties() {

        // Should this be in the spec (that Properties is never null)?  Or is it already?
        Properties jobProperties = new Properties();

        JSLProperties jslProperties = new JSLProperties();

        if (jobNavigator.getRootModelElement() != null) {
            jslProperties = jobNavigator.getRootModelElement().getProperties();
        }
        if (jslProperties != null) { // null if not job properties defined.
            for (Property property : jslProperties.getPropertyList()) {
                jobProperties.setProperty(property.getName(), property.getValue());
            }
        }
        return jobProperties;
    }

    public JobContext getWorkUnitJobContext() {
        return jobContext;
    }

    public ModelNavigator<JSLJob> getJobNavigator() {
        return jobNavigator;
    }

    public ListenerFactory getListenerFactory() {
        return listenerFactory;
    }

    public void setListenerFactory(ListenerFactory listenerFactory) {
        this.listenerFactory = listenerFactory;
    }

    @Override
    public BatchStatus getBatchStatus() {
        return batchStatus;
    }

    @Override
    public String getExitStatus() {
        return exitStatus;
    }

    public void setBatchStatus(BatchStatus status) {
        this.batchStatus = status;
    }

    @Override
    public void setExitStatus(String status) {
        this.exitStatus = status;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
//		buf.append(" restartOnForThisExecution: " + restartOnForThisExecution);
        buf.append(" restartOnForNextExecution: " + restartOnForNextExecution);
//		buf.append(" , workUnitInternalJobName = " + workUnitInternalJobName);
//		buf.append(" , workUnitInternalExecutionId = " + workUnitInternalExecutionId);
//		buf.append("\n-----------------------\n");
//		buf.append("workUnitInternalInstanceId: \n   " + workUnitInternalInstanceId);
        return buf.toString();
    }

    /*
     * @Override
     * public long getWorkUnitInternalExecutionId() {
     * return workUnitInternalExecutionId;
     * }
     *
     * @Override
     * public long getWorkUnitInternalInstanceId() {
     * return workUnitInternalInstanceId;
     * }
     *
     * @Override
     * public String getWorkUnitInternalJobName() {
     * return workUnitInternalJobName;
     * }
     */

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.jbatch.container.execution.impl.WorkUnitDescriptor#getTopLevelJobName()
     */
    @Override
    public String getTopLevelJobName() {
        // TODO: is it valid for this to be null? or should we throw an NPE if it is?
        return topLevelNameInstanceExecutionInfo == null ? null : topLevelNameInstanceExecutionInfo.getJobName();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.jbatch.container.execution.impl.WorkUnitDescriptor#getTopLevelInstanceId()
     */
    @Override
    public long getTopLevelInstanceId() {
        return topLevelNameInstanceExecutionInfo == null ? null : topLevelNameInstanceExecutionInfo.getInstanceId();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.jbatch.container.execution.impl.WorkUnitDescriptor#getTopLevelExecutionId()
     */
    @Override
    public long getTopLevelExecutionId() {
        return topLevelNameInstanceExecutionInfo == null ? null : topLevelNameInstanceExecutionInfo.getExecutionId();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.jbatch.container.execution.impl.WorkUnitDescriptor#getTopLevelJobProperties()
     */
    @Override
    public Properties getTopLevelJobProperties() {
        return jobProperties;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.jbatch.container.execution.impl.WorkUnitDescriptor#getTopLevelNameInstanceExecutionInfo()
     */
    @Override
    public TopLevelNameInstanceExecutionInfo getTopLevelNameInstanceExecutionInfo() {
        return topLevelNameInstanceExecutionInfo;
    }

    @Override
    public WorkUnitType getWorkUnitType() {
        return type;
    }

    /**
     * This is unique for top-level jobs, and will be extended with subclass
     * implementations to trace partitions and split-flows a bit differently.
     */
    @Trivial
    public String getStepExecutionCreatedMessage(RuntimeStepExecution runtimeStepExecution) {

        StringBuilder buf = new StringBuilder("\n");
        buf.append(LogHelper.dashes);

        buf.append("For step name = ");
        buf.append(runtimeStepExecution.getStepName());
        buf.append("\n New top-level step execution id = ");
        buf.append(runtimeStepExecution.getInternalStepThreadExecutionId());
        buf.append("\n" + LogHelper.dashes);

        return buf.toString();
    }

    public String getRestartOnForNextExecution() {
        return restartOnForNextExecution;
    }

    public void setRestartOnForNextExecution(String restartOnForNextExecution) {
        this.restartOnForNextExecution = restartOnForNextExecution;
    }

    //////
    // The remainder does not get customized with subclass extensions.
    // The point of using inheritance simply for logging is that this is such a common thing to
    // search for in the log that it's worth some extra attention to ensure some consistency.
    //////

    public final void logExecutionStartingMessage() {
        logEventTopicRoot();
        String logMsg = getExecutionLogMessage(MessageType.STARTED);
        logToJobLoggerAndTraceLog(logMsg);
    }

    public final void logExecutionCompletedMessage() {
        String logMsg = getExecutionLogMessage(MessageType.COMPLETED);
        logToJobLoggerAndTraceLog(logMsg);
    }

    public final void logExecutionFailedMessage() {
        String logMsg = getExecutionLogMessage(MessageType.FAILED);
        logToJobLoggerAndTraceLog(logMsg);
    }

    public final void logExecutionStoppedMessage() {
        String logMsg = getExecutionLogMessage(MessageType.STOPPED);
        logToJobLoggerAndTraceLog(logMsg);
    }

    public void logStepExecutionCreatedMessage(RuntimeStepExecution runtimeStepExecution) {
        String logMsg = getStepExecutionCreatedMessage(runtimeStepExecution);
        logToJobLoggerAndTraceLog(logMsg);
    }

    /**
     * Logs <code>logMessage</code> to both the special job log logger and also the
     * normal classname-based logger (as a FINE-level record).
     *
     * @param logMessage
     */
    @Trivial
    public final void logToJobLoggerAndTraceLog(String logMsg) {
        Logger logger = getClassNameLogger();

        JoblogUtil.logRawMsgToJobLogAndTraceOnly(Level.FINE, logMsg, logger);
    }

    protected abstract String getExecutionLogMessage(MessageType messageType);

    protected abstract Logger getClassNameLogger();

    protected static enum MessageType {
        STARTED, COMPLETED, FAILED, STOPPED
    }

    protected final class LogHelper {

        protected final static String dashes = "==========================================================\n";

        @Trivial
        protected final StringBuilder getBeginningPart(MessageType msgType) {
            StringBuilder buf = new StringBuilder("\n");
            buf.append(dashes);

            switch (msgType) {
                case STARTED:
                    buf.append("Started ");
                    break;
                case COMPLETED:
                    buf.append("Completed ");
                    break;
                case FAILED:
                    buf.append("Exception thrown when ");
                    break;
                case STOPPED:
                    buf.append("Stopped ");
                    break;
            }
            return buf;
        }

    }

    /**
     * Default is to return null
     *
     * @return
     */
    public String getRestartOnForThisExecution() {
        return null;
    }

    public StopLock getStopLock() {
        return stopLock;
    }

    public abstract void workStarted(Date date);

    public abstract void workStopping(Date date);

    public abstract void workEnded(Date date);

    ////////////////////////////
    // Unless overridden, we'll throw an exception since
    // the method is being called at a level of the type
    // hierarchy for which it doesn't apply.
    ////////////////////////////
    @Override
    public String getFlowName() {
        throw new IllegalStateException("Method shouldn't be called for object: " + this);
    }

    @Override
    public String getSplitName() {
        throw new IllegalStateException("Method shouldn't be called for object: " + this);
    }

    @Override
    public String getPartitionedStepName() {
        throw new IllegalStateException("Method shouldn't be called for object: " + this);
    }

    @Override
    public Integer getPartitionNumber() {
        throw new IllegalStateException("Method shouldn't be called for object: " + this);
    }

    @Override
    public String getCorrelationId() {
        return correlationId;
    }

    private void logEventTopicRoot() {

        if (getBatchEventsPublisher() != null) {

            JoblogUtil.logToJobLogAndTraceOnly(Level.INFO, "info.batch.events.publish.topic",
                                               new Object[] { getBatchEventsPublisher().resolveTopicRoot(BatchEventsPublisher.TOPIC_ROOT + '/') },
                                               getClassNameLogger());

        }
    }

}
