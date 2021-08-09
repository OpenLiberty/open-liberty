/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jaxrs21.fat.patch;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.core.Response;

public class SubResource {

    public static final Map<Integer, SubResource> resources = new ConcurrentHashMap<Integer, SubResource>();
    private final String data;

    public synchronized static SubResource lookup(int num) {
        SubResource p = resources.get(num);
        if (p == null) {
            p = new SubResource("blank-data");
            resources.put(num, p);
        }
        return p;
    }

    public SubResource(String data) {
        this.data = data;
    }

    @GET
    public Response get() {
        return Response.ok("GET: " + data).build();
    }

    @PATCH
    public Response patch() {
        System.out.println("Inside SubResource.patch()");
        return Response.ok("PATCH: " + data).build();
    }
}
