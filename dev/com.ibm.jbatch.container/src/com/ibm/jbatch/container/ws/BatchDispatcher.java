/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jbatch.container.ws;

import java.util.Properties;

import javax.batch.operations.JobExecutionNotRunningException;
import javax.batch.operations.JobSecurityException;
import javax.batch.operations.NoSuchJobExecutionException;

import com.ibm.jbatch.jsl.model.Step;

/**
 * There are currently two implementations of the BatchDispatcher:
 * 1) BatchLocalDispatcher - for dispatching jobs locally (same server)
 * 2) BatchJmsDispatcher - for queuing jobs to JMS.
 *
 * Both impls may be active in the same server. For example a server
 * may receive job submissions over the REST API and use the BatchJmsDispatcher
 * to queue the job submission to JMS. That same server may also be a JMS
 * listener and may end up consuming that job submission off the JMS queue,
 * at which point it dispatches the job locally via BatchLocalDispatcher.
 *
 * The high-level flow looks something like this:
 *
 * client -> REST API -> BatchJmsDispatcher -> JMS queue -> endpoint -> BatchInternalDispatcher -> job runs
 *
 * Without JMS it looks like:
 *
 * client -> REST API -> BatchLocalDispatcher -> BatchInternalDispatcher -> job runs
 *
 * The BatchJmsDispatcher is also used for starting multi-jvm partitions.
 * The high-level flow for that is:
 *
 * PartitionedStepControllerImpl (top-level thread) -> BatchJmsDispatcher -> JMS queue -> endpoint -> BatchLocalDispatcher -> partition runs
 *
 */
public interface BatchDispatcher {

    /**
     *
     * @param jobInstance
     * @param jobParameters
     *
     */
    public void start(WSJobInstance jobInstance, Properties jobParameters, long executionId) throws BatchDispatcherException;

    /**
     * @param instanceId the job instance to restart
     *
     */
    public void restartInstance(long instanceId, Properties restartParameters, long executionId) throws BatchDispatcherException;

    /**
     * @param the job instance id to mark failed and correlation id to publish
     *
     */
    public void markInstanceExecutionFailed(long instanceId, long executionId, String correlationId);

    /**
     * @param executionId the job execution to stop
     * @throws BatchJobNotLocalException
     */
    public void stop(long executionId) throws NoSuchJobExecutionException, JobExecutionNotRunningException, JobSecurityException, BatchDispatcherException, BatchJobNotLocalException;

    /**
     * Start a sub-job partition.
     */
    public void startPartition(PartitionPlanConfig partitionPlanConfig, Step step, PartitionReplyQueue partitionReplyQueue) throws BatchDispatcherException;

    /**
     * Create a JMS-enabled PartitionReplyQueue that can be used to communicate between
     * the top-level thread and partition threads over JMS.
     *
     * Note: this method should only be called if the impl is BatchJmsDispatcher.
     * If it's not, then it should throw an exception.
     *
     * @return JMS-enabled PartitionReplyQueue
     */
    public PartitionReplyQueue createPartitionReplyQueue() throws BatchDispatcherException;

}
