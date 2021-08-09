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
import com.ibm.ws.jmx.connector.datatypes.ServerNotificationRegistration;
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
                       RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.PATH_NOTIFICATIONS_CLIENTID_SERVERREGISTRATION })
public class NotificationServerRegistrationsHandler implements RESTHandler {
    public static final TraceComponent tc = Tr.register(NotificationServerRegistrationsHandler.class);

    @Activate
    protected void activate(ComponentContext context, Map<String, Object> properties) {}

    @Deactivate
    protected void deactivate(ComponentContext context, int reason) {}

    @Override
    public void handleRequest(RESTRequest request, RESTResponse response) {
        String method = request.getMethod();
        if (RESTHelper.isPostMethod(method)) {
            serverNotificationRegistration(request, response);
        } else if (RESTHelper.isDeleteMethod(method)) {
            removeServerNotifications(request, response);
        } else {
            throw new RESTHandlerMethodNotAllowedError("POST,DELETE");
        }
    }

    private void removeServerNotifications(RESTRequest request, RESTResponse response) {
        String clientIDString = RESTHelper.getRequiredParam(request, APIConstants.PARAM_CLIENTID);

        int clientID = -1;
        try {
            clientID = Integer.parseInt(clientIDString);
        } catch (NumberFormatException e) {
            ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_BAD_REQUEST);
        }

        //Remove all server-side registrations
        NotificationManager.getNotificationManager().removeAllServerRegistrations(request, clientID);
        response.setStatus(APIConstants.STATUS_NO_CONTENT);
    }

    @FFDCIgnore({ ConversionException.class, IOException.class, ClassNotFoundException.class })
    private void serverNotificationRegistration(RESTRequest request, RESTResponse response) {
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

        ServerNotificationRegistration serverNotificationRegistration = null;

        InputStream is = RESTHelper.getInputStream(request);
        try {
            serverNotificationRegistration = converter.readServerNotificationRegistration(is);
        } catch (ConversionException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        } catch (IOException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        } catch (ClassNotFoundException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        }

        //The java client will always call with a non-null operation field.  Direct http callers will have the operation set to null.
        if (serverNotificationRegistration.operation == null) {
            String returningURL = NotificationManager.getNotificationManager()
                            .addServerNotificationHTTP(request, clientID, serverNotificationRegistration, converter);
            OutputHelper.writeStringStreamingOutput(response, returningURL, converter);

        } else {
            //Make the call to handle the server notification registration
            NotificationManager.getNotificationManager()
                            .handleServerNotificationRegistration(request, clientID, serverNotificationRegistration, converter);

            //IMPORTANT: the java client expects a HTTP_NO_CONTENT return code, so we must remain returning null and never a URL in this scenario.
            response.setStatus(APIConstants.STATUS_NO_CONTENT);
            return;
        }
    }
}
