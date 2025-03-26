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

import javax.management.MBeanInfo;
import javax.management.ObjectName;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.microprofile.health.internal.AppTracker;
import com.ibm.ws.microprofile.health.internal.AppTrackerImpl;
import com.ibm.wsspi.application.ApplicationState;

import io.openliberty.checkpoint.spi.CheckpointPhase;
import io.openliberty.microprofile.health.internal.common.HealthCheckConstants;

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
            if (ProductInfo.getBetaEdition()) {
                /*
                 * This is built off of AppTrackerImpl, which sets an "healthCheckService" as the original
                 * interface. Ensure we are dealing with a HealthCheck40Service and above.
                 */
                if (healthCheckService != null && healthCheckService instanceof HealthCheck40Service) {
                    HealthCheck40Service hc40serv = (HealthCheck40Service) healthCheckService;
                    /*
                     * Start health check processes once the first application has started
                     */
                    if (!isOneAppStarted.getAndSet(true)) {

                        /*
                         * Starting health check processes will invoke an immediate started, liveness and readineess checks.
                         */
                        CheckpointPhase.onRestore(() -> hc40serv.startFileHealthCheckProcesses());

                    } else {
                        /*
                         * Do an immediate startup check after every application start (after the first application).
                         *
                         * If we get to last application to be started, perform "started" file health check to save up to 1 second for indicating start status
                         * of server (Depending on where in the 1 second interval the startup timer is).
                         *
                         * Example:
                         * Given Applications A and B.
                         *
                         * Elapsed time : 100ms. App A starts.
                         * - Health processes start which includes immediate startup check.
                         * - Startup status DOWN (due to convoluted startup check logic).
                         * - Next startup check would be at 1100ms elapsed time.
                         *
                         *
                         * Elapsed time: 500ms.
                         * - App A underlying logic has completed and would report UP at this moment.
                         *
                         * Elapsed time: 600ms. App B starts.
                         * - App B starts.
                         * - Immediate check for startup. App B returns UP. App A returns UP.
                         * - Started file is created.
                         * - Savings of (1100ms - 600ms) 500 ms.
                         *
                         *
                         */
                        hc40serv.performFileHealthCheck(HealthFileUtils.getStartFile(), HealthCheckConstants.HEALTH_CHECK_START);
                    }
                }

            }

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns the MBeanInfo of appName if the ApplicationMBean exists, otherwise null.
     *
     * @return the MBeanInfo of appName if the ApplicationMBean exists, otherwise null.
     */
    @Override
    protected String getApplicationMBean(String appName) {
        MBeanInfo bean = null;
        String state = "";
        try {
            ObjectName objectName = new ObjectName("WebSphere:service=com.ibm.websphere.application.ApplicationMBean,name=" + appName);

            bean = mbeanServer.getMBeanInfo(objectName);
            state = (String) mbeanServer.getAttribute(objectName, "State");
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "getApplicationMBean() : Failed to retrieve MBean for app: " + appName + " : \n" + e.getMessage());
            }
        }
        return state;
    }

}
