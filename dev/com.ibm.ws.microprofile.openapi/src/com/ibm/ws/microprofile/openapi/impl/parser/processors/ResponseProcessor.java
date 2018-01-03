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
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;

import com.ibm.ws.microprofile.openapi.impl.parser.ResolverCache;
import com.ibm.ws.microprofile.openapi.impl.parser.models.RefFormat;

public class ResponseProcessor {

    private final SchemaProcessor schemaProcessor;
    private final HeaderProcessor headerProcessor;
    private final LinkProcessor linkProcessor;
    private final ExternalRefProcessor externalRefProcessor;
    private final ResolverCache cache;
    private final OpenAPI openAPI;

    public ResponseProcessor(ResolverCache cache, OpenAPI openAPI) {
        schemaProcessor = new SchemaProcessor(cache, openAPI);
        headerProcessor = new HeaderProcessor(cache, openAPI);
        linkProcessor = new LinkProcessor(cache, openAPI);
        this.externalRefProcessor = new ExternalRefProcessor(cache, openAPI);
        this.cache = cache;
        this.openAPI = openAPI;
    }

    public void processResponse(APIResponse response) {

        if (response.getRef() != null) {
            processReferenceResponse(response);
        }
        Schema schema = null;
        if (response.getContent() != null) {
            Map<String, MediaType> content = response.getContent();
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
        if (response.getHeaders() != null) {
            Map<String, Header> headers = response.getHeaders();
            for (String headerName : headers.keySet()) {
                Header header = headers.get(headerName);
                headerProcessor.processHeader(header);
            }

        }
        if (response.getLinks() != null) {
            Map<String, Link> links = response.getLinks();
            for (String linkName : links.keySet()) {
                Link link = links.get(linkName);
                linkProcessor.processLink(link);
            }
        }
    }

    public void processReferenceResponse(APIResponse response) {
        RefFormat refFormat = computeRefFormat(response.getRef());
        String $ref = response.getRef();
        if (isAnExternalRefFormat(refFormat)) {
            final String newRef = externalRefProcessor.processRefToExternalResponse($ref, refFormat);

            if (newRef != null) {
                response.setRef(newRef);
            }
        }
    }
}