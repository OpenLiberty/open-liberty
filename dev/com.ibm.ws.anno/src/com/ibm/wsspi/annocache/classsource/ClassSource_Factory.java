/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.annocache.classsource;

import com.ibm.ws.annocache.classsource.specification.ClassSource_Specification_Direct_Bundle;
import com.ibm.ws.annocache.classsource.specification.ClassSource_Specification_Direct_EJB;
import com.ibm.ws.annocache.classsource.specification.ClassSource_Specification_Direct_WAR;
import com.ibm.ws.annocache.classsource.specification.ClassSource_Specification_Element;
import com.ibm.ws.annocache.classsource.specification.ClassSource_Specification_Elements;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.annocache.util.Util_Factory;
import com.ibm.wsspi.annocache.util.Util_InternMap;
import com.ibm.wsspi.annocache.util.Util_RelativePath;

public interface ClassSource_Factory extends com.ibm.wsspi.anno.classsource.ClassSource_Factory {
    String getHashText();

    //

    Util_Factory getUtilFactory();

    //

    ClassSource_Exception newClassSourceException(String message);

    ClassSource_Exception wrapIntoClassSourceException(
        String callingClassName, String callingMethodName,
        String message,
        Throwable throwable);

    //

    /**
     * Create default class source options.
     *
     * @return New default class source options.
     */
    ClassSource_Options createOptions();

    /**
     * Create class source options with default values and
     * with jandex enablement set to a specified value.
     * 
     * @param useJandex The jandex enablement for the new options.
     *
     * @return New default class source options.
     */
    ClassSource_Options createOptions(boolean useJandex);

    //

    String getCanonicalName(String classSourceName);

    //

    /** Canonical name for unnamed class sources. */
    String UNNAMED_CLASS_SOURCE = "*** UNNAMED ***";

    /**
     * Parameter used for aggregate class sources which do not have an associated
     * application.  Module level results from a class source with an unnamed
     * application are not persisted.  (Container level results are persisted.)
     */
    String UNNAMED_APP = null;

    /**
     * Parameter used for aggregate class source which do not have an associated
     * module. 
     */
    boolean IS_UNNAMED_MOD = true;

    /**
     * Parameter used for aggregate class sources which do not have an associated
     * module.  Module level results from a class source with an unnamed
     * module are not persisted.  (Container level results are persisted.)
     */
    String UNNAMED_MOD = null;

    /** Predefined un-set module category name. */
    String UNSET_CATEGORY_NAME = null;

    /** Predefined unused entry prefix parameter value. */
    String UNUSED_ENTRY_PREFIX = null;

    /**
     * Create a new empty aggregate class source. Assign options to the new
     * class source.
     * 
     * @param appName The name of the application of the class source.
     * @param modName The name of the module of the class source.
     * @param modNameCategory A name used to enable multiple results for
     *     the same module name.
     * @param options Options for the new class source. 
     * 
     * @return The new class source.
     *
     * @throws ClassSource_Exception Thrown if there was a problem creating the class source.
     */
    ClassSource_Aggregate createAggregateClassSource(
        String appName, String modName, String modNameCategory,
        ClassSource_Options options) throws ClassSource_Exception;

    ClassSource_MappedSimple createSimpleClassSource(
        ClassSource_Aggregate aggregate,
        String name,
        ClassSource_MappedSimple.SimpleClassProvider provider) throws ClassSource_Exception;

    ClassSource_MappedContainer createContainerClassSource(
        ClassSource_Aggregate aggregate,
        String name,
        Container container) throws ClassSource_Exception;
    
    ClassSource_MappedContainer createContainerClassSource(
        ClassSource_Aggregate aggregate,
        String name,
        Container container, String entryPrefix) throws ClassSource_Exception;

    ClassSource_MappedDirectory createDirectoryClassSource(
        ClassSource_Aggregate aggregate,
        String name,
        String dirPath) throws ClassSource_Exception;

    ClassSource_MappedDirectory createDirectoryClassSource(
        ClassSource_Aggregate aggregate,
        String name,
        String dirPath, String entryPrefix) throws ClassSource_Exception;

    ClassSource_MappedJar createJarClassSource(
        ClassSource_Aggregate aggregate,
        String name,
        String jarPath) throws ClassSource_Exception;    

    ClassSource_MappedJar createJarClassSource(
        ClassSource_Aggregate aggregate,
        String name,
        String jarPath, String entryPrefix) throws ClassSource_Exception;

    ClassSource_ClassLoader createClassLoaderClassSource(
            ClassSource_Aggregate aggregate,
            String name,
            ClassLoader classLoader) throws ClassSource_Exception;

    ClassSource_ClassLoader createClassLoaderClassSource(
        ClassSource_Aggregate aggregate,
        ClassLoader classLoader) throws ClassSource_Exception;    

    //

    // These are mostly for internal use.  Most class sources are created
    // as children of an aggregate class source, and the root aggregate class
    // source is usually created with it's own intern map.

    ClassSource_Aggregate createAggregateClassSource(
        Util_InternMap internMap,
        String appName, String modName, String modNameCategory,
        ClassSource_Options options) throws ClassSource_Exception;

    //

    ClassSource_MappedSimple createSimpleClassSource(
        Util_InternMap internMap,
        String name,
        ClassSource_MappedSimple.SimpleClassProvider provider) throws ClassSource_Exception;

    ClassSource_MappedContainer createContainerClassSource(
        Util_InternMap internMap,
        String name,
        Container container) throws ClassSource_Exception;    

    ClassSource_MappedContainer createContainerClassSource(
        Util_InternMap internMap,
        String name,
        Container container, String entryPrefix) throws ClassSource_Exception;

    ClassSource_MappedDirectory createDirectoryClassSource(
        Util_InternMap internMap,
        String name,
        String dirPath) throws ClassSource_Exception;    
    
    ClassSource_MappedDirectory createDirectoryClassSource(
        Util_InternMap internMap,
        String name,
        String dirPath, String entryPrefix) throws ClassSource_Exception;        

    ClassSource_MappedJar createJarClassSource(
        Util_InternMap internMap,
        String name,
        String jarPath) throws ClassSource_Exception;    

    ClassSource_MappedJar createJarClassSource(
        Util_InternMap internMap,
        String name,
        String jarPath, String entryPrefix) throws ClassSource_Exception;

    ClassSource_ClassLoader createClassLoaderClassSource(
        Util_InternMap internMap,
        String name,
        ClassLoader classLoader) throws ClassSource_Exception;

    //

    ClassSource_Specification_Elements newElementsSpecification
        (String appName, String modName, String modCatName);
    ClassSource_Specification_Element newElementSpecification(
        String name, ClassSource_Aggregate.ScanPolicy policy, Util_RelativePath relativePath);

    ClassSource_Specification_Direct_Bundle newBundleDirectSpecification
        (String appName, String modName, String modCatName);
    ClassSource_Specification_Direct_EJB newEJBDirectSpecification
        (String appName, String modName, String modCatName);
    ClassSource_Specification_Direct_WAR newWARDirectSpecification
        (String appName, String modName, String modCatName);
}
