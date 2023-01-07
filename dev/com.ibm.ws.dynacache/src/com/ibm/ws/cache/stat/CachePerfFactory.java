/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
package com.ibm.ws.cache.stat;

import com.ibm.ws.cache.intf.DCache;

public interface CachePerfFactory {
    
    /**
     * @param cacheName
     * @param swapToDisk
     * @return a CachePerf for the given cache, or null if no performance stats
     */
    CachePerf create(DCache cache);
}
