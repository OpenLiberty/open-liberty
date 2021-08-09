/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container;

import com.ibm.ws.http.VirtualHost;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.ws.webcontainer.webapp.WebAppConfiguration;
import com.ibm.ws.webcontainer.webapp.WebGroup;
import com.ibm.ws.webcontainer.webapp.WebGroupConfiguration;

public abstract class DeployedModule 
{

	/**
	 * @return
	 */
	public ClassLoader getClassLoader() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @return
	 */
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @return
	 */
	public String getContextRoot() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @return
	 */
	public String getDisplayName() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @return
	 */
	public abstract WebAppConfiguration getWebAppConfig();
	/**
	 * @return
	 */
	public abstract WebApp getWebApp();

	/**
	 * @return
	 */
	public String getVirtualHostName() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @return
	 */
	public WebGroupConfiguration getWebGroupConfig() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @return
	 */
	public WebGroup getWebGroup() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @return
	 */
	public VirtualHost[] getVirtualHosts()
	{
		// TODO Auto-generated method stub
		return null;
	}

}
