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
package com.ibm.ws.annocache.classsource.specification;

import java.util.logging.Logger;

import com.ibm.wsspi.annocache.classsource.ClassSource_Exception;
import com.ibm.wsspi.annocache.classsource.ClassSource_Factory;
import com.ibm.wsspi.annocache.classsource.ClassSource_MappedDirectory;
import com.ibm.wsspi.annocache.classsource.ClassSource_MappedJar;
import com.ibm.wsspi.annocache.classsource.ClassSource_Options;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.annocache.classsource.ClassSource_ClassLoader;

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

    ClassSource_Aggregate createRootClassSource(ClassSource_Options classSourceOptions)
        throws ClassSource_Exception;

    void addInternalClassSources(ClassSource_Aggregate rootClassSource)
        throws ClassSource_Exception;
    void addExternalClassSources(ClassSource_Aggregate rootClassSource)
        throws ClassSource_Exception;
    void addClassLoaderClassSource(ClassSource_Aggregate rootClassSource)
        throws ClassSource_Exception;

    //

    ClassSource_Aggregate createEmptyRootClassSource(ClassSource_Options classSourceOptions)
        throws ClassSource_Exception;

    ClassSource_MappedJar addJarClassSource(
        ClassSource_Aggregate aggregateClassSource,
        String name,
        String jarPath, String entryPrefix,
        ScanPolicy scanPolicy) throws ClassSource_Exception;
    
    ClassSource_MappedJar addJarClassSource(
        ClassSource_Aggregate aggregateClassSource,
        String name,
        String jarPath,
        ScanPolicy scanPolicy) throws ClassSource_Exception;    

    ClassSource_MappedDirectory addDirectoryClassSource(
        ClassSource_Aggregate aggregateClassSource,
        String name,
        String dirPath, String entryPrefix,
        ScanPolicy scanPolicy) throws ClassSource_Exception;
    
    ClassSource_MappedDirectory addDirectoryClassSource(
        ClassSource_Aggregate aggregateClassSource,
        String name,
        String dirPath,
        ScanPolicy scanPolicy) throws ClassSource_Exception;    

    ClassSource_ClassLoader addClassLoaderClassSource(
        ClassSource_Aggregate aggregateClassSource,
        String name,
        ClassLoader classLoader) throws ClassSource_Exception;

    boolean IS_PARTIAL = true;
    boolean IS_NOT_PARTIAL = false;

    boolean IS_EXCLUDED = true;
    boolean IS_NOT_EXCLUDED = false;
}
