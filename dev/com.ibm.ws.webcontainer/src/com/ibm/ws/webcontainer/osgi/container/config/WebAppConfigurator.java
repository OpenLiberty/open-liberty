/*******************************************************************************
 * Copyright (c) 2011, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.osgi.container.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.annocache.AnnotationsBetaHelper;
import com.ibm.ws.container.service.annotations.WebAnnotations;
import com.ibm.ws.container.service.config.ServletConfigurator;
import com.ibm.ws.container.service.config.ServletConfiguratorHelper;
import com.ibm.ws.container.service.config.WebFragmentInfo;
import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.dd.web.WebFragment;
import com.ibm.ws.javaee.dd.webbnd.WebBnd;
import com.ibm.ws.javaee.dd.webext.WebExt;
import com.ibm.ws.javaee.ddmodel.webext.WebExtComponentImpl;
import com.ibm.ws.resource.ResourceRefConfigFactory;
import com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 * Process web module metadata for a web container.
 * 
 * Web module metadata is obtained from web.xml, from any number of web-fragment.xml,
 * and from annotations.
 */
public class WebAppConfigurator implements ServletConfigurator {

    private static final TraceComponent tc = Tr.register(WebAppConfigurator.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

    private static final TraceNLS nls = TraceNLS.getTraceNLS(WebAppConfigurator.class, "com.ibm.ws.webcontainer.resources.LShimMessages");
    
    // Main state ... process configuration relative to a module container and a cache ...
    private final Container moduleContainer;
    private final NonPersistentCache overlayCache;

    // Main state ... a list of helpers used to configure subsets of configuration data.
    // A main step of configuration is to iterate across the parts of the configuration
    // data then iterate across each of the configuration helpers on each of the parts.
    // Each helper has several entry points (configureInit, configureFromWebApp,
    // configureFromWebFragment, configureFromAnnotations, configureDefaults, configureWebBnd,
    // configureWebExt, finish).  Generally, for each step of processing, a helper entry
    // point is selected and is performed using all of the helpers before moving to
    // processing which uses a different entry point.
    private final List<ServletConfiguratorHelper> configHelpers;

    // Main state ... allow processing to generate error messages.  These are
    // accumulated during processing.  If any are generated, at the conclusion
    // of processing, an exception is thrown.
    private final List<String> errorMessages;

    // Configuration data ... data from web.xml, ibm-web-bnd.xml (or xmi), and ibm-web-bnd.xml (or xmi). 
    private final WebApp webApp;
    private final WebBnd webBnd;
    private final WebExt webExt;

    // The servlet version and metadata complete setting.  Obtained from
    // web.xml, or defaulted according to the web container feature version.
    private final VersionInfo webVersionInfo;

    // Annotations ...

    private WebAnnotations webAnnotations;

    private void setWebAnnotations() throws UnableToAdaptException {
        webAnnotations = AnnotationsBetaHelper.getWebAnnotations(moduleContainer);
    }

    @Override
    public WebAnnotations getWebAnnotations() {
        return webAnnotations;
    }

    private void openInfoStore() throws UnableToAdaptException {
        webAnnotations.openInfoStore();
    }

    private void closeInfoStore() throws UnableToAdaptException {
        webAnnotations.closeInfoStore();
    }

    private List<WebFragmentInfo> getOrderedItems() throws UnableToAdaptException {
        return ( webAnnotations.getOrderedItems() );
    }

    // Transient state ... 

    // Widget for unique ID generation.  Used for just this one module, hence
    // a part of the transient state.
    //
    // A static would ensure unique ID across multiple configurations, but would
    // add an undesirable synchronization point.
    //
    // Contrast with the static 'uniqueId' of WebAppConfiguratorHelper.
    // One or the other of these unique ID fields is probably wrong.    
    private final AtomicLong uniqueId = new AtomicLong(1l);
    
    // Generic table of tables.  Elements are either mappings themselves,
    // or are sets.  Attributes are set during processing of web.xml and
    // web-fragment.xml, but then are cleared, then set again during
    // the processing of ibm-web-bnd.xml and ibm-web-ext.xml.
    private final Map<String, Object> attributes;

    // The type of the part of the web module which is being processed.
    // One of WEB, WEB_FRAGMENT, or ANNOTATION.
    private ConfigSource currentSource;

    // The part of the web module which is being processed; variously,
    // "WEB-INF/web.xml", or "WEB-INF/lib/FRAGMENT_JAR.jar", 
    private String currentLibraryURI;

    // The metadata-complete setting of the part of the web module which
    // is being processed.  (WEB and WEB_FRAGMENT each have their own
    // metadata-complete setting.)
    private boolean currentMetadataComplete;

    // Listener interface support ...

    /** The Servlet 3.0 HTTP listener classes. */
    private final Class<?>[] SERVLET30_LISTENER_INTERFACES = {
                javax.servlet.ServletContextListener.class,
                javax.servlet.ServletContextAttributeListener.class,
                javax.servlet.ServletRequestListener.class,
                javax.servlet.ServletRequestAttributeListener.class,
                javax.servlet.http.HttpSessionListener.class,
                javax.servlet.http.HttpSessionAttributeListener.class
    };

    /** The Servlet 3.1 HTTP ID listener class name. */
    private static final String HTTP_ID_LISTENER_CLASS_NAME = "javax.servlet.http.HttpSessionIdListener"; 

    /**
     * Obtain the list of enabled listener interfaces.  The list contents depends on servlet 3.1 enablement.
     * 
     * @return The list of enabled listener interfaces.
     */
    private List<Class<?>> getListenerInterfaces() {
        List<Class<?>> listenerInterfaces = 
            new ArrayList<Class<?>>(Arrays.asList(SERVLET30_LISTENER_INTERFACES));            

        // Condition the HTTP ID Listener on Servlet 3.1 enablement.
    
        if (com.ibm.ws.webcontainer.osgi.WebContainer.getServletContainerSpecLevel() <
            com.ibm.ws.webcontainer.osgi.WebContainer.SPEC_LEVEL_31) {
            return listenerInterfaces;
        }

        // Use reflection for referenced Servlet 3.1 classes.  Those won't load unless
        // Servlet 3.1 is enabled.
            
        Class<?> httpIDListenerInterface;           
        
        try {
            httpIDListenerInterface = Class.forName(HTTP_ID_LISTENER_CLASS_NAME);
        } catch (ClassNotFoundException e) {
            httpIDListenerInterface = null;
        }
            
        if (httpIDListenerInterface != null) {
            listenerInterfaces.add(httpIDListenerInterface);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Servlet 3.1 is enabled but failed to find the HTTP ID Listener class [ " + HTTP_ID_LISTENER_CLASS_NAME + " ]");
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Supported listener interfaces: " + listenerInterfaces);
        }
               
        return listenerInterfaces;                
    }
    
    ServletConfiguratorHelper webAppHelper;
            
    /**
     * First step of the configurator life-cycle.  Create the web app configurator
     * and prepare the collection of helpers.p
     * 
     * The next step is to complete the collection of helpers.  See {@link #addHelper(ServletConfiguratorHelper)}
     * and {@link WebAppConfigurationAdapter#adapt(Container, OverlayContainer, ArtifactContainer, Container)}.
     * 
     * @param moduleContainer The container of the module which is being configured.
     * @param overlayCache Cache available for the configuration processing.
     * @param resourceRefConfigFactory Factory for resource reference configuration items.
     * 
     * @throws UnableToAdaptException Thrown in case of an error creating the configurator.
     */
    public WebAppConfigurator(Container moduleContainer,
                              NonPersistentCache overlayCache,
                              ResourceRefConfigFactory resourceRefConfigFactory) throws UnableToAdaptException {

        this.moduleContainer = moduleContainer;
        this.overlayCache = overlayCache;

        this.configHelpers = new ArrayList<ServletConfiguratorHelper>();        

        this.webApp = moduleContainer.adapt(WebApp.class);
        this.webBnd = moduleContainer.adapt(WebBnd.class);
        this.webExt = moduleContainer.adapt(WebExt.class);

        this.webVersionInfo = new VersionInfo(this.webApp);

        // Transition to transient state ...

        this.errorMessages = new ArrayList<String>();

        this.attributes = new HashMap<String, Object>();

        // this.currentSource = null;
        // this.currentLibraryURI = null;
        // this.currentMetadataComplete = false;
    }
    
    /**
     * Configure the WebApp helper factory
     * @param webAppConfiguratorHelperFactory The factory to be used
     * @param resourceRefConfigFactory
     */
    public void configureWebAppHelperFactory(WebAppConfiguratorHelperFactory webAppConfiguratorHelperFactory, ResourceRefConfigFactory resourceRefConfigFactory) {
        webAppHelper = webAppConfiguratorHelperFactory.createWebAppConfiguratorHelper(this, resourceRefConfigFactory, getListenerInterfaces());        
        this.configHelpers.add(webAppHelper);
    }
    
    public String getContextRootFromServerConfig() {
        if (webExt == null) {
            return null;
        }
        
        // if the webExt is from the server config,
        // it will be of type WebExtComponentImpl
        if ( !(webExt instanceof WebExtComponentImpl) ) {
            return null;
        }
        
        String contextRoot = webExt.getContextRoot();

        // ensure context root is prefixed with "/"
        if ((contextRoot != null) && (contextRoot.length() > 0) && (contextRoot.charAt(0) != '/')) {
            contextRoot = "/" + contextRoot;
        }

        return contextRoot;
    }
    
    /**
     * Helper for obtaining version information from a web application.
     * 
     * Version information is the integer version ID and the metadata complete
     * setting.
     */
    private static class VersionInfo {
        /**
         * The version ID from the web application.  Defaulted according to the
         * current supported servlet specification level if no web application is
         * available.
         */
        public final int versionId;
        
        /**
         * The metadata-complete setting from the web application.  Set to true
         * if the web application is at version 2.4 or lower.  Set to true if
         * no web application is available.  Set to false if unset in the web
         * application.  Otherwise, set to the value obtained from the web application.
         */
        public final boolean isMetadataComplete;

        /**
         * Obtain version information from a web application.  Assign defaults
         * if the web application is null.  Version information consists of the
         * schema level of the web application and the metadata complete setting
         * of the web application.
         * 
         * @param webApp The web application for which to obtain version information.
         */
        public VersionInfo(WebApp webApp) {
            if (webApp == null) {
                this.versionId = WebAppConfiguratorHelper.getDefaultVersionId();
                this.isMetadataComplete = false;

            } else {
                this.versionId = WebAppConfiguratorHelper.getVersionId(webApp.getVersion());                
                if (this.versionId <= 24) {
                    this.isMetadataComplete = true;
                } else {
                    this.isMetadataComplete = webApp.isSetMetadataComplete() && webApp.isMetadataComplete();
                }
            }
        }
    }

    /**
     * Second step of running the configurator.  Following construction,
     * helpers must be put into the configurator.  After setting helpers,
     * {@link #configure()} may be invoked to perform the main configuration
     * processing.
     * 
     * Helpers are invoked in the same order that they were added to the
     * configurator.
     * 
     * @param helper The helper to add to the configurator.
     */
    public void addHelper(ServletConfiguratorHelper helper) {
        this.configHelpers.add(helper);
    }

    @Override
    public int getServletVersion() {
        return this.webVersionInfo.versionId;
    }

    @Override
    public boolean isMetadataComplete() {
        return this.webVersionInfo.isMetadataComplete;
    }

    @Override
    public Container getModuleContainer() {
        return this.moduleContainer;
    }

    @Override
    public Object getFromModuleCache(Class<?> owner) {
        return this.overlayCache.getFromCache(owner);
    }

    @Override
    public void addToModuleCache(Class<?> owner, Object data) {
        this.overlayCache.addToCache(owner, data);
    }

    @Override
    public ConfigSource getConfigSource() {
        return this.currentSource;
    }

    @Override
    public String getLibraryURI() {
        return this.currentLibraryURI;
    }

    @Override
    public boolean getMetadataCompleted() {
        return this.currentMetadataComplete;
    }

    @Override
    public long generateUniqueId() {
        return this.uniqueId.getAndIncrement();
    }

    @Override
    public void addErrorMessage(String errorMessage) {
        // This should probably limit the number of messages which are added.
        // If thousands of messages are added, the placement of the message
        // text into a single string buffer could overflow.
        this.errorMessages.add(errorMessage);
    }

    public boolean haveAnyErrorMessages() {
        return (!errorMessages.isEmpty());
    }
    
    public String getErrorMessageText() {
        String separatorString = System.getProperty("line.separator");
        
        StringBuilder errorMessageBuffer = new StringBuilder();            
        for (String errorMessage : errorMessages) {
            errorMessageBuffer.append(errorMessage).append(separatorString);
        }
        
        return errorMessageBuffer.toString();
    }
    
    /**
     * Main configuration API:
     * 
     * Do an initialization step, then process web.xml, then process fragments,
     * then do a defaulting step, then, if available, process the web binding and
     * the web extension, then do a finishing step. 
     * 
     * @throws UnableToAdaptException
     */
    public void configure() throws UnableToAdaptException {
        boolean webIsMetadataComplete = isMetadataComplete();
        
        // locate annotations

        // Collapse all of the prior scanner calls into a single step:
        // The resulting merge has obtained target information for all of the
        // target locations, with precedence per the location ordering.
        //
        // What particular targets are obtained is a curious business, as
        // the class selection uses the names of classes in the included
        // location, but obtains the information for each of those classes
        // using the entire class search space.
        
        // Maybe, move this inside the 'isMetadataComplete' test?
        // The web annotations don't seem to be used except when
        // metadata-complete is false.

        setWebAnnotations(); // throws UnableToAdaptException

        if ( !webIsMetadataComplete ) {
            openInfoStore(); // throws UnableToAdaptException
        }
        
        try {

            for (ServletConfiguratorHelper configHelper : configHelpers) {
                configHelper.configureInit();
            }

            //process web.xml
            if (webApp != null) {
                this.currentLibraryURI = WebApp.DD_NAME;
                this.currentSource = ConfigSource.WEB_XML;
                this.currentMetadataComplete = isMetadataComplete();

                for (ServletConfiguratorHelper configHelper : configHelpers) {
                    configHelper.configureFromWebApp(webApp);
                }
            }
            
            //process web-fragment.xml
            if (!webIsMetadataComplete) {
                for ( WebFragmentInfo webFragmentItem : getOrderedItems() ) {
                    WebFragment webFragment = webFragmentItem.getWebFragment();
                    this.currentLibraryURI = webFragmentItem.getLibraryURI();
                    this.currentSource = ConfigSource.WEB_FRAGMENT;
                    this.currentMetadataComplete = webFragment != null && webFragment.isMetadataComplete();
    
                    if (webFragment != null) {
                        for (ServletConfiguratorHelper configHelper : configHelpers) {
                            configHelper.configureFromWebFragment(webFragmentItem);
                        }
                    }
                    
                    if (!currentMetadataComplete) {
                        for (ServletConfiguratorHelper configHelper : configHelpers) {
                            configHelper.configureFromAnnotations(webFragmentItem);
                        }
                    }
                    
                    ((WebAppConfiguratorHelper) webAppHelper).processIgnoredMappings(webApp);
                }
            }

            for (ServletConfiguratorHelper configHelper : configHelpers) {
                configHelper.configureDefaults();
            }
            
        } finally {
            if (!webIsMetadataComplete) {
                closeInfoStore(); // throws UnableToAdaptException
            }
        }

        clearContext();

        if (webBnd != null) {
            for (ServletConfiguratorHelper configHelper : configHelpers) {
                configHelper.configureWebBnd(webBnd);
            }
        }

        if (webExt != null) {
            for (ServletConfiguratorHelper configHelper : configHelpers) {
                configHelper.configureWebExt(webExt);
            }
        }

        for (ServletConfiguratorHelper configHelper : configHelpers) {
            configHelper.finish();
        }
        
        if (this.haveAnyErrorMessages()) {
            throw new UnableToAdaptException(this.getErrorMessageText());
        }
    }
    
    /**
     * Validate configuration items which occupy the same locations in two configurations.
     * 
     * The parameters tell the location of the items, telling the element name of the parent
     * configuration item and the element name of the configuration item.
     * 
     * Comparison is not entirely symmetric: A prior configuration, which supplies a prior
     * value, is compared with a new value.
     * 
     * The prior configuration item must be supplied: Do not use this code for cases where
     * a new configuration item does not find a match in the prior configuration.
     * 
     * Both the value of the prior configuration item and the new value may be null.
     * 
     * The tests consider the prior and new values, and consider the locations of the
     * prior and new values.
     * 
     * Results are generated as side effects, either, with a null effect (a pass), with an
     * output to logs with DEBUG enablement (a soft failure), or with an error added to the
     * configurator (a hard failure).
     * 
     * A passing is when the values are the same.
     * 
     * A soft failure is when the values are different, the prior value is from web.xml,
     * and the new value is from web-fragment.xml.
     * 
     * A hard failure is when the values are different, and both the prior and new values
     * are from web-fragment.xml.
     * 
     * The case of the prior value being obtained from web-fragment.xml and the new
     * value is from web.xml is not possible, since values are considered from web.xml
     * before being considered from web-fragment.xml.
     * 
     * @param parentElementName The name of the element which contains the value which is being tested.
     * @param elementName The name of the element which is being tested.
     * @param newValue The new element value.  The value may be null.
     * @param priorConfigItem A prior configuration item which holds a prior element value.  The
     *     prior element value may be null.
     */
    public <T> void validateDuplicateConfiguration(String parentElementName, String elementName,
                                                   T newValue,
                                                   ConfigItem<T> priorConfigItem) {

        T priorValue = priorConfigItem.getValue();

        if (priorValue == null) {
            if (newValue == null) {
                return; // Same null value; ignore
            } else {
                // Different values: null vs non-null; check the source
            }
        } else if (newValue == null) {
            // Different values: non-null vs null; check the source
        } else if (priorConfigItem.compareValue(newValue)) {
            return; // Same non-null value; ignore
        }

        ConfigSource priorSource = priorConfigItem.getSource();
        ConfigSource newSource = getConfigSource();
        
        if (priorSource == ConfigSource.WEB_XML) {
            if (newSource == ConfigSource.WEB_FRAGMENT) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "{0}.{1} with value {2} is configured in web.xml, the value {3} from web-fragment.xml in {4} is ignored",
                             parentElementName, elementName,
                             priorConfigItem.getValue(), newValue, getLibraryURI());
                }
            } else {
                // web.xml vs web.xml: Should not be possible; ignore.
            }

        } else if (priorSource == ConfigSource.WEB_FRAGMENT) {
            // If the prior source is a web-fragment.xml, we have started
            // processing fragments, and any subsequent source must also
            // be a fragment.

            String priorLibraryURI = priorConfigItem.getLibraryURI();
            String newLibraryURI = getLibraryURI();
            
            if (!priorLibraryURI.equals(newLibraryURI)) {
                errorMessages.add(nls.getFormattedMessage("CONFLICT_SINGLE_VALUE_CONFIG_BETWEEN_WEB_FRAGMENT_XML",
                                                          new Object[] { parentElementName, elementName,
                                                                         priorValue, priorLibraryURI,
                                                                         newValue, newLibraryURI }, 
                                "Conflict configurations are found in the web-fragment.xml files"));
            } else {
                // Same library; should not be possible; ignore           
            }
        }
    }

    /**
     * Validate configuration items which are elements of keyed collections in two configurations. 
     *
     * Nearly the same as {@link #validateDuplicateConfiguration(String, String, Object, ConfigItem)},
     * but with a different parameterization of the element location.
     * 
     * @param parentElementName The name of the element which contains the
     *     value which is being tested.
     * @param keyElementName The element supplying the key value which was used to
     *     match the configuration items.
     * @param keyElementValue The value of the key element for the configuration items.
     * @param valueElementName The name of the element containing the value which is being tested.
     * @param newValue The new element value.  The value may be null.
     * @param priorConfigItem A prior configuration item which holds a prior element value.  The
     *     prior element value may be null.
     */
    public <T> void validateDuplicateKeyValueConfiguration(String parentElementName,
                                                           String keyElementName,
                                                           String keyElementValue,
                                                           String valueElementName,
                                                           T newValue,
                                                           ConfigItem<T> priorConfigItem) {
        
        T priorValue = priorConfigItem.getValue();

        if (priorValue == null) {
            if (newValue == null) {
                return; // Same null value; ignore
            } else {
                // Different values: null vs non-null; check the source
            }
        } else if (newValue == null) {
            // Different values: non-null vs null; check the source
        } else if (priorConfigItem.compareValue(newValue)) {
            return; // Same non-null value; ignore
        }

        ConfigSource priorSource = priorConfigItem.getSource();
        ConfigSource newSource = getConfigSource();
        
        if (priorSource == ConfigSource.WEB_XML) {
            if (newSource == ConfigSource.WEB_FRAGMENT) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "{0} {1} == {2} {3} == {4} is configured in web.xml, the value {5} from web-fragment.xml in {6} is ignored",
                             parentElementName,
                             keyElementName, keyElementValue,
                             valueElementName, priorValue,
                             newValue,
                             getLibraryURI());
                }                
            } else {
                // web.xml vs web.xml: Should not be possible; ignore.
            }
            
        } else if (priorSource == ConfigSource.WEB_FRAGMENT) {
            // If the prior source is a web-fragment.xml, we have started
            // processing fragments, and any subsequent source must also
            // be a fragment.

            String priorLibraryURI = priorConfigItem.getLibraryURI();
            String newLibraryURI = getLibraryURI();
            
            if (!priorLibraryURI.equals(newLibraryURI)) {
                errorMessages.add(nls.getFormattedMessage("CONFLICT_KEY_VALUE_CONFIG_BETWEEN_WEB_FRAGMENT_XML",
                                                          new Object[] { valueElementName, parentElementName,
                                                                         keyElementName, keyElementValue,
                                                                         priorValue, priorLibraryURI, newValue, newLibraryURI },
                                                        "Conflict configurations are found in the web-fragment.xml files"));
            } else {
                // Same library; should not be possible; ignore
            }
        }
    }
    
    // Default error-page validation modeled after
    // 'validateDuplicateKeyValueConfiguration', which ignores conflicts
    // within a single web.xml or a single web-fragment.xml.
    //
    // The presumption is that these conflicts are prevented by schema
    // validation.

    /**
     * Validate default error page configuration items.
     *
     * Nearly the same as {@link #validateDuplicateConfiguration(String, String, Object, ConfigItem)},
     * but with no location parameters: Default error page configuration items are in a fixed
     * location.
     * 
     * @param newLocation The default error page location.  May be null.
     * @param priorLocationItem The configuration item of the prior default error page location.
     *   The location value may be null. 
     */    
    public void validateDuplicateDefaultErrorPageConfiguration(String newLocation, ConfigItem<String> priorLocationItem) {
       
       String priorLocation = priorLocationItem.getValue();

       if (priorLocation == null) {
           if (newLocation == null) {
               return; // Same null Location; ignore
           } else {
               // Different Locations: null vs non-null; check the source
           }
       } else if (newLocation == null) {
           // Different Locations: non-null vs null; check the source
       } else if (priorLocation.equals(newLocation)) {
           return; // Same non-null Location; ignore
       }

       ConfigSource priorSource = priorLocationItem.getSource();
       ConfigSource newSource = getConfigSource();
       
       if (priorSource == ConfigSource.WEB_XML) {
           if (newSource == ConfigSource.WEB_FRAGMENT) {
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                   Tr.debug(tc, "Default error page with location {0} in web.xml overrides default error page with location {1} in web-fragment.xml in {2}.",
                            priorLocation, newLocation, getLibraryURI());                   
               }                
           } else {
               // web.xml vs web.xml: Should not be possible; ignore.
           }
           
       } else if (priorSource == ConfigSource.WEB_FRAGMENT) {
           // If the prior source is a web-fragment.xml, we have started
           // processing fragments, and any subsequent source must also
           // be a fragment.

           String priorLibraryURI = priorLocationItem.getLibraryURI();
           String newLibraryURI = getLibraryURI();
           
           if (!priorLibraryURI.equals(newLibraryURI)) {
               errorMessages.add(nls.getFormattedMessage("CONFLICT_DEFAULT_ERROR_PAGE_WEB_FRAGMENT_XML",
                                                         new Object[] { priorLocation, priorLibraryURI, newLocation, newLibraryURI },
                               "Multiple default error pages with different locations in one or more web-fragment.xml."));               
           } else {
               // Same library; should not be possible; ignore
           }
       }
    }

    /**
     * Obtain an attribute value as a mapping.  Create and return a new empty mapping
     * if one is not yet present.
     * 
     * Once obtained as a mapping, the attribute value must always be obtained
     * as a mapping.  A subsequent attempt to obtain the value as a set will fail
     * with a class cast exception.
     * 
     * @param key The key of the attribute value.
     * 
     * @return The attribute value as a mapping.
     */
    @SuppressWarnings("unchecked")
    public <T> Map<String, ConfigItem<T>> getConfigItemMap(String key) {
        Map<String, ConfigItem<T>> configItemMap = (Map<String, ConfigItem<T>>) attributes.get(key);
        if (configItemMap == null) {
            configItemMap = new HashMap<String, ConfigItem<T>>();
            attributes.put(key, configItemMap);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "ConfigContext create map instance for {0}", key);
            }
        }
        return configItemMap;
    }

    /**
     * Obtain an attribute value as a sset.  Create and return a new empty set
     * if one is not yet present.
     * 
     * Once obtained as a set, the attribute value must always be obtained
     * as a set.  A subsequent attempt to obtain the value as a mapping
     * will fail with a class cast exception.
     *
     * @param key The key of the attribute value.
     * 
     * @return The attribute value as a set.
     */    
    @SuppressWarnings("unchecked")
    public <T> Set<T> getContextSet(String key) {
        Set<T> set = (Set<T>) attributes.get(key);
        if (set == null) {
            set = new HashSet<T>();
            attributes.put(key, set);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "ConfigContext create set instance for {0}", key);
            }
        }
        return set;
    }

    /**
     * Remove all attributes.
     */
    private void clearContext() {
        attributes.clear();
    }

    /**
     * Create a configuration item using the current source and the default
     * (equals) comparator.
     * 
     * See {@link #createConfigItem(Object, MergeComparator)}.
     * 
     * @param value The value to place in the configuration item.  A null value
     *     may be provided.
     * 
     * @return The new configuration item.
     */
    public <T> ConfigItem<T> createConfigItem(T value) {
        return createConfigItem(value, null); // Null means use Object.compare()
    }

    /**
     * Create a configuration item using the current source.
     *
     * See {@link #getConfigSource()} and {@link #getLibraryURI()}.
     * 
     * @param value The value to place in the configuration item.  A null value
     *     may be provided.
     * @param comparator The comparator to be used for the configuration item.
     *     Null to select the default comparator.
     * 
     * @return The new configuration item.
     */    
    public <T> ConfigItem<T> createConfigItem(T value, MergeComparator<T> comparator) {
        return new ConfigItemImpl<T>(value, getConfigSource(), getLibraryURI(), comparator);
    }

    /**
     * Standard generic configuration item implementation.
     *
     * @param <I> The type of the value of the configuration item.
     */
    private static class ConfigItemImpl<I> implements ConfigItem<I> {
        /** The value held by this configuration item.  May be null. */
        private final I value;
        
        /** The source (web, web-fragment, or annotations) of this configuration item. */
        private final ConfigSource source;

        /**
         * The library URI of this configuration item.  Set to WEB-INF/web.xml when
         * the source is web.xml.
         */
        private final String libraryURI;

        /**
         * The comparator to be used by this configuration item.
         * 
         * Specific case of configuration require comparators which are not
         * all the same for the same held type.  The comparator field provides
         * for those cases.
         *
         * When null, default to compare values using {@link Object#equals(Object)}.
         */
        private final MergeComparator<I> comparator;
        
        //

        /**
         * Create a new configuration item encapsulating a value and a source
         * of the value.  The value may be null.
         * 
         * @param value The value of the configuration item.
         * @param source The source of the configuration item.
         * @param libraryURI The library URI of the source of the configuration item.
         * @param comparator The comparator to be used with the value.
         */
        public ConfigItemImpl(I value, ConfigSource source, String libraryURI, MergeComparator<I> comparator) {
            this.value = value;
            this.source = source;
            this.libraryURI = libraryURI;
            this.comparator = comparator;
        }

        public I getValue() {
            return value;
        }

        public ConfigSource getSource() {
            return source;
        }

        public String getLibraryURI() {
            return libraryURI;
        }

        /**
         * Compare the held value with another value.  Use
         * the comparator which was set during item construction.
         * 
         * A null comparator means compare using {@link Object#equals}
         * invoked on the held item.
         * 
         * Even if a comparator is set, a null value never compares
         * with a non-null value and two null or identical values
         * always compare.  Do not use the comparator for these cases.
         * 
         * @param otherValue The other value which is to be compared.
         * 
         * @return True if the values compare.  False if the values do
         *     not compare.
         */
        public boolean compareValue(I otherValue) {
            if (value == otherValue) {
                return true; // Always true, regardless of the comparator.
            } else if ((value == null) || (otherValue == null)) {
                return false; // Always false, regardless of the comparator.
            } else {
                if (comparator == null) {
                    return value.equals(otherValue); // Default to use Object.equals.
                } else {
                    // Two non-identical, non-null values: Compare using the comparator.
                    return comparator.compare(value, otherValue);
                }
            }
        }
        
        /**
         * Answer the held value as a particular type.
         * 
         * This default implementation simply casts the value to the
         * requested type.  See {@link Class#cast(Object)}.  Accordingly,
         * the default implementation will throw a ClassCastException
         * if the value is not of the requested type.
         * 
         * This default implementation provides no immediate advantage
         * over using direct conversion.  However, changing from the
         * direct java conversion to this method enables particular
         * configuration item types to provide additional conversion
         * steps.
         * 
         * @return The held value converted to the requested type.
         */
        public <T> T getValue(Class<T> cls) {
            return cls.cast(value);
        }
    }
}
