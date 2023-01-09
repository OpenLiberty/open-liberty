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

import java.io.InputStream;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import com.ibm.ws.jbatch.utility.http.EntityReader;

/**
 * For deserializing a JobExecution[] from JSON.
 */
public class JobExecutionListMessageBodyReader implements EntityReader<JobExecutionList> {
    

    @Override
    public JobExecutionList readEntity(InputStream entityStream) {
        // Read json
        JsonReader jsonReader = readerFactory.createReader(entityStream);
        JsonArray jsonArray = jsonReader.readArray();
        jsonReader.close();
        
        JobExecutionList retMe = new JobExecutionList();
        
        for (JsonValue jsonValue : jsonArray) {
            retMe.add( new JobExecutionModel( (JsonObject) jsonValue ) );
        }
        
        return retMe;
    }


}
