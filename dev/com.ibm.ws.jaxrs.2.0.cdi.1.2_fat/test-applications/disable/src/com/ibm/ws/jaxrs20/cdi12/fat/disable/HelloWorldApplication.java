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
package com.ibm.ws.jaxrs20.cdi12.fat.disable;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

/**
 * <code>HelloWorldApplication</code> is a {@link javax.ws.rs.core.Application} subclass which contains configuration information for the JAX-RS application.
 * Any JAX-RS resources or providers that are to be used must be returned in
 * either the {@link #getClasses()} or {@link #getSingletons()} methods. Note
 * that providers are always singletons according to the JavaDoc.
 */
public class HelloWorldApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(HelloWorldResourceForC.class);
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> objs = new HashSet<Object>();
        objs.add(new HelloWorldResourceForS());
        return objs;
    }
}
