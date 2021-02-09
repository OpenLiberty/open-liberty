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

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.openliberty.grpc.server.monitor.GrpcMonitoringServerInterceptorService;

/**
 * A service which provides GrpcMonitoringServerInterceptor instances
 */
@SuppressWarnings("restriction")
@Component(service = { GrpcMonitoringServerInterceptorService.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class GrpcMonitoringServerInterceptorServiceImpl implements GrpcMonitoringServerInterceptorService {

	@Override
	public ServerInterceptor createInterceptor(String serviceName, String appName) {
		return new GrpcMonitoringServerInterceptor(serviceName, appName);
	}

	/**
	 * A {@link ServerInterceptor} which gathers statistics about incoming GRPC
	 * calls.
	 */
	private class GrpcMonitoringServerInterceptor implements ServerInterceptor {

		private String serviceName;
		private String appName;
		private final Clock clock;

		public GrpcMonitoringServerInterceptor(String serviceName, String appName) {
			this.serviceName = serviceName;
			this.appName = appName;
			this.clock = Clock.systemDefaultZone();
		}

		@Override
		public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
			ServerCallHandler<ReqT, RespT> next) {
			MethodDescriptor<ReqT, RespT> methodDescriptor = call.getMethodDescriptor();
			GrpcMethod grpcMethod = GrpcMethod.of(methodDescriptor);
			GrpcServerStatsMonitor metrics = new GrpcServerStatsMonitor(appName, serviceName);
			ServerCall<ReqT, RespT> monitoringCall = new GrpcMonitoringServerCall(call, clock, grpcMethod, metrics);
			return new GrpcMonitoringServerCallListener<>(next.startCall(monitoringCall, headers), metrics, grpcMethod);
		}
	}
}
