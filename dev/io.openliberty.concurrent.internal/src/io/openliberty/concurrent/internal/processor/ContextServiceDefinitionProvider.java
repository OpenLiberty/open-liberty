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
import java.security.AccessController;
import java.util.Collections;
import java.util.List;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.javaee.dd.common.ContextService;
import com.ibm.ws.javaee.dd.common.JNDIEnvironmentRef;
import com.ibm.ws.javaee.version.JavaEEVersion;
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

    private static final TraceComponent tc = Tr.register(ContextServiceDefinitionProvider.class);

    private static final List<Class<? extends JNDIEnvironmentRef>> REF_CLASSES = //
                    Collections.<Class<? extends JNDIEnvironmentRef>> singletonList(ContextService.class);

    /**
     * The Jakarta EE major version (ex. 10)
     */
    private int eeVersion;

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

    /**
     * The service ranking of JavaEEVersion ensures we get the highest
     * Jakarta EE version for the configured features.
     */
    @Reference(policyOption = ReferencePolicyOption.GREEDY)
    protected void setEEVersion(ServiceReference<JavaEEVersion> ref) {
        String version = (String) ref.getProperty("version");
        eeVersion = Integer.parseInt(version.substring(0, version.indexOf('.')));
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
            final boolean trace = TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled();
            if (trace)
                Tr.entry(this, tc, "createInjectionBinding", ContextServiceDefinitionBinding.toString(annotation, eeVersion), instanceClass, member, jndiName);

            InjectionBinding<ContextServiceDefinition> injectionBinding = //
                            new ContextServiceDefinitionBinding(jndiName, ivNameSpaceConfig, eeVersion);
            injectionBinding.merge(annotation, instanceClass, null);

            if (trace)
                Tr.exit(this, tc, "createInjectionBinding", injectionBinding);
            return injectionBinding;
        }

        @Override
        @Trivial
        public ContextServiceDefinition[] getAnnotations(ContextServiceDefinition.List pluralAnnotation) {
            ContextServiceDefinition[] annos = pluralAnnotation.value();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Object[] a = new String[annos.length];
                for (int i = 0; i < annos.length; i++)
                    a[i] = new StringBuilder().append("ContextServiceDefinition@").append(Integer.toHexString(annos[i].hashCode())) //
                                    .append(' ').append(annos[i].name()) //
                                    .toString();
                Tr.debug(this, tc, "getAnnotations", a);
            }
            return annos;
        }

        @Override
        @Trivial
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
                        binding = new ContextServiceDefinitionBinding(jndiName, ivNameSpaceConfig, eeVersion);
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