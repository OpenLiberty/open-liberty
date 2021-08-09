/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine.processor;

import java.lang.reflect.Member;
import java.util.List;

import javax.annotation.sql.DataSourceDefinition;
import javax.annotation.sql.DataSourceDefinitions;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.javaee.dd.common.DataSource;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionConfigConstants;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionProcessor;

public class DataSourceDefinitionProcessor
                extends InjectionProcessor<DataSourceDefinition, DataSourceDefinitions>
{
    private static final TraceComponent tc = Tr.register(DataSourceDefinitionProcessor.class,
                                                         InjectionConfigConstants.traceString,
                                                         InjectionConfigConstants.messageFile);

    public DataSourceDefinitionProcessor()
    {
        super(DataSourceDefinition.class, DataSourceDefinitions.class);
    }

    @Override
    public void processXML()
                    throws InjectionException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "processXML : " + this);

        List<? extends DataSource> dsds = ivNameSpaceConfig.getDataSourceDefinitions();

        if (dsds != null)
        {
            for (DataSource dsd : dsds)
            {
                String jndiName = dsd.getName();
                InjectionBinding<DataSourceDefinition> injectionBinding = ivAllAnnotationsCollection.get(jndiName);
                DataSourceDefinitionInjectionBinding binding;

                if (injectionBinding != null)
                {
                    binding = (DataSourceDefinitionInjectionBinding) injectionBinding;
                }
                else
                {
                    binding = new DataSourceDefinitionInjectionBinding(jndiName, ivNameSpaceConfig);
                    addInjectionBinding(binding);
                }

                binding.mergeXML(dsd);
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "processXML : " + this);
    }

    @Override
    public void resolve(InjectionBinding<DataSourceDefinition> binding)
                    throws InjectionException
    {
        ((DataSourceDefinitionInjectionBinding) binding).resolve();
    }

    @Override
    public InjectionBinding<DataSourceDefinition> createInjectionBinding(DataSourceDefinition annotation,
                                                                         Class<?> instanceClass,
                                                                         Member member,
                                                                         String jndiName) // F743-33811
    throws InjectionException
    {
        InjectionBinding<DataSourceDefinition> injectionBinding =
                        new DataSourceDefinitionInjectionBinding(jndiName, ivNameSpaceConfig);
        injectionBinding.merge(annotation, instanceClass, null);
        return injectionBinding;
    }

    @Override
    public String getJndiName(DataSourceDefinition annotation)
    {
        return annotation.name();
    }

    @Override
    public DataSourceDefinition[] getAnnotations(DataSourceDefinitions annotation)
    {
        return annotation.value();
    }
}
