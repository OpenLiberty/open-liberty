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

import com.ibm.websphere.monitor.annotation.Monitor;
import com.ibm.websphere.monitor.annotation.ProbeAtEntry;
import com.ibm.websphere.monitor.annotation.ProbeAtReturn;
import com.ibm.websphere.monitor.annotation.ProbeSite;
import com.ibm.websphere.monitor.annotation.PublishedMetric;
import com.ibm.websphere.monitor.annotation.This;
import com.ibm.websphere.monitor.meters.MeterCollection;

/**
 * Monitor class for gRPC Client. </br>
 * This class is responsible for managing the gRPC client MXBean object, as well
 * as the actual updating of the values of the counters defined in the MXBean
 * object.
 */
@Monitor(group = "GrpcClient")
public class GrpcClientMonitor {
	private final static String METRIC_KEY_DELIMETER = "/";

	@PublishedMetric
	public MeterCollection<GrpcClientMetrics> grpcClientCountByName = new MeterCollection<GrpcClientMetrics>(
			"GrpcClient", this);

	@ProbeAtEntry
	// @ProbeSite(clazz =
	// "io.openliberty.grpc.internal.monitor.GrpcClientStatsMonitor", method =
	// "recordCallStarted")
	@ProbeSite(clazz = "com.ibm.ws.threading.internal.ExecutorServiceImpl", method = "execute")
	public void atRpcCallStart(@This GrpcClientStatsMonitor clientStats) {
		System.out.println("atRpcCallStart");
		GrpcClientMetrics stats = getGrpcClientStats(clientStats.getMethod());
		stats.recordCallStarted();
	}

	@ProbeAtEntry
	@ProbeSite(clazz = "io.openliberty.grpc.internal.monitor.GrpcClientStatsMonitor", method = "recordClientHandled")
	public void atGrpcClientHandled(@This GrpcClientStatsMonitor clientStats) {
		System.out.println("atGrpcClientHandled");
		GrpcClientMetrics stats = getGrpcClientStats(clientStats.getMethod());
		stats.recordClientHandled();
	}

	@ProbeAtReturn
	@ProbeSite(clazz = "io.openliberty.grpc.internal.monitor.GrpcClientStatsMonitor", method = "recordMsgReceived")
	public void atClientMsgReceived(@This GrpcClientStatsMonitor clientStats) {
		System.out.println("atGrpcClientMsgReceived");
		GrpcClientMetrics stats = getGrpcClientStats(clientStats.getMethod());
		stats.incrementReceivedMsgCountBy(1);
	}

	@ProbeAtReturn
	@ProbeSite(clazz = "io.openliberty.grpc.internal.monitor.GrpcClientStatsMonitor", method = "recordMsgSent")
	public void atGrpcClientMsgSent(@This GrpcClientStatsMonitor clientStats) {
		System.out.println("atGrpcClientMsgSent");
		GrpcClientMetrics stats = getGrpcClientStats(clientStats.getMethod());
		stats.incrementSentMsgCountBy(1);
	}

	private synchronized GrpcClientMetrics getGrpcClientStats(GrpcMethod method) {
		String key = method.serviceName() + METRIC_KEY_DELIMETER + method.methodName();
		GrpcClientMetrics stats = grpcClientCountByName.get(key);
		if (stats == null) {
			stats = new GrpcClientMetrics(method);
			grpcClientCountByName.put(key, stats);
		}
		return stats;
	}

}
