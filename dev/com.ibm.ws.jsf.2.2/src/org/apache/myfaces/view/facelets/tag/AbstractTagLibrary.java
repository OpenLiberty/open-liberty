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
package org.apache.myfaces.view.facelets.tag;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;
import javax.faces.context.FacesContext;
import javax.faces.view.facelets.BehaviorConfig;
import javax.faces.view.facelets.BehaviorHandler;
import javax.faces.view.facelets.ComponentConfig;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.ConverterConfig;
import javax.faces.view.facelets.ConverterHandler;
import javax.faces.view.facelets.FaceletException;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.Tag;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;
import javax.faces.view.facelets.ValidatorConfig;
import javax.faces.view.facelets.ValidatorHandler;
import org.apache.myfaces.shared.config.MyfacesConfig;
import org.apache.myfaces.view.facelets.tag.composite.CompositeComponentResourceTagHandler;
import org.apache.myfaces.view.facelets.tag.composite.CompositeResouceWrapper;

/**
 * Base class for defining TagLibraries in Java
 * 
 * @author Jacob Hookom
 * @version $Id: AbstractTagLibrary.java 1539436 2013-11-06 19:35:31Z lu4242 $
 */
public abstract class AbstractTagLibrary implements TagLibrary
{
    private final Map<String, TagHandlerFactory> _factories;

    private final Map<String, Method> _functions;

    private final String _namespace;
    private final String _aliasNamespace;
    private Boolean _strictJsf2FaceletsCompatibility;

    public AbstractTagLibrary(String namespace, String aliasNamespace)
    {
        _namespace = namespace;
        _aliasNamespace = aliasNamespace;
        _factories = new HashMap<String, TagHandlerFactory>();
        _functions = new HashMap<String, Method>();
    }
    
    public AbstractTagLibrary(String namespace)
    {
        this(namespace,null);
    }
    

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.myfaces.view.facelets.tag.TagLibrary#containsNamespace(java.lang.String)
     */
    public boolean containsNamespace(String ns)
    {
        return _namespace.equals(ns) || (_aliasNamespace != null && _aliasNamespace.equals(ns));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.myfaces.view.facelets.tag.TagLibrary#containsTagHandler(java.lang.String, java.lang.String)
     */
    public boolean containsTagHandler(String ns, String localName)
    {
        return containsNamespace(ns) && _factories.containsKey(localName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.myfaces.view.facelets.tag.TagLibrary#createTagHandler(java.lang.String, java.lang.String,
     * org.apache.myfaces.view.facelets.tag.TagConfig)
     */
    public TagHandler createTagHandler(String ns, String localName, TagConfig tag) throws FacesException
    {
        if (containsNamespace(ns))
        {
            TagHandlerFactory f = _factories.get(localName);
            if (f != null)
            {
                return f.createHandler(tag);
            }
        }
        
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.myfaces.view.facelets.tag.TagLibrary#containsFunction(java.lang.String, java.lang.String)
     */
    public boolean containsFunction(String ns, String name)
    {
        return containsNamespace(ns) && _functions.containsKey(name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.myfaces.view.facelets.tag.TagLibrary#createFunction(java.lang.String, java.lang.String)
     */
    public Method createFunction(String ns, String name)
    {
        return containsNamespace(ns) ? _functions.get(name) : null;
    }

    public String getNamespace()
    {
        return _namespace;
    }

    /**
     * Add a ComponentHandler with the specified componentType and rendererType, aliased by the tag name.
     * 
     * @see ComponentHandler
     * @see javax.faces.application.Application#createComponent(java.lang.String)
     * @param name
     *            name to use, "foo" would be &lt;my:foo />
     * @param componentType
     *            componentType to use
     * @param rendererType
     *            rendererType to use
     */
    protected final void addComponent(String name, String componentType, String rendererType)
    {
        _factories.put(name, new ComponentHandlerFactory(componentType, rendererType));
    }

    /**
     * Add a ComponentHandler with the specified componentType and rendererType, aliased by the tag name. The Facelet
     * will be compiled with the specified HandlerType (which must extend AbstractComponentHandler).
     * 
     * @see AbstractComponentHandler
     * @param name
     *            name to use, "foo" would be &lt;my:foo />
     * @param componentType
     *            componentType to use
     * @param rendererType
     *            rendererType to use
     * @param handlerType
     *            a Class that extends AbstractComponentHandler
     */
    protected final void addComponent(String name, String componentType, String rendererType, 
                                      Class<? extends TagHandler> handlerType)
    {
        _factories.put(name, new UserComponentHandlerFactory(componentType, rendererType, handlerType));
    }
    
    protected final void addComponentFromResourceId(String name, String resourceId)
    {
        _factories.put(name, new UserComponentFromResourceIdHandlerFactory(resourceId));
    }

    /**
     * Add a ConvertHandler for the specified converterId
     * 
     * @see javax.faces.view.facelets.ConverterHandler
     * @see javax.faces.application.Application#createConverter(java.lang.String)
     * @param name
     *            name to use, "foo" would be &lt;my:foo />
     * @param converterId
     *            id to pass to Application instance
     */
    protected final void addConverter(String name, String converterId)
    {
        _factories.put(name, new ConverterHandlerFactory(converterId));
    }

    /**
     * Add a ConvertHandler for the specified converterId of a TagHandler type
     * 
     * @see javax.faces.view.facelets.ConverterHandler
     * @see javax.faces.view.facelets.ConverterConfig
     * @see javax.faces.application.Application#createConverter(java.lang.String)
     * @param name
     *            name to use, "foo" would be &lt;my:foo />
     * @param converterId
     *            id to pass to Application instance
     * @param type
     *            TagHandler type that takes in a ConverterConfig
     */
    protected final void addConverter(String name, String converterId, Class<? extends TagHandler> type)
    {
        _factories.put(name, new UserConverterHandlerFactory(converterId, type));
    }

    /**
     * Add a ValidateHandler for the specified validatorId
     * 
     * @see javax.faces.view.facelets.ValidatorHandler
     * @see javax.faces.application.Application#createValidator(java.lang.String)
     * @param name
     *            name to use, "foo" would be &lt;my:foo />
     * @param validatorId
     *            id to pass to Application instance
     */
    protected final void addValidator(String name, String validatorId)
    {
        _factories.put(name, new ValidatorHandlerFactory(validatorId));
    }

    /**
     * Add a ValidateHandler for the specified validatorId
     * 
     * @see javax.faces.view.facelets.ValidatorHandler
     * @see javax.faces.view.facelets.ValidatorConfig
     * @see javax.faces.application.Application#createValidator(java.lang.String)
     * @param name
     *            name to use, "foo" would be &lt;my:foo />
     * @param validatorId
     *            id to pass to Application instance
     * @param type
     *            TagHandler type that takes in a ValidatorConfig
     */
    protected final void addValidator(String name, String validatorId, Class<? extends TagHandler> type)
    {
        _factories.put(name, new UserValidatorHandlerFactory(validatorId, type));
    }

    /**
     * Use the specified HandlerType in compiling Facelets. HandlerType must extend TagHandler.
     * 
     * @see TagHandler
     * @param name
     *            name to use, "foo" would be &lt;my:foo />
     * @param handlerType
     *            must extend TagHandler
     */
    protected final void addTagHandler(String name, Class<? extends TagHandler> handlerType)
    {
        _factories.put(name, new HandlerFactory(handlerType));
    }

    /**
     * Add a UserTagHandler specified a the URL source.
     * 
     * @see UserTagHandler
     * @param name
     *            name to use, "foo" would be &lt;my:foo />
     * @param source
     *            source where the Facelet (Tag) source is
     */
    protected final void addUserTag(String name, URL source)
    {
        if (_strictJsf2FaceletsCompatibility == null)
        {
            MyfacesConfig config = MyfacesConfig.getCurrentInstance(
                    FacesContext.getCurrentInstance().getExternalContext());

            _strictJsf2FaceletsCompatibility = config.isStrictJsf2FaceletsCompatibility();
        }
        if (Boolean.TRUE.equals(_strictJsf2FaceletsCompatibility))
        {
            _factories.put(name, new LegacyUserTagFactory(source));
        }
        else
        {
            _factories.put(name, new UserTagFactory(source));
        }
    }

    /**
     * Add a Method to be used as a Function at Compilation.
     * 
     * @see javax.el.FunctionMapper
     * 
     * @param name
     *            (suffix) of function name
     * @param method
     *            method instance
     */
    protected final void addFunction(String name, Method method)
    {
        _functions.put(name, method);
    }
    
    /**
     * @since 2.0
     * @param behaviorId
     * @param behaviorClass
     */
    protected final void addBehavior(String name, String behaviorId)
    {
        _factories.put(name, new BehaviorHandlerFactory(behaviorId));
    }
    
    /**
     * @since 2.0
     * @param behaviorId
     * @param behaviorClass
     * @param handlerType
     */
    protected final void addBehavior(String name, String behaviorId,
            Class<? extends TagHandler> handlerType)
    {
        _factories.put(name, new UserBehaviorHandlerFactory(behaviorId,handlerType));
    }    

    private static class ValidatorConfigWrapper implements ValidatorConfig
    {

        private final TagConfig parent;
        private final String validatorId;

        public ValidatorConfigWrapper(TagConfig parent, String validatorId)
        {
            this.parent = parent;
            this.validatorId = validatorId;
        }

        public String getValidatorId()
        {
            return this.validatorId;
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

    private static class ConverterConfigWrapper implements ConverterConfig
    {
        private final TagConfig parent;
        private final String converterId;

        public ConverterConfigWrapper(TagConfig parent, String converterId)
        {
            this.parent = parent;
            this.converterId = converterId;
        }

        public String getConverterId()
        {
            return this.converterId;
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

    private static class HandlerFactory implements TagHandlerFactory
    {
        private final static Class<?>[] CONSTRUCTOR_SIG = new Class[]{TagConfig.class};

        protected final Class<? extends TagHandler> handlerType;

        public HandlerFactory(Class<? extends TagHandler> handlerType)
        {
            this.handlerType = handlerType;
        }

        public TagHandler createHandler(TagConfig cfg) throws FacesException, ELException
        {
            try
            {
                return handlerType.getConstructor(CONSTRUCTOR_SIG).newInstance(new Object[] { cfg });
            }
            catch (InvocationTargetException ite)
            {
                Throwable t = ite.getCause();
                if (t instanceof FacesException)
                {
                    throw (FacesException) t;
                }
                else if (t instanceof ELException)
                {
                    throw (ELException) t;
                }
                else
                {
                    throw new FacesException("Error Instantiating: " + handlerType.getName(), t);
                }
            }
            catch (Exception e)
            {
                throw new FacesException("Error Instantiating: " + handlerType.getName(), e);
            }
        }
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

    private static class UserTagFactory implements TagHandlerFactory
    {
        protected final URL location;

        public UserTagFactory(URL location)
        {
            this.location = location;
        }

        public TagHandler createHandler(TagConfig cfg) throws FacesException, ELException
        {
            return new UserTagHandler(cfg, this.location);
        }
    }
    
    private static class LegacyUserTagFactory implements TagHandlerFactory
    {
        protected final URL location;

        public LegacyUserTagFactory(URL location)
        {
            this.location = location;
        }

        public TagHandler createHandler(TagConfig cfg) throws FacesException, ELException
        {
            return new LegacyUserTagHandler(cfg, this.location);
        }
    }

    private static class ComponentHandlerFactory implements TagHandlerFactory
    {

        protected final String componentType;

        protected final String renderType;

        /**
         * @param handlerType
         */
        public ComponentHandlerFactory(String componentType, String renderType)
        {
            this.componentType = componentType;
            this.renderType = renderType;
        }

        public TagHandler createHandler(TagConfig cfg) throws FacesException, ELException
        {
            ComponentConfig ccfg = new ComponentConfigWrapper(cfg, this.componentType, this.renderType);
            return new ComponentHandler(ccfg);
        }
    }

    private static class UserComponentHandlerFactory implements TagHandlerFactory
    {

        private final static Class<?>[] CONS_SIG = new Class[] { ComponentConfig.class };

        protected final String componentType;

        protected final String renderType;

        protected final Class<? extends TagHandler> type;

        protected final Constructor<? extends TagHandler> constructor;

        /**
         * @param handlerType
         */
        public UserComponentHandlerFactory(String componentType, String renderType, Class<? extends TagHandler> type)
        {
            this.componentType = componentType;
            this.renderType = renderType;
            this.type = type;
            try
            {
                this.constructor = this.type.getConstructor(CONS_SIG);
            }
            catch (Exception e)
            {
                throw new FaceletException("Must have a Constructor that takes in a ComponentConfig", e);
            }
        }

        public TagHandler createHandler(TagConfig cfg) throws FacesException, ELException
        {
            try
            {
                ComponentConfig ccfg = new ComponentConfigWrapper(cfg, componentType, renderType);
                return constructor.newInstance(new Object[] { ccfg });
            }
            catch (InvocationTargetException e)
            {
                throw new FaceletException(e.getCause().getMessage(), e.getCause().getCause());
            }
            catch (Exception e)
            {
                throw new FaceletException("Error Instantiating ComponentHandler: " + this.type.getName(), e);
            }
        }
    }

    private static class ValidatorHandlerFactory implements TagHandlerFactory
    {

        protected final String validatorId;

        public ValidatorHandlerFactory(String validatorId)
        {
            this.validatorId = validatorId;
        }

        public TagHandler createHandler(TagConfig cfg) throws FacesException, ELException
        {
            return new ValidatorHandler(new ValidatorConfigWrapper(cfg, this.validatorId));
        }
    }

    private static class ConverterHandlerFactory implements TagHandlerFactory
    {

        protected final String converterId;

        public ConverterHandlerFactory(String converterId)
        {
            this.converterId = converterId;
        }

        public TagHandler createHandler(TagConfig cfg) throws FacesException, ELException
        {
            return new ConverterHandler(new ConverterConfigWrapper(cfg, this.converterId));
        }
    }

    private static class UserConverterHandlerFactory implements TagHandlerFactory
    {
        private final static Class<?>[] CONS_SIG = new Class[] { ConverterConfig.class };

        protected final String converterId;

        protected final Class<? extends TagHandler> type;

        protected final Constructor<? extends TagHandler> constructor;

        public UserConverterHandlerFactory(String converterId, Class<? extends TagHandler> type)
        {
            this.converterId = converterId;
            this.type = type;
            try
            {
                this.constructor = this.type.getConstructor(CONS_SIG);
            }
            catch (Exception e)
            {
                throw new FaceletException("Must have a Constructor that takes in a ConverterConfig", e);
            }
        }

        public TagHandler createHandler(TagConfig cfg) throws FacesException, ELException
        {
            try
            {
                ConverterConfig ccfg = new ConverterConfigWrapper(cfg, converterId);
                return constructor.newInstance(new Object[] { ccfg });
            }
            catch (InvocationTargetException e)
            {
                throw new FaceletException(e.getCause().getMessage(), e.getCause().getCause());
            }
            catch (Exception e)
            {
                throw new FaceletException("Error Instantiating ConverterHandler: " + type.getName(), e);
            }
        }
    }

    private static class UserValidatorHandlerFactory implements TagHandlerFactory
    {
        private final static Class<?>[] CONS_SIG = new Class[] { ValidatorConfig.class };

        protected final String validatorId;

        protected final Class<? extends TagHandler> type;

        protected final Constructor<? extends TagHandler> constructor;

        public UserValidatorHandlerFactory(String validatorId, Class<? extends TagHandler> type)
        {
            this.validatorId = validatorId;
            this.type = type;
            try
            {
                this.constructor = this.type.getConstructor(CONS_SIG);
            }
            catch (Exception e)
            {
                throw new FaceletException("Must have a Constructor that takes in a ConverterConfig", e);
            }
        }

        public TagHandler createHandler(TagConfig cfg) throws FacesException, ELException
        {
            try
            {
                ValidatorConfig ccfg = new ValidatorConfigWrapper(cfg, validatorId);
                return constructor.newInstance(new Object[] { ccfg });
            }
            catch (InvocationTargetException e)
            {
                throw new FaceletException(e.getCause().getMessage(), e.getCause().getCause());
            }
            catch (Exception e)
            {
                throw new FaceletException("Error Instantiating ValidatorHandler: " + type.getName(), e);
            }
        }
    }
    
    private static class BehaviorConfigWrapper implements BehaviorConfig
    {
        protected final TagConfig parent;

        protected final String behaviorId;

        public BehaviorConfigWrapper(TagConfig parent, String behaviorId)
        {
            this.parent = parent;
            this.behaviorId = behaviorId;
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

        public String getBehaviorId()
        {
            return this.behaviorId;
        }
    }
    
    private static class BehaviorHandlerFactory implements TagHandlerFactory
    {
        protected final String behaviorId;
               
        public BehaviorHandlerFactory(String behaviorId)
        {
            super();
            this.behaviorId = behaviorId;
        }

        public TagHandler createHandler(TagConfig cfg) throws FacesException, ELException
        {
            BehaviorConfig bcfg = new BehaviorConfigWrapper(cfg,this.behaviorId);
            return new BehaviorHandler(bcfg);
        }
    }

    private static class UserBehaviorHandlerFactory implements TagHandlerFactory
    {
        private final static Class<?>[] CONS_SIG = new Class[] { BehaviorConfig.class };

        protected final String behaviorId;

        protected final Class<? extends TagHandler> type;

        protected final Constructor<? extends TagHandler> constructor;

        public UserBehaviorHandlerFactory(String behaviorId, Class<? extends TagHandler> type)
        {
            this.behaviorId = behaviorId;
            this.type = type;
            try
            {
                this.constructor = this.type.getConstructor(CONS_SIG);
            }
            catch (Exception e)
            {
                throw new FaceletException("Must have a Constructor that takes in a BehaviorConfig", e);
            }
        }

        public TagHandler createHandler(TagConfig cfg) throws FacesException, ELException
        {
            try
            {
                BehaviorConfig bcfg = new BehaviorConfigWrapper(cfg,this.behaviorId);
                return constructor.newInstance(new Object[] { bcfg });
            }
            catch (InvocationTargetException e)
            {
                throw new FaceletException(e.getCause().getMessage(), e.getCause().getCause());
            }
            catch (Exception e)
            {
                throw new FaceletException("Error Instantiating BehaviorHandler: " + this.type.getName(), e);
            }
        }
    }
    
    private static class UserComponentFromResourceIdHandlerFactory implements TagHandlerFactory
    {
        protected final String resourceId;
        
        public UserComponentFromResourceIdHandlerFactory(String resourceId)
        {
            this.resourceId = resourceId;
        }

        public TagHandler createHandler(TagConfig cfg) throws FacesException, ELException
        {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            ResourceHandler resourceHandler = facesContext.getApplication().getResourceHandler();
            Resource compositeComponentResourceWrapped = resourceHandler.createResourceFromId(resourceId);
            if (compositeComponentResourceWrapped != null)
            {
                Resource compositeComponentResource
                        = new CompositeResouceWrapper(compositeComponentResourceWrapped);
                ComponentConfig componentConfig = new ComponentConfigWrapper(cfg,
                        "javax.faces.NamingContainer", null);

                return new CompositeComponentResourceTagHandler(componentConfig,
                                                                compositeComponentResource);
            }
            else
            {
                throw new FaceletException(
                    "Error Instantiating Component from <resource-id> declaration: " + this.resourceId);
            }
        }
    }
}
