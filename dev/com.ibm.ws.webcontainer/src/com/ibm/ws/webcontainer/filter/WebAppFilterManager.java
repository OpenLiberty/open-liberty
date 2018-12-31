/*******************************************************************************
 * Copyright (c) 1997, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.filter;

import java.beans.Beans;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.servlet.error.ServletErrorReport;
import com.ibm.websphere.servlet.event.FilterListenerImpl;
import com.ibm.websphere.servlet.request.extended.IRequestExtended;
import com.ibm.ws.managedobject.ManagedObject;
import com.ibm.ws.webcontainer.WebContainer;
import com.ibm.ws.webcontainer.collaborator.CollaboratorMetaDataImpl;
import com.ibm.ws.webcontainer.extension.DefaultExtensionProcessor;
import com.ibm.ws.webcontainer.osgi.interceptor.RegisterRequestInterceptor;
import com.ibm.ws.webcontainer.servlet.FileServletWrapper;
import com.ibm.ws.webcontainer.servlet.H2Handler;
import com.ibm.ws.webcontainer.servlet.ServletWrapper;
import com.ibm.ws.webcontainer.servlet.WsocHandler;
import com.ibm.ws.webcontainer.srt.SRTServletRequest;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.ws.webcontainer.webapp.WebApp.ANNOT_TYPE;
import com.ibm.ws.webcontainer.webapp.WebAppConfiguration;
import com.ibm.ws.webcontainer.webapp.WebAppDispatcherContext;
import com.ibm.ws.webcontainer.webapp.WebAppErrorReport;
import com.ibm.ws.webcontainer.webapp.WebAppEventSource;
import com.ibm.ws.webcontainer.webapp.WebAppRequestDispatcher;
import com.ibm.wsspi.http.HttpInboundConnection;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.webcontainer.RequestProcessor;
import com.ibm.wsspi.webcontainer.WCCustomProperties;
import com.ibm.wsspi.webcontainer.WebContainerConstants;
import com.ibm.wsspi.webcontainer.WebContainerRequestState;
import com.ibm.wsspi.webcontainer.collaborator.CollaboratorInvocationEnum;
import com.ibm.wsspi.webcontainer.collaborator.ICollaboratorHelper;
import com.ibm.wsspi.webcontainer.collaborator.ICollaboratorMetaData;
import com.ibm.wsspi.webcontainer.collaborator.IWebAppNameSpaceCollaborator;
import com.ibm.wsspi.webcontainer.collaborator.IWebAppSecurityCollaborator;
import com.ibm.wsspi.webcontainer.extension.ExtensionProcessor;
import com.ibm.wsspi.webcontainer.filter.IFilterConfig;
import com.ibm.wsspi.webcontainer.filter.IFilterMapping;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.logging.LoggerHelper;
import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;
import com.ibm.wsspi.webcontainer.security.SecurityViolationException;
import com.ibm.wsspi.webcontainer.servlet.GenericServletWrapper;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;
import com.ibm.wsspi.webcontainer.servlet.IServletWrapper;
import com.ibm.wsspi.webcontainer.util.ServletUtil;
import com.ibm.wsspi.webcontainer.util.ThreadContextHelper;


// PM92496

/**
 * Provides filter management for a WebApp.
 * 
 */
@SuppressWarnings("unchecked")
public class WebAppFilterManager implements com.ibm.wsspi.webcontainer.filter.WebAppFilterManager {
    protected Hashtable _filterWrappers = new Hashtable();

    private Map chainCache = (Map) Collections.synchronizedMap(new LinkedHashMap(20, .75f, true) {
        public boolean removeEldestEntry(Map.Entry eldest) {
            return size() > 200;
        }
    });
    private Map forwardChainCache = (Map) Collections.synchronizedMap(new LinkedHashMap(10, .75f, true) {
        public boolean removeEldestEntry(Map.Entry eldest) {
            return size() > 100;
        }
    });
    private Map includeChainCache = (Map) Collections.synchronizedMap(new LinkedHashMap(5, .75f, true) {
        public boolean removeEldestEntry(Map.Entry eldest) {
            return size() > 100;
        }
    });
    private Map errorChainCache = (Map) Collections.synchronizedMap(new LinkedHashMap(2, .75f, true) {
        public boolean removeEldestEntry(Map.Entry eldest) {
            return size() > 100;
        }
    });

    public boolean _filtersDefined = false;

    static final int FMI_MAPPING_SINGLE_SLASH = 0;
    static final int FMI_MAPPING_PATH_MATCH = 1;
    static final int FMI_MAPPING_EXTENSION_MATCH = 2;
    static final int FMI_MAPPING_EXACT_MATCH = 3;

    // public static final int FILTER_REQUEST = 0;
    // public static final int FILTER_FORWARD = 1;
    // public static final int FILTER_INCLUDE = 2;
    // public static final int FILTER_ERROR = 3;

    protected static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.filter");
    private static final String CLASS_NAME = "com.ibm.ws.webcontainer.filter.WebAppFilterManager";
    private static TraceNLS nls = TraceNLS.getTraceNLS(WebAppFilterManager.class, "com.ibm.ws.webcontainer.resources.Messages");

    WebAppConfiguration webAppConfig;
    // com.ibm.etools.webapplication.WebApp _webModuleDD;
    com.ibm.ws.webcontainer.webapp.WebApp webApp;

    private ICollaboratorHelper collabHelper;
    private IWebAppNameSpaceCollaborator webAppNameSpaceCollab;
    private IWebAppSecurityCollaborator secCollab;
    private boolean sessionSecurityIntegrationEnabled;
    // PK77465 private boolean disableSecurityPreInvokeOnFilters =
    // Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.disablesecuritypreinvokeonfilters")).booleanValue();
    // //PK42868
    private boolean assumeFilterSuccessOnSecurityError = WCCustomProperties.ASSUME_FILTER_SUCCESS_ON_SECURITY_ERROR;
    private static boolean useOriginalRequestState = WCCustomProperties.USE_ORIGINAL_REQUEST_STATE; //PM88028 

    // LIDB-3598: begin
    private WebAppEventSource _evtSource = null;
    // LIDB-3598: end

    private WebComponentMetaData defaultComponentMetaData;
    
    public static final boolean DEFER_SERVLET_REQUEST_LISTENER_DESTROY_ON_ERROR = WCCustomProperties.DEFER_SERVLET_REQUEST_LISTENER_DESTROY_ON_ERROR;  //PI26908

    public WebAppFilterManager(WebAppConfiguration webGroupConfig, WebApp webApp) {
        this.webAppConfig = webGroupConfig;
        this.webApp = webApp;

        // LIDB-3598: begin
        _evtSource = (WebAppEventSource) webApp.getServletContextEventSource();

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { // 306998.15

            logger.logp(Level.FINE, CLASS_NAME, "WebAppFilterManager", "Adding debug filter invocation listener");
            // 292460: begin resolve issues concerning LIDB-3598
            // WASCC.web.webcontainer: redid this portion.
            FilterListenerImpl testListener = new FilterListenerImpl();
            _evtSource.addFilterInvocationListener(testListener);
            _evtSource.addFilterListener(testListener);
            _evtSource.addFilterErrorListener(testListener);
        }
        // 292460: end resolve issues concerning LIDB-3598
        // WASCC.web.webcontainer: redid this portion.
        // LIDB-3598: end

        this.collabHelper = webApp.getCollaboratorHelper();
        this.webAppNameSpaceCollab = this.collabHelper.getWebAppNameSpaceCollaborator();
        this.secCollab = this.collabHelper.getSecurityCollaborator();

        this.defaultComponentMetaData = webApp.getWebAppCmd();

        if (webApp.getSessionContext() != null)
            this.sessionSecurityIntegrationEnabled = webApp.getSessionContext().getIntegrateWASSecurity();
    }

    /** public interface **/

    /**
     * Initialize the manager and see if any filters should be preloaded
     * 
     */
    public void init() {
        // load the filter mappings (copy of the list in case add is called while we iterate)
        List fMappings = new ArrayList(webAppConfig.getFilterMappings());

        // if the list isn't empty, show that this web app has filters
        if (!fMappings.isEmpty()) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                logger.logp(Level.FINE, CLASS_NAME, "init", "filter mappings at init time ->" + fMappings);

            _filtersDefined = true;

            // add the filter mappings to the uri or servlet mapping lists
            Iterator iter = fMappings.iterator();

            while (iter.hasNext()) {
                FilterMapping fMapping = (FilterMapping) iter.next();

                addFilterMapping(fMapping);

                //PK86553 - start
                if (WCCustomProperties.INVOKE_FILTER_INIT_AT_START_UP){
                    String filterName = fMapping.getFilterConfig().getFilterName();
                    if (_filterWrappers.get(filterName) == null){
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                            logger.logp(Level.FINE, CLASS_NAME,"init", "load filter init at startup, filter ->"+ filterName);
                        try{
                            loadFilter(filterName);
                        }
                        catch (Throwable th) {
                            //logger.logp(Level.FINE, CLASS_NAME, "init", "Can not initialize filter ["+filterName+"] at startup.");//596191
                            LoggerHelper.logParamsAndException(logger, Level.SEVERE, CLASS_NAME,"init", "init.exception.thrown.by.filter.at.startup", new Object[]{filterName} , th );

                        }
                    }
                }
                //PK86553 - end
            }
        } else if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "init", "no filter mappings at init time");
        }

    }

    private void addFilterMapping(FilterMapping fMapping) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "addFilterMapping", "filter mapping->" + fMapping);
        }
        if (fMapping != null) {
            _filtersDefined = true;

            String urlMap = fMapping.getUrlPattern();
            if (urlMap != null) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "addFilterMapping", "add url pattern filter mapping");
                }

                webAppConfig.addUriMappedFilterInfo(fMapping);
            } else {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "addFilterMapping", "add servlet name filter mapping");
                }

                // add the filter mapping info to the servlet filter mappings
                webAppConfig.addServletMappedFilterInfo(fMapping);
            }

        }
    }

    /**
     * Returns a FilterInstanceWrapper object corresponding to the passed in
     * filter name. If the filter inst has previously been created, return that
     * instance...if not, then create a new filter instance
     * 
     * @param filterName
     *            - String containing the name of the filter inst to find/create
     * 
     * @return a FilterInstance object corresponding to the passed in filter
     *         name
     * 
     * @throws ServletException
     * 
     */
    public FilterInstanceWrapper getFilterInstanceWrapper(String filterName) throws ServletException {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.entering(CLASS_NAME,"getFilterInstanceWrapper", "entry for " + filterName);

        try {
            FilterInstanceWrapper filterInstW;

            // see if the filter is already loaded

            filterInstW = (FilterInstanceWrapper) (_filterWrappers.get(filterName));
            if (filterInstW == null) { //PM01682 Start
                synchronized(webAppConfig.getFilterInfo(filterName)){
                    // may be more are waiting for lock, check and see if the filter is already loaded
                    filterInstW = (FilterInstanceWrapper) (_filterWrappers.get(filterName));
                    if (filterInstW == null) {
                        // filter not loaded yet...create an instance wrapper
                        filterInstW = loadFilter(filterName);
                    }
                }//PM01682 End
            }
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
                logger.exiting(CLASS_NAME,"getFilterInstanceWrapper", "exit for " + filterName);
            }
            return filterInstW;
        } catch (ServletException e) {
            // logServletError(filterName,
            // nls.getString("Failed.to.load.filter","Failed to load filter"),
            // e);
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e,
                                                                         "com.ibm.ws.webcontainer.filter.WebAppFilterManager.getFilterInstanceWrapper", "166", this);
            throw e;
        } catch (Throwable th) {
            // logServletError(filterName,
            // nls.getString("Failed.to.load.filter","Failed to load filter"),
            // th);
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th,
                                                                         "com.ibm.ws.webcontainer.filter.WebAppFilterManager.getFilterInstanceWrapper", "172", this);
            throw new ServletException(MessageFormat.format("Filter [{0}]: could not be loaded", new Object[] { filterName }), th);
        }

    }

    public WebAppFilterChain getFilterChain(String reqURI, RequestProcessor requestProcessor, DispatcherType dispatcherType) throws ServletException {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { // 306998.15
            logger.logp(Level.FINE, CLASS_NAME, "getFilterChain", "requestURI [" + reqURI + "] no request passed");
        }
        return this.getFilterChain(null, reqURI, requestProcessor, DispatcherType.REQUEST); //PM88028 added this
    }

    /**
     * Returns a WebAppFilterChain object corresponding to the passed in uri and
     * servlet. If the filter chain has previously been created, return that
     * instance...if not, then create a new filter chain instance, ensuring that
     * all filters in the chain are loaded
     * 
     * @param uri
     *            - String containing the uri for which a filter chain is
     *            desired
     * 
     * @return a WebAppFilterChain object corresponding to the passed in uri
     * 
     * @throws ServletException
     * 
     */
    public WebAppFilterChain getFilterChain(ServletRequest request, String reqURI, RequestProcessor requestProcessor, DispatcherType dispatcherType) throws ServletException {
        // strip uri?
        String processorName = "null";
        boolean isInternal = false;
        if (requestProcessor != null) {
            if (requestProcessor instanceof IServletWrapper) {
                String tempProcessorName = ((IServletWrapper) requestProcessor).getServletName();
                if (tempProcessorName != null) {
                    processorName = tempProcessorName;
                }
            }
            isInternal = requestProcessor.isInternal();
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { // 306998.15
            logger.logp(Level.FINE, CLASS_NAME, "getFilterChain", "requestURI [" + reqURI + "] isInternal [" + isInternal + "]");
        }

        FilterChainContents fcc = getFilterChainContents(reqURI, processorName, dispatcherType, isInternal);

        // instantiate and populate a filter chain for this uri
        // PK02277: New constructor to set the WebApp
        WebAppFilterChain newChain = new WebAppFilterChain(webApp);

        // if the chain has filters, add them to the new filter chain
        int nbrOfNames = 0; // PM50111
        if (fcc._hasFilters) {
            // add filter wrappers to this chain
            ArrayList filterNames = fcc.getFilterNames();

            nbrOfNames = filterNames.size();
            for (int i = 0; i < nbrOfNames; i++)
                newChain.addFilter(getFilterInstanceWrapper((String) filterNames.get(i)));
        }
        //PM50111 Start
        if(nbrOfNames > 0 )  {          
            WebContainerRequestState reqStateSaveFilteredRequest = WebContainerRequestState.getInstance(true);   
            reqStateSaveFilteredRequest.setInvokedFilters(true);
            //PM88028 // save of the request in reqState                            
            if(useOriginalRequestState){                    
                reqStateSaveFilteredRequest.setAttribute("unFilteredRequestObject", request);   
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { 
                    logger.logp(Level.FINE, CLASS_NAME, "getFilterChain", "request ["+ request+ "] ,requestURI [" + reqURI + "] has filters [" + nbrOfNames + "]");
                }
            }
        }
        else
            WebContainerRequestState.getInstance(true).setInvokedFilters(false);
        //PM50111 End


        // set the target servlet
        newChain.setRequestProcessor(requestProcessor);

        return newChain;
    }

    /**
     * Returns a WebAppFilterChain object corresponding to the passed in uri and
     * servlet. If the filter chain has previously been created, return that
     * instance...if not, then create a new filter chain instance, ensuring that
     * all filters in the chain are loaded
     * 
     * @param uri
     *            - String containing the uri for which a filter chain is
     *            desired
     * 
     * @return a WebAppFilterChain object corresponding to the passed in uri
     * 
     * @throws ServletException
     * 
     */
    public WebAppFilterChain getFilterChain(String reqURI, ServletWrapper reqServlet) throws ServletException {

        return this.getFilterChain(reqURI, reqServlet, DispatcherType.REQUEST);
    }

    /**
     * Shuts down the filter manager instance and sets the filter config to null
     * for each loaded filter object
     * 
     */
    public void shutdown() {
        // call destroy on each filter instance wrapper
        Enumeration filterWrappers = _filterWrappers.elements();

        ClassLoader origClassLoader = ThreadContextHelper.getContextClassLoader();
        try {
            final ClassLoader warClassLoader = webApp.getClassLoader();
            if (warClassLoader != origClassLoader) {
                ThreadContextHelper.setClassLoader(warClassLoader);
            }
            while (filterWrappers.hasMoreElements()) {
                try {
                    FilterInstanceWrapper fw = (FilterInstanceWrapper) filterWrappers.nextElement();

                    Throwable t = this.webApp.invokeAnnotTypeOnObjectAndHierarchy(fw.getFilterInstance(), ANNOT_TYPE.PRE_DESTROY);
                    if (t != null) {
                        // log exception - could be from user's code - and move on 
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { // 306998.15
                            logger.logp(Level.FINE, CLASS_NAME, "shutdown", "Exception caught during preDestroy processing: " + t);
                        }
                    }

                    fw.destroy();


                } catch (Throwable th) {
                    // should probably log this...continue on to destroy all
                    // filters
                    com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th, "com.ibm.ws.webcontainer.filter.WebAppFilterManager.shutdown",
                                                                                 "237", this);
                }
            }
        } finally {
            ThreadContextHelper.setClassLoader(origClassLoader);
        }

        // release the hash tables
        /* Nulls can cause NPE in doFilter, GC should clean these up when
         * WebAppFilterMater is no longer referenced.
        _filterWrappers = null;
        chainCache = null;
        forwardChainCache = null;
        includeChainCache = null;
        errorChainCache = null;*/
    }

    /** private methods **/

    /**
     * Creates a new FilterInstanceWrapper object corresponding to the passed in
     * filter name. This new object is added to the _filterWrappers hash table
     * 
     * @param filterName
     *            - String containing the name of the filter inst to create
     * 
     * @return a FilterInstanceWrapper object corresponding to the passed in
     *         filter name
     * 
     * @throws ServletException
     * 
     */
    private FilterInstanceWrapper loadFilter(String filterName) throws ServletException {
        ClassLoader origClassLoader = ThreadContextHelper.getContextClassLoader();
        try {
            final ClassLoader warClassLoader = webApp.getClassLoader();
            if (warClassLoader != origClassLoader) {
                ThreadContextHelper.setClassLoader(warClassLoader);
            }
            // initialize the filter instance wrapper
            return _loadFilter(filterName);
        } finally {
            ThreadContextHelper.setClassLoader(origClassLoader);
        }
    }

    private FilterInstanceWrapper _loadFilter(String filterName) throws ServletException {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.entering(CLASS_NAME, "_loadFilter", "filter--->" + filterName);

        FilterInstanceWrapper fiw = null;
        Filter filter = null;

        try {
            // load an instance of the filter class
            // need to get an instance of the wccm filter object here
            IFilterConfig filterConfig = webAppConfig.getFilterInfo(filterName);

            // We want to set to the facade if not set because the config came
            // from WCCM
            // or it was previously set to the internal webApp for dynamically
            // added filters
            filterConfig.setIServletContext((IServletContext) (this.webApp.getFacade()));

            // place the parm names and values in a hashtable
            // HashMap filterInitParams = filterConfig.getInitParams();

            // get the filter class name
            String filterClass = filterConfig.getFilterClassName();

            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) // 306998.15
                logger.logp(Level.FINE, CLASS_NAME, "_loadFilter", "Instantiating Filter Class: {0}", filterClass);

            ManagedObject mo =  null;
            try {
                //boolean filterCreated=false;
                if (filterConfig.getFilter()!=null){
                    filter = filterConfig.getFilter();
                    mo = this.webApp.getCdiContexts().remove(filter);
                } else if (filterConfig.getFilterClass()!=null){
                    //filter = filterConfig.getFilterClass().newInstance();
                    mo = ((com.ibm.ws.webcontainer.osgi.webapp.WebApp)webApp).injectAndPostConstruct(filterConfig.getFilterClass(), filterConfig.getFilterClassLoader());  //PMDINH
                    filter = (Filter)mo.getObject();
                    //filterCreated = true;
                } else {
                    // instantiate the filter
                    // begin 296658 allow FilterConfig to override the default
                    // classloader used WASCC.web.webcontainer
                    final ClassLoader filterLoader = filterConfig.getFilterClassLoader();
                    if (filterLoader != null) {
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { // 306998.15
                            logger.logp(Level.FINE, CLASS_NAME, "_loadFilter", "FilterConfig classloader: " + filterLoader);
                        }
                        
                        // If a serialized version of the filter exists create a new object from it by using Beans.instantiate
                        // otherwise use a ManagedObject factory to create it for cdi 1.2 support.
                        final String serializedName = filterClass.replace('.','/').concat(".ser");
                        InputStream is = (InputStream)AccessController.doPrivileged
                            (new PrivilegedAction() {
                                  public Object run() {
                                      return filterLoader.getResourceAsStream(serializedName);
                                  }
                            });
                        if (is!=null) {
                             if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { // 306998.15
                                logger.logp(Level.FINE, CLASS_NAME, "_loadFilter", "serialized filter exists: " +  serializedName);
                            }
                           filter = (javax.servlet.Filter) Beans.instantiate(filterLoader, filterClass);
                           mo = ((com.ibm.ws.webcontainer.osgi.webapp.WebApp)webApp).injectAndPostConstruct(filter);
                        } else {  
                            Class<?> fClass = filterLoader.loadClass(filterClass);
                            mo = ((com.ibm.ws.webcontainer.osgi.webapp.WebApp)webApp).injectAndPostConstruct(fClass);
                            filter = (Filter)mo.getObject();
                        }     
                        //filterCreated=true;
                        filterConfig.setFilterClassLoader(null); // clean up
                        // reference to
                        // filter
                        // classloader;
                        // only needed for
                        // init.
                    } else {
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { // 306998.15
                            logger.logp(Level.FINE, CLASS_NAME, "_loadFilter", "Filter default classloader: " + webApp.getClassLoader());
                        }
                        final ClassLoader loader = webApp.getClassLoader();
                        final String serializedName = filterClass.replace('.','/').concat(".ser");
                        InputStream is = (InputStream)AccessController.doPrivileged
                            (new PrivilegedAction() {
                                  public Object run() {
                                      return loader.getResourceAsStream(serializedName);
                                  }
                            });
                        if (is!=null) {
                           filter = (javax.servlet.Filter) Beans.instantiate(loader, filterClass);
                           mo = ((com.ibm.ws.webcontainer.osgi.webapp.WebApp)webApp).injectAndPostConstruct(filter);
                        } else {  
                            Class<?> fClass = loader.loadClass(filterClass);
                            mo = ((com.ibm.ws.webcontainer.osgi.webapp.WebApp)webApp).injectAndPostConstruct(fClass);
                            filter = (Filter)mo.getObject();
                        }     
                   }
                    // end 296658 allow FilterConfig to override the default
                    // classloader used WASCC.web.webcontainer
                }
                //if (filterCreated) {
                //    mo = ((com.ibm.ws.webcontainer.osgi.webapp.WebApp)webApp).injectAndPostConstruct(filter);
                //}
            } catch (ClassNotFoundException e) {
                com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, "com.ibm.ws.webcontainer.filter.WebAppFilterManager.loadFilter",
                                                                             "298", this);
                throw new ServletException(MessageFormat.format(nls.getString("Could.not.find.required.filter.class",
                                "Filter [{0}]: Could not find required filter class - {1}.class"), new Object[] { filterName, filterClass }), e);
            } catch (ClassCastException e) {
                com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, "com.ibm.ws.webcontainer.filter.WebAppFilterManager.loadFilter",
                                                                             "303", this);
                throw new ServletException(MessageFormat.format(nls.getString("Filter.not.a.filter.class", "Filter [{0}]: not a filter class"),
                                                                new Object[] { filterName }), e);
            } catch (NoClassDefFoundError e) {
                com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, "com.ibm.ws.webcontainer.filter.WebAppFilterManager.loadFilter",
                                                                             "308", this);
                throw new ServletException(MessageFormat.format(nls.getString("Filter.was.found.but.is.missing.another.required.class",
                                "Filter [{0}]: {1} was found, but is missing another required class.\n"), new Object[] { filterName, filterClass }), e);
            } catch (ClassFormatError e) {
                com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, "com.ibm.ws.webcontainer.filter.WebAppFilterManager.loadFilter",
                                                                             "313", this);
                throw new ServletException(MessageFormat.format(nls.getString("Filter.found.but.corrupt",
                                "Filter [{0}]: {1} was found, but is corrupt:\n"), new Object[] { filterName, filterClass }), e);
            }

            // now we need to add the filter to the wrapper hash
            // LIDB-3598: begin made into separate method to allow overriding of
            // FilterInstanceWrapper
            fiw = createFilterInstanceWrapper(filterName, filter, mo);
            // LIDB-3598: end
            // On injection failure if CP is set , filter will not be initialized //596191 :: PK97815
            // initialize the filter instance wrapper
            fiw.init(filterConfig);

            // add the filter instance to the wrapper hash
            _filterWrappers.put(filterName, fiw);
        } catch (ServletException e) {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, "com.ibm.ws.webcontainer.filter.WebAppFilterManager.loadFilter", "380",
                                                                         this);
            throw e;
        } //596191 :: PK97815 Start
        catch (InjectionException ie) {						
            com.ibm.ws.ffdc.FFDCFilter.processException(ie, "com.ibm.ws.webcontainer.filter.WebAppFilterManager.loadFilter", "381", this);
            throw new ServletException(MessageFormat.format(nls.getString("Filter.found.but.injection.failure","The [{0}] filter was found but a resource injection failure has occurred:\n"),
                                                            new Object[] { filterName }), ie);   			
        }//596191 :: PK97815 End
        catch (Throwable th) {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th, "com.ibm.ws.webcontainer.filter.WebAppFilterManager.loadFilter", "385",
                                                                         this);
            throw new ServletException(MessageFormat.format("Filter [{0}]: could not be loaded", new Object[] { filterName }), th);
        }

        finally {
            if (null != filter){ //the managed object will now be tracked with the FilterInstanceWrapper
                this.webApp.getCdiContexts().remove(filter); 
            }
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.exiting(CLASS_NAME, "_loadFilter");

        return fiw;
    }

    // begin LIDB-3598 added support for FilterInvocationListeners
    protected FilterInstanceWrapper createFilterInstanceWrapper (String filterName, Filter filter, ManagedObject mo) throws InjectionException{ //596191 updated signature
        return new FilterInstanceWrapper(filterName, filter, _evtSource, mo);
    }

    // end LIDB-3598 added support for FilterInvocationListeners

    /**
     * Gets the filter chain contents object for the passed in uri...if it
     * doesn't exist, a new filter chain contents is created
     * 
     * @param requestURI
     *            the request uri
     * 
     * @return a filter chain contents object corresponding to the passed in uri
     */
    private FilterChainContents getFilterChainContents(String reqURI, String reqServletName, DispatcherType dispatcherType, boolean servletIsInternal) {
        final boolean isTraceOn = com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && logger.isLoggable(Level.FINE)) {
            logger.entering(CLASS_NAME, "getFilterChainContents", "reqUri->" + reqURI + ", reqServletName->" + reqServletName + ", mode->" + dispatcherType
                            + ", servletIsInternal->" + servletIsInternal);
        }
        FilterChainContents fcc = null;
        String strippedUri = null;

        // search for existing filter chains by URI (if not null) else by name
        if (reqURI != null) {
            int queryIndex = reqURI.indexOf("?");
            if (queryIndex > 0) {
                strippedUri = reqURI.substring(0, queryIndex);
            } else {
                strippedUri = reqURI;
            }

            // begin 262685: strip off any path elements after ;
            int semicolon = strippedUri.indexOf(';');
            if (semicolon != -1) {
                strippedUri = strippedUri.substring(0, semicolon);
            }
            // end 262685: strip off any path elements after ;

            if (isTraceOn && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "getFilterChainContents", "strippedUri->" + strippedUri);
            }

            // 144464 end part 1
            // see if the chain has been previously constructed (look for a
            // filter contents object)
            if (dispatcherType == DispatcherType.REQUEST) {
                fcc = (FilterChainContents) chainCache.get(strippedUri);
                if (isTraceOn && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "getFilterChainContents", "filter request mode, get cache entry fcc->" + fcc);
                }
            } else if (dispatcherType == DispatcherType.FORWARD) {
                fcc = (FilterChainContents) forwardChainCache.get(strippedUri);
                if (isTraceOn && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "getFilterChainContents", "filter forward mode, get cache entry fcc->" + fcc);
                }
            } else if (dispatcherType == DispatcherType.INCLUDE) {
                fcc = (FilterChainContents) includeChainCache.get(strippedUri);
                if (isTraceOn && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "getFilterChainContents", "filter include mode, get cache entry fcc->" + fcc);
                }
            } else if (dispatcherType == DispatcherType.ERROR) {
                fcc = (FilterChainContents) errorChainCache.get(strippedUri);
                if (isTraceOn && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "getFilterChainContents", "filter error mode, get cache entry fcc->" + fcc);
                }
            }

        } else {
            if (dispatcherType == DispatcherType.REQUEST) {
                fcc = (FilterChainContents) chainCache.get(reqServletName);
                if (isTraceOn && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "getFilterChainContents", "filter request mode, get cache entry fcc->" + fcc);
                }
            } else if (dispatcherType == DispatcherType.FORWARD) {
                fcc = (FilterChainContents) forwardChainCache.get(reqServletName);
                if (isTraceOn && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "getFilterChainContents", "filter forward mode, get cache entry fcc->" + fcc);
                }
            } else if (dispatcherType == DispatcherType.INCLUDE) {
                fcc = (FilterChainContents) includeChainCache.get(reqServletName);
                if (isTraceOn && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "getFilterChainContents", "filter include mode, get cache entry fcc->" + fcc);
                }
            } else if (dispatcherType == DispatcherType.ERROR) {
                fcc = (FilterChainContents) errorChainCache.get(reqServletName);
                if (isTraceOn && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "getFilterChainContents", "filter error mode, get cache entry fcc->" + fcc);
                }

            }

        }

        if (fcc == null) {
            // no current chain...build one
            int i;

            // create a filter contents object so we can store it and avoid
            // doing this step again
            fcc = new FilterChainContents();

            // check the uri against the uri filter mapping list
            IFilterMapping fmInfo;
            int nbrOfMappings;

            if (strippedUri != null) {
                List<IFilterMapping> uriFilterMappings = webAppConfig.getUriFilterMappings();
                if (uriFilterMappings != null) {
                    nbrOfMappings = uriFilterMappings.size();

                    if (isTraceOn && logger.isLoggable(Level.FINE)) {
                        logger.logp(Level.FINE, CLASS_NAME, "getFilterChainContents", "number of uri filter mapping->" + nbrOfMappings);
                    }
                    for (i = 0; i < nbrOfMappings; i++) {
                        fmInfo = (IFilterMapping) uriFilterMappings.get(i);
                        if (isTraceOn && logger.isLoggable(Level.FINE)) {
                            logger.logp(Level.FINE, CLASS_NAME, "getFilterChainContents", "filter mapping info->" + fmInfo);
                        }
                        if ((servletIsInternal || servletIsInternal == fmInfo.getFilterConfig().isInternal())
                                        && (uriMatch(strippedUri, fmInfo, dispatcherType))) { // 144464
                            // part
                            // 3
                            fcc.addFilter(fmInfo.getFilterConfig().getFilterName());
                        }
                    }
                }
            }

            // check the requested servlet name against the servlet filter
            // mapping list
            List<IFilterMapping> servletFilterMappings = webAppConfig.getServletFilterMappings();
            if (servletFilterMappings != null) {
                nbrOfMappings = servletFilterMappings.size();

                if (isTraceOn && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "getFilterChainContents", "number of servlet filter mapping->" + nbrOfMappings);
                }
                for (i = 0; i < nbrOfMappings; i++) {
                    fmInfo = (IFilterMapping) servletFilterMappings.get(i);
                    String filterServlet = fmInfo.getServletConfig().getServletName();
                    if (isTraceOn && logger.isLoggable(Level.FINE)) {
                        logger.logp(Level.FINE, CLASS_NAME, "getFilterChainContents", "filter mapping info->" + fmInfo);
                    }
                    if (filterServlet != null) {
                        if (reqServletName.equals(filterServlet)) {
                            if (isTraceOn && logger.isLoggable(Level.FINE)) {
                                logger.logp(Level.FINE, CLASS_NAME, "getFilterChainContents", "matches servlet name");
                            }
                            for (int j = 0; j < fmInfo.getDispatchMode().length; j++) {
                                if (isTraceOn && logger.isLoggable(Level.FINE)) {
                                    logger.logp(Level.FINE, CLASS_NAME, "getFilterChainContents", "current dispatch mode->"
                                                    + fmInfo.getDispatchMode()[j]);
                                }
                                if ((servletIsInternal || servletIsInternal == fmInfo.getFilterConfig().isInternal())
                                                && (dispatcherType == fmInfo.getDispatchMode()[j])) {
                                    fcc.addFilter(fmInfo.getFilterConfig().getFilterName());
                                    break;
                                }
                            }
                        }
                        // begin, ADDED for Servlet 2.5 SRV.6.2.5
                        else if (filterServlet.equals("*")) {
                            if (isTraceOn && logger.isLoggable(Level.FINE)) {
                                logger.logp(Level.FINE, CLASS_NAME, "getFilterChainContents", "matches *");
                            }
                            for (int j = 0; j < fmInfo.getDispatchMode().length; j++) {
                                if (isTraceOn && logger.isLoggable(Level.FINE)) {
                                    logger.logp(Level.FINE, CLASS_NAME, "getFilterChainContents", "current dispatch mode->"
                                                    + fmInfo.getDispatchMode()[j]);
                                }
                                if (fmInfo.getDispatchMode()[j] == DispatcherType.FORWARD
                                                || fmInfo.getDispatchMode()[j] == DispatcherType.INCLUDE) {
                                    if ((servletIsInternal || servletIsInternal == fmInfo.getFilterConfig().isInternal())
                                                    && (dispatcherType == fmInfo.getDispatchMode()[j])) {
                                        fcc.addFilter(fmInfo.getFilterConfig().getFilterName());
                                        break;
                                    }
                                }
                            }
                        }
                    } else if (logger.isLoggable(Level.WARNING)) { // 306998.15
                        logger.logp(Level.WARNING, CLASS_NAME, "getFilterChainContents", "Null.Filter.Mapping");
                    }
                    // end, ADDED for Servlet 2.5 SRV.6.2.5
                }
            }

            // add the new chain contents to the chain list, indexed by the uri
            // or name
            if (strippedUri != null) {
                if (dispatcherType == DispatcherType.REQUEST)
                    chainCache.put(strippedUri, fcc);
                else if (dispatcherType == DispatcherType.FORWARD)
                    forwardChainCache.put(strippedUri, fcc);
                else if (dispatcherType == DispatcherType.INCLUDE)
                    includeChainCache.put(strippedUri, fcc);
                else if (dispatcherType == DispatcherType.ERROR)
                    errorChainCache.put(strippedUri, fcc);
            } else {
                if (dispatcherType == DispatcherType.REQUEST)
                    chainCache.put(reqServletName, fcc);
                else if (dispatcherType == DispatcherType.FORWARD)
                    forwardChainCache.put(reqServletName, fcc);
                else if (dispatcherType == DispatcherType.INCLUDE)
                    includeChainCache.put(reqServletName, fcc);
                else if (dispatcherType == DispatcherType.ERROR)
                    errorChainCache.put(reqServletName, fcc);
            }

            // 144464 part 4
        }
        if (isTraceOn && logger.isLoggable(Level.FINE)) {
            logger.exiting(CLASS_NAME, "getFilterChainContents");
        }

        return fcc;
    }

    /**
     * Compares the request uri to the passed in filter uri to see if the filter
     * associated with the filter uri should filter the request uri
     * 
     * @param requestURI
     *            the request uri
     * @param filterURI
     *            the uri associated with the filter
     * 
     * @return boolean indicating whether the uri's match
     */
    private boolean uriMatch(String requestURI, IFilterMapping fmInfo, DispatcherType dispatcherType) {
        boolean theyMatch = false;

        // determine what type of filter uri we have
        switch (fmInfo.getMappingType()) {
            case FMI_MAPPING_SINGLE_SLASH:
                // default servlet mapping...if requestURI is "/", they match
                if (requestURI.equals("/")) // 144908
                    theyMatch = true;
                break;

            case FMI_MAPPING_PATH_MATCH:
                // it's a path mapping...match string is already stripped
                if (requestURI.startsWith(fmInfo.getUrlPattern() + "/") || requestURI.equals(fmInfo.getUrlPattern()))
                    theyMatch = true;
                break;

            case FMI_MAPPING_EXTENSION_MATCH:
                // it's an extension mapping...get the extension
                String ext = fmInfo.getUrlPattern().substring(2);

                // compare to any extension on the request uri
                int index = requestURI.lastIndexOf('.');

                if (index != -1)
                    if (ext.equals(requestURI.substring(index + 1)))
                        theyMatch = true;
                break;

            case FMI_MAPPING_EXACT_MATCH:
                // it's an exact match
                if (requestURI.equals(fmInfo.getUrlPattern()))
                    theyMatch = true;
                break;
            default:
                // should never happen...give a message?
                break;
        }

        // Check if dispatch mode matches
        boolean dispMatch = false;
        if (theyMatch) {
            for (int i = 0; i < fmInfo.getDispatchMode().length; i++) {
                if (dispatcherType == fmInfo.getDispatchMode()[i]) {
                    dispMatch = true;
                    break;
                }
            }
        }

        return dispMatch && theyMatch;
    }

    public void doFilter(ServletRequest request, ServletResponse response, RequestProcessor requestProcessor,
                         WebAppDispatcherContext dispatchContext) throws ServletException, IOException {
        final boolean isTraceOn = com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && logger.isLoggable(Level.FINE)) { // 306998.15
            logger.entering(CLASS_NAME, "doFilter");
        }

        String nested = (String) request.getAttribute(WebAppRequestDispatcher.DISPATCH_NESTED_ATTR);
        boolean isNested = (nested != null && (nested.equalsIgnoreCase(WebAppRequestDispatcher.NESTED_TRUE)));
        // filter mappings are defined for this web app...create a chain
        WebAppFilterChain fc;
        if (!isNested) {
            // filter mappings are defined for this web app...create a chain
            fc = getFilterChain(request, dispatchContext.getRelativeUri(), requestProcessor, DispatcherType.REQUEST); //PM88028

        } else if (request.getDispatcherType()==DispatcherType.ERROR) {
            fc = getFilterChain(request, dispatchContext.getRelativeUri(), requestProcessor, DispatcherType.ERROR); //PM88028
        } else {
            if (request.getDispatcherType()==DispatcherType.INCLUDE) {
                // Begin 308220, doFilter fails for named dispatches because we
                // don't check wether the path info attribute exists first
                String attr = (String) request.getAttribute(WebAppRequestDispatcher.SERVLET_PATH_INCLUDE_ATTR);
                if (attr == null) {
                    // its a named dispatch since the attrs are missing...
                    fc = getFilterChain(request, null, requestProcessor, DispatcherType.INCLUDE); //PM88028
                } else {
                    StringBuffer relUri = new StringBuffer(attr);
                    String pi = (String) request.getAttribute(WebAppRequestDispatcher.PATH_INFO_INCLUDE_ATTR);
                    if (pi != null)
                        relUri.append(pi);
                    String ru = relUri.toString();
                    fc = getFilterChain(request, ru, requestProcessor, DispatcherType.INCLUDE);//PM88028
                }
                // End 308220, doFilter fails for named dispatches because we
                // don't check wether the path info attribute exists first
            } else if (request.getDispatcherType()==DispatcherType.FORWARD) {
                fc = getFilterChain(request, dispatchContext.getRelativeUri(), requestProcessor, DispatcherType.FORWARD);//PM88028
            } else if (request.getDispatcherType()==DispatcherType.ASYNC) {
                fc = getFilterChain(request, dispatchContext.getRelativeUri(), requestProcessor, DispatcherType.ASYNC);//PM88028
            } else {
                fc = getFilterChain(request, dispatchContext.getRelativeUri(), requestProcessor, DispatcherType.REQUEST);//PM88028
            }
        }

        if (requestProcessor != null) {
            fc.setRequestProcessor(requestProcessor);
        }

        // invoke the first filter
        fc.doFilter(request, response);
        if (isTraceOn && logger.isLoggable(Level.FINE)) { // 306998.15
            logger.exiting(CLASS_NAME, "doFilter");
        }

    }

    public boolean invokeFilters(ServletRequest request, ServletResponse response, IServletContext context,
                                 RequestProcessor requestProcessor, EnumSet<CollaboratorInvocationEnum> colEnum) throws ServletException, IOException {
        return invokeFilters(request, response, context, requestProcessor, colEnum, null);
        
    }

    public boolean invokeFilters(ServletRequest request, ServletResponse response, IServletContext context,
                             RequestProcessor requestProcessor, EnumSet<CollaboratorInvocationEnum> colEnum,
                             HttpInboundConnection httpInboundConnection) throws ServletException, IOException {
        final boolean isTraceOn = com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && logger.isLoggable(Level.FINE)) {
            logger.entering(CLASS_NAME, "invokeFilters", "request->" + request + ", response->" + response + ", requestProcessor->"
                            + requestProcessor + ", context->" + context);
        }
        boolean result = false;
        IExtendedRequest wasreq = null;
        WebAppDispatcherContext dispatchContext = null;
        IServletConfig servletConfig = null;
        WebComponentMetaData componentMetaData = null;

        wasreq = (IExtendedRequest) ServletUtil.unwrapRequest(request);
        dispatchContext = (WebAppDispatcherContext) wasreq.getWebAppDispatcherContext();
        boolean securityEnforced = false;

        boolean isInclude = dispatchContext.isInclude();
        boolean isForward = dispatchContext.isForward();
        boolean isRequest = dispatchContext.getDispatcherType()==DispatcherType.REQUEST;

        HttpServletRequest httpServletReq = (HttpServletRequest) ServletUtil.unwrapRequest(request,HttpServletRequest.class);
        HttpServletResponse httpServletRes = (HttpServletResponse) ServletUtil.unwrapResponse(response,HttpServletResponse.class);

        ICollaboratorMetaData collabMetaData = null;

        //PI08268
        boolean checkDefaultMethodAttributeSet = false;                         
        String attributeTargetClass = null;                                             
        //PI08268

        boolean h2InUse = false;

        try {
            if (requestProcessor != null) {

                if (requestProcessor instanceof ExtensionProcessor) {
                    IServletWrapper servletWrapper = ((ExtensionProcessor) requestProcessor).getServletWrapper(request, response);
                    if (servletWrapper != null) {
                        requestProcessor = servletWrapper;
                    } else if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                        logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "ExtensionProcessor could not return us a ServletWrapper");
                    }
                }


                if (requestProcessor instanceof IServletWrapper) {
                    IServletWrapper servletWrapper = (IServletWrapper) requestProcessor;
                    // unlike tWAS the metadata is already created, so no need to call load on the servletWrapper

                    servletConfig = servletWrapper.getServletConfig();
                    if (servletConfig != null)
                        componentMetaData = servletConfig.getMetaData();
                    else if (isTraceOn && logger.isLoggable(Level.FINE)) {
                        logger.logp(Level.FINE, CLASS_NAME, "invokeFilters", "servletConfig is null");
                    }
                    dispatchContext.pushServletReference(servletWrapper);

                    // let the servlet warrper know that the request is about to start.
                    if (servletWrapper instanceof ServletWrapper) {
                        ((ServletWrapper)servletWrapper).startRequest(request);
                    }
                    //PI08268 - start - disable JSP and Static default methods (i.e TRACE, PUT, DELETE...)
                    else {
                        String httpMethod = httpServletReq.getMethod().toUpperCase();
                        if (!(httpMethod.equals("GET") || httpMethod.equals("POST"))){ //quick check since most request is GET/POST
                            if (servletWrapper instanceof FileServletWrapper){  // subsequent static request takes this path
                                attributeTargetClass = "com.ibm.ws.webcontainer.isstaticclass";
                                checkDefaultMethodAttributeSet = setDefaultMethod(httpServletReq, attributeTargetClass, httpMethod);
                            }
                            else if (servletWrapper instanceof GenericServletWrapper){
                                attributeTargetClass = "com.ibm.ws.webcontainer.jsp.isjspclass";
                                checkDefaultMethodAttributeSet = setDefaultMethod(httpServletReq, attributeTargetClass, httpMethod);
                            }
                        }
                    }
                    //PI08268 - end  
                } else if (requestProcessor instanceof ExtensionProcessor) {
                    componentMetaData = ((ExtensionProcessor) requestProcessor).getMetaData();
                    
                    //PI08268 - only first request to static file is here since it does not have a wrapper
                    String httpMethod = httpServletReq.getMethod().toUpperCase();
                    if (!(httpMethod.equals("GET") || httpMethod.equals("POST"))){
                        if (requestProcessor instanceof DefaultExtensionProcessor){
                            attributeTargetClass = "com.ibm.ws.webcontainer.isstaticclass";
                            checkDefaultMethodAttributeSet = setDefaultMethod(httpServletReq, attributeTargetClass, httpMethod);
                        }
                    }
                    //PI08268
                }
            }

            if (componentMetaData == null) {
                componentMetaData = getDefaultComponentMetaData();
            }

            collabMetaData = new CollaboratorMetaDataImpl(componentMetaData, httpServletReq, httpServletRes, dispatchContext, servletConfig,
                                                          context,requestProcessor);

            collabHelper.preInvokeCollaborators(collabMetaData, colEnum);

            //PM92496  Start            
            if (httpServletReq.isSecure()) {
                ServletRequest implRequest = ServletUtil.unwrapRequest(httpServletReq);
                ((SRTServletRequest) implRequest).setSSLAttributesInRequest(httpServletReq, ((SRTServletRequest) implRequest).getCipherSuite());
            }
            //PM92496  End
            
            //PI08268  reset after the preInvokeCollaborators()
            if (checkDefaultMethodAttributeSet){
                httpServletReq.removeAttribute(attributeTargetClass);
                httpServletReq.removeAttribute("com.ibm.ws.webcontainer.security.checkdefaultmethod");
                checkDefaultMethodAttributeSet = false; 
            }
            //PI08268
            
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) 
                logger.logp(Level.FINE, CLASS_NAME, "invokeFilters", "### looking at isFiltersDefined");

            if (context.isFiltersDefined()) {

                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) 
                    logger.logp(Level.FINE, CLASS_NAME, "invokeFilters", "### calling doFilter");

                doFilter(request, response, requestProcessor, dispatchContext);
            }
            else {

                boolean handled = false;

                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) 
                    logger.logp(Level.FINE, CLASS_NAME, "invokeFilters", "no more filters defined");

                if (requestProcessor != null) {
                    if (!RegisterRequestInterceptor.notifyRequestInterceptors("AfterFilters", httpServletReq, httpServletRes)) {

                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) 
                            logger.logp(Level.FINE, CLASS_NAME, "invokeFilters", "looking at WSOC upgrade handlers");

                        WsocHandler wsocHandler = ((com.ibm.ws.webcontainer.osgi.webapp.WebApp) webApp).getWebSocketHandler();
                        if (wsocHandler != null) {
                            //Should WebSocket handle this request?
                            if (wsocHandler.isWsocRequest(request)) {
                                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) 
                                    logger.logp(Level.FINE, CLASS_NAME, "invokeFilters", "upgrade to WSOC");
                                HttpServletRequest httpRequest = (HttpServletRequest) ServletUtil.unwrapRequest(request, HttpServletRequest.class);
                                HttpServletResponse httpResponse = (HttpServletResponse) ServletUtil.unwrapResponse(response, HttpServletResponse.class);
                                wsocHandler.handleRequest(httpRequest, httpResponse);
                                handled = true;
                            } 
                        }

                        if (!handled) {
                            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) 
                                logger.logp(Level.FINE, CLASS_NAME, "invokeFilters", "looking at H2 upgrade");
                            // Check if this is an HTTP2 upgrade request
                            if (request instanceof HttpServletRequest) {
                                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) 
                                    logger.logp(Level.FINE, CLASS_NAME, "invokeFilters", "looking at H2 handler");
                                H2Handler h2Handler = ((com.ibm.ws.webcontainer.osgi.webapp.WebApp) webApp).getH2Handler();
                                if (h2Handler != null) {
                                    if (request instanceof SRTServletRequest) {
                                        SRTServletRequest srtReq = (SRTServletRequest) request;
                                        IRequestExtended iReq = (IRequestExtended)srtReq.getIRequest();
                                        if (iReq != null) {
                                            httpInboundConnection = iReq.getHttpInboundConnection();
                                            logger.logp(Level.FINE, CLASS_NAME, "invokeTarget", "HttpInboundConnection: " + httpInboundConnection);
                                        }
                                    }

                                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) 
                                        logger.logp(Level.FINE, CLASS_NAME, "invokeFilters", "looking at isH2Request");
                                    if (httpInboundConnection != null && h2Handler.isH2Request(httpInboundConnection, request)) {
                                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) 
                                            logger.logp(Level.FINE, CLASS_NAME, "invokeFilters", "upgrading to H2");
                                        HttpServletRequest httpRequest = (HttpServletRequest) ServletUtil.unwrapRequest(request, HttpServletRequest.class);                                
                                        HttpServletResponse httpResponse = (HttpServletResponse) ServletUtil.unwrapResponse(response, HttpServletResponse.class);
                                        
                                        try {
                                            h2InUse = true;
                                            h2Handler.handleRequest(httpInboundConnection, httpRequest, httpResponse);
                                            
                                            if (httpResponse.getStatus() == HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
                                                if (isTraceOn && logger.isLoggable(Level.FINE)) 
                                                    logger.logp(Level.FINE, CLASS_NAME, "invokeFilters", "H2 determined SC_INTERNAL_SERVER_ERROR, throwing IOException");
                                                
                                                IOException ioe = new IOException("Http2 determined internal exception while handling request");
                                                throw ioe;
                                                
                                            } else {
                                                if (isTraceOn && logger.isLoggable(Level.FINE)) 
                                                    logger.logp(Level.FINE, CLASS_NAME, "invokeFilters", "H2 status OK");
                                            }
                                            
                                        } catch (Exception x) {
                                            // need to change these to IOExceptions, since the cause could be the
                                            // H2 code closing down the connection while it is in app code.
                                            
                                            if (isTraceOn && logger.isLoggable(Level.FINE)) 
                                                logger.logp(Level.FINE, CLASS_NAME, "invokeFilters", "H2 caught exception: " + x);
                                            
                                            IOException ioe = new IOException("Http2 received internal exception while handling request");
                                            throw ioe;
                                        }
                                    }
                                }
                            }
                            if (h2InUse && com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) 
                                logger.logp(Level.FINE, CLASS_NAME, "invokeFilters", "in H2 processing calling requestProcessor.handleRequest");

                            try {
                                requestProcessor.handleRequest(request, response);
                            }  catch (Exception x) {
                                if (h2InUse) {
                                    // need to change these to IOExceptions, since the cause could be the
                                    // H2 code closing down the connection while it is being handled.

                                    if (isTraceOn && logger.isLoggable(Level.FINE)) 
                                        logger.logp(Level.FINE, CLASS_NAME, "invokeFilters", "H2 caught exception:: " + x);

                                    IOException ioe = new IOException("Http2 detected internal exception");
                                    throw ioe;
                                } else {
                                    throw x;
                                }
                            }    
                        }
                    }
                } else {
                    webApp.finishedFiltersWithNullTarget(request, response, requestProcessor);
                }
            }
        } catch (IOException ioe) {
            if (!h2InUse) {
                if (DEFER_SERVLET_REQUEST_LISTENER_DESTROY_ON_ERROR) {
                    WebContainerRequestState reqState = WebContainerRequestState.getInstance(true); //PI26908
                    reqState.setAttribute("invokeFiltersException", "IOE"); //PI26908
                }
                if (isRethrowOriginalException(request, isInclude, isForward)) {
                    dispatchContext.pushException(ioe);
                    throw ioe;
                }
                if ((com.ibm.ws.webcontainer.osgi.WebContainer.getServletContainerSpecLevel() >= 31)
                    && ioe.getMessage() != null
                    && ioe.getMessage().contains("SRVE0918E")) {
                    throw ioe;
                }
                // LIBERTY: TODO send error when not include or forward
                ServletErrorReport errorReport = WebAppErrorReport.constructErrorReport(ioe, requestProcessor);
                dispatchContext.pushException(ioe);
                //61464 don't log a FFDC for a FileNotFound IOException
                //com.ibm.ws.ffdc.FFDCFilter.processException(ioe, "com.ibm.ws.webcontainer.filter.WebAppFilterManager.invokeFilters", "1038");
                throw errorReport;
            }
        } catch (ServletErrorReport ser) {
            if(DEFER_SERVLET_REQUEST_LISTENER_DESTROY_ON_ERROR){
                WebContainerRequestState reqState = WebContainerRequestState.getInstance(true); //PI26908
                reqState.setAttribute("invokeFiltersException", "SER"); //PI26908
            }

            //com.ibm.ws.ffdc.FFDCFilter.processException(ser, "com.ibm.ws.webcontainer.filter.WebAppFilterManager.invokeFilters", "1042", this);
            dispatchContext.pushException(ser);
            if (!dispatchContext.isInclude() && !dispatchContext.isForward() && (request instanceof HttpServletRequest)) {
                if (isTraceOn && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "invokeFilters", "Servlet error report caught, call send error with security context");
                }
                WebApp app = (WebApp) dispatchContext.getWebApp();
                app.sendError((HttpServletRequest) request, (HttpServletResponse) response, ser);
            } else {
                throw ser;
            }

        } catch (ServletException se) { // Added for ARD LIDB3518, ignore
            // dispatch state so we can get error
            // pages
            // when there is no top level servlet to handle the exception
            if(DEFER_SERVLET_REQUEST_LISTENER_DESTROY_ON_ERROR){
                WebContainerRequestState reqState = WebContainerRequestState.getInstance(true); //PI26908
                reqState.setAttribute("invokeFiltersException", "SRVE"); //PI26908
            }

            if (isRethrowOriginalException(request, isInclude, isForward)) {
                dispatchContext.pushException(se);
                throw se;
            }
            ServletErrorReport errorReport = WebAppErrorReport.constructErrorReport(se, requestProcessor);
            dispatchContext.pushException(se);
            // LIBERTY: TODO send error when not include or forward
            Throwable causedBy = se.getCause();
            if (causedBy!=null && causedBy instanceof FileNotFoundException) {
                //don't log a FFDC
                if (isTraceOn && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "invokeFilters", "FileNotFound", se);
                }
            } else {
                //start 140014
                if(!webApp.getDestroyed())
                    com.ibm.ws.ffdc.FFDCFilter.processException(se, "com.ibm.ws.webcontainer.filter.WebAppFilterManager.invokeFilters", "1064");
                else
                    logger.logp(Level.WARNING, CLASS_NAME, "invokeFilters", "filter.failed.app.destroyed", new Object[] {httpServletReq.getRequestURI()});
                //end 140014
            }
            throw errorReport;
        } catch (SecurityViolationException wse) {
            if (collabMetaData != null) {
                // begin PK10057: leveraged code from helper class
                if (servletConfig == null)
                    collabMetaData.setSecurityObject(collabHelper.processSecurityPreInvokeException(wse, requestProcessor, httpServletReq, httpServletRes, dispatchContext, webApp, null));
                else
                    collabMetaData.setSecurityObject(collabHelper.processSecurityPreInvokeException(wse, requestProcessor, httpServletReq, httpServletRes, dispatchContext, webApp, servletConfig
                                                                                                    .getServletName()));
                // end PK10057
            }
        } 
        catch (RuntimeException re)
        {
            if(DEFER_SERVLET_REQUEST_LISTENER_DESTROY_ON_ERROR){
                WebContainerRequestState reqState = WebContainerRequestState.getInstance(true); //PI26908
                reqState.setAttribute("invokeFiltersException", "RE"); //PI26908
            }

            if (isRethrowOriginalException(request, isInclude,isForward))
            {
                dispatchContext.pushException(re);
                throw re;
            }

//            boolean invokedAsyncErrorHandling = false;
//            if (re instanceof AsyncIllegalStateException){
//            	WebContainerRequestState reqState = WebContainerRequestState.getInstance(false);
//    	         if (reqState!=null&&reqState.isAsyncMode())
//    	         {
//    	        	 if (isTraceOn && logger.isLoggable(Level.FINE)) {
//    	                    logger.logp(Level.FINE, CLASS_NAME, "invokeFilters", "invokeAsyncErrorHandling");
//    	             }
//    	        	 invokedAsyncErrorHandling = true;
//    	        	 ListenerHelper.invokeAsyncErrorHandling(reqState.getAsyncContext(), reqState, re, AsyncListenerEnum.ERROR, ExecuteNextRunnable.FALSE);
//    	         } 
////    	         else {
////    	        	 //do nothing because startAsync was never called successfully so we can let standard
////    	        	 //error dispatching occur (e.g. async is not supported)
////    	         }
//            }
//            if (!invokedAsyncErrorHandling){
            ServletErrorReport errorReport = WebAppErrorReport.constructErrorReport(re, dispatchContext.getCurrentServletReference());
            dispatchContext.pushException(re);
            com.ibm.ws.ffdc.FFDCFilter.processException(
                                                        re,
                                                        "com.ibm.ws.webcontainer.filter.WebAppFilterManager.invokeFilters",
                                                        "1105",
                                                        this);
            throw errorReport;
//            }
        }
        catch (Throwable th2) {
            if(DEFER_SERVLET_REQUEST_LISTENER_DESTROY_ON_ERROR){
                WebContainerRequestState reqState = WebContainerRequestState.getInstance(true); //PI26908
                reqState.setAttribute("invokeFiltersException", "THR"); //PI26908
            }

            com.ibm.ws.ffdc.FFDCFilter.processException(th2, "com.ibm.ws.webcontainer.filter.WebAppFilterManager.invokeFilters", "1111", this);
            // LIBERTY: TODO send error when not include or forward
            ServletErrorReport errorReport = WebAppErrorReport.constructErrorReport(th2, requestProcessor);
            dispatchContext.pushException(th2);
            throw errorReport;
        } finally {
            //PM50111 Start
            WebContainerRequestState reqFilterState = WebContainerRequestState.getInstance(false);
            if(reqFilterState!= null) {
                reqFilterState.setInvokedFilters(false); // what if chain does not have any resource or had an exception.
                reqFilterState.removeAttribute("unFilteredRequestObject"); //PM88028
//                reqFilterState.removeAttribute("com.ibm.ws.webcontainer.ThisThreadSetWL");
            }

            //PM50111 End

            //PI08268
            if (checkDefaultMethodAttributeSet){
                httpServletReq.removeAttribute(attributeTargetClass);
                httpServletReq.removeAttribute("com.ibm.ws.webcontainer.security.checkdefaultmethod");
                checkDefaultMethodAttributeSet = false; 
            }
            //PI08268
            
            if (isRequest&&requestProcessor != null) {
                if (requestProcessor instanceof IServletWrapper) {
                    // 271276, do not add to cache if we have error status code
                    if (request.getAttribute(javax.servlet.RequestDispatcher.ERROR_STATUS_CODE) == null)
                        WebContainer.addToCache((HttpServletRequest) request, requestProcessor, this.webApp);
                    // tell the servlet that the request has completed.
                    if (requestProcessor instanceof ServletWrapper) {
                        ((ServletWrapper) requestProcessor).finishRequest(request);
                    }    
                }
            }
            try {
                if (collabMetaData!=null)
                    collabHelper.postInvokeCollaborators(collabMetaData, colEnum);
            } catch (Throwable th) {
                com.ibm.ws.ffdc.FFDCFilter.processException(th, "com.ibm.ws.webcontainer.filter.WebAppFilterManager.invokeFilters", "1132",
                                                            this);
                ServletErrorReport errorReport = WebAppErrorReport.constructErrorReport(th, requestProcessor);
                throw errorReport;
            }
        }

        if (isTraceOn && logger.isLoggable(Level.FINE)) {
            logger.exiting(CLASS_NAME, "invokeFilters", "result=" + result);
        }

        return result;
    }

    private WebComponentMetaData getDefaultComponentMetaData() {
        return this.defaultComponentMetaData;
    }

    private boolean isRethrowOriginalException(ServletRequest req, boolean isInclude, boolean isForward) {
        return (isInclude || isForward) && req.getAttribute(WebContainerConstants.IGNORE_DISPATCH_STATE) == null;
    }

    private String getPath(HttpServletRequest request) {
        String servletPath = null;
        String pathInfo = null;

        if (request.getAttribute(WebAppRequestDispatcher.REQUEST_URI_INCLUDE_ATTR) != null) {
            servletPath = (String) request.getAttribute(WebAppRequestDispatcher.SERVLET_PATH_INCLUDE_ATTR);
            pathInfo = (String) request.getAttribute(WebAppRequestDispatcher.PATH_INFO_INCLUDE_ATTR);
        }

        else {
            servletPath = request.getServletPath();
            pathInfo = request.getPathInfo();
        }

        if (pathInfo != null) {
            int semicolon = pathInfo.indexOf(';');

            if (semicolon != -1) {
                pathInfo = pathInfo.substring(0, semicolon);
            }
        }

        return (servletPath + pathInfo);
    }

    // End PK27620

    /**
     * Method areFiltersDefined.
     */
    public boolean areFiltersDefined() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "areFiltersDefined", "_filtersDefined->" + _filtersDefined);
        }
        return this._filtersDefined;
    }
    
    //PI08268
    private boolean setDefaultMethod(HttpServletRequest httpServletReq, String attributeTargetClass, String httpMethod){
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME,"setDefaultMethod","set attribute checkdefaultmethod ["+httpMethod+"] , and ["+ attributeTargetClass+"]");

        httpServletReq.setAttribute("com.ibm.ws.webcontainer.security.checkdefaultmethod", httpMethod);
        httpServletReq.setAttribute(attributeTargetClass, "TRUE"); 
        return true;
    }
    //PI08268

}
