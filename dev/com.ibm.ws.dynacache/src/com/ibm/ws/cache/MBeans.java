/*******************************************************************************
 * Copyright (c) 1997, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache;

/**
 * Access cache statistics with the MBean interface, using JACL

 * Obtain the MBean identifier with the queryNames command, for example:
 $AdminControl queryNames type=DynaCache,*  // Returns a list of the available dynamic cache MBeans

 * Select your dynamic cache MBean and run the following command:
 set mbean [$AdminControl queryNames type=DynaCache,*]

 * Retrieve the names of the available cache statistics:
 $AdminControl invoke $mbean getCacheStatisticNames

 * Retrieve the names of the available cache instances:
 $AdminControl invoke $mbean getCacheInstanceNames

 * Retrieve all of the available cache statistics for the base cache instance:
 $AdminControl invoke $mbean getAllCacheStatistics

 * Retrieve all of the available cache statistics for the named cache instance:
 $AdminControl invoke $mbean getAllCacheStatistics "services/cache/servletInstance_4"

 * Retrieve cache statistics that are specified by the names array for the base cache instance:
 $AdminControl invoke $mbean getCacheStatistics {"DiskCacheSizeInMB ObjectsReadFromDisk4000K RemoteObjectMisses"}

 * Retrieve cache statistics that are specified by the names array for the named cache instance:
 $AdminControl invoke $mbean getCacheStatistics {services/cache/servletInstance_4 "ExplicitInvalidationsLocal CacheHits"}

 * Retrieve all the cache IDs in memory for the named cache instance that matches the specified regular expression:
 $AdminControl invoke $mbean getCacheIDsInMemory {services/cache/servletInstance_4 \S}

 * Retrieve all cache IDs on disk for the named cache instance that matches the specified regular expression:
 $AdminControl invoke $mbean getCacheIDsOnDisk {services/cache/servletInstance_4 \S}

 * Retrieves the CacheEntry, which holds metadata information for the cache ID:
 $AdminControl invoke $mbean getCacheEntry {services/cache/servletInstance_4 cache_id_1}

 * Invalidates all cache entries that match the pattern-mapped cache IDs in the named cache instance and all cache entries dependent upon the matched entries in the instance:
 $AdminControl invoke $mbean invalidateCacheIDs {services/cache/servletInstance_4 cache_id_1 true}
 */
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ReflectionException;
import javax.management.StandardMBean;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.cache.CacheAdminMBean;
import com.ibm.websphere.cache.exception.DynamicCacheException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cache.intf.DCache;
import com.ibm.ws.cache.util.MessageDigestUtility;
import com.ibm.wsspi.cache.CacheStatistics;

/**
 * All public methods in this class are exposed as runtime operations on the
 * Dynacache mbean.
 * 
 * If the cache instance is not specified then the mbean operations by default operate
 * on the base cache instance.
 * 
 * Note that the object name property here must agree with CacheAdminMBean.OBJECT_NAME.
 * 
 * Also note that this implements CacheAdmin, since it supports the
 * getCacheStatisticNames(DCache cache) method in addition to those published on the
 * CacheAdminMBean interface
 */
@Component(
           service = { MBeans.class, DynamicMBean.class },
           immediate = true,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = {
                       "service.vendor=IBM",
                       "jmx.objectname=WebSphere:feature=CacheAdmin,type=DynaCache,name=DistributedMap" })
public class MBeans extends StandardMBean implements CacheAdmin {

    private static TraceComponent tc = Tr.register(MBeans.class, DynaCacheConstants.TRACE_GROUP, DynaCacheConstants.NLS_FILE);

    private static MessageDigest messageDigestMD5 = null;
    private final static boolean INCLUDE_VALUE = true; // indicate to include the cache value in hashcode calculation

    public MBeans() {
        super(CacheAdminMBean.class, false); // yes, CacheAdminMBean, not internal
    }

    //--------------------------------------------------------------
    //  MBean Methods
    //
    //  Note: Signatures of MBean methods in CacheServiceImpl.java match the 
    //  signatures of DynaCache.xml.  The methods here may not because they need
    //  data passed from the CacheServiceImpl methods in order to perform the
    //  function of the MBean.
    //--------------------------------------------------------------

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.cache.CacheAdminMBean#getCacheSize()
     */
    @Override
    public int getCacheSize() {
        CacheConfig commonCacheConfig = ServerCache.getCacheService().getCacheConfig();
        if (commonCacheConfig != null) {
            return commonCacheConfig.cacheSize;
        }
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.cache.CacheAdminMBean#getUsedCacheSize()
     */
    @Override
    public int getUsedCacheSize() {
        DCache cache = null;
        if (ServerCache.servletCacheEnabled) {
            cache = ServerCache.cache;
        }
        int size = -1;
        if (cache != null) {
            size = cache.getNumberCacheEntries();
        } else {
            // DYNA1059W=DYNA1059W: WebSphere Dynamic Cache instance named {0} cannot be used because of Dynamic Servlet cache service has not be started.
            Tr.error(tc, "DYNA1059W", new Object[] { DCacheBase.DEFAULT_CACHE_NAME });
        }
        return size;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.cache.CacheAdminMBean#getDiskOverflow()
     */
    @Override
    public boolean getDiskOverflow() {
        CacheConfig commonCacheConfig = ServerCache.getCacheService().getCacheConfig();
        if (commonCacheConfig != null) {
            return commonCacheConfig.enableDiskOffload;
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.cache.CacheAdminMBean#getCacheStatisticNames()
     */
    @Override
    public String[] getCacheStatisticNames() {

        DCache cache = null;

        if (ServerCache.servletCacheEnabled) {
            cache = ServerCache.cache;
        } else if (ServerCache.objectCacheEnabled) {
            cache = ServerCache.getConfiguredCache(DCacheBase.DEFAULT_DISTRIBUTED_MAP_NAME);
        } else {
            //DYNA1061E=DYNA1061E: Neither servlet cache nor object caching is enabled.
            Tr.error(tc, "DYNA1061E");
        }

        return getCacheStatisticNames(cache);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.cache.CacheAdminMBean#getCacheStatisticNames(java.lang.String)
     */
    @Override
    public String[] getCacheStatisticNames(String cacheInstance) throws AttributeNotFoundException {
        DCache cache = getCache(cacheInstance);
        return getCacheStatisticNames(cache);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.CacheAdmin#getCacheStatisticNames(com.ibm.ws.cache.intf.DCache)
     */
    @Override
    public String[] getCacheStatisticNames(DCache cache) {

        Map statistics = getCacheStatisticsMap(cache.getCacheStatistics());

        String names[] = new String[statistics.size()];
        Iterator it = statistics.keySet().iterator();
        int i = 0;
        while (it.hasNext()) {
            names[i] = (String) it.next();
            i++;
        }
        return names;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.cache.CacheAdminMBean#getCacheInstanceNames()
     */
    @Override
    public String[] getCacheInstanceNames() {
        Map instances = ServerCache.getCacheInstances();
        String names[] = new String[instances.size()];
        Iterator it = instances.keySet().iterator();
        int i = 0;
        while (it.hasNext()) {
            names[i] = (String) it.next();
            i++;
        }
        return names;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.cache.CacheAdminMBean#getAllCacheStatistics()
     */
    @Override
    public String[] getAllCacheStatistics() {
        DCache cache = null;
        if (ServerCache.servletCacheEnabled) {
            cache = ServerCache.cache;
        }
        String stats[] = null;
        if (cache != null) {
            Map statistics = getCacheStatisticsMap(cache.getCacheStatistics());
            Iterator keys = statistics.keySet().iterator();
            Iterator values = statistics.values().iterator();
            stats = new String[statistics.size()];
            int i = 0;
            while (keys.hasNext()) {
                stats[i] = keys.next() + "=" + values.next();
                i++;
            }
        } else {
            stats = new String[0];
            // DYNA1059W=DYNA1059W: WebSphere Dynamic Cache instance named {0} cannot be used because of Dynamic Servlet cache service has not be started.
            Tr.error(tc, "DYNA1059W", new Object[] { DCacheBase.DEFAULT_CACHE_NAME });
        }

        return stats;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.cache.CacheAdminMBean#getAllCacheStatistics(java.lang.String)
     */
    @Override
    public String[] getAllCacheStatistics(String cacheInstance) throws javax.management.AttributeNotFoundException {

        //Get the cache for this cacheInstance
        DCache cache1 = getCache(cacheInstance);

        Map statistics = getCacheStatisticsMap(cache1.getCacheStatistics());
        Iterator keys = statistics.keySet().iterator();
        Iterator values = statistics.values().iterator();
        String stats[] = new String[statistics.size()];
        int i = 0;
        while (keys.hasNext()) {
            stats[i] = keys.next() + "=" + values.next();
            i++;
        }

        return stats;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.cache.CacheAdminMBean#getCacheStatistics(java.lang.String[])
     */
    @Override
    public String[] getCacheStatistics(String[] names) throws javax.management.AttributeNotFoundException {

        if (names == null)
            return null;

        DCache cache = null;
        if (ServerCache.servletCacheEnabled) {
            cache = ServerCache.cache;
        }
        String stats[] = null;
        if (cache != null) {
            Map statistics = getCacheStatisticsMap(cache.getCacheStatistics());
            stats = new String[names.length];
            for (int i = 0; i < names.length; i++) {

                if (!statistics.containsKey(names[i])) {
                    Tr.error(tc, "DYNA1052E", new Object[] { names[i] });
                    throw new AttributeNotFoundException(names[i] + " is not a valid cache statistic name");
                }

                stats[i] = names[i] + "=" + statistics.get(names[i]);
            }
        } else {
            stats = new String[0];
        }

        return stats;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.cache.CacheAdminMBean#getCacheStatistics(java.lang.String, java.lang.String[])
     */
    @Override
    public String[] getCacheStatistics(String cacheInstance, String[] names) throws javax.management.AttributeNotFoundException {
        if (names == null)
            return null;
        //Get the cache for this cacheInstance
        DCache cache1 = getCache(cacheInstance);

        Map statistics = getCacheStatisticsMap(cache1.getCacheStatistics());
        String stats[] = new String[names.length];
        for (int i = 0; i < names.length; i++) {

            if (!statistics.containsKey(names[i])) {
                Tr.error(tc, "DYNA1052E", new Object[] { names[i] });
                throw new AttributeNotFoundException(names[i] + " is not a valid cache statistic name");
            }

            stats[i] = names[i] + "=" + statistics.get(names[i]);
        }
        return stats;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.cache.CacheAdminMBean#getCacheIDsInMemory(java.lang.String, java.lang.String)
     */
    @Override
    public String[] getCacheIDsInMemory(String cacheInstance, String pattern) throws javax.management.AttributeNotFoundException {

        // Get the cache for this cacheInstance
        DCache cache1 = getCache(cacheInstance);
        // Check that the input pattern is a valid regular expression
        Pattern cpattern = checkPattern(pattern);

        List<String> matchSet = new ArrayList<String>(10);
        int i = 0;
        Enumeration vEnum = cache1.getAllIds();
        while (vEnum.hasMoreElements()) {
            Object key = vEnum.nextElement();
            String skey = key.toString();
            boolean matches = checkMatch(cpattern, skey);
            if (matches) {
                matchSet.add(skey);
                i++;
                //if (tc.isDebugEnabled()) {
                //  Tr.debug(tc,"getCacheIDsInMemory:  Cache element # " + i +" = " + skey + " matches the pattern " + pattern + " for cacheInstance = " + cacheInstance);
                //}    
            }
        }
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getCacheIDsInMemory" + "/" + cacheInstance + "/" + "Exiting. Number of matches found = " + i);

        //Allocate output String array #entries matched and return
        //Convert to string array and return 
        String[] cids = matchSet.toArray(new String[matchSet.size()]);
        return cids;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.cache.CacheAdminMBean#getCacheIDsOnDisk(java.lang.String, java.lang.String)
     */
    @Override
    public String[] getCacheIDsOnDisk(String cacheInstance, String pattern) throws javax.management.AttributeNotFoundException {

        // Get the cache for this cacheInstance
        DCache cache1 = getCache(cacheInstance);

        //Throw exception if disk caching is not enabled.  
        if (!cache1.getSwapToDisk()) {
            Tr.error(tc, "DYNA1051E", new Object[] { "getCacheIDsOnDisk", cacheInstance });
            throw new AttributeNotFoundException("Disk caching is not enabled.");
        }
        // Check that the input pattern is a valid regular expression
        Pattern cpattern = checkPattern(pattern);

        List<String> matchSet = new ArrayList<String>(10);
        if (cache1.getIdsSizeDisk() > 0) {
            int index = 0;
            boolean more = false;

            do {

                more = false;
                Collection diskSet = cache1.getIdsByRangeDisk(index, 100);
                if (diskSet != null) {
                    if (true == diskSet.contains(HTODDynacache.DISKCACHE_MORE)) {
                        more = true; //are there any IDs on the disk
                    }
                }

                index = 1;
                diskSet.remove(HTODDynacache.DISKCACHE_MORE);
                Iterator idIterator = diskSet.iterator();
                while (idIterator.hasNext()) {
                    Object key = idIterator.next();
                    String skey = key.toString();
                    boolean matches = checkMatch(cpattern, skey);
                    if (matches) {
                        matchSet.add(skey);
                        //if (tc.isDebugEnabled()) {
                        //   Tr.debug(tc,"getCacheIDsOnDisk:  Disk Cache id = " +skey + " matches the pattern " + pattern + " for cacheInstance = " + cacheInstance);
                        //}
                    }
                }
            } while (more == true);
        }

        //Allocate output array #entries matched 
        String[] cids = matchSet.toArray(new String[matchSet.size()]);
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getCacheIDsOnDisk: Exiting. Number of matches found = " + matchSet.size());
        return cids;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.cache.CacheAdminMBean#getCacheIDsInPushPullTable(java.lang.String, java.lang.String)
     */
    @Override
    public String[] getCacheIDsInPushPullTable(String cacheInstance, String pattern) throws javax.management.AttributeNotFoundException { // LI4337-17

        // Get the cache for this cacheInstance
        DCache cache1 = getCache(cacheInstance);
        // Check that the input pattern is a valid regular expression
        Pattern cpattern = checkPattern(pattern);

        // retrieve all the cache ids from PushPullTable
        List<String> matchSet = new ArrayList<String>(10);
        int count = 0;
        Object[] list = cache1.getCacheIdsInPushPullTable().toArray();
        for (int i = 0; i < list.length; i++) {
            String skey = list[i].toString();
            boolean matches = checkMatch(cpattern, skey);
            if (matches) {
                matchSet.add(skey);
                count++;
                //if (tc.isDebugEnabled()) {
                //  Tr.debug(tc,"getCacheIDsInPushPullTable:  Cache element # " + count +" = " + skey + " matches the pattern " + pattern + " for cacheInstance = " + cacheInstance);
                //}    
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "getCacheIDsInPushPullTable" + "/" + cacheInstance + "/" + "Exiting. Number of matches found = " + count);
        }
        //Allocate output String array #entries matched and return
        //Convert to string array and return 
        String[] cids = matchSet.toArray(new String[matchSet.size()]);
        return cids;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.cache.CacheAdminMBean#invalidateCacheIDs(java.lang.String, java.lang.String, boolean)
     */
    @Override
    public String[] invalidateCacheIDs(String cacheInstance, String pattern, boolean waitOnInvalidation) throws javax.management.AttributeNotFoundException {

        // Get the cache for this cacheInstance
        DCache cache1 = getCache(cacheInstance);

        //Check to see if request is to clear the cache
        if (pattern.equals("*")) {
            cache1.clear();
            if (tc.isDebugEnabled())
                Tr.debug(tc, "invalidateCacheIDs: Exiting. Cleared memory and disk cache since input pattern is *");
            return new String[] { "*" };
        }

        // Check that the input pattern is a valid regular expression
        Pattern cpattern = checkPattern(pattern);

        // Get union of matches in memory and on disk
        Collection<String> invalidateSet = new ArrayList<String>();

        // Call mbeans to get matches in memory and on disk.
        String[] memoryMatches = getCacheIDsInMemory(cacheInstance, pattern);
        if (null != memoryMatches) {
            for (int i = 0; i < memoryMatches.length; i++) {
                invalidateSet.add(memoryMatches[i]);
            }
        }
        if (cache1.getSwapToDisk()) {
            String[] diskMatches = getCacheIDsOnDisk(cacheInstance, pattern);
            if (null != diskMatches) {
                for (int i = 0; i < diskMatches.length; i++) {
                    invalidateSet.add(diskMatches[i]);
                }
            }
        }

        Iterator invalidates = invalidateSet.iterator();
        // Invalidate the set of cache ids matched to the pattern and all
        // entries dependent on the matched entry
        while (invalidates.hasNext()) {
            Object cacheID = invalidates.next();
            cache1.invalidateById(cacheID, waitOnInvalidation);
        }

        if (tc.isDebugEnabled())
            Tr.debug(tc, "invalidateCacheIDs: Exiting. Number of matches found = " + invalidateSet.size());

        //need to convert to string array before return.
        //Allocate output array #entries matched 
        String[] cids = invalidateSet.toArray(new String[invalidateSet.size()]);
        return cids;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.cache.CacheAdminMBean#getCacheEntry(java.lang.String, java.lang.String)
     */
    @Override
    public String getCacheEntry(String cacheInstance, String cacheId) throws javax.management.AttributeNotFoundException { // 429429       

        if (cacheId == null) {
            throw new AttributeNotFoundException(cacheId + " is null.");
        }

        //Get the cache for this cacheInstance
        DCache cache1 = getCache(cacheInstance);
        //Get the cache entry

        EntryInfo ei = new EntryInfo();
        ei.setId(cacheId);
        com.ibm.websphere.cache.CacheEntry cacheEntry = cache1.getEntry(ei, true);

        String stringEntry = null;
        if (null != cacheEntry) {
            stringEntry = cacheEntry.toString();
            // Release cacheEntry and decrement referenced count for it before
            // return.
            cacheEntry.finish();
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "getCacheEntry: Returning: " + stringEntry
                         + " for cacheInstance = " + cacheInstance);
        }

        return stringEntry;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.cache.CacheAdminMBean#clearCache(java.lang.String)
     */
    @Override
    public void clearCache(String cacheInstance) throws javax.management.AttributeNotFoundException {

        //Get the cache for this cacheInstance
        DCache cache1 = getCache(cacheInstance);
        cache1.clear();
        if (tc.isDebugEnabled())
            Tr.debug(tc, "clearCache: Exiting. Cleared memory and disk cache for cacheInstance = " + cacheInstance);

        return;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.cache.CacheAdminMBean#getCacheDigest(java.lang.String, boolean, boolean, boolean)
     */
    @Override
    public String getCacheDigest(String cacheInstance, boolean useMemoryCacheDigest,
                                 boolean cacheIDOnly, boolean debug) throws javax.management.AttributeNotFoundException { // LI4337-17

        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "getCacheDigest: cacheInstance=" + cacheInstance + " useMemoryCacheDigest=" + useMemoryCacheDigest +
                         " cacheIDOnly=" + cacheIDOnly + " debug=" + debug);
        }
        // Get the cache for this cacheInstance
        DCache cache1 = getCache(cacheInstance);

        // Get the message digest
        getMessageDigestMD5();

        // Calculate total hashcode for cache ids and cache value in memory cache  
        int totalHashcode = cache1.getMemoryCacheHashcode(debug, !cacheIDOnly);

        if (cacheIDOnly == true) {
            // Calculate total hashcode for cache ids in PushPullTable  
            totalHashcode += cache1.getCacheIdsHashcodeInPushPullTable(debug);
        }

        if (useMemoryCacheDigest == false) {
            // Calculate total hashcode for cache ids and cache value in disk cache  
            try {
                totalHashcode += cache1.getDiskCacheHashcode(debug, !cacheIDOnly);
            } catch (DynamicCacheException e) {
                Tr.error(tc, "DYNA1053E", new Object[] { DynaCacheConstants.MBEAN_GET_CACHE_DIGEST, e.getMessage() });
                throw new AttributeNotFoundException(e.getMessage());
            }
        }

        // Convert hashcode to MD5 digest
        String md5Hash = MessageDigestUtility.processMessageDigestForData(MBeans.messageDigestMD5, totalHashcode);

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "getCacheDigest: Exiting. Entire cache hash in MD5 is " + md5Hash + " for cacheInstance = " + cacheInstance);
        }
        return md5Hash;
    }

    // create messageDigest for MD5 algoritm if it is not created.
    private void getMessageDigestMD5() throws AttributeNotFoundException {
        if (MBeans.messageDigestMD5 == null) {
            try {
                MBeans.messageDigestMD5 = MessageDigestUtility.createMessageDigest("MD5");
            } catch (NoSuchAlgorithmException e) {
                Tr.error(tc, "DYNA1044E", new Object[] { e.getMessage() });
                throw new AttributeNotFoundException("Message digest for MD5 is not available. " + e.getMessage());
            }
        }
    }

    private DCache getCache(String cacheInstance) throws AttributeNotFoundException {
        if (cacheInstance == null) {
            Tr.error(tc, "DYNA1042E", new Object[] { cacheInstance });
            throw new AttributeNotFoundException(cacheInstance + " is not a valid cache instance name.");
        }
        DCache cache = null;
        if (cacheInstance.equals(DCacheBase.DEFAULT_CACHE_NAME)) {
            cache = ServerCache.cache;
        } else {
            cache = ServerCache.getConfiguredCache(cacheInstance);
        }
        if (cache == null) {
            Tr.error(tc, "DYNA1042E", new Object[] { cacheInstance });
            throw new AttributeNotFoundException(cacheInstance + " is not a valid cache instance name.");
        }
        return cache;
    }

    private Pattern checkPattern(String pattern) throws AttributeNotFoundException {
        Pattern p = null;
        // Check that the input pattern is a valid regular expression
        try {
            p = Pattern.compile(pattern);
        } catch (PatternSyntaxException pse) {
            Tr.error(tc, "DYNA1043E", new Object[] { pattern, pse.getMessage() });
            throw new AttributeNotFoundException("The pattern " + pattern + " is not a valid regular expression. Caught PatternSyntaxException:" + pse.getMessage());
        }
        return p;
    }

    private boolean checkMatch(Pattern pattern, String key) {
        // Does the input pattern match the key?
        return pattern.matcher(key).find();
    }

    public static Map getCacheStatisticsMap(CacheStatistics cacheStatistics) {
        TreeMap<String, Number> stats = new TreeMap<String, Number>();
        if (cacheStatistics != null) {
            stats.put("CacheHits", cacheStatistics.getCacheHitsCount());
            stats.put("CacheLruRemoves", cacheStatistics.getCacheLruRemovesCount());
            stats.put("CacheMisses", cacheStatistics.getCacheMissesCount());
            stats.put("CacheRemoves", cacheStatistics.getCacheRemovesCount());
            stats.put("ExplicitInvalidationsFromMemory", cacheStatistics.getExplicitInvalidationsFromMemoryCount());
            stats.put("MemoryCacheEntries", cacheStatistics.getMemoryCacheEntriesCount());
            stats.put("MemoryCacheSizeInMB", cacheStatistics.getMemoryCacheSizeInMBCount());
            stats.put("TimeoutInvalidationsFromMemory", cacheStatistics.getTimeoutInvalidationsFromMemoryCount());
            Map<String, Number> extendedStats = cacheStatistics.getExtendedStats();
            if (extendedStats != null) {
                stats.putAll(cacheStatistics.getExtendedStats());
            }
        }
        return stats;
    }

    @Override
    public MBeanInfo getMBeanInfo() {

        MBeanInfo mbeanInfo = super.getMBeanInfo();
        MBeanOperationInfo[] existingMbeanOperationInfo = mbeanInfo.getOperations();

        // new mbean operations to be added  ... Reflection did not correctly expose these methods
        MBeanOperationInfo getCacheInstanceNamesMbeanOperationInfo = createMbeanOperationInfo("getCacheInstanceNames", "[Ljava.lang.String;");
        MBeanOperationInfo getAllCacheStatisticsMbeanOperationInfo = createMbeanOperationInfo("getAllCacheStatistics", "[Ljava.lang.String;");
        MBeanOperationInfo getCacheStatisticNamesMbeanOperationInfo = createMbeanOperationInfo("getCacheStatisticNames", "[Ljava.lang.String;");
        MBeanOperationInfo getUsedCacheSizeMbeanOperationInfo = createMbeanOperationInfo("getUsedCacheSize", Integer.TYPE.getName());
        MBeanOperationInfo getCacheSizeMbeanOperationInfo = createMbeanOperationInfo("getCacheSize", Integer.TYPE.getName());
        MBeanOperationInfo getDiskOverflowMbeanOperationInfo = createMbeanOperationInfo("getDiskOverflow", Boolean.TYPE.getName());
        MBeanOperationInfo[] addedMbeanOperationInfo = new MBeanOperationInfo[] {
                                                                                 getCacheInstanceNamesMbeanOperationInfo,
                                                                                 getAllCacheStatisticsMbeanOperationInfo,
                                                                                 getCacheStatisticNamesMbeanOperationInfo,
                                                                                 getUsedCacheSizeMbeanOperationInfo,
                                                                                 getCacheSizeMbeanOperationInfo,
                                                                                 getDiskOverflowMbeanOperationInfo };

        MBeanOperationInfo[] modifiedMBeanOperationInfo = new MBeanOperationInfo[existingMbeanOperationInfo.length + addedMbeanOperationInfo.length];
        System.arraycopy(existingMbeanOperationInfo, 0, modifiedMBeanOperationInfo, 0, existingMbeanOperationInfo.length);
        System.arraycopy(addedMbeanOperationInfo, 0, modifiedMBeanOperationInfo, existingMbeanOperationInfo.length, addedMbeanOperationInfo.length);

        final MBeanInfo nmbi = new MBeanInfo(this.getClass().getName(),
                        mbeanInfo.getDescription(), null, mbeanInfo.getConstructors(),
                        modifiedMBeanOperationInfo, mbeanInfo.getNotifications(),
                        mbeanInfo.getDescriptor());

        return nmbi;
    }

    @Override
    public Object invoke(String actionName, Object params[], String signature[])
                    throws MBeanException, ReflectionException {

        Object o = null;
        try {
            Method m = this.getClass().getMethod(actionName, getParameterTypes(params));
            o = m.invoke(this, params);

        } catch (SecurityException e) {
            throw new MBeanException(e);
        } catch (NoSuchMethodException e) {
            throw new MBeanException(e);
        } catch (IllegalArgumentException e) {
            throw new MBeanException(e);
        } catch (IllegalAccessException e) {
            throw new MBeanException(e);
        } catch (InvocationTargetException e) {
            throw new MBeanException(e);
        }

        return o;
    }

    Class<?>[] getParameterTypes(Object params[]) {
        Class<?>[] parameterTypes = null;
        if (null != params) {
            parameterTypes = new Class<?>[params.length];
            int i = 0;
            for (Object object : params) {
                parameterTypes[i++] = object.getClass();
            }
        }
        return parameterTypes;
    }

    public MBeanOperationInfo createMbeanOperationInfo(String operationName, String returnType) {
        MBeanOperationInfo addOperationInfo =
                        new MBeanOperationInfo(
                                        operationName,
                                        "Operation exposed for management", //TODO for now description is empty 
                                        null,
                                        returnType,
                                        MBeanOperationInfo.INFO);
        return addOperationInfo;
    }

}
