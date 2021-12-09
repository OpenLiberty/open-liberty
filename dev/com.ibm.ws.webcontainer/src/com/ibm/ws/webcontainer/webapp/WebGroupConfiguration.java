/*******************************************************************************
 * Copyright (c) 1997, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.webapp;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.container.BaseConfiguration;
import com.ibm.ws.webcontainer.VirtualHost;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;

public class WebGroupConfiguration extends BaseConfiguration {
    
    protected static final Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.webapp");
    protected static final String CLASS_NAME = "com.ibm.ws.webcontainer.webapp.WebGroupConfiguration";
	
	private String contextRoot;
	private VirtualHost webAppHost;
	private int versionID;

	public WebGroupConfiguration(String id) 
	{
		super(id);
	}
	
	/**
	 * Returns the contextRoot.
	 * @return String
	 */
	public String getContextRoot() {
		return contextRoot;
	}

	/**
	 * Sets the contextRoot.
	 * @param contextRoot The contextRoot to set
	 */
	public void setContextRoot(String contextRoot) {
	    if (contextRoot.endsWith("/") && !contextRoot.equals("/")) {
	        logger.logp(Level.FINE, CLASS_NAME, "setContextRoot", "context root [" + contextRoot + "] cannot end with a slash (/)");

	        contextRoot = contextRoot.substring(0, contextRoot.length() - 1);
	    }

	    logger.logp(Level.FINE, CLASS_NAME, "setContextRoot", " [" + contextRoot + "]");
	    this.contextRoot = contextRoot;
	}

	/**
	 * Returns the isServlet2_3.
	 * @return boolean
	 */
	public boolean isServlet2_3() {
		return (versionID >= 23);
	}

	/**
	 * Returns the webAppHost.
	 * @return WebAppHost
	 */
	public VirtualHost getWebAppHost() {
		return webAppHost;
	}

	/**
	 * Sets the webAppHost.
	 * @param webAppHost The webAppHost to set
	 */
	public void setWebAppHost(VirtualHost webAppHost) {
		this.webAppHost = webAppHost;
	}

	/**
	 * @return
	 */
	public int getVersionID()
	{
		return versionID;
	}

	/**
	 * @param i
	 */
	public void setVersionID(int i)
	{
		versionID = i;
	}

}
