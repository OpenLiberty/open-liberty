/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package remoteApp.basic;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("/wait")
public class WaitService {
    Logger LOG = Logger.getLogger(WaitService.class.getName());

    @GET
    public Response waitFor(@QueryParam("waitTime") @DefaultValue("20") int seconds) {
        Response response;
        try {
            LOG.info("About to sleep for " + seconds + " seconds");
            Thread.sleep(seconds * 1000);
            LOG.info("Waking up now");
            response = Response.ok("Waited for " + seconds + " seconds").build();
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "Caught unexpected exception", t);
            response = Response.status(500).entity("Unexpected exception: " + t).build();
        }
        return response;
    }
}
