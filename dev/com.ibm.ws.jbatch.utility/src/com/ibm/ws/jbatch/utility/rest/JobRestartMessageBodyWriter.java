/*******************************************************************************
 * Copyright (c) 2019,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
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

public class JobRestartMessageBodyWriter implements EntityWriter {

    private JobRestart jobRestart;
    
    /**
     * CTOR.  Used by SimpleHttpClient clients.
     */
    public JobRestartMessageBodyWriter(JobRestart jobRestart) {
        this.jobRestart = jobRestart;
    }
    
    /**
     * TODO: common with JobSubmissionMessageBodyWriter (and with Kaushik's server-side REST code)
     * 
     * @return a JsonObject for the given map
     */
    protected JsonObject buildJsonObjectFromMap( Map map ) {
        JsonObjectBuilder builder = builderFactory.createObjectBuilder();
        
        for (Map.Entry entry : (Set<Map.Entry>) map.entrySet() ) {
            builder.add( (String) entry.getKey(), (String) entry.getValue() );
        }
        
        return builder.build();
    }

    @Override
    public void writeEntity(OutputStream entityStream) {
    
        JsonObjectBuilder builder = builderFactory.createObjectBuilder();
        
        builder.add( "jobParameters", buildJsonObjectFromMap( jobRestart.getJobParameters() ) );
        
        writerFactory.createWriter(entityStream).writeObject( builder.build() );
    }
    

}
