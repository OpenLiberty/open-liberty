/*******************************************************************************
 * Copyright (c) 2021,2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.concurrent.internal.processor;

import java.lang.reflect.Member;
import java.security.AccessController;
import java.util.Collections;
import java.util.List;

import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.javaee.dd.common.ContextService;
import com.ibm.ws.javaee.dd.common.JNDIEnvironmentRef;
import com.ibm.ws.kernel.service.util.SecureAction;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionProcessor;
import com.ibm.wsspi.injectionengine.InjectionProcessorProvider;

import jakarta.enterprise.concurrent.ContextServiceDefinition;

/**
 * Registers a provider to process ContextServiceDefinition.
 */
@Component(service = InjectionProcessorProvider.class)
public class ContextServiceDefinitionProvider extends InjectionProcessorProvider<ContextServiceDefinition, ContextServiceDefinition.List> {

    static final SecureAction priv = AccessController.doPrivileged(SecureAction.get());

    private static final List<Class<? extends JNDIEnvironmentRef>> REF_CLASSES = //
                    Collections.<Class<? extends JNDIEnvironmentRef>> singletonList(ContextService.class);

    @Override
    @Trivial
    public Class<ContextServiceDefinition> getAnnotationClass() {
        return ContextServiceDefinition.class;
    }

    @Override
    @Trivial
    public Class<ContextServiceDefinition.List> getAnnotationsClass() {
        return ContextServiceDefinition.List.class;
    }

    @Override
    @Trivial
    public List<Class<? extends JNDIEnvironmentRef>> getJNDIEnvironmentRefClasses() {
        return REF_CLASSES;
    }

    @Override
    public InjectionProcessor<ContextServiceDefinition, ContextServiceDefinition.List> createInjectionProcessor() {
        return new Processor();
    }

    class Processor extends InjectionProcessor<ContextServiceDefinition, ContextServiceDefinition.List> {
        @Trivial
        public Processor() {
            super(ContextServiceDefinition.class, ContextServiceDefinition.List.class);
        }

        @Override
        public InjectionBinding<ContextServiceDefinition> createInjectionBinding(ContextServiceDefinition annotation,
                                                                                 Class<?> instanceClass, Member member,
                                                                                 String jndiName) throws InjectionException {
            InjectionBinding<ContextServiceDefinition> injectionBinding = //
                            new ContextServiceDefinitionBinding(jndiName, ivNameSpaceConfig);
            injectionBinding.merge(annotation, instanceClass, null);
            return injectionBinding;
        }

        @Override
        public ContextServiceDefinition[] getAnnotations(ContextServiceDefinition.List pluralAnnotation) {
            return pluralAnnotation.value();
        }

        @Override
        public String getJndiName(ContextServiceDefinition annotation) {
            return annotation.name();
        }

        @Override
        public void processXML() throws InjectionException {
            List<? extends ContextService> contextServiceDefinitions = //
                            ivNameSpaceConfig.getJNDIEnvironmentRefs(ContextService.class);

            if (contextServiceDefinitions != null)
                for (ContextService contextService : contextServiceDefinitions) {
                    String jndiName = contextService.getName();
                    InjectionBinding<ContextServiceDefinition> injectionBinding = ivAllAnnotationsCollection.get(jndiName);

                    ContextServiceDefinitionBinding binding;
                    if (injectionBinding == null) {
                        binding = new ContextServiceDefinitionBinding(jndiName, ivNameSpaceConfig);
                        addInjectionBinding(binding);
                    } else {
                        binding = (ContextServiceDefinitionBinding) injectionBinding;
                    }

                    binding.mergeXML(contextService);
                }
        }

        @Override
        public void resolve(InjectionBinding<ContextServiceDefinition> injectionBinding) throws InjectionException {
            ((ContextServiceDefinitionBinding) injectionBinding).resolve();
        }
    }
}