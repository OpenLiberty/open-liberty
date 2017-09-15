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

package javax.batch.operations;

import java.util.List;
import java.util.Set;
import java.util.Properties;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.StepExecution;

/**
 * JobOperator provide the interface for operating on batch jobs.
 * Through the JobOperator a program can start, stop, and restart jobs.
 * It can additionally inspect job history, to discover what jobs
 * are currently running and what jobs have previously run. 
 * 
 * The JobOperator interface imposes no security constraints. However,
 * the implementer is free to limit JobOperator methods with a security
 * scheme of its choice.  The implementer should terminate any method
 * that is limited by the security scheme with a JobSecurityException. 
 *
 */

public interface JobOperator {
	/**
	 * Returns a set of all job names known to the batch runtime.
	 * 
	 * @return a set of job names.
	 * @throws JobSecurityException Thrown when the implementation determines 
	 *   that execution of this method with these parameters is not authorized
	 *    (by some implementation-specific mechanism).
	 */
	public Set<String> getJobNames() throws JobSecurityException;
	/**
	 * Returns number of instances of a job with a particular name.
	 * 
	 * @param jobName
	 *            specifies the name of the job.
	 * @return count of instances of the named job.
	 * @throws NoSuchJobException Thrown when the jobName parameter
	 *   value doesn't correspond to a job recognized by the 
     *   implementation's repository.
	 * @throws JobSecurityException Thrown when the implementation determines 
	 *   that execution of this method with these parameters is not authorized
	 *    (by some implementation-specific mechanism).
	 */
	public int getJobInstanceCount(String jobName) throws 
         NoSuchJobException,
         JobSecurityException;

	/**
	 * Returns all JobInstances belonging to a job with a particular name 
       * in reverse chronological order.
	 * 
	 * @param jobName
	 *            specifies the job name.
	 * @param start
	 *            specifies the relative starting number (zero based) to 
       *            return from the	 
	 *            maximal list of job instances.
	 * @param count
	 *            specifies the number of job instances to return from the
	 *            starting position of the maximal list of job instances.
	 * @return list of JobInstances. 
	 * @throws NoSuchJobException Thrown when the jobName parameter
	 *   value doesn't correspond to a job recognized by the 
     *   implementation's repository.
	 * @throws JobSecurityException Thrown when the implementation determines 
	 *   that execution of this method with these parameters is not authorized
	 *    (by some implementation-specific mechanism).
	 */
	public List<JobInstance> getJobInstances(String jobName, int start, 
        int count)throws NoSuchJobException, JobSecurityException;

	/**
	 * Returns execution ids for job instances with the specified
	 * name that have running executions. 
	 *
	 * @param jobName
	 *            specifies the job name.
	 * @return a list of execution ids. 
	 * @throws NoSuchJobException Thrown when the jobName parameter
	 *   value doesn't correspond to a job recognized by the 
     *   implementation's repository.
	 * @throws JobSecurityException Thrown when the implementation determines 
	 *   that execution of this method with these parameters is not authorized
	 *    (by some implementation-specific mechanism).
	 */
	public List<Long> getRunningExecutions(String jobName) throws 
        NoSuchJobException, JobSecurityException;


	/**
	 * Returns job parameters for a specified job instance. These are the 
       * key/value pairs specified when the instance was originally created 
       * by the start method.
	 * 
	 * @param executionId
	 *            specifies the execution from which to retrieve the 
       * parameters. 
	 * @return a Properties object containing the key/value job parameter 
       * pairs.
	 * @throws NoSuchJobExecutionException Thrown when the executionId parameter
	 *   value doesn't correspond to an execution recognized by the 
     *   implementation's repository.
	 * @throws JobSecurityException Thrown when the implementation determines 
	 *   that execution of this method with these parameters is not authorized
	 *    (by some implementation-specific mechanism).
	 */
	public Properties getParameters(long executionId)
			throws NoSuchJobExecutionException, JobSecurityException;

	/**
	 * Creates a new job instance and starts the first execution of that
	 * instance, which executes asynchronously.
	 * 
	 * Note the Job XML describing the job is first searched for by name
	 * according to a means prescribed by the batch runtime implementation.
	 * This may vary by implementation. If the Job XML is not found by that
	 * means, then the batch runtime must search for the specified Job XML 
	 * as a resource from the META-INF/batch-jobs directory based on the 
	 * current class loader. Job XML files under META-INF/batch-jobs 
	 * directory follow a naming convention of "name".xml where "name" is
	 * the value of the jobXMLName parameter (see below).   
	 * 
	 * @param jobXMLName
	 *            specifies the name of the Job XML describing the job.
	 * @param jobParameters
	 *            specifies the keyword/value pairs for attribute 
	 *            substitution in the Job XML.
	 * @return executionId for the job execution.
	 * @throws JobStartException Thrown for some error condition other than
	 * those listed by the other checked exceptions on this method.  Note that
	 * batch runtime implementations have a choice of detecting certain 
	 * conditions via upfront validation or only later, during execution.  
	 * This nets out to the fact that one implementation may throw 
	 * JobStartException on a given error condition while another may not.
	 * @throws JobSecurityException Thrown when the implementation determines 
	 *   that execution of this method with these parameters is not authorized
	 *    (by some implementation-specific mechanism).
	 */
	public long start(String jobXMLName, Properties jobParameters) throws 
        JobStartException, JobSecurityException;

	/**
	 * Restarts a failed or stopped job instance, which executes asynchronously.
	 * 
	 * @param executionId
	 *            specifies the execution to to restart. This execution 
	 *            must be the most recent execution that ran.
	 * @param restartParameters
	 *            specifies the keyword/value pairs for attribute 
	 *            substitution in the Job XML.            
	 * @return new executionId
	 * @throws JobExecutionAlreadyCompleteException Thrown when the job execution
	 * associated with executionId is currently complete.
	 * @throws NoSuchJobExecutionException Thrown when the executionId parameter
	 *   value doesn't correspond to an execution recognized by the 
     *   implementation's repository.
	 * @throws JobExecutionNotMostRecentException Thrown when the executionId
	 * parameter value does not represent the most recent execution for the
	 * corresponding job instance.
	 * @throws JobRestartException Thrown for some error condition other than
	 * those listed by the other checked exceptions on this method.  Note that
	 * batch runtime implementations have a choice of detecting certain 
	 * conditions via upfront validation or only later, during execution.  
	 * This nets out to the fact that one implementation may throw 
	 * JobRestartException on a given error condition while another may not.
	 * @throws JobSecurityException Thrown when the implementation determines 
	 *   that execution of this method with these parameters is not authorized
	 *    (by some implementation-specific mechanism).
	 */
	public long restart(long executionId, Properties restartParameters)			
			throws JobExecutionAlreadyCompleteException,
			NoSuchJobExecutionException, 
			JobExecutionNotMostRecentException, 
			JobRestartException,
			JobSecurityException;

	/**
	 * Request a running job execution stops. This
	 * method notifies the job execution to stop 
	 * and then returns. The job execution normally 
	 * stops and does so asynchronously. Note 
	 * JobOperator cannot guarantee the jobs stops: 
	 * it is possible a badly behaved batch application 
	 * does not relinquish control. 
	 * <p>
	 * Note for partitioned batchlet steps the Batchlet
	 * stop method is invoked on each thread actively
	 * processing a partition.   
	 * 
	 * @param executionId
	 *            specifies the job execution to stop. 
	 *            The job execution must be running.
	 * @throws NoSuchJobExecutionException Thrown when the executionId parameter
	 *   value doesn't correspond to an execution recognized by the 
     *   implementation's repository.
	 * @throws JobExecutionNotRunningException Thrown when the associated
	 *   execution is not running (is not already STARTED or STARTING).
	 * @throws JobSecurityException Thrown when the implementation determines 
	 *   that execution of this method with these parameters is not authorized
	 *    (by some implementation-specific mechanism).
	 */
	public void stop(long executionId) throws NoSuchJobExecutionException,
			JobExecutionNotRunningException, JobSecurityException;

	/**
	 * Set batch status to ABANDONED.  The instance must have 
	 * no running execution. 
	 * <p>
	 * Note that ABANDONED executions cannot be restarted.
	 * 
	 * @param executionId
	 *            specifies the job execution to abandon.
	 * @throws NoSuchJobExecutionException Thrown when the executionId parameter
	 *   value doesn't correspond to an execution recognized by the 
     *   implementation's repository.
	 * @throws JobExecutionIsRunningException Thrown when the job execution
	 * associated with executionId is currently running
	 * @throws JobSecurityException Thrown when the implementation determines 
	 *   that execution of this method with these parameters is not authorized
	 *    (by some implementation-specific mechanism).
	 */
	public void abandon(long executionId) throws 
                  NoSuchJobExecutionException, 
			JobExecutionIsRunningException, JobSecurityException;
	
	
	/**
	 * Return the job instance for the specified execution id.
	 * 
	 * @param executionId
	 *            specifies the job execution.
	 * @return job instance
	 * @throws NoSuchJobExecutionException Thrown when the executionId parameter
	 *   value doesn't correspond to an execution recognized by the 
     *   implementation's repository.
	 * @throws JobSecurityException Thrown when the implementation determines 
	 *   that execution of this method with these parameters is not authorized
	 *    (by some implementation-specific mechanism).
	 */
	public JobInstance getJobInstance(long executionId) throws 
        NoSuchJobExecutionException, JobSecurityException;

	/**
	 * Return all job executions belonging to the specified job instance.
	 * 
	 * @param instance
	 *            specifies the job instance.
	 * @return list of job executions
	 * @throws NoSuchJobInstanceException Thrown when the instance parameter
	 *   value doesn't correspond to a job instance recognized by the 
     *   implementation's repository.
	 * @throws JobSecurityException Thrown when the implementation determines 
	 *   that execution of this method with these parameters is not authorized
	 *    (by some implementation-specific mechanism).
	 */
	public List<JobExecution> getJobExecutions(JobInstance instance) throws 
        NoSuchJobInstanceException, JobSecurityException;

	/**
	 * Return job execution for specified execution id.
	 * 
	 * @param executionId
	 *            specifies the job execution.
	 * @return job execution
	 * @throws NoSuchJobExecutionException Thrown when the executionId parameter
	 *   value doesn't correspond to an execution recognized by the 
     *   implementation's repository.
	 * @throws JobSecurityException Thrown when the implementation determines 
	 *   that execution of this method with these parameters is not authorized
	 *    (by some implementation-specific mechanism).
	 */
	public JobExecution getJobExecution(long executionId) 
        throws NoSuchJobExecutionException, JobSecurityException;

	/**
	 * Return StepExecutions for specified execution id. 
	 * 
	 * @param jobExecutionId
	 *            specifies the job execution.
	 * @return step executions (order not guaranteed)
	 * @throws NoSuchJobExecutionException Thrown when the jobExecutionId parameter
	 *   value doesn't correspond to an execution recognized by the 
     *   implementation's repository.
	 * @throws JobSecurityException Thrown when the implementation determines 
	 *   that execution of this method with these parameters is not authorized
	 *    (by some implementation-specific mechanism).
	 */
	public List<StepExecution> getStepExecutions(long jobExecutionId) 
        throws NoSuchJobExecutionException, JobSecurityException;	

}
