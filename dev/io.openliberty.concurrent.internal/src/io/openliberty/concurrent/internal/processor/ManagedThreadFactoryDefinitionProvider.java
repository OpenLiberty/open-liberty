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
import com.ibm.ws.javaee.dd.common.ManagedThreadFactory;
import com.ibm.ws.kernel.service.util.SecureAction;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionProcessor;
import com.ibm.wsspi.injectionengine.InjectionProcessorProvider;

import jakarta.enterprise.concurrent.ManagedThreadFactoryDefinition;

/**
 * Registers a provider to process ManagedThreadFactoryDefinition.
 */
@Component(service = InjectionProcessorProvider.class)
public class ManagedThreadFactoryDefinitionProvider extends InjectionProcessorProvider<ManagedThreadFactoryDefinition, ManagedThreadFactoryDefinition.List> {

    static final SecureAction priv = AccessController.doPrivileged(SecureAction.get());

    private static final List<Class<? extends JNDIEnvironmentRef>> REF_CLASSES = //
                    Collections.<Class<? extends JNDIEnvironmentRef>> singletonList(ManagedThreadFactory.class);

    private final InjectionProcessor<ManagedThreadFactoryDefinition, ManagedThreadFactoryDefinition.List> processor = new Processor();

    @Override
    @Trivial
    public Class<ManagedThreadFactoryDefinition> getAnnotationClass() {
        return ManagedThreadFactoryDefinition.class;
    }

    @Override
    @Trivial
    public Class<ManagedThreadFactoryDefinition.List> getAnnotationsClass() {
        return ManagedThreadFactoryDefinition.List.class;
    }

    @Override
    @Trivial
    public List<Class<? extends JNDIEnvironmentRef>> getJNDIEnvironmentRefClasses() {
        return REF_CLASSES;
    }

    @Override
    public InjectionProcessor<ManagedThreadFactoryDefinition, ManagedThreadFactoryDefinition.List> createInjectionProcessor() {
        return new Processor();
    }

    class Processor extends InjectionProcessor<ManagedThreadFactoryDefinition, ManagedThreadFactoryDefinition.List> {
        public Processor() {
            super(ManagedThreadFactoryDefinition.class, ManagedThreadFactoryDefinition.List.class);
        }

        @Override
        public InjectionBinding<ManagedThreadFactoryDefinition> createInjectionBinding(ManagedThreadFactoryDefinition annotation,
                                                                                       Class<?> instanceClass, Member member,
                                                                                       String jndiName) throws InjectionException {
            InjectionBinding<ManagedThreadFactoryDefinition> injectionBinding = //
                            new ManagedThreadFactoryDefinitionBinding(jndiName, ivNameSpaceConfig);
            injectionBinding.merge(annotation, instanceClass, null);
            return injectionBinding;
        }

        @Override
        public ManagedThreadFactoryDefinition[] getAnnotations(ManagedThreadFactoryDefinition.List pluralAnnotation) {
            return pluralAnnotation.value();
        }

        @Override
        public String getJndiName(ManagedThreadFactoryDefinition annotation) {
            return annotation.name();
        }

        @Override
        public void processXML() throws InjectionException {
            List<? extends ManagedThreadFactory> managedThreadFactoryDefinitions = //
                            ivNameSpaceConfig.getJNDIEnvironmentRefs(ManagedThreadFactory.class);

            if (managedThreadFactoryDefinitions != null)
                for (ManagedThreadFactory managedThreadFactory : managedThreadFactoryDefinitions) {
                    String jndiName = managedThreadFactory.getName();
                    InjectionBinding<ManagedThreadFactoryDefinition> injectionBinding = ivAllAnnotationsCollection.get(jndiName);

                    ManagedThreadFactoryDefinitionBinding binding;
                    if (injectionBinding == null) {
                        binding = new ManagedThreadFactoryDefinitionBinding(jndiName, ivNameSpaceConfig);
                        addInjectionBinding(binding);
                    } else {
                        binding = (ManagedThreadFactoryDefinitionBinding) injectionBinding;
                    }

                    binding.mergeXML(managedThreadFactory);
                }
        }

        @Override
        public void resolve(InjectionBinding<ManagedThreadFactoryDefinition> injectionBinding) throws InjectionException {
            ((ManagedThreadFactoryDefinitionBinding) injectionBinding).resolve();
        }
    }
}