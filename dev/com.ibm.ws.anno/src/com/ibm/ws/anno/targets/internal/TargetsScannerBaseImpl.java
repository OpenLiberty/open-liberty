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

    public String getHashText() {
        return hashText;
    }

    //

    public TargetsScannerBaseImpl(
        AnnotationTargetsImpl_Targets targets,
        ClassSource_Aggregate rootClassSource) {

        super();

        String methodName = "<init>";
        
        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());

        this.targets = targets;

        this.factory = targets.getFactory();

        this.classNameInternMap = targets.getClassNameInternMap();
        this.fieldNameInternMap = targets.getFieldNameInternMap();
        this.methodSignatureInternMap = targets.getMethodSignatureInternMap();

        this.rootClassSource = rootClassSource;

        this.targetsDataMap = new HashMap<String, TargetsTableImpl>();

        this.resultBuckets = createResultBuckets();
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

    public AnnotationTargetsImpl_Targets getTargets() {
        return targets;
    }

    public AnnotationTargetsImpl_Factory getFactory() {
        return factory;
    }

    public UtilImpl_InternMap getClassNameInternMap() {
        return classNameInternMap;
    }

    public UtilImpl_InternMap getFieldNameInternMap() {
        return fieldNameInternMap;
    }

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

    protected TargetsTableImpl createTargetsData(ScanPolicy scanPolicy) {
        return new TargetsTableImpl( getFactory(),
                                    getClassNameInternMap(),
                                    getFieldNameInternMap(),
                                    getMethodSignatureInternMap(),
                                    scanPolicy.name() );
    }

    protected TargetsTableImpl createTargetsData(String classSourceName) {
        return new TargetsTableImpl( getFactory(),
                                    getClassNameInternMap(),
                                    getFieldNameInternMap(),
                                    getMethodSignatureInternMap(),
                                    classSourceName );
    }

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

    public String internClassName(String className) {
        return getClassNameInternMap().intern(className);
    }

    public String internClassName(String className, boolean doForce) {
        return getClassNameInternMap().intern(className, doForce);
    }

    public Set<String> internClassNames(Set<String> uninternedClassNames) {
        Set<String> internedClassNames = createIdentityStringSet(uninternedClassNames.size());
        for ( String uninternedClassName : uninternedClassNames ) {
            internedClassNames.add( internClassName(uninternedClassName) );
        }
        return internedClassNames;
    }

    //

    protected final ClassSource_Aggregate rootClassSource;

    public ClassSource_Aggregate getRootClassSource() {
        return rootClassSource;
    }

    public ClassSource_Options getScanOptions() {
        return getRootClassSource().getOptions();
    }

    public int getScanThreads() {
        return getScanOptions().getScanThreads();
    }

    public boolean isScanSingleThreaded() {
        return ( getScanThreads() == 1 );
    }

    // The single thread scan process should be used when there is only one
    // non-external source.  That reduces the processing cost considerably.

    public boolean isScanSingleSource() {
        boolean foundFirst = false;

        for ( ClassSource childClassSource : rootClassSource.getClassSources() ) {
            ScanPolicy childScanPolicy = rootClassSource.getScanPolicy(childClassSource);
            if ( childScanPolicy == ScanPolicy.EXTERNAL ) {
                continue;
            } else if ( foundFirst ) {
                return false;
            } else {
                foundFirst = true;
            }
        }

        return true;
    }

    public boolean getUseJandex() {
        return getScanOptions().getUseJandex();
    }

    //

    private void displayJandex() {
        String methodName = "displayJandex";
        String prefix = CLASS_NAME + "." + methodName + ": ";

        ClassSource_Aggregate useRootClassSource = getRootClassSource();
        String appName = useRootClassSource.getApplicationName();
        String modName = useRootClassSource.getModuleName();

        if ( !getUseJandex() ) {
            System.out.println("App [ " + appName + " ] [ " + modName + " ]: Jandex is not enabled");
            return;
        } else {
            System.out.println("App [ " + appName + " ] [ " + modName + " ]: Jandex is enabled");
        }

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

        System.out.println(
            "Jandex coverage for module [ " + modName + " ] of application [ " + appName + " ]:" +
            " [ " + Integer.toString(sourceCount) + " ] module locations were scanned for annotations." +
            " [ " + Integer.toString(jandexSourceCount) + " ] of the module locations had Jandex indexes." +
            " [ " + Integer.toString(classCount) + " ] module classes were processed." +
            " [ " + Integer.toString(jandexClassCount) + " ] of the module classes were processed directly from Jandex indexes.");

        String coverageMsg = nlsFormat("ANNO_JANDEX_USAGE", appName, modName, sourceCount, jandexSourceCount, classCount, jandexClassCount);
        logger.logp(Level.INFO, CLASS_NAME, methodName, coverageMsg);
    }

    //

    public TargetsTableImpl scanInternal(ClassSource classSource,
                                        Set<String> i_useResolvedClassNames,
                                        Set<String> i_useUnresolvedClassNames) {
        String methodName = "scanInternal";

        String classSourceName = classSource.getName();

        TargetsTableImpl targetsData = createTargetsData(classSourceName);

        createTargetsData(classSourceName);

        try {
            targetsData.scanInternal(
                classSource,
                TargetsVisitorClassImpl.DONT_RECORD_NEW_UNRESOLVED, i_useResolvedClassNames,
                TargetsVisitorClassImpl.DONT_RECORD_NEW_RESOLVED, i_useUnresolvedClassNames,
                TargetsVisitorClassImpl.SELECT_ALL_ANNOTATIONS ); // throws AnnotationTargets_Exception {

        } catch ( AnnotationTargets_Exception e ) {
            // CWWKC0044W
            logger.logp(Level.WARNING, CLASS_NAME, methodName,
                "[ {0} ] ANNO_TARGETS_SCAN_EXCEPTION [ {1} ]",
                new Object[] { getHashText(), e });
            logger.logp(Level.WARNING, CLASS_NAME, methodName, "Scan error", e);
        }

        targetsData.setName(classSourceName);
        targetsData.setStamp( classSource.getStamp() );

        displayJandex();

        return targetsData;
    }

    public TargetsTableImpl scanExternal(ClassSource classSource,
                                        Set<String> i_useResolvedClassNames,
                                        Set<String> i_useUnresolvedClassNames) {
        String methodName = "scanExternal";
        
        String classSourceName = classSource.getName();

        TargetsTableImpl targetsData = createTargetsData(classSourceName);

        try {
            targetsData.scanExternal(classSource, i_useResolvedClassNames, i_useUnresolvedClassNames);
            // throws AnnotationTargets_Exception {

        } catch ( AnnotationTargets_Exception e ) {
            // CWWKC0044W
            logger.logp(Level.WARNING, CLASS_NAME, methodName,
                    "[ {0} ] ANNO_TARGETS_SCAN_EXCEPTION [ {1} ]",
                    new Object[] { getHashText(), e });
            logger.logp(Level.WARNING, CLASS_NAME, methodName, "Scan error", e);
        }

        targetsData.setName(classSourceName);
        targetsData.setStamp( classSource.getStamp() );

        return targetsData;
    }

    //

    // Class source name -> class source targets data
    protected final Map<String, TargetsTableImpl> targetsDataMap;

    public Map<String, TargetsTableImpl> getTargetsDataMap() {
        return targetsDataMap;
    }

    public TargetsTableImpl getTargetsData(String classSourceName) {
        return getTargetsDataMap().get(classSourceName);
    }

    public void putTargetsData(String classSourceName, TargetsTableImpl targetsData) {
        getTargetsDataMap().put(classSourceName, targetsData);
    }

    public TargetsTableImpl getTargetsData(ClassSource classSource) {
        return getTargetsDataMap().get(classSource.getName());
    }

    public void putTargetsData(ClassSource classSource, TargetsTableImpl targetsData) {
        getTargetsDataMap().put(classSource.getName(), targetsData);
    }

    public TargetsTableImpl getTargetsData(ScanPolicy scanPolicy) {
        return getTargetsDataMap().get(scanPolicy.name());
    }

    public void putTargetsData(ScanPolicy scanPolicy, TargetsTableImpl targetsData) {
        getTargetsDataMap().put(scanPolicy.name(), targetsData);
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

    // for ( ScanPolicy scanPolicy : ScanPolicy.values() ) {
    //     useResultBuckets[ scanPolicy.ordinal() ] = createTargetsData();
    // }

    protected final TargetsTableImpl[] resultBuckets;

    public TargetsTableImpl[] createResultBuckets() {
        return new TargetsTableImpl[ ScanPolicy.values().length ];
    }

    public void putResultBuckets(TargetsTableImpl[] useResultBuckets) {
        for ( ScanPolicy scanPolicy : ScanPolicy.values() ) {
            resultBuckets[ scanPolicy.ordinal() ] = useResultBuckets[ scanPolicy.ordinal() ];
        }
    }

    public void putExternalResults(TargetsTableImpl externalResults) {
        resultBuckets[ ScanPolicy.EXTERNAL.ordinal() ] = externalResults;
    }

    public TargetsTableImpl[] getResultBuckets() {
        return resultBuckets;
    }

    public TargetsTableImpl getResultBucket(ScanPolicy scanPolicy) {
        return resultBuckets[ scanPolicy.ordinal() ];
    }

    public TargetsTableImpl getSeedBucket() {
        return getResultBucket(ScanPolicy.SEED);
    }

    public TargetsTableImpl getPartialBucket() {
        return getResultBucket(ScanPolicy.PARTIAL);
    }

    public TargetsTableImpl getExcludedBucket() {
        return getResultBucket(ScanPolicy.EXCLUDED);
    }

    public TargetsTableImpl getExternalBucket() {
        return getResultBucket(ScanPolicy.EXTERNAL);
    }

    //
    
    // Used by 'TargetsScannerImpl_Overall.validInternalResults'.
    //
    // Does not need to be synchronized.  No write is performed
    // on the merged annotations.

    protected void mergeAnnotations(TargetsTableImpl[] buckets) {
        Set<String> i_addedPackageNames = createIdentityStringSet();
        Set<String> i_addedClassNames = createIdentityStringSet();

        ClassSource_Aggregate useRootClassSource = getRootClassSource();

        for ( ClassSource classSource : useRootClassSource.getClassSources() ) {
            ScanPolicy scanPolicy = useRootClassSource.getScanPolicy(classSource);
            if ( scanPolicy == ScanPolicy.EXTERNAL ) {
                continue;
            }

            TargetsTableImpl targetsData = getTargetsData(classSource);
            if ( targetsData == null ) {
                continue; // Failed to scan the class source.
            }

            TargetsTableImpl bucket = buckets[ scanPolicy.ordinal() ];
            if ( bucket == null ) {
                bucket = buckets[ scanPolicy.ordinal() ] = createTargetsData(scanPolicy);
            }
            bucket.restrictedAdd(targetsData, i_addedPackageNames, i_addedClassNames);
        }

        for ( ScanPolicy scanPolicy : ScanPolicy.values() ) {
            if ( buckets[ scanPolicy.ordinal() ] == null ) {
                buckets[ scanPolicy.ordinal() ] = createTargetsData(scanPolicy);
            }
        }
    }

    // Used by 'TargetsScannerImpl_Overall.validInternalClasses'.
    //
    // Does not need to be synchronized.  This step occurs on a new not yet shared
    // class table.

    public void mergeClasses(TargetsTableClassesMultiImpl useClassTable) {

        Set<String> i_addedPackageNames = createIdentityStringSet();
        Set<String> i_addedClassNames = createIdentityStringSet();

        for ( ClassSource classSource : getRootClassSource().getClassSources() ) {
            TargetsTableImpl targetsData = getTargetsData(classSource);
            if ( targetsData == null ) {
                continue; // Failed to scan the class source.
            }

            TargetsTableClassesImpl targetsClassData = targetsData.getClassTable();

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
                             TargetsTableImpl targetsData) {

        Set<String> i_addedPackageNames = createIdentityStringSet();
        Set<String> i_addedClassNames = createIdentityStringSet();

        Set<String> i_newlyAddedPackageNames = createIdentityStringSet();
        Set<String> i_newlyAddedClassNames = createIdentityStringSet();

        TargetsTableClassesImpl targetsClassData = targetsData.getClassTable();

        useClassTable.restrictedAdd(targetsClassData,
                i_newlyAddedPackageNames, i_addedPackageNames,
                i_newlyAddedClassNames, i_addedClassNames);
    }
}
