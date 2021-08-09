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
package mpRestClientFT.retry;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/retry")
public class RetryRemoteResource {
    Logger LOG = Logger.getLogger(RetryRemoteResource.class.getName());

    static AtomicInteger failThenSucceed = new AtomicInteger(1);

    @GET
    @Path("/failThenSucceed")
    public String failThenSucceed() {
        LOG.info("failThenSucceed " + failThenSucceed.get());
        if (failThenSucceed.getAndIncrement() % 2 == 1) { // fail on odd numbers
            throw new IllegalStateException("Expected Failure");
        }
        return "Success"; // succeed on evens
    }

    @GET
    @Path("/alwaysSucceed")
    public String alwaysSucceed() {
        return "Success";
    }

    @GET
    @Path("/alwaysFail")
    public String alwaysFail() {
        throw new IllegalStateException("Expected Failure");
    }
}
