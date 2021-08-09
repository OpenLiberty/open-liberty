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

package com.ibm.ws.annocache.classsource.specification.internal;

import java.io.File;

import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_Factory;
import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_MappedDirectory;
import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_MappedJar;
import com.ibm.ws.annocache.classsource.specification.ClassSource_Specification_Direct_EJB;
import com.ibm.wsspi.annocache.classsource.ClassSource_Exception;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate.ScanPolicy;

public class ClassSourceImpl_Specification_Direct_EJB
    extends ClassSourceImpl_Specification_Direct
    implements ClassSource_Specification_Direct_EJB {

    @SuppressWarnings("hiding")
    public static final String CLASS_NAME = ClassSourceImpl_Specification_Direct_EJB.class.getSimpleName();

    //

    public ClassSourceImpl_Specification_Direct_EJB(
        ClassSourceImpl_Factory factory,
        String appName, String modName, String modCatName) {

        super(factory, appName, modName, modCatName);
    }

    public void addInternalClassSources(ClassSource_Aggregate rootClassSource)
        throws ClassSource_Exception {

        String useModulePath = getModulePath();
        File useModuleFile = new File(useModulePath);
        if ( !useModuleFile.exists() ) {
            throw new IllegalArgumentException("Module location [ " + useModuleFile.getAbsolutePath() + " ] does not exist");
        }

        if ( useModuleFile.isDirectory() ) {
            if ( useModulePath.charAt( useModulePath.length() - 1) != '/') {
                useModulePath += '/';
            }

            @SuppressWarnings("unused")
            ClassSourceImpl_MappedDirectory dirClassSource =
                addDirectoryClassSource(rootClassSource, modName, useModulePath, ScanPolicy.SEED);
                // throws ClassSource_Exception

        } else {
            @SuppressWarnings("unused")
            ClassSourceImpl_MappedJar jarClassSource =
                addJarClassSource(rootClassSource, modName, useModulePath, ScanPolicy.SEED);
                // throws ClassSource_Exception
        }
    }
}
