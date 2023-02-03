/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jaxrs20.cdi12.fat.beanvalidation;

import java.util.WeakHashMap;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/perrequestleak/")
public class BookStoreWithValidationPerRequestLeak {

    static WeakHashMap<BookStoreWithValidationPerRequestLeak, String> map = new WeakHashMap<>();

    Person person;

    @Inject
    public void setPerson(Person person) {
        this.person = person;
        System.out.println("PerRequest Person Injection successful...");

        // Store references to 'this' in a weak hashmap to verify those references to person get cleaned up
        map.put(this, null);
    }

    @GET
    @Path("book")
    @NotNull
    @Produces(MediaType.TEXT_PLAIN)
    public String book(@NotNull @QueryParam("id") String id) {
        return person.talk() + " " + id;
    }

    @GET
    @Path("size")
    @Produces(MediaType.TEXT_PLAIN)
    public String book() {
        int size = map.size();

        // Explicitly call garbage collection to make sure references to person get cleaned up
        for (int i = 0; i < 10; ++i) {
            if (size == 1) {
                break;
            }

            System.out.println("calling system gc");
            System.gc();

            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
            size = map.size();
        }

        System.out.println("hashmap size=" + size);
        return Integer.toString(size);
    }
}