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
package org.apache.myfaces.view.facelets.compiler;

import org.apache.myfaces.shared.util.ArrayUtils;
import org.apache.myfaces.shared.util.StringUtils;
import org.apache.myfaces.shared.util.WebConfigParamUtils;
import org.apache.myfaces.view.facelets.tag.AbstractTagLibrary;
import org.apache.myfaces.view.facelets.tag.TagLibrary;
import org.apache.myfaces.view.facelets.tag.composite.CompositeComponentResourceTagHandler;
import org.apache.myfaces.view.facelets.tag.composite.CompositeResouceWrapper;
import org.apache.myfaces.view.facelets.util.ParameterCheck;
import org.apache.myfaces.view.facelets.util.ReflectionUtil;

import javax.faces.FacesException;
import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;
import javax.faces.application.ViewHandler;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.view.facelets.ComponentConfig;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.Tag;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.apache.myfaces.config.element.facelets.FaceletBehaviorTag;
import org.apache.myfaces.config.element.facelets.FaceletComponentTag;
import org.apache.myfaces.config.element.facelets.FaceletConverterTag;
import org.apache.myfaces.config.element.facelets.FaceletFunction;
import org.apache.myfaces.config.element.facelets.FaceletHandlerTag;
import org.apache.myfaces.config.element.facelets.FaceletSourceTag;
import org.apache.myfaces.config.element.facelets.FaceletTag;
import org.apache.myfaces.config.element.facelets.FaceletTagLibrary;
import org.apache.myfaces.config.element.facelets.FaceletValidatorTag;

/**
 * Handles creating a {@link org.apache.myfaces.view.facelets.tag.TagLibrary TagLibrary}
 * from a {@link java.net.URL URL} source.
 * 
 * @author Jacob Hookom
 * @version $Id: TagLibraryConfig.java 1537798 2013-11-01 01:15:36Z lu4242 $
 */
public final class TagLibraryConfig
{

    //private final static String SUFFIX = ".taglib.xml";

    //protected final static Logger log = Logger.getLogger("facelets.compiler");
    protected final static Logger log = Logger.getLogger(TagLibraryConfig.class.getName());

    private static class TagLibraryImpl extends AbstractTagLibrary
    {
        private String _compositeLibraryName;
        
        private final ResourceHandler _resourceHandler;
        private Pattern _acceptPatterns;
        private String _extension;
        private String[] _defaultSuffixesArray;
        
        public TagLibraryImpl(FacesContext facesContext, String namespace)
        {
            super(namespace);
            _compositeLibraryName = null;
            _resourceHandler = facesContext.getApplication().getResourceHandler();
            ExternalContext externalContext = facesContext.getExternalContext();
            
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
        public boolean containsTagHandler(String ns, String localName)
        {
            boolean result = super.containsTagHandler(ns, localName);
            
            if (!result && _compositeLibraryName != null && containsNamespace(ns))
            {
                for (String defaultSuffix : _defaultSuffixesArray)
                {
                    String resourceName = localName + defaultSuffix;
                    if (handles(resourceName))
                    {
                        Resource compositeComponentResource = _resourceHandler.createResource(
                                resourceName, _compositeLibraryName);
                        
                        if (compositeComponentResource != null)
                        {
                            URL url = compositeComponentResource.getURL();
                            return (url != null);
                        }
                    }
                }
            }
            return result;
        }
        
        @Override
        public TagHandler createTagHandler(String ns, String localName,
                TagConfig tag) throws FacesException
        {
            TagHandler tagHandler = super.createTagHandler(ns, localName, tag);
            
            if (tagHandler == null && _compositeLibraryName != null && containsNamespace(ns))
            {
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
                        Resource compositeComponentResource = new CompositeResouceWrapper(
                            _resourceHandler.createResource(resourceName, _compositeLibraryName));
                        
                        if (compositeComponentResource != null)
                        {
                            ComponentConfig componentConfig = new ComponentConfigWrapper(tag,
                                    "javax.faces.NamingContainer", null);
                            
                            return new CompositeComponentResourceTagHandler(
                                    componentConfig, compositeComponentResource);
                        }
                    }
                }
            }
            return tagHandler;
        }

        public void setCompositeLibrary(String compositeLibraryName)
        {
            _compositeLibraryName = compositeLibraryName;
        }

        public void putConverter(String name, String id)
        {
            ParameterCheck.notNull("name", name);
            ParameterCheck.notNull("id", id);
            this.addConverter(name, id);
        }

        public void putConverter(String name, String id, Class<? extends TagHandler> handlerClass)
        {
            ParameterCheck.notNull("name", name);
            ParameterCheck.notNull("id", id);
            ParameterCheck.notNull("handlerClass", handlerClass);
            this.addConverter(name, id, handlerClass);
        }

        public void putValidator(String name, String id)
        {
            ParameterCheck.notNull("name", name);
            ParameterCheck.notNull("id", id);
            this.addValidator(name, id);
        }

        public void putValidator(String name, String id, Class<? extends TagHandler> handlerClass)
        {
            ParameterCheck.notNull("name", name);
            ParameterCheck.notNull("id", id);
            ParameterCheck.notNull("handlerClass", handlerClass);
            this.addValidator(name, id, handlerClass);
        }

        public void putTagHandler(String name, Class<? extends TagHandler> type)
        {
            ParameterCheck.notNull("name", name);
            ParameterCheck.notNull("type", type);
            this.addTagHandler(name, type);
        }
        
        public void putComponentFromResourceId(String name, String resourceId)
        {
            ParameterCheck.notNull("name", name);
            ParameterCheck.notNull("resourceId", resourceId);
            this.addComponentFromResourceId(name, resourceId);
        }

        public void putComponent(String name, String componentType, String rendererType)
        {
            ParameterCheck.notNull("name", name);
            ParameterCheck.notNull("componentType", componentType);
            this.addComponent(name, componentType, rendererType);
        }

        public void putComponent(String name, String componentType, String rendererType, 
                                 Class<? extends TagHandler> handlerClass)
        {
            ParameterCheck.notNull("name", name);
            ParameterCheck.notNull("componentType", componentType);
            ParameterCheck.notNull("handlerClass", handlerClass);
            this.addComponent(name, componentType, rendererType, handlerClass);
        }

        public void putUserTag(String name, URL source)
        {
            ParameterCheck.notNull("name", name);
            ParameterCheck.notNull("source", source);
            this.addUserTag(name, source);
        }

        public void putFunction(String name, Method method)
        {
            ParameterCheck.notNull("name", name);
            ParameterCheck.notNull("method", method);
            this.addFunction(name, method);
        }
        
        public void putBehavior(String name, String id)
        {
            ParameterCheck.notNull("name", name);
            ParameterCheck.notNull("id", id);
            this.addBehavior(name, id);
        }
        
        public void putBehavior(String name, String id, Class<? extends TagHandler> handlerClass)
        {
            ParameterCheck.notNull("name", name);
            ParameterCheck.notNull("id", id);
            ParameterCheck.notNull("handlerClass", handlerClass);
            this.addBehavior(name, id, handlerClass);
        }
    }
    
    private static class ComponentConfigWrapper implements ComponentConfig
    {

        protected final TagConfig parent;

        protected final String componentType;

        protected final String rendererType;

        public ComponentConfigWrapper(TagConfig parent, String componentType,
                String rendererType)
        {
            this.parent = parent;
            this.componentType = componentType;
            this.rendererType = rendererType;
        }

        public String getComponentType()
        {
            return this.componentType;
        }

        public String getRendererType()
        {
            return this.rendererType;
        }

        public FaceletHandler getNextHandler()
        {
            return this.parent.getNextHandler();
        }

        public Tag getTag()
        {
            return this.parent.getTag();
        }

        public String getTagId()
        {
            return this.parent.getTagId();
        }
    }    

    public TagLibraryConfig()
    {
        super();
    }
    
    public static TagLibrary create(FacesContext facesContext, FaceletTagLibrary faceletTagLibrary)
    {
        if (isNotEmpty(faceletTagLibrary.getLibraryClass()))
        {
            TagLibrary t = null;
            Class<?> type;
            try
            {
                type = createClass(TagLibrary.class, faceletTagLibrary.getLibraryClass());
                t = (TagLibrary) type.newInstance();
            }
            catch (Exception ex)
            {
                throw new FacesException("Cannot instantiate TagLibrary", ex);
            }
            // No further processing required.
            return t;
        }
        
        TagLibraryImpl impl = new TagLibraryImpl(facesContext, faceletTagLibrary.getNamespace());
        
        impl.setCompositeLibrary(faceletTagLibrary.getCompositeLibraryName());
        
        for (FaceletFunction ff : faceletTagLibrary.getFunctions())
        {
            try
            {
                Class<?> functionClass = createClass(Object.class, ff.getFunctionClass());
                impl.putFunction(ff.getFunctionName(), createMethod(functionClass, ff.getFunctionSignature()));
            }
            catch (Exception ex)
            {
                throw new FacesException("Cannot instantiate Function Class", ex);
            }
        }
        
        for (FaceletTag ft : faceletTagLibrary.getTags())
        {
            try
            {
                if (ft.isHandlerTag())
                {
                    FaceletHandlerTag tag = (FaceletHandlerTag) ft.getTagDefinition();
                    if (tag.getHandlerClass() != null)
                    {
                        Class<? extends TagHandler> handlerClass = 
                            createClass(TagHandler.class, tag.getHandlerClass());
                        impl.putTagHandler(ft.getName(), handlerClass);
                    }
                }
                else if (ft.isComponentTag())
                {
                    FaceletComponentTag tag = (FaceletComponentTag) ft.getTagDefinition();
                    if (tag.getHandlerClass() != null)
                    {
                        Class<? extends TagHandler> handlerClass = 
                            createClass(TagHandler.class, tag.getHandlerClass());
                        impl.putComponent(ft.getName(), tag.getComponentType(), tag.getRendererType(), handlerClass);
                    }
                    else if (tag.getResourceId() != null)
                    {
                        impl.putComponentFromResourceId(ft.getName(), tag.getResourceId());
                    }
                    else 
                    {
                        impl.putComponent(ft.getName(), tag.getComponentType(), tag.getRendererType());
                    }
                }
                else if (ft.isSourceTag())
                {
                    FaceletSourceTag tag = (FaceletSourceTag) ft.getTagDefinition();
                    impl.putUserTag(ft.getName(), new URL(tag.getSource()));
                }
                else if (ft.isConverterTag())
                {
                    FaceletConverterTag tag = (FaceletConverterTag) ft.getTagDefinition();
                    if (tag.getHandlerClass() != null)
                    {
                        Class<? extends TagHandler> handlerClass = 
                            createClass(TagHandler.class, tag.getHandlerClass());
                        impl.putConverter(ft.getName(), tag.getConverterId(), handlerClass);
                    }
                    else
                    {
                        impl.putConverter(ft.getName(), tag.getConverterId());
                    }
                }
                else if (ft.isValidatorTag())
                {
                    FaceletValidatorTag tag = (FaceletValidatorTag) ft.getTagDefinition();
                    if (tag.getHandlerClass() != null)
                    {
                        Class<? extends TagHandler> handlerClass = 
                            createClass(TagHandler.class, tag.getHandlerClass());
                        impl.putValidator(ft.getName(), tag.getValidatorId(), handlerClass);
                    }
                    else
                    {
                        impl.putValidator(ft.getName(), tag.getValidatorId());
                    }
                }
                else if (ft.isBehaviorTag())
                {
                    FaceletBehaviorTag tag = (FaceletBehaviorTag) ft.getTagDefinition();
                    if (tag.getHandlerClass() != null)
                    {
                        Class<? extends TagHandler> handlerClass = 
                            createClass(TagHandler.class, tag.getHandlerClass());
                        impl.putBehavior(ft.getName(), tag.getBehaviorId(), handlerClass);
                    }
                    else
                    {
                        impl.putBehavior(ft.getName(), tag.getBehaviorId());
                    }
                }
            }
            catch (Exception ex)
            {
                throw new FacesException("Cannot instantiate Tag "+ft.getName()+" from namespace "+
                    faceletTagLibrary.getNamespace(), ex);
            }
        }
        return impl;
    }
    
    @SuppressWarnings("unchecked")
    private static <T> Class<? extends T> createClass(Class<T> type, String name) throws Exception
    {
        Class<? extends T> factory = (Class<? extends T>)ReflectionUtil.forName(name);
        if (!type.isAssignableFrom(factory))
        {
            throw new Exception(name + " must be an instance of " + type.getName());
        }
        return factory;
    }

    private static Method createMethod(Class<?> type, String s) throws Exception
    {
        int pos = s.indexOf(' ');
        if (pos == -1)
        {
            throw new Exception("Must Provide Return Type: " + s);
        }
        else
        {
            int pos2 = s.indexOf('(', pos + 1);
            if (pos2 == -1)
            {
                throw new Exception("Must provide a method name, followed by '(': " + s);
            }
            else
            {
                String mn = s.substring(pos + 1, pos2).trim();
                pos = s.indexOf(')', pos2 + 1);
                if (pos == -1)
                {
                    throw new Exception("Must close parentheses, ')' missing: " + s);
                }
                else
                {
                    String[] ps = s.substring(pos2 + 1, pos).trim().split(",");
                    Class<?>[] pc;
                    if (ps.length == 1 && "".equals(ps[0]))
                    {
                        pc = new Class[0];
                    }
                    else
                    {
                        pc = new Class[ps.length];
                        for (int i = 0; i < pc.length; i++)
                        {
                            pc[i] = ReflectionUtil.forName(ps[i].trim());
                        }
                    }
                    try
                    {
                        return type.getMethod(mn, pc);
                    }
                    catch (NoSuchMethodException e)
                    {
                        throw new Exception("No Function Found on type: " + type.getName() + " with signature: "
                                + s);
                    }

                }

            }
        }
    }

    private static boolean isNotEmpty(String value)
    {
        return value != null && value.length() > 0;
    }

}
