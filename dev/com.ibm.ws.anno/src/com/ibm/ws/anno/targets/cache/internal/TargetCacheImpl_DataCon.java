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
import com.ibm.ws.anno.util.internal.UtilImpl_FileUtils;

public class TargetCacheImpl_DataCon extends TargetCacheImpl_DataBase {

    public static final String CLASS_NAME = TargetCacheImpl_DataCon.class.getSimpleName();

    //

    public TargetCacheImpl_DataCon(
        TargetCacheImpl_Factory factory,
        TargetCacheImpl_DataMod modData,
        String conName, String e_conName, File conDir) {

        super(factory, conName, e_conName, conDir, NO_CHILD_PREFIX);

        this.modData = modData;
    }

    //

    protected final TargetCacheImpl_DataMod modData;

    @Trivial
    public TargetCacheImpl_DataMod getModData() {
        return modData;
    }

    //

    protected void removeStorage() {
        @SuppressWarnings("unused")
        int failedRemovals = UtilImpl_FileUtils.removeAll( logger, getDataDir() );
    }

    //

    protected volatile File timeStampFile;

    public File getTimeStampFile() {
        if ( timeStampFile == null ) {
            synchronized ( this ) {
                if ( timeStampFile == null ) {
                    timeStampFile = getDataFile(TargetCache_ExternalConstants.TIMESTAMP_NAME);
                }
            }
        }
        return timeStampFile;
    }

    public boolean hasTimeStampFile() {
        if ( isDisabled() ) {
            return false;
        }

        return exists( getTimeStampFile() );
    }

    public void write(final TargetsTableTimeStampImpl stampTable) {
        TargetCacheImpl_DataMod.ScheduleCallback writer = new TargetCacheImpl_DataMod.ScheduleCallback() {
            @Override
            public void execute() {
                String methodName = "write.execute";

                long writeStart = System.nanoTime();

                File useTimeStampFile = getTimeStampFile();
                synchronized( useTimeStampFile ) {
                    try {
                        createWriter(useTimeStampFile).write(stampTable);
                        // 'createWriter', 'write' throws IOException
                    } catch ( IOException e ) {
                        // CWWKC00??W
                        logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_TARGETS_CACHE_EXCEPTION [ {0} ]", e);
                        logger.logp(Level.WARNING, CLASS_NAME, methodName, "Cache error", e);
                    }
                }

                @SuppressWarnings("unused")
                long writeDuration = addWriteTime(writeStart, "Write Stamp");
            }
        };

        getModData().scheduleWrite(writer);
    }

    public TargetsTableTimeStampImpl readStampTable() {
    	boolean didRead;

    	long readStart = System.nanoTime();

        TargetsTableTimeStampImpl stampTable = new TargetsTableTimeStampImpl();

        File useTimeStampFile = getTimeStampFile();
        synchronized( useTimeStampFile ) {
        	didRead = read(stampTable, useTimeStampFile);
        }

        @SuppressWarnings("unused")
        long readDuration = addReadTime(readStart, "Read Stamp");

        return ( didRead ? stampTable : null );
    }

    //

    public File getClassRefsFile() {
        return getDataFile(TargetCache_ExternalConstants.CLASS_REFS_NAME);
    }

    public boolean hasClassRefsFile() {
        return ( !isDisabled() && exists( getClassRefsFile() ) );
    }

    public void write(final TargetsTableClassesImpl classesTable) {
        TargetCacheImpl_DataMod.ScheduleCallback writer = new TargetCacheImpl_DataMod.ScheduleCallback() {
            @Override
            public void execute() {
                String methodName = "write.execute";

                long writeStart = System.nanoTime();

                File useClassRefsFile = getClassRefsFile();
                synchronized ( useClassRefsFile ) {
                    try {
                        createWriter(useClassRefsFile).write(classesTable);
                        // 'createWriter', 'write' throws IOException
                    } catch ( IOException e ) {
                        // CWWKC00??W
                        logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_TARGETS_CACHE_EXCEPTION [ {0} ]", e);
                        logger.logp(Level.WARNING, CLASS_NAME, methodName, "Cache error", e);
                    }
                }

                @SuppressWarnings("unused")
                long writeDuration = addWriteTime(writeStart, "Write Classes");
            }
        };

        getModData().scheduleWrite(writer);
    }

    public boolean read(TargetsTableClassesImpl classesTable) {
        if ( isDisabled() ) {
            return false;
        }

        boolean didRead;

        long readStart = System.nanoTime();

        File useClassRefsFile = getClassRefsFile();
        synchronized ( useClassRefsFile ) {
            didRead = read(classesTable, useClassRefsFile);
        }

        @SuppressWarnings("unused")
        long readDuration = addReadTime(readStart, "Read Classes");

        return didRead;
    }

    //

    protected volatile File annoTargetsFile;

    public File getAnnoTargetsFile() {
        if ( annoTargetsFile == null ) {
            synchronized(this) {
                if ( annoTargetsFile == null ) {
                    annoTargetsFile = getDataFile(TargetCache_ExternalConstants.ANNO_TARGETS_NAME);
                }
            }
        }
        return annoTargetsFile;
    }

    public boolean hasAnnoTargetsFile() {
        return ( !isDisabled() && exists( getAnnoTargetsFile() ) );
    }

    public void write(final TargetsTableAnnotationsImpl targetTable) {
        TargetCacheImpl_DataMod.ScheduleCallback writer = new TargetCacheImpl_DataMod.ScheduleCallback() {
            @Override
            public void execute() {
                String methodName = "write.execute";
                
                long writeStart = System.nanoTime();

                File useAnnoTargetsFile = getAnnoTargetsFile();
                synchronized ( useAnnoTargetsFile ) {
                    try {
                        createWriter( getAnnoTargetsFile() ).write(targetTable);
                        // 'createWriter', 'write' throws IOException
                    } catch ( IOException e ) {
                        // CWWKC00??W
                        logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_TARGETS_CACHE_EXCEPTION [ {0} ]", e);
                        logger.logp(Level.WARNING, CLASS_NAME, methodName, "Cache error", e);
                    }
                }

                @SuppressWarnings("unused")
                long writeDuration = addWriteTime(writeStart, "Write Targets");
            }
        };

        getModData().scheduleWrite(writer);
    }

    public boolean read(TargetsTableAnnotationsImpl targetTable) {
        if ( isDisabled() ) {
            return false;
        }

        boolean didRead;

        long readStart = System.nanoTime();

        File useAnnoTargetsFile = getAnnoTargetsFile();
        synchronized ( useAnnoTargetsFile ) {
        	didRead = read(targetTable, useAnnoTargetsFile);
        }

        @SuppressWarnings("unused")
        long readDuration = addReadTime(readStart, "Read Targets");

        return didRead;
    }
    
    public boolean read(TargetsTableTimeStampImpl stampTable) {
        if ( isDisabled() ) {
            return false;
        }

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

    public boolean read(TargetsTableImpl targetData) {
        if ( isDisabled() ) {
            return false;
        }

        return read( targetData.getClassTable() ) && 
               read( targetData.getTargetTable() ) &&
               read( targetData.getStampTable() );
    }

    public void write(TargetsTableImpl targetData) {
        if ( !shouldWrite( getDataDir(), "Container data" ) ) {
            return;
        }

        write( targetData.getClassTable() );
        write( targetData.getTargetTable() );
        write( targetData.getStampTable() );
    }

    public void writeStamp(TargetsTableImpl targetData) {
        write ( targetData.getStampTable() );
    }
}

