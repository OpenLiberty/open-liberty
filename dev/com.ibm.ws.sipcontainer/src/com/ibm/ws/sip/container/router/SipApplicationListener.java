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
package com.ibm.ws.sip.container.router;

import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sip.container.was.filters.SipFilter;
import com.ibm.ws.webcontainer.webapp.WebApp;

@Component(service = javax.servlet.ServletContainerInitializer.class,
configurationPolicy = ConfigurationPolicy.IGNORE,
configurationPid = "com.ibm.ws.sip.container.router.SipApplicationListener",
name = "com.ibm.ws.sip.container.router.SipApplicationListener",
property = {"service.vendor=IBM"} )
public class SipApplicationListener implements ServletContainerInitializer {
	
	/*
	 * Trace variable
	 */
	private static final TraceComponent tc = Tr
			.register(SipApplicationListener.class);
	
	SipAppDescManager appDescMangar  = SipAppDescManager.getInstance();

	/**
	 * This method is invoked during the initialization of the webApp.
	 * In case webApp is being initialized before sipApp, we need to be invoked during initialization to initialize sip related members
	 * (such as SipFilter etc.) 
	 */
	@Override
	public void onStartup(Set<Class<?>> arg0, ServletContext arg1)
			throws ServletException {
		WebApp webApp = (WebApp)arg1;
		
		
		try {
			if (appDescMangar.getSipAppDesc(webApp) != null) {
				appDescMangar.getSipAppDesc(webApp).setIsDuringWebAppInitialization(true);
				appDescMangar.initSipAppIfNeeded(webApp.getName());
			}
		} catch (Throwable e) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc,"SipApplicationListener:onStartup Failed to initalized application: "+ webApp.getName() , e.getLocalizedMessage(),e.getStackTrace());
			}
		}
		
	}

	

}
