/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jaxrs21.fat.exception;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/")
public class ExceptionResource {
    @GET
    @Path("echo")
    @Produces({ "application/json" })
    public CompletionStage<List<Object>> hello() {
        System.out.println("Jim... in hello method");
        try {
            CompletableFuture<List<Object>> response = new CompletableFuture<>();
            int a = 2;
            int b = 0;
            int c = a / b;
            System.out.println(c);
            return response;
        } catch (Exception e) {
            System.out.println("Jim... in hello... caught: " + e);
            throw e;
        }
    }
}
