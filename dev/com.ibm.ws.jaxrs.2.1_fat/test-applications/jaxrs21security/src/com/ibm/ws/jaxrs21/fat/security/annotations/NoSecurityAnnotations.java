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
package com.ibm.ws.jaxrs21.fat.security.annotations;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * A sample resource that demonstrates
 * that with No secruity annotatations the resource is accessible
 */
@Path(value = "/NoSecAnnotations")
public class NoSecurityAnnotations {

    @GET
    @Produces(value = "text/plain")
    public String getMessage() {
        return "remotely accessible because of No Security Annotations";
    }
}
