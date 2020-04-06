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
 * The IdGenerator normally is responsible for generating cache 
 * entry ids and data ids, and invalidating data ids. <p>
 * One IdGenerator instance will exist for each cacheable servlet 
 * identified in WebSphere.  When implementing this interface, be
 * aware that multiple threads may be using the same IdGenerator
 * concurrently. 
 * @ibm-api 
 */
public interface IdGenerator {

   /**
    * @deprecated
    * This method is called once on servlet initialization, 
    * and should take configuration values from its CacheConfig
    * argurment and store them locally. Additional config 
    * information from user applications or other sources may be 
    * read here as well.  
    * @ibm-api 
    */
   public void initialize(CacheConfig cc);

   /**
    * This method is called once on every request for a cacheable 
    * servlet. It generates the id that is used as a key by the 
    * cache to identify the output of the servlet.
    *
    * @param request The request object being used for this invocation
    *
    * @return a String uniquely identifying this invocation
    * of a cacheable servlet.
    * @ibm-api 
    */
   public String getId(ServletCacheRequest request);

   /** 
    * @deprecated
    *
    * @return the Sharing Policy of this cache entry
    * @ibm-api 
    */
   public int getSharingPolicy(ServletCacheRequest request);

}
