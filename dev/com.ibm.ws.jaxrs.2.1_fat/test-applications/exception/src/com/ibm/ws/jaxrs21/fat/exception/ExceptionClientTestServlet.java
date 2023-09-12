/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs21.fat.exception;


import java.util.Objects;
import java.util.concurrent.CompletionStage;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JaxrsExceptionClientTestServlet")
public class ExceptionClientTestServlet extends FATServlet {

    private static final String URI_CONTEXT_ROOT = "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default") + "/exception/";

    private Client client;

    @Override
    public void before() throws ServletException {
        client = ClientBuilder.newClient();
    }

    @Override
    public void after() {
        client.close();
    }

    @Test
    public void testHelloWorld() {
        Client client = ClientBuilder.newClient();
        System.out.println("Jim... sending request.");
        try {
            CompletionStage<String> csResponse = client.target(URI_CONTEXT_ROOT).path("echo").request().rx().get(String.class);
            System.out.println("Jim... back from request");

            csResponse.toCompletableFuture().get();
            Objects.requireNonNull(System.out);
            csResponse.thenAccept(System.out::println);
            csResponse.exceptionally(e -> {
                System.out.println("Jim... exceptionally: " + e.getMessage());

                e.printStackTrace();

                System.out.println("Jim... exceptionally cause: " + e.getCause());
                return null;
            });
        } catch (Throwable e) {
            System.out.println("Jim... caught " + e);
        }
        System.out.println("Jim... Closing client.");
        client.close();
    }
}