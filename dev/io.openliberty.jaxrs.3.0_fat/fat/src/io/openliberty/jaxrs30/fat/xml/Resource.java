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
package io.openliberty.jaxrs30.fat.xml;

import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

@Path("/path")
@Produces("application/xml")
public class Resource {

    private final Map<Integer, Entity> database = new HashMap<>();

    public Resource() {
        Entity e = new Entity();
        e.setEntityName("foo");
        e.setEntityNumber(300);
        database.put(e.getEntityNumber(), e);
    }

    @GET
    @Path("/{number}")
    public Entity getEntity(@PathParam("number") int number) {
        return database.get(number);
    }
}
