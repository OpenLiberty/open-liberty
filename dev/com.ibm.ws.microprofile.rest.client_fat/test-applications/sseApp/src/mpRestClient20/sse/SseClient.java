/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpRestClient20.sse;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.InboundSseEvent;

import org.reactivestreams.Publisher;

@Produces(MediaType.SERVER_SENT_EVENTS)
@Path("/sse")
public interface SseClient extends AutoCloseable {
    @GET
    @Path("/{endpoint}")
    Publisher<InboundSseEvent> anySse(@PathParam("endpoint") String endpoint);

    @GET
    @Path("/send3strings")
    Publisher<String> send3strings();

    @GET
    @Path("/send3ints")
    Publisher<Integer> send3ints(@QueryParam("startingAt") int startingAt);

    @GET
    @Path("/send7objects")
    Publisher<SomeObject> send7objects();
}