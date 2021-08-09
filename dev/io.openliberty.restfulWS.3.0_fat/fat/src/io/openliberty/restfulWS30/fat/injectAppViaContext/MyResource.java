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
package io.openliberty.restfulWS30.fat.injectAppViaContext;

import java.util.Set;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

@Path("/myresource")
@Produces(MediaType.TEXT_PLAIN)
@RequestScoped
public class MyResource {

    private static int _counter = 0;
    private int instanceId = _counter++;

    @Context
    private Application app;

    private boolean appInjectedBeforePostConstruct;

    @PostConstruct
    protected void postConstruct() {
        appInjectedBeforePostConstruct = app != null;
    }

    @GET
    @Path("appID")
    public int appID() {
        if (app == null) { //injection of app failed...
            return -1;
        }
        Set<Object> singletons = app.getSingletons();
        if (singletons == null || singletons.size() < 1) {
            return -2;
        }
        MyAppAccessor app = (MyAppAccessor) singletons.iterator().next();
        return app.getAppID();
    }

    @GET
    @Path("resourceID")
    public int resourceID() {
        return this.instanceId;
    }

    @GET
    @Path("providersInjectedInAppBeforePostConstruct")
    public boolean providersInjectedInAppBeforePostConstruct() {
        if (app == null) { //injection of app failed...
            return false;
        }
        Set<Object> singletons = app.getSingletons();
        if (singletons == null || singletons.size() < 1) {
            return false;
        }
        MyAppAccessor app = (MyAppAccessor) singletons.iterator().next();
        return app.isProvidersInjectedBeforePostConstruct();
    }

    @GET
    @Path("appInjectedInResourceBeforePostConstruct")
    public boolean appInjectedInResourceBeforePostConstruct() {
        return appInjectedBeforePostConstruct;
    }
}
