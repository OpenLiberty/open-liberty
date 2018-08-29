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
import com.ibm.ws.anno.targets.cache.TargetCache_ExternalConstants;
import com.ibm.ws.anno.targets.internal.TargetsTableClassesImpl;
import com.ibm.ws.anno.targets.internal.TargetsTableImpl;
import com.ibm.ws.anno.targets.internal.TargetsTableTimeStampImpl;
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
 * {@link com.ibm.ws.anno.targets.cache.TargetCache_ExternalConstants#SEED_RESULT_NAME},
 * {@link com.ibm.ws.anno.targets.cache.TargetCache_ExternalConstants#PARTIAL_RESULT_NAME},
 * {@link com.ibm.ws.anno.targets.cache.TargetCache_ExternalConstants#EXCLUDED_RESULT_NAME}, or
 * {@link com.ibm.ws.anno.targets.cache.TargetCache_ExternalConstants#EXTERNAL_RESULT_NAME}.
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
    public static final String CLASS_NAME = TargetCacheImpl_DataCon.class.getSimpleName();

    //

    public TargetCacheImpl_DataCon(
        TargetCacheImpl_DataBase parentData,
        String conName, String e_conName, File conDir) {

        super( parentData.getFactory(), conName, e_conName, conDir);

        this.parentData = parentData;

        this.timeStampFile =
            getDataFile(TargetCache_ExternalConstants.TIMESTAMP_NAME);
        this.annoTargetsFile =
            getDataFile(TargetCache_ExternalConstants.ANNO_TARGETS_NAME);
        this.classRefsFile =
            getDataFile(TargetCache_ExternalConstants.CLASS_REFS_NAME);
    }

    //

    // DataApp for shared non-results container data.
    // DataMod for non-shared results container data.

    private final TargetCacheImpl_DataBase parentData;

    @Trivial
    public TargetCacheImpl_DataBase getParentData() {
        return parentData;
    }

    //

    private final File timeStampFile;

    @Trivial
    public File getTimeStampFile() {
        return timeStampFile;
    }

    public boolean hasTimeStampFile() {
        return ( exists( getTimeStampFile() ) );
    }

    //

    private final File annoTargetsFile;

    public File getAnnoTargetsFile() {
        return annoTargetsFile;
    }

    public boolean hasAnnoTargetsFile() {
        return ( exists( getAnnoTargetsFile() ) );
    }

    //

    private final File classRefsFile;

    @Trivial
    public File getClassRefsFile() {
    	return classRefsFile;
    }

    public boolean hasClassRefsFile() {
        return ( exists( getClassRefsFile() ) );
    }

    //

    private void write(
        TargetCacheImpl_DataMod modData,
        final TargetsTableTimeStampImpl stampTable) {

        TargetCacheImpl_DataMod.ScheduleCallback writer = new TargetCacheImpl_DataMod.ScheduleCallback() {
            @Override
            public void execute() {
                String methodName = "write.execute";

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
                long writeDuration = addWriteTime(writeStart, "Write Stamp");
            }
        };

        modData.scheduleWrite(writer);
    }

    private void write(
        TargetCacheImpl_DataMod modData,
        final TargetsTableClassesImpl classesTable) {

        TargetCacheImpl_DataMod.ScheduleCallback writer = new TargetCacheImpl_DataMod.ScheduleCallback() {
            @Override
            public void execute() {
                String methodName = "write.execute";

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
                long writeDuration = addWriteTime(writeStart, "Write Classes");
            }
        };

        modData.scheduleWrite(writer);
    }

    private void write(
        TargetCacheImpl_DataMod modData,
        final TargetsTableAnnotationsImpl targetTable) {

        TargetCacheImpl_DataMod.ScheduleCallback writer = new TargetCacheImpl_DataMod.ScheduleCallback() {
            @Override
            public void execute() {
                String methodName = "write.execute";

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
                long writeDuration = addWriteTime(writeStart, "Write Targets");
            }
        };

        modData.scheduleWrite(writer);
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

    private boolean read(TargetsTableClassesImpl classesTable) {
        long readStart = System.nanoTime();

        boolean didRead = read( classesTable, getClassRefsFile() );

        @SuppressWarnings("unused")
        long readDuration = addReadTime(readStart, "Read Classes");

        return didRead;
    }

    private boolean read(TargetsTableAnnotationsImpl targetTable) {
        long readStart = System.nanoTime();

        boolean didRead = read( targetTable, getAnnoTargetsFile() );

        @SuppressWarnings("unused")
        long readDuration = addReadTime(readStart, "Read Targets");

        return didRead;
    }

    //

    public TargetsTableTimeStampImpl readStampTable() {
    	long readStart = System.nanoTime();

        TargetsTableTimeStampImpl stampTable = new TargetsTableTimeStampImpl();

        boolean didRead = read( stampTable, getTimeStampFile() );

        @SuppressWarnings("unused")
        long readDuration = addReadTime(readStart, "Read Stamp");

        return ( didRead ? stampTable : null );
    }

    //

    @Trivial
    public boolean shouldWrite(TargetCacheImpl_DataMod modData, String outputDescription) {
        return ( modData.shouldWrite(outputDescription) );
    }

    @Trivial
    public boolean shouldRead(TargetCacheImpl_DataMod modData, String inputDescription) {
        return modData.shouldRead(inputDescription);
    }

    //

    public boolean hasFiles() {
    	return ( hasTimeStampFile() &&  hasAnnoTargetsFile() && hasClassRefsFile() );
    }

    public boolean read(TargetsTableImpl targetData) {
        return ( read( targetData.getClassTable() ) && 
                 read( targetData.getTargetTable() ) &&
                 read( targetData.getStampTable() ) );
    }

    //

    public void writeStamp(TargetCacheImpl_DataMod modData, TargetsTableImpl targetData) {
        write ( modData, targetData.getStampTable() );
    }

    public void write(TargetCacheImpl_DataMod modData, TargetsTableImpl targetData) {
        write( modData, targetData.getClassTable() );
        write( modData, targetData.getTargetTable() );
        write( modData, targetData.getStampTable() );
    }
}

