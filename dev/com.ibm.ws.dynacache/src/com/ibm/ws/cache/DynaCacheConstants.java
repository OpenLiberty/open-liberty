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
package com.ibm.ws.cache;

public class DynaCacheConstants {
	
	// For VBC feature eanbled:
	public static final boolean VBC_CACHE_NEW_CONTENT  = false;
	public static final boolean VBC_USE_CACHED_CONTENT = true;
	
	// For VBC feature enabled: If cache entry is invalid ( VET < t < RET ), these constant will be used. 
	// It is used to indicate getting cache entry from local memory, disk or remote
	public static final int VBC_INVALID_NOT_USED = 0;  // default
	public static final int VBC_INVALID_MEMORY_HIT = 1;
	public static final int VBC_INVALID_DISK_HIT = 2;
	public static final int VBC_INVALID_REMOTE_HIT = 3;
	
	// These constants are used for setting up the trace
	public static final String TRACE_GROUP = "WebSphere Dynamic Cache"; //$NON-NLS-1$
	public static final String NLS_FILE = "com.ibm.ws.cache.resources.dynacache"; //$NON-NLS-1$
	
	public static final String WEBSPHERE_TYPE = "WebSphere:type=DynaCache,*"; //$NON-NLS-1$

	// MBean commands
	public static final String MBEAN_GET_CACHE_INSTANCE_NAMES = "getCacheInstanceNames"; //$NON-NLS-1$
	public static final String MBEAN_GET_ALL_CACHE_STATISTICS = "getAllCacheStatistics"; //$NON-NLS-1$
	public static final String MBEAN_GET_CACHE_STATISTICS = "getCacheStatistics"; //$NON-NLS-1$
	public static final String MBEAN_GET_CACHE_IDS_IN_MEMORY = "getCacheIDsInMemory"; //$NON-NLS-1$
	public static final String MBEAN_GET_CACHE_IDS_ON_DISK = "getCacheIDsOnDisk"; //$NON-NLS-1$
	public static final String MBEAN_GET_CACHE_IDS_IN_PUSHPULLTABLE = "getCacheIDsInPushPullTable"; //$NON-NLS-1$
	public static final String MBEAN_GET_CACHE_ENTRY = "getCacheEntry"; //$NON-NLS-1$
	public static final String MBEAN_INVALIDATE_CACHE_IDS = "invalidateCacheIDs"; //$NON-NLS-1$
	public static final String MBEAN_CLEAR_CACHE = "clearCache"; //$NON-NLS-1$
	public static final String MBEAN_GET_CACHE_DIGEST = "getCacheDigest"; //$NON-NLS-1$
	public static final String MBEAN_COMPARE_CACHES = "compareCaches"; //$NON-NLS-1$
	
	public static final String NODE = "node"; //$NON-NLS-1$
	public static final String PROCESS = "process"; //$NON-NLS-1$

	public static final String OBJECT_CLASS_STRING = "java.lang.String"; //$NON-NLS-1$
	public static final String OBJECT_CLASS_STRING_ARRAY = "[Ljava.lang.String;"; //$NON-NLS-1$
	
	public static final String PRIMITIVE_TYPE_BOOLEAN = "boolean"; //$NON-NLS-1$
	
	public static final String NEW_LINE = "\n"; //$NON-NLS-1$
	public static final String EMPTY_STRING = ""; //$NON-NLS-1$
}
