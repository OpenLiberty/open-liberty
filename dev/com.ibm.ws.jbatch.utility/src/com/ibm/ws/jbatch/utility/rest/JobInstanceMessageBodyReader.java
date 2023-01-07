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

import javax.batch.runtime.JobInstance;
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
        JsonReader jsonReader = readerFactory.createReader(entityStream);
        JsonObject jsonObject = jsonReader.readObject();
        jsonReader.close();

        return new JobInstanceModel( jsonObject );
    }


}
