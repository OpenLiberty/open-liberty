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
package io.openliberty.restfulWS30.cdi30.fat.basic;

import java.util.HashSet;
import java.util.Set;

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
public class HelloWorldApplication extends Application {

    @Context
    private UriInfo uriinfoForC;

    Person personForC;
    HelloWorldResourceForS helloWorldResourceForS;
    HelloWorldResource2 helloWorldResource2;
    ContextRequestFilter contextRequestFilter;
    JordanExceptionMapProvider jordanExceptionMapProvider;

    @Override
    public Set<Class<?>> getClasses() {
        System.out.println("@Context in getClasses Application: " + (uriinfoForC != null));
        System.out.println("@Inject in getClasses Application: " + (personForC != null));
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(HelloWorldResourceForC.class);
        classes.add(HelloWorldResourceForT1.class);
        classes.add(HelloWorldResourceForT2.class);
        classes.add(HelloWorldResourceForT3.class);
        classes.add(HelloWorldResource3Child.class);
        classes.add(CdiConstructorInjectionResource.class);
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        System.out.println("@Context in getSingletons Application: " + (uriinfoForC != null));
        System.out.println("@Inject in getSingletons Application: " + (personForC != null));
        
        this.helloWorldResource2.setType("Singleton2");
        this.helloWorldResource2.setUriInfo(uriinfoForC);
        this.helloWorldResource2.setPerson(personForC);
        
        Set<Object> objs = new HashSet<Object>();
        objs.add(this.helloWorldResourceForS);
        objs.add(this.helloWorldResource2);
        objs.add(this.contextRequestFilter);
        objs.add(this.jordanExceptionMapProvider);
        return objs;
    }

    @Inject
    public void setPerson(Person person) {
        this.personForC = person;
        System.out.println("Application Injection successful for Person");
    }
    
    @Inject
    public void setHelloWorldResourceForS(HelloWorldResourceForS helloWorldResourceForS) {
        this.helloWorldResourceForS = helloWorldResourceForS;
        System.out.println("Application Injection successful for HelloWorldResourceForS");
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
    
    @Inject
    public void setJordanExceptionMapProvider(JordanExceptionMapProvider jordanExceptionMapProvider) {
        this.jordanExceptionMapProvider = jordanExceptionMapProvider;
        System.out.println("Application Injection successful for JordanExceptionMapProvider");
    }
}
