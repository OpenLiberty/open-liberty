/*******************************************************************************
 * Copyright (c) 2017, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.targets.cache.internal;

// import java.io.ByteArrayOutputStream; // 30315
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.annocache.service.internal.AnnotationCacheServiceImpl_Logging;
import com.ibm.ws.annocache.targets.cache.TargetCache_ParseError;
import com.ibm.ws.annocache.targets.cache.interfaces.TargetCache_FactoryLiberty;
import com.ibm.ws.annocache.targets.internal.TargetsTableAnnotationsImpl;
import com.ibm.ws.annocache.targets.internal.TargetsTableClassesImpl;
import com.ibm.ws.annocache.targets.internal.TargetsTableContainersImpl;
import com.ibm.ws.annocache.targets.internal.TargetsTableDetailsImpl;
import com.ibm.ws.annocache.targets.internal.TargetsTableTimeStampImpl;
import com.ibm.ws.annocache.util.internal.UtilImpl_FileUtils;
import com.ibm.ws.annocache.util.internal.UtilImpl_Utils;
import com.ibm.wsspi.annocache.targets.cache.TargetCache_Factory;
import com.ibm.wsspi.annocache.targets.cache.TargetCache_InternalConstants;
import com.ibm.wsspi.annocache.targets.cache.TargetCache_Options;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM"})
public class TargetCacheImpl_Factory implements TargetCache_Factory, TargetCache_FactoryLiberty {
    private static final String CLASS_NAME = TargetCacheImpl_Factory.class.getSimpleName();

    protected static final Logger logger = AnnotationCacheServiceImpl_Logging.ANNO_LOGGER;

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

    public static TargetCacheImpl_Options createOptionsFromProperties() {
        boolean disabled      = getSystemProperty(TargetCache_Options.DISABLED_PROPERTY_NAME, TargetCache_Options.DISABLED_DEFAULT);

        String dir            = getSystemProperty(TargetCache_Options.DIR_PROPERTY_NAME, TargetCache_Options.DIR_DEFAULT);
        boolean readOnly      = getSystemProperty(TargetCache_Options.READ_ONLY_PROPERTY_NAME, TargetCache_Options.READ_ONLY_DEFAULT);
        boolean alwaysValid   = getSystemProperty(TargetCache_Options.ALWAYS_VALID_PROPERTY_NAME, TargetCache_Options.ALWAYS_VALID_DEFAULT);
        // boolean validate     = getSystemProperty(TargetCache_Options.VALIDATE_PROPERTY_NAME, TargetCache_Options.VALIDATE_DEFAULT);

        int writeThreads      = getSystemProperty(TargetCache_Options.WRITE_THREADS_PROPERTY_NAME, TargetCache_Options.WRITE_THREADS_DEFAULT);
        int writeLimit        = getSystemProperty(TargetCache_Options.WRITE_LIMIT_PROPERTY_NAME, TargetCache_Options.WRITE_LIMIT_DEFAULT);
        boolean useJandexFormat    = getSystemProperty(TargetCache_Options.USE_JANDEX_FORMAT_PROPERTY_NAME, TargetCache_Options.USE_JANDEX_FORMAT_DEFAULT);
        boolean useBinaryFormat    = getSystemProperty(TargetCache_Options.USE_BINARY_FORMAT_PROPERTY_NAME, TargetCache_Options.USE_BINARY_FORMAT_DEFAULT);

        boolean logQueries = getSystemProperty(TargetCache_Options.LOG_QUERIES_PROPERTY_NAME, TargetCache_Options.LOG_QUERIES_DEFAULT);

        return new TargetCacheImpl_Options(
            disabled,
            dir,
            readOnly, alwaysValid,
            // validate,
            writeThreads, writeLimit,
            useJandexFormat, useBinaryFormat,
            logQueries);
    }

    public static TargetCacheImpl_Options createOptionsFromDefaults() {
        return new TargetCacheImpl_Options(
            TargetCache_Options.DISABLED_DEFAULT,

             TargetCache_Options.DIR_DEFAULT,
             TargetCache_Options.READ_ONLY_DEFAULT,
             TargetCache_Options.ALWAYS_VALID_DEFAULT,
             // TargetCache_Options.VALIDATE_DEFAULT,
             TargetCache_Options.WRITE_THREADS_DEFAULT,
             TargetCache_Options.WRITE_LIMIT_DEFAULT,
             TargetCache_Options.USE_JANDEX_FORMAT_DEFAULT,
             TargetCache_Options.USE_BINARY_FORMAT_DEFAULT,
             TargetCache_Options.LOG_QUERIES_DEFAULT );
    }

    //

    protected static class CacheLock {
        // EMPTY
    }
    private final CacheLock cacheLock = new CacheLock();

    private TargetCache_Options options;
    private TargetCacheImpl_DataApps cache;

    //

    // 30315 Temporary logging.  Keeping for future use.
    //
    // See comments on:
    //
    // open-liberty/dev//com.ibm.ws.container.service/src/
    //     com/ibm/ws/container/service/annocache/internal/ModuleAnnotationsImpl.java
    //
    // The use of 'getDeploymentName' instead of 'getName' caused creates of different
    // application data which used the same application name.  That led to concurrent
    // access to the same cache data file, which caused overlapping reads and writes
    // of the same files.  A variety of data read exceptions occurs, generally because
    // of reads of incompletely written data.
    //
    // The problem was timing sensitive, requiring the custom trace, below, to enable
    // gathering of minimal trace output while reproducing the problem.
    //
    // I've left the trace code, commented out, for potential future use.
    // 
    // Calls were made from this class and from 'TargetCacheImpl_DataApps'.

//    private static long nanoTime() {
//        return System.nanoTime();
//    }
//    
//    private final static long nanoStart = nanoTime();
//
//    private static long nanoStart() {
//        return nanoStart;
//    }
//    
//    private static long nanoElapsed() {
//        return nanoTime() - nanoStart();
//    }
//
//    private final List<String> cacheLog = new ArrayList<>();
//
//    protected static class LogLock {
//        // EMPTY
//    }
//    private final LogLock logLock = new LogLock();
//
//    public static String addDetails(String activity) {
//        return "ACL [" + nanoElapsed() + "] [ " + Thread.currentThread().getName() + " ] " + activity;
//    }
//    
//    public String printStack(String message) {
//        Throwable th = new Throwable(message);
//
//        ByteArrayOutputStream output = new ByteArrayOutputStream();
//        PrintWriter writer = new PrintWriter(output);
//        th.printStackTrace(writer);
//        writer.flush();
//
//        return output.toString();
//    }
//    
//    public void log(String activity) {
//        String message = addDetails(activity);
//        synchronized(logLock) {
//            cacheLog.add(message);
//        }
//    }
//
//    public void logStack(String activity) {
//        String message = addDetails(activity); 
//        String stackText = printStack(message);
//
//        synchronized(logLock) {
//            cacheLog.add(message);
//            cacheLog.add(stackText);
//        }
//    }
//
//    public void dumpLog() {
//        List<String> log;
//
//        synchronized(logLock) {
//            log("dump annocache factory log");
//            log = new ArrayList<>(cacheLog);
//            cacheLog.clear();
//        }
//
//        for ( String message : log ) {
//            System.out.println(message);
//        }
//    }

    @Activate
    public TargetCacheImpl_Factory(BundleContext bundleContext) {
        this(createOptionsFromProperties());

        if ( !options.getDisabled() ) {
            String workArea = getOsgiWorkArea(bundleContext);
            if ( workArea != null ) {
                options.setDir(workArea + File.separatorChar + TargetCache_Options.CACHE_NAME_DEFAULT);
            }
        }
        final String methodName = "TargetCacheImpl_Factory(BundleContext bundleContext)";
        if ( logger.isLoggable(Level.FINER) ) { // INFO is temporary
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "Cache Disabled [ {0} ]",
                Boolean.valueOf( options.getDisabled() ));
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "Cache Dir [ {0} ]",
                options.getDir());
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "Cache Write Threads [ {0} ]",
                Integer.valueOf(options.getWriteThreads()));
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "Cache Write Limit [ {0} ]",
                Integer.valueOf(options.getWriteLimit()));
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "Use Jandex Format For Containers [ {0} ]",
                Boolean.valueOf(options.getUseJandexFormat()));
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "Use Binary Format [ {0} ]",
                Boolean.valueOf(options.getUseBinaryFormat()));
        }
    }

    private String getOsgiWorkArea(BundleContext bundleContext) {
        String methodName = "getOsgiWorkArea";

        File osgiWorkFile = bundleContext.getDataFile(""); // Empty string obtains the work directory.
        if ( osgiWorkFile == null ) {
            if ( logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "OSGi Platform does not have file system support.");
            }
            return null;
        }

        String osgiWorkPath = osgiWorkFile.getAbsolutePath();
        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "OSGi Work Path [ {0} ]", osgiWorkPath);
        }
        return osgiWorkPath;
    }
    public TargetCacheImpl_Factory(TargetCache_Options options) {
        setOptions(options);
    }

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
                finer(methodName, "  Enabled             [ {0} ]", Boolean.valueOf(!options.getDisabled()));
                finer(methodName, "  Directory           [ {0} ]", options.getDir());
                finer(methodName, "  AlwaysValid         [ {0} ]", Boolean.valueOf(options.getAlwaysValid()));
                finer(methodName, "  ReadOnly            [ {0} ]", Boolean.valueOf(options.getReadOnly()));
             // finer(methodName, "  Validate            [ {0} ]", Boolean.valueOf(options.getValidate()));
                finer(methodName, "  Write Threads       [ {0} ]", Integer.valueOf(options.getWriteThreads()));
                finer(methodName, "  Write Limit         [ {0} ]", Integer.valueOf(options.getWriteLimit()));
                finer(methodName, "  Use Jandex Format   [ {0} ]", Boolean.valueOf(options.getUseJandexFormat()));
                finer(methodName, "  Use Binary Format   [ {0} ]", Boolean.valueOf(options.getUseBinaryFormat()));
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
    
    /**
     * Release cache data for an application.
     * 
     * @param appName The name of the application. This should be the application's deployment name.
     */
    @Override
    public boolean release(String appName) {
        boolean result = getCache().release(appName);

        // logStack("Release [ " + appName + " ] [ " + result + " ]"); // 30315
        //
        // dumpLog(); // 30315

        return result;
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
        // log("Create cache [ " + cacheName + " ]"); // 30315
        return new TargetCacheImpl_DataApps(this, cacheName, e_cacheName);
    }

    //

    protected TargetCacheImpl_DataApp createAppData(
        TargetCacheImpl_DataApps appsData,
        String appName, String e_appName, File appDir) {

        // log("Create app [ " + appName + " ]"); // 30315
        return new TargetCacheImpl_DataApp(appsData, appName, e_appName, appDir);
    }

    protected TargetCacheImpl_DataMod createModData(
        TargetCacheImpl_DataApp appData,
        String modName, String e_modName, File modDir, boolean isLightweight) {

        // log("Create mod [ " + appData.getName() + " ] [ " + modName + " ]"); // 30315        
        return new TargetCacheImpl_DataMod(appData, modName, e_modName, modDir, isLightweight);
    }

    protected TargetCacheImpl_DataCon createConData(
        TargetCacheImpl_DataBase parentCache, // App or Mod
        String conName, String e_conName, File conFile,
        boolean isSource) {

        // log("Create con [ " + parentCache.getName() + " ] [ " + conName + " ]"); // 30315        
        return new TargetCacheImpl_DataCon(
            parentCache,
            conName, e_conName, conFile,
            isSource);
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
        return new TargetCacheImpl_Reader(this, path, stream, TargetCache_InternalConstants.SERIALIZATION_ENCODING);
    }

    protected TargetCacheImpl_Writer createWriter(String path, OutputStream stream) {
        return new TargetCacheImpl_Writer(this,
            path, stream,
            TargetCache_InternalConstants.SERIALIZATION_ENCODING);
    }


    protected TargetCacheImpl_ReaderBinary createBinaryReader(
        String path, boolean readStrings, boolean readFull) throws IOException {

        return new TargetCacheImpl_ReaderBinary(this,
            path,
            TargetCache_InternalConstants.SERIALIZATION_ENCODING,
            readStrings, readFull); // throws IOException
    }

    protected TargetCacheImpl_WriterBinary createBinaryWriter(String path, OutputStream stream)
        throws IOException {

        return new TargetCacheImpl_WriterBinary(this,
            path, stream,
            TargetCache_InternalConstants.SERIALIZATION_ENCODING); // throws IOException
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
}
