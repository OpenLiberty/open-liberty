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
package com.ibm.ws.jaxrs.fat.securitycontext;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Path("/context/securitycontext/notbeanmethod")
public class SecurityContextNotBeanMethodResource {

    private SecurityContext s = null;

    @GET
    public Response requestSecurityInfo() {
        if (s == null) {
            return Response.ok("false").build();
        }
        return Response.ok(SecurityContextUtils.securityContextToJSON(s)).type(MediaType.APPLICATION_XML).build();
    }

    @Context
    public void injectSecurityContext(SecurityContext secContext) {
        /*
         * this method does not start with "set" as its name so it is not
         * expected to be injected.
         */
        this.s = secContext;
    }

    public void setSecurityContext(SecurityContext secContext) {
        /*
         * this method does not have a @Context annotation so it is not expected
         * to be injected.
         */
        this.s = secContext;
    }

    @Context
    public void setSecurityContext(SecurityContext secContext, SecurityContext secContext2) {
        /*
         * this method is not a Java bean method (it has 2 parameters) so it
         * will not be used for injection
         */
        this.s = secContext;
    }
}
