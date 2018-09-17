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
package com.ibm.ws.anno.classsource.specification.internal;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.anno.classsource.internal.ClassSourceImpl_Aggregate;
import com.ibm.ws.anno.classsource.internal.ClassSourceImpl_ClassLoader;
import com.ibm.ws.anno.classsource.internal.ClassSourceImpl_Factory;
import com.ibm.ws.anno.classsource.internal.ClassSourceImpl_MappedDirectory;
import com.ibm.ws.anno.classsource.internal.ClassSourceImpl_MappedJar;
import com.ibm.ws.anno.classsource.internal.ClassSourceImpl_Options;
import com.ibm.ws.anno.classsource.specification.ClassSource_Specification;
import com.ibm.ws.anno.service.internal.AnnotationServiceImpl_Logging;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.classsource.ClassSource_Exception;

public abstract class ClassSourceImpl_Specification implements ClassSource_Specification {
    protected static final Logger logger = AnnotationServiceImpl_Logging.ANNO_LOGGER;

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
    public ClassSourceImpl_Aggregate createRootClassSource(ClassSourceImpl_Options classSourceOptions)
        throws ClassSource_Exception {

        ClassSourceImpl_Aggregate rootClassSource = createEmptyRootClassSource(classSourceOptions);

        addInternalClassSources(rootClassSource);
        addExternalClassSources(rootClassSource);
        addClassLoaderClassSource(rootClassSource);

        return rootClassSource;
    }

    //

    @Override
    public ClassSourceImpl_Aggregate createEmptyRootClassSource(ClassSourceImpl_Options classSourceOptions)
        throws ClassSource_Exception {

        return getFactory().createAggregateClassSource(
        	getAppName(), getModName(), getModCategoryName(),
        	classSourceOptions );
    }

    //


    @Override
    public abstract void addInternalClassSources(ClassSourceImpl_Aggregate rootClassSource)
        throws ClassSource_Exception;

    @Override
    public abstract void addExternalClassSources(ClassSourceImpl_Aggregate rootClassSource)
        throws ClassSource_Exception;

    @Override
    public void addClassLoaderClassSource(ClassSourceImpl_Aggregate rootClassSource)
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
        ClassSourceImpl_Aggregate rootClassSource,
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
        ClassSourceImpl_Aggregate rootClassSource,
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
        ClassSourceImpl_Aggregate rootClassSource,
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
        ClassSourceImpl_Aggregate rootClassSource,
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
        ClassSourceImpl_Aggregate rootClassSource,
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
