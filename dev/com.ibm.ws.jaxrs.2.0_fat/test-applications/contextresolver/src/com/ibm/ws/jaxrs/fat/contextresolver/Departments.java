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
package com.ibm.ws.jaxrs.fat.contextresolver;

import java.util.Iterator;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

@Path(value = "/departments")
public class Departments {

    @GET
    @Produces(value = "text/xml")
    public DepartmentListWrapper getDepartments() {
        Iterator<Department> dptIter = DepartmentDatabase.getDepartments().iterator();
        DepartmentListWrapper wrapper = new DepartmentListWrapper();
        List<Department> dptList = wrapper.getDepartmentList();
        while (dptIter.hasNext()) {
            dptList.add(dptIter.next());
        }
        return wrapper;
    }

    @GET
    @Path(value = "/{departmentId}")
    @Produces(value = { "text/xml" })
    public Response getDepartment(@PathParam(value = "departmentId") String departmentId,
                                  @QueryParam(value = "type") String type,
                                  @Context Request req) {
        Department dept = DepartmentDatabase.getDepartment(departmentId);
        return Response.ok(dept).build();
    }

    @DELETE
    @Path(value = "/{departmentId}")
    public Response deleteDepartment(@PathParam(value = "departmentId") String departmentId) {
        Department dept = DepartmentDatabase.removeDepartment(departmentId);
        if (dept == null) {
            return Response.status(404).build();
        }
        return Response.status(204).build();
    }

    @POST
    @Consumes(value = "text/xml")
    public void addDepartment(Department department) {
        DepartmentDatabase.addDepartment(department);
    }

    @HEAD
    @Produces(value = "text/xml")
    @Path(value = "/{departmentId}")
    public Response exists(@PathParam(value = "departmentId") String departmentId) {
        Department dpt = DepartmentDatabase.getDepartment(departmentId);
        Response resp = null;
        if (dpt != null) {
            ResponseBuilder rb = Response.ok();
            rb.entity(dpt);
            resp = rb.build();
            resp.getMetadata().add("resolved-id", departmentId);
        } else {
            ResponseBuilder rb = Response.noContent();
            rb.entity(null);
            resp = rb.build();
            resp.getMetadata().add("unresolved-id", departmentId);
        }
        return resp;
    }

}
