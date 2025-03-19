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

import java.util.List;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.microprofile.openapi20.internal.services.OpenAPIModelOperations;
import io.openliberty.microprofile.openapi20.internal.utils.Constants;
import io.openliberty.microprofile.openapi20.internal.utils.LoggingUtils;
import io.openliberty.microprofile.openapi20.internal.utils.OpenAPIModelOperationsImpl;
import io.smallrye.openapi.api.SmallRyeOASConfig;
import io.smallrye.openapi.runtime.OpenApiRuntimeException;
import io.smallrye.openapi.runtime.io.Format;
import io.smallrye.openapi.runtime.io.IOContext;
import io.smallrye.openapi.runtime.io.JsonIO;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE, service = OpenAPIModelOperations.class)
public class OpenAPI31ModelOperations extends OpenAPIModelOperationsImpl {

    private static final TraceComponent tc = Tr.register(OpenAPI31ModelOperations.class);

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

    @SuppressWarnings("unchecked")
    private <V, A extends V, O extends V, AB, OB> Info parseInfo(String infoJson, IOContext<V, A, O, AB, OB> io) {
        V infoNode = io.jsonIO().fromString(infoJson, Format.JSON);
        Info result = null;
        if (io.jsonIO().isObject(infoNode)) {
            result = io.infoIO().readObject(Info.class, (O) infoNode);
        }
        if (result == null) {
            throw new OpenApiRuntimeException("Unable to parse info JSON: " + infoJson);
        }
        return result;
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
            && model.getPaths().getPathItems().isEmpty()
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

    @Override
    @Trivial
    public OpenAPI createDefaultOpenApiModel() {
        OpenAPI openAPI = OASFactory.createOpenAPI();
        openAPI.setOpenapi(SmallRyeOASConfig.Defaults.VERSION);
        openAPI.paths(OASFactory.createPaths());
        openAPI.info(OASFactory.createInfo().title(Constants.DEFAULT_OPENAPI_DOC_TITLE).version(Constants.DEFAULT_OPENAPI_DOC_VERSION));
        if (LoggingUtils.isEventEnabled(tc)) {
            Tr.event(this, tc, "Created base OpenAPI document");
        }
        return openAPI;
    }

    @Override
    public List<SchemaType> getTypes(Schema schema) {
        return schema.getType();
    }
}
