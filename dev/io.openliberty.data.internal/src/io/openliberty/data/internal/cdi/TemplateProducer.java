/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.data.internal.cdi;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.annotation.Trivial;

import jakarta.data.Template;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

public class TemplateProducer implements PrivilegedAction<DataExtensionMetadata> {

    private final DataExtensionMetadata svc;

    //public void dispose(Template template) {
    //    System.out.println("Producer.dispose for " + template);
    //}

    public TemplateProducer() {
        svc = AccessController.doPrivileged(this);
    }

    @ApplicationScoped
    @Produces
    public Template produce() {
        return new Delegator();
    }

    /**
     * Obtain the service that informed CDI of this extension.
     */
    @Override
    @Trivial
    public DataExtensionMetadata run() {
        BundleContext bundleContext = FrameworkUtil.getBundle(DataExtensionMetadata.class).getBundleContext();
        ServiceReference<DataExtensionMetadata> ref = bundleContext.getServiceReference(DataExtensionMetadata.class);
        return bundleContext.getService(ref);
    }

    @Trivial
    private class Delegator implements Template {
        @Override
        public <T, K> void delete(Class<T> entityClass, K id) {
            if (id == null || entityClass == null)
                throw new NullPointerException(id == null ? "id" : "entityClass");

            svc.getProvider(entityClass).getTemplate().delete(entityClass, id);
        }

        @Override
        public <T, K> Optional<T> find(Class<T> entityClass, K id) {
            if (id == null || entityClass == null)
                throw new NullPointerException(id == null ? "id" : "entityClass");

            return svc.getProvider(entityClass).getTemplate().find(entityClass, id);
        }

        @Override
        public <T> T insert(T entity) {
            if (entity == null)
                throw new NullPointerException("entity");

            return svc.getProvider(entity.getClass()).getTemplate().insert(entity);
        }

        @Override
        public <T> T insert(T entity, Duration ttl) {
            if (entity == null)
                throw new NullPointerException("entity");

            return svc.getProvider(entity.getClass()).getTemplate().insert(entity, ttl);
        }

        @Override
        public <T> Iterable<T> insert(Iterable<T> entities) {
            Iterator<T> it = entities.iterator();
            if (!it.hasNext())
                return Collections.<T> emptyList();

            Class<?> entityClass = it.next().getClass();
            return svc.getProvider(entityClass).getTemplate().insert(entities);
        }

        @Override
        public <T> Iterable<T> insert(Iterable<T> entities, Duration ttl) {
            Iterator<T> it = entities.iterator();
            if (!it.hasNext())
                return Collections.<T> emptyList();

            Class<?> entityClass = it.next().getClass();
            return svc.getProvider(entityClass).getTemplate().insert(entities, ttl);
        }

        @Override
        public <T> T update(T entity) {
            if (entity == null)
                throw new NullPointerException("entity");

            return svc.getProvider(entity.getClass()).getTemplate().update(entity);
        }

        @Override
        public <T> Iterable<T> update(Iterable<T> entities) {
            Iterator<T> it = entities.iterator();
            if (!it.hasNext())
                return Collections.<T> emptyList();

            Class<?> entityClass = it.next().getClass();
            return svc.getProvider(entityClass).getTemplate().update(entities);
        }
    }
}