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
import com.ibm.ws.javaee.dd.common.JNDIEnvironmentRef;
import com.ibm.ws.javaee.dd.common.ManagedScheduledExecutor;
import com.ibm.ws.kernel.service.util.SecureAction;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionProcessor;
import com.ibm.wsspi.injectionengine.InjectionProcessorProvider;

import jakarta.enterprise.concurrent.ManagedScheduledExecutorDefinition;

/**
 * Registers a provider to process ManagedScheduledExecutorDefinition.
 */
@Component(service = InjectionProcessorProvider.class)
public class ManagedScheduledExecutorDefinitionProvider extends InjectionProcessorProvider<ManagedScheduledExecutorDefinition, ManagedScheduledExecutorDefinition.List> {

    static final SecureAction priv = AccessController.doPrivileged(SecureAction.get());

    private static final List<Class<? extends JNDIEnvironmentRef>> REF_CLASSES = //
                    Collections.<Class<? extends JNDIEnvironmentRef>> singletonList(ManagedScheduledExecutor.class);

    @Override
    @Trivial
    public Class<ManagedScheduledExecutorDefinition> getAnnotationClass() {
        return ManagedScheduledExecutorDefinition.class;
    }

    @Override
    @Trivial
    public Class<ManagedScheduledExecutorDefinition.List> getAnnotationsClass() {
        return ManagedScheduledExecutorDefinition.List.class;
    }

    @Override
    @Trivial
    public List<Class<? extends JNDIEnvironmentRef>> getJNDIEnvironmentRefClasses() {
        return REF_CLASSES;
    }

    @Override
    public InjectionProcessor<ManagedScheduledExecutorDefinition, ManagedScheduledExecutorDefinition.List> createInjectionProcessor() {
        return new Processor();
    }

    class Processor extends InjectionProcessor<ManagedScheduledExecutorDefinition, ManagedScheduledExecutorDefinition.List> {
        public Processor() {
            super(ManagedScheduledExecutorDefinition.class, ManagedScheduledExecutorDefinition.List.class);
        }

        @Override
        public InjectionBinding<ManagedScheduledExecutorDefinition> createInjectionBinding(ManagedScheduledExecutorDefinition annotation,
                                                                                           Class<?> instanceClass, Member member,
                                                                                           String jndiName) throws InjectionException {
            InjectionBinding<ManagedScheduledExecutorDefinition> injectionBinding = //
                            new ManagedScheduledExecutorDefinitionBinding(jndiName, ivNameSpaceConfig);
            injectionBinding.merge(annotation, instanceClass, null);
            return injectionBinding;
        }

        @Override
        public ManagedScheduledExecutorDefinition[] getAnnotations(ManagedScheduledExecutorDefinition.List pluralAnnotation) {
            return pluralAnnotation.value();
        }

        @Override
        public String getJndiName(ManagedScheduledExecutorDefinition annotation) {
            return annotation.name();
        }

        @Override
        public void processXML() throws InjectionException {
            List<? extends ManagedScheduledExecutor> managedScheduledExecutorDefinitions = //
                            ivNameSpaceConfig.getJNDIEnvironmentRefs(ManagedScheduledExecutor.class);

            if (managedScheduledExecutorDefinitions != null)
                for (ManagedScheduledExecutor managedScheduledExecutor : managedScheduledExecutorDefinitions) {
                    String jndiName = managedScheduledExecutor.getName();
                    InjectionBinding<ManagedScheduledExecutorDefinition> injectionBinding = ivAllAnnotationsCollection.get(jndiName);

                    ManagedScheduledExecutorDefinitionBinding binding;
                    if (injectionBinding == null) {
                        binding = new ManagedScheduledExecutorDefinitionBinding(jndiName, ivNameSpaceConfig);
                        addInjectionBinding(binding);
                    } else {
                        binding = (ManagedScheduledExecutorDefinitionBinding) injectionBinding;
                    }

                    binding.mergeXML(managedScheduledExecutor);
                }
        }

        @Override
        public void resolve(InjectionBinding<ManagedScheduledExecutorDefinition> injectionBinding) throws InjectionException {
            ((ManagedScheduledExecutorDefinitionBinding) injectionBinding).resolve();
        }
    }
}