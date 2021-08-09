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
package com.ibm.ws.jaxrs21.client.JAXRS21ComplexClientTest.service;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * Example: https://blogs.oracle.com/arungupta/entry/jax_rs_custom_entity_providers
 */
@Path("fruits")
public class JAXRS21MyResource {
    private final String[] response = { "apple", "banana", "mango" };

    private final JAXRS21MyObject object = new JAXRS21MyObject(3);

    @POST
    @Consumes(value = JAXRS21MyObject.MIME_TYPE)
    public String getFruit(JAXRS21MyObject mo) {
        return response[Integer.valueOf(mo.getIndex()) % 3];
    }

    @GET
    @Produces(value = JAXRS21MyObject.MIME_TYPE)
    public JAXRS21MyObject getFruits() {
        return object;
    }
}
