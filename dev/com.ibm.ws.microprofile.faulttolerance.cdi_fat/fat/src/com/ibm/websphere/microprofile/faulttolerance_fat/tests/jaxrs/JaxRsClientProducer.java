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
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.jaxrs;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

@ApplicationScoped
public class JaxRsClientProducer {

    @Resource
    private ManagedExecutorService executorService;

    /**
     * Produces a configured JAX-RS Client which we can Inject elsewhere
     */
    @Produces
    @ApplicationScoped
    public Client produceClient() {
        return ClientBuilder.newBuilder()
                        .executorService(executorService)
                        .build();
    }

}
