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
package com.ibm.ws.jaxrs.fat.security.annotations;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * A sample resource that demonstrates
 * Class level All security annotations at the same time
 */
@Path(value = "/ClassAllAnnotations")
@PermitAll
@DenyAll
@RolesAllowed({ "Role1" })
public class ClassLevelAllAnnotations {

    @GET
    @Produces(value = "text/plain")
    public String getMessage() {
        return "remotely inaccessible to all through class level DenyAll";
    }
}
