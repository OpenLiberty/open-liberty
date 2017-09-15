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
package com.ibm.ws.cache;

import java.io.Serializable;
import java.util.Enumeration;

import com.ibm.ws.cache.intf.DCacheConfig;
import com.ibm.ws.util.ObjectPool;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * All of the caching metadata is set via a EntryInfo object,
 * which is a simple struct object that contains the metadata
 * for all caching options.  For a cache entry
 * that is being cached, all of these options can be set by
 * a CacheEntry.
 * <p>
 * The following is a summary list of the caching metadata for a CacheEntry:
 * <ul>
 *     <li>The template.
 *     <li>The id.
 *     <li>The priority option.
 *     <li>The timeLimit or expirationTime options.
 *     <li>The dataIds option.
 * </ul>
 * @ibm-private-in-use
 */
public class EntryInfo implements com.ibm.websphere.cache.EntryInfo, Serializable, Cloneable {
   private static final long serialVersionUID = 1342185474L;
   private static TraceComponent tc = Tr.register(EntryInfo.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

   /**
	* The entry is kept local to the JVM that executed the entry's JSP
	* or command instead of shared across all JVMs.
	* This option is useful when there is affinity between a client
	* and web application server and the data is only used by that client
	* (e.g., a shopping cart).
        * @ibm-private-in-use
	*/
   public static final int NOT_SHARED = 1;

   /**
	* The entry is shared across multiple JVMs.
	* The entry is pushed to all JVMs after its JSP or command is
	* executed instead of waiting for the JVMs to pull it.
	* Pushing these entries is delayed for a short time
	* (which is configurable) to exploit the efficiency of batching
	* several entries in one message.
	* This option is useful when the entry is very heavily used by
	* all clients (e.g., a popular product display).
        * @ibm-private-in-use
	*/
   public static final int SHARED_PUSH = 2;

   /**
    * The entry is shared across multiple JVMs.
    * Other JVMs get it by pulling it when needed and then storing
    * it in its local cache for further requests.
    * This option is useful when the entry is shared by all clients
    * but is not accessed often enough between invalidations to
    * warrant pushing it to other (e.g., a not-so-popular product display).
    * 
    * @deprecated Share type PULL should not be used in new code development.
    *             Use share type PUSH_PULL instead.  Share type PULL, if
    *             present in existing code, will function like share type
    *             PUSH_PULL.
    *
    * @ibm-private-in-use
    */
   public static final int SHARED_PULL = 3;

   /**
	* The entry is shared across multiple JVMs.
        * IDs for new entries are pushed to all JVMs once it is 
        * cached on one of them. Only requests from other servers 
        * in the cluster entries for IDs previously broadcast receive 
        * the entry when needed. The dynamic cache always sends out 
        * cache entry invalidations to all JVMs. 
        * @ibm-private-in-use
   */
   public static final int SHARED_PUSH_PULL = 4;
   
   protected static final boolean SET = true;
   protected static final boolean UNSET = false;

   private boolean expirationTimeFlag = UNSET;
   private boolean inactivityFlag = UNSET; // CPF-Inactivity
   private boolean idFlag = UNSET;
   private boolean priorityFlag = UNSET;
   private boolean sharingPolicyFlag = UNSET;
   private boolean lock = UNSET;

   /**
	* This is the identifier of the cache entry.
	* It must be unique within the scope of the cache.
	* which is the WebSphere server group for the application.
	*/
   protected Object id = null;

   protected Object userMetaData = null;
   
   /**
	* This is the maximum time interval in seconds that the
	* entry should be cached.
	* A negative value implies that there is no time limit.
	*/
   protected int timeLimit = -1;

   /**
    * This is the minimum time interval in seconds that the
    * entry should be cached.  This timer is reset if
    * the cache entry is read.
    * A negative value implies that there is no time limit.
    */
   protected int inactivity = -1; // CPF-Inactivity

   /**
	* This is the absolute time when the entry should be expired.
	* This value is the output of the output of Date.getTime method.
	* A negative value implies that there is no time limit.
	* <p>
	* The command writer can set either a time limit or an expiration
	* time, but not both; an exception is thrown if both are set.
	* The cache will calculate an expiration time based on a time limit.
	*/
   protected long expirationTime = -1;

   /**
	* This is the time when the entry becomes invalid. It is used by VBC. 
	* Value set to -1 means not used 
	*/
  protected long validatorExpirationTime = -1;

   /**
	* A larger priority gives a CacheEntry a longer time in the
	* cache when it is not being used.
	* The value of priority should be based on the ratio of
	* the cost of computing the CacheEntry to the cost of
	* the memory in the cache (the size of the CacheEntry).
	* The default is used when this value is not set.
	*/
   protected int priority = -1;


   /**
	* The sharingPolicy determines how the entry is handled in a
	* distributed environment (ie, when there are multiple JVMs).
	* The three possibilities are NOT_SHARED, SHARED_PUSH_PULL and SHARED_PUSH.
	* The default is NOT_SHARED.
	*/
   protected int sharingPolicy = NOT_SHARED;

   /**
   * The persist to disk property determines if the entry
   * gets sent to the disk when overflow, replication or
   *server stopping occur. The default value is true which
   *means that the entry stays in the memory only.
   */
   protected boolean persistToDisk = true;    //@memOnly

   /**
	* This is the set of templates (eg, JSP, Servlet class, Command class)
	* that this entry depends on.
	* Its values are Strings.
	* If the Dynacache.invalidateByTemplate is called with one of these,
	* the entry will be invalidated.
        * @ibm-private-in-use
	*/
   public ValueSet templates = new ValueSet(4);

   private String template = null;

   /**
	* This is the list of CacheEntry data ids that make this entry
	* invalid when they become invalid.
	* Its elements are Strings.  They are the identifiers used in
	* the Cache.invalidateById/s methods.
	* It must be unique within the same scope as the CacheEntry id.
	* These data ids identify the underlying dynamic content
	* (i.e., the raw data).
	* When a piece of data is used in only one CacheEntry,
	* the data id of the data can be the same as the CacheEntry id.
	* When a piece of data is used in multiple fragments,
	* its data id would be different from any of the CacheEntry ids.
        * @ibm-private-in-use
	*/
   public ValueSet dataIds = new ValueSet(4);
   /**
        * This is a list of alias cache entries mapping to this cache entry.
        * @ibm-private-in-use
   */
   public ValueSet aliasList = new ValueSet(4);

   transient private EntryInfoPool entryInfoPool = null;

   // set by JAXRPCCache.setValue(JAXRPCEntryInfo, Object) to CACHE_TYPE_JAXRPC
   public int cacheType = CacheEntry.CACHE_TYPE_DEFAULT;  
   
	/**
	 * This is the id of the external cache group for this FragmentInfo.
	 * It is used to lookup the ExternalCacheAdaptor class name and
	 * the ip addresses for the external caches that this fragment should
	 * be written to.
	 */
	public String externalCacheGroupId = null;
   
   /**
	* resets this EntryInfo for reuse
        * @ibm-private-in-use
   */
   public void reset() {
	  expirationTimeFlag = UNSET;
      inactivityFlag = UNSET; 
	  idFlag = UNSET;
	  priorityFlag = UNSET;
	  sharingPolicyFlag = UNSET;
	  lock = UNSET;
	  id = null;
	  timeLimit = -1;
      inactivity = -1;
   	  expirationTime = -1;
   	  validatorExpirationTime = -1;
	  priority = -1;
	  sharingPolicy = NOT_SHARED;
	  persistToDisk = true;   
	  templates.clear();
	  template = null;
	  dataIds.clear();
	  aliasList.clear();
	  userMetaData = null;
      entryInfoPool = null;
      cacheType = CacheEntry.CACHE_TYPE_DEFAULT;
      externalCacheGroupId = null;
  }

   /**
	* This gets the id variable.
	*
	* @return The cache id.
        * @ibm-private-in-use
   */
   public String getId() {
	   if (id != null) {
		   return id.toString();
	   }
	   return null;
   }

   /**
	* This gets the id variable.
	*
	* @return The cache id.
        * @ibm-private-in-use
   */
   public Object getIdObject() {
	  return id;
   }

   /**
	* This sets the id variable.
	*
	* @param id The cache id.
        * @ibm-private-in-use
	*/
   public void setId(Object id) {
	  if (lock == SET) {
		 throw new IllegalStateException("EntryInfo is locked");
	  }
	  /*if (idFlag == SET) {
		 if (tc.isDebugEnabled()) Tr.debug(tc, "Illegal State: tried to set id to "+id+
											   ", but id was already set to "+this.id  );
		  throw new IllegalStateException("id was already set");
	  } */
	  idFlag = SET;
	  this.id = id;
	  if (tc.isDebugEnabled())
		 Tr.debug(tc, "set id=" + id);
   }

   /**
	* This sets the id variable.
	*
	* @param id The cache id.
        * @ibm-private-in-use
	*/
   public void setId(String id) {
	   setId((Object)id);
   }


   /**
	* This checks to see if the cache id has been set.
	* @return True indicates that it has been set.
	*/
   /* package */
   boolean wasIdSet() {
	  return (idFlag == SET);
   }

   /**
	* This checks to see if the batch is enabled
	* @return True indicates that the batch is enabled.
    * @ibm-private-in-use
    * @deprecated The updates for Push or Push-Pull sharing policies are 
    *             always done in an asynchronous batch mode. It always
    *             returns true. 
	*/
   public boolean isBatchEnabled() {
	  return true;
   }

   /**
	* This sets the value that the batch is enabled
	* @param flag true indicates that the batch is enabled.
    * @ibm-private-in-use
    * @deprecated The updates for Push or Push-Pull sharing policies are 
    *             always done in an asynchronous batch mode. Calling 
    *             setBatchEnabled(false) has no effect on cache replication. 
	*/
   public void setBatchEnabled(boolean flag) {
   }

   /**
	* This gets the sharing policy in the sharingPolicy variable.
	* Included for forward compatibility with distributed caches.
	*
	* @return The sharing policy.
        * @ibm-private-in-use
	*/
   public int getSharingPolicy() {
	  return sharingPolicy;
   }
	/**
	* This gets the persistToDisk variable.
	*
	* @return The persistToDisk.
        * @ibm-private-in-use
	*/

   public boolean getPersistToDisk() {      
	  return persistToDisk;
   }

   /**
	* This sets the sharing policy in the sharingPolicy variable.
	* Included for forward compatibility with distributed caches.
	*
	* @param The new sharing policy.
        * @ibm-private-in-use
	*/
   public void setSharingPolicy(int sharingPolicy) {
	  if (lock == SET) {
		 throw new IllegalStateException("EntryInfo is locked");
	  }
	  sharingPolicyFlag = SET;
	  this.sharingPolicy = sharingPolicy;
	  if ((sharingPolicy != NOT_SHARED) && (sharingPolicy != SHARED_PUSH) && (sharingPolicy != SHARED_PULL) && (sharingPolicy != SHARED_PUSH_PULL)) {
		 throw new IllegalArgumentException("Illegal sharing policy: " + sharingPolicy);
	  }
   }

	/**
	* This assigns value to the persistToDisk variable.It gets set by the cacheProcessor.
	*
	* @param  persistToDisk.
        * @ibm-private-in-use
	*/

   public void setPersistToDisk(boolean persistToDisk) {      //@memOnly
	  if (lock == SET) {
		 throw new IllegalStateException("EntryInfo is locked");
	  }
	  //persistToDisk = SET; ??? todo - need this ???
	  this.persistToDisk = persistToDisk;
	  }                                                    //@memOnly

   /**
	* This checks to see if the sharing policy has been set.
	* Included for forward compatibility with distributed caches.
	* @return True indicates that it has been set.
	*/
   /* package */
   boolean wasSharingPolicySet() {
	  return (sharingPolicyFlag == SET);
   }

   /**
	* This determines whether the sharingPolicy is NOT_SHARED.
	* Included for forward compatibility with distributed caches.
	*
	* @return True indicates that the sharingPolicy is NOT_SHARED.
        * @ibm-private-in-use
	*/
   public boolean isNotShared() {
	  return (sharingPolicy == NOT_SHARED);
   }

   /**
	* This determines whether the sharingPolicy is SHARED_PUSH or SHARED_PUSH_PULL.
	* Included for forward compatibility with distributed caches.
	*
	* @return True indicates that the sharingPolicy is SHARED_PUSH or SHARED_PUSH_PULL.
        * @ibm-private-in-use
	*/
   public boolean isSharedPush() {
	  return (sharingPolicy == SHARED_PUSH || sharingPolicy == SHARED_PUSH_PULL);
   }

   /**
	* This determines whether the sharingPolicy is SHARED_PULL or SHARED_PUSH_PULL.
	* Included for forward compatibility with distributed caches.
	*
	* @return True indicates that the sharingPolicy is SHARED_PULL or SHARED_PUSH_PULL.
        * @ibm-private-in-use
	*/
   public boolean isSharedPull() {
	  return (sharingPolicy == SHARED_PULL || sharingPolicy == SHARED_PUSH_PULL);
   }

   /**
	* This gets the time limit on this cache entry.
	*
	* @param The time limit.
        * @ibm-private-in-use
	*/
   public int getTimeLimit() {
	  return timeLimit;
   }

   /**
	* This sets the time limit in the timeLimit variable. Once an entry is cached,
	* it will remain in the cache for this many seconds
	*
	* @param timeLimit This time limit.
        * @ibm-private-in-use
	*/
   public void setTimeLimit(int timeLimit) {
	  if (lock == SET) {
		 throw new IllegalStateException("EntryInfo is locked");
	  }
	  expirationTimeFlag = SET;
	  this.timeLimit = timeLimit;
	  if (timeLimit > 0) {
         long ttlmsec = ((long)timeLimit) * 1000;  
		 expirationTime = ttlmsec + System.currentTimeMillis();
	  }
   }
   /**
    * This gets the inactivity timer for this cache entry.
    * 
    * @return the inactivity timer for this cache entry.
    * @ibm-private-in-use
    */
   public int getInactivity() { // CPF-Inactivity
       if (com.ibm.ws.cache.TimeLimitDaemon.UNIT_TEST_INACTIVITY) {
         System.out.println("EntryInfo.getInactivity() "+inactivity);
       }
      return inactivity;
   }

   /**
    * This sets the inactivity timer variable. Once an entry is cached,
    * it will remain in the cache for this many seconds if not accessed.
    *
    * @param inactivity This inactivity timer.
    * @ibm-private-in-use
    */
   public void setInactivity(int inactivity) { // CPF-Inactivity
      if (lock == SET) {
         throw new IllegalStateException("EntryInfo is locked");
      }
      inactivityFlag = SET;
      this.inactivity = inactivity; // Seconds
      if (com.ibm.ws.cache.TimeLimitDaemon.UNIT_TEST_INACTIVITY) {
        System.out.println("EntryInfo.setInactivity() "+inactivity);
      }
   }

   /**
	* This gets the expiration time from the expirationTime variable.
	*
	* @return The expiration time.
        * @ibm-private-in-use
	*/
   public long getExpirationTime() {
	  return expirationTime;
   }

   /**
	* This sets the expirationTime variable.
	*
	* @param The new expiration time.
        * @ibm-private-in-use
	*/
   public void setExpirationTime(long expirationTime) {
	  if (lock == SET) {
		 throw new IllegalStateException("EntryInfo is locked");
	  }
	  expirationTimeFlag = SET;
	  this.expirationTime = expirationTime;
	  this.timeLimit = (int) ((expirationTime - System.currentTimeMillis()) / 1000L);
   }

   /**
	* This gets the validator expiration time from the validatorExpirationTime variable.
	*
	* @return The validator expiration time.
    * @ibm-private-in-use
	*/
  public long getValidatorExpirationTime() {
	  return validatorExpirationTime;
  }

  /**
	* This sets the validatorExpirationTime variable.
	*
	* @param The new validator expiration time.
    * @ibm-private-in-use
	*/
  public void setValidatorExpirationTime(long validatorExpirationTime) {
	  if (lock == SET) {
		 throw new IllegalStateException("EntryInfo is locked");
	  }
	  this.validatorExpirationTime = validatorExpirationTime;
  }

   /**
	* This gets the priority in the priority variable.
	*
	* @return The priority.
        * @ibm-private-in-use
	*/
   public int getPriority() {
	  return priority;
   }

   /**
	* This assigns the new priority to the priority variable.
	*
	* @param priority The new priority.
        * @ibm-private-in-use
	*/
   public void setPriority(int priority) {
	  if (lock == SET) {
		 throw new IllegalStateException("EntryInfo is locked");
	  }
	  priorityFlag = SET;
	  this.priority = priority;
   }

   /**
	* This checks to see if the priority has been set.
	* @return True indicates that it has been set.
        * @ibm-private-in-use
	*/
   public boolean wasPrioritySet() {
	  return (priorityFlag == SET);
   }

   /**
	* This gets the templates in the templates variable.
	*
	* @return An Enumeration of the template names.
        * @ibm-private-in-use
	*/
   public Enumeration getTemplates() {
	  return templates.elements();
   }

   /**
	* This gets on of the templates for this EntryInfo.
	*
	* @return The first template set on this EntryInfo.
        * @ibm-private-in-use
	*/
   public String getTemplate() {
	  return (String) templates.getOne();
   }

   /**
	* This adds a template name to the templates variable.
	*
	* @param template The new template name.
        * @ibm-private-in-use
	*/
   public void addTemplate(String template) {
	  if (lock == SET) {
		 throw new IllegalStateException("EntryInfo is locked");
	  }
	  if (template != null && !template.equals("")) {
		  templates.add(template);
	  }
   }

   /**
	* This gets the data ids from the dataIds variable.
	*
	* @return The Enumeration of data ids.
        * @ibm-private-in-use
	*/
   public Enumeration getDataIds() {
	  return dataIds.elements();
   }

   /**
	* This unions a new data id into the dataIds variable.
	*
	* @param dataId The new data id.
        * @ibm-private-in-use
	*/
   public void addDataId(String dataId) {
	   addDataId((Object)dataId);
   }

   /**
	* This unions a new data id into the dataIds variable.
	*
	* @param dataId The new data id.
        * @ibm-private-in-use
	*/
   public void addDataId(Object dataId) {
	  if (lock == SET) {
		 throw new IllegalStateException("EntryInfo is locked");
	  }
	  if (dataId != null && !dataId.equals("")) {
		  dataIds.add(dataId);
	  }
   }

   /**
	* This unions the dependencies (in the form of cache ids, data ids
	* and template names) into the dataIds and templates variables.
	*
	* @param entryInfo The EntryInfo containing the new dependencies.
        * @ibm-private-in-use
	*/
   public void unionDependencies(com.ibm.websphere.cache.EntryInfo eInfo) {
	  EntryInfo entryInfo = (EntryInfo) eInfo;
	  if (entryInfo == this) {
		 return;
	  }
	  if (lock == SET) {
		 throw new IllegalStateException("EntryInfo is locked");
	  }
	  dataIds.union(entryInfo.dataIds);
	  templates.union(entryInfo.templates);
	  if (entryInfo.id != null) {
		 dataIds.add(entryInfo.id);
	  }
	  // if timeLimit is unset or
	  //    my timeLimit is set and entryInfo timeLimit is set and
	  //       entryInfo's timeLimit is less than mine
	  //  then set mine to entryInfo's
	  if (expirationTimeFlag == SET) {
		 setTimeLimit(entryInfo.timeLimit);
	  } else if ((entryInfo.timeLimit == -1) || ((entryInfo.timeLimit != -1) && (entryInfo.timeLimit < timeLimit))) {
		 timeLimit = entryInfo.timeLimit;
	  }
      // CPF-Inactivity
      if (inactivityFlag == SET) {
         setInactivity(entryInfo.inactivity);
      } else if ((entryInfo.inactivity == -1) || ((entryInfo.inactivity != -1) && (entryInfo.inactivity < inactivity))) {
         inactivity = entryInfo.inactivity;
      }
   }

   /**
	* This is called by CacheEntry to lock the EntryInfo, so that nothing
	* in the EntryInfo will be changed during or after coping the CacheEntry.
	*/
   /* package */
   final void lock() {
	  lock = SET;
   }

   /**
	* This overrides the method in Object so this object can be cloned.
	*
	* @return The cloned object.
        * @ibm-private-in-use
	*/
   public Object clone() throws CloneNotSupportedException {
	  return super.clone();
   }
   
   /**
	* This gets the alias list from the aliasList variable.
	*
	* @return The Enumeration of alias list.
        * @ibm-private-in-use
	*/
   public Enumeration getAliasList() {
	  return aliasList.elements();
   }

   /**
	* This unions a new alias into the aliasList variable.
	*
	* @param alias The new alias.
        * @ibm-private-in-use
	*/
   public void addAlias(Object alias) {
	  if (lock == SET) {
		 throw new IllegalStateException("EntryInfo is locked");
	  }
	  if (alias != null && !alias.equals("")) {
		  aliasList.add(alias);
	  }
   }
   
   /**
	* This gets the userMetaData in the userMetaData variable.
	*
	* @return The userMetaData.
        * @ibm-private-in-use
	*/
   public Object getUserMetaData() {
	  return userMetaData;
   }

   /**
	* This assigns the new userMetaData to the userMetaData variable.
	*
	* @param userMetaData The new userMetaData.
        * @ibm-private-in-use
	*/
   public void setUserMetaData(Object userMetaData) {
	  if (lock == SET) {
		 throw new IllegalStateException("EntryInfo is locked");
	  }
	  this.userMetaData = userMetaData;
   }

      /**
      * Pool of EntryInfo objects
      * @ibm-private-in-use
      */

   static class EntryInfoPool extends ObjectPool {

       private boolean  disableDependencyId = false;
       private String   poolName            = null;
       
       /**
       * Constructor for the EntryInfoPool class
       * 
       * @param poolName The name to be associated with this pool
       * @param size size of this pool
       * @return The new EntryInfoPool object
       * @ibm-private-in-use
       */
       public EntryInfoPool(String poolName, int size) {
           super(poolName, size);
           this.poolName = poolName;
       }

       /**
       * Creates a new EntryInfo object
       * 
       * @return A new EntryInfo object
       * @ibm-private-in-use
       */
       public Object createObject() {
           EntryInfo ei = new EntryInfo();
           ei.reset();
           return ei;
       }
       
       //---------------------------------
       /**          
       * Allocates an entryInfo object from the pool, creating a 
       * new one if needed and initializes its Id, dataIds and 
       * aliasList with those passed in as params 
       * 
       * @param key The cache key
       * @param dependencyIds An array of data Ids
       * @param aliasIds An array of alias keys
       * @return An EntryInfo object
       * @ibm-private-in-use
       */
       //---------------------------------
       public EntryInfo allocate(Object key, Object [] dependencyIds, Object [] aliasIds ) {
           EntryInfo ei = (EntryInfo)remove();
           ei.entryInfoPool = this;
           ei.setId(key);
           if (dependencyIds != null && !disableDependencyId ) {
               for (int i = 0; i < dependencyIds.length; i++) {
                   ei.addDataId(dependencyIds[i]);
               }
           }
           if (aliasIds != null) {
               for (int i = 0; i < aliasIds.length; i++)
                   ei.addAlias(aliasIds[i]);
           }
           return ei;
       }

       /**
       * Set global features configured for the cache on the EntryInfo object
       * @ibm-private-in-use
       */
       public void setFeatures( DCacheConfig cacheConfig ) {
           final String methodName = "setFeatures()";
           disableDependencyId = cacheConfig.isDisableDependencyId();
           if (tc.isDebugEnabled())
              Tr.debug(tc, methodName+" poolName=" + poolName + " disableDependencyId=" + disableDependencyId);
       }
       //---------------------------------

   }
   //--------------------------------------------------------------------

   /*
    * This allocates an EntryInfo pool.
    * 
    * @param poolName The name to be associated with the new pool being created
    * @param size size of the new pool
    * @return The newly created EntryInfoPool object
    * @ibm-private-in-use
    */
   static public EntryInfoPool createEntryInfoPool(String poolName, int size){
       final String methodName = "createEntryInfoPool()";
       EntryInfoPool pool = new EntryInfoPool(poolName, size);
       //pools.add(pool);
       if (tc.isDebugEnabled())
          Tr.debug(tc, methodName+" poolName=" + poolName);
       return pool;
   }
   //---------------------------------

   /*
    * This releases an allocated EntryInfo and returns it
    * to the pool of free EntryInfo objects
    * @ibm-private-in-use
    */
   public void returnToPool() {
       EntryInfoPool eip = entryInfoPool;
       // An assertion failure here means this EI
       // did not come from a EI pool and this
       // should never be the case;
       assert entryInfoPool != null;
       reset(); // this will reset var entryInfoPool
       eip.add(this);
   }
   //---------------------------------

	public int getCacheType() {
		return cacheType;
	}
	
	public void setCacheType(int cacheType) {
		this.cacheType = cacheType;
	}
	
	public void copyMetadata(CacheEntry cacheEntry) {
		this.id = cacheEntry.id;
		this.timeLimit = cacheEntry.timeLimit;
        this.inactivity = cacheEntry.inactivity; 
		this.expirationTime = cacheEntry.expirationTime;
		this.validatorExpirationTime = cacheEntry.validatorExpirationTime;
		this.priority = cacheEntry.priority;
		for (int i=0; i < cacheEntry._templates.length; i++) {
			this.templates.add(cacheEntry._templates[i]);
		}
		for (int i=0; i < cacheEntry._dataIds.length; i++) {
			this.dataIds.add(cacheEntry._dataIds[i]);
		}
		for (int i=0; i < cacheEntry.aliasList.length; i++) {
			this.aliasList.add(cacheEntry.aliasList[i]);
		}
		this.sharingPolicy = cacheEntry.sharingPolicy;
		this.persistToDisk = cacheEntry.persistToDisk;
		this.userMetaData = cacheEntry.userMetaData;
		this.cacheType = cacheEntry.cacheType;
		this.externalCacheGroupId = cacheEntry.externalCacheGroupId;
	}

	public void setExternalCacheGroupId(String externalCacheGroup) {
		this.externalCacheGroupId = externalCacheGroup;
	}

	public String getExternalCacheGroupId() {
		return externalCacheGroupId;
	}
}
