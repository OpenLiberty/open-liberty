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
package com.ibm.websphere.cache;

import java.util.Map;
import java.util.Set;

/**
 * This class provides applications with an extended java.util.Map interface
 * to access the WebSphere Dynamic Cache, allowing inspection and manipulation of the
 * cache.   The cache does not have any authorization checking done before allowing
 * access to its contents, so care should be taken on the type of data that is placed
 * in the cache.   The default WebSphere Dynamic Cache instance is created when
 * the cache is enabled in the administrative console and is bound into the global
 * JNDI namespace with the name "services/cache/distributedmap".
 *
 * Additional cache instances can be created using a properties file
 * cacheinstances.properties with the following format:
 * <xmp>
 *    cache.instance.0=/services/cache/instance_one
 *    cache.instance.0.cacheSize=1000
 *    cache.instance.0.enableDiskOffload=true
 *    cache.instance.0.diskOffloadLocation=${WAS_INSTALL_ROOT}/temp
 *    cache.instance.1=/services/cache/instance_two
 *    cache.instance.1.cacheSize=1500
 *    cache.instance.1.enableDiskOffload=true
 *    cache.instance.1.diskOffloadLocation=C:/disk
 * </xmp>
 *
 * The distributedmap.properties file must be located in either you application
 * server or application's classpath.
 * The first entry in the properties file (cache.instance.0) specifies the JNDI
 * name for the cache instance in the global namespace.   You could then lookup
 * the cache instance using the following code:
 * <xmp>
 *     InitialContext ic = new InitialContext();
 *     DistributedMap dm =(DistributedMap)ic.lookup("services/cache/instance_one");
 * </xmp>
 *
 * Alternatively, you can define a resource-ref for in cache in your module's
 * deployement descriptor and then lookup the cache using the java:comp namespace.
 *
 * <xmp>
 *   <resource-ref>
 *     <res-ref-name>dmap/LayoutCache</res-ref-name>
 *     <res-type>com.ibm.websphere.cache.DistributedMap</res-type>
 *     <res-auth>Container</res-auth>
 *     <res-sharing-scope>Shareable</res-sharing-scope>
 *  </resource-ref>
 * </xmp>
 *
 * An example of looking up the resource-ref:
 *
 * <xmp>
 *     InitialContext ic = new InitialContext();
 *     DistributedMap dm =(DistributedMap)ic.lookup("java:comp/env/dmap/LayoutCache");
 * </xmp>
 *
 *
 * @ibm-api
 */
public interface DistributedMap extends Map {

   /**
	* setSharingPolicy - sets the sharing policy for DistributedMap.
	*
	* @param sharingPolicy policy to set. Default is EntryInfo.NOT_SHARED
	* @see #getSharingPolicy()
    * @ibm-api
	*/
   public void setSharingPolicy(int sharingPolicy);

   /**
	* getSharingPolicy - gets the sharing policy for DistributedMap.
	*
	* @return returns the current sharing policy of DistributedMap.
	* @see #setSharingPolicy(int)
    * @ibm-api
	*/
   public int getSharingPolicy();

   /**
    * setDRSBootstrap - Enables or disbales DRS bootstrap support
    *
    * @param drsBootstrap - true (default) to enable DRS bootstrap support, 
    * or false to ignore DRS bootstrap messages for this cache instance
    * @see #isDRSBootstrapEnabled()
    * @ibm-api
    */
   public void setDRSBootstrap(boolean drsBootstrap);

   /**
    * isDRSBootstrapEnabled - check whether DRS bootstrap for DistributedMap is enabled or not.
    *
    * @return returns the current DRS bootstrap of DistributedMap. True means enabled
    * @see #setDRSBootstrap(boolean)
    * @ibm-api
    */
   public boolean isDRSBootstrapEnabled();  // 390766

   /**
    * Set the global time-to-live this this map.
    *
	* @param timeToLive the time in seconds that cache entries should remain
	*                   in the cache.  The default value is -1 and means the entry 
	*                   does not time out.     
    * @ibm-api
    */
   
   public void setTimeToLive(int timeToLive);

   /**
    * Sets the global priority for this map..
    *
    * @param priority the global priority value for the cache entries.  entries
	*                 with higher priority will remain in the cache longer
	*                 than those with a lower priority in the case of cache
	*                 overflow.  Valid priorities are 1 through 16 with 1 being
	*                 the default value.  1 is the lowest priority and 16 is the highest.
    * @ibm-api
    */
   public void setPriority(int priority);

   /**
	* Returns the value to which this map maps the specified key.  Returns
	* <tt>null</tt> if the map contains no mapping for this key.  A return
	* value of <tt>null</tt> does not <i>necessarily</i> indicate that the
	* map contains no mapping for the key; it's also possible that the map
	* explicitly maps the key to <tt>null</tt>.  The <tt>containsKey</tt>
	* operation may be used to distinguish these two cases.
	*
	* @param key key whose associated value is to be returned.
	* @return the value to which this map maps the specified key, or
	*          <tt>null</tt> if the map contains no mapping for this key.
	*
	* @throws ClassCastException if the key is not of an inappropriate type for
	*        this map. (Currently supports only String)
	* @throws NullPointerException key is <tt>null</tt> and this map does not
	*        not permit <tt>null</tt> keys.
	*
	* @see #containsKey(Object)
    * @ibm-api
	*/
   Object get(Object key);

   /**
	* Associates the specified value with the specified key in this map
	* (optional operation).  If the map previously contained a mapping for
	* this key, the old value is replaced.   This method will use optional
	* metadata looked up from the cache configuration file.  If no meta data
	* is found, the entrie is cached with an infinite timeout and the cache's
	* default priority.
	*
	* Metadata found in the cache configuration is looked up via class name
	* and includes priority, timeout, and dependency ids.
	*
	* @param key key with which the specified value is to be associated.
	* @param value value to be associated with the specified key.
	* @return previous value associated with specified key, or <tt>null</tt>
	*          if there was no mapping for key.  A <tt>null</tt> return can
	*          also indicate that the map previously associated <tt>null</tt>
	*          with the specified key, if the implementation supports
	*          <tt>null</tt> values.
	*
	* @throws UnsupportedOperationException if the <tt>put</tt> operation is
	*             not supported by this map.
	* @throws ClassCastException if the class of the specified key or value
	*             prevents it from being stored in this map.
	* @throws IllegalArgumentException if some aspect of this key or value
	*             prevents it from being stored in this map.
	* @throws NullPointerException this map does not permit <tt>null</tt>
	*            keys or values, and the specified key or value is
	*            <tt>null</tt>.
    * @ibm-api
	*/
   Object put(Object key, Object value);

   /**
	* Associates the specified value with the specified key in this map
	* (optional operation).  If the map previously contained a mapping for
	* this key, the old value is replaced.
	*
	* @param key key with which the specified value is to be associated.
	* @param value value to be associated with the specified key.
	* @param priority the priority value for the cache entry.  entries
	*                 with higher priority will remain in the cache longer
	*                 than those with a lower priority in the case of cache
	*                 overflow.  Valid priorities are 1 through 16 with 1 being
	*                 the default value.  1 is the lowest priority and 16 is the highest.
	* @param timeToLive the time in seconds that the cache entry should remain
	*                   in the cache.  The default value is -1 and means the entry 
	*                   does not time out.  
	* @param sharingPolicy how the cache entry should be shared in a cluster.
	*                      values are EntryInfo.NOT_SHARED, EntryInfo.SHARED_PUSH,
	*                      and EntryInfo.SHARED_PUSH_PULL.
	* @param dependencyIds an optional set of dependency ids to associate with
	*                      the cache entry
	* @return previous value associated with specified key, or <tt>null</tt>
	*          if there was no mapping for key.  A <tt>null</tt> return can
	*          also indicate that the map previously associated <tt>null</tt>
	*          with the specified key, if the implementation supports
	*          <tt>null</tt> values.
	*
	* @throws UnsupportedOperationException if the <tt>put</tt> operation is
	*             not supported by this map.
	* @throws ClassCastException if the class of the specified key or value
	*             prevents it from being stored in this map.
	* @throws IllegalArgumentException if some aspect of this key or value
	*             prevents it from being stored in this map.
	* @throws NullPointerException this map does not permit <tt>null</tt>
	*            keys or values, and the specified key or value is
	*            <tt>null</tt>.
    * @ibm-api
	*/
   Object put(Object key, Object value, int priority, int timeToLive, int sharingPolicy, Object dependencyIds[]);

   /**
    * Associates the specified value with the specified key in this map
    * (optional operation).  If the map previously contained a mapping for
    * this key, the old value is replaced.
    *
    * @param key key with which the specified value is to be associated.
    * @param value value to be associated with the specified key.
    * @param priority the priority value for the cache entry.  entries
    *                 with higher priority will remain in the cache longer
    *                 than those with a lower priority in the case of cache
    *                 overflow.  Valid priorities are 1 through 16 with 1 being
	*                 the default value.  1 is the lowest priority and 16 is the highest.
    * @param timeToLive the time in seconds that the cache entry should remain    
    *                  	in the cache.  The default value is -1 and means the entry 
	*                   does not time out. 
    * @param inactivityTime the time in seconds that the cache entry should remain 
    *			in the cache if not accessed. This is reset once an entry is 
    *			accessed.
    * @param sharingPolicy how the cache entry should be shared in a cluster.
    *                      values are EntryInfo.NOT_SHARED, EntryInfo.SHARED_PUSH,
    *                      EntryInfo.SHARED_PULL, and EntryInfo.SHARED_PUSH_PULL
    * @param dependencyIds an optional set of dependency ids to associate with
    *                      the cache entry
    * @return previous value associated with specified key, or <tt>null</tt>
    *          if there was no mapping for key.  A <tt>null</tt> return can
    *          also indicate that the map previously associated <tt>null</tt>
    *          with the specified key, if the implementation supports
    *          <tt>null</tt> values.
    *
    * @throws UnsupportedOperationException if the <tt>put</tt> operation is
    *             not supported by this map.
    * @throws ClassCastException if the class of the specified key or value
    *             prevents it from being stored in this map.
    * @throws IllegalArgumentException if some aspect of this key or value
    *             prevents it from being stored in this map.
    * @throws NullPointerException this map does not permit <tt>null</tt>
    *            keys or values, and the specified key or value is
    *            <tt>null</tt>.
   *@ibm-api 
    */
   Object put(Object key, Object value, int priority, int timeToLive, int inactivityTime, int sharingPolicy, Object dependencyIds[]);

   /**
	* invalidate - invalidates the given key.  If the key is
	* for a specific cache entry, then only that object is
	* invalidated.  If the key is for a dependency id, then
	* all objects that share that dependency id will be
	* invalidated.
	* @param key the key which will be invalidated
	* @see #remove(Object key)
    * @ibm-api
	*/
   void invalidate(Object key);

   /**
	* invalidate - invalidates the given key.  If the key is
	* for a specific cache entry, then only that object is
	* invalidated.  If the key is for a dependency id, then
	* all objects that share that dependency id will be
	* invalidated.
	* @param key the key which will be invalidated
	* @param wait if true, then the method will not complete until the invalidation
	*             has occured.  if false, then the invalidation will occur in batch mode
	* @see #remove(Object key)
    * @ibm-api
	*/
   public void invalidate(Object key, boolean wait);

   /**
	* enableListener - enable or disable the invalidation and change listener support.
	* You must call enableListener(true) before calling addInvalidationListner() or addChangeListener().
	*
	* @param enable - true to enable support for invalidation and change listeners
	*                 or false to disable support for invalidation and change listeners
	* @return boolean "true" means listener support was successfully enabled or disabled.
	*                 "false" means this DistributedMap is configurated to use the listener's J2EE context for
	*    	      event notification and the callback registration failed.  In this case, the caller's thread
	*    	      context will be used for event notification.
	*
    * @ibm-api
	*/
   public boolean enableListener(boolean enable);

   /**
	* addInvalidationListener - adds an invalidation listener for this DistributeMap.
	*
	* @param listener the invalidation listener object
	* @return boolean "true" means the invalidation listener was successfully added.
	*                 "false" means either the passed listener object is null or listener support is not enable.
	* @see #removeInvalidationListener(com.ibm.websphere.cache.InvalidationListener)
    * @ibm-api
	*/
   public boolean addInvalidationListener(InvalidationListener listener);

   /**
	* removeInvalidationListener - removes an invalidation listener for this DistributedMap.
	*
	* @param listener the invalidation listener object
	* @return boolean "true" means the invalidation listener was successfully removed.
	*                 "false" means either passed listener object is null or listener support is not enable.
	* @see #addInvalidationListener(com.ibm.websphere.cache.InvalidationListener)
    * @ibm-api
	*/
   public boolean removeInvalidationListener(InvalidationListener listener);

   /**
	* addChangeListener - adds a change listener for this DistributedMap.
	*
	* @param listener the change listener object
	* @return boolean "true" means the change listener was successfully added.
	*                 "false" means either the passed listener object is null or listener support is not enable.
	* @see #removeChangeListener(com.ibm.websphere.cache.ChangeListener)
    * @ibm-api
	*/
   public boolean addChangeListener(ChangeListener listener);

   /**
	* removeChangeListener - removes a change listener for this DistributedMap.
	*
	* @param listener the change listener object
	* @return boolean "true" means the change listener was successfully removed.
	*                 "false" means either passed listener object is null or listener support is not enable.
	* @see #addChangeListener(com.ibm.websphere.cache.ChangeListener)
    * @ibm-api
	*/
   public boolean removeChangeListener(ChangeListener listener);

   // todo: v6.1
   ///**
   // * This method will release all resources associated
   // * with this map.  Once a map is destroyed you can
   // * no longer use it.
   // */
   //public boolean destroy();

   /**
    * Adds an alias for the given key in the cache's mapping table. If the alias is already
    * associated with another key, it will be changed to associate with the new key.
        *
    * @param key the key assoicated with alias
    * @param aliasArray the alias to use for lookups
    * @throws IllegalArgumentException if the key is not in the cache's mapping table.
    * @ibm-api
    */
   public void addAlias(Object key, Object[] aliasArray);

   /**
    * Removes an alias from the cache's mapping table.
    * @param alias the alias to move out of the cache's mapping table
    * @ibm-api
    */
   public void removeAlias(Object alias);

   /**
    * Returns the total number of key-value mappings. Returns size of memory map plus disk map if includeDiskCache is
    * true. Returns size of memory map size if includeDiskCache is false.
    * @param includeDiskCache true to get the size of the memory and disk maps; false to get the size of memory map.
    * @return the number of key-value mappings in this map.
    * @ibm-api
    */
   public int size(boolean includeDiskCache);

   /**
    * Returns true if this map contains no key-value mappings. Checks both memory and disk maps if includeDiskCache
    * is true. Check only memory cache if includeDiskCache is false.
    * @param includeDiskCache true to check the memory and disk maps; false to check the memory map.
    * @return true if this map contains no key-value mappings.
    * @ibm-api
    */
   public boolean isEmpty(boolean includeDiskCache);

   /**
    * Returns true if this map contains mapping for the specified key. Checks both memory and disk map if includeDiskCache
    * is true. Check only memory map if includeDiskCache is false.
    * @param key whose presence in this map is to be tested.
    * @param includeDiskCache true to check the specified key contained in the memory or disk maps; false to check the specified key contained in the memory map.
    * @return true if this map contains a mapping for the specified key.
    * @ibm-api
    */
   public boolean containsKey(Object key, boolean includeDiskCache);

   /**
    * Returns a set view of the keys contained in this map. Returns all the keys in both memory map and disk map if includeDiskCache is true.
    * Return only keys in memory map if includeDiskCache is false.
    * Warning: If this method is used with includeDiskCache set to true, all the keys on disk are read into memory and that might consume a lot of memory depending
    * on the size of disk map.
    * @param includeDiskCache true to get keys contained in the memory and disk maps; false to get keys contained in the memory map.
    * @return a set view of the keys contained in this map.
    * @ibm-api
    */
   public Set keySet(boolean includeDiskCache);
}
