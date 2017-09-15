/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca17.processor;

import java.lang.reflect.Member;
import java.util.List;

import javax.resource.AdministeredObjectDefinition;
import javax.resource.AdministeredObjectDefinitions;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.javaee.dd.common.AdministeredObject;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionConfigConstants;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionProcessor;

/**
 *
 */
public class AdministeredObjectDefinitionProcessor extends InjectionProcessor<AdministeredObjectDefinition, AdministeredObjectDefinitions>
{

    private final static TraceComponent tc = Tr.register(AdministeredObjectDefinitionProcessor.class,
                                                         InjectionConfigConstants.traceString,
                                                         InjectionConfigConstants.messageFile);

    public AdministeredObjectDefinitionProcessor()
    {
        super(AdministeredObjectDefinition.class, AdministeredObjectDefinitions.class);
    }

    /**
     * @param annotationClass
     * @param annotationsClass
     */
    public AdministeredObjectDefinitionProcessor(Class<AdministeredObjectDefinition> annotationClass, Class<AdministeredObjectDefinitions> annotationsClass) {

        super(AdministeredObjectDefinition.class, AdministeredObjectDefinitions.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.injectionengine.InjectionProcessor#processXML()
     */
    @Override
    public void processXML() throws InjectionException {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "processXML : " + this);

        List<? extends AdministeredObject> administeredObjectDefinitions = ivNameSpaceConfig.getJNDIEnvironmentRefs(AdministeredObject.class);

        if (administeredObjectDefinitions != null)
        {
            for (AdministeredObject administeredObject : administeredObjectDefinitions)
            {
                String jndiName = administeredObject.getName();
                InjectionBinding<AdministeredObjectDefinition> injectionBinding = ivAllAnnotationsCollection.get(jndiName);
                AdministeredObjectDefinitionInjectionBinding binding;

                if (injectionBinding != null)
                {
                    binding = (AdministeredObjectDefinitionInjectionBinding) injectionBinding;
                }
                else
                {
                    binding = new AdministeredObjectDefinitionInjectionBinding(jndiName, ivNameSpaceConfig);
                    addInjectionBinding(binding);
                }

                binding.mergeXML(administeredObject);
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "processXML : " + this);

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.injectionengine.InjectionProcessor#createInjectionBinding(java.lang.annotation.Annotation, java.lang.Class, java.lang.reflect.Member, java.lang.String)
     */
    @Override
    public InjectionBinding<AdministeredObjectDefinition> createInjectionBinding(AdministeredObjectDefinition annotation, Class<?> instanceClass, Member member, String jndiName) throws InjectionException {
        InjectionBinding<AdministeredObjectDefinition> injectionBinding =
                        new AdministeredObjectDefinitionInjectionBinding(jndiName, ivNameSpaceConfig);
        injectionBinding.merge(annotation, instanceClass, null);
        return injectionBinding;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.injectionengine.InjectionProcessor#resolve(com.ibm.wsspi.injectionengine.InjectionBinding)
     */
    @Override
    public void resolve(InjectionBinding<AdministeredObjectDefinition> injectionBinding) throws InjectionException {

        ((AdministeredObjectDefinitionInjectionBinding) injectionBinding).resolve();

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.injectionengine.InjectionProcessor#getJndiName(java.lang.annotation.Annotation)
     */
    @Override
    public String getJndiName(AdministeredObjectDefinition annotation) {

        return annotation.name();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.injectionengine.InjectionProcessor#getAnnotations(java.lang.annotation.Annotation)
     */
    @Override
    public AdministeredObjectDefinition[] getAnnotations(AdministeredObjectDefinitions pluralAnnotation) {
        return pluralAnnotation.value();
    }

}
