/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
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

import com.ibm.ws.microprofile.metrics.impl.CounterImpl;
import com.ibm.ws.microprofile.metrics.impl.SharedMetricRegistries;

public class BaseMetrics {
    private static BaseMetrics baseMetrics = null;
    private static String BASE = MetricRegistry.Type.BASE.getName();
    MBeanServer mbs;
    private static Set<String> gcObjectNames = new HashSet<String>();

    public static synchronized BaseMetrics getInstance() {
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
            if (objectName.contains(BaseMetricConstants.GC_OBJECT_TYPE)) {
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
        MetricRegistry registry = SharedMetricRegistries.getOrCreate(BASE);
        //MEMORY METRICS
        registry.register(new Metadata("memory.usedHeap", "Used Heap Memory", "memory.usedHeap.description", MetricType.GAUGE, MetricUnits.BYTES),
                          new BMGauge<Number>(BaseMetricConstants.MEMORY_OBJECT_TYPE, "HeapMemoryUsage", "used"));

        registry.register(new Metadata("memory.committedHeap", "Committed Heap Memory", "memory.committedHeap.description", MetricType.GAUGE, MetricUnits.BYTES),
                          new BMGauge<Number>(BaseMetricConstants.MEMORY_OBJECT_TYPE, "HeapMemoryUsage", "committed"));

        registry.register(new Metadata("memory.maxHeap", "Max Heap Memory", "memory.maxHeap.description", MetricType.GAUGE, MetricUnits.BYTES),
                          new BMGauge<Number>(BaseMetricConstants.MEMORY_OBJECT_TYPE, "HeapMemoryUsage", "max"));

        //JVM METRICS
        registry.register(new Metadata("jvm.uptime", "JVM Uptime", "jvm.uptime.description", MetricType.GAUGE, MetricUnits.MILLISECONDS),
                          new BMGauge<Number>(BaseMetricConstants.RUNTIME_OBJECT_TYPE, "Uptime"));

        //THREAD JVM -
        registry.register(new Metadata("thread.count", "Thread Count", "thread.count.description", MetricType.COUNTER, MetricUnits.NONE),
                          new BMCounter(BaseMetricConstants.THREAD_OBJECT_TYPE, "ThreadCount"));

        registry.register(new Metadata("thread.daemon.count", "Daemon Thread Count", "thread.daemon.count.description", MetricType.COUNTER, MetricUnits.NONE),
                          new BMCounter(BaseMetricConstants.THREAD_OBJECT_TYPE, "DaemonThreadCount"));

        registry.register(new Metadata("thread.max.count", "Peak Thread Count", "thread.max.count.description", MetricType.COUNTER, MetricUnits.NONE),
                          new BMCounter(BaseMetricConstants.THREAD_OBJECT_TYPE, "PeakThreadCount"));

        //CLASSLOADING METRICS
        registry.register(new Metadata("classloader.currentLoadedClass.count", "Current Loaded Class Count", "classloader.currentLoadedClass.count.description", MetricType.COUNTER, MetricUnits.NONE),
                          new BMCounter(BaseMetricConstants.CLASSLOADING_OBJECT_TYPE, "LoadedClassCount"));

        registry.register(new Metadata("classloader.totalLoadedClass.count", "Total Loaded Class Count", "classloader.totalLoadedClass.count.description", MetricType.COUNTER, MetricUnits.NONE),
                          new BMCounter(BaseMetricConstants.CLASSLOADING_OBJECT_TYPE, "TotalLoadedClassCount"));

        registry.register(new Metadata("classloader.totalUnloadedClass.count", "Total Unloaded Class Count", "classloader.totalUnloadedClass.count.description", MetricType.COUNTER, MetricUnits.NONE),
                          new BMCounter(BaseMetricConstants.CLASSLOADING_OBJECT_TYPE, "UnloadedClassCount"));

        //OPERATING SYSTEM
        registry.register(new Metadata("cpu.availableProcessors", "Available Processors", "cpu.availableProcessors.description", MetricType.GAUGE, MetricUnits.NONE),
                          new BMGauge<Number>(BaseMetricConstants.OS_OBJECT_TYPE, "AvailableProcessors"));

        registry.register(new Metadata("cpu.systemLoadAverage", "System Load Average", "cpu.systemLoadAverage.description", MetricType.GAUGE, MetricUnits.NONE),
                          new BMGauge<Number>(BaseMetricConstants.OS_OBJECT_TYPE, "SystemLoadAverage"));

        registry.register(new Metadata("cpu.processCpuLoad", "Process CPU Load", "cpu.processCpuLoad.description", MetricType.GAUGE, MetricUnits.PERCENT),
                          new BMGauge<Number>(BaseMetricConstants.OS_OBJECT_TYPE, "ProcessCpuLoad"));

        //GARBAGE COLLECTOR METRICS
        for (String gcName : gcObjectNames) {

            String gcNameNoSpace = removeSpaces(gcName);

            //gc.%s.count
            String nameToRegister = "gc." + gcNameNoSpace + ".count";
            registry.register(new Metadata(nameToRegister, "Garbage Collection Count", "garbageCollectionCount.description", MetricType.COUNTER, MetricUnits.NONE),
                              new BMCounter(BaseMetricConstants.GC_OBJECT_TYPE_NAME + gcName, "CollectionCount"));

            //gc.%s.time
            nameToRegister = "gc." + gcNameNoSpace + ".time";
            registry.register(new Metadata(nameToRegister, "Garbage Collection Time", "garbageCollectionTime.description", MetricType.GAUGE, MetricUnits.MILLISECONDS),
                              new BMGauge<Number>(BaseMetricConstants.GC_OBJECT_TYPE_NAME + gcName, "CollectionTime"));
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
