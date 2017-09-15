/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.server;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

@Component
public abstract class BaseHttpTest {

    protected volatile HttpService http = null;
    private final Set<String> registrations = new HashSet<String>();

    protected void activate(ComponentContext context) throws Exception {}

    protected void deactivate(ComponentContext context) {
        for (String alias : registrations) {
            http.unregister(alias);
        }
        http = null;
    }

    protected void registerServlet(String alias, Servlet servlet, Dictionary<String, Object> initParams, HttpContext context) throws ServletException, NamespaceException {
        if (http == null) {
            throw new NullPointerException("Http service is not present");
        }
        http.registerServlet(alias, servlet, initParams, context);
        registrations.add(alias);
    }

    @Reference(name = "http", service = HttpService.class)
    protected void setHttp(HttpService ref) {
        this.http = ref;
    }

    protected void unsetHttp(HttpService ref) {
        if (ref == this.http) {
            this.http = null;
        }
    }

}
