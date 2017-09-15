/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.connector.server.rest.handlers;

import java.io.IOException;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jmx.connector.server.rest.APIConstants;
import com.ibm.ws.jmx.connector.server.rest.helpers.ErrorHelper;
import com.ibm.ws.jmx.connector.server.rest.helpers.RESTHelper;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.rest.handler.RESTHandler;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;
import com.ibm.wsspi.rest.handler.helper.RESTHandlerMethodNotAllowedError;
import com.ibm.wsspi.rest.handler.helper.RESTRoutingHelper;

/**
 * Legacy code for V2 and V3 clients ONLY.
 * 
 * DO NOT add new functionality or change behaviour.
 */
@Component(service = { RESTHandler.class },
           configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           property = { "service.vendor=IBM",
                       RESTHandler.PROPERTY_REST_HANDLER_CUSTOM_ROUTING + "=true",
                       RESTHandler.PROPERTY_REST_HANDLER_CONTEXT_ROOT + "=" + APIConstants.JMX_CONNECTOR_API_ROOT_PATH,
                       RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.PATH_ROUTER,
                       RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.PATH_ROUTER_URI })
public class MBeanRouterHandler implements RESTHandler {
    public static final TraceComponent tc = Tr.register(MBeanRouterHandler.class);

    private final String KEY_ROUTING_HELPER = "routingHelper";
    private final AtomicServiceReference<RESTRoutingHelper> routingHelperRef = new AtomicServiceReference<RESTRoutingHelper>(KEY_ROUTING_HELPER);

    @Activate
    protected void activate(ComponentContext context) {
        routingHelperRef.activate(context);
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        routingHelperRef.deactivate(context);
    }

    @Reference(service = RESTRoutingHelper.class,
               name = KEY_ROUTING_HELPER,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setRoutingHelper(ServiceReference<RESTRoutingHelper> routingHelper) {
        routingHelperRef.setReference(routingHelper);
    }

    protected void unsetRoutingHelper(ServiceReference<RESTRoutingHelper> routingHelper) {
        routingHelperRef.unsetReference(routingHelper);
    }

    @Override
    public void handleRequest(RESTRequest request, RESTResponse response) {
        String method = request.getMethod();
        if (request.getPathVariable(APIConstants.PARAM_URI) != null) {
            if (RESTHelper.isGetMethod(method)) {
                getProxy(request, response);
            } else if (RESTHelper.isPostMethod(method)) {
                postProxy(request, response);
            } else if (RESTHelper.isPutMethod(method)) {
                putProxy(request, response);
            } else if (RESTHelper.isDeleteMethod(method)) {
                deleteProxy(request, response);
            } else {
                throw new RESTHandlerMethodNotAllowedError("GET,POST,PUT,DELETE");
            }
        } else {
            if (RESTHelper.isGetMethod(method)) {
                getProxyRoot(request, response);
            } else {
                throw new RESTHandlerMethodNotAllowedError("GET");
            }
        }
    }

    /**
     * @Legacy
     */
    private void getProxyRoot(RESTRequest request, RESTResponse response) {
        try {
            getRoutingHelper().routeRequest(request, response, true);
        } catch (Exception e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        }

        response.setContentType(APIConstants.MEDIA_TYPE_APPLICATION_JSON);
    }

    /**
     * @Legacy
     */
    private void getProxy(RESTRequest request, RESTResponse response) {
        RESTHelper.getRequiredParam(request, APIConstants.PARAM_URI);

        try {
            getRoutingHelper().routeRequest(request, response, true);
        } catch (Exception e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        }

        response.setContentType(APIConstants.MEDIA_TYPE_APPLICATION_JSON);
    }

    /**
     * @Legacy
     */
    private void putProxy(RESTRequest request, RESTResponse response) {
        RESTHelper.getRequiredParam(request, APIConstants.PARAM_URI);

        try {
            getRoutingHelper().routeRequest(request, response, true);
        } catch (Exception e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        }

        response.setContentType(APIConstants.MEDIA_TYPE_APPLICATION_JSON);
    }

    /**
     * @Legacy
     */
    private void postProxy(RESTRequest request, RESTResponse response) {
        RESTHelper.getRequiredParam(request, APIConstants.PARAM_URI);

        try {
            getRoutingHelper().routeRequest(request, response, true);
        } catch (Exception e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        }

        response.setContentType(APIConstants.MEDIA_TYPE_APPLICATION_JSON);
    }

    /**
     * @Legacy
     */
    private void deleteProxy(RESTRequest request, RESTResponse response) {
        RESTHelper.getRequiredParam(request, APIConstants.PARAM_URI);

        try {
            getRoutingHelper().routeRequest(request, response, true);
        } catch (Exception e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        }

        response.setContentType(APIConstants.MEDIA_TYPE_APPLICATION_JSON);
    }

    private RESTRoutingHelper getRoutingHelper() throws IOException {
        RESTRoutingHelper routingHelper = routingHelperRef.getService();

        if (routingHelper == null) {
            IOException ioe = new IOException(TraceNLS.getFormattedMessage(this.getClass(),
                                                                           APIConstants.TRACE_BUNDLE_FILE_TRANSFER,
                                                                           "OSGI_SERVICE_ERROR",
                                                                           new Object[] { "RESTRoutingHelper" },
                                                                           "CWWKX0122E: OSGi service is not available."));
            throw ErrorHelper.createRESTHandlerJsonException(ioe, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        }

        return routingHelper;
    }
}
