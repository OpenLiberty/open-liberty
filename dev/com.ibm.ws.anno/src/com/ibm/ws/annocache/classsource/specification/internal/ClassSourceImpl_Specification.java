/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.classsource.specification.internal;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.annotation.Trivial;

import com.ibm.wsspi.annocache.classsource.ClassSource_Exception;
import com.ibm.wsspi.annocache.classsource.ClassSource_Options;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate.ScanPolicy;

import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_Aggregate;
import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_ClassLoader;
import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_Factory;
import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_MappedDirectory;
import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_MappedJar;
import com.ibm.ws.annocache.classsource.specification.ClassSource_Specification;
import com.ibm.ws.annocache.service.internal.AnnotationCacheServiceImpl_Logging;


public abstract class ClassSourceImpl_Specification implements ClassSource_Specification {
    protected static final Logger logger = AnnotationCacheServiceImpl_Logging.ANNO_LOGGER;

    public static final String CLASS_NAME = ClassSourceImpl_Specification.class.getSimpleName();

    //

    protected String hashText;

    @Override
    public String getHashText() {
        return hashText;
    }

    //

    public ClassSourceImpl_Specification(
        ClassSourceImpl_Factory factory,
        String appName, String modName, String modCatName) {

        super();

        String methodName = "<init>";

        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());

        this.factory = factory;

        this.appName = appName;
        this.modName = modName;
        this.modCatName = modCatName;

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, this.hashText);
        }
    }

    //

    protected ClassSourceImpl_Factory factory;

    @Override
    public ClassSourceImpl_Factory getFactory() {
        return factory;
    }

    //

    protected final String appName;
    protected final String modName;
    protected final String modCatName;

    @Override
    public String getAppName() {
        return appName;
    }

    @Override
    public String getModName() {
        return modName;
    }
    
    @Override
    public String getModCategoryName() {
        return modCatName;
    }    

    //

    private ClassLoader rootClassLoader;

    @Override
    public ClassLoader getRootClassLoader() {
        return rootClassLoader;
    }

    @Override
    public void setRootClassLoader(ClassLoader rootClassLoader) {
        this.rootClassLoader = rootClassLoader;
    }

    //

    @Override
    public ClassSourceImpl_Aggregate createRootClassSource(ClassSource_Options classSourceOptions)
        throws ClassSource_Exception {

        ClassSourceImpl_Aggregate rootClassSource = createEmptyRootClassSource(classSourceOptions);

        addInternalClassSources(rootClassSource);
        addExternalClassSources(rootClassSource);
        addClassLoaderClassSource(rootClassSource);

        return rootClassSource;
    }

    //

    @Override
    public ClassSourceImpl_Aggregate createEmptyRootClassSource(ClassSource_Options classSourceOptions)
        throws ClassSource_Exception {

        return getFactory().createAggregateClassSource(
        	getAppName(), getModName(), getModCategoryName(),
        	classSourceOptions );
    }

    //


    @Override
    public abstract void addInternalClassSources(ClassSource_Aggregate rootClassSource)
        throws ClassSource_Exception;

    @Override
    public abstract void addExternalClassSources(ClassSource_Aggregate rootClassSource)
        throws ClassSource_Exception;

    @Override
    public void addClassLoaderClassSource(ClassSource_Aggregate rootClassSource)
        throws ClassSource_Exception {

        ClassLoader useRootClassLoader = getRootClassLoader();

        if ( useRootClassLoader != null ) {
            @SuppressWarnings("unused")
            ClassSourceImpl_ClassLoader classLoaderClassSource =
                addClassLoaderClassSource(rootClassSource, "classloader", useRootClassLoader);
        }
    }

    //

    @Override
    public ClassSourceImpl_MappedJar addJarClassSource(
        ClassSource_Aggregate rootClassSource,
        String name,
        String jarPath, String entryPrefix,
        ScanPolicy scanPolicy) throws ClassSource_Exception {
    
        ClassSourceImpl_MappedJar jarClassSource =
            getFactory().createJarClassSource(rootClassSource, name, jarPath, entryPrefix);

        rootClassSource.addClassSource(jarClassSource, scanPolicy); // throws ClassSource_Exception

        return jarClassSource;
    }

    @Override
    public ClassSourceImpl_MappedJar addJarClassSource(
        ClassSource_Aggregate rootClassSource,
        String name,
        String jarPath,
        ScanPolicy scanPolicy) throws ClassSource_Exception {
    
        ClassSourceImpl_MappedJar jarClassSource =
            getFactory().createJarClassSource(rootClassSource, name, jarPath);

        rootClassSource.addClassSource(jarClassSource, scanPolicy); // throws ClassSource_Exception

        return jarClassSource;
    }

    @Override
    public ClassSourceImpl_MappedDirectory addDirectoryClassSource(
        ClassSource_Aggregate rootClassSource,
        String name,
        String dirPath, String entryPrefix,
        ScanPolicy scanPolicy) throws ClassSource_Exception {
    
        ClassSourceImpl_MappedDirectory dirClassSource =
            getFactory().createDirectoryClassSource(rootClassSource, name, dirPath, entryPrefix);

        rootClassSource.addClassSource(dirClassSource, scanPolicy); // throws ClassSource_Exception

        return dirClassSource;
    }

    @Override
    public ClassSourceImpl_MappedDirectory addDirectoryClassSource(
        ClassSource_Aggregate rootClassSource,
        String name,
        String dirPath,
        ScanPolicy scanPolicy) throws ClassSource_Exception {
    
        ClassSourceImpl_MappedDirectory dirClassSource =
            getFactory().createDirectoryClassSource(rootClassSource, name, dirPath);

        rootClassSource.addClassSource(dirClassSource, scanPolicy); // throws ClassSource_Exception

        return dirClassSource;
    }

    @Override
    public ClassSourceImpl_ClassLoader addClassLoaderClassSource(
        ClassSource_Aggregate rootClassSource,
        String name,
        ClassLoader classLoader) throws ClassSource_Exception {

        ClassSourceImpl_ClassLoader classLoaderClassSource =
            getFactory().createClassLoaderClassSource(rootClassSource, name, classLoader);
        // 'createClassLaoderClassSource' throws ClassSource_Exception

        rootClassSource.addClassSource(classLoaderClassSource, ScanPolicy.EXTERNAL);

        return classLoaderClassSource;
    }

    //

    @Override
    public void log(Logger useLogger) {
        logHeader(useLogger);
        logInternal(useLogger);
        logExternal(useLogger);
        logClassLoader(useLogger);
        logTrailer(useLogger);
    }

    @Override
    public void logHeader(Logger useLogger) {
        String methodName = "logHeader";

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Specification [ " + getClass().getName() + " ]");
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  Application Name [ " + getAppName() + " ]");
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  Module Name [ " + getModName() + " ]");
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  Module Category Name [ " + getModCategoryName() + " ]");
    }

    @Override
    public void logTrailer(Logger useLogger) {
        // Default to nothing
    }

    @Override
    public abstract void logInternal(Logger useLogger);

    @Override
    public abstract void logExternal(Logger useLogger);

    @Override
    @Trivial
    public void logClassLoader(Logger useLogger) {
        String methodName = "logClassLoader";

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  ClassLoader [ " + getRootClassLoader() + " ]");
    }
}
