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
package com.ibm.ws.jaxrs20.cdi12.fat.complex;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

/**
 * <code>HelloWorldApplication</code> is a {@link javax.ws.rs.core.Application} subclass which contains configuration information for the JAX-RS application.
 * Any JAX-RS resources or providers that are to be used must be returned in
 * either the {@link #getClasses()} or {@link #getSingletons()} methods. Note
 * that providers are always singletons according to the JavaDoc.
 */
public class HelloWorldApplication extends Application {

    @Context
    private UriInfo uriInfo;

//@Inject
    Person person;

    @Inject
    public void setPerson(Person person) {
        this.person = person;
        System.out.println("Application Injection successful...");
    }

    @Override
    public Set<Class<?>> getClasses() {
//  System.out.println("@Context in getClasses Application: " + uriInfo);
//  System.out.println("@Inject in getClasses Application: " + person);
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(HelloWorldResource2.class);
        classes.add(ContextRequestFilter.class);
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
//  System.out.println("@Context in getSingletons Application: " + uriInfo);
//  System.out.println("@Inject in getSingletons Application: " + person);
        Set<Object> objs = new HashSet<Object>();
//        objs.add(new HelloWorldResourceForS());
        objs.add(new JordanResource());
        objs.add(new JordanResourceProvider());
        objs.add(new HelloWorldResource2());
        objs.add(new ContextRequestFilter());
        return objs;
    }

}
