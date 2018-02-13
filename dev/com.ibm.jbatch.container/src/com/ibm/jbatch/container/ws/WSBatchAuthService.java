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

import java.util.Set;

import javax.batch.operations.JobSecurityException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.operations.NoSuchJobInstanceException;

/**
 * This is really a placeholder for the authorization service.
 *
 * As we work to add the roles of submitter, monitor, and admin, we
 * need to flesh this out further.
 *
 * Anything I write could imply to someone I've given this more thought than
 * I have.
 *
 * Really the only thing that is accomplished with this is laying the
 * groundwork for the notion that authorization will function differently
 * in WAS than in the RI.
 *
 * In WAS the JobOperator will need to use this service in addition to the WSJobManager.
 */
public interface WSBatchAuthService {

    /**
     * @param instanceId
     * @return The input parameter <code>instanceId</code> if the current security context is authorized to operate on the corresponding job instance
     * @throws NoSuchJobInstanceException if the job instance identified by <code>instanceId</code> isn't present in the repository
     * @throws JobSecurityException if the current security context isn't authorized to operate on the job instance identified by <code>instanceId</code>
     */
    public long authorizedInstanceRead(long instanceId) throws NoSuchJobInstanceException, JobSecurityException;

    public boolean isAuthorizedInstanceRead(long instanceId) throws NoSuchJobInstanceException;

    /**
     * Since authorization is based on the job instance, the checks performed against a given job execution are equivalent to the
     * checks performed against the corresponding instance. This means that for two executions, JE1 and JE2, of a given instance JI1,
     * this method will either succeed (i.e. return normally with <code>executionId</code>) or throw <code>JobSecurityException</code> for
     * the execution IDs of both JE1 and JE2.
     *
     * @param executionId
     * @return The input parameter <code>executionId</code> if the current security context is authorized to operate on the corresponding job execution
     * @throws NoSuchJobExecutionException if the job execution identified by <code>executionId</code> isn't present in the repository
     * @throws JobSecurityException if the current security context isn't authorized to operate on the job execution identified by <code>executionId</code>
     */
    public long authorizedExecutionRead(long executionId) throws NoSuchJobExecutionException, JobSecurityException;

    public boolean isAuthorizedExecutionRead(long executionId) throws NoSuchJobExecutionException;

    /**
     * In the RI, we used "current application name" as the tag.
     * In WebSphere, this will be more like "current runAs userid".
     *
     * @return the runAs "userid"
     */
    public abstract String getRunAsUser();

    /**
     * Returns with no exceptions if the current seucrity context is authorized to submit jobs
     *
     * @throws JobSecurityException if the current security context isn't authorized to submit jobs
     */
    public abstract void authorizedJobSubmission() throws JobSecurityException;

    /**
     * Returns the given execution id if the current user is authorized to restart it.
     *
     * @param executionId the executionId to restart
     * @return the given executionId that is authorized to be restarted
     * @throws NoSuchJobExecutionException
     * @throws JobSecurityException if the current user does not have authority to restart the given executionId
     */
    public abstract long authorizedJobRestartByExecution(long executionId) throws NoSuchJobExecutionException, JobSecurityException;

    /**
     * Returns the given instance id if the current user is authorized to restart it.
     *
     * @param instanceId the instanceId to restart
     * @return the given instanceId that is authorized to be restarted
     * @throws JobSecurityException if the current user does not have authority to restart the given instanceId
     */
    public abstract long authorizedJobRestartByInstance(long instanceId) throws JobSecurityException;

    /**
     * Returns the given execution id if the current user is authorized to stop it.
     *
     * @param executionId the executionId to stop
     * @return the given executionId that is authorized to be stopped
     * @throws NoSuchJobExecutionException
     * @throws JobSecurityException if the current user does not have authority to stop the given executionId
     */
    public abstract long authorizedJobStopByExecution(long executionId) throws NoSuchJobExecutionException, JobSecurityException;

    /**
     * Returns the given instance id if the current user is authorized to stop it.
     *
     * @param instanceId the instanceId to stop
     * @return the given instanceId that is authorized to be stopped
     * @throws JobSecurityException if the current user does not have authority to stop the given instanceId
     */
    public abstract long authorizedJobStopByInstance(long instanceId) throws JobSecurityException;

    /**
     * Returns the given instance id if the current user is authorized to purge it
     *
     * @param instanceId the job instanceId to purge
     * @return
     * @throws NoSuchJobInstanceException
     * @throws JobSecurityException f the current user does not have authority to purge the given instanceId
     */
    public long authorizedJobPurgeByInstance(long instanceId) throws NoSuchJobInstanceException, JobSecurityException;

    /**
     * @return true if the current user is part of the batchAdmin role, false otherwise
     */
    public abstract boolean isAdmin();

    /**
     * @return true if the current user is part of the batchSubmitter role, false otherwise
     */
    public abstract boolean isSubmitter();

    /**
     * @return true if the current user is part of the batchMonitor role, false otherwise
     */
    public abstract boolean isMonitor();

    /**
     * @return true if the current user is part of the batchGroupAdmin role, false otherwise
     */
    public boolean isGroupAdmin();

    /**
     * /**
     *
     * @return true if the current user is part of the batchGroupMonitor role, false otherwise
     */
    public abstract boolean isGroupMonitor();

    /**
     * @return true if the current user is part of any batch role. batchAdmin, batchGroupAdmin, batchSubmitter, batchGroupMonitor or batchMonitor, false otherwise
     */
    public boolean isInAnyBatchRole();

    /**
     * Returns the given instance id if the current user is authorized to abandon it
     *
     * @param instanceId the job instanceId to abandon
     * @return
     * @throws NoSuchJobInstanceException
     * @throws JobSecurityException f the current user does not have authority to abandon the given instanceId
     */
    long authorizedJobAbandonByInstance(long instanceId) throws NoSuchJobInstanceException, JobSecurityException;

    /**
     * @param stepExecutionId
     *
     * @throws IllegalArgumentException if the corresponding step execution entry doesn't exist in the persistent store.
     * @throws JobSecurityException f the current user does not have authority to read the given stepExecutionId
     */
    public long authorizedStepExecutionRead(long stepExecutionId) throws IllegalArgumentException, JobSecurityException;

    /**
     * @return
     */
    Set<String> getGroupsForSubject();

    /**
     * @param groupNames
     * @return
     */
    Set<String> normalizeGroupNames(Set<String> groupNames);
}
