/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.annocache.service.internal.AnnotationCacheServiceImpl_Logging;
import com.ibm.ws.annocache.util.internal.UtilImpl_FileUtils;
import com.ibm.wsspi.annocache.targets.cache.TargetCache_ExternalConstants;

/**
 * Query data.
 *
 * Held weakly by the applications object under the app name and full module name.
 *
 * Held strongly by annotation targets data.
 *
 * The queries file may already exist, indicating that the targets data was obtained
 * from the cache.
 *
 * Each time the queries data is created, the first time the writer is obtained
 * a header is written to the queries file.  Finalizing the queries data causes
 * a trailer to be written to the queries file, and causes the writer to be closed.
 */
public class TargetCacheImpl_DataQueries implements TargetCache_ExternalConstants {
    // Logging ...

    // private static final String CLASS_NAME = TargetCacheImpl_DataQueries.class.getSimpleName();
    protected static final Logger logger = AnnotationCacheServiceImpl_Logging.ANNO_LOGGER;

    protected static final Logger queryLogger = AnnotationCacheServiceImpl_Logging.ANNO_QUERY_LOGGER;

    // Life-cycle ...

    /**
     * Create queries data for a specified module.
     *
     * The module file may be null.  This indicates that the parent targets data is not
     * being written to the cache, either because the application or module names are not
     * known, or because cache writes are not enabled.
     *
     * @param factory The factory which is creating this queries data.
     * @param appName The application name.  May be null.
     * @param e_appName The encoded application name.  May be null.
     * @param modName The module name.  May be null.
     * @param e_modName The encoded module name.  May be null.
     * @param modFile The module cache directory.  May be null.
     */
    public TargetCacheImpl_DataQueries(
        TargetCacheImpl_Factory factory,
        String appName, String e_appName,
        String modName, String e_modName, File modFile) {

        this.factory = factory;

        this.appName = appName;
        this.e_appName = e_appName;

        this.modName = modName;
        this.e_modName = e_modName;

        this.modFile = modFile;
        this.queriesFile = ( (modFile == null) ? null : new File(modFile, QUERIES_NAME) );

        // 'isSetWriter == true' with 'writer == null' is correct for 'queriesFile' being null.

        this.isSetWriter = ( queriesFile == null );
        this.writer = null;
    }

    private final TargetCacheImpl_Factory factory;

    public TargetCacheImpl_Factory getFactory() {
        return factory;
    }

    // Application and module naming ...

    private final String appName;
    private final String e_appName;

    private final String modName;
    private final String e_modName;

    public String getAppName() {
        return appName;
    }

    public String e_getAppName() {
        return e_appName;
    }

    public String getModName() {
        return modName;
    }

    public String e_getModName() {
        return e_modName;
    }

    // Module and queries file ...

    // The module file may be null, in which case the queries file
    // will be null.
    //
    // A non-null queries file does not indicate that the actual file
    // exists: The actual file is not necessarily created until the
    // writer is obtained the first time.
    //
    // The actual queries file, if it exists before the writer is obtained,
    // is from prior access to the annotations cache.

    private final File modFile;
    private final File queriesFile;

    public File getModFile() {
        return modFile;
    }

    public File getQueriesFile() {
        return queriesFile;
    }

    // Writer state ...

    // 'isSetWriter' tells if the writer has been processed.
    //
    // There are three valid states:
    //
    // 'isSetWriter == false' and 'writer == null'
    //   A module file is available; the writer has not yet been obtained;
    //   the queries data is not yet finalized.
    // 'isSetWriter == true'  and 'writer != null'
    //   A module file is available; the writer has been obtained;
    //   the queries data is not yet finalized.
    // 'isSetWriter == true'  and 'writer == null'
    //   Either, the module file is not available (and the writer
    //   may or may not have been requested, and the queries data might
    //   or might not be finalized), or the module file is available and
    //   the queries data is finalized (and the writer may or may not have
    //   been requested).
    //
    // The fourth state is not valid:
    //
    // 'isSetWriter == false' and 'writer != null'
    //
    private boolean isSetWriter;
    private TargetCacheImpl_Writer writer;

    /**
     * Obtain the queries writer.  Create and assign it if necessary.
     * Create any absent parent directories.
     *
     * Do not truncate an already present queries file.
     *
     * Write a new header every time the queries data is opened.
     *
     * Answer null if no module file was set.  This disables query
     * logging.
     *
     * (Answer null if the queries data is finalized.  Such a call
     * should not be possible.)
     *
     * @return The writer of this queries data.
     */
    private synchronized TargetCacheImpl_Writer getWriter() {
        if ( !isSetWriter ) {
            isSetWriter = true;

            boolean isNew = !UtilImpl_FileUtils.exists(queriesFile);

            if ( isNew ) {
                if ( !UtilImpl_FileUtils.ensureDir(logger, modFile) ) {
                    return null; // Logging in 'ensureDir'.
                }
            }

            FileOutputStream outputStream;
            try {
                outputStream = UtilImpl_FileUtils.createFileOutputStream(queriesFile, UtilImpl_FileUtils.DO_APPEND);
            } catch ( IOException e ) {
                return null; // FFDC
            }

            writer = factory.createWriter( queriesFile.getPath(), outputStream );

            // A null writer is unexpected.  Currently, this is only possible
            // if the serialization encoding, currently set to UTF-8, is not valid,
            // which should never be the case.

            if ( writer != null ) {
                try {
                    writer.writeQueryHeader(); // throws IOException
                } catch ( IOException e ) {
                    // FFDC
                }
            }
        }

        return writer;
    }

    // Goal: ( isSetWriter && (writer == null) )

    /**
     * Finalize this queries data.
     *
     * If the writer is set and not null, write a trailer then close
     * the writer.
     *
     * The end state is that 'isSetWriter' should be true and 'writer'
     * is null.
     *
     * @throws Throwable Thrown in case of an error finalizing the queries
     *     data.  Never thrown by this implementation: The throws declaration
     *     is present to preserve the 'finalize' API.
     */
    @Override
    protected void finalize() throws Throwable {
        if ( !isSetWriter ) {
            isSetWriter = true;
            // 'writer' is already null.

        } else if ( writer != null ) {
            TargetCacheImpl_Writer useWriter = writer;
            writer = null;

            try {
                useWriter.writeTrailer();
            } finally {
                useWriter.close();
            }
        }
    }

    // Query writers ...

    public synchronized void writeQuery(
        String methodName, String className,
        String title,
        int policies, String annoType,
        Collection<String> specificClasses,
        String annotationClass,
        Collection<String> resultClasses) {

        TargetCacheImpl_Writer useWriter = getWriter();
        if ( useWriter == null ) {
            return;
        }

        // The writer is flushed at the conclusion of 'writeQuery'.

        try {
            useWriter.writeQuery(
                className, methodName,
                title,
                policies, annoType,
                specificClasses,
                annotationClass,
                resultClasses); // throws IOException
        } catch ( IOException e ) {
            // FFDC
        }
    }

    public synchronized void writeQuery(
        String methodName, String className,
        String title,
        Collection<String> sources, String annoType,
        Collection<String> specificClasses,
        String annotationClass,
        Collection<String> resultClasses) {

        TargetCacheImpl_Writer useWriter = getWriter();
        if ( useWriter == null ) {
            return;
        }

        // The writer is flushed at the conclusion of 'writeQuery'.

        try {
            useWriter.writeQuery(
                className, methodName,
                title,
                sources, annoType,
                specificClasses,
                annotationClass,
                resultClasses); // throws IOException
        } catch ( IOException e ) {
            // FFDC
        }
    }

    public synchronized void writeQuery(
        String methodName, String className,
        String title,
        int policies, Collection<String> sources, String annoType,
        Collection<String> specificClasses,
        String annotationClass,
        Collection<String> resultClasses) {

        TargetCacheImpl_Writer useWriter = getWriter();
        if ( useWriter == null ) {
            return;
        }

        // The writer is flushed at the conclusion of 'writeQuery'.

        try {
            useWriter.writeQuery(
                className, methodName,
                title,
                policies, sources, annoType,
                specificClasses,
                annotationClass,
                resultClasses); // throws IOException
        } catch ( IOException e ) {
            // FFDC
        }
    }

    //

    public static boolean getLogQueries() {
        return ( queryLogger.isLoggable(Level.FINER) );
    }

    public static void logQuery(
        String className, String methodName,
        String title,
        int policies, String annoType,
        Collection<String> specificClasses,
        String annotationClass,
        Collection<String> resultClasses) {

        StringBuilder builder = new StringBuilder();
        TargetCacheImpl_Writer.logQuery(builder,
            title,
            policies, annoType,
            specificClasses,
            annotationClass,
            resultClasses);

        queryLogger.logp(Level.FINER, className, methodName, builder.toString());
    }

    public static void logQuery(
        String className, String methodName,
        String title,
        Collection<String> sources, String annoType,
        Collection<String> specificClasses,
        String annotationClass,
        Collection<String> resultClasses) {

        StringBuilder builder = new StringBuilder();
        TargetCacheImpl_Writer.logQuery(builder,
            title,
            sources, annoType,
            specificClasses,
            annotationClass,
            resultClasses);

        queryLogger.logp(Level.FINER, className, methodName, builder.toString());
    }

    public static void logQuery(
        String className, String methodName,
        String title,
        int policies, Collection<String> sources, String annoType,
        Collection<String> specificClasses,
        String annotationClass,
        Collection<String> resultClasses) {

        StringBuilder builder = new StringBuilder();

        TargetCacheImpl_Writer.logQuery(builder,
            title,
            policies, sources, annoType,
            specificClasses,
            annotationClass,
            resultClasses);

        queryLogger.logp(Level.FINER, className, methodName, builder.toString());
    }
}
