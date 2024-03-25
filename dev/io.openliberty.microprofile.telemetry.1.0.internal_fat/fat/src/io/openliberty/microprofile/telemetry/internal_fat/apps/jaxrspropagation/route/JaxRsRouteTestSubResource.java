/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.route;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

public class JaxRsRouteTestSubResource {

    public JaxRsRouteTestSubResource(String id) {
        this.id = id;
    }

    private final String id;

    @Path("/details")
    @GET
    public String details() {
        return id;
    }

}
