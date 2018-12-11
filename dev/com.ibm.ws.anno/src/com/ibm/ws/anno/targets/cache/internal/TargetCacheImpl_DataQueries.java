/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.anno.targets.cache.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Logger;

import com.ibm.ws.anno.service.internal.AnnotationServiceImpl_Logging;
import com.ibm.ws.anno.util.internal.UtilImpl_FileUtils;
import com.ibm.wsspi.anno.targets.cache.TargetCache_ExternalConstants;

/**
 * Query data
 *
 * Held weakly by the applications object.  Associated with module data,
 * but managed independently.
 */
public class TargetCacheImpl_DataQueries implements TargetCache_ExternalConstants {
    // private static final String CLASS_NAME = TargetCacheImpl_DataQueries.class.getSimpleName();
    protected static final Logger logger = AnnotationServiceImpl_Logging.ANNO_LOGGER;

    //

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

        this.isSetWriter = ( queriesFile == null );
        this.writer = null;
    }

    private final TargetCacheImpl_Factory factory;

    public TargetCacheImpl_Factory getFactory() {
        return factory;
    }

    //

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

    //

    private final File modFile;
    private final File queriesFile;

    public File getModFile() {
        return modFile;
    }

    public File getQueriesFile() {
        return queriesFile;
    }

    //

    private boolean isSetWriter;
    private TargetCacheImpl_Writer writer;

    // Synchronized: Only create the query file (and write the query file header)
    // at most once.
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
                outputStream = UtilImpl_FileUtils.createFileOutputStream(queriesFile, UtilImpl_FileUtils.DO_NOT_APPEND);
            } catch ( IOException e ) {
                return null; // FFDC
            }

            writer = factory.createWriter( queriesFile.getPath(), outputStream );

            if ( writer != null ) {
                if ( isNew ) {
                    try {
                        writer.writeQueryHeader(); // throws IOException
                    } catch ( IOException e ) {
                        // FFDC
                    }
                }
            }
        }

        return writer;
    }

    // Goal: ( isSetWriter && (writer == null) )

    protected void finalize() throws Throwable {
        if ( !isSetWriter ) {
            isSetWriter = true;
            // 'writer' is already null.

        } else if ( writer != null ) {
            TargetCacheImpl_Writer useWriter = writer;
            writer = null;

            useWriter.writeTrailer(); // 'close' in 'writeTrailer'
        }
    }

    // Synchronized: The query data may be used by more than one targets table.
    public synchronized void writeQuery(
        String title,
        int policies, String type,
        Collection<String> specificClasses,
        String annotationClass,
        Collection<String> resultClasses) {

        TargetCacheImpl_Writer useWriter = getWriter();
        if ( useWriter == null ) {
            return;
        }

        try {
            useWriter.writeQuery(title, policies, type, specificClasses, annotationClass, resultClasses); // throws IOException
        } catch ( IOException e ) {
            // FFDC
        }
    }

    // Synchronized: The query data may be used by more than one targets table.
    public synchronized void writeQuery(
        String title,
        Collection<String> sources,
        String type,
        Collection<String> specificClasses,
        String annotationClass,
        Collection<String> resultClasses) {

        TargetCacheImpl_Writer useWriter = getWriter();
        if ( useWriter == null ) {
            return;
        }

        try {
            useWriter.writeQuery(title,
                                 sources,
                                 type,
                                 specificClasses, annotationClass,
                                 resultClasses); // throws IOException
        } catch ( IOException e ) {
            // FFDC
        }
    }

    // Synchronized: The query data may be used by more than one targets table.
    public synchronized void writeQuery(
        String title,
        int policies, Collection<String> sources,
        String type,
        Collection<String> specificClasses,
        String annotationClass,
        Collection<String> resultClasses) {

        TargetCacheImpl_Writer useWriter = getWriter();
        if ( useWriter == null ) {
            return;
        }

        try {
            useWriter.writeQuery(title,
                                 policies, sources,
                                 type,
                                 specificClasses, annotationClass,
                                 resultClasses); // throws IOException
        } catch ( IOException e ) {
            // FFDC
        }
    }
}
