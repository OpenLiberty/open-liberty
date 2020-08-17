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

package com.ibm.ws.anno.test.cases;

import java.io.File;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

import com.ibm.ws.anno.classsource.internal.ClassSourceImpl_Factory;
import com.ibm.ws.anno.classsource.specification.ClassSource_Specification_Direct;
import com.ibm.ws.anno.info.internal.InfoStoreFactoryImpl;
import com.ibm.ws.anno.service.internal.AnnotationServiceImpl_Service;
import com.ibm.ws.anno.targets.internal.AnnotationTargetsImpl_Factory;
import com.ibm.ws.anno.targets.internal.AnnotationTargetsImpl_Targets;
import com.ibm.ws.anno.util.internal.UtilImpl_Factory;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.classsource.ClassSource_Exception;
import com.ibm.wsspi.anno.info.AnnotationInfo;
import com.ibm.wsspi.anno.info.ClassInfo;
import com.ibm.wsspi.anno.info.FieldInfo;
import com.ibm.wsspi.anno.info.InfoStore;
import com.ibm.wsspi.anno.info.InfoStoreException;
import com.ibm.wsspi.anno.info.InfoStoreFactory;
import com.ibm.wsspi.anno.info.MethodInfo;
import com.ibm.wsspi.anno.info.PackageInfo;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Exception;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Fault;
import com.ibm.wsspi.anno.util.Util_BidirectionalMap;
import com.ibm.wsspi.anno.util.Util_InternMap;

public abstract class AnnotationTest_BaseClass {
    // The target name is used to locate the target module relative to
    // the data directory.

    SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=fine").logTo(TestConstants.BUILD_LOGS + this.getClass().getSimpleName());

    @Rule
    public TestRule outputRule = outputMgr;

    public abstract String getTargetName();

    // The class source specifications embed all information used to locate
    // the resources of the target.

    public abstract ClassSource_Specification_Direct createClassSourceSpecification();

    // Override in subclasses to increase the iteration count.  (A count of 20 is usual, so
    // to collect good timing results.)  The count is expected to be at least 1.

    public int getIterations() {
        return 1;
    }

    // Flag used to set the baseline results.  When set, the first run will store the
    // annotation targets to the specified storage location.
    //
    // Set this once to compute and store a baseline.  Then copy the baseline into the main
    // publish location, and change the value back to false.

    public boolean getSeedStorage() {
        return false;
    }

    // Override these to enable a detailed listing of the scan results.

    public boolean getDoPrint() {
        return false;
    }

    public boolean getDoPrintEach() {
        return false;
    }

    //

    public String selectProjectPath(String partialPath) {
        System.out.println("Placing project folder [ " + partialPath + " ]:");

        File eclipseFile = new File(partialPath);
        String eclipsePath = eclipseFile.getAbsolutePath();
        System.out.println("Trying eclipse location [ " + eclipsePath + " ]");

        if ( eclipseFile.exists() ) {
            return partialPath;
        }

        File repoRootFile = new File(TestConstants.REPOSITORY_TEST_ROOT);
        String repoRootPath = repoRootFile.getAbsolutePath();
        System.out.println("Trying repository location [ " + repoRootPath + " ]");

        if ( !repoRootFile.exists() ) {
            String errorMessage =
                "Unable to locate eclipse file [ " + eclipsePath + " ]" +
                " or repository root file [ " + repoRootPath + " ]";
            throw new IllegalArgumentException(errorMessage);
        }

        File repoFile = new File(repoRootFile, partialPath);
        String repoPath = repoFile.getAbsolutePath();
        System.out.println("Verifying target location [ " + repoPath + " ]");

        if ( repoFile.exists() ) {
            return repoPath;
        }

        String errorMessage =
            "Unable to locate eclipse file [ " + eclipsePath + " ]" +
            "; located repository root file [ " + repoRootPath + " ]" +
            " but failed to locate target file [ " + repoPath + " ]";
        throw new IllegalArgumentException(errorMessage);
    }

    @Before
    public void setUp() throws Exception {
        setProjectPath( selectProjectPath("publish" + File.separator + "appData") );
        setFactories(); // throws Exception
    }

    //

    protected String projectPath;

    public String getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    //

    protected String dataPath;

    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    //

    protected void setFactories() throws Exception {
        this.utilFactory = new UtilImpl_Factory();
        this.classSourceFactory = new ClassSourceImpl_Factory(utilFactory);
        this.annotationTargetsFactory = new AnnotationTargetsImpl_Factory(utilFactory, classSourceFactory);
        this.infoStoreFactory = new InfoStoreFactoryImpl(utilFactory);
        this.targetsService = new AnnotationServiceImpl_Service(null, utilFactory, classSourceFactory, annotationTargetsFactory, infoStoreFactory);
    }

    //

    protected AnnotationServiceImpl_Service targetsService;

    public AnnotationServiceImpl_Service getAnnotationService() {
        return targetsService;
    }

    //

    protected UtilImpl_Factory utilFactory;
    protected ClassSourceImpl_Factory classSourceFactory;
    protected AnnotationTargetsImpl_Factory annotationTargetsFactory;
    protected InfoStoreFactory infoStoreFactory;

    public UtilImpl_Factory getUtilFactory() {
        return utilFactory;
    }

    public ClassSourceImpl_Factory getClassSourceFactory() {
        return classSourceFactory;
    }

    public ClassSource_Aggregate createClassSource() throws ClassSource_Exception {
        return createClassSourceSpecification().createClassSource(getTargetName(), getClass().getClassLoader());
        // 'createClassSource' throws ClassSource_Exception
    }

    public AnnotationTargetsImpl_Factory getAnnotationTargetsFactory() {
        return annotationTargetsFactory;
    }

    public AnnotationTargetsImpl_Targets createAnnotationTargets(boolean detailFlag)
                    throws AnnotationTargets_Exception {

        return getAnnotationTargetsFactory().createTargets(detailFlag);
        // 'createTargets' throws AnnotationTargets_Exception
    }

    protected AnnotationTargets_Fault createFault(String unresolvedText) {
        return getAnnotationTargetsFactory().createFault(unresolvedText, null);
    }

    protected AnnotationTargets_Fault createFault(String unresolvedText, String[] parameters) {
        return getAnnotationTargetsFactory().createFault(unresolvedText, parameters);
    }

    //

    public InfoStoreFactory getInfoStoreFactory() {
        return infoStoreFactory;
    }

    protected InfoStore createInfoStore(ClassSource_Aggregate classSource)
                    throws InfoStoreException {
        return getInfoStoreFactory().createInfoStore(classSource);
        // 'createInfoStore' throws InfoStoreException
    }

    //

    public static final String COMMON_TEMP_STORAGE_PATH = "temp";
    public static final String COMMON_STORAGE_PATH = "cache";

    public static final String STORAGE_NAME_NO_DETAIL = "annoTargets_noDetail.txt";
    public static final String STORAGE_NAME_DETAIL = "annoTargets_detail.txt";

    public String getStoragePath(String storageFragment) {
        return getProjectPath() + File.separator +
               storageFragment + File.separator +
               getDataPath() + File.separator +
               getTargetName() + File.separator;
    }

    //

    public static final boolean DETAIL_IS_ENABLED = true;
    public static final boolean DETAIL_IS_NOT_ENABLED = false;

    public static final boolean DO_SEED_STORE = true;
    public static final boolean DO_NOT_SEED_STORE = false;

    public void runScanTest(boolean detailFlag,
                            String tempStoragePath, String tempStorageName,
                            boolean seedStorage, String storagePath, String storageName,
                            PrintWriter writer) throws Exception {

        runScanTest(detailFlag,
                    getIterations(),
                    tempStoragePath, tempStorageName,
                    seedStorage, storagePath, storageName,
                    writer);
        // throws Exception
    }

    public void runScanTest(boolean detailFlag, int iterations,
                            String tempStoragePath, String tempStorageName,
                            boolean seedStorage, String storagePath, String storageName,
                            PrintWriter writer) throws Exception {

        AnnotationTest_TestResult scanResult =
                        new AnnotationTest_TestResult(getTargetName(), detailFlag, iterations);

        for (int testIteration = 0; testIteration < iterations; testIteration++) {
            runScanTest(scanResult,
                        detailFlag, testIteration,
                        tempStoragePath, tempStorageName,
                        seedStorage, storagePath, storageName,
                        writer); // throws Exception

            seedStorage = false; // Only on the first scan
        }

        scanResult.display(writer);
    }

    public void runScanTest(AnnotationTest_TestResult scanResult,
                            boolean detailFlag, int testIteration,
                            String tempStoragePath, String tempStorageName,
                            boolean seedStorage, String storagePath, String storageName,
                            PrintWriter writer) throws Exception {

        displayLocationParameters(writer);

        long startTime = displayStamp("Begin Target Scan [ " + scanResult.targetName + " ]", writer);

        // Handle any initial exception by allowing the exception to be thrown, failing
        // the test.  A failure during the initial scan makes any subsequent validations
        // meaningless.

        ClassSource_Aggregate classSource = createClassSource(); // throws ClassSource_Exception
        AnnotationTargetsImpl_Targets annotationTargets = createAnnotationTargets(detailFlag); // throws AnnotationTargets_Exception

        annotationTargets.scan(classSource); // throws AnnotationTargets_Exception

        long endTime = displayStamp(startTime, "End Target Scan [ " + scanResult.targetName + " ]", writer);

        long scanTime = endTime - startTime;
        scanResult.addScanTime(scanTime);

        verifyTargetMappings(writer, annotationTargets, scanResult);
        verifySubclassMap(writer, annotationTargets, scanResult);
        verifyImplementsMap(writer, annotationTargets, scanResult);

        if (testIteration == 0) {
            long infoStartTime = displayStamp("Begin Info Scan [ " + scanResult.targetName + " ]", writer);

            verifyInfoStore(writer, classSource, annotationTargets, scanResult);

            long infoEndTime = displayStamp(infoStartTime, "End Info Scan [ " + scanResult.targetName + " ]", writer);

            long infoTime = infoEndTime - infoStartTime;

            scanResult.setInfoTime(infoTime);
        }

        if (getDoPrint()) {
            AnnotationTest_Printer printer = new AnnotationTest_Printer();

            printer.printClasses("Classes for [ " + scanResult.targetName + " ]", annotationTargets);
            printer.printTargets("Targets for [ " + scanResult.targetName + " ]", annotationTargets);
        }

        if (getDoPrintEach()) {
            displayNextResult(classSource, annotationTargets, startTime, endTime, scanResult, writer);
        }

        scanResult.addResults(classSource, annotationTargets);
    }

    protected void displayLocationParameters(PrintWriter writer) {
        writer.println("Location Parameters:");

        writer.println("  Current working directory: " + System.getProperty("user.dir"));
        writer.println("  System temporary directory: " + System.getProperty("java.io.tmpdir"));

        writer.println("  Project path: " + getProjectPath());
        writer.println("  Data path: " + getDataPath());
    }

    protected void verifyTargetMappings(PrintWriter writer,
                                        AnnotationTargetsImpl_Targets annotationTargets,
                                        AnnotationTest_TestResult scanResult) {

        writer.println("Verifying annotation mappings ...");

        verifyBidiMap(writer, "Package Annotations Mapping", annotationTargets.getPackageAnnotationData(), scanResult);
        verifyBidiMap(writer, "Class Annotations Mapping", annotationTargets.getClassAnnotationData(), scanResult);
        verifyBidiMap(writer, "Field Annotations Mapping", annotationTargets.getFieldAnnotationData(), scanResult);
        verifyBidiMap(writer, "Method Annotations Mapping", annotationTargets.getMethodAnnotationData(), scanResult);

        writer.println("Verifying annotation mappings ... done");
    }

    protected void verifySubclassMap(PrintWriter writer, AnnotationTargetsImpl_Targets annotationTargets, AnnotationTest_TestResult scanResult) {
        long startTime = displayStamp("Validating subclass map ... [ " + scanResult.targetName + " ]", writer);

        long firstCallTime = -1;

        Set<String> alreadyProcessed = new HashSet<String>();
        int newProblemCount = 0;
        int processedSubclassCount = 0;

        //   getSuperclassNames() returns a Map of classes and their direct superclass:
        //       Key      Values
        //       -----     ------
        //       class   superclass
        //
        //   Iterate through the super classes (the values column).
        //   Verify each superclass has at least one subclass.
        //   The superclass may appear multiple times in the values column since
        //      multiple classes can have the same superclass. Skip duplicates.
        //   Get all subclasses of the superclass.  For each of the subclasses, verify
        //     that it is a descendant of the superclass.

        for (String className : annotationTargets.getSuperclassNames().values()) {

            if (alreadyProcessed.contains(className)) {
                continue;
            } else {
                alreadyProcessed.add(className);
            }

            Set<String> subclassNameSet = annotationTargets.getSubclassNames(className);

            if (firstCallTime == -1) {
                firstCallTime = displayStamp(startTime, "First call subclass scan [ " + scanResult.targetName + " ]", writer);
            }

            // Test for no subclasses.  Since "className" is a superclass it should have at least 1 subclass
            if (subclassNameSet.size() == 0) {
                AnnotationTargets_Fault mapFault =
                                createFault("Class [ {0} ] has no subclasses!", new String[] { className });

                scanResult.addVerificationMessage(mapFault);
                writer.println(mapFault.getResolvedText());
                newProblemCount++;
                continue;
            }

            processedSubclassCount += subclassNameSet.size();
        }

        writer.println("Processed [ " + alreadyProcessed.size() + " ] unique superclasses," +
                       " and found [ " + processedSubclassCount + " ] subclasses." +
                       "  Found [ " + newProblemCount + " ] problems.");

        displayStamp(startTime, "End validation of subclass map [ " + scanResult.targetName + " ]", writer);
    }

    protected void verifyImplementsMap(PrintWriter writer, AnnotationTargetsImpl_Targets annotationTargets, AnnotationTest_TestResult scanResult) {
        long startTime = displayStamp("Validating implements map [ " + scanResult.targetName + " ]", writer);

        int problemCount = 0;

        writer.println("Processed [ " + annotationTargets.getScannedClassNames().size() + " ] classes." +
                       "  Found [ " + problemCount + " ] problems.");

        displayStamp(startTime, "End validation of implements map [ " + scanResult.targetName + " ]", writer);
    }

    protected void verifyInfoStore(PrintWriter writer,
                                   ClassSource_Aggregate classSource,
                                   AnnotationTargetsImpl_Targets annotationTargets,
                                   AnnotationTest_TestResult scanResult) throws InfoStoreException {
        writer.println("Verifying info store [ " + scanResult.targetName + " ] ...");

        // Simply fail with a thrown exception if the info store could not be opened.
        // That is another unrecoverable case.

        writer.println("Created info store ...");

        InfoStore infoStore = createInfoStore(classSource); // throws InfoStoreException
        infoStore.open(); // throws InfoStoreException

        writer.println("Created info store ... done");

        try {
            verifyInfoStore(writer, classSource, annotationTargets, infoStore, scanResult);
            // throws InfoStoreException

        } finally {

            writer.println("Discarding info store ...");

            infoStore.close(); // throws InfoStoreException

            writer.println("Discarding info store ... done");
        }
    }

    @SuppressWarnings("unused")
    protected void verifyInfoStore(PrintWriter writer,
                                   ClassSource_Aggregate classSource,
                                   AnnotationTargetsImpl_Targets annotationTargets,
                                   InfoStore infoStore,
                                   AnnotationTest_TestResult scanResult) throws InfoStoreException {

        writer.println("Running base verification ...");

        Set<String> packagesWithAnnotations = annotationTargets.getAnnotatedPackages();
        writer.println("Packages with annotations: [" + packagesWithAnnotations.size() + "]");

        for (String packageName : packagesWithAnnotations) {
            PackageInfo packageInfo = infoStore.getPackageInfo(packageName);
            Collection<? extends AnnotationInfo> annotationsInClass = packageInfo.getDeclaredAnnotations();
            if (!checkSize(annotationTargets, packageName, annotationsInClass.size())) {
                AnnotationTargets_Fault fault = createFault("PackageInfo for [ {0} ] has the wrong number of annotations", new String[] { packageName });
                scanResult.addVerificationMessage(fault);
                writer.println("Info fault: [ " + fault.getResolvedText() + " ]");
            }
            for (AnnotationInfo annotation : annotationsInClass) {
                if (!packageHasAnnotation(annotationTargets, packageName, annotation.getAnnotationClassName())) {
                    AnnotationTargets_Fault infoFault =
                                    createFault("Package [ {0} ] annotation [ {1} ] not found in targets",
                                                new String[] { packageName, annotation.getAnnotationClassName() });
                    scanResult.addVerificationMessage(infoFault);
                    writer.println("Info fault: [ " + infoFault.getResolvedText() + " ]");
                }
            }
        }

        Set<String> classesWithAnnotations = annotationTargets.getAnnotatedClasses();
        writer.println("Classes with annotations: [" + classesWithAnnotations.size() + "]");

        for (String className : classesWithAnnotations) {
            ClassInfo classInfo = infoStore.getDelayableClassInfo(className);
            Collection<? extends AnnotationInfo> annotationsInClass = classInfo.getDeclaredAnnotations();
            for (AnnotationInfo annotation : annotationsInClass) {
                if (!classHasAnnotation(annotationTargets, className, annotation.getAnnotationClassName())) {
                    AnnotationTargets_Fault infoFault =
                                    createFault("Class [ {0} ] annotation [ {1} ] not found in targets",
                                                new String[] { className, annotation.getAnnotationClassName() });
                    scanResult.addVerificationMessage(infoFault);
                    writer.println("Info fault: [ " + infoFault.getResolvedText() + " ]");
                }
            }
        }

        // Only check method/field annotations if the endpoint scanner was enabled to capture that information
        if (annotationTargets.getIsDetailEnabled()) {
            Set<String> classesWithMethodAnnotations = annotationTargets.getClassesWithMethodAnnotations();
            writer.println("Classes with method annotations: [" + classesWithMethodAnnotations.size() + "]");

            for (String className : classesWithMethodAnnotations) {
                ClassInfo classInfo = infoStore.getDelayableClassInfo(className);
                Collection<? extends MethodInfo> methods = classInfo.getDeclaredMethods();
                for (MethodInfo method : methods) {
                    Collection<? extends AnnotationInfo> annotationsInClass = method.getDeclaredAnnotations();
                    for (AnnotationInfo annotation : annotationsInClass) {
                        if (!classHasMethodAnnotation(annotationTargets, className, annotation.getAnnotationClassName())) {
                            AnnotationTargets_Fault infoFault =
                                            createFault("Class [ {0} ] method [ {1} ] annotation [ {2} ] not found in targets",
                                                        new String[] { className, method.getName(), annotation.getAnnotationClassName() });
                            scanResult.addVerificationMessage(infoFault);
                            writer.println("Info fault: [ " + infoFault.getResolvedText() + " ]");
                        }
                    }
                }
            }

            Set<String> classesWithFieldAnnotations = annotationTargets.getClassesWithFieldAnnotations();
            writer.println("Classes with method annotations: [" + classesWithFieldAnnotations.size() + "]");

            for (String className : classesWithFieldAnnotations) {
                ClassInfo classInfo = infoStore.getDelayableClassInfo(className);
                Collection<? extends FieldInfo> fields = classInfo.getDeclaredFields();
                for (FieldInfo field : fields) {
                    Collection<? extends AnnotationInfo> annotationsInClass = field.getDeclaredAnnotations();
                    for (AnnotationInfo annotation : annotationsInClass) {
                        if (!classHasFieldAnnotation(annotationTargets, className, annotation.getAnnotationClassName())) {
                            AnnotationTargets_Fault infoFault =
                                            createFault("Class [ {0} ] field [ {1} ] annotation [ {2} ] not found in targets",
                                                        new String[] { className, field.getName(), annotation.getAnnotationClassName() });
                            scanResult.addVerificationMessage(infoFault);
                            writer.println("Info fault: [ " + infoFault.getResolvedText() + " ]");
                        }
                    }
                }
            }
        }

        writer.println("Running base verification ... done");
    }

    /**
     * @param annotationTargets
     * @param packageName
     * @param i
     * @return
     */
    private boolean checkSize(AnnotationTargetsImpl_Targets annotationTargets, String packageName, int i) {
        return annotationTargets.getPackageAnnotations(packageName).size() == i;
    }

    protected void displayNextResult(ClassSource_Aggregate classSource,
                                     AnnotationTargetsImpl_Targets annotationTargets,
                                     long startTime, long endTime,
                                     AnnotationTest_TestResult scanResult,
                                     PrintWriter writer) {

        scanResult.displayNextResult(classSource, annotationTargets,
                                     startTime, endTime,
                                     writer);
    }

    private void verifyBidiMap(PrintWriter writer, String banner, Util_BidirectionalMap map, AnnotationTest_TestResult scanResult) {

        String holderTag = map.getHolderTag();
        String heldTag = map.getHeldTag();

        writer.println("Validating mapping [ " + banner + " ] Mapping [ " + holderTag + " ] to [ " + heldTag + " ] ...");

        if (!map.getIsEnabled()) {
            writer.println("Trivial validation: Mapping is not enabled");
            return;
        }

        Set<String> holderSet = map.getHolderSet();
        writer.println("Holders [ " + holderTag + " ] [ " + holderSet.size() + " ]");

        for (String className : holderSet) {
            Set<String> held = map.selectHeldOf(className);
            for (String annotationClassName : held) {
                if (!map.selectHoldersOf(annotationClassName).contains(className)) {
                    AnnotationTargets_Fault mapFault =
                                    createFault("Mapping [ {0} ] does not find reverse of holder [ {1} ] [ {2} ] to held [ {3} [ {4} ]",
                                                new String[] { banner,
                                                              holderTag, className,
                                                              heldTag, annotationClassName });
                    scanResult.addVerificationMessage(mapFault);

                    writer.println("Mapping fault: [ " + mapFault.getResolvedText() + " ]");
                }
            }
        }

        Set<String> heldSet = map.getHeldSet();
        writer.println("Held [ " + heldTag + " ] [ " + heldSet.size() + " ]");

        for (String annotationClassName : heldSet) {
            Set<String> holders = map.selectHoldersOf(annotationClassName);
            for (String className : holders) {
                if (!map.selectHeldOf(className).contains(annotationClassName)) {
                    AnnotationTargets_Fault mapFault =
                                    createFault("Mapping [ {0} ] does not find forward of holder [ {1} ] [ {2} ] to held [ {3} [ {4} ]",
                                                new String[] { banner,
                                                              holderTag, className,
                                                              heldTag, annotationClassName });
                    scanResult.addVerificationMessage(mapFault);

                    writer.println("Mapping fault: [ " + mapFault.getResolvedText() + " ]");
                }
            }
        }

        writer.println("Validating mapping [ " + banner + " ] Mapping [ " + holderTag + " ] to [ " + heldTag + " ] ... done");
    }

    //

    protected void verifyClasses(PrintWriter writer,
                                 AnnotationTargetsImpl_Targets annotationTargets,
                                 AnnotationTest_TestResult scanResult,
                                 String[] validClassNames,
                                 String[] nonValidClassNames) {

        writer.println("Verifying [ " + validClassNames.length + " ] valid and [ " + nonValidClassNames.length + " ] non-valid classes ...");

        for (String validClassName : validClassNames) {
            if (!isScannedClassName(annotationTargets, validClassName)) {
                AnnotationTargets_Fault mapFault =
                                createFault("Valid class [ {0} ] was not found!", new String[] { validClassName });

                scanResult.addVerificationMessage(mapFault);
                writer.println(mapFault.getResolvedText());
            }
        }

        for (String nonValidClassName : nonValidClassNames) {
            if (isScannedClassName(annotationTargets, nonValidClassName)) {
                AnnotationTargets_Fault mapFault =
                                createFault("Non-valid class [ {0} ] was found!", new String[] { nonValidClassName });

                scanResult.addVerificationMessage(mapFault);
                writer.println(mapFault.getResolvedText());
            }
        }

        writer.println("Verifying [ " + validClassNames.length + " ] valid and [ " + nonValidClassNames.length + " ] non-valid classes ... done");
    }

    //

    public long getTime() {
        return System.currentTimeMillis();
    }

    public long displayStamp(String banner, PrintWriter writer) {
        long endTime = getTime();

        writer.println(banner + " [ ****** ] [ " + endTime + " ]");

        return endTime;
    }

    public long displayStamp(long startTime, String banner, PrintWriter writer) {
        long endTime = getTime();

        writer.println(banner + " [ " + startTime + " ] [ " + endTime + " ] [ " + (endTime - startTime) + " ]");

        return endTime;
    }

    //

    public void discardRef(long value) {
        // NO-OP
    }

    public static boolean packageHasAnnotation(AnnotationTargetsImpl_Targets targets, String packageName, String annotationName) {
        return targets.getPackageAnnotationData().holds(packageName, annotationName);
    }

    public static boolean classHasAnnotation(AnnotationTargetsImpl_Targets targets, String className, String annotationName) {
        return targets.getClassAnnotationData().holds(className, annotationName);
    }

    public static boolean classHasMethodAnnotation(AnnotationTargetsImpl_Targets targets, String className, String annotationName) {
        return targets.getMethodAnnotationData().holds(className, annotationName);
    }

    public static boolean classHasFieldAnnotation(AnnotationTargetsImpl_Targets targets, String className, String annotationName) {
        return targets.getFieldAnnotationData().holds(className, annotationName);
    }

    public static boolean isScannedClassName(AnnotationTargetsImpl_Targets targets, String className) {
        String i_className = targets.getClassInternMap().intern(className, Util_InternMap.DO_NOT_FORCE);
        if (i_className == null) {
            return false;
        }

        return targets.i_containsScannedClassName(i_className);
    }
}
