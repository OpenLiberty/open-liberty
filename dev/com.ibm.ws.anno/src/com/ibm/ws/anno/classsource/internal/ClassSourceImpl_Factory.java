/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corporation 2011, 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */

package com.ibm.ws.anno.classsource.internal;

import com.ibm.ws.anno.classsource.specification.internal.ClassSourceImpl_Specification_Element;
import com.ibm.ws.anno.classsource.specification.internal.ClassSourceImpl_Specification_Elements;
import com.ibm.ws.anno.service.internal.AnnotationServiceImpl_Logging;
import com.ibm.ws.anno.service.internal.AnnotationServiceImpl_Service;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.annotation.Trivial;

import com.ibm.ws.anno.classsource.specification.internal.ClassSourceImpl_Specification_Direct_Bundle;
import com.ibm.ws.anno.classsource.specification.internal.ClassSourceImpl_Specification_Direct_EJB;
import com.ibm.ws.anno.classsource.specification.internal.ClassSourceImpl_Specification_Direct_WAR;

import com.ibm.ws.anno.util.internal.UtilImpl_Factory;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.classsource.ClassSource_ClassLoader;
import com.ibm.wsspi.anno.classsource.ClassSource_Exception;
import com.ibm.wsspi.anno.classsource.ClassSource_Factory;
import com.ibm.wsspi.anno.classsource.ClassSource_MappedSimple;
import com.ibm.wsspi.anno.classsource.ClassSource_Options;

import com.ibm.wsspi.anno.util.Util_InternMap;
import com.ibm.wsspi.anno.util.Util_RelativePath;

public class ClassSourceImpl_Factory implements ClassSource_Factory {
    private static final Logger logger = AnnotationServiceImpl_Logging.ANNO_LOGGER;

    public static final String CLASS_NAME = ClassSourceImpl_Factory.class.getSimpleName();

    //

    protected final String hashText;

    @Override
    @Trivial
    public String getHashText() {
        return hashText;
    }

    //

    public ClassSourceImpl_Factory(
        AnnotationServiceImpl_Service annoService,
        UtilImpl_Factory utilFactory) {
    	
        super();

        String methodName = "<init>";

        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());

        this.annoService = annoService;
        this.utilFactory = utilFactory;

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] Created", this.hashText);
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] Util Factory [ {1} ]",
                new Object[] { this.hashText, this.utilFactory.getHashText() });
        }
    }

    //

    protected final AnnotationServiceImpl_Service annoService;

    public AnnotationServiceImpl_Service getAnnotationService() {
        return annoService;
    }

    //

    protected final UtilImpl_Factory utilFactory;

    @Override
    @Trivial
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
    @Trivial
    public ClassSource_Exception wrapIntoClassSourceException(
        String callingClassName,
        String callingMethodName,
        String message, Throwable th) {

        String methodName = "wrapIntoClassSourceException";

        ClassSource_Exception wrappedException = new ClassSource_Exception(message, th);

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] [ {1} ] Wrap [ {2} ] as [ {3} ]",
                new Object[] { callingClassName, callingMethodName,
                               th.getClass().getName(),
                               wrappedException.getClass().getName() });
        }

        return wrappedException;
    }

    //

    @Override
    public ClassSourceImpl_Options createOptions() {
        return new ClassSourceImpl_Options();
    }

    @Override
    public ClassSourceImpl_Options createOptions(boolean useJandex) {
        ClassSourceImpl_Options useOptions = new ClassSourceImpl_Options();
        useOptions.setUseJandex(useJandex);
        return useOptions;
    }

    //

    @Override
    @Trivial
    public String getCanonicalName(String classSourceName) {
        return classSourceName.replace('\\', '/');
    }

    //

    @Override
    @Trivial
    public ClassSourceImpl_Aggregate createAggregateClassSource(
        String appName, String modName, String modCategoryName,
        ClassSource_Options options) throws ClassSource_Exception {

        Util_InternMap classInternMap =
            getUtilFactory().createInternMap(Util_InternMap.ValueType.VT_CLASS_NAME, "classes and packages");

        return createAggregateClassSource(classInternMap, appName, modName, modCategoryName, options);
        // throws ClassSource_Exception
    }

    @Override
    @Trivial
    public ClassSourceImpl_MappedSimple createSimpleClassSource(
        ClassSource_Aggregate aggregate,
        String name,
        ClassSource_MappedSimple.SimpleClassProvider provider) throws ClassSource_Exception {

        return createSimpleClassSource(aggregate.getInternMap(), name, provider);
        // 'createSimpleClassSource' throws ClassSource_Exception
    }

    @Override
    @Trivial
    public ClassSourceImpl_MappedContainer createContainerClassSource(
        ClassSource_Aggregate aggregate,
        String name,
        Container container) throws ClassSource_Exception {

        return createContainerClassSource(aggregate.getInternMap(), name, container);
        // 'createContainerClassSource' throws ClassSource_Exception
    }

    @Override
    @Trivial
    public ClassSourceImpl_MappedContainer createContainerClassSource(
        ClassSource_Aggregate aggregate,
        String name,
        Container container, String entryPrefix) throws ClassSource_Exception {

        return createContainerClassSource(aggregate.getInternMap(), name, container, entryPrefix);
        // 'createContainerClassSource' throws ClassSource_Exception
    }

    @Override
    @Trivial
    public ClassSourceImpl_MappedDirectory createDirectoryClassSource(
        ClassSource_Aggregate aggregate,
        String name,
        String dirPath) throws ClassSource_Exception {

        return createDirectoryClassSource(aggregate.getInternMap(), name, dirPath);
        // 'createDirectoryClassSource' throws ClassSource_Exception
    }

    @Override
    @Trivial
    public ClassSourceImpl_MappedDirectory createDirectoryClassSource(
        ClassSource_Aggregate aggregate,
        String name,
        String dirPath, String entryPrefix) throws ClassSource_Exception {

        return createDirectoryClassSource(aggregate.getInternMap(), name, dirPath, entryPrefix);
        // 'createDirectoryClassSource' throws ClassSource_Exception
    }

    @Override
    @Trivial
    public ClassSourceImpl_MappedJar createJarClassSource(
        ClassSource_Aggregate aggregate,
        String name,
        String jarPath) throws ClassSource_Exception {
        
        return createJarClassSource(aggregate.getInternMap(), name, jarPath);
        // 'createJarClassSource' throws ClassSource_Exception
    }

    @Override
    @Trivial
    public ClassSourceImpl_MappedJar createJarClassSource(
        ClassSource_Aggregate aggregate,
        String name,
        String jarPath,
        String entryNamePrefix) throws ClassSource_Exception {

        return createJarClassSource(aggregate.getInternMap(), name, jarPath, entryNamePrefix);
        // 'createJarClassSource' throws ClassSource_Exception
    }

    @Override
    @Trivial
    public ClassSourceImpl_ClassLoader createClassLoaderClassSource(
        ClassSource_Aggregate aggregate,
        ClassLoader classLoader) throws ClassSource_Exception {

        return createClassLoaderClassSource(
            aggregate,
            ClassSource_ClassLoader.CLASSLOADER_CLASSSOURCE_NAME,
            classLoader);
        // 'createSimpleClassSource' throws ClassSource_Exception
    }

    @Override
    @Trivial
    public ClassSourceImpl_ClassLoader createClassLoaderClassSource(
        ClassSource_Aggregate aggregate,
        String name,
        ClassLoader classLoader) throws ClassSource_Exception {

        return createClassLoaderClassSource(aggregate.getInternMap(), name, classLoader);
        // 'createSimpleClassSource' throws ClassSource_Exception
    }

    //

    @Override
    public ClassSourceImpl_Aggregate createAggregateClassSource(
        Util_InternMap internMap,
        String appName, String modName, String modCategoryName,
        ClassSource_Options options) throws ClassSource_Exception {

        return new ClassSourceImpl_Aggregate(this, internMap, appName, modName, modCategoryName, options);
    }

    @Override
    public ClassSourceImpl_MappedSimple createSimpleClassSource(
        Util_InternMap internMap,
        String name,
        ClassSource_MappedSimple.SimpleClassProvider provider) throws ClassSource_Exception {

        return new ClassSourceImpl_MappedSimple(this, internMap, name, provider);
        // throws ClassSource_Exception
    }

    @Override
    public ClassSourceImpl_MappedContainer createContainerClassSource(
        Util_InternMap internMap,
        String name,
        Container container)
        throws ClassSource_Exception {

        return new ClassSourceImpl_MappedContainer(this, internMap, name, container);
        // throws ClassSource_Exception
    }

    @Override
    public ClassSourceImpl_MappedContainer createContainerClassSource(
        Util_InternMap internMap,
        String name,
        Container container, String entryPrefix) throws ClassSource_Exception {

        return new ClassSourceImpl_MappedContainer(this, internMap, name, container, entryPrefix);
        // throws ClassSource_Exception
    }

    
    @Override
    public ClassSourceImpl_MappedDirectory createDirectoryClassSource(
        Util_InternMap internMap,
        String name,
        String dirPath) throws ClassSource_Exception {

        return new ClassSourceImpl_MappedDirectory(this, internMap, name, dirPath);
        // throws ClassSource_Exception
    }

    @Override
    public ClassSourceImpl_MappedDirectory createDirectoryClassSource(
        Util_InternMap internMap,
        String name,
        String dirPath, String entryPrefix) throws ClassSource_Exception {

        return new ClassSourceImpl_MappedDirectory(this, internMap, name, dirPath, entryPrefix);
        // throws ClassSource_Exception
    }

    @Override
    public ClassSourceImpl_MappedJar createJarClassSource(
        Util_InternMap internMap,
        String name,
        String jarPath) throws ClassSource_Exception {

        return new ClassSourceImpl_MappedJar(this, internMap, name, jarPath);
        // throws ClassSource_Exception
    }

    @Override
    public ClassSourceImpl_MappedJar createJarClassSource(
        Util_InternMap internMap,
        String name,
        String jarPath, String entryNamePrefix)

        throws ClassSource_Exception {

        return new ClassSourceImpl_MappedJar(this, internMap, name, jarPath, entryNamePrefix);
        // throws ClassSource_Exception
    }

    @Override
    public ClassSourceImpl_ClassLoader createClassLoaderClassSource(
        Util_InternMap internMap,
        String name,
        ClassLoader classLoader) throws ClassSource_Exception {

        return new ClassSourceImpl_ClassLoader(this, internMap, name, classLoader);
        // throws ClassSource_Exception
    }

    //

    @Override
    public ClassSourceImpl_Specification_Elements newElementsSpecification(String appName, String modName, String modCatName) {
        return new ClassSourceImpl_Specification_Elements(this, appName, modName, modCatName);
    }

    @Override
    public ClassSourceImpl_Specification_Element newElementSpecification(
        String name, ScanPolicy policy, Util_RelativePath relativePath) {
        return new ClassSourceImpl_Specification_Element(name, policy, relativePath);
    }

    @Override
    public ClassSourceImpl_Specification_Direct_Bundle newBundleDirectSpecification(String appName, String modName, String modCatName) {
        return new ClassSourceImpl_Specification_Direct_Bundle(this, appName, modName, modCatName);
    }

    @Override
    public ClassSourceImpl_Specification_Direct_EJB newEJBDirectSpecification(String appName, String modName, String modCatName) {
        return new ClassSourceImpl_Specification_Direct_EJB(this, appName, modName, modCatName);
    }

    @Override
    public ClassSourceImpl_Specification_Direct_WAR newWARDirectSpecification(String appName, String modName, String modCatName) {
        return new ClassSourceImpl_Specification_Direct_WAR(this, appName, modName, modCatName);
    }
}
