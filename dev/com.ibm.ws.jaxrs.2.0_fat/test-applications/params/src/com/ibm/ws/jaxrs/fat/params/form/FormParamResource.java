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
package com.ibm.ws.jaxrs.fat.params.form;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

@Path("/form")
public class FormParamResource {

    public FormParamResource() {

    }

    @POST
    @Path("withOnlyEntity")
    public String getRes(MultivaluedMap<String, String> entity) {
        return entity.toString();
    }

    @POST
    @Path("withOneKeyAndEntity")
    public String getRes(@FormParam("firstkey") String firstKey,
                         MultivaluedMap<String, String> entity) {
        return "firstkey=" + firstKey + "&entity=" + entity.toString();
    }

    @POST
    @Path("withStringEntity")
    public String getStrEntity(String entity) {
        return "str:" + entity;
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("httpServletRequestGetParam")
    public String getParameterValues(@Context HttpServletRequest request) {
        String id = request.getParameter("id");
        return "id=" + id;
    }
}
