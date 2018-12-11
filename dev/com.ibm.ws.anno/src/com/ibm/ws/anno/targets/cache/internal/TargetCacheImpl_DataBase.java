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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.anno.service.internal.AnnotationServiceImpl_Logging;
import com.ibm.ws.anno.targets.cache.TargetCache_ParseError;
import com.ibm.ws.anno.targets.cache.TargetCache_Readable;
import com.ibm.ws.anno.util.internal.UtilImpl_FileUtils;
import com.ibm.wsspi.anno.targets.cache.TargetCache_Options;

/**
 * Core type for cache data.
 */
public abstract class TargetCacheImpl_DataBase {
    private static final String CLASS_NAME = TargetCacheImpl_DataBase.class.getSimpleName();
    protected static final Logger logger = AnnotationServiceImpl_Logging.ANNO_LOGGER;

    //

    public static final String NO_CHILD_PREFIX = null;

    /**
     * Create new cache data.
     *
     * @param factory The factory which is creating the data.
     * @param name A name for the data.  Unless caching is disabled,
     *     the name must be unique within the enclosing context
     *     (application or module).
     * @param e_name The encoded name of the data.
     * @param dataDir The directory in which to store the data.  Null
     *     if caching is disabled.
     */
    public TargetCacheImpl_DataBase(
        TargetCacheImpl_Factory factory,
        String name, String e_name, File dataDir) {

        this.factory = factory;
        this.cacheOptions = factory.getCacheOptions();

        this.name = name;
        this.e_name = e_name;

        this.dataDir = dataDir;

        this.readTime = 0L;
        this.writeTime = 0L;
    }

    //

    protected final TargetCacheImpl_Factory factory;

    @Trivial
    public TargetCacheImpl_Factory getFactory() {
        return factory;
    }

    @Trivial
    protected TargetCacheImpl_DataCon createConData(
        TargetCacheImpl_DataBase parentCache,
        String conPath, String e_conPath, File conDir) {
        return getFactory().createConData(parentCache, conPath, e_conPath, conDir);
    }

    //

    protected final TargetCache_Options cacheOptions;

    @Trivial
    public TargetCache_Options getCacheOptions() {
        return cacheOptions;
    }

    @Trivial
    public boolean isDisabled() {
        return getCacheOptions().getDisabled();
    }

    @Trivial
    public boolean isAlwaysValid() {
        return getCacheOptions().getAlwaysValid();
    }

    @Trivial
    public boolean isReadOnly() {
        return getCacheOptions().getReadOnly();
    }

    // @Trivial
    // public boolean isValidating() {
    //     return getCacheOptions().getValidate();
    // }

    @Trivial
    public int getWriteThreads() {
        return getCacheOptions().getWriteThreads();
    }

    //

    @Trivial
    public boolean shouldWrite(String outputDescription) {
        String methodName = "shouldWrite";

        if ( isDisabled() ) {
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "Disabled: Skipping write of [ {0} ]", outputDescription);
            }
            return false;

        } else if ( isReadOnly() ) {
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "Read-only: Skipping write of [ {0} ]", outputDescription);
            }
            return false;

        } else {
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "Enabled and not read-only: Allowing write of [ {0} ]", outputDescription);
            }
            return true;
        }
    }

    @Trivial
    public boolean shouldRead(String inputDescription) {
        String methodName = "shouldRead";

        if ( isDisabled() ) {
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "Disabled: Skipping read of [ {0} ]", inputDescription);
            }
            return false;

        } else {
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "Enabled: Allowing read of [ {0} ]", inputDescription);
            }
            return true;
        }
    }

    //

    @Trivial
    public TargetCacheImpl_Reader createReader(File inputFile) throws IOException {
        String inputPath = inputFile.getName();

        InputStream inputStream = openInputStream(inputFile); // throws IOException

        return createReader(inputPath, inputStream);
    }

    @Trivial
    public TargetCacheImpl_Reader createReader(String inputPath, InputStream inputStream) {
        return getFactory().createReader(inputPath, inputStream);
    }

    @Trivial
    public TargetCacheImpl_Writer createWriter(File outputFile) throws IOException {
        String outputName = outputFile.getName();

        File parentFile = outputFile.getParentFile();

        mkdirs(parentFile);

        if ( !exists(parentFile) ) {
            throw new IOException("Parent [ " + parentFile.getName() + " ] for write [ " + outputName + " ] does not exist");
        } else if ( !isDirectory(parentFile) ) {
            throw new IOException("Parent [ " + parentFile.getName() + " ] for write [ " + outputName + " ] exists but is not a directory");
        }

        OutputStream outputStream = openOutputStream(outputFile); // throws IOException

        return createWriter(outputName, outputStream);
    }

    @Trivial
    public TargetCacheImpl_Writer createWriter(String outputName, OutputStream outputStream) {
        return getFactory().createWriter(outputName, outputStream);
    }

    //

    public boolean read(TargetCache_Readable readable, File inputFile) {
        IOException boundException = null;
        List<TargetCache_ParseError> parseErrors = null;
        try {
            TargetCacheImpl_Reader reader = createReader(inputFile); // throws IOException
            try {
                parseErrors = readable.readUsing(reader); // throws IOException
            } finally {
                reader.close(); // throws IOException
            }
        } catch ( IOException e ) {
            boundException = e;
        }

        if ( readError(inputFile, boundException, parseErrors) ) {
            return false;
        }

        return true;
    }

    @Trivial
    public boolean readError(File file,
                             Exception boundException,
                             List<TargetCache_ParseError> parseErrors) {

        String methodName = "readError";

        if ( (boundException == null) && ((parseErrors == null) || parseErrors.isEmpty()) ) {
            return false;
        }

        logger.logp(Level.WARNING, CLASS_NAME, methodName, "Failed to read table [ {0} ]", file.getPath());

        if ( boundException != null ) {
            logger.logp(Level.WARNING, CLASS_NAME, methodName, "Read exception: {0}", boundException);
            logger.logp(Level.WARNING, CLASS_NAME, methodName, "Read failure", boundException);
        }

        if ( (parseErrors != null) && !parseErrors.isEmpty() ) {
            for ( TargetCache_ParseError parseError : parseErrors ) {
                parseError.emit(logger);
            }
        }

        return true;
    }

    //

    protected final String name;
    protected final String e_name;

    @Trivial
    public String getName() {
        return name;
    }

    @Trivial
    public boolean isNamed() {
        return ( name != null );
    }

    @Trivial
    public String e_getName() {
        return e_name;
    }

    //

    protected final File dataDir;

    @Trivial
    public File getDataDir() {
        return dataDir;
    }

    @Trivial
    public boolean exists() {
        return ( exists( getDataDir() ) );
    }

    // Calls to obtain files in the cache tree MUST use a relative constructor.
    // The files must be created with a proper relationship to their parent.
    public File getDataFile(String relativePath) {
        if ( relativePath == null ) {
            return null;
        }
        File useDataDir = getDataDir();
        if ( useDataDir == null ) {
            return null;
        } else {
            return new File(useDataDir, relativePath);
        }
    }

    //

    public String e_addAppPrefix(String e_appName) {
        return ( TargetCacheImpl_Utils.APP_PREFIX_WIDGET.e_addPrefix(e_appName) );
    }

    public String e_removeAppPrefix(String e_appName) {
        return ( TargetCacheImpl_Utils.APP_PREFIX_WIDGET.e_removePrefix(e_appName) );
    }

    public String e_addModPrefix(String e_modName) {
        return ( TargetCacheImpl_Utils.MOD_PREFIX_WIDGET.e_addPrefix(e_modName) );
    }

    public String e_removeModPrefix(String e_modName) {
        return ( TargetCacheImpl_Utils.MOD_PREFIX_WIDGET.e_removePrefix(e_modName) );
    }

    public String e_addConPrefix(String e_conPath) {
        return ( TargetCacheImpl_Utils.CON_PREFIX_WIDGET.e_addPrefix(e_conPath) );
    }
    
    public String e_removeConPrefix(String e_conPath) {
        return ( TargetCacheImpl_Utils.CON_PREFIX_WIDGET.e_removePrefix(e_conPath) );
    }

    //

    /**
     * Container data names may include special characters, including
     * path separator characters.  These must be encoded to be used
     * in cache file names.
     *
     * See {@link TargetCacheImpl_Utils#encodePath}.
     *
     * @param name A value to be encoded.
     *
     * @return The encoded value.
     */
    @Trivial
    public String encode(String name) {
        return TargetCacheImpl_Utils.encodePath(name);
    }

    /**
     * Container data names may include special characters, including
     * path separator characters.  These are encoded when used to generate
     * cache file names.  Recover of a data name from a cache file
     * requires that the cache file name be decoded.
     *
     * See {@link TargetCacheImpl_Utils#decodePath}.
     *
     * @param name A value to be decoded.
     *
     * @return The decoded value.
     */

    @Trivial
    public String decode(String name) {
        return TargetCacheImpl_Utils.decodePath(name);
    }

    //

    @Trivial
    protected boolean exists(File targetFile) {
        return ( (targetFile != null) && UtilImpl_FileUtils.exists(targetFile) );
    }

    @Trivial
    protected boolean isDirectory(File targetFile) {
        return ( UtilImpl_FileUtils.isDirectory(targetFile) );
    }

    @Trivial
    protected boolean mkdirs(File targetFile) {
        return UtilImpl_FileUtils.mkdirs(targetFile);
    }

    @Trivial
    protected FileOutputStream openOutputStream(File file) throws IOException {
        return UtilImpl_FileUtils.createFileOutputStream(file); // throws IOException
    }

    @Trivial
    protected FileInputStream openInputStream(File file) throws IOException {
        return UtilImpl_FileUtils.createFileInputStream(file); // throws IOException
    }

    //

    private long readTime;

    @Trivial
    public long getReadTime() {
        return readTime;
    }

    public long addReadTime(long start, String description) {
        long duration = System.nanoTime() - start;
        readTime += duration;
        return duration;
    }

    private long writeTime;

    @Trivial
    public long getWriteTime() {
        return writeTime;
    }

    public long addWriteTime(long start, String description) {
        long duration = System.nanoTime() - start;
        writeTime += duration;
        return duration;
    }

    //

    @Trivial
    protected File e_getConDir(String e_conPath) {
        return getDataFile( e_addConPrefix(e_conPath) );
    }
}
