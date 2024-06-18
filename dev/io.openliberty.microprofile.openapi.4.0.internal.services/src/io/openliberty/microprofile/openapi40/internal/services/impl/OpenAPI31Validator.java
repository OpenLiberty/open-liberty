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

import io.openliberty.microprofile.openapi20.internal.services.OASValidationResult;
import io.openliberty.microprofile.openapi20.internal.services.OASValidator;

@Component(service = OASValidator.class, configurationPolicy = ConfigurationPolicy.IGNORE)
public class OpenAPI31Validator implements OASValidator {

    @Override
    public OASValidationResult validate(OpenAPI model) {
        // TODO: implement validation
        return new OASValidationResult();
    }

}
