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
package com.ibm.ws.jaxrs20.client.ComplexClientTest.service;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * Example: https://blogs.oracle.com/arungupta/entry/jax_rs_custom_entity_providers
 */
@Path("fruits")
public class MyResource {
    private final String[] response = { "apple", "banana", "mango" };
    private final MyObject object = new MyObject(3);

    @POST
    @Consumes(value = MyObject.MIME_TYPE)
    public String getFruit(MyObject mo) {
        return response[Integer.valueOf(mo.getIndex()) % 3];
    }

    @GET
    @Produces(value = MyObject.MIME_TYPE)
    public MyObject getFruits() {
        return object;
    }
}
