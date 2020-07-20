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

/**
 * Holds metrics used for client-side monitoring of gRPC services. </br>
 * Statistic reported:
 * <ul>
 * <li>Total number of RPCs started on the client.
 * <li>Total number of RPCs completed on the client, regardless of success or
 * failure.
 * <li>TODO Histogram of RPC response latency for completed RPCs, in seconds.
 * <li>Total number of stream messages received from the server.
 * <li>Total number of stream messages sent by the client.
 * </ul>
 */
public class GrpcClientStatsMonitor {

	private final GrpcMethod method;

	public GrpcClientStatsMonitor(GrpcMethod method) {
		this.method = method;
	}

	public void recordCallStarted() {
		System.out.println(String.format("ANNA client RPC started: service[%s] method[%s]", method.serviceName(),
				method.methodName()));
	}

	public void recordClientHandled() {
		System.out.println(String.format("ANNA client RPC completed: service[%s] method[%s]", method.serviceName(),
				method.methodName()));
	}

	public void recordMsgReceived() {
		System.out.println(String.format("ANNA client message received: service[%s] method[%s]", method.serviceName(),
				method.methodName()));
	}

	public void recordMsgSent() {
		System.out.println(String.format("ANNA client message sent: service[%s] method[%s]", method.serviceName(),
				method.methodName()));
	}

	public void recordLatency(double latencySec) {
		// TODO implement
	}

	public String getServiceName() {
		return method.serviceName();
	}

	public GrpcMethod getMethod() {
		return method;
	}
}
