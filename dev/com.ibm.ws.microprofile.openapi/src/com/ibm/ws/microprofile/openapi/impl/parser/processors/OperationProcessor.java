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
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;

import com.ibm.ws.microprofile.openapi.impl.parser.ResolverCache;
import com.ibm.ws.microprofile.openapi.impl.parser.models.RefFormat;

public class OperationProcessor {
    private final ParameterProcessor parameterProcessor;
    private final RequestBodyProcessor requestBodyProcessor;
    private final ResponseProcessor responseProcessor;
    private final ExternalRefProcessor externalRefProcessor;
    private final ResolverCache cache;

    public OperationProcessor(ResolverCache cache, OpenAPI openAPI) {
        this.parameterProcessor = new ParameterProcessor(cache, openAPI);
        this.responseProcessor = new ResponseProcessor(cache, openAPI);
        this.requestBodyProcessor = new RequestBodyProcessor(cache, openAPI);
        this.externalRefProcessor = new ExternalRefProcessor(cache, openAPI);

        this.cache = cache;
    }

    public void processOperation(Operation operation) {
        final List<Parameter> processedOperationParameters = parameterProcessor.processParameters(operation.getParameters());
        if (processedOperationParameters != null) {
            operation.setParameters(processedOperationParameters);
        }
        final RequestBody requestBody = operation.getRequestBody();
        if (requestBody != null) {
            requestBodyProcessor.processRequestBody(requestBody);
        }

        final Map<String, APIResponse> responses = operation.getResponses();
        if (responses != null) {
            for (String responseCode : responses.keySet()) {
                APIResponse response = responses.get(responseCode);
                if (response != null) {
                    responseProcessor.processResponse(response);
                }
            }
        }

        final Map<String, Callback> callbacks = operation.getCallbacks();
        if (callbacks != null) {
            for (String name : callbacks.keySet()) {
                Callback callback = callbacks.get(name);
                if (callback != null) {
                    if (callback.get("$ref") != null) {
                        String $ref = callback.get("$ref").getRef();
                        RefFormat refFormat = computeRefFormat($ref);
                        if (isAnExternalRefFormat(refFormat)) {
                            final String newRef = externalRefProcessor.processRefToExternalCallback($ref, refFormat);
                            if (newRef != null) {
                                callback.get("$ref").setRef(newRef);
                            }
                        }
                    }
                    for (String callbackName : callback.keySet()) {
                        PathItem pathItem = callback.get(callbackName);
                        final Map<PathItem.HttpMethod, Operation> operationMap = pathItem.readOperationsMap();

                        for (PathItem.HttpMethod httpMethod : operationMap.keySet()) {
                            Operation op = operationMap.get(httpMethod);
                            processOperation(op);
                        }

                        List<Parameter> parameters = pathItem.getParameters();
                        if (parameters != null) {
                            for (Parameter parameter : parameters) {
                                parameterProcessor.processParameter(parameter);
                            }
                        }
                    }
                }
            }
        }
    }
}