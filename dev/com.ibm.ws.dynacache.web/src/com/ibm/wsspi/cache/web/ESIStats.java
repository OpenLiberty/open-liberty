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
package com.ibm.wsspi.cache.web;

import java.io.IOException;

/**
 * This is the mechanism to provide Edge cache
 * statistics to the CacheMonitor.
 * @ibm-spi 
 */
public class ESIStats {

	private com.ibm.ws.cache.servlet.ESIStats esiStats = null;
	
	public ESIStats (){
		esiStats = new com.ibm.ws.cache.servlet.ESIStats();
	}    

    /**
     * Return the current setting for gathering entries as part of the statistics.
     * 
     * @return gather entry setting
     */
	public boolean getGatherEntries(){
		return esiStats.getGatherEntries();
	}

    /**
     * Set the current setting for gathering entries as part of the statistics.
     * 
     * @param gatherEntries gather entry setting
     */
	public void setGatherEntries(boolean gatherEntries){
		esiStats.setGatherEntries(gatherEntries);
	}

    /**
     * Gather a snap shot of the statistics from all of the ESI processors.
     * NOTE: This sends a message to all of the ESI processors currently running and
     * gathers the statistics.
     */
	public void gather () throws IOException {
		esiStats.gather();
	}

	/**
	 * Return all ESIServerStats objects gathered.
	 * 
	 * @return The ESIServerStats objects resulting from calling gather().
	 */
	public ESIServerStats[] getServerStats(){
		com.ibm.ws.cache.servlet.ESIServerStats[] esiss = esiStats.getServerStats();
		ESIServerStats[] esiServerStats = new ESIServerStats[esiss.length];
		for (int i=0; i< esiss.length; i++){
			esiServerStats[i] = new ESIServerStats(esiss[i]);
		}
		return esiServerStats;
	}

    /**
     * Release the references to the ESIServerStats objects.
     * NOTE: No message is sent to the remote ESI processors.
     * @return none
     */
	public void clear (){
		esiStats.clear();
	}

    /**
	 * Resets the counters in all of the ESI processors.
	 * NOTE: This sends a "reset counters" message to all of the ESI processors currently
	 * running.
	 * @return none
	 */
	public void resetCounters () throws IOException{
		esiStats.resetCounters();
	}

    /**
	 * Clear the caches in all of the ESI processors.
	 * NOTE: This sends a "clear cache" message to all of the ESI processors currently
	 * running.
	 * @return none
	 */
	public void clearCaches () throws IOException{
		esiStats.clearCaches();
	}

	/**
	 * An ESIServerStats is a logical grouping of ESIProcessorStats objects, grouped
	 * by hostname.
	 */
	static public class ESIServerStats{
		
		private com.ibm.ws.cache.servlet.ESIServerStats esiServerStats = null;
		
		public ESIServerStats(com.ibm.ws.cache.servlet.ESIServerStats esiss){
			esiServerStats = esiss;
		}
		
		public String getHostName() {
			return esiServerStats.getHostName();
		}

		public void addProcessorStats (com.ibm.ws.cache.servlet.ESIProcessorStats processorStats){
			esiServerStats.addProcessorStats(processorStats);
		}    
		   
		public ESIProcessorStats[] getProcessorStats(){
			com.ibm.ws.cache.servlet.ESIProcessorStats[] esips = esiServerStats.getProcessorStats();
			ESIProcessorStats[] esiProcessorStats = new ESIProcessorStats[esips.length];
			for (int i=0; i< esips.length; i++){
				esiProcessorStats[i] = new ESIProcessorStats(esips[i]);
			}
			return esiProcessorStats;   
		} 
		
		static public class ESIProcessorStats{
			
			private com.ibm.ws.cache.servlet.ESIProcessorStats esiProcessorStats = null;
			
			public ESIProcessorStats(com.ibm.ws.cache.servlet.ESIProcessorStats esips){
				esiProcessorStats = esips;
			}
			
			public String getHostName() {
				return esiProcessorStats.getHostName();
			}

			   
			public int getPID() {
				return esiProcessorStats.getPID();
			}
			  
			public int getCacheHits() {
				return esiProcessorStats.getCacheHits();
			}
			 
			public void setCacheHits(int hits) {
				esiProcessorStats.setCacheHits(hits);
			}
			  
			public int getCacheMissesByUrl() {
				return esiProcessorStats.getCacheMissesByUrl();
			}
			   
			public void setCacheMissesByUrl(int misses) {
				esiProcessorStats.setCacheMissesByUrl(misses);
			}
			
			public int getCacheMissesById() {
				return esiProcessorStats.getCacheMissesById();  
			}
			   
			public void setCacheMissesById(int misses) {
				esiProcessorStats.setCacheMissesById(misses);
			}

			   
			public int getCacheExpires() {
				return esiProcessorStats.getCacheExpires();
			}
			   
			public void setCacheExpires(int expires) {
				esiProcessorStats.setCacheExpires(expires);
			}


			public int getCachePurges() {
				return esiProcessorStats.getCachePurges();
			}

			public void setCachePurges(int purges) {
				esiProcessorStats.setCachePurges(purges); 
			}

			public void addCacheEntryStats (com.ibm.ws.cache.servlet.ESICacheEntryStats cacheEntry) {
				esiProcessorStats.addCacheEntryStats(cacheEntry);
			}
			 
			public ESICacheEntryStats[] getCacheEntryStats() {
				com.ibm.ws.cache.servlet.ESICacheEntryStats[] esices = esiProcessorStats.getCacheEntryStats();
				ESICacheEntryStats[] esiCacheEntryStats = new ESICacheEntryStats[esices.length];
				for (int i=0; i< esices.length; i++){
					esiCacheEntryStats[i] = new ESICacheEntryStats(esices[i]);
				}
				return esiCacheEntryStats;   
			}
			 
			public void handle () throws IOException{
				esiProcessorStats.handle();
			}
			
			static public class ESICacheEntryStats{
				
				private com.ibm.ws.cache.servlet.ESICacheEntryStats esiCacheEntryStats = null;
				
				public ESICacheEntryStats(com.ibm.ws.cache.servlet.ESICacheEntryStats esices){
					esiCacheEntryStats = esices;
				}
				
				public String getCacheId(){
				     return esiCacheEntryStats.getCacheId();
				}
				  
				public void setCacheId (String cacheId){
					esiCacheEntryStats.setCacheId(cacheId);
				}

				public String toString(){
				     return esiCacheEntryStats.toString();
				}
			}
			
		}
		
	}
	
}
