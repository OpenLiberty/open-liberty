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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;

import com.ibm.ws.microprofile.openapi.impl.parser.ResolverCache;
import com.ibm.ws.microprofile.openapi.impl.parser.models.RefFormat;

public class ParameterProcessor {

    private final ResolverCache cache;
    private final SchemaProcessor schemaProcessor;
    private final ExampleProcessor exampleProcessor;
    private final OpenAPI openAPI;
    private final ExternalRefProcessor externalRefProcessor;

    public ParameterProcessor(ResolverCache cache, OpenAPI openAPI) {
        this.cache = cache;
        this.openAPI = openAPI;
        this.schemaProcessor = new SchemaProcessor(cache, openAPI);
        this.exampleProcessor = new ExampleProcessor(cache, openAPI);
        this.externalRefProcessor = new ExternalRefProcessor(cache, openAPI);
    }

    public void processParameter(Parameter parameter) {
        String $ref = parameter.getRef();
        if ($ref != null) {
            RefFormat refFormat = computeRefFormat(parameter.getRef());
            if (isAnExternalRefFormat(refFormat)) {
                final String newRef = externalRefProcessor.processRefToExternalParameter($ref, refFormat);
                if (newRef != null) {
                    parameter.setRef(newRef);
                }
            }
        }
        if (parameter.getSchema() != null) {
            schemaProcessor.processSchema(parameter.getSchema());
        }
        if (parameter.getExamples() != null) {
            Map<String, Example> examples = parameter.getExamples();
            for (String exampleName : examples.keySet()) {
                final Example example = examples.get(exampleName);
                exampleProcessor.processExample(example);
            }
        }
        Schema schema = null;
        if (parameter.getContent() != null) {
            Map<String, MediaType> content = parameter.getContent();
            for (String mediaName : content.keySet()) {
                MediaType mediaType = content.get(mediaName);
                if (mediaType.getSchema() != null) {
                    schema = mediaType.getSchema();
                    if (schema != null) {
                        schemaProcessor.processSchema(schema);
                    }
                }
            }
        }
    }

    public List<Parameter> processParameters(List<Parameter> parameters) {

        if (parameters == null) {
            return null;
        }

        final List<Parameter> processedPathLevelParameters = new ArrayList<>();
        final List<Parameter> refParameters = new ArrayList<>();

        for (Parameter parameter : parameters) {
            if (parameter.getRef() != null) {
                RefFormat refFormat = computeRefFormat(parameter.getRef());
                final Parameter resolvedParameter = cache.loadRef(parameter.getRef(), refFormat, Parameter.class);

                if (resolvedParameter == null) {
                    // can't resolve it!
                    processedPathLevelParameters.add(parameter);
                    continue;
                }
                // if the parameter exists, replace it
                boolean matched = false;
                for (Parameter param : processedPathLevelParameters) {
                    if (param.getName().equals(resolvedParameter.getName())) {
                        // ref param wins
                        matched = true;
                        break;
                    }
                }
                for (Parameter param : parameters) {
                    if (param.getName() != null) {
                        if (param.getName().equals(resolvedParameter.getName())) {
                            // ref param wins
                            matched = true;
                            break;
                        }
                    }
                }
                processedPathLevelParameters.add(resolvedParameter);
            } else {
                processedPathLevelParameters.add(parameter);
            }
        }

        for (Parameter resolvedParameter : refParameters) {
            int pos = 0;
            for (Parameter param : processedPathLevelParameters) {
                if (param.getName().equals(resolvedParameter.getName())) {
                    // ref param wins
                    processedPathLevelParameters.set(pos, resolvedParameter);
                    break;
                }
                pos++;
            }

        }

        return processedPathLevelParameters;
    }
}