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

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.MBeanServerDelegate;

import org.eclipse.microprofile.metrics.MetricRegistry;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.microprofile.metrics.impl.SharedMetricRegistries;
import com.ibm.ws.microprofile.metrics.monitor.MappingTable;
import com.ibm.ws.microprofile.metrics.monitor.MonitorMetrics;
import com.ibm.ws.microprofile.metrics.monitor.MonitorMetricsHandler;

@Component(name = "com.ibm.ws.microprofile.metrics21.monitor.MonitorMetricsHandler21", property = { "service.vendor=IBM"})
public class MonitorMetricsHandler21 extends MonitorMetricsHandler {
	
	private static final TraceComponent tc = Tr.register(MonitorMetricsHandler21.class);
	@Override
    protected void activate(ComponentContext context) {
		this.mappingTable = MappingTable.getInstance();
		register();
        addMBeanListener();
        Tr.info(tc, "FEATURE_REGISTERED");
    }
	@Override
	@Reference
    public void getSharedMetricRegistries(SharedMetricRegistries sharedMetricRegistry) {
        super.sharedMetricRegistry = sharedMetricRegistry;
    }
	@Override
    protected void deactivate(ComponentContext context) {
    	MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
    	if (listener != null) {
        	try {
    			mbs.removeNotificationListener(MBeanServerDelegate.DELEGATE_NAME, listener);
    		} catch (Exception e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "deactivate exception message: ", e.getMessage());
                    FFDCFilter.processException(e, getClass().getSimpleName(), "deactivate:Exception");
                }
    		}     		
			listener = null;
    	}
    	SharedMetricRegistries.remove(MetricRegistry.Type.VENDOR.getName());
   	
    	Tr.info(tc, "FEATURE_UNREGISTERED");
    }
	@Override
	protected void register(String objectName, String[][] data) {
        MonitorMetrics metrics = null;
		if (!containMetrics(objectName))  {
			metrics = new MonitorMetrics21(objectName);
			metrics.createMetrics(super.sharedMetricRegistry, data);
        	metricsSet.add(metrics);
        	Tr.debug(tc, "Monitoring MXBean " + objectName + " is registered to mpMetrics.");
        } else {
        	Tr.debug(tc, objectName + " is already registered.");
        }
		
	}

}
