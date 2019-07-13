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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.batch.operations.JobSecurityException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.runtime.BatchRuntime;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.jbatch.container.ws.WSJobRepository;
import com.ibm.jbatch.container.ws.WSStepThreadExecutionAggregate;
import com.ibm.ws.jbatch.rest.internal.BatchRequestUtil;
import com.ibm.ws.jbatch.rest.utils.BatchJSONHelper;
import com.ibm.ws.jbatch.rest.utils.StringUtils;
import com.ibm.wsspi.rest.handler.RESTHandler;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

@Component(service = { RESTHandler.class },
                configurationPolicy = ConfigurationPolicy.IGNORE,
                immediate = true,
                property = {
                            "service.vendor=IBM",
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_JOBEXECUTIONS_ID_STEPEXECUTIONS,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_JOBEXECUTIONS_ID_STEPEXECUTIONS_NAME,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_STEPEXECUTIONS_ID,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_STEPEXECUTIONS_NAME,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_STEPEXECUTIONS_NAME_DESCRIPTIVE,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V1_JOBEXECUTIONS_ID_STEPEXECUTIONS,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V1_JOBEXECUTIONS_ID_STEPEXECUTIONS_NAME,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V1_STEPEXECUTIONS_ID,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V1_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_STEPEXECUTIONS_NAME,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V1_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_STEPEXECUTIONS_NAME_DESCRIPTIVE,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V2_JOBEXECUTIONS_ID_STEPEXECUTIONS,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V2_JOBEXECUTIONS_ID_STEPEXECUTIONS_NAME,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V2_STEPEXECUTIONS_ID,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V2_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_STEPEXECUTIONS_NAME,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V2_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_STEPEXECUTIONS_NAME_DESCRIPTIVE,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V3_JOBEXECUTIONS_ID_STEPEXECUTIONS,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V3_JOBEXECUTIONS_ID_STEPEXECUTIONS_NAME,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V3_STEPEXECUTIONS_ID,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V3_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_STEPEXECUTIONS_NAME,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V3_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_STEPEXECUTIONS_NAME_DESCRIPTIVE,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V4_JOBEXECUTIONS_ID_STEPEXECUTIONS,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V4_JOBEXECUTIONS_ID_STEPEXECUTIONS_NAME,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V4_STEPEXECUTIONS_ID,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V4_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_STEPEXECUTIONS_NAME,
                            RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_V4_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_STEPEXECUTIONS_NAME_DESCRIPTIVE,
                            RESTHandler.PROPERTY_REST_HANDLER_CUSTOM_SECURITY + "=true"
                })
public class StepExecutions implements RESTHandler {

    private WSJobRepository jobRepository;

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
     * Routes request to the appropriate handler.
     */
    private RequestRouter requestRouter = new RequestRouter()
                    .addHandler(new StepExecutionsHandler("/batch/jobexecutions/*/stepexecutions"))
                    .addHandler(new StepExecutionByStepNameHandler("/batch/jobexecutions/*/stepexecutions/*"))
                    .addHandler(new StepExecutionHandler("/batch/stepexecutions/*"))
                    .addHandler(new StepExecutionByJobExecNumberAndStepNameHandler("/batch/jobinstances/*/jobexecutions/*/stepexecutions/*"))
                    .addHandler(new StepExecutionByJobExecNumberAndStepNameHandler("/batch/jobinstances/*/jobexecnum/*/stepexecutions/*"))
                    .addHandler(new StepExecutionsHandler("/batch/v1/jobexecutions/*/stepexecutions"))
                    .addHandler(new StepExecutionByStepNameHandler("/batch/v1/jobexecutions/*/stepexecutions/*"))
                    .addHandler(new StepExecutionHandler("/batch/v1/stepexecutions/*"))
                    .addHandler(new StepExecutionByJobExecNumberAndStepNameHandler("/batch/v1/jobinstances/*/jobexecutions/*/stepexecutions/*"))
                    .addHandler(new StepExecutionByJobExecNumberAndStepNameHandler("/batch/v1/jobinstances/*/jobexecnum/*/stepexecutions/*"))
                    .addHandler(new StepExecutionsHandler("/batch/v2/jobexecutions/*/stepexecutions"))
                    .addHandler(new StepExecutionByStepNameHandler("/batch/v2/jobexecutions/*/stepexecutions/*"))
                    .addHandler(new StepExecutionHandler("/batch/v2/stepexecutions/*"))
                    .addHandler(new StepExecutionByJobExecNumberAndStepNameHandler("/batch/v2/jobinstances/*/jobexecutions/*/stepexecutions/*"))
                    .addHandler(new StepExecutionByJobExecNumberAndStepNameHandler("/batch/v2/jobinstances/*/jobexecnum/*/stepexecutions/*"))
                    .addHandler(new StepExecutionsHandler("/batch/v3/jobexecutions/*/stepexecutions"))
                    .addHandler(new StepExecutionByStepNameHandler("/batch/v3/jobexecutions/*/stepexecutions/*"))
                    .addHandler(new StepExecutionHandler("/batch/v3/stepexecutions/*"))
                    .addHandler(new StepExecutionByJobExecNumberAndStepNameHandler("/batch/v3/jobinstances/*/jobexecutions/*/stepexecutions/*"))
                    .addHandler(new StepExecutionByJobExecNumberAndStepNameHandler("/batch/v3/jobinstances/*/jobexecnum/*/stepexecutions/*"))
                    .addHandler(new StepExecutionsHandler("/batch/v4/jobexecutions/*/stepexecutions"))
                    .addHandler(new StepExecutionByStepNameHandler("/batch/v4/jobexecutions/*/stepexecutions/*"))
                    .addHandler(new StepExecutionHandler("/batch/v4/stepexecutions/*"))
                    .addHandler(new StepExecutionByJobExecNumberAndStepNameHandler("/batch/v4/jobinstances/*/jobexecutions/*/stepexecutions/*"))
                    .addHandler(new StepExecutionByJobExecNumberAndStepNameHandler("/batch/v4/jobinstances/*/jobexecnum/*/stepexecutions/*"));

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
        } catch (Exception e) {
            response.sendError(HttpURLConnection.HTTP_INTERNAL_ERROR, e.getMessage());
        }
    }

    /**
     * Handles "/batch/jobexecutions/{jobexecutionid}/stepexecutions",
     */
    private class StepExecutionsHandler extends RequestHandler {
        public StepExecutionsHandler(String path) {
            super(path);
        }

        public void get(RESTRequest request, RESTResponse response) throws Exception {
            getStepExecutionsData(request, response, getJobExecutionId(request));
        }
    }

    /**
     * Handles "/batch/jobexecutions/{jobexecutionid}/stepexecutions/{stepname}",
     */
    private class StepExecutionByStepNameHandler extends RequestHandler {
        public StepExecutionByStepNameHandler(String path) {
            super(path);
        }

        public void get(RESTRequest request, RESTResponse response) throws Exception {
            getStepExecutionsDataByStepName(request, response, getJobExecutionId(request), getStepName(request));
        }
    }

    /**
     * Handles "/batch/jobinstances/{jobinstanceid}/jobexecutions/{jobexecutionnumber}/stepexecutions/{stepname}"
     */
    private class StepExecutionByJobExecNumberAndStepNameHandler extends RequestHandler {
        public StepExecutionByJobExecNumberAndStepNameHandler(String path) {
            super(path);
        }

        public void get(RESTRequest request, RESTResponse response) throws Exception {
            getStepExecutionDataByJobExecNumAndStepName(request, response, getJobInstanceId(request), getJobExecutionNum(request), getStepName(request));
        }
    }

    /**
     * Handles "/batch/stepexecutions/{stepexecutionid}",
     */
    private class StepExecutionHandler extends RequestHandler {
        public StepExecutionHandler(String path) {
            super(path);
        }

        public void get(RESTRequest request, RESTResponse response)
                        throws Exception {
            getStepExecution(request, response, getStepExecutionId(request));
        }
    }

    /**
     * Gets the Step Execution given the Step Id value
     * @param request
     * @param response
     * @param jobExecutionId
     * @param stepId
     * @throws IOException
     */
    public void getStepExecutionsDataByStepName(RESTRequest request,
                                                RESTResponse response, long jobExecutionId, String stepName) throws IOException {

        List<WSStepThreadExecutionAggregate> stepExecAggregateList = new ArrayList<WSStepThreadExecutionAggregate>();

        try {

            stepExecAggregateList.add(jobRepository.getStepExecutionAggregateFromJobExecution(jobExecutionId, stepName));

            // Note: headers must be set *before* writing to the output stream
            response.setContentType(BatchJSONHelper.MEDIA_TYPE_APPLICATION_JSON);

            BatchJSONHelper.writeStepExecutionsList(stepExecAggregateList,
                                                    BatchRequestUtil.getUrlRoot(request),
                                                    response.getOutputStream());

        } catch (NoSuchJobExecutionException e) {
            response.sendError(HttpURLConnection.HTTP_BAD_REQUEST, "Job Execution Id " + jobExecutionId + " with Step Name " + stepName + " not found.");
        } catch (IllegalArgumentException e) {
            response.sendError(HttpURLConnection.HTTP_BAD_REQUEST, "Job Execution Id " + jobExecutionId + " with Step Name " + stepName + " not found.");
        }

    }

    /**
     * Gets the Step Execution given the Step Execution Id
     * @param request
     * @param response
     * @param stepExecutionId
     * @throws IOException
     */
    public void getStepExecution(RESTRequest request,
                                 RESTResponse response, long stepExecutionId) throws IOException {

        List<WSStepThreadExecutionAggregate> stepExecAggregateList = new ArrayList<WSStepThreadExecutionAggregate>();

        try {

            stepExecAggregateList.add(jobRepository.getStepExecutionAggregate(stepExecutionId));

            // Note: headers must be set *before* writing to the output stream
            response.setContentType(BatchJSONHelper.MEDIA_TYPE_APPLICATION_JSON);

            BatchJSONHelper.writeStepExecutionsList(stepExecAggregateList,
                                                    BatchRequestUtil.getUrlRoot(request),
                                                    response.getOutputStream());

        } catch (IllegalArgumentException e) {
            response.sendError(HttpURLConnection.HTTP_BAD_REQUEST, "Step execution " + stepExecutionId + " not found.");
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
     * 
     * @param request
     * @param response
     * @param jobExecutionId
     * @throws IOException
     */
    public void getStepExecutionsData(RESTRequest request,
                                      RESTResponse response, long jobExecutionId) throws IOException {

        try {

            // Get the Step Execution aggregates
            List<WSStepThreadExecutionAggregate> stepExecAggregateList = jobRepository.getStepExecutionAggregatesFromJobExecution(jobExecutionId);

            // Note: headers must be set *before* writing to the output stream
            response.setContentType(BatchJSONHelper.MEDIA_TYPE_APPLICATION_JSON);

            BatchJSONHelper.writeStepExecutionsList(stepExecAggregateList,
                                                    BatchRequestUtil.getUrlRoot(request),
                                                    response.getOutputStream());

        } catch (NoSuchJobExecutionException e) {
            response.sendError(HttpURLConnection.HTTP_BAD_REQUEST, "Job execution " + jobExecutionId + " not found.");
        } catch (IllegalArgumentException e) {
            response.sendError(HttpURLConnection.HTTP_BAD_REQUEST, "Job execution " + jobExecutionId + " not found.");
        }

    }

    /**
     * 
     * @param request
     * @param response
     * @param jobInstanceId
     * @param jobExecNum
     * @param stepName
     * @throws IOException
     */
    public void getStepExecutionDataByJobExecNumAndStepName(RESTRequest request,
                                                            RESTResponse response, long jobInstanceId, short jobExecNum, String stepName) throws IOException {

        List<WSStepThreadExecutionAggregate> stepExecAggregateList = new ArrayList<WSStepThreadExecutionAggregate>();

        try {

            stepExecAggregateList.add(jobRepository.getStepExecutionAggregateFromJobExecutionNumberAndStepName(jobInstanceId, jobExecNum, stepName));

            // Note: headers must be set *before* writing to the output stream
            response.setContentType(BatchJSONHelper.MEDIA_TYPE_APPLICATION_JSON);

            BatchJSONHelper.writeStepExecutionsList(stepExecAggregateList,
                                                    BatchRequestUtil.getUrlRoot(request),
                                                    response.getOutputStream());

        } catch (NoSuchJobExecutionException e) {
            response.sendError(HttpURLConnection.HTTP_BAD_REQUEST, "Job Instance Id " + jobInstanceId + " with Execution Sequence Number " + jobExecNum + " and Step Name " + stepName + " not found.");
        } catch (IllegalArgumentException e) {
            response.sendError(HttpURLConnection.HTTP_BAD_REQUEST, "Job Instance Id " + jobInstanceId + " with Execution Sequence Number " + jobExecNum + " and Step Name " + stepName + " not found.");
        }

    }

    /**
     * @return the step id (aka step name) as a String
     * 
     * @throws IOException
     */
    protected String getStepName(RESTRequest request) throws RequestException {
        String s = request.getPathVariable("stepname");
        if (s == null || s.trim().length() == 0) {
            throw new RequestException(HttpURLConnection.HTTP_BAD_REQUEST,
                            "The step id is null or empty");
        }

        return s;
    }

    /**
     * @return the step execution id as a long value
     * 
     * @throws IOException
     */
    protected long getStepExecutionId(RESTRequest request)
                    throws RequestException {
        String s = request.getPathVariable("stepexecutionid");
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException nfe) {
            throw new RequestException(HttpURLConnection.HTTP_BAD_REQUEST,
                            "The step execution id (" + s + ") must be a Long integer",
                            nfe);
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

    /**
     * 
     * @param request
     * @return the job instance id as a long
     * @throws RequestException
     */
    protected long getJobInstanceId(RESTRequest request)
                    throws RequestException {
        String s = request.getPathVariable("jobinstanceid");
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException nfe) {
            throw new RequestException(HttpURLConnection.HTTP_BAD_REQUEST,
                            "The job instance id (" + s + ") must be a Long integer",
                            nfe);
        }
    }
}
