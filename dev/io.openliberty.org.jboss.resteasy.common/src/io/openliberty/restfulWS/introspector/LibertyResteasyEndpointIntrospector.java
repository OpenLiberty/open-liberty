/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.restfulWS.introspector;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.wsspi.logging.Introspector;

@Component(service = { ApplicationStateListener.class, Introspector.class, LibertyResteasyEndpointLoggingIntrospector.class },
           immediate = true,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = "service.vendor=IBM")
public class LibertyResteasyEndpointIntrospector implements ApplicationStateListener, Introspector, LibertyResteasyEndpointLoggingIntrospector {

    private static final TraceComponent tc = Tr.register(LibertyResteasyEndpointIntrospector.class);

    private static final Map<String, List<String>> applicationEndpoints = new ConcurrentHashMap<String, List<String>>();

    @Override
    public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
        // add the application to the map
        applicationEndpoints.put(appInfo.getName(), new ArrayList<String>());
    }

    @Override
    public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {}

    @Override
    public void applicationStopping(ApplicationInfo appInfo) {}

    @Override
    public void applicationStopped(ApplicationInfo appInfo) {
        // remove all endpoints for the application to prevent a memory leak
        applicationEndpoints.remove(appInfo.getName());
    }

    @Override
    public String getIntrospectorName() {
        return "LibertyResteasyEndpointIntrospector";
    }

    @Override
    public String getIntrospectorDescription() {
        return "Log the JAX-RS endpoints for each Application.";
    }

    @Override
    public void introspect(PrintWriter out) throws Exception {
        for (String key : applicationEndpoints.keySet()) {
            out.println(key + " endpoints:");
            for (String ep : applicationEndpoints.get(key)) {
                out.println(ep);
            }
            out.println();
        }
    }

    @Override
    public String addEndpoint(String appName, String httpMethod, String endpoint, String className, String methodName) {
        StringBuilder sb = new StringBuilder();
        sb.append("- ");
        sb.append(httpMethod);
        sb.append(" ");
        sb.append(endpoint);
        sb.append(" - ");
        sb.append(className);
        sb.append("#");
        sb.append(methodName);
        sb.append("()");
        String endpointInfo = sb.toString();
        
        // appName key and it's List should always exist because we initialized during applicationStarting
        List<String> endpoints = applicationEndpoints.get(appName);
        if (endpoints != null) {
            endpoints.add(endpointInfo);
        } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Attempting to add endpoint to unknown application: " + appName + ". Known apps: " + applicationEndpoints.keySet());
        }
        
        return endpointInfo;
    }
}
