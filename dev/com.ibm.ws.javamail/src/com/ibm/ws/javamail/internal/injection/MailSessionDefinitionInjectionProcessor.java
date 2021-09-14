/*******************************************************************************
 * Copyright (c) 2015,2021
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javamail.internal.injection;

import java.lang.reflect.Member;
import java.util.List;

import javax.mail.MailSessionDefinition;
import javax.mail.MailSessionDefinitions;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.javaee.dd.common.MailSession;
import com.ibm.ws.javamail.internal.TraceConstants;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionProcessor;

public class MailSessionDefinitionInjectionProcessor extends InjectionProcessor<MailSessionDefinition, MailSessionDefinitions> {
    static final TraceComponent tc = Tr.register(MailSessionDefinitionInjectionProcessor.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    public MailSessionDefinitionInjectionProcessor() {
        super(MailSessionDefinition.class, MailSessionDefinitions.class);
    }

    /**
     * @param annotationClass
     * @param annotationsClass
     */
    public MailSessionDefinitionInjectionProcessor(Class<MailSessionDefinition> annotationClass, Class<MailSessionDefinitions> annotationsClass) {

        super(MailSessionDefinition.class, MailSessionDefinitions.class);
    }

    /**
     * Processes {@link ComponentNameSpaceConfiguration#getJNDIEnvironmnetRefs} for MailSessions.
     *
     * </ul>
     *
     * @throws InjectionException if an error is found processing the XML.
     **/
    @Override
    public void processXML() throws InjectionException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "processXML : " + this);

        List<? extends MailSession> mailSessionDefinitions = ivNameSpaceConfig.getJNDIEnvironmentRefs(MailSession.class);

        if (mailSessionDefinitions != null) {
            for (MailSession mailSession : mailSessionDefinitions) {
                String jndiName = mailSession.getName();
                InjectionBinding<MailSessionDefinition> injectionBinding = ivAllAnnotationsCollection.get(jndiName);
                MailSessionDefinitionInjectionBinding binding;

                if (injectionBinding != null) {
                    binding = (MailSessionDefinitionInjectionBinding) injectionBinding;
                } else {
                    binding = new MailSessionDefinitionInjectionBinding(jndiName, ivNameSpaceConfig);
                    addInjectionBinding(binding);
                }

                binding.mergeXML(mailSession);
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
    @Sensitive
    @Override
    public InjectionBinding<MailSessionDefinition> createInjectionBinding(@Sensitive MailSessionDefinition annotation, Class<?> instanceClass, Member member,
                                                                          String jndiName) throws InjectionException {
        InjectionBinding<MailSessionDefinition> injectionBinding = new MailSessionDefinitionInjectionBinding(jndiName, ivNameSpaceConfig);
        injectionBinding.merge(annotation, instanceClass, null);
        return injectionBinding;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.injectionengine.InjectionProcessor#resolve(com.ibm.wsspi.injectionengine.InjectionBinding)
     */
    @Override
    public void resolve(InjectionBinding<MailSessionDefinition> injectionBinding) throws InjectionException {

        ((MailSessionDefinitionInjectionBinding) injectionBinding).resolve();

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.injectionengine.InjectionProcessor#getJndiName(java.lang.annotation.Annotation)
     */
    @Override
    public String getJndiName(@Sensitive MailSessionDefinition annotation) {

        return annotation.name();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.injectionengine.InjectionProcessor#getAnnotations(java.lang.annotation.Annotation)
     */
    @Sensitive
    @Override
    public MailSessionDefinition[] getAnnotations(@Sensitive MailSessionDefinitions pluralAnnotation) {
        return pluralAnnotation.value();
    }
}
