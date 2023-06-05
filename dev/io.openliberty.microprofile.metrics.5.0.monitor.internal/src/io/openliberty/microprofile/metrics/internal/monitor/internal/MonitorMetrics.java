/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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
package io.openliberty.microprofile.metrics.internal.monitor.internal;

import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Tag;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import io.openliberty.microprofile.metrics50.helper.Constants;

import io.openliberty.microprofile.metrics50.helper.Util;
import io.openliberty.microprofile.metrics50.SharedMetricRegistries;
import io.openliberty.smallrye.metrics.adapters.SRMetricRegistryAdapter;

public class MonitorMetrics {

    private static final TraceComponent tc = Tr.register(MonitorMetrics.class);

    protected String objectName;
    protected String mbeanStatsName;
    protected MBeanServer mbs;
    protected Set<MetricID> vendorMetricIDs;
    protected Set<MetricID> baseMetricIDs;

    public MonitorMetrics(String objectName) {
        this.mbs = AccessController
                .doPrivileged((PrivilegedAction<MBeanServer>) () -> ManagementFactory.getPlatformMBeanServer());
        this.objectName = objectName;
        this.vendorMetricIDs = new HashSet<MetricID>();
        this.baseMetricIDs = new HashSet<MetricID>();
    }

    public String getObjectName() {
        return this.objectName;
    }

    public void createMetrics(SharedMetricRegistries sharedMetricRegistry, String[][] data) {
        MetricRegistry metricRegistry = sharedMetricRegistry.getOrCreate(MetricRegistry.VENDOR_SCOPE);
        
        /*
         * metricRegistry is null due to failed initialization of the MP Metrics runtime.
         * Fail silently.
         */
        if (metricRegistry == null) {
            if (tc.isDebugEnabled() || tc.isAnyTracingEnabled()) {
                Tr.debug(tc, "MetricRegistry obtained from SharedMetricRegistries was null. No metrics will be registered.");
            }
            return;
        }
        
        Set<MetricID> metricIDSet = null;

        for (String[] metricData : data) {

            String metricName = metricData[MappingTable.METRIC_NAME];
            String metricTagName = metricData[MappingTable.MBEAN_STATS_NAME];

            Tag metricTag = null;
            if (metricTagName != null) {
                metricTag = new Tag(metricTagName, getMBeanStatsString());
            }
            MetricID metricID = new MetricID(metricName, metricTag);
            String metricType = metricData[MappingTable.METRIC_TYPE];

            metricIDSet = vendorMetricIDs;

            // Resolve the resource bundle metric description
            String description = Tr.formatMessage(tc, metricData[MappingTable.METRIC_DESCRIPTION]);
            String unit = metricData[MappingTable.METRIC_UNIT];

            Map.Entry<String, Double> conversionMap = resolveBaseUnitAndConversionFactor(unit);
            final double conversionFactor = conversionMap.getValue();
            unit = conversionMap.getKey();

            Metadata metadata = Metadata.builder().withName(metricName).withDescription(description).withUnit(unit)
                    .build();

            if ("COUNTER".equalsIgnoreCase(metricType)) {
                MonitorCounter mc = metricData[MappingTable.MBEAN_SUBATTRIBUTE] == null
                        ? new MonitorCounter(mbs, objectName, metricData[MappingTable.MBEAN_ATTRIBUTE])
                        : new MonitorCounter(mbs, objectName, metricData[MappingTable.MBEAN_ATTRIBUTE],
                                metricData[MappingTable.MBEAN_SUBATTRIBUTE]);

                if (Util.SR_LEGACY_METRIC_REGISTRY_CLASS.isInstance(metricRegistry)) {
                    try {
                        Object cast = Util.SR_LEGACY_METRIC_REGISTRY_CLASS.cast(metricRegistry);

                        SRMetricRegistryAdapter srMetricRegistry = new SRMetricRegistryAdapter(cast);

                        if (metricTag == null) {
                            srMetricRegistry.functionCounter(metadata, mc, x -> (x.getCount() * conversionFactor));
                        } else {
                            srMetricRegistry.functionCounter(metadata, mc, x -> (x.getCount() * conversionFactor),
                                    metricTag);
                        }

                    } catch (ClassCastException e) {
                        // This should never actually happen.
                        Tr.debug(tc, "Incompatible Metric Registries. Coud not cast " + metricRegistry + " to "
                                + Util.SR_LEGACY_METRIC_REGISTRY_CLASS);
                    }

                }

                metricIDSet.add(metricID);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Registered " + metricID.toString());
                }
            } else if ("GAUGE".equalsIgnoreCase(metricType)) {
                MonitorGauge<Number> mg = metricData[MappingTable.MBEAN_SUBATTRIBUTE] == null
                        ? new MonitorGauge<Number>(mbs, objectName, metricData[MappingTable.MBEAN_ATTRIBUTE])
                        : new MonitorGauge<Number>(mbs, objectName, metricData[MappingTable.MBEAN_ATTRIBUTE],
                                metricData[MappingTable.MBEAN_SUBATTRIBUTE]);
                if (metricTag == null) {
                    metricRegistry.gauge(metadata, mg, x -> (x.getValue().doubleValue() * conversionFactor));
                } else {
                    metricRegistry.gauge(metadata, mg, x -> (x.getValue().doubleValue() * conversionFactor), metricTag);
                }

                metricIDSet.add(metricID);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Registered " + metricID.toString());
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Failed to register " + metricName + " because of invalid type " + metricType);
                }
            }
        }
    }

    protected String[] getRESTMBeanStatsTags() {
        String[] mbeanNameProperty = new String[3];
        for (String subString : objectName.split(",")) {
            subString = subString.trim();

            // Example of expected Mbean property
            // name=ApplicationName/fully.qualified.class.name/methodSignature(java.lang.String)
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
        MetricRegistry vendorRegistry = sharedMetricRegistry.getOrCreate(MetricRegistry.VENDOR_SCOPE);

        for (MetricID metricID : vendorMetricIDs) {
            boolean rc = vendorRegistry.remove(metricID);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unregistered " + metricID.toString() + " " + (rc ? "successfully" : "unsuccessfully"));
            }
        }
        vendorMetricIDs.clear();
    }

    protected Map.Entry<String, Double> resolveBaseUnitAndConversionFactor(String unit) {

        if (unit == null || unit.trim().isEmpty() || unit.equals(MetricUnits.NONE)) {
            return new AbstractMap.SimpleEntry<String, Double>(unit, 1.0);

        } else if (unit.equals(MetricUnits.NANOSECONDS)) {
            return new AbstractMap.SimpleEntry<String, Double>(MetricUnits.SECONDS, Constants.NANOSECONDCONVERSION);

        } else if (unit.equals(MetricUnits.MICROSECONDS)) {
            return new AbstractMap.SimpleEntry<String, Double>(MetricUnits.SECONDS, Constants.MICROSECONDCONVERSION);
        } else if (unit.equals(MetricUnits.SECONDS)) {
            return new AbstractMap.SimpleEntry<String, Double>(MetricUnits.SECONDS, Constants.SECONDCONVERSION);

        } else if (unit.equals(MetricUnits.MINUTES)) {
            return new AbstractMap.SimpleEntry<String, Double>(MetricUnits.SECONDS, Constants.MINUTECONVERSION);

        } else if (unit.equals(MetricUnits.HOURS)) {
            return new AbstractMap.SimpleEntry<String, Double>(MetricUnits.SECONDS, Constants.HOURCONVERSION);

        } else if (unit.equals(MetricUnits.DAYS)) {
            return new AbstractMap.SimpleEntry<String, Double>(MetricUnits.SECONDS, Constants.DAYCONVERSION);

        } else if (unit.equals(MetricUnits.PERCENT)) {
            return new AbstractMap.SimpleEntry<String, Double>(Constants.APPENDEDPERCENT, Double.NaN);

        } else if (unit.equals(MetricUnits.BYTES)) {
            return new AbstractMap.SimpleEntry<String, Double>(MetricUnits.BYTES, Constants.BYTECONVERSION);

        } else if (unit.equals(MetricUnits.KILOBYTES)) {
            return new AbstractMap.SimpleEntry<String, Double>(MetricUnits.BYTES, Constants.KILOBYTECONVERSION);

        } else if (unit.equals(MetricUnits.MEGABYTES)) {
            return new AbstractMap.SimpleEntry<String, Double>(MetricUnits.BYTES, Constants.MEGABYTECONVERSION);

        } else if (unit.equals(MetricUnits.GIGABYTES)) {
            return new AbstractMap.SimpleEntry<String, Double>(MetricUnits.BYTES, Constants.GIGABYTECONVERSION);

        } else if (unit.equals(MetricUnits.KILOBITS)) {
            return new AbstractMap.SimpleEntry<String, Double>(MetricUnits.BYTES, Constants.KILOBITCONVERSION);

        } else if (unit.equals(MetricUnits.MEGABITS)) {
            return new AbstractMap.SimpleEntry<String, Double>(MetricUnits.BYTES, Constants.MEGABITCONVERSION);
        } else if (unit.equals(MetricUnits.GIGABITS)) {
            return new AbstractMap.SimpleEntry<String, Double>(MetricUnits.BYTES, Constants.GIGABITCONVERSION);

        } else if (unit.equals(MetricUnits.KIBIBITS)) {
            return new AbstractMap.SimpleEntry<String, Double>(MetricUnits.BYTES, Constants.KIBIBITCONVERSION);
        } else if (unit.equals(MetricUnits.MEBIBITS)) {
            return new AbstractMap.SimpleEntry<String, Double>(MetricUnits.BYTES, Constants.MEBIBITCONVERSION);
        } else if (unit.equals(MetricUnits.GIBIBITS)) {
            return new AbstractMap.SimpleEntry<String, Double>(MetricUnits.BYTES, Constants.GIBIBITCONVERSION);
        } else if (unit.equals(MetricUnits.MILLISECONDS)) {
            return new AbstractMap.SimpleEntry<String, Double>(MetricUnits.SECONDS, Constants.MILLISECONDCONVERSION);
        } else {
            return null;
        }
    }
}
