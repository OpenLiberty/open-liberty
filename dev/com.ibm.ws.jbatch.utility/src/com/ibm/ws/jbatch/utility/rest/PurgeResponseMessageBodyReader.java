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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import com.ibm.ws.jbatch.utility.http.EntityReader;

/**
 * For deserializing a WSPurgeResponse from JSON.
 */
public class PurgeResponseMessageBodyReader implements EntityReader<List<WSPurgeResponse>> {

    @Override
    public List<WSPurgeResponse> readEntity(InputStream entityStream) {

        JsonReader jsonReader = Json.createReader(entityStream);
        JsonArray jsonArray = jsonReader.readArray();
        jsonReader.close();
        
        List<WSPurgeResponse> retMe = new ArrayList<WSPurgeResponse>();
        
        for (JsonValue jsonValue : jsonArray) {
            retMe.add( new PurgeResponseModel( (JsonObject) jsonValue ) );
        }
        
        return retMe;
    }
}
