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

import com.ibm.websphere.monitor.meters.Counter;
import com.ibm.websphere.monitor.meters.Meter;

import io.openliberty.grpc.GrpcClientStatsMXBean;

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
public class GrpcClientMetrics extends Meter implements GrpcClientStatsMXBean {
	private final Counter rpcStarted;
	private final Counter rpcCompleted;
	private final Counter streamMessagesReceived;
	private final Counter streamMessagesSent;

	private final GrpcMethod method;

	public GrpcClientMetrics(GrpcMethod method) {
		this.method = method;

		rpcStarted = new Counter();
		rpcStarted.setDescription("This shows total number of RPCs started on the client");
		rpcStarted.setUnit("ns");

		rpcCompleted = new Counter();
		rpcCompleted.setDescription("This shows total number of RPCs completed on the client");
		rpcCompleted.setUnit("ns");

		streamMessagesReceived = new Counter();
		streamMessagesReceived.setDescription("This shows total number of stream messages received from the server");
		streamMessagesReceived.setUnit("ns");

		streamMessagesSent = new Counter();
		streamMessagesSent.setDescription("This shows total number of stream messages sent by the client");
		streamMessagesSent.setUnit("ns");
	}

	@Override
	public long getRpcStartedCount() {
		return rpcStarted.getCurrentValue();
	}

	@Override
	public long getRpcCompletedCount() {
		return rpcCompleted.getCurrentValue();
	}

	@Override
	public long getReceivedMessagesCount() {
		return streamMessagesReceived.getCurrentValue();
	}

	@Override
	public long getSentMessagesCount() {
		return streamMessagesSent.getCurrentValue();
	}

	public void recordCallStarted() {
		rpcStarted.incrementBy(1);
		System.out.println(String.format("ANNA client RPC started: service[%s] method[%s] - %s", method.serviceName(),
				method.methodName(), getRpcStartedCount()));
	}

	public void recordClientHandled() {
		rpcCompleted.incrementBy(1);
		System.out.println(String.format("ANNA client RPC completed: service[%s] method[%s] - %s", method.serviceName(),
				method.methodName(), getRpcCompletedCount()));
	}

	/**
	 * This will increment received messages count by the specified number.
	 * 
	 * @param i
	 */
	public void incrementReceivedMsgCountBy(int i) {
		this.streamMessagesReceived.incrementBy(i);
		System.out.println(String.format("ANNA client received msg: service[%s] method[%s] - %s", method.serviceName(),
				method.methodName(), getReceivedMessagesCount()));
	}

	/**
	 * This will increment sent messages count by the specified number.
	 * 
	 * @param i
	 */
	public void incrementSentMsgCountBy(int i) {
		this.streamMessagesSent.incrementBy(i);
		System.out.println(String.format("ANNA client sent msg: service[%s] method[%s] - %s", method.serviceName(),
				method.methodName(), getSentMessagesCount()));
	}

	public void recordLatency(double latencySec) {
		// TODO Auto-generated method stub

	}
}
