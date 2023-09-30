/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
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
package com.ibm.ws.jaxrs20.client.fat.ibmjson4j.service;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

//import com.ibm.json.java.JSONArray;
//import com.ibm.json.java.JSONObject;

/**
 * Resource class for IBMJSON4JTest
 */
@Path("listusers")
public class UserListResource {

//    JSONObject jsonObj = new JSONObject();
//    JSONArray jsonArray = new JSONArray();
    JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
    JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
    JsonObject jsonObj = objectBuilder.build();
    JsonArray jsonArray = arrayBuilder.build();
    Book book = new Book("Java", "919933");

    public UserListResource() {
//        jsonObj.put("Jordan", "29");
        jsonObj = objectBuilder.add("Jordan", "29").build();

//        JSONObject obj0 = new JSONObject();
//        obj0.put("IBM", "100");
//        obj0.put("Liberty", "3");
//        jsonArray.add(0, obj0);
        JsonObject obj0 = objectBuilder.add("IBM", "100")
                        .add("Liberty", "3").build();
        jsonArray = arrayBuilder.add(obj0).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject getListOfUsers() {
        return jsonObj;
    }

//    @SuppressWarnings("unchecked")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject addListOfUsers(JsonObject newObj) {
//        jsonObj.putAll(newObj);
        //JSON-P arrays and objects are immutable so use a builder to create a copy.
        newObj.forEach(objectBuilder::add);
        jsonObj = objectBuilder.build();
        return jsonObj;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("array")
    public JsonArray getArrayOfUserList() {
        return jsonArray;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("array")
    public JsonArray addArrayOfUserList(JsonArray newArr) {
//        jsonArray.add(newArr);
        //JSON-P arrays and objects are immutable so use a builder to create a copy.
        newArr.forEach(arrayBuilder::add);
        jsonArray = arrayBuilder.build();
        return jsonArray;
    }

    @GET
    @Produces(MediaType.APPLICATION_XML)
    @Path("book")
    public Book getBook() {
        return book;
    }

    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    @Path("book")
    public Book addBook(Book newBook) {
        return newBook;
    }
    // TODO: Need a method that uses XMLToJSONTransformer
}