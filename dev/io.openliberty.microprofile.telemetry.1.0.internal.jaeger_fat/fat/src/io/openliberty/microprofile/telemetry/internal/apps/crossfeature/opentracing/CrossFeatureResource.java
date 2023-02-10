/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.apps.crossfeature.opentracing;

import io.opentracing.Tracer;
import jakarta.inject.Inject;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;

@ApplicationPath("/")
@Path("/")
public class CrossFeatureResource extends Application {

    @Inject
    private CrossFeatureClient client;

    @Inject
    private Tracer tracer;

    @GET
    @Path("1")
    public String get1() {
        String traceId = tracer.activeSpan().context().toTraceId();
        return traceId + ": get1: " + client.get2();
    }

    @GET
    @Path("2")
    public String get2() {
        return "get2: " + client.get3();
    }

    @GET
    @Path("3")
    public String get3() {
        return "get3";
    }
}
