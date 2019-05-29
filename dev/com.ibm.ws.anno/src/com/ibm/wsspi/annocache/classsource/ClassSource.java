/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.annocache.classsource;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Set;
import java.util.logging.Logger;

import com.ibm.wsspi.annocache.util.Util_InternMap;

//
// Class names (e.g., "java.lang.Object").
//
// Internal resource names (e.g., "java/lang/Object.class").
//
// Internal system dependent resource names (e.g., "java/lang/Object.class" or
// "java\lang\Object.class", depending on the platform.
//
// A class source traces scanned *class names*, and has the responsibility
// to locally convert a class name to an internal or external resource name.

public interface ClassSource extends com.ibm.wsspi.anno.classsource.ClassSource {
    // Logging ...

    /**
     * <p>A string representation of the class source suitable for logging.
     * The hash text should include a unique identifier (usually the base
     * hash code of the class source) plus the most relevant descriptive
     * information for the class source.</p>
     *
     * @return A string representation of the class source suitable for logging.
     */
    String getHashText();

    /**
     * <p>Log state information for the class source.</p>
     */
    void logState();

    /**
     * <p>Log state information for the class source to a specified
     * logger. State information uses 'debug' log enablement.</p>
     *
     * @param useLogger A logger which is to receive state information.
     */
    void log(Logger useLogger);

    // Embodiement ...

    /**
     * Answer the options of the class source.
     *
     * @return The options of the class source.
     */
    ClassSource_Options getOptions();

    /**
     * <P>The factory used to create this class source. Other factory based
     * objects created by the class source will use this factory.</p>
     *
     * @return The factory used to create this class source.
     */
    ClassSource_Factory getFactory();

    /**
     * <p>Answer the string intern map of the class source.</p>
     *
     * <p>Class sources intern all class names of scanned classes.</p>
     *
     * @return The intern map of the class source.
     */
    Util_InternMap getInternMap();

    //

    String NO_ENTRY_PREFIX = null;

    /**
     * Answer the prefix to use when selecting entries.
     * 
     * @return The prefix to use when selecting entries.
     *     When no, no prefix is used.
     */
    String getEntryPrefix();

    // Identity ...

    /**
     * <p>A name for this class source.</p>
     *
     * <p>The class source name is used as a unique ID when storing values to
     * annotation targets.</p>
     *
     * <p>When adding class sources to an aggregate, the names of the child class
     * sources must be unique.</p>
     *
     * @return A name for this class source.
     */
    String getName();

    /**
     * <p>Answer the canonical name of the class source. All use
     * of the class source uses the canonical name.</p>
     *
     * @return The canonical name of the class source.
     *
     * @see ClassSource_Factory#getCanonicalName(String)
     */
    String getCanonicalName();

    // Structure ...

    /**
     * <p>Answer the parent of this class source. Answer null if none is set.</p>
     *
     * @return The parent of this class source.
     */
    ClassSource_Aggregate getParentSource();

    /**
     * <p>Set the parent of this class source.</p>
     *
     * @param parent The parent of this class source.
     */
    void setParentSource(ClassSource_Aggregate parent);

    // Stamping ...

    String UNAVAILABLE_STAMP = "** UNAVAILABLE **";

    /**
     * <p>Value used for containers which do not record a time stamp.  This is
     * used for aggregate, class loader, and directory containers.</p>
     */
    String UNRECORDED_STAMP = "** UNRECORDED **";

    /**
     * <p>Answer the time stamp of the class source.</p>
     *
     * <p>The time stamp, if a numeric value, provides the
     * @return The time stamp of the class source.
     */
    String getStamp();

    // State ...

    /**
     * Open the class source for use. This will open any underlying objects.
     *
     * @throws ClassSource_Exception Thrown if the class source could not be opened.
     */
    void open() throws ClassSource_Exception;

    /**
     * Close the class source. Close any underlying objects.
     *
     * @throws ClassSource_Exception Thrown if the class source could not be closed.
     */
    void close() throws ClassSource_Exception;

    // Scanning ...

    /**
     * Leaf processing API: Process this leaf class source using a supplied streamer.
     * 
     * @param streamer The streamer to apply to this class source.
     *
     * @throws ClassSource_Exception Thrown if an error occured while applying the streamer.
     */
    void process(ClassSource_Streamer streamer) throws ClassSource_Exception;

    /**
     * <p>Tell if this class source used jandex processing.
     * 
     * @return True or false telling if this class source used jandex processing.
     */
    boolean isProcessedUsingJandex();

    /**
     * <p>Tell if this class source was processed using cache data.
     *
     * @return True or false telling if this class source used cache data.
     */
    boolean isReadFromCache();

    /**
     * Set that this class source was processed using cache data.
     *
     * @param processTime The time spent reading cache data.
     * @param processCount The count of classes read from the cache.
     */
    void setReadFromCache(long processTime, int processCount);

    /**
     * <p>Answer the time in nano-seconds spent processing the source data.</p>
     *
     * @return The time in nano-speconds spent processing the source data.
     */
    long getProcessTime();

    /**
     * <p>Answer the count of classes processed.
     *
     * @return The count of classes processed.
     */
    int getProcessCount();

    //

    void processSpecific(ClassSource_Streamer streamer, Set<String> i_classNames)
        throws ClassSource_Exception;

    // Static class naming helpers ...

    /** <p>Constant resource separation character.</p> */
    char RESOURCE_SEPARATOR_CHAR = '/';

    /** <p>Constant resource separation string.</p> */
    String RESOURCE_SEPARATOR_STRING = "/";

    /** <p>Constant class name separation character.</p> */
    char CLASS_SEPARATOR_CHAR = '.';

    /** <p>Constant extension for class resource names.</p> */
    String CLASS_EXTENSION = ".class";

    /** <p>Constant for inner classes.</p> */
    char INNER_CLASS_SEPARATOR = '$';

    // Stream handling ...

    /** The buffer size for reading jandex indexes. */
    int JANDEX_BUFFER_SIZE = 32 * 1024;

    /** The buffer size for reading classes. */
    int CLASS_BUFFER_SIZE = 8 * 1024;

    /**
     * <p>Open a buffered input stream for a named class which has a specified resource name.</p>
     *
     * @param className The name of the class for which to open an input stream.
     * @param resourceName The Name of the resource which is to be opened.
     *
     * @return The input stream for the named class. Null if no resource is
     *     available for the class.
     *
     * @throws ClassSource_Exception Thrown in case a resource is available
     *     for the class, but that resource could not be opened.
     */
    InputStream openClassResourceStream(String className, String resourceName)
        throws ClassSource_Exception;

    /**
     * <p>Open a buffered input stream for a named class which has a specified resource name.</p>
     *
     * @param className The name of the class for which to open an input stream.
     * @param resourceName The Name of the resource which is to be opened.
     * @param bufferSize The size of buffer to use for the input stream.
     *
     * @return The input stream for the named class. Null if no resource is
     *         available for the class.
     *
     * @throws ClassSource_Exception Thrown in case a resource is available
     *             for the class, but that resource could not
     *             be opened.
     */
    BufferedInputStream openResourceStream(String className, String resourceName, int bufferSize)
        throws ClassSource_Exception;

    /**
     * <p>Open an input stream for a named class which has a specified resource name.</p>
     *
     * <p>This code point is exposed to minimize class name to resource name conversion:
     * Processing which prefers to use the resource name will generate a class name, but
     * should not be forced to discard the resource name.</p>
     *
     * <p>Note the distinct cases: If no resource is available for the class,
     * answer null. If a resource is available but cannot be opened, throw an
     * exception.</p>
     *
     * @param className The name of the class for which to open an input stream.
     * @param resourceName The Name of the resource which is to be opened.
     *
     * @return The input stream for the named class. Null if no resource is
     *         available for the class.
     *
     * @throws ClassSource_Exception Thrown in case a resource is available
     *             for the class, but that resource could not
     *             be opened.
     */
    InputStream openResourceStream(String className, String resourceName) throws ClassSource_Exception;

    /**
     * <p>Class the input stream which was opened for a specified class.</p>
     *
     * <p>This code point is exposed to minimize class name to resource name conversion:
     * Processing which prefers to use the resource name will generate a class name, but
     * should not be forced to discard the resource name.</p>
     *
     * @param className The class for which the input stream was opened.
     * @param inputStream The input stream which is to be closed.
     *
     * @throws ClassSource_Exception Thrown in case the input stream could not be closed.
     */
    void closeResourceStream(String className, String resourceName, InputStream inputStream) throws ClassSource_Exception;
}
