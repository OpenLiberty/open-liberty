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
package com.ibm.jbatch.container.api.impl;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.batch.operations.BatchRuntimeException;
import javax.batch.operations.JobExecutionAlreadyCompleteException;
import javax.batch.operations.JobExecutionIsRunningException;
import javax.batch.operations.JobExecutionNotMostRecentException;
import javax.batch.operations.JobExecutionNotRunningException;
import javax.batch.operations.JobOperator;
import javax.batch.operations.JobRestartException;
import javax.batch.operations.JobSecurityException;
import javax.batch.operations.JobStartException;
import javax.batch.operations.NoSuchJobException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.operations.NoSuchJobInstanceException;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.StepExecution;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;


/**
 * Thin wrapper around the real JobOperatorImpl that suspends the active transaction
 * for every JobOperator API method.
 * 
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class JobOperatorImplSuspendTran implements JobOperator {

    /**
     * For suspending/resuming transactions
     */
    private TransactionManager tranMgr;
    
    /**
     * The real JobOperator.  All calls forwarded to this guy.
     */
    private JobOperator jobOperator;
    
	/**
	 * DS activate (for trace logs)
	 */
	@Activate
	protected void activate(ComponentContext context,  Map<String, Object> config) {		
	}
	
	/**
	 * DS deactivate (for trace logs)
	 */
	@Deactivate
	protected void deactivate() {
	}

    /**
     * DS injection
     */
    @Reference( service = EmbeddableWebSphereTransactionManager.class )
    protected void setTransactionManager(TransactionManager tranMgr) {
        this.tranMgr = tranMgr;
    }
    
    /**
     * DS injection.
     */
    @Reference(target = "(component.name=com.ibm.jbatch.container.api.impl.JobOperatorImpl)")
    protected void setJobOperator(JobOperator jobOperator) {
        this.jobOperator = jobOperator;
    }
    
    /**
     * @return the just-suspended tran, or null if there wasn't one
     */
    private Transaction suspendTran() {
        try {
            return tranMgr.suspend();
        } catch (SystemException se) {
            throw new BatchRuntimeException("Failed to suspend current transaction before JobOperator method", se);
        }
    }
    
    /**
     * Resume the given tran.
     */
    private void resumeTran(Transaction tran) {
        if (tran != null) {
            try {
                tranMgr.resume(tran);
            } catch (Exception e) {
                throw new BatchRuntimeException("Failed to resume transaction after JobOperator method", e);
            } 
        }
    }
    
    @Override
    public Set<String> getJobNames() throws JobSecurityException {
        Transaction tran = suspendTran();
        try {
            return jobOperator.getJobNames();
        } finally {
            resumeTran(tran);
        }
    }

    @Override
    public int getJobInstanceCount(String jobName) throws NoSuchJobException, JobSecurityException {
        Transaction tran = suspendTran();
        try {
            return jobOperator.getJobInstanceCount(jobName);
        } finally {
            resumeTran(tran);
        }
    }

    @Override
    public List<JobInstance> getJobInstances(String jobName, int start, int count) throws NoSuchJobException, JobSecurityException {
        Transaction tran = suspendTran();
        try {
            return jobOperator.getJobInstances(jobName, start, count);
        } finally {
            resumeTran(tran);
        }
    }

    @Override
    public List<Long> getRunningExecutions(String jobName) throws NoSuchJobException, JobSecurityException {
        Transaction tran = suspendTran();
        try {
            return jobOperator.getRunningExecutions(jobName);
        } finally {
            resumeTran(tran);
        }
    }

    @Override
    public Properties getParameters(long executionId) throws NoSuchJobExecutionException, JobSecurityException {
        Transaction tran = suspendTran();
        try {
            return jobOperator.getParameters(executionId);
        } finally {
            resumeTran(tran);
        }
    }

    @Override
    public long start(String jobXMLName, Properties jobParameters) throws JobStartException, JobSecurityException {
        Transaction tran = suspendTran();
        try {
            return jobOperator.start(jobXMLName, jobParameters);
        } finally {
            resumeTran(tran);
        }
    }

    @Override
    public long restart(long executionId, Properties restartParameters) throws JobExecutionAlreadyCompleteException,
                                                                               NoSuchJobExecutionException, 
                                                                               JobExecutionNotMostRecentException,
                                                                               JobRestartException, 
                                                                               JobSecurityException {
        Transaction tran = suspendTran();
        try {
            return jobOperator.restart(executionId, restartParameters);
        } finally {
            resumeTran(tran);
        }
    }

    @Override
    public void stop(long executionId) throws NoSuchJobExecutionException, JobExecutionNotRunningException, JobSecurityException {
        Transaction tran = suspendTran();
        try {
            jobOperator.stop(executionId);
        } finally {
            resumeTran(tran);
        }
    }

    @Override
    public void abandon(long executionId) throws NoSuchJobExecutionException, JobExecutionIsRunningException, JobSecurityException {
        Transaction tran = suspendTran();
        try {
            jobOperator.abandon(executionId);
        } finally {
            resumeTran(tran);
        }
    }

    @Override
    public JobInstance getJobInstance(long executionId) throws NoSuchJobExecutionException, JobSecurityException {
        Transaction tran = suspendTran();
        try {
            return jobOperator.getJobInstance(executionId);
        } finally {
            resumeTran(tran);
        }
    }

    @Override
    public List<JobExecution> getJobExecutions(JobInstance instance) throws NoSuchJobInstanceException, JobSecurityException {
        Transaction tran = suspendTran();
        try {
            return jobOperator.getJobExecutions(instance);
        } finally {
            resumeTran(tran);
        }
    }

    @Override
    public JobExecution getJobExecution(long executionId) throws NoSuchJobExecutionException, JobSecurityException {
        Transaction tran = suspendTran();
        try {
            return jobOperator.getJobExecution(executionId);
        } finally {
            resumeTran(tran);
        }
    }

    @Override
    public List<StepExecution> getStepExecutions(long jobExecutionId) throws NoSuchJobExecutionException, JobSecurityException {
        Transaction tran = suspendTran();
        try {
            return jobOperator.getStepExecutions(jobExecutionId);
        } finally {
            resumeTran(tran);
        }
    }

}
