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

import com.ibm.websphere.monitor.jmx.StatisticsMeter;

/**
 * Management interface for MBeans with names of the form
 * "WebSphere:type=GrpcClientStats,name=*" where * is the name of a gRPC method
 * of the form <serviceName>/<methodName>.
 * 
 * @ibm-api
 */
public interface GrpcClientStatsMXBean {
	/**
	 * Retrieves the total number of RPCs started on the client.
	 * 
	 * @return the total number of RPCs started on the client
	 */
	public long getRpcStartedCount();

	/**
	 * Retrieves the total number of RPCs completed on the client,
	 * 
	 * @return the total number of RPCs completed on the client
	 */
	public long getRpcCompletedCount();

	/**
	 * Retrieves the total number of stream messages that the client has received
	 * from the gRPC server.
	 * 
	 * @return the total number of stream messages received from the server
	 */
	public long getReceivedMessagesCount();

	/**
	 * Retrieves the total number of stream messages sent by the client.
	 * 
	 * @return the total number of stream messages sent by the client
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
}
