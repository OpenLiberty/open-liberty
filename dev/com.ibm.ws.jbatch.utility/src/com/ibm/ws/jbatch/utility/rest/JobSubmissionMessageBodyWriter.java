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

import java.io.OutputStream;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.ibm.ws.jbatch.utility.http.EntityWriter;
import com.ibm.ws.jbatch.utility.utils.ObjectUtils;

public class JobSubmissionMessageBodyWriter implements EntityWriter {

    /**
     * The jobsubmission object to write (used by SimpleHttpClient).
     */
    private JobSubmission jobSubmission;
    
    /**
     * CTOR. Used by SimpleHttpClient.
     */
    public JobSubmissionMessageBodyWriter(JobSubmission jobSubmission) {
        this.jobSubmission = jobSubmission;
    }

    /**
     * @return a JsonObject for the given map
     */
    protected JsonObject buildJsonObjectFromMap( Map map ) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        
        for (Map.Entry entry : (Set<Map.Entry>) map.entrySet() ) {
            builder.add( (String) entry.getKey(), (String) entry.getValue() );
        }
        
        return builder.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeEntity(OutputStream entityStream) {
        
        JsonObjectBuilder builder = Json.createObjectBuilder();
        
        builder.add( "applicationName", ObjectUtils.firstNonNull(jobSubmission.getApplicationName(), "") )
               .add( "moduleName", ObjectUtils.firstNonNull(jobSubmission.getModuleName(), "") )
               .add( "componentName", ObjectUtils.firstNonNull(jobSubmission.getComponentName(), "") )
               .add( "jobXMLName", ObjectUtils.firstNonNull(jobSubmission.getJobXMLName(), "" ) )
               .add( "jobParameters", buildJsonObjectFromMap( jobSubmission.getJobParameters() )) 
               .add( "jobXML", ObjectUtils.firstNonNull(jobSubmission.getJobXMLFile(), "") );
   
        Json.createWriter(entityStream).writeObject( builder.build() );
    }
    

}
