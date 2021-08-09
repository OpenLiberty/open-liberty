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
package com.ibm.websphere.cache;

/**
 * Implement this interface in your cacheable
 * object before placing it into cache if you
 * need to get control at the time the object
 * is being removed from cache.
 * 
 * @see DistributedNioMap
 * @ibm-api 
 */
public interface DistributedNioMapObject {
    /**
     * Release the cached object(ByteBuffers/MetaData) to the NIO buffer management.
     * 
     * @see DistributedNioMap
     * @ibm-api 
     */
	public void release();

	/**
	 * toString() method used to display.
     * @ibm-api 
	 */
	public String toString();
	
	/**
	 * This determines the best-effort size of the DistributedNioMapObject's value.

	 * @return The best-effort determination of the size of the DistributedNioMapObject's value. 
	 *  If the size cannot be determined, the return value is -1;
	 * @ibm-api
	 */
	public long getCacheValueSize();
}

