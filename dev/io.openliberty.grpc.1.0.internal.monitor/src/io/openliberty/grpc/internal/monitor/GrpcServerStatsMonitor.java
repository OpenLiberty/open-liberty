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
 * This is used to monitor gRPC Server related statistics. </br>
 * Statistic monitored:
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
public class GrpcServerStatsMonitor {

	private String appName;
	private String serviceName;
	private long latency;

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
	}

	public void recordServerHandled() {
	}

	/**
	 * This will increment received messages count
	 */
	public void recordMsgReceived() {
	}

	/**
	 * This will increment sent messages count
	 */
	public void recordMsgSent() {
	}

	public void recordLatency(long latencySec) {
		this.latency = latencySec;
	}

	public String getServiceName() {
		return serviceName;
	}
	
	public long getLatency() {
		return latency;
	}
}
