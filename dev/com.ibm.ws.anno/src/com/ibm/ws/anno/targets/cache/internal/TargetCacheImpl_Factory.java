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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.anno.service.internal.AnnotationServiceImpl_Logging;
import com.ibm.ws.anno.service.internal.AnnotationServiceImpl_Service;
import com.ibm.ws.anno.targets.cache.TargetCache_ParseError;
import com.ibm.ws.anno.targets.internal.TargetsTableClassesImpl;
import com.ibm.ws.anno.targets.internal.TargetsTableContainersImpl;
import com.ibm.ws.anno.targets.internal.TargetsTableDetailsImpl;
import com.ibm.ws.anno.targets.internal.TargetsTableTimeStampImpl;
import com.ibm.ws.anno.targets.internal.TargetsTableAnnotationsImpl;
import com.ibm.ws.anno.util.internal.UtilImpl_FileUtils;
import com.ibm.ws.anno.util.internal.UtilImpl_Utils;
import com.ibm.wsspi.anno.targets.cache.TargetCache_Factory;
import com.ibm.wsspi.anno.targets.cache.TargetCache_InternalConstants;
import com.ibm.wsspi.anno.targets.cache.TargetCache_Options;

public class TargetCacheImpl_Factory implements TargetCache_Factory {
    private static final String CLASS_NAME = TargetCacheImpl_Factory.class.getSimpleName();

    protected static final Logger logger = AnnotationServiceImpl_Logging.ANNO_LOGGER;

    @Trivial
    private static void finer(String methodName, String text) {
        logger.logp(Level.FINER, CLASS_NAME, methodName, text);
    }

    @Trivial
    private static void finer(String methodName, String text, Object parm) {
        logger.logp(Level.FINER, CLASS_NAME, methodName, text, parm);
    }

    //
    
    @Trivial
    protected static String getSystemProperty(String propertyName, String defaultValue) {
        return UtilImpl_Utils.getSystemProperty(logger, propertyName, defaultValue);
    }

    @Trivial
    protected static boolean getSystemProperty(String propertyName, boolean defaultValue) {
        return UtilImpl_Utils.getSystemProperty(logger, propertyName, defaultValue);
    }

    @Trivial
    protected static int getSystemProperty(String propertyName, int defaultValue) {
        return UtilImpl_Utils.getSystemProperty(logger, propertyName, defaultValue);
    }

    //

    public TargetCacheImpl_Factory(AnnotationServiceImpl_Service annoService) {
        this.annoService = annoService;
    }

    //

    protected final AnnotationServiceImpl_Service annoService;

    public AnnotationServiceImpl_Service getAnnotationService() {
        return annoService;
    }

    //

    public static TargetCacheImpl_Options createOptionsFromProperties() {
        boolean disabled       = getSystemProperty(TargetCache_Options.DISABLED_PROPERTY_NAME, TargetCache_Options.DISABLED_DEFAULT);

        String dir             = getSystemProperty(TargetCache_Options.DIR_PROPERTY_NAME, TargetCache_Options.DIR_DEFAULT);
        boolean readOnly       = getSystemProperty(TargetCache_Options.READ_ONLY_PROPERTY_NAME, TargetCache_Options.READ_ONLY_DEFAULT);
        boolean alwaysValid    = getSystemProperty(TargetCache_Options.ALWAYS_VALID_PROPERTY_NAME, TargetCache_Options.ALWAYS_VALID_DEFAULT);
        // boolean validate       = getSystemProperty(TargetCache_Options.VALIDATE_PROPERTY_NAME, TargetCache_Options.VALIDATE_DEFAULT);
        int writeThreads       = getSystemProperty(TargetCache_Options.WRITE_THREADS_PROPERTY_NAME, TargetCache_Options.WRITE_THREADS_DEFAULT);

        return new TargetCacheImpl_Options(
            disabled,
            dir,
            readOnly, alwaysValid,
            // validate,
            writeThreads);
    }

    public static TargetCacheImpl_Options createOptionsFromDefaults() {
        return new TargetCacheImpl_Options(
            TargetCache_Options.DISABLED_DEFAULT,

             TargetCache_Options.DIR_DEFAULT,
             TargetCache_Options.READ_ONLY_DEFAULT,
             TargetCache_Options.ALWAYS_VALID_DEFAULT,
             // TargetCache_Options.VALIDATE_DEFAULT,
             TargetCache_Options.WRITE_THREADS_DEFAULT);
    }

    //

    private class CacheLock {
        // EMPTY
    }
    private final CacheLock cacheLock = new CacheLock();

    private TargetCache_Options options;
    private TargetCacheImpl_DataApps cache;

    @Override
    @Trivial
    public TargetCacheImpl_Options createOptions() {
        return createOptionsFromProperties();
    }

    @Override
    @Trivial
    public TargetCache_Options getCacheOptions() {
        synchronized(cacheLock) {
            return options;
        }
    }

    @Override
    @Trivial
    public void setOptions(TargetCache_Options options) {
        synchronized(cacheLock) {
            String methodName = "setOptions";

            this.cache = null;
            this.options = options;

            if ( logger.isLoggable(Level.FINER) ) {
                finer(methodName, "Annotation Cache Options:");
                finer(methodName, "  Enabled         [ {0} ]", Boolean.valueOf(!options.getDisabled()));
                finer(methodName, "  Directory       [ {0} ]", options.getDir());
                finer(methodName, "  AlwaysValid     [ {0} ]", Boolean.valueOf(options.getAlwaysValid()));
                finer(methodName, "  ReadOnly        [ {0} ]", Boolean.valueOf(options.getReadOnly()));
                // finer(methodName, "  Validate        [ {0} ]", Boolean.valueOf(options.getValidate()));
                finer(methodName, "  Write Threads   [ {0} ]", Integer.valueOf(options.getWriteThreads()));
            }
        }
    }

    public void clearCache() {
        synchronized( cacheLock ) {
            cache = null;
        }
    }

    @Trivial
    public TargetCacheImpl_DataApps getCache() {
        synchronized( cacheLock ) {
            if ( cache == null ) {
                cache = createCache();
            }
        }
        return cache;
    }

    protected TargetCacheImpl_DataApps createCache() {
        TargetCache_Options useOptions = getCacheOptions();
        if ( useOptions == null ) {
            throw new IllegalStateException("Annotation targets cache options not set");
        }

        File cacheDir = new File( useOptions.getDir() );
        String cacheName = cacheDir.getName();
        String e_cacheName = TargetCacheImpl_Utils.encodePath(cacheName);

        return createCache(cacheName, e_cacheName);
    }

    //

    protected TargetCacheImpl_DataApps createCache(String cacheName, String e_cacheName) {
        return new TargetCacheImpl_DataApps(this, cacheName, e_cacheName);
    }

    //

    protected TargetCacheImpl_DataApp createAppData(
        TargetCacheImpl_DataApps appsData,
        String appName, String e_appName, File appDir) {

        return new TargetCacheImpl_DataApp(appsData, appName, e_appName, appDir);
    }

    protected TargetCacheImpl_DataMod createModData(
        TargetCacheImpl_DataApp appData,
        String modName, String e_modName, File modDir, boolean isLightweight) {

        return new TargetCacheImpl_DataMod(appData, modName, e_modName, modDir, isLightweight);
    }

    protected TargetCacheImpl_DataCon createConData(
        TargetCacheImpl_DataBase parentCache, // App or Mod
        String conName, String e_conName, File conDir) {

        return new TargetCacheImpl_DataCon(parentCache, conName, e_conName, conDir);
    }

    //

    protected TargetCacheImpl_DataQueries createQueriesData(
        String appName, String e_appName,
        String modName, String e_modName, File modDir) {

        return new TargetCacheImpl_DataQueries(
            this,
            appName, e_appName,
            modName, e_modName, modDir);
    }

    //

    protected TargetCacheImpl_Reader createReader(File inputFile) throws IOException {
        String inputPath = inputFile.getPath();

        FileInputStream inputStream = UtilImpl_FileUtils.createFileInputStream(inputFile); // throws IOException

        return createReader(inputPath, inputStream);
    }

    protected TargetCacheImpl_Reader createReader(String path, InputStream stream) {
        try {
            return new TargetCacheImpl_Reader(this, path, stream, TargetCache_InternalConstants.SERIALIZATION_ENCODING);
        } catch ( UnsupportedEncodingException e ) {
            return null; // FFDC
        }
    }

    protected TargetCacheImpl_Writer createWriter(String path, OutputStream stream) {
        try {
            return new TargetCacheImpl_Writer(this, path, stream, TargetCache_InternalConstants.SERIALIZATION_ENCODING);
        } catch ( UnsupportedEncodingException e ) {
            return null; // FFDC
        }
    }

    //

    @Trivial
    protected List<TargetCache_ParseError> read(TargetsTableContainersImpl containerTable, String path, InputStream stream) throws IOException {
        return createReader(path, stream).read(containerTable);
    }

    @Trivial
    protected List<TargetCache_ParseError> read(TargetsTableTimeStampImpl stampTable, String path, InputStream stream) throws IOException {
        return createReader(path, stream).read(stampTable);
    }

    @Trivial
    protected List<TargetCache_ParseError> read(TargetsTableClassesImpl classTable, String path, InputStream stream) throws IOException {
        return createReader(path, stream).read(classTable);
    }

    @Trivial
    protected List<TargetCache_ParseError> read(TargetsTableAnnotationsImpl targetTable, String path, InputStream stream) throws IOException {
        return createReader(path, stream).read(targetTable);
    }

    @Trivial
    protected List<TargetCache_ParseError> read(TargetsTableDetailsImpl detailTable, String path, InputStream stream) throws IOException {
        return createReader(path, stream).read(detailTable);
    }

    //

    @Trivial
    protected void write(TargetsTableContainersImpl containerTable, String path, OutputStream stream) throws IOException {
        createWriter(path, stream).write(containerTable);
    }

    @Trivial
    protected void write(TargetsTableTimeStampImpl stampTable, String path, OutputStream stream) throws IOException {
        createWriter(path, stream).write(stampTable);
    }

    @Trivial
    protected void write(TargetsTableClassesImpl classTable, String path, OutputStream stream) throws IOException {
        createWriter(path, stream).write(classTable);
    }

    @Trivial
    protected void write(TargetsTableAnnotationsImpl targetTable, String path, OutputStream stream) throws IOException {
        createWriter(path, stream).write(targetTable);
    }

    @Trivial
    protected void write(TargetsTableDetailsImpl detailTable, String path, OutputStream stream) throws IOException {
        createWriter(path, stream).write(detailTable);
    }
}
