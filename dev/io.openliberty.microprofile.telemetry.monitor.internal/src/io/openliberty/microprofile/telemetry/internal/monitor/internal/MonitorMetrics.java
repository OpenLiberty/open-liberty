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

import java.lang.management.ManagementFactory;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;
import org.eclipse.microprofile.config.ConfigProvider;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

import io.openliberty.microprofile.telemetry.internal.common.constants.OpenTelemetryConstants;
import io.openliberty.microprofile.telemetry.internal.interfaces.OpenTelemetryAccessor;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.DoubleGaugeBuilder;
import io.opentelemetry.api.metrics.LongCounterBuilder;
import io.opentelemetry.api.metrics.LongUpDownCounterBuilder;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableDoubleGauge;
import io.opentelemetry.api.metrics.ObservableLongCounter;
import io.opentelemetry.api.metrics.ObservableLongUpDownCounter;

public class MonitorMetrics {

	private static final TraceComponent tc = Tr.register(MonitorMetrics.class);

	public static final String SCOPE = "io.openliberty.monitor.metrics";

	public static final String OPEN_LIBERTY_NAMESPACE_PREFIX = "io.openliberty.";
	
	protected String objectName;
	protected String mbeanStatsName;
	protected MBeanServer mbs;
	protected Set<AutoCloseable> acInstrumentSet;

	public MonitorMetrics(String objectName) {
		this.mbs = AccessController
				.doPrivileged((PrivilegedAction<MBeanServer>) () -> ManagementFactory.getPlatformMBeanServer());
		this.objectName = objectName;
		acInstrumentSet = new HashSet<AutoCloseable>();
	}

	public String getObjectName() {
		return this.objectName;
	}

	public void createMetrics(String[][] data) {
		/*
		 * Just in case - If beta flag not enabled, do not do anything.
		 */
		if (!ProductInfo.getBetaEdition()) {
			return;
		}
		String appName = getApplicationName();

		/*
		 * Upon initial bundle start up (Which will start with mpTelemetry=2.x and up),
		 * there will be no appName (i.e., component metadata) available. The only stats
		 * available then are server stats (e.g., thread pool component).
		 * 
		 * Once an application is running an MBean is created in under an application
		 * context this bundle will be notified (through Mbean notificatiion) and
		 * proceed to create the metrics under the applicatoin thread (access to
		 * component metadata + appname)
		 * 
		 * Note: If a runtime/global OpenTelemetry is configured, we will be able to
		 * retrieve an OpenTelemetry instance and continue even if under the server
		 * context. Otherwise we will get an null instance an do nothing.
		 */
		OpenTelemetry otelInstance = OpenTelemetryAccessor.getOpenTelemetryInfo(appName).getOpenTelemetry();
		if (otelInstance == null) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, String.format("Unable to resolve an OpenTelemetry instance for the application name [%s]",
						appName));
			}
			return;
		}

		Meter otelMeter = otelInstance.getMeterProvider().get(SCOPE);

		for (String[] metricData : data) {

			String metricName = OPEN_LIBERTY_NAMESPACE_PREFIX + metricData[MappingTable.METRIC_NAME];
			String metricTagName = metricData[MappingTable.MBEAN_STATS_NAME];

			// Build the metric/meter specific attribute (i.e. poolName, servlet, etc)
			AttributesBuilder attributesBuilder = Attributes.builder();
			if (metricTagName != null) {
				attributesBuilder.put(OPEN_LIBERTY_NAMESPACE_PREFIX+ metricTagName, getMBeanStatsString());
			}

			String metricType = metricData[MappingTable.METRIC_TYPE];

			String description = Tr.formatMessage(tc, metricData[MappingTable.METRIC_DESCRIPTION]);
			String unit = metricData[MappingTable.METRIC_UNIT];

			if (MappingTable.LONG_COUNTER.equalsIgnoreCase(metricType)) {
				MonitorCounter mc = metricData[MappingTable.MBEAN_SUBATTRIBUTE] == null
						? new MonitorCounter(mbs, objectName, metricData[MappingTable.MBEAN_ATTRIBUTE])
						: new MonitorCounter(mbs, objectName, metricData[MappingTable.MBEAN_ATTRIBUTE],
								metricData[MappingTable.MBEAN_SUBATTRIBUTE]);

				ObservableLongCounter olc;
				LongCounterBuilder lcb = otelMeter.counterBuilder(metricName).setDescription(description);
				if (unit != null) {
					lcb.setUnit(unit);
				}
				olc = lcb.buildWithCallback(measurment -> measurment.record(mc.getCount(), attributesBuilder.build()));
				acInstrumentSet.add(olc);
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc, "Registered " + olc.toString());
				}
			} else if (MappingTable.LONG_UP_DOWN_COUNTER.equalsIgnoreCase(metricType)) {
				MonitorCounter mc = metricData[MappingTable.MBEAN_SUBATTRIBUTE] == null
						? new MonitorCounter(mbs, objectName, metricData[MappingTable.MBEAN_ATTRIBUTE])
						: new MonitorCounter(mbs, objectName, metricData[MappingTable.MBEAN_ATTRIBUTE],
								metricData[MappingTable.MBEAN_SUBATTRIBUTE]);

				ObservableLongUpDownCounter olc;
				
				LongUpDownCounterBuilder ludcb = otelMeter.upDownCounterBuilder(metricName).setDescription(description);
				if (unit != null) {
					ludcb.setUnit(unit);
				}
				olc = ludcb.buildWithCallback(measurment -> measurment.record(mc.getCount(), attributesBuilder.build()));
				acInstrumentSet.add(olc);
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc, "Registered " + olc.toString());
				}
			} else if (MappingTable.LONG_GAUGE.equalsIgnoreCase(metricType)) {
				MonitorGauge<Number> mg = metricData[MappingTable.MBEAN_SUBATTRIBUTE] == null
						? new MonitorGauge<Number>(mbs, objectName, metricData[MappingTable.MBEAN_ATTRIBUTE])
						: new MonitorGauge<Number>(mbs, objectName, metricData[MappingTable.MBEAN_ATTRIBUTE],
								metricData[MappingTable.MBEAN_SUBATTRIBUTE]);
				ObservableDoubleGauge odg;
				DoubleGaugeBuilder dgb = otelMeter.gaugeBuilder(metricName).setDescription(description);
				if (unit != null) {
					dgb.setUnit(unit);
				}
				odg = dgb.buildWithCallback(
						measurment -> measurment.record(mg.getValue().doubleValue(), attributesBuilder.build()));

				acInstrumentSet.add(odg);
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc, "Registered " + odg.toString());
				}
			} else {
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc, "Failed to register " + metricName + " because of invalid type " + metricType);
				}
			}
		}
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

	public void unregisterMetrics() {
		for (AutoCloseable ac : acInstrumentSet) {
			try {
				ac.close();
			} catch (Exception e) {
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc, String.format("Failed to close %s  due to exception encountered:\n %s", ac, e));
				}
			}
		}
	}

	private String getApplicationName() {
		ComponentMetaData metaData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor()
				.getComponentMetaData();
		if (metaData != null) {
			J2EEName name = metaData.getJ2EEName();
			if (name != null) {
				return name.getApplication();
			}
		}
		return null;
	}

}
