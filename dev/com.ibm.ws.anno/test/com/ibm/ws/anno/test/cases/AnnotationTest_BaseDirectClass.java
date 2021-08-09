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

import java.lang.annotation.Annotation;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

import com.ibm.ws.anno.classsource.internal.ClassSourceImpl_Factory;
import com.ibm.ws.anno.info.internal.InfoStoreFactoryImpl;
import com.ibm.ws.anno.service.internal.AnnotationServiceImpl_Service;
import com.ibm.ws.anno.targets.internal.AnnotationTargetsImpl_Factory;
import com.ibm.ws.anno.util.internal.UtilImpl_Factory;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.classsource.ClassSource_Factory;
import com.ibm.wsspi.anno.info.AnnotationInfo;
import com.ibm.wsspi.anno.info.ClassInfo;
import com.ibm.wsspi.anno.info.InfoStore;
import com.ibm.wsspi.anno.info.InfoStoreFactory;

// Abstract: Runs tests using the test environment class loader.
// The subclass API is to implement test methods.

public abstract class AnnotationTest_BaseDirectClass {

    SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=all").logTo(TestConstants.BUILD_LOGS + this.getClass().getSimpleName());

    @Rule
    public TestRule outputRule = outputMgr;

    // Main test API.
    //
    // Setup access to a class source, to annotation targets,
    // and to an info store.

    @Before
    public void setUp() throws Exception {

        setUpAnnotationService(); // throws Exception
        setUpClassLoader(); // throws Exception
        setUpClassSource(); // throws Exception
        //        setUpAnnotationTargets(); // throws Exception
        setUpInfoStore(); // throws Exception
    }

    @After
    public void tearDown() throws Exception {

        tearDownInfoStore(); // throws Exception
        //        tearDownAnnotationTargets(); // throws Exception
        tearDownClassSource(); // throws Exception
        tearDownClassLoader(); // throws Exception
        tearDownAnnotationService(); // throws Exception

    }

    //  Test services

    // Provide access to the central annotation service.
    //
    // The central annotation service provides the gateway to
    // the component services: Class source, annotation targets, and info store.

    private AnnotationServiceImpl_Service annotationService;

    public AnnotationServiceImpl_Service getAnnotationService() {
        return annotationService;
    }

    private void setUpAnnotationService() throws Exception {
        UtilImpl_Factory utilFactory = new UtilImpl_Factory();
        ClassSourceImpl_Factory classSourceFactory = new ClassSourceImpl_Factory(utilFactory);
        AnnotationTargetsImpl_Factory annotationTargetsFactory = new AnnotationTargetsImpl_Factory(utilFactory, classSourceFactory);
        InfoStoreFactoryImpl infoStoreFactory = new InfoStoreFactoryImpl(utilFactory);
        annotationService = new AnnotationServiceImpl_Service(null, utilFactory, classSourceFactory, annotationTargetsFactory, infoStoreFactory);

    }

    private void tearDownAnnotationService() throws Exception {
        annotationService = null;
    }

    public ClassSource_Factory getClassSourceFactory() {
        return getAnnotationService().getClassSourceFactory();
    }

    //    public AnnotationTargetsImpl_Factory getAnnotationTargetsFactory() {
    //        return getAnnotationService().getAnnotationTargetsFactory();
    //    }

    public InfoStoreFactory getInfoStoreFactory() {
        return getAnnotationService().getInfoStoreFactory();
    }

    // Setup the root class loader, the class source, targets, and the info store:

    // Setup the root class loader for the tests.  This setup
    // uses the test class loader itself as the class loader for tests.

    private ClassLoader testClassLoader;

    private void setUpClassLoader() throws Exception {
        testClassLoader = getRootClassLoader();
    }

    private void tearDownClassLoader() throws Exception {
        testClassLoader = null;
    }

    public ClassLoader getTestClassLoader() {
        return this.testClassLoader;
    }

    // The class source is mapped directly to the test class loader.

    private ClassSource_Aggregate classSource;

    private void setUpClassSource() throws Exception {
        classSource = getClassSourceFactory().createAggregateClassSource("RootClassSource");

        ((ClassSourceImpl_Factory) getClassSourceFactory()).addClassLoaderClassSource(classSource, "RootClassLoaderClassSource", getTestClassLoader());

        classSource.open();
    }

    private void tearDownClassSource() throws Exception {
        classSource.close();

        classSource = null;
    }

    public ClassSource_Aggregate getTestClassSource() {
        return classSource;
    }

    //

    // Disabling the targets code: The class loader based class source scans no classes.
    // The resulting targets table will be empty.
    //
    //    private AnnotationTargetsImpl_Targets annotationTargets;
    //
    //    private void setUpAnnotationTargets() throws Exception {
    //        annotationTargets = getAnnotationTargetsFactory().createTargets();
    //
    //        AnnotationTargetsImpl_Scanner testAnnotationScanner =
    //                        getAnnotationTargetsFactory().createScanner(getTestClassSource(), annotationTargets);
    //
    //        testAnnotationScanner.scan();
    //    }
    //
    //    private void tearDownAnnotationTargets() throws Exception {
    //        annotationTargets = null;
    //    }
    //
    //    public AnnotationTargetsImpl_Targets getAnnotationTargets() {
    //        return annotationTargets;
    //    }

    //

    private InfoStore infoStore;

    private void setUpInfoStore() throws Exception {
        infoStore = getInfoStoreFactory().createInfoStore(getTestClassSource());
    }

    private void tearDownInfoStore() throws Exception {
        infoStore = null;
    }

    public InfoStore getInfoStore() {
        return this.infoStore;
    }

    // Externalization: A root class loader must be supplied.

    // Use the testing class loader as the root class loader for
    // direct info access testing.
    //
    // That allows test data classes to be placed directly as source in the unit
    // test project.

    public ClassLoader getRootClassLoader() {
        return AnnotationTest_BaseDirectClass.class.getClassLoader();
    }

    // Extra info helpers

    public ClassInfo getClassInfo(String className) {
        return getInfoStore().getDelayableClassInfo(className);
    }

    public ClassInfo getClassInfo(Class<?> targetClass) {
        return getInfoStore().getDelayableClassInfo(targetClass.getName());
    }

    public AnnotationInfo getAnnotationInfo(String className, String annotationClassName) {
        ClassInfo classInfo = getClassInfo(className);
        if (classInfo == null) {
            return null;
        }

        return classInfo.getDeclaredAnnotation(annotationClassName);
    }

    public AnnotationInfo getAnnotationInfo(String className, Class<? extends Annotation> annotationClass) {
        ClassInfo classInfo = getClassInfo(className);
        if (classInfo == null) {
            return null;
        }

        return classInfo.getDeclaredAnnotation(annotationClass.getName());
    }
}