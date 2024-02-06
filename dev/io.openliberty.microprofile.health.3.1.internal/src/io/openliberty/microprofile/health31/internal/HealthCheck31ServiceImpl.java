/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.health31.internal;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponse.Status;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.health.internal.AppTracker;
import com.ibm.ws.microprofile.health.services.HealthCheckBeanCallException;

import io.openliberty.microprofile.health.internal.common.HealthCheckConstants;
import io.openliberty.microprofile.health30.internal.HealthCheck30HttpResponseBuilder;
import io.openliberty.microprofile.health31.services.HealthCheck31Executor;

/**
 * Microprofile Health Check Service Implementation
 */
@Component(service = HealthCheck31Service.class, property = { "service.vendor=IBM" })

public class HealthCheck31ServiceImpl implements HealthCheck31Service {

    private static final TraceComponent tc = Tr.register(HealthCheck31ServiceImpl.class);

    private AppTracker appTracker;
    private HealthCheck31Executor hcExecutor;

    final AtomicBoolean readinessWarningAlreadyShown = new AtomicBoolean(false);
    final AtomicBoolean startupWarningAlreadyShown = new AtomicBoolean(false);
    AtomicInteger unstartedAppsCounter = new AtomicInteger(0);

    @Reference(service = AppTracker.class)
    protected void setAppTracker(AppTracker service) {
        this.appTracker = service;
        appTracker.setHealthCheckService(this);
    }

    protected void unsetAppTracker(AppTracker service) {
        if (this.appTracker == service) {
            this.appTracker = null;
        }
    }

    @Reference(service = HealthCheck31Executor.class)
    protected void setHealthExecutor(HealthCheck31Executor service) {
        this.hcExecutor = service;
    }

    protected void unsetHealthExecutor(HealthCheck31Executor service) {
        if (this.hcExecutor == service) {
            this.hcExecutor = null;
        }
    }

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> properties) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "HealthCheckServiceImpl is activated");
    }

    @Deactivate
    protected void deactivate(ComponentContext cc, int reason) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "HealthCheckServiceImpl is deactivated");
    }

    /** {@inheritDoc} */
    @Override
    public void performHealthCheck(HttpServletRequest request, HttpServletResponse httpResponse) {
        performHealthCheck(request, httpResponse, HealthCheckConstants.HEALTH_CHECK_ALL);
    }

    /** {@inheritDoc} */
    @Override
    public void performHealthCheck(HttpServletRequest request, HttpServletResponse httpResponse, String healthCheckProcedure) {
        Set<HealthCheckResponse> hcResponses = null;
        Set<String> unstartedAppsSet = new HashSet<String>();
        Set<String> apps = appTracker.getAllAppNames();
        Iterator<String> appsIt = apps.iterator();
        boolean anyAppsInstalled = false;

        HealthCheck30HttpResponseBuilder hcHttpResponseBuilder = new HealthCheck30HttpResponseBuilder();

        // Verify if the default overall Startup/Readiness status is configured
        String defaultReadinessProp = ConfigProvider.getConfig().getOptionalValue(HealthCheckConstants.DEFAULT_OVERALL_READINESS_STATUS, String.class).orElse("");
        String defaultStartupProp = ConfigProvider.getConfig().getOptionalValue(HealthCheckConstants.DEFAULT_OVERALL_STARTUP_STATUS, String.class).orElse("");

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "In performHealthCheck(): The default overall Readiness status was configured to be overriden: mp.health.default.readiness.empty.response="
                         + defaultReadinessProp);
            Tr.debug(tc, "In performHealthCheck(): The default overall Startup status was configured to be overriden: mp.health.default.startup.empty.response="
                         + defaultStartupProp);
        }
        Status defaultOverallReadiness = defaultReadinessProp.equalsIgnoreCase("UP") ? Status.UP : Status.DOWN;
        Status defaultOverallStartup = defaultStartupProp.equalsIgnoreCase("UP") ? Status.UP : Status.DOWN;

        while (appsIt.hasNext()) {
            String appName = appsIt.next();
            if (appTracker.isInstalled(appName)) {
                anyAppsInstalled = true;
                if (!healthCheckProcedure.equals(HealthCheckConstants.HEALTH_CHECK_LIVE) && !unstartedAppsSet.contains(appName)) {
                    unstartedAppsSet.add(appName);
                }
            } else if (!appTracker.isUninstalled(appName) && !appTracker.isStarted(appName)) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "In performHealthCheck(): Application : " + appName + " has not started yet.");
                if (!(healthCheckProcedure.equals(HealthCheckConstants.HEALTH_CHECK_LIVE))) {
                    if (healthCheckProcedure.equals(HealthCheckConstants.HEALTH_CHECK_START)) {
                        hcHttpResponseBuilder.setOverallStatus(defaultOverallStartup);
                    } else if (healthCheckProcedure.equals(HealthCheckConstants.HEALTH_CHECK_READY)) {
                        hcHttpResponseBuilder.setOverallStatus(defaultOverallReadiness);
                    } else {
                        // If the /health is hit, it should have the aggregated status of the individual health check procedures
                        hcHttpResponseBuilder.setOverallStatus((defaultOverallStartup.equals(Status.UP)
                                                                && defaultOverallReadiness.equals(Status.UP)) ? Status.UP : Status.DOWN);
                    }

                    // Keep track of the unstarted applications names
                    if (!unstartedAppsSet.contains(appName)) {
                        unstartedAppsSet.add(appName);
                    }
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "In performHealthCheck(): unstartedAppsSet after adding the unstarted app : " + unstartedAppsSet);
                } else {
                    // for liveness check
                    hcHttpResponseBuilder.setOverallStatus(Status.UP);
                }
            } else {
                Set<String> modules = appTracker.getModuleNames(appName);
                if (modules != null) {
                    Iterator<String> moduleIt = modules.iterator();

                    while (moduleIt.hasNext()) {
                        String moduleName = moduleIt.next();
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "In performHealthCheck(): appName = " + appName + ", moduleName = " + moduleName);

                        try {
                            hcResponses = hcExecutor.runHealthChecks(appName, moduleName, healthCheckProcedure);
                        } catch (HealthCheckBeanCallException e) {
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "In performHealthCheck(): Caught the exception " + e + " for appName = " + appName + ", moduleName = " + moduleName);
                            hcHttpResponseBuilder.handleUndeterminedResponse(httpResponse);
                            return;
                        }

                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "In performHealthCheck(): hcResponses = " + hcResponses);

                        if (!hcResponses.isEmpty())
                            hcHttpResponseBuilder.addResponses(hcResponses);
                    }
                }
            }

        }

        if (unstartedAppsSet.isEmpty()) {
            // If all applications are started, reset counter
            unstartedAppsCounter.set(0);
        } else if (!unstartedAppsSet.isEmpty() && unstartedAppsCounter.get() != unstartedAppsSet.size()) {
            // Update the new number of unstarted applications, since some applications may have already started.
            unstartedAppsCounter.set(unstartedAppsSet.size());
            if (tc.isDebugEnabled())
                Tr.debug(tc, "In performHealthCheck(): numOfUnstartedApps after unstarted app set was updated. = " + unstartedAppsCounter.get());

            // If there are other applications that have not started yet, show the message again, with the updated set.
            if (!unstartedAppsSet.isEmpty()) {
                readinessWarningAlreadyShown.set(false);
                startupWarningAlreadyShown.set(false);
            } else {
                readinessWarningAlreadyShown.set(true);
                startupWarningAlreadyShown.set(true);
            }

        }

        if (!unstartedAppsSet.isEmpty()) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "In performHealthCheck(): numOfUnstartedApps = " + unstartedAppsCounter.get());

            if (healthCheckProcedure.equals(HealthCheckConstants.HEALTH_CHECK_START) && startupWarningAlreadyShown.compareAndSet(false, true)
                && !defaultOverallStartup.equals(Status.UP)) {
                Tr.warning(tc, "startup.healthcheck.applications.not.started.down.CWMMH0054W", new Object[] { unstartedAppsSet });
            } else if (healthCheckProcedure.equals(HealthCheckConstants.HEALTH_CHECK_READY) && readinessWarningAlreadyShown.compareAndSet(false, true)
                       && !defaultOverallReadiness.equals(Status.UP)) {
                Tr.warning(tc, "readiness.healthcheck.applications.not.started.down.CWMMH0053W", new Object[] { unstartedAppsSet });
            }
        }

        if (anyAppsInstalled && !(healthCheckProcedure.equals(HealthCheckConstants.HEALTH_CHECK_LIVE))) {
            hcHttpResponseBuilder.setOverallStatus(Status.DOWN);
        }

        hcHttpResponseBuilder.setHttpResponse(httpResponse);
    }

    @Override
    public void removeModuleReferences(String appName, String moduleName) {
        if (hcExecutor != null) {
            hcExecutor.removeModuleReferences(appName, moduleName);
        }
    }
}
