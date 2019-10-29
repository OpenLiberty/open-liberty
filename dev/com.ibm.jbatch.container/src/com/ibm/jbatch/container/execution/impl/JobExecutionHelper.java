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

import java.io.StringReader;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.operations.JobExecutionAlreadyCompleteException;
import javax.batch.operations.JobExecutionNotMostRecentException;
import javax.batch.operations.JobRestartException;
import javax.batch.operations.JobStartException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.runtime.BatchStatus;
import javax.xml.transform.stream.StreamSource;

import com.ibm.jbatch.container.impl.BatchKernelImpl;
import com.ibm.jbatch.container.jsl.ModelResolverFactory;
import com.ibm.jbatch.container.modelresolver.PropertyResolver;
import com.ibm.jbatch.container.modelresolver.PropertyResolverFactory;
import com.ibm.jbatch.container.navigator.ModelNavigator;
import com.ibm.jbatch.container.navigator.NavigatorFactory;
import com.ibm.jbatch.container.persistence.jpa.JobExecutionEntity;
import com.ibm.jbatch.container.persistence.jpa.JobInstanceEntity;
import com.ibm.jbatch.container.persistence.jpa.RemotablePartitionKey;
import com.ibm.jbatch.container.services.IJobXMLSource;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.util.SplitFlowConfig;
import com.ibm.jbatch.container.ws.PartitionPlanConfig;
import com.ibm.jbatch.container.ws.TopLevelNameInstanceExecutionInfo;
import com.ibm.jbatch.container.ws.WSJobInstance;
import com.ibm.jbatch.container.ws.events.BatchEventsPublisher;
import com.ibm.jbatch.container.ws.impl.StringUtils;
import com.ibm.jbatch.jsl.model.JSLJob;

public class JobExecutionHelper {

    private final static String CLASSNAME = JobExecutionHelper.class.getName();
    private final static Logger logger = Logger.getLogger(CLASSNAME);
    private static final long NO_EXECUTION_ID = -1;

    /**
     * BatchKernelImpl delegates some operations to this class.
     *
     * The ref back is needed to resolve DS service refs maintained by BatchKernelImpl.
     */
    private final BatchKernelImpl batchKernelImpl;

    /**
     * CTOR. Takes a ref to BatchKernelImpl by which other batch services are obtained.
     */
    public JobExecutionHelper(BatchKernelImpl batchKernelImpl) {
        this.batchKernelImpl = batchKernelImpl;
    }

    /**
     * @return the batch persistence service (via the batch kernel service)
     */
    protected IPersistenceManagerService getPersistenceManagerService() {
        return batchKernelImpl.getPersistenceManagerService();
    }

    /**
     * Note: this method is called on the job submission thread.
     *
     * Updates the jobinstance record with the jobXML and jobname (jobid in jobxml).
     *
     * @return a new RuntimeJobExecution record, ready to be dispatched.
     */
    public RuntimeJobExecution createJobStartExecution(final WSJobInstance jobInstance,
                                                       IJobXMLSource jslSource,
                                                       Properties jobParameters,
                                                       long executionId) throws JobStartException {

        // TODO - redesign to avoid cast?
        final JobInstanceEntity jobInstanceImpl = (JobInstanceEntity) jobInstance;
        long instanceId = jobInstance.getInstanceId();

        ModelNavigator<JSLJob> navigator = createFirstExecution(jobInstanceImpl, jslSource, jobParameters);
        JobExecutionEntity jobExec = null;
        try {
            jobExec = getPersistenceManagerService().getJobExecutionMostRecent(instanceId);
            // Check to make sure the executionId belongs to the most recent execution.
            // If not, a restart may have occurred. So, fail the start.
            // Also check to make sure a stop did not come in while the start was on the queue.
            BatchStatus currentBatchStatus = jobExec.getBatchStatus();
            if (jobExec.getExecutionId() != executionId ||
                currentBatchStatus.equals(BatchStatus.STOPPING) ||
                currentBatchStatus.equals(BatchStatus.STOPPED)) {
                throw new JobStartException();
            }

        } catch (IllegalStateException ie) {
            // If no execution exists, request came from old dispatch path.
            jobExec = getPersistenceManagerService().createJobExecution(instanceId, jobParameters, new Date());
            BatchEventsPublisher eventsPublisher = batchKernelImpl.getBatchEventsPublisher();
            if (eventsPublisher != null) {
                String correlationId = getCorrelationId(jobParameters);
                eventsPublisher.publishJobExecutionEvent(jobExec, BatchEventsPublisher.TOPIC_EXECUTION_STARTING, correlationId);
            }
        }

        TopLevelNameInstanceExecutionInfo topLevelInfo = new TopLevelNameInstanceExecutionInfo(jobInstanceImpl.getJobName(), instanceId, jobExec.getExecutionId());

        return new RuntimeJobExecution(topLevelInfo, jobParameters, navigator);
    }

    public RuntimeJobExecution createJobRestartExecution(long jobInstanceId, IJobXMLSource jobXML, Properties restartJobParameters,
                                                         long executionId) throws JobRestartException, JobExecutionAlreadyCompleteException, JobExecutionNotMostRecentException, NoSuchJobExecutionException {

        JobInstanceEntity jobInstance = getPersistenceManagerService().getJobInstance(jobInstanceId);

        // Check if the job XML has not been persisted yet. Could happen if job was stopped before
        // any executions were created.
        if (StringUtils.isEmpty(jobInstance.getJobXml())) {
            createFirstExecution(jobInstance, jobXML, restartJobParameters);
        }
        //Cache this away since we're going to null this out with the new execution
        String restartOnFromLastExecution = jobInstance.getRestartOn();

        ModelNavigator<JSLJob> navigator = getResolvedJobNavigator(jobInstance.getJobXml(), restartJobParameters);

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("On restart execution with jobInstance Id = " + jobInstanceId + ", batchStatus = " + jobInstance.getBatchStatus().name());
        }

        // TODO: we never check the state of the most-recent executionId. if it's still running (STARTED),
        //       then we'll end up creating an invalid execution record for the restart.  the invalid execution record will forever remain in state STARTING.
        // Timestamp now = new Timestamp(System.currentTimeMillis());
        JobExecutionEntity newExecution = null;
        if (executionId == NO_EXECUTION_ID) {
            //Old dispatch path.  Execution record has not yet been created.
            newExecution = getPersistenceManagerService().createJobExecution(jobInstanceId, restartJobParameters, new Date());
            BatchEventsPublisher eventsPublisher = batchKernelImpl.getBatchEventsPublisher();
            if (eventsPublisher != null) {
                String correlationId = getCorrelationId(restartJobParameters);
                eventsPublisher.publishJobExecutionEvent(newExecution, BatchEventsPublisher.TOPIC_EXECUTION_STARTING, correlationId);
            }

        } else {
            newExecution = getPersistenceManagerService().getJobExecutionMostRecent(jobInstanceId);
            // Check to make sure the executionId belongs to the most recent execution.
            // If not, another restart may have occurred. So, fail this restart.
            // Also check to make sure a stop did not come in while the start was on the queue.
            BatchStatus currentBatchStatus = newExecution.getBatchStatus();
            if (newExecution.getExecutionId() != executionId ||
                currentBatchStatus.equals(BatchStatus.STOPPING) ||
                currentBatchStatus.equals(BatchStatus.STOPPED)) {
                throw new JobRestartException();
            }
        }
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("On restartExecution got new execution id = " + newExecution.getExecutionId());
        }

        TopLevelNameInstanceExecutionInfo topLevelInfo = new TopLevelNameInstanceExecutionInfo(navigator.getRootModelElement().getId(), jobInstanceId, newExecution.getExecutionId());

        return new RuntimeJobExecution(topLevelInfo, restartJobParameters, restartOnFromLastExecution, navigator);
    }

    public ModelNavigator<JSLJob> createFirstExecution(JobInstanceEntity jobInstance, IJobXMLSource jslSource, Properties jobParameters) {

        StreamSource jslStream = null;
        if (!StringUtils.isEmpty(jobInstance.getJobXml())) {
            jslStream = new StreamSource(new StringReader(jobInstance.getJobXml()));
        } else {
            jslStream = jslSource.getJSLStreamSource();
        }

        JSLJob jobModel = ModelResolverFactory.createJobResolver().resolveModel(jslStream);
        ModelNavigator<JSLJob> navigator = getResolvedJobNavigator(jobModel, jobParameters, false);
        String jobName = navigator.getRootModelElement().getId();
        String jobXmlString = jslSource.getJSLString();

        jobInstance.setJobName(jobName);
        jobInstance.setJobXml(jobXmlString);

        getPersistenceManagerService().updateJobInstanceWithJobNameAndJSL(jobInstance.getInstanceId(),
                                                                          jobName,
                                                                          jobXmlString);
        return navigator;

    }

    public RuntimeSplitFlowExecution createSplitFlowExecution(SplitFlowConfig splitFlowConfig, JSLJob splitFlowJobModel) {

        getPersistenceManagerService().createSplitFlowExecution(splitFlowConfig.getRemotableSplitFlowKey(), new Date());

        ModelNavigator<JSLJob> navigator = getResolvedSplitFlowNavigator(splitFlowJobModel);

        return new RuntimeSplitFlowExecution(splitFlowConfig, navigator);
    }

    public RuntimePartitionExecution createPartitionExecution(PartitionPlanConfig partitionPlanConfig,
                                                              JSLJob partitionJobModel,
                                                              boolean isRemoteDispatch) throws JobStartException {

        RemotablePartitionKey partitionKey = new RemotablePartitionKey(partitionPlanConfig.getTopLevelNameInstanceExecutionInfo().getExecutionId(), partitionPlanConfig.getStepName(), partitionPlanConfig.getPartitionNumber());

        ModelNavigator<JSLJob> navigator = getResolvedPartitionNavigator(partitionJobModel, partitionPlanConfig.getPartitionPlanProperties());

        return new RuntimePartitionExecution(partitionPlanConfig, navigator, isRemoteDispatch);
    }

    /*
     * Find the correlation id if it has been passed.
     */
    private String getCorrelationId(Properties jobParameters) {

        if (jobParameters != null) {
            return jobParameters.getProperty("com_ibm_ws_batch_events_correlationId", null);
        } else {
            return null;
        }
    }

    private ModelNavigator<JSLJob> getResolvedJobNavigator(String jobXml, Properties jobParameters) {

        JSLJob jobModel = ModelResolverFactory.createJobResolver().resolveModel(new StreamSource(new StringReader(jobXml)));
        return getResolvedJobNavigator(jobModel, jobParameters, false);
    }

    private ModelNavigator<JSLJob> getResolvedJobNavigator(JSLJob jobModel, Properties jobParameters, boolean isPartitionedStep) {

        PropertyResolver<JSLJob> propResolver = PropertyResolverFactory.createJSLJobPropertyResolver(isPartitionedStep);
        propResolver.substituteProperties(jobModel, jobParameters);

        return NavigatorFactory.createJobNavigator(jobModel);
    }

    private ModelNavigator<JSLJob> getResolvedPartitionNavigator(JSLJob jobModel, Properties partitionPlanProperties) {
        return getResolvedJobNavigator(jobModel, partitionPlanProperties, true);
    }

    private ModelNavigator<JSLJob> getResolvedSplitFlowNavigator(JSLJob jobModel) {
        return getResolvedJobNavigator(jobModel, null, false);
    }

}
