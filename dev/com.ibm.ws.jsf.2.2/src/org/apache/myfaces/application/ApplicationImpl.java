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

import javax.el.CompositeELResolver;
import javax.el.ELContext;
import javax.el.ELContextListener;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.application.NavigationHandler;
import javax.faces.application.ProjectStage;
import javax.faces.application.Resource;
import javax.faces.application.ResourceDependencies;
import javax.faces.application.ResourceDependency;
import javax.faces.application.ResourceHandler;
import javax.faces.application.StateManager;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.component.UINamingContainer;
import javax.faces.component.UIOutput;
import javax.faces.component.UIViewRoot;
import javax.faces.component.behavior.Behavior;
import javax.faces.component.behavior.ClientBehaviorBase;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.DateTimeConverter;
import javax.faces.el.MethodBinding;
import javax.faces.el.PropertyResolver;
import javax.faces.el.ReferenceSyntaxException;
import javax.faces.el.ValueBinding;
import javax.faces.el.VariableResolver;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionListener;
import javax.faces.event.ComponentSystemEventListener;
import javax.faces.event.ListenerFor;
import javax.faces.event.ListenersFor;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;
import javax.faces.event.SystemEventListenerHolder;
import javax.faces.flow.FlowHandler;
import javax.faces.render.ClientBehaviorRenderer;
import javax.faces.render.RenderKit;
import javax.faces.render.Renderer;
import javax.faces.render.RendererWrapper;
import javax.faces.validator.Validator;
import javax.faces.view.ViewDeclarationLanguage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.myfaces.application.cdi.ConverterWrapper;
import org.apache.myfaces.cdi.util.ExternalArtifactResolver;
import org.apache.myfaces.application.cdi.ValidatorWrapper;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFWebConfigParam;
import org.apache.myfaces.config.RuntimeConfig;
import org.apache.myfaces.config.element.Property;
import org.apache.myfaces.config.element.ResourceBundle;
import org.apache.myfaces.context.RequestViewContext;
import org.apache.myfaces.context.RequestViewMetadata;
import org.apache.myfaces.el.PropertyResolverImpl;
import org.apache.myfaces.el.VariableResolverToApplicationELResolverAdapter;
import org.apache.myfaces.el.convert.MethodExpressionToMethodBinding;
import org.apache.myfaces.el.convert.ValueBindingToValueExpression;
import org.apache.myfaces.el.convert.ValueExpressionToValueBinding;
import org.apache.myfaces.el.unified.ELResolverBuilder;
import org.apache.myfaces.el.unified.ResolverBuilderForFaces;
import org.apache.myfaces.el.unified.resolver.FacesCompositeELResolver;
import org.apache.myfaces.el.unified.resolver.FacesCompositeELResolver.Scope;
import org.apache.myfaces.flow.FlowHandlerImpl;
import org.apache.myfaces.lifecycle.LifecycleImpl;
import org.apache.myfaces.shared.config.MyfacesConfig;
import org.apache.myfaces.shared.util.ClassUtils;
import org.apache.myfaces.util.ExternalSpecifications;
import org.apache.myfaces.view.facelets.FaceletCompositionContext;
import org.apache.myfaces.view.facelets.el.ELText;
import org.apache.myfaces.webapp.AbstractFacesInitializer;

import com.ibm.ws.jsp.webcontainerext.JSPExtensionFactory;
import com.ibm.wsspi.el.ELFactoryWrapperForCDI;

/**
 * DOCUMENT ME!
 * 
 * @author Manfred Geiler (latest modification by $Author: lu4242 $)
 * @author Anton Koinov
 * @author Thomas Spiegl
 * @author Stan Silvert
 * @version $Revision: 1550609 $ $Date: 2013-12-13 01:18:08 +0000 (Fri, 13 Dec 2013) $
 */
@SuppressWarnings("deprecation")
public class ApplicationImpl extends Application
{
    //private static final Log log = LogFactory.getLog(ApplicationImpl.class);
    private static final Logger log = Logger.getLogger(ApplicationImpl.class.getName());

    private final static VariableResolver VARIABLERESOLVER = new VariableResolverToApplicationELResolverAdapter();

    private final static PropertyResolver PROPERTYRESOLVER = new PropertyResolverImpl();

    // the name for the system property which specifies the current ProjectStage (see MYFACES-2545 for details)
    public final static String PROJECT_STAGE_SYSTEM_PROPERTY_NAME = "faces.PROJECT_STAGE";
    
    // MyFaces specific System Property to set the ProjectStage, if not present via the standard way
    @Deprecated
    public final static String MYFACES_PROJECT_STAGE_SYSTEM_PROPERTY_NAME = "org.apache.myfaces.PROJECT_STAGE";
    
    /**
     * Indicate the stage of the initialized application.
     */
    @JSFWebConfigParam(defaultValue="Production",
            expectedValues="Development, Production, SystemTest, UnitTest",
            since="2.0")
    private static final String PROJECT_STAGE_PARAM_NAME = "javax.faces.PROJECT_STAGE";

    /**
     * Indicate if the classes associated to components, converters, validators or behaviors
     * should be loaded as soon as they are added to the current application instance or instead
     * loaded in a lazy way.
     */
    @JSFWebConfigParam(defaultValue="true",since="2.0",tags="performance")
    private static final String LAZY_LOAD_CONFIG_OBJECTS_PARAM_NAME = "org.apache.myfaces.LAZY_LOAD_CONFIG_OBJECTS";
    private static final boolean LAZY_LOAD_CONFIG_OBJECTS_DEFAULT_VALUE = true;
    private Boolean _lazyLoadConfigObjects = null;
    
    
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
    private final Map<String, Object> _converterIdToClassMap = new ConcurrentHashMap<String, Object>();

    private final Map<Class<?>, Object> _converterTargetClassToConverterClassMap
            = new ConcurrentHashMap<Class<?>, Object>();
    
    private final Map<String, Object> _componentClassMap = new ConcurrentHashMap<String, Object>();

    private final Map<String, Object> _validatorClassMap = new ConcurrentHashMap<String, Object>();

    private final Map<Class<? extends SystemEvent>, SystemListenerEntry> _systemEventListenerClassMap
            = new ConcurrentHashMap<Class<? extends SystemEvent>, SystemListenerEntry>();

    private final Map<String, String> _defaultValidatorsIds = new HashMap<String, String>();
    
    private volatile Map<String, String> _cachedDefaultValidatorsIds = null;
    
    private final Map<String, Object> _behaviorClassMap = new ConcurrentHashMap<String, Object>();

    private final RuntimeConfig _runtimeConfig;

    private ELResolver elResolver;

    private ELResolverBuilder resolverBuilderForFaces;

    private ProjectStage _projectStage;

    private volatile boolean _firstRequestProcessed = false;
    
    // MYFACES-3442 If HashMap or other non thread-safe structure is used, it is
    // possible to fall in a infinite loop under heavy load unless a synchronized block
    // is used to modify it or a ConcurrentHashMap.
    private final Map<Class<?>, List<ListenerFor>> _classToListenerForMap
            = new ConcurrentHashMap<Class<?>, List<ListenerFor>>() ;

    private final Map<Class<?>, List<ResourceDependency>> _classToResourceDependencyMap
            = new ConcurrentHashMap<Class<?>, List<ResourceDependency>>() ;
    
    private List<Class<? extends Converter>> _noArgConstructorConverterClasses 
            = new CopyOnWriteArrayList<Class<? extends Converter>>();
    
    /** Value of javax.faces.DATETIMECONVERTER_DEFAULT_TIMEZONE_IS_SYSTEM_TIMEZONE parameter */
    private boolean _dateTimeConverterDefaultTimeZoneIsSystemTimeZone = false; 
    
    private final ExternalArtifactResolver _externalArtifactResolver;
    
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
        this(getRuntimeConfig());
    }

    private static RuntimeConfig getRuntimeConfig()
    {
        return RuntimeConfig.getCurrentInstance(
                FacesContext.getCurrentInstance().getExternalContext());
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
        _elContextListeners = new ArrayList<ELContextListener>();
        _resourceHandler = new ResourceHandlerImpl();
        _flowHandler = new FlowHandlerImpl();
        _runtimeConfig = runtimeConfig;

        if (log.isLoggable(Level.FINEST))
        {
            log.finest("New Application instance created");
        }
        
        String configParam = getFaceContext().getExternalContext().
                getInitParameter(Converter.DATETIMECONVERTER_DEFAULT_TIMEZONE_IS_SYSTEM_TIMEZONE_PARAM_NAME);
        if (configParam != null && configParam.toLowerCase().equals("true"))
        {
            _dateTimeConverterDefaultTimeZoneIsSystemTimeZone = true;
        }
        
        if (ExternalSpecifications.isCDIAvailable(getFaceContext().getExternalContext()))
        {
            _externalArtifactResolver = (ExternalArtifactResolver) 
                ClassUtils.newInstance(
                    "org.apache.myfaces.cdi.util.CDIExternalArtifactResolver");
        }
        else
        {
            _externalArtifactResolver = null;
        }
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
        // we don't need synchronization here since it is ok to have multiple
        // instances of the elresolver
        if (elResolver == null)
        {
            elResolver = createFacesResolver();
        }
        return elResolver;
    }

    private ELResolver createFacesResolver()
    {
        boolean supportJSPAndFacesEL = MyfacesConfig.getCurrentInstance(
                                getFaceContext().getExternalContext()).isSupportJSPAndFacesEL();
        CompositeELResolver resolver;
        if (supportJSPAndFacesEL)
        {
            resolver = new FacesCompositeELResolver(Scope.Faces);
        }
        else
        {
            resolver = new CompositeELResolver();
        }
        getResolverBuilderForFaces().build(resolver);
        return resolver;
    }

    protected final ELResolverBuilder getResolverBuilderForFaces()
    {
        if (resolverBuilderForFaces == null)
        {
            resolverBuilderForFaces = new ResolverBuilderForFaces(_runtimeConfig);
        }
        return resolverBuilderForFaces;
    }

    public final void setResolverBuilderForFaces(final ELResolverBuilder factory)
    {
        resolverBuilderForFaces = factory;
    }

    @Override
    public final java.util.ResourceBundle getResourceBundle(final FacesContext facesContext, final String name)
            throws FacesException, NullPointerException
    {

        checkNull(facesContext, "facesContext");
        checkNull(name, "name");

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
            return getResourceBundle(bundleName, locale, getClassLoader());
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

    private ClassLoader getClassLoader()
    {
        return ClassUtils.getContextClassLoader();
    }

    String getBundleName(final FacesContext facesContext, final String name)
    {
        ResourceBundle bundle = getRuntimeConfig(facesContext).getResourceBundle(name);
        return bundle != null ? bundle.getBaseName() : null;
    }

    java.util.ResourceBundle getResourceBundle(final String name, final Locale locale, final ClassLoader loader)
            throws MissingResourceException
    {
        return java.util.ResourceBundle.getBundle(name, locale, loader);
    }

    final RuntimeConfig getRuntimeConfig(final FacesContext facesContext)
    {
        return RuntimeConfig.getCurrentInstance(facesContext.getExternalContext());
    }

    final FacesContext getFaceContext()
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

        checkNull(componentExpression, "componentExpression");
        checkNull(facesContext, "facesContext");
        checkNull(componentType, "componentType");

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
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        // Check to see if CDI feature is enabled AND the app has an active BeanManager
        if (ExternalSpecifications.isCDIAvailable(externalContext) &&
                        externalContext.getApplicationMap().get(AbstractFacesInitializer.CDI_BEAN_MANAGER_INSTANCE) != null) 
        {
            ExpressionFactory expressionFactory = null;
            // casting using a null could produce an NPE, so structure this code such that we 
            // won't throw an NPE on the cast.
            ELFactoryWrapperForCDI x = JSPExtensionFactory.getWrapperExpressionFactory();
            if (x != null) {
                expressionFactory = (ExpressionFactory) x;
            }    
            
            // The code above works fine with Open Web Beans, under which we expect to have 
            // a 'wrapped' expression factory in the service registry. However we're not yet
            // sure that Weld provides such a thing. So...
            
            if (expressionFactory == null) { 
                expressionFactory = _runtimeConfig.getExpressionFactory();
            }
            return expressionFactory;
        }
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
        checkNull(systemEventClass, "systemEventClass");
        checkNull(source, "source");
        
        //Call events only if event processing is enabled.
        if (!facesContext.isProcessingEvents())
        {
            return;
        }
        
        // spec: If this argument is null the return from source.getClass() must be used as the sourceBaseType. 
        if (sourceBaseType == null)
        {
            sourceBaseType = source.getClass();
        }
        
        try
        {
            SystemEvent event = null;
            if (source instanceof SystemEventListenerHolder)
            {
                SystemEventListenerHolder holder = (SystemEventListenerHolder) source;

                // If the source argument implements SystemEventListenerHolder, call
                // SystemEventListenerHolder.getListenersForEventClass(java.lang.Class) on it, passing the
                // systemEventClass
                // argument. If the list is not empty, perform algorithm traverseListenerList on the list.
                event = _traverseListenerList(holder.getListenersForEventClass(systemEventClass), systemEventClass,
                                              source, event);
            }
            
            UIViewRoot uiViewRoot = facesContext.getViewRoot();
            if (uiViewRoot != null)
            {
                //Call listeners on view level
                event = _traverseListenerListWithCopy(uiViewRoot.getViewListenersForEventClass(systemEventClass), 
                        systemEventClass, source, event);
            }

            SystemListenerEntry systemListenerEntry = _systemEventListenerClassMap.get(systemEventClass);
            if (systemListenerEntry != null)
            {
                systemListenerEntry.publish(systemEventClass, sourceBaseType, source, event);
            }
        }
        catch (AbortProcessingException e)
        {
            // If the act of invoking the processListener method causes an AbortProcessingException to be thrown,
            // processing of the listeners must be aborted, no further processing of the listeners for this event must
            // take place, and the exception must be logged with Level.SEVERE.
            log.log(Level.SEVERE, "Event processing was aborted", e);
        }
    }

    @Override
    public void publishEvent(FacesContext facesContext, Class<? extends SystemEvent> systemEventClass, Object source)
    {
        publishEvent(facesContext, systemEventClass, source.getClass(), source);
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
        checkNull(actionListener, "actionListener");

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
        checkNull(locale, "locale");

        _defaultLocale = locale;
        if (log.isLoggable(Level.FINEST))
        {
            log.finest("set defaultLocale = " + locale.getCountry() + " " + locale.getLanguage());
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
        checkNull(messageBundle, "messageBundle");

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
        checkNull(navigationHandler, "navigationHandler");

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

    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public final void setPropertyResolver(final PropertyResolver propertyResolver)
    {
        checkNull(propertyResolver, "propertyResolver");

        if (getFaceContext() != null)
        {
            throw new IllegalStateException("propertyResolver must be defined before request processing");
        }

        _runtimeConfig.setPropertyResolver(propertyResolver);

        if (log.isLoggable(Level.FINEST))
        {
            log.finest("set PropertyResolver = " + propertyResolver.getClass().getName());
        }
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
                // if not found check for the "old" System Property
                // and print a warning message to the log (just to be 
                // sure that everyone recognizes the change in the name).
                stageName = System.getProperty(MYFACES_PROJECT_STAGE_SYSTEM_PROPERTY_NAME);
                if (stageName != null)
                {
                    log.log(Level.WARNING, "The system property " + MYFACES_PROJECT_STAGE_SYSTEM_PROPERTY_NAME
                            + " has been replaced by " + PROJECT_STAGE_SYSTEM_PROPERTY_NAME + "!"
                            + " Please change your settings.");
                }
            }
            
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
                    //http://java.sun.com/javaee/6/docs/api/javax/faces/application/Application.html#getProjectStage()
                    //no-op
                }
            }

            /*
             * If found, continue with the algorithm below, otherwise, look for an entry in the initParamMap of the
             * ExternalContext from the current FacesContext with the key ProjectStage.PROJECT_STAGE_PARAM_NAME
             */
            if (stageName == null)
            {
                FacesContext context = FacesContext.getCurrentInstance();
                stageName = context.getExternalContext().getInitParameter(ProjectStage.PROJECT_STAGE_PARAM_NAME);
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

    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public final PropertyResolver getPropertyResolver()
    {
        return PROPERTYRESOLVER;
    }

    @Override
    public final void setResourceHandler(ResourceHandler resourceHandler)
    {
        checkNull(resourceHandler, "resourceHandler");

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
        checkNull(locales, "locales");

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

    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public final void setVariableResolver(final VariableResolver variableResolver)
    {
        checkNull(variableResolver, "variableResolver");

        if (isFirstRequestProcessed())
        {
            throw new IllegalStateException("variableResolver must be defined before request processing");
        }

        _runtimeConfig.setVariableResolver(variableResolver);

        if (log.isLoggable(Level.FINEST))
        {
            log.finest("set VariableResolver = " + variableResolver.getClass().getName());
        }
    }

    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public final VariableResolver getVariableResolver()
    {
        return VARIABLERESOLVER;
    }

    @Override
    public final void setViewHandler(final ViewHandler viewHandler)
    {
        checkNull(viewHandler, "viewHandler");

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
        subscribeToEvent(systemEventClass, null, listener);
    }

    @Override
    public void subscribeToEvent(Class<? extends SystemEvent> systemEventClass, Class<?> sourceClass,
                                 SystemEventListener listener)
    {
        checkNull(systemEventClass, "systemEventClass");
        checkNull(listener, "listener");

        SystemListenerEntry systemListenerEntry;
        synchronized (_systemEventListenerClassMap)
        {
            systemListenerEntry = _systemEventListenerClassMap.get(systemEventClass);
            if (systemListenerEntry == null)
            {
                systemListenerEntry = new SystemListenerEntry();
                _systemEventListenerClassMap.put(systemEventClass, systemListenerEntry);
            }
        }

        systemListenerEntry.addListener(listener, sourceClass);
    }
    
    @Override
    public void unsubscribeFromEvent(Class<? extends SystemEvent> systemEventClass, SystemEventListener listener)
    {
        unsubscribeFromEvent(systemEventClass, null, listener);
    }

    @Override
    public void unsubscribeFromEvent(Class<? extends SystemEvent> systemEventClass, Class<?> sourceClass,
                                     SystemEventListener listener)
    {
        checkNull(systemEventClass, "systemEventClass");
        checkNull(listener, "listener");

        SystemListenerEntry systemListenerEntry = _systemEventListenerClassMap.get(systemEventClass);
        if (systemListenerEntry != null)
        {
            systemListenerEntry.removeListener(listener, sourceClass);
        }
    }

    @Override
    public final ViewHandler getViewHandler()
    {
        return _viewHandler;
    }

    @Override
    public void addBehavior(String behaviorId, String behaviorClass)
    {
        checkNull(behaviorId, "behaviorId");
        checkEmpty(behaviorId, "behaviorId");
        checkNull(behaviorClass, "behaviorClass");
        checkEmpty(behaviorClass, "behaviorClass");

        try
        {
            if(isLazyLoadConfigObjects())
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
        checkNull(componentType, "componentType");
        checkEmpty(componentType, "componentType");
        checkNull(componentClassName, "componentClassName");
        checkEmpty(componentClassName, "componentClassName");

        try
        {
            if(isLazyLoadConfigObjects())
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
        checkNull(converterId, "converterId");
        checkEmpty(converterId, "converterId");
        checkNull(converterClass, "converterClass");
        checkEmpty(converterClass, "converterClass");

        try
        {
            if(isLazyLoadConfigObjects())
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
        checkNull(targetClass, "targetClass");
        checkNull(converterClass, "converterClass");
        checkEmpty(converterClass, "converterClass");

        try
        {
            if(isLazyLoadConfigObjects())
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
        checkNull(validatorId, "validatorId");
        checkEmpty(validatorId, "validatorId");
        checkNull(validatorClass, "validatorClass");
        checkEmpty(validatorClass, "validatorClass");

        try
        {
            if(isLazyLoadConfigObjects())
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
        checkNull(behaviorId, "behaviorId");
        checkEmpty(behaviorId, "behaviorId");

        final Class<? extends Behavior> behaviorClass =
                getObjectFromClassMap(behaviorId, _behaviorClassMap);
        
        if (behaviorClass == null)
        {
            throw new FacesException("Could not find any registered behavior-class for behaviorId : " + behaviorId);
        }
        
        try
        {
            Behavior behavior = behaviorClass.newInstance();
            FacesContext facesContext = FacesContext.getCurrentInstance();
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
        checkNull(context, "context");
        checkNull(componentResource, "componentResource");
        
        UIComponent component = null;
        Resource resource;
        String fqcn;
        Class<? extends UIComponent> componentClass = null;

        /*
         * Obtain a reference to the ViewDeclarationLanguage for this Application instance by calling
         * ViewHandler.getViewDeclarationLanguage(javax.faces.context.FacesContext, java.lang.String), passing the
         * viewId found by calling UIViewRoot.getViewId() on the UIViewRoot in the argument FacesContext.
         */
        UIViewRoot view = context.getViewRoot();
        Application application = context.getApplication();
        ViewDeclarationLanguage vdl
                = application.getViewHandler().getViewDeclarationLanguage(context, view.getViewId());

        /*
         * Obtain a reference to the composite component metadata for this composite component by calling
         * ViewDeclarationLanguage.getComponentMetadata(javax.faces.context.FacesContext,
         * javax.faces.application.Resource), passing the facesContext and componentResource arguments to this method.
         * This version of JSF specification uses JavaBeans as the API to the component metadata.
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
             * ViewDeclarationLanguage.getScriptComponentResource(javax.faces.context.FacesContext,
             * javax.faces.application.Resource). If the result is non-null, and is a script written in one of the
             * languages listed in JSF 4.3 of the specification prose document, create a UIComponent instance from the
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
                fqcn = componentResource.getLibraryName() + "." + className;
                
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
                 * createComponent(java.lang.String) passing "javax.faces.NamingContainer" as the argument.
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
         * "javax.faces.Composite" as the argument.
         */
        component.setRendererType("javax.faces.Composite");

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
        checkNull(context, "context");
        checkNull(componentType, "componentType");

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
        checkNull(componentType, "componentType");
        checkEmpty(componentType, "componentType");

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
        checkNull(componentType, "componentType");
        checkEmpty(componentType, "componentType");

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
            _handleAnnotations(FacesContext.getCurrentInstance(), component, component);
            return component;
        }
        catch (Exception e)
        {
            log.log(Level.SEVERE, "Could not instantiate component componentType = " + componentType, e);
            throw new FacesException("Could not instantiate component componentType = " + componentType, e);
        }
    }

    /**
     * @deprecated Use createComponent(ValueExpression, FacesContext, String) instead.
     */
    @Deprecated
    @Override
    public final UIComponent createComponent(final ValueBinding valueBinding, final FacesContext facesContext,
                                             final String componentType) throws FacesException
    {

        checkNull(valueBinding, "valueBinding");
        checkNull(facesContext, "facesContext");
        checkNull(componentType, "componentType");
        checkEmpty(componentType, "componentType");

        final ValueExpression valExpression = new ValueBindingToValueExpression(valueBinding);

        return createComponent(valExpression, facesContext, componentType);
    }

    /**
     * Return an instance of the converter class that has been registered under the specified id.
     * <p>
     * Converters are registered via faces-config.xml files, and can also be registered via the addConverter(String id,
     * Class converterClass) method on this class. Here the the appropriate Class definition is found, then an instance
     * is created and returned.
     * <p>
     * A converter registered via a config file can have any number of nested attribute or property tags. The JSF
     * specification is very vague about what effect these nested tags have. This method ignores nested attribute
     * definitions, but for each nested property tag the corresponding setter is invoked on the new Converter instance
     * passing the property's defaultValuer. Basic typeconversion is done so the target properties on the Converter
     * instance can be String, int, boolean, etc. Note that:
     * <ol>
     * <li>the Sun Mojarra JSF implemenation ignores nested property tags completely, so this behaviour cannot be 
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
        checkNull(converterId, "converterId");
        checkEmpty(converterId, "converterId");

        final Class<? extends Converter> converterClass =
                getObjectFromClassMap(converterId, _converterIdToClassMap);
        if (converterClass == null)
        {
            throw new FacesException("Could not find any registered converter-class by converterId : " + converterId);
        }

        try
        {
            final Converter converter = createConverterInstance(converterClass);

            setConverterProperties(converterClass, converter);
            
            _handleAttachedResourceDependencyAnnotations(FacesContext.getCurrentInstance(), converter);

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
        Converter result = _externalArtifactResolver != null ? 
            _externalArtifactResolver.resolveManagedConverter(converterClass) : null;

        if (result == null)
        {
            return converterClass.newInstance();
        }
        else
        {
            return new ConverterWrapper(result);
        }
    }

    @Override
    public final Converter createConverter(final Class<?> targetClass)
    {
        checkNull(targetClass, "targetClass");

        return internalCreateConverter(targetClass);
    }

    @SuppressWarnings("unchecked")
    private Converter internalCreateConverter(final Class<?> targetClass)
    {
        // Locate a Converter registered for the target class itself.
        Object converterClassOrClassName = _converterTargetClassToConverterClassMap.get(targetClass);
        
        // Locate a Converter registered for interfaces that are
        // implemented by the target class (directly or indirectly).
        // Skip if class is String, for performance reasons 
        // (save 3 additional lookups over a concurrent map per request). 
        if (converterClassOrClassName == null && !String.class.equals(targetClass))
        {
            final Class<?> interfaces[] = targetClass.getInterfaces();
            if (interfaces != null)
            {
                for (int i = 0, len = interfaces.length; i < len; i++)
                {
                    // search all superinterfaces for a matching converter,
                    // create it
                    final Converter converter = internalCreateConverter(interfaces[i]);
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
                
                Converter converter = null;
                
                // check cached constructor information
                if (!_noArgConstructorConverterClasses.contains(converterClass))
                {
                    // the converter class either supports the one-arg constructor
                    // or has never been processed before
                    try
                    {
                        // look for a constructor that takes a single Class object
                        // See JSF 1.2 javadoc for Converter
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

        if (converterConfig != null && converterConfig.getProperties().size() > 0)
        {
            for (Property property : converterConfig.getProperties())
            {
                try
                {
                    BeanUtils.setProperty(converter, property.getPropertyName(), property.getDefaultValue());
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
        boolean classAlreadyProcessed = false;

        
        List<ResourceDependency> dependencyList = null;
        boolean isCachedList = false;
        
        if(context.isProjectStage(ProjectStage.Production) && _classToResourceDependencyMap.containsKey(inspectedClass))
        {
            dependencyList = _classToResourceDependencyMap.get(inspectedClass);
            if(dependencyList == null)
            {
                return; //class has been inspected and did not contain any resource dependency annotations
            }
            else if (dependencyList.isEmpty())
            {
                return;
            }
            
            isCachedList = true;    // else annotations were found in the cache
        }
        
        if(dependencyList == null)  //not in production or the class hasn't been inspected yet
        {   
            ResourceDependency dependency = inspectedClass.getAnnotation(ResourceDependency.class);
            ResourceDependencies dependencies = inspectedClass.getAnnotation(ResourceDependencies.class);
            if(dependency != null || dependencies != null)
            {
                //resource dependencies were found using one or both annotations, create and build a new list
                dependencyList = new ArrayList<ResourceDependency>();
                
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

        //resource dependencies were found through inspection or from cache, handle them
        if (dependencyList != null && !dependencyList.isEmpty()) 
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
        
        //if we're in production and the list is not yet cached, store it
        if(context.isProjectStage(ProjectStage.Production) && !isCachedList && dependencyList != null)
        {
            // Note at this point dependencyList cannot be null, but just let this
            // as a sanity check.
            _classToResourceDependencyMap.put(inspectedClass, dependencyList);
        }
        
        if (!classAlreadyProcessed)
        {
            rvc.setClassProcessed(inspectedClass);
        }
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
            
            // Create a UIOutput instance by passing javax.faces.Output. to 
            // Application.createComponent(java.lang.String).
            UIOutput output = (UIOutput) application.createComponent(context, UIOutput.COMPONENT_TYPE, null);
            
            // Get the annotation instance from the class and obtain the values of the name, library, and 
            // target attributes.
            String name = annotation.name();
            if (name != null && name.length() > 0)
            {
                name = ELText.parse(getExpressionFactory(),
                                    context.getELContext(), name).toString(context.getELContext());
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
            if (library != null && library.length() > 0)
            {
                library = ELText.parse(getExpressionFactory(),
                                       context.getELContext(), library).toString(context.getELContext());
                // If library is non-null, store it under the key "library".
                attributes.put("library", library);
            }
            
            // Identify the resource as created by effect of a @ResourceDependency annotation.
            output.getAttributes().put(RequestViewMetadata.RESOURCE_DEPENDENCY_KEY, 
                new Object[]{annotation.library(), annotation.name()});
            
            // If target is the empty string, let target be null.
            String target = annotation.target();
            if (target != null && target.length() > 0)
            {
                target = ELText.parse(getExpressionFactory(),
                                      context.getELContext(), target).toString(context.getELContext());
                // If target is non-null, store it under the key "target".
                attributes.put("target", target);
                context.getViewRoot().addComponentResource(context, output, target);
            }
            else
            {
                // Otherwise, if target is null, call 
                // UIViewRoot.addComponentResource(javax.faces.context.FacesContext, 
                // javax.faces.component.UIComponent), passing the UIOutput instance as the second argument.
                context.getViewRoot().addComponentResource(context, output);
            }
        }
    }

    // Note: this method used to be synchronized in the JSF 1.1 version. Why?
    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public final MethodBinding createMethodBinding(final String reference, Class<?>[] params)
            throws ReferenceSyntaxException
    {
        checkNull(reference, "reference");
        checkEmpty(reference, "reference");

        // TODO: this check should be performed by the expression factory. It is
        // a requirement of the TCK
        if (!(reference.startsWith("#{") && reference.endsWith("}")))
        {
            throw new ReferenceSyntaxException("Invalid method reference: '" + reference + "'");
        }

        if (params == null)
        {
            params = new Class[0];
        }

        MethodExpression methodExpression;

        try
        {
            methodExpression = getExpressionFactory().createMethodExpression(threadELContext(), reference,
                                                                             Object.class, params);
        }
        catch (ELException e)
        {
            throw new ReferenceSyntaxException(e);
        }

        return new MethodExpressionToMethodBinding(methodExpression);
    }

    @Override
    public final Validator createValidator(final String validatorId) throws FacesException
    {
        checkNull(validatorId, "validatorId");
        checkEmpty(validatorId, "validatorId");

        Class<? extends Validator> validatorClass =
                getObjectFromClassMap(validatorId, _validatorClassMap);
        if (validatorClass == null)
        {
            String message = "Unknown validator id '" + validatorId + "'.";
            log.severe(message);
            throw new FacesException(message);
        }

        try
        {
            Validator validator = createValidatorInstance(validatorClass);
            
            _handleAttachedResourceDependencyAnnotations(FacesContext.getCurrentInstance(), validator);
            
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
        Validator result = _externalArtifactResolver != null ? 
            _externalArtifactResolver.resolveManagedValidator(validatorClass) : null;

        if (result == null)
        {
            return validatorClass.newInstance();
        }
        else
        {
            return new ValidatorWrapper(result);
        }
    }

    /**
     * @deprecated
     */
    @Override
    public final ValueBinding createValueBinding(final String reference) throws ReferenceSyntaxException
    {
        checkNull(reference, "reference");
        checkEmpty(reference, "reference");

        ValueExpression valueExpression;

        try
        {
            valueExpression = getExpressionFactory().createValueExpression(
                    threadELContext(), reference, Object.class);
        }
        catch (ELException e)
        {
            throw new ReferenceSyntaxException(e);
        }

        return new ValueExpressionToValueBinding(valueExpression);
    }

    // gets the elContext from the current FacesContext()
    private final ELContext threadELContext()
    {
        return getFaceContext().getELContext();
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
        checkNull(stateManager, "stateManager");

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
        checkNull(flowHandler, "flowHandler");

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

    private void checkNull(final Object param, final String paramName)
    {
        if (param == null)
        {
            throw new NullPointerException(paramName + " cannot be null.");
        }
    }

    private void checkEmpty(final String param, final String paramName)
    {
        if (param.length() == 0)
        {
            throw new NullPointerException("String " + paramName + " cannot be empty.");
        }
    }

    private static SystemEvent _createEvent(Class<? extends SystemEvent> systemEventClass, Object source,
                                            SystemEvent event)
    {
        if (event == null)
        {
            try
            {
                Constructor<?>[] constructors = systemEventClass.getConstructors();
                Constructor<? extends SystemEvent> constructor = null;
                for (Constructor<?> c : constructors)
                {
                    if (c.getParameterTypes().length == 1)
                    {
                        // Safe cast, since the constructor belongs
                        // to a class of type SystemEvent
                        constructor = (Constructor<? extends SystemEvent>) c;
                        break;
                    }
                }
                if (constructor != null)
                {
                    event = constructor.newInstance(source);
                }

            }
            catch (Exception e)
            {
                throw new FacesException("Couldn't instanciate system event of type " + 
                        systemEventClass.getName(), e);
            }
        }

        return event;
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
        List<ListenerFor> listenerForList = null;
        boolean isCachedList = false;
        
        if(isProduction)
        {
            listenerForList = _classToListenerForMap.get(inspectedClass);

            if (listenerForList != null)
            {
                if (listenerForList.isEmpty())
                {
                    return; //class has been inspected and did not contain any listener annotations
                }
                
                isCachedList = true;    // else annotations were found in the cache
            }
        }

        if(listenerForList == null) //not in production or the class hasn't been inspected yet
        {
            ListenerFor listener = inspectedClass.getAnnotation(ListenerFor.class);
            ListenersFor listeners = inspectedClass.getAnnotation(ListenersFor.class);
            if(listener != null || listeners != null)
            {
                //listeners were found using one or both annotations, create and build a new list
                listenerForList = new ArrayList<ListenerFor>();
                
                if(listener != null)
                {
                    listenerForList.add(listener);
                }
                
                if(listeners != null)
                {
                    listenerForList.addAll(Arrays.asList(listeners.value()));
                }
            }
            else
            {
                listenerForList = Collections.emptyList();
            }
        }        
 
        // listeners were found through inspection or from cache, handle them
        if (listenerForList != null && !listenerForList.isEmpty()) 
        {
            for (int i = 0, size = listenerForList.size(); i < size; i++)
            {
                ListenerFor listenerFor = listenerForList.get(i);
                _handleListenerFor(context, inspected, component, listenerFor);
            }
        }
        
        //if we're in production and the list is not yet cached, store it
        if(isProduction && !isCachedList && listenerForList != null) 
        {
            // Note at this point listenerForList cannot be null, but just let listenerForList != null
            // as a sanity check.
            _classToListenerForMap.put(inspectedClass, listenerForList);
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
        
        if(isProduction)
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
        
        if(dependencyList == null)  //not in production or the class hasn't been inspected yet
        {   
            ResourceDependency dependency = inspectedClass.getAnnotation(ResourceDependency.class);
            ResourceDependencies dependencies = inspectedClass.getAnnotation(ResourceDependencies.class);
            if(dependency != null || dependencies != null)
            {
                //resource dependencies were found using one or both annotations, create and build a new list
                dependencyList = new ArrayList<ResourceDependency>();
                
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
            // as a sanity check.
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
            // Create a UIOutput instance by passing javax.faces.Output. to
            // Application.createComponent(java.lang.String).
            UIOutput output = (UIOutput) createComponent(context, UIOutput.COMPONENT_TYPE, null);

            // Get the annotation instance from the class and obtain the values of the name, library, and
            // target attributes.
            String name = annotation.name();
            if (name != null && name.length() > 0)
            {
                name = ELText.parse(getExpressionFactory(),
                                    context.getELContext(), name).toString(context.getELContext());
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
            if (library != null && library.length() > 0)
            {
                library = ELText.parse(getExpressionFactory(),
                                       context.getELContext(), library).toString(context.getELContext());
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
            if (target != null && target.length() > 0)
            {
                target = ELText.parse(getExpressionFactory(),
                                      context.getELContext(), target).toString(context.getELContext());
                // If target is non-null, store it under the key "target".
                attributes.put("target", target);
                context.getViewRoot().addComponentResource(context, output, target);
            }
            else
            {
                // Otherwise, if target is null, call UIViewRoot.addComponentResource(javax.faces.context.FacesContext,
                // javax.faces.component.UIComponent), passing the UIOutput instance as the second argument.
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

    private static SystemEvent _traverseListenerList(List<? extends SystemEventListener> listeners,
                                                     Class<? extends SystemEvent> systemEventClass, Object source,
                                                     SystemEvent event)
    {
        if (listeners != null && !listeners.isEmpty())
        {
            // perf: org.apache.myfaces.application.ApplicationImpl.
            //    SystemListenerEntry.getSpecificSourceListenersNotNull(Class<?>)
            // or javax.faces.component.UIComponent.subscribeToEvent(
            //      Class<? extends SystemEvent>, ComponentSystemEventListener)
            // creates a ArrayList:
            for (int i  = 0, size = listeners.size(); i < size; i++)
            {
                SystemEventListener listener = listeners.get(i);
                // Call SystemEventListener.isListenerForSource(java.lang.Object), passing the source argument.
                // If this returns false, take no action on the listener.
                if (listener.isListenerForSource(source))
                {
                    // Otherwise, if the event to be passed to the listener instances has not yet been constructed,
                    // construct the event, passing source as the argument to the one-argument constructor that takes
                    // an Object. This same event instance must be passed to all listener instances.
                    event = _createEvent(systemEventClass, source, event);

                    // Call SystemEvent.isAppropriateListener(javax.faces.event.FacesListener), passing the listener
                    // instance as the argument. If this returns false, take no action on the listener.
                    if (event.isAppropriateListener(listener))
                    {
                        // Call SystemEvent.processListener(javax.faces.event.FacesListener), passing the listener
                        // instance.
                        event.processListener(listener);
                    }
                }
            }
        }

        return event;
    }
    
    private static SystemEvent _traverseListenerListWithCopy(List<? extends SystemEventListener> listeners,
            Class<? extends SystemEvent> systemEventClass, Object source,
            SystemEvent event)
    {
        if (listeners != null && !listeners.isEmpty())
        {
            List<SystemEventListener> listenersCopy = new ArrayList<SystemEventListener>();
            int processedListenerIndex = 0;
            
            for (int i = 0; i < listeners.size(); i++)
            {
                listenersCopy.add(listeners.get(i));
            }
            
            // If the inner for is succesful, processedListenerIndex == listenersCopy.size()
            // and the loop will be complete.
            while (processedListenerIndex < listenersCopy.size())
            {                
                for (; processedListenerIndex < listenersCopy.size(); processedListenerIndex++ )
                {
                    SystemEventListener listener = listenersCopy.get(processedListenerIndex);
                    // Call SystemEventListener.isListenerForSource(java.lang.Object), passing the source argument.
                    // If this returns false, take no action on the listener.
                    if (listener.isListenerForSource(source))
                    {
                        // Otherwise, if the event to be passed to the listener instances has not yet been constructed,
                        // construct the event, passing source as the argument
                        // to the one-argument constructor that takes
                        // an Object. This same event instance must be passed to all listener instances.
                        event = _createEvent(systemEventClass, source, event);
    
                        // Call SystemEvent.isAppropriateListener(javax.faces.event.FacesListener), passing the listener
                        // instance as the argument. If this returns false, take no action on the listener.
                        if (event.isAppropriateListener(listener))
                        {
                            // Call SystemEvent.processListener(javax.faces.event.FacesListener), passing the listener
                            // instance.
                            event.processListener(listener);
                        }
                    }
                }
                
                boolean listChanged = false;
                if (listeners.size() == listenersCopy.size())
                {
                    for (int i = 0; i < listenersCopy.size(); i++)
                    {
                        if (listenersCopy.get(i) != listeners.get(i))
                        {
                            listChanged = true;
                            break;
                        }
                    }
                }
                else
                {
                    listChanged = true;
                }
                
                if (listChanged)
                {
                    for (int i = 0; i < listeners.size(); i++)
                    {
                        SystemEventListener listener = listeners.get(i);
                        
                        // check if listenersCopy.get(i) is valid
                        if (i < listenersCopy.size())
                        {
                            // The normal case is a listener was added, 
                            // so as heuristic, check first
                            // if we can find it at the same location
                            if (!listener.equals(listenersCopy.get(i)) &&
                                !listenersCopy.contains(listener))
                            {
                                listenersCopy.add(listener);
                            }
                        }
                        else
                        {
                            if (!listenersCopy.contains(listener))
                            {
                                listenersCopy.add(listener);
                            }
                        }
                    }
                }
            }
        }

        return event;
    }

    /**
     * Method to handle determining if the first request has 
     * been handled by the associated LifecycleImpl.
     * @return true if the first request has already been processed, false otherwise
     */
    private boolean isFirstRequestProcessed()
    {
        FacesContext context = FacesContext.getCurrentInstance();
        
        //if firstRequestProcessed is not set, check the application map
        if(!_firstRequestProcessed && context != null 
                && Boolean.TRUE.equals(context.getExternalContext().getApplicationMap()
                        .containsKey(LifecycleImpl.FIRST_REQUEST_PROCESSED_PARAM)))
        {
            _firstRequestProcessed = true;
        }
        return _firstRequestProcessed;
    }
    
    private static class SystemListenerEntry
    {
        private List<SystemEventListener> _lstSystemEventListener;
        private Map<Class<?>, List<SystemEventListener>> _sourceClassMap;

        public SystemListenerEntry()
        {
        }

        public void addListener(SystemEventListener listener)
        {
            assert listener != null;

            addListenerNoDuplicate(getAnySourceListenersNotNull(), listener);
        }

        public void addListener(SystemEventListener listener, Class<?> source)
        {
            assert listener != null;

            if (source == null)
            {
                addListener(listener);
            }
            else
            {
                addListenerNoDuplicate(getSpecificSourceListenersNotNull(source), listener);
            }
        }

        public void removeListener(SystemEventListener listener)
        {
            assert listener != null;

            if (_lstSystemEventListener != null)
            {
                _lstSystemEventListener.remove(listener);
            }
        }

        public void removeListener(SystemEventListener listener, Class<?> sourceClass)
        {
            assert listener != null;

            if (sourceClass == null)
            {
                removeListener(listener);
            }
            else
            {
                if (_sourceClassMap != null)
                {
                    List<SystemEventListener> listeners = _sourceClassMap.get(sourceClass);
                    if (listeners != null)
                    {
                        listeners.remove(listener);
                    }
                }
            }
        }

        public void publish(Class<? extends SystemEvent> systemEventClass, Class<?> classSource, Object source,
                            SystemEvent event)
        {
            if (source != null && _sourceClassMap != null)
            {
                event = _traverseListenerList(_sourceClassMap.get(classSource), systemEventClass, source, event);
            }

            _traverseListenerList(_lstSystemEventListener, systemEventClass, source, event);
        }

        private void addListenerNoDuplicate(List<SystemEventListener> listeners, SystemEventListener listener)
        {
            if (!listeners.contains(listener))
            {
                listeners.add(listener);
            }
        }

        private synchronized List<SystemEventListener> getAnySourceListenersNotNull()
        {
            if (_lstSystemEventListener == null)
            {
                /*
                 * TODO: Check if modification occurs often or not, might have to use a synchronized list instead.
                 * 
                 * Registrations found:
                 */
                _lstSystemEventListener = new CopyOnWriteArrayList<SystemEventListener>();
            }

            return _lstSystemEventListener;
        }

        private synchronized List<SystemEventListener> getSpecificSourceListenersNotNull(Class<?> sourceClass)
        {
            if (_sourceClassMap == null)
            {
                _sourceClassMap = new ConcurrentHashMap<Class<?>, List<SystemEventListener>>();
            }

            List<SystemEventListener> list = _sourceClassMap.get(sourceClass);
            if (list == null)
            {
                /*
                 * TODO: Check if modification occurs often or not, might have to use a synchronized list instead.
                 * 
                 * Registrations found:
                 */
                list = new CopyOnWriteArrayList<SystemEventListener>();
                _sourceClassMap.put(sourceClass, list);
            }

            return list;
        }
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
        
        if(obj == null)
        {
            return null;    //object for this id wasn't found on the map
        }
        
        if(obj instanceof Class<?>)
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
    
    private boolean isLazyLoadConfigObjects()
    {
        if (_lazyLoadConfigObjects == null)
        {
            String configParam
                    = getFaceContext().getExternalContext().getInitParameter(LAZY_LOAD_CONFIG_OBJECTS_PARAM_NAME);
            _lazyLoadConfigObjects = configParam == null
                                     ? LAZY_LOAD_CONFIG_OBJECTS_DEFAULT_VALUE
                                     : Boolean.parseBoolean(configParam);
        }
        return _lazyLoadConfigObjects;
    }
}
