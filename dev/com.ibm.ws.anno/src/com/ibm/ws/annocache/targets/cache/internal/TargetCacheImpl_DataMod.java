/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.targets.cache.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.annocache.targets.cache.TargetCache_ParseError;
import com.ibm.ws.annocache.targets.cache.TargetCache_Readable;
import com.ibm.ws.annocache.targets.cache.TargetCache_Reader;
import com.ibm.ws.annocache.targets.internal.TargetsTableClassesMultiImpl;
import com.ibm.ws.annocache.targets.internal.TargetsTableContainersImpl;
import com.ibm.ws.annocache.targets.internal.TargetsTableImpl;
import com.ibm.ws.annocache.util.internal.UtilImpl_InternMap;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.annocache.targets.cache.TargetCache_ExternalConstants;
import com.ibm.wsspi.annocache.util.Util_Consumer;

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
    @SuppressWarnings("unused")
    private static final String CLASS_NAME = TargetCacheImpl_DataMod.class.getSimpleName();

    /**
     * Local copy of binary read control parameter: The target file
     * contains a compact string table.
     */
    public static final boolean DO_READ_STRINGS = TargetCacheImpl_ReaderBinary.DO_READ_STRINGS;

    /**
     * Local copy of binary read control parameter: The target file is to be read fully.
     */
    public static final boolean DO_READ_FULL = TargetCacheImpl_ReaderBinary.DO_READ_FULL;

    //

    public TargetCacheImpl_DataMod(
        TargetCacheImpl_DataApp app,
        String modName, String e_modName, File modDir, boolean isLightweight) {

        super( app.getFactory(), modName, e_modName, modDir );

        // (new Throwable("DataMod [ " + modName + " : " + ((modDir == null) ? "*** NULL ***" : modDir.getAbsolutePath()) + " ]")).printStackTrace(System.out);

        this.app = app;
        this.isLightweight = isLightweight;
        this.useBinaryFormat = getCacheOptions().getUseBinaryFormat();

        //

        this.consLock = new ConsLock();
        this.cons = new HashMap<String, TargetCacheImpl_DataCon>();

        this.containersFile = getDataFile(TargetCache_ExternalConstants.CONTAINERS_NAME);

        this.seedCon = null;
        this.partialCon = null;
        this.excludedCon = null;
        this.externalCon = null;

        this.classesFile = getDataFile(TargetCache_ExternalConstants.CLASSES_NAME);

        this.unresolvedRefsFile = getDataFile(TargetCache_ExternalConstants.UNRESOLVED_REFS_NAME);
        this.resolvedRefsFile = getDataFile(TargetCache_ExternalConstants.RESOLVED_REFS_NAME);
    }

    //

    private final TargetCacheImpl_DataApp app;

    @Trivial
    public TargetCacheImpl_DataApp getApp() {
        return app;
    }

    //

    private final boolean isLightweight;

    @Trivial
    public boolean getIsLightweight() {
        return isLightweight;
    }

    @Override
    public File getDataFile(String relativePath) {
        if ( getIsLightweight() ) {
            return null;
        } else {
            return super.getDataFile(relativePath);
        }
    }

    //

    private final boolean useBinaryFormat;

    @Trivial
    public boolean getUseBinaryFormat() {
        return useBinaryFormat;
    }

    //

    protected final File containersFile;

    public File getContainersFile() {
        return containersFile;
    }

    public boolean hasContainersTable() {
        return exists( getContainersFile() );
    }

    public boolean readContainerTable(final TargetsTableContainersImpl containerTable) {
        long readStart = System.nanoTime();

        boolean didRead;

        File useContainersFile = getContainersFile();
        
        if ( getUseBinaryFormat() ) {
            Util_Consumer<TargetCacheImpl_ReaderBinary, IOException> readAction =
                new Util_Consumer<TargetCacheImpl_ReaderBinary, IOException>() {
                    public void accept(TargetCacheImpl_ReaderBinary reader) throws IOException {
                        reader.readEntire(containerTable);
                    }
                };
            didRead = readBinary(useContainersFile, DO_READ_STRINGS, DO_READ_FULL, readAction);
        } else {
            didRead = super.read(useContainersFile, containerTable);
        }

        @SuppressWarnings("unused")
        long readDuration = addReadTime(readStart, "Containers");

        return didRead;
    }

    public void writeContainersTable(final TargetsTableContainersImpl containerTable) {
        // String methodName = "writeContainersTable";

        Util_Consumer<TargetCacheImpl_Writer, IOException> writeAction;
        Util_Consumer<TargetCacheImpl_WriterBinary, IOException> writeActionBinary;

        if ( getUseBinaryFormat() ) {
            writeAction = null;
            writeActionBinary =
                new Util_Consumer<TargetCacheImpl_WriterBinary, IOException>() {
                    public void accept(TargetCacheImpl_WriterBinary writer) throws IOException {
                        writer.writeEntire(containerTable);
                    }
                };
        } else {
            writeAction =
                new Util_Consumer<TargetCacheImpl_Writer, IOException>() {
                    public void accept(TargetCacheImpl_Writer writer) throws IOException {
                        writer.write(containerTable);
                    }
                };
            writeActionBinary = null;
        }

        scheduleWrite(
            "Containers", getContainersFile(), DO_TRUNCATE,
            writeAction, writeActionBinary);
    }

    //

    private class ConsLock {
        // EMPTY
    }
    private final ConsLock consLock;

    private final Map<String, TargetCacheImpl_DataCon> cons;

    @Trivial
    protected Map<String, TargetCacheImpl_DataCon> getCons() {
        return cons;
    }

    public TargetCacheImpl_DataCon getSourceConForcing(boolean isNamed, String conPath) {
        TargetCacheImpl_DataCon con;

        synchronized(consLock) {
            Map<String, TargetCacheImpl_DataCon> useCons = getCons();

            con = useCons.get(conPath);
            if ( con == null ) {
                con = getApp().getSourceConForcing(isNamed, conPath);
                useCons.put(conPath, con);
            }
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

    /**
     * Create result container data.
     *
     * Result container data differs from simple container data in several ways:
     *
     * Result container data is named according to the category of data (SEED,
     * PARTIAL, EXCLUDED, or EXTERNAL) which is held.  Simple container data is
     * named based on the container from which the data was obtained.
     *
     * Result container data is stored relative to the enclosing module.  Simple
     * container data is stored relative to the enclosing application.
     *
     * Result container data is written if and only if the enclosing module
     * is written.  Simple container data is written if and only if the enclosing
     * application is written.
     *
     * @param resultConName The name of the result container.
     *
     * @return New result container data.
     */
    @Trivial
    public TargetCacheImpl_DataCon createResultConData(String resultConName) {
        String e_resultConName = encode(resultConName);
        File e_resultConFile = e_getConFile(e_resultConName);
        return createConData( this,
            resultConName, e_resultConName, e_resultConFile,
            TargetCacheImpl_DataCon.IS_RESULT );
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
        String resultConName = getResultName(scanPolicy);

        if ( scanPolicy == ScanPolicy.SEED ) {
            if ( seedCon == null ) {
                seedCon = createResultConData(resultConName);
            }
            return seedCon;

        } else if ( scanPolicy == ScanPolicy.PARTIAL ) {
            if ( partialCon == null ) {
                partialCon = createResultConData(resultConName);
            }
            return partialCon;

        } else if ( scanPolicy == ScanPolicy.EXCLUDED ) {
            if ( excludedCon == null ) {
                excludedCon = createResultConData(resultConName);
            }
            return excludedCon;

        } else if ( scanPolicy == ScanPolicy.EXTERNAL ) {
            if ( externalCon == null ) {
                externalCon = createResultConData(resultConName);
            }
            return externalCon;

        } else {
            throw new IllegalArgumentException("Unknown policy [ " + scanPolicy + " ]");
        }
    }

    public void writeResultCon(ScanPolicy scanPolicy, TargetsTableImpl resultData) {
        TargetCacheImpl_DataCon resultCon = getResultCon(scanPolicy);

        resultCon.writeStamp(this, resultData);
        resultCon.writeData(this, resultData);
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

        long readStart = System.nanoTime();

        File refsFile = getUnresolvedRefsFile();
        // System.out.println("Unrefs path [ " + refsFile.getAbsolutePath() + " ]");

        boolean didRead;

        if ( getUseBinaryFormat() ) {
            Util_Consumer<TargetCacheImpl_ReaderBinary, IOException> readAction =
                new Util_Consumer<TargetCacheImpl_ReaderBinary, IOException>() {
                    public void accept(TargetCacheImpl_ReaderBinary reader) throws IOException {
                        reader.readEntireUnresolvedRefs(i_unresolvedClassNames, classNameInternMap);
                    }
                };
            didRead = readBinary(refsFile, !DO_READ_STRINGS, DO_READ_FULL, readAction);
        } else {
            TargetCache_Readable refsReadable = new TargetCache_Readable() {
                @Override
                public List<TargetCache_ParseError> readUsing(TargetCache_Reader reader) throws IOException {
                    return basicReadUnresolvedRefs(classNameInternMap, i_unresolvedClassNames);
                }
            };

            didRead = super.read(refsFile, refsReadable);
        }

        @SuppressWarnings("unused")
        long readDuration = addReadTime(readStart, "Unresolved Refs");

        return didRead;
    }

    public void writeUnresolvedRefs(Set<String> unresolvedClassNames) {
        if ( !shouldWrite("Unresolved class references") ) {
            return;
        }

        // The unresolved class names are written from TargetsScannerOverallImpl.validInternalUnresolved,
        // with the unresolved class names obtained from internal class sources.
        //
        // The unresolved class names will be modified by TargetsScannerOverallImpl.validExternal, which
        // is performed as a following step.
        //
        // To prevent a ConcurrentModificationException, when asynchronous writes are enabled, the
        // unresolved class names collection must be copied before handing it off to the writer.
        //
        // When asynchronous writes are not enabled, the unresolved class names may be used as is,
        // since the write is synchronous and is completed before the call to validExternal.

        final Collection<String> useClassNames;
        if ( isWriteSynchronous() ) {
            useClassNames = unresolvedClassNames;
        } else {
            useClassNames = new ArrayList<String>(unresolvedClassNames);
        }

        Util_Consumer<TargetCacheImpl_Writer, IOException> writeAction;
        Util_Consumer<TargetCacheImpl_WriterBinary, IOException> writeActionBinary;

        if ( getUseBinaryFormat() ) {
            writeAction = null;
            writeActionBinary =
                new Util_Consumer<TargetCacheImpl_WriterBinary, IOException>() {
                    public void accept(TargetCacheImpl_WriterBinary writer) throws IOException {
                        writer.writeUnresolvedRefsEntire(useClassNames);
                    }
                };
        } else {
            writeAction =
                new Util_Consumer<TargetCacheImpl_Writer, IOException>() {
                    public void accept(TargetCacheImpl_Writer writer) throws IOException {
                        writer.writeUnresolvedRefs(useClassNames);
                    }
                };
            writeActionBinary = null;
        }

        scheduleWrite(
            "Unresolved classes", getUnresolvedRefsFile(), DO_TRUNCATE,
            writeAction, writeActionBinary);
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

        long readStart = System.nanoTime();

        File refsFile = getResolvedRefsFile();
        // System.out.println("Refs path [ " + refsFile.getAbsolutePath() + " ]");

        boolean didRead;

        if ( getUseBinaryFormat() ) {
            Util_Consumer<TargetCacheImpl_ReaderBinary, IOException> readAction =
                new Util_Consumer<TargetCacheImpl_ReaderBinary, IOException>() {
                    public void accept(TargetCacheImpl_ReaderBinary reader) throws IOException {
                        reader.readEntireResolvedRefs(i_resolvedClassNames, classNameInternMap);
                    }
                };
            didRead = readBinary(refsFile, !DO_READ_STRINGS, DO_READ_FULL, readAction);
        } else {
            TargetCache_Readable refsReadable = new TargetCache_Readable() {
                @Override
                public List<TargetCache_ParseError> readUsing(TargetCache_Reader reader) throws IOException {
                    return basicReadResolvedRefs(classNameInternMap, i_resolvedClassNames);
                }
            };
            didRead = super.read(refsFile, refsReadable);
        }

        @SuppressWarnings("unused")
        long readDuration = addReadTime(readStart, "Resolved Refs");

        return didRead;
    }

    public void writeResolvedRefs(Set<String> resolvedClassNames) {
        if ( !shouldWrite("Resolved class references") ) {
            return;
        }

        // The resolved class names are written from TargetsScannerOverallImpl.validInternalUnresolved,
        // with the resolved class names obtained from internal class sources.
        //
        // The resolved class names will be modified by TargetsScannerOverallImpl.validExternal, which
        // is performed as a following step.
        //
        // To prevent a ConcurrentModificationException, when asynchronous writes are enabled, the
        // resolved class names collection must be copied before handing it off to the writer.
        //
        // When asynchronous writes are not enabled, the resolved class names may be used as is,
        // since the write is synchronous and is completed before the call to validExternal.

        final Collection<String> useClassNames;
        if ( isWriteSynchronous() ) {
            useClassNames = resolvedClassNames;
        } else {
            useClassNames = new ArrayList<String>(resolvedClassNames);
        }

        Util_Consumer<TargetCacheImpl_Writer, IOException> writeAction;
        Util_Consumer<TargetCacheImpl_WriterBinary, IOException> writeActionBinary;
        
        if ( getUseBinaryFormat() ) {
            writeAction = null;
            writeActionBinary =
                new Util_Consumer<TargetCacheImpl_WriterBinary, IOException>() {
                    public void accept(TargetCacheImpl_WriterBinary writer) throws IOException {
                        writer.writeResolvedRefsEntire(useClassNames);
                    }
                };
        } else {
            writeAction =
                new Util_Consumer<TargetCacheImpl_Writer, IOException>() {
                    public void accept(TargetCacheImpl_Writer writer) throws IOException {
                        writer.writeResolvedRefs(useClassNames);
                    }
                };
            writeActionBinary = null;
        }

        scheduleWrite("Resolved Classes", getResolvedRefsFile(), DO_TRUNCATE, writeAction, writeActionBinary);
    }

    //

    protected final File classesFile;

    public File getClassesFile() {
        return classesFile;
    }

    public boolean hasClasses() {
        return ( exists( getClassesFile() ) );
    }

    public boolean readClasses(final TargetsTableClassesMultiImpl classesTable) {
        long readStart = System.nanoTime();

        File useClassesFile = getClassesFile();

        boolean didRead;

        if ( getUseBinaryFormat() ) {
            Util_Consumer<TargetCacheImpl_ReaderBinary, IOException> readAction =
                new Util_Consumer<TargetCacheImpl_ReaderBinary, IOException>() {
                    public void accept(TargetCacheImpl_ReaderBinary reader) throws IOException {
                        reader.readEntire(classesTable);
                    }
                };
            didRead = readBinary(useClassesFile, DO_READ_STRINGS, DO_READ_FULL, readAction);
        } else {
            didRead = read(useClassesFile, classesTable);
        }

        @SuppressWarnings("unused")
        long readDuration = addReadTime(readStart, "Classes");

        return didRead;
    }

    public void writeClasses(final TargetsTableClassesMultiImpl classesTable) {
        if ( !shouldWrite("Classes table") ) {
            return;
        }

        Util_Consumer<TargetCacheImpl_Writer, IOException> writeAction;
        Util_Consumer<TargetCacheImpl_WriterBinary, IOException> writeActionBinary;

        if ( getUseBinaryFormat() ) {
            writeAction = null;
            writeActionBinary =
                new Util_Consumer<TargetCacheImpl_WriterBinary, IOException>() {
                    public void accept (TargetCacheImpl_WriterBinary writer) throws IOException {
                        // See the comment on 'mergeClasses': This must be synchronized
                        // with updates to the class table which occur in 
                        // TargetsScannerImpl_Overall.validExternal'.
                        synchronized ( classesTable ) {
                            writer.writeEntire(classesTable);
                        }
                    }
                };
        } else {
            writeAction =
                new Util_Consumer<TargetCacheImpl_Writer, IOException>() {
                    public void accept(TargetCacheImpl_Writer writer) throws IOException {
                        // See the comment on 'mergeClasses': This must be synchronized
                        // with updates to the class table which occur in 
                        // TargetsScannerImpl_Overall.validExternal'.
                        synchronized ( classesTable ) {
                            writer.write(classesTable);
                        }
                    }
                };
            writeActionBinary = null;
        }

        scheduleWrite(
            "Classes", getClassesFile(), DO_TRUNCATE,
            writeAction, writeActionBinary);
    }

    // Writes are handled at the application level.

    public boolean shouldAppRead(String inputDescription) {
        return getApp().shouldWrite(inputDescription);
    }

    @Override
    public boolean shouldRead(String inputDescription) {
        if ( !isNamed() || getIsLightweight() ) {
            return false;
        } else if ( !shouldAppRead(inputDescription) ) {
            return false;
        } else {
            return super.shouldRead(inputDescription);
        }
    }

    public boolean shouldAppWrite(String outputDescription) {
        return getApp().shouldWrite(outputDescription);
    }

    @Override
    public boolean shouldWrite(String outputDescription) {
        if ( !isNamed() || getIsLightweight() ) {
            return false;
        } else if ( !shouldAppWrite(outputDescription) ) {
            return false;
        } else {
            return super.shouldWrite(outputDescription);
        }
    }

    @Trivial
    protected boolean isWriteSynchronous() {
        return ( getApp().isWriteSynchronous() );
    }

    // TODO: Unify the writer types.  Two parameters should not be
    //       necessary.

    @Trivial
    protected void scheduleWrite(
        String description,
        File outputFile,
        boolean doTruncate,
        Util_Consumer<TargetCacheImpl_Writer, IOException> writeAction,
        Util_Consumer<TargetCacheImpl_WriterBinary, IOException> writeActionBinary) {

        // System.out.println("Scheduled write [ " + description + " ] [ " + outputFile.getAbsolutePath() + " ]");

        getApp().scheduleWrite(this,
            description, outputFile, doTruncate,
            writeAction, writeActionBinary);
    }
}
