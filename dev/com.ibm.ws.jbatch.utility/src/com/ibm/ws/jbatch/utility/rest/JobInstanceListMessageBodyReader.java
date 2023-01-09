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
import java.util.ArrayList;
import java.util.List;

import javax.batch.runtime.JobInstance;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import com.ibm.ws.jbatch.utility.http.EntityReader;

/**
 * For reading a list of JobInstances.
 */
public class JobInstanceListMessageBodyReader implements EntityReader<List<JobInstance>> {

    @Override
    public List<JobInstance> readEntity(InputStream entityStream) {

        JsonReader jsonReader = readerFactory.createReader(entityStream);
        JsonArray jsonArray = jsonReader.readArray();
        jsonReader.close();
        
        List<JobInstance> retMe = new ArrayList<JobInstance>();
        
        for (JsonValue jsonValue : jsonArray) {
            retMe.add( new JobInstanceModel( (JsonObject) jsonValue ) );
        }
        
        return retMe;
    }
}
