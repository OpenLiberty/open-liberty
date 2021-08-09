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

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.http.base.internal.context.ExtServletContext;

public final class ServletHandler
                extends AbstractHandler implements Comparable<ServletHandler> {
    private final String alias;
    private final Servlet servlet;

    public ServletHandler(ExtServletContext context, Servlet servlet, String alias) {
        super(context);
        this.alias = alias;
        this.servlet = servlet;
    }

    public String getAlias() {
        return this.alias;
    }

    public Servlet getServlet() {
        return this.servlet;
    }

    @Override
    public void init()
                    throws ServletException {
        String name = "servlet_" + getId();
        ServletConfig config = new ServletConfigImpl(name, getContext(), getInitParams());
        this.servlet.init(config);
    }

    @Override
    public void destroy() {
        this.servlet.destroy();
    }

    public boolean matches(String uri) {
        if (uri == null) {
            return this.alias.equals("/");
        } else if (this.alias.equals("/")) {
            return uri.startsWith(this.alias);
        } else {
            return uri.equals(this.alias) || uri.startsWith(this.alias + "/");
        }
    }

    public boolean handle(HttpServletRequest req, HttpServletResponse res)
                    throws ServletException, IOException {
        // The felix impl uses req.getPathInfo() here: but that seems wrong based on what
        // getPathInfo is supposed to contain...
        final boolean matches = matches(req.getRequestURI());
        if (matches) {
            doHandle(req, res);
        }

        return matches;
    }

    private void doHandle(HttpServletRequest req, HttpServletResponse res)
                    throws ServletException, IOException {
        // set a sensible status code in case handleSecurity returns false
        // but fails to send a response
        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
        if (getContext().handleSecurity(req, res)) {
            // reset status to OK for further processing
            res.setStatus(HttpServletResponse.SC_OK);

            this.servlet.service(new ServletHandlerRequest(req, this.alias), res);
        }
    }

    @Override
    public int compareTo(ServletHandler other) {
        return other.alias.compareTo(this.alias);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof ServletHandler) {
            return ((ServletHandler) other).alias.equals(this.alias);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.alias.hashCode();
    }
}
