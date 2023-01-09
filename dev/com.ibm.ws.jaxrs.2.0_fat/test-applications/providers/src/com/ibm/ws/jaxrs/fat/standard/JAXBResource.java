/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package com.ibm.ws.jaxrs.fat.standard;

import java.io.IOException;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import com.ibm.ws.jaxrs.fat.standard.jaxb.Person;

@Path("providers/standard/jaxb")
public class JAXBResource {

    @POST
    @Path("/empty")
    public Response postEmptyJAXB(Person p) throws IOException {
        /*
         * this method should never be executed
         */
        return Response.serverError().build();
    }
}
