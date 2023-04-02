/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.async;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/**
 * Client interface for {@link JaxRsServerAsyncTestEndpoint}
 */
@Path("JaxRsServerAsyncTestEndpoint")
public interface JaxRsServerAsyncTestEndpointClient {

    @GET
    @Path("completionstage")
    public String getCompletionStage();

    @GET
    @Path("suspend")
    public String getSuspend();

}
