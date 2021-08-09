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
package org.apache.myfaces.resource;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

import javax.faces.context.FacesContext;
import org.apache.myfaces.shared.resource.ResourceLoader;
import org.apache.myfaces.shared.resource.ResourceMeta;
import org.apache.myfaces.shared.resource.ResourceMetaImpl;

/**
 * A resource loader implementation which loads resources from the webapp root.
 * It uses the methods on ExternalContext for handle resources.
 * 
 */
public class RootExternalContextResourceLoader extends ResourceLoader
{

    public RootExternalContextResourceLoader()
    {
        super("");
    }

    protected Set<String> getResourcePaths(String path)
    {
        if (path.startsWith("/"))
        {
            return FacesContext.getCurrentInstance().getExternalContext().getResourcePaths(path);
        }
        else
        {
            return FacesContext.getCurrentInstance().getExternalContext().getResourcePaths('/' + path);
        }
    }

    @Override
    public String getResourceVersion(String path)
    {
        return null;
    }

    @Override
    public String getLibraryVersion(String path)
    {
        return null;
    }

    //@Override
    public URL getResourceURL(String resourceId)
    {
        try
        {
            if (resourceId.startsWith("/"))
            {
                return FacesContext.getCurrentInstance().getExternalContext().getResource(
                    resourceId);
            }
            else
            {
                return FacesContext.getCurrentInstance().getExternalContext().getResource(
                    '/' + resourceId);
            }
        }
        catch (MalformedURLException e)
        {
            return null;
        }
    }
    
    @Override
    public URL getResourceURL(ResourceMeta resourceMeta)
    {
        if (resourceMeta.getLocalePrefix() != null)
        {
            // the webapp root folder cannot be localized, 
            // but if the resource lives in /contracts/[contract-name] it can be.
            return null;
        }
        return getResourceURL(resourceMeta.getResourceIdentifier());
    }

    @Override
    public InputStream getResourceInputStream(ResourceMeta resourceMeta)
    {
        return FacesContext.getCurrentInstance().getExternalContext().getResourceAsStream(
            '/' + resourceMeta.getResourceIdentifier());
    }

    @Override
    public ResourceMeta createResourceMeta(String prefix, String libraryName, String libraryVersion,
                                           String resourceName, String resourceVersion)
    {
        return new ResourceMetaImpl(prefix, libraryName, libraryVersion, resourceName, resourceVersion);
    }

    @Override
    public boolean libraryExists(String libraryName)
    {
        //No library can be created in root resolver
        return false;
    }
}
