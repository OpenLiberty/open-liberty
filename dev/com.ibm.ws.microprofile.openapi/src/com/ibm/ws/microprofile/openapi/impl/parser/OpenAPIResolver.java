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

package com.ibm.ws.microprofile.openapi.impl.parser;

import java.util.List;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;

import com.ibm.ws.microprofile.openapi.impl.parser.core.models.AuthorizationValue;
import com.ibm.ws.microprofile.openapi.impl.parser.processors.ComponentsProcessor;
import com.ibm.ws.microprofile.openapi.impl.parser.processors.OperationProcessor;
import com.ibm.ws.microprofile.openapi.impl.parser.processors.PathsProcessor;

public class OpenAPIResolver {

    private final OpenAPI openApi;
    private final ResolverCache cache;
    private final ComponentsProcessor componentsProcessor;
    private final PathsProcessor pathProcessor;
    private final OperationProcessor operationsProcessor;
    private Settings settings = new Settings();

    public OpenAPIResolver(OpenAPI openApi) {
        this(openApi, null, null, null);
    }

    public OpenAPIResolver(OpenAPI openApi, List<AuthorizationValue> auths) {
        this(openApi, auths, null, null);
    }

    public OpenAPIResolver(OpenAPI openApi, List<AuthorizationValue> auths, String parentFileLocation) {
        this(openApi, auths, parentFileLocation, null);
    }

    public OpenAPIResolver(OpenAPI openApi, List<AuthorizationValue> auths, String parentFileLocation, Settings settings) {
        this.openApi = openApi;
        this.settings = settings != null ? settings : new Settings();
        this.cache = new ResolverCache(openApi, auths, parentFileLocation);
        componentsProcessor = new ComponentsProcessor(openApi, this.cache);
        pathProcessor = new PathsProcessor(cache, openApi, this.settings);
        operationsProcessor = new OperationProcessor(cache, openApi);
    }

    public OpenAPI resolve() {
        if (openApi == null) {
            return null;
        }

        pathProcessor.processPaths();
        componentsProcessor.processComponents();

        if (openApi.getPaths() != null) {
            for (String pathname : openApi.getPaths().keySet()) {
                PathItem pathItem = openApi.getPaths().get(pathname);
                if (pathItem.readOperations() != null) {
                    for (Operation operation : pathItem.readOperations()) {
                        operationsProcessor.processOperation(operation);
                    }
                }
            }
        }

        return openApi;
    }

    public static class Settings {

        private boolean addParametersToEachOperation = true;

        /**
         * If true, resource parameters are added to each operation
         */
        public boolean addParametersToEachOperation() {
            return this.addParametersToEachOperation;
        }

        /**
         * If true, resource parameters are added to each operation
         */
        public Settings addParametersToEachOperation(final boolean addParametersToEachOperation) {
            this.addParametersToEachOperation = addParametersToEachOperation;
            return this;
        }

    }
}
