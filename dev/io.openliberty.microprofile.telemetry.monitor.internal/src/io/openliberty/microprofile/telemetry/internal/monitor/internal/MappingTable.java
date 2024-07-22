/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry.internal.monitor.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MappingTable {

	public static final int METRIC_NAME = 0;
	public static final int METRIC_DESCRIPTION = 1;
	public static final int METRIC_TYPE = 2;
	public static final int METRIC_UNIT = 3;
	public static final int MBEAN_ATTRIBUTE = 4;
	public static final int MBEAN_SUBATTRIBUTE = 5;
	public static final int MBEAN_STATS_NAME = 6;

	public static final int MBEAN_SECOND_ATTRIBUTE = 7;
	public static final int MBEAN_SECOND_SUBATTRIBUTE = 8;

	public static final String THREADPOOL_TAG_NAME = "pool";

	public static final String SESSION_TAG_NAME = "appname";

	public static final String LONG_UP_DOWN_COUNTER = "LONGUPDOWNCOUNTER";
	public static final String LONG_COUNTER = "LONGCOUNTER";
	public static final String LONG_GAUGE = "LONGGAUGE";
	


	private static MappingTable singleton = null;

	private Map<String, String[][]> mappingTable = new HashMap<String, String[][]>();

	public static MappingTable getInstance() {
		if (singleton == null)
			singleton = new MappingTable();
		return singleton;
	}

	/*
	 * In MonitorMetrics, the metric names and atributes will be pre-pended with "io.openliberty."
	 */
	private MappingTable() {

		String[][] requestTimeTable = new String[][] {
				{ "request_timing.requests", "requestTiming.requestCount.description", LONG_COUNTER, null,
						"RequestCount", null, null },
				{ "request_timing.active_request.count", "requestTiming.activeRequestCount.description",
						LONG_UP_DOWN_COUNTER, null, "ActiveRequestCount", null, null },
				{ "request_timing.slow_request.count", "requestTiming.slowRequestCount.description",
						LONG_UP_DOWN_COUNTER, null, "SlowRequestCount", null, null },
				{ "request_timing.hung_request.count", "requestTiming.hungRequestCount.description",
						LONG_UP_DOWN_COUNTER, null, "HungRequestCount", null, null } };
		mappingTable.put("WebSphere:type=RequestTimingStats,name=*", requestTimeTable);

		String[][] threadPoolTable = new String[][] {
				{ "threadpool.active_thread.count", "threadpool.activeThreads.description", LONG_UP_DOWN_COUNTER, null,
						"ActiveThreads", null, THREADPOOL_TAG_NAME },
				{ "threadpool.size", "threadpool.size.description", LONG_GAUGE, null, "PoolSize", null,
						THREADPOOL_TAG_NAME } };
		mappingTable.put("WebSphere:type=ThreadPoolStats,name=*", threadPoolTable);

		String[][] sessionTable = new String[][] {
				{ "session.creates", "session.create.total.description", LONG_COUNTER, null, "CreateCount", null,
						SESSION_TAG_NAME },
				{ "session.live_session.count", "session.liveSessions.description", LONG_UP_DOWN_COUNTER, null,
						"LiveCount", null, SESSION_TAG_NAME },
				{ "session.active_session.count", "session.activeSessions.description", LONG_UP_DOWN_COUNTER, null,
						"ActiveCount", null, SESSION_TAG_NAME },
				{ "session.invalidated", "session.invalidated.total.description", LONG_COUNTER, null,
						"InvalidatedCount", null, SESSION_TAG_NAME },
				{ "session.invalidated_by_timeout", "session.invalidatedbyTimeout.total.description", LONG_COUNTER,
						null, "InvalidatedCountbyTimeout", null, SESSION_TAG_NAME } };
		mappingTable.put("WebSphere:type=SessionStats,name=*", sessionTable);

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
