/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20;

import java.util.List;

import org.eclipse.microprofile.openapi.models.OpenAPI;

/**
 * The OpenAPIProvider interface defines a set of methods that are used to retrieve the information that is required to
 * generate an OpenAPI document for a given type of provider.
 */
public interface OpenAPIProvider {

    /**
     * The getApplicationPath method returns the path that should be appended to server URLs. If the application path
     * is already present on the Paths in the model, null is returned.
     * 
     * @return String
     *          The context root of the provider.
     */
    public String getApplicationPath();

    /**
     * Returns the OpenAPI model itself
     * 
     * @return the OpenAPI model
     */
    public OpenAPI getModel();

    /**
     * Returns a string representation of the source for the OpenAPI model, suitable for inclusion in log messages.
     * 
     * @return the source of the OpenAPI model
     */
    @Override
    String toString();
    
    /**
     * Returns a list of merge problems which occurred while building this model.
     * <p>
     * If this model was not made from merging multiple models, this will always return an empty list.
     * 
     * @return a list of merge problems. Each list item is a string describing which model couldn't be merged and why.
     */
    public List<String> getMergeProblems();
}
