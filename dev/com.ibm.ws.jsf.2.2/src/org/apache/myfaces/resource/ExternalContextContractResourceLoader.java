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
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;
import javax.faces.context.FacesContext;
import org.apache.myfaces.shared.resource.ContractResourceLoader;
import org.apache.myfaces.shared.resource.ResourceMeta;
import org.apache.myfaces.shared.resource.ResourceMetaImpl;

/**
 *
 * @author lu4242
 */
public class ExternalContextContractResourceLoader extends ContractResourceLoader
{

    /**
     * It checks version like this: /1/, /1_0/, /1_0_0/, /100_100/
     * 
     * Used on getLibraryVersion to filter resource directories
     **/
    protected static final Pattern VERSION_CHECKER = Pattern.compile("/\\p{Digit}+(_\\p{Digit}*)*/");

    /**
     * It checks version like this: /1.js, /1_0.js, /1_0_0.js, /100_100.js
     * 
     * Used on getResourceVersion to filter resources
     **/
    protected static final Pattern RESOURCE_VERSION_CHECKER = Pattern.compile("/\\p{Digit}+(_\\p{Digit}*)*\\..*");

    public ExternalContextContractResourceLoader(String prefix)
    {
        super(prefix);
    }

    protected Set<String> getResourcePaths(String contractName, String path)
    {
        return FacesContext.getCurrentInstance().getExternalContext().getResourcePaths(
            getPrefix() + '/' + contractName + '/' + path);
    }

    @Override
    public String getResourceVersion(String path, String contractName)
    {
        String resourceVersion = null;
        Set<String> resourcePaths = this.getResourcePaths(contractName, path);
        if (getPrefix() != null)
        {
            path = getPrefix() + '/' + path;
        }

        if (null != resourcePaths && !resourcePaths.isEmpty())
        {
            // resourceVersion = // execute the comment
            // Look in the resourcePaths for versioned resources.
            // If one or more versioned resources are found, take
            // the one with the "highest" version number as the value
            // of resourceVersion. If no versioned libraries
            // are found, let resourceVersion remain null.
            for (String resourcePath : resourcePaths)
            {
                String version = "";
                if (path.length() < resourcePath.length()) {
                    version = resourcePath.substring(path.length());
                }
                
                if (RESOURCE_VERSION_CHECKER.matcher(version).matches())
                {
                    version = version.substring(1, version.lastIndexOf('.'));
                    if (resourceVersion == null)
                    {
                        resourceVersion = version;
                    }
                    else if (getVersionComparator().compare(resourceVersion, version) < 0)
                    {
                        resourceVersion = version;
                    }
                }
            }
            //Since it is a directory and no version was found, set as invalid
            if (resourceVersion == null)
            {
                resourceVersion = VERSION_INVALID;
            }
        }
        return resourceVersion;
    }

    @Override
    public String getLibraryVersion(String path, String contractName)
    {
        String libraryVersion = null;
        Set<String> libraryPaths = this.getResourcePaths(contractName, path);
        path = getPrefix() + '/' + path;
        if (null != libraryPaths && !libraryPaths.isEmpty())
        {
            // Look in the libraryPaths for versioned libraries.
            // If one or more versioned libraries are found, take
            // the one with the "highest" version number as the value
            // of libraryVersion. If no versioned libraries
            // are found, let libraryVersion remain null.

            for (Iterator<String> it = libraryPaths.iterator(); it.hasNext();)
            {
                String libraryPath = it.next();
                String version = "";
                if (path.length() < libraryPath.length())
                {
                    version = libraryPath.substring(path.length());
                }

                if (VERSION_CHECKER.matcher(version).matches())
                {
                    version = version.substring(1, version.length() - 1);
                    if (libraryVersion == null)
                    {
                        libraryVersion = version;
                    }
                    else if (getVersionComparator().compare(libraryVersion, version) < 0)
                    {
                        libraryVersion = version;
                    }
                }
            }
        }
        return libraryVersion;
    }

    @Override
    public URL getResourceURL(ResourceMeta resourceMeta)
    {
        try
        {
            return FacesContext.getCurrentInstance().getExternalContext().getResource(
                getPrefix() + '/' + resourceMeta.getContractName() + '/' + resourceMeta.getResourceIdentifier());
        }
        catch (MalformedURLException e)
        {
            return null;
        }
    }

    @Override
    public InputStream getResourceInputStream(ResourceMeta resourceMeta)
    {
        return FacesContext.getCurrentInstance().getExternalContext().getResourceAsStream(
            getPrefix() + '/' + resourceMeta.getContractName() + '/' + resourceMeta.getResourceIdentifier());
    }

    @Override
    public ResourceMeta createResourceMeta(String prefix, String libraryName, String libraryVersion,
                                           String resourceName, String resourceVersion, String contractName)
    {
        return new ResourceMetaImpl(prefix, libraryName, libraryVersion, resourceName, 
            resourceVersion, contractName);
    }

    @Override
    public boolean libraryExists(String libraryName, String contractName)
    {
        if (getPrefix() != null && !"".equals(getPrefix()))
        {
            try
            {
                URL url =
                    FacesContext.getCurrentInstance().getExternalContext().getResource(
                        getPrefix() + '/' + contractName + '/' + libraryName);
                if (url != null)
                {
                    return true;
                }
            }
            catch (MalformedURLException e)
            {
                return false;
            }
        }
        else
        {
            try
            {

                URL url = FacesContext.getCurrentInstance().
                    getExternalContext().getResource(contractName + '/' +libraryName);

                if (url != null)
                {
                    return true;
                }
            }
            catch (MalformedURLException e)
            {
                return false;
            }
        }
        return false;
    }
    
}
