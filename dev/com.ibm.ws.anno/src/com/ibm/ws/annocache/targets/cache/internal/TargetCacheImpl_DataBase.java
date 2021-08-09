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
 *
 * Common properties of cache data are a factory, a name, and a file.
 *
 * A reference is taken of the factory cache options.  A helper API provides
 * direct access to cache options.
 *
 * Cache data tracks the time spent reading and writing its data.
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

    // Factory APIs ...

    /** The factory which created this data. */
    protected final TargetCacheImpl_Factory factory;

    /**
     * Answer the factory which created this data.
     *
     * @return The factory which created this data.
     */
    @Trivial
    public TargetCacheImpl_Factory getFactory() {
        return factory;
    }

    // TODO: Split this and move it to module and application data.
    //       Having both types of data use this common implementation is
    //       an unwarranted coupling.

    /**
     * Create child container data.
     *
     * @param parentCache The parent of the new container data.  May be
     *     module data or application data.
     * @param conPath The path to the container.
     * @param e_conPath The encoded path to the container.
     * @param conFile The file used by the container data.
     * @param isSource Control parameter: Is the new container data for
     *     a class source or for result data?
     *
     * @return New container data.
     */
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

    // Cache option helpers ...

    /** The cache options. */
    protected final TargetCache_Options cacheOptions;

    /**
     * Answer the cache options used by this cache data.
     *
     * These were obtained from the factory.  See {@link #getFactory()}.
     *
     * @return The cache options used by this cache data.
     */
    @Trivial
    public TargetCache_Options getCacheOptions() {
        return cacheOptions;
    }

    /**
     * Cache options helper: Tell if caching is disabled.
     *
     * @return True or false telling if caching is disabled.
     */
    @Trivial
    public boolean isDisabled() {
        return getCacheOptions().getDisabled();
    }

    /**
     * Cache options helper: Tell if cache data is always valid.
     *
     * @return True or false telling if cache data is always valid.
     */
    @Trivial
    public boolean isAlwaysValid() {
        return getCacheOptions().getAlwaysValid();
    }

    /**
     * Cache options helper: Tell if caching is set to read-only.
     *
     * @return True or false telling if caching is set to read-only.
     */
    @Trivial
    public boolean isReadOnly() {
        return getCacheOptions().getReadOnly();
    }

    // @Trivial
    // public boolean isValidating() {
    //     return getCacheOptions().getValidate();
    // }

    /**
     * Cache options helper: Tell the number of threads which may be used
     * for cache writes.
     *
     * If zero or one, only one thread will be used.  If greater than one, up
     * to that many threads will be used, and writes will be asynchronous.  If
     * minus one (-1), as many threads as are needed (up to {@link TargetCache_Options.WRITE_THREADS_MAX})
     * will be used, and writes will be asynchronous.
     *
     * Any values other than zero or one are experimental.
     *
     * @return The number of threads which may be used for cache writes.
     */
    @Trivial
    public int getWriteThreads() {
        return getCacheOptions().getWriteThreads();
    }

    /**
     * Cache options helper: Tell if queries are to be logged (written) to disk.
     * 
     * This is different than writing queries to server logs, which is
     * controlled by server trace enablement.
     * See {@link AnnotationCacheServiceImpl_Logging#ANNO_QUERY_LOGGER}.
     * 
     * @return True or false telling if queries are to be logged (written) to disk.
     */
    @Trivial
    public boolean getLogQueries() {
        return getCacheOptions().getLogQueries();
    }

    //

    /**
     * Tell if writes are enabled for this cache data.
     * 
     * Writes are disabled if either {@link #isDisabled()} or {@link #isReadOnly()}
     * returns true, or if this cache data has a null data file.  Otherwise,
     * writes are enabled.
     * 
     * Subclasses are expected to add additional conditions to tell if
     * writes are enabled.
     *
     * @param outputDescription The description of the write activity which
     *    is being attempted.
     *
     * @return True or false telling if the write activity is enabled.
     */
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

            // TODO: Testing the data file may be redundant with subclass tests.

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

    /**
     * Tell if reads are enabled for this cache data.
     * 
     * Reads are disabled if {@link #isDisabled()} returns true.  Otherwise,
     * reads are enabled.
     * 
     * Subclasses are expected to add additional conditions to tell if
     * reads are enabled.
     *
     * @param outputDescription The description of the read activity which
     *    is being attempted.
     *
     * @return True or false telling if the read activity is enabled.
     */
    @Trivial
    public boolean shouldRead(String inputDescription) {
        String methodName = "shouldRead";

        if ( isDisabled() ) {
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "Disabled: Skipping read of [ {0} ]", inputDescription);
            }
            return false;

            // TODO: Should there be a test for a null data file here, as is present
            //       in 'shouldWrite'?

        } else {
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "Enabled: Allowing read of [ {0} ]", inputDescription);
            }
            return true;
        }
    }

    // String write API ...

    /**
     * Create a writer for an output file.  Truncate the file.
     * 
     * See {@link #createWriter(File, boolean)}.
     *
     * @param outputFile The file to which to write.
     *
     * @return A new writer for the file.
     *
     * @throws IOException Thrown if the writer cannot be obtained.
     */
    @Trivial
    public TargetCacheImpl_Writer createWriter(File outputFile) throws IOException {
        return createWriter(outputFile, TargetCacheImpl_DataBase.DO_TRUNCATE);
    }

    /**
     * Create a writer for an output file.  The writer encapsulates an output
     * stream to the file.  The file is truncated according to the value of the
     * control parameter.
     *
     * Ensure that the parent directory of the output file exists and is a directory.
     * Create the parent directory if necessary.
     * 
     * @param outputFile The file to which to write.
     * @param doTruncate Control parameter: Tells whether to truncate the target file.
     *
     * @return A new writer for the file.
     *
     * @throws IOException Thrown if the writer cannot be obtained.  Thrown if the directory
     *     of the target file cannot be created, or exists as a simple file.  Thrown if an
     *     output stream cannot be opened on the file.
     */
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

    /**
     * Create a writer on an output stream.
     * 
     * The writer uses {@link TargetCache_InternalConstants#SERIALIZATION_ENCODING}.
     *
     * @param outputName A name associated with the output stream.
     * @param outputStream An output stream.
     *
     * @return A new writer for the output stream.
     */
    @Trivial
    public TargetCacheImpl_Writer createWriter(String outputName, OutputStream outputStream) {
        return getFactory().createWriter(outputName, outputStream);
    }

    /**
     * Write to a specified file using a write action.
     * 
     * Create a writer on the specified file, perform the write action using the
     * writer, then finish (close) the writer.
     *
     * Note the time spent writing, including time spent creating and finish the
     * writer, and add that time to the overall write time accumulated by this
     * cache data.
     *
     * Capture and log any write failure.
     *
     * @param description A description of the write action which is being performed.
     * @param outputFile The file to which to write.
     * @param doTruncate Control parameter: Tells whether to truncate the target file.
     * @param writeAction The action which performs the write.
     */
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

        // TODO: Should the success or failure of the write action be provided as a return
        //       value?
    }

    // Binary write API ...

    /**
     * Create a binary writer for an output file.  Truncate the file.
     * 
     * See {@link #createBinaryWriter(File, boolean)}.
     *
     * @param outputFile The file to which to write.
     *
     * @return A new writer for the file.
     *
     * @throws IOException Thrown if the writer cannot be obtained.
     */
    @Trivial
    public TargetCacheImpl_WriterBinary createBinaryWriter(File outputFile) throws IOException {
        return createBinaryWriter(outputFile, TargetCacheImpl_DataBase.DO_TRUNCATE);
    }

    /**
     * Create a binary writer for an output file.  The writer encapsulates an output
     * stream to the file.  The file is truncated according to the value of the
     * control parameter.
     *
     * Ensure that the parent directory of the output file exists and is a directory.
     * Create the parent directory if necessary.
     * 
     * @param outputFile The file to which to write.
     * @param doTruncate Control parameter: Tells whether to truncate the target file.
     *
     * @return A new writer for the file.
     *
     * @throws IOException Thrown if the writer cannot be obtained.  Thrown if the directory
     *     of the target file cannot be created, or exists as a simple file.  Thrown if an
     *     output stream cannot be opened on the file.
     */
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

    /**
     * Create a binary writer on an output stream.
     * 
     * @param outputName A name associated with the output stream.
     * @param outputStream An output stream.
     *
     * @return A new writer for the output stream.
     */
    @Trivial
    public TargetCacheImpl_WriterBinary createBinaryWriter(String outputName, OutputStream outputStream) 
        throws IOException {

        return getFactory().createBinaryWriter(outputName, outputStream);
        // 'createBinaryWriter' throws IOException
    }

    /**
     * Write to a specified file using a binary write action.
     * 
     * Create a writer on the specified file, perform the write action using the
     * writer, then finish (close) the writer.
     *
     * Note the time spent writing, including time spent creating and finish the
     * writer, and add that time to the overall write time accumulated by this
     * cache data.
     *
     * Capture and log any write failure.
     *
     * @param description A description of the write action which is being performed.
     * @param outputFile The file to which to write.
     * @param doTruncate Control parameter: Tells whether to truncate the target file.
     * @param writeAction The action which performs the write.
     */
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
        
        // TODO: Should the success or failure of the write action be provided as a return
        //       value?
    }

    // Read APIs ...

    /**
     * Create a reader for an input file.  The reader encapsulates an input
     * stream to the file.
     *
     * @param inputFile The file which is to be read.
     *
     * @return A new reader for the file.
     *
     * @throws IOException Thrown if the reader cannot be obtained.
     */
    @Trivial
    public TargetCacheImpl_Reader createReader(File inputFile) throws IOException {
        String inputPath = inputFile.getName();

        InputStream inputStream = openInputStream(inputFile); // throws IOException

        return createReader(inputPath, inputStream);
    }

    /**
     * Create a reader for an input stream.
     *
     * @param inputStream The stream which is to be read.
     *
     * @return A new reader for the stream.
     */
    @Trivial
    public TargetCacheImpl_Reader createReader(String inputPath, InputStream inputStream) {
        return getFactory().createReader(inputPath, inputStream);
    }

    /**
     * Perform a sequence of reads on a target file.
     *
     * Create a reader on the input file, then read the specified
     * objects, then finish (close) the reader.
     *
     * Log any errors, and answer true or false telling if all of
     * the reads were successful.
     *
     * @param inputFile The file which is to be read.
     *
     * @param readables A sequence readable objects.
     *
     * @return True or false telling if all of objects were read.
     */
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

    /**
     * Log errors from a read operation.  Tell if the operation was successful.
     *
     * @param file The file which was read.
     * @param boundException An exception which occurred during the attempted read.
     * @param parseErrors Parse errors which occurred during the attempted read.
     *
     * @return True or false telling if there were no errors.  That is, if the
     *     exception was null and the parse errors collection was null or empty.
     */
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

    // Unused reader for combined stamp, classes, and targets tables.

    public String basicValidCombined(
        File inputFile,
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

    /**
     * Create a binary reader for an input file.  The reader encapsulates an input
     * stream to the file.
     *
     * For efficiency, the target file is pre-loaded into a byte array.
     * 
     * According to the control parameter, the entire file may be read, or the
     * stamp subset may be read.  See {@link TargetCacheImpl_WriterBinary#STAMP_SIZE}.
     * Support for partial reads is provided for efficient stamp reads.  This function
     * is not currently used.
     *
     * @param inputPath The path to the file which is to be read.
     * @param readStrings Control parameter: Tell if the file has a string table.
     * @param readFull Control parameter: Tell how much of the file is pre-read.  The
     *     entire file, or just the stamp region.
     *
     * @return A new reader for the file.
     *
     * @throws IOException Thrown if the reader cannot be obtained.
     */
    @Trivial
    public TargetCacheImpl_ReaderBinary createBinaryReader(
        String inputPath, boolean readStrings, boolean readFull) throws IOException {

        return getFactory().createBinaryReader(inputPath, readStrings, readFull);
        // 'createBinaryReader' throws IOException
    }

    /**
     * Perform a binary read.
     *
     * Create a binary reader on the target file, then perform the read using that
     * reading, then finish (close) the reader.
     *
     * Log any errors, and answer true or false telling if the read was successful.
     *
     * @param inputFile The file which is to be read.
     * @param readStrings Control parameter: Tell if the file has a string table.
     * @param readFull Control parameter: Tell how much of the file is pre-read.  The
     *     entire file, or just the stamp region.
     * @param readAction The action used to perform the read.
     *
     * @return True or false telling if all of objects were read.
     */
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

    // Unused binary reader for combined stamp, classes, and targets tables.

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

    //

    /** The name of this cache data. */
    protected final String name;
    /** The encoded name of this cache data. */
    protected final String e_name;

    /**
     * Answer the name of this cache data.
     * 
     * @return The name of this cache data.
     */
    @Trivial
    public String getName() {
        return name;
    }

    /**
     * Answer the encoded name of this cache data.
     * 
     * The encoded name is safe for use as a file name.
     *
     * The encoded name may be null, in which case the
     * data file will be null, and reads and writes of this
     * data are disabled.
     *
     * @return The encoded name of this cache data.
     */
    @Trivial
    public String e_getName() {
        return e_name;
    }

    /**
     * Tell if this data is named.  That is, if the encoded name
     * is not null.
     * 
     * Unnamed data is prevented from being read or written.
     *
     * @return True or false telling if this data is named.
     */
    @Trivial
    public boolean isNamed() {
        return ( e_name != null );
    }

    //

    /**
     * The data file of this cache data.
     * 
     * The data file is absolute for the root cache data, or
     * is a child of the file of the parent of the cache data,
     * using the cache data's encoded name.
     */

    protected final File dataFile;

    /**
     * The data file of this cache data.
     * 
     * May be null, in which case reads and writes are disabled
     * for this cache data.
     *
     * @return The data file of this cache data.
     */
    @Trivial
    public File getDataFile() {
        return dataFile;
    }

    /**
     * Tell if the data file of this cache data exists.
     * 
     * Answer false if the data file is null.
     *
     * @return True or false telling if the data file exists.
     */
    @Trivial
    public boolean exists() {
        return ( exists( getDataFile() ) );
    }

    // Data may be stored in a single file, or in a directory
    // (which will contain one or more files), as decided by
    // particular subclasses.

    // Calls to obtain files in the cache tree MUST use a relative constructor.
    // The files must be created with a proper relationship to their parent.
    /**
     * Create a data file as a child of the data file of this cache data.
     *
     * Answer null for a null relative path, or if the data file of this
     * cache data is null.
     *
     * @param relativePath The relative path to the new child file.
     *
     * @return The new child file, or null.
     */
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

    /**
     * Obtain the container file for a specified base location.  That
     * is, add the container prefix and suffix values to the base location.
     * 
     * See {@link #e_addConPrefix}.
     *
     * @param e_conPath The base location of the container file.
     *
     * @return The base location updated with container prefix and suffix values.
     */
    @Trivial
    protected File e_getConFile(String e_conPath) {
        return getDataFile( e_addConPrefix(e_conPath) );
    }

    //

    /**
     * Naming helper: Attach application prefix and suffix values to
     * an encoded application name.
     *
     * @param e_appName The encoded application name.
     *
     * @return The name plus application prefix and suffix values.
     */
    public String e_addAppPrefix(String e_appName) {
        return ( TargetCacheImpl_Utils.APP_PREFIX_WIDGET.e_addPrefix(e_appName) );
    }

    /**
     * Naming helper: Remove application prefix and suffix values from
     * an encoded application name.
     *
     * @param e_appName The encoded application name.
     *
     * @return The name minus application prefix and suffix values.
     */
    public String e_removeAppPrefix(String e_appName) {
        return ( TargetCacheImpl_Utils.APP_PREFIX_WIDGET.e_removePrefix(e_appName) );
    }

    /**
     * Naming helper: Attach module prefix and suffix values to
     * an encoded module name.
     *
     * @param e_appName The encoded module name.
     *
     * @return The name plus module prefix and suffix values.
     */
    public String e_addModPrefix(String e_modName) {
        return ( TargetCacheImpl_Utils.MOD_PREFIX_WIDGET.e_addPrefix(e_modName) );
    }

    /**
     * Naming helper: Remove module prefix and suffix values from
     * an encoded module name.
     *
     * @param e_appName The encoded module name.
     *
     * @return The name minus module prefix and suffix values.
     */
    public String e_removeModPrefix(String e_modName) {
        return ( TargetCacheImpl_Utils.MOD_PREFIX_WIDGET.e_removePrefix(e_modName) );
    }

    /**
     * Naming helper: Attach container prefix and suffix values to
     * an encoded container name.
     *
     * @param e_appName The encoded container name.
     *
     * @return The name plus container prefix and suffix values.
     */
    public String e_addConPrefix(String e_conPath) {
        return ( TargetCacheImpl_Utils.CON_PREFIX_WIDGET.e_addPrefix(e_conPath) );
    }

    /**
     * Naming helper: Remove container prefix and suffix values from
     * an encoded container name.
     *
     * @param e_appName The encoded container name.
     *
     * @return The name minus container prefix and suffix values.
     */
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

    // File utilities ...

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

    // Time tracking APIs ...

    // TODO: Tracking of read times is not symmetric with tracking of write times.
    //       Compare callers to 'addReadTime' with callers to 'addWriteTime' to not
    //       the asymmetry.  These patterns should be unified.

    /** Total time spent reading cache data.  Measured in nano-seconds. */
    private long readTime;

    /**
     * Answer the total time spent reading cache data, in nano-seconds.
     *
     * @return The total time spent reading cache data, in nano-seconds.
     */
    @Trivial
    public long getReadTime() {
        return readTime;
    }

    /**
     * Add to the time spent reading cache data.
     *
     * Add the difference between the current time and the specified
     * start time.
     *
     * @param start The start time of the read action.
     * @param description A description of the read action.
     *
     * @return The duration of the read action.
     */
    public long addReadTime(long start, String description) {
        long duration = System.nanoTime() - start;
        readTime += duration;

        // System.out.println("Read [ " + description + " ] [ " + duration + " (ns) ]");

        return duration;
    }

    /** Total time spent writing cache data.  Measured in nano-seconds. */
    private long writeTime;

    /**
     * Answer the total time spent writing cache data, in nano-seconds.
     *
     * @return The total time spent writing cache data, in nano-seconds.
     */
    @Trivial
    public long getWriteTime() {
        return writeTime;
    }

    /**
     * Add to the time spent writing cache data.
     *
     * Add the difference between the current time and the specified
     * start time.
     *
     * @param start The start time of the write action.
     * @param description A description of the write action.
     *
     * @return The duration of the write action.
     */
    public long addWriteTime(long start, String description) {
        long duration = System.nanoTime() - start;
        writeTime += duration;

        // System.out.println("Write [ " + description + " ] [ " + duration + " (ns) ]");

        return duration;
    }
}
