/*******************************************************************************
 * Copyright (c) 1997,2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

// Change History
// Defect 217993 "JSPExtProcessor should not create a servlet wrapper" 2004/07/22 Scott Johnson
// Defect 223399 "BVT:JSPG0036E: Failed to find resource /secure/logon.jsp" 2004/08/11 Scott Johnson
// Defect 225901 "Support for pluggable JSP translation context class is broken when filesystem check is performed." 2004/08/20 Scott Johnson
// Defect 247773 SyncToOsThread changes, must do syncToOsThread around case sensitivity check 2005/01/14 Maxim Moldenhauer
// APAR   PK01617 "JspConfiguration object cached within the JSPExtensionServletWrapper class causing problems with trackDependencies" 2005/03/11 Shari McGuirl
// Defect 268095  "remove dependency on IExtensionProcessorExtended" 2005/04/13 Todd Kaplinger
// Defect 315405  2005/10/21  jsp container fails to call JSPErrorReport.setTargetServletName
// APAR PK13570    WITH SYNCTOOSTHREAD ENABLED, SIMPLE FILES (.GIF, .HTML, ETC...) Maxim Moldenhauer 2006/01/03
// feature LIDB4147-9 "Integrate Unified Expression Language"  Remove references to org.apache.commons.el.ExpressionEvaluatorImpl 
//                                       2006/08/14  Scott Johnson
// APAR PK27620   SERVLET FILTER IS NOT CALLED IN V6 FOR URL RESOURCES WHEN THESE ARE NOT FOUND.  IN V5, THE FILTER IS ALWAYS CALLED  Curtiss Howard 2006/08/23
// Feature LIDB4293-2 - "In-memory translation/compilation of JSPs" 2006/11/11 Scott Johnson
//415289 70FVT:useinmemory: Error received when hitting non-existent jsp    2007/01/17 09:32:07  Scott Johnson
// defect 395182.2  70FVT: make servlet 2.3 compatible with JSP 2.1 for migration 2007/02/07 Scott Johnson
// APAR PK31377   Servlet filter is not called for URL resources  Jay Sartoris 04/13/07
// Defect 437922  Non-existent JSP responds with 200 Error Code  Jay Sartoris 05/09/07
//APAR   PK45107 2007/08/07  EXCEPTION FROM SERVLET FILTER IS NOT PROPAGATED TO CLIENT - Maxim Moldenhauer
// APAR PK57843   THE JSP:INCLUDE TAG DOES NOT THROW EXCEPTION WHEN THE FILE IT - Seth Peterson 03/12/08    
// APAR PK81387   Possible Security exposure  Jay Sartoris 2009/03/02
// APAR PK83043   InvokeFiltersCompatibility set but no defined filter  will return 200 for non-existing JSP
// APAR PK80353   Top level JSP throws a 500 when it does not exist instead of 404 when throwMissingJSPException is set 05/08/09      sartoris
// APAR PK87901   When PrepareJsp requests are processed we must allow access to WEB-INF dir - Jay Sartoris 07/30/2009
// APAR PM10362   Provide option for tolerateLocaleMismatchForServingFiles in PK81387 code - Anup Aggarwal 04/26/2010
// APAR PI87565   Avoid creating a ServletConfig if one already exist for a particular jsp - Harold Padilla 09/27/2017

package com.ibm.ws.jsp.webcontainerext;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.tools.ToolProvider;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.JspOptions;
import com.ibm.ws.jsp.configuration.JspConfigurationManager;
import com.ibm.ws.jsp.configuration.JspXmlExtConfig;
import com.ibm.ws.jsp.inmemory.context.InMemoryJspTranslationContext;
import com.ibm.ws.jsp.inputsource.JspInputSourceFactoryImpl;
import com.ibm.ws.jsp.runtime.ContextListener;
import com.ibm.ws.jsp.taglib.GlobalTagLibraryCache;
import com.ibm.ws.jsp.taglib.TagLibraryCache;
import com.ibm.ws.jsp.taglib.TagLibraryInfoProxy;
import com.ibm.ws.jsp.taglib.annotation.AnnotationHandler;
import com.ibm.ws.jsp.translator.compiler.JDTCompilerFactory;
import com.ibm.ws.jsp.translator.compiler.JspCompilerFactoryImpl;
import com.ibm.ws.jsp.translator.env.JspTranslationEnvironmentImpl;
import com.ibm.ws.jsp.translator.resource.JspResourcesFactoryImpl;
import com.ibm.ws.jsp.webcontainerext.ws.PrepareJspHelper;
import com.ibm.ws.jsp.webcontainerext.ws.PrepareJspHelperFactory;
import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;
import com.ibm.ws.util.WSUtil;
import com.ibm.ws.webcontainer.util.DocumentRootUtils;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.jsp.compiler.JspCompilerFactory;
import com.ibm.wsspi.jsp.context.JspClassloaderContext;
import com.ibm.wsspi.jsp.context.translation.JspTranslationContext;
import com.ibm.wsspi.jsp.resource.JspInputSource;
import com.ibm.wsspi.jsp.resource.JspInputSourceFactory;
import com.ibm.wsspi.jsp.resource.translation.JspResources;
import com.ibm.wsspi.webcontainer.RequestProcessor;
import com.ibm.wsspi.webcontainer.WCCustomProperties;
import com.ibm.wsspi.webcontainer.collaborator.ICollaboratorHelper;
import com.ibm.wsspi.webcontainer.collaborator.IWebAppNameSpaceCollaborator;
import com.ibm.wsspi.webcontainer.extension.ExtensionProcessor;
import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;
import com.ibm.wsspi.webcontainer.servlet.IServletWrapper;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;
import com.ibm.ws.webcontainer.webapp.WebAppConfiguration;
import com.ibm.ws.webcontainer.servlet.IServletContextExtended;

public abstract class AbstractJSPExtensionProcessor extends com.ibm.ws.webcontainer.extension.WebExtensionProcessor {
    static protected Logger logger;

    private static final String CLASS_NAME = "com.ibm.ws.jsp.webcontainerext.AbstractJSPExtensionProcessor";
    private static final long NANOS_IN_A_MILLISECONDS = 1000000L;
    static {
        logger = Logger.getLogger("com.ibm.ws.jsp");
    }

    protected static final int numSyncObjects = 41;

    protected static Object[] syncObjects;
    static {
        syncObjects = new Object[numSyncObjects];
        for (int i = 0; i < numSyncObjects; i++)
            syncObjects[i] = new Object();
    }

    static final protected Object getSyncObject(String name) {
        return syncObjects[Math.abs(name.hashCode() % numSyncObjects)];
    }

    protected JspTranslationContext context = null;
    protected TagLibraryCache tlc = null;
    protected CodeSource codeSource = null;
    protected JspOptions jspOptions = null;
    protected JspConfigurationManager jspConfigurationManager = null;
    protected JspClassloaderContext jspClassloaderContext = null;
    protected JspCompilerFactory jspCompilerFactory = null;
    protected IServletContextExtended webapp = null;

    // defect 238792: begin list of JSP mapped servlets.
    protected HashMap jspFileMappings = new HashMap();
    private IWebAppNameSpaceCollaborator webAppNameSpaceCollab;
    // defect 238792: end list of JSP mapped servlets.

    private ICollaboratorHelper collabHelper;
    private WebComponentMetaData cmd;
    
    public AbstractJSPExtensionProcessor(IServletContext webapp, 
                                         JspXmlExtConfig webAppConfig, 
                                         GlobalTagLibraryCache globalTagLibraryCache,
                                         JspClassloaderContext jspClassloaderContext) throws Exception {
        super(webapp);
        final boolean isAnyTraceEnabled = com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled();
        this.webapp = (IServletContextExtended) webapp;
        this.jspOptions = webAppConfig.getJspOptions();
        collabHelper = this.webapp.getCollaboratorHelper();
		webAppNameSpaceCollab =  collabHelper.getWebAppNameSpaceCollaborator();
        //497716.2
        //always adding the lifecycle listener so we can cleanup the AnnotationHandler
        //doing logic for using ThreadTagPool within listener
        ContextListener contextListener = new ContextListener();
        contextListener.setIsUseThreadTagPool(jspOptions.isUseThreadTagPool());
        webapp.addLifecycleListener(contextListener);
        
        if (isAnyTraceEnabled&&logger.isLoggable (Level.FINER)){
            logger.entering(CLASS_NAME,"JSPExtensionProcessor", " app contextPath --> "+ webapp.getContextPath());
        }//d651265

        try {
            String extDocumentRoot = null,preFragmentExtendedDocumentRoot = null;
            if (jspOptions != null) {
                extDocumentRoot = jspOptions.getExtendedDocumentRoot();
                preFragmentExtendedDocumentRoot = jspOptions.getPreFragmentExtendedDocumentRoot();
            }
            Map tagLibMap = webAppConfig.getTagLibMap();
            jspConfigurationManager = new JspConfigurationManager(webAppConfig.getJspPropertyGroups(), webAppConfig.isServlet24(),
                                                                  webAppConfig.isServlet24_or_higher(), webAppConfig.getJspFileExtensions(), webAppConfig.isJCDIEnabledForRuntimeCheck());
            this.jspClassloaderContext = jspClassloaderContext;
            //  defect jdkcompiler
            String docRoot = null; //call getRealPath later - if needed
            boolean docRootRealPathCalled = false;
            if (jspOptions.isUseJDKCompiler() && ToolProvider.getSystemJavaCompiler() == null) {
                jspOptions.setUseJDKCompiler(false);
                
                /*
                 * Log a message containing information about this incident.
                 * If ToolProvider.getSystemJavaCompiler() is null, we won't be able to use the JDK compiler.
                 * The way to fix it, usually, is to make sure the Java SDK tools.jar
                 * is in the classpath.
                 */
                logger.logp(Level.WARNING, CLASS_NAME, "AbstractJSPExtensionProcessor", "system.java.compiler.not.found", webapp.getWebAppConfig().getModuleName());
            }
            if (jspOptions.isUseJDKCompiler()) {
                docRoot = webapp.getRealPath("/"); //needs to get the realPath for the jdk compiler
                docRootRealPathCalled=true;
                jspCompilerFactory = new JspCompilerFactoryImpl(docRoot, jspClassloaderContext, jspOptions);
            }
            else {
                //DEFAULT CASE
            	jspCompilerFactory = new JDTCompilerFactory(jspClassloaderContext.getClassLoader(), jspOptions);
            }
            if (jspOptions.isUseInMemory()) {
            	DocumentRootUtils dru=null;
                if (extDocumentRoot != null || preFragmentExtendedDocumentRoot !=null) {
                    dru = new DocumentRootUtils(webapp, extDocumentRoot,preFragmentExtendedDocumentRoot);
                }
                context = new InMemoryJspTranslationContext(webapp, jspOptions, extDocumentRoot, preFragmentExtendedDocumentRoot);
                URL contextURL = new File(webapp.getRealPath("/")).toURL();
                if (!docRootRealPathCalled) docRoot = webapp.getRealPath("/");
                JspInputSourceFactory tempInputSourceFactory = new JspInputSourceFactoryImpl(docRoot,contextURL, dru, false, webapp.getModuleContainer(), jspClassloaderContext.getClassLoader(),webapp);
                JspResourcesFactoryImpl tempResourceFactory = new JspResourcesFactoryImpl(jspOptions, context, webapp.getModuleContainer());
                JspTranslationEnvironmentImpl jspEnvironment = new JspTranslationEnvironmentImpl(jspOptions.getOutputDir().getPath(), webapp.getContextPath(), tempInputSourceFactory,
                                                                                                 tempResourceFactory, jspClassloaderContext, jspCompilerFactory);
       			context.setJspTranslationEnviroment(jspEnvironment);
                
            } else {
	            if (jspOptions.getTranslationContextClass() != null) {
	                context = loadTranslationContext(jspOptions.getTranslationContextClass(), webapp, jspOptions.getOutputDir().getPath(), webapp.getContextPath());
	            } else {
	                context = new JSPExtensionContext(webapp, jspOptions, extDocumentRoot, preFragmentExtendedDocumentRoot, jspClassloaderContext, jspCompilerFactory);
	            }
            }
            List eventListenerList = new ArrayList();
            eventListenerList.addAll(globalTagLibraryCache.getEventListenerList());

            Map globalTagLibMap = globalTagLibraryCache.getGlobalTagLibMapForWebApp(context, webAppConfig);

            for (Iterator itr = globalTagLibMap.values().iterator(); itr.hasNext();) {
                Object o = itr.next();
                if (o instanceof TagLibraryInfoProxy) {
                    TagLibraryInfoProxy proxy = (TagLibraryInfoProxy) o;
                    if (proxy.containsListenerDefs()) {
                        eventListenerList.addAll(proxy.getEventListenerList());
                    }
                }
            }

            long start = System.nanoTime();
            tlc = new TagLibraryCache(context, 
                                      tagLibMap, 
                                      jspOptions, 
                                      jspConfigurationManager, 
                                      globalTagLibMap, 
                                      globalTagLibraryCache.getImplicitTagLibPrefixMap(), 
                                      globalTagLibraryCache.getOptimizedTagConfigMap(),
                                      (WebAppConfiguration) webapp.getWebAppConfig());
            long end = System.nanoTime();
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "JSPExtensionProcessor", "TagLibraryCache created in " + ((end - start) / NANOS_IN_A_MILLISECONDS) + " ms");
            }
            eventListenerList.addAll(tlc.getEventListenerList());
            
            // Begin LIDB4147-24
            // Collect the TLD event listener classes and make sure they're scanned for
            // annotations.
            
            AnnotationHandler annotationHandler = AnnotationHandler.getInstance (webapp);
            List<Class<?>> eventListenerClasses = new LinkedList<Class<?>>();
            
            for (Iterator itr = eventListenerList.iterator(); itr.hasNext();) {
                 String eventListenerClassName = (String) itr.next();
                 try {
                     eventListenerClasses.add (Class.forName(eventListenerClassName, true,
                             context.getJspClassloaderContext().getClassLoader()));
                 }
                 
                 catch (ClassNotFoundException e) {
                      com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.jsp.webcontainerext.JspExtensionProcessor.init", "89", this);
                      if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.WARNING)) {
                          logger.logp(Level.WARNING, CLASS_NAME, "JSPExtensionProcessor", "jsp error failed to load event listener class = ["
                                  + eventListenerClassName + "]", e);
                      }
                  }
            }
            
            //annotationHandler.performScan (eventListenerClasses);
            
            // End LIDB4147-24
            
            for (Class<?> eventListenerClass : eventListenerClasses) {
                //663071 moved loadListeners after this call for 661473... just add the listener to the list now 
                webapp.getWebAppConfig().addListener(eventListenerClass.getName());
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "JSPExtensionProcessor", "Added Event Listener [{0}] to listeners list", eventListenerClass.getName());
                }
            }
        } catch (JspCoreException e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.jsp.webcontainerext.JSPExtensionProcessor.init", "97", this);
            if (isAnyTraceEnabled&&logger.isLoggable (Level.FINER))
                logger.exiting(CLASS_NAME,"JSPExtensionProcessor", "app contextPath --> "+ webapp.getContextPath()); //d651265
            if (e.getCause() != null)
                throw new ServletException(e.getLocalizedMessage(), e.getCause());
            else
                throw new ServletException(e.getLocalizedMessage());
        }

        if (webapp.getModuleContainer()!=null) {
            //I believe this is used to get the policy file in the app, this should be changed to pass in an Entry
            //this isn't in the alpha
            //TODO: uncomment this code and call real method
            //Entry rootEntry = webapp.getModuleContainer().getEntry("/");
            //rootEntry.
            Collection<URL> map = webapp.getModuleContainer().getURLs();
            Iterator<URL> it = map.iterator();
            if (it.hasNext()) {
                URL url = it.next(); //only gets the first one...
                if (url!=null) {
                    codeSource = new CodeSource(url, (java.security.cert.Certificate[])null);
                }
            }
        } else {
            try {
                String contextDir = context.getRealPath("/");
                URL url = new URL("file:" + contextDir);
                codeSource = new CodeSource(url, (java.security.cert.Certificate[])null);
            } catch (MalformedURLException e) {
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.jsp.webcontainerext.JSPExtensionProcessor.init", "112", this);
                if (isAnyTraceEnabled&&logger.isLoggable (Level.FINER))
                    logger.exiting(CLASS_NAME,"JSPExtensionProcessor", "app contextPath --> "+ webapp.getContextPath()); //d651265
                throw new ServletException(e);
            }
        }       


        if (isAnyTraceEnabled&&logger.isLoggable(Level.FINER))
            logger.exiting(CLASS_NAME,"JSPExtensionProcessor", "app contextPath --> "+ webapp.getContextPath()); //d651265
    }

    public IServletWrapper createServletWrapper(IServletConfig config) throws Exception {
        final boolean isAnyTraceEnabled = com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled();
        if (isAnyTraceEnabled && logger.isLoggable(Level.FINER)) {
            logger.entering(CLASS_NAME, "createServletWrapper");
        }
        JSPExtensionServletWrapper jspServletWrapper = new JSPExtensionServletWrapper(extensionContext,
                                                                                      jspOptions,
                                                                                      jspConfigurationManager,
                                                                                      tlc,
                                                                                      context,
                                                                                      codeSource);
        jspServletWrapper.initialize(config);
        if (isAnyTraceEnabled && logger.isLoggable(Level.FINER))
            logger.exiting(CLASS_NAME, "createServletWrapper"); //d651265
        return jspServletWrapper;
    }

    @Override
    @FFDCIgnore(value = { JspCoreException.class })
    public IServletWrapper getServletWrapper(ServletRequest req,
                                             ServletResponse resp) throws Exception {
        final boolean isAnyTraceEnabled = com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled();
        if (isAnyTraceEnabled && logger.isLoggable(Level.FINER)) {
            logger.entering(CLASS_NAME, "getServletWrapper",
                            "enter getServletWrapper(ServletRequest req, ServletResponse res)");
        }
        if (req instanceof HttpServletRequest) {
            HttpServletRequest httpReq = (HttpServletRequest) req;
            try {
                IServletWrapper jspServletWrapper = findWrapper(httpReq, (HttpServletResponse) resp);
                if (jspServletWrapper != null) {
                    // @BLB make sure the wrapper is JSPExtensionServletWrapper
                    if (jspServletWrapper instanceof AbstractJSPExtensionServletWrapper) {
                        if (isAnyTraceEnabled && logger.isLoggable(Level.FINER)) {
                            logger.exiting(CLASS_NAME, " getServletWrapper ");
                        } //d651265
                        return jspServletWrapper;
                    } else {
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.INFO)) {
                            logger.logp(Level.INFO, CLASS_NAME, "getServletWrapper", "wrapper is not of type JspExtensionServletWrapper.  Type is ["
                                                                                     + jspServletWrapper + "]");
                        }
                    }
                }
            } catch (JspCoreException e) {

                int code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                if (e.getCause() != null && e.getCause() instanceof java.io.FileNotFoundException) {
                    code = HttpServletResponse.SC_NOT_FOUND;
                }
                Throwable t = null;
                Throwable rootCause = e;
                while ((t = rootCause.getCause()) != null) {
                    rootCause = t;
                }
                // Defect 211450
                JSPErrorReport jser = new JSPErrorReport(rootCause.getLocalizedMessage(), rootCause);
                jser.setStackTrace(rootCause.getStackTrace());
                jser.setErrorCode(code);
                IServletConfig sconfig = getConfig(httpReq);
                jser.setTargetServletName(sconfig.getServletName()); // Defect 315405                                                 

                webapp.sendError(httpReq, (HttpServletResponse) resp, jser);
            }
        }
        if (isAnyTraceEnabled && logger.isLoggable(Level.FINER)) {
            logger.exiting(CLASS_NAME, " getServletWrapper", "null");
        } //d651265
        return null;
    }

    public void handleRequest(ServletRequest req, ServletResponse res) throws Exception {
        //PM63184 Start
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "handleRequest",
                        " this method will only be called when the requested jsp resource is not found and no resource handled the request. " + req.toString());
        }
        if (req instanceof HttpServletRequest) {
            HttpServletRequest httpReq = (HttpServletRequest) req;

            if (!WCCustomProperties.THROW_MISSING_JSP_EXCEPTION) {
                JSPErrorReport jser = (JSPErrorReport) httpReq.getAttribute("jsp.resourceMissingErrorReport");
                if (jser != null) {
                    httpReq.removeAttribute("jsp.resourceMissingErrorReport");
                    throw jser;
                } else {
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                        logger.logp(Level.FINE, CLASS_NAME, "handleRequest", " no error reported , should not be here" + httpReq.toString());
                    }
                }
            } else {
                FileNotFoundException fex = (FileNotFoundException) httpReq.getAttribute("jsp.resourceMissingExcep");
                if (fex != null) {
                    httpReq.removeAttribute("jsp.resourceMissingExcep");
                    throw fex;
                } else {
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                        logger.logp(Level.FINE, CLASS_NAME, "handleRequest", " no exception, , should not be here" + httpReq.toString());
                    }
                }
            }
        }
        //PM63184 End
    }

    // @BLB changed to friendly access.
    public IServletWrapper findWrapper(HttpServletRequest httpReq, HttpServletResponse res) throws Exception {
        final boolean isAnyTraceEnabled = com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled();
        if (isAnyTraceEnabled && logger.isLoggable(Level.FINER)) {
            logger.entering(CLASS_NAME, " findWrapper ", " , request URI -> " + httpReq.getRequestURI());
        } //d651265

        IServletWrapper jspServletWrapper = null;
        IServletConfig sconfig = getConfig(httpReq);

        String filename = sconfig.getFileName();
        boolean success = true;

        //PK81387 start
        boolean checkWEBINF = true;
        //PK87901 added check for PrepareJsp requests which occur during startup.
        if ((httpReq.getAttribute(Constants.INC_REQUEST_URI) != null)
            || (httpReq.getAttribute(Constants.FORWARD_REQUEST_URI) != null)
            || (httpReq.getAttribute(Constants.ASYNC_REQUEST_URI)!=null)
            || ((jspOptions.isPrepareJSPsSet()) && (httpReq.getClass().getName().equals("com.ibm.ws.jsp.webcontainerext.ws.PrepareJspServletRequest")))) {
            checkWEBINF = false;
        }
        //PK81387 end

        if (jspOptions.isDisableJspRuntimeCompilation() == false && // 223399
            (jspOptions.getTranslationContextClass() == null // 225901 
            || (jspOptions.getTranslationContextClass() != null && // 415289
            jspOptions.getTranslationContextClass().equals(Constants.IN_MEMORY_TRANSLATION_CONTEXT_CLASS)))) {
            success = handleCaseSensitivityCheck(filename, checkWEBINF); //PK81387 - added checkWEBINF param
        }
        if (success == false) { // case sensitivity match failed
            FileNotFoundException fex = new FileNotFoundException(JspCoreException.getMsg("jsp.error.failed.to.find.resource", new Object[] { filename }));
            if (WCCustomProperties.THROW_MISSING_JSP_EXCEPTION) { //PM63184 Start
                httpReq.setAttribute("jsp.resourceMissingExcep", fex);
            } else {
                JSPErrorReport jser = new JSPErrorReport(fex.getLocalizedMessage(), fex);
                IServletWrapper filterProxyServletWrapper = null;
                jser.setStackTrace(fex.getStackTrace());
                jser.setErrorCode(HttpServletResponse.SC_NOT_FOUND);
                jser.setTargetServletName(sconfig.getServletName());//Defect 315405
                httpReq.setAttribute("jsp.resourceMissingErrorReport", jser); //PM63184
            }
            //throw jser; //685033 - throw jser rather than fex
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "findWrapper", " Unable to find wrapper , file not found--> " + fex);
            }
            return null;
            //PM63184 End
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "findWrapper", "About to enter wrapper creationg sync block for " + filename);
        }
        synchronized (getSyncObject(filename)) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "findWrapper", "Entered sync block. looking for wrapper for " + filename);
            }
            RequestProcessor rp = extensionContext.getMappingTarget(filename);
            if (rp instanceof ExtensionProcessor) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "findWrapper", "wrapper for " + filename + " is null. creating");
                }
                jspServletWrapper = createServletWrapper(sconfig);
                //must call check for translation now so that the classname will be set
                //on the WebComponentMetaData for the preInvoke of the collaborators
                if (System.getSecurityManager() != null) {
                    try {
                        final HttpServletRequest finalReq = httpReq;
                        final AbstractJSPExtensionServletWrapper finalJspServletWrapper = (AbstractJSPExtensionServletWrapper) jspServletWrapper;
                        AccessController.doPrivileged(new java.security.PrivilegedExceptionAction() {
                            public Object run() throws JspCoreException {
                                finalJspServletWrapper.checkForTranslation(finalReq);
                                return null;
                            }
                        });
                    } catch (PrivilegedActionException pae) {
                        com.ibm.ws.ffdc.FFDCFilter
                                        .processException(pae, "com.ibm.ws.jsp.webcontainerext.AbstractJSPExtensionProcessor.findWrapper", "445", this);
                        throw (JspCoreException) pae.getException();
                    }
                } else {
                    ((AbstractJSPExtensionServletWrapper) jspServletWrapper).checkForTranslation(httpReq);
                }
                try { // 234573.1: attempt to replace mapping if add fails.
                    extensionContext.addMappingTarget(filename, jspServletWrapper);
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                        logger.logp(Level.FINE, CLASS_NAME, "findWrapper", "wrapper for " + filename + " created and added to RequestMapper");
                    }
                    // 234573.1: begin attempt to replace mapping if add fails.
                } catch (Exception e) { // exception should occur if mapping
                                        // already exists for this wrapper.
                    extensionContext.replaceMappingTarget(filename, jspServletWrapper);
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                        logger.logp(Level.FINE, CLASS_NAME, "findWrapper", "wrapper for " + filename + " removed and created new wrapper for RequestMapper");
                    }
                }
                // 234573.1: end attempt to replace mapping if add fails.
            } else {
                jspServletWrapper = (IServletWrapper) rp;
            }
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "findWrapper", "Exiting wrapper creation sync block for " + filename);
        }
        if (isAnyTraceEnabled && logger.isLoggable(Level.FINER)) {
            logger.exiting(CLASS_NAME, " findWrapper ", "Exiting wrapper creation sync block for -> " + filename + " ,  request URI -> " + httpReq.getRequestURI());
        } //d651265
        return jspServletWrapper;
    }

/*
 * public List getPatternList() {
 * return jspConfigurationManager.getJspExtensionList();
 * }
 */
    // PM07560 - Start
    public List getPatternList() {
        List mappings = jspConfigurationManager.getJspExtensionList();

        if (mappings.isEmpty())
            return mappings;

        if (WCCustomProperties.ENABLE_JSP_MAPPING_OVERRIDE) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, CLASS_NAME, "getPatternList", " enters enableJSPMappingOverride is TRUE");
            }

            //WebAppConfiguration config = ((com.ibm.ws.webcontainer.webapp.WebAppConfigurationImpl)extensionContext.getWebAppConfig());
            WebAppConfig config = this.webapp.getWebAppConfig();
            Map<String, List<String>> srvMappings = config.getServletMappings();
            if (srvMappings.isEmpty()) {
                return mappings;
            }

            Iterator iSrvNames = config.getServletNames();
            List<String> newMappings = new ArrayList<String>();
            String path = null, servletName;
            HashSet<String> urlPatternSet = new HashSet<String>();

            //for every servlet name in the mapping ...
            while (iSrvNames.hasNext()) {
                servletName = (String) iSrvNames.next();
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINER)) {
                    logger.logp(Level.FINER, CLASS_NAME, "getPatternList", " servletName [" + servletName + "]");
                }

                List<String> mapList = srvMappings.get(servletName);
                if (mapList != null) {
                    // ...get all its UrlPattern and put them into a set...
                    Iterator<String> m = mapList.iterator();
                    while (m.hasNext()) {
                        path = m.next();
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINER)) {
                            logger.logp(Level.FINER, CLASS_NAME, "getPatternList", " urlPattern [" + path + "]");
                        }
                        urlPatternSet.add(path);
                    }
                }
            }
            // ... find an extension in the original list that matches the UrlPattern set
            for (Iterator it = mappings.iterator(); it.hasNext();) {
                String mapping = (String) it.next();
                int dot = -1;
                dot = mapping.lastIndexOf(".");
                if (dot != -1) {
                    String extension = "*" + mapping.substring(dot);
                    if (!urlPatternSet.contains(extension)) {
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINER)) {
                            logger.logp(Level.FINER, CLASS_NAME, "getPatternList", " no match for extension [" + extension + "], add [" + mapping + "]");
                        }
                        newMappings.add(mapping);
                    } else {
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINER)) {
                            logger.logp(Level.FINER, CLASS_NAME, "getPatternList", " found a match for extension [" + extension + "], ignore [" + mapping + "]");
                        }
                    }
                } else { // no extension...just add to the mapping
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINER)) {
                        logger.logp(Level.FINER, CLASS_NAME, "getPatternList", " no extension, add [" + mapping + "]");
                    }
                    newMappings.add(mapping);
                }
            }

            mappings = newMappings;
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, CLASS_NAME, "getPatternList", " exits enableJSPMappingOverride");
            }
        }

        return mappings;
    }

    //PM07560 - ends

    //PK81387 - added checkWEBINF param
    private boolean handleCaseSensitivityCheck(String path, boolean checkWEBINF) throws IOException {
        if (System.getSecurityManager() != null) {
            final String tmpPath = path;
            final boolean tmpCheckWEBINF = checkWEBINF; //PK81387
            try {
                return ((Boolean) AccessController.doPrivileged(new PrivilegedExceptionAction() {
                    public Object run() throws IOException {
                        return _handleCaseSensitivityCheck(tmpPath, tmpCheckWEBINF); //PK81387 added tmpCheckWEBINF param
                    }
                })).booleanValue();
            } catch (PrivilegedActionException pae) {
                throw (IOException) pae.getException();
            }
        } else {
            return (_handleCaseSensitivityCheck(path, checkWEBINF)).booleanValue(); //PK81387 added checkWEBINF param
        }
    }

    private Boolean _handleCaseSensitivityCheck(String path, boolean checkWEBINF) throws IOException {
        //} //PM10362
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.entering(CLASS_NAME, "_handleCaseSensitivityCheck ", " path --> " + path);
        }

        boolean returnValue = true;

        Object token = ThreadIdentityManager.runAsServer();
        try {
            if (!isValidFilePath(path)) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                        logger.exiting(CLASS_NAME,"_handleCaseSensitivityCheck","Forbidden-invalid file path");
                return false;
            }
            Container adaptableContainer = webapp.getModuleContainer();
            if (adaptableContainer != null) {
                if (adaptableContainer.getEntry(path) != null) {
                    String upperMatchString = WSUtil.resolveURI(path).toUpperCase();
                    //

                    if (checkWEBINF && (upperMatchString.startsWith("/WEB-INF/") || upperMatchString.equals("/WEB-INF")))
                        return false;

                    //We check META-INF by default in the JSP codepath
                    else if (upperMatchString.startsWith("/META-INF/") || upperMatchString.equals("/META-INF"))
                        return false;

                    //Otherwise the Container already checks case sensitivity for us, so no need
                    return true;
                }
            }

            String fileSystemPath = context.getRealPath(path);

            if (adaptableContainer != null && fileSystemPath != null) {
                return true;
            } else if (fileSystemPath == null) {
                return false;
            } else {
                File caseFile = new File(fileSystemPath);
                returnValue = caseFile.exists();
                if (returnValue == false) {
                    return false;
                }
            }

            if (fileSystemPath.endsWith("jar") || fileSystemPath.endsWith("zip")) { // extended document root archive check
                returnValue = true; // archived resources are case sensitive already.
            } else {
                //PM10362  Start
                if (WCCustomProperties.TOLERATE_LOCALE_MISMATCH_FOR_SERVING_FILES && !isValidFilePath(fileSystemPath)) {
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                        logger.exiting(CLASS_NAME, "_handleCaseSensitivityCheck", " ---> Forbidden-invalid file path");
                    }
                    return Boolean.FALSE;
                }
                //PM10362 End
                File caseFile = new File(fileSystemPath);
                returnValue = caseFile.exists();

                //PK81387 start
                //  change FileSystem class to WebContainer one instead of utils. 
                if (returnValue) {
                    returnValue = com.ibm.wsspi.webcontainer.util.FileSystem.uriCaseCheck(caseFile, path.toString(), checkWEBINF);
                }
                //PK81387 end
            }
        } finally {
            ThreadIdentityManager.reset(token);
        }
        // End 247773

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.exiting(CLASS_NAME, "_handleCaseSensitivityCheck"); //PM10362

        return new Boolean(returnValue);
    }

    public IServletConfig getConfig(HttpServletRequest req) throws ServletException {
        final boolean isAnyTraceEnabled = com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled();
        if (isAnyTraceEnabled && logger.isLoggable(Level.FINER)) {
            logger.entering(CLASS_NAME, "getConfig", " req URI --> " + req.getRequestURI());
        }
        String includeUri = (String) req.getAttribute(Constants.INC_SERVLET_PATH);

        String jspUri = null;

        if (includeUri == null) {
            jspUri = req.getServletPath();
            if (req.getPathInfo() != null) {
                // begin 243717: strip off any path elements after ;
                // jspUri += req.getPathInfo();
                String pathInfo = req.getPathInfo();
                int semicolon = pathInfo.indexOf(';');
                if (semicolon != -1) {
                    pathInfo = pathInfo.substring(0, semicolon);
                }
                jspUri += pathInfo;
                // end 243717: strip off any path elements after ;
            }
        } else {
            String pathInfo = (String) req.getAttribute(Constants.INC_PATH_INFO);
            jspUri = includeUri;
            if (pathInfo != null) {
                // begin 243717: strip off any path elements after ;
                int semicolon = pathInfo.indexOf(';');
                if (semicolon != -1) {
                    pathInfo = pathInfo.substring(0, semicolon);
                }
                // end 243717: strip off any path elements after ;
                jspUri += pathInfo;
            }
        }
        jspUri = com.ibm.ws.util.WSUtil.resolveURI(jspUri);
        
        WebAppConfig webAppConfig = webapp.getWebAppConfig();
        IServletConfig sconfig = webAppConfig.getServletInfo(jspUri);
        
        if (sconfig != null) //PI87565: If ServletConfig exists, return it. Otherwise, create a new one.
            return sconfig;
        
        sconfig = createConfig(jspUri);
        sconfig.setServletName(jspUri);
        sconfig.setDisplayName(jspUri);
        sconfig.setServletContext(extensionContext);
        sconfig.setIsJsp(false);
        sconfig.setInitParams(new HashMap());
        sconfig.setFileName(jspUri);

        webapp.getWebAppConfig().addServletInfo(jspUri, sconfig);

        if (isAnyTraceEnabled && logger.isLoggable(Level.FINE)) {
            logger.exiting(CLASS_NAME, "getConfig", " reqURI --> " + req.getRequestURI() + ", jspUri -->" + jspUri);
        } //d651265
        return sconfig;
    }

    protected JspTranslationContext loadTranslationContext(String translationContextClass, ServletContext servletContext, String outputDir, String contextRoot)
                    throws JspCoreException {
        JspTranslationContext ctxt = null;

        if (System.getSecurityManager() != null) {
            try {
                final String finalTranslationContextClass = translationContextClass;
                final ServletContext finalServletContext = servletContext;
                final String finalOutputDir = outputDir;
                final String finalContextRoot = contextRoot;
                ctxt = (JspTranslationContext) AccessController.doPrivileged(new java.security.PrivilegedExceptionAction() {
                    public Object run() throws JspCoreException {
                        try {
                            String docRoot = finalServletContext.getRealPath("/");
                            URL contextURL = new File(docRoot).toURL();
                            JspInputSourceFactory jspInputSourceFactory = new JspInputSourceFactoryImpl(docRoot, contextURL, null, false, webapp.getModuleContainer(), jspClassloaderContext
                                            .getClassLoader(), finalServletContext);
                            Class contextClass = Class.forName(finalTranslationContextClass, true, extensionContext.getClassLoader());
                            Constructor constructor = contextClass.getConstructor(new Class[] { ServletContext.class });
                            JspTranslationContext ctxt = (JspTranslationContext) constructor.newInstance(new Object[] { finalServletContext });
                            JspResourcesFactoryImpl tempFactory = new JspResourcesFactoryImpl(jspOptions, ctxt, webapp.getModuleContainer());
                            JspTranslationEnvironmentImpl jspEnvironment = new JspTranslationEnvironmentImpl(finalOutputDir, finalContextRoot,
                                            jspInputSourceFactory, tempFactory, jspClassloaderContext, jspCompilerFactory);
                            ctxt.setJspTranslationEnviroment(jspEnvironment);
                            return ctxt;
                        } catch (Exception e) {
                            throw new JspCoreException("jsp.error.failed.load.context.class", new Object[] { finalTranslationContextClass }, e);
                        }
                    }
                });
            } catch (PrivilegedActionException pae) {
                throw (JspCoreException) pae.getException();
            }
        } else {
            try {
                String docRoot = servletContext.getRealPath("/");
                URL contextURL = new File(docRoot).toURL();
                JspInputSourceFactory jspInputSourceFactory = new JspInputSourceFactoryImpl(docRoot, contextURL, null, false, webapp.getModuleContainer(), jspClassloaderContext.getClassLoader(), servletContext);
                Class contextClass = Class.forName(translationContextClass, true, extensionContext.getClassLoader());
                Constructor constructor = contextClass.getConstructor(new Class[] { ServletContext.class });
                ctxt = (JspTranslationContext) constructor.newInstance(new Object[] { servletContext });
                JspResourcesFactoryImpl tempFactory = new JspResourcesFactoryImpl(jspOptions, ctxt, webapp.getModuleContainer());
                JspTranslationEnvironmentImpl jspEnvironment = new JspTranslationEnvironmentImpl(outputDir, contextRoot, jspInputSourceFactory,
                                                                                                 tempFactory, jspClassloaderContext, jspCompilerFactory);
                ctxt.setJspTranslationEnviroment(jspEnvironment);
            } catch (Exception e) {
                throw new JspCoreException("jsp.error.failed.load.context.class", new Object[] { translationContextClass }, e);
            }
        }
        return ctxt;
    }

    // begin 254491 [proxies BOTP] mis-handling of non-existent welcome-file's
    // WAS.webcontainer
    public boolean isAvailable(String resource) {
        final boolean isAnyTraceEnabled = com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled();
        if (isAnyTraceEnabled && logger.isLoggable(Level.FINER)) {
            logger.entering(CLASS_NAME, "isAvailable", "resource [" + resource + "] ");
        } //d651265
        boolean available = false;
        // not sure if needed; after first check for isAvailable, welcome file
        // goes directly to wrapper; can this ever be true?
        RequestProcessor rp = extensionContext.getMappingTarget(resource);
        available = (rp instanceof IServletWrapper); // true if wrapper has
                                                     // already been created.

        if (available == false) {
            if (jspOptions.isDisableJspRuntimeCompilation() == false) {
                try {
                    available = handleCaseSensitivityCheck(resource, false); //PK81387 - passing false
                } catch (IOException io) {
                    available = false;
                }
            } else {
                JspInputSource inputSource = context.getJspInputSourceFactory().createJspInputSource(resource);
                JspResources jspResources = context.getJspResourcesFactory().createJspResources(inputSource);

                String classname = jspResources.getClassName();
                String generatedSourceDir = jspResources.getGeneratedSourceFile().getParent();
                if (generatedSourceDir.endsWith(File.separator) == false) {
                    generatedSourceDir = generatedSourceDir + File.separator;
                }
                File tempDirJSP = new File(generatedSourceDir + classname + ".class");
                available = tempDirJSP.exists();

                if (available == false) {

                    String jspUri = inputSource.getRelativeURL();
                    if (jspUri.charAt(0) != '/') {
                        jspUri = "/" + jspUri;
                    }
                    String webinfClassFilePath = null;
                    File webinfClassFile = null;
                    String packageName = null;
                    if (jspOptions.isUseFullPackageNames() == false) {
                        webinfClassFilePath = context.getRealPath("/WEB-INF/classes") + jspUri.substring(0, jspUri.lastIndexOf("/") + 1);
                        webinfClassFile = new File(webinfClassFilePath + File.separator + classname + ".class");
                    } else {
                        packageName = jspResources.getPackageName();
                        String packageDir = packageName.replace('.', '/');
                        webinfClassFilePath = context.getRealPath("/WEB-INF/classes") + "/" + packageDir;
                        webinfClassFile = new File(webinfClassFilePath + File.separator + classname + ".class");
                    }
                    available = webinfClassFile.exists();
                }
            }
        }
        if (isAnyTraceEnabled && logger.isLoggable(Level.FINE)) {
            logger.exiting(CLASS_NAME, "isAvailable", "resource [" + resource + "] is available? [" + available + "]");
        } //d651265
        return available;

    }

    // end 254491 [proxies BOTP] mis-handling of non-existent welcome-file's
    // WAS.webcontainer

    //PK81387 - added checkWEBINF param
    protected abstract boolean processZOSCaseCheck(String path, boolean checkWEBINF) throws IOException;

    public IServletWrapper getServletWrapper() {
        // TODO Auto-generated method stub
        return null;
    }

    // End PK27620

    //PM10362 Start
    protected boolean isValidFilePath(String filePath) {

        if (filePath == null)
            return false;
        int len = filePath.length();
        for (int i = 0; i < len; i++) {
            if (filePath.charAt(i) < ' ') {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                    logger.logp(Level.FINE, CLASS_NAME, "isValidFilePath", " invalid char in filePath --> [" + filePath + "]");

                return false;
            }
        }

        return true;

    }//PM10362 End

    @SuppressWarnings({ "unchecked", "rawtypes" })
    void startPreTouch(final PrepareJspHelperFactory prepareJspHelperFactory) {
        // @BLB Begin Pretouch
        if (this.jspOptions.isPrepareJSPsSet()) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "JSPExtensionProcessor", "PrepareJSPs attribute is: " + this.jspOptions.getPrepareJSPs());
            }

            try {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "JSPExtensionProcessor", "Starting the Pretouch Thread ");
                }
                PreTouchThreadStarter preTouchThread = 
                                new PreTouchThreadStarter(prepareJspHelperFactory.createPrepareJspHelper(this, webapp, jspOptions));
                AccessController.doPrivileged(preTouchThread);
                
            } catch (Exception ex) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "JSPExtensionProcessor", "Pretouch threw an unexpected exception: ", ex);
                }
            }
        }
        // @BLB End Pretouch
    }

    private static class PreTouchThreadStarter implements PrivilegedExceptionAction<Object> {

        private final PrepareJspHelper pretouchHelper;
        
        PreTouchThreadStarter(PrepareJspHelper pretouchHelper) {
            this.pretouchHelper = pretouchHelper;
        }
        /* (non-Javadoc)
         * @see java.security.PrivilegedExceptionAction#run()
         */
        @Override
        public Object run() throws Exception {
            Thread preloadThread = new Thread(pretouchHelper, "JSP Preparation Helper Thread");
            preloadThread.setDaemon(true);
            preloadThread.start();
            return null;
        }
        
    }
}
