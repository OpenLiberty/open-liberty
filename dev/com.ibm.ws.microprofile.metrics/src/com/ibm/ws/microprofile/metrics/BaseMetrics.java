/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
    private static String BASE = "base";
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
        registry.register("memory.usedHeap", new BMGauge<Number>(BaseMetricConstants.MEMORY_OBJECT_TYPE, "HeapMemoryUsage", "used"),
                          new Metadata("memory.usedHeap", "Used Heap Memory", "Displays the amount of used heap memory in bytes.", MetricType.GAUGE, MetricUnits.BYTES));

        registry.register("memory.committedHeap", new BMGauge<Number>(BaseMetricConstants.MEMORY_OBJECT_TYPE, "HeapMemoryUsage", "committed"),
                          new Metadata("memory.committedHeap", "Committed Heap Memory", "Displays the amount of memory in bytes that is committed for the Java virtual machine "
                                                                                        + "to use. This amount of memory is guaranteed for the Java virtual machine to use.", MetricType.GAUGE, MetricUnits.BYTES));

        registry.register("memory.maxHeap", new BMGauge<Number>(BaseMetricConstants.MEMORY_OBJECT_TYPE, "HeapMemoryUsage", "max"),
                          new Metadata("memory.maxHeap", "Max Heap Memory", "Displays the maximum amount of heap memory in bytes that can be "
                                                                            + "used for memory management. This attribute displays -1 if the maximum heap memory size is undefined. "
                                                                            + "This amount of memory is not guaranteed to be available for memory management if it is greater "
                                                                            + "than the amount of committed memory. The Java virtual machine may fail to allocate memory even "
                                                                            + "if the amount of used memory does not exceed this maximum size.", MetricType.GAUGE, MetricUnits.BYTES));

        //JVM METRICS
        registry.register("jvm.uptime", new BMGauge<Number>(BaseMetricConstants.RUNTIME_OBJECT_TYPE, "Uptime"),
                          new Metadata("jvm.uptime", "JVM Uptime", "Displays the start time of the Java virtual machine in milliseconds. "
                                                                   + "This attribute displays the approximate time when the Java virtual machine started.", MetricType.GAUGE, MetricUnits.MILLISECONDS));

        //THREAD JVM -
        registry.register("thread.count", new BMCounter(BaseMetricConstants.THREAD_OBJECT_TYPE, "ThreadCount"),
                          new Metadata("thread.count", "Thread Count", "Displays the current number of live threads including both daemon "
                                                                       + "and non-daemon threads", MetricType.COUNTER, MetricUnits.NONE));

        registry.register("thread.daemon.count", new BMCounter(BaseMetricConstants.THREAD_OBJECT_TYPE, "DaemonThreadCount"),
                          new Metadata("thread.daemon.count", "Daemon Thread Count", "Displays the current number of live daemon threads.", MetricType.COUNTER, MetricUnits.NONE));

        registry.register("thread.max.count", new BMCounter(BaseMetricConstants.THREAD_OBJECT_TYPE, "PeakThreadCount"),
                          new Metadata("thread.max.count", "Peak Thread Count", "Displays the peak live thread count since the Java virtual"
                                                                                + " machine started or peak was reset. This includes daemon and non-daemon threads.", MetricType.COUNTER, MetricUnits.NONE));

        //CLASSLOADING METRICS
        registry.register("classloader.currentLoadedClass.count", new BMCounter(BaseMetricConstants.CLASSLOADING_OBJECT_TYPE, "LoadedClassCount"),
                          new Metadata("classloader.currentLoadedClass.count", "Current Loaded Class Count", "Displays the number of classes that are currently loaded"
                                                                                                             + " in the Java virtual machine.", MetricType.COUNTER, MetricUnits.NONE));

        registry.register("classloader.totalLoadedClass.count", new BMCounter(BaseMetricConstants.CLASSLOADING_OBJECT_TYPE, "TotalLoadedClassCount"),
                          new Metadata("classloader.totalLoadedClass.count", "Total Loaded Class Count", "Displays the total number of classes that have been "
                                                                                                         + "loaded since the Java virtual machine has started execution.", MetricType.COUNTER, MetricUnits.NONE));

        registry.register("classloader.totalUnloadedClass.count", new BMCounter(BaseMetricConstants.CLASSLOADING_OBJECT_TYPE, "UnloadedClassCount"),
                          new Metadata("classloader.totalUnloadedClass.count", "Total Unloaded Class Count", "Displays the total number of classes unloaded "
                                                                                                             + "since the Java virtual machine has started execution.", MetricType.COUNTER, MetricUnits.NONE));

        //OPERATING SYSTEM
        registry.register("cpu.availableProcessors", new BMGauge<Number>(BaseMetricConstants.OS_OBJECT_TYPE, "AvailableProcessors"),
                          new Metadata("cpu.availableProcessors", "Available Processors", "Displays the number of processors available to the "
                                                                                          + "Java virtual machine. This value may change during a particular invocation of the virtual machine.", MetricType.GAUGE, MetricUnits.NONE));

        registry.register("cpu.systemLoadAverage", new BMGauge<Number>(BaseMetricConstants.OS_OBJECT_TYPE, "SystemLoadAverage"),
                          new Metadata("cpu.systemLoadAverage", "System Load Average", "Displays the system load average for the last minute. "
                                                                                       + "The system load average is the sum of the number of runnable entities queued to the available processors "
                                                                                       + "and the number of runnable entities running on the available processors averaged over a period of time. "
                                                                                       + "The way in which the load average is calculated is operating system specific but is typically a damped "
                                                                                       + "time-dependent average. If the load average is not available, a negative value is displayed. "
                                                                                       + "This attribute is designed to provide a hint about the system load and may be queried frequently. "
                                                                                       + "The load average may be unavailable on some platform where it is expensive to implement this method.", MetricType.GAUGE, MetricUnits.NONE));

        registry.register("cpu.processCpuLoad", new BMGauge<Number>(BaseMetricConstants.OS_OBJECT_TYPE, "ProcessCpuLoad"),
                          new Metadata("cpu.processCpuLoad", "Process CPU Load", "Displays the 'recent cpu usage' for the Java Virtual "
                                                                                 + "Machine process", MetricType.GAUGE, MetricUnits.PERCENT));

        //GARBAGE COLLECTOR METRICS
        for (String gcName : gcObjectNames) {

            String gcNameNoSpace = removeSpaces(gcName);

            //gc.%s.count
            String nameToRegister = "gc." + gcNameNoSpace + ".count";
            registry.register(nameToRegister, new BMCounter(BaseMetricConstants.GC_OBJECT_TYPE_NAME + gcName, "CollectionCount"),
                              new Metadata(nameToRegister, "Garbage Collection Count", "Displays the total number of collections that have occurred. "
                                                                                       + "This attribute lists -1 if the collection count is undefined for this collector.", MetricType.COUNTER, MetricUnits.NONE));

            //gc.%s.time
            nameToRegister = "gc." + gcNameNoSpace + ".time";
            registry.register(nameToRegister, new BMGauge<Number>(BaseMetricConstants.GC_OBJECT_TYPE_NAME + gcName, "CollectionTime"),
                              new Metadata(nameToRegister, "Garbage Collection Time", "Displays the approximate accumulated collection elapsed "
                                                                                      + "time in milliseconds. This attribute displays -1 if the collection elapsed time is undefined "
                                                                                      + "for this collector. The Java virtual machine implementation may use a high resolution timer "
                                                                                      + "to measure the elapsed time. This attribute may display the same value even if the collection "
                                                                                      + "count has been incremented if the collection elapsed time is very short.", MetricType.GAUGE, MetricUnits.MILLISECONDS));
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