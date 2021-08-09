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
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;

@Path("/matrixparamnotset")
public class MatrixParamNotSetResource {

    @Path("int")
    @GET
    public String getDefault(@MatrixParam("count") int count) {
        return count + "";
    }

    @Path("short")
    @GET
    public String getDefault(@MatrixParam("smallCount") short smallCount) {
        return smallCount + "";
    }

    @Path("long")
    @GET
    public String getDefault(@MatrixParam("longCount") long longCount) {
        return longCount + "";
    }

    @Path("float")
    @GET
    public String getDefault(@MatrixParam("floatCount") float floatCount) {
        return floatCount + "";
    }

    @Path("double")
    @GET
    public String getDefault(@MatrixParam("count") double count) {
        return count + "";
    }

    @Path("byte")
    @GET
    public String getDefault(@MatrixParam("b") byte count) {
        return count + "";
    }

    @Path("char")
    @GET
    public String getDefault(@MatrixParam("letter") char letter) {
        return letter + "";
    }

    @Path("set")
    @GET
    public String getDefault(@MatrixParam("bag") Set<Integer> stuff) {
        return stuff.size() + "";
    }

    @Path("list")
    @GET
    public String getDefault(@MatrixParam("letter") List<String> stuff) {
        return stuff.size() + "";
    }
}
