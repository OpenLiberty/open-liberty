/*
 * Copyright 2012, 2020 International Business Machines Corp.
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
package com.ibm.jbatch.container.services;

import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.operations.NoSuchJobInstanceException;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.StepExecution;

import com.ibm.jbatch.container.exception.BatchIllegalJobStatusTransitionException;
import com.ibm.jbatch.container.exception.ExecutionAssignedToServerException;
import com.ibm.jbatch.container.exception.JobStoppedException;
import com.ibm.jbatch.container.execution.impl.RuntimeSplitFlowExecution;
import com.ibm.jbatch.container.execution.impl.RuntimeStepExecution;
import com.ibm.jbatch.container.persistence.jpa.JobExecutionEntity;
import com.ibm.jbatch.container.persistence.jpa.JobInstanceEntity;
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
import com.ibm.jbatch.container.ws.InstanceState;
import com.ibm.jbatch.container.ws.JobInstanceNotQueuedException;
import com.ibm.jbatch.container.ws.WSRemotablePartitionExecution;
import com.ibm.jbatch.container.ws.WSRemotablePartitionState;
import com.ibm.jbatch.container.ws.WSStepThreadExecutionAggregate;
import com.ibm.jbatch.spi.services.IBatchServiceBase;

public interface IPersistenceManagerService extends IBatchServiceBase {

    /////
    // Should the persistence layer throw these exceptions or just return null?
    ////

    /////
    //
    // Consider removing
    /////

    //
    // ORGANIZED METHODS
    // CREATE, READ (GET), UPDATE, DELETE
    //

    //
    // JobInstance
    //

    /**
     * Creates a JobIntance
     *
     * @param appName
     *                       The batch application or module name associated with this job
     *                       instance. Null is allowed.
     * @param jobXMLName
     *                       the JSL file name
     * @param submitter
     *                       the application tag that owns this job
     * @param date
     *                       creation time
     *
     * @return the job instance
     */
    public JobInstanceEntity createJobInstance(String appName, String jobXMLName, String submitter, Date date);

    /**
     * Creates a JobIntance
     *
     * @param appName
     *                       The batch application or module name associated with this job
     *                       instance. Null is allowed.
     * @param jobXMLName
     *                       the JSL file name
     * @param jsl
     *                       the entire JSL file String
     * @param submitter
     *                       the application tag that owns this job
     * @param date
     *                       creation time
     *
     * @return the job instance
     */
    public JobInstanceEntity createJobInstance(String appName, String jobXMLName, String jsl, String submitter, Date date);

    public JobInstanceEntity getJobInstance(long jobInstanceId) throws NoSuchJobInstanceException;

    public JobInstanceEntity getJobInstanceFromExecutionId(long jobExecutionId) throws NoSuchJobExecutionException;

    /**
     * @param jobName
     * @param start
     * @param count
     * @return List in order from most-recent creation time to least-recent. Only as a secondary sort order
     *         is the list also sorted in descending order of instance id (highest to lowest). Note the notion of
     *         an instance "creation time" is not part of the spec. The question of when the first job execution begins
     *         to execute isn't relevant with respect to this creation time.
     *
     *         Returns empty list if nothing found, (never NoSuchJobExecution).
     */
    public List<JobInstanceEntity> getJobInstances(String jobName, int start, int count);

    /**
     * Similar to {@link #getJobInstances(String, int, int)} but with extra qualifying parameter,
     * further filtering the list based on submitter id.
     *
     * @param jobName
     * @param submitter
     * @param start
     * @param count
     * @return See {@link #getJobInstances(String, int, int)}
     */
    public List<JobInstanceEntity> getJobInstances(String jobName, String submitter, int start, int count);

    /**
     * TODO - order?
     *
     * @param page
     *                     The page of rows to get (starts at 0)
     * @param pageSize
     *                     The number of rows per page
     *
     * @return a list of (top-level only) job instances, ordered from most-to-least-recent creation time.
     */
    public List<JobInstanceEntity> getJobInstances(int page, int pageSize);

    /**
     * @param page
     *                      The page of rows to get (starts at 0)
     * @param pageSize
     *                      The number of rows per page
     * @param submitter
     *                      Only return jobs owned by user. User can be null. Specify null
     *                      string to include all users.
     *                      TODO - is null still allowed?
     *
     * @return a list of (top-level only) job instances owned by given user, ordered in descending order of instance id
     */
    public List<JobInstanceEntity> getJobInstances(int page, int pageSize, String submitter);

    // TODO - Getting just one field worth it?
    // Some of the callers here are just calling to get the instance id to
    // turn around and look up something else, like the submitter.  Might
    // be nice to just be able to lookup the submitter by executionId.
    public long getJobInstanceIdFromExecutionId(long jobExecutionId) throws NoSuchJobExecutionException;

    public String getJobInstanceAppName(long jobInstanceId) throws NoSuchJobInstanceException;

    public String getJobInstanceAppNameFromExecutionId(long jobExecutionId) throws NoSuchJobExecutionException;

    /**
     * @param jobInstanceId
     * @return
     */
    public String getJobInstanceSubmitter(long jobInstanceId) throws NoSuchJobInstanceException;

    // Other JobInstance-related stuff like Job Names
    public Set<String> getJobNamesSet();

    public Set<String> getJobNamesSet(String submitter);

    public int getJobInstanceCount(String jobName);

    public int getJobInstanceCount(String jobName, String submitter);

    public JobInstanceEntity updateJobInstanceWithInstanceState(long jobInstanceId, InstanceState state, Date date);

    public JobInstance updateJobInstanceOnRestart(long jobInstanceId, Date date);

    public JobInstance updateJobInstanceStateOnConsumed(long instanceId) throws BatchIllegalJobStatusTransitionException, JobInstanceNotQueuedException;

    public JobInstance updateJobInstanceStateOnQueued(long instanceId) throws BatchIllegalJobStatusTransitionException;

    public JobInstance updateJobInstanceWithInstanceStateAndBatchStatus(long jobInstanceId, InstanceState state, BatchStatus batchStatus, Date date);

    public JobInstance updateJobInstanceNullOutRestartOn(final long jobInstanceId);

    public JobInstance updateJobInstanceWithRestartOn(long jobInstanceId, String restartOn);

    /**
     * Update the given jobinstance record with the given jobName and jobXml
     *
     * @param jobInstanceId
     *                          the jobinstance to update
     * @param jobName
     *                          the job name
     * @param jobXml
     *                          the job JSL source (XML)
     */
    public JobInstance updateJobInstanceWithJobNameAndJSL(long jobInstanceId, String jobName, String jobXml);

    //
    // Job Instance & Execution both
    //

    /**
     * Sets create and lastUpdated times, sets BatchStatus to STARTED,
     * sets InstanceState to DISPATCHED
     *
     * @param jobExecutionId
     * @param startedTime
     * @return
     * @throws NoSuchJobExecutionException
     */
    public JobExecution updateJobExecutionAndInstanceOnStarted(long jobExecutionId, Date startedTime) throws NoSuchJobExecutionException;

    public JobExecution updateJobExecutionAndInstanceOnStatusChange(long jobExecutionId, BatchStatus newBatchStatus, Date updateTime) throws NoSuchJobExecutionException;

    /**
     * Called if the execution has not yet reached the endpoint.
     *
     * Sets the BatchStatus and InstanceState to STOPPED, and sets the lastUpdated time
     *
     * @param jobExecutionId
     * @param updateTime
     *
     * @return the updated JobExecution
     *
     * @throws NoSuchJobExecutionException        if the job execution is not located by the find query
     * @throws ExecutionAssignedToServerException if the execution has been assigned to a server/endpoint (serverid set)
     */
    JobExecution updateJobExecutionAndInstanceOnStopBeforeServerAssigned(long jobExecutionId,
                                                                         Date updateTime) throws NoSuchJobExecutionException, ExecutionAssignedToServerException;

    public JobExecution updateJobExecutionAndInstanceOnEnd(long jobExecutionId, BatchStatus finalBatchStatus, String finalExitStatus,
                                                           Date endTime) throws NoSuchJobExecutionException;

    //
    // JobExecution (only)
    //
    /**
     * Creates new job execution.
     *
     * Responsible for making any necessary updates to instance as well.
     *
     * @param jobInstanceId
     * @param jobParameters
     * @param create        time
     * @return
     */
    public JobExecutionEntity createJobExecution(long jobInstanceId, Properties jobParameters, Date createTime);

    public JobExecutionEntity getJobExecution(long jobExecutionId) throws NoSuchJobExecutionException;

    /**
     * @param jobInstanceId
     * @return The executions are ordered in sequence, from most-recent to least-recent.
     *         The container keeps its own order and does not depend on execution id or creation time to order these.
     */
    public List<JobExecutionEntity> getJobExecutionsFromJobInstanceId(long jobInstanceId) throws NoSuchJobInstanceException;

    /**
     *
     * @param jobName
     * @return List ordered by creation time, most-to-least-recent
     *         Returns empty list if none found, rather than throwing exception.
     */
    public List<Long> getJobExecutionsRunning(String jobName);

    /**
     * This is "most recent" in terms of execution sequence, not w.r.t.
     * any timestamp or even execution id.
     *
     * @return the most-recent, based on internal sequencing, executionId
     *         associated with the given job instance ID.
     *
     * @throws IllegalStateException if no executions are found
     *                                   NoSuchJobInstanceException if instance itself isn't found
     */
    public JobExecutionEntity getJobExecutionMostRecent(long jobInstanceId) throws IllegalStateException, NoSuchJobInstanceException;

    // Getter that just gets one field?

    /**
     * Same as {@link #getJobExecutionMostRecent(long)}
     */
    public long getJobExecutionIdMostRecent(long jobInstanceId) throws IllegalStateException;

    public Properties getJobExecutionParameters(long jobExecutionId) throws NoSuchJobExecutionException;

    public JobExecutionEntity getJobExecutionFromJobExecNum(long jobInstanceId, int jobExecNum) throws NoSuchJobInstanceException, IllegalArgumentException;

    /**
     * Get the application name from an execution id.
     * TODO - SHOULD BE submitter?
     *
     * @param jobExecutionId * the job execution id
     * @return the application name
     */
    //public String getTagName(long jobExecutionId);

    /**
     * Sets the stored log directory path for the given job execution.
     *
     * @param execId
     * @param logDirPath
     */
    public JobExecutionEntity updateJobExecutionLogDir(long jobExecutionId, String logDirPath) throws NoSuchJobExecutionException;

    /**
     * Sets the server id and rest url for the given job execution.
     *
     * @param execId
     *
     * @throws NoSuchJobExecutionException if this job execution is not located by the find query
     * @throws JobStoppedException         if the specified execution has been stopped at the time this update is attempted
     *
     * @return the updated execution
     */
    public JobExecutionEntity updateJobExecutionServerIdAndRestUrlForStartingJob(long topLevelExecutionId) throws NoSuchJobExecutionException, JobStoppedException;

    // STEP THREAD

    /**
     * Needs to do three things
     *
     * 1. create new stepthreadexec id, in STARTING state 2. initialize instance
     * start count to 1
     *
     * @param instanceKey
     * @param isPartitioned Is this at the top-level, a partitioned step or not (NOT whether or not this is a partition or top-level thread of a partitioned step)
     * @return new step thread execution id
     */
    public TopLevelStepExecutionEntity createTopLevelStepExecutionAndNewThreadInstance(long jobExecutionId, StepThreadInstanceKey instanceKey, boolean isPartitioned);

    public StepThreadExecutionEntity createPartitionStepExecutionAndNewThreadInstance(long jobExecutionId, StepThreadInstanceKey instanceKey, boolean isRemoteDispatch);

    /**
     * Needs to:
     *
     * 1. create new stepthreadexec id, in STARTING state 2. increment step
     * instance start count (for top-level only) 3. copy over persistent
     * userdata to new step exec 4. point step thread instance to latest
     * execution
     *
     * @param instanceKey
     * @return new step thread execution
     */
    public StepThreadExecutionEntity createTopLevelStepExecutionOnRestartFromPreviousStepInstance(long jobExecutionId,
                                                                                                  TopLevelStepInstanceEntity stepInstance) throws NoSuchJobExecutionException;

    public StepThreadExecutionEntity createPartitionStepExecutionOnRestartFromPreviousStepInstance(long jobExecutionId, StepThreadInstanceEntity stepThreadInstance,
                                                                                                   boolean isRemoteDispatch) throws NoSuchJobExecutionException;

    /**
     *
     * Needs to:
     *
     * 1. create new stepthreadexec id, in STARTING state 2. increment step
     * instance start count (for top-level only) 3. don't copy persistent
     * userdata 4. delete checkpoint data 5. point step thread instance to latest
     * execution
     *
     *
     * @param instanceKey
     * @return new step thread execution id
     */
    public StepThreadExecutionEntity createTopLevelStepExecutionOnRestartAndCleanStepInstance(long jobExecutionId,
                                                                                              TopLevelStepInstanceEntity stepInstance) throws NoSuchJobExecutionException;

    /**
     * @param stepInstanceKey
     * @return null if not found (don't throw exception)
     */
    public StepThreadInstanceEntity getStepThreadInstance(StepThreadInstanceKey stepInstanceKey);

    public List<Integer> getStepThreadInstancePartitionNumbersOfRelatedCompletedPartitions(StepThreadInstanceKey topLevelKey);

    /**
     * Not a nice method name, but reflects when it's going to be called in current design.
     *
     * @param stepThreadInstance
     */
    public StepThreadInstanceEntity updateStepThreadInstanceWithCheckpointData(StepThreadInstanceEntity stepThreadInstance);

    public TopLevelStepInstanceEntity updateStepThreadInstanceWithPartitionPlanSize(StepThreadInstanceKey instanceKey, int numCurrentPartitions);

    /**
     * It might seems like this should delete related partition-level step executions as well.
     *
     * However these are owned by the job execution, not the step thread instance.
     *
     * @param stepThreadInstanceKey
     */
    public void deleteStepThreadInstanceOfRelatedPartitions(TopLevelStepInstanceKey stepInstanceKey);

    // Step Execution
    /**
     *
     * @param topLevelStepExecutionId
     * @return
     * @throws IllegalArgumentException if either:
     *                                      1) we have no entry at all with id equal to <code>stepExecutionId</code>
     *                                      2) we have a partition-level StepThreadExecutionEntity with this id (but not a top-level entry).
     */
    public TopLevelStepExecutionEntity getStepExecutionTopLevel(long topLevelStepExecutionId) throws IllegalArgumentException;

    /**
     *
     * @param jobExecutionId
     * @return ordered by start time
     * @throws NoSuchJobExecutionException
     */
    public List<StepExecution> getStepExecutionsTopLevelFromJobExecutionId(long jobExecutionId) throws NoSuchJobExecutionException;

    /**
     *
     * @param jobExecutionId
     * @return list of step execution aggregates associated with job execution
     *         identified by <code>jobExecutionId</code>. The list is ordered by start time, earliest to latest,
     *         of the top-level {@code StepExecution} of each {@code WSStepExecutionAggregate}
     * @throws NoSuchJobExecutionException
     */
    public List<WSStepThreadExecutionAggregate> getStepExecutionAggregatesFromJobExecutionId(long jobExecutionId) throws NoSuchJobExecutionException;

    public WSStepThreadExecutionAggregate getStepExecutionAggregateFromJobExecutionId(long jobExecutionId, String stepName) throws NoSuchJobExecutionException;

    /**
     * @param topLevelStepExecutionId
     * @return step exec aggregate
     * @throws IllegalArgumentException if either:
     *                                      1) we have no entry at all with id equal to <code>stepExecutionId</code>
     *                                      2) we have a partition-level StepThreadExecutionEntity with this id (but not a top-level entry).
     */
    public WSStepThreadExecutionAggregate getStepExecutionAggregate(long topLevelStepExecutionId) throws IllegalArgumentException;

    public WSStepThreadExecutionAggregate getStepExecutionAggregateFromJobExecutionNumberAndStepName(long jobInstanceId, int jobExecNum,
                                                                                                     String stepName) throws NoSuchJobInstanceException, IllegalArgumentException;

    /**
     * Full update: status, timestamps, persistent user data, metrics
     *
     * @param runtimeStepExecution
     */
    public StepThreadExecutionEntity updateStepExecution(RuntimeStepExecution runtimeStepExecution);

    /**
     * Update a StepExecution for the "top-level" StepExecution of a partitioned
     * step.
     *
     * This will aggregate the metrics from the "partition-level"
     * StepExecution(s), (which by the way are not spec-defined and not
     * accessible through standard, public APIs.)
     *
     * @param runtimeStepExecution
     *                                 the runtime StepExecution
     */
    public TopLevelStepExecutionEntity updateStepExecutionWithPartitionAggregate(RuntimeStepExecution runtimeStepExecution);

    /**
     * @param splitFlowKey
     */
    public RemotableSplitFlowEntity createSplitFlowExecution(RemotableSplitFlowKey splitFlowKey, Date createTime);

    /**
     * @param runtimeFlowInSplitExecution
     */
    public RemotableSplitFlowEntity updateSplitFlowExecution(RuntimeSplitFlowExecution runtimeSplitFlowExecution, BatchStatus newBatchStatus, Date date);

    /**
     * TODO - delta needed?
     *
     * @param key
     * @param logDirPath
     */
    public RemotableSplitFlowEntity updateSplitFlowExecutionLogDir(RemotableSplitFlowKey key, String logDirPath);

    /**
     * @param key
     * @param logDirPath
     */
    public RemotablePartitionEntity updatePartitionExecutionLogDir(RemotablePartitionKey key, String logDirPath);

    // purge
    public void purgeInGlassfish(String submitter);

    public boolean purgeJobInstanceAndRelatedData(long jobInstanceId);

    // other

    /// Higher-level methods (than persistence service)
    public boolean isJobInstancePurgeable(long jobInstanceId);

    /**
     * Gets JobInstanceEntity given the pre-purge search parameters within the WSSearchObject
     *
     * @param wsso
     *
     */
    public List<JobInstanceEntity> getJobInstances(IJPAQueryHelper queryHelper, int page, int pageSize);

    /**
     * @return
     */
    String getDisplayId();

    /**
     * @return
     */
    String getPersistenceType();

    /**
     *
     * @param jobInstanceID
     * @return list of group names if any exist
     */
    /*
     * public List<String> getGroupNamesForJobID(long jobInstanceID) throws NoSuchJobInstanceException;
     */

    public JobInstanceEntity updateJobInstanceWithGroupNames(long jobInstanceId, Set<String> groupNames);

    /**
     * Get the job repository table version number. This will initialize the persistent store (database)
     * if necessary in order to calculate and provide this value.
     *
     * @return job executions table version number
     * @throws Exception
     */
    int getJobExecutionEntityVersion() throws Exception;

    /**
     * Get the job repository table version number. This will initialize the persistent store (database)
     * if necessary in order to calculate and provide this value.
     *
     * @return job instances table version number
     * @throws Exception
     */
    int getJobInstanceEntityVersion() throws Exception;

    /**
     * @return the job execution version field, initialized or not (may return 'null')
     */
    Integer getJobExecutionEntityVersionField();

    /**
     * @return the job instance version field, initialized or not, (may return 'null')
     */
    Integer getJobInstanceEntityVersionField();

    /**
     * @return the step thread execution version field, initialized or not (may return 'null')
     */
    Integer getStepThreadExecutionEntityVersionField();

    /**
     * @param topLevelStepExecutionId
     * @return List of partition numbers, sorted low partition number to high, of related partitions in the recovery state.
     */
    public List<Integer> getRemotablePartitionsRecoveredForStepExecution(long topLevelStepExecutionId);

    /**
     * @param remotablePartitionKey
     * @return
     */
    public WSRemotablePartitionExecution createRemotablePartition(RemotablePartitionKey remotablePartitionKey);

    /**
     * @param remotablePartitionKey
     * @return
     */
    public WSRemotablePartitionState getRemotablePartitionInternalState(RemotablePartitionKey remotablePartitionKey);
}
