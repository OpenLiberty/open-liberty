/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.internal.utils;

import java.io.IOException;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.info.Info;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.microprofile.openapi20.internal.services.OpenAPIModelOperations;
import io.smallrye.openapi.api.constants.OpenApiConstants;
import io.smallrye.openapi.runtime.OpenApiRuntimeException;
import io.smallrye.openapi.runtime.io.info.InfoReader;

public class OpenAPIModelOperationsImpl implements OpenAPIModelOperations {

    @Override
    public OpenAPI shallowCopy(OpenAPI model) {
        OpenAPI result = OASFactory.createOpenAPI();

        // Shallow copy each part
        result.setOpenapi(model.getOpenapi());
        result.setComponents(model.getComponents());
        result.setExtensions(model.getExtensions());
        result.setExternalDocs(model.getExternalDocs());
        result.setInfo(model.getInfo());
        result.setPaths(model.getPaths());
        result.setSecurity(model.getSecurity());
        result.setServers(model.getServers());
        result.setTags(model.getTags());

        return result;
    }

    @Override
    @FFDCIgnore(IOException.class)
    public Info parseInfo(String infoJson) throws OpenApiRuntimeException {
        try {
            JsonNode infoNode = new ObjectMapper().readTree(infoJson);
            Info info = InfoReader.readInfo(infoNode);
            return info;
        } catch (IOException e) {
            throw new OpenApiRuntimeException(e);
        }
    }

    @Override
    public boolean isDefaultOpenApiModel(OpenAPI model) {

        // Create the variable to return
        boolean isDefault = false;

        /*
         * The SmallRye implementation generates an OpenAPI model regardless of whether the application contains any
         * OAS or JAX-RS annotations. The default model that is generated is of the form:
         *
         * openapi: 3.0.1
         * info:
         * title: Generated API
         * version: "1.0"
         * servers:
         * - url: http://localhost:8010
         * - url: https://localhost:8020
         * paths: {}
         *
         * This makes detecting whether the application is an OAS application a little more problematic. We need to
         * introspect the generated OpenAPI model object to determine whether it is a real model instance or just a
         * default.
         */
        if (model.getOpenapi().equals(OpenApiConstants.OPEN_API_VERSION)
            && model.getInfo() != null
            && model.getInfo().getContact() == null
            && model.getInfo().getDescription() == null
            && model.getInfo().getLicense() == null
            && model.getInfo().getTermsOfService() == null
            && model.getInfo().getTitle().equals(Constants.DEFAULT_OPENAPI_DOC_TITLE)
            && model.getInfo().getVersion().equals(Constants.DEFAULT_OPENAPI_DOC_VERSION)
            && model.getPaths() != null
            && model.getPaths().getPathItems() == null
            && model.getComponents() == null
            && model.getExtensions() == null
            && model.getExternalDocs() == null
            && model.getSecurity() == null
            && model.getServers() == null
            && model.getTags() == null) {
            isDefault = true;
        }

        return isDefault;
    }

}
