/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.rest.internal.resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;

import javax.batch.operations.JobExecutionAlreadyCompleteException;
import javax.batch.operations.JobExecutionNotMostRecentException;
import javax.batch.operations.JobExecutionNotRunningException;
import javax.batch.operations.JobRestartException;
import javax.batch.operations.JobSecurityException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.JobInstance;

import org.apache.commons.io.IOUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.jbatch.container.ws.BatchJobNotLocalException;
import com.ibm.jbatch.container.ws.WSJobExecution;
import com.ibm.jbatch.container.ws.WSJobInstance;
import com.ibm.jbatch.container.ws.WSJobRepository;
import com.ibm.jbatch.container.ws.WSStepThreadExecutionAggregate;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jbatch.joblog.JobExecutionLog;
import com.ibm.ws.jbatch.joblog.JobExecutionLog.LogLocalState;
import com.ibm.ws.jbatch.joblog.RemotePartitionLog;
import com.ibm.ws.jbatch.joblog.services.IJobLogManagerService;
import com.ibm.ws.jbatch.rest.BatchManager;
import com.ibm.ws.jbatch.rest.internal.BatchNoSuchJobExecutionException;
import com.ibm.ws.jbatch.rest.internal.BatchRequestUtil;
import com.ibm.ws.jbatch.rest.internal.ZipHelper;
import com.ibm.ws.jbatch.rest.utils.BatchJSONHelper;
import com.ibm.ws.jbatch.rest.utils.JobRestartModel;
import com.ibm.ws.jbatch.rest.utils.StringUtils;
import com.ibm.wsspi.rest.handler.RESTHandler;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

@Component(service = { RESTHandler.class },
                configurationPolicy = ConfigurationPolicy.IGNORE,
                immediate = true,
                property = {
                            "service.vendor=IBM",
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_JOBEXECUTIONS_PATH,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_JOBEXECUTIONS_ID_PATH,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_JOBEXECUTIONS_ID_JOBINSTANCE_PATH,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_JOBEXECUTIONS_ID_JOBLOGS_PATH,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_JOBINSTANCES_ID_JOBEXECUTIONS_PATH,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_PATH,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_PATH_DESCRIPTIVE,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_JOBINSTANCES_ID_JOBEXECUTIONS_ID_JOBLOGS_PATH,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V1_JOBEXECUTIONS_PATH,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V1_JOBEXECUTIONS_ID_PATH,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V1_JOBEXECUTIONS_ID_JOBINSTANCE_PATH,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V1_JOBEXECUTIONS_ID_JOBLOGS_PATH,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V1_JOBINSTANCES_ID_JOBEXECUTIONS_PATH,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V1_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_PATH,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V1_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_PATH_DESCRIPTIVE,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V1_JOBINSTANCES_ID_JOBEXECUTIONS_ID_JOBLOGS_PATH,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V2_JOBEXECUTIONS_PATH,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V2_JOBEXECUTIONS_ID_PATH,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V2_JOBEXECUTIONS_ID_JOBINSTANCE_PATH,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V2_JOBEXECUTIONS_ID_JOBLOGS_PATH,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V2_JOBINSTANCES_ID_JOBEXECUTIONS_PATH,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V2_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_PATH,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V2_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_PATH_DESCRIPTIVE,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V2_JOBINSTANCES_ID_JOBEXECUTIONS_ID_JOBLOGS_PATH,              
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V3_JOBEXECUTIONS_PATH,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V3_JOBEXECUTIONS_ID_PATH,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V3_JOBEXECUTIONS_ID_JOBINSTANCE_PATH,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V3_JOBEXECUTIONS_ID_JOBLOGS_PATH,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V3_JOBINSTANCES_ID_JOBEXECUTIONS_PATH,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V3_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_PATH,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V3_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_PATH_DESCRIPTIVE,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V3_JOBINSTANCES_ID_JOBEXECUTIONS_ID_JOBLOGS_PATH,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V4_JOBEXECUTIONS_PATH,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V4_JOBEXECUTIONS_ID_PATH,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V4_JOBEXECUTIONS_ID_JOBINSTANCE_PATH,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V4_JOBEXECUTIONS_ID_JOBLOGS_PATH,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V4_JOBINSTANCES_ID_JOBEXECUTIONS_PATH,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V4_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_PATH,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V4_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_PATH_DESCRIPTIVE,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V4_JOBINSTANCES_ID_JOBEXECUTIONS_ID_JOBLOGS_PATH,
                            RESTHandler.PROPERTY_REST_HANDLER_CUSTOM_SECURITY + "=true"
                })
public class JobExecutions implements RESTHandler {
	
	private static final TraceComponent tc = Tr.register(JobExecutions.class, "wsbatch", "com.ibm.ws.jbatch.rest.resources.RESTMessages");

    private WSJobRepository jobRepository;

    /**
     * The guts of batch job management.
     */
    private BatchManager batchManager;

    /**
     * To access job logs.
     */
    private IJobLogManagerService jobLogManagerService;

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
     * Routes request to the appropriate handler.
     */
    private RequestRouter requestRouter = new RequestRouter()
                    .addHandler(new JobExecutionsHandler().setPath("/batch/jobexecutions"))
                    .addHandler(new JobInstanceJobExecutionsHandler().setPath("/batch/jobinstances/*/jobexecutions"))
                    .addHandler(new JobExecutionHandler().setPath("/batch/jobexecutions/*"))
                    .addHandler(new JobExecutionJobExecNumberHandler().setPath("/batch/jobinstances/*/jobexecutions/*"))
                    .addHandler(new JobExecutionJobExecNumberHandler().setPath("/batch/jobinstances/*/jobexecnum/*"))
                    .addHandler(new JobInstanceHandler().setPath("/batch/jobexecutions/*/jobinstance"))
                    .addHandler(new JobInstanceHandler().setPath("/batch/jobinstances/*/jobexecutions/*/jobinstance"))
                    .addHandler(new JobLogsHandler().setPath("/batch/jobexecutions/*/joblogs"))
                    .addHandler(new JobLogsHandler().setPath("/batch/jobinstances/*/jobexecutions/*/joblogs"))
                    .addHandler(new JobExecutionsHandler().setPath("/batch/v1/jobexecutions"))
                    .addHandler(new JobInstanceJobExecutionsHandler().setPath("/batch/v1/jobinstances/*/jobexecutions"))
                    .addHandler(new JobExecutionHandler().setPath("/batch/v1/jobexecutions/*"))
                    .addHandler(new JobExecutionJobExecNumberHandler().setPath("/batch/v1/jobinstances/*/jobexecutions/*"))
                    .addHandler(new JobExecutionJobExecNumberHandler().setPath("/batch/v1/jobinstances/*/jobexecnum/*"))
                    .addHandler(new JobInstanceHandler().setPath("/batch/v1/jobexecutions/*/jobinstance"))
                    .addHandler(new JobInstanceHandler().setPath("/batch/v1/jobinstances/*/jobexecutions/*/jobinstance"))
                    .addHandler(new JobLogsHandler().setPath("/batch/v1/jobexecutions/*/joblogs"))
                    .addHandler(new JobLogsHandler().setPath("/batch/v1/jobinstances/*/jobexecutions/*/joblogs"))
                    .addHandler(new JobExecutionsHandler().setPath("/batch/v2/jobexecutions"))
                    .addHandler(new JobInstanceJobExecutionsHandler().setPath("/batch/v2/jobinstances/*/jobexecutions"))
                    .addHandler(new JobExecutionHandler().setPath("/batch/v2/jobexecutions/*"))
                    .addHandler(new JobExecutionJobExecNumberHandler().setPath("/batch/v2/jobinstances/*/jobexecutions/*"))
                    .addHandler(new JobExecutionJobExecNumberHandler().setPath("/batch/v2/jobinstances/*/jobexecnum/*"))
                    .addHandler(new JobInstanceHandler().setPath("/batch/v2/jobexecutions/*/jobinstance"))
                    .addHandler(new JobInstanceHandler().setPath("/batch/v2/jobinstances/*/jobexecutions/*/jobinstance"))
                    .addHandler(new JobLogsHandler().setPath("/batch/v2/jobexecutions/*/joblogs"))
                    .addHandler(new JobLogsHandler().setPath("/batch/v2/jobinstances/*/jobexecutions/*/joblogs"))
                    .addHandler(new JobExecutionsHandler().setPath("/batch/v3/jobexecutions"))
                    .addHandler(new JobInstanceJobExecutionsHandler().setPath("/batch/v3/jobinstances/*/jobexecutions"))
                    .addHandler(new JobExecutionHandler().setPath("/batch/v3/jobexecutions/*"))
                    .addHandler(new JobExecutionJobExecNumberHandler().setPath("/batch/v3/jobinstances/*/jobexecutions/*"))
                    .addHandler(new JobExecutionJobExecNumberHandler().setPath("/batch/v3/jobinstances/*/jobexecnum/*"))
                    .addHandler(new JobInstanceHandler().setPath("/batch/v3/jobexecutions/*/jobinstance"))
                    .addHandler(new JobInstanceHandler().setPath("/batch/v3/jobinstances/*/jobexecutions/*/jobinstance"))
                    .addHandler(new JobLogsHandler().setPath("/batch/v3/jobexecutions/*/joblogs"))
                    .addHandler(new JobLogsHandler().setPath("/batch/v3/jobinstances/*/jobexecutions/*/joblogs"))
                    .addHandler(new JobExecutionsHandler().setPath("/batch/v4/jobexecutions"))
                    .addHandler(new JobInstanceJobExecutionsHandler().setPath("/batch/v4/jobinstances/*/jobexecutions"))
                    .addHandler(new JobExecutionHandler().setPath("/batch/v4/jobexecutions/*"))
                    .addHandler(new JobExecutionJobExecNumberHandler().setPath("/batch/v4/jobinstances/*/jobexecutions/*"))
                    .addHandler(new JobExecutionJobExecNumberHandler().setPath("/batch/v4/jobinstances/*/jobexecnum/*"))
                    .addHandler(new JobInstanceHandler().setPath("/batch/v4/jobexecutions/*/jobinstance"))
                    .addHandler(new JobInstanceHandler().setPath("/batch/v4/jobinstances/*/jobexecutions/*/jobinstance"))
                    .addHandler(new JobLogsHandler().setPath("/batch/v4/jobexecutions/*/joblogs"))
                    .addHandler(new JobLogsHandler().setPath("/batch/v4/jobinstances/*/jobexecutions/*/joblogs"));

    /**
     * @param request
     * @param response
     * @throws IOException
     */
    public void handleRequest(final RESTRequest request,
                              final RESTResponse response) throws IOException {
        try {
            // First verify the batch container is started.
            BatchRuntime.getJobOperator();

            requestRouter.routeRequest(request, response);
        } catch (JobSecurityException jse) {
            response.sendError(HttpURLConnection.HTTP_UNAUTHORIZED, jse.getMessage());
        } catch (BatchNoSuchJobExecutionException bnsje) {
            response.sendError(HttpURLConnection.HTTP_BAD_REQUEST, "Job execution " + bnsje.getJobExecutionId() + " not found.");
        } catch (Exception e) {
            response.sendError(HttpURLConnection.HTTP_INTERNAL_ERROR, e.getMessage());
        }
    }

    /**
     * Handles "/batch/jobexecutions",
     */
    private class JobExecutionsHandler extends RequestHandler {

    }

    /**
     * Handles "/batch/jobinstances/{jobinstanceid}/jobexecutions",
     */
    private class JobInstanceJobExecutionsHandler extends RequestHandler {

        public void get(RESTRequest request, RESTResponse response) throws Exception {
            getJobInstanceJobExecutions(request, response, getJobInstanceId(request));
        }
    }

    /**
     * Handles
     * "/batch/jobexecutions/{jobexecutionid}",
     */
    private class JobExecutionHandler extends RequestHandler {

        public void get(RESTRequest request, RESTResponse response) throws Exception {
            getJobExecutionData(request, response, getJobExecutionId(request));
        }

        public void put(RESTRequest request, RESTResponse response) throws Exception {
            String action = request.getParameter("action");

            if ("stop".equalsIgnoreCase(action)) {
                stopJobExecution(request, response, getJobExecutionId(request));
            } else if ("restart".equalsIgnoreCase(action)) {
                restartJobExecution(request, response, getJobExecutionId(request));
            } else {
                response.sendError(HttpURLConnection.HTTP_BAD_REQUEST, "Invalid HTTP query parameters: only action=stop and action=restart are supported.");
            }
        }
    }

    /**
     * Handles
     * "/batch/jobinstances/{jobinstanceid}/jobexecutions/{jobexecutionnumber}",
     */
    private class JobExecutionJobExecNumberHandler extends RequestHandler {

        public void get(RESTRequest request, RESTResponse response) throws Exception {
            getJobExecutionDataByJobExecNumber(request, response, getJobInstanceId(request), getJobExecutionNum(request));
        }
    }

    /**
     * Handles
     * "/batch/jobexecutions/{jobexecutionid}/jobinstance
     * "/batch/jobinstances/{jobinstanceid}/jobexecutions/{jobexecutionid}/jobinstance
     */
    private class JobInstanceHandler extends RequestHandler {

        public void get(RESTRequest request, RESTResponse response) throws Exception {
            getJobInstanceData(request, response, getJobExecutionId(request));
        }
    }

    /**
     * Handles
     * "/batch/jobexecutions/{jobexecutionid}/joblogs"
     * "/batch/jobexecutions/{jobexecutionid}/joblogs?type=zip|text
     * "/batch/jobexecutions/{jobexecutionid}/joblogs?part={part}&type=zip|text"
     * "/batch/jobinstances/{jobinstanceid}/jobexecutions/{jobexecutionid}/joblogs"
     * "/batch/jobinstances/{jobinstanceid}/jobexecutions/{jobexecutionid}/joblogs?type=zip|text"
     * "/batch/jobinstances/{jobinstanceid}/jobexecutions/{jobexecutionid}/joblogs?part={part}&type=zip|text"
     * 
     */
    private class JobLogsHandler extends RequestHandler {
    	public void get(RESTRequest request, RESTResponse response) throws Exception {
    		getJobLog(request, response, getJobExecutionId(request));
    	}

        /**
         * Get the joblog for the given jobExecutionId.
         */
        protected void getJobLog(RESTRequest request, RESTResponse response, long jobExecutionId) throws Exception {
            try {
                JobExecutionLog jobExecutionLog = jobLogManagerService.getJobExecutionLog(jobExecutionId);

                // Check the localOnly param here. If it's set, we just want the logs from this server, no cascading requests
                boolean localOnly = "true".equals(request.getParameter("localOnly"));
                
                if (localOnly || jobExecutionLog.getLocalState() == LogLocalState.EXECUTION_LOCAL) {
                	if (!StringUtils.isEmpty(request.getParameter("part"))) {
                        sendJobExecutionLogPart(jobExecutionLog, request, response);
                    } else {
                        sendJobExecutionLog(jobExecutionLog, request, response);
                    }
                } else {
                		BatchRequestUtil.handleNonLocalRequest(BatchRequestUtil.buildJoblogsUrl(jobExecutionLog.getJobExecution(), null, request.getQueryString()), 
                				"GET",
                				request,
                				response);
                } 

            } catch (NoSuchJobExecutionException e) {
                throw new RequestException(HttpURLConnection.HTTP_BAD_REQUEST, "A job with job execution id " + jobExecutionId + " does not exist.");
            }
        }

        /**
         * Send the JobExecutionLog in the given response.
         */
        protected void sendJobExecutionLog(JobExecutionLog jobExecutionLog, RESTRequest request, RESTResponse response) throws IOException {

        	if ("zip".equals(request.getParameter("type"))) {

        		ZipOutputStream zipOutput = new ZipOutputStream(response.getOutputStream());

        		// Note: headers must be set *before* writing to the output stream
        		response.setContentType("application/zip");
        		response.setResponseHeader("Content-Disposition", "attachment; filename=" + StringUtils.enquote(getZipFileName(jobExecutionLog)));

        		// If there are remote partition logs, fetch them now.
        		// The localOnly flag is used to prevent cascading requests
        		if (jobExecutionLog.getRemotePartitionLogs() != null &&
        				!("true".equals(request.getParameter("localOnly")))) {

        			HashSet<String> partitionEndpointURLs = jobExecutionLog.getRemotePartitionEndpointURLs();

        			// Ignore local URL because the logs would have already been collected with the top-level execution logs
        			partitionEndpointURLs.remove(BatchRequestUtil.getUrlRoot(request));

        			for (String url : partitionEndpointURLs) {
        				// Fetch the contents from the remote partition executor
        				String joblogUrl = BatchRequestUtil.buildJoblogsUrl(url, jobExecutionLog.getExecutionId()) + "?type=zip&localOnly=true";
        				try {
        					HttpsURLConnection conn = BatchRequestUtil.sendRESTRequest(joblogUrl, "GET", request, null);

        					if (conn != null) {
        						// Copy zip entries from the remote request
        						ZipInputStream zipStream = new ZipInputStream(conn.getInputStream());
        						ZipHelper.copyZipEntries(zipStream, zipOutput);
        					} 
        				} catch (Exception ex) {
        					Tr.debug(tc, "Exception occurred fetching remote partition logs from " + joblogUrl +
        							", exception details: " + ex.getClass().getName() + ": " + ex.getLocalizedMessage());
        				}
        			}

        		}

        		ZipHelper.zipFilesToStream(jobExecutionLog.getJobLogFiles(),
        								   jobExecutionLog.getExecLogRootDir(),
        								   zipOutput);

        	} else if ("text".equals(request.getParameter("type"))) {

        		// Note: headers must be set *before* writing to the output stream
        		response.setContentType("text/plain; charset=UTF-8");

        		ZipHelper.aggregateFilesToStream(jobExecutionLog.getJobLogFiles(),
        				jobExecutionLog.getExecLogRootDir(),
        				response.getOutputStream());

        		// If there are remote partition logs, fetch them now.
        		// The localOnly flag is used to prevent cascading requests
        		if (jobExecutionLog.getRemotePartitionLogs() != null &&
        				!("true".equals(request.getParameter("localOnly")))) {

        			HashSet<String> partitionEndpointURLs = jobExecutionLog.getRemotePartitionEndpointURLs();

        			// Ignore local URL because the logs would have already been collected with the top-level execution logs
        			partitionEndpointURLs.remove(BatchRequestUtil.getUrlRoot(request));

        			// Fetch the contents from the remote partition executors
        			for (String url : partitionEndpointURLs) {
        				HttpsURLConnection conn = BatchRequestUtil.sendRESTRequest(
        						BatchRequestUtil.buildJoblogsUrl(url, jobExecutionLog.getExecutionId()) + "?type=text&localOnly=true", 
        						"GET",
        						request,
        						null);

        				// TODO what if the request fails? CGCG

        				// Copy job log text from the remote request
        				byte[] buf = new byte[1024];
        				int len;
        				while ((len = conn.getInputStream().read(buf)) != -1) {
        					response.getOutputStream().write(buf, 0, len);
        				}
        			}
        		}
        	} else {

        		// Send back a json array with REST links to each part.

        		// Note: headers must be set *before* writing to the output stream
        		response.setContentType(BatchJSONHelper.MEDIA_TYPE_APPLICATION_JSON);

        		BatchJSONHelper.writeJobExecutionLogLinks(jobExecutionLog,
        				BatchRequestUtil.getUrlRoot(request),
        				response.getOutputStream());
        	}
        }

        /**
         * Send a single part from the given JobExecutionLog on the given response.
         */
        protected void sendJobExecutionLogPart(JobExecutionLog jobExecutionLog, RESTRequest request, RESTResponse response) throws IOException, RequestException {

            String part = request.getParameter("part");
            File jobLogFile = jobExecutionLog.getPartByRelativePath(part);

            if (jobLogFile == null) {
        		throw new RequestException(HttpURLConnection.HTTP_BAD_REQUEST, "The job log part " + part + " does not exist for job execution " + jobExecutionLog.getExecutionId() + ".");
            }

            if ("zip".equals(request.getParameter("type"))) {

                // Note: headers must be set *before* writing to the output stream
                response.setContentType("application/zip");
                response.setResponseHeader("Content-Disposition", "attachment; filename="
                                                                  + StringUtils.enquote(getZipFileName(jobExecutionLog, request.getParameter("part"))));

                ZipHelper.zipFileToStream(jobLogFile,
                                          jobExecutionLog.getExecLogRootDir(),
                                          response.getOutputStream());

            } else {

                // TODO: encoding? are the joblogs guaranteed to be ASCII? If not I should probably use a Reader
                //       instead of an InputStream and specify the file encoding for writing to the output stream.
                // Note: the FileInputStream is closed by copyStream.
                response.setContentType("text/plain; charset=UTF-8");
                ZipHelper.copyStream(new FileInputStream(jobLogFile), response.getOutputStream());
            }
        }

        /**
         * @return a suggested zipfile name for the given jobexecution
         */
        protected String getZipFileName(JobExecutionLog jobExecutionLog) {
            return "joblogs.execution." + jobExecutionLog.getExecutionId() + ".zip";
        }

        /**
         * @return a suggested zipfile name for the given jobexecution and part
         */
        protected String getZipFileName(JobExecutionLog jobExecutionLog, String relativePartPath) {
            return "joblogs.execution."
                   + jobExecutionLog.getExecutionId()
                   + "."
                   // Note: replaceAll is expensive (compiles a regex), but we're not on a perf path.
                   + relativePartPath.replaceAll("\\\\", ".").replaceAll("/", ".")
                   + ".zip";
        }
    }

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

    /**
     * @return the job execution ID as a Long
     * 
     * @throws IOException
     */
    protected long getJobExecutionId(RESTRequest request) throws RequestException {
        String s = request.getPathVariable("jobexecutionid");
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException nfe) {
            throw new RequestException(HttpURLConnection.HTTP_BAD_REQUEST,
                            "The job execution id (" + s + ") must be a Long integer",
                            nfe);
        }
    }

    /**
     * Lookup jobexecutions for the given jobInstanceID and marshal them to the response
     * stream as JSON.
     */
    private void getJobInstanceJobExecutions(final RESTRequest request,
                                             final RESTResponse response,
                                             long jobInstanceId) throws IOException {

        // Note: headers must be set *before* writing to the output stream
        response.setContentType(BatchJSONHelper.MEDIA_TYPE_APPLICATION_JSON);

        List<WSJobExecution> jobExecutionList = new ArrayList<WSJobExecution>();
        Map<Long, List<WSStepThreadExecutionAggregate>> jobExecStepExecListMap = new HashMap<Long, List<WSStepThreadExecutionAggregate>>();
        for (WSJobExecution jobExec : jobRepository.getJobExecutionsFromInstance(jobInstanceId)) {
            jobExecutionList.add(jobExec);
            jobExecStepExecListMap.put(jobExec.getExecutionId(), jobRepository.getStepExecutionAggregatesFromJobExecution(jobExec.getExecutionId()));
        }
        BatchJSONHelper.writeJobExecutionList(jobExecutionList, jobExecStepExecListMap,
                                              BatchRequestUtil.getUrlRoot(request),
                                              response.getOutputStream());
    }

    /**
     * Retrieve and return the given jobExecution record.
     */
    private void getJobExecutionData(final RESTRequest request,
                                     final RESTResponse response,
                                     long jobExecutionId) throws IOException, RequestException {

        //Verify that job instance id matches execution id
        String instanceIdString = request.getPathVariable("jobinstanceid");
        Long instanceId = null;

        try {
            if (instanceIdString != null) {
                instanceId = Long.parseLong(instanceIdString);
            }
        } catch (NumberFormatException e) {
            throw new RequestException(HttpURLConnection.HTTP_BAD_REQUEST, "The job instance id must be a Long integer.");
        }

        WSJobExecution jobExec = null;
        try {
            jobExec = jobRepository.getJobExecution(jobExecutionId);

            // Verify the job execution id maps to the given job instance id
            if (instanceId != null) {
                if (!execIdMapsToInstanceId(jobExecutionId, instanceId)) {
                    throw new RequestException(HttpURLConnection.HTTP_BAD_REQUEST, "The job execution id " + jobExecutionId + " does not map to job instance id "+ instanceId + ".");
                }
            }

            // Note: headers must be set *before* writing to the output stream
            response.setContentType(BatchJSONHelper.MEDIA_TYPE_APPLICATION_JSON);

            List<WSStepThreadExecutionAggregate> stepExecAggregateList = jobRepository.getStepExecutionAggregatesFromJobExecution(jobExecutionId);

            BatchJSONHelper.writeJobExecution(jobExec, stepExecAggregateList, BatchRequestUtil.getUrlRoot(request), response.getOutputStream());

        } catch (NoSuchJobExecutionException e) {
            throw new RequestException(HttpURLConnection.HTTP_BAD_REQUEST, "Job execution " + jobExecutionId + " not found.");
        }

    }

    /**
     * Retrieve and return the given jobExecution record.
     */
    private void getJobInstanceData(final RESTRequest request,
                                    final RESTResponse response,
                                    long jobExecutionId) throws IOException, RequestException {
        try {

            // Note: headers must be set *before* writing to the output stream
            response.setContentType(BatchJSONHelper.MEDIA_TYPE_APPLICATION_JSON);

            BatchJSONHelper.writeJobInstance(jobRepository.getJobInstanceFromExecution(jobExecutionId),
                                             BatchRequestUtil.getUrlRoot(request),
                                             response.getOutputStream());

        } catch (NoSuchJobExecutionException e) {
            throw new RequestException(HttpURLConnection.HTTP_BAD_REQUEST, "A job with job execution id " + jobExecutionId + " does not exist.");
        }
    }

    /**
     * If this is no current running execution return an error.
     * @throws RequestException
     */
    @FFDCIgnore(BatchJobNotLocalException.class)
    protected void stopJobExecution(final RESTRequest request,
                                    final RESTResponse response,
                                    long jobExecutionId) throws IOException, RequestException {

        try {

            batchManager.stopJobExecution(jobExecutionId);

            // Note: headers must be set *before* writing to the output stream
            response.setContentType(BatchJSONHelper.MEDIA_TYPE_APPLICATION_JSON);

            List<WSStepThreadExecutionAggregate> stepExecAggregateList = jobRepository.getStepExecutionAggregatesFromJobExecution(jobExecutionId);
            
            BatchJSONHelper.writeJobExecution(jobRepository.getJobExecution(jobExecutionId), stepExecAggregateList,
                                              BatchRequestUtil.getUrlRoot(request),
                                              response.getOutputStream());

        } catch (JobExecutionNotRunningException e) {
            response.sendError(HttpURLConnection.HTTP_CONFLICT, "The job execution id " + jobExecutionId + " is not currently running.");
        } catch (NoSuchJobExecutionException e) {
            response.sendError(HttpURLConnection.HTTP_BAD_REQUEST, "A job with job execution id " + jobExecutionId + " does not exist.");
        } catch (BatchJobNotLocalException e) {
        	BatchRequestUtil.handleNonLocalRequest(BatchRequestUtil.buildStopUrl(e.getJobExecution(), null), 
        			"PUT",
        			request,
        			response);
        }
    }

    /**
     * If this is no current running execution return an error.
     * @throws IOException
     */
    protected void restartJobExecution(RESTRequest request,
                                       RESTResponse response,
                                       long jobExecutionId) throws IOException, RequestException {

        // Read the jobParams from the request body, if any.
        Properties jobParams = (BatchRequestUtil.getContentLength(request) > 0)
                        ? new JobRestartModel(BatchJSONHelper.readJsonObject(request.getInputStream())).getJobParameters()
                        : null;

        jobParams = ("true".equalsIgnoreCase(request.getParameter("reusePreviousParams")))
                        ? BatchJSONHelper.mergeProperties(jobRepository.getJobExecution(jobExecutionId).getJobParameters(),
                                                          jobParams)
                        : jobParams;

        try {

            WSJobInstance jobInstance = batchManager.restartJobExecution(jobExecutionId, jobParams);

            // Note: headers must be set *before* writing to the output stream
            response.setContentType(BatchJSONHelper.MEDIA_TYPE_APPLICATION_JSON);

            BatchJSONHelper.writeJobInstance(jobInstance,
                                             BatchRequestUtil.getUrlRoot(request),
                                             response.getOutputStream());

        } catch (NoSuchJobExecutionException e) {
            response.sendError(HttpURLConnection.HTTP_BAD_REQUEST, "The job execution id " + jobExecutionId + " does not exist.");
        } catch (JobExecutionAlreadyCompleteException e) {
            response.sendError(HttpURLConnection.HTTP_CONFLICT, "The job execution id " + jobExecutionId + " is already completed.");
        } catch (JobExecutionNotMostRecentException e) {
            response.sendError(HttpURLConnection.HTTP_BAD_REQUEST, "The job execution id " + jobExecutionId + " is not the most recent execution.");
        } catch (JobRestartException e) {
            response.sendError(HttpURLConnection.HTTP_CONFLICT, "The job execution id " + jobExecutionId + " could not be restarted.");
        }

    }

    /*
     * Verify if the given executionId actually maps to the given instance id, if not, return false.
     */
    private boolean execIdMapsToInstanceId(Long execId, Long instanceId) {

        JobInstance actualJobInstance = jobRepository.getJobInstanceFromExecution(execId);
        if (actualJobInstance.getInstanceId() != instanceId.longValue()) {

            return false;
        }

        return true;
    }

    private void getJobExecutionDataByJobExecNumber(final RESTRequest request,
                                                    final RESTResponse response, long jobInstanceId, short jobExecutionNum)
                    throws IOException, RequestException {

        WSJobExecution jobExec = null;
        try {
            jobExec = jobRepository.getJobExecutionFromJobExecNum(jobInstanceId, jobExecutionNum);

            // Note: headers must be set *before* writing to the output stream
            response.setContentType(BatchJSONHelper.MEDIA_TYPE_APPLICATION_JSON);

            List<WSStepThreadExecutionAggregate> stepExecAggregateList = jobRepository.getStepExecutionAggregatesFromJobExecution(jobExec.getExecutionId());

            BatchJSONHelper.writeJobExecution(jobExec, stepExecAggregateList,
                                              BatchRequestUtil.getUrlRoot(request),
                                              response.getOutputStream());

        } catch (NoSuchJobExecutionException e) {
            throw new RequestException(HttpURLConnection.HTTP_BAD_REQUEST,
                            "Job instance " + jobInstanceId + " and job execution number " + jobExecutionNum + " not found.");
        }

    }

    /**
     * 
     * @param request
     * @return the job execution number as a short
     * @throws RequestException
     */
    protected short getJobExecutionNum(RESTRequest request)
                    throws RequestException {
        String s = request.getPathVariable("jobexecutionnumber");
        try {
            return Short.parseShort(s);
        } catch (NumberFormatException nfe) {
            throw new RequestException(HttpURLConnection.HTTP_BAD_REQUEST,
                            "The job execution number (" + s + ") must be a Short integer",
                            nfe);
        }
    }
}

