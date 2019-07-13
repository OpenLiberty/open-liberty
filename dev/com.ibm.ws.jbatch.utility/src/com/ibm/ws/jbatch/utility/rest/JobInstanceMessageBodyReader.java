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

import java.io.InputStream;

import javax.batch.runtime.JobInstance;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.ibm.ws.jbatch.utility.http.EntityReader;

/**
 * For deserializing a JobInstance from JSON.
 */
public class JobInstanceMessageBodyReader implements EntityReader<JobInstance> {
    
    @Override
    public JobInstance readEntity(InputStream entityStream) {
        // Read json
        JsonReader jsonReader = Json.createReader(entityStream);
        JsonObject jsonObject = jsonReader.readObject();
        jsonReader.close();

        return new JobInstanceModel( jsonObject );
    }


}
