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
package io.openliberty.microprofile.telemetry.internal.utils.otelCollector;

import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Arrays;

import javax.json.JsonArray;
import javax.json.JsonValue;

import org.hamcrest.Matcher;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.utils.HttpRequest;

/**
 * Client for retrieving metrics from the OpenTelemetry Collector
 */
public class OtelCollectorQueryClient {

    private static final Class<OtelCollectorQueryClient> c = OtelCollectorQueryClient.class;

    private final String baseUrl;

    public OtelCollectorQueryClient(OtelCollectorContainer container) {
        baseUrl = container.getApiBaseUrl();
    }

    /**
     * The exported metrics are served at "http://otel-collector-metric:3131/metrics;
     */
    public String dumpMetrics() throws Exception {
        HttpRequest req = new HttpRequest(baseUrl + "/metrics");
        return req.run(String.class);
    }

    /**
     * For more info on the Prometheus metrics format: 
     * https://github.com/prometheus/docs/blob/main/content/docs/instrumenting/exposition_formats.md#text-format-details
     */
    public String getJVMMetrics() throws Exception{ 
        String result = dumpMetrics();
        List<String> splits = Arrays.asList(result.split("((?=# HELP))"));

        HashMap<String, Boolean> metricsFound = new HashMap<>();

        metricsFound.put("JvmClassCount",false);
        metricsFound.put("JvmClassLoaded",false);
        metricsFound.put("JvmClassUnloaded",false);
        metricsFound.put("JvmCpuCount",false);
        metricsFound.put("JvmCpuTime",false);
        metricsFound.put("JvmCpuRecentUtilization",false);
        metricsFound.put("JvmGcDuration",false);
        metricsFound.put("JvmMemoryCommitted",false);
        metricsFound.put("JvmMemoryUsedAfterLastGc",false);
        metricsFound.put("JvmMemoryLimit",false);
        metricsFound.put("JvmThreadCount",false);
        //Finds each expected JVM metric in the OpenTelemetry Collector output
        for(String s : splits){
            if(s.contains("jvm_class_count Number of classes currently loaded.")){
                metricsFound.put("JvmClassCount", true);
            }
            else if(s.contains("jvm_class_loaded Number of classes loaded since JVM start.")){
                metricsFound.put("JvmClassLoaded", true);
            }
            else if(s.contains("jvm_class_unloaded Number of classes unloaded since JVM start.")){
                metricsFound.put("JvmClassUnloaded", true);
            }
            else if(s.contains("jvm_cpu_count Number of processors available to the Java virtual machine.")){
                metricsFound.put("JvmCpuCount", true);
            }
            else if(s.contains("jvm_cpu_time CPU time used by the process as reported by the JVM.")){
                metricsFound.put("JvmCpuTime", true);
            }
            else if(s.contains("jvm_cpu_recent_utilization Recent CPU utilization for the process as reported by the JVM.")){
                metricsFound.put("JvmCpuRecentUtilization", true);
            }
            else if(s.contains("jvm_gc_duration Duration of JVM garbage collection actions.")){
                metricsFound.put("JvmGcDuration", true);
            }
            else if(s.contains("jvm_memory_committed Measure of memory committed.")){
                metricsFound.put("JvmMemoryCommitted", true);
            }
            else if(s.contains("jvm_thread_count Number of executing platform threads")){
                metricsFound.put("JvmThreadCount", true);
            }
            else if(s.contains("jvm_memory_used_after_last_gc Measure of memory used, as measured after the most recent garbage collection event on this pool.")){
                metricsFound.put("JvmMemoryUsedAfterLastGc", true);
            }
            else if(s.contains("jvm_memory_limit Measure of max obtainable memory.")){
                metricsFound.put("JvmMemoryLimit", true);
            }
            }
            if(!metricsFound.containsValue(false)){
                return "pass"; //All JVM metrics have been found
            }
            //Constructs a debug message to show which JVM metrics were found
            StringBuilder failureMessage= new StringBuilder();
            failureMessage.append("JVM metrics were missing. ");
            metricsFound.forEach((metric, found) -> {
                    failureMessage.append("Metric " + metric + " found = " + found + "\n");
            });
            
        return failureMessage.toString();
    }
}
