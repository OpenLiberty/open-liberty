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

import java.io.InputStream;
import java.net.URL;
import java.util.Comparator;
import javax.faces.FacesWrapper;

/**
 *
 */
public abstract class ContractResourceLoaderWrapper extends ContractResourceLoader
    implements FacesWrapper<ContractResourceLoader>
{

    public ContractResourceLoaderWrapper()
    {
        super(null);
    }

    @Override
    public String getResourceVersion(String path, String contractName)
    {
        return getWrapped().getResourceVersion(path, contractName);
    }

    @Override
    public String getLibraryVersion(String path, String contractName)
    {
        return getWrapped().getLibraryVersion(path, contractName);
    }

    @Override
    public ResourceMeta createResourceMeta(String prefix, String libraryName, 
        String libraryVersion, String resourceName, String resourceVersion, String contractName)
    {
        return getWrapped().createResourceMeta(prefix, libraryName, 
            libraryVersion, resourceName, resourceVersion, contractName);
    }

    @Override
    public boolean libraryExists(String libraryName, String contractName)
    {
        return getWrapped().libraryExists(libraryName, contractName);
    }

    @Override
    public URL getResourceURL(ResourceMeta resourceMeta)
    {
        return getWrapped().getResourceURL(resourceMeta);
    }

    @Override
    public InputStream getResourceInputStream(ResourceMeta resourceMeta)
    {
        return getWrapped().getResourceInputStream(resourceMeta);
    }

    @Override
    public boolean libraryExists(String libraryName)
    {
        return getWrapped().libraryExists(libraryName);
    }

    @Override
    public ResourceMeta createResourceMeta(String prefix, String libraryName, 
        String libraryVersion, String resourceName, String resourceVersion)
    {
        return getWrapped().createResourceMeta(prefix, libraryName, 
            libraryVersion, resourceName, resourceVersion);
    }

    @Override
    public String getLibraryVersion(String path)
    {
        return getWrapped().getLibraryVersion(path);
    }

    @Override
    public String getResourceVersion(String path)
    {
        return getWrapped().getResourceVersion(path);
    }

    @Override
    public void setPrefix(String prefix)
    {
        getWrapped().setPrefix(prefix);
    }

    @Override
    public String getPrefix()
    {
        return getWrapped().getPrefix();
    }

    @Override
    protected void setVersionComparator(Comparator<String> versionComparator)
    {
        getWrapped().setVersionComparator(versionComparator);
    }

    @Override
    protected Comparator<String> getVersionComparator()
    {
        return getWrapped().getVersionComparator();
    }

    @Override
    public boolean resourceExists(ResourceMeta resourceMeta)
    {
        return getWrapped().resourceExists(resourceMeta);
    }

}
