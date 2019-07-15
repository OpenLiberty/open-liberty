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
package com.ibm.ws.jbatch.utility.rest;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;

import com.ibm.ws.jbatch.utility.http.Base64Coder;
import com.ibm.ws.jbatch.utility.http.Response;
import com.ibm.ws.jbatch.utility.http.SimpleHttpClient;
import com.ibm.ws.jbatch.utility.utils.ResourceBundleUtils;
import com.ibm.ws.jbatch.utility.utils.StringUtils;
import com.ibm.ws.jbatch.utility.utils.TaskIO;


/**
 * A convenience class for invoking requests against the batch rest api.
 */
public class BatchRestClient {
    
    /**
     * The list of batch manager targets that can be used by this client.
     * The list is used for failover scenarios.
     * 
     * Use a LinkedHashSet for Set aspect (no dups) and predictable ordering.
     */
    private Set<String> targets = new LinkedHashSet<String>();

    /**
     * Iterator thru the set of targets.
     */
    private Iterator<String> targetIterator;
    
    /**
     * The current batch manager target being used by the client.
     */
    private String currentTarget;
    
    /**
     * User/pass for authorization.
     */
    private String user;
    private String pass;
    
    /**
     * HTTP timeout for connect and read
     */
    private int httpTimeout_ms = 0;
    
    /**
     * Boolean value representing if the user wants to reuse the previous parameters
     */
    private boolean reusePreviousParams = false;
    
    /**
     * TaskIO for issuing messages.
     */
    private TaskIO taskIO;
    
    /**
     * CTOR.
     * 
     * @param targets - non-empty list of batchManager REST api targets (host:port).
     */
    public BatchRestClient(List<String> targets) {
        if (targets.isEmpty()) {
            throw new IllegalArgumentException("Target list cannot be empty: " + targets);
        }
        
        this.targets.addAll(targets);
        this.targetIterator = this.targets.iterator();
        
        setCurrentTarget( this.targetIterator.next() );
    }
    
    /**
     * @param taskIO for issuing messages
     * 
     * @return this
     */
    public BatchRestClient setTaskIO(TaskIO taskIO) {
        this.taskIO = taskIO;
        return this;
    }
    
    /**
     * @return taskIO for issuing messages
     */
    protected TaskIO getTaskIO() {
        return taskIO;
    }
    
    /**
     * @return targetIterator
     */
    protected Iterator<String> getTargetIterator() {
        return targetIterator;
    }

    /**
     * @return the current target (host:port) to be used for the REST request.
     */
    protected String getCurrentTarget() {
        return currentTarget;
    }
    
    /**
     * @param target the current batch manager target to be used by this client 
     *               for REST requests.
     * 
     * @return this
     */
    protected BatchRestClient setCurrentTarget(String target) {
        this.currentTarget = target;
        return this;
    }

    /**
     * @return "https://" + getCurrentTarget()
     */
    protected String getCurrentTargetUrl() {
        return "https://" + getCurrentTarget();
    }
    
    /**
     * @return the batch REST API context path.
     */
    protected String getBatchRestContext() {
        return "ibm/api/batch";
    }
    
    /**
     * @return the batch REST V2 API context path.
     */
    protected String getBatchRestContext_V2() {
        return "ibm/api/batch/v2";
    }
    
    /**
     * Add an authorization header to the request.
     */
    public BatchRestClient setAuthorization(String user, String pass) {
        this.user = user;
        this.pass = pass;
        return this;
    }
    
    /**
     * @return the password. If null then the user is prompted for one.
     */
    protected String getPassword() {
        if ( pass == null ) {
            pass = StringUtils.firstNonNull( getTaskIO().promptForMaskedInput( "Password: " ), "");
        }
        return pass;
    }
    
    /**
     * @return the "Authorization" header value.
     */
    protected String getAuthorizationHeader() {
        return (StringUtils.isEmpty(user)) 
                    ? null
                    : "Basic " + Base64Coder.base64Encode(user + ":" + getPassword());
    }
    
    /**
     * @param httpTimeout_ms the http timeout for connect and read, in ms.
     * 
     * @return this
     */
    public BatchRestClient setHttpTimeout(int httpTimeout_ms) {
        this.httpTimeout_ms = httpTimeout_ms;
        return this;
    }
    
    /**
     * @return the http timeout for connect and read, in ms.
     */
    protected int getHttpTimeout() {
        return httpTimeout_ms;
    }
    
    /**
     * @return this
     */
    public BatchRestClient setReusePreviousParams(boolean reusePreviousParams){
        this.reusePreviousParams = reusePreviousParams;
        return this;
    }
    
    /**
     * @return the reusePreviousParams setting
     */
    protected boolean getReusePreviousParams(){
        return reusePreviousParams;
    }
    
    /**
     *
     * @return the executionId of the newly started job.
     */
    public JobInstance start(final String applicationName, 
                             final String moduleName,
                             final String componentName,
                             final String jobXMLName, 
                             final Properties jobParameters,
                             final String jobXMLFile) throws IOException {
        return handleFailover( new Callable<Response>() {
            public Response call() throws IOException {
                return new SimpleHttpClient().setTarget( getCurrentTargetUrl() )
                                     .path( getBatchRestContext() )
                                     .path( "jobinstances" )
                                     .header( "Accept", "application/json" )
                                     .header( "Content-Type", "application/json; charset=utf-8" )
                                     .header( "Authorization", getAuthorizationHeader())
                                     .setTimeout( getHttpTimeout() )
                                     .post( new JobSubmissionMessageBodyWriter( new JobSubmission(applicationName, 
                                                                                                  moduleName, 
                                                                                                  componentName,
                                                                                                  jobXMLName, 
                                                                                                  jobParameters,
                                                                                                  jobXMLFile) ) );
            }
        }).readEntity( new JobInstanceMessageBodyReader() );
    }
    
    /**
     *@return the list of JobExecutions for the given jobInstance.
     */
    public List<JobExecution> getJobExecutions(JobInstance jobInstance) throws IOException {
        return getJobExecutions( jobInstance.getInstanceId() );
    }
    
    /**
     *@return the list of JobExecutions for the given jobInstance.
     */
    public List<JobExecution> getJobExecutions(final long jobInstanceId) throws IOException {

        return handleFailover( new Callable<Response>() {
            public Response call() throws IOException {
                return new SimpleHttpClient().setTarget( getCurrentTargetUrl() )
                                     .path( getBatchRestContext() )
                                     .path( "jobinstances" )
                                     .path( String.valueOf(jobInstanceId) )
                                     .path("jobexecutions")
                                     .header( "Accept", "application/json" )
                                     .header( "Authorization", getAuthorizationHeader())
                                     .setTimeout( getHttpTimeout() )
                                     .get();
            }
        }).readEntity( new JobExecutionListMessageBodyReader() );
    }
    
    /**
     * @return the JobExecution object for the given ID.
     */
    public JobExecution getJobExecution(final long jobExecutionId) throws IOException {

        return handleFailover( new Callable<Response>() {
            public Response call() throws IOException {
                return new SimpleHttpClient().setTarget( getCurrentTargetUrl() )
                                     .path( getBatchRestContext() )
                                     .path("jobexecutions")
                                     .path( String.valueOf(jobExecutionId) )
                                     .header( "Accept", "application/json" )
                                     .header( "Authorization", getAuthorizationHeader())
                                     .setTimeout( getHttpTimeout() )
                                     .get();
            }
        }).readEntity( new JobExecutionMessageBodyReader() );
    }

    /**
     * @return the JobInstance of the restarted job.
     */
    public JobInstance restartJobExecution(final long jobExecutionId, final Properties jobParameters) throws IOException {
        
        return handleFailover( new Callable<Response>() {
            public Response call() throws IOException {
                return new SimpleHttpClient().setTarget( getCurrentTargetUrl() )
                                     .path( getBatchRestContext() )
                                     .path("jobexecutions")
                                     .path( String.valueOf(jobExecutionId) )
                                     .queryParam("action", "restart" )
                                     .queryParam("reusePreviousParams", String.valueOf(getReusePreviousParams()) )
                                     .header( "Accept", "application/json" )
                                     .header( "Content-Type", "application/json; charset=utf-8" )
                                     .header( "Authorization", getAuthorizationHeader())
                                     .setTimeout( getHttpTimeout() )
                                     .put( new JobRestartMessageBodyWriter( new JobRestart(jobParameters) ) );
            }
        }).readEntity( new JobInstanceMessageBodyReader() );
    }
    
    /**
     * @return the JobInstance of the restarted job.
     */
    public JobInstance restartJobInstance(final long jobInstanceId, final Properties jobParameters) throws IOException {
        
        return handleFailover( new Callable<Response>() {
            public Response call() throws IOException {
                return new SimpleHttpClient().setTarget( getCurrentTargetUrl() )
                                     .path( getBatchRestContext() )
                                     .path("jobinstances")
                                     .path( String.valueOf(jobInstanceId) )
                                     .queryParam("action", "restart")
                                     .queryParam("reusePreviousParams", String.valueOf(getReusePreviousParams()) )
                                     .header( "Accept", "application/json" )
                                     .header( "Content-Type", "application/json; charset=utf-8" )
                                     .header( "Authorization", getAuthorizationHeader())
                                     .setTimeout( getHttpTimeout() )
                                     .put( new JobRestartMessageBodyWriter( new JobRestart(jobParameters) ) );
            }
        }).readEntity( new JobInstanceMessageBodyReader() );
    }

    /**
     * @return the JobInstance associated with the given jobInstanceId.
     */
    public JobInstance getJobInstance(final long jobInstanceId) throws IOException {

        return handleFailover( new Callable<Response>() {
            public Response call() throws IOException {
                return new SimpleHttpClient().setTarget( getCurrentTargetUrl() )
                                     .path( getBatchRestContext() )
                                     .path("jobinstances")
                                     .path( String.valueOf(jobInstanceId) )
                                     .header( "Accept", "application/json" )
                                     .header( "Authorization", getAuthorizationHeader())
                                     .setTimeout( getHttpTimeout() )
                                     .get();
            }
        }).readEntity( new JobInstanceMessageBodyReader() );
    }
    
    /**
     * @return the JobInstance associated with the given jobExecutionId.
     */
    public JobInstance getJobInstanceForJobExecution(final long jobExecutionId) throws IOException {

        return handleFailover( new Callable<Response>() {
            public Response call() throws IOException {
                return new SimpleHttpClient().setTarget( getCurrentTargetUrl() )
                                     .path( getBatchRestContext() )
                                     .path("jobexecutions")
                                     .path( String.valueOf(jobExecutionId) )
                                     .path( "jobinstance" )
                                     .header( "Accept", "application/json" )
                                     .header( "Authorization", getAuthorizationHeader())
                                     .setTimeout( getHttpTimeout() )
                                     .get();
            }
        }).readEntity( new JobInstanceMessageBodyReader() );
    }
    
    /**
     * @param page The page of rows to return (starts at 0)
     * @param pageSize The number of rows per page
     * 
     * @return the list of all JobInstances
     */
    public List<JobInstance> getJobInstances(final int page, final int pageSize) throws IOException {

        return handleFailover( new Callable<Response>() {
            public Response call() throws IOException {
                return new SimpleHttpClient().setTarget( getCurrentTargetUrl() )
                                     .path( getBatchRestContext() )
                                     .path("jobinstances")
                                     .queryParam("page", String.valueOf(page))
                                     .queryParam("pageSize", String.valueOf(pageSize))
                                     .header( "Accept", "application/json" )
                                     .header( "Authorization", getAuthorizationHeader())
                                     .setTimeout( getHttpTimeout() )
                                     .get();
            }
        }).readEntity( new JobInstanceListMessageBodyReader() );
    }
    
    /**
     * @param page The page of rows to return (starts at 0)
     * @param pageSize The number of rows per page
     * 
     * @return the list of all JobInstances
     */
    public List<JobInstance> getJobInstances(final int page, final int pageSize, final String instanceId, 
            final String createTime, final String instanceState, final String exitStatus) throws IOException {
        
        return handleFailover( new Callable<Response>() {
            public Response call() throws IOException {
                return new SimpleHttpClient().setTarget( getCurrentTargetUrl() )
                                     .path( getBatchRestContext_V2() )
                                     .path("jobinstances")
                                     .queryParamNotNullOrEmpty("jobInstanceId", instanceId)
                                     .queryParamNotNullOrEmpty("createTime", createTime)
                                     .queryParamNotNullOrEmpty("instanceState", instanceState)
                                     .queryParamNotNullOrEmpty("exitStatus", exitStatus)
                                     .queryParam("page", String.valueOf(page))
                                     .queryParam("pageSize", String.valueOf(pageSize))
                                     .header( "Accept", "application/json" )
                                     .header( "Authorization", getAuthorizationHeader())
                                     .setTimeout( getHttpTimeout() )
                                     .get();
            }
        }).readEntity( new JobInstanceListMessageBodyReader() );
    }

    /**
     * @return the JobExecution record that was stopped.
     */
    public JobExecution stop(final long jobExecutionId) throws IOException {

        return handleFailover( new Callable<Response>() {
            public Response call() throws IOException {
                return new SimpleHttpClient().setTarget(  getCurrentTargetUrl() )
                                     .path( getBatchRestContext() )
                                     .path("jobexecutions")
                                     .path( String.valueOf(jobExecutionId) )
                                     .queryParam("action","stop")
                                     .queryParam("permitRedirect", "true")
                                     .header( "Accept", "application/json" )
                                     .header( "Authorization", getAuthorizationHeader())
                                     .setTimeout( getHttpTimeout() )
                                     .put( null );
            }
        }).readEntity( new JobExecutionMessageBodyReader() );
    }

    /**
     * @return the JobExecution record that was stopped.
     */
    public JobExecution stop(final JobInstance jobInstance) throws IOException {

        return handleFailover( new Callable<Response>() {
            public Response call() throws IOException {
                return new SimpleHttpClient().setTarget(  getCurrentTargetUrl() )
                                     .path( getBatchRestContext() )
                                     .path("jobinstances")
                                     .path( String.valueOf(jobInstance.getInstanceId()) )
                                     .queryParam("action","stop")
                                     .queryParam("permitRedirect", "true")
                                     .header( "Accept", "application/json" )
                                     .header( "Authorization", getAuthorizationHeader())
                                     .setTimeout( getHttpTimeout() )
                                     .put( null );
            }
        }).readEntity( new JobExecutionMessageBodyReader() );
    }

    /**
     * @return the REST API link for listing the joblog links for the given job instance.
     *         i.e "[batchRootContext]/jobinstances/[instanceId]/joblogs"
     */
    public String buildJobLogsRestLink(JobInstance jobInstance, String type) {
        return getCurrentTargetUrl() 
                    + "/" + getBatchRestContext() 
                    + "/jobinstances"
                    + "/" + jobInstance.getInstanceId() 
                    + "/joblogs"
                    + ( (StringUtils.isEmpty(type)) ? "" : "?type=" + type );
    }
    
    /**
     * @return a Response object for reading the http response
     */
    public Response getJobLogsForJobInstance(final long jobInstanceId, final String type) throws IOException {
        
        return handleFailover( new Callable<Response>() {
            public Response call() throws IOException {
                return new SimpleHttpClient().setTarget( getCurrentTargetUrl() )
                                     .path( getBatchRestContext() )
                                     .path("jobinstances")
                                     .path( String.valueOf(jobInstanceId) )
                                     .path("joblogs")
                                     .queryParam("type",type)
                                     .queryParam("permitRedirect", "true")
                                     .header( "Accept", "application/zip, text/plain" )
                                     .header( "Authorization", getAuthorizationHeader())
                                     .setTimeout( getHttpTimeout() )
                                     .get();
            }
        });
    }
    
    /**
     * @return a Response object for reading the http response
     */
    public Response getJobLogsForJobExecution(final long jobExecutionId, final String type) throws IOException {

        return handleFailover( new Callable<Response>() {
            public Response call() throws IOException {
                return new SimpleHttpClient().setTarget( getCurrentTargetUrl() )
                                     .path( getBatchRestContext() )
                                     .path("jobexecutions")
                                     .path( String.valueOf(jobExecutionId) )
                                     .path("joblogs")
                                     .queryParam("type",type)
                                     .queryParam("permitRedirect", "true")
                                     .header( "Accept", "application/zip, text/plain" )
                                     .header( "Authorization", getAuthorizationHeader())
                                     .setTimeout( getHttpTimeout() )
                                     .get();
            }
        });
    }
    
    /**
     * @param jobInstanceId The job instance to purge
     * 
     * @return the Response object
     */
    public Response purge(final long jobInstanceId, final boolean purgeJobStoreOnly) throws IOException {

        Response response =  handleFailover( new Callable<Response>() {
            public Response call() throws IOException {
                return new SimpleHttpClient().setTarget( getCurrentTargetUrl() )
                                     .path( getBatchRestContext() )
                                     .path("jobinstances")
                                     .path( String.valueOf(jobInstanceId) )
                                     .queryParam("purgeJobStoreOnly", String.valueOf(purgeJobStoreOnly))
                                     .queryParam("permitRedirect", "true")
                                     .header( "Authorization", getAuthorizationHeader())
                                     .setTimeout( getHttpTimeout() )
                                     .delete();
            }
        });
        
        // Ensure the request is sent.
        response.getInputStream();
        return response;
    }
    
    /**
     * Performs multi-purge via the REST API
     * @param purgeJobStoreOnly
     * @param page
     * @param pageSize
     * @param instanceId
     * @param createTime
     * @param instanceState
     * @param exitStatus
     * @return
     * @throws IOException
     */
    public List<WSPurgeResponse> purge(final boolean purgeJobStoreOnly, final int page, final int pageSize, final String instanceId, 
            final String createTime, final String instanceState, final String exitStatus) throws IOException {
        return handleFailover( new Callable<Response>() {
            public Response call() throws IOException {
                return new SimpleHttpClient().setTarget( getCurrentTargetUrl() )
                                     .path( getBatchRestContext_V2() )
                                     .path("jobinstances")
                                     .queryParamNotNullOrEmpty("jobInstanceId", instanceId)
                                     .queryParamNotNullOrEmpty("createTime", createTime)
                                     .queryParamNotNullOrEmpty("instanceState", instanceState)
                                     .queryParamNotNullOrEmpty("exitStatus", exitStatus)
                                     .queryParam("page", String.valueOf(page))
                                     .queryParam("pageSize", String.valueOf(pageSize))
                                     .queryParam("purgeJobStoreOnly", String.valueOf(purgeJobStoreOnly))
                                     .queryParam("permitRedirect", "true")
                                     .header( "Accept", "application/json" )
                                     .header( "Authorization", getAuthorizationHeader())
                                     .setTimeout( getHttpTimeout() )
                                     .delete();
            }
        }).readEntity( new PurgeResponseMessageBodyReader()); 
      

    }
    
    /**
     * Execute the operation encompassed in the given callable against the current target.
     * 
     * If it fails due to a retryable HTTP exception (e.g connection refused), then failover
     * to the next target in the list (if there's more than 1) and retry.
     * 
     * If all targets are exhausted, the exception is raised.
     * 
     * Note: the Callable should use getCurrentTarget() to obtain the current target.
     * 
     * @return the return value of the callable.
     * 
     */
    protected <T> T handleFailover( Callable<T> callMe ) throws IOException {
        
        do {
            try {
                return callMe.call();
                
            } catch (IOException ioe) {
                if ( getTargetIterator().hasNext() && isRetryable(ioe) ) {
                    
                    String prevTarget = getCurrentTarget();
                    
                    // Note: The callable should be using getCurrentTarget()
                    setCurrentTarget( getTargetIterator().next() );
                    
                    getTaskIO().info( ResourceBundleUtils.getMessage("failover.to.next.target", 
                                                                     prevTarget, 
                                                                     getCurrentTarget(),
                                                                     ioe.getClass().getName() + ":" + ioe.getMessage()) );
                } else {
                    throw ioe;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
           
        } while (true) ; // The loop exits when callMe.call() returns successfully or there's an exception
                         // and we've exhausted the target list.
    }
    
    /**
     * @return true if the given exception is retryable in a failover scenario.
     */
    protected boolean isRetryable(IOException e) {
        // TODO: not all exceptions are retryable. every 403/500 response will show
        //       up as an IOexception.  We only want to retry conn exceptions
        return true;
    }
    
}

