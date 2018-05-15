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
package com.ibm.jbatch.container.api.impl;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.StepExecution;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.jbatch.container.persistence.jpa.JobExecutionEntity;
import com.ibm.jbatch.container.persistence.jpa.JobInstanceEntity;
import com.ibm.jbatch.container.services.IBatchKernelService;
import com.ibm.jbatch.container.services.IJobXMLSource;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.servicesmanager.ServicesManager;
import com.ibm.jbatch.container.ws.BatchStatusValidator;
import com.ibm.jbatch.container.ws.BatchSubmitInvalidParametersException;
import com.ibm.jbatch.container.ws.InstanceState;
import com.ibm.jbatch.container.ws.WSBatchAuthService;
import com.ibm.jbatch.container.ws.WSJobExecution;
import com.ibm.jbatch.container.ws.WSJobInstance;
import com.ibm.jbatch.container.ws.events.BatchEventsPublisher;
import com.ibm.jbatch.spi.BatchSecurityHelper;
import com.ibm.jbatch.spi.services.IJobXMLLoaderService;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class JobOperatorImpl implements JobOperator {

    private final static Logger logger = Logger.getLogger(JobOperatorImpl.class.getName());

    /**
     * For accessing other service components like BatchKernel, PersistenceManager, etc.
     */
    private ServicesManager servicesManager;

    /**
     * TODO: can we combine WSBatchAuthService with BatchSecurityHelper?
     */
    private BatchSecurityHelper batchSecurityHelper;

    /**
     * For authorizing the user. This is an optional dependency.
     */
    private WSBatchAuthService authService;

    /**
     * For publishing jms event
     */
    private BatchEventsPublisher eventsPublisher = null;

    /**
     * DS activate (for trace logs)
     */
    @Activate
    protected void activate(ComponentContext context, Map<String, Object> config) {}

    /**
     * DS deactivate (for trace logs)
     */
    @Deactivate
    protected void deactivate() {}

    /**
     * DS injection
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setEventsPublisher(BatchEventsPublisher publisher) {
        eventsPublisher = publisher;
    }

    protected void unsetEventsPublisher(BatchEventsPublisher publisher) {
        if (eventsPublisher == publisher)
            eventsPublisher = publisher;
    }

    /**
     * DS injection
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setWSBatchAuthService(WSBatchAuthService bas) {
        this.authService = bas;
    }

    /**
     * DS un-inject
     */
    protected void unsetWSBatchAuthService(WSBatchAuthService bas) {
        if (this.authService == bas) {
            this.authService = null;
        }
    }

    /**
     * DS injection
     */
    @Reference
    protected void setServicesManager(ServicesManager ref) {
        servicesManager = ref;
    }

    /**
     * DS inject - This reference is NOT optional
     */
    @Reference
    protected void setBatchSecurityHelper(BatchSecurityHelper batchSecurityHelper) {
        this.batchSecurityHelper = batchSecurityHelper;
    }

    /**
     * DS un-inject
     */
    protected void unsetBatchSecurityHelper(BatchSecurityHelper bsh) {
        if (this.batchSecurityHelper == bsh) {
            this.batchSecurityHelper = null;
        }
    }

    /**
     * @return the batch kernel service.
     */
    protected IBatchKernelService getBatchKernelService() {
        return servicesManager.getBatchKernelService();
    }

    /**
     * @return the batch persistence service.
     */
    protected IPersistenceManagerService getPersistenceManagerService() {
        return servicesManager.getPersistenceManagerService();
    }

    /**
     * @return the "delegating" job xml service from ServicesManager (as opposed
     *         to the "preferred").
     */
    protected IJobXMLLoaderService getJobXMLLoaderService() {
        return servicesManager.getDelegatingJobXMLLoaderService();
    }

    /**
     * @return job execution id.
     */
    @Override
    @FFDCIgnore(JobSecurityException.class)
    public long start(String jobXMLName, Properties jobParameters) throws JobStartException, JobSecurityException {

        long retVal = 0L;
        /*
         * The whole point of this method is to have JobStartException serve as a blanket exception for anything other
         * than the rest of the more specific exceptions declared on the throws clause. So we won't log but just rethrow.
         */
        try {
            retVal = startInternal(jobXMLName, jobParameters);
        } catch (JobSecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new JobStartException(e);
        }
        return retVal;
    }

    /**
     * @return Stringified jobparams for tracing purposes.
     */
    @FFDCIgnore(IOException.class)
    protected String traceJobParameters(Properties jobParameters) {
        if (logger.isLoggable(Level.FINE)) {
            StringWriter jobParameterWriter = new StringWriter();
            if (jobParameters != null) {
                try {
                    jobParameters.store(jobParameterWriter, "Job parameters: ");
                } catch (IOException e) {
                    jobParameterWriter.write("Job parameters: not printable due to IOException: " + e.getMessage());
                }
            } else {
                jobParameterWriter.write("Job parameters: null");
            }
            return jobParameterWriter.toString();
        }
        return null;
    }

    /**
     * @return job execution id
     */
    private long startInternal(String jobXMLName, Properties jobParameters) throws JobStartException, JobSecurityException {

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Starting job: jobXMLName = " + jobXMLName + "\n" + traceJobParameters(jobParameters));
        }

        if (authService != null) {
            authService.authorizedJobSubmission();
        }

        WSJobInstance jobInstance = getBatchKernelService().createJobInstanceIntraApplication(jobXMLName, batchSecurityHelper.getRunAsUser());
        WSJobExecution jobExecution = getPersistenceManagerService().createJobExecution(jobInstance.getInstanceId(), jobParameters, new Date());

        IJobXMLSource jobXML = null;
        long executionId = 0;

        try {

            jobXML = getJobXMLLoaderService().loadJSL(jobXMLName);

            executionId = getBatchKernelService().startJob(jobInstance, jobXML, jobParameters, jobExecution.getExecutionId()).getKey();

        } catch (BatchSubmitInvalidParametersException bsipe) {
            markInstanceExecutionFailed(jobInstance, jobExecution, this.getCorrelationId(jobParameters));
            throw bsipe;
        } catch (RuntimeException e) {
            markInstanceExecutionFailed(jobInstance, jobExecution, this.getCorrelationId(jobParameters));
            throw e;
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Started job execution with executionId: " + executionId);
        }

        return executionId;
    }

    private void markInstanceExecutionFailed(long executionId, String correlationId) {
        WSJobExecution jobExecution = getPersistenceManagerService().getJobExecution(executionId);
        WSJobInstance jobInstance = jobExecution.getJobInstance();

        markInstanceExecutionFailed(jobInstance, jobExecution, correlationId);
    }

    /*
     * Used to mark job failed if start attempt fails.
     * Example failure is a bad jsl name.
     */
    private void markInstanceExecutionFailed(WSJobInstance jobInstance, WSJobExecution jobExecution, String correlationId) {

        //Disregard any attempted transitions out of the ABANDONED state.
        if (jobInstance.getBatchStatus() == BatchStatus.ABANDONED) {

            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Attempt to transition from BatchStatus ABANDONED to FAILED is disallowed. ");
            }

            return;
        }

        getPersistenceManagerService().updateJobInstanceWithInstanceStateAndBatchStatus(jobInstance.getInstanceId(),
                                                                                        InstanceState.FAILED,
                                                                                        BatchStatus.FAILED,
                                                                                        new Date());

        publishEvent(jobInstance, BatchEventsPublisher.TOPIC_INSTANCE_FAILED, correlationId);

        getPersistenceManagerService().updateJobExecutionAndInstanceOnStatusChange(jobExecution.getExecutionId(),
                                                                                   BatchStatus.FAILED,
                                                                                   new Date());

        publishEvent(jobExecution, BatchEventsPublisher.TOPIC_EXECUTION_FAILED, correlationId);
    }

    /*
     * Find the correlation id if it has been passed.
     */
    private String getCorrelationId(Properties jobParameters) {

        if (jobParameters != null) {
            return jobParameters.getProperty("com_ibm_ws_batch_events_correlationId", null);
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void abandon(long executionId) throws NoSuchJobExecutionException, JobExecutionIsRunningException, JobSecurityException {
        batchRoleCheck();

        long jobInstanceId = getPersistenceManagerService().getJobInstanceIdFromExecutionId(executionId);

        if (authService != null) {
            authService.authorizedJobAbandonByInstance(jobInstanceId);
        }

        JobExecution jobEx = getPersistenceManagerService().getJobExecution(executionId);

        // if it is not in STARTED or STARTING state, mark it as ABANDONED
        List<BatchStatus> runningStatusesList = Arrays.asList(new BatchStatus[] { BatchStatus.STARTED, BatchStatus.STARTING });
        Set<BatchStatus> runningStatusesSet = Collections.unmodifiableSet(new HashSet<BatchStatus>(runningStatusesList));

        if (!runningStatusesSet.contains(jobEx.getBatchStatus())) {
            // update table to reflect ABANDONED state
            getPersistenceManagerService().updateJobExecutionAndInstanceOnStatusChange(jobEx.getExecutionId(), BatchStatus.ABANDONED, new Date());
            logger.fine("Job Execution: " + executionId + " was abandoned");
        } else {
            throw new JobExecutionIsRunningException("Job Execution: " + executionId + " is still running");
        }

        publishEvent((WSJobExecution) jobEx, BatchEventsPublisher.TOPIC_EXECUTION_ABANDONED);
    }

    /**
     * Helper method to publish event
     *
     * @param jobEx
     * @param topicToPublish
     */
    private void publishEvent(WSJobExecution jobEx, String topicToPublish) {
        publishEvent(jobEx, topicToPublish, null);
    }

    /**
     * Helper method to publish event
     *
     * @param jobEx
     * @param topicToPublish
     * @param correlationId
     */
    private void publishEvent(WSJobExecution jobEx, String topicToPublish, String correlationId) {
        if (eventsPublisher != null) {
            eventsPublisher.publishJobExecutionEvent(jobEx, topicToPublish, correlationId);
        }
    }

    /**
     * Helper method to publish event with correlationId
     *
     * @param jobInstance
     * @param topicToPublish
     * @param correlationId
     */
    private void publishEvent(WSJobInstance jobInst, String topicToPublish, String correlationId) {
        if (eventsPublisher != null) {
            eventsPublisher.publishJobInstanceEvent(jobInst, topicToPublish, correlationId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JobExecution getJobExecution(long jobExecutionId) throws NoSuchJobExecutionException, JobSecurityException {

        if (authService != null) {
            authService.authorizedExecutionRead(jobExecutionId);
        }

        return getPersistenceManagerService().getJobExecution(jobExecutionId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<JobExecution> getJobExecutions(JobInstance instance) throws NoSuchJobInstanceException, JobSecurityException {

        if (authService != null) {
            authService.authorizedInstanceRead(instance.getInstanceId());
        }

        return new ArrayList<JobExecution>(getPersistenceManagerService().getJobExecutionsFromJobInstanceId(instance.getInstanceId()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JobInstance getJobInstance(long executionId) throws NoSuchJobExecutionException, JobSecurityException {

        if (authService != null) {
            authService.authorizedExecutionRead(executionId);
        }

        return getPersistenceManagerService().getJobInstanceFromExecutionId(executionId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getJobInstanceCount(String jobName) throws NoSuchJobException, JobSecurityException {

        int jobInstanceCount = 0;

        if (authService == null || authService.isAdmin() || authService.isMonitor()) {
            // Do an unfiltered query if no app security, or if the user is admin or monitor
            jobInstanceCount = getPersistenceManagerService().getJobInstanceCount(jobName);
        } else if (authService.isSubmitter()) {
            jobInstanceCount = getPersistenceManagerService().getJobInstanceCount(jobName, batchSecurityHelper.getRunAsUser());
        } else {
            throw new JobSecurityException("Current user " + authService.getRunAsUser() + " is not authorized to perform batch operations");
        }

        // Debate on mailing list if we should be reqiured to distinguish between 0 count and NoSuchJobException.
        if (jobInstanceCount == 0) {
            validateJobName(jobName);
        }

        return jobInstanceCount;
    }

    /**
     * Throws NoSuchJobException if and only if jobName is not in the Set returned
     * by getJobNames().
     *
     * Not crazy about having to do this, but the TCK forces us to (unless we want
     * to go back to all the people who have already dealt with this and argue we
     * should ignore these tests simply because we're running up against it now
     * and don't like the behavior).
     *
     * Would like to reconsider in 1.1.
     *
     * @param jobName
     * @throws NoSuchJobException
     */
    private void validateJobName(String jobName) throws NoSuchJobException {
        if (!getJobNames().contains(jobName)) {
            logger.fine("Job Name " + jobName + " not found");
            throw new NoSuchJobException("Job Name " + jobName + " not found");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<JobInstance> getJobInstances(String jobName, int start,
                                             int count) throws NoSuchJobException, JobSecurityException {

        batchRoleCheck();

        List<JobInstanceEntity> jobInstances = new ArrayList<JobInstanceEntity>();

        if (count == 0) {
            return new ArrayList<JobInstance>();
        } else if (count < 0) {
            throw new IllegalArgumentException("Count should be a positive integer (or 0, which will return an empty list)");
        }

        if (authService == null || authService.isAdmin() || authService.isMonitor()) {
            // Do an unfiltered query
            jobInstances = getPersistenceManagerService().getJobInstances(jobName, start, count);
        } else if (authService.isSubmitter()) {
            jobInstances = getPersistenceManagerService().getJobInstances(jobName, batchSecurityHelper.getRunAsUser(), start, count);
        } else {
            throw new JobSecurityException("Current user " + authService.getRunAsUser() + " is not authorized to perform batch operations");
        }

        // Some debate on the mailing list whether and how an implementation should be
        // required to distinguish between an "unknown" job resulting in NoSuchJobException and
        // a "known" job for which there happen to be zero instances (hence an empty list
        // return value).
        if (jobInstances.size() == 0) {
            try {
                validateJobName(jobName);
            } catch (NoSuchJobException e) { //FFDC instrumentation
                throw e;
            }
        }
        return new ArrayList<JobInstance>(jobInstances);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getJobNames() throws JobSecurityException {

        batchRoleCheck(); // Check the user before bothering querying the DB

        if (authService == null || authService.isAdmin() || authService.isMonitor()) {
            // Do an unfiltered query
            return getPersistenceManagerService().getJobNamesSet();
        } else if (authService.isSubmitter()) {
            return getPersistenceManagerService().getJobNamesSet(batchSecurityHelper.getRunAsUser());
        } else {
            throw new JobSecurityException("Current user " + authService.getRunAsUser() + " is not authorized to perform batch operations");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Properties getParameters(long executionId) throws NoSuchJobExecutionException, JobSecurityException {

        if (authService != null) {
            authService.authorizedExecutionRead(executionId);
        }

        return getPersistenceManagerService().getJobExecutionParameters(executionId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Long> getRunningExecutions(String jobName) throws NoSuchJobException, JobSecurityException {

        batchRoleCheck();

        List<Long> authorizedRunningExecutions = new ArrayList<Long>();

        // get the jobexecution ids associated with this job name
        List<Long> allRunningExecutions = getPersistenceManagerService().getJobExecutionsRunning(jobName);

        for (long id : allRunningExecutions) {
            try {
                logger.finer("Examining executionId: " + id);
                if (authService == null || authService.isAuthorizedInstanceRead(getPersistenceManagerService().getJobInstanceIdFromExecutionId(id))) {
                    JobExecution jobEx = getPersistenceManagerService().getJobExecution(id);
                    authorizedRunningExecutions.add(jobEx.getExecutionId());
                } else {
                    logger.finer("Don't have authorization for executionId: " + id);
                }
            } catch (NoSuchJobExecutionException e) {
                String errorMsg = "Just found execution with id = " + id + " in table, but now seeing it as gone";
                throw new IllegalStateException(errorMsg, e);
            }
        }

        if (authorizedRunningExecutions.size() == 0) {
            validateJobName(jobName);
        }

        return authorizedRunningExecutions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<StepExecution> getStepExecutions(long jobExecutionId) throws NoSuchJobExecutionException, JobSecurityException {

        if (authService != null) {
            authService.authorizedExecutionRead(jobExecutionId);
        }

        JobExecutionEntity jobEx = getPersistenceManagerService().getJobExecution(jobExecutionId);

        List<StepExecution> stepExecutions = new ArrayList<StepExecution>();
        stepExecutions.addAll(getPersistenceManagerService().getStepExecutionsTopLevelFromJobExecutionId(jobExecutionId));

        return stepExecutions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @FFDCIgnore({ JobExecutionAlreadyCompleteException.class,
                  NoSuchJobExecutionException.class,
                  JobExecutionNotMostRecentException.class,
                  JobSecurityException.class })
    public long restart(long oldExecutionId,
                        Properties restartParameters) throws JobExecutionAlreadyCompleteException, NoSuchJobExecutionException, JobExecutionNotMostRecentException, JobRestartException, JobSecurityException {
        batchRoleCheck();

        /*
         * The whole point of this method is to have JobRestartException serve as a blanket exception for anything other
         * than the rest of the more specific exceptions declared on the throws clause. So we won't log but just rethrow.
         */
        long retVal = 0L;
        try {
            retVal = restartInternal(oldExecutionId, restartParameters);
        } catch (JobExecutionAlreadyCompleteException e) {
            throw e;
        } catch (NoSuchJobExecutionException e) {
            throw e;
        } catch (JobExecutionNotMostRecentException e) {
            throw e;
        } catch (JobSecurityException e) {
            markInstanceExecutionFailed(oldExecutionId, this.getCorrelationId(restartParameters));
            throw e;
        } catch (Exception e) {
            markInstanceExecutionFailed(oldExecutionId, this.getCorrelationId(restartParameters));
            throw new JobRestartException(e);
        }

        return retVal;
    }

//	public void purgeButOnlyFromWithinGlassfish(String submitter) {
//		BatchSecurityHelper bsh = getBatchSecurityHelper();
//		if (isCurrentTagAdmin(bsh)) {
//			logger.finer("Current tag is admin, so authorized to purge.");
//			getPersistenceManagerService().purgeInGlassfish(submitter);
//		} else if (bsh.getRunAsUser().equals(submitter)) {
//			logger.finer("Current tag is the tag of record so authorized to purge.");
//			getPersistenceManagerService().purgeInGlassfish(submitter);
//		} else {
//			logger.finer("Current tag does not match the tag of record so will not purge.");
//		}
//	}

    /**
     * Restart the given execution using the given jobparams.
     *
     * @return execId of restarted job.
     */
    private long restartInternal(long oldExecutionId,
                                 Properties restartParameters) throws JobExecutionAlreadyCompleteException, NoSuchJobExecutionException, JobExecutionNotMostRecentException, JobRestartException, JobSecurityException {

        if (authService != null) {
            authService.authorizedJobRestartByExecution(oldExecutionId);
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("JobOperator restart, with old executionId = " + oldExecutionId + "\n" + traceJobParameters(restartParameters));
        }

        //Check if there are no job executions and step executions running.
        BatchStatusValidator.validateStatusAtExecutionRestart(oldExecutionId, restartParameters);
        //Set instance state to submitted to be consistent with start
        long instanceId = getPersistenceManagerService().getJobInstanceIdFromExecutionId(oldExecutionId);
        getPersistenceManagerService().updateJobInstanceOnRestart(instanceId, new Date());

        WSJobExecution jobExecution = getPersistenceManagerService().createJobExecution(instanceId, restartParameters, new Date());
        long newExecutionId = getBatchKernelService().restartJob(jobExecution.getExecutionId(), restartParameters).getKey();

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Restarted job with new executionId: " + newExecutionId + ", and old executionID: " + oldExecutionId);
        }

        return newExecutionId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop(long executionId) throws NoSuchJobExecutionException, JobExecutionNotRunningException, JobSecurityException {
        if (authService != null) {
            authService.authorizedJobStopByExecution(executionId);
        }

        getBatchKernelService().stopJob(executionId);
    }

//	private boolean isCurrentTagAdmin(BatchSecurityHelper helper) {
//		if (authService != null) {
//		    return authService.isAdmin();
//		}
//		return true;
//	}

//	public BatchSecurityHelper getBatchSecurityHelper() {
//		if (batchSecurityHelper == null) {
//			// Preserving this SPIManager code for testing purposes.
//			BatchSecurityHelper retMe = BatchSPIManager.getInstance().getBatchSecurityHelper();
//			return (retMe != null) ? retMe : new NoOpBatchSecurityHelper();
//		}
//		return batchSecurityHelper;
//	}

    /**
     * If app security is enabled we just do a quick check to make sure the user is part of some batch role.
     * Otherwise we throw a JobSecurityException immediately.
     */
    private void batchRoleCheck() {
        if (authService != null) {
            if (!authService.isInAnyBatchRole()) {
                throw new JobSecurityException("Current user " + authService.getRunAsUser() + " is not authorized to perform batch operations");
            }
        }
    }

}
