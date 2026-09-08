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
 * Registers Hibernate as a Jakarta Persistence provider in the OSGi service
 * registry and sets system properties that configure Hibernate to use Liberty's
 * transaction and classloading infrastructure.
 */
public class HibernatePersistenceActivator implements BundleActivator {

    private static final String PERSISTENCE_PROVIDER = PersistenceProvider.class.getName();
    private static final String HIBERNATE_PERSISTENCE_PROVIDER = "org.hibernate.jpa.HibernatePersistenceProvider";

    private static final String HIBERNATE_JTA_PLATFORM = "hibernate.transaction.jta.platform";
    private static final String LIBERTY_JTA_PLATFORM_CLASS = "com.ibm.ws.jpa.hibernate.LibertyJtaPlatform";

    private static final String HIBERNATE_BYTECODE_OPTIMIZER = "hibernate.bytecode.use_reflection_optimizer";

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

        // Tell Hibernate to use Liberty's JTA transaction manager integration.
        // This is the fallback for older Hibernate versions without built-in
        // WebSphereLibertyJtaPlatform; for newer versions it is overridden by
        // detection in AbstractJPAProviderIntegration.
        System.setProperty(HIBERNATE_JTA_PLATFORM, LIBERTY_JTA_PLATFORM_CLASS);

        // Disable reflection optimizer — bytecode enhancement is handled by
        // Liberty's class transformation pipeline, not Hibernate's own optimizer.
        System.setProperty(HIBERNATE_BYTECODE_OPTIMIZER, "false");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (hibernateSvcReg != null) {
            hibernateSvcReg.unregister();
            hibernateSvcReg = null;
        }
        System.clearProperty(HIBERNATE_JTA_PLATFORM);
        System.clearProperty(HIBERNATE_BYTECODE_OPTIMIZER);
    }
}
