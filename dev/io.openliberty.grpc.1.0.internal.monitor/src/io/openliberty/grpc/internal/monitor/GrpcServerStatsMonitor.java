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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This is used to report gRPC Server related statistics. </br>
 * Statistic reported:
 * <ul>
 * <li>Total number of RPCs started on the server.
 * <li>Total number of RPCs completed on the server, regardless of success or
 * failure.
 * <li>TODO Histogram of response latency of RPCs handled by the server, in
 * seconds.
 * <li>Total number of stream messages received from the client.
 * <li>Total number of stream messages sent by the server.
 * </ul>
 */
public class GrpcServerStatsMonitor {

	private String appName;
	private String serviceName;

	private AtomicInteger receivedMsgCount = new AtomicInteger();
	private AtomicInteger sentMsgCount = new AtomicInteger();

	public GrpcServerStatsMonitor(String aName, String sName) {
		setAppName(aName);
		setServiceName(sName);
	}

	public String getAppName() {
		return this.appName;
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
		System.out.println(String.format("ANNA started: service[%s] application[%s]", getServiceName(), getAppName()));
	}

	public void recordServerHandled() {
		System.out.println(String.format("ANNA handled: service[%s] application[%s]", getServiceName(), getAppName()));
	}

	/**
	 * This will increment received messages count
	 */
	public void recordMsgReceived() {
		this.receivedMsgCount.addAndGet(1);
		System.out.println(String.format("ANNA received msg: service[%s] application[%s] - %s", getServiceName(),
				getAppName(), receivedMsgCount.get()));
	}

	/**
	 * This will increment sent messages count 
	 */
	public void recordMsgSent() {
		this.sentMsgCount.addAndGet(1);
		System.out.println(String.format("ANNA sent msg: service[%s] application[%s] - %s", getServiceName(),
				getAppName(), sentMsgCount.get()));
	}

	public void recordLatency(double latencySec) {
		// TODO implement
	}

	public String getServiceName() {
		return serviceName;
	}
}
