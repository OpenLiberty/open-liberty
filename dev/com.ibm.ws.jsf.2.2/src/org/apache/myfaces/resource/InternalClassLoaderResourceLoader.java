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
import java.net.URL;

import javax.faces.application.ProjectStage;
import javax.faces.context.FacesContext;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFWebConfigParam;
import org.apache.myfaces.shared.resource.AliasResourceMetaImpl;
import org.apache.myfaces.shared.resource.ResourceLoader;
import org.apache.myfaces.shared.resource.ResourceMeta;
import org.apache.myfaces.shared.resource.ResourceMetaImpl;
import org.apache.myfaces.shared.util.ClassUtils;
import org.apache.myfaces.shared.util.WebConfigParamUtils;
import org.apache.myfaces.shared.renderkit.html.util.ResourceUtils;

/**
 * A resource loader implementation which loads resources from the thread ClassLoader.
 */
public class InternalClassLoaderResourceLoader extends ResourceLoader
{

    /**
     * If this param is true and the project stage is development mode,
     * the source javascript files will be loaded separately instead have
     * all in just one file, to preserve line numbers and make javascript
     * debugging of the default jsf javascript file more simple.
     */
    @JSFWebConfigParam(since = "2.0.1", defaultValue = "false", expectedValues = "true,false", group = "render")
    public static final String USE_MULTIPLE_JS_FILES_FOR_JSF_UNCOMPRESSED_JS
            = "org.apache.myfaces.USE_MULTIPLE_JS_FILES_FOR_JSF_UNCOMPRESSED_JS";

    /**
     * Define the mode used for jsf.js file:
     * <ul>
     * <li>normal : contains everything, including jsf-i18n.js, jsf-experimental.js and jsf-legacy.js</li>
     * <li>minimal-modern : is the core jsf with a baseline of ie9+,
     * without jsf-i18n.js, jsf-experimental.js and jsf-legacy.js</li>
     * <li>minimal: which is the same with a baseline of ie6, without jsf-i18n.js, jsf-experimental.js</li>
     * </ul>
     * <p>If org.apache.myfaces.USE_MULTIPLE_JS_FILES_FOR_JSF_UNCOMPRESSED_JS param is set to true and project stage
     * is Development, this param is ignored.</p>
     */
    @JSFWebConfigParam(since = "2.0.10,2.1.4", defaultValue = "normal",
                       expectedValues = "normal, minimal-modern, minimal", group = "render")
    public static final String MYFACES_JSF_MODE = "org.apache.myfaces.JSF_JS_MODE";
    
    private final boolean _useMultipleJsFilesForJsfUncompressedJs;
    private final String _jsfMode;
    private final boolean _developmentStage;

    public InternalClassLoaderResourceLoader(String prefix)
    {
        super(prefix);
        _useMultipleJsFilesForJsfUncompressedJs
                = WebConfigParamUtils.getBooleanInitParameter(FacesContext.getCurrentInstance().getExternalContext(),
                    USE_MULTIPLE_JS_FILES_FOR_JSF_UNCOMPRESSED_JS, false);

        _jsfMode = WebConfigParamUtils.getStringInitParameter(FacesContext.getCurrentInstance().getExternalContext(),
                    MYFACES_JSF_MODE, ResourceUtils.JSF_MYFACES_JSFJS_NORMAL);
        _developmentStage = FacesContext.getCurrentInstance().isProjectStage(ProjectStage.Development);
    }

    @Override
    public String getLibraryVersion(String path)
    {
        return null;
    }

    @Override
    public InputStream getResourceInputStream(ResourceMeta resourceMeta)
    {
        InputStream is;
        if (getPrefix() != null && !"".equals(getPrefix()))
        {
            String name = getPrefix() + '/' + resourceMeta.getResourceIdentifier();
            is = getClassLoader().getResourceAsStream(name);
            if (is == null)
            {
                is = this.getClass().getClassLoader().getResourceAsStream(name);
            }
            return is;
        }
        else
        {
            is = getClassLoader().getResourceAsStream(resourceMeta.getResourceIdentifier());
            if (is == null)
            {
                is = this.getClass().getClassLoader().getResourceAsStream(resourceMeta.getResourceIdentifier());
            }
            return is;
        }
    }

    public URL getResourceURL(String resourceId)
    {
        URL url;
        if (getPrefix() != null && !"".equals(getPrefix()))
        {
            String name = getPrefix() + '/' + resourceId;
            url = getClassLoader().getResource(name);
            if (url == null)
            {
                url = this.getClass().getClassLoader().getResource(name);
            }
            return url;
        }
        else
        {
            url = getClassLoader().getResource(resourceId);
            if (url == null)
            {
                url = this.getClass().getClassLoader().getResource(resourceId);
            }
            return url;
        }
    }
    
    @Override
    public URL getResourceURL(ResourceMeta resourceMeta)
    {
        return getResourceURL(resourceMeta.getResourceIdentifier());
    }

    @Override
    public String getResourceVersion(String path)
    {
        return null;
    }

    @Override
    public ResourceMeta createResourceMeta(String prefix, String libraryName, String libraryVersion,
                                           String resourceName, String resourceVersion)
    {
        //handle jsf.js
        final boolean javaxFacesLib = libraryName != null &&
        ResourceUtils.JAVAX_FACES_LIBRARY_NAME.equals(libraryName);
        final boolean javaxFaces = javaxFacesLib &&
                ResourceUtils.JSF_JS_RESOURCE_NAME.equals(resourceName);

        if (javaxFaces)
        {
            if (_developmentStage)
            {
                if (_useMultipleJsFilesForJsfUncompressedJs)
                {
                    return new AliasResourceMetaImpl(prefix, libraryName, libraryVersion,
                            resourceName, resourceVersion, ResourceUtils.JSF_UNCOMPRESSED_JS_RESOURCE_NAME, true);
                }
                else
                {
                    //normall we would have to take care about the standard jsf.js case also
                    //but our standard resource loader takes care of it,
                    // because this part is only called in debugging mode
                    //in production only in debugging
                    return new AliasResourceMetaImpl(prefix, libraryName, libraryVersion, resourceName, resourceVersion,
                                                     "jsf-uncompressed-full.js", false);
                }
            }
            else if (_jsfMode.equals(ResourceUtils.JSF_MYFACES_JSFJS_MINIMAL) )
            {
                return new AliasResourceMetaImpl(prefix, libraryName, libraryVersion, resourceName, resourceVersion,
                        ResourceUtils.JSF_MINIMAL_JS_RESOURCE_NAME, false);
            }
            else if (_jsfMode.equals(ResourceUtils.JSF_MYFACES_JSFJS_MINIMAL_MODERN) )
            {
                return new AliasResourceMetaImpl(prefix, libraryName, libraryVersion, resourceName, resourceVersion,
                        ResourceUtils.JSF_MINIMAL_MODERN_JS_RESOURCE_NAME, false);
            }
            else
            {
                return null;
            }
        }
        else if (javaxFacesLib && !_developmentStage && !_jsfMode.equals(ResourceUtils.JSF_MYFACES_JSFJS_NORMAL) &&
                                   (ResourceUtils.JSF_MYFACES_JSFJS_I18N.equals(resourceName) ||
                                   ResourceUtils.JSF_MYFACES_JSFJS_EXPERIMENTAL.equals(resourceName) ||
                                   ResourceUtils.JSF_MYFACES_JSFJS_LEGACY.equals(resourceName)) )
        {
            return new ResourceMetaImpl(prefix, libraryName, libraryVersion, resourceName, resourceVersion);
        }
        else if (_developmentStage && libraryName != null &&
                ResourceUtils.MYFACES_LIBRARY_NAME.equals(libraryName) &&
                ResourceUtils.MYFACES_JS_RESOURCE_NAME.equals(resourceName))
        {
            //handle the oamSubmit.js
            return new AliasResourceMetaImpl(prefix, libraryName, libraryVersion,
                    resourceName, resourceVersion, ResourceUtils.MYFACES_JS_RESOURCE_NAME_UNCOMPRESSED, true);
        }
        else if (_developmentStage && libraryName != null && libraryName.startsWith("org.apache.myfaces.core"))
        {
            return new ResourceMetaImpl(prefix, libraryName, libraryVersion, resourceName, resourceVersion);
        }
        else
        {
            return null;
        }
    }

    /**
     * Returns the ClassLoader to use when looking up resources under the top level package. By default, this is the
     * context class loader.
     *
     * @return the ClassLoader used to lookup resources
     */
    protected ClassLoader getClassLoader()
    {
        return ClassUtils.getContextClassLoader();
    }

    @Override
    public boolean libraryExists(String libraryName)
    {
        if (getPrefix() != null && !"".equals(getPrefix()))
        {
            URL url = getClassLoader().getResource(getPrefix() + '/' + libraryName);
            if (url == null)
            {
                url = this.getClass().getClassLoader().getResource(getPrefix() + '/' + libraryName);
            }
            if (url != null)
            {
                return true;
            }
        }
        else
        {
            URL url = getClassLoader().getResource(libraryName);
            if (url == null)
            {
                url = this.getClass().getClassLoader().getResource(libraryName);
            }
            if (url != null)
            {
                return true;
            }
        }
        return false;
    }

}
