/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics.monitor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;

public class MappingTable {
	
	public static final int METRIC_NAME = 0;
	public static final int METRIC_DISPLAYNAME = 1;
	public static final int METRIC_DESCRIPTION = 2;
	public static final int METRIC_TYPE = 3;
	public static final int METRIC_UNIT = 4;
	public static final int MBEAN_ATTRIBUTE = 5;
	public static final int MBEAN_SUBATTRIBUTE = 6;
	
	public static final String COUNTER = MetricType.COUNTER.toString().toUpperCase();
	public static final String GAUGE = MetricType.GAUGE.toString().toUpperCase();
	
	private static MappingTable singleton = null;

	private Map<String, String[][]> mappingTable = new HashMap<String, String[][]>();

	public static MappingTable getInstance() {
		if (singleton == null)
			singleton = new MappingTable();
		return singleton;
	}
	
	private MappingTable() {
		String[][] threadPoolTable = new String[][] {
			{ "threadpool.%s.activeThreads", "Active Threads", "threadpool.activeThreads.description", GAUGE, MetricUnits.NONE, "ActiveThreads", null },
			{ "threadpool.%s.size", "Thread Pool Size", "threadpool.size.description", GAUGE, MetricUnits.NONE, "PoolSize", null }
		};
		mappingTable.put("WebSphere:type=ThreadPoolStats,name=*", threadPoolTable);

		String[][] servletTable = new String[][] {
        	{ "servlet.%s.request.total", "Total Request", "servlet.request.total.description", COUNTER, MetricUnits.NONE, "RequestCount", null },
        	{ "servlet.%s.responseTime.total", "Total Response Time", "servlet.responseTime.total.description", GAUGE, MetricUnits.NANOSECONDS, "ResponseTimeDetails", "total" }
        };
		mappingTable.put("WebSphere:type=ServletStats,name=*", servletTable);
		
		String[][] connectionPoolTable = new String[][]{
			{ "connectionpool.%s.create.total", "Create Count", "connectionpool.create.total.description", COUNTER, MetricUnits.NONE, "CreateCount", null },
			{ "connectionpool.%s.destroy.total", "Destroy Count", "connectionpool.destroy.total.description", COUNTER, MetricUnits.NONE, "DestroyCount", null },
			{ "connectionpool.%s.managedConnections", "Managed Connections Count", "connectionpool.managedConnections.description", GAUGE, MetricUnits.NONE, "ManagedConnectionCount", null },
			{ "connectionpool.%s.connectionHandles", "Connection Handles Count", "connectionpool.connectionHandles.description", GAUGE, MetricUnits.NONE, "ConnectionHandleCount", null },
			{ "connectionpool.%s.freeConnections", "Free Connections Count", "connectionpool.freeConnections.description", GAUGE, MetricUnits.NONE,  "FreeConnectionCount", null },
			{ "connectionpool.%s.waitTime.total", "Total Wait Time", "connectionpool.waitTime.total.description", GAUGE, MetricUnits.MILLISECONDS,  "WaitTimeDetails", "total" },
			{ "connectionpool.%s.inUseTime.total", "Total In Use Time", "connectionpool.inUseTime.total.description", GAUGE, MetricUnits.MILLISECONDS,  "InUseTimeDetails", "total" },
			{ "connectionpool.%s.queuedRequests.total", "Queued Connection Request Count", "connectionpool.queuedRequests.total.description", COUNTER, MetricUnits.NONE,  "WaitTimeDetails", "count" },
			{ "connectionpool.%s.usedConnections.total", "Used Connections", "connectionpool.usedConnections.total.description", COUNTER, MetricUnits.NONE,  "InUseTimeDetails", "count" },
		};
		mappingTable.put("WebSphere:type=ConnectionPoolStats,name=*", connectionPoolTable);
		
		String[][] sessionTable = new String[][]{
        	{ "session.%s.create.total", "Total Create Count", "session.create.total.description", GAUGE, MetricUnits.NONE, "CreateCount", null },
        	{ "session.%s.liveSessions", "Live Sessions Count", "session.liveSessions.description", GAUGE, MetricUnits.NONE, "LiveCount", null },
        	{ "session.%s.activeSessions", "Active Sessions Count", "session.activeSessions.description", GAUGE, MetricUnits.NONE, "ActiveCount", null },
        	{ "session.%s.invalidated.total", "Total Invalidated Sessions Count", "session.invalidated.total.description", COUNTER, MetricUnits.NONE, "InvalidatedCount", null },
        	{ "session.%s.invalidatedbyTimeout.total", "Total Invalidated Sessions by Timeout Count", "session.invalidatedbyTimeout.total.description", COUNTER, MetricUnits.NONE, "InvalidatedCountbyTimeout", null }
		};
		mappingTable.put("WebSphere:type=SessionStats,name=*", sessionTable);
		
		String[][] jaxwsServerTable = new String[][]{
        	{ "jaxws.server.%s.invocations.total", "Total Endpoint Invocations Count", "jaxws.invocations.total.description", COUNTER, MetricUnits.NONE, "NumInvocations", null },
        	{ "jaxws.server.%s.checkedApplicationFaults.total", "Total Checked Application Faults Count", "jaxws.checkedApplicationFaults.total.description", COUNTER, MetricUnits.NONE, "NumCheckedApplicationFaults", null },
        	{ "jaxws.server.%s.logicalRuntimeFaults.total", "Total Logical Runtime Faults Count", "jaxws.logicalRuntimeFaults.total.description", COUNTER, MetricUnits.NONE, "NumLogicalRuntimeFaults", null },
        	{ "jaxws.server.%s.runtimeFaults.total", "Total Runtime Faults Count", "jaxws.runtimeFaults.total.description", COUNTER, MetricUnits.NONE, "NumRuntimeFaults", null },
        	{ "jaxws.server.%s.uncheckedApplicationFaults.total ", "Total Unchecked Application Faults Count", "jaxws.uncheckedApplicationFaults.total.description", COUNTER, MetricUnits.NONE, "NumUnCheckedApplicationFaults", null },
        	{ "jaxws.server.%s.responseTime.total", "Total Response Time", "jaxws.responseTime.total.description", GAUGE, MetricUnits.MILLISECONDS, "TotalHandlingTime", null }
		};
		mappingTable.put("WebSphere:feature=jaxws,*,type=Performance.Counter.Server", jaxwsServerTable);
		
		String[][] jaxwsClientTable = new String[][]{
        	{ "jaxws.client.%s.invocations.total", "Total Endpoint Invocations Count", "jaxws.invocations.total.description", COUNTER, MetricUnits.NONE, "NumInvocations", null },
        	{ "jaxws.client.%s.checkedApplicationFaults.total", "Total Checked Application Faults Count", "jaxws.checkedApplicationFaults.total.description", COUNTER, MetricUnits.NONE, "NumCheckedApplicationFaults", null },
        	{ "jaxws.client.%s.logicalRuntimeFaults.total", "Total Logical Runtime Faults Count", "jaxws.logicalRuntimeFaults.total.description", COUNTER, MetricUnits.NONE, "NumLogicalRuntimeFaults", null },
        	{ "jaxws.client.%s.runtimeFaults.total", "Total Runtime Faults Count", "jaxws.runtimeFaults.total.description", COUNTER, MetricUnits.NONE, "NumRuntimeFaults", null },
        	{ "jaxws.client.%s.uncheckedApplicationFaults.total ", "Total Unchecked Application Faults Count", "jaxws.uncheckedApplicationFaults.total.description", COUNTER, MetricUnits.NONE, "NumUnCheckedApplicationFaults", null },
        	{ "jaxws.client.%s.responseTime.total", "Total Response Time", "jaxws.responseTime.total.description", GAUGE, MetricUnits.MILLISECONDS, "TotalHandlingTime", null }
		};
		mappingTable.put("WebSphere:feature=jaxws,*,type=Performance.Counter.Client", jaxwsClientTable);

		String[][] sipContainerBasicCountersTable = new String[][] {
        	{ "sipcontainer.receivedMessages.count", "Incoming traffic", "sipcontainer.receivedMessages.count.description", GAUGE, MetricUnits.NONE, "ReceivedSipMsgs", null },
        	{ "sipcontainer.newSipApplications.count", "New SIP application sessions", "sipcontainer.newSipApplications.count.description", GAUGE, MetricUnits.NONE, "NewSipApplications", null },
        	{ "sipcontainer.requestProcessingTime.total", "Response time", "sipcontainer.requestProcessingTime.total.description", GAUGE, MetricUnits.NONE, "SipRequestProcessing", null },
        	{ "sipcontainer.invokerSize", "Queue size", "sipcontainer.invokerSize.description", GAUGE, MetricUnits.NONE, "InvokerSize", null },
        	{ "sipcontainer.rejectedMessages", "Rejected SIP messages", "sipcontainer.rejectedMessages.description", COUNTER, MetricUnits.NONE, "RejectedMessages", null },       	
        	{ "sipcontainer.sipTimersInvocations.count", "SIP timer invocations", "sipcontainer.sipTimersInvocations.count.description", COUNTER, MetricUnits.NONE, "SipTimersInvocations", null },        	
        	{ "sipcontainer.sipSessions", "Number of active SIP sessions", "sipcontainer.sipSessions.description", GAUGE, MetricUnits.NONE, "SipSessions", null },       	
        	{ "sipcontainer.sipAppSessions", "New SIP application sessions", "sipcontainer.sipAppSessions.description", GAUGE, MetricUnits.NONE, "SipAppSessions", null }
        };
		mappingTable.put("WebSphere:type=SipContainerBasicCounters,name=SipContainer.Basic", sipContainerBasicCountersTable);

		String[][] sipContainerTaskDurationCountersTable = new String[][] {
        	{ "sipTaskDuration.avgOutBoundQueue", 		"Average Task Duration in outbound queue", 	"sipTaskDuration.avgOutBoundQueue.description", 	GAUGE, MetricUnits.NONE, "AvgTaskDurationOutBoundQueue", null },        	
        	{ "sipTaskDuration.maxOutBoundQueue", 		"Maximum Task Duration in outbound queue", 	"sipTaskDuration.maxOutBoundQueue.description", 	GAUGE, MetricUnits.NONE, "MaxTaskDurationOutBoundQueue", null },        	
        	{ "sipTaskDuration.minOutBoundQueue",		"Minimum Task Duration in outbound queue", 	"sipTaskDuration.minOutBoundQueue.description", 	GAUGE, MetricUnits.NONE, "MinTaskDurationOutBoundQueue", null },
        	{ "sipTaskDuration.avgInProcessingQueue",	"Average Task Duration in processing queue","sipTaskDuration.avgInProcessingQueue.description", GAUGE, MetricUnits.NONE, "AvgTaskDurationInProcessingQueue", null },
        	{ "sipTaskDuration.maxInProcessingQueue",	"Maximum Task Duration in processing queue","sipTaskDuration.maxInProcessingQueue.description", GAUGE, MetricUnits.NONE, "MaxTaskDurationInProcessingQueue", null },
        	{ "sipTaskDuration.minInProcessingQueue",	"Minimum Task Duration in processing queue","sipTaskDuration.minInProcessingQueue.description", GAUGE, MetricUnits.NONE, "MinTaskDurationInProcessingQueue", null }
        };
		mappingTable.put("WebSphere:type=TaskDurationCounters,name=SipContainer.TaskDuration", sipContainerTaskDurationCountersTable);

		String[][] sipContainerQueueMonitorCountersTable = new String[][] {
        	{ "sipQueue.totalInProcessing", "Total number of tasks that have flowed through the processing SIP container queue", "sipQueue.totalInProcessing.description", GAUGE, MetricUnits.NONE, "TotalTasksCountInProcessingQueue", null },        	
        	{ "sipQueue.peakInProcessing", "Maximum number of tasks in the processing SIP container queue", "sipQueue.peakInProcessing.description", GAUGE, MetricUnits.NONE, "PeakTasksCountInProcessingQueue", null },
        	{ "sipQueue.minimumInProcessing", "Minimum number of tasks in the processing SIP container queue", "sipQueue.minimumInProcessing.description", GAUGE, MetricUnits.NONE, "MinTasksCountInProcessingQueue", null },
        	{ "sipQueue.percentageFullTasksInProcessing", "Maximum percent full of the processing SIP container queue", "sipQueue.percentageFullTasksInProcessing.description", GAUGE, MetricUnits.NONE, "PercentageFullTasksCountInProcessingQueue", null },
        	{ "sipQueue.totalOutBound", "Total number of tasks that have flowed through the outbound SIP stack queue", "sipQueue.totalOutBound.description", GAUGE, MetricUnits.NONE, "TotalTasksCountInOutboundQueue", null },
        	{ "sipQueue.peakOutBound", "Maximum number of tasks in the outbound SIP stack queue", "sipQueue.peakOutBound.description", GAUGE, MetricUnits.NONE, "PeakTasksCountInOutboundQueue", null },
        	{ "sipQueue.minimumOutBound", "Minimum number of tasks in the outbound SIP stack queue", "sipQueue.minimumOutBound.description", GAUGE, MetricUnits.NONE, "MinTasksCountInOutboundQueue", null },
        	{ "sipQueue.percentageFullTasksOutBound", "Maximum percent full of the outbound SIP stack queue", "sipQueue.percentageFullTasksOutBound.description", GAUGE, MetricUnits.NONE, "PercentageFullTasksCountInOutboundQueue", null }
        };
		mappingTable.put("WebSphere:type=QueueMonitoringModule,name=SipContainer.QueueMonitor", sipContainerQueueMonitorCountersTable);
	}
	
	private String getType(String objectName) {
        for (String subString : objectName.split(",")) {
            subString = subString.trim();
            if (subString.contains("type=")) {
            	return subString.split("=")[1];
            }
        }
		return "notype";
	}
	
	public String[][] getData(String objectName) {
		for (String k : mappingTable.keySet()) {
			if (objectName.contains(getType(k)))
				return mappingTable.get(k);
		}
		return null;
	}
	
	public boolean contains(String objectName) {
		for (String k : mappingTable.keySet()) {
			if (objectName.contains(getType(k)))
				return true;
		}
		return false;
	}

	public Set<String> getKeys() {
		return mappingTable.keySet();
	}
}
