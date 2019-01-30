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

import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.PathSegment;

@Path("/pathsegment")
public class PathSegmentResource {

    @Path("{loc}")
    @GET
    public String helloPath(@PathParam("loc") PathSegment pathSegment) {
        return pathSegment.getPath();
    }

    @Path("matrix/{loc}")
    @GET
    public String helloPath(@PathParam("loc") String path,
                            @PathParam("loc") PathSegment pathSegment,
                            @MatrixParam("mp") String matrix) {
        return path + "-"
               + pathSegment.getPath()
               + "-"
               + pathSegment.getMatrixParameters().get(matrix)
               + "-"
               + matrix;
    }

    @Path("/{parm1}/{parm2}/{parm3}")
    @GET
    public String multiPath(@PathParam("parm1") double d, @PathParam("parm2") PathSegment ps, @PathParam("parm3") long l) {
        return "" + d + "-" + ps.getPath() + "-" + l;
    }

    @Path("/{parm1}/{parm2}/hello")
    @GET
    public String multiPathLastOneFixed(@PathParam("parm1") double d, @PathParam("parm2") PathSegment ps) {
        return "" + d + "-" + ps.getPath() + "-hello";
    }

}
