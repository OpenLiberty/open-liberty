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
package org.apache.myfaces.webapp;

import org.apache.myfaces.config.FacesConfigValidator;
import org.apache.myfaces.config.FacesConfigurator;
import org.apache.myfaces.config.RuntimeConfig;
import org.apache.myfaces.context.servlet.StartupFacesContextImpl;
import org.apache.myfaces.context.servlet.StartupServletExternalContextImpl;
import org.apache.myfaces.application.FacesServletMappingUtils;
import org.apache.myfaces.context.ExceptionHandlerImpl;
import org.apache.myfaces.application.viewstate.StateUtils;
import org.apache.myfaces.util.WebConfigParamUtils;
import org.apache.myfaces.cdi.util.BeanEntry;
import org.apache.myfaces.spi.InjectionProvider;
import org.apache.myfaces.spi.InjectionProviderException;
import org.apache.myfaces.spi.InjectionProviderFactory;
import org.apache.myfaces.util.ExternalSpecifications;
import org.apache.myfaces.view.facelets.tag.MetaRulesetImpl;

import jakarta.el.ExpressionFactory;
import jakarta.faces.application.Application;
import jakarta.faces.application.ProjectStage;
import jakarta.faces.component.UIViewRoot;
import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.PostConstructApplicationEvent;
import jakarta.faces.event.PreDestroyApplicationEvent;
import jakarta.faces.event.SystemEvent;
import jakarta.servlet.ServletContext;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.faces.FacesException;
import jakarta.faces.FactoryFinder;
import jakarta.faces.application.ViewVisitOption;
import jakarta.faces.push.PushContext;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpointConfig;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ServiceLoader;
import org.apache.myfaces.cdi.util.CDIUtils;
import org.apache.myfaces.config.MyfacesConfig;
import org.apache.myfaces.config.annotation.CdiAnnotationProviderExtension;
import org.apache.myfaces.push.EndpointImpl;
import org.apache.myfaces.push.WebsocketConfigurator;
import org.apache.myfaces.push.WebsocketFacesInit;
import org.apache.myfaces.util.lang.ClassUtils;
import org.apache.myfaces.spi.ServiceProviderFinder;
import org.apache.myfaces.spi.ServiceProviderFinderFactory;
import org.apache.myfaces.view.facelets.ViewPoolProcessor;
import org.apache.myfaces.util.lang.StringUtils;

/**
 * Performs common initialization tasks.
 */
public class FacesInitializerImpl implements FacesInitializer
{
    private static final Logger log = Logger.getLogger(FacesInitializerImpl.class.getName());

    public static final String CDI_BEAN_MANAGER_INSTANCE = "oam.cdi.BEAN_MANAGER_INSTANCE";
    
    private static final String CDI_SERVLET_CONTEXT_BEAN_MANAGER_ATTRIBUTE = 
        "jakarta.enterprise.inject.spi.BeanManager";

    public static final String INJECTED_BEAN_STORAGE_KEY = "org.apache.myfaces.spi.BEAN_ENTRY_STORAGE";

    public static final String INITIALIZED = "org.apache.myfaces.INITIALIZED";
    
    private static final byte FACES_INIT_PHASE_PREINIT = 0;
    private static final byte FACES_INIT_PHASE_POSTINIT = 1;
    private static final byte FACES_INIT_PHASE_PREDESTROY = 2;
    private static final byte FACES_INIT_PHASE_POSTDESTROY = 3;
    
    /**
     * Performs all necessary initialization tasks like configuring this JSF
     * application.
     * 
     * @param servletContext The current {@link ServletContext}
     */
    @Override
    public void initFaces(ServletContext servletContext)
    {
        if (Boolean.TRUE.equals(servletContext.getAttribute(INITIALIZED)))
        {
            if (log.isLoggable(Level.FINEST))
            {
                log.finest("MyFaces already initialized");
            }
            return;
        }
        
        try
        {
            if (log.isLoggable(Level.FINEST))
            {
                log.finest("Initializing MyFaces");
            }
            
            long start = System.currentTimeMillis();

            // Some parts of the following configuration tasks have been implemented 
            // by using an ExternalContext. However, that's no problem as long as no 
            // one tries to call methods depending on either the ServletRequest or 
            // the ServletResponse.
            // JSF 2.0: FacesInitializer now has some new methods to
            // use proper startup FacesContext and ExternalContext instances.
            FacesContext facesContext = initStartupFacesContext(servletContext);
            ExternalContext externalContext = facesContext.getExternalContext();
            
            dispatchInitializationEvent(servletContext, FACES_INIT_PHASE_PREINIT);

            // Setup ServiceProviderFinder
            ServiceProviderFinder spf = ServiceProviderFinderFactory.getServiceProviderFinder(externalContext);
            Map<String, List<String>> spfConfig = spf.calculateKnownServiceProviderMapInfo(
                externalContext, ServiceProviderFinder.KNOWN_SERVICES);
            if (spfConfig != null)
            {
                spf.initKnownServiceProviderMapInfo(externalContext, spfConfig);
            }
            
            // Parse and validate the web.xml configuration file
            
            if (!WebConfigParamUtils.getBooleanInitParameter(externalContext,
                    MyfacesConfig.INITIALIZE_ALWAYS_STANDALONE, false))
            {
                FacesServletMappingUtils.ServletRegistrationInfo facesServletRegistration =
                        FacesServletMappingUtils.getFacesServletRegistration(facesContext, servletContext);
                if (facesServletRegistration == null
                        || facesServletRegistration.getMappings() == null
                        || facesServletRegistration.getMappings().length == 0)
                {
                    // check to see if the FacesServlet was found by MyFacesContainerInitializer
                    Boolean mappingAdded = (Boolean) servletContext.getAttribute(
                        MyFacesContainerInitializer.FACES_SERVLET_FOUND);

                    if (mappingAdded == null || !mappingAdded)
                    {
                        // check if the FacesServlet has been added dynamically
                        // in a Servlet 3.0 environment by MyFacesContainerInitializer
                        mappingAdded = (Boolean) servletContext.getAttribute(
                            MyFacesContainerInitializer.FACES_SERVLET_ADDED_ATTRIBUTE);

                        if (mappingAdded == null || !mappingAdded)
                        {
                            if (log.isLoggable(Level.WARNING))
                            {
                                log.warning("No mappings of FacesServlet found. Abort initializing MyFaces.");
                            }
                            return;
                        }
                    }
                }
            }
            
            initCDIIntegration(servletContext, externalContext);
            
            initContainerIntegration(servletContext, externalContext);

            // log environment integrations
            ExternalSpecifications.isBeanValidationAvailable();
            ExternalSpecifications.isCDIAvailable(externalContext);
            ExternalSpecifications.isEL3Available();
            ExternalSpecifications.isServlet4Available();

            String useEncryption = servletContext.getInitParameter(StateUtils.USE_ENCRYPTION);
            if ("false".equals(useEncryption))
            {
                log.warning(StateUtils.USE_ENCRYPTION + " is set to false. " 
                        + "This is unsecure and should only be used for local or intranet applications!");
            }
            else
            {
                StateUtils.initSecret(servletContext);
            }

            _dispatchApplicationEvent(servletContext, PostConstructApplicationEvent.class);
            
            initWebsocketIntegration(servletContext, externalContext);

            WebConfigParamsLogger.logWebContextParams(facesContext);
            
            //Start ViewPoolProcessor if necessary
            ViewPoolProcessor.initialize(facesContext);
            
            MyfacesConfig config = MyfacesConfig.getCurrentInstance(facesContext.getExternalContext());
            if (config.isAutomaticExtensionlessMapping())
            {
                initAutomaticExtensionlessMapping(facesContext, servletContext);
            }

            // publish resourceBundleControl to applicationMap, to make it available to the API
            ResourceBundle.Control resourceBundleControl = config.getResourceBundleControl();
            if (resourceBundleControl != null)
            {
                facesContext.getExternalContext().getApplicationMap().put(
                        MyfacesConfig.RESOURCE_BUNDLE_CONTROL, resourceBundleControl);
            }
 
            // print out a very prominent log message if the project stage is != Production
            if (!facesContext.isProjectStage(ProjectStage.Production)
                    && !facesContext.isProjectStage(ProjectStage.UnitTest))
            {
                ProjectStage projectStage = facesContext.getApplication().getProjectStage();
                StringBuilder message = new StringBuilder("\n\n");
                message.append("********************************************************************\n");
                message.append("*** WARNING: Apache MyFaces Core is running in ");
                message.append(projectStage.name().toUpperCase());        
                message.append(" mode.");
                int length = projectStage.name().length();
                for (int i = 0; i < 11 - length; i++)
                {
                    message.append(' ');
                }
                message.append(" ***\n");
                message.append("***                                            ");
                for (int i = 0; i < length; i++)
                {
                    message.append('^');
                }
                for (int i = 0; i < 18 - length; i++)
                {
                    message.append(' ');
                }
                message.append("***\n");
                message.append("*** Do NOT deploy to your live server(s) without changing this.  ***\n");
                message.append("*** See Application#getProjectStage() for more information.      ***\n");
                message.append("********************************************************************\n");
                message.append("\n");
                log.log(Level.WARNING, message.toString());
            }

            cleanupAfterStartup(facesContext);
            
            dispatchInitializationEvent(servletContext, FACES_INIT_PHASE_POSTINIT);
            
            destroyStartupFacesContext(facesContext);
            
            servletContext.setAttribute(INITIALIZED, Boolean.TRUE);
            
            log.log(Level.INFO, "MyFaces Core has started, it took ["
                    + (System.currentTimeMillis() - start)
                    + "] ms.");
        }
        catch (Exception ex)
        {
            log.log(Level.SEVERE, "An error occured while initializing MyFaces: "
                      + ex.getMessage(), ex);
        }
    }

    protected void cleanupAfterStartup(FacesContext facesContext)
    {
        ExternalContext externalContext = facesContext.getExternalContext();
        
        if (ExternalSpecifications.isCDIAvailable(externalContext))
        {
            BeanManager beanManager = CDIUtils.getBeanManager(externalContext);
            CdiAnnotationProviderExtension extension = CDIUtils.getOptional(beanManager,
                    CdiAnnotationProviderExtension.class);
            if (extension != null)
            {
                extension.release();
            }
        }
    }
    
    /**
     * Eventually we can use our plugin infrastructure for this as well
     * it would be a cleaner interception point than the base class
     * but for now this position is valid as well
     * <p/>
     * Note we add it for now here because the application factory object
     * leaves no possibility to have a destroy interceptor
     * and applications are per web application singletons
     * Note if this does not work out
     * move the event handler into the application factory
     *
     * @param servletContext The current {@link ServletContext}
     * @param eventClass     the class to be passed down into the dispatching
     *                       code
     */
    private void _dispatchApplicationEvent(ServletContext servletContext, Class<? extends SystemEvent> eventClass)
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        Application application = facesContext.getApplication();
        application.publishEvent(facesContext, eventClass, Application.class, application);
    }
    
    /**
     * Cleans up all remaining resources (well, theoretically).
     * 
     * @param servletContext The current {@link ServletContext}
     */
    @Override
    public void destroyFaces(ServletContext servletContext)
    {
        if (!Boolean.TRUE.equals(servletContext.getAttribute(INITIALIZED)))
        {
            return;
        }

        FacesContext facesContext = initShutdownFacesContext(servletContext);
        
        dispatchInitializationEvent(servletContext, FACES_INIT_PHASE_PREDESTROY);

        _dispatchApplicationEvent(servletContext, PreDestroyApplicationEvent.class);

        _callPreDestroyOnInjectedJSFArtifacts(facesContext);
        
        // clear the cache of MetaRulesetImpl in order to prevent a memory leak
        MetaRulesetImpl.clearMetadataTargetCache();

        if (facesContext.getExternalContext().getApplicationMap().containsKey("org.apache.myfaces.push"))
        {
            WebsocketFacesInit.clearWebsocketSessionLRUCache(facesContext.getExternalContext());
        }
        
        // clear UIViewParameter default renderer map
        try
        {
            Class<?> c = ClassUtils.classForName("jakarta.faces.component.UIViewParameter");
            Method m = c.getDeclaredMethod("releaseRenderer");
            m.setAccessible(true);
            m.invoke(null);
        }
        catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e)
        {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
        
        // TODO is it possible to make a real cleanup?

        // Destroy startup FacesContext, but note we do before publish postdestroy event on
        // plugins and before release factories.
        destroyShutdownFacesContext(facesContext);
        
        FactoryFinder.releaseFactories();

        dispatchInitializationEvent(servletContext, FACES_INIT_PHASE_POSTDESTROY);

        servletContext.removeAttribute(INITIALIZED);
    }

    /**
     * Configures this JSF application. It's required that every
     * FacesInitializer (i.e. every subclass) calls this method during
     * initialization.
     *
     * @param servletContext    the current ServletContext
     * @param externalContext   the current ExternalContext
     * @param expressionFactory the ExpressionFactory to use
     * @return the current runtime configuration
     */
    protected RuntimeConfig buildConfiguration(ServletContext servletContext,
                                               ExternalContext externalContext, ExpressionFactory expressionFactory)
    {
        RuntimeConfig runtimeConfig = RuntimeConfig.getCurrentInstance(externalContext);
        runtimeConfig.setExpressionFactory(expressionFactory);

        // And configure everything
        new FacesConfigurator(externalContext).configure();

        validateFacesConfig(servletContext, externalContext);

        return runtimeConfig;
    }

    protected void validateFacesConfig(ServletContext servletContext, ExternalContext externalContext)
    {
        String validate = servletContext.getInitParameter(MyfacesConfig.VALIDATE);
        if ("true".equals(validate) && log.isLoggable(Level.WARNING)) // the default value is false
        {
            List<String> warnings = FacesConfigValidator.validate(externalContext);

            for (String warning : warnings)
            {
                log.warning(warning);
            }
        }
    }

    /**
     * Try to load user-definied ExpressionFactory. Returns <code>null</code>,
     * if no custom ExpressionFactory was specified.
     *
     * @param externalContext the current ExternalContext
     * @return User-specified ExpressionFactory, or
     *         <code>null</code>, if no no custom implementation was specified
     */
    protected static ExpressionFactory getUserDefinedExpressionFactory(ExternalContext externalContext)
    {
        String expressionFactoryClassName
                = MyfacesConfig.getCurrentInstance(externalContext).getExpressionFactory();
        if (StringUtils.isNotBlank(expressionFactoryClassName))
        {
            if (log.isLoggable(Level.FINE))
            {
                log.fine("Attempting to load the ExpressionFactory implementation "
                        + "you've specified: '" + expressionFactoryClassName + "'.");
            }

            return loadExpressionFactory(expressionFactoryClassName);
        }

        return null;
    }

    /**
     * Loads and instantiates the given ExpressionFactory implementation.
     *
     * @param expressionFactoryClassName the class name of the ExpressionFactory implementation
     * @return the newly created ExpressionFactory implementation, or
     *         <code>null</code>, if an error occurred
     */
    protected static ExpressionFactory loadExpressionFactory(String expressionFactoryClassName)
    {
        return loadExpressionFactory(expressionFactoryClassName, true);
    }
    
    protected static ExpressionFactory loadExpressionFactory(String expressionFactoryClassName, boolean logMissing)
    {
        try
        {
            ClassLoader cl = ClassUtils.getContextClassLoader();
            if (cl == null)
            {
                cl = FacesInitializerImpl.class.getClassLoader();
            }

            Class<?> expressionFactoryClass = cl.loadClass(expressionFactoryClassName);
            return (ExpressionFactory) expressionFactoryClass.newInstance();
        }
        catch (Exception ex)
        {
            if (log.isLoggable(Level.FINE))
            {
                log.log(Level.FINE, "An error occured while instantiating a new ExpressionFactory. "
                        + "Attempted to load class '" + expressionFactoryClassName + "'.", ex);
            }
        }

        return null;
    }

    @Override
    public FacesContext initStartupFacesContext(ServletContext servletContext)
    {
        // We cannot use FacesContextFactory, because it is necessary to initialize 
        // before Application and RenderKit factories, so we should use different object. 
        return _createFacesContext(servletContext, true);
    }
        
    @Override
    public void destroyStartupFacesContext(FacesContext facesContext)
    {
        _releaseFacesContext(facesContext);
    }
    
    @Override
    public FacesContext initShutdownFacesContext(ServletContext servletContext)
    {
        return _createFacesContext(servletContext, false);
    }
    
    @Override    
    public void destroyShutdownFacesContext(FacesContext facesContext)
    {
        _releaseFacesContext(facesContext);
    }
    
    private FacesContext _createFacesContext(ServletContext servletContext, boolean startup)
    {
        ExternalContext externalContext = new StartupServletExternalContextImpl(servletContext, startup);
        ExceptionHandler exceptionHandler = new ExceptionHandlerImpl();
        FacesContext facesContext = new StartupFacesContextImpl(externalContext, 
                externalContext, exceptionHandler, startup);
        
        // If getViewRoot() is called during application startup or shutdown, 
        // it should return a new UIViewRoot with its locale set to Locale.getDefault().
        UIViewRoot startupViewRoot = new UIViewRoot();
        startupViewRoot.setLocale(Locale.getDefault());
        facesContext.setViewRoot(startupViewRoot);
        
        return facesContext;
    }
    
    private void _releaseFacesContext(FacesContext facesContext)
    {        
        // make sure that the facesContext gets released.
        // This is important in an OSGi environment 
        if (facesContext != null)
        {
            facesContext.release();
        }        
    }

    /**
     * The intention of this method is provide a point where CDI integration is done.
     * {@link jakarta.faces.flow.FlowScoped} and {@link jakarta.faces.view.ViewScoped} requires CDI in order to work,
     * so this method should set a BeanManager instance on application map under
     * the key "oam.cdi.BEAN_MANAGER_INSTANCE".
     * The default implementation look on ServletContext first and then use JNDI.
     * 
     * @param servletContext
     * @param externalContext 
     */
    protected void initCDIIntegration(
            ServletContext servletContext, ExternalContext externalContext)
    {
        // Lookup bean manager and put it into an application scope attribute to 
        // access it later. Remember the trick here is do not call any CDI api 
        // directly, so if no CDI api is on the classpath no exception will be thrown.
        
        // Try with servlet context
        Object beanManager = servletContext.getAttribute(CDI_SERVLET_CONTEXT_BEAN_MANAGER_ATTRIBUTE);
        if (beanManager == null)
        {
            beanManager = lookupBeanManagerFromCDI();
        }
        if (beanManager == null)
        {
            beanManager = lookupBeanManagerFromJndi();
        }
        if (beanManager != null)
        {
            externalContext.getApplicationMap().put(CDI_BEAN_MANAGER_INSTANCE, beanManager);
        }
    }

    /**
     * This method tries to use the CDI-1.1 CDI.current() method to lookup the CDI BeanManager.
     * We do all this via reflection to not blow up if CDI-1.1 is not on the classpath.
     * @return the BeanManager or {@code null} if either not in a CDI-1.1 environment
     *         or the BeanManager doesn't exist yet.
     */
    private Object lookupBeanManagerFromCDI()
    {
        try
        {
            Class cdiClass = ClassUtils.simpleClassForName("jakarta.enterprise.inject.spi.CDI", false);
            if (cdiClass != null)
            {
                Method currentMethod = cdiClass.getMethod("current");
                Object cdi = currentMethod.invoke(null);

                Method getBeanManagerMethod = cdiClass.getMethod("getBeanManager");
                Object beanManager = getBeanManagerMethod.invoke(cdi);
                return beanManager;
            }
        }
        catch (Exception e)
        {
            // ignore
        }
        return null;
    }

    /**
     * Try to lookup the CDI BeanManager from JNDI.
     * We do all this via reflection to not blow up if CDI is not available.
     */
    private Object lookupBeanManagerFromJndi()
    {
        Object beanManager = null;
        // Use reflection to avoid restricted API in GAE
        Class icclazz = null;
        Method lookupMethod = null;
        try
        {
            icclazz = ClassUtils.simpleClassForName("javax.naming.InitialContext");
            if (icclazz != null)
            {
                lookupMethod = icclazz.getMethod("doLookup", String.class);
            }
        }
        catch (Throwable t)
        {
            // noop
        }
        if (lookupMethod != null)
        {
            // Try with JNDI
            try
            {
                // in an application server
                //beanManager = InitialContext.doLookup("java:comp/BeanManager");
                beanManager = lookupMethod.invoke(icclazz, "java:comp/BeanManager");
            }
            catch (Exception e)
            {
                // silently ignore
            }
            catch (NoClassDefFoundError e)
            {
                //On Google App Engine, javax.naming.Context is a restricted class.
                //In that case, NoClassDefFoundError is thrown. stageName needs to be configured
                //below by context parameter.
            }

            if (beanManager == null)
            {
                try
                {
                    // in a servlet container
                    //beanManager = InitialContext.doLookup("java:comp/env/BeanManager");
                    beanManager = lookupMethod.invoke(icclazz, "java:comp/env/BeanManager");
                }
                catch (Exception e)
                {
                    // silently ignore
                }
                catch (NoClassDefFoundError e)
                {
                    //On Google App Engine, javax.naming.Context is a restricted class.
                    //In that case, NoClassDefFoundError is thrown. stageName needs to be configured
                    //below by context parameter.
                }
            }
        }

        return beanManager;
    }

    public void _callPreDestroyOnInjectedJSFArtifacts(FacesContext facesContext)
    {
        InjectionProvider injectionProvider = InjectionProviderFactory.getInjectionProviderFactory(
            facesContext.getExternalContext()).getInjectionProvider(facesContext.getExternalContext());
        List<BeanEntry> injectedBeanStorage =
                (List<BeanEntry>)facesContext.getExternalContext().getApplicationMap().get(INJECTED_BEAN_STORAGE_KEY);

        if (injectedBeanStorage != null)
        {
            for (BeanEntry entry : injectedBeanStorage)
            {
                try
                {
                    injectionProvider.preDestroy(entry.getInstance(), entry.getCreationMetaData());
                }
                catch (InjectionProviderException ex)
                {
                    log.log(Level.INFO, "Exception on PreDestroy", ex);
                }
            }
            injectedBeanStorage.clear();
        }
    }
    
    protected void initWebsocketIntegration(ServletContext servletContext, ExternalContext externalContext)
    {
        Boolean b = WebConfigParamUtils.getBooleanInitParameter(externalContext, 
                PushContext.ENABLE_WEBSOCKET_ENDPOINT_PARAM_NAME);
        
        if (Boolean.TRUE.equals(b))
        {
            // get the instance
            // see https://docs.oracle.com/javaee/7/api/javax/websocket/server/ServerContainer.html)
            final ServerContainer serverContainer = (ServerContainer) 
                    servletContext.getAttribute(ServerContainer.class.getName());
            if (serverContainer == null)
            {
                log.log(Level.INFO, "f:websocket support enabled but cannot found websocket ServerContainer instance "
                        + "on current context.");
                return;
            }

            try 
            {
                serverContainer.addEndpoint(ServerEndpointConfig.Builder
                        .create(EndpointImpl.class, EndpointImpl.JAKARTA_FACES_PUSH_PATH)
                        .configurator(new WebsocketConfigurator(externalContext)).build());

                //Init LRU cache
                WebsocketFacesInit.initWebsocketSessionLRUCache(externalContext);

                externalContext.getApplicationMap().put("org.apache.myfaces.push", "true");
            }
            catch (DeploymentException e)
            {
                log.log(Level.INFO, "Exception on initialize Websocket Endpoint: ", e);
            }
        }
    }
    
    /**
     * 
     * @since 2.3
     * @param facesContext 
     * @param servletContext
     */
    protected void initAutomaticExtensionlessMapping(FacesContext facesContext, ServletContext servletContext)
    {
        FacesServletMappingUtils.ServletRegistrationInfo facesServletRegistration =
                FacesServletMappingUtils.getFacesServletRegistration(facesContext, servletContext);
        if (facesServletRegistration != null)
        {
            facesContext.getApplication().getViewHandler().getViews(facesContext, "/", 
                    ViewVisitOption.RETURN_AS_MINIMAL_IMPLICIT_OUTCOME).forEach(s -> {
                        facesServletRegistration.getRegistration().addMapping(s);
                    });
        }
    }

    protected void initContainerIntegration(ServletContext servletContext, ExternalContext externalContext)
    {
        ExpressionFactory expressionFactory = getUserDefinedExpressionFactory(externalContext);
        if (expressionFactory == null)
        {
            String[] candidates = new String[] { "org.apache.el.ExpressionFactoryImpl",
                "com.sun.el.ExpressionFactoryImpl", "de.odysseus.el.ExpressionFactoryImpl",
                "org.jboss.el.ExpressionFactoryImpl", "com.caucho.el.ExpressionFactoryImpl" };
            
            for (String candidate : candidates)
            {
                expressionFactory = loadExpressionFactory(candidate, false);
                if (expressionFactory != null)
                {
                    if (log.isLoggable(Level.FINE))
                    {
                        log.fine(ExpressionFactory.class.getName() + " implementation found: " + candidate);
                    }
                    break;
                }
            }
        }

        if (expressionFactory == null)
        {
            throw new FacesException("No " + ExpressionFactory.class.getName() + " implementation found. "
                    + "Please provide <context-param> in web.xml: org.apache.myfaces.EXPRESSION_FACTORY");
        }
        
        buildConfiguration(servletContext, externalContext, expressionFactory);
    }
    

    /**
     * the central initialisation event dispatcher which calls
     * our listeners
     *
     * @param context
     * @param operation
     */
    private void dispatchInitializationEvent(ServletContext context, int operation)
    {
        if (operation == FACES_INIT_PHASE_PREINIT)
        {
            if (!loadFacesInitPluginsViaServiceLoader(context))
            {
                loadFacesInitViaContextParam(context);
            }
        }

        List<StartupListener> pluginEntries = (List<StartupListener>)
                context.getAttribute(MyfacesConfig.FACES_INIT_PLUGINS);
        if (pluginEntries == null)
        {
            return;
        }

        //now we process the plugins
        for (StartupListener initializer : pluginEntries)
        {
            log.info("Processing plugin");

            //for now the initializers have to be stateless to
            //so that we do not have to enforce that the initializer
            //must be serializable
            switch (operation)
            {
                case FACES_INIT_PHASE_PREINIT:
                    initializer.preInit(context);
                    break;
                case FACES_INIT_PHASE_POSTINIT:
                    initializer.postInit(context);
                    break;
                case FACES_INIT_PHASE_PREDESTROY:
                    initializer.preDestroy(context);
                    break;
                case FACES_INIT_PHASE_POSTDESTROY:
                    initializer.postDestroy(context);
                    break;
                default:
                    break;
            }
        }
        log.info("Processing MyFaces plugins done");
    }
    
   /**
     * Loads the faces init plugins per Service loader.
     * 
     * @return false if there are not plugins defined via ServiceLoader.
     */
    private boolean loadFacesInitPluginsViaServiceLoader(ServletContext servletContext)
    {   
        ServiceLoader<StartupListener> loader = ServiceLoader.load(StartupListener.class,
                ClassUtils.getContextClassLoader());

        Iterator<StartupListener> it = (Iterator<StartupListener>) loader.iterator();
        if (!it.hasNext())
        {
            return false;
        }
        
        List<StartupListener> listeners = new LinkedList<>();
        while (it.hasNext())
        {
            listeners.add(it.next());
        }

        servletContext.setAttribute(MyfacesConfig.FACES_INIT_PLUGINS, listeners);
        
        return true;
    }

    /**
     * loads the faces init plugins per reflection from the context param.
     */
    private void loadFacesInitViaContextParam(ServletContext servletContext)
    {
        String plugins = (String) servletContext.getInitParameter(MyfacesConfig.FACES_INIT_PLUGINS);
        if (plugins == null)
        {
            return;
        }
        log.info("MyFaces Plugins found");
        
        String[] pluginEntries = plugins.split(",");
        List<StartupListener> listeners = new ArrayList<>(pluginEntries.length);
        for (String pluginEntry : pluginEntries)
        {
            try
            {
                Class pluginClass = ClassUtils.getContextClassLoader().loadClass(pluginEntry);
                if (pluginClass == null)
                {
                    pluginClass = this.getClass().getClassLoader().loadClass(pluginEntry);
                }

                listeners.add((StartupListener) pluginClass.newInstance());
            }
            catch (ClassNotFoundException | InstantiationException | IllegalAccessException e)
            {
                log.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        servletContext.setAttribute(MyfacesConfig.FACES_INIT_PLUGINS, listeners);
    }
}
