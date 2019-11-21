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

import com.ibm.wsspi.http.ee8.Http2Consumers;

import io.grpc.InternalChannelz.SocketStats;
import io.grpc.InternalInstrumented;
import io.grpc.internal.InternalServer;
import io.grpc.internal.ServerListener;
import io.grpc.internal.ServerTransportListener;

import java.io.IOException;
import java.net.SocketAddress;

final class LibertyServer implements InternalServer {
	
	private final SocketAddress address;
	private GrpcConnectionHandler handler;
	private LibertyServerTransport transport;
	
	public LibertyServer(SocketAddress sa) {
		address = sa;
	}

	@Override
	public void start(ServerListener listener) throws IOException {
		//System.out.println("wtl: libertyserver start()");
		transport = new LibertyServerTransport();
		ServerTransportListener serverTransportListener = listener.transportCreated(transport);
		handler = new GrpcConnectionHandler(serverTransportListener);
		Http2Consumers.addHandler(handler);
	}

	@Override
	public SocketAddress getListenSocketAddress() {
		return address;
	}

	@Override
	public InternalInstrumented<SocketStats> getListenSocketStats() {
		return null;
	}

	@Override
	public void shutdown() {
		System.out.println("WTL: shutdown");
		Http2Consumers.removeHandler(handler);
		transport.shutdown();
	}
}
