/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.test.info;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TrConfigurator;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl;
import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_Aggregate;
import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_Factory;
import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_MappedDirectory;
import com.ibm.ws.annocache.info.internal.ClassInfoCacheImpl;
import com.ibm.ws.annocache.info.internal.DelayedClassInfoImpl;
import com.ibm.ws.annocache.info.internal.InfoStoreFactoryImpl;
import com.ibm.ws.annocache.info.internal.InfoStoreImpl;
import com.ibm.ws.annocache.info.internal.NonDelayedClassInfoImpl;
import com.ibm.ws.annocache.test.utils.TestLocalization;
import com.ibm.ws.annocache.util.internal.UtilImpl_Factory;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.classsource.ClassSource_ScanCounts;
import com.ibm.wsspi.anno.classsource.ClassSource_ScanCounts.ResultField;
import com.ibm.wsspi.annocache.classsource.ClassSource_Exception;
import com.ibm.wsspi.annocache.classsource.ClassSource_Factory;
import com.ibm.wsspi.annocache.classsource.ClassSource_Streamer;
import com.ibm.wsspi.annocache.info.InfoStore;
import com.ibm.wsspi.annocache.info.InfoStoreException;
import com.ibm.wsspi.annocache.util.Util_InternMap;

import test.common.SharedOutputManager;

/**
 * Test key info store internals.
 */
public class InfoStore_Internals_Test {
    protected SharedOutputManager outputMgr =
        SharedOutputManager.getInstance().trace("*=all").logTo(TestLocalization.LOGS_RELATIVE_PATH + getClass().getSimpleName());

    @Rule
    public TestRule outputRule = outputMgr;

    //

    protected static ClassSourceImpl_Aggregate rootClassSource;
    protected static InfoStoreImpl infoStore;

    @BeforeClass
    public static void setup() throws ClassSource_Exception, InfoStoreException {
        UtilImpl_Factory utilImplFactory = new UtilImpl_Factory();
        ClassSourceImpl_Factory factory = new ClassSourceImpl_Factory(utilImplFactory);

        ClassSourceImpl_Aggregate useRootClassSource =
            factory.createAggregateClassSource("TestEar", "TestMod", ClassSource_Factory.UNSET_CATEGORY_NAME, factory.createOptions() );

        ClassSourceImpl_MappedDirectory dirClassSource =
            factory.createDirectoryClassSource(
                useRootClassSource, "WEB-INF/classes", TestLocalization.putIntoProject("build/classes") + "/");
        useRootClassSource.addClassSource(dirClassSource);

        TestClassSource testClassSource =
            new TestClassSource(factory, useRootClassSource.getInternMap(), "Test");
        useRootClassSource.addClassSource(testClassSource);

        rootClassSource = useRootClassSource;

        InfoStoreFactoryImpl infoStoreFactory = new InfoStoreFactoryImpl(utilImplFactory);
        infoStore = infoStoreFactory.createInfoStore(rootClassSource);
    }

    //

    /*
     * Verify that a dangling reference resolves as an artificial class,
     * and that a single warning is emitted during the resolution attempt.
     */
    @Test
    public void testArtificialClass() {
        TraceComponent tc = Tr.register(
            InfoStore_Internals_Test.class,
            InfoStore_Internals_Test.class.getName(),
            "com.ibm.ws.annocache.resources.internal.AnnoMessages");

        TrConfigurator.registerTraceComponent(tc);

        // A reference to "dummy.class" is created as a delayed class info object.
        // Such references occur often, for example, as parameter or method return types,
        // or as superclasses or interfaces.
        //
        // The reference is expanded by the call to 'getClassInfo'.  This will obtain
        // an artificial class info object.  A warning will be emitted.

        DelayedClassInfoImpl dummyInfo_delayed =
            new DelayedClassInfoImpl("infostoretest.dummy", infoStore);
        NonDelayedClassInfoImpl dummyInfo_nonDelayed =
            dummyInfo_delayed.getClassInfo();

        Assert.assertTrue("ClassInfo for 'infostoretest.dummy' isArtificial", dummyInfo_nonDelayed.isArtificial());

        String expectedMessage = "CWWKC0022W:";
        String altExpectedMessage = "ANNO_CLASSINFO_SCAN_EXCEPTION";
        boolean detectedMessage = 
            ( outputMgr.checkForLiteralStandardOut(expectedMessage) ||
              outputMgr.checkForLiteralStandardOut(altExpectedMessage) );

        Assert.assertTrue("Detected message [ " + expectedMessage + " ]", detectedMessage);
    }

    public static final int TARGET_CACHE_SIZE = 100;

    /*
     * Test the function of ClassInfoCache methods
     */
    @Test
    public void testClassInfoCache() {
        TestClassInfoCache cache = new TestClassInfoCache(infoStore);

        for ( int classNo = 0; classNo < TARGET_CACHE_SIZE; classNo++ ) {
            String className = "test.classinfo.Test" + String.valueOf(classNo) + ".class";
            NonDelayedClassInfoImpl nonDelayedClassInfo = new TestNonDelayedClassInfo(className, infoStore);
            cache.addClassInfo(nonDelayedClassInfo);
        }

        // Validate the cache size and linkages, then adjust the cache elements
        // and re-validate after each adjustment.

        validateCache(cache, TARGET_CACHE_SIZE);

        // Move a middle info to the front of the cache list.
        NonDelayedClassInfoImpl middleInfo50 = cache.basicGetClassInfo("test.classinfo.Test50.class");
        cache.makeFirst(middleInfo50);
        validateCache(cache, TARGET_CACHE_SIZE);

        // Remove the first cached info.
        NonDelayedClassInfoImpl firstInfo = cache.getFirstClassInfo();
        cache.removeAsDelayable(firstInfo);
        validateCache(cache, TARGET_CACHE_SIZE - 1); // Removed 1.

        // Remove the last cached info.
        NonDelayedClassInfoImpl lastInfo = cache.getLastClassInfo();
        cache.removeAsDelayable(lastInfo);
        validateCache(cache, TARGET_CACHE_SIZE - 2); // Removed 2.

        // Remove a middle cached info.
        NonDelayedClassInfoImpl middleInfo70 = cache.basicGetClassInfo("test.classinfo.Test70.class");
        cache.removeAsDelayable(middleInfo70);
        validateCache(cache, TARGET_CACHE_SIZE - 3); // Removed 3.

        // Add a new first cached info.
        String useName = "test.classinfo.TestFirst.class";
        NonDelayedClassInfoImpl expectedFirstInfo = new TestNonDelayedClassInfo(useName, infoStore);
        cache.addClassInfo(expectedFirstInfo);
        NonDelayedClassInfoImpl actualFirstInfo = cache.getFirstClassInfo();
        Assert.assertEquals("Add [" + useName + "].  Expected first [ " + expectedFirstInfo + " ] actual [ " + actualFirstInfo + " ]",
            expectedFirstInfo, actualFirstInfo);
        validateCache(cache, (TARGET_CACHE_SIZE - 3) + 1); // Removed 3 and added 1.
    }

    /**
     * Validate the contents of ClassInfoCache classInfos and the linked list pointed to
     * by firstClassInfo
     */
    private void validateCache(TestClassInfoCache classInfoCache, int expectedCacheSize) {
        Assert.assertTrue(
            "Class cache expected size [ " + String.valueOf(expectedCacheSize) + " ]" +
            " actual size [ " + String.valueOf(classInfoCache.getClassInfosSize()) + " ]",
            (classInfoCache.getClassInfosSize() == expectedCacheSize));

        Map<String, NonDelayedClassInfoImpl> cachedClassInfos = classInfoCache.getClassInfos();

        int classNo = 0;
        NonDelayedClassInfoImpl currentInfo = classInfoCache.getFirstClassInfo();
        NonDelayedClassInfoImpl priorInfo = null;

        while ( (currentInfo != null) && (classNo < expectedCacheSize) ) {

            Assert.assertTrue("Class is cached [ " + currentInfo.getName() + " ]",
                               cachedClassInfos.containsKey(currentInfo.getName()));
            Assert.assertEquals("Correct linkage at [ " + Integer.valueOf(classNo) + " ]",
                                priorInfo, currentInfo.getPriorClassInfo());

            classNo++;
            priorInfo = currentInfo;
            currentInfo = currentInfo.getNextClassInfo();
        }

        Assert.assertEquals("Expected final class [ " + Integer.valueOf(expectedCacheSize) + " ] actual [ " + Integer.valueOf(classNo) + " ]",
                            expectedCacheSize, classNo);
        Assert.assertNull("Reach null final class at [ " + Integer.valueOf(expectedCacheSize) + " ]", currentInfo);
    }

    //

    private class TestNonDelayedClassInfo extends NonDelayedClassInfoImpl {
        TestNonDelayedClassInfo(String useName, InfoStore infoStore) {
            super(useName, (InfoStoreImpl) infoStore);
        }
    }

    private class TestClassInfoCache extends ClassInfoCacheImpl {
        public TestClassInfoCache(InfoStoreImpl infoStore) {
            super(infoStore);
        }

        public int getClassInfosSize() {
            return classInfos.size();
        }

        public Map<String, NonDelayedClassInfoImpl> getClassInfos() {
            return classInfos;
        }

        public NonDelayedClassInfoImpl getFirstClassInfo() {
            return firstClassInfo;
        }

        public NonDelayedClassInfoImpl getLastClassInfo() {
            return lastClassInfo;
        }

        @Override
        public void addAsFirst(NonDelayedClassInfoImpl classInfo) {
            super.addAsFirst(classInfo);
        }

        @Override
        public boolean addClassInfo(NonDelayedClassInfoImpl classInfo) {
            return super.addClassInfo(classInfo);
        }

        @Override
        public NonDelayedClassInfoImpl basicGetClassInfo(String name) {
            return super.basicGetClassInfo(name);
        }
    }

    //

    public static class TestClassSource extends ClassSourceImpl {
        public static final TraceComponent tc =
            Tr.register(TestClassSource.class, null, "com.ibm.ws.annocache.resources.internal.AnnoMessages");

        public TestClassSource(ClassSourceImpl_Factory factory, Util_InternMap internMap, String name) {
            super(factory, internMap, NO_ENTRY_PREFIX, name, name);
        }

        //

        @Override
        public void open() throws ClassSource_Exception {
            // NO-OP
        }

        @Override
        @Trivial
        public void close() throws ClassSource_Exception {
            // NO-OP
        }

        //

        @Override
        public InputStream openResourceStream(String className,
                                              String resourceName) throws ClassSource_Exception {

            String methodName = "openResourceStream";

            IOException e = new IOException("Failed to open [ " + resourceName + " ]");
            String eMsg =
                "[ " + getHashText() + " ]" +
                " Failed to open [ " + resourceName + " ]" + " for class [ " + className + " ]" +
                " in [ " + getName() + " ]";

            throw getFactory().wrapIntoClassSourceException(CLASS_NAME, methodName, eMsg, e);
        }

        @Override
        public void closeResourceStream(String className, String resourceName, InputStream inputStream) {
            // NO-OP
        }

        //

        @Override
        protected String computeStamp() {
            throw new UnsupportedOperationException(); // Unused
        }

        @Override
        public void log(Logger useLogger) {
            throw new UnsupportedOperationException(); // Unused
        }

        @Override
        protected int processFromScratch(ClassSource_Streamer streamer) throws ClassSource_Exception {
            throw new UnsupportedOperationException(); // Unused
        }

        @Override
        public void processSpecific(ClassSource_Streamer streamer, Set<String> i_classNames) throws ClassSource_Exception {
            throw new UnsupportedOperationException(); // Unused
        }

        // Obsolete

        @Override
        public void log(TraceComponent useTc) {
            // EMPTY
        }

        @Override
        public void setParentSource(com.ibm.wsspi.anno.classsource.ClassSource classSource) {
            // EMPTY
        }

        @Override
        public void scanClasses(com.ibm.wsspi.anno.classsource.ClassSource_Streamer streamer,
            Set<String> i_seedClassNamesSet, ScanPolicy scanPolicy) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ClassSource_ScanCounts getScanResults() {
            return null;
        }

        @Override
        public int getResult(ResultField resultField) {
            return 0;
        }

        @Override
        public boolean scanSpecificSeedClass(String specificClassName,
                com.ibm.wsspi.anno.classsource.ClassSource_Streamer streamer)
                throws com.ibm.wsspi.anno.classsource.ClassSource_Exception {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean scanReferencedClass(String referencedClassNam,
                com.ibm.wsspi.anno.classsource.ClassSource_Streamer streamer)
                throws com.ibm.wsspi.anno.classsource.ClassSource_Exception {
            throw new UnsupportedOperationException();
        }

        @Override
        public String inconvertResourceName(String externalResourceName) {
            return null;
        }

        @Override
        public String outconvertResourceName(String internalResourceName) {
            return null;
        }

        @Override
        public InputStream openClassStream(String className)
                throws com.ibm.wsspi.anno.classsource.ClassSource_Exception {
            return null;
        }

        @Override
        public void closeClassStream(String className, InputStream inputStream)
            throws com.ibm.wsspi.anno.classsource.ClassSource_Exception {
            // EMPTY
        }

        @Override
        public int getResourceExclusionCount() {
            return 0;
        }

        @Override
        public int getClassExclusionCount() {
            return 0;
        }

        @Override
        public int getClassInclusionCount() {
            return 0;
        }
    }
}
