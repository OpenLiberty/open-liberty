/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.processor.jms.connectionfactory;

import java.lang.reflect.Member;
import java.util.List;

import javax.jms.JMSConnectionFactoryDefinition;
import javax.jms.JMSConnectionFactoryDefinitions;

import com.ibm.ws.javaee.dd.common.JMSConnectionFactory;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionProcessor;

/**
 * This class provides processing to handle @JMSConnectionFactoryDefinition annotations defined in the target class
 */
public class JMSConnectionFactoryDefinitionProcessor extends InjectionProcessor<JMSConnectionFactoryDefinition, JMSConnectionFactoryDefinitions> {

    public JMSConnectionFactoryDefinitionProcessor() {
        super(JMSConnectionFactoryDefinition.class, JMSConnectionFactoryDefinitions.class);
    }

    /**
     * @param annotationClass
     * @param annotationsClass
     */
    public JMSConnectionFactoryDefinitionProcessor(Class<JMSConnectionFactoryDefinition> annotationClass, Class<JMSConnectionFactoryDefinitions> annotationsClass) {

        super(JMSConnectionFactoryDefinition.class, JMSConnectionFactoryDefinitions.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.injectionengine.InjectionProcessor#processXML()
     */
    @Override
    public void processXML() throws InjectionException {

        List<? extends JMSConnectionFactory> jmsConnectionFactoryDefinitions = ivNameSpaceConfig.getJNDIEnvironmentRefs(JMSConnectionFactory.class);

        if (jmsConnectionFactoryDefinitions != null) {
            for (JMSConnectionFactory jmsConnectionFactory : jmsConnectionFactoryDefinitions) {
                String jndiName = jmsConnectionFactory.getName();
                InjectionBinding<JMSConnectionFactoryDefinition> injectionBinding = ivAllAnnotationsCollection.get(jndiName);
                JMSConnectionFactoryDefinitionInjectionBinding binding;

                if (injectionBinding != null) {
                    binding = (JMSConnectionFactoryDefinitionInjectionBinding) injectionBinding;
                } else {
                    binding = new JMSConnectionFactoryDefinitionInjectionBinding(jndiName, ivNameSpaceConfig);
                    addInjectionBinding(binding);
                }

                binding.mergeXML(jmsConnectionFactory);
            }
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.injectionengine.InjectionProcessor#createInjectionBinding(java.lang.annotation.Annotation, java.lang.Class, java.lang.reflect.Member, java.lang.String)
     */
    @Override
    public InjectionBinding<JMSConnectionFactoryDefinition> createInjectionBinding(JMSConnectionFactoryDefinition annotation, Class<?> instanceClass, Member member, String jndiName) throws InjectionException {
        InjectionBinding<JMSConnectionFactoryDefinition> injectionBinding =
                        new JMSConnectionFactoryDefinitionInjectionBinding(jndiName, ivNameSpaceConfig);
        injectionBinding.merge(annotation, instanceClass, null);
        return injectionBinding;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.injectionengine.InjectionProcessor#resolve(com.ibm.wsspi.injectionengine.InjectionBinding)
     */
    @Override
    public void resolve(InjectionBinding<JMSConnectionFactoryDefinition> injectionBinding) throws InjectionException {

        ((JMSConnectionFactoryDefinitionInjectionBinding) injectionBinding).resolve();

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.injectionengine.InjectionProcessor#getJndiName(java.lang.annotation.Annotation)
     */
    @Override
    public String getJndiName(JMSConnectionFactoryDefinition annotation) {

        return annotation.name();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.injectionengine.InjectionProcessor#getAnnotations(java.lang.annotation.Annotation)
     */
    @Override
    public JMSConnectionFactoryDefinition[] getAnnotations(JMSConnectionFactoryDefinitions pluralAnnotation) {
        return pluralAnnotation.value();
    }

}
