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

import java.util.ArrayList;
import java.util.List;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobInstance;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;


/**
 * Simple JobInstance impl wrapped around a JsonObject.
 */
public class JobInstanceModel implements JobInstance {
    
    /**
     * Deserialized json.
     */
    private JsonObject jsonObject; 

    /**
     * CTOR.
     */
    public JobInstanceModel(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
    }


    @Override
    public long getInstanceId() {
        return jsonObject.getJsonNumber("instanceId").longValue();
    }

    @Override
    public String getJobName() {
        return jsonObject.getString("jobName", "");
    }
    
    public String getAppName() {
        return jsonObject.getString("appName", "");
    }
    
    public String getSubmitter() {
        return jsonObject.getString("submitter", "");
    }
    
    /**
     * @return the job's batchStatus.  May be null.
     */
    public BatchStatus getBatchStatus() {
        return JsonHelper.valueOfBatchStatus( jsonObject.getString("batchStatus", null) );
    }

    /**
     * @return list of HAL links for the jobinstance
     */
    public JsonArray getLinks() {
        return jsonObject.getJsonArray("_links");
    }
    
    /**
     * @return list of job execution links.
     */
    public List<String> getJobExecutionLinks() {
        
        List<String> retMe = new ArrayList<String>();
        
        for (JsonValue jsonValue : getLinks() ) {
            JsonObject jsonObject = (JsonObject) jsonValue;
            if ( jsonObject.getString("rel").equals("job execution") ) {
                retMe.add( jsonObject.getString("href") );
            }
        }
        
        return retMe;
    }
       
    /**
     * @return Stringified JobInstance record in the form "instanceId=<id>,jobName=<name>,..."
     */
    public String toString() {
        return JsonHelper.removeFields(jsonObject, "_links").toString();
    }

}
