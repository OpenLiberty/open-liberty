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
package com.ibm.ws.jaxrs21.fat.uriInfo;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

@ApplicationScoped
public class TestClient {

    final static String URI_CONTEXT_ROOT = "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default") + "/uriInfo/";

    private Client client;
    private WebTarget target;

    @PostConstruct
    private void initClient() {
        client = ClientBuilder.newClient().register(TestClientRequestResponseFilter.class);
        target = client.target(URI_CONTEXT_ROOT);
    }

    @PreDestroy
    private void closeClient() {
        client.close();
    }

    public String invokeRequest() {
        Response response = null;
        CompletableFuture<Response> completableFuture = target.path("resources/test/client").request().rx().get().toCompletableFuture();
        try {
            response = completableFuture.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return "client pre=" + response.getHeaderString("pre")
               + ", client preUri=" + response.getHeaderString("preUri")
               + ", client post=" + response.getHeaderString("post")
               + ", client postUri=" + response.getHeaderString("postUri");
    }
}
