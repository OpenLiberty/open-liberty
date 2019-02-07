/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.context;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Priority;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;

@ApplicationPath("/context")
public class DemoApplication extends javax.ws.rs.core.Application {

    @Context
    private UriInfo ui;

    @Context
    private HttpHeaders hh;

    @Context
    private Request re;

    @Context
    private Application ap;

    @Context
    private ResourceContext rc;

    @Context
    private Providers pr;

    @Context
    private Configuration c;

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(ClassesRestService.class);
        classes.add(ClassesRestService2.class);
        classes.add(ContextRequestFilter1.class);
        classes.add(ContextRequestFilter2.class);
        classes.add(ContextRequestFilter3.class);
        classes.add(ContextRequestFilter4.class);
        classes.add(JordanExceptionMapProvider.class);
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> objs = new HashSet<Object>();
        objs.add(new SingletonsRestService(ui, hh, re, pr, c));
        objs.add(new SingletonsRestService2(rc));
        //ContextRestService is the real @Context in Application test because the @Context only inject here
        //The parameter in Resource constructor is the normal parameter
        objs.add(new ContextRestService(ui, hh, re, pr, c, rc));
        objs.add(new ContextRequestFilter1());
        objs.add(new ContextRequestFilter2());
        objs.add(new ContextRequestFilter4());
        objs.add(new JordanExceptionMapProvider());
        return objs;
    }

    @Priority(1)
    public static class ContextRequestFilter1 implements ContainerRequestFilter {
        private UriInfo uriInfo;
        private Application app;
        private Configuration config;
        private Providers providers;

        @Context
        public void setUriInfo(UriInfo ui) {
            uriInfo = ui;
        }

        @Context
        public void setApplication(Application ap) {
            app = ap;
        }

        @Context
        public void setConfiguration(Configuration co) {
            config = co;
        }

        @Context
        public void setProviders(Providers pr) {
            providers = pr;
        }

        @Override
        public void filter(ContainerRequestContext context) throws IOException {
            if (app == null) {
                throw new RuntimeException();
            }
            if (uriInfo.getRequestUri().toString().contains(ContextUtil.HTTPHEADERSNAME1)) {
                context.getHeaders().add(providers.getClass().getSimpleName(), ContextUtil.METHODNAME1);
            }
        }
    }

    @Priority(2)
    public static class ContextRequestFilter2 implements ContainerRequestFilter {
        @Context
        private UriInfo uriInfo;
        @Context
        private Application app;
        @Context
        private Configuration config;
        @Context
        private Providers providers;

        @Override
        public void filter(ContainerRequestContext context) throws IOException {
            if (app == null) {
                throw new RuntimeException();
            }

            if (uriInfo.getRequestUri().toString().contains(ContextUtil.HTTPHEADERSNAME2)) {
                context.getHeaders().add(providers.getClass().getSimpleName(), ContextUtil.METHODNAME2);
            }
        }
    }

    @Priority(3)
    public static class ContextRequestFilter3 implements ContainerRequestFilter {
        private UriInfo uriInfo = null;
        private Application app = null;
        private Providers providers = null;

        public ContextRequestFilter3(@Context UriInfo ui, @Context Application ap, @Context Providers pr) {
            this.uriInfo = ui;
            this.app = ap;
            this.providers = pr;
        }

        @Override
        public void filter(ContainerRequestContext context) throws IOException {
            if (app == null) {
                throw new RuntimeException();
            }
            if (uriInfo.getRequestUri().toString().contains(ContextUtil.HTTPHEADERSNAME3)) {
                context.getHeaders().add(providers.getClass().getSimpleName(), ContextUtil.METHODNAME3);
            }
        }
    }

    @Priority(4)
    public static class ContextRequestFilter4 implements ContainerRequestFilter {
        @Context
        private UriInfo uriInfo;
        @Context
        private Application app;
        @Context
        private Providers providers;

        public void doFilter(ContainerRequestContext context, @Context UriInfo ui, @Context Application app, @Context Providers providers) {
            if (app == null) {
                throw new RuntimeException();
            }
            if (ui.getRequestUri().toString().contains(ContextUtil.HTTPHEADERSNAME4)) {
                context.getHeaders().add(providers.getClass().getSimpleName(), ContextUtil.METHODNAME4);
            }
        }

        @Override
        public void filter(ContainerRequestContext context) throws IOException {
            doFilter(context, uriInfo, app, providers);
        }
    }

}
