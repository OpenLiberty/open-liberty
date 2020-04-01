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
package com.ibm.ws.jaxrs.monitor;

import java.util.List;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.jaxrs.monitor.JaxRsMonitorFilter.RestMetricInfo;
import com.ibm.ws.jaxrs20.providers.api.JaxRsProviderRegister;

@Component(immediate = true, property = { "service.vendor=IBM" }, configurationPolicy = ConfigurationPolicy.IGNORE, service = {JaxRsProviderRegister.class, ApplicationStateListener.class})
public class JaxRsMonitorProviderRegister implements JaxRsProviderRegister, ApplicationStateListener {

	private JaxRsMonitorFilter monitorFilter = new JaxRsMonitorFilter();
	
    @Override
    public void installProvider(boolean clientSide, List<Object> providers, Set<String> features) {
        
    	// Register the metrics monitor filter class if we are not on the client.
        if (!clientSide) {
            // Add  built-in ContainerRequestFilter/ContainerResponseFilter to enable metric collection.
            providers.add(monitorFilter);
        }
    }
    
    @Override
    public void applicationStarting(ApplicationInfo appInfo) {
    	// When the application is starting we will create
    	// the application's metrics info object to store
    	// information such as whether the application is
    	// contained within an ear file or not.
    	String appName = appInfo.getDeploymentName();
    	RestMetricInfo metricInfo = monitorFilter.getMetricInfo(appName);
    	
    	// Determine if the application is packaged within an ear file.  This is 
    	// useful since a key created in the monitorFilter class will be 
    	// prefixed with the earname + warname or just the warname.
    	// See JaxRsMonitorFilter class for more information.
    	if (appInfo.getClass().getName().endsWith("EARApplicationInfoImpl")) {
    		metricInfo.setIsEar();
    	}
    }

    @Override
    public void applicationStarted(ApplicationInfo appInfo) {
    }

    @Override
    public void applicationStopping(ApplicationInfo appInfo) {
    }

    @Override
    public void applicationStopped(ApplicationInfo appInfo) {
    	// Allow the JaxRsMonitorFilter instance to clean up when the application
    	// is stopped.
    	monitorFilter.cleanApplication(appInfo.getDeploymentName());
    }

    
    
}
