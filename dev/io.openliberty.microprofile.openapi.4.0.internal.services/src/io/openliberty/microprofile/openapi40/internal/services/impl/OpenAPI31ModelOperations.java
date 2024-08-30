/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi40.internal.services.impl;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import io.openliberty.microprofile.openapi20.internal.services.OpenAPIModelOperations;
import io.openliberty.microprofile.openapi20.internal.utils.Constants;
import io.openliberty.microprofile.openapi20.internal.utils.OpenAPIModelOperationsImpl;
import io.smallrye.openapi.api.SmallRyeOASConfig;
import io.smallrye.openapi.runtime.io.Format;
import io.smallrye.openapi.runtime.io.IOContext;
import io.smallrye.openapi.runtime.io.JsonIO;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE, service = OpenAPIModelOperations.class)
public class OpenAPI31ModelOperations extends OpenAPIModelOperationsImpl {

    @Override
    public OpenAPI shallowCopy(OpenAPI model) {
        OpenAPI copy = super.shallowCopy(model);
        copy.setWebhooks(model.getWebhooks());
        return copy;
    }

    @Override
    public Info parseInfo(String infoJson) {
        return parseInfo(infoJson, IOContext.forJson(JsonIO.newInstance(null)));
    }

    private <V, A extends V, O extends V, AB, OB> Info parseInfo(String infoJson, IOContext<V, A, O, AB, OB> io) {
        V infoNode = io.jsonIO().fromString(infoJson, Format.JSON);
        return io.infoIO().readValue(infoNode);
    }

    @Override
    public boolean isDefaultOpenApiModel(OpenAPI model) {
        boolean isDefault = false;

        if (model.getOpenapi().equals(SmallRyeOASConfig.Defaults.VERSION)
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
            && model.getTags() == null
            && model.getWebhooks() == null) {
            isDefault = true;
        }

        return isDefault;
    }

}
