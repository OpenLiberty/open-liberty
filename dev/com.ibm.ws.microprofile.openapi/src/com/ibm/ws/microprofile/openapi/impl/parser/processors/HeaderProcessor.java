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

import java.util.Map;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;

import com.ibm.ws.microprofile.openapi.impl.parser.ResolverCache;
import com.ibm.ws.microprofile.openapi.impl.parser.models.RefFormat;

public class HeaderProcessor {

    private final ResolverCache cache;
    private final SchemaProcessor schemaProcessor;
    private final ExampleProcessor exampleProcessor;
    private final ExternalRefProcessor externalRefProcessor;
    private final OpenAPI openAPI;

    public HeaderProcessor(ResolverCache cache, OpenAPI openAPI) {
        this.cache = cache;
        this.openAPI = openAPI;
        this.schemaProcessor = new SchemaProcessor(cache, openAPI);
        this.exampleProcessor = new ExampleProcessor(cache, openAPI);
        this.externalRefProcessor = new ExternalRefProcessor(cache, openAPI);
    }

    public void processHeader(Header header) {

        if (header.getRef() != null) {
            RefFormat refFormat = computeRefFormat(header.getRef());
            String $ref = header.getRef();
            if (isAnExternalRefFormat(refFormat)) {
                final String newRef = externalRefProcessor.processRefToExternalHeader($ref, refFormat);
                if (newRef != null) {
                    header.setRef(newRef);
                }
            }
        }
        if (header.getSchema() != null) {
            schemaProcessor.processSchema(header.getSchema());

        }
        if (header.getExamples() != null) {
            if (header.getExamples() != null) {
                Map<String, Example> examples = header.getExamples();
                for (String key : examples.keySet()) {
                    exampleProcessor.processExample(header.getExamples().get(key));
                }

            }
        }
        Schema schema = null;
        if (header.getContent() != null) {
            Map<String, MediaType> content = header.getContent();
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
}