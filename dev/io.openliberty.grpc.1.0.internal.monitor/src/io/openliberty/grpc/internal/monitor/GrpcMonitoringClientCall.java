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

import java.time.Clock;

import io.grpc.ClientCall;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;

/**
 * A {@link SimpleForwardingClientCall} which increments counters for the RPC
 * call.
 */
public class GrpcMonitoringClientCall<ReqT, RespT>
		extends ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT> {
	private final GrpcClientStatsMonitor clientMetrics;
	private final GrpcMethod grpcMethod;
	private final Clock clock;

	protected GrpcMonitoringClientCall(ClientCall<ReqT, RespT> delegate, GrpcClientStatsMonitor clientMetrics,
			GrpcMethod grpcMethod, Clock clock) {
		super(delegate);
		this.clientMetrics = clientMetrics;
		this.grpcMethod = grpcMethod;
		this.clock = clock;
	}

	@Override
	public void start(ClientCall.Listener<RespT> delegate, Metadata metadata) {
		clientMetrics.recordCallStarted();
		super.start(new GrpcMonitoringClientCallListener<>(delegate, clientMetrics, grpcMethod, clock),
				metadata);
	}

	@Override
	public void sendMessage(ReqT requestMessage) {
		if (grpcMethod.streamsRequests()) {
			clientMetrics.recordMsgSent();;
		}
		super.sendMessage(requestMessage);
	}
}
