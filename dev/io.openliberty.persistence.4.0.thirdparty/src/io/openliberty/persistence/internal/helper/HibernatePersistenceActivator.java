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

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Bundle activator for the Hibernate JPA 4.0 third-party bundle.
 * Sets system properties that configure Hibernate to use Liberty's
 * transaction and classloading infrastructure.
 */
public class HibernatePersistenceActivator implements BundleActivator {

    private static final String HIBERNATE_JTA_PLATFORM = "hibernate.transaction.jta.platform";
    private static final String LIBERTY_JTA_PLATFORM_CLASS = "com.ibm.ws.jpa.hibernate.LibertyJtaPlatform";

    private static final String HIBERNATE_BYTECODE_OPTIMIZER = "hibernate.bytecode.use_reflection_optimizer";

    @Override
    public void start(BundleContext context) throws Exception {
        // Tell Hibernate to use Liberty's JTA transaction manager integration.
        // This is the fallback for older Hibernate versions without built-in
        // WebSphereLibertyJtaPlatform; for 5.2.13+ it is overridden by detection
        // in AbstractJPAProviderIntegration.
        System.setProperty(HIBERNATE_JTA_PLATFORM, LIBERTY_JTA_PLATFORM_CLASS);

        // Disable reflection optimizer — bytecode enhancement is handled by
        // Liberty's class transformation pipeline, not Hibernate's own optimizer.
        System.setProperty(HIBERNATE_BYTECODE_OPTIMIZER, "false");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        System.clearProperty(HIBERNATE_JTA_PLATFORM);
        System.clearProperty(HIBERNATE_BYTECODE_OPTIMIZER);
    }
}
