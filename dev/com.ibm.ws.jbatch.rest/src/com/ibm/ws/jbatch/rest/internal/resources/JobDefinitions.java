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

import javax.batch.operations.JobSecurityException;
import javax.batch.runtime.BatchRuntime;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.jbatch.container.ws.WSJobRepository;
import com.ibm.ws.jbatch.rest.utils.BatchJSONHelper;
import com.ibm.ws.jbatch.rest.utils.StringUtils;
import com.ibm.wsspi.rest.handler.RESTHandler;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

public class JobDefinitions implements RESTHandler {

    private WSJobRepository jobRepository;

    /**
     * DS injection
     * 
     * @param ref The WSJobRepository to associate.
     *
     * Note: The dependency is required; however we mark it OPTIONAL to ensure that
     *       the REST handler is started even if the batch container didn't, so we
     *       can respond with useful error messages instead of 404s.
     */
    @Reference(cardinality=ReferenceCardinality.OPTIONAL,
               policy=ReferencePolicy.DYNAMIC,
               policyOption=ReferencePolicyOption.GREEDY)
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
                                                    .addHandler( new JobDefinitionsHandler() );
    
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
        } catch (Exception e) {
            response.sendError(HttpURLConnection.HTTP_INTERNAL_ERROR, e.getMessage());
        }
    }
    
    /**
     * Handles "/batch/jobdefinitions", which is used to submit jobs.
     */
    private class JobDefinitionsHandler extends RequestHandler {
        public JobDefinitionsHandler() {
            super("/batch/jobdefinitions");
        }
        
        public void get(RESTRequest request, RESTResponse response) throws Exception {

            if (request.isUserInRole("Administrator")){
                
                // Note: headers must be set *before* writing to the output stream
                response.setContentType(BatchJSONHelper.MEDIA_TYPE_APPLICATION_JSON);
                
                BatchJSONHelper.writeJobDefinitions( jobRepository.getJobNames(), response.getOutputStream() );
            } else {
                response.setStatus(HttpURLConnection.HTTP_UNAUTHORIZED);;
            }
        }
    }

}
