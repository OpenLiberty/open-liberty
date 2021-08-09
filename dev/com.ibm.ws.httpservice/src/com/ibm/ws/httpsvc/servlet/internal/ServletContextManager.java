/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.ws.httpsvc.servlet.internal;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

public final class ServletContextManager {
    private final Bundle bundle;
    private final RootServletContext rootContext;
    private final Map<HttpContext, ExtServletContext> contextMap;

    public ServletContextManager(Bundle bundle, Dictionary<?, ?> initProps) {
        this.bundle = bundle;
        this.contextMap = new HashMap<HttpContext, ExtServletContext>();

        this.rootContext = new RootServletContext();
        rootContext.init(bundle, initProps);
    }

    /**
     * Servlet objects require a ServletContext object. This object provides a number of functions to access
     * the Http Service Java Servlet environment. It is created by the implementation of the Http Service for
     * each unique HttpContext object with which a Servlet object is registered.
     * <p>
     * Thus, Servlet objects registered with the same HttpContext object must also share the same ServletContext object.
     * 
     * @param httpContext
     * @return
     */
    public ExtServletContext getServletContext(HttpContext httpContext) {
        synchronized (this.contextMap) {
            ExtServletContext context = this.contextMap.get(httpContext);
            if (context == null) {
                context = addServletContext(httpContext);
            }

            return context;
        }
    }

    private ExtServletContext addServletContext(HttpContext httpContext) {
        ServletContextImpl context = new ServletContextImpl();
        context.init(this.bundle, this.rootContext, httpContext);

        this.contextMap.put(httpContext, context);
        return context;
    }
}