/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry.internal.apps.agentconfig;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/**
 * A test rest client
 */
@Path("/")
public interface TestClient {

    @GET
    @Path("/clientTarget")
    public String clientTarget();

}
