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
package com.ibm.ws.jaxrs.fat.params;

import java.util.List;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Path("/queryparamnotset")
public class QueryParamNotSetResource {

    @Path("int")
    @GET
    public String getDefault(@QueryParam("count") int count) {
        return Integer.valueOf(count).toString();
    }

    @Path("short")
    @GET
    public String getDefault(@QueryParam("smallCount") short smallCount) {
        return "" + smallCount;
    }

    @Path("long")
    @GET
    public String getDefault(@QueryParam("longCount") long longCount) {
        return "" + longCount;
    }

    @Path("float")
    @GET
    public String getDefault(@QueryParam("floatCount") float floatCount) {
        return "" + floatCount;
    }

    @Path("double")
    @GET
    public String getDefault(@QueryParam("d") double count) {
        return "" + count;
    }

    @Path("byte")
    @GET
    @Produces("text/plain")
    public String getDefault(@QueryParam("b") byte count) {
        return "" + count;
    }

    @Path("char")
    @GET
    public String getDefault(@QueryParam("letter") char count) {
        return count + "";
    }

    @Path("set")
    @GET
    public String getDefault(@QueryParam("bag") Set<Integer> stuff) {
        return "" + stuff.size();
    }

    @Path("list")
    @GET
    public String getDefault(@QueryParam("letter") List<String> stuff) {
        return "" + stuff.size();
    }
}
