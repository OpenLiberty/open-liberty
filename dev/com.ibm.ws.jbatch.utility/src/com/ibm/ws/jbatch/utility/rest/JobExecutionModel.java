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

import java.util.Date;
import java.util.Map;
import java.util.Properties;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

/**
 * Simple JobExecution impl wrapped around a JsonObject.
 */
public class JobExecutionModel implements JobExecution {
    
    /**
     * Deserialized json.
     */
    private JsonObject jsonObject; 

    /**
     * CTOR.
     */
    public JobExecutionModel(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    @Override
    public String getJobName() {
        return jsonObject.getString("jobName", "");
    }


    @Override
    public long getExecutionId() {
        if (jsonObject.getJsonNumber("executionId") == null)
            return -1;
        else
            return jsonObject.getJsonNumber("executionId").longValue();
    }


    @Override
    public BatchStatus getBatchStatus() {
        return BatchStatus.valueOf( jsonObject.getString("batchStatus") );
    }

    @Override
    public Date getStartTime() {
        return BatchDateFormat.parseDate( jsonObject.getString("startTime", null) );
    }


    @Override
    public Date getEndTime() {
        return BatchDateFormat.parseDate( jsonObject.getString("endTime", null) );
    }


    @Override
    public String getExitStatus() {
        return jsonObject.getString("exitStatus", "");
    }


    @Override
    public Date getCreateTime() {
        return BatchDateFormat.parseDate( jsonObject.getString("createTime", null) );
    }


    @Override
    public Date getLastUpdatedTime() {
        return BatchDateFormat.parseDate( jsonObject.getString("lastUpdatedTime", null) );
    }


    @Override
    public Properties getJobParameters() {
        return parseProperties( jsonObject.getJsonObject( "jobParameters") );
    }
    
    /**
     * @return a Properties object from the given JsonObject.
     */
    protected Properties parseProperties( JsonObject props ) {
        if (props == null) {
            return null;
        }
        
        Properties retMe = new Properties();
        
        for (Map.Entry<String, JsonValue> entry : props.entrySet() ) {
            retMe.setProperty( entry.getKey(), ((JsonString)entry.getValue()).getString() );
        }
        
        return retMe;
    }
    
    /**
     * @return Stringified JobExecution record in the form "executionId=<id>,jobName=<name>,..."
     */
    public String toString() {
        return JsonHelper.removeFields(jsonObject, "_links").toString();
    }

}
