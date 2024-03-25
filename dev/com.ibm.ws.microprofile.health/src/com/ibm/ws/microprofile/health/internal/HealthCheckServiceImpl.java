/*******************************************************************************
 * Copyright (c) 2017, 2024 Contributors to the Eclipse Foundation
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
import com.ibm.ws.microprofile.health.services.HealthCheckBeanCallException;
import com.ibm.ws.microprofile.health.services.HealthExecutor;

/**
 * Microprofile Health Check Service Implementation
 */
@Component(service = HealthCheckService.class, property = { "service.vendor=IBM" })

public class HealthCheckServiceImpl implements HealthCheckService {

    private static final TraceComponent tc = Tr.register(HealthCheckServiceImpl.class);

    private AppTracker appTracker;
    private HealthExecutor hcExecutor;

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

    @Reference(service = HealthExecutor.class)
    protected void setHealthExecutor(HealthExecutor service) {
        this.hcExecutor = service;
    }

    protected void unsetHealthExecutor(HealthExecutor service) {
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
        Set<HealthCheckResponse> hcResponses = null;
        Set<String> apps = appTracker.getAppNames();
        Iterator<String> appsIt = apps.iterator();

        HealthCheckHttpResponseBuilder hcHttpResponseBuilder = new HealthCheckHttpResponseBuilder();

        while (appsIt.hasNext()) {
            String appName = appsIt.next();
            Set<String> modules = appTracker.getModuleNames(appName);
            Iterator<String> moduleIt = modules.iterator();
            while (moduleIt.hasNext()) {
                String moduleName = moduleIt.next();
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "In performHealthCheck(): appName = " + appName + ", moduleName = " + moduleName);
                try {
                    hcResponses = hcExecutor.runHealthChecks(appName, moduleName);
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

    @Override
    public void removeModuleReferences(String appName, String moduleName) {
        if (hcExecutor != null) {
            hcExecutor.removeModuleReferences(appName, moduleName);
        }
    }
}
