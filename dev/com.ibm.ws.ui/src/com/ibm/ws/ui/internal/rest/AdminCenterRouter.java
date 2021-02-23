/*******************************************************************************
 * Copyright (c) 2013, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ui.internal.rest;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ui.internal.RequestNLS;
import com.ibm.ws.ui.internal.rest.v1.CatalogAPI;
import com.ibm.ws.ui.internal.rest.v1.DeployValidation;
import com.ibm.ws.ui.internal.rest.v1.IconRestHandler;
import com.ibm.ws.ui.internal.rest.v1.ToolDataAPI;
import com.ibm.ws.ui.internal.rest.v1.ToolboxAPI;
import com.ibm.ws.ui.internal.rest.v1.V1Root;
import com.ibm.ws.ui.internal.rest.v1.utils.FeatureUtils;
import com.ibm.ws.ui.internal.rest.v1.utils.URLUtils;
import com.ibm.ws.ui.internal.rest.v1.utils.UtilsRoot;
import com.ibm.ws.ui.internal.v1.ICatalogService;
import com.ibm.ws.ui.internal.v1.IFeatureToolService;
import com.ibm.ws.ui.internal.v1.IToolDataService;
import com.ibm.ws.ui.internal.v1.IToolboxService;
import com.ibm.wsspi.rest.handler.RESTHandler;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 * <p>Defines the URL router for adminCenter REST API.</p>
 * 
 * <p>Maps to host:port/ibm/api/adminCenter</p>
 */
@Component(service = { RESTHandler.class },
           configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           property = { "service.vendor=IBM",
                        RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.ADMIN_CENTER_ROOT_PATH,
                        RESTHandler.PROPERTY_REST_HANDLER_CUSTOM_SECURITY + "=" + "true" })
public class AdminCenterRouter implements RESTHandler, HTTPConstants {
    private static transient final TraceComponent tc = Tr.register(AdminCenterRouter.class);

    private ICatalogService catalogService;
    private IToolboxService toolboxService;
    private IToolDataService tooldataService;
    private IFeatureToolService featureToolService;

    /**
     * Map of the AdminCenterRestHandlers. Keys are the URLs which the handler
     * is registered to handle. Once set, this map can not be modified.
     */
    private Map<String, AdminCenterRestHandler> handlers;

    /**
     * Default constructor used by OSGi DS to instantiate the instance.
     */
    public AdminCenterRouter() {}

    @Reference(service = ICatalogService.class)
    protected void setICatalogService(ICatalogService catalogService) {
        this.catalogService = catalogService;
    }

    protected void unsetICatalogService(ICatalogService catalogService) {
        if (this.catalogService == catalogService) {
            this.catalogService = null;
        }
    }

    @Reference(service = IToolboxService.class)
    protected void setIToolboxService(IToolboxService toolboxService) {
        this.toolboxService = toolboxService;
    }

    protected void unsetIToolboxService(IToolboxService toolboxService) {
        if (this.toolboxService == toolboxService) {
            this.toolboxService = null;
        }
    }

    @Reference(service = IToolDataService.class)
    protected void setITooldataService(IToolDataService tooldataService) {
        this.tooldataService = tooldataService;
    }

    protected void unsetITooldataService(IToolDataService tooldataService) {
        if (this.tooldataService == tooldataService) {
            this.tooldataService = null;
        }
    }

    /**
     * The injection point for the IFeatureToolService that allows us to get feature tools.
     * 
     * @param variableRegistryService - The variableRegistry service
     */
    @Reference(service = IFeatureToolService.class)
    protected void setIFeatureToolService(IFeatureToolService featureToolService) {
        this.featureToolService = featureToolService;
    }

    protected void unsetIFeatureToolService(IFeatureToolService featureToolService) {
        if (this.featureToolService == featureToolService) {
            this.featureToolService = null;
        }
    }

    /**
     * Add the specified handler to the set of default handlers.
     * 
     * @param defaultHandlers
     * @param handler
     */
    private final void addHandler(Map<String, AdminCenterRestHandler> defaultHandlers,
                                  AdminCenterRestHandler handler) {
        defaultHandlers.put(handler.baseURL(), handler);
    }

    /**
     * Construct the AdminCenterRouter with all of the known URL handlers.
     * We have taken this approach so we are 100% guaranteed that once adminCenter/
     * is ready, all of our API URLs are ready too.
     */
    @Activate
    protected void activate() {
        Map<String, AdminCenterRestHandler> defaultHandlers = new HashMap<String, AdminCenterRestHandler>();

        addHandler(defaultHandlers, new APIRoot());
        addHandler(defaultHandlers, new V1Root());
        addHandler(defaultHandlers, new CatalogAPI(catalogService));
        addHandler(defaultHandlers, new ToolboxAPI(toolboxService));
        addHandler(defaultHandlers, new ToolDataAPI(tooldataService, toolboxService));
        addHandler(defaultHandlers, new UtilsRoot());
        addHandler(defaultHandlers, new FeatureUtils(featureToolService));
        addHandler(defaultHandlers, new URLUtils());
        addHandler(defaultHandlers, new IconRestHandler(featureToolService));
        addHandler(defaultHandlers, new DeployValidation());
        this.handlers = Collections.unmodifiableMap(defaultHandlers);
    }

    @Deactivate
    protected void deactivate() {
        handlers = null;
    }

    /**
     * For unit testing.
     * 
     * @param handlers
     */
    AdminCenterRouter(Map<String, AdminCenterRestHandler> handlers) {
        this.handlers = handlers;
    }

    /**
     * Try to find the appropriate rest handler for the given URL.
     * Return null if no match found.
     * 
     * @param requestURL The URL from the HTTP request. This is the URL that needs to be matched.
     * @return The RESTHandler for the given URL.
     */
    private AdminCenterRestHandler getHandler(final String requestURL) {
        // No request URL, no handler
        if (requestURL == null) {
            return null;
        }

        // No handlers, return null
        if (handlers.keySet().isEmpty()) {
            return null;
        }

        // If we have a direct hit, return it
        AdminCenterRestHandler handler = handlers.get(requestURL);
        if (handler != null) {
            return handler;
        }

        // If no direct match, then do a simple string match.
        // The longest one wins
        String bestMatchRoot = "";
        for (Entry<String, AdminCenterRestHandler> entry : handlers.entrySet()) {
            String key = entry.getKey();
            AdminCenterRestHandler keyHandler = entry.getValue();
            // Requirement 1: the handler URL must be a substring of the request URL
            if (!requestURL.startsWith(key)) {
                continue;
            }

            // Requirement 2: the request URL must not be a non-child superstring of the handler URL
            // e.g. handlerURL=/test, requestURL=/testing - these must not match
            // Grabbing the charAt here is safe because if we were an exact length match,
            // we would have been caught in the initial direct hit check.
            char nextChar = requestURL.charAt(key.length());
            //the only characters allowed after the key is '/' (for subpath)
            if (nextChar != '/') {
                continue;
            }

            // Requirement 3: the handler must support children if this is a possible child
            if (requestURL.length() > key.length() + 1) {
                if (!keyHandler.hasChildren()) {
                    continue;
                }
            }

            // Passed all requirements, update best match if this key is longer than previous
            if (key.length() > bestMatchRoot.length()) {
                bestMatchRoot = key;
            }
        }

        // If we found a match...
        if (bestMatchRoot.length() > 0) {
            return handlers.get(bestMatchRoot);
        }
        return null;
    }

    /**
     * {@inheritDoc} <p>Handles the URL to handler mapping for all adminCenter
     * requests. This is an additional layer beyond the RestHandler's routing
     * and was introduced to ensure that when the ibm/api/adminCenter URL is
     * available, ALL of the API URL handlers will be ready too.
     */
    @Override
    public void handleRequest(final RESTRequest request, final RESTResponse response) throws IOException {
        if (tc.isEventEnabled()) {
            Tr.event(tc, "REST request received from " + request.getRemoteHost() + ":" + request.getRemotePort() + " - path: " + request.getPath());
        }

        final RESTHandler handler = getHandler(request.getPath());
        if (handler != null) {
            try {
                RequestNLS.setRESTRequest(request);
                handler.handleRequest(request, response);
            } finally {
                RequestNLS.clearThreadLocal();
            }
        } else {
            response.setStatus(HTTP_NOT_FOUND);
        }
    }

    /**
     * Returns a read-only view of the handlers map.
     * Primarily used for unit testing.
     * 
     * @return a read-only view of the handlers map.
     */
    Map<String, AdminCenterRestHandler> getHandlers() {
        return handlers;
    }

}
