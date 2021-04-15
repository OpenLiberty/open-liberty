/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
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

import com.ibm.ws.kernel.productinfo.ProductInfo;

public class MappingTable {
	
	public static final int METRIC_REGISTRY_TYPE = 0;
	
	public static final int METRIC_NAME = 1;
	public static final int METRIC_DISPLAYNAME = 2;
	public static final int METRIC_DESCRIPTION = 3;
	public static final int METRIC_TYPE = 4;
	public static final int METRIC_UNIT = 5;
	public static final int MBEAN_ATTRIBUTE = 6;
	public static final int MBEAN_SUBATTRIBUTE = 7;
	public static final int MBEAN_STATS_NAME = 8;
	
	public static final int MBEAN_SECOND_ATTRIBUTE = 9; 
	public static final int MBEAN_SECOND_SUBATTRIBUTE = 10; 
	
	public static final String THREADPOOL_TAG_NAME = "pool";
	public static final String SERVLET_TAG_NAME = "servlet";

	public static final String CONNECTIONPOOL_TAG_NAME = "datasource";
	public static final String SESSION_TAG_NAME = "appname";
	public static final String JAXWS_SERVER_TAG_NAME = "endpoint";
	public static final String JAXWS_CLIENT_TAG_NAME = "endpoint";
	public static final String GRPC_SERVER_TAG_NAME = "grpc";
	public static final String GRPC_CLIENT_TAG_NAME = "grpc";
	
	public static final String COUNTER = MetricType.COUNTER.toString().toUpperCase();
	public static final String GAUGE = MetricType.GAUGE.toString().toUpperCase();
	public static final String SIMPLE_TIMER = MetricType.SIMPLE_TIMER.toString().toUpperCase().replaceAll(" ", "_");
	
	private static boolean isBeta = false;

	private static MappingTable singleton = null;

	private Map<String, String[][]> mappingTable = new HashMap<String, String[][]>();

	public static MappingTable getInstance() {
		betaFenceCheck();
		if (singleton == null)
			singleton = new MappingTable();
		return singleton;
	}
	
	
	private MappingTable() {
		
		String[][] requestTimeTable = new String[][] {
			{ "vendor", "requestTiming.requestCount", "Request Count", "requestTiming.requestCount.description", COUNTER, MetricUnits.NONE, "RequestCount", null, null },
			{ "vendor", "requestTiming.activeRequestCount", "Active Request Count", "requestTiming.activeRequestCount.description", GAUGE, MetricUnits.NONE, "ActiveRequestCount", null, null },
			{ "vendor", "requestTiming.slowRequestCount", "Slow Request Count", "requestTiming.slowRequestCount.description", GAUGE, MetricUnits.NONE, "SlowRequestCount", null, null },
			{ "vendor", "requestTiming.hungRequestCount", "Hung Request Count", "requestTiming.hungRequestCount.description", GAUGE, MetricUnits.NONE, "HungRequestCount", null, null }
		};
		if (isBeta) mappingTable.put("WebSphere:type=RequestTimingStats,name=*", requestTimeTable);
		
		String[][] threadPoolTable = new String[][] {
			{ "vendor", "threadpool.activeThreads", "Active Threads", "threadpool.activeThreads.description", GAUGE, MetricUnits.NONE, "ActiveThreads", null, THREADPOOL_TAG_NAME },
			{ "vendor", "threadpool.size", "Thread Pool Size", "threadpool.size.description", GAUGE, MetricUnits.NONE, "PoolSize", null, THREADPOOL_TAG_NAME }
		};
		mappingTable.put("WebSphere:type=ThreadPoolStats,name=*", threadPoolTable);

		String[][] servletTable = new String[][] {
        	{ "vendor", "servlet.request.total", "Total Request", "servlet.request.total.description", COUNTER, MetricUnits.NONE, "RequestCount", null, SERVLET_TAG_NAME },
        	{ "vendor", "servlet.responseTime.total", "Total Response Time", "servlet.responseTime.total.description", GAUGE, MetricUnits.NANOSECONDS, "ResponseTimeDetails", "total", SERVLET_TAG_NAME }
        };
		mappingTable.put("WebSphere:type=ServletStats,name=*", servletTable);
		
		
		String[][] restTable = new String[][] {
			{ "base", "REST.request", "Total Requests and Response Time", "REST.request.description", SIMPLE_TIMER, MetricUnits.NANOSECONDS, "RequestCount", null, null, "ResponseTimeDetails", "total" }
		};

		mappingTable.put("WebSphere:type=REST_Stats,name=*", restTable);
		String[][] connectionPoolTable = new String[][]{
			{ "vendor", "connectionpool.create.total", "Create Count", "connectionpool.create.total.description", COUNTER, MetricUnits.NONE, "CreateCount", null, CONNECTIONPOOL_TAG_NAME },
			{ "vendor", "connectionpool.destroy.total", "Destroy Count", "connectionpool.destroy.total.description", COUNTER, MetricUnits.NONE, "DestroyCount", null, CONNECTIONPOOL_TAG_NAME },
			{ "vendor", "connectionpool.managedConnections", "Managed Connections Count", "connectionpool.managedConnections.description", GAUGE, MetricUnits.NONE, "ManagedConnectionCount", null, CONNECTIONPOOL_TAG_NAME },
			{ "vendor", "connectionpool.connectionHandles", "Connection Handles Count", "connectionpool.connectionHandles.description", GAUGE, MetricUnits.NONE, "ConnectionHandleCount", null, CONNECTIONPOOL_TAG_NAME },
			{ "vendor", "connectionpool.freeConnections", "Free Connections Count", "connectionpool.freeConnections.description", GAUGE, MetricUnits.NONE,  "FreeConnectionCount", null, CONNECTIONPOOL_TAG_NAME },
			{ "vendor", "connectionpool.waitTime.total", "Total Wait Time", "connectionpool.waitTime.total.description", GAUGE, MetricUnits.MILLISECONDS,  "WaitTimeDetails", "total", CONNECTIONPOOL_TAG_NAME },
			{ "vendor", "connectionpool.inUseTime.total", "Total In Use Time", "connectionpool.inUseTime.total.description", GAUGE, MetricUnits.MILLISECONDS,  "InUseTimeDetails", "total", CONNECTIONPOOL_TAG_NAME },
			{ "vendor", "connectionpool.queuedRequests.total", "Queued Connection Request Count", "connectionpool.queuedRequests.total.description", COUNTER, MetricUnits.NONE,  "WaitTimeDetails", "count", CONNECTIONPOOL_TAG_NAME },
			{ "vendor", "connectionpool.usedConnections.total", "Used Connections", "connectionpool.usedConnections.total.description", COUNTER, MetricUnits.NONE,  "InUseTimeDetails", "count", CONNECTIONPOOL_TAG_NAME },
		};
		mappingTable.put("WebSphere:type=ConnectionPoolStats,name=*", connectionPoolTable);
		
		String[][] sessionTable = new String[][]{
        	{ "vendor", "session.create.total", "Total Create Count", "session.create.total.description", GAUGE, MetricUnits.NONE, "CreateCount", null, SESSION_TAG_NAME },
        	{ "vendor", "session.liveSessions", "Live Sessions Count", "session.liveSessions.description", GAUGE, MetricUnits.NONE, "LiveCount", null, SESSION_TAG_NAME },
        	{ "vendor", "session.activeSessions", "Active Sessions Count", "session.activeSessions.description", GAUGE, MetricUnits.NONE, "ActiveCount", null, SESSION_TAG_NAME },
        	{ "vendor", "session.invalidated.total", "Total Invalidated Sessions Count", "session.invalidated.total.description", COUNTER, MetricUnits.NONE, "InvalidatedCount", null, SESSION_TAG_NAME },
        	{ "vendor", "session.invalidatedbyTimeout.total", "Total Invalidated Sessions by Timeout Count", "session.invalidatedbyTimeout.total.description", COUNTER, MetricUnits.NONE, "InvalidatedCountbyTimeout", null, SESSION_TAG_NAME }
		};
		mappingTable.put("WebSphere:type=SessionStats,name=*", sessionTable);
		
		String[][] jaxwsServerTable = new String[][]{
        	{ "vendor", "jaxws.server.invocations.total", "Total Endpoint Invocations Count", "jaxws.invocations.total.description", COUNTER, MetricUnits.NONE, "NumInvocations", null, JAXWS_SERVER_TAG_NAME },
        	{ "vendor", "jaxws.server.checkedApplicationFaults.total", "Total Checked Application Faults Count", "jaxws.checkedApplicationFaults.total.description", COUNTER, MetricUnits.NONE, "NumCheckedApplicationFaults", null, JAXWS_SERVER_TAG_NAME },
        	{ "vendor", "jaxws.server.logicalRuntimeFaults.total", "Total Logical Runtime Faults Count", "jaxws.logicalRuntimeFaults.total.description", COUNTER, MetricUnits.NONE, "NumLogicalRuntimeFaults", null, JAXWS_SERVER_TAG_NAME },
        	{ "vendor", "jaxws.server.runtimeFaults.total", "Total Runtime Faults Count", "jaxws.runtimeFaults.total.description", COUNTER, MetricUnits.NONE, "NumRuntimeFaults", null, JAXWS_SERVER_TAG_NAME },
        	{ "vendor", "jaxws.server.uncheckedApplicationFaults.total ", "Total Unchecked Application Faults Count", "jaxws.uncheckedApplicationFaults.total.description", COUNTER, MetricUnits.NONE, "NumUnCheckedApplicationFaults", null, JAXWS_SERVER_TAG_NAME },
        	{ "vendor", "jaxws.server.responseTime.total", "Total Response Time", "jaxws.responseTime.total.description", GAUGE, MetricUnits.MILLISECONDS, "TotalHandlingTime", null, JAXWS_SERVER_TAG_NAME }
		};
		mappingTable.put("WebSphere:feature=jaxws,*,type=Performance.Counter.Server", jaxwsServerTable);
		
		String[][] jaxwsClientTable = new String[][]{
        	{ "vendor", "jaxws.client.invocations.total", "Total Endpoint Invocations Count", "jaxws.invocations.total.description", COUNTER, MetricUnits.NONE, "NumInvocations", null, JAXWS_CLIENT_TAG_NAME },
        	{ "vendor", "jaxws.client.checkedApplicationFaults.total", "Total Checked Application Faults Count", "jaxws.checkedApplicationFaults.total.description", COUNTER, MetricUnits.NONE, "NumCheckedApplicationFaults", null, JAXWS_CLIENT_TAG_NAME },
        	{ "vendor", "jaxws.client.logicalRuntimeFaults.total", "Total Logical Runtime Faults Count", "jaxws.logicalRuntimeFaults.total.description", COUNTER, MetricUnits.NONE, "NumLogicalRuntimeFaults", null, JAXWS_CLIENT_TAG_NAME },
        	{ "vendor", "jaxws.client.runtimeFaults.total", "Total Runtime Faults Count", "jaxws.runtimeFaults.total.description", COUNTER, MetricUnits.NONE, "NumRuntimeFaults", null, JAXWS_CLIENT_TAG_NAME },
        	{ "vendor", "jaxws.client.uncheckedApplicationFaults.total ", "Total Unchecked Application Faults Count", "jaxws.uncheckedApplicationFaults.total.description", COUNTER, MetricUnits.NONE, "NumUnCheckedApplicationFaults", null, JAXWS_CLIENT_TAG_NAME },
        	{ "vendor", "jaxws.client.responseTime.total", "Total Response Time", "jaxws.responseTime.total.description", GAUGE, MetricUnits.MILLISECONDS, "TotalHandlingTime", null, JAXWS_CLIENT_TAG_NAME }
		};
		mappingTable.put("WebSphere:feature=jaxws,*,type=Performance.Counter.Client", jaxwsClientTable);

		String[][] grpcServerTable = new String[][]{
	    	{ "vendor", "grpc.server.rpcStarted.total", "Total Server RPCs Started Count", "grpc.server.rpcStarted.total.description", COUNTER, MetricUnits.NONE, "RpcStartedCount", null, GRPC_SERVER_TAG_NAME },
	    	{ "vendor", "grpc.server.rpcCompleted.total", "Total Server RPCs Completed Count", "grpc.server.rpcCompleted	.total.description", COUNTER, MetricUnits.NONE, "RpcCompletedCount", null, GRPC_SERVER_TAG_NAME },
        	{ "vendor", "grpc.server.sentMessages.total", "Total Sent Stream Messages", "grpc.server.sentMessages.total.description", COUNTER, MetricUnits.NONE, "SentMessagesCount", null, GRPC_SERVER_TAG_NAME },
        	{ "vendor", "grpc.server.receivedMessages.total", "Total Received Stream Messages", "grpc.server.receivedMessages.total.description", COUNTER, MetricUnits.NONE, "ReceivedMessagesCount", null, GRPC_SERVER_TAG_NAME },
        	{ "vendor", "grpc.server.responseTime.total", "Total Response Time", "grpc.server.responseTime.total.description", GAUGE, MetricUnits.MILLISECONDS, "ResponseTimeDetails", "total", GRPC_SERVER_TAG_NAME }
		};
		mappingTable.put("WebSphere:type=GrpcServerStats,name=*", grpcServerTable);

		String[][] grpcClientTable = new String[][]{
	    	{ "vendor", "grpc.client.rpcStarted.total", "Total Client RPCs Started Count", "grpc.client.rpcStarted.total.description", COUNTER, MetricUnits.NONE, "RpcStartedCount", null, GRPC_CLIENT_TAG_NAME },
	    	{ "vendor", "grpc.client.rpcCompleted.total", "Total Client RPCs Completed Count", "grpc.client.rpcCompleted.total.description", COUNTER, MetricUnits.NONE, "RpcCompletedCount", null, GRPC_CLIENT_TAG_NAME },
        	{ "vendor", "grpc.client.sentMessages.total", "Total Sent Stream Messages", "grpc.client.sentMessages.total.description", COUNTER, MetricUnits.NONE, "SentMessagesCount", null, GRPC_CLIENT_TAG_NAME },
        	{ "vendor", "grpc.client.receivedMessages.total", "Total Received Stream Messages", "grpc.client.receivedMessages.total.description", COUNTER, MetricUnits.NONE, "ReceivedMessagesCount", null, GRPC_CLIENT_TAG_NAME },
        	{ "vendor", "grpc.client.responseTime.total", "Total Response Time", "grpc.client.responseTime.total.description", GAUGE, MetricUnits.MILLISECONDS, "ResponseTimeDetails", "total", GRPC_CLIENT_TAG_NAME }
		};
		mappingTable.put("WebSphere:type=GrpcClientStats,name=*", grpcClientTable);
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

	private static void betaFenceCheck() {
		/*
		 * If this is a BETA, flip  the beta flag to true.
		 */
	    if (ProductInfo.getBetaEdition() && !isBeta) {
	            isBeta = !isBeta;
	    }
	}
}
