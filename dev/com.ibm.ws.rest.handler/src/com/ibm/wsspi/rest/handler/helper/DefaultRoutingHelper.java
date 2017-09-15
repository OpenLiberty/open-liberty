/*******************************************************************************
 * Copyright (c) 2014, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.rest.handler.helper;

import java.io.IOException;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.rest.handler.RESTHandlerContainer;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;
import com.ibm.wsspi.webcontainer.util.RequestUtils;

/**
 * This helper service routes/bridges an incoming request to and from a RESTHandler that resides in another Collective member.
 * 
 * @ibm-spi
 */
public class DefaultRoutingHelper {

    /**
     * Encapsulates legacy constants for JMX connector clients v1, v2 and v3.
     */
    public interface LegacyJMX {
        /**
         * JMC Connector root URI.
         */
        public static final String CONNECTOR_URI = "IBMJMXConnectorREST";

        /**
         * Router prefix URI.
         */
        public static final String ROUTER_URI = CONNECTOR_URI + "/router";

        /**
         * This parameter represents the host name to be used in a routing context.
         */
        public static final String ROUTING_KEY_HOST_NAME = "com.ibm.websphere.jmx.connector.rest.routing.hostName";

        /**
         * This parameter represents the server name to be used in a routing context.
         */
        public static final String ROUTING_KEY_SERVER_NAME = "com.ibm.websphere.jmx.connector.rest.routing.serverName";

        /**
         * This parameter represents the server user directory to be used in a routing context.
         */
        public static final String ROUTING_KEY_SERVER_USER_DIR = "com.ibm.websphere.jmx.connector.rest.routing.serverUserDir";
    }

    private static final TraceComponent tc = Tr.register(DefaultRoutingHelper.class);

    protected void activate(ComponentContext cc, Map<String, Object> props) {
    }

    protected void deactivate(ComponentContext cc) {
    }

    protected void setCollectivePlugin(ServiceReference<RESTRoutingHelper> ref) {
    }

    protected void unsetCollectivePlugin(ServiceReference<RESTRoutingHelper> ref) {
    }

    /**
     * The target RESTHandler did not want to provide custom routing, so route the request to it.
     * 
     */
    public void routeRequest(RESTRequest request, RESTResponse response) throws IOException {
    }

    /**
     * The target RESTHandler did not want to provide custom routing, so route the request to it.
     * 
     * @param request
     * @param response
     * @param legacyURI whether or not the request is using the legacy /router URI
     */
    public void routeRequest(RESTRequest request, RESTResponse response, boolean legacyURI) throws IOException {
    }

    /**
     * Quick check for legacy routing context (used from JMX connector)
     */
    public static boolean containsLegacyRoutingContext(RESTRequest request) {
        return request.getHeader(LegacyJMX.ROUTING_KEY_HOST_NAME) != null;
    }

    /**
     * Quick check for multiple routing context, without actually fetching all pieces
     */
    public static boolean containsRoutingContext(RESTRequest request) {
        if (request.getHeader(RESTHandlerContainer.COLLECTIVE_HOST_NAMES) != null) {
            return true;
        }

        //No routing header found, so check query strings
        return getQueryParameterValue(request, RESTHandlerContainer.COLLECTIVE_HOST_NAMES) != null;
    }

    // Use this method for parsing query string for POST. Using RESTRequest's method will read the
    // request's body once and for all.
    public static String getQueryParameterValue(RESTRequest request, String name) {
        if (!"post".equalsIgnoreCase(request.getMethod())) {
            return request.getParameter(name);
        }

        if (request.getQueryString() == null) {
            return null;
        }

        Hashtable params = null;
        try {
            params = RequestUtils.parseQueryString(request.getQueryString());
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event("DefaultRoutingHelper", tc, "Failed to parse the query string:\n Exception: " + e);
            }
            return null;
        }

        String[] values = (String[]) params.get(name);
        String value = null;
        if (values != null && values.length > 0)
        {
            value = values[0];
        }

        return value;
    }

    // Use this method for parsing query string for POST. Using RESTRequest's method will read the
    // request's body once and for all.
    public static String[] getQueryParameterValues(RESTRequest request, String name) {
        if (!"post".equalsIgnoreCase(request.getMethod())) {
            return request.getParameterValues(name);
        }

        if (request.getQueryString() == null) {
            return null;
        }

        Hashtable params = null;
        try {
            params = RequestUtils.parseQueryString(request.getQueryString());
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event("DefaultRoutingHelper", tc, "Failed to parse the query string:\n Exception: " + e);
            }
            return null;
        }

        return (String[]) params.get(name);
    }

    /**
     * This helper method looks for the routing keys in the HTTP headers
     * 
     * @param httpServletRequest of the current request
     * @return a 3-sized String array containing hostName, userDir and serverName respectively, or null if no routing context was found.
     */
    public static RoutingContext getLegacyRoutingContext(RESTRequest request) {
        return null;
    }

    public static String URLDecoder(String name) {
        return null;
    }

    /**
     * This helper method looks for the routing keys in the HTTP headers first, and then falls-back into looking at the query string.
     * 
     * @param httpServletRequest of the current request
     * @return a list of routing contexts, or null if none found.
     */
    public static List<RoutingContext> getRoutingContext(RESTRequest request) {
        return null;
    }

    /**
     * This inner class encapsulates the routing context.
     */
    public static class RoutingContext {
        final public String hostName;
        final public String serverInstallDir;
        final public String serverUserDir;
        final public String serverName;

        public RoutingContext(String hostName, String serverUserDir, String serverName) {
            this(hostName, null, serverUserDir, serverName);
        }

        public RoutingContext(String hostName, String serverInstallDir, String serverUserDir, String serverName) {
            this.hostName = hostName;
            this.serverInstallDir = serverInstallDir;
            this.serverUserDir = serverUserDir;
            this.serverName = serverName;
        }
    }
}
