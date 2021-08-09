/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.resourceInfoAtStartup.test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

@Path("/client")
public class ClientResource {

    private static final int HTTP_PORT = Integer.getInteger("bvt.prop.HTTP_default", 8010);

    @GET
    public Response test(@QueryParam("clients") @DefaultValue("50") int clients) {
        Client c = ClientBuilder.newClient();
        WebTarget target = c.target("http://localhost:" + HTTP_PORT + "/resourceInfoAtStartup/1");
        final CountDownLatch latch = new CountDownLatch(clients);
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger errorCount = new AtomicInteger(0);
        for (int i = 0; i < clients; i++) {
            target.request().async().get(new InvocationCallback<String>() {

                @Override
                public void completed(String arg0) {
                    successCount.incrementAndGet();
                    latch.countDown();
                }

                @Override
                public void failed(Throwable arg0) {
                    errorCount.incrementAndGet();
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return Response.ok("Successful clients: " + successCount.get() + " Errors: " + errorCount.get() + "\n").build();
    }
}

