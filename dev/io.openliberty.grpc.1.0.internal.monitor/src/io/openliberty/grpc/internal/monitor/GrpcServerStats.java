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
import com.ibm.websphere.monitor.meters.StatisticsMeter;

import io.openliberty.grpc.monitor.GrpcServerStatsMXBean;

/**
 * This is used to report gRPC Server related statistics. </br>
 * Statistic reported:
 * <ul>
 * <li>Total number of RPCs started on the server.
 * <li>Total number of RPCs completed on the server, regardless of success or
 * failure.
 * <li>Histogram of response latency of RPCs handled by the server, in
 * milliseconds.
 * <li>Total number of stream messages received from the client.
 * <li>Total number of stream messages sent by the server.
 * </ul>
 */
public class GrpcServerStats extends Meter implements GrpcServerStatsMXBean {

	private String appName;
	private String serviceName;

	private Counter receivedMsgCount;
	private Counter sentMsgCount;
	private Counter serverStarted;
	private Counter serverHandled;
    private StatisticsMeter responseTime;

	public GrpcServerStats(String aName, String sName) {
		setAppName(aName);
		setServiceName(sName);

		serverStarted = new Counter();
		serverStarted.setDescription("This shows total number of RPCs started on the server");
		serverStarted.setUnit("ns");

		serverHandled = new Counter();
		serverHandled.setDescription("This shows total number of RPCs completed on the server");
		serverHandled.setUnit("ns");

		receivedMsgCount = new Counter();
		receivedMsgCount.setDescription("This shows number of received stream messages");
		receivedMsgCount.setUnit("ns");

		sentMsgCount = new Counter();
		sentMsgCount.setDescription("This shows number of stream messages sent by the service");
		sentMsgCount.setUnit("ns");
		
        responseTime = new StatisticsMeter();
        responseTime.setDescription("Average RPC Response Time");
        responseTime.setUnit("ns");
	}

	@Override
	public String getAppName() {
		return this.appName;
	}

	@Override
	public String getDescription() {
		return "Report gRPC statistics for the specified service and application.";
	}

	/**
	 * @param appName the application name which contains the specified gRPC service
	 */
	public void setAppName(String appName) {
		this.appName = appName;
	}

	/**
	 * @param name the servletName to set
	 */
	public void setServiceName(String name) {
		this.serviceName = name;
	}

	public void recordCallStarted() {
		serverStarted.incrementBy(1);
	}

	public void recordServerHandled() {
		serverHandled.incrementBy(1);
	}

	/**
	 * This will increment received messages count by the specified number.
	 * 
	 * @param i
	 */
	public void incrementReceivedMsgCountBy(int i) {
		this.receivedMsgCount.incrementBy(i);
	}

	/**
	 * This will increment sent messages count by the specified number.
	 * 
	 * @param i
	 */
	public void incrementSentMsgCountBy(int i) {
		this.sentMsgCount.incrementBy(i);
	}

	public void recordLatency(long latencyMs) {
		responseTime.addDataPoint(latencyMs);
	}

	@Override
	public String getServiceName() {
		return serviceName;
	}

	@Override
	public long getReceivedMessagesCount() {
		return receivedMsgCount.getCurrentValue();
	}

	@Override
	public long getSentMessagesCount() {
		return sentMsgCount.getCurrentValue();
	}

	@Override
	public long getRpcStartedCount() {
		return serverStarted.getCurrentValue();
	}

	@Override
	public long getRpcCompletedCount() {
		return serverHandled.getCurrentValue();
	}
    @Override
    public double getResponseTime() {
        return responseTime.getMean();
    }

    @Override
    public StatisticsMeter getResponseTimeDetails() {
        return responseTime;
    }
}
