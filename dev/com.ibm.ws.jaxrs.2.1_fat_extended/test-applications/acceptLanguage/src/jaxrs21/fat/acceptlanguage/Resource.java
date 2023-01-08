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
package jaxrs21.fat.acceptlanguage;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("resource")
@Produces(MediaType.TEXT_PLAIN)
public class Resource {

    @Context
    HttpHeaders headers;

    @GET
    @Path("fromHttpHeaders")
    public Response getAcceptableLanguagesFromHttpHeaders() {
        List<Locale> locales = headers.getAcceptableLanguages();
        String localesStr = locales.stream().map(Locale::toLanguageTag).collect(Collectors.joining(","));
        return Response.ok(localesStr).build();
    }

    @GET
    @Path("fromContainerRequestFilter")
    // this method should never be invoked because the filter should abort with the filter output before it gets here
    public Response getAcceptableLanguagesFromRequestFilter() {
        return Response.ok("Filter not invoked.").build();
    }
}
