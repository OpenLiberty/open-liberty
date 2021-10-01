/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.client.callback.server;

import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.Response;

@Path("rest")
public class MyResource {

    @GET
    @Path("get")
    public String getme(@QueryParam("server") String server, @QueryParam("port") String port) {

        final String tserv = server;
        final String tport = port;

        InvocationCallback<Response> ic = new InvocationCallback<Response>() {

            @Override
            public void completed(Response resp1) {

                InvocationCallback<Response> ic2 = new InvocationCallback<Response>() {

                    @Override
                    public void completed(Response resp2) {
                        Client c = ClientBuilder.newClient();
                        c.target("http://" + tserv + ":" + tport + "/jaxrs20clientcallback/Test/rest/three").request().get();
                        resp2.getHeaders().put("RESULT", Collections.singletonList(new Object()));
                        System.out.print("async-->call three");
                    }

                    @Override
                    public void failed(Throwable arg0) {

                    }
                };

                Client c = ClientBuilder.newClient();
                Future<Response> fc = c.target("http://" + tserv + ":" + tport + "/jaxrs20clientcallback/Test/rest/two").request().async().get(ic2);

                try {
                    Response rc = fc.get();
                    if (rc.getHeaders().get("RESULT") != null) {
                        resp1.getHeaders().put("RESULT", Collections.singletonList(new Object()));
                        System.out.print("async-->call two");
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

        Client c = ClientBuilder.newClient();
        Future<Response> f = c.target("http://" + server + ":" + port + "/jaxrs20clientcallback/Test/rest/one").request().async().get(ic);
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

    @GET
    @Path("hello")
    @Produces("text/plain")
    public String hello() {
        return "hello";
    }
}
