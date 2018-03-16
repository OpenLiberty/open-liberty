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
import java.util.SortedSet;
import java.util.TreeSet;

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
import com.ibm.ws.microprofile.config.interfaces.ConfigException;
import com.ibm.ws.microprofile.config13.interfaces.Config13Constants;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.application.Application;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;

/**
 * Assorted osgi based utility methods
 */
public class OSGiConfigUtils {

    private static final TraceComponent tc = Tr.register(OSGiConfigUtils.class);

    /**
     * Get the j2ee name of the application. If the ComponentMetaData is available on the thread then that can be used, otherwise fallback
     * to asking the CDIService for the name ... the CDI context ID is the same as the j2ee name.
     *
     * @param bundleContext The bundle context to use when looking up the CDIService
     * @return the application name
     */
    public static String getApplicationName(BundleContext bundleContext) {
        String applicationName = null;
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (cmd == null) {
            //if the component metadata is null then we're probably running in the CDI startup sequence so try asking CDI for the application
            applicationName = getCDIAppName(bundleContext);
        } else {
            applicationName = cmd.getJ2EEName().getApplication();
        }

        if (applicationName == null) {
            throw new ConfigException(Tr.formatMessage(tc, "no.application.name.CWMCG0201E"));
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
        return FrameworkUtil.getBundle(clazz).getBundleContext();
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
     * Find a service of the given type
     *
     * @param bundleContext The context to use to find the service
     * @param serviceClass The class of the required service
     * @return the service instance or null
     */
    public static <T> T getService(BundleContext bundleContext, Class<T> serviceClass) {
        ServiceReference<T> ref = bundleContext.getServiceReference(serviceClass);
        T service = null;
        if (ref != null) {
            service = bundleContext.getService(ref);
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
        Collection<ServiceReference<Application>> appRefs;
        try {
            appRefs = bundleContext.getServiceReferences(Application.class,
                                                         FilterUtils.createPropertyFilter("name", applicationName));
        } catch (InvalidSyntaxException e) {
            throw new ConfigException(e);
        }

        ServiceReference<Application> appRef = null;
        if (appRefs != null && appRefs.size() > 0) {
            if (appRefs.size() > 1) {
                throw new ConfigException(Tr.formatMessage(tc, "duplicate.application.name.CWMCG0202E", applicationName));
            }
            appRef = appRefs.iterator().next();
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
        ServiceReference<?> appRef = getApplicationServiceRef(bundleContext, applicationName);
        if (appRef != null) {
            //not actually sure what the difference is between service.pid and ibm.extends.source.pid but this is the way it is done everywhere else!
            applicationPID = (String) appRef.getProperty(Constants.SERVICE_PID);
            String sourcePid = (String) appRef.getProperty("ibm.extends.source.pid");
            if (sourcePid != null) {
                applicationPID = sourcePid;
            }
        } ;
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
        // Get the CDIService
        CDIService cdiService = getCDIService(bundleContext);
        if (cdiService != null) {
            appName = cdiService.getCurrentApplicationContextID();
        }
        return appName;
    }

    /**
     * Get the filter string which will extract <appProperties> elements for the given application
     *
     * @param bundleContext The context to use in looking up OSGi service references
     * @param applicationName The application name to look for
     * @return A filter string to be used by the OSGi ConfigurationAdmin service
     */
    public static String getApplicationConfigFilter(BundleContext bundleContext, String applicationName) {
        String applicationConfigFilter = null;

        String applicationPID = getApplicationPID(bundleContext, applicationName);

        StringBuilder applicationFilter = new StringBuilder(200);
        applicationFilter.append("(&");
        applicationFilter.append(FilterUtils.createPropertyFilter("service.factoryPid", "appProperties"));
        applicationFilter.append(FilterUtils.createPropertyFilter("config.parentPID", applicationPID));
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
        //sorting the Configuration objects by their PID which are in the format "appProperties_xx" where xx is an incrementing integer
        //so the first one discovered might be "appProperties_17" and the second one is "appProperties_18"
        //This ordering is not a defined API but I'm hoping that it won't change
        SortedSet<Configuration> configSet = new TreeSet<>((o1, o2) -> o1.getPid().compareTo(o2.getPid()));
        try {
            String applicationFilter = getApplicationConfigFilter(bundleContext, applicationName);
            ConfigurationAdmin admin = getConfigurationAdmin(bundleContext);
            Configuration[] osgiConfigs = admin.listConfigurations(applicationFilter);
            for (Configuration cfg : osgiConfigs) {
                configSet.add(cfg);
            }
        } catch (IOException | InvalidSyntaxException e) {
            throw new ConfigException(e);
        }

        return configSet;
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
