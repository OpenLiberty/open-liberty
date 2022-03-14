/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.prototype;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/ep2")
public class PrototypeResource {

    @GET
    @Path("jaxwsEP2")
    public String echo() {
        return "Echo from JAX-RS Endpoint 2";
    }
}
