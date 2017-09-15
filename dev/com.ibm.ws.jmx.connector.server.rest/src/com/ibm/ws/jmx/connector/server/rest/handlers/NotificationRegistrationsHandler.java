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
import java.io.InputStream;
import java.util.Map;

import javax.management.NotificationFilter;
import javax.management.ObjectName;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jmx.connector.converter.JSONConverter;
import com.ibm.ws.jmx.connector.datatypes.ConversionException;
import com.ibm.ws.jmx.connector.datatypes.NotificationRegistration;
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
                       RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.PATH_NOTIFICATIONS_CLIENTID_REGISTRATIONS,
                       RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.PATH_NOTIFICATIONS_CLIENTID_REGISTRATIONS_OBJECTNAME })
public class NotificationRegistrationsHandler implements RESTHandler {
    public static final TraceComponent tc = Tr.register(NotificationRegistrationsHandler.class);

    @Activate
    protected void activate(ComponentContext context, Map<String, Object> properties) {}

    @Deactivate
    protected void deactivate(ComponentContext context, int reason) {}

    @Override
    public void handleRequest(RESTRequest request, RESTResponse response) {
        String method = request.getMethod();
        if (request.getPathVariable("objectName") != null) {
            if (RESTHelper.isGetMethod(method)) {
                fetchFilters(request, response);
            } else if (RESTHelper.isPutMethod(method)) {
                clientNotificationUpdate(request, response);
            } else if (RESTHelper.isDeleteMethod(method)) {
                clientNotificationDelete(request, response);
            } else {
                throw new RESTHandlerMethodNotAllowedError("GET,PUT,DELETE");
            }
        } else {
            if (RESTHelper.isGetMethod(method)) {
                fetchRegistrations(request, response);
            } else if (RESTHelper.isPostMethod(method)) {
                clientNotificationCreation(request, response);
            } else if (RESTHelper.isDeleteMethod(method)) {
                removeClientNotifications(request, response);
            } else {
                throw new RESTHandlerMethodNotAllowedError("GET,POST,DELETE");
            }
        }

    }

    @FFDCIgnore({ ConversionException.class, IOException.class, ClassNotFoundException.class })
    private void clientNotificationCreation(RESTRequest request, RESTResponse response) {
        RESTHelper.ensureConsumesJson(request);

        String clientIDString = RESTHelper.getRequiredParam(request, APIConstants.PARAM_CLIENTID);

        int clientID = -1;
        try {
            clientID = Integer.parseInt(clientIDString);
        } catch (NumberFormatException e) {
            ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_BAD_REQUEST);
        }
        //Get the converter
        JSONConverter converter = JSONConverter.getConverter();

        NotificationRegistration notificationRegistration = null;
        InputStream is = RESTHelper.getInputStream(request);
        try {
            notificationRegistration = converter.readNotificationRegistration(is);
        } catch (ConversionException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        } catch (IOException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        } catch (ClassNotFoundException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        }

        //Make the call to add the notification and get the URL
        final String url = NotificationManager.getNotificationManager()
                        .addClientNotification(request, clientID, notificationRegistration, converter);

        OutputHelper.writeStringStreamingOutput(response, url, converter);
    }

    @FFDCIgnore({ ConversionException.class, IOException.class, ClassNotFoundException.class })
    private void clientNotificationUpdate(RESTRequest request, RESTResponse response) {
        RESTHelper.ensureConsumesJson(request);

        String objectName = RESTHelper.getRequiredParam(request, APIConstants.PARAM_OBJECT_NAME);
        String clientIDString = RESTHelper.getRequiredParam(request, APIConstants.PARAM_CLIENTID);

        int clientID = -1;
        try {
            clientID = Integer.parseInt(clientIDString);
        } catch (NumberFormatException e) {
            ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_BAD_REQUEST);
        }

        //Get the converter
        JSONConverter converter = JSONConverter.getConverter();

        //Need to decode the objectName since it's a PathParam
        objectName = RESTHelper.URLDecoder(objectName, null);

        //Get the updated filters
        NotificationFilter[] filters = null;
        InputStream is = RESTHelper.getInputStream(request);
        try {
            filters = converter.readNotificationFilters(is);
        } catch (ConversionException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        } catch (IOException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        } catch (ClassNotFoundException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        }

        //Make the update
        NotificationManager.getNotificationManager().updateClientNotification(request, clientID, objectName, filters, converter);

        //Return the converter
        JSONConverter.returnConverter(converter);

        response.setStatus(APIConstants.STATUS_NO_CONTENT);
    }

    private void clientNotificationDelete(RESTRequest request, RESTResponse response) {
        String objectName = RESTHelper.getRequiredParam(request, APIConstants.PARAM_OBJECT_NAME);
        String clientIDString = RESTHelper.getRequiredParam(request, APIConstants.PARAM_CLIENTID);

        int clientID = -1;
        try {
            clientID = Integer.parseInt(clientIDString);
        } catch (NumberFormatException e) {
            ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_BAD_REQUEST);
        }

        //Convert the ObjectName here so that we can return a bad request if something is wrong
        ObjectName objectNameObj = RESTHelper.objectNameConverter(objectName, true, null);

        //Remove the notification
        NotificationManager.getNotificationManager().removeClientNotification(request, objectNameObj, clientID);
        response.setStatus(APIConstants.STATUS_NO_CONTENT);
    }

    private void fetchRegistrations(RESTRequest request, RESTResponse response) {
        String clientIDString = RESTHelper.getRequiredParam(request, APIConstants.PARAM_CLIENTID);

        int clientID = -1;
        try {
            clientID = Integer.parseInt(clientIDString);
        } catch (NumberFormatException e) {
            ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_BAD_REQUEST);
        }

        JSONConverter converter = JSONConverter.getConverter();

        //Fetch the list of mbeans that this inbox is listening to
        String[] notificationRegistrations = NotificationManager.getNotificationManager()
                        .getRegisteredClientNotifications(request, clientID, converter);

        OutputHelper.writeStringArrayStreamingOutput(response, notificationRegistrations, converter);
    }

    private void fetchFilters(RESTRequest request, RESTResponse response) {
        String objectName = RESTHelper.getRequiredParam(request, APIConstants.PARAM_OBJECT_NAME);
        String clientIDString = RESTHelper.getRequiredParam(request, APIConstants.PARAM_CLIENTID);

        int clientID = -1;
        try {
            clientID = Integer.parseInt(clientIDString);
        } catch (NumberFormatException e) {
            ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_BAD_REQUEST);
        }

        JSONConverter converter = JSONConverter.getConverter();

        //Fetch the current notification filters for this mbean (pertaining to this client)
        NotificationFilter[] filters = NotificationManager.getNotificationManager()
                        .getRegisteredFilters(request, clientID, RESTHelper.URLDecoder(objectName, converter), converter);

        OutputHelper.writeNotificationFilterArrayStreamingOutput(response, filters, converter);
    }

    private void removeClientNotifications(RESTRequest request, RESTResponse response) {
        String clientIDString = RESTHelper.getRequiredParam(request, APIConstants.PARAM_CLIENTID);

        int clientID = -1;
        try {
            clientID = Integer.parseInt(clientIDString);
        } catch (NumberFormatException e) {
            ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_BAD_REQUEST);
        }

        //Remove all client-side registrations
        NotificationManager.getNotificationManager().removeAllClientRegistrations(request, clientID);
        response.setStatus(APIConstants.STATUS_NO_CONTENT);
    }
}
