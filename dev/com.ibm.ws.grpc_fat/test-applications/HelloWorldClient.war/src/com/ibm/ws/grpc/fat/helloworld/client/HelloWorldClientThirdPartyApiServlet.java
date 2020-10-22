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
package com.ibm.ws.grpc.fat.helloworld.client;

import java.util.logging.Logger;

import javax.net.ssl.SSLException;
import javax.servlet.annotation.WebServlet;

import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;

@WebServlet(name = "grpcClientThirdPartyApi", urlPatterns = "/grpcClientThirdPartyApi")

public class HelloWorldClientThirdPartyApiServlet extends HelloWorldClientServlet {

    private static final long serialVersionUID = 1L;
    protected static final Class<?> c = HelloWorldClientThirdPartyApiServlet.class;
    private static final Logger LOG = Logger.getLogger(c.getName());

    @Override
    protected ManagedChannel createChannel(String address, int port, boolean useTls) throws SSLException {
        LOG.info("connecting to helloworld gRPC service at " + address + ":" + port);
        return NettyChannelBuilder.forAddress(address, port).usePlaintext().build();
    }

    @Override
    protected String getURLPath() {
        return "grpcClientThirdPartyApi";
    }
}
