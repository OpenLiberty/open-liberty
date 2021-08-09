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
import com.ibm.ws.jmx.connector.datatypes.MBeanQuery;
import com.ibm.ws.jmx.connector.datatypes.ObjectInstanceWrapper;
import com.ibm.ws.jmx.connector.server.rest.APIConstants;
import com.ibm.ws.jmx.connector.server.rest.helpers.ErrorHelper;
import com.ibm.ws.jmx.connector.server.rest.helpers.MBeanServerHelper;
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
                       RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.PATH_MBEANS })
public class MBeanQueryHandler implements RESTHandler {

    public static final TraceComponent tc = Tr.register(MBeanQueryHandler.class);

    @Activate
    protected void activate(ComponentContext context, Map<String, Object> properties) {}

    @Deactivate
    protected void deactivate(ComponentContext context, int reason) {}

    @Override
    public void handleRequest(RESTRequest request, RESTResponse response) {
        String method = request.getMethod();
        if (RESTHelper.isGetMethod(method)) {
            simpleQuery(request, response);
        } else if (RESTHelper.isPostMethod(method)) {
            complexQuery(request, response);
        } else {
            throw new RESTHandlerMethodNotAllowedError("GET,POST");
        }
    }

    private void simpleQuery(RESTRequest request, RESTResponse response) {
        String objectName = RESTHelper.getQueryParam(request, APIConstants.PARAM_OBJECT_NAME);
        String className = RESTHelper.getQueryParam(request, APIConstants.PARAM_CLASSNAME);
        ObjectName objectNameObj = objectName == null ? null : RESTHelper.objectNameConverter(objectName, false, null);

        final ObjectInstanceWrapper[] returnInstances = MBeanServerHelper.queryObjectName(objectNameObj, null, className, null);

        OutputHelper.writeObjectInstanceArrayOutput(response, returnInstances, JSONConverter.getConverter());
    }

    @FFDCIgnore({ ConversionException.class, IOException.class, ClassNotFoundException.class })
    private void complexQuery(RESTRequest request, RESTResponse response) {
        RESTHelper.ensureConsumesJson(request);

        String objectName = RESTHelper.getQueryParam(request, APIConstants.PARAM_OBJECT_NAME);
        String className = RESTHelper.getQueryParam(request, APIConstants.PARAM_CLASSNAME);
        InputStream is = RESTHelper.getInputStream(request);

        //Get the converter
        JSONConverter converter = JSONConverter.getConverter();

        //Read the input
        MBeanQuery inputQuery = null;
        try {
            inputQuery = converter.readMBeanQuery(is);
        } catch (ConversionException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        } catch (IOException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        } catch (ClassNotFoundException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        }

        if (inputQuery.objectName == null && objectName != null) {
            //Our objectName and className are coming from the query string
            ObjectName objectNameObj = RESTHelper.objectNameConverter(objectName, false, null);

            inputQuery.objectName = objectNameObj;
            inputQuery.className = className;
        }

        //Make the query
        final ObjectInstanceWrapper[] returnInstances = MBeanServerHelper
                        .queryObjectName(inputQuery.objectName, inputQuery.queryExp, inputQuery.className, converter);

        OutputHelper.writeObjectInstanceArrayOutput(response, returnInstances, JSONConverter.getConverter());
    }
}
