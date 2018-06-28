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
package com.ibm.ws.jaxrs20.cdi12.fat.beanvalidation;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/perrequest/")
public class BookStoreWithValidationPerRequest {
    @NotNull
    private String id;

    @QueryParam("id")
    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    Person person;

    @Inject
    public void setPerson(Person person) {
        this.person = person;
        System.out.println("PerRequest Person Injection successful...");
    }

    @GET
    @Path("book")
    @NotNull
    @Produces(MediaType.TEXT_PLAIN)
    public String book() {
        return person.talk();
    }
}