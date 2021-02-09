/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.targets.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectweb.asm.Opcodes;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.annocache.service.internal.AnnotationCacheServiceImpl_Logging;
import com.ibm.ws.annocache.targets.cache.internal.TargetCacheImpl_DataApp;
import com.ibm.ws.annocache.targets.cache.internal.TargetCacheImpl_DataApps;
import com.ibm.ws.annocache.targets.cache.internal.TargetCacheImpl_DataMod;
import com.ibm.ws.annocache.targets.cache.internal.TargetCacheImpl_DataQueries;
import com.ibm.ws.annocache.targets.delta.internal.TargetsDeltaImpl;
import com.ibm.ws.annocache.util.internal.UtilImpl_EmptyBidirectionalMap;
import com.ibm.ws.annocache.util.internal.UtilImpl_EmptyInternMap;
import com.ibm.ws.annocache.util.internal.UtilImpl_IdentityStringSet;
import com.ibm.ws.annocache.util.internal.UtilImpl_InternMap;
import com.ibm.ws.annocache.util.internal.UtilImpl_NonInternSet;
import com.ibm.ws.annocache.util.internal.UtilImpl_Utils;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.util.Util_InternMap.ValueType;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.annocache.targets.AnnotationTargets_Exception;
import com.ibm.wsspi.annocache.targets.AnnotationTargets_OpCodes;
import com.ibm.wsspi.annocache.targets.AnnotationTargets_Targets;
import com.ibm.wsspi.annocache.targets.cache.TargetCache_InternalConstants.QueryType;
import com.ibm.wsspi.annocache.targets.cache.TargetCache_Options;
import com.ibm.wsspi.annocache.util.Util_BidirectionalMap;
import com.ibm.wsspi.annocache.util.Util_InternMap;

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
    protected static final Logger logger = AnnotationCacheServiceImpl_Logging.ANNO_LOGGER;
    protected static final Logger stateLogger = AnnotationCacheServiceImpl_Logging.ANNO_STATE_LOGGER;
    protected static final Logger queryLogger = AnnotationCacheServiceImpl_Logging.ANNO_QUERY_LOGGER;

    public static final String CLASS_NAME = AnnotationTargetsImpl_Targets.class.getSimpleName(); 
            
    protected final String hashText;
    protected String hashName;

    @Trivial
    public String getHashText() {
        return hashText;
    }

    @Trivial
    protected String getHashName() {
        return hashName;
    }

    protected void appendHashName(String text) {
        hashName += text;
    }

    //

    protected AnnotationTargetsImpl_Targets(
        AnnotationTargetsImpl_Factory factory,
        TargetCacheImpl_DataApps annoCache,
        UtilImpl_InternMap classNameInternMap,
        UtilImpl_InternMap fieldNameInternMap,
        UtilImpl_InternMap methodSignatureInternMap,
        boolean isLightweight) {

        String methodName = "<init>";

        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());
        this.hashName = this.hashText;

        this.factory = factory;

        this.annoCache = annoCache;
        this.isLightweight = isLightweight;

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
        this.specificClassNames = null;

        this.rootClassSource = null;
        this.overallScanner = null;

        this.seedTable = null;
        this.partialTable = null;
        this.excludedTable = null;

        this.externalTable = null;

        this.classTable = null;

        this.queriesData = null;

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ]", this.hashText);
        }
    }

    // Factory, and related methods ...

    protected final AnnotationTargetsImpl_Factory factory;

    @Override
    @Trivial
    public AnnotationTargetsImpl_Factory getFactory() {
        return factory;
    }

    protected UtilImpl_EmptyInternMap createEmptyInternMap(Util_InternMap.ValueType valueType, String mapName) {
        return getFactory().getUtilFactory().createEmptyInternMap(valueType, mapName);
    }

    protected Util_BidirectionalMap createBidiMap(String holderTag, UtilImpl_InternMap holderInternMap,
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


    private final boolean isLightweight;

    @Override
    public boolean getIsLightweight() {
        return isLightweight;
    }

    // Class, field, and method interning ...

    protected final UtilImpl_InternMap classNameInternMap;

    public UtilImpl_InternMap getClassNameInternMap() {
        return classNameInternMap;
    }

    public String internClassName(String className, boolean doForce) {
        return getClassNameInternMap().intern(className, doForce);
    }

    @Trivial
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

    @Trivial
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
     * @param useSpecificClassNames The names of the classes which are to be scanned.
     */
    @Override
    public void scan(ClassSource_Aggregate useRootClassSource, Set<String> useSpecificClassNames)
        throws AnnotationTargets_Exception {
        scan(useRootClassSource, useSpecificClassNames, TargetsVisitorClassImpl.SELECT_ALL_ANNOTATIONS);
        // throws AnnotationTargets_Exception {
    }

    //

    /**
     * <p>Do a limited scan.  The cache is not enabled, and no external class source is available.</p>
     *
     * @param useRootClassSource The class source which is to be scanned.
     */
    @Override
    @Trivial
    public void scanLimited(ClassSource_Aggregate useRootClassSource) {
        String methodName = "scanLimited";

        if ( limitedScan ) {
            if (logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER / RETURN (already scanned)", getHashName());
            }
            return;
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "ENTER [ {0} ]", getHashName());
        }

        limitedScan = true;

        TargetsScannerLimitedImpl limitedScanner = new TargetsScannerLimitedImpl(this, useRootClassSource);

        limitedScanner.scanContainer();

        putLimitedResults(limitedScanner);

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "RETURN [ {0} ]", getHashName());
        }
    }

    protected void putLimitedResults(TargetsScannerLimitedImpl scanner) {
        seedTable = scanner.getTargetsTable();
        partialTable = null;
        excludedTable = null;
        externalTable = null;

        classTable = null;
    }

    //

    /**
     * <p>Scan specific classes for annotations.</p>
     *
     * <p>Scan only for annotations: Do not complete class reference information.</p>
     *
     * @param useRootClassSource The root class source which is to be scanned.
     * @param useSpecificClassNames The names of the classes which are to be scanned.
     * @param useSpecificAnnotationClassNames The names of the annotations which of are of interest.
     */
    @Override
    @Trivial
    public void scan(
        ClassSource_Aggregate useRootClassSource,
        Set<String> useSpecificClassNames,
        Set<String> useSpecificAnnotationClassNames) throws AnnotationTargets_Exception {

        String methodName = "scan";

        if ( specificScan ) {
            if (logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER / RETURN (already scanned)", getHashName());
            }
            return;
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "ENTER [ {0} ]", getHashName());
        }

        specificScan = true;
        specificClassNames = new HashSet<String>(useSpecificClassNames);

        // Need to set the app and module names to use the query logger.
        setNames(useRootClassSource);

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "App [ {0} ] Mod [ {1} ]", new Object[] { getAppName(), getModFullName() });
        }

        TargetsScannerSpecificImpl specificScanner = new TargetsScannerSpecificImpl(this, useRootClassSource);

        specificScanner.scan(useSpecificClassNames, useSpecificAnnotationClassNames);
        // throws AnnotationTargets_Exception

        putSpecificResults(specificScanner);

        // The query logger has its own reference to module data.

        TargetCacheImpl_DataApp appData = getAnnoCache().getAppForcing( getAppName() );

        putQueriesData(appData);

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "RETURN [ {0} ]", getHashName());
        }
    }

    protected void putSpecificResults(TargetsScannerBaseImpl scanner) {
        seedTable = scanner.getSeedTable();
        partialTable = scanner.getPartialTable();
        excludedTable = scanner.getExcludedTable();
        externalTable = scanner.getExternalTable();

        classTable = scanner.getClassTable();
    }

    // Policy data fan-outs ...

    // Phase 1: Internal data

    protected ClassSource_Aggregate rootClassSource;

    protected String appName;
    protected String modName;
    protected String modCatName;
    protected String modFullName;

    protected void setRootClassSource(ClassSource_Aggregate rootClassSource) {
        this.rootClassSource = rootClassSource;

        this.setNames(rootClassSource);
    }

    protected void setNames(ClassSource_Aggregate useRootClassSource) {
        this.appName = useRootClassSource.getApplicationName();
        this.modName = useRootClassSource.getModuleName();
        this.modCatName = useRootClassSource.getModuleCategoryName();

        String useModFullName;
        if ( this.modName == null ) {
            useModFullName = null;
        } else if ( this.modCatName == null ) {
            useModFullName = this.modName;
        } else {
            useModFullName = this.modName + "_" + this.modCatName;
        }

        this.modFullName = useModFullName;

        this.appendHashName( "(" + this.appName + ":" + this.modFullName + ")" );
    }

    @Trivial
    public String getAppName() {
        return appName;
    }
    
    @Trivial
    public String getModName() {
        return modName;
    }

    @Trivial
    public String getModCatName() {
        return modCatName;
    }

    @Trivial
    public String getModFullName() {
        return modFullName;
    }

    protected ClassSource_Aggregate consumeRootClassSource() {
        ClassSource_Aggregate useRootClassSource = rootClassSource;
        rootClassSource = null;
        return useRootClassSource;
    }

    //

    protected boolean limitedScan;
    protected boolean specificScan;
    protected Set<String> specificClassNames;

    public boolean isLimited() {
        return limitedScan;
    }

    @Override
    public boolean isSpecific() {
        return specificScan;
    }

    @Override
    public Set<String> getSpecificClassNames() {
        return specificClassNames;
    }

    //

    protected TargetsScannerOverallImpl overallScanner;

    // Phase 1: Internal result tables:

    protected TargetsTableImpl seedTable;
    protected TargetsTableImpl partialTable;
    protected TargetsTableImpl excludedTable;

    // Phase 2: External result tables:

    protected TargetsTableImpl externalTable;

    protected TargetsTableClassesMultiImpl classTable;

    //

    public TargetsTableImpl getSeedTable() {
        ensureInternalResults();

        return seedTable;
    }

    public TargetsTableImpl getPartialTable() {
        ensureInternalResults();

        return partialTable;
    }

    public TargetsTableImpl getExcludedTable() {
        ensureInternalResults();

        return excludedTable;
    }

    public TargetsTableImpl getExternalTable() {
        ensureExternalResults();

        return externalTable;
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
    // PreScan:          (rootClassSource != null) (overallScanner == null) (externalTable == null)
    // PostInternalScan: (rootClassSource != null) (overallScanner != null) (externalTable == null)
    // PostExternalScan: (rootClassSource == null) (overallScanner == null) (externalTable != null)
    //
    // When a limited scan is performed, the scanner is not held temporarily,
    // and there are only two phases:
    //
    // PreScan:          (seedTable == null)
    // PostScan:         (seedTable != null)
    //
    // When a specific scan is performed, the scanner is not held temporarily,
    // and there are only two phases:
    //
    // PreScan:          (seedTable == null)
    // PostScan:         (seedTable != null)

    public boolean hasInternalTable() {
        if ( limitedScan || specificScan ) {
            return ( seedTable != null );
        } else {
            return ( (externalTable != null) || (overallScanner != null ) );
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
    protected TargetsTableImpl getPolicyTable(ScanPolicy policy) {
        if ( policy == ScanPolicy.SEED ) {
            return getSeedTable();

        } else if ( policy == ScanPolicy.PARTIAL ) {
            return getPartialTable();

        } else if ( policy == ScanPolicy.EXCLUDED ) {
            return getExcludedTable();

        } else if ( policy == ScanPolicy.EXTERNAL ) {
            return getExternalTable();

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

        return classTable;
    }

    @Trivial
    protected Set<String> getInternalClassNames(String classSourceName) {
        return getInternalClassTable().getClassNames(classSourceName);
    }

    public TargetsTableClassesMultiImpl getClassTable() {
        ensureExternalResults();

        return classTable;
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
    @Trivial
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

    // 'ensureInternalResults' and 'ensureExternalResults',
    // which are invoked before accessing data, and which are the
    // primary steps which update internal state, must be synchronized.
    //
    // After the data is ensured, the subsequent access is safe to access
    // the respective internal or external data.

    protected synchronized void ensureInternalResults() {
        String methodName = "ensureInternalResults";

        if ( hasInternalTable() ) {
            return;
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "ENTER [ {0} ]", getHashName());
        }

        ClassSource_Aggregate useRootClassSource = consumeRootClassSource();
        String useAppName = getAppName();
        String useModFullName = getModFullName();

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "App [ {0} ] Mod [ {1} ]", new Object[] { useAppName, useModFullName });
        }

        TargetCacheImpl_DataApp appData = getAnnoCache().getAppForcing(useAppName);
        TargetCacheImpl_DataMod modData = appData.getModForcing(useModFullName, getIsLightweight() );

        TargetsScannerOverallImpl useOverallScanner =
            new TargetsScannerOverallImpl(this, useRootClassSource, modData);

        synchronized(modData) {
            useOverallScanner.validInternal();
        }

        putInternalResults(useOverallScanner);

        putQueriesData(appData);

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "RETURN [ {0} ]", getHashName());
        }
    }

    protected void putInternalResults(TargetsScannerOverallImpl useOverallScanner) {
        String methodName = "putInternalResults";

        overallScanner = useOverallScanner;

        seedTable = useOverallScanner.getSeedTable();
        partialTable = useOverallScanner.getPartialTable();
        excludedTable = useOverallScanner.getExcludedTable();

        // The class data is incomplete, but, is useful for queries which obtain
        // annotation results and which do not need superclass or interface information.
        classTable = useOverallScanner.getClassTable();

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] Class name intern map [ {1} ]",
                new Object[] { getHashName(), classNameInternMap.getHashText() });
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] Seed cass name intern map [ {1} ]",
                new Object[] { getHashName(), seedTable.getClassNameInternMap().getHashText() });
        }
    }

    private static final int NS_IN_MS = 1000 * 1000;

    // 'ensureInternalResults' and 'ensureExternalResults',
    // which are invoked before accessing data, and which are the
    // primary steps which update internal state, must be synchronized.
    //
    // After the data is ensured, the subsequent access is safe to access
    // the respective internal or external data.

    protected synchronized void ensureExternalResults() {
        String methodName = "ensureExternalResults";

        if ( externalTable != null ) {
            return;
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "ENTER [ {0} ]", getHashName());
        }

        ensureInternalResults();

        TargetsScannerOverallImpl useOverallScanner = overallScanner;
        overallScanner = null;

        useOverallScanner.validExternal();

        externalTable = useOverallScanner.getExternalTable();

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

        ClassSource_Aggregate useRootClassSource = useOverallScanner.getRootClassSource();

        useRootClassSource.addCacheReadTime(cacheReadTime, "Module Reads");
        useRootClassSource.addCacheReadTime(containerReadTime, "Container Reads");

        useRootClassSource.addCacheWriteTime(cacheWriteTime, "Module Writes");
        useRootClassSource.addCacheWriteTime(containerWriteTime, "Container Writes");

        // The class table was already set during internal processing.

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "RETURN [ {0} ]", getHashName());
        }
    }

    //

    @Override
    @Trivial
    public Set<String> getSeedClassNames() {
        TargetsTableImpl useSeedTable = getSeedTable();

        if ( useSeedTable == null ) {
            return Collections.emptySet();
        } else {
            return useSeedTable.getClassNames();
        }
    }

    @Override
    public boolean isSeedClassName(String className) {
        TargetsTableImpl useSeedTable = getSeedTable();

        if ( useSeedTable == null ) {
            return false;
        } else {
            return useSeedTable.containsClassName(className);
        }
    }

    @Override
    @Trivial
    public Set<String> getPartialClassNames() {
        TargetsTableImpl usePartialTable = getPartialTable();

        if ( usePartialTable == null ) {
            return Collections.emptySet();
        } else {
            return usePartialTable.getClassNames();
        }
    }

    @Override
    public boolean isPartialClassName(String className) {
        TargetsTableImpl usePartialTable = getPartialTable();

        if ( usePartialTable == null ) {
            return false;
        } else {
            return usePartialTable.containsClassName(className);
        }
    }

    @Override
    @Trivial
    public Set<String> getExcludedClassNames() {
        TargetsTableImpl useExcludedTable = getExcludedTable();

        if ( useExcludedTable == null ) {
            return Collections.emptySet();
        } else {
            return useExcludedTable.getClassNames();
        }
    }

    @Override
    public boolean isExcludedClassName(String className) {
        TargetsTableImpl useExcludedTable = getExcludedTable();

        if ( useExcludedTable == null ) {
            return false;
        } else {
            return useExcludedTable.containsClassName(className);
        }
    }

    @Override
    @Trivial
    public Set<String> getExternalClassNames() {
        TargetsTableImpl useExternalTable = getExternalTable();

        if ( useExternalTable == null ) {
            return Collections.emptySet();
        } else {
            return useExternalTable.getClassNames();
        }
    }

    @Override
    public boolean isExternalClassName(String className) {
        TargetsTableImpl useExternalTable = getExternalTable();

        if ( useExternalTable == null ) {
            return false;
        } else {
            return useExternalTable.containsClassName(className);
        }
    }

    /**
     * <p>Answer the union of class names obtained from class sources having
     * specified policies.</p>
     *
     * @param scanPolicies Specification of which scan policies to select, as the bit-or
     *        of the policy values.
     *
     * @return The union of class names obtained for the specified policies.
     */
    @Override
    @Trivial
    public Set<String> getClassNames(int scanPolicies) {
        return uninternClassNames( i_getClassNames(scanPolicies) );
    }

    @Trivial
    public Set<String> i_getClassNames(int scanPolicies) {
        int nonEmptyCount = 0;
        int totalCount = 0;

        Set<String> i_seed = null;
        if ( ScanPolicy.SEED.accept(scanPolicies) ) {
            TargetsTableImpl useSeedTable = getSeedTable();
            if ( useSeedTable != null ) {
                i_seed = useSeedTable.i_getClassNames();
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
            TargetsTableImpl usePartialTable = getPartialTable();
            if ( usePartialTable != null ) {
                i_partial = usePartialTable.i_getClassNames();
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
            TargetsTableImpl useExcludedTable = getExcludedTable();
            if ( useExcludedTable != null ) {
                i_excluded = useExcludedTable.i_getClassNames();
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
            TargetsTableImpl useExternalTable = getExternalTable();
            if ( useExternalTable != null ) {
                i_external = useExternalTable.i_getClassNames();
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

            return i_result;
        }
    }

    // Seed data specific annotation lookups.
    //
    // Most lookups are for annotations in seed data.

    @Override
    public Set<String> getAnnotatedPackages() {
        TargetsTableImpl useSeedTable = getSeedTable();

        if ( useSeedTable == null ) {
            return Collections.emptySet();
        } else {
            return useSeedTable.getAnnotatedTargets(AnnotationCategory.PACKAGE);
        }
    }

    @Override
    public Set<String> getAnnotatedPackages(String annotationClassName) {
        TargetsTableImpl useSeedTable = getSeedTable();

        Set<String> annotatedPackages;

        if ( useSeedTable == null ) {
            annotatedPackages = Collections.emptySet();
        } else {
            annotatedPackages = useSeedTable.getAnnotatedTargets(AnnotationCategory.PACKAGE, annotationClassName);
        }

        writeQuery(
            CLASS_NAME, "getAnnotatedPackages",
            "Discover annotated packages",
            ScanPolicy.SEED, QueryType.PACKAGE,
            annotationClassName, annotatedPackages);

        return annotatedPackages;
    }

    @Override
    public Set<String> getPackageAnnotations() {
        TargetsTableImpl useSeedTable = getSeedTable();

        if ( useSeedTable == null ) {
            return Collections.emptySet();
        } else {
            return useSeedTable.getAnnotations(AnnotationCategory.PACKAGE);
        }
    }

    @Override
    public Set<String> getPackageAnnotations(String packageName) {
        TargetsTableImpl useSeedTable = getSeedTable();

        if ( useSeedTable == null ) {
            return Collections.emptySet();
        } else {
            return useSeedTable.getAnnotations(AnnotationCategory.PACKAGE, packageName);
        }
    }

    @Override
    @Trivial
    public Set<String> getAnnotatedClasses() {
        TargetsTableImpl useSeedTable = getSeedTable();

        if ( useSeedTable == null ) {
            return Collections.emptySet();
        } else {
            return useSeedTable.getAnnotatedTargets(AnnotationCategory.CLASS);
        }
    }

    /**
     * Answer the SEED classes which have a specified class annotation.
     *
     * Do not answer subclasses.  This method does not handle inheritance
     * cases.
     *
     * This query is logged.
     *
     * @param annotationClassName The name of the target class annotation.
     *
     * @return Names of SEED classes which have the specified class annotation.
     */
    @Override
    public Set<String> getAnnotatedClasses(String annotationClassName) {
        String methodName = "getAnnotatedClasses";

        Set<String> annotatedClasses;

        TargetsTableImpl useSeedTable = getSeedTable();

        if ( useSeedTable == null ) {
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "ENTER [ {0} ] RETURN [ 0 ]", annotationClassName);
            }
            annotatedClasses = Collections.emptySet();

        } else {
            annotatedClasses = useSeedTable.getAnnotatedTargets(AnnotationCategory.CLASS, annotationClassName);

            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "ENTER [ {0} ] RETURN [ {1} ]",
                        new Object[] { annotationClassName, Integer.valueOf(annotatedClasses.size()) });
                for ( String annotatedClassName : annotatedClasses ) {
                    logger.logp(Level.FINER, CLASS_NAME, methodName, "  [ {0} ]", annotatedClassName);
                }
            }
        }

        writeQuery(
            CLASS_NAME, methodName,
            "Discover annotated classes",
            ScanPolicy.SEED, QueryType.CLASS,
            annotationClassName, annotatedClasses);

        return annotatedClasses;
    }

    /**
     * Answer the SEED annotations which have the specified class annotation
     * and which are in the specified class source.
     *
     * Do not answer subclasses.  This method does not handle inheritance
     * cases.
     *
     * Answer only class names which are also in the specified class source.
     *
     * TODO: This seems unnecessarily restrictive: If the target class source
     *       does not have the SEED policy, an empty result must occur.
     *
     * This query is logged.
     *
     * @param classSourceName The name of the class source to which to
     *     restrict results.
     * @param annotationClassName The name of the target class annotation.
     *
     * @return Names of SEED classes which have ths specified class annotation.
     */
    @Override
    public Set<String> getAnnotatedClasses(String classSourceName, String annotationClassName) {
        Set<String> annotatedClassNames = getAnnotatedClasses(annotationClassName);
        Set<String> classSourceClassNames = getInternalClassNames(classSourceName);

        Set<String> annotatedClasses = ( UtilImpl_Utils.restrict(annotatedClassNames, classSourceClassNames) );

        writeQuery(
            CLASS_NAME, "getAnnotatedClasses",
            "Discover annotated classes",
            classSourceName, QueryType.CLASS,
            annotationClassName, annotatedClasses);

        return annotatedClasses;
    }

    @Override
    @Trivial
    public Set<String> getClassAnnotations() {
        TargetsTableImpl useSeedTable = getSeedTable();

        if ( useSeedTable == null ) {
            return Collections.emptySet();
        } else {
            return useSeedTable.getAnnotations(AnnotationCategory.CLASS);
        }
    }

    @Override
    public Set<String> getClassAnnotations(String className) {
        TargetsTableImpl useSeedTable = getSeedTable();

        if ( useSeedTable == null ) {
            return Collections.emptySet();
        } else {
            return useSeedTable.getAnnotations(AnnotationCategory.CLASS, className);
        }
    }

    @Trivial
    public Set<String> getClassesWithFieldAnnotations() {
        TargetsTableImpl useSeedTable = getSeedTable();

        if ( useSeedTable == null ) {
            return Collections.emptySet();
        } else {
            return useSeedTable.getAnnotatedTargets(AnnotationCategory.FIELD);
        }
    }

    /**
     * Answer the SEED annotations which have the specified field annotation.
     *
     * This query is logged.
     *
     * @param annotationClassName The name of the target field annotation.
     *
     * @return Names of SEED classes which have ths specified field annotation.
     */
    @Override
    public Set<String> getClassesWithFieldAnnotation(String annotationClassName) {
        TargetsTableImpl useSeedTable = getSeedTable();

        Set<String> annotatedClasses;

        if ( useSeedTable == null ) {
            annotatedClasses = Collections.emptySet();
        } else {
            annotatedClasses = useSeedTable.getAnnotatedTargets(AnnotationCategory.FIELD, annotationClassName);
        }

        writeQuery(
            CLASS_NAME, "getClassesWithFieldAnnotations",
            "Discover classes with annotated fields",
            ScanPolicy.SEED, QueryType.FIELD,
            annotationClassName, annotatedClasses);

        return annotatedClasses;
    }

    @Override
    public Set<String> getFieldAnnotations() {
        TargetsTableImpl useSeedTable = getSeedTable();

        if ( useSeedTable == null ) {
            return Collections.emptySet();
        } else {
            return useSeedTable.getAnnotations(AnnotationCategory.FIELD);
        }
    }

    @Override
    public Set<String> getFieldAnnotations(String className) {
        TargetsTableImpl useSeedTable = getSeedTable();

        if ( useSeedTable == null ) {
            return Collections.emptySet();
        } else {
            return useSeedTable.getAnnotations(AnnotationCategory.FIELD, className);
        }
    }

    @Trivial
    public Set<String> getClassesWithMethodAnnotations() {
        TargetsTableImpl useSeedTable = getSeedTable();

        if ( useSeedTable == null ) {
            return Collections.emptySet();
        } else {
            return useSeedTable.getAnnotatedTargets(AnnotationCategory.METHOD);
        }
    }

    /**
     * Answer the SEED annotations which have the specified method annotation.
     *
     * This query is logged.
     *
     * @param annotationClassName The name of the target method annotation.
     *
     * @return Names of SEED classes which have ths specified method annotation.
     */
    @Override
    public Set<String> getClassesWithMethodAnnotation(String annotationClassName) {
        TargetsTableImpl useSeedTable = getSeedTable();

        Set<String> annotatedClasses;

        if ( useSeedTable == null ) {
            annotatedClasses = Collections.emptySet();
        } else {
            annotatedClasses = useSeedTable.getAnnotatedTargets(AnnotationCategory.METHOD, annotationClassName);
        }

        writeQuery(
            CLASS_NAME, "getClassesWithMethodAnnotations",
            "Discover classes with annotated methods",
            ScanPolicy.SEED, QueryType.METHOD,
            annotationClassName, annotatedClasses);

        return annotatedClasses;
    }

    @Override
    public Set<String> getMethodAnnotations() {
        TargetsTableImpl useSeedTable = getSeedTable();

        if ( useSeedTable == null ) {
            return Collections.emptySet();
        } else {
            return useSeedTable.getAnnotations(AnnotationCategory.METHOD);
        }
    }

    @Override
    public Set<String> getMethodAnnotations(String className) {
        TargetsTableImpl useSeedTable = getSeedTable();

        if ( useSeedTable == null ) {
            return Collections.emptySet();
        } else {
            return useSeedTable.getAnnotations(AnnotationCategory.METHOD, className);
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
    public Set<String> getAnnotatedPackages(String annotationClassName, int scanPolicies) {
        // logging in 'selectAnnotatedTargets'
        return selectAnnotatedTargets(annotationClassName, scanPolicies, AnnotationCategory.PACKAGE);
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
    @Trivial
    public Set<String> getAnnotatedClasses(int scanPolicies) {
        return selectAnnotatedTargets(scanPolicies, AnnotationCategory.CLASS);
    }

    /**
     * Answer the annotations which have the specified class annotation,
     * and which are in the specified class source, and which are in a source
     * which has a specified policy.
     *
     * Do not answer subclasses.  This method does not handle inheritance
     * cases.
     *
     * Answer only classes which are in a region which has one of the specified
     * policies.
     *
     * Answer only class names which are also in the specified class source.
     *
     * TODO: This seems unnecessarily restrictive: If the target class source
     *       does not have one of the specified policies, an empty result must
     *       occur.
     *
     * This query is logged.
     *
     * @param classSourceName The name of the class source to which to
     *     restrict results.
     * @param annotationClassName The name of the target class annotation.
     * @param scanPolicies The scan policies to which to restrict the results.
     *
     * @return Names of SEED classes which have ths specified class annotation.
     */
    @Override
    public Set<String> getAnnotatedClasses(String classSourceName, String annotationClassName, int scanPolicies) {
        Set<String> annotatedClassNames = getAnnotatedClasses(annotationClassName, scanPolicies);
        Set<String> classSourceClassNames = getClassNames(classSourceName);

        Set<String> annotatedClasses = ( UtilImpl_Utils.restrict(annotatedClassNames, classSourceClassNames) );

        writeQuery(
            CLASS_NAME, "getAnnotatedClasses",
            "Discover annotated classes",
            scanPolicies, classSourceName, QueryType.CLASS,
            annotationClassName, annotatedClasses);

        return annotatedClasses;
    }

    /**
     * Select classes which have a specified class annotation.
     *
     * This query is logged.
     *
     * @param annotationClassName The name of the target method annotation.
     * @param scanPolicies The scan policies to which to restrict the annotated
     *     classes.
     *
     * @return Names of classes which have ths specified class annotation.
     */
    @Override
    public Set<String> getAnnotatedClasses(String annotationClassName, int scanPolicies) {
        String methodName = "getAnnotatedClasses";

        // logging in 'selectAnnotatedTargets'
        Set<String> annotatedClassNames = selectAnnotatedTargets(annotationClassName, scanPolicies, AnnotationCategory.CLASS);

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "Annotated classes [ {0} ] [ {1} ]: [ {2} ]",
                new Object[] { annotationClassName,
                               Integer.valueOf(scanPolicies),
                               Integer.valueOf(annotatedClassNames.size()) });

            for ( String annotatedClassName : annotatedClassNames ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "  [ {0} ]", annotatedClassName );
            }
        }

        return annotatedClassNames;
    }

    @Override
    @Trivial
    public Set<String> getClassAnnotations(int scanPolicies) {
        return selectAnnotations(scanPolicies, AnnotationCategory.CLASS);
    }

    @Override
    public Set<String> getClassAnnotations(String className, int scanPolicies) {
        return selectAnnotations(className, scanPolicies, AnnotationCategory.CLASS);
    }

    //

    @Override
    @Trivial
    public Set<String> getClassesWithFieldAnnotations(int scanPolicies) {
        return selectAnnotatedTargets(scanPolicies, AnnotationCategory.FIELD);
    }

    @Override
    public Set<String> getClassesWithFieldAnnotation(String annotationClassName, int scanPolicies) {
        // logging in 'selectAnnotatedTargets'
        return selectAnnotatedTargets(annotationClassName, scanPolicies, AnnotationCategory.FIELD);
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
    @Trivial
    public Set<String> getClassesWithMethodAnnotations(int scanPolicies) {
        return selectAnnotatedTargets(scanPolicies, AnnotationCategory.METHOD);
    }

    @Override
    public Set<String> getClassesWithMethodAnnotation(String annotationClassName, int scanPolicies) {
        // logging in 'selectAnnotatedTargets'
        return selectAnnotatedTargets(annotationClassName, scanPolicies, AnnotationCategory.METHOD);
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

    public Util_BidirectionalMap i_getPackageAnnotations() {
        return i_getAnnotationsMap(ScanPolicy.SEED, AnnotationCategory.PACKAGE);
    }

    public boolean i_packageHasAnnotation(String i_packageName, String i_annotationClassName) {
        return i_getPackageAnnotations().holds(i_packageName, i_annotationClassName);
    }

    public Util_BidirectionalMap i_getClassAnnotations() {
        return i_getAnnotationsMap(ScanPolicy.SEED, AnnotationCategory.CLASS);
    }

    public boolean i_classHasAnnotation(String i_className, String i_annotationClassName) {
        return i_getClassAnnotations().holds(i_className, i_annotationClassName);
    }

    public Util_BidirectionalMap i_getFieldAnnotations() {
        return i_getAnnotationsMap(ScanPolicy.SEED, AnnotationCategory.FIELD);
    }

    public boolean i_classHasFieldAnnotation(String i_className, String i_annotationClassName) {
        return i_getFieldAnnotations().holds(i_className, i_annotationClassName);
    }

    public Util_BidirectionalMap i_getMethodAnnotations() {
        return i_getAnnotationsMap(ScanPolicy.SEED, AnnotationCategory.METHOD);
    }

    public boolean i_classHasMethodAnnotation(String i_className, String i_annotationClassName) {
        return i_getMethodAnnotations().holds(i_className, i_annotationClassName);
    }

    protected final UtilImpl_EmptyBidirectionalMap emptyPackageAnnotations;
    protected final UtilImpl_EmptyBidirectionalMap emptyClassAnnotations;
    protected final UtilImpl_EmptyBidirectionalMap emptyFieldAnnotations;
    protected final UtilImpl_EmptyBidirectionalMap emptyMethodAnnotations;

    protected Util_BidirectionalMap i_getAnnotationsMap(ScanPolicy policy, AnnotationCategory category) {
        TargetsTableImpl policyTable = getPolicyTable(policy);

        if ( policyTable == null ) {
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
            return policyTable.i_getAnnotations(category);
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
            TargetsTableImpl useSeedTable = getSeedTable();
            if ( useSeedTable != null ) {
                i_selectedSeed = useSeedTable.i_getAnnotationNames(category);
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
            TargetsTableImpl usePartialTable = getPartialTable();
            if ( usePartialTable != null ) {
                i_selectedPartial = usePartialTable.i_getAnnotationNames(category);
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
            TargetsTableImpl useExcludedTable = getExcludedTable();
            if ( useExcludedTable != null ) {
                i_selectedExcluded = useExcludedTable.i_getAnnotationNames(category);
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
            TargetsTableImpl useSeedTable = getSeedTable();
            if ( useSeedTable != null ) {
                Set<String> i_selected = useSeedTable.i_getAnnotations(category, i_classOrPackageName);
                if ( !i_selected.isEmpty() ) {
                    return i_selected;
                }
            }
        }

        if ( ScanPolicy.PARTIAL.accept(scanPolicies) ) {
            TargetsTableImpl usePartialTable = getPartialTable();
            if ( usePartialTable != null ) {
                Set<String> i_selected = usePartialTable.i_getAnnotations(category, i_classOrPackageName);
                if ( !i_selected.isEmpty() ) {
                    return i_selected;
                }
            }
        }

        if ( ScanPolicy.EXCLUDED.accept(scanPolicies) ) {
            TargetsTableImpl useExcludedTable = getExcludedTable();
            if ( useExcludedTable != null ) {
                Set<String> i_selected = useExcludedTable.i_getAnnotations(category, i_classOrPackageName);
                if ( !i_selected.isEmpty() ) {
                    return i_selected;
                }
            }
        }

        return Collections.emptySet();
    }

    @Trivial
    protected Set<String> selectAnnotatedTargets(int scanPolicies, AnnotationCategory category) {
        return uninternClassNames( i_selectAnnotatedTargets(scanPolicies, category) );
    }

    @Trivial
    protected Set<String> i_selectAnnotatedTargets(int scanPolicies, AnnotationCategory category) {
        int nonEmptyCount = 0;
        int totalCount = 0;

        Set<String> i_selectedSeed = null;
        if ( ScanPolicy.SEED.accept(scanPolicies) ) {
            TargetsTableImpl useSeedTable = getSeedTable();
            if ( useSeedTable != null ) {
                i_selectedSeed = useSeedTable.getAnnotatedTargets(category);
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
            TargetsTableImpl usePartialTable = getPartialTable();
            if ( usePartialTable != null ) {
                i_selectedPartial = usePartialTable.getAnnotatedTargets(category);
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
            TargetsTableImpl useExcludedTable = getExcludedTable();
            if ( useExcludedTable != null ) {
                i_selectedExcluded = useExcludedTable.getAnnotatedTargets(category);
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

            return i_result;
        }
    }

    /**
     * Select classes which have a specified annotation.  The annotation may
     * be a package, class, field, or method annotation.  Restrict the results
     * to classes which are in regions having the specifed scan policies.
     *
     * This query is logged.
     *
     * @param annotationClassName The name of the target method annotation.
     * @param scanPolicies The scan policies to which to restrict the annotated
     *     classes.
     * @param category The type of annotation to select.
     *
     * @return Names of classes which have ths specified annotation.
     */
    protected Set<String> selectAnnotatedTargets(String annotationClassName,
                                                 int scanPolicies,
                                                 AnnotationCategory category) {

        // There is a direct line into this call from public APIs (e.g., from getMethodAnnotations).
        // To this point, no steps have been taken to ensure that scans have been performed,
        // meaning, no steps have been taken to ensure the intern maps are populated.

        Set<String> annotatedClasses;

        if ( scanPolicies == 0 ) {
            annotatedClasses = Collections.emptySet();

        } else {
            if ( ScanPolicy.EXTERNAL.accept(scanPolicies) ) {
                ensureExternalResults();
            } else {
                ensureInternalResults();
            }

            String i_annotationClassName = internClassName(annotationClassName, Util_InternMap.DO_NOT_FORCE);
            if ( i_annotationClassName == null ) {
                annotatedClasses = Collections.emptySet();
            } else {
                annotatedClasses =  uninternClassNames( i_selectAnnotatedTargets(i_annotationClassName, scanPolicies, category) );
            }
        }

        writeQuery(
            CLASS_NAME, "selectedAnnotatedTargets",
            "Discover annotated classes",
            scanPolicies, asQueryType(category),
            annotationClassName, annotatedClasses);
        
        return annotatedClasses;
    }

    protected Set<String> i_selectAnnotatedTargets(String i_annotationClassName,
                                                   int scanPolicies,
                                                   AnnotationCategory category) {

        int nonEmptyCount = 0;
        int totalCount = 0;

        Set<String> i_selectedSeed = null;
        if ( ScanPolicy.SEED.accept(scanPolicies) ) {
            TargetsTableImpl useSeedTable = getSeedTable();
            if ( useSeedTable != null ) {
                i_selectedSeed = useSeedTable.getAnnotatedTargets(category, i_annotationClassName);
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
            TargetsTableImpl usePartialTable = getPartialTable();
            if ( usePartialTable != null ) {
                i_selectedPartial = usePartialTable.getAnnotatedTargets(category, i_annotationClassName);
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
            TargetsTableImpl useExcludedTable = getExcludedTable();
            if ( useExcludedTable != null ) {
                i_selectedExcluded = useExcludedTable.getAnnotatedTargets(category, i_annotationClassName);
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

            return i_result;
        }
    }

    // Special helpers for obtaining inherited annotations ...

    @Trivial
    private String printString(Set<String> values) {
        if ( values.isEmpty() ) {
            return "{ }";

        } else if ( values.size() == 1 ) {
            for ( String value : values ) {
                return "{ " + value + " }";
            }
            return null; // Unreachable

        } else {
            StringBuilder builder = new StringBuilder();
            builder.append("{ ");
            boolean first = true;
            for ( String value : values ) {
                if ( !first ) {
                    builder.append(", ");
                } else {
                    first = false;
                }
                builder.append(value);
            }
            builder.append(" }");
            return builder.toString();
        }
    }

    @Override
    @Trivial
    public Set<String> getAllInheritedAnnotatedClasses(String annotationClassName, int scanPolicies) {
        return getAllInheritedAnnotatedClasses(annotationClassName, scanPolicies, scanPolicies);
    }

    @Override
    @Trivial
    public Set<String> getAllInheritedAnnotatedClasses(String annotationClassName,
                                                       int declarerScanPolicies,
                                                       int inheritorScanPolicies) {
        String methodName = "getAllInheritedAnnotatedClasses";

        Object[] logParms;
        if ( logger.isLoggable(Level.FINER) ) {
            logParms = new Object[] {
                getHashName(),
                annotationClassName,
                Integer.toHexString(declarerScanPolicies),
                Integer.toHexString(inheritorScanPolicies) };
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] ENTER [ {1} ] Super policies [ {2} ] Sub policies [ {3} ]",
                        logParms);
        } else {
            logParms = null;
        }

        Set<String> allClassNames = new HashSet<String>();

        // For each class which has the specified annotation as a class annotation ...

        for ( String className : getAnnotatedClasses(annotationClassName, declarerScanPolicies) ) {
            if ( logParms != null ) {
                logParms[1] = className;
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                            "[ {0} ] Add immediate [ {1} ]", logParms);
            }
            allClassNames.add(className);

            Set<String> subclassNames = getSubclassNames(className);
            if ( logParms != null ) {
                logParms[1] = printString(subclassNames);
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Add subclasses [ {1} ]", logParms);
            }
            allClassNames.addAll(subclassNames);

            // The result of 'getSubclassnames' can never be null for
            // a class which is recorded as a declared class annotation
            // target.  That is only possible for a class which was
            // scanned, and such never answer null from 'getSubclassNames'.
        }

        Set<String> regionClassNames = getClassNames(inheritorScanPolicies);
        if ( logParms != null ) {
            logParms[1] = Integer.valueOf(regionClassNames.size());
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Region count of classes [ {1} ]", logParms);
        }

        Set<String> selectedRegionClassNames = UtilImpl_Utils.restrict(allClassNames, regionClassNames);
        if ( logParms != null ) {
            logParms[1] = printString(selectedRegionClassNames);
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] RETURN [ {1} ]", logParms);
        }
        return selectedRegionClassNames;
    }

    // Class relationship data for all scanned classes.

    @Override
    @Trivial
    public Set<String> getAllInheritedAnnotatedClasses(String annotationClassName) {
        String methodName = "getAllInheritedAnnotatedClasses";
        Object[] logParms;
        if ( logger.isLoggable(Level.FINER) ) {
            logParms = new Object[] { getHashName(), annotationClassName };
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER [ {1} ]", logParms);
        } else {
            logParms = null;
        }

        Set<String> annotatedClassNames = new HashSet<String>();

        // For each class which has the specified annotation as a class annotation ...

        for ( String annotatedClassName : getAnnotatedClasses(annotationClassName) ) {
            if ( logParms != null ) {
                logParms[1] = annotatedClassName;
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Add immediate [ {1} ]", logParms);
            }
            annotatedClassNames.add(annotatedClassName);

            Set<String> subclassNames =  getSubclassNames(annotatedClassName);
            if ( logParms != null ) {
                logParms[1] = printString(subclassNames);
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Add subclasses [ {1} ]", logParms);
            }
            annotatedClassNames.addAll(subclassNames);

            // The result of 'getSubclassnames' can never be null for
            // a class which is recorded as a declared class annotation
            // target.  That is only possible for a class which was
            // scanned, and such never answer null from 'getSubclassNames'.
        }

        writeQuery(
            CLASS_NAME, methodName,
            "Discover inherited annotated classes",
            ScanPolicy.SEED, QueryType.INHERITED,
            annotationClassName, annotatedClassNames);

        if ( logParms != null ) {
            logParms[1] = printString(annotatedClassNames);
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] RETURN [ {1} ]", logParms);
        }
        return annotatedClassNames;
    }

    @Override
    @Trivial
    public String getSuperclassName(String subclassName) {
        String methodName = "getSuperclassName";
        String superclassName = getClassTable().getSuperclassName(subclassName);
        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] ENTER [ {1} ] / RETURN [ {2} ]",
                new Object[] { getHashName(), subclassName, superclassName });
        }
        return superclassName;
    }

    @Trivial
    public String i_getSuperclassName(String i_subclassName) {
        String methodName = "i_getSuperclassName";
        String superclassName = getClassTable().i_getSuperclassName(i_subclassName);
        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] ENTER [ {1} ] / RETURN [ {2} ]",
                new Object[] { getHashName(), i_subclassName, superclassName });
        }
        return superclassName;
    }

    @Trivial
    public Map<String, String> i_getSuperclassNames() {
        return getClassTable().i_getSuperclassNames();
    }

    @Override
    @Trivial
    public String[] getInterfaceNames(String className) {
        String methodName = "getInterfaceNames";
        String[] interfaceNames = getClassTable().getInterfaceNames(className);
        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] ENTER [ {1} ] / RETURN [ {2} ]",
                new Object[] { getHashName(), className, interfaceNames });
        }
        return interfaceNames;
    }

    @Trivial
    protected String[] i_getInterfaceNames(String i_className) {
        String methodName = "i_getInterfaceNames";
        String[] interfaceNames = getClassTable().i_getInterfaceNames(i_className);
        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] ENTER [ {1} ] / RETURN [ {2} ]",
                new Object[] { getHashName(), i_className, interfaceNames });
        }
        return interfaceNames;
    }

    @Trivial
    protected Map<String, String[]> i_getInterfaceNames() {
        return getClassTable().i_getInterfaceNames();
    }

    @Override
    @Trivial
    public Set<String> getAllImplementorsOf(String interfaceName) {
        String methodName = "getAllImplementorsOf";

        ensureExternalResults();

        String i_interfaceName = internClassName(interfaceName, Util_InternMap.DO_NOT_FORCE);
        if ( i_interfaceName == null ) {
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] ENTER [ {1} ] / RETURN [ null ] (not stored)",
                    new Object[] { getHashName(), interfaceName });
            }
            return Collections.emptySet();

        } else {
            Set<String> allImpl = getClassTable().i_getAllImplementorsOf(i_interfaceName);
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] ENTER [ {1} ] / RETURN [ {2} ]",
                    new Object[] { getHashName(), interfaceName, allImpl });
            }
            return allImpl;
        }
    }

    @Override
    @Trivial
    public Set<String> getSubclassNames(String superclassName) {
        String methodName = "getSubclassNames";
        Set<String> subclassNames = getClassTable().getSubclassNames(superclassName);
        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] ENTER [ {1} ] / RETURN [ {2} ]",
                new Object[] { getHashName(), superclassName, subclassNames });
        }
        return subclassNames;
    }

    @Override
    @Trivial
    public boolean isInstanceOf(String candidateClassName, Class<?> classOrInterface) {
        return isInstanceOf( candidateClassName, classOrInterface.getName(), classOrInterface.isInterface() );
    }

    @Trivial
    private void displayClassNames() {
        String methodName = "displayInternMap";
        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER [ Class names ]", getHashName());
            getClassNameInternMap().log(logger);
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] RETURN [ Class names ]", getHashName());
        }
    }

    @Override
    @Trivial
    public boolean isInstanceOf(String candidateClassName, String criterionClassName, boolean isInterface) {
        String methodName = "isInstanceOf";

        ensureExternalResults();

        displayClassNames();

        String i_classOrInterfaceName = internClassName(criterionClassName, Util_InternMap.DO_NOT_FORCE);
        if ( i_classOrInterfaceName == null ) {
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0 ] ENTER Class [ {1} ] Super or Interface [ {2} ] / RETURN [ false ] (criteria not stored)",
                    new Object[] { getHashName(), candidateClassName, criterionClassName });
            }
            return false;
        }

        String i_candidateClassName = internClassName(candidateClassName, Util_InternMap.DO_NOT_FORCE);
        if ( i_candidateClassName == null ) {
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0 ] ENTER Class [ {1} ] Super or Interface [ {2} ] / RETURN [ false ] (target not stored)",
                    new Object[] { getHashName(), candidateClassName, criterionClassName });
            }
            return false;
        }

        boolean isInstance = getClassTable().i_isInstanceOf(i_candidateClassName, i_classOrInterfaceName, isInterface);
        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] ENTER Class [ {1} ] Super or Interface [ {2} ] / RETURN [ {3} ]",
                new Object[] { getHashName(), candidateClassName, criterionClassName, Boolean.valueOf(isInstance) });
        }
        return isInstance;
    }

    //

    @Override
    public Integer i_getModifiers(String i_className) {
        return getClassTable().i_getModifiers(i_className);
    }

    @Override
    public Integer getModifiers(String className) {
        ensureExternalResults();

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
        ensureExternalResults();

        String i_className = internClassName(className, Util_InternMap.DO_NOT_FORCE);
        if ( i_className == null ) {
            return null;
        }
        return i_getModifiersSet(i_className);
    }

    @Override
    @Trivial
    public EnumSet<AnnotationTargets_OpCodes> i_getModifiersSet(
        String i_className,
        EnumSet<AnnotationTargets_OpCodes> modifierSet) {

        String methodName = "i_getModifiersSet";

        Integer modifiers = i_getModifiers(i_className);
        if ( modifiers == null ) {
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Class [ {1} ] Modifiers [ null ] (not stored)",
                    new Object[] { getHashName(), i_className });
            }
            return null;

        } else {
            modifierSet = AnnotationTargets_OpCodes.place( modifiers.intValue(), modifierSet );
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Class [ {1} ] Modifiers [ {2} ]",
                    new Object[] { getHashName(), i_className, modifierSet });
            }
            return modifierSet;
        }
    }

    @Override
    @Trivial
    public EnumSet<AnnotationTargets_OpCodes> getModifiersSet(
        String className,
        EnumSet<AnnotationTargets_OpCodes> modifierSet) {

        String methodName = "getModifiersSet";

        String i_className = internClassName(className, Util_InternMap.DO_NOT_FORCE);
        if ( i_className == null ) {
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Class [ {1} ] Modifiers [ null ] (not stored)",
                    new Object[] { getHashName(), className });
            }
            return null;
        } else {
            EnumSet<AnnotationTargets_OpCodes> modifiers = i_getModifiersSet(i_className, modifierSet);
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Class [ {1} ] Modifiers [ {2} ]",
                    new Object[] { getHashName(), className, modifiers });
            }
            return modifiers;
        }
    }

    @Override
    @Trivial
    public boolean isAbstract(String className) {
        String methodName = "isAbstract";

        boolean isAbstract;
        String isAbstractReason;

        Integer modifiers = getModifiers(className);
        if ( modifiers == null ) {
            isAbstract = false;
            isAbstractReason = "modifiers not stored";
        } else {
            isAbstract = ( (modifiers.intValue() & Opcodes.ACC_ABSTRACT) != 0 );
            isAbstractReason = "from modifiers";
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] Class [ {1} ] IsAbstract [ {2} ] ({3}",
                new Object[] { getHashName(), className, Boolean.valueOf(isAbstract), isAbstractReason });
        }

        return isAbstract;
    }

    @Override
    @Trivial
    public boolean isInterface(String className) {
        String methodName = "isInterface";

        boolean isInterface;
        String isInterfaceReason;

        Integer modifiers = getModifiers(className);
        if ( modifiers == null ) {
            isInterface = false;
            isInterfaceReason = "modifiers not stored";
        } else {
            isInterface = ( (modifiers.intValue() & Opcodes.ACC_INTERFACE) != 0 );
            isInterfaceReason = "from modifiers";
        }

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] Class [ {1} ] IsInterface [ {2} ] ({3}",
                new Object[] { getHashName(), className, Boolean.valueOf(isInterface), isInterfaceReason });
        }

        return isInterface;
    }

    // Logging ...

    @Trivial
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
        // useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  Validate:      [ {0} ]", Boolean.valueOf(cacheOptions.getValidate()));
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  Write Threads: [ {0} ]", Integer.valueOf(cacheOptions.getWriteThreads()));
    }

    //

    @Override
    @Trivial
    public void logState() {
        if (stateLogger.isLoggable(Level.FINER) ) {
            log(stateLogger);
        }
    }

    @Override
    @Trivial
    public void log(Logger useLogger) {
        String methodName = "log";

        if ( !useLogger.isLoggable(Level.FINER) ) {
            return;
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "BEGIN STATE [ {0} ]", getHashName());

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

        if ( seedTable != null ) {
            seedTable.log(useLogger);
        }
        if ( partialTable != null ) {
            partialTable.log(useLogger);
        }
        if ( excludedTable != null ) {
            excludedTable.log(useLogger);
        }

        // Overall scan results.  Particular rules apply to both
        // of these values ...

        // ... class data is null pre-scan, is set to a partially completed
        // value by the internal scan phase, and is completed by the
        // external scan phase.
        if ( classTable != null ) {
            classTable.log(useLogger);
        }

        // ...external data is null pre-scan, and is set after the external
        // scan phase.
        if ( externalTable != null ) {
            externalTable.log(useLogger);
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "END STATE [ {0} ]", getHashName());
    }

    //

    public TargetsDeltaImpl subtract(AnnotationTargetsImpl_Targets initialTargets) {
        return new TargetsDeltaImpl(
            getFactory(),
            getAppName(), getModName(), getModCatName(),
            this, initialTargets);
    }

    //

    protected TargetCacheImpl_DataQueries queriesData;

    @Trivial
    public TargetCacheImpl_DataQueries getQueriesData() {
        return queriesData;
    }

    /**
     * Put queries data into these targets.
     * 
     * Queries data is only created if writes are enabled by the
     * application data, and only when cache query logging is enabled.
     * {@link TargetCache_Options#getLogQueries()}.
     *
     * @param appData Application data for which to create query data.
     */
    protected void putQueriesData(TargetCacheImpl_DataApp appData) {
        if ( !appData.shouldWrite("query data") ) {
            return;
        } else if ( !appData.getLogQueries() ) {
            return;
        } else {
            this.queriesData = appData.getApps().getQueriesForcing( getAppName(), getModFullName() );
        }
        
    }

    //

    protected void writeQuery(
        String className, String methodName,
        String title,
        ScanPolicy scanPolicy, QueryType queryType,
        String annotationClass, Collection<String> resultClasses) {

        if ( TargetCacheImpl_DataQueries.getLogQueries() ) {
            TargetCacheImpl_DataQueries.logQuery(
                className, methodName,
                title,
                scanPolicy.getValue(), queryType.getTag(),
                specificClassNames, annotationClass, resultClasses);
        }

        TargetCacheImpl_DataQueries useQueriesData = getQueriesData();
        if ( useQueriesData == null ) {
            return;
        } else {
            useQueriesData.writeQuery(
                className, methodName,
                title,
                scanPolicy.getValue(), queryType.getTag(),
                specificClassNames, annotationClass, resultClasses);
        }
    }

    protected void writeQuery(
        String className, String methodName,
        String title,
        int policies, QueryType queryType,
        String annotationClass, Collection<String> resultClasses) {

        if ( TargetCacheImpl_DataQueries.getLogQueries() ) {
            TargetCacheImpl_DataQueries.logQuery(
                className, methodName,
                title,
                policies, queryType.getTag(),
                specificClassNames, annotationClass, resultClasses);
        }

        TargetCacheImpl_DataQueries useQueriesData = getQueriesData();
        if ( useQueriesData == null ) {
            return;
        } else {
            useQueriesData.writeQuery(
                className, methodName,
                title,
                policies, queryType.getTag(),
                specificClassNames, annotationClass, resultClasses);
        }
    }

    protected void writeQuery(
        String className, String methodName,
        String title,
        String source, QueryType queryType,
        String annotationClass, Collection<String> resultClasses) {

        TargetCacheImpl_DataQueries useQueriesData = getQueriesData();
        if ( useQueriesData == null ) {
            return;

        } else {
            List<String> sources = new ArrayList<String>(1);
            sources.add(source);

            useQueriesData.writeQuery(
                className, methodName,
                title,
                sources, queryType.getTag(),
                specificClassNames, annotationClass, resultClasses);
        }
    }

    protected void writeQuery(
        String className, String methodName,
        String title,
        int policies, String source, QueryType queryType,
        String annotationClass, Collection<String> resultClasses) {

        if ( TargetCacheImpl_DataQueries.getLogQueries() ) {
            List<String> sources = new ArrayList<String>(1);
            sources.add(source);

            TargetCacheImpl_DataQueries.logQuery(
                className, methodName,
                title,
                policies, sources, queryType.getTag(),
                specificClassNames, annotationClass, resultClasses);
        }

        TargetCacheImpl_DataQueries useQueriesData = getQueriesData();
        if ( useQueriesData == null ) {
            return;
            
        } else {    
            List<String> sources = new ArrayList<String>(1);
            sources.add(source);

            useQueriesData.writeQuery(
                className, methodName,
                title,
                policies, sources, queryType.getTag(),
                specificClassNames, annotationClass, resultClasses);
        }
    }

    protected QueryType asQueryType(AnnotationCategory category) {
        if ( category == AnnotationCategory.PACKAGE ) {
            return QueryType.PACKAGE;
        } else if ( category == AnnotationCategory.CLASS ) {
            return QueryType.CLASS;
        } else if ( category == AnnotationCategory.FIELD ) {
            return QueryType.FIELD;
        } else if ( category == AnnotationCategory.METHOD ) {
            return QueryType.METHOD;
        } else {
            throw new IllegalArgumentException("Unknown annotation category [ " + category + " ]");
        }
    }

    //

    @Override
    public boolean getIsDetailEnabled() {
        return true;
    }

    @Override
    public void addClassSource(com.ibm.wsspi.anno.classsource.ClassSource classSource,
        com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy scanPolicy) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void scan(com.ibm.wsspi.anno.classsource.ClassSource_Aggregate classSource)
            throws com.ibm.wsspi.anno.targets.AnnotationTargets_Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void scan(com.ibm.wsspi.anno.classsource.ClassSource_Aggregate classSource, Set<String> useSpecificClassNames)
            throws com.ibm.wsspi.anno.targets.AnnotationTargets_Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void scan(com.ibm.wsspi.anno.classsource.ClassSource_Aggregate classSource, boolean greedy)
            throws com.ibm.wsspi.anno.targets.AnnotationTargets_Exception {
        throw new UnsupportedOperationException();
    }
}
