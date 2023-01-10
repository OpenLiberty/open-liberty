/*******************************************************************************
 * Copyright (c) 1997, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
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
