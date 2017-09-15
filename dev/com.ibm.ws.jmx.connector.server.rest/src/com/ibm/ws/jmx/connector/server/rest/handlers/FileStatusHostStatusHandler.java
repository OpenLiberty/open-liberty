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
                       RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.PATH_FILE_STATUS_TASKID_HOSTS,
                       RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.PATH_FILE_STATUS_TASKID_HOSTS_HOST })
public class FileStatusHostStatusHandler implements RESTHandler {
    public static final TraceComponent tc = Tr.register(FileStatusHostStatusHandler.class);

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
    public void handleRequest(RESTRequest request, RESTResponse response) {
        boolean foundHandler = false;
        String method = request.getMethod();
        if (request.getPathVariable(APIConstants.PARAM_HOST) != null) {
            if (RESTHelper.isGetMethod(method)) {
                taskHostStatus(request, response);
                foundHandler = true;
            }
        } else {
            if (RESTHelper.isGetMethod(method)) {
                taskHosts(request, response);
                foundHandler = true;
            }
        }

        if (!foundHandler) {
            throw new RESTHandlerMethodNotAllowedError("GET");
        }
    }

    /**
     * Returns a JSON array of {hostName, status, } tuple, representing the host names associated with that task (no particular order).
     * 
     * [ {"hostName" : "host", "hostStatus" : "status", "hostURL" : "url"}* ]
     */
    private void taskHosts(RESTRequest request, RESTResponse response) {
        String taskID = RESTHelper.getRequiredParam(request, APIConstants.PARAM_TASK_ID);

        String taskHostsJson = getMultipleRoutingHelper().getHosts(taskID);
        OutputHelper.writeJsonOutput(response, taskHostsJson);
    }

    /**
     * Returns a JSON array of CommandResult serialization, representing the steps taken in that host
     * 
     * [
     * {
     * "timestamp" : String,
     * "status" : String,
     * "description" : String,
     * "returnCode" : int,
     * "StdOut" : String,
     * "StdErr" : String
     * }*
     * ]
     * 
     * 
     */
    private void taskHostStatus(RESTRequest request, RESTResponse response) {
        String taskID = RESTHelper.getRequiredParam(request, APIConstants.PARAM_TASK_ID);
        String host = RESTHelper.getRequiredParam(request, APIConstants.PARAM_HOST);

        String taskHostStatusJson = getMultipleRoutingHelper().getHostDetails(taskID, host);
        OutputHelper.writeJsonOutput(response, taskHostStatusJson);
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