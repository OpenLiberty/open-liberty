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
import java.util.List;
import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.ObjectName;

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
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jmx.connector.converter.JSONConverter;
import com.ibm.ws.jmx.connector.datatypes.ConversionException;
import com.ibm.ws.jmx.connector.server.rest.APIConstants;
import com.ibm.ws.jmx.connector.server.rest.helpers.AttributeRoutingHelper;
import com.ibm.ws.jmx.connector.server.rest.helpers.ErrorHelper;
import com.ibm.ws.jmx.connector.server.rest.helpers.MBeanServerHelper;
import com.ibm.ws.jmx.connector.server.rest.helpers.OutputHelper;
import com.ibm.ws.jmx.connector.server.rest.helpers.RESTHelper;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.rest.handler.RESTHandler;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;
import com.ibm.wsspi.rest.handler.helper.RESTHandlerMethodNotAllowedError;

@Component(service = { RESTHandler.class },
           configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           property = { "service.vendor=IBM",
                       RESTHandler.PROPERTY_REST_HANDLER_CUSTOM_ROUTING + "=true",
                       RESTHandler.PROPERTY_REST_HANDLER_CONTEXT_ROOT + "=" + APIConstants.JMX_CONNECTOR_API_ROOT_PATH,
                       RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.PATH_MBEANS_OBJECTNAME_ATTRIBUTES,
                       RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.PATH_MBEANS_OBJECTNAME_ATTRIBUTES_ATTRIBUTE })
public class AttributeHandler implements RESTHandler {

    public static final TraceComponent tc = Tr.register(AttributeHandler.class);

    private final String KEY_ROUTING_HELPER = "routingHelper";
    private final AtomicServiceReference<AttributeRoutingHelper> routingHelperRef = new AtomicServiceReference<AttributeRoutingHelper>(KEY_ROUTING_HELPER);

    @Activate
    protected void activate(ComponentContext context, Map<String, Object> properties) {
        routingHelperRef.activate(context);
    }

    @Deactivate
    protected void deactivate(ComponentContext context, int reason) {
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
                getAttribute(request, response);
            } else if (RESTHelper.isPutMethod(method)) {
                setAttribute(request, response);
            } else {
                throw new RESTHandlerMethodNotAllowedError("GET,PUT");
            }
        } else {
            if (RESTHelper.isGetMethod(method)) {
                getAttributes(request, response);
            } else if (RESTHelper.isPostMethod(method)) {
                setAttributes(request, response);
            } else {
                throw new RESTHandlerMethodNotAllowedError("GET,POST");
            }
        }
    }

    private void getAttributes(RESTRequest request, RESTResponse response) {
        String objectName = RESTHelper.getRequiredParam(request, APIConstants.PARAM_OBJECT_NAME);
        List<String> queryAttributes = RESTHelper.getQueryParams(request, APIConstants.PARAM_ATTRIBUTE);

        if (RESTHelper.containsSingleRoutingContext(request)) {
            getRoutingHelper().getAttributes(request, response, objectName, queryAttributes, false);
            return;
        }

        //Get the converted ObjectName
        ObjectName objectNameObj = RESTHelper.objectNameConverter(objectName, true, null);

        if (queryAttributes.isEmpty()) {
            //Since no specific attribute was specified, we must query ALL attributes for the given ObjectName
            MBeanInfo info = MBeanServerHelper.getMBeanInfo(objectNameObj);
            MBeanAttributeInfo[] attributeInfo = info.getAttributes();

            if (attributeInfo != null) {
                for (int i = 0; i < attributeInfo.length; i++) {
                    queryAttributes.add(attributeInfo[i].getName());
                }
            }
        }

        String[] queryArray = new String[queryAttributes.size()];
        final AttributeList attributeList = MBeanServerHelper.getAttributes(objectNameObj, queryAttributes.toArray(queryArray));
        OutputHelper.writeAttributeListOutput(response, attributeList, JSONConverter.getConverter());
    }

    @FFDCIgnore({ ConversionException.class, IOException.class, ClassNotFoundException.class })
    private void setAttributes(RESTRequest request, RESTResponse response) {
        if (RESTHelper.containsSingleRoutingContext(request)) {
            getRoutingHelper().setAttributes(request, response, false);
            return;
        }

        RESTHelper.ensureConsumesJson(request);

        String objectName = RESTHelper.getRequiredParam(request, APIConstants.PARAM_OBJECT_NAME);
        InputStream is = RESTHelper.getInputStream(request);

        //Fetch a converter
        JSONConverter converter = JSONConverter.getConverter();

        //Get the converted ObjectName
        ObjectName objectNameObj = RESTHelper.objectNameConverter(objectName, true, converter);

        //Read the input AttributeList
        AttributeList inputAttributeList = null;
        try {
            inputAttributeList = converter.readAttributeList(is);
        } catch (ConversionException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        } catch (IOException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        } catch (ClassNotFoundException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        }

        final AttributeList attributeList = MBeanServerHelper.setAttributes(objectNameObj, inputAttributeList, converter);
        OutputHelper.writeAttributeListOutput(response, attributeList, converter);
    }

    private void getAttribute(RESTRequest request, RESTResponse response) {
        String objectName = RESTHelper.getRequiredParam(request, APIConstants.PARAM_OBJECT_NAME);
        String attributeName = RESTHelper.getRequiredParam(request, APIConstants.PARAM_ATTRIBUTE);

        if (RESTHelper.containsSingleRoutingContext(request)) {
            getRoutingHelper().getAttribute(request, response, objectName, attributeName, false);
            return;
        }

        //Get the converted ObjectName
        ObjectName objectNameObj = RESTHelper.objectNameConverter(objectName, true, null);

        final Object pojo = MBeanServerHelper.getAttribute(objectNameObj, attributeName);
        OutputHelper.writePOJOOutput(response, pojo, JSONConverter.getConverter());
    }

    @FFDCIgnore({ ConversionException.class, IOException.class, ClassNotFoundException.class })
    private void setAttribute(RESTRequest request, RESTResponse response) {
        if (RESTHelper.containsSingleRoutingContext(request)) {
            getRoutingHelper().setAttribute(request, response, false);
            return;
        }

        RESTHelper.ensureConsumesJson(request);

        String objectName = RESTHelper.getRequiredParam(request, APIConstants.PARAM_OBJECT_NAME);
        String attributeName = RESTHelper.getRequiredParam(request, APIConstants.PARAM_ATTRIBUTE);
        InputStream is = RESTHelper.getInputStream(request);

        //Fetch a converter
        JSONConverter converter = JSONConverter.getConverter();

        //Get the converted ObjectName
        ObjectName objectNameObj = RESTHelper.objectNameConverter(objectName, true, converter);

        //Read the input value
        Object newValue = null;
        try {
            newValue = converter.readPOJO(is);
        } catch (ConversionException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        } catch (IOException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        } catch (ClassNotFoundException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_BAD_REQUEST);
        }

        //Make the Attribute
        Attribute attribute = new Attribute(attributeName, newValue);

        //Set the new value on the server
        MBeanServerHelper.setAttribute(objectNameObj, attribute, converter);

        //Recycle converter
        JSONConverter.returnConverter(converter);

        response.setContentType(APIConstants.MEDIA_TYPE_APPLICATION_JSON);
        response.setStatus(APIConstants.STATUS_NO_CONTENT);
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
