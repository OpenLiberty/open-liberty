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
package com.ibm.ws.jbatch.rest.utils;

import java.util.Properties;

import javax.json.JsonObject;

/**
 * Helper class for serializing the POST payload for submitting a 
 * new batch job to the REST api.
 */
public class JobSubmissionModel {
    
    private JsonObject jsonObject;
    
    public JobSubmissionModel(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    public String getApplicationName() {
        return jsonObject.getString("applicationName", null);
    }
    
    public String getJobXMLName() {
        return jsonObject.getString("jobXMLName", null);
    }
    
    public Properties getJobParameters() {
        return BatchJSONHelper.convertJsonObjectToProperties( jsonObject.getJsonObject("jobParameters") );
    }

    public String getModuleName() {
       return jsonObject.getString("moduleName", null);
    }

    public String getComponentName() {
        return jsonObject.getString("componentName", null);
    }
    
    public String getJobXML() {
    	return jsonObject.getString("jobXML", null);
    }
}