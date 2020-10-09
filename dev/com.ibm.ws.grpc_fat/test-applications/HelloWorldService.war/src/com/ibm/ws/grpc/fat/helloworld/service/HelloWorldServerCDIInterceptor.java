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

package com.ibm.ws.grpc.fat.helloworld.service;

import java.util.logging.Logger;

import javax.inject.Inject;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

public class HelloWorldServerCDIInterceptor implements ServerInterceptor {

    protected static final Class<?> c = HelloWorldServerCDIInterceptor.class;
    private static final Logger LOG = Logger.getLogger(c.getName());

    @Inject
    GreetingCDIBean bean;

    public static final Context.Key<Object> EXAMPLE_CONTEXT_KEY = Context.key("exampleContext");

    @Override
    public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
                                                      ServerCallHandler<ReqT, RespT> next) {
        String message = bean.getInterceptorMessage();
        Context context = Context
                        .current()
                        .withValue(EXAMPLE_CONTEXT_KEY, message);
        LOG.info("HelloWorldServerCDIInterceptor message: " + message);
        return Contexts.interceptCall(context, call, headers, next);
    }
}