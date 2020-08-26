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
package io.openliberty.grpc.monitor;

import com.ibm.websphere.monitor.jmx.StatisticsMeter;

/**
 * Management interface for MBeans with names of the form
 * "WebSphere:type=GrpcServerStats,name=*" where * is the name of a gRPC
 * service.
 * 
 * @ibm-api
 */
public interface GrpcServerStatsMXBean {

	/**
	 * Retrieves the gRPC service name.
	 * 
	 * @return the gRPC service name
	 */
	public String getServiceName();

	/**
	 * Retrieves the total number of RPCs started on the server.
	 * 
	 * @return the total number of RPCs started on the server
	 */
	public long getRpcStartedCount();

	/**
	 * Retrieves the total number of RPCs completed on the server,
	 * 
	 * @return the total number of RPCs completed on the server
	 */
	public long getRpcCompletedCount();

	/**
	 * Retrieves the total number of stream messages that the server has received
	 * for this gRPC service.
	 * 
	 * @return the total number of stream messages received for this service
	 */
	public long getReceivedMessagesCount();

	/**
	 * Retrieves the total number of stream messages sent by this gRPC service.
	 * 
	 * @return the total number of stream messages sent by this service
	 */
	public long getSentMessagesCount();

	/**
	 * Retrieves the average response time for specified RPC.
	 * 
	 * @return the average response time
	 */
	public double getResponseTime();

	/**
	 * Retrieves the statistical details on the response time.
	 * 
	 * @return response time details
	 */
	public StatisticsMeter getResponseTimeDetails();

	/**
	 * Retrieves the name of the application of which the gRPC service is a member.
	 * 
	 * @return application name
	 */
	public String getAppName();
}
