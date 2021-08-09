/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jaxrs21.fat.form;


import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@ApplicationPath("/")
@Path("/form")
@ApplicationScoped
public class FormResource extends Application {
    private static final Logger LOG = Logger.getLogger(FormResource.class.getName());

    @POST
    public Response processForm( @FormParam("value") String value, Form form ) {
        String fromForm = form.asMap().getFirst( "value" );
        LOG.info("FromFormParam: " + value);
        LOG.info("FromForm: " + fromForm);
        return Response.ok()
                        .header("FromFormParam", value)
                        .header("FromForm", fromForm)
                        .build();
      }

    @POST
    @Path("/crlf")
    @Consumes(MediaType.TEXT_PLAIN + "; charset=utf-8")
    @Produces(MediaType.TEXT_PLAIN + "; charset=utf-8")
    public Response testCRLFsArePreserved(String body) {
        LOG.info("body: " + body);
        if (body.indexOf('\n') < 0) {
            return Response.status(575).build();
        }
        return Response.ok().build();
    }
}

