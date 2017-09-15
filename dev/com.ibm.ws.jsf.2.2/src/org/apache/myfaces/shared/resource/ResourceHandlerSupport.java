/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.shared.resource;

import javax.faces.application.ResourceHandler;
import javax.faces.context.FacesContext;

/**
 * A utility class to isolate a ResourceHandler implementation from its
 * underlying implementation
 */
public abstract class ResourceHandlerSupport
{

    /**
     * Calculate the resource base path.
     * 
     * It should extract a string like:
     * 
     * ResourceHandler.RESOURCE_IDENTIFIER + '/' + getResourceName()
     * 
     * For example:
     * 
     * /javax.faces.resource/image.jpg
     * 
     * This is used on ResourceHandler.handleResourceRequest()
     * 
     */
    public abstract String calculateResourceBasePath(FacesContext facesContext);

    /**
     * Return an array of resource loaders used to find resources.
     * The order of ResourceLoaders define its precedence. 
     * 
     * @return
     */
    public abstract ResourceLoader[] getResourceLoaders();

    /**
     * Return an array of resource loaders used to find resources 
     * associated with a contract. The order of ContractResourceLoaders 
     * define its precedence. 
     * 
     * @since 2.2
     * @return 
     */
    public abstract ContractResourceLoader[] getContractResourceLoaders();

    /**
     * Return an array of resource loaders used to find resources
     * that can be located using ResourceHandler.createViewResource().
     * The order of ResourceLoaders define its precedence. 
     * 
     * @since 2.2
     * @return 
     */
    public abstract ResourceLoader[] getViewResourceLoaders();
    
    /**
     * Check if the mapping used is done using extensions (.xhtml, .jsf)
     * or if it is not (/faces/*)
     * @return
     */
    public abstract boolean isExtensionMapping();
    
    /**
     * Get the mapping used as prefix(/faces) or sufix(.jsf)
     * 
     * @return
     */
    public abstract String getMapping();
    
    /**
     * Return the time when the app started. This is useful to set the
     * "Last-Modified" header in some specific cases.
     * 
     * @return
     */
    public abstract long getStartupTime();
    
    /**
     * Return the time that should be set on "Expires" header in a resource.
     * 
     * @return
     */
    public abstract long getMaxTimeExpires();
    
    public String getResourceIdentifier()
    {
        return ResourceHandler.RESOURCE_IDENTIFIER;
    }
}
