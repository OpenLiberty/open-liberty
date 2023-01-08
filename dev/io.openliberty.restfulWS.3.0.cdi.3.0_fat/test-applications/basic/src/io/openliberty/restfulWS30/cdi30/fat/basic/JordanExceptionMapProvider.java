/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package io.openliberty.restfulWS30.cdi30.fat.basic;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class JordanExceptionMapProvider implements ExceptionMapper<JordanException> {

    private UriInfo uriinfo;

    @Context
    public void setUriInfo(UriInfo ui) {
        uriinfo = ui;
    }

    @Inject
    SimpleBean simplebean;

    @Override
    public Response toResponse(JordanException arg0) {
        System.out.println("Provider Context uriinfo: " + uriinfo.getPath());
        System.out.println("Provider Inject simplebean: " + simplebean.getMessage());
        return Response.status(200).build();
    }
}
