/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics;

import java.io.IOException;
import java.io.Writer;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.metrics.exceptions.EmptyRegistryException;
import com.ibm.ws.microprofile.metrics.exceptions.HTTPMethodNotAllowedException;
import com.ibm.ws.microprofile.metrics.exceptions.HTTPNotAcceptableException;
import com.ibm.ws.microprofile.metrics.exceptions.NoSuchMetricException;
import com.ibm.ws.microprofile.metrics.exceptions.NoSuchRegistryException;
import com.ibm.ws.microprofile.metrics.helper.Util;
import com.ibm.ws.microprofile.metrics.impl.SharedMetricRegistries;
import com.ibm.ws.microprofile.metrics.writer.JSONMetadataWriter;
import com.ibm.ws.microprofile.metrics.writer.JSONMetricWriter;
import com.ibm.ws.microprofile.metrics.writer.OutputWriter;
import com.ibm.ws.microprofile.metrics.writer.PrometheusMetricWriter;
import com.ibm.wsspi.rest.handler.RESTHandler;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

@Component(service = { RESTHandler.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true, property = { "service.vendor=IBM",
                                                                                                                             RESTHandler.PROPERTY_REST_HANDLER_CONTEXT_ROOT + "="
                                                                                                                                                   + Constants.PATH_METRICS,
                                                                                                                             RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + Constants.PATH_ROOT,
                                                                                                                             RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + Constants.PATH_SUB,
                                                                                                                             RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + Constants.PATH_SUB_ATTRIBUTE
})
public class MetricsHandler implements RESTHandler {

    private static final TraceComponent tc = Tr.register(MetricsHandler.class);

    BaseMetrics bm;
    SharedMetricRegistries sharedMetricRegistry;

    @Activate
    protected void activate(ComponentContext context, Map<String, Object> properties) {
        bm = BaseMetrics.getInstance(sharedMetricRegistry);
        for (String registry : Constants.REGISTRY_NAMES_LIST) {
            sharedMetricRegistry.getOrCreate(registry);
        }
        Util.SHARED_METRIC_REGISTRIES = sharedMetricRegistry;
    }

    @Reference
    public void getSharedMetricRegistries(SharedMetricRegistries sharedMetricRegistry) {
        this.sharedMetricRegistry = sharedMetricRegistry;
    }

    @Deactivate
    protected void deactivate(ComponentContext context, int reason) {}

    @Override
    @FFDCIgnore({ EmptyRegistryException.class, NoSuchMetricException.class, NoSuchRegistryException.class, HTTPNotAcceptableException.class, HTTPMethodNotAllowedException.class })
    public void handleRequest(RESTRequest request, RESTResponse response) throws IOException {

        Locale locale = null;
        String regName = "";
        String attName = "";
        String acceptHeader = "";
        String method = "";
        try {
            locale = request.getLocale();
            regName = request.getPathVariable(Constants.SUB);
            attName = request.getPathVariable(Constants.ATTRIBUTE);
            acceptHeader = request.getHeader(Constants.ACCEPT_HEADER);
            method = request.getMethod();
            setInitialContentType(request, response);
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Tr.formatMessage(tc, locale, "internal.error.CWMMC0006E", e));
        }

        try {
            OutputWriter outputWriter = getOutputWriter(request, response, locale);
            String attribute = request.getPathVariable(Constants.ATTRIBUTE);
            String sub = request.getPathVariable(Constants.SUB);

            if (outputWriter instanceof JSONMetricWriter) {
                response.setContentType(Constants.JSONCONTENTTYPE);
            } else if (outputWriter instanceof JSONMetadataWriter) {
                response.setContentType(Constants.JSONCONTENTTYPE);
            } else {
                response.setContentType(Constants.TEXTCONTENTTYPE);
            }

            if (attribute != null) {
                attribute = checkSlash(attribute);
                outputWriter.write(sub, attribute);
            } else if (sub != null) {
                sub = checkSlash(sub);
                outputWriter.write(sub);
            } else {
                outputWriter.write();
            }
        } catch (EmptyRegistryException e) {
            Tr.event(tc, "The " + regName + " registry is empty.");
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } catch (NoSuchRegistryException e) {
            Tr.event(tc, "The registry" + regName + " was not found.");
            response.sendError(HttpServletResponse.SC_NOT_FOUND, Tr.formatMessage(tc, locale, "registryNotFound.info.CWMMC0003I", regName));
        } catch (NoSuchMetricException e) {
            Tr.event(tc, "The metric " + attName + " was not found.");
            response.sendError(HttpServletResponse.SC_NOT_FOUND, Tr.formatMessage(tc, locale, "metricNotFound.info.CWMMC0002I", attName));
        } catch (IOException e) {
            Tr.event(tc, "internal.error.CWMMC0006E");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Tr.formatMessage(tc, locale, "internal.error.CWMMC0006E"));
        } catch (HTTPNotAcceptableException e) {
            Tr.event(tc, "Accept Header: " + acceptHeader + ", is invalid.");
            response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, Tr.formatMessage(tc, locale, "notAcceptable.info.CWMMC0000I", acceptHeader));
        } catch (HTTPMethodNotAllowedException e) {
            Tr.event(tc, "HTTP method: " + method + ", is not allowed.");
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, Tr.formatMessage(tc, locale, "requestType.info.CWMMC0001I"));
        }
    }

    private OutputWriter getOutputWriter(RESTRequest request, RESTResponse response, Locale locale) throws IOException, HTTPNotAcceptableException, HTTPMethodNotAllowedException {
        String method = request.getMethod();
        String accept = request.getHeader(Constants.ACCEPT_HEADER);
        Writer writer = response.getWriter();

        if (accept == null) {
            accept = Constants.ACCEPT_HEADER_TEXT;
        }

        if (Constants.METHOD_GET.equals(method)) {
            if (accept.contains(Constants.ACCEPT_HEADER_TEXT)) {
                return new PrometheusMetricWriter(writer, locale);
            } else if (accept.contains(Constants.ACCEPT_HEADER_JSON)) {
                return new JSONMetricWriter(writer);
            } else {
                Tr.event(tc, "The Accept header is invalid.");
                return new PrometheusMetricWriter(writer, locale);
            }
        } else if (Constants.METHOD_OPTIONS.equals(method)) {
            if (accept.contains(Constants.ACCEPT_HEADER_JSON)) {
                return new JSONMetadataWriter(writer, locale);
            } else {
                throw new HTTPNotAcceptableException();
            }
        } else {
            throw new HTTPMethodNotAllowedException();
        }
    }

    private void setInitialContentType(RESTRequest request, RESTResponse response) {

        String accept = request.getHeader(Constants.ACCEPT_HEADER);
        if (accept == null) {
            accept = Constants.ACCEPT_HEADER_TEXT;
        }

        if (accept.contains(Constants.ACCEPT_HEADER_TEXT)) {
            response.setContentType(Constants.TEXTCONTENTTYPE);
        } else if (accept.contains(Constants.ACCEPT_HEADER_JSON)) {
            response.setContentType(Constants.JSONCONTENTTYPE);
        } else {
            response.setContentType(Constants.TEXTCONTENTTYPE);
        }
    }

    private String checkSlash(String s) {
        if (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
