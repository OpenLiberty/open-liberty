/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics.monitor;

import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Set;

import javax.management.MBeanServer;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Tag;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.metrics.impl.SharedMetricRegistries;

public class MonitorMetrics {

	private static final TraceComponent tc = Tr.register(MonitorMetrics.class);

	protected String objectName;
	protected String mbeanStatsName;
	protected MBeanServer mbs;
	protected Set<MetricID> vendorMetricIDs;
	protected Set<MetricID> baseMetricIDs;

	public MonitorMetrics(String objectName) {
		this.mbs = ManagementFactory.getPlatformMBeanServer();
		this.objectName = objectName;
		this.vendorMetricIDs = new HashSet<MetricID>();
		this.baseMetricIDs = new HashSet<MetricID>();
	}
	

	public void createMetrics(SharedMetricRegistries sharedMetricRegistry, String[][] data) {

		MetricRegistry vendorRegistry = sharedMetricRegistry.getOrCreate(MetricRegistry.Type.VENDOR.getName());
		MetricRegistry baseRegistry = sharedMetricRegistry.getOrCreate(MetricRegistry.Type.BASE.getName());
		MetricRegistry metricRegistry = null;
		Set<MetricID> metricIDSet = null;

		for (String[] metricData : data) {

			String metricName = metricData[MappingTable.METRIC_NAME];
			String metricTagName = metricData[MappingTable.MBEAN_STATS_NAME];

			Tag metricTag = null;
			if (metricTagName != null) {
				metricTag = new Tag(metricTagName, getMBeanStatsString());
			}
			MetricID metricID = new MetricID(metricName, metricTag);
			MetricType type = MetricType.valueOf(metricData[MappingTable.METRIC_TYPE]);

			/*
			 * New for the REST metrics (which are registered under base) Will there be
			 * future optional base metrics?
			 */
			metricRegistry = (metricData[MappingTable.METRIC_REGISTRY_TYPE].equalsIgnoreCase("vendor")) ? vendorRegistry
					: baseRegistry;
			metricIDSet = (metricData[MappingTable.METRIC_REGISTRY_TYPE].equalsIgnoreCase("vendor")) ? vendorMetricIDs
					: baseMetricIDs;

			if (MetricType.COUNTER.equals(type)) {
				MonitorCounter mc = metricData[MappingTable.MBEAN_SUBATTRIBUTE] == null
						? new MonitorCounter(mbs, objectName, metricData[MappingTable.MBEAN_ATTRIBUTE])
						: new MonitorCounter(mbs, objectName, metricData[MappingTable.MBEAN_ATTRIBUTE],
								metricData[MappingTable.MBEAN_SUBATTRIBUTE]);

				metricRegistry.register(Metadata.builder().withName(metricName)
						.withDisplayName(metricData[MappingTable.METRIC_DISPLAYNAME])
						.withDescription(metricData[MappingTable.METRIC_DESCRIPTION]).withType(type)
						.withUnit(metricData[MappingTable.METRIC_UNIT]).build(), mc, metricTag);
				metricIDSet.add(metricID);
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
					Tr.debug(tc, "Registered " + metricID.toString());
				}
			} else if (MetricType.GAUGE.equals(type)) {
				MonitorGauge<Number> mg = metricData[MappingTable.MBEAN_SUBATTRIBUTE] == null
						? new MonitorGauge<Number>(mbs, objectName, metricData[MappingTable.MBEAN_ATTRIBUTE])
						: new MonitorGauge<Number>(mbs, objectName, metricData[MappingTable.MBEAN_ATTRIBUTE],
								metricData[MappingTable.MBEAN_SUBATTRIBUTE]);
				metricRegistry.register(Metadata.builder().withName(metricName)
						.withDisplayName(metricData[MappingTable.METRIC_DISPLAYNAME])
						.withDescription(metricData[MappingTable.METRIC_DESCRIPTION]).withType(type)
						.withUnit(metricData[MappingTable.METRIC_UNIT]).build(), mg, metricTag);
				metricIDSet.add(metricID);
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
					Tr.debug(tc, "Registered " + metricID.toString());
				}
			}
			// Only REST_Stats is using SIMPLETIMER at the moment
			else if (MetricType.SIMPLE_TIMER.equals(type)) {

				MonitorSimpleTimer mst = new MonitorSimpleTimer(mbs, objectName,
						metricData[MappingTable.MBEAN_ATTRIBUTE], metricData[MappingTable.MBEAN_SUBATTRIBUTE],
						metricData[MappingTable.MBEAN_SECOND_ATTRIBUTE],
						metricData[MappingTable.MBEAN_SECOND_SUBATTRIBUTE]);
				String[] objName_rest = getRESTMBeanStatsTags();

				// Only REST STAT metric is using SIMPLE TIMER.. explicitly creating the tags
				// necessary.
				String appName = objName_rest[0];
				Tag classTag = new Tag("class", objName_rest[1]);
				Tag methodTag = new Tag("method", objName_rest[2]);

				metricRegistry.register(Metadata.builder().withName(metricName)
						.withDisplayName(metricData[MappingTable.METRIC_DISPLAYNAME])
						.withDescription(metricData[MappingTable.METRIC_DESCRIPTION]).withType(type)
						.withUnit(metricData[MappingTable.METRIC_UNIT]).build(), mst, classTag, methodTag);

				metricID = new MetricID(metricName, classTag, methodTag);
				metricIDSet.add(metricID);
				sharedMetricRegistry.associateMetricIDToApplication(metricID, appName, metricRegistry);
				
				
				//Make sure we register the UnmappedException counter.
				MetricsJaxRsEMCallbackImpl.registerOrRetrieveRESTUnmappedExceptionMetric(objName_rest[1], objName_rest[2]);
				
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
					Tr.debug(tc, "Registered " + metricID.toString());
				}
			} else {
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
					Tr.debug(tc, "Failed to register " + metricName + " because of invalid type " + type);
				}
			}

			// reset
			metricRegistry = null;
		}
	}

	protected String[] getRESTMBeanStatsTags() {
		String[] mbeanNameProperty = new String[3];
		for (String subString : objectName.split(",")) {
			subString = subString.trim();
			
			//Example of expected Mbean property name=ApplicationName/fully.qualified.class.name/methodSignature(java.lang.String)
			if (subString.contains("name=")) {
				mbeanNameProperty = subString.split("/");

				mbeanNameProperty[0] = mbeanNameProperty[0].substring(mbeanNameProperty[0].indexOf("=") + 1,
						mbeanNameProperty[0].length());

				// blank method
				mbeanNameProperty[2] = mbeanNameProperty[2].replaceAll("\\(\\)", "");
				// otherwise first bracket becomes underscores
				mbeanNameProperty[2] = mbeanNameProperty[2].replaceAll("\\(", "_");
				// second bracket is removed
				mbeanNameProperty[2] = mbeanNameProperty[2].replaceAll("\\)", "");

				break;
			}
		}
		return mbeanNameProperty;
	}

	protected String getMBeanStatsString() {
		if (mbeanStatsName == null) {
			String serviceName = null;
			String serviceURL = null;
			String portName = null;
			String mbeanObjName = null;
			StringBuffer sb = new StringBuffer();
			for (String subString : objectName.split(",")) {
				subString = subString.trim();
				if (subString.contains("service=")) {
					serviceName = getMBeanStatsServiceName(subString);
					serviceURL = getMBeanStatsServiceURL(subString);
					continue;
				}
				if (subString.contains("port=")) {
					portName = getMBeanStatsPortName(subString);
					continue;
				}
				if (subString.contains("name=")) {
					mbeanObjName = getMBeanStatsName(subString);
					break;
				}
			}
			if (serviceURL != null && serviceName != null && portName != null) {
				sb.append(serviceURL);
				sb.append(".");
				sb.append(serviceName);
				sb.append(".");
				sb.append(portName);
			} else if (mbeanObjName != null) {
				sb.append(mbeanObjName);
			} else {
				sb.append("unknown");
			}

			mbeanStatsName = sb.toString();
		}
		return mbeanStatsName;
	}

	private String getMBeanStatsName(String nameStr) {
		String mbeanName = nameStr.split("=")[1];
		mbeanName = mbeanName.replaceAll(" ", "_");
		mbeanName = mbeanName.replaceAll("/", "_");
		mbeanName = mbeanName.replaceAll("[^a-zA-Z0-9_]", "_");
		return mbeanName;
	}

	private String getMBeanStatsServiceName(String serviceStr) {
		serviceStr = serviceStr.split("=")[1];
		serviceStr = serviceStr.replaceAll("\"", "");
		String serviceName = serviceStr.substring(serviceStr.indexOf("}") + 1);
		return serviceName;
	}

	private String getMBeanStatsServiceURL(String serviceStr) {
		serviceStr = serviceStr.split("=")[1];
		serviceStr = serviceStr.replaceAll("\"", "");
		String serviceURL = serviceStr.substring(serviceStr.indexOf("{") + 1, serviceStr.indexOf("}"));
		serviceURL = serviceURL.replace("http://", "").replace("https://", "").replace("/", ".");
		return serviceURL;
	}

	private String getMBeanStatsPortName(String portStr) {
		portStr = portStr.split("=")[1];
		String portName = portStr.replaceAll("\"", "");
		return portName;
	}

	public void unregisterMetrics(SharedMetricRegistries sharedMetricRegistry) {
		MetricRegistry vendorRegistry = sharedMetricRegistry.getOrCreate(MetricRegistry.Type.VENDOR.getName());
		MetricRegistry baseRegistry = sharedMetricRegistry.getOrCreate(MetricRegistry.Type.BASE.getName());

		for (MetricID metricID : vendorMetricIDs) {
			boolean rc = vendorRegistry.remove(metricID);
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "Unregistered " + metricID.toString() + " " + (rc ? "successfully" : "unsuccessfully"));
			}
		}

		for (MetricID metricID : baseMetricIDs) {
			boolean rc = baseRegistry.remove(metricID);
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "Unregistered " + metricID.toString() + " " + (rc ? "successfully" : "unsuccessfully"));
			}
		}
		vendorMetricIDs.clear();
		baseMetricIDs.clear();
	}
}
