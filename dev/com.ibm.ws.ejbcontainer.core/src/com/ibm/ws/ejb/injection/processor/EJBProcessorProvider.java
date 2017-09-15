/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejb.injection.processor;

import java.util.Collections;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.EJBs;

import com.ibm.ws.javaee.dd.common.EJBRef;
import com.ibm.ws.javaee.dd.common.JNDIEnvironmentRef;
import com.ibm.wsspi.injectionengine.InjectionProcessor;
import com.ibm.wsspi.injectionengine.InjectionProcessorProvider;

/**
 * Provides the EJB injection processor to the injection engine.
 */
public final class EJBProcessorProvider extends InjectionProcessorProvider<EJB, EJBs>
{
    List<Class<? extends JNDIEnvironmentRef>> REF_CLASSES =
                    Collections.<Class<? extends JNDIEnvironmentRef>> singletonList(EJBRef.class);

    @Override
    public Class<EJB> getAnnotationClass()
    {
        return EJB.class;
    }

    @Override
    public Class<EJBs> getAnnotationsClass()
    {
        return EJBs.class;
    }

    public List<Class<? extends JNDIEnvironmentRef>> getJNDIEnvironmentRefClasses()
    {
        return REF_CLASSES;
    }

    @Override
    public InjectionProcessor<EJB, EJBs> createInjectionProcessor()
    {
        return new EJBProcessor();
    }
}
