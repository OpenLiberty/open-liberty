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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestRule;

import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_Aggregate;
import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_Factory;
import com.ibm.ws.annocache.info.internal.InfoStoreFactoryImpl;
import com.ibm.ws.annocache.info.internal.InfoStoreImpl;
import com.ibm.ws.annocache.test.utils.TestLocalization;
import com.ibm.ws.annocache.util.internal.UtilImpl_Factory;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.annocache.classsource.ClassSource_ClassLoader;
import com.ibm.wsspi.annocache.classsource.ClassSource_Factory;
import com.ibm.wsspi.annocache.info.AnnotationInfo;
import com.ibm.wsspi.annocache.info.ClassInfo;
import com.ibm.wsspi.annocache.info.FieldInfo;
import com.ibm.wsspi.annocache.info.Info;
import com.ibm.wsspi.annocache.info.MethodInfo;

import test.common.SharedOutputManager;

// Abstract: Runs tests using the test environment class loader.
// The subclass API is to implement test methods.

public abstract class InfoStore_TestBase {

    protected SharedOutputManager outputMgr =
        SharedOutputManager.getInstance().trace("*=all").logTo(TestLocalization.LOGS_RELATIVE_PATH + getClass().getSimpleName());

    @Rule
    public TestRule outputRule = outputMgr;

    // Main test API.
    //
    // Setup access to a class source, to annotation targets,
    // and to an info store.

    @BeforeClass
    public static void setUp() throws Exception {
        UtilImpl_Factory utilFactory = new UtilImpl_Factory();
        classSourceFactory = new ClassSourceImpl_Factory(utilFactory);
        infoStoreFactory = new InfoStoreFactoryImpl(utilFactory);
        setUpClassSource(); // throws Exception
        setUpInfoStore(); // throws Exception
    }

    @AfterClass
    public static void tearDown() throws Exception {
        tearDownInfoStore(); // throws Exception
        tearDownClassSource(); // throws Exception
        classSourceFactory = null;
        infoStoreFactory = null;
    }

    //

    private static ClassSourceImpl_Factory classSourceFactory;
    private static InfoStoreFactoryImpl infoStoreFactory;


    public static ClassSourceImpl_Factory getClassSourceFactory() {
        return classSourceFactory;
    }

    public static InfoStoreFactoryImpl getInfoStoreFactory() {
        return infoStoreFactory;
    }

    //

    private static ClassSourceImpl_Aggregate rootClassSource;

    private static void setUpClassSource() throws Exception {
        rootClassSource = createClassSource(); // throws Exception
        rootClassSource.open();
    }

    protected static ClassSourceImpl_Aggregate createClassSource() throws Exception {
        ClassSourceImpl_Factory factory = getClassSourceFactory();

        ClassSourceImpl_Aggregate useRootClassSource =
            factory.createAggregateClassSource(
                "AppName", "ModName", ClassSource_Factory.UNSET_CATEGORY_NAME,
                factory.createOptions() );

        addComponentClassSources(useRootClassSource);

        ClassSource_ClassLoader classLoaderClassSource =
            factory.createClassLoaderClassSource(useRootClassSource, "external", getRootClassLoader());
        useRootClassSource.addClassLoaderClassSource(classLoaderClassSource);

        return useRootClassSource;
    }

    protected static void addComponentClassSources(ClassSourceImpl_Aggregate useRootClassSource)
        throws Exception {
        // Do nothing by default
    }

    public static ClassLoader getRootClassLoader() {
        return InfoStore_TestBase.class.getClassLoader();
    }

    private static void tearDownClassSource() throws Exception {
        rootClassSource.close();
        rootClassSource = null;
    }

    public static ClassSource_Aggregate getRootClassSource() {
        return rootClassSource;
    }

    //

    private static InfoStoreImpl infoStore;

    private static void setUpInfoStore() throws Exception {
        infoStore = getInfoStoreFactory().createInfoStore( getRootClassSource() );
    }

    private static void tearDownInfoStore() throws Exception {
        infoStore = null;
    }

    public static InfoStoreImpl getInfoStore() {
        return infoStore;
    }

    //

    public static ClassInfo getClassInfo(String className) {
        return getInfoStore().getDelayableClassInfo(className);
    }

    public static ClassInfo getClassInfo(Class<?> targetClass) {
        return getInfoStore().getDelayableClassInfo(targetClass.getName());
    }

    public static MethodInfo getDeclaredMethod(ClassInfo classInfo, String methodName) {
        for ( MethodInfo methodInfo : classInfo.getDeclaredMethods() ) {
            if ( methodInfo.getName().equals(methodName) ) {
                return methodInfo;
            }
        }
        return null;
    }

    public static AnnotationInfo getAnnotationInfo(String className, String annotationClassName) {
        ClassInfo classInfo = getClassInfo(className);
        if (classInfo == null) {
            return null;
        }

        return classInfo.getDeclaredAnnotation(annotationClassName);
    }

    public static AnnotationInfo getAnnotationInfo(String className, Class<? extends Annotation> annotationClass) {
        ClassInfo classInfo = getClassInfo(className);
        if (classInfo == null) {
            return null;
        }

        return classInfo.getDeclaredAnnotation(annotationClass.getName());
    }
    

    public void removeMethod(
        Collection<MethodInfo> methods,
        ClassInfo declaringClassInfo,
        String methodName, String methodDesc) {

        Iterator<MethodInfo> useMethods = methods.iterator();
        while ( useMethods.hasNext() ) {
            MethodInfo methodInfo = useMethods.next();
            if ( methodInfo.getName().equals(methodName) &&
                 methodInfo.getDescription().equals(methodDesc) &&
                 methodInfo.getDeclaringClass().getQualifiedName().equals(declaringClassInfo.getQualifiedName()) ) {
                useMethods.remove();
                return;
            }
        }

        Assert.fail("Did not find " + declaringClassInfo.getName() + '.' + methodName + " in " + methods);
    }

    public void removeField(
        Collection<? extends FieldInfo> fields,
        ClassInfo declaringClassInfo,
        String fieldName) {

        Iterator<? extends FieldInfo> useField = fields.iterator();

        while ( useField.hasNext() ) {
            FieldInfo fieldInfo = useField.next();
            if ( fieldInfo.getName().equals(fieldName) &&
                 fieldInfo.getDeclaringClass().getQualifiedName().equals(declaringClassInfo.getQualifiedName()) ) {
                useField.remove();
                return;
            }
        }

        Assert.fail("Did not find " + declaringClassInfo.getName() + '.' + fieldName + " in " + fields);
    }

    /**
     * Select all methods having a specified name.
     *
     * @param methods The methods from which to select.
     * @param methodName The name of the methods to select.
     *
     * @return The methods which have the specified name.
     */
    public List<MethodInfo> getMethods(
        Collection<? extends MethodInfo> methods,
        String methodName) {

        return getMethods(methods, methodName, (String[]) null);
    }

    /**
     * Select all methods having a specified name and parameter types.
     * 
     * @param methods The methods from which to select.
     * @param methodName The name of the methods to select.
     * @param paramTypeNames The names of the types of the parameters of the
     *     methods to select.  If null, select all methods having the
     *     specified name, regardless of their parameters.
     *
     * @return The methods which have the specified name and parameters.
     */
    public List<MethodInfo> getMethods(
        Collection<? extends MethodInfo> methods,
        String methodName,
        String... parmTypeNames) {

        ArrayList<MethodInfo> methodInfos = new ArrayList<MethodInfo>(1);

        for (MethodInfo methodInfo : methods) {
            if (!methodInfo.getName().equals(methodName) ) {
                continue;
            }
            
            if ( parmTypeNames != null ) {
                List<String> actualParmTypeNames = methodInfo.getParameterTypeNames();
                if ( actualParmTypeNames.size() != parmTypeNames.length ) {
                    continue;
                }
                
                boolean okParms = true;
                for ( int parmNo = 0; okParms && (parmNo < parmTypeNames.length); parmNo++ ) {
                    if (!parmTypeNames[parmNo].equals(actualParmTypeNames.get(parmNo))) {
                        okParms = false;
                    }
                }
                if ( !okParms ) {
                    continue;
                }
            }

            methodInfos.add(methodInfo);
        }

        return methodInfos;
    }

    public FieldInfo getField(Collection<? extends FieldInfo> fields, String fieldName) {
        for ( FieldInfo fieldInfo : fields ) {
            if ( fieldInfo.getName().equals(fieldName) ) {
                return fieldInfo;
            }
        }
        return null;
    }

    public boolean containsInfo(List<? extends Info> infos, Info info) {
        return ( getInfo(infos, info.getName()) != null );
    }

    public Info getInfo(List<? extends Info> infos, String infoName) {
        for ( Info info : infos ) {
            if ( info.getName().equals(infoName) ) {
                return info;
            }
        }
        return null;
    }
}
