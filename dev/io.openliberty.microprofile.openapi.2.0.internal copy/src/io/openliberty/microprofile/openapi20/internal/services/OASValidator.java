/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.internal.services;

import org.eclipse.microprofile.openapi.models.OpenAPI;

import io.openliberty.microprofile.openapi20.internal.ValidationComponent;

/**
 * OpenAPI model validator. Checks structural constraints on the model and reports errors for violations.
 * <p>
 * Each provider of this service should set the property {@code openapi.version} to the version of the OpenAPI spec they validate.
 * <p>
 * Generally, this service should be called through {@link ValidationComponent} which will identify the correct service depending on the version of the OpenAPI spec the model declares.
 */
public interface OASValidator {

    /**
     * Validate an Open API model
     *
     * @param model the model to validate
     * @return the validation result
     */
    public OASValidationResult validate(OpenAPI model);

}
