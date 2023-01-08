/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package io.openliberty.restfulWS30.cdi30.fat.complex;

import java.util.HashSet;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;

/**
 * {@code HelloWorldApplication} is a {@link jakarta.ws.rs.core.Application} subclass which contains configuration information for the JAX-RS application.
 * Any JAX-RS resources or providers that are to be used must be returned in
 * either the {@link #getClasses()} or {@link #getSingletons()} methods. Note
 * that providers are always singletons according to the JavaDoc.
 */
@ApplicationScoped
public class HelloWorldApplication extends Application {

    @Context
    private UriInfo uriInfo;

    Person person;
    JordanResource jordanResource;
    JordanResourceProvider jordanResourceProvider;
    HelloWorldResource2 helloWorldResource2;
    ContextRequestFilter contextRequestFilter;

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(HelloWorldResource2.class);
        classes.add(ContextRequestFilter.class);
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> objs = new HashSet<Object>();
        objs.add(jordanResource);
        objs.add(jordanResourceProvider);
        objs.add(helloWorldResource2);
        objs.add(contextRequestFilter);
        return objs;
    }


    @Inject
    public void setPerson(Person person) {
        this.person = person;
        System.out.println("Application Injection successful...");
    }
    
    @Inject
    public void setJordanResource(JordanResource jordanResource) {
        this.jordanResource = jordanResource;
        System.out.println("Application Injection successful for JordanResource");
    }
    
    @Inject
    public void setJordanResourceProvider(JordanResourceProvider jordanResourceProvider) {
        this.jordanResourceProvider = jordanResourceProvider;
        System.out.println("Application Injection successful for JordanResourceProvider");
    }
    
    @Inject
    public void setHelloWorldResource2(HelloWorldResource2 helloWorldResource2) {
        this.helloWorldResource2 = helloWorldResource2;
        System.out.println("Application Injection successful for HelloWorldResource2");
    }
    
    @Inject
    public void setContextRequestFilter(ContextRequestFilter contextRequestFilter) {
        this.contextRequestFilter = contextRequestFilter;
        System.out.println("Application Injection successful for ContextRequestFilter");
    }
}
