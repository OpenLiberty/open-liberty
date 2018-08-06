/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014, 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.anno.targets.cache.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.anno.targets.cache.TargetCache_ExternalConstants;
import com.ibm.ws.anno.targets.cache.TargetCache_Options;
import com.ibm.ws.anno.targets.cache.TargetCache_ParseError;
import com.ibm.ws.anno.targets.cache.TargetCache_Readable;
import com.ibm.ws.anno.targets.cache.TargetCache_Reader;
import com.ibm.ws.anno.targets.cache.internal.TargetCacheImpl_Utils.PrefixWidget;
import com.ibm.ws.anno.targets.internal.TargetsTableClassesMultiImpl;
import com.ibm.ws.anno.targets.internal.TargetsTableContainersImpl;
import com.ibm.ws.anno.targets.internal.TargetsTableImpl;
import com.ibm.ws.anno.util.internal.UtilImpl_InternMap;
import com.ibm.ws.anno.util.internal.UtilImpl_PoolExecutor;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;

public class TargetCacheImpl_DataMod extends TargetCacheImpl_DataBase {

    public static final String CLASS_NAME = TargetCacheImpl_DataMod.class.getSimpleName();

    //

    public TargetCacheImpl_DataMod(
        TargetCacheImpl_Factory factory,
        String modName, String e_modName, File modDir) {

        super(factory,
              modName, e_modName, modDir,
              TargetCache_ExternalConstants.CON_PREFIX);

        int writeThreads = this.cacheOptions.getWriteThreads();

        if ( writeThreads == 1 ) {
            this.writePool = null;

        } else {
            int corePoolSize = 0;

            int maxPoolSize;
            if ( writeThreads == TargetCache_Options.WRITE_THREADS_UNBOUNDED) {
                maxPoolSize = TargetCache_Options.WRITE_THREADS_MAX;
            } else if ( writeThreads > TargetCache_Options.WRITE_THREADS_MAX ) {
                maxPoolSize = TargetCache_Options.WRITE_THREADS_MAX;
            } else {
                maxPoolSize = writeThreads;
            }

            this.writePool = UtilImpl_PoolExecutor.createNonBlockingExecutor(corePoolSize, maxPoolSize);
        }

        this.activeConsLock = new ActiveConsLock();
        this.activeCons = new WeakHashMap<String, TargetCacheImpl_DataCon>();

        this.loadConStore(activeCons);
    }

    public void loadConStore(final Map<String, TargetCacheImpl_DataCon> useConStore) {
        if ( isDisabled() ) {
            return;
        }

        TargetCacheImpl_Utils.PrefixListWidget conListWidget =
            new TargetCacheImpl_Utils.PrefixListWidget( getChildPrefixWidget() ) {
                @Override
                public void storeChild(String conName, String e_conName, File conDir) {
                    TargetCacheImpl_DataCon conData = createConData(conName, e_conName, conDir);
                    useConStore.put(conName, conData);
                }
        };

        conListWidget.storeParent( getDataDir() );
    }

    //

    @Trivial
    public TargetCacheImpl_DataCon createConData(File conDir) {
        PrefixWidget usePrefixWidget = getChildPrefixWidget();

        String conDirName = conDir.getName();
        String e_conName = usePrefixWidget.e_removePrefix(conDirName);
        String conName = decode(e_conName);

        return createConData(conName, e_conName, conDir);
    }

    @Trivial
    public TargetCacheImpl_DataCon createConData(String conName) {
        String e_conName = encode(conName);
        return createConData( conName, e_conName, e_getConDir(e_conName) );
    }

    @Trivial
    public File e_getConDir(String e_conName) {
        return getDataFile( e_addChildPrefix(e_conName) );
    }

    @Trivial
    public TargetCacheImpl_DataCon createConData(String conName, String e_conName, File conDir) {
        return getFactory().createConData(this, conName, e_conName, conDir);
    }

    //

    protected volatile File containersFile;

    public File getContainersFile() {
        if ( containersFile == null ) {
            synchronized(this) {
                if ( containersFile == null ) {
                    containersFile = getDataFile(TargetCache_ExternalConstants.CONTAINERS_NAME);
                }
            }
        }
        return containersFile;
    }

    public boolean hasContainersTable() {
        if ( isDisabled() ) {
            return false;
        }

        File useContainersFile = getContainersFile();
        synchronized( useContainersFile ) {
            return exists(useContainersFile);
        }
    }

    public boolean readContainerTable(TargetsTableContainersImpl containerTable) {
    	boolean didRead;

    	long readStart = System.nanoTime();

        File useContainersFile = getContainersFile();
        synchronized( useContainersFile ) {
            didRead = super.read(containerTable, useContainersFile);
        }

        @SuppressWarnings("unused")
        long readDuration = addReadTime(readStart, "Read Containers");

        return didRead;
    }

    public void writeContainersTable(TargetsTableContainersImpl containerTable) {
        String methodName = "writeContainersTable";

        File useContainersFile = getContainersFile();

        if ( !shouldWrite(useContainersFile, "Containers list") ) {
            return;
        }

        long writeStart = System.nanoTime();

        synchronized(useContainersFile) {
            try {
                createWriter(useContainersFile).write(containerTable);
            } catch ( IOException e ) {
                // CWWKC00??W
                logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_TARGETS_CACHE_EXCEPTION [ {0} ]", e);
                logger.logp(Level.WARNING, CLASS_NAME, methodName, "Cache error", e);
            }

            for ( String containerName : getActiveConNames() ) {
                if ( !containerTable.containsName(containerName) ) {
                    removeConData(containerName);
                }
            }
        }

        @SuppressWarnings("unused")
        long writeDuration = addWriteTime(writeStart, "Write containers");
    }

    //

    protected void removeConData(String containerName) {
        TargetCacheImpl_DataCon conData = getActiveCons().remove(containerName);
        if ( conData != null ) {
            conData.removeStorage();
        }
    }

    //

    private class ActiveConsLock {
        // EMPTY
    }
    private final ActiveConsLock activeConsLock;
    private final WeakHashMap<String, TargetCacheImpl_DataCon> activeCons;

    @Trivial
    protected Map<String, TargetCacheImpl_DataCon> getActiveCons() {
        return activeCons;
    }

    public long getContainerReadTime() {
    	long containerReadTime = 0L;

    	for ( TargetCacheImpl_DataCon activeCon : activeCons.values() ) {
    		containerReadTime += activeCon.getReadTime();
    	}

    	return containerReadTime;
    }

    public long getContainerWriteTime() {
    	long containerWriteTime = 0L;

    	for ( TargetCacheImpl_DataCon activeCon : activeCons.values() ) {
    		containerWriteTime += activeCon.getWriteTime();
    	}

    	return containerWriteTime;
    }

    @Trivial
    public Set<String> getActiveConNames() {
        synchronized ( activeConsLock ) {
            return getActiveCons().keySet();
        }
    }

    public boolean hasActiveConData(String conName) {
        synchronized ( activeConsLock ) {
            return getActiveCons().containsKey(conName);
        }
    }

    public TargetCacheImpl_DataCon getActiveCon(String conName) {
        synchronized ( activeConsLock ) {
            return getActiveCons().get(conName);
        }
    }
    
    public TargetCacheImpl_DataCon putActiveCon(String conName) {
        TargetCacheImpl_DataCon activeConData = createConData(conName);

        synchronized( activeConsLock ) {
            getActiveCons().put(conName, activeConData);
        }

        return activeConData;
    }

    public TargetCacheImpl_DataCon getActiveConForcing(String conName) {
        synchronized ( activeConsLock ) {
            Map<String, TargetCacheImpl_DataCon> useActiveCons = getActiveCons();

            TargetCacheImpl_DataCon activeCon = useActiveCons.get(conName);

            if ( activeCon == null ) {
                activeCon = createConData(conName);
                useActiveCons.put(conName, activeCon);
            }

            return activeCon;
        }
    }

    //

    private static class SeedLock {
        // EMPTY
    }
    private final SeedLock seedLock = new SeedLock();
    private volatile TargetCacheImpl_DataCon seedCon;

    private static class PartialLock {
        // EMPTY
    }
    private final PartialLock partialLock = new PartialLock();
    private volatile TargetCacheImpl_DataCon partialCon;

    private static class ExcludedLock {
        // EMPTY
    }
    private final ExcludedLock excludedLock = new ExcludedLock();
    private volatile TargetCacheImpl_DataCon excludedCon;

    private static class ExternalLock {
        // EMPTY
    }
    private final Object externalLock = new ExternalLock();
    private volatile TargetCacheImpl_DataCon externalCon;

    private static final String[] RESULT_NAMES = new String[ScanPolicy.values().length];

    static {
        RESULT_NAMES[ ScanPolicy.SEED.ordinal()     ] = TargetCache_ExternalConstants.SEED_RESULT_NAME;
        RESULT_NAMES[ ScanPolicy.PARTIAL.ordinal()  ] = TargetCache_ExternalConstants.PARTIAL_RESULT_NAME;
        RESULT_NAMES[ ScanPolicy.EXCLUDED.ordinal() ] = TargetCache_ExternalConstants.EXCLUDED_RESULT_NAME;
        RESULT_NAMES[ ScanPolicy.EXTERNAL.ordinal() ] = TargetCache_ExternalConstants.EXTERNAL_RESULT_NAME;
    }

    @Trivial
    public static String getResultName(ScanPolicy scanPolicy) {
        return RESULT_NAMES[ scanPolicy.ordinal() ];
    }

    public TargetCacheImpl_DataCon getResultConData(ScanPolicy scanPolicy) {
        String fileName = getResultName(scanPolicy);

        if ( scanPolicy == ScanPolicy.SEED ) {
            if ( seedCon == null ) {
                synchronized ( seedLock ) {
                    seedCon = createConData( getDataFile(fileName) );
                }
            }
            return seedCon;

        } else if ( scanPolicy == ScanPolicy.PARTIAL ) {
            if ( partialCon == null ) {
                synchronized ( partialLock ) {
                    partialCon = createConData( getDataFile(fileName) );
                }
            }
            return partialCon;

        } else if ( scanPolicy == ScanPolicy.EXCLUDED ) {
            if ( excludedCon == null ) {
                synchronized ( excludedLock ) {
                    excludedCon = createConData( getDataFile(fileName) );
                }
            }
            return excludedCon;

        } else if ( scanPolicy == ScanPolicy.EXTERNAL ) {
            if ( externalCon == null ) {
                synchronized ( externalLock ) {
                    externalCon = createConData( getDataFile(fileName) );
                }
            }
            return externalCon;

        } else {
            throw new IllegalArgumentException("Unknown policy [ " + scanPolicy + " ]");
        }
    }

    public boolean hasResultConData(ScanPolicy scanPolicy) {
        if ( isDisabled() ) {
            return false;
        }

        return getResultConData(scanPolicy).exists();
    }

    public boolean readResultData(ScanPolicy scanPolicy, TargetsTableImpl resultData) {
        return getResultConData(scanPolicy).read(resultData);
    }

    public void writeResultData(ScanPolicy scanPolicy, TargetsTableImpl resultData) {
        getResultConData(scanPolicy).write(resultData);
    }

    //

    private volatile File unresolvedRefsFile;

    public File getUnresolvedRefsFile() {
        if ( unresolvedRefsFile == null ) {
            synchronized (this) {
                if ( unresolvedRefsFile == null ) {
                    unresolvedRefsFile = getDataFile(TargetCache_ExternalConstants.UNRESOLVED_REFS_NAME);
                }
            }
        }
        return unresolvedRefsFile;
    }

    public boolean hasUnresolvedRefs() {
        return ( !isDisabled() && exists( getUnresolvedRefsFile() ) );
    }

    public List<TargetCache_ParseError> basicReadUnresolvedRefs(UtilImpl_InternMap classNameInternMap, Set<String> i_unresolvedClassNames)
        throws FileNotFoundException, IOException {

        return createReader( getUnresolvedRefsFile() ).readUnresolvedRefs(classNameInternMap, i_unresolvedClassNames);
        // 'createReader' throws IOException
        // 'read' throws IOException
    }

    public boolean readUnresolvedRefs(final UtilImpl_InternMap classNameInternMap,
                                      final Set<String> i_unresolvedClassNames) {

        TargetCache_Readable refsReadable = new TargetCache_Readable() {
            @Override
            public List<TargetCache_ParseError> readUsing(TargetCache_Reader reader) throws IOException {
                return basicReadUnresolvedRefs(classNameInternMap, i_unresolvedClassNames);
            }
        };

        long readStart = System.nanoTime();

        boolean didRead = super.read( refsReadable, getUnresolvedRefsFile() );

        @SuppressWarnings("unused")
        long readDuration = addReadTime(readStart, "Read Unresolved Refs");

        return didRead;
    }

    public void writeUnresolvedRefs(final Set<String> unresolvedClassNames) {
        final File useUnresolvedRefsFile = getUnresolvedRefsFile();

        if ( !shouldWrite(useUnresolvedRefsFile, "Unresolved class references") ) {
            return;
        }

        ScheduleCallback writer = new ScheduleCallback() {
            @Override
            public void execute() {
                String methodName = "writeUnresolvedRefs.execute";

                long writeStart = System.nanoTime();

                synchronized(useUnresolvedRefsFile) {
                    try {
                        createWriter(useUnresolvedRefsFile).writeUnresolvedRefs(unresolvedClassNames); // throws IOException
                    } catch ( IOException e ) {
                        // CWWKC00??W
                        logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_TARGETS_CACHE_EXCEPTION [ {0} ]", e);
                        logger.logp(Level.WARNING, CLASS_NAME, methodName, "Cache error", e);
                    }
                }

                @SuppressWarnings("unused")
                long writeDuration = addWriteTime(writeStart, "Write Unresolved Refs");
            }
        };

        scheduleWrite(writer);
    }

    //

    protected volatile File resolvedRefsFile;

    public File getResolvedRefsFile() {
        if ( resolvedRefsFile == null ) {
            synchronized(this ) {
                if ( resolvedRefsFile == null ) {
                    resolvedRefsFile = getDataFile(TargetCache_ExternalConstants.RESOLVED_REFS_NAME);
                }
            }
        }
        return resolvedRefsFile;
    }

    public boolean hasResolvedRefs() {
        return ( !isDisabled() && exists( getResolvedRefsFile() ) );
    }

    public List<TargetCache_ParseError> basicReadResolvedRefs(UtilImpl_InternMap classNameInternMap,
                                                              Set<String> i_resolvedClassNames)
        throws FileNotFoundException, IOException {

        return createReader( getResolvedRefsFile() ).readResolvedRefs(classNameInternMap, i_resolvedClassNames);
        // 'createReader' throws IOException
        // 'read' throws IOException
    }

    public boolean readResolvedRefs(final UtilImpl_InternMap classNameInternMap,
                                    final Set<String> i_resolvedClassNames) {

        TargetCache_Readable refsReadable = new TargetCache_Readable() {
            @Override
            public List<TargetCache_ParseError> readUsing(TargetCache_Reader reader) throws IOException {
                return basicReadResolvedRefs(classNameInternMap, i_resolvedClassNames);
            }
        };

        long readStart = System.nanoTime();

        boolean didRead = super.read( refsReadable, getResolvedRefsFile() );

        @SuppressWarnings("unused")
        long readDuration = addReadTime(readStart, "Read Resolved Refs");

        return didRead;
    }

    public void writeResolvedRefs(final Set<String> resolvedClassNames) {
        final File useResolvedRefsFile = getResolvedRefsFile();

        if ( !shouldWrite(useResolvedRefsFile, "Resolved class references") ) {
            return;
        }

        ScheduleCallback writer = new ScheduleCallback() {
            @Override           
            public void execute() {
                String methodName = "writeResolvedRefs.execute";

                long writeStart = System.nanoTime();

                synchronized ( useResolvedRefsFile ) {
                    try {
                        createWriter(useResolvedRefsFile).writeResolvedRefs(resolvedClassNames); // throws IOException
                    } catch ( IOException e ) {
                        // CWWKC00??W
                        logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_TARGETS_CACHE_EXCEPTION [ {0} ]", e);
                        logger.logp(Level.WARNING, CLASS_NAME, methodName, "Cache error", e);
                    }
                }

                @SuppressWarnings("unused")
                long writeDuration = addWriteTime(writeStart, "Write Resolved Refs");
            }
        };

        scheduleWrite(writer);
    }

    //

    protected volatile File classRefsFile;

    public File getClassRefsFile() {
        if ( classRefsFile == null ) {
            synchronized(this) {
                if ( classRefsFile == null ) {
                    classRefsFile = getDataFile(TargetCache_ExternalConstants.CLASS_REFS_NAME);
                }
            }
        }
        return classRefsFile;
    }

    public boolean hasClassRefs() {
        return ( !isDisabled() && exists( getClassRefsFile() ) );
    }

    public boolean readClassRefs(TargetsTableClassesMultiImpl classesTable) {
    	boolean didRead;

    	long readStart = System.nanoTime();

        File useClassRefsFile = getClassRefsFile();
        synchronized (useClassRefsFile ) {
            didRead = read(classesTable, useClassRefsFile);
        }

        @SuppressWarnings("unused")
        long readDuration = addReadTime(readStart, "Read Class Refs");

        return didRead;
    }

    public void writeClassRefs(final TargetsTableClassesMultiImpl classesTable) {
        final File useClassRefsFile = getClassRefsFile();

        if ( !shouldWrite(useClassRefsFile, "Class relationship table") ) {
            return;
        }

        ScheduleCallback writer = new ScheduleCallback() {
            @Override
            public void execute() {
                String methodName = "writeClassRefs.execute";

                long writeStart = System.nanoTime();

                synchronized( useClassRefsFile ) {
                    try {
                        // See the comment on 'mergeClasses': This must be synchronized
                        // with updates to the class table which occur in 
                        // TargetsScannerImpl_Overall.validExternal'.
                        synchronized ( classesTable ) {
                            createWriter(useClassRefsFile).write(classesTable); // 'write' throws IOException
                        }
                    } catch ( IOException e ) {
                        // CWWKC00??W
                        logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_TARGETS_CACHE_EXCEPTION [ {0} ]", e);
                        logger.logp(Level.WARNING, CLASS_NAME, methodName, "Cache error", e);
                    }
                }

                @SuppressWarnings("unused")
                long writeDuration = addWriteTime(writeStart, "Write Class Refs");
            }
        };

        scheduleWrite(writer);
    }

    //

    public boolean readConData(String conName, TargetsTableImpl targetsData) {
        TargetCacheImpl_DataCon conData = getActiveCon(conName);
        if ( conData == null ) {
            return false;
        }

        return conData.read(targetsData);
    }

    public void writeConData(String conName, TargetsTableImpl targetsData) {
        getActiveConForcing(conName).write(targetsData);
    }

    // Handle writes at the module level and below at the module level.
    //
    // Writes are simple: Most can be simply spawned and allowed to complete
    // in their own time.
    //
    // Reads require additional coordination (joins for the results) and are
    // handled by the calling code.

    protected final UtilImpl_PoolExecutor writePool;

    @Trivial
    protected UtilImpl_PoolExecutor getWritePool() {
        return writePool;
    }

    protected void scheduleWrite(final ScheduleCallback writer) {
        final Throwable scheduler = new Throwable("ModData [ " + getName() + " ] [ " + e_getName() + " ]");

        UtilImpl_PoolExecutor useWritePool = getWritePool();
        if ( useWritePool == null ) {
            writer.execute();
        } else {
            Runnable writeRunner = new Runnable() {
                @Override
                public void run() {
                    String methodName = "scheduleWrite.run";
                    try {
                        // System.out.println("BEGIN write [ " + writer + " ]");
                        writer.execute();
                        // System.out.println("END write [ " + writer + " ]");

                    } catch ( RuntimeException e ) {
                        // Capture and display any exception from the spawned writer thread.
                        // Without this added step information about the spawning thread is
                        // lost, making debugging writer problems very difficult.

                        logger.logp(Level.WARNING, CLASS_NAME, methodName, "Caught Asynchronous exception [ {0} ]", e);
                        logger.logp(Level.WARNING, CLASS_NAME, methodName, "Scheduler", scheduler);
                        logger.logp(Level.WARNING, CLASS_NAME, methodName, "Synchronization error", e);

                        throw e;
                    }
                }
            };
            useWritePool.execute(writeRunner);
        }
    }

    //

    protected interface ScheduleCallback {
        void execute();
    }
}
