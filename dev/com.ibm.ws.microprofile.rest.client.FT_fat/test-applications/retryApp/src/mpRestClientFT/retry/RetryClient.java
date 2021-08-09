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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
@Path("/retry")
public interface RetryClient {

    @GET
    @Path("/failThenSucceed")
    @Loggable
    @Retry(retryOn= {WebApplicationException.class}, maxRetries = 2)
    String failThenSucceed();

    @GET
    @Path("/alwaysSucceed")
    @Loggable
    String alwaysSucceed();

    @GET
    @Path("/alwaysFail")
    @Fallback(fallbackMethod="alwaysSucceed")
    String useFallbackMethod();

    @GET
    @Path("/alwaysFail")
    @Fallback(fallbackMethod="defaultFallback")
    String useDefaultFallbackMethod();

    @GET
    @Path("/alwaysFail")
    @Fallback(MyFallback.class)
    String useDefaultFallbackClass();

    default String defaultFallback() {
        return "defaultFallback";
    }
}
