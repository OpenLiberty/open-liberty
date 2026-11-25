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

package com.ibm.ws.jbatch.wola.internal;

import java.util.List;
import java.util.Properties;

import javax.batch.operations.JobSecurityException;
import javax.batch.runtime.JobInstance;

import com.ibm.jbatch.container.ws.BatchDispatcherException;
import com.ibm.jbatch.container.ws.BatchJobNotLocalException;
import com.ibm.jbatch.container.ws.WSJobExecution;
import com.ibm.jbatch.container.ws.WSJobInstance;
import com.ibm.jbatch.container.ws.WSJobRepository;
import com.ibm.ws.jbatch.rest.BatchManager;

/**
 * Some wrapper methods around BatchManager and WSJobRepository.
 *
 * Mostly these are convenience methods that take either an instanceId or an
 * executionId and do the right thing based on whichever one (instanceId or
 * executionid) is specified.
 *
 */
public class BatchManagerHelper {

	private static final String CLASS_NAME = BatchManagerHelper.class.getName();

	/**
	 * The guts of batch job management.
	 */
	private BatchManager batchManager;

	/**
	 * For getting job data from the DB.
	 */
	private WSJobRepository jobRepository;

	/**
	 * CTOR.
	 */
	public BatchManagerHelper(BatchManager batchManager, WSJobRepository jobRepository) {
		this.batchManager = batchManager;
		this.jobRepository = jobRepository;
	}

	/**
	 * Restart a Job.
	 * 
	 * Either an instanceId or executionId may be specified. Pass -1 for whichever
	 * one you're NOT specifying.
	 * 
	 * @return the job instance.
	 */
	public WSJobInstance restartJob(long instanceId, long executionId, Properties jobParameters) {

		if (instanceId > 0) {
			return batchManager.restartJobInstance(instanceId, jobParameters);

		} else if (executionId > 0) {
			return batchManager.restartJobExecution(executionId, jobParameters);

		} else {
			throw new IllegalArgumentException("Either instanceId or executionId must be specified");
		}
	}

	/**
	 * Stop a Job.
	 * 
	 * Either an instanceId or executionId may be specified. Pass -1 for whichever
	 * one you're NOT specifying.
	 * 
	 * @return the latest job execution record.
	 * @throws BatchJobNotLocalException
	 * @throws BatchDispatcherException
	 * @throws JobSecurityException
	 */
	public WSJobExecution stopJob(long instanceId, long executionId)
			throws JobSecurityException, BatchDispatcherException, BatchJobNotLocalException {

		if (instanceId > 0) {
			batchManager.stopJobInstance(instanceId);

		} else if (executionId > 0) {
			batchManager.stopJobExecution(executionId);

		} else {
			throw new IllegalArgumentException("Either instanceId or executionId must be specified");
		}

		// Return the latest jobexecution
		List<WSJobExecution> jobExecutions = getJobExecutions(instanceId, executionId);

		return (jobExecutions != null && jobExecutions.size() > 0) ? jobExecutions.get(0) : null;
	}

	/**
	 * 
	 * Either an instanceId or executionId may be specified. Pass -1 for whichever
	 * you're NOT specifying.
	 * 
	 * @return the latest JobExecution record for the job.
	 */
	protected List<WSJobExecution> getJobExecutions(long instanceId, long executionId) {

		if (instanceId > 0) {
			return jobRepository.getJobExecutionsFromInstance(instanceId);

		} else if (executionId > 0) {
			JobInstance jobInstance = jobRepository.getJobInstanceFromExecution(executionId);
			return jobRepository.getJobExecutionsFromInstance(jobInstance.getInstanceId());

		} else {
			throw new IllegalArgumentException("Either instanceId or executionId must be specified");
		}
	}

	/**
	 * Return the JobInstance record.
	 * 
	 * Either an instanceId or executionId may be specified. Pass -1 for whichever
	 * one you're NOT specifying.
	 * 
	 * @return the job instance
	 */
	protected WSJobInstance getJobInstance(long instanceId, long executionId) {

		if (instanceId > 0) {
			return jobRepository.getJobInstance(instanceId);

		} else if (executionId > 0) {
			return jobRepository.getJobInstanceFromExecution(executionId);

		} else {
			throw new IllegalArgumentException("Either instanceId or executionId must be specified");
		}
	}

}