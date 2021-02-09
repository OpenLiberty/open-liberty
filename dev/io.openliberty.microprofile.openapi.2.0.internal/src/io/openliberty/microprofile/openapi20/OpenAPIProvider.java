/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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

import org.eclipse.microprofile.openapi.models.servers.Server;

import io.smallrye.openapi.runtime.io.Format;

/**
 * The OpenAPIProvider interface defines a set of methods that are used to retrieve the information that is required to
 * generate an OpenAPI document for a given type of provider.
 */
public interface OpenAPIProvider {
    /**
     * The getOpenAPIDocument method returns the OpenAPI document for the provider in the specified format.
     *
     * @param format
     *          The format of the generated OpenAPI document.
     * @return String
     *          The OpenAPI document.
     */
    public String getOpenAPIDocument(Format format);

    /**
     * The getOpenAPIDocument method returns the OpenAPI document for the provider in the specified format.
     *
     * @param servers
     *          The list of servers that should be added to the OpenAPI model before generating the OpenAPI document.
     * @param format
     *          The format of the generated OpenAPI document.
     * @return String
     *          The OpenAPI document.
     */
    public String getOpenAPIDocument(List<Server> servers, Format format);

    /**
     * The getContextRoot method returns the context root for the provider.
     * 
     * @return String
     *          The context root of the provider.
     */
    public String getContextRoot();

    /**
     * The getApplicationPath method returns the path that should be appended to server URLs. If the application path
     * is already present on the Paths in the model, null is returned.
     * 
     * @return String
     *          The context root of the provider.
     */
    public String getApplicationPath();

    /**
     * The getServersDefined method returns true ifthe OpenAPI model generated from the application contains server
     * definitions.  If the OpenAPI model does not contain any server definitions they will be generated on demand each
     * time the OpenAPI document is requested. 
     * 
     * @return boolean
     *          The true iff the OpenAPI model contains server definitions.
     */
    public boolean getServersDefined();
}
