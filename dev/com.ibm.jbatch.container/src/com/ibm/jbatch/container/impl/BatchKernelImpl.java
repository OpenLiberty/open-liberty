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
package com.ibm.jbatch.container.impl;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.operations.JobExecutionAlreadyCompleteException;
import javax.batch.operations.JobExecutionNotMostRecentException;
import javax.batch.operations.JobExecutionNotRunningException;
import javax.batch.operations.JobRestartException;
import javax.batch.operations.JobStartException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.operations.NoSuchJobInstanceException;
import javax.batch.runtime.BatchStatus;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.jbatch.container.IThreadRootController;
import com.ibm.jbatch.container.RASConstants;
import com.ibm.jbatch.container.callback.IJobExecutionEndCallbackService;
import com.ibm.jbatch.container.callback.IJobExecutionStartCallbackService;
import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;
import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.container.exception.JobStoppedException;
import com.ibm.jbatch.container.execution.impl.JobExecutionHelper;
import com.ibm.jbatch.container.execution.impl.RuntimeJobExecution;
import com.ibm.jbatch.container.execution.impl.RuntimePartitionExecution;
import com.ibm.jbatch.container.execution.impl.RuntimeSplitFlowExecution;
import com.ibm.jbatch.container.execution.impl.RuntimeWorkUnitExecution;
import com.ibm.jbatch.container.persistence.jpa.JobInstanceEntity;
import com.ibm.jbatch.container.services.IBatchKernelService;
import com.ibm.jbatch.container.services.IJobXMLSource;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.services.impl.JobXMLSource;
import com.ibm.jbatch.container.util.BatchJobWorkUnit;
import com.ibm.jbatch.container.util.BatchPartitionWorkUnit;
import com.ibm.jbatch.container.util.BatchSplitFlowWorkUnit;
import com.ibm.jbatch.container.util.BatchWorkUnit;
import com.ibm.jbatch.container.util.SplitFlowConfig;
import com.ibm.jbatch.container.ws.JobStoppedOnStartException;
import com.ibm.jbatch.container.ws.PartitionPlanConfig;
import com.ibm.jbatch.container.ws.PartitionReplyQueue;
import com.ibm.jbatch.container.ws.WSJobExecution;
import com.ibm.jbatch.container.ws.WSJobInstance;
import com.ibm.jbatch.container.ws.events.BatchEventsPublisher;
import com.ibm.jbatch.jsl.model.JSLJob;
import com.ibm.jbatch.jsl.model.Step;
import com.ibm.jbatch.spi.services.IBatchConfig;
import com.ibm.jbatch.spi.services.IBatchThreadPoolService;
import com.ibm.ws.container.service.metadata.extended.MetaDataIdentifierService;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.kernel.service.utils.ServerQuiesceListener;

@Component(service = { IBatchKernelService.class, ServerQuiesceListener.class }, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" })
public class BatchKernelImpl implements IBatchKernelService, ServerQuiesceListener {

    private final static Logger logger = Logger.getLogger(BatchKernelImpl.class.getName(),
                                                          RASConstants.BATCH_MSG_BUNDLE);

    // Key is top-level job execution Id
    private final Map<Long, BatchJobWorkUnit> executingJobs = new ConcurrentHashMap<Long, BatchJobWorkUnit>();

    // Setting this up with an eye on the plans for supporting remote partitions & splitflows
    private final Map<String, SubWorkUnitComparisonHelper> executingSubWorkUnits = new ConcurrentHashMap<String, SubWorkUnitComparisonHelper>();

    private IBatchThreadPoolService executorService = null;

    private IPersistenceManagerService persistenceService = null;

    private MetaDataIdentifierService metaDataIdentifierService = null;

    private final List<IJobExecutionStartCallbackService> jobExecutionStartCallbacks = new ArrayList<IJobExecutionStartCallbackService>();

    private final List<IJobExecutionEndCallbackService> jobExecutionEndCallbacks = new ArrayList<IJobExecutionEndCallbackService>();

    private JobExecutionHelper jobExecutionHelper;

    private final Object shutdownLock = new Object();

    /**
     * For publishing job event
     */
    private BatchEventsPublisher eventsPublisher;

    public BatchKernelImpl() {}

    @Activate
    protected void activate() {
        jobExecutionHelper = new JobExecutionHelper(this);
    }

    @Deactivate
    protected void deactivate() {
        shutdown();
    }

    @Reference
    public void setIBatchThreadPoolService(IBatchThreadPoolService reference) {
        executorService = reference;
    }

    @Reference(policyOption = ReferencePolicyOption.GREEDY)
    public void setIPersistenceManagerService(IPersistenceManagerService reference, Map<String, Object> props) {
        logger.log(Level.INFO, "batch.kernel.persistence", props.get("persistenceType"));
        persistenceService = reference;
    }

    /**
     * DS injection
     */
    @Reference
    public void setMetaDataIdentifierService(MetaDataIdentifierService metaDataIdentifierService) {
        this.metaDataIdentifierService = metaDataIdentifierService;
    }

    public void unsetMetaDataIdentifierService(MetaDataIdentifierService ref) {
        if (this.metaDataIdentifierService == ref) {
            this.metaDataIdentifierService = null;
        }
    }

    /**
     * @return the persistence service
     */
    public IPersistenceManagerService getPersistenceManagerService() {
        return persistenceService;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void setJobExecutionStartCallbackService(IJobExecutionStartCallbackService reference) {
        jobExecutionStartCallbacks.add(reference);
    }

    protected void unsetJobExecutionStartCallbackService(IJobExecutionStartCallbackService reference) {
        jobExecutionStartCallbacks.remove(reference);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void setJobExecutionEndCallbackService(IJobExecutionEndCallbackService reference) {
        jobExecutionEndCallbacks.add(reference);
    }

    protected void unsetJobExecutionEndCallbackService(IJobExecutionEndCallbackService reference) {
        jobExecutionEndCallbacks.remove(reference);
    }

    /**
     * DS injection
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setEventsPublisher(BatchEventsPublisher publisher) {
        eventsPublisher = publisher;
    }

    protected void unsetEventsPublisher(BatchEventsPublisher publisher) {
        if (this.eventsPublisher == publisher) {
            eventsPublisher = publisher;
        }
    }

    /**
     *
     * @return BatchEvents
     */
    public BatchEventsPublisher getBatchEventsPublisher() {
        return this.eventsPublisher;
    }

    @Override
    public void init(IBatchConfig pgcConfig) throws BatchContainerServiceException {}

    @Override
    public void shutdown() throws BatchContainerServiceException {
        synchronized (shutdownLock) {
            stopAllActiveJobs();
            stopAllActiveSubWorkUnits();
            waitForActiveJobsAndSubJobsToStop();
        }
    }

    @Override
    public WSJobInstance createJobInstanceIntraApplication(String jobXMLName, String submitter) {

        String amc = metaDataIdentifierService.getMetaDataIdentifier(ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData());

        /*
         * Here we're removing the very first prefix from the AMC; everything up through the first hash.
         * We expect this to be one of: WEB#/EJB#/CLIENT#
         * The REST path stores the user-supplied appname/modname with no prefix and later on (in AppModuleContextService)
         * infers the prefix and prepends it itself. If we leave the prefix on here the later REST path will end up
         * with two prefixes and the REST call will fail.
         */
        int firstHash = amc.indexOf("#");
        String fixedAmc = amc.substring(firstHash + 1);

        return createJobInstance(fixedAmc, jobXMLName, submitter, null, null);
    }

    /**
     * @return a new JobInstance for the given appName and JSL file.
     *
     *         Note: Inline JSL takes precedence over JSL within .war
     */
    @Override
    public WSJobInstance createJobInstance(String appName, String jobXMLName, String submitter, String jsl, String correlationId) {

        JobInstanceEntity retMe = null;

        retMe = getPersistenceManagerService().createJobInstance(appName,
                                                                 jobXMLName,
                                                                 jsl,
                                                                 submitter,
                                                                 new Date());

        publishEvent(retMe, BatchEventsPublisher.TOPIC_INSTANCE_SUBMITTED, correlationId);

        return retMe;
    }

    /**
     * Publish jms topic if batch jms event is available
     *
     * @param objectToPublish WSJobInstance
     * @param eventToPublish
     */
    private void publishEvent(WSJobExecution objectToPublish, String eventToPublishTo, String correlationId) {
        if (eventsPublisher != null) {
            eventsPublisher.publishJobExecutionEvent(objectToPublish, eventToPublishTo, correlationId);
        }
    }

    /**
     * Publish jms topic if batch jms event is available
     *
     * @param objectToPublish WSJobInstance
     * @param eventToPublish
     */
    private void publishEvent(WSJobInstance objectToPublish, String eventToPublishTo, String correlationId) {
        if (eventsPublisher != null) {
            eventsPublisher.publishJobInstanceEvent(objectToPublish, eventToPublishTo, correlationId);
        }
    }

    /**
     * Note: this method must run under the application context.
     */
    @Override
    public Entry<Long, Future<?>> startJob(WSJobInstance jobInstance,
                                           IJobXMLSource jobXML,
                                           Properties jobParameters,
                                           long executionId) throws JobStartException {

        traceJobXML(jobXML);
        // Check BatchStatus instead of InstanceState since this narrows the window
        // of a restart changing the status after a stop has occurred.  InstanceState
        // gets changed early; whereas BatchStatus changes right before the job is executed.
        JobInstanceEntity instanceEntity = getPersistenceManagerService().getJobInstance(jobInstance.getInstanceId());
        if (instanceEntity.getBatchStatus() == BatchStatus.STOPPED) {
            // Do not continue with the execution.  Instance has been stopped.
            throw new JobStoppedOnStartException();
        }

        RuntimeJobExecution jobExecution = jobExecutionHelper.createJobStartExecution(jobInstance, jobXML, jobParameters, executionId);

        BatchJobWorkUnit batchWork = new BatchJobWorkUnit(this, jobExecution, jobExecutionStartCallbacks, jobExecutionEndCallbacks);
        registerExecutingJob(jobExecution.getTopLevelExecutionId(), batchWork);

        Future<?> futureWork = null;

        try {
            // Set server id and rest URL since this is now a viable execution.
            getPersistenceManagerService().updateJobExecutionServerIdAndRestUrlForStartingJob(jobExecution.getTopLevelExecutionId());
            futureWork = executorService.executeTask(batchWork, null);
        } catch (RuntimeException e) {
            workUnitCompleted(batchWork);
            throw e;
        } catch (JobStoppedException e) {
            workUnitCompleted(batchWork);
            throw new BatchContainerRuntimeException(e);
        }

        return new AbstractMap.SimpleEntry<Long, Future<?>>(jobExecution.getTopLevelExecutionId(), futureWork);
    }

    @Override
    public Entry<Long, Future<?>> restartJob(long executionId,
                                             Properties jobOverrideProps) throws JobRestartException, JobExecutionAlreadyCompleteException, JobExecutionNotMostRecentException, NoSuchJobExecutionException {

        long instanceId = getPersistenceManagerService().getJobInstanceIdFromExecutionId(executionId);
        JobInstanceEntity jobInstance = getPersistenceManagerService().getJobInstance(instanceId);
        return restartJobInstance(instanceId, new JobXMLSource(jobInstance.getJobXml()), jobOverrideProps, executionId);
    }

    @Override
    public Entry<Long, Future<?>> restartJobInstance(long instanceId, IJobXMLSource jobXML, Properties jobOverrideProps,
                                                     long executionId) throws JobRestartException, JobExecutionAlreadyCompleteException, JobExecutionNotMostRecentException, NoSuchJobExecutionException, NoSuchJobInstanceException {

        RuntimeJobExecution jobExecution = jobExecutionHelper.createJobRestartExecution(instanceId, jobXML, jobOverrideProps, executionId);

        BatchJobWorkUnit batchWork = new BatchJobWorkUnit(this, jobExecution, jobExecutionStartCallbacks, jobExecutionEndCallbacks);

        registerExecutingJob(jobExecution.getTopLevelExecutionId(), batchWork);

        Future<?> futureExecution = null;

        try {
            // Set server id and rest URL since this is now a viable execution.
            getPersistenceManagerService().updateJobExecutionServerIdAndRestUrlForStartingJob(jobExecution.getTopLevelExecutionId());

            futureExecution = executorService.executeTask(batchWork, null);
        } catch (RuntimeException e) {
            workUnitCompleted(batchWork);
            throw e;
        } catch (JobStoppedException e) {
            workUnitCompleted(batchWork);
            throw new BatchContainerRuntimeException(e);
        }

        // use the new execution instance in the restarted message.
        // listener can use the job instance id to correlate.
        String correlationId = jobExecution.getCorrelationId();
        publishEvent(getPersistenceManagerService().getJobExecution(jobExecution.getTopLevelExecutionId()),
                     BatchEventsPublisher.TOPIC_EXECUTION_RESTARTING, correlationId);

        return new AbstractMap.SimpleEntry<Long, Future<?>>(jobExecution.getTopLevelExecutionId(), futureExecution);

    }

    /**
     * Build a list of batch work units and set them up in STARTING state but
     * don't start them yet.
     */
    @Override
    public BatchPartitionWorkUnit createPartitionWorkUnit(PartitionPlanConfig config,
                                                          Step step,
                                                          PartitionReplyQueue partitionReplyQueue,
                                                          boolean isRemoteDispatch) {

        JSLJob partitionJobModel = ParallelStepBuilder.buildPartitionLevelJSLJob(config.getTopLevelExecutionId(),
                                                                                 config.getJobProperties(),
                                                                                 step,
                                                                                 config.getPartitionNumber());

        RuntimePartitionExecution partitionExecution = jobExecutionHelper.createPartitionExecution(config, partitionJobModel, isRemoteDispatch);

        BatchPartitionWorkUnit batchWorkUnit = new BatchPartitionWorkUnit(this, partitionExecution, config, jobExecutionStartCallbacks, jobExecutionEndCallbacks, partitionReplyQueue);

        registerExecutingSubWorkUnit(partitionExecution.getTopLevelExecutionId(), batchWorkUnit);

        return batchWorkUnit;
    }

    @Override
    public BatchSplitFlowWorkUnit createSplitFlowWorkUnit(SplitFlowConfig splitFlowConfig, JSLJob splitFlowJobModel, BlockingQueue<BatchSplitFlowWorkUnit> completedWorkQueue) {

        RuntimeSplitFlowExecution execution = jobExecutionHelper.createSplitFlowExecution(splitFlowConfig, splitFlowJobModel);

        BatchSplitFlowWorkUnit batchWork = new BatchSplitFlowWorkUnit(this, execution, completedWorkQueue, jobExecutionStartCallbacks, jobExecutionEndCallbacks);
        registerExecutingSubWorkUnit(execution.getTopLevelExecutionId(), batchWork);

        return batchWork;
    }

    @Override
    public Future<?> runPartition(BatchPartitionWorkUnit batchWork) {
        // This call is non-blocking
        return executorService.executeParallelTask(batchWork, null);
    }

    @Override
    public void runSplitFlow(BatchSplitFlowWorkUnit batchWork) {
        // This call is non-blocking
        executorService.executeParallelTask(batchWork, null);
    }

    /*
     * This errs on the side of cleaning things up too readily without issue errors.
     * Perhaps along with this we should be issuing messages in certain conditions, like
     * if we are expecting to find something in here that's not here, or if the partition-level
     * unit completes after the job-level unit has already been cleaned up (though perhaps there's nothing
     * to do in such a case since things are well on their way to ending.
     */
    @Override
    public void workUnitCompleted(BatchWorkUnit workUnit) {

        long jobExecutionId = workUnit.getRuntimeWorkUnitExecution().getTopLevelExecutionId();

        if (workUnit instanceof BatchJobWorkUnit) {

            if (executingJobs.containsKey(jobExecutionId)) {
                executingJobs.remove(jobExecutionId);
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("job completed: For jobExecId = " + jobExecutionId + ", removed job work unit");
                }
            } else {
                // Perhaps we could throw an exception, but not sure what the point would be except to generate and FFDC
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("job completed: For jobExecId = " + jobExecutionId + ", but didn't find job work unit to remove");
                }
            }

        } else {

            SubWorkUnitComparisonHelper comparisonHelper = new SubWorkUnitComparisonHelper(workUnit);

            if (executingSubWorkUnits.containsKey(comparisonHelper.comparisonKey)) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("subjob completed: For jobExecId = " + jobExecutionId + ", removed subjob work unit");
                }
                executingSubWorkUnits.remove(comparisonHelper.comparisonKey);
            } else {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("subjob completed: For jobExecId = " + jobExecutionId + ", but didn't find job work unit to remove");
                }
            }
        }
    }

    @Override
    public void stopWorkUnit(BatchWorkUnit workUnit) {
        IThreadRootController controller = workUnit.getController();
        if (controller != null) {
            controller.stop();
        }
    }

    @Override
    public void stopJob(long jobExecutionId) throws NoSuchJobExecutionException, JobExecutionNotRunningException {

        // Needs revisting once we have remote subjobs, we could only have subjobs of a given TLJ
        // running in this JVM

        BatchJobWorkUnit jobWorkUnit = executingJobs.get(jobExecutionId);
        if (jobWorkUnit == null) {
            String msg = "JobExecution with execution id of " + jobExecutionId + " is not running.";
            throw new JobExecutionNotRunningException(msg);
        }
        stopWorkUnit(jobWorkUnit);

        // Needs revisting once we have remote subjobs, we could only have subjobs of a given TLJ
        // running in this JVM

        // Just remove, don't try stopping directly.  Let the stop of the top-level job drill down
        // and stop the right executing step.
        //
        // So what is the point of even having this set?  To set up for the remote subjob case I suppose.

    }

    /*
     * stop all jobs
     * - for each TLJ: stop job
     */

    private void registerExecutingJob(long jobExecutionId, BatchJobWorkUnit workUnit) {

        if (executingJobs.containsKey(jobExecutionId)) {
            throw new IllegalStateException("Already have entry in executingJobs map for job exec id = " + jobExecutionId);
        }

        executingJobs.put(jobExecutionId, workUnit);
    }

    private void registerExecutingSubWorkUnit(long jobExecutionId, BatchWorkUnit workUnit) {

        SubWorkUnitComparisonHelper comparisonHelper = new SubWorkUnitComparisonHelper(workUnit);

        if (executingSubWorkUnits.containsKey(comparisonHelper.comparisonKey)) {
            throw new IllegalStateException("Already have entry in relatedSubWorkUnits set for job exec id = " + jobExecutionId + ", for subjob work unit: " + workUnit);
        }
        executingSubWorkUnits.put(comparisonHelper.comparisonKey, comparisonHelper);
    }

    /**
     * Stop all active executions in executingWorkUnitSets.
     */
    protected void stopAllActiveJobs() {

        for (Long jobExecutionId : executingJobs.keySet()) {
            try {
                logger.log(Level.INFO, "stopping.job.at.shutdown", jobExecutionId);
                stopJob(jobExecutionId);
            } catch (Exception e) {
                // FFDC it.  Could be the job just finished.  In any event,
                // continue on and stop the remaining jobs.
            }
        }
    }

    /**
     * Stop all active executions in executingWorkUnitSets.
     */
    protected void stopAllActiveSubWorkUnits() {

        for (String key : executingSubWorkUnits.keySet()) {
            try {
                logger.fine("Issuing stop for sub job : " + key + " because the batch component is deactivating.");
                //Do we need a null check here ?
                //since we use a concurrentHashMap, and since a stop call doesn't mark a thread completed,
                //safe to assume we won't find a null executingSubWorkUnit
                stopWorkUnit(executingSubWorkUnits.get(key).batchWorkUnit);
            } catch (Exception e) {
                // FFDC it.  Could be the job just finished.  In any event,
                // continue on and stop the remaining jobs.
            }
        }
    }

    /**
     * If jobs are still running (i.e executingWorkUnitSets is not empty),
     * then wait a second or two to allow them to finish before shutting down the
     * component. If after the wait the jobs are still running, then issue a message
     * indicating as such and allow the component to shutdown.
     *
     * We cannot hold up component shutdown because of outstanding jobs. And we
     * cannot forcibly stop a job thread. If the component is deactivating because
     * of a config change (e.g. to the persistence service), then you could have problems
     * with jobs that "straddle" the update -- i.e. the job starts with one persistence config
     * and ends with another. But there's not much we can do here, other than warn the user
     * with a message.
     */
    protected void waitForActiveJobsAndSubJobsToStop() {

        // Don't wait if nothing to wait for...
        if (executingSubWorkUnits.isEmpty() && executingJobs.isEmpty()) {
            return;
        }

        try {
            // Hard-coded 2 seconds, until we have a config property
            Thread.sleep(2 * 1000);
        } catch (InterruptedException ie) {
            // Let it interrupt
        }

        if (!executingJobs.isEmpty()) {
            logger.log(Level.INFO, "jobs.running.at.shutdown", executingJobs.keySet());
        }

        if (!executingSubWorkUnits.isEmpty()) {

            logger.fine("The following job executions were still running at the time of deactivation: " + executingSubWorkUnits.keySet().toString());
        }
    }

    protected void traceJobXML(IJobXMLSource source) {
        if (logger.isLoggable(Level.FINE)) {
            String jobXML = source.getJSLString();
            int concatLen = jobXML.length() > 200 ? 200 : jobXML.length();
            logger.fine("Starting job with JSL: " + jobXML.substring(0, concatLen) + "... truncated ...");
        }
    }

    private class SubWorkUnitComparisonHelper {

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "SubWorkUnitComparisonHelper [comparisonKey=" + comparisonKey + "]";
        }

        private final BatchWorkUnit batchWorkUnit;

        private final String comparisonKey;

        public SubWorkUnitComparisonHelper(BatchWorkUnit workUnit) {
            this.batchWorkUnit = workUnit;
            if (batchWorkUnit.getRuntimeWorkUnitExecution() == null) {
                throw new IllegalStateException("Somehow got a not-fully initialized object.");
            }
            this.comparisonKey = getComparisonKey(batchWorkUnit.getRuntimeWorkUnitExecution());
        }

        private String getComparisonKey(RuntimeWorkUnitExecution rwue) {
            if (rwue instanceof RuntimeSplitFlowExecution) {
                return rwue.getTopLevelExecutionId() + "::" + ((RuntimeSplitFlowExecution) rwue).getFlowName();
            } else if (rwue instanceof RuntimePartitionExecution) {
                return rwue.getTopLevelExecutionId() + "::" + ((RuntimePartitionExecution) rwue).getStepName() + "::" + ((RuntimePartitionExecution) rwue).getPartitionNumber();
            } else {
                throw new IllegalArgumentException("Unknown class in type hierarchy");
            }
        }

        @Override
        public boolean equals(Object o) {
            if ((o != null) && (o instanceof SubWorkUnitComparisonHelper)) {
                return this.comparisonKey.equals(((SubWorkUnitComparisonHelper) o).comparisonKey);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return comparisonKey.hashCode();

        }

    }

    /**
     * Create the BatchPartitionWorkUnit and start the sub-job partition thread.
     *
     * @return A Map Entry with BatchPartitionWorkUnit as key and a Future WorkUnit as value
     */
    @Override
    public Entry<BatchPartitionWorkUnit, Future<?>> startPartition(PartitionPlanConfig partitionPlanConfig,
                                                                   Step step,
                                                                   PartitionReplyQueue partitionReplyQueue,
                                                                   boolean isRemoteDispatch) {

        BatchPartitionWorkUnit workUnit = createPartitionWorkUnit(partitionPlanConfig, step, partitionReplyQueue, isRemoteDispatch);

        Future<?> futureWork = runPartition(workUnit);

        // TODO: issue "partition.started" message ?

        return new AbstractMap.SimpleEntry<BatchPartitionWorkUnit, Future<?>>(workUnit, futureWork);
    }

    /*
     * Gets a jobExecutionId from the SubWorkUnitComparisonHelper comparisonKey
     * If subWorkUnit is a step, key format is "jobExecutionId:StepName:PartitionNumber"
     * If subWorkUnit is a split-flow, key format is "jobExecutionId:flowName"
     *
     * @param the key used in executingSubWorkUnits.
     *
     * @return jobExecutionId
     */
    private long getJobExecutionIdFromExecutingSubWorkUnitKey(String comparisonKey) {
        try {
            return Long.parseLong(comparisonKey.split("::")[0]);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Sub Work Unit found without a parent Execution Id");
        }
    }

    @Override
    public void serverStopping() {
        shutdown();
    }

    /*
     * Retrieves the batch status of the specified job execution from the
     * in memory collection of executing jobs.
     *
     * @param the specified job to locate
     *
     * @return the batch status of the specified job, or null if not found
     */
    @Override
    public BatchStatus getBatchStatus(long jobExecutionId) {
        BatchStatus retVal = null;
        BatchWorkUnit bwu = executingJobs.get(jobExecutionId);
        if (bwu != null) {
            retVal = bwu.getRuntimeWorkUnitExecution().getBatchStatus();
            logger.finer("Returning local BatchStatus of: " + retVal);
        } else {
            logger.finer("Local BatchStatus not found, returning <null>");
        }
        return retVal;
    }

}
