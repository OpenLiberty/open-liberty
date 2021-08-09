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

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;

import com.ibm.ws.microprofile.openapi.impl.parser.ResolverCache;

/**
 * Created by gracekarina on 13/06/17.
 */
public class ComponentsProcessor {
    private final ResolverCache cache;
    private final OpenAPI openApi;
    private final SchemaProcessor schemaProcessor;
    private final ResponseProcessor responseProcessor;
    private final RequestBodyProcessor requestBodyProcessor;
    private final ParameterProcessor parameterProcessor;
    private final HeaderProcessor headerProcessor;
    private final ExampleProcessor exampleProcessor;
    private final LinkProcessor linkProcessor;
    private final CallbackProcessor callbackProcessor;
    private final SecuritySchemeProcessor securitySchemeProcessor;

    public ComponentsProcessor(OpenAPI openApi, ResolverCache cache) {
        this.cache = cache;
        this.openApi = openApi;
        this.schemaProcessor = new SchemaProcessor(cache, openApi);
        this.responseProcessor = new ResponseProcessor(cache, openApi);
        this.requestBodyProcessor = new RequestBodyProcessor(cache, openApi);
        this.parameterProcessor = new ParameterProcessor(cache, openApi);
        this.headerProcessor = new HeaderProcessor(cache, openApi);
        this.exampleProcessor = new ExampleProcessor(cache, openApi);
        this.linkProcessor = new LinkProcessor(cache, openApi);
        this.callbackProcessor = new CallbackProcessor(cache, openApi);
        this.securitySchemeProcessor = new SecuritySchemeProcessor(cache, openApi);

    }

    public void processComponents() {
        if (openApi.getComponents() == null) {
            return;
        }
        final Map<String, Schema> schemas = openApi.getComponents().getSchemas();
        final Map<String, APIResponse> responses = openApi.getComponents().getResponses();
        final Map<String, RequestBody> requestBodies = openApi.getComponents().getRequestBodies();
        final Map<String, Parameter> parameters = openApi.getComponents().getParameters();
        final Map<String, Header> headers = openApi.getComponents().getHeaders();
        final Map<String, Example> examples = openApi.getComponents().getExamples();
        final Map<String, Link> links = openApi.getComponents().getLinks();
        final Map<String, Callback> callbacks = openApi.getComponents().getCallbacks();
        final Map<String, SecurityScheme> securitySchemes = openApi.getComponents().getSecuritySchemes();

        //schemas
        if (schemas != null) {
            Set<String> keySet = new LinkedHashSet<>();
            while (schemas.keySet().size() > keySet.size()) {
                processSchemas(keySet, schemas);
            }
        }

        //responses
        if (responses != null) {
            Set<String> keySet = new LinkedHashSet<>();
            while (responses.keySet().size() > keySet.size()) {
                processResponses(keySet, responses);
            }
        }

        //requestBodies
        if (requestBodies != null) {
            Set<String> keySet = new LinkedHashSet<>();
            while (requestBodies.keySet().size() > keySet.size()) {
                processRequestBodies(keySet, requestBodies);
            }
        }

        //parameters
        if (parameters != null) {
            Set<String> keySet = new LinkedHashSet<>();
            while (parameters.keySet().size() > keySet.size()) {
                processParameters(keySet, parameters);
            }
        }

        //headers
        if (headers != null) {
            Set<String> keySet = new LinkedHashSet<>();
            while (headers.keySet().size() > keySet.size()) {
                processHeaders(keySet, headers);
            }
        }

        //examples
        if (examples != null) {
            Set<String> keySet = new LinkedHashSet<>();
            while (examples.keySet().size() > keySet.size()) {
                processExamples(keySet, examples);
            }
        }

        //links
        if (links != null) {
            Set<String> keySet = new LinkedHashSet<>();
            while (links.keySet().size() > keySet.size()) {
                processLinks(keySet, links);
            }
        }

        //callbacks
        if (callbacks != null) {
            Set<String> keySet = new LinkedHashSet<>();
            while (callbacks.keySet().size() > keySet.size()) {
                processCallbacks(keySet, callbacks);
            }
        }

        //securitySchemes
        if (securitySchemes != null) {
            Set<String> keySet = new LinkedHashSet<>();
            while (securitySchemes.keySet().size() > keySet.size()) {
                processSecuritySchemes(keySet, securitySchemes);
            }
        }
    }

    private void processSecuritySchemes(Set<String> securitySchemeKey, Map<String, SecurityScheme> securitySchemes) {
        securitySchemeKey.addAll(securitySchemes.keySet());

        for (String securitySchemeName : securitySchemeKey) {
            final SecurityScheme securityScheme = securitySchemes.get(securitySchemeName);
            SecurityScheme resolvedSecurityScheme = securitySchemeProcessor.processSecurityScheme(securityScheme);
            securitySchemes.replace(securitySchemeName, securityScheme, resolvedSecurityScheme);
        }
    }

    private void processCallbacks(Set<String> callbackKey, Map<String, Callback> callbacks) {
        callbackKey.addAll(callbacks.keySet());

        for (String callbackName : callbackKey) {
            final Callback callback = callbacks.get(callbackName);
            callbackProcessor.processCallback(callback);
        }
    }

    private void processLinks(Set<String> linkKey, Map<String, Link> links) {
        linkKey.addAll(links.keySet());

        for (String linkName : linkKey) {
            final Link link = links.get(linkName);
            linkProcessor.processLink(link);
        }
    }

    private void processExamples(Set<String> exampleKey, Map<String, Example> examples) {
        exampleKey.addAll(examples.keySet());

        for (String exampleName : exampleKey) {
            final Example example = examples.get(exampleName);
            exampleProcessor.processExample(example);
        }
    }

    private void processHeaders(Set<String> HeaderKey, Map<String, Header> headers) {
        HeaderKey.addAll(headers.keySet());

        for (String headersName : HeaderKey) {
            final Header header = headers.get(headersName);
            headerProcessor.processHeader(header);
        }
    }

    private void processParameters(Set<String> ParametersKey, Map<String, Parameter> parameters) {
        ParametersKey.addAll(parameters.keySet());

        for (String parametersName : ParametersKey) {
            final Parameter parameter = parameters.get(parametersName);
            parameterProcessor.processParameter(parameter);
        }
    }

    private void processRequestBodies(Set<String> requestBodyKey, Map<String, RequestBody> requestBodies) {
        requestBodyKey.addAll(requestBodies.keySet());

        for (String requestBodyName : requestBodyKey) {
            final RequestBody requestBody = requestBodies.get(requestBodyName);
            requestBodyProcessor.processRequestBody(requestBody);
        }
    }

    private void processResponses(Set<String> responseKey, Map<String, APIResponse> responses) {
        responseKey.addAll(responses.keySet());

        for (String responseName : responseKey) {
            final APIResponse response = responses.get(responseName);
            responseProcessor.processResponse(response);
        }
    }

    public void processSchemas(Set<String> schemaKeys, Map<String, Schema> schemas) {
        schemaKeys.addAll(schemas.keySet());

        for (String schemaName : schemaKeys) {
            final Schema schema = schemas.get(schemaName);
            schemaProcessor.processSchema(schema);
        }
    }
}
