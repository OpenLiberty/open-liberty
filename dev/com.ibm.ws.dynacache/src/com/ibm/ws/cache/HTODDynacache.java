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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.ibm.websphere.cache.InvalidationEvent;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cache.intf.DCache;
import com.ibm.ws.cache.persistent.filemgr.FileManager;
import com.ibm.ws.cache.persistent.filemgr.FileManagerException;
import com.ibm.ws.cache.persistent.filemgr.FileManagerImpl;
import com.ibm.ws.cache.persistent.htod.HashtableAction;
import com.ibm.ws.cache.persistent.htod.HashtableEntry;
import com.ibm.ws.cache.persistent.htod.HashtableOnDisk;
import com.ibm.ws.cache.persistent.htod.HashtableOnDiskException;
import com.ibm.ws.cache.stat.CachePerf;
import com.ibm.ws.cache.util.ExceptionUtility;
import com.ibm.ws.cache.util.SerializationUtility;
import com.ibm.ws.util.ObjectPool;

public class HTODDynacache {

    TraceComponent tc = Tr.register(HTODDynacache.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

    public final static boolean CHECK_EXPIRED = true;
    public final static boolean DELETE = true;
    public final static boolean EXIST = true;
    public final static boolean COMPLETE_CLEAR = true;
    public final static boolean FILTER = true;
    public final static boolean ALL = true;
    public final static boolean ADD_IF_NEW = true;
    public final static int NUM_SCAN = 50;
    public final static String DISKCACHE_MORE = "DISKCACHE_MORE";
    public final static boolean CALLED_FROM_REMOVE = true;

    public final static int NO_EXCEPTION = 0;
    public final static int DISK_EXCEPTION = 1;
    public final static int DISK_SIZE_OVER_LIMIT_EXCEPTION = 2;
    public final static int OTHER_EXCEPTION = 3;
    public final static int SERIALIZATION_EXCEPTION = 4;
    public final static int DISK_SIZE_IN_ENTRIES_OVER_LIMIT_EXCEPTION = 5;
    public final static int DISK_CACHE_ENTRY_SIZE_OVER_LIMIT_EXCEPTION = 6;
    public final static int NO_EXCEPTION_ENTRY_OVERWRITTEN = 7;
    public final static int NO_HASHCODE_OLD_FORMAT = 8;

    public final static int CACHE_ID_DATA = 1;
    public final static int DEP_ID_DATA = 2;
    public final static int TEMPLATE_ID_DATA = 3;

    public final static String DISK_CACHE_IN_GB_OVER_LIMIT_MSG = "The disk cache size in GB is over the limit.";

    public final static ValueSet EMPTY_VS = new ValueSet(1);

    protected java.util.concurrent.locks.ReadWriteLock rwLock = new ReentrantReadWriteLock();

    public PrimitiveArrayPool byteArrayPool = null;
    public PrimitiveArrayPool longArrayPool = null;
    public HashtableEntryObjectPool htEntryPool = null;
    protected ResultObjectPool resultPool = null;
    protected EvictionEntryPool evictionEntryPool = null;

    protected HTODInvalidationBuffer invalidationBuffer = null;

    //
    // The three HTODS: on for objects, one for dependencies, and one for templates.
    //
    protected String filename; // base name of HTOD file
    protected String dirname;
    protected FileManager object_filemgr; // File manager instance for objects
    protected FileManager dependency_filemgr; // File manager instance for dependencies
    protected FileManager template_filemgr; // File manager instance for templates

    public static String object_suffix = "-objects";
    public static String dependency_suffix = "-dependencies";
    public static String template_suffix = "-templates";

    protected HashtableOnDisk object_cache = null;
    protected HashtableOnDisk dependency_cache = null;
    protected HashtableOnDisk template_cache = null;

    protected DCache cache = null;
    protected CacheOnDisk cod = null;

    boolean auto_rehash = false;
    long scanExpiredTime = 0;
    public String cacheName = "";

    //
    // The hashtable will automatically double and rehash all entries if it gets
    // too full, to cut down on collisions.  The point at which it decides to double
    // is the threshold.  Below we set the threshold to 75% full.  The initial size
    // is a prime number approximately 25% bigger than the default max expected
    // number of entries.  The number is chosen to hold all the fragments in the
    // fragment cache for the olympics for wont of a better idea.  Because doubling
    // is quite expensive, we probably should use a prime number something over
    // 1 million, which would impose an overhead of about 25 MB to hold the indexes
    // for the three HTODs.
    //
    protected int initial_hashtable_size = 1296109;
    protected int hashtable_threshold = 75;
    protected int initial_valueset_size = 509;
    protected boolean valueset_rehash = true;

    // previously set at FileManager.ORIGINAL

    protected int physical_disk_manager = FileManager.MULTIVOLUME;

    protected ValueSet removeIdsList = null;

    // CPF_PMI - now using cache.cachePerf
    //CachePerf cachePerf = ServerCache.cachePerf;

    protected HTODDependencyTable auxDataDependencyTable = null;
    protected HTODDependencyTable auxTemplateDependencyTable = null;

    protected boolean delayOffload = false;
    protected boolean disableDependencyId = false;
    protected boolean disableTemplatesSupport = false;
    protected long minDiskCacheSizeInBytes = 0;
    protected BinaryHeap EvictionTable = null;
    protected Object evictionTableMonitor = new Object();
    protected Random rand = null;
    protected Exception diskCacheException = null;
    protected boolean deleteDiskFiles = false;
    protected long timeElapsedWriteAuxTables = 0;
    protected int numDepIdsInAuxTable = 0;
    protected int numCacheIdsInDepIdAuxTable = 0;
    protected int numTemplatesInAuxTable = 0;
    protected int numCacheIdsInTemplateAuxTable = 0;
    protected int numExplicitBufferLimitOnStop = 0;
    protected HashMap<Object, Object> cacheIdsTable = null;
    //protected boolean testException = true;  // for debug use only
    long totalDeleted = 0;
    long totalDeletedSize = 0;

    /*************************************************************************
     * Constructor
     * Used for debug only
     *************************************************************************/
    public HTODDynacache() {
        PrimitiveArrayPool.PoolConfig bytePoolConfig = new PrimitiveArrayPool.PoolConfig();
        bytePoolConfig.type = PrimitiveArrayPool.PoolConfig.TYPE_BYTE; // Byte array type
        bytePoolConfig.numberOfPools = 20; // Number of byte array pools
        bytePoolConfig.poolSize = 2; // Number of byte arrays in each pool
        bytePoolConfig.poolEntryLife = 1000 * 60 * 5; // Pool entry life in ms
        bytePoolConfig.scanFrequency = 1000 * 60 * 1; // frequency to scan pool in ms
        byteArrayPool = new PrimitiveArrayPool(bytePoolConfig, this.cacheName);

        PrimitiveArrayPool.PoolConfig longPoolConfig = new PrimitiveArrayPool.PoolConfig();
        longPoolConfig.type = PrimitiveArrayPool.PoolConfig.TYPE_LONG; // Long array type
        longPoolConfig.numberOfPools = 20; // Number of long array pools
        longPoolConfig.poolSize = 2; // Number of long arrays in each pool
        longPoolConfig.poolEntryLife = 1000 * 60 * 5; // Pool entry life in ms
        longPoolConfig.scanFrequency = 1000 * 60 * 1; // frequency to scan pool in ms
        longArrayPool = new PrimitiveArrayPool(longPoolConfig, this.cacheName);

        this.htEntryPool = new HashtableEntryObjectPool(200);
        this.resultPool = new ResultObjectPool(10);
        this.minDiskCacheSizeInBytes = calculateMinCacheSizeInBytes();
        this.rand = new Random(System.currentTimeMillis());
    }

    /*************************************************************************
     * Constructor
     * 
     * @param filename The name of the file containing the three HTODs
     *************************************************************************/
    public HTODDynacache(String dirname,
                         String filename,
                         DCache cache,
                         CacheOnDisk cod)
        throws IOException, ClassNotFoundException, FileManagerException {
        this.dirname = dirname;
        this.filename = filename;
        this.cache = cache;
        this.cod = cod;
        this.cacheName = cache.getCacheName();
        this.delayOffload = cod.delayOffload;
        this.disableDependencyId = cod.disableDependencyId;
        this.disableTemplatesSupport = cod.disableTemplatesSupport;

        PrimitiveArrayPool.PoolConfig bytePoolConfig = new PrimitiveArrayPool.PoolConfig();
        bytePoolConfig.type = PrimitiveArrayPool.PoolConfig.TYPE_BYTE; // Byte array type
        bytePoolConfig.numberOfPools = cod.numberOfPools; // Number of byte array pools
        bytePoolConfig.poolSize = cod.poolSize; // Number of byte arrays in each pool
        bytePoolConfig.poolEntryLife = cod.poolEntryLife; // Pool entry life in ms
        bytePoolConfig.scanFrequency = 1000 * 60 * 1; // frequency to scan pool in ms
        byteArrayPool = new PrimitiveArrayPool(bytePoolConfig, this.cacheName);

        PrimitiveArrayPool.PoolConfig longPoolConfig = new PrimitiveArrayPool.PoolConfig();
        longPoolConfig.type = PrimitiveArrayPool.PoolConfig.TYPE_LONG; // Long array type
        longPoolConfig.numberOfPools = cod.numberOfPools; // Number of long array pools
        longPoolConfig.poolSize = cod.poolSize; // Number of long arrays in each pool
        longPoolConfig.poolEntryLife = cod.poolEntryLife; // Pool entry life in ms
        longPoolConfig.scanFrequency = 1000 * 60 * 1; // frequency to scan pool in ms
        longArrayPool = new PrimitiveArrayPool(longPoolConfig, this.cacheName);

        this.htEntryPool = new HashtableEntryObjectPool(200);

        this.invalidationBuffer = new HTODInvalidationBuffer(this.cod);

        init_files();

        if (delayOffload) {
            if (!this.disableDependencyId) {
                this.auxDataDependencyTable = new HTODDependencyTable(HTODDependencyTable.DEP_ID_TABLE, 1000, cod.delayOffloadDepIdBuckets, 2, cod.delayOffloadEntriesLimit, cod.dependencyCacheIndexEnabled, this);
            }
            if (!this.disableTemplatesSupport) {
                this.auxTemplateDependencyTable = new HTODDependencyTable(HTODDependencyTable.TEMPLATE_TABLE, 100, cod.delayOffloadTemplateBuckets, 2, cod.delayOffloadEntriesLimit, cod.dependencyCacheIndexEnabled, this);
            }
        }

        this.minDiskCacheSizeInBytes = calculateMinCacheSizeInBytes();

        ValueSet valueSet = this.cod.readAndDeleteInvalidationFile();
        if (valueSet != null && valueSet.size() > 0) {
            this.invalidationBuffer.add(valueSet, HTODInvalidationBuffer.EXPLICIT_BUFFER, CachePerf.DIRECT, CachePerf.LOCAL, !Cache.FROM_DEPID_TEMPLATE_INVALIDATION,
                                        HTODInvalidationBuffer.FIRE_EVENT, !HTODInvalidationBuffer.CHECK_FULL);
            this.cod.startState = CacheOnDisk.START_LPBT_REMOVE;
            valueSet.clear();
            valueSet = null;
        }
        if (this.cod.currentCacheSizeInBytes == 0) {
            if (getCacheIdsSize() == 0) {
                this.cod.currentCacheSizeInBytes = this.minDiskCacheSizeInBytes;
            }
        }
        this.resultPool = new ResultObjectPool(10);
        this.rand = new Random(System.currentTimeMillis());
        this.totalDeleted = 0;
        this.totalDeletedSize = 0;
    }

    /*************************************************************************
     * init_files()
     * This method opens a FileManager instance, creates (if needed) the
     * three HTOD instances on disk, and then initializes the HTOD instances
     * for use by this class.
     *************************************************************************/
    protected void init_files() throws IOException, ClassNotFoundException, FileManagerException {
        initFileManager();

        if ((object_cache = HashtableOnDisk.getInstance(object_filemgr, auto_rehash, 0, HashtableOnDisk.HAS_CACHE_VALUE, this)) == null) {
            HashtableOnDisk.createInstance(object_filemgr,
                                           cod.dataHashtableSize,
                                           hashtable_threshold);
            this.cod.dataFiles = this.cod.dataGB = 1;
            object_cache = HashtableOnDisk.getInstance(object_filemgr, auto_rehash, 0, HashtableOnDisk.HAS_CACHE_VALUE, this);
        }

        if (!this.disableDependencyId) {
            if ((dependency_cache = HashtableOnDisk.getInstance(dependency_filemgr, auto_rehash, 0, !HashtableOnDisk.HAS_CACHE_VALUE, this)) == null) {
                HashtableOnDisk.createInstance(dependency_filemgr,
                                               cod.depIdHashtableSize,
                                               hashtable_threshold);
                this.cod.dependencyIdFiles = this.cod.dependencyIdGB = 1;
                dependency_cache = HashtableOnDisk.getInstance(dependency_filemgr, auto_rehash, 0, !HashtableOnDisk.HAS_CACHE_VALUE, this);
            }
        } else {
            this.cod.dependencyIdFiles = this.cod.dependencyIdGB = 0;
        }

        if (!this.disableTemplatesSupport) {
            if ((template_cache = HashtableOnDisk.getInstance(template_filemgr, auto_rehash, 0, !HashtableOnDisk.HAS_CACHE_VALUE, this)) == null) {
                HashtableOnDisk.createInstance(template_filemgr,
                                               cod.templateHashtableSize,
                                               hashtable_threshold);
                this.cod.templateFiles = this.cod.templateGB = 1;
                template_cache = HashtableOnDisk.getInstance(template_filemgr, auto_rehash, 0, !HashtableOnDisk.HAS_CACHE_VALUE, this);
            }
        } else {
            this.cod.templateFiles = this.cod.templateGB = 0;
        }
    }

    /*************************************************************************
     * initfileManager()
     * Initialize the file manager instance. Caching within the filemanager is
     * experimental so it is disabled here.
     *************************************************************************/
    void initFileManager() throws IOException, FileManagerException

    {
        final String methodName = "initFileManager()";
        object_filemgr = new FileManagerImpl(filename + object_suffix + ".htod", // filename
        false, // automatic coalesce, not reccomended
        "rw", // "r" or "rw"
        physical_disk_manager, // physical file layer
        this);

        if (!this.disableDependencyId) {
            dependency_filemgr = new FileManagerImpl(filename + dependency_suffix + ".htod", // filename
            false, // automatic coalesce, not reccomended
            "rw", // "r" or "rw"
            physical_disk_manager, // physical file layer
            this);
        }

        if (!this.disableTemplatesSupport) {
            template_filemgr = new FileManagerImpl(filename + template_suffix + ".htod", // filename
            false, // automatic coalesce, not reccomended
            "rw", // "r" or "rw"
            physical_disk_manager, // physical file layer
            this);
        }
    }

    /*************************************************************************
     * initializeEvictionTable()
     *************************************************************************/
    public void initializeEvictionTable() {
        evictionEntryPool = new EvictionEntryPool(500);
        EvictionTable = new BinaryHeap(new EvictionTableEntry());
    }

    /*************************************************************************
     * close()
     * Close all HTOD instances and then the filemanager.
     *************************************************************************/
    public void close() {

        try {
            rwLock.writeLock().lock();
            closeNoRWLock();
        } finally {
            rwLock.writeLock().unlock();
        }

        if (this.deleteDiskFiles) {
            deleteDiskCacheFiles();
            this.deleteDiskFiles = false;
        }
    }

    private void closeNoRWLock() {
        final String methodName = "closeNoRWLock()";
        this.cod.diskCacheSizeInfo.allowOverflow = true;
        try {
            //dump_object_statistics();
            object_cache.close();
        } catch (Exception ex) {
            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.HTODDynacache.closeNoRWLock", "309", this);
            if (tc.isDebugEnabled())
                Tr.debug(tc, methodName, "this.cacheName=" + this.cacheName + "\n Exception: " + ExceptionUtility.getStackTrace(ex));
        }
        try {
            if (!this.disableDependencyId) {
                dependency_cache.close();
            }
        } catch (Exception ex) {
            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.HTODDynacache.closeNoRWLock", "315", this);
            if (tc.isDebugEnabled())
                Tr.debug(tc, methodName, "cacheName=" + this.cacheName + "\n Exception: " + ExceptionUtility.getStackTrace(ex));
        }
        try {
            if (!this.disableTemplatesSupport) {
                template_cache.close();
            }
        } catch (Exception ex) {
            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.HTODDynacache.closeNoRWLock", "321", this);
            if (tc.isDebugEnabled())
                Tr.debug(tc, methodName, "cacheName=" + this.cacheName + "\n Exception: " + ExceptionUtility.getStackTrace(ex));
        }
        try {
            object_filemgr.close();
        } catch (Exception ex) {
            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.HTODDynacache.closeNoRWLock", "327", this);
            if (tc.isDebugEnabled())
                Tr.debug(tc, methodName, "cacheName=" + this.cacheName + "\n Exception: " + ExceptionUtility.getStackTrace(ex));
        }
        try {
            if (!this.disableDependencyId) {
                dependency_filemgr.close();
            }
        } catch (Exception ex) {
            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.HTODDynacache.closeNoRWLock", "333", this);
            if (tc.isDebugEnabled())
                Tr.debug(tc, methodName, "cacheName=" + this.cacheName + "\n Exception: " + ExceptionUtility.getStackTrace(ex));
        }
        try {
            if (!this.disableTemplatesSupport) {
                template_filemgr.close();
            }
        } catch (Exception ex) {
            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.HTODDynacache.closeNoRWLock", "339", this);
            if (tc.isDebugEnabled())
                Tr.debug(tc, methodName, "cacheName=" + this.cacheName + "\n Exception: " + ExceptionUtility.getStackTrace(ex));
        } finally {
            this.cod.diskCacheSizeInfo.allowOverflow = false;
        }
    }

    /*************************************************************************
     * writeAuxiliaryDepTables()
     * This writes the memory HTOD Dependency Id table and Template table to the disk during system stop.
     *************************************************************************/
    public int writeAuxiliaryDepTables() {
        final String methodName = "writeAuxiliaryDepTables()";
        this.numDepIdsInAuxTable = 0;
        this.numTemplatesInAuxTable = 0;
        this.numCacheIdsInDepIdAuxTable = 0;
        this.numCacheIdsInTemplateAuxTable = 0;
        this.numExplicitBufferLimitOnStop = 0;
        int returnCode = NO_EXCEPTION;
        if (this.cod.valueSet == null) {
            this.cod.valueSet = new ValueSet(16);
        }
        if (delayOffload) {
            long start = System.nanoTime();
            if (!this.disableDependencyId) {
                if (!auxDataDependencyTable.isEmpty()) {
                    Enumeration e = auxDataDependencyTable.getKeys();
                    while (e.hasMoreElements()) {
                        Object id = e.nextElement();
                        if (auxDataDependencyTable.isUpdated(id)) {
                            ValueSet valueSet = auxDataDependencyTable.getEntries(id);
                            if (valueSet != null && valueSet.size() > 0) {
                                this.numCacheIdsInDepIdAuxTable += valueSet.size();
                            }
                            returnCode = writeValueSet(DEP_ID_DATA, id, valueSet, ALL); // valueSet may be empty after writeValueSet
                            this.cache.getCacheStatisticsListener().depIdsOffloadedToDisk(id);
                            if (returnCode == DISK_EXCEPTION) {
                                return returnCode;
                            }
                            if (returnCode == DISK_SIZE_OVER_LIMIT_EXCEPTION) {
                                int size = valueSet.size();
                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, methodName, "cacheName=" + this.cacheName + " depid=" + id + " disk overflow size=" + size);
                                if (size > 0) {
                                    this.cod.valueSet.union(valueSet);
                                }
                            }
                            this.numDepIdsInAuxTable++;
                        }
                    }
                    auxDataDependencyTable.clear();
                }
            }
            if (!this.disableTemplatesSupport) {
                if (!auxTemplateDependencyTable.isEmpty()) {
                    Enumeration e = auxTemplateDependencyTable.getKeys();
                    while (e.hasMoreElements()) {
                        Object template = e.nextElement();
                        if (auxTemplateDependencyTable.isUpdated(template)) {
                            ValueSet valueSet = auxTemplateDependencyTable.getEntries(template);
                            if (valueSet != null && valueSet.size() > 0) {
                                this.numCacheIdsInTemplateAuxTable += valueSet.size();
                            }
                            returnCode = writeValueSet(TEMPLATE_ID_DATA, template, valueSet, ALL); // valueSet may be empty after writeValueSet
                            this.cache.getCacheStatisticsListener().templatesOffloadedToDisk(template);
                            if (returnCode == DISK_EXCEPTION) {
                                return returnCode;
                            }
                            if (returnCode == DISK_SIZE_OVER_LIMIT_EXCEPTION) {
                                int size = valueSet.size();
                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, methodName, "cacheName=" + this.cacheName + " template=" + template + " disk overflow size=" + size);
                                if (size > 0) {
                                    this.cod.valueSet.union(valueSet);
                                }
                            }
                            this.numTemplatesInAuxTable++;
                        }
                    }
                    auxTemplateDependencyTable.clear();
                }
            }
            this.timeElapsedWriteAuxTables = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

            if (this.cod.valueSet != null && this.cod.valueSet.size() > 0) {
                this.numExplicitBufferLimitOnStop = this.cod.valueSet.size();
                if (this.cod.explicitBufferLimitOnStop > 0 && this.cod.valueSet.size() > this.cod.explicitBufferLimitOnStop) {
                    this.deleteDiskFiles = true;
                } else {
                    this.cod.createInvalidationFile();
                }
                this.cod.valueSet.clear();
                this.cod.valueSet = null;
            }
        }
        return returnCode;
    }

    /*************************************************************************
     * dump_object_statistics()
     * Dump filemanager and HTOD statistics to stdout. We could capture these
     * for forward to monitors if needed.
     *************************************************************************/
    public void dump_object_statistics() {
        final String methodName = "dump_object_statistics()";
        try {
            OutputStreamWriter out = new OutputStreamWriter(System.out);
            out.write("========================================================================");
            out.write("------------------ Object File Manager Statistics ---------------\n");
            object_filemgr.dump_stats(out, true);
            out.write("------------------ Object HTOD Statistics ---------------\n");
            object_cache.dump_htod_stats(out, true);
            out.write("========================================================================");

            if (!this.disableDependencyId) {
                out.write("========================================================================");
                out.write("------------------ Dependency File Manager Statistics ---------------\n");
                dependency_filemgr.dump_stats(out, true);
                out.write("------------------ Dependency HTOD Statistics ---------------\n");
                dependency_cache.dump_htod_stats(out, true);
                out.write("========================================================================");
            }

            if (!this.disableTemplatesSupport) {
                out.write("========================================================================");
                out.write("------------------ Template File Manager Statistics ---------------\n");
                template_filemgr.dump_stats(out, true);
                out.write("------------------ Template HTOD Statistics ---------------\n");
                template_cache.dump_htod_stats(out, true);
                out.write("========================================================================");
            }
            out.flush();
        } catch (Throwable t) {
            com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.cache.HTODDynacache.dump_object_statistics", "376", this);
            if (tc.isDebugEnabled())
                Tr.debug(tc, methodName, "cacheName=" + this.cacheName + "\nException: " + ExceptionUtility.getStackTrace(t));
        }
    }

    /*************************************************************************
     * clearDiskCache()
     * Clear the memory HTOD Dependency Id table and Template table. Delete all disk cache and
     * then opens a FileManager instance, recreates the three HOD instances on disk.
     *************************************************************************/
    public int clearDiskCache() {
        final String methodName = "clearDiskCache()";
        int returnCode = NO_EXCEPTION;
        Exception diskException = null;

        try {
            this.invalidationBuffer.setDiskClearInProgress(true);
            if (delayOffload) {
                if (!this.disableDependencyId) {
                    auxDataDependencyTable.clear();
                }
                if (!this.disableTemplatesSupport) {
                    auxTemplateDependencyTable.clear();
                }
            }

            stop(COMPLETE_CLEAR);

            try {
                rwLock.writeLock().lock();
                closeNoRWLock();
                deleteDiskCacheFiles(); // delete disk cache files
                this.cod.diskCacheSizeInfo.reset();
                init_files(); //restart things
                this.cod.enableCacheSizeInBytes = true;
                this.cod.currentCacheSizeInBytes = this.minDiskCacheSizeInBytes;
                if (this.cod.diskCacheSizeInfo.diskCacheSizeInGBLimit > 0) {
                    this.cache.setEnableDiskCacheSizeInBytesChecking(true);
                }
                if (this.cod.evictionPolicy != CacheConfig.EVICTION_NONE) {
                    synchronized (evictionTableMonitor) {
                        this.EvictionTable.clear();
                    }
                }
            } catch (FileManagerException ex) {
                this.diskCacheException = ex;
                diskException = ex;
                returnCode = DISK_EXCEPTION;
            } catch (HashtableOnDiskException ex) {
                this.diskCacheException = ex;
                diskException = ex;
                returnCode = DISK_EXCEPTION;
            } catch (IOException ex) {
                this.diskCacheException = ex;
                diskException = ex;
                returnCode = DISK_EXCEPTION;
            } catch (Exception ex) {
                returnCode = OTHER_EXCEPTION;
                diskException = ex;
            } finally {
                if (returnCode != NO_EXCEPTION) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, methodName, "cacheName=" + this.cacheName + "\n Exception: " + ExceptionUtility.getStackTrace(diskException));
                }
                if (returnCode == DISK_EXCEPTION || returnCode == OTHER_EXCEPTION) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(diskException, "com.ibm.ws.cache.HTODDynacache.clearDiskCache", "525", this);
                }
                rwLock.writeLock().unlock();
            }

        } finally {
            this.invalidationBuffer.setDiskClearInProgress(false);
        }
        return returnCode;
    }

    /*************************************************************************
     * stop()
     * Stop LPBT ASAP by clearing invalidation buffers. If "completeClear" is false,
     * create invalidation file and offload the invalidation buffer (EventAlreadyFired)
     * to the disk.
     *************************************************************************/
    public void stop(boolean completeClear) {
        final String methodName = "stop()";
        if (this.invalidationBuffer.isBackgroundInvalidationInProgress()) {
            this.invalidationBuffer.setLoopOnce(true);
            if (this.invalidationBuffer.isCleanupPending()) {
                this.invalidationBuffer.resetCleanupPending();
            }
            this.cod.waitForCleanupComplete();
            this.invalidationBuffer.setLoopOnce(false);
        }

        if (completeClear) {
            this.invalidationBuffer.clear(HTODInvalidationBuffer.EXPLICIT_BUFFER);
            this.invalidationBuffer.clear(HTODInvalidationBuffer.SCAN_BUFFER);
            this.invalidationBuffer.clear(HTODInvalidationBuffer.GC_BUFFER);
        } else {
            this.invalidationBuffer.clear(HTODInvalidationBuffer.SCAN_BUFFER);
            this.invalidationBuffer.clear(HTODInvalidationBuffer.GC_BUFFER);
            this.cod.valueSet = this.invalidationBuffer.getAndRemoveFromExplicitBuffer();
            if (this.cod.valueSet != null && this.cod.valueSet.size() > 0) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName, "cacheName=" + this.cacheName + " offload explicit buffer size=" + this.cod.valueSet.size());
            }
        }
    }

    /*************************************************************************
     * deleteDiskCacheFiles()
     * Delete all disk cache files per cache instance from the disk
     *************************************************************************/
    public void deleteDiskCacheFiles() {
        this.cod.deleteDiskCacheFiles(); // delete disk cache files
    }

    /*************************************************************************
     * delCacheEntry()
     * Delete cacheEntry from the disk. This also remove dependencies for all dataIds and templates
     * to the cacheEntry
     *************************************************************************/
    public void delCacheEntry(CacheEntry ce, int cause, int source, boolean fromDepIdTemplateInvalidation) {
        this.invalidationBuffer.add(ce.id, HTODInvalidationBuffer.EXPLICIT_BUFFER, cause, source, fromDepIdTemplateInvalidation, !HTODInvalidationBuffer.FIRE_EVENT,
                                    !HTODInvalidationBuffer.ALIAS_ID);
        for (int i = 0; i < ce.aliasList.length; i++) {
            this.invalidationBuffer.add(ce.aliasList[i], HTODInvalidationBuffer.EXPLICIT_BUFFER, cause, source, fromDepIdTemplateInvalidation, !HTODInvalidationBuffer.FIRE_EVENT,
                                        HTODInvalidationBuffer.ALIAS_ID);;
        }
    }

    /*************************************************************************
     * delCacheEntry()
     * Delete cacheEntry from the disk. This also remove dependencies for all dataIds and templates
     * to the cacheEntry
     *************************************************************************/
    public void delCacheEntry(ValueSet deleteList, int cause, int source, boolean fromDepIdTemplateInvalidation, boolean fireEvent) {
        if (deleteList != null && deleteList.size() > 0) {
            this.invalidationBuffer.add(deleteList, HTODInvalidationBuffer.EXPLICIT_BUFFER, cause, source, fromDepIdTemplateInvalidation, fireEvent,
                                        HTODInvalidationBuffer.CHECK_FULL);
        }
    }

    /*************************************************************************
     * delCacheEntry()
     * Delete cacheEntry from the disk. This also remove dependencies for all dataIds and templates
     * to the cacheEntry. Warning: the parameter id must be cache id NOT alias id.
     *************************************************************************/
    private Result delCacheEntry(Object id, int bufferType, boolean explicitBufferFirst) // 3821
    {
        final String methodName = "delCacheEntry()";
        Result result = getFromResultPool();
        HashtableEntry hte = null;
        EvictionTableEntry evt = null;
        Object currentId;
        boolean fireEvent = false;
        currentId = id;
        String msg = "DCThread";

        if (explicitBufferFirst == false) {
            msg = "GCThread";
        }

        if (bufferType == HTODInvalidationBuffer.EXPLICIT_BUFFER) {
            if (id instanceof ExplicitIdData) {
                byte info = ((ExplicitIdData) id).info;
                currentId = ((ExplicitIdData) id).id;
                result.cause = (info & HTODInvalidationBuffer.STATUS_CAUSE_MASK);
                result.source = (info & HTODInvalidationBuffer.STATUS_REMOTE) == 0 ? CachePerf.LOCAL : CachePerf.REMOTE;
                result.bFromDepIdTemplateInvalidation = (info & HTODInvalidationBuffer.STATUS_FROM_DEPID_TEMPLATE) == 0 ? false : true;
                fireEvent = (info & HTODInvalidationBuffer.STATUS_FIRE_EVENT) == 0 ? false : true;
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName, msg + " cacheName=" + this.cacheName + " *** ERROR *** id is not ExplicitIdData: " + id.getClass().toString());
                return result;
            }
        } else if (bufferType == HTODInvalidationBuffer.GC_BUFFER) {
            if (id instanceof EvictionTableEntry) {
                evt = (EvictionTableEntry) id;
                if (System.currentTimeMillis() > evt.expirationTime) {
                    result.cause = CachePerf.TIMEOUT;
                } else {
                    result.cause = CachePerf.DISK_GARBAGE_COLLECTOR;
                }
                result.source = CachePerf.LOCAL;
                fireEvent = true;
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName, msg + " cacheName=" + this.cacheName + " *** ERROR *** id is not EvictionTableEntry: " + id.getClass().toString());
                return result;
            }
        } else {
            fireEvent = true;
            result.cause = CachePerf.TIMEOUT;
            result.source = CachePerf.LOCAL;
        }

        // In this case, we need to convert the id to an EvictionTableEntry
        if (fireEvent && cache.isEnableListener() && cache.getEventSource().getPreInvalidationListenerCount() > 0
                && (bufferType != HTODInvalidationBuffer.EXPLICIT_BUFFER)) {
            // evt != null ==> bufferType is GC_BUFFER. We need to convert
            // EvictionTableEntry to cache id
            if (evt != null) {
                // try block used to get cache id from EvictionTableEntry
                try {
                    currentId = object_cache.getCacheKey(evt);
                    result.returnCode = NO_EXCEPTION;
                } catch (FileManagerException ex) {
                    this.diskCacheException = ex;
                    result.diskException = ex;
                    result.returnCode = DISK_EXCEPTION;
                } catch (HashtableOnDiskException ex) {
                    this.diskCacheException = ex;
                    result.diskException = ex;
                    result.returnCode = DISK_EXCEPTION;
                } catch (IOException ex) {
                    this.diskCacheException = ex;
                    result.diskException = ex;
                    result.returnCode = DISK_EXCEPTION;
                } catch (Exception ex) {
                    result.returnCode = OTHER_EXCEPTION;
                    result.diskException = ex;
                } finally {
                    if (result.returnCode == DISK_EXCEPTION || result.returnCode == OTHER_EXCEPTION) {
                        com.ibm.ws.ffdc.FFDCFilter.processException(result.diskException, "com.ibm.ws.cache.HTODDynacache.delCacheEntry", "655", this);
                    }
                    // handle the exception for getting cache id from
                    // EvictionTableEntry
                    if (result.returnCode != NO_EXCEPTION) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, methodName, msg + " cacheName=" + this.cacheName + "\n Exception: " + ExceptionUtility.getStackTrace(result.diskException));
                        evictionEntryPool.add(evt); // return evt to the pool
                        return result;
                    }
                }
            }

            if (cache.getEventSource().shouldInvalidate(currentId, result.source, result.cause) == false) {
                // insert EVT back to EvictionTable 
                if (bufferType == HTODInvalidationBuffer.GC_BUFFER) {
                    this.EvictionTable.insert(evt);
                } else {
                    // if running in high performance mode and entry is timeout, the id with 
                    // current time as expiration time will be put back into TimeLimitDaemon 
                    if (this.cod.diskCachePerformanceLevel == CacheConfig.HIGH && result.cause == CachePerf.TIMEOUT)
                        this.cod.cache.addToTimeLimitDaemon(currentId, System.currentTimeMillis(), -1);
                }
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName, msg + " cacheName=" + this.cacheName + " Skipping invalidate for id=" +
                                             currentId + " source=" + result.source + " cause=" + result.cause);
                return result;
            }
        }

        if (tc.isDebugEnabled())
            Tr.debug(tc, methodName, msg + " cacheName=" + this.cacheName + " id=" + id + " cause=" + result.cause + " source=" + result.source + " fromDTInv="
                                     + result.bFromDepIdTemplateInvalidation + "  fireEvent=" + fireEvent);
        boolean bRetrieveCacheValue = false;
        if (fireEvent && cache.isEnableListener() && cache.getEventSource().getInvalidationListenerCount() > 0 && this.cod.ignoreValueInInvalidationEvent == false) {
            bRetrieveCacheValue = true;
        }

        CacheEntry ce = null;
        if (id != null) {

            try {
                rwLock.writeLock().lock();
                if (this.invalidationBuffer.isLoopOnce()) {
                    if (evt != null) {
                        evictionEntryPool.add(evt); // return evt to the pool
                    }
                    return result;
                }
                //if (this.testException) {
                //    throw new FileManagerException("*** Generate filemanager exception");
                //}
                if (bufferType == HTODInvalidationBuffer.GC_BUFFER) {
                    hte = object_cache.getAndRemove(id, bRetrieveCacheValue);
                } else {
                    hte = object_cache.getAndRemove(currentId, bRetrieveCacheValue);
                }

                if (hte != null) {
                    //remove the entry from evictionTable
                    if ((bufferType != HTODInvalidationBuffer.GC_BUFFER) && (this.cod.evictionPolicy != CacheConfig.EVICTION_NONE)) {
                        //Find a matching entry in heap and remove it
                        long expTime = hte.expirationTime();
                        if (expTime <= 0)
                            expTime = Long.MAX_VALUE;
                        int hashcode = (hte.getKey()).hashCode();
                        if (this.invalidationBuffer.findAndRemoveFromGCBuffer(expTime, hashcode, hte.size()) == false) {
                            synchronized (evictionTableMonitor) {
                                EvictionTableEntry evt1 = EvictionTable.findAndRemove(expTime, hashcode, hte.size());
                                if (evt1 != null) {
                                    evictionEntryPool.add(evt1); // return evt to the pool
                                }
                            }
                        }
                    }
                    if (bufferType == HTODInvalidationBuffer.GC_BUFFER) {
                        result.data = hte.getKey();
                        currentId = result.data;
                    } else {
                        result.data = currentId;
                    }
                    result.numDelete = 1;
                    if (hte.size() > 0) {
                        result.deletedSize = hte.size();
                    } else {
                        result.deletedSize = hte.valueLength();
                    }
                    if (this.cod.enableCacheSizeInBytes) {
                        this.cod.currentCacheSizeInBytes = this.cod.currentCacheSizeInBytes - hte.size();
                    }
                    this.cache.getCacheStatisticsListener().deleteEntryFromDisk(currentId, hte.size());
                }
            } catch (FileManagerException ex) {
                this.diskCacheException = ex;
                result.diskException = ex;
                result.returnCode = DISK_EXCEPTION;
            } catch (HashtableOnDiskException ex) {
                this.diskCacheException = ex;
                result.diskException = ex;
                result.returnCode = DISK_EXCEPTION;
            } catch (IOException ex) {
                this.diskCacheException = ex;
                result.diskException = ex;
                result.returnCode = DISK_EXCEPTION;
            } catch (Exception ex) {
                result.returnCode = OTHER_EXCEPTION;
                result.diskException = ex;
            } finally {
                if (result.returnCode != NO_EXCEPTION) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, methodName, msg + " cacheName=" + this.cacheName + "\n Exception: " + ExceptionUtility.getStackTrace(result.diskException));
                }
                if (result.returnCode == DISK_EXCEPTION || result.returnCode == OTHER_EXCEPTION) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(result.diskException, "com.ibm.ws.cache.HTODDynacache.delCacheEntry", "700", this);
                }
                rwLock.writeLock().unlock();
            }

            if (hte != null) {
                Object value = hte.getValue();
                if (value != null) {
                    try {
                        ce = (CacheEntry) SerializationUtility.deserialize((byte[]) value, cacheName);
                    } catch (Exception ex) {
                        result.returnCode = SERIALIZATION_EXCEPTION;
                        result.diskException = ex;
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, methodName, msg + " cacheName=" + this.cacheName + "\n Exception: " + ExceptionUtility.getStackTrace(result.diskException));
                        com.ibm.ws.ffdc.FFDCFilter.processException(result.diskException, "com.ibm.ws.cache.HTODDynacache.delCacheEntry", "718", this);
                    }
                }
                if (ce != null) {
                    if (bufferType == HTODInvalidationBuffer.GC_BUFFER) {
                        this.invalidationBuffer.remove(ce.id);
                    }
                    synchronized (this.cache) {
                        if (ce._dataIds != null) {
                            //removedependencies for all dataIds to the cacheEntry
                            for (int i = 0; i < ce._dataIds.length; i++) {
                                //if (tc.isDebugEnabled()) Tr.debug(tc, methodName, msg + " cacheName=" + this.cacheName + " dataId=" + ce._dataIds[i]);
                                result.returnCode = delDependencyEntry(ce._dataIds[i], ce.id);
                                if (result.returnCode == DISK_EXCEPTION) {
                                    if (evt != null) {
                                        evictionEntryPool.add(evt); // return evt to the pool
                                    }
                                    return result;
                                }
                            }
                        }
                        if (ce._templates != null) {
                            //remove dependencies for all templates to the cacheEntry
                            for (int i = 0; i < ce._templates.length; i++) {
                                //if (tc.isDebugEnabled()) Tr.debug(tc, methodName, msg + " cacheName=" + this.cacheName + " template=" + ce._templates[i]);
                                result.returnCode = delTemplateEntry(ce._templates[i], ce.id);
                                if (result.returnCode == DISK_EXCEPTION) {
                                    if (evt != null) {
                                        evictionEntryPool.add(evt); // return evt to the pool
                                    }
                                    return result;
                                }
                            }
                        }
                        //remove dependencies for all aliases to the cacheEntry
                        for (int i = 0; i < ce.aliasList.length; i++) {

                            try {
                                rwLock.writeLock().lock();
                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, methodName, msg + " cacheName=" + this.cacheName + " deleting alias with id= " + ce.id + " alias=" + ce.aliasList[i]);
                                object_cache.remove(ce.aliasList[i]);
                                if (bufferType == HTODInvalidationBuffer.EXPLICIT_BUFFER) {
                                    this.invalidationBuffer.remove(ce.aliasList[i], HTODInvalidationBuffer.EXPLICIT_BUFFER);
                                }
                                result.numDelete++;
                            } catch (FileManagerException ex) {
                                this.diskCacheException = ex;
                                result.diskException = ex;
                                result.returnCode = DISK_EXCEPTION;
                            } catch (HashtableOnDiskException ex) {
                                this.diskCacheException = ex;
                                result.diskException = ex;
                                result.returnCode = DISK_EXCEPTION;
                            } catch (IOException ex) {
                                this.diskCacheException = ex;
                                result.diskException = ex;
                                result.returnCode = DISK_EXCEPTION;
                            } catch (Exception ex) {
                                result.returnCode = OTHER_EXCEPTION;
                                result.diskException = ex;
                            } finally {
                                if (result.returnCode != NO_EXCEPTION) {
                                    if (tc.isDebugEnabled())
                                        Tr.debug(tc, methodName, msg + " cacheName=" + this.cacheName + "\n Exception: " + ExceptionUtility.getStackTrace(result.diskException));
                                }
                                if (result.returnCode == DISK_EXCEPTION || result.returnCode == OTHER_EXCEPTION) {
                                    com.ibm.ws.ffdc.FFDCFilter.processException(result.diskException, "com.ibm.ws.cache.HTODDynacache.delCacheEntry", "700", this);
                                }
                                rwLock.writeLock().unlock();
                            }

                        }
                    }
                    if (ce.sharingPolicy == EntryInfo.SHARED_PUSH_PULL || ce.sharingPolicy == EntryInfo.SHARED_PULL) {
                        //if (tc.isDebugEnabled()) Tr.debug(tc, methodName, msg + " cacheName=" + this.cacheName + " call to invalidate PUSH-PULL table with id=" + ce.id);
                        if (bufferType == HTODInvalidationBuffer.EXPLICIT_BUFFER) {
                            if (result.source == CachePerf.LOCAL && result.bFromDepIdTemplateInvalidation) {
                                this.cache.invalidateById(ce.id, result.cause, result.source, false, !InvalidateByIdEvent.INVOKE_INTERNAL_INVALIDATE_BY_ID);
                            }
                        } else {
                            this.cache.invalidateById(ce.id, result.cause, result.source, false, !InvalidateByIdEvent.INVOKE_INTERNAL_INVALIDATE_BY_ID);
                        }
                    }
                    // fireEvent is set to false in the cache.lruToDisk for the error case.  
                    if (fireEvent && cache.isEnableListener() && cache.getEventSource().getInvalidationListenerCount() > 0) {
                        value = null;
                        // Value is valid if ignoreValueInInvalidationEvent is false. Otherwise, leave the value to NULL.
                        if (this.cod.ignoreValueInInvalidationEvent == false) {
                            value = hte.getSerializedCacheValue();
                            if (value == null) {
                                value = ce.serializedValue;
                            }
                        }
                        InvalidationEvent ie = null;
                        if (result.cause == CachePerf.DISK_GARBAGE_COLLECTOR) { //321649
                            ie = new InvalidationEvent(ce.id, value, InvalidationEvent.DISK_GARBAGE_COLLECTOR, InvalidationEvent.LOCAL, this.cache.getCacheName());
                        } else {
                            ie = new InvalidationEvent(ce.id, value, InvalidationEvent.DISK_TIMEOUT, InvalidationEvent.LOCAL, this.cache.getCacheName());
                        }
                        cache.getEventSource().fireEvent(ie);
                    }
                    returnToHashtableEntryPool(hte);
                    CachePerf cachePerf = cache.getCachePerf();
                    if (cachePerf != null && cachePerf.isPMIEnabled()) {
                        String template = "";
                        if (ce._templates != null && ce._templates.length > 0) {
                            template = ce._templates[0];
                        }
                        cachePerf.onInvalidate(template, result.cause, CachePerf.DISK, result.source);
                    }
                    this.cache.getCacheStatisticsListener().remove(ce.id, result.cause, CachePerf.DISK, result.source);
                    ce.reset();
                }
            }
        }
        if (tc.isDebugEnabled())
            Tr.debug(tc, methodName, msg + " cacheName=" + this.cacheName + " id=" + currentId + " cacheSizeInBytes=" + this.cod.currentCacheSizeInBytes + " deleteSize="
                                     + result.deletedSize + " bufferType=" + bufferType + " enable=" + this.cod.enableCacheSizeInBytes);
        if (evt != null) {
            evictionEntryPool.add(evt); // return evt to the pool
        }
        return result;
    }

    /*************************************************************************
     * delDependency()
     * This removes the specified dependency Id and its ValueSet (with all of its elements)
     * from the disk.
     *************************************************************************/
    public int delDependency(Object id) {
        int returnCode = NO_EXCEPTION;
        if (!this.disableDependencyId) {
            if (delayOffload) {
                auxDataDependencyTable.removeDependency(id);
            }
            returnCode = delValueSet(DEP_ID_DATA, id);
        }
        return returnCode;
    }

    /*************************************************************************
     * delTemplate()
     * This removes the specified template and its ValueSet (with all of its elements)
     * from the disk.
     *************************************************************************/
    public int delTemplate(String template) {
        int returnCode = NO_EXCEPTION;
        if (!this.disableTemplatesSupport) {
            if (delayOffload) {
                auxTemplateDependencyTable.removeDependency(template);
            }
            returnCode = delValueSet(TEMPLATE_ID_DATA, template);
        }
        return returnCode;
    }

    /*************************************************************************
     * delDependencyEntry()
     * This removes the specified entry for the specified dependency Id from the disk.
     *************************************************************************/
    public int delDependencyEntry(Object id,
                                  Object entry) {
        int returnCode = NO_EXCEPTION;
        if (!this.disableDependencyId) {
            if (delayOffload) {
                Result result = auxDataDependencyTable.removeEntry(id, entry);
                returnCode = result.returnCode;
                if (!result.bExist && returnCode != DISK_EXCEPTION) {
                    returnCode = delValueSetEntry(DEP_ID_DATA, id, entry);
                }
            } else {
                returnCode = delValueSetEntry(DEP_ID_DATA, id, entry);
            }
        }
        return returnCode;
    }

    /*************************************************************************
     * delTemplateEntry()
     * This removes the specified entry for the specified template from the disk.
     *************************************************************************/
    public int delTemplateEntry(String template,
                                Object entry) {
        int returnCode = NO_EXCEPTION;
        if (!this.disableTemplatesSupport) {
            if (delayOffload) {
                Result result = auxTemplateDependencyTable.removeEntry(template, entry);
                returnCode = result.returnCode;
                if (!result.bExist && returnCode != DISK_EXCEPTION) {
                    returnCode = delValueSetEntry(TEMPLATE_ID_DATA, template, entry);
                }
            } else {
                returnCode = delValueSetEntry(TEMPLATE_ID_DATA, template, entry);
            }
        }
        return returnCode;
    }

    /*************************************************************************
     * readCacheEntry()
     * This reads the cacheEntry entry from the specified Id from the disk.
     *************************************************************************/
    public Result readCacheEntry(Object id) {
        return readCacheEntry(id, !CALLED_FROM_REMOVE);
    }

    /*************************************************************************
     * readCacheEntry()
     * This reads the cacheEntry entry from the specified Id from the disk.
     *************************************************************************/
    public Result readCacheEntry(Object id, boolean calledFromRemove) {
        final String methodName = "readCacheEntry()";
        Result result = getFromResultPool();
        CacheEntry answer = null;
        if (this.invalidationBuffer.contains(id)) {
            return result;
        }
        HashtableEntry htEntry = null;
        boolean checkExpired = true;
        if (cache.isEnableListener() && cache.getEventSource().getInvalidationListenerCount() > 0 && this.cod.ignoreValueInInvalidationEvent == false) {
            checkExpired = false;
        }
        Object obj = null;
        try {
            rwLock.readLock().lock();
            //if (this.testException) {
            //    throw new FileManagerException("*** Generate filemanager exception");
            //}
            htEntry = object_cache.getHashTableEntry(id, checkExpired); // retrieve from HTOD
        } catch (FileManagerException ex) {
            this.diskCacheException = ex;
            result.diskException = ex;
            result.returnCode = DISK_EXCEPTION;
        } catch (HashtableOnDiskException ex) {
            this.diskCacheException = ex;
            result.diskException = ex;
            result.returnCode = DISK_EXCEPTION;
        } catch (IOException ex) {
            this.diskCacheException = ex;
            result.diskException = ex;
            result.returnCode = DISK_EXCEPTION;
        } catch (Exception ex) {
            result.returnCode = OTHER_EXCEPTION;
            result.diskException = ex;
        } finally {
            rwLock.readLock().unlock();
            if (result.returnCode != NO_EXCEPTION) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName, "cacheName=" + this.cacheName + "\n Exception: " + ExceptionUtility.getStackTrace(result.diskException));
            }
            if (result.returnCode == DISK_EXCEPTION || result.returnCode == OTHER_EXCEPTION) {
                com.ibm.ws.ffdc.FFDCFilter.processException(result.diskException, "com.ibm.ws.cache.HTODDynacache.readCacheEntry", "916", this);
                return result;
            }
        }

        boolean expired = false;
        boolean alias = false;
        int dataSize = 0;
        Object origId = id;
        if (htEntry != null) {
            if (htEntry.isAliasId()) {
                alias = true;
                obj = htEntry.getValue();
                origId = obj;
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName, "cacheName=" + this.cacheName + " found alias=" + id + " cacheId=" + origId);
                returnToHashtableEntryPool(htEntry);
                try {
                    rwLock.readLock().lock();
                    if (this.invalidationBuffer.contains(origId)) {
                        return result;
                    }
                    htEntry = object_cache.getHashTableEntry(origId, checkExpired); // retrieve from HTOD
                } catch (FileManagerException ex) {
                    this.diskCacheException = ex;
                    result.diskException = ex;
                    result.returnCode = DISK_EXCEPTION;
                } catch (HashtableOnDiskException ex) {
                    this.diskCacheException = ex;
                    result.diskException = ex;
                    result.returnCode = DISK_EXCEPTION;
                } catch (IOException ex) {
                    this.diskCacheException = ex;
                    result.diskException = ex;
                    result.returnCode = DISK_EXCEPTION;
                } catch (Exception ex) {
                    result.returnCode = OTHER_EXCEPTION;
                    result.diskException = ex;
                } finally {
                    rwLock.readLock().unlock();
                    if (result.returnCode != NO_EXCEPTION) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, methodName, "cacheName=" + this.cacheName + "\n Exception: " + ExceptionUtility.getStackTrace(result.diskException));
                    }
                    if (result.returnCode == DISK_EXCEPTION || result.returnCode == OTHER_EXCEPTION) {
                        com.ibm.ws.ffdc.FFDCFilter.processException(result.diskException, "com.ibm.ws.cache.HTODDynacache.readCacheEntry", "916", this);
                        return result;
                    }
                }
            }
            if (htEntry != null) {
                if (htEntry.isExpired()) {
                    expired = true;
                    if (checkExpired) {
                        if (cache.isEnableListener() && cache.getEventSource().getInvalidationListenerCount() > 0) {
                            // In here - ignoreValueInInvalidationEvent = true
                            InvalidationEvent ie = new InvalidationEvent(origId, null, InvalidationEvent.DISK_TIMEOUT, InvalidationEvent.LOCAL, this.cache.getCacheName());
                            if (!this.invalidationBuffer.contains(origId)) {
                                this.cache.getEventSource().fireEvent(ie);
                            }
                        }
                        this.invalidationBuffer.add(origId, HTODInvalidationBuffer.EXPLICIT_BUFFER, CachePerf.TIMEOUT, CachePerf.LOCAL, !Cache.FROM_DEPID_TEMPLATE_INVALIDATION,
                                                    !HTODInvalidationBuffer.FIRE_EVENT, !HTODInvalidationBuffer.ALIAS_ID);
                        if (alias) {
                            this.invalidationBuffer.add(id, HTODInvalidationBuffer.EXPLICIT_BUFFER, CachePerf.TIMEOUT, CachePerf.LOCAL, !Cache.FROM_DEPID_TEMPLATE_INVALIDATION,
                                                        !HTODInvalidationBuffer.FIRE_EVENT, HTODInvalidationBuffer.ALIAS_ID);
                        }
                        if (this.cod.diskCachePerformanceLevel != CacheConfig.HIGH && !calledFromRemove) {
                            this.cache.internalInvalidateByDepId(origId, CachePerf.TIMEOUT, CachePerf.LOCAL, Cache.FIRE_INVALIDATION_LISTENER);
                        }
                        returnToHashtableEntryPool(htEntry);
                        return result;
                    }
                }
                // Retrieve CE and put data back in CE
                obj = htEntry.getValue();
                dataSize = htEntry.size();
                if (obj != null) {
                    try {
                        answer = (CacheEntry) SerializationUtility.deserialize((byte[]) obj, cacheName);
                        answer.setValidatorExpirationTime(htEntry.validatorExpirationTime());
                        byte[] serializedCacheValue = htEntry.getSerializedCacheValue();
                        if (serializedCacheValue != null) {
                            answer.serializedValue = serializedCacheValue;
                        }
                    } catch (Exception ex) {
                        result.returnCode = SERIALIZATION_EXCEPTION;
                        result.diskException = ex;
                        com.ibm.ws.ffdc.FFDCFilter.processException(result.diskException, "com.ibm.ws.cache.HTODDynacache.readCacheEntry", "953", this);
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, methodName, "cacheName=" + this.cacheName + "\n Exception: " + ExceptionUtility.getStackTrace(result.diskException));
                    } finally {
                        returnToHashtableEntryPool(htEntry);
                    }
                }
            }
        }
        if (answer != null) {
            if (expired) {
                if (this.cache.isEnableListener() && cache.getEventSource().getInvalidationListenerCount() > 0) {
                    // In here - ignoreValueInInvalidationEvent = false
                    InvalidationEvent ie = new InvalidationEvent(answer.id, answer.serializedValue, InvalidationEvent.DISK_TIMEOUT, InvalidationEvent.LOCAL, this.cache.getCacheName());
                    if (!this.invalidationBuffer.contains(answer.id)) {
                        this.cache.getEventSource().fireEvent(ie);
                    }
                }
                this.invalidationBuffer.add(answer.id, HTODInvalidationBuffer.EXPLICIT_BUFFER, CachePerf.TIMEOUT, CachePerf.LOCAL, !Cache.FROM_DEPID_TEMPLATE_INVALIDATION,
                                            !HTODInvalidationBuffer.FIRE_EVENT, !HTODInvalidationBuffer.ALIAS_ID);
                for (int i = 0; i < answer.aliasList.length; i++) {
                    this.invalidationBuffer.add(answer.aliasList[i], HTODInvalidationBuffer.EXPLICIT_BUFFER, CachePerf.TIMEOUT, CachePerf.LOCAL,
                                                !Cache.FROM_DEPID_TEMPLATE_INVALIDATION, !HTODInvalidationBuffer.FIRE_EVENT, HTODInvalidationBuffer.ALIAS_ID);;
                }
                if (this.cod.diskCachePerformanceLevel != CacheConfig.HIGH && !calledFromRemove) {
                    //if (tc.isDebugEnabled()) Tr.debug(tc, methodName, "cacheName=" + this.cacheName + " id=" + answer.id + " check id as dep id");
                    this.cache.internalInvalidateByDepId(answer.id, CachePerf.TIMEOUT, CachePerf.LOCAL, Cache.FIRE_INVALIDATION_LISTENER);
                }
                answer = null;
            } else {
                if (!calledFromRemove && answer.serializedValue != null) {
                    this.cache.getCacheStatisticsListener().readEntryFromDisk(answer.id, dataSize);
                }
            }
        }
        result.data = answer;
        if (tc.isDebugEnabled())
            Tr.debug(tc, methodName, "cacheName=" + this.cacheName + " id=" + id + " cacheSizeInBytes=" + this.cod.currentCacheSizeInBytes);
        return result;
    }

    /*************************************************************************
     * readDependency()
     * This returns the valueSet containing all entries for the sepecified dependency id.
     *************************************************************************/
    public Result readDependency(Object id,
                                 boolean delete) {
        Result result = getFromResultPool();
        ValueSet vs = null;
        if (id == null) {
            return result;
        }
        if (!this.disableDependencyId) {
            if (delayOffload) {
                vs = auxDataDependencyTable.getEntries(id);
                if (vs == null) {
                    Result other = readValueSet(DEP_ID_DATA, id, delete);
                    result.copy(other);
                    returnToResultPool(other);
                    if (result.returnCode != DISK_EXCEPTION) {
                        vs = (ValueSet) result.data;
                        if (vs != null) {
                            if (delete == !DELETE && vs.size() <= auxDataDependencyTable.delayOffloadEntriesLimit) {
                                result.returnCode = auxDataDependencyTable.add(id, vs);
                            }
                        }
                    }
                } else {
                    if (delete == DELETE) {
                        auxDataDependencyTable.removeDependency(id);
                        result.returnCode = delValueSet(DEP_ID_DATA, id);
                    }
                }
            } else {
                Result other = readValueSet(DEP_ID_DATA, id, delete);
                result.copy(other);
                returnToResultPool(other);
                vs = (ValueSet) result.data;
            }
            if (result.returnCode != DISK_EXCEPTION && vs != null) {
                this.invalidationBuffer.filter(vs);
            }

            result.data = vs;
        }
        return result;
    }

    /*************************************************************************
     * readTemplate()
     * This returns the valueSet containing all entries for the sepecified template.
     *************************************************************************/
    public Result readTemplate(String template,
                               boolean delete) {
        Result result = getFromResultPool();
        ValueSet vs = null;
        if (template == null) {
            return result;
        }
        if (!this.disableTemplatesSupport) {
            if (delayOffload) {
                vs = auxTemplateDependencyTable.getEntries(template);
                if (vs == null) {
                    Result other = readValueSet(TEMPLATE_ID_DATA, template, delete);
                    result.copy(other);
                    returnToResultPool(other);
                    if (result.returnCode != DISK_EXCEPTION) {
                        vs = (ValueSet) result.data;
                        if (vs != null) {
                            if (delete == !DELETE && vs.size() <= auxTemplateDependencyTable.delayOffloadEntriesLimit) {
                                result.returnCode = auxTemplateDependencyTable.add(template, vs);
                            }
                        }
                    }
                } else {
                    if (delete == DELETE) {
                        auxTemplateDependencyTable.removeDependency(template);
                        result.returnCode = delValueSet(TEMPLATE_ID_DATA, template);
                    }
                }
            } else {
                Result other = readValueSet(TEMPLATE_ID_DATA, template, delete);
                result.copy(other);
                returnToResultPool(other);
                vs = (ValueSet) result.data;
            }
            if (result.returnCode != DISK_EXCEPTION && vs != null) {
                this.invalidationBuffer.filter(vs);
            }
            result.data = vs;
        }
        return result;
    }

    /*************************************************************************
     * readCacheIdsByRange()
     * This method is used by CacheMonitor to retrive the cache ids from the disk.
     * If index = 0, it starts the beginning. If index = 1, it means "next". If Index = -1, it means "previous".
     * The length of the max number of templates to be read. If length = -1, it reads all templates until the end.
     *************************************************************************/
    public Result readCacheIdsByRange(int index, int length) {
        Result result = readByRange(CACHE_ID_DATA, index, length, CHECK_EXPIRED, FILTER);
        return result;
    }

    /*************************************************************************
     * readDependencyByRange()
     * This method is used by CacheMonitor to retrive the dependency ids from the disk.
     * If index = 0, it starts the beginning. If index = 1, it means "next". If Index = -1, it means "previous".
     * The length of the max number of templates to be read. If length = -1, it reads all templates until the end.
     *************************************************************************/
    public Result readDependencyByRange(int index, int length) {
        Result result = getFromResultPool();
        if (!this.disableDependencyId) {
            Result other = readByRange(DEP_ID_DATA, index, length, !CHECK_EXPIRED, !FILTER);
            result.copy(other);
            returnToResultPool(other);
        }
        return result;
    }

    /*************************************************************************
     * readTemplatesByRange()
     * This method is used by CacheMonitor to retrive the templates from the disk.
     * If index = 0, it starts the beginning. If index = 1, it means "next". If Index = -1, it means "previous".
     * The length of the max number of templates to be read. If length = -1, it reads all templates until the end.
     *************************************************************************/
    public Result readTemplatesByRange(int index, int length) {
        Result result = getFromResultPool();
        if (!this.disableDependencyId) {
            Result other = readByRange(TEMPLATE_ID_DATA, index, length, !CHECK_EXPIRED, !FILTER);
            result.copy(other);
            returnToResultPool(other);
        }
        return result;
    }

    /*************************************************************************
     * getCacheIdsSize()
     * This method is used by CacheMonitor to get the number of cache Ids from the disk.
     *************************************************************************/
    public int getCacheIdsSize(boolean filter) {
        return getCacheIdsSize();
    }

    public int getCacheIdsSize() {

        try {
            rwLock.readLock().lock();
            int length = object_cache.size();
            if (length < 0) {
                return 0;
            }
            return length;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /*************************************************************************
     * getDepIdsSize()
     * This method is used by CacheMonitor to get the number of dependency Ids from the disk.
     *************************************************************************/
    public int getDepIdsSize() {
        int length = 0;
        if (!this.disableDependencyId) {
            try {
                rwLock.readLock().lock();
                length = dependency_cache.size();
            } finally {
                rwLock.readLock().unlock();
            }
            if (length < 0) {
                length = 0;
            }
        }
        return length;
    }

    /*************************************************************************
     * getTemplatesSize()
     * This method is used by CacheMonitor to get the number of templates from the disk.
     *************************************************************************/
    public int getTemplatesSize() {
        int length = 0;
        if (!this.disableTemplatesSupport) {
            try {
                rwLock.readLock().lock();
                length = template_cache.size();
            } finally {
                rwLock.readLock().unlock();
            }
            if (length < 0) {
                length = 0;
            }
        }
        return length;
    }

    /*************************************************************************
     * writeCacheEntry()
     * This creates or updates a cacheEntry in the disk.
     *************************************************************************/
    public int writeCacheEntry(CacheEntry ce) {

        final String methodName = "writeCacheEntry()";
        int returnCode = NO_EXCEPTION;
        Exception diskException = null;
        this.invalidationBuffer.remove(ce.id);
        byte[] serializedData = null; // retrieve byte array directly (avoid copy)
        int serializedDataLength = -1;
        try {
            ce.skipValueSerialized = true;
            serializedData = SerializationUtility.serialize(ce);
            serializedDataLength = serializedData.length;
        } catch (Exception ex) {
            returnCode = SERIALIZATION_EXCEPTION;
            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.HTODDynacache.writeCacheEntry", "1174", this);
            if (tc.isDebugEnabled())
                Tr.debug(tc, methodName, "cacheName=" + this.cacheName + "\nException: " + ExceptionUtility.getStackTrace(ex));
            return returnCode;
        } finally {
            ce.skipValueSerialized = false;
        }

        try {
            rwLock.writeLock().lock();
            int dataSize = HashtableOnDisk.HTENTRY_OVERHEAD_SIZE + ce.serializedId.length + serializedDataLength + ce.serializedValue.length;
            if (dataSize % 512 != 0) { // size adjustment for 512 blocks
                dataSize = (dataSize / 512 + 1) * 512;
            }
            //if (this.testException) {
            //    throw new FileManagerException("*** Generate filemanager exception");
            //}
            int oldEntrySize = 0; // initialize the oldEntry size to 0
            HashtableEntry htEntry = null;
            if (ce.valueHashcode == 0 && null != ce.value) {
                ce.valueHashcode = ce.value.hashCode();
            }
            if (ce.timeLimit > 0) {
                htEntry = object_cache.put(ce.id, serializedData, serializedDataLength, ce.expirationTime, ce.validatorExpirationTime, ce.serializedId, ce.serializedValue,
                                           ce.valueHashcode, !HashtableOnDisk.ALIAS_ID); // LI4337-17  // put into HTOD
            } else {
                htEntry = object_cache.put(ce.id, serializedData, serializedDataLength, -1, ce.validatorExpirationTime, ce.serializedId, ce.serializedValue, ce.valueHashcode,
                                           !HashtableOnDisk.ALIAS_ID); // LI4337-17  // put into HTOD
            }
            //delete old entry from eviction table or insert into eviction table only if eviction policy is set
            if (this.cod.evictionPolicy != CacheConfig.EVICTION_NONE) {
                boolean bInsertEntry = true;
                if (htEntry != null) {
                    // check whether EvictionTable's attributes match or not
                    if ((ce.getId()).hashCode() == (htEntry.getKey()).hashCode() &&
                        ce.expirationTime == htEntry.expirationTime() &&
                        dataSize == htEntry.size()) {
                        // old entry exists and eviction's attributes match
                        // skip inserting the entry into EvictionTable because it already exists in EvictionTable
                        bInsertEntry = false;
                    } else {
                        // old entry exists but eviction's attributes do not match 
                        // then delete the entry from EvictionTable
                        synchronized (evictionTableMonitor) {
                            long expTime = htEntry.expirationTime();
                            if (expTime <= 0)
                                expTime = Long.MAX_VALUE;
                            EvictionTableEntry evt = EvictionTable.findAndRemove(expTime, (htEntry.getKey()).hashCode(), htEntry.size());
                            if (evt != null) {
                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, "writeCacheEntry()", " **deleting** id=" + ce.id + " hc=" + evt.hashcode + " expirationTime=" + evt.expirationTime + " size="
                                                                      + evt.size);
                                evictionEntryPool.add(evt); // return evt to the pool
                            }
                        }
                    }
                }
                // if bInsertEntry boolean is true, insert the entry into EvictionTable. Else skip inserting.
                if (bInsertEntry == true) {
                    EvictionTableEntry evt = (EvictionTableEntry) evictionEntryPool.remove();
                    evt.hashcode = (ce.getId()).hashCode();
                    evt.expirationTime = ce.expirationTime;
                    if (evt.expirationTime <= 0)
                        evt.expirationTime = Long.MAX_VALUE;
                    evt.size = dataSize;
                    evt.id = ce.id; // use it for debug
                    synchronized (evictionTableMonitor) {
                        EvictionTable.insert(evt);
                    }
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "WriteCacheEntry()", "**adding** id=" + ce.id + " hc=" + evt.hashcode + " expirationTime=" + evt.expirationTime + " size=" + evt.size);
                }
            }
            // if htEntry is not null, it means the entry is overwritten
            if (htEntry != null) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName, "cacheName=" + this.cacheName + " overwritten with id=" + ce.id);
                returnCode = NO_EXCEPTION_ENTRY_OVERWRITTEN;
                oldEntrySize = htEntry.size();
                returnToHashtableEntryPool(htEntry);
            }
            if (this.cod.enableCacheSizeInBytes) {
                this.cod.currentCacheSizeInBytes = this.cod.currentCacheSizeInBytes + dataSize - oldEntrySize;
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "WriteCacheEntry()", "**** id=" + ce.id + " size=" + dataSize + " currentCacheSizeInBytes" + this.cod.currentCacheSizeInBytes + " overhead="
                                                      + HashtableOnDisk.HTENTRY_OVERHEAD_SIZE + " idSize=" + ce.serializedId.length + " ceSize=" + serializedDataLength
                                                      + " valueSize=" + ce.serializedValue.length);
            }
            if (tc.isDebugEnabled())
                Tr.debug(tc, methodName, "cacheName=" + this.cacheName + " cacheSizeInBytes" + this.cod.currentCacheSizeInBytes + " newSize=" + dataSize + " oldSize="
                                         + oldEntrySize + " id=" + ce.id + " enable=" + this.cod.enableCacheSizeInBytes);
            this.cache.getCacheStatisticsListener().writeEntryToDisk(ce.id, dataSize);
            for (int i = 0; i < ce.aliasList.length; i++) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName, "cacheName=" + this.cacheName + " adding alias with id=" + ce.id + " alias=" + ce.aliasList[i]);
                // alias id has no expiration time
                object_cache.put(ce.aliasList[i], ce.getIdObject(), -1, -1, -1, null, null, 0, HashtableOnDisk.ALIAS_ID);
                this.invalidationBuffer.remove(ce.aliasList[i], HTODInvalidationBuffer.EXPLICIT_BUFFER);
            }
        } catch (FileManagerException ex) {
            this.diskCacheException = ex;
            diskException = ex;
            returnCode = DISK_EXCEPTION;
        } catch (HashtableOnDiskException ex) {
            this.diskCacheException = ex;
            diskException = ex;
            returnCode = DISK_EXCEPTION;
        } catch (IOException ex) {
            diskException = ex;
            if (ex.getMessage().equals(DISK_CACHE_IN_GB_OVER_LIMIT_MSG)) {
                returnCode = DISK_SIZE_OVER_LIMIT_EXCEPTION;
            } else {
                this.diskCacheException = ex;
                returnCode = DISK_EXCEPTION;
            }
        } catch (Exception ex) {
            returnCode = OTHER_EXCEPTION;
            diskException = ex;
        } finally {
            rwLock.writeLock().unlock();
            if (returnCode != NO_EXCEPTION && returnCode != NO_EXCEPTION_ENTRY_OVERWRITTEN) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName, "cacheName=" + this.cacheName + "\n Exception: " + ExceptionUtility.getStackTrace(diskException));
            }
            if (returnCode == DISK_EXCEPTION || returnCode == OTHER_EXCEPTION) {
                com.ibm.ws.ffdc.FFDCFilter.processException(diskException, "com.ibm.ws.cache.HTODDynacache.writeCacheEntry", "1239", this);
            }
        }

        return returnCode;
    }

    // This method is used to update validator expiration time in disk emtry header. 
    // Since the real expiration time does not change, the GC will be updated. 
    public int updateExpirationTime(Object id, long oldExpirationTime, int size, long newExpirationTime, long newValidatorExpirationTime) {
        final String methodName = "updateExpirationTime()";
        int returnCode = NO_EXCEPTION;
        Exception diskException = null;

        // update the disk entry header only with new validator expiration time for now.
        if (tc.isDebugEnabled())
            Tr.debug(tc, methodName, "cacheName=" + this.cacheName + " id=" + id + " newValidatorExpirationTime=" + newValidatorExpirationTime);

        try {
            rwLock.writeLock().lock();
            object_cache.updateExpirationInHeader(id, newExpirationTime, newValidatorExpirationTime);
        } catch (FileManagerException ex) {
            this.diskCacheException = ex;
            diskException = ex;
            returnCode = DISK_EXCEPTION;
        } catch (HashtableOnDiskException ex) {
            this.diskCacheException = ex;
            diskException = ex;
            returnCode = DISK_EXCEPTION;
        } catch (IOException ex) {
            diskException = ex;
            if (ex.getMessage().equals(DISK_CACHE_IN_GB_OVER_LIMIT_MSG)) {
                returnCode = DISK_SIZE_OVER_LIMIT_EXCEPTION;
            } else {
                this.diskCacheException = ex;
                returnCode = DISK_EXCEPTION;
            }
        } catch (Exception ex) {
            returnCode = OTHER_EXCEPTION;
            diskException = ex;
        } finally {
            rwLock.writeLock().unlock();
            if (returnCode != NO_EXCEPTION && returnCode != NO_EXCEPTION_ENTRY_OVERWRITTEN) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName, "cacheName=" + this.cacheName + "\n Exception: " + ExceptionUtility.getStackTrace(diskException));
            }
            if (returnCode == DISK_EXCEPTION || returnCode == OTHER_EXCEPTION) {
                com.ibm.ws.ffdc.FFDCFilter.processException(diskException, "com.ibm.ws.cache.HTODDynacache.writeCacheEntry", "1239", this);
            }
        }

        return returnCode;
    }

    /*************************************************************************
     * writeDependency()
     * This creates or updates specified dependency Ids with new set of entries.
     *************************************************************************/
    public int writeDependency(Object id,
                               ValueSet valueSet) {
        final String methodName = "writeDependency()";
        int rc = NO_EXCEPTION;
        if (valueSet.size() == 0) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, methodName, "cacheName=" + this.cacheName + " valueSet=0");
        }
        if (!this.disableDependencyId) {
            if (delayOffload) {
                ValueSet vs = auxDataDependencyTable.getEntries(id);
                if (vs == null) {
                    if (valueSet.size() <= auxDataDependencyTable.delayOffloadEntriesLimit) {
                        Result result = writeValueSetEntry(DEP_ID_DATA, id, null, valueSet, !ADD_IF_NEW);
                        rc = result.returnCode;
                        boolean bComplete = result.bComplete;
                        returnToResultPool(result);
                        if (rc != DISK_EXCEPTION && !bComplete) {
                            rc = auxDataDependencyTable.add(id, valueSet);
                        }
                    } else {
                        rc = writeValueSet(DEP_ID_DATA, id, valueSet, !ALL); // valueSet may be empty after writeValueSet
                        this.cache.getCacheStatisticsListener().depIdsOffloadedToDisk(id);
                    }
                } else {
                    vs.union(valueSet);
                    if (vs.size() > auxDataDependencyTable.delayOffloadEntriesLimit) {
                        auxDataDependencyTable.removeDependency(id);
                        rc = writeValueSet(DEP_ID_DATA, id, vs, ALL); // valueSet may be empty after writeValueSet
                        if (rc != NO_EXCEPTION) {
                            valueSet.clear();
                            valueSet.addAll(vs);
                        }
                        this.cache.getCacheStatisticsListener().depIdsOffloadedToDisk(id);
                    }
                }
            } else {
                rc = writeValueSet(DEP_ID_DATA, id, valueSet, !ALL); // valueSet may be empty after writeValueSet
            }
        }
        return rc;
    }

    /*************************************************************************
     * writeDependencyEntry()
     * This adds a new entry for the specified dependency Ids.
     *************************************************************************/
    public int writeDependencyEntry(Object id,
                                    Object entry) {
        int rc = NO_EXCEPTION;
        if (!this.disableDependencyId) {
            if (delayOffload) {
                ValueSet vs = auxDataDependencyTable.getEntries(id);
                if (vs == null) {
                    Result result = writeValueSetEntry(DEP_ID_DATA, id, entry, null, !ADD_IF_NEW);
                    rc = result.returnCode;
                    boolean bExist = result.bExist;
                    returnToResultPool(result);
                    if (rc != DISK_EXCEPTION && rc != DISK_SIZE_OVER_LIMIT_EXCEPTION && bExist == !EXIST) {
                        rc = auxDataDependencyTable.add(id, entry);
                    }
                } else {
                    rc = auxDataDependencyTable.add(id, vs, entry);
                }
            } else {
                Result result = writeValueSetEntry(DEP_ID_DATA, id, entry, null, ADD_IF_NEW);
                rc = result.returnCode;
                returnToResultPool(result);
            }
        }
        return rc;
    }

    /*************************************************************************
     * writeTemplate()
     * This creates or updates specified template with new set of entries.
     *************************************************************************/
    public int writeTemplate(String template,
                             ValueSet valueSet) {
        final String methodName = "writeTemplate()";
        int rc = NO_EXCEPTION;
        if (valueSet.size() == 0) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, methodName, "cacheName=" + this.cacheName + " valueSet=0");
        }
        if (!this.disableTemplatesSupport) {
            if (delayOffload) {
                ValueSet vs = auxTemplateDependencyTable.getEntries(template);
                if (vs == null) {
                    if (valueSet.size() <= auxTemplateDependencyTable.delayOffloadEntriesLimit) {
                        Result result = writeValueSetEntry(TEMPLATE_ID_DATA, template, null, valueSet, !ADD_IF_NEW);
                        rc = result.returnCode;
                        boolean bComplete = result.bComplete;
                        returnToResultPool(result);
                        if (rc != DISK_EXCEPTION && !bComplete) {
                            rc = auxTemplateDependencyTable.add(template, valueSet);
                        }
                    } else {
                        rc = writeValueSet(TEMPLATE_ID_DATA, template, valueSet, !ALL); // valueSet may be empty after writeValueSet
                        this.cache.getCacheStatisticsListener().templatesOffloadedToDisk(template);
                    }
                } else {
                    vs.union(valueSet);
                    if (vs.size() > auxTemplateDependencyTable.delayOffloadEntriesLimit) {
                        auxTemplateDependencyTable.removeDependency(template);
                        rc = writeValueSet(TEMPLATE_ID_DATA, template, vs, ALL); // valueSet may be empty after writeValueSet
                        if (rc != NO_EXCEPTION) {
                            valueSet.clear();
                            valueSet.addAll(vs);
                        }
                        this.cache.getCacheStatisticsListener().templatesOffloadedToDisk(template);
                    }
                }
            } else {
                rc = writeValueSet(TEMPLATE_ID_DATA, template, valueSet, !ALL); // valueSet may be empty after writeValueSet
            }
        }
        return rc;
    }

    /*************************************************************************
     * writeTemplateEntry()
     * This adds a new entry for the specified template.
     *************************************************************************/
    public int writeTemplateEntry(String template,
                                  Object entry) {
        int rc = NO_EXCEPTION;
        if (!this.disableTemplatesSupport) {
            if (delayOffload) {
                ValueSet vs = auxTemplateDependencyTable.getEntries(template);
                if (vs == null) {
                    Result result = writeValueSetEntry(TEMPLATE_ID_DATA, template, entry, null, !ADD_IF_NEW);
                    rc = result.returnCode;
                    boolean bExist = result.bExist;
                    returnToResultPool(result);
                    if (rc != DISK_EXCEPTION && rc != DISK_SIZE_OVER_LIMIT_EXCEPTION && bExist == !EXIST) {
                        rc = auxTemplateDependencyTable.add(template, entry);
                    }
                } else {
                    rc = auxTemplateDependencyTable.add(template, vs, entry);
                }
            } else {
                Result result = writeValueSetEntry(TEMPLATE_ID_DATA, template, entry, null, ADD_IF_NEW);
                rc = result.returnCode;
                returnToResultPool(result);
            }
        }
        return rc;
    }

    /*************************************************************************
     * readValueSet()
     * These are common for both dependencies and templates so we parameterize
     * the basic operations by HTOD instance.
     *************************************************************************/
    Result readValueSet(int type, Object id, boolean delete) {
        final String methodName = "readValueSet()";
        Result result = getFromResultPool();
        ValueSet answer = new ValueSet(4);
        HashtableOnDisk cache_instance = null;
        HashtableOnDisk vshtod = null;
        if (delete) {

            try {
                rwLock.writeLock().lock();
                //if (this.testException) {
                //    throw new FileManagerException("*** Generate filemanager exception");
                //}
                if (type == DEP_ID_DATA) {
                    cache_instance = dependency_cache;
                } else {
                    cache_instance = template_cache;
                }
                Long vsinstid = (Long) cache_instance.get(id);
                if (vsinstid != null) {
                    long vsinstance = vsinstid.longValue();
                    FileManager filemgr = cache_instance.getFileManager();
                    if (vsinstance != -1) {
                        vshtod = cache_instance.getInstance(filemgr, valueset_rehash, vsinstance, !HashtableOnDisk.HAS_CACHE_VALUE, this);
                        int length = vshtod.size();
                        if (length > 0) {
                            answer = new ValueSet(length);
                            ValueSetReadCallback vscr = new ValueSetReadCallback(answer, null);
                            vshtod.iterateKeys(vscr, 0, length);

                            vshtod.clear(); // remove (deallocate) everything in the htod
                            vshtod.close(); // close it
                            vshtod = null;
                        }
                        HashtableOnDisk.destroyInstance(filemgr, vsinstance); // remove instance (deallocate header on disk)
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "readValueSet()", " id=" + id + " length=" + length + " valueSetSize=" + answer.size());
                    }
                    cache_instance.remove(id); // remove ref to sub-htod from dep/template htod
                }
            } catch (FileManagerException ex) {
                this.diskCacheException = ex;
                result.diskException = ex;
                result.returnCode = DISK_EXCEPTION;
            } catch (HashtableOnDiskException ex) {
                this.diskCacheException = ex;
                result.diskException = ex;
                result.returnCode = DISK_EXCEPTION;
            } catch (IOException ex) {
                this.diskCacheException = ex;
                result.diskException = ex;
                result.returnCode = DISK_EXCEPTION;
            } catch (Exception ex) {
                result.returnCode = OTHER_EXCEPTION;
                result.diskException = ex;
            } finally {
                if (result.returnCode != NO_EXCEPTION) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, methodName, "cacheName=" + this.cacheName + "\n Exception: " + ExceptionUtility.getStackTrace(result.diskException));
                }
                if (result.returnCode == DISK_EXCEPTION || result.returnCode == OTHER_EXCEPTION) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(result.diskException, "com.ibm.ws.cache.HTODDynacache.readValueSet", "1432", this);
                }
                try {
                    if (vshtod != null) {
                        vshtod.close();
                    }
                } catch (Exception ex) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.HTODDynacache.readValueSet", "1439", this);
                }
                rwLock.writeLock().unlock();
            }

        } else {
            try {
                rwLock.readLock().lock();
                if (type == DEP_ID_DATA) {
                    cache_instance = dependency_cache;
                } else {
                    cache_instance = template_cache;
                }
                Long vsinstid = (Long) cache_instance.get(id);
                if (vsinstid != null) {
                    long vsinstance = vsinstid.longValue();
                    FileManager filemgr = cache_instance.getFileManager();
                    if (vsinstance != -1) {
                        vshtod = cache_instance.getInstance(filemgr, valueset_rehash, vsinstance, !HashtableOnDisk.HAS_CACHE_VALUE, this);
                        int length = vshtod.size();
                        if (length > 0) {
                            answer = new ValueSet(length);
                            ValueSetReadCallback vscr = new ValueSetReadCallback(answer, null);
                            vshtod.iterateKeys(vscr, 0, length);
                            vshtod.close();
                            vshtod = null;
                        }
                    }
                }
            } catch (FileManagerException ex) {
                this.diskCacheException = ex;
                result.diskException = ex;
                result.returnCode = DISK_EXCEPTION;
            } catch (HashtableOnDiskException ex) {
                this.diskCacheException = ex;
                result.diskException = ex;
                result.returnCode = DISK_EXCEPTION;
            } catch (IOException ex) {
                this.diskCacheException = ex;
                result.diskException = ex;
                result.returnCode = DISK_EXCEPTION;
            } catch (Exception ex) {
                result.returnCode = OTHER_EXCEPTION;
                result.diskException = ex;
            } finally {
                if (result.returnCode != NO_EXCEPTION) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, methodName, "cacheName=" + this.cacheName + "\n Exception: " + ExceptionUtility.getStackTrace(result.diskException));
                }
                if (result.returnCode == DISK_EXCEPTION || result.returnCode == OTHER_EXCEPTION) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(result.diskException, "com.ibm.ws.cache.HTODDynacache.readValueSet", "1488", this);
                }
                try {
                    if (vshtod != null) {
                        vshtod.close();
                    }
                } catch (Exception ex) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.HTODDynacache.readValueSet", "1495", this);
                }
                rwLock.readLock().unlock();
            }
        }
        result.data = answer;
        return result;
    }

    /*************************************************************************
     * readValueSetSize()
     * These are common for both dependencies and templates so we parameterize
     * the basic operations by HTOD instance.
     *************************************************************************/
    Result readValueSetSize(int type, Object id) {
        final String methodName = "readValueSetSize()";
        Result result = getFromResultPool();
        ValueSet answer = new ValueSet(4);
        HashtableOnDisk cache_instance = null;
        HashtableOnDisk vshtod = null;
        try {
            rwLock.readLock().lock();
            if (type == DEP_ID_DATA) {
                cache_instance = dependency_cache;
            } else {
                cache_instance = template_cache;
            }
            Long vsinstid = (Long) cache_instance.get(id);
            if (vsinstid != null) {
                long vsinstance = vsinstid.longValue();
                FileManager filemgr = cache_instance.getFileManager();
                if (vsinstance != -1) {
                    vshtod = cache_instance.getInstance(filemgr, valueset_rehash, vsinstance, !HashtableOnDisk.HAS_CACHE_VALUE, this);
                    result.dataSize = vshtod.size();
                    vshtod.close();
                    vshtod = null;
                } else {
                    result.dataSize = 0;
                }
            }
        } catch (FileManagerException ex) {
            this.diskCacheException = ex;
            result.diskException = ex;
            result.returnCode = DISK_EXCEPTION;
        } catch (HashtableOnDiskException ex) {
            this.diskCacheException = ex;
            result.diskException = ex;
            result.returnCode = DISK_EXCEPTION;
        } catch (IOException ex) {
            this.diskCacheException = ex;
            result.diskException = ex;
            result.returnCode = DISK_EXCEPTION;
        } catch (Exception ex) {
            result.returnCode = OTHER_EXCEPTION;
            result.diskException = ex;
        } finally {
            if (result.returnCode != NO_EXCEPTION) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName, "cacheName=" + this.cacheName + "\n Exception: " + ExceptionUtility.getStackTrace(result.diskException));
            }
            if (result.returnCode == DISK_EXCEPTION || result.returnCode == OTHER_EXCEPTION) {
                com.ibm.ws.ffdc.FFDCFilter.processException(result.diskException, "com.ibm.ws.cache.HTODDynacache.readValueSetSize", "1621", this);
            }
            try {
                if (vshtod != null) {
                    vshtod.close();
                }
            } catch (Exception ex) {
                com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.HTODDynacache.readValueSetSize", "1628", this);
            }
            rwLock.readLock().unlock();
        }
        return result;
    }

    /*************************************************************************
     * readByRange()
     * This method is used by CacheMonitor to retrive cache ids for Cache entry,
     * dependency id, and template.
     *************************************************************************/
    private Result readByRange(int type, int index, int length, boolean checkExpired, boolean filter) {
        final String methodName = "readByRange()";
        Result result = getFromResultPool();
        ValueSet answer = null;
        ValueSet expiredIds = null;
        boolean all = false;
        HashtableOnDisk cache_instance = null;
        try {
            rwLock.readLock().lock();
            if (type == CACHE_ID_DATA) {
                cache_instance = object_cache;
            } else if (type == DEP_ID_DATA) {
                cache_instance = dependency_cache;
            } else {
                cache_instance = template_cache;
            }
            if (cache_instance != null) {
                if (index < 0) {
                    index = cache_instance.getPreviousRangeIndex();
                } else if (index > 0) {
                    index = cache_instance.getNextRangeIndex();
                } else {
                    cache_instance.initRangeIndex();
                }
                if (length == -1) {
                    all = true;
                    length = cache_instance.size();
                }
                if (length <= 0) {
                    result.data = EMPTY_VS;
                    return result;
                }
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName, "cacheName=" + this.cacheName + " before range index=" + index + " length=" + length);
                answer = new ValueSet(length);
                if (checkExpired) {
                    expiredIds = new ValueSet(length / 2);
                }
                ValueSetReadCallback vscr = new ValueSetReadCallback(answer, expiredIds);
                int rangeIndex = cache_instance.iterateKeys(vscr, index, length);
                if (checkExpired) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, methodName, "cacheName=" + this.cacheName + " after range index=" + rangeIndex + " length=" + length + " idsFound=" + answer.size()
                                                 + " idsExpired=" + expiredIds.size());
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, methodName, "cacheName=" + this.cacheName + " after range index=" + rangeIndex + " length=" + length + " idsFound=" + answer.size());
                }
                cache_instance.addRangeIndex(rangeIndex);
                if (checkExpired) {
                    // add a dummy entry to indicate more entries in the disk cache
                    if (!all && (answer.size() + expiredIds.size()) >= length) {
                        answer.add(DISKCACHE_MORE);
                    }
                } else {
                    if (!all && answer.size() >= length) {
                        answer.add(DISKCACHE_MORE);
                    }
                }
                if (expiredIds != null && expiredIds.size() > 0) {
                    this.invalidationBuffer.add(expiredIds, HTODInvalidationBuffer.SCAN_BUFFER, CachePerf.TIMEOUT, CachePerf.LOCAL, !Cache.FROM_DEPID_TEMPLATE_INVALIDATION,
                                                HTODInvalidationBuffer.FIRE_EVENT, HTODInvalidationBuffer.CHECK_FULL);
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, methodName, "*** HTOD SCAN: ids found=" + answer.size() + " expired ids=" + expiredIds.size());
                }
                if (filter) {
                    this.invalidationBuffer.filter(answer);
                }
            }
        } catch (FileManagerException ex) {
            this.diskCacheException = ex;
            result.diskException = ex;
            result.returnCode = DISK_EXCEPTION;
        } catch (HashtableOnDiskException ex) {
            this.diskCacheException = ex;
            result.diskException = ex;
            result.returnCode = DISK_EXCEPTION;
        } catch (IOException ex) {
            this.diskCacheException = ex;
            result.diskException = ex;
            result.returnCode = DISK_EXCEPTION;
        } catch (Exception ex) {
            result.returnCode = OTHER_EXCEPTION;
            result.diskException = ex;
        } finally {
            rwLock.readLock().unlock();
            if (result.returnCode != NO_EXCEPTION) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName, "cacheName=" + this.cacheName + "\n Exception: " + ExceptionUtility.getStackTrace(result.diskException));
            }
            if (result.returnCode == DISK_EXCEPTION || result.returnCode == OTHER_EXCEPTION) {
                com.ibm.ws.ffdc.FFDCFilter.processException(result.diskException, "com.ibm.ws.cache.HTODDynacache.readByRange", "1582", this);
            }
        }
        result.data = answer;
        return result;
    }

    /*************************************************************************
     * readExpiredByRange()
     *************************************************************************/
    protected Result readExpiredByRange(ValueSet expiredIds, int index, int length) {
        final String methodName = "readExpiredByRange()";
        Result result = getFromResultPool();
        HashtableOnDisk cache_instance = null;
        this.scanExpiredTime = 0;
        try {
            rwLock.readLock().lock();
            cache_instance = object_cache;
            if (cache_instance == null) {
                result.bComplete = true;
                return result;
            }
            if (index == 0) {
                cache_instance.rangeExpiredIndex = 0;
            }
            long t = System.nanoTime();
            DynaAction da = new DynaAction(expiredIds, this);
            int rangeIndex = cache_instance.iterateKeys(da, cache_instance.rangeExpiredIndex, length);
            this.scanExpiredTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t);

            if ((expiredIds.size() + da.notExpiredCount) < length) {
                cache_instance.rangeExpiredIndex = 0;
                result.bComplete = true;
            } else {
                cache_instance.rangeExpiredIndex = rangeIndex;
            }
        } catch (FileManagerException ex) {
            this.diskCacheException = ex;
            result.diskException = ex;
            result.returnCode = DISK_EXCEPTION;
        } catch (HashtableOnDiskException ex) {
            this.diskCacheException = ex;
            result.diskException = ex;
            result.returnCode = DISK_EXCEPTION;
        } catch (IOException ex) {
            this.diskCacheException = ex;
            result.diskException = ex;
            result.returnCode = DISK_EXCEPTION;
        } catch (Exception ex) {
            result.returnCode = OTHER_EXCEPTION;
            result.diskException = ex;
        } finally {
            rwLock.readLock().unlock();
            if (result.returnCode != NO_EXCEPTION) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName, "cacheName=" + this.cacheName + "\n Exception: " + ExceptionUtility.getStackTrace(result.diskException));
            }
            if (result.returnCode == DISK_EXCEPTION || result.returnCode == OTHER_EXCEPTION) {
                com.ibm.ws.ffdc.FFDCFilter.processException(result.diskException, "com.ibm.ws.cache.HTODDynacache.readExpiredByRange", "1640", this);
            }
        }

        return result;
    }

    /*************************************************************************
     * readHashcodeByRange()
     * This method is used by MBean to retrive cache ids and its associated hashcode.
     *************************************************************************/
    public Result readHashcodeByRange(int index, int length, boolean debug, boolean useValue) // LI4337-17
    {
        final String methodName = "readHashcodeByRange()";
        Result result = getFromResultPool();
        result.data = null;
        HashtableOnDisk cache_instance = object_cache;;
        try {
            rwLock.readLock().lock();
            if (cache_instance != null) {
                if (index < 0) {
                    index = cache_instance.getPreviousRangeIndex();
                } else if (index > 0) {
                    index = cache_instance.getNextRangeIndex();
                } else {
                    cache_instance.initRangeIndex();
                }
                if (length == -1) {
                    length = cache_instance.size();
                }
                if (length <= 0) {
                    return result;
                }
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName, "cacheName=" + this.cacheName + " before range index=" + index + " length=" + length);
                HashcodeReadCallback hrcb = new HashcodeReadCallback(invalidationBuffer, debug, useValue);
                int rangeIndex = cache_instance.iterateKeys(hrcb, index, length);
                cache_instance.addRangeIndex(rangeIndex);
                if (hrcb.isValidHashCode == true) {
                    result.totalHashcode = hrcb.totalHashcode;
                    result.data = hrcb.list;
                    result.dataSize = hrcb.count;
                    if ((hrcb.count + hrcb.expiredCount) == 100) {
                        result.bMore = true;
                    } else {
                        result.bMore = false;
                    }
                } else {
                    result.returnCode = HTODDynacache.NO_HASHCODE_OLD_FORMAT;
                }
            }
        } catch (FileManagerException ex) {
            this.diskCacheException = ex;
            result.diskException = ex;
            result.returnCode = DISK_EXCEPTION;
        } catch (HashtableOnDiskException ex) {
            this.diskCacheException = ex;
            result.diskException = ex;
            result.returnCode = DISK_EXCEPTION;
        } catch (IOException ex) {
            this.diskCacheException = ex;
            result.diskException = ex;
            result.returnCode = DISK_EXCEPTION;
        } catch (Exception ex) {
            result.returnCode = OTHER_EXCEPTION;
            result.diskException = ex;
        } finally {
            rwLock.readLock().unlock();
            if (result.returnCode != NO_EXCEPTION) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName, "cacheName=" + this.cacheName + "\n Exception: " + ExceptionUtility.getStackTrace(result.diskException));
            }
            if (result.returnCode == DISK_EXCEPTION || result.returnCode == OTHER_EXCEPTION) {
                com.ibm.ws.ffdc.FFDCFilter.processException(result.diskException, "com.ibm.ws.cache.HTODDynacache.readByRange", "1582", this);
            }
        }
        if (tc.isDebugEnabled())
            Tr.debug(tc, methodName, "cacheName=" + this.cacheName + " returnCode=" + result.returnCode + " size=" + result.dataSize + " more=" + result.bMore);
        return result;
    }

    /*************************************************************************
     * writeValueSet()
     * These are common for both dependencies and templates so we parameterize
     * the basic operations by HTOD instance.
     *************************************************************************/
    int writeValueSet(int type, Object id, ValueSet vs, boolean all) {
        final String methodName = "writeValueSet()";
        int returnCode = NO_EXCEPTION;
        Exception diskException = null;
        HashtableOnDisk cache_instance = null;
        if (vs.size() == 0) {
            return returnCode;
        }
        HashtableOnDisk vshtod = null;
        try {
            rwLock.writeLock().lock();
            if (type == DEP_ID_DATA) {
                cache_instance = dependency_cache;
            } else {
                cache_instance = template_cache;
            }
            //if (this.testException) {
            //    throw new FileManagerException("*** Generate filemanager exception");
            //}
            Long vsinstid = (Long) cache_instance.get(id);
            long vsinstance = 0;
            FileManager filemgr = cache_instance.getFileManager();
            if (vsinstid == null) {
                //
                // no such instance as yet.  Make one and put its pointer into the main valueset htod.
                //
                int valueSize = calculateTableSize(id, vs.size());
                vsinstance = HashtableOnDisk.createInstance(filemgr, valueSize, hashtable_threshold);
                vshtod = HashtableOnDisk.getInstance(filemgr, valueset_rehash, vsinstance, !HashtableOnDisk.HAS_CACHE_VALUE, this);
                cache_instance.put(id, new Long(vsinstance));
            } else {
                //
                // Existing instance.  Get it and clear it.
                // Is this right?  In the original code this method completely replaced the
                // existing valueset we I assume we want to do that here also.  If we don't
                // clear it this method would perform a merge.  If a merge is ok then we
                // can skip this step with potential for significant performance benefit.
                //
                // I don't think we should clear
                vsinstance = vsinstid.longValue();
                if (vsinstance != -1) {
                    vshtod = HashtableOnDisk.getInstance(filemgr, valueset_rehash, vsinstance, !HashtableOnDisk.HAS_CACHE_VALUE, this);
                } else {
                    int valueSize = calculateTableSize(id, vs.size());
                    vsinstance = HashtableOnDisk.createInstance(filemgr, valueSize, hashtable_threshold);
                    vshtod = HashtableOnDisk.getInstance(filemgr, valueset_rehash, vsinstance, !HashtableOnDisk.HAS_CACHE_VALUE, this);
                    cache_instance.put(id, new Long(vsinstance));
                }
                // pass the new table size to HashtableOndisk so that it can use it to rehash
                if (all == ALL) {
                    if (vshtod.size() > 0) {
                        vshtod.clear();
                    }
                    vshtod.tempTableSize = calculateTableSize(id, vs.size());
                } else {
                    vshtod.tempTableSize = calculateTableSize(id, vshtod.size() + vs.size());
                }
                //if (vshtod.size() < vs.size()) {
                //    // pass the new table size to HashtableOndisk so that it can use it to rehash
                //    vshtod.tempTableSize = calculateTableSize(id, vs.size());
                //}
                //vshtod.clear();
            }

            Object entry = null;
            do {
                entry = vs.getOne();
                if (entry != null) {
                    vshtod.put(entry, null); // value is null - better performance by skipping serialization and deserialization
                    vs.remove(entry);
                }
            } while (entry != null);

            vshtod.close();
            vshtod = null;
        } catch (FileManagerException ex) {
            this.diskCacheException = ex;
            diskException = ex;
            returnCode = DISK_EXCEPTION;
        } catch (HashtableOnDiskException ex) {
            this.diskCacheException = ex;
            diskException = ex;
            returnCode = DISK_EXCEPTION;
        } catch (IOException ex) {
            diskException = ex;
            if (ex.getMessage().equals(DISK_CACHE_IN_GB_OVER_LIMIT_MSG)) {
                returnCode = DISK_SIZE_OVER_LIMIT_EXCEPTION;
            } else {
                this.diskCacheException = ex;
                returnCode = DISK_EXCEPTION;
            }
        } catch (Exception ex) {
            returnCode = OTHER_EXCEPTION;
            diskException = ex;
        } finally {
            if (returnCode != NO_EXCEPTION) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName, "cacheName=" + this.cacheName + "\n Exception: " + ExceptionUtility.getStackTrace(diskException));
            }
            if (returnCode == DISK_EXCEPTION || returnCode == OTHER_EXCEPTION) {
                com.ibm.ws.ffdc.FFDCFilter.processException(diskException, "com.ibm.ws.cache.HTODDynacache.writeValueSet", "1738", this);
            }
            try {
                if (vshtod != null) {
                    vshtod.close();
                }
            } catch (Exception ex) {
                com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.HTODDynacache.writeValueSet", "1745", this);
            }
            rwLock.writeLock().unlock();
        }

        return returnCode;
    }

    /*************************************************************************
     * delValueSet()
     * Delete all entries from the specified dependency
     *************************************************************************/
    int delValueSet(int type, Object id) {
        final String methodName = "delValueSet()";
        int returnCode = NO_EXCEPTION;
        Exception diskException = null;
        HashtableOnDisk cache_instance = null;
        HashtableOnDisk vshtod = null;
        try {
            rwLock.writeLock().lock();
            if (type == DEP_ID_DATA) {
                cache_instance = dependency_cache;
            } else {
                cache_instance = template_cache;
            }
            //if (this.testException) {
            //    throw new FileManagerException("*** Generate filemanager exception");
            //}
            Long vsinstid = (Long) cache_instance.get(id);
            if (vsinstid != null) {
                //
                // Existing instance.  Get it and clear it.  Can't think of a better way to do this.
                //
                long vsinstance = vsinstid.longValue();
                FileManager filemgr = cache_instance.getFileManager();
                if (vsinstance != -1) {
                    vshtod = HashtableOnDisk.getInstance(filemgr, valueset_rehash, vsinstance, !HashtableOnDisk.HAS_CACHE_VALUE, this);
                    vshtod.clear();
                    vshtod.close();
                    vshtod = null;
                    HashtableOnDisk.destroyInstance(filemgr, vsinstance);
                }
                cache_instance.remove(id);
            }
        } catch (FileManagerException ex) {
            this.diskCacheException = ex;
            diskException = ex;
            returnCode = DISK_EXCEPTION;
        } catch (HashtableOnDiskException ex) {
            this.diskCacheException = ex;
            diskException = ex;
            returnCode = DISK_EXCEPTION;
        } catch (IOException ex) {
            this.diskCacheException = ex;
            diskException = ex;
            returnCode = DISK_EXCEPTION;
        } catch (Exception ex) {
            returnCode = OTHER_EXCEPTION;
            diskException = ex;
        } finally {
            if (returnCode != NO_EXCEPTION) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName, "cacheName=" + this.cacheName + "\n Exception: " + ExceptionUtility.getStackTrace(diskException));
            }
            if (returnCode == DISK_EXCEPTION || returnCode == OTHER_EXCEPTION) {
                com.ibm.ws.ffdc.FFDCFilter.processException(diskException, "com.ibm.ws.cache.HTODDynacache.delValueSet", "1811", this);
            }
            try {
                if (vshtod != null) {
                    vshtod.close();
                }
            } catch (Exception ex) {
                com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.HTODDynacache.delValueSet", "1818", this);
            }
            rwLock.writeLock().unlock();
        }

        return returnCode;
    }

    /*************************************************************************
     * delValueSetEntry()
     * Delete a single entry from the serialized ValueSet
     *************************************************************************/
    int delValueSetEntry(int type, Object id, Object entry) {
        final String methodName = "delValueSetEntry()";
        int returnCode = NO_EXCEPTION;
        Exception diskException = null;
        HashtableOnDisk cache_instance = null;
        HashtableOnDisk vshtod = null;
        try {
            rwLock.writeLock().lock();
            if (type == DEP_ID_DATA) {
                cache_instance = dependency_cache;
            } else {
                cache_instance = template_cache;
            }
            //if (this.testException) {
            //    throw new FileManagerException("*** Generate filemanager exception");
            //}
            Long vsinstid = (Long) cache_instance.get(id);
            if (vsinstid != null) {
                long vsinstance = vsinstid.longValue();
                FileManager filemgr = cache_instance.getFileManager();
                if (vsinstance != -1) {
                    vshtod = HashtableOnDisk.getInstance(filemgr, valueset_rehash, vsinstance, !HashtableOnDisk.HAS_CACHE_VALUE, this);
                    vshtod.remove(entry);
                    if (vshtod.size() == 0) {
                        HashtableOnDisk.destroyInstance(filemgr, vsinstance);
                        cache_instance.remove(id);
                    }
                    vshtod.close();
                    vshtod = null;
                } else {
                    cache_instance.remove(id);
                }
            }
        } catch (FileManagerException ex) {
            this.diskCacheException = ex;
            diskException = ex;
            returnCode = DISK_EXCEPTION;
        } catch (HashtableOnDiskException ex) {
            this.diskCacheException = ex;
            diskException = ex;
            returnCode = DISK_EXCEPTION;
        } catch (IOException ex) {
            this.diskCacheException = ex;
            diskException = ex;
            returnCode = DISK_EXCEPTION;
        } catch (Exception ex) {
            returnCode = OTHER_EXCEPTION;
            diskException = ex;
        } finally {
            if (returnCode != NO_EXCEPTION) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName, "cacheName=" + this.cacheName + "\n Exception: " + ExceptionUtility.getStackTrace(diskException));
            }
            if (returnCode == DISK_EXCEPTION || returnCode == OTHER_EXCEPTION) {
                com.ibm.ws.ffdc.FFDCFilter.processException(diskException, "com.ibm.ws.cache.HTODDynacache.delValueSetEntry", "1881", this);
            }
            try {
                if (vshtod != null) {
                    vshtod.close();
                }
            } catch (Exception ex) {
                com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.HTODDynacache.delValueSetEntry", "1888", this);
            }
            rwLock.writeLock().unlock();
        }

        return returnCode;
    }

    /*************************************************************************
     * writeValueSetEntry()
     * Write a single entry to a (possible existant) value set, creating a new
     * VS instance if needed.
     *************************************************************************/
    Result writeValueSetEntry(int type,
                              Object id,
                              Object entry,
                              ValueSet valueSet,
                              boolean addIfNew) {
        final String methodName = "writeValueSetEntry()";
        Result result = getFromResultPool();
        HashtableOnDisk cache_instance = null;
        HashtableOnDisk vshtod = null;
        try {
            rwLock.writeLock().lock();
            if (type == DEP_ID_DATA) {
                cache_instance = dependency_cache;
            } else {
                cache_instance = template_cache;
            }
            //if (this.testException) {
            //    throw new FileManagerException("*** Generate filemanager exception");
            //}
            Long vsinstid = (Long) cache_instance.get(id);
            long vsinstance = 0;
            FileManager filemgr = cache_instance.getFileManager();
            if (vsinstid == null) {
                //
                // no such instance as yet.  Make one and put its pointer into the main valueset htod.
                //
                if (addIfNew && entry != null) {
                    vsinstance = HashtableOnDisk.createInstance(filemgr, initial_valueset_size, hashtable_threshold);
                    cache_instance.put(id, new Long(vsinstance));
                    vshtod = HashtableOnDisk.getInstance(filemgr, valueset_rehash, vsinstance, !HashtableOnDisk.HAS_CACHE_VALUE, this);
                    vshtod.put(entry, null); // value is null - better performance by skipping serialization and deserialization
                } else {
                    cache_instance.put(id, new Long(-1));
                }
            } else {
                //
                // Existing instance.  Get it.
                //
                result.bExist = EXIST;
                vsinstance = vsinstid.longValue();
                if (vsinstance != -1) {
                    vshtod = HashtableOnDisk.getInstance(filemgr, valueset_rehash, vsinstance, !HashtableOnDisk.HAS_CACHE_VALUE, this);
                } else {
                    vsinstance = HashtableOnDisk.createInstance(filemgr, initial_valueset_size, hashtable_threshold);
                    vshtod = HashtableOnDisk.getInstance(filemgr, valueset_rehash, vsinstance, !HashtableOnDisk.HAS_CACHE_VALUE, this);
                    cache_instance.put(id, new Long(vsinstance));
                }
                if (valueSet != null) {
                    int length = vshtod.size();
                    if (length > 0) {
                        if ((valueSet.size() + length) > this.cod.delayOffloadEntriesLimit) {
                            vshtod.tempTableSize = calculateTableSize(id, vshtod.size() + valueSet.size());
                            entry = null;
                            do {
                                entry = valueSet.getOne();
                                if (entry != null) {
                                    vshtod.put(entry, null); // value is null - better performance by skipping serialization and deserialization
                                    valueSet.remove(entry);
                                }
                            } while (entry != null);
                            result.bComplete = true;
                        } else {
                            ValueSet vs = new ValueSet(length);
                            ValueSetReadCallback vscr = new ValueSetReadCallback(vs, null);
                            vshtod.iterateKeys(vscr, 0, length);
                            valueSet.union(vs);
                            vshtod.clear();
                        }
                    }
                } else {
                    if (entry != null) {
                        vshtod.put(entry, null); // value is null - better performance by skipping serialization and deserialization
                    }
                }
            }

            if (tc.isDebugEnabled())
                Tr.debug(tc, "writeValueSetEntry()", ": cachename=" + this.cacheName + " id=" + id + " entry=" + entry + " size=" + ((vshtod != null) ? vshtod.size() : 0));

            if (vshtod != null) {
                vshtod.close();
                vshtod = null;
            }
        } catch (FileManagerException ex) {
            this.diskCacheException = ex;
            result.diskException = ex;
            result.returnCode = DISK_EXCEPTION;
        } catch (HashtableOnDiskException ex) {
            this.diskCacheException = ex;
            result.diskException = ex;
            result.returnCode = DISK_EXCEPTION;
        } catch (IOException ex) {
            result.diskException = ex;
            if (ex.getMessage().equals(DISK_CACHE_IN_GB_OVER_LIMIT_MSG)) {
                result.returnCode = DISK_SIZE_OVER_LIMIT_EXCEPTION;
            } else {
                this.diskCacheException = ex;
                result.returnCode = DISK_EXCEPTION;
            }
        } catch (Exception ex) {
            result.returnCode = OTHER_EXCEPTION;
            result.diskException = ex;
        } finally {
            if (result.returnCode != NO_EXCEPTION) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName, "cacheName=" + this.cacheName + "\n Exception: " + ExceptionUtility.getStackTrace(result.diskException));
            }
            if (result.returnCode == DISK_EXCEPTION || result.returnCode == OTHER_EXCEPTION) {
                com.ibm.ws.ffdc.FFDCFilter.processException(result.diskException, "com.ibm.ws.cache.HTODDynacache.writeValueSetEntry", "1967", this);
            }
            try {
                if (vshtod != null) {
                    vshtod.close();
                }
            } catch (Exception ex) {
                com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.HTODDynacache.writeValueSetEntry", "1974", this);
            }
            rwLock.writeLock().unlock();
        }

        return result;
    }

    public int getNumObjects() {
        try {
            rwLock.readLock().lock();
            int answer = Math.max(object_cache.size(), 0);
            if (!this.disableDependencyId) {
                answer += Math.max(dependency_cache.size(), 0);
            }
            if (!this.disableTemplatesSupport) {
                answer += Math.max(template_cache.size(), 0);
            }
            return answer;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public String cacheType() {
        return "htod";
    }

    public void dump_htod_stats(Writer out, boolean labels) {
        try {
            object_cache.dump_htod_stats(out, labels);
        } catch (Exception e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.cache.HTODDynacache.dump_htod_stats", "2646", this);
        }
    }

    public void dump_filemgr_stats(Writer out, boolean labels) {
        try {
            object_cache.dump_filemgr_stats(out, labels);
        } catch (Exception e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.cache.HTODDynacache.dump_filemgr_stats", "2655", this);
        }
    }

    public void dump_stats_header(Writer out) {
        try {
            object_cache.dump_stats_header(out);
        } catch (Exception e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.cache.HTODDynacache.dump_stats_header", "2664", this);
        }
    }

    public void dump_filemgr_header(Writer out) {
        try {
            object_cache.dump_filemgr_header(out);
        } catch (Exception e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.cache.HTODDynacache.dump_filemgr_header", "2673", this);
        }
    }

    public void reset_stats() {
        object_cache.reset_stats();
    }

    /*************************************************************************
     * containsKey()
     * Check whether the cache id exists in the disk or not.
     *************************************************************************/
    public boolean containsKey(Object id) {
        final String methodName = "containsKey()";
        boolean found = false;

        if (!this.invalidationBuffer.contains(id)) {
            try {
                rwLock.readLock().lock();
                found = object_cache.containsKey(id);
            } catch (Throwable t) {
                com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.cache.HTODDynacache.containsKey", "1877", this);
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName, "cacheName=" + this.cacheName + "\nException: " + ExceptionUtility.getStackTrace(t));
            } finally {
                rwLock.readLock().unlock();
            }
        }
        return found;
    }

    private int populateAuxDependencyTable(int type, HashMap cacheIdsTable, int vlimit, int hlimit) {
        final String methodName = "populateAuxDependencyTable()";
        int returnCode = NO_EXCEPTION;
        HTODDependencyTable depTable = null;
        int size = 0;
        if (type == DEP_ID_DATA) {
            depTable = this.auxDataDependencyTable;
            size = getDepIdsSize();
        } else {
            depTable = this.auxTemplateDependencyTable;
            size = getTemplatesSize();
        }
        if (size == 0) {
            return returnCode;
        }
        Result result = readByRange(type, 0, -1, !CHECK_EXPIRED, !FILTER);
        returnCode = result.returnCode;
        if (returnCode == DISK_EXCEPTION) {
            returnToResultPool(result);
            return returnCode;
        }
        ValueSet valueSet = (ValueSet) result.data;
        returnToResultPool(result);
        if (valueSet == null || valueSet.isEmpty()) {
            return returnCode;
        }
        Iterator it = null;
        Iterator it2 = null;
        if (this.cod.diskCachePerformanceLevel == CacheConfig.CUSTOM || this.cod.diskCachePerformanceLevel == CacheConfig.BALANCED) {
            // collect all the dependencies with size of cache entries which is NOT over the
            // limit
            ArrayList depSizes = new ArrayList(vlimit);
            int index = 0;
            it = valueSet.iterator();
            while (it.hasNext()) {
                Object did = it.next();
                result = readValueSetSize(type, did);
                if (returnCode == DISK_EXCEPTION) {
                    returnToResultPool(result);
                    return returnCode;
                }
                int csize = result.dataSize;
                if (csize <= hlimit) {
                    DepEntry de = new DepEntry(did, csize);
                    depSizes.add(index, de);
                    index++;
                }
            }
            // **** For debug use only to invert some dummy dependencies. 
            //      This verifies the sorting and dependency removal 
            //for (int i=0; i < 10; i++) {
            //    DepEntry de = new DepEntry("did" + i, (i + 1) * 5);
            //    depSizes.add(index, de);
            //    index++;
            //}
            depSizes.trimToSize();
            // if number of dependencies is over the limit, remove the ones which has less 
            // number of cache entries
            if (depSizes.size() > vlimit) {
                int dsize = depSizes.size() - vlimit;
                Collections.sort(depSizes, new DepSizeComparator());
                for (int i = 0; i < dsize; i++) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, methodName, " remove id=" + ((DepEntry) depSizes.get(0)).id);
                    depSizes.remove(0);
                }
            }
            int dsize = depSizes.size();
            valueSet.clear();
            for (int i = 0; i < dsize; i++) {
                valueSet.add(((DepEntry) depSizes.get(i)).id);
            }
        }
        it = valueSet.iterator();
        while (it.hasNext()) {
            Object did = it.next();
            result = readValueSet(type, did, !DELETE);
            if (returnCode == DISK_EXCEPTION) {
                returnToResultPool(result);
                return returnCode;
            }
            ValueSet tempValueSet = (ValueSet) result.data;
            returnToResultPool(result);
            if (tempValueSet == null || tempValueSet.isEmpty()) {
                continue;
            }
            ValueSet idValueSet = new ValueSet(tempValueSet.size());
            it2 = tempValueSet.iterator();
            // To reduce the memory consumption used by cache ids. The cache ids table is built
            // so that same cache id object can be used in the dependency table, template table
            // and timeLimitDaemon table if running high performance mode.
            while (it2.hasNext()) {
                Object cid = it2.next();
                Object tid = cacheIdsTable.get(cid);
                if (tid == null) {
                    cacheIdsTable.put(cid, cid);
                    tid = cid;
                }
                idValueSet.add(tid);
            }
            depTable.add(did, idValueSet);
            if (tc.isDebugEnabled())
                Tr.debug(tc, methodName, " did= " + did + " cacheIdsSize=" + idValueSet.size());
            if (returnCode == DISK_EXCEPTION) {
                return returnCode;
            }
        }
        return returnCode;
    }

    /*************************************************************************
     * removeExpiredCache()
     * Remove all the cache ids in the Invalidation buffers.
     * If scan is true, scan the disk for expired entries.
     *************************************************************************/
    protected int removeExpiredCache(boolean scan) {
        final String methodName = "removeExpiredCache()";
        int returnCode = NO_EXCEPTION;
        if (object_cache == null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, methodName, "The disk object cache is null for cacheName=" + this.cacheName);
            return returnCode;
        }
        long start = System.nanoTime();
        long tPopulateDepTablesTime = 0;
        long tReadRangeTime = 0;
        long tDeleteTime = 0;
        long tScanExpiredCount = 0;
        long tExplicitDeleted = 0;
        long tScanDeleted = 0;
        long tGCDeleted = 0;
        long tDeletedSize = 0;
        long t;
        if (!scan) {
            int totalSize = this.invalidationBuffer.size() + this.invalidationBuffer.size(HTODInvalidationBuffer.GC_BUFFER);
            if (totalSize == 0) { // invalidationBuffer empty?
                return returnCode;
            }
        }
        if (getCacheIdsSize() > 0) {
            StringBuffer dcmsg = new StringBuffer();
            dcmsg.append("  DiskCacheSize=");
            dcmsg.append(getCacheIdsSize());
            dcmsg.append("  DepIdsSize=");
            dcmsg.append(getDepIdsSize());
            dcmsg.append("  TemplatesSize=");
            dcmsg.append(getTemplatesSize());
            if (this.cod.enableCacheSizeInBytes) {
                dcmsg.append("  CacheSizeInBytes=");
                dcmsg.append(this.cod.currentCacheSizeInBytes);
            }
            dcmsg.append("  Scan=");
            dcmsg.append(scan);
            dcmsg.append("  ExplicitBuffer=");
            dcmsg.append(this.invalidationBuffer.size(HTODInvalidationBuffer.EXPLICIT_BUFFER));
            dcmsg.append("  ScanBuffer=");
            dcmsg.append(this.invalidationBuffer.size(HTODInvalidationBuffer.SCAN_BUFFER));
            dcmsg.append("  GCBuffer=");
            dcmsg.append(this.invalidationBuffer.size(HTODInvalidationBuffer.GC_BUFFER));
            if (scan) {
                Tr.info(tc, "DYNA0057I", new Object[] { this.cacheName, dcmsg.toString() });
            } else if (!scan && tc.isEventEnabled()) {
                Tr.event(tc, "The disk cache cleanup started for the cache name \"" + this.cacheName + "\"." + "The statistics are: " + dcmsg.toString());
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName, "The disk cache cleanup started for cache name \"" + this.cacheName + "\"." + " The statistics are: " + dcmsg.toString());
            }
            if (this.cod.populateDependencyTable) {
                cacheIdsTable = new HashMap(getCacheIdsSize());
                this.cod.populateDependencyTable = false;
                t = System.nanoTime();
                if (!this.disableDependencyId) {
                    returnCode = populateAuxDependencyTable(DEP_ID_DATA, cacheIdsTable, this.cod.delayOffloadDepIdBuckets, cod.delayOffloadEntriesLimit);
                    if (returnCode == DISK_EXCEPTION) {
                        cacheIdsTable.clear();
                        cacheIdsTable = null;
                        return returnCode;
                    }
                }
                if (!this.disableTemplatesSupport) {
                    returnCode = populateAuxDependencyTable(TEMPLATE_ID_DATA, cacheIdsTable, this.cod.delayOffloadTemplateBuckets, cod.delayOffloadEntriesLimit);
                    if (returnCode == DISK_EXCEPTION) {
                        cacheIdsTable.clear();
                        cacheIdsTable = null;
                        return returnCode;
                    }
                }
                tPopulateDepTablesTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t);
            }

            int i = 0;
            boolean complete = false;
            ValueSet expiredIds = new ValueSet(NUM_SCAN);
            long lastTimeToDisplay = System.nanoTime();
            boolean firstScan = true;
            do {
                int scanExpiredCount = 0;
                this.scanExpiredTime = 0;
                if (scan && (this.invalidationBuffer.size() < this.cod.invalidationBufferSize)) {
                    if (this.invalidationBuffer.isLoopOnce()) {
                        break;
                    }
                    if (this.cod.evictionPolicy != CacheConfig.EVICTION_NONE &&
                        this.cod.diskCachePerformanceLevel != CacheConfig.HIGH &&
                        this.EvictionTable.size() > 0) {
                        ArrayList alist = getExpiredEntriesFromEvictionTable();
                        tScanExpiredCount += alist.size();
                        this.invalidationBuffer.add(alist, HTODInvalidationBuffer.GC_BUFFER);
                        if (tc.isDebugEnabled() || tc.isEventEnabled()) {
                            lastTimeToDisplay = System.nanoTime();
                            dcmsg.setLength(0);
                            dcmsg.append(methodName);
                            dcmsg.append(":scanGC ++++ cacheName=");
                            dcmsg.append(this.cacheName);
                            dcmsg.append(" DiskCacheSize=");
                            dcmsg.append(getCacheIdsSize());
                            if (this.cod.enableCacheSizeInBytes) {
                                dcmsg.append("  CacheSizeInBytes=");
                                dcmsg.append(this.cod.currentCacheSizeInBytes);
                            }
                            dcmsg.append(" ScanExpiredEntries=");
                            dcmsg.append(tScanExpiredCount);
                            dcmsg.append(" ExplicitEntriesDeleted=");
                            dcmsg.append(tExplicitDeleted);
                            dcmsg.append(" ScanEntriesDeleted=");
                            dcmsg.append(tScanDeleted);
                            dcmsg.append(" GCEntriesDeleted=");
                            dcmsg.append(tGCDeleted);
                            dcmsg.append(" DeletedSize=");
                            dcmsg.append(tDeletedSize);
                            dcmsg.append(" ExplicitBuffer=");
                            dcmsg.append(this.invalidationBuffer.size(HTODInvalidationBuffer.EXPLICIT_BUFFER));
                            dcmsg.append(" ScanBuffer=");
                            dcmsg.append(this.invalidationBuffer.size(HTODInvalidationBuffer.SCAN_BUFFER));
                            dcmsg.append(" GCBuffer=");
                            dcmsg.append(this.invalidationBuffer.size(HTODInvalidationBuffer.GC_BUFFER));
                            dcmsg.append(" CleanupScanPending=");
                            dcmsg.append(this.invalidationBuffer.isCleanupPending());

                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, dcmsg.toString());
                            } else {
                                Tr.event(tc, dcmsg.toString());
                            }
                        }
                        complete = true;
                    } else {
                        if (firstScan) {
                            i = 0;
                            firstScan = false;
                        }
                        Result result = readExpiredByRange(expiredIds, i, NUM_SCAN);
                        returnCode = result.returnCode;
                        if (returnCode == DISK_EXCEPTION) {
                            break;
                        }
                        complete = result.bComplete;
                        tReadRangeTime = tReadRangeTime + this.scanExpiredTime;
                        scanExpiredCount = expiredIds.size();
                        if (scanExpiredCount > 0) {
                            this.invalidationBuffer.add(expiredIds, HTODInvalidationBuffer.SCAN_BUFFER, CachePerf.TIMEOUT, CachePerf.LOCAL,
                                                        !Cache.FROM_DEPID_TEMPLATE_INVALIDATION, HTODInvalidationBuffer.FIRE_EVENT, !HTODInvalidationBuffer.CHECK_FULL);
                            expiredIds.clear();
                        }
                        tScanExpiredCount += scanExpiredCount;
                        if (tc.isEventEnabled() && (System.nanoTime() - lastTimeToDisplay) >= TimeUnit.SECONDS.toNanos(10)) {
                            lastTimeToDisplay = System.nanoTime();
                            dcmsg.setLength(0);
                            dcmsg.append(methodName);
                            dcmsg.append(":scanDisk ++++ cacheName=");
                            dcmsg.append(this.cacheName);
                            dcmsg.append(" DiskCacheSize=");
                            dcmsg.append(getCacheIdsSize());
                            if (this.cod.enableCacheSizeInBytes) {
                                dcmsg.append("  CacheSizeInBytes=");
                                dcmsg.append(this.cod.currentCacheSizeInBytes);
                            }
                            dcmsg.append(" ScanExpiredEntries=");
                            dcmsg.append(tScanExpiredCount);
                            dcmsg.append(" ExplicitEntriesDeleted=");
                            dcmsg.append(tExplicitDeleted);
                            dcmsg.append(" ScanEntriesDeleted=");
                            dcmsg.append(tScanDeleted);
                            dcmsg.append(" GCEntriesDeleted=");
                            dcmsg.append(tGCDeleted);
                            dcmsg.append(" DeletedSize=");
                            dcmsg.append(tDeletedSize);
                            dcmsg.append(" ExplicitBuffer=");
                            dcmsg.append(this.invalidationBuffer.size(HTODInvalidationBuffer.EXPLICIT_BUFFER));
                            dcmsg.append(" ScanBuffer=");
                            dcmsg.append(this.invalidationBuffer.size(HTODInvalidationBuffer.SCAN_BUFFER));
                            dcmsg.append(" GCBuffer=");
                            dcmsg.append(this.invalidationBuffer.size(HTODInvalidationBuffer.GC_BUFFER));
                            dcmsg.append(" CleanupScanPending=");
                            dcmsg.append(this.invalidationBuffer.isCleanupPending());
                            Tr.event(tc, dcmsg.toString());
                        }
                        if (getDiskCacheSizeInfo().doYield(getCacheIdsSize(), this.cod.currentCacheSizeInBytes)) {
                            Thread.yield();
                        }
                    }
                } else if (!scan) {
                    complete = true;
                }
                if (this.invalidationBuffer.isLoopOnce()) {
                    break;
                }
                t = System.nanoTime();
                Result res = deleteEntriesFromInvalidationBuffer(true);
                tDeleteTime += System.nanoTime() - t;
                returnCode = res.returnCode;
                tExplicitDeleted += res.numExplicitDeleted;
                tScanDeleted += res.numScanDeleted;
                tGCDeleted += res.numGCDeleted;
                tDeletedSize += res.deletedSize;
                returnToResultPool(res);

                if (returnCode == DISK_EXCEPTION) {
                    break;
                }
                if (complete || returnCode == DISK_EXCEPTION || this.invalidationBuffer.isLoopOnce()) {
                    break;
                }
                i++;
            } while (i > 0);
            this.totalDeleted += (tExplicitDeleted + tScanDeleted + tGCDeleted);
            this.totalDeletedSize += tDeletedSize;
            dcmsg.setLength(0);
            dcmsg.append("  DiskCacheSize=");
            dcmsg.append(getCacheIdsSize());
            dcmsg.append("  DepIdsSize=");
            dcmsg.append(getDepIdsSize());
            dcmsg.append("  TemplatesSize=");
            dcmsg.append(getTemplatesSize());
            if (this.cod.enableCacheSizeInBytes) {
                dcmsg.append("  CacheSizeInBytes=");
                dcmsg.append(this.cod.currentCacheSizeInBytes);
            }
            dcmsg.append("  TimeElapsed=");
            dcmsg.append(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
            if (tPopulateDepTablesTime != 0) {
                dcmsg.append("  PopulateDepTablesTime=");
                dcmsg.append(tPopulateDepTablesTime);
            }
            dcmsg.append("  ScanTime=");
            dcmsg.append(tReadRangeTime);
            dcmsg.append(" ScanExpiredEntries=");
            dcmsg.append(tScanExpiredCount);
            dcmsg.append("  DeleteTime=");
            dcmsg.append(TimeUnit.NANOSECONDS.toMillis(tDeleteTime));
            dcmsg.append("  ExplicitEntriesDeleted=");
            dcmsg.append(tExplicitDeleted);
            dcmsg.append("  ScanEntriesDeleted=");
            dcmsg.append(tScanDeleted);
            dcmsg.append("  GCEntriesDeleted=");
            dcmsg.append(tGCDeleted);
            dcmsg.append("  DeletedSize=");
            dcmsg.append(tDeletedSize);
            dcmsg.append("  totalDeleted=");
            dcmsg.append(this.totalDeleted);
            dcmsg.append("  totalDeletedSize=");
            dcmsg.append(this.totalDeletedSize);

            if (scan) {
                Tr.info(tc, "DYNA0058I", new Object[] { this.cacheName, dcmsg.toString() });
            } else if (!scan && tc.isEventEnabled()) {
                Tr.event(tc, "The disk cache cleanup finished for cache name \"" + this.cacheName + "\"." + " The statistics are: " + dcmsg.toString());
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName, "The disk cache cleanup finished for cache name \"" + this.cacheName + "\"." + " The statistics are: " + dcmsg.toString());
            }
            getDiskCacheSizeInfo().displayDiskCacheInfo();
        }

        this.invalidationBuffer.setlastRemoveTime();
        // clear the cache ids table to reduce the memory usage.
        this.cod.populateDependencyTable = false;
        if (cacheIdsTable != null) {
            cacheIdsTable.clear();
            cacheIdsTable = null;
        }
        return returnCode;
    }

    protected Result deleteEntriesFromInvalidationBuffer(boolean explicitBufferFirst) {
        final String methodName = "deleteEntriesFromInvalidationBuffer()";
        Result result = getFromResultPool();
        result.returnCode = NO_EXCEPTION;
        if (object_cache == null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, methodName, "The disk object cache is null for cacheName=" + this.cacheName);
            return result;
        }
        Object id = null;
        do {
            if (this.invalidationBuffer.isLoopOnce()) {
                break;
            }
            int bufferType;

            if (explicitBufferFirst) { //determines the order of processing invalidation buffers
                id = this.invalidationBuffer.getAndRemove(HTODInvalidationBuffer.EXPLICIT_BUFFER);
                bufferType = HTODInvalidationBuffer.EXPLICIT_BUFFER;
                if (id == null) {
                    id = this.invalidationBuffer.getAndRemove(HTODInvalidationBuffer.SCAN_BUFFER);
                    bufferType = HTODInvalidationBuffer.SCAN_BUFFER;
                    if (id == null) {
                        id = this.invalidationBuffer.getAndRemove(HTODInvalidationBuffer.GC_BUFFER);
                        bufferType = HTODInvalidationBuffer.GC_BUFFER;
                    }
                }
            } else {
                id = this.invalidationBuffer.getAndRemove(HTODInvalidationBuffer.GC_BUFFER);
                bufferType = HTODInvalidationBuffer.GC_BUFFER;
                if (id == null) {
                    id = this.invalidationBuffer.getAndRemove(HTODInvalidationBuffer.SCAN_BUFFER);
                    bufferType = HTODInvalidationBuffer.SCAN_BUFFER;
                    if (id == null) {
                        id = this.invalidationBuffer.getAndRemove(HTODInvalidationBuffer.EXPLICIT_BUFFER);
                        bufferType = HTODInvalidationBuffer.EXPLICIT_BUFFER;
                    }
                }
            }

            if (id != null) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName, "Get id for deletion: cacheName=" + this.cacheName + " id=" + id + " explicitBufferFirst=" + explicitBufferFirst);
                Result res = delCacheEntry(id, bufferType, explicitBufferFirst);
                result.returnCode = res.returnCode;
                if (result.returnCode == DISK_EXCEPTION) {
                    break;
                }
                if (res.numDelete > 0) {
                    id = res.data;
                    result.deletedSize += res.deletedSize;
                    if (bufferType == HTODInvalidationBuffer.EXPLICIT_BUFFER) {
                        result.numExplicitDeleted += res.numDelete;
                    } else if (bufferType == HTODInvalidationBuffer.SCAN_BUFFER) {
                        result.numScanDeleted += res.numDelete;
                        this.cache.internalInvalidateByDepId(id, res.cause, res.source, Cache.FIRE_INVALIDATION_LISTENER);
                    } else {
                        result.numGCDeleted += res.numDelete;
                        this.cache.internalInvalidateByDepId(id, res.cause, res.source, Cache.FIRE_INVALIDATION_LISTENER);
                    }
                }
                returnToResultPool(res);
            }

            if (getDiskCacheSizeInfo().doYield(getCacheIdsSize(), this.cod.currentCacheSizeInBytes)) {
                Thread.yield();
            }

        } while (id != null);

        return result;
    }

    /***************************************************************************
     * calculateTableSize() Return the tablesize.
     **************************************************************************/
    private int calculateTableSize(Object id, int size) {
        final String methodName = "calculateTableSize()";
        int tableSize = initial_valueset_size;
        boolean finish = false;
        do {
            if (size > tableSize) {
                tableSize = (tableSize * 2) + 1;
            } else {
                float ratio = (float) tableSize / (float) size;
                if (ratio < 1.5) {
                    tableSize = (tableSize * 2) + 1;
                }
                finish = true;
            }
        } while (finish == false);
        if (tc.isDebugEnabled())
            Tr.debug(tc, methodName, "cacheName=" + this.cacheName + " id=" + id + " size=" + size + " tableSize=" + tableSize);
        return tableSize;
    }

    /*************************************************************************
     * calculateMinCacheSizeInBytes()
     * Return the mininum cache size in bytes.
     *************************************************************************/
    protected int calculateMinCacheSizeInBytes() {
        return cod.dataHashtableSize * 8 + HashtableOnDisk.HT_INITIAL_OVERHEAD_SIZE;
    }

    /*************************************************************************
     * getDiskCacheSizeInfo()
     * Return the diskCacheSizeInfo.
     *************************************************************************/
    public DiskCacheSizeInfo getDiskCacheSizeInfo() {
        return cod.diskCacheSizeInfo;
    }

    /*************************************************************************
     * returnToHashtableEntryPool()
     * Return the HashtableEntry to the pool for reuse later.
     *************************************************************************/
    public void returnToHashtableEntryPool(HashtableEntry htEntry) {
        if (htEntry != null) {
            htEntry.reset();
            htEntryPool.add(htEntry);
            //htEntryPool.retCount++;
        }
    }

    /*************************************************************************
     * getFromHashtableEntryPool()
     * Get a HashtableEntry from the pool.
     *************************************************************************/
    public HashtableEntry getFromHashtableEntryPool() {
        //htEntryPool.getCount++;
        return (HashtableEntry) htEntryPool.remove();
    }

    /*************************************************************************
     * returnToResultPool()
     * Return the result object to the pool for reuse later.
     *************************************************************************/
    public void returnToResultPool(Result result) {
        if (result != null) {
            result.reset();
            resultPool.add(result);
        }
    }

    /*************************************************************************
     * getFromHashtableEntryPool()
     * Get a HashtableEntry from the pool.
     *************************************************************************/
    public Result getFromResultPool() {
        return (Result) resultPool.remove();
    }

    public ArrayList walkEvictionTable(int evictionPolicy, int deleteEntries, long deleteSize) {

        final String methodName = "walkEvictionTable()";
        if (evictionPolicy != CacheConfig.EVICTION_RANDOM && evictionPolicy != CacheConfig.EVICTION_SIZE_BASED) {
            return null;
        }

        ArrayList vs = null;
        if (deleteEntries > 0) {
            vs = new ArrayList(deleteEntries);
        } else {
            vs = new ArrayList(256);
        }
        int i = 0;
        long size = 0;

        EvictionTableEntry min = null;
        long startCurrentTime = System.currentTimeMillis();
        // Add GC list if the entry is found to be expired
        do {
            synchronized (evictionTableMonitor) {
                min = EvictionTable.minimum();
                if (min != null && min.expirationTime <= startCurrentTime) {
                    vs.add(EvictionTable.deleteMin());
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, methodName, "cacheName=" + this.cacheName + " expiredEntry=" + min.toString());
                    i++;
                    size += min.size;
                } else {
                    min = null;
                }
            }
        } while (min != null);

        if ((deleteEntries > 0 && deleteEntries <= i) || (deleteSize > 0 && deleteSize <= size)) {
            return vs;
        }

        if (evictionPolicy == CacheConfig.EVICTION_RANDOM) {
            // Remove based on Eviction Random policy
            if (deleteEntries > 0) {
                // Remove based on number of entries
                while (i < deleteEntries && EvictionTable.size() > 0) {
                    synchronized (evictionTableMonitor) {
                        int index = rand.nextInt(EvictionTable.size()) + 1;
                        EvictionTableEntry evt = EvictionTable.heapArray[index];
                        if (evt != null) {
                            i++;
                            vs.add(evt);
                            EvictionTable.delete(evt);
                        }
                    }
                }
            } else {
                // Removed base on size
                while (size < deleteSize && EvictionTable.size() > 0) {
                    synchronized (evictionTableMonitor) {
                        int index = rand.nextInt(EvictionTable.size()) + 1;
                        EvictionTableEntry evt = EvictionTable.heapArray[index];
                        if (evt != null) {
                            size += evt.size;
                            vs.add(evt);
                            EvictionTable.delete(evt);
                        }
                    }
                }
            }
        } else if (evictionPolicy == CacheConfig.EVICTION_SIZE_BASED) {
            // Remove based on Eviction Size policy
            synchronized (evictionTableMonitor) {
                EvictionTableEntry[] tempArray = new EvictionTableEntry[EvictionTable.size()];
                System.arraycopy(EvictionTable.heapArray, 1, tempArray, 0, EvictionTable.size());
                Arrays.sort(tempArray, new EvictionSizeComparator());
                int j = tempArray.length - 1;
                if (deleteEntries > 0) {
                    // Remove base on number of entries
                    while (i < deleteEntries && j >= 0) {
                        EvictionTableEntry evt = tempArray[j];
                        i++;
                        j--;
                        vs.add(evt);
                        EvictionTable.delete(evt);
                    }
                } else {
                    while (size < deleteSize && j >= 0) {
                        EvictionTableEntry evt = tempArray[j];
                        size += evt.size;
                        vs.add(evt);
                        j--;
                        EvictionTable.delete(evt);
                    }
                }
            }
        }
        return vs;
    }

    public ArrayList getExpiredEntriesFromEvictionTable() {

        final String methodName = "getExpiredEntriesFromEvictionTable()";
        if (this.cod.evictionPolicy == CacheConfig.EVICTION_NONE) {
            return null;
        }
        ArrayList vs = new ArrayList(256);
        EvictionTableEntry min = null;
        long startCurrenTime = System.currentTimeMillis();

        do {
            synchronized (evictionTableMonitor) {
                min = EvictionTable.minimum();
                if (min != null && min.expirationTime <= startCurrenTime) {
                    vs.add(EvictionTable.deleteMin());
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, methodName, "cacheName=" + this.cacheName + " expiredEntry=" + min.toString());
                } else {
                    min = null;
                }
            }
        } while (min != null);
        if (tc.isDebugEnabled())
            Tr.debug(tc, methodName, "cacheName=" + this.cacheName + " expiredSize=" + vs.size());

        return vs;
    }

    /*************************************************************************
     * This method gets the current pending removal size in the disk invalidation buffers.
     *************************************************************************/
    public int getPendingRemovalSize() {
        return this.invalidationBuffer.size();
    }

    /*************************************************************************
     * This method gets the current size of dependency id buckets in the memory table for the disk.
     *************************************************************************/
    public int getDepIdsBufferedSize() {
        if (delayOffload) {
            if (!this.disableDependencyId) {
                return this.auxDataDependencyTable.size();
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    /*************************************************************************
     * This method gets the current size of template buckets in the memory table for the disk.
     *************************************************************************/
    public int getTemplatesBufferedSize() {
        if (delayOffload) {
            if (!this.disableTemplatesSupport) {
                return this.auxTemplateDependencyTable.size();
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    /*************************************************************************
     * This method releases unused pools which is over its life time.
     *************************************************************************/
    public void releaseUnusedPools() {
        this.byteArrayPool.release();
        this.longArrayPool.release();
    }

    /**
     * Return a boolean to indicate whether the specified cache id exists in the aux dependency table
     */
    public boolean isCacheIdInAuxDepIdTable(Object id) {
        if (delayOffload) {
            if (!this.disableDependencyId) {
                return this.auxDataDependencyTable.containsCacheId(id);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /*************************************************************************
     * HashtableEntryObjectPool
     * The class is the pooling of the HashtableEntry objects used by
     * HashtableOnDisk.
     *************************************************************************/
    static class HashtableEntryObjectPool extends ObjectPool {

        //static long getCount = 0;
        //static long retCount = 0;

        public HashtableEntryObjectPool(int size) {
            super("HashtableEntryPool", size);
        }

        @Override
        public Object createObject() {
            return new HashtableEntry();
        }
    }

    /*************************************************************************
     * ResultObjectPool
     * The class is the pooling of the Result objects
     *************************************************************************/
    static class ResultObjectPool extends ObjectPool {

        public ResultObjectPool(int size) {
            super("HashtableEntryPool", size);
        }

        @Override
        public Object createObject() {
            return new Result();
        }
    }

    /*************************************************************************
     * ValueSetReadCallback
     * The class is used to save the entries during Cache Monitor reading cache ids,
     * dependency id or a template
     *************************************************************************/
    class ValueSetReadCallback implements HashtableAction {
        ValueSet ids;
        ValueSet expiredIds;

        ValueSetReadCallback(ValueSet ids, ValueSet expiredIds) {
            this.ids = ids;
            this.expiredIds = expiredIds;
        }

        public boolean execute(HashtableEntry entry)
                        throws Exception {
            if (expiredIds != null && entry.isExpired()) {
                expiredIds.add(entry.getKey());
            } else {
                ids.add(entry.getKey());
            }
            returnToHashtableEntryPool(entry);
            return true;
        }
    }

    /*************************************************************************
     * HashcodeReadCallback
     * The class is used to calculate the hashcode for cache ids and its value in the disk cache.
     *************************************************************************/
    class HashcodeReadCallback implements HashtableAction { // LI4337-17
        HTODInvalidationBuffer invalidationBuffer = null;
        int totalHashcode = 0;
        List<String> list = new ArrayList<String>(100);
        boolean debug = false;
        int count = 0; // number of entries not expired and not in InvalidationBuffer
        int expiredCount = 0; // number of entries expired or in InvalidationBuffer
        boolean includeValue = false; // indicate to calculate the hashcode for cache value
        boolean isValidHashCode = true; // indicate valid hashcode for cache value

        HashcodeReadCallback(HTODInvalidationBuffer invalidationBuffer, boolean debug, boolean includeValue) {
            this.invalidationBuffer = invalidationBuffer;
            this.debug = debug;
            this.includeValue = includeValue;
            this.totalHashcode = 0;
            this.count = 0;
            this.expiredCount = 0;
            this.isValidHashCode = true;
        }

        // callback walkhash for each entry found from the disk cache
        public boolean execute(HashtableEntry entry) throws Exception {
            if (this.isValidHashCode) {
                Object id = entry.getKey();
                // if entry is not expired or is not in InvalidationBuffer, calculate the hashcode. 
                if (entry.isExpired() == false && invalidationBuffer.contains(id) == false) {
                    int id_hc = id.hashCode();
                    totalHashcode += id_hc;
                    int value_hc = 0;
                    if (this.includeValue) {
                        // old format is used, hashcode for cache value is not available  ==> stop collecting the hashcodes
                        if (entry.isValidHashcodeForValue()) {
                            value_hc = entry.getCacheValueHashcode();
                            totalHashcode += value_hc;
                        } else {
                            this.isValidHashCode = false;
                        }
                    }
                    // if value hashcode is valid, log the cache id and its hashcode 
                    if (this.isValidHashCode) {
                        count++;
                        // if debug is true, save the cache id and its hashcode in the list
                        if (this.debug) {
                            StringBuffer sb = new StringBuffer();
                            sb.append("\nid=");
                            sb.append(id);
                            sb.append(" id_hashcode=");
                            sb.append(id_hc);
                            if (this.includeValue) {
                                sb.append(" value_hashcode=");
                                sb.append(value_hc);
                            }
                            list.add(sb.toString());
                        }
                    }
                } else {
                    this.expiredCount++;
                }
            }
            returnToHashtableEntryPool(entry);
            return this.isValidHashCode;
        }
    }

    /*************************************************************************
     * DynaAction
     * The class is used to save expired entries during scaning the disk
     *************************************************************************/
    class DynaAction implements HashtableAction {
        ValueSet expiredIds;
        int notExpiredCount = 0;
        HTODDynacache htod = null;

        DynaAction(ValueSet expiredIds, HTODDynacache htod) {
            this.expiredIds = expiredIds;
            this.htod = htod;
        }

        public boolean execute(HashtableEntry entry) throws Exception {
            final String methodName = "execute()";
            Object id = entry.getKey();
            Object tid = null;
            if (this.htod.cacheIdsTable != null) {
                tid = cacheIdsTable.get(id);
                if (tid == null) {
                    cacheIdsTable.put(id, id);
                    tid = id;
                }
            } else {
                tid = id;
            }
            if (entry.isExpired()) {
                expiredIds.add(tid);
            } else {
                //populate the EvictionTable when the server is coming up
                if (this.htod.cod.populateEvictionTable && !entry.isAliasId()) {

                    EvictionTableEntry evt = (EvictionTableEntry) ((htod.evictionEntryPool).remove());
                    evt.hashcode = tid.hashCode();
                    evt.expirationTime = entry.expirationTime();
                    if (evt.expirationTime <= 0)
                        evt.expirationTime = Long.MAX_VALUE;
                    evt.size = entry.size();
                    synchronized (this.htod.evictionTableMonitor) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, methodName, "Adding evt during startup, hashcode:" + evt.hashcode + " expirationTime:" + evt.expirationTime + " size:" + evt.size
                                                     + " index:" + evt.index + " EvictionTable.size" + EvictionTable.size());
                        this.htod.EvictionTable.insert(evt);
                    }
                }
                notExpiredCount++;
                if (!entry.isAliasId()) {
                    if (this.htod.cod.diskCachePerformanceLevel == CacheConfig.HIGH) {
                        long expirationTime = entry.expirationTime();
                        if (expirationTime > 0) { // has expiration time?
                            if (!this.htod.invalidationBuffer.contains(tid)) { // is it in invalidation buffer?
                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, methodName, "cacheName=" + this.htod.cacheName + " id=" + tid + " expiration=" + expirationTime);
                                this.htod.cache.addToTimeLimitDaemon(tid, expirationTime, -1); // no, let cache expiration maintain by TimeLimitDaemon
                            }
                        }
                    }
                }
            }
            returnToHashtableEntryPool(entry);
            return true;
        }
    }

    public static class EvictionSizeComparator implements Comparator {

        public int compare(Object evt1, Object evt2) {
            return ((EvictionTableEntry) evt1).size - ((EvictionTableEntry) evt2).size;
        }

        @Override
        public boolean equals(Object cmp) {
            if (this == cmp)
                return true;
            if (cmp == null)
                return false;
            return (getClass() == cmp.getClass());
        }
    }

    static public class EvictionTableEntry {

        public int size;
        public long expirationTime;
        public int hashcode;
        public int index;
        public Object id; // use it for debug

        public boolean lessThan(EvictionTableEntry other) {
            return expirationTime < other.expirationTime;
        }

        public boolean equals(EvictionTableEntry other) {
            return expirationTime == other.expirationTime;
        }

        public boolean lessThanOrEquals(EvictionTableEntry other) {
            return expirationTime <= other.expirationTime;
        }

        public void reset() {
            expirationTime = 0;
            size = 0;
            hashcode = 0;
            index = -1;
            id = ""; // use it for debug
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("EVT: index=");
            sb.append(index);
            sb.append(" expirationTime=");
            sb.append(expirationTime);
            sb.append(" id="); // use it for debug
            sb.append(id);
            sb.append(" hashcode=");
            sb.append(hashcode);
            sb.append(" size=");
            sb.append(size);
            return sb.toString();
        }
    }

    static class EvictionEntryPool extends com.ibm.ws.util.ObjectPool {
        public EvictionEntryPool(int size) {
            super("EvictionEntryPool", size);
        }

        @Override
        public Object createObject() {
            return (new EvictionTableEntry());
        }

        public boolean add(EvictionTableEntry evt) {
            //clean these objects up so they can be garbage collected/reused without us holding on too long
            evt.reset();
            return super.add(evt);
        }
    }

    public static class BinaryHeap {

        public EvictionTableEntry[] heapArray;
        private static final int DEFAULT_SIZE = 1024;
        private int heapSize;

        public BinaryHeap(EvictionTableEntry negInfinity) {
            heapArray = new EvictionTableEntry[DEFAULT_SIZE];
            heapArray[0] = negInfinity;
            heapArray[0].index = 0;
            heapArray[0].expirationTime = Long.MIN_VALUE;
            heapSize = 0;
        }

        //
        // Heap operations
        //

        /**
         * Insert an element E with key E.key into the heap, preserving
         * heap order.
         * 
         * @param el the InvalidationTask element to insert
         * 
         */
        public synchronized void insert(EvictionTableEntry el) {
            int i = ++heapSize;
            growIfNec();
            while (el.lessThan(heapArray[parent(i)])) {
                heapArray[i] = heapArray[parent(i)];
                heapArray[i].index = i;
                i = parent(i);
            }
            // i now indicates this element's proper place
            heapArray[i] = el;
            heapArray[i].index = i;

            //System.out.println("After insert:");
            //printHeap();
        }

        public void printHeap() {
            System.out.println("PRINT HEAP");
            for (int i = 0; i <= size(); i++) {
                System.out.println("*** evt:" + heapArray[i]);
            }
        }

        /**
         * Return a reference to the minimum element in the heap,
         * without removing it.
         */

        public EvictionTableEntry minimum() {
            if (isEmpty()) {
                return null;
            }
            return heapArray[1];
        } // minimum

        public synchronized void clear() {
            for (int i = 1; i <= heapSize; i++)
                heapArray[i] = null;
            heapSize = 0;
        }

        /**
         * Return a reference to the minimum element in the heap,
         * removing it from the heap.
         */
        public synchronized EvictionTableEntry deleteMin() {
            EvictionTableEntry min;
            if (isEmpty())
                min = null;
            else {
                // Pull out the last element
                EvictionTableEntry last = heapArray[heapSize];
                heapArray[heapSize--] = null;
                if (isEmpty())
                    min = last; // last was the only element
                else {
                    // Grab minimum from the top, put last back there, and re-heapify
                    min = heapArray[1];
                    heapArray[1] = last;
                    heapArray[1].index = 1;
                    heapify(1);
                }
            }
            return min;
        }

        public synchronized EvictionTableEntry findAndRemove(long expirationTime, int hashcode, int size) {
            //System.out.println("findAndRemove exp:"+ expirationTime+" hashcode:"+ hashcode+" size:"+size);
            EvictionTableEntry he = null;
            int i;
            for (i = 1; i <= heapSize; i++) {
                he = heapArray[i];
                //  System.out.println("i:"+ i+" exp:"+he.expirationTime+" hashcode:"+he.hashcode+" size:"+he.size);
                if (he.expirationTime == expirationTime) {
                    if (he.size == size && he.hashcode == hashcode)
                        break;
                }

            }
            if (i > heapSize)
                he = null;
            else {
                //if (tc.isDebugEnabled()) Tr.debug(tc, methodName, "cacheName=" + this.cacheName + " found exp=" + expirationTime + " hashcode=" + hashcode+ " size=" + size + " on index=" + i);
                //System.out.println("i:"+ i);
                heapArray[i] = heapArray[0];
                heapArray[i].index = i;
                percolateUp(i);
                deleteMin();
                heapArray[0].index = 0;
            }
            return he;
        }

        /**
         * Deletes the item with the given key from the heap.
         * 
         * @param i the index, or key, of the item to delete
         */
        public synchronized void delete(EvictionTableEntry el) {
            int i = findKey(el);
            if (i == -1)
                throw new java.lang.IllegalArgumentException();
            heapArray[i] = heapArray[0]; // heapArray[0] holds negative infinity
            heapArray[i].index = i;
            //System.out.println("Print Heap before precolate:");
            //printHeap();
            percolateUp(i);
            //System.out.println("printHeap before deleteMin and after precolate:");
            //printHeap();
            deleteMin();
            heapArray[0].index = 0;
        }

        private int findKey(EvictionTableEntry c) {
            return c.index;
        }

        /**
         * Reestablish heap property with the precondition
         * that the element at i is smaller than it's parent.
         * 
         * @param i the index which violates heap property
         */
        private void percolateUp(int i) {
            while (heapArray[i].lessThan(heapArray[parent(i)])) {
                // swap element i with it's parent
                int j = parent(i);
                EvictionTableEntry c = heapArray[j];
                heapArray[j] = heapArray[i];
                heapArray[j].index = j;
                heapArray[i] = c;
                heapArray[i].index = i;
                int prev = i;
                i = j;
                // need to call heapify at this point on
                // the item we just swapped
                heapify(prev);
            }
        }

        /**
         * Heapify assumes that given an index i into the heap, left(i)
         * and right(i) are heaps, but i may violate the heap property.
         * It effectively percolates i down the heap until the heap property
         * is reestablished. <p>
         * 
         * @param index the index of the heap array which violates the heap property.
         */
        private void heapify(int i) {
            EvictionTableEntry tmp = heapArray[i];
            int l;
            for (; left(i) <= heapSize; i = l) {
                l = left(i); // left child of current node
                // if the left child is not the end of the heap, and
                // the right child is less than left child, advance
                // to the right child.
                if (l < heapSize && heapArray[right(i)].lessThan(heapArray[l]))
                    l++;

                // if the current, lesser child is less than the item we are
                // heapifying for, then move that child into the
                // current slot, else we're done.
                if (heapArray[l].lessThan(tmp)) {
                    heapArray[i] = heapArray[l];
                    heapArray[i].index = i;
                } else
                    break;
            }
            heapArray[i] = tmp;
            heapArray[i].index = i;
        }

        public boolean isEmpty() {
            return heapSize == 0;
        }

        public int size() {
            return heapSize;
        }

        private static int parent(int i) {
            return i / 2;
        }

        private static int left(int i) {
            return 2 * i;
        }

        private static int right(int i) {
            return 2 * i + 1;
        }

        /**
         * Private method that doubles the heap array if full.
         */
        private void growIfNec() {
            if ((heapSize + 1) == heapArray.length) {
                EvictionTableEntry[] oldHeap = heapArray;
                heapArray = new EvictionTableEntry[heapSize * 2];
                System.arraycopy(oldHeap, 0, heapArray, 0, oldHeap.length);
            }
        }
    }

    public static class DepSizeComparator implements Comparator {

        public DepSizeComparator() {}

        public int compare(Object evt1, Object evt2) {
            return ((DepEntry) evt1).size - ((DepEntry) evt2).size;
        }
    }

    static public class DepEntry {
        public int size;
        public Object id;

        public DepEntry(Object id, int size) {
            this.id = id;
            this.size = size;
        }
    }

}
