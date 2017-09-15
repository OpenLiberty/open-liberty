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
package org.apache.felix.http.base.internal.handler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.osgi.service.http.HttpContext;

final class ServletHandlerRequest
                extends HttpServletRequestWrapper {
    private String contextPath;

    public ServletHandlerRequest(HttpServletRequest req, String alias) {
        super(req);
    }

    @Override
    public String getAuthType() {
        String authType = (String) getAttribute(HttpContext.AUTHENTICATION_TYPE);
        if (authType != null) {
            return authType;
        }

        return super.getAuthType();
    }

    @Override
    public String getContextPath() {
        /*
         * FELIX-2030 Calculate the context path for the Http Service
         * registered servlets from the container context and servlet paths
         */
        if (contextPath == null) {
            final String context = super.getContextPath();
            final String servlet = super.getServletPath();
            if (context.length() == 0) {
                contextPath = servlet;
            } else if (servlet.length() == 0) {
                contextPath = context;
            } else {
                contextPath = context + servlet;
            }
        }

        return contextPath;
    }

    @Override
    public String getPathTranslated() {
        final String info = getPathInfo();
        return (null == info) ? null : getRealPath(info);
    }

    @Override
    public String getRemoteUser() {
        String remoteUser = (String) getAttribute(HttpContext.REMOTE_USER);
        if (remoteUser != null) {
            return remoteUser;
        }

        return super.getRemoteUser();
    }

    @Override
    public String getServletPath() {
        return "";
    }
}
