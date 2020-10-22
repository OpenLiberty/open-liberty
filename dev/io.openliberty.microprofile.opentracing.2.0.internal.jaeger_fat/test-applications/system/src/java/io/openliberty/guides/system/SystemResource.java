/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Initial implementation
 *******************************************************************************/
package io.openliberty.guides.system;

import java.util.Properties;
import java.util.logging.Logger;

// CDI
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
// JAX-RS
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@RequestScoped
@Path("properties")
public class SystemResource {

    private static final Logger LOGGER = Logger.getLogger(SystemResource.class.getName());

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Properties getProperties() {
        LOGGER.info("System: Getting system properties");
        return System.getProperties();
    }
}
