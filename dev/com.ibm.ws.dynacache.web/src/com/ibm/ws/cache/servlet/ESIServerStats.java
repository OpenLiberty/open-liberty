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

import java.util.LinkedList;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

/**
 * An ESIServerStats is a logical grouping of ESIProcessorStats objects, grouped
 * by hostname.
 */
public class ESIServerStats
{
   private static final TraceComponent _tc = Tr.register(ESIServerStats.class,"WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");
   private final String      _hostName;
   private final LinkedList  _statList = new LinkedList();

   /**
    * Construct grouping of ESIProcessorStats, grouped by hostname.
    *
    * @param name The hostname on which an ESIProcessor is running.
    * @return none
    */
   public ESIServerStats (String hostName)
   {
      _hostName = hostName;
   }    

   /**
    * Return the name of the ESI server.  This is generally the hostname.
    *
    * @return The ESI server name.
    */
   public String getHostName() {
      return _hostName;
   }

   /**
    * Return all of the currently running ESI processors.
    * @return The list of currently running ESI processors.
    */
   public void addProcessorStats (ESIProcessorStats processorStats)
   {
      _statList.add (processorStats);
      if (_tc.isDebugEnabled()) Tr.debug(_tc, "addProcessorStats " + this);
   }    

   /**
    * Return the ESI processor stats.
    * @return The ESI processor stats.
    */
   public ESIProcessorStats[] getProcessorStats()
   {
      if (_tc.isDebugEnabled()) Tr.debug(_tc, "getProcessorStats");
      ESIProcessorStats[] pStats = new ESIProcessorStats[_statList.size()];
      for (int idx = 0; idx < pStats.length; idx++) {
         pStats[idx] = (ESIProcessorStats)_statList.get(idx);
      }
      return pStats;
   }    

}
