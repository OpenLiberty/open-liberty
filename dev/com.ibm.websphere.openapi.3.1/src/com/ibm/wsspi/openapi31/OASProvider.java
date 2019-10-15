/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.openapi31;

import org.eclipse.microprofile.openapi.models.OpenAPI;

/**
 *
 * This interface provides ability to provide OpenAPI Specification documents for a RESTful web services.
 * <p>
 * RESTful web services that want to push their OpenAPI Specification documents into the OpenAPI feature, have to implement this interface.
 *
 * @ibm-spi
 */
public interface OASProvider {
    /**
     * OpenAPI Specification model can be exposed by implementing this method.
     *
     * @return {@link OpenAPI} model constructed through code.
     */
    default OpenAPI getOpenAPIModel() {
        return null;
    }

    /**
     * OpenAPI Specification document can be exposed by implementing this method.
     *
     * @return String containing OpenAPI document.
     */
    default String getOpenAPIDocument() {
        return null;
    }

    /**
     * @return Context root of the document provider.
     */
    String getContextRoot();

    /**
     * Specifies whether the provided documents should be included in the public aggregated OpenAPI feature document or not.
     *
     * @return Specifies if documents should be public or private.
     */
    boolean isPublic();
}
