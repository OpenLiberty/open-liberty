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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.anno.targets.cache.TargetCache_ExternalConstants;
import com.ibm.ws.anno.targets.cache.TargetCache_Options;
import com.ibm.ws.anno.targets.cache.TargetCache_ParseError;
import com.ibm.ws.anno.targets.cache.TargetCache_Readable;
import com.ibm.ws.anno.targets.cache.TargetCache_Reader;
import com.ibm.ws.anno.targets.internal.TargetsTableClassesMultiImpl;
import com.ibm.ws.anno.targets.internal.TargetsTableContainersImpl;
import com.ibm.ws.anno.targets.internal.TargetsTableImpl;
import com.ibm.ws.anno.util.internal.UtilImpl_InternMap;
import com.ibm.ws.anno.util.internal.UtilImpl_PoolExecutor;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;

/**
 * Annotation cache data for a single module.
 *
 * Each module has its own directory, relative to the directory of the
 * enclosing application, and named using the standard pattern:
 * <code>
 *     appFolder + MOD_PREFIX + encode(modName)
 * </code>
 *
 * Module information consists of a list of containers (including policy settings for
 * each container), the actual data of the containers (which is held by the enclosing
 * application and which is shared between modules), resolved class references,
 * unresolved class references, and four result containers (for SEED, PARTIAL,
 * EXCLUDED, and EXTERNAL results).
 *
 * The list of containers, resolved class references, and unresolved class references
 * are stored each in their own file.
 *
 * Storage of non-result container data is managed by the enclosing application, with the
 * container data stored in sub-directories of the application directory.
 *
 * Storage of result container is managed at the module level, with the result container
 * data stored in sub-directories of the module.
 *
 * Module data is held weakly by the enclosing application.
 *
 * Container data of a module is held strongly by the module.
 */
public class TargetCacheImpl_DataMod extends TargetCacheImpl_DataBase {
    public static final String CLASS_NAME = TargetCacheImpl_DataMod.class.getSimpleName();

    //

    public TargetCacheImpl_DataMod(
        TargetCacheImpl_DataApp app,
        String modName, String e_modName, File modDir) {

        super( app.getFactory(), modName, e_modName, modDir );

        this.app = app;

        //

        this.cons = new HashMap<String, TargetCacheImpl_DataCon>();
        this.containersFile = getDataFile(TargetCache_ExternalConstants.CONTAINERS_NAME);

        this.seedCon = null;
        this.partialCon = null;
        this.excludedCon = null;
        this.externalCon = null;

        this.unresolvedRefsFile = getDataFile(TargetCache_ExternalConstants.UNRESOLVED_REFS_NAME);
        this.resolvedRefsFile = getDataFile(TargetCache_ExternalConstants.RESOLVED_REFS_NAME);
        this.classRefsFile = getDataFile(TargetCache_ExternalConstants.CLASS_REFS_NAME);

        //

        int writeThreads = this.cacheOptions.getWriteThreads();

        if ( !app.isNamed() ) {
            this.writePool = null;

        } else if ( writeThreads == 1 ) {
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
    }

    //

    private final TargetCacheImpl_DataApp app;

    @Trivial
    public TargetCacheImpl_DataApp getApp() {
    	return app;
    }

    //

    protected final File containersFile;

    public File getContainersFile() {
        return containersFile;
    }

    public boolean hasContainersTable() {
        return exists( getContainersFile() );
    }

    public boolean readContainerTable(TargetsTableContainersImpl containerTable) {
        boolean didRead;

        long readStart = System.nanoTime();

        didRead = super.read( containerTable, getContainersFile() );

        @SuppressWarnings("unused")
        long readDuration = addReadTime(readStart, "Read Containers");

        return didRead;
    }

    public void writeContainersTable(TargetsTableContainersImpl containerTable) {
        String methodName = "writeContainersTable";

        long writeStart = System.nanoTime();

        try {
            createWriter( getContainersFile() ).write(containerTable);
        } catch ( IOException e ) {
            // CWWKC00??W
            logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_TARGETS_CACHE_EXCEPTION [ {0} ]", e);
            logger.logp(Level.WARNING, CLASS_NAME, methodName, "Cache error", e);
        }

        @SuppressWarnings("unused")
        long writeDuration = addWriteTime(writeStart, "Write containers");
    }

    //

    private final Map<String, TargetCacheImpl_DataCon> cons;

    @Trivial
    protected Map<String, TargetCacheImpl_DataCon> getCons() {
        return cons;
    }

    public TargetCacheImpl_DataCon getConForcing(String conPath) {
        Map<String, TargetCacheImpl_DataCon> useCons = getCons();

        TargetCacheImpl_DataCon con = useCons.get(conPath);
        if ( con == null ) {
            con = getApp().getConForcing(conPath);
            useCons.put(conPath, con);
        }

        return con;
    }

    //

    public long getContainerReadTime() {
    	long containerReadTime = 0L;

    	for ( TargetCacheImpl_DataCon con : getCons().values() ) {
    		containerReadTime += con.getReadTime();
    	}

    	return containerReadTime;
    }

    public long getContainerWriteTime() {
    	long containerWriteTime = 0L;

    	for ( TargetCacheImpl_DataCon con : getCons().values() ) {
    		containerWriteTime += con.getWriteTime();
    	}

    	return containerWriteTime;
    }

    //

    @Trivial
    public TargetCacheImpl_DataCon createConData(File conDir) {
        String conDirName = conDir.getName();
        String e_conPath = e_removeConPrefix(conDirName);
        String conPath = decode(e_conPath);

        return createConData(conPath, e_conPath, conDir);
    }

    @Trivial
    public TargetCacheImpl_DataCon createConData(String conPath) {
    	String e_conPath = encode(conPath);
        return createConData( conPath, e_conPath, e_getConDir(e_conPath) );
    }

    @Trivial
    public File e_getConDir(String e_conPath) {
        return getDataFile( e_addConPrefix(e_conPath) );
    }

    @Trivial
    public TargetCacheImpl_DataCon createConData(String conPath, String e_conPath, File conDir) {
        return getFactory().createConData(this, conPath, e_conPath, conDir);
    }

    //

    private TargetCacheImpl_DataCon seedCon;
    private TargetCacheImpl_DataCon partialCon;
    private TargetCacheImpl_DataCon excludedCon;
    private TargetCacheImpl_DataCon externalCon;

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

    public TargetCacheImpl_DataCon getResultCon(ScanPolicy scanPolicy) {
        String fileName = getResultName(scanPolicy);

        if ( scanPolicy == ScanPolicy.SEED ) {
            if ( seedCon == null ) {
                seedCon = createConData( getDataFile(fileName) );
            }
            return seedCon;

        } else if ( scanPolicy == ScanPolicy.PARTIAL ) {
            if ( partialCon == null ) {
                partialCon = createConData( getDataFile(fileName) );
            }
            return partialCon;

        } else if ( scanPolicy == ScanPolicy.EXCLUDED ) {
            if ( excludedCon == null ) {
                excludedCon = createConData( getDataFile(fileName) );
            }
            return excludedCon;

        } else if ( scanPolicy == ScanPolicy.EXTERNAL ) {
            if ( externalCon == null ) {
                externalCon = createConData( getDataFile(fileName) );
            }
            return externalCon;

        } else {
            throw new IllegalArgumentException("Unknown policy [ " + scanPolicy + " ]");
        }
    }

    public boolean hasResultCon(ScanPolicy scanPolicy) {
        return getResultCon(scanPolicy).exists();
    }

    public boolean readResultCon(ScanPolicy scanPolicy, TargetsTableImpl resultData) {
        return getResultCon(scanPolicy).read(resultData);
    }

    public void writeResultCon(ScanPolicy scanPolicy, TargetsTableImpl resultData) {
        getResultCon(scanPolicy).write(this, resultData);
    }

    //

    private final File unresolvedRefsFile;

    public File getUnresolvedRefsFile() {
        return unresolvedRefsFile;
    }

    public boolean hasUnresolvedRefs() {
        return ( exists( getUnresolvedRefsFile() ) );
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
        if ( !shouldWrite("Unresolved class references") ) {
            return;
        }

        ScheduleCallback writer = new ScheduleCallback() {
            @Override
            public void execute() {
                String methodName = "writeUnresolvedRefs.execute";

                long writeStart = System.nanoTime();

                try {
                    createWriter( getUnresolvedRefsFile() ).writeUnresolvedRefs(unresolvedClassNames); // throws IOException
                } catch ( IOException e ) {
                    // CWWKC00??W
                    logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_TARGETS_CACHE_EXCEPTION [ {0} ]", e);
                    logger.logp(Level.WARNING, CLASS_NAME, methodName, "Cache error", e);
                }

                @SuppressWarnings("unused")
                long writeDuration = addWriteTime(writeStart, "Write Unresolved Refs");
            }
        };

        scheduleWrite(writer);
    }

    //

    protected final File resolvedRefsFile;

    public File getResolvedRefsFile() {
        return resolvedRefsFile;
    }

    public boolean hasResolvedRefs() {
        return ( exists( getResolvedRefsFile() ) );
    }

    public List<TargetCache_ParseError> basicReadResolvedRefs(
        UtilImpl_InternMap classNameInternMap,
        Set<String> i_resolvedClassNames) throws FileNotFoundException, IOException {

        return createReader( getResolvedRefsFile() ).readResolvedRefs(classNameInternMap, i_resolvedClassNames);
        // 'createReader' throws IOException
        // 'read' throws IOException
    }

    public boolean readResolvedRefs(
        final UtilImpl_InternMap classNameInternMap,
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
        if ( !shouldWrite("Resolved class references") ) {
            return;
        }

        ScheduleCallback writer = new ScheduleCallback() {
            @Override           
            public void execute() {
                String methodName = "writeResolvedRefs.execute";

                long writeStart = System.nanoTime();

                try {
                    createWriter( getResolvedRefsFile() ).writeResolvedRefs(resolvedClassNames); // throws IOException
                } catch ( IOException e ) {
                    // CWWKC00??W
                    logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_TARGETS_CACHE_EXCEPTION [ {0} ]", e);
                    logger.logp(Level.WARNING, CLASS_NAME, methodName, "Cache error", e);
                }

                @SuppressWarnings("unused")
                long writeDuration = addWriteTime(writeStart, "Write Resolved Refs");
            }
        };

        scheduleWrite(writer);
    }

    //

    protected final File classRefsFile;

    public File getClassRefsFile() {
        return classRefsFile;
    }

    public boolean hasClassRefs() {
        return ( exists( getClassRefsFile() ) );
    }

    public boolean readClassRefs(TargetsTableClassesMultiImpl classesTable) {
    	long readStart = System.nanoTime();

        boolean didRead = read( classesTable, getClassRefsFile() );

        @SuppressWarnings("unused")
        long readDuration = addReadTime(readStart, "Read Class Refs");

        return didRead;
    }

    public void writeClassRefs(final TargetsTableClassesMultiImpl classesTable) {
        if ( !shouldWrite("Class relationship table") ) {
            return;
        }

        ScheduleCallback writer = new ScheduleCallback() {
            @Override
            public void execute() {
                String methodName = "writeClassRefs.execute";

                long writeStart = System.nanoTime();

                try {
                    // See the comment on 'mergeClasses': This must be synchronized
                    // with updates to the class table which occur in 
                    // TargetsScannerImpl_Overall.validExternal'.
                    synchronized ( classesTable ) {
                        createWriter( getClassRefsFile() ).write(classesTable); // 'write' throws IOException
                    }
                } catch ( IOException e ) {
                    // CWWKC00??W
                    logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_TARGETS_CACHE_EXCEPTION [ {0} ]", e);
                    logger.logp(Level.WARNING, CLASS_NAME, methodName, "Cache error", e);
                }

                @SuppressWarnings("unused")
                long writeDuration = addWriteTime(writeStart, "Write Class Refs");
            }
        };

        scheduleWrite(writer);
    }

    //

    @Trivial
    @Override
    public boolean shouldRead(String inputDescription) {
        return getApp().shouldRead(inputDescription);
    }

    @Trivial
    @Override
    public boolean shouldWrite(String outputDescription) {
        return getApp().shouldWrite(outputDescription);
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
