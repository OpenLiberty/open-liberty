/*******************************************************************************
 * Copyright (c) 1997, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.web.command;

import java.io.*;

import com.ibm.websphere.command.CacheableCommand;
import com.ibm.ws.cache.intf.DCache;
import com.ibm.ws.cache.DCacheBase;
import com.ibm.ws.cache.DynaCacheConstants;
import com.ibm.ws.cache.EntryInfo;

/**
 * It uses serialization to copy the command on put into the cache
 */
public class SerializedPutCommandStorage extends SerializedCommandStorage {
   
   private static final long serialVersionUID = 2356937049967292797L;
    
   /**
    * This implements the method in the CommandStoragePolicy interface.
    *
    * @param object The cached representation of the command.
    * @return The command that is given out during a cache hit.
    */
   public CacheableCommand prepareForCacheAccess(Serializable inputObject, DCache cache, EntryInfo ei) {
      if (inputObject instanceof byte[]) {
         //if it is still in byte[] format, deserialize and put back into
         // the cache so next cache hit does not require deserialize
         CacheableCommand cc = super.prepareForCacheAccess(inputObject,cache,ei);
         if ( cc != null ) {
        	 EntryInfo nei = (com.ibm.ws.cache.EntryInfo)cc.getEntryInfo();
        	 if (nei != null) {
        		 cache.setValue(nei, cc, !DCacheBase.COORDINATE, DynaCacheConstants.VBC_CACHE_NEW_CONTENT);
        	 }
        	 else { 
        		 cache.setValue(ei, cc, !DCacheBase.COORDINATE, DynaCacheConstants.VBC_CACHE_NEW_CONTENT);
        	 }
         }
         return cc;
      }
      return (CacheableCommand) inputObject;
   }
}
