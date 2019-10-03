/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics21.monitor;


import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Tag;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.metrics.impl.SharedMetricRegistries;
import com.ibm.ws.microprofile.metrics.monitor.MappingTable;
import com.ibm.ws.microprofile.metrics.monitor.MonitorCounter;
import com.ibm.ws.microprofile.metrics.monitor.MonitorGauge;
import com.ibm.ws.microprofile.metrics.monitor.MonitorMetrics;

public class MonitorMetrics21 extends MonitorMetrics {
	private static final TraceComponent tc = Tr.register(MonitorMetrics21.class);


	public MonitorMetrics21(String objectName) {
		super(objectName);
	}
	@Override
	public void createMetrics(SharedMetricRegistries sharedMetricRegistry, String[][] data) {
		
        MetricRegistry registry = sharedMetricRegistry.getOrCreate(MetricRegistry.Type.VENDOR.getName());
        
        for (String[] metricData : data) {
        	
            String metricName = metricData[MappingTable.METRIC_NAME];
            String metricTagName = metricData[MappingTable.MBEAN_STATS_NAME];
            
            Tag metricTag = null;
            if (metricTagName != null) {
            	metricTag = new Tag(metricTagName, getMBeanStatsString());
            }
            MetricID metricID = new MetricID(metricName, metricTag);
            MetricType type = MetricType.valueOf(metricData[MappingTable.METRIC_TYPE]);
            
            if (MetricType.COUNTER.equals(type)) {
                MonitorCounter mc = metricData[MappingTable.MBEAN_SUBATTRIBUTE] == null ? 
                        new MonitorCounter(mbs, objectName, metricData[MappingTable.MBEAN_ATTRIBUTE]) :
                        new MonitorCounter(mbs, objectName, metricData[MappingTable.MBEAN_ATTRIBUTE], metricData[MappingTable.MBEAN_SUBATTRIBUTE]);
                
        		registry.register(
        				Metadata.builder().withName(metricName).withDisplayName(metricData[MappingTable.METRIC_DISPLAYNAME]).withDescription(metricData[MappingTable.METRIC_DESCRIPTION]).withType(type).withUnit(metricData[MappingTable.METRIC_UNIT]).build(), 
        			mc, metricTag);
        		
        		
        		
        		metricIDs.add(metricID);
        		Tr.debug(tc, "Registered " + metricID.toString());
        	} else if (MetricType.GAUGE.equals(type)) {
            	MonitorGauge<Number> mg =  metricData[MappingTable.MBEAN_SUBATTRIBUTE] == null ?
            		new MonitorGauge21<Number>(mbs, objectName, metricData[MappingTable.MBEAN_ATTRIBUTE]) :
            		new MonitorGauge21<Number>(mbs, objectName, metricData[MappingTable.MBEAN_ATTRIBUTE], metricData[MappingTable.MBEAN_SUBATTRIBUTE]);
        		registry.register(
        				Metadata.builder().withName(metricName).withDisplayName(metricData[MappingTable.METRIC_DISPLAYNAME]).withDescription(metricData[MappingTable.METRIC_DESCRIPTION]).withType(type).withUnit(metricData[MappingTable.METRIC_UNIT]).build(),
        			mg, metricTag);
        		metricIDs.add(metricID);
        		Tr.debug(tc, "Registered " + metricID.toString());
            } else {
            	Tr.debug(tc, "Falied to register " + metricName + " because of invalid type " + type);
            }
        }		
	}
}
