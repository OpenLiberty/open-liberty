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
package com.ibm.ws.jbatch.rest.internal.resources;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.batch.operations.BatchRuntimeException;
import javax.batch.operations.JobSecurityException;
import javax.batch.operations.NoSuchJobInstanceException;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.JobExecution;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.IOUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.jbatch.container.exception.JobInstanceSearchNotSupportedException;
import com.ibm.jbatch.container.ws.BatchDispatcherException;
import com.ibm.jbatch.container.ws.BatchJobNotLocalException;
import com.ibm.jbatch.container.ws.BatchLocationService;
import com.ibm.ws.jbatch.rest.utils.PurgeStatus;
import com.ibm.jbatch.container.ws.WSJobExecution;
import com.ibm.jbatch.container.ws.WSJobInstance;
import com.ibm.jbatch.container.ws.WSJobOperator;
import com.ibm.jbatch.container.ws.WSJobRepository;
import com.ibm.jbatch.container.ws.WSRemotablePartitionExecution;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jbatch.rest.utils.WSPurgeResponse;
import com.ibm.ws.jbatch.rest.utils.WSSearchConstants;
import com.ibm.ws.jbatch.rest.utils.WSSearchObject;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jbatch.joblog.JobExecutionLog;
import com.ibm.ws.jbatch.joblog.JobInstanceLog;
import com.ibm.ws.jbatch.joblog.RemotePartitionLog;
import com.ibm.ws.jbatch.joblog.services.IJobLogManagerService;
import com.ibm.ws.jbatch.rest.BatchManager;
import com.ibm.ws.jbatch.rest.JPAQueryHelperImpl;
import com.ibm.ws.jbatch.rest.internal.BatchJobExecutionAlreadyCompleteException;
import com.ibm.ws.jbatch.rest.internal.BatchJobExecutionNotRunningException;
import com.ibm.ws.jbatch.rest.internal.BatchJobRestartException;
import com.ibm.ws.jbatch.rest.internal.BatchNoSuchJobInstanceException;
import com.ibm.ws.jbatch.rest.internal.BatchRequestUtil;
import com.ibm.ws.jbatch.rest.internal.ZipHelper;
import com.ibm.ws.jbatch.rest.utils.BatchJSONHelper;
import com.ibm.ws.jbatch.rest.utils.JobRestartModel;
import com.ibm.ws.jbatch.rest.utils.JobSubmissionModel;
import com.ibm.ws.jbatch.rest.utils.ResourceBundleRest;
import com.ibm.ws.jbatch.rest.utils.StringUtils;
import com.ibm.wsspi.rest.handler.RESTHandler;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

@Component(service = { RESTHandler.class },
                configurationPolicy = ConfigurationPolicy.IGNORE,
                immediate = true,
                property = {
                            "service.vendor=IBM",
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_JOBINSTANCES,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_JOBINSTANCES_ID,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_JOBINSTANCES_ID_JOBLOGS,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V1_JOBINSTANCES,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V1_JOBINSTANCES_ID,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V1_JOBINSTANCES_ID_JOBLOGS,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V2_JOBINSTANCES,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V2_JOBINSTANCES_ID,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V2_JOBINSTANCES_ID_JOBLOGS,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V3_JOBINSTANCES,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V3_JOBINSTANCES_ID,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V3_JOBINSTANCES_ID_JOBLOGS,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V4_JOBINSTANCES,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V4_JOBINSTANCES_ID,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V4_JOBINSTANCES_ID_JOBLOGS,
                            RESTHandler.PROPERTY_REST_HANDLER_CUSTOM_SECURITY + "=true"
                })
public class JobInstances implements RESTHandler {
	
	private static final TraceComponent tc = Tr.register(JobInstances.class, "wsbatch", "com.ibm.ws.jbatch.rest.resources.RESTMessages");

    private WSJobRepository jobRepository;

    private WSJobOperator wsJobOperator;

    /**
     * The guts of batch job management.
     */
    private BatchManager batchManager;

    /**
     * To access job logs.
     */
    private IJobLogManagerService jobLogManagerService;
    
    /**
     * For checking whether jobexecutions ran locally.
     */
    private BatchLocationService batchLocationService;

    /**
     * DS injection
     *
     * @param ref The WSJobRepository to associate.
     *
     *            Note: The dependency is required; however we mark it OPTIONAL to ensure that
     *            the REST handler is started even if the batch container didn't, so we
     *            can respond with useful error messages instead of 404s.
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL,
                    policy = ReferencePolicy.DYNAMIC,
                    policyOption = ReferencePolicyOption.GREEDY)
    protected void setWSJobRepository(WSJobRepository ref) {
        this.jobRepository = ref;
    }

    protected void unsetWSJobRepository(WSJobRepository ref) {
        if (this.jobRepository == ref) {
            this.jobRepository = null;
        }
    }

    /**
     * Sets the job manager reference.
     *
     * @param ref The job manager to associate.
     *
     *            Note: The dependency is required; however we mark it OPTIONAL to ensure that
     *            the REST handler is started even if the batch container didn't, so we
     *            can respond with useful error messages instead of 404s.
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL,
                    policy = ReferencePolicy.DYNAMIC,
                    policyOption = ReferencePolicyOption.GREEDY)
    protected void setWSJobOperator(WSJobOperator ref) {
        this.wsJobOperator = ref;
    }

    protected void unsetWSJobOperator(WSJobOperator ref) {
        if (this.wsJobOperator == ref) {
            this.wsJobOperator = null;
        }
    }

    /**
     * DS injection.
     *
     * Note: The dependency is required; however we mark it OPTIONAL to ensure that
     * the REST handler is started even if the batch container didn't, so we
     * can respond with useful error messages instead of 404s.
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL,
                    policy = ReferencePolicy.DYNAMIC,
                    policyOption = ReferencePolicyOption.GREEDY)
    protected void setBatchManager(BatchManager ref) {
        this.batchManager = ref;
    }

    protected void unsetBatchManager(BatchManager ref) {
        if (this.batchManager == ref) {
            this.batchManager = null;
        }
    }

    /**
     * DS injection
     *
     * Note: The dependency is required; however we mark it OPTIONAL to ensure that
     * the REST handler is started even if the batch container didn't, so we
     * can respond with useful error messages instead of 404s.
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL,
                    policy = ReferencePolicy.DYNAMIC,
                    policyOption = ReferencePolicyOption.GREEDY)
    protected void setIJobLogManagerService(IJobLogManagerService jobLogManagerService) {
        this.jobLogManagerService = jobLogManagerService;
    }

    /**
     * DS un-setter.
     */
    protected void unsetIJobLogManagerService(IJobLogManagerService jobLogManagerService) {
        if (this.jobLogManagerService == jobLogManagerService) {
            this.jobLogManagerService = null;
        }
    }
    
    /**
     * DS injection
     */
    @Reference
    protected void setBatchLocationService(BatchLocationService batchLocationService) {
        this.batchLocationService = batchLocationService;
    }

    /**
     * Routes request to the appropriate handler.
     */
    private RequestRouter requestRouter = new RequestRouter()
                    .addHandler(new JobInstancesHandler().setPath("/batch/jobinstances"))
                    .addHandler(new JobInstanceHandler().setPath("/batch/jobinstances/*"))
                    .addHandler(new JobLogsHandler().setPath("/batch/jobinstances/*/joblogs"))
                    .addHandler(new JobInstancesHandler().setPath("/batch/v1/jobinstances"))
                    .addHandler(new JobInstanceHandler().setPath("/batch/v1/jobinstances/*"))
                    .addHandler(new JobLogsHandler().setPath("/batch/v1/jobinstances/*/joblogs"))
                    .addHandler(new JobInstancesHandler_v2().setPath("/batch/v2/jobinstances"))
                    .addHandler(new JobInstanceHandler().setPath("/batch/v2/jobinstances/*"))
                    .addHandler(new JobLogsHandler().setPath("/batch/v2/jobinstances/*/joblogs"))
                    .addHandler(new JobInstancesHandler_v3().setPath("/batch/v3/jobinstances"))
    				.addHandler(new JobInstanceHandler().setPath("/batch/v3/jobinstances/*"))
    				.addHandler(new JobLogsHandler().setPath("/batch/v3/jobinstances/*/joblogs"))
    				.addHandler(new JobInstancesHandler_v4().setPath("/batch/v4/jobinstances"))
    				.addHandler(new JobInstanceHandler().setPath("/batch/v4/jobinstances/*"))
    				.addHandler(new JobLogsHandler().setPath("/batch/v4/jobinstances/*/joblogs"));

    /**
     * @param request
     * @param response
     * @throws IOException
     */
    public void handleRequest(final RESTRequest request, final RESTResponse response) throws IOException {
        try {
            // First verify the batch container is started.
            BatchRuntime.getJobOperator();

            requestRouter.routeRequest(request, response);
        } catch (JobSecurityException jse) {
            response.sendError(HttpURLConnection.HTTP_UNAUTHORIZED, jse.getMessage());
        } catch (BatchNoSuchJobInstanceException bnsjie) {
            response.sendError(HttpURLConnection.HTTP_BAD_REQUEST, bnsjie.getMessage());
        } catch (JobInstanceSearchNotSupportedException jisnse) {
        	response.sendError(HttpURLConnection.HTTP_INTERNAL_ERROR,
        			ResourceBundleRest.getMessage("in.memory.search.not.supported"));
        } catch (UnsupportedOperationException uoe) {
        	response.sendError(HttpURLConnection.HTTP_INTERNAL_ERROR, uoe.getMessage());
        } catch (Exception e) {
            response.sendError(HttpURLConnection.HTTP_INTERNAL_ERROR, e.getMessage());
        }
    }

    /**
     * Handles "/batch/jobinstances", which is used to submit jobs.
     */
    private class JobInstancesHandler extends RequestHandler {
        public void get(RESTRequest request, RESTResponse response) throws Exception {
            listJobInstances(request, response);
        }
        public void post(RESTRequest request, RESTResponse response) throws Exception {
            submitJob(request, response);
        }
    }

    /**
     * Handles "/batch/v2/jobinstances", which is used to perform the pre-purge query and purge
     */
    private class JobInstancesHandler_v2 extends JobInstancesHandler {

        public void get(RESTRequest request, RESTResponse response) throws Exception {
            searchJobInstances(request, response);
        }

        public void delete(RESTRequest request, RESTResponse response) throws Exception {
            purgeJobInstances(request, response);
        }
    }

    /**
     * Handles "/batch/v3/jobinstances", which is used to perform the pre-purge query and purge
     */
    private class JobInstancesHandler_v3 extends JobInstancesHandler {

        public void get(RESTRequest request, RESTResponse response) throws Exception {
            searchJobInstances(request, response, 3);
        }

        public void delete(RESTRequest request, RESTResponse response) throws Exception {
            purgeJobInstances(request, response, 3);
        }
    }

    /**
     * Handles "/batch/v4/jobinstances", which is used to perform the pre-purge query and purge
     */
    private class JobInstancesHandler_v4 extends JobInstancesHandler {

        public void get(RESTRequest request, RESTResponse response) throws Exception {
            searchJobInstances(request, response, 4);
        }

        public void delete(RESTRequest request, RESTResponse response) throws Exception {
            purgeJobInstances(request, response, 4);
        }
    }

    /**
     * Handles "/batch/jobinstances/{jobinstanceid}"
     */
    private class JobInstanceHandler extends RequestHandler {

        public void get(RESTRequest request, RESTResponse response) throws Exception {
            getJobInstanceData(request, response, getJobInstanceId(request));
        }

        public void put(RESTRequest request, RESTResponse response) throws Exception {

            String action = request.getParameter("action");

            if ("stop".equalsIgnoreCase(action)) {
                stopJobInstance(request, response, getJobInstanceId(request));
            } else if ("restart".equalsIgnoreCase(action)) {
                restartJobInstance(request, response, getJobInstanceId(request));
            } else {
                response.sendError(HttpURLConnection.HTTP_BAD_REQUEST, "Invalid HTTP query parameters: only action=stop and action=restart are supported.");
            }
        }

        public void delete(RESTRequest request, RESTResponse response) throws Exception {
            purgeJobInstance(request, response);
        }

    }

    /**
     * Handles
     * "/batch/jobinstances/{jobinstanceid}/joblogs"
     * "/batch/jobinstances/{jobinstanceid}/joblogs?type=zip"
     *
     */
    private class JobLogsHandler extends RequestHandler {

        public void get(RESTRequest request, RESTResponse response) throws Exception {

            long jobInstanceId = getJobInstanceId(request);

            try {
                JobInstanceLog jobInstanceLog = jobLogManagerService.getJobInstanceLog(jobInstanceId);
                
                if (jobInstanceLog.areExecutionsLocal() || "true".equals(request.getParameter("localOnly"))) {
                    sendJobInstanceLog(jobInstanceLog, request, response);
                } else {
                    handleJobLogsNotLocal(request, response, jobInstanceId);
                }
            } catch (NoSuchJobInstanceException e) {
                throw new BatchNoSuchJobInstanceException(e, jobInstanceId);
            }
        }

        /**
         * Send the job instance log on the given REST response
         */
        protected void sendJobInstanceLog(JobInstanceLog jobInstanceLog,
                                          RESTRequest request,
                                          RESTResponse response) throws IOException {

            if ("zip".equals(request.getParameter("type"))) {
            	
            	ZipOutputStream zipOutput = new ZipOutputStream(response.getOutputStream());

        		// Note: headers must be set *before* writing to the output stream
        		response.setContentType("application/zip");
        		response.setResponseHeader("Content-Disposition", "attachment; filename=" + StringUtils.enquote(getZipFileName(jobInstanceLog)));

        		HashSet<String> partitionEndpointURLs = new HashSet<String>();

        		for (JobExecutionLog jobExecutionLog : jobInstanceLog.getJobExecutionLogs()) {
        			// If there are remote partition logs, fetch them now.
        			// The localOnly flag is used to prevent cascading requests
        			if (jobExecutionLog.getRemotePartitionLogs() != null &&
        					!("true".equals(request.getParameter("localOnly")))) {

        				partitionEndpointURLs.addAll(jobExecutionLog.getRemotePartitionEndpointURLs());

        			}
        		}

        		// Ignore local URL because the logs would have already been collected with the top-level execution logs
        		partitionEndpointURLs.remove(BatchRequestUtil.getUrlRoot(request));
        		
        		System.out.println("CGCG endpoint URLs:");

        		for (String url : partitionEndpointURLs) {
            		System.out.println("CGCG " + url);
        			
        			// Fetch the contents from the remote partition executor
        			String joblogUrl = BatchRequestUtil.buildJoblogsUrlForJobInstance(jobInstanceLog.getJobInstance().getInstanceId(),
        					url,
        					"type=zip&localOnly=true");
        			try {
        				HttpsURLConnection conn = BatchRequestUtil.sendRESTRequest(joblogUrl, "GET", request, null);

        				if (conn != null) {
        					ZipInputStream zipInput = new ZipInputStream(conn.getInputStream());
        					ZipHelper.copyZipEntries(zipInput, zipOutput);
        				}
        			} catch (Exception ex) {
        				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
        					Tr.debug(tc, "Exception occurred fetching remote partition logs from " + joblogUrl +
        							", exception details: " + ex.getClass().getName() + ": " + ex.getLocalizedMessage());
        				}
        			}
        		}


        		ZipHelper.zipFilesToStream(jobInstanceLog.getJobLogFiles(),
        				jobInstanceLog.getInstanceLogRootDirs(),
        				zipOutput);

            } else if ("text".equals(request.getParameter("type"))) {

            	// Note: headers must be set *before* writing to the output stream
            	response.setContentType("text/plain; charset=UTF-8");

            	ZipHelper.aggregateFilesToStream(jobInstanceLog.getJobLogFiles(),
                                                 jobInstanceLog.getInstanceLogRootDirs(),
                                                 response.getOutputStream());
                
        		HashSet<String> partitionEndpointURLs = new HashSet<String>();

        		for (JobExecutionLog jobExecutionLog : jobInstanceLog.getJobExecutionLogs()) {
        			// If there are remote partition logs, fetch them now.
        			// The localOnly flag is used to prevent cascading requests
        			if (jobExecutionLog.getRemotePartitionLogs() != null &&
        					!("true".equals(request.getParameter("localOnly")))) {

        				partitionEndpointURLs.addAll(jobExecutionLog.getRemotePartitionEndpointURLs());
        			}
        		}

        		// Ignore local URL because the logs would have already been collected with the top-level execution logs
        		partitionEndpointURLs.remove(BatchRequestUtil.getUrlRoot(request));

        		System.out.println("CGCG endpoint URLs:");
        		
        		// Fetch the contents from the remote partition executors
        		for (String url : partitionEndpointURLs) {
            		System.out.println("CGCG " + url);
        			
        			String joblogUrl = BatchRequestUtil.buildJoblogsUrlForJobInstance(jobInstanceLog.getJobInstance().getInstanceId(),
        					url,
        					"type=text&localOnly=true");
        			try {
        				HttpsURLConnection conn = BatchRequestUtil.sendRESTRequest(joblogUrl, "GET", request, null);

        				// Copy job log text from the remote request
        				if (conn != null) {
        					byte[] buf = new byte[2048];
        					int len;
        					while ((len = conn.getInputStream().read(buf)) != -1) {
        						response.getOutputStream().write(buf, 0, len);
        					}
        				}
        			} catch (Exception ex) {
        				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
        					Tr.debug(tc, "Exception occurred fetching remote partition logs from " + joblogUrl +
        							", exception details: " + ex.getClass().getName() + ": " + ex.getLocalizedMessage());
        				}
        			}

        		}
            } else {

            	// Note: headers must be set *before* writing to the output stream
            	response.setContentType(BatchJSONHelper.MEDIA_TYPE_APPLICATION_JSON);

                BatchJSONHelper.writeJobInstanceLogLinks(jobInstanceLog,
                                                         BatchRequestUtil.getUrlRoot(request),
                                                         response.getOutputStream());
            }
        }

        /**
         * If all executions of the given job instance ran on the same server,
         * then just forward the request to that server.
         * Otherwise, fail the request for now... or maybe just return an error
         * code along with all jobexecution log links
         * @throws IOException
         */
        protected void handleJobLogsNotLocal(RESTRequest request,
                                                      RESTResponse response,
                                                      long jobInstanceId) throws IOException {

        	if ("zip".equals(request.getParameter("type"))
        			|| "text".equals(request.getParameter("type"))) {
        		//This will fail if all executions didn't run on the same endpoint.
        		String restUrl = findSingleJobExecutionEndpoint(jobInstanceId);
        		
        		BatchRequestUtil.handleNonLocalRequest(BatchRequestUtil.buildJoblogsUrlForJobInstance(jobInstanceId, restUrl, request.getQueryString()),
        				"GET",
        				request,
        				response);
        	} else {
        		// Send links to remote job executions.
        		JobInstanceLog jobInstanceLog = jobLogManagerService.getJobInstanceLogAllExecutions(jobInstanceId);

        		response.setContentType(BatchJSONHelper.MEDIA_TYPE_APPLICATION_JSON);

                BatchJSONHelper.writeJobInstanceLogLinks(jobInstanceLog,
                                                         BatchRequestUtil.getUrlRoot(request),
                                                         response.getOutputStream());
        	}
        }

        /**
         * @return a suggested file name for the joblog zipfile.
         */
        protected String getZipFileName(JobInstanceLog jobInstanceLog) {
            return "joblogs."
                   + jobInstanceLog.getJobInstance().getJobName()
                   + ".instance."
                   + jobInstanceLog.getJobInstance().getInstanceId()
                   + ".zip";
        }

        public void delete(RESTRequest request, RESTResponse response) throws Exception {
            purgeLocalJobLogs(request, response, getJobInstanceId(request));
        }
    };

    /**
     * @return the job instance ID as a Long
     *
     * @throws IOException
     */
    protected long getJobInstanceId(RESTRequest request) throws RequestException {
        String s = request.getPathVariable("jobinstanceid");
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException nfe) {
            throw new RequestException(HttpURLConnection.HTTP_BAD_REQUEST,
                            "The job instance id (" + s + ") must be a Long integer",
                            nfe);
        }
    }

    /*
     * Response with job instance details.
     */
    private void getJobInstanceData(final RESTRequest request,
                                    final RESTResponse response,
                                    long jobInstanceID) throws IOException, RequestException {

        WSJobInstance jobInstance = null;
        try {
            jobInstance = jobRepository.getJobInstance(jobInstanceID);
        } catch (NoSuchJobInstanceException e) {
            throw new BatchNoSuchJobInstanceException(e, jobInstanceID);
        }

        List<WSJobExecution> jobExecs = jobRepository.getJobExecutionsFromInstance(jobInstanceID);

        // Note: headers must be set *before* writing to the output stream
        response.setContentType(BatchJSONHelper.MEDIA_TYPE_APPLICATION_JSON);

        BatchJSONHelper.writeJobInstance(jobInstance,
                                         jobExecs,
                                         BatchRequestUtil.getUrlRoot(request),
                                         response.getOutputStream());
    }

    /**
     * Submit a new job and respond with its JobInstance data.
     */
    private void submitJob(final RESTRequest request, final RESTResponse response) throws IOException, RequestException {

        InputStream jsonInput = null;
        InputStream jslInput = null;

        if (request.isMultiPartRequest()) {
            jsonInput = request.getPart("jobdata");
            jslInput = request.getPart("jsl");
            if (jsonInput == null) {
                throw new RequestException(HttpURLConnection.HTTP_BAD_REQUEST, "A multipart form must be submitted with a part name of jobdata.");
            }

            if (jslInput == null) {
                throw new RequestException(HttpURLConnection.HTTP_BAD_REQUEST, "A multipart form must be submitted with a part name of jsl.");
            }
        } else {
            jsonInput = request.getInputStream();
        }

        JobSubmissionModel jobSubmission = new JobSubmissionModel(BatchJSONHelper.readJsonObject(jsonInput));

        if (StringUtils.isEmpty(jobSubmission.getApplicationName())
            && StringUtils.isEmpty(jobSubmission.getModuleName())) {
            throw new RequestException(HttpURLConnection.HTTP_BAD_REQUEST, "Either attribute [applicationName] or [moduleName] must be provided.");
        } else if (StringUtils.isEmpty(jobSubmission.getJobXMLName()) &&
                   StringUtils.isEmpty(jobSubmission.getJobXML())) {
            throw new RequestException(HttpURLConnection.HTTP_BAD_REQUEST, "Either attribute [jobXMLName] or [jobXML] must be provided.");
        }

        try {
            WSJobInstance jobInstance;

            // Determine if we are getting an inline JSL file.  There are two paths:
            // 1 - Inline JSL via multipart HTTP request.  Message has a jobdata and jsl part
            // 2 - Inline JSL via singlepart HTTP request.  JSL is included as jobXML in JSON object.

            // JSL via multipart HTTP request
            if (jslInput != null) {
                // Defaulting to UTF-8
                BufferedReader br = new BufferedReader(new InputStreamReader(
                                jslInput, StandardCharsets.UTF_8));
                String jslBody = IOUtils.toString(br);

                jobInstance = batchManager.start(
                                                 jobSubmission.getApplicationName(),
                                                 jobSubmission.getModuleName(),
                                                 jobSubmission.getComponentName(),
                                                 jobSubmission.getJobXMLName(),
                                                 jobSubmission.getJobParameters(),
                                                 jslBody);

            } else {
                // JSL via singlepart HTTP or No JSL - dependent of value of getJobXML()
                jobInstance = batchManager.start(
                                                 jobSubmission.getApplicationName(),
                                                 jobSubmission.getModuleName(),
                                                 jobSubmission.getComponentName(),
                                                 jobSubmission.getJobXMLName(),
                                                 jobSubmission.getJobParameters(),
                                                 jobSubmission.getJobXML());
            }

            // Note: headers must be set *before* writing to the output stream
            response.setContentType(BatchJSONHelper.MEDIA_TYPE_APPLICATION_JSON);
            response.setStatus(HttpURLConnection.HTTP_CREATED);

            BatchJSONHelper.writeJobInstance(jobInstance,
                                             BatchRequestUtil.getUrlRoot(request),
                                             response.getOutputStream());
        } catch (BatchDispatcherException e) {
            //Defect 191113: changed to e.getMessage() because  e.getCause().toString() would result in a NPE
            response.sendError(HttpURLConnection.HTTP_INTERNAL_ERROR,
                               "The batch dispatcher encountered exception while dispatching the submit job request for job " +
                                               jobSubmission.getJobXMLName() + ", job instance " + e.getJobInstanceId() + ". The exception details is: " + e.getCause());
        }
    }

    /**
     * Stopping a job instance should get forwarded to the current running job execution. If
     * this is no current execution return an error. There is some duplicate code here since I don't konw how to reference
     * another instance of a REST Handler implementation yet.
     */
    @FFDCIgnore(BatchJobNotLocalException.class)
    private void stopJobInstance(final RESTRequest request, final RESTResponse response, long jobInstanceID) throws IOException, RequestException {

        try {

            long jobExecutionId = batchManager.stopJobInstance(jobInstanceID);

            // Note: headers must be set *before* writing to the output stream
            response.setContentType(BatchJSONHelper.MEDIA_TYPE_APPLICATION_JSON);

            if (jobExecutionId >= 0) {
                // Return the latest job execution record, to be consistent with JobExecutions.stopJobExecution.
                BatchJSONHelper.writeJobExecution(jobRepository.getJobExecution(jobExecutionId),
                                                  Collections.EMPTY_LIST,
                                                  BatchRequestUtil.getUrlRoot(request),
                                                  response.getOutputStream());
            } else {
                // Return an empty JSON object since there is no job execution
                BatchJSONHelper.writeJsonObject(BatchJSONHelper.createJsonObject(), response.getOutputStream());

            }

        } catch (BatchJobExecutionNotRunningException e) {
            response.sendError(
                               HttpURLConnection.HTTP_CONFLICT,
                               "The most recent job execution id "
                                               + e.getJobExecutionId()
                                               + " associated with job instance id "
                                               + e.getJobInstanceId() + " is not currently running.");
        } catch (BatchJobNotLocalException e) {
        	String restUrl = e.getJobExecution().getRestUrl();
        	BatchRequestUtil.handleNonLocalRequest(BatchRequestUtil.buildStopUrlForJobInstance(jobInstanceID, restUrl),
        			"PUT",
        			request,
        			response);
        }
    }

    /**
     * Restarting a job instance should get forwarded to the current running job execution. There is some duplicate
     * code here since I don't konw how to reference another instance of a REST Handler implementation yet.
     */

    private void restartJobInstance(final RESTRequest request, final RESTResponse response, long jobInstanceID) throws IOException, RequestException {

        // Read the jobParams from the request body, if any.
        Properties jobParams = (BatchRequestUtil.getContentLength(request) > 0)
                        ? new JobRestartModel(BatchJSONHelper.readJsonObject(request.getInputStream())).getJobParameters()
                        : null;

        boolean reuseParams = "true".equalsIgnoreCase(request.getParameter("reusePreviousParams"));

        jobParams = (reuseParams)
                        ? BatchJSONHelper.mergeProperties(jobRepository.getJobExecutionsFromInstance(jobInstanceID).get(0).getJobParameters(),
                                                          jobParams)
                        : jobParams;

        try {
            WSJobInstance jobInstance = batchManager.restartJobInstance(jobInstanceID, jobParams);

            // Note: headers must be set *before* writing to the output stream
            response.setContentType(BatchJSONHelper.MEDIA_TYPE_APPLICATION_JSON);

            BatchJSONHelper.writeJobInstance(jobInstance,
                                             BatchRequestUtil.getUrlRoot(request),
                                             response.getOutputStream());

        } catch (BatchJobExecutionAlreadyCompleteException e) {
            response.sendError(HttpURLConnection.HTTP_CONFLICT,
                               "The most recent job execution " + e.getJobExecutionId() + " for job instance " + e.getJobInstanceId()
                                               + " cannot be restarted because it is already complete.");
        } catch (BatchDispatcherException e) {
            response.sendError(HttpURLConnection.HTTP_INTERNAL_ERROR,
                    "The batch dispatcher encountered exception while dispatching the restart request for job execution " + e.getJobExecutionId() + ".  The exception details is: "
                                               + e.toString());
        } catch (BatchJobRestartException e) {
            response.sendError(HttpURLConnection.HTTP_CONFLICT,
                               "The job " + jobInstanceID + " cannot be restarted.");
        }
    }

    /**
     * publish event only when purge is success
     */
    protected void purgeJobInstance(RESTRequest request, RESTResponse response) throws Exception {
        long jobInstanceId = getJobInstanceId(request);

        boolean purgeJobStoreOnly = "true".equalsIgnoreCase(request.getParameter("purgeJobStoreOnly")); //default is false. We won't purge DB if we can't purge the filesystem

        try {

        	if (!jobRepository.isJobInstancePurgeable(jobInstanceId)) {
        		throw new RequestException(HttpURLConnection.HTTP_CONFLICT, "The specified job instance, " + jobInstanceId + ", cannot be purged because it has active job executions.");
        	}
        } catch (NoSuchJobInstanceException e) {
        	throw new BatchNoSuchJobInstanceException(e, jobInstanceId);
        }

        if (!purgeJobStoreOnly) { //Try to purge job logs and DB, or send a redirect if not local

        	boolean fileSuccess = false;
        	
        	JobInstanceLog instanceLog = null;
            try {
            	instanceLog = jobLogManagerService.getJobInstanceLog(jobInstanceId);
            } catch (NoSuchJobInstanceException e) {
            	throw new BatchNoSuchJobInstanceException(e, jobInstanceId);
            }

            // If the local flag is set, or we have any local logs, purge those
            if ("true".equals(request.getParameter("localOnly")) || instanceLog.areExecutionsLocal()) {
            	fileSuccess = instanceLog.purge();
            } 
            
            // If any logs exists on other endpoints, send requests to purge those
            if (!instanceLog.areExecutionsLocal() || instanceLog.hasRemotePartitionLogs()){
            	HashSet<String> restUrls = new HashSet<String>();
            	restUrls.addAll(findJobExecutionEndpoints(jobInstanceId));
    			restUrls.remove(batchLocationService.getBatchRestUrl());

                // If executions exist on only one endpoint, redirect to that endpoint.
            	if (restUrls.size() == 1) {
            		// This call will handle the SSL and redirect enabled checking
	            	BatchRequestUtil.handleNonLocalRequest(
	            			BatchRequestUtil.buildPurgeUrlForJobInstance(jobInstanceId, restUrls.iterator().next()),
	        				"DELETE",
	        				request,
	        				response);

	            	return;

            	} else {
            		// Send requests to all endpoints to delete their local job logs.
            		String responses = "";

            		for (String restUrl : restUrls) {

            			try {
            				HttpURLConnection connection = BatchRequestUtil.sendRESTRequest(BatchRequestUtil.buildJoblogsUrlForJobInstance(jobInstanceId, restUrl, "localOnly=true"),
                																		    "DELETE",
                																		    request,
                																		    null);

            				if (connection != null) {
            					// Log the response code, and if it failed, send an error response
            					responses = responses.concat("A request to " + connection.getURL().getPath() + " returned response code " + connection.getResponseCode() + ". ");
            					if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                                	response.sendError(HttpURLConnection.HTTP_INTERNAL_ERROR, "An error occurred while purging the job instance (" + jobInstanceId + "). "
                                			+ "Not all job log files were deleted so no attempt was made to delete database entries. " + responses);
                        			return;
                    			}
            				} else {
            					// Null connection means we failed to connect, but can't report a response code
            					responses = responses.concat("A request to " + BatchRequestUtil.buildJoblogsUrlForJobInstance(jobInstanceId, restUrl, "") + " failed. ");
            					response.sendError(HttpURLConnection.HTTP_INTERNAL_ERROR, "An error occurred while purging the job instance (" + jobInstanceId + "). "
            							+ "Not all job log files were deleted so no attempt was made to delete database entries. " + responses);
            					return;
            				}

            			} catch (Exception ex) {
            				responses = responses.concat("Exception occurred during a request to " + BatchRequestUtil.buildJoblogsUrlForJobInstance(jobInstanceId, restUrl, "") +
            											 ", exception details: " + ex.getClass().getName() + ": " + ex.getLocalizedMessage());
                        	response.sendError(HttpURLConnection.HTTP_INTERNAL_ERROR, "An exception occurred while purging the job instance (" + jobInstanceId + "). "
                        			+ "Not all job log files were deleted so no attempt was made to delete database entries." + responses);
                        	return;
            			}

            		}
            		fileSuccess = true;
            	}
            }


            if (fileSuccess) {
            	boolean dbSuccess = wsJobOperator.purgeJobInstance(jobInstanceId);

            	if (!dbSuccess) {
            		response.sendError(HttpURLConnection.HTTP_INTERNAL_ERROR, "An error occurred while purging the job instance (" + jobInstanceId + "). "
            				+ "The job logs were sucessfully deleted but not all database entries were deleted.");
            	}
            } else {
            	response.sendError(HttpURLConnection.HTTP_INTERNAL_ERROR, "An error occurred while purging the job instance (" + jobInstanceId + "). "
            			+ "Not all job log files were deleted so no attempt was made to delete database entries.");
            }



        } else { //We won't even try to purge job logs. Only purge from database.

            boolean dbSuccess = wsJobOperator.purgeJobInstance(jobInstanceId);

            if (!dbSuccess) {
                response.sendError(HttpURLConnection.HTTP_INTERNAL_ERROR, "An error occurred while purging the job instance (" + jobInstanceId + "). Not all database entries were deleted.");
            }
        }
    }

    /**
     *
     */
    protected void listJobInstances(RESTRequest request, RESTResponse response) throws Exception {

        int page = Integer.parseInt(StringUtils.firstNonNull(request.getParameter("page"), "0"));
        int pageSize = Integer.parseInt(StringUtils.firstNonNull(request.getParameter("pageSize"), "50"));

        WSSearchObject wsso = null;
        try {
        	// new up a search object that will do a base query sorted by descending create time
            wsso = new WSSearchObject(null, null, null, null, null, "-createTime", null);
        } catch (Exception e) {
            throw new RequestException(HttpURLConnection.HTTP_BAD_REQUEST, "An error occurred while processing the specified parameters in " +
                                                                           request.getCompleteURL() + "; Original Exception Message: " + e.getMessage());
        }

        // call the version of getJobInstances that takes a query helper obj
        List<WSJobInstance> jobInstances = jobRepository.getJobInstances(new JPAQueryHelperImpl(wsso), page, pageSize);

        // Note: headers must be set *before* writing to the output stream
        response.setContentType(BatchJSONHelper.MEDIA_TYPE_APPLICATION_JSON);

        BatchJSONHelper.writeJobInstances(jobInstances,
                                          BatchRequestUtil.getUrlRoot(request),
                                          response.getOutputStream());
    }

    /**
     * Parse the incoming parameters for any fields that we don't recognize
     *
     * @param request The incoming REST request
     * @return a String representation of the unrecognized parameters, separated by commas
     * @throws Exception
     */
    private String getUnrecognizedParameters(RESTRequest request, int version) throws Exception {

    	String unrecognized = null;
    	List<String> validParams = null;

    	if (version == 3) {
    		validParams = WSSearchConstants.VALID_SEARCH_PARAMS_V3;
    	} else if (version == 4) {
    		validParams = WSSearchConstants.VALID_SEARCH_PARAMS_V4;
    	}

     	Map<String, String[]> params = request.getParameterMap();
     	for (Map.Entry<String, String[]> entry : params.entrySet()) {
     		if (!validParams.contains(entry.getKey())) {
     			// ignore purgeJobStoreOnly, its not a search param but its valid for purge
     			if (!entry.getKey().startsWith("jobParameter") && !entry.getKey().contains(".") && !entry.getKey().startsWith("purgeJobStoreOnly")) {
     				// Construct a string listing any filters we don't recognize
     				if (unrecognized != null) {
     					unrecognized = unrecognized.concat("," + entry.getKey());
     				} else {
     					unrecognized = entry.getKey();
     				}
     			}
     		} else if (entry.getKey().equals("sort")) {
 				String[] split = entry.getValue()[0].split(",");
 				for(int i = 0; i < split.length; i++) {
 					String field = split[i];
 					if (field.startsWith("-"))
 	                    field = field.substring(1);

 					if(!WSSearchConstants.VALID_SORT_FIELDS.contains(field)) {
 						if (unrecognized != null) {
 	    					unrecognized = unrecognized.concat("," + "sort="+split[i]);
 	    				} else {
 	    					unrecognized = "sort="+split[i];
 	    				}
 					} else if (field.equals("lastUpdatedTime")
 			      			&& (jobRepository.getJobInstanceEntityVersion() < 2)) {
 			      		throw new RequestException(HttpURLConnection.HTTP_NOT_IMPLEMENTED, "A search or sort by last update time was requested, but the job instance table does not contain the UPDATETIME column.");
 			      	}

 				}
     		} else if (entry.getKey().equals("lastUpdatedTime")
     				&& (jobRepository.getJobInstanceEntityVersion() < 2)) {
     			throw new RequestException(HttpURLConnection.HTTP_NOT_IMPLEMENTED, "A search or sort by last update time was requested, but the job instance table does not contain the UPDATETIME column.");
     		}
     	}

     	return unrecognized;
    }

    /**
     * Extract the specified job parameters from the incoming REST request.
     * Currently doesn't handle multiple job parameters, new ones will overwrite (keeps current behavior essentially).
     *
     * This code can be modified later to build a proper list without overwriting once we support multiples.
     *
     * @param request The incoming REST request
     * @return a list of job parameter String[] objects.  String[0] is name, String[1] is the value.
     * @throws Exception
     */
    private Map<String, String> getJobParameters(RESTRequest request) throws Exception {

    	Map<String, String> jobParams = new HashMap<String, String>();

    	Map<String, String[]> params = request.getParameterMap();
    	for (Map.Entry<String, String[]> entry : params.entrySet()) {
    		String key = entry.getKey();
    		if (key.startsWith("jobParameter.")) {
    			key = key.substring(key.indexOf(".")+1);
    			jobParams.put(key, entry.getValue()[0]);
    		}
    	}

        if ((!jobParams.isEmpty()) && (jobRepository.getJobExecutionEntityVersion() < 2)) {
       	 throw new RequestException(HttpURLConnection.HTTP_NOT_IMPLEMENTED, ResourceBundleRest.getMessage("db.tables.not.created.for.jobparm.search"));
        }

    	return jobParams;
    }

    /**
     * Utility method for actually performing the job instances V3 or V4 search.
     * This is shared between search and purge.
     *
     * @param request The incoming REST request
     * @return a List of WSJobIntance objects matching the query parameters
     * @throws Exception
     */
    private List<WSJobInstance> doJobInstanceSearch(RESTRequest request, int version) throws Exception {

    	 int page = Integer.parseInt(StringUtils.firstNonNull(request.getParameter("page"), "0"));
         int pageSize = Integer.parseInt(StringUtils.firstNonNull(request.getParameter("pageSize"), "50"));

     	 Map<String, String> jobParameters = getJobParameters(request);

         WSSearchObject wsso = null;
         try {
        	 if (version == 3) {
        		 wsso = new WSSearchObject(request.getParameter("jobInstanceId"), request.getParameter("createTime"),
        				 request.getParameter("instanceState"), request.getParameter("exitStatus"),
        				 request.getParameter("lastUpdatedTime"), request.getParameter("sort"), jobParameters);
        	 } else if (version == 4) {
        		 wsso = new WSSearchObject(request.getParameter("jobInstanceId"), request.getParameter("createTime"),
        				 request.getParameter("instanceState"), request.getParameterValues("exitStatus"),
        				 request.getParameter("lastUpdatedTime"), request.getParameter("sort"), jobParameters,
        				 request.getParameterValues("submitter"), request.getParameterValues("appName"), request.getParameterValues("jobName"),
        				 request.getParameter("ignoreCase"));
        	 }
         } catch (Exception e) {
             throw new RequestException(HttpURLConnection.HTTP_BAD_REQUEST, "An error occurred while processing the specified parameters in " +
                                                                            request.getCompleteURL() + "; Original Exception Message: " + e.getMessage());
         }

        List<WSJobInstance> jobInstances = jobRepository.getJobInstances(new JPAQueryHelperImpl(wsso), page, pageSize);

    	return jobInstances;
    }
    /**
     * Queries for Job Instances given the input parameters
     * @param request
     * @param response
     * @throws Exception
     */
    protected void searchJobInstances(RESTRequest request, RESTResponse response, int version) throws Exception {

        String unrecognized = getUnrecognizedParameters(request, version);

    	// Add any unrecognized terms to a header for UI to handle
    	if (unrecognized != null) {
    		response.addResponseHeader("X-IBM-Unrecognized-Fields", unrecognized);
    	}

    	List<WSJobInstance> jobInstances = doJobInstanceSearch(request, version);

        // Note: headers must be set *before* writing to the output stream
        response.setContentType(BatchJSONHelper.MEDIA_TYPE_APPLICATION_JSON);

        BatchJSONHelper.writeJobInstances(jobInstances,
                                          BatchRequestUtil.getUrlRoot(request),
                                          response.getOutputStream());

    }

    /**
     * Queries for Job Instances given the input parameters
     * @param request
     * @param response
     * @throws Exception
     */
    protected void searchJobInstances(RESTRequest request, RESTResponse response) throws Exception {

        String instanceIdParams = request.getParameter("jobInstanceId");
        String startTimeParams = request.getParameter("createTime");
        String instanceStateParams = request.getParameter("instanceState");
        String exitStatusParams = request.getParameter("exitStatus");

        int page = Integer.parseInt(StringUtils.firstNonNull(request.getParameter("page"), "0"));
        int pageSize = Integer.parseInt(StringUtils.firstNonNull(request.getParameter("pageSize"), "50"));

        WSSearchObject wsso = null;
        try {
            wsso = new WSSearchObject(instanceIdParams, startTimeParams, instanceStateParams, exitStatusParams);
        } catch (Exception e) {
            throw new RequestException(HttpURLConnection.HTTP_BAD_REQUEST, "An error occurred while processing the specified parameters in " +
                                                                           request.getCompleteURL() + "; Original Exception Message: " + e.getMessage());
        }

        List<WSJobInstance> jobInstances = jobRepository.getJobInstances(new JPAQueryHelperImpl(wsso), page, pageSize);

        // Note: headers must be set *before* writing to the output stream
        response.setContentType(BatchJSONHelper.MEDIA_TYPE_APPLICATION_JSON);

        BatchJSONHelper.writeJobInstances(jobInstances,
                                          BatchRequestUtil.getUrlRoot(request),
                                          response.getOutputStream());

    }


    /**
     * Queries for jobs matching the input query criteria, and then purges the jobs one by one
     *
     * This method is used when doing a purge via the V2 API
     *
     * @param request
     * @param response
     * @throws Exception
     */
    protected void purgeJobInstances(RESTRequest request, RESTResponse response) throws Exception {

        int page = Integer.parseInt(StringUtils.firstNonNull(request.getParameter("page"), "0"));
        int pageSize = Integer.parseInt(StringUtils.firstNonNull(request.getParameter("pageSize"), "50"));

        WSSearchObject wsso = null;
        try {
            wsso = new WSSearchObject(request.getParameter("jobInstanceId"), request.getParameter("createTime"), request.getParameter("instanceState"), request.getParameter("exitStatus"));
        } catch (Exception e) {
            throw new RequestException(HttpURLConnection.HTTP_BAD_REQUEST, "An error occurred while processing the specified parameters in " + request.getCompleteURL());
        }

        // Query for jobs
        List<WSJobInstance> jobInstances = jobRepository.getJobInstances(new JPAQueryHelperImpl(wsso), page, pageSize);

        // Extract job instance ids returned by the query given the input parameters
        ArrayList<Long> instanceList = new ArrayList<Long>(jobInstances.size());
        for (WSJobInstance job : jobInstances) {
            instanceList.add(job.getInstanceId());
        }

        // Purge
        ArrayList<WSPurgeResponse> purgeResponseList = new ArrayList<WSPurgeResponse>(instanceList.size());
        for (long instanceId : instanceList) {
            try {
                purgeResponseList.add(purgeJobInstance("true".equalsIgnoreCase(request.getParameter("purgeJobStoreOnly")), instanceId, request));
            } catch (Exception e) {
                purgeResponseList.add(new WSPurgeResponse(instanceId,
                                PurgeStatus.FAILED, "Exception ocurred during purge of job instance " + instanceId + " : " + e.getMessage(),
                                null));

            }
        }

        // Note: headers must be set *before* writing to the output stream
        response.setContentType(BatchJSONHelper.MEDIA_TYPE_APPLICATION_JSON);

        BatchJSONHelper.buildAndWritePurgeJsonObject(purgeResponseList, response.getOutputStream());

    }

    /**
     * Queries for jobs matching the input query criteria, and then purges the jobs one by one
     *
     * This method is used when doing a purge via the V3 or V4 API.
     *
     * @param request
     * @param response
     * @throws Exception
     */
    protected void purgeJobInstances(RESTRequest request, RESTResponse response, int version) throws Exception {

    	String unrecognized = getUnrecognizedParameters(request, version);

    	// Add any unrecognized terms to a header for UI to handle
    	if (unrecognized != null) {
    		response.addResponseHeader("X-IBM-Unrecognized-Fields", unrecognized);
    	}

    	// Query for jobs
        List<WSJobInstance> jobInstances = doJobInstanceSearch(request, version);

        // Extract job instance ids returned by the query given the input parameters
        ArrayList<Long> instanceList = new ArrayList<Long>(jobInstances.size());
        for (WSJobInstance job : jobInstances) {
            instanceList.add(job.getInstanceId());
        }

        // Purge
        ArrayList<WSPurgeResponse> purgeResponseList = new ArrayList<WSPurgeResponse>(instanceList.size());
        for (long instanceId : instanceList) {
            try {
                purgeResponseList.add(purgeJobInstance("true".equalsIgnoreCase(request.getParameter("purgeJobStoreOnly")), instanceId,request));
            } catch (Exception e) {
                purgeResponseList.add(new WSPurgeResponse(instanceId,
                                PurgeStatus.FAILED, "Exception ocurred during purge of job instance " + instanceId + " : " + e.getMessage(),
                                null));

            }
        }

        // Note: headers must be set *before* writing to the output stream
        response.setContentType(BatchJSONHelper.MEDIA_TYPE_APPLICATION_JSON);

        BatchJSONHelper.buildAndWritePurgeJsonObject(purgeResponseList, response.getOutputStream());

    }

    /**
     * Purges the DB and/or File System for the incoming jobInstanceId
     * @param request
     * @param response
     * @param jobInstanceId
     * @return
     */
    @FFDCIgnore(NoSuchJobInstanceException.class)
    protected WSPurgeResponse purgeJobInstance(boolean purgeJobStoreOnly, long jobInstanceId, RESTRequest request) throws Exception {

        WSPurgeResponse purgeResponse = new WSPurgeResponse(jobInstanceId, PurgeStatus.COMPLETED, "Successful purge.", null);

        try {

            if (!jobRepository.isJobInstancePurgeable(jobInstanceId)) {
                purgeResponse.setMessage("The specified job instance, " + jobInstanceId + ", cannot be purged because it has active job executions.");
                purgeResponse.setPurgeStatus(PurgeStatus.STILL_ACTIVE);
                return purgeResponse;
            }
        } catch (NoSuchJobInstanceException e) {
   		   //Defect 195757: changing exception to have a translated message
           purgeResponse.setMessage(ResourceBundleRest.getMessage("job.instance.not.found", jobInstanceId));
    	   purgeResponse.setPurgeStatus(PurgeStatus.FAILED);
           return purgeResponse;
        }

        if (!purgeJobStoreOnly) { //Try to purge job logs and DB, or send a redirect if not local

            boolean fileSuccess = false;
            
            JobInstanceLog instanceLog = null;
            try {
            	instanceLog = jobLogManagerService.getJobInstanceLog(jobInstanceId);
            } catch (NoSuchJobInstanceException e) {
            	throw new BatchNoSuchJobInstanceException(e, jobInstanceId);
            }


            if (instanceLog.areExecutionsLocal()) {
            	
                fileSuccess = instanceLog.purge();

            } else {
            	// Send requests to all endpoints to delete their local job logs.
            	List<String> restUrls = findJobExecutionEndpoints(jobInstanceId);
            	String responses = "";

            	for (String restUrl : restUrls) {
            		try {
            			HttpURLConnection connection = BatchRequestUtil.sendRESTRequest(BatchRequestUtil.buildJoblogsUrlForJobInstance(jobInstanceId, restUrl, ""),
            																			"DELETE",
            																			request,
            																			null);

            			if (connection != null) {
        					// Log the response code, and if it failed, send an error response
            				responses = responses.concat("A request to " + connection.getURL().getPath() + "/" + BatchRequestUtil.getUrlVersion(request.getURL()) + " returned response code " +  connection.getResponseCode() + ". ");
                			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                				purgeResponse.setMessage("An error occurred while purging the job instance (" + jobInstanceId + "). "
                						+ "Not all job log files were deleted so no attempt was made to delete database entries. " + responses);
                				purgeResponse.setPurgeStatus(PurgeStatus.FAILED);
                			}
        				} else {
        					// Null connection means we failed to connect, but can't report a response code
        					responses = responses.concat("A request to " + BatchRequestUtil.buildJoblogsUrlForJobInstance(jobInstanceId, restUrl, "") + " failed. ");
        					purgeResponse.setMessage("An error occurred while purging the job instance (" + jobInstanceId + "). "
        							+ "Not all job log files were deleted so no attempt was made to delete database entries. " + responses);
        					purgeResponse.setPurgeStatus(PurgeStatus.FAILED);
        				}
            		} catch (Exception ex) {
        				responses = responses.concat("Exception occurred during a request to " + BatchRequestUtil.buildJoblogsUrlForJobInstance(jobInstanceId, restUrl, "") +
								 ", exception details: " + ex.getClass().getName() + ": " + ex.getLocalizedMessage());
            			purgeResponse.setMessage("An exception occurred while purging the job instance (" + jobInstanceId + "). "
            					+ "Not all job log files were deleted so no attempt was made to delete database entries." + responses);
        				purgeResponse.setPurgeStatus(PurgeStatus.FAILED);
            		}

            	}
            	if (purgeResponse.getPurgeStatus() == PurgeStatus.COMPLETED) {
            		fileSuccess = true;
            	}
            }

            if (fileSuccess) {
                boolean dbSuccess = wsJobOperator.purgeJobInstance(jobInstanceId);

                if (!dbSuccess) {
                    purgeResponse.setMessage("An error occurred while purging the job instance (" + jobInstanceId + "). "
                                             + "The job logs were sucessfully deleted but not all database entries were deleted.");
                    purgeResponse.setPurgeStatus(PurgeStatus.JOBLOGS_ONLY);
                }
            } else {
                purgeResponse.setMessage("An error occurred while purging the job instance (" + jobInstanceId + "). "
                                         + "Not all job log files were deleted so no attempt was made to delete database entries.");
                purgeResponse.setPurgeStatus(PurgeStatus.FAILED);
            }

        } else { //We won't even try to purge job logs. Only purge from database.
        	boolean dbSuccess = wsJobOperator.purgeJobInstance(jobInstanceId);

            if (!dbSuccess) {
                purgeResponse.setMessage("An error occurred while purging the job instance (" + jobInstanceId + "). Not all database entries were deleted.");
                purgeResponse.setPurgeStatus(PurgeStatus.FAILED);
            }
        }

        return purgeResponse;
    }

    /*
     * Returns a REST URL of the endpoint where the job executions were run. If all the jobs wen't run
     * on the same endpoint we throw an exception.
     */
    private String findSingleJobExecutionEndpoint(long jobInstanceId) {

        // FYI If we got here then we're guaranteed that there's at least one jobexecution.
        // TODO: if WsJobRepository is totally internal then we could convert its RVs to
        //       WSJobExecution/WSJobInstance to simplify things for the callers
        List<WSJobExecution> jobExecutions = jobRepository.getJobExecutionsFromInstance(jobInstanceId);

        List<String> restUrls = new ArrayList<String>();
        for (JobExecution jobExecution : jobExecutions) {
            restUrls.add(((WSJobExecution) jobExecution).getRestUrl());
        }

        if (StringUtils.areEqual(restUrls)) {

            String restUrl = ((WSJobExecution)jobExecutions.get(0)).getRestUrl();

            return restUrl;
        } else {
            // TODO: use different response code than 500 ??
            throw new BatchRuntimeException("Cannot provide a single endpoint for job instance " + jobInstanceId
                                            + " because its job executions ran on different endpoints: " + restUrls);
        }
    }

    /**
     * Returns REST URLs for all endpoints with executions for the given job instance.
     */
    private List<String> findJobExecutionEndpoints(long jobInstanceId) {
        List<WSJobExecution> jobExecutions = jobRepository.getJobExecutionsFromInstance(jobInstanceId);

        List<String> restUrls = new ArrayList<String>();
        for (JobExecution jobExecution : jobExecutions) {
            restUrls.add(((WSJobExecution) jobExecution).getRestUrl());
            List<WSRemotablePartitionExecution> remotePartitions = jobRepository.getRemotablePartitionsForJobExecution(jobExecution.getExecutionId());
            if (remotePartitions != null) {
            	for (WSRemotablePartitionExecution partition : remotePartitions) {
            		restUrls.add(partition.getRestUrl());
            	}
            }
        }
        return restUrls;
    }

    /*
     * Purges the local job logs given the instanceId, and the URL's host/port.
     */
    protected void purgeLocalJobLogs(RESTRequest request, RESTResponse response, long jobInstanceId) {

    	JobInstanceLog instanceLog = null;
    	try {
    		instanceLog = jobLogManagerService.getLocalJobInstanceLog(jobInstanceId);
    		instanceLog.purge();

    	} catch (NoSuchJobInstanceException e) {
        	// If no Instance is found, throw an exception similar to existing single purge
        	throw new BatchNoSuchJobInstanceException(e, jobInstanceId);
        }
    }

}
