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

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.MethodDescriptor;

public class HelloWorldClientInterceptor implements ClientInterceptor {

    protected static final Class<?> c = HelloWorldClientInterceptor.class;
    private static final Logger LOG = Logger.getLogger(c.getName());

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(final MethodDescriptor<ReqT, RespT> methodDescriptor, final CallOptions callOptions,
                                                               final Channel channel) {

        LOG.info(this.getClass().getCanonicalName() + " has been invoked!");
        return channel.newCall(methodDescriptor, callOptions);
    }

}