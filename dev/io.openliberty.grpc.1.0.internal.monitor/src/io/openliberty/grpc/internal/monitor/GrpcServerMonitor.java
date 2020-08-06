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
	public MeterCollection<GrpcServerStats> grpcServerCountByName = new MeterCollection<GrpcServerStats>(
			"GrpcServer", this);

	@ProbeAtEntry
	@ProbeSite(clazz = "io.openliberty.grpc.internal.monitor.GrpcServerStatsMonitor", method = "recordCallStarted")
	public void atGrpcServerStart(@This Object serverStats) {
		GrpcServerStatsMonitor stats = (GrpcServerStatsMonitor) serverStats;
		getGrpcServerStats(stats.getAppName(), stats.getServiceName()).recordCallStarted();		
	}

	@ProbeAtEntry
	@ProbeSite(clazz = "io.openliberty.grpc.internal.monitor.GrpcServerStatsMonitor", method = "recordServerHandled")
	public void atGrpcServerHandled(@This Object serverStats) {
		GrpcServerStatsMonitor stats = (GrpcServerStatsMonitor) serverStats;
		getGrpcServerStats(stats.getAppName(), stats.getServiceName()).recordServerHandled();
	}

	@ProbeAtReturn
	@ProbeSite(clazz = "io.openliberty.grpc.internal.monitor.GrpcServerStatsMonitor", method = "recordMsgReceived")
	public void atGrpcServerMsgReceived(@This Object serverStats) {
		GrpcServerStatsMonitor stats = (GrpcServerStatsMonitor) serverStats;
		getGrpcServerStats(stats.getAppName(), stats.getServiceName()).incrementReceivedMsgCountBy(1);
	}

	@ProbeAtReturn
	@ProbeSite(clazz = "io.openliberty.grpc.internal.monitor.GrpcServerStatsMonitor", method = "recordMsgSent")
	public void atGrpcServerMsgSent(@This Object serverStats) {
		GrpcServerStatsMonitor stats = (GrpcServerStatsMonitor) serverStats;
		getGrpcServerStats(stats.getAppName(), stats.getServiceName()).incrementSentMsgCountBy(1);
	}

	private synchronized GrpcServerStats getGrpcServerStats(String appName, String serviceName) {
		GrpcServerStats stats = grpcServerCountByName.get(serviceName);
		if (stats == null) {
			stats = new GrpcServerStats(appName, serviceName);
			grpcServerCountByName.put(serviceName, stats);
		}
		return stats;
	}

}
