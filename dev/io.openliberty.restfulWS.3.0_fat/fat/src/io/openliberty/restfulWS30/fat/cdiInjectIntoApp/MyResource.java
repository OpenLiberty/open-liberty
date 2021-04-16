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
package io.openliberty.restfulWS30.fat.cdiInjectIntoApp;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;

@Path("/resource")
public class MyResource {

    static Map<String,AtomicInteger> resourceCounterMap = new HashMap<>();

    static {
        resourceCounterMap.put("1", new AtomicInteger(0));
        resourceCounterMap.put("2", new AtomicInteger(0));
    }

    @Context
    Application app;

    @GET
    @Path("checkAppInjection")
    public String checkAppInjectionNotNull() {
        // Do not update any counters for this method.
        if (app == null) {
            return "Failed to inject app into MyResource";
        }
        Map<String, Object> props = app.getProperties();
        if (props == null) {
            return "Application#getProperties is null";
        }
        Object counter = props.get("counter");
        if (counter == null) {
            return "Counter that is supposed to be injected into MyApplication is null";
        }
        return "SUCCESS";
    }

    @GET
    @Path("1")
    public String one() {
        int resourceCount = resourceCounterMap.get("1").incrementAndGet();
        int appCount = ((InvocationCounter) app.getProperties().get("counter")).invoke();
        return "" + resourceCount + " - " + appCount;
    }

    @GET
    @Path("2")
    public String two() {
        int resourceCount = resourceCounterMap.get("2").incrementAndGet();
        int appCount = ((InvocationCounter) app.getProperties().get("counter")).invoke();
        return "" + resourceCount + " - " + appCount;
    }
}
