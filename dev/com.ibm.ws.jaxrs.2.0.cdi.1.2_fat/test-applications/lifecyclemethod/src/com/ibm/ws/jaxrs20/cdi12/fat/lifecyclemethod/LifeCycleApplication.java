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
package com.ibm.ws.jaxrs20.cdi12.fat.lifecyclemethod;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
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
@ApplicationScoped
public class LifeCycleApplication extends Application {

    @Context
    private UriInfo uriInfo;

    LifeCyclePerson person;

    @Inject
    public void setPerson(LifeCyclePerson person) {
        this.person = person;
        System.out.println("Application Injection successful...");
    }

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(LifeCycleResource1.class);
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        System.out.println("@Context in getSingletons Application: " + uriInfo);
        System.out.println("@Inject in getSingletons Application: " + person);
        Set<Object> objs = new HashSet<Object>();
        objs.add(new LifeCycleResource2("Singleton2"));
        return objs;
    }

    @PostConstruct
    public void method1()
    {
        System.out.println("postConstruct method is called on " + this.getClass().getName());
    }

    @PreDestroy
    public void method2()
    {
        System.out.println("preDestory method is called on " + this.getClass().getName());
    }
}
