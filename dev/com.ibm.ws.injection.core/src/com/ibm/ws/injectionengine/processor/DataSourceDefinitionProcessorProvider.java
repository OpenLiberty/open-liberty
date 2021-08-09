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

import java.util.Collections;
import java.util.List;

import javax.annotation.sql.DataSourceDefinition;
import javax.annotation.sql.DataSourceDefinitions;

import com.ibm.ws.javaee.dd.common.DataSource;
import com.ibm.ws.javaee.dd.common.JNDIEnvironmentRef;
import com.ibm.wsspi.injectionengine.InjectionProcessor;
import com.ibm.wsspi.injectionengine.InjectionProcessorProvider;

public class DataSourceDefinitionProcessorProvider
                extends InjectionProcessorProvider<DataSourceDefinition, DataSourceDefinitions>
{
    List<Class<? extends JNDIEnvironmentRef>> REF_CLASSES =
                    Collections.<Class<? extends JNDIEnvironmentRef>> singletonList(DataSource.class);

    @Override
    public Class<DataSourceDefinition> getAnnotationClass()
    {
        return DataSourceDefinition.class;
    }

    @Override
    public Class<DataSourceDefinitions> getAnnotationsClass()
    {
        return DataSourceDefinitions.class;
    }

    public List<Class<? extends JNDIEnvironmentRef>> getJNDIEnvironmentRefClasses()
    {
        return REF_CLASSES;
    }

    @Override
    public InjectionProcessor<DataSourceDefinition, DataSourceDefinitions> createInjectionProcessor()
    {
        return new DataSourceDefinitionProcessor();
    }
}
