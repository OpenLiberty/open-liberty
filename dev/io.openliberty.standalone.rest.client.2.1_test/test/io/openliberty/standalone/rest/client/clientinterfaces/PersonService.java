/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.standalone.rest.client.clientinterfaces;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import io.openliberty.standalone.rest.client.entities.Person;

@Path("/person")
public interface PersonService {

    @GET
    @Path("{id}")
    Person get(@PathParam("id") String id);

    @POST
    String post(Person newPerson);

    @Path("/{personId}/pets")
    PetService petsByOwner(@PathParam("personId") String personId);
}