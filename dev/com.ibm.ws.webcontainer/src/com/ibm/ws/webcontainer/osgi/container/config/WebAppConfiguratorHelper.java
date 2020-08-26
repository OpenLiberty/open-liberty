/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
//  CHANGE HISTORY
//      Defect          Date            Modified By             Description
//--------------------------------------------------------------------------------------
//      PM84305         07/10/13        pmdinh                  Prevent OOM which may occur due to application design
//      PI05845         08/21/14        sartoris                tWAS PM94199 Conditionally enabled default error pages in Servlet 3.0
//      131004          09/29/14        bitonti                 Unconditionally enable default error pages in Servlet 3.1
//      148426          10/06/14        bitonti                 Give extension default error page precedence in Servlet 3.0
//      PI67942         10/21/16        zaroman                 encode URI after dispatch

package com.ibm.ws.webcontainer.osgi.container.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.security.RunAs;
import javax.servlet.DispatcherType;
import javax.servlet.MultipartConfigElement;
import javax.servlet.SessionTrackingMode;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.ErrorPage;
import com.ibm.ws.container.service.annotations.WebAnnotations;
import com.ibm.ws.container.service.annotations.FragmentAnnotations;
import com.ibm.ws.container.service.annotations.SpecificAnnotations;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.container.service.config.ServletConfigurator;
import com.ibm.ws.container.service.config.ServletConfigurator.ConfigItem;
import com.ibm.ws.container.service.config.ServletConfigurator.ConfigSource;
import com.ibm.ws.container.service.config.ServletConfigurator.MergeComparator;
import com.ibm.ws.container.service.config.ServletConfiguratorHelper;
import com.ibm.ws.container.service.config.WebFragmentInfo;
import com.ibm.ws.container.service.config.extended.RefBndAndExtHelper;
import com.ibm.ws.injectionengine.osgi.util.OSGiJNDIEnvironmentRefBindingHelper;
import com.ibm.ws.javaee.dd.DeploymentDescriptor;
import com.ibm.ws.javaee.dd.common.AdministeredObject;
import com.ibm.ws.javaee.dd.common.ConnectionFactory;
import com.ibm.ws.javaee.dd.common.DataSource;
import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.DescriptionGroup;
import com.ibm.ws.javaee.dd.common.DisplayName;
import com.ibm.ws.javaee.dd.common.EJBRef;
import com.ibm.ws.javaee.dd.common.EnvEntry;
import com.ibm.ws.javaee.dd.common.Icon;
import com.ibm.ws.javaee.dd.common.JMSConnectionFactory;
import com.ibm.ws.javaee.dd.common.JMSDestination;
import com.ibm.ws.javaee.dd.common.JNDIEnvironmentRef;
import com.ibm.ws.javaee.dd.common.Listener;
import com.ibm.ws.javaee.dd.common.MailSession;
import com.ibm.ws.javaee.dd.common.MessageDestinationRef;
import com.ibm.ws.javaee.dd.common.ParamValue;
import com.ibm.ws.javaee.dd.common.PersistenceContextRef;
import com.ibm.ws.javaee.dd.common.PersistenceUnitRef;
import com.ibm.ws.javaee.dd.common.ResourceBaseGroup;
import com.ibm.ws.javaee.dd.common.ResourceEnvRef;
import com.ibm.ws.javaee.dd.common.ResourceRef;
import com.ibm.ws.javaee.dd.common.wsclient.ServiceRef;
import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.dd.web.WebFragment;
import com.ibm.ws.javaee.dd.web.common.CookieConfig;
import com.ibm.ws.javaee.dd.web.common.Filter;
import com.ibm.ws.javaee.dd.web.common.FilterMapping;
import com.ibm.ws.javaee.dd.web.common.LocaleEncodingMapping;
import com.ibm.ws.javaee.dd.web.common.LocaleEncodingMappingList;
import com.ibm.ws.javaee.dd.web.common.MimeMapping;
import com.ibm.ws.javaee.dd.web.common.MultipartConfig;
import com.ibm.ws.javaee.dd.web.common.Servlet;
import com.ibm.ws.javaee.dd.web.common.ServletMapping;
import com.ibm.ws.javaee.dd.web.common.SessionConfig;
import com.ibm.ws.javaee.dd.web.common.WelcomeFileList;
import com.ibm.ws.javaee.dd.webbnd.VirtualHost;
import com.ibm.ws.javaee.dd.webbnd.WebBnd;
import com.ibm.ws.javaee.dd.webext.WebExt;
import com.ibm.ws.resource.ResourceRefConfigFactory;
import com.ibm.ws.resource.ResourceRefConfigList;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.session.SessionCookieConfigImpl;
import com.ibm.ws.webcontainer.filter.FilterConfig;
import com.ibm.ws.webcontainer.metadata.EJBRefImpl;
import com.ibm.ws.webcontainer.metadata.EnvEntryImpl;
import com.ibm.ws.webcontainer.metadata.InjectionTargetsEditable;
import com.ibm.ws.webcontainer.metadata.MessageDestinationRefImpl;
import com.ibm.ws.webcontainer.metadata.PersistenceContextRefImpl;
import com.ibm.ws.webcontainer.metadata.PersistenceUnitRefImpl;
import com.ibm.ws.webcontainer.metadata.ResourceEnvRefImpl;
import com.ibm.ws.webcontainer.metadata.ResourceRefImpl;
import com.ibm.ws.webcontainer.metadata.ServiceRefImpl;
import com.ibm.ws.webcontainer.osgi.container.config.merge.AdministeredObjectComparator;
import com.ibm.ws.webcontainer.osgi.container.config.merge.ConnectionFactoryComparator;
import com.ibm.ws.webcontainer.osgi.container.config.merge.DataSourceComparator;
import com.ibm.ws.webcontainer.osgi.container.config.merge.EJBRefComparator;
import com.ibm.ws.webcontainer.osgi.container.config.merge.EnvEntryComparator;
import com.ibm.ws.webcontainer.osgi.container.config.merge.JMSConnectionFactoryComparator;
import com.ibm.ws.webcontainer.osgi.container.config.merge.JMSDestinationComparator;
import com.ibm.ws.webcontainer.osgi.container.config.merge.MailSessionComparator;
import com.ibm.ws.webcontainer.osgi.container.config.merge.MessageDestinationRefComparator;
import com.ibm.ws.webcontainer.osgi.container.config.merge.PersistenceContextRefComparator;
import com.ibm.ws.webcontainer.osgi.container.config.merge.PersistenceUnitRefComparator;
import com.ibm.ws.webcontainer.osgi.container.config.merge.ResourceEnvRefComparator;
import com.ibm.ws.webcontainer.osgi.container.config.merge.ResourceRefComparator;
import com.ibm.ws.webcontainer.osgi.container.config.merge.ServiceRefComparator;
import com.ibm.ws.webcontainer.osgi.metadata.WebComponentMetaDataImpl;
import com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer.osgi.webapp.WebAppConfiguration;
import com.ibm.ws.webcontainer.servlet.ServletConfig;
import com.ibm.ws.webcontainer.servlet.TargetConfig;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.anno.info.AnnotationInfo;
import com.ibm.wsspi.anno.info.AnnotationValue;
import com.ibm.wsspi.anno.info.ClassInfo;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets;
import com.ibm.wsspi.injectionengine.JNDIEnvironmentRefType;
import com.ibm.wsspi.webcontainer.WCCustomProperties;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

/**
 * WebAppConfigurator processes all the required web application related configurations
 * from web.xml, web-fragment.xml and annotations.  Configure them into the WebAppConfiguration.
 */
public class WebAppConfiguratorHelper implements ServletConfiguratorHelper {
    private static final String CLASS_NAME = WebAppConfiguratorHelper.class.getSimpleName();

    private static final TraceComponent tc = Tr.register(WebAppConfiguratorHelper.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

    private static final TraceNLS nls = TraceNLS.getTraceNLS(WebAppConfiguratorHelper.class, "com.ibm.ws.webcontainer.resources.LShimMessages");

    // private static List<Class<?>> SUPPORTED_LISTENER_INTERFACE_NAMES = new ArrayList<Class<?>>();

    private static final String CDI_CONVERSATION_FILTER = "CDI Conversation Filter";

    private static final EnvEntryComparator ENV_ENTRY_COMPARATOR = new EnvEntryComparator();

    private static final ServiceRefComparator SERVICE_REF_COMPARATOR = new ServiceRefComparator();
    
    private static final ResourceRefComparator RESOURCE_REF_COMPARATOR = new ResourceRefComparator();

    private static final ResourceEnvRefComparator RESOURCE_ENV_REF_COMPARATOR = new ResourceEnvRefComparator();

    private static final MessageDestinationRefComparator MESSAGE_DESTINATION_REF_COMPARATOR = new MessageDestinationRefComparator();
    
    private static final PersistenceUnitRefComparator PERSISTENCE_UNIT_REF_COMPARATOR = new PersistenceUnitRefComparator();

    private static final PersistenceContextRefComparator PERSISTENCE_CONTEXT_REF_COMPARATOR = new PersistenceContextRefComparator();

    private static final DataSourceComparator DATA_SOURCE_COMPARATOR = new DataSourceComparator();

    private static final MailSessionComparator MAIL_SESSION_COMPARATOR = new MailSessionComparator();

    private static final EJBRefComparator EJB_REF_COMPARATOR = new EJBRefComparator();
    
    private static final ConnectionFactoryComparator CF_COMPARATOR = new ConnectionFactoryComparator();
    
    private static final AdministeredObjectComparator ADMINISTERED_OBJECT_COMPARATOR = new AdministeredObjectComparator();
    
    private static final JMSConnectionFactoryComparator JMS_CF_COMPARATOR = new JMSConnectionFactoryComparator();
    
    private static final JMSDestinationComparator JMS_DESTINATION_COMPARATOR = new JMSDestinationComparator();

    private final ServletConfigurator configurator;

    private final List<Class<?>> listenerInterfaces;
    
    // PI05845 start
    
    /**
     * Local synonym for the "com.ibm.ws.webcontainer.allowdefaulterrorpage" WC property.
     * When true, default error pages are allowed with Servlet 3.0.  When false, default
     * error pages are allowed only with Servlet 3.1.  Since Servlet 3.0 is the lowest
     * supported level, allowing default error pages in Servlet 3.0 means always
     * supporting default error pages.
     */
    private static boolean allowDefaultErrorPageInServlet30 = WCCustomProperties.ALLOW_DEFAULT_ERROR_PAGE;
    
    private static boolean deferProcessingIncompleteFiltersInWebXML = WCCustomProperties.DEFER_PROCESSING_INCOMPLETE_FILTERS_IN_WEB_XML;
    
    public static boolean allowDefaultErrorPageInServlet30() {
        return allowDefaultErrorPageInServlet30;
    }
    
    // PI05845 end
    
    /**
     * Convert a version ID string value to an integer value.  The integer value
     * is ten (10) times the string value converted to a numeric value.
     * 
     * @param version The version as a string value.
     * 
     * @return The version as an integer value.
     * 
     * @throws IllegalStateException Thrown if the version string value is not a
     *     valid servlet specification level.  That is, "2.2", "2.3", "2.4", "2.5",
     *     "3.0", or "3.1".  "3.1" is accepted even if the supported specification
     *     level is 3.0.
     */
    public static int getVersionId(String version) throws IllegalStateException {
        int versionID = 0;
        if ("5.0".equals(version)) {
            versionID = 50;
        }else if ("4.0".equals(version)) {
            versionID = 40;
        }else if ("3.1".equals(version)) {
            versionID = 31;
        } else if ("3.0".equals(version)) {
            versionID = 30;
        } else if ("2.5".equals(version)) {
            versionID = 25;
        } else if ("2.4".equals(version)) {
            versionID = 24;
        } else if ("2.3".equals(version)) {
            versionID = 23;
        } else if ("2.2".equals(version)) {
            versionID = 22;
        } else {
            throw new IllegalStateException("invalid web-app version");
        }
        return versionID;
    }
    
    /**
     * Answer the Servlet specification level.  This is obtained from
     * the web container.  See {@link import com.ibm.ws.webcontainer.WebContainer#getServletContainerSpecLevel()}.
     * 
     * @return The Servlet specification level.
     */
    public static int getServletSpecLevel() {
        // Always obtain this dynamically.  A cached value does make sense,
        // but there have been problems obtaining the value too early.

        int containerSpecLevel = com.ibm.ws.webcontainer.osgi.WebContainer.getServletContainerSpecLevel();

        // if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
        //     Tr.debug(tc, "Servlet container level {0}", Integer.valueOf(containerSpecLevel));
        // }

        return containerSpecLevel;
    }

    /**
     * Tell if the supported servlet specification level is 3.1 or higher.
     * 
     * @return True if the supported servlet specification level is 3.1 or higher.
     *     Otherwise, false.
     */
    public static boolean isServletSpecLevel31OrHigher() {
        return ( getServletSpecLevel() >= com.ibm.ws.webcontainer.osgi.WebContainer.SPEC_LEVEL_31 );
    }    

    /**
     * Answer the default version level.  Used to assign the version ID to a
     * web configuration which has no associated descriptor.
     * 
     * The current implementation always answers the 3.0 version.  Maybe,
     * the value should depend on the feature enablement.
     * 
     * See {@link #isServletSpecLevel31OrHigher()}.
     * 
     * @return The default version level.
     */
    public static int getDefaultVersionId() {
        return com.ibm.ws.webcontainer.osgi.WebContainer.SPEC_LEVEL_30;
        
        // Should this be conditional on the servlet specification level??
        //
        // return (WebAppConfiguratorHelper.isServletSpecLevel31OrHigher()
        //     ? com.ibm.ws.webcontainer.osgi.WebContainer.SPEC_LEVEL_31
        //     : com.ibm.ws.webcontainer.osgi.WebContainer.SPEC_LEVEL_30);
    }
    
    protected static final class ConfigurationWriter {
        private final WebAppConfiguration config;
        private final ResourceRefConfigFactory resourceRefConfigFactory;

        public ConfigurationWriter(WebAppConfiguration config, ResourceRefConfigFactory resourceRefConfigFactory) {
            this.config = config;
            this.resourceRefConfigFactory = resourceRefConfigFactory;
        }

        public void setContextRoot(String contextRoot) {
            config.setContextRoot(contextRoot);
        }

        public void setDistributable(boolean distributable) {
            this.config.setDistributable(distributable);
        }
        
        public boolean isDistributable() {
            return this.config.isDistributable();
        }

        public void setModuleName(String moduleName) {
            config.setModuleName(moduleName);
        }

        public void setJ2EEModuleName(String j2eeModuleName) {
            config.setJ2EEModuleName(j2eeModuleName);
        }

        public void setApplicationName(String appName) {
            config.setApplicationName(appName);
        }

        public void setBundleHeaders(Dictionary<String, String> bundleHeaders) {
            config.setBundleHeaders(bundleHeaders);
        }

        public void setOrderedLibPaths(ArrayList<String> orderedLibPaths) {
            config.setOrderedLibPaths(orderedLibPaths);
        }

        public void addRef(JNDIEnvironmentRefType refType, JNDIEnvironmentRef ref) {
            refType.addRef(config.getAllRefs(), ref);
        }

        public void setVersion(int servletVersion) {
            config.setVersion(servletVersion);
        }

        public void setMetadataComplete(boolean metadataComplete) {
            config.setMetadataComplete(metadataComplete);
        }

        public void setDescription(String description) {
            config.setDescription(description);
        }

        public void setDisplayName(String displayName) {
            config.setDisplayName(displayName);
        }

        public void addListener(String listenerClass) {
            config.addListener(listenerClass);
            Set<String> setOfWebXmlDefinedListeners = config.getWebXmlDefinedListeners();
            setOfWebXmlDefinedListeners.add(listenerClass);
        }

        public void addLocaleEncodingMap(String locale, String encoding) {
            config.addLocaleEncodingMap(locale, encoding);
        }

        public void setSessionTimeout(int sessionTimeout) {
            config.setSessionTimeout(sessionTimeout);
            config.setModuleSessionTimeoutSet(true);
        }

        public void addServletMapping(String servletName, String urlPattern) {
            config.addServletMapping(servletName, urlPattern);
        }

        public Map<JNDIEnvironmentRefType, Map<String, String>> getAllRefBindings() {
            return config.getAllRefBindings();
        }
        
        public Map<String,String> getEnvEntryValues() {
            return config.getEnvEntryValues();
        }

        public void addFilterInfo(FilterConfig filterConfig) {
            config.addFilterInfo(filterConfig);
        }

        public void addServletInfo(String servletName, ServletConfig servletConfig) {
            config.addServletInfo(servletName, servletConfig);
        }

        public ResourceRefConfigList getResourceRefConfigList() {
            return config.getResourceRefConfigList(resourceRefConfigFactory);
        }
        
        public ServletConfig createServletConfig(String servletId, String servletName) {
            ServletConfig servletConfig = new ServletConfig(servletId, config);
            servletConfig.setMetaData(new WebComponentMetaDataImpl(config.getMetaData()));
            servletConfig.setServletName(servletName);
            return servletConfig;
        }

        public FilterConfig createFilterConfig(String filterId, String filterName) {
            FilterConfig filterConfig = new FilterConfig(filterId, config);
            filterConfig.setName(filterName);
            return filterConfig;
        }

        public void addContextParam(String contextParamName, String contextParamValue) {
            @SuppressWarnings("unchecked")
            Map<String, String> webAppContextParamMap = config.getContextParams();
            webAppContextParamMap.put(contextParamName, contextParamValue);
        }

        public void addDefaultWelcomeFiles() {
            List<String> welcomeFiles = config.getWelcomeFileList();
            if (welcomeFiles.isEmpty()) {
                welcomeFiles.addAll(Arrays.asList("index.html", "index.htm", "index.jsp"));
            }
        }

        public void setDefaultDisplayName() {
            //Ensure we always have a default display name
            if (config.getDisplayName() == null) {
                config.setDisplayName(config.getModuleName());
            }
        }

        public void addWelcomeFile(String welcomeFile) {
            List<String> welcomeFiles = config.getWelcomeFileList();
            welcomeFiles.add(welcomeFile);
        }

        public void addSessionTrackingMode(SessionTrackingMode mode) {
            EnumSet<SessionTrackingMode> stm = config.getInternalDefaultSessionTrackingMode();
            if (stm == null) {
                stm = EnumSet.noneOf(SessionTrackingMode.class);
                config.setDefaultSessionTrackingMode(stm);
                config.setModuleSessionTrackingModeSet(true);
            }
            stm.add(mode);
        }

        public void addCodeErrorPage(int errorCode, ErrorPage configErrorPage) {
            @SuppressWarnings("unchecked")
            Map<Integer, ErrorPage> webAppCodeErrorPages = config.getCodeErrorPages();
            webAppCodeErrorPages.put(Integer.valueOf(errorCode), configErrorPage);
        }

        public void addExceptionErrorPage(String exceptionType, ErrorPage configErrorPage) {
            @SuppressWarnings("unchecked")
            Map<String, ErrorPage> webAppExceptionErrorPages = config.getExceptionErrorPages();
            webAppExceptionErrorPages.put(exceptionType, configErrorPage);
        }

        // PI05845 start

        public void setDefaultErrorPage(ErrorPage newDefaultErrorPage) {
            // Assign the default error page unconditionally in Servlet 3.1,
            // but for backwards compatibility, do not override an extension
            // default error page in Servlet 3.0.
            //
            // See also 'com.ibm.ws.webcontainer.webapp.
            // WebAppConfiguration.setInitializeWebExtProps(WebExt)'.

            String currentLocation = config.getDefaultErrorPage();
            String newLocation = newDefaultErrorPage.getLocation();

            if (newLocation.equals(currentLocation)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Default error page {0} same as prior default error page; ignoring.", newLocation);
                }
                return; // Simply ignore it if it is the same.
            }

            if ((currentLocation != null) && !currentLocation.isEmpty()) {
                if (isServletSpecLevel31OrHigher()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Descriptor default error page {0} overwrites extension default error page {1}.", newLocation, currentLocation);
                    }
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Descriptor default error page {0} masked by extension default error page {1}.", newLocation, currentLocation);
                    }                    
                    return;
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Assigning descriptor default error page {0}.", newLocation);
                }                
            }

            config.setDefaultErrorPage(newLocation);
        }

        // PI05845 end

        public SessionCookieConfigImpl getSessionCookieConfig() {
            SessionCookieConfigImpl sessionCookieConfigImpl = config.getSessionCookieConfig();
            if (sessionCookieConfigImpl == null) {
                sessionCookieConfigImpl = new SessionCookieConfigImpl();
                config.setSessionCookieConfig(sessionCookieConfigImpl);
            }
            return sessionCookieConfigImpl;
        }

        public void cacheResults(ServletConfigurator configurator) {
            configurator.addToModuleCache(WebAppConfig.class, config);
            configurator.addToModuleCache(WebModuleMetaData.class, config.getMetaData());
        }

        public String getDisplayName() {
            String displayName = config.getDisplayName();
            if (displayName != null) {
                return displayName;
            }
            return config.getModuleName();
        }

        public void addMimeMapping(String extension, String mimeType) {
            @SuppressWarnings("unchecked")
            Map<String, String> webAppMimeMappings = config.getMimeMappings();
            webAppMimeMappings.put(extension, mimeType);
            config.getServletInfos();
        }
        
        //PM84305
        //method to set any property which can be specified by a custom property and/or application's context param
        public void setGlobalAndPrivateConfig(){
            config.setDisableStaticMappingCache();
            config.setSTSHeaderValue(); //PI67099
            config.setEncodeDispatchedRequestURI(); //PI67942
            config.setMultiReadOfPostDataValue(); //MultiRead
        }
        
        public void setVirtualHostName(VirtualHost virtualHost) {
            config.setVirtualHostName(virtualHost.getName());
        }

        /**
         * @param denyUncoveredHttpMethods
         */
        public void setDenyUncoveredHttpMethods(boolean denyUncoveredHttpMethods) {
            this.config.setDenyUncoveredHttpMethods(denyUncoveredHttpMethods);
            
        }
        
        public void setDefaultContextRootUsed(boolean isDefaultContextRootUsed) {
            config.setDefaultContextRootUsed(isDefaultContextRootUsed);
        }
        
        public void setDefaultContextPath(String defaultContextPath) {
            String methodName = "setDefaultContextPath";
            boolean isDefaultContextRootUsed = config.isDefaultContextRootUsed();
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + ": defaultContextPath --> " + defaultContextPath + "; isDefaultContextRootUsed --> " + isDefaultContextRootUsed);
            }
            
            /** If the default context root is being used, then use the default-context-path element instead */
            if(isDefaultContextRootUsed) {
                config.setContextRoot(defaultContextPath);
            }
        }
        
        public void setRequestEncoding(String encoding){
            config.setModuleRequestEncoding(encoding);
        }
        
        public void setResponseEncoding(String encoding) {
            config.setModuleResponseEncoding(encoding);
        }
    }
    
    private static final class MappingIndexPair<T, N> {
        private T t;
        private N n;
        
        public MappingIndexPair(T t, N n){
            this.t = t;
            this.n = n;
        }
        
        public T getMapping(){
            return this.t;
        }
        
        public N getIndex(){
            return this.n;
        }
    }

    protected final ConfigurationWriter webAppConfiguration;
    
    protected interface DeferredAction {
        boolean isAllServletAction();

        void doAction(ServletConfig servletConfig) throws UnableToAdaptException;
    }

    protected List<DeferredAction> allActions = new ArrayList<DeferredAction>();

    private Set<String> annotationScanningRequiredClasses = new HashSet<String>();

    private Map<String, String> urlToServletNameMap; // Servlet 3.1

    // Counter used to generated unique IDs
    private static final AtomicLong uniqueId;

    static {
        uniqueId = new AtomicLong(1l);
    }

    public static String generateId() {
        return "WebAppGeneratedId" + uniqueId.getAndIncrement();
    }

    ArrayList<MappingIndexPair<FilterMapping, Integer>> deferredFilterMappings = new ArrayList<MappingIndexPair<FilterMapping, Integer>>();
    int filterMappingIndex = 0;
    int filterMappingIndexOffset = 0;

    public WebAppConfiguratorHelper(ServletConfigurator configurator,
                                    ResourceRefConfigFactory resourceRefConfigFactory,
                                    List<Class<?>> listenerInterfaces) {
        this.configurator = configurator;
                
        WebModuleInfo moduleInfo = (WebModuleInfo) configurator.getFromModuleCache(WebModuleInfo.class);
        
        ApplicationMetaData amd = ((ExtendedApplicationInfo)moduleInfo.getApplicationInfo()).getMetaData();
        
        this.webAppConfiguration = new ConfigurationWriter(new WebAppConfiguration(amd, generateId()), resourceRefConfigFactory);
        
        this.webAppConfiguration.setContextRoot(moduleInfo.getContextRoot());
        this.webAppConfiguration.setModuleName(moduleInfo.getName());
        this.webAppConfiguration.setJ2EEModuleName(moduleInfo.getURI());
        this.webAppConfiguration.setApplicationName(moduleInfo.getApplicationInfo().getDeploymentName());
        
        this.listenerInterfaces = listenerInterfaces;
        
        if (isServletSpecLevel31OrHigher()) {
            this.urlToServletNameMap = new HashMap<String, String>();
        }        
    }

    private void removeFromRequiredClasses(Set<String> selectedClassNames, String reason) {
        String methodName = "removeFromRequiredClasses";

        String mcText;        
        if (!this.configurator.getMetadataCompleted()) {
            this.annotationScanningRequiredClasses.removeAll(selectedClassNames);
            mcText = ": Metadata-incomplete: Remove: ";
        } else {
            mcText = ": Metadata-Complete: Ignore: ";
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + ": " + reason);
            for (String selectedClassName : selectedClassNames) { 
                Tr.debug(tc, methodName + mcText + " [ " + selectedClassName + " ]");
            }
        }
    }

    private void addToRequiredClasses(String className, String reason) {
        String methodName = "addToRequiredClasses";
        
        if (!this.configurator.getMetadataCompleted()) {
            this.annotationScanningRequiredClasses.add(className);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + ": Metadata-incomplete: Add [ " + className + " ]: " + reason);
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + ": Metadata-complete: Ignore [ " + className + " ]: " + reason);
            }            
        }
    }

    private Object getFromModuleCache(String path, Class<?> owner) throws UnableToAdaptException {
        Entry entry = this.configurator.getModuleContainer().getEntry(path);
        if (entry != null) {
            NonPersistentCache entryCache = entry.adapt(NonPersistentCache.class);
            return entryCache.getFromCache(owner);
        } 
        
        return null;
    }

    @Override
    public void configureInit() throws UnableToAdaptException {
        String methodName = "configureInit";
        String displayName = webAppConfiguration.getDisplayName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, "WebAppConfiguration [ " + displayName + " ]");
        }
        
        configureWebAppVersion();

        configureBundleHeaders();

        // TODO: Previously, when metadata-complete was set, this would
        //       result in an empty list of locations.  With the update
        //       to generate the list even when metadata-complete is true,
        //       the list won't be empty.
        List<String> orderedLibPaths = new ArrayList<String>();
        for (WebFragmentInfo fragmentInfo : configurator.getWebAnnotations().getOrderedItems()) {
            String libraryURI = fragmentInfo.getLibraryURI();
            if (libraryURI.startsWith("WEB-INF/lib/")) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + ": Adding library fragment [ " + libraryURI + " ]"); 
                }                
                orderedLibPaths.add(libraryURI.substring("WEB-INF/lib/".length()));                                
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + ": Skipping non-library fragment [ " + libraryURI + " ]"); 
                }
            }
        }
        configureOrderedLibPaths(orderedLibPaths);
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, methodName, "WebAppConfiguration [ " + displayName + " ]");
        }
    }

    @SuppressWarnings("unchecked")
    private void configureBundleHeaders() throws UnableToAdaptException {
        // Extract the bundle headers (for WABs) and stash in the config object
        Object manifestHeaders = getFromModuleCache("/META-INF/MANIFEST.MF", Dictionary.class);
        if (manifestHeaders != null) {
            this.webAppConfiguration.setBundleHeaders((Dictionary<String, String>) manifestHeaders);
        }
    }

    @Override
    public void configureFromWebApp(WebApp webApp) throws UnableToAdaptException {
        configureDisplayName(webApp.getDisplayNames());
        configureDescription(webApp.getDescriptions());
        configureContextParam(webApp.getContextParams());
        configureErrorPages(webApp.getErrorPages());
        configureMimeMapping(webApp.getMimeMappings());
        configureWelcomeFileList(webApp.getWelcomeFileList());
        configureDistributableFromWebApp(webApp.isSetDistributable());

        if (isServletSpecLevel31OrHigher()) {
            configureDenyUncoveredHttpMethodsFromWebApp(webApp.isSetDenyUncoveredHttpMethods());        
        }
        
        configureSessionConfig(webApp.getSessionConfig());
        configureServlets(webApp, webApp.getServlets());
        configureServletMappings(webApp.getServletMappings());
        configureLocaleEncodingMap(webApp.getLocaleEncodingMappingList());
        configureListener(webApp.getListeners());
        configureEnvEntries(webApp.getEnvEntries());
        configureResourceRefs(webApp.getResourceRefs());
        configureResourceEnvRefs(webApp.getResourceEnvRefs());
        configureMessageDestinationRefs(webApp.getMessageDestinationRefs());
        configurePersistenceUnitRefs(webApp.getPersistenceUnitRefs());
        configurePersistenceContextRefs(webApp.getPersistenceContextRefs());
        configureDataSources(webApp.getDataSources());
        configureMailSessions(webApp.getMailSessions());
        configureServiceRefs(webApp.getServiceRefs());
        configureEJBRefs(webApp.getEJBRefs());
        configureEJBLocalRefs(webApp.getEJBLocalRefs());
        configureConnectionFactories(webApp.getConnectionFactories());
        configureJMSConnectionFactories(webApp.getJMSConnectionFactories());
        configureJMSDestinations(webApp.getJMSDestinations());
        
        configureAdministeredObjects(webApp.getAdministeredObjects());
        // filter & filter-mapping
        configureFilters(webApp, webApp.getFilters());
        configureFilterMappings(webApp, webApp.getFilterMappings());
    }

    @Override
    public void configureFromWebFragment(WebFragmentInfo webFragmentItem) throws UnableToAdaptException {
        WebFragment webFragment = webFragmentItem.getWebFragment();
        configureContextParam(webFragment.getContextParams());
        configureErrorPages(webFragment.getErrorPages());
        configureMimeMapping(webFragment.getMimeMappings());
        configureWelcomeFileList(webFragment.getWelcomeFileList());
        configureDistributableFromFragment(webFragment.isSetDistributable());
        configureSessionConfig(webFragment.getSessionConfig());
        configureServlets(webFragment, webFragment.getServlets());
        configureServletMappings(webFragment.getServletMappings());
        configureLocaleEncodingMap(webFragment.getLocaleEncodingMappingList());
        configureListener(webFragment.getListeners());
        configureEnvEntries(webFragment.getEnvEntries());
        configureResourceRefs(webFragment.getResourceRefs());
        configureResourceEnvRefs(webFragment.getResourceEnvRefs());
        configureMessageDestinationRefs(webFragment.getMessageDestinationRefs());
        configurePersistenceUnitRefs(webFragment.getPersistenceUnitRefs());
        configurePersistenceContextRefs(webFragment.getPersistenceContextRefs());
        configureDataSources(webFragment.getDataSources());
        configureServiceRefs(webFragment.getServiceRefs());
        configureEJBRefs(webFragment.getEJBRefs());
        configureEJBLocalRefs(webFragment.getEJBLocalRefs());

        // filter & filter-mapping
        configureFilters(webFragment, webFragment.getFilters());
        configureFilterMappings(webFragment, webFragment.getFilterMappings());
    }

    private void logAnnotations(String methodName, String title, WebFragmentInfo webFragmentItem, Set<String> annotationClassNames) {
        if ( annotationClassNames.isEmpty() ) {
            return;
        }

        if ( !TraceComponent.isAnyTracingEnabled() || !tc.isDebugEnabled()) {
            return;
        }

        String prefix = CLASS_NAME + "." + methodName + ": ";
        Tr.debug(tc, prefix + "[ " + webFragmentItem + " ]: " + title + ": [ " + annotationClassNames.size() + " ]");
        for ( String className : annotationClassNames ) {
            Tr.debug(tc, prefix + "  [ " + className + " ]");
        }
    }

    public void configureFromAnnotations(WebFragmentInfo webFragmentItem) throws UnableToAdaptException {    	
    	com.ibm.ws.container.service.annotations.FragmentAnnotations fragmentAnnotations =
    		configurator.getWebAnnotations().getFragmentAnnotations(webFragmentItem);

        Set<String> webServletClassNames = fragmentAnnotations.selectAnnotatedClasses(WebServlet.class);
        configureServletAnnotation(webServletClassNames);

        configureListenerAnnotation(fragmentAnnotations.selectAnnotatedClasses(WebListener.class));
        configureMultipartConfigAnnotation(fragmentAnnotations.selectAnnotatedClasses(javax.servlet.annotation.MultipartConfig.class));
        configureRunAsAnnotation(fragmentAnnotations.selectAnnotatedClasses(RunAs.class));

        Set<String> webFilterClassNames = fragmentAnnotations.selectAnnotatedClasses(WebFilter.class);
        configureFilterAnnotation(webFilterClassNames);
    }

    @Override
    public void configureDefaults() throws UnableToAdaptException {

        //I think we need to process specifiedClasses first to find what servlets were defined 
        //in case a filter is mapped to * (all servlets)
        configureSpecifiedClasses();

        for (DeferredAction action : allActions) {
            if (action.isAllServletAction()) {
                Map<String, ConfigItem<ServletConfig>> servletMap = configurator.getConfigItemMap("servlet");
                for (ConfigItem<ServletConfig> configItem : servletMap.values()) {
                    ServletConfig servletConfig = configItem.getValue();
                    action.doAction(servletConfig);
                }
            } else {
                action.doAction(null);
            }
        }

        //default configuration
        configureDefaultConfigurations();
    }

    protected void configureSpecificClass(String className) throws UnableToAdaptException {
        String methodName = "configureSpecificClass";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, "Specific class name [ " + className + " ]");
        }

        if (configurator.isMetadataComplete()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, methodName, "Ignore: Metadata-complete");
            }
            return;
        }

        if (!annotationScanningRequiredClasses.contains(className)) { 
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, methodName, "Ignore: Not an annotation scanning required class");
            }            
        }

        // Specific classes are Servlet, Filter, and Listener classes which are listed 
        // in a web.xml or web-fragment.xml.  These are processed for annotations except
        // when they are in an excluded region.
        //
        // Annotation source categories include seed (non-metadata-complete regions),
        // partial (metadata-complete regions), excluded (regions outside of an absolute-order),
        // and external (regions outside of the web module.
        //
        // A first implementation merged the excluded and external regions, then processed
        // specific classes which were in partial regions, the logic being that seed classes
        // should not be processed as specific classes, since these are processed by the usual
        // annotation processing, and non-partial, non-seed classes also should not be processed
        //
        // The second part of the rule is incorrect: Classes in excluded regions are not to
        // be processed. Classes in external regions are to be processed. 

        WebAnnotations webAnnotations = configurator.getWebAnnotations();

        // Be careful about retrieving the class categorization; we need to know if a
        // class is external, but don't need the other values.  The other values might
        // require significant processing to obtain, if partial updates are implemented.

        boolean isIncluded;

        boolean isSetIsExcluded;
        boolean isExcluded;

        boolean skipClass;
        String skipReason;

        if ( skipClass = (isIncluded = webAnnotations.isIncludedClass(className)) ) {
            skipReason = "Seed";
            isSetIsExcluded = false;
            isExcluded = false;
        } else {
            isSetIsExcluded = true;
            if ( skipClass = (isExcluded = webAnnotations.isExcludedClass(className)) ) {
                skipReason = "Excluded";
            } else {
                skipReason = null;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            boolean isPartial = webAnnotations.isPartialClass(className);
            if (!isSetIsExcluded) {
                isExcluded = webAnnotations.isExcludedClass(className);
            }
            boolean isExternal = webAnnotations.isExternalClass(className);

            Tr.debug(tc, methodName + ": Class [ " + className + " ] Seed     [ " + Boolean.valueOf(isIncluded) + " ]");
            Tr.debug(tc, methodName + ": Class [ " + className + " ] Partial  [ " + Boolean.valueOf(isPartial) + " ]");
            Tr.debug(tc, methodName + ": Class [ " + className + " ] Excluded [ " + Boolean.valueOf(isExcluded) + " ]");
            Tr.debug(tc, methodName + ": Class [ " + className + " ] External [ " + Boolean.valueOf(isExternal) + " ]");
        }

        if (skipClass) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, methodName, "Reject: " + skipReason + " class");
            }
            return;
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName, "Select: Non-seed, non-excluded, class");
            }
        }

        Set<String> specifiedClasses = Collections.singleton(className);

        SpecificAnnotations specificAnnotations = webAnnotations.getSpecificAnnotations(specifiedClasses);

        Set<String> webServletClassNames = specificAnnotations.selectAnnotatedClasses(WebServlet.class);
        if ( !webServletClassNames.isEmpty() ) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + ": Detected @WebServlet on [ " + className + " ]");
            }
            configureServletAnnotation(webServletClassNames);
        }

        Set<String> webListenerClassNames = specificAnnotations.selectAnnotatedClasses(WebListener.class);
        if ( !webListenerClassNames.isEmpty() ) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + ": Detected @WebListener on [ " + className + " ]");
            }
            configureListenerAnnotation(webListenerClassNames, true);
        }
                
        Set<String> multipartClassNames = specificAnnotations.selectAnnotatedClasses(javax.servlet.annotation.MultipartConfig.class);
        if ( !multipartClassNames.isEmpty() ) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + ": Detected @MultipartConfig on [ " + className + " ]");
            }
            configureMultipartConfigAnnotation(multipartClassNames);
        }                

        Set<String> webFilterClassNames = specificAnnotations.selectAnnotatedClasses(WebFilter.class);
        if ( !webFilterClassNames.isEmpty() ) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + ": Detected @WebFilter on [ " + className + " ]");
            }
            configureFilterAnnotation(webFilterClassNames, true);            
        }                        
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, methodName);
        }                                
    }

    protected void configureSpecifiedClasses() throws UnableToAdaptException {
        String methodName = "configureSpecifiedClasses";
        String displayName = webAppConfiguration.getDisplayName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, "WebAppConfiguration [ " + displayName + " ]");
        }

        if (configurator.isMetadataComplete()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, methodName, "WebAppConfiguration [ " + displayName + " ]: Metadata-complete");
            }
            return;
        }

        if (annotationScanningRequiredClasses.isEmpty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, methodName, "WebAppConfiguration [ " + displayName + " ]: No annotation required classes");
            }
            return;
        }

        // Specific classes are Servlet, Filter, and Listener classes which are listed 
        // in a web.xml or web-fragment.xml.  These are processed for annotations except
        // when they are in an excluded region.
        //
        // Servlets must be handled first (before processing data which may reference the
        // servlet, which includes filters and listeners).  Filters and listeners must be
        // processing in the order in which they appear in the XML files.
        //
        // Annotation source categories include seed (non-metadata-complete regions),
        // partial (metadata-complete regions), excluded (regions outside of an absolute-order),
        // and external (regions outside of the web module.
        //
        // A first implementation merged the excluded and external regions, then processed
        // specific classes which were in partial regions, the logic being that seed classes
        // should not be processed as specific classes, since these are processed by the usual
        // annotation processing, and non-partial, non-seed classes also should not be processed
        //
        // The second part of the rule is incorrect: Classes in excluded regions are not to
        // be processed. Classes in external regions are to be processed. 

        WebAnnotations webAnnotations = configurator.getWebAnnotations();
        
        Set<String> specifiedClasses = new HashSet<String>();
        
        for (String specifiedClassName : annotationScanningRequiredClasses) {
            
            boolean isIncluded;
            
            boolean isSetIsExcluded;
            boolean isExcluded;
            
            boolean skipClass;
            String skipReason;
            
            if ( skipClass = (isIncluded = webAnnotations.isIncludedClass(specifiedClassName)) ) {
                skipReason = "Seed";
                isSetIsExcluded = false;
                isExcluded = false;
            } else {
                isSetIsExcluded = true;
                if ( skipClass = (isExcluded = webAnnotations.isExcludedClass(specifiedClassName)) ) {
                    skipReason = "Excluded";
                } else {
                    skipReason = null;
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                boolean isPartial = webAnnotations.isPartialClass(specifiedClassName);
                if (!isSetIsExcluded) {
                    isExcluded = webAnnotations.isExcludedClass(specifiedClassName);
                }
                boolean isExternal = webAnnotations.isExternalClass(specifiedClassName);

                Tr.debug(tc, methodName + ": Class [ " + specifiedClassName + " ] Seed     [ " + Boolean.valueOf(isIncluded) + " ]");
                Tr.debug(tc, methodName + ": Class [ " + specifiedClassName + " ] Partial  [ " + Boolean.valueOf(isPartial) + " ]");
                Tr.debug(tc, methodName + ": Class [ " + specifiedClassName + " ] Excluded [ " + Boolean.valueOf(isExcluded) + " ]");
                Tr.debug(tc, methodName + ": Class [ " + specifiedClassName + " ] External [ " + Boolean.valueOf(isExternal) + " ]");
            }

            if (skipClass) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + ": Reject: " + skipReason + " class [ " + specifiedClassName + " ]");
                }                                
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + ": Select: Non-seed, non-excluded, class [ " + specifiedClassName + " ]");
                }                
                specifiedClasses.add(specifiedClassName);                
            }                
        }

        if (specifiedClasses.isEmpty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, methodName, "WebAppConfiguration [ " + displayName + " ]: No selected classes");
            }
            return;
        }

        SpecificAnnotations specificAnnotations = webAnnotations.getSpecificAnnotations(specifiedClasses);
        Set<String> webServletClassNames = specificAnnotations.selectAnnotatedClasses(WebServlet.class);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            for (String webServletClassName : webServletClassNames) {
                Tr.debug(tc, methodName + ": Selected @WebServlet class [ " + webServletClassName + " ]");
            }
        }
        configureServletAnnotation(webServletClassNames);

        // don't process listeners now

        Set<String> multipartClassNames = specificAnnotations.selectAnnotatedClasses(javax.servlet.annotation.MultipartConfig.class);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            for (String multipartClassName : multipartClassNames) {
                Tr.debug(tc, methodName + ": Selected @MultipartConfig class [ " + multipartClassName + " ]");
            }
        }
        configureMultipartConfigAnnotation(multipartClassNames);

        // don't process filters now

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, methodName, "WebAppConfiguration [ " + displayName + " ]");
        }                    
    }

    @Override
    public void configureWebBnd(WebBnd webBnd) {
        OSGiJNDIEnvironmentRefBindingHelper.processBndAndExt(webAppConfiguration.getAllRefBindings(),
                                                             webAppConfiguration.getEnvEntryValues(),
                                                             webAppConfiguration.getResourceRefConfigList(),
                                                             webBnd, null);
        VirtualHost vhost = webBnd.getVirtualHost();
        if ( vhost != null ) {
            webAppConfiguration.setVirtualHostName(vhost);
        }
    }

    @Override
    public void configureWebExt(WebExt webExt) {
        configureResourceRefExtensions(webExt.getResourceRefs());
    }

    @Override
    public void finish() {
        webAppConfiguration.cacheResults(configurator);
        webAppConfiguration.setGlobalAndPrivateConfig();   //PM84305 
    }

    private void configureOrderedLibPaths(List<String> allLocations) {
        webAppConfiguration.setOrderedLibPaths(new ArrayList<String>(allLocations));
    }

    private void configureEnvEntries(List<EnvEntry> envEntries) {
        Map<String, ConfigItem<EnvEntry>> envEntryConfigItemMap = configurator.getConfigItemMap("env-entry");
        Set<String> additiveEnvEntryNames = configurator.getContextSet("env-entry-name");
        for (EnvEntry envEntry : envEntries) {
            String name = envEntry.getName();
            if (name == null) {
                continue;
            }
            ConfigItem<EnvEntry> existedEnvEntry = envEntryConfigItemMap.get(name);
            if (existedEnvEntry == null) {
                EnvEntryImpl envEntryImpl = new EnvEntryImpl(envEntry);
                webAppConfiguration.addRef(JNDIEnvironmentRefType.EnvEntry, envEntryImpl);
                envEntryConfigItemMap.put(name, createConfigItem(envEntryImpl, ENV_ENTRY_COMPARATOR));

                if (configurator.getConfigSource() == ConfigSource.WEB_XML && envEntry.getInjectionTargets().size() == 0) {
                    additiveEnvEntryNames.add(name);
                }
            } else {
                processDuplicateJndiReferenceConfig("env-entry", "env-entry-name", name, existedEnvEntry, envEntry, additiveEnvEntryNames);
            }
        }
    }

    private void configureServiceRefs(List<ServiceRef> serviceRefs) {
        Map<String, ConfigItem<ServiceRef>> serviceRefConfigItemMap = configurator.getConfigItemMap("service-ref");
        Set<String> additiveServiceRefNames = configurator.getContextSet("service-ref-name");
        for (ServiceRef serviceRef : serviceRefs) {
            String name = serviceRef.getName();
            if (name == null) {
                continue;
            }
            ConfigItem<ServiceRef> existedServiceRef = serviceRefConfigItemMap.get(name);
            if (existedServiceRef == null) {
                ServiceRefImpl serviceRefImpl = new ServiceRefImpl(serviceRef);
                webAppConfiguration.addRef(JNDIEnvironmentRefType.ServiceRef, serviceRefImpl);
                serviceRefConfigItemMap.put(name, createConfigItem(serviceRefImpl, SERVICE_REF_COMPARATOR));

                if (configurator.getConfigSource() == ConfigSource.WEB_XML && serviceRef.getInjectionTargets().size() == 0) {
                    additiveServiceRefNames.add(name);
                }
            } else {
                processDuplicateJndiReferenceConfig("service-ref", "service-ref-name", name, existedServiceRef, serviceRef, additiveServiceRefNames);
            }
        }
    }
    
    private void configureResourceRefs(List<ResourceRef> resourceRefs) {
        Map<String, ConfigItem<ResourceRef>> resourceRefConfigItemMap = configurator.getConfigItemMap("resource-ref");
        Set<String> additiveResourceRefNames = configurator.getContextSet("res-ref-name");
        for (ResourceRef resourceRef : resourceRefs) {
            String name = resourceRef.getName();
            if (name == null) {
                continue;
            }
            ConfigItem<ResourceRef> existedResourceRef = resourceRefConfigItemMap.get(name);
            if (existedResourceRef == null) {
                ResourceRefImpl resourceRefImpl = new ResourceRefImpl(resourceRef);
                webAppConfiguration.addRef(JNDIEnvironmentRefType.ResourceRef, resourceRefImpl);
                resourceRefConfigItemMap.put(name, createConfigItem(resourceRefImpl, RESOURCE_REF_COMPARATOR));

                if (configurator.getConfigSource() == ConfigSource.WEB_XML && resourceRef.getInjectionTargets().size() == 0) {
                    additiveResourceRefNames.add(name);
                }
            } else {
                processDuplicateJndiReferenceConfig("resource-ref", "res-ref-name", name, existedResourceRef, resourceRef, additiveResourceRefNames);
            }
        }
    }

    private void configureResourceEnvRefs(List<ResourceEnvRef> resourceEnvRefs) {
        Map<String, ConfigItem<ResourceEnvRef>> resourceEnvRefConfigItemMap = configurator.getConfigItemMap("resource-env-ref");
        Set<String> additiveResourceEnvRefNames = configurator.getContextSet("resource-env-ref-name");
        for (ResourceEnvRef resourceEnvRef : resourceEnvRefs) {
            String name = resourceEnvRef.getName();
            if (name == null) {
                continue;
            }
            ConfigItem<ResourceEnvRef> existedResourceEnvRef = resourceEnvRefConfigItemMap.get(name);
            if (existedResourceEnvRef == null) {
                ResourceEnvRefImpl resourceEnvRefImpl = new ResourceEnvRefImpl(resourceEnvRef);
                webAppConfiguration.addRef(JNDIEnvironmentRefType.ResourceEnvRef, resourceEnvRefImpl);
                resourceEnvRefConfigItemMap.put(name, createConfigItem(resourceEnvRefImpl, RESOURCE_ENV_REF_COMPARATOR));

                if (configurator.getConfigSource() == ConfigSource.WEB_XML && resourceEnvRef.getInjectionTargets().size() == 0) {
                    additiveResourceEnvRefNames.add(name);
                }
            } else {
                processDuplicateJndiReferenceConfig("resource-env-ref", "resource-env-ref-name", name, existedResourceEnvRef, resourceEnvRef, additiveResourceEnvRefNames);
            }
        }
    }
    
    private void configureMessageDestinationRefs(List<MessageDestinationRef> messageDestRefs) {
        Map<String, ConfigItem<MessageDestinationRef>> messageDestRefConfigItemMap = configurator.getConfigItemMap("message-destination-ref");
        Set<String> additiveMessageDestRefNames = configurator.getContextSet("message-destination-ref-name");
        for (MessageDestinationRef messageDestRef : messageDestRefs) {
            String name = messageDestRef.getName();
            if (name == null) {
                continue;
            }
            ConfigItem<MessageDestinationRef> existedMessageDestRef = messageDestRefConfigItemMap.get(name);
            if (existedMessageDestRef == null) {
                MessageDestinationRefImpl messageDestRefImpl = new MessageDestinationRefImpl(messageDestRef);
                webAppConfiguration.addRef(JNDIEnvironmentRefType.MessageDestinationRef, messageDestRefImpl);
                messageDestRefConfigItemMap.put(name, createConfigItem(messageDestRefImpl, MESSAGE_DESTINATION_REF_COMPARATOR));

                if (configurator.getConfigSource() == ConfigSource.WEB_XML && messageDestRef.getInjectionTargets().size() == 0) {
                    additiveMessageDestRefNames.add(name);
                }
            } else {
                processDuplicateJndiReferenceConfig("message-destination-ref", "message-destination-ref-name", name, existedMessageDestRef, messageDestRef, additiveMessageDestRefNames);
            }
        }
    }

    private void configurePersistenceUnitRefs(List<PersistenceUnitRef> persistenceUnitRefs) {
        Map<String, ConfigItem<PersistenceUnitRef>> persistenceUnitRefConfigItemMap = configurator.getConfigItemMap("persistence-unit-ref");
        Set<String> additivePersistenceUnitRefNames = configurator.getContextSet("persistence-unit-ref-name");
        for (PersistenceUnitRef persistenceUnitRef : persistenceUnitRefs) {
            String name = persistenceUnitRef.getName();
            if (name == null) {
                continue;
            }
            ConfigItem<PersistenceUnitRef> existedPersistenceUnitRef = persistenceUnitRefConfigItemMap.get(name);
            if (existedPersistenceUnitRef == null) {
                PersistenceUnitRefImpl persistenceUnitRefImpl = new PersistenceUnitRefImpl(persistenceUnitRef);
                webAppConfiguration.addRef(JNDIEnvironmentRefType.PersistenceUnitRef, persistenceUnitRefImpl);
                persistenceUnitRefConfigItemMap.put(name, createConfigItem(persistenceUnitRefImpl, PERSISTENCE_UNIT_REF_COMPARATOR));

                if (configurator.getConfigSource() == ConfigSource.WEB_XML && persistenceUnitRef.getInjectionTargets().size() == 0) {
                    additivePersistenceUnitRefNames.add(name);
                }
            } else {
                processDuplicateJndiReferenceConfig("persistence-unit-ref", "persistence-unit-ref-name", name, existedPersistenceUnitRef, persistenceUnitRef,
                                                    additivePersistenceUnitRefNames);
            }
        }
    }

    private void configurePersistenceContextRefs(List<PersistenceContextRef> persistenceContextRefs) {
        Map<String, ConfigItem<PersistenceContextRef>> persistenceContextRefConfigItemMap = configurator.getConfigItemMap("persistence-context-ref");
        Set<String> additivePersistenceContextRefNames = configurator.getContextSet("persistence-context-ref-name");
        for (PersistenceContextRef persistenceContextRef : persistenceContextRefs) {
            String name = persistenceContextRef.getName();
            if (name == null) {
                continue;
            }
            ConfigItem<PersistenceContextRef> existedPersistenceContextRef = persistenceContextRefConfigItemMap.get(name);
            if (existedPersistenceContextRef == null) {
                PersistenceContextRefImpl persistenceContextRefImpl = new PersistenceContextRefImpl(persistenceContextRef);
                webAppConfiguration.addRef(JNDIEnvironmentRefType.PersistenceContextRef, persistenceContextRefImpl);
                persistenceContextRefConfigItemMap.put(name, createConfigItem(persistenceContextRefImpl, PERSISTENCE_CONTEXT_REF_COMPARATOR));

                if (configurator.getConfigSource() == ConfigSource.WEB_XML && persistenceContextRef.getInjectionTargets().size() == 0) {
                    additivePersistenceContextRefNames.add(name);
                }
            } else {
                processDuplicateJndiReferenceConfig("persistence-context-ref", "persistence-context-ref-name", name, existedPersistenceContextRef, persistenceContextRef,
                                                    additivePersistenceContextRefNames);
            }
        }
    }

    private void configureDataSources(List<DataSource> dataSources) {
        Map<String, ConfigItem<DataSource>> dataSourceConfigItemMap = configurator.getConfigItemMap("data-source");
        for (DataSource dataSource : dataSources) {
            String name = dataSource.getName();
            if (name == null) {
                continue;
            }
            ConfigItem<DataSource> existedDataSource = dataSourceConfigItemMap.get(name);
            if (existedDataSource == null) {
                dataSourceConfigItemMap.put(name, createConfigItem(dataSource, DATA_SOURCE_COMPARATOR));
                webAppConfiguration.addRef(JNDIEnvironmentRefType.DataSource, dataSource);
            } else {
                if (existedDataSource.getSource() == ConfigSource.WEB_XML && configurator.getConfigSource() == ConfigSource.WEB_FRAGMENT) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "{0}.{1} with value {2}  is configured in web.xml, the value {3} from web-fragment.xml in {4} is ignored", "data-source", "name",
                                 existedDataSource.getValue(), name, configurator.getLibraryURI());
                    }
                } else if (existedDataSource.getSource() == ConfigSource.WEB_FRAGMENT && configurator.getConfigSource() == ConfigSource.WEB_FRAGMENT
                           && !existedDataSource.compareValue(dataSource)) {
                    configurator.addErrorMessage(nls.getFormattedMessage("CONFLICT_DATASOURCE_REFERENCE_BETWEEN_WEB_FRAGMENT_XML",
                                                                         new Object[] { name,
                                                                                       existedDataSource.getLibraryURI(),
                                                                                       configurator.getLibraryURI() },
                                                                         "Two data-source configurations with the same name {0} found in the web-fragment.xml of {1} and {2}."));
                }
            }
        }
    }
    
    /**
     * @param mailSessions
     */
    private void configureMailSessions(List<MailSession> mailSessions) {
        String displayName = webAppConfiguration.getDisplayName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Begin processing mail-session annotations for WebAppConfiguration {0} ", displayName);
        }

        Map<String, ConfigItem<MailSession>> mailSessionConfigItemMap = configurator.getConfigItemMap("mail-session");
        for (MailSession mailSession : mailSessions) {
            String name = mailSession.getName();
            if (name == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Mail-session annotation skipped due to null name value");
                }
                continue;
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Processing mail-session " + name);
            }
            ConfigItem<MailSession> existedMailSession = mailSessionConfigItemMap.get(name);
            if (existedMailSession == null) {
                mailSessionConfigItemMap.put(name, createConfigItem(mailSession, MAIL_SESSION_COMPARATOR));
                webAppConfiguration.addRef(JNDIEnvironmentRefType.MailSession, mailSession);
            } else {
                if (existedMailSession.getSource() == ConfigSource.WEB_XML && configurator.getConfigSource() == ConfigSource.WEB_FRAGMENT) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "{0}.{1} with value {2}  is configured in web.xml, the value {3} from web-fragment.xml in {4} is ignored", "data-source", "name",
                                 existedMailSession.getValue(), name, configurator.getLibraryURI());
                    }
                } else if (existedMailSession.getSource() == ConfigSource.WEB_FRAGMENT && configurator.getConfigSource() == ConfigSource.WEB_FRAGMENT
                           && !existedMailSession.compareValue(mailSession)) {
                    configurator.addErrorMessage(nls.getFormattedMessage("CONFLICT_MAILSESSION_REFERENCE_BETWEEN_WEB_FRAGMENT_XML",
                                                                         new Object[] { name,
                                                                                        existedMailSession.getLibraryURI(),
                                                                                       this.configurator.getLibraryURI() },
                                                                         "Two mail-session configurations with the same name {0} found in the web-fragment.xml of {1} and {2}."));
                }
            }
        }
    }


    private void configureConnectionFactories(List<ConnectionFactory> connFactories) {
        Map<String, ConfigItem<ConnectionFactory>> cfConfigItemMap = configurator.getConfigItemMap("connection-factory");
        for (ConnectionFactory connFactory : connFactories) {
            String name = connFactory.getName();
            if (name == null) {
                continue;
            }
            ConfigItem<ConnectionFactory> existedCF = cfConfigItemMap.get(name);
            if (existedCF == null) {
                cfConfigItemMap.put(name, createConfigItem(connFactory, CF_COMPARATOR));
                webAppConfiguration.addRef(JNDIEnvironmentRefType.ConnectionFactory, connFactory);
            } else {
                if (existedCF.getSource() == ConfigSource.WEB_XML && configurator.getConfigSource() == ConfigSource.WEB_FRAGMENT) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "{0}.{1} with value {2}  is configured in web.xml, the value {3} from web-fragment.xml in {4} is ignored", "connection-factory", "name",
                                 existedCF.getValue(), name, configurator.getLibraryURI());
                    }
                } else if (existedCF.getSource() == ConfigSource.WEB_FRAGMENT && configurator.getConfigSource() == ConfigSource.WEB_FRAGMENT
                           && !existedCF.compareValue(connFactory)) {
                    configurator.addErrorMessage(nls.getFormattedMessage("CONFLICT_CONNECTION_FACTORY_REFERENCE_BETWEEN_WEB_FRAGMENT_XML",
                                                                         new Object[] { name,
                                                                                        existedCF.getLibraryURI(),
                                                                                       this.configurator.getLibraryURI() },
                                                                         "Two connection-factory configurations with the same name {0} found in the web-fragment.xml of {1} and {2}."));
                }
            }
        }
    }
   
     
    private void configureAdministeredObjects(List<AdministeredObject> adminObjects) {
        Map<String, ConfigItem<AdministeredObject>> cfConfigItemMap = configurator.getConfigItemMap("administered-object");
        for (AdministeredObject adminObject : adminObjects) {
            String name = adminObject.getName();
            if (name == null) {
                continue;
            }
            ConfigItem<AdministeredObject> existedAdminObject = cfConfigItemMap.get(name);
            if (existedAdminObject == null) {
                cfConfigItemMap.put(name, createConfigItem(adminObject, ADMINISTERED_OBJECT_COMPARATOR));
                webAppConfiguration.addRef(JNDIEnvironmentRefType.AdministeredObject, adminObject);
            } else {
                if (existedAdminObject.getSource() == ConfigSource.WEB_XML && configurator.getConfigSource() == ConfigSource.WEB_FRAGMENT) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "{0}.{1} with value {2}  is configured in web.xml, the value {3} from web-fragment.xml in {4} is ignored", "administered-object", "name",
                                 existedAdminObject.getValue(), name, configurator.getLibraryURI());
                    }
                } else if (existedAdminObject.getSource() == ConfigSource.WEB_FRAGMENT && configurator.getConfigSource() == ConfigSource.WEB_FRAGMENT
                           && !existedAdminObject.compareValue(adminObject)) {
                    configurator.addErrorMessage(nls.getFormattedMessage("CONFLICT_ADMINISTERED_OBJECT_REFERENCE_BETWEEN_WEB_FRAGMENT_XML",
                                                                         new Object[] { name,
                                                                                        existedAdminObject.getLibraryURI(),
                                                                                       this.configurator.getLibraryURI() },
                                                                         "Two administered-object configurations with the same name {0} found in the web-fragment.xml of {1} and {2}."));
                }
            }
        }
    }
   
    /**
     * To configure JMS ConnectionFactory
     * @param jmsConnFactories
     */
    private void configureJMSConnectionFactories(List<JMSConnectionFactory> jmsConnFactories) {
        
        Map<String, ConfigItem<JMSConnectionFactory>> jmsCfConfigItemMap = configurator.getConfigItemMap(JNDIEnvironmentRefType.JMSConnectionFactory.getXMLElementName());
        
        for (JMSConnectionFactory jmsConnFactory : jmsConnFactories) {
            String name = jmsConnFactory.getName();
            if (name == null) {
                continue;
            }
            ConfigItem<JMSConnectionFactory> existedCF = jmsCfConfigItemMap.get(name);
            if (existedCF == null) {
                jmsCfConfigItemMap.put(name, createConfigItem(jmsConnFactory, JMS_CF_COMPARATOR));
                webAppConfiguration.addRef(JNDIEnvironmentRefType.JMSConnectionFactory, jmsConnFactory);
            } else {
                if (existedCF.getSource() == ConfigSource.WEB_XML && configurator.getConfigSource() == ConfigSource.WEB_FRAGMENT) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "{0}.{1} with value {2}  is configured in web.xml, the value {3} from web-fragment.xml in {4} is ignored", JNDIEnvironmentRefType.JMSConnectionFactory.getXMLElementName(), "name",
                                 existedCF.getValue(), name, configurator.getLibraryURI());
                    }
                } else if (existedCF.getSource() == ConfigSource.WEB_FRAGMENT && configurator.getConfigSource() == ConfigSource.WEB_FRAGMENT
                           && !existedCF.compareValue(jmsConnFactory)) {
                    configurator.addErrorMessage(nls.getFormattedMessage("CONFLICT_JMS_CONNECTION_FACTORY_REFERENCE_BETWEEN_WEB_FRAGMENT_XML",
                                                                         new Object[] { name,
                                                                                        existedCF.getLibraryURI(),
                                                                                       this.configurator.getLibraryURI() },
                                                                         "Two "+JNDIEnvironmentRefType.JMSConnectionFactory.getXMLElementName()+" configurations with the same name {0} found in the web-fragment.xml of {1} and {2}."));
                }
            }
        }
    }

    /**
     * To configure JMS Destinations
     * @param jmsDestinations
     */
    private void configureJMSDestinations(List<JMSDestination> jmsDestinations) {
        Map<String, ConfigItem<JMSDestination>> jmsDestinationConfigItemMap = configurator.getConfigItemMap(JNDIEnvironmentRefType.JMSConnectionFactory.getXMLElementName());
        
        for (JMSDestination jmsDestination : jmsDestinations) {
            String name = jmsDestination.getName();
            if (name == null) {
                continue;
            }
            ConfigItem<JMSDestination> existedCF = jmsDestinationConfigItemMap.get(name);
            if (existedCF == null) {
                jmsDestinationConfigItemMap.put(name, createConfigItem(jmsDestination, JMS_DESTINATION_COMPARATOR));
                webAppConfiguration.addRef(JNDIEnvironmentRefType.JMSDestination, jmsDestination);
            } else {
                if (existedCF.getSource() == ConfigSource.WEB_XML && configurator.getConfigSource() == ConfigSource.WEB_FRAGMENT) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "{0}.{1} with value {2}  is configured in web.xml, the value {3} from web-fragment.xml in {4} is ignored", JNDIEnvironmentRefType.JMSConnectionFactory.getXMLElementName(), "name",
                                 existedCF.getValue(), name, configurator.getLibraryURI());
                    }
                } else if (existedCF.getSource() == ConfigSource.WEB_FRAGMENT && configurator.getConfigSource() == ConfigSource.WEB_FRAGMENT
                           && !existedCF.compareValue(jmsDestination)) {
                    configurator.addErrorMessage(nls.getFormattedMessage("CONFLICT_JMS_DESTINATION_REFERENCE_BETWEEN_WEB_FRAGMENT_XML",
                                                                         new Object[] { name,
                                                                                        existedCF.getLibraryURI(),
                                                                                       this.configurator.getLibraryURI() },
                                                                         "Two "+JNDIEnvironmentRefType.JMSConnectionFactory.getXMLElementName()+" configurations with the same name {0} found in the web-fragment.xml of {1} and {2}."));
                }
            }
        }
    }
    
    private void configureEJBRefs(List<EJBRef> ejbRefs) {
        Map<String, ConfigItem<EJBRef>> ejbRefConfigItemMap = configurator.getConfigItemMap("ejb-ref");
        Set<String> additiveEJBRefNames = configurator.getContextSet("ejb-ref-name");
        for (EJBRef ejbRef : ejbRefs) {
            String name = ejbRef.getName();
            if (name == null) {
                continue;
            }
            ConfigItem<EJBRef> existedEJBRef = ejbRefConfigItemMap.get(name);
            if (existedEJBRef == null) {
                EJBRefImpl ejbRefImpl = new EJBRefImpl(ejbRef);
                webAppConfiguration.addRef(JNDIEnvironmentRefType.EJBRef, ejbRefImpl);
                ejbRefConfigItemMap.put(name, createConfigItem(ejbRefImpl, EJB_REF_COMPARATOR));

                if (configurator.getConfigSource() == ConfigSource.WEB_XML && ejbRef.getInjectionTargets().size() == 0) {
                    additiveEJBRefNames.add(name);
                }
            } else {
                processDuplicateJndiReferenceConfig("ejb-ref", "ejb-ref-name", name, existedEJBRef, ejbRef, additiveEJBRefNames);
            }
        }
    }
    
    private void configureEJBLocalRefs(List<EJBRef> ejbRefs) {
        Map<String, ConfigItem<EJBRef>> ejbRefConfigItemMap = configurator.getConfigItemMap("ejb-local-ref");
        Set<String> additiveEJBRefNames = configurator.getContextSet("ejb-ref-name");
        for (EJBRef ejbRef : ejbRefs) {
            String name = ejbRef.getName();
            if (name == null) {
                continue;
            }
            ConfigItem<EJBRef> existedEJBRef = ejbRefConfigItemMap.get(name);
            if (existedEJBRef == null) {
                EJBRefImpl ejbRefImpl = new EJBRefImpl(ejbRef);
                webAppConfiguration.addRef(JNDIEnvironmentRefType.EJBRef, ejbRefImpl);
                ejbRefConfigItemMap.put(name, createConfigItem(ejbRefImpl, EJB_REF_COMPARATOR));

                if (configurator.getConfigSource() == ConfigSource.WEB_XML && ejbRef.getInjectionTargets().size() == 0) {
                    additiveEJBRefNames.add(name);
                }
            } else {
                processDuplicateJndiReferenceConfig("ejb-local-ref", "ejb-ref-name", name, existedEJBRef, ejbRef, additiveEJBRefNames);
            }
        }
    }

    private void addFilterMapping(final FilterConfig filterConfig, final String urlPattern, final String servletName, DispatcherType[] dispatcherTypes, boolean processMappingsNow) {
        addFilterMapping(filterConfig, urlPattern, servletName, dispatcherTypes, processMappingsNow, -1);
    }

    private void addFilterMapping(final FilterConfig filterConfig, final String urlPattern, final String servletName, DispatcherType[] dispatcherTypes, boolean processMappingsNow, final int index) {
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "addFilterMapping", "servletName [ " + servletName + " ] urlPattern [ " + urlPattern + " ] processMappingsNow [ " + processMappingsNow + "] index [ " + index + " ]");
        } 
        final EnumSet<DispatcherType> dispatcherTypeEnumSet;
        if (dispatcherTypes != null && dispatcherTypes.length > 0) {
            dispatcherTypeEnumSet = EnumSet.noneOf(DispatcherType.class);
            for (DispatcherType dispatcherType : dispatcherTypes) {
                dispatcherTypeEnumSet.add(dispatcherType);
            }
        } else {
            dispatcherTypeEnumSet = null;
        }

        final WebAppConfiguratorHelper configHelper = this;

        if (urlPattern != null) {
            if (processMappingsNow) {
                filterConfig.addMappingForUrlPatterns(dispatcherTypeEnumSet, true, index, urlPattern);
            } else {
                allActions.add(new DeferredAction() {
                    @Override
                    public boolean isAllServletAction() {
                        return false;
                    }

                    @Override
                    public void doAction(ServletConfig servletConfig) throws UnableToAdaptException {
                        filterConfig.addMappingForUrlPatterns(dispatcherTypeEnumSet, true, index, urlPattern);
                        configHelper.configureSpecificClass(filterConfig.getClassName());
                    }
                });
            }
        } else {
            if (servletName.equals("*")) {
                if (processMappingsNow) {
                    Map<String, ConfigItem<ServletConfig>> servletMap = configurator.getConfigItemMap("servlet");
                    for (ConfigItem<ServletConfig> configItem : servletMap.values()) {
                        ServletConfig servletConfig = configItem.getValue();
                        if (servletConfig.getServletName() == null) {
                            continue;
                        }
                        filterConfig.addMappingForServletNames(dispatcherTypeEnumSet, true, servletConfig.getServletName());
                    }
                } else {
                    allActions.add(new DeferredAction() {
                        @Override
                        public boolean isAllServletAction() {
                            return true;
                        }

                        @Override
                        public void doAction(ServletConfig servletConfig) throws UnableToAdaptException {
                            String configName = servletConfig.getServletName();
                            if (configName != null) {
                                filterConfig.addMappingForServletNames(dispatcherTypeEnumSet, true, configName);
                                configHelper.configureSpecificClass(filterConfig.getClassName());
                            }
                        }
                    });
                }
            } else {
                if (processMappingsNow) {
                    filterConfig.addMappingForServletNames(dispatcherTypeEnumSet, true, servletName);
                } else {
                    allActions.add(new DeferredAction() {
                        @Override
                        public boolean isAllServletAction() {
                            return false;
                        }

                        @Override
                        public void doAction(ServletConfig servletConfig) throws UnableToAdaptException {
                            filterConfig.addMappingForServletNames(dispatcherTypeEnumSet, true, servletName);
                            configHelper.configureSpecificClass(filterConfig.getClassName());
                        }
                    });
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "addFilterMapping", "servletName [ " + servletName + " ] urlPattern [ " + urlPattern + " ] processMappingsNow [ " + processMappingsNow + "] index [ " + index + " ]");
        }
    }

    private void configureTargetConfig(TargetConfig targetConfig, String className, String reason, DescriptionGroup descGroup) {
        if (className != null) {
            targetConfig.setClassName(className);
            addToRequiredClasses(className, reason);
        }

        configureDisplayName(targetConfig, getDisplayName(descGroup.getDisplayNames()));
        configureDescription(targetConfig, getDescription(descGroup.getDescriptions()));
        configureIcons(targetConfig, descGroup.getIcons());
    }

    private void configureInitParams(TargetConfig targetConfig, List<ParamValue> initParams, String targetConfigType) {
        Map<String, ConfigItem<String>> initParamMap = configurator.getConfigItemMap(targetConfigType + ".init-param");
        for (ParamValue initParam : initParams) {
            String initParamName = initParam.getName();
            String initParamValue = initParam.getValue();
            configureInitParam(targetConfig, initParamMap, initParamName, initParamValue, targetConfigType);
        }
    }

    private void configureAsyncSupported(TargetConfig targetConfig, boolean isAsyncSupported, String targetConfigType) {
        String targetName = targetConfig.getName();
        Map<String, ConfigItem<Boolean>> asyncSupportedMap = configurator.getConfigItemMap(targetConfigType + ".async-supported");
        ConfigItem<Boolean> existedAsyncSupported = asyncSupportedMap.get(targetName);

        if (existedAsyncSupported == null) {
            targetConfig.setAsyncSupported(isAsyncSupported);
            asyncSupportedMap.put(targetName, createConfigItem(Boolean.valueOf(isAsyncSupported)));
        } else {
            validateDuplicateConfiguration(targetConfigType, "async-supported", Boolean.valueOf(isAsyncSupported), existedAsyncSupported);
        }
    }

    private void configureDisplayName(TargetConfig targetConfig, String displayName) {
        if (displayName != null && !displayName.isEmpty() && targetConfig.getDisplayName() == null) {
            targetConfig.setDisplayName(displayName);
        }
    }

    private void configureDescription(TargetConfig targetConfig, String description) {
        if (description != null && !description.isEmpty() && targetConfig.getDescription() == null) {
            targetConfig.setDescription(description);
        }
    }

    private void configureIcons(TargetConfig targetConfig, List<Icon> icons) {
        for (Icon icon : icons) {
            configureSmallIcon(targetConfig, icon.getSmallIcon());
            configureLargeIcon(targetConfig, icon.getLargeIcon());
        }
    }

    private void configureSmallIcon(TargetConfig targetConfig, String smallIcon) {
        if (smallIcon != null && !smallIcon.isEmpty() && targetConfig.getSmallIcon() == null) {
            targetConfig.setSmallIcon(smallIcon);
        }
    }

    private void configureLargeIcon(TargetConfig targetConfig, String largeIcon) {
        if (largeIcon != null && !largeIcon.isEmpty() && targetConfig.getLargeIcon() == null) {
            targetConfig.setLargeIcon(largeIcon);
        }
    }

    private void configureInitParam(TargetConfig targetConfig,
                                    Map<String, ConfigItem<String>> initParamMap,
                                    String initParamName, String initParamValue,
                                    String targetConfigType) {
        String targetName = targetConfig.getName();
        ConfigItem<String> existedInitParam = initParamMap.get(targetName + "." + initParamName);

        if (existedInitParam == null) {
            targetConfig.addInitParameter(initParamName, initParamValue);
            initParamMap.put(targetName + "." + initParamName, createConfigItem(initParamValue));
        } else {
            validateDuplicateKeyValueConfiguration(targetConfigType, "init-param.param-name", initParamName, "param-value", initParamValue, existedInitParam);
        }
    }

    private void configureCommonWebAnnotation(TargetConfig targetConfig, AnnotationInfo webAnnotation, String targetConfigType) {
        AnnotationValue asyncSupportedValue = webAnnotation.getValue("asyncSupported");
        boolean asyncSupported = (null == asyncSupportedValue ? false : asyncSupportedValue.getBooleanValue());
        configureAsyncSupported(targetConfig, asyncSupported, targetConfigType);

        AnnotationValue displayNameValue = webAnnotation.getValue("displayName");
        configureDisplayName(targetConfig, displayNameValue != null ? displayNameValue.getStringValue() : null);

        AnnotationValue descriptionValue = webAnnotation.getValue("description");
        configureDescription(targetConfig, descriptionValue != null ? descriptionValue.getStringValue() : null);

        AnnotationValue smallIconValue = webAnnotation.getValue("smallIcon");
        configureSmallIcon(targetConfig, smallIconValue != null ? smallIconValue.getStringValue() : null);

        AnnotationValue largeIconValue = webAnnotation.getValue("largeIcon");
        configureLargeIcon(targetConfig, largeIconValue != null ? largeIconValue.getStringValue() : null);

        AnnotationValue initParamsValue = webAnnotation.getValue("initParams");
        final List<? extends AnnotationValue> initParamList;
        final Map<String, ConfigItem<String>> initParamMap;
        if (initParamsValue != null) {
            initParamList = initParamsValue.getArrayValue();
            initParamMap = configurator.getConfigItemMap(targetConfigType + ".init-param");
        } else {
            initParamList = Collections.emptyList();
            initParamMap = null;
        }
        for (AnnotationValue initParam : initParamList) {
            AnnotationInfo initParamAnnotation = initParam.getAnnotationValue();
            String initParamName = initParamAnnotation.getValue("name").getStringValue();
            String initParamValue = initParamAnnotation.getValue("value").getStringValue();
            configureInitParam(targetConfig, initParamMap, initParamName, initParamValue, targetConfigType);
        }
    }

    private void configureContextParam(List<ParamValue> paramValues) {
        Map<String, ConfigItem<String>> contextParamValueConfigItemMap = configurator.getConfigItemMap("context-param");

        for (ParamValue paramValue : paramValues) {
            String contextParamName = paramValue.getName();
            String contextParamValue = paramValue.getValue();

            ConfigItem<String> existedParamValueConfigItem = contextParamValueConfigItemMap.get(contextParamName);
            if (existedParamValueConfigItem == null) {
                webAppConfiguration.addContextParam(contextParamName, contextParamValue);

                contextParamValueConfigItemMap.put(contextParamName, createConfigItem(contextParamValue));
            } else {
                validateDuplicateKeyValueConfiguration("context-param", "param-name", contextParamName, "param-value", contextParamValue,
                                                       existedParamValueConfigItem);
            }
        }
    }

    private void configureWebAppVersion() {
        webAppConfiguration.setVersion(configurator.getServletVersion());
        webAppConfiguration.setMetadataComplete(configurator.isMetadataComplete());
    }

    private void configureDefaultConfigurations() {
        webAppConfiguration.addDefaultWelcomeFiles();
        webAppConfiguration.setDefaultDisplayName();
    }

    private void configureDescription(List<Description> descriptions) {
        String description = getDescription(descriptions);
        if (description != null) {
            this.webAppConfiguration.setDescription(description);
        }
    }

    private void configureDisplayName(List<DisplayName> displayNames) {
        String displayName = getDisplayName(displayNames);
        if (displayName != null) {
            this.webAppConfiguration.setDisplayName(displayName);
        }
    }
    
    private void configureDistributableFromWebApp(boolean distributableSet) {
        if (!WCCustomProperties.IGNORE_DISTRIBUTABLE) {
            this.webAppConfiguration.setDistributable(distributableSet);
        }
    }
    
    private void configureDenyUncoveredHttpMethodsFromWebApp(boolean denyUncoveredHttpMethods) {
        this.webAppConfiguration.setDenyUncoveredHttpMethods(denyUncoveredHttpMethods);
    }
    
    private void configureDistributableFromFragment(boolean distributableSet) {
        if (!WCCustomProperties.IGNORE_DISTRIBUTABLE) {
            //In order for distributable to be set to true, the web.xml would have to have it set and all
            //webframgents would also have to have it set.
            //Therefore, we can assume that distributable is set correctly and we should only set it to false
            //if it is not set
            //from the spec: 
            //ix. The web.xml resulting from the merge is considered <distributable>
            //only if all its web fragments are marked as <distributable> as well.
            if (this.webAppConfiguration.isDistributable() && !distributableSet) {
                this.webAppConfiguration.setDistributable(distributableSet);
                //log something here
                if (TraceComponent.isAnyTracingEnabled() && tc.isInfoEnabled()) {
                    Tr.info(tc, "DISTRIBUTABLE_SET_TO_FALSE_IN_FRAGMENT", new Object[] {configurator.getLibraryURI()});
                }
            }
        }
    }

    // Error pages, and default error pages, per the servlet specification:
    //
    // Servlet 3.0 specification:
    //
    // The error-page contains a mapping between an error code or an exception
    // type to the path of a resource in the Web application. The sub-element
    // exception-type contains a fully qualified class name of a Java exception
    // type. The sub element location element contains the location of the
    // resource in the web application relative to the root of the web application.
    // The value of the location must have a leading "\".
    //
    // Error-page declarations using the exception-type element in the deployment
    // descriptor must be unique up to the class name of the exception-type.
    // Similarly, error-page declarations using the status-code element must be
    // unique in the deployment descriptor up to the status code.
    //
    // Servlet 3.1 specification:
    //
    // If an error-page element in the deployment descriptor does not contain
    // an exception-type or an error-code element, the error page is a default error
    // page.
    //
    // Default error pages are supported in Liberty when the web container feature version
    // is 3.1, or when the web container feature version is 3.0 and the "allow default error
    // pages" WC property is set.
    //
    // Default error pages for Servlet 3.0 are enabled in TWAS by PM94199, and are
    // enabled in Liberty by PI05845.  In both cases support is enabled by a property.
    
    public static final String DEFAULT_ERROR_PAGE_KEY = "DEFAULT";
    
    private void configureErrorPages(List<com.ibm.ws.javaee.dd.web.common.ErrorPage> errorPages) {
        
        Map<String, ConfigItem<String>> errorCodeConfigItemMap =
                        configurator.getConfigItemMap("error-page.error-code");
        Map<String, ConfigItem<String>> exceptionTypeConfigItemMap =
                        configurator.getConfigItemMap("error-page.exception-type");
                
        // Don't even ask for the default map if default error pages are not supported.
        Map<String, ConfigItem<String>> defaultConfigItemMap;        
        if (allowDefaultErrorPageInServlet30() || isServletSpecLevel31OrHigher()) {
            defaultConfigItemMap = configurator.getConfigItemMap("error-page.default");
        } else {
            defaultConfigItemMap = null;
        }

        for (com.ibm.ws.javaee.dd.web.common.ErrorPage errorPage : errorPages) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {                                                
                Tr.debug(tc, "Processing error page {0} with error-code {1} and exception-type {2}.",
                         errorPage.getLocation(),
                         Integer.valueOf(errorPage.getErrorCode()),
                         errorPage.getExceptionType());
            }

            // An error page having no location is not schema valid; the case might
            // be blocked by schema validation, but handle it in case it reaches the
            // configurator.  Ignore any error page which does not have a location.

            String location = errorPage.getLocation();            
            if (isNullOrEmptyString(location)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {                                                
                    Tr.debug(tc, "No location; ignoring");
                }
                continue; // No location: Ignore
            }

            ErrorPage configErrorPage = new ErrorPage(location);            

            // If both an error code and an exception type are set, ignore the error
            // page.
            //
            // Having both error code and exception type set is not schema valid; the
            // case might be blocked by schema validation, but handle it in case it
            // reaches this far.  

            // Duplications are handled by 'validateDuplicateKeyValueConfiguration'
            // and 'validateDuplicateDefaultErrorPageConfiguration':
            //
            // If a duplication is detected for either error-code or exception-type, per
            // 'validateDuplicateKeyValueConfiguration', ignore the duplicating error page
            // if it uses the same location as the initial error page.
            //
            // If a duplication is detected and a different location is provided, either,
            // completely ignore the duplicate default error page, or ignore the page and
            // emit a debug message, or generate a configuration error.  
            //
            // If the duplication is between two elements of web.xml, or between two elements
            // of the same web-fragment.xml, completely ignore the duplication.
            //
            // If the duplication is between an element of a web-fragment.xml and an element
            // of web.xml, ignore the duplication but emit a debug message.
            //
            // If the duplication is between two different web-fragment.xml, generate a
            // configuration error.
            //
            // A duplication between different web-fragment.xml is ignored if there is
            // also a duplication between the web-fragment.xml and web.xml: In such a case,
            // the duplication between the web-fragment.xml and the web.xml creates a
            // debug message and the duplication between the two web-fragment.xml is never
            // noticed.
            
            if (errorPage.isSetErrorCode()) { // An error-code error page ...
                int errorCode = errorPage.getErrorCode();
                String errorCodeString = String.valueOf(errorCode);
                ConfigItem<String> existedErrorPage = errorCodeConfigItemMap.get(errorCodeString);
                if (existedErrorPage == null) {
                    // First error page for the error code: Add it to the tables.
                    configErrorPage.setErrorParam(errorCodeString);
                    webAppConfiguration.addCodeErrorPage(errorCode, configErrorPage);
                    errorCodeConfigItemMap.put(errorCodeString, createConfigItem(location));
                    
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {                                                
                        Tr.debug(tc, "New error-code error page {0} {1}", errorCodeString, location);
                    }
                    
                } else {
                    // Duplicate error page for the error code.  Test the location and source,
                    // and possibly issue a debug message, or generate a configuration error.
                    validateDuplicateKeyValueConfiguration("error-page", "error-code", errorCodeString, "location", location, existedErrorPage);
                }
                
            } else if (!isNullOrEmptyString(errorPage.getExceptionType())) { // An exception type error page ...                
                String exceptionType = errorPage.getExceptionType();
                ConfigItem<String> existedErrorPage = exceptionTypeConfigItemMap.get(exceptionType);
                if (existedErrorPage == null) {
                    // First error page for the exception type: Add it to the tables.                    
                    configErrorPage.setErrorParam(exceptionType);
                    webAppConfiguration.addExceptionErrorPage(exceptionType, configErrorPage);
                    exceptionTypeConfigItemMap.put(exceptionType, createConfigItem(location));
                    
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {                                                
                        Tr.debug(tc, "New exception-type error page {0} {1}", exceptionType, location);
                    }
                    
                } else {
                    // Duplicate error page for the exception type.  Test the location and source,
                    // and possibly issue a debug message, or generate a configuration error.                    
                    validateDuplicateKeyValueConfiguration("error-page", "exception-type", exceptionType, "location", location, existedErrorPage);
                }

            // PI05845 start                                
            } else {
                // Neither error-code nor exception-type is set: A default error page.
                //
                // Ignore these if the feature level is less than 3.1, unless enabled
                // by custom property.
                //
                // By the specification, default error pages ought to be supported when
                // the feature level is 3.0.  However, in TWAS, default error pages
                // were not initially supported.  Support was added by PM94199, but
                // to preserve backwards compatibility, the support is conditional
                // on a custom property.  The Liberty analogue is a WC property.
                // Support is added to Liberty using the new APAR PI05845.
                
                if (!allowDefaultErrorPageInServlet30() && !isServletSpecLevel31OrHigher()) {
                    if (configurator.getConfigSource() == ConfigSource.WEB_XML) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {                                                
                            Tr.debug(tc, "Ignoring default error page with location {0} in web.xml for feature level {1}",
                                     location, Integer.toString(getServletSpecLevel()));
                        }
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {                                                                        
                            Tr.debug(tc, "Ignoring default error page with location {0} in web-fragment.xml in {1} for feature level {2}",
                                     location, configurator.getLibraryURI(), Integer.toString(getServletSpecLevel()));                                                                 
                        }
                    }
                    continue;
                }
                    
                // Ignore for any schema level less than 3.0.  The case is only possible with
                // invalid XML text, but handle it, in case schema validation is off.
                
                int schemaLevel = configurator.getServletVersion();
                if ( schemaLevel < com.ibm.ws.webcontainer.osgi.WebContainer.SPEC_LEVEL_30) {
                    if (configurator.getConfigSource() == ConfigSource.WEB_XML) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {                                                                        
                            Tr.debug(tc, "Ignoring default error page with location {0} in web.xml for schema level {1}",
                                     location, Integer.toString(schemaLevel));
                        }
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {                                                
                            Tr.debug(tc, "Ignoring default error page with location {0} in web-fragment.xml in {1} for specification level {2}",
                                     location, configurator.getLibraryURI(), Integer.toString(schemaLevel));
                        }
                    }                    
                }
                
                // The 'default' map is at most a singleton!
                ConfigItem<String> existingDefaultErrorPage = defaultConfigItemMap.get(DEFAULT_ERROR_PAGE_KEY);

                if (existingDefaultErrorPage == null) {
                    // First default error page: Add it to the tables.
                    webAppConfiguration.setDefaultErrorPage(configErrorPage);
                    defaultConfigItemMap.put(DEFAULT_ERROR_PAGE_KEY, createConfigItem(location));
                } else {
                    // Duplicate default error page.  Test the location and source,
                    // and possibly issue a debug message, or generate a configuration error.                                        
                    validateDuplicateDefaultErrorPageConfiguration(location, existingDefaultErrorPage);
                }                
            }
            // PI05845 end                                        
        }
    }

    private void configureFilters(DeploymentDescriptor webDD, List<Filter> filters) {
        for (Filter filter : filters) {
            processFilterConfig(webDD, filter);
        }
    }

    private void configureFilterMappings(DeploymentDescriptor webDD, List<FilterMapping> filterMappings) {
        for (FilterMapping filterMapping : filterMappings) {
            processFilterMappingConfig(webDD, filterMapping);
        }
    }

    private void configureFilterAnnotation(Set<String> webFilterClassNames) throws UnableToAdaptException {
        configureFilterAnnotation(webFilterClassNames, false);
    }

    private void configureFilterAnnotation(Set<String> webFilterClassNames, boolean processMappings) throws UnableToAdaptException {
        String methodName = "configureFilterAnnotation";
        String displayName = webAppConfiguration.getDisplayName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, "WebAppConfiguration [ " + displayName + " ]");
        }                                        
        
        WebAnnotations webAnnotations = configurator.getWebAnnotations();

        removeFromRequiredClasses(webFilterClassNames, "WebFilter");

        AnnotationTargets_Targets targets = webAnnotations.getAnnotationTargets();
        Map<String, ConfigItem<FilterConfig>> filterMap = configurator.getConfigItemMap("filter");

        for (String className : webFilterClassNames) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + ": @WebFilter target class [ " + className + " ]");
            }                                        
            
            // @WebFilter validation
            
            if (!targets.isInstanceOf(className, javax.servlet.Filter.class)) {
                Tr.error(tc, "ERROR_WEBFILTER_MUST_IMPLEMENT_FILTER", className);
                continue;
            }

            ClassInfo targetClass = webAnnotations.getClassInfo(className);

            AnnotationInfo webFilterAnnotation = targetClass.getAnnotation(WebFilter.class);
            AnnotationValue valueAttribute = webFilterAnnotation.getValue("value");
            boolean valueAttributeConfigured = (null == valueAttribute ? false : !valueAttribute.getArrayValue().isEmpty());
            
            AnnotationValue urlPatternsAnnotation = webFilterAnnotation.getValue("urlPatterns");
            boolean urlPatternsAttributeConfigured = (null == urlPatternsAnnotation ? false : !urlPatternsAnnotation.getArrayValue().isEmpty());
            if (valueAttributeConfigured && urlPatternsAttributeConfigured) {
                Tr.error(tc, "ERROR_WEBFILTER_HAS_VALUE_AND_PATTERNS", className);
                continue;
            }
            
            AnnotationValue servletNameAttribute = webFilterAnnotation.getValue("servletNames");
            boolean servletNameAttributeConfigured = (null == servletNameAttribute ? false : !servletNameAttribute.getArrayValue().isEmpty());
            if (!valueAttributeConfigured && !urlPatternsAttributeConfigured && !servletNameAttributeConfigured) {
                Tr.error(tc, "ERROR_WEBFILTER_MISSING_VAL_PATT_SERVLET", className);
                continue;
            }

            AnnotationValue nameValue = webFilterAnnotation.getValue("filterName");
            String filterName = (null == nameValue ? null : nameValue.getStringValue());
            if (null == filterName || filterName.isEmpty()) {
                filterName = className;
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "@WebFilter filter name == {0}", filterName);
            }

            FilterConfig filterConfig;
            ConfigItem<FilterConfig> existedFilter = filterMap.get(filterName);
            if (existedFilter == null) {
                filterConfig = webAppConfiguration.createFilterConfig("FilterGeneratedId" + configurator.generateUniqueId(), filterName);
                filterConfig.setFilterClassName(className);

                webAppConfiguration.addFilterInfo(filterConfig);

                filterMap.put(filterName, createConfigItem(filterConfig));

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Add new filter from WebFilter annotation filter class name = {0}", className);
                }
            } else {
                filterConfig = existedFilter.getValue();
            }

            configureCommonWebAnnotation(filterConfig, webFilterAnnotation, "filter");
            configureWebFilterAnnotation(filterConfig, webFilterAnnotation, processMappings);
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, methodName, "WebAppConfiguration [ " + displayName + " ]");
        }                                                
    }

    private void configureListener(List<Listener> listeners) {
        addListener(listeners, false);
    }

    private void addListener(List<Listener> listeners, boolean processListenerNow) {
        Set<String> listenerClassNames = configurator.getContextSet("listener");
        for (Listener listener : listeners) {
            String listenerClassName = listener.getListenerClassName();
            if (listenerClassNames.contains(listenerClassName)) {
                continue;
            }
            listenerClassNames.add(listenerClassName);
            addToRequiredClasses(listenerClassName, "Listener");
            if (processListenerNow) {
                webAppConfiguration.addListener(listenerClassName);
            } else {
                addDeferredListener(listenerClassName);
            }
        }
    }

    private void configureListenerAnnotation(Set<String> selectedTargets) throws UnableToAdaptException {
        configureListenerAnnotation(selectedTargets, false);
    }

    private void configureListenerAnnotation(Set<String> selectedTargets, boolean processListenerNow) throws UnableToAdaptException {
        String methodName = "configureListenerAnnotation";
        String displayName = webAppConfiguration.getDisplayName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, "WebAppConfiguration [ " + displayName + " ]");
        }

        WebAnnotations webAnnotations = configurator.getWebAnnotations();
        Set<String> listenerClassNames = configurator.getContextSet("listener");

        removeFromRequiredClasses(selectedTargets, "Listener");

        for (String listenerClassName : selectedTargets) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + ": @WebListener target class [ " + listenerClassName + " ]");
            }
            
            ClassInfo listenerClassInfo = webAnnotations.getClassInfo(listenerClassName);

            if (listenerClassNames.contains(listenerClassName)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "The same listener class {0} has already been added, the @WebListener is ignored", listenerClassName);
                }
                continue;
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "In processWebListenerAnnotations - processing WebListener {0}", listenerClassName);
            }

            boolean implementSupportedListenerInterfaceName = false;
            for (Class<?> iface : listenerInterfaces) {
                if (listenerClassInfo.isInstanceOf(iface)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "WebListener {0} implements required interface {1}", listenerClassName, iface.getName());
                    }
                    implementSupportedListenerInterfaceName = true;
                    break;
                }
            }

            if (!implementSupportedListenerInterfaceName) {
                Tr.error(tc, "ERROR_WEBLISTENER_MUST_IMPLEMENT_IFACE", listenerClassName);
            }
            if (processListenerNow) {
                webAppConfiguration.addListener(listenerClassName);
            } else {
                addDeferredListener(listenerClassName);
            }
            listenerClassNames.add(listenerClassName);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, methodName, "WebAppConfiguration [ " + displayName + " ]");
        }
    }

    private void addDeferredListener(String listenerClassName) {
        final String finalListenerClassName = listenerClassName;
        final WebAppConfiguratorHelper configHelper = this;
        allActions.add(new DeferredAction() {
            @Override
            public boolean isAllServletAction() {
                return false;
            }

            @Override
            public void doAction(ServletConfig servletConfig) throws UnableToAdaptException {
                webAppConfiguration.addListener(finalListenerClassName);
                configHelper.configureSpecificClass(finalListenerClassName);
            }
        });
    }

    private void configureMimeMapping(List<MimeMapping> mimeMappings) {
        Map<String, ConfigItem<String>> mimeMappingConfigItemMap = configurator.getConfigItemMap("mime-mapping");

        for (MimeMapping mimeMapping : mimeMappings) {

            String extension = mimeMapping.getExtension();
            String mimeType = mimeMapping.getMimeType();
            if (isNullOrEmptyString(extension) || isNullOrEmptyString(mimeType)) {
                continue;
            }

            ConfigItem<String> existedMimeMapping = mimeMappingConfigItemMap.get(extension);
            if (existedMimeMapping == null) {
                webAppConfiguration.addMimeMapping(extension, mimeType);
                mimeMappingConfigItemMap.put(extension, createConfigItem(mimeType));
            } else {
                validateDuplicateKeyValueConfiguration("mime-mapping", "extension", extension, "mime-type", mimeType, existedMimeMapping);
            }
        }

    }

    private void configureMultipartConfigAnnotation(Set<String> selectedTargets) throws UnableToAdaptException {
        String methodName = "configureMultipartConfigAnnotation";
        String displayName = webAppConfiguration.getDisplayName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, "WebAppConfiguration [ " + displayName + " ]");
        }                                        
        
        WebAnnotations webAnnotations = configurator.getWebAnnotations();

        for (String targetClassName : selectedTargets) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "@MultipartConfig found on class {0}", targetClassName);
            }

            ClassInfo classInfo = webAnnotations.getClassInfo(targetClassName);
            final String fullyQualifiedClassName = targetClassName;

            AnnotationInfo mpCfgAnnotation = classInfo.getAnnotation(javax.servlet.annotation.MultipartConfig.class);

            AnnotationValue locationValue = mpCfgAnnotation.getValue("location");
            final String location = locationValue.getStringValue();

            AnnotationValue maxFileSizeValue = mpCfgAnnotation.getValue("maxFileSize");
            final long maxFileSize = maxFileSizeValue.getLongValue();

            AnnotationValue maxRequestSizeValue = mpCfgAnnotation.getValue("maxRequestSize");
            final long maxReqSize = maxRequestSizeValue.getLongValue();

            AnnotationValue fileSizeThresholdValue = mpCfgAnnotation.getValue("fileSizeThreshold");
            final int fileSizeThreshold = fileSizeThresholdValue.getIntValue();

            allActions.add(new DeferredAction() {
                @Override
                public boolean isAllServletAction() {
                    return true;
                }

                @Override
                public void doAction(ServletConfig servletConfig) {
                    if (fullyQualifiedClassName.equals(servletConfig.getClassName()) && servletConfig.getServletName() != null) {
                        if (servletConfig.getMultipartConfig() == null) {
                            servletConfig.setMultipartConfig(new MultipartConfigElement(location, maxFileSize, maxReqSize, fileSizeThreshold));
                        }
                    }
                }
            });
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, methodName, "WebAppConfiguration [ " + displayName + " ]");
        }                                                
    }

    private void configureRunAsAnnotation(Set<String> selectedTargets) throws UnableToAdaptException {
        String methodName = "configureRunAsAnnotation";
        String displayName = webAppConfiguration.getDisplayName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, "WebAppConfiguration [ " + displayName + " ]");
        }                                        

        WebAnnotations webAnnotations = configurator.getWebAnnotations();

        for (String targetClassName : selectedTargets) {
            ClassInfo classInfo = webAnnotations.getClassInfo(targetClassName);
            final String fullyQualifiedClassName = targetClassName;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "@RunAs found on class {0}", fullyQualifiedClassName);
            }

            AnnotationInfo runAsAnnotation = classInfo.getAnnotation(RunAs.class);
            AnnotationValue value = runAsAnnotation.getValue("value");
            final String role = value.getStringValue();

            allActions.add(new DeferredAction() {
                @Override
                public boolean isAllServletAction() {
                    return true;
                }

                @Override
                public void doAction(ServletConfig servletConfig) {
                    if (fullyQualifiedClassName.equals(servletConfig.getClassName()) && servletConfig.getServletName() != null) {
                        if (servletConfig.getRunAsRole() == null) {
                            servletConfig.setRunAsRole(role);
                        }
                    }
                }
            });

            Map<String, ConfigItem<ServletConfig>> servletMap = configurator.getConfigItemMap("servlet");
            for (ConfigItem<ServletConfig> configItem : servletMap.values()) {
                ServletConfig servletConfig = configItem.getValue();

                if (!fullyQualifiedClassName.equals(servletConfig.getClassName()) || servletConfig.getServletName() == null) {
                    continue;
                }

                if (servletConfig.getRunAsRole() == null) {
                    servletConfig.setRunAsRole(role);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "@RunAs for servlet name " + servletConfig.getServletName() + " has value == " + role);
                    }
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "@RunAs for servlet name " + servletConfig.getServletName() + " is configured in the web.xml or web-fragment.xml, @RunAs is ignored");
                    }
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, methodName, "WebAppConfiguration [ " + displayName + " ]");
        }                                                
    }

    private void configureServlets(DeploymentDescriptor webDD, List<Servlet> servlets) {
        for (Servlet servlet : servlets) {
            processServletConfig(webDD, servlet);
        }
    }

    private void configureServletMappings(List<ServletMapping> servletMappings) throws UnableToAdaptException {
        for (ServletMapping servletMapping : servletMappings) {
            processServletMappingConfig(servletMapping);
        }
    }

    private void configureLocaleEncodingMap(LocaleEncodingMappingList mapList) {
        if (mapList != null) {
            List<LocaleEncodingMapping> mappingList = mapList.getLocaleEncodingMappings();
            if (mappingList != null) {
                for (LocaleEncodingMapping oneMapping : mappingList) {
                    webAppConfiguration.addLocaleEncodingMap(oneMapping.getLocale(), oneMapping.getEncoding());
                }
            }
        }
    }

    private void configureServletAnnotation(Set<String> webServletClassNames) throws UnableToAdaptException {
        String methodName = "configureServletAnnotation";
        String displayName = webAppConfiguration.getDisplayName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, "WebAppConfiguration [ " + displayName + " ]");
        }        
        
        WebAnnotations webAnnotations = configurator.getWebAnnotations();

        removeFromRequiredClasses(webServletClassNames, "Servlet");

        AnnotationTargets_Targets targets = webAnnotations.getAnnotationTargets();
        Map<String, ConfigItem<ServletConfig>> servletMap = configurator.getConfigItemMap("servlet");

        for (String className : webServletClassNames) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + ": @WebServlet target class [ " + className + " ]");
            }            
            
            //@WebServlet validation

            if (!targets.isInstanceOf(className, HttpServlet.class)) {
                Tr.error(tc, "ERROR_WEBSERVLET_MUST_IMPLEMENT_HTTPSERVLET", className);
                continue;
            }

            ClassInfo targetClass = webAnnotations.getClassInfo(className);

            AnnotationInfo webServletAnnotation = targetClass.getAnnotation(WebServlet.class);
            AnnotationValue valueAttribute = webServletAnnotation.getValue("value");
            
            AnnotationValue urlPatternsAttribute = webServletAnnotation.getValue("urlPatterns");
            boolean valueAttributeConfigured = (null == valueAttribute ? false : !webServletAnnotation.getValue("value").getArrayValue().isEmpty());
            boolean urlPatternsAttributeConfigured = (null == urlPatternsAttribute ? false : !webServletAnnotation.getValue("urlPatterns").getArrayValue().isEmpty());
            if (valueAttributeConfigured && urlPatternsAttributeConfigured) {
                Tr.error(tc, "ERROR_WEBSERVLET_HAS_VALUE_AND_PATTERNS", className);
                continue;
            }
            if (!valueAttributeConfigured && !urlPatternsAttributeConfigured) {
                Tr.error(tc, "ERROR_WEBSERVLET_MISSING_PATTERNS", className);
                continue;
            }

            AnnotationValue nameValue = webServletAnnotation.getValue("name");
            String servletName = (null == nameValue ? null : nameValue.getStringValue());
            if (null == servletName || servletName.isEmpty()) {
                servletName = className;
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "@WebServlet servlet name == " + servletName);
            }

            ConfigItem<ServletConfig> existedServlet = servletMap.get(servletName);

            ServletConfig servletConfig = null;
            if (existedServlet == null) {
                servletConfig = webAppConfiguration.createServletConfig("ServletGeneratedId" + configurator.generateUniqueId(), servletName);
                servletConfig.setClassName(className);
                webAppConfiguration.addServletInfo(servletName, servletConfig);

                servletMap.put(servletName, createConfigItem(servletConfig));

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Add new servlet from WebServlet annotation servlet class name = {0}", className);
                }
            } else {
                servletConfig = existedServlet.getValue();
            }

            configureCommonWebAnnotation(servletConfig, webServletAnnotation, "servlet");
            configureWebServletAnnotation(servletConfig, webServletAnnotation);
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, methodName, "WebAppConfiguration [ " + displayName + " ]");
        }                
    }

    private void configureSessionConfig(SessionConfig sessionConfig) {
        if (sessionConfig == null) {
            return;
        }

        Map<String, ConfigItem<String>> sessionConfigItemMap = configurator.getConfigItemMap("session-config");

        if (sessionConfig.isSetSessionTimeout()) {
            int sessionTimeout = sessionConfig.getSessionTimeout();
            ConfigItem<String> existedTimeout = sessionConfigItemMap.get("session-timeout");
            if (existedTimeout == null) {
                webAppConfiguration.setSessionTimeout(sessionTimeout);

                sessionConfigItemMap.put("session-timeout", createConfigItem(String.valueOf(sessionTimeout)));
            } else {
                validateDuplicateConfiguration("session-config", "session-timeout", String.valueOf(sessionTimeout), existedTimeout);
            }
        }

        CookieConfig cookieConfig = sessionConfig.getCookieConfig();
        if (cookieConfig != null) {

            SessionCookieConfigImpl sessionCookieConfigImpl = webAppConfiguration.getSessionCookieConfig();

            String cookieComment = cookieConfig.getComment();
            if (cookieComment != null) {
                ConfigItem<String> existedComment = sessionConfigItemMap.get("comment");
                if (existedComment == null) {
                    sessionCookieConfigImpl.setComment(cookieComment, false);
                    sessionConfigItemMap.put("comment", createConfigItem(cookieComment));
                } else {
                    validateDuplicateConfiguration("cookie-config", "comment", cookieComment, existedComment);
                }
            }

            String cookieDomain = cookieConfig.getDomain();
            if (cookieDomain != null) {
                ConfigItem<String> existedDomain = sessionConfigItemMap.get("domain");
                if (existedDomain == null) {
                    sessionCookieConfigImpl.setDomain(cookieDomain, false);
                    sessionConfigItemMap.put("domain", createConfigItem(cookieDomain));
                } else {
                    validateDuplicateConfiguration("cookie-config", "domain", cookieDomain, existedDomain);
                }
            }

            if (cookieConfig.isSetMaxAge()) {
                int maxAge = cookieConfig.getMaxAge();
                ConfigItem<String> existedMaxAge = sessionConfigItemMap.get("max-age");
                if (existedMaxAge == null) {
                    sessionCookieConfigImpl.setMaxAge(maxAge, false);
                    sessionConfigItemMap.put("max-age", createConfigItem(String.valueOf(maxAge)));
                } else {
                    validateDuplicateConfiguration("cookie-config", "max-age", String.valueOf(maxAge), existedMaxAge);
                }
            }

            String cookieName = cookieConfig.getName();
            if (cookieName != null) {
                ConfigItem<String> existedName = sessionConfigItemMap.get("name");
                if (existedName == null) {
                    sessionCookieConfigImpl.setName(cookieName, false);
                    sessionConfigItemMap.put("name", createConfigItem(cookieName));
                } else {
                    validateDuplicateConfiguration("cookie-config", "name", cookieDomain, existedName);
                }
            }

            String cookiePath = cookieConfig.getPath();
            if (cookiePath != null) {
                ConfigItem<String> existedPath = sessionConfigItemMap.get("path");
                if (existedPath == null) {
                    sessionCookieConfigImpl.setPath(cookiePath, false);
                    sessionConfigItemMap.put("path", createConfigItem(cookiePath));
                } else {
                    validateDuplicateConfiguration("cookie-config", "path", cookiePath, existedPath);
                }
            }

            if (cookieConfig.isSetHTTPOnly()) {
                final boolean httpOnly = cookieConfig.isHTTPOnly();
                ConfigItem<String> existedHttpOnly = sessionConfigItemMap.get("http-only");
                if (existedHttpOnly == null) {
                    sessionCookieConfigImpl.setHttpOnly(httpOnly, false);
                    sessionConfigItemMap.put("http-only", createConfigItem(String.valueOf(httpOnly)));
                } else {
                    validateDuplicateConfiguration("cookie-config", "http-only", String.valueOf(httpOnly), existedHttpOnly);
                }

            }

            if (cookieConfig.isSetSecure()) {
                final boolean cookieSecured = cookieConfig.isSecure();
                ConfigItem<String> existedHttpOnly = sessionConfigItemMap.get("secure");
                if (existedHttpOnly == null) {
                    sessionCookieConfigImpl.setSecure(cookieSecured, false);
                    sessionConfigItemMap.put("secure", createConfigItem(String.valueOf(cookieSecured)));
                } else {
                    validateDuplicateConfiguration("cookie-config", "secure", String.valueOf(cookieSecured), existedHttpOnly);
                }

            }
        }

        List<SessionConfig.TrackingModeEnum> trackingModeValues = sessionConfig.getTrackingModeValues();
        if (trackingModeValues != null && !trackingModeValues.isEmpty()) {
            for (SessionConfig.TrackingModeEnum trackingModeValue : trackingModeValues) {
                SessionTrackingMode mode = null;
                switch (trackingModeValue) {
                    case COOKIE:
                        mode = SessionTrackingMode.COOKIE;
                        break;
                    case SSL:
                        mode = SessionTrackingMode.SSL;
                        break;
                    case URL:
                        mode = SessionTrackingMode.URL;
                        break;
                }
                webAppConfiguration.addSessionTrackingMode(mode);
            }
        }
    }

    private void configureWebFilterAnnotation(FilterConfig filterConfig, AnnotationInfo webFilterAnnotation, boolean processMappingsNow) {
        String methodName = "configureWebFilterAnnotation";
        String displayName = webAppConfiguration.getDisplayName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, "WebAppConfiguration [ " + displayName + " ]");
        }        
        
        String filterName = filterConfig.getFilterName();
        Map<String, ConfigItem<List<String>>> filterMappingMap = configurator.getConfigItemMap("filter-mapping");
        if (!filterMappingMap.containsKey(filterName)) {
            if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc,  methodName + ": Add [ " + filterName + " ]");
            }
            
            AnnotationValue dispatcherTypesValue = webFilterAnnotation.getValue("dispatcherTypes");

            List<? extends AnnotationValue> dispatcherTypeList =
                            (null == dispatcherTypesValue ? new ArrayList<AnnotationValue>() : dispatcherTypesValue.getArrayValue());
            DispatcherType[] dispatcherTypes = new DispatcherType[dispatcherTypeList.size()];
            int index = 0;
            for (AnnotationValue dispatcherType : dispatcherTypeList) {
                String dispatcherTypeValue = dispatcherType.getStringValue();
                dispatcherTypes[index++] = DispatcherType.valueOf(dispatcherTypeValue);
            }

            AnnotationValue urlPatternsValue = webFilterAnnotation.getValue("value");
            if ((null == urlPatternsValue) || urlPatternsValue.getArrayValue().isEmpty()) {
                urlPatternsValue = webFilterAnnotation.getValue("urlPatterns");
            }

            if (null != urlPatternsValue) {
                for (AnnotationValue urlPatternValue : urlPatternsValue.getArrayValue()) {
                    addFilterMapping(filterConfig, urlPatternValue.getStringValue(), null, dispatcherTypes, processMappingsNow);
                }
            }

            AnnotationValue servletNamesValue = webFilterAnnotation.getValue("servletNames");
            if (null != servletNamesValue) {
                for (AnnotationValue servletNameValue : servletNamesValue.getArrayValue()) {
                    addFilterMapping(filterConfig, null, servletNameValue.getStringValue(), dispatcherTypes, false);
                }
            }
        } else {
            if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc,  methodName + ": Ignore [ " + filterName + " ]: Duplicate");
            }
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, methodName, "WebAppConfiguration [ " + displayName + " ]");
        }
    }

    private void configureWebServletAnnotation(ServletConfig servletConfig, AnnotationInfo webServletAnnotation) throws UnableToAdaptException {
        String methodName = "configureWebServletAnnotation";
        String displayName = webAppConfiguration.getDisplayName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, "WebAppConfiguration [ " + displayName + " ]");
        }        
        
        AnnotationValue loadOnStartupValue = webServletAnnotation.getValue("loadOnStartup");
        int loadOnStartup = (null == loadOnStartupValue ? -1 : loadOnStartupValue.getIntValue());

        String servletName = servletConfig.getServletName();
        Map<String, ConfigItem<Boolean>> loadOnStartupMap = configurator.getConfigItemMap("servlet.load-on-startup");
        if (!loadOnStartupMap.containsKey(servletName)) {
            servletConfig.setLoadOnStartup(loadOnStartup);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "@WebServlet servlet == {0} class == {1} loadOnStartup == {2}", servletName, servletConfig.getClassName(), Integer.valueOf(loadOnStartup));
            }
        }

        Map<String, ConfigItem<List<String>>> servletMappingMap = configurator.getConfigItemMap("servlet-mapping");
        if (!servletMappingMap.containsKey(servletName)) {
            AnnotationValue urlPatternListValue = webServletAnnotation.getValue("value");
            List<? extends AnnotationValue> urlPatternList = (null == urlPatternListValue ? null : urlPatternListValue.getArrayValue());
            if (null == urlPatternList || urlPatternList.isEmpty()) {
                urlPatternList = webServletAnnotation.getValue("urlPatterns").getArrayValue();
            }
            for (AnnotationValue urlPatternValue : urlPatternList) {
                String urlText = urlPatternValue.getStringValue();
                
                // Servlet 3.1: Don't allow a URL to map to more than one servlet name.
                //              Fail discovery if a duplicate mapping is made.
                //
                // Testing is across calls to 'configureWebServletAnnotation', which is
                // made once from 'configureServletAnnotation', which has multiple callers.
                //
                // That is to say, 'urlToServletNameMap' is deliberately scoped to the,
                // configurator helper, and detects conflicts at a wider scope than a single
                // WebServlet annotation.

                if (isServletSpecLevel31OrHigher()) {
                    // Strange to 'put' on error cases.
                    String existingName = urlToServletNameMap.put(urlText, servletName);
                    if ((existingName != null) && !existingName.equals(servletName)) {
                        Tr.error(tc, "duplicate.url.pattern.for.servlet.mapping", urlText, servletName, existingName);
                        throw new UnableToAdaptException(nls.getFormattedMessage("duplicate.url.pattern.for.servlet.mapping",
                                                                                 new Object[] { urlText, servletName, existingName }, 
                                                                                 "servlet-mapping value matches multiple servlets: " + urlText));
                    }
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + ": Map servlet [ " + servletName + " ] to URL [ " + urlText + " ]");
                }
                webAppConfiguration.addServletMapping(servletName, urlText);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, methodName, "WebAppConfiguration [ " + displayName + " ]");
        }
    }

    private void configureWelcomeFileList(WelcomeFileList welcomeFileList) {
        if (welcomeFileList == null) {
            return;
        }
        Set<String> welcomeFileSet = configurator.getContextSet("welcome-file-list");

        for (String welcomeFile : welcomeFileList.getWelcomeFiles()) {
            if (welcomeFileSet.contains(welcomeFile)) {
                continue;
            }
            welcomeFileSet.add(welcomeFile);
            webAppConfiguration.addWelcomeFile(welcomeFile);
        }
    }

    private void configureResourceRefExtensions(List<com.ibm.ws.javaee.dd.commonext.ResourceRef> resRefExts) {
        RefBndAndExtHelper.configureResourceRefExtensions(resRefExts, webAppConfiguration.getResourceRefConfigList());
    }

    private MultipartConfigElement createMultipartConfigElement(MultipartConfig multipartConfig) {
        return new MultipartConfigElement(
                        multipartConfig.getLocation(),
                        multipartConfig.isSetMaxFileSize() ? multipartConfig.getMaxFileSize() : -1L,
                        multipartConfig.isSetMaxRequestSize() ? multipartConfig.getMaxRequestSize() : -1L,
                        multipartConfig.isSetFileSizeThreshold() ? multipartConfig.getFileSizeThreshold() : 0);
    }

    private String getDescription(List<Description> descriptions) {
        for (Description description : descriptions) {
            String value = description.getValue();
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return null;
    }

    private String getDisplayName(List<DisplayName> displayNames) {
        for (DisplayName displayName : displayNames) {
            String value = displayName.getValue();
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private boolean isSameMultipartConfigs(MultipartConfig configA, MultipartConfig configB) {
        if (configA.getLocation() == null) {
            if (configB.getLocation() != null) {
                return false;
            }
        } else if (!configA.getLocation().equals(configB.getLocation())) {
            return false;
        }
        if ((configA.isSetFileSizeThreshold() || configB.isSetFileSizeThreshold()) && (!configA.isSetFileSizeThreshold() || !configB.isSetFileSizeThreshold()
            || configA.getFileSizeThreshold() != configB.getFileSizeThreshold())) {
            return false;
        }
        if ((configA.isSetMaxFileSize() || configB.isSetMaxFileSize()) && (!configA.isSetMaxFileSize() || !configB.isSetMaxFileSize()
            || configA.getMaxFileSize() != configB.getMaxFileSize())) {
            return false;
        }
        if ((configA.isSetMaxRequestSize() || configB.isSetMaxRequestSize()) && (!configA.isSetMaxRequestSize() || !configB.isSetMaxRequestSize()
            || configA.getMaxRequestSize() != configB.getMaxRequestSize())) {
            return false;
        }
        return true;
    }

    private final boolean isNullOrEmptyString(String value) {
        return value == null || value.isEmpty();
    }

    private FilterConfig createCdiConversationFilter(DeploymentDescriptor webDD) {
        // TODO this is a temporary solution
        // Allow a proper FilterConfig object get created for the ""CDI Conversation Filter" filter
        // but leave the impl details blank.  The impl details are filled in by 
        // com.ibm.ws.cdi.web.WeldConfiguratorHelperFactory.WeldConfigHelper.configureFromWebApp(WebApp)
        return processFilterConfig(webDD, new Filter() {
            
            @Override
            public List<Description> getDescriptions() {
                return Collections.emptyList();
            }
            
            @Override
            public List<Icon> getIcons() {
                return Collections.emptyList();
            }
            
            @Override
            public List<DisplayName> getDisplayNames() {
                return Collections.emptyList();
            }
            
            @Override
            public boolean isSetAsyncSupported() {
                return true;
            }
            
            @Override
            public boolean isAsyncSupported() {
                return true;
            }
            
            @Override
            public List<ParamValue> getInitParams() {
                return Collections.emptyList();
            }
            
            @Override
            public String getFilterName() {
                return CDI_CONVERSATION_FILTER;
            }
            
            @Override
            public String getFilterClass() {
                return null;
            }
        });
    }
    /**
     * Merging Rules :
     * a. Once a filter configuration exists, the following ones with the same filter name are ignored
     * b. If init-param/asyncSupported is configured in web.xml, all the values from web-fagment.xml and annotation are ignored
     * b. If init-param/asyncSupported is NOT configured in web.xml,
     * b1. Those values from web-fragment.xml and annotation will be considered
     * b2. If they are configured in different web-fragment.xml, their values should be the same, or will trigger a warning
     */
    private FilterConfig processFilterConfig(DeploymentDescriptor webDD, Filter filter) {

        String filterName = filter.getFilterName();
        Map<String, ConfigItem<FilterConfig>> filterMap = configurator.getConfigItemMap("filter");
        ConfigItem<FilterConfig> existedFilter = filterMap.get(filterName);

        FilterConfig filterConfig = null;
        if (existedFilter == null) {
            String id = webDD.getIdForComponent(filter);
            if (id == null) {
                id = "FilterGeneratedId" + configurator.generateUniqueId();
            }
            filterConfig = webAppConfiguration.createFilterConfig(id, filterName);

            configureTargetConfig(filterConfig, filter.getFilterClass(), "Filter", filter);

            webAppConfiguration.addFilterInfo(filterConfig);

            filterMap.put(filterName, createConfigItem(filterConfig));
        } else {
            filterConfig = existedFilter.getValue();
        }

        configureInitParams(filterConfig, filter.getInitParams(), "filter");

        if (filter.isSetAsyncSupported()) {
            configureAsyncSupported(filterConfig, filter.isAsyncSupported(), "filter");
        }
        return filterConfig;
    }

    
    private void processFilterMappingConfig(DeploymentDescriptor webDD, FilterMapping filterMapping){
        processFilterMappingConfig(webDD, filterMapping, false, filterMappingIndex);
    }
    
    /**
     * Merging Rules :
     * a. If the filter-mapping is configured in web.xml, any other configuration from web-fragment.xml and annotation are ignored
     * b. If no filter-mapping is configured in web.xml, those configurations from web-fragment.xml should be additive
     * c. If filter-mapping is configured in web-fragment.xml, those configurations from annotation are ignored
     * @param webDD 
     */
    private void processFilterMappingConfig(DeploymentDescriptor webDD, FilterMapping filterMapping, boolean defer, int index) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "processFilterMappingConfig", "webDD [ " + webDD + " ] defer [ " + defer + " ] index [ " + index + " ]");
        } 
        String filterName = filterMapping.getFilterName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "processFilterMappingConfig: filterName [ " + filterName + " ]");
        } 
        Map<String, ConfigItem<FilterConfig>> filterMap = configurator.getConfigItemMap("filter");
        ConfigItem<FilterConfig> filterItem = filterMap.get(filterName);
        if (filterItem == null) {
            if(defer){
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, " No filter configuration found for {0}, the filter-mapping configuration in {1} is ignored", filterName, configurator.getLibraryURI());
                }
                filterMappingIndexOffset++;
            }
            else if (deferProcessingIncompleteFiltersInWebXML){
                deferredFilterMappings.add(new MappingIndexPair<FilterMapping, Integer>(filterMapping, filterMappingIndex));
            }
            filterMappingIndex++;
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "processFilterMappingConfig", "webDD [ " + webDD + " ] defer [ " + defer + " ] index [ " + index + " ]");
            }  
            return;
        }
        filterMappingIndex++;

        FilterConfig filterConfig = filterItem.getValue();
        if (filterConfig == null && CDI_CONVERSATION_FILTER.equals(filterName)) {
            // TODO this is a temporary solution until we can come up with a way
            // for CDI to add an implied filter with the name "CDI Conversation Filter"
            // NOTE this temporary solution works hand in hand with
            // com.ibm.ws.cdi.web.WeldConfiguratorHelperFactory.WeldConfigHelper.configureInit()
            // where this empty ConfigItem<IFilterConfig> is created as a place holder.

            // First remove the existing placeholder ConfigItem<IFilterConfig> so we can create a proper FilterConfig for it
            filterMap.remove(filterName);
            filterConfig = createCdiConversationFilter(webDD);
        }
        Map<String, ConfigItem<FilterMapping>> filterMappingMap = configurator.getConfigItemMap("filter-mapping");
        ConfigItem<FilterMapping> existedFilterMapping = filterMappingMap.get(filterName);
        if (existedFilterMapping == null) {
            DispatcherType[] dispatcherTypes = getDispatcherTypes(filterMapping);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "processFilterMappingConfig: existing filter mapping not found");
            } 
            if(defer){
                addFilterMapping(filterConfig, filterMapping.getURLPattern(), filterMapping.getServletName(),
                                 dispatcherTypes, false, index);                
            }
            else{
                addFilterMapping(filterConfig, filterMapping.getURLPattern(), filterMapping.getServletName(),
                                 dispatcherTypes, false); 
            }
            filterMappingMap.put(filterName, createConfigItem(filterMapping));
        } else {
            if ((existedFilterMapping.getSource() == ConfigSource.WEB_XML && configurator.getConfigSource() == ConfigSource.WEB_XML)
                || (existedFilterMapping.getSource() == ConfigSource.WEB_FRAGMENT && configurator.getConfigSource() == ConfigSource.WEB_FRAGMENT)) {

                DispatcherType[] dispatcherTypes = getDispatcherTypes(filterMapping);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "processFilterMappingConfig: existing filter mapping found");
                } 
                addFilterMapping(filterConfig, filterMapping.getURLPattern(), filterMapping.getServletName(),
                                 dispatcherTypes, false); 
            } else if (existedFilterMapping.getSource() == ConfigSource.WEB_XML && configurator.getConfigSource() == ConfigSource.WEB_FRAGMENT) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "filter-mapping for filter {0} is configured in web.xml, the value from web-fragment.xml in {1} is ignored", filterName,
                             configurator.getLibraryURI());
                }
            } else if (existedFilterMapping.getSource() == ConfigSource.WEB_FRAGMENT && configurator.getConfigSource() == ConfigSource.ANNOTATION) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "filter-mapping for filter {0} is configured in web-fragment.xml from {1}, the value from annotation is ignored", filterName,
                             existedFilterMapping.getLibraryURI());
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "processFilterMappingConfig", "webDD [ " + webDD + " ] defer [ " + defer + " ] index [ " + index + " ]");
        }  
    }

    private DispatcherType[] getDispatcherTypes(FilterMapping filterMapping) {
        List<FilterMapping.DispatcherEnum> dispatcherValues = filterMapping.getDispatcherValues();
        if (dispatcherValues == null) {
            return null;
        }
        DispatcherType[] dispatcherTypes = new DispatcherType[dispatcherValues.size()];
        int index = 0;
        for (FilterMapping.DispatcherEnum dispatcherValue : dispatcherValues) {
            DispatcherType dispatcherType = null;
            switch (dispatcherValue) {
                case ASYNC:
                    dispatcherType = DispatcherType.ASYNC;
                    break;
                case ERROR:
                    dispatcherType = DispatcherType.ERROR;
                    break;
                case FORWARD:
                    dispatcherType = DispatcherType.FORWARD;
                    break;
                case INCLUDE:
                    dispatcherType = DispatcherType.INCLUDE;
                    break;
                case REQUEST:
                    dispatcherType = DispatcherType.REQUEST;
                    break;
            }
            dispatcherTypes[index++] = dispatcherType;
        }
        return dispatcherTypes;
    }

    /**
     * Merging Rules :
     * a. Once a servlet configuration exists, the following ones with the same servlet name are ignored
     * b. If init-param/loadOnStartup/multipartConfig/asyncSupported is configured in web.xml, all the values from web-fagment.xml and annotation are ignored
     * b. If init-param/loadOnStartup/multipartConfig/asyncSupported is NOT configured in web.xml,
     * b1. Those values from web-fragment.xml and annotation will be considered
     * b2. If they are configured in different web-fragment.xml, their values should be the same, or will trigger a warning
     */
    private void processServletConfig(DeploymentDescriptor webDD, Servlet servlet) {
        String servletName = servlet.getServletName();
        Map<String, ConfigItem<ServletConfig>> servletMap = configurator.getConfigItemMap("servlet");
        ConfigItem<ServletConfig> existedServlet = servletMap.get(servletName);

        ServletConfig servletConfig = null;
        if (existedServlet == null) {
            String servletId = webDD.getIdForComponent(servlet);
            if (servletId == null) {
                servletId = "ServletGeneratedId" + configurator.generateUniqueId();
            }
            servletConfig = webAppConfiguration.createServletConfig(servletId, servletName);

            configureTargetConfig(servletConfig, servlet.getServletClass(), "Servlet", servlet);

            String jspFile = servlet.getJSPFile();
            if (jspFile != null) {
                servletConfig.setIsJsp(true);
                servletConfig.setFileName(jspFile);
            }
            if (servlet.isSetEnabled()) {
                servletConfig.setEnabled(servlet.isEnabled());
            }

            webAppConfiguration.addServletInfo(servletName, servletConfig);

            servletMap.put(servletName, createConfigItem(servletConfig));

        } else {
            servletConfig = existedServlet.getValue();
        }

        configureInitParams(servletConfig, servlet.getInitParams(), "servlet");

        if (servlet.isSetLoadOnStartup()) {
            Map<String, ConfigItem<Integer>> servletLoadOnStartupMap = configurator.getConfigItemMap("servlet.load-on-startup");

            ConfigItem<Integer> existedLoadOnStartup = servletLoadOnStartupMap.get(servletName);

            if (existedLoadOnStartup == null) {
                servletConfig.setStartUpWeight(servlet.isNullLoadOnStartup() ? null : Integer.valueOf(servlet.getLoadOnStartup()));
                servletLoadOnStartupMap.put(servletName, createConfigItem(Integer.valueOf(servlet.getLoadOnStartup())));
            } else {
                validateDuplicateConfiguration("servlet", "load-on-startup", servlet.isNullLoadOnStartup() ? null : Integer.valueOf(servlet.getLoadOnStartup()),
                                               existedLoadOnStartup);
            }
        }

        if (servlet.getMultipartConfig() != null) {
            Map<String, ConfigItem<MultipartConfig>> servletMultipartConfigMap = configurator.getConfigItemMap("servlet.multipart-config");
            if (existedServlet == null || !servletMultipartConfigMap.containsKey(servletName)) {
                servletConfig.setMultipartConfig(createMultipartConfigElement(servlet.getMultipartConfig()));
                servletMultipartConfigMap.put(servletName, createConfigItem(servlet.getMultipartConfig()));
            } else {
                ConfigItem<MultipartConfig> existedMultipartConfig = servletMultipartConfigMap.get(servletName);
                if (existedMultipartConfig.getSource() == ConfigSource.WEB_FRAGMENT && configurator.getConfigSource() == ConfigSource.WEB_FRAGMENT) {
                    if (!isSameMultipartConfigs(existedMultipartConfig.getValue(), servlet.getMultipartConfig())) {
                        // TODO: What goes here?
                    }
                } else if (existedMultipartConfig.getSource() == ConfigSource.WEB_XML && configurator.getConfigSource() == ConfigSource.WEB_FRAGMENT) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "multipart-config for servlet {0} is configured in web.xml, the value from web-fragment.xml in {1}  is ignored", servletName,
                                 configurator.getLibraryURI());
                    }
                }
            }
        }

        if (servlet.isSetAsyncSupported()) {
            configureAsyncSupported(servletConfig, servlet.isAsyncSupported(), "servlet");
        }
        com.ibm.ws.javaee.dd.common.RunAs runAs = servlet.getRunAs();
        if (runAs != null) {
            servletConfig.setRunAsRole(runAs.getRoleName());
        }
    }

    /**
     * Merging Rules :
     * a. If the url-pattern is configured in web.xml, any other configuration from web-fragment.xml and annotation are ignored
     * b. If no url-pattern is configured in web.xml, those configurations from web-fragment.xml should be additive
     * c. If url-pattern is configured in web-fragment.xml, those configurations from annotation are ignored
     * 
     * @param configContext
     * @param servletMapping
     * @param source
     * @param libraryURI
     * @throws UnableToAdaptException 
     */
    private void processServletMappingConfig(ServletMapping servletMapping) throws UnableToAdaptException {
        String servletName = servletMapping.getServletName();
        Map<String, ConfigItem<List<String>>> servletMappingMap = configurator.getConfigItemMap("servlet-mapping");
        ConfigItem<List<String>> existedServletMapping = servletMappingMap.get(servletName);

        if (existedServletMapping == null) {
            List<String> urlPatterns = servletMapping.getURLPatterns();
            for (String urlPattern : urlPatterns) {
                if (isServletSpecLevel31OrHigher()) {
                    // Strange to 'put' on error cases.
                    String existingName = urlToServletNameMap.put(urlPattern, servletName);
                    if ((existingName != null) && !(existingName.equals(servletName))) {
                        Tr.error(tc,"duplicate.url.pattern.for.servlet.mapping", urlPattern, servletName, existingName);
                        throw new UnableToAdaptException(nls.getFormattedMessage("duplicate.url.pattern.for.servlet.mapping",
                                                                                 new Object[]{urlPattern, servletName, existingName} , 
                                                                                 "servlet-mapping value matches multiple servlets: " + urlPattern));
                    }
                }
                webAppConfiguration.addServletMapping(servletName, urlPattern);
            }
            if (urlPatterns.size() > 0) {
                servletMappingMap.put(servletName, createConfigItem(urlPatterns));
            }
        } else {
            if ((existedServletMapping.getSource() == ConfigSource.WEB_XML && configurator.getConfigSource() == ConfigSource.WEB_XML)
                || (existedServletMapping.getSource() == ConfigSource.WEB_FRAGMENT && configurator.getConfigSource() == ConfigSource.WEB_FRAGMENT)) {
                for (String urlPattern : servletMapping.getURLPatterns()) {
                    if (isServletSpecLevel31OrHigher()) {
                        // Strange to 'put' on error cases.                     
                        String  existingName =  urlToServletNameMap.put(urlPattern,servletName);
                        if ((existingName != null) && !(existingName.equals(servletName))) {
                            Tr.error(tc,"duplicate.url.pattern.for.servlet.mapping", urlPattern, servletName, existingName);
                            throw new UnableToAdaptException( nls.getFormattedMessage("duplicate.url.pattern.for.servlet.mapping",
                                                                                      new Object[]{urlPattern, servletName, existingName} , 
                                                                                      "servlet-mapping value matches multiple servlets: " + urlPattern));
                        }
                    }
                    webAppConfiguration.addServletMapping(servletName, urlPattern);
                }
            } else if (existedServletMapping.getSource() == ConfigSource.WEB_XML && configurator.getConfigSource() == ConfigSource.WEB_FRAGMENT) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "servlet-mapping for servlet " + servletName + " is configured in web.xml, the value from web-fragment.xml in "
                                 + configurator.getLibraryURI()
                                 + " is ignored");
                }
            } else if (existedServletMapping.getSource() == ConfigSource.WEB_FRAGMENT && configurator.getConfigSource() == ConfigSource.ANNOTATION) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "servlet-mapping for servlet " + servletName + " is configured in web-fragment.xml from " + existedServletMapping.getLibraryURI()
                                 + " , the value from annotation is ignored");
                }
            }
        }
    }

    private <T extends ResourceBaseGroup> void processDuplicateJndiReferenceConfig(String referenceElementName, String jndiElementName, String jndiElementValue,
                                                                                   ConfigItem<T> existedConfigItem,
                                                                                   T currentValue,
                                                                                   Set<String> additiveJNDIReferenceNames) {
        if (existedConfigItem.getSource() == ConfigSource.WEB_XML && configurator.getConfigSource() == ConfigSource.WEB_FRAGMENT) {
            if (isServletSpecLevel31OrHigher() || additiveJNDIReferenceNames.contains(jndiElementValue)) {
                existedConfigItem.getValue(InjectionTargetsEditable.class).addInjectionTargets(currentValue.getInjectionTargets());
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "{0}.{1} with value {2}  is configured in web.xml with injection-target, the configurations from web-fragment.xml in {3} is ignored",
                             referenceElementName, jndiElementName,
                             jndiElementValue, configurator.getLibraryURI());
                }
            }
        } else if (existedConfigItem.getSource() == ConfigSource.WEB_FRAGMENT && configurator.getConfigSource() == ConfigSource.WEB_FRAGMENT) {
            if (existedConfigItem.compareValue(currentValue)) {
                existedConfigItem.getValue(InjectionTargetsEditable.class).addInjectionTargets(currentValue.getInjectionTargets());
            } else {
                //Tr.error(tc, "CONFLICT_JNDI_REFERENCE_BETWEEN_WEB_FRAGMENT_XML", referenceElementName, jndiElementValue, jndiElementName, existedConfigItem.getLibraryURI(),
                //         configurator.getLibraryURI());
                configurator.addErrorMessage(nls.getFormattedMessage("CONFLICT_JNDI_REFERENCE_BETWEEN_WEB_FRAGMENT_XML",
                                                                     new Object[] { referenceElementName, jndiElementValue, jndiElementName, existedConfigItem.getLibraryURI(),
                                                                                   configurator.getLibraryURI() },
                                                                     "Conflict JNDI configurations are found in the web-fragment.xml files"));
            }
        }
    }

    //
    // Convenience methods that forward to the configurator
    //

    private <T> ConfigItem<T> createConfigItem(T value) {
        return this.configurator.createConfigItem(value);
    }

    private <T> ConfigItem<T> createConfigItem(T value, MergeComparator<T> comparator) {
        return this.configurator.createConfigItem(value, comparator);
    }

    private <T> void validateDuplicateConfiguration(String parentElementName,
                                                    String elementName,
                                                    T currentValue,
                                                    ConfigItem<T> existedConfigItem) {
        this.configurator.validateDuplicateConfiguration(parentElementName, elementName, currentValue, existedConfigItem);
    }

    private <T> void validateDuplicateKeyValueConfiguration(String parentElementName,
                                                            String keyElementName,
                                                            String keyElementValue,
                                                            String valueElementName,
                                                            T currentValue,
                                                            ConfigItem<T> existedConfigItem) {
        this.configurator.validateDuplicateKeyValueConfiguration(parentElementName,
                                                                 keyElementName, keyElementValue,
                                                                 valueElementName, currentValue, existedConfigItem);
    }
    
    private void validateDuplicateDefaultErrorPageConfiguration(String newLocationValue,
                                                                ConfigItem<String> currentLocationItem) {
        this.configurator.validateDuplicateDefaultErrorPageConfiguration(newLocationValue, currentLocationItem);        
    }
    
    public void processIgnoredMappings(DeploymentDescriptor webDD) throws UnableToAdaptException{
        if(!deferredFilterMappings.isEmpty()){
            for(MappingIndexPair<FilterMapping, Integer> filterPair : deferredFilterMappings){
                processFilterMappingConfig(webDD, filterPair.getMapping(), true, filterPair.getIndex()-filterMappingIndexOffset);
            }
        }
    }
}
