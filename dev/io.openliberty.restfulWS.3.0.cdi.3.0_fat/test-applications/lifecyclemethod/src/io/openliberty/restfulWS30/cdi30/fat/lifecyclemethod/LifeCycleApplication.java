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
package io.openliberty.restfulWS30.cdi30.fat.lifecyclemethod;

import java.util.HashSet;
import java.util.Set;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Application;

/**
 * {@code HelloWorldApplication} is a {@link jakarta.ws.rs.core.Application} subclass which contains configuration information for the JAX-RS application.
 * Any JAX-RS resources or providers that are to be used must be returned in
 * either the {@link #getClasses()} or {@link #getSingletons()} methods. Note
 * that providers are always singletons according to the JavaDoc.
 */
@ApplicationScoped
public class LifeCycleApplication extends Application {

    LifeCyclePerson person;
    LifeCycleResource2 lifeCycleResource2;

    @Inject
    public void setPerson(LifeCyclePerson person) {
        this.person = person;
        System.out.println("Application Injection successful...");
    }
    
    @Inject
    public void setLifeCycleResource2(LifeCycleResource2 lifeCycleResource2) {
        this.lifeCycleResource2 = lifeCycleResource2;
        System.out.println("Application Injection successful for LifeCycleResource2");
    }

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(LifeCycleResource1.class);
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        System.out.println("@Inject in getSingletons Application: " + person);
        Set<Object> objs = new HashSet<Object>();
        this.lifeCycleResource2.setType("Singleton2");
        objs.add(this.lifeCycleResource2);
        return objs;
    }

    @PostConstruct
    public void method1() {
        System.out.println("postConstruct method is called on " + this.getClass().getName());
    }

    @PreDestroy
    public void method2() {
        System.out.println("preDestory method is called on " + this.getClass().getName());
    }
}
