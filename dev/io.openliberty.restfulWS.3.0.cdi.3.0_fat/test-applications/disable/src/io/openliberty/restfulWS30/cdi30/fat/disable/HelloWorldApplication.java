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
package io.openliberty.restfulWS30.cdi30.fat.disable;

import java.util.HashSet;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Application;

/**
 * {@code HelloWorldApplication} is a {@link jakarta.ws.rs.core.Application} subclass which contains configuration information for the JAX-RS application.
 * Any JAX-RS resources or providers that are to be used must be returned in
 * either the {@link #getClasses()} or {@link #getSingletons()} methods. Note
 * that providers are always singletons according to the JavaDoc.
 */
@ApplicationScoped // In EE9 we have decided that you MUST explicitly declare an Application subclass as a CDI bean for Injection.
public class HelloWorldApplication extends Application {

    HelloWorldResourceForS helloWorldResourceForS;

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(HelloWorldResourceForC.class);
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> objs = new HashSet<Object>();
        System.out.println("helloWorldResourceForS=" + this.helloWorldResourceForS);
        objs.add(this.helloWorldResourceForS);
        return objs;
    }
    
    @Inject
    public void setHelloWorldResourceForS(HelloWorldResourceForS helloWorldResourceForS) {
        System.out.println("Injecting helloWorldResourceForS...");
        this.helloWorldResourceForS = helloWorldResourceForS;
    }
}
