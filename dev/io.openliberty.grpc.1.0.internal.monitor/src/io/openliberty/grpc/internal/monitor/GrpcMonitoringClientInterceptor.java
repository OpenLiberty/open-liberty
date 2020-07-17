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

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.MethodDescriptor;

/**
 * A {@link ClientInterceptor} which gathers statistics about incoming GRPC
 * calls.
 */
public class GrpcMonitoringClientInterceptor implements ClientInterceptor {
	private final Clock clock;

	public GrpcMonitoringClientInterceptor() {
		this.clock = Clock.systemDefaultZone();
	}
	
	@Override
	public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> methodDescriptor,
			CallOptions callOptions, Channel channel) {
		GrpcMethod grpcMethod = GrpcMethod.of(methodDescriptor);
		GrpcClientStatsMonitor metrics = new GrpcClientStatsMonitor(grpcMethod);
		return new GrpcMonitoringClientCall<>(channel.newCall(methodDescriptor, callOptions), metrics, grpcMethod,
				clock);
	}

}
