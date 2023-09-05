/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
import java.security.AccessController;
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
import com.ibm.ws.kernel.service.util.SecureAction;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.PassivationCapable;

/**
 * Bean that delegates to the OSGi service registry to obtain resources.
 *
 * @param <T> type of resource (such as ManagedExecutorService or ContextService)
 */
public class ConcurrencyResourceBean<T> implements Bean<T>, PassivationCapable {
    private final static TraceComponent tc = Tr.register(ConcurrencyResourceBean.class);

    // TODO eventually remove for code that only applies to EE 11+
    final static SecureAction priv = AccessController.doPrivileged(SecureAction.get());

    /**
     * Injectable bean types.
     */
    private final Set<Type> beanTypes;

    /**
     * OSGi filter for the resource.
     */
    private final String filter;

    /**
     * Qualifiers for the injection points for this bean.
     */
    private final Set<Annotation> qualifiers;

    /**
     * Type of resource.
     */
    private final Class<T> resourceType;

    /**
     * Construct a new Producer/ProducerFactory for this resource.
     *
     * @param resourceType type of resource.
     * @param filter       OSGi filter for the resource.
     * @param types        bean types.
     * @param qualifiers   qualifiers for the bean.
     */
    public ConcurrencyResourceBean(Class<T> resourceType, String filter, Set<Type> types, Set<Annotation> qualifiers) {
        this.filter = filter;
        this.resourceType = resourceType;
        this.beanTypes = types;
        this.qualifiers = qualifiers;
    }

    @Override
    @Trivial
    public T create(CreationalContext<T> cc) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "create", cc, resourceType, filter, qualifiers);

        T instance;
        Bundle bundle = FrameworkUtil.getBundle(ConcurrencyResourceBean.class);
        BundleContext bundleContext = priv.getBundleContext(bundle);
        Collection<ServiceReference<T>> refs;
        try {
            refs = priv.getServiceReferences(bundleContext, resourceType, filter);
        } catch (InvalidSyntaxException x) {
            throw new IllegalArgumentException(x); // internal error forming the filter?
        }
        Iterator<ServiceReference<T>> it = refs.iterator();
        if (it.hasNext())
            instance = priv.getService(bundleContext, it.next());
        else
            throw new IllegalStateException("The " + resourceType.getName() + " resource with " + filter + " filter cannot be found or is unavailable."); // TODO NLS

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "create", instance);
        return instance;
    }

    @Override
    public void destroy(T instance, CreationalContext<T> creationalContext) {
    }

    @Override
    public Class<T> getBeanClass() {
        return resourceType;
    }

    /**
     * @return unique identifier for PassivationCapable.
     */
    @Override
    public String getId() {
        return new StringBuilder(getClass().getName()) //
                        .append(':').append(resourceType.getClass().getSimpleName()) //
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
    public String toString() {
        return new StringBuilder(getClass().getSimpleName()).append('@').append(Integer.toHexString(hashCode())) //
                        .append(' ').append(resourceType.getClass().getSimpleName()) //
                        .append(' ').append(filter) //
                        .append(" with qualifiers ").append(qualifiers) //
                        .toString();
    }
}