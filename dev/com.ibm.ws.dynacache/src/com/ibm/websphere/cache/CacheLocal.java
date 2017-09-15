/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.cache;

public interface CacheLocal {
	 /**
	    * Clear the contents of the cache in memory and optionally on disk.
	    * Calling this API method will NOT result in invalidations being sent to other servers/JVMs in the replication domain.
	    * If clearDisk is set to false, diskOffload and flushToDiskOnStop is enabled on the cache instance , the entries in memory will be flushed to disk before removing them from memory.
	    * 
	    * @param clearDisk  When set to true the disk cache will be cleared as well
	    */

	public void clearMemory(boolean clearDisk);
}

