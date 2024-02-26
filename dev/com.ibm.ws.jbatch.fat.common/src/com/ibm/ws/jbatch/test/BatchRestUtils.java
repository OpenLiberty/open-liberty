/*******************************************************************************
 * Copyright (c) 2014, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.batch.runtime.BatchStatus;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonStructure;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.util.EntityUtils;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.DataSource;
import com.ibm.websphere.simplicity.config.DataSourceProperties;
import com.ibm.websphere.simplicity.config.DatabaseStore;
import com.ibm.websphere.simplicity.config.JdbcDriver;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.common.encoder.Base64Coder;
import com.ibm.ws.jbatch.test.dbservlet.DbServletClient;

import componenttest.common.apiservices.Bootstrap;
import componenttest.common.apiservices.BootstrapProperty;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;

/**
 * Batch REST client utility
 */
public class BatchRestUtils {

    private LibertyServer server = null;

    public BatchRestUtils(LibertyServer server) {
        this.server = server;
    }
    
    public static String BATCH_V2_URL = "/ibm/api/batch/v2/";
    public static final String BATCH_BASE_URL = "/ibm/api/batch/";

    //public static final int POLLING_TIMEOUT_MILLISECONDS = 100000;
    public static final int POLLING_TIMEOUT_MILLISECONDS = 10000000;

    public static final String HEADER_CONTENT_TYPE_KEY = "Content-Type";

    public static final String MEDIA_TYPE_APPLICATION_JSON = "application/json; charset=UTF-8";
    
    public static final String MEDIA_TYPE_TEXT_HTML = "text/html;charset=UTF-8";
    
    public static final String MEDIA_TYPE_MUTLITPART_FORM_WITH_BOUNDARY = "multipart/form-data;boundary=";
    
    public static final String ADMIN_USERNAME = "bob";
    
    public static final String ADMIN_PASS = "bobpwd";

    /**
     * helper for simple logging.
     */
    private static void log(String method, Object msg) {
        Log.info(BatchRestUtils.class, method, String.valueOf(msg));
    }

    public static JsonObject propertiesToJson(Properties props) {
        JsonObjectBuilder builder = Json.createObjectBuilder();

        for (Iterator iter = props.keySet().iterator(); iter.hasNext();) {
            String key = (String) iter.next();
            builder = builder.add(key, props.getProperty(key));
        }
        return builder.build();
    }
    
    /*
     * Utility method to get the instance log diractory
     * 
     * @param The instance id for which to obtain the log directory
     * @return A File object representing the instance log directory
     */
    public File getInstanceDirectory(Long jobInstanceId) {
        String method = "getInstanceDirectory";

        File joblogsDir = new File(server.getServerRoot() + File.separator + "logs" + File.separator + "joblogs");
        Path path = Paths.get(joblogsDir.getAbsolutePath());

        File instanceDir = null;

        Stream<Path> files = null;
        try {
            files = Files.walk(path);

            Iterator<Path> iterator = files.iterator();
            while (iterator.hasNext()) {
                Path p = iterator.next();
                p.getFileName();
                if(p.endsWith("instance." + jobInstanceId)) {
                    instanceDir = p.toFile();
                    break;
                }
            }
        } catch (IOException e) {
             log(method,"Could not determine whether instance directory exists: " + e);
        } finally {
            if(files != null)
                files.close();
        }

        return instanceDir;
    }
    
    /**
     * Expects response code 409 instead of 200 
     * 
     * @param jobInstanceId
     * @param baseUrl
     * @param username
     * @param password
     * 
     * @return jobInstance
     */
    public void purgeJobInstanceExpectHttpConflict(long jobInstanceId, String baseUrl, String username, String password) throws IOException {
        String method = "purgeJobInstanceExpectHttpConflict";

        HttpURLConnection con = HttpUtils.getHttpConnection(buildURL(baseUrl + "jobinstances/" + jobInstanceId),
                                                            HttpURLConnection.HTTP_CONFLICT,
                                                            new int[0],
                                                            10 * 1000,
                                                            HTTPRequestMethod.DELETE,
                                                            BatchRestUtils.buildHeaderMap(username,password),
                                                            null);

        logReaderContents(method, "Purge response: ", HttpUtils.getErrorStream(con));

      }
    
    /**
     * Expects response code 400 instead of 200 
     * 
     * @param jobInstanceId
     * @param baseUrl
     * @param username
     * @param password
     * 
     * @return jobInstance
     */
    public void purgeJobInstanceExpectBadRequest(long jobInstanceId, String baseUrl, String username, String password) throws IOException {
        String method = "purgeJobInstanceExpectBadRequest";

        HttpURLConnection con = HttpUtils.getHttpConnection(buildURL(baseUrl + "jobinstances/" + jobInstanceId),
                                                            HttpURLConnection.HTTP_BAD_REQUEST,
                                                            new int[0],
                                                            10 * 1000,
                                                            HTTPRequestMethod.DELETE,
                                                            BatchRestUtils.buildHeaderMap(username,password),
                                                            null);

        logReaderContents(method, "Purge response: ", HttpUtils.getErrorStream(con));

      }


    /**
     * @return the response - a jobInstance JsonObject
     */
    public JsonObject submitJobAndWaitUntilFinished(String appName, String jobName, String baseUrl) throws Exception {
    	JsonObject jobInstance = submitJob(appName, jobName, baseUrl);
    	return waitForJobInstanceToFinish(jobInstance, baseUrl);
    }
    
    public JsonObject submitJobAndWaitUntilFinished(String appName, String jobName, Properties jobParameters, String baseUrl) throws Exception {
    	JsonObject jobInstance = submitJob(appName, jobName, jobParameters, baseUrl);
    	return waitForJobInstanceToFinish(jobInstance, baseUrl);
    }

    /**
     * @return the response - a jobInstance JsonObject
     */
    public JsonObject submitJob(String appName, String jobName, String baseUrl) throws Exception {
        JsonObjectBuilder payloadBuilder = Json.createObjectBuilder()
                        .add("applicationName", appName)
                        .add("jobXMLName", jobName);
                       

        return submitJob(payloadBuilder.build(), baseUrl);
    }
    
    /**
     * @return the response - a jobInstance JsonObject
     */
    public JsonObject submitJobWithModuleName(String appName, String moduleName, String jobName, String baseUrl) throws Exception {
        JsonObjectBuilder payloadBuilder = Json.createObjectBuilder()
                        .add("applicationName", appName)
                        .add("moduleName", moduleName)
                        .add("jobXMLName", jobName);
                       

        return submitJob(payloadBuilder.build(), baseUrl);
    }
    
    /**
     * @return the response - a jobInstance JsonObject
     */
    public JsonObject submitJob(String appName, String jobName, String baseUrl, String username, String password) throws Exception {
        JsonObjectBuilder payloadBuilder = Json.createObjectBuilder()
                        .add("applicationName", appName)
                        .add("jobXMLName", jobName);
                       

        return submitJob(payloadBuilder.build(), baseUrl, username, password);
    }
    
    /**
     * @return the response - a jobInstance JsonObject
     */
    public JsonObject submitJob(String appName, String jobName, Properties jobParameters, String baseUrl, String username, String password) throws Exception {
        JsonObjectBuilder payloadBuilder = Json.createObjectBuilder()
                        .add("applicationName", appName)
                        .add("jobXMLName", jobName)
                        .add("jobParameters", propertiesToJson(jobParameters));
                       

        return submitJob(payloadBuilder.build(), baseUrl, username, password);
    }
    
    /**
     * 
     * @param appName
     * @param jobName
     * @param xml
     * @return the response - a jobInstance JsonObject
     * @throws Exception
     */
    public JsonObject submitJobviaJSON(String appName, String jobName, String xml, String baseUrl) throws Exception {
        JsonObjectBuilder payloadBuilder = Json.createObjectBuilder()
                        .add("applicationName", appName)
                        .add("jobXMLName", jobName)
                        .add("jobXML", xml);
                       

        return submitJob(payloadBuilder.build(), baseUrl);
    }

    /**
     * @return the response - a jobInstance JsonObject
     */
    public JsonObject submitJob(String appName, String jobName, Properties jobParameters, String baseUrl) throws Exception {
        JsonObjectBuilder payloadBuilder = Json.createObjectBuilder()
                        .add("applicationName", appName)
                        .add("jobXMLName", jobName)
                        .add("jobParameters", propertiesToJson(jobParameters));

        return submitJob(payloadBuilder.build(), baseUrl);
    }

    /**
     * @return the response - a jobInstance JsonObject
     */
    public JsonObject submitJob(JsonObject jobSubmitPayload, String baseUrl) throws Exception {

       return submitJob(jobSubmitPayload, baseUrl, ADMIN_USERNAME , ADMIN_PASS);
    }
    
    /**
     * @return the response - a jobInstance JsonObject
     */
    public JsonObject submitJob(JsonObject jobSubmitPayload, String baseUrl, String username, String password) throws Exception {

        log("submitJob", "Request: jobSubmitPayload= " + jobSubmitPayload.toString());

        HttpURLConnection con = HttpUtils.getHttpConnection(buildURL(baseUrl + "jobinstances"),
                                                            HttpURLConnection.HTTP_CREATED,
                                                            new int[0],
                                                            10 * 1000,
                                                            HTTPRequestMethod.POST,
                                                            BatchRestUtils.buildHeaderMap(username,password),
                                                            new ByteArrayInputStream(jobSubmitPayload.toString().getBytes("UTF-8")));

        assertEquals(MEDIA_TYPE_APPLICATION_JSON, con.getHeaderField("Content-Type"));

        JsonObject jobInstance = Json.createReader(con.getInputStream()).readObject();

        log("submitJob", "Response: jsonResponse= " + jobInstance.toString());

        jobInstance.getJsonNumber("instanceId"); // verifies this is a valid number

        assertEquals(username, jobInstance.getString("submitter"));

        return jobInstance;
    }
    
    /**
     * @return the response - the connection response message
     */
    public String submitJobExpectFailure(String appName, String jobName, String baseUrl) throws Exception {
    	
    	JsonObjectBuilder payloadBuilder = Json.createObjectBuilder()
                .add("applicationName", appName)
                .add("jobXMLName", jobName);
    	JsonObject jobSubmitPayload = payloadBuilder.build();

        log("submitJobExpectFailure", "Request: jobSubmitPayload= " + jobSubmitPayload.toString());

        HttpURLConnection con = HttpUtils.getHttpConnection(buildURL(baseUrl + "jobinstances"),
                                                            HttpURLConnection.HTTP_INTERNAL_ERROR,
                                                            new int[0],
                                                            10 * 1000,
                                                            HTTPRequestMethod.POST,
                                                            BatchRestUtils.buildHeaderMap(ADMIN_USERNAME, ADMIN_PASS),
                                                            new ByteArrayInputStream(jobSubmitPayload.toString().getBytes("UTF-8")));
        
        log("submitJobExpectFailure", "Response: " + con.getResponseMessage());

        String line;
        BufferedReader br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
        while ((line = br.readLine()) != null) {
        	log("submitJobExpectFailure", "Error Stream: " + line);	
        }
        br.close();

        return con.getResponseMessage();
    }

    public JsonObject restartJobInstance(long jobInstanceId, Properties restartJobParms) throws IOException {
    	return restartJobInstance(jobInstanceId, restartJobParms, BATCH_BASE_URL);
    }

    /**
     * @param jobInstanceId
     * @param restartJobParms
     * @return jobInstance
     */
    public JsonObject restartJobInstance(long jobInstanceId, Properties restartJobParms, String baseUrl) throws IOException {

        JsonObject restartPayload = Json.createObjectBuilder()
                        .add("jobParameters", propertiesToJson(restartJobParms))
                        .build();

        HttpURLConnection con = HttpUtils.getHttpConnection(buildURL(baseUrl + "jobinstances/" + jobInstanceId + "?action=restart"),
                                                            HttpURLConnection.HTTP_OK,
                                                            new int[0],
                                                            10 * 1000,
                                                            HTTPRequestMethod.PUT,
                                                            BatchRestUtils.buildHeaderMap(),
                                                            new ByteArrayInputStream(restartPayload.toString().getBytes("UTF-8")));

        assertEquals(MEDIA_TYPE_APPLICATION_JSON, con.getHeaderField("Content-Type"));

        //OK not to close anything here?
        JsonObject jobInstance = Json.createReader(con.getInputStream()).readObject();

        log("restartJobInstance", "Response: jsonResponse= " + jobInstance.toString());

        // Verify new execution's instance matches original instance
        assertEquals(jobInstanceId, jobInstance.getJsonNumber("instanceId").longValue());

        return jobInstance;
    }
    
    /**
     * Expects response code 409 instead of 200 
     * 
     * @param jobInstanceId
     * @param restartJobParms
     * @return jobInstance
     */
    public void restartJobInstanceExpectHttpConflict(long jobInstanceId, Properties restartJobParms, String baseUrl) throws IOException {

        JsonObject restartPayload = Json.createObjectBuilder()
                        .add("jobParameters", propertiesToJson(restartJobParms))
                        .build();

        HttpURLConnection con = HttpUtils.getHttpConnection(buildURL(baseUrl + "jobinstances/" + jobInstanceId + "?action=restart"),
                                                            HttpURLConnection.HTTP_CONFLICT,
                                                            new int[0],
                                                            10 * 1000,
                                                            HTTPRequestMethod.PUT,
                                                            BatchRestUtils.buildHeaderMap(),
                                                            new ByteArrayInputStream(restartPayload.toString().getBytes("UTF-8")));

        System.out.println(con.getHeaderField("Content-Type"));
        assertEquals(MEDIA_TYPE_TEXT_HTML, con.getHeaderField("Content-Type"));
        
      }
    
    /**
     * @param jobInstance
     * @param restartJobParms as Properties obj
     * @param baseUrl
     * @return jobInstance
     */
    public JsonObject restartJobExecution(long jobExecutionId, Properties restartJobParms, String baseUrl) throws IOException {
    	return restartJobExecution(jobExecutionId, propertiesToJson(restartJobParms), baseUrl);
    }

    /**
     * @param jobInstance
     * @param restart job parms as serialized JsonObject
     * @param baseUrl
     * @return jobInstance
     */
    public JsonObject restartJobExecution(long jobExecutionId, JsonObject jsonJobParms, String baseUrl) throws IOException{

        JsonObject restartPayload = Json.createObjectBuilder().add("jobParameters", jsonJobParms).build();

        log("restartJobExecution", "Request: restartPayload= " + restartPayload.toString());

    	HttpURLConnection con = HttpUtils.getHttpConnection(buildURL(baseUrl + "jobexecutions/" + jobExecutionId + "?action=restart"),
                                                    HttpURLConnection.HTTP_OK,
                                                    new int[0],
                                                    10 * 1000,
                                                    HTTPRequestMethod.PUT,
                                                    BatchRestUtils.buildHeaderMap(),
                                                    new ByteArrayInputStream(restartPayload.toString().getBytes("UTF-8")));

    	assertEquals(MEDIA_TYPE_APPLICATION_JSON, con.getHeaderField("Content-Type"));

    	//OK not to close anything here?
    	JsonObject jobInstance = Json.createReader(con.getInputStream()).readObject();

    	log("restartJobExecution", "Response: jsonResponse= " + jobInstance.toString());

    	return jobInstance;
    }
    
    /**
     * @param jobInstance
     * @param jobExecutionId
     * @param restartJobParms
     * @return jobInstance
     */
    public void restartJobExecutionExpectHttpConflict(JsonObject jobInstance, long jobExecutionId, Properties restartJobParms, String baseUrl) throws IOException{
    	JsonObject restartPayload = Json.createObjectBuilder()
                .add("jobParameters", propertiesToJson(restartJobParms))
                .build();

    	HttpURLConnection con = HttpUtils.getHttpConnection(buildURL(baseUrl + "jobexecutions/" + jobExecutionId + "?action=restart"),
                                                    HttpURLConnection.HTTP_CONFLICT,
                                                    new int[0],
                                                    10 * 1000,
                                                    HTTPRequestMethod.PUT,
                                                    BatchRestUtils.buildHeaderMap(),
                                                    new ByteArrayInputStream(restartPayload.toString().getBytes("UTF-8")));

    	assertEquals(MEDIA_TYPE_TEXT_HTML, con.getHeaderField("Content-Type"));

        }
    
    /**
     * @param jobInstance
     * @param jobExecutionId
     * @param restartJobParms
     * @return jobInstance
     */
    public void restartJobExecutionExpectHttpBadRequest(long jobExecutionId, Properties restartJobParms, String baseUrl) throws IOException{
    	JsonObject restartPayload = Json.createObjectBuilder()
                .add("jobParameters", propertiesToJson(restartJobParms))
                .build();

    	HttpURLConnection con = HttpUtils.getHttpConnection(buildURL(baseUrl + "jobexecutions/" + jobExecutionId + "?action=restart"),
                                                    HttpURLConnection.HTTP_BAD_REQUEST,
                                                    new int[0],
                                                    10 * 1000,
                                                    HTTPRequestMethod.PUT,
                                                    BatchRestUtils.buildHeaderMap(),
                                                    new ByteArrayInputStream(restartPayload.toString().getBytes("UTF-8")));

    	assertEquals(MEDIA_TYPE_TEXT_HTML, con.getHeaderField("Content-Type"));

        }

    /**
     * Stop the given job execution
     * 
     * @return the JobExecution JsonObject
     */
    public JsonObject stopJobExecution(long jobExecutionId, String baseUrl) throws IOException {

        HttpURLConnection con = HttpUtils.getHttpConnection(buildURL(baseUrl + "jobexecutions/" + jobExecutionId + "?action=stop"),
                                                            HttpURLConnection.HTTP_OK,
                                                            new int[0],
                                                            10 * 1000,
                                                            HTTPRequestMethod.PUT,
                                                            BatchRestUtils.buildHeaderMap(),
                                                            null);

        assertEquals(MEDIA_TYPE_APPLICATION_JSON, con.getHeaderField("Content-Type"));

        //OK not to close anything here?
        JsonObject jobExecution = Json.createReader(con.getInputStream()).readObject();

        log("stopJobExecution", "Response: jsonResponse= " + jobExecution.toString());

        assertEquals(jobExecutionId, jobExecution.getJsonNumber("executionId").longValue());

        return jobExecution;
    }
    
    /**
     * Stop the given job execution
     * 
     * @return the JobExecution JsonObject
     */
    public JsonObject stopJobExecutionAndWaitTillStopped(long jobExecutionId, String baseUrl) throws IOException, InterruptedException {

        HttpURLConnection con = HttpUtils.getHttpConnection(buildURL(baseUrl + "jobexecutions/" + jobExecutionId + "?action=stop"),
                                                            HttpURLConnection.HTTP_OK,
                                                            new int[0],
                                                            10 * 1000,
                                                            HTTPRequestMethod.PUT,
                                                            BatchRestUtils.buildHeaderMap(),
                                                            null);

        assertEquals(MEDIA_TYPE_APPLICATION_JSON, con.getHeaderField("Content-Type"));

        //OK not to close anything here?
        JsonObject jobExecution = Json.createReader(con.getInputStream()).readObject();

        log("stopJobExecutionAndWaitTillStopped", "Response: jsonResponse= " + jobExecution.toString());

        assertEquals(jobExecutionId, jobExecution.getJsonNumber("executionId").longValue());
        
        return waitForJobExecutionToReachStatus(jobExecutionId, baseUrl, BatchStatus.STOPPED);

    }
    

    /**
     * Issue a stop for the given job instance, (doesn't wait)
     * 
     * @return the JobExecution JsonObject
     */
    public JsonObject stopJobInstance(long jobInstanceId, String baseUrl) throws IOException {

        HttpURLConnection con = HttpUtils.getHttpConnection(buildURL(baseUrl + "jobinstances/" + jobInstanceId + "?action=stop"),
                                                            HttpURLConnection.HTTP_OK,
                                                            new int[0],
                                                            10 * 1000,
                                                            HTTPRequestMethod.PUT,
                                                            BatchRestUtils.buildHeaderMap(),
                                                            null);

        assertEquals(MEDIA_TYPE_APPLICATION_JSON, con.getHeaderField("Content-Type"));
        
        JsonObject jobExecution = Json.createReader(con.getInputStream()).readObject();

        log("stopJobInstance", "Response: jsonResponse= " + jobExecution.toString());

        return jobExecution;
    }

    /**
     * Issue a stop for the given job instance, (doesn't wait)
     * 
     * @return the JobExecution JsonObject
     */
    public JsonObject stopJobInstance(long jobInstanceId ) throws IOException {
    	return stopJobInstance(jobInstanceId,BATCH_BASE_URL);
    }
    
    
    /**
     * @return The createDate parsed from the status message in YYYY-MM-DD format
     */
    public String getJobCreateDateFromStatus(long jobInstanceId, String baseUrl) throws IOException {
    	
    	JsonObject jobExecution = getOnlyJobExecution(jobInstanceId, baseUrl);
    	String createTime = jobExecution.getString("createTime");
    	
    	final Matcher matcher = Pattern.compile("([0-9][0-9][0-9][0-9]\\/[0-9][0-9]\\/[0-9][0-9])").matcher(createTime);
    	assertTrue(createTime, matcher.find());
    	return matcher.group(1).replace("/", "-");
    }

    /**
     * This hinges on an important detail of the URL invoked: that the executions are
     * sorted most-recent to least-recent. This should be documented and treated as
     * a normal, external API (i.e. the behavior should be stable and maintained, etc.).
     * 
     * @param jobInstanceId
     * @return A sorted array of job executions, or an empty array if there are none.
     * @throws IOException
     */
    public JsonArray getJobExecutionsMostRecentFirst(long jobInstanceId, String baseUrl) throws IOException {
        // Get job executions for this jobinstance
        HttpURLConnection con = HttpUtils.getHttpConnection(buildURL(baseUrl + "jobinstances/" + jobInstanceId + "/jobexecutions"),
                                                            HttpURLConnection.HTTP_OK,
                                                            new int[0],
                                                            10 * 1000,
                                                            HTTPRequestMethod.GET,
                                                            BatchRestUtils.buildHeaderMap(),
                                                            null);

        assertEquals(MEDIA_TYPE_APPLICATION_JSON, con.getHeaderField("Content-Type"));

        // Read the response (an array of  job executions)
        JsonArray jsonArray = Json.createReader(con.getInputStream()).readArray();

        log("getJobExecutionsMostRecentFirst", "Response: jsonArray= " + jsonArray.toString());

        return jsonArray;
    }

  
    /**
     * See 
     * {@link BatchRestUtils#waitForNthJobExecution(long, String, int)}
     * which this method calls, with numExecsToWaitFor=1
     * 
     * @param jobInstanceId
     * @param baseUrl
     * 
     * @return the first job execution JSON obj. 
     * @throws IllegalStateException if more than 1 executions exist at any point.  Will also throw IllegalStateException if we exhaust the wait timeout without seeing the 1st execution.
     */
    public JsonObject waitForFirstJobExecution(long jobInstanceId, String baseUrl) throws IOException, InterruptedException {    
        return waitForNthJobExecution(jobInstanceId, baseUrl, 1);
    }
    
    /**
     * See 
     * {@link BatchRestUtils#waitForFirstJobExecution(long, String)}
     * which this method calls, with BATCH_BASE_URL
     * 
     * @param jobInstanceId
     * 
     * @return the first job execution JSON obj. 
     * @throws IllegalStateException if more than 1 executions exist at any point.  Will also throw IllegalStateException if we exhaust the wait timeout without seeing the 1st execution.
     */
    public JsonObject waitForFirstJobExecution(long jobInstanceId) throws IOException, InterruptedException {    
        return waitForFirstJobExecution(jobInstanceId, BATCH_BASE_URL);
    }
    /**
     * Waits for the first job execution after (more recent than), a specified previous job exception.
     * 
     * If there are no previous job executions at all, throw IllegalStateException.
     *   Else if the most-recent execution executionId equals previousJobExecId, then  call {@link BatchRestUtils#waitForNthJobExecution(long, String, int)} to wait for the next execution, subject
     *     to timeout and other validation.
     *   Else if the most-recent execution executionId does not equals previousJobExecId, then the only non-exception possibility is that a new execution has been created (asynchronously) since the call to this
     *     method was made.  If this is the case, validate that the second-most-recent execution id matches previousJobExecId, otherwise throw IllegalArgumentException
     * 
     * @param jobInstanceId
     * @param baseUrl
     * @param previousJobExecId
     * 
     * @return the most recent job execution JSON obj. 
     * @throws IllegalStateException 
     */
    public JsonObject waitForNextJobExecution(Long jobInstanceId, String baseUrl, Long previousJobExecId) throws IOException, InterruptedException {

        log("waitForNextJobExecution", "Entering - jobInstanceId = " + jobInstanceId + ", previousJobExecId = " + previousJobExecId);
    	JsonObject retVal;
    	
        JsonArray jobExecutions = getJobExecutionsMostRecentFirst(jobInstanceId, baseUrl);
        int initialNumExecs = jobExecutions.size();
        if (initialNumExecs > 0) {
            long mostRecentExecId = jobExecutions.getJsonObject(0).getJsonNumber("executionId").longValue();
      	
            if (mostRecentExecId != previousJobExecId) {
            	// The only way we can exit successfully from this block is if the previousJobExecId execution is the second-most-recent, and the new most-recent one has just been created.   
            	// And for this to be the case there needs to be at least two executions.
            	if (initialNumExecs == 1) {
            		throw new IllegalStateException("Found only one job execution with id = " + mostRecentExecId + ", which doesn't match expected previous id of = " + previousJobExecId);
            	} else {
            		long secondMostRecentExecId = jobExecutions.getJsonObject(1).getJsonNumber("executionId").longValue();
            		if (secondMostRecentExecId == previousJobExecId) {
            			retVal = jobExecutions.getJsonObject(0);
            			// SUCCESS
            			log("waitForNextJobExecution", "Next execution created already, returning response: " + retVal);
            			return retVal;
            		} else {
            			throw new IllegalStateException("Found more than one job execution with first id = " + mostRecentExecId + ",  second id = " + secondMostRecentExecId + ", but that's not what was expected, with expected previous id of = " + previousJobExecId);
            		}
            	}
            } else {
            	log("waitForNextJobExecution", "Most recent execution found with expected id = " + previousJobExecId);
            }
        } else {
            throw new IllegalStateException("Found zero job executions, but expected previous id of = " + previousJobExecId);
        }

        return waitForNthJobExecution(jobInstanceId, baseUrl, initialNumExecs + 1);
    }
        
    /**
     * Waits for the Nth job execution (1-indexed) to be created by polling. 
     * 
     * @param jobInstanceId
     * @param baseUrl
     * @param numExecsToWaitFor - Starting at 1 (not zero)
     * 
     * @return the most recent job execution JSON obj. This "first most recent" execution is the "Nth-ever execution" from earlier to later in time.   It would be the (N-1)th restart execution for N>=2.
     * @throws IllegalStateException if more than N executions exist at any point.  Will also throw IllegalStateException if we exhaust the wait timeout without seeing the Nth execution.
     */
    public JsonObject waitForNthJobExecution(final long jobInstanceId, final String baseUrl, final int numExecsToWaitFor) throws IOException, InterruptedException {

    	int NUM_TRIES = 30;
    	JsonArray jobExecutions = null;
    	String excMsg = null;

        log("waitForNthJobExecution", "Entering - jobInstanceId = " + jobInstanceId + ", numExecsToWaitFor = " + numExecsToWaitFor);

        for (int i = 0; i < NUM_TRIES; ++i) {
        	jobExecutions = getJobExecutionsMostRecentFirst(jobInstanceId, baseUrl);
        	int numExecs = jobExecutions.size();
        	if (numExecs > numExecsToWaitFor) {
        		excMsg = "Found: " + numExecs + ", jobExecutions, but was only looking for N = " + numExecsToWaitFor;
            	log("waitForNthJobExecution", excMsg);
            	throw new IllegalStateException(excMsg);
        	} else if (jobExecutions.size() == numExecsToWaitFor) {
            	JsonObject retVal = jobExecutions.getJsonObject(0);
            	log("waitForNthJobExecution", "Found Nth exec #" + jobExecutions.size() + ", returning response: " + retVal);
            	return retVal;
        	} else { 
        		Thread.sleep(1 * 1000);
        	}
        }

        excMsg = "waitForNthJobExecution timed out for jobInstanceId " + jobInstanceId + ", waiting for numExecsToWaitFor = " + numExecsToWaitFor + ", but only found = " + jobExecutions.size();
        log("waitForNthJobExecution", excMsg);
        throw new IllegalStateException(excMsg);
    }
    
    
    /**
     * Throws assertion failure if there is more or less than one job execution.
     * 
     * @return the first job execution id associated with the given job instance
     */
    public JsonObject getOnlyJobExecution(long jobInstanceId) throws IOException {
    	return getOnlyJobExecution(jobInstanceId, BATCH_BASE_URL);
    }

    
    /**
     * Throws assertion failure if there is more or less than one job execution.
     * 
     * @return the first job execution id associated with the given job instance
     */
    public JsonObject getOnlyJobExecution(long jobInstanceId, String baseUrl) throws IOException {

        JsonArray jobExecutions = getJobExecutionsMostRecentFirst(jobInstanceId, baseUrl);

        // Verify the job execution record.
        assertEquals(1, jobExecutions.size());
        return jobExecutions.getJsonObject(0);
    }

    /**
     * @param restartJobInstance
     * @return
     * @throws IOException
     */
    public long getMostRecentExecutionIdFromInstance(JsonObject jobInstance, String baseUrl) throws IOException {
        long jobInstanceId = jobInstance.getJsonNumber("instanceId").longValue();

        JsonArray jobExecutions = getJobExecutionsMostRecentFirst(jobInstanceId, baseUrl);

        JsonObject mostRecent = jobExecutions.getJsonObject(0);

        return mostRecent.getJsonNumber("executionId").longValue();

    }

    /**
     * @param server
     * @param executionId
     * @return
     * @throws IOException
     * @throws MalformedURLException
     * @throws
     */
    public JsonObject getJobExecutionFromExecutionId(long jobExecutionId, String baseUrl) throws IOException {

        // Get job executions for this jobinstance
        HttpURLConnection con = HttpUtils.getHttpConnection(buildURL(baseUrl + "jobexecutions/" + jobExecutionId),
                                                            HttpURLConnection.HTTP_OK,
                                                            new int[0],
                                                            10 * 1000,
                                                            HTTPRequestMethod.GET,
                                                            BatchRestUtils.buildHeaderMap(),
                                                            null);

        assertEquals(MEDIA_TYPE_APPLICATION_JSON, con.getHeaderField("Content-Type"));

        // Read the response (a single job execution)
        JsonObject jsonResponse = Json.createReader(con.getInputStream()).readObject();

        log("getJobExecutionFromExecutionId", "Response: " + jsonResponse);

        return jsonResponse;
    }
    
   
    /**
     * Uses the href from the job execution to get an array of step executions PLUS a _links array.
     * 
     * Thus for a job execution with two steps, this will return an array of size 3, two for the steps and one for the links.
     * 
     * <p>
     * Two caveats that follow closely from the spec:
     * <p><ol>
     * <li>Only the steps that actually execute for a given job execution will have associated step executions, not steps that ran in previous executions or that we didn't get to
     * <li>The array is ordered by execution start time.  This isn't the only obvious choice, order in JSL would be another conceivable choice.
     * </ol><p>
     * 
     * 
     * @param jobExecution
     * @return step executions JsonArray, sorted from earliest start time to latest, PLUS a "_links" array.
     */
    public JsonArray getStepExecutionArrayFromJobExecutionLinks(JsonObject jobExecution) throws IOException {

        //Verify that the job execution has a link to step executions
        JsonArray linkArray = jobExecution.getJsonArray("_links");
        assertNotNull("linkarray cannot be null", linkArray);

        //Getting by index rather than name since we control the order and context of the link array
        //This way we can tell if anything changes or is added to the link array.
        JsonObject stepExecLink = linkArray.getJsonObject(2); // 0 - self, 1 - job instance, 2 - step execs

        String stepExecsURL = stepExecLink.getString("href");

        HttpURLConnection con = HttpUtils.getHttpConnection(new URL(stepExecsURL),
                                              HttpURLConnection.HTTP_OK,
                                              new int[0],
                                              10 * 1000,
                                              HTTPRequestMethod.GET,
                                              buildHeaderMap(),
                                              null);

        JsonArray stepExecs = Json.createReader(con.getInputStream()).readArray();

        log("getStepExecutionArrayFromJobExecutionLinks", "Response: " + stepExecs);

        return stepExecs;
    }

    /**
     * @param server
     * @param executionId
     * @return
     * @throws IOException
     * @throws MalformedURLException
     * @throws
     */
    public JsonArray getStepExecutionFromExecutionIdAndStepName(long jobExecutionId, String stepName, String baseUrl) throws IOException {

        // Get step executions for this step
        HttpURLConnection con = HttpUtils.getHttpConnection(buildURL(baseUrl + "jobexecutions/" + jobExecutionId + "/stepexecutions/" + stepName),
                                                            HttpURLConnection.HTTP_OK,
                                                            new int[0],
                                                            10 * 1000,
                                                            HTTPRequestMethod.GET,
                                                            BatchRestUtils.buildHeaderMap(),
                                                            null);

        assertEquals(MEDIA_TYPE_APPLICATION_JSON, con.getHeaderField("Content-Type"));

        // Read the response (a single job execution)
        JsonArray jsonResponse = Json.createReader(con.getInputStream()).readArray();

        log("getStepExecutionFromExecutionIdAndStepName", "Response: " + jsonResponse);

        return jsonResponse;
    }


    /**
     * 
     * @param executionId
     * 
     * @return jobinstance JsonObject
     */
    public JsonObject getJobInstanceFromExecutionId(long jobExecutionId, String baseUrl) throws IOException {

        // Get job executions for this jobinstance
        HttpURLConnection con = HttpUtils.getHttpConnection(buildURL(baseUrl + "jobexecutions/" + jobExecutionId + "/jobinstance"),
                                                            HttpURLConnection.HTTP_OK,
                                                            new int[0],
                                                            10 * 1000,
                                                            HTTPRequestMethod.GET,
                                                            BatchRestUtils.buildHeaderMap(),
                                                            null);

        assertEquals(MEDIA_TYPE_APPLICATION_JSON, con.getHeaderField("Content-Type"));

        // Read the response (a single job instance)
        JsonObject jsonResponse = Json.createReader(con.getInputStream()).readObject();

        log("getJobInstanceFromExecutionId", "Response: " + jsonResponse);

        return jsonResponse;
    }
    
    // Utility method to get the job name from job instance JSON
    public static String getJobName(JsonObject obj) {
        return obj.getString("jobName");
    }

    // Utility method to get the job submitter from job instance JSON
    public static String getSubmitter(JsonObject obj) {
        return obj.getString("submitter");
    }

    // Utility method to get the app name from job instance JSON
    public static String getAppName(JsonObject obj) {
        return obj.getString("appName");
    }
    
    public static long execId(JsonObject obj) {
        JsonNumber num = obj.getJsonNumber("executionId");
        if (num == null) {
            throw new IllegalArgumentException("JsonObject: " + obj + " did not contain key = executionId");
        }
        return num.longValue();
    }

    public static long instanceId(JsonObject obj) {
        JsonNumber num = obj.getJsonNumber("instanceId");
        if (num == null) {
            throw new IllegalArgumentException("JsonObject: " + obj + " did not contain key = instanceId");
        }
        return num.longValue();
    }
    
    
    /**
     * Asserts that actualJobName is equal to one of two things:
     *  1) the expectedJobName parm
     *      or 
     *  2) the not set value of "" (empty string)
     * 
     * @param expectedJobName
     * @param actualJobName
     */
    public static void assertJobNamePossiblySet(String expectedJobName, String actualJobName) {
        // I'm sure there's a more elegant JUnit API here.
        if (!actualJobName.equals("")) {
            assertEquals(expectedJobName, actualJobName);
        }
    }
    
    /**
     * 
     * @param baseUrl
     * 
     * @return job instances as array (JsonArray)
     */
    public JsonArray getJobInstances(String baseUrl) throws IOException {

        // Get job executions for this jobinstance
        HttpURLConnection con = HttpUtils.getHttpConnection(buildURL(baseUrl + "jobinstances"),
                                                            HttpURLConnection.HTTP_OK,
                                                            new int[0],
                                                            10 * 1000,
                                                            HTTPRequestMethod.GET,
                                                            BatchRestUtils.buildHeaderMap(),
                                                            null);

        assertEquals(MEDIA_TYPE_APPLICATION_JSON, con.getHeaderField("Content-Type"));

        // Read the response (a single job instance)
        JsonArray jsonResponse = Json.createReader(con.getInputStream()).readArray();

        log("getJobInstances", "Response: " + jsonResponse);

        return jsonResponse;
    }

    /**
     * Poll the job instance for status until it reaches one of the terminal states.
     * Polls once a second. Times out after 60 seconds.
     * 
     * @param jobInstance JsonObject representation of job instance
     * 
     * @return the job instance record (JSON)
     * 
     * @throws RuntimeException times out after 30 seconds
     */
    public JsonObject waitForJobInstanceToFinish(JsonObject jobInstance, String baseUrl) throws IOException, InterruptedException {
    	long jobInstanceId = jobInstance.getJsonNumber("instanceId").longValue();
    	return waitForJobInstanceToFinish(jobInstanceId, baseUrl);
    }
    
    /**
     * Poll the job instance for status until it reaches one of the terminal states.
     * Polls once a second. Times out after 60 seconds.
     * 
     * @param jobInstanceId 
     * @return the job instance record (JSON)
     * 
     * @throws RuntimeException times out after 60 seconds
     */
    public JsonObject waitForJobInstanceToFinish(long jobInstanceId, String baseUrl) throws IOException, InterruptedException {
    	return waitForJobInstanceToFinish(jobInstanceId, baseUrl, 60, null, null);
    }
    
    /**
     * See {@link com.ibm.ws.jbatch.test.BatchRestUtils.waitForJobInstanceToFinish(long, String)}
     * 
     * @param jobInstanceId
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public JsonObject waitForJobInstanceToFinish(long jobInstanceId) throws IOException, InterruptedException {
    	return waitForJobInstanceToFinish(jobInstanceId, BATCH_BASE_URL);
    }

    /**
     * Poll the job instance for status until it reaches one of the completed states.
     * Polls once a second. 
     * 
     * @param timeout timeout in seconds 
     * 
     * @return the job instance record (JSON)
     * 
     * @throws RuntimeException
     */
    public JsonObject waitForJobInstanceToFinish(long jobInstanceId, String baseUrl, int timeout, String username, String password) throws IOException, InterruptedException {
    	if(username==null&&password==null){
    		return waitForJobInstanceToFinish(jobInstanceId, baseUrl, timeout, BatchRestUtils.buildHeaderMap());
    	}else{
    		return waitForJobInstanceToFinish(jobInstanceId, baseUrl, timeout, BatchRestUtils.buildHeaderMap(username,password));
    	}
    }
    
    /**
     * Poll the job instance for status until it reaches one of the completed states.
     * Polls once a second. 
     * 
     * @param timeout timeout in seconds 
     * 
     * @return the job instance record (JSON)
     * 
     * @throws RuntimeException
     */
    public JsonObject waitForJobInstanceToFinish(long jobInstanceId, String baseUrl, int timeout, Map<String, String> header) throws IOException, InterruptedException {

        JsonObject jobInstance = null;

        for (int i = 0; i < timeout; ++i) {

            // Get jobinstance record
            log("waitForJobInstanceToFinish", "Retrieving status for job instance " + jobInstanceId);

            HttpURLConnection con = HttpUtils.getHttpConnection(buildURL(baseUrl + "jobinstances/" + jobInstanceId),
                                                                HttpURLConnection.HTTP_OK,
                                                                new int[0],
                                                                10 * 1000,
                                                                HTTPRequestMethod.GET,
                                                                header,
                                                                null);

            assertEquals(MEDIA_TYPE_APPLICATION_JSON, con.getHeaderField("Content-Type"));

            jobInstance = Json.createReader(con.getInputStream()).readObject();

            log("waitForJobInstanceToFinish", "Response: jsonResponse= " + jobInstance.toString());

            if (BatchRestUtils.isDone(jobInstance)) {
                return jobInstance;
            } else {
                // Sleep a second then try again
                Thread.sleep(1 * 1000);
            }
        }

        throw new RuntimeException("Timed out waiting for job instance " + jobInstanceId + " to finish.  Last status: " + jobInstance.toString());
    }

    /**
     * Polls each job instance for status until it reaches one of the terminal states.
     * Polls once a second. Times out after 60 seconds polling for any one job.
     * 
     * @param jobInstances List of job instance ids
     * @return a list of job instance record (JSON), for each respective job instance
     * 
     * @throws RuntimeException times out after 60 seconds
     */
    public List<JsonObject> waitForJobInstancesToFinish(List<Long> jobInstances, String baseUrl) throws IOException, InterruptedException {
    	List<JsonObject> retVal = new ArrayList<JsonObject>();
    	for (Long ji : jobInstances) {
    		retVal.add(waitForJobInstanceToFinish(ji, baseUrl, 60, null, null));
    	}
    	return retVal;
    }
    
    // Doesn't require step execution to exist to start waiting
    public JsonObject waitForStepInJobExecutionToStart(long jobExecutionId, String stepName) throws IOException, InterruptedException {
        return waitForStepInJobExecutionToReachStatus(jobExecutionId, stepName, BatchStatus.STARTED);
    }

    // Doesn't require step execution to exist to start waiting
    public JsonObject waitForStepInJobExecutionToComplete(long jobExecutionId, String stepName) throws IOException, InterruptedException {
        JsonObject stepExecution = waitForStepInJobExecutionToFinish(jobExecutionId, stepName);
        String batchStatus = stepExecution.getString("batchStatus");
        if (batchStatus.equals(BatchStatus.COMPLETED.toString())) {
            return stepExecution;
        } else {
            throw new RuntimeException("Expected COMPLETED state, found terminating state of: " + batchStatus);
        }
    }
    
    // Doesn't require step execution to exist to start waiting
    public JsonObject waitForStepInJobExecutionToReachStatus(long jobExecutionId, String stepName, BatchStatus batchStatus) throws IOException, InterruptedException {

        JsonObject stepExecution = null;
        int[] possibleResponse = new int[1];
        possibleResponse[0] = HttpURLConnection.HTTP_INTERNAL_ERROR;

        log("waitForStepInJobExecutionToReachStatus", "Begin loop for status = " + batchStatus);
        for (int i = 0; i < 30; i++) {

            // Get job executions for this step
            HttpURLConnection con = HttpUtils.getHttpConnection(buildURL("/ibm/api/batch/jobexecutions/" + jobExecutionId + "/stepexecutions"),
                                                                HttpURLConnection.HTTP_OK,
                                                                possibleResponse,
                                                                10 * 1000,
                                                                HTTPRequestMethod.GET,
                                                                BatchRestUtils.buildHeaderMap(),
                                                                null);

            //An internal server error could occur if no step executions have started yet. Wait 1 second and try again.
            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                assertEquals(MEDIA_TYPE_APPLICATION_JSON, con.getHeaderField("Content-Type"));

                // Read the response (all step executions in the given job execution id)
                JsonArray jsonResponse = Json.createReader(con.getInputStream()).readArray();
                log("waitForStepInJobExecutionToReachStatus", "OK Response: " + jsonResponse);

                for (int j = 0; j < jsonResponse.size(); j++) {
                    stepExecution = jsonResponse.getJsonObject(j);
                    if (stepExecution.getJsonString("stepName") != null &&
                        stepExecution.getString("stepName").equals(stepName) &&
                        stepExecution.getString("batchStatus").equals(batchStatus.toString())) {
                        return stepExecution;
                    }
                }
            }
            Thread.sleep(1 * 1000);
        }
        throw new RuntimeException("Timed out waiting for step to reach status: " + batchStatus + ".  Last state: " + stepExecution == null ? "<null>" : stepExecution.toString());
    }

    public JsonObject waitForStepInJobExecutionToFinish(long jobExecutionId, String stepName) throws IOException, InterruptedException {

        JsonObject stepExecution = null;
        int[] possibleResponse = new int[1];
        possibleResponse[0] = HttpURLConnection.HTTP_INTERNAL_ERROR;
        for (int i = 0; i < 30; i++) {

            log("waitForStepInJobExecutionToFinish", "Begin loop");

            // Get job executions for this step
            HttpURLConnection con = HttpUtils.getHttpConnection(buildURL("/ibm/api/batch/jobexecutions/" + jobExecutionId + "/stepexecutions"),
                                                                HttpURLConnection.HTTP_OK,
                                                                possibleResponse,
                                                                10 * 1000,
                                                                HTTPRequestMethod.GET,
                                                                BatchRestUtils.buildHeaderMap(),
                                                                null);

            //An internal server error could occur if no step executions have started yet. Wait 1 second and try again.
            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                assertEquals(MEDIA_TYPE_APPLICATION_JSON, con.getHeaderField("Content-Type"));

                // Read the response (all step executions in the given job execution id)
                JsonArray jsonResponse = Json.createReader(con.getInputStream()).readArray();
                log("waitForStepInJobExecutionToFinish", "OK Response: " + jsonResponse);

                for (int j = 0; j < jsonResponse.size(); j++) {
                    stepExecution = jsonResponse.getJsonObject(j);
                    if (stepExecution.getJsonString("stepName") != null &&
                        stepExecution.getString("stepName").equals(stepName) &&
                        isDone(stepExecution)) {
                        return stepExecution;
                    }
                }
                log("waitForStepInJobExecutionToFinish", "Didn't see terminating status");
            }
            Thread.sleep(1 * 1000);
        }
        throw new RuntimeException("Timed out waiting for step to reach final status. ast state: " + stepExecution.toString());
    }
    
    /**
     * Poll the job instance for status until its batchStatus changes to STARTED.
     * Polls once a second. Times out after 30 seconds.
     * 
     * @return the job instance record (JSON)
     * 
     * @throws RuntimeException times out after 30 seconds
     */
    public JsonObject waitForJobInstanceToStart(long jobInstanceId) throws IOException, InterruptedException {
    	return waitForJobInstanceToStart(jobInstanceId,BATCH_BASE_URL);
    }

    /**
     * Poll the job instance for status until its batchStatus changes to STARTED.
     * Polls once a second. Times out after 30 seconds.
     * 
     * @return the job instance record (JSON)
     * 
     * @throws RuntimeException times out after 30 seconds
     */
    public JsonObject waitForJobInstanceToStart(long jobInstanceId, String baseUrl) throws IOException, InterruptedException {

        JsonObject jobInstance = null;

        for (int i = 0; i < 30; ++i) {

            // Get jobinstance record
            log("waitForJobInstanceToStart", "Retrieving status for job instance " + jobInstanceId);

            HttpURLConnection con = HttpUtils.getHttpConnection(buildURL(baseUrl + "jobinstances/" + jobInstanceId),
                                                                HttpURLConnection.HTTP_OK,
                                                                new int[0],
                                                                10 * 1000,
                                                                HTTPRequestMethod.GET,
                                                                BatchRestUtils.buildHeaderMap(),
                                                                null);

            assertEquals(MEDIA_TYPE_APPLICATION_JSON, con.getHeaderField("Content-Type"));

            jobInstance = Json.createReader(con.getInputStream()).readObject();

            log("waitForJobInstanceToStart", "Response: jsonResponse= " + jobInstance.toString());

            if (jobInstance.getString("batchStatus").equals(BatchStatus.STARTED.toString())) {
                return jobInstance;
            } else {
                // Sleep a second then try again
                Thread.sleep(1 * 1000);
            }
        }

        throw new RuntimeException("Timed out waiting for job instance " + jobInstanceId + " to start.  Last status: " + jobInstance.toString());
    }
    
    /**
     * Poll the job execution for status until its batchStatus changes to STARTED.
     * Polls once a second. Times out after 30 seconds.
     * 
     * @return the job execution record (JSON)
     * 
     * @throws RuntimeException times out after 30 seconds
     */
    public JsonObject waitForJobExecutionToStart(long jobExecutionId, String baseUrl) throws IOException, InterruptedException {
    	return waitForJobExecutionToReachStatus(jobExecutionId, baseUrl, BatchStatus.STARTED);
    }
    
    public JsonObject waitForJobExecutionToReachStatus(long jobExecutionId, String baseUrl, BatchStatus batchStatus) throws IOException, InterruptedException {

        JsonObject jobExecution = null;
        int[] possibleResponse = new int[1];
        possibleResponse[0] = HttpURLConnection.HTTP_INTERNAL_ERROR;
        int NUM_RETRIES = 30;

       log("waitForJobExecutionToReachStatus", "Begin loop for status = " + batchStatus);

        for (int i = 0; i < NUM_RETRIES; i++) {

            // Get job executions for this step
            HttpURLConnection con = HttpUtils.getHttpConnection(buildURL(baseUrl + "jobexecutions/" + jobExecutionId),
                                                                HttpURLConnection.HTTP_OK,
                                                                possibleResponse,
                                                                10 * 1000,
                                                                HTTPRequestMethod.GET,
                                                                BatchRestUtils.buildHeaderMap(),
                                                                null);

            //An internal server error could occur if no step executions have started yet. Wait 1 second and try again.
            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                assertEquals(MEDIA_TYPE_APPLICATION_JSON, con.getHeaderField("Content-Type"));

                // Read the response (all step executions in the given job execution id)
                jobExecution = Json.createReader(con.getInputStream()).readObject();
                log("waitForJobExecutionToReachStatus", "OK Response: " + jobExecution);
                String actualStatus = jobExecution.getString("batchStatus");
                if (actualStatus.equals(batchStatus.toString())) {
                	return jobExecution;
                }

                log("waitForJobExecutionToReachStatus", "Saw status = " + actualStatus + ", but waiting for status: " + batchStatus);
            }
            Thread.sleep(1 * 1000);
        }
        throw new IllegalStateException("Timed out waiting for job to reach status: " + batchStatus + ".  Last state: " + jobExecution.toString());
    }

    
    
    
    /**
     * Poll the partitions from the step execution for status until any one's batchStatus changes to STARTED.
     * Polls once a second. Times out after 30 seconds.
     * 
     * Assumes stepExecution already exists
     * 
     * @return the partition array record (JSON)
     * 
     * @throws RuntimeException times out after 30 seconds
     */
    public JsonArray waitForAnyPartitionToStart(long jobExecutionId, String stepName, String baseUrl) throws IOException, InterruptedException {

    	JsonObject stepExecution = null;
    	JsonArray partitions = null;

        for (int i = 0; i < 30; ++i) {

        	stepExecution = getStepExecutionFromExecutionIdAndStepName(jobExecutionId, stepName, baseUrl).getJsonObject(0);
        	partitions = stepExecution.getJsonArray("partitions");
        	
            log("waitForAnyPartitionToStart", "Partitions = " + partitions);
        	if (partitions != null) {
        		for (int j = 0; j < partitions.size(); j++) {
        			if (partitions.getJsonObject(j).getString("batchStatus").equals(BatchStatus.STARTED.toString())) {
        				return partitions;
        			}
        		}
        	}
            // Sleep a second then try again
            Thread.sleep(1 * 1000);
        }

        throw new RuntimeException("Timed out waiting for a partition to start.  Last step execution: " + stepExecution.toString());
    }
    
    /**
     * 
     * Returns array of status holders (job instance, partition, whatever) that matches by BatchStatus
     * 
     * @param statusHolderArray
     * @param targetStatus
     * @return
     */
    public List<JsonObject> filterArrayByBatchStatus(JsonArray statusHolderArray, BatchStatus targetStatus) {
    	List<JsonObject> retVal = new ArrayList<JsonObject>();
    	for (int i = 0; i < statusHolderArray.size(); i++) {
    		JsonObject obj = statusHolderArray.getJsonObject(i);
    		if (obj.getString("batchStatus").equals(targetStatus.toString())) {
    			retVal.add(obj);
    		}
    	}
    	return retVal;
    }

    /**
     * 
     * Get the third part (serverName) of the (host, WLP_USER_DIR, serverName) triplet
     * 
     * @param remotablePartition
     * @return
     */
    public String getServerNameForRemotablePartition(JsonObject remotablePartition) {
    	String serverId = remotablePartition.getString("serverId");
    	return serverId.substring(serverId.lastIndexOf("/") + 1);
    }
    
    /**
     * @return a URL to the target server
     */
    public URL buildURL(String path) throws MalformedURLException {
        URL retMe = new URL("https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + path);
        log("buildURL", retMe.toString());
        return retMe;
    }
    
    /**
     * @return a URL to the target server
     */
    public URL buildURL(String hostname, int port, String path) throws MalformedURLException {
    	URL retMe = new URL("https://" + hostname + ":" + port + path);
    	log("buildURL", retMe.toString());
    	return retMe;
    }
    
    /**
     * @return a URL to the target server using its IP address instead of hostname.
     */
    public URL buildURLUsingIPAddr(String path) throws MalformedURLException, UnknownHostException {
        return ("localhost".equals(server.getHostname())) ? buildURL(InetAddress.getLocalHost().getHostAddress(), server.getHttpDefaultSecurePort(), path) : buildURL(path);
    }

    /**
     * @return map of headers for rest api requests, using username of: {@link ADMIN_USERNAME}
     */
    public static Map<String, String> buildHeaderMap() {
        return buildHeaderMap(ADMIN_USERNAME, ADMIN_PASS);
    }
    
    /*
     * @return map of headers with the supplied user/pass for rest api requests
     */
    public static Map<String, String> buildHeaderMap(String username, String password){
    	 Map<String, String> headers = new HashMap<String, String>();
         headers.put("Authorization", "Basic " + Base64Coder.base64Encode(username + ":" + password));
         headers.put("Content-Type", BatchRestUtils.MEDIA_TYPE_APPLICATION_JSON);
         return headers;

    }

    /**
     * This could be a job instance, job execution, step execution (anything that has a 
     * "batchStatus" property).
     * 
     * @param batchStatusHolder 
     * 
     * @return true if batchStatusHolder.batchStatus is any of STOPPED, FAILED, COMPLETED, ABANDONED.
     */
    public static boolean isDone(JsonObject batchStatusHolder) {
        String batchStatus = batchStatusHolder.getString("batchStatus");
        return ("STOPPED".equals(batchStatus) ||
                "FAILED".equals(batchStatus) ||
                "COMPLETED".equals(batchStatus) || "ABANDONED".equals(batchStatus));
    }

    /**
     * @return true if compareMe is equal to any of toUs.
     */
    public static boolean isStatus(String compareMe, String... toUs) {
        return Arrays.asList(toUs).contains(compareMe);
    }

    /**
     * @param server
     * @param longValue
     * @return
     * @throws IOException
     */
    public JsonObject getFinalJobExecution(long jobExecutionId, String baseUrl) throws IOException {
        long curTime = System.currentTimeMillis();
        long elapsedTime = System.currentTimeMillis() - curTime;

        while (elapsedTime < POLLING_TIMEOUT_MILLISECONDS) {
            JsonObject exec = getJobExecutionFromExecutionId(jobExecutionId, baseUrl);
            if (isDone(exec)) {
                return exec;
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException("Exceeed timeout without job reaching terminating status.");
    }

    /**
     * @param jobInstance
     * @return
     */
    public long getOnlyExecutionIdFromInstance(JsonObject jobInstance, String baseUrl) throws IOException {

        long jobInstanceId = jobInstance.getJsonNumber("instanceId").longValue();
        JsonObject onlyJobExecution = getOnlyJobExecution(jobInstanceId, baseUrl);
        return onlyJobExecution.getJsonNumber("executionId").longValue();
    }
    
    /**
     * @param jobExec JsonObject 
     * @param jobName job name embedded in logpath we'll search against
     * @return execution date in the form yyyy-mm-dd, as obtained from the job log or 'null' if no match found
     */
    public String getLogDateFromJobExecution(JsonObject jobExec, String jobName) {

        String logpath = jobExec.getJsonString("logpath").getString();

        String winOrUnixSeparator = "[/\\\\]";
        String patternStr = ".*" + jobName + winOrUnixSeparator + "([0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9])";
        Pattern pattern = Pattern.compile(patternStr);
        Matcher m = pattern.matcher(logpath);
        String logDate = null;
        while (m.find()) {
            logDate = m.group(1);
        }

        if (logDate == null) {
        	log("getLogDateFromJobExecution", "No match for jobName = " + jobName + "\nlogpath = " + logpath + "\npatternStr = " + patternStr);
        }
        
        return logDate;
    }

    /**
     * Inner class that holds the output from a process.
     */
    private static class ProcessOutput {
        private final List<String> sysout;
        private final List<String> syserr;

        ProcessOutput(Process p) throws Exception {
            sysout = new ArrayList<String>();
            syserr = new ArrayList<String>();

            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while (br.ready()) {
                sysout.add(br.readLine());
            }
            br.close();

            br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while (br.ready()) {
                syserr.add(br.readLine());
            }
            br.close();
        }

        void printOutput() {
        	log("processOutput", "SYSOUT: ");
            System.out.println("SYSOUT:");
            for (String x : sysout) {
            	log("processOutput", " " + x);
                System.out.println(" " + x);
            }

            log("processOutput", "SYSERR: ");
            System.out.println("SYSERR:");
            for (String x : syserr) {
            	log("processOutput", " " + x);
                System.out.println(" " + x);
            }
        }

        private static String search(List<String> list, String z) {
            for (String x : list) {
                if (x.contains(z)) {
                    return x;
                }
            }

            return null;
        }

        String getLineInSysoutContaining(String s) {
            return search(sysout, s);
        }
    }
    

    private ProcessBuilder getProcessBuilder(LibertyServer server) throws Exception {
        String scriptName;
        String serverName = server.getServerName();
        String installRoot = server.getInstallRoot();
        Machine machine = server.getMachine();

        if (machine.getOperatingSystem() == OperatingSystem.WINDOWS) {
            scriptName = installRoot + File.separator + "bin" + File.separator + "ddlGen.bat";
        } else {
            scriptName = installRoot + File.separator + "bin" + File.separator + "ddlGen";
        }

        return new ProcessBuilder(scriptName, "generate", serverName).directory(new File(installRoot));
    }

    public String getBatchDDL(LibertyServer server) throws Exception {

        ProcessBuilder processBuilder = getProcessBuilder(server);
        Process process = processBuilder.start();
        int returnCode = process.waitFor();
        
        ProcessOutput processOutput = new ProcessOutput(process);
        processOutput.printOutput();

        if (returnCode != 0)
            throw new Exception("Expected return code 0, actual return code " + returnCode);

        String successMessage = processOutput.getLineInSysoutContaining("CWWKD0107I");
        if (successMessage == null)
            throw new Exception("Output did not contain success message CWWKD0107I");

        File outputPath = new File(successMessage.substring(72));
        if (outputPath.exists() == false)
            throw new Exception("Output path did not exist: " + outputPath.toString());

        String[] ddlFiles = outputPath.list();
        if (ddlFiles.length == 0)
            throw new Exception("There was no output in the output directory: " + outputPath.toString());

        File ddlFile = null;

        for (String fileName : ddlFiles) {
            if ("databaseStore[BatchDatabaseStore]_batchPersistence.ddl".equals(fileName))
                ddlFile = new File(outputPath, fileName);
        }

        if (!ddlFile.equals(null))
            return ddlFile.getAbsolutePath();
        else
            return null;
    }

    /*
     * Change the schema value to user1 in case of a oracle database,
     * or dbuser1 in the case of a sql server database
     */
    public static void updateSchemaIfNecessary(LibertyServer server) throws Exception {

        ServerConfiguration config = server.getServerConfiguration();
        Bootstrap bs = Bootstrap.getInstance();
        String dbType = bs.getValue(BootstrapProperty.DB_VENDORNAME.getPropertyName());
        if (dbType != null && (dbType.equalsIgnoreCase("oracle") || dbType.equalsIgnoreCase("sqlserver")))  {

            String user1 = bs.getValue(BootstrapProperty.DB_USER1.getPropertyName());
            for (DatabaseStore ds : config.getDatabaseStores()) {
                ds.setSchema(user1);
            }
        }

        server.updateServerConfiguration(config);
        if (server.isStarted()) {
            server.waitForConfigUpdateInLogUsingMark(null);
        }

    }
    
    /**
     * Submits a job using an inline jsl file (vs. jsl included in the .war file)
     * 
     * @param appName
     * @param jobName
     * @param xml
     * @return
     * @throws Exception
     */
    public JsonObject submitJob(String appName, String jobName, String xml, String baseUrl) throws Exception {
        JsonObjectBuilder payloadBuilder = Json.createObjectBuilder()
                        .add("applicationName", appName)
                        .add("jobXMLName", jobName);

        return submitJobInlineJSL(payloadBuilder.build(), xml, baseUrl);
    }

    /**
     * Submits an Inline JSL job
     * 
     * @param jobSubmitPayload
     * @param xml
     * @return
     * @throws Exception
     */
    @SuppressWarnings("deprecation")
    public JsonObject submitJobInlineJSL(JsonObject jobSubmitPayload, String xml, String baseUrl) throws Exception {

        String boundary = "-----------------------------49424d5f4a4241544348";
        JsonObject jsonObject = null;

        log("submitJob", "Request: jobDataPayload= " + jobSubmitPayload.toString());
        log("submitJob", "Request: jslPayload= " + xml);

        // Create the HTTP client
        HttpClient httpClient = createHTTPClient();
        HttpPost httpPost = new HttpPost(buildURL(baseUrl + "jobinstances").toString());

        // Add headers on httpPost
        httpPost.addHeader("Authorization", "Basic " + Base64Coder.base64Encode("bob:bobpwd"));
        httpPost.addHeader("Content-Type", BatchRestUtils.MEDIA_TYPE_MUTLITPART_FORM_WITH_BOUNDARY + boundary);

        // Multipart builder
        MultipartEntityBuilder eBuilder = MultipartEntityBuilder.create();
        eBuilder.addPart("jobdata", new StringBody(jobSubmitPayload.toString(), ContentType.APPLICATION_JSON));
        eBuilder.addPart("jsl", new StringBody(xml, ContentType.APPLICATION_XML));;
        eBuilder.setBoundary(boundary);
        HttpEntity multipart = eBuilder.build();
        httpPost.setEntity(multipart);

        // Execute the POST HTTP request and process the response
        HttpResponse response = httpClient.execute(httpPost);
        HttpEntity responseEntity = response.getEntity();

        String entityString = EntityUtils.toString(responseEntity);
        log("submitJob", "Response: payload= " + entityString);

        JsonReader jsonReader = Json.createReader(new StringReader(entityString));
        jsonObject = jsonReader.readObject();
        jsonReader.close();

        httpClient.getConnectionManager().shutdown();

        return jsonObject;
    }

    /**
     * Utility method to read a JSL file from the file system
     * 
     * @param file
     * @return
     * @throws IOException
     */
    public String readInlineJSLFromFile(String file) throws IOException
    {
        return new String(Files.readAllBytes(Paths.get(server.getInstallRoot() + File.separator + file)), StandardCharsets.UTF_8);
    }

    /**
     * Creates the HTTP Client which will trust all SSL certificates
     * 
     * @return
     * @throws Exception
     */
    @SuppressWarnings("deprecation")
    public static HttpClient createHTTPClient() throws Exception {

        SSLContext ctx = SSLContext.getInstance("SSL");
        X509TrustManager tm = new X509TrustManager() {

            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] chain,
                                           String authType) throws CertificateException {}

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] chain,
                                           String authType) throws CertificateException {}

        };

        ctx.init(null, new TrustManager[] { tm }, null);
        org.apache.http.conn.ssl.SSLSocketFactory ssf = new org.apache.http.conn.ssl.SSLSocketFactory(ctx);
        ssf.setHostnameVerifier(org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        org.apache.http.conn.scheme.SchemeRegistry registry = new org.apache.http.conn.scheme.SchemeRegistry();
        registry.register(new org.apache.http.conn.scheme.Scheme("https", 443, ssf));
        org.apache.http.conn.ClientConnectionManager cliMgr = new org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager(registry);

        return new org.apache.http.impl.client.DefaultHttpClient(cliMgr);

    }
    
    public String executeSql(LibertyServer server, String dataSourceJndi, String sql) throws Exception {

        String userName = "user";
        String password = "pass";

        ServerConfiguration configuration = server.getServerConfiguration();
        ConfigElementList<DataSource> dataSourcesList = configuration.getDataSources();
        Iterator<DataSource> dataSourcesListIterator = dataSourcesList.iterator();

        while (dataSourcesListIterator.hasNext()) {
            DataSource dataSource = dataSourcesListIterator.next();

            if (dataSource.getJndiName().equals(dataSourceJndi)) {
                Set<DataSourceProperties> dataSourcePropertiesList = dataSource.getDataSourceProperties();
                Iterator<DataSourceProperties> dataSourcePropertiesListIterator = dataSourcePropertiesList.iterator();

                while (dataSourcePropertiesListIterator.hasNext()) {
                    DataSourceProperties dataSourceProperties = dataSourcePropertiesListIterator.next();
                    userName = dataSourceProperties.getUser();
                    password = dataSourceProperties.getPassword();
                    break;
                }
            }

            if (!userName.equals("user"))
                break;
        }

        return new DbServletClient()
                        .setDataSourceJndi(dataSourceJndi)
                        .setDataSourceUser(userName, password)
                        .setHostAndPort(server.getHostname(), server.getHttpDefaultPort())
                        .setSql(sql)
                        .executeQuery();
    }
    
    public String executeSql(LibertyServer server, String hostname, int port, String dataSourceJndi, String sql) throws Exception {

        String userName = "user";
        String password = "pass";

        ServerConfiguration configuration = server.getServerConfiguration();
        ConfigElementList<DataSource> dataSourcesList = configuration.getDataSources();
        Iterator<DataSource> dataSourcesListIterator = dataSourcesList.iterator();

        while (dataSourcesListIterator.hasNext()) {
            DataSource dataSource = dataSourcesListIterator.next();

            if (dataSource.getJndiName().equals(dataSourceJndi)) {
                Set<DataSourceProperties> dataSourcePropertiesList = dataSource.getDataSourceProperties();
                Iterator<DataSourceProperties> dataSourcePropertiesListIterator = dataSourcePropertiesList.iterator();

                while (dataSourcePropertiesListIterator.hasNext()) {
                    DataSourceProperties dataSourceProperties = dataSourcePropertiesListIterator.next();
                    userName = dataSourceProperties.getUser();
                    password = dataSourceProperties.getPassword();
                    break;
                }
            }

            if (!userName.equals("user"))
                break;
        }

        return new DbServletClient()
                        .setDataSourceJndi(dataSourceJndi)
                        .setDataSourceUser(userName, password)
                        .setHostAndPort(hostname, port)
                        .setSql(sql)
                        .executeQuery();
    }

    public String executeSqlUpdate(LibertyServer server, String dataSourceJndi, String sql) throws Exception {

        String userName = "user";
        String password = "pass";

        ServerConfiguration configuration = server.getServerConfiguration();
        ConfigElementList<DataSource> dataSourcesList = configuration.getDataSources();
        Iterator<DataSource> dataSourcesListIterator = dataSourcesList.iterator();

        while (dataSourcesListIterator.hasNext()) {
            DataSource dataSource = dataSourcesListIterator.next();

            if (dataSource.getJndiName().equals(dataSourceJndi)) {
                Set<DataSourceProperties> dataSourcePropertiesList = dataSource.getDataSourceProperties();
                Iterator<DataSourceProperties> dataSourcePropertiesListIterator = dataSourcePropertiesList.iterator();

                while (dataSourcePropertiesListIterator.hasNext()) {
                    DataSourceProperties dataSourceProperties = dataSourcePropertiesListIterator.next();
                    userName = dataSourceProperties.getUser();
                    password = dataSourceProperties.getPassword();
                    break;
                }
            }

            if (!userName.equals("user"))
                break;
        }

        HttpURLConnection conn = new DbServletClient()
                        .setDataSourceJndi(dataSourceJndi)
                        .setDataSourceUser(userName, password)
                        .setHostAndPort(server.getHostname(), server.getHttpDefaultPort())
                        .setSql(sql)
                        .executeUpdate();

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String retVal = br.readLine();
        br.close();

        return retVal;
    }
    
    

    /**
     * In between submissions, wait for each job to finish
     */
    public List<JsonObject> submitMultipleJobsSerially(int numJobs, String appName, String jobName, String baseUrl) throws Exception {
       	List<JsonObject> retVal = new ArrayList<JsonObject>();
    	for (int i = 0; i < numJobs; i++) {
    		retVal.add(submitJobAndWaitUntilFinished(appName, jobName, baseUrl));
    	}
    	return retVal;
    }
    
    /**
     * Utility method to run multiple jobs
     * 
     * @param numberOfJobs
     * @param jobName
     * @return
     * @throws Exception
     */
    public ArrayList<Long> runMultipleJobs(int numberOfJobs, String jobName, String URL) throws Exception
    {
       return runMultipleJobs(numberOfJobs, jobName, URL, ADMIN_USERNAME, ADMIN_PASS);
    }
    
    /**
     * Utility method to submit multiple jobs
     * 
     * @param numberOfJobs
     * @param jobName
     * @param username
     * @param password
     * @return
     * @throws Exception
     */
    public ArrayList<Long> runMultipleJobs(int numberOfJobs, String jobName, String URL, String username, String password) throws Exception
    {
        ArrayList<Long> submittedJobs = new ArrayList<Long>();
        JsonObject instance = null;

        for (int i = 0; i < numberOfJobs; i++) {
            instance = submitJob("SimpleBatchJob", jobName, URL, username, password);
            long jobInstanceId = instance.getJsonNumber("instanceId").longValue();
            submittedJobs.add(i, jobInstanceId);
            waitForJobInstanceToFinish(jobInstanceId, URL);
        }

        return submittedJobs;
    }
    
    public ArrayList<Long> runMultipleJobs(int numberOfJobs, String jobName, Properties jobParameters, String URL) throws Exception
    {
    	ArrayList<Long> submittedJobs = new ArrayList<Long>();
        JsonObject instance = null;

        for (int i = 0; i < numberOfJobs; i++) {
            instance = submitJob("SimpleBatchJob", jobName, jobParameters, URL);
            long jobInstanceId = instance.getJsonNumber("instanceId").longValue();
            submittedJobs.add(i, jobInstanceId);
            waitForJobInstanceToFinish(jobInstanceId, URL);
        }

        return submittedJobs;
    }
    
    /**
     * Call the batch api /ibm/api/batch/{path}
     *
     * @param server
     * @param path
     * @return JsonStructure
     * @throws Exception
     */
    public JsonStructure getBatchApi(String path) throws Exception {

        JsonStructure struct = null;

        HttpURLConnection con = HttpUtils.getHttpConnection(buildURL("/ibm/api/batch" + path),
                                                            HttpURLConnection.HTTP_OK,
                                                            new int[0],
                                                            10 * 1000,
                                                            HTTPRequestMethod.GET,
                                                            BatchRestUtils.buildHeaderMap(),
                                                            null);

        struct = Json.createReader(con.getInputStream()).read();

        log("getBatchApi", "Response: jsonResponse= " + struct.toString());

        return struct;
    }

    
    /**
     * Checks the DB for the given jobInstance and jobName prior to a purge
     * @param jobInstanceId
     * @param jobName
     * @param method
     * @throws Exception
     */
    public void checkDBEntriesCreated(Long jobInstanceId, String jobName, String method) throws Exception {
        String schema = getDatabaseSchema();
        String tp = getDatabaseTablePrefix();

        String queryInstance = "SELECT JOBINSTANCEID,jobname FROM " + schema + "." + tp + "JOBINSTANCE WHERE JOBINSTANCEID = " + jobInstanceId;
        String queryExecution = "SELECT FK_JOBINSTANCEID,batchstatus FROM " + schema + "." + tp + "JOBEXECUTION WHERE FK_JOBINSTANCEID = " + jobInstanceId;
        String queryStepExecution = "SELECT B.BATCHSTATUS FROM " + schema + "." + tp + "JOBEXECUTION A INNER JOIN " + schema + "." + tp + "STEPTHREADEXECUTION  B "
                                    + "ON A.JOBEXECID = B.FK_JOBEXECID WHERE A.FK_JOBINSTANCEID = " + jobInstanceId;

        String response = executeSql(server, "jdbc/batch", queryInstance);

        assertTrue("Job instance " + jobInstanceId + " not found in database, response was: " + response,
                   response.contains(Long.toString(jobInstanceId)) && response.contains(jobName));

        response = executeSql(server, "jdbc/batch", queryExecution);
        assertTrue("Job execution for instance " + jobInstanceId + " not found in database, response was: " + response,
                   response.contains(Long.toString(jobInstanceId) + "|" + BatchStatus.COMPLETED.ordinal()));

        response = executeSql(server, "jdbc/batch", queryStepExecution);
        assertTrue("Step execution for instance " + jobInstanceId + " not found in database, response was: " + response,
                   response.contains(BatchStatus.COMPLETED.ordinal() + ""));

    }
    
    public void checkFileSystemEntriesCreatedWithDate(Long jobInstanceId, String jobName, String method, String dateDirectory) {
        File joblogsDir = new File(server.getServerRoot() + File.separator + "logs" + File.separator + "joblogs");
        File instanceDir = new File(joblogsDir, jobName + File.separator
                                                + dateDirectory + File.separator
                                                + "instance." + jobInstanceId);
        if (instanceDir.exists()) {
            log(method, "Contents of directory " + instanceDir.getAbsolutePath() + ":");
            for (String s : instanceDir.list()) {
                log(method, "  > " + s);
            }
        } else {
            fail("Job log directory not found before purge: " + instanceDir.getAbsolutePath());
        }
    }

    /**
     * Checks the File System for the given jobInstance and jobName prior to a purge
     * @param jobInstanceId
     * @param jobName
     * @param method
     */
    public void checkFileSystemEntriesCreated(Long jobInstanceId, String jobName, String method) {
        File joblogsDir = new File(server.getServerRoot() + File.separator + "logs" + File.separator + "joblogs");
        File instanceDir = new File(joblogsDir, jobName + File.separator
                                                + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + File.separator
                                                + "instance." + jobInstanceId);
        if (instanceDir.exists()) {
            log(method, "Contents of directory " + instanceDir.getAbsolutePath() + ":");
            for (String s : instanceDir.list()) {
                log(method, "  > " + s);
            }
        } else {
            fail("Job log directory not found before purge: " + instanceDir.getAbsolutePath());
        }
    }
    
    /**
     * Checks the File System for the given jobInstance and jobName prior to a purge
     * @param jobInstanceId
     * @param jobName
     * @param method
     */
    public void checkFileSystemEntriesCreated(Long jobInstanceId, String date, String jobName, String method) {
        File joblogsDir = new File(server.getServerRoot() + File.separator + "logs" + File.separator + "joblogs");
        File instanceDir = new File(joblogsDir, jobName + File.separator
                                                + date + File.separator
                                                + "instance." + jobInstanceId);
        if (instanceDir.exists()) {
            log(method, "Contents of directory " + instanceDir.getAbsolutePath() + ":");
            for (String s : instanceDir.list()) {
                log(method, "  > " + s);
            }
        } else {
            fail("Job log directory not found before purge: " + instanceDir.getAbsolutePath());
        }
    }

    /**
     * Checks the DB for the given jobInstance and jobName after a purge
     * @param jobInstanceId
     * @param jobName
     * @param method
     * @throws Exception
     */
    public void checkDBEntriesRemoved(Long jobInstanceId, String jobName, String method) throws Exception {
        String schema = getDatabaseSchema();
        String tp = getDatabaseTablePrefix();

        String queryInstance = "SELECT JOBINSTANCEID,jobname FROM " + schema + "." + tp + "JOBINSTANCE WHERE JOBINSTANCEID = " + jobInstanceId;
        String queryExecution = "SELECT FK_JOBINSTANCEID,batchstatus FROM " + schema + "." + tp + "JOBEXECUTION WHERE FK_JOBINSTANCEID = " + jobInstanceId;
        String queryStepExecution = "SELECT B.BATCHSTATUS FROM " + schema + "." + tp + "JOBEXECUTION A INNER JOIN " + schema + "." + tp + "STEPTHREADEXECUTION  B "
                                    + "ON A.JOBEXECID = B.FK_JOBEXECID WHERE A.FK_JOBINSTANCEID = " + jobInstanceId;

        String response = executeSql(server, "jdbc/batch", queryInstance);
        assertTrue("Job instance data remained in the database after purge, response was: " + response, response.isEmpty());

        response = executeSql(server, "jdbc/batch", queryExecution);
        assertTrue("Job execution data remained in the database after purge, response was: " + response, response.isEmpty());

        response = executeSql(server, "jdbc/batch", queryStepExecution);
        assertTrue("Step execution data remained in the database after purge, response was: " + response, response.isEmpty());
    }
    
    

    /**
     * Checks the File System for the given jobInstance and jobName after a purge
     * @param jobInstanceId
     * @param jobName
     * @param method
     */
    public void checkFileSystemEntriesRemoved(Long jobInstanceId, String jobName, String method) {
        File joblogsDir = new File(server.getServerRoot() + File.separator + "logs" + File.separator + "joblogs");
        File instanceDir = new File(joblogsDir, jobName + File.separator
                                                + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + File.separator
                                                + "instance." + jobInstanceId);

        assertTrue("Job log directory remained after purge: " + instanceDir.getAbsolutePath(),
                   !instanceDir.exists());
    }
    
    /**
     * Checks the File System for the given jobInstance and jobName after a purge
     * @param jobInstanceId
     * @param date
     * @param jobName
     * @param method
     */
    public void checkFileSystemEntriesRemoved(Long jobInstanceId, String date, String jobName, String method) {
        File joblogsDir = new File(server.getServerRoot() + File.separator + "logs" + File.separator + "joblogs");
        File instanceDir = new File(joblogsDir, jobName + File.separator
                                                + date + File.separator
                                                + "instance." + jobInstanceId);

        assertTrue("Job log directory remained after purge: " + instanceDir.getAbsolutePath(),
                   !instanceDir.exists());
    }
    
    /**
     * @return the most recent (highest numbered) job instance ID
     */
    public long getHighestNumJobInstanceId() throws Exception {
    	String schema = getDatabaseSchema();
        String tp = getDatabaseTablePrefix();
        
        String queryInstance = "SELECT JOBINSTANCEID FROM " + schema + "." + tp + "JOBINSTANCE ORDER BY JOBINSTANCEID DESC";
        
        String instanceIds = executeSql(server, "jdbc/batch", queryInstance);
        return Long.parseLong(instanceIds.substring(1, instanceIds.indexOf("]")));
    }
    
    /**
     * @return the most recent (highest numbered) job instance ID
     */
    public long getJobInstanceCountFromDB() throws Exception {
    	String schema = getDatabaseSchema();
        String tp = getDatabaseTablePrefix();
        
        String queryInstance = "SELECT COUNT(*) FROM " + schema + "." + tp + "JOBINSTANCE";
        
        String instanceIds = executeSql(server, "jdbc/batch", queryInstance);
        return Long.parseLong(instanceIds.substring(1, instanceIds.indexOf("]")));
    }
    
    /**
     * Submit a purge request
     * 
     * @param jobInstanceId
     * @return PurgeObject 
     * @throws Exception
     */
    public void purgeJobInstance(long jobInstanceId, String baseUrl, String username, String password) throws Exception {
        String method = "purgeJobInstance";


        // Get job executions for this jobinstance
        HttpURLConnection con = HttpUtils.getHttpConnection(buildURL(baseUrl + "jobinstances/" + jobInstanceId),
                                                            HttpURLConnection.HTTP_OK,
                                                            new int[0],
                                                            10 * 1000,
                                                            HTTPRequestMethod.DELETE,
                                                            BatchRestUtils.buildHeaderMap(username,password),
                                                            null);
        
        logReaderContents(method, "Successful purge response: ", HttpUtils.getConnectionStream(con));

    }

    /** 
     * Checks that the file system remains intact when the purgeJobStoreOnly option is utilized
     */
    public void confirmFileSystemIntact(Long jobInstanceId, String jobName, String method) {
        File joblogsDir = new File(server.getServerRoot() + File.separator + "logs" + File.separator + "joblogs");
        File instanceDir = new File(joblogsDir, jobName + File.separator
                                                + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + File.separator
                                                + "instance." + jobInstanceId);
        if (!instanceDir.exists()) {
            fail("Job log directory removed when purgeJobStoreOnly option used: " + instanceDir.getAbsolutePath());
        }
    }
    
    /** 
     * Checks that the file system remains intact when the purgeJobStoreOnly option is utilized
     */
    public void confirmFileSystemIntact(Long jobInstanceId, String date, String jobName, String method) {
        File joblogsDir = new File(server.getServerRoot() + File.separator + "logs" + File.separator + "joblogs");
        File instanceDir = new File(joblogsDir, jobName + File.separator
                                                + date + File.separator
                                                + "instance." + jobInstanceId);
        if (!instanceDir.exists()) {
            fail("Job log directory removed when purgeJobStoreOnly option used: " + instanceDir.getAbsolutePath());
        }
    }
    
    /** 
     * Returns the current UTC Date in yyyy-MM-dd HH:mm:ss.SSS format
     * @return
     */
    public String getCurrentDate() {
        DateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Date date = new Date();
        return dFormat.format(date);
    }

    /**
     * Returns the current UTC Day in yyyy-MM-dd format
     * @return
     */
    public String getCurrentDay() {
        DateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        return dFormat.format(date);
    }

    /**
     * Subtracts X number of days from the current UTC Date
     * @param days
     * @return
     */
    public String subtractDaysFromDate(int days) {
        Calendar cal = Calendar.getInstance();
        Date date = new Date();
        DateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        cal.setTime(date);
        cal.add(Calendar.DATE, -days);
        return dFormat.format(cal.getTime());
    }
    
    private void logReaderContents(String method, String prefix, BufferedReader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        String nl = "";
        while ((line = reader.readLine()) != null) {
            sb.append(nl);
            sb.append(line);
            nl = System.getProperty("line.separator");
        }
        log(method, prefix + sb.toString());
    }
    
    public boolean isDatabaseDB2() throws Exception {
        return isDatabase("db2");
    }

    public boolean isDatabaseDerby() throws Exception {
        return isDatabase("derby");
    }

    public boolean isDatabaseOracle() throws Exception {
        return isDatabase("oracle");
    }

    private boolean isDatabase( String dbType) throws Exception {

        ServerConfiguration config = server.getServerConfiguration();

        DatabaseStore ds = config.getDatabaseStores().getById("BatchDatabaseStore");
        String dbRef = ds.getDataSourceRef();

        DataSource d = config.getDataSources().getBy("id", dbRef);

        //returns properties.derby.embedded || properties.db2.jcc || properties.oracle || properties.microsoft.sqlserver
        String dsp = d.getDataSourcePropertiesUsedAlias();

        if (dsp.toLowerCase().contains(dbType)) {
            return true;
        }

        // For test there only one jdbc driver. String may have db type hint.
        ConfigElementList<JdbcDriver> jd = d.getJdbcDrivers();
        JdbcDriver driver = jd.get(0);
        String lr = driver.getLibraryRef();

        if (lr.toLowerCase().contains(dbType)) {
            //I've seen a case where the derby library ref is left in even when another db
        	//type is being used, thus causing a false positive for derby
        	if(dbType.equals("derby")  && !dsp.equals("properties.derby.embedded"))
        		return false;
        	else
        		return true;
        }

        return false;
    }
    
    public String getDatabaseTablePrefix( ) throws Exception{
    	
    	String tablePrefix = server.getServerConfiguration().getDatabaseStores().get(0).getTablePrefix();
    	
    	return (tablePrefix != null)? tablePrefix : "";
    }
    
    public String getDatabaseSchema( ) throws Exception{
    	
    	String schema = server.getServerConfiguration().getDatabaseStores().get(0).getSchema();
    	
    	return (schema != null)? schema : null;
    }
}
