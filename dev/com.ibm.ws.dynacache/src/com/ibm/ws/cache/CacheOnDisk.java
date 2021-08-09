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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.Future;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.cache.intf.DCache;
import com.ibm.ws.cache.util.ExceptionUtility;

public class CacheOnDisk implements DynacacheOnDisk {

    private static final boolean IS_UNIT_TEST = false;

    private static final long SECONDS_FOR_24_HOURS = 24 * 60 * 60;
    private static final long SECONDS_FOR_23_HOURS = 23 * 60 * 60;

    // constants used for check htod.properites
    public final static byte PROPERTY_FILE_OK = 0;
    public final static byte PROPERTY_ERROR_NO_FILES = (byte) 0x01;
    public final static byte PROPERTY_ERROR_FILE_NOT_EXIST = (byte) 0x02;
    public final static byte PROPERTY_ERROR_FILE_CORRUPT = (byte) 0x04;
    public final static byte PROPERTY_ERROR_DISABLE_DEPID = (byte) 0x08;
    public final static byte PROPERTY_ERROR_DISABLE_TEMPLATE = (byte) 0x10;
    public final static byte PROPERTY_ERROR_CACHE_SIZE = (byte) 0x20;
    public final static byte PROPERTY_ERROR_FIELD_CHECK = (byte) 0x40;
    public final static byte PROPERTY_ERROR_GB = (byte) 0x80;

    // properties
    public final static String HTOD_VERSION = "version";
    public final static String HTOD_VERSION_NUM = "6.0";
    public final static String DISABLE_DEPENDENCY_ID = "disableDependencyId";
    public final static String DISABLE_TEMPLATE_SUPPORT = "disableTemplatesSupport";
    public final static String CACHE_SIZE_IN_BYTES = "cacheSizeInBytes";
    public final static String FIELD_CHECK = "fieldCheck";
    public final static String DATA_GB = "dataGB";
    public final static String DEPID_GB = "dependencyIdGB";
    public final static String TEMPLATE_GB = "templateGB";

    // fileNames
    public final static String HTOD_LAST_SCAN_FILENAME = "lastscantime";
    public final static String HTOD_PROPERTIES_FILENAME = "htod.properties";
    public final static String HTOD_INVALIDATION_FILENAME = "invalidations.htod";
    public final static String HTOD_IN_PROGRESS_FILENAME = "InProgress";

    // constants used for checking htod.properites
    public final static int START_NONE = 0;
    public final static int START_LPBT_SCAN = 1;
    public final static int START_LPBT_REMOVE = 2;

    // constant used to filter the ids that to be removed pending
    public final static boolean FILTER = true;

    // constant used to delete inPregress dummy file
    public final static boolean DELETE_IN_PROGRESS_FILE = true;

    // constants used for determining the type of garage collector
    public final static int DISK_CACHE_SIZE_IN_ENTRIES_TYPE = 1;
    public final static int DISK_CACHE_SIZE_IN_BYTES_TYPE = 2;

    // constants used for checkDirectoryWriteable return code
    public static final int LOCATION_OK = 0;
    public static final int LOCATION_NOT_DEFINED = 1;
    public static final int LOCATION_NOT_VALID = 2;
    public static final int LOCATION_NOT_DIRECTORY = 3;
    public static final int LOCATION_NOT_WRITABLE = 4;
    public static final int LOCATION_CANNOT_MAKE_DIR = 5;

    public final static int GC_THRESHOLD = 20; // 20% left

    private static TraceComponent tc = Tr.register(CacheOnDisk.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");
    protected String sep = File.separator;
    // this will be appended to the WAS install root plus the logs directory
    protected String swapDirPath = null;
    protected String swapRootDirPath = null;
    protected String cacheName = null;
    protected String diskCacheName = null;
    // protected String multiCleanupSync = null;
    // default of 30 minutes to check cleaup hour
    protected long sleepInterval = 30 * 60 * 1000;// 4 * 60 * 60 * 1000;
    protected int cleanupHour = 0; // means 12 am
    // default of 24 hours in milliseconds
    protected long invalInterval = 24 * 60 * 60 * 1000;
    protected int cleanupFrequency = CacheConfig.DEFAULT_DISKCACHE_CLEANUP_FREQUENCY; // in minutes; 0 means use
                                                                                      // cleanupHour instead; t > 0
                                                                                      // means run cleanup every t
                                                                                      // minutes; Max= 24 hr, Min 1 hr
    protected boolean delayOffload; // true means use Aux dep table
    protected int delayOffloadEntriesLimit = CacheConfig.DEFAULT_MAX_BUFFERED_CACHE_IDS_PER_METADATA; // limit num of
                                                                                                      // cache entries
                                                                                                      // for depid or
                                                                                                      // template in Aux
                                                                                                      // dep table; Min=
                                                                                                      // 100
    protected int delayOffloadDepIdBuckets = CacheConfig.DEFAULT_MAX_BUFFERED_DEPENDENCY_IDS; // limit num of buckets
                                                                                              // for depid or template
                                                                                              // in Aux dep table;
    protected int delayOffloadTemplateBuckets = CacheConfig.DEFAULT_MAX_BUFFERED_TEMPLATES; // limit num of buckets
                                                                                            // template in Aux dep
                                                                                            // table; Min= 100
    protected boolean dependencyCacheIndexEnabled = false;

    protected int dataHashtableSize;
    protected int depIdHashtableSize;
    protected int templateHashtableSize;
    protected int numberOfPools;
    protected int poolSize;
    protected int poolEntryLife;
    protected int invalidationBufferSize;
    protected int invalidationBufferLife;
    protected boolean disableDependencyId = false;
    protected boolean disableTemplatesSupport = false;
    protected boolean disableDependencyIdFromPropertyFile = false;
    protected boolean disableTemplatesSupprotFromPropertyFile = false;
    protected int dataFiles = 0;
    protected int dependencyIdFiles = 0;
    protected int templateFiles = 0;
    protected boolean ignoreValueInInvalidationEvent = false;
    protected int diskCachePerformanceLevel = CacheConfig.DEFAULT_DISKCACHE_PERFORMANCE_LEVEL;
    protected int explicitBufferLimitOnStop = CacheConfig.DEFAULT_EXPLICIT_BUFFER_LIMIT_ON_STOP;

    protected long sleepTime = 0;
    protected boolean setAlarm = true;
    protected String lastScanFileName = null;
    protected String htodPropertyFileName = null;
    protected String invalidationFileName = null;
    protected String inProgressFileName = null;

    protected final String HTOD_PREF = "_dynacache";

    protected DCache cache = null;

    protected HTODDynacache htod = null;

    protected long lastScanTime = 0;
    protected byte propertyFileStatus = PROPERTY_FILE_OK;
    protected ValueSet valueSet = null;
    protected boolean stopping = false;

    protected int startState = START_NONE;
    protected boolean enableCacheSizeInBytes = false;
    protected long currentCacheSizeInBytes = 0;
    protected int dataGB = 1;
    protected int dependencyIdGB = 1;
    protected int templateGB = 1;

    protected boolean populateEvictionTable = false; // 3821 NK begin
    protected boolean populateDependencyTable = false;
    protected GarbageCollectorThread garbageCollectionThread = null;
    protected DiskCleanupThread diskCleanupThread = null;
    protected int evictionPolicy = CacheConfig.DEFAULT_DISKCACHE_EVICTION_POLICY;
    protected DiskCacheSizeInfo diskCacheSizeInfo = null;

    protected boolean doNotify = false;
    protected Object diskCacheMonitor = new Object() {
    };

    private String definedLocation = "";
    private String alternateLocation = "";

    public CacheOnDisk(CacheConfig cacheConfig, DCache c) {

        final String methodName = "constructor()";
        if (cacheConfig.diskOffloadLocation == null) {
            cacheConfig.setOffloadOffloadLocationAndProcess("");
        }
        this.cache = c;
        this.cacheName = c.getCacheName();
        String location = cacheConfig.diskOffloadLocation;
        this.cleanupHour = cacheConfig.htodCleanupHour;
        this.invalInterval = cacheConfig.htodInvalInterval;
        this.cleanupFrequency = cacheConfig.htodCleanupFrequency;
        this.delayOffload = cacheConfig.htodDelayOffload;
        this.delayOffloadEntriesLimit = cacheConfig.htodDelayOffloadEntriesLimit;
        this.delayOffloadDepIdBuckets = cacheConfig.htodDelayOffloadDepIdBuckets;
        this.delayOffloadTemplateBuckets = cacheConfig.htodDelayOffloadTemplateBuckets;
        this.dependencyCacheIndexEnabled = cacheConfig.htodDependencyCacheIndexEnabled;
        this.dataHashtableSize = cacheConfig.htodDataHashtableSize;
        this.depIdHashtableSize = cacheConfig.htodDepIdHashtableSize;
        this.templateHashtableSize = cacheConfig.htodTemplateHashtableSize;
        this.numberOfPools = cacheConfig.htodNumberOfPools;
        this.poolSize = cacheConfig.htodPoolSize;
        this.poolEntryLife = cacheConfig.htodPoolEntryLife;
        this.invalidationBufferSize = cacheConfig.htodInvalidationBufferSize;
        this.invalidationBufferLife = cacheConfig.htodInvalidationBufferLife;
        this.disableDependencyId = cacheConfig.disableDependencyId;
        this.disableTemplatesSupport = cacheConfig.disableTemplatesSupport;
        if (cacheConfig.enableServletSupport) {
            this.disableTemplatesSupport = cacheConfig.disableTemplatesSupport;
        } else {
            this.disableTemplatesSupport = true;
        }
        this.ignoreValueInInvalidationEvent = cacheConfig.ignoreValueInInvalidationEvent;
        this.diskCachePerformanceLevel = cacheConfig.diskCachePerformanceLevel;
        this.evictionPolicy = cacheConfig.diskCacheEvictionPolicy;
        this.explicitBufferLimitOnStop = cacheConfig.explicitBufferLimitOnStop;

        int diskCacheSizeLimit = cacheConfig.diskCacheSize; // 3821 NK begin
        int diskCacheSizeInGBLimit = cacheConfig.diskCacheSizeInGB;
        int diskCacheEntrySizeInMBLimit = cacheConfig.diskCacheEntrySizeInMB;
        int highThreshold = cacheConfig.diskCacheHighThreshold;
        int lowThreshold = cacheConfig.diskCacheLowThreshold;
        this.diskCacheSizeInfo = new DiskCacheSizeInfo(this.cacheName);

        if (this.diskCachePerformanceLevel < CacheConfig.MIN_DISKCACHE_PERFORMANCE_LEVEL
            || this.diskCachePerformanceLevel > CacheConfig.MAX_DISKCACHE_PERFORMANCE_LEVEL) {
            Tr.warning(tc, "DYNA0069W", new Object[] { new Integer(this.diskCachePerformanceLevel), "diskCachePerformanceLevel", this.cacheName,
                                                       new Integer(CacheConfig.MIN_DISKCACHE_PERFORMANCE_LEVEL), new Integer(CacheConfig.MAX_DISKCACHE_PERFORMANCE_LEVEL),
                                                       new Integer(CacheConfig.DEFAULT_DISKCACHE_PERFORMANCE_LEVEL) });
            this.diskCachePerformanceLevel = CacheConfig.DEFAULT_DISKCACHE_PERFORMANCE_LEVEL;
        }

        if (this.diskCachePerformanceLevel == CacheConfig.HIGH || this.diskCachePerformanceLevel == CacheConfig.CUSTOM
            || diskCachePerformanceLevel == CacheConfig.BALANCED) {
            delayOffload = true;
            if (this.diskCachePerformanceLevel == CacheConfig.BALANCED) {
                this.delayOffloadEntriesLimit = CacheConfig.DEFAULT_MAX_BUFFERED_CACHE_IDS_PER_METADATA;
                this.delayOffloadDepIdBuckets = CacheConfig.DEFAULT_MAX_BUFFERED_DEPENDENCY_IDS;
                this.delayOffloadTemplateBuckets = CacheConfig.DEFAULT_MAX_BUFFERED_TEMPLATES;
            } else if (this.diskCachePerformanceLevel == CacheConfig.HIGH) {
                this.delayOffloadEntriesLimit = Integer.MAX_VALUE;
                this.delayOffloadDepIdBuckets = Integer.MAX_VALUE;
                this.delayOffloadTemplateBuckets = Integer.MAX_VALUE;
            } else {
                if (this.delayOffloadEntriesLimit < CacheConfig.MIN_DISKCACHE_BUFFERED_CACHE_IDS_PER_METADATA) {
                    Tr.warning(tc, "DYNA0069W", new Object[] { new Integer(delayOffloadEntriesLimit), "htodDelayOffloadEntriesLimit", this.cacheName,
                                                               new Integer(CacheConfig.MIN_DISKCACHE_BUFFERED_CACHE_IDS_PER_METADATA),
                                                               new Integer(CacheConfig.MAX_DISKCACHE_BUFFERED_CACHE_IDS_PER_METADATA),
                                                               new Integer(CacheConfig.MIN_DISKCACHE_BUFFERED_CACHE_IDS_PER_METADATA) });
                    this.delayOffloadEntriesLimit = CacheConfig.MIN_DISKCACHE_BUFFERED_CACHE_IDS_PER_METADATA;
                }
                if (this.delayOffloadDepIdBuckets < CacheConfig.MIN_DISKCACHE_BUFFERED_DEPENDENCY_IDS) {
                    Tr.warning(tc, "DYNA0069W", new Object[] { new Integer(delayOffloadDepIdBuckets), "htodDelayOffloadDepIdBuckets", this.cacheName,
                                                               new Integer(CacheConfig.MIN_DISKCACHE_BUFFERED_DEPENDENCY_IDS),
                                                               new Integer(CacheConfig.MAX_DISKCACHE_BUFFERED_DEPENDENCY_IDS),
                                                               new Integer(CacheConfig.MIN_DISKCACHE_BUFFERED_DEPENDENCY_IDS) });
                    this.delayOffloadDepIdBuckets = CacheConfig.MIN_DISKCACHE_BUFFERED_DEPENDENCY_IDS;
                }
                if (this.delayOffloadTemplateBuckets < CacheConfig.MIN_DISKCACHE_BUFFERED_TEMPLATES) {
                    Tr.warning(tc, "DYNA0069W",
                               new Object[] { new Integer(delayOffloadTemplateBuckets), "htodDelayOffloadTemplateBuckets",
                                              this.cacheName, new Integer(CacheConfig.MIN_DISKCACHE_BUFFERED_TEMPLATES),
                                              new Integer(CacheConfig.MAX_DISKCACHE_BUFFERED_TEMPLATES), new Integer(CacheConfig.MIN_DISKCACHE_BUFFERED_TEMPLATES) });
                    this.delayOffloadTemplateBuckets = CacheConfig.MIN_DISKCACHE_BUFFERED_TEMPLATES;
                }
            }
        } else {
            delayOffload = false;
        }
        if (this.diskCachePerformanceLevel != CacheConfig.HIGH) {
            if (this.cleanupFrequency < CacheConfig.MIN_CLEANUP_FREQUENCY) {
                Tr.warning(tc, "DYNA0069W", new Object[] { new Integer(this.cleanupFrequency), "htodCleanupFrequency", this.cacheName,
                                                           new Integer(CacheConfig.MIN_CLEANUP_FREQUENCY), new Integer(CacheConfig.MAX_CLEANUP_FREQUENCY),
                                                           new Integer(CacheConfig.MIN_CLEANUP_FREQUENCY) });
                this.cleanupFrequency = CacheConfig.MIN_CLEANUP_FREQUENCY;
            }
            if (this.cleanupFrequency > CacheConfig.MAX_CLEANUP_FREQUENCY) {
                Tr.warning(tc, "DYNA0069W", new Object[] { new Integer(this.cleanupFrequency), "htodCleanupFrequency", this.cacheName,
                                                           new Integer(CacheConfig.MIN_CLEANUP_FREQUENCY), new Integer(CacheConfig.MAX_CLEANUP_FREQUENCY),
                                                           new Integer(CacheConfig.MAX_CLEANUP_FREQUENCY) });
                this.cleanupFrequency = CacheConfig.MAX_CLEANUP_FREQUENCY;
            }
        }

        if (this.evictionPolicy < CacheConfig.MIN_DISKCACHE_EVICTION_POLICY || this.evictionPolicy > CacheConfig.MAX_DISKCACHE_EVICTION_POLICY) {
            Tr.warning(tc, "DYNA0069W", new Object[] { new Integer(this.evictionPolicy), "diskCacheEvictionPolicy", this.cacheName,
                                                       new Integer(CacheConfig.MIN_DISKCACHE_EVICTION_POLICY), new Integer(CacheConfig.MAX_DISKCACHE_EVICTION_POLICY),
                                                       new Integer(CacheConfig.DEFAULT_DISKCACHE_EVICTION_POLICY) });
            this.evictionPolicy = CacheConfig.DEFAULT_DISKCACHE_EVICTION_POLICY;
        }

        if (diskCacheSizeLimit < 0 || (diskCacheSizeLimit > 0 && diskCacheSizeLimit < CacheConfig.MIN_DISKCACHE_SIZE)) {
            Tr.warning(tc, "DYNA0069W", new Object[] { new Integer(diskCacheSizeLimit), "diskCacheSize", this.cacheName,
                                                       new Integer(CacheConfig.MIN_DISKCACHE_SIZE), new Integer(CacheConfig.MAX_DISKCACHE_SIZE),
                                                       new Integer(CacheConfig.MIN_DISKCACHE_SIZE) });
            diskCacheSizeLimit = CacheConfig.MIN_DISKCACHE_SIZE;
        }
        if (diskCacheSizeInGBLimit < 0 || (diskCacheSizeInGBLimit > 0 && diskCacheSizeInGBLimit < CacheConfig.MIN_DISKCACHE_SIZE_GB)) {
            Tr.warning(tc, "DYNA0069W", new Object[] { new Integer(diskCacheSizeInGBLimit), "diskCacheSizeInGB", this.cacheName,
                                                       new Integer(CacheConfig.MIN_DISKCACHE_SIZE_GB), new Integer(CacheConfig.MAX_DISKCACHE_SIZE_GB),
                                                       new Integer(CacheConfig.MIN_DISKCACHE_SIZE_GB) });
            diskCacheSizeInGBLimit = CacheConfig.MIN_DISKCACHE_SIZE_GB;
        }
        if (diskCacheEntrySizeInMBLimit < 0) {
            Tr.warning(tc, "DYNA0069W", new Object[] { new Integer(diskCacheEntrySizeInMBLimit), "diskCacheEntrySizeInMB", this.cacheName,
                                                       new Integer(CacheConfig.MIN_DISKCACHE_ENTRY_SIZE_MB), new Integer(CacheConfig.MAX_DISKCACHE_ENTRY_SIZE_MB),
                                                       new Integer(CacheConfig.MIN_DISKCACHE_ENTRY_SIZE_MB) });
            diskCacheEntrySizeInMBLimit = CacheConfig.MIN_DISKCACHE_ENTRY_SIZE_MB;
        }

        if (this.evictionPolicy != CacheConfig.EVICTION_NONE) {
            if (highThreshold < CacheConfig.MIN_HIGH_THRESHOLD || highThreshold > CacheConfig.MAX_HIGH_THRESHOLD
                || lowThreshold < CacheConfig.MIN_LOW_THRESHOLD || lowThreshold > CacheConfig.MAX_LOW_THRESHOLD || highThreshold <= lowThreshold) {
                Tr.info(tc, "DYNA0068W", new Object[] { this.cacheName });
                highThreshold = CacheConfig.DEFAULT_HIGH_THRESHOLD;
                lowThreshold = CacheConfig.DEFAULT_LOW_THRESHOLD;
            }
        }

        this.diskCacheName = this.cacheName;
        this.diskCacheName = diskCacheName.replace('/', '_');
        this.diskCacheName = diskCacheName.replace('\\', '_');

        boolean found = findSwapDirPath(location);
        if (found == false) {
            return;
        }

        boolean flushToDiskOnStop = cacheConfig.flushToDiskOnStop;
        if (this.cacheName.equals(DCacheBase.DEFAULT_CACHE_NAME)) {
            String systemProperty = System.getProperty("com.ibm.ws.cache.flushToDiskOnStop");
            if (systemProperty != null && systemProperty.equalsIgnoreCase("true")) {
                flushToDiskOnStop = true;
            }
        }
        if (flushToDiskOnStop)
            Tr.info(tc, "DYNA0060I", new Object[] { this.cacheName });
        else
            Tr.info(tc, "DYNA0061I", new Object[] { this.cacheName });

        int lastIndex = swapDirPath.lastIndexOf(sep);
        swapRootDirPath = swapDirPath.substring(0, lastIndex);
        lastScanFileName = swapDirPath + sep + HTOD_LAST_SCAN_FILENAME;
        htodPropertyFileName = swapDirPath + sep + HTOD_PROPERTIES_FILENAME;
        invalidationFileName = swapDirPath + sep + HTOD_INVALIDATION_FILENAME;
        inProgressFileName = swapDirPath + sep + HTOD_IN_PROGRESS_FILENAME;

        File df = new File(this.inProgressFileName);
        if (df.exists()) {
            // Disk cache files have been removed because of unexpected JVM termination.
            Tr.warning(tc, "DYNA0056W");
            deleteDiskCacheFiles();
        }
        createInProgressFile();

        loadAndCheckPropertyFile();
        if (this.propertyFileStatus != 0) {
            if ((this.propertyFileStatus & PROPERTY_ERROR_FILE_CORRUPT) > 0 || (this.propertyFileStatus & PROPERTY_ERROR_CACHE_SIZE) > 0
                || (this.propertyFileStatus & PROPERTY_ERROR_FIELD_CHECK) > 0 || (this.propertyFileStatus & PROPERTY_ERROR_GB) > 0) {
                if (this.currentCacheSizeInBytes > 0) {
                    this.dataGB = this.dataFiles;
                    this.dependencyIdGB = this.dependencyIdFiles;
                    this.templateGB = this.templateFiles;
                }
                deletePropertyFile();
            }
        }

        try {
            htod = new HTODDynacache(swapDirPath, swapDirPath + sep + HTOD_PREF, cache, this);
        } catch (Throwable t) {
            com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.cache.CacheOnDisk.CacheOnDisk", "338", this);
            traceDebug(methodName, "cacheName=" + this.cacheName + "\nException: " + ExceptionUtility.getStackTrace(t));
            Tr.error(tc, "DYNA0055E", new Object[] { this.cacheName, definedLocation, alternateLocation });
            c.setSwapToDisk(false);
            return;
        }

        if (this.disableDependencyId) {
            if (this.dependencyIdFiles > 0) {
                Tr.warning(tc, "DYNA1033W",
                           new Object[] { String.valueOf(this.disableDependencyId), this.cacheName, String.valueOf(!this.disableDependencyId) });
                this.disableDependencyId = false;
                if (this.dependencyIdGB == 0) {
                    this.dependencyIdGB = this.dependencyIdFiles;
                }
            } else {
                this.dependencyIdGB = 0;
            }
        } else {
            if (this.dependencyIdFiles == 0) {
                Tr.warning(tc, "DYNA1033W",
                           new Object[] { String.valueOf(this.disableDependencyId), this.cacheName, String.valueOf(!this.disableDependencyId) });
                this.disableDependencyId = true;
                if (this.dependencyIdGB > 0) {
                    this.dependencyIdGB = 0;
                }
            } else {
                if (this.dependencyIdGB == 0) {
                    this.dependencyIdGB = this.dependencyIdFiles;
                }
            }
        }
        if (this.disableTemplatesSupport) {
            if (this.templateFiles > 0) {
                Tr.warning(tc, "DYNA1034W",
                           new Object[] { String.valueOf(this.disableTemplatesSupport), this.cacheName, String.valueOf(!this.disableTemplatesSupport) });
                this.disableTemplatesSupport = false;
                if (this.templateGB == 0) {
                    this.templateGB = this.templateFiles;
                }
            } else {
                this.templateGB = 0;
            }
        } else {
            if (this.templateFiles == 0) {
                Tr.warning(tc, "DYNA1034W",
                           new Object[] { String.valueOf(this.disableTemplatesSupport), this.cacheName, String.valueOf(!this.disableTemplatesSupport) });
                this.disableTemplatesSupport = true;
                if (this.templateGB > 0) {
                    this.templateGB = 0;
                }
            } else {
                if (this.templateGB == 0) {
                    this.templateGB = this.templateFiles;
                }
            }
        }
        int minGB = this.dataGB + this.dependencyIdGB + this.templateGB;
        if (diskCacheSizeInGBLimit > 0 && minGB > diskCacheSizeInGBLimit) {
            diskCacheSizeInGBLimit = minGB;
            Tr.warning(tc, "DYNA0071W", new Object[] { this.cacheName, new Integer(diskCacheSizeInGBLimit), swapDirPath });
        }

        c.setEnableDiskCacheSizeInBytesChecking(false);
        if (this.currentCacheSizeInBytes > 0) {
            if (diskCacheSizeInGBLimit > 0) {
                c.setEnableDiskCacheSizeInBytesChecking(true);
            }
            this.enableCacheSizeInBytes = true;
        } else {
            if (diskCacheSizeInGBLimit > 0) {
                Tr.error(tc, "DYNA0066W", new Object[] { this.cacheName, swapDirPath });
                diskCacheSizeInGBLimit = 0;
            }
        }

        this.diskCacheSizeInfo.initialize(diskCacheSizeLimit, diskCacheEntrySizeInMBLimit, diskCacheSizeInGBLimit, this.dataGB, this.dependencyIdGB,
                                          this.templateGB, highThreshold, lowThreshold);
        if (this.propertyFileStatus != 0) {
            updatePropertyFile();
        }

        if (cleanupFrequency != 0) {
            sleepTime = cleanupFrequency * 60 * 1000;
        }

        readLastScanFile();
        setAlarm = true;
        if (this.diskCachePerformanceLevel == CacheConfig.HIGH) {
            setAlarm = false;
            this.cleanupFrequency = 0;
            this.startState = START_LPBT_SCAN;
        }
        long now = System.currentTimeMillis();
        // if have not done scan in a while (most
        // likely because of server being stopped during
        // the cleanup hour), do the scan on startup
        if (this.lastScanTime > 0 && this.cleanupFrequency > 0) {
            if ((this.lastScanTime + this.cleanupFrequency) <= now) {
                setAlarm = false; // don't set alarm again because it is already set in doCleanup()
                this.startState = START_LPBT_SCAN;
            }
        } else {
            updateLastScanFile();
        }

        // If the disk files are in old format or, both disk cache size and disk cache size in GB are disabled, then
        // disable DCGC
        if (this.evictionPolicy != CacheConfig.EVICTION_NONE) { // 325796
            if (this.enableCacheSizeInBytes == false) { // check for new format
                this.evictionPolicy = CacheConfig.EVICTION_NONE;
                this.populateEvictionTable = false;
                Tr.error(tc, "DYNA0067W", new Object[] { this.cacheName, swapDirPath });
            } else if (diskCacheSizeLimit == 0 && diskCacheSizeInGBLimit == 0) {
                this.evictionPolicy = CacheConfig.EVICTION_NONE;
                this.populateEvictionTable = false;
                Tr.error(tc, "DYNA0070W", new Object[] { this.cacheName });
            } else {
                this.htod.initializeEvictionTable();
                this.populateEvictionTable = true; // 3821 NK

                // Create the garbage collector thread and get the thread from the pool
                this.garbageCollectionThread = new GarbageCollectorThread(this);
            }
        }
        // Create the disk cleanup thread and get the thread from the pool
        this.diskCleanupThread = new DiskCleanupThread(this);

        // restore cache config in case of config has changed.
        cacheConfig.htodCleanupHour = this.cleanupHour;
        cacheConfig.htodInvalInterval = this.invalInterval;
        cacheConfig.htodCleanupFrequency = this.cleanupFrequency;
        cacheConfig.htodDelayOffload = this.delayOffload;
        cacheConfig.htodDelayOffloadEntriesLimit = this.delayOffloadEntriesLimit;
        cacheConfig.htodDelayOffloadDepIdBuckets = this.delayOffloadDepIdBuckets;
        cacheConfig.htodDelayOffloadTemplateBuckets = this.delayOffloadTemplateBuckets;
        cacheConfig.disableDependencyId = this.disableDependencyId;
        cacheConfig.disableTemplatesSupport = this.disableTemplatesSupport;
        cacheConfig.diskCachePerformanceLevel = this.diskCachePerformanceLevel;
        cacheConfig.diskCacheEvictionPolicy = this.evictionPolicy;
        cacheConfig.explicitBufferLimitOnStop = this.explicitBufferLimitOnStop;

        cacheConfig.diskCacheSize = diskCacheSizeLimit;
        cacheConfig.diskCacheSizeInGB = diskCacheSizeInGBLimit;
        cacheConfig.diskCacheEntrySizeInMB = diskCacheEntrySizeInMBLimit;
        cacheConfig.diskCacheHighThreshold = highThreshold;
        cacheConfig.diskCacheLowThreshold = lowThreshold;

        StringBuffer configMsg = new StringBuffer();
        if (diskCacheSizeLimit > 0) {
            configMsg.append("  DiskCacheSize=");
            configMsg.append(diskCacheSizeLimit);
        }
        if (diskCacheSizeInGBLimit > 0) {
            configMsg.append("  DiskCacheSizeInGB=");
            configMsg.append(diskCacheSizeInGBLimit);
        }
        if (diskCacheEntrySizeInMBLimit > 0) {
            configMsg.append("  DiskCacheEntryInMB=");
            configMsg.append(diskCacheEntrySizeInMBLimit);
        }
        configMsg.append("  DiskCachePerformanceLevel=");
        configMsg.append(this.diskCachePerformanceLevel);
        configMsg.append("  DelayOffload=");
        configMsg.append(this.delayOffload);
        if (this.diskCachePerformanceLevel == CacheConfig.LOW) {
            if (this.cleanupFrequency == 0) {
                configMsg.append("  CleanupHour=");
                configMsg.append(this.cleanupHour);
            } else {
                configMsg.append("  CleanupFrequency=");
                configMsg.append(this.cleanupFrequency);
            }
        } else if (this.diskCachePerformanceLevel == CacheConfig.CUSTOM || this.diskCachePerformanceLevel == CacheConfig.BALANCED) {
            configMsg.append("  DelayOffloadEntriesLimit=");
            configMsg.append(this.delayOffloadEntriesLimit);
            configMsg.append("  DelayOffloadDepIdBuckets=");
            configMsg.append(this.delayOffloadDepIdBuckets);
            configMsg.append("  DelayOffloadTemplateBuckets=");
            configMsg.append(this.delayOffloadTemplateBuckets);
            if (this.cleanupFrequency == 0) {
                configMsg.append("  CleanupHour=");
                configMsg.append(this.cleanupHour);
            } else {
                configMsg.append("  CleanupFrequency=");
                configMsg.append(this.cleanupFrequency);
            }
        }
        configMsg.append("  DiskCacheEvictionPolicy=");
        configMsg.append(this.evictionPolicy);
        if (this.evictionPolicy != CacheConfig.EVICTION_NONE) {
            configMsg.append("  DiskCacheHighThreshold=");
            configMsg.append(highThreshold);
            configMsg.append("  DiskCacheLowThreshold=");
            configMsg.append(lowThreshold);
        }
        configMsg.append("  DataHashtableSize=");
        configMsg.append(this.dataHashtableSize);
        configMsg.append("  DepIdHashtableSize=");
        configMsg.append(this.depIdHashtableSize);
        configMsg.append("  TemplateHashtableSize=");
        configMsg.append(this.templateHashtableSize);
        Tr.info(tc, "DYNA0059I", new Object[] { this.cacheName, configMsg.toString() });

        if (setAlarm) {
            if (cleanupFrequency == 0) {
                sleepTime = calculateSleepTime();
                // if the server is started at midnight, it sets alarm for 10 sec.
                if (sleepTime > SECONDS_FOR_23_HOURS * 1000) {
                    setAlarm = false;
                    this.startState = START_LPBT_SCAN;
                } else {
                    traceDebug(methodName, "cacheName=" + this.cacheName + " sleepTime=" + sleepTime);
                }
            }
            if (setAlarm) {
                if (!(this.startState == START_LPBT_SCAN || this.startState == START_LPBT_REMOVE || this.populateEvictionTable)) { // 316215
                    Scheduler.createNonDeferrable(sleepTime, null, new Runnable() {
                        @Override
                        public void run() {
                            alarm(null);
                        }
                    });

                }
            }
        }
        if (this.diskCachePerformanceLevel != CacheConfig.LOW) {
            this.populateDependencyTable = true;
        }
    }

    /**
     * Call this method to close the disk file manager and operation.
     */
    @Override
    public void close(boolean deleteProgressFile) {
        final String methodName = "close()";
        traceDebug(methodName, "cacheName=" + this.cacheName + " deleteProgressFile=" + deleteProgressFile);
        if (deleteProgressFile) { // 316654
            // remove the InProgress (dummy) file when close
            deleteInProgressFile();
        }
        try {
            htod.close();
        } catch (Throwable t) {
            com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.cache.CacheOnDisk.close", "574", this);
            traceDebug(methodName, "cacheName=" + this.cacheName + "\nException: " + ExceptionUtility.getStackTrace(t));
        }
    }

    /**
     * Call this method to offload auxiliary dependency tables to the disk and update property file.
     */
    @Override
    public int writeAuxiliaryDepTables() {
        int returnCode = htod.writeAuxiliaryDepTables();
        if (returnCode == HTODDynacache.DISK_EXCEPTION) {
            stopOnError(this.htod.diskCacheException);
        } else {
            updatePropertyFile();
        }
        return returnCode;
    }

    /**
     * Call this method to read the timestamp of the last scan in Last Scan file.
     */
    private void readLastScanFile() {
        final String methodName = "readLastScanFile()";
        final File f = new File(lastScanFileName);
        traceDebug(methodName, "cacheName=" + this.cacheName);
        if (f.exists()) {
            final CacheOnDisk cod = this;
            AccessController.doPrivileged(new PrivilegedAction() {
                @Override
                public Object run() {
                    FileInputStream fis = null;
                    ObjectInputStream ois = null;
                    try {
                        fis = new FileInputStream(f);
                        ois = new ObjectInputStream(fis);
                        cod.lastScanTime = ois.readLong();
                    } catch (Throwable t) {
                        com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.cache.CacheOnDisk.readLastScanFile", "611", cod);
                        traceDebug(methodName, "cacheName=" + cod.cacheName + "\nException: " + ExceptionUtility.getStackTrace(t));
                    } finally {
                        try {
                            if (ois != null) {
                                ois.close();
                            }
                            if (fis != null) {
                                fis.close();
                            }
                        } catch (Throwable t) {
                            com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.cache.CacheOnDisk.readLastScanFile", "622", cod);
                            traceDebug(methodName, "cacheName=" + cod.cacheName + "\nException: " + ExceptionUtility.getStackTrace(t));
                        }
                    }
                    return null;
                }
            });
        }
    }

    /**
     * Call this method to update the timestamp of the last scan in Last Scan file.
     */
    protected void updateLastScanFile() {
        final String methodName = "updateLastScanFile()";
        final File f = new File(lastScanFileName);
        final CacheOnDisk cod = this;
        traceDebug(methodName, "cacheName=" + this.cacheName);
        AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                FileOutputStream fos = null;
                ObjectOutputStream oos = null;
                try {
                    fos = new FileOutputStream(f);
                    oos = new ObjectOutputStream(fos);
                    oos.writeLong(System.currentTimeMillis());
                } catch (Throwable t) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.cache.CacheOnDisk.updateLastScanFile", "650", cod);
                    traceDebug(methodName, "cacheName=" + cod.cacheName + "\nException: " + ExceptionUtility.getStackTrace(t));
                } finally {
                    try {
                        if (oos != null) {
                            oos.close();
                        }
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (Throwable t) {
                        com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.cache.CacheOnDisk.updateLastScanFile", "661", cod);
                        traceDebug(methodName, "cacheName=" + cod.cacheName + "\nException: " + ExceptionUtility.getStackTrace(t));
                    }
                }
                return null;
            }
        });
    }

    /**
     * Call this method to load and check the HTOD property file per instance.
     */
    private void loadAndCheckPropertyFile() {
        final String methodName = "loadAndCheckPropertyFile()";
        final File f = new File(htodPropertyFileName);
        final File swapDirPathFile = new File(swapDirPath);
        final CacheOnDisk cod = this;
        traceDebug(methodName, "cacheName=" + this.cacheName);
        AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                cod.propertyFileStatus = PROPERTY_FILE_OK;
                String[] files = swapDirPathFile.list();
                if (files != null && files.length > 0) {
                    for (int i = 0; i < files.length; i++) {
                        if (files[i].startsWith(HTOD_PREF + HTODDynacache.object_suffix)) {
                            cod.dataFiles++;
                        } else if (files[i].startsWith(HTOD_PREF + HTODDynacache.dependency_suffix)) {
                            cod.dependencyIdFiles++;
                        } else if (files[i].startsWith(HTOD_PREF + HTODDynacache.template_suffix)) {
                            cod.templateFiles++;
                        }
                    }
                } else {
                    traceDebug(methodName, "cacheName=" + cod.cacheName + " no disk cache files exist");
                    cod.propertyFileStatus = (byte) (cod.propertyFileStatus | PROPERTY_ERROR_NO_FILES);
                    return null;
                }
                if (f.exists()) {
                    FileInputStream fis = null;
                    try {
                        fis = new FileInputStream(f);
                        Properties htodProp = new Properties();
                        htodProp.load(fis);

                        String sTemp = (String) htodProp.get(DISABLE_DEPENDENCY_ID);
                        if (sTemp != null) {
                            cod.disableDependencyIdFromPropertyFile = (Boolean.valueOf(sTemp)).booleanValue();
                        } else {
                            cod.propertyFileStatus = (byte) (cod.propertyFileStatus | PROPERTY_ERROR_DISABLE_DEPID);
                        }
                        sTemp = (String) htodProp.get(DISABLE_TEMPLATE_SUPPORT);
                        if (sTemp != null) {
                            cod.disableTemplatesSupprotFromPropertyFile = (Boolean.valueOf(sTemp)).booleanValue();
                        } else {
                            cod.propertyFileStatus = (byte) (cod.propertyFileStatus | PROPERTY_ERROR_DISABLE_TEMPLATE);
                        }
                        sTemp = (String) htodProp.get(CACHE_SIZE_IN_BYTES);
                        long currentCacheSizeInBytes = 0;
                        if (sTemp != null) {
                            currentCacheSizeInBytes = Long.parseLong(sTemp);
                        } else {
                            cod.propertyFileStatus = (byte) (cod.propertyFileStatus | PROPERTY_ERROR_CACHE_SIZE);
                            return null;
                        }
                        byte[] b = sTemp.getBytes(StandardCharsets.UTF_8);
                        int fieldCheck = 0;
                        for (int i = 0; i < b.length; i++) {
                            fieldCheck += b[i];
                        }
                        fieldCheck = fieldCheck * 3;
                        sTemp = (String) htodProp.get(FIELD_CHECK);
                        if (sTemp != null) {
                            int expFieldCheck = Integer.parseInt(sTemp);
                            if (fieldCheck != expFieldCheck) {
                                cod.propertyFileStatus = (byte) (cod.propertyFileStatus | PROPERTY_ERROR_FIELD_CHECK);
                                return null;
                            }
                        } else {
                            cod.propertyFileStatus = (byte) (cod.propertyFileStatus | PROPERTY_ERROR_FIELD_CHECK);
                            return null;
                        }

                        sTemp = (String) htodProp.get(DATA_GB);
                        int dataGB = 0;
                        if (sTemp != null) {
                            dataGB = Integer.parseInt(sTemp);
                        } else {
                            cod.propertyFileStatus = (byte) (cod.propertyFileStatus | PROPERTY_ERROR_GB);
                            return null;
                        }
                        sTemp = (String) htodProp.get(DEPID_GB);
                        int dependencyIdGB = 0;
                        if (sTemp != null) {
                            dependencyIdGB = Integer.parseInt(sTemp);
                        } else {
                            cod.propertyFileStatus = (byte) (cod.propertyFileStatus | PROPERTY_ERROR_GB);
                            return null;
                        }
                        sTemp = (String) htodProp.get(TEMPLATE_GB);
                        int templateGB = 0;
                        if (sTemp != null) {
                            templateGB = Integer.parseInt(sTemp);
                        } else {
                            cod.propertyFileStatus = (byte) (cod.propertyFileStatus | PROPERTY_ERROR_GB);
                            return null;
                        }
                        cod.currentCacheSizeInBytes = currentCacheSizeInBytes;
                        cod.dataGB = dataGB;
                        cod.dependencyIdGB = dependencyIdGB;
                        cod.templateGB = templateGB;

                        traceDebug(methodName, "cacheName=" + cod.cacheName + " htod.properties exist with status=" + cod.propertyFileStatus
                                               + " disableDependencyIdFromProperty=" + cod.disableDependencyIdFromPropertyFile
                                               + " disableTemplatesSupportFromProperty=" + cod.disableTemplatesSupprotFromPropertyFile + " cacheSizeInBytes="
                                               + cod.currentCacheSizeInBytes + " dataGB=" + cod.dataGB + " dependencyIdGB=" + cod.dependencyIdGB + " templateGB="
                                               + cod.templateGB);
                    } catch (Throwable t) {
                        com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.cache.CacheOnDisk.loadAndCheckPropertyFile", "782", cod);
                        traceDebug(methodName, "cacheName=" + cod.cacheName + "\nException: " + ExceptionUtility.getStackTrace(t));
                        cod.propertyFileStatus = (byte) (cod.propertyFileStatus | PROPERTY_ERROR_FILE_CORRUPT);
                    } finally {
                        try {
                            if (fis != null) {
                                fis.close();
                            }
                        } catch (Throwable t) {
                            com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.cache.CacheOnDisk.loadAndCheckPropertyFile", "791", cod);
                            traceDebug(methodName, "cacheName=" + cod.cacheName + "\nException: " + ExceptionUtility.getStackTrace(t));
                        }
                    }

                } else {
                    cod.propertyFileStatus = (byte) (cod.propertyFileStatus | PROPERTY_ERROR_FILE_NOT_EXIST);
                    traceDebug(methodName, "cacheName=" + cod.cacheName + " htod.properties not exist" + " dependencyIdFiles="
                                           + cod.dependencyIdFiles + " templateFiles=" + cod.templateFiles);
                }
                return null;
            }
        });
    }

    /**
     * Call this method to update the HTOD property file per instance.
     */
    protected void updatePropertyFile() {
        final String methodName = "updatePropertyFile()";
        final File f = new File(htodPropertyFileName);
        final CacheOnDisk cod = this;
        traceDebug(methodName, "cacheName=" + this.cacheName);
        AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                FileOutputStream fos = null;
                Properties htodProp = new Properties();

                try {
                    fos = new FileOutputStream(f);
                    htodProp.put(HTOD_VERSION, HTOD_VERSION_NUM);
                    htodProp.put(DISABLE_DEPENDENCY_ID, Boolean.toString(disableDependencyId));
                    htodProp.put(DISABLE_TEMPLATE_SUPPORT, Boolean.toString(disableTemplatesSupport));
                    long cacheSizeInBytes = 0;
                    int fieldCheck = 0;
                    if (cod.enableCacheSizeInBytes) {
                        if (cod.currentCacheSizeInBytes < cod.htod.minDiskCacheSizeInBytes) {
                            cod.currentCacheSizeInBytes = cod.htod.minDiskCacheSizeInBytes;
                        }
                        cacheSizeInBytes = cod.currentCacheSizeInBytes;
                        htodProp.put(CACHE_SIZE_IN_BYTES, Long.toString(cacheSizeInBytes));
                        String s = String.valueOf(cacheSizeInBytes);
                        byte[] b = s.getBytes(StandardCharsets.UTF_8);
                        fieldCheck = 0;
                        for (int i = 0; i < b.length; i++) {
                            fieldCheck += b[i];
                        }
                        fieldCheck = fieldCheck * 3;
                        htodProp.put(FIELD_CHECK, String.valueOf(fieldCheck));
                        htodProp.put(DATA_GB, String.valueOf(cod.diskCacheSizeInfo.currentDataGB));
                        htodProp.put(DEPID_GB, String.valueOf(cod.diskCacheSizeInfo.currentDependencyIdGB));
                        htodProp.put(TEMPLATE_GB, String.valueOf(cod.diskCacheSizeInfo.currentTemplateGB));
                        traceDebug(methodName, "cacheName=" + cod.cacheName + " disableDependencyId=" + disableDependencyId
                                               + " disableTemplatesSupport=" + disableTemplatesSupport + " cacheSizeInBytes=" + cacheSizeInBytes + " fieldCheck="
                                               + fieldCheck + " dataGB=" + cod.diskCacheSizeInfo.currentDataGB + " dependencyIdGB="
                                               + cod.diskCacheSizeInfo.currentDependencyIdGB + " templateGB=" + cod.diskCacheSizeInfo.currentTemplateGB);
                    } else {
                        traceDebug(methodName, "cacheName=" + cod.cacheName + " disableDependencyId=" + disableDependencyId
                                               + " disableTemplatesSupport=" + disableTemplatesSupport);
                    }
                    htodProp.store(fos, "HTOD properties - Do not modify the properties");
                } catch (Throwable t) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.cache.CacheOnDisk.updatePropertyFile", "651", cod);
                    traceDebug(methodName, "cacheName=" + cod.cacheName + "\nException: " + ExceptionUtility.getStackTrace(t));
                } finally {
                    try {
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (Throwable t) {
                        com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.cache.CacheOnDisk.updatePropertyFile", "859", cod);
                        traceDebug(methodName, "cacheName=" + cod.cacheName + "\nException: " + ExceptionUtility.getStackTrace(t));
                    }
                }
                return null;
            }
        });

    }

    /**
     * Call this method to delete the HTOD property file.
     */
    private void deletePropertyFile() {
        final String methodName = "deletePropertyFile()";
        final File f = new File(htodPropertyFileName);
        final CacheOnDisk cod = this;
        traceDebug(methodName, "cacheName=" + this.cacheName);
        AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                try {
                    f.delete();
                } catch (Throwable t) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.cache.CacheOnDisk.deletePropertyFile", "883", cod);
                    traceDebug(methodName, "cacheName=" + cod.cacheName + "\nException: " + ExceptionUtility.getStackTrace(t));
                }
                return null;
            }
        });
    }

    /**
     * Call this method to delete all disk cache files per cache instance.
     */
    @Override
    public void deleteDiskCacheFiles() {
        final String methodName = "deleteDiskCacheFiles()";
        final File f = new File(swapDirPath);
        final CacheOnDisk cod = this;
        traceDebug(methodName, "cacheName=" + this.cacheName);
        AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                // delete files
                File fl[] = f.listFiles();
                for (int i = 0; i < fl.length; i++) {
                    try {
                        fl[i].delete();
                    } catch (Throwable t) {
                        com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.cache.CacheOnDisk.deleteDiskCacheFiles", "908", cod);
                        traceDebug(methodName, "cacheName=" + cod.cacheName + "\nException: " + ExceptionUtility.getStackTrace(t));
                    }
                }
                return null;
            }
        });
    }

    /**
     * Call this method to delete all disk cache files per JVM because the JVM is aborted abnormally.
     */
    private void deleteAllDiskCacheFiles() {
        final String methodName = "deleteAllDiskCacheFiles()";
        final File f = new File(swapRootDirPath);
        final CacheOnDisk cod = this;
        traceDebug(methodName, "cacheName=" + this.cacheName);
        AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                // delete files
                File fd[] = f.listFiles();
                for (int i = 0; i < fd.length; i++) {
                    if (fd[i].isDirectory()) {
                        File fl[] = fd[i].listFiles();
                        for (int j = 0; j < fl.length; j++) {
                            try {
                                fl[j].delete();
                            } catch (Throwable t) {
                                com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.cache.CacheOnDisk.deleteAllDiskCacheFiles", "938", cod);
                                traceDebug(methodName, "cacheName=" + cod.cacheName + "\nException: " + ExceptionUtility.getStackTrace(t));
                            }
                        }
                    }
                }
                return null;
            }
        });
    }

    /**
     * Call this method to create "InProgress" file. It is being used to indicate whether the JVM is aborted abnormally.
     */
    private void createInProgressFile() {
        final String methodName = "createInProgressFile()";
        final File f = new File(this.inProgressFileName);
        final CacheOnDisk cod = this;
        traceDebug(methodName, "cacheName=" + this.cacheName + " file=" + this.inProgressFileName);
        AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                try {
                    f.createNewFile();
                } catch (Throwable t) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.cache.CacheOnDisk.createInProgressFile", "964", cod);
                    traceDebug(methodName, "cacheName=" + cod.cacheName + "\nException: " + ExceptionUtility.getStackTrace(t));
                }
                return null;
            }
        });
    }

    /**
     * Call this method to delete "InProgress" file. It is being used to indicate whether the JVM is aborted abnormally.
     */
    private void deleteInProgressFile() {
        final String methodName = "deleteInProgressFile()";
        final File f = new File(this.inProgressFileName);
        final CacheOnDisk cod = this;
        traceDebug(methodName, "cacheName=" + this.cacheName);
        AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                try {
                    f.delete();
                } catch (Throwable t) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.cache.CacheOnDisk.deleteInProgressFile", "987", cod);
                    traceDebug(methodName, "cacheName=" + cod.cacheName + "\nException: " + ExceptionUtility.getStackTrace(t));
                }
                return null;
            }
        });
    }

    /**
     * Call this method to stop the LPBT.
     *
     * @param completeClear
     *            - boolean to select clear all invalidation buffers completely or not.
     */
    @Override
    public void stop(boolean completeClear) {
        stopping = true;
        this.htod.invalidationBuffer.setStopping(true);
        htod.stop(completeClear);
        if (this.garbageCollectionThread != null) { // 313370
            synchronized (this.garbageCollectionThread.gcMonitor) {
                if (garbageCollectionThread.currentThread != null) {
                    garbageCollectionThread.currentThread.cancel(false);
                }
            }
        }
        if (this.diskCleanupThread != null) {
            synchronized (this.diskCleanupThread.dcMonitor) {
                this.diskCleanupThread.stopped = true;
                this.diskCleanupThread.processDiskCleanup = true;
                if (diskCleanupThread.currentThread != null) {
                    diskCleanupThread.currentThread.cancel(false);
                }
            }
        }
    }

    /**
     * Call this method to stop the LPBT.
     *
     * @param completeClear
     *            - boolean to select clear all invalidation buffers completely or not.
     */
    @Override
    public void stopOnError(Exception ex) {
        this.cache.setSwapToDisk(false);
        stop(HTODDynacache.COMPLETE_CLEAR);
        close(!DELETE_IN_PROGRESS_FILE);
        Tr.error(tc, "DYNA0072E", new Object[] { this.cacheName, swapDirPath, ex.getMessage() });
    }

    /**
     * Call this method to read in all invalidation cache ids if the invalidation file exists. and then delete the
     * invalidation file.
     */
    protected ValueSet readAndDeleteInvalidationFile() {
        final String methodName = "readAndDeleteInvalidationFile()";
        final File f = new File(invalidationFileName);
        final CacheOnDisk cod = this;
        this.valueSet = new ValueSet(1);
        if (f.exists()) {
            AccessController.doPrivileged(new PrivilegedAction() {
                @Override
                public Object run() {
                    FileInputStream fis = null;
                    ObjectInputStream ois = null;
                    try {
                        fis = new FileInputStream(f);
                        ois = new ObjectInputStream(fis);
                        int size = ois.readInt();
                        cod.valueSet = new ValueSet(size);
                        for (int i = 0; i < size; i++) {
                            cod.valueSet.add(ois.readObject());
                        }
                    } catch (Throwable t1) {
                        com.ibm.ws.ffdc.FFDCFilter.processException(t1, "com.ibm.ws.cache.CacheOnDisk.readAndDeleteInvalidationFile", "1056", cod);
                        traceDebug(methodName, "cacheName=" + cod.cacheName + "\nException: " + ExceptionUtility.getStackTrace(t1));
                    } finally {
                        try {
                            if (ois != null) {
                                ois.close();
                            }
                            if (fis != null) {
                                fis.close();
                            }
                            f.delete();
                        } catch (Throwable t2) {
                            com.ibm.ws.ffdc.FFDCFilter.processException(t2, "com.ibm.ws.cache.CacheOnDisk.readAndDeleteInvalidationFile", "1068", cod);
                            traceDebug(methodName, "cacheName=" + cod.cacheName + "\nException: " + ExceptionUtility.getStackTrace(t2));
                        }
                    }
                    return null;
                }
            });
        }
        traceDebug(methodName, "cacheName=" + this.cacheName + " " + invalidationFileName + " valueSet=" + valueSet.size());
        return this.valueSet;
    }

    /**
     * Call this method to create invalidation file to offload the invalidation cache ids. When the server is restarted,
     * the invalidation cache ids are read back. The LPBT will be called to remove these cache ids from the disk.
     */
    protected void createInvalidationFile() {
        final String methodName = "createInvalidationFile()";
        final File f = new File(invalidationFileName);
        final CacheOnDisk cod = this;
        traceDebug(methodName, "cacheName=" + this.cacheName + " valueSet=" + cod.valueSet.size());
        AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                FileOutputStream fos = null;
                ObjectOutputStream oos = null;
                try {
                    fos = new FileOutputStream(f);
                    oos = new ObjectOutputStream(fos);

                    oos.writeInt(cod.valueSet.size());
                    Iterator it = valueSet.iterator();
                    while (it.hasNext()) {
                        Object entryId = it.next();
                        oos.writeObject(entryId);
                    }
                } catch (Throwable t1) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(t1, "com.ibm.ws.cache.CacheOnDisk.createInvalidationFile", "1106", cod);
                    traceDebug(methodName, "cacheName=" + cod.cacheName + "\nException: " + ExceptionUtility.getStackTrace(t1));
                } finally {
                    try {
                        oos.close();
                        fos.close();
                    } catch (Throwable t2) {
                        com.ibm.ws.ffdc.FFDCFilter.processException(t2, "com.ibm.ws.cache.CacheOnDisk.createInvalidationFile", "1113", cod);
                        traceDebug(methodName, "cacheName=" + cod.cacheName + "\nException: " + ExceptionUtility.getStackTrace(t2));
                    }
                }

                return null;
            }
        });
    }

    /**
     * Call this method when the alarm is triggered. It is being checked to see whether a disk cleanup is scheduled to
     * run.
     */
    public void alarm(final Object alarmContext) {
        final String methodName = "alarm()";
        synchronized (this) {
            if (!stopping && !this.htod.invalidationBuffer.isDiskClearInProgress()) {
                this.htod.invalidationBuffer.invokeBackgroundInvalidation(HTODInvalidationBuffer.SCAN);
            } else if (stopping) {
                traceDebug(methodName, "cacheName=" + this.cacheName + " abort disk cleanup because of server is stopping.");
            } else {
                if (cleanupFrequency == 0) {
                    sleepTime = calculateSleepTime();
                }
                traceDebug(methodName, "cacheName=" + this.cacheName + " disk clear is in progress - skip disk scan and set alarm sleepTime="
                                       + sleepTime);
                Scheduler.createNonDeferrable(sleepTime, alarmContext, new Runnable() {
                    @Override
                    public void run() {
                        alarm(alarmContext);
                    }
                });
            }
        }
    }

    @Override
    @Trivial
    public void invokeDiskCleanup(boolean scan) {
        if (scan || htod.invalidationBuffer.size() > 0 || htod.invalidationBuffer.size(HTODInvalidationBuffer.GC_BUFFER) > 0) {
            if (this.diskCleanupThread != null) {
                synchronized (this.diskCleanupThread.dcMonitor) {
                    if (this.diskCleanupThread.currentThread == null) {
                        this.diskCleanupThread.scan = scan;

                        this.diskCleanupThread.processDiskCleanup = true;
                        this.diskCleanupThread.currentThread = spawnThread(this.diskCleanupThread);
                    }
                }
            }
        }
    }

    @Override
    public boolean invokeDiskCacheGarbageCollector(int GCType) {
        boolean notified = false;
        if (this.garbageCollectionThread != null) {
            synchronized (this.garbageCollectionThread.gcMonitor) {
                if (this.garbageCollectionThread.currentThread == null) {
                    this.garbageCollectionThread.GCType = GCType;
                    this.garbageCollectionThread.processGC = true;
                    garbageCollectionThread.currentThread = spawnThread(this.garbageCollectionThread);
                    notified = true;
                }
            }
        }
        return notified;
    }

    /**
     * Call this method to clear the disk cache per cache instance.
     */
    @Override
    public void clearDiskCache() {
        if (htod.clearDiskCache() == HTODDynacache.DISK_EXCEPTION) {
            stopOnError(this.htod.diskCacheException);
        } else {
            updateLastScanFile();
            updatePropertyFile();
            createInProgressFile();
        }
    }

    /**
     * Call this method to write a cache entry to the disk.
     */
    @Override
    public int writeCacheEntry(CacheEntry ce) { // @A5C
        int returnCode = htod.writeCacheEntry(ce);
        if (returnCode == HTODDynacache.DISK_EXCEPTION) {
            stopOnError(this.htod.diskCacheException);
        }
        return returnCode;
    }

    /**
     * Call this method to read a cache entry from the disk.
     *
     * @param id
     *            - cache id.
     */
    @Override
    public CacheEntry readCacheEntry(Object id) { // SKS-O
        Result result = htod.readCacheEntry(id);
        if (result.returnCode == HTODDynacache.DISK_EXCEPTION) {
            stopOnError(result.diskException);
            this.htod.returnToResultPool(result);
            return null;
        }
        CacheEntry cacheEntry = (CacheEntry) result.data;
        this.htod.returnToResultPool(result);
        return cacheEntry;
    }

    /**
     * Call this method to read a cache entry from the disk.
     *
     * @param id
     *            - cache id.
     * @param checkDependency
     *            - true to check whether cache id is also depid id.
     */
    @Override
    public CacheEntry readCacheEntry(Object id, boolean calledFromRemove) {
        Result result = htod.readCacheEntry(id, calledFromRemove);
        if (result.returnCode == HTODDynacache.DISK_EXCEPTION) {
            stopOnError(result.diskException);
            this.htod.returnToResultPool(result);
            return null;
        }
        CacheEntry cacheEntry = (CacheEntry) result.data;
        this.htod.returnToResultPool(result);
        return cacheEntry;
    }

    /**
     * Call this method to a cache entry from the disk.
     *
     * @param ce
     *            - cache entry.
     */
    @Override
    public void delCacheEntry(CacheEntry ce, int cause, int source, boolean fromDepIdTemplateInvalidation) {
        htod.delCacheEntry(ce, cause, source, fromDepIdTemplateInvalidation);
    }

    /**
     * Call this method to remove multiple of cache ids from the disk.
     *
     * @param removeList
     *            - a collection of cache ids.
     */
    @Override
    public void delCacheEntry(ValueSet removeList, int cause, int source, boolean fromDepIdTemplateInvalidation, boolean fireEvent) {
        htod.delCacheEntry(removeList, cause, source, fromDepIdTemplateInvalidation, fireEvent);
    }

    /**
     * Call this method to read a specified dependency id which contains the cache ids from the disk.
     *
     * @param id
     *            - dependency id.
     * @param delete
     *            - boolean to delete the dependency id after reading
     * @return valueSet - the collection of cache ids.
     */
    @Override
    public ValueSet readDependency(Object id, boolean delete) { // SKS-O
        Result result = htod.readDependency(id, delete);
        if (result.returnCode == HTODDynacache.DISK_EXCEPTION) {
            stopOnError(result.diskException);
            this.htod.returnToResultPool(result);
            return HTODDynacache.EMPTY_VS;
        }
        ValueSet valueSet = (ValueSet) result.data;
        if (valueSet == null) {
            valueSet = HTODDynacache.EMPTY_VS;
        }
        this.htod.returnToResultPool(result);
        return valueSet;
    }

    /**
     * Call this method to read a specified template which contains the cache ids from the disk.
     *
     * @param template
     *            - template id.
     * @param delete
     *            - boolean to delete the template after reading
     * @return valueSet - the collection of cache ids.
     */
    @Override
    public ValueSet readTemplate(String template, boolean delete) {
        Result result = htod.readTemplate(template, delete);
        if (result.returnCode == HTODDynacache.DISK_EXCEPTION) {
            stopOnError(result.diskException);
            this.htod.returnToResultPool(result);
            return HTODDynacache.EMPTY_VS;
        }
        ValueSet valueSet = (ValueSet) result.data;
        if (valueSet == null) {
            valueSet = HTODDynacache.EMPTY_VS;
        }
        this.htod.returnToResultPool(result);
        return valueSet;
    }

    /**
     * Call this method to get the cache ids based on the index and the length from the disk.
     *
     * @param index
     *            If index = 0, it starts the beginning. If index = 1, it means "next". If Index = -1, it means
     *            "previous".
     * @param length
     *            The max number of templates to be read. If length = -1, it reads all templates until the end.
     * @return valueSet - the collection of cache ids.
     */
    @Override
    public ValueSet readCacheIdsByRange(int index, int length) {
        Result result = htod.readCacheIdsByRange(index, length);
        if (result.returnCode == HTODDynacache.DISK_EXCEPTION) {
            stopOnError(result.diskException);
            this.htod.returnToResultPool(result);
            return HTODDynacache.EMPTY_VS;
        }
        ValueSet valueSet = (ValueSet) result.data;
        if (valueSet == null) {
            valueSet = HTODDynacache.EMPTY_VS;
        }
        this.htod.returnToResultPool(result);
        return valueSet;
    }

    /**
     * Call this method to get the dependency ids based on the index and the length from the disk.
     *
     * @param index
     *            If index = 0, it starts the beginning. If index = 1, it means "next". If Index = -1, it means
     *            "previous".
     * @param length
     *            The max number of templates to be read. If length = -1, it reads all templates until the end.
     * @return valueSet - the collection of dependency ids.
     */
    @Override
    public ValueSet readDependencyByRange(int index, int length) {
        Result result = htod.readDependencyByRange(index, length);
        if (result.returnCode == HTODDynacache.DISK_EXCEPTION) {
            stopOnError(result.diskException);
            this.htod.returnToResultPool(result);
            return HTODDynacache.EMPTY_VS;
        }
        ValueSet valueSet = (ValueSet) result.data;
        if (valueSet == null) {
            valueSet = HTODDynacache.EMPTY_VS;
        }
        this.htod.returnToResultPool(result);
        return valueSet;
    }

    /**
     * Call this method to get the template ids based on the index and the length from the disk.
     *
     * @param index
     *            If index = 0, it starts the beginning. If index = 1, it means "next". If Index = -1, it means
     *            "previous".
     * @param length
     *            The max number of templates to be read. If length = -1, it reads all templates until the end.
     * @return valueSet - the collection of templates.
     */
    @Override
    public ValueSet readTemplatesByRange(int index, int length) {
        Result result = htod.readTemplatesByRange(index, length);
        if (result.returnCode == HTODDynacache.DISK_EXCEPTION) {
            stopOnError(result.diskException);
            this.htod.returnToResultPool(result);
            return HTODDynacache.EMPTY_VS;
        }
        ValueSet valueSet = (ValueSet) result.data;
        if (valueSet == null) {
            valueSet = HTODDynacache.EMPTY_VS;
        }
        this.htod.returnToResultPool(result);
        return valueSet;
    }

    /**
     * Call this method to get the number of the cache ids in the disk.
     *
     * @param filter
     *            if true, filter the size from the invalidation buffer. Else, no filter - real size of entries in the
     *            disk
     * @return int - the size
     */
    @Override
    public int getCacheIdsSize(boolean filter) {
        if (filter == CacheOnDisk.FILTER) {
            return htod.getCacheIdsSize(filter) - htod.invalidationBuffer.size();
        } else {
            return htod.getCacheIdsSize(filter);
        }

    }

    /**
     * Call this method to get the number of the dependency ids in the disk.
     *
     * @return int - the size
     */
    @Override
    public int getDepIdsSize() {
        return htod.getDepIdsSize();
    }

    /**
     * Call this method to get the number of the templates in the disk.
     *
     * @return int - the size
     */
    @Override
    public int getTemplatesSize() {
        return htod.getTemplatesSize();
    }

    /**
     * Call this method to get the total number of cache entries size in the disk.
     *
     * @return long - the size
     */
    @Override
    public long getCacheSizeInBytes() {
        return this.currentCacheSizeInBytes;
    }

    @Override
    public int getDiskCacheSizeLimit() { // 3821 NK begin
        return this.diskCacheSizeInfo.diskCacheSizeLimit;
    }

    @Override
    public int getDiskCacheSizeHighLimit() { // 3821 NK begin
        return this.diskCacheSizeInfo.diskCacheSizeHighLimit;
    }

    public int getDiskCachesizeLowLimit() {
        return this.diskCacheSizeInfo.diskCacheSizeLowLimit;
    }

    @Override
    public int getDiskCacheSizeInGBLimit() {
        return this.diskCacheSizeInfo.diskCacheSizeInGBLimit;
    }

    @Override
    public long getDiskCacheEntrySizeInBytesLimit() {
        return this.diskCacheSizeInfo.diskCacheEntrySizeInBytesLimit;
    }

    @Override
    public long getDiskCacheSizeInBytesLimit() {
        return this.diskCacheSizeInfo.getDiskCacheSizeInBytesLimit();
    }

    @Override
    public long getDiskCacheSizeInBytesHighLimit() {
        return this.diskCacheSizeInfo.getDiskCacheSizeInBytesHighLimit();
    }

    public long getDiskCacheSizeInBytesLowLimit() {
        return this.diskCacheSizeInfo.getDiskCacheSizeInBytesLowLimit();
    }

    @Override
    public int getEvictionPolicy() {
        return this.evictionPolicy;
    } // 3821 NK end

    /**
     * Call this method to delete a cache id from a specified dependency in the disk.
     *
     * @param id
     *            - dependency id.
     * @param entry
     *            - cache id.
     */
    @Override
    public void delDependencyEntry(Object id, Object entry) { // SKS-O
        if (htod.delDependencyEntry(id, entry) == HTODDynacache.DISK_EXCEPTION) {
            stopOnError(this.htod.diskCacheException);
        }
    }

    /**
     * Call this method to delete a cache id from a specified template in the disk.
     *
     * @param template
     *            - template id.
     * @param entry
     *            - cache id.
     */
    @Override
    public void delTemplateEntry(String template, Object entry) {
        if (htod.delTemplateEntry(template, entry) == HTODDynacache.DISK_EXCEPTION) {
            stopOnError(this.htod.diskCacheException);
        }
    }

    /**
     * Call this method to delete speciifed dependency id from the disk.
     *
     * @param id
     *            - dependency id.
     */
    @Override
    public void delDependency(Object id) { // SKS-O
        if (htod.delDependency(id) == HTODDynacache.DISK_EXCEPTION) {
            stopOnError(this.htod.diskCacheException);
        }
    }

    /**
     * Call this method to delete speciifed template from the disk.
     *
     * @param id
     *            - template id.
     */
    @Override
    public void delTemplate(String template) {
        if (htod.delTemplate(template) == HTODDynacache.DISK_EXCEPTION) {
            stopOnError(this.htod.diskCacheException);
        }
    }

    /**
     * Call this method to write a dependency id with a collection of cache ids to the disk.
     *
     * @param id
     *            - dependency id.
     * @param vs
     *            - a collection of cache ids.
     */
    @Override
    public int writeDependency(Object id, ValueSet vs) { // SKS-O
        int returnCode = htod.writeDependency(id, vs);
        if (returnCode == HTODDynacache.DISK_EXCEPTION) {
            stopOnError(this.htod.diskCacheException);
        }
        return returnCode;
    }

    /**
     * Call this method to write a template with a collection of cache ids to the disk.
     *
     * @param template
     *            - template id.
     * @param vs
     *            - a collection of cache ids.
     */
    @Override
    public int writeTemplate(String template, ValueSet vs) {
        int returnCode = htod.writeTemplate(template, vs);
        if (returnCode == HTODDynacache.DISK_EXCEPTION) {
            stopOnError(this.htod.diskCacheException);
        }
        return returnCode;
    }

    /**
     * Call this method to add a cache id for a specified dependency id to the disk.
     *
     * @param id
     *            - dependency id.
     * @param entry
     *            - cache id.
     */
    @Override
    public int writeDependencyEntry(Object id, Object entry) { // SKS-O
        int returnCode = htod.writeDependencyEntry(id, entry);
        if (returnCode == HTODDynacache.DISK_EXCEPTION) {
            stopOnError(this.htod.diskCacheException);
        }
        return returnCode;
    }

    /**
     * Call this method to add a cache id for a specified template to the disk.
     *
     * @param template
     *            - template id.
     * @param entry
     *            - cache id.
     */
    @Override
    public int writeTemplateEntry(String template, Object entry) {
        int returnCode = htod.writeTemplateEntry(template, entry);
        if (returnCode == HTODDynacache.DISK_EXCEPTION) {
            stopOnError(this.htod.diskCacheException);
        }
        return returnCode;
    }

    /**
     * This method returns true if disk cache contains a mapping for the specified key.
     *
     * @param key
     *            - key is to be tested.
     * @return true if disk cache contains a mapping for the specified key.
     */
    @Override
    public boolean containsKey(Object key) {
        return htod.containsKey(key);
    }

    /**
     * This method gets the start state of HTOD which is used in the Cache.start() method
     *
     * @return startState START_NONE, START_LPBT_SCAN, START_LPBT_REMOVE
     */
    @Override
    public int getStartState() {
        return this.startState;
    }

    /**
     * This method returns true if EvictionTable needs to be populated on server restart
     **/
    @Override
    public boolean shouldPopulateEvictionTable() {
        return this.populateEvictionTable;
    }

    /**
     * This method gets the current pending removal size in the disk invalidation buffers.
     */
    @Override
    public int getPendingRemovalSize() {
        return htod.getPendingRemovalSize();
    }

    /**
     * This method gets the current size of dependency id buckets in the memory table for the disk.
     */
    @Override
    public int getDepIdsBufferedSize() {
        return htod.getDepIdsBufferedSize();
    }

    /**
     * This method gets the current size of template buckets in the memory table for the disk.
     */
    @Override
    public int getTemplatesBufferedSize() {
        return htod.getTemplatesBufferedSize();
    }

    // return the sleep time in msec
    protected long calculateSleepTime() {
        Calendar c = new GregorianCalendar();
        int currentHour = c.get(Calendar.HOUR_OF_DAY);
        int currentMin = c.get(Calendar.MINUTE);
        int currentSec = c.get(Calendar.SECOND);
        long stime = SECONDS_FOR_24_HOURS - ((currentHour * 60 + currentMin) * 60 + currentSec) + cleanupHour * 60 * 60;
        if (stime > SECONDS_FOR_24_HOURS) {
            stime = stime - SECONDS_FOR_24_HOURS;
        }
        if (stime < 10) {
            stime = 10;
        }
        stime = stime * 1000; // convert to msec
        return stime;
    }

    /**
     * This method checks whether the disk cleanup is running or not
     */
    @Override
    public boolean isCleanupRunning() {
        return this.htod.invalidationBuffer.isBackgroundInvalidationInProgress();
    }

    /**
     * This method clears invalidaiton buffers in HTODDynacache
     */
    @Override
    public void clearInvalidationBuffers() {
        this.htod.invalidationBuffer.clear(HTODInvalidationBuffer.EXPLICIT_BUFFER); // 3821 NK begin
        this.htod.invalidationBuffer.clear(HTODInvalidationBuffer.SCAN_BUFFER);
        if (this.evictionPolicy != CacheConfig.EVICTION_NONE) {
            this.htod.invalidationBuffer.clear(HTODInvalidationBuffer.GC_BUFFER); // 3821 NK end
        }
    }

    /**
     * This method checks whether invalidaiton buffers in HTODDynacache is full or not
     */
    @Override
    @Trivial
    public boolean isInvalidationBuffersFull() {
        return this.htod.invalidationBuffer.isFull();
    }

    /**
     * This method releases unused pools which is over its life time.
     */
    @Override
    public void releaseUnusedPools() {
        this.htod.releaseUnusedPools();
    }

    /**
     * This method find the hashcode based on index and length.
     *
     * @param index
     *            If index = 0, it starts the beginning. If index = 1, it means "next". If Index = -1, it means
     *            "previous".
     * @param length
     *            The max number of cache ids to be read. If length = -1, it reads all cache ids until the end.
     * @param debug
     *            true to output id and its hashcode to systemout.log
     * @param useValue
     *            true to calculate hashcode including value
     */
    @Override
    public Result readHashcodeByRange(int index, int length, boolean debug, boolean useValue) { // LI4337-17
        Result result = this.htod.readHashcodeByRange(index, length, debug, useValue);
        if (result.returnCode == HTODDynacache.DISK_EXCEPTION) {
            stopOnError(result.diskException);
        }
        return result;
    }

    /**
     * This method is used to update expiration times in GC and disk emtry header
     */
    @Override
    public int updateExpirationTime(Object id, long oldExpirationTime, int size, long newExpirationTime, long newValidatorExpirationTime) {
        int returnCode = this.htod.updateExpirationTime(id, oldExpirationTime, size, newExpirationTime, newValidatorExpirationTime);
        if (returnCode == HTODDynacache.DISK_EXCEPTION) {
            stopOnError(this.htod.diskCacheException);
        }
        return returnCode;
    }

    @Override
    public Exception getDiskCacheException() {
        return htod.diskCacheException;
    }

    @Override
    public void waitForCleanupComplete() {
        final String methodName = "waitForCleanupComplete()";
        if (this.diskCleanupThread != null) {
            boolean running = false;
            synchronized (this.diskCleanupThread) {
                running = this.diskCleanupThread.currentThread != null && this.diskCleanupThread.scan;
            }

            if (running) {
                synchronized (this.diskCacheMonitor) {
                    this.doNotify = true;
                    try {
                        traceDebug(methodName, "waiting for cleanup completion (max 5 sec) for cache name \"" + this.cacheName);
                        this.diskCacheMonitor.wait(5000);
                    } catch (Exception ex) {
                    }
                    this.doNotify = false;
                }
            }
        }
    }

    private boolean findSwapDirPath(String location) {

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, cacheName + " findSwapDirPath " + location);
        }

        int index = location.indexOf("##");
        if (index == -1) {
            alternateLocation = location;
        } else {
            definedLocation = location.substring(0, index);
            alternateLocation = location.substring(index + 2);
        }

        if (!definedLocation.equals("")) {
            definedLocation = definedLocation + sep + diskCacheName;
        }
        if (!alternateLocation.equals("")) {
            alternateLocation = alternateLocation + sep + diskCacheName;
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, cacheName + " definedLocation " + definedLocation);
            Tr.debug(tc, cacheName + " alternateLocation " + alternateLocation);
        }

        int rc1 = checkDirectoryWriteable(definedLocation);
        if (tc.isDebugEnabled())
            Tr.debug(tc, cacheName + " rc1 " + rc1);

        if (rc1 == CacheOnDisk.LOCATION_OK) {
            swapDirPath = definedLocation;
            if (tc.isInfoEnabled())
                Tr.info(tc, "DYNA0053I", new Object[] { this.cacheName, definedLocation });
        } else {
            int rc2 = checkDirectoryWriteable(alternateLocation);
            if (tc.isDebugEnabled())
                Tr.debug(tc, cacheName + " rc2 " + rc2);
            if (rc2 == CacheOnDisk.LOCATION_OK && rc1 == CacheOnDisk.LOCATION_NOT_DEFINED) {
                swapDirPath = alternateLocation;
                if (tc.isInfoEnabled())
                    Tr.info(tc, "DYNA0053I", new Object[] { this.cacheName, alternateLocation });
            } else if (rc2 == CacheOnDisk.LOCATION_OK) {
                swapDirPath = alternateLocation;
                if (tc.isWarningEnabled())
                    Tr.warning(tc, "DYNA0054W", new Object[] { this.cacheName, definedLocation, alternateLocation });
            } else {
                swapDirPath = "";
                if (tc.isErrorEnabled())
                    Tr.error(tc, "DYNA0055E", new Object[] { this.cacheName, definedLocation, alternateLocation });
                cache.setSwapToDisk(false);
                return false;
            }
        }

        return true;
    }

    /**
     * Check the location whether it is writable or not
     *
     * @param location
     * @return int 0 = OK 1 = LOCATION_NOT_DEFINED 2 = INVALID_LOCATION 3 = LOCATION_NOT_DIRECTORY 4 =
     *         LOCATION_NOT_WRITABLE 5 = LOCATION_CANNOT_MAKE_DIR
     */
    protected int checkDirectoryWriteable(String location) {
        final String methodName = "checkDirectoryWriteable()";
        int rc = CacheOnDisk.LOCATION_OK;
        if (location.equals("")) {
            rc = CacheOnDisk.LOCATION_NOT_DEFINED;
        } else if (location.startsWith("${") && location.indexOf("}") > 0) {
            rc = CacheOnDisk.LOCATION_NOT_VALID;
        } else {
            try {
                File f = new File(location);
                if (!f.exists()) {
                    if (!f.mkdirs()) {
                        rc = CacheOnDisk.LOCATION_CANNOT_MAKE_DIR;
                    }
                } else if (!f.isDirectory()) {
                    rc = CacheOnDisk.LOCATION_NOT_DIRECTORY;
                } else if (!f.canWrite()) {
                    rc = CacheOnDisk.LOCATION_NOT_WRITABLE;
                }
            } catch (Throwable t) {
                rc = CacheOnDisk.LOCATION_NOT_WRITABLE;
                com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.cache.CacheOnDisk.checkDirectoryWriteable", "1779", this);
                traceDebug(methodName, "cacheName=" + this.cacheName + " location=" + location + "\nException: " + ExceptionUtility.getStackTrace(t));
            }
        }

        traceDebug(methodName, "cacheName=" + this.cacheName + " location=" + location + " rc=" + rc);
        return rc;
    }

    /**
     * Return a boolean to indicate whether the specified cache id exists in the aux dependency table
     */
    @Override
    public boolean isCacheIdInAuxDepIdTable(Object id) {
        return this.htod.isCacheIdInAuxDepIdTable(id);
    }

    private Future<?> spawnThread(Runnable task) {
        return Scheduler.submit(task);
    }

    private void traceDebug(String methodName, String message) {
        if (IS_UNIT_TEST) {
            System.out.println(this.getClass().getName() + "." + methodName + " " + message);
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " " + message);
            }
        }
    }
}