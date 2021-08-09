/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache;

import java.util.ArrayList;
import java.util.Properties;

import com.ibm.wsspi.library.Library;

public interface CacheService {

    public CacheConfig getCacheConfig();

    public String getCacheName();

    public void addCacheInstanceConfig(CacheConfig cacheconfig, boolean create) throws Exception;

    public CacheConfig addCacheInstanceConfig(Properties properties);

    public CacheConfig getCacheInstanceConfig(String reference);

    public void destroyCacheInstance(String reference);

    public ArrayList getServletCacheInstanceNames();

    public ArrayList getObjectCacheInstanceNames();

    public CacheInstanceInfo[] getCacheInstanceInfo();

    public Library getSharedLibrary();
}
