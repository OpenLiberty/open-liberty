/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.paramconverter;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.ibm.ws.jaxrs.fat.paramconverter.annotations.NoPublicConstructorListObjectParam;
import com.ibm.ws.jaxrs.fat.paramconverter.annotations.NoPublicConstructorObjectParam;
import com.ibm.ws.jaxrs.fat.paramconverter.annotations.StringArrayParam;
import com.ibm.ws.jaxrs.fat.paramconverter.annotations.StringListParam;
import com.ibm.ws.jaxrs.fat.paramconverter.annotations.StringMapParam;
import com.ibm.ws.jaxrs.fat.paramconverter.annotations.StringSetParam;
import com.ibm.ws.jaxrs.fat.paramconverter.annotations.TestListObjectParam;
import com.ibm.ws.jaxrs.fat.paramconverter.annotations.TestObjectParam;
import com.ibm.ws.jaxrs.fat.paramconverter.objects.NoPublicConstructorListObject;
import com.ibm.ws.jaxrs.fat.paramconverter.objects.NoPublicConstructorObject;
import com.ibm.ws.jaxrs.fat.paramconverter.objects.TestListObject;
import com.ibm.ws.jaxrs.fat.paramconverter.objects.TestObject;

@Path("resource")
@Produces(MediaType.APPLICATION_JSON)
public class TestResource {

    @GET
    @Path("stringarray")
    public Response stringArrayParam(@QueryParam("ids") @StringArrayParam final String[] ids) {
        System.out.println("stringArrayParam ids=" + ids);
        return Response.ok(ids).build();
    }

    @GET
    @Path("stringlist")
    public Response stringListParam(@QueryParam("ids") @StringListParam final List<String> ids) {
        System.out.println("stringListParam stringListParam ids=");
        for (String id : ids) {
            System.out.println("\"" + id + "\"");
        }
        return Response.ok(ids).build();
    }

    @GET
    @Path("stringset")
    public Response stringSetParam(@QueryParam("ids") @StringSetParam final Set<String> ids) {
        System.out.println("stringSetParam ids=" + ids);
        if (ids == null) {
            return Response.ok(ids).build();
        }
        SortedSet<String> sortedIds = new TreeSet<String>(ids);
        System.out.println("stringSetParam sorted ids=" + sortedIds);
        return Response.ok(sortedIds).build();
    }

    @GET
    @Path("stringsortedset")
    public Response stringSortedSetParam(@QueryParam("ids") @StringSetParam final SortedSet<String> ids) {
        System.out.println("stringSortedSetParam ids=" + ids);
        return Response.ok(ids).build();
    }

    @GET
    @Path("stringmap")
    public Response stringMapParam(@QueryParam("ids") @StringMapParam final Map<String, String> ids) {
        System.out.println("stringMapParam ids=" + ids);
        return Response.ok(ids).build();
    }

    @GET
    @Path("multiparam")
    public Response multiParams(@QueryParam("list") @StringListParam final List<String> list,
                                @QueryParam("set") @StringSetParam final Set<String> set) {
        System.out.println("multiParams list=" + list);
        System.out.println("multiParams set=" + set);
        SortedSet<String> sortedSet = new TreeSet<String>(set);
        System.out.println("multiParams sorted set=" + sortedSet);
        return Response.ok(list.toString() + "," + sortedSet.toString()).build();
    }

    @GET
    @Path("testobject")
    public Response testObject(@QueryParam("object") @TestObjectParam final TestObject object) {
        System.out.println("testObject ids=" + object.content);
        return Response.ok(object).build();
    }

    @GET
    @Path("testlistobject")
    public Response testListObject(@QueryParam("object") @TestListObjectParam final TestListObject object) {
        System.out.println("testListObject ids=" + object);
        return Response.ok(object).build();
    }

    @GET
    @Path("nopublicconstructorobject")
    public Response noPublicConstructorObject(@QueryParam("object") @NoPublicConstructorObjectParam final NoPublicConstructorObject object) {
        System.out.println("noPublicConstructorObject ids=" + object.content);
        return Response.ok(object.content).build();
    }

    @GET
    @Path("nopublicconstructorlistobject")
    public Response noPublicConstructorListObject(@QueryParam("object") @NoPublicConstructorListObjectParam final NoPublicConstructorListObject object) {
        System.out.println("noPublicConstructorListObject ids=" + object);
        String string = "";
        for (String s : object) {
            string += s;
        }
        return Response.ok(string).build();
    }

}
