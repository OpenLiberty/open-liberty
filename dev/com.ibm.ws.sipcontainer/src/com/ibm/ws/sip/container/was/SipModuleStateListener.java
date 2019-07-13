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
package com.ibm.ws.sip.container.was;

import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.sip.SipServlet;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedModuleInfo;
import com.ibm.ws.container.service.state.ModuleStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.sip.container.events.EventsDispatcher;
import com.ibm.ws.sip.container.parser.SipAppDesc;
import com.ibm.ws.sip.container.parser.SipServletDesc;
import com.ibm.ws.sip.container.router.SipAppDescManager;
import com.ibm.ws.webcontainer.osgi.metadata.WebModuleMetaDataImpl;
import com.ibm.ws.webcontainer.webapp.WebAppConfiguration;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE, configurationPid = "com.ibm.ws.sip.container.was.SipModuleStateListener", service = ModuleStateListener.class, property = { "service.vendor=IBM" })
public class SipModuleStateListener implements ModuleStateListener {

	/**
	 * Class Logger.
	 */
	private static final LogMgr c_logger = Log.get(SipModuleStateListener.class);
	
	/**
	 * @see com.ibm.ws.container.service.state.ModuleStateListener#moduleStarting(com.ibm.ws.container.service.app.deploy.ModuleInfo)
	 */
	@Override
	public void moduleStarting(ModuleInfo moduleInfo)
			throws StateChangeException {
		// TODO Auto-generated method stub

	}

	/**
	 * When the module is started we try to initalize the application if there is a load on startup servlet
	 * @see com.ibm.ws.container.service.state.ModuleStateListener#moduleStarted(com.ibm.ws.container.service.app.deploy.ModuleInfo)
	 */
	@Override
	public void moduleStarted(ModuleInfo moduleInfo)
			throws StateChangeException {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(null, "moduleStarted", moduleInfo);
		}
		

			
	}

	/**
	 * @see com.ibm.ws.container.service.state.ModuleStateListener#moduleStopping(com.ibm.ws.container.service.app.deploy.ModuleInfo)
	 */
	@Override
	public void moduleStopping(ModuleInfo moduleInfo) {
		// TODO Auto-generated method stub

	}

	/**
	 * @see com.ibm.ws.container.service.state.ModuleStateListener#moduleStopped(com.ibm.ws.container.service.app.deploy.ModuleInfo)
	 */
	@Override
	public void moduleStopped(ModuleInfo moduleInfo) {
		// TODO Auto-generated method stub

	}

}
