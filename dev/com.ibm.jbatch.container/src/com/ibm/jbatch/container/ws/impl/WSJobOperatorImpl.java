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

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.operations.JobExecutionAlreadyCompleteException;
import javax.batch.operations.JobExecutionNotMostRecentException;
import javax.batch.operations.JobExecutionNotRunningException;
import javax.batch.operations.JobRestartException;
import javax.batch.operations.JobSecurityException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.operations.NoSuchJobInstanceException;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.jbatch.container.services.IBatchKernelService;
import com.ibm.jbatch.container.services.IJobXMLSource;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.services.impl.JobXMLSource;
import com.ibm.jbatch.container.util.BatchPartitionWorkUnit;
import com.ibm.jbatch.container.ws.PartitionPlanConfig;
import com.ibm.jbatch.container.ws.PartitionReplyQueue;
import com.ibm.jbatch.container.ws.WSBatchAuthService;
import com.ibm.jbatch.container.ws.WSJobInstance;
import com.ibm.jbatch.container.ws.WSJobOperator;
import com.ibm.jbatch.container.ws.events.BatchEventsPublisher;
import com.ibm.jbatch.jsl.model.Step;
import com.ibm.jbatch.spi.BatchSecurityHelper;
import com.ibm.jbatch.spi.services.IJobXMLLoaderService;

/**
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
                property = { "service.vendor=IBM" })
public class WSJobOperatorImpl implements WSJobOperator {

    private final static String sourceClass = WSJobOperatorImpl.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);

    private IPersistenceManagerService persistenceManagerService;
    private IBatchKernelService batchKernelService;
    private WSBatchAuthService authService;

    /**
     * For publishing job event
     */
    private BatchEventsPublisher eventsPublisher;

    private BatchSecurityHelper batchSecurityHelper = null;

    private IJobXMLLoaderService jslLoaderService;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL,
                    policy = ReferencePolicy.DYNAMIC,
                    policyOption = ReferencePolicyOption.GREEDY)
    protected void setWSBatchAuthService(WSBatchAuthService bas) {
        this.authService = bas;
    }

    @Reference(policyOption = ReferencePolicyOption.GREEDY)
    protected void setIPersistenceManagerService(IPersistenceManagerService ref) {
        this.persistenceManagerService = ref;
    }

    @Reference
    protected void setIJobXMLLoaderService(IJobXMLLoaderService ref) {
        this.jslLoaderService = ref;
    }

    @Reference()
    protected void setIBatchKernelService(IBatchKernelService bks) {
        this.batchKernelService = bks;
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

    protected void unsetIJobXMLLoaderService(IJobXMLLoaderService ref) {
        if (this.jslLoaderService == ref) {
            this.jslLoaderService = null;
        }
    }

    protected void unsetIBatchKernelService(IBatchKernelService bks) {
        if (this.batchKernelService == bks) {
            this.batchKernelService = null;
        }
    }

    /**
     * DS injection
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL,
                    policy = ReferencePolicy.DYNAMIC,
                    policyOption = ReferencePolicyOption.GREEDY)
    protected void setEventsPublisher(BatchEventsPublisher publisher) {
        eventsPublisher = publisher;
    }

    protected void unsetEventsPublisher(BatchEventsPublisher publisher) {
        if (eventsPublisher == publisher)
            eventsPublisher = null;
    }

    /**
     * @param appName
     * @param jobXMLName
     * 
     * @return newly created JobInstance (Note: job instance must be started separately)
     * 
     *         Note: Inline JSL takes precedence over JSL within .war
     */
    @Override
    public WSJobInstance createJobInstance(String appName, String jobXMLName, String jsl, String correlationId) {

        if (authService != null) {
            authService.authorizedJobSubmission();
        }

        return batchKernelService.createJobInstance(appName, jobXMLName, batchSecurityHelper.getRunAsUser(), jsl, correlationId);

    }

    /**
     * {@inheritDoc}
     * 
     * Note: this method must be called while running under the app's context.
     */
    @Override
    public Entry<Long, Future<?>> start(WSJobInstance jobInstance, Properties jobParameters, long execId) {

        if (authService != null) {
            authService.authorizedJobSubmission();
        }

        traceJobStart(jobInstance.getJobXMLName(), jobParameters);

        IJobXMLSource jobXML;
        if (!StringUtils.isEmpty(jobInstance.getJobXml())) {
            jobXML = new JobXMLSource(jobInstance.getJobXml());
        } else {
            jobXML = jslLoaderService.loadJSL(jobInstance.getJobXMLName());
        }

        Entry<Long, Future<?>> execIdFutureEntry = batchKernelService.startJob(jobInstance, jobXML, jobParameters, execId);

        long executionId = execIdFutureEntry.getKey();
        if (logger.isLoggable(Level.FINE) && executionId > 0) {
            logger.fine("Started job with instanceId: " + jobInstance.getInstanceId() + ", executionId: " + executionId);
        }

        return execIdFutureEntry;
    }

    /**
     * Trace job submission..
     */
    protected void traceJobStart(String jobXMLName, Properties jobParameters) {

        if (logger.isLoggable(Level.FINE)) {
            StringWriter jobParameterWriter = new StringWriter();
            if (jobParameters != null) {
                try {
                    jobParameters.store(jobParameterWriter, "Job parameters on start: ");
                } catch (IOException e) {
                    jobParameterWriter.write("Job parameters on start: not printable");
                }
            } else {
                jobParameterWriter.write("Job parameters on start = null");
            }

            logger.fine("Starting job: jobXMLName = " + jobXMLName + "\n" + jobParameterWriter.toString());
        }
    }

    /**
     * Trace job xml file..
     */
    protected void traceJobXML(String jobXML) {

        if (logger.isLoggable(Level.FINE)) {
            int concatLen = jobXML.length() > 200 ? 200 : jobXML.length();
            logger.fine("Starting job: " + jobXML.substring(0, concatLen) + "... truncated ...");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean purgeJobInstance(long jobInstanceId) throws JobSecurityException, NoSuchJobInstanceException {
        if (authService != null) {
            authService.authorizedJobPurgeByInstance(jobInstanceId);
        }

        //save this instance object to use in the publishEvent call.
        WSJobInstance jobInstance = persistenceManagerService.getJobInstance(jobInstanceId);

        boolean purgeSuccess = persistenceManagerService.purgeJobInstanceAndRelatedData(jobInstanceId);

        if (purgeSuccess) {
            publishPurgeEvent(jobInstance);
        }
        return purgeSuccess;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Entry<Long, Future<?>> restartInstance(long instanceId, Properties restartParameters, long executionId)
                    throws JobExecutionAlreadyCompleteException,
                    NoSuchJobExecutionException, JobExecutionNotMostRecentException,
                    JobRestartException, JobSecurityException {

        if (authService != null) {
            authService.authorizedJobRestartByInstance(instanceId);
        }

        WSJobInstance jobInstance = persistenceManagerService.getJobInstance(instanceId);

        IJobXMLSource jobXML;
        if (!StringUtils.isEmpty(jobInstance.getJobXml())) {
            jobXML = new JobXMLSource(jobInstance.getJobXml());
        } else {
            jobXML = jslLoaderService.loadJSL(jobInstance.getJobXMLName());
        }

        Entry<Long, Future<?>> execIdFutureEntry = batchKernelService.restartJobInstance(instanceId, jobXML, restartParameters, executionId);

        return execIdFutureEntry;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop(long executionId) throws NoSuchJobExecutionException,
                    JobExecutionNotRunningException, JobSecurityException {

        if (authService != null) {
            authService.authorizedJobStopByExecution(executionId);
        }
        batchKernelService.stopJob(executionId);
    }

    /**
     * Publish events
     * 
     * @param jobInstance
     * @param topicToPublish
     */
    private void publishPurgeEvent(WSJobInstance jobInstance) {
        if (eventsPublisher != null) {
            eventsPublisher.publishJobInstanceEvent(jobInstance, BatchEventsPublisher.TOPIC_INSTANCE_PURGED, null);
        }
    }

    /**
     * Start a partition sub-job. This method is called when the partition is being started
     * on a remote executor via JMS (multi-jvm partitions).
     */
    @Override
    public Future<?> startPartition(PartitionPlanConfig partitionPlanConfig,
                                    Step step,
                                    PartitionReplyQueue partitionReplyQueue) {
        //This partition was dispatched remotely. Hence, param isRemoteDispatch should be true
        Entry<BatchPartitionWorkUnit, Future<?>> workUnitFutureEntry = batchKernelService.startPartition(partitionPlanConfig, step, partitionReplyQueue, true);
        return workUnitFutureEntry.getValue();
    }
}
