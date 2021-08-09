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
public class JobRestartModel {
    
    private JsonObject jsonObject;
    
    public JobRestartModel(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
    }
    
    public Properties getJobParameters() {
        return BatchJSONHelper.convertJsonObjectToProperties( jsonObject.getJsonObject("jobParameters") );
    }
}