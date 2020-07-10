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
package io.openliberty.grpc.internal.monitor;

import io.grpc.ForwardingServerCallListener;
import io.grpc.ServerCall;

/**
 * A {@link ForwardingServerCallListener} which updates gRPC metrics for a
 * single gRPC service based on updates received from GRPC.
 */
class GrpcMonitoringServerCallListener<R> extends ForwardingServerCallListener<R> {
	private final ServerCall.Listener<R> delegate;
	private final GrpcMethod grpcMethod;
	private final GrpcServerMetrics serverMetrics;

	GrpcMonitoringServerCallListener(ServerCall.Listener<R> delegate, GrpcServerMetrics serverMetrics,
			GrpcMethod grpcMethod) {
		this.delegate = delegate;
		this.serverMetrics = serverMetrics;
		this.grpcMethod = grpcMethod;
	}

	@Override
	protected ServerCall.Listener<R> delegate() {
		return delegate;
	}

	@Override
	public void onMessage(R request) {
		if (grpcMethod.streamsRequests()) {
			serverMetrics.incrementReceivedMsgCountBy(1);
		}
		super.onMessage(request);
	}
}