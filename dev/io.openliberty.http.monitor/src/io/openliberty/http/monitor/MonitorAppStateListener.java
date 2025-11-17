/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.monitor;


import java.util.Map;
import java.util.stream.Stream;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.app.manager.module.DeployedAppInfoFactory;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.wsspi.application.handler.ApplicationTypeSupported;


@Component(service = { ApplicationStateListener.class }, configurationPid = "com.ibm.ws.monitor.internal.MonitoringFrameworkExtender", configurationPolicy = ConfigurationPolicy.OPTIONAL, immediate = true)
public class MonitorAppStateListener implements ApplicationStateListener{

	private final static String MONITORING_GROUP_FILTER = "filter";
	
    private static final TraceComponent tc = Tr.register(MonitorAppStateListener.class);
	
    /*
     * By Default, without any monitor-1.0 filters on, all monitor components are enabled
     */
    private static volatile boolean isHTTPEnabled = true;
    
    /*
     * Targeted with (type=spring)
     * Cheap way of using static, but ServletFilter needs to know.
     */
    private static ApplicationTypeSupported springApplicationTypeSupported = null;
    
    
    @Reference(target = "(type=spring)")
    public void setApplicationTypeSupported(ApplicationTypeSupported appTypeSupported) {
    	springApplicationTypeSupported = appTypeSupported;
    }
    
    public void unsetApplicationTypeSupported(ApplicationTypeSupported appTypeSupported) {
    	if (springApplicationTypeSupported == appTypeSupported) {
    		springApplicationTypeSupported = null;
    	}
    }
    
    public static boolean isSpringFeatureEnabled() {
    	return (springApplicationTypeSupported != null);
    }
    
    public static boolean isHTTPEnabled() {
        return isHTTPEnabled;
    }
    
	@Override
	public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {

	}

	@Override
	public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {

	}

	@Override
	public void applicationStopping(ApplicationInfo appInfo) {

	}

	@Override
	public void applicationStopped(ApplicationInfo appInfo) {
		HttpServerStatsMonitor.getInstance().removeStat(appInfo.getDeploymentName());
		
	}
	
    @Activate
    protected void activate(ComponentContext context, Map<String, Object> properties) {
        resolveMonitorFilter(properties);
    }

    @Modified
    protected void modified(ComponentContext context, Map<String, Object> properties) {
        resolveMonitorFilter(properties);
    }
    
    private void resolveMonitorFilter(Map<String, Object> properties) {
        String filter;

        if ((filter = (String) properties.get(MONITORING_GROUP_FILTER)) != null && filter.length() != 0) {
            // Original MonitoringFrameWorkExtender matches case

            if (filter.length() > 0) {
            	isHTTPEnabled = Stream.of(filter.split(",")).anyMatch(item -> item.equals("HTTP"));
            } else {
                // by default, every monitor component is enabled if length is 0
            	isHTTPEnabled = true;
            }
        } else if (filter == null) {
            /*
             * This bundle starts automatically with monitor-1.0 and servlet based features
             * If `filter` is null, we'll assume that there was no config and we will enable by default.
             */
        	isHTTPEnabled = true;
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, String.format("HTTP filter is enabled set to: [%s]", isHTTPEnabled));
        }
    }

}
