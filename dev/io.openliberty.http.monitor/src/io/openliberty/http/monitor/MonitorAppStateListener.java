/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.monitor;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.kernel.productinfo.ProductInfo;

@Component(service = { ApplicationStateListener.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class MonitorAppStateListener implements ApplicationStateListener{

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
    	/*
    	 * Just prevent this from happening.
    	 * This bundle starts from an auto-feature.
    	 * User's are not explicitly enabling this so
    	 * lets not throw an exception. We'll
    	 * quietly get out of the way.
    	 */
    	if (!ProductInfo.getBetaEdition()) { 
    		return;
    	}
		HttpStatsMonitor.getInstance().removeStat(appInfo.getDeploymentName());
		
	}

}
