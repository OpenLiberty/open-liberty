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
package com.ibm.ws.rest.handler.validator.openapi;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.eclipse.microprofile.openapi.spi.OASFactoryResolver;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Provides information about the configured resource validator REST endpoint via Open API.
 */
@Component(name = "com.ibm.ws.rest.handler.validator.openapi",
           configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true)
public class ValidatorOpenAPI {
    private static final TraceComponent tc = Tr.register(ValidatorOpenAPI.class);

    @Reference
    private OASFactoryResolver oas;

    @Activate
    protected void activate(ComponentContext context) {
        Info info = oas.createObject(Info.class).title("Validator REST Endpoint").version("1.0").description("Validates configured resources.");
        OpenAPI api = oas.createObject(OpenAPI.class).info(info);
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {}
}