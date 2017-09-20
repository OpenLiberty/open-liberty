/*******************************************************************************
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.health.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;

import org.eclipse.microprofile.health.HealthCheck;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer.webapp.WebAppConfigExtended;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

/**
 * Retrieves the application and modules names during application deployments
 */
@Component(service = { AppTracker.class, ServletContainerInitializer.class }, configurationPolicy = ConfigurationPolicy.IGNORE, 
       property = { "service.vendor=IBM" })
@HandlesTypes(HealthCheck.class)
public class AppTrackerImpl implements ServletContainerInitializer, AppTracker {

    private static final TraceComponent tc = Tr.register(AppTrackerImpl.class);

    private static final String BUNDLE_CONTEXT_KEY = "osgi-bundlecontext";

    private final HashMap<String, Set<String>> appModules = new HashMap<String, Set<String>>();

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> properties) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "AppTrackerImpl is activated");
    }

    @Deactivate
    protected void deactivate(ComponentContext cc, int reason) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "AppTrackerImpl is deactivated");
    }

    /** {@inheritDoc} */
    @Override
    public void onStartup(Set<Class<?>> arg0, ServletContext ctx) throws ServletException {
        IServletContext isc = (IServletContext) ctx;
        AppModuleName pair = setAppModuleNames(isc);
        if (pair != null) {
            isc.addListener(new AppTrackerServletContextListener(pair, this));
        }
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getAppNames() {
        return appModules.keySet();
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getModuleNames(String appName) {
        return appModules.get(appName);
    }

    /*
     * collect all app and module names and save it for later use
     */
    private AppModuleName setAppModuleNames(IServletContext isc) {

        WebAppConfig webAppConfig = isc.getWebAppConfig();
        if (webAppConfig.isSystemApp()) {
            Tr.debug(tc, "Detected system app so won't track for health check; appName = ", webAppConfig.getApplicationName());
            return null;
        }

        if (isOsgiApp(isc)) {
            Tr.debug(tc, "Detected OSGi app, so won't track for health check; appName = ", webAppConfig.getApplicationName());
            return null;
        }

        WebModuleMetaData webModuleMetaData = ((WebAppConfigExtended) webAppConfig).getMetaData();
        String appName = webModuleMetaData.getApplicationMetaData().getName();

        String moduleName = webModuleMetaData.getJ2EEName().toString();
        return addAppModuleNames(appName, moduleName);
    }

    // Seems like this should be an SPI instead of having to calculate based on patterns this far down the chain.
    // Will look into this in the future, hopefully.
    private boolean isOsgiApp(IServletContext isc) {
        Object bundleCtxAttr = isc.getAttribute(BUNDLE_CONTEXT_KEY);
        Tr.debug(tc, "Servet context attr for key = " + BUNDLE_CONTEXT_KEY + ", = " + bundleCtxAttr);
        if (bundleCtxAttr != null) {
            return true;
        } else {
            return false;
        }
    }

    /*
     * Store module names for the app
     */
    private synchronized AppModuleName addAppModuleNames(String appName, String moduleAndAppName) {
        HashSet<String> moduleNames = null;
        String moduleName = moduleAndAppName.split("#")[1];
        if (appModules.containsKey(appName)) {
            moduleNames = (HashSet<String>) appModules.get(appName);
            moduleNames.add(moduleName);
        } else {
            moduleNames = new HashSet<String>();
            moduleNames.add(moduleName);
            appModules.put(appName, moduleNames);
        }

        if (tc.isDebugEnabled())
            Tr.debug(tc, "addAppModuleNames(): modules added = " + appModules.toString() + " for app: " + appName);

        AppModuleName retVal = new AppModuleName();
        retVal.appName = appName;
        retVal.moduleName = moduleName;
        return retVal;
    }

    // Package-level visibility seems appropriate
    static class AppModuleName {
        String appName;
        String moduleName;
    }

    /**
     * @param pair
     */
    public void moduleStopped(AppModuleName pair) {
        Set<String> modules = appModules.get(pair.appName);
        if (modules != null) {
            modules.remove(pair.moduleName);
            // If that was the last module, remove the entry for the app.
            if (modules.size() <= 0) {
                appModules.remove(pair.appName);
            }
        }
    }
}
