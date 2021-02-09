/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
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
import java.util.Map;
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
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.microprofile.metrics.impl.CounterImpl;
import com.ibm.ws.microprofile.metrics.impl.SharedMetricRegistries;

@Component(service = { BaseMetrics.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class BaseMetrics {
    private static String BASE = MetricRegistry.Type.BASE.getName();
    MBeanServer mbs;
    private static Set<String> gcObjectNames = new HashSet<String>();

    private static SharedMetricRegistries SHARED_METRIC_REGISTRY;

    @Reference
    public void setSharedMetricRegistries(SharedMetricRegistries sharedMetricRegistry) {
        BaseMetrics.SHARED_METRIC_REGISTRY = sharedMetricRegistry;
    }

    @Activate
    protected void activate(ComponentContext context, Map<String, Object> properties) {
        mbs = ManagementFactory.getPlatformMBeanServer();
        Set<ObjectInstance> mBeanObjectInstanceSet = mbs.queryMBeans(null, null);
        populateGcNames(mBeanObjectInstanceSet);
        createBaseMetrics();
    }

    @Deactivate
    protected void deactivate(ComponentContext context, int reason) {
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
        registry.register("memory.usedHeap", new BMGauge<Number>(BaseMetricConstants.MEMORY_OBJECT_TYPE, "HeapMemoryUsage", "used"),
                          new Metadata("memory.usedHeap", "Used Heap Memory", "memory.usedHeap.description", MetricType.GAUGE, MetricUnits.BYTES));

        registry.register("memory.committedHeap", new BMGauge<Number>(BaseMetricConstants.MEMORY_OBJECT_TYPE, "HeapMemoryUsage", "committed"),
                          new Metadata("memory.committedHeap", "Committed Heap Memory", "memory.committedHeap.description", MetricType.GAUGE, MetricUnits.BYTES));

        registry.register("memory.maxHeap", new BMGauge<Number>(BaseMetricConstants.MEMORY_OBJECT_TYPE, "HeapMemoryUsage", "max"),
                          new Metadata("memory.maxHeap", "Max Heap Memory", "memory.maxHeap.description", MetricType.GAUGE, MetricUnits.BYTES));

        //JVM METRICS
        registry.register("jvm.uptime", new BMGauge<Number>(BaseMetricConstants.RUNTIME_OBJECT_TYPE, "Uptime"),
                          new Metadata("jvm.uptime", "JVM Uptime", "jvm.uptime.description", MetricType.GAUGE, MetricUnits.MILLISECONDS));

        //THREAD JVM -
        registry.register("thread.count", new BMCounter(BaseMetricConstants.THREAD_OBJECT_TYPE, "ThreadCount"),
                          new Metadata("thread.count", "Thread Count", "thread.count.description", MetricType.COUNTER, MetricUnits.NONE));

        registry.register("thread.daemon.count", new BMCounter(BaseMetricConstants.THREAD_OBJECT_TYPE, "DaemonThreadCount"),
                          new Metadata("thread.daemon.count", "Daemon Thread Count", "thread.daemon.count.description", MetricType.COUNTER, MetricUnits.NONE));

        registry.register("thread.max.count", new BMCounter(BaseMetricConstants.THREAD_OBJECT_TYPE, "PeakThreadCount"),
                          new Metadata("thread.max.count", "Peak Thread Count", "thread.max.count.description", MetricType.COUNTER, MetricUnits.NONE));

        //CLASSLOADING METRICS
        registry.register("classloader.currentLoadedClass.count", new BMCounter(BaseMetricConstants.CLASSLOADING_OBJECT_TYPE, "LoadedClassCount"),
                          new Metadata("classloader.currentLoadedClass.count", "Current Loaded Class Count", "classloader.currentLoadedClass.count.description", MetricType.COUNTER, MetricUnits.NONE));

        registry.register("classloader.totalLoadedClass.count", new BMCounter(BaseMetricConstants.CLASSLOADING_OBJECT_TYPE, "TotalLoadedClassCount"),
                          new Metadata("classloader.totalLoadedClass.count", "Total Loaded Class Count", "classloader.totalLoadedClass.count.description", MetricType.COUNTER, MetricUnits.NONE));

        registry.register("classloader.totalUnloadedClass.count", new BMCounter(BaseMetricConstants.CLASSLOADING_OBJECT_TYPE, "UnloadedClassCount"),
                          new Metadata("classloader.totalUnloadedClass.count", "Total Unloaded Class Count", "classloader.totalUnloadedClass.count.description", MetricType.COUNTER, MetricUnits.NONE));

        //OPERATING SYSTEM
        registry.register("cpu.availableProcessors", new BMGauge<Number>(BaseMetricConstants.OS_OBJECT_TYPE, "AvailableProcessors"),
                          new Metadata("cpu.availableProcessors", "Available Processors", "cpu.availableProcessors.description", MetricType.GAUGE, MetricUnits.NONE));

        registry.register("cpu.systemLoadAverage", new BMGauge<Number>(BaseMetricConstants.OS_OBJECT_TYPE, "SystemLoadAverage"),
                          new Metadata("cpu.systemLoadAverage", "System Load Average", "cpu.systemLoadAverage.description", MetricType.GAUGE, MetricUnits.NONE));

        registry.register("cpu.processCpuLoad", new BMGauge<Number>(BaseMetricConstants.OS_OBJECT_TYPE, "ProcessCpuLoad"),
                          new Metadata("cpu.processCpuLoad", "Process CPU Load", "cpu.processCpuLoad.description", MetricType.GAUGE, MetricUnits.PERCENT));

        //GARBAGE COLLECTOR METRICS
        for (String gcName : gcObjectNames) {

            String gcNameNoSpace = removeSpaces(gcName);

            //gc.%s.count
            String nameToRegister = "gc." + gcNameNoSpace + ".count";
            registry.register(nameToRegister, new BMCounter(BaseMetricConstants.GC_OBJECT_TYPE_NAME + gcName, "CollectionCount"),
                              new Metadata(nameToRegister, "Garbage Collection Count", "garbageCollectionCount.description", MetricType.COUNTER, MetricUnits.NONE));

            //gc.%s.time
            nameToRegister = "gc." + gcNameNoSpace + ".time";
            registry.register(nameToRegister, new BMGauge<Number>(BaseMetricConstants.GC_OBJECT_TYPE_NAME + gcName, "CollectionTime"),
                              new Metadata(nameToRegister, "Garbage Collection Time", "garbageCollectionTime.description", MetricType.GAUGE, MetricUnits.MILLISECONDS));
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