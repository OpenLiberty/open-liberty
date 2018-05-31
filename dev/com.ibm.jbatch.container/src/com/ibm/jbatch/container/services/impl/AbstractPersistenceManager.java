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
/**
 *
 */
package com.ibm.jbatch.container.services.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.operations.NoSuchJobInstanceException;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;

import com.ibm.jbatch.container.RASConstants;
import com.ibm.jbatch.container.context.impl.MetricImpl;
import com.ibm.jbatch.container.exception.BatchIllegalJobStatusTransitionException;
import com.ibm.jbatch.container.exception.PersistenceException;
import com.ibm.jbatch.container.execution.impl.RuntimePartitionExecution;
import com.ibm.jbatch.container.execution.impl.RuntimeSplitFlowExecution;
import com.ibm.jbatch.container.execution.impl.RuntimeStepExecution;
import com.ibm.jbatch.container.persistence.jpa.JobExecutionEntity;
import com.ibm.jbatch.container.persistence.jpa.JobInstanceEntity;
import com.ibm.jbatch.container.persistence.jpa.RemotablePartitionEntity;
import com.ibm.jbatch.container.persistence.jpa.RemotablePartitionKey;
import com.ibm.jbatch.container.persistence.jpa.RemotableSplitFlowEntity;
import com.ibm.jbatch.container.persistence.jpa.RemotableSplitFlowKey;
import com.ibm.jbatch.container.persistence.jpa.StepThreadExecutionEntity;
import com.ibm.jbatch.container.persistence.jpa.TopLevelStepExecutionEntity;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.ws.InstanceState;
import com.ibm.jbatch.container.ws.WSStepThreadExecutionAggregate;

/**
 * @author skurz
 *
 */
public abstract class AbstractPersistenceManager implements IPersistenceManagerService {

    private final static Logger logger = Logger.getLogger(AbstractPersistenceManager.class.getName(),
                                                          RASConstants.BATCH_MSG_BUNDLE);

    public static List<BatchStatus> RUNNING_STATUSES = Collections.unmodifiableList(Arrays.asList(new BatchStatus[] { BatchStatus.STARTED, BatchStatus.STARTING,
                                                                                                                      BatchStatus.STOPPING }));
    public static List<BatchStatus> FINAL_STATUSES = Arrays.asList(new BatchStatus[] { BatchStatus.STOPPED, BatchStatus.ABANDONED, BatchStatus.FAILED, BatchStatus.COMPLETED });
    private static Set<BatchStatus> FINAL_STATUS_SET = Collections.unmodifiableSet(new HashSet<BatchStatus>(FINAL_STATUSES));

    public static boolean isFinalBatchStatus(BatchStatus batchStatus) {
        return FINAL_STATUS_SET.contains(batchStatus);
    }

    public static List<InstanceState> FINAL_INSTANCE_STATES = Arrays.asList(new InstanceState[] { InstanceState.STOPPED, InstanceState.FAILED, InstanceState.COMPLETED });
    private static Set<InstanceState> FINAL_INSTANCE_STATE_SET = Collections.unmodifiableSet(new HashSet<InstanceState>(FINAL_INSTANCE_STATES));

    public static boolean isFinalInstanceState(InstanceState instanceState) {
        return FINAL_INSTANCE_STATE_SET.contains(instanceState);
    }

    @Override
    public String getDisplayId() {
        return null;
    }

    @Override
    public long getJobInstanceIdFromExecutionId(long jobExecutionId) throws NoSuchJobExecutionException {

        return getJobInstanceFromExecutionId(jobExecutionId).getInstanceId();
    }

    @Override
    public String getJobInstanceAppName(long jobInstanceId) throws NoSuchJobInstanceException {

        return getJobInstance(jobInstanceId).getAmcName();
    }

    @Override
    public String getJobInstanceAppNameFromExecutionId(long jobExecutionId) throws NoSuchJobExecutionException {

        return getJobInstanceFromExecutionId(jobExecutionId).getAmcName();
    }

    @Override
    public String getJobInstanceSubmitter(long jobInstanceId) throws NoSuchJobInstanceException {

        return getJobInstance(jobInstanceId).getSubmitter();
    }

    //
    //	@Override
    //
    //	public Map<String, List<StepThreadExecutionEntity>> getStepThreadExecutionsForJobExecution(long jobExecutionId) throws NoSuchJobExecutionException {
    //		Map<String, List<StepThreadExecutionEntity>> resultMap =
    //				new HashMap<String, List<StepThreadExecutionEntity>>();
    //
    //		List<StepThreadExecutionEntity> unsortedList = getStepThreadExecutionsForJobExecutionUnsorted(jobExecutionId);
    //
    //		for (StepThreadExecutionEntity s : unsortedList) {
    //			String stepName = s.getStepName();
    //			if (resultMap.containsKey(stepName)) {
    //				List<StepThreadExecutionEntity> list = resultMap.get(stepName);
    //				if (s instanceof TopLevelStepExecutionEntity) {
    //					list.add(0,s);
    //				} else {
    //					list.add(s);
    //				}
    //			} else {
    //				List<StepThreadExecutionEntity> list = new ArrayList<StepThreadExecutionEntity>();
    //				list.add(s);
    //				resultMap.put(stepName, list);
    //			}
    //		}
    //		return resultMap;
    //	}

    @Override
    public boolean isJobInstancePurgeable(long jobInstanceId) {
        InstanceState instanceState = getJobInstance(jobInstanceId).getInstanceState();

        if (instanceState.equals(InstanceState.SUBMITTED)
            || instanceState.equals(InstanceState.JMS_QUEUED)
            || instanceState.equals(InstanceState.JMS_CONSUMED)
            || instanceState.equals(InstanceState.DISPATCHED)) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public JobInstanceEntity updateJobInstanceStateOnConsumed(long instanceId) throws BatchIllegalJobStatusTransitionException {
        JobInstanceEntity retVal = getJobInstance(instanceId);

        InstanceState currentState = retVal.getInstanceState();
        if (FINAL_INSTANCE_STATE_SET.contains(currentState)) {
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Do nothing, instance = " + instanceId + " is already in final state: " + currentState);
            }
        } else if (currentState == InstanceState.JMS_CONSUMED) {
            // we want to allow re-processing the same message after a rollback
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Ignore, instance = " + instanceId + " already in JMS_CONSUMED state");
            }
        } else if (currentState == InstanceState.JMS_QUEUED) {
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Transition instance = " + instanceId + " to JMS_CONSUMED state");
            }
            retVal = updateJobInstanceWithInstanceState(instanceId, InstanceState.JMS_CONSUMED, new Date());
        } else {
            String excMsg = "Illegal attempt to transition from instance state = " + currentState + " to JMS_CONSUMED.  Throwing exception.";
            logger.fine(excMsg);
            throw new BatchIllegalJobStatusTransitionException(excMsg);
        }

        return retVal;
    }

    @Override
    public JobInstanceEntity updateJobInstanceStateOnQueued(long instanceId) throws BatchIllegalJobStatusTransitionException {
        JobInstanceEntity retVal = getJobInstance(instanceId);

        InstanceState currentState = retVal.getInstanceState();
        if (FINAL_INSTANCE_STATE_SET.contains(currentState)) {
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Do nothing, instance = " + instanceId + " is already in final state: " + currentState);
            }
        } else if (currentState == InstanceState.SUBMITTED) {
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Transition instance = " + instanceId + " to JMS_QUEUED state");
            }
            retVal = updateJobInstanceWithInstanceState(instanceId, InstanceState.JMS_QUEUED, new Date());
        } else {
            String excMsg = "Illegal attempt to transition from instance state = " + currentState + " to JMS_QUEUED.  Throwing exception.";
            logger.fine(excMsg);
            throw new BatchIllegalJobStatusTransitionException(excMsg);
        }

        return retVal;
    }

    @Override
    public JobExecutionEntity getJobExecutionMostRecent(long jobInstanceId) throws IllegalStateException, NoSuchJobInstanceException {

        List<JobExecutionEntity> executions = getJobExecutionsFromJobInstanceId(jobInstanceId);

        if (executions == null || executions.size() == 0) {
            throw new IllegalStateException("Did not find any executions associated with instance id: " + jobInstanceId);
        } else {
            return executions.get(0);
        }
    }

    @Override
    public long getJobExecutionIdMostRecent(long jobInstanceId) {
        return getJobExecutionMostRecent(jobInstanceId).getExecutionId();
    }

    @Override
    public Properties getJobExecutionParameters(long jobExecutionId) throws NoSuchJobExecutionException {
        return getJobExecution(jobExecutionId).getJobParameters();
    }

    /**
     *
     * @param stepExecutionId
     * @return
     * @throws IllegalArgumentException if either:
     *             1) we have no entry at all with id equal to <code>stepExecutionId</code>
     *             2) we have a partition-level StepThreadExecutionEntity with this id (but not a top-level entry).
     */
    @Override
    public TopLevelStepExecutionEntity getStepExecutionTopLevel(long stepExecutionId) throws IllegalArgumentException {

        StepThreadExecutionEntity stepExec = getStepThreadExecution(stepExecutionId);

        if (stepExec == null) {
            throw new IllegalArgumentException("No top-level step thread execution found for key = " + stepExecutionId);
        }
        try {
            TopLevelStepExecutionEntity topLevelStepExec = (TopLevelStepExecutionEntity) stepExec;
            return topLevelStepExec;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Found step thread execution found for key = " + stepExecutionId +
                                               ", but it was a partition-level step thread, not a top-level execution");
        }
    }

    protected abstract StepThreadExecutionEntity getStepThreadExecution(long stepExecutionId);

    /**
     * {@inheritDoc}
     */
    @Override
    public List<WSStepThreadExecutionAggregate> getStepExecutionAggregatesFromJobExecutionId(
                                                                                             long jobExecutionId) throws NoSuchJobExecutionException {
        // Optimize for programming ease rather than a single trip to the database.
        // We might have to revisit.
        List<WSStepThreadExecutionAggregate> retVal = new ArrayList<WSStepThreadExecutionAggregate>();

        //sorted by start time
        List<StepExecution> topLevelStepExecutions = getStepExecutionsTopLevelFromJobExecutionId(jobExecutionId);
        for (StepExecution stepExec : topLevelStepExecutions) {
            retVal.add(getStepExecutionAggregate(stepExec.getStepExecutionId()));
        }
        return retVal;
    }

    /* Additional helper methods */

    /**
     * This method is used to serialized an object saved into a table BLOB field.
     *
     * @param theObject the object to be serialized
     * @return a object byte array
     * @throws IOException
     */
    protected byte[] serializeObject(Serializable theObject) throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(baos);
        oout.writeObject(theObject);
        byte[] data = baos.toByteArray();
        baos.close();
        oout.close();

        return data;
    }

    protected void updateStepExecutionStatusTimeStampsUserDataAndMetrics(StepThreadExecutionEntity stepExec, RuntimeStepExecution runtimeStepExecution) {

        stepExec.setBatchStatus(runtimeStepExecution.getBatchStatus());
        stepExec.setExitStatus(runtimeStepExecution.getExitStatus());
        stepExec.setStepName(runtimeStepExecution.getStepName());
        stepExec.setStartTime(runtimeStepExecution.getStartTime());
        stepExec.setEndTime(runtimeStepExecution.getEndTime());
        try {
            stepExec.setPersistentUserDataBytes(serializeObject(runtimeStepExecution.getPersistentUserDataObject()));
        } catch (IOException e) {
            throw new PersistenceException(e);
        }
        updateStepExecutionMetrics(stepExec, runtimeStepExecution.getMetrics());

        // Note there's nothing in the spec that says we have to do this, but seems useful.
        stepExec.getJobExecution().setLastUpdatedTime(runtimeStepExecution.getLastUpdatedTime());
    }

    private void updateStepExecutionMetrics(StepThreadExecutionEntity stepData, List<Metric> metrics) {
        for (int i = 0; i < metrics.size(); i++) {
            if (metrics.get(i).getType().equals(MetricImpl.MetricType.READ_COUNT)) {
                stepData.setReadCount(metrics.get(i).getValue());
            } else if (metrics.get(i).getType().equals(MetricImpl.MetricType.WRITE_COUNT)) {
                stepData.setWriteCount(metrics.get(i).getValue());
            } else if (metrics.get(i).getType().equals(MetricImpl.MetricType.PROCESS_SKIP_COUNT)) {
                stepData.setProcessSkipCount(metrics.get(i).getValue());
            } else if (metrics.get(i).getType().equals(MetricImpl.MetricType.COMMIT_COUNT)) {
                stepData.setCommitCount(metrics.get(i).getValue());
            } else if (metrics.get(i).getType().equals(MetricImpl.MetricType.ROLLBACK_COUNT)) {
                stepData.setRollbackCount(metrics.get(i).getValue());
            } else if (metrics.get(i).getType().equals(MetricImpl.MetricType.READ_SKIP_COUNT)) {
                stepData.setReadSkipCount(metrics.get(i).getValue());
            } else if (metrics.get(i).getType().equals(MetricImpl.MetricType.FILTER_COUNT)) {
                stepData.setFilterCount(metrics.get(i).getValue());
            } else if (metrics.get(i).getType().equals(MetricImpl.MetricType.WRITE_SKIP_COUNT)) {
                stepData.setWriteSkipCount(metrics.get(i).getValue());
            }
        }
    }

    //
    // Leave the methods in for now.  It will be easier to see where they
    // are called today, whereas if we remove completely the impls and the clients,
    // we kind of have to start over to re-enabled.
    //
    @Override
    public RemotableSplitFlowEntity createSplitFlowExecution(
                                                             RemotableSplitFlowKey splitFlowKey, Date createTime) {
        return null;
    }

    @Override
    public RemotableSplitFlowEntity updateSplitFlowExecution(
                                                             RuntimeSplitFlowExecution runtimeSplitFlowExecution,
                                                             BatchStatus newBatchStatus, Date date) {
        return null;
    }

    @Override
    public RemotableSplitFlowEntity updateSplitFlowExecutionLogDir(
                                                                   RemotableSplitFlowKey key, String logDirPath) {
        return null;
    }

    @Override
    public RemotablePartitionEntity createPartitionExecution(
                                                             RemotablePartitionKey partitionKey, Date createTime) {
        return null;
    }

    @Override
    public RemotablePartitionEntity updatePartitionExecution(
                                                             RuntimePartitionExecution runtimePartitionExecution,
                                                             BatchStatus newBatchStatus, Date date) {
        return null;
    }

    @Override
    public RemotablePartitionEntity updatePartitionExecutionLogDir(
                                                                   RemotablePartitionKey key, String logDirPath) {
        return null;
    }

    /**
     * TODO - The 'verify' methods that follow are a basic ground work to guard against the invalid transition of job Batch Status / instance State.
     * For now we are only disallowing updates once batch status or instance state are marked 'COMPLETED'/'ABANDONED'.
     * <p>These checks could potentially be improved. E.g. <p><ul>
     * <li>Should an job execution status ever transition from FAILED (except maybe to ABANDONED)?
     * <li>In contrast a job instance needs to transition out of FAILED status during a RESTART.
     * </ul><p>
     * A limitation of this approach is that we're not leveraging DB locking like we do in the DB-based persistence. This is likely to end up
     * providing just one more check rather than being continually developed as the basis of our status transition validation.
     */
    protected void verifyStatusTransitionIsValid(JobExecutionEntity exec, BatchStatus toStatus) throws BatchIllegalJobStatusTransitionException {

        switch (exec.getBatchStatus()) {
            case COMPLETED:
                //COMPLETED to ABANDONED is allowed since it's already allowed in released TCK tests.
                if (toStatus == BatchStatus.ABANDONED) {
                    break;
                }
            case ABANDONED:
                throw new BatchIllegalJobStatusTransitionException("Job execution: " + exec.getExecutionId()
                                                                   + " cannot be transitioned from Batch Status: " + exec.getBatchStatus().name()
                                                                   + " to " + toStatus.name());
            case STARTING:
            case STARTED:
            case STOPPING:
            case FAILED:

            default:
        }

    }

    /**
     * See description of: {@link AbstractPersistenceManager#verifyStatusTransitionIsValid(JobExecutionEntity, BatchStatus)
     *
     * @param instance
     * @param toStatus
     * @throws BatchIllegalJobStatusTransitionException
     */
    protected void verifyStatusTransitionIsValid(JobInstanceEntity instance, BatchStatus toStatus) throws BatchIllegalJobStatusTransitionException {

        switch (instance.getBatchStatus()) {
            case COMPLETED:
                //COMPLETED to ABANDONED is allowed since it's already allowed in released TCK tests.
                if (toStatus == BatchStatus.ABANDONED) {
                    break;
                }
            case ABANDONED:
                throw new BatchIllegalJobStatusTransitionException("Job instance: " + instance.getInstanceId()
                                                                   + " cannot be transitioned from Batch Status: " + instance.getBatchStatus().name()
                                                                   + " to " + toStatus.name());
            case STARTING:
            case STARTED:
            case STOPPING:
            case FAILED:

            default:
        }

    }

    /**
     *
     * See description of: {@link AbstractPersistenceManager#verifyStatusTransitionIsValid(JobExecutionEntity, BatchStatus)
     *
     * @param stepExec
     * @param toStatus
     * @throws BatchIllegalJobStatusTransitionException
     */
    protected void verifyThreadStatusTransitionIsValid(StepThreadExecutionEntity stepExec, BatchStatus toStatus) throws BatchIllegalJobStatusTransitionException {

        switch (stepExec.getBatchStatus()) {
            case COMPLETED:
                //COMPLETED to ABANDONED is allowed since it's already allowed in released TCK tests.
                if (toStatus == BatchStatus.ABANDONED) {
                    break;
                }
            case ABANDONED:
                throw new BatchIllegalJobStatusTransitionException("Job Step Thread execution: " + stepExec.getStepExecutionId()
                                                                   + " cannot be transitioned from Batch Status: " + stepExec.getBatchStatus().name()
                                                                   + " to " + toStatus.name());
            case STARTING:
            case STARTED:
            case STOPPING:
            case FAILED:

            default:
        }

    }

    /**
     *
     * See description of: {@link AbstractPersistenceManager#verifyStatusTransitionIsValid(JobExecutionEntity, BatchStatus)
     *
     * @param jobInstance
     * @param toState
     * @throws BatchIllegalJobStatusTransitionException
     */
    protected void verifyStateTransitionIsValid(JobInstanceEntity jobInstance, InstanceState toState) throws BatchIllegalJobStatusTransitionException {

        switch (jobInstance.getInstanceState()) {
            case COMPLETED:
                //COMPLETED to ABANDONED is allowed since it's already allowed in released TCK tests.
                if (toState == InstanceState.ABANDONED) {
                    break;
                }
            case ABANDONED:
                throw new BatchIllegalJobStatusTransitionException("Job Instance: " + jobInstance.getInstanceId()
                                                                   + " cannot be transitioned from Instance State: " + jobInstance.getInstanceState().name()
                                                                   + " to " + toState.name());
            case SUBMITTED:
            case JMS_QUEUED:
            case JMS_CONSUMED:
            case DISPATCHED:
            case FAILED:
            case STOPPED:
            default:
        }

    }
}
