/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.health40.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ApplicationClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.health.internal.AppTracker;
import com.ibm.ws.microprofile.health.internal.AppTrackerImpl;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.application.ApplicationState;

/**
 *
 */
@Component(service = { AppTracker.class,
                       ApplicationStateListener.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true, property = { "service.vendor=IBM" })
public class AppTracker40Impl extends AppTrackerImpl implements AppTracker, ApplicationStateListener {

    private static final TraceComponent tc = Tr.register(AppTracker40Impl.class);

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

            if (tc.isDebugEnabled())
                Tr.debug(tc, "applicationStarting(): starting app added in appStateMap = " + appStateMap.toString() + " for app: " + appName);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {
        String appName = appInfo.getDeploymentName();
        lock.writeLock().lock();
        try {
            if (appStateMap.containsKey(appName)) {
                appStateMap.replace(appName, ApplicationState.STARTING, ApplicationState.STARTED);
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "applicationStarted(): started app updated in appStateMap = " + appStateMap.toString() + " for app: " + appName);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void applicationStopping(ApplicationInfo appInfo) {
    }

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
            String state = getApplicationMBean(appName);
            if (state.equals("STARTING")) {
                appStateMap.replace(appName, ApplicationState.INSTALLED);
            } else {
                appStateMap.remove(appName);
            }
            if (tc.isDebugEnabled())
                Tr.debug(tc, "applicationStopped(): stopped app removed from appStateMap = " + appStateMap.toString() + " for app: " + appName);
        } finally {
            lock.writeLock().unlock();
        }
    }

}
