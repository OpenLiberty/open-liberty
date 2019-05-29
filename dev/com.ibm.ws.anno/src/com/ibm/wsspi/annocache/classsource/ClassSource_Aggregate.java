/*******************************************************************************
 * Copyright (c) 2011, 2019 IBM Corporation and others.
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.ibm.wsspi.annocache.util.Util_InternMap;

/**
 * <p>Aggregator of class sources.</p>
 *
 * <p>An important detail for child class sources is the scan policy which
 * is set for the child class source. These are mainly used for WEB modules,
 * which must carefully partition the scan results depending on scan policy.</p>
 *
 * <p>Servlet annotations in metadata-complete and excluded locations of a WAR
 * file are ignored. Servlet Container Initializer (SCI) and Manage Beans annotations
 * are processing in all locations of a WAR file.</p>
 *
 * <p>For JAR type modules (EJB and Client), child class sources are obtained
 * for these parts of the module class path:</p>
 *
 * <ul><li>The JAR contents</li>
 * <li>MANIFEST Class-Path jars</li>
 * <li>Application library jars</li>
 * <li>The external references class loader</li>
 * </ul>
 *
 * <p>For a WEB module, child class sources are obtained for these parts
 * of the web module class path:</p>
 *
 * <ul><li>WEB-INF/classes</li>
 * <li>WEB-INF/lib/*.jar</li>
 * <li>MANIFEST Class-Path jars</li>
 * <li>Application library jars</li>
 * <li>The external references class loader</li>
 * </ul>
 *
 * <p>The classes directory should be represented by a single class source.
 * Each WEB-INF/lib jar should be represented by a single class source. The
 * remainder of the class path may be represented by one or several class
 * sources.</p>
 *
 * <p>Scan policies are set as:</p>
 *
 * <ul><li>SEED</li>
 * <li>PARTIAL</li>
 * <li>EXCLUDED</li>
 * <li>EXTERNAL</li>
 * </ul>
 *
 * <p>For a web module, in all cases, all locations outside of the web module
 * archive are marked as EXTERNAL. Locations within the web module archive
 * are marked using the following specialized rules:</p>
 *
 * <p>A web module with no locations marked as metadata-complete and with no
 * absolute ordering will have WEB-INF/classes and all WEB-INF library jars
 * marked as SEED.</p>
 *
 * <p>A web module which is not metadata-complete and which has some jars
 * marked as metadata-complete, and which has no absolute ordering, has
 * WEB-INF/classes marked as SEED, has the non-metadata complete jars marked
 * as PARTIAL.</p>
 *
 * <p>A web module which is metadata-complete and which has an absolute ordering
 * has WEB-INF/classes marked as PARTIAL and has jars marked as PARTIAL or
 * EXCLUDED depending on which jars are present in the absolute ordering.</p>
 *
 * <p>A web module which is not metadata-complete and which has an absolute ordering
 * has WEB-INF/classes marked as SEED, has non-metadata-complete jars which are listed
 * in the absolute ordering marked as SEED, has metadata-complete jars which are
 * listed in the absolute ordering marked as PARTIAL, and has other jars not listed
 * in the absolute ordering marked as EXCLUDED.</p>
 *
 * <p>During scans, annotations are read from all SEED, PARTIAL, and EXCLUDED
 * locations. The scan places the annotations data for these locations in
 * independent storage, allowing each subset of annotations to be independently
 * queried.</p>
 *
 * <p>During scans, class information is read for all classes in SEED and PARTIAL
 * locations. Class information for EXCLUDED and EXTERNAL locations is read only
 * to complete class information for other classes.</p>
 */
public interface ClassSource_Aggregate extends com.ibm.wsspi.anno.classsource.ClassSource_Aggregate {
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

    // Identify ...
    
    /**
     * <p>Answer the name of the application of this class source.</p>
     *
     * @return The name of the application of this class source.
     */
    String getApplicationName();

    /**
     * <p>Answer the name of the module of this class source.</p>
     *
     * @return The name of the module of this class source.
     */
    String getModuleName();

    /**
     * <p>Answer the module category name of this class source.</p>
     * 
     * <p>The category name is used to enable multiple results for
     * the same module.  Multiple results are needed because CDI
     * and J2EE process modules differently.
     *
     * @return The category name of this class source.
     */
    String getModuleCategoryName();

    // Structure ...

    /**
     * <p>Control value for processing web module components.</p>
     *
     * <p>Scan policy values ({@link ScanPolicy#getValue}) are
     * intended to be bitwise OR'ed with each other to generate
     * selection values for annotation target access. See, for
     * example,
     * {@link com.ibm.wsspi.annocache.targets.AnnotationTargets_Targets#getAnnotatedClasses()},
     * which obtains values for the seed region, and
     * {@link com.ibm.wsspi.annocache.targets.AnnotationTargets_Targets#getAnnotatedClasses(int)},
     * which obtains values for the regions specified by the scan policies parameter.</p>
     *
     * <p>The four scan policies are not fully symmetric! Annotations are
     * recorded for the seed, partial, and excluded regions, and are not recorded
     * for the external region.</p>
     *
     * <p>The purpose and utility of the scan policy partitioning of annotations
     * is to support specialized rules for detecting annotations in web modules.
     * Web modules obtain scan results for at least three purposes: Generation of
     * Servlet metadata; Detection of Servlet Container Initializers (SCI); Generation
     * of Managed Beans metadata. Of these three purposes, the first uses annotations
     * from only the seed region, while SCI and Managed Beans uses annotations from
     * all of the seed, partial, and excluded regions.</p>
     */
    public enum ScanPolicy {
        /**
         * <p>Policy for non-metadata-complete regions of the target scan space. For
         * JAR scans (EJB and CLIENT), this region will be of the JAR contents and
         * nothing else. For WAR scans (WEB), this region will be the non-metadata
         * complete subset of the WAR.</p>
         *
         * <p>Annotations are scanned from the seed region and are the results
         * obtained by accessing annotations through the annotation targets table
         * through default queries.  For example:
         * {@link com.ibm.wsspi.annocache.targets.AnnotationTargets_Targets#getAnnotatedClasses()}.</p>
         */
        SEED(0x01),

        /**
         * <p>Policy for metadata-complete (but not excluded) regions of the target
         * scan space. This is intended to support WAR scans, which partition the
         * WAR scan space into non-metadata-complete, metadata-complete, and excluded
         * regions. The partial region included metadata-complete fragments. The
         * partial region will include the WEB-INF/classes location if the main web
         * module descriptor is metadata complete.</p>
         *
         * <p>Annotations are scanned from the partial region. However, the default
         * queries do not obtain results from the partial region. To obtain results
         * from the partial region, a scan policy selector must be provided. For
         * example:
         * {@link com.ibm.wsspi.annocache.targets.AnnotationTargets_Targets#getAnnotatedClasses(int)}.</p>
         */
        PARTIAL(0x02),

        /**
         * <p>Policy for excluded regions of the target scan space. This is intended
         * to support WAR scans, which partition the WAR scan space into
         * non-metadata-complete, metadata-complete, and excluded regions. Excluded
         * regions include those web module jars (jars under WEB-INF/lib) which
         * are excluded from an absolute ordering in the main web module descriptor.</p>
         *
         * <p>Annotations are scanned from the excluded region. However, the default
         * queries do not obtain results from the partial region. To obtain results
         * from the partial region, a scan policy selector must be provided. For
         * example:
         * {@link com.ibm.wsspi.annocache.targets.AnnotationTargets_Targets#getAnnotatedClasses(int)}.</p> 
         */
        EXCLUDED(0x04),

        /**
         * <p>Policy for regions outside of the core region of the target scan space.
         * For all module type scans (EJB and CLIENT for JAR files, WEB for WAR files),
         * this includes all parts of the scan space which is external to the target
         * module. For most modules, this includes the module MANIFEST class path
         * elements, JAR files from the application library, JAR files from shared
         * libraries, and any elements of the module external references class loader.</p>
         *
         * <p>The external region has two distinguishing features which differentiate
         * it from the seed, partial, and excluded regions:</p>
         *
         * <ul>
         * <li>The external region is only scanned to complete class information
         * for classes from the other regions.</li>
         * <li>No annotations are recorded for classes scanned from the external
         * region.</li>
         * </ul>
         */
        EXTERNAL(0x08);

        private ScanPolicy(int value) {
            this.value = value;
        }

        protected int value;

        /**
         * <p>Integer value for the scan policy. This is intended to
         * be bitwise OR'ed to generate scan policy selectors for annotation
         * targets methods.</p>
         *
         * @return An integer value for the scan policy.
         */
        public int getValue() {
            return value;
        }

        /**
         * <p>Tell if this scan policy accepts the specified policy
         * selector. The selector is accepted if it contains the
         * bit value of the scan policy.</p>
         *
         * @param policies The bitwise OR of scan policy values to test.
         * @return True if the bit value of this scan policy is set in the policies.
         *         Otherwise, false.
         *
         *         {@link #getValue()}
         */
        public boolean accept(int policies) {
            return ((policies & getValue()) != 0);
        }

        //

        /**
         * <p>Helper: Bitwise OR of all recorded regions ({@link #SEED},
         * {@link #PARTIAL}, {@link #EXCLUDED}.</p>
         */
        public static final int ALL_EXCEPT_EXTERNAL =
                        SEED.getValue() | PARTIAL.getValue() | EXCLUDED.getValue();

        /**
         * <p>Tell if the scan policy is singular -- has only one
         * scan policy bit value set.</p>
         *
         * @param policies The bitwise OR of scan policy values to test.
         * @return True if exactly one policy bit value is set. Otherwise,
         *         false.
         *
         *         {@link #getValue()}
         */
        public static boolean isSingular(int policies) {
            return ((policies == SEED.getValue()) ||
                    (policies == PARTIAL.getValue()) ||
                    (policies == EXCLUDED.getValue()) ||
                    (policies == EXTERNAL.getValue()));
        }

        /**
         * <p>Answer the singular policy which is represented by a
         * scan policy selector. Answer null if the scan policy selector
         * does not match exactly one scan policy.</p>
         *
         * @param policies A bitwise OR of scan value policies.
         * @return The single scan policy which matches the scan policy
         *         selector. Null if the scan policy selector does not
         *         exactly one scan policy.
         *
         *         {@link #getValue()}
         */
        public static ScanPolicy asSingular(int policies) {
            return ((policies == SEED.getValue()) ? SEED :
                    (policies == PARTIAL.getValue()) ? PARTIAL :
                    (policies == EXCLUDED.getValue()) ? EXCLUDED :
                    (policies == EXTERNAL.getValue()) ? EXTERNAL : null);
        }
    }

    /**
     * <p>Main API to add new class sources. Note that the added class source need not have
     * the same factory as the aggregate class source. Add the class source with the SEED
     * scan policy.</p>
     *
     * @param classSource The class source to add to this aggregate.
     */
    void addClassSource(ClassSource classSource);

    /**
     * <p>Main API to add new class sources.
     *
     * @param classSource The class source to add to this aggregate.
     */
    void addClassLoaderClassSource(ClassSource_ClassLoader classSource);

    /**
     * <p>Main API to add new class sources. Note that the added class source need not have
     * the same factory as the aggregate class source. Add the class source using the
     * supplied scan policy.</p>
     *
     * @param classSource The class source to add to this aggregate.
     *
     * @param scanPolicy The policy to apply to the class source.
     */
    void addClassSource(ClassSource classSource, ScanPolicy scanPolicy);

    /**
     * <p>Answer the entire list of class sources of this aggregate.</p>
     *
     * <p>The order is significant, and is used to handle precedence for
     * classes with multiple occurrences.</p>
     *
     * @return The entire list of class sources of this aggregate.
     */
    List<? extends ClassSource> getClassSources();

    /**
     * <p>Answer the subset of class sources which have the specified scan policy.</p>
     *
     * @param scanPolicy The scan policy on which to select class sources.
     *
     * @return The class sources which have the specified scan policy.
     */
    Set<? extends ClassSource> getClassSources(ScanPolicy scanPolicy);

    /**
     * <p>Answer the subset of seed class sources of this aggregate.</p>
     *
     * <p>Unless partial class sources are defined, the sets of seed and
     * excluded class sources partition the entire list of class sources.
     * When partial class sources are defined, the seed, partial, and
     * excluded class sources partition the entire list of class sources.</p>
     *
     * @return The subset of seed class sources of this aggregate.
     */
    Set<? extends ClassSource> getSeedClassSources();

    /**
     * <p>Answer the subset of partial class sources of this aggregate.</p>
     *
     * @return The subset of partial class sources of this aggregate.
     */
    Set<? extends ClassSource> getPartialClassSources();

    /**
     * <p>Answer the subset of excluded class sources of this aggregate.</p>
     *
     * @return The subset of excluded class sources of this aggregate.
     */
    Set<? extends ClassSource> getExcludedClassSources();

    /**
     * <p>Answer the subset of external class sources of this aggregate.</p>
     *
     * @return The subset of external class sources of this aggregate.
     */
    Set<? extends ClassSource> getExternalClassSources();

    /**
     * <p>Tell the scan policy of the class source.</p>
     *
     * @param classSource The class source for which to tell the scan policy.
     *
     * @return The scan policy of the class source.
     */
    ScanPolicy getScanPolicy(ClassSource classSource);

    /**
     * Tell the count of internal class sources.
     *
     * The count is used during scanning to optimize threading and
     * string management.
     *
     * @return The count of internal class sources.
     */
    int getInternalSourceCount();

    // Class source naming support ...

    String getCanonicalName(String classSourceName);
    Map<String, String> getCanonicalNames();
    
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

    List<ClassSource> getSuccessfulOpens();
    List<ClassSource> getFailedOpens();

    // Resource lookup ...

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
    BufferedInputStream openClassResourceStream(String className, String resourceName)
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

    // Lookup statistics ...

    long getTotalLookups();
    long getRepeatLookups();

    Map<String, Integer> getLookupCounts();

    Boolean getGlobalResult(String resourceName);
    Set<String> getFailedLookups(ClassSource classSource);

    Map<String, ? extends ClassSource> getFirstSuccesses();
    ClassSource getFirstSuccess(String resourceName);

    //

    /**
     * Total nano-seconds spend reading data from the annotations cache.
     * 
     * @return The nano-seconds spending reading data from the annotations cache.
     */
    long getCacheReadTime();

    /**
     * Add to the nano-second cache read time.
     * 
     * @param readTime Nano-seconds to add to the cache read time.
     * @param description A short description of the read activity.
     *     Used for tracing.
     *
     * @return The new total read time.
     */
    long addCacheReadTime(long readTime, String description);

    /**
     * Total nano-seconds spend writing data to the annotations cache.
     * 
     * @return The nano-seconds spending writing data to the annotations cache.
     */    
    long getCacheWriteTime();

    /**
     * Add to the nano-second cache write time.
     * 
     * @param writeTime Nano-seconds to add to the cache write time.
     * @param description A short description of the write activity.
     *     Used for tracing.
     *
     * @return The new total write time.
     */
    long addCacheWriteTime(long writeTime, String description);

    interface TimingData {
        int getScanSources();
        int getScanClasses();
        long getScanTime();

        int getReadSources();
        int getReadClasses();
        long getReadTime();

        int getJandexSources();
        long getJandexTime();
        int getJandexClasses();

        int getExternalSources();
        long getExternalTime();
        int getExternalClasses();

        long getCacheReadTime();
        long getCacheWriteTime();

        TimingData clone();
    }

    TimingData getTimingData();
}
