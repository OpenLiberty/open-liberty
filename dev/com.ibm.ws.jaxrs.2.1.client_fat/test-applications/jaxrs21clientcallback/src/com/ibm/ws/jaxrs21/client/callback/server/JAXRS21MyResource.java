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
package com.ibm.ws.jaxrs21.client.callback.server;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

@Path("rest")
public class JAXRS21MyResource {

    @GET
    @Path("get")
    public String getme(@QueryParam("server") String server, @QueryParam("port") String port) {

        final String tserv = server;
        final String tport = port;
        final Client c = ClientBuilder.newClient();

        InvocationCallback<Response> ic = new InvocationCallback<Response>() {

            @Override
            public void completed(Response resp1) {

                final Response resp2 = c.target("http://" + tserv + ":" + tport + "/jaxrs21clientcallback/Test/rest/three").request().get();

                GenericType<Response> genericTypeResponse = new GenericType<Response>() {
                    {
                        resp2.getHeaders().put("RESULT", Collections.singletonList(new Object()));
                        System.out.print("async-->call three");
                    }
                };

                // Switch the async code to reactive client code
                CompletionStage<Response> completionStage = c.target("http://" + tserv + ":" + tport + "/jaxrs21clientcallback/Test/rest/two").request().rx().get(genericTypeResponse);
                CompletableFuture<Response> completableFuture = completionStage.toCompletableFuture();

                if (!(completableFuture.isDone())) {
                    if (completableFuture.isCompletedExceptionally() || completableFuture.isCancelled()) {
                        System.out.print("completableFuture failed with an exception");
                    } else {
                        System.out.print("sleeping....waiting for completableFuture to complete");
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                        }
                        if (!(completableFuture.isDone())) {
                            System.out.print("completableFuture failed because it took to long");
                        }
                    }
                }

                try {
                    Response rc = completableFuture.get();

                    if (rc != null) {
                        if (resp2.getHeaders().get("RESULT") != null) {
                            resp1.getHeaders().put("RESULT", Collections.singletonList(new Object()));
                            System.out.print("rx-->call two");
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failed(Throwable arg0) {

            }

        };

        Future<Response> f = c.target("http://" + server + ":" + port + "/jaxrs21clientcallback/Test/rest/one").request().async().get(ic);
        try {
            Response r = f.get();
            if (r.getHeaders().get("RESULT") != null) {
                System.out.print("sync-->call one");
                return "PASS";
            }

        } catch (InterruptedException e) {

            e.printStackTrace();
        } catch (ExecutionException e) {

            e.printStackTrace();
        }

        return "FAIL";
    }

    @GET
    @Path("one")
    public String oneme() {
        return "call one";
    }

    @GET
    @Path("two")
    public String twome() {
        return "call two";
    }

    @GET
    @Path("three")
    public String threeme() {
        return "call three";
    }

    public Boolean printHeaders(MultivaluedMap<String, Object> headers) {
        Boolean b = true;
        for (String theKey : headers.keySet()) {
            System.out.print("theKey: " + theKey + " value: " + headers.get(theKey));
        }
        return b;
    }
}
