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

package com.ibm.ws.annocache.test.scan;

import java.io.File;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Before;

import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_Aggregate;
import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_ClassLoader;
import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_Factory;
import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_MappedDirectory;
import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_MappedJar;
import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_Options;
import com.ibm.ws.annocache.classsource.specification.internal.ClassSourceImpl_Specification;
import com.ibm.ws.annocache.classsource.specification.internal.ClassSourceImpl_Specification_Direct;
import com.ibm.ws.annocache.info.internal.InfoStoreImpl;
import com.ibm.ws.annocache.targets.internal.AnnotationTargetsImpl_Targets;
import com.ibm.ws.annocache.test.scan.Test_Base_State.SourceFactory;
import com.ibm.ws.annocache.test.scan.Test_Base_State.SpecificationFactory;
import com.ibm.ws.annocache.test.utils.TestLocalization;
import com.ibm.ws.annocache.test.utils.TestUtils;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.annocache.classsource.ClassSource_Exception;
import com.ibm.wsspi.annocache.classsource.ClassSource_Factory;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.annocache.info.ClassInfo;
import com.ibm.wsspi.annocache.info.PackageInfo;
import com.ibm.wsspi.annocache.targets.AnnotationTargets_Fault;

import test.common.SharedOutputManager;

public abstract class Test_Base {
    // Convenience copy of the unset category name from the class source factory.
    public static final String UNSET_MOD_CATEGORY_NAME = ClassSource_Factory.UNSET_CATEGORY_NAME;

    // Omit a category name for JavaEE scan caching.
    public static final String JAVAEE_MOD_CATEGORY_NAME = UNSET_MOD_CATEGORY_NAME;

    // CDI addes a category name, since it generates results with a different
    // structure than JavaEE.
    public static final String CDI_MOD_CATEGORY_NAME = "cdi";

    //

    public static SharedOutputManager outputManager;

    public void setUpManager() throws Exception {
        outputManager = SharedOutputManager.getInstance();
        outputManager.traceTo(
            "*=fine",
            new File( TestLocalization.LOGS_PATH + '/' + getClass().getSimpleName() ) );
    }

    //

    public static Test_Base_State baseState;
 
    @Before
    public void setUpBase() throws Exception {
        if ( baseState != null ) {
            return;
        }

        setUpManager(); // throws Exception
        setUpStorage(); // throws Exception

        SpecificationFactory specificationFactory = new SpecificationFactory() {
            @Override
            public ClassSourceImpl_Specification createSpecification(ClassSourceImpl_Factory factory) {
                return Test_Base.this.createClassSourceSpecification(factory);
            }
        };

        SourceFactory sourceFactory = new SourceFactory() {
            @Override
            public ClassSourceImpl_Aggregate createSource(
                ClassSourceImpl_Factory factory,
                ClassSourceImpl_Options options) throws ClassSource_Exception {
                return Test_Base.this.createClassSource(factory, options);
            }
           };

        baseState = new Test_Base_State(
            getClass().getSimpleName(),
            getAppName(), getAppSimpleName(), getModName(), getModSimpleName(),
            getRootClassLoader(),
            specificationFactory,
            sourceFactory);

        baseState.setUpBase(); // throws Exception
    }

    public void setUpStorage() throws Exception {
        String storageForAllCases = TestLocalization.putIntoStorage( getClass().getSimpleName() );

        TestUtils.prepareDirectory(storageForAllCases);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        displayResults();

        baseState.tearDownSuiteStores(); // throws InfoStoreException
        baseState = null;
    }

    public static final int NS_IN_MS = 1000000;

    public static void displayResults() {
        EnumMap<TestOptions_SuiteCase, Test_Base_Result> suiteResults = baseState.getSuiteResults();

        println("Results Summary");
        println("================================================================================");

        for ( TestOptions_SuiteCase suiteCase : TestOptions_SuiteCase.values() ) {
            Test_Base_Result suiteResult = suiteResults.get(suiteCase);
            if ( suiteResult == null ) {
                println("  [ " + padRight(suiteCase.name(), CASE_LEN) + " ]" +
                        " [ " + padLeft("***", TIME_LEN) + " (ms) ]" +
                                " [ " + padLeft("***", TIME_LEN) + " (ms) ]" +
                        " [ " + padLeft("***", COUNT_LEN) + " (classes) ]");
            } else {
                println("  [ " + padRight(suiteCase.name(), CASE_LEN) + " ]" +
                        " [ " + padLeft((suiteResult.scanTime / NS_IN_MS), TIME_LEN) + " (ms) ]" +
                        " [ " + padLeft((suiteResult.infoTime / NS_IN_MS), TIME_LEN) + " (ms) ]" +
                        " [ " + padLeft(suiteResult.seedClassCount, COUNT_LEN) + " (classes) ]");

                ClassSource_Aggregate.TimingData timingData = suiteResult.getTimingData();

                println("    Scan     [ " + padLeft(timingData.getScanSources(), COUNT_LEN) + " (sources) ]" +
                        " [ " + padLeft((timingData.getScanTime() / NS_IN_MS), TIME_LEN) + " (ms) ]" +
                        " [ " + padLeft(timingData.getScanClasses(), COUNT_LEN) + " (classes) ]");

                println("    Cache    [ " + padLeft(timingData.getReadSources(), COUNT_LEN) + " (sources) ]" +
                        " [ " + padLeft((timingData.getReadTime() / NS_IN_MS), TIME_LEN) + " (ms) ]" +
                        " [ " + padLeft(timingData.getReadClasses(), COUNT_LEN) + " (classes) ]");

                println("    Jandex   [ " + padLeft(timingData.getJandexSources(), COUNT_LEN) + " (sources) ]" +
                        " [ " + padLeft((timingData.getJandexTime() / NS_IN_MS), TIME_LEN) + " (ms) ]" +
                        " [ " + padLeft(timingData.getJandexClasses(), COUNT_LEN) + " (classes) ]");

                println("    External [ " + padLeft(timingData.getExternalSources(), COUNT_LEN) + " (sources) ]" +
                        " [ " + padLeft((timingData.getExternalTime() / NS_IN_MS), TIME_LEN) + " (ms) ]" +
                        " [ " + padLeft(timingData.getExternalClasses(), COUNT_LEN) + " (classes) ]");

                println("    Cache Write  [ " + padLeft((timingData.getCacheWriteTime() / NS_IN_MS), TIME_LEN) + " (ms) ]" +
                        " Cache Read [ " + padLeft((timingData.getCacheReadTime() / NS_IN_MS), TIME_LEN) + " (ms) ]");
            }
        }

        println("================================================================================");
    }

    public static final int CASE_LEN = 18;
    public static final int TIME_LEN = 5;
    public static final int COUNT_LEN = 5;

    public static final String PAD = "                              ";
    public static final int MAX_PAD = 30;

    public static String padLeft(String text, int padLen) {
        return pad(text, padLen, PAD_LEFT);
    }

    public static String padRight(String text, int padLen) {
        return pad(text, padLen, !PAD_LEFT);
    }

    public static final boolean PAD_LEFT = true;
    public static final boolean PAD_RIFHT = false;

    public static String pad(String text, int padLen, boolean padLeft) {
        if ( padLen > MAX_PAD ) {
                padLen = MAX_PAD;
        }

        int textLen = text.length();
        if ( textLen >= padLen ) {
                return text;
        }

        int fillLen = padLen - textLen;
        String fill = PAD.substring(0, fillLen);
        
        if ( padLeft ) {
                return fill + text;
        }else {
                return text + fill;
        }
    }

    public static String padLeft(int value, int padLen) {
        return pad(value, padLen, PAD_LEFT);
    }
    
    public static String padRight(int value, int padLen) {
        return pad(value, padLen, !PAD_LEFT);
    }

    public static String pad(int value, int padLen, boolean padLeft) {
        return pad(Integer.toString(value), padLen, padLeft);
    }

    public static String padLeft(long value, int padLen) {
        return pad(value, padLen, PAD_LEFT);
    }
    
    public static String padRight(long value, int padLen) {
        return pad(value, padLen, !PAD_LEFT);
    }    

    public static String pad(long value, int padLen, boolean padLeft) {
        return pad(Long.toString(value), padLen, padLeft);
    }

    //

    public static PrintWriter getWriter() {
        return baseState.getWriter();
    }

    public static void println(String text) {
        baseState.println(text);
    }

    public static String toString(Collection<? extends Object> values) {
        return TestUtils.toString(values);
    }

    public static Set<String> filter(Set<String> classNames) {
        return TestUtils.filter(classNames);
    }

    //

    public abstract String getAppName();
    public abstract String getAppSimpleName();
    public abstract String getModName();
    public abstract String getModSimpleName();

    public ClassLoader getRootClassLoader() {
        return getClass().getClassLoader();
    }

    public ClassSourceImpl_Specification_Direct createClassSourceSpecification(
        ClassSourceImpl_Factory classSourceFactory) {
        return null;
    }

    @SuppressWarnings("unused")
    public ClassSourceImpl_Aggregate createClassSource(
        ClassSourceImpl_Factory classSourceFactory,
        ClassSourceImpl_Options options) throws ClassSource_Exception {
        return null;
    }

    public ClassSourceImpl_MappedDirectory addDirectoryClassSource(
        ClassSourceImpl_Aggregate rootClassSource,
        String name,
        String dirPath, String entryPrefix) throws ClassSource_Exception {

        ClassSourceImpl_MappedDirectory dirClassSource =
            rootClassSource.getFactory().createDirectoryClassSource(rootClassSource, name, dirPath, entryPrefix);
        // 'createDirectoryClassSource' throws ClassSource_Exception

        rootClassSource.addClassSource(dirClassSource);

        return dirClassSource;
    }

    public ClassSourceImpl_MappedDirectory addClassesDirectoryClassSource(
        ClassSourceImpl_Aggregate rootClassSource,
        String warPath) throws ClassSource_Exception {

        return addDirectoryClassSource(rootClassSource, "classes", warPath, "WEB-INF/classes/");
        // throws ClassSource_Exception
    }

    public ClassSourceImpl_MappedJar addJarClassSource(
        ClassSourceImpl_Aggregate rootClassSource,
        String name,
        String jarPath) throws ClassSource_Exception {

        ClassSourceImpl_MappedJar jarClassSource =
            rootClassSource.getFactory().createJarClassSource(rootClassSource, name, jarPath);
        // 'createDirectoryClassSource' throws ClassSource_Exception

        rootClassSource.addClassSource(jarClassSource);

        return jarClassSource;
    }

    public void addLibJarClassSources(
        ClassSourceImpl_Aggregate rootClassSource,
        String warPath,
        ScanPolicy scanPolicy,
        String... jarNames) throws ClassSource_Exception {

        ClassSourceImpl_Factory classSourceFactory = rootClassSource.getFactory();

        String warLibPath = warPath + "WEB-INF/lib/";

        for ( String jarName : jarNames ) {
            String jarPath = warLibPath + jarName;

            ClassSourceImpl_MappedJar jarClassSource =
                classSourceFactory.createJarClassSource(rootClassSource, jarPath, jarPath);
            // throws ClassSource_Exception

            rootClassSource.addClassSource(jarClassSource, scanPolicy);
        }
    }

    public ClassSourceImpl_ClassLoader addClassLoaderClassSource(
        ClassSourceImpl_Aggregate rootClassSource,
        String name,
        ClassLoader classLoader) throws ClassSource_Exception {

        ClassSourceImpl_ClassLoader classLoaderClassSource =
            rootClassSource.getFactory().createClassLoaderClassSource(rootClassSource, name, classLoader);
        // 'createClassLoaderClassSource' throws ClassSource_Exception

        rootClassSource.addClassLoaderClassSource(classLoaderClassSource);

        return classLoaderClassSource;
    }

    public ClassSourceImpl_ClassLoader addClassLoaderClassSource(
        ClassSourceImpl_Aggregate rootClassSource)  throws ClassSource_Exception { 

        return addClassLoaderClassSource(rootClassSource, "external references",  getRootClassLoader());
    }

    //

    public PackageInfo getPackageInfo(String packageName) {
        return baseState.getPackageInfo(packageName);
    }

    public ClassInfo getClassInfo(String className) {
        return baseState.getClassInfo(className);
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

    public InfoStoreImpl getBaseStore() {
        return getSuiteStore( getBaseCase() );
    }

    public Test_Base_Result getBaseResults() {
        return getSuiteResults( getBaseCase() );
    }

    //

    public TestOptions getSuiteOptions(TestOptions_SuiteCase suiteCase) {
        return suiteCase.getOptions();
    }

    public ClassSourceImpl_Aggregate getSuiteSource(TestOptions_SuiteCase suiteCase) {
        return baseState.getSuiteSource(suiteCase);
    }
    
    public AnnotationTargetsImpl_Targets getSuiteTargets(TestOptions_SuiteCase suiteCase) {
        return baseState.getSuiteTargets(suiteCase);
    }

    public InfoStoreImpl getSuiteStore(TestOptions_SuiteCase suiteCase) {
        return baseState.getSuiteStore(suiteCase);
    }

    public Test_Base_Result getSuiteResults(TestOptions_SuiteCase suiteCase) {
        return baseState.getSuiteResults(suiteCase);
    }

    //

    /**
     * Main test entry point: Call this from subclasses to perform
     * that test using particular suite options.
     * 
     * @param suiteCase The suite case to use for running the test.
     * 
     * @throws Exception Thrown in case of a test failure.
     */
    public void runSuiteTest(TestOptions_SuiteCase suiteCase) throws Exception {
        baseState.runSuiteTest(suiteCase); // throws Exception
    }

    //

    public AnnotationTargets_Fault createFault(String unresolvedText) {
        return baseState.createFault(unresolvedText);
    }

    public AnnotationTargets_Fault createFault(String unresolvedText, String parm) {
        return baseState.createFault(unresolvedText, parm);
    }

    public AnnotationTargets_Fault createFault(String unresolvedText, String... parms) {
        return baseState.createFault(unresolvedText, parms);
    }
}
