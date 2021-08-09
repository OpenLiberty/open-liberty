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
package com.ibm.ws.jaxrs.managedbeans;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.ManagedBean;
import javax.annotation.PostConstruct;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

@ApplicationPath("")
@ManagedBean("application")
public class ManagedBeanApp extends Application {

    @Context
    private UriInfo info;

    private int value = 99;
    private boolean injectedBeforePostConstruct = false;

    @Override
    public java.util.Set<java.lang.Class<?>> getClasses() {
        Set<Class<?>> resources = new HashSet<Class<?>>();
        resources.add(ManagedBeanResource.class);
        return resources;
    }
    @Override
    public Set<Object> getSingletons() {
        Set<Object> set = new HashSet<Object>();
        set.add(new ApplicationHolderSingleton(this));
        return set;
    }

    public String getValue() {
        return Integer.toString(value) + "," + injectedBeforePostConstruct;
    }

    @PostConstruct
    public void postConstruct() {
        // increment value so that we can verify if the @PostConstruct annotated method is called
        value++;

        // the jaxrs spec says that @Context injection must occur before the @PostConstruct method is called.
        injectedBeforePostConstruct = info != null;
    }
}
