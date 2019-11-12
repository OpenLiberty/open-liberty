/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics;

import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Tag;

import com.ibm.ws.microprofile.metrics.impl.CounterImpl;
import com.ibm.ws.microprofile.metrics.impl.SharedMetricRegistries;

public class BaseMetrics {

    protected static BaseMetrics baseMetrics = null;
    protected static String BASE = MetricRegistry.Type.BASE.getName();
    public MBeanServer mbs;
    protected static Set<String> gcObjectNames = new HashSet<String>();

    protected static SharedMetricRegistries SHARED_METRIC_REGISTRY;

    public static synchronized BaseMetrics getInstance(SharedMetricRegistries sharedMetricRegistry) {
        SHARED_METRIC_REGISTRY = sharedMetricRegistry;
        if (baseMetrics == null)
            baseMetrics = new BaseMetrics();
        return baseMetrics;
    }

    protected BaseMetrics() {
        mbs = ManagementFactory.getPlatformMBeanServer();
        Set<ObjectInstance> mBeanObjectInstanceSet = mbs.queryMBeans(null, null);
        populateGcNames(mBeanObjectInstanceSet);
        createBaseMetrics();
    }

    private void populateGcNames(Set<ObjectInstance> mBeanObjectInstanceSet) {
        for (ObjectInstance objInstance : mBeanObjectInstanceSet) {
            String objectName = objInstance.getObjectName().toString();
            if (objectName.matches(BaseMetricConstants.GC_OBJECT_TYPE)) {
                for (String subString : objectName.split(",")) {
                    subString = subString.trim();
                    if (subString.contains("name=")) {
                        String name = subString.split("=")[1];
                        gcObjectNames.add(name);
                    }
                }
            }
        }
    }

    public void createBaseMetrics() {
        MetricRegistry registry = SHARED_METRIC_REGISTRY.getOrCreate(BASE);
        //MEMORY METRICS
        registry.register(Metadata.builder().withName("memory.usedHeap").withDisplayName("Used Heap Memory").withDescription("memory.usedHeap.description").withType(MetricType.GAUGE).withUnit(MetricUnits.BYTES).build(),
                          new BMGauge<Number>(BaseMetricConstants.MEMORY_OBJECT_TYPE, "HeapMemoryUsage", "used"));

        registry.register(Metadata.builder().withName("memory.committedHeap").withDisplayName("Committed Heap Memory").withDescription("memory.committedHeap.description").withType(MetricType.GAUGE).withUnit(MetricUnits.BYTES).build(),
                          new BMGauge<Number>(BaseMetricConstants.MEMORY_OBJECT_TYPE, "HeapMemoryUsage", "committed"));

        registry.register(Metadata.builder().withName("memory.maxHeap").withDisplayName("Max Heap Memory").withDescription("memory.maxHeap.description").withType(MetricType.GAUGE).withUnit(MetricUnits.BYTES).build(),
                          new BMGauge<Number>(BaseMetricConstants.MEMORY_OBJECT_TYPE, "HeapMemoryUsage", "max"));

        //JVM METRICS
        registry.register(Metadata.builder().withName("jvm.uptime").withDisplayName("JVM Uptime").withDescription("jvm.uptime.description").withType(MetricType.GAUGE).withUnit(MetricUnits.MILLISECONDS).build(),
                          new BMGauge<Number>(BaseMetricConstants.RUNTIME_OBJECT_TYPE, "Uptime"));

        //THREAD JVM
        //turnGauge
        registry.register(Metadata.builder().withName("thread.count").withDisplayName("Thread Count").withDescription("thread.count.description").withType(MetricType.GAUGE).withUnit(MetricUnits.NONE).build(),
                          new BMGauge<Number>(BaseMetricConstants.THREAD_OBJECT_TYPE, "ThreadCount"));

        //turnGauge
        registry.register(Metadata.builder().withName("thread.daemon.count").withDisplayName("Daemon Thread Count").withDescription("thread.daemon.count.description").withType(MetricType.GAUGE).withUnit(MetricUnits.NONE).build(),
                          new BMGauge<Number>(BaseMetricConstants.THREAD_OBJECT_TYPE, "DaemonThreadCount"));
        //turnGauge
        registry.register(Metadata.builder().withName("thread.max.count").withDisplayName("Peak Thread Count").withDescription("thread.max.count.description").withType(MetricType.GAUGE).withUnit(MetricUnits.NONE).build(),
                          new BMGauge<Number>(BaseMetricConstants.THREAD_OBJECT_TYPE, "PeakThreadCount"));

        //CLASSLOADING METRICS
        //turnGauge
        registry.register(Metadata.builder().withName("classloader.loadedClasses.count").withDisplayName("Current Loaded Class Count").withDescription("classloader.currentLoadedClass.count.description").withType(MetricType.GAUGE).withUnit(MetricUnits.NONE).build(),
                          new BMGauge<Number>(BaseMetricConstants.CLASSLOADING_OBJECT_TYPE, "LoadedClassCount"));

        registry.register(Metadata.builder().withName("classloader.loadedClasses.total").withDisplayName("Total Loaded Class Count").withDescription("classloader.totalLoadedClass.count.description").withType(MetricType.COUNTER).withUnit(MetricUnits.NONE).build(),
                          new BMCounter(BaseMetricConstants.CLASSLOADING_OBJECT_TYPE, "TotalLoadedClassCount"));

        registry.register(Metadata.builder().withName("classloader.unloadedClasses.total").withDisplayName("Total Unloaded Class Count").withDescription("classloader.totalUnloadedClass.count.description").withType(MetricType.COUNTER).withUnit(MetricUnits.NONE).build(),
                          new BMCounter(BaseMetricConstants.CLASSLOADING_OBJECT_TYPE, "UnloadedClassCount"));

        //OPERATING SYSTEM
        registry.register(Metadata.builder().withName("cpu.availableProcessors").withDisplayName("Available Processors").withDescription("cpu.availableProcessors.description").withType(MetricType.GAUGE).withUnit(MetricUnits.NONE).build(),
                          new BMGauge<Number>(BaseMetricConstants.OS_OBJECT_TYPE, "AvailableProcessors"));

        registry.register(Metadata.builder().withName("cpu.systemLoadAverage").withDisplayName("System Load Average").withDescription("cpu.systemLoadAverage.description").withType(MetricType.GAUGE).withUnit(MetricUnits.NONE).build(),
                          new BMGauge<Number>(BaseMetricConstants.OS_OBJECT_TYPE, "SystemLoadAverage"));

        registry.register(Metadata.builder().withName("cpu.processCpuLoad").withDisplayName("Process CPU Load").withDescription("cpu.processCpuLoad.description").withType(MetricType.GAUGE).withUnit(MetricUnits.PERCENT).build(),
                          new BMGauge<Number>(BaseMetricConstants.OS_OBJECT_TYPE, "ProcessCpuLoad"));

        // Leaving out until new spec is released
//        registry.register(Metadata.builder().withName("cpu.processCpuTime").withDisplayName("Process CPU Time").withDescription("cpu.processCpuTime.description").withType(MetricType.GAUGE).withUnit(MetricUnits.NANOSECONDS).build(),
//                          new BMGauge<Number>(BaseMetricConstants.OS_OBJECT_TYPE, "ProcessCpuTime"));

        //GARBAGE COLLECTOR METRICS
        for (String gcName : gcObjectNames) {

            String gcNameNoSpace = removeSpaces(gcName);
            Tag gcNameNoSpaceTag = new Tag("name", gcNameNoSpace);
            String nameToRegister = "gc.total";
            registry.register(Metadata.builder().withName(nameToRegister).withDisplayName("Garbage Collection Count").withDescription("garbageCollectionCount.description").withType(MetricType.COUNTER).withUnit(MetricUnits.NONE).build(),
                              new BMCounter(BaseMetricConstants.GC_OBJECT_TYPE_NAME + gcName, "CollectionCount"), gcNameNoSpaceTag);

            nameToRegister = "gc.time";
            registry.register(Metadata.builder().withName(nameToRegister).withDisplayName("Garbage Collection Time").withDescription("garbageCollectionTime.description").withType(MetricType.GAUGE).withUnit(MetricUnits.MILLISECONDS).build(),
                              new BMGauge<Number>(BaseMetricConstants.GC_OBJECT_TYPE_NAME + gcName, "CollectionTime"), gcNameNoSpaceTag);
        }

    }

    private String removeSpaces(String aString) {
        return aString.replaceAll("\\s+", "");
    }

    private class BMGauge<T> implements Gauge<T> {
        String objectName, attribute, subAttribute;
        boolean isComposite = false;

        public BMGauge(String objectName, String attribute) {
            this.objectName = objectName;
            this.attribute = attribute;
        }

        public BMGauge(String objectName, String attribute, String subAttribute) {
            this.objectName = objectName;
            this.attribute = attribute;
            this.subAttribute = subAttribute;
            isComposite = true;
        }

        @Override
        public T getValue() {
            try {
                if (isComposite) {
                    CompositeData value = (CompositeData) mbs.getAttribute(new ObjectName(objectName), attribute);
                    return (T) value.get(subAttribute);
                } else {
                    T value = (T) mbs.getAttribute(new ObjectName(objectName), attribute);
                    return value;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private class BMCounter extends CounterImpl {
        String objectName, attribute;

        public BMCounter(String objectName, String attribute) {
            this.objectName = objectName;
            this.attribute = attribute;
        }

        @Override
        public long getCount() {
            try {
                Number value = (Number) mbs.getAttribute(new ObjectName(objectName), attribute);
                return value.longValue();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        }

    }

}