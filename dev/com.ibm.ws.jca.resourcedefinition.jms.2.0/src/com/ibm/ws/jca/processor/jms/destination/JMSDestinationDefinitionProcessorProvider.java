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
package com.ibm.ws.jca.processor.jms.destination;

import java.util.Collections;
import java.util.List;

import javax.jms.JMSDestinationDefinition;
import javax.jms.JMSDestinationDefinitions;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.javaee.dd.common.JMSDestination;
import com.ibm.ws.javaee.dd.common.JNDIEnvironmentRef;
import com.ibm.wsspi.injectionengine.InjectionProcessor;
import com.ibm.wsspi.injectionengine.InjectionProcessorProvider;

/**
 *
 */
@Component(service = { InjectionProcessorProvider.class })
public class JMSDestinationDefinitionProcessorProvider extends InjectionProcessorProvider<JMSDestinationDefinition, JMSDestinationDefinitions> {

    List<Class<? extends JNDIEnvironmentRef>> REF_CLASSES = Collections.<Class<? extends JNDIEnvironmentRef>> singletonList(JMSDestination.class);

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.injectionengine.InjectionProcessorProvider#getAnnotationClass()
     */
    @Override
    public Class<JMSDestinationDefinition> getAnnotationClass() {

        return JMSDestinationDefinition.class;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.injectionengine.InjectionProcessorProvider#getAnnotationsClass()
     */
    @Override
    public Class<JMSDestinationDefinitions> getAnnotationsClass() {

        return JMSDestinationDefinitions.class;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.injectionengine.InjectionProcessorProvider#getJNDIEnvironmentRefClasses()
     */
    @Override
    public List<Class<? extends JNDIEnvironmentRef>> getJNDIEnvironmentRefClasses() {

        return REF_CLASSES;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.injectionengine.InjectionProcessorProvider#createInjectionProcessor()
     */
    @Override
    public InjectionProcessor<JMSDestinationDefinition, JMSDestinationDefinitions> createInjectionProcessor() {

        return new JMSDestinationDefinitionProcessor();
    }

}
