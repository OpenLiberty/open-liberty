/*
 * Copyright (c) 2015,2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package test.iiop.interceptor;

import org.omg.CORBA.LocalObject;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.PortableInterceptor.ForwardRequest;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;

/**
 *
 */
public class ServerRequestInterceptorImpl extends LocalObject implements ServerRequestInterceptor {
    private static final long serialVersionUID = 1L;

    @Override
    public void receive_request_service_contexts(ServerRequestInfo ri) throws ForwardRequest {}

    @Override
    public void receive_request(ServerRequestInfo ri) throws ForwardRequest {
        System.out.println("### in receive_request()");
        System.out.println("###    operation: '" + ri.operation() + "'");
        if (ri.operation().equals("sayHello")) {
            System.out.println("###    raising NO_PERMISSION");
            throw new NO_PERMISSION("Can't touch this.");
        }
    }

    @Override
    public void send_reply(ServerRequestInfo ri) {}

    @Override
    public void send_exception(ServerRequestInfo ri) throws ForwardRequest {
        System.out.println("### in send_exception()");
    }

    @Override
    public void send_other(ServerRequestInfo ri) throws ForwardRequest {}

    @Override
    public String name() {
        return ServerRequestInterceptorImpl.class.getName();
    }

    @Override
    public void destroy() {}
}
