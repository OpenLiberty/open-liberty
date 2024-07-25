/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.internal;

import java.io.IOException;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.jandex.Index;

import com.ibm.wsspi.adaptable.module.Container;

import io.openliberty.microprofile.openapi20.internal.services.ModelGenerator;
import io.openliberty.microprofile.openapi20.internal.utils.Constants;
import io.openliberty.microprofile.openapi20.internal.utils.OpenAPIUtils;
import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.constants.OpenApiConstants;
import io.smallrye.openapi.api.util.ConfigUtil;
import io.smallrye.openapi.api.util.FilterUtil;
import io.smallrye.openapi.api.util.MergeUtil;
import io.smallrye.openapi.runtime.OpenApiProcessor;
import io.smallrye.openapi.runtime.OpenApiStaticFile;
import io.smallrye.openapi.runtime.io.CurrentScannerInfo;
import io.smallrye.openapi.runtime.scanner.SchemaRegistry;
import io.smallrye.openapi.runtime.scanner.processor.JavaSecurityProcessor;

public class ModelGeneratorImpl implements ModelGenerator {

    @Override
    public OpenAPI generateModel(OpenApiConfig config, Container appContainer, ClassLoader appClassloader, ClassLoader tccl, Index index) {
        OpenAPI openAPIModel;
        try {
            // Perform the processing rules from the spec in order

            // Step 1: Call an OASModelReader if configured in the application to generate the initial model
            openAPIModel = OpenApiProcessor.modelFromReader(config, tccl);

            // Step 2: Read openapi.yaml file from the application if present and add to model
            try (OpenApiStaticFile staticFile = StaticFileProcessor.getOpenAPIFile(appContainer)) {
                openAPIModel = MergeUtil.merge(openAPIModel, OpenApiProcessor.modelFromStaticFile(staticFile));
            } catch (IOException e) {
                // Can only get this when closing the file, ignore and FFDC
            }

            // Step 3: Scan OpenAPI and JAX-RS annotations and add to model
            if (index != null) {
                openAPIModel = MergeUtil.merge(openAPIModel, OpenApiProcessor.modelFromAnnotations(config, index));
            }

            // Step 4: Apply any filters configured in the application to the model
            OASFilter filter = OpenApiProcessor.getFilter(config, appClassloader);
            if (filter != null) {
                openAPIModel = FilterUtil.applyFilter(filter, openAPIModel);
            }

            // At this point if we have an empty model, we can give up
            if (openAPIModel != null) {
                // Set required fields
                if (openAPIModel.getOpenapi() == null) {
                    openAPIModel.setOpenapi(OpenApiConstants.OPEN_API_VERSION);
                }

                if (openAPIModel.getPaths() == null) {
                    openAPIModel.setPaths(OASFactory.createPaths());
                }

                if (openAPIModel.getInfo() == null) {
                    openAPIModel.setInfo(OASFactory.createInfo());
                }

                if (openAPIModel.getInfo().getTitle() == null) {
                    openAPIModel.getInfo().setTitle(Constants.DEFAULT_OPENAPI_DOC_TITLE);
                }

                if (openAPIModel.getInfo().getVersion() == null) {
                    openAPIModel.getInfo().setVersion(Constants.DEFAULT_OPENAPI_DOC_VERSION);
                }

                ConfigUtil.applyConfig(config, openAPIModel);

                if (OpenAPIUtils.isDefaultOpenApiModel(openAPIModel)) {
                    openAPIModel = null;
                }
            }

        } finally {
            // Some versions of smallrye-open-api store the "current" instance of several objects in thread-locals while building the model
            // and don't clear them properly at the end leading to memory leaks if applications are redeployed (#24577).
            // Manually "remove" the current instance of these objects.
            SchemaRegistry.remove();
            JavaSecurityProcessor.remove();
            CurrentScannerInfo.remove();
        }

        return openAPIModel;
    }

}
