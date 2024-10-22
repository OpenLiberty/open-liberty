/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.monitor.metrics;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.time.Duration;
import java.util.List;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.http.monitor.HttpStatAttributes;
import io.openliberty.http.monitor.HttpServerStatsMonitor;

@Component(configurationPolicy = IGNORE, immediate = true)
public class MetricsManager {

	
	private static MetricsManager instance;
	
	private static final TraceComponent tc = Tr.register(MetricsManager.class);
    
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    private volatile List<HTTPMetricAdapter> httpMetricRuntimes;
	
    
    @Activate
    public void activate() {
    	instance = this;
    }
    
    @Deactivate
    public void deactivate() {

    	instance = null;
    }
   
    public static MetricsManager getInstance() {
    	if (instance != null) {
        	return instance;
    	} 
    	
    	if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
        	Tr.debug(tc, "No RestMetricManager Instance available ");
    	}
    	return null;
    }
    
    /**
     * 
     * @param httpStatAttributes
     * @param duration
     */
	public void updateHttpMetrics(HttpStatAttributes httpStatAttributes, Duration duration) {
		httpMetricRuntimes.stream().forEach(adapters -> adapters.updateHttpMetrics(httpStatAttributes,duration));	
	}
	

	
}
