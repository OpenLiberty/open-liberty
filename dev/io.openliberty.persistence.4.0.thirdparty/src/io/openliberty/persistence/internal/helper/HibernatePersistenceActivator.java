/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package io.openliberty.persistence.internal.helper;

import java.util.Hashtable;

import jakarta.persistence.spi.PersistenceProvider;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Bundle activator for the Hibernate JPA 4.0 third-party bundle.
 * Registers Hibernate as a Jakarta Persistence provider in the OSGi service registry
 * so that Liberty's JPA container can discover it when no {@code <provider>} is
 * specified in {@code persistence.xml}.
 * <p>
 * JTA platform selection is handled entirely by
 * {@code AbstractJPAProviderIntegration}, which detects whether
 * {@code WebSphereLibertyJtaPlatform} is available and sets the appropriate
 * per-persistence-unit property. No system property is set here.
 */
public class HibernatePersistenceActivator implements BundleActivator {

    private static final String PERSISTENCE_PROVIDER = PersistenceProvider.class.getName();
    private static final String HIBERNATE_PERSISTENCE_PROVIDER = "org.hibernate.jpa.HibernatePersistenceProvider";

    private ServiceRegistration<?> hibernateSvcReg = null;

    @Override
    public void start(BundleContext context) throws Exception {
        // Register Hibernate as a JPA PersistenceProvider in the OSGi service registry.
        // JakartaPersistenceActivator tracks this service and exposes it via
        // PersistenceProviderResolverHolder so Liberty's JPA container can discover
        // Hibernate when no <provider> is specified in persistence.xml.
        PersistenceProvider provider = new org.hibernate.jpa.HibernatePersistenceProvider();
        Hashtable<String, Object> props = new Hashtable<>();
        props.put(PERSISTENCE_PROVIDER, HIBERNATE_PERSISTENCE_PROVIDER);
        props.put("jakarta.persistence.provider", HIBERNATE_PERSISTENCE_PROVIDER);
        hibernateSvcReg = context.registerService(PERSISTENCE_PROVIDER, provider, props);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (hibernateSvcReg != null) {
            hibernateSvcReg.unregister();
            hibernateSvcReg = null;
        }
    }
}
