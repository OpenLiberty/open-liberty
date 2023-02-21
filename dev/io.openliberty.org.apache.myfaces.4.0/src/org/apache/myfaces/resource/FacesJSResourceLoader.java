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

import jakarta.faces.FacesException;
import jakarta.faces.application.ProjectStage;
import jakarta.faces.application.ResourceVisitOption;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.MappingMatch;
import org.apache.myfaces.renderkit.html.util.ResourceUtils;
import org.apache.myfaces.util.ExternalContextUtils;
import org.apache.myfaces.view.facelets.tag.faces.JsfLibrary;

import java.io.*;
import java.net.URL;
import java.util.Comparator;
import java.util.Iterator;

/**
 * Resource loader coming in from our jakarta.faces library
 * The problem why we need a specialized loader is:
 * we have to append/change the mapping information in Development mode
 * (and remove it if present for prod mode)
 * according to the library name and request path
 * Both values are dynamic and the request path is dependent on the
 * patterns provided in the web.xml
 */
public class FacesJSResourceLoader extends ResourceLoaderWrapper
{

    public static final String SOURCE_MAP_MARKER = "//# sourceMappingURL=";

    private final ResourceLoader delegate;

    public FacesJSResourceLoader(ResourceLoader delegate)
    {
        this.delegate = delegate;
    }

    @Override
    public ResourceLoader getWrapped()
    {
        return delegate;
    }


    /**
     * this is the central part, we basically intercept the input stream via an internal pipe
     * and change and add the information on the fly
     * <p />
     * What happens is that a request is coming in which tries to load a faces.js file.
     * In development mode faces-development.js is loaded transparently.
     * In production mode faces.js is loaded.
     * Also, the request, in case of an extension based match, does not load the resource via the .js extension
     * but via js.&lt;match extension&Gt; aka faces.js becomes faces.js.jsf
     * now this extension is dependent on the current match of the request triggering the Faces Servlet.
     * In order to load the mapping files correctly we have to take this into consideration by adding
     * in case of an extension match the match extensions.
     * Also given that we only want to have mapping files in development mode
     * we have to retarget the mapping file to faces-development.js.map no matter how the faces.js
     * request looks like.
     * <p />
     * For production mode we do not want to have any mapping request at all (for now)
     *
     * @param resourceMeta the incoming resource metadata for the faces.js resource
     * @return an input stream on the resource or the mapped input stream which adds the map data
     */
    @Override
    public InputStream getResourceInputStream(ResourceMeta resourceMeta)
    {
        final String extMap = ".map";
        final String resourceName = resourceMeta.getResourceName();
        final String libraryName = resourceMeta.getLibraryName();
        final String mappingLongName = ResourceUtils.FACES_UNCOMPRESSED_JS_RESOURCE_NAME + extMap;
        final String facesJsShortName = ResourceUtils.FACES_MINIMAL_JS_RESOURCE_NAME;
        final String facesJsLongName = ResourceUtils.FACES_UNCOMPRESSED_JS_RESOURCE_NAME;
        HttpServletRequest req = ExternalContextUtils.
                getHttpServletRequest(FacesContext.getCurrentInstance().getExternalContext());

        //name mapping does not happen on meta level
        if(!(libraryName.equals(JsfLibrary.NAMESPACE) &&
                (
                 resourceName.contains(facesJsShortName) ||
                 resourceName.contains(facesJsLongName) //  just in case the behavior is altered in the future
                ) &&
            !resourceName.contains(mappingLongName)) ||
            // should never happen, but this is a safety net that we only can do the remapping detection
            // if an active request is going on
            req == null
        )
        {
            return delegate.getResourceInputStream(resourceMeta);
        }
        try (InputStream inputStream = delegate.getResourceInputStream(resourceMeta);
             ByteArrayOutputStream writer = new ByteArrayOutputStream())
        {
            new BufferedReader(new InputStreamReader(inputStream))
                    .lines()
                    .forEach(line -> {
                        // filtering out the source map for now
                        if (line.contains(SOURCE_MAP_MARKER))
                        {
                            return;
                        }
                        try
                        {
                            writer.write(line.getBytes());
                            writer.write("\n".getBytes());
                        }
                        catch (IOException e)
                        {
                            throw new FacesException(e);
                        }
                    });

            // for development mode we reattach an altered map reference
            if(FacesContext.getCurrentInstance().isProjectStage(ProjectStage.Development))
            {
                // Now the resource is preloaded upon the initial page request. By triggering the Faces Servlet
                // we have to check, whether we have a path based or extension based pattern triggering
                // the request and then add it to the js mapping as extension (if extension based, otherwise
                // we can pass it through as is because the path never is touched by a mapping reference in our case)
                String mappedResourceName = mappingLongName;
                if(req.getHttpServletMapping().getMappingMatch() == MappingMatch.EXTENSION)
                {
                    // as per Servlet spec 12.2, extension based matches must start with *.<extension>
                    // we safely can cut the leading parts before wildcard and the wildcard itself
                    String pattern = req.getHttpServletMapping().getPattern();
                    String extension = pattern.substring(pattern.lastIndexOf("*.") + 1);
                    mappedResourceName = mappedResourceName + extension;
                }

                //We have to rely now on the resource being loaded during a request otherwise we cannot to the remapping
                writer.write("\n".getBytes());
                writer.write(SOURCE_MAP_MARKER.getBytes());
                writer.write(mappedResourceName.getBytes());
                // we are now adding the library name to make the mapping request a resource request
                writer.write(("?ln=" + libraryName).getBytes());
            }
            return new ByteArrayInputStream(writer.toByteArray());
        }
        catch(IOException ex)
        {
            throw new FacesException(ex);
        }
    }

    @Override
    public String getResourceVersion(String path)
    {
        return delegate.getResourceVersion(path);
    }

    @Override
    public String getLibraryVersion(String path)
    {
        return delegate.getLibraryVersion(path);
    }

    @Override
    public URL getResourceURL(ResourceMeta resourceMeta)
    {
        return delegate.getResourceURL(resourceMeta);
    }

    @Override
    public ResourceMeta createResourceMeta(String prefix, String libraryName, String libraryVersion,
                                           String resourceName, String resourceVersion)
    {
        return delegate.createResourceMeta(prefix, libraryName, libraryVersion, resourceName, resourceVersion);
    }

    @Override
    public ResourceMeta createResourceMeta(String prefix, String libraryName, String libraryVersion,
                                           String resourceName, String resourceVersion, String contractName)
    {
        return delegate.createResourceMeta(prefix, libraryName, libraryVersion, resourceName,
                resourceVersion, contractName);
    }

    @Override
    public boolean libraryExists(String libraryName)
    {
        return delegate.libraryExists(libraryName);
    }

    @Override
    public boolean resourceExists(ResourceMeta resourceMeta)
    {
        return delegate.resourceExists(resourceMeta);
    }

    @Override
    public Iterator<String> iterator(FacesContext facesContext, String path,
                                     int maxDepth, ResourceVisitOption... options)
    {
        return delegate.iterator(facesContext, path, maxDepth, options);
    }

    @Override
    public Comparator<String> getVersionComparator()
    {
        return delegate.getVersionComparator();
    }

    @Override
    public void setVersionComparator(Comparator<String> versionComparator)
    {
        delegate.setVersionComparator(versionComparator);
    }

    @Override
    public String getPrefix()
    {
        return delegate.getPrefix();
    }

    @Override
    public void setPrefix(String prefix)
    {
        delegate.setPrefix(prefix);
    }
}
