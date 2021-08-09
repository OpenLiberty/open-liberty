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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.anno.classsource.internal.ClassSourceImpl_Aggregate;
import com.ibm.ws.anno.classsource.internal.ClassSourceImpl_Factory;
import com.ibm.ws.anno.classsource.specification.ClassSource_Specification;
import com.ibm.ws.anno.service.internal.AnnotationServiceImpl_Logging;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.classsource.ClassSource_Exception;

public abstract class ClassSourceImpl_Specification implements ClassSource_Specification {
    public static final String CLASS_NAME = ClassSourceImpl_Specification.class.getName();
    private static final TraceComponent tc = Tr.register(ClassSourceImpl_Specification.class);

    //

    protected String hashText;

    @Override
    public String getHashText() {
        return hashText;
    }

    //

    protected ClassSourceImpl_Specification(ClassSourceImpl_Factory factory) {
        super();

        this.factory = factory;

        this.hashText = AnnotationServiceImpl_Logging.getBaseHash(this);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, this.hashText);
        }
    }

    //

    protected ClassSourceImpl_Factory factory;

    @Override
    public ClassSourceImpl_Factory getFactory() {
        return factory;
    }

    //

    @Override
    public abstract ClassSource_Aggregate createClassSource(String targetName, ClassLoader rootClassLoader) throws ClassSource_Exception;

    //

    public ClassSourceImpl_Aggregate createAggregateClassSource(String name) throws ClassSource_Exception {
        return getFactory().createAggregateClassSource(name);
    }

    //

    @Override
    public void logState() {
        TraceComponent stateLogger = AnnotationServiceImpl_Logging.stateLogger;

        if (stateLogger.isDebugEnabled()) {
            log(stateLogger);
        }
    }

    @Override
    public abstract void log(TraceComponent logger);
}
