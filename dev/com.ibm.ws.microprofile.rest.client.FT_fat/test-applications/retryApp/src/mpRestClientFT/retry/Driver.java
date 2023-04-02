/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
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
package mpRestClientFT.retry;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@RequestScoped
public class Driver {

    @Inject
    @RestClient
    private RetryClient client;

    @Loggable
    @Retry(retryOn= {WebApplicationException.class}, maxRetries = 2)
    String failThenSucceed() {
        return client.failThenSucceed();
    }

    @Retry(retryOn= {WebApplicationException.class}, maxRetries = 2)
    String failThenSucceed(RetryClient client) {
        return client.failThenSucceed();
    }

    String failThenSucceed_RestClient() {
        return client.failThenSucceed();
    }

    @Loggable
    String alwaysSucceed() {
        return client.alwaysSucceed();
    }
}
