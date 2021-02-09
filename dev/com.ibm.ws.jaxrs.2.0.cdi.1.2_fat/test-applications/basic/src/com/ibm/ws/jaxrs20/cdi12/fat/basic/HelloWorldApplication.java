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
package com.ibm.ws.jaxrs20.cdi12.fat.basic;

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
    private UriInfo uriinfoForC;

    Person personForC;

    @Inject
    public void setPerson(Person person) {
        this.personForC = person;
        System.out.println("Application Injection successful...");
    }

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
        Set<Object> objs = new HashSet<Object>();
        objs.add(new HelloWorldResourceForS());
        objs.add(new HelloWorldResource2("Singleton2", uriinfoForC, personForC));
        objs.add(new ContextRequestFilter());
        objs.add(new JordanExceptionMapProvider());
        return objs;
    }
}
