/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.monitor.pmi.application;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.ws.sip.container.pmi.ApplicationModuleInterface;
import com.ibm.ws.sip.container.pmi.listener.ApplicationsPMIListener;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE)

/**
 * This class is a listener to SIP loaded applications. Each time the SIP Container loads a new SIP application
 * it calls getApplicationModuel() method that creates a module for this application to manage the application counters.
 */
public class ApplicationsModule implements ApplicationsPMIListener {

	/**
	 * Class Logger. 
	 */
	private static final Logger s_logger = Logger.getLogger(ApplicationsModule.class
		.getName());
	
	private final ConcurrentMap<String, ApplicationModuleInterface> appModules = new ConcurrentHashMap<String, ApplicationModuleInterface>();
	
	/** Singleton, created on the service activation */
    private static ApplicationsModule s_singleton = null;
    
	public static ApplicationsModule getInstance() {
        return s_singleton;
    }
	   
	/**
     * Activate this service component.
     */
    @Activate
    protected void activate(Map<String, Object> componentConfig) {
    	// Keep the instance created in DS activation
    	s_singleton = this;
    	if (s_logger != null && s_logger.isLoggable(Level.FINEST)) {
    		s_logger.log(Level.FINEST, "ApplicationsModule activated", componentConfig);
    	}
    }

    @Modified
	protected void modified(Map<String, Object> properties) {
    }
   
    /**
     * Deactivate this component.
     */
    @Deactivate
    protected void deactivate(int reason) {
    }
    
	
	@Override
	public void updateCounters(String appName) {
		ApplicationModule appModule = (ApplicationModule) appModules.get(appName);
		
		if (appModule != null) {
			appModule.updateCounters();
		}
	}

	@Override
	public ApplicationModuleInterface getApplicationModule(String appName) {
		if (appModules.containsKey(appName)) {
			return appModules.get(appName);
		}
		
		if (s_logger != null && s_logger.isLoggable(Level.FINEST)) {
            s_logger.logp(Level.FINEST, ApplicationModule.class.getName(),
					"getApplicationModule", "creating new ApplicationModule for " + appName);
        }
		ApplicationModuleInterface appModule = new ApplicationModule(appName);
		appModules.put(appName, appModule);
		
		return appModule;
	}

}
