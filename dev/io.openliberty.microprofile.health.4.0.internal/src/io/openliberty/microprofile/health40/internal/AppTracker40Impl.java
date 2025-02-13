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

import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.microprofile.health.internal.AppTracker;
import com.ibm.ws.microprofile.health.internal.AppTrackerImpl;
import com.ibm.wsspi.application.ApplicationState;

/**
 *
 */
@Component(service = { AppTracker.class,
                       ApplicationStateListener.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true, property = { "service.vendor=IBM" })
public class AppTracker40Impl extends AppTrackerImpl implements AppTracker, ApplicationStateListener {

    private static final TraceComponent tc = Tr.register(AppTracker40Impl.class);

    /*
     * Flag to indicate that the first application has started
     */
    private static AtomicBoolean isOneAppStarted = new AtomicBoolean(false);

    /** {@inheritDoc} */
    @Override
    public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {

        String appName = appInfo.getDeploymentName();
        lock.writeLock().lock();
        try {
            if (appStateMap.containsKey(appName)) {
                appStateMap.replace(appName, ApplicationState.STARTING, ApplicationState.STARTED);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "applicationStarted(): started app updated in appStateMap = " + appStateMap.toString() + " for app: " + appName);
                }
            }

            /*
             * Start the File Health checking.
             * We don't know if it's enabled or not, logic
             * will be handled by the HealthCheck40Service (or higher levels).
             */
            if (!isOneAppStarted.getAndSet(true)) {

                /*
                 * This is built off of AppTrackerImpl, which sets an "healthCheckService" as the original
                 * interface. Ensure we are dealing with a HealthCheck40Service and above.
                 */
                if (healthCheckService != null && healthCheckService instanceof HealthCheck40Service) {
                    ((HealthCheck40Service) healthCheckService).startFileHealthCheckProcesses();
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

}
