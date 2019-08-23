/*******************************************************************************
 * Copyright (c) 2017, 2019 Contributors to the Eclipse Foundation
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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ApplicationClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.EARApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.application.ApplicationState;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;

/**
 * Retrieves the application and modules names during application deployments
 */
@Component(service = { AppTracker.class,
                       ApplicationStateListener.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true, property = { "service.vendor=IBM" })
public class AppTrackerImpl implements AppTracker, ApplicationStateListener {

    private static final TraceComponent tc = Tr.register(AppTrackerImpl.class);

    private final HashMap<String, Set<String>> appModules = new HashMap<String, Set<String>>();

    /**
     * Lock for accessing application/deferred task information.
     */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Tracks the state of starting/started applications.
     */
    private final Map<String, ApplicationState> appStateMap = new HashMap<String, ApplicationState>();

    private HealthCheckService healthCheckService;

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
    public Set<String> getAppNames() {
        return appModules.keySet();
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getModuleNames(String appName) {
        return appModules.get(appName);
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(UnableToAdaptException.class)
    public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
        String appName = appInfo.getDeploymentName();
        if (tc.isDebugEnabled())
            Tr.debug(tc, "applicationStarting() : appName = " + appName);

        Container appContainer = appInfo.getContainer();
        if (appContainer == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "applicationStarting() : appContainer=null for " + appInfo);
            }
            return;
        }

        try {
            NonPersistentCache cache = appContainer.adapt(NonPersistentCache.class);
            ApplicationClassesContainerInfo acci = (ApplicationClassesContainerInfo) cache.getFromCache(ApplicationClassesContainerInfo.class);
            if (acci == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "applicationStarting() : applicationClassesContainerInfo=null for " + appInfo);
                }
                return;
            }
        } catch (UnableToAdaptException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "applicationStarting() : Failed to adapt NonPersistentCache: container=" + appContainer + " : \n" + e.getMessage());
            }
            return;
        }

        // Process the application to check if it is a WAR or an EAR file and register it.
        processApplication(appContainer, appInfo, appName, false);

        // Add starting application to the starting app map, to keep track of all the application states.
        lock.writeLock().lock();
        try {
            appStateMap.put(appName, ApplicationState.STARTING);
        } finally {
            lock.writeLock().unlock();
        }

        if (tc.isDebugEnabled())
            Tr.debug(tc, "applicationStarting(): starting app added in appStateMap = " + appStateMap.toString() + " for app: " + appName);
    }

    /**
     * Gets the Application module names from the WebModuleMetadata
     *
     * @param webModuleMetaData
     */
    private String getAppModuleNameFromMetaData(WebModuleMetaData webModuleMetaData) {
        String appModuleName = null;
        appModuleName = webModuleMetaData.getJ2EEName().toString();
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getAppModuleNameFromMetaData() : appModuleName = " + appModuleName);

        return appModuleName;
    }

    @FFDCIgnore(UnableToAdaptException.class)
    private WebModuleMetaData getWebModuleMetaData(Container container) {
        WebModuleMetaData wmmd = null;
        NonPersistentCache overlayCache = null;

        try {
            overlayCache = container.adapt(NonPersistentCache.class);
        } catch (UnableToAdaptException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "getWebModuleMetaData() : Failed to adapt NonPersistentCache: container=" + container + " : \n" + e.getMessage());
            }
        }

        if (overlayCache != null) {
            wmmd = (WebModuleMetaData) overlayCache.getFromCache(WebModuleMetaData.class);

        }
        return wmmd;
    }

    /**
     * Stores the application modules for the applications
     *
     * @param appName
     * @param moduleAndAppName
     */
    private synchronized void addAppModuleNames(String appName, String moduleAndAppName) {
        HashSet<String> moduleNames = null;
        if (moduleAndAppName == null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "addAppModuleNames(): moduleAndAppName is null.");
            return;
        }
        String moduleName = moduleAndAppName.split("#")[1];
        if (appModules.containsKey(appName)) {
            // Update the appModule map
            moduleNames = (HashSet<String>) appModules.get(appName);
            moduleNames.add(moduleName);
            appModules.replace(appName, moduleNames);
        } else {
            moduleNames = new HashSet<String>();
            moduleNames.add(moduleName);
            appModules.put(appName, moduleNames);
        }

        if (tc.isDebugEnabled())
            Tr.debug(tc, "addAppModuleNames(): modules added = " + appModules.toString() + " for app: " + appName + " and for module: " + moduleName);
    }

    /** {@inheritDoc} */
    @Override
    public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {
        String appName = appInfo.getDeploymentName();
        lock.writeLock().lock();
        try {
            if (appStateMap.containsKey(appName)) {
                appStateMap.replace(appName, ApplicationState.STARTING, ApplicationState.STARTED);
            }
        } finally {
            lock.writeLock().unlock();
        }

        if (tc.isDebugEnabled())
            Tr.debug(tc, "applicationStarted(): started app updated in appStateMap = " + appStateMap.toString() + " for app: " + appName);
    }

    /**
     * Returns true if the application with the specified name is started, otherwise false.
     *
     * @return true if the application with the specified name is started, otherwise false.
     */
    @Override
    public boolean isStarted(String appName) {
        lock.readLock().lock();
        try {
            return appStateMap.get(appName) == ApplicationState.STARTED;
        } finally {
            lock.readLock().unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void applicationStopping(ApplicationInfo appInfo) {}

    /** {@inheritDoc} */
    @Override
    public void applicationStopped(ApplicationInfo appInfo) {
        String appName = appInfo.getDeploymentName();

        // Remove the registered application modules
        Container appContainer = appInfo.getContainer();
        if (appContainer == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "applicationStopped() : appContainer=null for " + appInfo);
            }
            return;
        }

        // Process the application to check if it is a WAR or an EAR file and unregister it.
        processApplication(appContainer, appInfo, appName, true);

        // Remove the stopped application from the appState map
        lock.writeLock().lock();
        try {
            appStateMap.remove(appName);
        } finally {
            lock.writeLock().unlock();
        }

        if (tc.isDebugEnabled())
            Tr.debug(tc, "applicationStopped(): stopped app removed from appStateMap = " + appStateMap.toString() + " for app: " + appName);
    }

    /**
     * Processes the application and checks if it is an EAR or WAR file and registers/unregisters the application,
     * depending on the application state.
     *
     * @param appContainer
     * @param appInfo
     * @param appName
     * @param isAppStopped
     */
    private void processApplication(Container appContainer, ApplicationInfo appInfo, String appName, boolean isAppStopped) {
        //Check if the deployed application is an EAR or WAR file
        if (appInfo instanceof EARApplicationInfo) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "processApplication() : App " + appInfo.getDeploymentName() + " is an EAR file.");
            EARApplicationInfo earAppInfo = (EARApplicationInfo) appInfo;
            processEARApplication(appContainer, earAppInfo, isAppStopped);
        } else {
            // If the application is a WAR file, simply get the WebModuleMetaData from the application container to register the appName and appModuleName
            WebModuleMetaData webModuleMetaData = getWebModuleMetaData(appContainer);
            if (webModuleMetaData != null) {
                String appModuleName = getAppModuleNameFromMetaData(webModuleMetaData);
                if (isAppStopped) {
                    // Unregister the app and module names for the WAR application
                    moduleStopped(appName, appModuleName);
                } else {
                    // Register the app and module names for the WAR application
                    addAppModuleNames(appName, appModuleName);
                }
            }
        }
    }

    /**
     * Processes the EAR application and registers/unregisters the application, depending on the application state.
     *
     * @param appContainer
     * @param earAppInfo
     * @param isAppStopped
     */
    @FFDCIgnore(UnableToAdaptException.class)
    private void processEARApplication(Container appContainer, EARApplicationInfo earAppInfo, boolean isAppStopped) {
        for (Entry entry : appContainer) {
            try {
                Container c = entry.adapt(Container.class);
                if (c != null) {
                    WebModuleMetaData webModuleMetaData = getWebModuleMetaData(c);
                    if (webModuleMetaData != null) {
                        String appName = earAppInfo.getDeploymentName();
                        String appModuleName = getAppModuleNameFromMetaData(webModuleMetaData);
                        if (isAppStopped) {
                            // Unregister the app and module names
                            moduleStopped(appName, appModuleName);
                        } else {
                            // Register the app and module names
                            addAppModuleNames(appName, appModuleName);
                        }
                    }
                }
            } catch (UnableToAdaptException e) {
                if (tc.isDebugEnabled()) {
                    Tr.event(tc, "processEARApplication() : Failed to adapt entry: entry=" + entry + " : \n" + e.getMessage());
                }
            }
        }
    }

    /**
     * Removes the stopped modules.
     *
     * @param appName
     * @param appModuleName
     */
    public synchronized void moduleStopped(String appName, String appModuleName) {
        Set<String> modules = appModules.get(appName);
        String moduleName = appModuleName.split("#")[1];
        if (modules != null) {
            modules.remove(moduleName);

            // If that was the last module, remove the entry for the app.
            if (modules.size() <= 0) {
                appModules.remove(appName);
            }

            if (tc.isDebugEnabled())
                Tr.debug(tc, "moduleStopped(): app module removed = " + appModules.toString() + " for app: " + appName + " and for module: " + moduleName);

        }

        if (healthCheckService != null) {
            healthCheckService.removeModuleReferences(appName, moduleName);
        }
    }

    @Override
    public void setHealthCheckService(HealthCheckService healthService) {
        this.healthCheckService = healthService;
    }
}
