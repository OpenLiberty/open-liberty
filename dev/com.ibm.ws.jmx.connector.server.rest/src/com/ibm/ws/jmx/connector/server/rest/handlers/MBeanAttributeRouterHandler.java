/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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
import java.util.List;

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
import com.ibm.ws.jmx.connector.server.rest.helpers.AttributeRoutingHelper;
import com.ibm.ws.jmx.connector.server.rest.helpers.ErrorHelper;
import com.ibm.ws.jmx.connector.server.rest.helpers.RESTHelper;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.rest.handler.RESTHandler;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;
import com.ibm.wsspi.rest.handler.helper.RESTHandlerMethodNotAllowedError;

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
                       RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.PATH_ROUTER_MBEANS_OBJECTNAME_ATTRIBUTES,
                       RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.PATH_ROUTER_MBEANS_OBJECTNAME_ATTRIBUTES_ATTRIBUTE })
public class MBeanAttributeRouterHandler implements RESTHandler {
    public static final TraceComponent tc = Tr.register(MBeanAttributeRouterHandler.class);

    private final String KEY_ROUTING_HELPER = "routingHelper";
    private final AtomicServiceReference<AttributeRoutingHelper> routingHelperRef = new AtomicServiceReference<AttributeRoutingHelper>(KEY_ROUTING_HELPER);

    @Activate
    protected void activate(ComponentContext context) {
        routingHelperRef.activate(context);
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        routingHelperRef.deactivate(context);
    }

    @Reference(service = AttributeRoutingHelper.class,
               name = KEY_ROUTING_HELPER,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setRoutingHelper(ServiceReference<AttributeRoutingHelper> routingHelper) {
        routingHelperRef.setReference(routingHelper);
    }

    protected void unsetRoutingHelper(ServiceReference<AttributeRoutingHelper> routingHelper) {
        routingHelperRef.unsetReference(routingHelper);
    }

    @Override
    public void handleRequest(RESTRequest request, RESTResponse response) {
        String method = request.getMethod();
        if (request.getPathVariable(APIConstants.PARAM_ATTRIBUTE) != null) {
            if (RESTHelper.isGetMethod(method)) {
                getRoutedAttribute(request, response);
            } else if (RESTHelper.isPostMethod(method)) {
                postRoutedAttribute(request, response);
            } else if (RESTHelper.isPutMethod(method)) {
                putRoutedAttribute(request, response);
            } else if (RESTHelper.isDeleteMethod(method)) {
                deleteRoutedAttribute(request, response);
            } else {
                throw new RESTHandlerMethodNotAllowedError("GET,POST,PUT,DELETE");
            }
        } else {
            if (RESTHelper.isGetMethod(method)) {
                getRoutedAttributes(request, response);
            } else if (RESTHelper.isPostMethod(method)) {
                postRoutedAttributes(request, response);
            } else if (RESTHelper.isPutMethod(method)) {
                putRoutedAttributes(request, response);
            } else if (RESTHelper.isDeleteMethod(method)) {
                deleteRoutedAttributes(request, response);
            } else {
                throw new RESTHandlerMethodNotAllowedError("GET,POST,PUT,DELETE");
            }
        }
    }

    /**
     * @Legacy
     */
    private void getRoutedAttributes(RESTRequest request, RESTResponse response) {
        String objectName = RESTHelper.getRequiredParam(request, APIConstants.PARAM_OBJECT_NAME);
        List<String> queryAttributes = RESTHelper.getQueryParams(request, APIConstants.PARAM_ATTRIBUTE);

        getRoutingHelper().getAttributes(request, response, objectName, queryAttributes, true);

        response.setContentType(APIConstants.MEDIA_TYPE_APPLICATION_JSON);
    }

    /**
     * @Legacy
     */
    private void getRoutedAttribute(RESTRequest request, RESTResponse response) {
        String objectName = RESTHelper.getRequiredParam(request, APIConstants.PARAM_OBJECT_NAME);
        String attribute = RESTHelper.getRequiredParam(request, APIConstants.PARAM_ATTRIBUTE);

        getRoutingHelper().getAttribute(request, response, objectName, attribute, true);

        response.setContentType(APIConstants.MEDIA_TYPE_APPLICATION_JSON);
    }

    /**
     * @Legacy
     */
    private void putRoutedAttributes(RESTRequest request, RESTResponse response) {
        RESTHelper.getRequiredParam(request, APIConstants.PARAM_OBJECT_NAME);

        getRoutingHelper().setAttributes(request, response, true);

        response.setContentType(APIConstants.MEDIA_TYPE_APPLICATION_JSON);
    }

    /**
     * @Legacy
     */
    private void putRoutedAttribute(RESTRequest request, RESTResponse response) {
        RESTHelper.getRequiredParam(request, APIConstants.PARAM_OBJECT_NAME);
        RESTHelper.getRequiredParam(request, APIConstants.PARAM_ATTRIBUTE);

        getRoutingHelper().setAttribute(request, response, true);

        response.setContentType(APIConstants.MEDIA_TYPE_APPLICATION_JSON);
    }

    /**
     * @Legacy
     */
    private void postRoutedAttributes(RESTRequest request, RESTResponse response) {
        RESTHelper.getRequiredParam(request, APIConstants.PARAM_OBJECT_NAME);

        getRoutingHelper().setAttributes(request, response, true);

        response.setContentType(APIConstants.MEDIA_TYPE_APPLICATION_JSON);
    }

    /**
     * @Legacy
     */
    private void postRoutedAttribute(RESTRequest request, RESTResponse response) {
        RESTHelper.getRequiredParam(request, APIConstants.PARAM_OBJECT_NAME);
        RESTHelper.getRequiredParam(request, APIConstants.PARAM_ATTRIBUTE);

        getRoutingHelper().setAttribute(request, response, true);

        response.setContentType(APIConstants.MEDIA_TYPE_APPLICATION_JSON);
    }

    /**
     * @Legacy
     */
    private void deleteRoutedAttributes(RESTRequest request, RESTResponse response) {
        RESTHelper.getRequiredParam(request, APIConstants.PARAM_OBJECT_NAME);

        getRoutingHelper().deleteAttributes(request, response, true);

        response.setContentType(APIConstants.MEDIA_TYPE_APPLICATION_JSON);
    }

    /**
     * @Legacy
     */
    private void deleteRoutedAttribute(RESTRequest request, RESTResponse response) {
        RESTHelper.getRequiredParam(request, APIConstants.PARAM_OBJECT_NAME);
        RESTHelper.getRequiredParam(request, APIConstants.PARAM_ATTRIBUTE);

        getRoutingHelper().deleteAttribute(request, response, true);

        response.setContentType(APIConstants.MEDIA_TYPE_APPLICATION_JSON);
    }

    private AttributeRoutingHelper getRoutingHelper() {
        AttributeRoutingHelper routingHelper = routingHelperRef.getService();

        if (routingHelper == null) {
            IOException ioe = new IOException(TraceNLS.getFormattedMessage(this.getClass(),
                                                                           APIConstants.TRACE_BUNDLE_FILE_TRANSFER,
                                                                           "OSGI_SERVICE_ERROR",
                                                                           new Object[] { "AttributeRoutingHelper" },
                                                                           "CWWKX0122E: OSGi service is not available."));
            throw ErrorHelper.createRESTHandlerJsonException(ioe, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        }

        return routingHelper;
    }
}
