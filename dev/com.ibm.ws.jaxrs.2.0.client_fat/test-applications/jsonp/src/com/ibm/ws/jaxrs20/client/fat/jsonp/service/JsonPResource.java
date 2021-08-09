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
package com.ibm.ws.jaxrs20.client.fat.jsonp.service;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonStructure;
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
        JsonObject o = Json.createObjectBuilder().add("firstName", "jordan")
                        .add("lastName", "zhang").build();

        return o;
    }

    @GET
    @Produces("application/json")
    @Path("getJsonArray")
    public JsonArray getJsonArray() {
        JsonArray ja = Json.createArrayBuilder().add("alex").add("iris").add("grant").add("zhubin").add("wei").add("jordan").build();

        return ja;
    }

    @POST
    @Consumes("application/json")
    @Path("putJsonObject")
    public int putJsonObject(JsonObject o) {

        String fn = o.getString("firstName");
        String ln = o.getString("lastName");

        if (fn.equals("jordan") && ln.equals("zhang")) {
            return 1;
        }

        return 0;
    }

    @POST
    @Consumes("application/json")
    @Path("putJsonArray")
    public int putJsonArray(JsonArray ja) {
        if (ja.size() == 6 && ja.getString(0).equals("alex") && ja.getString(5).equals("jordan")) {
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
                                .add("name", ja.getString(i))
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

    @GET
    @Produces("application/json")
    @Path("getJsonStructure")
    public JsonStructure getJsonStructure() {
        JsonStructure o = Json.createObjectBuilder().add("firstName", "ellen")
                        .add("lastName", "xiao").build();
        return o;
    }

    @POST
    @Consumes("application/json")
    @Path("putJsonStructure")
    public int putJsonStructure(JsonStructure o) {

        String fn = ((JsonObject) o).getString("firstName");
        String ln = ((JsonObject) o).getString("lastName");

        if (fn.equals("jordan") && ln.equals("zhang")) {
            return 1;
        }

        return 0;
    }
}
