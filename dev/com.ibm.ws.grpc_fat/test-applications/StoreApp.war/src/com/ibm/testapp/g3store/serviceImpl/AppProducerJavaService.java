/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.testapp.g3store.serviceImpl;

import java.io.IOException;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class AppProducerJavaService {

    public static void main(String[] args) throws IOException, InterruptedException {

        final Server server = ServerBuilder.forPort(9080)
                        .addService(new StoreProducerService())
                        .build();

        server.start();

        Runtime.getRuntime()
                        .addShutdownHook(new Thread(() -> {
                            //System.out.println("Received Shutdown Request");
                            server.shutdown();
                            //System.out.println("Successfully stopped the server");
                        }));

        server.awaitTermination();
    }

}
