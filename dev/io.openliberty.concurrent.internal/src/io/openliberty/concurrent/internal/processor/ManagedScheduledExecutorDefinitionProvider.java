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
import com.ibm.ws.javaee.dd.common.ManagedScheduledExecutor;
import com.ibm.ws.javaee.version.JavaEEVersion;
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

    private static final TraceComponent tc = Tr.register(ManagedScheduledExecutorDefinitionProvider.class);

    private static final List<Class<? extends JNDIEnvironmentRef>> REF_CLASSES = //
                    Collections.<Class<? extends JNDIEnvironmentRef>> singletonList(ManagedScheduledExecutor.class);

    /**
     * The Jakarta EE major version (ex. 10)
     */
    private int eeVersion;

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

    /**
     * The service ranking of JavaEEVersion ensures we get the highest
     * Jakarta EE version for the configured features.
     */
    @Reference(policyOption = ReferencePolicyOption.GREEDY)
    protected void setEEVersion(ServiceReference<JavaEEVersion> ref) {
        String version = (String) ref.getProperty("version");
        eeVersion = Integer.parseInt(version.substring(0, version.indexOf('.')));
    }

    class Processor extends InjectionProcessor<ManagedScheduledExecutorDefinition, ManagedScheduledExecutorDefinition.List> {
        @Trivial
        public Processor() {
            super(ManagedScheduledExecutorDefinition.class, ManagedScheduledExecutorDefinition.List.class);
        }

        @Override
        public InjectionBinding<ManagedScheduledExecutorDefinition> createInjectionBinding(ManagedScheduledExecutorDefinition annotation,
                                                                                           Class<?> instanceClass, Member member,
                                                                                           String jndiName) throws InjectionException {
            final boolean trace = TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled();
            if (trace)
                Tr.entry(this, tc, "createInjectionBinding", ManagedScheduledExecutorDefinitionBinding.toString(annotation, eeVersion), instanceClass, member, jndiName);

            InjectionBinding<ManagedScheduledExecutorDefinition> injectionBinding = //
                            new ManagedScheduledExecutorDefinitionBinding(jndiName, ivNameSpaceConfig, eeVersion);
            injectionBinding.merge(annotation, instanceClass, null);

            if (trace)
                Tr.exit(this, tc, "createInjectionBinding", injectionBinding);
            return injectionBinding;
        }

        @Override
        @Trivial
        public ManagedScheduledExecutorDefinition[] getAnnotations(ManagedScheduledExecutorDefinition.List pluralAnnotation) {
            ManagedScheduledExecutorDefinition[] annos = pluralAnnotation.value();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Object[] a = new String[annos.length];
                for (int i = 0; i < annos.length; i++)
                    a[i] = new StringBuilder().append("ManagedScheduledExecutorDefinition@").append(Integer.toHexString(annos[i].hashCode())) //
                                    .append(' ').append(annos[i].name()) //
                                    .toString();
                Tr.debug(this, tc, "getAnnotations", a);
            }
            return annos;
        }

        @Override
        @Trivial
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
                        binding = new ManagedScheduledExecutorDefinitionBinding(jndiName, ivNameSpaceConfig, eeVersion);
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