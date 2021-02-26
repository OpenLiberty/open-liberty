/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.restfulWS30.fat.managedbeans;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.ManagedBean;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

@ManagedBean("resource")
@Path("/resource")
@Produces(MediaType.TEXT_PLAIN)
public class Resource {

    static AtomicInteger COUNTER = new AtomicInteger(0);

    @Context
    Application app;

    private boolean appInjectedBeforePostConstruct;

    private int id;

    @PostConstruct
    public void init() {
        id = COUNTER.getAndIncrement();
        appInjectedBeforePostConstruct = app != null;
        System.out.println("Resource init");
    }

    @GET
    @Path("/priorapp")
    public boolean isUriInfoInjectedBeforeAppPostConstruct() {
        return App.uriInfoInjectedBeforePostConstruct;
    }

    @GET
    @Path("/priorresource")
    public boolean isAppInjectedBeforeResourcePostConstruct() {
        return appInjectedBeforePostConstruct;
    }

    @GET
    @Path("/id")
    public int getInstanceId() {
        return id;
    }
}
