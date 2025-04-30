/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi40.internal.impl;

import java.net.URL;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.jandex.Index;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;

import io.openliberty.microprofile.openapi20.internal.services.ModelGenerator;
import io.openliberty.microprofile.openapi20.internal.services.OpenAPIModelOperations;
import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.SmallRyeOpenAPI;
import io.smallrye.openapi.jaxrs.JaxRsAnnotationScanner;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class ModelGenerator40Impl implements ModelGenerator {

    @Reference
    private OpenAPIModelOperations modelOps;

    @Override
    public OpenAPI generateModel(OpenApiConfig config, Container appContainer, ClassLoader appClassloader, ClassLoader threadContextClassloader, Index index) {

        SmallRyeOpenAPI.Builder builder = SmallRyeOpenAPI.builder();
        builder.withApplicationClassLoader(appClassloader);
        if (index == null) {
            builder.enableAnnotationScan(false);
        } else {
            builder.withIndex(index);
        }
        builder.withScannerClassLoader(JaxRsAnnotationScanner.class.getClassLoader());
        builder.withResourceLocator(path -> ModelGenerator40Impl.getResource(appContainer, path));
        OpenAPI model = builder.build().model();

        if (modelOps.isDefaultOpenApiModel(model)) {
            model = null;
        }

        return model;
    }

    private static URL getResource(Container container, String path) {
        Entry entry = container.getEntry(path);
        if (entry != null) {
            return entry.getResource();
        } else {
            return null;
        }
    }

}
