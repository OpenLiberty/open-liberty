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
import java.io.OutputStream;
import java.util.Map;

import javax.management.ObjectName;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jmx.connector.converter.JSONConverter;
import com.ibm.ws.jmx.connector.datatypes.JMXServerInfo;
import com.ibm.ws.jmx.connector.server.rest.APIConstants;
import com.ibm.ws.jmx.connector.server.rest.helpers.ErrorHelper;
import com.ibm.ws.jmx.connector.server.rest.helpers.MBeanServerHelper;
import com.ibm.ws.jmx.connector.server.rest.helpers.OutputHelper;
import com.ibm.ws.jmx.connector.server.rest.helpers.POJOHelper;
import com.ibm.ws.jmx.connector.server.rest.helpers.RESTHelper;
import com.ibm.wsspi.kernel.service.utils.FileUtils;
import com.ibm.wsspi.rest.handler.RESTHandler;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;
import com.ibm.wsspi.rest.handler.helper.RESTHandlerMethodNotAllowedError;

@Component(service = { RESTHandler.class },
           configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           property = { "service.vendor=IBM",
                       RESTHandler.PROPERTY_REST_HANDLER_CONTEXT_ROOT + "=" + APIConstants.JMX_CONNECTOR_API_ROOT_PATH,
                       RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.PATH_ROOT,
                       RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.PATH_MBEANCOUNT,
                       RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.PATH_MBEANSERVER,
                       RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.PATH_DEFAULTDOMAIN,
                       RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.PATH_DOMAINS,
                       RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.PATH_API,
                       RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.PATH_GRAPH,
                       RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.PATH_INSTANCEOF,
                       RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.PATH_POJO })
public class JMXHandler implements RESTHandler {
    public static final TraceComponent tc = Tr.register(JMXHandler.class);

    @Activate
    protected void activate(ComponentContext context, Map<String, Object> properties) {}

    @Deactivate
    protected void deactivate(ComponentContext context, int reason) {}

    @Override
    public void handleRequest(RESTRequest request, RESTResponse response) {
        String path = request.getPath();
        String method = request.getMethod();
        if (RESTHelper.isGetMethod(method) && path.endsWith("/mbeanCount")) {
            getMBeanCount(request, response);
        } else if (RESTHelper.isGetMethod(method) && path.endsWith("/defaultDomain")) {
            getDefaultDomain(request, response);
        } else if (RESTHelper.isGetMethod(method) && path.endsWith("/domains")) {
            getDomains(request, response);
        } else if (RESTHelper.isGetMethod(method) && path.endsWith("/mbeanServer")) {
            getMBeanServer(request, response);
        } else if (RESTHelper.isGetMethod(method) && path.endsWith("/graph")) {
            map(request, response);
        } else if (RESTHelper.isGetMethod(method) && path.endsWith("/api")) {
            api(request, response);
        } else if (RESTHelper.isGetMethod(method) && path.endsWith("/instanceOf")) {
            instanceOf(request, response);
        } else if (RESTHelper.isGetMethod(method) && path.endsWith("/pojo")) {
            getPOJO(request, response);
        } else if (RESTHelper.isGetMethod(method) && path.endsWith("/")) {
            getJMX(request, response);
        } else {
            throw new RESTHandlerMethodNotAllowedError("GET");
        }
    }

    private void getJMX(RESTRequest request, RESTResponse response) {
        final JMXServerInfo jmxObj = new JMXServerInfo();
        jmxObj.version = APIConstants.SERVER_CONNECTOR_VERSION;
        jmxObj.mbeansURL = APIConstants.JMX_CONNECTOR_API_ROOT_PATH + "/mbeans";
        jmxObj.createMBeanURL = APIConstants.JMX_CONNECTOR_API_ROOT_PATH + "/mbeans/factory";
        jmxObj.instanceOfURL = APIConstants.JMX_CONNECTOR_API_ROOT_PATH + "/instanceOf";
        jmxObj.mbeanCountURL = APIConstants.JMX_CONNECTOR_API_ROOT_PATH + "/mbeanCount";
        jmxObj.defaultDomainURL = APIConstants.JMX_CONNECTOR_API_ROOT_PATH + "/defaultDomain";
        jmxObj.domainsURL = APIConstants.JMX_CONNECTOR_API_ROOT_PATH + "/domains";
        jmxObj.notificationsURL = APIConstants.JMX_CONNECTOR_API_ROOT_PATH + "/notifications";
        jmxObj.fileTransferURL = APIConstants.JMX_CONNECTOR_API_ROOT_PATH + "/file";
        jmxObj.apiURL = APIConstants.JMX_CONNECTOR_API_ROOT_PATH + "/api";
        jmxObj.graphURL = APIConstants.JMX_CONNECTOR_API_ROOT_PATH + "/graph";

        OutputHelper.writeJMXStreamingOutput(response, jmxObj, JSONConverter.getConverter());
    }

    private void getMBeanCount(RESTRequest request, RESTResponse response) {
        final Integer mbeanCount = MBeanServerHelper.getMBeanCount();

        OutputHelper.getIntegerStreamingOutput(response, mbeanCount, JSONConverter.getConverter());
    }

    private void getDefaultDomain(RESTRequest request, RESTResponse response) {
        final String defaultDomain = MBeanServerHelper.getDefaultDomain();

        OutputHelper.writeStringStreamingOutput(response, defaultDomain, JSONConverter.getConverter());
    }

    private void getDomains(RESTRequest request, RESTResponse response) {
        final String[] domains = MBeanServerHelper.getDomains();

        OutputHelper.writeStringArrayStreamingOutput(response, domains, JSONConverter.getConverter());
    }

    private void getMBeanServer(RESTRequest request, RESTResponse response) {
        String serverName = MBeanServerHelper.getMBeanServerName();
        OutputHelper.writeTextOutput(response, serverName);
    }

    private void map(RESTRequest request, RESTResponse response) {
        writeFileToResponse(response, "/Docs/url_map.jpg");

        response.setContentType(APIConstants.MEDIA_TYPE_IMAGE_JPEG);
    }

    private void api(RESTRequest request, RESTResponse response) {
        writeFileToResponse(response, "/Docs/api.html");

        response.setContentType(APIConstants.MEDIA_TYPE_TEXT_HTML);
    }

    private void instanceOf(RESTRequest request, RESTResponse response) {
        String objectName = RESTHelper.getQueryParam(request, APIConstants.PARAM_OBJECT_NAME);
        String className = RESTHelper.getQueryParam(request, APIConstants.PARAM_CLASSNAME);
        ObjectName objectNameObj = RESTHelper.objectNameConverter(objectName, false, null);

        final boolean isInstanceOf = MBeanServerHelper.instanceOf(objectNameObj, className, null);

        OutputHelper.writeBooleanOutput(response, isInstanceOf, JSONConverter.getConverter());
    }

    private void getPOJO(RESTRequest request, RESTResponse response) {
        POJOHelper pojoObj = new POJOHelper();
        final String pojos = pojoObj.getPOJOObject();

        OutputHelper.writeStringStreamingOutput(response, pojos, JSONConverter.getConverter());
    }

    private void writeFileToResponse(RESTResponse response, String path) {
        InputStream inputStream = this.getClass().getResourceAsStream(path);

        if (inputStream != null) {
            OutputStream outputStream = null;
            try {
                outputStream = response.getOutputStream();
                byte[] buffer = new byte[2048];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, len);
                }
            } catch (IOException ioe) {
                throw ErrorHelper.createRESTHandlerJsonException(ioe, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
            } finally {
                FileUtils.tryToClose(outputStream);
                FileUtils.tryToClose(inputStream);
            }
        }
    }
}
