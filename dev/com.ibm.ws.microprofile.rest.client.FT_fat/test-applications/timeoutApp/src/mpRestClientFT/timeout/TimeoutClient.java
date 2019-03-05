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
package mpRestClientFT.timeout;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
@Path("/timeout")
public interface TimeoutClient {

    @GET
    @Path("/waitXSeconds/{x}")
    @Timeout(value=3, unit=ChronoUnit.SECONDS)
    String sync(@PathParam("x") int x);

    @GET
    @Path("/waitXSeconds/{x}")
    @Timeout(value=3, unit=ChronoUnit.SECONDS)
    CompletionStage<String> rcAsync(@PathParam("x") int x);

    @GET
    @Path("/waitXSeconds/{x}")
    @Asynchronous
    @Timeout(value=3, unit=ChronoUnit.SECONDS)
    Future<String> ftAsync(@PathParam("x") int x);

}
