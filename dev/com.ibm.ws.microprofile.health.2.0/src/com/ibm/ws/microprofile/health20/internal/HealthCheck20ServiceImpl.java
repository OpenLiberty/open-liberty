/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.health20.internal;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.health.internal.AppTracker;
import com.ibm.ws.microprofile.health.internal.HealthCheckHttpResponseBuilder;
import com.ibm.ws.microprofile.health.services.HealthCheckBeanCallException;
import com.ibm.ws.microprofile.health20.services.HealthCheck20Executor;

/**
 * Microprofile Health Check Service Implementation
 */
@Component(service = HealthCheck20Service.class, property = { "service.vendor=IBM" })

public class HealthCheck20ServiceImpl implements HealthCheck20Service {

    private static final TraceComponent tc = Tr.register(HealthCheck20ServiceImpl.class);

    private AppTracker appTracker;
    private HealthCheck20Executor hcExecutor;

    @Reference(service = AppTracker.class)
    protected void setAppTracker(AppTracker service) {
        this.appTracker = service;
    }

    protected void unsetAppTracker(AppTracker service) {
        if (this.appTracker == service) {
            this.appTracker = null;
        }
    }

    @Reference(service = HealthCheck20Executor.class)
    protected void setHealthExecutor(HealthCheck20Executor service) {
        this.hcExecutor = service;
    }

    protected void unsetHealthExecutor(HealthCheck20Executor service) {
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
        Set<String> apps = appTracker.getAppNames();
        Iterator<String> appsIt = apps.iterator();

        HealthCheckHttpResponseBuilder hcHttpResponseBuilder = new HealthCheck20HttpResponseBuilder();

        while (appsIt.hasNext()) {
            String appName = appsIt.next();
            Set<String> modules = appTracker.getModuleNames(appName);
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

        hcHttpResponseBuilder.setHttpResponse(httpResponse);
    }
}
