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
package com.ibm.ws.cache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.websphere.cache.DistributedObjectCache;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.cache.intf.DCache;
import com.ibm.ws.cache.intf.DCacheConfig;
import com.ibm.ws.cache.util.FieldInitializer;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;

public class CacheConfig implements DCacheConfig, Cloneable {

    private static TraceComponent tc = Tr.register(CacheConfig.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

    // Used in distributedmap.properties
    public static final String CACHE_NAME = "com.ibm.ws.cache.CacheConfig.cacheName";
    public static final String CACHE_SIZE = "com.ibm.ws.cache.CacheConfig.cacheSize";
    public static final String ENABLE_DISK_OFFLOAD = "com.ibm.ws.cache.CacheConfig.enableDiskOffload";
    public static final String DISK_OFFLOAD_LOCATION = "com.ibm.ws.cache.CacheConfig.diskOffloadLocation";
    public static final String USE_LISTENER_CONTEXT = "com.ibm.ws.cache.CacheConfig.useListenerContext";
    public static final String FLUSH_TO_DISK_ON_STOP = "com.ibm.ws.cache.CacheConfig.flushToDiskOnStop";
    public static final String ENABLE_LOCKING_SUPPORT = "com.ibm.ws.cache.CacheConfig.enableLockingSupport";
    public static final String DISABLE_DEPENDENCY_ID = "com.ibm.ws.cache.CacheConfig.disableDependencyId";
    public static final String DISABLE_TEMPLATES_SUPPORT = "com.ibm.ws.cache.CacheConfig.disableTemplatesSupport";
    public static final String ENABLE_NIO_SUPPORT = "com.ibm.ws.cache.CacheConfig.enableNioSupport";
    public static final String ENABLE_REPLICATION_ACKS = "com.ibm.ws.cache.CacheConfig.enableReplicationAcks";
    public static final String ENABLE_CACHE_REPLICATION = "com.ibm.ws.cache.CacheConfig.enableCacheReplication";
    public static final String REPLICATION_DOMAIN = "com.ibm.ws.cache.CacheConfig.replicationDomain";
    public static final String DISK_CLEANUP_FREQUENCY = "com.ibm.ws.cache.CacheConfig.htodCleanupFrequency";
    public static final String DISK_DELAY_OFFLOAD = "com.ibm.ws.cache.CacheConfig.htodDelayOffload";
    public static final String DISK_DELAY_OFFLOAD_ENTRIES_LIMIT = "com.ibm.ws.cache.CacheConfig.htodDelayOffloadEntriesLimit";
    public static final String DISK_DELAY_OFFLOAD_DEPID_BUCKETS = "com.ibm.ws.cache.CacheConfig.htodDelayOffloadDepIdBuckets";
    public static final String DISK_DELAY_OFFLOAD_TEMPLATE_BUCKETS = "com.ibm.ws.cache.CacheConfig.htodDelayOffloadTemplateBuckets";
    public static final String IGNORE_VALUE_IN_INVALIDATION_EVENT = "com.ibm.ws.cache.CacheConfig.ignoreValueInInvalidationEvent";
    public static final String DISKCACHE_PERFORMANCE_LEVEL = "com.ibm.ws.cache.CacheConfig.diskCachePerformanceLevel";
    public static final String DISKCACHE_EVICTION_POLICY = "com.ibm.ws.cache.CacheConfig.diskCacheEvictionPolicy";
    public static final String DISKCACHE_HIGH_THRESHOLD = "com.ibm.ws.cache.CacheConfig.diskCacheHighThreshold";
    public static final String DISKCACHE_LOW_THRESHOLD = "com.ibm.ws.cache.CacheConfig.diskCacheLowThreshold";
    public static final String DISKCACHE_SIZE = "com.ibm.ws.cache.CacheConfig.diskCacheSize";
    public static final String DISKCACHE_SIZE_GB = "com.ibm.ws.cache.CacheConfig.diskCacheSizeInGB";
    public static final String DISKCACHE_ENTRY_SIZE_MB = "com.ibm.ws.cache.CacheConfig.diskCacheEntrySizeInMB";
    public static final String USE_SERVER_CLASSLOADER = "com.ibm.ws.cache.CacheConfig.useServerClassLoader";
    public static final String DISKCACHE_EXPLICIT_BUFFER_LIMIT_ON_STOP = "com.ibm.ws.cache.CacheConfig.explicitBufferLimitOnStop";
    public static final String DISABLE_STORE_COOKIES = "com.ibm.ws.cache.CacheConfig.disableStoreCookies";
    public static final String FILTER_TIMEOUT_INVALIDATION = "com.ibm.ws.cache.CacheConfig.filterTimeOutInvalidation";
    public static final String FILTER_LRU_INVALIDATION = "com.ibm.ws.cache.CacheConfig.filterLRUInvalidation";
    public static final String FILTER_INACTIVITY_INVALIDATION = "com.ibm.ws.cache.CacheConfig.filterInactivityInvalidation";
    public static final String LRU_TO_DISK_TRIGGER_TIME = "com.ibm.ws.cache.CacheConfig.lruToDiskTriggerTime";
    public static final String LRU_TO_DISK_TRIGGER_PERCENT = "com.ibm.ws.cache.CacheConfig.lruToDiskTriggerPercent";
    public static final String CACHE_ENTRY_WINDOW = "com.ibm.ws.cache.CacheConfig.cacheEntryWindow";
    public static final String CACHE_PERCENTAGE_WINDOW = "com.ibm.ws.cache.CacheConfig.cachePercentageWindow";
    public static final String CACHE_INVALIDATE_ENTRY_WINDOW = "com.ibm.ws.cache.CacheConfig.cacheInvalidateEntryWindow";
    public static final String CACHE_INVALIDATE_PERCENT_WINDOW = "com.ibm.ws.cache.CacheConfig.cacheInvalidatePercentWindow";
    public static final String BATCH_UPDATE_MILLISECONDS = "com.ibm.ws.cache.CacheConfig.batchUpdateMilliseconds";
    public static final String CASCADE_CACHESPEC_PROPERTIES = "com.ibm.ws.cache.CacheConfig.cascadeCachespecProperties";
    public static final String CACHE_PROVIDER_NAME = "com.ibm.ws.cache.CacheConfig.cacheProviderName";
    public static final String MEMORY_CACHE_SIZE_IN_MB = "com.ibm.ws.cache.CacheConfig.memoryCacheSizeInMB";
    public static final String MEMORY_CACHE_HIGH_THRESHOLD = "com.ibm.ws.cache.CacheConfig.memoryCacheHighThreshold";
    public static final String MEMORY_CACHE_LOW_THRESHOLD = "com.ibm.ws.cache.CacheConfig.memoryCacheLowThreshold";
    public static final String CREATE_CACHE_AT_SERVER_STARTUP = "com.ibm.ws.cache.CacheConfig.createCacheAtServerStartup";
    public static final String REPLICATION_PAYLOAD_SIZE_IN_MB = "com.ibm.ws.cache.CacheConfig.replicationPayloadSizeInMB";
    public static final String CACHE_PROVIDER_OBJECT_GRID = "com.ibm.ws.objectgrid.dynacache.CacheProviderImpl";
    public static final String CACHE_PROVIDER_RESTORE_DYNACACHE_DEFAULTS = "com.ibm.ws.cache.CacheConfig.restoreDynacacheDefaults";
    public static final String AUTO_FLUSH_IMPORTS = "com.ibm.ws.cache.CacheConfig.autoFlushIncludes";
    public static final String PROPOGATE_INVALIDATIONS_NOT_SHARED = "com.ibm.ws.cache.CacheConfig.propogateInvalidationsNotShared";
    public static final String ALWAYS_SET_SURROGATE_CONTROL_HDR = "com.ibm.ws.cache.CacheConfig.alwaysSetSurrogateControlHdr";
    public static final String DISCARD_JSP_CONTENT = "discardJSPContent";
    public static final String CACHE_PROVIDER_DYNACACHE = "default";
    public static final String USE_602_REQUIRED_ATTR_COMPATIBILITY = "com.ibm.ws.use602RequiredAttrCompatibility";
    public static final String ALWAYS_TRIGGER_COMMAND_INVALIDATIONS = "com.ibm.ws.CacheConfig.alwaysTriggerCommandInvalidations";
    public static final String CACHE_ENTRY_REF_COUNT_TRACKING = "com.ibm.ws.cache.CacheConfig.refCountTracking";
    public static final String ENABLE_INTER_CELL_INVALIDATION = "com.ibm.ws.cache.CacheConfig.enableInterCellInvalidation";
    public static final String ALWAYS_SYNCHRONIZE_ON_GETS = "com.ibm.ws.cache.CacheConfig.alwaysSynchronizeOnGets";
    public static final String FILTERED_STATUS_CODES = "com.ibm.ws.cache.CacheConfig.filteredStatusCodes";
    public static final String IGNORE_CACHEABLE_COMMAND_SERIALIZATION_EXCEPTION = "com.ibm.ws.cache.CacheConfig.ignoreCacheableCommandDeserializationException";
    public static final String DISK_DEPENDENCY_CACHE_INDEX_ENABLED = "com.ibm.ws.cache.CacheConfig.htodDependencyCacheIndexEnabled";
    public static final String LIBRARY_REF = "com.ibm.ws.cache.CacheConfig.libraryRef";
    public static final String WEBSERVICES_SET_REQUIRED_TRUE = "com.ibm.ws.cache.CacheConfig.webservicesSetRequiredTrue";

    // ---------------------------------------------------------
    // Warning - Never change these values!! They are
    // used by customers in cacheInstance.properties.
    // ---------------------------------------------------------
    public static final int CACHE_UNITS_ENTRIES = 0x01;
    public static final int CACHE_UNITS_KILOBYTES = 0x02;
    // ---------------------------------------------------------

    public static final int HIGH = 3;
    public static final int CUSTOM = 2;
    public static final int BALANCED = 1;
    public static final int LOW = 0;

    public static final int EVICTION_NONE = 0;
    public static final int EVICTION_RANDOM = 1;
    public static final int EVICTION_SIZE_BASED = 2;
    // -------------------------------------------------
    // Config settings - Behaviour Change from v5
    // -------------------------------------------------
    boolean filterTimeOutInvalidation = false; // v5 was false
    boolean filterLRUInvalidation = false;
    boolean filterInactivityInvalidation = false;

    // Define for default value
    public static final int DEFAULT_DISABLE_CACHE_SIZE_MB = -1;
    public static final int DEFAULT_DISKCACHE_PERFORMANCE_LEVEL = BALANCED;
    public static final int DEFAULT_DISKCACHE_EVICTION_POLICY = EVICTION_NONE;
    public static final boolean DEFAULT_DISKCACHE_DELAY_OFFLOAD = true;
    public static final int DEFAULT_DISKCACHE_CLEANUP_FREQUENCY = 0;
    public static final int DEFAULT_MAX_BUFFERED_CACHE_IDS_PER_METADATA = 1000;
    public static final int DEFAULT_MAX_BUFFERED_DEPENDENCY_IDS = 1000;
    public static final int DEFAULT_MAX_BUFFERED_TEMPLATES = 100;
    public static final int DEFAULT_HIGH_THRESHOLD = 80; // unit in percent
    public static final int DEFAULT_LOW_THRESHOLD = 70; // unit in percent
    public static final int DEFAULT_DISKCACHE_SIZE = 0;
    public static final int DEFAULT_DISKCACHE_SIZE_GB = 0;
    public static final int DEFAULT_DISKCACHE_ENTRY_SIZE_MB = 0;
    public static final int DEFAULT_DISKCACHE_POOL_ENTRY_LIFE = 1000 * 60 * 5; // Life is five minutes
    public static final int DEFAULT_EXPLICIT_BUFFER_LIMIT_ON_STOP = 0; // explicit buffer limit on stop
    public static final int DEFAULT_ENTRY_WINDOW = 50; // PK32201 and PK35824 DRS (2% or 50 entries) batching fix
    public static final int DEFAULT_PERCENTAGE_WINDOW = 2; // PK32201 and PK35824 DRS (2% or 50 entries) batching fix
    public static final int DEFAULT_TLD_TIME_GRANULARITY = 5; // unit in sec
    public static final int DEFAULT_LRU_TO_DISK_TRIGGER_TIME = DEFAULT_TLD_TIME_GRANULARITY * 1000; // unit in msec
    public static final int DEFAULT_LRU_TO_DISK_TRIGGER_PERCENT = 0; // unit in percent
    public static final int DEFAULT_LRU_TO_DISK_TRIGGER_TIME_FOR_TRIMCACHE = 1000; // unit in msec
    public static final int DEFAULT_REPLICATION_PAYLOAD_SIZE_IN_MB = 10; // 10 MB

    // Define for maxinum and mininum values
    public static final int MAX_DISKCACHE_PERFORMANCE_LEVEL = HIGH;
    public static final int MIN_DISKCACHE_PERFORMANCE_LEVEL = LOW;
    public static final int MAX_CLEANUP_FREQUENCY = 1440;
    public static final int MIN_CLEANUP_FREQUENCY = 0;
    public static final int MAX_DISKCACHE_BUFFERED_CACHE_IDS_PER_METADATA = Integer.MAX_VALUE;
    public static final int MIN_DISKCACHE_BUFFERED_CACHE_IDS_PER_METADATA = 100;
    public static final int MAX_DISKCACHE_BUFFERED_DEPENDENCY_IDS = Integer.MAX_VALUE;
    public static final int MIN_DISKCACHE_BUFFERED_DEPENDENCY_IDS = 100;
    public static final int MAX_DISKCACHE_BUFFERED_TEMPLATES = Integer.MAX_VALUE;
    public static final int MIN_DISKCACHE_BUFFERED_TEMPLATES = 10;
    public static final int MAX_DISKCACHE_EVICTION_POLICY = EVICTION_SIZE_BASED;
    public static final int MIN_DISKCACHE_EVICTION_POLICY = EVICTION_NONE;
    public static final int MAX_DISKCACHE_SIZE = Integer.MAX_VALUE;
    public static final int MIN_DISKCACHE_SIZE = 20;
    public static final int MAX_DISKCACHE_SIZE_GB = Integer.MAX_VALUE;
    public static final int MIN_DISKCACHE_SIZE_GB = 3;
    public static final int MAX_DISKCACHE_ENTRY_SIZE_MB = Integer.MAX_VALUE;
    public static final int MIN_DISKCACHE_ENTRY_SIZE_MB = 0;
    public static final int MAX_HIGH_THRESHOLD = 100; // unit in percent
    public static final int MIN_HIGH_THRESHOLD = 1; // unit in percent
    public static final int MAX_LOW_THRESHOLD = 100; // unit in percent
    public static final int MIN_LOW_THRESHOLD = 1; // unit in percent
    public static final int MAX_LRU_TO_DISK_TRIGGER_TIME = DEFAULT_TLD_TIME_GRANULARITY * 1000; // unit in msec
    public static final int MIN_LRU_TO_DISK_TRIGGER_TIME = 1; // unit in msec
    public static final int MAX_LRU_TO_DISK_TRIGGER_PERCENT = 100; // unit in percent
    public static final int MIN_LRU_TO_DISK_TRIGGER_PERCENT = 0; // unit in percent

    /**
     * This determines how many cycles in the clock algorithm must pass before an unused entry is chosen as a victim.
     * Each entry's clock starts with this and is decremented each clock cycle. A clock value of <= 0 implies a victim
     * candidate. Its default is 1.
     */
    public static int DEFAULT_PRIORITY = 1;
    public static int MAX_PRIORITY = 16;

    // -------------------------------------------------
    // Config settings - General
    // -------------------------------------------------
    String cacheProviderName = CACHE_PROVIDER_DYNACACHE;
    boolean restoreDynacacheDefaults = true;
    boolean defaultProvider = true;
    boolean createCacheAtServerStartup = false;
    boolean autoFlushIncludes = false;

    String cacheName = null;
    String jndiName = null;
    String tempDir = null;
    String dtdDir = null;
    String serverServerName = null;
    int cachePriority = DEFAULT_PRIORITY;
    int jspCachePriority = DEFAULT_PRIORITY;
    int commandCachePriority = DEFAULT_PRIORITY;
    int diskHashBuckets = 1024;
    boolean webservicesSetRequiredTrue = true;
    // -------------------------------------------------
    // Config settings - Cache Size
    // -------------------------------------------------
    int cacheSize = 2000;
    int memoryCacheSizeInMB = DEFAULT_DISABLE_CACHE_SIZE_MB; // default: -1 means disable
    int memoryCacheHighThreshold = 95;
    int memoryCacheLowThreshold = 80;

    // -------------------------------------------------
    // Config settings - Replication
    // -------------------------------------------------
    String replicationDomain = null;
    boolean enableCacheReplication = false;
    int replicationType = 0;
    int defaultShareType = EntryInfo.NOT_SHARED;
    int pushFrequency = 1; // seconds
    int batchUpdateInterval = 1000; // msec
    int batchUpdateMilliseconds = -1;
    boolean drsDisabled = false; // To indicate if replication is temporarily disabled due to congestion
    boolean drsBootstrapEnabled = true;
    int congestionSleepTimeMilliseconds = 250; // DRS congestion sleep time in ms
    int replicationPayloadSizeInMB = DEFAULT_REPLICATION_PAYLOAD_SIZE_IN_MB; // default payload size = 20 MB

    // -------------------------------------------------
    // Config settings - HTOD
    // -------------------------------------------------
    boolean enableDiskOffload = false;
    String diskOffloadLocation = null;
    boolean flushToDiskOnStop = false;
    int htodCleanupFrequency = DEFAULT_DISKCACHE_CLEANUP_FREQUENCY; // in minutes; 0 means use cleanupHour instead; t >
    // 0 means run cleanup every t minutes; Max= 24 hr,
    // Min 1 hr
    int diskCachePerformanceLevel = HIGH; // default: 1 means balanced setting which indicates some metadata will be
    // kept in memory.
    // 0 means low setting which indicates limited metadata will be kept im memory.
    // 2 means custom setting which indicates some metadata will be kept im memory.
    // 3 means high setting which indicates all metadata will be kept in memory.
    int diskCacheEntrySizeInMB = DEFAULT_DISKCACHE_ENTRY_SIZE_MB; // default: 0 means disable or maximum size of
    // individual cache entry in MB.
    // Any cache entry larger than this when evicted from memory will not be offloaded to disk
    int diskCacheSizeInGB = DEFAULT_DISKCACHE_SIZE_GB; // default: 0 means disable or maximum disk cache size in GB

    int diskCacheSize = DEFAULT_DISKCACHE_SIZE; // default: 0 means disable or maximum disk cache size

    int diskCacheEvictionPolicy = EVICTION_RANDOM;
    int diskCacheHighThreshold = DEFAULT_HIGH_THRESHOLD;
    int diskCacheLowThreshold = DEFAULT_LOW_THRESHOLD;

    // -------------------------------------------------
    // Config settings - External Cache Groups
    // -------------------------------------------------
    List<ExternalCacheGroup> externalGroups = new ArrayList<ExternalCacheGroup>();

    // -----------------------------------------------------------
    // Config settings
    // -----------------------------------------------------------
    boolean useListenerContext = false;
    boolean disableDependencyId = false;
    boolean enableLockingSupport = false;
    boolean disableTemplatesSupport = false;
    boolean enableReplicationAcks = false;
    boolean enableNioSupport = false;
    boolean enableServletSupport = true;
    boolean propogateInvalidationsNotShared = false;
    boolean alwaysSetSurrogateControlHdr = false;
    String filteredStatusCodes = null;

    // -----------------------------------------------------------
    // Non-WCCM config items
    // -----------------------------------------------------------
    int maxTimeLimitInSeconds = 86400; // used by TimeLimitDaemon
    int configReloadInterval = 5000; // msec
    int timeGranularityInSeconds = DEFAULT_TLD_TIME_GRANULARITY; // used by TimeLimitDaemon

    // time in msec - frequency in which cache entries in memory are aynchronously offloaded to disk cache
    // Note: TLD granularity is a multiple of the lruToDiskTriggerTime
    // Default: 5000 msec or 5 sec (same as timeLimitDaemon)
    // Scope: applicable to all cache instances.
    int lruToDiskTriggerTime = DEFAULT_LRU_TO_DISK_TRIGGER_TIME;

    // Percentage of the memory cache size used as a overflow buffer when disk offload is enabled.
    // Cache entries in the overflow buffer are purged and asynchronously offloaded to disk at a
    // frequency of lruToDiskTriggerTime milliseconds. If the this memory overflow buffer is full,
    // cache entries are offloaded to disk synchronously on the callers thread.
    // Default: 0 - no overflow buffer
    // Scope: configurable per cache instance
    int lruToDiskTriggerPercent = DEFAULT_LRU_TO_DISK_TRIGGER_PERCENT;

    int timeHoldingInvalidations = 200000; // used by InvalidationAuditDaemon
    int htodCleanupHour = 0; // 0 means do it at midnight
    long htodInvalInterval = 24 * 60 * 60 * 1000; // ms
    boolean htodDelayOffload = DEFAULT_DISKCACHE_DELAY_OFFLOAD; // true means use Aux dep table
    int htodDelayOffloadDepIdBuckets = DEFAULT_MAX_BUFFERED_DEPENDENCY_IDS; // limit num of buckets for depid in Aux dep
    // table
    int htodDelayOffloadTemplateBuckets = DEFAULT_MAX_BUFFERED_TEMPLATES; // limit num of buckets for template in Aux
    // dep table
    int htodDelayOffloadEntriesLimit = DEFAULT_MAX_BUFFERED_CACHE_IDS_PER_METADATA; // limit num of cache entries for
    // depid or template in Aux dep
    // table; Min= 100
    int htodDataHashtableSize = 477551; // Hashtable size for disk cache entries
    int htodDepIdHashtableSize = 47743; // Hashtable size for dep id entries
    int htodTemplateHashtableSize = 1031; // Hashtable size for template entries
    int htodNumberOfPools = 20; // Number of primitive array pools
    int htodPoolSize = 2; // Number of primitive arrays in each pool
    int htodPoolEntryLife = DEFAULT_DISKCACHE_POOL_ENTRY_LIFE; // Life is five minutes
    int htodInvalidationBufferSize = 1000; // size of Invalidation buffer to trigger LPBT
    int htodInvalidationBufferLife = 1000 * 10; // Life for Invalidation buffer to trigger LPBT
    boolean htodDependencyCacheIndexEnabled = false;
    int explicitBufferLimitOnStop = DEFAULT_EXPLICIT_BUFFER_LIMIT_ON_STOP;

    int cacheEntryWindow = DEFAULT_ENTRY_WINDOW;
    int cachePercentageWindow = DEFAULT_PERCENTAGE_WINDOW;
    int cacheInvalidateEntryWindow = DEFAULT_ENTRY_WINDOW;
    int cacheInvalidatePercentWindow = DEFAULT_PERCENTAGE_WINDOW;

    public boolean disableTemplateInvalidation = false;
    boolean ignoreValueInInvalidationEvent = false; // default: false
    // false means the value is valid when firing invalidation event
    // true means the value is set to NULL when firing invalidation event
    boolean useServerClassLoader = false;
    boolean cascadeCachespecProperties = false;
    boolean use602RequiredAttrCompatibility = false;
    boolean alwaysTriggerCommandInvalidations = false;
    boolean alwaysSynchronizeOnGets = false;
    boolean ignoreCacheableCommandDeserializationException = false;

    String disableStoreCookies = "none";
    boolean cacheInstanceStoreCookies = true;
    // -----------------------------------------------------------
    String topology = ""; // capturing the topology of a provider
    String libraryRef = null;

    // -----------------------------------------------------------
    // Users of this config
    // -----------------------------------------------------------
    DistributedObjectCache distributedObjectCache = null;
    DCache cache = null;
    ConcurrentHashMap _passedInProperties = new ConcurrentHashMap();
    // -----------------------------------------------------------

    boolean refCountTracking = false;
    boolean enableInterCellInvalidation = false;
    int[] statusCodesArray = null;

    public static class ExternalCacheGroup {
        public String name;
        public int type;
        public List<ExternalCacheGroupMember> members = new ArrayList<ExternalCacheGroupMember>(0);
    }

    public static class ExternalCacheGroupMember {
        public String address;
        public String beanName;
    }

    public static Properties convert(Map<String, Object> map) {
        Properties p = new Properties();
        Set<Map.Entry<String, Object>> set = map.entrySet();
        for (Map.Entry<String, Object> entry : set) {
            p.put(entry.getKey(), entry.getValue());
        }
        return p;
    }

    public CacheConfig() {
        FieldInitializer.initFromSystemProperties(this);
        determineCacheProvider();
    }

    // ------------------------------------------------------
    // Build cache config with defaults.
    // Set system proeprties to override defaults.
    // ------------------------------------------------------
    public CacheConfig(Map<String, Object> map) {

        FieldInitializer.initFromSystemProperties(this);
        overrideCacheConfig(convert(map));
        determineCacheProvider();
        _passedInProperties.putAll(map);
        _passedInProperties.putAll(System.getProperties());

        WsLocationAdmin locAdmin = Scheduler.getLocationAdmin();
        // allow to run outside of OSGI for Unit tests
        if (locAdmin != null) {
            serverServerName = locAdmin.getServerName();
        }

        if (filteredStatusCodes != null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "filteredStatusCodes: " + filteredStatusCodes);
            }
            StringTokenizer st = new StringTokenizer(filteredStatusCodes);
            statusCodesArray = new int[st.countTokens()];
            int i = 0;
            while (st.hasMoreTokens()) {
                try {
                    statusCodesArray[i] = Integer.parseInt(st.nextToken());
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "added filteredStatusCodes: " + statusCodesArray[i]);
                    }
                } catch (NumberFormatException ex) {
                    Tr.error(tc, "Error parsing filteredStatusCodes: " + filteredStatusCodes);
                    statusCodesArray[i] = -1;
                }
                i++;
            }
        }
    }

    public CacheConfig(Properties properties, CacheConfig config) {

        serverServerName = Scheduler.getLocationAdmin().getServerName();
        cachePriority = config.cachePriority;
        jspCachePriority = config.jspCachePriority;
        commandCachePriority = config.commandCachePriority;
        diskHashBuckets = config.diskHashBuckets;

        // -------------------------------------------------
        // DynamicCache settings - Cache Size
        // Common for ServletCacheInstance and ObjectCacheInstance
        // -------------------------------------------------
        cacheSize = config.cacheSize;
        cacheProviderName = config.cacheProviderName;

        // -------------------------------------------------
        // DynamicCache settings - Replication Defaults
        // -------------------------------------------------
        cacheProviderName = config.cacheProviderName;
        enableCacheReplication = config.enableCacheReplication;
        replicationType = config.replicationType;

        // -------------------------------------------------
        // DynamicCache settings - Replication
        // -------------------------------------------------
        setBatchUpdateInterval(config, config.pushFrequency);

        // -------------------------------------------------
        // DynamicCache settings - HTOD
        // -------------------------------------------------
        enableDiskOffload = false;
        flushToDiskOnStop = false;

        diskCacheSizeInGB = config.diskCacheSizeInGB;
        diskCacheSize = config.diskCacheSize;
        diskCacheEntrySizeInMB = config.diskCacheEntrySizeInMB;
        diskCachePerformanceLevel = config.diskCachePerformanceLevel;
        htodCleanupFrequency = config.htodCleanupFrequency;
        diskCacheEvictionPolicy = config.diskCacheEvictionPolicy;
        diskCacheHighThreshold = config.diskCacheHighThreshold;
        diskCacheLowThreshold = config.diskCacheLowThreshold;
        htodDelayOffloadEntriesLimit = config.htodDelayOffloadEntriesLimit;
        htodDelayOffloadDepIdBuckets = config.htodDelayOffloadDepIdBuckets;
        htodDelayOffloadTemplateBuckets = config.htodDelayOffloadTemplateBuckets;

        // -------------------------------------------------
        // Override config
        // -------------------------------------------------
        FieldInitializer.initFromSystemProperties(this);
        overrideCacheConfig(properties);
        _passedInProperties.putAll(properties);
        _passedInProperties.putAll(System.getProperties());
        determineCacheProvider();

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "CacheConfig():1 cacheName=" + cacheName);
        }
    }

    protected void reset() {
        tempDir = null;
        dtdDir = null;
        diskOffloadLocation = null;
        cacheName = null;
        distributedObjectCache = null;
        cache = null;
        serverServerName = null;
        if (externalGroups != null) {
            externalGroups.clear();
        }
    }

    private void processOffloadDirectory() {

        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "processOffloadDirectory", diskOffloadLocation, enableDiskOffload);
        }

        if (null == tempDir) {
            WsLocationAdmin locationAdmin = Scheduler.getLocationAdmin();
            if (null == locationAdmin) {
                tempDir = System.getProperty("java.io.tmpdir");
            } else {
                tempDir = Scheduler.getLocationAdmin().resolveString(WsLocationConstants.SYMBOL_SERVER_WORKAREA_DIR);
            }
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "cache default dir ", tempDir);
            }
        }

        if (enableDiskOffload) {
            if (diskOffloadLocation != null && !diskOffloadLocation.isEmpty()) {
                diskOffloadLocation = Scheduler.getLocationAdmin().resolveString(diskOffloadLocation);
            } else {
                diskOffloadLocation = tempDir + "_dynacache";
            }
        }

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "processOffloadDirectory", diskOffloadLocation);
        }

    }

    // Override config settings
    // used by com.ibm.ws.cache.spi.DistributedMapFactory
    public void overrideCacheConfig(Properties properties) {
        if (properties != null) {
            FieldInitializer.initFromSystemProperties(this, properties);
        }
        processOffloadDirectory();
        if (!this.enableServletSupport) {
            this.disableTemplatesSupport = true;
        }
    }

    @Override
    public Object clone() {
        Object o = null;
        try {
            o = super.clone();
        } catch (Exception e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.cache.CacheConfig", "314");
        }
        // --------------------------------------------------------------------
        // Since the DistributedObjectCacheFactory is cloning baseBase config,
        // we have to reset a few config items to prevent problems creating
        // the new cache. The clone will contain everthing else from the
        // donor.
        // --------------------------------------------------------------------
        ((CacheConfig) o).cacheName = null;
        ((CacheConfig) o).jndiName = null;
        ((CacheConfig) o).cache = null;
        ((CacheConfig) o).distributedObjectCache = null;
        ((CacheConfig) o).enableServletSupport = false;
        ((CacheConfig) o).enableDiskOffload = false;
        ((CacheConfig) o).flushToDiskOnStop = false;
        ((CacheConfig) o).disableDependencyId = false;
        ((CacheConfig) o).disableTemplatesSupport = false;
        return o;
    }

    @Override
    public int getBatchUpdateInterval() {
        return batchUpdateInterval;
    }

    @Override
    public int getCachePriority() {
        return cachePriority;
    }

    @Override
    public int getCacheSize() {
        return cacheSize;
    }

    public int getCacheSizeInMB() {
        return memoryCacheSizeInMB;
    }

    @Override
    public int getCleanupFrequency() {
        return htodCleanupFrequency;
    }

    @Override
    public int getDelayOffloadDepIdBuckets() {
        return htodDelayOffloadDepIdBuckets;
    }

    @Override
    public int getDelayOffloadEntriesLimit() {
        return htodDelayOffloadEntriesLimit;
    }

    @Override
    public int getDelayOffloadTemplateBuckets() {
        return htodDelayOffloadTemplateBuckets;
    }

    @Override
    public int getDiskCacheEntrySizeInMB() {
        return diskCacheEntrySizeInMB;
    }

    @Override
    public int getDiskCacheEvictionPolicy() {
        return diskCacheEvictionPolicy;
    }

    @Override
    public int getDiskCacheHighThreshold() {
        return diskCacheHighThreshold;
    }

    @Override
    public int getDiskCacheLowThreshold() {
        return diskCacheLowThreshold;
    }

    @Override
    public int getDiskCachePerformanceLevel() {
        return diskCachePerformanceLevel;
    }

    // Batching parameters for Dynacache - DRS interaction

    @Override
    public int getDiskCacheSize() {
        return diskCacheSize;
    }

    @Override
    public int getDiskCacheSizeInGB() {
        return diskCacheSizeInGB;
    }

    @Override
    public int getEntryWindow() {
        return cacheEntryWindow;
    }

    @Override
    public int getHighThresholdCacheSizeInMB() {
        return this.memoryCacheHighThreshold;
    }

    @Override
    public int getLowThresholdCacheSizeInMB() {
        return this.memoryCacheLowThreshold;
    }

    @Override
    public int getInvalidateEntryWindow() {
        return cacheInvalidateEntryWindow;
    }

    @Override
    public int getInvalidatePercentageWindow() {
        return this.cacheInvalidatePercentWindow;
    }

    @Override
    public int getLruToDiskTriggerPercent() {
        return lruToDiskTriggerPercent;
    }

    @Override
    public int getLruToDiskTriggerTime() {
        return lruToDiskTriggerTime;
    }

    @Override
    public long getMaxCacheSize() {
        return cacheSize;
    }

    @Override
    public long getMaxCacheSizeInMB() {
        return memoryCacheSizeInMB;
    }

    @Override
    public int getPercentageWindow() {
        return cachePercentageWindow;
    }

    @Override
    public int getCongestionSleepTimeMilliseconds() {
        return congestionSleepTimeMilliseconds;
    }

    @Override
    public boolean isCacheInstanceStoreCookies() {
        return cacheInstanceStoreCookies;
    }

    @Override
    public boolean isCascadeCachespecProperties() {
        return this.cascadeCachespecProperties;
    }

    @Override
    public boolean isDelayOffload() {
        return this.htodDelayOffload;
    }

    @Override
    public boolean isDrsBootstrapEnabled() {
        return this.drsBootstrapEnabled;
    }

    @Override
    public boolean isDrsDisabled() {
        return this.drsDisabled;
    }

    @Override
    public boolean isEnableCacheReplication() {
        return this.enableCacheReplication;
    }

    @Override
    public boolean isEnableDiskOffload() {
        return this.enableDiskOffload;
    }

    @Override
    public boolean isEnableServletSupport() {
        return this.enableServletSupport;
    }

    @Override
    public boolean isFilterLRUInvalidation() {
        return this.filterLRUInvalidation;
    }

    @Override
    public boolean isFilterTimeOutInvalidation() {
        return this.filterTimeOutInvalidation;
    }

    @Override
    public boolean isFilterInactivityInvalidation() {
        return this.filterInactivityInvalidation;
    }

    @Override
    public boolean isFlushToDiskOnStop() {
        return this.flushToDiskOnStop;
    }

    @Override
    public boolean isUseServerClassLoader() {
        return this.useServerClassLoader;
    }

    @Override
    public void setCacheInstanceStoreCookies(boolean cacheInstanceStoreCookies) {
        this.cacheInstanceStoreCookies = cacheInstanceStoreCookies;
    }

    @Override
    public void setCachePriority(int cachePriority) {
        if (cachePriority < 0) {
            cachePriority = DEFAULT_PRIORITY;
        }
        this.cachePriority = cachePriority;
    }

    // Used by test cases
    public void setDiskOffloadLocation(String s) {
        this.diskOffloadLocation = s;
    }

    @Override
    public void setDrsBootstrapEnabled(boolean drsBootstrapEnabled) {
        this.drsBootstrapEnabled = drsBootstrapEnabled;
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setDRSBootstrap() cacheName=" + this.cacheName + " drsBootstrap=" + drsBootstrapEnabled);
    }

    @Override
    public void setDrsDisabled(boolean drsDisabled) {
        this.drsDisabled = drsDisabled;
    }

    // Used by test cases
    public void setEnableDiskOffload(boolean b) {
        this.enableDiskOffload = b;
        processOffloadDirectory();
    }

    // Used by test cases
    public void setEnableNioSupport(boolean b) {
        this.enableNioSupport = b;
    }

    public void setMaxCacheSize(int size) {
        this.cacheSize = size;
    }

    public void setOffloadOffloadLocationAndProcess(String s) {
        this.diskOffloadLocation = s;
        processOffloadDirectory();
    }

    @Override
    public String getCacheName() {
        return cacheName;
    }

    @Override
    public int getDefaultShareType() {
        return defaultShareType;
    }

    @Override
    public boolean isDisableDependencyId() {
        return disableDependencyId;
    }

    @Override
    public boolean isEnableNioSupport() {
        return enableNioSupport;
    }

    @Override
    public String getServerServerName() {
        return serverServerName;
    }

    @Override
    public EvictorAlgorithmType getEvictorAlgorithmType() {
        return EvictorAlgorithmType.LRUEvictor;
    }

    @Override
    public boolean isDistributed() {
        return isEnableCacheReplication();
    }

    @Override
    public int getReplicationPayloadSizeInMB() {
        return replicationPayloadSizeInMB;
    }

    @Override
    public String getCacheProviderName() {
        return cacheProviderName;
    }

    @Override
    public boolean isAutoFlushIncludes() {
        return autoFlushIncludes;
    }

    @Override
    public boolean alwaysSetSurrogateControlHdr() {
        return alwaysSetSurrogateControlHdr;
    }

    @Override
    public boolean isUse602RequiredAttrCompatibility() {
        return use602RequiredAttrCompatibility;
    }

    @Override
    public boolean alwaysTriggerCommandInvalidations() {
        return alwaysTriggerCommandInvalidations;
    }

    @Override
    public boolean alwaysSynchronizeOnGets() {
        return alwaysSynchronizeOnGets;
    }

    @Override
    public void setAlwaysSynchronizeOnGets(boolean alwaysSynchronizeOnGets) {
        this.alwaysSynchronizeOnGets = alwaysSynchronizeOnGets;
    }

    @Override
    public int[] getFilteredStatusCodes() {
        return statusCodesArray;
    }

    @Trivial
    public boolean isRefCountTrackingEnabled() {
        return refCountTracking;
    }

    @Override
    public boolean isEnableInterCellInvalidation() {
        return enableInterCellInvalidation;
    }

    @Override
    public boolean isWebservicesSetRequiredTrue() {
        return webservicesSetRequiredTrue;
    }

    /**
     * Determines if default cache provider is being used and sets flag accordingly.
     *
     */
    public void determineCacheProvider() {
        this.defaultProvider = true;
        if (cacheProviderName.equals("")) {
            cacheProviderName = CacheConfig.CACHE_PROVIDER_DYNACACHE;
        }
        if (!cacheProviderName.equals(CACHE_PROVIDER_DYNACACHE)) {
            defaultProvider = false;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Alternate CacheProvider " + cacheProviderName + " set for " + cacheName);
            }
        }
    }

    @Override
    @Trivial
    public boolean isDefaultCacheProvider() {
        return defaultProvider;
    }

    // only called when the alternate cache provider could not create the cache.. we need to then revert to the default
    public void resetProvider(String cacheName) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Reverting to the default Dynacache cache provider");
        }
        this.cacheProviderName = CACHE_PROVIDER_DYNACACHE;
        this.enableCacheReplication = false;
        this.defaultProvider = true;
        this.cacheName = cacheName;
    }

    /**
     * This method reverts the common configuration template for all cache instances to use Dynaache defaults. This
     * method only comes into play when ObjectGrid is configured as the cache provider for the default cache.
     */
    void restoreDynacacheProviderDefaults() { // restore commonConfig to Dynacache defaults
        if (restoreDynacacheDefaults) {
            if (cacheProviderName != CacheConfig.CACHE_PROVIDER_DYNACACHE) {
                cacheProviderName = CacheConfig.CACHE_PROVIDER_DYNACACHE;
                enableCacheReplication = false;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "OVERRIDING Object Grid default for " + cacheName);
                }
            }
        }
    }

    @Override
    public boolean isIgnoreCacheableCommandDeserializationException() {
        return ignoreCacheableCommandDeserializationException;
    }

    private void setBatchUpdateInterval(Object config, int pushFrequency) {

        if (tc.isDebugEnabled()) {
            Tr.entry(tc, "setBatchUpdateInterval", new Object[] { config });
        }

        if (pushFrequency < 1) {
            pushFrequency = 1;
        }

        // If we have batchUpdateMilliseconds defined, use it. Otherwise, use old method
        if (batchUpdateMilliseconds == -1) {
            batchUpdateInterval = pushFrequency * 1000;
        }

        if (tc.isDebugEnabled()) {
            Tr.exit(tc, "setBatchUpdateInterval", new Integer(batchUpdateInterval));
        }
    }

    public void setDiskCacheEvictionPolicy(int evictionPolicy) {
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, "setDiskCacheEvictionPolicy", new Integer(evictionPolicy));
        }

        if (evictionPolicy >= CacheConfig.EVICTION_NONE && evictionPolicy <= CacheConfig.EVICTION_SIZE_BASED)
            diskCacheEvictionPolicy = evictionPolicy;

        if (tc.isDebugEnabled()) {
            Tr.entry(tc, "setDiskCacheEvictionPolicy", new Integer(diskCacheEvictionPolicy));
        }
    }

    @Override
    public String getServerNodeName() {
        return null;
    }

    @Override
    public Map<String, String> getProperties() {
        return _passedInProperties;
    }

    @Override
    public String toString() {
        return "CacheConfig [ " + "  cacheName=" + cacheName + ", jndiName=" + jndiName + ", cacheSize=" + cacheSize + ", cache=" + cache
               + ", libraryRef=" + libraryRef + ", createCacheAtServerStartup=" + createCacheAtServerStartup + ", distributedObjectCache="
               + distributedObjectCache + ", defaultProvider=" + defaultProvider + ", enableCacheReplication=" + enableCacheReplication
               + ", enableDiskOffload=" + enableDiskOffload + ", enableNioSupport=" + enableNioSupport + ", alwaysSetSurrogateControlHdr="
               + alwaysSetSurrogateControlHdr + ", externalGroups=" + externalGroups + ", alwaysSynchronizeOnGets=" + alwaysSynchronizeOnGets
               + ", alwaysTriggerCommandInvalidations=" + alwaysTriggerCommandInvalidations + ", cacheInstanceStoreCookies="
               + cacheInstanceStoreCookies + ", cachePercentageWindow=" + cachePercentageWindow + ", cachePriority=" + cachePriority
               + ", cacheProviderName=" + cacheProviderName + ", cascadeCachespecProperties=" + cascadeCachespecProperties
               + ", commandCachePriority=" + commandCachePriority + ", configReloadInterval=" + configReloadInterval + ", disableDependencyId="
               + disableDependencyId + ", disableStoreCookies=" + disableStoreCookies + ", disableTemplateInvalidation="
               + disableTemplateInvalidation + ", disableTemplatesSupport=" + disableTemplatesSupport + ", diskCacheEntrySizeInMB="
               + diskCacheEntrySizeInMB + ", diskCacheEvictionPolicy=" + diskCacheEvictionPolicy + ", diskCacheHighThreshold="
               + diskCacheHighThreshold + ", diskCacheLowThreshold=" + diskCacheLowThreshold + ", diskCachePerformanceLevel="
               + diskCachePerformanceLevel + ", diskCacheSize=" + diskCacheSize + ", diskCacheSizeInGB=" + diskCacheSizeInGB + ", diskHashBuckets="
               + diskHashBuckets + ", diskOffloadLocation=" + diskOffloadLocation + ", htodCleanupFrequency=" + htodCleanupFrequency
               + ", htodCleanupHour=" + htodCleanupHour + ", htodDataHashtableSize=" + htodDataHashtableSize + ", htodDelayOffload="
               + htodDelayOffload + ", htodDelayOffloadDepIdBuckets=" + htodDelayOffloadDepIdBuckets + ", htodDelayOffloadEntriesLimit="
               + htodDelayOffloadEntriesLimit + ", htodDelayOffloadTemplateBuckets=" + htodDelayOffloadTemplateBuckets + ", htodDepIdHashtableSize="
               + htodDepIdHashtableSize + ", htodDependencyCacheIndexEnabled=" + htodDependencyCacheIndexEnabled + ", htodInvalInterval="
               + htodInvalInterval + ", htodInvalidationBufferLife=" + htodInvalidationBufferLife + ", htodInvalidationBufferSize="
               + htodInvalidationBufferSize + ", htodNumberOfPools=" + htodNumberOfPools + ", htodPoolEntryLife=" + htodPoolEntryLife
               + ", htodPoolSize=" + htodPoolSize + ", htodTemplateHashtableSize=" + htodTemplateHashtableSize + ", lruToDiskTriggerPercent="
               + lruToDiskTriggerPercent + ", lruToDiskTriggerTime=" + lruToDiskTriggerTime + ", explicitBufferLimitOnStop="
               + explicitBufferLimitOnStop + ", drsBootstrapEnabled=" + drsBootstrapEnabled + ", drsDisabled=" + drsDisabled + ", dtdDir=" + dtdDir
               + ", filterInactivityInvalidation=" + filterInactivityInvalidation + ", filterLRUInvalidation=" + filterLRUInvalidation
               + ", filterTimeOutInvalidation=" + filterTimeOutInvalidation + ", filteredStatusCodes=" + filteredStatusCodes
               + ", flushToDiskOnStop=" + flushToDiskOnStop + ", ignoreCacheableCommandDeserializationException="
               + ignoreCacheableCommandDeserializationException + ", ignoreValueInInvalidationEvent=" + ignoreValueInInvalidationEvent
               + ", jspCachePriority=" + jspCachePriority + ", memoryCacheHighThreshold=" + memoryCacheHighThreshold + ", memoryCacheLowThreshold="
               + memoryCacheLowThreshold + ", memoryCacheSizeInMB=" + memoryCacheSizeInMB + ", refCountTracking=" + refCountTracking
               + ", serverServerName=" + serverServerName + ", statusCodesArray=" + Arrays.toString(statusCodesArray) + ", tempDir=" + tempDir
               + ", batchUpdateInterval=" + batchUpdateInterval + ", batchUpdateMilliseconds=" + batchUpdateMilliseconds
               + ", timeGranularityInSeconds=" + timeGranularityInSeconds + ", maxTimeLimitInSeconds=" + maxTimeLimitInSeconds
               + ", timeHoldingInvalidations=" + timeHoldingInvalidations + ", getClass()=" + getClass() + ", hashCode()=" + hashCode()
               + ", toString()=" + super.toString() + "]";
    }

    /**
     * @return the createCacheAtServerStartup
     */
    public boolean isCreateCacheAtServerStartup() {
        return createCacheAtServerStartup;
    }

    /**
     * @return the distributedObjectCache
     */
    public DistributedObjectCache getDistributedObjectCache() {
        return distributedObjectCache;
    }

    public void setDistributedObjectCache(DistributedObjectCache cache) {
        distributedObjectCache = cache;
    }

    /**
     * @return the cache
     */
    public DCache getCache() {
        return cache;
    }

    /**
     * @param cache the cache to set
     */
    public void setCache(DCache cache) {
        this.cache = cache;
    }

    /**
     * @return the externalGroups
     */
    public List<ExternalCacheGroup> getExternalGroups() {
        return externalGroups;
    }

    /**
     * @return the jspCachePriority
     */
    public int getJspCachePriority() {
        return jspCachePriority;
    }

}
