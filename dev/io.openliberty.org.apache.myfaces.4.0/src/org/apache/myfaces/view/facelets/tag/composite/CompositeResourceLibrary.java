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
package org.apache.myfaces.view.facelets.tag.composite;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.regex.Pattern;

import jakarta.faces.FacesException;
import jakarta.faces.application.Resource;
import jakarta.faces.application.ResourceHandler;
import jakarta.faces.application.ViewHandler;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.facelets.ComponentConfig;
import jakarta.faces.view.facelets.FaceletHandler;
import jakarta.faces.view.facelets.Tag;
import jakarta.faces.view.facelets.TagConfig;
import jakarta.faces.view.facelets.TagHandler;

import org.apache.myfaces.util.lang.ArrayUtils;
import org.apache.myfaces.util.lang.StringUtils;
import org.apache.myfaces.util.WebConfigParamUtils;
import org.apache.myfaces.view.facelets.tag.TagLibrary;

/**
 * This class create composite component tag handlers for "http://java.sun.com/jsf/composite/"
 * namespace. Note that the class that create composite component tag handlers using its own 
 * namespace defined in facelet taglib .xml file see TagLibraryConfig.TagLibraryImpl
 * 
 * @author Leonardo Uribe (latest modification by $Author$)
 * @version $Revision$ $Date$
 */
public class CompositeResourceLibrary implements TagLibrary
{
    public final static String NAMESPACE_PREFIX = "jakarta.faces.composite";
    public final static String JCP_NAMESPACE_PREFIX = "http://xmlns.jcp.org/jsf/composite/";
    public final static String SUN_NAMESPACE_PREFIX = "http://java.sun.com/jsf/composite/";
    
    private final ResourceHandler _resourceHandler;
    private Pattern _acceptPatterns;
    private String _extension;
    private String[] _defaultSuffixesArray;
    private String _namespacePrefix;
    
    public CompositeResourceLibrary(FacesContext facesContext, String namespacePrefix)
    {
        super();
        _namespacePrefix = namespacePrefix;
        _resourceHandler = facesContext.getApplication().getResourceHandler();

        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        
        _acceptPatterns = loadAcceptPattern(externalContext);

        _extension = loadFaceletExtension(externalContext);
        
        String defaultSuffixes = WebConfigParamUtils.getStringInitParameter(externalContext,
                ViewHandler.DEFAULT_SUFFIX_PARAM_NAME, ViewHandler.DEFAULT_SUFFIX );
        
        _defaultSuffixesArray = StringUtils.splitShortString(defaultSuffixes, ' ');
        
        boolean faceletsExtensionFound = false;
        for (String ext : _defaultSuffixesArray)
        {
            if (_extension.equals(ext))
            {
                faceletsExtensionFound = true;
                break;
            }
        }
        if (!faceletsExtensionFound)
        {
            _defaultSuffixesArray = (String[]) ArrayUtils.concat(_defaultSuffixesArray, new String[]{_extension});
        }
    }
    
    /**
     * Load and compile a regular expression pattern built from the Facelet view mapping parameters.
     * 
     * @param context
     *            the application's external context
     * 
     * @return the compiled regular expression
     */
    private Pattern loadAcceptPattern(ExternalContext context)
    {
        assert context != null;

        String mappings = context.getInitParameter(ViewHandler.FACELETS_VIEW_MAPPINGS_PARAM_NAME);
        if (mappings == null)
        {
            return null;
        }

        // Make sure the mappings contain something
        mappings = mappings.trim();
        if (mappings.length() == 0)
        {
            return null;
        }

        return Pattern.compile(toRegex(mappings));
    }

    private String loadFaceletExtension(ExternalContext context)
    {
        assert context != null;

        String suffix = context.getInitParameter(ViewHandler.FACELETS_SUFFIX_PARAM_NAME);
        if (suffix == null)
        {
            suffix = ViewHandler.DEFAULT_FACELETS_SUFFIX;
        }
        else
        {
            suffix = suffix.trim();
            if (suffix.length() == 0)
            {
                suffix = ViewHandler.DEFAULT_FACELETS_SUFFIX;
            }
        }

        return suffix;
    }
    
    /**
     * Convert the specified mapping string to an equivalent regular expression.
     * 
     * @param mappings
     *            le mapping string
     * 
     * @return an uncompiled regular expression representing the mappings
     */
    private String toRegex(String mappings)
    {
        assert mappings != null;

        // Get rid of spaces
        mappings = mappings.replaceAll("\\s", "");

        // Escape '.'
        mappings = mappings.replaceAll("\\.", "\\\\.");

        // Change '*' to '.*' to represent any match
        mappings = mappings.replaceAll("\\*", ".*");

        // Split the mappings by changing ';' to '|'
        mappings = mappings.replaceAll(";", "|");

        return mappings;
    }
    
    public boolean handles(String resourceName)
    {
        if (resourceName == null)
        {
            return false;
        }
        // Check extension first as it's faster than mappings
        if (resourceName.endsWith(_extension))
        {
            // If the extension matches, it's a Facelet viewId.
            return true;
        }

        // Otherwise, try to match the view identifier with the facelet mappings
        return _acceptPatterns != null && _acceptPatterns.matcher(resourceName).matches();
    }

    @Override
    public boolean containsFunction(String ns, String name)
    {
        // Composite component tag library does not suport functions
        return false;
    }

    @Override
    public boolean containsNamespace(String ns)
    {
        if (ns != null && ns.startsWith(_namespacePrefix))
        {
            if (ns.length() > _namespacePrefix.length())
            {
                String libraryName = ns.substring(_namespacePrefix.length());
                return _resourceHandler.libraryExists(libraryName);
            }
        }        
        return false;
    }

    @Override
    public boolean containsTagHandler(String ns, String localName)
    {
        if (ns != null && ns.startsWith(_namespacePrefix))
        {
            if (ns.length() > _namespacePrefix.length())
            {
                String libraryName = ns.substring(_namespacePrefix.length());
                
                for (String defaultSuffix : _defaultSuffixesArray)
                {
                    String resourceName = localName + defaultSuffix;
                    if (handles(resourceName))
                    {
                        Resource compositeComponentResource = 
                            _resourceHandler.createResource(resourceName, libraryName);
                        if (compositeComponentResource != null)
                        {
                            URL url = compositeComponentResource.getURL();
                            return (url != null);
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public Method createFunction(String ns, String name)
    {
        // Composite component tag library does not suport functions
        return null;
    }

    @Override
    public TagHandler createTagHandler(String ns, String localName, TagConfig tag) throws FacesException
    {
        if (ns != null && ns.startsWith(_namespacePrefix))
        {
            if (ns.length() > _namespacePrefix.length())
            {
                String libraryName = ns.substring(_namespacePrefix.length());
                for (String defaultSuffix : _defaultSuffixesArray)
                {
                    String resourceName = localName + defaultSuffix;
                    if (handles(resourceName))
                    {
                        // MYFACES-3308 If a composite component exists, it requires to 
                        // be always resolved. In other words, it should always exists a default.
                        // The call here for resourceHandler.createResource, just try to get
                        // the Resource and if it does not exists, it just returns null.
                        // The intention of this code is just create an instance and pass to
                        // CompositeComponentResourceTagHandler. Then, its values 
                        // (resourceName, libraryName) will be used to derive the real instance
                        // to use in a view, based on the locale used.
                        Resource compositeComponentResourceWrapped
                                = _resourceHandler.createResource(resourceName, libraryName);
                        if (compositeComponentResourceWrapped != null)
                        {
                            Resource compositeComponentResource
                                    = new CompositeResouceWrapper(compositeComponentResourceWrapped);
                            ComponentConfig componentConfig = new ComponentConfigWrapper(tag,
                                    "jakarta.faces.NamingContainer", null);

                            return new CompositeComponentResourceTagHandler(componentConfig,
                                                                            compositeComponentResource);
                        }
                    }
                }
            }
        }
        return null;
    }

    private static class ComponentConfigWrapper implements ComponentConfig
    {
        protected final TagConfig parent;
        protected final String componentType;
        protected final String rendererType;

        public ComponentConfigWrapper(TagConfig parent, String componentType, String rendererType)
        {
            this.parent = parent;
            this.componentType = componentType;
            this.rendererType = rendererType;
        }

        @Override
        public String getComponentType()
        {
            return this.componentType;
        }

        @Override
        public String getRendererType()
        {
            return this.rendererType;
        }

        @Override
        public FaceletHandler getNextHandler()
        {
            return this.parent.getNextHandler();
        }

        @Override
        public Tag getTag()
        {
            return this.parent.getTag();
        }

        @Override
        public String getTagId()
        {
            return this.parent.getTagId();
        }
    }
}
