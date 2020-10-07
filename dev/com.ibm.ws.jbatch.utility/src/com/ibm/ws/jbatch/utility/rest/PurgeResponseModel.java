/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jbatch.utility.rest;

import javax.json.JsonObject;

/**
 * Simple WSPurgeResponse impl wrapped around a JsonObject.
 */
public class PurgeResponseModel implements WSPurgeResponse {
    
    /**
     * Deserialized json.
     */
    private JsonObject jsonObject; 

    /**
     * CTOR.
     */
    public PurgeResponseModel(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
    }
    
    @Override
    public long getInstanceId() {
        return jsonObject.getJsonNumber("instanceId").longValue();
    }
    
    @Override
    public String getMessage() {
        return jsonObject.getString("message");
    }
    
    @Override
    public String getPurgeStatus() {
       return jsonObject.getString("purgeStatus");
    }

    @Override
    public String getRedirectUrl() {
        return jsonObject.getString("redirectUrl", "");
    }

}
