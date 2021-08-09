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

package com.ibm.ws.annocache.test.scan;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import org.junit.After;
import org.junit.Before;

import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_Aggregate;
import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_Factory;
import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_Options;
import com.ibm.ws.annocache.classsource.specification.internal.ClassSourceImpl_Specification;
import com.ibm.ws.annocache.info.internal.InfoStoreFactoryImpl;
import com.ibm.ws.annocache.info.internal.InfoStoreImpl;
import com.ibm.ws.annocache.service.internal.AnnotationCacheServiceImpl_Service;
import com.ibm.ws.annocache.targets.cache.internal.TargetCacheImpl_Factory;
import com.ibm.ws.annocache.targets.cache.internal.TargetCacheImpl_Options;
import com.ibm.ws.annocache.targets.delta.internal.TargetsDeltaImpl;
import com.ibm.ws.annocache.targets.internal.AnnotationTargetsImpl_Factory;
import com.ibm.ws.annocache.targets.internal.AnnotationTargetsImpl_Targets;
import com.ibm.ws.annocache.targets.internal.TargetsTableImpl;
import com.ibm.ws.annocache.test.utils.TestLocalization;
import com.ibm.ws.annocache.test.utils.TestUtils;
import com.ibm.ws.annocache.util.internal.UtilImpl_Factory;
import com.ibm.wsspi.annocache.classsource.ClassSource;
import com.ibm.wsspi.annocache.classsource.ClassSource_Exception;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.annocache.info.ClassInfo;
import com.ibm.wsspi.annocache.info.InfoStoreException;
import com.ibm.wsspi.annocache.info.PackageInfo;
import com.ibm.wsspi.annocache.targets.AnnotationTargets_Exception;
import com.ibm.wsspi.annocache.targets.AnnotationTargets_Fault;

public class Test_Base_State {

    //

    protected final PrintWriter writer = new PrintWriter(System.out, true);

    public PrintWriter getWriter() {
        return writer;
    }

    public static long getTime() {
        return System.nanoTime();
    }

    public static final int NS_IN_MS = 1000000;

    public long displayStamp(String banner) {
        long endTime = getTime();

        getWriter().println(
            banner +
            " [ ****** ]" +
            "[ " + (endTime / NS_IN_MS) + " (ms) ]");

        return endTime;
    }

    public long displayStamp(long startTime, String banner) {
        long endTime = getTime();

        getWriter().println(
            banner +
            " [ " + (startTime / NS_IN_MS) + " (ms) ]" +
            " [ " + (endTime / NS_IN_MS) + " (ms) ]" +
            " [ " + ((endTime - startTime) / NS_IN_MS) + " (ms) ]");

        return endTime;
    }

    public void println(String text) {
        getWriter().println(text);
    }

    //

    @Before
    public void setUpBase() throws Exception {
        setUpSuite( getBaseCase() ); // 'setUpSuite' throws Exception
    }

    @After
    public void tearDown() throws Exception {
        tearDownSuiteStores();
    }

    //

    public void setUpSuite(TestOptions_SuiteCase suiteCase) throws Exception {
        TestOptions_SuiteCase[] prereqs = suiteCase.getPrereqs();
        if ( prereqs != null ) {
            for ( TestOptions_SuiteCase prereqCase : prereqs ) {
                setUpSuite(prereqCase); // throws Exception
            }
        }

        recordSuite(suiteCase); // throws Exception
    }

    public void recordSuite(TestOptions_SuiteCase suiteCase) throws Exception {
        if ( isRecorded(suiteCase) ) {
            return;
        }

        setOptions( getSuiteOptions(suiteCase) );
        setUpCore(); // throws Exception

        recordState(suiteCase);

        tearDownCore();
        setOptions(null);
    }

    public void setUpCore() throws Exception {
        setUpFactories(); // throws Exception
        setUpSpecification();
        setUpClassSource(); // throws Exception
        setUpTargets(); // throws Exception
        setUpInfoStore(); // throws Exception
    }

    public void tearDownCore() throws Exception {
        tearDownInfoStore(); // throws Exception
        tearDownTargets();
        tearDownClassSource();
        tearDownSpecification();
        tearDownFactories();
    }

    public boolean isRecorded(TestOptions_SuiteCase suiteCase) {
        return ( getSuiteSource(suiteCase) != null );
    }

    public void recordState(TestOptions_SuiteCase suiteCase) {
        setSuiteSource( suiteCase, getClassSource() );
        setSuiteTargets( suiteCase, getTargets() );
        setSuiteStore( suiteCase, getInfoStore() );

        Test_Base_Result scanResult = createScanResult();
        scanResult.setScanTime( getScanTime() );
        scanResult.setCounts( getClassSource(), getTargets() );
        scanResult.setTimingData( getClassSource().getTimingData().clone() );

        setSuiteResults( suiteCase, scanResult);
    }

    public void tearDownSuiteStores() throws InfoStoreException {
        InfoStoreException firstException = null;

        for ( TestOptions_SuiteCase suiteCase : getSuiteStores().keySet() ) {
            InfoStoreImpl useInfoStore = getSuiteStore(suiteCase);
            if ( useInfoStore != null ) {
                try {
                    useInfoStore.close(); // throws InfoStoreException
                } catch ( InfoStoreException e ) {
                    if ( firstException != null ) {
                        firstException = e;
                    } else {
                        System.out.println("Ignoring exception [ " + e + " ]");
                        e.printStackTrace(System.out);
                    }
                }
            }
        }

        if ( firstException != null ) {
            throw firstException;
        }
    }

    //

    public interface SpecificationFactory {
        ClassSourceImpl_Specification createSpecification(ClassSourceImpl_Factory factory);
    }

    public interface SourceFactory {
        ClassSourceImpl_Aggregate createSource(
            ClassSourceImpl_Factory factory,
            ClassSourceImpl_Options options) throws ClassSource_Exception;
    }

    public Test_Base_State(
        String testClassName,
        String appName, String appSimpleName,
        String modName, String modSimpleName,
        ClassLoader rootClassLoader,
        SpecificationFactory specificationFactory,
        SourceFactory sourceFactory) {

        this.testClassName = testClassName;

        this.appName = appName;
        this.appSimpleName = appSimpleName;
        this.modName = modName;
        this.modSimpleName = modSimpleName;

        this.fullName = this.appSimpleName + this.modSimpleName;

        this.rootClassLoader = rootClassLoader;

        this.specificationFactory = specificationFactory;
        this.sourceFactory = sourceFactory;
    }

    private final String testClassName;
    
    public String getTestClassName() {
        return testClassName;
    }

    private final String appName;
    private final String appSimpleName;
    private final String modName;
    private final String modSimpleName;

    public String getAppName() {
        return appName;
    }

    public String getAppSimpleName() {
        return appSimpleName;
    }

    public String getModName() {
        return modName;
    }

    public String getModSimpleName() {
        return modSimpleName;
    }

    protected final String fullName;

    public String getFullName() {
        return fullName;
    }

    public String getDescription(TestOptions_SuiteCase suiteCase) {
        return "[ " + suiteCase + " ] [ " + getFullName() + " ]";
    }

    //
    
    private final ClassLoader rootClassLoader;
    
    public ClassLoader getRootClassLoader() {
        return rootClassLoader;
    }

    //

    private final SpecificationFactory specificationFactory;
    
    public SpecificationFactory getSpecificationFactory() {
        return specificationFactory;
    }

    private ClassSourceImpl_Specification classSourceSpecification;

    public void setUpSpecification() {
        classSourceSpecification = getSpecificationFactory().createSpecification( getClassSourceFactory() );
    }

    public void tearDownSpecification() {
        classSourceSpecification = null;
    }

    public ClassSourceImpl_Specification getClassSourceSpecification() {
        return classSourceSpecification;
    }

    private final SourceFactory sourceFactory;
    
    public SourceFactory getSourceFactory() {
        return sourceFactory;
    }

    public ClassSourceImpl_Aggregate createClassSource() throws ClassSource_Exception {
        ClassSourceImpl_Options useOptions = createRawClassSourceOptions();

        ClassSourceImpl_Specification useSpecification = getClassSourceSpecification();
        if ( useSpecification != null ) {
            return useSpecification.createRootClassSource(useOptions);
            // throws ClassSource_Exception

        } else {
            return getSourceFactory().createSource( getClassSourceFactory(), useOptions);
            // 'createSource' throws ClassSource_Exception
        }
    }

    //

    public TestOptions testOptions;

    public void setOptions(TestOptions testOptions) {
        this.testOptions = testOptions;
    }

    public TestOptions getOptions() {
        return testOptions;
    }

    public TestOptions_Scan getScanOptions() {
        return getOptions().scanOptions;
    }

    public TestOptions_Cache getCacheOptions() {
        return getOptions().cacheOptions;
    }

    public void displayOptions(TestOptions_SuiteCase suiteCase) {
        TestOptions useOptions = getSuiteOptions(suiteCase);

        println("Test Options:");
        println("  Title       [ " + useOptions.getTitle() + " ]");
        println("  Description [ " + useOptions.getDescription() + " ]");

        TestOptions_Scan useScanOptions = useOptions.getScanOptions();

        println("Scan Options:");
        println("  Use Jandex      [ " + Boolean.valueOf(useScanOptions.useJandex) + " ]");
        println("  Scan Threads    [ " + Integer.valueOf(useScanOptions.scanThreads) + " ]");        

        TestOptions_Cache useCacheOptions = useOptions.getCacheOptions();

        if ( useCacheOptions == null ) {
            println("Cache Options:");
            println("  Enabled [ false ]");
        } else {
            println("Cache Options:");
            println("  Enabled [ true ]");
            println("  Storage [ " + useCacheOptions.storageSuffix + " ]");
            println("  Clean Storage [ " + Boolean.valueOf(useCacheOptions.cleanStorage) + " ]");
            println("  Always Valid [ " + Boolean.valueOf(useCacheOptions.alwaysValid) + " ]");
            println("  Read Only [ " + Boolean.valueOf(useCacheOptions.readOnly) + " ]");
            println("  Write Threads [ " + Integer.valueOf(useCacheOptions.writeThreads) + " ]");
            println("  Use Jandex Format [ " + Boolean.valueOf(useCacheOptions.useJandexFormat) + " ]");
            println("  Use Binary Format [ " + Boolean.valueOf(useCacheOptions.useBinaryFormat) + " ]");
        }
    }

    //

    public void setUpFactories() throws Exception {
        utilFactory = new UtilImpl_Factory();
        classSourceFactory = new ClassSourceImpl_Factory(utilFactory);

        cacheFactory = new TargetCacheImpl_Factory(createRawCacheOptions());
      
        targetsFactory = new AnnotationTargetsImpl_Factory(utilFactory, classSourceFactory, cacheFactory);

        infoStoreFactory = new InfoStoreFactoryImpl(utilFactory);
        targetsService = new AnnotationCacheServiceImpl_Service(cacheFactory, utilFactory, infoStoreFactory, classSourceFactory, targetsFactory);
    }

    public void tearDownFactories() {
        utilFactory = null;
        classSourceFactory = null;
        cacheFactory = null;
        targetsFactory = null;
        infoStoreFactory = null;

        targetsService = null;
    }

    //

    protected AnnotationCacheServiceImpl_Service targetsService;

    protected UtilImpl_Factory utilFactory;
    protected ClassSourceImpl_Factory classSourceFactory;

    protected TargetCacheImpl_Factory cacheFactory;
    protected AnnotationTargetsImpl_Factory targetsFactory;
    protected InfoStoreFactoryImpl infoStoreFactory;

    public AnnotationCacheServiceImpl_Service getAnnotationService() {
        return targetsService;
    }

    public UtilImpl_Factory getUtilFactory() {
        return utilFactory;
    }

    public ClassSourceImpl_Factory getClassSourceFactory() {
        return classSourceFactory;
    }

    public ClassSourceImpl_Options createRawClassSourceOptions() {
        TestOptions_Scan testScanOptions = getScanOptions();

        ClassSourceImpl_Options rawScanOptions = getClassSourceFactory().createOptions();

        rawScanOptions.setUseJandex(testScanOptions.useJandex);
        rawScanOptions.setScanThreads(testScanOptions.scanThreads);
        return rawScanOptions;
    }

    //

    public TargetCacheImpl_Factory getCacheFactory() {
        return cacheFactory;
    }

    public TargetCacheImpl_Options createRawCacheOptions() {
        TestOptions_Cache testCacheOptions = getCacheOptions();

        TargetCacheImpl_Options rawCacheOptions = TargetCacheImpl_Factory.createOptionsFromDefaults();

        if ( testCacheOptions == null ) {
            rawCacheOptions.setDisabled(true);

        } else {
            rawCacheOptions.setDisabled(false);

            String storageForCase = TestLocalization.putIntoStorage(
                getTestClassName(),
                testCacheOptions.storageSuffix );

            if ( testCacheOptions.cleanStorage ) {
                TestUtils.prepareDirectory(storageForCase);
            }

            rawCacheOptions.setDir(storageForCase);

            rawCacheOptions.setReadOnly(testCacheOptions.readOnly);
            rawCacheOptions.setAlwaysValid(testCacheOptions.alwaysValid);
            rawCacheOptions.setWriteThreads(testCacheOptions.writeThreads);
            rawCacheOptions.setUseJandexFormat(testCacheOptions.useJandexFormat);
            rawCacheOptions.setUseBinaryFormat(testCacheOptions.useBinaryFormat);
        }

        return rawCacheOptions;
    }

    public AnnotationTargetsImpl_Factory getTargetsFactory() {
        return targetsFactory;
    }

    public AnnotationTargetsImpl_Targets createTargets()
        throws AnnotationTargets_Exception {
        return getTargetsFactory().createTargets();
        // 'createTargets' throws AnnotationTargets_Exception
    }

    //

    public InfoStoreFactoryImpl getInfoStoreFactory() {
        return infoStoreFactory;
    }

    protected InfoStoreImpl createInfoStore(ClassSourceImpl_Aggregate rootClassSource)
        throws InfoStoreException {
        return getInfoStoreFactory().createInfoStore(rootClassSource);
        // 'createInfoStore' throws InfoStoreException
    }

    //

    protected ClassSourceImpl_Aggregate classSource;

    public void setUpClassSource() throws Exception{
        classSource = createClassSource(); // throws ClassSource_Exception
    }

    public void tearDownClassSource() {
        classSource = null;
    }

    public ClassSourceImpl_Aggregate getClassSource() {
        return classSource;
    }

    public void displaySource(TestOptions_SuiteCase suiteCase) {
        ClassSourceImpl_Aggregate useClassSource = getSuiteSource(suiteCase);

        println("Class source [ " + useClassSource.getApplicationName() + " ]" +
                " [ " + useClassSource.getModuleName() + " ]");

        for ( ClassSource componentClassSource : useClassSource.getClassSources() ) {
            ScanPolicy componentScanPolicy = useClassSource.getScanPolicy(componentClassSource);
            String componentName = componentClassSource.getName();
            println("  Component [ " + componentName + " ]" +
                    " [ " + componentScanPolicy + " ]" +
                    " [ " + componentClassSource + " ]");
        }
    }

    //

    protected AnnotationTargetsImpl_Targets targets;
    protected long scanTime;

    public AnnotationTargetsImpl_Targets getTargets() {
        return targets;
    }

    public long getScanTime() {
        return scanTime;
    }

    //

    public void setUpTargets() throws Exception {
        long startTime = displayStamp("Begin Target Scan [ " + getFullName() + " ]");

        // Handle any initial exception by allowing the exception to be thrown, failing
        // the test.  A failure during the initial scan makes any subsequent validations
        // meaningless.

        targets = createTargets(); // throws AnnotationTargets_Exception

        // Scan installs the class source but leaves the scan to be performed on demand.

        ClassSourceImpl_Aggregate useClassSource = getClassSource();

        targets.scan(useClassSource); // throws AnnotationTargets_Exception

        // These force scans; 'seed', 'partial', and 'excluded' obtain internal results.
        // 'external obtains external results.
        //
        // 'partial' and 'excluded' are current redundant with 'seed', but are present
        // in case the internals change to more incrementally obtain the scan results.

        @SuppressWarnings("unused")
        TargetsTableImpl seedTable = targets.getSeedTable();
        @SuppressWarnings("unused")
        TargetsTableImpl partialTable = targets.getPartialTable();
        @SuppressWarnings("unused")
        TargetsTableImpl excludedTable = targets.getExcludedTable();
        @SuppressWarnings("unused")
        TargetsTableImpl externalTable = targets.getExternalTable();

        long endTime = displayStamp("End Target Scan [ " + getFullName() + " ]");

        scanTime = endTime - startTime;

        useClassSource.setTimingData();
    }

    public void tearDownTargets() {
        targets = null;
    }

    //

    protected InfoStoreImpl infoStore;

    public void setUpInfoStore() throws Exception {
        // Simply fail with a thrown exception if the info store could not be opened.
        // That is another unrecoverable case.

        println("Created info store ...");

        infoStore = createInfoStore(classSource); // throws InfoStoreException

        println("Created info store ... done");
    }

    public void tearDownInfoStore() throws Exception {
        infoStore = null;
    }

    public InfoStoreImpl getInfoStore() {
        return infoStore;
    }

    //

    public String getDataPath() {
        return TestLocalization.getDataPath();
    }

    public String getStoragePath() {
        return TestLocalization.getStoragePath();
    }

    public void displayLocationParameters() {
        println("Location Parameters:");

        String workPath = System.getProperty("user.dir");
        String fullWorkPath = new File(workPath).getAbsolutePath();
        println("  Work path [ $user.dir ]: " + workPath + " [ " + fullWorkPath + " ]");

        String tempPath = System.getProperty("java.io.tmpdir");
        String fullTempPath = new File(tempPath).getAbsolutePath();
        println("  Temp path [ $java.io.tmpdir ]: " + tempPath + " [ " + fullTempPath + " ]");

        String projectPath = TestLocalization.getEclipseProjectPath();
        String fullProjectPath = new File(projectPath).getAbsolutePath();
        println("  Project path: " + projectPath + " [ " + fullProjectPath + " ]"); 

        String dataPath = TestLocalization.getDataPath();
        String fullDataPath = new File(dataPath).getAbsolutePath();
        println("  Data path: " + dataPath + " [ " + fullDataPath + " ]"); 

        String storagePath = TestLocalization.getStoragePath();
        String fullStoragePath = new File(storagePath).getAbsolutePath();
        println("  Storage path: " + storagePath + " [ " + fullStoragePath + " ]");
    }

    //

    // displayLocationParameters();
    // displayOptions();
    // scanResult.display( getWriter() );

    public void runSuiteTest(TestOptions_SuiteCase suiteCase) throws Exception {
        boolean isBaseCase = ( suiteCase == getBaseCase() );
        String description = getDescription(suiteCase);
        
        println("Running suite case " + description + " ... base [ " + Boolean.valueOf(isBaseCase) + " ] ...");

        System.gc();

        displayLocationParameters();
        displayOptions(suiteCase);

        if ( !isBaseCase ) {
            setUpSuite(suiteCase); // throws Exception
        }

        displaySource(suiteCase);

        validateTargets(suiteCase);
        validateInfoStore(suiteCase); // throws Exception
        validateDifference(suiteCase);

        String failureMessage = null;

        Test_Base_Result useResults = getSuiteResults(suiteCase);
        if ( useResults.getTestFailed() ) {
            failureMessage = "Base scan failure detected";
        } else if ( !useResults.difference.isNull(suiteCase.getIgnoreMissingPackages(), suiteCase.getIgnoreMissingInterfaces()) ) {
            failureMessage = "Scan difference detected";
        }

        println("Running suite case " + description + " ... base [ " + Boolean.valueOf(isBaseCase) + " ] ... done");

        if ( failureMessage != null ) {
            throw new Exception(failureMessage);
        }
    }

    public void validateTargets(TestOptions_SuiteCase suiteCase) {
        String description = getDescription(suiteCase);

        PrintWriter useWriter = getWriter();

        Test_Base_Result useResults = getSuiteResults(suiteCase);
        AnnotationTargetsImpl_Targets useTargets = getSuiteTargets(suiteCase);

        println("Validating targets " + description + " ...");

        useResults.validateInterns(useWriter, useTargets);
        useResults.validateAnnotations(useWriter, useTargets);
        useResults.validateSubclasses(useWriter, useTargets);
        useResults.validateImplements(useWriter, useTargets);

        println("Validating targets " + description + " ... done");
    }

    public void validateInfoStore(TestOptions_SuiteCase suiteCase) throws Exception {
        String description = getDescription(suiteCase);

        PrintWriter useWriter = getWriter();

        Test_Base_Result useResults = getSuiteResults(suiteCase);
        AnnotationTargetsImpl_Targets useTargets = getSuiteTargets(suiteCase);
        InfoStoreImpl useInfoStore = getSuiteStore(suiteCase);

        long infoStartTime = displayStamp("Validating info store " + description + " ...");

        useInfoStore.open(); // throws InfoStoreException
        try {
            useResults.validateInfoStore(useWriter, useTargets, useInfoStore);
        } finally {
            useInfoStore.close(); // throws InfoStoreException
        }

        long infoEndTime = displayStamp(infoStartTime, "Validating info store " + description + " ... done");
        useResults.setInfoTime(infoEndTime - infoStartTime);
    }

    public void validateDifference(TestOptions_SuiteCase suiteCase) {
        String description = getDescription(suiteCase);

        println("Validating target difference" + description + " ...");

        AnnotationTargetsImpl_Targets useTargets = getSuiteTargets(suiteCase);
        AnnotationTargetsImpl_Targets baseTargets = getBaseTargets();

        Test_Base_Result useResults = getSuiteResults(suiteCase);

        TargetsDeltaImpl useDifference = useTargets.subtract(baseTargets); 
        useResults.difference = useDifference;

        if ( !useDifference.isNull() ) {
            List<String> nonNull = new ArrayList<String>();
            useDifference.describe("Case [ " + suiteCase.name() + " ]", nonNull);
            println("Differences:");
            println("========================================");
            for ( String nonNullElement : nonNull ) {
                println(nonNullElement);
            }
            println("========================================");

            useDifference.log( getWriter() );
        }

        println("Validating target difference" + description + " ... done");
    }

    //

    public TestOptions_SuiteCase getBaseCase() {
        return TestOptions_SuiteCase.SINGLE;
    }

    public TestOptions getBaseOptions() {
        return getSuiteOptions( getBaseCase() );
    }

    public AnnotationTargetsImpl_Targets getBaseTargets() {
        return getSuiteTargets( getBaseCase() );
    }

    public String i_getClassName(Class<?> clazz) {
        return getBaseTargets().internClassName(clazz.getName(), false);
    }

    protected AnnotationTargets_Fault createFault(String unresolvedText) {
        return getBaseTargets().getFactory().createFault(unresolvedText);
    }

    protected AnnotationTargets_Fault createFault(String unresolvedText, String parameter) {
        return getBaseTargets().getFactory().createFault(unresolvedText, parameter);
    }    

    protected AnnotationTargets_Fault createFault(String unresolvedText, String[] parameters) {
        return getBaseTargets().getFactory().createFault(unresolvedText, parameters);
    }

    
    public InfoStoreImpl getBaseStore() {
        return getSuiteStore( getBaseCase() );
    }

    public PackageInfo getPackageInfo(String packageName) {
        return getBaseStore().getPackageInfo(packageName);
    }

    public ClassInfo getClassInfo(String className) {
        return getBaseStore().getDelayableClassInfo(className);
    }

    public Test_Base_Result getBaseResults() {
        return getSuiteResults( getBaseCase() );
    }

    //

    public TestOptions getSuiteOptions(TestOptions_SuiteCase suiteCase) {
        return suiteCase.getOptions();
    }

    //

    protected final EnumMap<TestOptions_SuiteCase, ClassSourceImpl_Aggregate> suiteSources =
        new EnumMap<TestOptions_SuiteCase, ClassSourceImpl_Aggregate>(TestOptions_SuiteCase.class);

    public ClassSourceImpl_Aggregate getSuiteSource(TestOptions_SuiteCase suiteCase) {
        return suiteSources.get(suiteCase);
    }

    public void setSuiteSource(TestOptions_SuiteCase suiteCase, ClassSourceImpl_Aggregate source) {
        suiteSources.put(suiteCase, source);
    }

    //

    protected final EnumMap<TestOptions_SuiteCase, AnnotationTargetsImpl_Targets> suiteTargets =
        new EnumMap<TestOptions_SuiteCase, AnnotationTargetsImpl_Targets>(TestOptions_SuiteCase.class);

    public AnnotationTargetsImpl_Targets getSuiteTargets(TestOptions_SuiteCase suiteCase) {
        return suiteTargets.get(suiteCase);
    }

    public void setSuiteTargets(TestOptions_SuiteCase suiteCase, AnnotationTargetsImpl_Targets targets) {
        suiteTargets.put(suiteCase, targets);
    }

    //

    protected final EnumMap<TestOptions_SuiteCase, InfoStoreImpl> suiteStores =
        new EnumMap<TestOptions_SuiteCase, InfoStoreImpl>(TestOptions_SuiteCase.class);

    protected final EnumMap<TestOptions_SuiteCase, InfoStoreImpl> getSuiteStores() {
        return suiteStores;
    }

    public InfoStoreImpl getSuiteStore(TestOptions_SuiteCase suiteCase) {
        return suiteStores.get(suiteCase);
     }
    
    public void setSuiteStore(TestOptions_SuiteCase suiteCase, InfoStoreImpl store) {
        suiteStores.put(suiteCase, store);
    }

    //

    public Test_Base_Result createScanResult() {
        return new Test_Base_Result( getAppSimpleName(), getModSimpleName() );
    }

    protected final EnumMap<TestOptions_SuiteCase, Test_Base_Result> suiteResults =
        new EnumMap<TestOptions_SuiteCase, Test_Base_Result>(TestOptions_SuiteCase.class);

    protected EnumMap<TestOptions_SuiteCase, Test_Base_Result> getSuiteResults() {
        return suiteResults;
    }

    public void setSuiteResults(TestOptions_SuiteCase suiteCase, Test_Base_Result results) {
        suiteResults.put(suiteCase, results);
    }

    public Test_Base_Result getSuiteResults(TestOptions_SuiteCase suiteCase) {
        return suiteResults.get(suiteCase);
    }
}
