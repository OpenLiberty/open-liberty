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
	public MeterCollection<GrpcClientStats> grpcClientCountByName = new MeterCollection<GrpcClientStats>(
			"GrpcClient", this);

	@ProbeAtEntry
	@ProbeSite(clazz = "io.openliberty.grpc.internal.monitor.GrpcClientStatsMonitor", method = "recordCallStarted")
	public void atRpcCallStart(@This Object clientStats) {
		GrpcClientStatsMonitor stats = (GrpcClientStatsMonitor) clientStats;
		getGrpcClientStats(stats.getMethod()).recordCallStarted();
	}

	@ProbeAtEntry
	@ProbeSite(clazz = "io.openliberty.grpc.internal.monitor.GrpcClientStatsMonitor", method = "recordClientHandled")
	public void atGrpcClientHandled(@This Object clientStats) {
		GrpcClientStatsMonitor stats = (GrpcClientStatsMonitor) clientStats;
		getGrpcClientStats(stats.getMethod()).recordClientHandled();
	}

	@ProbeAtReturn
	@ProbeSite(clazz = "io.openliberty.grpc.internal.monitor.GrpcClientStatsMonitor", method = "recordMsgReceived")
	public void atClientMsgReceived(@This Object clientStats) {
		GrpcClientStatsMonitor stats = (GrpcClientStatsMonitor) clientStats;
		getGrpcClientStats(stats.getMethod()).incrementReceivedMsgCountBy(1);
	}

	@ProbeAtReturn
	@ProbeSite(clazz = "io.openliberty.grpc.internal.monitor.GrpcClientStatsMonitor", method = "recordMsgSent")
	public void atGrpcClientMsgSent(@This Object clientStats) {
		GrpcClientStatsMonitor stats = (GrpcClientStatsMonitor) clientStats;
		getGrpcClientStats(stats.getMethod()).incrementSentMsgCountBy(1);
	}

	private synchronized GrpcClientStats getGrpcClientStats(GrpcMethod method) {
		String key = method.serviceName() + METRIC_KEY_DELIMETER + method.methodName();
		GrpcClientStats stats = grpcClientCountByName.get(key);
		if (stats == null) {
			stats = new GrpcClientStats(method);
			grpcClientCountByName.put(key, stats);
		}
		return stats;
	}
}
