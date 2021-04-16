/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.rest.bridge;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;

import javax.batch.operations.JobExecutionAlreadyCompleteException;
import javax.batch.operations.JobExecutionNotMostRecentException;
import javax.batch.operations.JobExecutionNotRunningException;
import javax.batch.operations.JobRestartException;
import javax.batch.operations.JobSecurityException;
import javax.batch.operations.JobStartException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.runtime.BatchStatus;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;
import com.ibm.jbatch.container.ws.BatchInternalDispatcher;
import com.ibm.jbatch.container.ws.BatchJobNotLocalException;
import com.ibm.jbatch.container.ws.BatchLocationService;
import com.ibm.jbatch.container.ws.InstanceState;
import com.ibm.jbatch.container.ws.PartitionPlanConfig;
import com.ibm.jbatch.container.ws.PartitionReplyQueue;
import com.ibm.jbatch.container.ws.WSJobExecution;
import com.ibm.jbatch.container.ws.WSJobInstance;
import com.ibm.jbatch.container.ws.WSJobOperator;
import com.ibm.jbatch.container.ws.WSJobRepository;
import com.ibm.jbatch.jsl.model.Step;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * Dispatches batch jobs locally (i.e the batch app lives in the same server).
 *
 * Constructs the proper application runtime context (ComponentMetaData, ClassLoader)
 * in order to dispatch into the app.
 *
 * Methods return a Future object represented the newly started jobs or partitions
 */
@Component(service = BatchInternalDispatcher.class,
		   configurationPolicy = ConfigurationPolicy.IGNORE,
		   property = { "service.vendor=IBM" })
public class BatchInternalDispatcherImpl implements BatchInternalDispatcher {

	/**
	 * JobManager for internal use from REST layer
	 */
	private WSJobOperator wsJobOperator;

	/**
	 * Batch persistence layer.
	 */
	private WSJobRepository jobRepository;

	/**
	 * For retrieving the CMD and classloader context for a given
	 * app/module name.
	 */
	private AppModuleContextService appModuleContextService;

	/**
	 * For creating J2EENames.
	 */
	private J2EENameFactory j2eeNameFactory;

	/**
	 * For checking whether jobexecutions ran locally.
	 */
	private BatchLocationService batchLocationService;

	/**
	 * This is the value of javax.enterprise.concurrent.ManagedTask.IDENTITY_NAME,
	 * but is hard-coded here to avoid a dependency on the concurrency feature.
	 */
	private static final String MANAGEDTASK_IDENTITY_NAME = "javax.enterprise.concurrent.IDENTITY_NAME";


	private static final String ENDPOINT_EXECUTION_WAIT = "com.ibm.ws.jbatch.internal.test.endpoint.execution.wait";
	private static boolean triggerEndpointExecutionWait = false;

	static {
		triggerEndpointExecutionWait = Boolean.getBoolean(ENDPOINT_EXECUTION_WAIT);
	}


	/**
	 * Sets the job manager reference.
	 *
	 * @param ref The job manager to associate.
	 *
	 */
	@Reference
	protected void setWSJobOperator(WSJobOperator ref) {
		this.wsJobOperator = ref;
	}

	/**
	 * DS injection
	 */
	@Reference
	protected void setAppModuleContextService(AppModuleContextService appModuleContextService) {
		this.appModuleContextService = appModuleContextService;
	}

	/**
	 * DS injection
	 *
	 * @param ref The WSJobRepository to associate.
	 *
	 */
	@Reference
	protected void setWSJobRepository(WSJobRepository ref) {
		this.jobRepository = ref;
	}

	/**
	 * DS injection
	 */
	@Reference
	protected void setJ2EENameFactory(J2EENameFactory j2eeNameFactory) {
		this.j2eeNameFactory = j2eeNameFactory;
	}

	/**
	 * DS injection
	 */
	@Reference
	protected void setBatchLocationService(BatchLocationService batchLocationService) {
		this.batchLocationService = batchLocationService;
	}

	/**
	 * @return a J2EEName for the given app / module / comp.
	 */
	protected J2EEName createJ2EEName(String j2eeName) {
		return j2eeNameFactory.create( j2eeName.getBytes(StandardCharsets.UTF_8) );
	}

	/**
	 * Bridge into the given app's runtime context and start the job.
	 *
	 * @param jobInstance
	 * @param jobParameters
	 *
	 */
	@Override
	public Future<?> start(WSJobInstance jobInstance, Properties jobParameters, long executionId)
			throws JobStartException, JobSecurityException {

		// Needed for test
		if (triggerEndpointExecutionWait) {
			try {
				Thread.sleep(10 * 1000);
			} catch (InterruptedException e) {}
		}

		J2EEName j2eeName = createJ2EEName( jobInstance.getAmcName() );

		// Create a proxy that will run under the given app's runtime context (CMD/classloader)
		WSJobOperator proxy = createProxyForAppContext(j2eeName, wsJobOperator, WSJobOperator.class);

		return proxy.start(jobInstance, jobParameters, executionId).getValue();
	}

	/**
	 * If couldn't start instance then mark instance/execution FAILED.
	 * Example: Maybe couldn't locate application contents (JSL).
	 */
	@Override
	public void markInstanceExecutionFailed(long instanceId, long executionId ){

		jobRepository.updateJobInstanceAndExecutionWithInstanceStateAndBatchStatus(instanceId, executionId, InstanceState.FAILED, BatchStatus.FAILED);

	}

	/**
	 * @return the job execution.
	 */
	@Override
	public WSJobExecution getJobExecution(long executionId){

		return jobRepository.getJobExecution(executionId);

	}

	/**
	 * @return the job instance.
	 */
	@Override
	public WSJobInstance getJobInstance(long instanceId){

		return jobRepository.getJobInstance(instanceId);

	}

	/**
	 * Lookup the app associated with the given executionId, then bridge into
	 * the app's runtime context and restart the job.
	 */
	@Override
	public Future<?> restartInstance(long instanceId, Properties restartParameters, long executionId)
			throws JobExecutionAlreadyCompleteException,
				   NoSuchJobExecutionException,
				   JobExecutionNotMostRecentException,
				   JobRestartException,
				   JobSecurityException {

		J2EEName j2eeName = createJ2EEName(jobRepository.getJobInstance(instanceId).getAmcName());

		// Create a proxy that will run under the given app's runtime context (CMD/classloader)
		WSJobOperator proxy = createProxyForAppContext(j2eeName, wsJobOperator, WSJobOperator.class);
		return proxy.restartInstance(instanceId, restartParameters, executionId).getValue();
	}

	/**
	 * Lookup the app associated with the given executionId, then bridge into
	 * the app's runtime context and stop the job.
	 * @throws BatchJobNotLocalException
	 */
	@Override
	public void stop(long executionId) throws NoSuchJobExecutionException,
			JobExecutionNotRunningException, JobSecurityException, BatchJobNotLocalException {

		batchLocationService.assertIsLocalJobExecution(executionId) ;

		J2EEName j2eeName = createJ2EEName( jobRepository.getBatchAppNameFromExecution(executionId) );

		// Create a proxy that will run under the given app's runtime context (CMD/classloader)
		WSJobOperator proxy = createProxyForAppContext(j2eeName, wsJobOperator, WSJobOperator.class);
		proxy.stop(executionId);
	}

	/**
	 * Use WSContextService to wrap a contextual proxy around the given instance.
	 * The proxy propagates the current security context from this thread along
	 * with the jee-metadata and classloader contexts of the given app component (j2eeName).
	 *
	 * @return a contextual proxy for the given app component (j2eeName)
	 */
	private <T> T createProxyForAppContext(J2EEName j2eeName, T instance, Class<T> intf) {

		Map<String, String> execProps = new HashMap<String, String>();

		// TaskIdentity identifies the task for the purposes of mgmt/auditing.
		execProps.put(MANAGEDTASK_IDENTITY_NAME, "batch.request.app.proxy");

		// TaskOwner identifies the submitter of the task.
		execProps.put(WSContextService.TASK_OWNER, "batch.runtime");

		try {
			return appModuleContextService.createContextualProxy(execProps, j2eeName, instance, intf);
		} catch (IllegalStateException ise) {
			throw new BatchContainerAppNotFoundException(j2eeName.toString(),
														 "Failed to load the application context for application "
														 + j2eeName.toString() + ". Verify the application is installed.", ise);
		} catch (Exception e) {
			throw new BatchContainerRuntimeException("Failed to load the application context for application "
													 + j2eeName.toString() + ". Verify the application is installed.", e);
		}
	}

	/**
	 * Start a sub-job partition thread.
	 *
	 * This method is called only for multi-JVM partitions, which are started
	 * remotely by the top-level thread via JMS.
	 */
	@Override
	public Future<?> startPartition( PartitionPlanConfig partitionPlanConfig,
								Step step,
								PartitionReplyQueue partitionReplyQueue) {

		J2EEName j2eeName = createJ2EEName( jobRepository.getBatchAppNameFromExecution(partitionPlanConfig.getTopLevelExecutionId()) );

		// Create a proxy that will run under the given app's runtime context (CMD/classloader)
		WSJobOperator proxy = createProxyForAppContext(j2eeName, wsJobOperator, WSJobOperator.class);

		return proxy.startPartition( partitionPlanConfig, step, partitionReplyQueue );

	}

}
