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

package com.ibm.ws.jaxrs2x.fat.jsonp.service;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("jsonp")
public class JsonPResource {

    @GET
    @Produces("application/json")
    @Path("getJsonObject")
    public JsonObject getJsonObject() {
        JsonObject o = Json.createObjectBuilder().add("firstName", "alex")
                        .add("lastName", "zan").build();

        return o;
    }

    @GET
    @Produces("application/json")
    @Path("getJsonArray")
    public JsonArray getJsonArray() {
        JsonArray ja = Json.createArrayBuilder().add("alex").add("iris").add("grant").add("zhubin").add("wei").build();

        return ja;
    }

    @POST
    @Produces("text/plain")
    @Consumes("application/json")
    @Path("putJsonObject")
    public int putJsonObject(JsonObject o) {

        String fn = o.getString("firstName");
        String ln = o.getString("lastName");

        if (fn.equals("alex") && ln.equals("zan")) {
            return 1;
        }

        return 0;
    }

    @POST
    @Produces("text/plain")
    @Consumes("application/json")
    @Path("putJsonArray")
    public int putJsonArray(JsonArray ja) {
        if (ja.size() == 5 && ja.getString(0).equals("alex") && ja.getString(3).equals("zhubin")) {
            return 1;
        }
        return 0;
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("readMans")
    public JsonArray readMans(JsonArray ja) {

        JsonArrayBuilder listB = Json.createArrayBuilder();
        if (ja.size() > 0) {

            for (int i = 0; i < ja.size(); i++) {

                JsonObject jo = Json.createObjectBuilder()
                                .add("name", ja.getString(0))
                                .add("age", i + 20)
                                .add("gender", ((i % 2) == 0) ? "M" : "F")
                                .add("job", Json.createObjectBuilder().add("title", "softengineer").add("woritems", 10).build())
                                .add("fav", Json.createArrayBuilder().add("sport").add("travel").build())
                                .build();
                listB.add(jo);
            }
        }

        return listB.build();
    }
}
