/*******************************************************************************
 * Copyright (c) 1997, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.servlet.event.ApplicationListener;
import com.ibm.websphere.servlet.event.FilterErrorListener;
import com.ibm.websphere.servlet.event.FilterInvocationListener;
import com.ibm.websphere.servlet.event.FilterListener;
import com.ibm.websphere.servlet.event.ServletErrorListener;
import com.ibm.websphere.servlet.event.ServletInvocationListener;
import com.ibm.websphere.servlet.event.ServletListener;
import com.ibm.websphere.servlet.request.IRequest;
import com.ibm.websphere.servlet.response.IResponse;
import com.ibm.websphere.servlet.response.ResponseUtils;
import com.ibm.ws.container.Container;
import com.ibm.ws.container.DeployedModule;
import com.ibm.ws.javaee.dd.webext.WebExt;
import com.ibm.websphere.security.audit.context.AuditManager;
import com.ibm.ws.util.WSThreadLocal;
import com.ibm.ws.webcontainer.async.AsyncContextFactory;
import com.ibm.ws.webcontainer.async.AsyncContextImpl;
import com.ibm.ws.webcontainer.core.BaseContainer;
import com.ibm.ws.webcontainer.exception.WebAppHostNotFoundException;
import com.ibm.ws.webcontainer.exception.WebAppNotLoadedException;
import com.ibm.ws.webcontainer.exception.WebContainerException;
import com.ibm.ws.webcontainer.exception.WebGroupVHostNotFoundException;
import com.ibm.ws.webcontainer.osgi.collaborator.CollaboratorServiceImpl;
import com.ibm.ws.webcontainer.servlet.CacheServletWrapper;
import com.ibm.ws.webcontainer.servlet.CacheServletWrapperFactory;
import com.ibm.ws.webcontainer.session.IHttpSessionContext;
import com.ibm.ws.webcontainer.spi.servlet.http.IHttpServletResponseListener;
import com.ibm.ws.webcontainer.srt.SRTConnectionContext;
import com.ibm.ws.webcontainer.srt.SRTServletRequest;
import com.ibm.ws.webcontainer.srt.SRTServletResponse;
import com.ibm.ws.webcontainer.util.InvalidCacheTargetException;
import com.ibm.ws.webcontainer.util.VirtualHostMapper;
import com.ibm.ws.webcontainer.util.WSURLDecoder;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.ws.webcontainer.webapp.WebAppDispatcherContext;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.webcontainer.IPlatformHelper;
import com.ibm.wsspi.webcontainer.RequestProcessor;
import com.ibm.wsspi.webcontainer.WCCustomProperties;
import com.ibm.wsspi.webcontainer.WebContainerRequestState;
import com.ibm.wsspi.webcontainer.collaborator.IWebAppSecurityCollaborator;
import com.ibm.wsspi.webcontainer.extension.ExtensionFactory;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.security.SecurityViolationException;
import com.ibm.wsspi.webcontainer.servlet.AsyncContext;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;
import com.ibm.wsspi.webcontainer.servlet.IExtendedResponse;
import com.ibm.wsspi.webcontainer.servlet.IServletWrapper;
import com.ibm.wsspi.webcontainer.util.ThreadContextHelper;
import com.ibm.wsspi.webcontainer.util.URIMatcherFactory;

/**
 * Container that handles incoming HTTP requests
 */
@SuppressWarnings("unchecked")
public abstract class WebContainer extends BaseContainer {
    protected static final String ISO = "ISO-8859-1";

    protected String encoding = null;
    protected boolean decode = true;
    protected WebContainerConfiguration wcconfig;
    protected static List applicationListeners = new ArrayList();
    protected static List servletListeners = new ArrayList();
    protected static List servletErrorListeners = new ArrayList();
    protected static List servletInvocationListeners = new ArrayList();
    // LIDB-3598: begin
    protected static List filterInvocationListeners = new ArrayList();
    // 292460: begin resolve issues concerning LIDB-3598 WASCC.web.webcontainer
    protected static List filterErrorListeners = new ArrayList();
    protected static List filterListeners = new ArrayList();
    // 292460: end resolve issues concerning LIDB-3598 WASCC.web.webcontainer
    // LIDB-3598: end

    // CODE REVIEW START
    private static List servletRequestListeners = new ArrayList();
    private static List servletRequestAttributeListeners = new ArrayList();
    private static List servletContextListeners = new ArrayList();
    private static List servletContextAttributeListeners = new ArrayList();
    private static List sessionListeners = new ArrayList();
    private static List sessionIdListeners = new ArrayList();//servlet3.1
    private static List sessionAttributeListeners = new ArrayList();
    // CODE REVIEW END

    public static final String DEFAULT_HOST = "default_host";
    protected static boolean _initialized;
    protected static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer");
    private static final String CLASS_NAME = "com.ibm.ws.webcontainer.internal.WebContainer";

    private static TraceNLS nls = TraceNLS.getTraceNLS(WebContainer.class, "com.ibm.ws.webcontainer.resources.Messages");

    protected static final AtomicReference<WebContainer> self = new AtomicReference<WebContainer>();
    protected static volatile CountDownLatch selfInit = new CountDownLatch(1);

    // 112102 - add HashMap to hold cipher suite to bit size mapping
    private HashMap _cipherToBit = new HashMap();

    private static WSThreadLocal cacheKeyStringBuilder = new WSThreadLocal();

    private static boolean listenersInitialized = false;

    protected static Properties webConProperties = new Properties();

    protected SessionRegistry sessionRegistry;

    protected boolean isStopped = false; // 582053

    protected ReentrantReadWriteLock readWriteLockForStopping = new ReentrantReadWriteLock();
    public final static String urlPrefix = ";jsessionid=";

    public static List<ExtensionFactory> extensionFactories = new ArrayList<ExtensionFactory>();

    public static List<ExtensionFactory> postInitExtensionFactories;

    private static ArrayList httpResponseListeners = new ArrayList();

    private static ServiceLoader<ServletContainerInitializer> servletContainerInitializers;
    
    protected AuditManager auditManager;

    private static int invocationCacheSize = 500;
    static {
        servletContainerInitializers = ServiceLoader.load(ServletContainerInitializer.class, WebContainer.class.getClassLoader());
        String invocationCacheSizeStr = (String) AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                return System.getProperty("invocationCacheSize");
            }
        });
        if (invocationCacheSizeStr != null) {
            try {
                //In previous versions, invocation cache size was per thread. Therefore,
                // we multiply the property by the (old) default number of threads. It
                // used to be 10 threads, but now it is 50.
                invocationCacheSize = Integer.parseInt(invocationCacheSizeStr) * 10;
            } catch (NumberFormatException e) {
                invocationCacheSize = 500;
            }
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME, "<static init>", "context classloader->" + Thread.currentThread().getContextClassLoader() + ", webcontainer classloader->"
                                                                 + WebContainer.class.getClassLoader());

    }

    protected final static ConcurrentMap _cacheMap = new ConcurrentHashMap(invocationCacheSize);
    final private static AtomicInteger _cacheSize = new AtomicInteger();

    protected boolean vHostCompatFlag = true;

    protected IPlatformHelper platformHelper;

    private static boolean isDefaultTempDir = false;// 252090

    private static String tempDir = null;

    private static boolean servletCachingInitNeeded = true; //TODO: make false and require dynacache to set to true

    public static boolean appInstallBegun = false;
    
    // Servlet 4.0 : Must be static since referenced from static method
    protected static CacheServletWrapperFactory cacheServletWrapperFactory;

    protected WebContainer(String name, Container parent) {
        super(name, parent);
        requestMapper = new VirtualHostMapper();
    }

    public void initialize(WebContainerConfiguration config) {
        this.wcconfig = config;
        this.auditManager = new AuditManager();

        // initialize the encoding
        getURIEncoding();

        _initialized = true;

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) //306998.15
            logger.logp(Level.FINE, CLASS_NAME, "initialize", "Web Container invocationCache --> [" + invocationCacheSize+ "]");

        webConProperties = new Properties();
    }

    /**
     *
     */
    private static void registerGlobalWebAppListeners() {
        if (logger.isLoggable(Level.FINE)) {
            logger.entering(CLASS_NAME, "registerGlobalWebAppListeners");
        }
        String classes = WCCustomProperties.LISTENERS;
        StringTokenizer st;
        String classname = null;
        if (classes != null) {
            st = new StringTokenizer(classes, ",");

            while (st.hasMoreElements()) {
                try {
                    classname = st.nextToken().trim();

                    addGlobalListener(classname);
                } catch (Throwable th) {
                    com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(
                                                                                 th,
                                                                                 CLASS_NAME + ".registerGlobalWebAppListeners",
                                                                                 "785",
                                                                                 getWebContainer());
                }
            }
        }

        listenersInitialized = true;

        if (logger.isLoggable(Level.FINE)) {
            logger.exiting(CLASS_NAME, "registerGlobalWebAppListeners");
        }
    }

    public static void addGlobalListener(String classname) {
        if (logger.isLoggable(Level.FINE)) {
            logger.entering(CLASS_NAME, "addGlobalListener");
            logger.logp(Level.FINE, CLASS_NAME, "addGlobalListener", "classname->" + classname);
        }
        // determine listener type...first, instantiate it
        Object listener = loadListener(classname);

        if (listener != null) {
            if (listener instanceof ApplicationListener) {
                applicationListeners.add(listener);
            }
            if (listener instanceof ServletListener) {
                servletListeners.add(listener);
            }
            if (listener instanceof ServletErrorListener) {
                servletErrorListeners.add(listener);
            }
            if (listener instanceof ServletInvocationListener) {
                servletInvocationListeners.add(listener);
            }
            // LIDB-3598: begin
            if (listener instanceof FilterInvocationListener) {
                filterInvocationListeners.add(listener);
            }
            //292460:    begin resolve issues concerning LIDB-3598    WASCC.web.webcontainer
            if (listener instanceof FilterListener) {
                filterListeners.add(listener);
            }
            if (listener instanceof FilterErrorListener) {
                filterErrorListeners.add(listener);
            }
            // 292460: end resolve issues concerning LIDB-3598 WASCC.web.webcontainer
            // LIDB-3598: end

            // CODE REVIEW START
            if (listener instanceof ServletContextAttributeListener) {
                servletContextAttributeListeners.add(listener);
            }
            if (listener instanceof ServletContextListener) {
                servletContextListeners.add(listener);
            }
            if (listener instanceof ServletRequestAttributeListener) {
                servletRequestAttributeListeners.add(listener);
            }
            if (listener instanceof ServletRequestListener) {
                servletRequestListeners.add(listener);
            }
            if (listener instanceof HttpSessionListener) {
                sessionListeners.add(listener);
            }
            if (listener instanceof HttpSessionAttributeListener) {
                sessionAttributeListeners.add(listener);
            }
            
            // Servlet 3.1 start.  ONly want to look for HttpSessionIdListener if Servlet 3.1 or later
            // is the Servlet implementation being used. 
            if(com.ibm.ws.webcontainer.osgi.WebContainer.getServletContainerSpecLevel() >= 
                            com.ibm.ws.webcontainer.osgi.WebContainer.SPEC_LEVEL_31) {
                try {
                    if(Class.forName("javax.servlet.http.HttpSessionIdListener").isInstance(listener)) {
                        sessionIdListeners.add(listener);
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                            logger.logp(Level.FINE, CLASS_NAME, "addGlobalListener", "Adding the following HttpSessionIdListener: " + listener.toString());
                        }
                    } 
                } catch(ClassNotFoundException e) {
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                        logger.logp(Level.FINE, CLASS_NAME, "addGlobalListener", "javax.servlet.http.HttpSessionIdListener was expected to be found but was not.");
                    }
                }
            }
           
            // CODE REVIEW END
        }
        if (logger.isLoggable(Level.FINE)) {
            logger.exiting(CLASS_NAME, "addGlobalListener");
        }
    }

    private static Object loadListener(String lClassName) {
        Object listener = null;

        try {
            // instantiate the listener
            listener = java.beans.Beans.instantiate(ThreadContextHelper.getContextClassLoader(), lClassName);
        } catch (IOException io) {
            // io problem...give warning
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(
                                                                         io,
                                                                         CLASS_NAME + ".loadListener",
                                                                         "1523",
                                                                         getWebContainer());
        } catch (ClassNotFoundException e) {
            // couldn't find the class...give warning
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(
                                                                         e,
                                                                         CLASS_NAME + ".loadListener",
                                                                         "1527",
                                                                         getWebContainer());
        } catch (ClassCastException e) {
            // bad class cast...give warning
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(
                                                                         e,
                                                                         CLASS_NAME + ".loadListener",
                                                                         "1531",
                                                                         getWebContainer());
        } catch (NoClassDefFoundError e) {
            // no class def...give warning
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(
                                                                         e,
                                                                         CLASS_NAME + ".loadListener",
                                                                         "1535",
                                                                         getWebContainer());
        } catch (ClassFormatError e) {
            // bad class format...give warning
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(
                                                                         e,
                                                                         CLASS_NAME + ".loadListener",
                                                                         "1539",
                                                                         getWebContainer());
        }

        return listener;
    }

    /**
     * Method loadCipherToBit.
     */
    // 112102 - added method below to fill the cipher to bit size table
    protected void loadCipherToBit() {
        boolean keySizeFromCipherMap =
                        Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.keysizefromciphermap", "true")).booleanValue();
        //721610
        if (keySizeFromCipherMap) {
            this.getKeySizefromCipherMap("toLoad"); // this will load the Map with values
        } else {
            Properties cipherToBitProps = new Properties();

            // load the ssl cipher suite bit sizes property file
            try {
                String fileName = System.getProperty("server.root") + File.separator + "properties" + File.separator + "sslbitsizes.properties";

                cipherToBitProps.load(new FileInputStream(fileName));
            } catch (Exception ex) {
                logger.logp(Level.SEVERE, CLASS_NAME, "loadCipherToBit", "failed.to.load.sslbitsizes.properties ", ex); /* 283348.1 */
                com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(ex, CLASS_NAME + ".loadCipherToBit", "825", this);
            }

            // put the properties into a hash map for unsynch. reference
            _cipherToBit.putAll(cipherToBitProps);
        }
    }

    public void destroy() {
        // destroy the subcomponents first
        super.destroy();

        requestMapper = null;
        // Begin 257796, part 1
        if (!isDefaultTempDir()) { //252090
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) //306998.14
                logger.logp(Level.FINE, CLASS_NAME, "destroy", "deleting tempDirectory");
            if (tempDir != null) {
                File f = new File(tempDir);
                if (f.exists()) {
                    try {
                        removeDir(f);
                    } catch (SecurityException e) {                       
                        // Translated to SRVE8058E: Did not have access to delete the temporary directory
                        logger.logp(Level.SEVERE, CLASS_NAME, "destroy", nls.getString("Did.not.have.access.to.delete.Directory"));
                    }

                }
            }
        }
        // End 257796, part 1

        // only shutdown if executor has been retrieved
        if (AsyncContextImpl.executorRetrieved.get()) {

            logger.logp(Level.INFO, CLASS_NAME, "destroy", "shutting down async servlet thread pool exucutor"); // do this ?
            AsyncContextImpl.ExecutorFieldHolder.field.shutdown();
            try {
                AsyncContextImpl.ExecutorFieldHolder.field.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.logp(Level.FINE, CLASS_NAME, "destroy", "There was some interruption : " + e.getMessage());
            }
        }
    }

    // Begin 257796, part 2
    private boolean removeDir(File dir) {

        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = removeDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }

    // Begin 257796, part 2

    /**
     * Method shutdown.
     */
    public void shutdown() {
        destroy();
    }

    /**
     * Method addWebApplication.
     * 
     * @param deployedModule
     * @throws WebAppNotLoadedException
     */
    //BEGIN: NEVER INVOKED BY WEBSPHERE APPLICATION SERVER (Common Component Specific)
    public void addWebApplication(DeployedModule deployedModule) throws WebAppNotLoadedException {
        try {
            appInstallBegun = true;
            addWebApp(deployedModule);
        } catch (WebAppNotLoadedException e) {
            throw e;
        } catch (Throwable th) {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th, CLASS_NAME, "695", this);
            throw new WebAppNotLoadedException(th.getMessage(), th); // 296368 added rootCause to newly created exception.
        }
    }

    //END: NEVER INVOKED BY WEBSPHERE APPLICATION SERVER (Common Component Specific)

    //BEGIN: NEVER INVOKED BY WEBSPHERE APPLICATION SERVER (Common Component Specific)
    protected void addWebApp(DeployedModule dm) throws WebAppNotLoadedException {
        try {
            String virtualHost = dm.getVirtualHostName();

            if ((virtualHost == null) || (virtualHost.equals("")))
                virtualHost = DEFAULT_HOST;

            VirtualHost vHost = getVirtualHost(virtualHost);

            if (vHost == null) {
                logger.logp(Level.INFO, CLASS_NAME, "addWebApp",
                            "host.has.not.been.defined",
                            new String[] { virtualHost });
                throw new WebAppNotLoadedException("Virtual Host " + virtualHost + " not found");
            }

            //need to parse the ibm-web-ext before mapping the context root
            com.ibm.wsspi.adaptable.module.Container mc = dm.getWebApp().getModuleContainer();
            WebExt ext = null;
            if (mc != null) {
                try {
                    ext = mc.adapt(WebExt.class);
                } catch (UnableToAdaptException e) {
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                        logger.logp(Level.FINE, CLASS_NAME, "commonInitializationFinally", "Unable to parse the WebExt file", e);
                    }
                    ext = null;
                }
            }
            if (ext != null) {
                dm.getWebAppConfig().setInitializeWebExtProps(ext);
            }
            //done parsing the ibm-web-ext file
            vHost.addWebApplication(dm, extensionFactories);
        } catch (WebAppNotLoadedException wahnf) {
            com.ibm.ws.ffdc.FFDCFilter.processException(wahnf, "com.ibm.ws.webcontainer.Webcontainer", "732", this);
            throw wahnf;
        } catch (Throwable th) {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th, CLASS_NAME, "736", this);
            throw new WebAppNotLoadedException(th.getMessage(), th);
        }

    }

    //END: NEVER INVOKED BY WEBSPHERE APPLICATION SERVER (Common Component Specific)

    /**
     * Method getVirtualHost. Returns null if the input name does not
     * match any configured host.
     * 
     * @param targetHost
     * @return VirtualHost
     * 
     *         This method is not to be used in any request processing as it
     *         is not optimized for performance
     * @throws WebAppHostNotFoundException
     */
    public VirtualHost getVirtualHost(String targetHost) throws WebAppHostNotFoundException {
        Iterator i = requestMapper.targetMappings();

        while (i.hasNext()) {
            RequestProcessor rp = (RequestProcessor) i.next();
            if (rp instanceof VirtualHost) {
                VirtualHost vHost = (VirtualHost) rp;
                if (targetHost.equalsIgnoreCase(vHost.getName()))
                    return vHost;
            }
        }

        return null;
    }

    /**
     * Method removeWebApplication.
     * 
     * @param deployedModule
     * @throws Exception
     */
    public void removeWebApplication(DeployedModule deployedModule) throws Exception {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "removeWebApplication", "Deployed Module Virtual Host Name:", deployedModule.getVirtualHostName());
        }

        try {
            VirtualHost vHost = getVirtualHost(deployedModule.getVirtualHostName());

            if (vHost == null)
                throw new WebAppHostNotFoundException("VirtualHost not found");

            vHost.removeWebApplication(deployedModule);

        } catch (Exception e) {
            logger.logp(Level.SEVERE, CLASS_NAME, "removeWebApplication", "Exception", new Object[] { e }); /* 283348.1 */
            throw e;
        }

    }

    /**
     * Method getWebContainer.
     * 
     * @return WebContainer
     */
    public static WebContainer getWebContainer() {
        WebContainer selfInstance = self.get();
        if (selfInstance != null) {
            return selfInstance;
        }
        CountDownLatch currentLatch = selfInit;
        try {
            currentLatch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // auto-FFDC
            Thread.currentThread().interrupt();
        }
 
        currentLatch.countDown(); // don't wait again
        return self.get();
    }

    /**
     * Method setSessionRegistry.
     * 
     * @param _sessRegistry
     */
    public void setSessionRegistry(SessionRegistry sessRegistry) {
        this.sessionRegistry = sessRegistry;
    }

    /**
     * Method to satisfy the Webcontainer MBean interface
     * 
     * @param moduleName - name of the module to be restarted
     */
    public void restartWebApplication(String moduleName) throws WebAppNotLoadedException {
    // throw new WebAppNotLoadedException("Could not restart :"+moduleName);
    }

    /**
     * Method reload.
     * 
     * @param deployedModule
     */
    public void reload(DeployedModule webModuleConfig) throws WebAppNotLoadedException {
        restartWebApplication(webModuleConfig);
    }

    /**
     * Method restartWebApplication.
     * 
     * @param webModuleConfig
     * @throws WebAppNotLoadedException
     */
    //BEGIN: NEVER INVOKED BY WEBSPHERE APPLICATION SERVER (Common Component Specific)
    public void restartWebApplication(DeployedModule webModuleConfig) throws WebAppNotLoadedException {
        try {
            removeWebApplication(webModuleConfig);
        } catch (Exception e) {
            String groupName = webModuleConfig.getName();
            //Translated to SRVE0314E: Failed to remove web module {0}: {1}
            String message = nls.getFormattedMessage("failed.to.remove.webmodule", new Object[] { groupName, e }, "Failed to remove web module");
            logger.logp(Level.SEVERE, CLASS_NAME, "restartWebApplication", message);
            return;
        }

        addWebApplication(webModuleConfig);
    }

    //END: NEVER INVOKED BY WEBSPHERE APPLICATION SERVER (Common Component Specific)

    /**
     * Method areRequestsOutstanding.
     * 
     * @return boolean
     */
    public abstract boolean areRequestsOutstanding();

    // Begin 277095
    public WebContainerConfiguration getWebContainerConfig() {
        return wcconfig;
    }

    // End 277095

    /**
     * Method getSessionContext.
     * 
     * @param moduleConfig
     * @param webApp
     * @param string
     * @return IHttpSessionContext
     */
    public IHttpSessionContext getSessionContext(DeployedModule moduleConfig, WebApp webApp, String host, ArrayList[] listeners) throws Throwable {
        try {
            return getSessionRegistry().getSessionContext(moduleConfig, webApp, host, listeners);
        } catch (Throwable e) {
            //Translated to SRVE8059E: An unexpected exception occurred when trying to retrieve the session context
            logger.logp(Level.SEVERE, CLASS_NAME, "getSessionContext", nls.getString("unable.to.get.sessionContext"), e); /* 283348.1 */
            return null;
        }

    }

    /**
     * Method getSessionRegistry.
     */
    protected SessionRegistry getSessionRegistry() {
        return sessionRegistry;
    }

    /**
     * 
     * Entry point into the webcontainer for all request processing. Currently,
     * this method has been implemented for the HTTP protocol.
     * 
     * @param req
     * @param res
     * @throws IOException
     */
    public void handleRequest(IRequest req, IResponse res) throws IOException {
        // We don't know what processor to use, we'll have to find it
        handleRequest(req, res, null, null);
    }

    public static String getHostAliasKey(String host, int port) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "getHostAliasKey", "host-->"+host +", port-->"+port);
        }
        StringBuilder vhostKey = new StringBuilder();
        String serverName = host;
        // Begin 255189, Part 1
        if (serverName != null && serverName.length() > 0) {
            if (serverName.charAt(0) == '[' && serverName.charAt(serverName.length() - 1) == ']')
                serverName = serverName.substring(1, serverName.length() - 1);
            vhostKey.append(serverName.toLowerCase()); //have to do lower case here instead of mapper since context root is case sensitive, stupid VirtualHostContextRootMapper!
        }
        // End 255189, Part 1
        vhostKey.append(':');
        vhostKey.append(port);
        return vhostKey.toString();
    }

    public void handleRequest(IRequest req, IResponse res, VirtualHost vhost, RequestProcessor processor) throws IOException {
        final boolean isTraceOn = com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled();
        Object localSecObject = null;
        SRTConnectionContext connContext = null;
        IExtendedRequest hreq = null;
        IExtendedResponse hres = null;
        StringBuilder cacheKey = null;
        WebContainerRequestState reqState = WebContainerRequestState.getInstance(false);

        String vhostKey = getHostAliasKey(req.getServerName(), req.getServerPort());

        boolean ardRequest = false;
        if (reqState != null) {
            ardRequest = reqState.isArdRequest();
            if (ardRequest) {
                req.setShouldClose(false);
            }
            reqState.init();
        } else {
            reqState = WebContainerRequestState.createInstance();

        }

        IWebAppSecurityCollaborator localSecCollab = null;
        try {
            if (!_initialized) {
                throw new WebGroupVHostNotFoundException("Not found.");
            }
            //getting the default security collaborator - it is only used in this method for preInvoke and postInvoke
            //therefore, it doesn't matter if we get the correct one or not.  This collaborator should be local to this method
            localSecCollab = CollaboratorServiceImpl.getWebAppSecurityCollaborator("default");
            if (localSecCollab != null) {
                localSecObject = localSecCollab.preInvoke();
            }

            connContext = getConnectionContext();
            connContext.prepareForNextConnection(req, res);
            hreq = connContext.getRequest();
            hres = connContext.getResponse();
            auditManager.setHttpServletRequest((Object)hreq);

            reqState.setCurrentThreadsIExtendedRequest(hreq);
            reqState.setCurrentThreadsIExtendedResponse(hres);

            if (isTraceOn && logger.isLoggable(Level.FINE)) //306998.15
                logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "Handling request with virtual host key of --> [" + vhostKey.toString()+"]");

            // try and get the StringBuilder from the ThreadLocal storage
            // TODO: Is this better than creating an object every time?
            cacheKey = (StringBuilder) cacheKeyStringBuilder.get();
            if (cacheKey == null) {
                cacheKey = new StringBuilder();
                cacheKeyStringBuilder.set(cacheKey);
            }

            WebAppDispatcherContext currDispatchContext = (WebAppDispatcherContext) hreq.getWebAppDispatcherContext();

            // Begin 293696 ServletRequest.getPathInfo() fails WASCC.web.webcontainer
            String reqURI = req.getRequestURI();
            String decodedReqURI = null;
            if (WCCustomProperties.DECODE_URL_PLUS_SIGN) {
                decodedReqURI = URLDecoder.decode(reqURI, encoding);
            } else {
                decodedReqURI = WSURLDecoder.decode(reqURI, encoding);
            }

            currDispatchContext.setDecodedReqUri(decodedReqURI);
            if (isTraceOn && logger.isLoggable(Level.FINE)) { //306998.15
                logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "webcontainer.handleRequest request uri --> (not decoded=" + reqURI + "), (decoded=" + decodedReqURI
                                                                     + "), (encoding=" + encoding + ")");
            }
            // End 293696 ServletRequest.getPathInfo() fails WASCC.web.webcontainer

            //TODO: do we still need this check any more since we don't actually decode anything
            if (decode) {
                // URLs have been decoded with UTF-8 but not yet URLDecoded
                //reqURI = URLDecoder.decode(reqURI, encoding); // encoding should be UTF-8
                String isoURI = new String(reqURI.getBytes(encoding), StandardCharsets.ISO_8859_1);
                hreq.setAttribute("com.ibm.websphere.servlet.uri_non_decoded", isoURI);
                if (isTraceOn && logger.isLoggable(Level.FINE)) //306998.15 
                {
                    logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "webcontainer.handleRequest uri_non_decoded --> " + isoURI);
                }
            }

            // Note: this typecast is required due to the JDK on a Mac
            cacheKey.append((CharSequence) vhostKey);
            // begin 272738    Duplicate CacheServletWrappers when url-rewriting is enabled    WAS.webcontainer: rewritten to handle jsessionid.
            PathInfoHelper pathInfoHelper = removeExtraPathInfo(reqURI);
            String strippedRequestURI = null;
            String extraPathInfo = null;
            if (pathInfoHelper != null) {
                strippedRequestURI = pathInfoHelper.getBasePath();
                extraPathInfo = pathInfoHelper.getExtraPathInfo();
                if (isTraceOn && logger.isLoggable(Level.FINE)) { //306998.15
                    logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "PathInfoHelper strippedRequestURI --> ["+ strippedRequestURI + "] extraPath --> [" + extraPathInfo+"]");
                }
            }
            cacheKey.append(strippedRequestURI);
            // end 272738    Duplicate CacheServletWrappers when url-rewriting is enabled    WAS.webcontainer: rewritten to handle jsessionid.

            CacheServletWrapper wrapper = getFromCache(cacheKey);
            if (wrapper != null) {
                WebApp webApp = wrapper.getWebApp();// 325429
                if (isTraceOn && logger.isLoggable(Level.FINE)) //306998.15 
                {
                    logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "Found CacheServletWrapper with key --> ["+cacheKey.toString()+"]");
                }
                try {

                    // 325429 BEGIN
                    if (isTraceOn && logger.isLoggable(Level.FINE)) //306998.15
                        logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "Check if webApp ["+ webApp.getApplicationName()+"] is being destroyed --> " + (webApp.getDestroyed().booleanValue()));
                    if (webApp.getDestroyed().booleanValue()) { // should be a fast boolean check.
                        //no need to invalidate here, this is duplicate of what the destroying webapp will do
                        // wrapper.invalidate();
                        if (isTraceOn && logger.isLoggable(Level.FINE)) //306998.15
                            logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "sending sendUnavailableException for servlet from: ["+wrapper.getServletPath()+"]");
                        sendUnavailableException(req, res);
                        return;
                    }
                    // 325429 END
                    // begin 272738    Duplicate CacheServletWrappers when url-rewriting is enabled    WAS.webcontainer: rewritten to handle jsessionid.    
                    currDispatchContext.setWebApp(webApp);
                    connContext.start();
                    // begin 279547    FVT: Testcase Web02 failed on the build o0521.11. When a secure    WAS.webcontainer    
                    String contextPath = webApp.getContextPath();
                    if (contextPath == null || contextPath.equalsIgnoreCase("/")) {
                        contextPath = "";
                    }
                    String servletPath = wrapper.getServletPath();
                    int prePathInfoLength = contextPath.length() + servletPath.length();
                    //End 293696    ServletRequest.getPathInfo() fails    WASCC.web.webcontainer
                    //Start PI51122
                    String pathInfo;
                    if(servletPath == "/")
                        pathInfo = "";
                    else
                        pathInfo = decodedReqURI.substring(prePathInfoLength);
                    //End PI5122
                    //End 293696    ServletRequest.getPathInfo() fails    WASCC.web.webcontainer
                    //PM76167 (PM71654) Start
                    String cacheWrapperPathInfo = wrapper.getPathInfo();                                        
                    if (isTraceOn&&logger.isLoggable (Level.FINE)) {                     
                            logger.logp(Level.FINE, CLASS_NAME,"handleRequest", "previous pathInfo --> ["+ cacheWrapperPathInfo + "] , pathinfo from uri -->["+pathInfo +"]");
                    }
                    // The following case is required as the first request to context-root without "/"  returns pathInfo ="/" 
                    // but relativeURI is not updated for wrapper as / from "".
                    if(pathInfo.equals("") && ("/").equals(cacheWrapperPathInfo)) {
                            pathInfo = "/";
                    } //PM71654 End
                    
                    //start PI31292
                    if(WCCustomProperties.USE_SEMICOLON_AS_DELIMITER_IN_URI){
                        //do not remove jsessionid in the following case: /path;1232;jsessionid=...
                        int semiColon = pathInfo.indexOf(';');
                        String lowerCasePathInfo = pathInfo.toLowerCase();
                        int jsessionIndex = lowerCasePathInfo.indexOf(";jsessionid");
                        if(semiColon > -1 && !lowerCasePathInfo.substring(semiColon + 1).startsWith("jsessionid")){
                            if(jsessionIndex < 0)
                                pathInfo = pathInfo.substring(0, semiColon);
                            else
                                pathInfo = pathInfo.substring(0, semiColon) + pathInfo.substring(jsessionIndex);
                        }
                    }
                    //end PI31292

                    currDispatchContext.setRequestURI(reqURI);
                    pathInfo = pathInfo.trim();
                    currDispatchContext.setPathElements(servletPath, (pathInfo.equalsIgnoreCase("") ? null : pathInfo));
                    // end 279547    FVT: Testcase Web02 failed on the build o0521.11. When a secure    WAS.webcontainer    

                    if (isTraceOn && logger.isLoggable(Level.FINE)) //306998.15 
                    {
                        logger.logp(Level.FINE, CLASS_NAME,"handleRequest", "CachedServletWrapper servletPath --> ["+ currDispatchContext.getServletPath()+"]");
                        logger.logp(Level.FINE, CLASS_NAME,"handleRequest", "CachedServletWrapper pathInfo --> ["+ currDispatchContext.getPathInfo()+"]");
                    }
                    // end 272738    Duplicate CacheServletWrappers when url-rewriting is enabled    WAS.webcontainer: rewritten to handle jsessionid.

                    currDispatchContext.setQueryString(hreq.getQueryString());
                    hreq.setValuesIfMultiReadofPostdataEnabled(); //MultiRead
                    if (vhost != null) {
                        vhost.addSecureRedirect(hreq, vhostKey);
                    }
                    if (isTraceOn&&logger.isLoggable (Level.FINE)) {                     
                        logger.logp(Level.FINE, CLASS_NAME,"handleRequest", "start handling the resource by wrapper -->["+ wrapper.getName()+"]");
                    }
                    //auditManager.setHttpServletRequest((Object)hreq);

                    wrapper.handleRequest(hreq, hres);
                } catch (InvalidCacheTargetException ne) {
                    // This will happen when the cacheWrapper has been invalidated
                    wrapper = null;
                    RequestProcessor v;

                    if (processor != null) {
                        v = processor;
                    } else if (this.isVHostCompatFlag()) {
                        if (isTraceOn && logger.isLoggable(Level.FINE)) //306998.15
                        {
                            logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "Looking for vhost with key --> " + vhostKey);
                        }
                        v = requestMapper.map(vhostKey);
                    } else {
                        if (isTraceOn && logger.isLoggable(Level.FINE)) //306998.15
                        {
                            logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "Looking for vhost with key --> " + vhostKey + decodedReqURI);
                        }
                        v = requestMapper.map(vhostKey + decodedReqURI);
                    }

                    if (v != null) {
                        if (isTraceOn && logger.isLoggable(Level.FINE)) //306998.15
                        {
                            logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "request processor handling request --> " + v);
                        }

                        v.handleRequest(hreq, hres);
                    } else {
                        throw new WebGroupVHostNotFoundException(vhostKey);
                    }
                }
                // 325429 BEGIN
                catch (Throwable th) {
                    wrapper.invalidate();
                    if (isTraceOn && logger.isLoggable(Level.FINE)) //306998.15
                        logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "removing cache wrapper because exception was thrown and rethrowing the exception.");
                    throw th;
                }
                // 325429 END
            } else {
                currDispatchContext.setRequestURI(reqURI);
                //Begin 252775, for the first request, forward query string attribute was not set
                //because we didn't set the query string on the current dispatch context.
                currDispatchContext.setQueryString(req.getQueryString());
                // End 252775

                if (processor == null) {
                    if (this.isVHostCompatFlag()) {
                        if (isTraceOn && logger.isLoggable(Level.FINE)) //306998.15 
                        {
                            logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "Looking for vhost with key --> " + vhostKey);
                        }
                        processor = requestMapper.map(vhostKey);
                    } else {
                        if (isTraceOn && logger.isLoggable(Level.FINE)) //306998.15
                        {
                            logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "Looking for vhost with key --> " + vhostKey + decodedReqURI);
                        }
                        processor = requestMapper.map(vhostKey + decodedReqURI);
                    }
                }

                if (processor != null) {
                    if (isTraceOn && logger.isLoggable(Level.FINE)) //306998.15
                    {
                        logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "request processor handling request --> " + processor);
                    }
                    processor.handleRequest(hreq, hres);
                } else {
                    throw new WebGroupVHostNotFoundException(vhostKey);
                }
            }
        } catch (WebGroupVHostNotFoundException e) {
            //PK85685 Start
            //res.addHeader("Content-Type", "text/html");
            res.setStatusCode(404);
            String webGroupVHostNotFound = WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.webgroupvhostnotfound"); //read Custom property
            byte[] outBytes = null;

            if (webGroupVHostNotFound == null || webGroupVHostNotFound.length() == 0) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "handleRequest",
                                "WebGroup/VHost not found , webGroupVHostNotFound custom property is either not set by user or has null value--> " + webGroupVHostNotFound);
                }
                res.addHeader("Content-Type", "text/html");
                String output =
                                "<H1>"
                                                + MessageFormat.format(nls.getString("Web.Group.VHost.Not.Found", "WebGroup Not Found"),
                                                                       new Object[] { ResponseUtils.encodeDataString(req.getRequestURI()) })
                                                + "</H1><BR><H3>"
                                                + e.getMessage()
                                                + "</H3><BR>";                  //PI67093

                outBytes = output.getBytes();
            } else {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "WebGroup/VHost not found, webGroupVHostNotFound custom property is set to value--> "
                                                                         + webGroupVHostNotFound);
                }
                res.addHeader("Content-Type", "text/html;charset=UTF-8");
                String output = webGroupVHostNotFound;
                outBytes = output.getBytes("UTF-8"); // The custom property is stored in server.xml which is in UTF-8 and ISO-8859-1 is a subset of UTF-8 so it would work for anything in there.                  

            }
            //PK85685 End
            res.getOutputStream().write(outBytes, 0, outBytes.length);
            
            //760370 - start - do not factor out any of these properties to WCCustomProperties, else it will not work
            String reqURI = req.getRequestURI();
            String suppressLoggingWebGroupVHostNotFound = WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.suppressloggingwebgroupvhostnotfound");                     

            if (suppressLoggingWebGroupVHostNotFound != null && suppressLoggingWebGroupVHostNotFound.equalsIgnoreCase(reqURI)){
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                    logger.logp(Level.FINE, CLASS_NAME, "handleRequest", nls.getString("Web.Group.VHost.Not.Found", "WebGroup Not Found"), new Object[] { truncateURI(reqURI) });
            }
            else{  //760370 - end
                logger.logp(Level.SEVERE, CLASS_NAME, "handleRequest", nls.getString("Web.Group.VHost.Not.Found", "WebGroup Not Found"), new Object[] { truncateURI(reqURI) });
            }
        } catch (WebContainerException e) {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, CLASS_NAME, "134", this);
            res.setStatusCode(500); 
            byte[] outBytes = null;
            String output = WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.displaycustomizedexceptiontext");

            if (output != null){
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "WebContainerException.  Response with a customized display text");
                }

                res.addHeader("Content-Type", "text/html;charset=UTF-8");
                outBytes = output.trim().getBytes("UTF-8");                  
            }
            else{
                res.addHeader("Content-Type", "text/html");
                res.setStatusCode(500);           
                output =
                                "<H1>"
                                                + nls.getFormattedMessage("Engine.Exception.[{0}]", new Object[] { e.getMessage() }, "Internal Server Error. <br> Exception Message: ")
                                                + "</H1><BR><H3>"
                                                + e.getMessage()
                                                + "</H3><BR>";              //PI67093
                outBytes = output.getBytes();
            }

            res.getOutputStream().write(outBytes, 0, outBytes.length);

            Object[] args = { e };
            String formattedMessage = nls.getFormattedMessage("Engine.Exception.[{0}]", args , "Engine Exception");
            logger.logp(Level.SEVERE, CLASS_NAME, "handleRequest", formattedMessage);
        } catch (SecurityViolationException e) {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, CLASS_NAME, "148", this);
            res.setStatusCode(403);
            byte[] outBytes = null;
            String output = WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.displaycustomizedexceptiontext");

            if (output != null){
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "SecurityViolationException.  Response with a customized display text");
                }

                res.addHeader("Content-Type", "text/html;charset=UTF-8");
                outBytes = output.trim().getBytes("UTF-8");                
            }
            else {
                res.addHeader("Content-Type", "text/html");
                res.setStatusCode(403);
                //Translated to SRVE0218E: Forbidden: Web Security Exception
                output =
                                "<H1>"
                                                + nls.getString("Forbidden.Web.Security.Exception", "Forbidden: Web Security Exception")
                                                + "</H1><BR><H3>"
                                                + e.getMessage()
                                                + "</H3><BR>";              //PI67093
                outBytes = output.getBytes();
            }
            res.getOutputStream().write(outBytes, 0, outBytes.length);

            Object[] args = { e };
            //Translated to SRVE0139E: Exception in Security preInvoke {0}
            String formattedMessage = nls.getFormattedMessage("preInvoke.Security.Exception", args , "Exception in Security preInvoke");
            logger.logp(Level.SEVERE, CLASS_NAME, "handleRequest", formattedMessage);

        } catch (Throwable th) {
            //TODO: should we handle exception here and complete the async processing?
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th, CLASS_NAME, "162", this);
            res.setStatusCode(500);
            byte[] outBytes = null;
            String output = WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.displaycustomizedexceptiontext");

            if (output != null){
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "Exception.  Response with a customized display text");
                }

                res.addHeader("Content-Type", "text/html;charset=UTF-8");
                outBytes = output.trim().getBytes("UTF-8");                  
            }
            else { 
                res.addHeader("Content-Type", "text/html");
                res.setStatusCode(500);
                output =
                                "<H1>"
                                                + nls.getFormattedMessage("Engine.Exception.[{0}]", new Object[] { th.getMessage() }, "Engine Exception")
                                                + "</H1><BR><H3>"
                                                + th.getMessage()
                                                + "</H3><BR>";              //PI67093
                outBytes = output.getBytes();
            }

            res.getOutputStream().write(outBytes, 0, outBytes.length);

            Object[] args = { th };
            String formattedMessage = nls.getFormattedMessage("Engine.Exception.[{0}]", args , "Engine Exception");
            logger.logp(Level.SEVERE, CLASS_NAME, "handleRequest", formattedMessage);

        } finally {
            if (cacheKey != null)
                cacheKey.setLength(0);

            if (connContext != null) {
                //request state may have been created since first initialization...try again
                reqState = WebContainerRequestState.getInstance(false);
                if (reqState != null && reqState.isAsyncMode()) {
                    AsyncContext asyncContext = reqState.getAsyncContext();
                    asyncContext.executeNextRunnable();
                } else {
                    connContext.finishConnection(); //this is okay for ARD since we have clones of the connection context.
                    releaseConnectionContext(connContext);
                    connContext = null;
                }
            }
            try {
                if (localSecCollab != null) {
                    localSecCollab.postInvoke(localSecObject);
                }
            } catch (Exception e) {
                Object[] args = { e };
                com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, CLASS_NAME, "183", this);
                logger.logp(Level.SEVERE, CLASS_NAME, "handleRequest", "postInvoke.Security.Exception.", args);
            }
        }
    }

    /**
     * Method getWebContainerProperties.
     * 
     * @return Properties
     */
    public static Properties getWebContainerProperties() {
        return WebContainer.webConProperties;
    }

    protected abstract SRTConnectionContext getConnectionContext();

    protected abstract void releaseConnectionContext(SRTConnectionContext context);

    // begin 272738    Duplicate CacheServletWrappers when url-rewriting is enabled    WAS.webcontainer: rewritten to prevent duplicates    
    public static void addToCache(HttpServletRequest req, RequestProcessor s, WebApp app) {
        //Begin 253010  
        // check if cache is full
        if (_cacheSize.get() >= invocationCacheSize) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) //306998.15
            {
                logger.logp(Level.FINE, CLASS_NAME, "addToCache", "cache is full");
            }
            return;
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) //306998.15
        {
            logger.logp(Level.FINE, CLASS_NAME, "addToCache", "WebApp = " + app);
        }

        // PK80333 Start
        if (app.getDestroyed()) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) //306998.15
            {
                logger.logp(Level.FINE, CLASS_NAME, "addToCache", "Not caching as the webapp is destroyed");
            }
            return;
        }
        // PK80333 End

        StringBuilder cacheKey = (StringBuilder) cacheKeyStringBuilder.get();
        if (cacheKey == null) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) //306998.15
            {
                logger.logp(Level.FINE, CLASS_NAME, "addToCache", "cache key is null");
            }
            return;
        }
        String cacheKeyStr = cacheKey.toString();
        // Servlet 4.0 : Use CacheServletWrapperFactory
        CacheServletWrapper wrapper =  cacheServletWrapperFactory.createCacheServletWrapper((IServletWrapper) s, req, cacheKeyStr, app);
        if (_cacheMap.containsKey(cacheKeyStr) || _cacheMap.putIfAbsent(cacheKeyStr, wrapper) != null) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) //306998.15
            {
                logger.logp(Level.FINE, CLASS_NAME, "addToCache", "Already cached cacheKey --> " + cacheKey);
            }
        } else {
            ((IServletWrapper)s).addServletReferenceListener(wrapper);
            // keep a rough count of the number of items in the cache
            _cacheSize.incrementAndGet();
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) //306998.15 
            {
                logger.logp(Level.FINE, CLASS_NAME, "addToCache", "Added to cache cacheKey --> " + cacheKey + " uri -->" + req.getRequestURI() + " servletWrapper -->"
                                                                      + ((IServletWrapper) s).getServletName());
            }
        }
        // End 253010
    }

    // end 272738    Duplicate CacheServletWrappers when url-rewriting is enabled    WAS.webcontainer: rewritten to prevent duplicates

    public static CacheServletWrapper getFromCache(CharSequence key) {
        return (CacheServletWrapper) _cacheMap.get(key.toString());
    }

    public static CacheServletWrapper removeFromCache(CharSequence key) {
        CacheServletWrapper wrapper = (CacheServletWrapper) _cacheMap.remove(key.toString());
        if (wrapper != null) {
            _cacheSize.decrementAndGet();
        }

        return wrapper;
    }

    public Integer getKeySize(String cipherSuite) {
        String keySize = (String) _cipherToBit.get(cipherSuite);

        if (keySize == null || keySize.equals("") || keySize.equals("-1"))
            return null;

        return new Integer(keySize);
    }

    //721610
    public Integer getKeySizefromCipherMap(String cipherSuite) {

        return ReadCipherBitSize.cipherData.get(cipherSuite);

    }

    // public void registerVirtualHostExtensionFactory(ExtensionFactory factory)
    // {
    // try {
    // ExtensionProcessor ep = factory.createExtensionProcessor(null);
    // Iterator patListIt = factory.getPatternList().iterator();
    // while (patListIt.hasNext()) {
    // String vhostMapping = (String) patListIt.next();
    // vhostMapping=vhostMapping.toLowerCase();
    // requestMapper.addMapping(vhostMapping, ep);
    // }
    // }
    // catch (Exception e) {
    // }
    // }

    /**
     * @param factory
     */
    public void addExtensionFactory(ExtensionFactory factory) {
        List l = factory.getPatternList();

        Iterator it = l.iterator();

        StringBuilder mapStr = new StringBuilder(' ');

        while (it.hasNext()) {
            String mapping = (String) it.next();
            mapStr.append(mapping);
            mapStr.append(' ');
        }
        logger.logp(Level.INFO, CLASS_NAME, "addExtensionFactory", "ExtensionFactory.[{0}].registered.successfully", new Object[] { factory.getClass().toString() });
        logger.logp(Level.INFO, CLASS_NAME, "addExtensionFactory", "ExtensionFactory.[{0}].associated.with.patterns.[{1}]", new Object[] { factory.getClass().toString(),
                                                                                                                                          mapStr.toString() });
        if (!appInstallBegun)
            extensionFactories.add(factory);
        else {
            // LIBERTY: add to existing apps now?
            Iterator<VirtualHost> vhosts = getVirtualHosts();
            while (vhosts.hasNext()) {
                com.ibm.ws.webcontainer.osgi.DynamicVirtualHost vhost = (com.ibm.ws.webcontainer.osgi.DynamicVirtualHost) vhosts.next();
                Iterator webApps = vhost.getWebApps();
                while (webApps.hasNext()) {
                    WebApp webApp = (WebApp) webApps.next();
                    webApp.addExtensionFactory(factory);
                }
            }
            // LIBERTY synchronized(extensionFactories){
            // LIBERTY if (postInitExtensionFactories==null){
            // LIBERTY postInitExtensionFactories = Collections.synchronizedList(new
            // ArrayList<ExtensionFactory>());
            // LIBERTY }
            // LIBERTY }
            // LIBERTY postInitExtensionFactories.add(factory);
        }
    }

    /**
     * @param listener
     */
    public static void addHttpServletResponseListener(IHttpServletResponseListener listener) {
        if (listener != null) {
            httpResponseListeners.add(listener);
        }
    }

    public static void notifyHttpServletResponseListenersPreHeaderCommit(HttpServletRequest request, HttpServletResponse response) {
        // need to notify listeners registered in the ServletRequestAttributeListener array
        if (!httpResponseListeners.isEmpty()) {
            Iterator i = httpResponseListeners.iterator();

            while (i.hasNext()) {
                // get the listener
                IHttpServletResponseListener rL = (IHttpServletResponseListener) i.next();

                // invoke the listener's attr added method
                rL.preHeaderCommit(request, response);
            }
        }
    }

    /**
     * @param isSystemApp
     * @return List
     */
    public static List getApplicationListeners(boolean isSystemApp) {
        if (!listenersInitialized && !isSystemApp)
            registerGlobalWebAppListeners();
        return applicationListeners;
    }

    /**
     * @param isSystemApp
     * @return List
     */
    public static List getServletErrorListeners(boolean isSystemApp) {
        if (!listenersInitialized && !isSystemApp)
            registerGlobalWebAppListeners();

        return servletErrorListeners;
    }

    /**
     * @param isSystemApp
     * @return List
     */
    public static List getServletInvocationListeners(boolean isSystemApp) {
        if (!listenersInitialized && !isSystemApp)
            registerGlobalWebAppListeners();
        return servletInvocationListeners;
    }

    // LIDB-3598: begin
    public static List getFilterInvocationListeners(boolean isSystemApp) {
        if (!listenersInitialized && !isSystemApp)
            registerGlobalWebAppListeners();

        return filterInvocationListeners;
    }

    // 292460: begin resolve issues concerning LIDB-3598 WASCC.web.webcontainer
    public static List getFilterListeners(boolean isSystemApp) {
        if (!listenersInitialized && !isSystemApp)
            registerGlobalWebAppListeners();

        return filterListeners;
    }

    public static List getFilterErrorListeners(boolean isSystemApp) {
        if (!listenersInitialized && !isSystemApp)
            registerGlobalWebAppListeners();

        return filterErrorListeners;
    }

    // 292460: end resolve issues concerning LIDB-3598 WASCC.web.webcontainer

    // LIDB-3598: end

    // CODE REVIEW START
    public static List getServletContextAttributeListeners(boolean isSystemApp) {
        if (!listenersInitialized && !isSystemApp)
            registerGlobalWebAppListeners();
        return servletContextAttributeListeners;
    }

    public static List getServletContextListeners(boolean isSystemApp) {
        if (!listenersInitialized && !isSystemApp)
            registerGlobalWebAppListeners();
        return servletContextListeners;
    }

    public static List getServletRequestAttributeListeners(boolean isSystemApp) {
        if (!listenersInitialized && !isSystemApp)
            registerGlobalWebAppListeners();
        return servletRequestAttributeListeners;
    }

    public static List getServletRequestListeners(boolean isSystemApp) {
        if (!listenersInitialized && !isSystemApp)
            registerGlobalWebAppListeners();
        return servletRequestListeners;
    }

    public static List getSessionListeners(boolean isSystemApp) {
        if (!listenersInitialized && !isSystemApp)
            registerGlobalWebAppListeners();
        return sessionListeners;
    }

    public static List getSessionAttributeListeners(boolean isSystemApp) {
        if (!listenersInitialized && !isSystemApp)
            registerGlobalWebAppListeners();
        return sessionAttributeListeners;
    }
    
    public static List getSessionIdListeners(boolean isSystemApp) {
        if (!listenersInitialized && !isSystemApp)
            registerGlobalWebAppListeners();
        return sessionIdListeners;
    }

//	CODE REVIEW END

    /**
     * @param isSystemApp
     * @return List
     */
    public static List getServletListeners(boolean isSystemApp) {
        if (!listenersInitialized && !isSystemApp)
            registerGlobalWebAppListeners();
        return servletListeners;
    }

    public List getExtensionFactories() {
        return extensionFactories;
    }

    protected static String truncateURI(String uri) {
        if (uri.length() > 128) {
            uri = uri.substring(0, 127);
        }
        return uri;
    }

    /**
     * @return String
     */
    public String getURIEncoding() {
        if (encoding != null)
            return encoding;

        if (!(getPlatformHelper().isDecodeURIPlatform())) {
            decode = false;
        }

        // default encoding.
        encoding = "UTF-8";

        if (!WCCustomProperties.DECODE_URL_AS_UTF8) {
            encoding = ISO;
            decode = false;
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) //306998.15
        {
            logger.logp(Level.FINE, CLASS_NAME, "getURIEncoding", "encoding -->" + encoding + " decode -->" + decode);
        }

        return encoding;
    }

    public boolean isEnableSecurityAtWARBoundary() {
        return false;
    }

    public boolean isEnableSecurityAtEARBoundary() {
        return false;
    }

    // begin 272738    Duplicate CacheServletWrappers when url-rewriting is enabled    WAS.webcontainer
    private PathInfoHelper removeExtraPathInfo(String pathInfo) {
        if (pathInfo == null)
            return null;

        int semicolon = pathInfo.indexOf(';');
        if (semicolon != -1) {
            String tmpPathInfo = pathInfo.substring(0, semicolon);
            String extraPathInfo = pathInfo.substring(semicolon);
            return new PathInfoHelper(tmpPathInfo, extraPathInfo);
        }
        return new PathInfoHelper(pathInfo, null);

    }

    private class PathInfoHelper {
        private String basePath = null;
        private String extraPathInfo = null;

        private PathInfoHelper(String pathInfo, String extraPathInfo) {
            this.basePath = pathInfo;
            this.extraPathInfo = extraPathInfo;
        }

        private String getExtraPathInfo() {
            return extraPathInfo;
        }

        private String getBasePath() {
            return basePath;
        }
    }

    // end 272738    Duplicate CacheServletWrappers when url-rewriting is enabled    WAS.webcontainer

    // LIDB3816

    public static Iterator getCachedServletWrapperNames() {
        // return _cacheMap.keySet().iterator(); //316624
        Set cacheSet = _cacheMap.keySet(); // 316624
        List l = new ArrayList(cacheSet); // 316624
        return l.listIterator(); // 316624
    }

    protected boolean isVHostCompatFlag() {
        return this.vHostCompatFlag;
    }

    protected void setVHostCompatFlag(boolean hostCompatFlag) {
        this.vHostCompatFlag = hostCompatFlag;
    }

    public Iterator<VirtualHost> getVirtualHosts() {
        return this.getTargetMappings();
    }

    //This is added so that if things go horribly wrong - we can call this to send a generic exception page to the caller
    //This was a problem when an application was stopped while processing a request.  We would get to the WebApp's sendError
    //and throw a NPE because we couldn't get the application's configuration
    public static void sendAppUnavailableException(HttpServletRequest req, HttpServletResponse res) throws IOException {
        
        if ((req instanceof SRTServletRequest) && (res instanceof SRTServletResponse)) {
            IRequest ireq = ((SRTServletRequest) req).getIRequest();
            IResponse ires = ((SRTServletResponse) res).getIResponse();
            sendUnavailableException(ireq, ires);
        }
    }
    
    // 325429
    // 582053 change it to protected from private
    protected static void sendUnavailableException(IRequest req, IResponse res) throws IOException {

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) //306998.15
            logger.logp(Level.FINE, CLASS_NAME, "sendUnavailableException", "Inside sendUnavailableException");
        res.addHeader("Content-Type", "text/html");
        res.setStatusCode(503);
        //Translated to SRVE0095I: Servlet has become temporarily unavailable for service: {0}
        String formattedMessage = nls.getFormattedMessage("Servlet.has.become.temporarily.unavailable.for.service:.{0}", new Object[] { truncateURI(req.getRequestURI()) },
                        "Servlet has become temporarily unavailable for service");
        
        String output = "<H1>"
                        + formattedMessage
                        + "</H1><BR>";

        byte[] outBytes = output.getBytes();
        res.getOutputStream().write(outBytes, 0, outBytes.length);

        logger.logp(Level.SEVERE, CLASS_NAME, "sendUnavailableException", formattedMessage );
    }

    // 582053 Start
    public synchronized void setWebContainerStopping(boolean isStopped) {
        try {
            readWriteLockForStopping.writeLock().lock();
            this.isStopped = isStopped;
        } finally {
            readWriteLockForStopping.writeLock().unlock();
        }
    }

    // 582053 End

    public static boolean isDefaultTempDir() {
        return isDefaultTempDir;
    }

    public static void setIsDefaultTempDir(boolean isTempDir) {
        isDefaultTempDir = isTempDir;
    }

    // Begin 252090, follow up to zos temp dir, don't punish single servant users
    public static String getTempDir() {
        return tempDir;
    }

    public static void setTempDir(String tempD) {
        tempDir = tempD;
    }

    public IPlatformHelper getPlatformHelper() {
        if (platformHelper == null) {
            platformHelper = new PlatformHelper();
        }
        return platformHelper;
    }

    // End 252090

    protected static boolean isServletCachingInitNeeded() {
        return servletCachingInitNeeded;
    }

    public static void setServletCachingInitNeeded(boolean bool) {
        servletCachingInitNeeded = bool;
    }

    public boolean isCachingEnabled() {
        return false;
    }

    public ClassLoader getExtClassLoader() {
        return WebContainer.class.getClassLoader();
    }

    public static synchronized Iterator<ServletContainerInitializer> getServletContainerInitializers() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) //306998.15
            logger.logp(Level.FINE, CLASS_NAME, "getServletContainerInitializers", "servletContainerInitializers->" + servletContainerInitializers.iterator());
        return servletContainerInitializers.iterator();
    }

    public abstract void decrementNumRequests();
    
    public abstract AsyncContextFactory getAsyncContextFactory();
    
    // Servlet 4.0
    public abstract URIMatcherFactory getURIMatcherFactory();

    // ================== CLASS ================== 721610
    private static class ReadCipherBitSize {
        private static HashMap<String, Integer> cipherData = new HashMap<String, Integer>();

        static {

            //Summary of JSSE cipher suites and there strengths

//    		cipherData.put("_AES_256_", 256);
//    		cipherData.put("_3DES_", 168);
//    		cipherData.put("_AES_128_", 128);
//    		cipherData.put("_RC4_128_", 128);
//    		cipherData.put("_DES_", 56);
//    		cipherData.put("_RC4_40_", 40);
//    		cipherData.put("_DES40_", 40);
//    		cipherData.put("_NULL_", 0);

            //JSSE supported ciphers

            // _AES_256_ is 256

            cipherData.put("SSL_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384", 256);
            cipherData.put("SSL_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384", 256);
            cipherData.put("SSL_ECDHE_ECDSA_WITH_AES_256_CBC_SHA", 256);
            cipherData.put("SSL_ECDHE_RSA_WITH_AES_256_GCM_SHA384", 256);
            cipherData.put("SSL_ECDHE_RSA_WITH_AES_256_CBC_SHA384", 256);
            cipherData.put("SSL_ECDHE_RSA_WITH_AES_256_CBC_SHA", 256);

            cipherData.put("SSL_RSA_WITH_AES_256_GCM_SHA384", 256);
            cipherData.put("SSL_RSA_WITH_AES_256_CBC_SHA384", 256);
            cipherData.put("SSL_RSA_WITH_AES_256_CBC_SHA", 256);

            cipherData.put("SSL_ECDH_ECDSA_WITH_AES_256_GCM_SHA384", 256);
            cipherData.put("SSL_ECDH_ECDSA_WITH_AES_256_CBC_SHA384", 256);
            cipherData.put("SSL_ECDH_ECDSA_WITH_AES_256_CBC_SHA", 256);
            cipherData.put("SSL_ECDH_RSA_WITH_AES_256_GCM_SHA384", 256);
            cipherData.put("SSL_ECDH_RSA_WITH_AES_256_CBC_SHA384", 256);
            cipherData.put("SSL_ECDH_RSA_WITH_AES_256_CBC_SHA", 256);
            cipherData.put("SSL_ECDH_anon_WITH_AES_256_CBC_SHA", 256);

            cipherData.put("SSL_DHE_DSS_WITH_AES_256_GCM_SHA384", 256);
            cipherData.put("SSL_DHE_DSS_WITH_AES_256_CBC_SHA256", 256);
            cipherData.put("SSL_DHE_DSS_WITH_AES_256_CBC_SHA", 256);

            cipherData.put("SSL_DHE_RSA_WITH_AES_256_GCM_SHA384", 256);
            cipherData.put("SSL_DHE_RSA_WITH_AES_256_CBC_SHA256", 256);
            cipherData.put("SSL_DHE_RSA_WITH_AES_256_CBC_SHA", 256);

            cipherData.put("SSL_DH_anon_WITH_AES_256_CBC_SHA", 256);
            cipherData.put("SSL_DH_anon_WITH_AES_256_GCM_SHA384", 256);
            cipherData.put("SSL_DH_anon_WITH_AES_256_CBC_SHA256", 256);

            // _3DES_ is 168
            cipherData.put("SSL_RSA_FIPS_WITH_3DES_EDE_CBC_SHA", 168);
            cipherData.put("SSL_RSA_FIPS_WITH_3DES_EDE_CBC_SHA", 168);

            cipherData.put("SSL_RSA_WITH_3DES_EDE_CBC_SHA", 168);
            cipherData.put("SSL_RSA_WITH_3DES_EDE_CBC_SHA", 168);

            cipherData.put("SSL_DH_DSS_WITH_3DES_EDE_CBC_SHA", 168);
            cipherData.put("SSL_DH_RSA_WITH_3DES_EDE_CBC_SHA", 168);
            cipherData.put("SSL_DH_anon_WITH_3DES_EDE_CBC_SHA", 168);

            cipherData.put("SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA", 168);
            cipherData.put("SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA", 168);
            cipherData.put("SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA", 168);

            cipherData.put("SSL_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA", 168);
            cipherData.put("SSL_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA", 168);

            cipherData.put("SSL_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA", 168);
            cipherData.put("SSL_ECDH_RSA_WITH_3DES_EDE_CBC_SHA", 168);
            cipherData.put("SSL_ECDH_anon_WITH_3DES_EDE_CBC_SHA", 168);

            cipherData.put("SSL_DH_anon_WITH_3DES_EDE_CBC_SHA", 168);
            cipherData.put("SSL_DH_anon_WITH_DES_EDE_CBC_SHA", 168);

            cipherData.put("SSL_KRB5_WITH_3DES_EDE_CBC_SHA", 168);
            cipherData.put("SSL_KRB5_WITH_3DES_EDE_CBC_MD5", 168);

            // _AES_128_ is 128

            cipherData.put("SSL_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256", 128);
            cipherData.put("SSL_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256", 128);
            cipherData.put("SSL_ECDHE_ECDSA_WITH_AES_128_CBC_SHA", 128);
            cipherData.put("SSL_ECDHE_RSA_WITH_AES_128_GCM_SHA256", 128);
            cipherData.put("SSL_ECDHE_RSA_WITH_AES_128_CBC_SHA256", 128);
            cipherData.put("SSL_ECDHE_RSA_WITH_AES_128_CBC_SHA", 128);

            cipherData.put("SSL_RSA_WITH_AES_128_GCM_SHA256", 128);
            cipherData.put("SSL_RSA_WITH_AES_128_CBC_SHA256", 128);
            cipherData.put("SSL_RSA_WITH_AES_128_CBC_SHA", 128);

            cipherData.put("SSL_ECDH_ECDSA_WITH_AES_128_GCM_SHA256", 128);
            cipherData.put("SSL_ECDH_ECDSA_WITH_AES_128_CBC_SHA256", 128);
            cipherData.put("SSL_ECDH_ECDSA_WITH_AES_128_CBC_SHA", 128);
            cipherData.put("SSL_ECDH_RSA_WITH_AES_128_GCM_SHA256", 128);
            cipherData.put("SSL_ECDH_RSA_WITH_AES_128_CBC_SHA256", 128);
            cipherData.put("SSL_ECDH_RSA_WITH_AES_128_CBC_SHA", 128);

            cipherData.put("SSL_DHE_RSA_WITH_AES_128_GCM_SHA256", 128);
            cipherData.put("SSL_DHE_RSA_WITH_AES_128_CBC_SHA256", 128);
            cipherData.put("SSL_DHE_RSA_WITH_AES_128_CBC_SHA", 128);

            cipherData.put("SSL_DHE_DSS_WITH_AES_128_GCM_SHA256", 128);
            cipherData.put("SSL_DHE_DSS_WITH_AES_128_CBC_SHA256", 128);
            cipherData.put("SSL_DHE_DSS_WITH_AES_128_CBC_SHA", 128);

            cipherData.put("SSL_DH_anon_WITH_AES_128_CBC_SHA", 128);
            cipherData.put("SSL_DH_anon_WITH_AES_128_GCM_SHA256", 128);
            cipherData.put("SSL_DH_anon_WITH_AES_128_CBC_SHA256", 128);

            cipherData.put("SSL_ECDH_anon_WITH_AES_128_CBC_SHA", 128);

            // _RC4_128_ is 128
            cipherData.put("SSL_RSA_WITH_RC4_128_MD5", 128);
            cipherData.put("SSL_RSA_WITH_RC4_128_SHA", 128);
            cipherData.put("SSL_DHE_DSS_WITH_RC4_128_SHA", 128);

            cipherData.put("SSL_ECDHE_ECDSA_WITH_RC4_128_SHA", 128);
            cipherData.put("SSL_ECDHE_RSA_WITH_RC4_128_SHA", 128);
            cipherData.put("SSL_ECDH_ECDSA_WITH_RC4_128_SHA", 128);
            cipherData.put("SSL_ECDH_RSA_WITH_RC4_128_SHA", 128);

            cipherData.put("SSL_DH_anon_WITH_RC4_128_MD5", 128);
            cipherData.put("SSL_ECDH_anon_WITH_RC4_128_SHA", 128);
            cipherData.put("SSL_KRB5_WITH_RC4_128_SHA", 128);
            cipherData.put("SSL_KRB5_WITH_RC4_128_MD5", 128);

            // _DES_ is 56
            cipherData.put("SSL_RSA_FIPS_WITH_DES_CBC_SHA", 56);
            cipherData.put("SSL_RSA_FIPS_WITH_DES_EDE_CBC_SHA", 56);
            cipherData.put("SSL_DH_DSS_WITH_DES_CBC_SHA", 56);
            cipherData.put("SSL_DH_RSA_WITH_DES_CBC_SHA", 56);
            cipherData.put("SSL_DHE_DSS_WITH_DES_CBC_SHA", 56);
            cipherData.put("SSL_DHE_RSA_WITH_DES_CBC_SHA", 56);
            cipherData.put("SSL_DH_anon_WITH_DES_CBC_SHA", 56);
            cipherData.put("SSL_RSA_WITH_DES_CBC_SHA", 56);
            cipherData.put("SSL_RSA_FIPS_WITH_DES_EDE_CBC_SHA", 56);
            cipherData.put("SSL_DHE_RSA_WITH_DES_CBC_SHA", 56);
            cipherData.put("SSL_DHE_DSS_WITH_DES_CBC_SHA", 56);
            cipherData.put("SSL_KRB5_WITH_DES_CBC_SHA", 56);
            cipherData.put("SSL_KRB5_WITH_DES_CBC_MD5", 56);
            cipherData.put("SSL_KRB5_EXPORT_WITH_DES_CBC_40_SHA", 56);
            cipherData.put("SSL_KRB5_EXPORT_WITH_DES_CBC_40_MD5", 56);

            // _RC4_40_ is 40	
            cipherData.put("SSL_RSA_EXPORT_WITH_RC4_40_MD5", 40);
            cipherData.put("SSL_DH_anon_EXPORT_WITH_RC4_40_MD5", 40);
            cipherData.put("SSL_KRB5_EXPORT_WITH_RC4_40_SHA", 40);
            cipherData.put("SSL_KRB5_EXPORT_WITH_RC4_40_MD5", 40);

            // _DES40_ is 40
            cipherData.put("SSL_RSA_EXPORT_WITH_DES40_CBC_SHA", 40);
            cipherData.put("SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA", 40);
            cipherData.put("SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA", 40);
            cipherData.put("SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA", 40);

            //_NULL_ is 0
            cipherData.put("SSL_RSA_WITH_NULL_SHA", 0);
            cipherData.put("SSL_RSA_WITH_NULL_SHA", 0);
            cipherData.put("SSL_RSA_WITH_NULL_SHA256", 0);
            cipherData.put("SSL_ECDH_ECDSA_WITH_NULL_SHA", 0);
            cipherData.put("SSL_ECDH_RSA_WITH_NULL_SHA", 0);
            cipherData.put("SSL_ECDHE_ECDSA_WITH_NULL_SHA", 0);
            cipherData.put("SSL_ECDHE_RSA_WITH_NULL_SHA", 0);
            cipherData.put("SSL_ECDH_anon_WITH_NULL_SHA", 0);

            // not supported after v5 ownwards, 
            cipherData.put("SSL_RSA_EXPORT_WITH_RC2_CBC_40_MD5", 40);

            // not supported , not add them
            cipherData.put("SSL_FORTEZZA_KEA_WITH_NULL_SHA", 0);
            // cipherData.put("SSL_FORTEZZA_KEA_WITH_FORTEZZA_CBC_SHA", -1); 

            // These Ciphers are part of original properties file. Keeping them

            //# SSL Version 2
            cipherData.put("RC4-MD5", 128);
            cipherData.put("EXP-RC4-MD5", 128);
            cipherData.put("RC2-MD5", 128);
            cipherData.put("EXP-RC2-MD5", 128);
            cipherData.put("IDEA-CBC-MD5", 128);
            cipherData.put("DES-CBC-MD5", 64);
            cipherData.put("DES-CBC3-MD5", 192);

            //# SSL Version 3 and TLS Version 1
            cipherData.put("NULL-MD5", 0);
            cipherData.put("NULL-SHA", 0);
            cipherData.put("EXP-RC4-MD5", 40);
            cipherData.put("RC4-MD5", 128);
            cipherData.put("RC4-SHA", 128);
            cipherData.put("EXP-RC2-CBC-MD5", 40);
            cipherData.put("IDEA-CBC-SHA", 128);
            cipherData.put("EXP-DES-CBC-SHA", 40);
            cipherData.put("DES-CBC-SHA", 56);
            cipherData.put("DES-CBC3-SHA", 168);

            cipherData.put("EXP-EDH-DSS-DES-CBC-SHA", 40);
            cipherData.put("EDH-DSS-CBC-SHA", 56);
            cipherData.put("EDH-DSS-DES-CBC3-SHA", 168);
            cipherData.put("EXP-EDH-RSA-DES-CBC-SHA", 40);
            cipherData.put("EDH-RSA-DES-CBC-SHA", 56);
            cipherData.put("EDH-RSA-DES-CBC3-SHA", 168);

            cipherData.put("EXP-ADH-RC4-MD5", 40);
            cipherData.put("ADH-RC4-MD5", 128);
            cipherData.put("EXP-ADH-DES-CBC-SHA", 40);
            cipherData.put("ADH-DES-CBC-SHA", 56);
            cipherData.put("ADH-DES-CBC3-SHA", 168);

            //# AES ciphersuites from RFC3268, extending TLS Version 1
            cipherData.put("AES128-SHA", 128);
            cipherData.put("AES256-SHA", 256);

            cipherData.put("DH-DSS-AES128-SHA", 128);
            cipherData.put("DH-DSS-AES256-SHA", 256);
            cipherData.put("DH-RSA-AES128-SHA", 128);
            cipherData.put("DH-RSA-AES256-SHA", 256);

            cipherData.put("DHE-DSS-AES128-SHA", 128);
            cipherData.put("DHE-DSS-AES256-SHA", 256);
            cipherData.put("DHE-RSA-AES128-SHA", 128);
            cipherData.put("DHE-RSA-AES256-SHA", 256);

            cipherData.put("ADH-AES128-SHA", 128);
            cipherData.put("ADH-AES256-SHA", 256);

            // # Additional Export 1024 and other cipher suites
            cipherData.put("EXP1024-DES-CBC-SHA", 56);
            cipherData.put("EXP1024-RC4-SHA", 56);
            cipherData.put("EXP1024-DHE-DSS-DES-CBC-SHA", 56);
            cipherData.put("EXP1024-DHE-DSS-RC4-SHA", 56);
            cipherData.put("DHE-DSS-RC4-SHA", 128);

            // Cipher list provided by GSkit

            // Allowed SSLV30:    		
            cipherData.put("TLS_RSA_WITH_NULL_NULL", 0); // Default TLSV12          
            cipherData.put("TLS_RSA_WITH_NULL_MD5", 0); // Default SSLV30,  Default TLSV11 ,  Default TLSV10,  
            cipherData.put("TLS_RSA_WITH_NULL_SHA", 0); // Default SSLV30, Default TLSV11,  Default TLSV10, 

            cipherData.put("TLS_RSA_EXPORT_WITH_RC4_40_MD5", 40); // Default SSLV30, Default TLSV10, 

            cipherData.put("TLS_RSA_WITH_RC4_128_MD5", 128); //  Default TLSV11 , Default TLSV10, 
            cipherData.put("TLS_RSA_WITH_RC4_128_SHA", 128); // Default SSLV30, , Default TLSV12 ,Default TLSV11 

            cipherData.put("TLS_RSA_EXPORT_WITH_RC2_CBC_40_MD5,", 40); // Default SSLV30, Default TLSV10, 

            cipherData.put("TLS_RSA_WITH_DES_CBC_SHA", 56); // Default SSLV30, Default TLSV11 ,Default TLSV10, 

            cipherData.put("TLS_RSA_WITH_3DES_EDE_CBC_SHA", 168); // Default SSLV30, Allowed TLSV10, Default TLSV10,  FIPS Allowed  TLSV10, Allowed TLSV11

            cipherData.put("TLS_RSA_EXPORT1024_WITH_DES_CBC_SHA", 56);//  (Deprecated)  // Default SSLV30, Default TLSV10, 
            cipherData.put("TLS_RSA_EXPORT1024_WITH_RC4_56_SHA", 56); //(Deprecated)  // Default SSLV30, Default TLSV10, 

            cipherData.put("SSL_RSA_FIPS_WITH_DES_CBC_SHA", 56); //(Deprecated)  // Default SSLV30,
            cipherData.put("SSL_RSA_FIPS_WITH_3DES_EDE_CBC_SHA", 168); //(Deprecated) // Default SSLV30, Default TLSV10, Default TLSV11, FIPS Allowed  TLSV10, FIPS Allowed TLSV11

            cipherData.put("TLS_RSA_WITH_AES_128_CBC_SHA", 128); // Default SSLV30, Default TLSV10, Default TLSV11, FIPS Allowed  TLSV10, FIPS Allowed TLSV11
            cipherData.put("TLS_RSA_WITH_AES_256_CBC_SHA", 256); // Default SSLV30, Default TLSV10, Default TLSV11, FIPS Allowed  TLSV10, FIPS Allowed TLSV11

            // Default SSLV30 CipherSpecs: All are added above

            //	FIPS Allowed  SSLV30 CipherSpecs://None

            //	Suite B  Allowed SSLV30 CipherSpecs://None

            //	Allowed TLSV10 CipherSpecs: //same list as Allowed SSLV30 

            //Default TLSV10 CipherSpecs: //same list as Default SSLV30 

            //FIPS Allowed  TLSV10 CipherSpecs: // included above with Default SSLV30

            //Suite B  Allowed TLSV10 CipherSpecs: //None

            //Allowed TLSV11 CipherSpecs: //same list as Default SSLV30 

            //Default TLSV11 CipherSpecs:

            //FIPS Allowed  TLSV11 CipherSpecs:

            //Suite B  Allowed TLSV11 CipherSpecs: //None

            //Allowed TLSV12 CipherSpecs:

            cipherData.put("TLS_ECDHE_RSA_WITH_RC4_128_SHA", 128);
            cipherData.put("TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA", 168);
            cipherData.put("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA", 128);
            cipherData.put("TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA", 256);
            cipherData.put("TLS_ECDHE_ECDSA_WITH_RC4_128_SHA", 128);
            cipherData.put("TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA", 168);

            //Default TLSV12 CipherSpecs:

            cipherData.put("TLS_RSA_WITH_AES_128_GCM_SHA256", 128);
            cipherData.put("TLS_RSA_WITH_AES_256_GCM_SHA384", 256);
            cipherData.put("TLS_RSA_WITH_AES_128_CBC_SHA256", 128);
            cipherData.put("TLS_RSA_WITH_AES_256_CBC_SHA256", 256);
            cipherData.put("TLS_RSA_WITH_AES_128_CBC_SHA", 128);
            cipherData.put("TLS_RSA_WITH_AES_256_CBC_SHA", 256);
            cipherData.put("TLS_RSA_WITH_3DES_EDE_CBC_SHA", 168);
            cipherData.put("TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA", 128);
            cipherData.put("TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA", 256);
            cipherData.put("TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256", 128);
            cipherData.put("TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384", 256);
            cipherData.put("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256", 128);
            cipherData.put("TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384", 256);
            cipherData.put("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256", 128);
            cipherData.put("TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384", 256);
            cipherData.put("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", 128);
            cipherData.put("TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384", 256);

            cipherData.put("TLS_RSA_WITH_NULL_SHA256", 0);
            cipherData.put("TLS_RSA_WITH_NULL_SHA", 0);
            cipherData.put("TLS_ECDHE_RSA_WITH_NULL_SHA", 0);
            cipherData.put("TLS_ECDHE_ECDSA_WITH_NULL_SHA", 0);

            //FIPS Allowed  TLSV12 CipherSpecs: All are included above

            //Suite B  Allowed TLSV12 CipherSpecs: All are included above

        }

    }
    // ================== CLASS ==================
}
