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

import com.ibm.ejs.ras.*;
import java.io.*;
import java.util.*;

/**
 * The ESIStats class is used for gathering and accessing statistics associated with
 * an ESI processor (e.g. the ESI fragment cache which resides in the WebSphere
 * webserver plugin).
 * 
 * Example usage:
 *    // Create an instance
 *    ESIStats stats = new ESIStats();
 *    // Gather counter plus cached entry information
 *    stats.setGatherEntries(true);
 *    // Gather statistics
 *    stats.gather();
 *    // Access stats
 *    ESIServerStats[] sStats = stats.getServerStats();
 *    for (int i = 0; i < sStats.length; i++) {
 *       System.out.println("host name: " + sStats[i].getHostName();
 *       ESIProcessorStats[] pStats = sStats[i].getProcessorStats();
 *       for (int j = 0; j < pStats.length; j++) {
 *          System.out.println("   PID: " + pStats[i].getPID();
 *          System.out.println("   cache hits: " + pStats[i].getCacheHits();
 *          System.out.println("   cache misses by URL: " + pStats[i].getCacheMissesByUrl();
 *          System.out.println("   cache misses by cache id: " + pStats[i].getCacheMissesById();
 *          System.out.println("   cache expirations: " + pStats[i].getCacheExpires();
 *          System.out.println("   cache purges: " + pStats[i].getCachePurges();
 *          ESICacheEntryStats[] cStats = pStats.getCacheEntryStats();
 *          for (int k = 0; k < cStats.length; k++) {
 *             System.out.println("      cache id: " + cStats[k].getCacheId());
 *          }
 *       }
 *    }
 */
public class ESIStats
{
   private static final TraceComponent _tc = Tr.register(ESIStats.class,"WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");
   private HashMap        _serverStats = new HashMap();
   private boolean        _gatherEntries = false;

   /**
    * Contructor for ESI (Edge Side Include) statistics which can be gathered from
    * the currently running ESI processors "on the edge" (e.g. the ESI processors
    * which are resident in the webserver plugin.
    */
   public ESIStats ()
   {
   }    

   /**
    * Return the current setting for gathering entries as part of the statistics.
    * @return gather entry setting
    */
   public boolean getGatherEntries()
   {
      return _gatherEntries;
   }

   public void setGatherEntries(boolean gatherEntries)
   {
      _gatherEntries = gatherEntries;
      if (_tc.isDebugEnabled()) Tr.debug(_tc, "setGatherEntries " + gatherEntries);
   }

   /**
    * Gather a snap shot of the statistics from all of the ESI processors.
    * NOTE: This sends a message to all of the ESI processors currently running and
    * gathers the statistics.
    * @return none
    */
   public void gather () throws IOException
   {
      if (_tc.isEntryEnabled()) Tr.entry(_tc, "gather");
      clear();
      int gatherWhat = _gatherEntries ? ESIProcessorStats.GATHER_ALL :
                                        ESIProcessorStats.CACHE_COUNTS;
      ESIProcessorStats[] stats = ESIProcessor.gather(gatherWhat);
      if (_tc.isDebugEnabled()) Tr.debug(_tc, "sorting gathered statistics");
      // Put into hash table by hostname
      for (int idx = 0; idx < stats.length; idx++) {
         String hostName = stats[idx].getHostName();
         ESIServerStats serverStats = (ESIServerStats)_serverStats.get (hostName);
         if (serverStats == null) {
            serverStats = new ESIServerStats (hostName);
            _serverStats.put (hostName,serverStats);
         }
         serverStats.addProcessorStats (stats[idx]);
      }
      if (_tc.isEntryEnabled()) Tr.exit(_tc, "gather");
   }

   /**
    * Return all ESIServerStats objects gathered.
    * @return The ESIServerStats objects resulting from calling gather().
    */
   public ESIServerStats[] getServerStats()
   {
      return (ESIServerStats[]) _serverStats.values().toArray(new ESIServerStats[0]);
   }

   /**
    * Release the references to the ESIServerStats objects.
    * NOTE: No message is sent to the remote ESI processors.
    * @return none
    */
   public void clear ()
   {
      _serverStats.clear();
   }

   /**
    * Resets the counters in all of the ESI processors.
    * NOTE: This sends a "reset counters" message to all of the ESI processors currently
    * running.
    * @return none
    */
   public void resetCounters () throws IOException
   {
      ESIProcessor.resetCounters();
   }

   /**
    * Clear the caches in all of the ESI processors.
    * NOTE: This sends a "clear cache" message to all of the ESI processors currently
    * running.
    * @return none
    */
   public void clearCaches () throws IOException
   {
      ESIProcessor.clearCaches();
   }

}
