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
package com.ibm.ws.jbatch.rest.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;

import com.ibm.jbatch.container.ws.WSJobExecution;
import com.ibm.jbatch.container.ws.WSJobInstance;
import com.ibm.jbatch.container.ws.WSPartitionStepAggregate;
import com.ibm.jbatch.container.ws.WSPartitionStepThreadExecution;
import com.ibm.ws.jbatch.rest.utils.WSPurgeResponse;
import com.ibm.jbatch.container.ws.WSStepThreadExecutionAggregate;
import com.ibm.jbatch.container.ws.WSTopLevelStepExecution;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jbatch.joblog.JobExecutionLog;
import com.ibm.ws.jbatch.joblog.JobInstanceLog;
import com.ibm.ws.jbatch.rest.internal.BatchRequestUtil;

/**
 * This class is also used by com.ibm.ws.jbatch.wola.
 * 
 */
public class BatchJSONHelper {

	public static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";
	public static final String MEDIA_TYPE_APPLICATION_JSON = "application/json; charset=UTF-8";

	/**
	 * @return the JsonObject read from the given stream.
	 */
	public static JsonObject readJsonObject(InputStream inputStream) {
		JsonReader jsonReader = Json.createReader(inputStream);
		JsonObject jsonObject = jsonReader.readObject();
		jsonReader.close();
		return jsonObject;
	}

	/**
	 * Write the given json object to the stream.
	 */
	public static void writeJsonObject( JsonObject jsonObject, OutputStream outputStream) {
		writeJsonStructure(jsonObject, outputStream);
	}

	/**
	 * Write the given json structure to the stream.
	 */
	public static void writeJsonStructure( JsonStructure jsonStructure, OutputStream outputStream) {
		//JsonWriter jsonWriter = Json.createWriter(outputStream);
		JsonWriter jsonWriter = BatchJSONHelper.createPrettyJsonWriter(outputStream);

		jsonWriter.write( jsonStructure );
		jsonWriter.close();
	}
	
	/**
     * Create an empty JSON object
     */
    public static JsonObject createJsonObject() {
       return Json.createObjectBuilder().build();
    }

	/**
	 * Write a JSON array of the given jobInstances to the given outputstream.
	 * 
	 * @throws IOException 
	 */
	public static void writeJobInstances( List<WSJobInstance> jobInstances, 
			String urlRoot, 
			OutputStream outputStream ) throws IOException {

		JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();

		for (WSJobInstance jobInstance : jobInstances ) {
			
			JsonObjectBuilder json = toJsonObjectBuilderWithLinks(jobInstance,
					new ArrayList<WSJobExecution>(jobInstance.getJobExecutions()),
					urlRoot);
			jsonArrayBuilder.add( json );
		}
		
		writeJsonStructure( jsonArrayBuilder.build(), outputStream);
	}
	
	

	/**
	 * @throws IOException 
	 */
	public static void writeJobInstance( WSJobInstance jobInstance, 
			String urlRoot, 
			OutputStream outputStream ) throws IOException {
		writeJobInstance(jobInstance, new ArrayList<WSJobExecution>(), urlRoot, outputStream);
	}

	/**
	 * 
	 * @param jobInstance 
	 * @param urlRoot for building HAL-like _links.
	 * @param jobExecList additional HAL-like _links for the job exec records
	 *
	 * @throws IOException 
	 */
	public static void writeJobInstance(WSJobInstance jobInstance, 
			List<WSJobExecution> jobExecList, 
			String urlRoot,
			OutputStream outputStream) throws IOException {
		JsonObject jsonObject = toJsonObjectBuilderWithLinks(jobInstance,
				jobExecList,
				urlRoot).build();
		writeJsonStructure( jsonObject, outputStream);
	}

	/**
	 * @throws IOException 
	 * 
	 */
	public static void writeJobExecution(WSJobExecution jobExecution, List<WSStepThreadExecutionAggregate> stepExecs, 
			String urlRoot,
			OutputStream outputStream) throws IOException {
		writeJsonStructure(toJsonObjectBuilderWithLinks(jobExecution,stepExecs, urlRoot).build(), outputStream);
	}

	/**
	 * Convert the jobexecutionlog to a JSON array of REST API links, then write
	 * the JSON to the given outputstream.
	 */
	public static void writeJobExecutionLogLinks(JobExecutionLog jobExecutionLog, 
			String urlRoot,
			OutputStream outputStream) throws IOException {

		writeJsonStructure( convertJobExecutionLogLinksToJsonArray(Arrays.asList(jobExecutionLog), urlRoot).build(), 
				outputStream );
	}

	public static void writeStepExecutionsList(
			List<WSStepThreadExecutionAggregate> stepExecAggregateList, String urlRoot,
			OutputStream outputStream) throws IOException {

		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
		JsonArrayBuilder partitionBuilder = Json.createArrayBuilder();
		JsonObjectBuilder topLevelObject = null;
		JsonObjectBuilder stepObjectLinks = null;

		//if stepExecAggregateList is empty then arrayBuilder has nothing to be updated with
		if (!stepExecAggregateList.isEmpty()){
			// Loop through list - each entry is a list which contains 
			// entry [0] as top level and entry's [1..n] which are the partitions
			for (WSStepThreadExecutionAggregate stepExecAggregate : stepExecAggregateList) {

				topLevelObject = 
						convertStepExecutionToJsonObjectInBasicFormatNoLink(stepExecAggregate.getTopLevelStepExecution(), urlRoot);

				stepObjectLinks = 
						buildStepExecutionLinks(stepExecAggregate.getTopLevelStepExecution().getJobExecutionId(),
								stepExecAggregate.getTopLevelStepExecution().getJobInstanceId(), urlRoot);

				// If remotable partition entities are active, we'll use the aggregates
				if (stepExecAggregate.getPartitionAggregate() != null) {
					for (WSPartitionStepAggregate partitionStepAggregate : stepExecAggregate.getPartitionAggregate()) {
						partitionBuilder.add(convertStepExecutionPartitionToJsonObject(partitionStepAggregate, 
								urlRoot, partitionStepAggregate.getPartitionStepThread().getPartitionNumber()));
					}
				} else { // Otherwise we'll just use the step thread executions
					for (WSPartitionStepThreadExecution partitionStepExec : stepExecAggregate.getPartitionLevelStepExecutions()) {
						partitionBuilder.add(convertStepExecutionPartitionToJsonObject(partitionStepExec, 
								urlRoot, partitionStepExec.getPartitionNumber()));
					}
				}

				// Add the partition array to the top level execution and output
				topLevelObject.add("partitions", partitionBuilder);
				arrayBuilder.add(topLevelObject);
			}

			// Add the links
			arrayBuilder.add(stepObjectLinks);
		}

		writeJsonStructure(arrayBuilder.build(), outputStream);

	}

	/**
	 * @return The JSON-ified list of job executions.
	 * @throws IOException 
	 */
	public static void writeJobExecutionList(List<WSJobExecution> jobExecutionList, Map<Long, List<WSStepThreadExecutionAggregate>> jobExecStepExecListMap,
			String urlRoot,
			OutputStream outputStream) throws IOException {
		writeJsonStructure( toJsonArrayBuilderWithLinks(jobExecutionList, jobExecStepExecListMap, urlRoot).build(),
				outputStream );
	}

	/**
	 * @throws IOException 
	 * 
	 */
	public static void writeJobDefinitions(Set<String> jobNames, OutputStream outputStream) throws IOException {

		JsonObjectBuilder jsonObjBuilder = Json.createObjectBuilder();

		JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();

		for (String jobName : jobNames) {
			jsonArrayBuilder.add(jobName);
		}

		jsonObjBuilder.add("jobdefinitions", jsonArrayBuilder);

		writeJsonStructure( jsonObjBuilder.build(), outputStream);	
	}


	/**
	 * Convert the jobinstancelog to a JSON array of REST API links, then write
	 * the JSON to the given outputstream.
	 */
	public static void writeJobInstanceLogLinks(JobInstanceLog jobInstanceLog,
			String urlRoot, 
			OutputStream outputStream) throws IOException {
		
		JsonArrayBuilder links = convertJobExecutionLogLinksToJsonArray(jobInstanceLog.getJobExecutionLogs(), urlRoot);

		// Add links for the entire joblog, both as aggregated text and as a zip file.
		links.add( Json.createObjectBuilder()
				.add("rel", "joblog text")
				.add("href", urlRoot + "jobinstances/" + jobInstanceLog.getJobInstance().getInstanceId() + "/joblogs?type=text"))
				.add( Json.createObjectBuilder()
						.add("rel", "joblog zip")
						.add("href", urlRoot + "jobinstances/" + jobInstanceLog.getJobInstance().getInstanceId() + "/joblogs?type=zip")) ;

		writeJsonStructure( links.build(),
				outputStream );
	}
	
	/**
	 * @return a JsonObjectBuilder for the given PurgeResponse
	 */
	public static JsonObjectBuilder toJsonObjectBuilder(WSPurgeResponse purgeResponse) {
		JsonObjectBuilder jsonObjBuilder = Json.createObjectBuilder();
		
		jsonObjBuilder.add("instanceId",purgeResponse.getInstanceId())
			.add("purgeStatus",purgeResponse.getPurgeStatus().toString())
			.add("message",purgeResponse.getMessage());
		
		return jsonObjBuilder;
	}


	/**
	 * @return a JsonObjectBuilder for the given JobInstance.
	 */
	public static JsonObjectBuilder toJsonObjectBuilder(WSJobInstance jobInstance) {

		JsonObjectBuilder jsonObjBuilder = Json.createObjectBuilder();

		// Use parens since they can never be part of a legal xs:id value
		jsonObjBuilder.add("jobName", StringUtils.firstNonNull(jobInstance.getJobName(), ""))
		.add("instanceId", jobInstance.getInstanceId())
		.add("appName", StringUtils.firstNonNull(jobInstance.getAmcName(), ""))
		.add("submitter", StringUtils.firstNonNull(jobInstance.getSubmitter(), ""))
		.add("batchStatus", ((jobInstance.getBatchStatus() != null) ? jobInstance.getBatchStatus().name() : ""))
		.add("jobXMLName", StringUtils.firstNonNull(jobInstance.getJobXMLName(), ""))
		.add("instanceState", ((jobInstance.getInstanceState() != null) ? jobInstance.getInstanceState().name() : ""))
		.add("lastUpdatedTime", formatDate(jobInstance.getLastUpdatedTime()));
		return jsonObjBuilder;
	}

	/**
	 * 
	 * @param jobInstance 
	 * @param urlRoot for building HAL-like _links.
	 * @param jobExecList additional HAL-like _links for the job exec records
	 * 
	 * @return JsonObjectBuilder containing the jobinstance data
	 */
	protected static JsonObjectBuilder toJsonObjectBuilderWithLinks(WSJobInstance jobInstance, 
			List<WSJobExecution> jobExecList, 
			String urlRoot) {

		JsonObjectBuilder jsonObjBuilder = toJsonObjectBuilder(jobInstance);
		
		// Get most recent job execution and look for the JES Job Name and JES Job ID job parameters
		if(jobExecList != null && !jobExecList.isEmpty()) {
			
			String jesJobName = null;
			String jesJobID = null;
			
			Properties jobParams = jobExecList.get(0).getJobParameters();
				
			if(jobParams != null) {
				jesJobName = jobParams.getProperty("com.ibm.ws.batch.submitter.jobName");
				jesJobID = jobParams.getProperty("com.ibm.ws.batch.submitter.jobId");
			}
			
			if(jesJobName != null) {
				jsonObjBuilder.add("JESJobName", jesJobName);
			}
			if(jesJobID != null) {
				jsonObjBuilder.add("JESJobId", jesJobID);
			}
		}
		

		// Add REST-specific links.
		JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder()
				.add(Json.createObjectBuilder()
						.add("rel", "self")
						.add("href",urlRoot + "jobinstances/" + jobInstance.getInstanceId() ))
						.add(Json.createObjectBuilder()
								.add("rel", "job logs")
								.add("href",urlRoot + "jobinstances/" + jobInstance.getInstanceId() + "/joblogs" ));;

								for (WSJobExecution jobExec : ((jobExecList != null) ? jobExecList : new ArrayList<WSJobExecution>())) {
									jsonArrayBuilder.add(Json.createObjectBuilder()
											.add("rel", "job execution")
											.add("href",urlRoot + "jobinstances/" + jobInstance.getInstanceId()+ "/jobexecutions/" + jobExec.getExecutionNumberForThisInstance()));
								}

								jsonObjBuilder.add("_links", jsonArrayBuilder);

								return jsonObjBuilder;
	}


	/**
	 * @return a JsonObjectBuilder for the given JobExecution.
	 * @param urlRoor for writing WOLA tests, should be null
	 */
	public static JsonObjectBuilder toJsonObjectBuilder(WSJobExecution jobExecution, List<WSStepThreadExecutionAggregate> stepExecs) {

		JsonObjectBuilder jsonObjBuilder = toJsonObjectBuilderInBasicFormat(jobExecution);

		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
		for(WSStepThreadExecutionAggregate stepExec : stepExecs){
			arrayBuilder.add(convertStepExecutionSummaryToJsonObject(stepExec.getTopLevelStepExecution(),jobExecution.getExecutionId()));
		}
		jsonObjBuilder.add("stepExecutions", arrayBuilder);

		return jsonObjBuilder;
	}

	public static JsonObjectBuilder toJsonObjectBuilderInBasicFormat(WSJobExecution jobExecution) {
		
		return buildExecutionJson(jobExecution, null);
	}
	
	private static JsonObjectBuilder toJsonObjectBuilderInBasicFormat(WSJobExecution jobExecution, String version) {
		
		return buildExecutionJson(jobExecution, version);
	}
	
	// Utility method to build the job execution json object
	private static JsonObjectBuilder buildExecutionJson(WSJobExecution jobExecution, String version)
	{
		JsonObjectBuilder jsonObjBuilder = Json.createObjectBuilder();
		
		jsonObjBuilder.add("jobName", jobExecution.getJobName())
		.add("executionId", jobExecution.getExecutionId())
		.add("instanceId",  jobExecution.getInstanceId())
		.add("batchStatus", jobExecution.getBatchStatus().name())
		.add("exitStatus", StringUtils.firstNonNull( jobExecution.getExitStatus(), "") )
		.add("createTime", formatDate(jobExecution.getCreateTime()))
		.add("endTime", formatDate(jobExecution.getEndTime()))
		.add("lastUpdatedTime", formatDate(jobExecution.getLastUpdatedTime()))
		.add("startTime", formatDate(jobExecution.getStartTime()))
		.add("jobParameters", convertPropertiesToJsonObject(jobExecution.getJobParameters()));
		
		if(version == null)
			jsonObjBuilder.add("restUrl", StringUtils.trimSuffix(jobExecution.getRestUrl(),"/"));
		else
			jsonObjBuilder.add("restUrl", StringUtils.trimSuffix(jobExecution.getRestUrl(),"/") + "/" + version);
			
		jsonObjBuilder.add("serverId", jobExecution.getServerId())
		.add("logpath", StringUtils.firstNonNull(jobExecution.getLogpath(), "") );
		
		return jsonObjBuilder;
	}


	/**
	 * @return a JsonObjectBuilder for the given JobExecution.
	 * @param urlRoor for writing WOLA tests, should be null
	 */
	protected static JsonObjectBuilder toJsonObjectBuilderWithLinks(WSJobExecution jobExecution, List<WSStepThreadExecutionAggregate> stepExecs, String urlRoot) {

		JsonObjectBuilder jsonObjBuilder = toJsonObjectBuilderInBasicFormat(jobExecution,BatchRequestUtil.getUrlVersion(urlRoot));

		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
		for(WSStepThreadExecutionAggregate stepExec : stepExecs){
			arrayBuilder.add(convertStepExecutionSummaryToJsonObjectWithLink(stepExec.getTopLevelStepExecution(),jobExecution.getExecutionId(),urlRoot));
		}
		jsonObjBuilder.add("stepExecutions", arrayBuilder);

		jsonObjBuilder.add("_links", buildJobExecutionLinksArray(jobExecution,urlRoot));

		return jsonObjBuilder;
	}

	/*
	 * @return JsonArrayBuilder for an arrray of rest links
	 */
	protected static JsonArrayBuilder buildJobExecutionLinksArray(WSJobExecution jobExecution,String urlRoot){

		return Json.createArrayBuilder()
				.add(Json.createObjectBuilder()
						.add("rel", "self")
						.add("href",urlRoot + "jobexecutions/" + jobExecution.getExecutionId()))
				.add(Json.createObjectBuilder()
						.add("rel", "job instance")
						.add("href",urlRoot + "jobinstances/" + jobExecution.getInstanceId()))
				.add(Json.createObjectBuilder()
						.add("rel", "step executions")
						.add("href",urlRoot + "jobexecutions/" + jobExecution.getExecutionId() + "/stepexecutions"))                       
				.add(Json.createObjectBuilder()
						.add("rel", "job logs")
						.add("href", BatchRequestUtil.buildJoblogsUrl(jobExecution, urlRoot)))
				.add(Json.createObjectBuilder()
						.add("rel", "stop url")
						.add("href", BatchRequestUtil.buildStopUrl(jobExecution, urlRoot)));
	}

	/**
	 * 
	 * @param jobExecutionId
	 * @param jobInstanceId
	 * @param urlRoot
	 * @return the job execution and job instance links to the parent entities
	 */
	protected static JsonObjectBuilder buildStepExecutionLinks(long jobExecutionId, long jobInstanceId, String urlRoot) {

		JsonObjectBuilder jsonObjBuilder = Json.createObjectBuilder();
		jsonObjBuilder
		.add("_links", Json.createArrayBuilder()
				.add(Json.createObjectBuilder()
						.add("rel", "job execution")
						.add("href", urlRoot + "jobexecutions/" + jobExecutionId))
						.add(Json.createObjectBuilder()
								.add("rel", "job instance")
								.add("href", urlRoot
										+ "jobinstances/" + jobInstanceId)));

		return jsonObjBuilder;
	}


	/*
	 * @return a JsonObjectBuilder for the summary of the given StepExecution 
	 */
	protected static JsonObjectBuilder convertStepExecutionSummaryToJsonObjectWithLink(StepExecution stepExecution, Long jobExecutionId, String urlRoot){

		JsonObjectBuilder jsonObjBuilder = convertStepExecutionSummaryToJsonObject(stepExecution,jobExecutionId);

		jsonObjBuilder.add("stepExecution",urlRoot + "jobexecutions/" + jobExecutionId + "/stepexecutions/"  + stepExecution.getStepName());

		return jsonObjBuilder;
	}

	/*
	 * @return a JsonObjectBuilder for the summary of the given StepExecution 
	 */
	protected static JsonObjectBuilder convertStepExecutionSummaryToJsonObject(StepExecution stepExecution, Long jobExecutionId){

		JsonObjectBuilder jsonObjBuilder = Json.createObjectBuilder();

		jsonObjBuilder.add("stepExecutionId", stepExecution.getStepExecutionId())
		.add("stepName", stepExecution.getStepName())
		.add("batchStatus", (stepExecution.getBatchStatus() == null ? "":stepExecution.getBatchStatus().name()))
		.add("exitStatus", StringUtils.firstNonNull( stepExecution.getExitStatus(), ""));

		return jsonObjBuilder;
	}

	/**
	 * @return The JSON-ified list of job executions.
	 * @throws IOException 
	 */
	protected static JsonArrayBuilder toJsonArrayBuilderWithLinks(List<WSJobExecution> jobExecutionList, Map<Long,List<WSStepThreadExecutionAggregate>> jobExecStepExecListMap, 
			String urlRoot) {
		JsonArrayBuilder retMe = Json.createArrayBuilder();

		for ( WSJobExecution jobExecution : jobExecutionList ) {
			retMe.add( toJsonObjectBuilderWithLinks( jobExecution,jobExecStepExecListMap.get(jobExecution.getExecutionId()),urlRoot));
		}

		return retMe;
	}

	/**
	 * @return a JsonObjectBuilder for the given StepExecution.
	 */
	public static JsonObjectBuilder convertStepExecutionToJsonObjectInBasicFormatNoLink(WSTopLevelStepExecution stepExecution,
			String urlRoot) { 

		JsonObjectBuilder jsonObjBuilder = Json.createObjectBuilder();

		jsonObjBuilder.add("stepExecutionId", stepExecution.getStepExecutionId())
		.add("stepName", stepExecution.getStepName())
		.add("executionId", stepExecution.getJobExecutionId())
		.add("instanceId", stepExecution.getJobInstanceId())
		.add("batchStatus", stepExecution.getBatchStatus().name())
		.add("startTime", formatDate(stepExecution.getStartTime()))
		.add("endTime", formatDate(stepExecution.getEndTime()))
		.add("exitStatus", StringUtils.firstNonNull( stepExecution.getExitStatus(), "") )
		.add("metrics", convertStepMetricsToJsonObject(stepExecution.getMetrics())

				//We don't add any links back to individual step executions since we always want to 
				//return back the entire list. If we get a request to retrieve specific step executions
				//we can always add to the API it later.       
				);

		return jsonObjBuilder;
	}
	
	/**
	 * Builds the Partition information in the output JSON object
	 * @param stepExecution
	 * @param jobExecutionId
	 * @param urlRoot
	 * @param partNumber
	 * @return
	 */
	protected static JsonObjectBuilder convertStepExecutionPartitionToJsonObject(
			WSPartitionStepAggregate partitionAggregate, String urlRoot, int partNumber) {

		StepExecution stepExecution = partitionAggregate.getPartitionStepThread();
		JsonObjectBuilder jsonObjBuilder = Json.createObjectBuilder();
		
		String serverId = partitionAggregate.getRemotablePartition() == null ? "":partitionAggregate.getRemotablePartition().getServerId();
		String restUrl = partitionAggregate.getRemotablePartition() == null ? "":partitionAggregate.getRemotablePartition().getRestUrl();

		jsonObjBuilder
		.add("partitionNumber", partNumber)
		.add("batchStatus", stepExecution.getBatchStatus().name())
		.add("startTime", formatDate(stepExecution.getStartTime()))
		.add("endTime", formatDate(stepExecution.getEndTime()))
		.add("exitStatus", StringUtils.firstNonNull(stepExecution.getExitStatus(), ""))
		.add("restUrl", restUrl)
		.add("serverId",serverId)
		.add("metrics",
				convertStepMetricsToJsonObject(stepExecution
						.getMetrics())

						// We don't add any links back to individual step executions
						// since we always want to
						// return back the entire list. If we get a request to retrieve
						// specific step executions
						// we can always add to the API it later.

				);

		return jsonObjBuilder;
	}
	
	protected static JsonObjectBuilder convertStepExecutionPartitionToJsonObject(
			StepExecution stepExecution, String urlRoot, int partNumber) {

		JsonObjectBuilder jsonObjBuilder = Json.createObjectBuilder();

		jsonObjBuilder
		.add("partitionNumber", partNumber)
		.add("batchStatus", stepExecution.getBatchStatus().name())
		.add("startTime", formatDate(stepExecution.getStartTime()))
		.add("endTime", formatDate(stepExecution.getEndTime()))
		.add("exitStatus", StringUtils.firstNonNull(stepExecution.getExitStatus(), ""))
		.add("metrics",
				convertStepMetricsToJsonObject(stepExecution
						.getMetrics())

						// We don't add any links back to individual step executions
						// since we always want to
						// return back the entire list. If we get a request to retrieve
						// specific step executions
						// we can always add to the API it later.

				);

		return jsonObjBuilder;
	}


	/**
	 * @return a JsonObjectBuilder for the given StepExecution.
	 */
	protected static JsonObjectBuilder convertStepMetricsToJsonObject(Metric[] metrics) {

		JsonObjectBuilder jsonObjBuilder = Json.createObjectBuilder();

		for (Metric metric : metrics) {
			String name = metric.getType().toString();
			String value =Long.valueOf(metric.getValue()).toString();

			jsonObjBuilder.add(name, value);

		}

		return jsonObjBuilder;
	}

	/**
	 * @return the given date formatted as a string, or "" if the date is null.
	 */
	private static String formatDate(Date d) {
		return (d != null) ? BatchDateFormat.get().format(d) : "";
	}



	/**
	 * @return a pretty-print configured JsonWriter
	 */
	protected static JsonWriter createPrettyJsonWriter(OutputStream outputStream) {
		Map<String, Object> writerConfig = new HashMap<String, Object>(1);
		writerConfig.put(JsonGenerator.PRETTY_PRINTING, true);

		JsonWriterFactory writerFactory = Json.createWriterFactory(writerConfig);

		return writerFactory.createWriter(outputStream);
	}

	/**
	 * @return a Properties object from the given JsonObject.  Returns an empty Properties object
	 * if the parm is 'null'.
	 */
	protected static Properties convertJsonObjectToProperties(JsonObject jsonObject) {

		Properties retMe = new Properties();

		if (jsonObject != null) {
			for (Map.Entry<String, JsonValue> entry : jsonObject.entrySet() ) {
				retMe.setProperty( entry.getKey(), ((JsonString)entry.getValue()).getString() );
			}
		}

		return retMe;
	}

	/**
	 * 
	 * @return a JsonObject for the given properties object
	 */
	protected static JsonObject convertPropertiesToJsonObject( Properties props) {

		JsonObjectBuilder builder = Json.createObjectBuilder();

		if (props != null) {
			for (String key : props.stringPropertyNames()) {
				builder.add(key, props.getProperty(key));
			}
		}

		return builder.build();
	}




	/**
	 * @return a JSON array of joblog part links.
	 */
	protected static JsonArrayBuilder convertJobExecutionLogLinksToJsonArray(List<JobExecutionLog> jobExecutionLogs,
			String urlRoot) throws IOException {
		JsonArrayBuilder retMe = Json.createArrayBuilder();
		
		String urlVersion = BatchRequestUtil.getUrlVersion(urlRoot);

		for (JobExecutionLog jobExecutionLog : jobExecutionLogs) {

			// Add links for the entire joblog, both as aggregated text and as a zip file.
			retMe.add(Json.createObjectBuilder()
					.add("rel", "joblog text")
					.add("href", ((WSJobExecution)jobExecutionLog.getJobExecution()).getRestUrl() + "/" + urlVersion +"jobexecutions/" + 
							     jobExecutionLog.getExecutionId() + "/joblogs?type=text"));
			retMe.add(Json.createObjectBuilder()
					.add("rel", "joblog zip")
					.add("href", ((WSJobExecution)jobExecutionLog.getJobExecution()).getRestUrl() + "/" + urlVersion + "jobexecutions/" + 
					             jobExecutionLog.getExecutionId() + "/joblogs?type=zip"));

			for (String relativePath : jobExecutionLog.getRelativePaths()) {

				relativePath = StringUtils.normalizePath(relativePath);

				// Add links for each individual joblog part, as both plain text and as a zip file.
				retMe.add(Json.createObjectBuilder()
						.add("rel", "joblog part text")
						.add("href", ((WSJobExecution)jobExecutionLog.getJobExecution()).getRestUrl() + "/" + urlVersion + "jobexecutions/" + 
						             jobExecutionLog.getExecutionId() + "/joblogs?part=" + relativePath + "&type=text"));
				retMe.add(Json.createObjectBuilder()
						.add("rel", "joblog part zip")
						.add("href", ((WSJobExecution)jobExecutionLog.getJobExecution()).getRestUrl() + "/" + urlVersion + "jobexecutions/" + 
						             jobExecutionLog.getExecutionId() + "/joblogs?part=" + relativePath + "&type=zip"));
			}
		}

		return retMe;
	}

	/**
	 * @return the merged properties.  Props are merged in the order that they're received,
	 *         so props at the end of the arg list override props at the beginning.
	 */
	public static Properties mergeProperties( Properties... properties) {
		Properties retMe = new Properties();
		for (Properties props : ((properties != null) ? properties : new Properties[] {}) ) {
			if (props != null) {
				retMe.putAll(props);
			}
		}
		return retMe;
	}

	/**
	 * Build JsonObjectBuilder with partition information.
	 * Note: startTime, endTime, and metrics are not available in this object
	 * 
	 * @param partitionNumber
	 * @param batchStatus
	 * @param exitStatus
	 * @param stepName
	 * @param topLevelInstanceId
	 * @param topLevelExecutionId
	 * @return
	 */
	public static JsonObjectBuilder convertPartitionToJsonObjectBuilderForEvent(int partitionNumber,
			BatchStatus batchStatus, String exitStatus, String stepName,
			long topLevelInstanceId, long topLevelExecutionId, long topLevelStepExecutionId) {
		
		JsonObjectBuilder jsonObjBuilder = Json.createObjectBuilder();

		jsonObjBuilder
		.add("partitionNumber", partitionNumber)
		.add("batchStatus",batchStatus.name())
		.add("exitStatus", StringUtils.firstNonNull(exitStatus, ""))
		.add("stepName", stepName)
		.add("executionId", topLevelExecutionId)
		.add("instanceId", topLevelInstanceId)
		.add("stepExecutionId", topLevelStepExecutionId);
		
		return jsonObjBuilder;
	}

	/**
	 * Build JsonObjectBuilder with checkpoint info
	 * @param stepName
	 * @param jobInstanceId
	 * @param jobExecutionId
	 * @param stepExecutionId
	 * @return
	 */
	public static JsonObjectBuilder convertCheckpointToJsonObjectBuilderForEvent(
			String stepName, long jobInstanceId, long jobExecutionId, long stepExecutionId) {
		JsonObjectBuilder jsonObjBuilder = Json.createObjectBuilder();

		jsonObjBuilder
		.add("stepName", stepName)
		.add("executionId", jobExecutionId)
		.add("instanceId", jobInstanceId)
		.add("stepExecutionId", stepExecutionId);
		
		return jsonObjBuilder;
	}

	public static JsonObjectBuilder convertSplitFlowToJsonObjectBuilderForEvent(
			String splitName, String flowName, long jobInstanceId, long jobExecutionId) {
		JsonObjectBuilder jsonObjBuilder = Json.createObjectBuilder();

		jsonObjBuilder
		.add("splitName", splitName)
		.add("flowName", flowName)
		.add("executionId", jobExecutionId)
		.add("instanceId", jobInstanceId);
		
		return jsonObjBuilder;
	}
	/**
	 * Build the Json Object for a job log event.
	 * 
	 * @param jobInstanceId
	 * @param jobExecutionId
	 * @param appName
	 * @param partNumber
	 * @param logContent
	 * @param partitionNumber
	 * @param stepName
	 * @param splitName
	 * @param flowName
	 * @return
	 */
	@Trivial
	public static JsonObjectBuilder convertJobLogToJsonObjectBuilderForEvent(
			long jobInstanceId, long jobExecutionId, String appName,
			int partNumber, Integer partitionNumber, String stepName, String splitName,
			String flowName, boolean finalLog, String logContent) {

		JsonObjectBuilder jsonObjBuilder = Json.createObjectBuilder();

		jsonObjBuilder
		.add("executionId", jobExecutionId)
		.add("instanceId", jobInstanceId)
		.add("appName", appName)
		.add("partNumber", partNumber)
		.add("finalLog", finalLog);
		
		if (partitionNumber != null){
			jsonObjBuilder
			.add("partitionNumber", partitionNumber)
			.add("stepName", stepName);
		}
		
		if (splitName != null){
			jsonObjBuilder
			.add("splitName", splitName)
			.add("flowName", flowName);
		}		
		
		//Create a JsonArray where each long of the job log is one object in the array
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        BufferedReader bufferedReader = new BufferedReader(new StringReader(logContent));
        String line = null;
		try {
			while( (line = bufferedReader.readLine()) != null ) {
				arrayBuilder.add(line);
			}
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}

        jsonObjBuilder.add("contents", arrayBuilder);
		
		return jsonObjBuilder;
	}
	
    public static void buildAndWritePurgeJsonObject(
            ArrayList<WSPurgeResponse> responseList,
            OutputStream outputStream) {
        JsonObjectBuilder jsonObjBuilder = Json.createObjectBuilder();
        JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();

        WSPurgeResponse purgeResponse;
        
        for (int i = 0; i < responseList.size(); i++) {
            purgeResponse = responseList.get(i);
            jsonObjBuilder.add("instanceId", purgeResponse.getInstanceId());
            jsonObjBuilder.add("purgeStatus", purgeResponse.getPurgeStatus().name());
            jsonObjBuilder.add("message", purgeResponse.getMessage());
            jsonObjBuilder.add("redirectUrl", ((purgeResponse.getRedirectURL() != null) ? purgeResponse.getRedirectURL() : ""));
            jsonArrayBuilder.add(jsonObjBuilder);
        }
        
        writeJsonStructure(jsonArrayBuilder.build(), outputStream);
    }
}


