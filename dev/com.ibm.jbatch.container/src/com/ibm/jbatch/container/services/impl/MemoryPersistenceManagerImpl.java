/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.jbatch.container.services.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.operations.JobRestartException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.operations.NoSuchJobInstanceException;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.StepExecution;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.jbatch.container.RASConstants;
import com.ibm.jbatch.container.exception.BatchIllegalJobStatusTransitionException;
import com.ibm.jbatch.container.exception.ExecutionAssignedToServerException;
import com.ibm.jbatch.container.exception.JobStoppedException;
import com.ibm.jbatch.container.exception.PersistenceException;
import com.ibm.jbatch.container.execution.impl.RuntimeStepExecution;
import com.ibm.jbatch.container.persistence.jpa.JobExecutionEntity;
import com.ibm.jbatch.container.persistence.jpa.JobExecutionEntityV2;
import com.ibm.jbatch.container.persistence.jpa.JobInstanceEntity;
import com.ibm.jbatch.container.persistence.jpa.JobInstanceEntityV2;
import com.ibm.jbatch.container.persistence.jpa.RemotablePartitionEntity;
import com.ibm.jbatch.container.persistence.jpa.RemotablePartitionKey;
import com.ibm.jbatch.container.persistence.jpa.RemotableSplitFlowEntity;
import com.ibm.jbatch.container.persistence.jpa.RemotableSplitFlowKey;
import com.ibm.jbatch.container.persistence.jpa.StepThreadExecutionEntity;
import com.ibm.jbatch.container.persistence.jpa.StepThreadInstanceEntity;
import com.ibm.jbatch.container.persistence.jpa.StepThreadInstanceKey;
import com.ibm.jbatch.container.persistence.jpa.TopLevelStepExecutionEntity;
import com.ibm.jbatch.container.persistence.jpa.TopLevelStepInstanceEntity;
import com.ibm.jbatch.container.persistence.jpa.TopLevelStepInstanceKey;
import com.ibm.jbatch.container.services.IJPAQueryHelper;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.util.WSStepThreadExecutionAggregateImpl;
import com.ibm.jbatch.container.ws.BatchLocationService;
import com.ibm.jbatch.container.ws.InstanceState;
import com.ibm.jbatch.container.ws.RemotablePartitionState;
import com.ibm.jbatch.container.ws.WSPartitionStepThreadExecution;
import com.ibm.jbatch.container.ws.WSStepThreadExecutionAggregate;
import com.ibm.jbatch.container.ws.WSTopLevelStepExecution;
import com.ibm.jbatch.spi.services.IBatchConfig;

/**
 * Note: MemoryPersistenceManagerImpl is ranked lower than JPAPersistenceManagerImpl
 * so if they're both activated, JPA should take precedence. Note that all @Reference
 * injectors of IPersistenceManagerService should set the GREEDY option so that they
 * always get injected with JPA over Memory if it's available.
 */
@Component(service = IPersistenceManagerService.class, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM",
                                                                                                                      "service.ranking:Integer=10",
                                                                                                                      "persistenceType=In-Memory" })
public class MemoryPersistenceManagerImpl extends AbstractPersistenceManager implements IPersistenceManagerService {

    private final static Logger logger = Logger.getLogger(MemoryPersistenceManagerImpl.class.getName(),
                                                          RASConstants.BATCH_MSG_BUNDLE);

    /**
     * For resolving the batch REST url and serverId of this server.
     */
    private BatchLocationService batchLocationService;

    /**
     * In memory data stores.
     * Enforcing batch id rule across memory & JPA:
     * IDs must to be >= 1
     */
    static class Data {
        protected final AtomicLong jobInstanceIdGenerator = new AtomicLong(1);
        protected final AtomicLong executionInstanceIdGenerator = new AtomicLong(1);
        protected final AtomicLong stepExecutionIdGenerator = new AtomicLong(1);

        protected final Map<Long, JobInstanceEntity> jobInstanceData = new ConcurrentHashMap<Long, JobInstanceEntity>();
        protected final Map<Long, JobExecutionEntity> executionInstanceData = new ConcurrentHashMap<Long, JobExecutionEntity>();
        protected final Map<Long, StepThreadExecutionEntity> stepExecutionInstanceData = new ConcurrentHashMap<Long, StepThreadExecutionEntity>();
        protected final Map<StepThreadInstanceKey, StepThreadInstanceEntity> stepThreadInstanceData = new ConcurrentHashMap<StepThreadInstanceKey, StepThreadInstanceEntity>();
        protected final Map<RemotablePartitionKey, RemotablePartitionEntity> partitionData = new ConcurrentHashMap<RemotablePartitionKey, RemotablePartitionEntity>();
        protected final Map<RemotableSplitFlowKey, RemotableSplitFlowEntity> splitFlowData = new ConcurrentHashMap<RemotableSplitFlowKey, RemotableSplitFlowEntity>();
    }

    /**
     * Global static data. This GLOBAL_DATA object survives across restarts
     * of the MemoryPersistenceManagerImpl service. So, e.g, if this service
     * was used for a while, then the batch component was disabled and re-enabled,
     * the newly re-enabled MemoryPersistenceManagerImpl service instance would use
     * the same global data object as the previously disabled instance. This is
     * what we want.
     */
    private static final Data GLOBAL_DATA = new Data();

    private Data data;

    /**
     * DS activate
     */
    @Activate
    protected void activate(ComponentContext context, Map<String, Object> config) {

        logger.log(Level.INFO, "persistence.service.status", new Object[] { "In-Memory", "activated" });

        data = GLOBAL_DATA;

        // TODO:
        // int maxSize = (Integer) config.get("maxJobInstanceRecords");
    }

    /**
     * DS deactivate
     */
    @Deactivate
    protected void deactivate() {
        logger.log(Level.INFO, "persistence.service.status", new Object[] { "In-Memory", "deactivated" });
    }

    /**
     * DS injection
     */
    @Reference
    protected void setBatchLocationService(BatchLocationService batchLocationService) {
        this.batchLocationService = batchLocationService;
    }

    @Override
    public void init(IBatchConfig batchConfig) {}

    @Override
    public void shutdown() {}

    @Override
    public JobInstanceEntity createJobInstance(String appName, String jobXMLName, String submitter, Date createTime) {
        return createJobInstance(appName, jobXMLName, null, submitter, createTime);
    }

    @Override
    public JobInstanceEntity createJobInstance(String appName, String jobXMLName, String jsl, String submitter, Date createTime) {

        final JobInstanceEntity jobInstance = new JobInstanceEntityV2(data.jobInstanceIdGenerator.getAndIncrement());
        jobInstance.setAmcName(appName);
        jobInstance.setJobXmlName(jobXMLName);
        jobInstance.setJobXml(jsl);
        jobInstance.setSubmitter(submitter);
        jobInstance.setCreateTime(createTime);
        jobInstance.setLastUpdatedTime(createTime);
        jobInstance.setInstanceState(InstanceState.SUBMITTED);
        jobInstance.setBatchStatus(BatchStatus.STARTING); // Not sure how important the batch status is, the instance state is more important.  I guess we'll set it.
        data.jobInstanceData.put(jobInstance.getInstanceId(), jobInstance);
        return jobInstance;
    }

    @Override
    public JobInstanceEntity getJobInstance(long jobInstanceId) throws NoSuchJobInstanceException {

        final JobInstanceEntity jobInstance = data.jobInstanceData.get(jobInstanceId);
        if (jobInstance == null) {
            throw new NoSuchJobInstanceException("No job instance found for id = " + jobInstanceId);
        }
        return jobInstance;
    }

    @Override
    public JobInstanceEntity getJobInstanceFromExecutionId(long jobExecutionId) throws NoSuchJobExecutionException {
        final JobExecutionEntity jobExecution = data.executionInstanceData.get(jobExecutionId);
        if (jobExecution == null) {
            throw new NoSuchJobExecutionException("No job execution found for id = " + jobExecutionId);
        }
        return jobExecution.getJobInstance();
    }

    @Override
    public List<JobInstanceEntity> getJobInstances(String jobName, int start, int count) {
        return getJobInstances(jobName, null, start, count);
    }

    @Override
    public List<JobInstanceEntity> getJobInstances(String jobName, String submitter, int start, int count) {
        //apache
        final List<JobInstanceEntity> out = new LinkedList<JobInstanceEntity>();
        for (final JobInstanceEntity jobInstance : data.jobInstanceData.values()) {
            if (jobInstance.getJobName() != null && jobInstance.getJobName().equals(jobName)) {
                final boolean hasTag = submitter != null;
                if (!hasTag || submitter.equals(jobInstance.getSubmitter())) {
                    out.add(jobInstance);
                }
            }
        }

        // sorting could be optimized a bit but is it necessary for this impl?
        Collections.sort(out, ReverseJobInstanceCreationDateComparator.INSTANCE);

        if (start + count > out.size()) {
            return out.subList(start, out.size());
        } else {
            return out.subList(start, start + count);
        }
    }

    @Override
    public List<JobInstanceEntity> getJobInstances(int page, int pageSize) {
        return getJobInstances(page, pageSize, null);
    }

    @Override
    public List<JobInstanceEntity> getJobInstances(int page, int pageSize, String user) {
        // sql used:
        // SELECT A.jobinstanceid, A.name, A.apptag, A.appname, A.jobxmlname, B.batchstatus"
        // + " FROM {schema}.{tablePrefix}jobinstancedata A"
        // + " LEFT OUTER JOIN {schema}.{tablePrefix}jobstatus B"
        // + " ON A.jobinstanceid = B.jobinstanceid"
        // + " WHERE " + userQuery + "A." + TOP_LEVEL_JOB_ONLY_NAME_FILTER) );

        List<JobInstanceEntity> jobInstancesMatched = new ArrayList<JobInstanceEntity>();

        List<JobInstanceEntity> jobInstances = new ArrayList<JobInstanceEntity>(data.jobInstanceData.values());
        Collections.sort(jobInstances, ReverseJobInstanceCreationDateComparator.INSTANCE);

        for (JobInstanceEntity jobInstance : jobInstances) {

            if (user != null) {
                if ((user.equals(jobInstance.getSubmitter()))) {
                    // put in list to be returned
                    jobInstancesMatched.add(jobInstance);
                }
            } else {
                // also put in list - no user specified so this is good to go
                jobInstancesMatched.add(jobInstance);
            }
        }
        return readJobInstancesPage(jobInstancesMatched, page, pageSize);
    }

    @Override
    public Set<String> getJobNamesSet() {
        return getJobNamesSet(null);
    }

    @Override
    public Set<String> getJobNamesSet(String submitter) {
        final Set<String> out = new HashSet<String>();
        final boolean hasTag = submitter != null;
        for (final JobInstanceEntity jobInstance : data.jobInstanceData.values()) {
            if (!hasTag || submitter.equals(jobInstance.getSubmitter())) {
                //Assume jobInstance is invalid and ignore if null jobName
                if (jobInstance.getJobName() != null) {
                    out.add(jobInstance.getJobName());
                }
            }
        }
        return out;
    }

    @Override
    public int getJobInstanceCount(String jobName) {
        return getJobInstanceCount(jobName, null);
    }

    @Override
    public int getJobInstanceCount(String jobName, String submitter) {
        //apache
        final boolean hasTag = submitter != null;
        int i = 0;
        for (final JobInstanceEntity jobInstance : data.jobInstanceData.values()) {
            //Assume jobInstance is invalid and ignore if null jobName
            if (jobInstance.getJobName() != null) {
                if (jobInstance.getJobName().equals(jobName) && (!hasTag || submitter.equals(jobInstance.getSubmitter()))) {
                    i++;
                }
            }
        }
        return i;
    }

    @Override
    public JobInstanceEntity updateJobInstanceWithInstanceState(long jobInstanceId, InstanceState state, Date lastUpdated) {
        JobInstanceEntity jobInstance = data.jobInstanceData.get(jobInstanceId);

        try {
            verifyStateTransitionIsValid(jobInstance, state);
        } catch (BatchIllegalJobStatusTransitionException e) {
            throw new PersistenceException(e);
        }

        jobInstance.setInstanceState(state);
        jobInstance.setLastUpdatedTime(lastUpdated);
        return jobInstance;
    }

    @Override
    public JobInstance updateJobInstanceOnRestart(long jobInstanceId, Date lastUpdated) {
        JobInstanceEntity jobInstance = data.jobInstanceData.get(jobInstanceId);
        if ((jobInstance.getInstanceState() == InstanceState.STOPPED) ||
            (jobInstance.getInstanceState() == InstanceState.FAILED)) {

            try {
                verifyStateTransitionIsValid(jobInstance, InstanceState.SUBMITTED);
            } catch (BatchIllegalJobStatusTransitionException e) {
                throw new PersistenceException(e);
            }

            jobInstance.setInstanceState(InstanceState.SUBMITTED);
            jobInstance.setBatchStatus(BatchStatus.STARTING);
            jobInstance.setLastUpdatedTime(lastUpdated);
        } else {
            String msg = "The job instance " + jobInstanceId + " cannot be restarted because it is still in a non-final state.";
            throw new JobRestartException(msg);
        }
        return jobInstance;
    }

    @Override
    public JobInstance updateJobInstanceNullOutRestartOn(long jobInstanceId) {
        JobInstanceEntity jobInstance = data.jobInstanceData.get(jobInstanceId);
        jobInstance.setRestartOn(null);
        return jobInstance;
    }

    @Override
    public JobInstance updateJobInstanceWithRestartOn(long jobInstanceId, String restartOn) {
        JobInstanceEntity jobInstance = data.jobInstanceData.get(jobInstanceId);
        jobInstance.setRestartOn(restartOn);
        return jobInstance;
    }

    @Override
    public JobInstance updateJobInstanceWithJobNameAndJSL(long instanceId, String jobName, String jobXml) {
        JobInstanceEntity jobInstance = data.jobInstanceData.get(instanceId);
        jobInstance.setJobName(jobName);
        jobInstance.setJobXml(jobXml);
        return jobInstance;
    }

    @Override
    public JobExecution updateJobExecutionAndInstanceOnStarted(long jobExecutionId, Date startedTime) throws NoSuchJobExecutionException {
        JobExecutionEntity exec = getJobExecution(jobExecutionId);

        try {
            verifyStatusTransitionIsValid(exec, BatchStatus.STARTED);
            verifyStateTransitionIsValid(exec.getJobInstance(), InstanceState.DISPATCHED);
        } catch (BatchIllegalJobStatusTransitionException e) {
            throw new PersistenceException(e);
        }

        exec.setBatchStatus(BatchStatus.STARTED);
        exec.getJobInstance().setInstanceState(InstanceState.DISPATCHED);
        exec.getJobInstance().setBatchStatus(BatchStatus.STARTED);
        exec.getJobInstance().setLastUpdatedTime(startedTime);
        exec.setStartTime(startedTime);
        exec.setLastUpdatedTime(startedTime);
        return exec;
    }

    @Override
    public JobExecution updateJobExecutionAndInstanceOnStopBeforeServerAssigned(long jobExecutionId,
                                                                                Date lastUpdatedTime) throws NoSuchJobExecutionException, ExecutionAssignedToServerException {
        JobExecutionEntity exec = getJobExecution(jobExecutionId);
        if (exec.getServerId().equals("")) {

            try {
                verifyStatusTransitionIsValid(exec, BatchStatus.STOPPED);
            } catch (BatchIllegalJobStatusTransitionException e) {
                throw new PersistenceException(e);
            }

            exec.setBatchStatus(BatchStatus.STOPPED);
            exec.getJobInstance().setInstanceState(InstanceState.STOPPED);
            exec.getJobInstance().setBatchStatus(BatchStatus.STOPPED);
            exec.getJobInstance().setLastUpdatedTime(lastUpdatedTime);
            exec.setLastUpdatedTime(lastUpdatedTime);
        } else {
            String msg = "Job execution " + jobExecutionId + " is in an invalid state";
            throw new ExecutionAssignedToServerException(msg);
        }
        return exec;
    }

    @Override
    public JobExecution updateJobExecutionAndInstanceOnStatusChange(long jobExecutionId, BatchStatus newBatchStatus, Date updateTime) throws NoSuchJobExecutionException {
        JobExecutionEntity exec = getJobExecution(jobExecutionId);

        try {
            verifyStatusTransitionIsValid(exec, newBatchStatus);
        } catch (BatchIllegalJobStatusTransitionException e) {
            throw new PersistenceException(e);
        }

        exec.setBatchStatus(newBatchStatus);
        exec.getJobInstance().setBatchStatus(newBatchStatus);
        exec.getJobInstance().setLastUpdatedTime(updateTime);
        exec.setLastUpdatedTime(updateTime);

        //Trace for testing and debug.
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest("JobExecution : " + Long.toString(jobExecutionId) + " batchStatus updated : " + newBatchStatus.toString());
        }
        return exec;
    }

    @Override
    public JobExecution updateJobExecutionAndInstanceOnEnd(long jobExecutionId, BatchStatus finalBatchStatus, String finalExitStatus,
                                                           Date endTime) throws NoSuchJobExecutionException {
        JobExecutionEntity exec = getJobExecution(jobExecutionId);

        try {
            verifyStatusTransitionIsValid(exec, finalBatchStatus);
        } catch (BatchIllegalJobStatusTransitionException e) {
            throw new PersistenceException(e);
        }

        exec.setBatchStatus(finalBatchStatus);
        exec.getJobInstance().setBatchStatus(finalBatchStatus);
        exec.setExitStatus(finalExitStatus);
        exec.getJobInstance().setExitStatus(finalExitStatus);
        exec.getJobInstance().setLastUpdatedTime(endTime);
        // set the state to be the same value as the batchstatus
        // Note: we only want to do this is if the batchStatus is one of the "done" statuses.
        if (isFinalBatchStatus(finalBatchStatus)) {
            InstanceState newInstanceState = InstanceState.valueOf(finalBatchStatus.toString());
            exec.getJobInstance().setInstanceState(newInstanceState);
        }
        exec.setLastUpdatedTime(endTime);
        exec.setEndTime(endTime);
        return exec;
    }

    @Override
    public JobExecutionEntity createJobExecution(final long jobInstanceId, Properties jobParameters, Date createTime) {

        // put new JobOperatorJobExecution obj into the executions table

        JobExecutionEntity execution = new JobExecutionEntityV2(data.executionInstanceIdGenerator.getAndIncrement());
        execution.setCreateTime(createTime);
        execution.setLastUpdatedTime(createTime);
        execution.setBatchStatus(BatchStatus.STARTING);
        execution.setJobParameters(jobParameters);
        execution.setServerId(batchLocationService.getServerId());
        execution.setRestUrl(batchLocationService.getBatchRestUrl());

        final JobInstanceEntity jobInstance = getJobInstance(jobInstanceId);

        // The number of executions previously will also conveniently be the index of this, the next execution
        // (given that numbering starts at 0).
        int currentNumExecutionsPreviously = jobInstance.getNumberOfExecutions();
        execution.setExecutionNumberForThisInstance(currentNumExecutionsPreviously);
        jobInstance.setNumberOfExecutions(currentNumExecutionsPreviously + 1);
        data.executionInstanceData.put(execution.getExecutionId(), execution);

        // Link in each direction
        jobInstance.getJobExecutions().add(0, execution);
        execution.setJobInstance(jobInstance);

        return execution;
    }

    @Override
    public JobExecutionEntity getJobExecution(long jobExecutionId) throws NoSuchJobExecutionException {
        final JobExecutionEntity jobExecution = data.executionInstanceData.get(jobExecutionId);
        if (jobExecution == null) {
            throw new NoSuchJobExecutionException("No job execution found for id = " + jobExecutionId);
        }
        return jobExecution;
    }

    /*
     * @return The executions are ordered in sequence, from most-recent to least-recent.
     * The container keeps its own order and does not depend on execution id or creation time to order these.
     */
    @Override
    public List<JobExecutionEntity> getJobExecutionsFromJobInstanceId(long jobInstanceId) throws NoSuchJobInstanceException {

        JobInstanceEntity instance = getJobInstance(jobInstanceId);
        List<JobExecutionEntity> executions = new ArrayList<JobExecutionEntity>(instance.getJobExecutions());

        // sorting could be optimized a bit but is it necessary for this impl?
        Collections.sort(executions, ReverseJobExecutionSequenceForOneInstanceComparator.INSTANCE);

        return executions;
    }

    @Override
    public List<Long> getJobExecutionsRunning(String jobName) {
        List<Long> out = new ArrayList<Long>();
        List<JobExecutionEntity> entities = new ArrayList<JobExecutionEntity>();

        for (final JobInstanceEntity jobInstance : data.jobInstanceData.values()) {
            //Assume jobInstance is invalid and ignore if null jobName
            if (jobInstance.getJobName() != null) {
                if (jobInstance.getJobName().equals(jobName)) {
                    for (JobExecutionEntity exec : jobInstance.getJobExecutions()) {
                        if (RUNNING_STATUSES.contains(exec.getBatchStatus())) {
                            entities.add(exec);
                        }
                    }
                }
            }
        }

        Collections.sort(entities, ReverseJobExecutionCreationDateComparator.INSTANCE);

        for (JobExecutionEntity e : entities) {
            out.add(e.getExecutionId());
        }

        return out;
    }

    /**
     * @return a job execution matching the job instance id and job execution sequence number
     */
    @Override
    public JobExecutionEntity getJobExecutionFromJobExecNum(long jobInstanceId, int jobExecNum) throws NoSuchJobInstanceException, IllegalArgumentException {

        // throws NoSuchJobInstanceException
        JobInstanceEntity instance = getJobInstance(jobInstanceId);

        List<JobExecutionEntity> executions = new ArrayList<JobExecutionEntity>(instance.getJobExecutions());

        for (JobExecutionEntity jobExecution : executions) {
            if (jobExecution.getExecutionNumberForThisInstance() == jobExecNum) {
                return jobExecution;
            }
        }

        throw new IllegalArgumentException("Didn't find any job execution entries at job instance id: "
                                           + jobInstanceId + ", job execution sequence number: " + jobExecNum);
    }

    @Override
    public JobExecutionEntity updateJobExecutionLogDir(long jobExecutionId, String logDirPath) throws NoSuchJobExecutionException {
        JobExecutionEntity exec = getJobExecution(jobExecutionId);
        exec.setLogpath(logDirPath);
        return exec;
    }

    @Override
    public JobExecutionEntity updateJobExecutionServerIdAndRestUrlForStartingJob(long jobExecutionId) throws NoSuchJobExecutionException, JobStoppedException {
        JobExecutionEntity exec = getJobExecution(jobExecutionId);
        if (exec.getBatchStatus() == BatchStatus.STARTING) {
            exec.setServerId(batchLocationService.getServerId());
            exec.setRestUrl(batchLocationService.getBatchRestUrl());
        } else {
            String msg = "No job execution found for id = " + jobExecutionId + " and status = STARTING";
            throw new JobStoppedException(msg);
        }
        return exec;
    }

    @Override
    public TopLevelStepExecutionEntity createTopLevelStepExecutionAndNewThreadInstance(long jobExecutionId, StepThreadInstanceKey instanceKey, boolean isPartitioned) {

        // 1. Find related objects

        // OK, exception handling is slightly different than JPA, we're throwing a different unchecked exception, but we didn't
        // document the exact exception anyway so we'll go with it.
        JobInstanceEntity jobInstance = getJobInstance(instanceKey.getJobInstance());
        JobExecutionEntity jobExecution = getJobExecution(jobExecutionId);

        // 2. Construct and initalize new entity instances
        //   Note some important initialization (e.g. batch status = STARTING and startcount = 1), is done in the constructors
        final TopLevelStepInstanceEntity stepInstance = new TopLevelStepInstanceEntity(jobInstance, instanceKey.getStepName(), isPartitioned);
        long newStepExecutionId = data.stepExecutionIdGenerator.getAndIncrement();
        final TopLevelStepExecutionEntity stepExecution = new TopLevelStepExecutionEntity(newStepExecutionId, jobExecution, instanceKey.getStepName(), isPartitioned);

        // 3. Update the relationships that didn't get updated in constructors
        jobInstance.getStepThreadInstances().add(stepInstance);
        stepInstance.setLatestStepThreadExecution(stepExecution);
        jobExecution.getStepThreadExecutions().add(stepExecution);

        // 4. Persist
        data.stepExecutionInstanceData.put(newStepExecutionId, stepExecution);
        data.stepThreadInstanceData.put(instanceKey, stepInstance);

        return stepExecution;
    }

    @Override
    public StepThreadExecutionEntity createPartitionStepExecutionAndNewThreadInstance(long jobExecutionId, StepThreadInstanceKey instanceKey, boolean isRemoteDispatch) {

        // 1. Find related objects

        // OK, exception handling is slightly different than JPA, we're throwing a different unchecked exception, but we didn't
        // document the exact exception anyway so we'll go with it.
        final JobInstanceEntity jobInstance = getJobInstance(instanceKey.getJobInstance());
        final JobExecutionEntity jobExecution = getJobExecution(jobExecutionId);
        final TopLevelStepExecutionEntity topLevelStepExecution = getStepExecutionTopLevelFromJobExecutionIdAndStepName(jobExecutionId, instanceKey.getStepName());

        // 2. Construct and initalize new entity instances
        //   Note some important initialization (e.g. batch status = STARTING and startcount = 1), is done in the constructors
        final StepThreadInstanceEntity stepInstance = new StepThreadInstanceEntity(jobInstance, instanceKey.getStepName(), instanceKey.getPartitionNumber());
        long newStepExecutionId = data.stepExecutionIdGenerator.getAndIncrement();
        final StepThreadExecutionEntity stepExecution = new StepThreadExecutionEntity(newStepExecutionId, jobExecution, instanceKey.getStepName(), instanceKey.getPartitionNumber());

        // 3. Update the relationships that didn't get updated in constructors
        jobInstance.getStepThreadInstances().add(stepInstance);
        stepInstance.setLatestStepThreadExecution(stepExecution);
        jobExecution.getStepThreadExecutions().add(stepExecution);
        stepExecution.setTopLevelStepExecution(topLevelStepExecution);
        topLevelStepExecution.getTopLevelAndPartitionStepExecutions().add(stepExecution);

        /*
         * 222050 - Backout 205106
         * RemotablePartitionEntity remotablePartition = null;
         * if (isRemoteDispatch) {
         * RemotablePartitionKey remotablePartitionKey = new RemotablePartitionKey(jobExecution.getExecutionId(), instanceKey.getStepName(), instanceKey.getPartitionNumber());
         * remotablePartition = data.partitionData.get(remotablePartitionKey);
         * remotablePartition.setStepExecution(stepExecution);
         * stepExecution.setRemotablePartition(remotablePartition);
         * }
         */

        // 4. persist
        data.stepExecutionInstanceData.put(newStepExecutionId, stepExecution);
        data.stepThreadInstanceData.put(instanceKey, stepInstance);

        return stepExecution;
    }

    @Override
    public StepThreadExecutionEntity createTopLevelStepExecutionOnRestartFromPreviousStepInstance(long jobExecutionId,
                                                                                                  TopLevelStepInstanceEntity stepInstance) throws NoSuchJobExecutionException {

        // 1. Find related objects
        JobExecutionEntity newJobExecution = getJobExecution(jobExecutionId);
        StepThreadExecutionEntity lastStepExecution = stepInstance.getLatestStepThreadExecution();

        // 2. Construct and initalize new entity instances
        long newStepExecutionId = data.stepExecutionIdGenerator.getAndIncrement();
        TopLevelStepExecutionEntity newStepExecution = new TopLevelStepExecutionEntity(newStepExecutionId, newJobExecution, stepInstance.getStepName(), stepInstance.isPartitionedStep());
        newStepExecution.setPersistentUserDataBytes(lastStepExecution.getPersistentUserDataBytes());
        stepInstance.incrementStartCount();

        // 3. Update the relationships that didn't get updated in constructors
        stepInstance.setLatestStepThreadExecution(newStepExecution);
        newJobExecution.getStepThreadExecutions().add(newStepExecution);

        // 4. Persist
        data.stepExecutionInstanceData.put(newStepExecutionId, newStepExecution);

        return newStepExecution;
    }

    @Override
    public StepThreadExecutionEntity createPartitionStepExecutionOnRestartFromPreviousStepInstance(long jobExecutionId, StepThreadInstanceEntity stepThreadInstance,
                                                                                                   boolean isRemoteDispatch) throws NoSuchJobExecutionException {

        // 1. Find related objects
        JobExecutionEntity newJobExecution = getJobExecution(jobExecutionId);
        StepThreadExecutionEntity lastStepExecution = stepThreadInstance.getLatestStepThreadExecution();
        final TopLevelStepExecutionEntity topLevelStepExecution = getStepExecutionTopLevelFromJobExecutionIdAndStepName(jobExecutionId, stepThreadInstance.getStepName());

        // 2. Construct and initalize new entity instances
        long newStepExecutionId = data.stepExecutionIdGenerator.getAndIncrement();
        StepThreadExecutionEntity newStepExecution = new StepThreadExecutionEntity(newStepExecutionId, newJobExecution, stepThreadInstance.getStepName(), stepThreadInstance.getPartitionNumber());
        newStepExecution.setPersistentUserDataBytes(lastStepExecution.getPersistentUserDataBytes());

        // 3. Update the relationships that didn't get updated in constructors
        stepThreadInstance.setLatestStepThreadExecution(newStepExecution);
        newJobExecution.getStepThreadExecutions().add(newStepExecution);
        newStepExecution.setTopLevelStepExecution(topLevelStepExecution);
        topLevelStepExecution.getTopLevelAndPartitionStepExecutions().add(newStepExecution);

        /*
         * 222050 - Backout 205106
         * RemotablePartitionEntity remotablePartition = null;
         * if (isRemoteDispatch) {
         * RemotablePartitionKey remotablePartitionKey = new RemotablePartitionKey(newJobExecution.getExecutionId(), stepThreadInstance.getStepName(),
         * stepThreadInstance.getPartitionNumber());
         * remotablePartition = data.partitionData.get(remotablePartitionKey);
         * remotablePartition.setStepExecution(newStepExecution);
         *
         * newStepExecution.setRemotablePartition(remotablePartition);
         *
         * }
         */

        // 4. Persist
        data.stepExecutionInstanceData.put(newStepExecutionId, newStepExecution);

        return newStepExecution;
    }

    @Override
    public TopLevelStepExecutionEntity createTopLevelStepExecutionOnRestartAndCleanStepInstance(final long jobExecutionId,
                                                                                                final TopLevelStepInstanceEntity stepInstance) throws NoSuchJobExecutionException {

        // 1. Find related objects
        final JobExecutionEntity newJobExecution = getJobExecution(jobExecutionId);

        // 2. Construct and initalize new entity instances
        long newStepExecutionId = data.stepExecutionIdGenerator.getAndIncrement();
        TopLevelStepExecutionEntity newStepExecution = new TopLevelStepExecutionEntity(newStepExecutionId, newJobExecution, stepInstance.getStepName(), stepInstance.isPartitionedStep());
        stepInstance.incrementStartCount(); // Non-obvious interpretation of the spec
        stepInstance.deleteCheckpointData();

        // 3. Update the relationships that didn't get updated in constructors
        stepInstance.setLatestStepThreadExecution(newStepExecution);
        newJobExecution.getStepThreadExecutions().add(newStepExecution);

        // 4. Persist
        data.stepExecutionInstanceData.put(newStepExecutionId, newStepExecution);

        return newStepExecution;
    }

    /**
     * @return null if not found (don't throw exception)
     */
    @Override
    public StepThreadInstanceEntity getStepThreadInstance(StepThreadInstanceKey stepInstanceKey) {
        return data.stepThreadInstanceData.get(stepInstanceKey);
    }

    /**
     * @return list of partition numbers related to this top-level step instance
     *         which are in COMPLETED state, in order of increasing partition number.
     */
    @Override
    public List<Integer> getStepThreadInstancePartitionNumbersOfRelatedCompletedPartitions(StepThreadInstanceKey topLevelKey) {

        List<Integer> out = new ArrayList<Integer>();

        long compareInstanceId = topLevelKey.getJobInstance();

        for (StepThreadInstanceEntity stepThreadInstance : data.stepThreadInstanceData.values()) {
            if ((stepThreadInstance.getJobInstance().getInstanceId() == compareInstanceId) &&
                (stepThreadInstance.getStepName().equals(topLevelKey.getStepName())) &&
                (!(stepThreadInstance instanceof TopLevelStepInstanceEntity)) &&
                (stepThreadInstance.getLatestStepThreadExecution().getBatchStatus() == BatchStatus.COMPLETED)) {

                out.add(stepThreadInstance.getPartitionNumber());
            }
        }

        Collections.sort(out);
        return out;
    }

    @Override
    public StepThreadInstanceEntity updateStepThreadInstanceWithCheckpointData(StepThreadInstanceEntity stepThreadInstance) {
        // Nothing to do since in-memory object already updated.
        return stepThreadInstance;
    }

    @Override
    public TopLevelStepInstanceEntity updateStepThreadInstanceWithPartitionPlanSize(StepThreadInstanceKey instanceKey, int numCurrentPartitions) {
        TopLevelStepInstanceEntity stepInstance = null;
        try {
            stepInstance = (TopLevelStepInstanceEntity) data.stepThreadInstanceData.get(instanceKey);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Expected top-level step instance, but found another type.", e);
        }
        stepInstance.setPartitionPlanSize(numCurrentPartitions);
        return stepInstance;
    }

    // Not well-suited to factor out into an interface method since for JPA it's embedded in a tran typically
    private TopLevelStepExecutionEntity getStepExecutionTopLevelFromJobExecutionIdAndStepName(long jobExecutionId, String stepName) {
        JobExecutionEntity jobExecution = data.executionInstanceData.get(jobExecutionId);
        // Copy list to avoid ConcurrentModificationException
        for (StepThreadExecutionEntity stepExec : new ArrayList<StepThreadExecutionEntity>(jobExecution.getStepThreadExecutions())) {
            if (stepExec.getStepName().equals(stepName)) {
                if (stepExec instanceof TopLevelStepExecutionEntity) {
                    return (TopLevelStepExecutionEntity) stepExec;
                }
            }
        }
        // Bad if we've gotten here.
        throw new IllegalStateException("Couldn't find top-level step execution for jobExecution = " + jobExecutionId + ", and stepName = " + stepName);
    }

    /**
     * It might seems like this should delete related partition-level step executions as well.
     *
     * However these are owned by the job execution, not the step thread instance.
     *
     * @param stepThreadInstanceKey
     */
    @Override
    public void deleteStepThreadInstanceOfRelatedPartitions(TopLevelStepInstanceKey stepInstanceKey) {

        long compareInstanceId = stepInstanceKey.getJobInstance();

        for (StepThreadInstanceEntity stepThreadInstance : data.stepThreadInstanceData.values()) {
            if ((stepThreadInstance.getJobInstance().getInstanceId() == compareInstanceId) &&
                (stepThreadInstance.getStepName().equals(stepInstanceKey.getStepName())) &&
                (!(stepThreadInstance instanceof TopLevelStepInstanceEntity))) {

                StepThreadInstanceKey removalKey = new StepThreadInstanceKey(stepThreadInstance);
                data.stepThreadInstanceData.remove(removalKey);
            }
        }
    }

    @Override
    public StepThreadExecutionEntity getStepThreadExecution(long stepExecutionId) throws IllegalArgumentException {
        StepThreadExecutionEntity retVal = data.stepExecutionInstanceData.get(stepExecutionId);
        if (retVal == null) {
            throw new IllegalArgumentException("StepThreadExecEntity with id =" + stepExecutionId + " should be persisted at this point, but didn't find it.");
        }
        return retVal;
    }

    @Override
    public List<StepExecution> getStepExecutionsTopLevelFromJobExecutionId(long jobExecutionId) throws NoSuchJobExecutionException {
        List<StepExecution> out = new ArrayList<StepExecution>();
        /*
         * query="SELECT s FROM StepThreadExecutionEntity s
         * WHERE (s.jobExec.jobExecId = :jobExecId AND TYPE(s) = TopLevelStepExecutionEntity)
         * ORDER BY s.startTime ASC")
         */

        for (StepThreadExecutionEntity stepThreadExecution : data.stepExecutionInstanceData.values()) {
            if (stepThreadExecution.getJobExecution().getExecutionId() == jobExecutionId) {
                if (stepThreadExecution instanceof TopLevelStepExecutionEntity) {
                    out.add(stepThreadExecution);
                }
            }
        }

        // If empty, try to get job execution to generate NoSuchJobExecutionException if unknown id
        if (out.isEmpty()) {
            getJobExecution(jobExecutionId);
        }

        Collections.sort(out, StepExecutionStartDateComparator.INSTANCE);
        return out;
    }

    @Override
    public WSStepThreadExecutionAggregate getStepExecutionAggregateFromJobExecutionId(long jobExecutionId, String stepName) throws NoSuchJobExecutionException {

        WSStepThreadExecutionAggregateImpl retVal = new WSStepThreadExecutionAggregateImpl();

        List<StepThreadExecutionEntity> partitionExecs = new ArrayList<StepThreadExecutionEntity>();

        boolean foundTopLevelStepExec = false;
        for (StepThreadExecutionEntity stepThreadExecution : data.stepExecutionInstanceData.values()) {
            if (stepThreadExecution.getJobExecution().getExecutionId() == jobExecutionId && stepThreadExecution.getStepName().equals(stepName)) {
                if (stepThreadExecution instanceof TopLevelStepExecutionEntity) {
                    if (foundTopLevelStepExec) {
                        throw new IllegalArgumentException("Found two top-level step thread execs at job execution id: " + jobExecutionId + ", and stepName: " + stepName);
                    }
                    retVal.setTopLevelStepExecution((WSTopLevelStepExecution) stepThreadExecution);
                    foundTopLevelStepExec = true;
                } else {
                    partitionExecs.add(stepThreadExecution);
                }
            }
        }

        if (!foundTopLevelStepExec) {
            throw new IllegalArgumentException("Didn't find top-level step thread exec at job execution id: " + jobExecutionId + ", and stepName: " + stepName);
        }

        // 222050 - Backout 205106
        // List<WSPartitionStepAggregate> partitionAggregate = new ArrayList<WSPartitionStepAggregate>();

        Collections.sort(partitionExecs, StepThreadExecutionPartitionNumberComparator.INSTANCE);

        //Get the RemotablePartitions for all partitions if any
        /*
         * 222050 Backout 205106
         * for (StepThreadExecutionEntity partitionStep : partitionExecs) {
         * RemotablePartitionKey partitionKey = new RemotablePartitionKey(partitionStep.getJobExecution().getExecutionId(), partitionStep.getStepName(),
         * partitionStep.getPartitionNumber());
         * RemotablePartitionEntity partitionEntity = data.partitionData.get(partitionKey);
         * partitionAggregate.add(new WSPartitionStepAggregateImpl(partitionStep, partitionEntity));
         * }
         *
         * retVal.setPartitionAggregate(partitionAggregate);
         */
        retVal.setPartitionLevelStepExecutions(new ArrayList<WSPartitionStepThreadExecution>(partitionExecs));
        return retVal;
    }

    @Override
    public WSStepThreadExecutionAggregate getStepExecutionAggregate(long topLevelStepExecutionId) {

        WSStepThreadExecutionAggregateImpl retVal = new WSStepThreadExecutionAggregateImpl();

        List<StepThreadExecutionEntity> partitionExecs = new ArrayList<StepThreadExecutionEntity>();

        boolean foundTopLevelStepExec = false;
        for (StepThreadExecutionEntity stepThreadExecution : data.stepExecutionInstanceData.values()) {
            if (stepThreadExecution.getTopLevelStepExecution().getStepExecutionId() == topLevelStepExecutionId) {
                if (stepThreadExecution instanceof TopLevelStepExecutionEntity) {
                    retVal.setTopLevelStepExecution((WSTopLevelStepExecution) stepThreadExecution);
                    foundTopLevelStepExec = true;
                } else {
                    partitionExecs.add(stepThreadExecution);
                }
            }
        }

        if (!foundTopLevelStepExec) {
            throw new IllegalArgumentException("Didn't find top-level step thread exec at id: " + topLevelStepExecutionId);
        }

        // 222050 - Backout 205106
        // List<WSPartitionStepAggregate> partitionAggregate = new ArrayList<WSPartitionStepAggregate>();

        Collections.sort(partitionExecs, StepThreadExecutionPartitionNumberComparator.INSTANCE);

        //Get the RemotablePartitions for all partitions if any
        /*
         * 222050 - Backout 205106
         * for (StepThreadExecutionEntity partitionStep : partitionExecs) {
         * RemotablePartitionKey partitionKey = new RemotablePartitionKey(partitionStep.getJobExecution().getExecutionId(), partitionStep.getStepName(),
         * partitionStep.getPartitionNumber());
         * RemotablePartitionEntity partitionEntity = data.partitionData.get(partitionKey);
         * partitionAggregate.add(new WSPartitionStepAggregateImpl(partitionStep, partitionEntity));
         * }
         *
         * retVal.setPartitionAggregate(partitionAggregate);
         */
        retVal.setPartitionLevelStepExecutions(new ArrayList<WSPartitionStepThreadExecution>(partitionExecs));
        return retVal;
    }

    /**
     * @return a WSStepThreadExecutionAggregate matching the job instance id, stepname, and job execution sequence number
     */
    @Override
    public WSStepThreadExecutionAggregate getStepExecutionAggregateFromJobExecutionNumberAndStepName(long jobInstanceId, int jobExecNum,
                                                                                                     String stepName) throws NoSuchJobInstanceException, IllegalArgumentException {

        WSStepThreadExecutionAggregateImpl retVal = new WSStepThreadExecutionAggregateImpl();

        List<StepThreadExecutionEntity> partitionExecs = new ArrayList<StepThreadExecutionEntity>();

        boolean foundTopLevelStepExec = false;
        for (StepThreadExecutionEntity stepThreadExecution : data.stepExecutionInstanceData.values()) {
            if (stepThreadExecution.getJobExecution().getJobInstance().getInstanceId() == jobInstanceId
                && stepThreadExecution.getStepName().equals(stepName)
                && stepThreadExecution.getJobExecution().getExecutionNumberForThisInstance() == jobExecNum) {
                if (stepThreadExecution instanceof TopLevelStepExecutionEntity) {
                    if (foundTopLevelStepExec) {
                        throw new IllegalArgumentException("Found two top-level step thread execs at job instance id: "
                                                           + jobInstanceId + " stepName: " + stepName + ", and job execution sequence number: "
                                                           + jobExecNum);
                    }
                    retVal.setTopLevelStepExecution((WSTopLevelStepExecution) stepThreadExecution);
                    foundTopLevelStepExec = true;
                } else {
                    partitionExecs.add(stepThreadExecution);
                }
            }
        }

        if (!foundTopLevelStepExec) {
            // trigger NoSuchJobInstanceException
            getJobInstance(jobInstanceId);

            throw new IllegalArgumentException("Didn't find top-level step thread exec at job instance id: "
                                               + jobInstanceId + " stepName: " + stepName + ", and job execution sequence number: "
                                               + jobExecNum);
        }

        // 222050 - Backout 205106
        // List<WSPartitionStepAggregate> partitionAggregate = new ArrayList<WSPartitionStepAggregate>();

        Collections.sort(partitionExecs, StepThreadExecutionPartitionNumberComparator.INSTANCE);

        //Get the RemotablePartitions for all partitions if any
        /*
         * 222050 - Backout 205106
         * for (StepThreadExecutionEntity partitionStep : partitionExecs) {
         * RemotablePartitionKey partitionKey = new RemotablePartitionKey(partitionStep.getJobExecution().getExecutionId(), partitionStep.getStepName(),
         * partitionStep.getPartitionNumber());
         * RemotablePartitionEntity partitionEntity = data.partitionData.get(partitionKey);
         * partitionAggregate.add(new WSPartitionStepAggregateImpl(partitionStep, partitionEntity));
         * }
         *
         * retVal.setPartitionAggregate(partitionAggregate);
         */
        retVal.setPartitionLevelStepExecutions(new ArrayList<WSPartitionStepThreadExecution>(partitionExecs));
        return retVal;
    }

    @Override
    public StepThreadExecutionEntity updateStepExecution(final RuntimeStepExecution runtimeStepExecution) {
        StepThreadExecutionEntity stepExecutionEntity = getStepThreadExecution(runtimeStepExecution.getInternalStepThreadExecutionId());
        if (stepExecutionEntity == null) {
            throw new IllegalStateException("StepThreadExecEntity with id =" + runtimeStepExecution.getInternalStepThreadExecutionId()
                                            + " should be persisted at this point, but didn't find it.");
        }
        updateStepExecutionStatusTimeStampsUserDataAndMetrics(stepExecutionEntity, runtimeStepExecution);
        return stepExecutionEntity;
    }

    @Override
    public TopLevelStepExecutionEntity updateStepExecutionWithPartitionAggregate(final RuntimeStepExecution runtimeStepExecution) {

        TopLevelStepExecutionEntity stepExec = (TopLevelStepExecutionEntity) getStepThreadExecution(runtimeStepExecution.getInternalStepThreadExecutionId());
        updateStepExecutionStatusTimeStampsUserDataAndMetrics(stepExec, runtimeStepExecution);
        for (StepThreadExecutionEntity stepThreadExec : stepExec.getTopLevelAndPartitionStepExecutions()) {
            // Exclude this very row, which shows up in this list.  It will be the only one of subclass type
            if (!(stepThreadExec instanceof TopLevelStepExecutionEntity)) {
                stepExec.addMetrics(stepThreadExec);
            }
        }
        return stepExec;
    }

    private static class ReverseJobExecutionSequenceForOneInstanceComparator implements Comparator<JobExecutionEntity> {
        public static final ReverseJobExecutionSequenceForOneInstanceComparator INSTANCE = new ReverseJobExecutionSequenceForOneInstanceComparator();

        @Override
        public int compare(final JobExecutionEntity o1, JobExecutionEntity o2) {
            Integer i1 = o1.getExecutionNumberForThisInstance();
            Integer i2 = o2.getExecutionNumberForThisInstance();
            return i2.compareTo(i1);
        }
    }

    private static class ReverseJobExecutionCreationDateComparator implements Comparator<JobExecutionEntity> {
        public static final ReverseJobExecutionCreationDateComparator INSTANCE = new ReverseJobExecutionCreationDateComparator();

        @Override
        public int compare(final JobExecutionEntity o1, JobExecutionEntity o2) {
            return o2.getCreateTime().compareTo((o1.getCreateTime()));
        }
    }

    private static class ReverseJobInstanceCreationDateComparator implements Comparator<JobInstanceEntity> {
        public static final ReverseJobInstanceCreationDateComparator INSTANCE = new ReverseJobInstanceCreationDateComparator();

        @Override
        public int compare(final JobInstanceEntity o1, JobInstanceEntity o2) {
            return o2.getCreateTime().compareTo((o1.getCreateTime()));
        }
    }

    private static class StepExecutionStartDateComparator implements Comparator<StepExecution> {
        public static final StepExecutionStartDateComparator INSTANCE = new StepExecutionStartDateComparator();

        @Override
        public int compare(final StepExecution o1, StepExecution o2) {
            return o1.getStartTime().compareTo((o2.getStartTime()));
        }
    }

    private static class StepThreadExecutionPartitionNumberComparator implements Comparator<StepThreadExecutionEntity> {
        public static final StepThreadExecutionPartitionNumberComparator INSTANCE = new StepThreadExecutionPartitionNumberComparator();

        @Override
        public int compare(final StepThreadExecutionEntity o1, StepThreadExecutionEntity o2) {
            return o1.getPartitionNumber() - o2.getPartitionNumber();
        }
    }

    private static class JobExecutionIDComparator implements Comparator<JobExecution> {
        public static final JobExecutionIDComparator INSTANCE = new JobExecutionIDComparator();

        //this method needed in order to sort certain result lists/maps
        @Override
        public int compare(final JobExecution o1, final JobExecution o2) {
            return new Long(o1.getExecutionId()).compareTo(o2.getExecutionId());
        }
    }

    private static class StepExecutionIDComparator implements Comparator<StepExecution> {
        public static final StepExecutionIDComparator INSTANCE = new StepExecutionIDComparator();

        //this method needed in order to sort certain result lists/maps
        @Override
        public int compare(final StepExecution o1, final StepExecution o2) {
            return new Long(o1.getStepExecutionId()).compareTo(o2.getStepExecutionId());
        }
    }

    /*
     * Commenting out to show it's not used.
     * private void cleanUp(final long instanceId) {
     * final JobInstanceEntity jobInstance = data.jobInstanceData.remove(instanceId);
     * if (jobInstance == null) {
     * return;
     * }
     *
     * //synchronized (jobInstance.executions) {
     * synchronized (jobInstance) {
     * for (final JobExecutionEntity executionInstanceData : data.executionInstanceData.values()) {
     * data.executionInstanceData.remove(executionInstanceData.getExecutionId());
     * synchronized (executionInstanceData.getStepThreadExecutions()) {
     * for (final StepExecution stepExecution : executionInstanceData.getStepThreadExecutions()) {
     * data.stepExecutionInstanceData.remove(stepExecution.getStepExecutionId());
     * }
     * }
     * }
     * }
     * }
     */

    /**
     *
     * @return a page of data from the jobinstances result set.
     */
    private List<JobInstanceEntity> readJobInstancesPage(List<JobInstanceEntity> totalJobInstances, int page, int pageSize) {

        List<JobInstanceEntity> retMe = new ArrayList<JobInstanceEntity>();

        // Move to the first row in the page
        int index = page * pageSize;

        if (logger.isLoggable(Level.FINEST)) {
            logger.finest("page value : " + page);
            logger.finest("pageSize value : " + pageSize);
            logger.finest("index (page * pageSize) : " + index);
            logger.finest("totalJobInstances.size() = " + totalJobInstances.size());
        }

        if (totalJobInstances.size() < 1) {
            return retMe; //return the empty list
        }

        while (retMe.size() < pageSize && index < totalJobInstances.size()) {
            retMe.add(totalJobInstances.get(index));
            index++;
        }

        return retMe;
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Disable the split-flow and partition execution methods for now.  Revisit when we do cross-JVM distribution of partitions and splitflows within
// a single job.   Rationale for not including this logic is we don't want to externalize new associated tables and create a customer DB migration
// problem if it turns out we need to make changes.
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//	/* (non-Javadoc)
//	 * @see com.ibm.jbatch.container.services.IPersistenceManagerService#createSplitFlowExecution(com.ibm.jbatch.container.persistence.jpa.RemotableSplitFlowKey, java.util.Date)
//	 */
//	@Override
//	public RemotableSplitFlowEntity createSplitFlowExecution(
//			RemotableSplitFlowKey splitFlowKey, Date createTime) {
//
//		// 1. Find related objects
//		JobExecutionEntity jobExec = getJobExecution(splitFlowKey.getJobExec());
//
//		// 2. Construct and initalize new entity instances
//		final RemotableSplitFlowEntity splitFlow = new RemotableSplitFlowEntity();
//		splitFlow.setInternalStatus(BatchStatus.STARTING.ordinal());
//		splitFlow.setFlowName(splitFlowKey.getFlowName());
//		splitFlow.setCreateTime(createTime);
//		splitFlow.setServerId(batchLocationService.getServerId());
//		splitFlow.setRestUrl(batchLocationService.getBatchRestUrl());
//
//		// 3. Update the relationships that didn't get updated in constructors
//		splitFlow.setJobExecution(jobExec);
//		jobExec.getSplitFlowExecutions().add(splitFlow);
//
//		// 4. Persist
//		data.splitFlowData.put(splitFlowKey, splitFlow);
//		return splitFlow;
//	}
//
//	@Override
//	public RemotableSplitFlowEntity updateSplitFlowExecution(RuntimeSplitFlowExecution runtimeSplitFlowExecution, BatchStatus newBatchStatus, Date date) {
//
//		RemotableSplitFlowKey splitFlowKey = new RemotableSplitFlowKey(runtimeSplitFlowExecution.getTopLevelExecutionId(), runtimeSplitFlowExecution.getFlowName());
//		RemotableSplitFlowEntity splitFlowEntity = data.splitFlowData.get(splitFlowKey);
//		if (splitFlowEntity == null) {
//			throw new IllegalArgumentException("No split flow execution found for key = " + splitFlowKey);
//		}
//		splitFlowEntity.setBatchStatus(newBatchStatus);
//		splitFlowEntity.setExitStatus(runtimeSplitFlowExecution.getExitStatus());
//		ExecutionStatus executionStatus = runtimeSplitFlowExecution.getFlowStatus();
//		if (executionStatus != null) {
//			splitFlowEntity.setInternalStatus(executionStatus.getExtendedBatchStatus().ordinal());
//		}
//		if (newBatchStatus.equals(BatchStatus.STARTED)) {
//			splitFlowEntity.setStartTime(date);
//		} else if (FINAL_STATUS_SET.contains(newBatchStatus)) {
//			splitFlowEntity.setEndTime(date);
//		}
//		return splitFlowEntity;
//	}
//
//	@Override
//	public RemotableSplitFlowEntity updateSplitFlowExecutionLogDir(RemotableSplitFlowKey key, String logDirPath) {
//
//		RemotableSplitFlowEntity splitFlowEntity = data.splitFlowData.get(key);
//		if (splitFlowEntity == null) {
//			throw new IllegalArgumentException("No split flow execution found for key = " + key);
//		}
//		splitFlowEntity.setLogpath(logDirPath);
//		return splitFlowEntity;
//	}
//
//	@Override
//	public RemotablePartitionEntity createPartitionExecution(
//			RemotablePartitionKey partitionKey, Date createTime) {
//
//		// 1. Find related objects
//		JobExecutionEntity jobExec = getJobExecution(partitionKey.getJobExec());
//
//		// 2. Construct and initalize new entity instances
//		final RemotablePartitionEntity partition = new RemotablePartitionEntity();
//		partition.setStepName(partitionKey.getStepName());
//		partition.setPartitionNumber(partitionKey.getPartitionNumber());
//		partition.setInternalStatus(BatchStatus.STARTING.ordinal());
//		partition.setCreateTime(createTime);
//		partition.setServerId(batchLocationService.getServerId());
//		partition.setRestUrl(batchLocationService.getBatchRestUrl());
//
//		// 3. Update the relationships that didn't get updated in constructors
//		partition.setJobExec(jobExec);
//		jobExec.getPartitionExecutions().add(partition);
//
//		// 4. persist
//		data.partitionData.put(partitionKey, partition);
//		return partition;
//	}
//
//	/* (non-Javadoc)
//	 * @see com.ibm.jbatch.container.services.IPersistenceManagerService#updatePartitionExecution(com.ibm.jbatch.container.execution.impl.RuntimePartitionExecution, javax.batch.runtime.BatchStatus, java.util.Date)
//	 */
//	@Override
//	public RemotablePartitionEntity updatePartitionExecution(
//			RuntimePartitionExecution runtimePartitionExecution,
//			BatchStatus newBatchStatus, Date date) {
//		RemotablePartitionKey partitionKey = new RemotablePartitionKey(runtimePartitionExecution.getTopLevelExecutionId(),
//				runtimePartitionExecution.getStepName(), runtimePartitionExecution.getPartitionNumber());
//		RemotablePartitionEntity partitionEntity = data.partitionData.get(partitionKey);
//		if (partitionEntity == null) {
//			throw new IllegalArgumentException("No partition execution found for key = " + partitionKey);
//		}
//		partitionEntity.setBatchStatus(newBatchStatus);
//		partitionEntity.setExitStatus(runtimePartitionExecution.getExitStatus());
//		partitionEntity.setInternalStatus(runtimePartitionExecution.getBatchStatus().ordinal());
//		if (newBatchStatus.equals(BatchStatus.STARTED)) {
//			partitionEntity.setStartTime(date);
//		} else if (FINAL_STATUS_SET.contains(newBatchStatus)) {
//			partitionEntity.setEndTime(date);
//		}
//		return partitionEntity;
//	}
//
//	@Override
//	public RemotablePartitionEntity updatePartitionExecutionLogDir(
//			RemotablePartitionKey key, String logDirPath) {
//		RemotablePartitionEntity partitionEntity = data.partitionData.get(key);
//		if (partitionEntity == null) {
//			throw new IllegalArgumentException("No partition execution found for key = " + key);
//		}
//		partitionEntity.setLogpath(logDirPath);
//		return partitionEntity;
//	}

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.jbatch.container.services.IPersistenceManagerService#purgeInGlassfish(java.lang.String)
     */
    @Override
    public void purgeInGlassfish(String submitter) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.jbatch.container.services.IPersistenceManagerService#purgeJobInstanceAndRelatedData(long)
     */
    @Override
    public boolean purgeJobInstanceAndRelatedData(long jobInstanceId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public JobInstance updateJobInstanceWithInstanceStateAndBatchStatus(
                                                                        long jobInstanceId, InstanceState state, BatchStatus batchStatus, Date lastUpdated) {
        JobInstanceEntity jobInstance = data.jobInstanceData.get(jobInstanceId);

        try {
            verifyStatusTransitionIsValid(jobInstance, batchStatus);
            verifyStateTransitionIsValid(jobInstance, state);
        } catch (BatchIllegalJobStatusTransitionException e) {
            throw new PersistenceException(e);
        }

        jobInstance.setInstanceState(state);
        jobInstance.setBatchStatus(batchStatus);
        jobInstance.setLastUpdatedTime(lastUpdated);

        //Trace for testing and debug.
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest("JobInstance : " + Long.toString(jobInstanceId) + " batchStatus updated : " + batchStatus.toString()
                          + " InstanceState updated : " + state.toString());
        }

        return jobInstance;
    }

    @Override
    public List<JobInstanceEntity> getJobInstances(IJPAQueryHelper queryHelper, int page, int pageSize) {
        // TODO: Should we implement this for Memory Persistence?
        //throw new UnsupportedOperationException("The REST URL search parameters requesting this function "
        //   + "are not supported by the Java batch memory-based persistence configuration.");

        String delieveredQuery = queryHelper.getQuery();

        if (delieveredQuery.equals(queryHelper.DEFAULT_QUERY)) {
            return getJobInstances(page, pageSize);
        } else {
            throw new UnsupportedOperationException("The REST URL search parameters requesting this function "
                                                    + "are not supported by the Java batch memory-based persistence configuration.");
        }
    }

    @Override
    public RemotablePartitionEntity createRemotablePartition(long jobExecId,
                                                             String stepName,
                                                             int partitionNum,
                                                             RemotablePartitionState partitionState) {
        RemotablePartitionKey partitionKey = new RemotablePartitionKey(jobExecId, stepName, partitionNum);
        JobExecutionEntity jobExecution = data.executionInstanceData.get(partitionKey.getJobExec());
        RemotablePartitionEntity partition = new RemotablePartitionEntity(jobExecution, partitionKey);
        partition.setInternalStatus(RemotablePartitionState.QUEUED);
        partition.setLastUpdated(new Date());
        data.partitionData.put(partitionKey, partition);

        jobExecution.getRemotablePartitions().add(partition);
        return partition;
    }

    @Override
    public RemotablePartitionEntity updateRemotablePartitionInternalState(
                                                                          long jobExecId, String stepName, int partitionNum,
                                                                          RemotablePartitionState internalStatus) {
        RemotablePartitionKey partitionKey = new RemotablePartitionKey(jobExecId, stepName, partitionNum);
        RemotablePartitionEntity partition = data.partitionData.get(partitionKey);
        partition.setInternalStatus(internalStatus);
        partition.setRestUrl(batchLocationService.getBatchRestUrl());
        partition.setServerId(batchLocationService.getServerId());
        partition.setLastUpdated(new Date());

        return partition;
    }

    /** {@inheritDoc} */
    @Override
    public String getPersistenceType() {
        return "MEM";
    }

    /** {@inheritDoc} */
    /*
     * @Override
     *
     * public List<String> getGroupNamesForJobID(long jobInstanceID) throws NoSuchJobInstanceException {
     * // TODO Auto-generated method stub
     * return null;
     * }
     */

    /** {@inheritDoc} */
    @Override
    public JobInstanceEntity updateJobInstanceWithGroupNames(long jobInstanceId, Set<String> groupNames) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public int getJobExecutionTableVersion() {
        return getJobExecutionTableVersionField();
    }

    /** {@inheritDoc} */
    @Override
    public int getJobInstanceTableVersion() {
        return getJobInstanceTableVersionField();
    }

    /** {@inheritDoc} */
    @Override
    public Integer getJobExecutionTableVersionField() {
        return 2;
    }

    /** {@inheritDoc} */
    @Override
    public Integer getJobInstanceTableVersionField() {
        return 3;
    }

}
