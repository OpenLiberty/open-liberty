/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.apps.crossfeature.telemetry;

import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;

import io.opentelemetry.api.trace.Span;

@ApplicationPath("/")
@Path("/")
public class CrossFeatureResource extends Application {

    @Inject
    private CrossFeatureClient client;

    @Inject
    private Span span;

    @GET
    @Path("1")
    public String get1() {
        String traceId = span.getSpanContext().getTraceId();
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
