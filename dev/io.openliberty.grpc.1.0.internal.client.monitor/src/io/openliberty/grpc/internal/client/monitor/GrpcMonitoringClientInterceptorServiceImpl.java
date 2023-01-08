/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.grpc.internal.client.monitor;

import java.time.Clock;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.MethodDescriptor;
import io.openliberty.grpc.client.monitor.GrpcMonitoringClientInterceptorService;

/**
 * A service which provides GrpcMonitoringClientInterceptor instances
 */
@Component(service = { GrpcMonitoringClientInterceptorService.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class GrpcMonitoringClientInterceptorServiceImpl implements GrpcMonitoringClientInterceptorService {

	@Override
	public ClientInterceptor createInterceptor() {
		return new GrpcMonitoringClientInterceptor();
	}

	/**
	 * A {@link ClientInterceptor} which gathers statistics about incoming GRPC
	 * calls.
	 */
	private class GrpcMonitoringClientInterceptor implements ClientInterceptor {

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
}
