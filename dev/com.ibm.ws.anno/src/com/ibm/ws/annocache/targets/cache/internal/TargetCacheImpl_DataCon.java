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
import java.util.List;
import java.util.logging.Level;

import org.jboss.jandex.Index;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.annocache.jandex.internal.Jandex_Utils;
import com.ibm.ws.annocache.jandex.internal.SparseIndex;
import com.ibm.ws.annocache.targets.cache.TargetCache_ParseError;
import com.ibm.ws.annocache.targets.internal.TargetsScannerOverallImpl;
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
public class TargetCacheImpl_DataCon extends TargetCacheImpl_DataBase
    implements TargetCache_ExternalConstants {

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
        TargetCacheImpl_DataBase parentData,
        String conName, String e_conName, File conFile,
        boolean isSource) {

        super( parentData.getFactory(), conName, e_conName, conFile );

        // System.out.println("Data Parent [ " + getClass().getSimpleName() + " ] [ " + name + " ] [ " + parentData.getName() + " ]");

        // (new Throwable("DataCon [ " + conName + " : " + ((conFile == null) ? "*** NULL ***" : conFile.getAbsolutePath()) + " ]")).printStackTrace(System.out);

        String methodName = "<init>";

        this.parentData = parentData;

        this.isSource = isSource;

        this.stampLink = createPeerLink(TIMESTAMP_NAME, TIMESTAMP_NAME);

        // !isSource forces !useJandexFormat.
        if ( this.getUseJandexFormat() ) {
            this.coreDataLink = createPeerLink(DATA_NAME_JANDEX, DATA_NAME_JANDEX);
        } else {
            this.coreDataLink = createPeerLink(DATA_NAME_INTERNAL, DATA_NAME_INTERNAL);
        }

        this.targetsTable = null;

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "Container [ {0} ] of [ {1} ]",
                new Object[] { getName(), parentData.getName() });

            logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "IsComponent [ {0} ]", new Object[] { this.isSource });

            logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "Use Jandex Format [ {0} ] Use Binary Format [ {1} ]",
                    new Object[] { getUseJandexFormat(), getUseBinaryFormat() });
            // logger.logp(Level.FINER, CLASS_NAME, methodName,
            //         "Write Threshold [ {0} ]",
            //         new Object[] { getWriteLimit() });

            logger.logp(Level.FINER, CLASS_NAME, methodName, "{0}", this.stampLink);
            logger.logp(Level.FINER, CLASS_NAME, methodName, "{0}", this.coreDataLink);
        }
    }

    //

    /**
     * Create a file as a peer of the data file.
     *
     * @param cacheName A name to associate with the file.
     * @param cacheExt The extension to use for the file.
     *
     * @return A peer of the data file that has the supplied prefix.
     */
    protected TargetCacheImpl_DataFile createPeerLink(String cacheName, String cacheExt) {
        File useDataFile = getDataFile();

        File peerFile;
        if ( useDataFile != null ) {
            String peerName = useDataFile.getName() + "." + cacheExt;
            peerFile = new File( useDataFile.getParentFile(), peerName );
        } else {
            peerFile = null;
        }

        return new TargetCacheImpl_DataFile(cacheName, peerFile);
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
    public boolean readCoreData(TargetsTableImpl targetData) {
        if ( getUseJandexFormat() ) {
            return ( basicReadJandex(targetData) );
        } else {
            return ( basicReadCoreData(targetData) );
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
    public void writeData(TargetCacheImpl_DataMod modData, TargetsTableImpl targetData) {
        if ( !shouldWrite("Full data") ) {
            return;
        } else if ( targetData.getUsedJandex() ) {
            return;
        }

        if ( getUseJandexFormat() ) {
            writeJandex( modData, targetData.consumeJandexIndex() );
        } else {
            writeCoreData(modData, targetData);
        }
    }

    // File structure for time stamp data.
    // Only used when data is separate or when jandex format is being used. 

    private final TargetCacheImpl_DataFile stampLink;
 
    @Trivial
    protected TargetCacheImpl_DataFile getStampLink() {
        return stampLink;
    }

    @Trivial
    public File getStampFile() {
        return stampLink.getFile();
    }

    @Trivial
    public boolean getHasStampFile() {
        return stampLink.getHasFile();
    }

    protected void setHasStampFile(boolean hasStampFile) {
        stampLink.setHasFile(hasStampFile);
    }

    public boolean readStamp(final TargetsTableTimeStampImpl useStampTable) {
        long readStart = System.nanoTime();

        boolean didRead;

        File stampFile = getStampFile();

        if ( getUseBinaryFormat() ) {
            Util_Consumer<TargetCacheImpl_ReaderBinary, IOException> readAction =
                new Util_Consumer<TargetCacheImpl_ReaderBinary, IOException>() {
                    public void accept(TargetCacheImpl_ReaderBinary reader) throws IOException {
                        reader.readEntire(useStampTable);
                    }
                };
            didRead = readBinary(stampFile, !DO_READ_STRINGS, !DO_READ_FULL, readAction);
        } else {
            didRead = read(stampFile, useStampTable);
        }

        @SuppressWarnings("unused")
        long readDuration = addReadTime(readStart, "Stamp");

        return didRead;
    }

    private void writeStamp(
        TargetCacheImpl_DataMod modData,
        final TargetsTableTimeStampImpl useStampTable) {

        Util_Consumer<TargetCacheImpl_Writer, IOException> writeAction;
        Util_Consumer<TargetCacheImpl_WriterBinary, IOException> writeActionBinary;

        if ( getUseBinaryFormat() ) {
            writeAction = null;
            writeActionBinary =
                new Util_Consumer<TargetCacheImpl_WriterBinary, IOException>() {
                    public void accept(TargetCacheImpl_WriterBinary useWriter) throws IOException {
                        useWriter.writeEntire(useStampTable);
                    }
                };
        } else {
            writeAction =
                new Util_Consumer<TargetCacheImpl_Writer, IOException>() {
                    public void accept(TargetCacheImpl_Writer useWriter) throws IOException {
                        useWriter.write(useStampTable);
                    }
                };
            writeActionBinary = null;
        }

        modData.scheduleWrite(
            "Stamp", getStampFile(), DO_TRUNCATE,
            writeAction, writeActionBinary);
    }

    // File structure for combined data (stamp plus classes plus annotation
    // targets).
    //
    // Only used when data is separate and jandex format is not being used.

    private final TargetCacheImpl_DataFile coreDataLink;
 
    @Trivial
    protected TargetCacheImpl_DataFile getCoreDataLink() {
        return coreDataLink;
    }

    @Trivial
    public File getCoreDataFile() {
        return coreDataLink.getFile();
    }

    @Trivial
    public boolean getHasCoreDataFile() {
        return coreDataLink.getHasFile();
    }

    protected void setHasCoreDataFile(boolean hasCombinedFile) {
        coreDataLink.setHasFile(hasCombinedFile);
    }

    public boolean basicReadCoreData(final TargetsTableImpl targetData) {
        long readStart = System.nanoTime();

        File coreDataFile = getCoreDataFile();

        boolean didRead;

        if ( getUseBinaryFormat() ) {
            Util_Consumer<TargetCacheImpl_ReaderBinary, IOException> readAction =
                new Util_Consumer<TargetCacheImpl_ReaderBinary, IOException>() {
                    public void accept(TargetCacheImpl_ReaderBinary reader) throws IOException {
                        reader.readEntire(
                            targetData.getClassTable(),
                            targetData.getAnnotationTable() );
                    }
                };
            didRead = readBinary(coreDataFile, DO_READ_STRINGS, DO_READ_FULL, readAction);
        } else {
            didRead = read(
                coreDataFile,
                targetData.getClassTable(),
                targetData.getAnnotationTable() );
        }

        @SuppressWarnings("unused")
        long readDuration = addReadTime(readStart, "Core Data");

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
    private void writeCoreData(TargetCacheImpl_DataMod modData, final TargetsTableImpl targetData) {
        Util_Consumer<TargetCacheImpl_Writer, IOException> writeAction;
        Util_Consumer<TargetCacheImpl_WriterBinary, IOException> writeActionBinary;

        if ( getUseBinaryFormat() ) {
            writeAction = null;
            writeActionBinary =
                new Util_Consumer<TargetCacheImpl_WriterBinary, IOException>() {
                    public void accept(TargetCacheImpl_WriterBinary useWriter) throws IOException {
                        useWriter.writeEntire(
                            targetData.getClassTable(),
                            targetData.getAnnotationTable() );
                    }
                };
        } else {
            writeAction =
                new Util_Consumer<TargetCacheImpl_Writer, IOException>() {
                    public void accept(TargetCacheImpl_Writer useWriter) throws IOException {
                        useWriter.write( targetData.getClassTable() );
                        useWriter.write( targetData.getAnnotationTable() );
                    }
                };
            writeActionBinary = null;
        }

        modData.scheduleWrite(
            "Core data", getCoreDataFile(), DO_TRUNCATE,
            writeAction, writeActionBinary);
    }

    private boolean basicReadJandex(TargetsTableImpl targetData) {
        String methodName = "readJandex";

        long readStart = System.nanoTime();

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "Container [ " + getName() + " ] Jandex File [ " + getCoreDataLink() + " ]");
        }

        File coreDataFile = getCoreDataFile();
        String coreDataPath = coreDataFile.getPath();
  
        boolean didRead;

        try {
            SparseIndex sparseIndex = Jandex_Utils.basicReadSparseIndex(coreDataPath);
            targetData.transfer(sparseIndex);
            didRead = true;

        } catch ( IOException e ) {
            List<TargetCache_ParseError> errors = Collections.emptyList(); 
            readError(coreDataFile, e, errors);
            didRead = false;
        }

        @SuppressWarnings("unused")
        long readDuration = addReadTime(readStart, "Jandex");

        // System.out.println("Sparse jandex cache read [ " + getName() + " ] [ " + readDuration + " ]");

        return didRead;
    }

    private void writeJandex(
        TargetCacheImpl_DataMod modData,
        final Index jandexIndex) {

        Util_Consumer<TargetCacheImpl_Writer, IOException> writeAction =
            new Util_Consumer<TargetCacheImpl_Writer, IOException>() {
                public void accept(TargetCacheImpl_Writer useWriter) throws IOException {
                    useWriter.write(jandexIndex);
                }
            };

        modData.scheduleWrite("Jandex", getCoreDataFile(), DO_TRUNCATE, writeAction, null);
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
        stampLink.setHasFile(false);
        coreDataLink.setHasFile(false);
    }

    public boolean hasCoreDataFile() {
        if ( !shouldRead("Full read") ) {
            return false;
        } else {
            return ( getHasCoreDataFile() );
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
    public void writeStamp(TargetCacheImpl_DataMod modData, TargetsTableImpl targetData) {
        if ( !shouldWrite("Time stamp") ) {
            return;
        }

        writeStamp( modData, targetData.getStampTable() );
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

        if ( currentStamp.equals(ClassSource.UNRECORDED_STAMP) ||
             currentStamp.equals(ClassSource.UNAVAILABLE_STAMP) ) {
            if ( !useAlwaysValid ) {
                return "Stamp unavailable";
            }
        }

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

        if ( !getHasStampFile() ) {
            return "No stamp file";
        }

        if ( useAlwaysValid ) {
            return null;
        }

        TargetsTableTimeStampImpl useStampTable =
            new TargetsTableTimeStampImpl(classSourceName);

        if ( !readStamp(useStampTable) ) {
            return "Stamp read failure";
        } else if ( !useStampTable.getStamp().equals(currentStamp) ) {
            return "Stamp change";
        } else {
            return null;
        }
    }
}
