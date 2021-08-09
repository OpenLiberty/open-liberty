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

import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jmx.connector.converter.JSONConverter;
import com.ibm.ws.jmx.connector.datatypes.ServerNotificationRegistration;
import com.ibm.ws.jmx.connector.datatypes.ServerNotificationRegistration.Operation;
import com.ibm.ws.jmx.connector.server.rest.APIConstants;
import com.ibm.ws.jmx.connector.server.rest.helpers.ErrorHelper;
import com.ibm.ws.jmx.connector.server.rest.helpers.OutputHelper;
import com.ibm.ws.jmx.connector.server.rest.helpers.RESTHelper;
import com.ibm.ws.jmx.connector.server.rest.notification.NotificationManager;
import com.ibm.wsspi.rest.handler.RESTHandler;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;
import com.ibm.wsspi.rest.handler.helper.RESTHandlerMethodNotAllowedError;

@Component(service = { RESTHandler.class },
           configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           property = {
                       "service.vendor=IBM",
                       RESTHandler.PROPERTY_REST_HANDLER_CUSTOM_ROUTING + "=true",
                       RESTHandler.PROPERTY_REST_HANDLER_CONTEXT_ROOT + "=" + APIConstants.JMX_CONNECTOR_API_ROOT_PATH,
                       RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.PATH_NOTIFICATIONS_CLIENTID_SERVERREGISTRATIONS_SOURCEOBJNAME_LISTENERS,
                       RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.PATH_NOTIFICATIONS_CLIENTID_SERVERREGISTRATIONS_SOURCEOBJNAME_LISTENERS_LISTENEROBJNAME_IDS,
                       RESTHandler.PROPERTY_REST_HANDLER_ROOT + "="
                                       + APIConstants.PATH_NOTIFICATIONS_CLIENTID_SERVERREGISTRATIONS_SOURCEOBJNAME_LISTENERs_LISTENEROBJNAME_IDS_REGISTRATIONID })
public class NotificationListenerHandler implements RESTHandler {
    public static final TraceComponent tc = Tr.register(NotificationListenerHandler.class);

    @Activate
    protected void activate(ComponentContext context, Map<String, Object> properties) {}

    @Deactivate
    protected void deactivate(ComponentContext context, int reason) {}

    @Override
    public void handleRequest(RESTRequest request, RESTResponse response) {
        String method = request.getMethod();
        if (request.getPathVariable("registrationID") != null) {
            if (RESTHelper.isGetMethod(method)) {
                getSpecificServerRegistration(request, response);
            } else if (RESTHelper.isDeleteMethod(method)) {
                deleteSpecificServerRegistration(request, response);
            } else {
                throw new RESTHandlerMethodNotAllowedError("GET,DELETE");
            }
        } else if (request.getPathVariable("listener_objName") != null) {
            if (RESTHelper.isGetMethod(method)) {
                getRegisteredIDs(request, response);
            } else if (RESTHelper.isDeleteMethod(method)) {
                deleteListenerServerRegistrations(request, response);
            } else {
                throw new RESTHandlerMethodNotAllowedError("GET,DELETE");
            }
        } else {
            if (RESTHelper.isGetMethod(method)) {
                getRegisteredListeners(request, response);
            } else if (RESTHelper.isDeleteMethod(method)) {
                deleteRegisteredListeners(request, response);
            } else {
                throw new RESTHandlerMethodNotAllowedError("GET,DELETE");
            }
        }
    }

    private void getSpecificServerRegistration(RESTRequest request, RESTResponse response) {
        String source_objName = RESTHelper.getRequiredParam(request, APIConstants.PARAM_SOURCE_OBJNAME);
        String listener_objName = RESTHelper.getRequiredParam(request, APIConstants.PARAM_LISTENER_OBJNAME);
        String registrationID = RESTHelper.getRequiredParam(request, APIConstants.PARAM_REGISTRATIONID);
        String clientIDString = RESTHelper.getRequiredParam(request, APIConstants.PARAM_CLIENTID);

        int clientID = -1;
        try {
            clientID = Integer.parseInt(clientIDString);
        } catch (NumberFormatException e) {
            ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_BAD_REQUEST);
        }

        ServerNotificationRegistration registration = NotificationManager.getNotificationManager()
                        .getSpecificServerRegistration(request, clientID, source_objName, listener_objName, registrationID);

        if (registration != null) {
            OutputHelper.writeServerRegistrationStreamingOutput(response, registration, JSONConverter.getConverter());
        } else {
            response.setStatus(APIConstants.STATUS_NO_CONTENT);
        }
    }

    private void deleteSpecificServerRegistration(RESTRequest request, RESTResponse response) {
        String source_objName = RESTHelper.getRequiredParam(request, APIConstants.PARAM_SOURCE_OBJNAME);
        String listener_objName = RESTHelper.getRequiredParam(request, APIConstants.PARAM_LISTENER_OBJNAME);
        String registrationID = RESTHelper.getRequiredParam(request, APIConstants.PARAM_REGISTRATIONID);
        String clientIDString = RESTHelper.getRequiredParam(request, APIConstants.PARAM_CLIENTID);

        int clientID = -1;
        try {
            clientID = Integer.parseInt(clientIDString);
        } catch (NumberFormatException e) {
            ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_BAD_REQUEST);
        }

        //Get the converter
        JSONConverter converter = JSONConverter.getConverter();

        //We'll build the server registration ourselves
        ServerNotificationRegistration serverNotificationRegistration = new ServerNotificationRegistration();
        serverNotificationRegistration.objectName = RESTHelper.objectNameConverter(source_objName, true, converter);
        serverNotificationRegistration.listener = RESTHelper.objectNameConverter(listener_objName, true, converter);
        serverNotificationRegistration.operation = Operation.RemoveSpecific;

        NotificationManager.getNotificationManager()
                        .deleteServerRegistrationHTTP(request, clientID, serverNotificationRegistration, registrationID, converter);
        response.setStatus(APIConstants.STATUS_NO_CONTENT);
    }

    private void getRegisteredIDs(RESTRequest request, RESTResponse response) {
        String source_objName = RESTHelper.getRequiredParam(request, APIConstants.PARAM_SOURCE_OBJNAME);
        String listener_objName = RESTHelper.getRequiredParam(request, APIConstants.PARAM_LISTENER_OBJNAME);
        String clientIDString = RESTHelper.getRequiredParam(request, APIConstants.PARAM_CLIENTID);

        int clientID = -1;
        try {
            clientID = Integer.parseInt(clientIDString);
        } catch (NumberFormatException e) {
            ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_BAD_REQUEST);
        }

        String[] ids = NotificationManager.getNotificationManager()
                        .getRegisteredIDs(request, clientID, source_objName, listener_objName);

        OutputHelper.writeStringArrayStreamingOutput(response, ids, JSONConverter.getConverter());
    }

    private void deleteListenerServerRegistrations(RESTRequest request, RESTResponse response) {
        String source_objName = RESTHelper.getRequiredParam(request, APIConstants.PARAM_SOURCE_OBJNAME);
        String listener_objName = RESTHelper.getRequiredParam(request, APIConstants.PARAM_LISTENER_OBJNAME);
        String clientIDString = RESTHelper.getRequiredParam(request, APIConstants.PARAM_CLIENTID);

        int clientID = -1;
        try {
            clientID = Integer.parseInt(clientIDString);
        } catch (NumberFormatException e) {
            ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_BAD_REQUEST);
        }

        //Get the converter
        JSONConverter converter = JSONConverter.getConverter();

        //We'll build the server registration ourselves
        ServerNotificationRegistration serverNotificationRegistration = new ServerNotificationRegistration();
        serverNotificationRegistration.objectName = RESTHelper.objectNameConverter(source_objName, true, converter);
        serverNotificationRegistration.listener = RESTHelper.objectNameConverter(listener_objName, true, converter);
        serverNotificationRegistration.operation = Operation.RemoveAll;

        NotificationManager.getNotificationManager()
                        .deleteServerRegistrationHTTP(request, clientID, serverNotificationRegistration, null, converter);
        response.setStatus(APIConstants.STATUS_NO_CONTENT);
    }

    private void getRegisteredListeners(RESTRequest request, RESTResponse response) {
        String source_objName = RESTHelper.getRequiredParam(request, APIConstants.PARAM_SOURCE_OBJNAME);
        String clientIDString = RESTHelper.getRequiredParam(request, APIConstants.PARAM_CLIENTID);

        int clientID = -1;
        try {
            clientID = Integer.parseInt(clientIDString);
        } catch (NumberFormatException e) {
            ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_BAD_REQUEST);
        }

        String[] listeners = NotificationManager.getNotificationManager()
                        .getRegisteredListeners(request, clientID, source_objName);

        OutputHelper.writeStringArrayStreamingOutput(response, listeners, JSONConverter.getConverter());
    }

    private void deleteRegisteredListeners(RESTRequest request, RESTResponse response) {
        String source_objName = RESTHelper.getRequiredParam(request, APIConstants.PARAM_SOURCE_OBJNAME);
        String clientIDString = RESTHelper.getRequiredParam(request, APIConstants.PARAM_CLIENTID);

        int clientID = -1;
        try {
            clientID = Integer.parseInt(clientIDString);
        } catch (NumberFormatException e) {
            ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_BAD_REQUEST);
        }

        //Get the converter
        JSONConverter converter = JSONConverter.getConverter();

        NotificationManager.getNotificationManager()
                        .deleteRegisteredListeners(request, clientID, RESTHelper.objectNameConverter(source_objName, true, converter), converter);
        response.setStatus(APIConstants.STATUS_NO_CONTENT);
    }
}
