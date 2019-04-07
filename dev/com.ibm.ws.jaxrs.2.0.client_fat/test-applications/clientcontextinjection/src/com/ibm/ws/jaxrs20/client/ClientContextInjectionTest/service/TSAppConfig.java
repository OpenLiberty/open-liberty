/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.client.ClientContextInjectionTest.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;

@ApplicationPath("/")
public class TSAppConfig extends Application {

    @Context
    UriInfo info;
    @Context
    Request request;
    @Context
    HttpHeaders headers;
    @Context
    SecurityContext security;
    @Context
    Providers providers;
    @Context
    ResourceContext resources;

    @Override
    public java.util.Set<java.lang.Class<?>> getClasses() {
        Set<Class<?>> resources = new HashSet<Class<?>>();
        resources.add(Resource.class);
        resources.add(StringBeanEntityProviderWithInjectables.class);
        return resources;
    }

    @Override
    public Set<Object> getSingletons() {
        Object single = new SingletonWithInjectables(this);
        return Collections.singleton(single);
    }

    public String getInjectedContextValues() {
        return StringBeanEntityProviderWithInjectables.computeMask(//
        /*
         * Spec: 9.2.1 Application Note that this cannot be injected
         * into the Application subclass itself since this would create
         * a circular dependency.
         */
                                                                   this, info, request, headers, security, providers, resources,
                                                                   // Configuration injection N/A on Application
                                                                   ClientBuilder.newClient().getConfiguration());
    }
}
