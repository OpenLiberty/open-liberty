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
package org.apache.myfaces.application;

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.el.CompositeELResolver;
import jakarta.el.ELContext;
import jakarta.el.ELContextListener;
import jakarta.el.ELException;
import jakarta.el.ELResolver;
import jakarta.el.ExpressionFactory;
import jakarta.el.ValueExpression;
import jakarta.faces.FacesException;
import jakarta.faces.FacesWrapper;
import jakarta.faces.application.Application;
import jakarta.faces.application.NavigationHandler;
import jakarta.faces.application.ProjectStage;
import jakarta.faces.application.Resource;
import jakarta.faces.application.ResourceDependencies;
import jakarta.faces.application.ResourceDependency;
import jakarta.faces.application.ResourceHandler;
import jakarta.faces.application.StateManager;
import jakarta.faces.application.ViewHandler;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIComponentBase;
import jakarta.faces.component.UINamingContainer;
import jakarta.faces.component.UIOutput;
import jakarta.faces.component.UIViewRoot;
import jakarta.faces.component.behavior.Behavior;
import jakarta.faces.component.behavior.ClientBehavior;
import jakarta.faces.component.behavior.ClientBehaviorBase;
import jakarta.faces.component.behavior.FacesBehavior;
import jakarta.faces.component.search.SearchExpressionHandler;
import jakarta.faces.component.search.SearchKeywordResolver;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.DateTimeConverter;
import jakarta.faces.convert.FacesConverter;
import jakarta.faces.event.ActionListener;
import jakarta.faces.event.ComponentSystemEventListener;
import jakarta.faces.event.ListenerFor;
import jakarta.faces.event.ListenersFor;
import jakarta.faces.event.SystemEvent;
import jakarta.faces.event.SystemEventListener;
import jakarta.faces.flow.FlowHandler;
import jakarta.faces.render.ClientBehaviorRenderer;
import jakarta.faces.render.RenderKit;
import jakarta.faces.render.Renderer;
import jakarta.faces.render.RendererWrapper;
import jakarta.faces.validator.FacesValidator;
import jakarta.faces.validator.Validator;
import jakarta.faces.view.ViewDeclarationLanguage;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.myfaces.cdi.wrapper.FacesBehaviorCDIWrapper;
import org.apache.myfaces.cdi.wrapper.FacesClientBehaviorCDIWrapper;
import org.apache.myfaces.cdi.wrapper.FacesConverterCDIWrapper;
import org.apache.myfaces.cdi.wrapper.FacesValidatorCDIWrapper;
import org.apache.myfaces.component.search.AllSearchKeywordResolver;
import org.apache.myfaces.component.search.ChildSearchKeywordResolver;
import org.apache.myfaces.component.search.CompositeComponentParentSearchKeywordResolver;
import org.apache.myfaces.component.search.CompositeSearchKeywordResolver;
import org.apache.myfaces.component.search.FormSearchKeywordResolver;
import org.apache.myfaces.component.search.IdSearchKeywordResolver;
import org.apache.myfaces.component.search.NamingContainerSearchKeywordResolver;
import org.apache.myfaces.component.search.NextSearchKeywordResolver;
import org.apache.myfaces.component.search.NoneSearchKeywordResolver;
import org.apache.myfaces.component.search.ParentSearchKeywordResolver;
import org.apache.myfaces.component.search.PreviousSearchKeywordResolver;
import org.apache.myfaces.component.search.RootSearchKeywordResolver;
import org.apache.myfaces.component.search.SearchExpressionHandlerImpl;
import org.apache.myfaces.component.search.ThisSearchKeywordResolver;
import org.apache.myfaces.config.RuntimeConfig;
import org.apache.myfaces.config.element.Property;
import org.apache.myfaces.config.element.ResourceBundle;
import org.apache.myfaces.config.webparameters.MyfacesConfig;
import org.apache.myfaces.context.RequestViewContext;
import org.apache.myfaces.context.RequestViewMetadata;
import org.apache.myfaces.el.DefaultELResolverBuilder;
import org.apache.myfaces.flow.FlowHandlerImpl;
import org.apache.myfaces.lifecycle.LifecycleImpl;
import org.apache.myfaces.core.api.shared.lang.LambdaPropertyDescriptor;
import org.apache.myfaces.core.api.shared.lang.PropertyDescriptorUtils;
import org.apache.myfaces.core.api.shared.lang.PropertyDescriptorWrapper;
import org.apache.myfaces.core.api.shared.lang.Assert;
import org.apache.myfaces.util.lang.ClassUtils;
import org.apache.myfaces.util.lang.Lazy;
import org.apache.myfaces.util.lang.StringUtils;
import org.apache.myfaces.view.facelets.FaceletCompositionContext;
import org.apache.myfaces.view.facelets.el.ELText;

/**
 * DOCUMENT ME!
 * 
 * @author Manfred Geiler (latest modification by $Author$)
 * @author Anton Koinov
 * @author Thomas Spiegl
 * @author Stan Silvert
 * @version $Revision$ $Date$
 */
@SuppressWarnings("deprecation")
public class ApplicationImpl extends Application
{
    private static final Logger log = Logger.getLogger(ApplicationImpl.class.getName());

    // the name for the system property which specifies the current ProjectStage (see MYFACES-2545 for details)
    public final static String PROJECT_STAGE_SYSTEM_PROPERTY_NAME = "faces.PROJECT_STAGE";

    /**
     * Key under UIViewRoot to generated unique ids for components added 
     * by @ResourceDependency effect.
     */
    private static final String RESOURCE_DEPENDENCY_UNIQUE_ID_KEY =
              "oam.view.resourceDependencyUniqueId";

    // ~ Instance fields
    // --------------------------------------------------------------------------
    // --

    private Collection<Locale> _supportedLocales = Collections.emptySet();
    private Locale _defaultLocale;
    private String _messageBundle;

    private ViewHandler _viewHandler;
    private NavigationHandler _navigationHandler;
    private ActionListener _actionListener;
    private String _defaultRenderKitId;
    private ResourceHandler _resourceHandler;
    private StateManager _stateManager;
    private FlowHandler _flowHandler;

    private ArrayList<ELContextListener> _elContextListeners;

    // components, converters, and validators can be added at runtime--must
    // synchronize, uses ConcurrentHashMap to allow concurrent read of map
    private final Map<String, Object> _converterIdToClassMap = new ConcurrentHashMap<>();

    private final Map<Class<?>, Object> _converterTargetClassToConverterClassMap = new ConcurrentHashMap<>();
    
    private final Map<String, Object> _componentClassMap = new ConcurrentHashMap<>();

    private final Map<String, Object> _validatorClassMap = new ConcurrentHashMap<>();

    private ApplicationImplEventManager _eventManager;
    
    private final Map<String, String> _defaultValidatorsIds = new HashMap<>();
    
    private volatile Map<String, String> _cachedDefaultValidatorsIds = null;
    
    private final Map<String, Object> _behaviorClassMap = new ConcurrentHashMap<>();

    private final RuntimeConfig _runtimeConfig;
    private final MyfacesConfig _myfacesConfig;

    private Lazy<ELResolver> elResolver;

    private ProjectStage _projectStage;

    private volatile boolean _firstRequestProcessed = false;
    
    // MYFACES-3442 If HashMap or other non thread-safe structure is used, it is
    // possible to fall in a infinite loop under heavy load unless a synchronized block
    // is used to modify it or a ConcurrentHashMap.
    private final Map<Class<?>, List<ListenerFor>> _classToListenerForMap = new ConcurrentHashMap<>() ;

    private final Map<Class<?>, List<ResourceDependency>> _classToResourceDependencyMap = new ConcurrentHashMap<>();
    
    private List<Class<? extends Converter>> _noArgConstructorConverterClasses = new CopyOnWriteArrayList<>();
    
    private Map<Class<? extends Converter>, Boolean> _cdiManagedConverterMap = new ConcurrentHashMap<>();
    
    private Map<Class<? extends Validator>, Boolean> _cdiManagedValidatorMap = new ConcurrentHashMap<>();
    
    private Map<Class<? extends Behavior>, Boolean> _cdiManagedBehaviorMap = new ConcurrentHashMap<>();
    
    /** Value of jakarta.faces.DATETIMECONVERTER_DEFAULT_TIMEZONE_IS_SYSTEM_TIMEZONE parameter */
    private boolean _dateTimeConverterDefaultTimeZoneIsSystemTimeZone = false; 
    
    private SearchExpressionHandler _searchExpressionHandler;
    
    private SearchKeywordResolver _searchExpressionResolver;

    /**
     * Represents semantic null in _componentClassMap. 
     */
    private static final UIComponent NOTHING = new UIComponentBase()
    {
        @Override
        public String getFamily()
        {
            return null;
        }
    };
    
    // ~ Constructors
    // --------------------------------------------------------------------------
    // -----

    public ApplicationImpl()
    {
        this(RuntimeConfig.getCurrentInstance(FacesContext.getCurrentInstance()));
    }

    ApplicationImpl(final RuntimeConfig runtimeConfig)
    {
        if (runtimeConfig == null)
        {
            throw new IllegalArgumentException("runtimeConfig must mot be null");
        }
        // set default implementation in constructor
        // pragmatic approach, no syncronizing will be needed in get methods
        _viewHandler = new ViewHandlerImpl();
        _navigationHandler = new NavigationHandlerImpl();
        _actionListener = new ActionListenerImpl();
        _defaultRenderKitId = "HTML_BASIC";
        _stateManager = new StateManagerImpl();
        _elContextListeners = new ArrayList<>();
        _resourceHandler = new ResourceHandlerImpl();
        _flowHandler = new FlowHandlerImpl();
        _searchExpressionHandler = new SearchExpressionHandlerImpl();
        _runtimeConfig = runtimeConfig;
        _myfacesConfig = MyfacesConfig.getCurrentInstance(getFacesContext());
        _eventManager = new ApplicationImplEventManager();

        if (log.isLoggable(Level.FINEST))
        {
            log.finest("New Application instance created");
        }
        
        String configParam = getFacesContext().getExternalContext().
                getInitParameter(Converter.DATETIMECONVERTER_DEFAULT_TIMEZONE_IS_SYSTEM_TIMEZONE_PARAM_NAME);
        if (configParam != null && configParam.toLowerCase().equals("true"))
        {
            _dateTimeConverterDefaultTimeZoneIsSystemTimeZone = true;
        }
        
        elResolver = new Lazy<>(() ->
        {
            CompositeELResolver celr = new CompositeELResolver();

            new DefaultELResolverBuilder(_runtimeConfig, _myfacesConfig)
                    .build(getFacesContext(), celr);

            return celr;
        });
    }

    // ~ Methods
    // --------------------------------------------------------------------------
    // ----------

    @Override
    public final void addELResolver(final ELResolver resolver)
    {
        if (isFirstRequestProcessed())
        {
            throw new IllegalStateException("It is illegal to add a resolver after the first request is processed");
        }
        if (resolver != null)
        {
            _runtimeConfig.addApplicationElResolver(resolver);
        }
    }

    @Override
    public void addDefaultValidatorId(String validatorId)
    {
        if (_validatorClassMap.containsKey(validatorId))
        {
            Class<? extends Validator> validatorClass =
                    getObjectFromClassMap(validatorId, _validatorClassMap);

            // Ensure atomicity between _defaultValidatorsIds and _cachedDefaultValidatorsIds
            synchronized(_defaultValidatorsIds)
            {
                _defaultValidatorsIds.put(validatorId, validatorClass.getName());
                _cachedDefaultValidatorsIds = null;
            }
        }
    }

    @Override
    public Map<String, String> getDefaultValidatorInfo()
    {
        // cachedMap ensures we will not return null if after the check for null
        // _cachedDefaultValidatorsIds is set to null. In theory the unmodifiable map
        // always has a reference to _defaultValidatorsIds, so any instance set
        // in _cachedDefaultValidatorsIds is always the same.
        Map<String, String> cachedMap = _cachedDefaultValidatorsIds;
        if (cachedMap == null)
        {
            synchronized(_defaultValidatorsIds)
            {
                if (_cachedDefaultValidatorsIds == null)
                {
                    _cachedDefaultValidatorsIds = Collections.unmodifiableMap(_defaultValidatorsIds);
                }
                cachedMap = _cachedDefaultValidatorsIds;
            }
        }
        return cachedMap;
    }

    @Override
    public final ELResolver getELResolver()
    {
        return elResolver.get();
    }

    @Override
    public final java.util.ResourceBundle getResourceBundle(final FacesContext facesContext, final String name)
            throws FacesException, NullPointerException
    {

        Assert.notNull(facesContext, "facesContext");
        Assert.notNull(name, "name");

        final String bundleName = getBundleName(facesContext, name);

        if (bundleName == null)
        {
            return null;
        }

        Locale locale = Locale.getDefault();

        final UIViewRoot viewRoot = facesContext.getViewRoot();
        if (viewRoot != null && viewRoot.getLocale() != null)
        {
            locale = viewRoot.getLocale();
        }

        try
        {
            return getResourceBundle(bundleName, locale, ClassUtils.getContextClassLoader());
        }
        catch (MissingResourceException e)
        {
            try
            {
                return getResourceBundle(bundleName, locale, this.getClass().getClassLoader());
            }
            catch (MissingResourceException e1)
            {            
                throw new FacesException("Could not load resource bundle for name '"
                                         + name + "': " + e.getMessage(), e1);
            }
        }
    }

    String getBundleName(final FacesContext facesContext, final String name)
    {
        ResourceBundle bundle = _runtimeConfig.getResourceBundle(name);
        return bundle != null ? bundle.getBaseName() : null;
    }

    java.util.ResourceBundle getResourceBundle(final String name, final Locale locale, final ClassLoader loader)
            throws MissingResourceException
    {
        if (_myfacesConfig.getResourceBundleControl() != null)
        {
            return java.util.ResourceBundle.getBundle(name, locale, loader,_myfacesConfig.getResourceBundleControl());
        }

        return java.util.ResourceBundle.getBundle(name, locale, loader);
    }

    final FacesContext getFacesContext()
    {
        return FacesContext.getCurrentInstance();
    }

    @Override
    public final UIComponent createComponent(final ValueExpression componentExpression,
                                             final FacesContext facesContext, final String componentType)
            throws FacesException, NullPointerException
    {

        /*
         * Before the component instance is returned, it must be inspected for the presence of a ListenerFor (or
         * ListenersFor) or ResourceDependency (or ResourceDependencies) annotation. If any of these annotations are
         * present, the action listed in ListenerFor or ResourceDependency must be taken on the component, 
         * before it is
         * returned from this method. This variant of createComponent must not inspect the Renderer for the 
         * component to
         * be returned for any of the afore mentioned annotations. Such inspection is the province of
         */

        Assert.notNull(componentExpression, "componentExpression");
        Assert.notNull(facesContext, "facesContext");
        Assert.notNull(componentType, "componentType");

        ELContext elContext = facesContext.getELContext();

        try
        {
            Object retVal = componentExpression.getValue(elContext);

            UIComponent createdComponent;

            if (retVal instanceof UIComponent)
            {
                createdComponent = (UIComponent) retVal;
                _handleAnnotations(facesContext, createdComponent, createdComponent);
            }
            else
            {
                createdComponent = createComponent(facesContext, componentType);
                componentExpression.setValue(elContext, createdComponent);
            }

            return createdComponent;
        }
        catch (FacesException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new FacesException(e);
        }
    }

    @Override
    public UIComponent createComponent(ValueExpression componentExpression, FacesContext context, 
                                       String componentType, String rendererType)
    {
        // Like createComponent(ValueExpression, FacesContext, String)
        UIComponent component = createComponent(componentExpression, context, componentType);

        if (rendererType != null)
        {
            _inspectRenderer(context, component, componentType, rendererType);
        }

        return component;
    }

    @Override
    public final ExpressionFactory getExpressionFactory()
    {
        return _runtimeConfig.getExpressionFactory();
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <T> T evaluateExpressionGet(final FacesContext context, final String expression,
                                             final Class<? extends T> expectedType) throws ELException
    {
        ELContext elContext = context.getELContext();

        ExpressionFactory factory = getExpressionFactory();

        return (T) factory.createValueExpression(elContext, expression, expectedType).getValue(elContext);
    }

    @Override
    public final void addELContextListener(final ELContextListener listener)
    {
        synchronized (_elContextListeners)
        {
            _elContextListeners.add(listener);
        }
    }

    @Override
    public void publishEvent(FacesContext facesContext, Class<? extends SystemEvent> systemEventClass,
                             Class<?> sourceBaseType, Object source)
    {
        _eventManager.publishEvent(facesContext, systemEventClass, sourceBaseType, source);
    }

    @Override
    public void publishEvent(FacesContext facesContext, Class<? extends SystemEvent> systemEventClass, Object source)
    {
        _eventManager.publishEvent(facesContext, systemEventClass, source);
    }

    @Override
    public final void removeELContextListener(final ELContextListener listener)
    {
        synchronized (_elContextListeners)
        {
            _elContextListeners.remove(listener);
        }
    }

    @Override
    public final ELContextListener[] getELContextListeners()
    {
        // this gets called on every request, so I can't afford to synchronize
        // I just have to trust that toArray() with do the right thing if the
        // list is changing (not likely)
        return _elContextListeners.toArray(new ELContextListener[_elContextListeners.size()]);
    }

    @Override
    public final void setActionListener(final ActionListener actionListener)
    {
        Assert.notNull(actionListener, "actionListener");

        _actionListener = actionListener;
        if (log.isLoggable(Level.FINEST))
        {
            log.finest("set actionListener = " + actionListener.getClass().getName());
        }
    }

    @Override
    public final ActionListener getActionListener()
    {
        return _actionListener;
    }

    @Override
    public Iterator<String> getBehaviorIds()
    {
        return _behaviorClassMap.keySet().iterator();
    }

    @Override
    public final Iterator<String> getComponentTypes()
    {
        return _componentClassMap.keySet().iterator();
    }

    @Override
    public final Iterator<String> getConverterIds()
    {
        return _converterIdToClassMap.keySet().iterator();
    }

    @Override
    public final Iterator<Class<?>> getConverterTypes()
    {
        return _converterTargetClassToConverterClassMap.keySet().iterator();
    }

    @Override
    public final void setDefaultLocale(final Locale locale)
    {
        Assert.notNull(locale, "locale");

        _defaultLocale = locale;
        if (log.isLoggable(Level.FINEST))
        {
            log.finest("set defaultLocale = " + locale.getCountry() + ' ' + locale.getLanguage());
        }
    }

    @Override
    public final Locale getDefaultLocale()
    {
        return _defaultLocale;
    }

    @Override
    public final void setMessageBundle(final String messageBundle)
    {
        Assert.notNull(messageBundle, "messageBundle");

        _messageBundle = messageBundle;
        if (log.isLoggable(Level.FINEST))
        {
            log.finest("set MessageBundle = " + messageBundle);
        }
    }

    @Override
    public final String getMessageBundle()
    {
        return _messageBundle;
    }

    @Override
    public final void setNavigationHandler(final NavigationHandler navigationHandler)
    {
        Assert.notNull(navigationHandler, "navigationHandler");

        _navigationHandler = navigationHandler;
        if (log.isLoggable(Level.FINEST))
        {
            log.finest("set NavigationHandler = " + navigationHandler.getClass().getName());
        }
    }

    @Override
    public final NavigationHandler getNavigationHandler()
    {
        return _navigationHandler;
    }

    @Override
    public ProjectStage getProjectStage()
    {
        // If the value has already been determined by a previous call to this
        // method, simply return that value.
        if (_projectStage == null)
        {
            String stageName = null;
            
            // try to obtain the ProjectStage from the system property
            // faces.PROJECT_STAGE as proposed by Ed Burns
            stageName = System.getProperty(PROJECT_STAGE_SYSTEM_PROPERTY_NAME);
            
            if (stageName == null)
            {
                // Look for a JNDI environment entry under the key given by the
                // value of ProjectStage.PROJECT_STAGE_JNDI_NAME (return type of
                // java.lang.String).
                try
                {
                    Context ctx = new InitialContext();
                    Object temp = ctx.lookup(ProjectStage.PROJECT_STAGE_JNDI_NAME);
                    if (temp != null)
                    {
                        if (temp instanceof String)
                        {
                            stageName = (String) temp;
                        }
                        else
                        {
                            log.severe("JNDI lookup for key " + ProjectStage.PROJECT_STAGE_JNDI_NAME
                                    + " should return a java.lang.String value");
                        }
                    }
                }
                catch (NamingException e)
                {
                    // no-op
                }
                catch (NoClassDefFoundError er)
                {
                    //On Google App Engine, javax.naming.Context is a restricted class.
                    //In that case, NoClassDefFoundError is thrown. stageName needs to be configured
                    //below by context parameter.
                    //It can be done with changing the order to look first at
                    // context param, but it is defined in the spec.
                    //http://java.sun.com/javaee/6/docs/api/jakarta/faces/application/Application.html#getProjectStage()
                    //no-op
                }
            }

            /*
             * If found, continue with the algorithm below, otherwise, look for an entry in the initParamMap of the
             * ExternalContext from the current FacesContext with the key ProjectStage.PROJECT_STAGE_PARAM_NAME
             */
            if (stageName == null)
            {
                stageName = getFacesContext().getExternalContext()
                        .getInitParameter(ProjectStage.PROJECT_STAGE_PARAM_NAME);
            }

            // If a value is found
            if (stageName != null)
            {
                /*
                 * see if an enum constant can be obtained by calling ProjectStage.valueOf(), passing 
                 * the value from the initParamMap. If this succeeds without exception, save the value 
                 * and return it.
                 */
                try
                {
                    _projectStage = ProjectStage.valueOf(stageName);
                    return _projectStage;
                }
                catch (IllegalArgumentException e)
                {
                    log.log(Level.INFO, "Couldn't discover the current project stage: "+stageName);
                }
            }
            else
            {
                if (log.isLoggable(Level.INFO))
                {
                    log.info("Couldn't discover the current project stage, using " + ProjectStage.Production);
                }
            }

            /*
             * If not found, or any of the previous attempts to discover the enum constant value have failed, log a
             * descriptive error message, assign the value as ProjectStage.Production and return it.
             */
            
            _projectStage = ProjectStage.Production;
        }

        return _projectStage;
    }


    @Override
    public final void setResourceHandler(ResourceHandler resourceHandler)
    {
        Assert.notNull(resourceHandler, "resourceHandler");

        if(isFirstRequestProcessed())
        {
            throw new IllegalStateException(
                    "setResourceHandler may not be executed after a lifecycle request has been completed");
        }
        _resourceHandler = resourceHandler;
    }

    @Override
    public final ResourceHandler getResourceHandler()
    {
        return _resourceHandler;
    }

    @Override
    public final void setSupportedLocales(final Collection<Locale> locales)
    {
        Assert.notNull(locales, "locales");

        _supportedLocales = locales;
        if (log.isLoggable(Level.FINEST))
        {
            log.finest("set SupportedLocales");
        }
    }

    @Override
    public final Iterator<Locale> getSupportedLocales()
    {
        return _supportedLocales.iterator();
    }

    @Override
    public final Iterator<String> getValidatorIds()
    {
        return _validatorClassMap.keySet().iterator();
    }

    @Override
    public final void setViewHandler(final ViewHandler viewHandler)
    {
        Assert.notNull(viewHandler, "viewHandler");

        if(isFirstRequestProcessed())
        {
            throw new IllegalStateException(
                    "setViewHandler may not be executed after a lifecycle request has been completed");
        }
        _viewHandler = viewHandler;
        if (log.isLoggable(Level.FINEST))
        {
            log.finest("set ViewHandler = " + viewHandler.getClass().getName());
        }
    }
    
    @Override
    public void subscribeToEvent(Class<? extends SystemEvent> systemEventClass, SystemEventListener listener)
    {
        _eventManager.subscribeToEvent(systemEventClass, listener);
    }

    @Override
    public void subscribeToEvent(Class<? extends SystemEvent> systemEventClass, Class<?> sourceClass,
                                 SystemEventListener listener)
    {
        _eventManager.subscribeToEvent(systemEventClass, sourceClass, listener);
    }
    
    @Override
    public void unsubscribeFromEvent(Class<? extends SystemEvent> systemEventClass, SystemEventListener listener)
    {
        _eventManager.unsubscribeFromEvent(systemEventClass, listener);
    }

    @Override
    public void unsubscribeFromEvent(Class<? extends SystemEvent> systemEventClass, Class<?> sourceClass,
                                     SystemEventListener listener)
    {
        _eventManager.unsubscribeFromEvent(systemEventClass, sourceClass, listener);
    }

    @Override
    public final ViewHandler getViewHandler()
    {
        return _viewHandler;
    }

    @Override
    public void addBehavior(String behaviorId, String behaviorClass)
    {
        Assert.notEmpty(behaviorId, "behaviorId");
        Assert.notEmpty(behaviorClass, "behaviorClass");

        try
        {
            if (_myfacesConfig.isLazyLoadConfigObjects())
            {
                _behaviorClassMap.put(behaviorId, behaviorClass);
            }
            else
            {
                _behaviorClassMap.put(behaviorId, ClassUtils.simpleClassForName(behaviorClass));
            }
            
            if (log.isLoggable(Level.FINEST))
            {
                log.finest("add Behavior class = " + behaviorClass + " for id = " + behaviorId);
            }
        }
        catch (Exception e)
        {
            log.log(Level.SEVERE, "Behavior class " + behaviorClass + " not found", e);
        }

    }

    @Override
    public final void addComponent(final String componentType, final String componentClassName)
    {
        Assert.notEmpty(componentType, "componentType");
        Assert.notEmpty(componentClassName, "componentClassName");

        try
        {
            if (_myfacesConfig.isLazyLoadConfigObjects())
            {
                _componentClassMap.put(componentType, componentClassName);
            }
            else
            {
                _componentClassMap.put(componentType, ClassUtils.simpleClassForName(componentClassName));
            }
            
            if (log.isLoggable(Level.FINEST))
            {
                log.finest("add Component class = " + componentClassName + " for type = " + componentType);
            }
        }
        catch (Exception e)
        {
            log.log(Level.SEVERE, "Component class " + componentClassName + " not found", e);
        }
    }

    @Override
    public final void addConverter(final String converterId, final String converterClass)
    {
        Assert.notEmpty(converterId, "converterId");
        Assert.notEmpty(converterClass, "converterClass");

        try
        {
            if (_myfacesConfig.isLazyLoadConfigObjects())
            {
                _converterIdToClassMap.put(converterId, converterClass);
            }
            else
            {
                _converterIdToClassMap.put(converterId, ClassUtils.simpleClassForName(converterClass));
            }
            if (log.isLoggable(Level.FINEST))
            {
                log.finest("add Converter id = " + converterId + " converterClass = " + converterClass);
            }
        }
        catch (Exception e)
        {
            log.log(Level.SEVERE, "Converter class " + converterClass + " not found", e);
        }
    }

    @Override
    public final void addConverter(final Class<?> targetClass, final String converterClass)
    {
        Assert.notNull(targetClass, "targetClass");
        Assert.notEmpty(converterClass, "converterClass");

        try
        {
            if (_myfacesConfig.isLazyLoadConfigObjects())
            {
                _converterTargetClassToConverterClassMap.put(targetClass, converterClass);
            }
            else
            {
                _converterTargetClassToConverterClassMap.put(targetClass,
                                                             ClassUtils.simpleClassForName(converterClass));
            }

            if (log.isLoggable(Level.FINEST))
            {
                log.finest("add Converter for class = " + targetClass + " converterClass = " + converterClass);
            }
        }
        catch (Exception e)
        {
            log.log(Level.SEVERE, "Converter class " + converterClass + " not found", e);
        }
    }

    @Override
    public final void addValidator(final String validatorId, final String validatorClass)
    {
        Assert.notEmpty(validatorId, "validatorId");
        Assert.notEmpty(validatorClass, "validatorClass");

        try
        {
            if (_myfacesConfig.isLazyLoadConfigObjects())
            {
                _validatorClassMap.put(validatorId, validatorClass);
            }
            else
            {
                _validatorClassMap.put(validatorId, ClassUtils.simpleClassForName(validatorClass));
            }
            
            if (log.isLoggable(Level.FINEST))
            {
                log.finest("add Validator id = " + validatorId + " class = " + validatorClass);
            }
        }
        catch (Exception e)
        {
            log.log(Level.SEVERE, "Validator class " + validatorClass + " not found", e);
        }
    }

    @Override
    public Behavior createBehavior(String behaviorId) throws FacesException
    {
        Assert.notEmpty(behaviorId, "behaviorId");

        final Class<? extends Behavior> behaviorClass = getObjectFromClassMap(behaviorId, _behaviorClassMap);
        if (behaviorClass == null)
        {
            throw new FacesException("Could not find any registered behavior-class for behaviorId : " + behaviorId);
        }

        try
        {
            if (!_cdiManagedBehaviorMap.containsKey(behaviorClass))
            {
                FacesBehavior annotation = behaviorClass.getAnnotation(FacesBehavior.class);
                if (annotation != null && annotation.managed())
                {
                    _cdiManagedBehaviorMap.put(behaviorClass, true);
                }
                else
                {
                    _cdiManagedBehaviorMap.put(behaviorClass, false);
                }
            }

            boolean managed = _cdiManagedBehaviorMap.get(behaviorClass);

            Behavior behavior = null;
            if (managed)
            {
                if (ClientBehavior.class.isAssignableFrom(behaviorClass))
                {
                    behavior = new FacesClientBehaviorCDIWrapper((Class<ClientBehavior>)behaviorClass, behaviorId);
                }
                else
                {
                    behavior = new FacesBehaviorCDIWrapper(behaviorClass, behaviorId);
                }
                Behavior innerBehavior = ((FacesWrapper<Behavior>)behavior).getWrapped();

                FacesContext facesContext = getFacesContext();
                _handleAttachedResourceDependencyAnnotations(facesContext, innerBehavior);

                if (innerBehavior instanceof ClientBehaviorBase)
                {
                    ClientBehaviorBase clientBehavior = (ClientBehaviorBase) innerBehavior;
                    String renderType = clientBehavior.getRendererType();
                    if (renderType != null)
                    {
                        ClientBehaviorRenderer cbr = facesContext.getRenderKit().getClientBehaviorRenderer(renderType);
                        _handleAttachedResourceDependencyAnnotations(facesContext, cbr);
                    }
                }
            }
            else
            {
                behavior = behaviorClass.newInstance();
                FacesContext facesContext = getFacesContext();
                _handleAttachedResourceDependencyAnnotations(facesContext, behavior);

                if (behavior instanceof ClientBehaviorBase)
                {
                    ClientBehaviorBase clientBehavior = (ClientBehaviorBase) behavior;
                    String renderType = clientBehavior.getRendererType();
                    if (renderType != null)
                    {
                        ClientBehaviorRenderer cbr = facesContext.getRenderKit().getClientBehaviorRenderer(renderType);
                        _handleAttachedResourceDependencyAnnotations(facesContext, cbr);
                    }
                }
            }

            return behavior;
        }
        catch (Exception e)
        {
            log.log(Level.SEVERE, "Could not instantiate behavior " + behaviorClass, e);
            throw new FacesException("Could not instantiate behavior: " + behaviorClass, e);
        }
    }

    @Override
    public UIComponent createComponent(FacesContext context, Resource componentResource)
    {
        Assert.notNull(context, "context");
        Assert.notNull(componentResource, "componentResource");
        
        UIComponent component = null;
        Resource resource;
        String fqcn;
        Class<? extends UIComponent> componentClass = null;

        /*
         * Obtain a reference to the ViewDeclarationLanguage for this Application instance by calling
         * ViewHandler.getViewDeclarationLanguage(jakarta.faces.context.FacesContext, java.lang.String), passing the
         * viewId found by calling UIViewRoot.getViewId() on the UIViewRoot in the argument FacesContext.
         */
        UIViewRoot view = context.getViewRoot();
        Application application = context.getApplication();
        ViewDeclarationLanguage vdl
                = application.getViewHandler().getViewDeclarationLanguage(context, view.getViewId());

        /*
         * Obtain a reference to the composite component metadata for this composite component by calling
         * ViewDeclarationLanguage.getComponentMetadata(jakarta.faces.context.FacesContext,
         * jakarta.faces.application.Resource), passing the facesContext and componentResource arguments to this method.
         * This version of Faces specification uses JavaBeans as the API to the component metadata.
         */
        BeanInfo metadata = vdl.getComponentMetadata(context, componentResource);
        if (metadata == null)
        {
            throw new FacesException("Could not get component metadata for " 
                    + componentResource.getResourceName()
                    + ". Did you forget to specify <composite:interface>?");
        }

        /*
         * Determine if the component author declared a component-type for this component instance by obtaining the
         * BeanDescriptor from the component metadata and calling its getValue() method, passing
         * UIComponent.COMPOSITE_COMPONENT_TYPE_KEY as the argument. If non-null, the result must be a ValueExpression
         * whose value is the component-type of the UIComponent to be created for this Resource component. Call through
         * to createComponent(java.lang.String) to create the component.
         */
        BeanDescriptor descriptor = metadata.getBeanDescriptor();
        ValueExpression componentType = (ValueExpression) descriptor.getValue(
                UIComponent.COMPOSITE_COMPONENT_TYPE_KEY);
        boolean annotationsApplied = false;
        if (componentType != null)
        {
            component = application.createComponent((String) componentType.getValue(context.getELContext()));
            annotationsApplied = true;
        }
        else
        {
            /*
             * Otherwise, determine if a script based component for this Resource can be found by calling
             * ViewDeclarationLanguage.getScriptComponentResource(jakarta.faces.context.FacesContext,
             * jakarta.faces.application.Resource). If the result is non-null, and is a script written in one of the
             * languages listed in Faces 4.3 of the specification prose document, create a UIComponent instance from the
             * script resource.
             */
            resource = vdl.getScriptComponentResource(context, componentResource);
            if (resource != null)
            {
                String name = resource.getResourceName();
                String className = name.substring(0, name.lastIndexOf('.'));

                component = (UIComponent)ClassUtils.newInstance(className);
            }
            else
            {
                /*
                 * Otherwise, let library-name be the return from calling Resource.getLibraryName() on the argument
                 * componentResource and resource-name be the return from calling Resource.getResourceName() on the
                 * argument componentResource. Create a fully qualified Java class name by removing any file extension
                 * from resource-name and let fqcn be library-name + "." + resource-name. If a class with the name of
                 * fqcn cannot be found, take no action and continue to the next step. If any of 
                 * InstantiationException,
                 * IllegalAccessException, or ClassCastException are thrown, wrap the exception in a FacesException and
                 * re-throw it. If any other exception is thrown, log the exception and continue to the next step.
                 */

                boolean isProduction = FacesContext.getCurrentInstance().isProjectStage(ProjectStage.Production);
                String name = componentResource.getResourceName();
                String className = name.substring(0, name.lastIndexOf('.'));
                fqcn = componentResource.getLibraryName() + '.' + className;
                
                if (isProduction)
                {
                    componentClass = (Class<? extends UIComponent>) _componentClassMap.get(fqcn);
                }
                if (componentClass == null)
                {
                    try
                    {
                        componentClass = ClassUtils.classForName(fqcn);
                        if (isProduction)
                        {
                            _componentClassMap.put(fqcn, componentClass);
                        }
                    }
                    catch (ClassNotFoundException e)
                    {
                        // Remember here that classForName did not find Class
                        if (isProduction)
                        {
                            _componentClassMap.put(fqcn, NOTHING.getClass());
                        }
                    }
                }

                if (componentClass != null && NOTHING.getClass() != componentClass)
                {
                    try
                    {
                        component = componentClass.newInstance();
                    }
                    catch (InstantiationException e)
                    {
                        log.log(Level.SEVERE, "Could not instantiate component class name = " + fqcn, e);
                        throw new FacesException("Could not instantiate component class name = " + fqcn, e);
                    }
                    catch (IllegalAccessException e)
                    {
                        log.log(Level.SEVERE, "Could not instantiate component class name = " + fqcn, e);
                        throw new FacesException("Could not instantiate component class name = " + fqcn, e);
                    }
                    catch (Exception e)
                    {
                        log.log(Level.SEVERE, "Could not instantiate component class name = " + fqcn, e);
                    }
                }

                /*
                 * If none of the previous steps have yielded a UIComponent instance, call
                 * createComponent(java.lang.String) passing "jakarta.faces.NamingContainer" as the argument.
                 */
                if (component == null)
                {
                    component = application.createComponent(context, UINamingContainer.COMPONENT_TYPE, null);
                    annotationsApplied = true;
                }
            }
        }

        /*
         * Call UIComponent.setRendererType(java.lang.String) on the UIComponent instance, passing
         * "jakarta.faces.Composite" as the argument.
         */
        component.setRendererType("jakarta.faces.Composite");

        /*
         * Store the argument Resource in the attributes Map of the UIComponent under the key,
         * Resource.COMPONENT_RESOURCE_KEY.
         */
        component.getAttributes().put(Resource.COMPONENT_RESOURCE_KEY, componentResource);

        /*
         * Store composite component metadata in the attributes Map of the UIComponent under the key,
         * UIComponent.BEANINFO_KEY.
         */
        component.getAttributes().put(UIComponent.BEANINFO_KEY, metadata);

        /*
         * Before the component instance is returned, it must be inspected for the presence of a 
         * ListenerFor annotation.
         * If this annotation is present, the action listed in ListenerFor must be taken on the component, 
         * before it is
         * returned from this method.
         */
        if (!annotationsApplied)
        {
            _handleAnnotations(context, component, component);
        }

        return component;
    }

    @Override
    public UIComponent createComponent(FacesContext context, String componentType, String rendererType)
    {
        Assert.notNull(context, "context");
        Assert.notNull(componentType, "componentType");

        // Like createComponent(String)
        UIComponent component = createComponent(context, componentType);

        // A null value on this field is valid! If that so, no need to do any log
        // or look on RenderKit map for a inexistent renderer!
        if (rendererType != null)
        {
            _inspectRenderer(context, component, componentType, rendererType);
        }

        return component;
    }

    /**
     * This works just like createComponent(String componentType), but without call
     * FacesContext.getCurrentInstance()
     * 
     * @param facesContext
     * @param componentType
     * @return
     * @throws FacesException 
     */
    private final UIComponent createComponent(FacesContext facesContext, 
            final String componentType) throws FacesException
    {
        Assert.notEmpty(componentType, "componentType");

        final Class<? extends UIComponent> componentClass =
                getObjectFromClassMap(componentType, _componentClassMap);
        if (componentClass == null)
        {
            log.log(Level.SEVERE, "Undefined component type " + componentType);
            throw new FacesException("Undefined component type " + componentType);
        }

        try
        {
            UIComponent component = componentClass.newInstance();
            _handleAnnotations(facesContext, component, component);
            return component;
        }
        catch (Exception e)
        {
            log.log(Level.SEVERE, "Could not instantiate component componentType = " + componentType, e);
            throw new FacesException("Could not instantiate component componentType = " + componentType, e);
        }
    }
    
    @Override
    public final UIComponent createComponent(final String componentType) throws FacesException
    {
        Assert.notEmpty(componentType, "componentType");

        final Class<? extends UIComponent> componentClass =
                getObjectFromClassMap(componentType, _componentClassMap);
        if (componentClass == null)
        {
            log.log(Level.SEVERE, "Undefined component type " + componentType);
            throw new FacesException("Undefined component type " + componentType);
        }

        try
        {
            UIComponent component = componentClass.newInstance();
            _handleAnnotations(getFacesContext(), component, component);
            return component;
        }
        catch (Exception e)
        {
            log.log(Level.SEVERE, "Could not instantiate component componentType = " + componentType, e);
            throw new FacesException("Could not instantiate component componentType = " + componentType, e);
        }
    }

    /**
     * Return an instance of the converter class that has been registered under the specified id.
     * <p>
     * Converters are registered via faces-config.xml files, and can also be registered via the addConverter(String id,
     * Class converterClass) method on this class. Here the the appropriate Class definition is found, then an instance
     * is created and returned.
     * <p>
     * A converter registered via a config file can have any number of nested attribute or property tags. The Faces
     * specification is very vague about what effect these nested tags have. This method ignores nested attribute
     * definitions, but for each nested property tag the corresponding setter is invoked on the new Converter instance
     * passing the property's defaultValuer. Basic typeconversion is done so the target properties on the Converter
     * instance can be String, int, boolean, etc. Note that:
     * <ol>
     * <li>the Sun Mojarra Faces implemenation ignores nested property tags completely, so this behaviour cannot be 
     * relied on across implementations.
     * <li>there is no equivalent functionality for converter classes registered via the Application.addConverter api
     * method.
     * </ol>
     * <p>
     * Note that this method is most commonly called from the standard f:attribute tag. As an alternative, most
     * components provide a "converter" attribute which uses an EL expression to create a Converter instance, in which
     * case this method is not invoked at all. The converter attribute allows the returned Converter instance to be
     * configured via normal dependency-injection, and is generally a better choice than using this method.
     */
    @Override
    public final Converter createConverter(final String converterId)
    {
        Assert.notEmpty(converterId, "converterId");

        final Class<? extends Converter> converterClass = getObjectFromClassMap(converterId, _converterIdToClassMap);
        if (converterClass == null)
        {
            throw new FacesException("Could not find any registered converter-class by converterId : " + converterId);
        }

        try
        {
            if (!_cdiManagedConverterMap.containsKey(converterClass))
            {
                FacesConverter annotation = converterClass.getAnnotation(FacesConverter.class);
                if (annotation != null && annotation.managed())
                {
                    _cdiManagedConverterMap.put(converterClass, true);
                }
                else
                {
                    _cdiManagedConverterMap.put(converterClass, false);
                }
            }
            
            boolean managed = _cdiManagedConverterMap.get(converterClass);

            Converter converter = null;
            if (managed)
            {
                converter = new FacesConverterCDIWrapper(converterClass, null, converterId);
                
                setConverterProperties(converterClass, ((FacesWrapper<Converter>)converter).getWrapped());

                _handleAttachedResourceDependencyAnnotations(getFacesContext(), 
                        ((FacesWrapper<Converter>)converter).getWrapped());
            }
            else
            {
                converter = createConverterInstance(converterClass);

                setConverterProperties(converterClass, converter);

                _handleAttachedResourceDependencyAnnotations(getFacesContext(), converter);
            }

            return converter;
        }
        catch (Exception e)
        {
            log.log(Level.SEVERE, "Could not instantiate converter " + converterClass, e);
            throw new FacesException("Could not instantiate converter: " + converterClass, e);
        }
    }

    private Converter createConverterInstance(Class<? extends Converter> converterClass)
            throws InstantiationException, IllegalAccessException
    {
        return converterClass.newInstance();
    }

    @Override
    public final Converter createConverter(final Class<?> targetClass)
    {
        Assert.notNull(targetClass, "targetClass");

        return internalCreateConverter(targetClass);
    }

    @SuppressWarnings("unchecked")
    private Converter<?> internalCreateConverter(final Class<?> targetClass)
    {
        // Locate a Converter registered for the target class itself.
        Object converterClassOrClassName = _converterTargetClassToConverterClassMap.get(targetClass);
        
        // Locate a Converter registered for interfaces that are
        // implemented by the target class (directly or indirectly).
        // Skip if class is String, for performance reasons 
        // (save 3 additional lookups over a concurrent map per request). 
        if (converterClassOrClassName == null && !String.class.equals(targetClass))
        {
            final Class<?>[] interfaces = targetClass.getInterfaces();
            if (interfaces != null)
            {
                for (int i = 0, len = interfaces.length; i < len; i++)
                {
                    // search all superinterfaces for a matching converter,
                    // create it
                    final Converter<?> converter = internalCreateConverter(interfaces[i]);
                    if (converter != null)
                    {
                        return converter;
                    }
                }
            }
        }

        // Get EnumConverter for enum classes with no special converter, check
        // here as recursive call with java.lang.Enum will not work
        if (converterClassOrClassName == null && targetClass.isEnum())
        {
            converterClassOrClassName = _converterTargetClassToConverterClassMap.get(Enum.class);
        }

        if (converterClassOrClassName != null)
        {
            try
            {
                Class<? extends Converter> converterClass = null;
                if (converterClassOrClassName instanceof Class<?>)
                {
                    converterClass = (Class<? extends Converter>) converterClassOrClassName;
                }
                else if (converterClassOrClassName instanceof String)
                {
                    converterClass = ClassUtils.simpleClassForName((String) converterClassOrClassName);
                    _converterTargetClassToConverterClassMap.put(targetClass, converterClass);
                }
                else
                {
                    //object stored in the map for this id is an invalid type.  remove it and return null
                    _converterTargetClassToConverterClassMap.remove(targetClass);
                }

                if (!_cdiManagedConverterMap.containsKey(converterClass))
                {
                    FacesConverter annotation = converterClass.getAnnotation(FacesConverter.class);
                    if (annotation != null && annotation.managed())
                    {
                        _cdiManagedConverterMap.put(converterClass, true);
                    }
                    else
                    {
                        _cdiManagedConverterMap.put(converterClass, false);
                    }
                }
                
                Converter<?> converter = null;
                
                if (Boolean.TRUE.equals(_cdiManagedConverterMap.get(converterClass)))
                {
                    converter = new FacesConverterCDIWrapper(converterClass, targetClass, null);
                    
                    setConverterProperties(converterClass, ((FacesWrapper<Converter>)converter).getWrapped());
                }
                else
                {
                    // check cached constructor information
                    if (!_noArgConstructorConverterClasses.contains(converterClass))
                    {
                        // the converter class either supports the one-arg constructor
                        // or has never been processed before
                        try
                        {
                            // look for a constructor that takes a single Class object
                            // See Faces 1.2 javadoc for Converter
                            Constructor<? extends Converter> constructor = converterClass
                                    .getConstructor(new Class[] { Class.class });

                            converter = constructor.newInstance(new Object[] { targetClass });
                        }
                        catch (Exception e)
                        {
                            // the constructor does not exist
                            // add the class to the no-arg constructor classes cache
                            _noArgConstructorConverterClasses.add(converterClass);

                            // use no-arg constructor
                            converter = createConverterInstance(converterClass);
                        }
                    }
                    else
                    {
                        // use no-arg constructor
                        converter = createConverterInstance(converterClass);
                    }
                    
                    setConverterProperties(converterClass, converter);
                }

                return converter;
            }
            catch (Exception e)
            {
                log.log(Level.SEVERE, "Could not instantiate converter " + converterClassOrClassName.toString(), e);
                throw new FacesException("Could not instantiate converter: " + converterClassOrClassName.toString(), e);
            }
        }

        // locate converter for primitive types
        if (targetClass == Long.TYPE)
        {
            return internalCreateConverter(Long.class);
        }
        else if (targetClass == Boolean.TYPE)
        {
            return internalCreateConverter(Boolean.class);
        }
        else if (targetClass == Double.TYPE)
        {
            return internalCreateConverter(Double.class);
        }
        else if (targetClass == Byte.TYPE)
        {
            return internalCreateConverter(Byte.class);
        }
        else if (targetClass == Short.TYPE)
        {
            return internalCreateConverter(Short.class);
        }
        else if (targetClass == Integer.TYPE)
        {
            return internalCreateConverter(Integer.class);
        }
        else if (targetClass == Float.TYPE)
        {
            return internalCreateConverter(Float.class);
        }
        else if (targetClass == Character.TYPE)
        {
            return internalCreateConverter(Character.class);
        }

        // Locate a Converter registered for the superclass (if any) of the
        // target class,
        // recursively working up the inheritance hierarchy.
        Class<?> superClazz = targetClass.getSuperclass();

        return superClazz != null ? internalCreateConverter(superClazz) : null;

    }

    private void setConverterProperties(final Class<?> converterClass, final Converter converter)
    {
        final org.apache.myfaces.config.element.Converter converterConfig = _runtimeConfig
                .getConverterConfiguration(converterClass.getName());
        
        // if the converter is a DataTimeConverter, check the init param for the default timezone (since 2.0)
        if (converter instanceof DateTimeConverter && _dateTimeConverterDefaultTimeZoneIsSystemTimeZone)
        {    
            ((DateTimeConverter) converter).setTimeZone(TimeZone.getDefault());
        }

        if (converterConfig != null && !converterConfig.getProperties().isEmpty())
        {
            Map<String, ? extends PropertyDescriptorWrapper> pds = PropertyDescriptorUtils.getCachedPropertyDescriptors(
                    FacesContext.getCurrentInstance().getExternalContext(),
                    converterClass);

            for (int i = 0; i < converterConfig.getProperties().size(); i++)
            {
                Property property = converterConfig.getProperties().get(i);
                try
                {
                    PropertyDescriptorWrapper pd = pds.get(property.getPropertyName());
                    // see MYFACES-2602 - skip set value if it was already set via constructor and now != null
                    if (!pd.getPropertyType().isPrimitive())
                    {
                        Object defaultValue;

                        Function<Object, Object> readFunction = null;
                        if (pd instanceof LambdaPropertyDescriptor)
                        {
                            readFunction = ((LambdaPropertyDescriptor) pd).getReadFunction();
                        }

                        if (readFunction != null)
                        {
                            defaultValue = readFunction.apply(converter);
                        }
                        else
                        {
                            defaultValue = pd.getReadMethod().invoke(converter);
                        }

                        if (defaultValue != null)
                        {
                            continue;
                        }
                    }

                    Object convertedValue = ClassUtils.convertToType(property.getDefaultValue(), pd.getPropertyType());

                    BiConsumer<Object, Object> writeFunction = null;
                    if (pd instanceof LambdaPropertyDescriptor)
                    {
                        writeFunction = ((LambdaPropertyDescriptor) pd).getWriteFunction();
                    }

                    if (writeFunction != null)
                    {
                        writeFunction.accept(converter, convertedValue);
                    }
                    else
                    {
                        pd.getWriteMethod().invoke(converter, convertedValue);
                    }
                }
                catch (Throwable th)
                {
                    log.log(Level.SEVERE, "Initializing converter : " + converterClass.getName() + " with property : "
                            + property.getPropertyName() + " and value : " + property.getDefaultValue() + " failed.");
                }
            }
        }
    }
    
    private void _handleAttachedResourceDependencyAnnotations(FacesContext context, Object inspected)
    {
        if (inspected == null)
        {
            return;
        }
        
        // This and only this method handles @ResourceDependency and @ResourceDependencies annotations
        // The source of these annotations is Class<?> inspectedClass.
        // Because Class<?> and its annotations cannot change
        // during request/response, it is sufficient to process Class<?> only once per view.
        RequestViewContext rvc = RequestViewContext.getCurrentInstance(context);
        Class<?> inspectedClass = inspected.getClass();
        if (rvc.isClassAlreadyProcessed(inspectedClass))
        {
            return;
        }

        // reset cache each time
        if (context.isProjectStage(ProjectStage.Development))
        {
            _classToResourceDependencyMap.remove(inspectedClass);
        }
        
        List<ResourceDependency> dependencyList = _classToResourceDependencyMap.get(inspectedClass);
        if (dependencyList == null)
        {
            dependencyList = new ArrayList<>(5);

            ResourceDependency dependency = inspectedClass.getAnnotation(ResourceDependency.class);
            if (dependency != null)
            {
                dependencyList.add(dependency);
            }

            ResourceDependencies dependencies = inspectedClass.getAnnotation(ResourceDependencies.class);
            if (dependencies != null)
            {
                dependencyList.addAll(Arrays.asList(dependencies.value()));
            }

            _classToResourceDependencyMap.put(inspectedClass,
                    dependencyList.isEmpty() ? Collections.emptyList() : dependencyList);
        }

        if (!dependencyList.isEmpty()) 
        {
            for (int i = 0, size = dependencyList.size(); i < size; i++)
            {
                ResourceDependency dependency = dependencyList.get(i);
                if (!rvc.isResourceDependencyAlreadyProcessed(dependency))
                {
                    _handleAttachedResourceDependency(context, dependency, inspectedClass);
                    rvc.setResourceDependencyAsProcessed(dependency);
                }
            }
        }

        rvc.setClassProcessed(inspectedClass);
    }

    /**
     * If the ResourceDependency component is created under facelets processing, it should receive
     * an special unique component id. This method check if there is a FaceletCompositionContext
     * and if that so, set the id. Components added by the effect of ResourceDependency are special,
     * because they do not have state, but they depends on the view structure, so with PSS, 
     * each time the view is built they are "recalculated", so they work as if they were transient
     * components that needs to be created at each request, but there are some cases were the 
     * components needs to be saved and restored fully. If a component is created outside facelets 
     * control (render response phase) it is expected to use the default implementation of 
     * createUniqueId(), but in that case, note that this happens after markInitialState() is 
     * called, and the component in this case is saved and restored fully, as expected.
     * 
     * This code cannot be called from facelets component tag handler, because in cases where a
     * component subtree is created using binding property, facelets lost control over component
     * creation and delegates it to the user, but since the binding code is executed each time the
     * view is created, the effect over ResourceDependency persists and the binding code takes into
     * account in the recalculation step, even if later the node related to the binding property
     * is dropped and recreated from the state fully. 
     * 
     * @param facesContext
     * @param component 
     */
    private void setResourceIdOnFaceletsMode(FacesContext facesContext, UIComponent component,
            Class<?> inspectedClass)
    {
        if (component.getId() == null)
        {
            FaceletCompositionContext mctx = FaceletCompositionContext.getCurrentInstance(facesContext);
            if (mctx != null)
            {
                UIViewRoot root = facesContext.getViewRoot();
                root.getAttributes().put(RESOURCE_DEPENDENCY_UNIQUE_ID_KEY, Boolean.TRUE);
                try
                {
                    String uid = root.createUniqueId(facesContext, null);
                    component.setId(uid);
                }
                finally
                {
                    root.getAttributes().put(RESOURCE_DEPENDENCY_UNIQUE_ID_KEY, Boolean.FALSE);
                }
                if (!mctx.isUsingPSSOnThisView())
                {
                    // Now set the identifier that will help to know which classes has been already inspected.
                    component.getAttributes().put(
                            RequestViewContext.RESOURCE_DEPENDENCY_INSPECTED_CLASS, inspectedClass);
                }
                else if (mctx.isRefreshTransientBuildOnPSSPreserveState())
                {
                    component.getAttributes().put(
                            RequestViewContext.RESOURCE_DEPENDENCY_INSPECTED_CLASS, inspectedClass);
                }
            }
            else
            {
                // This happens when there is a programmatic addition, which means the user has added the
                // components to the tree on render response phase or earlier but outside facelets control.
                // In that case we need to save the dependency.
                component.getAttributes().put(
                        RequestViewContext.RESOURCE_DEPENDENCY_INSPECTED_CLASS, inspectedClass);
            }
        }
    }
    
    private void _handleAttachedResourceDependency(FacesContext context, ResourceDependency annotation, 
            Class<?> inspectedClass)
    {
        // If this annotation is not present on the class in question, no action must be taken. 
        if (annotation != null)
        {
            Application application = context.getApplication();
            
            // Create a UIOutput instance by passing jakarta.faces.Output. to 
            // Application.createComponent(java.lang.String).
            UIOutput output = (UIOutput) application.createComponent(context, UIOutput.COMPONENT_TYPE, null);
            
            // Get the annotation instance from the class and obtain the values of the name, library, and 
            // target attributes.
            String name = annotation.name();
            if (StringUtils.isNotEmpty(name))
            {
                name = ELText.parseAsString(getExpressionFactory(), context.getELContext(), name);
            }
            
            // Obtain the renderer-type for the resource name by passing name to 
            // ResourceHandler.getRendererTypeForResourceName(java.lang.String).
            String rendererType = application.getResourceHandler().getRendererTypeForResourceName(name);
            
            // Call setRendererType on the UIOutput instance, passing the renderer-type.
            output.setRendererType(rendererType);
            
            // If the @ResourceDependency was done inside facelets processing,
            // call setId() and set a proper id from facelets
            setResourceIdOnFaceletsMode(context, output, inspectedClass);
            
            // Obtain the Map of attributes from the UIOutput component by calling UIComponent.getAttributes().
            Map<String, Object> attributes = output.getAttributes();
            
            // Store the name into the attributes Map under the key "name".
            attributes.put("name", name);
            
            // If library is the empty string, let library be null.
            String library = annotation.library();
            if (StringUtils.isNotEmpty(library))
            {
                library = ELText.parseAsString(getExpressionFactory(), context.getELContext(), library);
                // If library is non-null, store it under the key "library".
                attributes.put("library", library);
            }
            
            // Identify the resource as created by effect of a @ResourceDependency annotation.
            output.getAttributes().put(RequestViewMetadata.RESOURCE_DEPENDENCY_KEY, 
                new Object[]{annotation.library(), annotation.name()});
            
            // If target is the empty string, let target be null.
            String target = annotation.target();
            if (StringUtils.isNotEmpty(target))
            {
                target = ELText.parseAsString(getExpressionFactory(), context.getELContext(), target);
                // If target is non-null, store it under the key "target".
                attributes.put("target", target);
                context.getViewRoot().addComponentResource(context, output, target);
            }
            else
            {
                // Otherwise, if target is null, call 
                // UIViewRoot.addComponentResource(jakarta.faces.context.FacesContext, 
                // jakarta.faces.component.UIComponent), passing the UIOutput instance as the second argument.
                context.getViewRoot().addComponentResource(context, output);
            }
        }
    }

    @Override
    public final Validator createValidator(final String validatorId) throws FacesException
    {
        Assert.notEmpty(validatorId, "validatorId");

        Class<? extends Validator> validatorClass = getObjectFromClassMap(validatorId, _validatorClassMap);
        if (validatorClass == null)
        {
            String message = "Unknown validator id '" + validatorId + "'.";
            log.severe(message);
            throw new FacesException(message);
        }

        try
        {
            if (!_cdiManagedValidatorMap.containsKey(validatorClass))
            {
                FacesValidator annotation = validatorClass.getAnnotation(FacesValidator.class);
                if (annotation != null && annotation.managed())
                {
                    _cdiManagedValidatorMap.put(validatorClass, true);
                }
                else
                {
                    _cdiManagedValidatorMap.put(validatorClass, false);
                }
            }
            
            boolean managed = _cdiManagedValidatorMap.get(validatorClass);

            Validator validator = null;
            if (managed)
            {
                validator = new FacesValidatorCDIWrapper(validatorClass, validatorId);
                
                _handleAttachedResourceDependencyAnnotations(getFacesContext(), 
                        ((FacesWrapper<Validator>)validator).getWrapped());
            }
            else
            {
                validator = createValidatorInstance(validatorClass);
        
                _handleAttachedResourceDependencyAnnotations(getFacesContext(), validator);
            }
            
            return validator;
        }
        catch (Exception e)
        {
            log.log(Level.SEVERE, "Could not instantiate validator " + validatorClass, e);
            throw new FacesException("Could not instantiate validator: " + validatorClass, e);
        }
    }

    private Validator createValidatorInstance(Class<? extends Validator> validatorClass)
            throws InstantiationException, IllegalAccessException
    {
        return validatorClass.newInstance();
    }

    @Override
    public final String getDefaultRenderKitId()
    {
        return _defaultRenderKitId;
    }

    @Override
    public final void setDefaultRenderKitId(final String defaultRenderKitId)
    {
        _defaultRenderKitId = defaultRenderKitId;
    }

    @Override
    public final StateManager getStateManager()
    {
        return _stateManager;
    }

    @Override
    public final void setStateManager(final StateManager stateManager)
    {
        Assert.notNull(stateManager, "stateManager");

        if(isFirstRequestProcessed())
        {
            throw new IllegalStateException(
                    "setStateManager may not be executed after a lifecycle request has been completed");
        }
        
        _stateManager = stateManager;
    }
    
    @Override
    public final void setFlowHandler(FlowHandler flowHandler)
    {
        Assert.notNull(flowHandler, "flowHandler");

        if(isFirstRequestProcessed())
        {
            throw new IllegalStateException(
                    "setFlowHandler may not be executed after a lifecycle request has been completed");
        }
        _flowHandler = flowHandler;
    }

    @Override
    public final FlowHandler getFlowHandler()
    {
        return _flowHandler;
    }

    private void _handleAnnotations(FacesContext context, Object inspected, UIComponent component)
    {   
        // determine the ProjectStage setting via the given FacesContext
        // note that a local getProjectStage() could cause problems in wrapped environments
        boolean isProduction = context.isProjectStage(ProjectStage.Production);
        
        Class<?> inspectedClass = inspected.getClass();
        _handleListenerForAnnotations(context, inspected, inspectedClass, component, isProduction);

        _handleResourceDependencyAnnotations(context, inspectedClass, component, isProduction);
    }

    private void _handleRendererAnnotations(FacesContext context, Renderer inspected, UIComponent component)
    {   
        // determine the ProjectStage setting via the given FacesContext
        // note that a local getProjectStage() could cause problems in wrapped environments
        boolean isProduction = context.isProjectStage(ProjectStage.Production);
        Renderer innerRenderer = inspected;
        while (innerRenderer != null)
        {
            if (innerRenderer instanceof RendererWrapper)
            {
                Class<?> inspectedClass = innerRenderer.getClass();
                _handleListenerForAnnotations(context, innerRenderer, inspectedClass, component, isProduction);

                _handleResourceDependencyAnnotations(context, inspectedClass, component, isProduction);
                
                // get the inner wrapper
                innerRenderer = ((RendererWrapper)innerRenderer).getWrapped();
            }
            else
            {
                Class<?> inspectedClass = innerRenderer.getClass();
                _handleListenerForAnnotations(context, innerRenderer, inspectedClass, component, isProduction);

                _handleResourceDependencyAnnotations(context, inspectedClass, component, isProduction);
                
                innerRenderer = null;
            }
        }
    }

    private void _handleListenerForAnnotations(FacesContext context, Object inspected, Class<?> inspectedClass,
                                               UIComponent component, boolean isProduction)
    {
        // reset cache each time
        if (!isProduction)
        {
            _classToListenerForMap.remove(inspectedClass);
        }

        List<ListenerFor> listenerForList = _classToListenerForMap.get(inspectedClass);
        if (listenerForList == null)
        {
            listenerForList = new ArrayList<>(5);

            ListenerFor listener = inspectedClass.getAnnotation(ListenerFor.class);
            if (listener != null)
            {
                listenerForList.add(listener);
            }

            ListenersFor listeners = inspectedClass.getAnnotation(ListenersFor.class);
            if (listeners != null)
            {
                listenerForList.addAll(Arrays.asList(listeners.value()));
            }
            
            _classToListenerForMap.put(inspectedClass, 
                    listenerForList.isEmpty() ? Collections.emptyList() : listenerForList);
        }

        // listeners were found through inspection or from cache, handle them
        if (!listenerForList.isEmpty()) 
        {
            for (int i = 0, size = listenerForList.size(); i < size; i++)
            {
                ListenerFor listenerFor = listenerForList.get(i);
                _handleListenerFor(context, inspected, component, listenerFor);
            }
        }
    }

    private void _handleListenerFor(FacesContext context, Object inspected, UIComponent component,
                                    ListenerFor annotation)
    {
        // If this annotation is not present on the class in question, no action must be taken.
        if (annotation != null)
        {
            // Determine the "target" on which to call subscribeToEvent.
            // If the class to which this annotation is attached implements ComponentSystemEventListener
            if (inspected instanceof ComponentSystemEventListener)
            {
                // If the class to which this annotation is attached is a UIComponent instance, "target" is the
                // UIComponent instance.

                // If the class to which this annotation is attached is a Renderer instance, "target" is the
                // UIComponent instance.

                /*
                 * If "target" is a UIComponent call UIComponent.subscribeToEvent(Class, ComponentSystemEventListener)
                 * passing the systemEventClass() of the annotation as the first argument and the instance of the class
                 * to which this annotation is attached (which must implement ComponentSystemEventListener) as the
                 * second argument.
                 */
                component.subscribeToEvent(annotation.systemEventClass(), (ComponentSystemEventListener) inspected);
            }
            // If the class to which this annotation is attached implements SystemEventListener and does not implement
            // ComponentSystemEventListener, "target" is the Application instance.
            else if (component instanceof SystemEventListener)
            {
                // use the Application object from the FacesContext (note that a
                // direct use of subscribeToEvent() could cause problems if the
                // Application is wrapped)
                Application application = context.getApplication();
                
                // If "target" is the Application instance, inspect the value of the sourceClass() annotation attribute
                // value.
                if (Void.class.equals(annotation.sourceClass()))
                {
                    /*
                     * If the value is Void.class, call Application.subscribeToEvent(Class, SystemEventListener),
                     * passing the value of systemEventClass() as the first argument and the instance of the class to
                     * which this annotation is attached (which must implement SystemEventListener) as the second
                     * argument.
                     */
                    application.subscribeToEvent(annotation.systemEventClass(), (SystemEventListener) inspected);
                }
                else
                {
                    /*
                     * Otherwise, call Application.subscribeToEvent(Class, Class, SystemEventListener), passing the
                     * value of systemEventClass() as the first argument, the value of sourceClass() as the second
                     * argument, and the instance of the class to which this annotation is attached (which must
                     * implement SystemEventListener) as the third argument.
                     */
                    application.subscribeToEvent(annotation.systemEventClass(), annotation.sourceClass(),
                                     (SystemEventListener) inspected);
                }
            }

            /*
             * If the class to which this annotation is attached implements ComponentSystemEventListener and is neither
             * an instance of Renderer nor UIComponent, the action taken is unspecified. This case must not trigger any
             * kind of error.
             */
        }
    }

    private void _handleResourceDependencyAnnotations(FacesContext context, Class<?> inspectedClass,
                                                      UIComponent component, boolean isProduction)
    {
        // This and only this method handles @ResourceDependency and @ResourceDependencies annotations
        // The source of these annotations is Class<?> inspectedClass.
        // Because Class<?> and its annotations cannot change
        // during request/response, it is sufficient to process Class<?> only once per view.
        RequestViewContext rvc = RequestViewContext.getCurrentInstance(context);
        if (rvc.isClassAlreadyProcessed(inspectedClass))
        {
            return;
        }
        boolean classAlreadyProcessed = false;

        
        List<ResourceDependency> dependencyList = null;
        boolean isCachedList = false;
        
        if (isProduction)
        {
            dependencyList = _classToResourceDependencyMap.get(inspectedClass);

            if (dependencyList != null)
            {
                if (dependencyList.isEmpty())
                {
                    return; //class has been inspected and did not contain any resource dependency annotations
                }
                
                isCachedList = true;    // else annotations were found in the cache
            }
        }
        
        if (dependencyList == null)  //not in production or the class hasn't been inspected yet
        {   
            ResourceDependency dependency = inspectedClass.getAnnotation(ResourceDependency.class);
            ResourceDependencies dependencies = inspectedClass.getAnnotation(ResourceDependencies.class);
            if(dependency != null || dependencies != null)
            {
                //resource dependencies were found using one or both annotations, create and build a new list
                dependencyList = new ArrayList<>();
                
                if(dependency != null)
                {
                    dependencyList.add(dependency);
                }
                
                if(dependencies != null)
                {
                    dependencyList.addAll(Arrays.asList(dependencies.value()));
                }
            }
            else
            {
                dependencyList = Collections.emptyList();
            }
        }        
 
        // resource dependencies were found through inspection or from cache, handle them
        if (dependencyList != null && !dependencyList.isEmpty()) 
        {
            for (int i = 0, size = dependencyList.size(); i < size; i++)
            {
                ResourceDependency dependency = dependencyList.get(i);
                if (!rvc.isResourceDependencyAlreadyProcessed(dependency))
                {
                    _handleResourceDependency(context, component, dependency, inspectedClass);
                    rvc.setResourceDependencyAsProcessed(dependency);
                }
            }
        }
        
        //if we're in production and the list is not yet cached, store it
        if(isProduction && !isCachedList && dependencyList != null)   
        {
            // Note at this point listenerForList cannot be null, but just let dependencyList != null
            // as a verification check.
            _classToResourceDependencyMap.put(inspectedClass, dependencyList);
        }
        
        if (!classAlreadyProcessed)
        {
            rvc.setClassProcessed(inspectedClass);
        }
    }
    
    private void _handleResourceDependency(FacesContext context, UIComponent component, ResourceDependency annotation,
            Class<?> inspectedClass)
    {
        // If this annotation is not present on the class in question, no action must be taken.
        if (annotation != null)
        {
            // Create a UIOutput instance by passing jakarta.faces.Output. to
            // Application.createComponent(java.lang.String).
            UIOutput output = (UIOutput) createComponent(context, UIOutput.COMPONENT_TYPE, null);

            // Get the annotation instance from the class and obtain the values of the name, library, and
            // target attributes.
            String name = annotation.name();
            if (StringUtils.isNotEmpty(name))
            {
                name = ELText.parseAsString(getExpressionFactory(), context.getELContext(), name);
            }

            // Obtain the renderer-type for the resource name by passing name to
            // ResourceHandler.getRendererTypeForResourceName(java.lang.String).
            // (note that we can not use this.getResourceHandler(), because the Application might be wrapped)
            String rendererType = context.getApplication().getResourceHandler().getRendererTypeForResourceName(name);

            // Call setRendererType on the UIOutput instance, passing the renderer-type.
            output.setRendererType(rendererType);
            
            // If the @ResourceDependency was done inside facelets processing,
            // call setId() and set a proper id from facelets
            setResourceIdOnFaceletsMode(context, output, inspectedClass);

            // Obtain the Map of attributes from the UIOutput component by calling UIComponent.getAttributes().
            Map<String, Object> attributes = output.getAttributes();

            // Store the name into the attributes Map under the key "name".
            attributes.put("name", name);

            // If library is the empty string, let library be null.
            String library = annotation.library();
            if (StringUtils.isNotEmpty(library))
            {
                library = ELText.parseAsString(getExpressionFactory(), context.getELContext(), library);
                // If library is non-null, store it under the key "library".
                if ("this".equals(library))
                {
                    // Special "this" behavior
                    Resource resource = (Resource)component.getAttributes().get(Resource.COMPONENT_RESOURCE_KEY);
                    if (resource != null)
                    {
                        attributes.put("library", resource.getLibraryName());
                    }
                }
                else
                {
                    attributes.put("library", library);
                }
            }
            
            // Identify the resource as created by effect of a @ResourceDependency annotation.
            output.getAttributes().put(RequestViewMetadata.RESOURCE_DEPENDENCY_KEY, 
                new Object[]{annotation.library(), annotation.name()});

            // If target is the empty string, let target be null.
            String target = annotation.target();
            if (StringUtils.isNotEmpty(target))
            {
                target = ELText.parseAsString(getExpressionFactory(), context.getELContext(), target);
                // If target is non-null, store it under the key "target".
                attributes.put("target", target);
                context.getViewRoot().addComponentResource(context, output, target);
            }
            else
            {
                // Otherwise, if target is null, call
                // UIViewRoot.addComponentResource(jakarta.faces.context.FacesContext,
                // jakarta.faces.component.UIComponent), passing the UIOutput instance as the second argument.
                context.getViewRoot().addComponentResource(context, output);
            }
        }
    }
    
    private void _inspectRenderer(FacesContext context, UIComponent component,
                                  String componentType, String rendererType)
    {
        /*
         * The Renderer instance to inspect must be obtained by calling FacesContext.getRenderKit() and calling
         * RenderKit.getRenderer(java.lang.String, java.lang.String) on the result, passing the argument componentFamily
         * of the newly created component as the first argument and the argument rendererType as the second argument.
         */
        RenderKit renderKit = context.getRenderKit();
        if (renderKit == null)
        {
            // If no renderKit is set, it means we are on initialization step, skip this step.
            return;
        }
        Renderer renderer = renderKit.getRenderer(component.getFamily(), rendererType);
        if (renderer == null)
        {
            // If no such Renderer can be found, a message must be logged with a helpful error message.
            log.severe("renderer cannot be found for component type " + componentType + " and renderer type "
                    + rendererType);
        }
        else
        {
            // Otherwise, UIComponent.setRendererType(java.lang.String) must be called on the newly created
            // UIComponent instance, passing the argument rendererType as the argument.
            component.setRendererType(rendererType);

            /*
             * except the Renderer for the component to be returned must be inspected for the annotations mentioned in
             * createComponent(ValueExpression, FacesContext, String) as specified in the documentation for that method.
             */
            _handleRendererAnnotations(context, renderer, component);
        }
    }

    /**
     * Method to handle determining if the first request has 
     * been handled by the associated LifecycleImpl.
     * @return true if the first request has already been processed, false otherwise
     */
    private boolean isFirstRequestProcessed()
    {
        FacesContext context = getFacesContext();
        
        //if firstRequestProcessed is not set, check the application map
        if(!_firstRequestProcessed && context != null 
                && Boolean.TRUE.equals(context.getExternalContext().getApplicationMap()
                        .containsKey(LifecycleImpl.FIRST_REQUEST_PROCESSED_PARAM)))
        {
            _firstRequestProcessed = true;
        }
        return _firstRequestProcessed;
    }

    /*
     * private method to look for config objects on a classmap.  The objects can be either a type string
     * or a Class<?> object.  This is done to facilitate lazy loading of config objects.   
     * @param id 
     * @param classMap 
     * @return
     */
    private <T> Class<? extends T> getObjectFromClassMap(String id, Map<String, Object> classMap)
    {
        Object obj = classMap.get(id);
        
        if (obj == null)
        {
            return null;    //object for this id wasn't found on the map
        }
        
        if (obj instanceof Class<?>)
        {
            return (Class<? extends T>)obj;
        }
        else if (obj instanceof String )
        {
            Class<?> clazz = ClassUtils.simpleClassForName((String)obj);
            classMap.put(id, clazz);
            return (Class<? extends T>)clazz;
        }
        
        //object stored in the map for this id is an invalid type.  remove it and return null
        classMap.remove(id);
        return null;        
    }

    
    @Override
    public final void setSearchExpressionHandler(SearchExpressionHandler searchExpressionHandler)
    {
        Assert.notNull(searchExpressionHandler, "searchExpressionHandler");

        if(isFirstRequestProcessed())
        {
            throw new IllegalStateException(
                    "setFlowHandler may not be executed after a lifecycle request has been completed");
        }
        _searchExpressionHandler = searchExpressionHandler;
    }

    @Override
    public final SearchExpressionHandler getSearchExpressionHandler()
    {
        return _searchExpressionHandler;
    }

    @Override
    public SearchKeywordResolver getSearchKeywordResolver()
    {
        // we don't need synchronization here since it is ok to have multiple
        // instances of the elresolver
        if (_searchExpressionResolver == null)
        {
            _searchExpressionResolver = createSearchExpressionResolver();
        }
        return _searchExpressionResolver;
    }
    
    private SearchKeywordResolver createSearchExpressionResolver()
    {
        // Chain of responsibility pattern
        CompositeSearchKeywordResolver baseResolver = new CompositeSearchKeywordResolver();
        
        for (SearchKeywordResolver child : _runtimeConfig.getApplicationSearchExpressionResolvers())
        {
            baseResolver.add(child);
        }
        
        baseResolver.add(new ThisSearchKeywordResolver());
        baseResolver.add(new ParentSearchKeywordResolver());
        baseResolver.add(new ChildSearchKeywordResolver());
        baseResolver.add(new CompositeComponentParentSearchKeywordResolver());
        baseResolver.add(new FormSearchKeywordResolver());
        baseResolver.add(new NamingContainerSearchKeywordResolver());
        baseResolver.add(new NextSearchKeywordResolver());
        baseResolver.add(new NoneSearchKeywordResolver());
        baseResolver.add(new PreviousSearchKeywordResolver());
        baseResolver.add(new RootSearchKeywordResolver());
        baseResolver.add(new IdSearchKeywordResolver());
        baseResolver.add(new AllSearchKeywordResolver());
        
        return baseResolver;
    }

    @Override
    public void addSearchKeywordResolver(SearchKeywordResolver resolver)
    {
        if (isFirstRequestProcessed())
        {
            throw new IllegalStateException(
                    "It is illegal to add a search expression resolver after the first request is processed");
        }
        if (resolver != null)
        {
            _runtimeConfig.addApplicationSearchExpressionResolver(resolver);
        }
    }
}
