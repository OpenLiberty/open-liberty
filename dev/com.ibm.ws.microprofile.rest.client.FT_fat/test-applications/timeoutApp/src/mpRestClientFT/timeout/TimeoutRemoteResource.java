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
package mpRestClientFT.timeout;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;

@Path("/timeout")
public class TimeoutRemoteResource {
    Logger LOG = Logger.getLogger(TimeoutRemoteResource.class.getName());

    @GET
    @Path("/waitXSeconds/{x}")
    public String waitXSeconds(@PathParam("x") int x) {
        LOG.info("waitXSeconds " + x + " entry");
        try {
            Thread.sleep(x * 1000);
            return "Waited " + x + " seconds";
        } catch (Throwable t) {
            LOG.log(Level.INFO, "waitXSeconds " + x + " exception", t);
            throw new WebApplicationException(t);
        } finally {
            LOG.info("waitXSeconds " + x + " exit");
        }
    }
}
