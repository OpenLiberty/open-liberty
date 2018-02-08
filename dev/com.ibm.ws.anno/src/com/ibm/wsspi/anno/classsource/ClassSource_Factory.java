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

package com.ibm.wsspi.anno.classsource;

import com.ibm.ws.anno.classsource.internal.ClassSourceImpl_Aggregate;
import com.ibm.ws.anno.classsource.internal.ClassSourceImpl_ClassLoader;
import com.ibm.ws.anno.classsource.internal.ClassSourceImpl_MappedDirectory;
import com.ibm.ws.anno.classsource.internal.ClassSourceImpl_MappedJar;
import com.ibm.ws.anno.classsource.internal.ClassSourceImpl_MappedSimple;
import com.ibm.ws.anno.classsource.specification.ClassSource_Specification_Container_EJB;
import com.ibm.ws.anno.classsource.specification.ClassSource_Specification_Container_WAR;
import com.ibm.ws.anno.classsource.specification.ClassSource_Specification_Direct_Bundle;
import com.ibm.ws.anno.classsource.specification.ClassSource_Specification_Direct_EJB;
import com.ibm.ws.anno.classsource.specification.ClassSource_Specification_Direct_WAR;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.anno.classsource.ClassSource_MappedSimple.SimpleClassProvider;
import com.ibm.wsspi.anno.util.Util_Factory;
import com.ibm.wsspi.anno.util.Util_InternMap;

public interface ClassSource_Factory {
    String getHashText();

    //

    Util_Factory getUtilFactory();

    //

    ClassSource_Exception newClassSourceException(String message);

    ClassSource_Exception wrapIntoClassSourceException(String callingClassName,
                                                       String callingMethodName,
                                                       String message, Throwable th);

    //
    
    /**
     * Create default class source options.
     *
     * @return New default class source options.
     */
	ClassSource_Options createOptions();

    //

    String getCanonicalName(String classSourceName);

    //

    /**
     * Create a new empty aggregate class source.  Give the class source
     * default options.
     *  
     * @param name The name of the class source.
     * 
     * @return THe new class source.
     * 
     * @throws ClassSource_Exception Thrown if there was a problem creating the class source.
     */
    ClassSource_Aggregate createAggregateClassSource(String name)
    	throws ClassSource_Exception;

    /**
     * Create a new empty aggregate class source.  Assign options to the new
     * class source.
     *  
     * @param name The name of the class source.
     * @param options Options for the new class source.
     *
     * @return The new class source.
     *
     * @throws ClassSource_Exception Thrown if there was a problem creating the class source.
     */    
    ClassSource_Aggregate createAggregateClassSource(String name, ClassSource_Options options) 
    	throws ClassSource_Exception;

    //

    // These are mostly for internal use.  Most class sources are created
    // as children of an aggregate class source, and the root aggregate class
    // source is usually created with it's own intern map.

    ClassSource_Aggregate createAggregateClassSource(
    	Util_InternMap internMap, String name)
    	throws ClassSource_Exception;

	ClassSourceImpl_Aggregate createAggregateClassSource(
		Util_InternMap internMap, String name, ClassSource_Options options)
		throws ClassSource_Exception;

	//

    
    ClassSource_MappedContainer createContainerClassSource(
    	Util_InternMap internMap, String name, Container container)
    	throws ClassSource_Exception;

    ClassSource_MappedContainer createContainerClassSource(
    	Util_InternMap internMap, String name, ClassSource_Options options, Container container)
    	throws ClassSource_Exception;

    ClassSource_MappedSimple createSimpleClassSource(
    		Util_InternMap internMap, String name, SimpleClassProvider provider)
    		throws ClassSource_Exception;
	
	ClassSourceImpl_MappedSimple createSimpleClassSource(
		Util_InternMap internMap, String name, ClassSource_Options options, SimpleClassProvider provider)
		throws ClassSource_Exception;

    ClassSource_MappedDirectory createDirectoryClassSource(
    	Util_InternMap internMap, String name, String dirPath)
    	throws ClassSource_Exception;
	
	ClassSourceImpl_MappedDirectory createDirectoryClassSource(
		Util_InternMap internMap, String name, ClassSource_Options options, String dirPath)
		throws ClassSource_Exception;

    ClassSource_MappedJar createJarClassSource(
    	Util_InternMap internMap, String name, String jarPath)
    	throws ClassSource_Exception;

	ClassSourceImpl_MappedJar createJarClassSource(
		Util_InternMap internMap, String name, ClassSource_Options options, String jarPath)
		throws ClassSource_Exception;    

    ClassSource_ClassLoader createClassLoaderClassSource(
    	Util_InternMap internMap, String name, ClassLoader classLoader)
    	throws ClassSource_Exception;
    
	ClassSourceImpl_ClassLoader createClassLoaderClassSource(
		Util_InternMap internMap, String name, ClassSource_Options options, ClassLoader classLoader)
		throws ClassSource_Exception;
    
    //

    ClassSource_Specification_Direct_EJB newEJBSpecification();

    ClassSource_Specification_Direct_Bundle newEBASpecification();

    ClassSource_Specification_Direct_WAR newWARSpecification();

    ClassSource_Specification_Container_EJB newEJBContainerSpecification();

    ClassSource_Specification_Container_WAR newWARContainerSpecification();
}
