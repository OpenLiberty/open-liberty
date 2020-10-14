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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.operations.JobSecurityException;
import javax.batch.runtime.BatchRuntime;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.jbatch.container.ws.BatchDispatcherException;
import com.ibm.jbatch.container.ws.BatchJobNotLocalException;
import com.ibm.jbatch.container.ws.WSJobExecution;
import com.ibm.jbatch.container.ws.WSJobInstance;
import com.ibm.jbatch.container.ws.WSJobRepository;
import com.ibm.jbatch.container.ws.WSStepThreadExecutionAggregate;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jbatch.rest.BatchManager;
import com.ibm.ws.jbatch.rest.JPAQueryHelperImpl;
import com.ibm.ws.jbatch.rest.utils.BatchJSONHelper;
import com.ibm.ws.jbatch.rest.utils.JobRestartModel;
import com.ibm.ws.jbatch.rest.utils.JobSubmissionModel;
import com.ibm.ws.jbatch.rest.utils.PurgeStatus;
import com.ibm.ws.jbatch.rest.utils.StringUtils;
import com.ibm.ws.jbatch.rest.utils.WSPurgeResponse;
import com.ibm.ws.jbatch.rest.utils.WSSearchObject;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaDataSlot;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

/**
 * BatchWolaListener is the target object that batch WOLA clients (e.g. the
 * batchManagerZos native client) send their requests to.
 *
 * BatchWolaListener is registered with JNDI under a well-known fixed name. The
 * JNDI name is what WOLA clients use to lookup and invoke requests on the
 * listener.
 *
 * BatchWolaListener parses the client request and forwards it on to
 * BatchManagerImpl.
 * 
 * @Component(configurationPolicy = ConfigurationPolicy.IGNORE)
 */
@Component(service = BatchWolaListener.class, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true /*
																													 * ,
																													 * properties
																													 * =
																													 * {
																													 * "osgi.jndi.service.name=BatchWolaListener"
																													 * }
																													 */ ) // TODO:
																															// this
																															// supposedly
																															// auto-registers
																															// the
																															// service
																															// with
																															// jndi
public class BatchWolaListener {

	private static final Logger logger = Logger.getLogger(BatchWolaListener.class.getName(),
			"com.ibm.ws.jbatch.wola.internal.resources.BatchWolaMessages");

	/**
	 * The listener's JNDI name (needed by clients). It's the same as the class
	 * name, sans the "internal" package.
	 */
	public static final String JndiName = "com.ibm.ws.jbatch.wola.BatchWolaListener";

	/**
	 * The guts of batch job management.
	 */
	private BatchManager batchManager;

	/**
	 * For getting job data from the DB.
	 */
	private WSJobRepository jobRepository;

	/**
	 * DS injection.
	 * 
	 * Note: The dependency is required; however we mark it OPTIONAL to ensure that
	 * BatchWolaListener is started even if the batch container didn't, so we can
	 * respond with useful error messages instead of "object not found" JNDI lookup
	 * failures.
	 */
	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
	protected void setBatchManager(BatchManager ref) {
		this.batchManager = ref;
	}

	/**
	 * DS un-set.
	 */
	protected void unsetBatchManager(BatchManager ref) {
		if (this.batchManager == ref) {
			this.batchManager = null;
		}
	}

	/**
	 * DS injection
	 * 
	 * @param ref The WSJobRepository to associate.
	 *
	 *            Note: The dependency is required; however we mark it OPTIONAL to
	 *            ensure that the BatchWolaListener is started even if the batch
	 *            container didn't, so we can respond with useful error messages
	 *            instead of "object not found" JNDI lookup failures.
	 */
	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
	protected void setWSJobRepository(WSJobRepository ref) {
		this.jobRepository = ref;
	}

	/**
	 * DS un-set.
	 */
	protected void unsetWSJobRepository(WSJobRepository ref) {
		if (this.jobRepository == ref) {
			this.jobRepository = null;
		}
	}

	/**
	 * Bind to JNDI.
	 */
	@Activate
	protected void activate(ComponentContext context, Map<String, Object> config) {

		try {
			jndiBind(JndiName);

			if (jndiLookup(JndiName) != this) {
				throw new RuntimeException("BatchWolaListener is not bound in JNDI: " + jndiLookup(JndiName));
			}

			logger.log(Level.INFO, "batch.wola.listener.activation", JndiName);

		} catch (NamingException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * Un-bind from JNDI.
	 */
	@Deactivate
	@FFDCIgnore(javax.naming.NamingException.class)
	protected void deactivate() {

		try {
			jndiUnbind(JndiName);

			logger.log(Level.INFO, "batch.wola.listener.deactivation", JndiName);

		} catch (NamingException e) {
			// This might fail if we're shutting down with a NoInitialContextException.
			// No need to FFDC or propagate the exception.
		}

	}

	/**
	 * Lazy-created because we're not guaranteed in activate() that
	 * batchManager/jobRepository have been injected yet.
	 * 
	 * @return BatchManagerHelper
	 */
	protected BatchManagerHelper getBatchManagerHelper() {
		return new BatchManagerHelper(batchManager, jobRepository);
	}

	/**
	 * WOLA requests invoke this method. The payload contains raw parameter data.
	 * This method parses the parms from the payload and forward the request to the
	 * BatchManager.
	 * 
	 * @param payload raw parm data
	 * 
	 * @return TODO: work out response payload format. JSON data would be nice,
	 *         although I'd have to parse it in native code.
	 */
	public byte[] execute(byte[] payloadBytes) {

		try {
			// First verify the batch container is started.
			BatchRuntime.getJobOperator();

			// Parse the JSON request payload
			JsonObject payload = BatchWolaJsonHelper.readJsonRequest(payloadBytes);

			// Run the requested command
			JsonObject responseMessage = runCommand(payload);

			// Write back the response.
			return BatchWolaJsonHelper.writeJsonResponse(responseMessage);

		} catch (Exception e) {

			return BatchWolaJsonHelper.writeJsonErrorResponseMessage(e.getMessage());
		}
	}

	/**
	 * 
	 * @return a Json response message
	 * @throws BatchJobNotLocalException
	 * @throws BatchDispatcherException
	 * @throws JobSecurityException
	 * 
	 * @throws IllegalArgumentException  if the command is unrecognized.
	 */
	protected JsonObject runCommand(JsonObject request)
			throws JobSecurityException, BatchDispatcherException, BatchJobNotLocalException {

		String command = request.getString("command");

		if ("ping".equalsIgnoreCase(command)) {
			return pingCommand(request);
		} else if ("submit".equalsIgnoreCase(command)) {
			return submitCommand(request);
		} else if ("restart".equalsIgnoreCase(command)) {
			return restartCommand(request);
		} else if ("stop".equalsIgnoreCase(command)) {
			return stopCommand(request);
		} else if ("purge".equalsIgnoreCase(command)) {
			return purgeCommand(request);
		} else if ("listJobs".equalsIgnoreCase(command)) {
			return listJobsCommand(request);
		} else if ("getJobInstance".equalsIgnoreCase(command)) {
			return getJobInstanceCommand(request);
		} else if ("getJobExecution".equalsIgnoreCase(command)) {
			return getJobExecutionCommand(request);
		} else if ("getJobExecutions".equalsIgnoreCase(command)) {
			return getJobExecutionsCommand(request);
		} else {
			throw new IllegalArgumentException("Unrecognized command: " + command);
		}

	}

	/**
	 * List job instances based on a set of query parameters
	 * 
	 * @param request The Json request message from the native client
	 * 
	 * @return a Json response message
	 */
	protected JsonObject listJobsCommand(JsonObject request) {

		JsonArrayBuilder jsonResponseBuilder = Json.createArrayBuilder();
		BatchWolaRequest wolaRequest = new BatchWolaRequest(request);
		WSSearchObject wsso = null;

		// Check for multi-pre-purge query
		if (wolaRequest.getInstanceId() != null || wolaRequest.getCreateTime() != null
				|| wolaRequest.getInstanceState() != null || wolaRequest.getExitStatus() != null) {

			try {
				wsso = new WSSearchObject(wolaRequest.getInstanceId(), wolaRequest.getCreateTime(),
						wolaRequest.getInstanceState(), wolaRequest.getExitStatus());
			} catch (Exception e) {
				/*
				 * In the java-only (batchManager) client, it uses the rest API and thus a
				 * RequestException is thrown at that point in the flow. Need to handle it
				 * differently here since we're sending a response message back to the native
				 * client
				 */
				return BatchWolaJsonHelper
						.buildJsonErrorResponseMessage("An error occurred while processing the specified parameters");
			}

			List<WSJobInstance> jobInstances = jobRepository.getJobInstances(new JPAQueryHelperImpl(wsso),
					wolaRequest.getPage(), wolaRequest.getPageSize());

			for (WSJobInstance jobInstance : jobInstances)
				jsonResponseBuilder.add(BatchJSONHelper.toJsonObjectBuilder(jobInstance));

		} else { // query without instance range specified

			try {
				// new up a search object that will do a base query sorted by descending create
				// time
				wsso = new WSSearchObject(null, null, null, null, null, "-createTime", null);
			} catch (Exception e) {
				/*
				 * In the java-only (batchManager) client, it uses the rest API and thus a
				 * RequestException is thrown at that point in the flow. Need to handle it
				 * differently here since we're sending a response message back to the native
				 * client
				 */
				return BatchWolaJsonHelper
						.buildJsonErrorResponseMessage("An error occurred while processing the specified parameters");
			}
			List<WSJobInstance> jobInstances = jobRepository.getJobInstances(new JPAQueryHelperImpl(wsso),
					wolaRequest.getPage(), wolaRequest.getPageSize());

			for (WSJobInstance jobInstance : jobInstances)
				jsonResponseBuilder.add(BatchJSONHelper.toJsonObjectBuilder(jobInstance));
		}

		return BatchWolaJsonHelper.buildJsonArrayResponseMessage(jsonResponseBuilder.build());
	}

	/**
	 * Get the JobExecution list for the given job instanceId.
	 */
	protected JsonObject getJobExecutionsCommand(JsonObject request) {

		JsonArrayBuilder jsonResponseBuilder = Json.createArrayBuilder();

		List<WSJobExecution> jobExecutions = getBatchManagerHelper().getJobExecutions(
				BatchWolaJsonHelper.parseLong(request, "instanceId", -1),
				BatchWolaJsonHelper.parseLong(request, "executionId", -1));
		List<WSJobExecution> jobExecutionList = new ArrayList<WSJobExecution>();
		Map<Long, List<WSStepThreadExecutionAggregate>> jobExecStepExecListMap = new HashMap<Long, List<WSStepThreadExecutionAggregate>>();
		for (WSJobExecution jobExec : jobExecutions) {
			jobExecutionList.add(jobExec);
			jobExecStepExecListMap.put(jobExec.getExecutionId(),
					jobRepository.getStepExecutionAggregatesFromJobExecution(jobExec.getExecutionId()));
			jsonResponseBuilder.add(
					BatchJSONHelper.toJsonObjectBuilder(jobExec, jobExecStepExecListMap.get(jobExec.getExecutionId())));
		}

		return BatchWolaJsonHelper.buildJsonArrayResponseMessage(jsonResponseBuilder.build());
	}

	/**
	 * Get the JobExecution with the given executionId.
	 */
	protected JsonObject getJobExecutionCommand(JsonObject request) {

		long jobExecutionId = BatchWolaJsonHelper.parseLong(request, "executionId", -1);

		JsonObject jsonObject = BatchJSONHelper.toJsonObjectBuilder(jobRepository.getJobExecution(jobExecutionId),
				jobRepository.getStepExecutionAggregatesFromJobExecution(jobExecutionId)).build();

		return BatchWolaJsonHelper.buildJsonObjectResponseMessage(jsonObject);
	}

	/**
	 * This one's easy.
	 */
	protected JsonObject pingCommand(JsonObject request) {

		// Optionally throw an exception to test the error paths.
		if (request.getString("throwException", null) != null) {
			throw new RuntimeException("Ping Exception");
		}

		return BatchWolaJsonHelper.buildJsonObjectResponseMessage(request);
	}

	/**
	 * Submit a purge request to the BatchManager for a single job instance, or a
	 * set of job instances based on query parameters
	 * 
	 * @param request The Json request message from the native client
	 * 
	 * @return a Json response message
	 */
	protected JsonObject purgeCommand(JsonObject request) {

		BatchWolaRequest wolaRequest = new BatchWolaRequest(request);

		// Check for multi-purge
		if ((wolaRequest.getInstanceId() != null && !isNumeric(wolaRequest.getInstanceId()))
				|| wolaRequest.getCreateTime() != null || wolaRequest.getInstanceState() != null
				|| wolaRequest.getExitStatus() != null) {

			List<WSPurgeResponse> purgeResponseList = batchManager.purge(wolaRequest.getPurgeJobStoreOnly(),
					wolaRequest.getPage(), wolaRequest.getPageSize(), wolaRequest.getInstanceId(),
					wolaRequest.getCreateTime(), wolaRequest.getInstanceState(), wolaRequest.getExitStatus());

			JsonArrayBuilder jsonResponseBuilder = Json.createArrayBuilder();

			for (WSPurgeResponse response : purgeResponseList) {
				jsonResponseBuilder.add(BatchJSONHelper.toJsonObjectBuilder(response));
			}

			return BatchWolaJsonHelper.buildJsonArrayResponseMessage(jsonResponseBuilder.build());
		} else { // single purge

			WSPurgeResponse purgeResponse = batchManager.purge(Long.parseLong(wolaRequest.getInstanceId()),
					wolaRequest.getPurgeJobStoreOnly());

			if (purgeResponse.getPurgeStatus() == PurgeStatus.COMPLETED
					|| purgeResponse.getPurgeStatus() == PurgeStatus.JOBLOGS_ONLY) {
				JsonObjectBuilder jsonObjBuilder = Json.createObjectBuilder();
				jsonObjBuilder.add("response", purgeResponse.getMessage());
				return jsonObjBuilder.build();

			} else {
				return BatchWolaJsonHelper.buildJsonErrorResponseMessage(purgeResponse.getMessage());

			}
		}

	}

	/**
	 * Submit a job request to the BatchManager.
	 */
	protected JsonObject submitCommand(JsonObject request) {
		// If instance ID is present, should be treated as a restart
		if (request.getString("instanceId", null) != null) {
			return restartCommand(request);
		} else {

			JobSubmissionModel jobSubmission = new JobSubmissionModel(request);

			if (StringUtils.isEmpty(jobSubmission.getJobXMLName()) && StringUtils.isEmpty(jobSubmission.getJobXML())) {
				throw new IllegalArgumentException("Either jobXMLName or jobXML must be provided.");
			} else if (StringUtils.isEmpty(jobSubmission.getApplicationName())
					&& StringUtils.isEmpty(jobSubmission.getModuleName())) {
				throw new IllegalArgumentException("Either applicationName or moduleName must be provided.");
			}

			WSJobInstance jobInstance = batchManager.start(jobSubmission.getApplicationName(),
					jobSubmission.getModuleName(), jobSubmission.getComponentName(), jobSubmission.getJobXMLName(),
					jobSubmission.getJobParameters(), jobSubmission.getJobXML());

			return BatchWolaJsonHelper
					.buildJsonObjectResponseMessage(BatchJSONHelper.toJsonObjectBuilder(jobInstance).build());
		}

	}

	/**
	 * Restart a Job. Either an instanceId or executionId may be provided.
	 * 
	 * @return the JobInstance record (in a JSON response message).
	 */
	protected JsonObject restartCommand(JsonObject request) {

		JobRestartModel jobRestart = new JobRestartModel(request);

		// Read the jobParams from the request body, if any.
		// Merge with previous params if requested.
		Properties jobParams = (StringUtils.isEmpty(request.getString("reusePreviousParams", null)))
				? jobRestart.getJobParameters()
				: BatchJSONHelper.mergeProperties(getPreviousJobParameters(request), jobRestart.getJobParameters());

		WSJobInstance jobInstance = getBatchManagerHelper().restartJob(
				BatchWolaJsonHelper.parseLong(request, "instanceId", -1),
				BatchWolaJsonHelper.parseLong(request, "executionId", -1), jobParams);

		return BatchWolaJsonHelper
				.buildJsonObjectResponseMessage(BatchJSONHelper.toJsonObjectBuilder(jobInstance).build());
	}

	/**
	 * @return the job params from the previous execution of the job
	 */
	protected Properties getPreviousJobParameters(JsonObject request) {

		long instanceId = BatchWolaJsonHelper.parseLong(request, "instanceId", -1);
		if (instanceId >= 0) {
			// Note: assumes most recent execution is first in the list.
			return jobRepository.getJobExecutionsFromInstance(instanceId).get(0).getJobParameters();
		} else {
			return jobRepository.getJobExecution(BatchWolaJsonHelper.parseLong(request, "executionId", -1))
					.getJobParameters();
		}
	}

	/**
	 * Stop a Job. Either an instanceId or executionId may be provided.
	 * 
	 * @return the latest JobExecution record (in a JSON response message)
	 * @throws BatchJobNotLocalException
	 * @throws BatchDispatcherException
	 * @throws JobSecurityException
	 */
	protected JsonObject stopCommand(JsonObject request)
			throws JobSecurityException, BatchDispatcherException, BatchJobNotLocalException {

		WSJobExecution latestJobExecution = getBatchManagerHelper().stopJob(
				BatchWolaJsonHelper.parseLong(request, "instanceId", -1),
				BatchWolaJsonHelper.parseLong(request, "executionId", -1));
		if (latestJobExecution != null) {
			return BatchWolaJsonHelper.buildJsonObjectResponseMessage(BatchJSONHelper
					.toJsonObjectBuilder(latestJobExecution,
							jobRepository
									.getStepExecutionAggregatesFromJobExecution(latestJobExecution.getExecutionId()))
					.build());
		} else {
			// Return an empty JSON object since there is no job execution
			return BatchWolaJsonHelper.buildJsonObjectResponseMessage(BatchJSONHelper.createJsonObject());
		}
	}

	/**
	 * 
	 * Either an instanceId or executionId may be specified. If executionId, then
	 * the JobInstance associated with that JobExecution is returned.
	 * 
	 * @return the job instance record (in a JSON response message).
	 * 
	 */
	protected JsonObject getJobInstanceCommand(JsonObject request) {

		WSJobInstance jobInstance = getBatchManagerHelper().getJobInstance(
				BatchWolaJsonHelper.parseLong(request, "instanceId", -1),
				BatchWolaJsonHelper.parseLong(request, "executionId", -1));

		return BatchWolaJsonHelper
				.buildJsonObjectResponseMessage(BatchJSONHelper.toJsonObjectBuilder(jobInstance).build());
	}

	/**
	 * Utility method for validating the jobInstanceId input. A non-numeric value
	 * means an input range was specified for multi purge
	 */
	private boolean isNumeric(String s) {
		return java.util.regex.Pattern.matches("\\d+", s);
	}

	/**
	 * @return the target ref'ed by the given jndiName.
	 */
	private Object jndiLookup(String jndiName) throws NamingException {
		try {
			// Put a dummy CMD on the thread to placate JNDI, which checks for such things..
			ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().beginContext(getDummyComponentMetaData());
			return new InitialContext().lookup(jndiName);
		} finally {
			ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().endContext();
		}
	}

	/**
	 *
	 */
	private void jndiBind(String jndiName) throws NamingException {
		try {
			// Put a dummy CMD on the thread to placate JNDI, which checks for such things..
			ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().beginContext(getDummyComponentMetaData());
			new InitialContext().bind(jndiName, this);
		} finally {
			ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().endContext();
		}
	}

	/**
	*
	*/
	private void jndiUnbind(String jndiName) throws NamingException {
		try {
			// Put a dummy CMD on the thread to placate JNDI, which checks for such things..
			ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().beginContext(getDummyComponentMetaData());
			new InitialContext().unbind(jndiName);
		} finally {
			ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().endContext();
		}
	}

	/**
	 * A dummy CMD to put on the thread while doing JNDI lookups in order to sleaze
	 * our way past JNDI's CMD checks.
	 */
	private static ComponentMetaData dummyCMD = null;

	/**
	 * @return a dummy CMD to put on the thread while doing the JNDI lookup so we
	 *         can sleaze our way past JNDI's CMD checks.
	 */
	private ComponentMetaData getDummyComponentMetaData() {

		if (dummyCMD == null) {

			dummyCMD = new ComponentMetaData() {
				@Override
				public ModuleMetaData getModuleMetaData() {
					return null;
				}

				@Override
				public J2EEName getJ2EEName() {
					return null;
				}

				@Override
				public String getName() {
					return "WOLARequestDispatcher.DummyComponentMetaData";
				}

				@Override
				public void setMetaData(MetaDataSlot slot, Object metadata) {
				}

				@Override
				public Object getMetaData(MetaDataSlot slot) {
					return null;
				}

				@Override
				public void release() {
				}
			};
		}

		return dummyCMD;
	}
}