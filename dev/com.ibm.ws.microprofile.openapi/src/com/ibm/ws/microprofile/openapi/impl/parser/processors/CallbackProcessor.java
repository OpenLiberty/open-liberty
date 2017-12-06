/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi.impl.parser.processors;

import static com.ibm.ws.microprofile.openapi.impl.parser.util.RefUtils.computeRefFormat;
import static com.ibm.ws.microprofile.openapi.impl.parser.util.RefUtils.isAnExternalRefFormat;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;

import com.ibm.ws.microprofile.openapi.impl.parser.ResolverCache;
import com.ibm.ws.microprofile.openapi.impl.parser.models.RefFormat;

/**
 * Created by gracekarina on 23/06/17.
 */
public class CallbackProcessor {
    private final ResolverCache cache;
    private final OperationProcessor operationProcessor;
    private final ParameterProcessor parameterProcessor;
    private final OpenAPI openAPI;
    private final ExternalRefProcessor externalRefProcessor;

    public CallbackProcessor(ResolverCache cache, OpenAPI openAPI) {
        this.cache = cache;
        this.operationProcessor = new OperationProcessor(cache, openAPI);
        this.parameterProcessor = new ParameterProcessor(cache, openAPI);
        this.externalRefProcessor = new ExternalRefProcessor(cache, openAPI);
        this.openAPI = openAPI;
    }

    public void processCallback(Callback callback) {
        if (callback.get("$ref") != null) {
            processReferenceCallback(callback);
        }
        //Resolver PathItem
        for (String name : callback.keySet()) {
            PathItem pathItem = callback.get(name);
            final Map<PathItem.HttpMethod, Operation> operationMap = pathItem.readOperationsMap();

            for (PathItem.HttpMethod httpMethod : operationMap.keySet()) {
                Operation operation = operationMap.get(httpMethod);
                operationProcessor.processOperation(operation);
            }

            List<Parameter> parameters = pathItem.getParameters();
            if (parameters != null) {
                for (Parameter parameter : parameters) {
                    parameterProcessor.processParameter(parameter);
                }
            }
        }
    }

    public void processReferenceCallback(Callback callback) {
        String $ref = callback.get("$ref").getRef();
        RefFormat refFormat = computeRefFormat($ref);
        if (isAnExternalRefFormat(refFormat)) {
            final String newRef = externalRefProcessor.processRefToExternalCallback($ref, refFormat);
            if (newRef != null) {
                callback.get("$ref").setRef("#/components/callbacks/" + newRef);
            }
        }
    }
}
