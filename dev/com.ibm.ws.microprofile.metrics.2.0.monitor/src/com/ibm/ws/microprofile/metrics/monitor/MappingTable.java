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
	
	public static final int METRIC_NAME = 0;
	public static final int METRIC_DISPLAYNAME = 1;
	public static final int METRIC_DESCRIPTION = 2;
	public static final int METRIC_TYPE = 3;
	public static final int METRIC_UNIT = 4;
	public static final int MBEAN_ATTRIBUTE = 5;
	public static final int MBEAN_SUBATTRIBUTE = 6;
	public static final int MBEAN_STATS_NAME = 7; // name change pending
	
	public static final String THREADPOOL_TAG_NAME = "pool";
	public static final String SERVLET_TAG_NAME = "servlet";
	public static final String CONNECTIONPOOL_TAG_NAME = "datasource";
	public static final String SESSION_TAG_NAME = "appname";
	public static final String JAXWS_SERVER_TAG_NAME = "endpoint";
	public static final String JAXWS_CLIENT_TAG_NAME = "endpoint";
	
	public static final String COUNTER = MetricType.COUNTER.toString().toUpperCase();
	public static final String GAUGE = MetricType.GAUGE.toString().toUpperCase();
	
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
			{ "requestTiming.requestCount", "Request Count", "requestTiming.requestCount.description", COUNTER, MetricUnits.NONE, "RequestCount", null, null },
			{ "requestTiming.activeRequestCount", "Active Request Count", "requestTiming.activeRequestCount.description", GAUGE, MetricUnits.NONE, "ActiveRequestCount", null, null },
			{ "requestTiming.slowRequestCount", "Slow Request Count", "requestTiming.slowRequestCount.description", GAUGE, MetricUnits.NONE, "SlowRequestCount", null, null },
			{ "requestTiming.hungRequestCount", "Hung Request Count", "requestTiming.hungRequestCount.description", GAUGE, MetricUnits.NONE, "HungRequestCount", null, null }
		};
		if (isBeta) mappingTable.put("WebSphere:type=RequestTimingStats,name=*", requestTimeTable);
		
		String[][] threadPoolTable = new String[][] {
			{ "threadpool.activeThreads", "Active Threads", "threadpool.activeThreads.description", GAUGE, MetricUnits.NONE, "ActiveThreads", null, THREADPOOL_TAG_NAME },
			{ "threadpool.size", "Thread Pool Size", "threadpool.size.description", GAUGE, MetricUnits.NONE, "PoolSize", null, THREADPOOL_TAG_NAME }
		};
		mappingTable.put("WebSphere:type=ThreadPoolStats,name=*", threadPoolTable);

		String[][] servletTable = new String[][] {
        	{ "servlet.request.total", "Total Request", "servlet.request.total.description", COUNTER, MetricUnits.NONE, "RequestCount", null, SERVLET_TAG_NAME },
        	{ "servlet.responseTime.total", "Total Response Time", "servlet.responseTime.total.description", GAUGE, MetricUnits.NANOSECONDS, "ResponseTimeDetails", "total", SERVLET_TAG_NAME }
        };
		mappingTable.put("WebSphere:type=ServletStats,name=*", servletTable);
		
		String[][] connectionPoolTable = new String[][]{
			{ "connectionpool.create.total", "Create Count", "connectionpool.create.total.description", COUNTER, MetricUnits.NONE, "CreateCount", null, CONNECTIONPOOL_TAG_NAME },
			{ "connectionpool.destroy.total", "Destroy Count", "connectionpool.destroy.total.description", COUNTER, MetricUnits.NONE, "DestroyCount", null, CONNECTIONPOOL_TAG_NAME },
			{ "connectionpool.managedConnections", "Managed Connections Count", "connectionpool.managedConnections.description", GAUGE, MetricUnits.NONE, "ManagedConnectionCount", null, CONNECTIONPOOL_TAG_NAME },
			{ "connectionpool.connectionHandles", "Connection Handles Count", "connectionpool.connectionHandles.description", GAUGE, MetricUnits.NONE, "ConnectionHandleCount", null, CONNECTIONPOOL_TAG_NAME },
			{ "connectionpool.freeConnections", "Free Connections Count", "connectionpool.freeConnections.description", GAUGE, MetricUnits.NONE,  "FreeConnectionCount", null, CONNECTIONPOOL_TAG_NAME },
			{ "connectionpool.waitTime.total", "Total Wait Time", "connectionpool.waitTime.total.description", GAUGE, MetricUnits.MILLISECONDS,  "WaitTimeDetails", "total", CONNECTIONPOOL_TAG_NAME },
			{ "connectionpool.inUseTime.total", "Total In Use Time", "connectionpool.inUseTime.total.description", GAUGE, MetricUnits.MILLISECONDS,  "InUseTimeDetails", "total", CONNECTIONPOOL_TAG_NAME },
			{ "connectionpool.queuedRequests.total", "Queued Connection Request Count", "connectionpool.queuedRequests.total.description", COUNTER, MetricUnits.NONE,  "WaitTimeDetails", "count", CONNECTIONPOOL_TAG_NAME },
			{ "connectionpool.usedConnections.total", "Used Connections", "connectionpool.usedConnections.total.description", COUNTER, MetricUnits.NONE,  "InUseTimeDetails", "count", CONNECTIONPOOL_TAG_NAME },
		};
		mappingTable.put("WebSphere:type=ConnectionPoolStats,name=*", connectionPoolTable);
		
		String[][] sessionTable = new String[][]{
        	{ "session.create.total", "Total Create Count", "session.create.total.description", GAUGE, MetricUnits.NONE, "CreateCount", null, SESSION_TAG_NAME },
        	{ "session.liveSessions", "Live Sessions Count", "session.liveSessions.description", GAUGE, MetricUnits.NONE, "LiveCount", null, SESSION_TAG_NAME },
        	{ "session.activeSessions", "Active Sessions Count", "session.activeSessions.description", GAUGE, MetricUnits.NONE, "ActiveCount", null, SESSION_TAG_NAME },
        	{ "session.invalidated.total", "Total Invalidated Sessions Count", "session.invalidated.total.description", COUNTER, MetricUnits.NONE, "InvalidatedCount", null, SESSION_TAG_NAME },
        	{ "session.invalidatedbyTimeout.total", "Total Invalidated Sessions by Timeout Count", "session.invalidatedbyTimeout.total.description", COUNTER, MetricUnits.NONE, "InvalidatedCountbyTimeout", null, SESSION_TAG_NAME }
		};
		mappingTable.put("WebSphere:type=SessionStats,name=*", sessionTable);
		
		String[][] jaxwsServerTable = new String[][]{
        	{ "jaxws.server.invocations.total", "Total Endpoint Invocations Count", "jaxws.invocations.total.description", COUNTER, MetricUnits.NONE, "NumInvocations", null, JAXWS_SERVER_TAG_NAME },
        	{ "jaxws.server.checkedApplicationFaults.total", "Total Checked Application Faults Count", "jaxws.checkedApplicationFaults.total.description", COUNTER, MetricUnits.NONE, "NumCheckedApplicationFaults", null, JAXWS_SERVER_TAG_NAME },
        	{ "jaxws.server.logicalRuntimeFaults.total", "Total Logical Runtime Faults Count", "jaxws.logicalRuntimeFaults.total.description", COUNTER, MetricUnits.NONE, "NumLogicalRuntimeFaults", null, JAXWS_SERVER_TAG_NAME },
        	{ "jaxws.server.runtimeFaults.total", "Total Runtime Faults Count", "jaxws.runtimeFaults.total.description", COUNTER, MetricUnits.NONE, "NumRuntimeFaults", null, JAXWS_SERVER_TAG_NAME },
        	{ "jaxws.server.uncheckedApplicationFaults.total ", "Total Unchecked Application Faults Count", "jaxws.uncheckedApplicationFaults.total.description", COUNTER, MetricUnits.NONE, "NumUnCheckedApplicationFaults", null, JAXWS_SERVER_TAG_NAME },
        	{ "jaxws.server.responseTime.total", "Total Response Time", "jaxws.responseTime.total.description", GAUGE, MetricUnits.MILLISECONDS, "TotalHandlingTime", null, JAXWS_SERVER_TAG_NAME }
		};
		mappingTable.put("WebSphere:feature=jaxws,*,type=Performance.Counter.Server", jaxwsServerTable);
		
		String[][] jaxwsClientTable = new String[][]{
        	{ "jaxws.client.invocations.total", "Total Endpoint Invocations Count", "jaxws.invocations.total.description", COUNTER, MetricUnits.NONE, "NumInvocations", null, JAXWS_CLIENT_TAG_NAME },
        	{ "jaxws.client.checkedApplicationFaults.total", "Total Checked Application Faults Count", "jaxws.checkedApplicationFaults.total.description", COUNTER, MetricUnits.NONE, "NumCheckedApplicationFaults", null, JAXWS_CLIENT_TAG_NAME },
        	{ "jaxws.client.logicalRuntimeFaults.total", "Total Logical Runtime Faults Count", "jaxws.logicalRuntimeFaults.total.description", COUNTER, MetricUnits.NONE, "NumLogicalRuntimeFaults", null, JAXWS_CLIENT_TAG_NAME },
        	{ "jaxws.client.runtimeFaults.total", "Total Runtime Faults Count", "jaxws.runtimeFaults.total.description", COUNTER, MetricUnits.NONE, "NumRuntimeFaults", null, JAXWS_CLIENT_TAG_NAME },
        	{ "jaxws.client.uncheckedApplicationFaults.total ", "Total Unchecked Application Faults Count", "jaxws.uncheckedApplicationFaults.total.description", COUNTER, MetricUnits.NONE, "NumUnCheckedApplicationFaults", null, JAXWS_CLIENT_TAG_NAME },
        	{ "jaxws.client.responseTime.total", "Total Response Time", "jaxws.responseTime.total.description", GAUGE, MetricUnits.MILLISECONDS, "TotalHandlingTime", null, JAXWS_CLIENT_TAG_NAME }
		};
		mappingTable.put("WebSphere:feature=jaxws,*,type=Performance.Counter.Client", jaxwsClientTable);
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
