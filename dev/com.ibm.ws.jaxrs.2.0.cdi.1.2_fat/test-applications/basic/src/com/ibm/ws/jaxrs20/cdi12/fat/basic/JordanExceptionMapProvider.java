/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package com.ibm.ws.jaxrs20.cdi12.fat.basic;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class JordanExceptionMapProvider implements ExceptionMapper<JordanException> {

    private UriInfo uriinfo;
    //todo add back after provider context injection is ready.
//    @Context
//    public void setUriInfo(UriInfo ui) {
//        uriinfo = ui;
//    }

    @Inject
    SimpleBean simplebean;

    @Override
    public Response toResponse(JordanException arg0) {
        //todo add back after provider context injection is ready.
//        System.out.println("Provider Context uriinfo: " + uriinfo.getPath());
        System.out.println("Provider Inject simplebean: " + simplebean.getMessage());
        return Response.status(200).build();
    }
}
