/*
* IBM Confidential
*
* OCO Source Materials
*
* WLP Copyright IBM Corp. 2018
*
* The source code for this program is not published or otherwise divested
* of its trade secrets, irrespective of what has been deposited with the
* U.S. Copyright Office.
*/
package com.ibm.wsspi.anno.targets.cache;

public interface TargetCache_Factory {
	/**
	 * Create options with default values.
	 * 
	 * @return Cache options with default values.
	 */
    TargetCache_Options createOptions();

    /**
     * Answer the current cache options.
     * 
     * @return The current cache options.
     */
    TargetCache_Options getCacheOptions();
    
    /**
     * Set the cache options.  This must be done before
     * any activity which uses the cache.
     * 
     * @param options Options to set as the cache options.
     */
    void setOptions(TargetCache_Options options);
}
