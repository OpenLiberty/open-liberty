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
import java.io.IOException;
import java.util.Collections;
import java.util.logging.Level;

import org.jboss.jandex.Index;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.annocache.jandex.internal.Jandex_Utils;
import com.ibm.ws.annocache.jandex.internal.SparseIndex;
import com.ibm.ws.annocache.targets.internal.TargetsScannerOverallImpl;
import com.ibm.ws.annocache.targets.internal.TargetsTableAnnotationsImpl;
import com.ibm.ws.annocache.targets.internal.TargetsTableClassesImpl;
import com.ibm.ws.annocache.targets.internal.TargetsTableImpl;
import com.ibm.ws.annocache.targets.internal.TargetsTableTimeStampImpl;
import com.ibm.wsspi.annocache.classsource.ClassSource;
import com.ibm.wsspi.annocache.targets.cache.TargetCache_ExternalConstants;
import com.ibm.wsspi.annocache.util.Util_Consumer;

/**
 * Annotation cache data for a single container.
 *
 * Container data is managed in two different ways:
 * 
 * Non-result container data (which has a container) has its own directory relative to
 * the directory of the enclosing application, and named using the
 * standard pattern:
 * <code>
 *     appFolder + CON_PREFIX + encode(containerPath)
 * </code>
 *
 * Result container data (which has no actual container) has its own
 * directory relative to the directory of the enclosing module, and
 * named using tags specific to the result type.  One of:
 * {@link com.ibm.wsspi.annocache.targets.cache.TargetCache_ExternalConstants#SEED_RESULT_NAME},
 * {@link com.ibm.wsspi.annocache.targets.cache.TargetCache_ExternalConstants#PARTIAL_RESULT_NAME},
 * {@link com.ibm.wsspi.annocache.targets.cache.TargetCache_ExternalConstants#EXCLUDED_RESULT_NAME}, or
 * {@link com.ibm.wsspi.annocache.targets.cache.TargetCache_ExternalConstants#EXTERNAL_RESULT_NAME}.
 *
 * Container data has four parts:
 * <ul><li>Time stamp information<li>
 *     <li>Classes information, which consists of class references and detailed class information</li>
 *     <li>Annotation targets information</li>
 * </ul>
 *
 * Each part of the container has its own table.
 */
public class TargetCacheImpl_DataCon extends TargetCacheImpl_DataBase {
    private static final String CLASS_NAME = TargetCacheImpl_DataCon.class.getSimpleName();

    //

    // The container data is relative to the enclosing application,
    // not to the enclosing module.
    //
    // Whether a container file is available depends on whether the
    // application directory is available, not on whether the module
    // directory is available.
    //
    // Container data is stored either as a single file or as a directory,
    // according to the cache settings.

    /** Control parameter: Is this data for a component? */
    public static final boolean IS_SOURCE = true;

    /** Control parameter: Is this data for a result bucket? */
    public static final boolean IS_RESULT = false;

    /** Local copy of binary read control parameter: Read a compact string table. */
    public static final boolean DO_READ_STRINGS = TargetCacheImpl_ReaderBinary.DO_READ_STRINGS;

    /** Local copy of binary read control parameter: Read the target file fully. */
    public static final boolean DO_READ_FULL = TargetCacheImpl_ReaderBinary.DO_READ_FULL;

    public TargetCacheImpl_DataCon(
        TargetCacheImpl_DataBase parentCache,
        String conName, String e_conName, File conFile,
        boolean isSource) {

        super( parentCache.getFactory(), conName, e_conName, conFile );

        // (new Throwable("DataCon [ " + conName + " : " + ((conFile == null) ? "*** NULL ***" : conFile.getAbsolutePath()) + " ]")).printStackTrace(System.out);

        String methodName = "<init>";

        this.parentData = parentCache;

        this.isSource = isSource;

        if ( this.getUseSeparateCaches() ) {
            // When separating container data, do not write to the component file.
            // Write a time stamp file and either a jandex file or a classes file and
            // an annotation targets file.

            this.combinedFileData = null;

            this.stampDataFile = createFileData(TargetCache_ExternalConstants.TIMESTAMP_NAME);

            if ( this.getUseJandexFormat() ) {
                this.jandexFileData = createFileData(TargetCache_ExternalConstants.JANDEX_NAME);
                this.classesFileData = null;
                this.targetsFileData = null;
            } else {
                this.jandexFileData = null;
                this.classesFileData = createFileData(TargetCache_ExternalConstants.CLASSES_NAME);
                this.targetsFileData = createFileData(TargetCache_ExternalConstants.ANNO_TARGETS_NAME);
            }

        } else {
            // When combining container data, if jandex formatting is being used,
            // write to a stamp file and to a jandex data file.  But if jandex formatting
            // is not being used, write to a component file.  In either case, do not
            // write to a classes file or to a targets file.

            if ( this.getUseJandexFormat() ) {
                this.combinedFileData = null;

                this.stampDataFile = createFileData( TargetCache_ExternalConstants.TIMESTAMP_NAME, getDataFile() );

                // Put on a jandex prefix: The first character cannot be "C_".

                File stampFile = this.stampDataFile.getFile();

                File jandexFile;
                if ( stampFile == null ) {
                    jandexFile = null;
                } else {
                    String jandexName =
                        TargetCache_ExternalConstants.JANDEX_PREFIX +
                        stampFile.getName();
                    jandexFile = new File( stampFile.getParentFile(), jandexName );
                }

                this.jandexFileData = createFileData(TargetCache_ExternalConstants.JANDEX_NAME, jandexFile);

            } else {
                this.combinedFileData = createFileData( TargetCache_ExternalConstants.COMPONENT_NAME, getDataFile() );

                this.stampDataFile = null;
                this.jandexFileData = null;
            }

            this.classesFileData = null;
            this.targetsFileData= null;
        }

        this.targetsTable = null;

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "Container [ {0} ] of [ {1} ]",
                new Object[] { getName(), parentCache.getName() });

            logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "IsComponent [ {0} ] Separate [ {1} ]",
                    new Object[] { this.isSource, getUseSeparateCaches() });
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "Use Jandex Format [ {0} ] Use Binary Format [ {1} ]",
                    new Object[] { getUseJandexFormat(), getUseBinaryFormat() });
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "Write Threshold [ {0} ]",
                    new Object[] { getWriteLimit() });

            if ( this.combinedFileData != null ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "{0}", this.combinedFileData);
            }
            if ( this.stampDataFile != null ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "{0}", this.stampDataFile);
            }
            if ( this.jandexFileData != null ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "{0}", this.jandexFileData);
            }
            if ( this.classesFileData != null ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "{0}", this.classesFileData);
            }
            if ( this.targetsFileData != null ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "{0}", this.targetsFileData);
            }
        }
    }

    //

    protected TargetCacheImpl_DataFile createFileData(String cacheName) {
        return new TargetCacheImpl_DataFile(cacheName, getDataFile(cacheName) );
    }

    protected TargetCacheImpl_DataFile createFileData(String cacheName, File cacheFile) {
        return new TargetCacheImpl_DataFile(cacheName, cacheFile);
    }

    // DataApp for simple container data; DataMod for policy container data.

    private final TargetCacheImpl_DataBase parentData;

    @Trivial
    public TargetCacheImpl_DataBase getParentData() {
        return parentData;
    }

    private final boolean isSource;

    @Trivial
    public boolean getIsSource() {
        return isSource;
    }

    @Trivial
    public boolean getIsResult() {
        return !isSource;
    }

    //

    @Trivial
    public boolean getUseSeparateCaches() {
        return getCacheOptions().getSeparateContainers();
    }

    /**
     * Tell if data is written in jandex format.
     * 
     * Obtain the value from the cache options.  See
     * {@link #getCacheOptions()} and
     * {@link com.ibm.wsspi.annocache.targets.cache.TargetCache_Options#getUseJandexFormat()}.
     *
     * This applies only to component data: Override the option value
     * to false when {@link #getIsSource()} answers false.
     *
     * @return True or false telling if data is to be written in jandex format
     */
    @Trivial
    public boolean getUseJandexFormat() {
        if ( !getIsSource() ) {
            return false;
        } else {
            return getCacheOptions().getUseJandexFormat();
        }
    }

    @Trivial
    public boolean getAlwaysValid() {
        return getCacheOptions().getAlwaysValid();
    }

    @Trivial
    public boolean getUseBinaryFormat() {
        return getCacheOptions().getUseBinaryFormat();
    }

@Trivial
    public int getWriteLimit() {
        return getCacheOptions().getWriteLimit();
    }

    //

    /**
     * Read data.  No stamp validation is performed.  The read data
     * is not cached in the container.
     *
     * @param targetData The data which is to be read.
     *
     * @return True or false telling if the read was successful.
     */
    public boolean basicReadData(TargetsTableImpl targetData) {
        if ( getUseJandexFormat() ) {
            return ( basicReadStamp( targetData.getStampTable() ) &&
                     basicReadJandex(targetData) );
        } else if ( getUseSeparateCaches() ) {
            return ( basicReadStamp( targetData.getStampTable() ) &&
                     basicReadClasses( targetData.getClassTable() ) && 
                     basicReadTargets( targetData.getAnnotationTable() ) );
        } else {
            return ( basicReadDataCombined(targetData) );
        }
    }

    /**
     * Write all data: The class table, the annotations table, and the stamp table.
     *
     * Module data is needed, since writes are performed at the module level.  The
     * module data cannot be stored in the container data because the container data
     * is shared between modules.
     *
     * The read data is not cached.
     * 
     * This write is used for both result data and for source data.
     * 
     * @param modData The module which will perform the write.
     * @param targetData The data which is to be written.
     */
    public void basicWriteData(TargetCacheImpl_DataMod modData, TargetsTableImpl targetData) {
        if ( !shouldWrite("Full data") ) {
            return;
        } else if ( getWriteLimit() > targetData.getClassNames().size() ) {
            return;
        } else if ( targetData.getUsedJandex() ) {
            return;
        }

        if ( getUseJandexFormat() ) {
            basicWriteStamp( modData, targetData.getStampTable() );
            basicWriteJandex( modData, targetData.consumeJandexIndex() );

        } else if ( getUseSeparateCaches() ) {
            basicWriteStamp( modData, targetData.getStampTable() );
            basicWriteClasses( modData, targetData.getClassTable() );
            basicWriteTargets( modData, targetData.getAnnotationTable() );

        } else {
            basicWriteDataCombined(modData, targetData);
        }
    }

    // File structure for time stamp data.
    // Only used when data is separate or when jandex format is being used. 

    private final TargetCacheImpl_DataFile stampDataFile;
 
    @Trivial
    protected TargetCacheImpl_DataFile getStampFileData() {
        return stampDataFile;
    }

    @Trivial
    public File getStampFile() {
        return stampDataFile.getFile();
    }

    @Trivial
    public boolean getHasStampFile() {
        return stampDataFile.getHasFile();
    }

    protected void setHasStampFile(boolean hasStampFile) {
        stampDataFile.setHasFile(hasStampFile);
    }

    private boolean basicReadStamp(TargetsTableTimeStampImpl useStampTable) {
        long readStart = System.nanoTime();

        boolean didRead;

        File useStampFile = getStampFile();

        if ( getUseBinaryFormat() ) {
            didRead = readBinary(
                useStampFile, !DO_READ_STRINGS, !DO_READ_FULL,
                (TargetCacheImpl_ReaderBinary reader) -> {
                    reader.readEntire(useStampTable);
                } );
        } else {
            didRead = read(useStampFile, useStampTable);
        }

        @SuppressWarnings("unused")
        long readDuration = addReadTime(readStart, "Read Stamp");

        return didRead;
    }

    private void basicWriteStamp(
        TargetCacheImpl_DataMod modData,
        TargetsTableTimeStampImpl useStampTable) {

        String description;
        if ( logger.isLoggable(Level.FINER) ) {
            description = "Container [ " + getName() + " ] TimeStamp [ " + getStampFileData() + " ]";
        } else {
            description = null;
        }

        Util_Consumer<TargetCacheImpl_Writer, IOException> writeAction;
        Util_Consumer<TargetCacheImpl_WriterBinary, IOException> writeActionBinary;

        if ( getUseBinaryFormat() ) {
            writeAction = null;
            writeActionBinary =
                (TargetCacheImpl_WriterBinary useWriter) -> {
                    useWriter.writeEntire(useStampTable);
                };
        } else {
            writeAction =
                (TargetCacheImpl_Writer useWriter) -> {
                    useWriter.write(useStampTable);
                };
            writeActionBinary = null;
        }

        modData.scheduleWrite(
            description, getStampFile(), DO_TRUNCATE,
            writeAction, writeActionBinary);
    }

    /**
     * Write the stamp table to an existing targets table.
     *
     * @param modData The module which will perform the write.
     * @param useStampTable The data which is to be written.
     */
    private void basicRewriteStamp(
        TargetCacheImpl_DataMod modData,
        TargetsTableTimeStampImpl useStampTable) {

        String description;
        if ( logger.isLoggable(Level.FINER) ) {
            description = "Container [ " + getName() + " ] Container file [ " + getStampFileData() + " ]";
        } else {
            description = null;
        }

        Util_Consumer<TargetCacheImpl_Writer, IOException> writeAction;
        Util_Consumer<TargetCacheImpl_WriterBinary, IOException> writeActionBinary;
        
        if ( getUseBinaryFormat() ) {
            writeAction = null;
            writeActionBinary =
                (TargetCacheImpl_WriterBinary useWriter) -> {
                    useWriter.rewrite(useStampTable);
                };
        } else {
            writeAction =
                (TargetCacheImpl_Writer useWriter) -> {
                    useWriter.write(useStampTable);
                };
            writeActionBinary = null;
        }

        modData.scheduleWrite(
            description, getCombinedFile(), DO_NOT_TRUNCATE,
            writeAction, writeActionBinary);
    }

    // File structure for annotation targets data.
    // Only used when data is separate and jandex format is not being used.

    private final TargetCacheImpl_DataFile targetsFileData;

    @Trivial
    protected TargetCacheImpl_DataFile getTargetsFileData() {
        return targetsFileData;
    }

    public File getTargetsFile() {
        return targetsFileData.getFile();
    }

    @Trivial
    public boolean getHasTargetsFile() {
        return targetsFileData.getHasFile();
    }

    protected void setHasTargetsFile(boolean hasTargetsFile) {
        targetsFileData.setHasFile(hasTargetsFile);
    }

    private boolean basicReadTargets(TargetsTableAnnotationsImpl targetTable) {
        long readStart = System.nanoTime();

        File useTargetsFile = getTargetsFile();

        boolean didRead;

        if ( getUseBinaryFormat() ) {
            didRead = readBinary(
                useTargetsFile, DO_READ_STRINGS, DO_READ_FULL,
                (TargetCacheImpl_ReaderBinary reader) -> {
                    reader.readEntire(targetTable);
                } );
        } else {
            didRead = read(useTargetsFile, targetTable);
        }

        @SuppressWarnings("unused")
        long readDuration = addReadTime(readStart, "Read Targets");

        // System.out.println("Read targets [ " + getName() + " ] [ " + readDuration + " ]");
        
        return didRead;
    }

    private void basicWriteTargets(
        TargetCacheImpl_DataMod modData,
        TargetsTableAnnotationsImpl targetTable) {

        String description;
        if ( logger.isLoggable(Level.FINER) ) {
            description = "Container [ " + getName() + " ] Targets [ " + getTargetsFileData() + " ]";
        } else {
            description = null;
        }

        Util_Consumer<TargetCacheImpl_Writer, IOException> writeAction;
        Util_Consumer<TargetCacheImpl_WriterBinary, IOException> writeActionBinary;

        if ( getUseBinaryFormat() ) {
            writeAction = null;
            writeActionBinary =
                (TargetCacheImpl_WriterBinary useWriter) -> {
                    useWriter.writeEntire(targetTable);
                };
        } else {
            writeAction =
                (TargetCacheImpl_Writer useWriter) -> {
                    useWriter.write(targetTable);
                };
            writeActionBinary = null;
        }

        modData.scheduleWrite(
            description, getTargetsFile(), DO_TRUNCATE,
            writeAction, writeActionBinary);
    }

    // File structure for classes data.
    // Only used when data is separate and jandex format is not being used.

    private final TargetCacheImpl_DataFile classesFileData;

    @Trivial
    protected TargetCacheImpl_DataFile getClassesFileData() {
        return classesFileData;
    }

    @Trivial
    public File getClassesFile() {
        return classesFileData.getFile();
    }

    @Trivial
    public boolean getHasClassesFile() {
        return classesFileData.getHasFile();
    }

    protected void setHasClassesFile(boolean hasClassesFile) {
        classesFileData.setHasFile(hasClassesFile);
    }

    private boolean basicReadClasses(TargetsTableClassesImpl classesTable) {
        long readStart = System.nanoTime();

        File useClassRefsFile = getClassesFile();

        boolean didRead;

        if ( getUseBinaryFormat() ) {
            didRead = readBinary(
                useClassRefsFile, DO_READ_STRINGS, DO_READ_FULL,
                (TargetCacheImpl_ReaderBinary reader) -> {
                    reader.readEntire(classesTable);
                } );
        } else {
            didRead = read(useClassRefsFile, classesTable);
        }

        @SuppressWarnings("unused")
        long readDuration = addReadTime(readStart, "Read Classes");

        // System.out.println("Read classes [ " + getName() + " ] [ " + readDuration + " ]");

        return didRead;
    }

    private void basicWriteClasses(
        TargetCacheImpl_DataMod modData,
        TargetsTableClassesImpl classesTable) {

        String description;
        if ( logger.isLoggable(Level.FINER) ) {
            description = "Container [ " + getName() + " ] Class references [ " + getClassesFileData() + " ]";
        } else {
            description = null;
        }

        Util_Consumer<TargetCacheImpl_Writer, IOException> writeAction;
        Util_Consumer<TargetCacheImpl_WriterBinary, IOException> writeActionBinary;
            
        if ( getUseBinaryFormat() ) {
            writeAction = null;
            writeActionBinary =
                (TargetCacheImpl_WriterBinary useWriter) -> {
                    useWriter.writeEntire(classesTable);
                };
        } else {
            writeAction =
                (TargetCacheImpl_Writer useWriter) -> {
                    useWriter.write(classesTable);
                };
            writeActionBinary = null;
        }

        modData.scheduleWrite(
            description, getClassesFile(), DO_NOT_TRUNCATE,
            writeAction, writeActionBinary);
    }

    // File structure for combined data (stamp plus classes plus annotation
    // targets).
    //
    // Only used when data is separate and jandex format is not being used.

    private final TargetCacheImpl_DataFile combinedFileData;
 
    @Trivial
    protected TargetCacheImpl_DataFile getCombinedFileData() {
        return combinedFileData;
    }

    @Trivial
    public File getCombinedFile() {
        return combinedFileData.getFile();
    }

    @Trivial
    public boolean getHasCombinedFile() {
        return combinedFileData.getHasFile();
    }

    protected void setHasCombinedFile(boolean hasCombinedFile) {
        combinedFileData.setHasFile(hasCombinedFile);
    }

    public boolean basicReadDataCombined(TargetsTableImpl targetData) {
        long readStart = System.nanoTime();

        File useCombinedFile = getCombinedFile();

        boolean didRead;

        if ( getUseBinaryFormat() ) {
            didRead = readBinary(
                useCombinedFile, DO_READ_STRINGS, DO_READ_FULL,
                (TargetCacheImpl_ReaderBinary reader) -> {
                    reader.readEntire(
                        targetData.getStampTable(),
                        targetData.getClassTable(),
                        targetData.getAnnotationTable() );
                } );

        } else {
            didRead = read(
                useCombinedFile,
                targetData.getStampTable(),
                targetData.getClassTable(),
                targetData.getAnnotationTable() );
        }

        @SuppressWarnings("unused")
        long readDuration = addReadTime(readStart, "Read Together");

        // System.out.println("Read combined [ " + getName() + " ] [ " + readDuration + " ]");

        return didRead;
    }

    /**
     * Write all data to a single file: Write the stamp, then the class table, then
     * the annotations table.
     *
     * Module data is needed, since writes are performed at the module level.  The
     * module data cannot be stored in the container data because the container data
     * is shared between modules.
     *
     * @param modData The module which will perform the write.
     * @param targetData The data which is to be written.
     */
    private void basicWriteDataCombined(TargetCacheImpl_DataMod modData, TargetsTableImpl targetData) {
        String description;
        if ( logger.isLoggable(Level.FINER) ) {
            description = "Container [ " + getName() + " ] Container file [ " + getStampFileData() + " ]";
        } else {
            description = null;
        }

        Util_Consumer<TargetCacheImpl_Writer, IOException> writeAction;
        Util_Consumer<TargetCacheImpl_WriterBinary, IOException> writeActionBinary;
        
        if ( getUseBinaryFormat() ) {
            writeAction = null;
            writeActionBinary =
                (TargetCacheImpl_WriterBinary useWriter) -> {
                    useWriter.writeEntire(
                        targetData.getStampTable(),
                        targetData.getClassTable(),
                        targetData.getAnnotationTable() );
                };
        } else {
            writeAction =
                (TargetCacheImpl_Writer useWriter) -> {
                    useWriter.write( targetData.getStampTable() );
                    useWriter.write( targetData.getClassTable() );
                    useWriter.write( targetData.getAnnotationTable() );
                };
            writeActionBinary = null;
        }

        modData.scheduleWrite(
            description, getCombinedFile(), DO_TRUNCATE,
            writeAction, writeActionBinary);
    }

    // File structure for jandex data.
    // Only used when jandex format is being used.

    private final TargetCacheImpl_DataFile jandexFileData;

    @Trivial
    protected TargetCacheImpl_DataFile getJandexFileData() {
        return jandexFileData;
    }

    @Trivial
    public File getJandexFile() {
        return jandexFileData.getFile();
    }

    @Trivial
    public boolean getHasJandexFile() {
        return jandexFileData.getHasFile();
    }

    protected void setHasJandexFile(boolean hasJandexFile) {
        jandexFileData.setHasFile(hasJandexFile);
    }

    private boolean basicReadJandex(TargetsTableImpl targetData) {
        String methodName = "readJandex";

        long readStart = System.nanoTime();

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "Container [ " + getName() + " ] Jandex File [ " + getJandexFileData() + " ]");
        }

        File useJandexFile = getJandexFile();
        String useJandexPath = useJandexFile.getPath();
  
        boolean didRead;

        try {
            SparseIndex sparseIndex = Jandex_Utils.basicReadSparseIndex(useJandexPath);
            targetData.transfer(sparseIndex);
            didRead = true;

        } catch ( IOException e ) {
            readError( useJandexFile, e, Collections.emptyList() );
            didRead = false;
        }

        @SuppressWarnings("unused")
        long readDuration = addReadTime(readStart, "Read Jandex");

        // System.out.println("Sparse jandex cache read [ " + getName() + " ] [ " + readDuration + " ]");

        return didRead;
    }

    private void basicWriteJandex(
        TargetCacheImpl_DataMod modData,
        Index jandexIndex) {

        String description;
        if ( logger.isLoggable(Level.FINER) ) {
            description = "Container [ " + getName() + " ] Jandex File [ " + getJandexFileData() + " ]";
        } else {
            description = null;
        }

        Util_Consumer<TargetCacheImpl_Writer, IOException> writeAction =
            (TargetCacheImpl_Writer useWriter) -> {
                useWriter.write(jandexIndex);
            };

        modData.scheduleWrite(description, getJandexFile(), DO_TRUNCATE, writeAction, null);
    }

    // Cache of the combined targets data.

    private TargetsTableImpl targetsTable;

    public TargetsTableImpl getTargetsTable() {
        return targetsTable;
    }

    public void setTargetsTable(TargetsTableImpl targetsTable) {
        this.targetsTable = targetsTable; 
    }

    //

    /**
     * Tell if data of the container should be read.
     *
     * Container data is read only if the parent is enabled
     * for reads, and only if reading is enabled in general.
     *
     * @param inputDescription The input which is querying
     *     if reads are enabled.
     *
     * @return True or false telling if reads are enabled for
     *     the container.
     */
    @Override
    public boolean shouldRead(String inputDescription) {
        if ( !getParentData().shouldRead(inputDescription) ) {
            return false;
        } else {
            return super.shouldRead(inputDescription);
        }
    }

    /**
     * Tell if data of the container should be written.
     *
     * Container data is written only if the parent is enabled
     * for writes, and only if writing is enabled in general.
     *
     * @param outputDescription The output which is querying
     *     if writes are enabled.
     *
     * @return True or false telling if writes are enabled for
     *     the container.
     */
    @Override
    public boolean shouldWrite(String outputDescription) {
        if ( !getParentData().shouldWrite(outputDescription) ) {
            return false;
        } else {
            return super.shouldWrite(outputDescription);
        }
    }

    //

    @SuppressWarnings("unused")
    private void clearFiles() {
        // useJandexFormat only if isSource

        if ( combinedFileData != null ) { // !useJandexFormat && !useSeparateCaches 
            combinedFileData.setHasFile(false);
        }
        if ( stampDataFile != null ) { // useJandexFormat || useSeparateCaches
            stampDataFile.setHasFile(false);
        }
        if ( targetsFileData != null ) { // !useJandexFormat && useSeparateCaches
            targetsFileData.setHasFile(false);
        }
        if ( classesFileData != null ) { // !useJandexFormat && useSeparateCaches
            classesFileData.setHasFile(false);
        }
        if ( jandexFileData != null ) { // useJandexFormat
            jandexFileData.setHasFile(false);
        }
    }

    public boolean hasRequiredFiles() {
        if ( !shouldRead("Full read") ) {
            return false;

        } else if ( getUseJandexFormat() ) {
            return ( getHasStampFile() && getHasJandexFile() );
        } else if ( getUseSeparateCaches() ) {
            return ( getHasStampFile() && getHasTargetsFile() && getHasClassesFile() );
        } else {
            return ( getHasCombinedFile() );
        }
    }

    //

    /**
     * Update stamp information.  Perform a partial rewrite of of the stamp file. 
     * 
     * Module data is needed, since writes are performed at the module level.  The
     * module data cannot be stored in the container data because the container data
     * is shared between modules.
     * 
     * @param modData The module which will perform the write.
     * @param stampData The stamp data which is to be written.
     */
    public void rewriteStamp(TargetCacheImpl_DataMod modData, TargetsTableTimeStampImpl stampData) {
        if ( !shouldWrite("Time stamp") ) {
            return;
        }

        if ( getUseSeparateCaches() || getUseJandexFormat() ) {
            basicWriteStamp(modData, stampData);
        } else {
            basicRewriteStamp(modData, stampData);
        }
    }

    //

    /**
     * Read and validate the container data.
     * 
     * If the container data is valid, if the data was not previously read, it will be read
     * and cached to the container data.
     *
     * If the container data is not valid (because of a changed time stamp, because data is
     * not available, or because the data could not be read), clear the cache targets table
     * and mark all files as being absent.
     *
     * @param The scanner requesting the read.
     * @param classSourceName The name of the class source of this container data.
     * @param currentStamp The current time stamp of the class source.
     *
     * @return A string describing how the data is not valid.  Null if the data is valid.
     */
    public String isValid(TargetsScannerOverallImpl scanner, String classSourceName, String currentStamp) {
        boolean useAlwaysValid = getAlwaysValid();

        // If the current stamp is non-comparable, unless validity is forced,
        // the answer is forced to non-valid.

        if ( currentStamp.equals(ClassSource.UNRECORDED_STAMP) ||
             currentStamp.equals(ClassSource.UNAVAILABLE_STAMP) ) {
            if ( !useAlwaysValid ) {
                return "Stamp unavailable";
            }
        }

        // If a table is stored to the container data, check it for
        // a container difference.

        TargetsTableImpl useTable = getTargetsTable();
        if ( useTable != null ) {
            if ( useAlwaysValid ) {
                return null;
            } else if ( currentStamp.equals( useTable.getStamp() ) ) {
                return null;
            } else {
                return "Stamp change";
            }
        }

        // If no table is stored to the container, and if no data
        // is stored to cache, then the result must be non-valid.

        if ( !hasRequiredFiles() ) {
            return "No cache data";
        }

        // Data is available in the cache and validity is forced.

        if ( useAlwaysValid ) {
            return null;
        }

        // Actually check the cached stamp value.
        // The current stamp must be comparable.
        // If the stamp could not be read, or is different, the result
        // is non-valid.
        //
        // Otherwise, the result is valid.

        TargetsTableTimeStampImpl useStampTable =
            new TargetsTableTimeStampImpl(classSourceName);

        if ( !basicReadStamp(useStampTable) ) {
            return "Stamp read failure";
        } else if ( !useStampTable.getStamp().equals(currentStamp) ) {
            return "Stamp change";
        } else {
            return null;
        }
    }
}
