/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wsspi.anno.classsource;

import java.io.InputStream;
import java.util.Set;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.util.Util_InternMap;

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

public interface ClassSource {
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
     * @param logger A logger which is to receive state information.
     */
    void log(TraceComponent logger);

    // Context and identity ...

    /**
     * <P>The factory used to create this class source. Other factory based
     * objects created by the class source will use this factory.</p>
     * 
     * @return The factory used to create this class source.
     */
    ClassSource_Factory getFactory();

    /**
     * <p>Answer the parent of this class source. Answer null if none is set.</p>
     * 
     * @return The parent of this class source.
     */
    ClassSource getParentSource();

    /**
     * <p>Set the parent of this class source.</p>
     * 
     * @param parent The parent of this class source.
     */
    void setParentSource(ClassSource classSource);

    /**
     * <p>A name for this class source.</p>
     * 
     * <p>The class source name is used as a unique ID when storing values to
     * annotation targets. See {@link com.ibm.wsspi.anno.targets.AnnotationTargets_Targets#getClassSourceNames()}.</p>
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

    //

    // State management ...

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

    // String management ...

    /**
     * <p>Answer the string intern map of the class source.</p>
     * 
     * <p>Class sources intern all class names of scanned classes.</p>
     * 
     * @return The intern map of the class source.
     */
    Util_InternMap getInternMap();

    //

    /**
     * <p>Entry point for scanning a class source which is a child of an aggregate
     * class source.</p>
     * 
     * @param streamer A selection and processing helper for the scan operation.
     * @param i_seedClassNamesSet The accumulated seed class names.
     * @param scanPolicy The scan policy of the class source (recorded by the parent).
     */
    void scanClasses(ClassSource_Streamer streamer, Set<String> i_seedClassNamesSet, ScanPolicy scanPolicy);

    //

    /**
     * <p>Answer statistics for a scan processing. (These are only available
     * after scanning is complete.)</p>
     * 
     * @return Statistics for scan processing.
     */
    ClassSource_ScanCounts getScanResults();

    /**
     * <p>Answer a specific field from the scan results table.</p>
     * 
     * @param resultField The scan results field which is to be retrieved.
     * 
     * @return The value of the requested scan results field.
     */
    int getResult(ClassSource_ScanCounts.ResultField resultField);

    // Alternate scan processing ...

    /**
     * <p>Alternate scan processing step: Perform scanning only on specific class.</p>
     * 
     * @param specificClassNamesThe name of the class which is to be scanned.
     * @param streamer A selection and processing helper for the scan operation.
     * 
     * @return True if the streamer processed the class. Otherwise, false.
     * 
     * @throws ClassSource_Exception Thrown in case of an error during scan processing.
     */
    boolean scanSpecificSeedClass(String specificClassName, ClassSource_Streamer streamer) throws ClassSource_Exception;

    //

    /**
     * <p>Required entry point for scans of referenced classes.</p>
     * 
     * @param referencedClassNam The names of a referenced classes which
     *            requires scanning.
     * @param streamer A selection and processing helper for the scan operation.
     * 
     * @return True if the streamer processed the class. Otherwise, false.
     * 
     * @throws ClassSource_Exception Thrown in case of an error during scan processing.
     */
    boolean scanReferencedClass(String referencedClassNam, ClassSource_Streamer streamer) throws ClassSource_Exception;

    //

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

    /**
     * <p>Perform a resource append operation: This places
     * a resource separator between the supplied values.
     * 
     * @param head The first value to put into the concatenated value.
     * @param tail The second value to put into the concatenated value.
     * 
     * @return The first value concatenated with a resource separator
     *         and with the second value.
     */
    String resourceAppend(String head, String tail);

    /**
     * <p>Tell if a specified resource is a directory resource.
     * Resources which end with the resource separator are directory
     * resources.</p>
     * 
     * @param resourceName The resource name to test.
     * 
     * @return True if the resource is a directory resource. Otherwise,
     *         false.
     */
    boolean isDirectoryResource(String resourceName);

    /**
     * <p>Tell if a specified resource is a class resource. A resource
     * is a class resource if and only if it has the class extension.</p>
     * 
     * @param resourceName The resource which is to be tested.
     * 
     * @return True if the resource is a class resource. Otherwise, false.
     */
    boolean isClassResource(String resourceName);

    /**
     * <p>Convert a resource name to a class name. Conversion strips
     * the class extension and converts all resource separators to
     * class separators. Note that the inner class separator is not
     * changed by the conversion.
     * 
     * @param resourceName The resource to convert to a class name.
     * 
     * @return The class name for the resource.
     */
    String getClassNameFromResourceName(String resourceName);

    /**
     * <p>Convert a class name to a resource name. Conversion
     * changes the class separator to the resource separator and
     * adds the class extension. The inner class separator is not
     * changed by the conversion.</p>
     * 
     * @param className The class name to convert to a resource.
     * 
     * @return The resource for the class name.
     */
    String getResourceNameFromClassName(String className);

    /**
     * <p>Optional API for processing which uses an alternate
     * form for resources. For example, directory based processing
     * may change the resource separator to a platform specific
     * separator.</p>
     * 
     * @param externalResourceName The external form of the resource.
     * 
     * @return The internal form of the resource.
     */
    String inconvertResourceName(String externalResourceName);

    /**
     * <p>Optional API for processing which uses an alternate
     * form for resources. For example, directory based processing
     * may change the resource separator to a platform specific
     * separator.</p>
     * 
     * @param internalResourceName The internal form of the resource.
     * 
     * @return The external form of the resource.
     */
    String outconvertResourceName(String internalResourceName);

    // Stream handling ...

    // Cases:
    // 1) The class is not present in the class source.
    // 2) The class is present, but could not be opened.
    //
    // Results:
    // 1) Class is not present: Answer null.
    // 2) Class is present, but could not be opened: Exception
    // 3) Class is present, and could be opened: InputStream

    /**
     * <p>Open an input stream for a named class.</p>
     * 
     * <p>Note the distinct cases: If no resource is available for the class,
     * answer null. If a resource is available but cannot be opened, throw an
     * exception.</p>
     * 
     * @param className The name of the class for which to open an input stream.
     * 
     * @return The input stream for the named class. Null if no resource is
     *         available for the class.
     * 
     * @throws ClassSource_Exception Thrown in case a resource is available
     *             for the class, but that resource could not
     *             be opened.
     */
    InputStream openClassStream(String className) throws ClassSource_Exception;

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
     * @param className The class for which the input stream was opened.
     * @param inputStream The input stream which is to be closed.
     * 
     * @throws ClassSource_Exception Thrown in case the input stream could not be closed.
     */
    void closeClassStream(String className, InputStream inputStream) throws ClassSource_Exception;

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

    //

    /**
     * <p>Answer the count of resources which were excluded from processing because
     * they were not class resources. This includes all container resources and
     * all resources which do not have the class extension.</p>
     * 
     * @return The count of resources excluded from processing as non-class resources.
     * 
     * @see ClassSource#isClassResource(String)
     */
    int getResourceExclusionCount();

    /**
     * <p>Answer the count of class resources which were excluded as duplicates
     * resources for the same class. For example, an aggregate class source may
     * contain a resource for the same class in two different child class sources.
     * Only one of these is processed; the others are excluded and will contribute
     * to the exclusion count.</p>
     * 
     * <p>Class resources skipping by the stream are not included in this count.</p>
     * 
     * <p>The class inclusion count plus the class exclusion count add up to the
     * count of all class resources encountered during processing.</p>
     * 
     * <p>Classes from non-seed class sources are not included in either statistic.</p>
     * 
     * @return The count of class resources for duplicate classes.
     * 
     * @see #getClassInclusionCount()
     * @see ClassSource#isClassResource(String)
     * @see ClassSource_Streamer#doProcess(String)
     */
    int getClassExclusionCount();

    /**
     * <p>Answer the count of class resources for distinct classes. For example, an
     * aggregate class source may contain a resource for the same class in two different
     * child class sources. The class inclusion count is incremented just once for the
     * entire set of duplicating class resources. The exclusion count is incremented
     * once for each of the other duplicating class resources.</p>
     * 
     * <p>Class resources skipping by the stream are not included in this count.</p>
     * 
     * @return The count of class resources for distinct classes.
     * 
     * @see #getClassExclusionCount()
     * @see ClassSource#isClassResource(String)
     * @see ClassSource_Streamer#doProcess(String)
     */
    int getClassInclusionCount();
}