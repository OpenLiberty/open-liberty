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
package com.ibm.ws.cdi.impl.weld.injection;

import java.lang.annotation.Annotation;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import org.jboss.weld.injection.spi.ResourceReference;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIException;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionTarget;

/**
 * A ResourceReferenceFactory implementation based on an InjectionBinding.
 * <p>
 * The resource to be injected will be created by calling getInjectionObject on the InjectionBinding.
 *
 * @param <T>
 *
 * @param <T> The type of the resource
 */
public class ResourceReferenceFactoryImpl<T> implements ResourceReferenceFactory<T> {

    private static final TraceComponent tc = Tr.register(ResourceReferenceFactoryImpl.class);

    private final WebSphereInjectionServices webSphereInjectionServices;
    private final InjectionPoint injectionPoint;
    private final Class<?> targetClass;

    private InjectionBinding<?> binding = null;

    public ResourceReferenceFactoryImpl(WebSphereInjectionServices webSphereInjectionServices, InjectionPoint injectionPoint) {
        this.webSphereInjectionServices = webSphereInjectionServices;
        this.injectionPoint = injectionPoint;

        Bean<?> bean = injectionPoint.getBean();

        if (bean != null) {
            this.targetClass = bean.getBeanClass();
        }
        else {
            this.targetClass = injectionPoint.getMember().getDeclaringClass();
        }
    }

    /** {@inheritDoc} */
    @Override
    public ResourceReference<T> createResource() {

        if (this.binding == null) {
            InjectionTarget[] targets = null;
            try {
                targets = this.webSphereInjectionServices.getInjectionTargets(targetClass);
            } catch (CDIException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Could not find anything to inject for ", injectionPoint.getMember());
                }
            }

            if (targets != null && targets.length > 0) {
                for (InjectionTarget target : targets) {
                    if (target.getMember().equals(injectionPoint.getMember())) {
                        this.binding = target.getInjectionBinding();
                        break;
                    }
                }
            }
        }

        ResourceReference<T> reference = null;
        if (binding != null) {
            reference = createResourceReference(binding);
        }
        return reference;
    }

    private <S extends Annotation> ResourceReference<T> createResourceReference(InjectionBinding<S> binding) {
        return new BindingResourceReferenceImpl<T, S>(binding);
    }

}