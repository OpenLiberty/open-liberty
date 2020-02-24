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
package com.ibm.ws.webcontainer.webapp;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.FilterRegistration;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.SessionTrackingMode;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.BaseConfiguration;
import com.ibm.ws.container.ErrorPage;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.javaee.dd.common.DataSource;
import com.ibm.ws.javaee.dd.common.EJBRef;
import com.ibm.ws.javaee.dd.common.EnvEntry;
import com.ibm.ws.javaee.dd.common.JNDIEnvironmentRef;
import com.ibm.ws.javaee.dd.common.MessageDestinationRef;
import com.ibm.ws.javaee.dd.common.PersistenceContextRef;
import com.ibm.ws.javaee.dd.common.PersistenceUnitRef;
import com.ibm.ws.javaee.dd.common.ResourceEnvRef;
import com.ibm.ws.javaee.dd.common.ResourceRef;
import com.ibm.ws.javaee.dd.common.wsclient.ServiceRef;
import com.ibm.ws.javaee.dd.webext.Attribute;
import com.ibm.ws.javaee.dd.webext.MimeFilter;
import com.ibm.ws.javaee.dd.webext.WebExt;
import com.ibm.ws.resource.ResourceRefConfigFactory;
import com.ibm.ws.resource.ResourceRefConfigList;
import com.ibm.ws.session.SessionCookieConfigImpl;
import com.ibm.ws.session.SessionManagerConfig;
import com.ibm.ws.webcontainer.osgi.WebContainer;
import com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer.util.MetaInfResourceFinder;
import com.ibm.wsspi.injectionengine.JNDIEnvironmentRefBindingHelper;
import com.ibm.wsspi.injectionengine.JNDIEnvironmentRefType;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.webcontainer.WCCustomProperties;
import com.ibm.wsspi.webcontainer.filter.IFilterConfig;
import com.ibm.wsspi.webcontainer.filter.IFilterMapping;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;

/**
 * @author asisin
 */
@SuppressWarnings("unchecked")
public abstract class WebAppConfiguration extends BaseConfiguration implements WebAppConfigExtended {
    private static final String CLASS_NAME = WebAppConfiguration.class.getName();
    private static final Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.webapp");

    private static final TraceComponent tc = Tr.register(WebAppConfiguration.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);
    private static TraceNLS nls = TraceNLS.getTraceNLS(WebAppConfiguration.class, "com.ibm.ws.webcontainer.resources.Messages");

    private int version;
    private String contextRoot;
    private boolean isDefaultContextRootUsed;
    // private int sessionTimeout = -1; // negative is no timeout (default)
    private int sessionTimeout = 0; // use the value from the session config if
    // it's not been set in the web.xml
    private boolean moduleSessionTimeoutSet = false;
    private boolean moduleSessionTrackingModeSet = false;
    private SessionCookieConfigImpl sessionCookieConfig;
    private boolean hasProgrammaticCookieConfig = false;
    private EnumSet<SessionTrackingMode> sessionDefaultTrackingModeSet;
    private SessionManagerConfig sessionManagerConfig;
    private String displayName;
    private String description;
    private int reloadInterval;
    private boolean distributable;
    private boolean denyUncoveredHttpMethods;
    private boolean reloadingEnabled;
    private Boolean serveServletsByClassnameEnabled;
    private String defaultErrorPage;
    private String additionalClassPath;
    private Boolean fileServingEnabled;
    private Boolean directoryBrowsingEnabled;
    private boolean autoRequestEncoding;
    private boolean autoResponseEncoding;
    private boolean autoLoadFilters;
    private Map requestListeners;
    private Map requestAttributeListeners;
    private Map sessionListeners;
    private HashMap localeMap;
    private String moduleName;
    private String j2eeModuleName;
    private String moduleId;
    private boolean isSyncToThreadEnabled;
    private boolean encodeDispatchedRequestURI;

    private boolean isSystemApp;

    // LIDB3518-1.2 06/26/07 mmolden ARD
    private boolean ardEnabled;
    private String ardDispatchType;
    // LIDB3518-1.2 06/26/07 mmolden ARD

    // List of listener classes
    private ArrayList listeners = new ArrayList();

    // Welcome files (String filenames/servlet URIs)
    private List<String> welcomeFileList = new ArrayList<String>();

    // Might turn out that we will store it in a different datastructure
    // (servletName, mappings} redundancy
    private Map<String, List<String>> servletMappings;

    private int lastIndexBeforeDeclaredFilters=0;
    // ordered list of filterMappings
    private List<IFilterMapping> filterMappings;

    // {servletName, ServletConfig}
    private Map<String, IServletConfig> servletInfo;

    // {filterName, FilterConfig}
    private Map<String, IFilterConfig> filterInfo;

    // format for storage {String extension, String type}
    private Map<String, String> mimeMappings = new HashMap<String, String>();

    // MimeFilter objects {String mimeType, MimeFilter}
    private HashMap mimeFilters;
    private boolean isMimeFilteringEnabled = false;

    public WebGroup theWebGroup;

    private Map<String, String> jspAttributes = null;

    private HashMap fileServingAttributes = new HashMap();

    private HashMap invokerAttributes = new HashMap();

    private HashMap contextParams = new HashMap();

    private String virtualHost;
    private HashMap exceptionErrorPages = new HashMap();
    private HashMap codeErrorPages = new HashMap();
    private List tagLibList;
    private boolean precompileJsps;
    protected WebApp webApp;

    // begin LIDB2356.1: WebContainer work for incorporating SIP
    private List<String> virtualHostList = Collections.emptyList();
    // end LIDB2356.1: WebContainer work for incorporating SIP
    private int appStartupWeight;
    private int moduleStartupWeight;
    private boolean metaDataComplete;
    private List<Class<?>> classesToScan;
    private List<IFilterMapping> uriFilterMappingInfos;
    private List<IFilterMapping> servletFilterMappingInfos;
    private Map<String, Dynamic> dynamicServletRegistrationMap;
    private String applicationName;
    private List<String> libBinPathList;
    private Set<String> webXmlDefinedListeners = new HashSet<String>();

    private Map<JNDIEnvironmentRefType, List<? extends JNDIEnvironmentRef>> allRefs =
                    new EnumMap<JNDIEnvironmentRefType, List<? extends JNDIEnvironmentRef>>(JNDIEnvironmentRefType.class);
    private Map<JNDIEnvironmentRefType, Map<String, String>> allRefBindings = JNDIEnvironmentRefBindingHelper.createAllBindingsMap();
    private Map<String, String> envEntryValues;
    private ResourceRefConfigList resourceRefConfigList;
    
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    private static String disallowAllFileServingProp; // PK54499
    private static String disallowServeServletsByClassnameProp; // PK52059
    private boolean disableStaticMappingCache = false;      //PM84305
    private Map jspCachedLocations = null;
    private String primedSTSHeader = null;
    private boolean enablemultireadofpostdata = false; //MultiRead
    //since servlet40
    private String requestEncoding = null;
    private String responseEncoding = null;
    private static final String NULLSERVLETNAME = "com.ibm.ws.webcontainer.NullServletName"; //PI93226
    
    /**
     * Constructor.
     * 
     * @param id
     */
    public WebAppConfiguration(String id) {
        super(id);
        this.servletInfo = new ConcurrentHashMap<String, IServletConfig>();
        this.servletMappings = new HashMap<String, List<String>>();
        this.filterInfo = new HashMap<String, IFilterConfig>();
        this.filterMappings = new ArrayList();
        this.classesToScan = new ArrayList<Class<?>>();
    }

    /**
     * Returns the additionalClassPath.
     * 
     * @return String
     */
    public String getAdditionalClassPath() {
        return this.additionalClassPath;
    }

    // begin LIDB2356.1: WebContainer work for incorporating SIP
    public List getVirtualHostList() {
        return this.virtualHostList;
    }

    // end LIDB2356.1: WebContainer work for incorporating SIP

    /**
     * Returns the servlet name as obtained from the <servlet> definition in
     * web.xml
     */
    public Iterator getServletNames() {
        return this.servletInfo.keySet().iterator();
    }

    public void addServletInfo(String name, IServletConfig info) {
        if (name == null || name.isEmpty()){
            Tr.debug(tc, "addServletInfo", "servlet name is null/empty. Use internal servlet name " + NULLSERVLETNAME);
            name = NULLSERVLETNAME;
        }
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "addServletInfo", "servlet name: " + name);
        }

        this.servletInfo.put(name, info);
    }

    public void removeServletInfo(String name) {
        if (name == null || name.isEmpty()){
            Tr.debug(tc, "removeServletInfo", "servlet name is null/empty. Use internal servlet name " + NULLSERVLETNAME);
            name = NULLSERVLETNAME;
        }

        this.servletInfo.remove(name);
    }

    public Iterator<IServletConfig> getServletInfos() {
        return this.servletInfo.values().iterator();
    }

    public Iterator<IFilterConfig> getFilterInfos() {
        return this.filterInfo.values().iterator();
    }

    /**
     * Returns the autoRequestEncoding.
     * 
     * @return boolean
     */
    public boolean isAutoRequestEncoding() {
        return this.autoRequestEncoding;
    }

    /**
     * Returns the autoResponseEncoding.
     * 
     * @return boolean
     */
    public boolean isAutoResponseEncoding() {
        return this.autoResponseEncoding;
    }

    /**
     * Returns the defaultErrorPage.
     * 
     * @return String
     */
    public String getDefaultErrorPage() {
        return this.defaultErrorPage;
    }

    /**
     * Returns the directoryBrowsingEnabled.
     * 
     * @return boolean
     */
    public boolean isDirectoryBrowsingEnabled() {
        if (this.directoryBrowsingEnabled != null)
            return this.directoryBrowsingEnabled.booleanValue();

        directoryBrowsingEnabled = WCCustomProperties.DIRECTORY_BROWSING_ENABLED;

        return directoryBrowsingEnabled;
    }

    /**
     * Returns the fileServingEnabled.
     * 
     * @return boolean
     */
    public boolean isFileServingEnabled() {
        // PK54499 START
        disallowAllFileServingProp = WCCustomProperties.DISALLOW_ALL_FILE_SERVING;
        if (disallowAllFileServingProp != null && !this.getApplicationName().equalsIgnoreCase("isclite")) {
            try
            {
                if (Boolean.valueOf(disallowAllFileServingProp).booleanValue())
                {
                    this.fileServingEnabled = Boolean.FALSE;
                }
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                {
                    logger.logp(Level.FINE, CLASS_NAME, "isFileServing", "PK54499: disallowAllFileServingProp set to " + disallowAllFileServingProp + " for application: "
                                    + this.getApplicationName());
                }
            }
            catch (Exception x)
            {
                logger.logp(Level.SEVERE, CLASS_NAME, "isFileServing", "Illegal value set for property com.ibm.ws.webcontainer.disallowallfileserving");
            }
        }
        // PK54499 END

        if (this.fileServingEnabled != null)
            return this.fileServingEnabled.booleanValue();

        return WCCustomProperties.FILE_SERVING_ENABLED;
    }

    /**
     * Returns the mimeMappings.
     * 
     * @return Map
     */
    public Map getMimeMappings() {
        return this.mimeMappings;
    }

    /**
     * Returns the reloadingEnabled.
     * 
     * @return boolean
     */
    public boolean isReloadingEnabled() {
        return this.reloadingEnabled;
    }

    /**
     * Returns the reloadInterval.
     * 
     * @return int
     */
    public int getReloadInterval() {
        return this.reloadInterval;
    }

    /**
     * Returns the requestAttributeListeners.
     * 
     * @return Map
     */
    public Map getRequestAttributeListeners() {
        return this.requestAttributeListeners;
    }

    /**
     * Returns the requestListeners.
     * 
     * @return Map
     */
    public Map getRequestListeners() {
        return this.requestListeners;
    }

    /**
     * Returns the sessionListeners.
     * 
     * @return Map
     */
    public Map getSessionListeners() {
        return this.sessionListeners;
    }

    /**
     * Sets the additionalClassPath.
     * 
     * @param additionalClassPath
     *          The additionalClassPath to set
     */
    public void setAdditionalClassPath(String additionalClassPath) {
        this.additionalClassPath = additionalClassPath;
    }

    /**
     * Sets the autoRequestEncoding.
     * 
     * @param autoRequestEncoding
     *          The autoRequestEncoding to set
     */
    public void setAutoRequestEncoding(boolean autoRequestEncoding) {
        this.autoRequestEncoding = autoRequestEncoding;
    }

    /**
     * Sets the autoResponseEncoding.
     * 
     * @param autoResponseEncoding
     *          The autoResponseEncoding to set
     */
    public void setAutoResponseEncoding(boolean autoResponseEncoding) {
        this.autoResponseEncoding = autoResponseEncoding;
    }

    /**
     * Sets the defaultErrorPage.
     * 
     * @param defaultErrorPage
     *          The defaultErrorPage to set
     */
    public void setDefaultErrorPage(String defaultErrorPage) {
        this.defaultErrorPage = defaultErrorPage;
    }

    /**
     * Sets the directoryBrowsingEnabled.
     * 
     * @param directoryBrowsingEnabled
     *          The directoryBrowsingEnabled to set
     */
    public void setDirectoryBrowsingEnabled(Boolean directoryBrowsingEnabled) {
        this.directoryBrowsingEnabled = directoryBrowsingEnabled;
    }

    /**
     * Sets the fileServingEnabled.
     * 
     * @param fileServingEnabled
     *          The fileServingEnabled to set
     */
    public void setFileServingEnabled(Boolean fileServingEnabled) {
        this.fileServingEnabled = fileServingEnabled;
    }

    /**
     * Sets the mimeMappings.
     * 
     * @param mimeMappings
     *          The mimeMappings to set
     */
    public void setMimeMappings(HashMap mimeMappings) {
        this.mimeMappings = mimeMappings;
    }

    /**
     * Sets the reloadingEnabled.
     * 
     * @param reloadingEnabled
     *          The reloadingEnabled to set
     */
    public void setReloadingEnabled(boolean reloadingEnabled) {
        this.reloadingEnabled = reloadingEnabled;
    }

    /**
     * Sets the reloadInterval.
     * 
     * @param reloadInterval
     *          The reloadInterval to set
     */
    public void setReloadInterval(int reloadInterval) {
        this.reloadInterval = reloadInterval;
    }

    /**
     * Sets the requestAttributeListeners.
     * 
     * @param requestAttributeListeners
     *          The requestAttributeListeners to set
     */
    public void setRequestAttributeListeners(Map requestAttributeListeners) {
        this.requestAttributeListeners = requestAttributeListeners;
    }

    /**
     * Sets the requestListeners.
     * 
     * @param requestListeners
     *          The requestListeners to set
     */
    public void setRequestListeners(Map requestListeners) {
        this.requestListeners = requestListeners;
    }

    /**
     * Sets the sessionListeners.
     * 
     * @param sessionListeners
     *          The sessionListeners to set
     */
    public void setSessionListeners(Map sessionListeners) {
        this.sessionListeners = sessionListeners;
    }

    /**
     * Returns the displayName.
     * 
     * @return String
     */
    public String getDisplayName() {
        return this.displayName;
    }

    /**
     * Sets the displayName.
     * 
     * @param displayName
     *          The displayName to set
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the jspAttributes.
     * 
     * @return Map<String,String>
     */
    public Map<String, String> getJspAttributes() {
        if (null == this.jspAttributes) {
            this.jspAttributes = new HashMap<String, String>();
        }
        return this.jspAttributes;
    }

    /**
     * Sets the jspAttributes.
     * 
     * @param jspAttributes
     *            The jspAttributes to set
     */
    public void setJspAttributes(Map<String, String> jspAttributes) {
        this.jspAttributes = jspAttributes;
    }

    public String toString() {
        return this.displayName;
    }

    /**
     * Returns the contextParams.
     * 
     * @return List
     */
    public java.util.HashMap getContextParams() {
        return this.contextParams;
    }

    /**
     * Sets the contextParams.
     * 
     * @param contextParams
     *            The contextParams to set
     */
    public void setContextParams(java.util.HashMap contextParams) {
        this.contextParams = contextParams;
    }

    /**
     * Method getServletInfo.
     * 
     * @param string
     * @return ServletConfig
     */
    public IServletConfig getServletInfo(String string) {
        if (string == null || string.isEmpty()){
            Tr.debug(tc, "getServletInfo", "servlet name is null/empty. Use internal servlet name " + NULLSERVLETNAME);
            string = NULLSERVLETNAME;
        }

        return (IServletConfig) this.servletInfo.get(string);
    }

    /**
     * Returns the fileServingAttributes.
     * 
     * @return List
     */
    public HashMap getFileServingAttributes() {
        return this.fileServingAttributes;
    }

    /**
     * Sets the fileServingAttributes.
     * 
     * @param fileServingAttributes
     *            The fileServingAttributes to set
     */
    public void setFileServingAttributes(HashMap fileServingAttributes) {
        this.fileServingAttributes = fileServingAttributes;
    }

    /**
     * Returns the serveServletsByClassname.
     * 
     * @return boolean
     */
    public boolean isServeServletsByClassnameEnabled() {
        // PK52059 START
        disallowServeServletsByClassnameProp = WCCustomProperties.DISALLOW_SERVE_SERVLETS_BY_CLASSNAME_PROP;
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "isServeServletsByClassnameEnabled", "disallowServeServletsByClassnameProp = " + disallowServeServletsByClassnameProp);
        }
        if (disallowServeServletsByClassnameProp != null) {
            try {
                if (Boolean.valueOf(disallowServeServletsByClassnameProp).booleanValue()) {
                    this.serveServletsByClassnameEnabled = Boolean.FALSE;
                }
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "isServeServletsByClassnameEnabled", "PK52059: disallowServeServletsByClassnameProp set to "
                                    + disallowServeServletsByClassnameProp
                                    + " for application: " + this.getApplicationName());
                }
            } catch (Exception x) {
                logger.logp(Level.SEVERE, CLASS_NAME, "isServeServletsByClassnameEnabled",
                                "Illegal value set for property com.ibm.ws.webcontainer.disallowserveservletsbyclassname");
            }
        }  // PK52059 END

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "isServeServletsByClassnameEnabled", "value = " + (this.serveServletsByClassnameEnabled != null ? this.serveServletsByClassnameEnabled.booleanValue() : WCCustomProperties.SERVE_SERVLETS_BY_CLASSNAME_ENABLED));
        }

        if (this.serveServletsByClassnameEnabled != null) 
            return this.serveServletsByClassnameEnabled.booleanValue();

        return WCCustomProperties.SERVE_SERVLETS_BY_CLASSNAME_ENABLED;
    }

    /**
     * Sets the serveServletsByClassname.
     * 
     * @param serveServletsByClassname
     *            The serveServletsByClassname to set
     */
    public void setServeServletsByClassnameEnabled(Boolean serveServletsByClassname) {
        this.serveServletsByClassnameEnabled = serveServletsByClassname;
    }

    /**
     * Returns the invokerAttributes.
     * 
     * @return List
     */
    public HashMap getInvokerAttributes() {
        return this.invokerAttributes;
    }

    /**
     * Sets the invokerAttributes.
     * 
     * @param invokerAttributes
     *            The invokerAttributes to set
     */
    public void setInvokerAttributes(HashMap invokerAttributes) {
        this.invokerAttributes = invokerAttributes;
    }

    public int getLastIndexBeforeDeclaredFilters() {
        return lastIndexBeforeDeclaredFilters;
    }

    public void setLastIndexBeforeDeclaredFilters(int lastIndexBeforeDeclaredFilters) {
        this.lastIndexBeforeDeclaredFilters = lastIndexBeforeDeclaredFilters;
    }

    /**
     * Returns the filterMappings.
     * 
     * @return List
     */
    public List getFilterMappings() {
        return this.filterMappings;
    }

    /**
     * Sets the filterMappings.
     * 
     * @param filterMappings
     *            The filterMappings to set
     */
    public void setFilterMappings(List<IFilterMapping> filterMappings) {
        this.filterMappings = filterMappings;
    }

    /**
     * Method getFilterInfo.
     * 
     * @param filterName
     * @return FilterConfig
     */
    public IFilterConfig getFilterInfo(String filterName) {
        return this.filterInfo.get(filterName);
    }

    // Begin 202490f_4
    /**
     * Method addFilterInfo.
     * 
     * @param config
     */
    public void addFilterInfo(IFilterConfig config) {
        this.filterInfo.put(config.getFilterName(), config);
    }

    // End 202490f_4

    /**
     * Method getVirtualHostName.
     * 
     * @return String
     */
    public String getVirtualHostName() {
        return this.virtualHost;
    }

    /**
     * Returns the servletMappings.
     * 
     * @return HashMap
     */
    public Map<String, List<String>> getServletMappings() {
        return this.servletMappings;
    }

    /**
     * Sets the servletMappings.
     * 
     * @param servletMappings
     *            The servletMappings to set
     */
    public void setServletMappings(Map<String, List<String>> servletMappings) {
        this.servletMappings = servletMappings;
    }

    /**
     * Method getServletMapping.
     * 
     * @param servletName
     * @return ServletMapping
     */
    public List<String> getServletMappings(String servletName) {
        return this.servletMappings.get(servletName);
    }

    /**
     * Returns the mimeFilters.
     * 
     * @return List
     */
    public HashMap getMimeFilters() {
        return this.mimeFilters;
    }

    /**
     * Sets the mimeFilters.
     * 
     * @param mimeFilters
     *            The mimeFilters to set
     */
    public void setMimeFilters(HashMap mimeFilters) {
        if (mimeFilters != null && mimeFilters.size() > 0) {
            this.isMimeFilteringEnabled = true;
        }
        this.mimeFilters = mimeFilters;
    }

    public String getMimeType(String extension) {
        return (String) this.mimeMappings.get(extension);
    }

    /**
     * Method getErrorPageByExceptionType.
     * 
     * @param th
     * @return ErrorPage
     */
    public ErrorPage getErrorPageTraverseRootCause(Throwable th) {
        while (th != null && th instanceof ServletException) { // defect 155880
            // - Check
            // rootcause !=
            // null
            Throwable rootCause = ((ServletException) th).getRootCause();
            if (rootCause == null) {
                break;
            }
            ErrorPage er = getErrorPageByExceptionType(th);
            if (er != null)
                return er;

            th = rootCause;
        }
        if (th != null)
            return getErrorPageByExceptionType(th);
        return null;
    }

    /**
     * Method getErrorPageByExceptionType.
     * 
     * @param rootException
     * @return ErrorPage
     */
    public ErrorPage getErrorPageByExceptionType(Throwable rootException) {
        // Begin 256281
        // Check for a perfect match first
        String exceptionName = rootException.getClass().getName();
        Object obj = exceptionErrorPages.get(exceptionName);
        if (obj != null)
            return (ErrorPage) obj;
        // Check to see if its a child of another exception type in the list
        Iterator i = exceptionErrorPages.values().iterator();
        ErrorPage curEP = null;
        ClassLoader warClassLoader = getWebApp().getClassLoader();
        while (i.hasNext()) {
            ErrorPage ep = (ErrorPage) i.next();
            // Class exceptionClass = ep.getException();
            Class exceptionClass = ep.getException(warClassLoader);
            if (exceptionClass != null && exceptionClass.isInstance(rootException)) {
                if (curEP == null)
                    curEP = ep;
                // else if
                // (curEP.getException().isAssignableFrom(exceptionClass))
                // //PK52168
                else if (curEP.getException(warClassLoader).isAssignableFrom(exceptionClass)) // PK52168
                    curEP = ep;
            }
        }
        return curEP;
        // End 256281
    }

    /**
     * Get an error page based on the error code.
     * 
     * @param code
     * @return ErrorPage
     */
    public ErrorPage getErrorPageByErrorCode(Integer code) {
        return (ErrorPage) this.codeErrorPages.get(code);
    }

    /**
     * Method getWelcomeFileList.
     * 
     * @return List<String>
     */
    public List<String> getWelcomeFileList() {
        return this.welcomeFileList;
    }

    /**
     * Method setWelcomeFileList.
     */
    public void setWelcomeFileList(List<String> list) {
        this.welcomeFileList = list;
    }

    /**
     * Method getTagLibs.
     * 
     * @return List
     */
    public List getTagLibs() {
        return this.tagLibList;
    }

    public void setTagLibs(java.util.List l) {
        this.tagLibList = l;
    }

    /**
     * Returns the listeners.
     * 
     * @return List
     */
    public List getListeners() {
        return this.listeners;
    }

    public void addListener(String listenerClass) {
        this.listeners.add(listenerClass);
    }

    // public void addGlobalListeners(ArrayList globalListeners)
    // {
    // listeners.addAll(0, globalListeners);
    //

    /**
     * Sets the listeners.
     * 
     * @param listeners
     *          The listeners to set
     */
    public void setListeners(ArrayList listeners) {
        this.listeners = listeners;
    }


    public void setWebXmlDefinedListeners(Set<String> setOfWebXmlDefinedListeners) {
        this.webXmlDefinedListeners = setOfWebXmlDefinedListeners;
    }

    public Set<String> getWebXmlDefinedListeners() {
        return webXmlDefinedListeners;
    }

    /**
     * Returns the moduleId.
     * 
     * @return String
     */
    public String getModuleId() {
        return this.moduleId;
    }

    /**
     * Returns the moduleName.
     * 
     * @return String
     */
    public String getModuleName() {
        return this.moduleName;
    }

    public String getJ2EEModuleName() {
        return this.j2eeModuleName;
    }

    /**
     * Return the applicationName.
     * 
     * @return String
     */
    public String getApplicationName() {
        if (this.applicationName != null)
            return this.applicationName;
        else if (webApp != null)
            return this.webApp.getApplicationName();
        else
            return null;

    }

    /**
     * Sets the moduleId.
     * 
     * @param moduleId
     *          The moduleId to set
     */
    public void setModuleId(String moduleId) {
        this.moduleId = moduleId;
    }

    /**
     * Sets the moduleName.
     * 
     * @param moduleName
     *          The moduleName to set
     */
    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public void setJ2EEModuleName(String j2eeModuleName) {
        this.j2eeModuleName = j2eeModuleName;
    }

    /**
     * Returns the description.
     * 
     * @return String
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Sets the description.
     * 
     * @param description
     *          The description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the distributable.
     * 
     * @return boolean
     */
    public boolean isDistributable() {
        return this.distributable;
    }

    /**
     * Sets the distributable.
     * 
     * @param distributable
     *          The distributable to set
     */
    public void setDistributable(boolean distributable) {
        this.distributable = distributable;
    }


    /**
     * Sets the denyUncoveredHttpMethods.
     * 
     * @param denyUncoveredHttpMethods
     *          The denyUncoveredHttpMethods to set
     */
    public void setDenyUncoveredHttpMethods(boolean denyUncoveredHttpMethods) {
        this.denyUncoveredHttpMethods = denyUncoveredHttpMethods;
    }

    /**
     * Returns the denyUncoveredHttpMethods.
     * 
     * @return boolean
     */
    public boolean isDenyUncoveredHttpMethods() {
        return this.denyUncoveredHttpMethods;
    }




    /**
     * Sets the codeErrorPages.
     * 
     * @param codeErrorPages
     *          The codeErrorPages to set
     */
    public void setCodeErrorPages(HashMap codeErrorPages) {
        this.codeErrorPages = codeErrorPages;
    }

    /**
     * Sets the exceptionErrorPages.
     * 
     * @param exceptionErrorPages
     *          The exceptionErrorPages to set
     */
    public void setExceptionErrorPages(HashMap exceptionErrorPages) {
        this.exceptionErrorPages = exceptionErrorPages;
    }

    /**
     * Returns the sessionTimeout.
     * 
     * @return int
     */
    public int getSessionTimeout() {
        return this.sessionTimeout;
    }

    public boolean isModuleSessionTimeoutSet() {
        return this.moduleSessionTimeoutSet;
    }

    public boolean isModuleSessionTrackingModeSet() {
        return this.moduleSessionTrackingModeSet;
    }

    /**
     * Sets the sessionTimeout.
     * 
     * @param sessionTimeout
     *          The sessionTimeout to set
     */
    public void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public void setModuleSessionTimeoutSet(boolean b) {
        this.moduleSessionTimeoutSet = b;
    }

    public void setModuleSessionTrackingModeSet(boolean b) {
        this.moduleSessionTrackingModeSet = b;
    }

    public SessionManagerConfig getSessionManagerConfig() {
        return this.sessionManagerConfig;
    }

    public void setSessionManagerConfig(SessionManagerConfig smcBase) {
        this.sessionManagerConfig = smcBase;
    }

    public SessionCookieConfigImpl getSessionCookieConfig() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
        {
            logger.logp(Level.FINE, CLASS_NAME, "getSessionCookieConfigurator", "scc = " + this.sessionCookieConfig  + " for application: "
                            + this.getApplicationName());
        }
        return this.sessionCookieConfig;
    }

    public void setSessionCookieConfig(SessionCookieConfigImpl scc) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
        {
            logger.logp(Level.FINE, CLASS_NAME, "setSessionCookieConfig", "scc = " + scc + " for application: "
                            + this.getApplicationName());
        }

        this.sessionCookieConfig = scc;
    }

    public void setSessionCookieConfigInitilialized() {
        if (this.sessionCookieConfig!=null && WebContainer.getServletContainerSpecLevel() >= WebContainer.SPEC_LEVEL_31) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            {
                logger.logp(Level.FINE, CLASS_NAME, "setSessionCookieConfigInitialized", "scc = " +  this.sessionCookieConfig + " for application: "
                                + this.getApplicationName());
            }
            this.sessionCookieConfig.setContextInitialized();
        }
    }

    public void setHasProgrammaticCookieConfig() {
        hasProgrammaticCookieConfig = true;
    }

    public boolean hasProgrammaticCookieConfig() {
        return hasProgrammaticCookieConfig;
    }

    public EnumSet<SessionTrackingMode> getSessionTrackingMode() {
        if (this.sessionManagerConfig == null)
        {
            return getDefaultSessionTrackingMode();
        } else {
            return EnumSet.copyOf(this.sessionManagerConfig.getSessionTrackingMode());
        }
    }

    public EnumSet<SessionTrackingMode> getDefaultSessionTrackingMode() {
        if (this.sessionDefaultTrackingModeSet==null) {
            return EnumSet.noneOf(SessionTrackingMode.class);
        } else {
            return EnumSet.copyOf(this.sessionDefaultTrackingModeSet);
        }
    }

    public EnumSet<SessionTrackingMode> getInternalDefaultSessionTrackingMode() {
        return this.sessionDefaultTrackingModeSet;
    }

    // stm can't be null
    public void setSessionTrackingMode(Set<SessionTrackingMode> stm) {
        sessionManagerConfig.setEffectiveTrackingModes(EnumSet.copyOf(stm));
    }

    // only called internally during startup
    public void setDefaultSessionTrackingMode(EnumSet<SessionTrackingMode> stm) {
        this.sessionDefaultTrackingModeSet = stm;
    }

    public void setServletInfos(Map<String, IServletConfig> sInfos) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "setServletInfos", "servletInfo --> " + sInfos);
        }
        this.servletInfo = sInfos;
    }

    public void setAppStartupWeight(int appStartupWeight) {
        this.appStartupWeight = appStartupWeight;
    }

    public int getAppStartupWeight() {
        return this.appStartupWeight;
    }

    public void setFilterInfos(Map<String, IFilterConfig> fInfos) {
        this.filterInfo = fInfos;
    }

    @FFDCIgnore(NullPointerException.class) // Caused by a user error that is flagged in the console, so don't ffdc.
    public void addServletMapping(String servletName, String urlPattern) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME, "addServletMapping", "adding servletName->" + servletName + ", urlPattern->" + urlPattern);

        List<String> mappings = this.servletMappings.get(servletName);
        if (mappings == null) {
            IServletConfig scon = getServletInfo(servletName);
            mappings = new ArrayList<String>();
            mappings.add(urlPattern);

            // add to web group mappings
            servletMappings.put(servletName, mappings);

            // add to servlet's config mapping
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "addServletMapping", "servletInfo --> " + servletInfo + " servletName --> " + servletName);
            }

            // Start: d70465

            // d70465: When the mapping doesn't match a servlet, 'scon' is null, which results
            //         in a NPE.  'servlet.name.for.servlet.mapping.not.found' displays incomplete
            //         information.  As a temporary fix, a more complete message is added to the
            //         NPE.  The case of an NPE from within 'setMappings' is not updated.  That
            //         is unlikely, but is still handled to avoid a behavioral change.
            //
            // d98303: (TODO): Opened to cleanup the NLS and exception handling.  Throwing an NPE here
            //         fails the module startup, perhaps not the desired behavior in a developer
            //         mode.  However, the behavior is propagated to callers somewhat widely,
            //         making a change impossible (for now).

            if (scon == null) { // d70465: Peel off this particular case.)
                Object[] objectArray = new Object[] {servletName, urlPattern, getModuleName(), getApplicationName()};
                logger.logp(Level.SEVERE, CLASS_NAME, "addServletMapping", "servlet.name.for.servlet.mapping.not.found", objectArray);
                String msg = nls.getFormattedMessage("servlet.name.for.servlet.mapping.not.found", objectArray, "Servlet name not found adding servlet mapping; servlet name=" + servletName + "; URL pattern=" + urlPattern + "; module=" + getModuleName() + "; application=" + getApplicationName());
                throw new RuntimeException(new MetaDataException(msg));            
            } else {
                scon.setMappings(mappings);
            }

            // END: d70465

        } else {
            boolean found = false;
            for (String curPattern : mappings) {
                if (urlPattern.equals(curPattern)) {
                    found = true;
                    break; // we know we don't need to add it now
                }
            }

            if (!found) {
                mappings.add(urlPattern);

                // no need to set the mappings in the ServletConfig
                // because it already points to the mappings List
                // and all we need to to is update the list.
            }
        }
    }

    /**
     * Method setVirtualHostName.
     * 
     * @param string
     */
    public void setVirtualHostName(String string) {
        this.virtualHost = string;
    }

    /**
     * Method setPrecompileJSPs.
     * 
     * @param b
     */
    public void setPrecompileJSPs(boolean b) {
        this.precompileJsps = b;
    }

    public boolean getPreCompileJSPs() {
        return this.precompileJsps;
    }

    /**
     * Returns the autoLoadFilters.
     * 
     * @return boolean
     */
    public boolean isAutoLoadFilters() {
        return this.autoLoadFilters;
    }

    /**
     * Sets the autoLoadFilters.
     * 
     * @param autoLoadFilters
     *          The autoLoadFilters to set
     */
    public void setAutoLoadFilters(boolean autoLoadFilters) {
        this.autoLoadFilters = autoLoadFilters;
    }

    /**
     * @param locale
     * @return String
     */
    public String getLocaleEncoding(Locale locale) {
        if (localeMap == null)
            return null;

        String encoding = (String) localeMap.get(locale.toString());
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "getLocaleEncoding", "locale->" + locale);
        }

        if (encoding == null) {
            encoding = (String) localeMap.get(locale.getLanguage() + "_" + locale.getCountry());
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "getLocaleEncoding", "locale->" + locale + ", language->" + locale.getLanguage() + ", country->"
                                + locale.getCountry());
            }
            if (encoding == null) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "getLocaleEncoding", "locale->" + locale + ", language->" + locale.getLanguage());
                }
                encoding = (String) localeMap.get(locale.getLanguage());
            }
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "getLocaleEncoding", "encoding->" + encoding);
        }
        return encoding;
    }

    public void addLocaleEncodingMap(String locale, String encoding) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "addLocaleEncodingMap", "locale->" + locale + ", encoding->" + encoding);
        }
        if (localeMap == null)
            localeMap = new HashMap();

        localeMap.put(locale, encoding);
    }

    /**
     * @return
     */
    public boolean isMimeFilteringEnabled() {
        return this.isMimeFilteringEnabled;
    }

    /**
     * @return
     */
    public boolean isServlet2_4() {
        return (this.version == 24);
    }

    /**
     * @return
     */
    public boolean isServlet2_5() {
        return (this.version == 25);
    }

    /**
     * @return
     */
    public boolean isServlet2_4OrHigher() {
        return (this.version >= 24);
    }

    /**
     * @return
     */
    public com.ibm.ws.webcontainer.webapp.WebApp getWebApp() {
        return this.webApp;
    }

    /**
     * @param app
     */
    public void setWebApp(com.ibm.ws.webcontainer.webapp.WebApp app) {
        this.webApp = app;
    }

    /**
     * @param servletName
     */
    public void removeServletMappings(String servletName) {
        this.servletMappings.remove(servletName);
    }

    /**
     * @param contextRoot
     */
    public void setContextRoot(String contextRoot) {
        this.contextRoot = contextRoot;

    }

    /**
     * @return
     */
    public String getContextRoot() {
        return this.contextRoot;
    }
    
    /**
     * 
     * @param isDefaultContextRootUsed
     */
    public void setDefaultContextRootUsed(boolean isDefaultContextRootUsed) {
        this.isDefaultContextRootUsed = isDefaultContextRootUsed;
    }
    
    /**
     * 
     * @return
     */
    public boolean isDefaultContextRootUsed() {
        return this.isDefaultContextRootUsed;
    }

    /**
     * @param i
     */
    public void setVersion(int i) {
        this.version = i;
    }

    public int getVersion() {
        return this.version;
    }

    /**
     * @return
     */
    public boolean isSyncToThreadEnabled() {
        return this.isSyncToThreadEnabled;
    }

    /**
     * @param b
     */
    public void setSyncToThreadEnabled(boolean b) {
        this.isSyncToThreadEnabled = b;
    }

    public int getModuleStartupWeight() {
        return this.moduleStartupWeight;
    }

    public void setModuleStartupWeight(int moduleStartupWeight) {
        this.moduleStartupWeight = moduleStartupWeight;
    }

    // LIDB3816
    public HashMap getCodeErrorPages() {
        return this.codeErrorPages;

    }

    // LIDB3816
    public HashMap getExceptionErrorPages() {
        return this.exceptionErrorPages;
    }

    // LIDB3518-1.2 06/26/07 mmolden ARD
    /**
     * @return Returns the ardDispatchType.
     */
    public String getArdDispatchType() {
        return this.ardDispatchType;
    }

    /**
     * @param ardDispatchType
     *          The ardDispatchType to set.
     */
    public void setArdDispatchType(String ardDispatchType) {
        this.ardDispatchType = ardDispatchType;
        if ((this.ardDispatchType).equals("CLIENT_SIDE") || (this.ardDispatchType).equals("SERVER_SIDE"))
            this.ardEnabled = true;
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "setArdDispatchType", " type --> " + ardDispatchType);
        }
    }

    /**
     * @return Returns the ardEnabled.
     */
    public boolean isArdEnabled() {
        return this.ardEnabled;
    }

    // LIDB3518-1.2 06/26/07 mmolden ARD

    public void setMetadataComplete(boolean b) {
        this.metaDataComplete = b;
    }

    public boolean isMetadataComplete() {
        return this.metaDataComplete;
    }

    public void addClassesToScan(List<Class<?>> list) {
        this.classesToScan.addAll(list);
    }

    public List<Class<?>> getClassesToScan() {
        return this.classesToScan;
    }

    // Start PK66137
    /**
     * @return
     */
    public boolean isSystemApp() {
        return this.isSystemApp;
    }

    /**
     * @param b
     */
    public void setSystemApp(boolean b) {
        this.isSystemApp = b;
    }

    // End PK66137

    public void addUriMappedFilterInfo(IFilterMapping fmInfo) {
        if (uriFilterMappingInfos == null) {
            uriFilterMappingInfos = new ArrayList<IFilterMapping>();
        }
        uriFilterMappingInfos.add(fmInfo);
    }

    public void addServletMappedFilterInfo(IFilterMapping fmInfo) {
        if (servletFilterMappingInfos == null) {
            servletFilterMappingInfos = new ArrayList<IFilterMapping>();
        }
        servletFilterMappingInfos.add(fmInfo);
    }

    public List<IFilterMapping> getUriFilterMappings() {
        return this.uriFilterMappingInfos;
    }

    public List<IFilterMapping> getServletFilterMappings() {
        return this.servletFilterMappingInfos;
    }

    public Map<String, ? extends ServletRegistration> getServletInfoMap() {
        // TODO Auto-generated method stub
        return this.servletInfo;
    }

    public Map<String, ? extends FilterRegistration> getFilterInfoMap() {
        // TODO Auto-generated method stub
        return this.filterInfo;
    }

    public void addDynamicServletRegistration(String servletName,
                                              Dynamic dynamicServletRegistration) {
        if (this.dynamicServletRegistrationMap==null){
            this.dynamicServletRegistrationMap = new HashMap<String, Dynamic>();
        }
        this.dynamicServletRegistrationMap.put(servletName, dynamicServletRegistration);
    }

    public Map<String, ? extends Dynamic> getDynamicServletRegistrations() {
        return this.dynamicServletRegistrationMap;
    }

    public void setLibBinPathList(List<String> libBinPathList) {
        this.libBinPathList = libBinPathList;
    }

    @Override
    public List<String> getLibBinPathList() {
        if (libBinPathList == null) {
            List<String> libBinPathListTemp = new ArrayList<String>();
            if (webApp != null) {
                MetaInfResourceFinder metaInfRes = webApp.getMetaInfResourceFinder();
                for (URL url : metaInfRes.getJarResourceURLs()) {
                    String path;
                    if ("jar".equals(url.getProtocol())) {
                        path = url.getFile().substring("file:".length(), url.getFile().indexOf("!/"));
                    } else if ("file".equals(url.getProtocol())) {
                        path = url.getPath();
                    } else {
                        continue;
                    }
                    libBinPathListTemp.add(path);
                }
            }
            libBinPathList = libBinPathListTemp;
        }
        return libBinPathList;
    }

    @Override
    public boolean isJCDIEnabled() {
        return false;
    }

    @Override
    public void setJCDIEnabled(boolean b) {}

    public Map<JNDIEnvironmentRefType, List<? extends JNDIEnvironmentRef>> getAllRefs() {
        return allRefs;
    }

    private <T> List<T> getRefs(JNDIEnvironmentRefType refType, Class<T> type) {
        if (type != refType.getType()) {
            throw new IllegalArgumentException();
        }
        @SuppressWarnings("rawtypes")
        List<T> refs = (List) this.allRefs.get(refType);
        return refs == null ? Collections.<T> emptyList() : refs;
    }

    public List<ServiceRef> getServiceRefs() {
        return getRefs(JNDIEnvironmentRefType.ServiceRef, ServiceRef.class);
    }

    public List<ResourceRef> getResourceRefs() {
        return getRefs(JNDIEnvironmentRefType.ResourceRef, ResourceRef.class);
    }

    public List<ResourceEnvRef> getResourceEnvRefs() {
        return getRefs(JNDIEnvironmentRefType.ResourceEnvRef, ResourceEnvRef.class);
    }

    public List<MessageDestinationRef> getMessageDestinationRefs() {
        return getRefs(JNDIEnvironmentRefType.MessageDestinationRef, MessageDestinationRef.class);
    }

    /**
     * Access the list of env-entries defined. This is never null but
     * might be empty.
     * 
     */
    public List<EnvEntry> getEnvEntries() {
        return getRefs(JNDIEnvironmentRefType.EnvEntry, EnvEntry.class);
    }

    public List<DataSource> getDataSources() {
        return getRefs(JNDIEnvironmentRefType.DataSource, DataSource.class);
    }

    public Map<JNDIEnvironmentRefType, Map<String, String>> getAllRefBindings() {
        return allRefBindings;
    }

    public Map<String, String> getEnvEntryValues() {
        if (this.envEntryValues == null) {
            this.envEntryValues = new HashMap<String, String>();
        }
        return this.envEntryValues;
    }

    public ResourceRefConfigList getResourceRefConfigList(ResourceRefConfigFactory resourceRefConfigFactory) {
        if (this.resourceRefConfigList == null && resourceRefConfigFactory != null) {
            this.resourceRefConfigList = resourceRefConfigFactory.createResourceRefConfigList();
        }
        return this.resourceRefConfigList;
    }

    /**
     * This is never null but
     * might be empty.
     */
    public List<? extends PersistenceContextRef> getPersistenceContextRefs() {
        return getRefs(JNDIEnvironmentRefType.PersistenceContextRef, PersistenceContextRef.class);
    }

    /**
     * This is never null but
     * might be empty.
     */
    public List<? extends PersistenceUnitRef> getPersistenceUnitRefs() {
        return getRefs(JNDIEnvironmentRefType.PersistenceUnitRef, PersistenceUnitRef.class);
    }

    public List<? extends EJBRef> getEJBRefs() {
        return getRefs(JNDIEnvironmentRefType.EJBRef, EJBRef.class);
    }

    public void setInitializeWebExtProps(WebExt ext) {
        String methodName = "setInitializeWebExtProps";

        if (ext.isSetAutoEncodeRequests()) {
            this.setAutoRequestEncoding(ext.isAutoEncodeRequests());
        }
        if (ext.isSetAutoEncodeResponses()) {
            this.setAutoResponseEncoding(ext.isAutoEncodeResponses());
        }
        if (ext.isSetAutoloadFilters()) {
            this.setAutoLoadFilters(ext.isAutoloadFilters());
        }
        if (ext.isSetEnableDirectoryBrowsing()) {
            this.setDirectoryBrowsingEnabled(ext.isEnableDirectoryBrowsing());
        }
        if (ext.isSetEnableFileServing()) {
            this.setFileServingEnabled(ext.isEnableFileServing());
        }
        if (ext.isSetEnableReloading()) {
            this.setReloadingEnabled(ext.isEnableReloading());
        }
        if (ext.isSetEnableServingServletsByClassName()) {
            this.setServeServletsByClassnameEnabled(ext.isEnableServingServletsByClassName());
        }
        if (ext.isSetPreCompileJsps()) {
            this.setPrecompileJSPs(ext.isPreCompileJsps());
        }
        if (ext.isSetReloadInterval()) {
            this.setReloadInterval(ext.getReloadInterval());
        }

        // Attempt to assign the default error page.
        //
        // Override the descriptor error page if in Servlet 3.0;
        // Ignore the extension error page if in Servlet 3.1.
        //
        // See also 'com.ibm.ws.webcontainer.osgi.container.config.
        // WebAppConfiguratorHelper.ConfigurationWriter.setDefaultErrorPage'.

        String extPage=ext.getDefaultErrorPage();
        if ((extPage != null) && (extPage.length() > 0)) {
            String currentPage = getDefaultErrorPage();        
            if (!extPage.equals(currentPage)) {
                if ((currentPage != null) && (currentPage.length() != 0)) {
                    if (WebContainer.getServletContainerSpecLevel() >= WebContainer.SPEC_LEVEL_31) {                                
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {            
                            logger.logp(Level.FINE, CLASS_NAME, methodName,
                                        "Servlet 3.1: Descriptor default error page " + currentPage +
                                        " overrides extension default error page " + extPage + ".");
                        }
                    } else {
                        this.setDefaultErrorPage(extPage);
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {            
                            logger.logp(Level.FINE, CLASS_NAME, methodName,
                                        "Servlet 3.0: Descriptor default error page " + currentPage +
                                        " overridden by extension default error page " + extPage + ".");
                        }
                    }                                
                } else {
                    this.setDefaultErrorPage(extPage);
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {            
                        logger.logp(Level.FINE, CLASS_NAME, methodName,
                                    "Assigning ibm-web-ext.xml default error page " + extPage + ".");
                    }                
                }
            } else {
                // Ignore it: it's the same as the current default error page.
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {            
                    logger.logp(Level.FINE, CLASS_NAME, methodName,
                                "ibm-web-ext.xml default error page page same as descriptor default error page " + extPage + ".");
                }                            
            }
        }

        List<Attribute> fileServingAttributes=ext.getFileServingAttributes();
        if (fileServingAttributes!=null && fileServingAttributes.size()>0) {
            HashMap fileServingHashMap = convertAttributeListToMap(fileServingAttributes);
            this.setFileServingAttributes(fileServingHashMap);
        }

        List<Attribute> jspAttributes = ext.getJspAttributes();
        if (jspAttributes !=null && jspAttributes.size() > 0) {
            HashMap jspServingHashMap = convertAttributeListToMap(jspAttributes);
            this.setJspAttributes(jspServingHashMap);
        }

        List<Attribute> invokerAttributes=ext.getInvokerAttributes();
        if (invokerAttributes!=null && invokerAttributes.size()>0) {
            HashMap invokerHashMap = convertAttributeListToMap(invokerAttributes);
            this.setInvokerAttributes(invokerHashMap);
        }
        List<MimeFilter> mimeFilters=ext.getMimeFilters();
        if (fileServingAttributes!=null && fileServingAttributes.size()>0) {
            HashMap mimeFiltersHashMap = convertMimeFiltersToMap(mimeFilters);
            this.setMimeFilters(mimeFiltersHashMap);
        }
        /*List<com.ibm.ws.javaee.dd.commonext.ResourceRef> resourceRefs=ext.getResourceRefs();
    if (resourceRefs!=null && resourceRefs.size()>0) {
        HashMap resourceRefsHashMap = convertAttributeListToMap(resourceRefs);
        //TODO: nothing to set on the webcontainer config here
        //this.set(resourceRefsHashMap);
    }
    List<ServletCacheConfig> servletCacheConfigs=ext.getServletCacheConfigs();
    if (fileServingAttributes!=null && fileServingAttributes.size()>0) {
        HashMap servletCacheConfigsHashMap = convertAttributeListToMap(servletCacheConfigs);
        //TODO: nothing to set on the webcontainer config here
        this.setS(servletCacheConfigsHashMap);
    }
    List<ServletExtension> servletExtensions=ext.getServletExtensions();
    if (fileServingAttributes!=null && fileServingAttributes.size()>0) {
        HashMap servletExtensionsHashMap = convertAttributeListToMap(servletExtensions);
        //TODO: nothing to set on the webcontainer config here
        this.set(servletExtensionsHashMap);
    }*/
    }
    private HashMap convertAttributeListToMap(List<Attribute> attributeList) {
        HashMap hm = new HashMap();
        for(Attribute a: attributeList) {
            String name = a.getName();
            String value = a.getValue();
            if (name != null && name.equals("extendedDocumentRoot")) {
                try{ //PK93292: ALLOW THE USE OF WEBSPHERE VARIABLES IN EXTENDEDDOCUMENTROOT JSP ATTRIBUTE.
                    WsLocationAdmin locationService = WebContainer.getLocationService();
                    value = locationService.resolveString(value);
                }catch (Exception e){
                    // TODO: This should probably be a warning, with an nls. 
                    logger.logp(Level.FINE, CLASS_NAME, "convertAttributeListToMap", "varaible expansion failed for " + name, e);
                }
            }

            hm.put(name, value);            
        }
        return hm;
    }

    private HashMap convertMimeFiltersToMap(List<MimeFilter> mimeFilters) {
        HashMap hm = new HashMap();
        for (MimeFilter mf:mimeFilters) {
            com.ibm.ws.container.MimeFilter wmf = new com.ibm.ws.container.MimeFilter(mf.getMimeType(), mf.getTarget());
            hm.put(mf.getMimeType(), wmf);
        }
        return hm;
    }

    public boolean isErrorPagePresent() {
        int codeErrorPageLen = (null != this.getCodeErrorPages()) ? this.getCodeErrorPages().size() : 0;
        int exceptionErrorPageLen = (null != this.getExceptionErrorPages()) ? this.getExceptionErrorPages().size() : 0;

        return codeErrorPageLen > 0 || exceptionErrorPageLen > 0;
    }

    //PM84305 - start
    public void setDisableStaticMappingCache(){
        if (this.contextParams != null){
            String value = (String) this.contextParams.get("com.ibm.ws.webcontainer.DISABLE_STATIC_MAPPING_CACHE");
            if (value != null ){
                if (value.equalsIgnoreCase("true")){
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                        logger.logp(Level.FINE, CLASS_NAME,"setDisableStaticMappingCache", "cxtParam disable static mapping cache for application -> "+ applicationName);
                    this.setDisableStaticMappingCache(true);
                }
                return;
            }
        }
        if (WCCustomProperties.DISABLE_STATIC_MAPPING_CACHE != null){
            String disableStaticMappingCacheApp = WCCustomProperties.DISABLE_STATIC_MAPPING_CACHE;

            if (disableStaticMappingCacheApp.equals("*")){
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                    logger.logp(Level.FINE, CLASS_NAME,"setDisableStaticMappingCache", "disable static mapping cache for all apps.");
                this.setDisableStaticMappingCache(true);
                return;
            }
            else{
                String [] parsedStr = disableStaticMappingCacheApp.split(",");
                for (String toCheckStr:parsedStr){
                    toCheckStr = toCheckStr.trim();
                    if (applicationName != null && applicationName.equalsIgnoreCase(toCheckStr)){
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                            logger.logp(Level.FINE, CLASS_NAME,"setDisableStaticMappingCache", "disable static mapping cache for application -> "+ applicationName);
                        this.setDisableStaticMappingCache(true);
                        return;
                    }
                }
            }
        }
    }

    public void setDisableStaticMappingCache(boolean b){  
        this.disableStaticMappingCache = b;
    }

    public boolean isDisableStaticMappingCache(){
        return this.disableStaticMappingCache;
    }
    //PM84305 - end

    public void setJspCachedLocations(Map jspCachedLocations) {
        this.jspCachedLocations = jspCachedLocations;
    }

    public Map getJspCachedLocations() {
        return jspCachedLocations;
    }

    /**
     * PI67099
     */
    public void setSTSHeaderValue() {
        String STSHeader  = null;
        if (this.contextParams != null){
            STSHeader = (String) this.contextParams.get("com.ibm.ws.webcontainer.ADD_STS_HEADER_WEBAPP");
            if (STSHeader != null ){
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                    logger.logp(Level.FINE, CLASS_NAME,"setSTSHeaderValue", "cxtParam provided for STS_HEADER in web application -> "+ applicationName);                          
                this.primedSTSHeader = this.generateSTSHeader(STSHeader);
            }         
        }       
        //if still null check server level property
        if(STSHeader == null) {
            STSHeader = WCCustomProperties.ADD_STRICT_TRANSPORT_SECURITY_HEADER;         
            this.primedSTSHeader = this.generateSTSHeader(STSHeader);
        }
        if(STSHeader == null || this.primedSTSHeader == null ) {
            // this make sure we have value set , we have gone through this once for this object           
            this.primedSTSHeader = "NoValue";                    
        }       
    }

    // called from SRTServletResponse
    /**
     * @return
     */
    public String getSTSHeaderValue() {        
        return this.primedSTSHeader;                       
    }

    /**
     * @param value
     * @return
     */
    private String generateSTSHeader(String value){

        /*      
        Possible Values: 
        Strict-Transport-Security:"max-age=31536000";
        Strict-Transport-Security:"max-age=31536000; includeSubDomains";
        Strict-Transport-Security:"max-age=31536000; includeSubDomains; preload";

        The overall requirements for directives are:
            1.  The order of appearance of directives is not significant.

            2.  All directives MUST appear only once in an STS header field.

            3.  Directive names are case-insensitive.

            4.  UAs MUST ignore any STS header field containing directives, or
            other header field value data, that does not conform to the
            syntax defined in this specification.

            5.  If an STS header field contains directive(s) not recognized by
            the UA, the UA MUST ignore the unrecognized directives, and if
            the STS header field otherwise satisfies the above requirements
            (1 through 4), the UA MUST process the recognized directives.

             The max-age directive value can optionally be quoted:
             Strict-Transport-Security: max-age="31536000"
         */
        if (value != null){
            String[] tokens = value.split(";");
            int size = tokens.length;
            int maxAgeToken = -1;
            int number = -1;
            boolean quotes = false;
            // max-age is mandatory and the value of max-age must be number
            try { 
                for(int i=0 ; i<size ; i++){
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
                        logger.logp(Level.FINE, CLASS_NAME,"generateSTSHeader", "size -->"+ size + " token ["+i+"] -->" + tokens[i]);
                    }
                    if( tokens[i].toLowerCase().contains("max-age")){
                        maxAgeToken= i;                   
                        String[] tok = tokens[i].split("=");
                        if(tok.length == 2) {
                            tok[1] = tok[1].trim();
                            if((tok[1].startsWith("\"")) && (tok[1].endsWith("\""))){
                                quotes = true;
                                tok[1] = tok[1].replace("\"", "");
                            }
                            number = Integer.parseInt(tok[1].trim());
                            if( number < 0){
                                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
                                    logger.logp(Level.FINE, CLASS_NAME,"generateSTSHeader", "max-age value must be 0 or greater " + number);
                                }
                                return null;
                            }
                        }
                        else{
                            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
                                logger.logp(Level.FINE, CLASS_NAME,"generateSTSHeader", "max-age value must be provided");
                            }
                            return null;
                        }

                        break; // since it can be only once, we will pick the first instance
                    }                
                }
            } catch(NumberFormatException e) {
                //Error Message ?
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
                    logger.logp(Level.FINE, CLASS_NAME,"generateSTSHeader", "max-age value must be number");
                }
                return null;
            }

            String generatedHeaderValue = null;
            if(maxAgeToken >= 0){
                // Add token to the header value
                if(quotes){
                    generatedHeaderValue = "max-age="+"\""+number+"\"";
                }
                else
                    generatedHeaderValue = "max-age="+number;
            }
            else{
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
                    logger.logp(Level.FINE, CLASS_NAME,"generateSTSHeader", "max-age is mandatory token");
                }
                return null;
            }

            if(size > 1) {
                int includeSubDomainsToken =-1;
                int preloadToken = -1;
                // this is optional
                for(int i=0 ; i<size ; i++){
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
                        logger.logp(Level.FINE, CLASS_NAME,"generateSTSHeader", "includeSubDomains , size -->"+ size + " token ["+i+"] -->" + tokens[i]);
                    }
                    if( tokens[i].toLowerCase().contains("includesubdomains")){
                        includeSubDomainsToken = i;  
                        break;
                    }                               
                }

                if(includeSubDomainsToken >= 0){
                    generatedHeaderValue = generatedHeaderValue +"; "+ "includeSubDomains" ;
                }

                // this is optional
                for(int i=0 ; i<size ; i++){
                    if( tokens[i].toLowerCase().contains("preload")){
                        preloadToken = i;    
                        break;
                    }                               
                }               
                if (preloadToken >= 0){
                    generatedHeaderValue = generatedHeaderValue +"; "+ "preload" ;
                }
            }
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
                logger.logp(Level.FINE, CLASS_NAME,"generateSTSHeader",
                            " property -->"+ value + " , response value-->" + generatedHeaderValue);
            }
            return generatedHeaderValue;
        }
        return null;
    }

    public void setEncodeDispatchedRequestURI() {
        if (this.contextParams != null){
            String value = (String) this.contextParams.get("com.ibm.ws.webcontainer.ENCODE_DISPATCHED_REQUEST_URI");
            if (value != null){
                if(value.equalsIgnoreCase("true")){
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                        logger.logp(Level.FINE, CLASS_NAME,"setEncodeDispatchedRequestURI", "Encode dispatched request URI for application -> "+ applicationName);
                    this.encodeDispatchedRequestURI = true;
                    return;
                }
                else if(value.equalsIgnoreCase("false")){
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                        logger.logp(Level.FINE, CLASS_NAME,"setEncodeDispatchedRequestURI", "Do not encode dispatched request URI for application -> "+ applicationName);
                    this.encodeDispatchedRequestURI = false;
                    return;
                }
                else{
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                        logger.logp(Level.FINE, CLASS_NAME,"setEncodeDispatchedRequestURI", "Invalid value set for context param [com.ibm.ws.webcontainer.ENCODE_DISPATCHED_REQUEST_URI] in application -> "+ applicationName);
                }
            }
        }
        if (WCCustomProperties.ENCODE_DISPATCHED_REQUEST_URI){
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                logger.logp(Level.FINE, CLASS_NAME,"setEncodeDispatchedRequestURI", "Encode dispatched request URI per custom property for application -> "+ applicationName);
            this.encodeDispatchedRequestURI = true;
        }
    }

    public boolean isEncodeDispatchedRequestURI(){
        return this.encodeDispatchedRequestURI;
    }

    //MultiRead Start
    /**
     * @return
     */
    public boolean isEnablemultireadofpostdata() {
        return this.enablemultireadofpostdata;
    }
    
    public void setMultiReadOfPostDataValue() {
        String multiRead  = null;
        if (this.contextParams != null){
            multiRead = (String) this.contextParams.get("com.ibm.ws.webcontainer.SET_MULTI_READ_WEBAPP");
            if (multiRead != null){
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                    logger.logp(Level.FINE, CLASS_NAME,"setMultiReadOfPostDataValue", "cxtParam provided for MULTI_READ: "+multiRead +" in web application -> "+ applicationName);                                              

                if(multiRead.equalsIgnoreCase("true")){
                    this.setEnablemultireadofpostdata(true);
                }
                else if(multiRead.equalsIgnoreCase("false")){
                    this.setEnablemultireadofpostdata(false);
                }
                return;
            }
        }
        //if null check server level property
        if(multiRead == null) {
            this.setEnablemultireadofpostdata(WCCustomProperties.ENABLE_MULTI_READ_OF_POST_DATA);
        }
    }
    
    /**
     * @param enablemultireadofpostdata the enablemultireadofpostdata to set
     */
    private void setEnablemultireadofpostdata(boolean enablemultireadofpostdata) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME,"setEnablemultireadofpostdata", "set enablemultireadofpostdata: "+enablemultireadofpostdata);                                              

        this.enablemultireadofpostdata = enablemultireadofpostdata;
    }
    //MultiRead End

    public String getModuleRequestEncoding(){
        return this.requestEncoding;
    }

    public void setModuleRequestEncoding(String encoding) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "setModuleRequestEncoding", " request encoding [" + encoding +"]");
        }
        this.requestEncoding = encoding;
    }
    
    public String getModuleResponseEncoding(){
        return this.responseEncoding;
    }
    
    public void setModuleResponseEncoding(String encoding) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "setModuleResponseEncoding", " response encoding [" + encoding +"]");
        }
        this.responseEncoding = encoding;
    }
}
