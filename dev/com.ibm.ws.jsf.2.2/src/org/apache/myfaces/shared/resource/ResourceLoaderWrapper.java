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

public abstract class ResourceLoaderWrapper extends ResourceLoader implements FacesWrapper<ResourceLoader>
{
    
    public ResourceLoaderWrapper()
    {
        super(null);
    }

    public String getResourceVersion(String path)
    {
        return getWrapped().getResourceVersion(path);
    }

    public String getLibraryVersion(String path)
    {
        return getWrapped().getLibraryVersion(path);
    }

    public URL getResourceURL(ResourceMeta resourceMeta)
    {
        return getWrapped().getResourceURL(resourceMeta);
    }

    public InputStream getResourceInputStream(ResourceMeta resourceMeta)
    {
        return getWrapped().getResourceInputStream(resourceMeta);
    }

    public ResourceMeta createResourceMeta(String prefix, String libraryName,
            String libraryVersion, String resourceName, String resourceVersion)
    {
        return getWrapped().createResourceMeta(prefix, libraryName, libraryVersion,
                resourceName, resourceVersion);
    }

    public boolean libraryExists(String libraryName)
    {
        return getWrapped().libraryExists(libraryName);
    }

    public String getPrefix()
    {
        return getWrapped().getPrefix();
    }

    public void setPrefix(String prefix)
    {
        getWrapped().setPrefix(prefix);
    }

    @Override
    public boolean resourceExists(ResourceMeta resourceMeta)
    {
        return getWrapped().resourceExists(resourceMeta);
    }

    /*
    @Override
    public boolean resourceIdExists(String resourceId)
    {
        return getWrapped().resourceIdExists(resourceId);
    }

    @Override
    public URL getResourceURL(String resourceId)
    {
        return getWrapped().getResourceURL(resourceId);
    }*/

    @Override
    protected Comparator<String> getVersionComparator()
    {
        return getWrapped().getVersionComparator();
    }

    @Override
    protected void setVersionComparator(Comparator<String> versionComparator)
    {
        getWrapped().setVersionComparator(versionComparator);
    }
    
}
