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

import javax.annotation.ManagedBean;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;

@Path("managedbean")
@RequestScoped
@ManagedBean("resource")
public class ManagedBeanResource {

    @Context
    Application application;

    private int value = 999;
    private boolean injectedBeforePostConstruct = false;

    @GET
    @Path("resourcevalue")
    public String getResourceValue() {
        return Integer.toString(value) + "," + injectedBeforePostConstruct;
    }

    @GET
    @Path("applicationvalue")
    public String applicationValue() {
        ApplicationHolderSingleton singleton = getAppHolderSingleton();
        return singleton.getValue();
    }

    @PostConstruct
    public void postConstruct() {
        value++;
        injectedBeforePostConstruct = application != null;
    }

    private ApplicationHolderSingleton getAppHolderSingleton() {
        return (ApplicationHolderSingleton) application.getSingletons().iterator().next();
    }
}
