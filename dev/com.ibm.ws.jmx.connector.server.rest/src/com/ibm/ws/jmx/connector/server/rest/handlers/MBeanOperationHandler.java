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
import com.ibm.ws.jmx.connector.datatypes.Invocation;
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
                       RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.PATH_MBEANS_OBJECTNAME_OPERATIONS_OPERATION })
public class MBeanOperationHandler implements RESTHandler {

    public static final TraceComponent tc = Tr.register(MBeanOperationHandler.class);

    @Activate
    protected void activate(ComponentContext context, Map<String, Object> properties) {}

    @Deactivate
    protected void deactivate(ComponentContext context, int reason) {}

    @Override
    public void handleRequest(RESTRequest request, RESTResponse response) {
        String method = request.getMethod();
        if (RESTHelper.isPostMethod(method)) {
            invocation(request, response);
        } else {
            throw new RESTHandlerMethodNotAllowedError("POST");
        }
    }

    @FFDCIgnore({ ConversionException.class, IOException.class, ClassNotFoundException.class })
    private void invocation(RESTRequest request, RESTResponse response) {
        RESTHelper.ensureConsumesJson(request);

        String objectName = RESTHelper.getRequiredParam(request, APIConstants.PARAM_OBJECT_NAME);
        String operation = RESTHelper.getRequiredParam(request, APIConstants.PARAM_OPERATION);
        InputStream is = RESTHelper.getInputStream(request);

        //Get the converter
        JSONConverter converter = JSONConverter.getConverter();

        //Read the input
        Invocation invocation = null;
        try {
            invocation = converter.readInvocation(is);
        } catch (ConversionException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        } catch (IOException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        } catch (ClassNotFoundException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        }

        //Get the converted ObjectName
        ObjectName objectNameObj = RESTHelper.objectNameConverter(objectName, true, converter);

        //Make the invocation
        final Object pojo = MBeanServerHelper
                        .invoke(objectNameObj, operation, invocation.params, invocation.signature, converter);

        OutputHelper.writePOJOOutput(response, pojo, converter);
    }
}
