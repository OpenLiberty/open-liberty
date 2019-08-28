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
package com.ibm.ws.rest.handler.config.openapi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.openapi.Constants;
import com.ibm.ws.microprofile.openapi.impl.core.util.Json;
import com.ibm.ws.microprofile.openapi.impl.core.util.Yaml;
import com.ibm.ws.microprofile.openapi.impl.parser.OpenAPIV3Parser;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIUtils;
import com.ibm.wsspi.rest.handler.RESTHandler;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 * Displays config schema
 */
@Component(name = "com.ibm.ws.rest.handler.config.openapi.Config",
           configurationPolicy = ConfigurationPolicy.IGNORE,
           service = { RESTHandler.class },
           property = { RESTHandler.PROPERTY_REST_HANDLER_CONTEXT_ROOT + "=/openapi/platform",
                        RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=/config" })
public class ConfigSchemaRESTHandler implements RESTHandler {
    private static final TraceComponent tc = Tr.register(ConfigSchemaRESTHandler.class);

    /**
     * Restricts use of the config schema end-point to GET requests only.
     * All other requests will respond with a 405 - method not allowed error.
     *
     * {@inheritDoc}
     */
    @Override
    public final void handleRequest(RESTRequest request, RESTResponse response) throws IOException {
        if (!"GET".equals(request.getMethod())) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Request method was " + request.getMethod() + " but the config schema endpoint is restricted to GET requests only.");
            }
            response.setResponseHeader("Accept", "GET");
            response.sendError(405); // Method Not Allowed
            return;
        }

        OpenAPI openAPI = getOpenAPIDocument(response);

        if (openAPI != null) {
            String acceptHeader = request.getHeader(Constants.ACCEPT_HEADER);
            String format = "yaml";
            if (acceptHeader != null && acceptHeader.equals("application/json")) {
                format = "json";
            }
            String formatParam = request.getParameter("format");
            if (formatParam != null && formatParam.equals("json")) {
                format = "json";
            }

            if (format.equals("json")) {
                response.setContentType("application/json");
                response.getWriter().write(Json.pretty(openAPI));
            } else {
                response.setContentType("text/plain");
                response.getWriter().write(Yaml.pretty(openAPI));
            }
        }
    }

    private OpenAPI getOpenAPIDocument(RESTResponse response) {
        OpenAPI openAPI = null;
        InputStream inputStream = ConfigSchemaRESTHandler.class.getResourceAsStream("/META-INF/openapi.yaml");
        if (inputStream != null) {
            String document = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"));
            openAPI = new OpenAPIV3Parser().readContents(document, null, null, null).getOpenAPI();
            response.setCharacterEncoding("UTF-8");
            if (openAPI == null) {
                if (OpenAPIUtils.isEventEnabled(tc)) {
                    Tr.event(this, tc, "Error retrieving openapi.yaml for config validation. Returning error code 500.");
                }
                response.setStatus(500);
            }
        } else {
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(this, tc, "Null inputStream for openapi.yaml. Return 500.");
            }
            response.setStatus(500);
        }
        return openAPI;
    }
}
