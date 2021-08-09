/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine.processor;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Resource;
import javax.annotation.Resources;

import com.ibm.ws.javaee.dd.common.EnvEntry;
import com.ibm.ws.javaee.dd.common.JNDIEnvironmentRef;
import com.ibm.ws.javaee.dd.common.MessageDestinationRef;
import com.ibm.ws.javaee.dd.common.ResourceEnvRef;
import com.ibm.ws.javaee.dd.common.ResourceRef;
import com.ibm.wsspi.injectionengine.InjectionProcessor;
import com.ibm.wsspi.injectionengine.InjectionProcessorProvider;

public class ResourceProcessorProvider
                extends InjectionProcessorProvider<Resource, Resources>
{
    @SuppressWarnings("unchecked")
    List<Class<? extends JNDIEnvironmentRef>> REF_CLASSES = Arrays.<Class<? extends JNDIEnvironmentRef>> asList
                    (EnvEntry.class,
                     ResourceRef.class,
                     ResourceEnvRef.class,
                     MessageDestinationRef.class);

    @Override
    public Class<Resource> getAnnotationClass()
    {
        return Resource.class;
    }

    @Override
    public Class<Resources> getAnnotationsClass()
    {
        return Resources.class;
    }

    public List<Class<? extends JNDIEnvironmentRef>> getJNDIEnvironmentRefClasses()
    {
        return REF_CLASSES;
    }

    @Override
    public InjectionProcessor<Resource, Resources> createInjectionProcessor()
    {
        return new ResourceProcessor();
    }
}
