/*******************************************************************************
 * Copyright (c) 1997, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.webapp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.security.RunAs;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.MultipartConfigElement;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestAttributeEvent;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.ServletResponse;
import javax.servlet.ServletSecurityElement;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.annotation.HandlesTypes;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.TruncatableThrowable;
import com.ibm.websphere.servlet.error.ServletErrorReport;
import com.ibm.websphere.servlet.event.ApplicationEvent;
import com.ibm.websphere.servlet.event.ApplicationListener;
import com.ibm.websphere.servlet.event.FilterErrorListener;
import com.ibm.websphere.servlet.event.FilterInvocationListener;
import com.ibm.websphere.servlet.event.FilterListener;
import com.ibm.websphere.servlet.event.ServletContextEventSource;
import com.ibm.websphere.servlet.event.ServletErrorEvent;
import com.ibm.websphere.servlet.event.ServletErrorListener;
import com.ibm.websphere.servlet.event.ServletInvocationListener;
import com.ibm.websphere.servlet.event.ServletListener;
import com.ibm.websphere.webcontainer.async.AsyncRequestDispatcher;
import com.ibm.ws.container.Container;
import com.ibm.ws.container.DeployedModule;
import com.ibm.ws.container.ErrorPage;
import com.ibm.ws.container.MimeFilter;
import com.ibm.ws.container.service.annotations.WebAnnotations;
import com.ibm.ws.container.service.annocache.AnnotationsBetaHelper;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.managedobject.ManagedObject;
import com.ibm.ws.session.SessionCookieConfigImpl;
import com.ibm.ws.session.utils.IDGeneratorImpl;
import com.ibm.ws.session.utils.LoggingUtil;
import com.ibm.ws.util.WSThreadLocal;
import com.ibm.ws.webcontainer.WebContainer;
import com.ibm.ws.webcontainer.async.AsyncListenerEnum;
import com.ibm.ws.webcontainer.async.ListenerHelper;
import com.ibm.ws.webcontainer.async.ListenerHelper.CheckDispatching;
import com.ibm.ws.webcontainer.async.ListenerHelper.ExecuteNextRunnable;
import com.ibm.ws.webcontainer.core.BaseContainer;
import com.ibm.ws.webcontainer.core.RequestMapper;
import com.ibm.ws.webcontainer.core.Response;
import com.ibm.ws.webcontainer.exception.WebAppNotLoadedException;
import com.ibm.ws.webcontainer.exception.WebContainerException;
import com.ibm.ws.webcontainer.extension.DefaultExtensionProcessor;
import com.ibm.ws.webcontainer.extension.InvokerExtensionProcessor;
import com.ibm.ws.webcontainer.extension.WebExtensionProcessor;
import com.ibm.ws.webcontainer.filter.FilterConfig;
import com.ibm.ws.webcontainer.filter.FilterMapping;
import com.ibm.ws.webcontainer.filter.WebAppFilterManager;
import com.ibm.ws.webcontainer.internal.util.MajorHandlingRuntimeException;
import com.ibm.ws.webcontainer.metadata.JspConfigDescriptorImpl;
import com.ibm.ws.webcontainer.osgi.collaborator.CollaboratorHelperImpl;
import com.ibm.ws.webcontainer.osgi.collaborator.CollaboratorServiceImpl;
import com.ibm.ws.webcontainer.servlet.DefaultErrorReporter;
import com.ibm.ws.webcontainer.servlet.DirectoryBrowsingServlet;
import com.ibm.ws.webcontainer.servlet.IServletContextExtended;
import com.ibm.ws.webcontainer.servlet.ServletConfig;
import com.ibm.ws.webcontainer.servlet.ServletWrapper;
import com.ibm.ws.webcontainer.servlet.exception.NoTargetForURIException;
import com.ibm.ws.webcontainer.session.IHttpSessionContext;
import com.ibm.ws.webcontainer.spiadapter.collaborator.IInvocationCollaborator;
import com.ibm.ws.webcontainer.util.DocumentRootUtils;
import com.ibm.ws.webcontainer.util.EmptyEnumeration;
import com.ibm.ws.webcontainer.util.IteratorEnumerator;
import com.ibm.ws.webcontainer.util.MetaInfResourceFinder;
import com.ibm.ws.webcontainer.util.UnsynchronizedStack;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets;
import com.ibm.wsspi.http.HttpInboundConnection;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.webcontainer.ClosedConnectionException;
import com.ibm.wsspi.webcontainer.RequestProcessor;
import com.ibm.wsspi.webcontainer.WCCustomProperties;
import com.ibm.wsspi.webcontainer.WebContainerRequestState;
import com.ibm.wsspi.webcontainer.annotation.AnnotationHelperManager;
import com.ibm.wsspi.webcontainer.cache.CacheManager;
import com.ibm.wsspi.webcontainer.collaborator.CollaboratorHelper;
import com.ibm.wsspi.webcontainer.collaborator.CollaboratorInvocationEnum;
import com.ibm.wsspi.webcontainer.collaborator.ICollaboratorHelper;
import com.ibm.wsspi.webcontainer.collaborator.IWebAppNameSpaceCollaborator;
import com.ibm.wsspi.webcontainer.collaborator.IWebAppTransactionCollaborator;
import com.ibm.wsspi.webcontainer.collaborator.TxCollaboratorConfig;
import com.ibm.wsspi.webcontainer.collaborator.WebAppInitializationCollaborator;
import com.ibm.wsspi.webcontainer.extension.ExtensionFactory;
import com.ibm.wsspi.webcontainer.extension.ExtensionProcessor;
import com.ibm.wsspi.webcontainer.facade.ServletContextFacade;
import com.ibm.wsspi.webcontainer.filter.IFilterConfig;
import com.ibm.wsspi.webcontainer.filter.IFilterMapping;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.logging.LoggerHelper;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.wsspi.webcontainer.security.SecurityViolationException;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;
import com.ibm.wsspi.webcontainer.servlet.IServletWrapper;
import com.ibm.wsspi.webcontainer.util.EncodingUtils;
import com.ibm.wsspi.webcontainer.util.ServletUtil;
import com.ibm.wsspi.webcontainer.util.ThreadContextHelper;
import com.ibm.wsspi.webcontainer.util.URIMapper;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

/**
 * @author mmolden
 */
@SuppressWarnings("unchecked")
public abstract class WebApp extends BaseContainer implements ServletContext, IServletContextExtended {

    //loginProcessor && logoutProcessor are cached locally - if osgi is used, we will always call the securityCollaborator
    protected ExtensionProcessor loginProcessor = null;
    protected ExtensionProcessor logoutProcessor = null;
    protected ICollaboratorHelper collabHelper;
    private static WSThreadLocal envObject = new WSThreadLocal();
    protected List<String> orderedLibPaths = null;

    protected final static com.ibm.websphere.security.WebSphereRuntimePermission perm = new com.ibm.websphere.security.WebSphereRuntimePermission(
                    "accessServletContext");

    public static String WELCOME_FILE_LIST = "com.ibm.ws.webcontainer.config.WelcomeFileList";

    public static final String DIR_BROWSING_MAPPING = "__dirBrowsing__" + System.currentTimeMillis();

    public static final String FILTER_PROXY_MAPPING = "/__filterProxy__" + System.currentTimeMillis(); // PK15276

    protected static final Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.webapp");
    protected static final String CLASS_NAME = "com.ibm.ws.webcontainer.webapp.WebApp";

    protected ClassLoader loader;

    // begin defect 293789: add ability for components to register
    // ServletContextFactories
    protected ServletContext facade = null;
    protected List<com.ibm.websphere.servlet.container.WebContainer.Feature> features = new ArrayList<com.ibm.websphere.servlet.container.WebContainer.Feature>();
    // private boolean isWarnedOfAsyncDisablement=false;
    // end defect 293789: add ability for components to register
    // ServletContextFactories

    // need to call preInvoke() on this before any control
    // goes to the user application.
    //
    protected String applicationName;
    protected WebAppConfiguration config;
    private Boolean securityEnabledForApp = null;
    protected WebExtensionProcessor webExtensionProcessor;

    protected boolean production = true;

    protected boolean isServlet23;

    protected String contextPath;

    protected String[][] internalServletList = { { "DirectoryBrowsingServlet", "com.ibm.ws.webcontainer.servlet.DirectoryBrowsingServlet" },
                                                 { "SimpleFileServlet", "com.ibm.ws.webcontainer.servlet.SimpleFileServlet" },
    };

    protected final String BY_NAME_ONLY = "/_" + System.currentTimeMillis() + "_/";

    protected WebAppFilterManager filterManager;

    protected String documentRoot;

    public static final String SERVLET_API_VERSION = "Servlet 2.5";

    protected static int disableServletAuditLogging = -1;

    protected String serverInfo = null;

    protected IHttpSessionContext sessionCtx = null;

    protected WebAppEventSource eventSource = new WebAppEventSource();

    protected static final TraceNLS nls = TraceNLS.getTraceNLS(WebApp.class, "com.ibm.ws.webcontainer.resources.Messages");
    protected static final TraceNLS liberty_nls = TraceNLS.getTraceNLS(WebApp.class, "com.ibm.ws.webcontainer.resources.LShimMessages");
    protected static final TraceNLS error_nls = TraceNLS.getTraceNLS(WebApp.class, "com.ibm.ws.webcontainer.resources.ErrorPage");

    protected ArrayList sessionListeners = new ArrayList(); // cmd PQ81253
    protected ArrayList sessionIdListeners = new ArrayList(); //servlet3.1
    protected ArrayList sessionAttrListeners = new ArrayList(); // cmd PQ81253

    // Not adding a list here for SessionIdListeners for Servlet 3.1 because I don't see where these are
    // even used anymore in Liberty.
    protected ArrayList addedSessionListeners = new ArrayList(); // 434577
    protected ArrayList addedSessionAttrListeners = new ArrayList(); // 434577

    protected ArrayList servletContextListeners = new ArrayList();
    protected ArrayList servletContextLAttrListeners = new ArrayList();
    protected ArrayList servletRequestListeners = new ArrayList();
    protected ArrayList servletRequestLAttrListeners = new ArrayList();
    
    protected static boolean prependSlashToResource = false; // 263020
    private Boolean destroyed = Boolean.FALSE;// 325429
    protected IWebAppNameSpaceCollaborator webAppNameSpaceCollab;
    private IWebAppTransactionCollaborator txCollab;

    protected ArrayList sessionActivationListeners = new ArrayList();
    protected ArrayList sessionBindingListeners = new ArrayList();

    private String scratchdir = null;

    private int jspClassLoaderLimit = 0; // PK50133
    // PK82657 - protected LinkedList jspClassLoaders = new LinkedList();
    // //PK50133
    protected ArrayList jspClassLoaderExclusionList = null; // PK50133
    protected JSPClassLoadersMap jspClassLoadersMap = null; // PK82657
    protected boolean jspClassLoaderLimitTrackIF = false; // PK82657

    protected DefaultExtensionProcessor defaultExtProc = null;
    protected DirectoryBrowsingServlet directoryBrowsingServlet = null;

    //CDI  @inject support 
    // Map needs to be thread safe
    protected Map<Object, ManagedObject> cdiContexts = new ConcurrentHashMap<Object, ManagedObject>();
    public Map<Object, ManagedObject> getCdiContexts() {
        return cdiContexts;
    }

    // LIBERTY Added for delayed start.
    protected DeployedModule moduleConfig;
    protected List extensionFactories;
    protected volatile boolean initialized = false;
    private Object lock = new Object() {};    
    private boolean afterServletContextCreated = false;

    private static boolean redirectContextRoot = WCCustomProperties.REDIRECT_CONTEXT_ROOT;

    private static boolean errorExceptionTypeFirst = WCCustomProperties.ERROR_EXCEPTION_TYPE_FIRST;
    private static boolean initFilterBeforeServletInit = WCCustomProperties.INIT_FILTER_BEFORE_INIT_SERVLET; //PM62909
    //protected final static boolean stopAppStartupOnListenerException = WCCustomProperties.STOP_APP_STARTUP_ON_LISTENER_EXCEPTION ; //PI58875 update server.xml without restarting server may not pick up the change dynamically.
    private static boolean SET_400_SC_ON_TOO_MANY_PARENT_DIRS = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.set400scontoomanyparentdirs")).booleanValue(); //PI80786

    private List<IServletConfig> sortedServletConfigs;
    private int effectiveMajorVersion;
    private int effectiveMinorVersion;

    protected boolean canAddServletContextListener = true; //Servlet30 addListenerbehavior
    protected boolean withinContextInitOfProgAddListener = false;
    protected String lastProgAddListenerInitialized; // PI41941
    private ClassLoader webInfLibClassloader;
    protected Map<String, URL> metaInfCache;
    
    protected final static boolean useMetaInfCache = (WCCustomProperties.META_INF_RESOURCES_CACHE_SIZE > 0);

    //The following two JSF listener classes are used to make sure that the JSF ServletContextListener 
    //is fired before the CDI listener when we are using the JSF implementation shipped with WAS.
    private static final String SUN_CONFIGURE_LISTENER_CLASSNAME = "com.sun.faces.config.ConfigureListener";
    private static final String MYFACES_LIFECYCLE_LISTENER_CLASSNAME = "org.apache.myfaces.webapp.StartupServletContextListener";
    private static final String JSF_IMPL_ENABLED_PARAM = "com.ibm.ws.jsf.JSF_IMPL_ENABLED";
    private static final String JSF_IMPL_ENABLED_CUSTOM = "Custom";
    private static final String JSF_IMPL_ENABLED_NONE = "None";
        
    protected DocumentRootUtils staticDocRoot = null;
    protected DocumentRootUtils jspDocRoot = null;
        
    enum InitializationCollaborCommand { STARTING, STARTED, STOPPING, STOPPED };
        
    protected com.ibm.wsspi.adaptable.module.Container container;
    protected MetaInfResourceFinder metaInfResourceFinder;

    public enum ANNOT_TYPE {
        POST_CONSTRUCT(PostConstruct.class),
        PRE_DESTROY(PreDestroy.class);
    
        // This could be replaced by the annotation name.
        // See the comment on 'lookupMethod'.
        public final Class<? extends Annotation> annotationClass;
        
        private ANNOT_TYPE(Class<? extends Annotation> annotationClass) {
            this.annotationClass = annotationClass;
        }
    };
    
    private final AnnotatedMethods postConstructMethods = new AnnotatedMethods(ANNOT_TYPE.POST_CONSTRUCT);
    private final AnnotatedMethods preDestroyMethods = new AnnotatedMethods(ANNOT_TYPE.PRE_DESTROY);
    
    private static Object[] OBJ_EMPTY = new Object[] {};
    private static Class<?>[] CLASS_EMPTY = new Class<?>[] {};
    private volatile boolean isInitializing = false;

    // PK37608 Start
    static {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "<init>", " : suppressWSEPHeader set to " + WCCustomProperties.SUPPRESS_WSEP_HEADER);
        }
        // Begin 263020
        if (WCCustomProperties.PREPEND_SLASH_TO_RESOURCE != null && WCCustomProperties.PREPEND_SLASH_TO_RESOURCE.equals("true")) {
            prependSlashToResource = true;
        }
        // End 263020
    }

  private static final List DEFAULT_JSP_EXTENSIONS = new ArrayList();
  static {
      DEFAULT_JSP_EXTENSIONS.add(".jsp");
      DEFAULT_JSP_EXTENSIONS.add(".jsv");
      DEFAULT_JSP_EXTENSIONS.add(".jsw");
      DEFAULT_JSP_EXTENSIONS.add(".jspx");
  }
  
  public static final boolean DEFER_SERVLET_REQUEST_LISTENER_DESTROY_ON_ERROR = WCCustomProperties.DEFER_SERVLET_REQUEST_LISTENER_DESTROY_ON_ERROR;  //PI26908
  
    // PK37698 End
    public WebApp(WebAppConfiguration webAppConfig, Container parent) {
        super(webAppConfig.getId(), parent);
        this.config = webAppConfig;
        // PK63920 Start
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.entering(CLASS_NAME, "<init> [ " + this + " ] with name -> [ " + name + " ] and parent [ " + parent + " ]");
        // PK63920 End
        this.requestMapper = new URIMapper(true);
        
        if (WCCustomProperties.REMOVE_ATTRIBUTE_FOR_NULL_OBJECT) {
            this.attributes = new ConcurrentHashMap();
        } else {
            this.attributes = Collections.synchronizedMap(new HashMap()); // PK27027
        }

        if (useMetaInfCache) {
            //prevent rehash of the map by making initial capacity one greater than maximum size
            //and loadFactor of 1. If the initial capacity is greater than the maximum number of 
            //entries divided by the load factor, no rehash operations will ever occur.
            //The removeEldestEntry will take effect when we're at the cache size + 1.
            metaInfCache = new LinkedHashMap(WCCustomProperties.META_INF_RESOURCES_CACHE_SIZE + 1, 1.0f, true) {
                public boolean removeEldestEntry(Map.Entry eldest) {
                    return size() > WCCustomProperties.META_INF_RESOURCES_CACHE_SIZE;
                }
            };
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { // 306998.15
            logger.exiting(CLASS_NAME, "<init> name --> " + name);
        }
    }
    
    // No longer in used. See the prior call from 'initialize'.
    // public void setupWebAppAnnotations () {
    // }

    /**
     * <p>Data type used to store lookup results for <code>javax.annotation.PostConstruct</code>
     * and <code>javax.annotation.PreDestroy</code> on invocation targets.</p>
     */
    public static class AnnotatedMethods {
        /** <p>Each data set is for a specified annotation type.</p> */
        public final Class<? extends Annotation> annotationClass;
        
        /**
         * <p>Classes which have no occurrences of the specified annotation on
         * declared methods.</p>
         */
        private final Set<String> noOpClassNames = new HashSet<String>();
        
        /**
         * <p>A mapping of class names to method names, recording the method
         * of the class which has the specified annotation.</p>
         */
        private final Map<String, String> annotatedMethodNames = new ConcurrentHashMap<String, String>();

        /**
         * <p>Standard constructor.  Obtain the annotation class from the
         * annotation type.</p>
         * 
         * @param annotationType A type encoding the annotation which is to be handled.
         *
         * @link #annotationName
         */
        public AnnotatedMethods(ANNOT_TYPE annotationType) {
            this.annotationClass = annotationType.annotationClass;
        }
        
        /**
         * <p>Standard constructor.  Set the annotation class directly.</p>
         * 
         * @param annotationClass The class of the annotation which is to be handled.
         *
         * @link #annotationName
         */        
        public AnnotatedMethods(Class<? extends Annotation> annotationClass) {
            this.annotationClass = annotationClass;
        }

        private static final Method[] EMPTY_METHODS = new Method[] {};
        
        /**
         * <p>Look for the target annotation on declared methods of
         * the specified class.  Answer the name of the method which
         * has the target annotation.  Answer null if no methods have
         * the target annotation.</p>
         * 
         * <p>At most one occurrence of the target annotation is expected.
         * If more than one method has the target annotation, the return
         * value is the name of one of the annotated methods, selected
         * arbitrarily.</p>
         * 
         * <p>Cache the result for later lookups.</p>
         * 
         * <p>Synchronized to ensure safe updates to the result cache.</p>
         * 
         * @param targetClass The class to search for the target annotation.
         * 
         * @return The name of the declared method having the target annotation.
         *         Null if no methods have the target annotation. 
         */
        // Synchronize: Concurrent get/put are possible.
        // All access to storage (noOpClassNames and annotatedMethodNames) are
        // performed through lookupMethod.
        private Method lookupMethod(final Class<?> targetClass) throws NoSuchMethodException, InjectionException {
            String methodName = "lookupMethod";
            
            String targetClassName = targetClass.getName();
            
            boolean outcome = false;
            while (!outcome) {
                try {
                    if (noOpClassNames.contains(targetClassName)) {
                        outcome = true;
                        return null;
                    }
                    outcome = true;                    
                } catch (java.util.ConcurrentModificationException cme) {
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                        logger.logp(Level.FINE, CLASS_NAME, methodName, "Exception accessing noOpClassNames: " +cme.getMessage());
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        //ignore                        
                    }
                }
            }
            
            String annotatedMethodName = annotatedMethodNames.get(targetClassName);
            if (annotatedMethodName != null) {
                // Would storing the methods instead of their names be better?
                //
                // That would prevent additional method lookups, but the method object is
                // much heavier object than the method name, and, storing the method object
                // might hold the class and java reflection objects for too long.
                
                return targetClass.getDeclaredMethod(annotatedMethodName, CLASS_EMPTY);
                // throws NoSuchMethodException, SecurityException
            }
                
            Method annotatedMethod = null;

            Method[] declaredMethods;            
            try {                
                declaredMethods = (Method [])AccessController.doPrivileged(new java.security.PrivilegedAction<Object []>()
                {
                    public Object[] run()
                    {
                        return(targetClass.getDeclaredMethods());
                    }
                });

                
            } catch (NoClassDefFoundError e) {
                // See the comment preceding this method for a sample stack showing a
                // problem of using reflection to locate annotated methods.
                
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {                
                    logger.logp(Level.FINE, CLASS_NAME, methodName,                            
                                "Unable to process [ {0} ] methods of class [ {2} ]: {3}",
                                new Object[] { annotationClass, targetClass, e} );
                }
                declaredMethods = EMPTY_METHODS; 
            }
            
            for (Method declaredMethod : declaredMethods) {
                Annotation annotation = declaredMethod.getAnnotation(annotationClass);
                if (annotation == null) {
                    continue;
                }
                
                // A loop could be used, if the reference to the annotation class were
                // to be avoided.
                //
                // 'getAnnotation' sees inherited annotations.  The current two
                // uses, PostConstruct and PreDestroy, are not inheritable, so this
                // is not a problem.
                //
                // Conceivably, there might be more work to obtain all annotations
                // on a method (which must look upwards for inherited values).
                //
                // for ( Annotation annotation : declaredMethod.getDeclaredAnnotations() ) {
                //     if (annotation.annotationType().getName().equals(annotationClass.getName()) ) {
                    
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                        logger.logp(Level.FINE, CLASS_NAME, methodName,                            
                                    "Located [ {0} ] method [ {1} ] of class [ {2} ]",
                                    new Object[] { annotationClass, declaredMethod, targetClassName });
                    }

                    // Having more than one method having the target annotation is not valid.
                    // might need to not break and check if more than one method and throw the exception later ...
                    if (annotatedMethod == null) {
                        annotatedMethod = declaredMethod;
                    } else {
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                            logger.logp(Level.FINE, CLASS_NAME, methodName,                            
                                        "Multiple methods annotated with [ {0} ] in class [ {1} ]",
                                        new Object[] { annotationClass, targetClassName });
                        }
                        throw new InjectionException();
                    }                    
            }

            if (annotatedMethod == null) {
                synchronized(noOpClassNames) {
                    noOpClassNames.add(targetClassName);
                }
            
            } else {
                // Would storing the methods instead of their names be better?
                //
                // No method lookup on reuse, but the method object is much heavier
                // object than the method name.  Storing the method would avoid
                // dealing with NoSuchMethodException handling on re-use.
                
                annotatedMethodNames.put(targetClassName, annotatedMethod.getName());
            }

            return annotatedMethod;
        }

        /**
         * <p>Select the target methods of a particular object and for a particular annotation.</p>
         * 
         * <p>Answer the target methods in a list sorted in super class order (top-most first),
         * and partitioned by class.</p>
         * 
         * <p>Each element of the list has a non-empty list of methods.</p>
         * 
         * <p>Only methods with empty parameter lists are selected.  The return type of the
         * methods is not yet checked.</p>
         * 
         * <p>Data for the selection is computed by {@link #lookupMethod(Class<?>)}.</p>
         * 
         * @param obj The target object for which to select annotated methods.
         * 
         * @return The list of target methods, in superclass order, partitioned by class.
         */
        public List<Method> selectMethods(Object obj) throws NoSuchMethodException, InjectionException {
            List<Method> annotatedMethods = null;

            // Would a cache of names of classes with empty results for their
            // overall result be worth keeping?

            Class<? extends Object> originalClass = obj.getClass();
            List<Class<? extends Object>> classesList = new ArrayList<Class<? extends Object>>();
            for (Class<?> objClass = originalClass; objClass != null; objClass = objClass.getSuperclass()) {
                Method annotatedMethod = lookupMethod(objClass); // throws NoSuchMethodException
                if (annotatedMethod != null) {
                    if (annotatedMethods == null) {
                        annotatedMethods = new ArrayList<Method>();
                    }

                    //this doesn't match the tWAS behavior
                    
                    //private and package can't be overridden
                    //protected requires looking at every class
                    //public can just call originalClass.getMethod
                    Method m = null;
                    if (Modifier.isProtected(annotatedMethod.getModifiers())) {
                        //need to go through the list of classes and find if any declared methods override it
                        for (Class<? extends Object> c: classesList) {
                            try {
                                m = c.getDeclaredMethod(annotatedMethod.getName(), annotatedMethod.getParameterTypes());
                                break;
                            } catch (NoSuchMethodException e) {
                                //this is fine ...
                            }
                        }
                    } else if (Modifier.isPublic(annotatedMethod.getModifiers())) {
                        try {
                            m = originalClass.getMethod(annotatedMethod.getName(), annotatedMethod.getParameterTypes());
                        } catch (NoSuchMethodException e) {
                            //this is fine ...
                        }    
                    }
                    else if (!Modifier.isPrivate(annotatedMethod.getModifiers())) {
                        //not public, protected, or private - must be package
                        //need to go through the list of classes and find if any declared methods override it
                        for (Class<? extends Object> c: classesList) {
                            try {
                                m = c.getDeclaredMethod(annotatedMethod.getName(), annotatedMethod.getParameterTypes());
                                if (c.getPackage().equals(annotatedMethod.getDeclaringClass().getPackage())) {
                                    //if package is equal then the method is overwritten
                                    break;
                                } else {
                                    //continue looking for methods in parent tree
                                    m=null;
                                    continue;
                                }
                            } catch (NoSuchMethodException e) {
                                //this is fine ...
                            }
                        }
                    }

                    if (m==null || (m.getDeclaringClass().equals(annotatedMethod.getDeclaringClass()))) {
                        annotatedMethods.add(annotatedMethod);
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
                            logger.logp(Level.FINEST, CLASS_NAME, "selectMethods",                            
                                        "Adding annotated method {0} in {1} class to the list.",
                                        new Object[] { annotatedMethod.getName(), objClass });
                        }
                    } else {
                        //a different sub-class declared this method, don't add
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
                            logger.logp(Level.FINEST, CLASS_NAME, "selectMethods",                            
                                        "Annotated method {0} in {1} class overridden by subclass.  Not adding to the list.",
                                        new Object[] { annotatedMethod.getName(), objClass });
                        }
                    }
                }
                classesList.add(objClass);
            }
            
            if (annotatedMethods == null) {
                return Collections.EMPTY_LIST;
                
            } else {
                // TODO: Will need to handle method overloading.
                
                // The methods are invoked from the top-most super class to the current class.
                Collections.reverse(annotatedMethods);
                return annotatedMethods;
            }
        }        
    }

    /** Control parameter: Accept annotations on partial classes. */
    protected final static boolean DO_ACCEPT_PARTIAL = true;
    /** Control parameter: Do not accept annotations on partial classes. */
    protected final static boolean DO_NOT_ACCEPT_PARTIAL = false;
    
    /** Control parameter: Accept annotations on excluded classes. */  
    protected final static boolean DO_ACCEPT_EXCLUDED = true;
    /** Control parameter: Do not accept annotations on excluded classes. */    
    protected final static boolean DO_NOT_ACCEPT_EXCLUDED = false;
    
    /**
     * Tell if annotations on a target class are to be processed.  This is
     * controlled by the metadata-complete and absolute ordering settings
     * of the web module.
     * 
     * Metadata complete may be set for the web-module as a whole, or may be set
     * individually on fragments.  Annotations are ignored when the target class  
     * is in a metadata complete region.
     * 
     * When an absolute ordering element is present in the web module descriptor,
     * jars not listed are excluded from annotation processing.
     * 
     * Control parameters are provided to allow testing for the several usage cases.
     * See {@link com.ibm.wsspi.anno.classsource.ClassSource_Aggregate} for detailed
     * documentation.
     * 
     * Caution: Extra testing is necessary for annotations on inherited methods.
     * An inherited methods have two associated classes: The class which defined
     * the method and the class which is using the method definition.
     * 
     * Caution: Extra testing is necessary for inheritable annotations.  Normally,
     * class annotations apply only to the class which provides the annotation
     * definition.  As a special case, a class annotation may be declared as being
     * inherited, in which case the annotation applies to all subclasses of the
     * class which has the annotation definition.
     * 
     * @param className The name of the class which is to be tested.
     * @param acceptPartial Control parameter: Tell if partial classes are accepted.
     * @param acceptExcluded Control parameter: Tell if excluded classes are accepted.
     * 
     * @return True if annotations are accepted from the class.  Otherwise, false.
     */
    protected boolean acceptAnnotationsFrom(String className, boolean acceptPartial, boolean acceptExcluded) {
        // As an optimization, immediately return with a false - do-not-accept - value
        // when the web module is metadata complete and neither partial nor excluded
        // classes are accepted:
        //
        // Always accept annotations from seed classes.
        //
        // Accept classes from metadata-complete but non-excluded regions of the web module
        // if 'acceptPartial' is true.
        //
        // Accept classes from excluded regions of the web module if 'acceptExcluded' is true.
        //
        // If 'acceptPartial' and 'acceptExcluded' are both false, then only accept a class
        // if that class is in a non-metadata-complete region of the web module.
        //
        // If the web module as a whole is metatadata complete, the non-metadata complete
        // region is empty.

        if ( config.isMetadataComplete() ) {
            if ( !acceptPartial && !acceptExcluded ) {
                return false;
            }
        }

        AnnotationTargets_Targets targets = getAnnotationTargets();
        if ( targets == null ) {
        	return false; // Annotations failure
        }

        return ( targets.isSeedClassName(className) ||
                 (acceptPartial && targets.isPartialClassName(className)) ||
                 (acceptExcluded && targets.isExcludedClassName(className)) );
    }

    /**
     * Answer the annotation targets of the web module.
     *
     * @return The annotations targets of the web module.  Return null if
     *     annotation processing fails.
     */
    private AnnotationTargets_Targets getAnnotationTargets() {
        try {
            // Conditionally use the cache enabled implementation.
            WebAnnotations webAnnotations = AnnotationsBetaHelper.getWebAnnotations( getModuleContainer() );
            return webAnnotations.getAnnotationTargets();
        } catch ( UnableToAdaptException e ) {
            return null; // FFDC
        } 
    }
    
    /**
     * <p>Invoke post-construct and pre-destroy methods on the target object.</p>
     * 
     * <p>Method are selected on the super class chain of the target object, including
     * the class of the target object.  Invocations are performed starting with the
     * method from the top-most class.  All invocations are performed on the target
     * object.</p>
     * 
     * <p>This implementation does not handle cases where a method has been overridden
     * between classes, and does not handle cases where a method has a method protection
     * which is not valid to allow invocation.</p>
     * 
     * <p>The selected methods must have zero parameters and a void return type.  Methods
     * with one or more parameters are ignored.  Methods with zero parameters and with a
     * non-null return type cause an exception to be returned.</p>
     * 
     * <p>Failure to invoke a method, either because a method has a non-null return type,
     * or because an exception thrown by the method, cause invocation to halt, with
     * a return value of an <code>InjectionException</code>.</p>
     * 
     * <p>See the extensive comments on {@link javax.annotation.PostConstruct} and
     * {@link javax.annotation.PreDestroy} for an initial discussion of the rules
     * for post-construct and pre-destroy methods.</p>
     * 
     * @param obj The object for which to invoke post-construct or pre-destroy methods.
     * @param type The type of methods (post-construct or pre-destroy) which are to
     *             be invoked.
     *             
     * @return The result of invoking the methods.  Null if all invocations are
     *         successful.  A throwable in case of an error during the invocation.
     */
    public Throwable invokeAnnotTypeOnObjectAndHierarchy(Object obj, ANNOT_TYPE type) {
        String methodName = "invokeAnnotTypeOnObjectAndHierarchy";

        if (obj == null) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, methodName, "Unable to invoke [ {0} ] methods: Null object", type);
            }
            return null;
        }

        if (type == ANNOT_TYPE.POST_CONSTRUCT) {
            //need to validate the preDestroy methods as well
            Throwable t = validateAndRun(false, obj, ANNOT_TYPE.PRE_DESTROY);
            if (t!=null) {
                return t; //only return here if a throwable was thrown
            }
        }
        //validate and run the methods
        return validateAndRun(true, obj, type);
        
   }
    
    private Throwable validateAndRun(boolean run, Object obj, ANNOT_TYPE type) {
        String methodName = "validateAndRun";
        List<Method> annotatedMethods;
        try {
            AnnotatedMethods targetPool = ( (type == ANNOT_TYPE.POST_CONSTRUCT) ? postConstructMethods : preDestroyMethods );
            annotatedMethods = targetPool.selectMethods(obj); // throws NoSuchMethodException 
        } catch (NoSuchMethodException e) {
            // This case is very unlikely: The cached method data would need to become
            // inconsistent with the Class data.  That should not be possible unless the
            // cache is retained while reloading the class.  Not possible today, but maybe
            // possible later when class updates are allowed.

            // Wrap this exception in an InjectionException so the error will be correctly
            // processed and a 404 returned to the user.
            InjectionException ie = new InjectionException(e);
            return ie;
        } catch (InjectionException e) {
            return e;
        }

        for (Method annotatedMethod : annotatedMethods) {
            Class<?> targetClass = annotatedMethod.getDeclaringClass();

            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, methodName, "Invoke method [ {0} ] from [ {1} ]" , new Object[] { annotatedMethod, targetClass });
            }

            try {
                //Can't be static
                //No non-runtimeExceptions are allowed
                //No parameters are allowed
                //Return type was non-void, which is not allowed.

                boolean badExceptionType = false;
                Class<?>[] exceptionTypes = annotatedMethod.getExceptionTypes();
                for (Class<?> c:exceptionTypes) {
                    if (!RuntimeException.class.isAssignableFrom(c)) {
                        badExceptionType = true;                            
                    }
                }

                if (Modifier.isStatic(annotatedMethod.getModifiers()) ||
                                (badExceptionType) ||
                                (annotatedMethod.getParameterTypes().length>0) ||
                                (!(annotatedMethod.getReturnType() == void.class))) {
                    logger.logp(Level.SEVERE, CLASS_NAME, methodName, "unable.to.invoke.method", new Object[] { annotatedMethod, targetClass });

                    // Throw back an exception: The client should see a 404 error.
                    String s = MessageFormat.format(nls.getString("unable.to.invoke.method",
                                                                  "SRVE8061E: Unable to invoke method [{0}] of class [{1}]\n"),
                                                                  new Object[] { annotatedMethod, targetClass });                        
                    InjectionException se = new InjectionException(s);
                    return se;  
                }
                
                if (run) {
                    final Method m = annotatedMethod;                
                    AccessController.doPrivileged(new PrivilegedExceptionAction<Method>() {
                        public Method run() throws Exception {
                            m.setAccessible(true); // throws SecurityException
                            return m;
                        }
                    });
                    m.invoke(obj, OBJ_EMPTY); // throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
                }
            } catch (Throwable t) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, methodName, "Unable to obtain, set access, or invoke method ", t);
                }

                if (t instanceof InjectionException) {
                    return t;
                    
                } else {
                    // Wrap this exception in an InjectionException so the error will be correctly
                    // processed and a 404 returned to the user.
                    InjectionException ie = new InjectionException(t);
                    return ie;
                }
            }
       }
    
       return null; 
    }

    // BEGIN: NEVER INVOKED BY WEBSPHERE APPLICATION SERVER (Common Component
    // Specific)
    public void initialize(WebAppConfiguration config, DeployedModule moduleConfig, // BEGIN:
                           List extensionFactories) throws ServletException, Throwable {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.entering(CLASS_NAME, "Initialize WebApp -> [ " + this + " ]");

        this.loader = moduleConfig.getClassLoader(); // NEVER INVOKED BY
        // WEBSPHERE APPLICATION
        // SERVER (Common Component
        // Specific)
        this.applicationName = config.getApplicationName(); // NEVER INVOKED BY
        // WEBSPHERE APPLICATION
        // SERVER (Common
        // Component Specific)

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME, "initialize", "Initializing application " + this.applicationName);
        // PK63920 End
        serverInfo = getServerInfo(); // NEVER INVOKED BY WEBSPHERE APPLICATION
        // SERVER (Common Component Specific)

        // Initialize the Logger for IDGeneratorImpl before setting Thread context ClassLoader
        // in order to avoid using the application ClassLoader to load the Logger's resource bundle.
        IDGeneratorImpl.init();

        ClassLoader origClassLoader = null; // NEVER INVOKED BY WEBSPHERE
        // APPLICATION SERVER (Common
        // Component Specific)
        boolean webAppNameCollPreInvokeCalled = false;
        try {
            origClassLoader = ThreadContextHelper.getContextClassLoader(); // NEVER
            // INVOKED
            // BY
            // WEBSPHERE
            // APPLICATION
            // SERVER
            // (Common
            // Component
            // Specific)
            final ClassLoader warClassLoader = getClassLoader(); // NEVER
            // INVOKED BY
            // WEBSPHERE
            // APPLICATION
            // SERVER
            // (Common
            // Component
            // Specific)
            if (warClassLoader != origClassLoader) // NEVER INVOKED BY WEBSPHERE
            // APPLICATION SERVER (Common
            // Component Specific)
            {
                ThreadContextHelper.setClassLoader(warClassLoader); // NEVER
                // INVOKED
                // BY
                // WEBSPHERE
                // APPLICATION
                // SERVER
                // (Common
                // Component
                // Specific)
            } else {
                origClassLoader = null; // NEVER INVOKED BY WEBSPHERE
                // APPLICATION SERVER (Common Component
                // Specific)
            }

            commonInitializationStart(config, moduleConfig); // NEVER INVOKED BY
            // WEBSPHERE
            // APPLICATION
            // SERVER (Common
            // Component
            // Specific)
            
            callWebAppInitializationCollaborators(InitializationCollaborCommand.STARTING);

            // No longer in use; post-construct and pre-destroy are located on demand.
            // Find annotations like PostConstruct and PreDestroy on objects in this web app
            // setupWebAppAnnotations();

            webAppNameSpaceCollab.preInvoke(config.getMetaData().getCollaboratorComponentMetaData()); //added 661473
            webAppNameCollPreInvokeCalled = true;
            commonInitializationFinish(extensionFactories); // NEVER INVOKED BY
            
            this.initializeServletContainerInitializers(moduleConfig);
            
            loadLifecycleListeners(); //added 661473
            // WEBSPHERE
            // APPLICATION
            // SERVER (Common
            // Component
            // Specific)
            try {
                //moved out of commonInitializationFinish
                notifyServletContextCreated();
            } catch (Throwable th) {
                // pk435011
                logger.logp(Level.SEVERE, CLASS_NAME, "initialize", "error.notifying.listeners.of.WebApp.start", new Object[] { th });
                if (WCCustomProperties.STOP_APP_STARTUP_ON_LISTENER_EXCEPTION) {          //PI58875
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                        logger.logp(Level.FINE, CLASS_NAME, "initialize", "rethrowing exception due to stopAppStartupOnListenerException");
                    throw th;
                }
            }
            commonInitializationFinally(extensionFactories); // NEVER INVOKED BY
            // WEBSPHERE
            // APPLICATION
            // SERVER (Common
            // Component
            // Specific)
            
            // Fix for 96420, in which if the first call to AnnotationHelperManager happens in destroy(), we can get 
            // errors because the bundle associated with the thread context classloader may have been uninstalled, 
            // resulting in us being unable to load a resource bundle for AnnotationHelperManager. 
            AnnotationHelperManager.verifyClassIsLoaded();
            
        } finally {
            // if initialization failed, this can be null.
            if (webAppNameCollPreInvokeCalled) {
                webAppNameSpaceCollab.postInvoke(); //added 661473            
            }
            if (origClassLoader != null) // NEVER INVOKED BY WEBSPHERE
            // APPLICATION SERVER (Common Component
            // Specific)
            {
                final ClassLoader fOrigClassLoader = origClassLoader; // NEVER
                // INVOKED
                // BY
                // WEBSPHERE
                // APPLICATION
                // SERVER
                // (Common
                // Component
                // Specific)

                ThreadContextHelper.setClassLoader(fOrigClassLoader); // NEVER
                // INVOKED
                // BY
                // WEBSPHERE
                // APPLICATION
                // SERVER
                // (Common
                // Component
                // Specific)
            }
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                logger.exiting(CLASS_NAME, "initializeTargetMappings");
        }
        // PK63920 Start
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.exiting(CLASS_NAME, "Initialize WebApp -> [ " + this + " ] ApplicationName -> [ " + config.getApplicationName() + " ]");
        // PK63920 End
    }

    protected void commonInitializationFinish(List extensionFactories) {
        
        if (com.ibm.ws.webcontainer.osgi.WebContainer.isServerStopping())
            return;
        
        try {
            initializeExtensionProcessors(extensionFactories);
        } catch (Throwable th) {
            // pk435011
            logger.logp(Level.SEVERE, CLASS_NAME, "commonInitializationFinish", "error.initializing.extension.factories", new Object[] { th });

        }
        //We need to move this above createServletWrappers so that the correct facades are reference from getServletContext methods
        initializeServletContextFacades();
        try {
            createServletWrappers();
        } catch (Throwable th) {
            logger.logp(Level.SEVERE, CLASS_NAME, "commonInitializationFinish", "error.while.initializing.servlets", new Object[] { th });
        }

        initFilterConfigs();

        // End 309151, Undo Call-Order change of MetaDataListener and
        // ExtensionProcess

    }

    private void initFilterConfigs() {
        Iterator<IFilterConfig> filterInfos = this.config.getFilterInfos();
        while (filterInfos.hasNext()) {
            filterInfos.next().setIServletContext(this);
        }
    }

    // PK83345 Start
    protected void commonInitializationFinally(List extensionFactories) {
        // PM62909 , Specification Topic :: Web Application Deployment, Initialize Filters before Startup Servlets
        
        if (com.ibm.ws.webcontainer.osgi.WebContainer.isServerStopping())
            return;

        if(initFilterBeforeServletInit){
            try {   
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                    logger.logp(Level.FINE,CLASS_NAME, "commonInitializationFinally", "initFilterBeforeServletInit set");      
                initializeFilterManager();
            } catch (Throwable th) {
                logger.logp(Level.SEVERE, CLASS_NAME, "commonInitializationFinally", "error.initializing.filters", th);
            }
        } //PM62909 End
        try {
            
            doLoadOnStartupActions();
        } catch (Throwable th) {
            // pk435011
            logger.logp(Level.SEVERE, CLASS_NAME, "commonInitializationFinally", "error.while.initializing.servlets", new Object[] { th });

        }
        try {
            initializeTargetMappings();
        } catch (Throwable th) {
            // pk435011
            logger.logp(Level.SEVERE, CLASS_NAME, "commonInitializationFinally", "error.while.initializing.target.mappings", th);

        }
        if(!initFilterBeforeServletInit){ //PM62909
            try {
                // initialize filter manager
                // keeping old implementation for now
                initializeFilterManager();
            } catch (Throwable th) {
                // pk435011
                logger.logp(Level.SEVERE, CLASS_NAME, "commonInitializationFinally", "error.initializing.filters", th);
            }
        }

    }

    private void doLoadOnStartupActions() throws Exception {
        for (IServletConfig iServletConfig : this.sortedServletConfigs) {
            if (iServletConfig != null && !com.ibm.ws.webcontainer.osgi.WebContainer.isServerStopping()) {
                if (iServletConfig.getServletWrapper() != null) {
                    // Liberty - if a servlet init fails, report error but continue to initialize remaining servlets
                    try {
                        iServletConfig.getServletWrapper().loadOnStartupCheck();
                    } catch (Throwable t) { //PI20514
                        logger.logp(Level.SEVERE, CLASS_NAME, "doLoadOnStartupActions", "error.initializing.servlet", new Object[] { iServletConfig.getServletName(), t }); //PI20514
                    }
                } else if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "doLoadOnStartupActions", "servletWrapper for iServletConfig=>" + iServletConfig.getServletName() + " is null");
                }

            } else if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "doLoadOnStartupActions", "iServletConfig is null");
            }
        }
    }
    
    private void callWebAppInitializationCollaborators(InitializationCollaborCommand command) {
        Set<WebAppInitializationCollaborator> webAppInitializationCollaborators = CollaboratorServiceImpl.getWebAppInitializationCollaborator();
        if (webAppInitializationCollaborators != null) {
            for (WebAppInitializationCollaborator collab: webAppInitializationCollaborators) {
                try
                {
                    com.ibm.wsspi.adaptable.module.Container moduleContainer = this.getModuleContainer();
                    switch (command) {
                        case STARTED:
                            collab.started(moduleContainer);
                            break;
                        case STARTING:
                            collab.starting(moduleContainer);
                            break;
                        case STOPPING:
                            collab.stopping(moduleContainer);
                            break;
                        case STOPPED:
                            collab.stopped(moduleContainer);
                            break;
                    }
                } catch (Throwable t) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.webcontainer.webapp.WebApp.callWebAppInitializationCollaborators", "685", this);
                    //logger.logp(Level.SEVERE, CLASS_NAME,"WebApp", nls.getString("error.on.collaborator.started.call", "Error occured while invoking initialization collaborator call."));
                }
            }
        }
    }
    
    // Begin 299205, Collaborator added in extension processor recieves no
    // events
    protected void commonInitializationStart(WebAppConfiguration config, DeployedModule moduleConfig) throws Throwable {
        // End 299205, Collaborator added in extension processor recieves no
        // events
        WebGroupConfiguration webGroupCfg = ((WebGroup) parent).getConfiguration();
        isServlet23 = webGroupCfg.isServlet2_3();
        int versionID = webGroupCfg.getVersionID();
        effectiveMajorVersion = versionID / 10;
        effectiveMinorVersion = versionID % 10;

        collabHelper = createCollaboratorHelper(moduleConfig); // must happen
        // before
        // createSessionContext
        // which calls
        // startEnvSetup
        webAppNameSpaceCollab = collabHelper.getWebAppNameSpaceCollaborator();
        // LIBERTY: make sure we initialize the connector collaborator before trying to use it
        collabHelper.getWebAppConnectionCollaborator();

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "commonInitializationStart", "servlet spec version -->" + versionID +
                                                                             "effectiveMajorVersion->" + effectiveMajorVersion +
                                                                             "effectiveMinorVersion->" + effectiveMinorVersion);
        }
        contextPath = ((WebGroup) parent).getConfiguration().getContextRoot();
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "commonInitializationStart", "contextPath -->" + contextPath);
        }
        this.webExtensionProcessor = this.getWebExtensionProcessor();

        this.staticDocRoot = new DocumentRootUtils(this,this.config,DocumentRootUtils.STATIC_FILE);
        this.jspDocRoot = new DocumentRootUtils(this,this.config,DocumentRootUtils.JSP);
        
        // LIBERTY CMD placeholder until we get metadata
        //WebModuleMetaData cmd = null;
        //webAppNameSpaceCollab.preInvoke(cmd);

        webAppNameSpaceCollab.preInvoke(config.getMetaData().getCollaboratorComponentMetaData());
        try {
            loadWebAppAttributes();

            // loadLifecycleListeners();
            //since we have now removed clearing the listeners from within loadLifecycleListeners (due to when it is being called), 
            //we need to add this method to clear listeners now in case there was an error and the app gets updated
            clearLifecycleListeners();
        } finally {
            webAppNameSpaceCollab.postInvoke();
        }

        registerGlobalWebAppListeners();
        txCollab = collabHelper.getWebAppTransactionCollaborator();
        createSessionContext(moduleConfig);
        eventSource
                        .onApplicationStart(new ApplicationEvent(this, this, new com.ibm.ws.webcontainer.util.IteratorEnumerator(config.getServletNames())));
    }

    public abstract WebExtensionProcessor getWebExtensionProcessor();

    /**
     * Method createSessionContext.
     * 
     * @param moduleConfig
     */
    protected void createSessionContext(DeployedModule moduleConfig) throws Throwable {
        try {
            // added sessionIdListeners for Servlet 3.1
            ArrayList sessionRelatedListeners[] = new ArrayList[] { sessionListeners, sessionAttrListeners, sessionIdListeners }; // cmd
            // PQ81253
            this.sessionCtx = ((WebGroup) parent).getSessionContext(moduleConfig, this, sessionRelatedListeners); // cmd
            // PQ81253
        } catch (Throwable th) {
            // pk435011
            logger.logp(Level.SEVERE, CLASS_NAME, "createSessionContext", "error.obtaining.session.context", th);
            throw new WebAppNotLoadedException(th.getMessage());
        }

    }

    /**
     * Method initializeFilterManager.
     */
    protected void initializeFilterManager() {
        if (filterManager != null)
            return;

        filterManager = new WebAppFilterManager(config, this);

        filterManager.init();
    }

    /**
     * Method initializeTargetMappings.
     */
    protected void initializeTargetMappings() throws Exception {
        // NOTE: namespace preinvoke/postinvoke not necessary as the only
        // external
        // code being run is the servlet's init() and that is handled in the
        // ServletWrapper

        // check if an extensionFactory is present for *.jsp:
        // We do this by constructing an arbitrary mapping which
        // will only match the *.xxx extension pattern
        //
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.entering(CLASS_NAME, "initializeTargetMappings");

        initializeStaticFileHandler();

        initializeInvokerProcessor();

        if (config.isDirectoryBrowsingEnabled()) {
            try {
                IServletWrapper dirServlet = getServletWrapper("DirectoryBrowsingServlet");
                requestMapper.addMapping(DIR_BROWSING_MAPPING, dirServlet);
            } catch (WebContainerException wce) {
                // pk435011
                logger.logp(Level.WARNING, CLASS_NAME, "initializeTargetMappings", "mapping.for.directorybrowsingservlet.already.exists");

            } catch (Exception exc) {
                // pk435011
                logger.logp(Level.WARNING, CLASS_NAME, "initializeTargetMappings", "mapping.for.directorybrowsingservlet.already.exists");
            }
        }

    }

    /**
     * Method createServletWrappers.
     */
    protected void createServletWrappers() throws Exception {
        // NOTE: namespace preinvoke/postinvoke not necessary as the only
        // external
        // code being run is the servlet's init() and that is handled in the
        // ServletWrapper

        // check if an extensionFactory is present for *.jsp:
        // We do this by constructing an arbitrary mapping which
        // will only match the *.xxx extension pattern
        //
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.entering(CLASS_NAME, "createServletWrappers");

        WebExtensionProcessor jspProcessor = (WebExtensionProcessor) requestMapper.map("/dummyPath.jsp");

        if (jspProcessor == null) {
            // No extension processor present to handle this kind of
            // target. Hence warn, skip.
            // pk435011
            // LIBERTY: changing log level to debug as this is valid if there's no JSP support configured
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                logger.logp(Level.FINE, CLASS_NAME, "createServletWrappers", "No Extension Processor found for handling JSPs");
            //logger.logp(Level.WARNING, CLASS_NAME, "createServletWrappers", "no.jsp.extension.handler.found");

        }

        Iterator<IServletConfig> sortedServletConfigIterator = sortNamesByStartUpWeight(config.getServletInfos());
        Map<String, List<String>> mappings = config.getServletMappings();
        String path = null;
        IServletConfig servletConfig;
        IServletWrapper wrapper = null;
        while (sortedServletConfigIterator.hasNext() && !com.ibm.ws.webcontainer.osgi.WebContainer.isServerStopping()) {
            wrapper = null; // 248871: reset wrapper to null
            servletConfig = sortedServletConfigIterator.next();
            String servletName = servletConfig.getServletName();
            List<String> mapList = mappings.get(servletConfig.getServletName());
            servletConfig.setServletContext(this.getFacade());

            // Begin 650884
            // WARNING!!! We shouldn't map by name only as there is
            // no way to configure a security constraint
            // for a dynamically added path.

            //Consolidate the code to setup a single entry map list when its mapped by name only
            // if (mapList==null){
            // //WARNING!!! We shouldn't map by name only as there is
            // //no way to configure a security constraint
            // //for a dynamically added path.
            // //Also, if there was nothing mapped to the servlet
//                              //in web.xml, we would have never called WebAppConfiguration.addServletMapping
//                              //which sets the mappings on sconfig. Adding the list directly to the hashMap short
//                              //circuits that logic so future calls to addMapping on the ServletConfig wouldn't work
            // //unless there was at least one mapping in web.xml
            //                  
            //                  
            // // hardcode the path, since it had no mappings
            // String byNamePath = BY_NAME_ONLY + servletName;
            //
            // // Add this to the config, because we will be looking at
            // // the mappings in order to get to the servlet through the
            // // mappings in the config.
            // mapList = new ArrayList<String>();
            // mapList.add(byNamePath);
            // mappings.put(servletName, mapList);
            // }
            // End 650884

            if (mapList == null || mapList.isEmpty()) {
                wrapper = jspAwareCreateServletWrapper(jspProcessor,
                                                       servletConfig, servletName);
            }
            else {
                for (String urlPattern : mapList) {
                    path = urlPattern;

                    if (path == null) {
                        // shouldn't happen since there is a mapping specified
                        // but too bad the user can never hit the servlet
                        // pk435011
                        // Begin 650884
                        logger.logp(Level.SEVERE, CLASS_NAME, "createServletWrappers", "illegal.servlet.mapping", servletName); // PK33511
                        // path = "/" + BY_NAME_ONLY + "/" + servletName;
                        // End 650884
                    } else if (path.equals("/")) {
                        path = "/*";
                    }

                    if (wrapper == null) { // 248871: Check to see if we've
                        // already found wrapper for
                        // servletName

                        wrapper = jspAwareCreateServletWrapper(jspProcessor,
                                                               servletConfig, servletName);

                        if (wrapper == null)
                            continue;
                    }
                    try {
                        // Begin:248871: Check to see if we found the wrapper
                        // before adding
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                            logger.logp(Level.FINE, CLASS_NAME, "createServletWrappers"
                                        , "determine whether to add mapping for path[{0}] wrapper[{1}] isEnabled[{2}]"
                                        , new Object[] { path, wrapper, servletConfig.isEnabled() });
                        if (path != null && servletConfig.isEnabled()) {
                            requestMapper.addMapping(path, wrapper);
                        }
                        // End:248871
                    } catch (Exception e) {
                        //TODO: ???? extension processor used to call addMappingTarget after the wrappers had been added.
                        //Now it is done before, and you can get this exception here in the case they call addMappingTarget
                        //and add the mapping to the servletConfig because we'll try to add it again, but it will
                        //already be mapped. You could add a list of paths to ignore and not try to add again. So any
                        //path added via addMappingTarget will be recorded and addMapping can be skipped. It is preferrable
                        //to just have them not call addMappingTarget any more instead of adding the extra check.
                        // pk435011
                        logger.logp(Level.WARNING, CLASS_NAME, "createServletWrappers", "error.while.adding.servlet.mapping.for.path", new Object[] {
                                    path, wrapper, getApplicationName() });
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, CLASS_NAME + ".createServletWrappers", "455", this);
                            // pk435011
                            logger.logp(Level.SEVERE, CLASS_NAME, "createServletWrappers", "error.adding.servlet.mapping.for.servlet", new Object[] {
                                        servletName, getApplicationName(), e }); // PK33511
                        }
                    }
                }
            }

            servletConfig.setServletWrapper(wrapper);

            this.initializeNonDDRepresentableAnnotation(servletConfig);
            // set the servlet wrapper on the
            // servlet config so
            // ServletConfig.addMapping
            // can put it in the
            // requestMapper

        }
    }

    protected IServletWrapper jspAwareCreateServletWrapper(IServletConfig servletConfig, String servletName) {
        
        return jspAwareCreateServletWrapper((WebExtensionProcessor) requestMapper.map("/dummyPath.jsp"),servletConfig,servletName);
    }
    
    private IServletWrapper jspAwareCreateServletWrapper(
                                                         WebExtensionProcessor jspProcessor, IServletConfig servletConfig, String servletName) {
        IServletWrapper wrapper = null;
        if (!servletConfig.isJsp()) {
            try {
                wrapper = getServletWrapper(servletName);
                // getServletWrapper does addWrapper itself so
                // no need to invoke
            } catch (Throwable t) {
                // pk435011
                logger.logp(Level.SEVERE, CLASS_NAME, "jspAwareCreateServletWrapper", "uncaught.init.exception.thrown.by.servlet",
                            new Object[] { servletName, getApplicationName(), t }); // PK33511
                // t.printStackTrace(System.err); @283348D
                // continue;
            }
        } else {
            try {
                // its a JSP in Servlet clothing
                if (jspProcessor != null) {
                    wrapper = jspProcessor.createServletWrapper(servletConfig);
                } else {
                    // pk435011
                    logger.logp(Level.WARNING, CLASS_NAME, "jspAwareCreateServletWrapper", "jsp.processor.not.defined.skipping",
                                servletConfig.getFileName());

                }
            } catch (Throwable t) {
                // pk435011
                logger.logp(Level.SEVERE, CLASS_NAME, "jspAwareCreateServletWrapper", "error.while.initializing.jsp.as.servlet",
                            new Object[] { servletConfig.getFileName(), getApplicationName(), t }); // PK33511
                // t.printStackTrace(System.err); @283348D
            }
        }
        return wrapper;
    }

    /**
     * Process any annotation which could not be managed by an update
     * to the the descriptor based configuration.
     * 
     * @param servletConfig The servlet configuration embedding a class
     *     which is to be processed for annotations.
     */
    private void initializeNonDDRepresentableAnnotation(IServletConfig servletConfig) {
        if (com.ibm.ws.webcontainer.osgi.WebContainer.isServerStopping())
            return;
        
        String methodName = "initializeNonDDRepresentableAnnotation";
        
        String configClassName = servletConfig.getClassName();
        if (configClassName == null) {
            return; // Strange; but impossible to process; ignore.
        }
        
        if (!acceptAnnotationsFrom(configClassName, DO_NOT_ACCEPT_PARTIAL, DO_NOT_ACCEPT_EXCLUDED)) {
            return; // Ignore: In a metadata-complete or excluded region.
        }

        // Process: In a non-metadata-complete, non-excluded region.
        
        Class<?> configClass;
        
        try {
            configClass = Class.forName(configClassName, false, this.getClassLoader());
        } catch (ClassNotFoundException e) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                LoggingUtil.logParamsAndException(logger, Level.FINE, CLASS_NAME, methodName,
                                                  "unable to load class [{0}] which is benign if the class is never used",
                                                  new Object[] { configClassName }, e);
            }
            return;
            
        } catch (NoClassDefFoundError e) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                LoggingUtil.logParamsAndException(logger, Level.FINE, CLASS_NAME, methodName,
                                                  "unable to load class [{0}] which is benign if the class is never used",
                                                  new Object[] { configClassName }, e);
            }
            return;
        }
        
        checkForServletSecurityAnnotation(configClass, servletConfig);
    }

    protected void initializeStaticFileHandler() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.entering(CLASS_NAME, "initializeStaticFileHandler. file serving enabled = " + config.isFileServingEnabled()
                                + ", extensionProcessingDisabled = " + getExtensionProcessingDisabled());
        }
        // String nextPattern = null; // PK18713
        if (config.isFileServingEnabled() && !getExtensionProcessingDisabled()) {
            //check CacheManager
            CacheManager cm = com.ibm.ws.webcontainer.osgi.WebContainer.getCacheManager();
            if (cm !=null && cm.isStaticFileCachingEnabled(contextPath)) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                    logger.logp(Level.FINE, CLASS_NAME,"initializeStaticFileHandler", "Caching is enabled.  Static resources to be handled by SimpleFileServlet");
                }
                try {
                    IServletWrapper staticFileProcessor = getServletWrapper("SimpleFileServlet");
                    addStaticFilePatternMappings(staticFileProcessor);
                }
                catch (Throwable exc)
                {
                    com.ibm.ws.ffdc.FFDCFilter.processException(exc, "com.ibm.ws.webcontainer.webapp.WebAppImpl.initializeStaticFileHandler", "542",
                        this);
                    logger.logp(Level.SEVERE, CLASS_NAME,"initializeStaticFileHandler", "Error.while.adding.static.file.processor",exc);  /*283348.1*/                   
                }
            } else {
                try {
                    addStaticFilePatternMappings(null);
                    // defect 220552: end defer URL mappings to
                    // FileExtensionProcessor instead of hardcoding.
                } catch (Throwable exc) {
                    com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(exc, CLASS_NAME + ".initializeStaticFileHandler", "542", this);
                    // pk435011
                    logger.logp(Level.SEVERE, CLASS_NAME, "initializeStaticFileHandler", "error.while.adding.static.file.processor", exc); /* 283348.1 */
                }
            }
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.exiting(CLASS_NAME, "initializeStaticFileHandler");
        }
    }

    // defect 39851 needs to stop the exception from always being thrown
    @FFDCIgnore(Exception.class)
    protected void addStaticFilePatternMappings(RequestProcessor proxyReqProcessor) {
        String nextPattern;
        ExtensionProcessor fileExtensionProcessor = getDefaultExtensionProcessor(this, getConfiguration().getFileServingAttributes());

        List patternList = fileExtensionProcessor.getPatternList();
        Iterator patternIter = patternList.iterator();
        int globalPatternsCount = 0;
        while (patternIter.hasNext()) {
            nextPattern = (String) patternIter.next(); // PK18713
            try {
                if (proxyReqProcessor == null)
                    requestMapper.addMapping(nextPattern, fileExtensionProcessor); // PK18713
                else
                    requestMapper.addMapping(nextPattern, proxyReqProcessor);
            } catch (Exception e) {
                // Mapping clash. Log error
                // pk435011
                // LIBERTY: Fix for RTC defect 49695 -- The logging level should match the severity of the message.
                if (!!!"/*".equals(nextPattern)) {
                    logger.logp(Level.SEVERE, CLASS_NAME, "initializeStaticFileHandler", "error.adding.servlet.mapping.file.handler", nextPattern);
                } else {
                    globalPatternsCount++;
                }
            }
        }
        
        if (globalPatternsCount > 1) {
            logger.logp(Level.SEVERE, CLASS_NAME, "initializeStaticFileHandler", "error.adding.servlet.mapping.file.handler", "/*");
        }
    }

    private void initializeInvokerProcessor() {
        if (config.isServeServletsByClassnameEnabled()) {
            // PK57136 - STARTS
            try {
                InvokerExtensionProcessor invokerExtensionProcessor = (InvokerExtensionProcessor) getInvokerExtensionProcessor(this);
                List patternList = invokerExtensionProcessor.getPatternList();
                Iterator patternIter = patternList.iterator();
                while (patternIter.hasNext()) {
                    try {
                        requestMapper.addMapping((String) patternIter.next(), invokerExtensionProcessor);
                    } catch (Throwable e) {
                        com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, CLASS_NAME + ".initializeInvokerProcessor", "671", this);
                        // PK67022 remove stack from error message
                        logger.logp(Level.SEVERE, CLASS_NAME, "initializeInvokerProcessor", nls.getString("error.initializing.extension.factories"));
                    }
                }
            } catch (Throwable th) {
                com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th, CLASS_NAME + ".initializeInvokerProcessor", "678", this);
                logger.logp(Level.SEVERE, CLASS_NAME, "initializeInvokerProcessor", nls.getString("error.initializing.extension.factories"), th);
            }
            /*
             * try { requestMapper.addMapping("/servlet/*",
             * getInvokerExtensionProcessor(this)); } catch
             * (WebContainerException wce) { } catch (Exception exc) { //TODO:
             * // Mapping for /servlet/ already exists }
             */
            // PK57136 - ENDS
        }
    }

    /**
     * @param app
     * @return
     */
    protected abstract InvokerExtensionProcessor getInvokerExtensionProcessor(WebApp app);

    protected abstract ExtensionProcessor getDefaultExtensionProcessor(WebApp app, HashMap map);

    public IServletWrapper createServletWrapper(IServletConfig sc) throws Exception {
        return this.webExtensionProcessor.createServletWrapper(sc);
    }

    /**
     * Method getFacade.
     * 
     * @return ServletContext
     */
    public ServletContext getFacade() {
        if (this.facade == null)
            this.facade = new ServletContextFacade(this);

        return this.facade;
    }

    public static String normalize(String path) { //PI05525 , access this from IRequestImpl
        String URI = path;

        int qIndex;
        String qString = "";

        if ((qIndex = URI.indexOf("?")) != -1) {
            qString = URI.substring(qIndex);
            URI = URI.substring(0, qIndex);

        }

        while (true) {
            int index = URI.indexOf("/./");
            if (index < 0)
                break;
            URI = URI.substring(0, index) + URI.substring(index + 2);
        }

        while (true) {
            int index = URI.indexOf("/../");
            if (index < 0)
                break;
            if (index == 0)
                return (null); // Trying to go outside our context
            int index2 = URI.lastIndexOf('/', index - 1);
            URI = URI.substring(0, index2) + URI.substring(index + 3);
        }

        return URI + qString;
    }

    // PK61140 - Starts
    public IServletWrapper getServletWrapper(String servletName) throws Exception {
        return getServletWrapper(servletName, false);
    }

    // PK61140 - Ends
    /**
     * Method getServletWrapper.
     * 
     * @param string
     * @return Object
     */
    public IServletWrapper getServletWrapper(String servletName, boolean addMapping) throws Exception // PK61140
    {
        IServletWrapper targetWrapper = null;

        IServletConfig sconfig = config.getServletInfo(servletName);
        if (sconfig != null) {
            IServletWrapper existingServletWrapper = sconfig.getServletWrapper();
            if (existingServletWrapper != null)
                return existingServletWrapper;
        }

        // Retrieve the list of mappings associated with 'servletName'
        List<String> mappings = config.getServletMappings(servletName);

        if (mappings != null) {
            for (String mapping : mappings) {
                if (mapping.length() > 0 && mapping.charAt(0) != '/' && mapping.charAt(0) != '*')
                    mapping = '/' + mapping;
                RequestProcessor p = requestMapper.map(mapping);
                if (p != null) {
                    if (p instanceof IServletWrapper) {
                        if (((IServletWrapper) p).getServletName().equals(servletName)) {
                            targetWrapper = (IServletWrapper) p;
                            break;
                        }
                    }
                }
            }
        }

        if (targetWrapper != null)
            return targetWrapper;

        // Begin 650884
        // PK61140 - Starts
        // String path = BY_NAME_ONLY + servletName;
        // RequestProcessor p = requestMapper.map(path);
        // // RequestProcessor p = requestMapper.map(BY_NAME_ONLY + servletName);
        //
        // // PK61140 - Ends
        //       
        //
        // if (p != null)
        // if (p instanceof ServletWrapper) {
        // if (((ServletWrapper) p).getServletName().equals(servletName))
        // targetWrapper = (ServletWrapper) p;
        // }
        //
        // if (targetWrapper != null)
        // return targetWrapper;
        // End 650884

        if (sconfig == null) {
            int internalIndex;
            if ((internalIndex = getInternalServletIndex(servletName)) >= 0) {
                sconfig = loadInternalConfig(servletName, internalIndex);
            } else {
                // Not found in DD, and not an Internal Servlet, stray??
                //
                return null;
            }

        }

        // return webExtensionProcessor.createServletWrapper(sconfig); //
        // PK61140
        // PK61140 - Starts
        IServletWrapper sw = webExtensionProcessor.createServletWrapper(sconfig);

        // Begin 650884
        // if ((sw != null)) {
        // if (addMapping) {
        // synchronized (sconfig) {
        // if (!requestMapper.exists(path)) {
        // requestMapper.addMapping(path, sw);
        // }
        // }
        // }
        // }
        // End 650884
        return sw;
        // PK61140 - Ends
    }

    public IServletWrapper getMimeFilterWrapper(String mimeType) throws ServletException {
        IServletWrapper wrapper = null;
        MimeFilter mimeFilter = (MimeFilter) config.getMimeFilters().get(mimeType);
        if (mimeFilter != null) {
            String servletName = mimeFilter.getTarget();
            try {
                wrapper = getServletWrapper(servletName);
            } catch (Exception e) {
                wrapper = null;
            }
        }
        return wrapper;
    }

    protected boolean isSystemApp() {
        return WCCustomProperties.DISABLE_SYSTEM_APP_GLOBAL_LISTENER_LOADING && config.isSystemApp();
    }
    
    protected void registerGlobalWebAppListeners() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.entering(CLASS_NAME, "registerGlobalWebAppListeners");

        try {
            // PK66137
            boolean isSystemApp = isSystemApp();
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                logger.logp(Level.FINE, CLASS_NAME, "registerGlobalWebAppListeners", "systemApp--> " + config.isSystemApp()
                        + " DISABLE_SYSTEM_APP_GLOBAL_LISTENER_LOADING--> " + WCCustomProperties.DISABLE_SYSTEM_APP_GLOBAL_LISTENER_LOADING);
            // End PK66137

            List appListeners = WebContainer.getApplicationListeners(isSystemApp);
            try {
                for (int i = 0; i < appListeners.size(); i++)
                    eventSource.addApplicationListener((ApplicationListener) appListeners.get(i));
            } catch (Throwable th) {
                logError("Failed to add global application listener: " + th);
            }

            List serListeners = WebContainer.getServletListeners(isSystemApp);
            try {
                for (int i = 0; i < serListeners.size(); i++)
                    eventSource.addServletListener((ServletListener) serListeners.get(i));
            } catch (Throwable th) {
                logError("Failed to load global servlet listener: " + th);
            }

            List erListeners = WebContainer.getServletErrorListeners(isSystemApp);
            try {
                for (int i = 0; i < erListeners.size(); i++)
                    eventSource.addServletErrorListener((ServletErrorListener) erListeners.get(i));
            } catch (Throwable th) {
                logError("Failed to load global servlet error listener: " + th);
            }

            List invListeners = WebContainer.getServletInvocationListeners(isSystemApp);
            try {
                for (int i = 0; i < invListeners.size(); i++)
                    eventSource.addServletInvocationListener((ServletInvocationListener) invListeners.get(i));
            } catch (Throwable th) {
                logError("Failed to load global servlet invocation listener: " + th);
            }

            // LIDB-3598: begin
            List finvListeners = WebContainer.getFilterInvocationListeners(isSystemApp);
            try {
                for (int i = 0; i < finvListeners.size(); i++)
                    eventSource.addFilterInvocationListener((FilterInvocationListener) finvListeners.get(i));
            } catch (Throwable th) {
                logError("Failed to load global filter invocation listener: " + th);
            }
            // 292460: begin resolve issues concerning LIDB-3598
            // WASCC.web.webcontainer
            List fListeners = WebContainer.getFilterListeners(isSystemApp);
            try {
                for (int i = 0; i < fListeners.size(); i++)
                    eventSource.addFilterListener((FilterListener) fListeners.get(i));
            } catch (Throwable th) {
                logError("Failed to load global filter listener: " + th);
            }
            List ferrorListeners = WebContainer.getFilterErrorListeners(isSystemApp);
            try {
                for (int i = 0; i < ferrorListeners.size(); i++)
                    eventSource.addFilterErrorListener((FilterErrorListener) ferrorListeners.get(i));
            } catch (Throwable th) {
                logError("Failed to load global filter error listener: " + th);
            }
            // 292460: end resolve issues concerning LIDB-3598
            // WASCC.web.webcontainer

            // LIDB-3598: end

            // CODE REVIEW START
            List scaListeners = WebContainer.getServletContextAttributeListeners(isSystemApp);
            try {
                for (int i = 0; i < scaListeners.size(); i++)
                    servletContextLAttrListeners.add(i, scaListeners.get(i));
            } catch (Throwable th) {
                com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th, CLASS_NAME + ".registerGlobalWebAppListeners", "817", this);
                logError("Failed to load global serfvlet context attribute listener: " + th);
            }

            List scListeners = WebContainer.getServletContextListeners(isSystemApp);
            try {
                for (int i = 0; i < scListeners.size(); i++)
                    servletContextListeners.add(i, scListeners.get(i));
            } catch (Throwable th) {
                com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th, CLASS_NAME + ".registerGlobalWebAppListeners", "827", this);
                logError("Failed to load global serfvlet context listener: " + th);
            }

            List sraListeners = WebContainer.getServletRequestAttributeListeners(isSystemApp);
            try {
                for (int i = 0; i < sraListeners.size(); i++)
                    servletRequestLAttrListeners.add(i, sraListeners.get(i));
            } catch (Throwable th) {
                com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th, CLASS_NAME + ".registerGlobalWebAppListeners", "837", this);
                logError("Failed to load global serfvlet request attribute listener: " + th);
            }

            List srListeners = WebContainer.getServletRequestListeners(isSystemApp);
            try {
                for (int i = 0; i < srListeners.size(); i++)
                    servletRequestListeners.add(i, srListeners.get(i));
            } catch (Throwable th) {
                com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th, CLASS_NAME + ".registerGlobalWebAppListeners", "847", this);
                logError("Failed to load global servlet request listener: " + th);
            }

            List sListeners = WebContainer.getSessionListeners(isSystemApp);
            try {
                for (int i = 0; i < sListeners.size(); i++)
                    sessionListeners.add(i, sListeners.get(i));
            } catch (Throwable th) {
                com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th, CLASS_NAME + ".registerGlobalWebAppListeners", "857", this);
                logError("Failed to load global session listener: " + th);
            }

            List saListeners = WebContainer.getSessionAttributeListeners(isSystemApp);
            try {
                for (int i = 0; i < saListeners.size(); i++)
                    sessionAttrListeners.add(i, saListeners.get(i));
            } catch (Throwable th) {
                com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th, CLASS_NAME + ".registerGlobalWebAppListeners", "867", this);
                logError("Failed to load global session attribute listener: " + th);
            }
            // CODE REVIEW END
        } catch (Throwable th) {
            // pk435001
            logger.logp(Level.SEVERE, CLASS_NAME, "registerGlobalWebAppListeners", "error.processing.global.listeners.for.webapp", new Object[] {
                                                                                                                                                 getApplicationName(), th }); /* 283348.1 */

        }

    }

    // 275172, Remove registerWebAppListeners since appserver.properties is no
    // longer in the core
    // This functionality is still available in the shell.

    public boolean isMimeFilteringEnabled() {
        return this.config.isMimeFilteringEnabled();
    }

    /**
     * Method loadInternalConfig.
     * 
     * @param servletName
     */
    private ServletConfig loadInternalConfig(String servletName, int internalIndex) throws ServletException {
        ServletConfig sconfig = createConfig("InternalServlet_" + servletName, internalIndex);
        sconfig.setServletName(servletName);
        sconfig.setDisplayName(servletName);
        sconfig.setServletContext(this.getFacade());
        sconfig.setIsJsp(false);

        sconfig.setClassName(internalServletList[internalIndex][1]);

        return sconfig;
    }

    protected ServletConfig createConfig(String internalServletName, int internalIndex) throws ServletException
    {
        return (com.ibm.ws.webcontainer.servlet.ServletConfig) webExtensionProcessor.createConfig(internalServletName);
    }

    /**
     * Method isInternalServlet.
     * 
     * @param servletName
     * @return boolean
     */
    public boolean isInternalServlet(String servletName) {
        return getInternalServletIndex(servletName) >= 0;
    }

    private int getInternalServletIndex(String servletName) {
        for (int i = 0; i < this.internalServletList.length; i++) {
            if (internalServletList[i][0].equals(servletName))
                return i;
        }

        return -1;
    }

    /**
     * Method initializeExtensionProcessors.
     */
    protected void initializeExtensionProcessors(List extensionFactories) {
        // TODO: nameSpace preinvoke/postinvoke

        if (extensionFactories == null) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "initializeExtensionProcessors", "No extension processors");
            }
            return;
        } 

        // process the ExtensionFactories
        for (int i = 0; i < extensionFactories.size(); i++) {
            ExtensionFactory fac = (ExtensionFactory) extensionFactories.get(i);
            ExtensionProcessor processor = null;
            
            // Get the global patterns that this factory creates processors for
            List patterns = fac.getPatternList();
           
            // If extension processing is disabled ignore the factories which return a pattern list
            // Do this before calling createExensionPorcessor so we do not incure the overhead of the creation
            // unless needed.
            if (getExtensionProcessingDisabled() && !patterns.isEmpty()) {
                 if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {  
                     logger.logp(Level.FINE, CLASS_NAME, "initializeExtensionProcessors", "Extension factory with patterns ignored : " + fac.getClass());                   
                 }   
                 continue;
            }

            try {
                processor = fac.createExtensionProcessor(this);
                if (processor == null) {
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                        logger.logp(Level.FINE, CLASS_NAME, "initializeExtensionProcessors", "Extension factory has no processor:" + fac.getClass());
                   }

                    // if the factory returns a null processor, it means
                    // that this factory doesn't want to be associated with
                    // this particular webapp.
                    continue;
                }

            } catch (Throwable e) {
                // Extension processor failed to initialize
                com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, CLASS_NAME + ".initializeExtensionFactories", "883", this);
                // e.printStackTrace(System.err); @283348D
                // 435011
                logger.logp(Level.SEVERE, CLASS_NAME, "initializeExtensionProcessors", "extension.processor.failed.to.initialize.in.factory",
                            new Object[] { fac, e }); /* 283348.1 */
                continue;
            }
            
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "initializeExtensionProcessors", "Extension processor class =" + fac.getClass());
            }

            // Get the global patterns that this factory creates processors for
            Iterator it = patterns.iterator();

            StringBuffer mapStr = new StringBuffer(' ');

            while (it.hasNext()) {
                String mapping = (String) it.next();
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "initializeExtensionProcessors", "Add factory mappings =" + mapping);
                }
                try {
                    requestMapper.addMapping(mapping, processor);
                    mapStr.append(mapping);
                    mapStr.append(' ');
                } catch (Exception exc) {
                    // TODO:
                    // processor already exists for specified pattern
                    // pk435011
                    logger.logp(Level.SEVERE, CLASS_NAME, "initializeExtensionProcessors", "request.processor.already.present.for.mapping", mapping);
                }
            }
            
            // Get the additional patterns that the specific extension processor
            // might want to be associated with
        
            patterns = processor.getPatternList();
            
            if (getExtensionProcessingDisabled() &&!patterns.isEmpty()) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {  
                    logger.logp(Level.FINE, CLASS_NAME, "initializeExtensionProcessors", "Extension processor with patterns ignored : " + processor.getClass());                   
                }   
            } else {    
                it = patterns.iterator();

                while (it.hasNext()) {
                    String mapping = (String) it.next();
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                        logger.logp(Level.FINE, CLASS_NAME, "initializeExtensionProcessors", "Add processor mapping =" + mapping);
                    }

                    try {
                        requestMapper.addMapping(mapping, processor);
                    } catch (Exception exc) {
                    // TODO:
                    // processor already exists for specified pattern
                    // pk435011
                    // Alex TODO: This does not seem to me to be a problem, perhaps a timing artifact at
                    // worst.  Will revisit.
            //        logger.logp(Level.SEVERE, CLASS_NAME, "initializeExtensionProcessors", "error.adding.servlet.mapping.for.servlet", new Object[] {
            //                mapping, getApplicationName(), exc });
                    }
                }
            }    
        }

    }

    protected void loadWebAppAttributes() {
        // add ServletContextEventSource as an attribute
        setAttribute(ServletContextEventSource.ATTRIBUTE_NAME, getServletContextEventSource());
        try {
            setAttribute("com.ibm.websphere.servlet.application.classpath", getClasspath());
            setAttribute("com.ibm.websphere.servlet.application.name", config.getDisplayName());
            setAttribute("com.ibm.websphere.servlet.application.host", getServerName());
            setAttribute("com.ibm.websphere.servlet.enterprise.application.name", getApplicationName());
            if (orderedLibPaths != null)
                setAttribute(ServletContext.ORDERED_LIBS, orderedLibPaths);
            if (config.getWelcomeFileList() != null)
                setAttribute(WELCOME_FILE_LIST, config.getWelcomeFileList());

            // SDJ 104265 - allow user to define scratch dir

            Map attrs = config.getJspAttributes();
            Iterator i = attrs.keySet().iterator();
            while (i.hasNext()) {
                String name = (String) i.next();
                if (name.toLowerCase().equals("jspclassloaderlimit")) {
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                        logger.logp(Level.FINE, CLASS_NAME, "loadWebAppAttributes", "JSPClassLoaderLimit: " + attrs.get(name));
                    }
                    setJSPClassLoaderLimit(new Integer((attrs.get(name)).toString()).intValue());
                } else if (name.toLowerCase().equals("jspclassloaderexclusionlist")) {
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                        logger.logp(Level.FINE, CLASS_NAME, "loadWebAppAttributes", "JSPClassLoaderExclusionList: " + attrs.get(name));
                    }
                    this.jspClassLoaderExclusionList = new ArrayList();
                    setJSPClassLoaderExclusionList(attrs.get(name).toString());
                }
                // PK50133 end
                // PK82657 start
                else if (name.toLowerCase().equals("jspclassloaderlimit.trackincludesandforwards")) {
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                        logger.logp(Level.FINE, CLASS_NAME, "loadWebAppAttributes", "JSPClassLoaderLimit.TrackIncludesAndForwards: "
                                + attrs.get(name));
                    }
                    setJSPClassLoaderLimitTrackIF(Boolean.parseBoolean(attrs.get(name).toString()));
                }
                // PK82657 end

            }
            //This isn't the scratchdir parameter. It's the system property used to set the scratchdir option on a server-wide basis.
            //The JSP engine scratchdir parameter takes precedence over the system property. See JSPExtensionFactory for that.
            scratchdir = System.getProperty("com.ibm.websphere.servlet.temp.dir");

            if (scratchdir == null) {
                setAttribute("javax.servlet.context.tempdir", new File(getTempDirectory()));
            } else {
                logger.logp(Level.FINE, CLASS_NAME, "loadWebAppAttributes", "System property com.ibm.websphere.servlet.temp.dir set.");
                setAttribute("javax.servlet.context.tempdir", new File(getTempDirectory(scratchdir, true, true)));
            }
            
        } catch (Exception e) {
            // pk435011
            logger.logp(Level.SEVERE, CLASS_NAME, "loadWebAppAttributes", "error.while.setting.WebAppAttributes", e);
            // e.printStackTrace(System.err); @283348D
        }
    }

    private void clearLifecycleListeners() {
        // clear the current arrays
        servletContextListeners.clear();
        servletContextLAttrListeners.clear();

        // 2.4 Listeners
        servletRequestListeners.clear();
        servletRequestLAttrListeners.clear();

        // cmd PQ81253 session listeners
        sessionListeners.clear();
        sessionAttrListeners.clear();

        sessionActivationListeners.clear();
        sessionBindingListeners.clear();
        
        cdiContexts.clear();
    }

    protected void loadLifecycleListeners() throws Throwable{
        logger.entering(CLASS_NAME, "loadLifecycleListeners");
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME, "loadLifecycleListeners", "stopAppStartupOnListenerException = " + WCCustomProperties.STOP_APP_STARTUP_ON_LISTENER_EXCEPTION);
        
        try {
            // get a list of the defined listeners
            java.util.List listeners = config.getListeners();

            // see if we have any listeners to process
            if (!listeners.isEmpty()) {
                // we do have listeners...process 'em
                Iterator iter = listeners.iterator();

                while (iter.hasNext() && !com.ibm.ws.webcontainer.osgi.WebContainer.isServerStopping()) {
                    // get the listener instance
                    String listenerClass = null;
                    Object curObj = iter.next();
                    listenerClass = getListenerClassName(curObj);

                    if (listenerClass != null) {
                        // determine listener type...first, instantiate it
                        // 596191 Start
                        Object listener = null;
                        try {
                            listener = loadListener(listenerClass);
                        } catch(InjectionException ie){
                            com.ibm.ws.ffdc.FFDCFilter.processException(ie, "com.ibm.ws.webcontainer.webapp.WebApp.loadListener", "672", this);
                            LoggerHelper.logParamsAndException(logger, Level.SEVERE, CLASS_NAME,"loadLifecycleListeners", "Listener.found.but.injection.failure", new Object[]{listenerClass} , ie );
                            if (WCCustomProperties.STOP_APP_STARTUP_ON_LISTENER_EXCEPTION) { //PI58875
                                throw ie;
                            }
                        } // 596191 End

                        if (listener != null) {
                            if (listener instanceof javax.servlet.ServletContextListener) {
                                addServletContextListener((javax.servlet.ServletContextListener) listener);
                            }
                            if (listener instanceof javax.servlet.ServletContextAttributeListener) {
                                // add to the context attr listener list
                                servletContextLAttrListeners.add(listener);
                            }

                            // 2.4 Listeners
                            if (listener instanceof javax.servlet.ServletRequestListener) {
                                // add to the request listener list
                                servletRequestListeners.add(listener);
                            }
                            if (listener instanceof javax.servlet.ServletRequestAttributeListener) {
                                // add to the request attribute list
                                servletRequestLAttrListeners.add(listener);
                            }
                            // cmd PQ81253 BEGIN load session listeners here
                            if (listener instanceof javax.servlet.http.HttpSessionListener) {
                                // add to the session listener list
                                this.sessionCtx.addHttpSessionListener((javax.servlet.http.HttpSessionListener) listener, name);
                                this.sessionListeners.add(listener);
                            }
                            
                            // Servlet 3.1 add the HttpSessionIdListeners
                            checkForSessionIdListenerAndAdd(listener);
                            
                            if (listener instanceof javax.servlet.http.HttpSessionAttributeListener) {
                                // add to the session attribute listener list
                                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                                    logger
                                            .logp(Level.FINE, CLASS_NAME, "addLifecycleListener",
                                                    "listener instanceof javax.servlet.http.HttpSessionAttributeListener");
                                    logger.logp(Level.FINE, CLASS_NAME, "addLifecycleListener", "name : " + name);
                                }
                                this.sessionCtx.addHttpSessionAttributeListener((HttpSessionAttributeListener) listener, name);

                                // 434577
                                // add to a mirror list because we can't get access to the list
                                // the
                                // session context is holding on to later on.

                                this.sessionAttrListeners.add(listener);
                            }
                            // cmd PQ81253 END

                            if (listener instanceof javax.servlet.http.HttpSessionActivationListener) {
                                sessionActivationListeners.add(listener);
                            }

                            if (listener instanceof javax.servlet.http.HttpSessionBindingListener) {
                                sessionBindingListeners.add(listener);
                            }
                        }
                    }
                }
            }
        } catch (Throwable th) {
            // pk435011
            logger.logp(Level.SEVERE, CLASS_NAME, "loadLifecycleListeners", "error.processing.global.listeners.for.webapp", new Object[] { getApplicationName(), th });
			if (WCCustomProperties.STOP_APP_STARTUP_ON_LISTENER_EXCEPTION) { //PI58875
                throw th;
            }
        }
        logger.exiting(CLASS_NAME, "loadLifecycleListeners");
    }

    /**
     * @return
     */
    protected String getListenerClassName(Object curObj) {
        if (curObj instanceof String) {
            return (String) curObj;
        }
        return null;
    }

    // LIDB1234.2 - added method below to load a listener class
    protected Object loadListener(String lClassName) throws InjectionException, Throwable //596191 :: PK97815
    {
        Object listener = null;

        try {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                logger.logp(Level.FINE, CLASS_NAME, "loadListener", "loadListener Classloader " + getClassLoader());
            // PK16542 end
            // instantiate the listener
            listener = java.beans.Beans.instantiate(getClassLoader(), lClassName);
        } catch (Throwable th) {
            // some exception, log error.
            logError("Failed to load listener: " + lClassName, th);
            if (WCCustomProperties.STOP_APP_STARTUP_ON_LISTENER_EXCEPTION) { //PI58875
                throw th;
            }
        }
        return listener;
    }

    public abstract Servlet getSimpleFileServlet();
    
    public abstract Servlet getDirectoryBrowsingServlet();
    
    public abstract boolean getExtensionProcessingDisabled();

    protected abstract void initializeServletContextFacades();

    // LIDB1234.2 - method added below to notify listeners of servlet context
    // creation
    public void notifyServletContextCreated() throws Throwable {

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME, "notifyServletContextCreated", "ENTRY"); //PI26908

        TxCollaboratorConfig txConfig = null;
        final boolean hasListeners = !servletContextListeners.isEmpty();

        try {
            if (hasListeners) {
                webAppNameSpaceCollab.preInvoke(getModuleMetaData().getCollaboratorComponentMetaData());

                txConfig = txCollab.preInvoke(null, this.isServlet23);
                if (txConfig != null)
                    txConfig.setDispatchContext(null);

                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                    logger.logp(Level.FINE, CLASS_NAME, "notifyServletContextCreated", "stopAppStartupOnListenerException = " + WCCustomProperties.STOP_APP_STARTUP_ON_LISTENER_EXCEPTION);

                Iterator i = servletContextListeners.iterator();
                ServletContextEvent sEvent = new ServletContextEvent(this.getFacade());

                // canAddServletContextListener used in sL.contextInitialized
                canAddServletContextListener = false;
                while (i.hasNext()) {
                    // get the listener
                    ServletContextListener sL = (ServletContextListener) i.next();

                    // invoke the listener's context initd method
                    // PK27660 - wrap contextInitialized in try/catch
                    try {
                        Set<String> webXmlDefListeners = this.config.getWebXmlDefinedListeners();
                        if (webXmlDefListeners != null && !webXmlDefListeners.contains(sL.getClass().getName())) {
                            withinContextInitOfProgAddListener = true;
                            lastProgAddListenerInitialized = sL.getClass().getName(); // PI41941
                        }
                        sL.contextInitialized(sEvent);
                    } catch (Throwable th) {
                        com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th, CLASS_NAME + ".notifyServletContextCreated", "1341", this);
                        // pk435011
                        logger.logp(Level.SEVERE, CLASS_NAME, "notifyServletContextCreated", "exception.while.initializing.context", new Object[] { th } );
                        if (withinContextInitOfProgAddListener) {
                            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                                logger.logp(Level.FINE, CLASS_NAME, "notifyServletContextCreated", "rethrowing exception since the scl was programmatically added");
                            throw th;
                        } else if (WCCustomProperties.STOP_APP_STARTUP_ON_LISTENER_EXCEPTION) {
                            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                                logger.logp(Level.FINE, CLASS_NAME, "notifyServletContextCreated", "rethrowing exception due to stopAppStartupOnListenerException");
                            
                            throw th;
                        }

                    } finally {
                        withinContextInitOfProgAddListener = false;
                    }    
                }
            }
            setAttribute("com.ibm.ws.jsp.servletContextListeners.contextInitialized", "true"); // PM05903
        } catch (Throwable e) {
            if (withinContextInitOfProgAddListener || WCCustomProperties.STOP_APP_STARTUP_ON_LISTENER_EXCEPTION)
                throw e;
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, CLASS_NAME + ".notifyServletContextCreated", "1353", this);
            // e.printStackTrace(System.err); @283348D
            // pk435011
            logger.logp(Level.SEVERE, CLASS_NAME, "notifyServletContextCreated", "exception.caught.in.notifyServletContextCreated", new Object[] { e } ); // PK27660
        } finally {
            canAddServletContextListener = true;
            if (hasListeners) {
                try {
                    txCollab.postInvoke(null, txConfig, this.isServlet23);
                } catch (Exception e) {
                    com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, CLASS_NAME + ".notifyServletContextCreated", "1327", this);

                }
                webAppNameSpaceCollab.postInvoke();
            }
        }
    }
    
    /*
     * F743-31926 - refactored this method and added support for the SCI Extension Point.
     * 
     * Initialize all of the ServletContainerINitializers including those 
     * from the SCI Extension Point.
     */
    protected void initializeServletContainerInitializers(DeployedModule moduleConfig) {
        if (WCCustomProperties.DISABLE_SCI_FOR_PRE_V8_APPS && this.getVersionID()<30 ) { //don't do this for 2.5 apps 
            //don't handle SCIs for Servlet 2.5 and lower apps
            logger.logp(Level.FINE, CLASS_NAME, "initializeServletContainerInitializers", "No processing of ServletContainerInitializers on application due to custom property");
            return;
        }
        
        if (com.ibm.ws.webcontainer.osgi.WebContainer.isServerStopping())
            return;

        ServletContext ctx = this;
        List<ServletContainerInitializer> myScis = new ArrayList<ServletContainerInitializer>();
        HashMap<ServletContainerInitializer, Class[]> handleTypesHashMap = new HashMap<ServletContainerInitializer, Class[]>();
        HashMap<ServletContainerInitializer, HashSet<Class<?>>> onStartupHashMap = new HashMap<ServletContainerInitializer, HashSet<Class<?>>>();
        boolean needToScanClassesExtensionPoint = false;
        boolean needToScanClasses = false;
        
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
            logger.entering(CLASS_NAME, "initializeServletContainerInitializers");
            StringBuffer msg=new StringBuffer(";warFile=").append(moduleConfig.getName());
            logger.logp(Level.FINEST, CLASS_NAME, "initializeServletContainerInitializers", msg.toString());
        } 

        // investigate all of the ExtensionPointSCIs and determine if there is a need to scan for classes.
        needToScanClassesExtensionPoint = initializeExtensionPointSCIs(myScis, moduleConfig, handleTypesHashMap, onStartupHashMap);
        
        ServiceLoader<ServletContainerInitializer> scis = ServiceLoader.load(ServletContainerInitializer.class, this.getClassLoader());
            
        //map for classNames and 
        for (ServletContainerInitializer sci:scis) {
            
            // If server is stopping don't process any more sci's
            if (com.ibm.ws.webcontainer.osgi.WebContainer.isServerStopping())
                break;
            
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                logger.logp(Level.FINE, CLASS_NAME, "initializeServletContainerInitializers","Checking " +sci.getClass().getName());
            }
            
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                logger.logp(Level.FINE, CLASS_NAME, "initializeServletContainerInitializers","ServletContainerInitializer " +sci.getClass().getName() + " is valid.");
            }
            determineWhetherToAddScis(sci, myScis);
            //if the ServletContainerInitializer came from a valid jar, check the HandlesTypes annotation
            if(investigateHandlesTypes(sci, handleTypesHashMap, onStartupHashMap)){
                needToScanClasses = true;
            }
        }
        //by now we should have put the SCI's with their appropriate classes in the handleTypesHashMap
        // if we need to scan classes for either regular or ExtesnionPoint SCIs then we need to do so.
        if (needToScanClasses || needToScanClassesExtensionPoint) {
           scanForHandlesTypesClasses(moduleConfig, handleTypesHashMap, onStartupHashMap);
        }
        
        //need to use myScis instead of scis as the instances of the ServletContainerInitializer changes in scis
        for (ServletContainerInitializer sci:myScis) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                logger.logp(Level.FINE, CLASS_NAME, "initializeServletContainerInitializers", "looping through ServletContainerInitializers again");
            }
            HashSet<Class<?>> setOfClasses = onStartupHashMap.get(sci);
            if (setOfClasses!=null && setOfClasses.isEmpty()) {
                setOfClasses=null;
            }
            try {
                sci.onStartup(setOfClasses, ctx);
            } catch (ServletException e) {
                logger.logp(Level.WARNING, CLASS_NAME,"initializeServletContainerInitializers", "exception.occurred.while.running.ServletContainerInitializers.onStartup", new Object[] {sci, e, this.config.getDisplayName()});
            }
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
            logger.exiting(CLASS_NAME, "initializeServletContainerInitializers");
        }
    }
    
    protected abstract void determineWhetherToAddScis(ServletContainerInitializer sci, List<ServletContainerInitializer> myScis);
    
    /*
     * F743-31926
     * 
     * Called from initializeServletContainerInitializers(boolean, boolean,includedJars,warFile)
     * 
     * Grabs all of the SCIs from the ExtensionPoint and investigates each to
     * see if we need to scan for classes.
     * 
     * @return - True if we need to scan for classes, False otherwise.
     */
    private boolean initializeExtensionPointSCIs(List<ServletContainerInitializer> myScis, DeployedModule module, HashMap<ServletContainerInitializer, Class[]> handleTypesHashMap, HashMap<ServletContainerInitializer, HashSet<Class<?>>> onStartupHashMap){
        
         Iterator<ServletContainerInitializer> servletContainerInitializersIterator = com.ibm.ws.webcontainer.osgi.WebContainer.getServletContainerInitializerExtension();//com.ibm.ws.webcontainer.WSWebContainer.getServletContainerInitializerRegistry();
         if (servletContainerInitializersIterator==null) 
             return false;
         boolean needToScanClasses = false;
         while(servletContainerInitializersIterator.hasNext()){
             ServletContainerInitializer sci = servletContainerInitializersIterator.next();
             String className = sci.getClass().getName();
             try{
                 myScis.add(sci);
                 if(investigateHandlesTypes(sci, handleTypesHashMap, onStartupHashMap)){
                     needToScanClasses = true;
                 }
             }catch (Exception e){
                logger.logp(Level.SEVERE, CLASS_NAME,"initializeExtensionPointSCIs", "exception.occured.while.processing.ServletContainerInitializer.initializeExtensionPointSCIs", new Object[] {className});
             }
         }
         
         return needToScanClasses;
    }

    /*
     * F743-31926
     * 
     * Check the ServletContainerInitializer for HandlesTypes.
     * 
     * @return - True if we need to scan for classes, False otherwise.
     */
    private boolean investigateHandlesTypes(ServletContainerInitializer sci, HashMap<ServletContainerInitializer, Class[]> handleTypesHashMap, HashMap<ServletContainerInitializer, HashSet<Class<?>>> onStartupHashMap){
        boolean needToScan = false;
        try {
            HandlesTypes handles = sci.getClass().getAnnotation(HandlesTypes.class);
            //handles is the classes which we are to look for and find all implementing classes.
            if (handles!=null) {
                Class[] classes = handles.value();
                needToScan=true;
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
                    for (Class c:classes) {
                        logger.logp(Level.FINE, CLASS_NAME, "initializeServletContainerInitializers","Handles class to look contains " + c);
                    }
                }
                handleTypesHashMap.put(sci, classes);
                onStartupHashMap.put(sci, new HashSet<Class<?>>());
            }
        } catch (RuntimeException e) {
            //the HandlesTypes class wasn't found in the classloader
            if (WCCustomProperties.LOG_SERVLET_CONTAINER_INITIALIZER_CLASSLOADER_ERRORS) {
                logger.logp(Level.WARNING, CLASS_NAME,"initializeServletContainerInitializers", "exception.occurred.while.initializing.ServletContainerInitializers.HandlesTypes", new Object[] {sci.getClass().getName(), this.config.getDisplayName()});
            } else {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                    logger.logp(Level.FINE, CLASS_NAME,"initializeServletContainerInitializers", "exception.occurred.while.initializing.ServletContainerInitializers.HandlesTypes", new Object[] {sci.getClass().getName(), this.config.getDisplayName()});
                }
            }
        }
        return needToScan;
    }
    
    /*
     * F743-31926
     * 
     * Scan for the classes 
     */
    protected abstract void scanForHandlesTypesClasses(DeployedModule deployedModule, HashMap<ServletContainerInitializer, Class[]> handleTypesHashMap, HashMap<ServletContainerInitializer, HashSet<Class<?>>> onStartupHashMap);
    
    // LIDB1234.2 - method added below to notify listeners of servlet context
    // destruction
    public void notifyServletContextDestroyed() {
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME, "notifyServletContextDestroyed", "ENTRY"); //PI26908
        
        TxCollaboratorConfig txConfig = null;
        try {

            if (webAppNameSpaceCollab != null) {
                webAppNameSpaceCollab.preInvoke(getModuleMetaData().getCollaboratorComponentMetaData());
            }

            if (txCollab != null) {
                txConfig = txCollab.preInvoke(null, this.isServlet23);
            }
            if (txConfig != null)
                txConfig.setDispatchContext(null);
            // need to notify listeners registered in the
            // _servletContextListeners array
            if (!servletContextListeners.isEmpty()) {
                ServletContextEvent sEvent = new ServletContextEvent(this.getFacade());

                // listeners must be notified in reverse order of definition
                for (int listenerIndex = servletContextListeners.size() - 1; listenerIndex > -1; listenerIndex--) {
                    // get the listener
                    ServletContextListener sL = (ServletContextListener) servletContextListeners.get(listenerIndex);

                    // invoke the listener's context destroyed method
                    try {
                        sL.contextDestroyed(sEvent);
                    } catch (Throwable th) {
                        com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th, CLASS_NAME + ".notifyServletContextDestroyed", "1405", this);
                        // pk435011
                        logger.logp(Level.SEVERE, CLASS_NAME, "notifyServletContextDestroyed", "exception.caught.destroying.context", new Object[] { th } );
                    }
                }
            }
        } catch (Exception e) {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, CLASS_NAME + ".notifyServletContextDestroyed", "1417", this);
            // pk435011
            logger.logp(Level.SEVERE, CLASS_NAME, "notifyServletContextDestroyed", "exception.caught.in.notifyServletContextDestroyed", e); // PK27660
        } finally {
            if (txCollab != null) {
                try {
                    txCollab.postInvoke(null, txConfig, this.isServlet23);
                } catch (Exception e) {
                    com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, CLASS_NAME + ".notifyServletContextDestroyed", "1557", this);
                }
            }
            if (webAppNameSpaceCollab != null) {
                webAppNameSpaceCollab.postInvoke();
            }
        }
    }

    // LIDB1234.2 - method added below to notify listeners of servlet context
    // attr creation
    public void notifyServletContextAttrAdded(String name, Object value) {

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME, "notifyServletContextAttrAdded", "ENTRY"); //PI26908

        // need to notify listeners registered in the
        // ServletContextAttributeListener array
        if (!servletContextLAttrListeners.isEmpty()) {
            // We run the risk of getting a concurrent modification
            Iterator i = servletContextLAttrListeners.iterator();
            ServletContextAttributeEvent sEvent = new ServletContextAttributeEvent(this.getFacade(), name, value);

            while (i.hasNext()) {
                // get the listener
                ServletContextAttributeListener sL = (ServletContextAttributeListener) i.next();

                // invoke the listener's attr added method
                sL.attributeAdded(sEvent);
            }
        }

    }

    // LIDB1234.2 - method added below to notify listeners of servlet context
    // attr replacement
    public void notifyServletContextAttrReplaced(String name, Object value) {
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME, "notifyServletContextAttrReplaced", "ENTRY"); //PI26908
        
        // need to notify listeners registered in the
        // ServletContextAttributeListener array
        if (!servletContextLAttrListeners.isEmpty()) {
            Iterator i = servletContextLAttrListeners.iterator();
            ServletContextAttributeEvent sEvent = new ServletContextAttributeEvent(this.getFacade(), name, value);

            while (i.hasNext()) {
                // get the listener
                ServletContextAttributeListener sL = (ServletContextAttributeListener) i.next();

                // invoke the listener's attr added method
                sL.attributeReplaced(sEvent);
            }
        }
    }

    // LIDB1234.2 - method added below to notify listeners of servlet context
    // attr removal
    public void notifyServletContextAttrRemoved(String name, Object value) {
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME, "notifyServletContextAttrRemoved", "ENTRY"); //PI26908

        // need to notify listeners registered in the
        // ServletContextAttributeListener array
        if (!servletContextLAttrListeners.isEmpty()) {
            Iterator i = servletContextLAttrListeners.iterator();
            ServletContextAttributeEvent sEvent = new ServletContextAttributeEvent(this.getFacade(), name, value);

            while (i.hasNext()) {
                // get the listener
                ServletContextAttributeListener sL = (ServletContextAttributeListener) i.next();

                // invoke the listener's attr added method
                sL.attributeRemoved(sEvent);
            }
        }
    }

    // PK91120 Start
    public boolean notifyServletRequestCreated(ServletRequest request)
    {        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME, "notifyServletRequestCreated", "ENTRY"); //PI26908

        boolean servletRequestListenerCreated = false;
        if (!servletRequestListeners.isEmpty())
        {
            WebContainerRequestState reqState = WebContainerRequestState.getInstance(true);
            if (reqState.getAttribute("com.ibm.ws.webcontainer.invokeListenerRequest") == null)
            {
                reqState.setAttribute("com.ibm.ws.webcontainer.invokeListenerRequest", false);

                Iterator i = servletRequestListeners.iterator();
                ServletRequestEvent sEvent = new ServletRequestEvent(this.getFacade(), request);

                while (i.hasNext())
                {
                    // get the listener
                    ServletRequestListener sL = (ServletRequestListener) i.next();

                    // invoke the listener's request initd method
                    sL.requestInitialized(sEvent);
                }
                servletRequestListenerCreated = true;
            }       
            else{
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                    logger.logp(Level.FINE, CLASS_NAME,"notifyServletRequestCreated", 
                            " ServletListener already invoked for request, reqState --> "+ reqState);                               
            }
        }
        return servletRequestListenerCreated;
    }

    // PK91120 End

    public void notifyServletRequestDestroyed(ServletRequest request) {
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME, "notifyServletRequestDestroyed", "ENTRY"); //PI26908

        if (!servletRequestListeners.isEmpty()) {
            ServletRequestEvent sEvent = new ServletRequestEvent(this.getFacade(), request);

            // listeners must be notified in reverse order of definition
            for (int listenerIndex = servletRequestListeners.size() - 1; listenerIndex > -1; listenerIndex--) {
                // get the listener
                ServletRequestListener sL = (ServletRequestListener) servletRequestListeners.get(listenerIndex);

                // invoke the listener's request destroyed method
                sL.requestDestroyed(sEvent);
            }
        }
    }

    public void notifyServletRequestAttrAdded(ServletRequest request, String name, Object value) {
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME, "notifyServletRequestAttrAdded", "ENTRY"); //PI26908

        // need to notify listeners registered in the
        // ServletRequestAttributeListener array
        if (!servletRequestLAttrListeners.isEmpty()) {
            Iterator i = servletRequestLAttrListeners.iterator();
            ServletRequestAttributeEvent sEvent = new ServletRequestAttributeEvent(this.getFacade(), request, name, value);

            while (i.hasNext()) {
                // get the listener
                ServletRequestAttributeListener sL = (ServletRequestAttributeListener) i.next();

                // invoke the listener's attr added method
                sL.attributeAdded(sEvent);
            }
        }
    }

    public void notifyServletRequestAttrReplaced(ServletRequest request, String name, Object value) {
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME, "notifyServletRequestAttrReplaced", "ENTRY"); //PI26908

        // need to notify listeners registered in the
        // ServletRequestAttributeListener array
        if (!servletRequestLAttrListeners.isEmpty()) {
            Iterator i = servletRequestLAttrListeners.iterator();
            ServletRequestAttributeEvent sEvent = new ServletRequestAttributeEvent(this.getFacade(), request, name, value);

            while (i.hasNext()) {
                // get the listener
                ServletRequestAttributeListener sL = (ServletRequestAttributeListener) i.next();

                // invoke the listener's attr added method
                sL.attributeReplaced(sEvent);
            }
        }
    }

    public void notifyServletRequestAttrRemoved(ServletRequest request, String name, Object value) {
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME, "notifyServletRequestAttrRemoved", "ENTRY"); //PI26908

        // need to notify listeners registered in the
        // ServletRequestAttributeListener array
        if (!servletRequestLAttrListeners.isEmpty()) {
            Iterator i = servletRequestLAttrListeners.iterator();
            ServletRequestAttributeEvent sEvent = new ServletRequestAttributeEvent(this.getFacade(), request, name, value);

            while (i.hasNext()) {
                // get the listener
                ServletRequestAttributeListener sL = (ServletRequestAttributeListener) i.next();

                // invoke the listener's attr added method
                sL.attributeRemoved(sEvent);
            }
        }
    }

    // LIDB441.9.2
    // This method can only be called after loadLifecycleListeners which clears
    // the listener arrays.
    // It has been added with the intention of being called from
    // WebAppInitializationCollaborator impls
    public void addLifecycleListener(java.util.EventListener listener) {
        String listenerClassName = getClassName(listener);
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.entering(CLASS_NAME, "addLifecycleListener : listenerClass = " + listenerClassName);     
        }       

        if (listener != null) {
            if (listener instanceof javax.servlet.ServletContextListener) {
                addServletContextListener(listener);
            }
            if (listener instanceof javax.servlet.ServletContextAttributeListener) {
                // add to the context attr listener list
                servletContextLAttrListeners.add(listener);
            }

            if (listener instanceof javax.servlet.http.HttpSessionListener) {
                // add to the session listener list
                this.sessionCtx.addHttpSessionListener((javax.servlet.http.HttpSessionListener) listener, name);

                // 434577
                // add to a mirror list because we can't get access to the list
                // the
                // session context is holding on to later on.

                this.sessionListeners.add(listener);
                this.addedSessionListeners.add(listener);
            }
            
            // Servlet 3.1
            checkForSessionIdListenerAndAdd(listener);

            // \PK34418 begins
            if (listener instanceof javax.servlet.http.HttpSessionAttributeListener) {
                // add to the session attribute listener list
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                    logger
                            .logp(Level.FINE, CLASS_NAME, "addLifecycleListener",
                                    "listener instanceof javax.servlet.http.HttpSessionAttributeListener");
                    logger.logp(Level.FINE, CLASS_NAME, "addLifecycleListener", "name : " + name);
                }
                this.sessionCtx.addHttpSessionAttributeListener((HttpSessionAttributeListener) listener, name);

                // 434577
                // add to a mirror list because we can't get access to the list
                // the
                // session context is holding on to later on.

                this.sessionAttrListeners.add(listener);
                this.addedSessionAttrListeners.add(listener);

            }// PK34418

            // 2.4 Listeners
            if (listener instanceof javax.servlet.ServletRequestListener) {
                servletRequestListeners.add(listener);
            }
            if (listener instanceof javax.servlet.ServletRequestAttributeListener) {
                servletRequestLAttrListeners.add(listener);
            }

            if (listener instanceof javax.servlet.http.HttpSessionActivationListener) {
                sessionActivationListeners.add(listener);
            }

            if (listener instanceof javax.servlet.http.HttpSessionBindingListener) {
                sessionBindingListeners.add(listener);
            }
        }
        logger.exiting(CLASS_NAME, "addLifecycleListener");
    }

    private String getClassName(Object object) {
        return object == null ? null : object.getClass().getName();
    }

    private void addServletContextListener(java.util.EventListener listener) {
        String listenerClassName = getClassName(listener);
        boolean addFirst = false;
        //if using WAS shipped JSF jar, register JSF listener first
        //for performance reasons, treating non JSF apps as highest priority (will fail out of first check)
        Object jsf_impl_enabled_param = this.getAttribute(JSF_IMPL_ENABLED_PARAM);
        if (!JSF_IMPL_ENABLED_NONE.equals(jsf_impl_enabled_param) && !JSF_IMPL_ENABLED_CUSTOM.equals(jsf_impl_enabled_param)) {
            if (SUN_CONFIGURE_LISTENER_CLASSNAME.equals(listenerClassName) ||
                MYFACES_LIFECYCLE_LISTENER_CLASSNAME.equals(listenerClassName)) {
                addFirst = true;
            }
        }
        if (addFirst) {
            servletContextListeners.add(0, listener);
        } else {
            servletContextListeners.add(listener);
        }
    }

    /**
     * Method getServletContextEventSource.
     * 
     * @return Object
     */
    public ServletContextEventSource getServletContextEventSource() {
        return this.eventSource;
    }

    /**
     * @see com.ibm.ws.webcontainer.webapp.WebAppContext#getClassLoader()
     */
    public ClassLoader getClassLoader() {
        return this.loader;
    }
    
    /**
     * Internal use method which can be called when withinContextInitOfProgAddListener
     * is set and Servlet 3.1 is enabled - the servlet 3.1 version of getClassLoader
     * throws an unSupportedOperationException when withinContextInitOfProgAddListener
     * is true. This is needed by startEnvSetUp which can be called independantly of
     * a servletContextListener and from a different thread, for example by 
     * session inavlidation. 
     */
    public ClassLoader getClassLoaderInternal() {
        return this.loader;
    }

    /**
     * @see com.ibm.ws.webcontainer.webapp.WebAppContext#getDocumentRoot()
     */
    public String getDocumentRoot() {
        return this.documentRoot;
    }

    /**
     * @see com.ibm.ws.webcontainer.webapp.WebAppContext#getClasspath()
     */
    public String getClasspath() {
        return this.loader.toString();
    }

    /**
     * Method getNodeName.
     * 
     * @return int
     */
    public abstract String getNodeName();

    public abstract String getServerName();

    // begin PK31450
    public String getTempDirectory() {
        return getTempDirectory(true);
    }

    public String getCommonTempDirectory() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
            logger.entering(CLASS_NAME, "getCommonTempDirectory");
        }
        String tempDir = null;
        if (scratchdir == null) {
            tempDir = getTempDirectory(false);
        } else {
            tempDir = getTempDirectory(scratchdir, true, false);
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
            logger.exiting(CLASS_NAME, "getCommonTempDirectory");
        }
        return tempDir;
    }

    public String getTempDirectory(boolean checkZOSFlag) {
        // LIBERTY specific code
        String sr = com.ibm.ws.webcontainer.osgi.WebContainer.getTempDirectory();
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) /* MD15600 */
            logger.logp(Level.FINE, CLASS_NAME, "getTempDirectory", "Using.[{0}].as.server.root", sr); /* MD15600 */
        if (sr == null) { /* MD15600 */
            return sr; /* MD15600 */
        } /* MD15600 */

        return getTempDirectory(sr, false, checkZOSFlag);
    }

    public String getTempDirectory(String dirRoot, boolean override, boolean checkZOSFlag) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
            logger.logp(Level.FINEST, CLASS_NAME, "getTempDirectory", "dirRoot-->" + dirRoot + " override --> " + override + " checkZOSFlag --> "
                    + checkZOSFlag);
        }

        StringBuilder dir = new StringBuilder(dirRoot);

        if (!(dir.charAt(dir.length() - 1) == java.io.File.separatorChar)) {
            dir.append(java.io.File.separator);
        }

        // begin 247392, part 2
        if (checkZOSFlag && !WebContainer.isDefaultTempDir()) {
            // END PK31450
            // Begin 257796, part 1
            dir.append(getNodeName()).append(java.io.File.separator).append(getServerName().replace(' ', '_')).append(
                    "_" + WebContainer.getWebContainer().getPlatformHelper().getServerID());
            if (WebContainer.getTempDir() == null) {
                WebContainer.setTempDir(dir.toString());
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                    logger.logp(Level.FINE, CLASS_NAME, "getTempDirectory", "ZOS temp dir is:" + WebContainer.getTempDir());
            }
            dir.append(java.io.File.separator).append(getApplicationName().replace(' ', '_')).append(java.io.File.separator).append(
                    config.getModuleName().replace(' ', '_'));
            // End 257796, part 1
        } else
            dir.append(getTempDirChildren());

        // defect 112137 begin - don't replace spaces with underscores
        // java.io.File tmpDir = new java.io.File(dir.toString().replace(' ',
        // '_'));
        java.io.File tmpDir = new java.io.File(dir.toString());
        // defect 112137 end

        if (!tmpDir.exists()) {
            // 117050 OS/400 support for servers running under two different
            // profile
            
            // PI09896 removing chown call causing
            // errors in IBM i series
            
            boolean success = tmpDir.mkdirs();
            
            // Since 150896: Background servlet initialization 
            // Seperate web applications can be installed concurrently 
            // In the case of WABs, the same bundle can be installed at
            // two different locations. In this case the web container 
            // was trying to create the same temp directory on
            // two different threads at the same time. 
          
            if ((success == false) && (!tmpDir.exists())) {
                // pk435011
                logger.logp(Level.SEVERE, CLASS_NAME, "getTempDirectory", "failed.to.create.temp.directory", tmpDir.toString());
            }
        }

        if (tmpDir.canRead() == false || tmpDir.canWrite() == false) {
            if (override) {
                // pk435011
                logger.logp(Level.SEVERE, CLASS_NAME, "getTempDirectory", "unable.to.use.specified.temp.directory", new Object[] { tmpDir.toString(),
                        tmpDir.canRead(), tmpDir.canWrite() });
            } else {
                // pk435011
                logger.logp(Level.SEVERE, CLASS_NAME, "getTempDirectory", "unable.to.use.default.temp.directory", new Object[] { tmpDir.toString(),
                        tmpDir.canRead(), tmpDir.canWrite() });
            }
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "getTempDirectory", "directory --> " + tmpDir.getAbsolutePath());
        }
        return tmpDir.getAbsolutePath();
    }

    public String getTempDirChildren() {
        StringBuilder dir = new StringBuilder();

        // SDJ D99077 - use Uri of web module in constructing temp dir, not the
        // web module's display name
        // defect 113620 - replace spaces with underscores starting with
        // servername
        dir.append(getNodeName()).append(java.io.File.separator).append(getServerName().replace(' ', '_')).append(java.io.File.separator).append(
                getApplicationName().replace(' ', '_')).append(java.io.File.separator).append(config.getModuleName().replace(' ', '_'));

        return dir.toString();
    }

    public static boolean isDisableServletAuditLogging() {
        // 89638
        if (disableServletAuditLogging == -1) {
            String skipAudit = AccessController.doPrivileged(new PrivilegedAction<String>(){

                @Override
                public String run() {
                    return System.getProperty("com.ibm.servlet.engine.disableServletAuditLogging");
                }
            });

            if (skipAudit != null && skipAudit.toLowerCase().equals("true")) {
                disableServletAuditLogging = 1;
                // System.out.println(new java.util.Date() +
                // " [Servlet.Message]-[Servlet Logging to the Audit Facility has been disabled.]");
            } else {
                disableServletAuditLogging = 0;
            }
        }

        return disableServletAuditLogging == 1 ? true : false;
    }

    /**
     * @see com.ibm.ws.webcontainer.webapp.WebAppContext#getContextPath()
     */
    public String getContextPath() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME, "getContextPath", "contextPath->" + contextPath);

        return this.contextPath;
    }

    /**
     * @see com.ibm.ws.webcontainer.webapp.WebAppContext#logServletMessage(String,
     *      String)
     */
    public void logServletMessage(String servletName, String message) {
        Object[] args = { servletName, message };

        if (isDisableServletAuditLogging())
            logger.logp(Level.FINE, CLASS_NAME, "logServletMessage", "[Servlet Message]-[{0}]:.{1}", args);
        else
            logger.logp(Level.INFO, CLASS_NAME, "logServletMessage", "log.servlet.message", args);
    }

    /**
     * @see com.ibm.ws.webcontainer.webapp.WebAppContext#logServletError(String,
     *      String, Throwable)
     */
    public void logServletError(String servletName, String message, Throwable th) {
        ServletException e = null;

        if (th instanceof ServletException) {
            e = (ServletException) th;

            while (e != null) {
                th = e.getRootCause();

                if (th == null) {
                    th = e;
                    break;
                }
                e = th instanceof ServletException ? (ServletException) th : null;
            }
        }

        // log the error
        if (message.equals(""))
            // pk435011
            logger.logp(Level.SEVERE, CLASS_NAME, "logServletError", "log.servlet.error", new Object[] { servletName, th });
        else
            // pk435011
            logger.logp(Level.SEVERE, CLASS_NAME, "logServletError", "log.servlet.error.and.message", new Object[] { servletName, message, th });

        // 105840 - end

    }

    /**
     * @see com.ibm.ws.webcontainer.webapp.WebAppContext#logServletError(String,
     *      String)
     */
    public void logServletError(String servletName, String message) {
        Object[] args = { servletName, message };
        // pk435011
        logger.logp(Level.SEVERE, CLASS_NAME, "logServletError", "log.servlet.error", args);

    }

    /**
     * @see com.ibm.ws.webcontainer.webapp.WebAppContext#logError(String)
     */
    public void logError(String message) {
        // pk435011
        logger.logp(Level.SEVERE, CLASS_NAME, "logError", "Error.reported.{0}", message);
    }

    public void logMessage(String message, Throwable th, String method, Level level) {
        ServletException e = null;

        if (th instanceof ServletException) {
            e = (ServletException) th;

            while (e != null) {
                th = e.getRootCause();

                if (th == null) {
                    th = e;
                    break;
                }
                e = th instanceof ServletException ? (ServletException) th : null;
            }
        }

        // log the error
        Object[] args = { message, th };
        // pk435011
        logger.logp(level, CLASS_NAME, method, "log.servlet.error", args);
    }
    
    public void logTrace(String message, Throwable th) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logMessage(message, th, "logTrace", Level.FINE);
        }
    }
    
    /**
     * @see com.ibm.ws.webcontainer.webapp.WebAppContext#logError(String,
     *      Throwable)
     */
    public void logError(String message, Throwable th) {
        logMessage(message, th, "logError", Level.SEVERE);
    }

    /**
     * @see com.ibm.ws.webcontainer.webapp.WebAppContext#getServletContext(String)
     */
    public ServletContext getServletContext(String path) {
        return ((WebApp) ((WebGroup) parent).findContext(path)).getFacade();
    }

    /**
     * @see com.ibm.ws.webcontainer.webapp.WebAppContext#getResourceAsStream(String)
     */
    public InputStream getResourceAsStream(String path) {
        
        try {
            if (container != null){
                Entry entry = findResourceInModule(path);
                if(entry != null){
                    return entry.adapt(InputStream.class);
                }
                return null;
            }
        
            URL url = getResource(path);
            if (url == null)
                return null;
            URLConnection conn = url.openConnection();
            return conn.getInputStream();
        } catch (MalformedURLException e) {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, CLASS_NAME + ".getResourceAsStream", "602", this);
            return null;
        } catch (IOException e) {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, CLASS_NAME + ".getResourceAsStream", "606", this);
            return null;
        } catch (UnableToAdaptException e) {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, CLASS_NAME + ".getResourceAsStream", "606", this);
            return null;
        }
    }

    /**
     * @see com.ibm.ws.webcontainer.webapp.WebAppContext#getResource(String)
     */
    public URL getResource(String p) throws MalformedURLException {
        String rPath = null;
        URL returnURL = null;

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "getResource", "resource --> " + p);
        }
        
        /*
         * The spec states the resource must start with a / so if one isn't
         * there we prepend one.
         */
        // Begin 263020
        if (p.charAt(0) != '/' && p.charAt(0) != '\\') {
            if (prependSlashToResource) {
                logger.logp(Level.WARNING, CLASS_NAME, "getResource", "resource.path.has.to.start.with.slash");
                rPath = "/" + p;
            } else {
                throw new MalformedURLException(nls.getString("resource.path.has.to.start.with.slash"));
            }
        } else {
            rPath = p;
        }
        // End 263020

        String uri = getRealPath(rPath);
        if (uri == null) {
            return null;
        }

        java.io.File checkFile = new java.io.File(uri);
        if (!checkFile.exists()) {
                
            // If we are going to look in a dox root we need to decide whether to look in the jsttaic file doc roots
            // or the jsp doc roots. If the request resource would be processed by a JSP exntenion processor look in 
            // the jsp doc roots otherwise look in that statis doc roots 
            boolean useJSPRoot=false;
            if (staticDocRoot.hasDocRoot() || jspDocRoot.hasDocRoot()) {
                
                RequestProcessor requestProcessor = requestMapper.map(uri);
                
                if (requestProcessor!=null) {
                                                
                        try {
                            
                                Class jspProcessorClass = Class.forName("com.ibm.ws.jsp.webcontainerext.AbstractJSPExtensionProcessor");                            
                                useJSPRoot = jspProcessorClass.isInstance(requestProcessor);
                                
                                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                                        logger.logp(Level.FINE, CLASS_NAME, "getResource", "useJSPRoot = " + useJSPRoot + ", request Processor is " + requestProcessor.getClass().getName());
                        }
                            
                        } catch (ClassNotFoundException cnfe) {
                                
                                useJSPRoot=false;
                                
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                            logger.logp(Level.FINE, CLASS_NAME, "getResource", "useJSPRoot = " + useJSPRoot + ", ClassNotFoundException.");
                        }
                        }
                }    
            }
            
            if (useJSPRoot) {
                    returnURL = jspDocRoot.getURL(rPath,metaInfCache);
            } else {
                returnURL = staticDocRoot.getURL(rPath,metaInfCache);
            }
        } else {
                returnURL = checkFile.toURL();                  
        } 
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.exiting(CLASS_NAME, "getResource", "URL --> " + (returnURL == null ? "null." : returnURL.toString()));
        }

        return returnURL;
        
    }

    /**
     * @see com.ibm.ws.webcontainer.webapp.WebAppContext#getRealPath(String)
     */
    public String getRealPath(String uri) {
        if (uri == null) {
            uri = "/";
        } else if (uri.equals("/")) {
            return getDocumentRoot();
        }
        // SDJ 106155 2001/06/25 begin
        else if (!uri.startsWith("/") && !uri.startsWith("\\")) {
            uri = "/" + uri;
        }
        // uri = uri.replace('/', java.io.File.separatorChar);
        return ((getDocumentRoot() + uri).replace('/', java.io.File.separatorChar));
        // SDJ 106155 2001/06/25 end

    }

    /**
     * @see com.ibm.ws.webcontainer.webapp.WebAppContext#log(String)
     */
    public void log(String msg) {
        if (isDisableServletAuditLogging()) {
            // System.out.println(new java.util.Date() + " [" + getName() + "]["
            // + this.getContextPath() + "][Servlet.LOG]-[" + msg + "]:.");
        } else {
            logger.logp(Level.INFO, CLASS_NAME, "log", "log.servlet.message", new Object[] { getName(), msg });
        }
    }

    /**
     * @see com.ibm.ws.webcontainer.webapp.WebAppContext#log(String, Throwable)
     */
    public void log(String message, Throwable th) {
        Object[] args = { getName(), this.getContextPath(), message, th };
        // 89638
        if (isDisableServletAuditLogging()) {
            // System.out.println(new java.util.Date() + " [" + getName() + "]["
            // + this.getContextPath() + "][Servlet.LOG]-[" + message + "]:." +
            // th);
        } else {
            logger.logp(Level.INFO, CLASS_NAME, "log", "log.servlet.message.with.throwable", args);
        }
    }

    /**
     * @see com.ibm.ws.webcontainer.webapp.WebAppContext#getMimeType(String)
     */
    public String getMimeType(String file) {
        // PK76142 Start
        if (file == null || file.length() == 0 || !file.contains(".")) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "getMimeType", "returning null: " +file);
            }
            return null;
        }
        // PK76142 End

        int pathElementIndex = file.indexOf(';');
        int dot = -1;
        String extWithDot;
        if (pathElementIndex == -1) {
            dot = file.lastIndexOf('.');
            if (dot == -1)
                dot = 0;
            extWithDot = file.substring(dot);
        } else {
            dot = file.lastIndexOf('.', pathElementIndex);
            if (dot == -1)
                dot = 0;
            extWithDot = file.substring(dot, pathElementIndex);
        }

        String extWithoutDot = extWithDot.substring(1);

        String type = config.getMimeType(extWithoutDot);

        if (type == null)
            type = config.getMimeType(extWithDot);

        if (type != null)
            return type;
        return ((WebGroup) parent).getMimeType(extWithDot, extWithoutDot);
    }

    /**
     * @see com.ibm.ws.webcontainer.webapp.WebAppContext#getConfiguration()
     */
    public WebAppConfiguration getConfiguration() {
        return this.config;
    }

    public Set getResourcePaths(String path) {
        return getResourcePaths(path,true);
    }
    
    /**
     * @see com.ibm.ws.webcontainer.webapp.WebAppContext#getResourcePaths(String)
     */
    public Set getResourcePaths(String path, boolean searchMetaInf) {
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.entering(CLASS_NAME, "getResourcePaths", "path->[" + path + "] searchMetaInf = " + searchMetaInf);

        if ((WCCustomProperties.SKIP_META_INF_RESOURCES_PROCESSING == true) && (searchMetaInf == true)) {
            // override passed in parm with custom property behavior
          
            searchMetaInf = false;
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "getResourcePaths", " override searchMetaInf to false due to custom property");
            }    
        }
        
        HashSet set = new HashSet();

        // get the root path
        java.io.File root = new java.io.File(getDocumentRoot() + path);

        if (root.exists()) {
            // list the files in the root
            java.io.File[] fileList = root.listFiles();

            if (fileList != null) {
                for (int i = 0; i < fileList.length; i++) {
                    String resourcePath = fileList[i].getPath();
                    resourcePath = resourcePath.substring(getDocumentRoot().length());
                    resourcePath = resourcePath.replace('\\', '/');
                    if (fileList[i].isDirectory()) {
                        if (resourcePath.endsWith("/") == false) {
                            resourcePath += "/";
                        }
                    }

                    set.add(resourcePath);
                }
            }
        }

        // search the static doc roots and include a search of meta-inf resources if boolean parameter is true
        set.addAll(staticDocRoot.getResourcePaths(path,searchMetaInf));
        
        // look at the JSP doc roots but don't search meta-inf resources this time (meta-inf resources are common
        // to both doc roots).
        set.addAll(jspDocRoot.getResourcePaths(path,false));
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.exiting(CLASS_NAME, "getResourcePaths", "size of set = " + set.size());

        return (set);
    }

    /**
     * @see com.ibm.ws.webcontainer.webapp.WebAppContext#getServletContextName()
     */
    public String getServletContextName() {
        return this.config.getDisplayName();
    }

    /**
     * @see com.ibm.ws.webcontainer.webapp.WebAppContext#getApplicationName()
     */
    public String getApplicationName() {
        return this.applicationName;
    }

    /**
     * @see com.ibm.ws.webcontainer.webapp.WebAppContext#getSessionContext()
     */
    public IHttpSessionContext getSessionContext() {
        return this.sessionCtx;
    }

    /**
     * @see javax.servlet.ServletContext#getAttribute(String)
     */
    public Object getAttribute(String name) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
        {
            logger.logp(Level.FINE, CLASS_NAME, "getAttribute", "name->" + name);
        }

        return this.attributes.get(name);
    }

    /**
     * @see javax.servlet.ServletContext#getAttributeNames()
     */
    public Enumeration getAttributeNames() {
        // return new
        // IteratorEnumerator(((HashMap)(((HashMap)attributes).clone())).keySet().iterator());
        // PK27027 - We have to create a new HashMap since the attributes
        // HashMap is synchronized and it cannot be cloned.
        HashMap tmpAttributes = new HashMap(attributes.size());
        tmpAttributes.putAll(attributes);

        return new IteratorEnumerator(tmpAttributes.keySet().iterator());
    }

    /**
     * @see javax.servlet.ServletContext#getContext(String)
     */
    public ServletContext getContext(String path) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME, "getContext", "path->[" + path + "]");
        // Begin 377689.1 revert back to best match getContext
        WebApp s = (WebApp) ((WebGroup) parent).findContext(path);
        if (s != null) {
            return s.getFacade();
        }
        return null;
    }

    /**
     * @see javax.servlet.ServletContext#getInitParameter(String)
     */
    public String getInitParameter(String name) {
        String value = (String) this.config.getContextParams().get(name);
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE) == true)
        {
            logger.logp(Level.FINE, CLASS_NAME, "getInitParameter", "name->" + name + "value->" + value);
        }
        return value;
    }

    /**
     * @see javax.servlet.ServletContext#getInitParameterNames()
     */
    public Enumeration getInitParameterNames() {
        return new IteratorEnumerator(((HashMap) (config.getContextParams().clone())).keySet().iterator());
    }

    /**
     * @see javax.servlet.ServletContext#getMajorVersion()
     */
    public int getMajorVersion() {
        return 3;
    }

    /**
     * @see javax.servlet.ServletContext#getMinorVersion()
     */
    public int getMinorVersion() {
        return 0; // 398349
    }

    public abstract WebAppDispatcherContext createDispatchContext();

    /**
     * @see javax.servlet.ServletContext#getNamedDispatcher(String)
     */
    public RequestDispatcher getNamedDispatcher(String name) {
        IServletWrapper w;
        try {
            // w = getServletWrapper(name); // PK61140
            w = getServletWrapper(name, true); // PK61140
        } catch (Exception wce) {
            w = null;
        }

        if (w == null)
            return null;

        // begin PK07351 6021Request dispatcher could not be reused as it was in
        // V5. WAS.webcontainer
        // WebAppDispatcherContext dispatchContext = createDispatchContext();
        // RequestDispatcher ward = new WebAppRequestDispatcher(this, w,
        // dispatchContext);
        RequestDispatcher ward = getRequestDispatcher(this, w);
        // end PK07351 6021Request dispatcher could not be reused as it was in
        // V5. WAS.webcontainer

        return ward;
    }
    
    /**
     *  Method added for override by WebApp40 
     */  
    protected RequestDispatcher getRequestDispatcher(WebApp webApp, RequestProcessor p) {
        return new WebAppRequestDispatcher(webApp, p);
    }


    public WebModuleMetaData getModuleMetaData() {
        return this.config.getMetaData();
    }

    /**
     * @see javax.servlet.ServletContext#getRequestDispatcher(String)
     */
    public RequestDispatcher getRequestDispatcher(String path) {
        if (path != null) {
            if (!path.startsWith("/"))
                path = "/" + path;

            String uri = WebGroup.stripURL(path);

            if (!initialized) {
                try {
                    this.initialize();
                }
                catch (Throwable th) {
                    this.failed();
                    this.destroy();
                    //this = null;     // PK40127
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()) {
                        logger.logp(Level.SEVERE, CLASS_NAME, "getRequestDispatcher", "Failed.to.initialize.webapp.{0}", this.getName());
                    }
                    return null;
                }
            }
            
            RequestProcessor p = requestMapper.map(uri);

            if (p == null)
                return null;

            // begin PK07351 6021Request dispatcher could not be reused as it
            // was in V5. WAS.webcontainer
            /*
             * WebAppDispatcherContext dispatchContext =
             * createDispatchContext();
             * dispatchContext.setRequestURI((contextPath.equals("/")) ? path :
             * this.contextPath + path);
             * 
             * int qMark = path.indexOf('?'); if (qMark != -1) path =
             * path.substring(0, qMark);
             * 
             * return new WebAppRequestDispatcher(this, path, dispatchContext);
             */
            return getRequestDispatcher(this, path);
            // end PK07351 6021Request dispatcher could not be reused as it was
            // in V5. WAS.webcontainer

        } else
            return null;
    }

    /**
     *  Method added for override by WebApp40 
     */  
    protected RequestDispatcher getRequestDispatcher(WebApp app, String path) {
        return new WebAppRequestDispatcher(app, path);
    }
    
    /**
     * @see javax.servlet.ServletContext#getServlet(String)
     * @deprecated
     */
    public Servlet getServlet(String arg0) throws ServletException {
        // as per spec return null
        return null;
    }

    /**
     * @see javax.servlet.ServletContext#getServletNames()
     * @deprecated
     */
    public Enumeration getServletNames() {
        return EmptyEnumeration.instance();
    }

    /**
     * @see javax.servlet.ServletContext#getServlets()
     * @deprecated
     */
    public Enumeration getServlets() {
        return EmptyEnumeration.instance();
    }

    /**
     * @see javax.servlet.ServletContext#log(Exception, String)
     * @deprecated
     */
    public void log(Exception th, String message) {
        ServletException e = null;

        if (th instanceof ServletException) {
            e = (ServletException) th;

            while (e != null) {
                th = (Exception) e.getRootCause();

                if (th == null) {
                    th = e;
                    break;
                } else
                    e = th instanceof ServletException ? (ServletException) th : null;
            }
        }

        // log the error
        Object[] args = { message, th };

        // pk435011
        logger.logp(Level.SEVERE, CLASS_NAME, "log", "log.servlet.error", args);
    }

    /**
     * @see javax.servlet.ServletContext#removeAttribute(String)
     */
    public void removeAttribute(String arg0) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "removeAttribute", "name [{0}]", new Object[] { name });
        }
        Object o = attributes.remove(arg0);

        this.notifyServletContextAttrRemoved(arg0, o);

        // TODO: check WebAppBean stuff
    }

    /**
     * @see javax.servlet.ServletContext#setAttribute(String, Object)
     */
    public void setAttribute(String name, Object value) {
        // TODO: check is WebAppBean stuff is needed
        // or add BeanContextChild elements..
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "setAttribute", "name [{0}], value [{1}]", new Object[] { name, value });
        }
        //PM71991 START
        if( WCCustomProperties.REMOVE_ATTRIBUTE_FOR_NULL_OBJECT && value== null){
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
                 logger.logp(Level.FINE, CLASS_NAME,"setAttribute", "cannot set attribute with null value, remove the attribute -->"+name);
            }
            this.removeAttribute(name);
        }
        else{//PM71991 END

            if (attributes.containsKey(name)) {
                Object oldValue = attributes.put(name, value);
                this.notifyServletContextAttrReplaced(name, oldValue);
            } else {
                attributes.put(name, value);
                this.notifyServletContextAttrAdded(name, value);
            }
        }
    }

    /**
     * Method sortByStartUpWeight. Sorts the servlets in the order of their
     * startUp weight. This way, we can iterate the list, and call their
     * respective init() methods if loadAtStartUp() is true, without worrying
     * about the order.
     * 
     * @param iterator
     * @return Iterator
     */
    protected Iterator<IServletConfig> sortNamesByStartUpWeight(Iterator<IServletConfig> iterator) {

        // System.out.println("in sort");
        int min = Integer.MAX_VALUE;
        int maxpos = 0;

        sortedServletConfigs = new ArrayList<IServletConfig>();
        while (iterator.hasNext()) {
            IServletConfig sc = iterator.next();
            addToStartWeightList(sc);
        }

        return sortedServletConfigs.iterator();

    }

    /**
     * Method addToStartWeightList.
     */
    public void addToStartWeightList(IServletConfig sc) {
        // we haven't started sorting the startup weights yet so just ignore. It
        // will be added later.
        if (this.sortedServletConfigs == null)
            return;
        int size = this.sortedServletConfigs.size();
        int pos = 0;
        boolean added = false;

        if (size == 0 || !sc.isLoadOnStartup())
            sortedServletConfigs.add(sc);
        else {
            // remove the current entry if it was already added
            if (sc.isAddedToLoadOnStartup() && sc.isWeightChanged())
                sortedServletConfigs.remove(sc);

            int value = sc.getStartUpWeight();

            for (IServletConfig curServletConfig : sortedServletConfigs) {
                int curStartupWeight = curServletConfig.getStartUpWeight();
                if (value < curStartupWeight || !curServletConfig.isLoadOnStartup()) {
                    sortedServletConfigs.add(pos, sc);
                    added = true;
                    break;
                }
                pos++;
            }

            if (!added)
                sortedServletConfigs.add(sc);
        }
        sc.setAddedToLoadOnStartup(true);

    }

    public void destroy() {
        
       
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME, "destroy", "entry");
        
        //Lock 
        synchronized(lock){

            if(!initialized){
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                    logger.logp(Level.FINE, CLASS_NAME, "destroy", "WebApp {0} has not been initialized", applicationName);
            }
            // 325429 BEGIN
            if (destroyed.booleanValue()) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                    logger.logp(Level.FINE, CLASS_NAME, "destroy", "WebApp {0} is already destroyed", applicationName);
                return;
            }
            destroyed = Boolean.TRUE;
            // 325429 END
            try {
/*
 * Liberty - comenting out WAS code
 * origClassLoader = ThreadContextHelper.getContextClassLoader();
 * final ClassLoader warClassLoader = getClassLoader();
 * if (warClassLoader != origClassLoader)
 * {
 * ThreadContextHelper.setClassLoader(warClassLoader);
 * }
 * else
 * {
 * origClassLoader = null;
 * }
 */
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                    logger.logp(Level.FINE, CLASS_NAME, "destroy", "WebApp {0} is destroying", applicationName);

                if (webAppNameSpaceCollab != null) {
                    webAppNameSpaceCollab.preInvoke(getModuleMetaData().getCollaboratorComponentMetaData());
                }
                notifyStop();
                
                //PM70296 Start
                // The following destroy logic was added for servlets which does not any mapping defined in config,
                // so targetMappings were not generated. It will also call destory on serverServletByClassname servlets.
                
                Iterator<String> namesIt = this.config.getServletNames();
                while (namesIt.hasNext()) {
                    String servletName = namesIt.next();
                    if (this.config.getServletMappings(servletName)==null) {//no mappings for this servlet name                     
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                            logger.logp(Level.FINE, CLASS_NAME, "destroy", "no mappings for servlet:  "+ servletName);
                        IServletWrapper wrapper = getServletWrapper(servletName);
                        if(wrapper != null){ //the servlet was loaded
                            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                                logger.logp(Level.FINE, CLASS_NAME, "destroy", "call destroy for servlet:  "+ servletName);
                            wrapper.destroy();                       
                        }
                    }
                }   //PM70296 End

                Iterator targets = requestMapper.targetMappings();

                // The super class lifecycle handling will not work for
                // ServletWrappers
                // because it doesn't extend BaseContainer. Hence we will explictly
                // destroy each ServletWrapper

                while (targets.hasNext()) {
                    RequestProcessor p = (RequestProcessor) targets.next();

                    if (p instanceof IServletWrapper)
                        ((IServletWrapper) p).destroy();
                }

                super.destroy();

                if (filterManager != null && filterManager.areFiltersDefined())
                    filterManager.shutdown();

                Enumeration enumServletNames = new IteratorEnumerator(config.getServletNames());

                eventSource.onApplicationEnd(new ApplicationEvent(this, this, enumServletNames));
                if (sessionCtx != null) {
                    sessionCtx.stop(name);
                }

                eventSource.onApplicationUnavailableForService(new ApplicationEvent(this, this, enumServletNames));

                jspClassLoadersMap = null; // PK50133

                notifyServletContextDestroyed();

                preDestroyListeners(new ArrayList[] { servletContextListeners, servletContextLAttrListeners, servletRequestListeners,
                                                      servletRequestLAttrListeners, sessionListeners, sessionIdListeners, sessionAttrListeners, sessionActivationListeners, sessionBindingListeners });
                
                //Release all the CDI managed contexts
                for (Map.Entry<Object, ManagedObject> entry : cdiContexts.entrySet()) {
                    ManagedObject mo = entry.getValue();
                    if (null != mo)
                        mo.release();
                }
                cdiContexts.clear();
                
            } catch (Throwable th) {
                Object[] vals = { this.getName(), th };
                // pk435011
                logger.logp(Level.SEVERE, CLASS_NAME, "destroy", "WebApp.destroy.encountered.errors", vals);
                com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th, CLASS_NAME + ".destroy", "2459", this);
            } finally {
                if (webAppNameSpaceCollab != null) {
                    webAppNameSpaceCollab.postInvoke();
                }
                /*
                 * Liberty commenting out WAS code
                 * if (origClassLoader != null)
                 * {
                 * final ClassLoader fOrigClassLoader = origClassLoader;
                 * 
                 * ThreadContextHelper.setClassLoader(fOrigClassLoader);
                 * }
                 */
            }

            destroyListeners(new ArrayList[] { servletContextListeners, servletContextLAttrListeners, servletRequestListeners,
                                              servletRequestLAttrListeners, sessionListeners, sessionIdListeners, sessionAttrListeners, sessionActivationListeners, sessionBindingListeners });

            // Begin 299205, Collaborator added in extension processor recieves no
            // events
            finishDestroyCleanup();

        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME, "destroy", "exit");
    }

    protected void destroyListeners(ArrayList listeners[]) {
    }

    
    protected void preDestroyListeners(ArrayList listeners[]) {
        // before destroying, call preDestroy method on annotated objects
        for (ArrayList list : listeners) {
            for (Object listener : list) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "destroyListeners", "found listener: " + listener );
                }    
                this.invokeAnnotTypeOnObjectAndHierarchy(listener, ANNOT_TYPE.PRE_DESTROY);
            }
       }

    }

    protected void finishDestroyCleanup() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME, "finishDestroyCleanup", "core WebApp {finishDestroyCleanup()", applicationName);
        
        //Remove all 3 nulls below, these should be garbage collected when the WebApp is no longer referenced.
        //this.loader = null; // PK17371
        //this.sessionCtx = null;
        //this.defaultExtProc = null;
        
        // parent.removeSubContainer(name); //PK37449
        callWebAppInitializationCollaborators(InitializationCollaborCommand.STOPPED);

        attributes.clear();
    }

    // End 299205, Collaborator added in extension processor recieves no events

    /**
     * Method sendError.
     * 
     * @param req
     * @param res
     * @param error
     */
    public void sendError(HttpServletRequest req, HttpServletResponse res, ServletErrorReport error) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.entering(CLASS_NAME, "sendError", "error :" + error.getMessage());

        req.setAttribute("javax.servlet.jsp.jspException", error);
        
        WebContainerRequestState reqState = WebContainerRequestState.getInstance(true);   //PI80786

        // PK82794
        if (WCCustomProperties.SUPPRESS_LAST_ZERO_BYTE_PACKAGE)
            reqState.setAttribute("com.ibm.ws.webcontainer.suppresslastzerobytepackage", "true");
        // PK82794

        if (!res.isCommitted())
            res.resetBuffer();

        String servletName = error.getTargetServletName();

        // begin PK04668 IF THE CLIENT THAT MADE THE SERVLET REQUEST GOES
        // DOWN,THERE IS WAS.webcontainer
        Throwable rootCause = error.getRootCause();
        if (rootCause instanceof ClosedConnectionException) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "sendError",
                        "sendError occured as a result of ClosedConnectionException. skip sending error page to client");
                if (rootCause.getCause() != null)
                    logger.logp(Level.FINE, CLASS_NAME, "sendError", "cause of closed connection", rootCause.getCause());
            }
            eventSource.onServletServiceError(new ServletErrorEvent(this, this, servletName, null, error));
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                logger.exiting(CLASS_NAME, "sendError", "ClosedConnectionException");
            return;
        } else if (rootCause instanceof javax.servlet.UnavailableException) {
            // end PK04668 IF THE CLIENT THAT MADE THE SERVLET REQUEST GOES
            // DOWN,THERE IS WAS.webcontainer
            // 198256 - begin
            // if (error instanceof WebAppErrorReport)
            // {
            // ((WebAppErrorReport)
            // error).setErrorCode(HttpServletResponse.SC_NOT_FOUND);
            // }
            // 198256 - end
            this.eventSource.onServletServiceDenied(new ServletErrorEvent(this, this, servletName, null, error));
        }
        
        //PI80786 set error code to 400 to avoid logging as 500
        if (SET_400_SC_ON_TOO_MANY_PARENT_DIRS){
            if(reqState.getAttribute("com.ibm.ws.webcontainer.set400") != null) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "sendError", "Setting the status code to 400");
                }
                error.setErrorCode(HttpServletResponse.SC_BAD_REQUEST);
                reqState.removeAttribute("com.ibm.ws.webcontainer.set400");
            }
        }
        //PI80786

        if (error.getErrorCode() >= 500) {
            if (servletName == null)
                // Defect 211450
                logError(error.getUnencodedMessage(), error);
            else {
                eventSource.onServletServiceError(new ServletErrorEvent(this, this, servletName, null, error));
                logTrace(error.getUnencodedMessage(), error);
            }
        }

        if (req.getAttribute(ServletErrorReport.ATTRIBUTE_NAME) != null) {
            // we are in a recursive situation...report a recursive error
            ServletErrorReport oError = (ServletErrorReport) req.getAttribute(ServletErrorReport.ATTRIBUTE_NAME);
            reportRecursiveError(req, res, oError, error);
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                logger.exiting(CLASS_NAME, "sendError", "recursive error");
            return;
        }

        // set the error code in the response
        try {
            // if the error code hasn't been set, set it to server error
            if (error.getErrorCode() < 100 || error.getErrorCode() > 599) {
                error.setErrorCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }

            // d124698 - set the status code attribute in the request

            req.setAttribute(javax.servlet.RequestDispatcher.ERROR_STATUS_CODE, new Integer(error.getErrorCode()));

            // set the response status
            res.setStatus(error.getErrorCode());

            // We have to determine the charset to use with the error page
            String clientEncoding = req.getCharacterEncoding();
            // PK21127 start
            if (clientEncoding != null && !EncodingUtils.isCharsetSupported(clientEncoding)) {
                // charset not supported, continue with the logic to determine
                // the encoding
                clientEncoding = null;
            }
            // PK21127 end
            if (clientEncoding == null)
                clientEncoding = com.ibm.wsspi.webcontainer.util.EncodingUtils.getEncodingFromLocale(req.getLocale());
            if (clientEncoding == null)
                clientEncoding = System.getProperty("default.client.encoding");
            if (clientEncoding == null)
                clientEncoding = "ISO-8859-1";

            res.setContentType("text/html;charset=" + clientEncoding);
        } catch (IllegalStateException ise) {
            // failed to set status code.
            // This could be caused by:
            // 1. the servlet is being included.
            // 2. the stream response is already committed
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(ise, CLASS_NAME + ".handleError", "865", this);
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                logger.logp(Level.FINE, CLASS_NAME, "sendError", "WebApp.sendError() failed to set status code.\n"
                                                                 + "This may be caused by a servlet calling response.sendError() "
                                                                 + "while being included or after the response has already been committed to the client.", ise);
        }

        // set the exception as an error bean in the response
        req.setAttribute(ServletErrorReport.ATTRIBUTE_NAME, error);

        // get this request's error page dispatcher
        RequestDispatcher rd = null;
        try {
            rd = getErrorPageDispatcher(req, error);
        } catch (MajorHandlingRuntimeException e) {
            //If there was a major problem like the application has stopped from under us, we will throw this
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {//306998.15
                logger.logp(Level.FINE, CLASS_NAME, "sendError", "sending sendUnavailableException for context: ["+req.getContextPath()+"]");
            }
            try {
                WebContainer.sendAppUnavailableException(req, res);
            } catch (IOException e1) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {//306998.15
                    logger.logp(Level.FINE, CLASS_NAME, "sendError", "", e);
                }
            }
            return;
        }

        // PK37608 Start
        if (WCCustomProperties.SUPPRESS_WSEP_HEADER) {

            Enumeration ViaHeaderValues = req.getHeaders("Via");

            if (ViaHeaderValues != null) {
                boolean foundODRHeader = false;
                while (!foundODRHeader && ViaHeaderValues.hasMoreElements()) {

                    String ViaHeaderValue = (String) ViaHeaderValues.nextElement();

                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                        logger.logp(Level.FINE, CLASS_NAME, "sendError", "Via Header value : " + ViaHeaderValue);
                    }

                    if (ViaHeaderValue.indexOf("On-Demand Router") != -1) {

                        foundODRHeader = true;

                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                            logger.logp(Level.FINE, CLASS_NAME, "sendError", "Via Header with On-Demand Router found, add $WSEP header to response.");
                        }

                        res.addHeader("$WSEP", "");

                    }
                }

                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE) && foundODRHeader == false) {
                    logger.logp(Level.FINE, CLASS_NAME, "sendError",
                            "No Via header found with On-Demand Router. Do not add $WSEP header to response.");
                }

            } else if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "sendError", "No Via Header, do not add $WSEP header to response.");
            }

        } else {

            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "sendError", "Add $WSEP header to response.");
            }

            res.addHeader("$WSEP", "");
        }
        // PK37608 End

        try {
            if (rd == null) {
                //PI09474 Start
                // PK23428 BEGIN
                try {

                    PrintWriter pw = res.getWriter();
                    if( WCCustomProperties.DISPLAY_TEXT_WHEN_NO_ERROR_PAGE_DEFINED == null) {
                        // PM03788 Start
                        if (WCCustomProperties.SET_UNENCODED_HTML_IN_SENDERROR) {
                            pw.println(error.getUnencodedMessageAsHTML());
                        }
                        else {
                            if (WCCustomProperties.INCLUDE_STACK_IN_DEFAULT_ERROR_PAGE || this.isLocalAddr(req.getRemoteAddr())) {
                                pw.println(error.getDebugMessageAsHTML());
                            } else {
                                pw.println(error.getMessageAsHTML());
                            }
                        }// PM03788 End
                    }
                    else {
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                            logger.logp(Level.FINE, CLASS_NAME, "sendError", "display custom error text provided in the property");
                        }
                        pw.println(WCCustomProperties.DISPLAY_TEXT_WHEN_NO_ERROR_PAGE_DEFINED);
                    }
                } catch (IllegalStateException ise) {
                    ServletOutputStream os = res.getOutputStream();
                    reqState.setAttribute("com.ibm.ws.webcontainer.AllowWriteFromE", true); 
                    if( WCCustomProperties.DISPLAY_TEXT_WHEN_NO_ERROR_PAGE_DEFINED == null) {                                             
                        // PM03788 Start
                        if (WCCustomProperties.SET_UNENCODED_HTML_IN_SENDERROR) {
                            os.println(error.getUnencodedMessageAsHTML());
                        }
                        else {
                            os.println(error.getMessageAsHTML());
                        }// PM03788 End
                    }
                    else {
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                            logger.logp(Level.FINE, CLASS_NAME, "sendError", "display custom error text provided in the property");
                        }
                        os.println(WCCustomProperties.DISPLAY_TEXT_WHEN_NO_ERROR_PAGE_DEFINED);
                    }
                    reqState.removeAttribute("com.ibm.ws.webcontainer.AllowWriteFromE");
                }
                // PK23428 END
                // PI09474 End
            } else {
                try {
                    // first attempt to forward (this permits setting of
                    // headers)
                    // res.setStatus(200);
                    rd.forward(req, res);
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                        logger.exiting(CLASS_NAME, "sendError", "after forward");
                    return;
                } catch (IllegalStateException ise) {
                    // include the error handler
                    rd.include(req, res);
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                        logger.exiting(CLASS_NAME, "sendError", "after include");
                    return;
                }

            }
        } catch (Throwable th) {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th, CLASS_NAME + ".handleError", "912", this);
            // logger.logp(Level.SEVERE, CLASS_NAME,"sendError",
            // "Error.occurred.while.invoking.error.reporter", new Object[] {
            // error, th });

            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "sendError", "Error occurred while invoking error reporter");
                logger.logp(Level.FINE, CLASS_NAME, "sendError", "URL: " + req.getRequestURL());
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);

                if (th instanceof ServletException) {
                    DefaultErrorReporter.printFullStackTrace(pw, (ServletException) th);
                } else {
                    new TruncatableThrowable(th).printStackTrace(pw);
                }

                pw.flush();
                logger.logp(Level.FINE, CLASS_NAME, "sendError", "Full Exception dump of original error", sw.toString());
                sw = new StringWriter();
                pw = new PrintWriter(sw);
                if (th instanceof ServletException) {
                    DefaultErrorReporter.printFullStackTrace(pw, (ServletException) th);
                } else {
                    new TruncatableThrowable(th).printStackTrace(pw);
                }

                pw.flush();
                logger.logp(Level.FINE, CLASS_NAME, "sendError", "Full Exception dump of recursive error", sw.toString());
            }

            reportRecursiveError(req, res, error, new WebAppErrorReport(th));
        }

        try {
            // reset the error bean object
            req.setAttribute(ServletErrorReport.ATTRIBUTE_NAME, null);

            // 101639 reset the jsp exception object
            req.setAttribute("javax.servlet.jsp.jspException", null);
        } catch (Throwable th) {
            /* ignore */
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th, CLASS_NAME + ".handleError", "961", this);
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.exiting(CLASS_NAME, "sendError");

    }
    
   private boolean isLocalAddr(String remoteAddr) {
        
        // Look for the address to be a loopback address according to IPV4
        // this can be any address from 127.0.0.0 to 127.255.255.254 
        boolean result=true;
        if (remoteAddr!=null) {
            InetAddress addr;
            try {
                addr = InetAddress.getByName(remoteAddr);
                result = addr.isLoopbackAddress();
            } catch (UnknownHostException e) {
                result = false;
            }    
        }
        
        return result;
    }    

    // begin PK04177 CONTENTS UNDER WEBMODULE WEB-INF DIRECTORY ARE ACCESSIBLE
    // WAS.webcontainer: rewritten
    private boolean isForbidden(String uri) {
        String reqUri = uri.toUpperCase();
        reqUri = removeLeadingSlashes(reqUri);

        if (reqUri == null) {
            return false;
        }

        // As per spec (servlet 2.4), deny access to WEB-INF
        if (reqUri.startsWith("WEB-INF/") || reqUri.startsWith("META-INF/"))
            return true;
        else if ((reqUri.equals("WEB-INF") || reqUri.equals("META-INF"))) {
            return true;
        } else {
            return false;
        }
    }

    /*
     * removeLeadingSlashes -- Removes all slashes from the head of the input
     * String.
     */
    private String removeLeadingSlashes(String src) {
        String result = null;
        int i = 0;
        boolean done = false;

        if (src == null)
            return null;

        int len = src.length();
        while ((!done) && (i < len)) {
            if ((src.charAt(i) == '/') || (src.charAt(i) == ' ')) {
                i++;
            } else {
                done = true;
            }
        }

        // If all slashes were stripped off and there was no remainder, then
        // return null.
        if (done) {
            result = src.substring(i);
        }

        return result;
    }

    // end PK04177 CONTENTS UNDER WEBMODULE WEB-INF DIRECTORY ARE ACCESSIBLE
    // WAS.webcontainer: rewritten

    /**
     * Set request attributes for a generated error, and create and
     * return an error page dispatcher, if an error page is configured
     * for the generated error.
     * 
     * Look for the error page based on the error code and exception.
     * Use the default error page if the code and exception lookup fails.
     * 
     * The default is to lookup first by error-code, and second by
     * exception-type.  However, this order is reversed if the customer
     * property {@link WCCustomProperties#ERROR_EXCEPTION_TYPE_FIRST} is set.
     *   
     * Lookup by exception-type uses the root exception, not the immediate
     * exception as packaged in the error report.  The root exception is
     * obtained by repeating calls to {@link ServletException#getRootCause()}. 
     * 
     * Answer null if no error page is found for the generated error.
     *
     * A major exception (a runtime exception) occurs if the web application
     * is destroyed, or if the web application configuration is null.
     * 
     * @param req The request which generated an error.
     * @param ser The generated error.
     *
     * @return A dispatcher for the error.  Null if none is available.
     */
    public RequestDispatcher getErrorPageDispatcher(ServletRequest req, ServletErrorReport ser) {

        if (this.getDestroyed().booleanValue()) { // should be a fast boolean check.
            throw new MajorHandlingRuntimeException("WebContainer can not handle the request");
        }

        String methodName = "getErrorPageDispatcher";

        // Get and Set error request attributes.
        Integer errorCode = new Integer(ser.getErrorCode());
        Class errorException = ser.getExceptionClass();
        String errorMessage = ser.getMessage();
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, methodName, "Looking for defined Error Page!");
            logger.logp(Level.FINE, CLASS_NAME, methodName, "Exception errorCode=" + errorCode);
            logger.logp(Level.FINE, CLASS_NAME, methodName, "Exception type=" + errorException);
            logger.logp(Level.FINE, CLASS_NAME, methodName, "Exception message=" + errorMessage);
        }

        // 114582 - begin - get down to the root servlet exception
        ServletException sx = (ServletException) ser;
        Throwable th = sx.getRootCause();

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, methodName, "Servlet Exception is: " + sx);
            logger.logp(Level.FINE, CLASS_NAME, methodName, "th (root cause) is: " + th);
        }

        // Defect 114582 - walk upwards to the root servlet exception.

        // Defect 114582 - test against ServletException if preV7; otherwise, test against ServletErrorReport.

        boolean isPreV7 =
            ((WCCustomProperties.ERROR_PAGE_COMPATIBILITY != null) &&
             WCCustomProperties.ERROR_PAGE_COMPATIBILITY.equalsIgnoreCase("V6"));
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, methodName, (isPreV7 ? "is PreV7" : "not PreV7"));
        }        

        // Walk upwards while there is another servlet exception and a new root cause.

        // Defect 155880 - Don't shift upwards if the root cause is null.

        if (isPreV7) {
            while (th != null && th instanceof ServletException && ((ServletException) th).getRootCause() != null) { // defect
                sx = (ServletException) th;
                th = sx.getRootCause();
                
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, methodName, "unwrapping exceptions Servlet Exception is now: " + sx);
                    logger.logp(Level.FINE, CLASS_NAME, methodName, "th (root cause) is now: " + th);
                }                
            }

        } else {
            while (th != null && th instanceof ServletErrorReport && ((ServletErrorReport) th).getRootCause() != null) {
                sx = (ServletException) th;
                th = sx.getRootCause();

                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, methodName, "unwrapping exceptions Servlet Exception is now: " + sx);
                    logger.logp(Level.FINE, CLASS_NAME, methodName, "th (root cause) is now: " + th);
                }
                
            }
        }

        // 114582 - only set exception type if root exception is not null
        if (th != null) {
            req.setAttribute("javax.servlet.error.exception", th);
            if (errorException != null) {
                req.setAttribute("javax.servlet.error.exception_type", errorException);
            }
        }
        
        if (errorMessage != null) {
            req.setAttribute("javax.servlet.error.message", errorMessage);
        }


        // add the request uri to the request object
        HttpServletRequest httpServletReq = (HttpServletRequest) ServletUtil.unwrapRequest(req, HttpServletRequest.class);
        req.setAttribute("javax.servlet.error.request_uri", httpServletReq.getRequestURI());

        WebContainerRequestState.getInstance(true).setAttribute("isErrorDispatcherType", "true");

        // add the target servlet name to the request object
        if (ser.getTargetServletName() != null) {
            req.setAttribute("javax.servlet.error.servlet_name", ser.getTargetServletName());
        } else {
            IExtendedRequest wsRequest = (IExtendedRequest) ServletUtil.unwrapRequest(req);
            WebAppDispatcherContext ctxt = (WebAppDispatcherContext) wsRequest.getWebAppDispatcherContext();
            RequestProcessor processor = ctxt.getCurrentServletReference();
            if (processor != null) {
                String name = processor.getName();
                req.setAttribute("javax.servlet.error.servlet_name", name);
            }
        }

        // LIDB1234.5 - end

        // Arguably, the request attributes are unnecessary if the configuration is unavailable,
        // meaning, the exception maybe could be thrown at the beginning of this method.

        if (config==null) { //if the application was stopped then config will be null.  It's possible that a previous request could have made it here and thrown an exception 
            throw new MajorHandlingRuntimeException("WebContainer can not handle the request", sx);
        }

        // Look for an error page which matches by error-code or exception-type, then
        // look for a default error page.
                
        String errorURL = null;
        Object errorSelector = null;
        String errorURLCase = null;

        // START PK55149:
        //
        // If custom property "com.ibm.wsspi.webcontainer.WCCustomProperties.ERROR_EXCEPTION_TYPE_FIRST" is
        // enabled, lookup by exception type then by error code.
        //
        // Default to lookup by error code then by error code.
        
        // Maybe lookup the error-code first ...
        if (!errorExceptionTypeFirst) {
            ErrorPage errorPage = config.getErrorPageByErrorCode(errorCode);

            if (errorPage != null) {
                errorURL = errorPage.getLocation();
                if ((errorURL != null)) { // Should not be null or empty, but check, just in case.                
                    if (errorURL.length() > 0) {
                        errorSelector = errorCode;
                        errorURLCase = "error-code";
                    } else {
                        errorURL = null;
                    }
                }
            }            
        }
        
        // Either, the error-code lookup was first, but failed,
        // or the exception-type lookup is first.       
        if (errorURL == null) {
            if (th != null) {
                ErrorPage errorPage;                
                if (isPreV7) {
                    errorPage = config.getErrorPageByExceptionType(th);
                } else {
                    errorPage = config.getErrorPageTraverseRootCause(th);
                }

                if (errorPage != null) {
                    errorURL = errorPage.getLocation();
                    if ((errorURL != null)) { // Should not be null or empty, but check, just in case.
                        if (errorURL.length() > 0) {
                            errorSelector = errorCode;
                            errorURLCase = "exception-type";
                        } else {
                            errorURL = null;
                        }
                    }
                }
            }
        }

        // .. or lookup by error-code second.
        if (errorExceptionTypeFirst && (errorURL == null)) {
            ErrorPage errorPage = config.getErrorPageByErrorCode(errorCode);
            
            if (errorPage != null) {
                errorURL = errorPage.getLocation();
                if ((errorURL != null)) {  // Should not be null or empty, but check, just in case.
                    if (errorURL.length() > 0) {
                        errorSelector = errorCode;
                        errorURLCase = "error-code";
                    } else {
                        errorURL = null;
                    }
                }
            }
        }
        
        // END PK55149

        // If no error page matching the specific error code or exception type was
        // obtained, then, if configured, use the default error page.  A default
        // error page will be obtained from the web.xml or web-fragment.xml as
        // an error page with no error code and no exception type, or will be
        // obtained from the ibm-web-ext.xml as an expliclit value.
        
        // But, don't use a default error page for error code 403.
        // Error code 403 is a security error code:
        // "Error 403--Forbidden when trying to access the servlet".
        
        if (errorURL == null) {            
            if (errorCode.intValue() == 403) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, methodName, "No match on error-code or exception-type; code 403 prevents default error page");
                }
                return null;
            }
            
            errorURL = config.getDefaultErrorPage();
            if (errorURL != null) {
                // Should no be empty, but check, just in case.            
                if (errorURL.length() > 0) {
                    errorSelector = "DEFAULT";
                    errorURLCase = "default";
                } else {
                    errorURL = null;
                }
            }
        }

        if (errorURL == null) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, methodName, "No match on error-code or exception-type; no default error page");
            }            
            return null;
        }

        // Prior logging put this out before forcing the initial "/"; keep to that pattern.
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, methodName, "Found " + errorURLCase + "=" + errorSelector + " with location=" + errorURL);
        }

        if (!errorURL.startsWith("/")) {
            errorURL = "/" + errorURL;
        }

        // WebAppDispatcherContext dispatchContext = new
        // WebAppDispatcherContext(this);
        // dispatchContext.setRelativeUri(errorURL);

        return getRequestDispatcher(errorURL);
    }

    /**
     * @param req
     * @param res
     * @param oError
     * @param error
     */
    private void reportRecursiveError(ServletRequest req, ServletResponse res, ServletErrorReport originalErr, ServletErrorReport recurErr) {

        try {
            String message = error_nls.getString("error.page.exception", "Error Page Exception");

            // log("Original Error: ", originalErr);
            Object[] args = { getName(), this.getContextPath(), message, recurErr };
            logger.logp(Level.SEVERE, CLASS_NAME, "reportRecursiveError", message + ":", args);

            PrintWriter out;
            try {
                out = res.getWriter();
            } catch (IllegalStateException e) {
                com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, CLASS_NAME + ".reportRecursiveError", "985", this);
                out = new PrintWriter(new OutputStreamWriter(res.getOutputStream(), res.getCharacterEncoding()));
            }
            if (!WCCustomProperties.SUPPRESS_HTML_RECURSIVE_ERROR_OUTPUT) { // PK77421
                out
                        .println("<H1>"
                            + message
                            + "</H1>\n<H4>"
                                + nls
                                        .getString("cannot.use.error.page",
                                            "The server cannot use the error page specified for your application to handle the Original Exception printed below.") // 406426
                            + "</H4>");
                out.println("<BR><H3>" + error_nls.getString("original.exception", "Original Exception") + ": </H3>"); // 406426
                printErrorInfo(out, originalErr);
                out.println("<BR><BR><H3>" + error_nls.getString("error.page.exception", "Error Page Exception") + ": </H3>"); // 406426
                printErrorInfo(out, recurErr);
                out.flush();
            }
        } catch (Throwable th) {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th, CLASS_NAME + ".reportRecursiveError", "998", this);
            log("Unable to report exception to client", th);
        }
    }

    /**
     * @param out
     * @param originalErr
     */
    private void printErrorInfo(PrintWriter out, ServletErrorReport e) throws IOException {
        out.println("<B>" + error_nls.getString("error.message", "Error Message") + ": </B>" + e.getMessage() + "<BR>"); // 406426
        out.println("<B>" + error_nls.getString("error.code", "Error Code") + ": </B>" + e.getErrorCode() + "<BR>"); // 406426
                out.println("<B>"+error_nls.getString("target.servlet", "Target Servlet")+": </B>" 
                             + DefaultErrorReporter.encodeChars(e.getTargetServletName()) + "<BR>"); //406426 //PM18512, encoded                
        out.println("<B>" + error_nls.getString("error.stack", "Error Stack") + ": </B><BR>"); // 406426

        DefaultErrorReporter.printShortStackTrace(out, e);
    }

    /**
     * @see com.ibm.ws.core.RequestProcessor#handleRequest(IWCCRequest,
     *      IWCCResponse)
     */
    public void handleRequest(ServletRequest request, ServletResponse response) throws Exception {
        handleRequest(request, response, null);
    }
    
    public void handleRequest(ServletRequest request, ServletResponse response, HttpInboundConnection httpInboundConnection) throws Exception {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.entering(CLASS_NAME, "handleRequest");

        IExtendedRequest req = (IExtendedRequest) request;
        Response res = (Response) response;
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "URI --> " + req.getRequestURI() + " handled by WebApp --> " + applicationName);
        }
        WebAppDispatcherContext dispatchContext = (WebAppDispatcherContext) req.getWebAppDispatcherContext();
        String fullUri = dispatchContext.getDecodedReqUri(); // 280335, do not
        // decode again
        // since done in
        // VirtualHost
        String partialUri = fullUri;

        dispatchContext.setWebApp(this);

        if (!contextPath.equals("/")) {
            int index = 0;
            if (contextPath.endsWith("/*")) {
                index = contextPath.length() - 1;
            } else {
                index = contextPath.length();
            }

            partialUri = fullUri.substring(index); // .trim()

            // BEGIN PK27974

            if (WebApp.redirectContextRoot && (partialUri.length() == 0) && (req instanceof HttpServletRequest)) {
                // PK79143 Start
                // dispatchContext.sendRedirect (((HttpServletRequest)
                // req).getRequestURL() + "/");
                ((HttpServletResponse) res).sendRedirect(((HttpServletRequest) req).getRequestURL() + "/");
                // PK79143 End

                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                    logger.exiting(CLASS_NAME, "handleRequest");
                return;
            }

            // END PK27974
        }

        if (partialUri.length() == 0)
            partialUri = "/";
        dispatchContext.setRelativeUri(partialUri);
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "setValues IfMultiReadofPostdataEnabled");
        }
        req.setValuesIfMultiReadofPostdataEnabled(); //MultiRead

        if (isForbidden(partialUri)) {
            WebAppErrorReport ser = new WebAppErrorReport(new ServletException(MessageFormat.format(nls.getString("File.not.found",
                    "File not found: {0}"), new Object[] { partialUri })));
            ser.setErrorCode(HttpServletResponse.SC_NOT_FOUND);
            if (req instanceof HttpServletRequest) {
                sendError((HttpServletRequest) req, (HttpServletResponse) res, (WebAppErrorReport) ser);
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                    logger.exiting(CLASS_NAME, "handleRequest - sendError : Not allowed to access contents of WEB-INF/META-INF");
                return;
            } else {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                    logger.exiting(CLASS_NAME, "handleRequest - Not allowed to access contents of WEB-INF/META-INF");
                throw new ServletException("Not allowed to access contents of WEB-INF/META-INF");
            }
        }
        boolean securityEnabled = ((CollaboratorHelperImpl)collabHelper).isSecurityEnabled();
        if (securityEnabled) {
            // these form login related identifiers can occur anywhere in the
            // URI
            // hence this is the only place to check for them.
            String servletPath = null; // PK79894
            String pathInfo = null; // PK79894
            if (fullUri.indexOf("j_security_check") != -1 && (!WCCustomProperties.ENABLE_EXACT_MATCH_J_SECURITY_CHECK || fullUri.endsWith("/j_security_check") )) { //F011107
                // PK79894 Start
                // Note: setting the servletPath to "/j_security check" is arguably wrong
                // for a request such aa /<context-root>/a/b/j_security_check which will be 
                // processed as a "j_ecurity_check". However such a request is not valid
                // according to the servlet specification (SRV 12.5.3.1). Further the
                // customer who requested PK79894 did not complain about the servletPath
                // being incorrect. As a result it was decided not to change the servletPath 
                // setting in PK95461 but leave it hard-coded as "j_security_check"
                // 

                //PI60797 - start - return 404 for all methods but POST
                if (WCCustomProperties.ENABLE_POST_ONLY_J_SECURITY_CHECK && !req.getMethod().equalsIgnoreCase("POST")){
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                        logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "Only POST is allowed for /j_security_check"); 
                    }

                    WebAppErrorReport ser = new WebAppErrorReport(new ServletException(MessageFormat.format(nls.getString("File.not.found",
                                    "File not found: {0}"), new Object[] { partialUri })));
                    ser.setErrorCode(HttpServletResponse.SC_NOT_FOUND);
                    if (req instanceof HttpServletRequest) {
                        sendError((HttpServletRequest) req, (HttpServletResponse) res, (WebAppErrorReport) ser);
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                            logger.exiting(CLASS_NAME, "handleRequest - sendError : Only POST is allowed for /j_security_check");
                        return;
                    } else {
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                            logger.exiting(CLASS_NAME, "handleRequest - Only POST is allowed for /j_security_check");
                        throw new ServletException("Only POST is allowed for /j_security_check");
                    }
                }
                //PI60797 - end
                servletPath = "/j_security_check"; // Get servletPath
                pathInfo = getPathInfoforSecureloginlogout(fullUri, "j_security_check"); // Get
                // PathInfo
                dispatchContext.setPathElements(servletPath, pathInfo);
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "security is enabled, URI = " + fullUri + " , servletPath = "
                            + ((HttpServletRequest) req).getServletPath() + " , pathInfo = " + req.getPathInfo());
                }// PK79894 End

                // PK95461 - setPathElements sets the relativeURI to servletPath+pathInfo
                // which will be wrong if the URI was something like /<context-root>/a/b/j_security check
                // This is problematic because the relativeURI is used for filter mappring, so
                // reset the requestURI
                dispatchContext.setRelativeUri(partialUri);

                ExtensionProcessor p = getLoginProcessor();
                if (p != null) {
                    try{
                        if (isFiltersDefined()) {
                            EnumSet<CollaboratorInvocationEnum> filterCollabEnum = EnumSet.of(CollaboratorInvocationEnum.NAMESPACE, CollaboratorInvocationEnum.CLASSLOADER, CollaboratorInvocationEnum.SESSION, CollaboratorInvocationEnum.EXCEPTION);
                            // PM79980 , adding CollaboratorInvocationEnum.NAMESPACE , filter can do a resource lookup.
                            filterManager.invokeFilters((HttpServletRequest) req, (HttpServletResponse) res, this, p, filterCollabEnum, httpInboundConnection);
                        } else {
                            p.handleRequest(req, res);
                        }
                    }
                    catch (SecurityViolationException e){
                        throw e;
                    }catch (Throwable th) {
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                            logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "Exception ", new Object[] { th });

                        handleException(th, req, res, p);
                    }
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                        logger.exiting(CLASS_NAME, "handleRequest");
                    return;
                }
            } else if (fullUri.indexOf("ibm_security_logout") != -1) {
                // PK79894 Start
                // Note: setting the servletPath to "ibm_security_logout" is arguably wrong
                // for a request such aa /<context-root>/a/b/ibm_security_logout which will be 
                // processed as a "ibm_security_logout". However, the customer who requested
                // PK79894 did not complain about the servletPath being incorrect. As a result
                // it was decided not to change the servletPath setting in PK95461 but leave
                // it hard-coded as "ibm_security_logout"
                // 
                servletPath = "/ibm_security_logout"; // Get servletPath
                pathInfo = getPathInfoforSecureloginlogout(fullUri, "ibm_security_logout"); // Get
                // PathInfo
                dispatchContext.setPathElements(servletPath, pathInfo);
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "security is enabled,  URI = " + fullUri + " , servletPath = "
                            + ((HttpServletRequest) req).getServletPath() + " , pathInfo = " + req.getPathInfo());
                }// PK79894 End

                // 610571 - setPathElements sets the relativeURI to servletPath+pathInfo
                // which will be wrong if the URI was something like /<context-root/a/b/ibm_security_logout
                // This is problematic because the relativeURI is used for filter mappring, so
                // reset the requestURI.
                dispatchContext.setRelativeUri(partialUri);

                ExtensionProcessor p = getLogoutProcessor();
                if (p != null) {
                    try{
                        if (isFiltersDefined()) {
                            EnumSet<CollaboratorInvocationEnum> filterCollabEnum = EnumSet.of(CollaboratorInvocationEnum.NAMESPACE, CollaboratorInvocationEnum.CLASSLOADER, CollaboratorInvocationEnum.SECURITY, CollaboratorInvocationEnum.SESSION, CollaboratorInvocationEnum.EXCEPTION);
                            filterManager.invokeFilters((HttpServletRequest) req, (HttpServletResponse) res, this, p, filterCollabEnum);
                        } else {
                            p.handleRequest(req, res);
                        }
                    }catch (SecurityViolationException e){
                        throw e;
                    }catch (Throwable th) {
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                            logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "Exception ", new Object[] { th });

                        handleException(th, req, res, p);
                    }
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                        logger.exiting(CLASS_NAME, "handleRequest");
                    return;
                }
            }
        }

        RequestProcessor requestProcessor;
        RequestMapper rm=requestMapper; // RTC 116972: requestMapper volatile; possible race between handleRequest() and destroy().
        if(rm==null)
        {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "Request arrived after destroy(); no requestMapper, discard silently.");
            }
            requestProcessor = null;
        }
        else
            requestProcessor = rm.map(req);

        try {
            req.start(); // TODO:This can be removed if we had servlet wrapper
            // returned to webcontainer
            res.start();

      // LIBERTY - following code is not in tWAS - why is it needed?
            if (requestProcessor != null) {
                if (requestProcessor instanceof ExtensionProcessor) {
                    IServletWrapper servletWrapper = ((ExtensionProcessor) requestProcessor).getServletWrapper(req, res);
                    if (servletWrapper != null) {
                        requestProcessor = servletWrapper;
                    } else {
                        boolean isJSP = false;
                        Iterator<String> i = DEFAULT_JSP_EXTENSIONS.iterator();
                        while (i.hasNext()) {
                            String pattern = (String) i.next();
                            if (partialUri.contains(pattern)) {
                                isJSP = true;
                                break;
                            }
                        }
                        if (isJSP && (req.getAttribute(javax.servlet.RequestDispatcher.ERROR_STATUS_CODE) == null)) {
                            // If the JSP has failed to compile, there will be an error code set by sendError. If it isn't
                            // set, we assume that the JSP extension handler isn't present (likely because the feature is
                            // not enabled. 
                            if (requestProcessor instanceof DefaultExtensionProcessor) {
                                logger.logp(Level.WARNING, CLASS_NAME, "handleRequest", "no.jsp.extension.handler.found");
                            } else {
                                logger.logp(Level.WARNING, CLASS_NAME, "handleRequest", "File.not.found", new Object[] { partialUri });
                            }
                        } else {
                            logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "WARNING: ExtensionProcessor could not return us a ServletWrapper");
                        }

                    }
                }
            }

            filterManager.invokeFilters((HttpServletRequest) req, (HttpServletResponse) res, this, requestProcessor, CollaboratorHelper.allCollabEnum, httpInboundConnection);

            if (requestProcessor != null) {
                if (requestProcessor instanceof IServletWrapper) {
                    // 271276, do not add to cache if we have error status code
                    if (req.getAttribute(javax.servlet.RequestDispatcher.ERROR_STATUS_CODE) == null)
                        WebContainer.addToCache((HttpServletRequest) req, requestProcessor, this);
                }
            }
        } catch (Throwable th) {
            //PI80786
            if (SET_400_SC_ON_TOO_MANY_PARENT_DIRS) { 
                if(th.getMessage().contains("is invalid because it contains more references to parent directories")){
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                        logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "Request contains more ../ than allowed, will set 400 SC");
                    }

                    WebContainerRequestState reqState = WebContainerRequestState.getInstance(false);
                    if(reqState != null)
                        reqState.setAttribute("com.ibm.ws.webcontainer.set400", "true");
                }
            }
            //PI80786

            boolean logStack = true;
            boolean donothandleexception = false;
            if (th instanceof ServletException) {
                //ServletErrorReport -> ServletException -> FileNotFoundException
                Throwable firstCause = th.getCause();
                if (firstCause!=null) {
                    if (firstCause instanceof FileNotFoundException) {
                        logStack = false;
                    } else {
                        Throwable secondCause = firstCause.getCause();
                        if (secondCause !=null && secondCause instanceof FileNotFoundException) {
                            logStack = false;
                        }
                    }
                }
            } 
            
            if((com.ibm.ws.webcontainer.osgi.WebContainer.getServletContainerSpecLevel() >= 31) && th instanceof IOException){
                if(th.getMessage()!=null && th.getMessage().contains("SRVE0918E")){
                    // dont do anything
                    // we have already given chance to servlet and filters to handle it
                    // this is application error but should not be returned to client.
                    logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "donothandleexception SRVE0918E");
                    donothandleexception = true;
                }
            }
            //160973 Begin
            //ServletErrorReport encodes certain characters and this gets logged in trace.
            //This change will log a cleaned up version to the messages.log while retaining
            //the origninal error with stack to the trace.log. This will cause there to be
            //nearly duplicate messages in the trace.log.
            if (logStack) {
                if (th instanceof WebAppErrorReport) {
                    logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "Original Exception", new Object[] { th });
                    logger.logp(Level.SEVERE, CLASS_NAME, "handleRequest", "Exception", new Object[] { new Throwable(((WebAppErrorReport) th).getUnencodedMessage(),th.getCause()) });
                }
                else {
                logger.logp(Level.SEVERE, CLASS_NAME, "handleRequest", "Exception", new Object[] { th });
                }
            }
            else {
                logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "Exception", new Object[] { th });
            }
            //160973 End
            if(!donothandleexception) {
                handleException(th, req, res, requestProcessor);
            }
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.exiting(CLASS_NAME, "handleRequest");
        return;

    }

    public void handleException(Throwable th, ServletRequest req, ServletResponse res, RequestProcessor requestProcessor) {
        WebContainerRequestState reqState = WebContainerRequestState.getInstance(false);
        if (reqState != null && reqState.isAsyncMode()) {
                //Don't call execute next runnable because that will be done by WebContainer or DispatchRunnable exiting
                //Don't check dispatching because we know its true and want to invoke error handling anyway because we are in control
            // of when it is invoked.
            ListenerHelper.invokeAsyncErrorHandling(reqState.getAsyncContext(), reqState, th, AsyncListenerEnum.ERROR, ExecuteNextRunnable.FALSE, CheckDispatching.FALSE);
        }
        else{
            if (th instanceof ServletErrorReport) {
                // Almost all exceptions from the wrapper will be caught here
                this.sendError((HttpServletRequest) req, (HttpServletResponse) res, (ServletErrorReport) th);
            } else {
                // Just in case something unforeseen happens
                // But first log in FFDC
                com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th, CLASS_NAME + ".handleRequest", "985", this);
                WebAppErrorReport r = new WebAppErrorReport(th);
                if (requestProcessor != null && requestProcessor instanceof ServletWrapper)
                    r.setTargetServletName(((ServletWrapper) requestProcessor).getServletName());
                r.setErrorCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                this.sendError((HttpServletRequest) req, (HttpServletResponse) res, r);
            }
        }
        //start PI26908
        if(DEFER_SERVLET_REQUEST_LISTENER_DESTROY_ON_ERROR){
            if(reqState != null && reqState.getAttribute("deferringNotifyServletRequestDestroyed") != null){
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                            logger.logp(Level.FINE, CLASS_NAME, "handleException", "about to call notifyServletRequestDestroyed()");
                    notifyServletRequestDestroyed(req);
                    reqState.removeAttribute("deferringNotifyServletRequestDestroyed");
            }
        }//end PI26908
    }

    // PK79894 Start
    // find pathInfo if associated alongwith servletPath(/j_security_check or
    // /ibm_security_logout ) in URI
    private String getPathInfoforSecureloginlogout(String currentURI, String securityString) {
        String pathInfo = null;
        int lastIndex_Security = currentURI.lastIndexOf(securityString);
        String restURI = currentURI.substring(lastIndex_Security + securityString.length());

        if (restURI.equals(""))
            pathInfo = null;
        else
            pathInfo = restURI;
        return pathInfo;
    }

    // PK79894 End

    /**
     * Returns the nameSpaceCollaborator.
     * 
     * @return WebAppNameSpaceCollaborator
     */
    public ICollaboratorHelper getWebAppCollaboratorHelper() {
        return this.collabHelper;
    }

    public IInvocationCollaborator[] getWebAppInvocationCollaborators() {
        return null;
    }

    /**
     * Method isFiltersDefined.
     * 
     * @return boolean
     */
    public boolean isFiltersDefined() {
        return this.filterManager.areFiltersDefined();
    }

    /**
     * Method getFilterManager.
     */
    public com.ibm.wsspi.webcontainer.filter.WebAppFilterManager getFilterManager() {
        return this.filterManager;

    }

    /**
     * @param sc
     * @return
     */
    public boolean isErrorPageDefined(int sc) {
        ErrorPage ep = config.getErrorPageByErrorCode(new Integer(sc));

        if (ep == null) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "isErrorPageDefined", "Could not locate custom error page for error code =" + sc);
            }
            return false;
        }
        String errorURL = ep.getLocation();
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "isErrorPageDefined", "Found error-code=" + sc + " with location=" + errorURL);
        }
        return true;
    }

    /**
     * @return
     */
    public List getWelcomeFileList() {
        return this.config.getWelcomeFileList();
    }

    /**
     * @return
     */
    public String getWebAppName() {
        return this.config.getDisplayName();
    }

    /*
     * Used by the following components to add servlets to the Webcontainer
     * runtime: (1) Portal Server (2) WebServices
     */
    public void addDynamicServlet(String servletName, String servletClass, String mappingURI, Properties initParameters) throws ServletException,
            SecurityException {

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.entering(CLASS_NAME, "addDyanamicServlet");
            logger.logp(Level.FINE, CLASS_NAME, "addDynamicServlet", " servletName[" + servletName + "] servletClass [" + servletClass
                    + "] mappingURI [" + mappingURI + "]");
        }

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(perm);
        }
        // make sure the servlet doesn't already exist
        IServletConfig sconfig = config.getServletInfo(servletName);

        if (sconfig == null) {
            // Adding a brand new servlet to the container
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "addDynamicServlet",
                        "Servlet not found in the web application configuration. Creating new config.");
            }
            sconfig = this.webExtensionProcessor.createConfig("DYN_" + servletName + "_" + System.currentTimeMillis()); // PK63920
            sconfig.setServletName(servletName);
            sconfig.setDisplayName(servletName);
            sconfig.setDescription("dynamic servlet " + servletName);
            sconfig.setClassName(servletClass);
            sconfig.setStartUpWeight(new Integer(1));
            sconfig.setServletContext(getFacade());

            // check the parameters
            if (initParameters == null) {
                initParameters = new Properties();
            } else
                sconfig.setInitParams(initParameters);

            // add to the config
            config.addServletInfo(servletName, sconfig);
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "addDynamicServlet", " added servletMapping to config for [" + servletName + "] sconfig [" + sconfig
                    + "]");
        }

        IServletWrapper s = null;
        try {
            s = getServletWrapper(servletName);
        } catch (Exception e) {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, CLASS_NAME + ".addDynamicServlet", "3084", this);
            // pk435011
            logger.logp(Level.SEVERE, CLASS_NAME, "addDynamicServlet", "exception.occured.while.creating.wrapper.for.servlet", new Object[] {
                    servletName, e }); /* 283348.1 */
            throw new ServletException(e);
        }

        if (s == null) {
            // pk435011
            logger.logp(Level.SEVERE, CLASS_NAME, "addDynamicServlet", "could.not.create.wrapper.for.servlet", servletName);
            throw new ServletException("Could not create wrapper for the dynamic servlet " + servletName);
        }

        try {
            sconfig.setServletWrapper(s);
            sconfig.addMapping(ServletConfig.CheckContextInitialized.FALSE, mappingURI);
            config.addServletMapping(servletName, mappingURI);
        } catch (Exception e) {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, CLASS_NAME + ".addDynamicServlet", "3095", this);
            // pk435011
            logger
                    .logp(Level.SEVERE, CLASS_NAME, "addDynamicServlet", "mapping.already.exists",
                            new Object[] { mappingURI, getApplicationName(), e }); /* 283348.1 */
            throw new ServletException(e);
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.exiting(CLASS_NAME, "addDyanamicServlet");
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.websphere.servlet.context.IBMServletContext#addHttpSessionListener
     * (javax.servlet.http.HttpSessionListener)
     */
    public void addHttpSessionListener(HttpSessionListener listener) throws SecurityException {
        this.addHttpSessionListener(listener, true);
    }

    protected void addHttpSessionListener(HttpSessionListener listener, boolean securityCheckNeeded) throws SecurityException {
        if (securityCheckNeeded) {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPermission(perm);
            }
        }
        this.getSessionContext().addHttpSessionListener(listener, name);
    }

    protected void addHttpSessionAttributeListener(HttpSessionAttributeListener listener) throws SecurityException {
        this.getSessionContext().addHttpSessionAttributeListener(listener, name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.websphere.servlet.context.IBMServletContext#fireSessionAttributeAdded
     * (javax.servlet.http.HttpSessionBindingEvent)
     */
    public void fireSessionAttributeAdded(HttpSessionBindingEvent event) {
        this.sessionCtx.sessionAttributeAddedEvent(event);

    }

    /*
     * (non-Javadoc)
     * 
     * @seecom.ibm.websphere.servlet.context.IBMServletContext#
     * fireSessionAttributeRemoved(javax.servlet.http.HttpSessionBindingEvent)
     */
    public void fireSessionAttributeRemoved(HttpSessionBindingEvent event) {
        this.sessionCtx.sessionAttributeRemovedEvent(event);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.servlet.context.IBMServletContext#
     * fireSessionAttributeReplaced(javax.servlet.http.HttpSessionBindingEvent)
     */
    public void fireSessionAttributeReplaced(HttpSessionBindingEvent event) {
        this.sessionCtx.sessionAttributeReplacedEvent(event);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.websphere.servlet.context.IBMServletContext#fireSessionCreated
     * (javax.servlet.http.HttpSessionEvent)
     */
    public void fireSessionCreated(HttpSessionEvent event) {
        this.sessionCtx.sessionCreatedEvent(event);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.websphere.servlet.context.IBMServletContext#fireSessionDestroyed
     * (javax.servlet.http.HttpSessionEvent)
     */
    public void fireSessionDestroyed(HttpSessionEvent event) {
        this.sessionCtx.sessionDestroyedEvent(event);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.websphere.servlet.context.IBMServletContext#getSessionTimeout()
     */
    public int getSessionTimeout() {
        return this.sessionCtx.getSessionTimeOut();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.websphere.servlet.context.IBMServletContext#isSessionTimeoutSet()
     */
    public boolean isSessionTimeoutSet() {
        return this.sessionCtx.isSessionTimeoutSet();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.websphere.servlet.context.IBMServletContext#loadServlet(java.
     * lang.String)
     */
    public void loadServlet(String servletName) throws ServletException, SecurityException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(perm);
        }

        ServletWrapper s;

        try {
            s = (ServletWrapper) getServletWrapper(servletName);
            if (s != null) {
                s.load();
            }
        } catch (Exception e) {
            throw new ServletException("Servlet load failed: " + e.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.websphere.servlet.context.IBMServletContext#removeDynamicServlet
     * (java.lang.String)
     */
    public void removeDynamicServlet(String servletName) throws SecurityException {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "removeDynamicServlet", "remove dynamic servlet for -->" + servletName);
        }
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(perm);
        }
        removeServlet(servletName);
    }

    public boolean removeServlet(String servletName) {
        // make sure the servlet doesn't already exist
        boolean hasDestroyed = false;
        IServletConfig sconfig = config.getServletInfo(servletName);

        if (sconfig == null)
            return hasDestroyed;

        List<String> l = config.getServletMappings(servletName);

        if (l != null) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "removeServlet", "number of servletMappings for -->[" + servletName + "] is -->[" + l.size()
                        + "]");
            }
            Iterator<String> mappings = l.iterator();
            while (mappings.hasNext()) {
                String mapping = mappings.next();
                if (mapping.charAt(0) != '/' && mapping.charAt(0) != '*')
                    mapping = '/' + mapping;
                RequestProcessor p = requestMapper.map(mapping);
                if (p != null) {
                    if (p instanceof ServletWrapper) {
                        // if
                        // (((ServletWrapper)p).getServletName().equals(servletName))
                        // {
                        try {
                            if (!hasDestroyed) {
                                ((ServletWrapper) p).destroy();
                                hasDestroyed = true;
                            }
                            requestMapper.removeMapping(mapping);
                        } catch (Throwable th) {
                            // pk435011
                            logger.logp(Level.WARNING, CLASS_NAME, "removeServlet", "encountered.problems.while.removing.servlet", new Object[] {
                                    servletName, th });
                        }
                    } else {
                        continue;
                    }
                }
            }

            config.removeServletMappings(servletName);
            config.removeServletInfo(servletName);
        } else {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "removeServlet", "no servletMappings for -->[" + servletName + "]");
            }
        }
        return hasDestroyed;
    }

    private void started() {
        //need to only call the WebAppInitializationCollaborators during started - notifyStart is too early for liberty as the app hasn't been fully intialized
        callWebAppInitializationCollaborators(InitializationCollaborCommand.STARTED);
    }

    /**
     *
     */
    //this actually is called multiple times and does not signify that the application has started since we're doing lazy loading in tWAS.
    //use the started method above for any started specific requirements
    public void notifyStart() {
        try {
            eventSource.onApplicationAvailableForService(new ApplicationEvent(this, this, new com.ibm.ws.webcontainer.util.IteratorEnumerator(config.getServletNames())));
            // LIBERTY: next line added by V8 sync
            //this.setInitialized(true);
        } catch (Exception e) {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, CLASS_NAME + ".started", "3220", this);
            // pk435011
            logger.logp(Level.SEVERE, CLASS_NAME, "started", "error.on.collaborator.started.call");
        }
    }

    //method may be overridden to prevent caching of the loginProcessor
    public ExtensionProcessor getLoginProcessor() {
        if (loginProcessor == null)
            loginProcessor = collabHelper.getSecurityCollaborator().getFormLoginExtensionProcessor(this);

        return loginProcessor;
    }

    //method may be overridden to prevent caching of the logoutProcessor
    public ExtensionProcessor getLogoutProcessor() {
        if (logoutProcessor == null)
            logoutProcessor = collabHelper.getSecurityCollaborator().getFormLogoutExtensionProcessor(this);

        return logoutProcessor;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.webcontainer.servlet.IServletContext#
     * registerRequestDispatcherFactory
     * (com.ibm.wsspi.webcontainer.servlet.RequestDispatcherFactory)
     */
    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.wsspi.webcontainer.servlet.IServletContext#removeLifeCycleListener
     * (java.util.EventListener)
     */
    public void removeLifeCycleListener(EventListener listener) {
        if (listener != null) {
            if (listener instanceof javax.servlet.ServletContextListener) {
                // add to the context listener list
                servletContextListeners.remove(listener);
            }
            if (listener instanceof javax.servlet.ServletContextAttributeListener) {
                // add to the context attr listener list
                servletContextLAttrListeners.remove(listener);
            }

            if (listener instanceof javax.servlet.http.HttpSessionListener) {
                // session context currently has no way to remove
                // lifecycle listeners
            }
            
            // For Servlet 3.1 need to ensure we check to see if it is a HttpSessionIdListener
            if (isHttpSessionIdListener(listener)) {
                // session context currently has no way to remove lifecycle listener
            }

            // 2.4 Listeners
            if (listener instanceof javax.servlet.ServletRequestListener) {
                servletRequestListeners.remove(listener);
            }
            if (listener instanceof javax.servlet.ServletRequestAttributeListener) {
                servletRequestLAttrListeners.remove(listener);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.wsspi.webcontainer.servlet.IServletContext#addMappingTarget(java
     * .lang.String, com.ibm.ws.webcontainer.core.RequestProcessor)
     */
    public void addMappingTarget(String mapping, RequestProcessor target) throws Exception {
        this.requestMapper.addMapping(mapping, target);
    }

    public void addMappingFilter(String mapping, com.ibm.websphere.servlet.filter.IFilterConfig config) {
        addMappingFilter(mapping, (com.ibm.wsspi.webcontainer.filter.IFilterConfig) config);
    }

    public void addMappingFilter(String mapping, IFilterConfig config) {
        IFilterMapping fmapping = new FilterMapping(mapping, config, null);
        _addMapingFilter(config, fmapping);
    }

    /**
     * Adds a filter against a specified servlet config into this context
     * 
     * @param sConfig
     * @param config
     */
    public void addMappingFilter(IServletConfig sConfig, IFilterConfig config) {
        IFilterMapping fmapping = new FilterMapping(null, config, sConfig);
        _addMapingFilter(config, fmapping);
    }

    private void _addMapingFilter(IFilterConfig config, IFilterMapping fmapping) {
        fmapping.setDispatchMode(config.getDispatchType());
        // Begin 202490f_4
        this.config.addFilterInfo(fmapping.getFilterConfig());
        this.config.getFilterMappings().add(fmapping);
        // End 202490f_4 this.config
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.wsspi.webcontainer.servlet.IServletContext#getMappingTarget(java
     * .lang.String)
     */
    public RequestProcessor getMappingTarget(String mapping) {
        return this.requestMapper.map(mapping);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.webcontainer.servlet.IServletContext#targets()
     */
    public Iterator targets() {
        return this.requestMapper.targetMappings();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.webcontainer.servlet.IServletContext#getWebAppConfig()
     */
    public WebAppConfig getWebAppConfig() {
        return this.config;
    }

    /**
     *
     */
    public void failed() {
        eventSource.onApplicationUnavailableForService(new ApplicationEvent(this, this, new com.ibm.ws.webcontainer.util.IteratorEnumerator(config
                .getServletNames())));
    }

    public abstract String getServerInfo();

    public void replaceMappingTarget(String mapping, RequestProcessor target) throws Exception {
        this.requestMapper.replaceMapping(mapping, target);
    }

    public IFilterConfig createFilterConfig(String id) {
        FilterConfig fc = new FilterConfig(id, config);
        fc.setServletContext(this);
        return fc;
    }

    public com.ibm.websphere.servlet.filter.IFilterConfig getFilterConfig(String id) {
        return createFilterConfig(id);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.wsspi.webcontainer.servlet.IServletContext#finishEnvSetup(boolean
     * )
     */
    public void finishEnvSetup(boolean transactional) throws Exception {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "finishEnvSetup", "enter");
        }
        // unset the classloader
        UnsynchronizedStack envObjectStack = (UnsynchronizedStack) envObject.get();
        EnvObject env = null;
        if (envObjectStack != null) {
            env = (EnvObject) envObjectStack.pop();
            final ClassLoader origLoader = env.origClassLoader;
            if (origLoader != null) {
                ThreadContextHelper.setClassLoader(origLoader);
            }
            if (envObjectStack.isEmpty()) {
                envObject.remove();
            }
        }

        // namespace postinvoke
        webAppNameSpaceCollab.postInvoke();

        if (transactional && env != null)
            txCollab.postInvoke(null, env.txConfig, this.isServlet23);
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "finishEnvSetup", "exit");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.wsspi.webcontainer.servlet.IServletContext#startEnvSetup(boolean)
     */
    public void startEnvSetup(boolean transactional) throws Exception {
        // set classloader
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "startEnvSetup", "enter");
        }
        ClassLoader origCl = ThreadContextHelper.getContextClassLoader();
        final ClassLoader warClassLoader = getClassLoaderInternal();
        if (warClassLoader != origCl) {
            ThreadContextHelper.setClassLoader(warClassLoader);
        } else {
            origCl = null;
        }
        // createCollaboratorHelper();
        webAppNameSpaceCollab.preInvoke(getModuleMetaData().getCollaboratorComponentMetaData());

        // transaction preinvoke
        Object tx = null;
        if (transactional) {
            tx = txCollab.preInvoke(null, this.isServlet23);
        }
        UnsynchronizedStack envObjectStack = (UnsynchronizedStack) envObject.get();
        if (envObjectStack == null) {
            envObjectStack = new UnsynchronizedStack();
            envObject.set(envObjectStack);
        }
        envObjectStack.push(new EnvObject(origCl, tx));
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "startEnvSetup", "exit");
        }
    }

    // begin defect 293789: add ability for components to register
    // ServletContextFactories
    public void addFeature(com.ibm.websphere.servlet.container.WebContainer.Feature feature) {
        this.features.add(feature);
    }

    // end defect 293789: add ability for components to register
    // ServletContextFactories

    public boolean isFeatureEnabled(com.ibm.websphere.servlet.container.WebContainer.Feature feature) {
        return this.features.contains(feature);
    }

    public ArrayList getServletContextAttrListeners() {
        return this.servletContextLAttrListeners;
    }

    public ArrayList getServletContextListeners() {
        return this.servletContextListeners;
    }

    public ArrayList getServletRequestAttrListeners() {
        return this.servletRequestLAttrListeners;
    }

    public ArrayList getServletRequestListeners() {
        return this.servletRequestListeners;
    }

    // 325429
    public Boolean getDestroyed() {
        return this.destroyed;
    }

    class EnvObject {
        ClassLoader origClassLoader;
        Object txConfig;

        EnvObject(ClassLoader cl, Object tx) {
            this.origClassLoader = cl;
            this.txConfig = tx;
        }
    }

    public ICollaboratorHelper getCollaboratorHelper() {
        return this.collabHelper;
    }

    protected void setCollaboratorHelper(ICollaboratorHelper collab) {
        this.collabHelper = collab;
    }

    protected abstract ICollaboratorHelper createCollaboratorHelper(DeployedModule moduleConfig);

    public boolean isServlet23() {
        return this.isServlet23;
    }

    public int getVersionID() {
        return config.getVersion();
    }

    public abstract com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData getWebAppCmd();

    protected void notifyStop() {
        callWebAppInitializationCollaborators(InitializationCollaborCommand.STOPPING);
    }

    public AsyncRequestDispatcher getAsyncRequestDispatcher(String path) {
        logger.logp(Level.WARNING, CLASS_NAME, "getAsyncRequestDispatcher", "ARD.Not.Enabled");
        return (AsyncRequestDispatcher) getRequestDispatcher(path);
    }

    // PK50133 start
    public void addAndCheckJSPClassLoaderLimit(ServletWrapper sw) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.entering(CLASS_NAME, "addAndCheckJSPClassLoaderLimit");
        }

        ServletWrapper swToUnload = null;
        synchronized (jspClassLoadersMap) {
            swToUnload = jspClassLoadersMap.putSW(sw.getServletName(), sw);
        }

        // PK82657 start - replace linked last code with call to LRU map
        /*
         * synchronized(jspClassLoaders) { if (jspClassLoaders.size() >
         * jspClassLoaderLimit) { swToUnload =
         * (ServletWrapper)jspClassLoaders.getFirst();
         * jspClassLoaders.removeFirst(); jspClassLoaders.addLast(sw); } else {
         * jspClassLoaders.addLast(sw); } }
         */

        if (swToUnload != null) {
            try {
                requestMapper.removeMapping(swToUnload.getServletName());
                swToUnload.unload();
            } catch (Exception e) {
                logger.logp(Level.SEVERE, CLASS_NAME, "addAndCheckJSPClassLoaderLimit", "Exception.occured.during.servlet.unload", e);
            }
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.exiting(CLASS_NAME, "addAndCheckJSPClassLoaderLimit");
        }
    }

    public void setJSPClassLoaderLimit(int i) {
        this.jspClassLoaderLimit = i;
        this.jspClassLoadersMap = new JSPClassLoadersMap(i); // PK82657
    }

    public int getJSPClassLoaderLimit() {
        return jspClassLoaderLimit;
    }

    public void setJSPClassLoaderExclusionList(String s) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "setJSPClassLoaderExclusionList", "value: " + s);
        }

        StringTokenizer st = new StringTokenizer(s, ",");
        while (st.hasMoreElements()) {
            this.jspClassLoaderExclusionList.add(st.nextToken().trim());
        }
    }

    public List getJSPClassLoaderExclusionList() {
        return this.jspClassLoaderExclusionList;
    }

    public Class getJSPClassLoaderClassName() {
        try {
            return Class.forName("com.ibm.ws.jsp.webcontainerext.JSPExtensionClassLoader");
        } catch (java.lang.ClassNotFoundException cnf) {
            return null;
        }
    }

    // PK50133 end

    public boolean isCachingEnabled() {
        return false;
    }

    // PK82657 start
    public void setJSPClassLoaderLimitTrackIF(boolean trackIF) {
        this.jspClassLoaderLimitTrackIF = trackIF;
    }

    public boolean isJSPClassLoaderLimitTrackIF() {
        return jspClassLoaderLimitTrackIF;
    }

    private class JSPClassLoadersMap extends LinkedHashMap {

        // classloader limit as set by jsp attribute JSPClassLoaderLimit.
        int limit;
        ServletWrapper swToUnload;

        public JSPClassLoadersMap(int limit) {
            // call LinkedHashMap constructor and init the limit var.
            // set loadFactor param to 1.1 to ensure no resizing
            // set lastAccessed param to true so we remove the least recently
            // used instead of least recently added.
            super(limit + 1, 1.1f, true);
            this.limit = limit;
        }

        public ServletWrapper putSW(Object index, Object value) {
            this.put(index, value);
            return swToUnload;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry entry) {
            if (size() > limit) {
                // unload the servlet wrapper, then return true to remove the
                // entry from the map
                // unloadSW((ServletWrapper)entry.getValue());
                swToUnload = (ServletWrapper) entry.getValue();
                return true;
            }
            swToUnload = null;
            return false;
        }

    }

    // PK82657 end

    public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
        return commonAddFilter(filterName, null, null, filterClass);
    }

    public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
        return commonAddFilter(filterName, null, filter, null);
    }

    public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, String className) {
        return commonAddFilter(filterName, className, null, null);
    }

    private javax.servlet.FilterRegistration.Dynamic commonAddFilter(String filterName, String className, Filter filter,
            Class<? extends Filter> filterClass) {

        if (initialized) {
            throw new IllegalStateException(liberty_nls.getString("Not.in.servletContextCreated"));
        }
        if (withinContextInitOfProgAddListener) {
            throw new UnsupportedOperationException(MessageFormat.format(
                    nls.getString("Unsupported.op.from.servlet.context.listener"),
                    new Object[] {"addFilter", lastProgAddListenerInitialized, getApplicationName()}));  // PI41941
        }
        
        if (this.config.getFilterInfo(filterName) != null) {
            return null;
        } 

        IFilterConfig filterConfig = createFilterConfig(filterName);
        if (className != null) {
            filterConfig.setFilterClassName(className);
            processDynamicInjectionMetaData(className, null);
        } else if (filter != null) {
            filterConfig.setFilter(filter);
        } else if (filterClass != null) {
            filterConfig.setFilterClass(filterClass);
            processDynamicInjectionMetaData(null, filterClass);
        }

        this.config.addFilterInfo(filterConfig);

        return filterConfig;
    }

    public javax.servlet.ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.entering(CLASS_NAME, "addServlet(String, Class<? extends Servlet>)");

        javax.servlet.ServletRegistration.Dynamic dynamic = commonAddServlet(servletName, null, null, servletClass);

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.exiting(CLASS_NAME, "addServlet(String, Class<? extends Servlet>)");
        return dynamic;
    }

    public javax.servlet.ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.entering(CLASS_NAME, "addServlet(String, Servlet)");

        javax.servlet.ServletRegistration.Dynamic dynamic = commonAddServlet(servletName, null, servlet, null);

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.exiting(CLASS_NAME, "addServlet(String, Servlet)");

        return dynamic;
    }

    public javax.servlet.ServletRegistration.Dynamic addServlet(String servletName, String className) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))

            logger.entering(CLASS_NAME, "addServlet(String, String)");

        javax.servlet.ServletRegistration.Dynamic dynamic = commonAddServlet(servletName, className, null, null);

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.exiting(CLASS_NAME, "addServlet(String, String)");
        return dynamic;
    }

    public javax.servlet.ServletRegistration.Dynamic commonAddServlet(String servletName, String className, Servlet servlet,
            Class<? extends Servlet> servletClass) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.entering(CLASS_NAME, "commonAddServlet");

        // make sure the servlet doesn't already exist
        IServletConfig sconfig = config.getServletInfo(servletName);

        if (initialized) {
            throw new IllegalStateException(liberty_nls.getString("Not.in.servletContextCreated"));
        } 
        if (withinContextInitOfProgAddListener) {
            throw new UnsupportedOperationException(MessageFormat.format(
                    nls.getString("Unsupported.op.from.servlet.context.listener"),
                    new Object[] {"addServlet", lastProgAddListenerInitialized, getApplicationName()}));  // PI41941
        } 
        
        if (sconfig == null) {
            // Adding a brand new servlet to the container
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger
                        .logp(Level.FINE, CLASS_NAME, "commonAddServlet",
                                "Servlet name not found in the web application configuration. Creating new config.");
            }

            if (servlet != null && isExistingServletWithSameInstance(servlet)) {
                logger.logp(Level.SEVERE, CLASS_NAME, "commonAddServlet", "servlet.with.same.object.instance.already.exists");
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                    logger.exiting(CLASS_NAME, "commonAddServlet");
                return null;
            }

            try {
                sconfig = this.webExtensionProcessor.createConfig("DYN_" + servletName + "_" + System.currentTimeMillis());

                sconfig.setServletName(servletName);
                sconfig.setClassName(className);
                sconfig.setServletClass(servletClass);
                sconfig.setServlet(servlet);
                sconfig.setServletContext(getFacade());

                // add to the config
                config.addServletInfo(servletName, sconfig);
                config.addDynamicServletRegistration(servletName, sconfig);
                IServletWrapper s = null;
                try {
                    s = getServletWrapper(servletName);
                } catch (Exception e) {
                    com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, CLASS_NAME + ".addDynamicServlet", "3084", this);
                    // pk435011

                    throw new ServletException(e);
                }

                s.setTarget(servlet);
                sconfig.setServletWrapper(s);

               
                //if servlet instance is null, we need to add this class to the list of classes to pass to the injection engine since we will create the instance later
                if (servlet==null) {
                        Class clazz = servletClass;
                        if (clazz==null) {
                                if (className!=null){
                                        try {
                                                                clazz = Class.forName(className, true, this.getClassLoader());
                                                                loadAnnotationsForProgrammaticServlets(clazz,sconfig);
                                                        } catch (ClassNotFoundException e) {
                                                                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                                                                        logger.logp(Level.FINE, CLASS_NAME, "commonAddServlet", "exception.occured.while.loading.class.for.servlet", new Object[] { servletName, e });
                                                        }
                                }
                        } else {
                                loadAnnotationsForProgrammaticServlets(clazz,sconfig);
                        }
                } else {
                    //handles caching for a servlet already created
                    s.modifyTarget(servlet); // to handle SingleThreadModel & caching
                }
            } catch (ServletException e) {
                FFDCFilter.processException(e, this.getClass().getName() + ".commonAddServlet", "5500");
            } // PK63920
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                logger.exiting(CLASS_NAME, "commonAddServlet");
            return sconfig;
        } else {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                logger.logp(Level.FINER, CLASS_NAME, "commonAddServlet", "named sconfig already exists->" + sconfig);
            if (!sconfig.isClassDefined()) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                    logger.logp(Level.FINER, CLASS_NAME, "commonAddServlet", "class not defined so setting class");
                sconfig.setClassName(className);
                sconfig.setServletClass(servletClass);
                sconfig.setServlet(servlet);
                config.addDynamicServletRegistration(servletName, sconfig);
                return sconfig;
            }
            logger.logp(Level.SEVERE, CLASS_NAME, "commonAddServlet", "servlet.with.same.name.already.exists",  new Object[] {servletName});
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                logger.exiting(CLASS_NAME, "addServlet");
            return null;
        }

    }

    private void loadAnnotationsForProgrammaticServlets(final Class clazz,
                        IServletConfig sconfig) {
        RunAs runAsAnnotation = (RunAs) clazz.getAnnotation(RunAs.class);
        if (runAsAnnotation!=null)
                sconfig.setRunAsRole(runAsAnnotation.value());
        
        checkForServletSecurityAnnotation(clazz, sconfig);
        SecurityManager sm = System.getSecurityManager();
        if (sm == null) {
            processDynamicInjectionMetaData(null,  clazz);
        } else {
            AccessController.doPrivileged(new PrivilegedAction(){

                @Override
                public Object run() {
                    processDynamicInjectionMetaData(null,  clazz);
                    return null;
                }
            });
        }

        MultipartConfig multipartConfig = (MultipartConfig) clazz.getAnnotation(MultipartConfig.class);
        if (multipartConfig!=null){
                MultipartConfigElement multipartConfigElement = new MultipartConfigElement(multipartConfig);
                sconfig.setMultipartConfig(multipartConfigElement);
        }   
        
        
        }

        private void checkForServletSecurityAnnotation(Class clazz,
                        IServletConfig sconfig) {
        ServletSecurity servletSecurityAnnotation = (ServletSecurity) clazz.getAnnotation(ServletSecurity.class);
        if (servletSecurityAnnotation!=null)
        {
            ServletSecurityElement servletSecurity = new ServletSecurityElement(servletSecurityAnnotation);
            sconfig.setServletSecurity(servletSecurity);
        }
    }

        public void commonAddListener(String listenerClassName, EventListener listener,
            Class<? extends EventListener> listenerClass) {

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.entering(CLASS_NAME, "addListener");
        if (initialized) {
            throw new IllegalStateException(liberty_nls.getString("Not.in.servletContextCreated"));
        }
        if (withinContextInitOfProgAddListener) {
            throw new UnsupportedOperationException(MessageFormat.format(
                    nls.getString("Unsupported.op.from.servlet.context.listener"),
                    new Object[] {"addListener", lastProgAddListenerInitialized, getApplicationName()}));  // PI41941
        }
        if (com.ibm.ws.webcontainer.osgi.WebContainer.isServerStopping()) {
            return;
        }
        Class className = listenerClass;
        try {
            if (listenerClassName != null) {
                className = Class.forName(listenerClassName, true, this.getClassLoader());
            } else if (listener != null) {
                className = listener.getClass();
            }
            Class[] validListenerClasses = new Class[6];
            validListenerClasses[0] = javax.servlet.ServletContextAttributeListener.class;
            validListenerClasses[1] = javax.servlet.ServletRequestListener.class;
            validListenerClasses[2] = javax.servlet.ServletRequestAttributeListener.class;
            validListenerClasses[3] = javax.servlet.http.HttpSessionListener.class;
            validListenerClasses[4] = javax.servlet.http.HttpSessionAttributeListener.class;

                //if this was called from ServletContainerInitializer#onStartup, then the listener can implement ServletContextListener
            if (canAddServletContextListener) {
                validListenerClasses[5] = javax.servlet.ServletContextListener.class;
            } else {
                if ((javax.servlet.ServletContextListener.class).isAssignableFrom(className)) {
                    throw new IllegalArgumentException(nls.getString("Error.adding.ServletContextListener"));
                }
            }
            boolean valid = false;
            Set classesSet = new HashSet();
            for (Class c : validListenerClasses) {
                if (c != null && c.isAssignableFrom(className)) {
                    valid = true;
                    classesSet.add(c);
                }
            }
            if (!valid) {
                throw new IllegalArgumentException(nls.getString("Invalid.Listener"));
            }

            if (listener == null) {
                //processDynamicInjectionMetaData(null, className);

                // Class or className was passed, need to create an instance
                // Injection done here
                try {
                    listener = createListener(className);
                } catch (Exception e) {
                        logger.logp(Level.SEVERE, CLASS_NAME, "commonAddListener", "exception.occurred.while.creating.listener.instance", new Object[] {
                                className, e });
                }
            }
            // add it to the end of the ordered list of listeners
            if (classesSet.contains(javax.servlet.ServletContextAttributeListener.class)) {
                this.servletContextLAttrListeners.add(listener);
            }
            if (classesSet.contains(javax.servlet.ServletRequestListener.class)) {
                this.servletRequestListeners.add(listener);
            }
            if (classesSet.contains(javax.servlet.ServletRequestAttributeListener.class)) {
                this.servletRequestLAttrListeners.add(listener);
            }
            if (classesSet.contains(javax.servlet.http.HttpSessionListener.class)) {
                    this.sessionListeners.add(listener);//add to this list in case we need to do a preDestroy
                this.addHttpSessionListener((HttpSessionListener) listener, false);
            }
            if (classesSet.contains(javax.servlet.http.HttpSessionAttributeListener.class)) {
                        sessionAttrListeners.add(listener);//add to this list in case we need to do a preDestroy
                this.addHttpSessionAttributeListener((HttpSessionAttributeListener) listener);
            }
            if (classesSet.contains(javax.servlet.ServletContextListener.class)) {
                addServletContextListener(listener);
            }
        } catch (ClassNotFoundException e) {
            // This method can be called from addListener(String) which calls this method with a null
            // value for listenerClass, in turn if a ClassNotFoundException occurs the className is also null 
            // as className is set to = listenerClass before calling Class.forName which can throw the ClassNotFoundException.
            // Due to className having the possibility of being null we can't log className.getName() as an NPE will occur. Also
            // we should be logging listenerClassName as that is the listenerClassName that could not be found.
            logger.logp(Level.SEVERE, CLASS_NAME, "commonAddListener", 
                        "exception.occurred.while.adding.listener", new Object[] {listenerClassName});
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.exiting(CLASS_NAME, "addListener");
    }

    private boolean isExistingServletWithSameInstance(Servlet servletToAdd) {
        Iterator<IServletConfig> servletInfoIterator = config.getServletInfos();
        while (servletInfoIterator.hasNext()) {
            IServletConfig curConfig = servletInfoIterator.next();
            if (curConfig.getServlet() == servletToAdd) {
                return true;
            }
        }
        return false;
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> c) throws ServletException {
        // injection done in WebAppImpl
        if (withinContextInitOfProgAddListener) {
            throw new UnsupportedOperationException(MessageFormat.format(
                    nls.getString("Unsupported.op.from.servlet.context.listener"),
                    new Object[] {"createFilter", lastProgAddListenerInitialized, getApplicationName()}));  // PI41941
        }
        return this.createAsManageObject(c);
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> c) throws ServletException {
        // injection done in WebAppImpl
        if (withinContextInitOfProgAddListener) {
            throw new UnsupportedOperationException(MessageFormat.format(
                    nls.getString("Unsupported.op.from.servlet.context.listener"),
                    new Object[] {"createServlet", lastProgAddListenerInitialized, getApplicationName()}));  // PI41941
        }
        return this.createAsManageObject(c);
    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        if (withinContextInitOfProgAddListener) {
            throw new UnsupportedOperationException(MessageFormat.format(
                    nls.getString("Unsupported.op.from.servlet.context.listener"),
                    new Object[] {"getDefaultSessionTrackingModes", lastProgAddListenerInitialized, getApplicationName()}));  // PI41941
        }
        return config.getDefaultSessionTrackingMode();
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        if (withinContextInitOfProgAddListener) {
            throw new UnsupportedOperationException(MessageFormat.format(
                    nls.getString("Unsupported.op.from.servlet.context.listener"),
                    new Object[] {"getEffectiveSessionTrackingModes", lastProgAddListenerInitialized, getApplicationName()}));  // PI41941
        }
        return config.getSessionTrackingMode();
    }

    @Override
    public int getEffectiveMajorVersion() throws UnsupportedOperationException {
      if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE) == true)
      {
            logger.logp(Level.FINE, CLASS_NAME, "getEffectiveMajorVersion", "effectiveMajorVersion->" + effectiveMajorVersion);
        }
        return this.effectiveMajorVersion;
    }

    @Override
    public int getEffectiveMinorVersion() throws UnsupportedOperationException {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE) == true)
        {
            logger.logp(Level.FINE, CLASS_NAME, "getEffectiveMinorVersion", "effectiveMinorVersion->" + effectiveMinorVersion);
        }
        return this.effectiveMinorVersion;
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        if (withinContextInitOfProgAddListener) {
            throw new UnsupportedOperationException(MessageFormat.format(
                    nls.getString("Unsupported.op.from.servlet.context.listener"),
                    new Object[] {"getSessionCookieConfig", lastProgAddListenerInitialized, getApplicationName()}));  // PI41941
        }
        /*if (!config.getSessionManagerConfigBase().isAllowProgrammaticConfigurationSupport()) {
            //throw runtime exception 'cause you can't do programmatic config on the base object
            throw new RuntimeException(nls.getString("programmatic.sessions.disabled"));
        }*/
        return config.getSessionCookieConfig();
    }

    @Override
    public boolean setInitParameter(String name, String value) throws IllegalStateException, IllegalArgumentException {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE) == true)
        {
            logger.logp(Level.FINE, CLASS_NAME, "setInitParameter", "name->" + name + "value->" + value);
            logger.logp(Level.FINE, CLASS_NAME, "setInitParameter", "initialized->" + initialized + "withinContextInitOfProgAddListener->" + withinContextInitOfProgAddListener);
        }

        if (initialized)
            throw new IllegalStateException(liberty_nls.getString("Not.in.servletContextCreated"));

        if (withinContextInitOfProgAddListener) {
            throw new UnsupportedOperationException(MessageFormat.format(
                    nls.getString("Unsupported.op.from.servlet.context.listener"),
                    new Object[] {"setInitParameter", lastProgAddListenerInitialized, getApplicationName()}));  // PI41941
        }

        HashMap ctxParams = this.config.getContextParams();
        if (ctxParams.containsKey(name)) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE) == true)
            {
                logger.logp(Level.FINE, CLASS_NAME, "setInitParameter", "ignoring init parameter with same key as another entry");
            }
            return false;
        }
        else
            ctxParams.put(name, value);

        return true;
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) throws IllegalStateException {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.entering(CLASS_NAME, "setSessionTrackingModes(Set<SessionTrackingMode>)");
        }
        if (initialized) {
            throw new IllegalStateException(nls.getString("programmatic.sessions.already.been.initialized"));
        }
        if (withinContextInitOfProgAddListener) {
            throw new UnsupportedOperationException(MessageFormat.format(
                    nls.getString("Unsupported.op.from.servlet.context.listener"),
                    new Object[] {"setSessionTrackingModes", lastProgAddListenerInitialized, getApplicationName()}));  // PI41941
        }
        if (sessionTrackingModes == null) {
            sessionTrackingModes = EnumSet.noneOf(SessionTrackingMode.class);
        }
        // TODO: FIGURE OUT WHAT TO DO HERE. SPEC DIFFERS FROM OUR IMPL
        if ((sessionTrackingModes.contains(SessionTrackingMode.SSL)) && (sessionTrackingModes.size() > 1)) {
            //our default SSL implementation allows for SSL with either cookies or url rewriting and just stores the id SESSIONMANAGEMENTAFFINI
            throw new IllegalArgumentException("When setting the session tracking modes to SSL, you must not include another tracking mode.");
        }
        // I think we need to add the following WebSphere specific behavior
        // if (sessionTrackingModes.contains(SessionTrackingMode.SSL)) {
        // sessionTrackingModes.add(SessionTrackingMode.COOKIE);
        // }

        //throw runtime exception 'cause you can't do programmatic config on the base object
        /*if (!config.getSessionManagerConfigBase().isAllowProgrammaticConfigurationSupport()) {
            throw new RuntimeException(nls.getString("programmatic.sessions.disabled"));
        }*/

        config.setSessionTrackingMode(sessionTrackingModes);
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.exiting(CLASS_NAME, "setSessionTrackingModes(Set<SessionTrackingMode>)");
        }
    }

    public boolean containsTargetMapping(String mapping) {
        return this.requestMapper.exists(mapping);
    }

    public void finishedFiltersWithNullTarget(ServletRequest request, ServletResponse response, RequestProcessor requestProcessor) throws NoTargetForURIException {

        // a filter could potentially send on a ServletRequest instead of
        // HttpServletRequest
        HttpServletRequest httpRequest = (HttpServletRequest) ServletUtil.unwrapRequest(request, HttpServletRequest.class);
        HttpServletResponse httpResponse = (HttpServletResponse) ServletUtil.unwrapResponse(response, HttpServletResponse.class);

        String requestURI = httpRequest.getRequestURI();
        // WAS does this instead of the above:
        /*
         * IExtendedRequest iExtendedRequest = ServletUtil.unwrapRequest(request);
         * HttpServletResponse httpResponse = (HttpServletResponse)
         * ServletUtil.unwrapResponse(response, HttpServletResponse.class); String
         * relativeURI =
         * iExtendedRequest.getWebAppDispatcherContext().getRelativeUri();
         */

        NoTargetForURIException nt = new NoTargetForURIException(requestURI);
        WebAppErrorReport r = new WebAppErrorReport(nt);
        if (requestProcessor != null && requestProcessor instanceof ServletWrapper) {
            r.setTargetServletName(((ServletWrapper) requestProcessor).getServletName());
        } else {
            r.setTargetServletName(requestURI);
        }
        r.setErrorCode(HttpServletResponse.SC_NOT_FOUND);

        this.sendError(httpRequest, httpResponse, r);
    }

    // LIBERTY-specific method
    @SuppressWarnings("unchecked")
    public void addExtensionFactory(ExtensionFactory factory) {
        synchronized (lock) {
            if (initialized) {
                if (null == extensionFactories) {
                    extensionFactories = new ArrayList();
                }
                if(!extensionFactories.contains(factory)){
                extensionFactories.add(factory);
                }
                initializeExtensionProcessors(extensionFactories);
            } else {
                if(!extensionFactories.contains(factory)){
                extensionFactories.add(factory);
            }
        }
    }
    }
    
    // LIBERTY-specific method
    @SuppressWarnings("unchecked")
    public void removeExtensionFactory(ExtensionFactory factory) {
        synchronized (lock) {
            if (initialized && !destroyed) {
                if (null == extensionFactories) {
                    extensionFactories = new ArrayList();
                }
                extensionFactories.remove(factory);
                initializeExtensionProcessors(extensionFactories);
            } else {
                if (extensionFactories!=null) {
                    extensionFactories.remove(factory);
                }
            }
        }
    } 

    // LIBERTY: New V8 stuff added from here
    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
        //make sure session config is up to date with session cookie & set session cookie initialized

        // LIBERTY: cope with session not being present
        // this.config.getSessionCookieConfig().setContextInitialized();
        SessionCookieConfigImpl scci = this.config.getSessionCookieConfig();
        if (scci != null)
            scci.setContextInitialized();
    }

    @Override
    public void addListener(Class<? extends EventListener> listenerClass) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.entering(CLASS_NAME, "addListener(Class<? extends EventListener>)");

        commonAddListener(null, null, listenerClass);

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.exiting(CLASS_NAME, "addListener(Class<? extends EventListener>)");
    }

    @Override
    public void addListener(String listenerClassName) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.entering(CLASS_NAME, "addListener(String)");

        commonAddListener(listenerClassName, null, null);

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.exiting(CLASS_NAME, "addListener(String)");
    }

    @Override
    public <T extends EventListener> void addListener(T listener) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.entering(CLASS_NAME, "<T extends EventListener> addListener(T)");

        commonAddListener(null, listener, null);

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.exiting(CLASS_NAME, "<T extends EventListener> addListener(T)");
    }

    @Override
    public <T extends EventListener> T createListener(Class<T> listener) throws ServletException {
        // injection done in WebAppImpl
        if (withinContextInitOfProgAddListener) {
            throw new UnsupportedOperationException(MessageFormat.format(
                    nls.getString("Unsupported.op.from.servlet.context.listener"),
                    new Object[] {"createListener", lastProgAddListenerInitialized, getApplicationName()}));  // PI41941
        }
        return this.createAsManageObject(listener);
    }

    protected void processDynamicInjectionMetaData(String className, Class<?> klass) {
        try {
            processDynamicInjectionMetaData(klass != null ? klass : loader.loadClass(className));
        } catch (Throwable t) {
            // ??? The dynamic add methods don't allow an exception to be thrown.
            if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                LoggingUtil.logParamsAndException(logger, Level.FINE, CLASS_NAME, "processDynamicInjectionMetaData",
                                                  "failed to process dynamic injection metadata",
                                                  new Object[] { klass != null ? klass : className }, t);
            }
        }
    }

    protected abstract void processDynamicInjectionMetaData(Class<?> klass) throws InjectionException;

    @Override
    public FilterRegistration getFilterRegistration(String arg0) {
        return config.getFilterInfo(arg0);
    }

    @Override
    public Map<String, FilterRegistration> getFilterRegistrations() {
        return Collections.unmodifiableMap(config.getFilterInfoMap());
    }

    @Override
    public ServletRegistration getServletRegistration(String servletName) {
        IServletWrapper servletWrapper;
        try {
            servletWrapper = getServletWrapper(servletName);
        } catch (Exception e) {
            return null;
        }

        if (servletWrapper != null)
            return servletWrapper.getServletConfig();
        else
            return null;
    }

    @Override
    public Map<String, ServletRegistration> getServletRegistrations() {
        return Collections.unmodifiableMap(config.getServletInfoMap());
    }

    /* No longer used ... have to use getSessionCookieConfig and set the values on that
    @Override
    public void setSessionCookieConfig(SessionCookieConfig arg0) {
        // TODO Auto-generated method stub

    }*/

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        if (withinContextInitOfProgAddListener) {
            throw new UnsupportedOperationException(MessageFormat.format(
                    nls.getString("Unsupported.op.from.servlet.context.listener"),
                    new Object[] {"getJspConfigDescriptor", lastProgAddListenerInitialized, getApplicationName()}));  // PI41941
        }
        JspConfigDescriptorImpl jspConfigDescriptor = new JspConfigDescriptorImpl(this);
        if (jspConfigDescriptor.getJspPropertyGroups().isEmpty() && jspConfigDescriptor.getTaglibs().isEmpty()) {
            return null;
        }
        return jspConfigDescriptor;
    }

    @Override
    public void declareRoles(String... arg0) {
    // TODO Auto-generated method stub

    }

    public Map<String, ? extends ServletRegistration.Dynamic> getDynamicServletRegistrations() {
        return this.config.getDynamicServletRegistrations();
    }

        /*private ClassLoader getWebInfLibClassloader() {
                ClassLoader tempWebInfLibClassloader = webInfLibClassloader;
            if (tempWebInfLibClassloader == null) { 
                synchronized(this) {
                        tempWebInfLibClassloader = webInfLibClassloader;
                    if (tempWebInfLibClassloader == null) 
                    {
                        URL url;
                                        try {
                                                File libFile = new File(getRealPath("/WEB-INF/lib"));
                                                url = libFile.toURI().toURL();
                                                tempWebInfLibClassloader = webInfLibClassloader = new URLClassLoader(new URL [] {url});
                                        } catch (MalformedURLException e) {
                                                // TODO Auto-generated catch block
                                                e.printStackTrace();
                                        }
                    }
                }
            }
             return tempWebInfLibClassloader;
        }*/

    // LIBERTY Added for delayed start.
    public void setModuleConfig(DeployedModule moduleConfig) {
        this.moduleConfig = moduleConfig;
    }

    // LIBERTY Added for delayed start.
    public void setExtensionFactories(List extensionFactories) {
        this.extensionFactories = extensionFactories;
    }
   
    // LIBERTY Added for delayed start.
    public void initialize() throws ServletException, Throwable {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.entering(CLASS_NAME, "Initialize : app = " + config.getApplicationName() + ", initialized = " + initialized + ", destroyed = " + destroyed + ", initializing = " + isInitializing );
        
        if (isInitializing)
            throw new java.lang.IllegalStateException("application is starting, can't initialize it now.");
        
        if (!initialized && !destroyed) {
            synchronized (lock) {
                if (!initialized && !destroyed &&!com.ibm.ws.webcontainer.osgi.WebContainer.isServerStopping()) {
                    try {
                        isInitializing = true; 
                        initialize(this.config, this.moduleConfig, this.extensionFactories);
                        started();
                        initialized = true;
                        config.setSessionCookieConfigInitilialized();
                    }
                    finally {
                        isInitializing = false; 
                    }
                }
            }
        }
               
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.exiting(CLASS_NAME, "Initialize : initialized = " + initialized + ", destroyed = " + destroyed);

    }

    public boolean isSecurityEnabledForApplication(){
        //since security enabled/disabled will restart the apps (is this only for the feature set or for app security too???)
        if (securityEnabledForApp==null) {
            boolean securityEnabled = ((CollaboratorHelperImpl)this.getCollaboratorHelper()).isSecurityEnabled();
            securityEnabledForApp = Boolean.valueOf(securityEnabled);
        }
        return securityEnabledForApp.booleanValue();
    }
    
    // LIBERTY add to access the Adaptable API Container
    public com.ibm.wsspi.adaptable.module.Container getModuleContainer(){
        return container;
    }
    
    // LIBERTY add to set the Adaptable API Container
    public void setModuleContainer(com.ibm.wsspi.adaptable.module.Container c){
        container = c;
        metaInfResourceFinder = new MetaInfResourceFinder(container);
    }   
    
    /**
     * @return the metaInfResourceFinder
     */
    public MetaInfResourceFinder getMetaInfResourceFinder() {
        return metaInfResourceFinder;
    }
    
    protected Entry findResourceInModule(String path){        
        return metaInfResourceFinder.findResourceInModule(path);
    }   
    
    protected void addResourcePaths(Set set, com.ibm.wsspi.adaptable.module.Container c, String path, boolean searchMetaInf) throws UnableToAdaptException {
        //Do not call getEntry on /
        if(path.equals("/")){
            addAllEntries(set, c);
        }
        else {
            //Search for entries with a given path inside the root container
            Entry entry = c.getEntry(path);
            if (entry != null) {
                com.ibm.wsspi.adaptable.module.Container directory = entry.adapt(com.ibm.wsspi.adaptable.module.Container.class);
                //if this entry is a container, and that container is not a nested archive, add it with a trailing /, and add it's contents.
                if (directory != null && !directory.isRoot()) {
                    //add path for directory with a trailing / as required by ServletContext api.
                    set.add(entry.getPath()+"/");
                    addAllEntries(set, directory);
                }else{
                    set.add(entry.getPath());
                }
                
            }
        }        

        //Look in WEB-INF/lib jars as well.
        //paths added to the set for jars need to strip the /META-INF/resources part..
        if (searchMetaInf) { 
            for(com.ibm.wsspi.adaptable.module.Container jar: metaInfResourceFinder.getJarResourceContainers()){
                if(jar!=null && jar.isRoot()){
                    Entry jarResource = jar.getEntry("/META-INF/resources/" + path);
                    if(jarResource!=null){
                        com.ibm.wsspi.adaptable.module.Container resourceDir = jarResource.adapt(com.ibm.wsspi.adaptable.module.Container.class);
                        if(resourceDir!=null && !resourceDir.isRoot()){
                            set.add(jarResource.getPath().substring("/META-INF/resources".length())+"/");
                            for(Entry entry : resourceDir){
                                String jarrespath = entry.getPath();
                                com.ibm.wsspi.adaptable.module.Container possibleContainer = entry.adapt(com.ibm.wsspi.adaptable.module.Container.class);
                                //If this container appears to be a directory then we need to add / to the path
                                //If this container is a nested archive, then we add it as-is.
                                if(possibleContainer != null && !possibleContainer.isRoot()) { 
                                    jarrespath = jarrespath + "/";
                                }
                                set.add(jarrespath.substring("/META-INF/resources".length()));
                            }
                        }else{
                            set.add(jarResource.getPath().substring("/META-INF/resources".length()));
                        }
                    }
                }
            }
        }
        //Remove the requested path from the Set if there was a matching entry
        set.remove(path);
        set.remove(path + "/");
        
    }
    
    /**
     * Add all entry paths from the Container into the Set
     */
    private void addAllEntries(Set s, com.ibm.wsspi.adaptable.module.Container dir) throws UnableToAdaptException {
        for(Entry entry : dir){
            String path = entry.getPath();
            com.ibm.wsspi.adaptable.module.Container possibleContainer = entry.adapt(com.ibm.wsspi.adaptable.module.Container.class);
            //If this container appears to be a directory then we need to add / to the path
            //If this container is a nested archive, then we add it as-is.
            if(possibleContainer != null && !possibleContainer.isRoot()) { 
                path = path + "/";
            }
            s.add(path);
        }
    }
        
    protected abstract <T> T createAsManageObject(Class<?> Klass) throws ServletException;

    
    public void performPreDestroy(Object target) throws InjectionException {
        //No-op ... code in osgi.WebApp
    }
    
    /*
     * Return true if the EventListener is an instaceof HttpSessionIdListener
     */
    protected abstract boolean isHttpSessionIdListener(Object listener);
    
    /*
     * If the listener is an HttpSessionIdListener then it can be added. This method only 
     * does anything useful in the WebApp31.
     */
    protected abstract void checkForSessionIdListenerAndAdd(Object listener);
    
}
