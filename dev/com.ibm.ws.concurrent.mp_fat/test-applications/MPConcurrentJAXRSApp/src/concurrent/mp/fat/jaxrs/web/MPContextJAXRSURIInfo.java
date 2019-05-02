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
package concurrent.mp.fat.jaxrs.web;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;

// @ApplicationScoped // TODO JAX-RS context for URIInfo (and probably other types) is currently not propagated.
// If the injected URIInfo could be made into a CDI bean that is request scoped, then CDI context propagation
// should in theory allow the JAX-RS context to propagate.
@Path("/testUriInfo")
public class MPContextJAXRSURIInfo {
    /**
     * Maximum number of nanoseconds to wait for a task to complete.
     */
    static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

    // TODO add code to shut down this executor (otherwise, it will stay around until the Liberty application stops)
    static final ManagedExecutor executor = ManagedExecutor.builder()
                    .propagated(ThreadContext.APPLICATION, ThreadContext.CDI)
                    .cleared(ThreadContext.ALL_REMAINING)
                    .build();

    @Context
    UriInfo uriInfo;

    private class CollectURIInfo implements Supplier<JsonObject> {
        String requestUri;

        CollectURIInfo(String requestUri) {
            this.requestUri = requestUri;
        }

        @Override
        public JsonObject get() {
            System.out.println("Supplier.get for request " + requestUri);
            JsonObject json = Json.createObjectBuilder()
                            //.add("class", Integer.toHexString(MPContextJAXRSURIInfo.this.hashCode()))
                            //.add("uriInfo", Integer.toHexString(uriInfo.hashCode()))
                            .add("uriInfoPath", uriInfo.getPath())
                            .add("uriInfoQueryParam", uriInfo.getQueryParameters().getFirst("q"))
                            .build();
            System.out.println("Response: " + json);
            return json;
        }
    }

    public Response failure(Throwable x) {
        StringWriter sw = new StringWriter();
        x.printStackTrace(new PrintWriter(sw));

        JsonObject json = Json.createObjectBuilder()
                        .add("failure", sw.toString())
                        .build();

        return Response.status(Status.PARTIAL_CONTENT).entity(json).encoding(MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("path1")
    @Produces(MediaType.APPLICATION_JSON)
    public Response path1() throws ExecutionException, InterruptedException, TimeoutException {
        String request = uriInfo.getRequestUri().toString();
        System.out.println("--> " + request);

        Thread.sleep(500); // encourages requests to overlap when the test submits multiple

        Response response;
        try {
            CompletableFuture<JsonObject> future = executor.supplyAsync(new CollectURIInfo(request));
            JsonObject json = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            response = Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (ExecutionException | InterruptedException | TimeoutException x) {
            response = failure(x);
        }

        System.out.println("<-- " + request + "\r\n " + response.getEntity());
        return response;
    }

    @GET
    @Path("path2")
    @Produces(MediaType.APPLICATION_JSON)
    public Response path2() throws ExecutionException, InterruptedException, TimeoutException {
        String request = uriInfo.getRequestUri().toString();
        System.out.println("--> " + request);

        Thread.sleep(500); // encourages requests to overlap when the test submits multiple

        Response response;
        try {
            CompletableFuture<JsonObject> future = executor.supplyAsync(new CollectURIInfo(request));
            JsonObject json = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            response = Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (ExecutionException | InterruptedException | TimeoutException x) {
            response = failure(x);
        }

        System.out.println("<-- " + request + "\r\n " + response.getEntity());
        return response;
    }
}