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
package com.ibm.wsspi.cache;

import java.util.Map;

/**
 * This interface is used by the WebSphere Admin Console to look
 * up all the cache providers and expose them on the Dynamic 
 * Cache Service panel, Cache Provider drop down menu.
 * 
 * @author Rohit
 * @private
 * @since WAS7.0.0
 * @ibm-spi
 * 
 */
public interface CacheProviderLoader {

	/**
	 * Returns an individual CacheProvider successfully
	 * loaded by Dynacache runtime
	 */
   	public CacheProvider getCacheProvider(String name); 

   	/**
   	 * Returns a map of cache provider names to {@link CacheProvider}
   	 * This map is used by the admin console to flush out the 
   	 * cache provider drop down menu.
   	 */
	public Map<String, CacheProvider> getCacheProviders();
	
}
