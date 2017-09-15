/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.anno.classsource.internal;

import java.text.MessageFormat;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.anno.classsource.specification.ClassSource_Specification_Container_WAR;
import com.ibm.ws.anno.classsource.specification.internal.ClassSourceImpl_Specification_Container_EJB;
import com.ibm.ws.anno.classsource.specification.internal.ClassSourceImpl_Specification_Container_WAR;
import com.ibm.ws.anno.classsource.specification.internal.ClassSourceImpl_Specification_Direct_Bundle;
import com.ibm.ws.anno.classsource.specification.internal.ClassSourceImpl_Specification_Direct_EJB;
import com.ibm.ws.anno.classsource.specification.internal.ClassSourceImpl_Specification_Direct_WAR;
import com.ibm.ws.anno.service.internal.AnnotationServiceImpl_Logging;
import com.ibm.ws.anno.util.internal.UtilImpl_Factory;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.classsource.ClassSource_Exception;
import com.ibm.wsspi.anno.classsource.ClassSource_Factory;
import com.ibm.wsspi.anno.classsource.ClassSource_MappedSimple;
import com.ibm.wsspi.anno.util.Util_InternMap;

public class ClassSourceImpl_Factory implements ClassSource_Factory {
    public static final String CLASS_NAME = ClassSourceImpl_Factory.class.getName();
    private static final TraceComponent tc = Tr.register(ClassSourceImpl_Factory.class);

    //

    protected final String hashText;

    @Override
    public String getHashText() {
        return hashText;
    }

    //

    public ClassSourceImpl_Factory(UtilImpl_Factory utilFactory) {
        super();

        String methodName = "init";

        this.hashText = AnnotationServiceImpl_Logging.getBaseHash(this);

        this.utilFactory = utilFactory;

        if (tc.isDebugEnabled()) {
            Tr.debug(methodName, tc, MessageFormat.format("[ {0} ] Created",
                                                          this.hashText));
            Tr.debug(methodName, tc, MessageFormat.format("[ {0} ] Util Factory [ {1} ]",
                                                          new Object[] { this.hashText, this.utilFactory.getHashText() }));
        }
    }

    //

    protected final UtilImpl_Factory utilFactory;

    @Override
    public UtilImpl_Factory getUtilFactory() {
        return utilFactory;
    }

    //

    @Override
    public ClassSource_Exception newClassSourceException(String message) {
        ClassSource_Exception exception = new ClassSource_Exception(message);

        return exception;
    }

    @Override
    public ClassSource_Exception wrapIntoClassSourceException(String callingClassName,
                                                              String callingMethodName,
                                                              String message, Throwable th) {

        String methodName = "wrapIntoClassSourceException";

        ClassSource_Exception wrappedException = new ClassSource_Exception(message, th);

        if (tc.isDebugEnabled()) {
            Tr.debug(methodName, tc, MessageFormat.format("[ {0} ] [ {1} ] Wrap [ {2} ] as [ {3} ]",
                                                          new Object[] { callingClassName, callingMethodName,
                                                                        th.getClass().getName(),
                                                                        wrappedException.getClass().getName() }));

        }

        return wrappedException;
    }

    //

    @Override
    public String getCanonicalName(String classSourceName) {
        return classSourceName.replace('\\', '/');
    }

    //

    @Override
    public ClassSourceImpl_Aggregate createAggregateClassSource(String name) throws ClassSource_Exception {
        Util_InternMap classInternMap = getUtilFactory().createInternMap(Util_InternMap.ValueType.VT_CLASS_NAME, "classes and packages");

        return createAggregateClassSource(classInternMap, name); // throws ClassSource_Exception
    }

    //

    @Override
    public ClassSourceImpl_Aggregate createAggregateClassSource(Util_InternMap internMap, String name) throws ClassSource_Exception {
        return new ClassSourceImpl_Aggregate(this, internMap, name);
    }

    @Override
    public ClassSourceImpl_MappedDirectory createDirectoryClassSource(Util_InternMap internMap, String name, String dirPath) throws ClassSource_Exception {
        return new ClassSourceImpl_MappedDirectory(this, internMap, name, dirPath); // throws ClassSource_Exception
    }

    @Override
    public ClassSourceImpl_MappedJar createJarClassSource(Util_InternMap internMap, String name, String jarPath) throws ClassSource_Exception {
        return new ClassSourceImpl_MappedJar(this, internMap, name, jarPath); // throws ClassSource_Exception
    }

    @Override
    public ClassSourceImpl_ClassLoader createClassLoaderClassSource(Util_InternMap internMap, String name, ClassLoader classLoader) throws ClassSource_Exception {
        return new ClassSourceImpl_ClassLoader(this, internMap, name, classLoader);
    }

    @Override
    public ClassSourceImpl_MappedContainer createContainerClassSource(Util_InternMap internMap, String name, Container container) throws ClassSource_Exception {
        return new ClassSourceImpl_MappedContainer(this, internMap, name, container);
    }

    @Override
    public ClassSourceImpl_MappedSimple createSimpleClassSource(Util_InternMap internMap, String name, ClassSource_MappedSimple.SimpleClassProvider provider)
                    throws ClassSource_Exception {
        return new ClassSourceImpl_MappedSimple(this, internMap, name, provider);
    }

    //

    @Override
    public ClassSourceImpl_Specification_Direct_EJB newEJBSpecification() {
        return new ClassSourceImpl_Specification_Direct_EJB(this);
    }

    @Override
    public ClassSourceImpl_Specification_Direct_Bundle newEBASpecification() {
        return new ClassSourceImpl_Specification_Direct_Bundle(this);
    }

    @Override
    public ClassSourceImpl_Specification_Direct_WAR newWARSpecification() {
        return new ClassSourceImpl_Specification_Direct_WAR(this);
    }

    @Override
    public ClassSourceImpl_Specification_Container_EJB newEJBContainerSpecification() {
        return new ClassSourceImpl_Specification_Container_EJB(this);
    }

    @Override
    public ClassSource_Specification_Container_WAR newWARContainerSpecification() {
        return new ClassSourceImpl_Specification_Container_WAR(this);
    }

    //

    //    // Factory helpers (part 1)
    //
    //    ClassSource_MappedContainer addContainerClassSource(String classSourceName, Container container, boolean isSeed)
    //                    throws ClassSource_Exception;
    //
    //    ClassSource_MappedDirectory addDirectoryClassSource(String classSourceName, String dirPath, boolean isSeed)
    //                    throws ClassSource_Exception;
    //
    //    ClassSource_MappedJar addJarClassSource(String classSourceName, String jarPath, boolean isSeed)
    //                    throws ClassSource_Exception;
    //
    //    ClassSource_ClassLoader addClassLoaderClassSource(String classSourceName, ClassLoader classLoader)
    //                    throws ClassSource_Exception;
    //
    //    // Factory helpers (part 2)
    //
    //    ClassSource_MappedContainer addContainerClassSource(String classSourceName, Container container, boolean isSeed, boolean isPartial)
    //                    throws ClassSource_Exception;
    //
    //    ClassSource_MappedDirectory addDirectoryClassSource(String classSourceName, String dirPath, boolean isSeed, boolean isPartial)
    //                    throws ClassSource_Exception;
    //
    //    ClassSource_MappedJar addJarClassSource(String classSourceName, String jarPath, boolean isSeed, boolean isPartial)
    //                    throws ClassSource_Exception;

    public ClassSourceImpl_MappedDirectory createDirectoryClassSource(ClassSource_Aggregate aggregate, String name, String dirPath)
                    throws ClassSource_Exception {

        return createDirectoryClassSource(aggregate.getInternMap(), name, dirPath);
        // 'createDirectoryClassSource' throws ClassSource_Exception
    }

    public ClassSourceImpl_MappedDirectory addDirectoryClassSource(ClassSource_Aggregate aggregate, String name, String dirPath, ScanPolicy scanPolicy)
                    throws ClassSource_Exception {

        ClassSourceImpl_MappedDirectory dirClassSource = createDirectoryClassSource(aggregate, name, dirPath);
        // throws ClassSource_Exception

        aggregate.addClassSource(dirClassSource, scanPolicy);

        return dirClassSource;
    }

    public ClassSourceImpl_MappedJar createJarClassSource(ClassSource_Aggregate aggregate, String name, String jarPath)
                    throws ClassSource_Exception {

        return createJarClassSource(aggregate.getInternMap(), name, jarPath);
        // 'createJarClassSource' throws ClassSource_Exception
    }

    public ClassSourceImpl_MappedJar addJarClassSource(ClassSource_Aggregate aggregate, String name, String jarPath, ScanPolicy scanPolicy)
                    throws ClassSource_Exception {

        ClassSourceImpl_MappedJar jarClassSource = createJarClassSource(aggregate, name, jarPath);
        // throws ClassSource_Exception

        aggregate.addClassSource(jarClassSource, scanPolicy);

        return jarClassSource;
    }

    public ClassSourceImpl_MappedSimple createSimpleClassSource(ClassSource_Aggregate aggregate, String name, ClassSource_MappedSimple.SimpleClassProvider provider)
                    throws ClassSource_Exception {

        return createSimpleClassSource(aggregate.getInternMap(), name, provider);
        // 'createSimpleClassSource' throws ClassSource_Exception
    }

    public ClassSourceImpl_MappedSimple addSimpleClassSource(ClassSource_Aggregate aggregate, String name, ClassSource_MappedSimple.SimpleClassProvider provider,
                                                             ScanPolicy scanPolicy)
                    throws ClassSource_Exception {

        ClassSourceImpl_MappedSimple simpleClassSource = createSimpleClassSource(aggregate, name, provider);
        // throws ClassSource_Exception

        aggregate.addClassSource(simpleClassSource, scanPolicy);

        return simpleClassSource;
    }

    //

    public ClassSourceImpl_ClassLoader createClassLoaderClassSource(ClassSource_Aggregate aggregate, String name, ClassLoader classLoader)
                    throws ClassSource_Exception {
        return createClassLoaderClassSource(aggregate.getInternMap(), name, classLoader);
        // 'createSimpleClassSource' throws ClassSource_Exception
    }

    public ClassSourceImpl_ClassLoader addClassLoaderClassSource(ClassSource_Aggregate aggregate, String name, ClassLoader classLoader)
                    throws ClassSource_Exception {

        ClassSourceImpl_ClassLoader classLoaderClassSource = createClassLoaderClassSource(aggregate, name, classLoader);
        // throws ClassSource_Exception

        // class loader class sources may only be added as external class sources
        // class loaders provide no iteration capability; an exhaustive scan is
        // not possible, even if it was desired
        aggregate.addClassSource(classLoaderClassSource, ScanPolicy.EXTERNAL);

        return classLoaderClassSource;
    }

    //

    public ClassSourceImpl_MappedContainer createContainerClassSource(ClassSource_Aggregate aggregate, String name, Container container)
                    throws ClassSource_Exception {
        return createContainerClassSource(aggregate.getInternMap(), name, container);
        // 'createContainerClassSource' throws ClassSource_Exception
    }

    public ClassSourceImpl_MappedContainer addContainerClassSource(ClassSource_Aggregate aggregate, String name, Container container, ScanPolicy scanPolicy)
                    throws ClassSource_Exception {

        ClassSourceImpl_MappedContainer containerClassSource = createContainerClassSource(aggregate, name, container);
        // throws ClassSource_Exception

        aggregate.addClassSource(containerClassSource, scanPolicy);

        return containerClassSource;
    }
}