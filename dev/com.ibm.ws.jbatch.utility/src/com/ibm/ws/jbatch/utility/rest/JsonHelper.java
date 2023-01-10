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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.batch.runtime.BatchStatus;
import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import com.ibm.ws.jbatch.utility.utils.StringUtils;

/**
 * Helper methods for converting BatchStatus to JSON.
 * 
 * Note: this class is duplicated in com.ibm.ws.jbatch.rest
 */
public class JsonHelper {
    
    private static final JsonBuilderFactory builderFactory = Json.createBuilderFactory(null);

    /**
     * @return batchStatus.name(), or null if batchStatus == null.
     */
    public static String getName(BatchStatus batchStatus) {
        return (batchStatus != null) ? batchStatus.name() : null;
    }
    
    /**
     * @return the BatchStatus with the given null, or null if batchStatusName == null.
     */
    public static BatchStatus valueOfBatchStatus(String batchStatusName) {
        return ( !StringUtils.isEmpty(batchStatusName) ) ? BatchStatus.valueOf(batchStatusName) : null;
    }
    
    /**
     * @return a copy of the given jsonObject with the given fields removed.
     * 
     */
    public static JsonObject removeFields(JsonObject jsonObject, String... removeField) {
        JsonObjectBuilder retMe = builderFactory.createObjectBuilder();
        
        List<String> removeFieldList = Arrays.asList(removeField);
        
        for (Map.Entry<String, JsonValue> entry : jsonObject.entrySet()) {
            if ( !removeFieldList.contains( entry.getKey() ) ) {
                retMe.add( entry.getKey(), entry.getValue() );
            }
        }
        
        return retMe.build();
    }
}