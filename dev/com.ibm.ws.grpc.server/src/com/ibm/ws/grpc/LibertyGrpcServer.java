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

package com.ibm.ws.grpc;

import com.ibm.ws.http2.Http2Consumers;

import io.grpc.InternalChannelz.SocketStats;
import io.grpc.InternalInstrumented;
import io.grpc.internal.InternalServer;
import io.grpc.internal.ServerListener;
import io.grpc.internal.ServerTransportListener;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

final class LibertyGrpcServer implements InternalServer {

	private static final String CLASS_NAME = LibertyGrpcServer.class.getName();
	private static final Logger logger = Logger.getLogger(LibertyGrpcServer.class.getName());
	
	private GrpcConnectionHandler handler;
	private LibertyServerTransport transport;
		
	public LibertyGrpcServer() {
	}

	@Override
	public void start(ServerListener listener) throws IOException {
		transport = new LibertyServerTransport();
		ServerTransportListener serverTransportListener = listener.transportCreated(transport);
		handler = new GrpcConnectionHandler(serverTransportListener);
		Http2Consumers.addHandler(handler);
	}

	@Override
	public SocketAddress getListenSocketAddress() {
		return null;
	}

	@Override
	public InternalInstrumented<SocketStats> getListenSocketStats() {
		return null;
	}

	@Override
	public void shutdown() {
		Utils.traceMessage(logger, CLASS_NAME, Level.FINE, "shutdown",
				"removing handler: " + handler + " and shutting down transport: " + transport);
		Http2Consumers.removeHandler(handler);
		transport.shutdown();
	}
}
