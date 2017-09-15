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
package com.ibm.jbatch.container.services;

import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;

import javax.batch.operations.JobExecutionAlreadyCompleteException;
import javax.batch.operations.JobExecutionNotMostRecentException;
import javax.batch.operations.JobExecutionNotRunningException;
import javax.batch.operations.JobRestartException;
import javax.batch.operations.JobStartException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.runtime.BatchStatus;

import com.ibm.jbatch.container.util.BatchPartitionWorkUnit;
import com.ibm.jbatch.container.util.BatchSplitFlowWorkUnit;
import com.ibm.jbatch.container.util.BatchWorkUnit;
import com.ibm.jbatch.container.util.SplitFlowConfig;
import com.ibm.jbatch.container.ws.PartitionPlanConfig;
import com.ibm.jbatch.container.ws.PartitionReplyQueue;
import com.ibm.jbatch.container.ws.WSJobInstance;
import com.ibm.jbatch.jsl.model.JSLJob;
import com.ibm.jbatch.jsl.model.Step;
import com.ibm.jbatch.spi.services.IBatchServiceBase;

public interface IBatchKernelService extends IBatchServiceBase {

    /*
     * Top-level job stuff
     */
    WSJobInstance createJobInstance(String appName, String jobXMLName, String submitter, String jsl, String correlationId);

    WSJobInstance createJobInstanceIntraApplication(String jobXMLName, String runAsUser);

    /**
     * Creates the job execution record and dispatches the job.
     *
     * @param jobInstance the jobInstance to be started
     * @param jobXML
     * @param the job parameters supplied by the user when submitting the job
     * @param the executionId for the job
     * @return A Map.Entry with executionId of the newly started job as Key and Future object as value.
     *
     */
    Entry<Long, Future<?>> startJob(WSJobInstance jobInstance, IJobXMLSource jobXML, Properties jobParameters, long executionId) throws JobStartException;

    /**
     * Restarts the job execution record
     *
     * @param the executionId to be restarted
     * @param jobOverrideProperties supplied by submitter on restart
     * @return A Map.Entry with executionId of the newly restarted job as Key and Future object as value.
     *
     */
    Entry<Long, Future<?>> restartJob(long executionID,
                                      Properties overrideJobParameters) throws JobRestartException, JobExecutionAlreadyCompleteException, JobExecutionNotMostRecentException, NoSuchJobExecutionException;

    /**
     * Restarts the job instance record
     *
     * @param the instanceId of the job to be restarted
     * @param jobXML of the job to be restarted
     * @param Properties supplied by submitter on restart
     * @param last executionId of the job to restarted
     * @return A Map.Entry with executionId of the newly restarted job as Key and Future object as value.
     */
    Entry<Long, Future<?>> restartJobInstance(long instanceID, IJobXMLSource jobXML, Properties overrideJobParameters,
                                              long executionId) throws JobRestartException, JobExecutionAlreadyCompleteException, JobExecutionNotMostRecentException, NoSuchJobExecutionException;

    /*
     * Partition and Split-Flow methods
     */
    BatchPartitionWorkUnit createPartitionWorkUnit(PartitionPlanConfig config,
                                                   Step step,
                                                   PartitionReplyQueue partitionReplyQueue,
                                                   boolean isRemoteDispatch);

    /**
     * Runs the batch partition
     *
     * @param the Partition work unit to be started
     * @return A Future object representing the newly started partition.
     *
     */
    Future<?> runPartition(BatchPartitionWorkUnit batchWork);

    /**
     * @param flowName
     * @param splitFlowJobModel
     * @param completedWorkQueue
     * @param topLevelNameInstanceExecutionInfo
     * @return
     */
    BatchSplitFlowWorkUnit createSplitFlowWorkUnit(SplitFlowConfig splitFlowConfig, JSLJob splitFlowJobModel, BlockingQueue<BatchSplitFlowWorkUnit> completedWorkQueue);

    void runSplitFlow(BatchSplitFlowWorkUnit batchWork);

    /*
     * Stop/deregister/shutdown methods
     */
    public void stopJob(long executionId) throws JobExecutionNotRunningException, NoSuchJobExecutionException;

    void stopWorkUnit(BatchWorkUnit workUnit) throws NoSuchJobExecutionException, JobExecutionNotRunningException;

    void workUnitCompleted(BatchWorkUnit workUnit);

    /**
     * Shutdown the batch kernel service. The batch kernel should take care
     * of quiescing active jobs for an orderly shutdown.
     */
    @Override
    void shutdown();

    /**
     * Create the BatchPartitionWorkUnit and start the sub-job partition thread.
     *
     * @return A Map.Entry with BatchPartitionWorkUnit for the newly started partition as key and Future object as value
     */
    public Entry<BatchPartitionWorkUnit, Future<?>> startPartition(PartitionPlanConfig partitionPlanConfig, Step step, PartitionReplyQueue partitionReplyQueue,
                                                                   boolean isRemoteDispatch);

    /**
     * Retrieves the batch status of the specified job execution from the
     * in memory collection of executing jobs.
     *
     * @param the specified job to locate
     *
     * @return the batch status of the specified job, or null if not found
     */
    public BatchStatus getBatchStatus(long jobExecutionId);
}
