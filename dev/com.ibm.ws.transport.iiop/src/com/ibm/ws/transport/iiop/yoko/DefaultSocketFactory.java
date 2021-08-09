/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transport.iiop.yoko;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.omg.CORBA.Policy;
import org.omg.CSIIOP.TransportAddress;
import org.omg.IOP.TaggedComponent;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.transport.iiop.yoko.helper.SocketFactoryHelper;

public class DefaultSocketFactory extends SocketFactoryHelper {

    private static final TraceComponent tc = Tr.register(DefaultSocketFactory.class);

    private static final int[] EMPTY_INT_ARRAY = {};

    public DefaultSocketFactory() {
        super(tc);
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, ConnectException {
        if (port == 0)
            Tr.error(tc, "PORT_ZERO", host);
        return createPlainSocket(host, port);
    }

    @Override
    public Socket createSelfConnection(InetAddress address, int port) throws IOException, ConnectException {
        return new Socket(address, port);
    }

    @Override
    public ServerSocket createServerSocket(int port, int backlog, String[] params) throws IOException {
        return createServerSocket(port, backlog, null, params);
    }

    @Override
    public ServerSocket createServerSocket(int port, int backlog, InetAddress address, String[] params) throws IOException {
        boolean soReuseAddr = true;
        for (int i = 0; i < params.length - 1; i++) {
            String param = params[i];
            if ("--soReuseAddr".equals(param)) {
                soReuseAddr = Boolean.parseBoolean(params[++i]);
                break;
            }
        }
        ServerSocket socket = new ServerSocket();
        IOException bindError = openSocket(port, backlog, address, socket, soReuseAddr);
        if (bindError != null) {
            throw bindError;
        }
        return socket;
    }

    @Override
    public int[] tags() {
        return EMPTY_INT_ARRAY;
    }

    @Override
    public TransportAddress[] getEndpoints(TaggedComponent tc, Policy[] policies) {
        throw new UnsupportedOperationException();
    }

}
