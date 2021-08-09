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
package com.ibm.ws.cache.servlet;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

public class ESICacheEntryStats {

   private static final TraceComponent _tc = Tr.register(ESICacheEntryStats.class,"WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");
   private String    _cacheId = null;

   public ESICacheEntryStats () {}    

   /**
    * Return the URL of the cache entry.
    * @return The URL of the cache entry.
    */
   public String getCacheId()
   {
      return _cacheId;
   }
   /**
    * Set the URL of the cache entry.
    * @param The URL of the cache entry.
    */
   public void setCacheId (String cacheId)
   {
      _cacheId = cacheId;
      if (_tc.isDebugEnabled()) Tr.debug(_tc, "setCacheId " + cacheId);
   }

   public String toString()
   {
      return _cacheId;
   }
}
