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
import java.time.Instant;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.Status;

/**
 * A {@link ForwardingServerCall} which updates gRPC metrics based on the
 * server-side actions taken for a single RPC, e.g., messages sent, latency,
 * etc.
 */
public class GrpcMonitoringServerCall<R, S> extends ForwardingServerCall.SimpleForwardingServerCall<R, S> {
	private static final long MILLIS_PER_SECOND = 1000L;

	private final Clock clock;
	private final GrpcMethod grpcMethod;
	private final GrpcServerStatsMonitor serverMetrics;
	private final Instant startInstant;

	GrpcMonitoringServerCall(ServerCall<R, S> delegate, Clock clock, GrpcMethod grpcMethod, GrpcServerStatsMonitor serverMetrics) {
		super(delegate);
		this.clock = clock;
		this.grpcMethod = grpcMethod;
		this.serverMetrics = serverMetrics;
		this.startInstant = clock.instant();

		reportStartMetrics();
	}

	@Override
	public void close(Status status, Metadata responseHeaders) {
		reportEndMetrics(status);
		super.close(status, responseHeaders);
	}

	@Override
	public void sendMessage(S message) {
		if (grpcMethod.streamsResponses()) {
			serverMetrics.recordMsgSent();
		}
		super.sendMessage(message);
	}

	private void reportStartMetrics() {
		serverMetrics.recordCallStarted();
	}

	private void reportEndMetrics(Status status) {
		serverMetrics.recordServerHandled();//(status.getCode());
		double latencySec = (clock.millis() - startInstant.toEpochMilli()) / (double) MILLIS_PER_SECOND;
		serverMetrics.recordLatency(latencySec);
	}
}
