/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package io.openliberty.concurrent.internal.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.PassivationCapable;

/**
 * Bean that delegates to the OSGi service registry to obtain ManagedScheduledExecutorService resources.
 */
public class ManagedScheduledExecutorBean implements Bean<ManagedScheduledExecutorService>, PassivationCapable {
    private final static TraceComponent tc = Tr.register(ManagedScheduledExecutorBean.class);

    /**
     * Injectable bean types.
     */
    private final Set<Type> beanTypes = Set.of(ManagedScheduledExecutorService.class);

    /**
     * OSGi filter for the resource.
     */
    private final String filter;

    /**
     * Qualifiers for the injection points for this bean.
     */
    private final Set<Annotation> qualifiers;

    /**
     * Construct a new Producer/ProducerFactory for this resource.
     *
     * @param filter     OSGi filter for the resource.
     * @param qualifiers qualifiers for the bean.
     */
    public ManagedScheduledExecutorBean(String filter, Set<Annotation> qualifiers) {
        this.filter = filter;
        this.qualifiers = qualifiers;
    }

    @Override
    @Trivial
    public ManagedScheduledExecutorService create(CreationalContext<ManagedScheduledExecutorService> cc) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "create", cc, filter, qualifiers);

        ManagedScheduledExecutorService instance;
        Bundle bundle = FrameworkUtil.getBundle(ManagedScheduledExecutorBean.class);
        BundleContext bundleContext = bundle.getBundleContext();
        Collection<ServiceReference<ManagedScheduledExecutorService>> refs;
        try {
            refs = bundleContext.getServiceReferences(ManagedScheduledExecutorService.class, filter);
        } catch (InvalidSyntaxException x) {
            throw new IllegalArgumentException(x); // internal error forming the filter?
        }
        Iterator<ServiceReference<ManagedScheduledExecutorService>> it = refs.iterator();
        if (it.hasNext())
            instance = bundleContext.getService(it.next());
        else
            throw new IllegalStateException("The ManagedScheduledExecutorService resource with " + filter + " filter cannot be found or is unavailable."); // TODO NLS

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "create", instance);
        return instance;
    }

    @Override
    public void destroy(ManagedScheduledExecutorService instance, CreationalContext<ManagedScheduledExecutorService> creationalContext) {
    }

    @Override
    public Class<ManagedScheduledExecutorService> getBeanClass() {
        return ManagedScheduledExecutorService.class;
    }

    /**
     * @return unique identifier for PassivationCapable.
     */
    @Override
    public String getId() {
        return new StringBuilder(getClass().getName()) //
                        .append(":").append(qualifiers) //
                        .append(':').append(filter) //
                        .toString();
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return ApplicationScoped.class;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.emptySet();
    }

    @Override
    public Set<Type> getTypes() {
        return beanTypes;
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    @Override
    @Trivial
    public String toString() {
        return new StringBuilder(getClass().getSimpleName()).append('@').append(Integer.toHexString(hashCode())) //
                        .append(' ').append(filter) //
                        .append(" with qualifiers ").append(qualifiers) //
                        .toString();
    }
}