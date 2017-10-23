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

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFWebConfigParam;
import org.apache.myfaces.config.FacesConfigValidator;
import org.apache.myfaces.config.FacesConfigurator;
import org.apache.myfaces.config.ManagedBeanBuilder;
import org.apache.myfaces.config.RuntimeConfig;
import org.apache.myfaces.config.element.ManagedBean;
import org.apache.myfaces.context.ReleaseableExternalContext;
import org.apache.myfaces.context.servlet.StartupFacesContextImpl;
import org.apache.myfaces.context.servlet.StartupServletExternalContextImpl;
import org.apache.myfaces.shared.application.FacesServletMappingUtils;
import org.apache.myfaces.shared.context.ExceptionHandlerImpl;
import org.apache.myfaces.shared.util.StateUtils;
import org.apache.myfaces.shared.util.WebConfigParamUtils;
import org.apache.myfaces.cdi.util.BeanEntry;
import org.apache.myfaces.spi.InjectionProvider;
import org.apache.myfaces.spi.InjectionProviderException;
import org.apache.myfaces.spi.InjectionProviderFactory;
import org.apache.myfaces.spi.ViewScopeProvider;
import org.apache.myfaces.spi.ViewScopeProviderFactory;
import org.apache.myfaces.spi.WebConfigProvider;
import org.apache.myfaces.spi.WebConfigProviderFactory;
import org.apache.myfaces.util.ExternalSpecifications;
import org.apache.myfaces.view.facelets.tag.MetaRulesetImpl;

import javax.el.ExpressionFactory;
import javax.faces.application.Application;
import javax.faces.application.ProjectStage;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.PostConstructApplicationEvent;
import javax.faces.event.PreDestroyApplicationEvent;
import javax.faces.event.SystemEvent;
import javax.servlet.ServletContext;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.application.ViewVisitOption;
import javax.faces.push.PushContext;
import javax.servlet.ServletRegistration;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;
import org.apache.myfaces.push.EndpointImpl;
import org.apache.myfaces.push.WebsocketConfigurator;
import org.apache.myfaces.push.WebsocketFacesInit;
import org.apache.myfaces.shared.util.ClassUtils;
import org.apache.myfaces.spi.ServiceProviderFinder;
import org.apache.myfaces.spi.ServiceProviderFinderFactory;
import org.apache.myfaces.view.facelets.ViewPoolProcessor;

/**
 * Performs common initialization tasks.
 */
public abstract class AbstractFacesInitializer implements FacesInitializer
{
    /**
     * The logger instance for this class.
     */
    //private static final Log log = LogFactory.getLog(AbstractFacesInitializer.class);
    private static final Logger log = Logger.getLogger(AbstractFacesInitializer.class.getName());
    
    /**
     * If the servlet mapping for the FacesServlet is added dynamically, Boolean.TRUE 
     * is stored under this key in the ServletContext.
     * ATTENTION: this constant is duplicate in MyFacesContainerInitializer.
     */
    private static final String FACES_SERVLET_ADDED_ATTRIBUTE = "org.apache.myfaces.DYNAMICALLY_ADDED_FACES_SERVLET";

    /**
     * This parameter specifies the ExpressionFactory implementation to use.
     */
    @JSFWebConfigParam(since="1.2.7", group="EL")
    protected static final String EXPRESSION_FACTORY = "org.apache.myfaces.EXPRESSION_FACTORY";
    
    /**
     * If this param is set to true, the check for faces servlet mapping is not done 
     */
    @JSFWebConfigParam(since="2.0.3", defaultValue="false")
    protected static final String INITIALIZE_ALWAYS_STANDALONE = "org.apache.myfaces.INITIALIZE_ALWAYS_STANDALONE";
    
    /**
     * Indicate if log all web config params should be done before initialize the webapp. 
     * <p>
     * If is set in "auto" mode, web config params are only logged on "Development" and "Production" project stages.
     * </p> 
     */
    @JSFWebConfigParam(expectedValues="true, auto, false", defaultValue="auto")
    public static final String INIT_PARAM_LOG_WEB_CONTEXT_PARAMS = "org.apache.myfaces.LOG_WEB_CONTEXT_PARAMS";
    public static final String INIT_PARAM_LOG_WEB_CONTEXT_PARAMS_DEFAULT ="auto";
    
    /**
     * This parameter enables automatic extensionless mapping for all JSF views.
     */
    @JSFWebConfigParam(since="2.3", expectedValues = "true, false", defaultValue = "false")
    public static final String INIT_PARAM_AUTOMATIC_EXTENSIONLESS_MAPPING = 
            "org.apache.myfaces.AUTOMATIC_EXTENSIONLESS_MAPPING";
    public static final boolean INIT_PARAM_AUTOMATIC_EXTENSIONLESS_MAPPING_DEFAULT = false;
    
    public static final String CDI_BEAN_MANAGER_INSTANCE = "oam.cdi.BEAN_MANAGER_INSTANCE";
    
    private static final String CDI_SERVLET_CONTEXT_BEAN_MANAGER_ATTRIBUTE = 
        "javax.enterprise.inject.spi.BeanManager";

    private static final String INJECTED_BEAN_STORAGE_KEY = "org.apache.myfaces.spi.BEAN_ENTRY_STORAGE";

    /**
     * Performs all necessary initialization tasks like configuring this JSF
     * application.
     */
    public void initFaces(ServletContext servletContext)
    {
        try
        {
            if (log.isLoggable(Level.FINEST))
            {
                log.finest("Initializing MyFaces");
            }

            // Some parts of the following configuration tasks have been implemented 
            // by using an ExternalContext. However, that's no problem as long as no 
            // one tries to call methods depending on either the ServletRequest or 
            // the ServletResponse.
            // JSF 2.0: FacesInitializer now has some new methods to
            // use proper startup FacesContext and ExternalContext instances.
            FacesContext facesContext = FacesContext.getCurrentInstance();
            ExternalContext externalContext = facesContext.getExternalContext();

            // Setup ServiceProviderFinder
            ServiceProviderFinder spf = ServiceProviderFinderFactory.getServiceProviderFinder(
                externalContext);
            Map<String, List<String>> spfConfig = spf.calculateKnownServiceProviderMapInfo(
                externalContext, ServiceProviderFinder.KNOWN_SERVICES);
            if (spfConfig != null)
            {
                spf.initKnownServiceProviderMapInfo(externalContext, spfConfig);
            }
            
            // Parse and validate the web.xml configuration file
            
            if (!WebConfigParamUtils.getBooleanInitParameter(externalContext, INITIALIZE_ALWAYS_STANDALONE, false))
            {
                WebConfigProvider webConfigProvider = WebConfigProviderFactory.getWebConfigProviderFactory(
                        facesContext.getExternalContext()).getWebConfigProvider(facesContext.getExternalContext());

                if (webConfigProvider.getFacesServletMappings(facesContext.getExternalContext()).isEmpty())
                {
                    // check if the FacesServlet has been added dynamically
                    // in a Servlet 3.0 environment by MyFacesContainerInitializer
                    Boolean mappingAdded = (Boolean) servletContext.getAttribute(FACES_SERVLET_ADDED_ATTRIBUTE);
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

            initCDIIntegration(servletContext, externalContext);
            
            initContainerIntegration(servletContext, externalContext);
            
            ViewScopeProviderFactory factory = ViewScopeProviderFactory.getViewScopeHandlerFactory(
                externalContext);
            
            ViewScopeProvider viewScopeHandler = factory.getViewScopeHandler(
                externalContext);
            
            ManagedBeanDestroyerListener listener = (ManagedBeanDestroyerListener)
                externalContext.getApplicationMap().get(
                    ManagedBeanDestroyerListener.APPLICATION_MAP_KEY);
            if (listener != null)
            {
                listener.setViewScopeHandler(viewScopeHandler);
            }

            String useEncryption = servletContext.getInitParameter(StateUtils.USE_ENCRYPTION);
            if (!"false".equals(useEncryption)) // the default value is true
            {
                StateUtils.initSecret(servletContext);
            }

            // initialize eager managed beans
            _createEagerBeans(facesContext);

            _dispatchApplicationEvent(servletContext, PostConstructApplicationEvent.class);
            
            initWebsocketIntegration(servletContext, externalContext);

            if ( (facesContext.isProjectStage(ProjectStage.Development) || 
                  facesContext.isProjectStage(ProjectStage.Production)) &&
                 log.isLoggable(Level.INFO))
            {
                log.info("ServletContext initialized.");
            }

            WebConfigParamsLogger.logWebContextParams(facesContext);
            
            checkForDeprecatedContextParams(facesContext);
            
            //Force output EL message
            ExternalSpecifications.isBeanValidationAvailable();
            
            //Start ViewPoolProcessor if necessary
            ViewPoolProcessor.initialize(facesContext);
            
            Boolean automaticExtensionlessMapping = WebConfigParamUtils.getBooleanInitParameter(
                    externalContext, INIT_PARAM_AUTOMATIC_EXTENSIONLESS_MAPPING, 
                    INIT_PARAM_AUTOMATIC_EXTENSIONLESS_MAPPING_DEFAULT);
            if (Boolean.TRUE.equals(automaticExtensionlessMapping))
            {
                initAutomaticExtensionlessMapping(facesContext, servletContext);
            }

            // print out a very prominent log message if the project stage is != Production
            if (!facesContext.isProjectStage(ProjectStage.Production) &&
                !facesContext.isProjectStage(ProjectStage.UnitTest))
            {
                ProjectStage projectStage = facesContext.getApplication().getProjectStage();
                StringBuilder message = new StringBuilder("\n\n");
                message.append("*******************************************************************\n");
                message.append("*** WARNING: Apache MyFaces-2 is running in ");
                message.append(projectStage.name().toUpperCase());        
                message.append(" mode.");
                int length = projectStage.name().length();
                for (int i = 0; i < 11 - length; i++)
                {
                    message.append(" ");
                }
                message.append("   ***\n");
                message.append("***                                         ");
                for (int i = 0; i < length; i++)
                {
                    message.append("^");
                }
                for (int i = 0; i < 20 - length; i++)
                {
                    message.append(" ");
                }
                message.append("***\n");
                message.append("*** Do NOT deploy to your live server(s) without changing this. ***\n");
                message.append("*** See Application#getProjectStage() for more information.     ***\n");
                message.append("*******************************************************************\n");
                log.log(Level.WARNING, message.toString());
            }

        }
        catch (Exception ex)
        {
            log.log(Level.SEVERE, "An error occured while initializing MyFaces: "
                      + ex.getMessage(), ex);
        }
    }
    
    /**
     * Checks for application scoped managed-beans with eager=true,
     * creates them and stores them in the application map.
     * @param facesContext
     */
    private void _createEagerBeans(FacesContext facesContext)
    {
        ExternalContext externalContext = facesContext.getExternalContext();
        RuntimeConfig runtimeConfig = RuntimeConfig.getCurrentInstance(externalContext);
        List<ManagedBean> eagerBeans = new ArrayList<ManagedBean>();
        
        // check all registered managed-beans
        for (ManagedBean bean : runtimeConfig.getManagedBeans().values())
        {
            String eager = bean.getEager();
            if (eager != null && "true".equals(eager))
            {
                // eager beans are only allowed for application scope
                if (ManagedBeanBuilder.APPLICATION.equals(bean.getManagedBeanScope()))
                {
                    // add to eager beans
                    eagerBeans.add(bean);
                }
                else
                {
                    // log warning and continue (the bean will be lazy loaded)
                    log.log(Level.WARNING, "The managed-bean with name "
                            + bean.getManagedBeanName()
                            + " must be application scoped to support eager=true.");
                }
            }
        }
        
        // check if there are any eager beans
        if (!eagerBeans.isEmpty())
        {
            ManagedBeanBuilder managedBeanBuilder = new ManagedBeanBuilder();
            Map<String, Object> applicationMap = externalContext.getApplicationMap();
            
            for (ManagedBean bean : eagerBeans)
            {
                // check application scope for bean instance
                if (applicationMap.containsKey(bean.getManagedBeanName()))
                {
                    // do not build bean, because it already exists
                    // (e.g. @ManagedProperty from previous managed bean already created it)
                    continue;
                }

                // create instance
                Object beanInstance = managedBeanBuilder.buildManagedBean(facesContext, bean);
                
                // put in application scope
                applicationMap.put(bean.getManagedBeanName(), beanInstance);
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
     * @param servletContext the servlet context to be passed down
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
     */
    public void destroyFaces(ServletContext servletContext)
    {

        FacesContext facesContext = FacesContext.getCurrentInstance();

        if (!WebConfigParamUtils.getBooleanInitParameter(facesContext.getExternalContext(),
                                                         INITIALIZE_ALWAYS_STANDALONE, false))
        {
            //We need to check if the current application was initialized by myfaces
            WebConfigProvider webConfigProvider = WebConfigProviderFactory.getWebConfigProviderFactory(
                    facesContext.getExternalContext()).getWebConfigProvider(facesContext.getExternalContext());

            if (webConfigProvider.getFacesServletMappings(facesContext.getExternalContext()).isEmpty())
            {
                // check if the FacesServlet has been added dynamically
                // in a Servlet 3.0 environment by MyFacesContainerInitializer
                Boolean mappingAdded = (Boolean) servletContext.getAttribute(FACES_SERVLET_ADDED_ATTRIBUTE);
                if (mappingAdded == null || !mappingAdded)
                {
                    if (log.isLoggable(Level.WARNING))
                    {
                        log.warning("No mappings of FacesServlet found. Abort destroy MyFaces.");
                    }
                    return;
                }
            }
        }

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
            Class<?> c = Class.forName("javax.faces.component.UIViewParameter");
            Method m = c.getDeclaredMethod("releaseRenderer");
            m.setAccessible(true);
            m.invoke(null);
        }
        catch(ClassNotFoundException e)
        {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
        catch(NoSuchMethodException e)
        {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
        catch(IllegalAccessException e)
        {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
        catch(InvocationTargetException e)
        {
            log.log(Level.SEVERE, e.getMessage(), e);
        }

        // TODO is it possible to make a real cleanup?
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
        String validate = servletContext.getInitParameter(FacesConfigValidator.VALIDATE_CONTEXT_PARAM);
        if ("true".equals(validate) && log.isLoggable(Level.WARNING))
        { // the default value is false
            List<String> warnings = FacesConfigValidator.validate(
                    externalContext);

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
                = WebConfigParamUtils.getStringInitParameter(externalContext, EXPRESSION_FACTORY);
        if (expressionFactoryClassName != null
                && expressionFactoryClassName.trim().length() > 0)
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
        try
        {
            ClassLoader cl = ClassUtils.getContextClassLoader();
            if (cl == null)
            {
                cl = AbstractFacesInitializer.class.getClassLoader();
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

    public FacesContext initStartupFacesContext(ServletContext servletContext)
    {
        // We cannot use FacesContextFactory, because it is necessary to initialize 
        // before Application and RenderKit factories, so we should use different object. 
        return _createFacesContext(servletContext, true);
    }
        
    public void destroyStartupFacesContext(FacesContext facesContext)
    {
        _releaseFacesContext(facesContext);
    }
    
    public FacesContext initShutdownFacesContext(ServletContext servletContext)
    {
        return _createFacesContext(servletContext, false);
    }
        
    public void destroyShutdownFacesContext(FacesContext facesContext)
    {
        _releaseFacesContext(facesContext);
    }
    
    private FacesContext _createFacesContext(ServletContext servletContext, boolean startup)
    {
        ExternalContext externalContext = new StartupServletExternalContextImpl(servletContext, startup);
        ExceptionHandler exceptionHandler = new ExceptionHandlerImpl();
        FacesContext facesContext = new StartupFacesContextImpl(externalContext, 
                (ReleaseableExternalContext) externalContext, exceptionHandler, startup);
        
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
     * Performs initialization tasks depending on the current environment.
     *
     * @param servletContext  the current ServletContext
     * @param externalContext the current ExternalContext
     */
    protected abstract void initContainerIntegration(
            ServletContext servletContext, ExternalContext externalContext);

    /**
     * The intention of this method is provide a point where CDI integration is done.
     * Faces Flow and javax.faces.view.ViewScope requires CDI in order to work, so
     * this method should set a BeanManager instance on application map under
     * the key "oam.cdi.BEAN_MANAGER_INSTANCE". The default implementation look on
     * ServletContext first and then use JNDI.
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
        Object beanManager = servletContext.getAttribute(
            CDI_SERVLET_CONTEXT_BEAN_MANAGER_ATTRIBUTE);
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
            externalContext.getApplicationMap().put(CDI_BEAN_MANAGER_INSTANCE,
                beanManager);
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
            Class cdiClass = null;
            Method cdiCurrentMethod = null;
            Method cdiGetBeanManagerMethod = null;
            cdiClass = simpleClassForNameNoException("javax.enterprise.inject.spi.CDI");
            if (cdiClass != null)
            {
                cdiCurrentMethod = cdiClass.getMethod("current");

                Object cdiInstance = cdiCurrentMethod.invoke(null);

                cdiGetBeanManagerMethod = cdiClass.getMethod("getBeanManager");
                return cdiGetBeanManagerMethod.invoke(cdiInstance);
            }
        }
        catch (Exception e)
        {
            // ignore
        }
        return null;
    }
    
    private static Class simpleClassForNameNoException(String type)
    {
        try
        {
            return ClassUtils.classForName(type);
        }
        catch (ClassNotFoundException e)
        {
            //log.log(Level.SEVERE, "Class " + type + " not found", e);
            //Ignore
            return null;
        }
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
            //
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
    
    protected void initWebsocketIntegration(
            ServletContext servletContext, ExternalContext externalContext)
    {
        Boolean b = WebConfigParamUtils.getBooleanInitParameter(externalContext, 
                PushContext.ENABLE_WEBSOCKET_ENDPOINT_PARAM_NAME);
        
        if (Boolean.TRUE.equals(b))
        {
            // According to https://tyrus.java.net/documentation/1.13/index/deployment.html section 3.2
            // we can create a websocket programmatically, getting ServerContainer instance from this location
            final ServerContainer serverContainer = (ServerContainer) 
                    servletContext.getAttribute("javax.websocket.server.ServerContainer");

            if (serverContainer != null)
            {
                try 
                {
                    serverContainer.addEndpoint(ServerEndpointConfig.Builder
                            .create(EndpointImpl.class, EndpointImpl.JAVAX_FACES_PUSH_PATH)
                            .configurator(new WebsocketConfigurator(externalContext)).build());
                    
                    //Init LRU cache
                    WebsocketFacesInit.initWebsocketSessionLRUCache(externalContext);
                    
                    externalContext.getApplicationMap().put("org.apache.myfaces.push", "true");
                }
                catch (DeploymentException e)
                {
                    log.log(Level.INFO, "Exception on Initialize Websocket Endpoint: ", e);
                }
            }
            else
            {
                log.log(Level.INFO, "f:websocket support enabled but cannot found websocket ServerContainer instance "+
                        "on current context. If websocket library is available, please include a FakeEndpoint instance "
                        + "into your code to force enable it (Tyrus users).");
            }
        }
    }
    
    /**
     * 
     * @since 2.3
     * @param facesContext 
     */
    protected void initAutomaticExtensionlessMapping(FacesContext facesContext, ServletContext servletContext)
    {
        final ServletRegistration facesServletRegistration = getFacesServletRegistration(facesContext, servletContext); 
        if (facesServletRegistration != null)
        {
            facesContext.getApplication().getViewHandler().getViews(facesContext, "/", 
                    ViewVisitOption.RETURN_AS_MINIMAL_IMPLICIT_OUTCOME).forEach(s -> {
                        facesServletRegistration.addMapping(s);
                    });
        }
    }
    
    private ServletRegistration getFacesServletRegistration(FacesContext facesContext, 
            ServletContext servletContext)
    {
        ServletRegistration facesServletRegistration = null;
        Map<String, ? extends ServletRegistration> map = servletContext.getServletRegistrations();
        if (map != null)
        {
            for (Map.Entry<String, ? extends ServletRegistration> entry : map.entrySet())
            {
                if (FacesServletMappingUtils.isFacesServlet(facesContext, entry.getValue().getClassName()))
                {
                    facesServletRegistration = entry.getValue();
                    break;
                }
            }
        }
        return facesServletRegistration;
    }
    
    protected void checkForDeprecatedContextParams(FacesContext facesContext)
    {
        ExternalContext externalContext = facesContext.getExternalContext();
        
        String value;
        
        value = externalContext.getInitParameter("org.apache.myfaces.CDI_MANAGED_CONVERTERS_ENABLED");
        if (value != null && !value.isEmpty())
        {
            log.severe("'org.apache.myfaces.CDI_MANAGED_CONVERTERS_ENABLED' is not supported anymore since 2.3. "
                    + "Please use @FacesConverter with managed=true.");
        }
        
        value = externalContext.getInitParameter("org.apache.myfaces.CDI_MANAGED_VALIDATORS_ENABLED");
        if (value != null && !value.isEmpty())
        {
            log.severe("'org.apache.myfaces.CDI_MANAGED_VALIDATORS_ENABLED' is not supported anymore since 2.3. "
                    + "Please use @FacesValidator with managed=true.");
        }
        
        value = externalContext.getInitParameter("org.apache.myfaces.SAVE_STATE_WITH_VISIT_TREE_ON_PSS");
        if (value != null && !value.isEmpty())
        {
            log.severe("'org.apache.myfaces.SAVE_STATE_WITH_VISIT_TREE_ON_PSS' is not supported anymore since 2.3.");
        }
        
        value = externalContext.getInitParameter("org.apache.myfaces.CACHE_OLD_VIEWS_IN_SESSION_MODE");
        if (value != null && !value.isEmpty())
        {
            log.severe("'org.apache.myfaces.CACHE_OLD_VIEWS_IN_SESSION_MODE' is not supported anymore since 2.3.");
        }
        
        value = externalContext.getInitParameter("org.apache.myfaces.HANDLE_STATE_CACHING_MECHANICS");
        if (value != null && !value.isEmpty())
        {
            log.severe("'org.apache.myfaces.HANDLE_STATE_CACHING_MECHANICS' is not supported anymore since 2.3.");
        }
        
        value = externalContext.getInitParameter("org.apache.myfaces.ERROR_HANDLER");
        if (value != null && !value.isEmpty())
        {
            log.severe("'org.apache.myfaces.ERROR_HANDLER' is not supported anymore since 2.3.");
        }
    }
}
