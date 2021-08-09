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
package com.ibm.wsspi.webcontainer;

import java.util.Properties;


/**
 *
* 
* WebContainerConfig is used to get WebContainer level config data such as
* default virtual host name and current caching status.
* @ibm-private-in-use
 */
public interface WebContainerConfig {

	/**
	 * Returns the defaultVirtualHostName.
	 * @return String
	 */
	public String getDefaultVirtualHostName();

	/**
	 * Returns the enableServletCaching.
	 * @return boolean
	 */
	public boolean isEnableServletCaching();

	public Properties getLocaleProps ();
	
	public Properties getConverterProps ();

	public boolean isArdEnabled();
	
	public int getArdIncludeTimeout();
	public int getMaximumExpiredEntries();
	public int getMaximumResponseStoreSize();

	void setUseAsyncRunnableWorkManager(boolean useAsyncRunnableWorkManager);

	void setAsyncRunnableWorkManagerName(String asyncRunnableWorkManagerName);

	void setNumAsyncTimerThreads(int numAsyncTimerThreads);

	boolean isUseAsyncRunnableWorkManager();

	String getAsyncRunnableWorkManagerName();

	int getNumAsyncTimerThreads();

	void setDefaultAsyncServletTimeout(long defaultAsyncServletTimeout);

	long getDefaultAsyncServletTimeout();


}
