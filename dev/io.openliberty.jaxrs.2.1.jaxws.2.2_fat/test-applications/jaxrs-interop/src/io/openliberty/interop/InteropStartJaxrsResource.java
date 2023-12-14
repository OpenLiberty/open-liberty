/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.interop;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/ep2")
public class InteropStartJaxrsResource {

    @GET
    @Path("jaxwsEP2")
    public String echo() {
        return "Echo from JAX-RS Endpoint 2";
    }
}
