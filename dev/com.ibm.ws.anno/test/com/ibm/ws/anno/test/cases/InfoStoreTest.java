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
package com.ibm.ws.anno.test.cases;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.annotation.Resource.AuthenticationType;
import javax.xml.bind.annotation.XmlAttachmentRef;
import javax.xml.bind.annotation.XmlList;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TrConfigurator;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.anno.classsource.internal.ClassSourceImpl;
import com.ibm.ws.anno.classsource.internal.ClassSourceImpl_Factory;
import com.ibm.ws.anno.info.internal.DelayedClassInfo;
import com.ibm.ws.anno.info.internal.InfoStoreFactoryImpl;
import com.ibm.ws.anno.info.internal.InfoStoreImpl;
import com.ibm.ws.anno.info.internal.NonDelayedClassInfo;
import com.ibm.ws.anno.service.internal.AnnotationServiceImpl_Service;
import com.ibm.ws.anno.targets.internal.AnnotationTargetsImpl_Factory;
import com.ibm.ws.anno.test.data.BClass;
import com.ibm.ws.anno.test.data.CIntf;
import com.ibm.ws.anno.test.data.DerivedBase;
import com.ibm.ws.anno.test.data.sub.InheritAnno;
import com.ibm.ws.anno.test.data.sub.SubBase;
import com.ibm.ws.anno.util.internal.UtilImpl_Factory;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.classsource.ClassSource_Exception;
import com.ibm.wsspi.anno.classsource.ClassSource_ScanCounts;
import com.ibm.wsspi.anno.classsource.ClassSource_Streamer;
import com.ibm.wsspi.anno.info.AnnotationInfo;
import com.ibm.wsspi.anno.info.AnnotationValue;
import com.ibm.wsspi.anno.info.ClassInfo;
import com.ibm.wsspi.anno.info.FieldInfo;
import com.ibm.wsspi.anno.info.Info;
import com.ibm.wsspi.anno.info.InfoStore;
import com.ibm.wsspi.anno.info.InfoStoreException;
import com.ibm.wsspi.anno.info.InfoStoreFactory;
import com.ibm.wsspi.anno.info.MethodInfo;
import com.ibm.wsspi.anno.util.Util_InternMap;

import junit.framework.Assert;
import test.common.SharedOutputManager;

/**
 *
 */
public class InfoStoreTest {
    static InfoStore infoStore;
    private static final String SELF_ANNO_TARGET = "com.ibm.ws.anno.test.data.SelfAnnoTarget";
    private static final String SELF_ANNO = "com.ibm.ws.anno.test.data.sub.SelfAnno";

    SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=all").logTo(TestConstants.BUILD_LOGS + this.getClass().getSimpleName());

    @Rule
    public TestRule outputRule = outputMgr;

    @BeforeClass
    public static void setup() throws ClassSource_Exception, InfoStoreException {
        UtilImpl_Factory utilFactory = new UtilImpl_Factory();
        ClassSourceImpl_Factory classSourceFactory = new ClassSourceImpl_Factory(utilFactory);
        AnnotationTargetsImpl_Factory annotationTargetsFactory = new AnnotationTargetsImpl_Factory(utilFactory, classSourceFactory);
        InfoStoreFactoryImpl infoStoreFactory = new InfoStoreFactoryImpl(utilFactory);
        AnnotationServiceImpl_Service annoService = new AnnotationServiceImpl_Service(null, utilFactory, classSourceFactory, annotationTargetsFactory, infoStoreFactory);

        
        ClassSource_Aggregate classSource = classSourceFactory.createAggregateClassSource("AnnoInfoTest");
        String testClassesDir = System.getProperty("test.classesDir", "bin");
        classSourceFactory.addDirectoryClassSource(classSource, testClassesDir, testClassesDir, ScanPolicy.SEED);

        InfoStoreFactory infoFactory = annoService.getInfoStoreFactory();
        infoStore = infoFactory.createInfoStore(classSource);
    }

    @Test
    public void testPrivateMethods() {
        ClassInfo subInfo = infoStore.getDelayableClassInfo(SubBase.class.getCanonicalName());
        ClassInfo bInfo = infoStore.getDelayableClassInfo(BClass.class.getCanonicalName());
        Assert.assertNotNull("SubBase", subInfo);
        Assert.assertNotNull("BClass", bInfo);

        List<MethodInfo> methodInfos = getMethods(bInfo.getMethods(), "privateMethod");
        Assert.assertEquals(2, methodInfos.size());

        List<ClassInfo> classInfos = new ArrayList<ClassInfo>(methodInfos.size());
        for (MethodInfo mi : methodInfos) {
            classInfos.add(mi.getDeclaringClass());
        }
        Assert.assertTrue("subInfo: " + subInfo + " not in " + classInfos, contains(classInfos, subInfo));
        Assert.assertTrue("bInfo: " + bInfo + " not in " + classInfos, contains(classInfos, bInfo));
    }

    @Test
    public void testParameterAndAnnoOrder() {
        ClassInfo subInfo = infoStore.getDelayableClassInfo(SubBase.class.getCanonicalName());
        List<MethodInfo> methodInfos = getMethods(subInfo.getMethods(), "annoMethod");
        Assert.assertEquals(1, methodInfos.size());

        MethodInfo mi = methodInfos.get(0);
        Collection<String> parmTypes = mi.getParameterTypeNames();
        Assert.assertEquals(4, parmTypes.size());
        Assert.assertEquals(Arrays.asList("int", String.class.getName(), List.class.getName(), "long"), parmTypes);

        List<List<? extends AnnotationInfo>> parmAnnos = mi.getParameterAnnotations();
        Assert.assertEquals(4, parmAnnos.size());

        Assert.assertEquals(0, parmAnnos.get(0).size());
        Assert.assertEquals(0, parmAnnos.get(1).size());

        List<? extends AnnotationInfo> annos = parmAnnos.get(2);
        Assert.assertEquals(1, annos.size());
        Assert.assertEquals(XmlList.class.getName(), annos.get(0).getAnnotationClassName());

        annos = parmAnnos.get(3);
        Assert.assertEquals(1, annos.size());
        Assert.assertEquals(XmlAttachmentRef.class.getName(), annos.get(0).getAnnotationClassName());

    }

    @Test
    public void testOverrideWithAnnotation() {
        ClassInfo cInfo = infoStore.getDelayableClassInfo(CIntf.class.getCanonicalName());
        ClassInfo bInfo = infoStore.getDelayableClassInfo(BClass.class.getCanonicalName());
        ClassInfo integerClassInfo = infoStore.getDelayableClassInfo(Integer.class.getCanonicalName());
        Assert.assertNotNull("CIntf", cInfo);
        Assert.assertNotNull("BClass", bInfo);
        Assert.assertNotNull("Integer", integerClassInfo);

        List<MethodInfo> methodInfos = getMethods(bInfo.getMethods(), "n");
        Assert.assertEquals(2, methodInfos.size());

        MethodInfo nMethod = methodInfos.get(0);
        Assert.assertEquals(bInfo.getQualifiedName(), nMethod.getDeclaringClass().getQualifiedName());
        Assert.assertEquals(integerClassInfo, nMethod.getReturnType());
        Assert.assertEquals(1, nMethod.getAnnotations().size());
    }

    @Test
    public void testClassAnnotations() {
        ClassInfo info = infoStore.getDelayableClassInfo(SubBase.class.getCanonicalName());
        Assert.assertEquals(1, info.getDeclaredAnnotations().size());
        Assert.assertEquals(1, info.getAnnotations().size());
        Assert.assertTrue(info.isAnnotationPresent("com.ibm.ws.anno.test.data.sub.InheritAnno"));
        Assert.assertTrue(info.getAnnotation("com.ibm.ws.anno.test.data.sub.InheritAnno").isInherited());
        Assert.assertFalse(info.isAnnotationPresent("javax.annotation.Resource"));
        AnnotationInfo annoInfo = info.getAnnotation(InheritAnno.class);
        List<? extends AnnotationValue> values = annoInfo.getArrayValue("value");
        Assert.assertEquals(2, values.size());
        Assert.assertEquals("a", values.get(0).getStringValue());
        Assert.assertEquals("b", values.get(1).getStringValue());

        info = infoStore.getDelayableClassInfo(BClass.class.getCanonicalName());
        Assert.assertEquals(1, info.getDeclaredAnnotations().size());
        Assert.assertEquals(2, info.getAnnotations().size());
        Assert.assertTrue(info.isAnnotationPresent("com.ibm.ws.anno.test.data.sub.InheritAnno"));
        Assert.assertTrue(info.getAnnotation("com.ibm.ws.anno.test.data.sub.InheritAnno").isInherited());
        Assert.assertTrue(info.isAnnotationPresent("javax.annotation.Resource"));

        annoInfo = info.getAnnotation(Resource.class);
        Assert.assertEquals("/B", annoInfo.getValue("name").getStringValue());
        AnnotationValue annoValue = annoInfo.getValue("authenticationType");
        Assert.assertEquals(AuthenticationType.class.getName(), annoValue.getEnumClassName());
        Assert.assertEquals(AuthenticationType.APPLICATION.name(), annoValue.getStringValue());
    }

    @Test
    public void testMethodAnnotations() {
        ClassInfo info = infoStore.getDelayableClassInfo(SubBase.class.getCanonicalName());
        List<? extends MethodInfo> declaredMethods = info.getMethods();
        List<MethodInfo> methods = getMethods(declaredMethods, "publicMethod");
        Assert.assertEquals(2, methods.size());
        methods = getMethods(declaredMethods, "publicMethod", new String[0]);
        Assert.assertEquals(1, methods.size());
        MethodInfo method = methods.get(0);
        Collection<? extends AnnotationInfo> annos = method.getAnnotations();
        Assert.assertEquals(1, annos.size());
        annos = method.getDeclaredAnnotations();
        Assert.assertEquals(1, annos.size());
        Assert.assertTrue(method.isAnnotationPresent("org.junit.Test"));
        AnnotationInfo annoInfo = method.getAnnotation(Test.class);
        Assert.assertEquals(10000, annoInfo.getValue("timeout").getIntValue());

        info = infoStore.getDelayableClassInfo(BClass.class.getCanonicalName());
        methods = getMethods(info.getMethods(), "publicMethod");
        Assert.assertEquals(3, methods.size()); // Integer & Number
        method = methods.get(0);
        if (!method.getReturnTypeName().equals("java.lang.Integer")) {
            method = methods.get(1);
        }
        annos = method.getAnnotations();
        Assert.assertEquals(0, annos.size());
        annos = method.getDeclaredAnnotations();
        Assert.assertEquals(0, annos.size());
        Assert.assertFalse(method.isAnnotationPresent("org.junit.Test"));

        // other test above in OverrideWithAnnotation
    }

    @Test
    public void testFieldAnnotations() {
        ClassInfo info = infoStore.getDelayableClassInfo(SubBase.class.getCanonicalName());
        FieldInfo field = getField(info.getDeclaredFields(), "public1");
        Assert.assertNotNull(field);
        Collection<? extends AnnotationInfo> annos = field.getAnnotations();
        Assert.assertEquals(1, annos.size());
        annos = field.getDeclaredAnnotations();
        Assert.assertEquals(1, annos.size());
        Assert.assertTrue(field.isAnnotationPresent("javax.persistence.Id"));

        info = infoStore.getDelayableClassInfo(DerivedBase.class.getCanonicalName());
        field = getField(info.getDeclaredFields(), "public1");
        Assert.assertNotNull(field);
        annos = field.getAnnotations();
        Assert.assertEquals(0, annos.size());
        annos = field.getDeclaredAnnotations();
        Assert.assertEquals(0, annos.size());
        Assert.assertFalse(field.isAnnotationPresent("javax.persistence.Id"));
    }

    @Test
    public void testDeclaredFields() {
        ClassInfo subInfo = infoStore.getDelayableClassInfo(SubBase.class.getCanonicalName());
        ClassInfo derivedInfo = infoStore.getDelayableClassInfo(DerivedBase.class.getCanonicalName());

        Collection<FieldInfo> subFields = new ArrayList<FieldInfo>(subInfo.getDeclaredFields());
        removeField(subFields, subInfo, "public1");
        removeField(subFields, subInfo, "public2");
        removeField(subFields, subInfo, "protected1");
        removeField(subFields, subInfo, "protected2");
        removeField(subFields, subInfo, "package1");
        removeField(subFields, subInfo, "package2");
        removeField(subFields, subInfo, "private1");
        Assert.assertEquals(subFields.toString(), 0, subFields.size());

        Collection<FieldInfo> derivedFields = new ArrayList<FieldInfo>(derivedInfo.getDeclaredFields());
        removeField(derivedFields, derivedInfo, "public1");
        removeField(derivedFields, derivedInfo, "protected2");
        removeField(derivedFields, derivedInfo, "package1");
        removeField(derivedFields, derivedInfo, "private2");
        Assert.assertEquals(subFields.toString(), 0, derivedFields.size());
    }

    @Test
    public void testMethods() {
        ClassInfo subInfo = infoStore.getDelayableClassInfo(SubBase.class.getCanonicalName());
        Collection<MethodInfo> methods = new ArrayList<MethodInfo>(subInfo.getMethods());
        removeMethod(methods, subInfo, "publicMethod", "()V");
        removeMethod(methods, subInfo, "publicMethod", "(I)Ljava/lang/Number;");
        removeMethod(methods, subInfo, "protectedMethod", "()Ljava/util/Map;");
        removeMethod(methods, subInfo, "packageMethod", "()I");
        removeMethod(methods, subInfo, "privateMethod", "()V");
        removeMethod(methods, subInfo, "annoMethod", "(ILjava/lang/String;Ljava/util/List;J)V");
        Assert.assertEquals(methods.toString(), 0, methods.size());

        ClassInfo bInfo = infoStore.getDelayableClassInfo(BClass.class.getCanonicalName());
        methods = new ArrayList<MethodInfo>(bInfo.getMethods());
        removeMethod(methods, bInfo, "publicMethod", "()V");
        removeMethod(methods, bInfo, "publicMethod", "(I)Ljava/lang/Number;"); // Number
        removeMethod(methods, bInfo, "publicMethod", "(I)Ljava/lang/Integer;"); // Integer
        removeMethod(methods, subInfo, "protectedMethod", "()Ljava/util/Map;");
        removeMethod(methods, bInfo, "privateMethod", "()V");
        removeMethod(methods, subInfo, "privateMethod", "()V");
        removeMethod(methods, bInfo, "n", "()Ljava/lang/Number;"); // Number - BClass
        removeMethod(methods, bInfo, "n", "()Ljava/lang/Integer;"); // Integer - IntfC
        removeMethod(methods, subInfo, "annoMethod", "(ILjava/lang/String;Ljava/util/List;J)V");
        Assert.assertEquals(methods.toString(), 0, methods.size());
    }

    @Test
    public void testDefaultMethod() {
        ClassInfo info = infoStore.getDelayableClassInfo(InheritAnno.class.getCanonicalName());
        List<MethodInfo> methods = getMethods(info.getMethods(), "defaultValue");
        Assert.assertEquals(1, methods.size());
        Assert.assertEquals("abc123", methods.get(0).getAnnotationDefaultValue().getStringValue());

        methods = getMethods(info.getMethods(), "value");
        Assert.assertEquals(1, methods.size());
        Assert.assertNull(methods.get(0).getAnnotationDefaultValue());
    }

    /*
     * Test that only one Warning message is produced from the InfoStoreImpl.getClassSource() path when a resource stream
     * can not be created.
     */
    @Test
    public void testErrorMessages() {

        TraceComponent tc = Tr.register(InfoStoreTest.class, InfoStoreTest.class.getName(), "com.ibm.ws.anno.resources.internal.AnnoMessages");

        TrConfigurator.registerTraceComponent(tc);

        ClassSource_Aggregate aggie = infoStore.getClassSource();

        TestClassSource source = new TestClassSource((ClassSourceImpl_Factory) aggie.getFactory(), aggie.getInternMap(), "Dummy ClassSource");

        aggie.addClassSource(source);
        DelayedClassInfo dummyInfo = new DelayedClassInfo("infostoretest.dummy", (com.ibm.ws.anno.info.internal.InfoStoreImpl) infoStore);
        // dummy.class does not exist so it will not be found. When ClassSourceImpl_Test is invoked it will throw an
        // exception which should result in the display of a Warning message.
        NonDelayedClassInfo ndInfo = dummyInfo.getClassInfo();

        String expectedMessage = "CWWKC0022W:";

        // String msgs = outputMgr.getCapturedOut();

        // capture any failures and dump the captured messages for diagnosis
        try {
            Assert.assertTrue("DelayedClassInfo.getClassInfo() for infostoretest.dummy.class should have returned a NonDelayedClassInfo marked artificial",
                              ndInfo.isArtificial());

            Assert.assertTrue("System.out should contain " + expectedMessage, outputMgr.checkForLiteralStandardOut(expectedMessage));

         
        } catch (junit.framework.AssertionFailedError err) {
            outputMgr.dumpStreams();
            throw err;
        } finally {
            outputMgr.restoreStreams();
        }
    }

    /*
     * Test the function of ClassInfoCache methods
     */
    @Test
    public void testClassInfoCache() {
        TestClassInfoCache cache = new TestClassInfoCache((InfoStoreImpl) infoStore);
        // build a set of test ClassInfos
        for (int x = 0; x < 100; x++) {
            String useName = "test.classinfo.Test" + String.valueOf(x) + ".class";
            NonDelayedClassInfo nd = new TestNonDelayedClassInfo(useName, infoStore);
            cache.addClassInfo(nd);
        }

        validateClassInfos(cache, 100);

        // Remove a series of ClassInfo objects and verify that the count and linked list are correct
        String middle = "test.classinfo.Test50.class";
        NonDelayedClassInfo cInfo;

        // make middle first
        cInfo = cache.basicGetClassInfo(middle);
        cache.makeFirst(cInfo);
        validateClassInfos(cache, 100);

        // remove first, that should leave 99
        NonDelayedClassInfo firstCInfo = cache.getFirstClassInfo();
        cache.removeAsDelayable(firstCInfo);
        validateClassInfos(cache, 99);

        // remove last, that should leave 98
        NonDelayedClassInfo midCInfo = cache.getLastClassInfo();
        cache.removeAsDelayable(midCInfo);
        validateClassInfos(cache, 98);

        // remove one from the middle, that should leave 97
        cInfo = cache.basicGetClassInfo("test.classinfo.Test70.class");
        cache.removeAsDelayable(cInfo);
        validateClassInfos(cache, 97);

        // add a new one and verify it is first in the list
        String useName = "test.classinfo.TestFirst.class";
        NonDelayedClassInfo nd = new TestNonDelayedClassInfo(useName, infoStore);
        cache.addClassInfo(nd);
        cInfo = cache.getFirstClassInfo();
        Assert.assertEquals("Added classInfo [" + useName + "] but it is not first on the list", nd, cInfo);
        validateClassInfos(cache, 98); // just added a new one so count should be 98

    }

    @Test
    public void testRecursiveClassAnnotation() {
        ClassInfo selfAnnoTargetInfo = infoStore.getDelayableClassInfo(SELF_ANNO_TARGET);
        Assert.assertNotNull("Loaded [ " + SELF_ANNO_TARGET + " ]", selfAnnoTargetInfo);

        ClassInfo selfAnnoInfo = infoStore.getDelayableClassInfo(SELF_ANNO);
        Assert.assertNotNull("Loaded [ " + SELF_ANNO + " ]", selfAnnoInfo);
        Assert.assertTrue("Class [ " + SELF_ANNO + " ] is an annotation class", selfAnnoInfo.isAnnotationClass());

        Assert.assertTrue("Class [ " + SELF_ANNO_TARGET + " ] has annotation [ " + SELF_ANNO + " ]",
                          selfAnnoTargetInfo.isAnnotationPresent(SELF_ANNO));

        Assert.assertTrue("Class [ " + SELF_ANNO + " ] has annotation [ " + SELF_ANNO + " ]",
                          selfAnnoInfo.isAnnotationPresent(SELF_ANNO));
    }

    /**
     * Validate the contents of ClassInfoCache classInfos and the linked list pointed to
     * by firstClassInfo
     *
     * @param cache
     * @param i expected size of the classInfos map
     */
    private void validateClassInfos(TestClassInfoCache cache, int i) {
        Assert.assertTrue("ClassInfos map should contain " + String.valueOf(i) + " objects but contains " + String.valueOf(cache.getClassInfosSize()),
                          (cache.getClassInfosSize() == i));

        NonDelayedClassInfo classInfo = cache.getFirstClassInfo();
        NonDelayedClassInfo priorInfo = null;
        int x = 0;
        Map<String, NonDelayedClassInfo> classInfos = cache.getClassInfos();
        while (classInfo != null && x < i) {
            x++;
            // verify that the current ClassInfo is in the list
            Assert.assertTrue("The classInfos map does not contain the classInfo for: " + classInfo.getName(),
                              classInfos.containsKey(classInfo.getName()));

            // verify that the back pointer correctly points back to the prior ClassInfo
            Assert.assertEquals("The backward pointer for item number " + String.valueOf(x + 1) + "is not correct",
                                priorInfo, classInfo.getPriorClassInfo());
            priorInfo = classInfo;
            classInfo = classInfo.getNextClassInfo();

        }

        Assert.assertEquals("End of linked list of pointed to by ClassInfoCache.firstClassInfo not found after exceeding the map size",
                            i, x);
        Assert.assertNull("End of linked list of pointed to by ClassInfoCache.firstClassInfo not found",
                          classInfo);

    }

    private class TestNonDelayedClassInfo extends NonDelayedClassInfo {

        TestNonDelayedClassInfo(String useName, InfoStore infoStore) {

            super(useName, (InfoStoreImpl) infoStore);
        }
    }

    private class TestClassInfoCache extends com.ibm.ws.anno.info.internal.ClassInfoCache {

        /**
         * @param infoStore
         */
        public TestClassInfoCache(InfoStoreImpl infoStore) {
            super(infoStore);
            // TODO Auto-generated constructor stub
        }

        public int getClassInfosSize() {
            return classInfos.size();
        }

        public Map<String, NonDelayedClassInfo> getClassInfos() {
            return classInfos;
        }

        public NonDelayedClassInfo getFirstClassInfo() {
            return firstClassInfo;
        }

        public NonDelayedClassInfo getLastClassInfo() {
            return lastClassInfo;
        }

        @Override
        public void addAsFirst(NonDelayedClassInfo classInfo) {
            super.addAsFirst(classInfo);
        }

        @Override
        public boolean addClassInfo(NonDelayedClassInfo classInfo) {
            return super.addClassInfo(classInfo);
        }

        @Override
        public NonDelayedClassInfo basicGetClassInfo(String name) {
            return super.basicGetClassInfo(name);
        }
    }

    /**
     * @param methods
     * @param subInfo
     * @param string
     */
    private void removeMethod(Collection<MethodInfo> methods, ClassInfo classInfo, String methodName, String desc) {
        for (Iterator<MethodInfo> methodIter = methods.iterator(); methodIter.hasNext();) {
            MethodInfo m = methodIter.next();
            if (m.getName().equals(methodName) && m.getDescription().equals(desc)) {
                if (m.getDeclaringClass().getQualifiedName().equals(classInfo.getQualifiedName())) {
                    methodIter.remove();
                    return;
                }
            }
        }
        Assert.fail("Did not find " + classInfo.getName() + '.' + methodName + " in " + methods);
    }

    /**
     * @param subFields
     * @param subInfo
     * @param string
     */
    private void removeField(Collection<? extends FieldInfo> subFields, ClassInfo classInfo, String fieldName) {
        for (Iterator<? extends FieldInfo> fieldIter = subFields.iterator(); fieldIter.hasNext();) {
            FieldInfo f = fieldIter.next();
            if (f.getName().equals(fieldName)) {
                if (f.getDeclaringClass().getQualifiedName().equals(classInfo.getQualifiedName())) {
                    fieldIter.remove();
                    return;
                }
            }
        }
        Assert.fail("Did not find " + classInfo.getName() + '.' + fieldName + " in " + subFields);
    }

    private List<MethodInfo> getMethods(Collection<? extends MethodInfo> methods, String methodName) {
        return getMethods(methods, methodName, (String[]) null);
    }

    private List<MethodInfo> getMethods(Collection<? extends MethodInfo> methods, String methodName, String... parms) {
        ArrayList<MethodInfo> methodInfos = new ArrayList<MethodInfo>(5);

        for (MethodInfo methodInfo : methods) {
            if (methodInfo.getName().equals(methodName)) {
                if (parms != null) {
                    List<String> methodParms = methodInfo.getParameterTypeNames();
                    if (methodParms.size() != parms.length)
                        continue;
                    for (int i = 0; i < parms.length; ++i) {
                        if (!!!parms[i].equals(methodParms.get(i))) {
                            continue;
                        }
                    }
                }
                methodInfos.add(methodInfo);
            }
        }

        return methodInfos;
    }

    private FieldInfo getField(Collection<? extends FieldInfo> fields, String name) {
        for (FieldInfo f : fields) {
            if (f.getName().equals(name)) {
                return f;
            }
        }
        return null;

    }

    static private boolean contains(List<? extends Info> infos, Info info) {
        for (Info i : infos) {
            if (i.getName().equals(info.getName()))
                return true;
        }

        return false;
    }

    /**
     * Create a test ClassSource that can be used to generate errors to drive unit tests
     */
    private class TestClassSource extends ClassSourceImpl {

        private final TraceComponent tc = Tr.register(TestClassSource.class, null, "com.ibm.ws.anno.resources.internal.AnnoMessages");

        public TestClassSource(ClassSourceImpl_Factory factory, Util_InternMap internMap,
                               String name) {
            super(factory, internMap, name, factory.createOptions(), "TESTHASH");

            // tc.setResourceBundleName("com.ibm.ws.anno.resources.internal.AnnoMessages");

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, this.hashText);
            }
        }

        /**
         * Counter to keep track of the number of active users. Incremented for each 'open'
         * and decremented for each 'close'. The underlying ZipFile will be closed when the
         * count goes to 0.
         */
        protected int opens;

        /**
         * Open the ClassSource for processing. If this is the first open, the underlying jar
         * file will be opened.
         */
        @Override
        public void open() throws ClassSource_Exception {
            String methodName = "open";
            if (tc.isEntryEnabled()) {
                Tr.entry(tc, methodName, MessageFormat.format("[ {0} ] State [ {1} ]",
                                                              new Object[] { getHashText(), Integer.valueOf(this.opens) }));
            }

            this.opens++;

        }

        /**
         * Close the ClassSource for processing. If the open counter goes to zero, then close and release the
         * associated JarFile.
         */
        @Override
        @Trivial
        public void close() throws ClassSource_Exception {
            String methodName = "close";
            if (tc.isEntryEnabled()) {
                Tr.entry(tc, methodName, getHashText());
            }

            // Dummy ClassSource, don't do anything
            this.opens--;
            if (tc.isEntryEnabled()) {
                Tr.exit(tc, methodName, getHashText());
            }
        }

        @Override
        public int getResult(ClassSource_ScanCounts.ResultField resultField) {
            return 0;
        }

        @Override
        public void processFromScratch(ClassSource_Streamer streamer, Set<String> i_seedClassNames, ScanPolicy scanPolicy) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, MessageFormat.format("[ {0} ] ENTER", getHashText()));
            }
            // Dummy ClassSource, don't do anything
        }

        //

        @Override
        public InputStream openResourceStream(String className,
                                              String resourceName) throws ClassSource_Exception {

            String methodName = "openResourceStream";
            if (!className.startsWith("infostoretest.")) {
                //not the test class
                return null;
            }

            IOException e = new IOException("openResourceStream test exception");
            String eMsg = "[ " + getHashText() + " ]" +
                          " Failed to open [ " + resourceName + " ]" + " for class [ " + className + " ]" +
                          " in [ DUMMY ]";
            throw getFactory().wrapIntoClassSourceException(CLASS_NAME, methodName, eMsg, e);

        }

        @Override
        public void closeResourceStream(String className, String resourceName, InputStream inputStream) {
            try {
                inputStream.close(); // throws IOException

            } catch (IOException e) {
                Tr.warning(tc, "ANNO_CLASSSOURCE_CLOSE6_EXCEPTION",
                           getHashText(), resourceName, className, "test/classSource");
            }
        }

        @Override
        public void log(TraceComponent logger) {
            Tr.debug(logger, MessageFormat.format("Class Source [ {0} ]", getHashText()));

            logCounts(logger);
        }
    }
}
