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
package com.ibm.ws.anno.targets.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import com.ibm.websphere.ras.annotation.Trivial;

import com.ibm.ws.anno.service.internal.AnnotationServiceImpl_Logging;
import com.ibm.ws.anno.util.internal.UtilImpl_IdentityStringSet;
import com.ibm.ws.anno.util.internal.UtilImpl_InternMap;
import com.ibm.wsspi.anno.classsource.ClassSource;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.classsource.ClassSource_Options;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Exception;
import com.ibm.wsspi.anno.util.Util_InternMap;

public class TargetsScannerBaseImpl {
    private static final TraceComponent tc = Tr.register(TargetsScannerBaseImpl.class);

    private static String nlsFormat(String msgKey, Object... msgParms) {
        return Tr.formatMessage(tc, msgKey, msgParms);
    }

    protected static final Logger logger = AnnotationServiceImpl_Logging.ANNO_LOGGER;

    public static final String CLASS_NAME = TargetsScannerBaseImpl.class.getSimpleName();

    protected final String hashText;

    @Trivial
    public String getHashText() {
        return hashText;
    }

    //

    /**
     * Create a new targets scanner for annotation targets and aggregate
     * class source.
     *
     * Use the intern maps of the annotation targets as the intern maps
     * of the new scanner.
     *
     * @param targets The targets for which to create the scanner.
     * @param rootClassSource The aggregate class source for which to create
     *     the scanner.
     */
    public TargetsScannerBaseImpl(
        AnnotationTargetsImpl_Targets targets,
        ClassSource_Aggregate rootClassSource) {

        super();

        String methodName = "<init>";
        
        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());

        if ( targets == null ) {
            throw new IllegalArgumentException(getClass().getSimpleName() + ": " + this.hashText + " created with null targets");
        } else if ( rootClassSource == null ) {
            throw new IllegalArgumentException(getClass().getSimpleName() + ": " + this.hashText + " created with null class source");
        }

        this.targets = targets;

        this.factory = targets.getFactory();

        this.classNameInternMap = targets.getClassNameInternMap();
        this.fieldNameInternMap = targets.getFieldNameInternMap();
        this.methodSignatureInternMap = targets.getMethodSignatureInternMap();

        this.rootClassSource = rootClassSource;

        this.targetsTables = new HashMap<String, TargetsTableImpl>();

        this.resultTables = createResultTables();
        this.classTable = null;

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] on [ {1} ]",
                    new Object[] { this.hashText, rootClassSource.getHashText() });
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] for [ {1} ]",
                    new Object[] { this.hashText, targets.getHashText() });
        }
    }

    //

    protected final AnnotationTargetsImpl_Targets targets;

    protected final AnnotationTargetsImpl_Factory factory;

    protected final UtilImpl_InternMap classNameInternMap;
    protected final UtilImpl_InternMap fieldNameInternMap;
    protected final UtilImpl_InternMap methodSignatureInternMap;

    @Trivial
    public AnnotationTargetsImpl_Targets getTargets() {
        return targets;
    }

    @Trivial
    public AnnotationTargetsImpl_Factory getFactory() {
        return factory;
    }

    @Trivial
    public UtilImpl_InternMap getClassNameInternMap() {
        return classNameInternMap;
    }

    @Trivial
    public UtilImpl_InternMap getFieldNameInternMap() {
        return fieldNameInternMap;
    }

    @Trivial
    public UtilImpl_InternMap getMethodSignatureInternMap() {
        return methodSignatureInternMap;
    }

    //

    public UtilImpl_IdentityStringSet createIdentityStringSet() {
        return getFactory().getUtilFactory().createIdentityStringSet();
    }

    public Set<String> createIdentityStringSet(int size) {
        return getFactory().getUtilFactory().createIdentityStringSet(size);
    }

    public Set<String> createIdentityStringSet(Set<String> initialElements) {
        return getFactory().getUtilFactory().createIdentityStringSet(initialElements);
    }

    public UtilImpl_InternMap createInternMap(Util_InternMap.ValueType valueType, String mapName) {
        return getFactory().getUtilFactory().createInternMap(valueType, mapName);
    }

    //

    /**
     * Create a targets table for a scan policy.  Use the overall intern
     * maps as the intern maps of the new targets data.
     *
     * @param scanPolicy The scan policy for which to create the targets
     *     table.
     *
     * @return A new targets table.
     */
    protected TargetsTableImpl createTargetsTable(ScanPolicy scanPolicy) {
        // Use the intern maps of this scanner, which are the intern maps
        // of the annotations targets.
        TargetsTableImpl resultTable =
            new TargetsTableImpl( getFactory(),
                                  getClassNameInternMap(),
                                  getFieldNameInternMap(),
                                  getMethodSignatureInternMap(),
                                  scanPolicy.name() );
        // A stamp is not available for result tables.
        resultTable.setStamp(ClassSource.UNRECORDED_STAMP);
        return resultTable;
    }

    /**
     * Create a targets table for a class source.  Use the overall intern
     * maps as the intern maps of the new targets data.
     *
     * Set the name of the targets data using the name of the class source.
     * Set the stamp of the targets table using the stamp of the class source.
     *
     * @param classSource The class source for which to create the targets table.
     *
     * @return A new targets table.
     */
    protected TargetsTableImpl createTargetsTable(ClassSource classSource) {
        // Use the intern maps of this scanner, which are the intern maps
        // of the annotations targets.
        // Overridden in 'TargetsScannerOverallImpl' to conditionally
        // create the table with its own intern maps.
        TargetsTableImpl targetsTable = new TargetsTableImpl(
            getFactory(),
            getClassNameInternMap(),
            getFieldNameInternMap(),
            getMethodSignatureInternMap(),
            classSource.getName() );

        targetsTable.setStamp( classSource.getStamp() );

        return targetsTable;
    }

    /**
     * Create a new class table using the overall class intern map.
     *
     * @return A new class table.
     */
    public TargetsTableClassesMultiImpl createClassTable() {
        return new TargetsTableClassesMultiImpl( getFactory().getUtilFactory(), getClassNameInternMap() );
    }

    protected TargetsTableContainersImpl createContainerTable() {
        return new TargetsTableContainersImpl( getFactory() );
    }

    protected TargetsTableContainersImpl createContainerTable(ClassSource_Aggregate classSource) {
        TargetsTableContainersImpl useContainerTable = createContainerTable();
        useContainerTable.addNames(classSource);
        return useContainerTable;
    }

    //

    /**
     * Intern a class name using the top level class name intern map.
     * Add the class name if it is not already present.  Answer the already
     * interned value if an equal class name is already present.
     *
     * @param className The class name to intern.
     *
     * @return The interned class name.
     */
    public String internClassName(String className) {
        return getClassNameInternMap().intern(className);
    }

    /**
     * Intern a class name using the top level class name intern map.
     * According to the forcing parameter, add the class name if it is not
     * already present.  Answer the already interned value if an equal class
     * name is already present.
     *
     * @param className The class name to intern.
     * @param doForce Control parameter: If true, add the class name to the
     *     class name intern map if the class name is not already present.
     *     If false, do not add the class name.
     * @return The interned class name.  Null if forcing is turned off and
     *     the class name is not already present.
     */
    public String internClassName(String className, boolean doForce) {
        return getClassNameInternMap().intern(className, doForce);
    }

    // Used by 'TargetsScannerSpecificImpl.scan'.

    public Set<String> internClassNames(Set<String> uninternedClassNames) {
        Set<String> internedClassNames = createIdentityStringSet(uninternedClassNames.size());
        for ( String uninternedClassName : uninternedClassNames ) {
            internedClassNames.add( internClassName(uninternedClassName) );
        }
        return internedClassNames;
    }

    //

    protected final ClassSource_Aggregate rootClassSource;

    @Trivial
    public ClassSource_Aggregate getRootClassSource() {
        return rootClassSource;
    }

    public ClassSource_Options getScanOptions() {
        return getRootClassSource().getOptions();
    }

    public int getScanThreads() {
        return getScanOptions().getScanThreads();
    }

    /**
     * Tell if scanning is limited to using a single thread.
     *
     * See {@link #getScanOptions()}, {@link #getScanThreads()},
     * and {@link ClassSourceOptions#getScanThreads()}.
     *
     * See also {@link #isScanSingleSource}: Scanning will be
     * limited to a single thread if there is only one internal
     * class source.
     *
     * @return True or false telling if scanning is limited to using
     *     a single thread.
     */
    public boolean isScanSingleThreaded() {
        int useScanThreads = getScanThreads();
        return ( (useScanThreads == 0) || (useScanThreads == 1) );
    }

    /**
     * Tell if if there is zero or one internal class source.
     *
     * When there is no more than one internal class source, scanning
     * uses single thread processing, even if scanning is enabled to use
     * more than one thread.  This removes the unnecessary, substantial,
     * overhead of multi-thread scan processing.
     *
     * @return True or false tell if there is at most one internal class
     *     source.
     */
    public boolean isScanSingleSource() {
        boolean foundFirst = false;

        for ( ClassSource childClassSource : rootClassSource.getClassSources() ) {
            ScanPolicy childScanPolicy = rootClassSource.getScanPolicy(childClassSource);
            if ( childScanPolicy == ScanPolicy.EXTERNAL ) {
                continue; // Ignore the external class source
            } else if ( foundFirst ) {
                return false; // This is the second internal class source.
            } else {
                foundFirst = true; // This is the first internal class source.
            }
        }

        // This is *not* based on 'foundFirst': 'foundFirst' will be true
        // if there is exactly only internal class source.  'foundFirst' will
        // be false if there are no internal class sources.

        return true;
    }

    public boolean getUseJandex() {
        return getScanOptions().getUseJandex();
    }

    //

    private void displayJandexCoverage() {
        String methodName = "displayJandexCoverage";

        if ( !getUseJandex() ) {
            return;
        }

        ClassSource_Aggregate useRootClassSource = getRootClassSource();
        String appName = useRootClassSource.getApplicationName();
        String modName = useRootClassSource.getModuleName();

        int sourceCount = 0;
        int jandexSourceCount = 0;

        int classCount = 0;
        int jandexClassCount = 0;
        
        for ( ClassSource nextClassSource : useRootClassSource.getClassSources() ) {
            ScanPolicy nextScanPolicy = useRootClassSource.getScanPolicy(nextClassSource);
            if ( nextScanPolicy == ScanPolicy.EXTERNAL ) {
                continue;
            }

            sourceCount++;

            int nextClassCount = nextClassSource.getProcessCount();
            classCount += nextClassCount;

            if ( nextClassSource.isProcessedUsingJandex() ) {
                jandexSourceCount++;
                jandexClassCount += nextClassCount;
            }
        }

        // System.out.println(
        //     "Jandex coverage for module [ " + modName + " ] of application [ " + appName + " ]:" +
        //     " [ " + Integer.toString(sourceCount) + " ] module locations were scanned for annotations." +
        //     " [ " + Integer.toString(jandexSourceCount) + " ] of the module locations had Jandex indexes." +
        //     " [ " + Integer.toString(classCount) + " ] module classes were processed." +
        //     " [ " + Integer.toString(jandexClassCount) + " ] of the module classes were processed directly from Jandex indexes.");

        String coverageMsg = nlsFormat("ANNO_JANDEX_USAGE",
                                       appName, modName,
                                       sourceCount, jandexSourceCount,
                                       classCount, jandexClassCount);

        logger.logp(Level.INFO, CLASS_NAME, methodName, coverageMsg);
    }

    //

    @Trivial
    public TargetsTableImpl scanInternal(ClassSource classSource,
                                        Set<String> i_useResolvedClassNames,
                                        Set<String> i_useUnresolvedClassNames) {
        String methodName = "scanInternal";

        Object[] logParms;
        if ( logger.isLoggable(Level.FINER) ) {
            logParms = new Object[] { getHashText(), classSource.getName() };
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Class source [ {1} ]", logParms);
            if ( i_useResolvedClassNames != null ) {
                logParms[1] = Integer.valueOf(i_useResolvedClassNames.size());
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Initial resolved [ {1} ]", logParms);
            }
            if ( i_useUnresolvedClassNames != null ) {
                logParms[1] = Integer.valueOf(i_useUnresolvedClassNames.size());
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Initial unresolved [ {1} ]", logParms);
            }
        } else {
            logParms = null;
        }

        TargetsTableImpl targetsTable = createTargetsTable(classSource);

        try {
            targetsTable.scanInternal(
                classSource,
                TargetsVisitorClassImpl.DONT_RECORD_NEW_RESOLVED, i_useResolvedClassNames,
                TargetsVisitorClassImpl.DONT_RECORD_NEW_UNRESOLVED, i_useUnresolvedClassNames,
                TargetsVisitorClassImpl.SELECT_ALL_ANNOTATIONS ); // throws AnnotationTargets_Exception

        } catch ( AnnotationTargets_Exception e ) {
            // CWWKC0044W
            logger.logp(Level.WARNING, CLASS_NAME, methodName,
                "[ {0} ] ANNO_TARGETS_SCAN_EXCEPTION [ {1} ]",
                new Object[] { getHashText(), e });
            logger.logp(Level.WARNING, CLASS_NAME, methodName, "Scan error", e);
        }

        if ( logParms != null ) {
            if ( i_useResolvedClassNames != null ) {
                logParms[1] = Integer.valueOf(i_useResolvedClassNames.size());
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Final resolved [ {1} ]", logParms);
            }
            if ( i_useUnresolvedClassNames != null ) {
                logParms[1] = Integer.valueOf(i_useUnresolvedClassNames.size());
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Final unresolved [ {1} ]", logParms);
            }
        }

        displayJandexCoverage();

        return targetsTable;
    }

    @Trivial
    public TargetsTableImpl scanExternal(ClassSource classSource,
                                         Set<String> i_useResolvedClassNames,
                                         Set<String> i_useUnresolvedClassNames) {
        String methodName = "scanExternal";

        Object[] logParms;
        if ( logger.isLoggable(Level.FINER) ) {
            logParms = new Object[] { getHashText(), classSource.getName() };
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Class source [ {1} ]", logParms);
            if ( i_useResolvedClassNames != null ) {
                logParms[1] = Integer.valueOf(i_useResolvedClassNames.size());
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Initial resolved [ {1} ]", logParms);
            }
            if ( i_useUnresolvedClassNames != null ) {
                logParms[1] = Integer.valueOf(i_useUnresolvedClassNames.size());
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Initial unresolved [ {1} ]", logParms);
            }
        } else {
            logParms = null;
        }
        
        TargetsTableImpl targetsTable = createTargetsTable(classSource);

        try {
            targetsTable.scanExternal(classSource, i_useResolvedClassNames, i_useUnresolvedClassNames);
            // throws AnnotationTargets_Exception {

        } catch ( AnnotationTargets_Exception e ) {
            // CWWKC0044W
            logger.logp(Level.WARNING, CLASS_NAME, methodName,
                    "[ {0} ] ANNO_TARGETS_SCAN_EXCEPTION [ {1} ]",
                    new Object[] { getHashText(), e });
            logger.logp(Level.WARNING, CLASS_NAME, methodName, "Scan error", e);
        }

        if ( logParms != null ) {
            if ( i_useResolvedClassNames != null ) {
                logParms[1] = Integer.valueOf(i_useResolvedClassNames.size());
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Final resolved [ {1} ]", logParms);
            }
            if ( i_useUnresolvedClassNames != null ) {
                logParms[1] = Integer.valueOf(i_useUnresolvedClassNames.size());
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Final unresolved [ {1} ]", logParms);
            }
        }

        return targetsTable;
    }

    //

    // Class source name | scan policy -> classes + annotation targets

    protected final Map<String, TargetsTableImpl> targetsTables;

    @Trivial
    public Map<String, TargetsTableImpl> getTargetsTables() {
        return targetsTables;
    }

    public TargetsTableImpl getTargetsTable(String classSourceName) {
        return getTargetsTables().get(classSourceName);
    }

    public void putTargetsTable(String classSourceName, TargetsTableImpl targetsTable) {
        getTargetsTables().put(classSourceName, targetsTable);
    }

    public TargetsTableImpl getTargetsTable(ClassSource classSource) {
        return getTargetsTables().get(classSource.getName());
    }

    public void putTargetsTable(ClassSource classSource, TargetsTableImpl targetsTable) {
        getTargetsTables().put(classSource.getName(), targetsTable);
    }

    //

    protected TargetsTableClassesMultiImpl classTable;

    public void setClassTable(TargetsTableClassesMultiImpl classTable) {
        this.classTable = classTable;
    }

    public TargetsTableClassesMultiImpl getClassTable() {
        return classTable;
    }

    //

    protected final TargetsTableImpl[] resultTables;

    public TargetsTableImpl[] createResultTables() {
        return new TargetsTableImpl[ ScanPolicy.values().length ];
    }

    public void putResultTables(TargetsTableImpl[] useResultTables) {
        for ( ScanPolicy scanPolicy : ScanPolicy.values() ) {
            resultTables[ scanPolicy.ordinal() ] = useResultTables[ scanPolicy.ordinal() ];
        }
    }

    public void putExternalResults(TargetsTableImpl externalResults) {
        resultTables[ ScanPolicy.EXTERNAL.ordinal() ] = externalResults;
    }

    public TargetsTableImpl[] getResultTables() {
        return resultTables;
    }

    public TargetsTableImpl getResultTable(ScanPolicy scanPolicy) {
        return resultTables[ scanPolicy.ordinal() ];
    }

    public TargetsTableImpl getSeedTable() {
        return getResultTable(ScanPolicy.SEED);
    }

    public TargetsTableImpl getPartialTable() {
        return getResultTable(ScanPolicy.PARTIAL);
    }

    public TargetsTableImpl getExcludedTable() {
        return getResultTable(ScanPolicy.EXCLUDED);
    }

    public TargetsTableImpl getExternalTable() {
        return getResultTable(ScanPolicy.EXTERNAL);
    }

    //
    
    // Used by 'TargetsScannerImpl_Overall.validInternalResults'.
    //
    // Does not need to be synchronized.  No write is performed
    // on the merged annotations.

    protected void mergeInternalResults(TargetsTableImpl[] resultTables) {
        Set<String> i_addedPackageNames = createIdentityStringSet();
        Set<String> i_addedClassNames = createIdentityStringSet();

        ClassSource_Aggregate useRootClassSource = getRootClassSource();

        for ( ClassSource classSource : useRootClassSource.getClassSources() ) {
            ScanPolicy scanPolicy = useRootClassSource.getScanPolicy(classSource);
            if ( scanPolicy == ScanPolicy.EXTERNAL ) {
                continue;
            }

            TargetsTableImpl targetsTable = getTargetsTable(classSource);
            if ( targetsTable == null ) {
                continue; // Failed to scan the class source.
            }

            TargetsTableImpl resultTable = resultTables[ scanPolicy.ordinal() ];
            if ( resultTable == null ) {
                resultTable = ( resultTables[ scanPolicy.ordinal() ] = createTargetsTable(scanPolicy) );
            }
            resultTable.restrictedAdd(targetsTable, i_addedPackageNames, i_addedClassNames);
        }

        for ( ScanPolicy scanPolicy : ScanPolicy.values() ) {
            if ( resultTables[ scanPolicy.ordinal() ] == null ) {
                resultTables[ scanPolicy.ordinal() ] = createTargetsTable(scanPolicy);
            }
        }
    }

    // Used by 'TargetsScannerImpl_Overall.validInternalClasses'.

    public void mergeClasses(TargetsTableClassesMultiImpl useClassTable) {
        String methodName = "mergeClasses";

        Set<String> i_addedPackageNames = createIdentityStringSet();
        Set<String> i_addedClassNames = createIdentityStringSet();

        for ( ClassSource classSource : getRootClassSource().getClassSources() ) {
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] on [ {1} ]",
                    new Object[] { this.hashText, classSource.getHashText() });
            }

            TargetsTableImpl targetsTable = getTargetsTable(classSource);
            if ( targetsTable == null ) {
                if ( logger.isLoggable(Level.FINER) ) {
                    logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] on [ {1} ]: Null Data",
                        new Object[] { this.hashText, classSource.getHashText() });
                }
                continue; // Failed to scan the class source.
            }

            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] on [ {1} ]: Merging",
                new Object[] { this.hashText, classSource.getHashText() });

            TargetsTableClassesImpl targetsClassData = targetsTable.getClassTable();

            Set<String> i_newlyAddedPackageNames = createIdentityStringSet();
            Set<String> i_newlyAddedClassNames = createIdentityStringSet();

            useClassTable.restrictedAdd(targetsClassData,
                    i_newlyAddedPackageNames, i_addedPackageNames,
                    i_newlyAddedClassNames, i_addedClassNames);
        }
    }

    // Used by 'TargetsScannerImpl_Overall.validExternal'.
    //
    // Must be synchronized with writes of the class table.
    // If not synchronized, a collision can occurs between:
    //
    // (1) The write of class references generated by internal results:
    //
    // at TargetCacheImpl_ModData.scheduleWrite(TargetCacheImpl_ModData.java:568)
    // at TargetCacheImpl_ModData.writeClassRefs(TargetCacheImpl_ModData.java:535)
    // at TargetsScannerImpl_Overall.validInternalClasses(TargetsScannerImpl_Overall.java:1085)
    // at TargetsScannerImpl_Overall.validInternal(TargetsScannerImpl_Overall.java:1103)
    //
    // (2) The merge of classes generated by external results:
    //
    // at TargetsClassTableMultiImpl.markClassSourceRecord(TargetsClassTableMultiImpl.java:264)
    // at TargetsClassTableMultiImpl.i_addClassNames(TargetsClassTableMultiImpl.java:376)
    // at TargetsClassTableMultiImpl.restrictedAdd(TargetsClassTableMultiImpl.java:330)
    // at TargetsScannerImpl.mergeClasses(TargetsScannerImpl.java:415)
    // at TargetsScannerImpl_Overall.validExternal(TargetsScannerImpl_Overall.java:1154)
    // at AnnotationTargetsImpl_Targets.ensureExternalResults(AnnotationTargetsImpl_Targets.java:633)
    //
    // What happens is that the generation of the internal results creates the initial,
    // incomplete, class information, which is cached, then the generation of the external
    // results completes the class information.  The step of completing the class information
    // updates the class table which is stored into the cache.

    public void mergeClasses(TargetsTableClassesMultiImpl useClassTable,
                             TargetsTableImpl targetsTable) {

        Set<String> i_addedPackageNames = createIdentityStringSet();
        Set<String> i_addedClassNames = createIdentityStringSet();

        Set<String> i_newlyAddedPackageNames = createIdentityStringSet();
        Set<String> i_newlyAddedClassNames = createIdentityStringSet();

        TargetsTableClassesImpl targetsClassData = targetsTable.getClassTable();

        useClassTable.restrictedAdd(targetsClassData,
            i_newlyAddedPackageNames, i_addedPackageNames,
            i_newlyAddedClassNames, i_addedClassNames);
    }
}
