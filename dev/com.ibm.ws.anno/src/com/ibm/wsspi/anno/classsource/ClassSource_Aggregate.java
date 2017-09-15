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

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>Aggregator of class sources.</p>
 * 
 * <p>An important detail for child class sources is the scan policy which
 * is set for the child class source. These are mainly used for WEB modules,
 * which must carefully partition the scan results depending on scan policy.</p>
 * 
 * <p>Servlet annotations in metadata-complete and excluded locations of a WAR
 * file are ignored.  Servlet Container Initializer (SCI) and Manage Beans annotations
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
 * Each WEB-INF/lib jar should be represented by a single class source.  The
 * remainder of the class path may be represented by one or several class
 * sources.</p> 
 *
 * <p>Scan policies are set as:</p>
 * 
 * <ul><li>SEED</li>
 *     <li>PARTIAL</li>
 *     <li>EXCLUDED</li>
 *     <li>EXTERNAL</li>
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
 * as PARTIAL.</p
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
public interface ClassSource_Aggregate extends ClassSource {

    // Core API

    /**
     * <p>Control value for processing web module components.</p>
     * 
     * <p>Scan policy values ({@link ScanPolicy#getValue}) are
     * intended to be bitwise OR'ed with each other to generate
     * selection values for annotation target access.  See, for
     * example,
     * {@link com.ibm.wsspi.anno.targets.AnnotationTargets_Targets#getAnnotatedClasses()},
     * which obtains values for the seed region, and
     * {@link com.ibm.wsspi.anno.targets.AnnotationTargets_Targets#getAnnotatedClasses(int)},
     * which obtains values for the regions specified by the scan policies parameter.</p>
     * 
     * <p>The four scan policies are not fully symmetric!  Annotations are
     * recorded for the seed, partial, and excluded regions, and are not recorded
     * for the external region.</p>
     * 
     * <p>The purpose and utility of the scan policy partitioning of annotations
     * is to support specialized rules for detecting annotations in web modules.
     * Web modules obtain scan results for at least three purposes: Generation of
     * Servlet metadata; Detection of Servlet Container Initializers (SCI); Generation
     * of Managed Beans metadata.  Of these three purposes, the first uses annotations
     * from only the seed region, while SCI and Managed Beans uses annotations from
     * all of the seed, partial, and excluded regions.</p>
     */
    public enum ScanPolicy {
        /**
         * <p>Policy for non-metadata-complete regions of the target scan space.  For
         * JAR scans (EJB and CLIENT), this region will be of the JAR contents and
         * nothing else.  For WAR scans (WEB), this region will be the non-metadata
         * complete subset of the WAR.</p> 
         * 
         * <p>Annotations are scanned from the seed region and are the results
         * obtained by accessing annotations through the annotation targets table
         * through default queries.  For example:
         * {@link com.ibm.wsspi.anno.targets.AnnotationTargets_Targets#getAnnotatedClasses()}.</p>
         */
        SEED(0x01),
        /**
         * <p>Policy for metadata-complete (but not excluded) regions of the target
         * scan space.  This is intended to support WAR scans, which partition the
         * WAR scan space into non-metadata-complete, metadata-complete, and excluded
         * regions.  The partial region included metadata-complete fragments.  The
         * partial region will include the WEB-INF/classes location if the main web
         * module descriptor is metadata complete.</p>
         * 
         * <p>Annotations are scanned from the partial region.  However, the default
         * queries do not obtain results from the partial region.  To obtain results
         * from the partial region, a scan policy selector must be provided.  For
         * example:
         * {@link com.ibm.wsspi.anno.targets.AnnotationTargets_Targets#getAnnotatedClasses(int)}.</p>
         */
        PARTIAL(0x02),
        /**
         * <p>Policy for excluded regions of the target scan space.  This is intended
         * to support WAR scans, which partition the WAR scan space into
         * non-metadata-complete, metadata-complete, and excluded regions.  Excluded
         * regions include those web module jars (jars under WEB-INF/lib) which
         * are excluded from an absolute ordering in the main web module descriptor.</p>
         * 
         * <p>Annotations are scanned from the excluded region.  However, the default
         * queries do not obtain results from the partial region.  To obtain results
         * from the partial region, a scan policy selector must be provided.  For
         * example:
         * {@link com.ibm.wsspi.anno.targets.AnnotationTargets_Targets#getAnnotatedClasses(int)}.</p> 
         */
        EXCLUDED(0x04),
        /**
         * <p>Policy for regions outside of the core region of the target scan space.
         * For all module type scans (EJB and CLIENT for JAR files, WEB for WAR files),
         * this includes all parts of the scan space which is external to the target
         * module.  For most modules, this includes the module MANIFEST class path
         * elements, JAR files from the application library, JAR files from shared
         * libraries, and any elements of the module external references class loader.</p>
         * 
         * <p>The external region has two distinguishing features which differentiate
         * it from the seed, partial, and excluded regions:</p>
         * 
         * <ul><li>The external region is only scanned to complete class information
         *         for classes from the other regions.</li>
         *     <li>No annotations are recorded for classes scanned from the external
         *         region.</li>
         */
        EXTERNAL(0x08);

        private ScanPolicy(int value) {
            this.value = value;
        }

        protected int value;

        /**
         * <p>Integer value for the scan policy.  This is intended to
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
         * selector.  The selector is accepted if it contains the
         * bit value of the scan policy.</p>
         * 
         * @param policies The bitwise OR of scan policy values to test.
         * @return True if the bit value of this scan policy is set in the policies.
         *         Otherwise, false.
         *         
         * {@link #getValue()}
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
         * @return True if exactly one policy bit value is set.  Otherwise,
         *         false.
         *         
         * {@link #getValue()}
         */
        public static boolean isSingular(int policies) {
            return ((policies == SEED.getValue()) ||
                    (policies == PARTIAL.getValue()) ||
                    (policies == EXCLUDED.getValue()) ||
                    (policies == EXTERNAL.getValue()));
        }
        
        /**
         * <p>Answer the singular policy which is represented by a
         * scan policy selector.  Answer null if the scan policy selector
         * does not match exactly one scan policy.</p>
         * 
         * @param policies A bitwise OR of scan value policies.
         * @return The single scan policy which matches the scan policy
         *         selector.  Null if the scan policy selector does not
         *         exactly one scan policy.
         *         
         * {@link #getValue()}
         */
        public static ScanPolicy asSingular(int policies) {
            return ( (policies == SEED.getValue())     ? SEED :
                     (policies == PARTIAL.getValue())  ? PARTIAL : 
                     (policies == EXCLUDED.getValue()) ? EXCLUDED :
                     (policies == EXTERNAL.getValue()) ? EXTERNAL : null );            
        }
    }

    /**
     * <p>Main API to add new class sources. Note that the added class source need not have
     * the same factory as the aggregate class source. Add the class source with the SEED
     * scan policy.</p>
     * 
     * @param classSource The class source to add to this aggregate.
     * 
     * @param scanPolicy The policy to apply to the class source.
     */
    void addClassSource(ClassSource classSource);

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
     * @return The entire list of class sources of this aggregate.</p>
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

    // Class source naming support ...

    String getCanonicalName(String classSourceName);

    Map<String, String> getCanonicalNames();

    // Scanning ...

    /**
     * <p>Scan the classes of the class source using a supplied streamer.</p>
     * 
     * @param streamer A selection and processing helper for the scan operation.
     */
    void scanClasses(ClassSource_Streamer streamer);

    // Lookup statistics ...

    long getTotalLookups();

    long getRepeatLookups();

    Map<String, Integer> getLookupCounts();

    Boolean getGlobalResult(String resourceName);

    Set<String> getFailedLookups(ClassSource classSource);

    Map<String, ? extends ClassSource> getFirstSuccesses();

    ClassSource getFirstSuccess(String resourceName);
}
