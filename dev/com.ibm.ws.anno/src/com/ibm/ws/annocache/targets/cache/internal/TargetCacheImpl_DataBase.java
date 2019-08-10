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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.annocache.service.internal.AnnotationCacheServiceImpl_Logging;
import com.ibm.ws.annocache.targets.cache.TargetCache_ParseError;
import com.ibm.ws.annocache.targets.cache.TargetCache_Readable;
import com.ibm.ws.annocache.targets.internal.TargetsTableAnnotationsImpl;
import com.ibm.ws.annocache.targets.internal.TargetsTableClassesImpl;
import com.ibm.ws.annocache.targets.internal.TargetsTableTimeStampImpl;
import com.ibm.ws.annocache.util.internal.UtilImpl_FileUtils;
import com.ibm.wsspi.annocache.targets.cache.TargetCache_Options;
import com.ibm.wsspi.annocache.util.Util_Consumer;
import com.ibm.wsspi.annocache.util.Util_Function;

/**
 * Core type for cache data.
 */
public abstract class TargetCacheImpl_DataBase {
    private static final String CLASS_NAME = TargetCacheImpl_DataBase.class.getSimpleName();
    protected static final Logger logger = AnnotationCacheServiceImpl_Logging.ANNO_LOGGER;

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
     * @param dataFile The file or directory in which to store the data.  Null
     *     if caching is disabled.
     */
    public TargetCacheImpl_DataBase(
        TargetCacheImpl_Factory factory,
        String name, String e_name, File dataFile) {

        this.factory = factory;
        this.cacheOptions = factory.getCacheOptions();

        this.name = name;
        this.e_name = e_name;

        this.dataFile = dataFile;

        this.readTime = 0L;
        this.writeTime = 0L;

        // System.out.println("Data [ " + getClass().getSimpleName() + " ] [ " + name + " ]");
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
        String conPath, String e_conPath, File conFile,
        boolean isSource) {
        return getFactory().createConData(
            parentCache,
            conPath, e_conPath, conFile,
            isSource);
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

    @Trivial
    public boolean getLogQueries() {
        return getCacheOptions().getLogQueries();
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

        } else if ( getDataFile() == null ) {
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "Unnamed: Skipping write of [ {0} ]", outputDescription);
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
    public TargetCacheImpl_Writer createWriter(File outputFile) throws IOException {
        return createWriter(outputFile, TargetCacheImpl_DataBase.DO_TRUNCATE);
    }

    @Trivial
    public TargetCacheImpl_Writer createWriter(File outputFile, boolean doTruncate) throws IOException {
        String outputName = outputFile.getName();

        File parentFile = outputFile.getParentFile();

        mkdirs(parentFile);

        if ( !exists(parentFile) ) {
            throw new IOException("Parent [ " + parentFile.getName() + " ] for write [ " + outputName + " ] does not exist");
        } else if ( !isDirectory(parentFile) ) {
            throw new IOException("Parent [ " + parentFile.getName() + " ] for write [ " + outputName + " ] exists but is not a directory");
        }

        OutputStream outputStream = openOutputStream(outputFile, doTruncate); // throws IOException

        return createWriter(outputName, outputStream);
    }

    @Trivial
    public TargetCacheImpl_Writer createWriter(String outputName, OutputStream outputStream) {
        return getFactory().createWriter(outputName, outputStream);
    }

    public void performWrite(
        String description,
        File outputFile, boolean doTruncate,
        Util_Consumer<TargetCacheImpl_Writer, IOException> writeAction) {

        String methodName = "performWrite";

        if ( description != null ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "ENTER {0}", description);
        }

        long writeStart = System.nanoTime();

        TargetCacheImpl_Writer useWriter;

        try {
            useWriter = createWriter(outputFile, doTruncate);
        } catch ( IOException e ) {
            useWriter = null;
            // CWWKC0101W: Annotation processing cache error: {0}
            logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_TARGETS_CACHE_EXCEPTION", e.getMessage());
        }

        if ( useWriter != null ) {
            try {
                writeAction.accept(useWriter);
            } catch ( IOException e ) {
                // CWWKC0101W: Annotation processing cache error: {0}
                logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_TARGETS_CACHE_EXCEPTION", e.getMessage());
            } finally {
                useWriter.close();
            }
        }

        @SuppressWarnings("unused")
        long duration = addWriteTime(writeStart, description);

        if ( description != null ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "RETURN {0}", description);
        }
    }

    //

    @Trivial
    public TargetCacheImpl_WriterBinary createBinaryWriter(File outputFile) throws IOException {
        return createBinaryWriter(outputFile, TargetCacheImpl_DataBase.DO_TRUNCATE);
    }

    @Trivial
    public TargetCacheImpl_WriterBinary createBinaryWriter(File outputFile, boolean doTruncate) throws IOException {
        String outputName = outputFile.getName();

        File parentFile = outputFile.getParentFile();

        mkdirs(parentFile);

        if ( !exists(parentFile) ) {
            throw new IOException("Parent [ " + parentFile.getName() + " ] for write [ " + outputName + " ] does not exist");
        } else if ( !isDirectory(parentFile) ) {
            throw new IOException("Parent [ " + parentFile.getName() + " ] for write [ " + outputName + " ] exists but is not a directory");
        }

        OutputStream outputStream = openOutputStream(outputFile, doTruncate); // throws IOException

        return createBinaryWriter(outputName, outputStream); // throws IOException
    }

    @Trivial
    public TargetCacheImpl_WriterBinary createBinaryWriter(String outputName, OutputStream outputStream) 
        throws IOException {

        return getFactory().createBinaryWriter(outputName, outputStream);
        // 'createBinaryWriter' throws IOException
    }

    public void performBinaryWrite(
        String description,
        File outputFile, boolean doTruncate,
        Util_Consumer<TargetCacheImpl_WriterBinary, IOException> writeAction) {

        String methodName = "performWrite";

        if ( description != null ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "ENTER {0}", description);
        }

        long writeStart = System.nanoTime();

        TargetCacheImpl_WriterBinary useWriter;

        try {
            useWriter = createBinaryWriter(outputFile, doTruncate);
        } catch ( IOException e ) {
            useWriter = null;
            // CWWKC0101W: Annotation processing cache error: {0}
            logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_TARGETS_CACHE_EXCEPTION", e.getMessage());
        }

        if ( useWriter != null ) {
            try {
                writeAction.accept(useWriter);
            } catch ( IOException e ) {
                // CWWKC0101W: Annotation processing cache error: {0}
                logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_TARGETS_CACHE_EXCEPTION", e.getMessage());
            } finally {
                useWriter.close();
            }
        }

        @SuppressWarnings("unused")
        long duration = addWriteTime(writeStart, description);

        if ( description != null ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "RETURN {0}", description);
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

    public boolean readBinary(
        File inputFile, boolean readStrings, boolean readFull,
        Util_Consumer<TargetCacheImpl_ReaderBinary, IOException> readAction) {

        IOException boundException = null;

        try {
            TargetCacheImpl_ReaderBinary reader =
                createBinaryReader( inputFile.getPath(), readStrings, readFull ); // throws IOException

            if ( reader != null ) {
                try {
                    readAction.accept(reader);
                } finally {
                    reader.close(); // throws IOException
                }
            }

        } catch ( IOException e ) {
            boundException = e;
        }

        if ( readError(inputFile, boundException, null) ) {
            return false;
        }

        return true;
    }

    @SuppressWarnings("null")
    public String basicValidCombinedBinary(
        File inputFile, boolean readStrings, boolean readFull,
        Util_Function<TargetCacheImpl_ReaderBinary, IOException, String> readAction) {

        IOException boundException = null;
        String readResult = null;

        try {
            TargetCacheImpl_ReaderBinary reader =
                createBinaryReader( inputFile.getPath(), readStrings, readFull ); // throws IOException

            if ( reader != null ) {
                try {
                    readResult = readAction.apply(reader);
                } finally {
                    reader.close(); // throws IOException
                }
            }

        } catch ( IOException e ) {
            boundException = e;
        }

        if ( readError(inputFile, boundException, null) ) {
            return "Read exception: " + boundException.getMessage();
        } else {
            return readResult;
        }
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

    @Trivial
    public TargetCacheImpl_ReaderBinary createBinaryReader(
        String inputPath, boolean readStrings, boolean readFull) throws IOException {

        return getFactory().createBinaryReader(inputPath, readStrings, readFull);
        // 'createBinaryReader' throws IOException
    }

    public boolean read(File inputFile, TargetCache_Readable ... readables) {
        IOException boundException = null;
        List<TargetCache_ParseError> parseErrors = null;

        String methodName = "read";

        try {
            TargetCacheImpl_Reader reader = createReader(inputFile); // throws IOException
            
            if ( reader != null ) {
                try {
                    for ( TargetCache_Readable readable : readables ) {
                        if ( logger.isLoggable(Level.FINER) ) {
                            logger.logp(Level.FINER, CLASS_NAME, methodName, "Read [ {0} ]", readable);
                        }
                        parseErrors = readable.readUsing(reader); // throws IOException
                        if ( !parseErrors.isEmpty() ) {
                            break;
                        }
                    }
                } finally {
                    reader.close(); // throws IOException
                }
            }
        } catch ( IOException e ) {
            boundException = e;
        }

        if ( readError(inputFile, boundException, parseErrors) ) {
            return false;
        }

        return true;
    }

    public String basicValidCombined(File inputFile,
        String currentStamp,
        TargetsTableTimeStampImpl stampTable,
        TargetsTableClassesImpl classesTable,
        TargetsTableAnnotationsImpl targetsTable) {

        String methodName = "read";

        IOException boundException = null;
        List<TargetCache_ParseError> parseErrors = null;

        boolean stampDifference = false;

        try {
            TargetCacheImpl_Reader reader = createReader(inputFile); // throws IOException

            if ( reader != null ) {
                try {
                    if ( logger.isLoggable(Level.FINER) ) {
                        logger.logp(Level.FINER, CLASS_NAME, methodName, "Read [ {0} ]", stampTable);
                    }
                    parseErrors = stampTable.readUsing(reader); // throws IOException

                    if ( parseErrors.isEmpty() ) {
                        if ( !currentStamp.equals( stampTable.getStamp() ) ) {
                            stampDifference = true;

                        } else {
                            if ( logger.isLoggable(Level.FINER) ) {
                                logger.logp(Level.FINER, CLASS_NAME, methodName, "Read [ {0} ]", classesTable);
                            }
                            parseErrors = classesTable.readUsing(reader); // throws IOException

                            if ( parseErrors.isEmpty() ) {
                                if ( logger.isLoggable(Level.FINER) ) {
                                    logger.logp(Level.FINER, CLASS_NAME, methodName, "Read [ {0} ]", targetsTable);
                                }
                                parseErrors = targetsTable.readUsing(reader); // throws IOException
                            }
                        }
                    }

                } finally {
                    reader.close(); // throws IOException
                }
            }
        } catch ( IOException e ) {
            boundException = e;
        }

        if ( readError(inputFile, boundException, parseErrors) ) {
            if ( boundException != null ) {
                return "Read exception: " + boundException.getMessage();
            } else {
                return "Parse error";
            }
        } else if ( stampDifference ) {
            return "Stamp difference";
        } else {
            return null;
        }
    }

    //

    protected final String name;
    protected final String e_name;

    @Trivial
    public String getName() {
        return name;
    }

    @Trivial
    public String e_getName() {
        return e_name;
    }

    @Trivial
    public boolean isNamed() {
        return ( e_name != null );
    }

    //

    protected final File dataFile;

    @Trivial
    public File getDataFile() {
        return dataFile;
    }

    @Trivial
    public boolean exists() {
        return ( exists( getDataFile() ) );
    }

    // Data may be stored in a single file, or in a directory
    // (which will contain one or more files), as decided by
    // particular subclasses.

    // Calls to obtain files in the cache tree MUST use a relative constructor.
    // The files must be created with a proper relationship to their parent.
    public File getDataFile(String relativePath) {
        if ( relativePath == null ) {
            return null;
        }
        File useDataDir = getDataFile();
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
     * @param useName A value to be encoded.
     *
     * @return The encoded value.
     */
    @Trivial
    public String encode(String useName) {
        return TargetCacheImpl_Utils.encodePath(useName);
    }

    /**
     * Container data names may include special characters, including
     * path separator characters.  These are encoded when used to generate
     * cache file names.  Recover of a data name from a cache file
     * requires that the cache file name be decoded.
     *
     * See {@link TargetCacheImpl_Utils#decodePath}.
     *
     * @param useName A value to be decoded.
     *
     * @return The decoded value.
     */

    @Trivial
    public String decode(String useName) {
        return TargetCacheImpl_Utils.decodePath(useName);
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

    public static final boolean DO_TRUNCATE = true;
    public static final boolean DO_NOT_TRUNCATE = false;

    @Trivial
    protected OutputStream openOutputStream(File file, boolean doTruncate) throws IOException {
        if ( doTruncate ) {
            return UtilImpl_FileUtils.createFileOutputStream(file, UtilImpl_FileUtils.DO_NOT_APPEND);
        } else {
            return UtilImpl_FileUtils.createOverwriteOutputStream(file);
        }
    }

    @Trivial
    protected InputStream openInputStream(File file) throws IOException {
        return UtilImpl_FileUtils.createFileInputStream(file); // throws IOException
    }
    
    @Trivial
    protected RandomAccessFile openRandomInputFile(File file) throws IOException {
        return UtilImpl_FileUtils.createRandomInputFile(file); // throws IOException
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

        // System.out.println("Read [ " + description + " ] [ " + duration + " (ns) ]");

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

        // System.out.println("Write [ " + description + " ] [ " + duration + " (ns) ]");

        return duration;
    }

    //

    @Trivial
    protected File e_getConFile(String e_conPath) {
        return getDataFile( e_addConPrefix(e_conPath) );
    }
}
