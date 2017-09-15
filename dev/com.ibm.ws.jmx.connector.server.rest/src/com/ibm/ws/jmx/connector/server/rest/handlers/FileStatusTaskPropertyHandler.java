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

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jmx.connector.server.rest.APIConstants;
import com.ibm.ws.jmx.connector.server.rest.helpers.ErrorHelper;
import com.ibm.ws.jmx.connector.server.rest.helpers.MultipleRoutingHelper;
import com.ibm.ws.jmx.connector.server.rest.helpers.OutputHelper;
import com.ibm.ws.jmx.connector.server.rest.helpers.RESTHelper;
import com.ibm.wsspi.rest.handler.RESTHandler;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;
import com.ibm.wsspi.rest.handler.helper.RESTHandlerMethodNotAllowedError;

@Component(service = { RESTHandler.class },
           configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           property = { "service.vendor=IBM",
                       RESTHandler.PROPERTY_REST_HANDLER_CONTEXT_ROOT + "=" + APIConstants.JMX_CONNECTOR_API_ROOT_PATH,
                       RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.PATH_FILE_STATUS_TASKID_PROPERTIES,
                       RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.PATH_FILE_STATUS_TASKID_PROPERTIES_PROPERTY })
public class FileStatusTaskPropertyHandler implements RESTHandler {
    public static final TraceComponent tc = Tr.register(FileStatusTaskPropertyHandler.class);

    public static final String ROOT_URL = APIConstants.JMX_CONNECTOR_API_ROOT_PATH + APIConstants.PATH_FILE_STATUS;

    private transient MultipleRoutingHelper multipleRoutingHelper;
    private transient ComponentContext componentContext;

    @Activate
    protected void activate(ComponentContext context) {
        componentContext = context;
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        componentContext = null;
    }

    @Override
    public void handleRequest(RESTRequest request, RESTResponse response) throws IOException {
        boolean foundHandler = false;
        String method = request.getMethod();
        if (request.getPathVariable(APIConstants.PARAM_PROPERTY) != null) {
            if (RESTHelper.isGetMethod(method)) {
                taskProperty(request, response);
                foundHandler = true;
            }
        } else {
            if (RESTHelper.isGetMethod(method)) {
                taskProperties(request, response);
                foundHandler = true;
            }
        }

        if (!foundHandler) {
            throw new RESTHandlerMethodNotAllowedError("GET");
        }
    }

    /**
     * Returns the list of available properties and their corresponding URLs.
     * 
     */
    private void taskProperties(RESTRequest request, RESTResponse response) {
        String taskID = RESTHelper.getRequiredParam(request, APIConstants.PARAM_TASK_ID);

        String taskPropertiesText = getMultipleRoutingHelper().getTaskProperties(taskID);
        OutputHelper.writeTextOutput(response, taskPropertiesText);
    }

    /**
     * Returns the value of the property. An IllegalArgument exception is thrown if the value is not an instance of java.lang.String.
     * 
     */
    private void taskProperty(RESTRequest request, RESTResponse response) {
        String taskID = RESTHelper.getRequiredParam(request, APIConstants.PARAM_TASK_ID);
        String property = RESTHelper.getRequiredParam(request, APIConstants.PARAM_PROPERTY);

        String taskPropertyText = getMultipleRoutingHelper().getTaskProperty(taskID, property);
        OutputHelper.writeTextOutput(response, taskPropertyText);
    }

    private synchronized MultipleRoutingHelper getMultipleRoutingHelper() {
        if (multipleRoutingHelper == null) {
            BundleContext bc = componentContext.getBundleContext();
            ServiceReference<MultipleRoutingHelper> multipleRoutingHelperRef = bc.getServiceReference(MultipleRoutingHelper.class);

            multipleRoutingHelper = multipleRoutingHelperRef != null ? bc.getService(multipleRoutingHelperRef) : null;

            if (multipleRoutingHelper == null) {
                IOException ioe = new IOException(TraceNLS.getFormattedMessage(this.getClass(),
                                                                               APIConstants.TRACE_BUNDLE_FILE_TRANSFER,
                                                                               "OSGI_SERVICE_ERROR",
                                                                               new Object[] { "MultipleRoutingHelper" },
                                                                               "CWWKX0122E: OSGi service is not available."));
                throw ErrorHelper.createRESTHandlerJsonException(ioe, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
            }
        }

        return multipleRoutingHelper;
    }
}