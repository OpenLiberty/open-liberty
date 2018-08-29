/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2011, 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.anno.classsource.specification;

import java.util.logging.Logger;

import com.ibm.ws.anno.classsource.internal.ClassSourceImpl_Aggregate;
import com.ibm.ws.anno.classsource.internal.ClassSourceImpl_ClassLoader;
import com.ibm.ws.anno.classsource.internal.ClassSourceImpl_MappedDirectory;
import com.ibm.ws.anno.classsource.internal.ClassSourceImpl_MappedJar;
import com.ibm.ws.anno.classsource.internal.ClassSourceImpl_Options;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.classsource.ClassSource_Exception;
import com.ibm.wsspi.anno.classsource.ClassSource_Factory;

public interface ClassSource_Specification {
    String getHashText();

    void log(Logger useLogger);
    
    void logHeader(Logger useLogger);
    void logTrailer(Logger useLogger);

    void logInternal(Logger useLogger);
    void logExternal(Logger useLogger);
    void logClassLoader(Logger useLogger);

    //

    ClassSource_Factory getFactory();

    //

    String getAppName();
    String getModName();
	String getModCategoryName();

    //
    
    ClassLoader getRootClassLoader();
    void setRootClassLoader(ClassLoader rootClassLoader);

    //

    ClassSourceImpl_Aggregate createRootClassSource(ClassSourceImpl_Options classSourceOptions)
        throws ClassSource_Exception;

    void addInternalClassSources(ClassSourceImpl_Aggregate rootClassSource)
        throws ClassSource_Exception;
    void addExternalClassSources(ClassSourceImpl_Aggregate rootClassSource)
        throws ClassSource_Exception;
    void addClassLoaderClassSource(ClassSourceImpl_Aggregate rootClassSource)
        throws ClassSource_Exception;

    //

    ClassSourceImpl_Aggregate createEmptyRootClassSource(ClassSourceImpl_Options classSourceOptions)
        throws ClassSource_Exception;

    ClassSourceImpl_MappedJar addJarClassSource(
        ClassSourceImpl_Aggregate aggregateClassSource,
        String name,
        String jarPath, String entryPrefix,
        ScanPolicy scanPolicy) throws ClassSource_Exception;
    
    ClassSourceImpl_MappedJar addJarClassSource(
        ClassSourceImpl_Aggregate aggregateClassSource,
        String name,
        String jarPath,
        ScanPolicy scanPolicy) throws ClassSource_Exception;    

    ClassSourceImpl_MappedDirectory addDirectoryClassSource(
        ClassSourceImpl_Aggregate aggregateClassSource,
        String name,
        String dirPath, String entryPrefix,
        ScanPolicy scanPolicy) throws ClassSource_Exception;
    
    ClassSourceImpl_MappedDirectory addDirectoryClassSource(
        ClassSourceImpl_Aggregate aggregateClassSource,
        String name,
        String dirPath,
        ScanPolicy scanPolicy) throws ClassSource_Exception;    

    ClassSourceImpl_ClassLoader addClassLoaderClassSource(
        ClassSourceImpl_Aggregate aggregateClassSource,
        String name,
        ClassLoader classLoader) throws ClassSource_Exception;

    boolean IS_PARTIAL = true;
    boolean IS_NOT_PARTIAL = false;

    boolean IS_EXCLUDED = true;
    boolean IS_NOT_EXCLUDED = false;
}
