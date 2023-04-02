/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
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
package com.ibm.wsspi.annocache.targets.cache;

public interface TargetCache_Factory {
	/**
	 * Create options with default values.
	 * 
	 * @return Cache options with default values.
	 */
    TargetCache_Options createOptions();

    /**
     * Answer the current cache options.
     * 
     * @return The current cache options.
     */
    TargetCache_Options getCacheOptions();
    
    /**
     * Set the cache options.  This must be done before
     * any activity which uses the cache.
     * 
     * @param options Options to set as the cache options.
     */
    void setOptions(TargetCache_Options options);
}
