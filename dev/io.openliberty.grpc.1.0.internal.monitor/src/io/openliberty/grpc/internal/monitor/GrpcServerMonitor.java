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
 * Monitor class for gRPC server. </br>
 * This class is responsible for managing the gRPC server MXBean object, as well
 * as the actual updating of the values of the counters defined in the MXBean
 * object.
 */
@Monitor(group = "GrpcServer")
public class GrpcServerMonitor {

	@PublishedMetric
	public MeterCollection<GrpcServerMetrics> grpcServerCountByName = new MeterCollection<GrpcServerMetrics>(
			"GrpcServer", this);

	@ProbeAtEntry
	@ProbeSite(clazz = "io.openliberty.grpc.internal.monitor.GrpcServerStatsMonitor", method = "recordCallStarted")
	// @ProbeSite(clazz = "com.ibm.ws.threading.internal.ExecutorServiceImpl",
	// method = "execute")
	public void atGrpcServerStart(@This GrpcServerStatsMonitor serverStats) {
		System.out.println("atGrpcServerStart");
		GrpcServerMetrics stats = getGrpcServerStats(serverStats.getAppName(), serverStats.getServiceName());
		stats.recordCallStarted();
	}

	@ProbeAtEntry
	@ProbeSite(clazz = "io.openliberty.grpc.internal.monitor.GrpcServerStatsMonitor", method = "recordServerHandled")
	public void atGrpcServerHandled(@This GrpcServerStatsMonitor serverStats) {
		System.out.println("atGrpcServerHandled");
		GrpcServerMetrics stats = getGrpcServerStats(serverStats.getAppName(), serverStats.getServiceName());
		stats.recordServerHandled();
	}

	@ProbeAtReturn
	@ProbeSite(clazz = "io.openliberty.grpc.internal.monitor.GrpcServerStatsMonitor", method = "recordMsgReceived")
	public void atGrpcServerMsgReceived(@This GrpcServerStatsMonitor serverStats) {
		System.out.println("atGrpcServerMsgReceived");
		GrpcServerMetrics stats = getGrpcServerStats(serverStats.getAppName(), serverStats.getServiceName());
		stats.incrementReceivedMsgCountBy(1);
	}

	@ProbeAtReturn
	@ProbeSite(clazz = "io.openliberty.grpc.internal.monitor.GrpcServerStatsMonitor", method = "recordMsgSent")
	public void atGrpcServerMsgSent(@This GrpcServerStatsMonitor serverStats) {
		System.out.println("atGrpcServerMsgSent");
		GrpcServerMetrics stats = getGrpcServerStats(serverStats.getAppName(), serverStats.getServiceName());
		stats.incrementSentMsgCountBy(1);
	}

	private synchronized GrpcServerMetrics getGrpcServerStats(String appName, String serviceName) {
		GrpcServerMetrics stats = grpcServerCountByName.get(serviceName);
		if (stats == null) {
			stats = new GrpcServerMetrics(appName, serviceName);
			grpcServerCountByName.put(serviceName, stats);
		}
		return stats;
	}

}
