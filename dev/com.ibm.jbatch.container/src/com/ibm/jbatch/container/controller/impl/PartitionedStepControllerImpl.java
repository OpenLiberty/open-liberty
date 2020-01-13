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
package com.ibm.jbatch.container.controller.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.api.partition.PartitionPlan;
import javax.batch.api.partition.PartitionReducer.PartitionStatus;
import javax.batch.operations.JobExecutionNotRunningException;
import javax.batch.operations.JobRestartException;
import javax.batch.runtime.BatchStatus;

import com.ibm.jbatch.container.artifact.proxy.InjectionReferences;
import com.ibm.jbatch.container.artifact.proxy.PartitionAnalyzerProxy;
import com.ibm.jbatch.container.artifact.proxy.PartitionMapperProxy;
import com.ibm.jbatch.container.artifact.proxy.PartitionReducerProxy;
import com.ibm.jbatch.container.artifact.proxy.ProxyFactory;
import com.ibm.jbatch.container.artifact.proxy.StepListenerProxy;
import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;
import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.container.execution.impl.RuntimeWorkUnitExecution;
import com.ibm.jbatch.container.execution.impl.RuntimeWorkUnitExecution.StopLock;
import com.ibm.jbatch.container.jsl.CloneUtility;
import com.ibm.jbatch.container.persistence.jpa.EntityConstants;
import com.ibm.jbatch.container.persistence.jpa.TopLevelStepExecutionEntity;
import com.ibm.jbatch.container.persistence.jpa.TopLevelStepInstanceEntity;
import com.ibm.jbatch.container.persistence.jpa.TopLevelStepInstanceKey;
import com.ibm.jbatch.container.servicesmanager.ServicesManagerStaticAnchor;
import com.ibm.jbatch.container.status.ExecutionStatus;
import com.ibm.jbatch.container.util.BatchPartitionWorkUnit;
import com.ibm.jbatch.container.validation.ArtifactValidationException;
import com.ibm.jbatch.container.ws.BatchDispatcher;
import com.ibm.jbatch.container.ws.JoblogUtil;
import com.ibm.jbatch.container.ws.PartitionPlanConfig;
import com.ibm.jbatch.container.ws.PartitionReplyMsg;
import com.ibm.jbatch.container.ws.PartitionReplyTimeoutConstants;
import com.ibm.jbatch.container.ws.impl.PartitionReplyQueueLocal;
import com.ibm.jbatch.container.ws.impl.StringUtils;
import com.ibm.jbatch.container.ws.smf.ZosJBatchSMFLogging;
import com.ibm.jbatch.jsl.model.Analyzer;
import com.ibm.jbatch.jsl.model.JSLProperties;
import com.ibm.jbatch.jsl.model.PartitionMapper;
import com.ibm.jbatch.jsl.model.PartitionReducer;
import com.ibm.jbatch.jsl.model.Property;
import com.ibm.jbatch.jsl.model.Step;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.serialization.DeserializationObjectInputStream;

/**
 * This is the "top-level" controller, which spawns the partitions
 * and waits for them to finish. The partitions are either BatchletStepControllerImpls
 * or ChunkStepControllerImpls.
 *
 * Partitions are spawned under startPartition. The partitions communicate
 * their status back to the top-level thread via the analzyerStatusQueue.
 */
public class PartitionedStepControllerImpl extends BaseStepControllerImpl {

    private final static Logger logger = Logger.getLogger(PartitionedStepControllerImpl.class.getName());

    private enum ExecutionType {
        START, RESTART_NORMAL, RESTART_OVERRIDE, RESTART_AFTER_COMPLETION
    };

    private ExecutionType executionType = null;

    private final boolean isMultiJvm;

    /**
     * The partition work units.
     *
     * Declared 'volatile' to ensure thread safety in case a stop() is issued while
     * building the work units.
     */
    private volatile List<BatchPartitionWorkUnit> parallelBatchWorkUnits = new ArrayList<BatchPartitionWorkUnit>();

    /**
     * User-supplied object that gets called after all partitions have completed
     * (a la the "reduce" in map/reduce).
     */
    private PartitionReducerProxy partitionReducerProxy = null;

    /**
     * User-supplied object that gets called to process analyzerQueue data passed
     * back to the top-level from the partitions as they checkpoint/complete.
     */
    private PartitionAnalyzerProxy analyzerProxy = null;

    /**
     * TODO: this could possibly be moved to the parent class, since the other derived class,
     * SingleThreadedStepController, declares the same field.
     */
    protected List<StepListenerProxy> stepListeners = null;

    /**
     * If the BatchJmsDispatcher is available, then it's used for starting remote partitions
     * (multi-jvm mode).
     */
    private BatchDispatcher batchJmsDispatcher = null;

    private PartitionPlanDescriptor currentPlan;

    private static class FinishedPartition {
        Integer partitionNum;
        BatchStatus batchStatus;

        FinishedPartition(Integer partitionNum, BatchStatus batchStatus) {
            this.partitionNum = partitionNum;
            this.batchStatus = batchStatus;
        }
    }

    List<Integer> partitionsToExecute;
    List<Integer> startedPartitions = new ArrayList<Integer>();
    List<FinishedPartition> finishedPartitions = new ArrayList<FinishedPartition>();
    Set<Integer> finishedPartitionNumbers = new HashSet<Integer>();
    List<Throwable> analyzerExceptions = new ArrayList<Throwable>();

    // Better to keep track of whether we called rollback instead of having to check the status later on.
    private boolean rollbackPartitionedStepInvoked = false;

    /**
     * CTOR.
     */
    public PartitionedStepControllerImpl(final RuntimeWorkUnitExecution runtimeWorkUnitExecution, final Step step) {
        super(runtimeWorkUnitExecution, step);

        // Note: need to keep a local copy of BatchJmsDispatcher ref, just in case
        // it gets dynamically un-injected from ServicesManagerImpl while the job
        // is still running.
        setBatchJmsDispatcher(ServicesManagerStaticAnchor.getServicesManager().getBatchJmsDispatcher());
        this.isMultiJvm = isMultiJvm();
    }

    /**
     * Inject BatchJmsDispatcher ref.
     */
    private void setBatchJmsDispatcher(BatchDispatcher batchJmsDispatcher) {
        this.batchJmsDispatcher = batchJmsDispatcher;
    }

    /**
     * @return the BatchJmsDispatcher, or null if not enabled.
     */
    private BatchDispatcher getBatchJmsDispatcher() {
        return batchJmsDispatcher;
    }

    /**
     * TODO: which props to check? step? job? server? all of them?
     */
    private boolean isMultiJvm() {
        return getBatchJmsDispatcher() != null && isMultiJvmEnabled(runtimeWorkUnitExecution.getTopLevelJobProperties());
    }

    /**
     *
     * @return the value of prop "com.ibm.websphere.batch.partition.multiJVM", or true if the prop is not defined
     *         (by default, multi-jvm is enabled)
     */
    private boolean isMultiJvmEnabled(Properties properties) {
        return Boolean.parseBoolean(StringUtils.firstNonEmpty(properties.getProperty("com.ibm.websphere.batch.partition.multiJVM"), "true"));
    }

    /**
     * @return the SMF service
     */
    @Override
    protected ZosJBatchSMFLogging getJBatchSMFLoggingService() {
        return ServicesManagerStaticAnchor.getServicesManager().getJBatchSMFService();
    }

    @Override
    public ExecutionStatus execute() {
        return super.execute();
    }

    /**
     * The body of this method is synchronized with startPartition to close timing windows
     * so that a new partition doesn't get started after this method has gone thru
     * and stopped all currently running partitions.
     */
    @Override
    @FFDCIgnore(JobExecutionNotRunningException.class)
    public void stop() {

        StopLock stopLock = getStopLock(); // Store in local variable to facilitate Ctrl+Shift+G search in Eclipse
        synchronized (stopLock) {

            if (isStepStartingOrStarted()) {
                updateStepBatchStatus(BatchStatus.STOPPING);

                // It's possible we may try to stop a partitioned step before any
                // sub steps have been started.

                // Note: in multi-jvm mode parallelBatchWorkUnits will be empty (partitions may not be
                //       running locally, so no easy way to stop them).
                //       TODO: could queue stopPartition thru JMS i suppose...).
                for (BatchPartitionWorkUnit partition : parallelBatchWorkUnits) {
                    try {
                        getBatchKernelService().stopWorkUnit(partition);
                    } catch (JobExecutionNotRunningException e) {
                        logger.fine("Caught exception trying to stop work unit: " + partition + ", which was not running.");
                        // We want to stop all running sub steps.
                        // We do not want to throw an exception if a sub step has already been completed.
                    } catch (Exception e) {
                        // Blow up if it happens to force the issue.
                        throw new IllegalStateException(e);
                    }
                }
            } else {
                // Might not be set up yet to have a state.
                logger.fine("Ignoring stop, since step not in a state which has a valid status (might not be far enough along to have a state yet)");
            }
        }
    }

    /**
     * @return the partition plan, built either from the JSL or the user-provided PartitionMapper.
     */
    private PartitionPlanDescriptor buildPartitionPlan() {

        final PartitionMapper partitionMapperModel = getStep().getPartition().getMapper();
        final com.ibm.jbatch.jsl.model.PartitionPlan partitionPlanModel = getStep().getPartition().getPlan();

        if (partitionMapperModel != null) {
            return buildPartitionPlanFromMapper(partitionMapperModel);
        } else if (partitionPlanModel != null) {
            return buildPartitionPlanFromPlanElement(partitionPlanModel);
        } else {
            throw new IllegalArgumentException("Need plan or mapper but found neither.");
        }
    }

    /**
     * @return the partition plan, built using the user-provided Mapper.
     */
    private PartitionPlanDescriptor buildPartitionPlanFromMapper(final PartitionMapper partitionMapper) {

        PartitionMapperProxy partitionMapperProxy;

        final List<Property> propertyList = partitionMapper.getProperties() == null ? null : partitionMapper.getProperties().getPropertyList();

        // Set all the contexts associated with this controller.
        // Some of them may be null
        InjectionReferences injectionRef = new InjectionReferences(runtimeWorkUnitExecution.getWorkUnitJobContext(), runtimeStepExecution, propertyList);

        try {
            partitionMapperProxy = ProxyFactory.createPartitionMapperProxy(
                                                                           partitionMapper.getRef(), injectionRef, runtimeStepExecution);
        } catch (final ArtifactValidationException e) {
            throw new BatchContainerServiceException("Cannot create the PartitionMapper [" + partitionMapper.getRef() + "]", e);
        }

        PartitionPlan mapperPlan = partitionMapperProxy.mapPartitions();

        // We want to ignore override on the initial execution
        if (isRestartExecution()) {
            if (mapperPlan.getPartitionsOverride()) {
                setExecutionTypeIfNotSet(ExecutionType.RESTART_OVERRIDE);
            } else {
                setExecutionTypeIfNotSet(ExecutionType.RESTART_NORMAL);
            }
        } else {
            setExecutionTypeIfNotSet(ExecutionType.START);
        }

        //Set up the new partition plan
        PartitionPlanDescriptor retMe = new PartitionPlanDescriptor();
        retMe.setPartitionsOverride(mapperPlan.getPartitionsOverride());

        if (executionType == ExecutionType.RESTART_NORMAL) {
            retMe.setNumPartitionsInPlan(getTopLevelStepInstance().getPartitionPlanSize());
        } else {
            // All other cases including START behave the same
            retMe.setNumPartitionsInPlan(mapperPlan.getPartitions());
        }

        if (mapperPlan.getThreads() == 0) {
            retMe.setThreads(retMe.getNumPartitionsInPlan());
        } else {
            retMe.setThreads(mapperPlan.getThreads());
        }

        retMe.setPartitionProperties(mapperPlan.getPartitionProperties());

        return retMe;
    }

    /**
     * @return the PartitionPlanDescriptor, built from the data in the JSL.
     */
    private PartitionPlanDescriptor buildPartitionPlanFromPlanElement(com.ibm.jbatch.jsl.model.PartitionPlan partitionPlan) {

        String partitionsAttr = partitionPlan.getPartitions();
        String threadsAttr = null;

        int numPartitions = Integer.MIN_VALUE;
        int numThreads;
        Properties[] partitionProps = null;

        if (partitionsAttr != null) {
            try {
                numPartitions = Integer.parseInt(partitionsAttr);
            } catch (final NumberFormatException e) {
                throw new IllegalArgumentException("Could not parse partition instances value in stepId: " + getStepName()
                                                   + ", with instances=" + partitionsAttr, e);
            }
            partitionProps = new Properties[numPartitions];
            if (numPartitions < 0) {
                throw new IllegalArgumentException("Partition instances value must be 0 or greater in stepId: " + getStepName()
                                                   + ", with instances=" + partitionsAttr);
            }
        }

        threadsAttr = partitionPlan.getThreads();
        if (threadsAttr != null) {
            try {
                numThreads = Integer.parseInt(threadsAttr);
                if (numThreads == 0) {
                    numThreads = numPartitions;
                }
            } catch (final NumberFormatException e) {
                throw new IllegalArgumentException("Could not parse partition threads value in stepId: " + getStepName()
                                                   + ", with threads=" + threadsAttr, e);
            }
            if (numThreads < 0) {
                throw new IllegalArgumentException("Threads value must be 0 or greater in stepId: " + getStepName()
                                                   + ", with threads=" + threadsAttr);

            }
        } else { //default to number of partitions if threads isn't set
            numThreads = numPartitions;
        }

        if (partitionPlan.getProperties() != null) {

            List<JSLProperties> jslProperties = partitionPlan.getProperties();
            for (JSLProperties props : jslProperties) {
                Integer targetPartition = null;
                try {
                    targetPartition = Integer.parseInt(props.getPartition());
                } catch (NumberFormatException nfe) {
                    throw new IllegalArgumentException("Partition <properties> element should have an attributed  named 'partition' like <properties partition=\"2\">" +
                                                       " , but instead found <null> or non-Integer value of: " + props.getPartition());
                }
                try {
                    partitionProps[targetPartition] = CloneUtility.jslPropertiesToJavaProperties(props);
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new BatchContainerRuntimeException("There are only " + numPartitions + " partition instances, but there are "
                                                             + jslProperties.size()
                                                             + " partition properties lists defined. Remember that partition indexing is 0 based like Java arrays.", e);
                }
            }
        }

        // No override possibility when building from <plan> element
        if (isRestartExecution()) {
            setExecutionTypeIfNotSet(ExecutionType.RESTART_NORMAL);
        } else {
            setExecutionTypeIfNotSet(ExecutionType.START);
        }

        PartitionPlanDescriptor retMe = new PartitionPlanDescriptor();
        retMe.setNumPartitionsInPlan(numPartitions);
        retMe.setThreads(numThreads);
        retMe.setPartitionProperties(partitionProps);
        retMe.setPartitionsOverride(false); // No ability to set this when using JSL <plan> element

        return retMe;
    }

    // We could be more aggressive about validating illegal states and throwing exceptions here.
    private void setExecutionTypeIfNotSet(ExecutionType executionType) {
        if (this.executionType == null) {
            logger.finer("Setting initial execution type value");
            this.executionType = executionType;
        } else {
            logger.finer("Not setting execution type value since it's already set");
        }
    }

    @Override
    protected void markRestartAfterCompletion() {
        // If this immediate line fails (deleting partition-level step thread instance), then perhaps we're still in decent shape to try again
        // on a subsequent restart, since we still have the record on the top-level step thread instance.
        getPersistenceManagerService().deleteStepThreadInstanceOfRelatedPartitions(getTopLevelStepInstanceKey());
        executionType = ExecutionType.RESTART_AFTER_COMPLETION;
    }

    private boolean isRestartExecution() {
        return (getTopLevelStepInstance().getStartCount() > 1);
    }

    //@Override
    //public ExecutionStatus execute() {
    //    return super.execute();
    //}

    /**
     *
     */
    @Override
    protected void invokeCoreStep() {

        currentPlan = buildPartitionPlan();

        validatePlanNumberOfPartitions();

        if (executionType == ExecutionType.RESTART_OVERRIDE) {
            // Justification for doing this is in the spec Javadoc of PartitionPlan#setPartitionsOverride:
            //
            // "When true is specified, the partition count from the current run
            // is used and all results from past partitions are discarded. Any
            // resource cleanup or back out of work done in the previous run is the
            // responsibility of the application. The PartitionReducer artifact's
            // rollbackPartitionedStep method is invoked during restart before any
            // partitions begin processing to provide a cleanup hook."
            rollbackPartitionedStep();
        }

        // Not precisely spec-defined (see comment in method).  Since we've run the reducer's rollback, seems a good time to delete.
        //
        // Note we proactively clear out the partitions as soon as possible on the RESTART_AFTER_COMPLETED case, so no need to
        // clean up still, we've already done that.
        if (executionType == ExecutionType.RESTART_OVERRIDE) {
            cleanupPreviousPartitionData();
        }

        // We could avoid persisting if this is restart with override=false, but not sure that's best
        // Some aspects of this too, e.g. the exact ordering of this in relation to discarding the partition data,
        // are not precisely defined by the spec, and could be considered more carefully maybe if necessary.
        persistCurrentPlanSize();

        // Create the PartitionReplyQueue
        // The partitions pass back analyzer data, job status, and "partition complete" events on this queue.
        setPartitionReplyQueue(isMultiJvm ? getBatchJmsDispatcher().createPartitionReplyQueue() : new PartitionReplyQueueLocal());

        // kick off the threads
        executeAndWaitForCompletion();

        // Close the PartitionReplyQueue
        // In non multi-JVM mode, this does nothing.
        // In multi-JVM mode, it closes the JMS connection and reply-to queue.
        // Note: in the partition threads, the queue is closed in BatchPartitionWorkUnit.markThreadCompleted.
        getPartitionReplyQueue().close();

        // Deal with the results.
        checkFinishedPartitionsForFailures();

    }

    /**
     * Verify the number of partitions in the plan makes sense.
     *
     * @throws IllegalArgumentException if it doesn't make sense.
     */
    private void validatePlanNumberOfPartitions() {

        int numPreviousPartitions = getTopLevelStepInstance().getPartitionPlanSize();
        int numCurrentPartitions = currentPlan.getNumPartitionsInPlan();

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("For step: " + getStepName() + ", previous num partitions = " + numPreviousPartitions + ", and current num partitions = " + numCurrentPartitions);
        }

        if (executionType == ExecutionType.RESTART_NORMAL) {
            if (numPreviousPartitions == EntityConstants.PARTITION_PLAN_SIZE_UNINITIALIZED) {
                logger.fine("For step: " + getStepName() + ", previous num partitions has not been initialized, so don't validate the current plan size against it");
            } else if (numCurrentPartitions != numPreviousPartitions) {
                throw new IllegalArgumentException("Partition not configured for override, and previous execution used " + numPreviousPartitions +
                                                   " number of partitions, while current plan uses a different number: " + numCurrentPartitions);
            }
        }

        if (numCurrentPartitions < 0) {
            throw new IllegalArgumentException("Partition plan size is calculated as " + numCurrentPartitions + ", must be greater than or equal to 0.");
        }
    }

    /**
     * Write the plan size to the DB.
     */
    private void persistCurrentPlanSize() {
        // Don't want to have to think about whether the instance is dirty... just update from key.
        getPersistenceManagerService().updateStepThreadInstanceWithPartitionPlanSize(getTopLevelStepInstanceKey(),
                                                                                     currentPlan.getNumPartitionsInPlan());
    }

    /**
     * @return instance key for the top-level job
     */
    private TopLevelStepInstanceKey getTopLevelStepInstanceKey() {
        return (TopLevelStepInstanceKey) getStepThreadInstanceKey();
    }

    /**
     * @return the PartitionPlanConfig.
     */
    private PartitionPlanConfig buildPartitionPlanConfig(int partitionNum) {

        PartitionPlanConfig retMe = new PartitionPlanConfig(partitionNum, currentPlan.getPartitionProperties(partitionNum));
        retMe.setStepName(getStepName());
        retMe.setTopLevelNameInstanceExecutionInfo(runtimeWorkUnitExecution.getTopLevelNameInstanceExecutionInfo());
        retMe.setTopLevelStepExecutionId(runtimeStepExecution.getTopLevelStepExecutionId());
        retMe.setJobProperties(runtimeWorkUnitExecution.getWorkUnitJobContext().getProperties());
        retMe.setCorrelationId(runtimeWorkUnitExecution.getCorrelationId());
        retMe.setCreateTime(new Date());

        return retMe;
    }

    /**
     * @return the list of partition nums to execute. If this is a fresh start, it'll
     *         return all of them. If it's a restart, only the ones that haven't been executed.
     */
    private List<Integer> getPartitionNumbersToExecute() {
        // Note: The logic below works since 'currentPlan' had been validated for RESTART_NORMAL in
        // validatePlanNumberOfPartitions(PartitionPlanDescriptor). Otherwise there would need to be
        // a check to ensure the previous plans were the same size.

        List<Integer> partitionsToExecute = new ArrayList<Integer>();
        List<Integer> completedPartitions = new ArrayList<Integer>();

        // Build a list of all the partitions.
        for (int i = 0; i < currentPlan.getNumPartitionsInPlan(); i++) {
            partitionsToExecute.add(i);
        }
        // Defect 203551
        // If the execution type is a normal restart then remove all the completed partitions from the partitionsToExecute list.
        if (executionType == ExecutionType.RESTART_NORMAL) {
            completedPartitions = getPersistenceManagerService().getStepThreadInstancePartitionNumbersOfRelatedCompletedPartitions(getTopLevelStepInstanceKey());
            if (!completedPartitions.isEmpty()) {
                partitionsToExecute.removeAll(completedPartitions);
            }
        }

        return partitionsToExecute;
    }

    /**
     * Today we know the PartitionedStepControllerImpl always runs in the JVM of the top-level job, and so
     * will be notified on the top-level stop.
     *
     * Doing xJVM split-flow would be more complicated, since not only do we lose the single JVM for
     * the top-level job, the split and the child flows........we also can't count on the top-level
     * portion of the partition, the PartitionedStepControllerImpl, being in the same JVM as the
     * top-level job.
     *
     * If we add xJVM split-flow.. not only do we have to worry about the flows themselves...we also have to recode
     * some assumptions for partitions... since the partitioned step might be itself part of a split-flow.
     *
     * Note we also might have to worry about ensuring the STEP-level status was moved to STOPPING if we
     * were only checking the job level status remotely (see defect 203063).
     *
     * We could just check the DB.. but I guess we don't have to for now.
     *
     * @return true if the job is stopping, stopped, or failed.
     */
    private boolean isStoppingStoppedOrFailed() {
        BatchStatus jobBatchStatus = runtimeWorkUnitExecution.getBatchStatus();

        if (jobBatchStatus.equals(BatchStatus.STOPPING) || jobBatchStatus.equals(BatchStatus.STOPPED) || jobBatchStatus.equals(BatchStatus.FAILED)) {
            return true;
        }
        return false;
    }

    /**
     * Keep starting partitions until either..
     * (a) they've all been started or
     * (b) we reach max threads
     *
     * @throws JobStoppingException
     */
    private void startPartitions() throws JobStoppingException {

        logger.fine("startedPartitions = " + startedPartitions + ", numThreads = " + currentPlan.getThreads());

        while (startedPartitions.size() < partitionsToExecute.size()
               && startedPartitions.size() - finishedPartitions.size() < currentPlan.getThreads()) {

            int nextPartitionNumber = partitionsToExecute.get(startedPartitions.size());
            logger.finer("nextPartitionNumber = " + nextPartitionNumber);
            PartitionPlanConfig config = buildPartitionPlanConfig(nextPartitionNumber);

            StopLock stopLock = getStopLock(); // Store in local variable to facilitate Ctrl+Shift+G search in Eclipse
            synchronized (stopLock) {
                startPartition(config);
            }

            startedPartitions.add(nextPartitionNumber);
        }
    }

    /**
     * Start a new remote partition via JMS (multi-JVM mode).
     */
    private void startPartitionRemote(PartitionPlanConfig config) {

        // TODO: should we add another PartitionReplyMsg type, PARTITION_STARTED or something,
        //       so that the top-level thread at least knows that the startPartition JMS msg
        //       was picked up by an endpoint?
        getBatchJmsDispatcher().startPartition(config,
                                               getStep(),
                                               getPartitionReplyQueue());
    }

    /**
     * Start a new partition locally (same JVM).
     */
    private void startPartitionLocal(PartitionPlanConfig config) {

        //This partition was dispatched locally. Hence, param isRemoteDispatch should be false
        Entry<BatchPartitionWorkUnit, Future<?>> workUnitFutureEntry = getBatchKernelService().startPartition(config, getStep(), getPartitionReplyQueue(), false);

        parallelBatchWorkUnits.add(workUnitFutureEntry.getKey());
    }

    /**
     * Start a new partition.
     * This method is synchronized with stop().
     *
     * @return true if the partition was started; false otherwise (because we're stopping)
     * @throws JobStoppingException
     */
    private boolean startPartition(PartitionPlanConfig config) throws JobStoppingException {

        if (isStoppingStoppedOrFailed()) {
            throw new JobStoppingException();
        }

        if (isMultiJvm) {
            startPartitionRemote(config);
        } else {
            startPartitionLocal(config);
        }

        // TODO: should this message be issued from the partition?  (won't go to top-level log if so?)
        //       should we have a separate message issued from here, "partition.submitted" ?
        JoblogUtil.logToJobLogAndTraceOnly(Level.FINER, "partition.started", new Object[] { config.getPartitionNumber(),
                                                                                            getStepName(),
                                                                                            config.getTopLevelInstanceId(),
                                                                                            config.getTopLevelExecutionId() },
                                           logger);
        return true;
    }

    /**
     * Issue message for partition finished and add it to the finishedPartitions list.
     */
    private void partitionFinishedReplyMessageReceived(PartitionReplyMsg msg) {
        JoblogUtil.logToJobLogAndTraceOnly(Level.FINE, "partition.ended", new Object[] {
                                                                                         msg.getPartitionPlanConfig().getPartitionNumber(),
                                                                                         msg.getBatchStatus(),
                                                                                         msg.getExitStatus(),
                                                                                         msg.getPartitionPlanConfig().getStepName(),
                                                                                         msg.getPartitionPlanConfig().getTopLevelNameInstanceExecutionInfo().getInstanceId(),
                                                                                         msg.getPartitionPlanConfig().getTopLevelNameInstanceExecutionInfo().getExecutionId() },
                                           logger);

        addToFinishedPartitions(msg.getPartitionPlanConfig().getPartitionNumber(), msg.getBatchStatus());
    }

    private void addToFinishedPartitions(Integer partitionNumber, BatchStatus batchStatus) {
        finishedPartitions.add(new FinishedPartition(partitionNumber, batchStatus));
        finishedPartitionNumbers.add(partitionNumber);
    }

    /**
     * Call the analyzerProxy (if one was supplied) with the given partition data.
     */
    private void analyzePartitionReplyMsg(PartitionReplyMsg msg) {

        if (analyzerProxy == null) {
            return;
        }

        switch (msg.getMsgType()) {
            case PARTITION_COLLECTOR_DATA:
                Serializable collectorData = deserializeCollectorData(msg.getCollectorData());
                logger.finer("Analyze collector data: " + collectorData);
                analyzerProxy.analyzeCollectorData(collectorData);
                break;

            case PARTITION_FINAL_STATUS:
                logger.fine("Calling analyzeStatus(): " + msg);
                analyzerProxy.analyzeStatus(msg.getBatchStatus(), msg.getExitStatus());
                break;
        }
    }

    private Serializable deserializeCollectorData(byte[] bytes) {
        Serializable retVal = null;

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            DeserializationObjectInputStream ois = null;
            try {
                ois = new DeserializationObjectInputStream(bais, Thread.currentThread().getContextClassLoader());
                retVal = (Serializable) ois.readObject();
            } finally {
                ois.close();
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Problem while trying to deserialize the collector data");
        } catch (IOException e) {
            throw new IllegalStateException("Problem while trying to deserialize the collector data");
        }
        return retVal;
    }

    /**
     * Wait for partitions to send data back to the top-level thread.
     *
     * @return data sent back from the partition to the top-level thread
     */
    private PartitionReplyMsg waitForPartitionReplyMsg() {
        try {
            return getPartitionReplyQueue().take();
        } catch (InterruptedException ie) {
            throw new BatchContainerRuntimeException(ie);
        }
    }

    /**
     * Get the reply data sent back by the partition
     *
     * @return data sent back from the partition to the top-level thread or return null
     */
    private PartitionReplyMsg getPartitionReplyMsgNoWait() {
        return getPartitionReplyQueue().takeWithoutWaiting();
    }

    /**
     * Wait and process the data sent back by the partitions.
     * This method returns as soon as the next partition sends back a finished message.
     *
     */
    private void waitForNextPartitionToFinish() throws JobStoppingException {

        //Use this counter to count the number of cycles we recieve jms reply message
        boolean isStoppingStoppedOrFailed = false;
        PartitionReplyMsg msg = null;

        do {

            if (isMultiJvm) {
                if (isStoppingStoppedOrFailed()) {
                    isStoppingStoppedOrFailed = true;
                }
            }

            //TODO - We won't worry about the case when a local dispatch has been stopped until
            // we're prepared to code up the structure necessary to break free from waiting on
            // the BlockingQueue

            if (isStoppingStoppedOrFailed) {

                //TODO - Crude, and the JVM doesn't have to follow this too closely.
                // The idea is to give the partitions some time complete so we can report with
                // a more tidy final status.
                // Another idea would be to start a timer or do the first wait with a receive.
                //
                try {
                    Thread.sleep(PartitionReplyTimeoutConstants.BATCH_REPLY_MSG_SLEEP_AFTER_STOP);
                } catch (InterruptedException e) {
                    // do nothing
                }

                // Process any reply messages queued up, but in order to not delay stop too long we will
                // exit the first time we look at the queue and see there is no message.
                while (true) {
                    msg = getPartitionReplyMsgNoWait();
                    if (msg != null) {
                        try {
                            analyzePartitionReplyMsg(msg);
                        } catch (Throwable t) {
                            // FFDC.
                            // Remember the exception for rollback later.
                            analyzerExceptions.add(t);
                        }
                        if (isFinalMessageForPartition(msg)) {
                            partitionFinishedReplyMessageReceived(msg);
                        }
                    } else {
                        //If no messages left, break the loop
                        throw new JobStoppingException();
                    }
                }
            } else {
                //This is executed when stop has not been issued
                msg = waitForPartitionReplyMsg();
                if (msg == null) {

                    // Since we're not aggressively failing the top-level step when an individual partition fails, there's no
                    // need to be too proactive in looking for recovery failures.   Let's wait until we fail to get any reply messages
                    // back and save the trips to the DB.   Even if we have a bunch of collector messages piled up, it's not as if we
                    // have any waits each time we iterate through this loop, so we should clear them soon, I'd think.
                    if (isMultiJvm) {
                        // The caller has the loop that determines when all the partitions are done.
                        // Our job is just to exit this method when any partition has finished.  In the
                        // case of recovery it could actually be more than one that has finished at once.
                        // But we're not returning back anything to the caller of this method;  it's OK if
                        // more than one partition has finished.
                        if (checkForRecoveredRemotePartitions()) {
                            break;
                        }
                    }

                    //If waitForPartitionReplyMsg times out after 15 seconds, go back to the top
                    continue;
                }
            }

            try {
                analyzePartitionReplyMsg(msg);
            } catch (Throwable t) {
                // FFDC.
                // Remember the exception for rollback later.
                analyzerExceptions.add(t);
            }

            if (isFinalMessageForPartition(msg)) {
                partitionFinishedReplyMessageReceived(msg);
                break;
            }
            // else keep looping
        } while (true);
    }

    /**
     *
     * @return true if we recovered a partition, false otherwise
     */
    private boolean checkForRecoveredRemotePartitions() {

        boolean retVal = false;

        // We shouldn't be seeing recovered remote partitions too often, so we won't try to optimize either the query itself or the process of matching the results against the list of recovered partitions
        // that we've already processed.
        long stepExecId = runtimeStepExecution.getTopLevelStepExecutionId();
        List<Integer> recoveredPartitions = getPersistenceManagerService().getRemotablePartitionsRecoveredForStepExecution(stepExecId);
        if (recoveredPartitions.size() > 0) {
            logger.finer("Found list: " + recoveredPartitions + " of new recovered remote partitions for stepExecId = " + stepExecId);
        } else {
            logger.finer("Did not find any recovered partitions for stepExecId = " + stepExecId);
        }
        for (Integer recoveredPartitionNum : recoveredPartitions) {
            if (finishedPartitionNumbers.contains(recoveredPartitionNum)) {
                logger.finer("Recovered remote partition # " + recoveredPartitionNum + " is already marked as finished.");
                continue;
            } else {
                retVal = true;
                logger.finer("Found new recovered remote partition # " + recoveredPartitionNum);
                handleRecoveredRemotePartition(recoveredPartitionNum);
            }
        }

        return retVal;
    }

    /**
     * @param recoveredPartitionNum
     */
    private void handleRecoveredRemotePartition(int recoveredPartitionNum) {

        JoblogUtil.logToJobLogAndTraceOnly(Level.FINE, "partition.ended", new Object[] {
                                                                                         recoveredPartitionNum,
                                                                                         BatchStatus.FAILED,
                                                                                         "FAILED",
                                                                                         getStepName(),
                                                                                         runtimeWorkUnitExecution.getTopLevelNameInstanceExecutionInfo().getInstanceId(),
                                                                                         runtimeWorkUnitExecution.getTopLevelNameInstanceExecutionInfo().getExecutionId() },
                                           logger);

        addToFinishedPartitions(recoveredPartitionNum, BatchStatus.FAILED);
    }

    /*
     * Check if it's the last message received for a partition
     *
     * We are still checking for THREAD_COMPLETE to maintain compatibility with 8.5.5.7,
     * which sends FINAL_STATUS without a partitionNumber ,and THREAD_COMPLETE with a partitionNumber,
     * although, now the executor does not send a THREAD_COMPLETE message, just a FINAL_STATUS message with a partitionNumber
     *
     * @returns true if no more messages for the partition is expected to be received
     *
     * @returns false if more messages are to be received for the partition
     */
    private boolean isFinalMessageForPartition(PartitionReplyMsg msg) {

        switch (msg.getMsgType()) {
            case PARTITION_FINAL_STATUS:
                if (msg.getPartitionPlanConfig() != null) {
                    return true;
                } else {
                    return false;
                }
            case PARTITION_THREAD_COMPLETE:
                return true;
            default:
                return false;
        }
    }

    /**
     * Spawn the partitions and wait for them to complete.
     */
    @FFDCIgnore(JobStoppingException.class)
    private void executeAndWaitForCompletion() throws JobRestartException {
        if (isStoppingStoppedOrFailed()) {
            logger.fine("Job already in "
                        + runtimeWorkUnitExecution.getWorkUnitJobContext().getBatchStatus().toString()
                        + " state, exiting from executeAndWaitForCompletion() before beginning execution");
            return;
        }

        partitionsToExecute = getPartitionNumbersToExecute();

        logger.fine("Partitions to execute in this run: " + partitionsToExecute
                    + ".  Total number of partitions in step: " + currentPlan.getNumPartitionsInPlan());

        // Keep looping until all partitions have finished.
        while (finishedPartitions.size() < partitionsToExecute.size()) {

            logger.fine("Iterate through loop with finishedPartitions = " + finishedPartitions);

            try {
                startPartitions();
            } catch (JobStoppingException e) {
                break;
            }

            // Check that there are still un-finished partitions running.
            // If not, break out of the loop.
            if (finishedPartitions.size() >= partitionsToExecute.size()) {
                break;
            }

            try {
                waitForNextPartitionToFinish();
            } catch (JobStoppingException e) {
                break;
            }
        }

        if (!analyzerExceptions.isEmpty()) {
            rollbackPartitionedStep();
            throw new BatchContainerRuntimeException("Exception previously thrown by Analyzer, rolling back step.", analyzerExceptions.get(0));
        }
    }

    /**
     * check the batch status of each partition after it's done to see if we need to issue a rollback
     * start rollback if any have stopped or failed
     */
    private void checkFinishedPartitionsForFailures() {

        List<String> failingPartitionSeen = new ArrayList<String>();
        boolean stoppedPartitionSeen = false;

        for (FinishedPartition p : finishedPartitions) {

            int partitionNumber = p.partitionNum;
            BatchStatus batchStatus = p.batchStatus;

            if (logger.isLoggable(Level.FINE)) {
                logger.fine("For partitioned step: " + getStepName() + ", the partition # " +
                            partitionNumber + " ended with status '" + batchStatus);
            }

            // Keep looping, just to see the log messages perhaps.
            if (batchStatus.equals(BatchStatus.FAILED)) {

                String msg = "For partitioned step: " + getStepName() + ", the partition # " +
                             partitionNumber + " ended with status '" + batchStatus;
                failingPartitionSeen.add(msg);
            }

            // This code seems to suggest it might be valid for a partition to end up in STOPPED state without
            // the "top-level" step having been aware of this.   It's unclear from the spec if this is even possible
            // or a desirable spec interpretation.  Nevertheless, we'll code it as such noting the ambiguity.
            //
            // However, in the RI at least, we won't bother updating the step level BatchStatus, since to date we
            // would only transition the status in such a way independently.
            if (batchStatus.equals(BatchStatus.STOPPED)) {
                stoppedPartitionSeen = true;
            }
        }

        if (!failingPartitionSeen.isEmpty()) {
            markStepForFailure();
            rollbackPartitionedStep();
            throw new BatchContainerRuntimeException("One or more partitions failed: " + failingPartitionSeen);
        } else if (isStoppingStoppedOrFailed()) {
            rollbackPartitionedStep();
        } else if (stoppedPartitionSeen) {

            // At this point, the top-level job is still running.
            // If we see a stopped partition, we mark the step/job failed
            markStepForFailure();
            rollbackPartitionedStep();
        } else {
            // Call before completion
            if (this.partitionReducerProxy != null) {
                this.partitionReducerProxy.beforePartitionedStepCompletion();
            }
        }

    }

    /**
     * If rollback is false we never issued a rollback so we can issue a logicalTXSynchronizationBeforeCompletion
     * NOTE: this will get issued even in a partition fails or stops if no logicalTXSynchronizationRollback method is provied
     * We are assuming that not providing a rollback was intentional
     *
     */
    private void rollbackPartitionedStep() {

        rollbackPartitionedStepInvoked = true;

        if (this.partitionReducerProxy != null) {
            this.partitionReducerProxy.rollbackPartitionedStep();
        }
    }

    /**
     * @return the stepThreadInstance
     */
    private TopLevelStepInstanceEntity getTopLevelStepInstance() {
        return (TopLevelStepInstanceEntity) getStepThreadInstance();
    }

    @Override
    protected void setupStepArtifacts() {

        InjectionReferences injectionRef = null;
        injectionRef = new InjectionReferences(runtimeWorkUnitExecution.getWorkUnitJobContext(), runtimeStepExecution, null);
        this.stepListeners = runtimeWorkUnitExecution.getListenerFactory().getStepListeners(getStep(), injectionRef, runtimeStepExecution);

        Analyzer analyzer = getStep().getPartition().getAnalyzer();

        if (analyzer != null) {
            final List<Property> propList = analyzer.getProperties() == null ? null : analyzer.getProperties().getPropertyList();

            injectionRef = new InjectionReferences(runtimeWorkUnitExecution.getWorkUnitJobContext(), runtimeStepExecution, propList);

            try {
                analyzerProxy = ProxyFactory.createPartitionAnalyzerProxy(analyzer.getRef(), injectionRef, runtimeStepExecution);
            } catch (final ArtifactValidationException e) {
                throw new BatchContainerServiceException("Cannot create the analyzer [" + analyzer.getRef() + "]", e);
            }

        }

        PartitionReducer partitionReducer = getStep().getPartition().getReducer();

        if (partitionReducer != null) {

            final List<Property> propList = partitionReducer.getProperties() == null ? null : partitionReducer.getProperties().getPropertyList();

            injectionRef = new InjectionReferences(runtimeWorkUnitExecution.getWorkUnitJobContext(), runtimeStepExecution, propList);

            try {
                this.partitionReducerProxy = ProxyFactory.createPartitionReducerProxy(partitionReducer.getRef(), injectionRef, runtimeStepExecution);
            } catch (final ArtifactValidationException e) {
                throw new BatchContainerServiceException("Cannot create the reducer [" + partitionReducer.getRef() + "]", e);
            }
        }
    }

    /**
     * Invoke the StepListeners and PartitionReducer.
     */
    @Override
    protected void invokePreStepArtifacts() {

        if (stepListeners != null) {
            for (StepListenerProxy listenerProxy : stepListeners) {
                // Call beforeStep on all the step listeners
                listenerProxy.beforeStep();
            }
        }

        // Invoke the reducer before all parallel steps start (must occur
        // before mapper as well)
        if (this.partitionReducerProxy != null) {
            this.partitionReducerProxy.beginPartitionedStep();
        }
    }

    /**
     * Invoke the StepListeners and PartitionReducer.
     */
    @Override
    protected void invokePostStepArtifacts() {
        // Invoke the reducer after all parallel steps are done
        if (this.partitionReducerProxy != null) {
            if (rollbackPartitionedStepInvoked) {
                this.partitionReducerProxy.afterPartitionedStepCompletion(PartitionStatus.ROLLBACK);
            } else {
                this.partitionReducerProxy.afterPartitionedStepCompletion(PartitionStatus.COMMIT);
            }
        }

        // Called in spec'd order, e.g. Sec. 11.7
        if (stepListeners != null) {
            for (StepListenerProxy listenerProxy : stepListeners) {
                // Call afterStep on all the step listeners
                listenerProxy.afterStep();
            }
        }
    }

    @Override
    protected void updateStepExecution() {
        // Call special aggregating method
        TopLevelStepExecutionEntity topLevelStepExecutionEntity = getPersistenceManagerService().updateStepExecutionWithPartitionAggregate(runtimeStepExecution);
        topLevelStepExecutionEntity.getJobExecution().setLastUpdatedTime(runtimeStepExecution.getLastUpdatedTime());
    }

    @Override
    protected boolean isTopLevelStepThreadOfPartitionedStep() {
        return true;
    }

    /**
     *
     * Not spec-defined when exactly these checkpoints are no longer available.
     *
     * This would only matter if we wanted to support some scenario where after an intial failure, we failed right after the mapper said to override
     * but before we started executing the new partitions, and then we wanted to do a subsequent restart and possibly this time set override to 'false'.
     *
     * If you think this should allow picking up from the persisted partition (checkpoint & user data) info from the first execution, then we should leave
     * around this info as long as possible.
     */
    private void cleanupPreviousPartitionData() {
        getPersistenceManagerService().deleteStepThreadInstanceOfRelatedPartitions(getTopLevelStepInstanceKey());
    }

    /**
     * Describes plan attrs such as num partitions, num threads, partition properties.
     *
     * Let's make this NOT implement PartitionPlan to remove any confusion
     * and show that this instance is clearly NOT the user-supplied Plan
     */
    private static class PartitionPlanDescriptor {

        private int numPartitionsInPlan;
        private int threadCount;
        private Properties[] partitionProperties;
        private boolean partitionsOverride;

        public int getNumPartitionsInPlan() {
            return numPartitionsInPlan;
        }

        public void setNumPartitionsInPlan(int numPartitionsInPlan) {
            this.numPartitionsInPlan = numPartitionsInPlan;
        }

        public int getThreads() {
            return threadCount;
        }

        public void setThreads(int threadCount) {
            this.threadCount = threadCount;
        }

        public Properties getPartitionProperties(int partitionNum) {
            return (partitionProperties != null && partitionProperties.length > partitionNum) ? partitionProperties[partitionNum] : null;
        }

        public void setPartitionProperties(Properties[] partitionProperties) {
            this.partitionProperties = partitionProperties;
        }

        public void setPartitionsOverride(boolean override) {
            this.partitionsOverride = override;
        }

        public boolean getPartitionsOverride() {
            return this.partitionsOverride;
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            buf.append("partitionCount = " + numPartitionsInPlan);
            buf.append(", threadCount = " + threadCount);
            buf.append(", partitionsOverride = " + partitionsOverride);
            buf.append(", partitionProperties = " + partitionProperties);
            return buf.toString();
        }
    }

    // Use as a tool for flow of control.  Private to this class.
    private class JobStoppingException extends Exception {
        private static final long serialVersionUID = 1L;

        public JobStoppingException() {
            super();
        }
    }

}
