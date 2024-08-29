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
}
