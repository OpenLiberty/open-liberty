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
package io.openliberty.concurrent.processor;

import java.lang.reflect.Member;
import java.security.AccessController;
import java.util.Collections;
import java.util.List;

import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.javaee.dd.common.JNDIEnvironmentRef;
import com.ibm.ws.javaee.dd.common.ManagedExecutor;
import com.ibm.ws.kernel.service.util.SecureAction;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionProcessor;
import com.ibm.wsspi.injectionengine.InjectionProcessorProvider;

import jakarta.enterprise.concurrent.ManagedExecutorDefinition;

/**
 * Registers a provider to process ManagedExecutorDefinition.
 */
@Component(service = InjectionProcessorProvider.class)
public class ManagedExecutorDefinitionProvider extends InjectionProcessorProvider<ManagedExecutorDefinition, ManagedExecutorDefinition.List> {

    static final SecureAction priv = AccessController.doPrivileged(SecureAction.get());

    private static final List<Class<? extends JNDIEnvironmentRef>> REF_CLASSES = //
                    Collections.<Class<? extends JNDIEnvironmentRef>> singletonList(ManagedExecutor.class);

    @Override
    @Trivial
    public Class<ManagedExecutorDefinition> getAnnotationClass() {
        return ManagedExecutorDefinition.class;
    }

    @Override
    @Trivial
    public Class<ManagedExecutorDefinition.List> getAnnotationsClass() {
        return ManagedExecutorDefinition.List.class;
    }

    @Override
    @Trivial
    public List<Class<? extends JNDIEnvironmentRef>> getJNDIEnvironmentRefClasses() {
        return REF_CLASSES;
    }

    @Override
    public InjectionProcessor<ManagedExecutorDefinition, ManagedExecutorDefinition.List> createInjectionProcessor() {
        return new Processor();
    }

    class Processor extends InjectionProcessor<ManagedExecutorDefinition, ManagedExecutorDefinition.List> {
        public Processor() {
            super(ManagedExecutorDefinition.class, ManagedExecutorDefinition.List.class);
        }

        @Override
        public InjectionBinding<ManagedExecutorDefinition> createInjectionBinding(ManagedExecutorDefinition annotation,
                                                                                  Class<?> instanceClass, Member member,
                                                                                  String jndiName) throws InjectionException {
            InjectionBinding<ManagedExecutorDefinition> injectionBinding = //
                            new ManagedExecutorDefinitionBinding(jndiName, ivNameSpaceConfig);
            injectionBinding.merge(annotation, instanceClass, null);
            return injectionBinding;
        }

        @Override
        public ManagedExecutorDefinition[] getAnnotations(ManagedExecutorDefinition.List pluralAnnotation) {
            return pluralAnnotation.value();
        }

        @Override
        public String getJndiName(ManagedExecutorDefinition annotation) {
            return annotation.name();
        }

        @Override
        public void processXML() throws InjectionException {
            List<? extends ManagedExecutor> managedExecutorDefinitions = //
                            ivNameSpaceConfig.getJNDIEnvironmentRefs(ManagedExecutor.class);

            if (managedExecutorDefinitions != null)
                for (ManagedExecutor managedExecutor : managedExecutorDefinitions) {
                    String jndiName = managedExecutor.getName();
                    InjectionBinding<ManagedExecutorDefinition> injectionBinding = ivAllAnnotationsCollection.get(jndiName);

                    ManagedExecutorDefinitionBinding binding;
                    if (injectionBinding == null) {
                        binding = new ManagedExecutorDefinitionBinding(jndiName, ivNameSpaceConfig);
                        addInjectionBinding(binding);
                    } else {
                        binding = (ManagedExecutorDefinitionBinding) injectionBinding;
                    }

                    binding.mergeXML(managedExecutor);
                }
        }

        @Override
        public void resolve(InjectionBinding<ManagedExecutorDefinition> injectionBinding) throws InjectionException {
            ((ManagedExecutorDefinitionBinding) injectionBinding).resolve();
        }
    }
}