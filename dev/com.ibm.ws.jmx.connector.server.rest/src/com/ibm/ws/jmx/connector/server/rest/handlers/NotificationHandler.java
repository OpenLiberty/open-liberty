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
import com.ibm.ws.jmx.connector.datatypes.NotificationArea;
import com.ibm.ws.jmx.connector.datatypes.NotificationSettings;
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
                       RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.PATH_NOTIFICATIONS })
public class NotificationHandler implements RESTHandler {
    public static final TraceComponent tc = Tr.register(NotificationHandler.class);

    @Activate
    protected void activate(ComponentContext context, Map<String, Object> properties) {}

    @Deactivate
    protected void deactivate(ComponentContext context, int reason) {}

    @Override
    public void handleRequest(RESTRequest request, RESTResponse response) {
        String method = request.getMethod();
        if (RESTHelper.isPostMethod(method)) {
            getNotificationArea(request, response);
        } else {
            throw new RESTHandlerMethodNotAllowedError("POST");
        }
    }

    @FFDCIgnore({ ConversionException.class, IOException.class })
    private void getNotificationArea(RESTRequest request, RESTResponse response) {
        RESTHelper.ensureConsumesJson(request);

        //Get the converter
        JSONConverter converter = JSONConverter.getConverter();

        NotificationSettings notificationSettings = null;
        InputStream is = RESTHelper.getInputStream(request);
        try {
            notificationSettings = converter.readNotificationSettings(is);
        } catch (ConversionException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        } catch (IOException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        }

        //Trigger the creation of a new notification area (and related artifacts)
        NotificationArea newArea = NotificationManager.getNotificationManager()
                        .createNotificationArea(request, APIConstants.JMX_CONNECTOR_API_ROOT_PATH + "/notifications/", notificationSettings);
        OutputHelper.writeNotificationAreaOutput(response, newArea, JSONConverter.getConverter());
    }
}
