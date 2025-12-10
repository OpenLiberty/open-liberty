/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.jaxrs21.client.threadleak.server;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.util.concurrent.TimeUnit;

/**
 * CDI producer that creates a singleton JAX-RS Client.
 * This is the key pattern - ONE client instance reused for all requests.
 */
@ApplicationScoped
public class ComputerRestClientProducer {

    @Produces
    @ComputerRestClient
    public RestClient createRestClient() {
        // Create ONE client with timeout settings
        Client client = ClientBuilder.newBuilder()
                .connectTimeout(10_000, TimeUnit.MILLISECONDS)
                .readTimeout(10_000, TimeUnit.MILLISECONDS)
                .build();

        return new RestClient(client);
    }
}

