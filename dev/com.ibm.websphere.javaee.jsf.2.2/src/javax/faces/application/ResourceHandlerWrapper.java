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

import javax.faces.FacesWrapper;
import javax.faces.context.FacesContext;

/**
 * @since 2.0
 */
public abstract class ResourceHandlerWrapper extends ResourceHandler
    implements FacesWrapper<ResourceHandler>
{
    @Override
    public Resource createResource(String resourceName)
    {
        return getWrapped().createResource(resourceName);
    }
    
    @Override
    public Resource createResource(String resourceName, String libraryName)
    {
        return getWrapped().createResource(resourceName, libraryName);
    }
    
    @Override
    public Resource createResource(String resourceName, String libraryName, String contentType)
    {
        return getWrapped().createResource(resourceName, libraryName, contentType);
    }
    
    @Override
    public String getRendererTypeForResourceName(String resourceName)
    {
        return getWrapped().getRendererTypeForResourceName(resourceName);
    }
    
    @Override
    public void handleResourceRequest(FacesContext context) throws IOException
    {
        getWrapped().handleResourceRequest(context);
    }
    
    @Override
    public boolean isResourceRequest(FacesContext context)
    {
        return getWrapped().isResourceRequest(context);
    }
    
    @Override
    public boolean libraryExists(String libraryName)
    {
        return getWrapped().libraryExists(libraryName);
    }

    @Override
    public Resource createResourceFromId(String resourceId)
    {
        return getWrapped().createResourceFromId(resourceId);
    }

    @Override
    public ViewResource createViewResource(FacesContext context, String resourceName)
    {
        return getWrapped().createViewResource(context, resourceName);
    }

    @Override
    public boolean isResourceURL(String url)
    {
        return getWrapped().isResourceURL(url);
    }
    
    public abstract ResourceHandler getWrapped();
}
