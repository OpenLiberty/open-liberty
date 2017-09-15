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
package javax.faces.application;

import java.io.IOException;

import javax.faces.context.FacesContext;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFWebConfigParam;

/**
 * @since 2.0
 */
public abstract class ResourceHandler
{
    public static final String LOCALE_PREFIX = "javax.faces.resource.localePrefix";
    public static final String RESOURCE_EXCLUDES_DEFAULT_VALUE = ".class .jsp .jspx .properties .xhtml .groovy";
    
    /**
     * Space separated file extensions that will not be served by the default ResourceHandler implementation.
     */
    @JSFWebConfigParam(defaultValue=".class .jsp .jspx .properties .xhtml .groovy",since="2.0", group="resources")
    public static final String RESOURCE_EXCLUDES_PARAM_NAME = "javax.faces.RESOURCE_EXCLUDES";
    public static final String RESOURCE_IDENTIFIER = "/javax.faces.resource";
    
    /**
     * @since 2.2
     */
    public static final String RESOURCE_CONTRACT_XML = "javax.faces.contract.xml";
    
    /**
     * @since 2.2
     */
    public static final String WEBAPP_CONTRACTS_DIRECTORY_PARAM_NAME = "javax.faces.WEBAPP_CONTRACTS_DIRECTORY";

    /**
     * @since 2.2
     */
    public static final String WEBAPP_RESOURCES_DIRECTORY_PARAM_NAME = "javax.faces.WEBAPP_RESOURCES_DIRECTORY";
     
    public abstract Resource createResource(String resourceName);
    
    public abstract Resource createResource(String resourceName, String libraryName);
    
    public abstract Resource createResource(String resourceName, String libraryName, String contentType);
    
    public abstract String getRendererTypeForResourceName(String resourceName);
    
    public abstract void handleResourceRequest(FacesContext context) throws IOException;
    
    public abstract boolean isResourceRequest(FacesContext context);
    
    public abstract  boolean libraryExists(String libraryName);
    
    /**
     * @since 2.2
     * @param resourceId
     * @return 
     */
    public Resource createResourceFromId(String resourceId)
    {
        return null;
    }
    
    /**
     * 
     * @since 2.2
     * @param context
     * @param resourceName
     * @return 
     */
    public ViewResource createViewResource(FacesContext context,
                                       String resourceName)
    {
        return context.getApplication().getResourceHandler().createResource(resourceName);
    }
    
    public boolean isResourceURL(java.lang.String url)
    {
        if (url == null)
        {
            throw new NullPointerException();
        }
        return url.contains(RESOURCE_IDENTIFIER);
    }
}
