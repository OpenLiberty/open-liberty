/*******************************************************************************
 * Copyright (c) 1997, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.intf;

/**
 * This is the underlying JSPCache mechanism which is
 * used by the ServerCache and ServletCacheUnitImpl.
 */
public interface JSPCache {
	
	/**
	 * Sets the Cache for the JSP cache. It is called by the ServletCacheUnitImpl when Dynamic cache servlet service gets started.
	 * @param cache The Cache.
	 */
	public void setCache(DCache cache);
	
	/**
	 * Retrieve the underlying generic Cache object
	 */
	public DCache getCache();
	
}
