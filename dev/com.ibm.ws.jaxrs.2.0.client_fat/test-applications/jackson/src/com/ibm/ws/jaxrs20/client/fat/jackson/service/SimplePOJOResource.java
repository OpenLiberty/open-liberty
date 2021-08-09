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
package com.ibm.ws.jaxrs20.client.fat.jackson.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/person2")
public class SimplePOJOResource {

    @GET
    @Produces("application/json")
    @Path("map")
    public Map<String, Object> getMap() {
        Map<String, String> person = new HashMap<String, String>();
        person.put("name", "John Doe");
        person.put("age", "40");

        List<String> arr = new ArrayList<String>();
        arr.add("firstArrValue");
        arr.add("secondArrValue");

        Map<String, Object> json = new HashMap<String, Object>();
        json.put("person", person);
        json.put("arr", arr);
        return json;
    }

    @GET
    @Path("list")
    public List<String> getList() {
        List<String> arr = new ArrayList<String>();
        arr.add("firstArrValue");
        arr.add("secondArrValue");

        return arr;
    }

    @GET
    @Path("person")
    public Person getPOJO() {
        Person p = new Person();
        p.setAge(40);
        p.setName("John Doe");

        Manager m = new Manager();
        m.setManagerName("Jane Smith");
        m.setManagerId(123456789);
        p.setManager(m);
        return p;
    }
}
