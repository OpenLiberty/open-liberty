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

package com.ibm.ws.anno.classsource.specification.internal;

import com.ibm.ws.anno.classsource.internal.ClassSourceImpl_Aggregate;
import com.ibm.ws.anno.classsource.internal.ClassSourceImpl_Factory;
import com.ibm.ws.anno.classsource.internal.ClassSourceImpl_MappedJar;
import com.ibm.ws.anno.classsource.specification.ClassSource_Specification_Direct_Bundle;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.classsource.ClassSource_Exception;

public class ClassSourceImpl_Specification_Direct_Bundle
    extends ClassSourceImpl_Specification_Direct
    implements ClassSource_Specification_Direct_Bundle {

    public static final String CLASS_NAME = ClassSourceImpl_Specification_Direct_Bundle.class.getSimpleName();
    
    public ClassSourceImpl_Specification_Direct_Bundle(
        ClassSourceImpl_Factory factory,
        String appName, String modName, String modCatName) {

        super(factory, appName, modName, modCatName);
    }

    public void addInternalClassSources(ClassSourceImpl_Aggregate rootClassSource)
        throws ClassSource_Exception {
    
        @SuppressWarnings("unused")
        ClassSourceImpl_MappedJar jarClassSource =
            addJarClassSource(rootClassSource, modName, getModulePath(), ScanPolicy.SEED);
        // throws ClassSource_Exception
    }
}
