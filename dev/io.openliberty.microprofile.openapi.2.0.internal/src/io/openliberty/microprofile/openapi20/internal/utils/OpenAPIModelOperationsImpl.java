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

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.OpenAPI;

import io.openliberty.microprofile.openapi20.internal.services.OpenAPIModelOperations;

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
}
