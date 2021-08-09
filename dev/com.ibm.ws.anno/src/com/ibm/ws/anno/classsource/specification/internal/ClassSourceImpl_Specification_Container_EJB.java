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

package com.ibm.ws.anno.classsource.specification.internal;

import java.text.MessageFormat;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.anno.classsource.internal.ClassSourceImpl_Aggregate;
import com.ibm.ws.anno.classsource.internal.ClassSourceImpl_Factory;
import com.ibm.ws.anno.classsource.specification.ClassSource_Specification_Container_EJB;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.classsource.ClassSource_Exception;

public class ClassSourceImpl_Specification_Container_EJB
                extends ClassSourceImpl_Specification_Container
                implements ClassSource_Specification_Container_EJB {

    public ClassSourceImpl_Specification_Container_EJB(ClassSourceImpl_Factory factory) {
        super(factory);
    }

    //

    @Override
    public ClassSource_Aggregate createClassSource(String targetName, ClassLoader rootClassLoader)
                    throws ClassSource_Exception {

        ClassSourceImpl_Aggregate classSource = createAggregateClassSource(targetName);

        getFactory().addContainerClassSource(classSource,
                                             targetName,
                                             getImmediateContainer(),
                                             ScanPolicy.SEED); // throws ClassSource_Exception

        addStandardClassSources(targetName, rootClassLoader, classSource);

        return classSource;
    }

    //

    @Override
    public void log(TraceComponent logger) {

        if (logger.isDebugEnabled()) {
            Tr.debug(logger, MessageFormat.format("Class source specification [ {0} ]", getHashText()));
        }

        logCommon(logger);
    }
}
