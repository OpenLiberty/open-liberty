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

import javax.servlet.http.HttpServletResponse;

/**
 * The MetaDataGenerator normally is responsible for assigning 
 * timeout, external cache group, and priority via the appropriate
 * methods in the entry's FragmentInfo object. <p>
 * One MetaDataGenerator will exist for each cacheable servlet 
 * identified in WebSphere.  When implementing this interface, be
 * aware that multiple threads may be using the same MetaDataGenerator
 * concurrently. 
 * @ibm-api 
 */
public interface MetaDataGenerator {
   /**
    * @deprecated
    * Initialize is called once on servlet initialization, and should 
    * take configuration values from its CacheConfig argurment and 
    * store them locally. Additional config information from user 
    * applications or other sources may be read here as well. 
    * @ibm-api 
    */
   public void initialize(CacheConfig cc);
   
   /**
    * The servlet engine calls this method each time a new cache entry 
    * is created. It sets that entry's timeout, and optionally its 
    * priority and any external cache groups it is a member of. To 
    * set these variables, the setMetaData method should call 
    * CacheProxyRequest.getFragmentInfo() to get the 
    * FragmentInfo object associated with 
    * the entry being built. Then use methods like 
    * FragmentInfo.setTimeLimit() to set timeout values according 
    * to your application's needs.
    * 
    * @param request The request object being used for this invocation
    * @param response The response object being used for this invocation
    * @ibm-api 
    */
   public void setMetaData(ServletCacheRequest request, HttpServletResponse response);
}

