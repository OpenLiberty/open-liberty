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
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import io.openliberty.microprofile.openapi20.internal.services.OpenAPIModelOperations;
import io.openliberty.microprofile.openapi20.internal.utils.OpenAPIModelOperationsImpl;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE, service = OpenAPIModelOperations.class)
public class OpenAPI31ModelOperations extends OpenAPIModelOperationsImpl {

    @Override
    public OpenAPI shallowCopy(OpenAPI model) {
        OpenAPI copy = super.shallowCopy(model);
        copy.setWebhooks(model.getWebhooks());
        return copy;
    }

}
