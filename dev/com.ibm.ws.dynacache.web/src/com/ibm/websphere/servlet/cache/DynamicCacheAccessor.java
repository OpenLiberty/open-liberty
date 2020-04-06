/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.servlet.cache;

/**
 * 
 * 
 * @ibm-api 
 * @see com.ibm.websphere.cache.DynamicCacheAccessor
 * @deprecated 
 *             You should use com.ibm.websphere.cache.DynamicCacheAccessor
 * @ibm-api 
 */
public class DynamicCacheAccessor {


/**
 * This obtains a reference to the dynamic cache.
 * @return Reference to the cache or null if caching is disabled 
 * @deprecated 
 *             You should use com.ibm.websphere.cache.DynamicCacheAccessor
 * @ibm-api 
 */
   public static com.ibm.websphere.cache.Cache getCache() {
      return (com.ibm.websphere.cache.Cache) com.ibm.websphere.cache.DynamicCacheAccessor.getCache();
   }

/**
 * This determines if caching is enabled.
 * @return true if caching is enabled, false if it is disabled.
 * @deprecated 
 *             You should use com.ibm.websphere.cache.DynamicCacheAccessor
 * @ibm-api 
 */
   public static boolean isCachingEnabled() {
      return com.ibm.websphere.cache.DynamicCacheAccessor.isCachingEnabled();
   }


}
