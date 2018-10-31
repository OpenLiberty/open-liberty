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
import java.io.IOException;
import java.util.logging.Level;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.anno.targets.internal.TargetsTableClassesImpl;
import com.ibm.ws.anno.targets.internal.TargetsTableImpl;
import com.ibm.ws.anno.targets.internal.TargetsTableTimeStampImpl;
import com.ibm.wsspi.anno.targets.cache.TargetCache_ExternalConstants;
import com.ibm.ws.anno.targets.internal.TargetsTableAnnotationsImpl;

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
 * {@link com.ibm.wsspi.anno.targets.cache.TargetCache_ExternalConstants#SEED_RESULT_NAME},
 * {@link com.ibm.wsspi.anno.targets.cache.TargetCache_ExternalConstants#PARTIAL_RESULT_NAME},
 * {@link com.ibm.wsspi.anno.targets.cache.TargetCache_ExternalConstants#EXCLUDED_RESULT_NAME}, or
 * {@link com.ibm.wsspi.anno.targets.cache.TargetCache_ExternalConstants#EXTERNAL_RESULT_NAME}.
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
    private static final String CALLBACK_CLASS_NAME = CLASS_NAME + "$ScheduleCallback";

    //

    // The container data is relative to the enclosing application,
    // not to the enclosing module.
    //
    // Whether a container directory is available depends on whether the
    // application directory is available, not on whether the module
    // directory is available.

    public TargetCacheImpl_DataCon(
        TargetCacheImpl_DataBase parentCache,
        String conName, String e_conName, File conDir) {

        super( parentCache.getFactory(), conName, e_conName, conDir);

        String methodName = "<init>";

        this.parentCache = parentCache;

        // When the parent application is unnamed, the container
        // directory is null, which means the three container cache files
        // are null: No writes and no reads will be performed.

        this.timeStampFile =
            getDataFile(TargetCache_ExternalConstants.TIMESTAMP_NAME);
        this.annoTargetsFile =
            getDataFile(TargetCache_ExternalConstants.ANNO_TARGETS_NAME);
        this.classRefsFile =
            getDataFile(TargetCache_ExternalConstants.CLASS_REFS_NAME);

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "Container [ {0} ] of [ {1} ]",
                        new Object[] { getName(), parentCache.getName() });
            logger.logp(Level.FINER, CLASS_NAME,
                        "Time stamp file [ {0} ]",
                        ((this.timeStampFile == null) ? null : this.timeStampFile.getPath()));
            logger.logp(Level.FINER, CLASS_NAME,
                        "Targets file [ {0} ]",
                        ((this.annoTargetsFile == null) ? null : this.annoTargetsFile.getPath()));
            logger.logp(Level.FINER, CLASS_NAME,
                        "Class refs file [ {0} ]",
                        ((this.classRefsFile == null) ? null : this.classRefsFile.getPath()));
        }
    }

    //

    // DataApp for simple container data.
    // DataMod for policy container data.

    private final TargetCacheImpl_DataBase parentCache;

    @Trivial
    public TargetCacheImpl_DataBase getParentCache() {
        return parentCache;
    }

    //

    private final File timeStampFile;

    @Trivial
    public File getTimeStampFile() {
        return timeStampFile;
    }

    // Answers false if the stamp file is null.

    public boolean hasTimeStampFile() {
        return ( exists( getTimeStampFile() ) );
    }

    //

    private final File annoTargetsFile;

    public File getAnnoTargetsFile() {
        return annoTargetsFile;
    }

    // Answers false if the targets file is null.

    public boolean hasAnnoTargetsFile() {
        return ( exists( getAnnoTargetsFile() ) );
    }

    //

    private final File classRefsFile;

    @Trivial
    public File getClassRefsFile() {
        return classRefsFile;
    }

    // Answers false if the class refs file is null.

    public boolean hasClassRefsFile() {
        return ( exists( getClassRefsFile() ) );
    }

    //

    // 'write' cannot be entered if the stamp file is null.

    private void write(
        TargetCacheImpl_DataMod modData,
        final TargetsTableTimeStampImpl stampTable) {

        final String writeDescription;
        if ( logger.isLoggable(Level.FINER) ) {
            writeDescription = "Container [ " + getName() + " ] TimeStamp [ " + getTimeStampFile().getPath() + " ]";
        } else {
            writeDescription = null;
        }

        TargetCacheImpl_DataMod.ScheduleCallback writer = new TargetCacheImpl_DataMod.ScheduleCallback() {
            @Override
            public void execute() {
                String methodName = "write.execute";
                if ( writeDescription != null ) {
                    logger.logp(Level.FINER, CALLBACK_CLASS_NAME, methodName, "ENTER {0}", writeDescription);
                }

                long writeStart = System.nanoTime();

                try {
                    createWriter( getTimeStampFile() ).write(stampTable);
                    // 'createWriter', 'write' throws IOException
                } catch ( IOException e ) {
                    // CWWKC00??W
                    logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_TARGETS_CACHE_EXCEPTION [ {0} ]", e);
                    logger.logp(Level.WARNING, CLASS_NAME, methodName, "Cache error", e);
                }

                @SuppressWarnings("unused")
                long writeDuration = addWriteTime(writeStart, writeDescription);

                if ( writeDescription != null ) {
                    logger.logp(Level.FINER, CALLBACK_CLASS_NAME, methodName, "RETURN {0}", writeDescription);
                }
            }
        };

        modData.scheduleWrite(writer, writeDescription);
    }

    // 'write' cannot be entered if the class refs file is null.

    private void write(
        TargetCacheImpl_DataMod modData,
        final TargetsTableClassesImpl classesTable) {

        final String writeDescription;
        if ( logger.isLoggable(Level.FINER) ) {
            writeDescription = "Container [ " + getName() + " ] Class references [ " + getClassRefsFile().getPath() + " ]";
        } else {
            writeDescription = null;
        }

        TargetCacheImpl_DataMod.ScheduleCallback writer = new TargetCacheImpl_DataMod.ScheduleCallback() {
            @Override
            public void execute() {
                String methodName = "write.execute";
                if ( writeDescription != null ) {
                    logger.logp(Level.FINER, CALLBACK_CLASS_NAME, methodName, "ENTER {0}", writeDescription);
                }

                long writeStart = System.nanoTime();

                try {
                    createWriter( getClassRefsFile() ).write(classesTable);
                    // 'createWriter', 'write' throws IOException
                } catch ( IOException e ) {
                    // CWWKC00??W
                    logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_TARGETS_CACHE_EXCEPTION [ {0} ]", e);
                    logger.logp(Level.WARNING, CLASS_NAME, methodName, "Cache error", e);
                }

                @SuppressWarnings("unused")
                long writeDuration = addWriteTime(writeStart, writeDescription);

                if ( writeDescription != null ) {
                    logger.logp(Level.FINER, CALLBACK_CLASS_NAME, methodName, "RETURN {0}", writeDescription);
                }
            }
        };

        modData.scheduleWrite(writer, writeDescription);
    }

    // 'write' cannot be entered if the targets file is null.

    private void write(
        TargetCacheImpl_DataMod modData,
        final TargetsTableAnnotationsImpl targetTable) {

        final String writeDescription;
        if ( logger.isLoggable(Level.FINER) ) {
            writeDescription = "Container [ " + getName() + " ] Targets [ " + getAnnoTargetsFile().getPath() + " ]";
        } else {
            writeDescription = null;
        }

        TargetCacheImpl_DataMod.ScheduleCallback writer = new TargetCacheImpl_DataMod.ScheduleCallback() {
            @Override
            public void execute() {
                String methodName = "write.execute";
                if ( writeDescription != null ) {
                    logger.logp(Level.FINER, CALLBACK_CLASS_NAME, methodName, "ENTER {0}", writeDescription);
                }

                long writeStart = System.nanoTime();

                try {
                    createWriter( getAnnoTargetsFile() ).write(targetTable);
                    // 'createWriter', 'write' throws IOException
                } catch ( IOException e ) {
                    // CWWKC00??W
                    logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_TARGETS_CACHE_EXCEPTION [ {0} ]", e);
                    logger.logp(Level.WARNING, CLASS_NAME, methodName, "Cache error", e);
                }

                @SuppressWarnings("unused")
                long writeDuration = addWriteTime(writeStart, writeDescription);

                if ( writeDescription != null ) {
                    logger.logp(Level.FINER, CALLBACK_CLASS_NAME, methodName, "RETURN {0}", writeDescription);
                }
            }
        };

        modData.scheduleWrite(writer, writeDescription);
    }

    //

    private boolean read(TargetsTableTimeStampImpl stampTable) {
        boolean didRead;

        long readStart = System.nanoTime();

        TargetsTableTimeStampImpl readStampTable = readStampTable();
        if ( readStampTable == null ) {
            didRead = false;
        } else {
            didRead = true;
            stampTable.setName( readStampTable.getName() );
            stampTable.setStamp( readStampTable.getStamp() );
        }

        @SuppressWarnings("unused")
        long readDuration = addReadTime(readStart, "Read Stamp");

        return didRead;
    }

    // 'read(TargetsTableClassesImpl)' cannot be entered if the class refs file is null.

    private boolean read(TargetsTableClassesImpl classesTable) {
        long readStart = System.nanoTime();

        boolean didRead = read( classesTable, getClassRefsFile() );

        @SuppressWarnings("unused")
        long readDuration = addReadTime(readStart, "Read Classes");

        return didRead;
    }

    // 'read(TargetsTableAnnotationsImpl)' cannot be entered if the targets file is null.

    private boolean read(TargetsTableAnnotationsImpl targetTable) {
        long readStart = System.nanoTime();

        boolean didRead = read( targetTable, getAnnoTargetsFile() );

        @SuppressWarnings("unused")
        long readDuration = addReadTime(readStart, "Read Targets");

        return didRead;
    }

    //

    // 'readStampTable' cannot be entered if the stamp file is null.

    public TargetsTableTimeStampImpl readStampTable() {
        long readStart = System.nanoTime();

        TargetsTableTimeStampImpl stampTable = new TargetsTableTimeStampImpl();

        boolean didRead = read( stampTable, getTimeStampFile() );

        @SuppressWarnings("unused")
        long readDuration = addReadTime(readStart, "Read Stamp");

        return ( didRead ? stampTable : null );
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
        if ( !getParentCache().shouldRead(inputDescription) ) {
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
        if ( !getParentCache().shouldWrite(outputDescription) ) {
            return false;
        } else {
            return super.shouldWrite(outputDescription);
        }
    }

    //

    public boolean hasFiles() {
        return ( hasTimeStampFile() &&  hasAnnoTargetsFile() && hasClassRefsFile() );
    }

    public boolean read(TargetsTableImpl targetData) {
        return ( read( targetData.getClassTable() ) && 
                 read( targetData.getAnnotationTable() ) &&
                 read( targetData.getStampTable() ) );
    }

    //

    /**
     * Write stamp information.
     * 
     * Module data is needed, since writes are performed at the module level.  The
     * module data cannot be stored in the container data because the container data
     * is shared between modules.
     * 
     * @param modData The module which will perform the write.
     * @param targetData The data containing the stamp table which is to be written.
     */
    public void writeStamp(TargetCacheImpl_DataMod modData, TargetsTableImpl targetData) {
        write ( modData, targetData.getStampTable() );
    }

    /**
     * Write all data: The class table, the annotations table, and the tsamp table..
     *
     * Module data is needed, since writes are performed at the module level.  The
     * module data cannot be stored in the container data because the container data
     * is shared between modules.
     *
     * @param modData The module which will perform the write.
     * @param targetData The data which is to be written.
     */
    public void write(TargetCacheImpl_DataMod modData, TargetsTableImpl targetData) {
        write( modData, targetData.getClassTable() );
        write( modData, targetData.getAnnotationTable() );
        write( modData, targetData.getStampTable() );
    }
}

