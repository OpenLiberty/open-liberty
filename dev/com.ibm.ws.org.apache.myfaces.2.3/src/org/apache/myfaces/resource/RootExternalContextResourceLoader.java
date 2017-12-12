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
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import javax.faces.application.ResourceHandler;
import javax.faces.application.ResourceVisitOption;

import javax.faces.context.FacesContext;
import org.apache.myfaces.shared.resource.ExternalContextResourceLoaderIterator;
import org.apache.myfaces.shared.resource.ResourceLoader;
import org.apache.myfaces.shared.resource.ResourceMeta;
import org.apache.myfaces.shared.resource.ResourceMetaImpl;
import org.apache.myfaces.shared.util.WebConfigParamUtils;
import org.apache.myfaces.util.SkipMatchIterator;

/**
 * A resource loader implementation which loads resources from the webapp root.
 * It uses the methods on ExternalContext for handle resources.
 * 
 */
public class RootExternalContextResourceLoader extends ResourceLoader
{
    private static final String CONTRACTS = "contracts";
    
    private static final String RESOURCES = "resources";
    
    private String contractsDirectory = null;
    
    private String resourcesDirectory = null;

    public RootExternalContextResourceLoader()
    {
        super("");
        FacesContext facesContext = FacesContext.getCurrentInstance();
        contractsDirectory = WebConfigParamUtils.getStringInitParameter(facesContext.getExternalContext(), 
                ResourceHandler.WEBAPP_CONTRACTS_DIRECTORY_PARAM_NAME, CONTRACTS);
        contractsDirectory = contractsDirectory.startsWith("/") ? contractsDirectory : '/'+contractsDirectory;
        
        resourcesDirectory = WebConfigParamUtils.getStringInitParameter(facesContext.getExternalContext(), 
            ResourceHandler.WEBAPP_RESOURCES_DIRECTORY_PARAM_NAME, RESOURCES);
        resourcesDirectory = resourcesDirectory.startsWith("/") ? resourcesDirectory : '/'+resourcesDirectory;
    }

    protected Set<String> getResourcePaths(String path)
    {
        String correctedPath = path.startsWith("/") ? path : '/' + path;
        
        if (correctedPath.startsWith(contractsDirectory) || correctedPath.startsWith(resourcesDirectory))
        {
            // Resources under this directory should be accesed by other ContractResourceLoader
            return Collections.emptySet();
        }
        return FacesContext.getCurrentInstance().getExternalContext().getResourcePaths(correctedPath);
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
            String correctedResourceId = resourceId.startsWith("/") ? resourceId : "/"+resourceId;
            if (correctedResourceId.startsWith(contractsDirectory) || 
                correctedResourceId.startsWith(resourcesDirectory))
            {
                return null;
            }
            else
            {
                return FacesContext.getCurrentInstance().getExternalContext().getResource(
                    correctedResourceId);
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
        String resourceId = resourceMeta.getResourceIdentifier();
        String correctedResourceId = resourceId.startsWith("/") ? resourceId : "/"+resourceId;
        if (correctedResourceId.startsWith(contractsDirectory) ||  correctedResourceId.startsWith(resourcesDirectory))
        {
            return null;
        }
        return FacesContext.getCurrentInstance().getExternalContext().getResourceAsStream(correctedResourceId);
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
    
    @Override
    public Iterator<String> iterator(FacesContext facesContext, 
            String path, int maxDepth, ResourceVisitOption... options)
    {
        String basePath = path;
        
        if (getPrefix() != null)
        {
            basePath = getPrefix() + '/' + (path.startsWith("/") ? path.substring(1) : path);
        }
        
        return new RootExternalContextResourceLoaderIterator(
                new ExternalContextResourceLoaderIterator(facesContext, basePath, maxDepth, options), 
                    this.contractsDirectory, this.resourcesDirectory);
    }
    
    private static class RootExternalContextResourceLoaderIterator extends SkipMatchIterator<String>
    {
        private String contractsDirectory;
        private String resourcesDirectory;
        
        public RootExternalContextResourceLoaderIterator(Iterator delegate, String contractsDirectory, 
                String resourcesDirectory)
        {
            super(delegate);
            this.contractsDirectory = contractsDirectory;
            this.resourcesDirectory = resourcesDirectory;
        }

        @Override
        protected boolean match(String instance)
        {
            return instance.startsWith(contractsDirectory) || instance.startsWith(resourcesDirectory);
        }
    }
}
