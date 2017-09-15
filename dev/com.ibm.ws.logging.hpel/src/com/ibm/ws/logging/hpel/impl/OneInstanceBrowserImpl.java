/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.hpel.impl;

import java.util.Map;

import com.ibm.ws.logging.hpel.LogRepositoryBrowser;
import com.ibm.ws.logging.hpel.MainLogRepositoryBrowser;
import com.ibm.ws.logging.object.hpel.RepositoryPointerImpl;

/**
 * Implementation of the browser over instances when selected directory is an instance itself.
 */
public class OneInstanceBrowserImpl implements MainLogRepositoryBrowser {

	private final LogRepositoryBrowser browser;
	/**
	 * Initialize this main browser with a browser over files in the instance
	 * @param browser providing details about the instance
	 */
	public OneInstanceBrowserImpl(LogRepositoryBrowser browser) {
		this.browser = browser;
	}
	
	@Override
	public LogRepositoryBrowser find(RepositoryPointerImpl location,
			boolean ignoreTimestamp) {
		String[] instanceIds = location.getInstanceIds();
		if (instanceIds.length == 0) {
			return null;
		}
		LogRepositoryBrowser result = browser;
		for (int i=1; i<instanceIds.length && result!=null; i++) {
			Map<String, LogRepositoryBrowser> map = result.getSubProcesses();
			result = map.get(instanceIds[i]);
		}
		return result;
	}

	@Override
	public LogRepositoryBrowser findByMillis(long timestamp) {
		return timestamp<0 || browser.getTimestamp()<=timestamp ? browser : null;
	}

	@Override
	public LogRepositoryBrowser findNext(LogRepositoryBrowser current,
			long timelimit) {
		if (current == null) {
			return browser;
		} else {
			return null;
		}
	}

	@Override
	public LogRepositoryBrowser findNext(RepositoryPointerImpl location,
			long timelimit) {
		return null;
	}

}
