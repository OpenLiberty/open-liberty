/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry.internal.apps.agent;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

public class AgentSubResource {

    public AgentSubResource(String id) {
        this.id = id;
    }

    private final String id;

    @Path("/details")
    @GET
    public String details() {
        return id;
    }

}