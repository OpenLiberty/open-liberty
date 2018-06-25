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
package com.ibm.ws.jaxrs20.client.fat.ibmjson4j.service;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

/**
 * Resource class for IBMJSON4JTest
 */
@Path("listusers")
public class UserListResource {

    JSONObject jsonObj = new JSONObject();
    JSONArray jsonArray = new JSONArray();
    Book book = new Book("Java", "919933");

    public UserListResource() {
        jsonObj.put("Jordan", "29");

        JSONObject obj0 = new JSONObject();
        obj0.put("IBM", "100");
        obj0.put("Liberty", "3");
        jsonArray.add(0, obj0);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JSONObject getListOfUsers() {
        return jsonObj;
    }

    @SuppressWarnings("unchecked")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public JSONObject addListOfUsers(JSONObject newObj) {
        jsonObj.putAll(newObj);
        return jsonObj;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("array")
    public JSONArray getArrayOfUserList() {
        return jsonArray;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("array")
    public JSONArray addArrayOfUserList(JSONArray newArr) {
        jsonArray.add(newArr);
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