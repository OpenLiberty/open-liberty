/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config13.sources;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.config.xml.ConfigVariables;
import com.ibm.ws.microprofile.config.interfaces.ConfigException;
import com.ibm.ws.microprofile.config.interfaces.ConfigStartException;
import com.ibm.ws.microprofile.config13.interfaces.Config13Constants;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.application.Application;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

/**
 * Assorted osgi based utility methods
 */
public class OSGiConfigUtils {

    private static final TraceComponent tc = Tr.register(OSGiConfigUtils.class);
    /** Specifies the Factory PID attribute name in the Configuration. This will be used when searching the config. */
    public static final String CFG_SERVICE_FACTORY_PID = "service.factoryPid";
    /** Specifies the AppProperties value for a Factory PID sought in the Configuration */
    public static final String CFG_APP_PROPERTIES = "com.ibm.ws.appconfig.appProperties";
    /** Specifies the Parent PID attribute name in the Configuration. This will be used when searching the config. */
    public static final String CFG_CONFIG_PARENT_PID = "config.parentPID";
    /** Specifies the nested AppProperties Property value for a Factory PID sought in the Configuration */
    public static final String CFG_APP_PROPERTIES_PROPERTY = "com.ibm.ws.appconfig.appProperties.property";

    /**
     * Get the j2ee name of the application. If the ComponentMetaData is available on the thread then that can be used, otherwise fallback
     * to asking the CDIService for the name ... the CDI context ID is the same as the j2ee name.
     *
     * @param bundleContext The bundle context to use when looking up the CDIService
     * @return the application name
     */
    public static String getApplicationName(BundleContext bundleContext) {
        String applicationName = null;
        if (FrameworkState.isValid()) {
            ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();

            if (cmd == null) {
                //if the component metadata is null then we're probably running in the CDI startup sequence so try asking CDI for the application
                applicationName = getCDIAppName(bundleContext);
            } else {
                applicationName = cmd.getJ2EEName().getApplication();
            }

            if (applicationName == null) {
                if (cmd == null) {
                    throw new ConfigStartException(Tr.formatMessage(tc, "no.application.name.CWMCG0201E"));
                } else {
                    throw new ConfigException(Tr.formatMessage(tc, "no.application.name.CWMCG0201E"));
                }
            }
        }
        return applicationName;

    }

    /**
     * Get the OSGi bundle context for the given class
     *
     * @param clazz the class to find the context for
     * @return the bundle context
     */
    public static BundleContext getBundleContext(Class<?> clazz) {
        BundleContext context = null; //we'll return null if not running inside an OSGi framework (e.g. unit test)
        if (FrameworkState.isValid()) {
            Bundle bundle = FrameworkUtil.getBundle(clazz);

            if (bundle != null) {
                context = bundle.getBundleContext();
            }
        }
        return context;
    }

    /**
     * Get the OSGi ConfigurationAdmin service
     *
     * @param bundleContext the bundle context to use to find the service
     * @return the admin service
     */
    public static ConfigurationAdmin getConfigurationAdmin(BundleContext bundleContext) {
        return getService(bundleContext, ConfigurationAdmin.class);
    }

    /**
     * Get the CDIService if available
     *
     * @param bundleContext the context to use to find the CDIService
     * @return the CDIService or null
     */
    public static CDIService getCDIService(BundleContext bundleContext) {
        return getService(bundleContext, CDIService.class);
    }

    /**
     * Get the Config Variables service if available
     *
     * @param bundleContext the context to use to find the ConfigVariables service
     * @return the ConfigVariables service or null
     */
    public static ConfigVariables getConfigVariables(BundleContext bundleContext) {
        return getService(bundleContext, ConfigVariables.class);
    }

    /**
     * Find a service of the given type
     *
     * @param bundleContext The context to use to find the service
     * @param serviceClass The class of the required service
     * @return the service instance or null
     */
    public static <T> T getService(BundleContext bundleContext, Class<T> serviceClass) {
        T service = null;
        if (FrameworkState.isValid()) {
            ServiceReference<T> ref = bundleContext.getServiceReference(serviceClass);

            if (ref != null) {
                service = bundleContext.getService(ref);
            }
        }
        return service;
    }

    /**
     * Get the Application ServiceReferences which has the given name, or null if not found
     *
     * @param bundleContext The context to use to find the application service references
     * @param applicationName The application name to look for
     * @return A ServiceReference for the given application
     */
    public static ServiceReference<Application> getApplicationServiceRef(BundleContext bundleContext, String applicationName) {
        ServiceReference<Application> appRef = null;

        if (FrameworkState.isValid()) {
            Collection<ServiceReference<Application>> appRefs;
            try {
                appRefs = bundleContext.getServiceReferences(Application.class,
                                                             FilterUtils.createPropertyFilter("name", applicationName));
            } catch (InvalidSyntaxException e) {
                throw new ConfigException(e);
            }

            if (appRefs != null && appRefs.size() > 0) {
                if (appRefs.size() > 1) {
                    throw new ConfigException(Tr.formatMessage(tc, "duplicate.application.name.CWMCG0202E", applicationName));
                }
                appRef = appRefs.iterator().next();
            }
        }

        return appRef;
    };

    /**
     * Get the internal OSGi identifier for the Application with the given name
     *
     * @param bundleContext The context to use to find the Application reference
     * @param applicationName The application name to look for
     * @return The application pid
     */
    public static String getApplicationPID(BundleContext bundleContext, String applicationName) {
        String applicationPID = null;

        if (FrameworkState.isValid()) {
            ServiceReference<?> appRef = getApplicationServiceRef(bundleContext, applicationName);
            if (appRef != null) {
                //not actually sure what the difference is between service.pid and ibm.extends.source.pid but this is the way it is done everywhere else!
                applicationPID = (String) appRef.getProperty(Constants.SERVICE_PID);
                String sourcePid = (String) appRef.getProperty("ibm.extends.source.pid");
                if (sourcePid != null) {
                    applicationPID = sourcePid;
                }
            }
        }
        return applicationPID;

    }

    /**
     * Get the current application name using the CDIService. During CDI startup, the CDIService knows which application it is currently working with so we can ask it!
     *
     * @param bundleContext the context to use to find the CDIService
     * @return The application name
     */
    public static String getCDIAppName(BundleContext bundleContext) {
        String appName = null;
        if (FrameworkState.isValid()) {
            // Get the CDIService
            CDIService cdiService = getCDIService(bundleContext);
            if (cdiService != null) {
                appName = cdiService.getCurrentApplicationContextID();
            }
        }
        return appName;
    }

    /**
     * Get the filter string which will extract <appProperties> elements for the given application
     *
     * @param applicationPid The pid of the application in the config
     * @return A filter string to be used by the OSGi ConfigurationAdmin service
     */
    public static String getApplicationPropertiesConfigFilter(String applicationPid) {
        String applicationConfigFilter = null;

        StringBuilder applicationFilter = new StringBuilder(200);
        applicationFilter.append("(&");
        applicationFilter.append(FilterUtils.createPropertyFilter(CFG_SERVICE_FACTORY_PID, CFG_APP_PROPERTIES));
        applicationFilter.append(FilterUtils.createPropertyFilter(CFG_CONFIG_PARENT_PID, applicationPid));
        applicationFilter.append(')');

        applicationConfigFilter = applicationFilter.toString();

        return applicationConfigFilter;
    }

    /**
     * Get the filter string which will extract <property> elements nested inside a given <appProperties> element
     *
     * @param applicationPid The pid of the appProperties element in the config
     * @return A filter string to be used by the OSGi ConfigurationAdmin service
     */
    public static String getApplicationPropertiesPropertyConfigFilter(String applicationPropertyPid) {
        String applicationConfigFilter = null;

        StringBuilder applicationFilter = new StringBuilder(200);
        applicationFilter.append("(&");
        applicationFilter.append(FilterUtils.createPropertyFilter(CFG_SERVICE_FACTORY_PID, CFG_APP_PROPERTIES_PROPERTY));
        applicationFilter.append(FilterUtils.createPropertyFilter(CFG_CONFIG_PARENT_PID, applicationPropertyPid));
        applicationFilter.append(')');

        applicationConfigFilter = applicationFilter.toString();

        return applicationConfigFilter;
    }

    /**
     * Get the Configuration object which represents a <appProperties> element in the server.xml for a given application
     *
     * @param bundleContext The context to use in looking up OSGi service references
     * @param applicationName The application name to look for
     * @return The Configuration instance
     */
    public static SortedSet<Configuration> getConfigurations(BundleContext bundleContext, String applicationName) {

        //sorting the Configuration objects by their PID which are in the format "appProperties.property_xx" where xx is an incrementing integer
        //so the first one discovered might be "appProperties.property_17" and the second one is "appProperties.property_18"
        //This ordering is not a defined API but I'm hoping that it won't change
        SortedSet<Configuration> configSet = new TreeSet<>((o1, o2) -> o1.getPid().compareTo(o2.getPid()));

        if (FrameworkState.isValid()) {
            try {
                String applicationPID = getApplicationPID(bundleContext, applicationName);
                String applicationPropertiesPid = null;
                ConfigurationAdmin admin = getConfigurationAdmin(bundleContext);
                if (applicationPID != null && admin != null) {
                    String appPropertiesFilter = getApplicationPropertiesConfigFilter(applicationPID);
                    Configuration[] appPropertiesOsgiConfigs = admin.listConfigurations(appPropertiesFilter);
                    if (appPropertiesOsgiConfigs != null) {
                        for (Configuration cfg : appPropertiesOsgiConfigs) {
                            applicationPropertiesPid = cfg.getPid();
                        }
                    }

                    if (applicationPropertiesPid != null) {
                        String appPropertiesPropertyFilter = getApplicationPropertiesPropertyConfigFilter(applicationPropertiesPid);
                        Configuration[] appPropertiesPropertyOsgiConfigs = admin.listConfigurations(appPropertiesPropertyFilter);
                        if (appPropertiesPropertyOsgiConfigs != null) {
                            for (Configuration cfg : appPropertiesPropertyOsgiConfigs) {
                                configSet.add(cfg);
                            }
                        }
                    }
                }
            } catch (IOException |

                            InvalidSyntaxException e) {
                throw new ConfigException(e);
            }
        }

        return configSet;
    }

    /**
     * Get a Map that represents the name/value pairs of <variable> elements in the server.xml
     *
     * @param bundleContext the context to use in looking up OSGi service references
     * @return
     */
    public static Map<String, String> getVariableFromServerXML(BundleContext bundleContext) {
        Map<String, String> theMap = new HashMap<>();
        if (FrameworkState.isValid()) {
            ConfigVariables configVars = getConfigVariables(bundleContext);

            // Retrieve the Map of variables that have been defined in the server.xml
            theMap.putAll(configVars.getUserDefinedVariables());
        }
        return theMap;
    }

    /**
     * Test to see if the given configuration key starts with any known system prefixes
     *
     * @param key the key to test
     * @return true if it startsWith one of the system prefixes
     */
    public static boolean isSystemKey(String key) {
        for (String prefix : Config13Constants.SYSTEM_PREFIXES) {
            if (key.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
