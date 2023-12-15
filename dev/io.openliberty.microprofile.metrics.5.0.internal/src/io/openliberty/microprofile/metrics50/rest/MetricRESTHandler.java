/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.metrics50.rest;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.rest.handler.RESTHandler;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

import io.openliberty.microprofile.metrics50.SharedMetricRegistries;
import io.openliberty.microprofile.metrics50.helper.Constants;
import io.openliberty.microprofile.metrics50.helper.Util;
import jakarta.servlet.http.HttpServletResponse;

/**
 *
 */
public class MetricRESTHandler implements RESTHandler {
    private static final TraceComponent tc = Tr.register(MetricRESTHandler.class);
    protected SharedMetricRegistries sharedMetricRegistry;

    protected Object responseFunction;
    protected Method handleRequestMethod;

    /*
     * Reflectively loaded Small Rye Metrics Request Handler
     */
    protected Object srMetricsRequestHandlerObj;

    protected void createSRMetricRequestHandler() throws IllegalStateException {
        Class<?> clazz = Util.SR_METRICS_REQUEST_HANDLER_CLASS;
        Constructor<?> constructor;

        if (clazz == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The SmallRye Metric MetricsRequestHandler class was not resolved.");

                // Use an Error message like the following?!?!
                // Tr.error(tc, "MicroProfile Metrics encountered a class loading error.");
            }
        } else {
            try {
                constructor = clazz.getConstructor();
                srMetricsRequestHandlerObj = constructor.newInstance();
            } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
                    | IllegalArgumentException | InvocationTargetException e) {
                /*
                 * If this fails, this is due to changed API. This is the issue with using
                 * reflection to load.
                 */
            }

            try {
                handleRequestMethod = srMetricsRequestHandlerObj.getClass().getMethod("handleRequest", String.class,
                        String.class, Stream.class, Map.class, Util.SR_REST_RESPONDER_INTERFACE);
            } catch (NoSuchMethodException | SecurityException e) {
                /*
                 * If this fails, this is due to changed API. This is the issue with using
                 * reflection to load.
                 */
            }
        }

    }

    @Override
    public void handleRequest(RESTRequest request, RESTResponse response) throws IOException {
        Locale locale = null;
        String regName = "";
        String attName = "";
        String acceptHeader = null;
        String method = "";
        String requestPath = "";
        String accept = request.getHeader(Constants.ACCEPT_HEADER);
        Stream<String> acceptHeaders = null;
        try {
            locale = request.getLocale();
            regName = request.getPathVariable(Constants.SUB);
            attName = request.getPathVariable(Constants.ATTRIBUTE);
            acceptHeader = request.getHeader(Constants.ACCEPT_HEADER);
            method = request.getMethod();
            requestPath = request.getURI();
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    Tr.formatMessage(tc, locale, "internal.error.CWMMC0006E", e));
        }
        if (acceptHeader != null) {
            ArrayList<String> tmp = new ArrayList<String>();
            tmp.add(acceptHeader);
            acceptHeaders = tmp.stream();
        }

        ClassLoader baoClassLoader = Util.BUNDLE_ADD_ON_CLASSLOADER;

        /*
         * Present Internal Server error if (SmallRye) Metrics failed to initialize.
         * Check by seeing if the MetricsRequestHandler obj is null, if the class loader
         * is null or if the method is null.
         */
        if (srMetricsRequestHandlerObj == null || baoClassLoader == null || handleRequestMethod == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "SmallRye RequestHandler was not created or class loader was not created");
            }
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    Tr.formatMessage(tc, locale, "internal.error.CWMMC0006E"));
            return;
        }

        responseFunction = Proxy.newProxyInstance(baoClassLoader, new Class[] { Util.SR_REST_RESPONDER_INTERFACE },
                new InvocationHandler() {

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if (method.getName().equals("respondWith")) {
                            int status = (int) args[0];
                            String message = (String) args[1];
                            Map<String, String> headers = (Map<String, String>) args[2];
                            headers.forEach((key, value) -> {
                                /*
                                 * For Content-type, avoid adding to header directly and use
                                 * RestRsponse.setContentType();
                                 */
                                if (key.equalsIgnoreCase(Constants.CONTENT_TYPE_HEADER)) {
                                    if (!value.contains(Constants.CHARSET)) {
                                        value += (value.endsWith(";")) ? "" : ";";
                                        value += " " + Constants.CHARSET + "=" + Constants.UTF8;
                                    }
                                    // This will automatically set the header for Content-Type
                                    response.setContentType(value);
                                } else {
                                    response.addResponseHeader(key, value);
                                }
                            });
                            response.setStatus(status);
                            response.getWriter().write(message);
                        }
                        return null;
                    }
                });

        try {
            handleRequestMethod.invoke(srMetricsRequestHandlerObj, requestPath, method,
                    acceptHeaders == null ? null : acceptHeaders, request.getParameterMap(), responseFunction);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            /*
             * If this fails, this is due to changed API. This is the issue with using
             * reflection to load.
             */
        }
    }
}
