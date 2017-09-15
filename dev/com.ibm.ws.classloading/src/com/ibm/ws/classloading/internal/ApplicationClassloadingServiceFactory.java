/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.library.internal.LibraryStatusService;

/**
 * Creates ApplicationClassloadingService objects for each application
 * configured with a classloader element in server.xml. Each of these
 * services will be injected with the libraries configured.
 */

@Component(configurationPid = "ApplicationClassloadingServiceFactory", configurationPolicy = ConfigurationPolicy.IGNORE,
           property = "service.pid=com.ibm.wsspi.classloading.classloader")
public class ApplicationClassloadingServiceFactory implements ManagedServiceFactory {

    private static final TraceComponent tc = Tr.register(ApplicationClassloadingServiceFactory.class);

    private final ConcurrentMap<String, Configuration> instances = new ConcurrentHashMap<String, Configuration>();

    private volatile ConfigurationAdmin configAdmin;

    private static final String LIBRARY_REF_ATT = "privateLibraryRef";
    private static final String COMMON_LIBRARY_REF_ATT = "commonLibraryRef";
    public static final String COMPONENT_FACTORY_PID = "com.ibm.wsspi.classloading.classloader.app";

    @Reference(name = "configAdmin", policy = ReferencePolicy.STATIC)
    protected void setConfigAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    protected void unsetConfigAdmin(ConfigurationAdmin configAdmin) {
        if (this.configAdmin == configAdmin)
            this.configAdmin = null;
    }

    @Override
    public void deleted(String pid) {
        Configuration instance = instances.remove(pid);
        if (instance != null) {
            try {
                instance.delete();
            } catch (Exception e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Error while deleting application with classloader config pid:  " + pid + "Exception: " + e);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void updated(String pid, @SuppressWarnings("rawtypes") Dictionary config) throws ConfigurationException {

        // try to find an instance in the map
        Configuration existingInstance = instances.get(pid);
        try {
            // CREATE if there wasn't an instance
            if (existingInstance == null) {
                Configuration newInstance = configAdmin.createFactoryConfiguration(COMPONENT_FACTORY_PID);
                // sync so that putIfAbsent() and registerServices() are atomic
                synchronized (newInstance) {
                    existingInstance = instances.putIfAbsent(pid, newInstance);
                    if (existingInstance == null) {
                        Dictionary<String, Object> properties = buildServicePropsAndFilterTargets(pid, config);
                        newInstance.update(properties);
                        return;
                    }
                }
            }

            // UPDATE if there was already an instance
            synchronized (existingInstance) {
                // synchronized so we know inserted instance is
                existingInstance.update(config);
            }
        } catch (Exception e) {
            // Believe a user can do nothing about an exception here as none should be thrown
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Error while updating application with classloader config pid:  " + pid + "Exception: " + e);
        }
    }

    /**
     * Add the properties for this new service to allow
     * other components locate this service
     * 
     * @throws InvalidSyntaxException
     * @throws IOException
     */
    private Dictionary<String, Object> buildServicePropsAndFilterTargets(String pid, Dictionary<String, Object> config) throws IOException, InvalidSyntaxException {
        Dictionary<String, Object> result = new Hashtable<String, Object>();

        // we will use this later to discover the properties configured in this classloader element in config
        result.put("classloader.config.pid", pid);

        // Add the application so other DS components can key from this new classloader sevice
        String appFilter = "(classloader=" + pid + ")";
        Configuration[] appConfigs = configAdmin.listConfigurations(appFilter);

        if (appConfigs.length == 1) {
            Configuration appConfig = appConfigs[0];
            Dictionary<String, Object> properties = appConfig.getProperties();
            String appName = (String) properties.get("name");
            if (appName != null) {
                result.put("application.name", appName);
            }
            String appConfigPid = (String) properties.get("service.pid");
            try {
                result.put("application.pid", appConfigPid);
            } catch (NullPointerException swallowed) {
                /* this will be FFDC'd */
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "service.pid is null", swallowed);
            }
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Creating ApplicationClassloadingService for application" + appName);
        }

        List<String> allLibraries = new ArrayList<String>();

        String[] privateLibraryRefs = (String[]) config.get(LIBRARY_REF_ATT);
        if (privateLibraryRefs != null) {
            allLibraries.addAll(Arrays.asList(privateLibraryRefs));
        }

        String[] commonLibraryRefs = (String[]) config.get(COMMON_LIBRARY_REF_ATT);
        if (commonLibraryRefs != null) {
            allLibraries.addAll(Arrays.asList(commonLibraryRefs));
        }

        if (allLibraries.size() > 0) {
            String filter = buildTargetString(allLibraries);
            result.put("libraryStatus.target", filter);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "This application will wait for the following libraries ", filter);
        } else {
            // we need to clear the blocking target and make sure we 
            // do get activated by setting a target we know will happen
            result.put("libraryStatus.target", "(id=global)");
        }

        return result;
    }

    /**
     * This filter will cause the new application classloading service to block until these libraries are active
     * Each library is added twice as it may be an automatic librari in which case
     * its pid will not yet be known so we use the id.
     */
    private String buildTargetString(List<String> privateLibraries) {
        StringBuilder filter = new StringBuilder();
        filter.append("(&");
        for (String lib : privateLibraries)
            filter.append(String.format("(|(%s=%s)(%s=%s))", LibraryStatusService.LIBRARY_IDS, lib, LibraryStatusService.LIBRARY_PIDS, lib));
        filter.append(")");
        return filter.toString();
    }

    @Override
    public String getName() {
        return "ApplicationClassloaderConfigurationHelper";
    }

}
