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
package com.ibm.jbatch.container.ws.impl;

import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.StepExecution;

import com.ibm.jbatch.container.RASConstants;
import com.ibm.jbatch.container.persistence.jpa.JobExecutionEntity;
import com.ibm.jbatch.container.persistence.jpa.RemotablePartitionEntity;
import com.ibm.jbatch.container.persistence.jpa.RemotablePartitionKey;
import com.ibm.jbatch.container.persistence.jpa.StepThreadExecutionEntity;
import com.ibm.jbatch.container.services.impl.JPAPersistenceManagerImpl;
import com.ibm.wsspi.persistence.PersistenceServiceUnit;

/**
 * Perform recovery of jobs that were running when a previous instance of this server
 * shutdown abruptly and left them in an "in-flight" state in the DB.
 *
 */
public class WSStartupRecoveryServiceImpl {

    private final static Logger logger = Logger.getLogger(WSStartupRecoveryServiceImpl.class.getName(),
                                                          RASConstants.BATCH_MSG_BUNDLE);

    /**
     * For reading/updating the DB.
     */
    private JPAPersistenceManagerImpl persistenceManagerService;

    /**
     * WSStartupRecoveryServiceImpl runs just after the PSU is created but *before* the
     * PSU is set into the psuFuture in JPAPersistenceManagerImpl. This is so the recovery
     * service can run before anyone else tries to use the persistence service (everyone
     * else gets the PSU from the psuFuture). Note that all JPAPersistenceManagerImpl
     * methods called by this guy take the PSU as an arg so that they don't have to
     * get the PSU from the psuFuture themselves.
     */
    private PersistenceServiceUnit psu;

    /**
     * inject
     *
     * @return this
     */
    public WSStartupRecoveryServiceImpl setIPersistenceManagerService(JPAPersistenceManagerImpl pms) {
        this.persistenceManagerService = pms;
        return this;
    }

    /**
     * injection
     *
     * @return this
     */
    public WSStartupRecoveryServiceImpl setPersistenceServiceUnit(PersistenceServiceUnit psu) {
        this.psu = psu;
        return this;
    }

    /**
     * Look in the db for any partitions that this server executed with batch status of STARTING, STARTED, STOPPING
     * If we are starting up, and we already have jobs in on of the state above,
     * meaning those jobs did not finish when something happened to the server (jvm down, hang, etc)
     * Reset all those jobs to have batch status = exit status = FAILED
     */
    public WSStartupRecoveryServiceImpl recoverLocalPartitionsInInflightStates() {
        String methodName = "recoverLocalPartitionsInInflightStates";

        try {
            List<RemotablePartitionEntity> remotablePartitions = persistenceManagerService.getRemotablePartitionsRunningLocalToServer(psu);

            for (RemotablePartitionEntity partition : remotablePartitions) {

                StepThreadExecutionEntity stepExecution = partition.getStepExecution();
                RemotablePartitionKey key = new RemotablePartitionKey(stepExecution);
                String newExitStatus = stepExecution.getExitStatus();
                if (newExitStatus == null) {
                    newExitStatus = BatchStatus.FAILED.name();
                }

                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(methodName + ": marking [partition = " + key + ",old batchStatus=" + stepExecution.getBatchStatus().name()
                                + " to new batchStatus=FAILED, new exitStatus=" + newExitStatus + "]");
                }

                // The world is a more orderly place with these two timestamp values showing the same!
                Date markFailedTime = new Date();
                try {
                    persistenceManagerService.updateStepExecutionOnRecovery(psu, stepExecution.getStepExecutionId(), BatchStatus.FAILED, newExitStatus, markFailedTime);
                    persistenceManagerService.updateRemotablePartitionOnRecovery(psu, partition);
                } catch (Exception updateExc) {
                    logger.log(Level.WARNING, "partition.recovery.failed", new Object[] { key, updateExc });
                }
            }
        } catch (Exception exception) {
            logger.log(Level.WARNING, "recovery.failed", exception);
        }

        return this;
    }

    /**
     * Look in the db for any jobs that this server executed with batch status of STARTING, STARTED, STOPPING
     * If we are starting up, and we already have jobs in on of the state above,
     * meaning those jobs did not finish when something happened to the server (jvm down, hang, etc)
     * Reset all those jobs to have batch status = exit status = FAILED
     */
    public WSStartupRecoveryServiceImpl recoverLocalJobsInInflightStates() {
        String methodName = "recoverLocalJobsInInflightStates";

        try {
            List<JobExecutionEntity> executionInstances = persistenceManagerService.getJobExecutionsRunningLocalToServer(psu);

            //for each execution instance, update the batchstatus = exitstatus = FAILED, and timestamp of last update
            //TODO - This could be optimized if we do group update.
            for (JobExecutionEntity executionInstance : executionInstances) {
                long executionId = executionInstance.getExecutionId();
                long jobInstanceId = executionInstance.getInstanceId();

                String newExitStatus = executionInstance.getExitStatus();
                if (newExitStatus == null) {
                    newExitStatus = BatchStatus.FAILED.name();
                }

                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(methodName + ": marking [job instance=" + jobInstanceId + ",old batchStatus=" + executionInstance.getBatchStatus().name()
                                + " to new batchStatus=FAILED, new exitStatus=" + newExitStatus + "]");
                }

                // The world is a more orderly place with these two timestamp values showing the same!
                Date markFailedTime = new Date();
                try {
                    persistenceManagerService.updateJobExecutionAndInstanceFinalStatus(psu, executionId, BatchStatus.FAILED, newExitStatus, markFailedTime);
                } catch (Exception updateExc) {
                    logger.log(Level.WARNING, "job.recovery.failed", new Object[] { jobInstanceId, updateExc });
                }

                // Perhaps we could do some consistency checking validation between the job-level markFailed and the step-level markFailed... but we don't bother for now.
                for (StepExecution stepExecution : persistenceManagerService.getStepThreadExecutionsRunning(psu, executionId)) {
                    String newStepExitStatus = stepExecution.getExitStatus();
                    if (newStepExitStatus == null) {
                        newStepExitStatus = BatchStatus.FAILED.name();
                    }
                    logger.fine(methodName + ": marking [step execution=" + stepExecution.getStepExecutionId() + ",old batchStatus=" + stepExecution.getBatchStatus().name()
                                + " to new batchStatus=FAILED, new step exitStatus=" + newStepExitStatus + "]");
                    persistenceManagerService.updateStepExecutionOnRecovery(psu, stepExecution.getStepExecutionId(), BatchStatus.FAILED, newStepExitStatus, markFailedTime);
                }
            }
        } catch (Exception exception) {
            logger.log(Level.WARNING, "recovery.failed", exception);
        }

        return this;
    }

}
