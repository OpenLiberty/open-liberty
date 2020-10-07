/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.servlets;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletUtil;

import com.ibm.ws.sip.container.osgi.ServletContextManager;
import com.ibm.ws.sip.container.osgi.ServletInstanceHolderFactory;

class SipServletUtilImpl implements SipServletUtil {
    public ServletConfig wrapConfig(ServletConfig cfg) {
    	return ServletContextManager.
    		getInstance().
    			getContextFactory().wrapContext(cfg);
    }
    public void initSiplet(ServletContext ctx, SipServlet siplet) {
    	String appName = (String)ctx.getAttribute(ServletConfigWrapper.APP_NAME_ATTRIBUTE);
    	// SPR #VKAE69MPJB
    	// add single instance support for servlet acting as listener
    	// works only for WAS environment
    	ServletInstanceHolderFactory.getInstanceHolder().addSipletInstance(
			appName,
			siplet.getClass().getName(),
			siplet);
	
    	ServletInstanceHolderFactory.getInstanceHolder().saveSipletReference(
			appName, 
			siplet, 
			ctx);
    }
}
