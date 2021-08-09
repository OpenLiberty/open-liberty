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
package com.ibm.ws.jaxrs.fat.security.ssl;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import com.ibm.ws.jaxrs.fat.securitycontext.SecurityContextUtils;
import com.ibm.ws.jaxrs.fat.securitycontext.xml.SecurityContextInfo;

@Path("/ssl")
public class SSLSecurityResource {

    private SecurityContext s = null;

    @GET
    @Path("/get")
    @Produces(MediaType.APPLICATION_XML)
    public SecurityContextInfo requestSecurityInfo1() {
        return SecurityContextUtils.securityContextToJSON(s);
    }

    @Context
    public void setSecurityContext(SecurityContext secContext) {
        this.s = secContext;

    }

}
