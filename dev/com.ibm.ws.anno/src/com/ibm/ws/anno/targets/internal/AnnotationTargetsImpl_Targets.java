/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corporation 2011, 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */

package com.ibm.ws.anno.targets.internal;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectweb.asm.Opcodes;

import com.ibm.ws.anno.service.internal.AnnotationServiceImpl_Logging;
import com.ibm.ws.anno.targets.cache.TargetCache_Options;
import com.ibm.ws.anno.targets.cache.internal.TargetCacheImpl_DataApp;
import com.ibm.ws.anno.targets.cache.internal.TargetCacheImpl_DataApps;
import com.ibm.ws.anno.targets.cache.internal.TargetCacheImpl_DataMod;
import com.ibm.ws.anno.targets.delta.internal.TargetsDeltaImpl;
import com.ibm.ws.anno.util.internal.UtilImpl_BidirectionalMap;
import com.ibm.ws.anno.util.internal.UtilImpl_EmptyBidirectionalMap;
import com.ibm.ws.anno.util.internal.UtilImpl_EmptyInternMap;
import com.ibm.ws.anno.util.internal.UtilImpl_IdentityStringSet;
import com.ibm.ws.anno.util.internal.UtilImpl_InternMap;
import com.ibm.ws.anno.util.internal.UtilImpl_NonInternSet;
import com.ibm.ws.anno.util.internal.UtilImpl_Utils;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Exception;
import com.ibm.wsspi.anno.targets.AnnotationTargets_OpCodes;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets;
import com.ibm.wsspi.anno.util.Util_BidirectionalMap;
import com.ibm.wsspi.anno.util.Util_InternMap;
import com.ibm.wsspi.anno.util.Util_InternMap.ValueType;

/*
 * Summary of scan policies:
 *
 * A "scan space" is the space which may be scanned for class information
 * for a module.
 *
 * "Complete" regions are associated with a descriptor which
 * is set as metadata complete.  "Incomplete" regions are associated
 * with a descriptor which is not metadata complete.  "Excluded" regions
 * are associated with web modules which use an absolute ordering element,
 * and includes jar files omitted from the absolute ordering element.
 *
 * Classes within a module are located according to the module type:
 * A CLIENT or EJBJAR provides classes directly as elements of the module
 * archive.  RAR files provide classes directly from the module archive
 * and provide classes indirectly through nested JAR files.  WAR files
 * provide classes directly (but restricted to the WEB-INF/classes
 * directory) and indirectly through nested JARs, restricted to the
 * WEB-INF/lib directory.
 *
 * Classes outside of a module include four categories of classes:
 * Classes available in archives linked through the manifest class path;
 * Classes available in application library archives; Classes available
 * in shared libraries of the application; Classes available in the
 * external references class loader of the application.  (Java classes
 * are available through the external references class loader.)
 *
 * Scan results are recorded independently for the several scan regions
 * of a module:
 *
 * ClassSourceAggregate.ScanPolicy.SEED(0x01)
 *
 * Policy for incomplete regions of the target scan space.  Annotations are
 * scanned from the seed region.  Default queries obtain results from seed data.
 *
 * ClassSourceAggregate.ScanPolicy.PARTIAL(0x02)
 *
 * Policy for complete but not excluded regions of the scan space.  Annotations are
 * scanned from the partial region.  Default queries do not obtain results from
 * the partial region.
 *
 * ClassSourceAggregate.ScanPolicy.EXCLUDED(0x04)
 *
 * Policy for excluded regions of the scan space.  Annotations are scanned
 * from the excluded region.  Default queries do not obtain results from the
 * excluded region.
 *
 * Annotations are scanned from the excluded region.  However, the default
 * queries do not obtain results from the partial region.  To obtain results
 * from the partial region, a scan policy selector must be provided.
 *
 * ClassSourceAggregate.ScanPolicy.EXTERNAL(0x08)
 *
 * Policy for regions outside of the core region of the target scan space.
 * For all module type scans (EJB and CLIENT for JAR files, WEB for WAR files),
 * this includes all parts of the scan space which is external to the target
 * module.  For most modules, this includes the module MANIFEST class path
 * elements, JAR files from the application library, JAR files from shared
 * libraries, and any elements of the module external references class loader.
 *
 * The external region is only scanned to complete class information
 * for classes from the other regions.  No annotations are recorded for
 * classes scanned from the external region.
 *
 * For standard JavaEE module types, several cases result:
 *
 * 1) Complete EJBJAR or CLIENT JAR
 *    [ JAR(Partial), CLASSLOADER(External) ]
 * 2) Incomplete EJBJAR or CLIENT JAR
 *    [ JAR(SEED), CLASSLOADER(External) ]
 *
 * 3) Complete RAR.
 *    [ RAR(Partial), Nested Jar(Partial), ..., CLASSLOADER(External) ]
 * 4) Incomplete RAR.
 *    [ RAR(Seed), Nested Jar(Seed), ..., CLASSLOADER(External) ]
 *
 * 5) Wholly complete WAR.
 *    [ WEB-INF/classes(Partial), WEB-INF/LIB/jar(Partial), ..., CLASSLOADER(External) ]
 * 6) Wholly incomplete WAR.
 *    [ WEB-INF/classes(Seed), WEB-INF/LIB/jar(Seed), ..., CLASSLOADER(External) ]
 * 7) Partially complete WAR with no excluded regions
 *    [ WEB-INF/classes(Seed), WEB-INF/LIB/jar(Seed|Partial), ..., CLASSLOADER(External) ]
 * 8) Partially complete WAR with excluded regions.
 *    [ WEB-INF/classes(Seed), WEB-INF/LIB/jar(Seed|Partial|Excluded), ..., CLASSLOADER(External) ]
 *
 * <p>For updates, these patterns are distinguished:</p>
 *
 * 1) JAR + CLASSLOADER: EJBJAR and CLIENT cases.
 *
 * 2) DIR + CLASSLOADER: WAR + EMPTY LIB and RAR + EMPTY NESTED cases.
 *
 * 3) DIR + JARS + CLASSLOADER: WAR + LIB and RAR + NESTED cases.
 */

public class AnnotationTargetsImpl_Targets implements AnnotationTargets_Targets {
    protected static final Logger logger = AnnotationServiceImpl_Logging.ANNO_LOGGER;
    protected static final Logger stateLogger = AnnotationServiceImpl_Logging.ANNO_STATE_LOGGER;

    public static final String CLASS_NAME = AnnotationTargetsImpl_Targets.class.getSimpleName(); 
            
    protected final String hashText;

    public String getHashText() {
        return hashText;
    }

    //

    protected AnnotationTargetsImpl_Targets(
        AnnotationTargetsImpl_Factory factory,
        TargetCacheImpl_DataApps annoCache,
        UtilImpl_InternMap classNameInternMap,
        UtilImpl_InternMap fieldNameInternMap,
        UtilImpl_InternMap methodSignatureInternMap) {

        String methodName = "<init>";

        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());

        this.factory = factory;

        this.annoCache = annoCache;

        this.classNameInternMap = classNameInternMap;
        this.fieldNameInternMap = fieldNameInternMap;
        this.methodSignatureInternMap = methodSignatureInternMap;

        //

        this.emptyPackageAnnotations =
            createEmptyBidiMap(ValueType.VT_CLASS_NAME, "annotated package",
                               ValueType.VT_CLASS_NAME, "package annotation");
        this.emptyClassAnnotations =
            createEmptyBidiMap(ValueType.VT_CLASS_NAME, "annotated class",
                               ValueType.VT_CLASS_NAME, "class annotation");
        this.emptyFieldAnnotations =
            createEmptyBidiMap(ValueType.VT_FIELD_NAME, "annotated field",
                               ValueType.VT_CLASS_NAME, "field annotation");
        this.emptyMethodAnnotations =
            createEmptyBidiMap(ValueType.VT_OTHER, "annotated method",
                               ValueType.VT_CLASS_NAME, "method annotation");

        //

        this.limitedScan = false;
        this.specificScan = false;

        this.rootClassSource = null;
        this.overallScanner = null;

        this.seedData = null;
        this.partialData = null;
        this.excludedData = null;

        this.externalData = null;
        this.classData = null;

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ]", this.hashText);
        }
    }

    // Factory, and related methods ...

    protected final AnnotationTargetsImpl_Factory factory;

    @Override
    public AnnotationTargetsImpl_Factory getFactory() {
        return factory;
    }

    protected UtilImpl_EmptyInternMap createEmptyInternMap(Util_InternMap.ValueType valueType, String mapName) {
        return getFactory().getUtilFactory().createEmptyInternMap(valueType, mapName);
    }

    protected UtilImpl_BidirectionalMap createBidiMap(String holderTag, UtilImpl_InternMap holderInternMap,
                                                      String heldTag, UtilImpl_InternMap heldInternMap) {
        return getFactory().getUtilFactory().createBidirectionalMap(holderTag, holderInternMap, heldTag, heldInternMap);
    }

    protected UtilImpl_EmptyBidirectionalMap createEmptyBidiMap(ValueType holderType, String holderTag,
                                                                ValueType heldType, String heldTag) {
        return getFactory().getUtilFactory().createEmptyBidirectionalMap(holderType, holderTag, heldType, heldTag);
    }

    // Cache ...

    private final TargetCacheImpl_DataApps annoCache;

    public TargetCacheImpl_DataApps getAnnoCache() {
        return annoCache;
    }

    // Class, field, and method interning ...

    protected final UtilImpl_InternMap classNameInternMap;

    public UtilImpl_InternMap getClassNameInternMap() {
        return classNameInternMap;
    }

    public String internClassName(String className, boolean doForce) {
        return getClassNameInternMap().intern(className, doForce);
    }

    public Set<String> uninternClassNames(Set<String> classNames) {
        if ( (classNames == null) || classNames.isEmpty() ) {
            return Collections.emptySet();
        } else {
            return new UtilImpl_NonInternSet( getClassNameInternMap(), classNames );
        }
    }

    protected final UtilImpl_InternMap fieldNameInternMap;

    public UtilImpl_InternMap getFieldNameInternMap() {
        return fieldNameInternMap;
    }

    public Set<String> uninternFieldNames(Set<String> fieldNames) {
        if ( (fieldNames == null) || fieldNames.isEmpty() ) {
            return Collections.emptySet();
        } else {
            return new UtilImpl_NonInternSet( getFieldNameInternMap(), fieldNames );
        }
    }

    protected final UtilImpl_InternMap methodSignatureInternMap;

    public UtilImpl_InternMap getMethodSignatureInternMap() {
        return methodSignatureInternMap;
    }

    //

    /**
     * <p>Set to scan a fully realized class source.</p>
     *
     * <p>Use of {@link #scan(Future, Future)} is preferred: The step of
     * obtaining the external class source should be deferred until absolutely
     * necessary.</p>
     *
     * @param useRootClassSource The class source which is to be scanned.
     */
    @Override
    public void scan(ClassSource_Aggregate useRootClassSource) {
        setRootClassSource(useRootClassSource); // Scanning is now done on demand.
    }

    //

    /**
     * <p>Scan specific classes for annotations.</p>
     *
     * <p>Scan only for annotations: Do not complete class reference information.</p>
     *
     * @param useRootClassSource The root class source which is to be scanned.
     * @param specificClassNames The names of the classes which are to be scanned.
     */
    @Override
    public void scan(ClassSource_Aggregate useRootClassSource, Set<String> specificClassNames)
        throws AnnotationTargets_Exception {
        scan(useRootClassSource, specificClassNames, TargetsVisitorClassImpl.SELECT_ALL_ANNOTATIONS);
        // throws AnnotationTargets_Exception {
    }

    //

    /**
     * <p>Do a limited scan.  The cache is not enabled, and no external class source is available.</p>
     *
     * @param useRootClassSource The class source which is to be scanned.
     */
    @Override
    public void scanLimited(ClassSource_Aggregate useRootClassSource) {
        String methodName = "scanLimited";

        if ( limitedScan ) {
            if (logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER / RETURN (already scanned)", getHashText());
            }
            return;
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "ENTER [ {0} ]", getHashText());
        }

        limitedScan = true;

        TargetsScannerLimitedImpl limitedScanner = new TargetsScannerLimitedImpl(this, useRootClassSource);

        limitedScanner.scanContainer();

        putLimitedResults(limitedScanner);

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "RETURN [ {0} ]", getHashText());
        }
    }

    protected void putLimitedResults(TargetsScannerLimitedImpl scanner) {
        seedData = scanner.getTargetsData();
        partialData = null;
        excludedData = null;
        externalData = null;

        classData = null;
    }

    //

    /**
     * <p>Scan specific classes for annotations.</p>
     *
     * <p>Scan only for annotations: Do not complete class reference information.</p>
     *
     * @param useRootClassSource The root class source which is to be scanned.
     * @param specificClassNames The names of the classes which are to be scanned.
     * @param specificAnnotationClassNames The names of the annotations which of are of interest.
     */
    @Override
    public void scan(ClassSource_Aggregate useRootClassSource,
                     Set<String> specificClassNames,
                     Set<String> specificAnnotationClassNames)
                    throws AnnotationTargets_Exception {

        String methodName = "scan";

        if ( specificScan ) {
            if (logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER / RETURN (already scanned)", getHashText());
            }
            return;
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "ENTER [ {0} ]", getHashText());
        }

        specificScan = true;

        TargetsScannerSpecificImpl specificScanner = new TargetsScannerSpecificImpl(this, useRootClassSource);

        specificScanner.scan(specificClassNames, specificAnnotationClassNames);
        // throws AnnotationTargets_Exception

        putSpecificResults(specificScanner);

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "RETURN [ {0} ]", getHashText());
        }
    }

    protected void putSpecificResults(TargetsScannerBaseImpl scanner) {
        seedData = scanner.getSeedBucket();
        partialData = scanner.getPartialBucket();
        excludedData = scanner.getExcludedBucket();
        externalData = scanner.getExternalBucket();

        classData = scanner.getClassTable();
    }

    // Policy data fan-outs ...

    // Phase 1: Internal data

    protected ClassSource_Aggregate rootClassSource;
    protected String appName;
    protected String modName;

    protected void setRootClassSource(ClassSource_Aggregate rootClassSource) {
        this.rootClassSource = rootClassSource;

        this.appName = rootClassSource.getApplicationName();
        this.modName = rootClassSource.getModuleName();
    }

    public String getAppName() {
        return appName;
    }
    
    public String getModName() {
        return modName;
    }

    protected ClassSource_Aggregate consumeRootClassSource() {
        ClassSource_Aggregate useRootClassSource = rootClassSource;

        rootClassSource = null;

        return useRootClassSource;
    }

    protected boolean limitedScan;
    protected boolean specificScan;

    protected TargetsScannerOverallImpl overallScanner;

    protected TargetsTableImpl seedData;
    protected TargetsTableImpl partialData;
    protected TargetsTableImpl excludedData;

    // Phase 2: External data

    protected TargetsTableImpl externalData;
    protected TargetsTableClassesMultiImpl classData;

    //

    public TargetsTableImpl getSeedData() {
        ensureInternalResults();

        return seedData;
    }

    public TargetsTableImpl getPartialData() {
        ensureInternalResults();

        return partialData;
    }

    public TargetsTableImpl getExcludedData() {
        ensureInternalResults();

        return excludedData;
    }

    public TargetsTableImpl getExternalData() {
        ensureExternalResults();

        return externalData;
    }

    // Careful!
    //
    // Three modes must be handled:
    //
    // Overall, Limited, and Specific
    //
    // When an overall scan is performed, there are three phases,
    // with the scanner held temporarily between the internal and external
    // scan phases:
    //
    // PreScan:          (rootClassSource != null) (overallScanner == null) (externalData == null)
    // PostInternalScan: (rootClassSource != null) (overallScanner != null) (externalData == null)
    // PostExternalScan: (rootClassSource == null) (overallScanner == null) (externalData != null)
    //
    // When a limited scan is performed, the scanner is not held temporarily,
    // and there are only two phases:
    //
    // PreScan:          (seedData == null)
    // PostScan:         (seedData != null)
    //
    // When a specific scan is performed, the scanner is not held temporarily,
    // and there are only two phases:
    //
    // PreScan:          (seedData == null)
    // PostScan:         (seedData != null)

    public boolean hasInternalData() {
        if ( limitedScan || specificScan ) {
            return ( seedData != null );
        } else {
            return ( (externalData != null) || (overallScanner != null ) );
        }
    }

    //

    /**
     * <p>Answer the targets data for the specified policy. The result
     * is null when no sources were scanned having the specified policy.</p>
     *
     * @param policy The policy for which to obtain targets data.
     *
     * @return Targets data for the policy.
     */
    protected TargetsTableImpl getPolicyData(ScanPolicy policy) {
        if ( policy == ScanPolicy.SEED ) {
            return getSeedData();

        } else if ( policy == ScanPolicy.PARTIAL ) {
            return getPartialData();

        } else if ( policy == ScanPolicy.EXCLUDED ) {
            return getExcludedData();

        } else if ( policy == ScanPolicy.EXTERNAL ) {
            return getExternalData();

        } else {
            throw new IllegalArgumentException("Policy [ " + policy + " ]");
        }
    }

    /**
     * <p>Obtain the class table, but only ensure that it has the internal class
     * source class information.</p>
     *
     * @return The class table.
     */
    protected TargetsTableClassesMultiImpl getInternalClassTable() {
        ensureInternalResults();

        return classData;
    }

    protected Set<String> getInternalClassNames(String classSourceName) {
        return getInternalClassTable().getClassNames(classSourceName);
    }

    public TargetsTableClassesMultiImpl getClassTable() {
        ensureExternalResults();

        return classData;
    }

    /**
     * <p>Answer the names of the classes scanned from the named class source.
     * Answer an empty set if the class source is not present, or was not scanned.</p>
     *
     * <p>A class source will not be scanned if it failed to open, or, if a
     * specific scan was performed and no classes were left to scan when the
     * class source was reached.
     *
     * @param classSourceName
     *            The name of the class source for which to retrieve scanned class names.
     *
     * @return The names of the classes scanned from the class source.
     */
    public Set<String> getClassNames(String classSourceName) {
        // The external scan need not be performed if the class source is
        // one of the internal class sources.

        TargetsTableClassesMultiImpl internalClassTable = getInternalClassTable();
        if ( internalClassTable.getClassSourceNames().contains(classSourceName) ) {
            return internalClassTable.getClassNames(classSourceName);
        }

        TargetsTableClassesMultiImpl externalClassTable = getClassTable();
        return externalClassTable.getClassNames(classSourceName);
    }

    //

    protected void ensureInternalResults() {
        String methodName = "ensureInternalResults";

        if ( hasInternalData() ) {
            return;
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "ENTER [ {0} ]", getHashText());
        }

        ClassSource_Aggregate useRootClassSource = consumeRootClassSource();
        String useAppName = getAppName();
        String useModName = getModName();

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "App [ {0} ] Mod [ {1} ]", new Object[] { useAppName, useModName });
        }

        TargetCacheImpl_DataApp appData = getAnnoCache().getAppForcing(useAppName);
        TargetCacheImpl_DataMod modData = appData.getModForcing(useModName);

        TargetsScannerOverallImpl useOverallScanner =
            new TargetsScannerOverallImpl(this, useRootClassSource, modData);

        useOverallScanner.validInternal();

        putInternalResults(useOverallScanner);

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "RETURN [ {0} ]", getHashText());
        }
    }

    protected void putInternalResults(TargetsScannerOverallImpl useOverallScanner) {
        overallScanner = useOverallScanner;

        seedData = useOverallScanner.getSeedBucket();
        partialData = useOverallScanner.getPartialBucket();
        excludedData = useOverallScanner.getExcludedBucket();

        // The class data is incomplete, but, is useful for queries which obtain
        // annotation results and which do not need superclass or interface information.
        classData = useOverallScanner.getClassTable();
    }

    private static final int NS_IN_MS = 1000 * 1000;

    protected void ensureExternalResults() {
        String methodName = "ensureExternalResults";

        if ( externalData != null ) {
            return;
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "ENTER[ {0} ]", getHashText());
        }

        ensureInternalResults();

        TargetsScannerOverallImpl useOverallScanner = overallScanner;
        overallScanner = null;

        useOverallScanner.validExternal();

        externalData = useOverallScanner.getExternalBucket();

        long cacheReadTime = useOverallScanner.getCacheReadTime();
        long cacheWriteTime = useOverallScanner.getCacheWriteTime();
        long containerReadTime = useOverallScanner.getContainerReadTime();
        long containerWriteTime = useOverallScanner.getContainerWriteTime();

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "Cache Read [ {0} (ms)",
                	Long.valueOf(cacheReadTime / NS_IN_MS));
            logger.logp(Level.FINER, CLASS_NAME, methodName, "Cache Write [ {0} (ms)",
                	Long.valueOf(cacheWriteTime / NS_IN_MS));
            logger.logp(Level.FINER, CLASS_NAME, methodName, "Container Read [ {0} (ms)",
                	Long.valueOf(containerReadTime / NS_IN_MS));
            logger.logp(Level.FINER, CLASS_NAME, methodName, "Container Write [ {0} (ms)",
                	Long.valueOf(containerWriteTime / NS_IN_MS));
        }

        ClassSource_Aggregate rootClassSource = useOverallScanner.getRootClassSource();

        rootClassSource.addCacheReadTime(cacheReadTime, "Module Reads");
        rootClassSource.addCacheReadTime(containerReadTime, "Container Reads");

        rootClassSource.addCacheWriteTime(cacheWriteTime, "Module Writes");
        rootClassSource.addCacheWriteTime(containerWriteTime, "Container Writes");

        // The class table was already set during internal processing.

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "RETURN [ {0} ]", getHashText());
        }
    }

    //

    @Override
    public Set<String> getSeedClassNames() {
        TargetsTableImpl useSeedData = getSeedData();

        if ( useSeedData == null ) {
            return Collections.emptySet();
        } else {
            return useSeedData.getClassNames();
        }
    }

    @Override
    public boolean isSeedClassName(String className) {
        TargetsTableImpl useSeedData = getSeedData();

        if ( useSeedData == null ) {
            return false;
        } else {
            return useSeedData.containsClassName(className);
        }
    }

    @Override
    public Set<String> getPartialClassNames() {
        TargetsTableImpl usePartialData = getPartialData();

        if ( usePartialData == null ) {
            return Collections.emptySet();
        } else {
            return usePartialData.getClassNames();
        }
    }

    @Override
    public boolean isPartialClassName(String className) {
        TargetsTableImpl usePartialData = getPartialData();

        if ( usePartialData == null ) {
            return false;
        } else {
            return usePartialData.containsClassName(className);
        }
    }

    @Override
    public Set<String> getExcludedClassNames() {
        TargetsTableImpl useExcludedData = getExcludedData();

        if ( useExcludedData == null ) {
            return Collections.emptySet();
        } else {
            return useExcludedData.getClassNames();
        }
    }

    @Override
    public boolean isExcludedClassName(String className) {
        TargetsTableImpl useExcludedData = getExcludedData();

        if ( useExcludedData == null ) {
            return false;
        } else {
            return useExcludedData.containsClassName(className);
        }
    }

    @Override
    public Set<String> getExternalClassNames() {
        TargetsTableImpl useExternalData = getExternalData();

        if ( useExternalData == null ) {
            return Collections.emptySet();
        } else {
            return useExternalData.getClassNames();
        }
    }

    @Override
    public boolean isExternalClassName(String className) {
        TargetsTableImpl useExternalData = getExternalData();

        if ( useExternalData == null ) {
            return false;
        } else {
            return useExternalData.containsClassName(className);
        }
    }

    /**
     * <p>Answer the union of class names obtained from class sources having
     * specified policies.</p>
     *
     * @param int Specification of which scan policies to select, as the bit-or
     *        of the policy values.
     *
     * @return The union of class names obtained for the specified policies.
     */
    @Override
    public Set<String> getClassNames(int scanPolicies) {
        return uninternClassNames( i_getClassNames(scanPolicies) );
    }

    public Set<String> i_getClassNames(int scanPolicies) {
        int nonEmptyCount = 0;
        int totalCount = 0;

        Set<String> i_seed = null;
        if ( ScanPolicy.SEED.accept(scanPolicies) ) {
            TargetsTableImpl useSeedData = getSeedData();
            if ( useSeedData != null ) {
                i_seed = useSeedData.i_getClassNames();
                if ( i_seed.isEmpty() ) {
                    i_seed = null;
                } else {
                    nonEmptyCount++;
                    totalCount += i_seed.size();
                }
            }
        }

        Set<String> i_partial = null;
        if ( ScanPolicy.PARTIAL.accept(scanPolicies) ) {
            TargetsTableImpl usePartialData = getPartialData();
            if ( usePartialData != null ) {
                i_partial = usePartialData.i_getClassNames();
                if ( i_partial.isEmpty() ) {
                    i_partial = null;
                } else {
                    nonEmptyCount++;
                    totalCount += i_partial.size();
                }
            }
        }

        Set<String> i_excluded = null;
        if ( ScanPolicy.PARTIAL.accept(scanPolicies) ) {
            TargetsTableImpl useExcludedData = getExcludedData();
            if ( useExcludedData != null ) {
                i_excluded = useExcludedData.i_getClassNames();
                if ( i_excluded.isEmpty() ) {
                    i_excluded = null;
                } else {
                    nonEmptyCount++;
                    totalCount += i_excluded.size();
                }
            }
        }

        Set<String> i_external = null;
        if ( ScanPolicy.EXTERNAL.accept(scanPolicies) ) {
            TargetsTableImpl useExternalData = getExternalData();
            if ( useExternalData != null ) {
                i_external = useExternalData.i_getClassNames();
                if ( i_external.isEmpty() ) {
                    i_external = null;
                } else {
                    nonEmptyCount++;
                    totalCount += i_external.size();
                }
            }
        }

        if ( nonEmptyCount == 0 ) {
            return Collections.emptySet();

        } else if ( nonEmptyCount == 1 ) {
            if ( i_seed != null ) {
                return i_seed;
            } else if ( i_partial != null ) {
                return i_partial;
            } else if ( i_excluded != null ) {
                return i_excluded;
            } else {
                return i_external;
            }

        } else {
            UtilImpl_IdentityStringSet i_result = new UtilImpl_IdentityStringSet(totalCount);

            if ( i_seed != null ) {
                i_result.addAll(i_seed);
            }
            if ( i_partial != null ) {
                i_result.addAll(i_partial);
            }
            if ( i_excluded != null ) {
                i_result.addAll(i_excluded);
            }
            if ( i_external != null ) {
                i_result.addAll(i_external);
            }

            i_result.trimStorage();

            return i_result;
        }
    }

    // Seed data specific annotation lookups.
    //
    // Most lookups are for annotations in seed data.

    @Override
    public Set<String> getAnnotatedPackages() {
        TargetsTableImpl useSeedData = getSeedData();

        if ( useSeedData == null ) {
            return Collections.emptySet();
        } else {
            return useSeedData.getAnnotatedTargets(AnnotationCategory.PACKAGE);
        }
    }

    @Override
    public Set<String> getAnnotatedPackages(String annotationName) {
        TargetsTableImpl useSeedData = getSeedData();

        if ( useSeedData == null ) {
            return Collections.emptySet();
        } else {
            return useSeedData.getAnnotatedTargets(AnnotationCategory.PACKAGE, annotationName);
        }
    }

    @Override
    public Set<String> getPackageAnnotations() {
        TargetsTableImpl useSeedData = getSeedData();

        if ( useSeedData == null ) {
            return Collections.emptySet();
        } else {
            return useSeedData.getAnnotations(AnnotationCategory.PACKAGE);
        }
    }

    @Override
    public Set<String> getPackageAnnotations(String packageName) {
        TargetsTableImpl useSeedData = getSeedData();

        if ( useSeedData == null ) {
            return Collections.emptySet();
        } else {
            return useSeedData.getAnnotations(AnnotationCategory.PACKAGE, packageName);
        }
    }

    @Override
    public Set<String> getAnnotatedClasses() {
        TargetsTableImpl useSeedData = getSeedData();

        if ( useSeedData == null ) {
            return Collections.emptySet();
        } else {
            return useSeedData.getAnnotatedTargets(AnnotationCategory.CLASS);
        }
    }

    @Override
    public Set<String> getAnnotatedClasses(String annotationName) {
        String methodName = "getAnnotatedClasses";
        
        TargetsTableImpl useSeedData = getSeedData();

        if ( useSeedData == null ) {
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "ENTER [ {0} ] RETURN [ 0 ]", annotationName);
            }
            return Collections.emptySet();

        } else {
            Set<String> result = useSeedData.getAnnotatedTargets(AnnotationCategory.CLASS, annotationName);

            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "ENTER [ {0} ] RETURN [ {1} ]",
                        new Object[] { annotationName, Integer.valueOf(result.size()) });
                for ( String annotatedClassName : result ) {
                    logger.logp(Level.FINER, CLASS_NAME, methodName, "  [ {0} ]", annotatedClassName);
                }
            }

            return result;
        }
    }

    @Override
    public Set<String> getAnnotatedClasses(String classSourceName, String annotationName) {
        Set<String> annotatedClassNames = getAnnotatedClasses(annotationName);
        Set<String> classSourceClassNames = getInternalClassNames(classSourceName);

        return ( UtilImpl_Utils.restrict(annotatedClassNames, classSourceClassNames) );
    }

    @Override
    public Set<String> getClassAnnotations() {
        TargetsTableImpl useSeedData = getSeedData();

        if ( useSeedData == null ) {
            return Collections.emptySet();
        } else {
            return useSeedData.getAnnotations(AnnotationCategory.CLASS);
        }
    }

    @Override
    public Set<String> getClassAnnotations(String className) {
        TargetsTableImpl useSeedData = getSeedData();

        if ( useSeedData == null ) {
            return Collections.emptySet();
        } else {
            return useSeedData.getAnnotations(AnnotationCategory.CLASS, className);
        }
    }

    public Set<String> getClassesWithFieldAnnotations() {
        TargetsTableImpl useSeedData = getSeedData();

        if ( useSeedData == null ) {
            return Collections.emptySet();
        } else {
            return useSeedData.getAnnotatedTargets(AnnotationCategory.FIELD);
        }
    }

    @Override
    public Set<String> getClassesWithFieldAnnotation(String annotationName) {
        TargetsTableImpl useSeedData = getSeedData();

        if ( useSeedData == null ) {
            return Collections.emptySet();
        } else {
            return useSeedData.getAnnotatedTargets(AnnotationCategory.FIELD, annotationName);
        }
    }

    @Override
    public Set<String> getFieldAnnotations() {
        TargetsTableImpl useSeedData = getSeedData();

        if ( useSeedData == null ) {
            return Collections.emptySet();
        } else {
            return useSeedData.getAnnotations(AnnotationCategory.FIELD);
        }
    }

    @Override
    public Set<String> getFieldAnnotations(String className) {
        TargetsTableImpl useSeedData = getSeedData();

        if ( useSeedData == null ) {
            return Collections.emptySet();
        } else {
            return useSeedData.getAnnotations(AnnotationCategory.FIELD, className);
        }
    }

    public Set<String> getClassesWithMethodAnnotations() {
        TargetsTableImpl useSeedData = getSeedData();

        if ( useSeedData == null ) {
            return Collections.emptySet();
        } else {
            return useSeedData.getAnnotatedTargets(AnnotationCategory.METHOD);
        }
    }

    @Override
    public Set<String> getClassesWithMethodAnnotation(String annotationName) {
        TargetsTableImpl useSeedData = getSeedData();

        if ( useSeedData == null ) {
            return Collections.emptySet();
        } else {
            return useSeedData.getAnnotatedTargets(AnnotationCategory.METHOD, annotationName);
        }
    }

    @Override
    public Set<String> getMethodAnnotations() {
        TargetsTableImpl useSeedData = getSeedData();

        if ( useSeedData == null ) {
            return Collections.emptySet();
        } else {
            return useSeedData.getAnnotations(AnnotationCategory.METHOD);
        }
    }

    @Override
    public Set<String> getMethodAnnotations(String className) {
        TargetsTableImpl useSeedData = getSeedData();

        if ( useSeedData == null ) {
            return Collections.emptySet();
        } else {
            return useSeedData.getAnnotations(AnnotationCategory.METHOD, className);
        }
    }

    // Policy based annotation lookups ...
    //
    // Answers are obtained from the results matching the
    // scan policies, which are specified as a bit-wise or
    // of scan policy values.

    @Override
    public Set<String> getAnnotatedPackages(int scanPolicies) {
        return selectAnnotatedTargets(scanPolicies, AnnotationCategory.PACKAGE);
    }

    @Override
    public Set<String> getAnnotatedPackages(String annotationName, int scanPolicies) {
        return selectAnnotatedTargets(annotationName, scanPolicies, AnnotationCategory.PACKAGE);
    }

    @Override
    public Set<String> getPackageAnnotations(int scanPolicies) {
        return selectAnnotations(scanPolicies, AnnotationCategory.PACKAGE);
    }

    @Override
    public Set<String> getPackageAnnotations(String packageName, int scanPolicies) {
        return selectAnnotations(packageName, scanPolicies, AnnotationCategory.PACKAGE);
    }

    //

    @Override
    public Set<String> getAnnotatedClasses(int scanPolicies) {
        return selectAnnotatedTargets(scanPolicies, AnnotationCategory.CLASS);
    }

    @Override
    public Set<String> getAnnotatedClasses(String classSourceName, String annotationName, int scanPolicies) {
        Set<String> annotatedClassNames = getAnnotatedClasses(annotationName, scanPolicies);
        Set<String> classSourceClassNames = getClassNames(classSourceName);

        return (UtilImpl_Utils.restrict(annotatedClassNames, classSourceClassNames));
    }

    @Override
    public Set<String> getAnnotatedClasses(String annotationName, int scanPolicies) {
        String methodName = "getAnnotatedClasses";
        
        Set<String> annotatedClassNames = selectAnnotatedTargets(annotationName, scanPolicies, AnnotationCategory.CLASS);

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "Annotated classes [ {0} ] [ {1} ]: [ {2} ]",
                new Object[] { annotationName,
                               Integer.valueOf(scanPolicies),
                               Integer.valueOf(annotatedClassNames.size()) });

            for ( String annotatedClassName : annotatedClassNames ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "  [ {0} ]", annotatedClassName );
            }
        }

        return annotatedClassNames;
    }

    @Override
    public Set<String> getClassAnnotations(int scanPolicies) {
        return selectAnnotations(scanPolicies, AnnotationCategory.CLASS);
    }

    @Override
    public Set<String> getClassAnnotations(String className, int scanPolicies) {
        return selectAnnotations(className, scanPolicies, AnnotationCategory.CLASS);
    }

    //

    @Override
    public Set<String> getClassesWithFieldAnnotations(int scanPolicies) {
        return selectAnnotatedTargets(scanPolicies, AnnotationCategory.FIELD);
    }

    @Override
    public Set<String> getClassesWithFieldAnnotation(String annotationName, int scanPolicies) {
        return selectAnnotatedTargets(annotationName, scanPolicies, AnnotationCategory.FIELD);
    }

    @Override
    public Set<String> getFieldAnnotations(int scanPolicies) {
        return selectAnnotations(scanPolicies, AnnotationCategory.FIELD);
    }

    @Override
    public Set<String> getFieldAnnotations(String className, int scanPolicies) {
        return selectAnnotations(className, scanPolicies, AnnotationCategory.FIELD);
    }

    //

    @Override
    public Set<String> getClassesWithMethodAnnotations(int scanPolicies) {
        return selectAnnotatedTargets(scanPolicies, AnnotationCategory.METHOD);
    }

    @Override
    public Set<String> getClassesWithMethodAnnotation(String annotationName, int scanPolicies) {
        return selectAnnotatedTargets(annotationName, scanPolicies, AnnotationCategory.METHOD);
    }

    @Override
    public Set<String> getMethodAnnotations(int scanPolicies) {
        return selectAnnotations(scanPolicies, AnnotationCategory.METHOD);
    }

    @Override
    public Set<String> getMethodAnnotations(String className, int scanPolicies) {
        return selectAnnotations(className, scanPolicies, AnnotationCategory.METHOD);
    }

    // Complex annotations accessors ...

    // The SEED data accessors are preserved as unit test entry points.
    // These must ensure the scan has been performed.

    public Util_BidirectionalMap i_getPackageAnnotationData() {
        return i_getAnnotationsMap(ScanPolicy.SEED, AnnotationCategory.PACKAGE);
    }

    public boolean i_packageHasAnnotation(String i_packageName, String i_annotationClassName) {
        return i_getPackageAnnotationData().holds(i_packageName, i_annotationClassName);
    }

    public Util_BidirectionalMap i_getClassAnnotationData() {
        return i_getAnnotationsMap(ScanPolicy.SEED, AnnotationCategory.CLASS);
    }

    public boolean i_classHasAnnotation(String i_className, String i_annotationClassName) {
        return i_getClassAnnotationData().holds(i_className, i_annotationClassName);
    }

    public Util_BidirectionalMap i_getFieldAnnotationData() {
        return i_getAnnotationsMap(ScanPolicy.SEED, AnnotationCategory.FIELD);
    }

    public boolean i_classHasFieldAnnotation(String i_className, String i_annotationClassName) {
        return i_getFieldAnnotationData().holds(i_className, i_annotationClassName);
    }

    public Util_BidirectionalMap i_getMethodAnnotationData() {
        return i_getAnnotationsMap(ScanPolicy.SEED, AnnotationCategory.METHOD);
    }

    public boolean i_classHasMethodAnnotation(String i_className, String i_annotationClassName) {
        return i_getMethodAnnotationData().holds(i_className, i_annotationClassName);
    }

    protected final UtilImpl_EmptyBidirectionalMap emptyPackageAnnotations;
    protected final UtilImpl_EmptyBidirectionalMap emptyClassAnnotations;
    protected final UtilImpl_EmptyBidirectionalMap emptyFieldAnnotations;
    protected final UtilImpl_EmptyBidirectionalMap emptyMethodAnnotations;

    protected Util_BidirectionalMap i_getAnnotationsMap(ScanPolicy policy, AnnotationCategory category) {
        TargetsTableImpl policyData = getPolicyData(policy);

        if ( policyData == null ) {
            if ( category == AnnotationCategory.PACKAGE ) {
                return emptyPackageAnnotations;
            } else if ( category == AnnotationCategory.CLASS ) {
                return emptyClassAnnotations;
            } else if ( category == AnnotationCategory.FIELD ) {
                return emptyFieldAnnotations;
            } else if ( category == AnnotationCategory.METHOD ) {
                return emptyMethodAnnotations;
            } else {
                throw new IllegalArgumentException("Unknown annotation category [ " + category + " ]");
            }

        } else {
            return policyData.i_getAnnotationData(category);
        }
    }

    protected Set<String> selectAnnotations(int scanPolicies, AnnotationCategory category) {
        return uninternClassNames( i_selectAnnotations(scanPolicies, category) );
    }

    protected Set<String> i_selectAnnotations(int scanPolicies, AnnotationCategory category) {
        int nonEmptyCount = 0;
        int totalCount = 0;

        Set<String> i_selectedSeed = null;
        if ( ScanPolicy.SEED.accept(scanPolicies) ) {
            TargetsTableImpl useSeedData = getSeedData();
            if ( useSeedData != null ) {
                i_selectedSeed = useSeedData.i_getAnnotations(category);
                if ( i_selectedSeed.isEmpty() ) {
                    i_selectedSeed = null;
                } else {
                    nonEmptyCount++;
                    totalCount += i_selectedSeed.size();
                }
            }
        }

        Set<String> i_selectedPartial = null;
        if ( ScanPolicy.PARTIAL.accept(scanPolicies) ) {
            TargetsTableImpl usePartialData = getPartialData();
            if ( usePartialData != null ) {
                i_selectedPartial = usePartialData.i_getAnnotations(category);
                if ( i_selectedPartial.isEmpty() ) {
                    i_selectedPartial = null;
                } else {
                    nonEmptyCount++;
                    totalCount += i_selectedPartial.size();
                }
            }
        }

        Set<String> i_selectedExcluded = null;
        if ( ScanPolicy.EXCLUDED.accept(scanPolicies) ) {
            TargetsTableImpl useExcludedData = getExcludedData();
            if ( useExcludedData != null ) {
                i_selectedExcluded = useExcludedData.i_getAnnotations(category);
                if ( i_selectedExcluded.isEmpty() ) {
                    i_selectedExcluded = null;
                } else {
                    nonEmptyCount++;
                    totalCount += i_selectedExcluded.size();
                }
            }
        }

        if ( nonEmptyCount == 0 ) {
            return Collections.emptySet();

        } else if ( nonEmptyCount == 1 ) {
            if ( i_selectedSeed != null ) {
                return i_selectedSeed;
            } else if ( i_selectedPartial != null ) {
                return i_selectedPartial;
            } else {
                return i_selectedExcluded ;
            }

        } else {
            // Handles both the case when all three are requested and
            // when just two are requested.  When just two are requested,
            // or when just two are non-empty, there is one extra call
            // to 'addAll'.

            UtilImpl_IdentityStringSet i_result = new UtilImpl_IdentityStringSet(totalCount);

            if ( i_selectedSeed != null ) {
                i_result.addAll(i_selectedSeed);
            }
            if ( i_selectedPartial != null ) {
                i_result.addAll(i_selectedPartial);
            }
            if ( i_selectedExcluded != null ) {
                i_result.addAll(i_selectedExcluded);
            }

            i_result.trimStorage();

            return i_result;
        }
    }

    protected Set<String> selectAnnotations(String classOrPackageName,
                                            int scanPolicies,
                                            AnnotationCategory category) {

        // There is a direct line into this call from public APIs (e.g., from getMethodAnnotations).
        // To this point, no steps have been taken to ensure that scans have been performed,
        // meaning, no steps have been taken to ensure the intern maps are populated.

        if ( scanPolicies == 0 ) {
            return Collections.emptySet();
        } else if ( ScanPolicy.EXTERNAL.accept(scanPolicies) ) {
            ensureExternalResults();
        } else {
            ensureInternalResults();
        }

        String i_classOrPackageName = internClassName(classOrPackageName, Util_InternMap.DO_NOT_FORCE);
        if ( i_classOrPackageName == null ) {
            return Collections.emptySet();
        }

        return uninternClassNames( i_selectAnnotations(i_classOrPackageName, scanPolicies, category) );
    }

    protected Set<String> i_selectAnnotations(String i_classOrPackageName,
                                              int scanPolicies,
                                              AnnotationCategory category) {

        // Only one of the result buckets can have annotations results for
        // a given target.
        if ( ScanPolicy.SEED.accept(scanPolicies) ) {
            TargetsTableImpl useSeedData = getSeedData();
            if ( useSeedData != null ) {
                Set<String> i_selected = useSeedData.i_getAnnotations(category, i_classOrPackageName);
                if ( !i_selected.isEmpty() ) {
                    return i_selected;
                }
            }
        }

        if ( ScanPolicy.PARTIAL.accept(scanPolicies) ) {
            TargetsTableImpl usePartialData = getPartialData();
            if ( usePartialData != null ) {
                Set<String> i_selected = usePartialData.i_getAnnotations(category, i_classOrPackageName);
                if ( !i_selected.isEmpty() ) {
                    return i_selected;
                }
            }
        }

        if ( ScanPolicy.EXCLUDED.accept(scanPolicies) ) {
            TargetsTableImpl useExcludedData = getExcludedData();
            if ( useExcludedData != null ) {
                Set<String> i_selected = useExcludedData.i_getAnnotations(category, i_classOrPackageName);
                if ( !i_selected.isEmpty() ) {
                    return i_selected;
                }
            }
        }

        return Collections.emptySet();
    }

    protected Set<String> selectAnnotatedTargets(int scanPolicies, AnnotationCategory category) {
        return uninternClassNames( i_selectAnnotatedTargets(scanPolicies, category) );
    }

    protected Set<String> i_selectAnnotatedTargets(int scanPolicies, AnnotationCategory category) {
        int nonEmptyCount = 0;
        int totalCount = 0;

        Set<String> i_selectedSeed = null;
        if ( ScanPolicy.SEED.accept(scanPolicies) ) {
            TargetsTableImpl useSeedData = getSeedData();
            if ( useSeedData != null ) {
                i_selectedSeed = useSeedData.getAnnotatedTargets(category);
                if ( i_selectedSeed.isEmpty() ) {
                    i_selectedSeed = null;
                } else {
                    nonEmptyCount++;
                    totalCount += i_selectedSeed.size();
                }
            }
        }

        Set<String> i_selectedPartial = null;
        if ( ScanPolicy.PARTIAL.accept(scanPolicies) ) {
            TargetsTableImpl usePartialData = getPartialData();
            if ( usePartialData != null ) {
                i_selectedPartial = usePartialData.getAnnotatedTargets(category);
                if ( i_selectedPartial.isEmpty() ) {
                    i_selectedPartial = null;
                } else {
                    nonEmptyCount++;
                    totalCount += i_selectedPartial.size();
                }
            }
        }

        Set<String> i_selectedExcluded = null;
        if ( ScanPolicy.EXCLUDED.accept(scanPolicies) ) {
            TargetsTableImpl useExcludedData = getExcludedData();
            if ( useExcludedData != null ) {
                i_selectedExcluded = useExcludedData.getAnnotatedTargets(category);
                if ( i_selectedExcluded.isEmpty() ) {
                    i_selectedExcluded = null;
                } else {
                    nonEmptyCount++;
                    totalCount += i_selectedExcluded.size();
                }
            }
        }

        if ( nonEmptyCount == 0 ) {
            return Collections.emptySet();

        } else if ( nonEmptyCount == 1 ) {
            if ( i_selectedSeed != null ) {
                return i_selectedSeed;
            } else if ( i_selectedPartial != null ) {
                return i_selectedPartial;
            } else {
                return i_selectedExcluded;
            }

        } else {
            UtilImpl_IdentityStringSet i_result = new UtilImpl_IdentityStringSet(totalCount);

            if ( i_selectedSeed != null ) {
                i_result.addAll(i_selectedSeed);
            }
            if ( i_selectedPartial != null ) {
                i_result.addAll(i_selectedPartial);
            }
            if ( i_selectedExcluded != null ) {
                i_result.addAll(i_selectedExcluded);
            }

            i_result.trimStorage();

            return i_result;
        }
    }

    protected Set<String> selectAnnotatedTargets(String annotationName,
                                                 int scanPolicies,
                                                 AnnotationCategory category) {

        // There is a direct line into this call from public APIs (e.g., from getMethodAnnotations).
        // To this point, no steps have been taken to ensure that scans have been performed,
        // meaning, no steps have been taken to ensure the intern maps are populated.

        if ( scanPolicies == 0 ) {
            return Collections.emptySet();
        } else if ( ScanPolicy.EXTERNAL.accept(scanPolicies) ) {
            ensureExternalResults();
        } else {
            ensureInternalResults();
        }

        String i_annotationName = internClassName(annotationName, Util_InternMap.DO_NOT_FORCE);
        if ( i_annotationName == null ) {
            return Collections.emptySet();
        } else {
            return uninternClassNames( i_selectAnnotatedTargets(i_annotationName, scanPolicies, category) );
        }
    }

    protected Set<String> i_selectAnnotatedTargets(String i_annotationName,
                                                   int scanPolicies,
                                                   AnnotationCategory category) {

        int nonEmptyCount = 0;
        int totalCount = 0;

        Set<String> i_selectedSeed = null;
        if ( ScanPolicy.SEED.accept(scanPolicies) ) {
            TargetsTableImpl useSeedData = getSeedData();
            if ( useSeedData != null ) {
                i_selectedSeed = useSeedData.getAnnotatedTargets(category, i_annotationName);
                if ( i_selectedSeed.isEmpty() ) {
                    i_selectedSeed = null;
                } else {
                    nonEmptyCount++;
                    totalCount += i_selectedSeed.size();
                }
            }
        }

        Set<String> i_selectedPartial = null;
        if ( ScanPolicy.PARTIAL.accept(scanPolicies) ) {
            TargetsTableImpl usePartialData = getPartialData();
            if ( usePartialData != null ) {
                i_selectedPartial = usePartialData.getAnnotatedTargets(category, i_annotationName);
                if ( i_selectedPartial.isEmpty() ) {
                    i_selectedPartial = null;
                } else {
                    nonEmptyCount++;
                    totalCount += i_selectedPartial.size();
                }
            }
        }

        Set<String> i_selectedExcluded = null;
        if ( ScanPolicy.EXCLUDED.accept(scanPolicies) ) {
            TargetsTableImpl useExcludedData = getExcludedData();
            if ( useExcludedData != null ) {
                i_selectedExcluded = useExcludedData.getAnnotatedTargets(category, i_annotationName);
                if ( i_selectedExcluded.isEmpty() ) {
                    i_selectedExcluded = null;
                } else {
                    nonEmptyCount++;
                    totalCount += i_selectedExcluded.size();
                }
            }
        }

        if ( nonEmptyCount == 0 ) {
            return Collections.emptySet();

        } else if ( nonEmptyCount == 1 ) {
            if ( i_selectedSeed != null ) {
                return i_selectedSeed;
            } else if ( i_selectedPartial != null ) {
                return i_selectedPartial;
            } else {
                return i_selectedExcluded;
            }

        } else {
            UtilImpl_IdentityStringSet i_result = new UtilImpl_IdentityStringSet(totalCount);

            if ( i_selectedSeed != null ) {
                i_result.addAll(i_selectedSeed);
            }
            if ( i_selectedPartial != null ) {
                i_result.addAll(i_selectedPartial);
            }
            if ( i_selectedExcluded != null ) {
                i_result.addAll(i_selectedExcluded);
            }

            i_result.trimStorage();

            return i_result;
        }
    }

    // Special helpers for obtaining inherited annotations ...

    @Override
    public Set<String> getAllInheritedAnnotatedClasses(String annotationName, int scanPolicies) {
        return getAllInheritedAnnotatedClasses(annotationName, scanPolicies, scanPolicies);
    }

    @Override
    public Set<String> getAllInheritedAnnotatedClasses(String annotationName,
                                                       int declarerScanPolicies,
                                                       int inheritorScanPolicies) {

        Set<String> allClassNames = new HashSet<String>();

        // For each class which has the specified annotation as a class annotation ...

        for ( String className : getAnnotatedClasses(annotationName, declarerScanPolicies) ) {
            // Add that class as a declared target ...
            allClassNames.add(className);

            // And add all subclasses as targets ...
            allClassNames.addAll(getSubclassNames(className));

            // The result of 'getSubclassnames' can never be null for
            // a class which is recorded as a declared class annotation
            // target.  That is only possible for a class which was
            // scanned, and such never answer null from 'getSubclassNames'.
        }

        Set<String> regionClassNames = getClassNames(inheritorScanPolicies);

        return UtilImpl_Utils.restrict(allClassNames, regionClassNames);
    }

    // Class relationship data for all scanned classes.

    @Override
    public Set<String> getAllInheritedAnnotatedClasses(String annotationName) {
        Set<String> allClassNames = new HashSet<String>();

        // For each class which has the specified annotation as a class annotation ...

        for ( String className : getAnnotatedClasses(annotationName) ) {
            // Add that class as a declared target ...
            allClassNames.add(className);

            // And add all subclasses as targets ...
            allClassNames.addAll( getSubclassNames(className) );

            // The result of 'getSubclassnames' can never be null for
            // a class which is recorded as a declared class annotation
            // target.  That is only possible for a class which was
            // scanned, and such never answer null from 'getSubclassNames'.
        }

        return allClassNames;
    }

    @Override
    public String getSuperclassName(String subclassName) {
        return getClassTable().getSuperclassName(subclassName);
    }

    public String i_getSuperclassName(String i_subclassName) {
        return getClassTable().i_getSuperclassName(i_subclassName);
    }

    public Map<String, String> i_getSuperclassNames() {
        return getClassTable().i_getSuperclassNames();
    }

    @Override
    public String[] getInterfaceNames(String className) {
        return getClassTable().getInterfaceNames(className);
    }

    protected String[] i_getInterfaceNames(String i_className) {
        return getClassTable().i_getInterfaceNames(i_className);
    }

    protected Map<String, String[]> i_getInterfaceNames() {
        return getClassTable().i_getInterfaceNames();
    }

    @Override
    public Set<String> getAllImplementorsOf(String interfaceName) {
        String i_interfaceName = internClassName(interfaceName, Util_InternMap.DO_NOT_FORCE);
        if ( i_interfaceName == null ) {
            return Collections.emptySet();
        }

        return getClassTable().i_getAllImplementorsOf(i_interfaceName);
    }

    @Override
    public Set<String> getSubclassNames(String superclassName) {
        return getClassTable().getSubclassNames(superclassName);
    }

    @Override
    public boolean isInstanceOf(String candidateClassName, Class<?> classOrInterface) {
        return isInstanceOf( candidateClassName, classOrInterface.getName(), classOrInterface.isInterface() );
    }
    
    @Override
    public boolean isInstanceOf(String candidateClassName, String criterionClassName, boolean isInterface) {
        String i_classOrInterfaceName = internClassName(criterionClassName, Util_InternMap.DO_NOT_FORCE);
        if ( i_classOrInterfaceName == null ) {
            return false;
        }

        String i_candidateClassName = internClassName(candidateClassName, Util_InternMap.DO_NOT_FORCE);
        if ( i_candidateClassName == null ) {
            return false;
        }

        return getClassTable().i_isInstanceOf(i_candidateClassName,
                                              i_classOrInterfaceName,
                                              isInterface);
    }

    //

    @Override
    public Integer i_getModifiers(String i_className) {
        return getClassTable().i_getModifiers(i_className);
    }

    @Override
    public Integer getModifiers(String className) {
        String i_className = internClassName(className, Util_InternMap.DO_NOT_FORCE);
        if ( i_className == null ) {
            return null;
        }
        return getClassTable().i_getModifiers(i_className);
    }

    @Override
    public int i_getModifiersValue(String i_className) {
        return getClassTable().i_getModifiersValue(i_className);
    }

    @Override
    public int getModifiersValue(String className) {
        String i_className = internClassName(className, Util_InternMap.DO_NOT_FORCE);
        if ( i_className == null ) {
            return 0;
        }
        return getClassTable().i_getModifiersValue(i_className);
    }

    @Override
    public EnumSet<AnnotationTargets_OpCodes> i_getModifiersSet(String i_className) {
        Integer modifiers = i_getModifiers(i_className);
        if ( modifiers == null ) {
            return null;
        } else {
            return AnnotationTargets_OpCodes.split( modifiers.intValue() );
        }
    }

    @Override
    public EnumSet<AnnotationTargets_OpCodes> getModifiersSet(String className) {
        String i_className = internClassName(className, Util_InternMap.DO_NOT_FORCE);
        if ( i_className == null ) {
            return null;
        }
        return i_getModifiersSet(i_className);
    }

    @Override
    public EnumSet<AnnotationTargets_OpCodes> i_getModifiersSet(
        String i_className,
        EnumSet<AnnotationTargets_OpCodes> modifierSet) {

        Integer modifiers = i_getModifiers(i_className);
        if ( modifiers == null ) {
            return null;
        } else {
            return AnnotationTargets_OpCodes.place( modifiers.intValue(), modifierSet );
        }
    }

    @Override
    public EnumSet<AnnotationTargets_OpCodes> getModifiersSet(
        String className,
        EnumSet<AnnotationTargets_OpCodes> modifierSet) {

        String i_className = internClassName(className, Util_InternMap.DO_NOT_FORCE);
        if ( i_className == null ) {
            return null;
        }
        return i_getModifiersSet(i_className, modifierSet);
    }

    @Override
    public boolean isAbstract(String className) {
        Integer modifiers = getModifiers(className);
        if ( modifiers == null ) {
            return false;
        } else {
            return ( (modifiers.intValue() & Opcodes.ACC_ABSTRACT) != 0 );
        }
    }

    @Override
    public boolean isInterface(String className) {
        Integer modifiers = getModifiers(className);
        if ( modifiers == null ) {
            return false;
        } else {
            return ( (modifiers.intValue() & Opcodes.ACC_INTERFACE) != 0 );
        }
    }

    // Logging ...

    protected void log(Logger useLogger,
                       TargetCache_Options cacheOptions) {

        String methodName = "log";

        if ( !useLogger.isLoggable(Level.FINER) ) {
            return;
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Cache Options:");

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  Disabled:      [ {0} ]", Boolean.valueOf(cacheOptions.getDisabled()));
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  Directory:     [ {0} ]", cacheOptions.getDir());
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  Read-Only:     [ {0} ]", Boolean.valueOf(cacheOptions.getReadOnly()));
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  Always Valid:  [ {0} ]", Boolean.valueOf(cacheOptions.getAlwaysValid()));
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  Validate:      [ {0} ]", Boolean.valueOf(cacheOptions.getValidate()));
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  Write Threads: [ {0} ]", Integer.valueOf(cacheOptions.getWriteThreads()));
    }

    //

    @Override
    public void logState() {
        if (stateLogger.isLoggable(Level.FINER) ) {
            log(stateLogger);
        }
    }

    @Override
    public void log(Logger useLogger) {
        String methodName = "log";

        if ( !useLogger.isLoggable(Level.FINER) ) {
            return;
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "BEGIN STATE [ {0} ]", getHashText());

        // Scan flags: Used to distinguish the limited and specific scans.

        if ( limitedScan ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Limited scan");
        }
        if ( specificScan ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Specific scan");
        }

        // Overall scan intermediate settings:
        // The root class source is set before the internal scan step;
        // The overall scanner is set by the internal scan step.
        // Both the root class source and the overall scanner are cleared
        // by the external scan step.

        if ( rootClassSource != null ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Root class source [ " + rootClassSource + " ]");
        }
        if ( overallScanner != null ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Overall scanner still present");
        }

        // Internal results: Which are set depends on the
        // scan type and scan phase.
        //
        // Before any scan has been performed, none are set.
        //
        // After the internal phase of an overall scan,
        // none, some, or all may be set.
        //
        // After limited and specific scans, seed results are
        // set, and partial and excluded results are null.

        if ( seedData != null ) {
            seedData.log(useLogger);
        }
        if ( partialData != null ) {
            partialData.log(useLogger);
        }
        if ( excludedData != null ) {
            excludedData.log(useLogger);
        }

        // Overall scan results.  Particular rules apply to both
        // of these values ...

        // ... class data is null pre-scan, is set to a partially completed
        // value by the internal scan phase, and is completed by the
        // external scan phase.
        if ( classData != null ) {
            classData.log(useLogger);
        }

        // ...external data is null pre-scan, and is set after the external
        // scan phase.
        if ( externalData != null ) {
            externalData.log(useLogger);
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "END STATE [ {0} ]", getHashText());
    }

    //

    public TargetsDeltaImpl subtract(AnnotationTargetsImpl_Targets initialTargets) {
        return new TargetsDeltaImpl( getFactory(), getAppName(), getModName(), this, initialTargets);
    }
}
