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
package com.ibm.ws.jaxrs21.fat.securitycontext;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import com.ibm.ws.jaxrs21.fat.securitycontext.xml.SecurityContextInfo;

@Path("/context/securitycontext/constructor")
public class SecurityContextConstructorResource {

    final private SecurityContext secContext;

    public SecurityContextConstructorResource(@Context SecurityContext secContext) {
        this.secContext = secContext;
    }

    @GET
    @Produces(MediaType.APPLICATION_XML)
    public SecurityContextInfo requestSecurityInfo() {
        return SecurityContextUtils.securityContextToJSON(secContext);
    }

}
