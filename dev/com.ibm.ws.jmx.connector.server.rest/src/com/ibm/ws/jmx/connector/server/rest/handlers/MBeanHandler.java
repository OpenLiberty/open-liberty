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
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jmx.connector.converter.JSONConverter;
import com.ibm.ws.jmx.connector.datatypes.ConversionException;
import com.ibm.ws.jmx.connector.datatypes.CreateMBean;
import com.ibm.ws.jmx.connector.datatypes.MBeanInfoWrapper;
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
                       RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.PATH_MBEANS_FACTORY,
                       RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.PATH_MBEANS_OBJECTNAME })
public class MBeanHandler implements RESTHandler {

    public static final TraceComponent tc = Tr.register(MBeanHandler.class);

    @Activate
    protected void activate(ComponentContext context, Map<String, Object> properties) {}

    @Deactivate
    protected void deactivate(ComponentContext context, int reason) {}

    @Override
    public void handleRequest(RESTRequest request, RESTResponse response) {
        String method = request.getMethod();
        if (request.getPathVariable(APIConstants.PARAM_OBJECT_NAME) != null) {
            if (RESTHelper.isGetMethod(method)) {
                objectName(request, response);
            } else if (RESTHelper.isDeleteMethod(method)) {
                deleteMBean(request, response);
            } else {
                throw new RESTHandlerMethodNotAllowedError("GET,DELETE");
            }
        } else {
            if (RESTHelper.isPostMethod(method)) {
                createMBean(request, response);
            } else {
                throw new RESTHandlerMethodNotAllowedError("POST");
            }
        }
    }

    private void createMBean(RESTRequest request, RESTResponse response) {
        RESTHelper.ensureConsumesJson(request);

        //Get the converter
        JSONConverter converter = JSONConverter.getConverter();

        InputStream is = RESTHelper.getInputStream(request);

        //Fetch the input
        CreateMBean createMBean = null;
        try {
            createMBean = converter.readCreateMBean(is);
        } catch (ConversionException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        } catch (IOException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        } catch (ClassNotFoundException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        }

        //Create the MBean
        final ObjectInstance instance = MBeanServerHelper.createMBean(createMBean, converter);

        //Make our wrapper with a URL
        final ObjectInstanceWrapper instanceWrapper = new ObjectInstanceWrapper();
        instanceWrapper.objectInstance = instance;

        instanceWrapper.mbeanInfoURL = APIConstants.JMX_CONNECTOR_API_ROOT_PATH + "/mbeans/" + RESTHelper.URLEncoder(createMBean.objectName.getCanonicalName(), null);

        OutputHelper.writeObjectInstanceOutput(response, instanceWrapper, converter);
    }

    private void objectName(RESTRequest request, RESTResponse response) {
        String objectName = RESTHelper.getRequiredParam(request, APIConstants.PARAM_OBJECT_NAME);

        //Get the object for ObjectName
        ObjectName objectNameObj = RESTHelper.objectNameConverter(objectName, true, null);

        //Fetch the MBeanInfo
        MBeanInfo mbeanInfo = MBeanServerHelper.getMBeanInfo(objectNameObj);

        //Create wrapper
        final MBeanInfoWrapper mbeanInfoWrapper = new MBeanInfoWrapper();

        //Set the wrapped mbeanInfo
        mbeanInfoWrapper.mbeanInfo = mbeanInfo;

        //Get the encoded string for the ObjectName, which will be used in different places below
        String encodedObjectName = RESTHelper.URLEncoder(objectNameObj.getCanonicalName(), null);

        //Set the Attributes URL
        mbeanInfoWrapper.attributesURL = APIConstants.JMX_CONNECTOR_API_ROOT_PATH + "/mbeans/" + encodedObjectName + "/attributes";

        //Create and set the URL for each attribute
        MBeanAttributeInfo[] attributes = mbeanInfo.getAttributes();
        Map<String, String> attributeURLs = new HashMap<String, String>();

        for (MBeanAttributeInfo attributeInfo : attributes) {
            String key = attributeInfo.getName();
            String value = null;

            value = mbeanInfoWrapper.attributesURL + "/" + RESTHelper.URLEncoder(key, null);

            attributeURLs.put(key, value);
        }
        mbeanInfoWrapper.attributeURLs = attributeURLs;

        //Create and set the URL for each operation
        MBeanOperationInfo[] operations = mbeanInfo.getOperations();
        Map<String, String> operationURLs = new HashMap<String, String>();
        final String operationBaseURL = APIConstants.JMX_CONNECTOR_API_ROOT_PATH + "/mbeans/" + encodedObjectName + "/operations";

        for (MBeanOperationInfo operation : operations) {
            String key = operation.getName();
            String value = null;
            value = operationBaseURL + "/" + RESTHelper.URLEncoder(key, null);
            operationURLs.put(key, value);
        }

        mbeanInfoWrapper.operationURLs = operationURLs;

        OutputHelper.writeMBeanInfoOutput(response, mbeanInfoWrapper, JSONConverter.getConverter());
    }

    private void deleteMBean(RESTRequest request, RESTResponse response) {
        String objectName = RESTHelper.getRequiredParam(request, APIConstants.PARAM_OBJECT_NAME);

        //Get the object for ObjectName
        ObjectName objectNameObj = RESTHelper.objectNameConverter(objectName, true, null);

        //Unregister the MBean
        MBeanServerHelper.unregisterMBean(objectNameObj);

        response.setStatus(APIConstants.STATUS_NO_CONTENT);
    }
}
