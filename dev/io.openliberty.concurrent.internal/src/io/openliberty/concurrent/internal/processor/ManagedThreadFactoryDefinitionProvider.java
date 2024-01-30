/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.concurrent.internal.processor;

import java.lang.reflect.Member;
import java.util.Collections;
import java.util.List;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.javaee.dd.common.JNDIEnvironmentRef;
import com.ibm.ws.javaee.dd.common.ManagedThreadFactory;
import com.ibm.ws.javaee.version.JavaEEVersion;
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

    private static final TraceComponent tc = Tr.register(ManagedThreadFactoryDefinitionProvider.class);

    private static final List<Class<? extends JNDIEnvironmentRef>> REF_CLASSES = //
                    Collections.<Class<? extends JNDIEnvironmentRef>> singletonList(ManagedThreadFactory.class);

    //FIXME - This is never used is it necessary?
    private final InjectionProcessor<ManagedThreadFactoryDefinition, ManagedThreadFactoryDefinition.List> processor = new Processor();

    /**
     * The Jakarta EE major version (ex. 10)
     */
    private int eeVersion;

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

    /**
     * The service ranking of JavaEEVersion ensures we get the highest
     * Jakarta EE version for the configured features.
     */
    @Reference(policyOption = ReferencePolicyOption.GREEDY)
    protected void setEEVersion(ServiceReference<JavaEEVersion> ref) {
        String version = (String) ref.getProperty("version");
        eeVersion = Integer.parseInt(version.substring(0, version.indexOf('.')));
    }

    class Processor extends InjectionProcessor<ManagedThreadFactoryDefinition, ManagedThreadFactoryDefinition.List> {
        @Trivial
        public Processor() {
            super(ManagedThreadFactoryDefinition.class, ManagedThreadFactoryDefinition.List.class);
        }

        @Override
        public InjectionBinding<ManagedThreadFactoryDefinition> createInjectionBinding(ManagedThreadFactoryDefinition annotation,
                                                                                       Class<?> instanceClass, Member member,
                                                                                       String jndiName) throws InjectionException {
            final boolean trace = TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled();
            if (trace)
                Tr.entry(this, tc, "createInjectionBinding", ManagedThreadFactoryDefinitionBinding.toString(annotation, eeVersion), instanceClass, member, jndiName);

            InjectionBinding<ManagedThreadFactoryDefinition> injectionBinding = //
                            new ManagedThreadFactoryDefinitionBinding(jndiName, ivNameSpaceConfig, eeVersion);
            injectionBinding.merge(annotation, instanceClass, null);

            if (trace)
                Tr.exit(this, tc, "createInjectionBinding", injectionBinding);
            return injectionBinding;
        }

        @Override
        public ManagedThreadFactoryDefinition[] getAnnotations(ManagedThreadFactoryDefinition.List pluralAnnotation) {
            ManagedThreadFactoryDefinition[] annos = pluralAnnotation.value();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Object[] a = new String[annos.length];
                for (int i = 0; i < annos.length; i++)
                    a[i] = new StringBuilder().append("ManagedThreadFactoryDefinition@").append(Integer.toHexString(annos[i].hashCode())) //
                                    .append(' ').append(annos[i].name()) //
                                    .toString();
                Tr.debug(this, tc, "getAnnotations", a);
            }
            return annos;
        }

        @Override
        @Trivial
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
                        binding = new ManagedThreadFactoryDefinitionBinding(jndiName, ivNameSpaceConfig, eeVersion);
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