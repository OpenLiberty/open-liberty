/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jbatch.container.ws.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.operations.JobSecurityException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.operations.NoSuchJobInstanceException;
import javax.batch.runtime.BatchStatus;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.jbatch.container.RASConstants;
import com.ibm.jbatch.container.exception.BatchIllegalJobStatusTransitionException;
import com.ibm.jbatch.container.exception.ExecutionAssignedToServerException;
import com.ibm.jbatch.container.persistence.jpa.JobInstanceEntity;
import com.ibm.jbatch.container.persistence.jpa.RemotablePartitionKey;
import com.ibm.jbatch.container.services.IJPAQueryHelper;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.ws.InstanceState;
import com.ibm.jbatch.container.ws.JobInstanceNotQueuedException;
import com.ibm.jbatch.container.ws.WSBatchAuthService;
import com.ibm.jbatch.container.ws.WSJobExecution;
import com.ibm.jbatch.container.ws.WSJobInstance;
import com.ibm.jbatch.container.ws.WSJobRepository;
import com.ibm.jbatch.container.ws.WSRemotablePartitionExecution;
import com.ibm.jbatch.container.ws.WSRemotablePartitionState;
import com.ibm.jbatch.container.ws.WSStepThreadExecutionAggregate;
import com.ibm.jbatch.spi.BatchSecurityHelper;

/**
 * {@inheritDoc}
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" })
public class WSJobRepositoryImpl implements WSJobRepository {

    private final static String CLASSNAME = WSJobRepositoryImpl.class.getName();
    private final static Logger logger = Logger.getLogger(CLASSNAME, RASConstants.BATCH_MSG_BUNDLE);

    private IPersistenceManagerService persistenceManagerService;

    private WSBatchAuthService authService;

    private BatchSecurityHelper batchSecurityHelper = null;

    @Reference(policyOption = ReferencePolicyOption.GREEDY)
    protected void setIPersistenceManagerService(IPersistenceManagerService pms) {
        this.persistenceManagerService = pms;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setWSBatchAuthService(WSBatchAuthService bas) {
        this.authService = bas;
    }

    protected void unsetWSBatchAuthService(WSBatchAuthService bas) {
        if (this.authService == bas) {
            this.authService = null;
        }
    }

    protected void unsetIPersistenceManagerService(IPersistenceManagerService ref) {
        if (this.persistenceManagerService == ref) {
            this.persistenceManagerService = null;
        }
    }

    @Reference
    protected void setBatchSecurityHelper(BatchSecurityHelper batchSecurityHelper) {
        this.batchSecurityHelper = batchSecurityHelper;
    }

    /**
     * DS un-setter.
     */
    protected void unsetBatchSecurityHelper(BatchSecurityHelper batchSecurityHelper) {
        if (this.batchSecurityHelper == batchSecurityHelper) {
            this.batchSecurityHelper = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WSJobInstance getJobInstanceFromExecution(long executionId) throws NoSuchJobExecutionException, JobSecurityException {
        long instanceId = persistenceManagerService.getJobInstanceIdFromExecutionId(authorizedExecutionRead(executionId));
        return persistenceManagerService.getJobInstance(instanceId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WSJobExecution getJobExecution(long executionId) throws NoSuchJobExecutionException, JobSecurityException {
        return persistenceManagerService.getJobExecution(authorizedExecutionRead(executionId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WSJobExecution createJobExecution(long jobInstanceId, Properties jobParameters) {
        return persistenceManagerService.createJobExecution(jobInstanceId, jobParameters, new Date());
    }

    /**
     * {@inheritDoc}
     */
    public List<WSStepThreadExecutionAggregate> getStepExecutionsFromJobExecution(long jobExecutionId) throws NoSuchJobExecutionException, JobSecurityException {
        return persistenceManagerService.getStepExecutionAggregatesFromJobExecutionId(authorizedExecutionRead(jobExecutionId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<WSJobInstance> getJobInstances(IJPAQueryHelper queryHelper, int page, int pageSize) throws NoSuchJobExecutionException, JobSecurityException {

        if (authService == null || authService.isAdmin() || authService.isMonitor()) {
            return new ArrayList<WSJobInstance>(persistenceManagerService.getJobInstances(queryHelper, page, pageSize));
        } else if (authService.isGroupAdmin() || authService.isGroupMonitor()) {
            queryHelper.setGroups(authService.getGroupsForSubject());
            queryHelper.setQueryIssuer(authService.getRunAsUser());
            return new ArrayList<WSJobInstance>(persistenceManagerService.getJobInstances(queryHelper, page, pageSize));
        } else if (authService.isSubmitter()) {
            queryHelper.setQueryIssuer(authService.getRunAsUser());
            return new ArrayList<WSJobInstance>(persistenceManagerService.getJobInstances(queryHelper, page, pageSize));
        }

        throw new JobSecurityException("The current user " + batchSecurityHelper.getRunAsUser() + " is not authorized to perform any batch operations.");

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WSJobInstance getJobInstance(long instanceId) throws NoSuchJobExecutionException, JobSecurityException {

        return persistenceManagerService.getJobInstance(authorizedInstanceRead(instanceId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<WSJobExecution> getJobExecutionsFromInstance(long instanceId) throws NoSuchJobInstanceException, JobSecurityException {
        return new ArrayList<WSJobExecution>(persistenceManagerService.getJobExecutionsFromJobInstanceId(authorizedInstanceRead(instanceId)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBatchAppNameFromExecution(long executionId) throws NoSuchJobInstanceException, JobSecurityException {
        return persistenceManagerService.getJobInstanceAppNameFromExecutionId(authorizedExecutionRead(executionId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBatchAppNameFromInstance(long instanceId) throws NoSuchJobInstanceException, JobSecurityException {
        return persistenceManagerService.getJobInstanceAppName(authorizedInstanceRead(instanceId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WSJobExecution getMostRecentJobExecutionFromInstance(long instanceId) throws NoSuchJobInstanceException, JobSecurityException {
        return persistenceManagerService.getJobExecutionMostRecent(authorizedInstanceRead(instanceId));
    }

    private long authorizedInstanceRead(long instanceId) throws NoSuchJobInstanceException, JobSecurityException {
        if (authService != null) {
            authService.authorizedInstanceRead(instanceId);
        }
        return instanceId;
    }

    private long authorizedExecutionRead(long executionId) throws NoSuchJobExecutionException, JobSecurityException {
        if (authService != null) {
            authService.authorizedExecutionRead(executionId);
        }
        return executionId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getJobNames() {

        if (authService == null || authService.isAdmin() || authService.isMonitor()) {
            return persistenceManagerService.getJobNamesSet();
        } else if (authService.isSubmitter()) {
            return persistenceManagerService.getJobNamesSet(authService.getRunAsUser());
        }

        throw new JobSecurityException("The current user " + authService.getRunAsUser() + " is not authorized to perform any batch operations.");

    }

    @Override
    public boolean isJobInstancePurgeable(long jobInstanceId) throws NoSuchJobInstanceException, JobSecurityException {
        //Make sure users have permissions to know anything about this job instance
        if (authService != null) {
            authService.authorizedInstanceRead(jobInstanceId);
        }

        return persistenceManagerService.isJobInstancePurgeable(jobInstanceId);
    }

    @Override
    public WSJobInstance updateJobInstanceState(long instanceId, InstanceState state) {
        return persistenceManagerService.updateJobInstanceWithInstanceState(instanceId, state, new Date());
    }

    @Override
    public WSJobInstance updateJobInstanceStateOnRestart(long instanceId) {
        return (WSJobInstance) persistenceManagerService.updateJobInstanceOnRestart(instanceId, new Date());
    }

    @Override
    public WSJobInstance updateJobInstanceStateOnConsumed(long instanceId) throws BatchIllegalJobStatusTransitionException, JobInstanceNotQueuedException {
        return (WSJobInstance) persistenceManagerService.updateJobInstanceStateOnConsumed(instanceId);
    }

    @Override
    public WSJobInstance updateJobInstanceStateOnQueued(long instanceId) throws BatchIllegalJobStatusTransitionException {
        return (WSJobInstance) persistenceManagerService.updateJobInstanceStateOnQueued(instanceId);
    }

    /**
     * @return a updated JobInstance for the given appName and JSL file.
     *
     *         Note: Added for updating jobs to FAILED when JSL cannot be located, hence job fails.
     *         {@inheritDoc}
     *
     */
    @Override
    public WSJobInstance updateJobInstanceAndExecutionWithInstanceStateAndBatchStatus(long instanceId, long executionId,
                                                                                      final InstanceState state, final BatchStatus batchStatus) {

        JobInstanceEntity retMe = null;

        //Update the JobInstance
        retMe = (JobInstanceEntity) persistenceManagerService.updateJobInstanceWithInstanceStateAndBatchStatus(instanceId, state, batchStatus, new Date());

        //Update the Job Execution Instance
        persistenceManagerService.updateJobExecutionAndInstanceOnStatusChange(executionId, batchStatus, new Date());

        return retMe;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<WSStepThreadExecutionAggregate> getStepExecutionAggregatesFromJobExecution(
                                                                                           long jobExecutionId) throws NoSuchJobExecutionException, JobSecurityException {
        if (authService != null) {
            authService.authorizedExecutionRead(jobExecutionId);
        }
        return persistenceManagerService.getStepExecutionAggregatesFromJobExecutionId(jobExecutionId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WSStepThreadExecutionAggregate getStepExecutionAggregateFromJobExecution(
                                                                                    long jobExecutionId, String stepName) throws NoSuchJobExecutionException, JobSecurityException {

        if (authService != null) {
            authService.authorizedExecutionRead(jobExecutionId);
        }
        return persistenceManagerService.getStepExecutionAggregateFromJobExecutionId(jobExecutionId, stepName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WSStepThreadExecutionAggregate getStepExecutionAggregate(
                                                                    long topLevelStepExecutionId) throws IllegalArgumentException, JobSecurityException {

        if (authService != null) {
            authService.authorizedStepExecutionRead(topLevelStepExecutionId);
        }
        return persistenceManagerService.getStepExecutionAggregate(topLevelStepExecutionId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WSStepThreadExecutionAggregate getStepExecutionAggregateFromJobExecutionNumberAndStepName(long jobInstanceId,
                                                                                                     short jobExecNum,
                                                                                                     String stepName) throws NoSuchJobExecutionException, JobSecurityException {

        if (authService != null) {
            authService.authorizedInstanceRead(jobInstanceId);
        }
        return persistenceManagerService.getStepExecutionAggregateFromJobExecutionNumberAndStepName(jobInstanceId,
                                                                                                    jobExecNum, stepName);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WSJobExecution getJobExecutionFromJobExecNum(long jobInstanceId, int jobExecNum) throws NoSuchJobExecutionException, JobSecurityException {
        if (authService != null) {
            authService.authorizedInstanceRead(jobInstanceId);
        }
        return persistenceManagerService.getJobExecutionFromJobExecNum(jobInstanceId, jobExecNum);
    }

    @Override
    public WSJobInstance updateJobInstanceWithInstanceStateAndBatchStatus(
                                                                          long instanceId, InstanceState state, BatchStatus batchStatus) {
        return (WSJobInstance) persistenceManagerService.updateJobInstanceWithInstanceStateAndBatchStatus(instanceId, state, batchStatus, new Date());
    }

    @Override
    public WSJobExecution updateJobExecutionAndInstanceOnStatusChange(
                                                                      long jobExecutionId, BatchStatus status, Date date) {
        return (WSJobExecution) persistenceManagerService.updateJobExecutionAndInstanceOnStatusChange(jobExecutionId, status, date);
    }

    // DELETE ME - rename to better name
    @Override
    public WSJobExecution updateJobExecutionAndInstanceNotSetToServerYet(
                                                                         long jobExecutionId, Date date) throws ExecutionAssignedToServerException {
        return updateJobExecutionAndInstanceOnStopBeforeServerAssigned(jobExecutionId, date);
    }

    @Override
    public WSJobExecution updateJobExecutionAndInstanceOnStopBeforeServerAssigned(
                                                                                  long jobExecutionId, Date date) throws ExecutionAssignedToServerException {
        return (WSJobExecution) persistenceManagerService.updateJobExecutionAndInstanceOnStopBeforeServerAssigned(jobExecutionId, date);
    }

    @Override
    public WSRemotablePartitionExecution createRemotablePartition(RemotablePartitionKey remotablePartitionKey) {
        return persistenceManagerService.createRemotablePartition(remotablePartitionKey);
    }

    @Override
    public WSRemotablePartitionState getRemotablePartitionInternalState(RemotablePartitionKey remotablePartitionKey) {
        return persistenceManagerService.getRemotablePartitionInternalState(remotablePartitionKey);
    }

    /**
     * {@inheritDoc}
     *
     * @throws Exception
     */
    @Override
    public int getJobExecutionEntityVersion() throws Exception {
        return persistenceManagerService.getJobExecutionEntityVersion();
    }

    /**
     * {@inheritDoc}
     *
     * @throws Exception
     */
    @Override
    public int getJobInstanceEntityVersion() throws Exception {
        return persistenceManagerService.getJobInstanceEntityVersion();
    }

    @Override
    public WSJobInstance updateJobInstanceWithGroupNames(long jobInstanceId, Set<String> groupNames) {

        if (authService == null) {
            // no auth service (ie security feature not present, so cannot perform group security
            logger.log(Level.WARNING, "BATCH_SECURITY_NOT_ACTIVE", new Object[] { jobInstanceId });

            // no groups should be persisted - pass in an empty set
            return persistenceManagerService.updateJobInstanceWithGroupNames(jobInstanceId, new HashSet<String>());
        } else {
            Set<String> normalizedNames = new HashSet<String>();

            normalizedNames = authService.normalizeGroupNames(groupNames);

            return persistenceManagerService.updateJobInstanceWithGroupNames(jobInstanceId, normalizedNames);
        }
    }

}
