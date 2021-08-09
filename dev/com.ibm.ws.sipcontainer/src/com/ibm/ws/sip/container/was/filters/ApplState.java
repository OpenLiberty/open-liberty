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
package com.ibm.ws.sip.container.was.filters;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.websphere.servlet.container.WebContainer;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.wsspi.webcontainer.util.ThreadContextHelper;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
configurationPid = "com.ibm.ws.sip.container.was.filters.AppState",
property = {"service.vendor=IBM"} )
public class ApplState implements ApplicationStateListener{

	 private static final LogMgr c_logger =
             Log.get(ApplState.class);
	public ApplState()  {
		// TODO Auto-generated constructor stub
	}

	
	@Override
	public void applicationStarting(ApplicationInfo appInfo)
			throws StateChangeException {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug( "ApplicationStateListener starting");
		}
    	try{
    		/* 
    		 * Using the Starting method of the application state listener to add a global listener to webContainer.
    		 * This  has to be done to avoid registering the listener after a servlet has initialized.
    		 */
    		ClassLoader cl = ThreadContextHelper.getContextClassLoader();
    		ThreadContextHelper.setClassLoader(this.getClass().getClassLoader());
    		WebContainer.addGlobalListener("com.ibm.ws.sip.container.was.filters.SipServletListener");
    		ThreadContextHelper.setClassLoader(cl);
    	}
    	catch(Exception exception){
    		c_logger.traceDebug("The following exception happened while trying to add a global listener to webcontainer. ", exception);
    	}
	}

	@Override
	public void applicationStarted(ApplicationInfo appInfo)
			throws StateChangeException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void applicationStopping(ApplicationInfo appInfo) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void applicationStopped(ApplicationInfo appInfo) {
		// TODO Auto-generated method stub
		
	}

}
