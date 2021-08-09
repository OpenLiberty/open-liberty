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

import com.ibm.ws.cache.CacheProviderLoaderImpl;

/**
 * This factory returns a singleton instance of the 
 * {@link CacheProviderLoader} interface.
 * 
 */
public class CacheProviderLoaderFactory {

	public static CacheProviderLoader getCacheProviderLoader(){		
		return CacheProviderLoaderImpl.getInstance();		
	}	
}
