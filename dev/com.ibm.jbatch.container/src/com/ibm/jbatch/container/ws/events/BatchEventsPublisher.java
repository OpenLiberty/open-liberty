/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jbatch.container.ws.events;

import javax.batch.runtime.BatchStatus;

import com.ibm.jbatch.container.ws.WSJobExecution;
import com.ibm.jbatch.container.ws.WSJobInstance;
import com.ibm.jbatch.container.ws.WSStepThreadExecutionAggregate;

/**
 * Define topics to be published by the batch dispatcher and batch executor for various
 * job events. The topics are in a tree format and listeners can use appropriate JMS
 * wild card expression for filtering. Message is published is JMS TextMessage type and
 * its body contain a JSON text representation either a WSJobInstance or WSJobExecution
 * object. Also, the message also has the following JMS properties set (if available):
 * BatchJmsConstanst.PROPERTY_NAME_JOB_INSTANCE_ID
 * BatchJmsConstanst.PROPERTY_NAME_JOB_EXECUTION_ID
 *
 */
public interface BatchEventsPublisher {

    /**
     * Job events topic parts, used to construct the leave node topics
     *
     */
    public static final String TOPIC_ROOT = "batch";
    public static final String TOPIC_JOBS = "jobs";
    public static final String TOPIC_INSTANCE = "instance";
    public static final String TOPIC_EXECUTION = "execution";

    public static final String TOPIC_STEP = "step";
    public static final String TOPIC_PARTITION = "partition";
    public static final String TOPIC_SPLIT_FLOW = "split-flow";

    //============ TOPIC leave nodes ============== //
    // these are the ones that we use to publish  //

    /**
     * A message will be published for topic batch/jobs/instance/starting when a job instance is starting.
     *
     */
    public static final String TOPIC_INSTANCE_STARTING = TOPIC_ROOT + "/" + TOPIC_JOBS + "/" + TOPIC_INSTANCE + "/" + "starting";

    /**
     * A message will be published for topic batch/jobs/instance/submitted when a job instance is submitted.
     *
     */
    public static final String TOPIC_INSTANCE_SUBMITTED = TOPIC_ROOT + "/" + TOPIC_JOBS + "/" + TOPIC_INSTANCE + "/" + "submitted";
    /**
     * A message will be published for topic batch/jobs/instance/started when a job instance is started.
     *
     */
    public static final String TOPIC_INSTANCE_STARTED = TOPIC_ROOT + "/" + TOPIC_JOBS + "/" + TOPIC_INSTANCE + "/" + "started";

    /**
     * A message will be published for topic batch/jobs/instance/completed when a job instance is stopped.
     *
     */
    public static final String TOPIC_INSTANCE_COMPLETED = TOPIC_ROOT + "/" + TOPIC_JOBS + "/" + TOPIC_INSTANCE + "/" + "completed";
    /**
     * A message will be published for topic batch/jobs/instance/failed when a job instance is stopped.
     *
     */
    public static final String TOPIC_INSTANCE_FAILED = TOPIC_ROOT + "/" + TOPIC_JOBS + "/" + TOPIC_INSTANCE + "/" + "failed";

    /**
     * A message will be published for topic batch/jobs/instance/jms_queued when the dispatcher place a job instance on the queue.
     */
    public static final String TOPIC_INSTANCE_JMS_QUEUED = TOPIC_ROOT + "/" + TOPIC_JOBS + "/" + TOPIC_INSTANCE + "/" + "jms_queued";

    /**
     * A message will be published for topic batch/jobs/instance/jms_consumed when the dispatcher place a job instance on the queue.
     */
    public static final String TOPIC_INSTANCE_JMS_CONSUMED = TOPIC_ROOT + "/" + TOPIC_JOBS + "/" + TOPIC_INSTANCE + "/" + "jms_consumed";

    /**
     * A message will be published for topic batch/jobs/instance/dispatched when the batch runtime accepts the job instance for execution.
     */
    public static final String TOPIC_INSTANCE_DISPATCHED = TOPIC_ROOT + "/" + TOPIC_JOBS + "/" + TOPIC_INSTANCE + "/" + "dispatched";

    /**
     * A message will be published for topic batch/jobs/instance/purged when a job instance was purge successfully.
     */
    public static final String TOPIC_INSTANCE_PURGED = TOPIC_ROOT + "/" + TOPIC_JOBS + "/" + TOPIC_INSTANCE + "/" + "purged";

    /**
     * A message will be published for topic batch/jobs/instance/stopping when a job instance is stopping.
     *
     */
    public static final String TOPIC_INSTANCE_STOPPING = TOPIC_ROOT + "/" + TOPIC_JOBS + "/" + TOPIC_INSTANCE + "/" + "stopping";

    /**
     * A message will be published for topic batch/jobs/instance/stopped when a job instance is stopped.
     *
     */
    public static final String TOPIC_INSTANCE_STOPPED = TOPIC_ROOT + "/" + TOPIC_JOBS + "/" + TOPIC_INSTANCE + "/" + "stopped";

    /**
     * A message will be published for topic batch/jobs/execution/starting when a job execution is starting by the batch runtime.
     */
    public static final String TOPIC_EXECUTION_STARTING = TOPIC_ROOT + "/" + TOPIC_JOBS + "/" + TOPIC_EXECUTION + "/" + "starting";

    /**
     * A message will be published for topic batch/jobs/execution/stopping when a job execution is starting by the batch runtime.
     */
    public static final String TOPIC_EXECUTION_STOPPING = TOPIC_ROOT + "/" + TOPIC_JOBS + "/" + TOPIC_EXECUTION + "/" + "stopping";

    /**
     * A message will be published for topic batch/jobs/execution/started when a job execution is started by the batch runtime.
     */
    public static final String TOPIC_EXECUTION_STARTED = TOPIC_ROOT + "/" + TOPIC_JOBS + "/" + TOPIC_EXECUTION + "/" + "started";

    /**
     * A message will be published for topic batch/jobs/execution/completed when a job execution ends successfully.
     */
    public static final String TOPIC_EXECUTION_COMPLETED = TOPIC_ROOT + "/" + TOPIC_JOBS + "/" + TOPIC_EXECUTION + "/" + "completed";

    /**
     * A message will be published for topic batch/jobs/execution/failed when a job execution ends because of a failure.
     */
    public static final String TOPIC_EXECUTION_FAILED = TOPIC_ROOT + "/" + TOPIC_JOBS + "/" + TOPIC_EXECUTION + "/" + "failed";

    /**
     * A message will be published for topic batch/jobs/execution/abandoned when a job execution ends because of abandon operation.
     */
    public static final String TOPIC_EXECUTION_ABANDONED = TOPIC_ROOT + "/" + TOPIC_JOBS + "/" + TOPIC_EXECUTION + "/" + "abandoned";

    /**
     * A message will be published for topic batch/jobs/execution/failed when a job execution ends because of a failure.
     */
    public static final String TOPIC_EXECUTION_STOPPED = TOPIC_ROOT + "/" + TOPIC_JOBS + "/" + TOPIC_EXECUTION + "/" + "stopped";

    /**
     * A message will be published for topic batch/jobs/execution/restarting when the batch runtime restarts an execution.
     */
    public static final String TOPIC_EXECUTION_RESTARTING = TOPIC_ROOT + "/" + TOPIC_JOBS + "/" + TOPIC_EXECUTION + "/" + "restarting";

    /**
     * A message will be published for topic batch/jobs/execution/purged when the batch runtime purged execution.
     *
     * NOTE: There is no support for execution purge event because the container purges per instance id.
     */
    //public static final String TOPIC_EXECUTION_PURGED = TOPIC_ROOT + "/" + TOPIC_JOBS + "/" + TOPIC_EXECUTION + "/" + "purged";

    /**
     * A message will be published for topic batch/jobs/execution/split-flow/started when the batch runtime starts a split-flow path.
     */
    public static final String TOPIC_EXECUTION_SPLIT_FLOW_STARTED = TOPIC_ROOT + "/" + TOPIC_JOBS + "/" + TOPIC_EXECUTION + "/" + TOPIC_SPLIT_FLOW + "/" + "started";

    /**
     * A message will be published for topic batch/jobs/execution/split-flow/ended when the split-flow complete.
     */
    public static final String TOPIC_EXECUTION_SPLIT_FLOW_ENDED = TOPIC_ROOT + "/" + TOPIC_JOBS + "/" + TOPIC_EXECUTION + "/" + TOPIC_SPLIT_FLOW + "/" + "ended";

    /**
     * A message will be published for topic batch/job/execution/step/started when the batch runtime starts a step
     */
    public static final String TOPIC_EXECUTION_STEP_STARTED = TOPIC_ROOT + "/" + TOPIC_JOBS + "/" + TOPIC_EXECUTION + "/" + TOPIC_STEP + "/" + "started";

    /**
     * A message will be published for topic batch/job/execution/step/stopping when the batch runtime is stopping a step
     */
    public static final String TOPIC_EXECUTION_STEP_STOPPING = TOPIC_ROOT + "/" + TOPIC_JOBS + "/" + TOPIC_EXECUTION + "/" + TOPIC_STEP + "/" + "stopping";

    /**
     * A message will be published for topic batch/job/execution/step/completed when the batch runtime ends a step successfully
     */
    public static final String TOPIC_EXECUTION_STEP_COMPLETED = TOPIC_ROOT + "/" + TOPIC_JOBS + "/" + TOPIC_EXECUTION + "/" + TOPIC_STEP + "/" + "completed";

    /**
     * A message will be published for topic batch/job/execution/step/failed when the batch runtime ends a step in failure
     */
    public static final String TOPIC_EXECUTION_STEP_FAILED = TOPIC_ROOT + "/" + TOPIC_JOBS + "/" + TOPIC_EXECUTION + "/" + TOPIC_STEP + "/" + "failed";

    /**
     * A message will be published for topic batch/job/execution/step/stopped when the batch runtime stops a step
     */
    public static final String TOPIC_EXECUTION_STEP_STOPPED = TOPIC_ROOT + "/" + TOPIC_JOBS + "/" + TOPIC_EXECUTION + "/" + TOPIC_STEP + "/" + "stopped";
    /**
     * A message will be published for topic batch/job/execution/step/checkpoint when the batch runtime setting checkpoint data (after beginCheckpoint() and before endCheckpoint())
     */
    public static final String TOPIC_EXECUTION_STEP_CHECKPOINT = TOPIC_ROOT + "/" + TOPIC_JOBS + "/" + TOPIC_EXECUTION + "/" + TOPIC_STEP + "/" + "checkpoint";

    /**
     * A message will be published for topic batch/job/execution/partition/started when the batch runtime starts a partition
     */
    public static final String TOPIC_EXECUTION_PARTITION_STARTED = TOPIC_ROOT + "/" + TOPIC_JOBS + "/" + TOPIC_EXECUTION + "/" + TOPIC_PARTITION + "/" + "started";

    /**
     * A message will be published for topic batch/job/execution/partition/failed when the batch runtime ends a partition
     */
    public static final String TOPIC_EXECUTION_PARTITION_FAILED = TOPIC_ROOT + "/" + TOPIC_JOBS + "/" + TOPIC_EXECUTION + "/" + TOPIC_PARTITION + "/" + "failed";

    /**
     * A message will be published for topic batch/job/execution/partition/completed when the batch runtime ends a partition
     */
    public static final String TOPIC_EXECUTION_PARTITION_COMPLETED = TOPIC_ROOT + "/" + TOPIC_JOBS + "/" + TOPIC_EXECUTION + "/" + TOPIC_PARTITION + "/" + "completed";

    /**
     * A message will be published for topic batch/job/execution/partition/stopped when the batch runtime ends a partition
     */
    public static final String TOPIC_EXECUTION_PARTITION_STOPPED = TOPIC_ROOT + "/" + TOPIC_JOBS + "/" + TOPIC_EXECUTION + "/" + TOPIC_PARTITION + "/" + "stopped";

    /**
     * A message will be published for topic batch/job/execution/jobLogPart when the new job log part is created
     */
    public static final String TOPIC_JOB_LOG_PART = TOPIC_ROOT + "/" + TOPIC_JOBS + "/" + TOPIC_EXECUTION + "/" + "jobLogPart";

    /**
     * Publish the WSJobInstance data to this topic.
     * The message will be JMSTextMessage, and the body contain json format representation of the object
     *
     * @param objectToPublish WSJobInstance object
     * @param event topic to publish
     * @param correlationId may be null
     */
    public abstract void publishJobInstanceEvent(WSJobInstance objectToPublish, String event, String correlationId);

    public abstract void publishJobExecutionEvent(WSJobExecution objectToPublish, String event, String correlationId);

    public abstract void publishStepEvent(WSStepThreadExecutionAggregate objectToPublish, String event, String correlationId);

    public abstract void publishCheckpointEvent(String stepName, long jobInstanceId, long jobExecutionId, long stepExecutionId, String correlationId);

    public abstract void publishPartitionEvent(int partitionNumber, BatchStatus batchStatus, String exitStatus, String stepName,
                                               long topLevelInstanceId, long topLevelExecutionId, long topLevelStepExecutionId,
                                               String eventToPublish, String correlationId);

    /**
     * Publish split-flow data
     *
     * @param splitName
     * @param flowName
     * @param instanceId
     * @param executionId
     * @param splitFlowTopicString
     * @param correlationId may be null
     */
    public abstract void publishSplitFlowEvent(String splitName, String flowName,
                                               long instanceId, long executionId,
                                               String splitFlowTopicString, String correlationId);

    /**
     * Publish a job log event
     *
     * @param topLevelInstanceId
     * @param topLevelExecutionId
     * @param appName
     * @param correlationId- may be null
     * @param partitionStep- may be null
     * @param partitionNum- may be null
     * @param splitName- may be null
     * @param flowName- may be null
     * @param partNum
     * @param finalLog
     * @param jobLogContent
     */
    public abstract void publishJobLogEvent(long topLevelInstanceId, long topLevelExecutionId, String appName,
                                            String partitionStep, Integer partitionNum, String splitName,
                                            String flowName, int partNum, boolean finalLog, String jobLogContent, String correlationId);

    /*
     * Get the 'batch' topic root.
     *
     * @return default or modified event topic root string.
     */
    public abstract String resolveTopicRoot(String eventString);

}
